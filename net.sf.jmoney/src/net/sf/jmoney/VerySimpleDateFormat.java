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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.StringTokenizer;

public class VerySimpleDateFormat {

	public static final String[] DATE_PATTERNS =
		{
			"dd.MM.yyyy", //$NON-NLS-1$
			"dd/MM/yyyy", //$NON-NLS-1$
			"dd-MM-yyyy", //$NON-NLS-1$
			"MM/dd/yyyy", //$NON-NLS-1$
			"MM-dd-yyyy", //$NON-NLS-1$
			"yyyy.dd.MM", //$NON-NLS-1$
			"yyyy.MM.dd", //$NON-NLS-1$
			"yyyy.MM.dd.", //$NON-NLS-1$
			"yyyy/MM/dd", //$NON-NLS-1$
			"yyyy-MM-dd" }; //$NON-NLS-1$

	private SimpleDateFormat formatter;
	
	private int dayIndex, monthIndex, yearIndex;
    private String delimiter;

	public VerySimpleDateFormat(String pattern) {
		formatter = new SimpleDateFormat(pattern);
		dayIndex = pattern.indexOf("dd"); //$NON-NLS-1$
		monthIndex = pattern.indexOf("MM"); //$NON-NLS-1$
		yearIndex = pattern.indexOf("yyyy"); //$NON-NLS-1$
        if (pattern.indexOf(".") > -1) { //$NON-NLS-1$
            delimiter = "."; //$NON-NLS-1$
        } else if (pattern.indexOf("/") > -1) { //$NON-NLS-1$
            delimiter = "/"; //$NON-NLS-1$
        } else if (pattern.indexOf("-") > -1) { //$NON-NLS-1$
            delimiter = "-"; //$NON-NLS-1$
        }
	}

	/**
	 * @param date the date to be formatted, which may
	 * 			be null
	 * @return the formatted date, or an empty string if the date
	 * 			parameter is null
	 */
	public String format(Date date) {
		return (date == null) ? "" : formatter.format(date); //$NON-NLS-1$
	}

	public Date parse(String dateString) {
		try {
			Calendar cl = Calendar.getInstance();
			int day = cl.get(Calendar.DATE);
			int month = cl.get(Calendar.MONTH);
			int year = cl.get(Calendar.YEAR);
			cl.clear();
            cl.setLenient(false);
			StringTokenizer st = new StringTokenizer(dateString, delimiter, true);
			switch (st.countTokens()) {
				case 1 :
					day = Integer.parseInt(st.nextToken());
					break;
				case 3 :
					if (dayIndex < monthIndex) {
						day = Integer.parseInt(st.nextToken());
                        st.nextToken();   // ignore the delimiter
						month = Integer.parseInt(st.nextToken()) - 1;
					} else if (dayIndex > monthIndex) {
						month = Integer.parseInt(st.nextToken()) - 1;
                        st.nextToken();   // ignore the delimiter
						day = Integer.parseInt(st.nextToken());
					}
					break;
				case 5 :
					String d = null, m = null, y = null;
					if (dayIndex < monthIndex && monthIndex < yearIndex) {
						d = st.nextToken();
                        st.nextToken();   // ignore the delimiter
						m = st.nextToken();
                        st.nextToken();   // ignore the delimiter
						y = st.nextToken();
					} else if (monthIndex < dayIndex && dayIndex < yearIndex) {
						m = st.nextToken();
                        st.nextToken();   // ignore the delimiter
						d = st.nextToken();
                        st.nextToken();   // ignore the delimiter
						y = st.nextToken();
					} else if (
						yearIndex < monthIndex && monthIndex < dayIndex) {
						y = st.nextToken();
                        st.nextToken();   // ignore the delimiter
						m = st.nextToken();
                        st.nextToken();   // ignore the delimiter
						d = st.nextToken();
					} else if (yearIndex < dayIndex && dayIndex < monthIndex) {
						y = st.nextToken();
                        st.nextToken();   // ignore the delimiter
						d = st.nextToken();
                        st.nextToken();   // ignore the delimiter
						m = st.nextToken();
					}
					day = Integer.parseInt(d);
					month = Integer.parseInt(m) - 1;
					year = Integer.parseInt(y);
					if (y != null && y.length() < 3)
						year += year < 30 ? 2000 : 1900;
					break;
				default :
					throw new IllegalArgumentException("No valid date: " + dateString); //$NON-NLS-1$
			}
			cl.set(year, month, day);
			return cl.getTime();
		} catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                throw new IllegalArgumentException("No valid date: " + dateString); //$NON-NLS-1$
            }
            return null;
		}
	}
}
