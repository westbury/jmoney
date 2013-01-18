package net.sf.jmoney.handlers;

import net.sf.jmoney.resources.Messages;
import net.sf.jmoney.views.SessionEditorInputFactory;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

/**
 * This input is used for any editor which does not need input, the input to
 * the editor really being the session which is the input to the window page.
 * <P>
 * Therefore this input has no fields.
 */
public class SessionEditorInput implements IEditorInput, IPersistableElement {

	public SessionEditorInput() {
	}

	@Override
	public boolean exists() {
		// TODO: Should return false if session not open?
		return true;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	@Override
	public String getName() {
		return Messages.SessionEditorInput_Name;
	}

	/* (non-Javadoc)
     * @see org.eclipse.ui.IEditorInput#getPersistable()
     */
    @Override
	public IPersistableElement getPersistable() {
        // This class implements the IPersistableElement
    	// methods, so return a pointer to this object.
        return this;
    }

	@Override
	public String getToolTipText() {
		return Messages.SessionEditorInput_ToolTipText;
	}

	@Override
	public Object getAdapter(Class adapter) {
		return null;
	}

    @Override
    public boolean equals(Object obj) {
    	if (obj instanceof SessionEditorInput) {
    		return true;
    	}

    	return false;
    }

    @Override	
    public int hashCode() {
        return SessionEditorInput.class.hashCode();
    }

	@Override
	public String getFactoryId() {
		return SessionEditorInputFactory.ID;
	}

	/**
	 * @param memento
	 */
	@Override
	public void saveState(IMemento memento) {
		/*
		 * We save the part name and icon so that they appear correctly
		 * in the tab after a restore and before the editor is activated.
		 */
		
	}
}
