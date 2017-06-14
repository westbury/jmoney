package amazonscraper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;

import txr.matchers.DocumentMatcher;
import txr.matchers.MatchResults;

public class AmazonScraperContext {

	// Lazily created
	private DocumentMatcher ordersMatcher = null;

	// Lazily created
	private DocumentMatcher detailsMatcher = null;

	public MatchResults pasteOrdersFromClipboard() {
		if (ordersMatcher == null) {
			ordersMatcher = createMatcherFromResource("amazon-orders.txr");
		}

		MatchResults bindings = doMatchingFromClipboard(ordersMatcher);

		if (bindings == null || bindings.getCollections(0).isEmpty()) {
			throw new RuntimeException("Data in clipboard does not appear to be copied from the orders page.");
		}

		return bindings;
	}

	public MatchResults pasteDetailsFromClipboard() {
		if (detailsMatcher == null) {
			detailsMatcher = createMatcherFromResource("amazon-details.txr");
		}

		MatchResults orderBindings = doMatchingFromClipboard(detailsMatcher);

		if (orderBindings == null) {
			throw new RuntimeException("Data in clipboard does not appear to be a details page.");
		}
		
		return orderBindings;
	}

	private MatchResults doMatchingFromClipboard(DocumentMatcher matcher) {
		Display display = Display.getCurrent();
		Clipboard clipboard = new Clipboard(display);
		String plainText = (String)clipboard.getContents(TextTransfer.getInstance());
		clipboard.dispose();        

		MatchResults bindings = matcher.process(plainText);

		return bindings;
	}

	private DocumentMatcher createMatcherFromResource(String resourceName) {
		ClassLoader classLoader = getClass().getClassLoader();
		URL resource = classLoader.getResource(resourceName);
		try (InputStream txrInputStream = resource.openStream()) {
			return new DocumentMatcher(txrInputStream, "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

}
