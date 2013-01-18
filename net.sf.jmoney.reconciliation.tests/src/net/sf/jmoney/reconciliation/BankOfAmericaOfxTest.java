/**
 * 
 */
package net.sf.jmoney.reconciliation;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.Collection;

import junit.framework.TestCase;

/**
 * @author Roel De Meester
 * 
 */
public class BankOfAmericaOfxTest extends TestCase {

	private OfxImport ofxImport;

	private BufferedReader bufferedReader;

	@Override
	public void setUp() throws Exception {
		ofxImport = new OfxImport();
		bufferedReader = new BufferedReader(new InputStreamReader(this
				.getClass().getResourceAsStream("/BankOfAmerica.ofx")));
	}

	/**
	 * Test method for
	 * {@link net.sf.jmoney.reconciliation.OfxImport#getEntries(java.io.BufferedReader)}.
	 * 
	 * @throws ParseException
	 */
	public void testGetEntriesBufferedReader() {
		Collection<EntryData> entries = ofxImport.getEntries(bufferedReader, null, null);
		assertTrue(entries.size() > 0);
		assertEquals(7, entries.size());
		for (EntryData data: entries) {
			System.out.println(data);
		}
	}
}
