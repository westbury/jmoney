package net.sf.jmoney.importer.wizards;

import java.io.IOException;
import java.io.Reader;

import au.com.bytecode.opencsv.CSVReader;
import net.sf.jmoney.importer.wizards.CsvImportWizard.ImportedColumn;

public class CsvTransactionReader {

	/**
	 * The line currently being processed by this wizard, being valid only while the import is
	 * processing after the 'finish' button is pressed
	 */
	protected String [] currentLine;

	protected CSVReader reader;

	protected int rowNumber;

	/*
	 * Set when column headers are matched to expected columns.  This is the largest column
	 * index of all the columns that match to an expected column.
	 */
	private int maximumColumnIndex = -1;

	public CsvTransactionReader(Reader underlyingReader, ImportedColumn[] expectedColumns) throws IOException, ImportException {
		reader = new CSVReader(underlyingReader);
    	rowNumber = 0;
    	
    	String[] headerRow = readHeaderRow();
    	
    	// HACK
    	// Remove the BOM (Byte Ordering Mark) that is output by Toshl
    	if (headerRow[0].startsWith("﻿\"")) {
    		headerRow[0] = headerRow[0].substring(4);
    	}
    	
		/*
		 * Get the list of expected columns, validate the header row, and set the column indexes
		 * into the column objects.
		 * 
		 * We trim the text in the header.  This is helpful because some banks add spaces.  For example
		 * Paypal puts a space before the text in each header cell.
		 */
		
		for (int columnIndex = 0; columnIndex < expectedColumns.length; columnIndex++) {
			if (expectedColumns[columnIndex] != null) {
				int actualColumnIndex = -1;
				for (int columnIndex2 = 0; columnIndex2 < headerRow.length; columnIndex2++) {
					if (headerRow[columnIndex2].trim().equals(expectedColumns[columnIndex].getName())) {
						actualColumnIndex = columnIndex2; 
					}
				}
				if (actualColumnIndex == -1) {
					if (!expectedColumns[columnIndex].isOptional()) {
						throw new ImportException("Expected '" + expectedColumns[columnIndex].getName()
								+ "' in row 1, column " + (columnIndex+1) + " but found '" + headerRow[columnIndex] + "'.");
					}
				}
					expectedColumns[columnIndex].setColumnIndex(actualColumnIndex);
					
					if (maximumColumnIndex < actualColumnIndex) {
						maximumColumnIndex = actualColumnIndex;
					}
						
			}
		}


    	// The constructor should position the cursor on the first row
		readNext();
	}

	protected String[] readHeaderRow() throws IOException {
		return reader.readNext();
	}
	
	protected String[] readNextLine() throws IOException {
		rowNumber++;
		return reader.readNext();
	}

	/**
	 * This method gets the next row.  It is not normally called
	 * by derived classes because this class reads each row.  However
	 * it is available in case derived classes do need to advance
	 * the row for whatever reason.
	 *
	 * @return
	 * @throws IOException
	 */
	protected void readNext() throws IOException {
		do {
			currentLine = readNextLine();

			if (currentLine == null) {
				return;
			}

			/*
			 * If it contains a single empty string then we ignore this line but we don't terminate.
			 * Nationwide Building Society puts such a line after the header.
			 */
			if (currentLine.length == 1 && currentLine[0].isEmpty()) {
				continue;
			}

			// Ameritrade contains this.
			if (currentLine.length == 1 && currentLine[0].equals("***END OF FILE***")) {
				currentLine = null; // Indicates end-of-file
				currentLine = null;
			}

			/*
			 * There may be extra columns in the file that we ignore, but if there are
			 * fewer columns than expected then we can't import the row.
			 */
			if (currentLine.length <= maximumColumnIndex) {
				continue;
			}

			break;
		} while (true);

	}

	public boolean isEndOfFile() {
		return currentLine == null;
	}

	public String getCurrentLine(int columnIndex) {
		return currentLine[columnIndex];
	}

}
