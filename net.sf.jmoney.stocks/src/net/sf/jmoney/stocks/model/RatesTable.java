/*
 * Created on Oct 31, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sf.jmoney.stocks.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class contains a table of rates that can be used to calculate
 * commissions, taxes etc.
 * <P>
 * This is an immutable class.  If you want to modify a rates table,
 * you must create a new rates table and set the new table as the
 * property value.  (If you were to be allowed to modify a rates table
 * then there would be no easy means of ensuring listeners are notified
 * of the changes).
 * 
 * @author Nigel Westbury
 */
public class RatesTable {
	/*
	 * An immutable class containing details of a single band in a rates
	 * table.
	 */
	static class Band {
		private long bandStart;
		private BigDecimal proportion;

		public Band(long bandStart, BigDecimal proportion) {
			this.bandStart = bandStart;
			this.proportion = proportion;
		}

		public long getBandStart() {
			return bandStart;
		}

		public BigDecimal getProportion() {
			return proportion;
		}
	}
	
	private final long fixedAmount;
	
	private final List<Band> bands;

	/**
	 * the string value that was used to construct this rates table
	 * (cannot be null as a null string value equates to a null RatesTable)
	 */
	private final String stringValue;
	
	/*
	 * Default constructor constructs a rates table that returns zero
	 * for all amounts.
	 */
	public RatesTable() {
		this(0, Collections.singletonList(new Band(0, BigDecimal.ZERO)));
	}
	
	/**
	 * Construct a rates table from a String object. All classes created by
	 * plug-ins to contain JMoney properties must have both a toString() method
	 * and a constructor from String that re-constructs an object from the
	 * string.
	 */
	public RatesTable(String stringValue) {
		/*
		 * We save the original string value so if we convert to a string and we
		 * have not made any changes then we return exactly the same string.
		 * This is really important because otherwise database layers can't get
		 * the original value to check for conflicting updates.
		 */
		this.stringValue = stringValue;
		
		String[] numbers = stringValue.split(";");
        
        fixedAmount = new Long(numbers[0]).longValue();

		bands = new ArrayList<Band>();

        int i = 1;
        while (i < numbers.length) {
        	if (!stringValue.equals("0;0;0.0")) {
        		System.out.println("");
        	}
        	long bandStart = new Long(numbers[i++]).longValue();
        	BigDecimal proportion = new BigDecimal(numbers[i++]);
        	Band band = new Band(bandStart, proportion);
        	bands.add(band);
        }
	}
	
	public RatesTable(long fixedAmount, List<Band> bands) {
		this.fixedAmount = fixedAmount;
		this.bands = bands;
		
		String result = new Long(fixedAmount).toString();
		for (int i = 0; i < bands.size(); i++) {
			Band band = bands.get(i);
			result += ";" + band.bandStart;
			result += ";" + band.proportion;
		}
		stringValue = result;
	}

	@Override
	public String toString() {
		return stringValue;
	}
	
	public long calculateRate(long amount) {
		BigDecimal total = new BigDecimal(fixedAmount).movePointLeft(2);
		int i = 1;
		while (i < bands.size() && amount > bands.get(i).getBandStart()) {
			BigDecimal bandRange = new BigDecimal(bands.get(i).getBandStart() - bands.get(i-1).getBandStart()).movePointLeft(2);
			total = total.add(bandRange.multiply(bands.get(i-1).getProportion()));
			i++;
		}
		
		BigDecimal partialBandRange = new BigDecimal(amount - bands.get(i-1).getBandStart()).movePointLeft(2);
		total = total.add(partialBandRange.multiply(bands.get(i-1).getProportion()));

		BigDecimal total2 = total.movePointRight(2).setScale(0, BigDecimal.ROUND_HALF_UP);
		return total2.longValue();
	}
	
	public long getFixedAmount() {
		return fixedAmount;
	}
	
	public List<Band> getBands() {
		return Collections.unmodifiableList(bands);
	}
}
