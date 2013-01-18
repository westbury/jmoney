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

/**
 *
 * @author  Nigel
 */
public class PropertySetNotFoundException extends Exception {
    
	private static final long serialVersionUID = 8497895163661872417L;

	private String missingPropertySetId;
    
    /** Creates a new instance of PropertyNotFoundException */
    public PropertySetNotFoundException(String missingPropertySetId) {
        this.missingPropertySetId = missingPropertySetId;
    }
    
    @Override	
    public String getMessage() {
        return "The '" + missingPropertySetId + "' property set was not found."; //$NON-NLS-1$ //$NON-NLS-2$
    }
}
