package net.sf.jmoney.stocks.pages_new;

import org.eclipse.core.databinding.observable.DisposeEvent;
import org.eclipse.core.databinding.observable.IDisposeListener;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;

/**
		 * When a value is changed either in the model or in the target
		 * (the UI) then the link to the computed value is broken.  The
		 * link can be restored programatically in which case the current
		 * value is overwritten.
		 *  
		 * 
		 *  Now what happens if the two calculators are inconsistent?  Suppose
		 *  
		 *  'NetAmount' defaults to 'GrossAmount' - 'Tax'
		 *  'GrossAmount' defaults to 'NetAmount' + 'Tax'
		 *  'Tax' defaults to 20% of 'Gross Amount'
		 *  
		 *  The user enters 100 for 'NetAmount'.  The
		 *  sequence is then:
		 *  
		 *  'GrossAmount' defaults to 100 (as a blank tax amount is considered to be zero)
		 *  'Tax' defaults to 20
		 *  'GrossAmount' defaults to 120 (100 + 20)
		 *  'Tax' defaults to 24 (20% of 120)
		 *  
		 *  and it will eventually converge.  So that is ok.
		 *  
		 *  But what if it does not converge?  And is it ok that this is not a very efficient
		 *  way of getting the default value for the gross amount when one could simply have divided
		 *  100 by 0.8?
		 *  
		 *  A reasonable design is to say that no recursion is allowed.  The user must
		 *  specifically code a recursive implementation if one is desired.
		 * 
		 *  So the order in which default values are added to the default providing context is important.  No calculated default value
		 *  can depend on a calculated default value that is calculated later in the list.  A calculated
		 *  default value can depend on a non-default value from later in the list.  A non-default value
		 *  is the value that was either entered by the user or provided by the model.
		 *  
		 * In the above example, the first two rules are considered more inviolate than the third
		 * rule.  That is, it happens fairly often that the tax rate is not 20% but it almost never
		 * happens that the gross amount is not the sum of the net amount and the tax.
		 * 
		 * So the rule that defaults the tax to 20% is defined last.  The user enters 100 for the NetAmount.
		 * The other two are mutually recursive so cannot be calculated.  But now the 'Tax' is defined to be
		 * 20% of ('NetAmount' / 0.8) if 'GrossAmount' has no value.  Now things will work.  'Tax' defaults to
		 * 25, and then 'GrossAmount' defaults to 125.
		 * 
		 * Now what if the user enters 100 for 'NetAmount' and '150' for 'GrossAmount'.  There are three possible
		 * values for 'Tax' (20% of 150, 20% of 100/0.8, or 150 - 100).  Because of the inviobility order, we
		 * want the third to be used.  How do we ensure that the framework does that?  We do that in the calculation
		 * for 'Tax'.  The calculator must subtract net from gross to get the tax whenever both net and gross values
		 * are present.
		 *     
		 * In this example, the calculations are very slightly simpler and more intuitive if the tax is calculated first, with the others
		 * being calculated using the calculated default tax amount.  So that is the order we will use in this example.
		 * 
		 * 
		 * Let's go through some permutations:
		 * 
		 * Gross = 100, Net and Tax not entered
		 *
		 * 
		 * Now we have a problem.  We want each calculator to have access to calculated values only from previous
		 * calculations in the order.  We want them to have access to the actual entered values of all the properties.
		 * The calculated defaults are bound to the model, so are readily available from the model.  The actual entered
		 * values are available from the bindings.
		 * 
		 * This can't be enforced at compile time but it can be enforced at runtime.
		 * If a default value computer does access a value from later in the order then the computed default may change when
		 * the later default value changes.  The framework will detect such a change and throw an exception.  These checks are
		 * readily implemented by maintaining a flag in each default binding that is set on while a new default value is being
		 * set in the model.  Now when a default value changes due to the computed value changing, all flags in later default bindings
		 * within the context are checked.  If any are 'on', an exception is thrown.
		 * 
		 * Note that the 'order' means the order that the bindings are created in the context.
		 *  
 * 
 */
public class DefaultValueBinding<T> {
	
	protected boolean disposed = false;


	
	
	protected DefaultProvidingContext context;

	private IDisposeListener disposeListener;


	
	
	
	private IObservableValue<T> target;
	private IObservableValue<T> defaultValue;

	private boolean defaultActive = true;	

	private boolean updatingTarget;
	private IValueChangeListener<T> targetChangeListener = new IValueChangeListener<T>() {
		public void handleValueChange(ValueChangeEvent event) {
			if (!updatingTarget
					&& !/*Util.equals*/isEquals(event.diff.getOldValue(), event.diff
							.getNewValue())) {
				defaultActive = false;
			}
		}
	};
	private IValueChangeListener<T> defaultValueChangeListener = new IValueChangeListener<T>() {
		public void handleValueChange(ValueChangeEvent event) {
			if (!/*Util.equals*/isEquals(event.diff.getOldValue(), event.diff
							.getNewValue())) {
				doUpdate(defaultValue, target, false, false);
			}
		}
	};

	/**
	 * @param targetObservableValue
	 * @param defaultObservableValue
	 */
	public DefaultValueBinding(IObservableValue<T> targetObservableValue,
			IObservableValue<T> defaultObservableValue) {

		this.target = targetObservableValue;
		this.defaultValue = defaultObservableValue;
		
		// Assume calculated value applies if the binding is
		// applied.  Overwrite any value that may already be set.
		target.addValueChangeListener(targetChangeListener);
		defaultValue.addValueChangeListener(defaultValueChangeListener);
	}

	/**
	 * Initializes this binding with the given context and adds it to the list
	 * of bindings of the context.
	 * <p>
	 * Subclasses may extend, but must call the super implementation.
	 * </p>
	 * 
	 * @param context
	 */
	public final void init(DefaultProvidingContext context) {
		this.context = context;
		if (target.isDisposed())
			throw new IllegalArgumentException("Target observable is disposed"); //$NON-NLS-1$
		if (defaultValue.isDisposed())
			throw new IllegalArgumentException("Default value observable is disposed"); //$NON-NLS-1$
		this.disposeListener = new IDisposeListener() {
			public void handleDispose(DisposeEvent staleEvent) {
				if (!isDisposed())
					dispose();
			}
		};
		target.addDisposeListener(disposeListener);
		defaultValue.addDisposeListener(disposeListener);

		context.addBinding(this);

		updateModelToTarget();
	}

	public void updateModelToTarget() {
		doUpdate(defaultValue, target, true, false);
	}

	/**
	 * Sets the bound value to the default value.  This
	 * is a one-off.  The value will not be changed again
	 * if the default value changes.
	 */
	public void updateToDefault() {
		doUpdate(defaultValue, target, true, false);
	}

	/**
	 * Binds the bound value to the default value.  If the
	 * default value later changes then the bound value will
	 * be kept in sync.
	 * <P>
	 * This method does not have to be called when a field is
	 * bound to a default value.  Use this method only if a value
	 * has been explicitly set and you now want the value to revert
	 * to tracking the default value.
	 */
	public void bindToDefault() {
		doUpdate(defaultValue, target, true, false);
		defaultActive = true;
	}

	/**
	 * Incorporates the provided <code>newStats</code> into the
	 * <code>multieStatus</code>.
	 * 
	 * @param multiStatus
	 * @param newStatus
	 * @return <code>true</code> if the update should proceed
	 */
	/* package */boolean mergeStatus(MultiStatus multiStatus, IStatus newStatus) {
		if (!newStatus.isOK()) {
			multiStatus.add(newStatus);
			return multiStatus.getSeverity() < IStatus.ERROR;
		}
		return true;
	}

	/*
	 * This method may be moved to UpdateValueStrategy in the future if clients
	 * need more control over how the source value is copied to the destination
	 * observable.
	 */
	private void doUpdate(final IObservableValue<T> source,
			final IObservableValue<T> destination,
			final boolean explicit, final boolean validateOnly) {

		if (!defaultActive) {
			return;
		}
		
		source.getRealm().exec(new Runnable() {
			public void run() {
					// Get value
					final T value = source.getValue();

					// Set value
					destination.getRealm().exec(new Runnable() {
						public void run() {
							if (destination == target) {
								updatingTarget = true;
							}
							try {
								destination.setValue(value);
							} finally {
								if (destination == target) {
									updatingTarget = false;
								}
							}
						}
					});
			}
		});
	}

	/**
	 * Disposes of this Binding.
	 */
	public void dispose() {
		if (targetChangeListener != null) {
			target.removeValueChangeListener(targetChangeListener);
			targetChangeListener = null;
		}
		if (defaultValueChangeListener != null) {
			defaultValue.removeValueChangeListener(defaultValueChangeListener);
			defaultValueChangeListener = null;
		}
		target = null;
		defaultValue = null;

		if (context != null) {
			context.removeBinding(this);
		}
		context = null;
		if (disposeListener != null) {
			if (target != null) {
				target.removeDisposeListener(disposeListener);
			}
			if (defaultValue != null) {
				defaultValue.removeDisposeListener(disposeListener);
			}
			disposeListener = null;
		}
		target = null;
		defaultValue = null;
		disposed = true;
	}

	/**
	 * @param context
	 */
	/* package */void setDataBindingContext(DefaultProvidingContext context) {
		this.context = context;
	}

	/**
	 * @return true if the binding has been disposed. false otherwise.
	 */
	public boolean isDisposed() {
		return disposed;
	}

	// TEMP - remove this and use 'equals' version in Util
	public static final boolean isEquals(final Object left, final Object right) {
		return left == null ? right == null : ((right != null) && left
				.equals(right));
	}
}
