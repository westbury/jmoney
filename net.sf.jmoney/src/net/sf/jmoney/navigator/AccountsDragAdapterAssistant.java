package net.sf.jmoney.navigator;

import java.util.Iterator;

import net.sf.jmoney.model2.Account;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.navigator.CommonDragAdapterAssistant;

public class AccountsDragAdapterAssistant extends CommonDragAdapterAssistant {

	public AccountsDragAdapterAssistant() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Transfer[] getSupportedTransferTypes() {
		// Not needed????
		Transfer[] transfers= new Transfer[] {
				LocalSelectionTransfer.getTransfer()
		};
		return transfers;
	}

	@Override
	public boolean setDragData(DragSourceEvent event,
			IStructuredSelection selection) {
		if (selection != null) {
			for (Iterator iter= selection.iterator(); iter.hasNext();) {
				Object element= iter.next();
				if (!(element instanceof Account)) {
					return false;
				}
			}

			event.data = selection;
			return true;
		}
		return false;
	}

}
