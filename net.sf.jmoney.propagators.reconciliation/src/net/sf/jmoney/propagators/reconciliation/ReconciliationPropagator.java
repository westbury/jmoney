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

package net.sf.jmoney.propagators.reconciliation;

import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.qif.QIFEntry;
import net.sf.jmoney.reconciliation.ReconciliationEntry;

/**
 * This class connects the reconcilation field added by the reconciliation extension
 * plugin and the field that the QIF extension imports and exports.
 *
 * The reconciliation plugin and the QIF plugin are independent.  They could have been
 * written by two different developers who did not know about the other's work.
 * One chose to store the state as an integer (0, 1, or 2) and the other chose to store
 * the state as a character (' ', '*', or 'C').  An propagator like this one must then be
 * written by a developer who is aware of the two plugins.  This propagator connects the two
 * properties so when one plugin makes a change to its property, the value of the other
 * plugin's property is changed to match.
 * 
 * In any propagator class any number of propertyChange methods may be declared.
 *
 * @author  Nigel
 */
public class ReconciliationPropagator {
    
        public static void propertyChange(ScalarPropertyAccessor property, ReconciliationEntry sourceReconciliationEntry, QIFEntry destinationQifEntry) {
            if (property.getLocalName().equals("status")) {
            	if (sourceReconciliationEntry.getStatement() == null) {
            		if (destinationQifEntry.getReconcilingState() == 'C') {
            			destinationQifEntry.setReconcilingState(' ');
            		}
            	} else {
            		destinationQifEntry.setReconcilingState('C');
            	}
            }
        }

        public static void propertyChange(ScalarPropertyAccessor property, QIFEntry sourceQifEntry, ReconciliationEntry destinationReconciliationEntry) {
            if (property.getLocalName().equals("reconcilingState")) {
                switch (sourceQifEntry.getReconcilingState()) {
                    case ' ':
                        destinationReconciliationEntry.setStatus(ReconciliationEntry.UNCLEARED);
                        destinationReconciliationEntry.setStatement(null);
                        break;
                    case '*':
                        destinationReconciliationEntry.setStatus(ReconciliationEntry.RECONCILING);
                    	// Don't change the statement.
                        break;
                    case 'C':
                        destinationReconciliationEntry.setStatus(ReconciliationEntry.CLEARED);
                    	/*
                    	 * We have no way of knowing what statement this entry should
                    	 * be reconciled to, so we leave it as not yet reconciled to
                    	 * any statement.
                    	 */
                        break;
                }
            }
        }

/**
 * This is an alternative form.  This form avoids the need to switch on the
 * property name.  For a given source and destination pair, there can be either a
 * propertyChange method or there can be methods of the 'change...' pattern but
 * there cannot be both.  Note also that not all properties need a 'change...' method
 * or need to be processed by the propertyChange method.
 *        
        public static void changeStatus(ReconciliationEntry source, QIFEntryExtension destination) {
                switch (sourceReconciliationEntry.getStatus()) {
                    case ReconciliationEntry.UNCLEARED:
                        destinationQifEntry.setReconcilingState(' ');
                        break;
                    case ReconciliationEntry.RECONCILING:
                        destinationQifEntry.setReconcilingState('*');
                        break;
                    case ReconciliationEntry.CLEARED:
                        destinationQifEntry.setReconcilingState('C');
                        break;
                }
        }
 */
}
