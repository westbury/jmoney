/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (C) 2001-2008 Craig Cavanaugh and others
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

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;


/**
 * An extended LineNumberReader to help ease the pain of parsing
 * a QIF file
 * 
 * @author Craig Cavanaugh
 */
public class QifReader extends LineNumberReader {

    public QifReader(Reader in) {
        super(in, 8192);
    }

    public void mark() throws IOException {
        super.mark(256);
    }

    /**
     * Takes a peek at the next line and eats and empty line if found
     */
    public String peekLine() throws IOException {
        String peek;
        while (true) {
            mark();
            peek = readLine();
            if (peek != null) {
                peek = peek.trim();
                reset();
                if (peek.length() == 0) {
                    readLine(); // eat the empty line
                } else {
                    return peek.trim();
                }
            } else {
                return null;
            }
        }
    }

    @Override
    public String readLine() throws IOException {
    	while (true) {
    		String line = super.readLine();
    		if (line == null) {
    			return null;
    		}
    		line = line.trim();
    		if (line.length() > 0) {
    			return line;
    		}
    	}
    }
}
