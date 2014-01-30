/**
 * 
 */
package net.sf.jmoney.entrytable;

import java.util.HashMap;
import java.util.Map;

import net.sf.jmoney.isolation.IModelObject;
import net.sf.jmoney.isolation.SessionChangeAdapter;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.resources.Messages;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * This class implements the shell that drops down to show the split entries.
 * 
 * @author Nigel Westbury
 */
public class OtherEntriesShell {

		private Shell parentShell;
		
		private EntryData entryData;

		private Block<Entry, ISplitEntryContainer> rootBlock;
		
		private Shell shell;
		
		private RowSelectionTracker<SplitEntryRowControl> rowTracker = new RowSelectionTracker<SplitEntryRowControl>();

		private FocusCellTracker cellTracker = new FocusCellTracker();

	    private Map<Entry, SplitEntryRowControl> rowControls = new HashMap<Entry, SplitEntryRowControl>();

		public OtherEntriesShell(Shell parent, int style, EntryData entryData, Block<Entry, ISplitEntryContainer> rootBlock, boolean isLinked) {
			shell = new Shell(parent, style | SWT.MODELESS);
		
			this.parentShell = parent;
			if (entryData == null) {
				throw new RuntimeException("can't be null");
			}
			this.entryData = entryData;
			this.rootBlock = rootBlock;
			
			GridLayout layout = new GridLayout(1, false);
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			layout.verticalSpacing = 3;
	        shell.setLayout(layout);

	        Control entriesTable = createEntriesTable(shell, isLinked);
	        entriesTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

	        Control buttonArea = createButtonArea(shell);
	        buttonArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
	        
		    shell.pack();
		}

		private Control createEntriesTable(Composite parent, final boolean isLinked) {
			final Composite composite = new Composite(parent, SWT.NONE);
			
			GridLayout layout = new GridLayout(1, false);
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			layout.horizontalSpacing = 0;
			layout.verticalSpacing = 0;
			composite.setLayout(layout);

		    /*
		     * Use a single row tracker and cell focus tracker for this table.
		     */
			for (Entry entry: entryData.getSplitEntries()) {
				SplitEntryRowControl row = new SplitEntryRowControl(composite, rootBlock, isLinked, rowTracker, cellTracker, entry);
				rowControls.put(entry, row);
			}

			
			// Remove all this and use the updating composite...
			
			entryData.getEntry().getDataManager().addChangeListener(new SessionChangeAdapter() {
				@Override
				public void objectInserted(IModelObject newObject) {
					if (newObject instanceof Entry) {
						Entry newEntry = (Entry) newObject;
						if (newEntry.getTransaction() == entryData.getEntry().getTransaction()) {
							entryData.getSplitEntries().add(newEntry);
							SplitEntryRowControl row = new SplitEntryRowControl(composite, rootBlock, isLinked, rowTracker, cellTracker, newEntry);
							// Split entry rows take a final entry in the constructor
//							row.setInput(newEntry);
							rowControls.put(newEntry, row);
			    	        shell.pack();
						}
					}
				}
				
				@Override
				public void objectRemoved(IModelObject deletedObject) {
					if (deletedObject instanceof Entry) {
						Entry deletedEntry = (Entry) deletedObject;
						if (deletedEntry.getTransaction() == entryData.getEntry().getTransaction()) {
							entryData.getSplitEntries().remove(deletedEntry);
							rowControls.get(deletedEntry).dispose();
							rowControls.remove(deletedEntry);
			    	        shell.pack();
						}
					}
				}
			}, composite);

			return composite;
		}
		
		private Control createButtonArea(Composite parent) {
			Composite composite = new Composite(parent, SWT.NONE);
			
			RowLayout layout = new RowLayout(SWT.HORIZONTAL);
			layout.marginBottom = 0;
			layout.marginTop = 0;
			composite.setLayout(layout);

	        Button newSplitButton = new Button(composite, SWT.PUSH);
	        newSplitButton.setText(Messages.OtherEntriesShell_NewSplit);
	        newSplitButton.addSelectionListener(new SelectionAdapter() {
	        	@Override
				public void widgetSelected(SelectionEvent e) {
					addSplit();
				}
			}); 
	        
	        Button deleteSplitButton = new Button(composite, SWT.PUSH);
	        deleteSplitButton.setText(Messages.OtherEntriesShell_DeleteSplit);
	        deleteSplitButton.addSelectionListener(new SelectionAdapter() {
	        	@Override
				public void widgetSelected(SelectionEvent e) {
					deleteSplit();
				}
			}); 
	        
	        Button adjustButton = new Button(composite, SWT.PUSH);
	        adjustButton.setText(Messages.OtherEntriesShell_Adjust);
	        adjustButton.setToolTipText(Messages.OtherEntriesShell_AdjustToolTipText);
	        adjustButton.addSelectionListener(new SelectionAdapter() {
	        	@Override
				public void widgetSelected(SelectionEvent e) {
					adjustAmount();
				}
			}); 
	        
			return composite;
		}

		private void addSplit() {
			Transaction transaction = entryData.getEntry().getTransaction();
			Entry newEntry = transaction.createEntry();
			
			long total = 0;
			Commodity commodity = null;
			for (Entry entry: transaction.getEntryCollection()) {
				if (entry.getCommodityInternal() != null) {
    				if (commodity == null) {
    					commodity = entry.getCommodityInternal();
    				} else if (!commodity.equals(entry.getCommodityInternal())) {
    					// We have entries with mismatching commodities set.
    					// This means there is an exchange of one commodity
    					// for another so we do not expect the total amount
    					// of all the entries to be zero.  Leave the amount
    					// for this new entry blank (a zero amount).
    					total = 0;
    					break;
    				}
				}

				total += entry.getAmount();
			}
			
			newEntry.setAmount(-total);
			
			// We set the currency by default to be the currency
			// of the top-level entry.
			
			// The currency of an entry is not
			// applicable if the entry is an entry in a currency account
			// (because all entries in a currency account must have the
			// currency of the account).  However, we set it anyway so
			// the value is there if the entry is set to an income and
			// expense account (which would cause the currency property
			// to become applicable).

			// It may be that the currency of the top-level entry is not known.
			// This is not possible if entries in a currency account
			// are being listed, but may be possible if this entries list
			// control is used for more general purposes.  In this case,
			// the currency is not set and so the user must enter it.
			if (entryData.getEntry().getCommodityInternal() instanceof Currency) {
    			newEntry.setIncomeExpenseCurrency((Currency)entryData.getEntry().getCommodityInternal());
			}
			
       		// Select the new entry in the entries list.
//???                    setSelection(selectedEntry, newEntry);
		}

		private void deleteSplit() {
			SplitEntryRowControl rowControl = rowTracker.getSelectedRow();
			// TODO: Is a row ever not selected?
			if (rowControl != null) {
				Entry entry = rowControl.getInput();
				entryData.getEntry().getTransaction().deleteEntry(entry);
			}
		}
		
		private void adjustAmount() {
			SplitEntryRowControl rowControl = rowTracker.getSelectedRow();
			// TODO: Is a row ever not selected?
			if (rowControl != null) {
				Entry entry = rowControl.getInput();
				
				long totalAmount = 0;
				for (Entry eachEntry: entryData.getEntry().getTransaction().getEntryCollection()) {
					totalAmount += eachEntry.getAmount();
				}
				
				entry.setAmount(entry.getAmount() - totalAmount);
			}
		}

		public void open(Rectangle rect) {
	        /*
			 * Position the split-entries shell below the given rectangle, unless
			 * this control is so near the bottom of the display that the
			 * shell would go off the bottom of the display, in
			 * which case position the split-entries shell above this
			 * control.
			 * 
			 * In either case, the shell should overlap the rectangle, so if it
			 * is going downwards, align the top with the top of this control.
			 * 
			 * Note also that we put the shell one pixel to the left.  This is because
			 * a single pixel margin is always added to BlockLayout so that the
			 * selection line can be drawn.  We want the controls in the shell to
			 * exactly line up with the table header.
			 */

			Display display = shell.getDisplay();
	        int shellHeight = shell.getSize().y;
	        if (rect.y + rect.height + shellHeight <= display.getBounds().height) {
    	        shell.setLocation(rect.x - 1, rect.y);
	        } else {
    	        shell.setLocation(rect.x - 1, rect.y + rect.height - shellHeight);
	        }
	        
	        shell.open();
	        
	        /*
	         * We need to be sure to close the shell when it is no longer active.
	         * Listening for this shell to be deactivated does not work because there
	         * may be child controls which create child shells (third level shells).
	         * We do not want this shell to close if a child shell has been created
	         * and activated.  We want to close this shell only if the parent shell
	         * have been activated.  Note that if a grandparent shell is activated then
	         * we do not want to close this shell.  The parent will be closed anyway
	         * which would automatically close this one.
	         */
	        final ShellListener parentActivationListener = new ShellAdapter() {
				@Override
	        	public void shellActivated(ShellEvent e) {
					ICellControl2 focusCell = cellTracker.getFocusCell();
					if (focusCell != null) {
						focusCell.save();
					}
	        		shell.close();
	        	}
	        };
	        
	        parentShell.addShellListener(parentActivationListener);
	        
	        shell.addShellListener(new ShellAdapter() {
				@Override
				public void shellClosed(ShellEvent e) {
	        		parentShell.removeShellListener(parentActivationListener);
				}
	        });
		}
	}