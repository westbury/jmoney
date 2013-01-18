/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2004 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.isolation;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Vector;

import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.Session;

import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.UndoContext;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;

/**
 * An object that manages a view on the data.
 * <P>
 * This is a base object that is extended by DatastoreManager to manage a view
 * of data committed to a datastore and is also extended by TransactionManager
 * to manage a view of uncommitted data.
 */
public abstract class DataManager<K extends IObjectKey> {
	
	private ChangeManager changeManager = new ChangeManager();

	private IUndoContext undoContext = new UndoContext();

    private Vector<WeakReference<SessionChangeListener>> sessionChangeListenerRefs = new Vector<WeakReference<SessionChangeListener>>();

    private Vector<SessionChangeListener> sessionChangeListeners = new Vector<SessionChangeListener>();

    private Vector<SessionChangeFirerListener> sessionChangeFirerListeners = new Vector<SessionChangeFirerListener>();

	private boolean sessionFiring = false;

//	private ReferenceQueue referenceQueue = new ReferenceQueue();

	public void addChangeListener(SessionChangeListener listener) {
        sessionChangeListeners.add(listener);
	}

	public void removeChangeListener(SessionChangeListener listener) {
        sessionChangeListeners.remove(listener);
	}

	/**
	 * Adds a change listener.
	 * <P>
     * Adds the listener to the collection of listeners who will be notified
     * when a change is made to the version of the datastore as seen through
     * this data manager.  Notifications will be sent when either a
     * change is committed to this data view by a transaction manager
     * or an uncommitted change is made through this data manager.
     * <P>
     * When listening for changes to a datastore, there are two options.
     * If the listener is interested only in receiving committed changes
     * then the listener should listen to the Session object or the JMoneyPlugin
     * object.  However, if a listener wants to be notified of changes
     * made through a transaction manager, even though those changes are
     * not committed to the datastore, then the listener should add the
     * listener to the transaction manager using this method.
     * <P>
     * The listener will not recieve any notification at the time a transaction
     * is committed as the listener will already have been notified of the
     * changes.  Note that there is no support for rollbacks as the transaction
     * manager can be just dropped (and garbage collected) without ever having been
     * committed, so getting a notification for a change that is never committed is
     * not an issue.  Views should do a full refresh if they change the data manager
     * through which they are obtaining the data to be shown.
     * <P>
     * This method maintains only a weak reference to the listener.  Therefore
     * the caller MUST maintain a reference to the listener.  If the caller does
     * not maintain a reference to the listener then the listener will be garbage
     * collected and the caller may wonder why no events are being notified.
	 */
	public void addChangeListenerWeakly(SessionChangeListener listener) {
        sessionChangeListenerRefs.add(new WeakReference<SessionChangeListener>(listener));
    }
    
	/**
	 * Adds a change listener.
	 * <P>
	 * The listener is active only for as long as the given control exists.  When the
	 * given control is disposed, the listener is removed and will receive no more
	 * notifications.
	 * <P>
	 * This method is generally used when a listener is used to update contents in a
	 * control.  Typically multiple controls are updated by a listener and the parent
	 * composite control is passed to this method.
	 * <P>
	 * This method creates a strong reference to the listener.  There is thus no need
	 * for the caller to maintain a reference to the listener.
	 * 
	 * @param listener
	 * @param control
	 */
	public void addChangeListener(final SessionChangeListener listener, Control control) {
        sessionChangeListeners.add(listener);
        
		// Remove the listener when the given control is disposed.
		control.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				sessionChangeListeners.remove(listener);
			}
		});
    }
	
	public void addSessionChangeFirerListener(SessionChangeFirerListener listener) {
        sessionChangeFirerListeners.add(listener);
    }
    
    public void removeSessionChangeFirerListener(SessionChangeFirerListener listener) {
        sessionChangeFirerListeners.remove(listener);
    }
    
    /**
	 * Send change notifications to all listeners who are listening for changes
	 * to the version of the datastore as seen through this data manager.
	 * <P>
	 * In practice it is likely that the only listener will be the JMoneyPlugin
	 * object. Views should all listen to the JMoneyPlugin class for changes to
	 * the model. The JMoneyPlugin object will pass on events from this session
	 * object.
	 * <P>
	 * Listeners may register directly with a session object. However if they do
	 * so then they must re-register whenever the session object changes. If a
	 * viewer wants to listen for changes to a session even if that session is
	 * not the session currently shown in the workbench then it should register
	 * with the session object, but if the viewer wants to be told about changes
	 * to the current workbench window then it should register with the
	 * JMoneyPlugin object.
	 * 
	 * This method is public because the layer above this data manager is
	 * responsible for calling this method. Only the above layer (code outside
	 * the data manager) knows, for example, when objectInserted should be
	 * called.
	 */
    public void fireEvent(ISessionChangeFirer firer) {
    	sessionFiring = true;
    	
    	/*
		 * Notify listeners who are listening to us using the
		 * SessionChangeFirerListener interface.
		 */
        if (!sessionChangeFirerListeners.isEmpty()) {
        	/*
			 * Take a copy of the listener list. By doing this we allow
			 * listeners to safely add or remove listeners.
			 */
        	SessionChangeFirerListener listenerArray[] = new SessionChangeFirerListener[sessionChangeFirerListeners.size()];
        	sessionChangeFirerListeners.copyInto(listenerArray);
        	for (int i = 0; i < listenerArray.length; i++) {
        		listenerArray[i].sessionChanged(firer);
        	}
        }
    	
    	/*
		 * Notify listeners who are listening to us using the
		 * SessionChangeListener interface.
		 * 
		 * Take a copy of the listener list. By doing this we allow listeners to
		 * safely add or remove listeners. Note that listeners from both the
		 * weakly referenced list and from the normal list must be processed.
		 */
        Vector<SessionChangeListener> listenerArray = new Vector<SessionChangeListener>();

        for (WeakReference<SessionChangeListener> listenerRef: sessionChangeListenerRefs) {
        	SessionChangeListener listener = listenerRef.get();
        	if (listener != null) {
        		listenerArray.add(listener);
        	}
        }

        for (SessionChangeListener listener: sessionChangeListeners) {
        	listenerArray.add(listener);
        }

        for (SessionChangeListener listener: listenerArray) {
    		firer.fire(listener);
    	}

        sessionFiring = false;
    }

    /**
     * This method is used by plug-ins so that they know if
     * code is being called from within change notification.
     *
     * It is important for plug-ins to know this.  Plug-ins
     * MUST NOT change the session data while a listener is
     * being notified of a change to the datastore.
     * This can happen very indirectly.  For example, suppose
     * an account is deleted.  The navigation view's listener
     * is notified and so removes the account's node from the
     * navigation tree.  If an account properties panel is
     * open, the panel is destroyed.  Because the panel is
     * being destroyed, the control that had the focus is sent
     * a 'focus lost' notification.  The 'focus lost' notification
     * takes the edited data from the control and writes it to
     * the datastore.
     * <P>
     * Writing data to the datastore during session change notifications
     * can cause serious problems.  The data may conflict.  The
     * undo/redo operations are almost impossible to manage.
     * In the above scenario with the deleted account, an attempt
     * is made to update a property for an object that has been
     * deleted.  The problems are endless.
     * <P>
     * It would be good if the datastore simply ignored such changes.
     * This would provide more robust support for plug-ins, and plug-ins
     * would not have to test this flag.  However, for the time being,
     * plug-ins must test this flag and avoid making changes when this
     * flag is set.  Plug-ins only need to do this in focus lost events
     * as that is the only time I can think of where this problem may
     * occur.
     *  
     * @return true if the session is notifying listeners of
     * 			a change to the session data, otherwise false
     */
    // TODO: Revisit this, especially the last paragraph above.
    public boolean isSessionFiring() {
    	return sessionFiring;
    }

	/**
	 * This method is called when a transaction is about to start.
	 * <P>
	 * If the datastore is kept in a transactional database then the code
	 * needed to start a transaction should be put in the implementation
	 * of this method.
	 * <P>
	 * The framework will always call this method, then make changes to
	 * the datastore, then call <code>commitTransaction</code> within
	 * a single function call.  The framework also ensures that no events
	 * are fired between the call to <code>startTransaction</code> and
	 * the call to <code>commitTransaction</code>.  The implementation of
	 * this method thus has no need to support or guard against nested
	 * transactions.
	 * 
	 * @see commitTransaction
	 */
	public abstract void startTransaction();

	/**
	 * This method is called when a transaction is to be committed.
	 * <P>
	 * If the datastore is kept in a transactional database then the code
	 * needed to commit the transaction should be put in the implementation
	 * of this method.
	 * 
	 * @see startTransaction
	 */
	public abstract void commitTransaction();

	public ChangeManager getChangeManager() {
		return changeManager;
	}

	/**
	 * JMoney supports undo/redo using a context that is based on the data
	 * manager.
	 * 
	 * The underlying data manager (supported by the underlying datastore) and
	 * the transaction manager session objects each have a different context.
	 * 
	 * Changes within a transaction manager have a separate context. This is
	 * necessary because if a view is making changes to an through a transaction
	 * manager to an uncommitted version of the datastore then the undo/redo
	 * menu needs to show those changes for the view.
	 * 
	 * @return the undo/redo context to be used for changes made to this session
	 */
	public IUndoContext getUndoContext() {
		// If not in a transaction, use the workbench context.
		// This may need some tidying up, but by using a common context,
		// this allows undo/redo to work even across a closing or
		// opening of a session.  There may be a better way of doing this.
		if (this instanceof TransactionManager) {
			return undoContext;
		} else {
			return PlatformUI.getWorkbench().getOperationSupport().getUndoContext();
		}
	}

	/**
	 * Returns the session object. Objects often need access to the session
	 * object. For example, the CapitalAccount constructor sets the currency to
	 * the default currency for the session if a null currency is passed.
	 * <P>
	 * WARNING: Be very careful about the use of this method. The session will
	 * not be set until it is constructed. Therefore any code that may be called
	 * during the initial construction of a session may not call this method.
	 * This includes extendable object constructors that are being called to
	 * construct an object store from a datastore. It does not include the
	 * constructor when adding a new object. As these are the same constructors,
	 * this is very confusing.
	 * 
	 * @return the session object
	 */
	public abstract Session getSession();
	
	/**
	 * @param account
	 * @return
	 */
	public abstract boolean hasEntries(Account account);

	/**
	 * @param account
	 * @return
	 */
	public abstract Collection<Entry> getEntries(Account account);
}

/*
//@author Santhosh Kumar T - santhosh@in.fiorano.com 
public class WeakPropertyChangeListener implements PropertyChangeListener{ 
    WeakReference listenerRef; 
    Object src; 
 
    public WeakPropertyChangeListener(PropertyChangeListener listener, Object src){ 
        listenerRef = new WeakReference(listener); 
        this.src = src; 
    } 
 
    public void propertyChange(PropertyChangeEvent evt){ 
        PropertyChangeListener listener = (PropertyChangeListener)listenerRef.get(); 
        if(listener==null){ 
            removeListener(); 
        }else 
            listener.propertyChange(evt); 
    } 
 
    private void removeListener(){ 
        try{ 
            Method method = src.getClass().getMethod("removePropertyChangeListener" 
                    , new Class[] {PropertyChangeListener.class}); 
            method.invoke(src, new Object[]{ this }); 
        } catch(Exception e){ 
            e.printStackTrace(); 
        } 
    } 
}


How to use this:

KeyboardFocusManager focusManager = 
   KeyboardFocusManager.getCurrentKeyboardFocusManager(); 
 
// instead of registering direclty use weak listener 
// focusManager.addPropertyChangeListener(focusOwnerListener); 
 
focusManager.addPropertyChangeListener( 
    new WeakPropertyChangeListener(focusOwnerListener, focusManager));


How does this work:

Instead of registering propertyChangeListener directly to keyboardFocusManager, we wrap it inside WeakPropertyChangeListener and register this weak listener to keyboardFocusManager. This weak listener acts a delegate.
It receives the propertyChangeEvents from keyboardFocusManager and delegates it the wrapped listener.

The interesting part of this weak listener, it hold a weakReference to the original propertyChangeListener. so this delegate is eligible for garbage collection which it is no longer reachable via references. When it gets garbage
collection, the weakReference will be pointing to null. On next propertyChangeEvent notification from keyboardFocusManager, it find that the weakReference is pointing to null, and unregisters itself from
keyboardFocusManager. Thus the weak listener will also become eligible for garbage collection in next gc cycle.

This concept is not something new. If you have a habit of looking into swing sources, you will find that AbstractButton actually adds a weak listener to its action. The weak listener class used for this is :
javax.swing.AbstractActionPropertyChangeListener; This class is package-private, so you don't find it in javadoc.

The full-fledged, generic implementation of weak listeners is available in Netbeans OpenAPI: WeakListeners.java . It is worth to have a look at it.

We create a subclass of ReferenceQueue, which will help us in performing such resource cleanup very easily:

//	 @author Santhosh Kumar T - santhosh@in.fiorano.com 
	public class ActiveReferenceQueue 
	          extends ReferenceQueue implements Runnable{ 
	    private static ActiveReferenceQueue singleton = null; 
	 
	    public static ActiveReferenceQueue getInstance(){ 
	        if(singleton==null) 
	            singleton = new ActiveReferenceQueue(); 
	        return singleton; 
	    } 
	 
	    private ActiveReferenceQueue(){ 
	        Thread t = new Thread(this, "ActiveReferenceQueue"); 
	        t.setDaemon(false); 
	        t.start(); 
	    } 
	 
	    public void run(){ 
	        for(;;){ 
	            try{ 
	                Reference ref = super.remove(0); 
	                if(ref instanceof Runnable){ 
	                    try{ 
	                        ((Runnable)ref).run(); 
	                    } catch(Exception e){ 
	                        e.printStackTrace(); 
	                    } 
	                } 
	            } catch(InterruptedException e){ 
	                e.printStackTrace(); 
	            } 
	        } 
	    } 
	} 


	ActiveReferenceQueue is a singleton class, and starts a thread when it is instantiated. This thread keeps polling for weakReferences that are eligible for garbage collection, and if those weakReferenes implement Runnable interface, it executes them.

	To use this, we can create a subclass of WeakReference implementing Runnable interface, which does the cleanup it run() method.

	Let us see how we will change the WeakPropertyChangeListener to use the above reference queue:

//	 @author Santhosh Kumar T - santhosh@in.fiorano.com 
	public class WeakPropertyChangeListener implements PropertyChangeListener{ 
	    WeakReference listenerRef; 
	    Object src; 
	 
	    public WeakPropertyChangeListener(PropertyChangeListener listener, Object src){ 
	        listenerRef = new ListenerReference(listener, this); 
	        this.src = src; 
	    } 
	 
	    public void propertyChange(PropertyChangeEvent evt){ 
	        PropertyChangeListener listener = (PropertyChangeListener)listenerRef.get(); 
	        if(listener!=null) 
	            listener.propertyChange(evt); 
	    } 
	 
	    private void removeListener(){ 
	        try{ 
	            Method method = src.getClass().getMethod("removePropertyChangeListener" 
	                    , new Class[] {PropertyChangeListener.class}); 
	            method.invoke(src, new Object[]{ this }); 
	        } catch(Exception e){ 
	            e.printStackTrace(); 
	        } 
	    } 
	 
	    static class ListenerReference extends WeakReference{ 
	        WeakPropertyChangeListener listener; 
	 
	        public ListenerReference(Object ref, WeakPropertyChangeListener listener){ 
	            super(ref, ActiveReferenceQueue.getInstance()); 
	            this.listener = listener; 
	        } 
	 
	        public void run(){ 
	            listener.removeListener(); 
	            listener = null; 
	        } 
	    } 
	}
*/