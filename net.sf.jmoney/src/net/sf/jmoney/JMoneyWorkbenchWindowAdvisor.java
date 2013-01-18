/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2005 Johann Gyger <jgyger@users.sourceforge.net>
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

package net.sf.jmoney;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import net.sf.jmoney.model2.DatastoreManager;
import net.sf.jmoney.model2.MalformedPluginException;
import net.sf.jmoney.resources.Messages;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * @author Johann Gyger
 */
public class JMoneyWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

	public JMoneyWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
		super(configurer);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.application.WorkbenchWindowAdvisor#createActionBarAdvisor(org.eclipse.ui.application.IActionBarConfigurer)
	 */
    @Override	
	public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
		return new JMoneyActionBarAdvisor(configurer);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.application.WorkbenchWindowAdvisor#preWindowOpen()
	 */
	@Override
	public void preWindowOpen() {
		final IWorkbenchWindowConfigurer configurer = getWindowConfigurer();

		configurer.setShowCoolBar(true);
		configurer.setShowStatusLine(true);

		// JMoney has only one perspective, but other plug-ins may add more
		configurer.setShowPerspectiveBar(true);

        // Allow files to be dropped into the JMoney editor area
    	configurer.addEditorAreaTransfer(FileTransfer.getInstance());
    	
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		
		/**
		 * Maps file extensions to configuration elements that can handle a drop
		 * of files with that extension. Note the all extensions are put in the
		 * map as upper case, so the case of the extension of the dropped file
		 * is not relevant, even of platforms with case-sensitive file names,
		 */
		final Map<String, IConfigurationElement> extensionMap = new HashMap<String, IConfigurationElement>();
		
		for (IConfigurationElement element: registry.getConfigurationElementsFor("net.sf.jmoney.dropHandlers")) { //$NON-NLS-1$
			if (element.getName().equals("filetype")) { //$NON-NLS-1$
				String fileExtension = element.getAttribute("extension"); //$NON-NLS-1$
				extensionMap.put(fileExtension.toUpperCase(), element);
			}
		}
		
    	configurer.configureEditorAreaDropListener(new DropTargetListener() {

			@Override
			public void dragEnter(DropTargetEvent event) {
				String fileList[] = null;
				FileTransfer ft = FileTransfer.getInstance();
				if (ft.isSupportedType(event.currentDataType)) {
					fileList = (String[])event.data;
					
					if (fileList == null) {
						// We don't get to see the dragged data here,
						// so we can only assume it is valid.
					} else {
						for (String fileName : fileList) {
							File file = new File(fileName);

							String [] parts = file.getName().split(".");
							String extension = parts[parts.length-1];

							if (!extensionMap.containsKey(extension)) {
								event.detail = DND.DROP_NONE;
								return;
							}
						}
					}
				}

				if (event.detail == DND.DROP_DEFAULT) {
					/*
					 * If the user did not specifically modify the drag to be
					 * a move or a copy, set to be a copy.
					 */
		        	event.detail = DND.DROP_COPY;
				}
			}

			@Override
			public void dragLeave(DropTargetEvent event) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void dragOperationChanged(DropTargetEvent event) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void dragOver(DropTargetEvent event) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void drop(DropTargetEvent event) {
				String fileList[] = null;
				FileTransfer ft = FileTransfer.getInstance();
				if (ft.isSupportedType(event.currentDataType)) {
					fileList = (String[])event.data;
					
					for (String fileName : fileList) {
						File file = new File(fileName);
						
						String [] parts = file.getName().split("\\.");
						String extension = parts[parts.length-1];
						
						try {
							IConfigurationElement element = extensionMap.get(extension.toUpperCase());
							Object executableExtension = element.createExecutableExtension("class"); //$NON-NLS-1$
							if (!(executableExtension instanceof IDroppedFileImporter)) {
								String message = NLS.bind(
										Messages.MalformedPlugin_BadExtensionClass,
										new Object [] {
												element.getContributor().getName(),
												"net.sf.jmoney.dropHandlers", //$NON-NLS-1$
												executableExtension.getClass().getName(),
												IDroppedFileImporter.class.getName()
										});
								throw new MalformedPluginException(message);
							}

							IDroppedFileImporter dropHandler = (IDroppedFileImporter)executableExtension;
							dropHandler.importFile(file, configurer.getWindow());
						} catch (CoreException e) {
							StatusManager.getManager().handle(e, JMoneyPlugin.PLUGIN_ID);
						}
					}
				}
			}

			@Override
			public void dropAccept(DropTargetEvent event) {
				// TODO Auto-generated method stub
				
			}
		});
    }

	@Override
	public void postWindowRestore() {
		/*
		 * The title of a window should show a brief description of the input.
		 * This is set whenever a session is opened by the code that processed the
		 * action or handler that opened the session.  However in the case where
		 * a session is restored as a part of workbench restore, the title must be
		 * set here.
		 */
		IWorkbenchWindow window = getWindowConfigurer().getWindow();
		DatastoreManager sessionManager = (DatastoreManager)window.getActivePage().getInput();
		
		/*
		 * It is possible we are restoring a workbench window that has no session opened in it.
		 * In such a situation, the input will be null and we want to leave the title as it is
		 * with just the product name.
		 */
		if (sessionManager != null) {
			String productName = Platform.getProduct().getName();
			window.getShell().setText(
					productName + " - "	+ sessionManager.getBriefDescription());
		}
	}

}
