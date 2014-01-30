package net.sf.jmoney.model2;

import net.sf.jmoney.fields.AccountControl;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;


public abstract class AccountCellEditor<A extends Account> extends CellEditor {

	Session session;
//	Class<A> classOfAccount;

	public AccountCellEditor(Composite table, Session session/*, Class<A> classOfAccount*/) {
		super(table, SWT.SINGLE);

		this.session = session;
//		this.classOfAccount = classOfAccount;

		/*
		 * The super constructor makes a call to our createControl method.  This
		 * method creates the AccountControl.  However, the session and classOfAccount fields were not
		 * set at that time, so the AccountControl has a null session and classOfAccount set in it.
		 * We must set the properties before anything happens such as the control
		 * getting focus.
		 */
//		accountControl.setSession(session, classOfAccount);
	}

	// Fields if text

    /**
     * The text control; initially <code>null</code>.
     */
    protected AccountControl<A> accountControl;

    /**
     * State information for updating action enablement
     */
    private boolean isAccountSelected = false;

    /**
     * Checks to see if the "deleteable" state (can delete/
     * nothing to delete) has changed and if so fire an
     * enablement changed notification.
     */
    private void checkAccountSelected() {
        boolean oldIsAccountSelected = isAccountSelected;
        isAccountSelected = isAccountSelected();
        if (oldIsAccountSelected != isAccountSelected) {
            fireEnablementChanged(DELETE);
            fireEnablementChanged(SELECT_ALL);
            fireEnablementChanged(COPY);
            fireEnablementChanged(CUT);
        }
    }

    /* (non-Javadoc)
     * Method declared on CellEditor.
     */
	@Override
    protected Control createControl(Composite parent) {
		accountControl = new AccountControl<A>(parent, getClassOfAccount()) {
			@Override
			protected Session getSession() {
				return session;
			}
		};

        accountControl.addKeyListener(new KeyAdapter() {
            // hook key pressed - see PR 14201
		    @Override
            public void keyPressed(KeyEvent e) {
                keyReleaseOccured(e);
                // TODO: Is this code needed?

                // as a result of processing the above call, clients may have
                // disposed this cell editor
                if ((getControl() == null) || getControl().isDisposed()) {
					return;
				}
                checkAccountSelected(); // see explaination below
            }
        });

        accountControl.addTraverseListener(new TraverseListener() {
            @Override
			public void keyTraversed(TraverseEvent e) {
                if (e.detail == SWT.TRAVERSE_ESCAPE
                        || e.detail == SWT.TRAVERSE_RETURN) {
                    e.doit = false;
                }
            }
        });

        // We really want a selection listener but it is not supported so we
        // use a key listener and a mouse listener to know when selection changes
        // may have occured
        accountControl.addMouseListener(new MouseAdapter() {
		    @Override
            public void mouseUp(MouseEvent e) {
                checkAccountSelected();
            }
        });

        accountControl.addFocusListener(new FocusAdapter() {
		    @Override
            public void focusLost(FocusEvent e) {
                AccountCellEditor.this.focusLost();
            }
        });

        accountControl.setFont(parent.getFont());
        accountControl.setBackground(parent.getBackground());

        // TODO: Do we need these?
//        text.setText("");//$NON-NLS-1$
//        text.addModifyListener(getModifyListener());

        return accountControl;
    }

	/**
	 * One would expect that the account class could more simply be passed as a
	 * parameter to the AccountControl constructor. After all it never changes.
	 * However the CellEditor API uses an anti-pattern whereby the call to the
	 * abstract method CreateControl is made from the constructor. This means
	 * the CellEditor has not been constructed when CreateControl is called, and
	 * so CreateControl does not have available the fields that would have been
	 * used to store accountClass. This is a hack to get around the problem.
	 * 
	 * @return
	 */
    protected abstract Class<A> getClassOfAccount();

	/**
     * The <code>TextCellEditor</code> implementation of
     * this <code>CellEditor</code> framework method returns
     * the text string.
     *
     * @return the text string
     */
	@Override
    protected Object doGetValue() {
		return accountControl.getAccount();
    }

    /* (non-Javadoc)
     * Method declared on CellEditor.
     */
	@Override
    protected void doSetFocus() {
        if (accountControl != null) {
  //          text.selectAll();
	        accountControl.setFocus();
            checkAccountSelected();  // only if text
        }
    }

    /**
     * The <code>TextCellEditor</code> implementation of
     * this <code>CellEditor</code> framework method accepts
     * a text string (type <code>String</code>).
     *
     * @param value a text string (type <code>String</code>)
     */
	@Override
    protected void doSetValue(Object value) {
        Assert.isTrue(accountControl != null);
        Assert.isTrue(value == null || getClassOfAccount().isAssignableFrom(value.getClass()));

        // Listener was removed for text but not combo?
//      accountControl.removeModifyListener(getModifyListener());
        accountControl.setAccount(getClassOfAccount().cast(value));
//      accountControl.addModifyListener(getModifyListener());
    }

    /**
     * Processes a modify event that occurred in this text cell editor.
     * This framework method performs validation and sets the error message
     * accordingly, and then reports a change via <code>fireEditorValueChanged</code>.
     * Subclasses should call this method at appropriate times. Subclasses
     * may extend or reimplement.
     *
     * @param e the SWT modify event
     */
    protected void editOccured(ModifyEvent e) {
    	/* TODO: Figure out this code - it came from the text cell editor
        String value = text.getText();
        if (value == null) {
			value = "";//$NON-NLS-1$
		}
        Object typedValue = value;
        boolean oldValidState = isValueValid();
        boolean newValidState = isCorrect(typedValue);
        if (typedValue == null && newValidState) {
			Assert.isTrue(false,
                    "Validator isn't limiting the cell editor's type range");//$NON-NLS-1$
		}
        if (!newValidState) {
            // try to insert the current value into the error message.
            setErrorMessage(MessageFormat.format(getErrorMessage(),
                    new Object[] { value }));
        }
        valueChanged(oldValidState, newValidState);
        */
    }

    /**
     * Since a text editor field is scrollable we don't
     * set a minimumSize.
     */
    @Override
    public LayoutData getLayoutData() {
    	// Text did this:
//        return new LayoutData();

    	    /*
    	     * The <code>ComboBoxCellEditor</code> implementation of
    	     * this <code>CellEditor</code> framework method sets the
    	     * minimum width of the cell.  The minimum width is 10 characters
    	     * if <code>comboBox</code> is not <code>null</code> or <code>disposed</code>
    	     * eles it is 60 pixels to make sure the arrow button and some text is visible.
    	     * The list of CCombo will be wide enough to show its longest item.
    	     */
    	        LayoutData layoutData = super.getLayoutData();
    	        if ((accountControl == null) || accountControl.isDisposed()) {
    				layoutData.minimumWidth = 60;
    			} else {
    	            // make the comboBox 10 characters wide
    	            GC gc = new GC(accountControl);
    	            layoutData.minimumWidth = (gc.getFontMetrics()
    	                    .getAverageCharWidth() * 10) + 10;
    	            gc.dispose();
    	        }
    	        return layoutData;

    // We could also do this:
//    	        	return super.getLayoutData();
    }

    /**
     * Handles a default selection event from the text control by applying the editor
     * value and deactivating this cell editor.
     *
     * @param event the selection event
     *
     * @since 3.0
     */
    protected void handleDefaultSelection() {
        // same with enter-key handling code in keyReleaseOccured(e);
        fireApplyEditorValue();
        deactivate();
    }

    protected boolean isAccountSelected() {
        return (accountControl != null && accountControl.getAccount() != null);
    }

    /**
     * The <code>TextCellEditor</code>  implementation of this
     * <code>CellEditor</code> method returns <code>true</code> if
     * the current account is not null.
     */
    @Override
    public boolean isCopyEnabled() {
        return isAccountSelected();
    }

    /**
     * The <code>AccountCellEditor</code>  implementation of this
     * <code>CellEditor</code> method returns <code>true</code> if
     * the current account is not null.
     */
    @Override
    public boolean isCutEnabled() {
        return isAccountSelected();
    }

    /**
     * The <code>AccountCellEditor</code> implementation of this
     * <code>CellEditor</code> method returns <code>true</code>
     * in all cases.  This allows the account to be set to null
     * using the delete key.
     */
    @Override
    public boolean isDeleteEnabled() {
        return isAccountSelected();
    }

    /**
     * The <code>TextCellEditor</code>  implementation of this
     * <code>CellEditor</code> method always returns <code>true</code>.
     */
    @Override
    public boolean isPasteEnabled() {
        return true;
    }

    /**
     * The <code>TextCellEditor</code> implementation of this
     * <code>CellEditor</code> method copies the full account
     * name to the clipboard.
     */
    @Override
    public void performCopy() {
    	// TODO: Implement this
    }

    /**
     * The <code>TextCellEditor</code> implementation of this
     * <code>CellEditor</code> method copies the full account
     * name to the clipboard and sets the account to null.
     */
    @Override
    public void performCut() {
    	// TODO: Implement this

        // Do we need these?
        checkAccountSelected();
    }

    /**
     * The <code>AccountCellEditor</code> implementation of this
     * <code>CellEditor</code> method deletes the
     * account entirely from the control.  The value in this
     * editor becomes a null account reference.
     */
    @Override
    public void performDelete() {
        accountControl.setAccount(null);

        // Do we need these?
        checkAccountSelected();
    }

    /**
     * The <code>AccountCellEditor</code> implementation of this
     * <code>CellEditor</code> method pastes the
     * the clipboard contents into the control, completely replacing
     * what was there before.
     *
     * If the text was a full account name then that account will become
     * the value set in this editor.  As the full account name is always
     * put into the clipboard on a cut or paste, this ensures that accounts
     * can be copied or moved to other controls correctly.
     *
     * If the text is not the full account name of a valid account then the
     * list is searched for matches.
     */
    @Override
    public void performPaste() {
        // TODO: Implement this

        // Do we need these?
        checkAccountSelected();
    }

    // Methods if combobox

    /**
     * Applies the currently selected value and deactiavates the cell editor
     */
    void applyEditorValueAndDeactivate() {
        fireApplyEditorValue();
        deactivate();
    }

    /*
     *  (non-Javadoc)
     * @see org.eclipse.jface.viewers.CellEditor#focusLost()
     */
    @Override
    protected void focusLost() {
    	// This was in the combo code only:
        if (isActivated()) {
            applyEditorValueAndDeactivate();
        }

        // Otherwise this was called
       	super.focusLost();
    }
}
