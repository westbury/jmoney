package net.sf.jmoney.entrytable;


import java.util.HashSet;
import java.util.Set;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.isolation.IModelObject;
import net.sf.jmoney.isolation.SessionChangeAdapter;
import net.sf.jmoney.isolation.SessionChangeListener;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.resources.Messages;

import org.eclipse.core.databinding.observable.set.ComputedSet;
import org.eclipse.core.databinding.observable.set.IObservableSet;
import org.eclipse.core.databinding.observable.set.ISetChangeListener;
import org.eclipse.core.databinding.observable.set.SetChangeEvent;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;

/**
 * This control contains the properties that come from other entries in the
 * transaction.  If the transaction is not split then these properties will
 * be shown directly in this control.  If the transaction is split then a
 * --Split Entries-- label will be shown together with a button to open an
 * in-place shell that shows the other entries.
 * 
 * In fact the button is shown even if the transaction is not split.  This
 * enables the user to bring down the shell which contains the buttons used
 * to create splits.
 */
public class OtherEntriesControl extends Composite {

	private RowControl rowControl;
	private Block<Entry, ISplitEntryContainer> rootBlock;
	private RowSelectionTracker<BaseEntryRowControl> selectionTracker;
	private FocusCellTracker focusCellTracker;
	
	/**
	 * The composite containing whatever is to the left of
	 * the drop-down button.  This has a StackLayout.
	 */
	private Composite stackComposite;

	private StackLayout stackLayout;
	
	/**
	 * The label that is shown if this is a split entry.
	 * This label is shown instead of all the fields for
	 * properties that come from the other entry.
	 */
	private Label splitLabel;

	/**
	 * The composite that contains the fields for
	 * properties that come from the other entry.
	 * This composite is shown only if the entry is not split.
	 */
	private OtherEntryControl otherEntryControl;

	/**
	 * The small drop-down button to the right that shows
	 * the split entry data.
	 */
	Button downArrowButton;

//	private EntryData entryData;

//	private SessionChangeListener splitEntryListener = new SessionChangeAdapter() {
//
//		@Override
//		public void objectInserted(IModelObject newObject) {
//			if (newObject instanceof Entry
//					&& ((Entry)newObject).getTransaction() == entryData.getEntry().getTransaction()
//					&& entryData.getEntry().getTransaction().getEntryCollection().size() == 3) {
//				// Is now split but was not split before
//				stackLayout.topControl = splitLabel;
//				childComposite.layout(false);
//			}
//		}
//
//		@Override
//		public void objectRemoved(IModelObject deletedObject) {
//			if (deletedObject instanceof Entry
//					&& ((Entry)deletedObject).getTransaction() == entryData.getEntry().getTransaction()
//					&& deletedObject != entryData.getEntry()
//					&& entryData.getEntry().getTransaction().getEntryCollection().size() == 3) {
//				// Is now not split but was split before
//				
//				/*
//				 * This listener is called before the object is deleted, so there will
//				 * be three entries in the transaction.  We want to find the third (i.e.
//				 * not the main entry and not the one being deleted).
//				 */
//				Entry thirdEntry = null;
//				for (Entry entry : entryData.getEntry().getTransaction().getEntryCollection()) {
//					if (entry != entryData.getEntry()
//							&& entry != deletedObject) {
//						assert(thirdEntry == null);
//						thirdEntry = entry;
//					}
//				}
//				otherEntryControl.setInput(thirdEntry);
//				stackLayout.topControl = otherEntryControl;
//				childComposite.layout(false);
//			}
//		}
//	};
	
	static private Image downArrowImage = null;

	public OtherEntriesControl(Composite parent, final IObservableValue<? extends EntryData> masterEntryData, RowControl rowControl, Block<Entry, ISplitEntryContainer> rootBlock, RowSelectionTracker<BaseEntryRowControl> selectionTracker, FocusCellTracker focusCellTracker) {
		super(parent, SWT.NONE);
		this.rowControl = rowControl;
		this.rootBlock = rootBlock;
		this.selectionTracker = selectionTracker;
		this.focusCellTracker = focusCellTracker;
		
		setBackgroundMode(SWT.INHERIT_FORCE);
		
		setLayout(new DropdownButtonLayout());
		
		final IObservableSet<Entry> otherEntries = new ComputedSet<Entry>() {
			@Override
			protected Set<Entry> calculate() {
				// TODO Make getSplitEntries return Set, not Collection,
				// so we don't have to wrap here.
				if (masterEntryData.getValue() != null) {
					System.out.println(masterEntryData.getValue().getSplitEntries().size());
				} else {
					System.out.println("null");
				}
				return masterEntryData.getValue() == null
						? null
								: new HashSet<Entry>(masterEntryData.getValue().getSplitEntries());
			}
		};
		
		createChildComposite();
		createDownArrowButton(masterEntryData);

		otherEntries.addSetChangeListener(new ISetChangeListener<Entry>() {
			@Override
			public void handleSetChange(SetChangeEvent<Entry> event) {
				setStackControl(otherEntries);
			}
		});
		
		setStackControl(otherEntries);
		
	}

	private void setStackControl(Set<Entry> otherEntries) {
		if (otherEntries.size() == 1) {
			Entry theOnlyOtherEntry = otherEntries.iterator().next();
			otherEntryControl.setInput(theOnlyOtherEntry);
			stackLayout.topControl = otherEntryControl;
		} else {
			stackLayout.topControl = splitLabel;
		}
		stackComposite.layout(true);
	}

	private Control createDownArrowButton(final IObservableValue<? extends EntryData> entryData) {
		downArrowButton = new Button(this, SWT.NO_TRIM);
		if (downArrowImage == null) {
			ImageDescriptor descriptor = JMoneyPlugin.createImageDescriptor("comboArrow.gif"); //$NON-NLS-1$
			downArrowImage = descriptor.createImage();
		}
		downArrowButton.setImage(downArrowImage);

		downArrowButton.addSelectionListener(new SelectionAdapter() {
			@Override
		    public void widgetSelected(SelectionEvent event) {
				final OtherEntriesShell shell = new OtherEntriesShell(getShell(), SWT.ON_TOP, entryData.getValue(), rootBlock, true);
    	        Display display = getDisplay();
    	        Rectangle rect = display.map(OtherEntriesControl.this.getParent(), null, getBounds());
    	        shell.open(rect);
			}
		});
		
		return downArrowButton;
	}

	private Control createChildComposite() {
		stackComposite = new Composite(this, SWT.NONE);
		
		setBackgroundMode(SWT.INHERIT_FORCE);
		
		stackLayout = new StackLayout();
		stackComposite.setLayout(stackLayout);
		
		splitLabel = new Label(stackComposite, SWT.NONE);
		splitLabel.setText(Messages.OtherEntriesControl_SplitEntry);

		otherEntryControl = new OtherEntryControl(stackComposite, rowControl, rootBlock, true, selectionTracker, focusCellTracker);
		
		return stackComposite;
	}
	
	public void load(final EntryData entryData) {
//		// TODO: this should be done in a 'row release' method??
//		if (this.entryData != null) {
//			this.entryData.getEntry().getDataManager().removeChangeListener(splitEntryListener);
//		}
//		
//		this.entryData = entryData;
//		
//		if (entryData.getSplitEntries().size() == 1) {
//			otherEntryControl.setInput(entryData.getOtherEntry());
//			stackLayout.topControl = otherEntryControl;
//		} else {
//			stackLayout.topControl = splitLabel;
//		}
//		childComposite.layout(true);
//
//		// Listen for changes so this control is kept up to date.
//		entryData.getEntry().getDataManager().addChangeListener(splitEntryListener);
	}

	public void save() {
		/*
		 * The focus cell in our child table may not have been saved. Data in
		 * each cell is normally saved when another cell gets focus. However,
		 * there is a separate focus cell tracker for the cells inside this
		 * control and the cells outside.
		 */
//		IPropertyControl cell = focusCellTracker.getFocusCell();
//		if (cell != null) {
//			cell.save();
//		}
	}

	/**
	 * Internal class for laying out this control.  There are two child
	 * controls - the composite with the data for the other entries and
	 * a drop-down button.
	 */
	private class DropdownButtonLayout extends Layout {
	    @Override	
		public void layout(Composite composite, boolean force) {
			Rectangle bounds = composite.getClientArea();
			stackComposite.setBounds(0, 0, bounds.width-OtherEntriesBlock.DROPDOWN_BUTTON_WIDTH, bounds.height);
			downArrowButton.setBounds(bounds.width-OtherEntriesBlock.DROPDOWN_BUTTON_WIDTH, 0, OtherEntriesBlock.DROPDOWN_BUTTON_WIDTH, bounds.height);
		}

	    @Override	
		public Point computeSize(Composite composite, int wHint, int hHint, boolean force) {
			/*
			 * The button is always a fixed width.  Therefore we simply pass on to the contents,
			 * after adjusting for the button width. 
			 */
			int contentsWidthHint = (wHint == SWT.DEFAULT) ? SWT.DEFAULT : wHint - OtherEntriesBlock.DROPDOWN_BUTTON_WIDTH; 
			Point contentsSize = stackComposite.computeSize(contentsWidthHint, hHint, force);
			return new Point(contentsSize.x + OtherEntriesBlock.DROPDOWN_BUTTON_WIDTH, contentsSize.y);
		}
	}
}
