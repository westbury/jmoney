/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (C) 2001-2008 Craig Cavanaugh, Johann Gyger, and others
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
 *  Foundation, 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package net.sf.jmoney.qif.parser;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;



public class QifFile {

	/**
	 * Indicates whether the QIF file uses the mm/dd/yyyy or dd/mm/yyyy format.
	 * This field will always be left set to either the US or the EU format
	 * (it will never be left set to DetermineFromFile).
	 */
	private QifDateFormat dateFormat;

	@SuppressWarnings("unused")
	private boolean autoSwitch = false;
	
	private File file;

    public List<QifCategory> categories = new ArrayList<QifCategory>();

    public List<QifClassItem> classes = new ArrayList<QifClassItem>();

    public List<QifAccount> accountList = new ArrayList<QifAccount>();

    public List<QifTransaction> transactions = new ArrayList<QifTransaction>();

    public List<QifInvstTransaction> invstTransactions = new ArrayList<QifInvstTransaction>();

    public List<QifSecurity> securities = new ArrayList<QifSecurity>();

    public List<QifSecurityPrices> securityPrices = new ArrayList<QifSecurityPrices>();

	private List<QifMemorized> memorized = new ArrayList<QifMemorized>();

	// Fields used to guess date format from date order
	private int lastDateIfUS = 0;
	private int lastDateIfEU = 0;
	private boolean usDatesInOrder = true;
	private boolean euDatesInOrder = true;
	
	public QifFile(File file, QifDateFormat dateFormat) throws InvalidQifFileException, AmbiguousDateException, IOException {
		this.file = file;

// TODO: What if this is just a list of transactions,
	//	no account so we must create one from the name.
/*	
 * 		// Assume we are adding a new Bank Account and that the
		// first transaction contains the opening balance and
		// the name of the account under the category.
		String accountName = qifFile.getFileName();
		long startBalance = 0;
		Currency currency = session.getDefaultCurrency();

		for (QifTransaction transaction : qifFile.getTransactions()) {
			Iterator<QifTransaction> iterator = qifFile.iterator();

			BufferedReader buffer = new BufferedReader(new FileReader(qifFile));
			String line = buffer.readLine();
			assert (line.startsWith("!Type:"));

			do {
				line = buffer.readLine();
				switch (line.charAt(0)) {
				case QIFCashTransaction.TOTAL:
					startBalance = parseAmount(line, currency);
					break;
				case QIFCashTransaction.CATEGORY:
					// Assume line is formated as "L[Account Name]"
					accountName = line.substring(2, line.length() - 1);
					break;
				default:
					break;
				}
			} while (line.charAt(0) != QIFCashTransaction.END);
	
		*/

		
			this.dateFormat = dateFormat;

			FileReader reader = null;
			QifReader in = null;
			try {
				reader = new FileReader(file);
				in = new QifReader(reader);
				String line = in.readLine();
				while (line != null) {
					if (startsWith(line, "!Type:Class")) {
						parseClassList(in);
					} else if (startsWith(line, "!Type:Cat")) {
						parseCategoryList(in);
					} else if (startsWith(line, "!Account")) {
						parseAccounts(in);
					} else if (startsWith(line, "!Type:Memorized")) {
						parseMemorizedTransactions(in);
					} else if (startsWith(line, "!Type:Security")) {
						parseSecurities(in);
					} else if (startsWith(line, "!Type:Prices")) {
						parseSecuritiesPrices(in);
					} else if (startsWith(line, "!Type:Bank")) { // QIF from an online bank statement... assumes the account is known
						parseAccountTransactions(in);
					} else if (startsWith(line, "!Type:CCard")) { // QIF from an online credit card statement
						parseAccountTransactions(in);
					} else if (startsWith(line, "!Type:Oth")) { // QIF from an online credit card statement
						parseAccountTransactions(in);
					} else if (startsWith(line, "!Type:Cash")) { // Partial QIF export
						parseAccountTransactions(in);
					} else if (startsWith(line, "!Type:Invst")) { // investment transactions follow
						parseInvestmentAccountTransactions(in);
					} else if (startsWith(line, "!Type:")) { // unknown type - ignore
						parseUnknownType(in);
					} else if (startsWith(line, "!Option:AutoSwitch")) {
						autoSwitch = true;
					} else if (startsWith(line, "!Clear:AutoSwitch")) {
						autoSwitch = false;
					} else {
						throw new InvalidQifFileException("Unexpected data: " + line, in);
					}
					line = in.readLine();
				}
			} finally {
				if (in != null) in.close();
				if (reader != null) reader.close();
			}
			
			/*
			 * No month or day above 12, but if the dates are in order only when
			 * interpreted as EU or in order only when interpreted as US then assume the date order
			 * that puts the dates in order.
			 */
			if (this.dateFormat == QifDateFormat.DetermineFromFile || dateFormat == QifDateFormat.DetermineFromFileAndSystem) {
				if (usDatesInOrder && !euDatesInOrder) {
					this.dateFormat = QifDateFormat.UsDateOrder;
				}

				if (euDatesInOrder && !usDatesInOrder) {
					this.dateFormat = QifDateFormat.EuDateOrder;
				}
			}

			// TODO better than looking to the locale, get the currency of the account
			// and find a locale that uses that currency.  I don't know if we can use
			// the Java currency and locale data or if we need to just hard-code a few
			// currencies and date formats.
			
			if (this.dateFormat == QifDateFormat.DetermineFromFileAndSystem) {
				// The file contains dates but none have a day more
				// than 12.  So look to the locale.

				// Is there a more direct way of getting the date order
				// from the default locale?

				DateFormat localFormatter = DateFormat.getDateInstance(DateFormat.SHORT);
				Calendar calendar = Calendar.getInstance(localFormatter.getTimeZone());
				calendar.clear();
				calendar.set(2008, Calendar.NOVEMBER, 23);
				String formatted = localFormatter.format(calendar.getTime());

				QifDateFormat qifDateFormat;
				if (formatted.startsWith("11")) {
					// Month first
					this.dateFormat = QifDateFormat.UsDateOrder;
				} else if (formatted.startsWith("23")) {
					// Day first
					this.dateFormat = QifDateFormat.EuDateOrder;
				} else {
					// Some other order, Asian perhaps?
					throw new AmbiguousDateException();
				}
			}
			
			/*
			 * If we have not by now determined the date order then we throw
			 * an exception.
			 */
			if (this.dateFormat == QifDateFormat.DetermineFromFile) {
				throw new AmbiguousDateException();
			}
	}

    /** Tests if the source string starts with the prefix string. Case is
     * ignored.
     *
     * @param source  the source String.
     * @param prefix  the prefix String.
     *
     * @return true, if the source starts with the prefix string.
     */
    static boolean startsWith(final String source, final String prefix) {
        if (prefix.length() > source.length()) {
            return false;
        }
        return source.regionMatches(true, 0, prefix, 0, prefix.length());
    }


    enum QifType {
		cash(true),
		bank(true),
		creditcard(true),
		other(true),
		investment(true);

		private boolean isAccountTransactions;
		
		private QifType(boolean isAccountTransactions) {
			this.isAccountTransactions = isAccountTransactions;
		}

		public boolean isAccountTransactions() {
			return isAccountTransactions;
		}
	}
    
    static String getTypeString(String line, QifReader in) {
    	assert(startsWith(line, "!Type:"));
    	return line.substring(6).trim();
    }

    static QifType getType(String line, QifReader in) throws InvalidQifFileException {
    	assert(startsWith(line, "!Type:"));
    	String type = line.substring(6).trim();
    	if (type.equals("Cash")) {
    		return QifType.cash;
    	}
		if (type.equals("Bank")) {
			return QifType.bank;
		}
		if (type.equals("CCard")) {
			return QifType.creditcard;
		}
		if (type.startsWith("Oth")) {
			return QifType.other;
		}
		if (type.startsWith("Invst")) {
			return QifType.investment;
		}
		throw new InvalidQifFileException("Unknown type: " + line, in);
	}

	/**
	 * Called when !Account is found.
	 * A list of accounts is expected to follow.
	 * @throws IOException 
	 * @throws InvalidQifFileException 
	 */
    private void parseAccounts(QifReader in) throws IOException, InvalidQifFileException {
    	do {
    		QifAccount account = QifAccount.parseAccount(in, this);
    		accountList.add(account);
    	} while (in.peekLine() != null && !in.peekLine().startsWith("!"));
    }

    // TODO strip out investment account transaction checks
    private void parseAccountTransactions(QifReader in) throws IOException, InvalidQifFileException {
    	do {
    		QifTransaction transaction = QifTransaction.parseTransaction(in, this);
    		transactions.add(transaction);
    	} while (in.peekLine() != null && !in.peekLine().startsWith("!"));
    }

    private void parseInvestmentAccountTransactions(QifReader in) throws IOException, InvalidQifFileException {
    	do {
    		QifInvstTransaction transaction = QifInvstTransaction.parseTransaction(in, this);
    		invstTransactions.add(transaction);
    	} while (in.peekLine() != null && !in.peekLine().startsWith("!"));
    }

    /**
     * just eat the data, it's not useful right now
     * @throws IOException 
     * @throws InvalidQifFileException 
     */
    private void parseMemorizedTransactions(QifReader in) throws IOException, InvalidQifFileException {
    	do {
    		QifMemorized transaction = QifMemorized.parseMemorized(in, this);
    		memorized .add(transaction);
    	} while (in.peekLine() != null && !in.peekLine().startsWith("!"));
    }

    private void parseCategoryList(QifReader in) throws IOException, InvalidQifFileException {
    	do {
    		QifCategory category = QifCategory.parseCategory(in);
    		categories.add(category);
    	} while (in.peekLine() != null && !in.peekLine().startsWith("!"));
    }

    private void parseClassList(QifReader in) throws IOException, InvalidQifFileException {
    	do {
    		QifClassItem category = QifClassItem.parseClass(in);
    		classes.add(category);
    	} while (in.peekLine() != null && !in.peekLine().startsWith("!"));
    }

    /**
     * So far, I haven't see a security as part of a list, but it is supported
     * just in case there is another "variation" of the format
     * @throws IOException 
     * @throws InvalidQifFileException 
     */
    private void parseSecurities(QifReader in) throws IOException, InvalidQifFileException {
    	do {
    		QifSecurity security = QifSecurity.parseSecurity(in);
    		securities .add(security);
    	} while (in.peekLine() != null && !in.peekLine().startsWith("!"));
    }

    private void parseSecuritiesPrices(QifReader in) throws IOException, InvalidQifFileException {
    	do {
    		QifSecurityPrices security = QifSecurityPrices.parseSecurityPrices(in);
    		securityPrices.add(security);
    	} while (in.peekLine() != null && !in.peekLine().startsWith("!"));
    }

    /**
     * If !type: is found but the type is unknown then ignore all
     * data until the next line that starts with '!'.
     */
    private void parseUnknownType(QifReader in) throws IOException {
    	while (in.peekLine() != null && !in.peekLine().startsWith("!")) {
    		in.readLine();
    	}
    }

	/**
	 * Processes a date string to determine whether the date format is US or
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
	 * This method fully checks the date given the existing date format.
	 * That is, if dateFormat is set to a specific format then all dates
	 * will have been checked to be valid in that format.  If the format
	 * cannot yet be determined then this date must be valid in both formats.
	 * This ensures that, once a format is determined, all dates are known to
	 * be valid in that format.
	 * 
	 * @param in 
	 *
	 * @throws InvalidQifFileException 
	 */
	void processDate(String dateString, QifReader in) throws InvalidQifFileException {
		StringTokenizer st = new StringTokenizer(dateString, "/\'");
		int number1 = Integer.parseInt(st.nextToken().trim());
		if (number1 > 12) {
			if (dateFormat == QifDateFormat.UsDateOrder) {
				throw new InvalidQifFileException("Dates exist in both US and EU formats.", in); 
			}
			dateFormat = QifDateFormat.EuDateOrder;
		}
		int number2 = Integer.parseInt(st.nextToken().trim());
		if (number2 > 12) {
			if (dateFormat == QifDateFormat.EuDateOrder) {
				throw new InvalidQifFileException("Dates exist in both US and EU formats.", in); 
			}
			dateFormat = QifDateFormat.UsDateOrder;
		}

		/*
		 * If we don't yet know the date order because of the above tests then
		 * we keep track of date ordering (would the dates be in order if we
		 * assumed US date format? Would they be in order if we assumed EU
		 * format?
		 */
		if (dateFormat == QifDateFormat.DetermineFromFile || dateFormat == QifDateFormat.DetermineFromFileAndSystem) {
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
	}

	public String getFileName() {
		return file.getName();
	}

    /**
     * Converts a string into a data object <p>
     *
     * format "6/21' 1"  -> 6/21/2001
     * format "6/21'01"  -> 6/21/2001
     * format "9/18'2001 -> 9/18/2001
     * format "06/21/2001"
     * format "06/21/01"
     * format "3.26.03" -> German version of quicken
     * format "03-26-2005" -> MSMoney format
     * format "1.1.2005" -> kmymoney2
     * 20.1.94 European dd/mm/yyyy has been confirmed
     * 
     * 21/2/07 -> 02/21/2007 UK, Quicken 2007
     * D15/2/07
     * 
     * @param sDate String QIF date to parse
     * @return Returns parsed date
     */
    public QifDate parseDate(String sDate) {                               
    	int month = 0;
    	int day = 0;
    	int year = 0;

    	String[] chunks = sDate.split("/|'|\\.|-");

    	try {
    		switch (dateFormat) {
    		case UsDateOrder:
    			month = Integer.parseInt(chunks[0].trim());
    			day = Integer.parseInt(chunks[1].trim());
    			year = Integer.parseInt(chunks[2].trim());
    			break;
    		case EuDateOrder:
    			day = Integer.parseInt(chunks[0].trim());
    			month = Integer.parseInt(chunks[1].trim());
    			year = Integer.parseInt(chunks[2].trim());
    			break;
    		case DetermineFromFile:
    			throw new RuntimeException("should not happen");
    		case DetermineFromFileAndSystem:
    			throw new RuntimeException("should not happen");
    		}
    	} catch (NumberFormatException e) {
			throw new RuntimeException("should not happen because dates checked on first pass in constructor");
    	}

    	if (year < 100) {
    		if (year < 29) {
    			year += 2000;
    		} else {
    			year += 1900;
    		}
    	}

    	return new QifDate(year, month, day);
    }

    public static BigDecimal parseMoney(final String money) {
        String sMoney = money;

        if (sMoney != null) {
            BigDecimal bdMoney;
            sMoney = sMoney.trim(); // to be safe
            try {
                bdMoney = new BigDecimal(sMoney);
                return bdMoney;
            } catch (NumberFormatException e) {
                /* there must be commas, etc in the number.  Need to look for them
                 * and remove them first, and then try BigDecimal again.  If that
                 * fails, then give up and use NumberFormat and scale it down 
                 * */

                String[] split = sMoney.split("\\D");
                if (split.length > 2) {
                    StringBuffer buf = new StringBuffer();
                    if (sMoney.startsWith("-")) {
                        buf.append("-");
                    }
                    for (int i = 0; i < split.length - 1; i++) {
                        buf.append(split[i]);
                    }
                    buf.append('.');
                    buf.append(split[split.length - 1]);
                    try {
                        bdMoney = new BigDecimal(buf.toString());
                        return bdMoney;
                    } catch (NumberFormatException e2) {
                        Logger l = Logger.getAnonymousLogger();
                        l.info("second parse attempt failed");
                        l.info(buf.toString());
                        l.info("falling back to rounding");
                    }
                }
                NumberFormat formatter = NumberFormat.getNumberInstance();
                try {
                    Number num = formatter.parse(sMoney);
                    BigDecimal bd = new BigDecimal(num.floatValue());
                    if (bd.scale() > 6) {
                        Logger l = Logger.getAnonymousLogger();
                        l.warning("-Warning-");
                        l.warning("Large scale detected in QifUtils.parseMoney");
                        l.warning("Truncating scale to 2 places");
                        l.warning(bd.toString());
                        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
                        l.warning(bd.toString());
                    }
                    return bd;
                } catch (ParseException pe) {
                }
                Logger.getAnonymousLogger().severe("could not parse money: " + sMoney);
            }
        }
        return new BigDecimal("0");
    }

	public static int parseInteger(String value, String line, QifReader in) throws InvalidQifFileException {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw new InvalidQifFileException("Integer value was expected: " + line, in);
		}
	}
}
