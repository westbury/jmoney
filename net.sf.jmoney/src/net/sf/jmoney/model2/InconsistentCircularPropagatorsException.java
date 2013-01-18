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
public class InconsistentCircularPropagatorsException extends RuntimeException {

	private static final long serialVersionUID = -385956833951648836L;

	private String[] propertyIdList;
    private Object[] valueList;

    /** 
     * Creates a new instance of InconsistentCircularPropagatorsException
     * when the circular error is initially detected. 
     */
    public InconsistentCircularPropagatorsException(String propertyId, Object newValue) {
        propertyIdList = new String[] { propertyId };
        valueList = new Object[] { newValue };
    }
    
    /** 
     * Creates a new instance of InconsistentCircularPropagatorsException
     * in which we are adding more information to a previously throw
     * InconsistentCircularPropagatorsException.
     */
    public InconsistentCircularPropagatorsException(String propertyId, Object newValue, InconsistentCircularPropagatorsException previousException) {
        int size = propertyIdList.length;
        
        propertyIdList = new String[size + 1];
        valueList = new Object[size + 1];
        
        propertyIdList[0] = propertyId;
        valueList[0] = newValue;
        for (int i = 0; i < size; i++) {
            propertyIdList[i+1] = previousException.propertyIdList[i];
            valueList[i+1] = previousException.valueList[i];
        }
    }
    
}
