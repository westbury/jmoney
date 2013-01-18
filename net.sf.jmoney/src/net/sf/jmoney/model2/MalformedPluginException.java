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

package net.sf.jmoney.model2;

import java.lang.reflect.InvocationTargetException;

/**
 *
 * @author  Nigel
 */
public class MalformedPluginException extends RuntimeException {
    
	private static final long serialVersionUID = -6363680459650517598L;

	/** Creates a new instance of MalformedPluginExtension */
    public MalformedPluginException(String text) {
        super(text);
    }
    
    /**
     * Creates a new instance of MalformedPluginExtension
     * This constructor should be used when an uncaught exception
     * is raised inside a plug-in.  The stack for the original
     * cause is maintained and logged. 	 
     */
    public MalformedPluginException(String text, InvocationTargetException cause) {
        super(text, cause.getCause());
    }
    
}
