package net.sf.jmoney.email;

import javax.mail.MessagingException;

import net.sf.jmoney.email.handlers.IMultipartProcessor;

public interface IContentReader {

	void expectPlainText(ITextProcessor textProcessor) throws UnexpectedContentException;

	void expectHtml(ITextProcessor textProcessor) throws UnexpectedContentException;

	void expectBase64() throws UnexpectedContentException;
	
	void expectMimeMessage(ITextProcessor textProcessor) throws UnexpectedContentException;
	
	void expectMimeMultipart(IMultipartProcessor multipartProcessor) throws UnexpectedContentException;
}
