/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2006 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.importer.matcher;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.databinding.observable.set.ISetChangeListener;
import org.eclipse.core.databinding.observable.set.SetChangeEvent;
import org.eclipse.core.databinding.observable.value.ComputedValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogMessageArea;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.internal.databinding.provisional.swt.ControlCreator;
import org.eclipse.jface.internal.databinding.provisional.swt.UpdatingComposite;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ComboBoxViewerCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TableViewerFocusCellManager;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

import net.sf.jmoney.fields.AccountControl;
import net.sf.jmoney.importer.Activator;
import net.sf.jmoney.importer.model.MemoPattern;
import net.sf.jmoney.importer.model.MemoPatternInfo;
import net.sf.jmoney.importer.model.TransactionType;
import net.sf.jmoney.isolation.ObjectCollection;
import net.sf.jmoney.isolation.ReferenceViolationException;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Session;

/**
 * A dialog that allows the user to see how imported entries will be matched using the account's pattern matching rules.  The user may edit the pattern matching
 * rules in this dialog and see how the matching of the imported entries change.
 *
 * @author Nigel Westbury
 */
public class PatternMatchingDialog<T extends BaseEntryData> extends Dialog {

	public class ParametersComposite extends UpdatingComposite {

		LabelCreator labelCreator = new LabelCreator(this);

		public ParametersComposite(Composite parent, int style) {
			super(parent, style);
			setLayout(new GridLayout(2, false));
			createAndTrackControls();
		}

		@Override
		protected void createControls() {
			MemoPattern pattern = (MemoPattern)ViewerProperties.singleSelection().observe(patternViewer).getValue();
			String transactionTypeId = MemoPatternInfo.getTransactionTypeIdAccessor().observe(pattern).getValue();
			
			TransactionType<T> transType = lookupTransactionType(transactionTypeId);
			if (transType == null) {
				return;
			}

//			IObservableMap<String, String> transactionParameterValues = pattern.getTransactionParameterValueMap();

			T entryData = (T)ViewerProperties.<T>singleSelection().observe(entriesViewer).getValue();

			Object [] args = null;
			
			if (entryData != null) {

				boolean unmatchedFound = false;
				for (ImportEntryProperty<T> importEntryProperty : importEntryProperties) {
					String importEntryPropertyValue = importEntryProperty.getCurrentValue(entryData);
					Pattern compiledPattern = pattern.getCompiledPattern(importEntryProperty.id);

					if (compiledPattern != null) {
						Matcher m = compiledPattern.matcher(importEntryPropertyValue);
						if (!m.matches()) {
							unmatchedFound = true;
							break;
						}

						/*
						 * Only 'memo' provides arguments.
						 */
						if (importEntryProperty.id.equals("memo")) {
							/*
							 * Group zero is the entire string and the groupCount method
							 * does not include that group, so there is really one more group
							 * than the number given by groupCount.
							 *
							 * This code also tidies up the imported text.
							 */
							args = new Object[m.groupCount()+1];
							for (int i = 0; i <= m.groupCount(); i++) {
								// Not sure why it can be null, but it happened...
								args[i] = m.group(i) == null ? null : ImportMatcher.convertToMixedCase(m.group(i));
							}
						}
					}
				}
				
				if (unmatchedFound) {
					// Set args back to null, because null should be set if any don't match,
					// even if the memo pattern does match.
					args = null;
				}
			}
			
			GridDataFactory textLayoutData = GridDataFactory.fillDefaults().minSize(200, SWT.DEFAULT);
			
			for (TransactionParamMetadata paramMetadata : transType.getParameters()) {
				Label label = labelCreator.create();
				label.setText(paramMetadata.getName() + ":");

				TextboxCreator textboxCreator = new TextboxCreator(this, paramMetadata);
				Control textbox = textboxCreator.create();
				textLayoutData.applyTo(textbox);
			}
		}
	}

	public class LabelCreator extends ControlCreator<Label> {
		public LabelCreator(ParametersComposite updatingComposite) {
			super(updatingComposite, Label.class);
		}

		@Override
		public Label createControl() {
			return new Label(parent, SWT.NONE);
		}
	}

	public class TextboxCreator extends ControlCreator<Control> {
		private TransactionParamMetadata paramMetadata;
		
		public TextboxCreator(ParametersComposite updatingComposite, TransactionParamMetadata paramMetadata) {
			super(updatingComposite, Control.class);
			this.paramMetadata = paramMetadata;
		}

		@Override
		public Control createControl() {
			return paramMetadata.createControl(parent, ViewerProperties.singleSelection().observe(patternViewer), args);
		}
		
		@Override
		public boolean equals(Object other) {
			if (other instanceof PatternMatchingDialog.TextboxCreator) {
				return ((PatternMatchingDialog.TextboxCreator)other).paramMetadata.equals(paramMetadata);
			} else {
				return false;
			}
		}
		
		@Override
		public int hashCode() {
			return paramMetadata.hashCode();
		}
	}

	private static final int SAVE_PATTERNS_ONLY_ID = IDialogConstants.CLIENT_ID + 0;

	// Any value except Window.OK and Window.CANCEL
	public static final int SAVE_PATTERNS_ONLY = 2;

//	private TransactionManager transactionManager;

	/**
	 * The account for which we are configuring, which is in our own transaction.
	 */
	final IPatternMatcher account;

	private DialogMessageArea messageArea;

	TableViewer entriesViewer;

	TableViewer patternViewer;

	public IObservableValue<String[]> args = new ComputedValue<String[]>() {
		@Override
		protected String[] calculate() {
			BaseEntryData entryData = (BaseEntryData)ViewerProperties.<BaseEntryData>singleSelection().observe(entriesViewer).getValue();
			MemoPattern pattern = (MemoPattern)ViewerProperties.<MemoPattern>singleSelection().observe(patternViewer).getValue();

			/*
			 * The pattern may not yet have been entered if the user has just added a new
			 * pattern.
			 */
			if (pattern != null && entryData != null) {
				Pattern compiledPattern = pattern.getCompiledPattern("memo");
				Matcher m = compiledPattern.matcher(entryData.getTextForRegexMatching());
				if (m.matches()) {
					/*
					 * Group zero is the entire string and the groupCount method
					 * does not include that group, so there is really one more group
					 * than the number given by groupCount.
					 *
					 * This code also tidies up the imported text.
					 */
					String [] args = new String[m.groupCount()+1];
					for (int i = 0; i <= m.groupCount(); i++) {
						// Not sure why it can be null, but it happened...
						args[i] = m.group(i) == null ? null : ImportMatcher.convertToMixedCase(m.group(i));
					}
					return args;
				}
			}
			
			return null;
		}
	};
	
	AccountControl<IncomeExpenseAccount> defaultAccountControl;

	Image errorImage;

	/**
	 * When adding new patterns, we add to the end by default.
	 * We must set an index that is more than all prior ordering
	 * indexes.  This field contains the lowest integer that is
	 * more than all existing values (or 0 if no patterns
	 * currently exist).
	 */
	int nextOrderingIndex;

	private Collection<T> sampleEntries;

	private ArrayList<MemoPattern> sortedPatterns;

	private List<ImportEntryProperty<T>> importEntryProperties;
	
	private List<TransactionType<T>> applicableTransactionTypes;

	/**
	 * Creates an input dialog with OK and Cancel buttons. Note that the dialog
	 * will have no visual representation (no widgets) until it is told to open.
	 * <p>
	 * Note that the <code>open</code> method blocks for input dialogs.
	 * </p>
	 * All changes within this dialog are made within a transaction, so canceling
	 * is trivial (the transaction is simply not committed).
	 *
	 * @param parentShell
	 *            the parent shell
	 * @param importedEntries
	 */
	public PatternMatchingDialog(Shell parentShell, IPatternMatcher matcherInsideTransaction, Collection<T> sampleEntries, List<ImportEntryProperty<T>> importEntryProperties, List<TransactionType<T>> applicableTransactionTypes) {
		super(parentShell);
		this.sampleEntries = sampleEntries;
		this.importEntryProperties = importEntryProperties;
		this.applicableTransactionTypes = applicableTransactionTypes;
		
		this.account = matcherInsideTransaction;
		
		sortedPatterns = new ArrayList<MemoPattern>(account.getPatternCollection());
		Collections.sort(sortedPatterns, new Comparator<MemoPattern>(){
			@Override
			public int compare(MemoPattern pattern1, MemoPattern pattern2) {
				return pattern1.getOrderingIndex() - pattern2.getOrderingIndex();
			}
		});

		// Load the error indicator
		URL installURL = Activator.getDefault().getBundle().getEntry("/icons/error.gif");
		errorImage = ImageDescriptor.createFromURL(installURL).createImage();

		// Find an ordering index that is greater than all existing ordering indexes,
		// so new patterns can be added after all others.
		nextOrderingIndex = 0;
		for (MemoPattern pattern: account.getPatternCollection()) {
			if (nextOrderingIndex <= pattern.getOrderingIndex()) {
				nextOrderingIndex = pattern.getOrderingIndex() + 1;
			}
		}
	}

	@Override
	protected void buttonPressed(int buttonId) {
		switch (buttonId) {
		case IDialogConstants.OK_ID:
			okPressed();
			break;
		case IDialogConstants.CANCEL_ID:
			cancelPressed();
			break;
		case SAVE_PATTERNS_ONLY_ID:
			setReturnCode(SAVE_PATTERNS_ONLY);
			close();
			break;
		}
		super.buttonPressed(buttonId);
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Import Options for " + account.getName());
	}

	@Override
	public boolean close() {
		boolean closed = super.close();

		// Dispose the image
		if (closed) {
			errorImage.dispose();
		}

		return closed;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID,
				IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
		createButton(parent, SAVE_PATTERNS_ONLY_ID,
				"Save Patterns Only", false);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		composite.setLayout(new GridLayout(1, false));

		// Message label
		messageArea = new DialogMessageArea();
		messageArea.createContents(composite);

		// Ensure the message area is shown and fills the space
		messageArea.setTitleLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		messageArea.setMessageLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		messageArea.showTitle("Options for Statement Import", null);
		/*
		 * It should not be possible for there to be errors when the dialog box is first
		 * opened, because we don't allow the user to save the data when there are errors.
		 * However, just in case, we ensure that any errors are shown.
		 */
		updateErrorMessage();

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 200;
		gd.minimumHeight = 100;
		createSampleEntryArea(composite).setLayoutData(gd);

		Label label = new Label(composite, SWT.WRAP);
		label.setText("JMoney allows you to import bank account statements from the bank's servers. " +
				"Before these records can be imported into JMoney, you must specify categories that are to be assigned to each entry " +
				"because a requirement of JMoney is that all entries have an account or category assigned. " +
				"Categories can be assigned based on regex pattern matching.");

		GridData messageData = new GridData();
		Rectangle rect = getShell().getMonitor().getClientArea();
		messageData.widthHint = rect.width/2;
		label.setLayoutData(messageData);

		// Create the control containing the matching options
		Control whenIsReconcilableControl = createCategoryControls(composite);
		GridData containerData = new GridData(SWT.FILL, SWT.FILL, true, true);
		containerData.heightHint = 200;
		containerData.minimumHeight = 100;
		whenIsReconcilableControl.setLayoutData(containerData);

		defaultAccountControl.setAccount(account.getDefaultCategory());

		defaultAccountControl.account.addValueChangeListener(
				new IValueChangeListener<IncomeExpenseAccount>() {
					@Override
					public void handleValueChange(ValueChangeEvent<? extends IncomeExpenseAccount> event) {
						IncomeExpenseAccount defaultCategory = defaultAccountControl.getAccount();
						account.setDefaultCategory(defaultCategory);
						updateErrorMessage();
					}
				});
		applyDialogFont(composite);
		return composite;
	}

	private Control createSampleEntryArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());

		// Create the table of sample entries
		Control patternMatchingTableControl = createSampleEntriesTableControl(composite);
		GridData tableData = new GridData(SWT.FILL, SWT.FILL, true, true);
		tableData.grabExcessHorizontalSpace = true;
		tableData.grabExcessVerticalSpace = true;
		patternMatchingTableControl.setLayoutData(tableData);

		return composite;
	}

	private Control createCategoryControls(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		// Create the table of patterns
		Control patternMatchingTableControl = createPatternMatchingTableControl(composite);
		GridData tableData = new GridData(SWT.FILL, SWT.FILL, true, true);
		tableData.horizontalSpan = 2;
		tableData.grabExcessHorizontalSpace = true;
		tableData.grabExcessVerticalSpace = true;
		tableData.minimumHeight = 200;

		patternMatchingTableControl.setLayoutData(tableData);

		// The default category, if no rule matches

		new Label(composite, SWT.NONE).setText("Default category:");
		defaultAccountControl = new AccountControl<IncomeExpenseAccount>(composite, IncomeExpenseAccount.class) {
			@Override
			protected Session getSession() {
				return PatternMatchingDialog.this.account.getBaseObject().getSession();
			}
		};
		GridData accountData = new GridData();
		accountData.widthHint = 200;
		defaultAccountControl.setLayoutData(accountData);

		return composite;
	}

	private Control createSampleEntriesTableControl(Composite parent) {
		// Create the table viewer to display the sample entries
		entriesViewer = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION);

		// Set up the table
		final Table table = entriesViewer.getTable();

		// Turn on the header and the lines
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		// Set the content and label providers
		entriesViewer.setContentProvider(ArrayContentProvider.getInstance());
		//		viewer.setSorter(new PatternSorter());

		ColumnViewerToolTipSupport.enableFor(entriesViewer);

		// Add the columns for the input properties that may have patterns
		for (ImportEntryProperty<T> importEntryProperty : importEntryProperties) {
			addColumnForExampleInputMatcher(importEntryProperty, "<html>The pattern is a Java regular expression that is matched against the memo in the downloadable file.<br>For each record from the bank, the first row in this table with a matching pattern is used.</html>");
		}
		
		
//		TableViewerColumn column1 = new TableViewerColumn(entriesViewer, SWT.LEFT);
//		column1.getColumn().setWidth(40);
//		column1.getColumn().setText("Description");
//		column1.setLabelProvider(new CellLabelProvider() {
//			@Override
//			public void update(ViewerCell cell) {
//				BaseEntryData pattern = (BaseEntryData)cell.getElement();
//				cell.setText(pattern.getDefaultMemo());
//			}
//		});
//
//		// TODO don't hard code this.
//		final Currency currency = account.getBaseObject().getSession().getCurrencyForCode("GBP");
//
//		TableViewerColumn column2 = new TableViewerColumn(entriesViewer, SWT.RIGHT);
//		column2.getColumn().setWidth(30);
//		column2.getColumn().setText("Amount");
//		column2.setLabelProvider(new CellLabelProvider() {
//			@Override
//			public void update(ViewerCell cell) {
//				BaseEntryData pattern = (BaseEntryData)cell.getElement();
//				cell.setText(currency.format(pattern.amount));
//			}
//		});

		addColumn2(MemoPatternInfo.getOrderingIndexAccessor(), "The pattern index.");
		addColumn2(MemoPatternInfo.getTransactionTypeIdAccessor(), "The transaction type.");

		/*
		 * Build a list of all the columns. We use LinkedHashMap because that
		 * keeps the order of elements the same as the order in which they were
		 * added to the map. This results in the columns being in a more
		 * sensible order.
		 * 
		 * Note that we keep just a set of parameter names. We don't try to
		 * build a map of names to TransactionParamMetadata objects. This is
		 * because there may be different TransactionParamMetadata objects with
		 * the same name but in different transaction types. We put these in the
		 * same column but we must use in the label provider the correct
		 * TransactionParamMetadata for the transaction type.
		 */
		LinkedHashSet<String> allParams = new LinkedHashSet<String>();
		for (TransactionType<T> transactionType : applicableTransactionTypes) {
			for (TransactionParamMetadata paramMetadata : transactionType.getParameters()) {
				allParams.add(paramMetadata.getName());
			}
		}
		for (String paramName : allParams) {
			addColumnForParameter(paramName);
		}

		entriesViewer.setInput(sampleEntries);

		// Pack the columns
		for (int i = 0, n = table.getColumnCount(); i < n; i++) {
			table.getColumn(i).pack();
		}

		// Listen for changes to the patterns and refresh when changed.
		// Bit of a hack...
		account.getPatternCollection().addSetChangeListener(new ISetChangeListener<MemoPattern>() {
			@Override
			public void handleSetChange(SetChangeEvent<? extends MemoPattern> event) {
				updateSampleEntriesTable();
				updateErrorMessage();
				
				for (MemoPattern pattern : event.diff.getAdditions()) {
					pattern.addPropertyChangeListener(new PropertyChangeListener() {
						@Override
						public void propertyChange(PropertyChangeEvent event) {
							updateSampleEntriesTable();
							updateErrorMessage();
						}
					});
				}
			}
		});
		for (MemoPattern pattern : account.getPatternCollection()) {
			final PropertyChangeListener listener = new PropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent event) {
					updateSampleEntriesTable();
					updateErrorMessage();
				}
			};
			entriesViewer.getControl().addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent e) {
					pattern.removePropertyChangeListener(listener);
				}
			});
			pattern.addPropertyChangeListener(listener);
		}

		return table;
	}

	private void addColumnForExampleInputMatcher(final ImportEntryProperty<T> importEntryProperty, String tooltip) {
		TableViewerColumn column = new TableViewerColumn(entriesViewer, SWT.LEFT);
		column.getColumn().setWidth(100);
		column.getColumn().setText(importEntryProperty.label + " Input");
		column.getColumn().setToolTipText(tooltip);

		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				T entryData = (T)element;
				return importEntryProperty.getCurrentValue(entryData);
			}
		});
	}
	
	private Control createPatternMatchingTableControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(3, false));

		// Create the table viewer to display the pattern matching rules
		patternViewer = new TableViewer(composite, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);

		// Set up the table
		final Table table = patternViewer.getTable();
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		TableViewerFocusCellManager focusCellManager = new TableViewerFocusCellManager(patternViewer, new FocusCellOwnerDrawHighlighter(patternViewer));
		ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(patternViewer) {
			@Override
			protected boolean isEditorActivationEvent(
					ColumnViewerEditorActivationEvent event) {
				return event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL
						|| event.eventType == ColumnViewerEditorActivationEvent.MOUSE_CLICK_SELECTION
						|| (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && event.keyCode == SWT.CR)
						|| event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
			}
		};

		TableViewerEditor.create(patternViewer, focusCellManager, actSupport, ColumnViewerEditor.TABBING_HORIZONTAL
				| ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR
				| ColumnViewerEditor.TABBING_VERTICAL
				| ColumnViewerEditor.KEYBOARD_ACTIVATION);

		// Set the content and label providers
		patternViewer.setContentProvider(new PatternContentProvider());
		patternViewer.setSorter(new PatternSorter());

		ColumnViewerToolTipSupport.enableFor(patternViewer);

		// Add the columns
		TableViewerColumn column1 = new TableViewerColumn(patternViewer, SWT.LEFT);
		column1.getColumn().setWidth(40);
		column1.getColumn().setText("");
		column1.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				MemoPattern pattern = (MemoPattern)cell.getElement();
				if (isMemoPatternValid(pattern) != null) {
					cell.setImage(errorImage);
				} else {
					cell.setImage(null);
				}
			}

			@Override
			public String getToolTipText(Object element) {
				MemoPattern pattern = (MemoPattern)element;
				return isMemoPatternValid(pattern);
			}
		});

		column1.setEditingSupport(new EditingSupport(patternViewer) {
			@Override
			protected boolean canEdit(Object element) {
				return false;
			}

			@Override
			protected CellEditor getCellEditor(Object element) {
				return null;
			}

			@Override
			protected Object getValue(Object element) {
				return null;
			}

			@Override
			protected void setValue(Object element, Object value) {
			}
		});

		addColumn(MemoPatternInfo.getOrderingIndexAccessor(), "The pattern index, used in the above imported entries table to indicate the matching pattern.");

		for (ImportEntryProperty<T> importEntryProperty : importEntryProperties) {
			addColumn(importEntryProperty, "<html>The pattern is a Java regular expression that is matched against the memo in the downloadable file.<br>For each record from the bank, the first row in this table with a matching pattern is used.</html>");
		}
		
		addColumn(MemoPatternInfo.getTransactionTypeIdAccessor(), "The id for the transaction type.  The values of this property must be a type supported by the account type.");
		addColumn(MemoPatternInfo.getTransactionParameterValuesAccessor(), "Text that specifies the value of each parameter to the transaction, the parameter metadata depending on the transaction type.");
//		addColumn(MemoPatternInfo.getMemoAccessor(), "The value to be put in the memo field.  The values in this table may contain {0}, {1} etc. where the number matches the group number in the Java regular expression.");
//		addColumn(MemoPatternInfo.getAccountAccessor(), "The account to be used for entries that match this pattern.");
//		addColumn(MemoPatternInfo.getDescriptionAccessor(), "The value to be put in the description field.  The values in this table may contain {0}, [1} etc. where the number matches the group number in the Java regular expression.");

		/*
		 * Set the account as the input object that contains the list of pattern
		 * matching rules.
		 */
		patternViewer.setInput(account);

		// Pack the columns
		for (int i = 0, n = table.getColumnCount(); i < n; i++) {
			table.getColumn(i).pack();
		}

		// Turn on the header and the lines
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		// Create the area with the parameter values
		Control parametersAreaControl = createParametersArea(composite);
		GridData layoutData = new GridData(SWT.LEFT, SWT.FILL, true, true);
		layoutData.minimumHeight = 150;
		layoutData.minimumWidth = 400;
		parametersAreaControl.setLayoutData(layoutData);

		// Create the button area
		Control buttonAreaControl = createButtonArea(composite);
//		buttonAreaControl.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));

		return composite;
	}

	/**
	 *
	 * @param pattern
	 * @return null if the pattern is valid, or a string describing the error
	 * 		if the pattern is not valid
	 */
	protected String isMemoPatternValid(MemoPattern pattern) {
		if (pattern.getPattern() == null) {
			return "No regex pattern has been entered";
		}

		try {
			Pattern.compile(pattern.getPattern());
		} catch (PatternSyntaxException e) {
			return "The regex pattern is not valid: " + e.getDescription();
		}
		
		String typeId = pattern.getTransactionTypeId();
		if (typeId == null) {
			return "Transaction type has not been selected";
		}

		if (typeId.equals("basic") && pattern.getAccount() == null) {
			return "No account has been entered";
		}

		return null;
	}

	// Used for ordering index only and transaction type only
	private void addColumn2(final ScalarPropertyAccessor<?,MemoPattern> propertyAccessor, String tooltip) {
		TableViewerColumn column = new TableViewerColumn(entriesViewer, SWT.LEFT);
		column.getColumn().setWidth(propertyAccessor.getMinimumWidth());
		column.getColumn().setToolTipText(tooltip);

		if (propertyAccessor == MemoPatternInfo.getOrderingIndexAccessor()) {
			column.getColumn().setText("Pattern Index");
		} else if (propertyAccessor == MemoPatternInfo.getTransactionTypeIdAccessor()) {
			column.getColumn().setText(propertyAccessor.getDisplayName());
		}

		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				BaseEntryData entryData = (BaseEntryData)element;

				for (MemoPattern pattern: sortedPatterns) {
					/*
					 * The pattern may not yet have been entered if the user has just added a new
					 * pattern.
					 */
					boolean unmatchedFound = false;
					Object [] args = null;
					for (ImportEntryProperty importEntryProperty : importEntryProperties) {
						String importEntryPropertyValue = importEntryProperty.getCurrentValue(entryData);
						Pattern compiledPattern = pattern.getCompiledPattern(importEntryProperty.id);
						
						if (compiledPattern != null && importEntryPropertyValue != null) {
							Matcher m = compiledPattern.matcher(importEntryPropertyValue);
							if (!m.matches()) {
								unmatchedFound = true;
								break;
							}

							/*
							 * Only 'memo' provides arguments.
							 */
							if (importEntryProperty.id.equals("memo")) {
								/*
								 * Group zero is the entire string and the groupCount method
								 * does not include that group, so there is really one more group
								 * than the number given by groupCount.
								 *
								 * This code also tidies up the imported text.
								 */
								args = new Object[m.groupCount()+1];
								for (int i = 0; i <= m.groupCount(); i++) {
									// Not sure why it can be null, but it happened...
									args[i] = m.group(i) == null ? null : ImportMatcher.convertToMixedCase(m.group(i));
								}
							}
						}
					}
					
					if (!unmatchedFound) {
						if (propertyAccessor == MemoPatternInfo.getOrderingIndexAccessor()) {
							return Integer.toString(pattern.getOrderingIndex());
						} else if (propertyAccessor == MemoPatternInfo.getTransactionTypeIdAccessor()) {
							return pattern.getTransactionTypeId() == null
									? ""
											: lookupTransactionType(pattern.getTransactionTypeId()).getLabel();
						}
					}
				}

				return "no match";
			}

		});
	}

	/**
	 * Adds columns for the parameters.  The column contains the value that will be
	 * used for the parameter after substitution markers have been replaced by matches
	 * from the import data.
	 * <P>
	 * Note that the column contains information for all parameters with a given parameter
	 * label.  Thus two parameters with the same label but that are used in different transaction
	 * types will share the same column.
	 *
	 * @param parameterName the name of the parameters being shown in this column
	 */
	private void addColumnForParameter(final String parameterName) {
		String tooltip = MessageFormat.format(
				"The value to be used for the {0}.  The values in this table may contain {0}, [1} etc. where the number matches the group number in the Java regular expression.",
				parameterName);
		
		TableViewerColumn column = new TableViewerColumn(entriesViewer, SWT.LEFT);
		column.getColumn().setWidth(200);
		column.getColumn().setText(parameterName);
		column.getColumn().setToolTipText(tooltip);

		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				BaseEntryData entryData = (BaseEntryData)element;

				MemoPattern matchingPattern = null;
				Object [] args = null;
				for (MemoPattern pattern: sortedPatterns) {
					/*
					 * The pattern may not yet have been entered if the user has just added a new
					 * pattern.
					 */
					boolean unmatchedFound = false;
					for (ImportEntryProperty importEntryProperty : importEntryProperties) {
						String importEntryPropertyValue = importEntryProperty.getCurrentValue(entryData);
						Pattern compiledPattern = pattern.getCompiledPattern(importEntryProperty.id);

						if (compiledPattern != null && importEntryPropertyValue != null) {
							Matcher m = compiledPattern.matcher(importEntryPropertyValue);
							if (!m.matches()) {
								unmatchedFound = true;
								break;
							}

							/*
							 * Only 'memo' provides arguments.
							 */
							if (importEntryProperty.id.equals("memo")) {
								/*
								 * Group zero is the entire string and the groupCount method
								 * does not include that group, so there is really one more group
								 * than the number given by groupCount.
								 *
								 * This code also tidies up the imported text.
								 */
								args = new Object[m.groupCount()+1];
								for (int i = 0; i <= m.groupCount(); i++) {
									// Not sure why it can be null, but it happened...
									args[i] = m.group(i) == null ? null : ImportMatcher.convertToMixedCase(m.group(i));
								}
							}
						}
					}
					
					if (!unmatchedFound) {
						matchingPattern = pattern;
						break;
					}
				}
				
				/*
				 * Now we know the pattern that matches, lookup the transaction type.
				 * See if there is a parameter in this transaction type that has a name
				 * that matches the name in this column. 
				 */
				if (matchingPattern != null) {
					if (matchingPattern.getTransactionTypeId() == null) {
						// User hasn't entered type yet.  It must be a new pattern being entered.
						return "";
					} else {
						TransactionType transType = lookupTransactionType(matchingPattern.getTransactionTypeId());
						TransactionParamMetadata paramMetadata = lookupParamByName(transType, parameterName);
						if (paramMetadata != null) {
							return paramMetadata.getResolvedValueAsString(matchingPattern, args);
						} else {
							return "N/A";
						}
					}
				} else {
					return "no match";
				}
			}

		});
	}

	private void addColumn(final ImportEntryProperty importEntryProperty, String tooltip) {
		TableViewerColumn column = new TableViewerColumn(patternViewer, SWT.LEFT);
		column.getColumn().setWidth(100);
		column.getColumn().setText(importEntryProperty.label + " Pattern");
		column.getColumn().setToolTipText(tooltip);

		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				MemoPattern pattern = (MemoPattern)element;
				return pattern.getPattern(importEntryProperty.id);
			}

		});
		
		EditingSupport editingSupport;
		
		editingSupport= new EditingSupport(patternViewer) {
			@Override
			protected boolean canEdit(Object element) {
				return true;
			}

			@Override
			protected CellEditor getCellEditor(Object element) {
				return new TextCellEditor(patternViewer.getTable());
			}

			@Override
			protected Object getValue(Object element) {
				// The text cell editor requires that null is never returned
				// by this method.
				MemoPattern pattern = (MemoPattern)element;
				Object value = pattern.getPattern(importEntryProperty.id);
				if (value == null) {
					value = "";
				}
				return value;
			}

			@Override
			protected void setValue(Object element, Object value) {
				MemoPattern pattern = (MemoPattern)element;
				// Update only if a value change, otherwise
				// it all goes horribly circular
				if (value != null && !value.equals(pattern.getPattern(importEntryProperty.id))) {
					pattern.setPattern(importEntryProperty.id, (String)value);

					updateSampleEntriesTable();
					updateErrorMessage();

					// This seems to be necessary.  I don't know why the cell editor stopped doing this.
					patternViewer.refresh(element);
				}
			}
		};
		column.setEditingSupport(editingSupport);
	}

	private void addColumn(final ScalarPropertyAccessor<?,MemoPattern> propertyAccessor, String tooltip) {
		TableViewerColumn column = new TableViewerColumn(patternViewer, SWT.LEFT);
		column.getColumn().setWidth(propertyAccessor.getMinimumWidth());
		column.getColumn().setToolTipText(tooltip);

		if (propertyAccessor == MemoPatternInfo.getOrderingIndexAccessor()) {
			column.getColumn().setText("Pattern Index");
		} else {
			column.getColumn().setText(propertyAccessor.getDisplayName());
		}

		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				MemoPattern pattern = (MemoPattern)element;
				return propertyAccessor.formatValueForTable(pattern);
			}

		});
		
		EditingSupport editingSupport;
		
		if (propertyAccessor == MemoPatternInfo.getTransactionTypeIdAccessor()) {
			editingSupport= new EditingSupport(patternViewer) {
				
				@Override
				protected boolean canEdit(Object element) {
					return true;
				}

				@Override
				protected CellEditor getCellEditor(Object element) {
					ComboBoxViewerCellEditor cellEditor = new ComboBoxViewerCellEditor(patternViewer.getTable());
					cellEditor.setContentProvider(ArrayContentProvider.getInstance());
					cellEditor.setLabelProvider(new LabelProvider() {
						@Override
						public String getText(Object element) {
							return ((TransactionType)element).getLabel();
						}
					});

					cellEditor.setInput(applicableTransactionTypes);
					
					return cellEditor;
				}

				@Override
				protected Object getValue(Object element) {
					MemoPattern pattern = (MemoPattern)element;
					String transactionTypeId = pattern.getTransactionTypeId();
					
					for (TransactionType transType : applicableTransactionTypes) {
						if (transType.getId().equals(transactionTypeId)) {
							return transType;
						}
					}
					return null;
				}

				@Override
				protected void setValue(Object element, Object value) {
					MemoPattern pattern = (MemoPattern)element;
					TransactionType transactionType = (TransactionType)value;
					
					// Update only if a value change, otherwise
					// it all goes horribly circular
					if (transactionType != null && !transactionType.getId().equals(pattern.getTransactionTypeId())) {
						pattern.setTransactionTypeId(transactionType.getId());
						patternViewer.update(element, null);

						updateSampleEntriesTable();
						updateErrorMessage();
					}
				}
			};
		} else {
			editingSupport = null;
		}
		column.setEditingSupport(editingSupport);
	}

	/**
	 * Update the table containing the sample entry matches.
	 * This is called whenever the matching rules are changed.
	 */
	protected void updateSampleEntriesTable() {
		entriesViewer.refresh(true);
	}

	private Control createParametersArea(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText("Extracted Values");
		FillLayout groupLayout = new FillLayout();
		group.setLayout(groupLayout);
		
		final ScrolledComposite sc = new ScrolledComposite(group, SWT.V_SCROLL | SWT.H_SCROLL);

		final Composite updatingComposite = new ParametersComposite(sc, SWT.NONE);

		sc.setContent(updatingComposite);	

		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);

		final IObservableValue<Point> observeSize = new ComputedValue<Point>() {
			@Override
			protected Point calculate() {
				return updatingComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			}
		};
		
		// TODO replace this by one-way binding to Bean minSize property???
		sc.setMinSize(observeSize.getValue());
		
		observeSize.addValueChangeListener(new IValueChangeListener<Point>() {
			@Override
			public void handleValueChange(ValueChangeEvent<? extends Point> event) {
				sc.setMinSize(observeSize.getValue());
			}
		});
		
		return group;
	}

	private Composite createButtonArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 30;
		container.setLayout(layout);

		Button addButton = new Button(container, SWT.PUSH);
		addButton.setText("Add Row");
		addButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ObjectCollection<MemoPattern> patterns = account.getPatternCollection();
				MemoPattern newPattern = patterns.createNewElement(MemoPatternInfo.getPropertySet());

				newPattern.setOrderingIndex(nextOrderingIndex++);
				sortedPatterns.add(newPattern);

				/*
				 * Add the new pattern to the end of the table.
				 */
				patternViewer.add(newPattern);

				entriesViewer.refresh(true);
			}
		});

		Button removeButton = new Button(container, SWT.PUSH);
		removeButton.setText("Remove Row");
		removeButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection ssel = (IStructuredSelection)patternViewer.getSelection();
				if (ssel.size() > 0) {
					try {
						ObjectCollection<MemoPattern> patterns = account.getPatternCollection();
						for (Iterator<?> iter = ssel.iterator(); iter.hasNext(); ) {
							MemoPattern pattern = (MemoPattern) iter.next();
							patterns.deleteElement(pattern);
						}

						/*
						 * We have deleted patterns but remaining patterns are
						 * not affected so labels for the remaining patterns do not
						 * need updating.
						 */
						patternViewer.refresh(false);
						entriesViewer.refresh(true);
					} catch (ReferenceViolationException e1) {
						MessageDialog.openError(getShell(), "Pattern in Use", "The pattern cannot be removed because it is in use else where.  " + e1.getLocalizedMessage() + "  The object referencing is " + ((ExtendablePropertySet)e1.getPropertySet()).getObjectDescription());
					}
				}
			}
		});

		Button upButton = new Button(container, SWT.PUSH);
		upButton.setText("Up");
		upButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		upButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection ssel = (IStructuredSelection)patternViewer.getSelection();
				if (ssel.size() == 1) {
					MemoPattern thisPattern = (MemoPattern) ssel.getFirstElement();

					// Find the previous MemoPattern in the order.
					MemoPattern abovePattern = null;
					ObjectCollection<MemoPattern> patterns = account.getPatternCollection();
					for (MemoPattern pattern: patterns) {
						if (pattern.getOrderingIndex() < thisPattern.getOrderingIndex()) {
							if (abovePattern == null || pattern.getOrderingIndex() > abovePattern.getOrderingIndex()) {
								abovePattern = pattern;
							}
						}
					}

					if (abovePattern != null) {
						swapOrderOfPatterns(thisPattern, abovePattern);
					}

					/*
					 * The patterns are re-ordered but the labels are not
					 * affected so do not request a refresh of the labels.
					 */
					patternViewer.refresh(false);

					entriesViewer.refresh(true);
				}
			}
		});

		Button downButton = new Button(container, SWT.PUSH);
		downButton.setText("Down");
		downButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		downButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection ssel = (IStructuredSelection)patternViewer.getSelection();
				if (ssel.size() == 1) {
					MemoPattern thisPattern = (MemoPattern) ssel.getFirstElement();

					// Find the next MemoPattern in the order.
					MemoPattern belowPattern = null;
					ObjectCollection<MemoPattern> patterns = account.getPatternCollection();
					for (MemoPattern pattern: patterns) {
						if (pattern.getOrderingIndex() > thisPattern.getOrderingIndex()) {
							if (belowPattern == null || pattern.getOrderingIndex() < belowPattern.getOrderingIndex()) {
								belowPattern = pattern;
							}
						}
					}

					if (belowPattern != null) {
						swapOrderOfPatterns(thisPattern, belowPattern);
					}

					/*
					 * The patterns are re-ordered but the labels are not
					 * affected so do not request a refresh of the labels.
					 */
					patternViewer.refresh(false);

					entriesViewer.refresh(true);
				}
			}
		});

		return container;
	}

	/**
	 * Sets or clears the error message.
	 * If not <code>null</code>, the OK button is disabled.
	 *
	 * @param errorMessage
	 *            the error message, or <code>null</code> to clear
	 */
	public void updateErrorMessage() {
		String errorMessage = null;

		if (account.isReconcilable()) {
			if (account.getDefaultCategory() == null) {
				errorMessage = "All reconcilable accounts must have a default category set.";
			} else {
				// Check the patterns
				for (MemoPattern pattern: account.getPatternCollection()) {
					if (isMemoPatternValid(pattern) != null) {
						errorMessage = "There are errors in the patterns table.  Hover over the error image to the left of the row to see details.";
						break;
					}
				}
			}
		}

		if (errorMessage == null) {
			//			messageArea.clearErrorMessage();    ?????
			messageArea.restoreTitle();
		} else {
			messageArea.updateText(errorMessage, IMessageProvider.ERROR);
		}

		// If called during createDialogArea, the okButton
		// will not have been created yet.
		Button okButton = getButton(IDialogConstants.OK_ID);
		if (okButton != null) {
			okButton.setEnabled(errorMessage == null);
		}
	}

	void swapOrderOfPatterns(MemoPattern thisPattern,
			MemoPattern abovePattern) {
		/*
		 * First we remove both patterns from the sorted collection.
		 * This is because we break the integrity of the collection
		 * if we change a property that affects the sort order.
		 */
		sortedPatterns.remove(thisPattern);
		sortedPatterns.remove(abovePattern);

		// Swap the ordering indexes
		int thisIndex = thisPattern.getOrderingIndex();
		int aboveIndex = abovePattern.getOrderingIndex();
		abovePattern.setOrderingIndex(thisIndex);
		thisPattern.setOrderingIndex(aboveIndex);

		/*
		 * Now add them back to the sorted collection.
		 */
		sortedPatterns.add(thisPattern);
		sortedPatterns.add(abovePattern);
	}

	private TransactionType<T> lookupTransactionType(String transactionTypeId) {
		TransactionType<T> transType = null;
		for (TransactionType<T> eachTransType : applicableTransactionTypes) {
			if (eachTransType.getId().equals(transactionTypeId)) {
				transType = eachTransType;
				break;
			}
		}
		return transType;
	}

	private TransactionParamMetadata lookupParamByName(TransactionType<T> transType, String parameterName) {
		for (TransactionParamMetadata paramMetadata : transType.getParameters()) {
			if (paramMetadata.getName().equals(parameterName)) {
				return paramMetadata;
			}
		}
		return null;
	}

	/**
	 * This class provides the content for the table
	 */
	class PatternContentProvider implements IStructuredContentProvider {

		/**
		 * Gets the elements for the table.  The elements are the MemoPattern
		 * objects for the account.
		 */
		@Override
		public Object[] getElements(Object input) {
			IPatternMatcher account = (IPatternMatcher)input;
			return account.getPatternCollection().toArray(new MemoPattern[0]);
		}

		/**
		 * Disposes any resources
		 */
		@Override
		public void dispose() {
			// We don't create any resources, so we don't dispose any
		}

		/**
		 * Called when the input changes
		 */
		@Override
		public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
			// Nothing to do
		}
	}

	/**
	 * This class implements the sorting for the type catalog table.
	 */
	class PatternSorter extends ViewerSorter {
		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			MemoPattern pattern1 = (MemoPattern) e1;
			MemoPattern pattern2 = (MemoPattern) e2;

			return pattern1.getOrderingIndex() - pattern2.getOrderingIndex();
		}
	}
}
