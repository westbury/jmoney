/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2004,2009 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.importer.wizards;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;

import net.sf.jmoney.importer.Activator;
import net.sf.jmoney.model2.IDataManagerForAccounts;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.TransactionManagerForAccounts;

/**
 * A wizard to import data from a comma-separated file that has been down-loaded
 * into a file on the local machine.
 * <P>
 * This wizard is a single page wizard that asks only for the file.
 */
public abstract class CsvImportWizard extends Wizard {

	protected IWorkbenchWindow window;

	protected CsvImportWizardPage mainPage;

	protected CsvTransactionReader reader;
	
	protected List<MultiRowTransaction> currentMultiRowProcessors = new ArrayList<MultiRowTransaction>();

	/**
	 * This form of the constructor is used when being called from
	 * the Eclipse 'import' menu.
	 */
	public CsvImportWizard() {
		IDialogSettings workbenchSettings = Activator.getDefault().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection("CsvImportToAccountWizard");//$NON-NLS-1$
		if (section == null) {
			section = workbenchSettings.addNewSection("CsvImportToAccountWizard");//$NON-NLS-1$
		}
		setDialogSettings(section);
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.window = workbench.getActiveWorkbenchWindow();

		mainPage = new CsvImportWizardPage(window, getDescription());

		addPage(mainPage);
	}

	@Override
	public boolean performFinish() {
		String fileName = mainPage.getFileName();
		if (fileName != null) {
			File csvFile = new File(fileName);
			;
			boolean allImported = importFile(csvFile);

			if (allImported && mainPage.IsDeleteFile()) {
				boolean isDeleted = csvFile.delete();
				if (!isDeleted) {
					MessageDialog.openWarning(window.getShell(), "OFX file not deleted",
							MessageFormat.format(
									"All entries in {0} have been imported and an attempt was made to delete the file.  However the file deletion failed.",
									csvFile.getName()));
				}
			}
		}

		return true;
	}


	public boolean importFile(File file) {

		IDataManagerForAccounts datastoreManager = (IDataManagerForAccounts)window.getActivePage().getInput();
		if (datastoreManager == null) {
			MessageDialog.openError(window.getShell(), "Unavailable", "You must open an accounting session before you can create an account.");
			return false;
		}

		try {
			/*
			 * Create a transaction to be used to import the entries.  This allows the entries to
			 * be more efficiently written to the back-end datastore and it also groups
			 * the entire import as a single change for undo/redo purposes.
			 */
			TransactionManagerForAccounts transactionManager = new TransactionManagerForAccounts(datastoreManager);
			Session session = transactionManager.getSession();

			startImport(transactionManager);

			reader = getCsvTransactionReader(new FileReader(file));

        	if (processRows(session)) {
        		/*
        		 * All entries have been imported so we
        		 * can now commit the imported entries to the datastore.
        		 */
        		String transactionDescription = MessageFormat.format("Import {0}", file.getName());
        		transactionManager.commit(transactionDescription);									
        		return true;
        	} else {
        		return false;
        	}
		} catch (IOException e) {
			// This is probably not likely to happen so the default error handling is adequate.
			throw new RuntimeException(e);
//		} catch (ImportException e) {
//			// There are data in the import file that we are unable to process
//			e.printStackTrace();
//			MessageDialog.openError(window.getShell(), "Error in row " + e.getRowNumber(), e.getMessage());
//			return false;
		} catch (Exception e) {
			// There are data in the import file that we are unable to process
			e.printStackTrace();
			MessageDialog.openError(window.getShell(), "Errors in the downloaded file", e.getMessage());
			return false;
		}
	}

	/**
	 * This method is designed to be overridden if a custom implementation
	 * of the reader is required.
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 * @throws ImportException
	 */
	protected CsvTransactionReader getCsvTransactionReader(Reader reader)
			throws IOException, ImportException {
		return new CsvTransactionReader(reader, getExpectedColumns());
	}

	/**
	 * 
	 * @param session
	 * @return true if processing completed, false if processing did not complete
	 * 			because the user cancelled or some other reason
	 * @throws IOException
	 * @throws ImportException
	 */
	protected boolean processRows(Session session)
			throws IOException, ImportException {
		while (!reader.isEndOfFile()) {
			/*
			 * Try each of the current row processors.  If any are 'done' then we
			 * create their transaction and remove the processor from our list.
			 * If any can process (and only one should be able to process) then
			 * we leave it at that.
			 */
			boolean processed = false;
			for (Iterator<MultiRowTransaction> iter = currentMultiRowProcessors.iterator(); iter.hasNext(); ) {
				MultiRowTransaction currentMultiRowProcessor = iter.next();

				boolean thisOneProcessed = currentMultiRowProcessor.processCurrentRow(session);

				if (thisOneProcessed) {
					if (processed) {
						throw new RuntimeException("Can't have two current processors that can process the same row");
					}
					processed = true;
				}

				if (currentMultiRowProcessor.isDone()) {
					currentMultiRowProcessor.createTransaction(session);
					iter.remove();
				}
			}

			if (!processed) {
				importLine(reader);
			}
			
			reader.readNext();
		}

		for (MultiRowTransaction currentMultiRowProcessor : currentMultiRowProcessors) {
			currentMultiRowProcessor.createTransaction(session);
		}
		
		return true;
	}

	public abstract class ImportedColumn {

		/**
		 * Text in the header row that identifies this column
		 */
		private String name;

		protected int columnIndex;

		public boolean isOptional = false;
		
		ImportedColumn(String name) {
			this.name = name;
		}

		/**
		 * @return text in the header row that identifies this column
		 */
		public String getName() {
			return name;
		}

		public void setColumnIndex(int columnIndex) {
			this.columnIndex = columnIndex;
		}

		public boolean isOptional() {
			return isOptional;
		}
	}

	public class ImportedTextColumn extends ImportedColumn {

		public ImportedTextColumn(String name) {
			super(name);
		}

		public String getText() {
			return reader.getCurrentLine(columnIndex);
		}
	}

	public class ImportedDateColumn extends ImportedColumn {

		private DateFormat dateFormat;

		private String dateFormatString;

		public ImportedDateColumn(String name, DateFormat dateFormat) {
			super(name);
			this.dateFormat = dateFormat;
			this.dateFormatString = "<unknown>";
		}

		public ImportedDateColumn(String name, String dateFormatString) {
			super(name);
			this.dateFormat = new SimpleDateFormat(dateFormatString);
			this.dateFormatString = dateFormatString;
		}

		public Date getDate() throws ImportException {
			if (columnIndex == -1 || reader.getCurrentLine(columnIndex).isEmpty()) {
				return null;
			} else {
				try {
					return dateFormat.parse(reader.getCurrentLine(columnIndex));
				} catch (ParseException e) {
					throw new ImportException(
							MessageFormat.format(
									"A date in format {0} was expected but {1} was found.",
									dateFormatString,
									reader.getCurrentLine(columnIndex)),
									e);
				}
			}
		}
	}

	public class ImportedAmountColumn extends ImportedColumn {

		public ImportedAmountColumn(String name) {
			super(name);
		}

		public Long getAmount() throws ImportException {
			String amountString = reader.getCurrentLine(columnIndex);

			if (amountString.trim().length() == 0) {
				// If amount is blank, return null
				return null;
			}

			/*
			 * If the amount starts with a currency symbol then remove it.
			 * TODO: check that the currency matches.
			 */
			if (amountString.startsWith("£")) {
				amountString = amountString.substring(1);
			}
			if (amountString.startsWith("$")) {
				amountString = amountString.substring(1);
			}

			// remove any commas
			amountString = amountString.replace(",", "");

			boolean negate = amountString.startsWith("-");
			if (negate) {
				amountString = amountString.substring(1);
			}

			try {
				String parts [] = amountString.split("\\.");
				long amount = Long.parseLong(parts[0]) * 100;
				if (parts.length > 1) {
					if (parts[1].length() == 1) {
						parts[1] = parts[1] + "0";
					}
					if (parts[1].length() != 2) {
						throw new ImportException("Unexpected number of digits after point in " + amountString);
					}
					amount += Long.parseLong(parts[1]);
				}

				return negate ? -amount : amount;
			} catch (NumberFormatException e) {
				throw new ImportException("Unexpected currency amount " + reader.getCurrentLine(columnIndex) + " found.");
			}
		}
		
		public long getNonNullAmount() throws ImportException {
			Long amount = getAmount();
			return amount == null ? 0 : amount;
		}
	}

	public class ImportedNumberColumn extends ImportedColumn {

		public ImportedNumberColumn(String name) {
			super(name);
		}

		public long getAmount() throws ImportException {
			String numberString = reader.getCurrentLine(columnIndex);

			if (numberString.trim().length() == 0) {
				// Amount cannot be blank
				throw new ImportException("Number cannot be blank.");
			}

			// remove any commas
			numberString = numberString.replace(",", "");

			return Long.parseLong(numberString);
		}
	}

	protected abstract String getDescription();

	protected abstract void startImport(TransactionManagerForAccounts transactionManager) throws ImportException;

	protected abstract void importLine(CsvTransactionReader reader) throws ImportException;

	protected abstract ImportedColumn[] getExpectedColumns();
}