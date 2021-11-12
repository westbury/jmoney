/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2021 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.txr.debug;

import java.awt.Toolkit;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.net.URL;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import net.sf.jmoney.txr.Activator;

public class TxrDebugView extends ViewPart {

	public class PasteTxrAction extends Action {
		public PasteTxrAction() {
			super("Paste Txr");
			setToolTipText("Paste the TXR to be debugged");
			setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));
		}

		@Override
		public void run() {
			try {
				pasteTxr();
			} catch (Exception e) {
				e.printStackTrace();
				MessageDialog.openError(getViewSite().getShell(), "Paste Failed", e.getMessage());
			}
		}
	}

	public class PasteTextAction extends Action {
		public PasteTextAction() {
			super("Paste Test Text");
			setToolTipText("Paste Test Text");
			setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));
		}

		@Override
		public void run() {
			try {
				pasteDetails();
			} catch (Exception e) {
				e.printStackTrace();
				MessageDialog.openError(getViewSite().getShell(), "Paste Failed", e.getMessage());
			}
		}
	}

	public static String ID = "net.sf.jmoney.txr.TxrDebugView";

	private PasteTxrAction pasteTxrAction;

	private PasteTextAction pasteTextAction;

	private Image errorImage;

	private String txr = "@(collect)\nOrder Detail";
	
	private String testData = "line 1\nline 2";

	private Composite txrEditorComposite;

	private Composite testDataComposite;

	private ScrolledComposite sc;

	private Composite horizontallySplitComposite;
	
	public TxrDebugView() {
		pasteTxrAction = new PasteTxrAction();
		pasteTextAction = new PasteTextAction();

		// Load the error indicator
		URL installURL = Activator.getDefault().getBundle().getEntry("/icons/error.gif");
		errorImage = ImageDescriptor.createFromURL(installURL).createImage();
	}

	@Override
	public void init(IViewSite viewSite, IMemento memento) throws PartInitException {
		super.init(viewSite, memento);

		//		if (memento != null) {
		//			filter.init(memento.getChild("filter"));
		//		}

	}

	@Override
	public void saveState(IMemento memento) {	
		super.saveState(memento);
		//		filter.saveState(memento.createChild("filter"));
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
		// No items are currently in the pull down menu.
		Toolkit.getDefaultToolkit().getSystemClipboard().addFlavorListener(new FlavorListener() { 
			@Override 
			public void flavorsChanged(FlavorEvent e) {

				System.out.println("ClipBoard UPDATED: " + e.getSource() + " " + e.toString());
			} 
		}); ;
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(pasteTxrAction);
		manager.add(pasteTextAction);
	}

	@Override
	public void createPartControl(Composite parent) {
		Composite stackComposite = new Composite(parent, SWT.NONE);

		StackLayout stackLayout = new StackLayout();
		stackComposite.setLayout(stackLayout);

		Label noSessionLabel = new Label(stackComposite, SWT.WRAP);
		noSessionLabel.setText("No TXR or no test data has been pasted.");

		Control dataComposite = createMainArea(stackComposite);

		if (txr == null || testData == null) {
			stackLayout.topControl = noSessionLabel;
		} else {
			stackLayout.topControl = dataComposite;

			// TODO process TXR and test data here
		}

		contributeToActionBars();
	}

	private Control createMainArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));

		createTopArea(composite);
		createScrollableMainArea(composite).setLayoutData(new GridData(GridData.FILL_BOTH));
		createButtonComposite(composite);

		return composite;
	}

	private Control createButtonComposite(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new RowLayout(SWT.HORIZONTAL));

		Button commitButton = new Button(composite, SWT.PUSH);
		commitButton.setText("Commit");
		commitButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				commitChanges();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				commitChanges();
			}

			private void commitChanges() {
				// TODO do we want this button?
			}
		});

		Button abortButton = new Button(composite, SWT.PUSH);
		abortButton.setText("Abort");
		abortButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				abortChanges();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				abortChanges();
			}

			private void abortChanges() {
				// Do we want this button?
			}
		});

		return composite;
	}

	private void pasteTxr() {
		txr = getTextFromClipboard();
		this.parseTxr();
		this.runMatcher();
	}

	private void pasteDetails() {
		testData = getTextFromClipboard();
		this.runMatcher();
	}

	private String getTextFromClipboard() {
		Display display = Display.getCurrent();
		Clipboard clipboard = new Clipboard(display);
		String plainText = (String)clipboard.getContents(TextTransfer.getInstance());
		clipboard.dispose();        

		return plainText;
	}

	private void parseTxr() {
	
		
	}
	
	private void runMatcher() {
	
		
		while (txrEditorComposite.getChildren().length != 0) {
			txrEditorComposite.getChildren()[0].dispose();
		}
		
		int lineNumber = 0;
		for (String line : txr.split("\n")) {
			Text lineControl = new Text(txrEditorComposite, SWT.NONE);
			lineControl.setText(line);
			
			if (++lineNumber == 3) {
				lineControl.setLayoutData(new RowData(SWT.DEFAULT, 100));
			}
			
			Point[] offset = new Point[1];
			Listener listener = new Listener () {
			    public void handleEvent (Event event) {
			      switch (event.type) {
			        case SWT.MouseDown:
			        	System.out.println("Down: " + event.x + ',' + event.y);
//			          Rectangle rect = composite.getBounds ();
//			          if (rect.contains (event.x, event.y)) {
//			            Point pt1 = composite.toDisplay (0, 0);
//			            Point pt2 = shell.toDisplay (event.x, event.y); 
			            offset [0] = new Point (event.x, event.y);
//			          }
			          break;
			        case SWT.MouseMove:
			          if (offset[0] != null) {
				        	int gap = event.y - offset[0].y;
				        	if (gap > 0) {
				        		lineControl.setLayoutData(new RowData(SWT.DEFAULT, 20 + gap));
				        	} else {
				        		lineControl.setLayoutData(new RowData(SWT.DEFAULT, SWT.DEFAULT));
				        	}
				        	txrEditorComposite.layout();
			          }
			          
			          break;
			        case SWT.MouseUp:
			          offset [0] = null;
			          break;
			      }
			    }
			  };
			  lineControl.addListener (SWT.MouseDown, listener);
			  lineControl.addListener (SWT.MouseUp, listener);
			  lineControl.addListener (SWT.MouseMove, listener);
		}
		txrEditorComposite.layout();
		
		while (testDataComposite.getChildren().length != 0) {
			testDataComposite.getChildren()[0].dispose();
		}
		for (String line : testData.split("\n")) {
			Label lineControl = new Label(testDataComposite, SWT.NONE);
			lineControl.setText(line);
		}
		testDataComposite.layout();
		
		// Is this needed?  (if not, hsc)
//		sc.setMinSize(horizontallySplitComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		horizontallySplitComposite.setSize(horizontallySplitComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		horizontallySplitComposite.layout();
		
		sc.layout(true);
		sc.update();
		sc.getParent().update();
		sc.getParent().layout(true);
		
	}
	
	private Control createTopArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
 
		// Anything to go here?
		
		return composite;
	}
	
	private Control createScrollableMainArea(Composite parent) {
		sc = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
	
		horizontallySplitComposite = createHorizontallySplitArea(sc);
	
		sc.setContent(horizontallySplitComposite);	
	
//		sc.setExpandHorizontal(true);
//		sc.setExpandVertical(true);
//		sc.setMinSize(horizontallySplitComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		horizontallySplitComposite.setSize(horizontallySplitComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		sc.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent arg0) {
				horizontallySplitComposite.setSize(new Point(sc.getClientArea().width, sc.getClientArea().height));
			}
		});
		return sc;
	}
	
	private Composite createHorizontallySplitArea(Composite parent) {
		Composite containerOfSash = new Composite(parent, SWT.NONE);
		containerOfSash.setLayout(new FormLayout());

		// Create the sash first, so the other controls
		// can be attached to it.
		final Sash sash = new Sash(containerOfSash, SWT.BORDER | SWT.VERTICAL);
		FormData formData = new FormData();
		formData.top = new FormAttachment(0, 0); // Attach to top
		formData.bottom = new FormAttachment(100, 0); // Attach to bottom
		formData.left = new FormAttachment(50, 0); // Attach halfway across
		sash.setLayoutData(formData);

		sash.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				final int mimimumWidth = 61;  // In Windows, allows 3 lines minimum.  TODO: Calculate this for other OS's
				int x = event.x;
				if (x < mimimumWidth) {
					x = mimimumWidth;
				}
				if (x + sash.getSize().x > sash.getParent().getSize().x - mimimumWidth) {
					x = sash.getParent().getSize().x - mimimumWidth - sash.getSize().x;
				}

				// We re-attach to the left edge, and we use the x value of the event to
				// determine the offset from the left
				((FormData) sash.getLayoutData()).left = new FormAttachment(0, x);

				// Until the parent window does a layout, the sash will not be redrawn in
				// its new location.
				sash.getParent().layout();
			}
		});

		GridData gridData1 = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData1.heightHint = 200;   // TODO: tidy up???
		gridData1.widthHint = 200;   // TODO: tidy up???
		containerOfSash.setLayoutData(gridData1);

		Control fStatementSection = createTxrEditorArea(containerOfSash);

		formData = new FormData();
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(sash, 0);
		formData.top = new FormAttachment(0, 0);
		formData.bottom = new FormAttachment(100, 0);
		fStatementSection.setLayoutData(formData);

		Control fUnreconciledSection = createTestDataArea(containerOfSash);

		formData = new FormData();
		formData.left = new FormAttachment(sash, 0);
		formData.right = new FormAttachment(100, 0);
		formData.top = new FormAttachment(0, 0);
		formData.bottom = new FormAttachment(100, 0);
		fUnreconciledSection.setLayoutData(formData);

		return containerOfSash;
	}	

	private Control createTxrEditorArea(Composite parent) {
		txrEditorComposite = new Composite(parent, SWT.NONE);
		RowLayout rowLayout = new RowLayout(SWT.VERTICAL);
		rowLayout.wrap = false;
		txrEditorComposite.setLayout(rowLayout);

		for (String line : txr.split("\n")) {
			Text lineControl = new Text(txrEditorComposite, SWT.NONE);
			lineControl.setText(line);
		}

		return txrEditorComposite;
	}

	private Control createTestDataArea(Composite parent) {
		testDataComposite = new Composite(parent, SWT.NONE);
		RowLayout rowLayout = new RowLayout(SWT.VERTICAL);
		rowLayout.wrap = false;
		testDataComposite.setLayout(rowLayout);

		for (String line : testData.split("\n")) {
			Label lineControl = new Label(testDataComposite, SWT.NONE);
			lineControl.setText(line);
		}

		return testDataComposite;
	}

//	private Control createOrderControls(Composite parent) {
//		Composite composite = new Composite(parent, SWT.NONE);
//		composite.setLayout(new GridLayout(2, false));
//
//		Label orderNumberLabel = new Label(composite, 0);
//		orderNumberLabel.setText("Order Number:");
//		Text orderControl = new Text(composite, SWT.NONE);
//		orderControl.setLayoutData(new GridData(200, SWT.DEFAULT));
//
//		Label orderDateLabel = new Label(composite, 0);
//		orderDateLabel.setText("Order Date:");
//		DateControl orderDateControl = new DateControl(composite);
//		orderDateControl.setLayoutData(new GridData(200, SWT.DEFAULT));
//
//		Label orderAmountLabel = new Label(composite, 0);
//		orderAmountLabel.setText("Order Total:");
//		Text orderAmountControl = new Text(composite, SWT.NONE);
//		orderAmountControl.setLayoutData(new GridData(200, SWT.DEFAULT));
//
//		selObs.addValueChangeListener(new IValueChangeListener<Object>() {
//			@Override
//			public void handleValueChange(ValueChangeEvent<? extends Object> event) {
//				if (selObs.getValue() instanceof AmazonOrder) {
//					AmazonOrder order = (AmazonOrder)selObs.getValue();
//
//					orderControl.setText(order.getOrderNumber());
//					orderDateControl.setDate(order.getOrderDate());
//
//					orderAmountControl.setText(currencyFormatter.format(order.getOrderTotal()));
//				}
//			}
//		});
//
//		return composite;
//	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

	@Override
	public void dispose() {
		super.dispose();
		errorImage.dispose();
	}

}
