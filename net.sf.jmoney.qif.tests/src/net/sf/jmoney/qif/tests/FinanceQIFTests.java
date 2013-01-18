package net.sf.jmoney.qif.tests;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URL;

import net.sf.jmoney.qif.parser.QifAccount;
import net.sf.jmoney.qif.parser.QifDateFormat;
import net.sf.jmoney.qif.parser.QifFile;
import net.sf.jmoney.qif.parser.QifInvstTransaction;
import net.sf.jmoney.qif.parser.QifSecurity;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class FinanceQIFTests {

	static QifFile qifFile = null;
	
	@BeforeClass
	static public void setUp() throws Exception {
		File file = getTestFile("Finance-QIF.qif");
		qifFile = new QifFile(file, QifDateFormat.UsDateOrder);
	}

	@AfterClass
	static public void tearDown() throws Exception {
	}

	@Test
	public void transactionCount() {
	    assertEquals(0, qifFile.transactions.size()); 
	}

    static protected File getTestFile(String filename) {
        URL url = FinanceQIFTests.class.getResource("resources/" + filename);
        return new File(url.getFile());
    }

	@Test
	public void accounts() {
		// Each account occurs twice.
		// Should the parser combine these?
	    assertEquals(14, qifFile.accountList.size());
	    
	    QifAccount invstAccount = qifFile.accountList.get(12);
	    assertEquals("Mutual Fund", invstAccount.getName()); 
	    assertEquals(0, invstAccount.getTransactions().size()); 
	    assertEquals(1, invstAccount.getInvstTransactions().size());
	    
	    QifInvstTransaction trans = invstAccount.getInvstTransactions().get(0);
	    assertEquals(2006, trans.getDate().getYear()); 
	    assertEquals(1, trans.getDate().getMonth()); 
	    assertEquals(10, trans.getDate().getDay()); 
	    assertEquals("Mutual Fund", trans.getSecurity()); 
	    assertEquals("33", trans.getQuantity()); 
	}

	@Test
	public void securities() {
	    assertEquals(2, qifFile.securities.size());
	    
	    QifSecurity security = qifFile.securities.get(0);
	    assertEquals("Intuit", security.getName()); 
	    assertEquals("INTU", security.getSymbol()); 
	    assertEquals("Stock", security.getType()); 
	    assertEquals("High Risk", security.getGoal());
	}
}
