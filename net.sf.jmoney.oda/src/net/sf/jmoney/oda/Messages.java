/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2007 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.oda;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Resource messages wrapper for the package to obtain localized message text.
 */

public class Messages
{
    private static final String BUNDLE_NAME = "net.sf.jmoney.oda.messages";//$NON-NLS-1$

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
            .getBundle( BUNDLE_NAME );

    public static String getString(String key) {
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch( MissingResourceException e) {
            return '!' + key + '!';
        }
    }
    
    public static String getFormattedString(String key, Object[] args)
    {
        return MessageFormat.format(getString(key), args);
    }
}