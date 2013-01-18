package net.sf.jmoney.search.views;

import net.sf.jmoney.search.Activator;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;


/*
 * The page for setting the Search preferences.
 */
public class SearchPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public static final String PAGE_ID = "net.sf.jmoney.search.preferences.SearchPreferencePage"; //$NON-NLS-1$
	
	
	public static final String REUSE_EDITOR = "org.eclipse.search.reuseEditor"; //$NON-NLS-1$
	public static final String BRING_VIEW_TO_FRONT = "org.eclipse.search.bringToFront"; //$NON-NLS-1$
	public static final String LIMIT_HISTORY = "org.eclipse.search.limitHistory"; //$NON-NLS-1$
    
	public SearchPreferencePage() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
	}

	public static void initDefaults(IPreferenceStore store) {
		store.setDefault(REUSE_EDITOR, true);
		store.setDefault(BRING_VIEW_TO_FRONT, true);
		store.setDefault(LIMIT_HISTORY, 10);
	}


	public void createControl(Composite parent) {
		super.createControl(parent);
//		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), ISearchHelpContextIds.SEARCH_PREFERENCE_PAGE);
	}
	
	protected void createFieldEditors() {
		addField(new BooleanFieldEditor(REUSE_EDITOR, "&Reuse editors to show matches", getFieldEditorParent()));
		addField(new BooleanFieldEditor(BRING_VIEW_TO_FRONT, "&Bring search view to front after search", getFieldEditorParent()));
	}

	public void init(IWorkbench workbench) {
	}

	public static boolean isEditorReused() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		return store.getBoolean(REUSE_EDITOR);
	}
	
	public static boolean isViewBroughtToFront() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		return store.getBoolean(BRING_VIEW_TO_FRONT);
	}

	public static int getHistoryLimit() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		int limit = store.getInt(LIMIT_HISTORY);
		if (limit < 1) {
			limit = 1;
		} else if (limit >= 100) {
			limit = 99;
		}
		return limit;
	}
}
