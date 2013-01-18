package net.sf.jmoney.isolation;


import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public abstract class AbstractDataOperation extends AbstractOperation {

	private ChangeManager changeManager;

	public AbstractDataOperation(ChangeManager changeManager, String label) {
		super(label);
		this.changeManager = changeManager;
	}

	/**
	 * The changes that need to be undone to 'undo' this operation (i.e. these
	 * are the changes made when the operation was executed or last redone), or
	 * null if this operation has not yet been executed or has been undone.
	 */
	private ChangeManager.UndoableChange redoChanges = null;

	/**
	 * The changes that need to be undone to 'redo' this operation (i.e. these
	 * are the changes made when the operation was last undone), or null if this
	 * operation has not yet been executed or has not yet been undone, or if
	 * this operation was redone since it was last undone.
	 */
	private ChangeManager.UndoableChange undoChanges = null;

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		changeManager.setUndoableChange();

		execute();

		redoChanges = changeManager.takeUndoableChange();

		return Status.OK_STATUS;
	}

	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		changeManager.setUndoableChange();

		undoChanges.undoChanges();
		undoChanges = null;

		redoChanges = changeManager.takeUndoableChange();

		return Status.OK_STATUS;
	}

	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		changeManager.setUndoableChange();

		redoChanges.undoChanges();
		redoChanges = null;

		undoChanges = changeManager.takeUndoableChange();

		return Status.OK_STATUS;
	}

	public abstract IStatus execute() throws ExecutionException;
}
