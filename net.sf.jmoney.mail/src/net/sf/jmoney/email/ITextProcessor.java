package net.sf.jmoney.email;

public interface ITextProcessor {
	void processText(String content) throws UnexpectedContentException;
}
