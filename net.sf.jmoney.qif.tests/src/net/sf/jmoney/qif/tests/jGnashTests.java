package net.sf.jmoney.qif.tests;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URL;

import net.sf.jmoney.qif.parser.QifAccount;
import net.sf.jmoney.qif.parser.QifDateFormat;
import net.sf.jmoney.qif.parser.QifFile;
import net.sf.jmoney.qif.parser.QifSecurity;
import net.sf.jmoney.qif.parser.QifTransaction;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class jGnashTests {

	static protected QifFile qifFile = null;
	
	@BeforeClass
	static public void setUp() throws Exception {
		File file = getTestFile("jgnash.qif");
		qifFile = new QifFile(file, QifDateFormat.UsDateOrder);
	}

	@AfterClass
	static public void tearDown() throws Exception {
	}

	@Test
	public void transactionCount() {
	    assertEquals(0, qifFile.transactions.size()); 
	}

	@Test
	public void accountCount() {
	    assertEquals(4, qifFile.accountList.size()); 
	}

	@Test
	public void categoryCount() {
	    assertEquals(63, qifFile.categories.size()); 
	}

	@Test
	public void accountDetails() {
	    QifAccount account1 = qifFile.accountList.get(0);
	    assertEquals("Checking", account1.getName()); 
	    assertEquals(null, account1.getDescription()); 
	    assertEquals(0, account1.getTransactions().size()); 

	    QifAccount account2 = qifFile.accountList.get(1);
	    assertEquals("Savings", account2.getName()); 
	    assertEquals("3 Rivers Savings", account2.getDescription()); 
	    assertEquals(0, account2.getTransactions().size()); 

	    QifAccount account3 = qifFile.accountList.get(2);
	    assertEquals("Checking", account3.getName()); 
	    assertEquals(1, account3.getTransactions().size()); 

	    QifAccount account4 = qifFile.accountList.get(3);
	    assertEquals("Savings", account4.getName()); 
	    assertEquals(3, account4.getTransactions().size()); 

	    QifTransaction trans = account3.getTransactions().get(0);
	    assertEquals(0.00, trans.getAmount()); 
	    assertEquals(26, trans.getDate().getDay()); 
	    assertEquals(6, trans.getDate().getMonth()); 
	    assertEquals(2001, trans.getDate().getYear()); 

	    QifTransaction trans2 = account4.getTransactions().get(1);
	    assertEquals(400.00, trans2.getAmount()); 
	    assertEquals(26, trans2.getDate().getDay()); 
	    assertEquals(6, trans2.getDate().getMonth()); 
	    assertEquals(2001, trans2.getDate().getYear()); 
	    assertEquals(2, trans2.getSplits().size()); 
	}

	@Test
	public void securities() {
	    assertEquals(3, qifFile.securities.size()); 

	    QifSecurity security1 = qifFile.securities.get(0);
	    assertEquals("Dow Jones Industrials", security1.getName()); 
	    assertEquals(null, security1.getDescription()); 

	    QifSecurity security2 = qifFile.securities.get(1);
	    assertEquals("NASDAQ Composite", security2.getName()); 

	    QifSecurity security3 = qifFile.securities.get(2);
	    assertEquals("S&P 500 Index", security3.getName()); 
	}

    static protected File getTestFile(String filename) {
        URL url = FinanceQIFTests.class.getResource("resources/" + filename);
        return new File(url.getFile());
    }
}
