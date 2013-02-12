/*
 * 
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2010 Nigel Westbury <westbury@users.sourceforge.net>
 * 
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package net.sf.jmoney.taxes.us.wizards;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

import net.sf.jmoney.model2.IDatastoreManager;
import net.sf.jmoney.stocks.gains.CapitalGainsCalculator;
import net.sf.jmoney.stocks.gains.StockPurchaseAndSale;
import net.sf.jmoney.stocks.model.Stock;
import net.sf.jmoney.stocks.model.StockAccount;
import net.sf.jmoney.taxes.us.Activator;
import net.sf.jmoney.taxes.us.resources.Messages;
import net.sf.jmoney.views.feedback.Feedback;
import net.sf.jmoney.views.feedback.FeedbackView;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

/**
 * A wizard to export data to a TXF file.
 * <P>
 * Currently only stock purchase and sales are exported, giving the tax software the information
 * it needs to complete the stock capital gains on Schedule D.
 */
public class TxfExportWizard extends Wizard implements IExportWizard {

	/**
	 * Date format to be used when writing dates to the TXF file.
	 */
	private DateFormat txfFileDateFormat = new SimpleDateFormat("M/d/yy");

	//	private NumberFormat quantityFormat = new DecimalFormat("###,###,###");

	private IWorkbenchWindow window;

	private IStructuredSelection selection;

	private TxfExportWizardPage mainPage;

	public TxfExportWizard() {
		IDialogSettings workbenchSettings = Activator.getDefault().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection("TxfExportWizard");//$NON-NLS-1$
		if (section == null) {
			section = workbenchSettings.addNewSection("TxfExportWizard");//$NON-NLS-1$
		}
		setDialogSettings(section);
	}

	/**
	 * We will cache window object in order to be able to provide parent shell
	 * for the message dialog.
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.window = workbench.getActiveWorkbenchWindow();
		this.selection = selection;

		// Original JMoney disabled the export menu items when no
		// session was open.  I don't know how to do that in Eclipse,
		// so we display a message instead.
		IDatastoreManager sessionManager = (IDatastoreManager)window.getActivePage().getInput();
		if (sessionManager == null) {
			MessageDialog.openError(
					window.getShell(), 
					"Disabled Action Selected", 
			"You cannot export data unless you have a session open.  You must first open a session.");
			return;
		}

		/*
		 * This wizard exports all the selected accounts.  We check that the selected
		 * accounts are all stock accounts, and that at least one was selected.
		 */
		if (selection.isEmpty()) {
			MessageDialog.openError(
					window.getShell(), 
					"Unable to Export Capital Gains Data", 
					"You must select one or more stock accounts before running this wizard."
			);
			return;
		}
		for (Object selectedObject : selection.toList()) {
			if (!(selectedObject instanceof StockAccount)) {
				MessageDialog.openError(
						window.getShell(), 
						"Unable to Export Capital Gains Data", 
						"You have selected something that is not a stock account.  Only stock accounts can be selected when exporting capital gains data."
				);
				return;
			}
		}

		mainPage = new TxfExportWizardPage(window);
		addPage(mainPage);
	}

	@Override
	public boolean performFinish() {
		String fileName = mainPage.getFileName();
		final File txfFile = new File(fileName);

		if (dontOverwrite(txfFile))
			return false;

		int calendarYear = mainPage.getYear();
		Calendar calendar = Calendar.getInstance();
		calendar.clear();
		calendar.set(calendarYear, Calendar.JANUARY, 1);
		final Date startDate = calendar.getTime();
		calendar.set(calendarYear, Calendar.DECEMBER, 31);
		final Date endDate = calendar.getTime();

		MultiStatus result = exportFile(txfFile, startDate, endDate);

		if (!result.isOK()) {

			Feedback feedback = new Feedback(result.getMessage(), result) {
				@Override
				protected boolean canExecuteAgain() {
					return true;
				}

				@Override
				protected IStatus executeAgain() {
					return exportFile(txfFile, startDate, endDate);
				}
			};

			try {
				FeedbackView view = (FeedbackView)window.getActivePage().showView(FeedbackView.ID, null, IWorkbenchPage.VIEW_ACTIVATE);
				view.showResults(feedback);
			} catch (PartInitException e) {
				throw new RuntimeException(e);
			}
		}

		return true;
	}

	private MultiStatus exportFile(File txfFile, Date startDate, Date endDate) {
		MultiStatus result = new MultiStatus(Activator.PLUGIN_ID, IStatus.OK, "Export TXF File: " + txfFile.getPath(), null);

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(txfFile));

			// write header
			writeln(writer, "V041");
			writeln(writer, "AJMoney");
			writeln(writer, "D " + txfFileDateFormat.format(new Date()));
			writeln(writer, "^");

			Collection<StockPurchaseAndSale> matchedPurchaseAndSales = new ArrayList<StockPurchaseAndSale>();
			
			for (Object selectedObject : selection.toList()) {
				if (selectedObject instanceof StockAccount) {
					StockAccount account = (StockAccount)selectedObject;
					IStatus status = CapitalGainsCalculator.exportCapitalGains(account, startDate, endDate, matchedPurchaseAndSales);
					result.add(status);
				}
			}

			for (StockPurchaseAndSale purchaseAndSale : matchedPurchaseAndSales) {
				writeEntry(writer, purchaseAndSale.getStock(), purchaseAndSale.getQuantity(), purchaseAndSale.getBuyDate(), purchaseAndSale.getBasis(), purchaseAndSale.getSellDate(), purchaseAndSale.getProceeds());
			}
			
			writer.close();
		} catch (IOException e) {
			MessageDialog.openError(window.getShell(), "Export Failed", e.getLocalizedMessage());
		}
		return result;
	}


	private boolean dontOverwrite(File file) {
		if (file.exists()) {
			String title = Messages.Dialog_FileExists;
			String question = NLS.bind(Messages.Dialog_OverwriteExistingFile, file.getPath());

			boolean answer = MessageDialog.openQuestion(
					window.getShell(),
					title,
					question);
			return !answer;
		} else {
			return false;
		}
	}

	/**
	 * The format is this:
	 * 
	 * <code>
V041
AMy Software
D 10/27/2004
^
TD
N323
C1
L1
P300 Aspreva Pharmaceuticals
D5/11/06
$9,883.99
D1/3/07
$6,271.81
^
TD
N321
C1
L1
P700 BTU Intl
D5/9/06
$13,506.99
D1/3/07
$6,884.79
^
	 <code>
	 * @throws IOException 
	 */
	private void writeEntry(BufferedWriter writer, Stock stock, long quantity, Date purchaseDate, long costBasis, Date sellDate, long saleProceeds) throws IOException {
		NumberFormat currencyFormat = NumberFormat.getInstance(Locale.US);
		currencyFormat.setMinimumFractionDigits(2);
		currencyFormat.setMaximumFractionDigits(2);

		writeln(writer, "TD");

		Calendar c = Calendar.getInstance();
		c.setTime(purchaseDate);
		c.add(Calendar.YEAR, 1);
		if (c.getTime().before(sellDate)) {
			// Long term capital gains
			writeln(writer, "N323");
		} else {
			// Short term capital gains
			writeln(writer, "N321");
		}

		writeln(writer, "C1"); // Copy 1, but no idea why this is in this file at all
		writeln(writer, "L1"); // Line 1, but no idea why this is in this file at all
		writeln(writer, "P" + formatQuantity(quantity) + " " + stock.getName());
		writeln(writer, "D" + txfFileDateFormat.format(purchaseDate));
		writeln(writer, "$" + formatCurrency(costBasis));
		writeln(writer, "D" + txfFileDateFormat.format(sellDate));
		writeln(writer, "$" + formatCurrency(saleProceeds));
		writeln(writer, "^");
	}

	static private String formatQuantity(long quantity) {
		if (quantity % 1000 == 0) {
			return Long.toString(quantity/1000);
		} else {
			return new BigDecimal(quantity).divide(new BigDecimal(1000)).toPlainString();
		}
	}

	private String formatCurrency(long amount) {
		String x = Long.toString(amount);

		// Ensure at least 3 digits
		while (x.length() < 3) {
			x = "0" + x;
		}

		// Format the whole amount
		NumberFormat quantityFormat = new DecimalFormat("###,###,###");
		String result = quantityFormat.format(amount / 100);

		return result + "." + x.substring(x.length() - 2);
	}


	private void writeln(BufferedWriter writer, String line) throws IOException {
		writer.write(line);
		writer.newLine();
	}

}