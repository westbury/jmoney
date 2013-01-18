package net.sf.jmoney.stocks.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.stocks.model.RatesTable.Band;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * The implementation for the composite control that contains
 * the account property controls.
 */
class RatesEditorControl extends Composite {
	private class RowControls {
		public RowControls(Composite parent) {
			label1 = new Label(parent, SWT.NONE);
			percentageText = new Text(parent, SWT.BORDER);
			label2 = new Label(parent, SWT.NONE);
			upperBoundText = new Text(parent, SWT.BORDER);
			
			label1.setText("plus");
			percentageText.setLayoutData(new GridData(30, SWT.DEFAULT));
			upperBoundText.setLayoutData(new GridData(30, SWT.DEFAULT));
			
			FocusListener listener = new FocusAdapter() {
				@Override
				public void focusGained(FocusEvent e) {
					lastFocusRow = RowControls.this;
				}
			};
			
			percentageText.addFocusListener(listener);
			upperBoundText.addFocusListener(listener);
		}
		
		Label label1;
		Text  percentageText;
		Label label2;
		Text  upperBoundText;
		
		public void configureAsMiddleRow() {
			label2.setText("percent of the amount up to");
			upperBoundText.setVisible(true);
		}
		
		public void configureAsLastRow() {
			label2.setText("percent of the rest");
			upperBoundText.setVisible(false);
		}

		public void configureAsOnlyRow() {
			label2.setText("percent of the amount");
			upperBoundText.setVisible(false);
		}
	}

//	RatesTable ratesTable;

	private Text fixedAmountControl;

	private ArrayList<RowControls> rows = new ArrayList<RowControls>();

	// The grid in which the rate bands are displayed.
	private Composite middleRow;

	/*
	 * The last row which has focus. This is used only for determining which
	 * row to delete or where to insert a new row.
	 */
	private RowControls lastFocusRow;

	private Currency currencyForFormatting;

	private Button addRowButton;
	private Button removeRowButton;
	
	/**
	 * @param parent
	 */
	public RatesEditorControl(Composite parent) {
		super(parent, SWT.BORDER);

		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		setLayout(layout);

		// Create the controls to edit the properties.
		// Only the controls in the first line are created at
		// this time.   The rest are created when the account is set.

		// Create the top row
		Composite topRow = createFixedAmountRow();

		GridData topRowData = new GridData();
		topRowData.verticalAlignment = GridData.FILL;
		topRowData.horizontalAlignment = GridData.FILL;
		topRow.setLayoutData(topRowData);

		// Add the center control
		createBandRows();

		middleRow.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false));

		// Add the buttons
		createButtonsArea();
	}

	private Composite createButtonsArea() {
		Composite buttonRow = new Composite(this, SWT.NONE);

		FillLayout buttonLayout = new FillLayout();
		buttonLayout.marginHeight = 0;
		buttonLayout.marginWidth = 0;
		buttonRow.setLayout(buttonLayout);

		addRowButton = new Button(buttonRow, SWT.PUSH);
		addRowButton.setText("Add Band");
		addRowButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// Add a new row to the table
				RowControls newRow = new RowControls(middleRow);

				int lastFocusRowIndex = rows.indexOf(lastFocusRow);
				
				rows.add(lastFocusRowIndex+1, newRow);
				if (lastFocusRowIndex != rows.size()-2) {
					Control followingControl = rows.get(lastFocusRowIndex+2).label1;
					newRow.label1.moveAbove(followingControl);
					newRow.percentageText.moveAbove(followingControl);
					newRow.label2.moveAbove(followingControl);
					newRow.upperBoundText.moveAbove(followingControl);
					
					newRow.configureAsMiddleRow();
					if (rows.size() == 2) {
						// Existing row was only row, now it is last row
						RowControls lastRow = rows.get(1);
						lastRow.configureAsLastRow();
					}
				} else {
					RowControls previousRow = rows.get(lastFocusRowIndex);
					previousRow.configureAsMiddleRow();
					newRow.configureAsLastRow();
				}
				
				removeRowButton.setEnabled(true);

				middleRow.pack(true);
				pack(true);
				getParent().pack(true);
			}
		});

		removeRowButton = new Button(buttonRow, SWT.PUSH);
		removeRowButton.setText("Remove Band");
		removeRowButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// Remove a row from the table
				int lastFocusRowIndex = rows.indexOf(lastFocusRow);
				RowControls row = rows.remove(lastFocusRowIndex);
				row.label1.dispose();
				row.percentageText.dispose();
				row.label2.dispose();
				row.upperBoundText.dispose();

				if (lastFocusRowIndex == rows.size()) {
					// We removed the last row
					if (rows.size() == 1) {
						rows.get(0).configureAsOnlyRow();
						removeRowButton.setEnabled(false);
					} else {
						rows.get(lastFocusRowIndex-1).configureAsLastRow();
					}
				}
				
				
				middleRow.pack(true);
				pack(true);
				getParent().pack(true);
			}
		});

		return buttonRow;
	}

	private Composite createFixedAmountRow() {
		Composite topRow = new Composite(this, SWT.NONE);

		GridLayout topRowLayout = new GridLayout();
		topRowLayout.numColumns = 2;
		topRowLayout.marginHeight = 0;
		topRowLayout.marginWidth = 0;
		topRow.setLayout(topRowLayout);

		new Label(topRow, SWT.NONE).setText("Fixed amount");
		fixedAmountControl = new Text(topRow, SWT.BORDER);

		return topRow;
	}

	private void createBandRows() {
		middleRow = new Composite(this, SWT.NONE);

		GridLayout middleRowLayout = new GridLayout();
		middleRowLayout.numColumns = 4;
		middleRowLayout.marginHeight = 0;
		middleRowLayout.marginWidth = 0;

		middleRow.setLayout(middleRowLayout);

		// No sub-controls are added at this time.
	}

	/**
	 * @param account
	 */
	public void setRatesTable(RatesTable ratesTable, final Currency currencyForFormatting) {
		this.currencyForFormatting = currencyForFormatting;

		// Destroy all the controls in the middle grid.
		for (RowControls row: rows) {
			row.label1.dispose();
			row.percentageText.dispose();
			row.label2.dispose();
			row.upperBoundText.dispose();
		}

		// Note: Either ratesTable is null, or it is completely valid.
		
		if (ratesTable != null) {
			fixedAmountControl.setText(currencyForFormatting.format(ratesTable.getFixedAmount()));

			// Add a row for each row in the table

			List<Band> bands = ratesTable.getBands();

			if (bands.size() == 0) {
				RowControls row = new RowControls(middleRow);
				rows.add(row);

				row.configureAsOnlyRow();
			} else if (bands.size() == 1) {
				RowControls row = new RowControls(middleRow);
				rows.add(row);

				BigDecimal percentage = bands.get(0).getProportion();
				row.percentageText.setText(percentage.movePointRight(2).toString());

				row.configureAsOnlyRow();
			} else {
			for (int rowIndex = 0; rowIndex < bands.size(); rowIndex++) {
				Band band = bands.get(rowIndex);
				
				final RowControls row = new RowControls(middleRow);
				rows.add(row);
				
				BigDecimal percentage = band.getProportion();
				row.percentageText.setText(percentage.movePointRight(2).toString());
				
				if (rowIndex != bands.size()-1) {
					Band nextBand = bands.get(rowIndex+1);
					
					row.configureAsMiddleRow();
					
					row.upperBoundText.setText(currencyForFormatting.format(nextBand.getBandStart()));
//					row.upperBoundText.addFocusListener(
//							new FocusAdapter() {
//								@Override
//								public void focusLost(FocusEvent e) {
//									long amount = currencyForFormatting.parse(row.upperBoundText.getText());
//									// Update any controls here?
//								}
//							});
				} else {
					row.configureAsLastRow();
				}
			}
			}
		} else {
			RowControls row = new RowControls(middleRow);
			rows.add(row);

			row.configureAsOnlyRow();
		}
		
		removeRowButton.setEnabled(rows.size() > 1);
		
		// TODO: what is this?
		middleRow.pack();
		pack();
		getParent().pack(true);

		// We must listen for changes to the currency so that
		// we can change the format of the amount.
		/* TODO: complete this.  It may be this should be handled within an amount editor control which
		 * we should use here.

			account.getSession().addSessionChangeListener(
					new SessionChangeAdapter() {
						public void accountChanged(Account changedAccount, PropertyAccessor changedProperty, Object oldValue, Object newValue) {
							if (changedAccount == RatesEditor.this.account
									&& changedProperty == CapitalAccountInfo.getCurrencyAccessor()) {
								// Get the current text from the control and try to re-format
								// it for the new currency.
								// However, if the property can take null values and the control
								// contains the empty string then set the amount to null.
								// (The currency amount parser returns a zero amount for the
								// empty string).
								// Amounts can be represented by 'Long' or by 'long'.
								// 'Long' values can be null, 'long' values cannot be null.
								// If the text in the control now translates to a different long
								// value as a result of the new currency, update the new long value
								// in the datastore.

								// It is probably not necessary for us to set the control text here,
								// because this will be done by our listener if we are changing
								// the amount.  However, to protect against a future possibility
								// that a currency change may change the format without changing the amount,
								// we set the control text ourselves first.

								String amountString = propertyControl.getText();
								if (!amountString.equals("")) {
									Currency newCurrency = (Currency)newValue;
									long amount = newCurrency.parse(amountString);
									propertyControl.setText(newCurrency.format(amount));
									changedAccount.setLongPropertyValue(amountPropertyAccessor, amount);
								}
							}

						}
					});
		 */
	}

	public RatesTable getRatesTable() {
		long fixedAmount = currencyForFormatting.parse(fixedAmountControl.getText());

		/*
		 * Note that the Band objects contain the start of the range in which the
		 * percentage applies, whereas the rows of controls contain the upper end of
		 * the range to which the percentage applies.  This code therefore uses the
		 * value from the previous row (or zero for the first row) when constructing
		 * each band.
		 */
		long bandStart = 0;
		
		ArrayList<Band> bands = new ArrayList<Band>();
		for (RowControls row: rows) {
			BigDecimal proportion;

			try {
				proportion = new BigDecimal(row.percentageText.getText()).movePointLeft(2);
			} catch (NumberFormatException e) {
				proportion = BigDecimal.ZERO;
			}

			bands.add(new Band(bandStart, proportion));

			try {
				bandStart = currencyForFormatting.parse(row.upperBoundText.getText());
			} catch (NumberFormatException e) {
				bandStart = 0;
			}
		}

		return new RatesTable(fixedAmount, bands);
	}
}