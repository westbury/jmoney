/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2004 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.reconciliation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Vector;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.importer.matcher.EntryData;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.CurrencyAccount;
import net.sf.jmoney.reconciliation.reconcilePage.ImportStatementDialog;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

/**
 * Provides an implementation of the net.sf.jmoney.reconciliation.bankstatements
 * extension point.  This extension supports the import of Quicken Import Files
 * (QIF files).
 * <P>
 * Note that this is different from the QIF import provided by the
 * net.sf.jmoney.qif plug-in.  That plug-in is designed to import QIF
 * files that have been imported from another accounting package.  Those
 * QIF files may contain category information, split entries and various
 * other stuff.  This implementation imports only QIF files that have been
 * exported by online banking sites.  Those QIF files will contain only
 * a single entry per transaction, no categories etc.  The data are imported
 * into the reconciliation page where the user has the ability to either edit
 * the entries manually (e.g. set the category) or drag an existing entry onto
 * this entry so to merge the two entries.
 * 
 * @author Nigel Westbury
 */
public class QifImport implements IBankStatementSource {
	
	private static NumberFormat number = NumberFormat.getInstance(Locale.US);
	
	private Calendar calendar = Calendar.getInstance();
	
	/**
	 * Indicates whether the QIF file uses the mm/dd/yyyy or dd/mm/yyyy format.
	 * Set by the <code>isUSDateFormat</code> method
	 */
	private boolean usesUSDates;
	
	public Collection<EntryData> importEntries(Shell shell, CurrencyAccount account, Date defaultStartDate, Date defaultEndDate) {
		FileDialog dialog = new FileDialog(shell);
		dialog.setFilterExtensions(new String [] { "*.qif" } );
		dialog.setFilterNames(new String [] { "Quicken Import Files (*.qif)" } );
		String fileName = dialog.open();
		
		if (fileName == null) {
			return null;
		}
		
		File qifFile = new File(fileName);
		Vector<EntryData> entries = new Vector<EntryData>();
		
		ImportStatementDialog dialog2 = new ImportStatementDialog(shell, defaultStartDate, defaultEndDate, null);
		if (dialog2.open() != Dialog.OK) {
			return null;
		}
		Date startDate = dialog2.getStartDate();
		Date endDate = dialog2.getEndDate();
		
		Reader reader = null;
		BufferedReader buffer = null;
		try {
			reader = new FileReader(qifFile);
			buffer = new BufferedReader(reader);
			
			usesUSDates = isUSDateFormat(qifFile);
			
			String line = buffer.readLine();
			if (!line.startsWith("!Type:Bank")
					&& !line.startsWith("!Type:CCard")
					&& !line.startsWith("!Type:Invst")) {
				JMoneyPlugin.log(new RuntimeException("Cannot import " + line.substring(0)));
			}

			Date entryDate = null;

			line = buffer.readLine();
			// Bank of America outputs empty lines at the end
			while (line != null && line.length() >= 1) {
				EntryData entryData = new EntryData();
				
				do {
					// Bank of America outputs empty lines at the end
					if (line.length() >= 1) {
						switch (line.charAt(0)) {
						case 'T':
							entryData.setAmount(parseAmount(line, account.getCurrency()));
							break;
						case 'D':
							entryDate = parseDate(line);
							entryData.setClearedDate(entryDate);
							break;
						case 'N':
							entryData.setCheck(line.substring(1));
							break;
						case 'P':
							entryData.setPayee(line.substring(1));
							break;
						case 'M':
							entryData.setMemo(line.substring(1));
							break;
						default:
							break;
						}
					}
					
					line = buffer.readLine();
				} while (line.charAt(0) != '^');

				if (entryDate != null
				 && (startDate == null
			  	   || entryDate.compareTo(startDate) >= 0)
				  && (endDate == null
				   || entryDate.compareTo(endDate) <= 0)) {
					entries.add(entryData);
				}
				
				line = buffer.readLine();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (buffer != null) {
					buffer.close();
				}
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return entries;
	}
	
	/**
	 * @param line
	 * @param currency
	 * @return
	 */
	private long parseAmount(String line, Currency currency) {
		short factor = currency.getScaleFactor();
		Number n = number.parse(line, new ParsePosition(1));
		return n == null ? 0 : Math.round(n.doubleValue() * factor);
	}
	
	/**
	 * Parses the date string and returns a date object: <br>
	 * 11/2/98 ->> 11/2/1998 <br>
	 * 3/15'00 ->> 3/15/2000
	 */
	private Date parseDate(String line) {
		try {
			StringTokenizer st = new StringTokenizer(line, "D/\'-");
			int day, month, year;
			if (usesUSDates) {
				month = Integer.parseInt(st.nextToken().trim());
				day = Integer.parseInt(st.nextToken().trim());
			} else {
				day = Integer.parseInt(st.nextToken().trim());
				month = Integer.parseInt(st.nextToken().trim());
			}
			year = Integer.parseInt(st.nextToken().trim());
			if (year < 100) {
				if (line.indexOf("'") < 0)
					year = year + 1900;
				else
					year = year + 2000;
			}
			calendar.clear();
			calendar.setLenient(false);
			calendar.set(year, month - 1, day, 0, 0, 0);
			return calendar.getTime();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Scans a QIF file to determine whether the date format is US or
	 * EU ie: mm/dd/yyyy or dd/mm/yyyy
	 * <P>
	 * If a number is found in the day or month field that is
	 * greater than 12 then we know immediately the date format.
	 * <P>
	 * If we get to the end of the file with no such number greater
	 * than 12 then we assume the dates are in order and return the
	 * format that would put the dates in order.
	 * <P>
	 * If both formats put the dates in order then we give up
	 * and assume US format.  This scenario would be extremely
	 * unlikely.
	 * 
	 * @param qifFile
	 *            the file to scan
	 * @return
	 * @throws IOException
	 */
	private boolean isUSDateFormat(File file) throws IOException {
		String line;
		
		int lastDateIfUS = 0;
		int lastDateIfEU = 0;
		boolean usDatesInOrder = true;
		boolean euDatesInOrder = true;
		
		Reader reader = null;
		BufferedReader buffer = null;
		try {
			reader = new FileReader(file);
			buffer = new BufferedReader(reader);
			
			line = buffer.readLine();
			while (line != null) {
				if (line.charAt(0) == 'D') {
					StringTokenizer st = new StringTokenizer(line, "D/\'-");
					int number1 = Integer.parseInt(st.nextToken().trim());
					if (number1 > 12) {
						return false;
					}
					int number2 = Integer.parseInt(st.nextToken().trim());
					if (number2 > 12) {
						return true;
					}
					
					int year = Integer.parseInt(st.nextToken().trim());
					
					int dateIfUS = number1*100 + number2 + year*10000;
					if (dateIfUS < lastDateIfUS) {
						usDatesInOrder = false;
					}
					lastDateIfUS = dateIfUS;
					
					int dateIfEU = number1*100 + number2 + year*10000;
					if (dateIfEU < lastDateIfEU) {
						euDatesInOrder = false;
					}
					lastDateIfEU = dateIfEU;
				}

				line = buffer.readLine();
			}
		} finally {
			if (buffer != null) {
				buffer.close();
			}
			if (reader != null) {
				reader.close();
			}
		}
		
		// No month or day above 12, but if the dates are in
		// order only when we assume EU format then indicate
		// EU format, otherwise assume US format.
		return (usDatesInOrder || !euDatesInOrder);
	}
}
