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
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
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
import txr.matchers.DocumentMatcher;
import txr.matchers.DocumentMatcher.MatchPair;
import txr.matchers.MatchResults;
import txr.matchers.MatcherResult.IControlCallback;
import txr.parser.TxrErrorInDocumentException;

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

//	private String txr = "@(collect)\nOrder Detail\nAmount\n@amount\nPrice: @price\n@(until)\nWe also recommend:\n@(end)\nmatch line 1\nmatch line 2\nmatch line 3";
	
	private String txr = "Introduction\n"
			+ "\n"
			+ "@(collect)\n"
			+ "Title: @description\n"
			+ "Amount: £@amount\n"
			+ "\n"
			+ "@(until)\n"
			+ "Conclusion\n"
			+ "@(end)\n"
			+ "Conclusion\n"
			+ "Total: £@delivery\n";
	
	String [] testData = new String [] {
			"Introduction",
			"",
			"Title: Bananas",
			"Amount: £36",
			"",
			"Title: Oranges",
			"Amount: £42",
			"",
			"Conclusion",
			"Total: £78.00"
	};
	
//	private String testData = "line 1\nline 2\nline 3\nline 4\nline 5\nline 6\nline 7\nline 8\nline 9\nline 10\nline 11\nline 12\nline 13\nline 14";

	private Composite txrEditorComposite;

	private Composite testDataComposite;

	private ScrolledComposite sc;

	private Composite horizontallySplitComposite;

	private List<TxrLineMatch> txrLineMatches;
	private List<TextDataLineMatch> textDataLineMatches;

	private DocumentMatcher matcher;

	public TxrDebugView() {
		pasteTxrAction = new PasteTxrAction();
		pasteTextAction = new PasteTextAction();

		// Load the error indicator
		URL installURL = Activator.getDefault().getBundle().getEntry("/icons/error.gif");
		errorImage = ImageDescriptor.createFromURL(installURL).createImage();
		
		matcher = createMatcherFromResource("collect-test.txr");
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


		txrEditorComposite.layout();
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

	private void pasteDetails() {
		testData = getTextFromClipboard().split("\n");
		this.runMatcher();

		txrEditorComposite.layout();
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

	private String getTextFromClipboard() {
		Display display = Display.getCurrent();
		Clipboard clipboard = new Clipboard(display);
		String plainText = (String)clipboard.getContents(TextTransfer.getInstance());
		clipboard.dispose();        

		return plainText;
	}

	private void parseTxr() {


	}

	private Control txrLineRowComposite(Composite parent, int lineNumber, String line, int indentation, Listener listener) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 0;
		layout.verticalSpacing = 0;
		composite.setLayout(layout);

		Label lineNumberControl = new Label(composite, SWT.NONE);
		lineNumberControl.setText(Integer.toString(lineNumber));
		GridData lineNumberGridData = new GridData(SWT.FILL, SWT.BOTTOM, false, true);
		lineNumberGridData.widthHint = 30;
		lineNumberControl.setLayoutData(lineNumberGridData);

		Text lineControl = new Text(composite, SWT.NONE);
		lineControl.setText(line);
		GridData textGridData = new GridData(SWT.FILL, SWT.BOTTOM, true, true);
		textGridData.horizontalIndent = indentation * 30;
		lineControl.setLayoutData(textGridData);

		Control[] x = { composite, lineNumberControl, lineControl };
		for (Control c : x) {
			c.addListener (SWT.MouseDown, listener);
			c.addListener (SWT.MouseUp, listener);
			c.addListener (SWT.MouseMove, listener);
		}

		return composite;
	}

	private Control textDataLineRowComposite(Composite parent, int lineNumber, String line) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 0;
		layout.verticalSpacing = 0;
		composite.setLayout(layout);

		Label lineNumberControl = new Label(composite, SWT.NONE);
		lineNumberControl.setText(Integer.toString(lineNumber));
		GridData lineNumberGridData = new GridData(SWT.FILL, SWT.BOTTOM, false, true);
		lineNumberGridData.widthHint = 30;
		lineNumberControl.setLayoutData(lineNumberGridData);

		Label lineControl = new Label(composite, SWT.NONE);
		lineControl.setText(line);
		lineControl.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, true));

		return composite;
	}

	/**
	 * This class has two states: If its location is currently being dragged then 
	 * @author Nigel
	 *
	 */
	class TxrLineMatch {

		private int lineNumber;
		private String line;
		
		/**
		 * The line in the text data to which this is matched, or null if this is an unmatched line.
		 * 
		 * A line will be matched if either the matching rules matched it, or if the user explicitly stated
		 * that it is matched.
		 * 
		 * If this is set then the line on the left must be level with the text data line on the right.  This
		 * will likely involve adding empty space between lines (or either left or right). 
		 */
		private Integer textDataLineInstanceIndex;

		public IObservableValue<Integer> preceedingGap = new WritableValue<Integer>();
		
		/** indicates a line that does not match up with a data line.  It does however have a position in the text data but no line to the right. */
		protected boolean isDirective;
		
		public TxrLineMatch(int lineNumber, String line) {
			this.lineNumber = lineNumber;
			this.line = line;
			this.preceedingGap.setValue(0);
		}

		/**
		 * 			        	    Initially, and for small changes, the mouse is dragging the line up
			        	  or down.  However as one gets towards the limits of the editor area, the text
			        	  data area starts scrolling in the opposite direction.  Furthermore, the action
			        	  gradually switches so that the mouse position dictates the rate at which the text data
			        	  scrolls, so the text data will continue to scroll even if the mouse is not moved.
			        	  (stopping only when the mouse is moved back into the center third of the editor area or if the mouse button is
			        	  released).
			        	  The control will never go into the last sixth.  At that point the movement is only by scrolling the text data.
			        	  (though the speed will depend on where in the last sixth it is).  So second last sixth is the transition. 1) amount by which control moves relative to amount of mouse movement,
			        	  2) speed of scrolling is the addition of a) amount to compensate for lack of movement in first, and b) the speed based on the mouse position, gradually increasing in the entire last third.
			        	  

		 * @param movement the amount the mouse was moved since last updated
		 * @param positionWithinEditor a number in the range 0 (top of editor) to 1 (bottom of editor)
		 */
		// TODO this should not be here.  It should be handled outside in the dnd code. This object is updated only when dropped.
		public void setOffset(int movement, float positionWithinEditor) {
			if (positionWithinEditor < 0) {
				positionWithinEditor = 0;
			}
			if (positionWithinEditor > 1) {
				positionWithinEditor = 1;
			}
			
			if (positionWithinEditor < 0.166) {
				
			} else if (positionWithinEditor < 0.333) {
				
			} else if (positionWithinEditor < 0.666) {
				// Middle area, so move just the left area
				
			} else if (positionWithinEditor < 0.833) {
				
			} else {
				
			}
			
//			if (gap > 0) {
//				lineControl.setLayoutData(new RowData(SWT.DEFAULT, 20 + gap));
//			} else {
//				lineControl.setLayoutData(new RowData(SWT.DEFAULT, SWT.DEFAULT));
//			}
		}

		/**
		 * 
		 * @param gap the gap above this line, so zero if this line immediately follows the preceeding
		 * 			line, one if this line is to be positioned one line further down with a empty line above etc.
		 */
		public void setPreceedingGap(int gap) {
			// UI will listen and update layout objects
			preceedingGap.setValue(gap);
		}
	}

	class TextDataLineMatch {

		private int lineNumber;
		private String line;
		public IObservableValue<Integer> preceedingGap = new WritableValue<Integer>();
		
		public TextDataLineMatch(int lineNumber, String line) {
			this.lineNumber = lineNumber;
			this.line = line;
		}

		public void setPreceedingGap(int gap) {
			// UI will listen and update layout objects
			preceedingGap.setValue(gap);
		}
	}

	private DocumentMatcher createMatcherFromResource(String resourceName) {
		ClassLoader classLoader = getClass().getClassLoader();
		URL resource = classLoader.getResource(resourceName);
		try (InputStream txrInputStream = resource.openStream()) {
			return new DocumentMatcher(txrInputStream, "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (TxrErrorInDocumentException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private void runMatcher() {


		while (txrEditorComposite.getChildren().length != 0) {
			txrEditorComposite.getChildren()[0].dispose();
		}

		MatchPair results = matcher.process2(this.testData);



		while (testDataComposite.getChildren().length != 0) {
			testDataComposite.getChildren()[0].dispose();
		}

//		{
			textDataLineMatches = new ArrayList<>();
//			int textDataLineNumber = 0;
//			for (String line : testData) {
//				TextDataLineMatch lineMatch = new TextDataLineMatch(textDataLineNumber, line);
//				textDataLineMatches.add(lineMatch);
//				textDataLineNumber++;
//
//				Control lineControl = textDataLineRowComposite(testDataComposite, textDataLineNumber, line);
//				int rowHeight = lineControl.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
//				lineControl.setLayoutData(new RowData(SWT.DEFAULT, rowHeight));
//				
//				lineMatch.preceedingGap.addValueChangeListener(new IValueChangeListener<Integer>() {
//					@Override
//					public void handleValueChange(ValueChangeEvent<? extends Integer> event) {
//						lineControl.setLayoutData(new RowData(SWT.DEFAULT, rowHeight * (event.diff.getNewValue() + 1)));
//					}
//				});
//			}
//		}
		
		
		
		int[] currentDataLineIndex = { -1 };
		
		txrLineMatches = new ArrayList<>();
//		int lineNumber = 0;
		String[] txrLines = txr.split("\n");

		final int fudgeFactor = 3;

		results.matcherResults.createControls(new IControlCallback() {
			@Override
			public void createMatch(int txrLineIndex, int textDataLineIndex, int indentation) {
				System.out.println("create match: " + txrLineIndex + ", " + textDataLineIndex);
				System.out.println("     " + txrLines[txrLineIndex]);
				System.out.println("     " + testData[textDataLineIndex]);

				TxrLineMatch txrLineMatch = new TxrLineMatch(txrLineIndex + 1, txrLines[txrLineIndex]);
				txrLineMatches.add(txrLineMatch);

				// Push forward to textDataLineIndex
				do {
					int textDataLineIndexToOutput = currentDataLineIndex[0] + 1; 
					
					String line = testData[textDataLineIndexToOutput];
					TextDataLineMatch lineMatch = new TextDataLineMatch(textDataLineIndexToOutput, line);
					textDataLineMatches.add(lineMatch);

					Control lineControl = textDataLineRowComposite(testDataComposite, textDataLineIndexToOutput + 1, line);
					int rowHeight = lineControl.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
					lineControl.setLayoutData(new RowData(SWT.DEFAULT, rowHeight));
					
					lineMatch.preceedingGap.addValueChangeListener(new IValueChangeListener<Integer>() {
						@Override
						public void handleValueChange(ValueChangeEvent<? extends Integer> event) {
							lineControl.setLayoutData(new RowData(SWT.DEFAULT, rowHeight + (rowHeight + fudgeFactor) * event.diff.getNewValue()));
						}
					});
					
					currentDataLineIndex[0]++;
					System.out.println(" now " + currentDataLineIndex[0]);
				} while (currentDataLineIndex[0] < textDataLineIndex);
				
				txrLineMatch.textDataLineInstanceIndex = textDataLineMatches.size() - 1;

				Thread[] scrollingThread = new Thread[1];
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


							// Start a thread that updates
							new Thread(new String()).run();
							scrollingThread[0] = new Thread() {
								@Override
								public void run() {
								}
							};
							scrollingThread[0].start();
							break;
						case SWT.MouseMove:
							if (scrollingThread[0] != null) {
								scrollingThread[0].stop();
							}
							if (offset[0] != null) {
								/* Determine the mouse position relative to both the position where it
				        	  	   was when we last processed it and also the position within the TXR editor
				        	  	   area.
								 */
								int movement = event.y - offset[0].y;
								offset[0].y = event.y;

								Rectangle editorArea = txrEditorComposite.getClientArea();
								float positionWithinEditor = ((float)event.y - editorArea.y) / editorArea.height;

								txrLineMatch.setOffset(movement, positionWithinEditor);

								txrEditorComposite.layout();
							}

							break;
						case SWT.MouseUp:
							offset [0] = null;
							break;
						}
					}
				};

				Control lineControl = txrLineRowComposite(txrEditorComposite, txrLineIndex + 1, txrLines[txrLineIndex], indentation, listener);
				int rowHeight = lineControl.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
				lineControl.setLayoutData(new RowData(SWT.DEFAULT, rowHeight));
				
				txrLineMatch.preceedingGap.addValueChangeListener(new IValueChangeListener<Integer>() {
					@Override
					public void handleValueChange(ValueChangeEvent<? extends Integer> event) {
						lineControl.setLayoutData(new RowData(SWT.DEFAULT, rowHeight + (rowHeight + fudgeFactor) * event.diff.getNewValue()));
					}
				});
			}

			@Override
			public void createDirective(int txrLineIndex, int textDataLineIndex, int indentation) {
				System.out.println("create directive: " + txrLineIndex + ", " + textDataLineIndex);
				System.out.println("     " + txrLines[txrLineIndex]);
				System.out.println("     " + testData[textDataLineIndex]);

				TxrLineMatch txrLineMatch = new TxrLineMatch(txrLineIndex + 1, txrLines[txrLineIndex]);
				txrLineMatches.add(txrLineMatch);

				/*
				 * A directive has a line position in the text data but it does not match a line,
				 * so there will always be a blank space to the right of a directive. 
				 */
				
				// Push forward to textDataLineIndex
				// TODO this is duplicated above
				// In the case of a directive, we might already be on the correct line.
				// The directive does not itself match the line, so we don't push the matching line out.
				// Now currentDataLineIndex is last line actually pushed out, and textDataLineIndex is the one 
				// to be pushed next,
				// so if they are one apart then we are already current.
				while (currentDataLineIndex[0] < textDataLineIndex - 1) {
					int textDataLineIndexToOutput = currentDataLineIndex[0] + 1; 
					
					String line = testData[textDataLineIndexToOutput];
					TextDataLineMatch lineMatch = new TextDataLineMatch(textDataLineIndexToOutput + 1, line);
					textDataLineMatches.add(lineMatch);

					Control lineControl = textDataLineRowComposite(testDataComposite, textDataLineIndexToOutput + 1, line);
					int rowHeight = lineControl.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
					lineControl.setLayoutData(new RowData(SWT.DEFAULT, rowHeight));
					
					lineMatch.preceedingGap.addValueChangeListener(new IValueChangeListener<Integer>() {
						@Override
						public void handleValueChange(ValueChangeEvent<? extends Integer> event) {
							lineControl.setLayoutData(new RowData(SWT.DEFAULT, rowHeight + (rowHeight + fudgeFactor) * event.diff.getNewValue()));
						}
					});
					
					currentDataLineIndex[0]++;
					System.out.println(" now " + currentDataLineIndex[0]);
				};
				
				// We have not pushed the matching line out, so go right to end of array (no subtraction of one)
				txrLineMatch.textDataLineInstanceIndex = textDataLineMatches.size();
				
				txrLineMatch.isDirective = true;

				Listener listener = new Listener() {
					@Override
					public void handleEvent(Event event) {
						// TODO Auto-generated method stub
						
					}
				};
				
				Control lineControl = txrLineRowComposite(txrEditorComposite, txrLineIndex + 1, txrLines[txrLineIndex], indentation, listener);
				int rowHeight = lineControl.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
				lineControl.setLayoutData(new RowData(SWT.DEFAULT, rowHeight));
				
				txrLineMatch.preceedingGap.addValueChangeListener(new IValueChangeListener<Integer>() {
					@Override
					public void handleValueChange(ValueChangeEvent<? extends Integer> event) {
						lineControl.setLayoutData(new RowData(SWT.DEFAULT, rowHeight + (rowHeight + fudgeFactor) * event.diff.getNewValue()));
					}
				});
			}

			@Override
			public void createMismatch(int txrLineIndex, int textDataLineIndex, int indentation, String message) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void rewind(int textDataLineIndex) {
				// As currentDataLineIndex is the last line pushed out, and we want to push out again the line
				// we are rewinding to, hence we subtract one.
				System.out.println("rewinding: " + currentDataLineIndex[0] + " to " + (textDataLineIndex - 1));
				currentDataLineIndex[0] = textDataLineIndex - 1;
			}
		});

		txrEditorComposite.layout();


		// Set heights so that all matched lines in the TXR are lined up correctly with the
		// line from the text data.
		
		int txrLineLocation = 0;  // Location of last line processed, 1 is top position etc
		int textDataLineLocation = 0;  // Location of last line processed, 1 is top position etc
		int textDataLineInstanceIndex = -1;  // Last data line processed, 0-based
		for (TxrLineMatch txrLineMatch : txrLineMatches) {
			if (txrLineMatch.textDataLineInstanceIndex == null) {
				// This line is not matched, so it just goes immediately after the previous
				txrLineMatch.setPreceedingGap(0);
				txrLineLocation++;
			} else {
				assert (txrLineMatch.textDataLineInstanceIndex > textDataLineInstanceIndex); // Must move forwards
				
				while (textDataLineInstanceIndex < txrLineMatch.textDataLineInstanceIndex - 1) {
					textDataLineMatches.get(textDataLineInstanceIndex).setPreceedingGap(0);
					textDataLineInstanceIndex++;
					textDataLineLocation++;
				}
				
				if (txrLineMatch.isDirective) {
					// Update location to be where the matching lines would go if there were no gaps
					txrLineLocation++;
					
					// This TXR line is a directive, so put it on a line of its own (no data line)
					if (textDataLineLocation > txrLineLocation) {
						// We need to add space on the left to line these up
						txrLineMatch.setPreceedingGap(textDataLineLocation - txrLineLocation);
						txrLineLocation = textDataLineLocation;
					}
				} else {
					// Update location to be where the matching lines would go if there were no gaps
					txrLineLocation++;
					textDataLineLocation++;
					
					textDataLineInstanceIndex++;

					// Match up lines, so both appear lined up
					if (textDataLineLocation < txrLineLocation) {
						// We need to add space on the right to line these up
						textDataLineMatches.get(textDataLineInstanceIndex).setPreceedingGap(txrLineLocation - textDataLineLocation);
						txrLineMatch.setPreceedingGap(0);
						textDataLineLocation = txrLineLocation;
					} else {
						// We need to add space on the left to line these up (or no space is needed on either side)
						if (textDataLineInstanceIndex < textDataLineMatches.size())  // Should this test really be needed???
							textDataLineMatches.get(textDataLineInstanceIndex).setPreceedingGap(0);
						txrLineMatch.setPreceedingGap(textDataLineLocation - txrLineLocation);
						txrLineLocation = textDataLineLocation;
					}
					assert (textDataLineInstanceIndex == txrLineMatch.textDataLineInstanceIndex);
				}
			}
		}
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

		runMatcher(); // This sets it all up.
		
		return containerOfSash;
	}	

	private Control createTxrEditorArea(Composite parent) {
		txrEditorComposite = new Composite(parent, SWT.NONE);
		RowLayout rowLayout = new RowLayout(SWT.VERTICAL);
		rowLayout.wrap = false;
		txrEditorComposite.setLayout(rowLayout);

//		for (String line : txr.split("\n")) {
//			Text lineControl = new Text(txrEditorComposite, SWT.NONE);
//			lineControl.setText(line);
//		}

		return txrEditorComposite;
	}

	private Control createTestDataArea(Composite parent) {
		testDataComposite = new Composite(parent, SWT.NONE);
		RowLayout rowLayout = new RowLayout(SWT.VERTICAL);
		rowLayout.wrap = false;
		testDataComposite.setLayout(rowLayout);

//		for (String line : testData.split("\n")) {
//			Label lineControl = new Label(testDataComposite, SWT.NONE);
//			lineControl.setText(line);
//		}

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
