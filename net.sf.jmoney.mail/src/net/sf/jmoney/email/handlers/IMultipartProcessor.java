package net.sf.jmoney.email.handlers;

import net.sf.jmoney.email.IContentReader;

public interface IMultipartProcessor {

	void processParts(IContentReader contentReader);

}
