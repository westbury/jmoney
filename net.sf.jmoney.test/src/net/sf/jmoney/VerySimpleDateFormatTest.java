/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2002 Johann Gyger <johann.gyger@switzerland.org>
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
package net.sf.jmoney;

import java.util.Calendar;
import java.util.Date;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author Alex Zuroff <azuroff@users.sourceforge.net>
 * 
 * A test case that uses a number of different date patterns to test the
 * VerySimpleDateFormat class.
 *  
 */
public class VerySimpleDateFormatTest extends TestCase {

    // The possible actions to test
    private static final int FORMAT = 1;
    private static final int PARSE = 2;
    
	private VerySimpleDateFormat formatter;
    private String pattern;
	private Date dateValue;
	private String dateString;
    private int actionToTest;
    private String testLabel;
    private static int testNumber = 0;
	
	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * Constructor for VerySimpleDateFormatTest.
     * 
	 * @param pattern The pattern to use for the test
	 * @param dateValue The date to use for the test.  If this value is null,
     * the test checks that the parse method throws an IllegalArgumentException,
     * as the dateString being parsed is invalid for some reason.
	 * @param dateString The string representation of the date
	 * @param actionToTest  Tells the TestCase which action to test - formatting
     * the dateValue and comparing the result to dateString, or parsing dateString
     * and comparing the result to dateValue. 
	 */
	public VerySimpleDateFormatTest(String pattern, Date dateValue,
			String dateString, int actionToTest) {
		super("testFormatter");
		formatter = new VerySimpleDateFormat(pattern);
        this.pattern = pattern;
		this.dateValue = dateValue;
		this.dateString = dateString;
        this.actionToTest = actionToTest;
        this.testLabel = "test" + testNumber;
        testNumber++;
	}

	/**
	 * The parameterized test method. It takes parameters indicating the
	 * starting and expected results, and runs formatting and parsing tests on
	 * those parameters.
	 */
	public void testFormatter() {
        switch (actionToTest) {
            case FORMAT:
                testFormatting();
                break;
            case PARSE:
                testParsing();
                break;
            default:
                throw new RuntimeException("Invalid actionToTest value - " + actionToTest);
        }
	}

	/**
	 * Tests that the date is formatted correctly based on the supplied pattern.
	 */
	private void testFormatting() {
        String formatResult = formatter.format(dateValue);
        assertEquals(testLabel + ": Formatting error for pattern - " + pattern, formatResult, dateString);
	}

	/**
	 * Tests that the date is parsed correctly based on the supplied pattern.
	 */
	private void testParsing() {
        if (dateValue != null) {
            Date parseResult = formatter.parse(dateString);
            assertEquals(testLabel + ": Parsing error for pattern - " + pattern, parseResult, dateValue);
        } else {
            try {
                Date result = formatter.parse(dateString);
                fail(testLabel + ": Should have thrown IllegalArgumentException for pattern - " + pattern + ", date string - " + dateString + ", returned value - " + formatter.format(result));   // expectedParseResult was null, so it should have thrown an exception
            } catch (IllegalArgumentException expected) {
                // do nothing - we were expecting this
            }
        }
	}
    
	/**
     * Constructs a suite of tests using the Parameterized Test pattern.
     * Each test is a single instane of the VerySimpleDateFormatTest class, with 
     * different parameters passed to the constructor to test different scenarios.
     * 
	 * @return Test The test suite.
	 */
	public static Test suite() {
        // Here we build our test suite by using different parameters to the constructor.
        // Each instance of VerySimpleDateFormatTest will be a separate test to run.
        TestSuite suite = new TestSuite();

        Calendar cal = Calendar.getInstance();
        int month = cal.get(Calendar.MONTH);
        int year = cal.get(Calendar.YEAR);
        
        // First date to format is 1/1/2005
        Date dateToFormat1 = getDate(2005, Calendar.JANUARY, 1);
        
        // Next date to format is 12/14/2004
        Date dateToFormat2 = getDate(2004, Calendar.DECEMBER, 14);
        
        // First date to parse is 1/1/2005 (both 2 and 4-digit year)
        Date expectedParseResult1 = getDate(2005, Calendar.JANUARY, 1);
        
        // Next date to parse is 12/15/1998 (both 2 and 4-digit year)
        Date expectedParseResult2 = getDate(1998, Calendar.DECEMBER, 15);
        
        // Next date to parse is the 22nd day of the current month and year
        Date expectedParseResult3 = getDate(year, month, 22);
        
        // Next date to parse is March 3rd of the current year
        Date expectedParseResult4 = getDate(year, Calendar.MARCH, 3);
        
        String pattern = "dd.MM.yyyy";
        suite.addTest(new VerySimpleDateFormatTest(pattern, dateToFormat1, "01.01.2005", FORMAT));
        suite.addTest(new VerySimpleDateFormatTest(pattern, dateToFormat2, "14.12.2004", FORMAT));
        suite.addTest(new VerySimpleDateFormatTest(pattern, expectedParseResult1, "1.1.2005", PARSE));
        suite.addTest(new VerySimpleDateFormatTest(pattern, expectedParseResult1, "1.1.05", PARSE));
        suite.addTest(new VerySimpleDateFormatTest(pattern, expectedParseResult1, "01.01.2005", PARSE));
        suite.addTest(new VerySimpleDateFormatTest(pattern, expectedParseResult2, "15.12.1998", PARSE));
        suite.addTest(new VerySimpleDateFormatTest(pattern, expectedParseResult2, "15.12.98", PARSE));
        suite.addTest(new VerySimpleDateFormatTest(pattern, expectedParseResult3, "22", PARSE));
        suite.addTest(new VerySimpleDateFormatTest(pattern, null, "45", PARSE));            // parseable but invalid day
        suite.addTest(new VerySimpleDateFormatTest(pattern, expectedParseResult4, "3.3", PARSE));
        suite.addTest(new VerySimpleDateFormatTest(pattern, null, "22.17", PARSE));         // parseable but invalid month
        suite.addTest(new VerySimpleDateFormatTest(pattern, null, "abcd", PARSE));
        suite.addTest(new VerySimpleDateFormatTest(pattern, null, "50.01.2005", PARSE));    // parseable but invalid day
        suite.addTest(new VerySimpleDateFormatTest(pattern, null, "01.25.2005", PARSE));    // parseable but invalid month
        suite.addTest(new VerySimpleDateFormatTest(pattern, null, "14/12/2005", PARSE));    // invalid separator
        suite.addTest(new VerySimpleDateFormatTest(pattern, null, "14..12.2005", PARSE));    // invalid separator
        
        pattern = "MM/dd/yyyy";
        suite.addTest(new VerySimpleDateFormatTest(pattern, dateToFormat1, "01/01/2005", FORMAT));
        suite.addTest(new VerySimpleDateFormatTest(pattern, dateToFormat2, "12/14/2004", FORMAT));
        suite.addTest(new VerySimpleDateFormatTest(pattern, expectedParseResult1, "1/1/2005", PARSE));
        suite.addTest(new VerySimpleDateFormatTest(pattern, expectedParseResult1, "1/1/05", PARSE));
        suite.addTest(new VerySimpleDateFormatTest(pattern, expectedParseResult1, "01/01/2005", PARSE));
        suite.addTest(new VerySimpleDateFormatTest(pattern, expectedParseResult2, "12/15/1998", PARSE));
        suite.addTest(new VerySimpleDateFormatTest(pattern, expectedParseResult2, "12/15/98", PARSE));
        suite.addTest(new VerySimpleDateFormatTest(pattern, expectedParseResult3, "22", PARSE));
        suite.addTest(new VerySimpleDateFormatTest(pattern, null, "45", PARSE));            // parseable but invalid day
        suite.addTest(new VerySimpleDateFormatTest(pattern, expectedParseResult4, "3/3", PARSE));
        suite.addTest(new VerySimpleDateFormatTest(pattern, null, "17/22", PARSE));         // parseable but invalid month
        suite.addTest(new VerySimpleDateFormatTest(pattern, null, "abcd", PARSE));
        suite.addTest(new VerySimpleDateFormatTest(pattern, null, "01/50/2005", PARSE));    // parseable but invalid day
        suite.addTest(new VerySimpleDateFormatTest(pattern, null, "25/01/2005", PARSE));    // parseable but invalid month
        suite.addTest(new VerySimpleDateFormatTest(pattern, null, "14.12.2005", PARSE));    // invalid separator

        return suite;
    }

    /**
     * Returns a Date object containing the specified year, month, and day.
     * 
     * @param year The year
     * @param month The month
     * @param day The day
     * @return Date The date containing the given year, month, and day
     */
    private static Date getDate(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(year, month, day);
        return cal.getTime();
    }
}