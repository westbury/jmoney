/*
 * 
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2021 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.ofx.wizards;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.ofx.Activator;
import net.sf.jmoney.ofx.model.OfxEntryInfo;
import net.sf.jmoney.stocks.model.StockAccount;
import net.sf.jmoney.stocks.pages.StockBuyOrSellFacade;
import net.sf.jmoney.stocks.pages.StockDividendFacade;
import net.sf.jmoney.stocks.pages.StockEntryFacade;
import net.sf.jmoney.stocks.pages.StockEntryRowControl;
import net.sf.jmoney.stocks.pages.StockEntryRowControl.TransactionType;
import net.sf.jmoney.views.AccountEditor;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com.webcohesion.ofx4j.io.v2.OFXV2Writer;

/**
 * A wizard to export data to an OFX file.
 * 
 * Currently this wizard if a single page wizard that asks only for the file.
 * This feature is implemented as a wizard because the Eclipse workbench import
 * action requires all export implementations to be wizards.
 */
public class OfxExportWizard extends Wizard implements IExportWizard {
	private IWorkbenchWindow window;

	private StockAccount stockAccount;
	
	private OfxExportWizardPage mainPage;

	public OfxExportWizard() {
		IDialogSettings workbenchSettings = Activator.getDefault().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection("OfxExportWizard");//$NON-NLS-1$
		if (section == null) {
			section = workbenchSettings.addNewSection("OfxExportWizard");//$NON-NLS-1$
		}
		setDialogSettings(section);
	}

	/**
	 * We will cache window object in order to be able to provide parent shell
	 * for the message dialog.
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.window = workbench.getActiveWorkbenchWindow();

		mainPage = new OfxExportWizardPage(window);
		addPage(mainPage);
		
		stockAccount = (StockAccount)selection.getFirstElement();
	}

	@Override
	public boolean performFinish() {
		String fileName = mainPage.getFileName();
		if (fileName != null) {
			File ofxFile = new File(fileName);
//			File file = new File("C:/myfile.txt");

			this.export(ofxFile);
//			if (allImported && mainPage.IsDeleteFile()) {
//				boolean isDeleted = ofxFile.delete();
//				if (!isDeleted) {
//					MessageDialog.openWarning(window.getShell(), "OFX file not deleted", 
//							MessageFormat.format(
//									"All entries in {0} have been imported and an attempt was made to delete the file.  However the file deletion failed.", 
//									ofxFile.getName()));
//				}
//			}
		}

		return true;
	}

	private void export(File file) {
			try (FileOutputStream fos = new FileOutputStream(file)) {
				OFXV2Writer writer = new OFXV2Writer(fos);

				Map<String, String> headers = new HashMap<>();
				headers.put("SECURITY", "NONE");
				writer.writeHeaders(headers);

				writer.setWriteAttributesOnNewLine(true);

				writer.writeStartAggregate("OFX");
				writer.writeStartAggregate("SIGNONMSGSRSV1");
				writer.writeStartAggregate("SONRS");
				writer.writeStartAggregate("FI");
				writer.writeElement("ORG", "wellsfargo.com");
				writer.writeEndAggregate("FI");
				writer.writeEndAggregate("SONRS");
				writer.writeEndAggregate("SIGNONMSGSRSV1");

				writer.writeStartAggregate("INVSTMTMSGSRSV1");
				writer.writeStartAggregate("INVSTMTTRNRS");
				writer.writeStartAggregate("INVSTMTRS");
				writer.writeStartAggregate("INVTRANLIST");

				for (Entry entry : stockAccount.getEntries()) {
					// For now, only process entries that affect the currency amount
					if (entry.getCommodityInternal() == stockAccount.getCurrency()) {

						StockEntryFacade facade = new StockEntryFacade(entry, stockAccount);

						if (facade.getTransactionType() != null) {
						switch (facade.getTransactionType()) {
						case Buy:
							StockBuyOrSellFacade buyFacade = facade.buyOrSellFacade().getValue();
							writer.writeStartAggregate("BUYMF");
							writer.writeStartAggregate("INVBUY");
							writeTradeInv(writer, buyFacade, StockEntryRowControl.TransactionType.Buy);
							writer.writeEndAggregate("INVBUY");
							writer.writeElement("BUYTYPE", "BUY");
							writer.writeEndAggregate("BUYMF");
							break;
						case Sell:
							StockBuyOrSellFacade sellFacade = facade.buyOrSellFacade().getValue();
							writer.writeStartAggregate("SELLMF");
							writer.writeStartAggregate("INVSELL");
							writeTradeInv(writer, sellFacade, StockEntryRowControl.TransactionType.Sell);
							writer.writeEndAggregate("INVSELL");
							writer.writeElement("SELLTYPE", "SELL");
							writer.writeEndAggregate("SELLMF");
							break;
						case Dividend:
							StockDividendFacade dividendFacade = facade.dividendFacade().getValue();
							writer.writeStartAggregate("INCOME");

							writer.writeStartAggregate("INVTRAN");
							if (OfxEntryInfo.getFitidAccessor().getValue(entry) != null) {
								writer.writeElement("FITID", OfxEntryInfo.getFitidAccessor().getValue(entry));
							}
							writer.writeElement("DTTRADE", toTimestamp(entry.getTransaction().getDate()));
							writer.writeElement("DTSETTLE", toTimestamp(entry.getValuta() == null ? entry.getTransaction().getDate() : entry.getValuta()));
							if (entry.getMemo() != null && !entry.getMemo().trim().isEmpty()) {
								writer.writeElement("MEMO", entry.getMemo());
							}
							writer.writeElement("CURRSYM", "USD");
							writer.writeEndAggregate("INVTRAN");

							writer.writeStartAggregate("SECID");
							if (dividendFacade.getSecurity() != null && dividendFacade.getSecurity().getCusip() != null) {
								writer.writeElement("UNIQUEID", dividendFacade.getSecurity().getCusip());
								writer.writeElement("UNIQUEIDTYPE", "CUSPID");
							}
							writer.writeEndAggregate("SECID");

							writer.writeElement("INCOMETYPE", "DIV");
							writer.writeElement("TOTAL", entry.getCommodity().format(dividendFacade.getDividendAmount()));
							writer.writeElement("SUBACCTSEC", "CASH");
							writer.writeElement("SUBACCTFUND", "CASH");
							writer.writeStartAggregate("CURRENCY");
							writer.writeElement("CURRRATE", "1");
							writer.writeElement("CURRSYM", "USD");
							writer.writeEndAggregate("CURRENCY");

							writer.writeEndAggregate("INCOME");
							break;
						case Other:
							break;
						case Takeover:
							break;
						default:
							break;

						}
						}
					}
				}

				writer.writeEndAggregate("INVTRANLIST");
				writer.writeEndAggregate("INVSTMTRS");
				writer.writeEndAggregate("INVSTMTTRNRS");
				writer.writeEndAggregate("INVSTMTMSGSRSV1");


				writer.writeEndAggregate("OFX");
				writer.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	private void writeTradeInv(OFXV2Writer writer, StockBuyOrSellFacade tradeFacade, TransactionType type) throws IOException {
		Entry entry = tradeFacade.getMainEntry();
		
		writer.writeStartAggregate("INVTRAN");
		if (OfxEntryInfo.getFitidAccessor().getValue(entry) != null) {
			writer.writeElement("FITID", OfxEntryInfo.getFitidAccessor().getValue(entry));
		}
		writer.writeElement("DTTRADE", toTimestamp(entry.getTransaction().getDate()));
		writer.writeElement("DTSETTLE", toTimestamp(entry.getValuta() == null ? entry.getTransaction().getDate() : entry.getValuta()));
		if (entry.getMemo() != null && !entry.getMemo().trim().isEmpty()) {
			writer.writeElement("MEMO", entry.getMemo());
		}
		writer.writeEndAggregate("INVTRAN");

		writer.writeStartAggregate("SECID");
		if (tradeFacade.getSecurity() != null && tradeFacade.getSecurity().getCusip() != null) {
			writer.writeElement("UNIQUEID", tradeFacade.getSecurity().getCusip());
			writer.writeElement("UNIQUEIDTYPE", "CUSPID");
		}
		writer.writeEndAggregate("SECID");

		writer.writeElement("UNITS", tradeFacade.getSecurity().format(tradeFacade.quantity().getValue()));
		writer.writeElement("UNITPRICE", tradeFacade.calculatePrice().toPlainString());
		writer.writeElement("COMMISSION", entry.getCommodity().format(tradeFacade.getCommissionAmount()));
		writer.writeElement("INCOMETYPE", "DIV");

		writer.writeElement("INCOMETYPE", "DIV");
		writer.writeElement("TOTAL", entry.getCommodity().format(tradeFacade.getMainEntry().getAmount()));
		writer.writeElement("SUBACCTSEC", "CASH");
		writer.writeElement("SUBACCTFUND", "CASH");
		writer.writeStartAggregate("CURRENCY");
		writer.writeElement("CURRRATE", "1");
		writer.writeElement("CURRSYM", "USD");
		writer.writeEndAggregate("CURRENCY");
	}


	static DateFormat df = new SimpleDateFormat("yyyyMMdd");

	private String toTimestamp(Date date) {
		return df.format(date) + "120000.000[0:GMT]";
//				"20091030120000.000[-4:EDT]";
	}
}