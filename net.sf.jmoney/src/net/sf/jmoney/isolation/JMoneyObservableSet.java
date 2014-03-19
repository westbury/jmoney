package net.sf.jmoney.isolation;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.databinding.observable.AbstractObservable;
import org.eclipse.core.databinding.observable.ListenerList;
import org.eclipse.core.databinding.observable.ObservableTracker;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.set.IObservableSet;
import org.eclipse.core.databinding.observable.set.ISetChangeListener;
import org.eclipse.core.databinding.observable.set.SetChangeEvent;
import org.eclipse.core.databinding.observable.set.SetDiff;

/**
 * 
 * Abstract implementation of {@link IObservableSet}.
 * 
 * <p>
 * This class is thread safe. All state accessing methods must be invoked from
 * the {@link Realm#isCurrent() current realm}. Methods for adding and removing
 * listeners may be invoked from any thread.
 * </p>
 * 
 * @param <E>
 * 
 * @since 1.0
 * 
 */
public abstract class JMoneyObservableSet<E> extends AbstractObservable implements
		IObservableSet<E> {

	protected Set<E> wrappedSet;

	private boolean stale = false;

	/**
	 * @deprecated use getElementClass() instead
	 */
	protected Object elementType;

	/**
	 * @since 1.5
	 */
	private Class<E> elementClass;

	private ListenerList<ISetChangeListener<E>> setChangeListenerList = null;

	/**
	 * 
	 * @param wrappedSet
	 * @param elementType
	 * @deprecated use instead the form of the constructor that takes Class as
	 *             the parameter type for the element type
	 */
	protected JMoneyObservableSet(Set<E> wrappedSet, Object elementType) {
		this(Realm.getDefault(), wrappedSet, elementType);
	}

	/**
	 * 
	 * @param wrappedSet
	 * @param elementType
	 * @since 1.5
	 */
	protected JMoneyObservableSet(Set<E> wrappedSet, Class<E> elementType) {
		this(Realm.getDefault(), wrappedSet, elementType);
	}

	/**
	 * 
	 * @param realm
	 * @param wrappedSet
	 * @param elementType
	 * @deprecated use instead the form of the constructor that takes Class as
	 *             the parameter type for the element type
	 */
	protected JMoneyObservableSet(Realm realm, Set<E> wrappedSet, Object elementType) {
		super(realm);
		this.wrappedSet = wrappedSet;
		this.elementType = elementType;
		if (elementType instanceof Class) {
			this.elementClass = (Class<E>) elementType;
		} else {
			this.elementClass = null;
		}
	}

	/**
	 * 
	 * @param realm
	 * @param wrappedSet
	 * @param elementType
	 * @since 1.5
	 */
	// We must set deprecated fields in case any one uses them
	// @SuppressWarnings("deprecation")
	protected JMoneyObservableSet(Realm realm, Set<E> wrappedSet, Class<E> elementType) {
		super(realm);
		this.wrappedSet = wrappedSet;
		this.elementType = elementType;
		this.elementClass = elementType;
	}

	/**
	 * @param listener
	 */
	public synchronized void addSetChangeListener(ISetChangeListener<E> listener) {
		addListener(getSetChangeListenerList(), listener);
	}

	/**
	 * @param listener
	 */
	public synchronized void removeSetChangeListener(
			ISetChangeListener<E> listener) {
		if (setChangeListenerList != null) {
			removeListener(setChangeListenerList, listener);
		}
	}

	private ListenerList<ISetChangeListener<E>> getSetChangeListenerList() {
		if (setChangeListenerList == null) {
			setChangeListenerList = new ListenerList<ISetChangeListener<E>>();
		}
		return setChangeListenerList;
	}

	@Override
	protected boolean hasListeners() {
		return (setChangeListenerList != null && setChangeListenerList
				.hasListeners()) || super.hasListeners();
	}

	protected void fireSetChange(SetDiff<? extends E> diff) {
		// fire general change event first
		super.fireChange();

		if (setChangeListenerList != null) {
			setChangeListenerList.fireEvent(new SetChangeEvent<E>(this, diff));
		}
	}

	public boolean contains(Object o) {
		getterCalled();
		return wrappedSet.contains(o);
	}

	public boolean containsAll(Collection<?> c) {
		getterCalled();
		return wrappedSet.containsAll(c);
	}

	public boolean equals(Object o) {
		getterCalled();
		return o == this || wrappedSet.equals(o);
	}

	public int hashCode() {
		getterCalled();
		return wrappedSet.hashCode();
	}

	public boolean isEmpty() {
		getterCalled();
		return wrappedSet.isEmpty();
	}

	public Iterator<E> iterator() {
		getterCalled();
		final Iterator<E> wrappedIterator = wrappedSet.iterator();
		return new Iterator<E>() {

			public void remove() {
				wrappedIterator.remove();
			}

			public boolean hasNext() {
				ObservableTracker.getterCalled(JMoneyObservableSet.this);
				return wrappedIterator.hasNext();
			}

			public E next() {
				ObservableTracker.getterCalled(JMoneyObservableSet.this);
				return wrappedIterator.next();
			}
		};
	}

	public int size() {
		getterCalled();
		return wrappedSet.size();
	}

	public Object[] toArray() {
		getterCalled();
		return wrappedSet.toArray();
	}

	public <T> T[] toArray(T[] a) {
		getterCalled();
		return wrappedSet.toArray(a);
	}

	public String toString() {
		getterCalled();
		return wrappedSet.toString();
	}

	protected void getterCalled() {
		ObservableTracker.getterCalled(this);
	}

	public boolean add(E o) {
		throw new UnsupportedOperationException();
	}

	public boolean addAll(Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}

	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	public void clear() {
		throw new UnsupportedOperationException();
	}

	/**
	 * @return Returns the stale state.
	 */
	public boolean isStale() {
		getterCalled();
		return stale;
	}

	/**
	 * @param stale
	 *            The stale state to set. This will fire a stale event if the
	 *            given boolean is true and this observable set was not already
	 *            stale.
	 */
	public void setStale(boolean stale) {
		checkRealm();
		boolean wasStale = this.stale;
		this.stale = stale;
		if (!wasStale && stale) {
			fireStale();
		}
	}

	/**
	 * @param wrappedSet
	 *            The wrappedSet to set.
	 */
	protected void setWrappedSet(Set<E> wrappedSet) {
		this.wrappedSet = wrappedSet;
	}

	protected void fireChange() {
		throw new RuntimeException(
				"fireChange should not be called, use fireSetChange() instead"); //$NON-NLS-1$
	}

	public synchronized void dispose() {
		setChangeListenerList = null;
		super.dispose();
	}

	/**
	 * @deprecated use getElementClass instead
	 */
	public Object getElementType() {
		return elementType;
	}

	/**
	 * @since 1.5
	 */
	public Class<E> getElementClass() {
		return elementClass;
	}
}
