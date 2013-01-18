package net.sf.jmoney.importer.wizards;

import net.sf.jmoney.model2.Session;

public interface MultiRowTransaction {
	/**
	 * 
	 * @return true if this row was processed, false if this row is not a
	 * 		part of this transaction and should be separately processed
	 * 		by the caller
	 * @throws ImportException 
	 */
	boolean processCurrentRow(Session session) throws ImportException;

	/**
	 * 
	 * @return true if this transaction has received all its row and is
	 * 		ready to be created in the datastore, false if there may be
	 * 		more rows in this transaction
	 */
	boolean isDone();

	void createTransaction(Session session) throws ImportException;
}

