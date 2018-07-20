package org.eclipse.jface.internal.databinding.provisional.swt;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.databinding.observable.AbstractObservable;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.IObservable;
import org.eclipse.core.databinding.observable.ObservableTracker;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

/**
 * This is a provisional class. Please leave feedback, good or bad, in bug
 * 419415 so it can be decided what to promote to general API.
 * <P>
 * A composite that automatically tracks and registers listeners on its
 * dependencies as long as all of its dependencies are {@link IObservable}
 * objects. The tracking is done while the child controls for this composite are
 * being created. So when any of the dependencies change, the child controls are
 * re-created.
 * <P>
 * Child controls are re-used if possible. To do this, the code that creates the
 * child controls must not create the controls directly. Instead a control
 * creator is instantiated for each 'type' of control. If a control creator is
 * called to create a control then it will first search the existing unused
 * controls for a control that was previously created by that control creator
 * (or a control creator that 'equals' it). A new control is only created if
 * none exist. If the re-used control needs to be moved in the child control
 * order then that is handled by this class.
 * <P>
 * Any change to one of the observable dependencies causes the child controls to
 * be re-built.
 * <p>
 * Example: The composite contains a 'name' control, it contains a 'company
 * name' control only if isCompany (being of type IObservableValue<Boolean>) is
 * set.
 * </p>
 * 
 * <pre>
 * class MyComposite extends UpdatableComposite {
 * 		ControlCreator labelCreator = new LabelCreator(this);
 * 		ControlCreator companyNameControlCreator = new CompanyNameControlCreator(this);
 * 		ControlCreator genderComboCreator = new GenderComboCreator(this);
 * 
 *  	MyComposite(Composite parent, int style) {
 *  		super(parent, style);
 *  
 *  		// IMPORTANT: Constructors must always call this.  The constructor in the super class
 *  		// cannot do this for you because trackControls calls createControls which may use fields in this class
 *  		// that will not have been initialized when the super constructor is called.
 *  		trackControls();
 *   	}
 *   
 * 	&#064;Override
 * 	protected void createControls() {
 * 		Label label1 = labelCreator.create();
 * 		label1.setText("Name:");
 * 					
 * 		nameControlCreator.create();
 * 					
 * 		if (isCompany.getValue()) {
 * 			Label label2 = labelCreator.create();
 * 			label2.setText("Company Name:");
 * 					
 * 			companyNameControlCreator.create();
 * 		} else {
 * 			Label label3 = labelCreator.create();
 * 			label3.setText("Gender:");
 * 					
 * 			genderComboCreator.create();
 * 		}
 * 	}
 * </pre>
 * 
 * The implementation of the label creator might look something like this:
 * 
 * <pre>
 * public class PropertyLabelCreator extends ControlCreator&lt;Label&gt; {
 * 
 * 	public PropertyLabelCreator(PropertiesComposite updatingComposite) {
 * 		super(updatingComposite);
 * 	}
 * 
 * 	&#064;Override
 * 	public Label createControl() {
 * 		final CLabel label = new Label(composite, SWT.NONE);
 * 		label.setBackground(composite.getBackground());
 * 		return label;
 * 	}
 * }
 * </pre>
 * 
 * if isCompany is toggled on and off, you will see the second line of controls
 * switch back and forth. The 'Name:' label is the first control and will be
 * re-used. Likewise the 'name' control will be re-used. The labels for 'Company
 * Name:' and for 'Sex:' are both created by the same ControlCreator instance
 * and therefore will be re-used. The call to setText, made in the user's code,
 * will toggle the label text between "Company Name:" and "Sex:". The last
 * control will be created and disposed as 'isCompany' is toggled.
 * <P>
 * You often have a choice to make as to whether two controls share the same
 * creator. For example, for a combo box, you can have a separate control
 * creator for each combo. The values listed in the drop-down are set when the
 * control is first created. If the control is re-used then the drop-down values
 * will already be populated. The second option is to have a single creator for
 * many different combo boxes that may appear in the composite and that may all
 * have different lists of options in the drop-down. In that case a control that
 * was created for one use may be re-used for a different use with different
 * values in the drop-down. The user code in CreateControls must then set the
 * drop-down values after the control is 'created' (being sure to remove
 * previous values).
 * 
 * @author Nigel Westbury
 */
public abstract class UpdatingComposite extends Composite {

	/**
	 * key in the user data map in each control. The value in the map will be
	 * the control creator that created this control.
	 */
	public final String key = "org.eclipse.databinding.controlcreator"; //$NON-NLS-1$

	private boolean dirty = true;

	/**
	 * Array of observables this computed value depends on. This field has a
	 * value of <code>null</code> if we are not currently listening.
	 */
	private IObservable[] dependencies = null;

	/**
	 * Set to a non-null list only while controls are being re-built, this being
	 * the list of controls available for re-use and must be in order
	 */
	List<Control> remainder = null;

	private class ControlSetObservable extends AbstractObservable {
		private ControlSetObservable(Realm realm) {
			super(realm);
		}

		@Override
		public boolean isStale() {
			return false;
		}

		@Override
		public void fireChange() {
			super.fireChange();
		}
	}

	/**
	 * Inner class that implements interfaces that we don't want to expose as
	 * public API. Each interface could have been implemented using a separate
	 * anonymous class, but we combine them here to reduce the memory overhead
	 * and number of classes.
	 * 
	 * <p>
	 * The Runnable calls computeValue and stores the result in cachedValue.
	 * </p>
	 * 
	 * <p>
	 * The IChangeListener stores each observable in the dependencies list. This
	 * is registered as the listener when calling ObservableTracker, to detect
	 * every observable that is used by computeValue.
	 * </p>
	 * 
	 * <p>
	 * The IChangeListener is attached to every dependency.
	 * </p>
	 * 
	 */
	private class PrivateChangeInterface implements IChangeListener {
		@Override
		public void handleChange(ChangeEvent event) {
			makeDirty();
		}
	}

	/**
	 * This runnable updates the controls in the composite. It will create,
	 * dispose, and move around controls as necessary to get the list of child
	 * controls of the composite into their new required state. This method is
	 * called initially from the constructor of this composite and thereafter
	 * when any of the tracked dependencies change.
	 * <P>
	 * This code is in a runnable because it is called by the dependency
	 * tracking in <code>ObservableTracker</code>.
	 */
	private class PrivateRunnableInterface implements Runnable {
		@Override
		public void run() {
			remainder = new LinkedList<Control>();
			remainder.addAll(Arrays.asList(getChildren()));

			createControls();

			/*
			 * Remove any controls left.
			 */
			for (Control control : remainder) {
				remove(control);
			}
		}
	}

	private IChangeListener privateChangeInterface = new PrivateChangeInterface();
	private Runnable privateRunnableInterface = new PrivateRunnableInterface();

	private ControlSetObservable computeSizeObservable = new ControlSetObservable(
			Realm.getDefault());

	public UpdatingComposite(Composite parent, int style) {
		super(parent, style);

		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				stopListening();
			}
		});
	}

	protected final void makeDirty() {
		if (!dirty) {
			dirty = true;

			stopListening();

			/*
			 * Start the runnable asynchronously, so it runs after all the
			 * observables have been updated.
			 */
			Display.getCurrent().asyncExec(new Runnable() {
				@Override
				public void run() {
					/*
					 * Although we stop listening to the dependencies when this
					 * composite is disposed, there is a small window in which
					 * the composite may be disposed before we get back onto the
					 * UI thread here.
					 */
					if (isDisposed()) {
						return;
					}

					createAndTrackControls();

					/*
					 * Having added and removed controls, this composite now
					 * needs to be laid out. The children are okay because they
					 * should be laid out when they are created or re-used, so
					 * passing <code>false</code> is fine.
					 */
					layout(false);

					/*
					 * Assume that the preferred size has changed. Parent
					 * composites may need to re-layout their controls whenever
					 * the size of this control changes.
					 */
					computeSizeObservable.fireChange();
				}
			});
		}
	}

	/**
	 * This line will do the following:
	 * <UL>
	 * <LI>Run the <code>createControls</code> method</LI>
	 * <LI>While doing so, add any observable that is touched to the
	 * dependencies list</LI>
	 * </UL>
	 */
	protected void createAndTrackControls() {
		IObservable[] newDependencies = ObservableTracker.runAndMonitor(
				privateRunnableInterface, privateChangeInterface, null);

		dependencies = newDependencies;

		dirty = false;
	}

	/**
	 * This method is called when a control is no longer needed. This default
	 * implementation will dispose the control.
	 * <P>
	 * Override this if the layout allows controls to be left hidden.
	 * 
	 * @param control
	 */
	private void remove(Control control) {
		// First tell the control creator that this control
		// is going away.
		ControlCreator<?> creator = (ControlCreator<?>) control.getData(key);
		creator.remove(control);

		control.dispose();
	}

	/**
	 * 
	 */
	private void stopListening() {
		// Stop listening for dependency changes.
		if (dependencies != null) {
			for (int i = 0; i < dependencies.length; i++) {
				IObservable observable = dependencies[i];

				observable.removeChangeListener(privateChangeInterface);
			}
			dependencies = null;
		}
	}

	/**
	 * This method is called while getters are being tracked. However we want to
	 * track only getters on observables that were used to determine which
	 * controls are created and setting properties on those controls outside of
	 * the control creator. We really don't want to track getters that are used
	 * while creating the control. For example it may be that
	 * 
	 * @param controlCreator
	 * @return the control, which may be either re-used or just created by the
	 *         control creator
	 */
	<T extends Control> T create(ControlCreator<T> controlCreator) {
		ObservableTracker.setIgnore(true);
		try {
			T matchingControl = null;
			for (Control control : remainder) {
				if (controlCreator.equals(control.getData(key))) {
					matchingControl = controlCreator.typeControl(control);
					break;
				}
			}

			if (matchingControl != null) {
				/*
				 * Move this control to be before any other remaining controls.
				 */
				if (matchingControl != remainder.get(0)) {
					matchingControl.moveAbove(remainder.get(0));
				}
				remainder.remove(matchingControl);
			} else {
				matchingControl = controlCreator.createControl();
				matchingControl.setData(key, controlCreator);
				controlCreator.controls.add(matchingControl);
				/*
				 * Move this control to be before any other remaining controls.
				 */
				if (!remainder.isEmpty()) {
					matchingControl.moveAbove(remainder.get(0));
				}
			}
			return matchingControl;
		} finally {
			ObservableTracker.setIgnore(false);
		}
	}

	/**
	 * The implementation of this method creates the child controls in the
	 * composite.
	 * <P>
	 * DO NOT create controls directly in this composite. You must use control
	 * creators to create all child controls. Be aware that this method will be
	 * called multiple times as dependencies change.
	 * <P>
	 * DO use tracked getters to get values, otherwise the UI will not
	 * automatically update as the value changes. If the getter for a value is
	 * not a tracked getter then create an observable for the value and then get
	 * the value from the observable (the observable you created would then be
	 * automatically added as a dependency).
	 */
	abstract protected void createControls();

	/**
	 * Overrides the implementation from the super class to make it a tracked
	 * getter.
	 * <P>
	 * This has nothing much to do with the purpose of this class. It's just
	 * that computeSize should always be a tracked getter because that allows
	 * the parent composite to adjust its layout accordingly.
	 * 
	 * @TrackedGetter
	 */
	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		ObservableTracker.getterCalled(computeSizeObservable);
		return super.computeSize(wHint, hHint, changed);
	}
}
