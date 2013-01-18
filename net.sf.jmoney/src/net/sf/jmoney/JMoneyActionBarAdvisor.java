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

import net.sf.jmoney.resources.Messages;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;

/**
 * @author Johann Gyger
 */
public class JMoneyActionBarAdvisor extends ActionBarAdvisor {

    private final IWorkbenchWindow window;
    private IAction newAction;
    private IAction importAction;
    private IAction exportAction;
    private IAction quitAction;
    private IAction undoAction;
    private IAction redoAction;
    private IAction preferencesAction;
    private IAction introAction;
    private IAction aboutAction;
    private IWorkbenchAction showHelpAction;
    private IWorkbenchAction searchHelpAction;
    private IWorkbenchAction dynamicHelpAction;

    private ContributionItem newTransactionItem;
    private ContributionItem deleteTransactionItem;
    private ContributionItem duplicateTransactionItem;
    private ContributionItem cutTransactionItem;
    private ContributionItem pasteCombineTransactionItem;
    private ContributionItem viewTransactionItem;

    public JMoneyActionBarAdvisor(IActionBarConfigurer configurer) {
        super(configurer);
        window = configurer.getWindowConfigurer().getWindow();
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.application.ActionBarAdvisor#makeActions(org.eclipse.ui.IWorkbenchWindow)
     */
    @Override	
    protected void makeActions(final IWorkbenchWindow window) {
    	/*
    	 * Define the global retargetable commands.
    	 * These do not have to be cleaned up when this plug-in is unloaded because
    	 * this action bar advisor is used only when this plug-in's application is the
    	 * running application, so this plug-in can't be unloaded without stopping the
    	 * application.  Thus these commands do not have to be cleaned up in
    	 * Plugin.shutdown. 
    	 */
//    	ICommandService cmdService = (ICommandService)PlatformUI.getWorkbench().getAdapter(ICommandService.class);
//    	IBindingService bindingService = (IBindingService)PlatformUI.getWorkbench().getAdapter(IBindingService.class);
//    	Category editCategory = cmdService.getCategory("net.sf.jmoney.category.edit");
//    	Category navigateCategory = cmdService.getCategory("net.sf.jmoney.category.navigate");
//
//    	Command deleteTransactionCommand = cmdService.getCommand("net.sf.jmoney.deleteTransaction");
//    	deleteTransactionCommand.define("Delete Transaction", "Delete A Transaction", editCategory);
//    	
//    	Command duplicateTransactionCommand = cmdService.getCommand("net.sf.jmoney.deleteTransaction");
//    	duplicateTransactionCommand.define("Duplicate Transaction", "Duplicate A Transaction", editCategory);
//    	
//    	Command deleteTransactionCommand = cmdService.getCommand("net.sf.jmoney.deleteTransaction");
//    	deleteTransactionCommand.define("Delete Transaction", "Delete A Transaction", editCategory);
//    	
//    	Command deleteTransactionCommand = cmdService.getCommand("net.sf.jmoney.deleteTransaction");
//    	deleteTransactionCommand.define("Delete Transaction", "Delete A Transaction", editCategory);

        newAction = ActionFactory.NEW_WIZARD_DROP_DOWN.create(window);
        register(newAction);

        importAction = ActionFactory.IMPORT.create(window);
        register(importAction);

        exportAction = ActionFactory.EXPORT.create(window);
        register(exportAction);

        quitAction = ActionFactory.QUIT.create(window);
        register(quitAction);

        preferencesAction = ActionFactory.PREFERENCES.create(window);
        register(preferencesAction);

        undoAction = ActionFactory.UNDO.create(window);
        register(undoAction);
        
        redoAction = ActionFactory.REDO.create(window);
        register(redoAction);
        
        showHelpAction = ActionFactory.HELP_CONTENTS.create(window);
        register(showHelpAction);

        searchHelpAction = ActionFactory.HELP_SEARCH.create(window);
        register(searchHelpAction);

        dynamicHelpAction = ActionFactory.DYNAMIC_HELP.create(window);
        register(dynamicHelpAction);
        
        if (window.getWorkbench().getIntroManager().hasIntro()) {
            introAction = ActionFactory.INTRO.create(window);
            register(introAction);
        }

        aboutAction = ActionFactory.ABOUT.create(window);
        register(aboutAction);
        
    	// Create the contribution items from the commands.
        
		newTransactionItem = createContributionItemFromCommand(
				"net.sf.jmoney.newTransaction", //$NON-NLS-1$
				"new_entry.gif", //$NON-NLS-1$
				Messages.JMoneyActionBarAdvisor_NewTransactionToolTipText);

		deleteTransactionItem = createContributionItemFromCommand(
				"net.sf.jmoney.deleteTransaction", //$NON-NLS-1$
				"delete_entry.gif", //$NON-NLS-1$
				Messages.JMoneyActionBarAdvisor_DeleteTransactionToolTipText);

		duplicateTransactionItem = createContributionItemFromCommand(
				"net.sf.jmoney.duplicateTransaction", //$NON-NLS-1$
				"duplicate_entry.gif", //$NON-NLS-1$
				Messages.JMoneyActionBarAdvisor_DuplicateTransactionToolTipText);

		cutTransactionItem = createContributionItemFromCommand(
				"net.sf.jmoney.cutTransaction", //$NON-NLS-1$
				"delete_entry.gif", //$NON-NLS-1$
				"Mark the selected transaction so it can be combined with another.");

		pasteCombineTransactionItem = createContributionItemFromCommand(
				"net.sf.jmoney.pasteCombineTransaction", //$NON-NLS-1$
				"delete_entry.gif", //$NON-NLS-1$
				"Paste entries from the cut transaction into this one, and delete the cut transaction.");

		viewTransactionItem = createContributionItemFromCommand(
				"net.sf.jmoney.transactionDetails", //$NON-NLS-1$
				"view_transaction.gif", //$NON-NLS-1$
				Messages.JMoneyActionBarAdvisor_TransactionDetailsToolTipText);
    }

    /**
     * Helper method to create a contribution item that wraps a command.  This
     * method is used so that commands can be added to the menu and
     * toolbar programatically.
     * <P>
     * This could be done in plugin.xml, but by adding this plug-in's
     * own commands programatically, we have full control over the initial
     * layout and ordering of the menu and toolbar.
     */
	private ContributionItem createContributionItemFromCommand(String commandId, String iconFile, String tooltip) {
		CommandContributionItemParameter params = new CommandContributionItemParameter(
    			PlatformUI.getWorkbench(), 
    			null, 
    			commandId,
				CommandContributionItem.STYLE_PUSH);
		params.tooltip = tooltip;
		params.icon = JMoneyPlugin.createImageDescriptor(iconFile);
		return new CommandContributionItem(params);
	}

    /* (non-Javadoc)
     * @see org.eclipse.ui.application.ActionBarAdvisor#fillMenuBar(org.eclipse.jface.action.IMenuManager)
     */
    @Override	
    protected void fillMenuBar(IMenuManager menuBar) {
        menuBar.add(createFileMenu());
        menuBar.add(createEditMenu());
        menuBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        menuBar.add(createNavigateMenu());
        menuBar.add(createWindowMenu());
        menuBar.add(createHelpMenu());
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.application.ActionBarAdvisor#fillMenuBar(org.eclipse.jface.action.IMenuManager)
     */
    @Override	
    protected void fillCoolBar(ICoolBarManager coolBar) {
    	super.fillCoolBar(coolBar);
    	
    	IToolBarManager toolbar = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
    	coolBar.add(new ToolBarContributionItem(toolbar, "main")); //$NON-NLS-1$
    	toolbar.add(newAction);

    	toolbar.add(new Separator("edit")); //$NON-NLS-1$
		toolbar.add(newTransactionItem);
		toolbar.add(deleteTransactionItem);
		toolbar.add(duplicateTransactionItem);
    	
    	toolbar.add(new Separator("navigate")); //$NON-NLS-1$
		toolbar.add(viewTransactionItem);
    	
    	toolbar.add(new Separator("openEditors")); //$NON-NLS-1$

    	toolbar.add(new Separator());
    	toolbar.add(importAction);
        toolbar.add(exportAction);
    }

    /**
     * Creates and returns the File menu.
     */
    private MenuManager createFileMenu() {
        MenuManager menu = new MenuManager(Messages.JMoneyActionBarAdvisor_MenuFileName, IWorkbenchActionConstants.M_FILE);

        menu.add(new GroupMarker(IWorkbenchActionConstants.FILE_START));

        menu.add(new Separator("sessionGroup")); //$NON-NLS-1$

        /*
         * This group is used only so that we can add the 'close session' command
         * at this position.  We should be able to just add the menu item here,
         * but I cannot get that to work, so we add the group here and then add
         * the 'close session' to this group declaratively in plugin.xml. 
         */
        menu.add(new Separator("closeSessionGroup")); //$NON-NLS-1$

        menu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

        menu.add(new Separator());
        menu.add(importAction);
        menu.add(exportAction);

        menu.add(new Separator());
        menu.add(quitAction);
        menu.add(new GroupMarker(IWorkbenchActionConstants.FILE_END));

        return menu;
    }   

    /**
     * Creates and returns the Edit menu.
     */
    private MenuManager createEditMenu() {
        MenuManager menu = new MenuManager(Messages.JMoneyActionBarAdvisor_MenuEditName, IWorkbenchActionConstants.M_EDIT);

        menu.add(new GroupMarker(IWorkbenchActionConstants.FILE_START));
        menu.add(undoAction);
        menu.add(redoAction);

        menu.add(new Separator("transactions")); //$NON-NLS-1$
        menu.add(newTransactionItem);
        menu.add(deleteTransactionItem);
        menu.add(duplicateTransactionItem);
        menu.add(cutTransactionItem);
        menu.add(pasteCombineTransactionItem);
    	
        menu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        menu.add(new GroupMarker(IWorkbenchActionConstants.FILE_END));

        return menu;
    }   

    /**
     * Creates and returns the Navigate menu.
     */
    private MenuManager createNavigateMenu() {
        MenuManager menu = new MenuManager(Messages.JMoneyActionBarAdvisor_MenuNavigateName, IWorkbenchActionConstants.M_NAVIGATE);
        menu.add(new Separator("openEditors")); //$NON-NLS-1$

        // TODO: Is this the right place in the menu for this?
        menu.add(new Separator("openDialogs")); //$NON-NLS-1$
        menu.add(viewTransactionItem);

		menu.add(new Separator());
        menu.add(createReportsMenu());
        menu.add(createChartsMenu());
        menu.add(new Separator());
        menu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        return menu;
    }

    /**
     * Creates and returns the Reports menu.
     */
    private MenuManager createReportsMenu() {
        MenuManager menu = new MenuManager(Messages.JMoneyActionBarAdvisor_MenuReportsName, JMoneyPlugin.createImageDescriptor("report.gif"), "reports");  //$NON-NLS-1$//$NON-NLS-2$
        menu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        return menu;
    }

    /**
     * Creates and returns the Charts menu.
     */
    private MenuManager createChartsMenu() {
        MenuManager menu = new MenuManager(Messages.JMoneyActionBarAdvisor_MenuChartsName, JMoneyPlugin.createImageDescriptor("chart.gif"), "charts");  //$NON-NLS-1$//$NON-NLS-2$
        menu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        return menu;
    }

    /**
     * Creates and returns the Window menu.
     */
    private MenuManager createWindowMenu() {
        MenuManager menu = new MenuManager(Messages.JMoneyActionBarAdvisor_MenuWindowName, IWorkbenchActionConstants.M_WINDOW);
        {
            MenuManager showViewMenuMgr = new MenuManager(Messages.JMoneyActionBarAdvisor_MenuShowViewName, "showView"); //$NON-NLS-1$
            IContributionItem showViewMenu = ContributionItemFactory.VIEWS_SHORTLIST.create(window);
            showViewMenuMgr.add(showViewMenu);
            menu.add(showViewMenuMgr);
        }
        menu.add(new Separator());
        menu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        menu.add(new Separator());
        menu.add(preferencesAction);
        return menu;
    }

    /**
     * Creates and returns the Help menu.
     */
    private MenuManager createHelpMenu() {
        MenuManager menu = new MenuManager(Messages.JMoneyActionBarAdvisor_MenuHelpName, IWorkbenchActionConstants.M_HELP);

        // Help
        if (introAction != null) {
            menu.add(introAction);
        }
        menu.add(new Separator());
        menu.add(new GroupMarker(IWorkbenchActionConstants.HELP_START));
        menu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        menu.add(new Separator());
        menu.add(showHelpAction);
        menu.add(searchHelpAction);
        menu.add(dynamicHelpAction);
        menu.add(new Separator());
        menu.add(aboutAction);
        menu.add(new GroupMarker(IWorkbenchActionConstants.HELP_END));    

        return menu;
    }

}
