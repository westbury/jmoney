package net.sf.jmoney;

import java.io.File;

import org.eclipse.ui.IWorkbenchWindow;

public interface IDroppedFileImporter {

	void importFile(File file, IWorkbenchWindow window);

}
