package net.sf.jmoney.handlers;

import java.util.Map;

import net.sf.jmoney.JMoneyPlugin;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * This is a generic handler that can be used to open an editor. The input to
 * the editor is the session, so this handler can be used whenever the editor is
 * editing the session as a whole and does not need anything more specific as
 * input.
 * 
 * To use this handler implementation, declare your handler as follows:
 * 
 * <pre>
 * &lt;command
 *           commandId=&quot;net.sf.jmoney.command.openEditor&quot;&gt;
 *    &lt;parameter name=&quot;net.sf.jmoney.openEditor.editorId&quot; 
 *                           value=&quot;&lt;&lt;&lt;put your editor id here&gt;&gt;&gt;&quot;/&gt;
 *     &lt;/command&gt;
 * </pre>
 */
public class OpenEditorHandler extends AbstractHandler {

	private static final String PARAMETER_EDITOR_ID = "net.sf.jmoney.openEditor.editorId";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Map parameters = event.getParameters();
		final String editorId = (String) parameters.get(PARAMETER_EDITOR_ID);

		try {
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			IEditorInput editorInput = new SessionEditorInput();
			
			/**
			 * Note that this extended form of openEditor is used because we must match on the editor id,
			 * not the input id.  The form that takes just the first two parameters will match on the input
			 * which is no good because the same input may be used for multiple editors.
			 */
			window.getActivePage().openEditor(editorInput, editorId, true, IWorkbenchPage.MATCH_ID);
		} catch (PartInitException e) {
			JMoneyPlugin.log(e);
		}

		return null;
	}
}
