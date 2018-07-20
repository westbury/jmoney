/*******************************************************************************
 * Copyright (c) 2009, 2010 Matthew Hall and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Hall - initial API and implementation (bug 268472)
 *     Matthew Hall - bug 300953
 ******************************************************************************/

package org.eclipse.jface.databinding.fieldassist;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.databinding.ValidationStatusProvider;
import org.eclipse.core.databinding.observable.DisposeEvent;
import org.eclipse.core.databinding.observable.IDecoratingObservable;
import org.eclipse.core.databinding.observable.IDisposeListener;
import org.eclipse.core.databinding.observable.IObservable;
import org.eclipse.core.databinding.observable.IObserving;
import org.eclipse.core.databinding.observable.list.IListChangeListener;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.ListChangeEvent;
import org.eclipse.core.databinding.observable.list.ListDiffVisitor;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.swt.ISWTObservable;
import org.eclipse.jface.databinding.viewers.IViewerObservable;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Widget;

/**
 * Decorates the underlying controls of the target observables of a
 * {@link ValidationStatusProvider} with {@link ControlDecoration}s mirroring
 * the current validation status. Only those target observables which implement
 * {@link ISWTObservable} or {@link IViewerObservable} are decorated.
 * 
 * @since 1.4
 */
public class ControlDecorationSupport {
	/**
	 * Creates a ControlDecorationSupport which observes the validation status
	 * of the specified {@link ValidationStatusProvider}, and displays a
	 * {@link ControlDecoration} over the underlying SWT control of all target
	 * observables that implement {@link ISWTObservable} or
	 * {@link IViewerObservable}.
	 * 
	 * @param validationStatusProvider
	 *            the {@link ValidationStatusProvider} to monitor.
	 * @param position
	 *            SWT alignment constant (e.g. SWT.LEFT | SWT.TOP) to use when
	 *            constructing {@link ControlDecorationSupport}
	 * @return a ControlDecorationSupport which observes the validation status
	 *         of the specified {@link ValidationStatusProvider}, and displays a
	 *         {@link ControlDecoration} over the underlying SWT control of all
	 *         target observables that implement {@link ISWTObservable} or
	 *         {@link IViewerObservable}.
	 */
	public static ControlDecorationSupport create(
			ValidationStatusProvider validationStatusProvider, int position) {
		return create(validationStatusProvider, position, null,
				new ControlDecorationUpdater());
	}

	/**
	 * Creates a ControlDecorationSupport which observes the validation status
	 * of the specified {@link ValidationStatusProvider}, and displays a
	 * {@link ControlDecoration} over the underlying SWT control of all target
	 * observables that implement {@link ISWTObservable} or
	 * {@link IViewerObservable}.
	 * 
	 * @param validationStatusProvider
	 *            the {@link ValidationStatusProvider} to monitor.
	 * @param position
	 *            SWT alignment constant (e.g. SWT.LEFT | SWT.TOP) to use when
	 *            constructing {@link ControlDecoration} instances.
	 * @param composite
	 *            the composite to use when constructing
	 *            {@link ControlDecoration} instances.
	 * @return a ControlDecorationSupport which observes the validation status
	 *         of the specified {@link ValidationStatusProvider}, and displays a
	 *         {@link ControlDecoration} over the underlying SWT control of all
	 *         target observables that implement {@link ISWTObservable} or
	 *         {@link IViewerObservable}.
	 */
	public static ControlDecorationSupport create(
			ValidationStatusProvider validationStatusProvider, int position,
			Composite composite) {
		return create(validationStatusProvider, position, composite,
				new ControlDecorationUpdater());
	}

	/**
	 * Creates a ControlDecorationSupport which observes the validation status
	 * of the specified {@link ValidationStatusProvider}, and displays a
	 * {@link ControlDecoration} over the underlying SWT control of all target
	 * observables that implement {@link ISWTObservable} or
	 * {@link IViewerObservable}.
	 * 
	 * @param validationStatusProvider
	 *            the {@link ValidationStatusProvider} to monitor.
	 * @param position
	 *            SWT alignment constant (e.g. SWT.LEFT | SWT.TOP) to use when
	 *            constructing {@link ControlDecoration} instances.
	 * @param composite
	 *            the composite to use when constructing
	 *            {@link ControlDecoration} instances.
	 * @param updater
	 *            custom strategy for updating the {@link ControlDecoration}(s)
	 *            whenever the validation status changes.
	 * @return a ControlDecorationSupport which observes the validation status
	 *         of the specified {@link ValidationStatusProvider}, and displays a
	 *         {@link ControlDecoration} over the underlying SWT control of all
	 *         target observables that implement {@link ISWTObservable} or
	 *         {@link IViewerObservable}.
	 */
	public static ControlDecorationSupport create(
			ValidationStatusProvider validationStatusProvider, int position,
			Composite composite, ControlDecorationUpdater updater) {
		return new ControlDecorationSupport(validationStatusProvider, position,
				composite, updater);
	}

	private final int position;
	private final Composite composite;
	private final ControlDecorationUpdater updater;

	private IObservableValue<IStatus> validationStatus;
	private IObservableList<IObservable> targets;

	private IDisposeListener disposeListener = new IDisposeListener() {
		@Override
		public void handleDispose(DisposeEvent staleEvent) {
			dispose();
		}
	};

	private IValueChangeListener<IStatus> statusChangeListener = new IValueChangeListener<IStatus>() {
		@Override
		public void handleValueChange(ValueChangeEvent<? extends IStatus> event) {
			statusChanged(validationStatus.getValue());
		}
	};

	private IListChangeListener<IObservable> targetsChangeListener = new IListChangeListener<IObservable>() {
		@Override
		public void handleListChange(ListChangeEvent<? extends IObservable> event) {
			handleListChange2(event);
		}

		private <E2 extends IObservable> void handleListChange2(ListChangeEvent<E2> event) {
			event.diff.accept(new ListDiffVisitor<E2>() {
				@Override
				public void handleAdd(int index, IObservable element) {
					targetAdded(element);
				}

				@Override
				public void handleRemove(int index, IObservable element) {
					targetRemoved(element);
				}
			});
			statusChanged(validationStatus.getValue());
		}
	};

	private static class TargetDecoration {
		public final IObservable target;
		public final ControlDecoration decoration;

		TargetDecoration(IObservable target, ControlDecoration decoration) {
			this.target = target;
			this.decoration = decoration;
		}
	}

	private List<TargetDecoration> targetDecorations;

	private ControlDecorationSupport(
			ValidationStatusProvider validationStatusProvider, int position,
			Composite composite, ControlDecorationUpdater updater) {
		this.position = position;
		this.composite = composite;
		this.updater = updater;

		this.validationStatus = validationStatusProvider.getValidationStatus();
		Assert.isTrue(!this.validationStatus.isDisposed());

		this.targets = validationStatusProvider.getTargets();
		Assert.isTrue(!this.targets.isDisposed());

		this.targetDecorations = new ArrayList<TargetDecoration>();

		validationStatus.addDisposeListener(disposeListener);
		validationStatus.addValueChangeListener(statusChangeListener);

		targets.addDisposeListener(disposeListener);
		targets.addListChangeListener(targetsChangeListener);

		for (Iterator<IObservable> it = targets.iterator(); it.hasNext();)
			targetAdded(it.next());

		statusChanged(validationStatus.getValue());
	}

	private void targetAdded(IObservable target) {
		Control control = findControl(target);
		if (control != null)
			targetDecorations.add(new TargetDecoration(target,
					new ControlDecoration(control, position, composite)));
	}

	private void targetRemoved(IObservable target) {
		for (Iterator<TargetDecoration> it = targetDecorations.iterator(); it
				.hasNext();) {
			TargetDecoration targetDecoration = it.next();
			if (targetDecoration.target == target) {
				targetDecoration.decoration.dispose();
				it.remove();
			}
		}
	}

	private Control findControl(IObservable target) {
		if (target instanceof ISWTObservable) {
			Widget widget = ((ISWTObservable) target).getWidget();
			if (widget instanceof Control)
				return (Control) widget;
		}

		if (target instanceof IViewerObservable) {
			Viewer viewer = ((IViewerObservable) target).getViewer();
			return viewer.getControl();
		}

		if (target instanceof IDecoratingObservable) {
			IObservable decorated = ((IDecoratingObservable) target)
					.getDecorated();
			Control control = findControl(decorated);
			if (control != null)
				return control;
		}

		if (target instanceof IObserving) {
			Object observed = ((IObserving) target).getObserved();
			if (observed instanceof IObservable)
				return findControl((IObservable) observed);
		}

		return null;
	}

	private void statusChanged(IStatus status) {
		for (Iterator<TargetDecoration> it = targetDecorations.iterator(); it
				.hasNext();) {
			TargetDecoration targetDecoration = it.next();
			ControlDecoration decoration = targetDecoration.decoration;
			updater.update(decoration, status);
		}
	}

	/**
	 * Disposes this ControlDecorationSupport, including all control decorations
	 * managed by it. A ControlDecorationSupport is automatically disposed when
	 * its target ValidationStatusProvider is disposed.
	 */
	public void dispose() {
		if (validationStatus != null) {
			validationStatus.removeDisposeListener(disposeListener);
			validationStatus.removeValueChangeListener(statusChangeListener);
			validationStatus = null;
		}

		if (targets != null) {
			targets.removeDisposeListener(disposeListener);
			targets.removeListChangeListener(targetsChangeListener);
			targets = null;
		}

		disposeListener = null;
		statusChangeListener = null;
		targetsChangeListener = null;

		if (targetDecorations != null) {
			for (Iterator<TargetDecoration> it = targetDecorations.iterator(); it
					.hasNext();) {
				TargetDecoration targetDecoration = it.next();
				targetDecoration.decoration.dispose();
			}
			targetDecorations.clear();
			targetDecorations = null;
		}
	}
}
