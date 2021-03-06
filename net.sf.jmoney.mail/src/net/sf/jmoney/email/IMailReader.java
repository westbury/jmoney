package net.sf.jmoney.email;

import java.util.Date;
import java.util.Set;

import net.sf.jmoney.model2.Session;

/**
 * Interface to be implemented by all extensions
 * that know how to extract accounting information from e-mail.
 * 
 * @author westbury.nigel2
 *
 */
public interface IMailReader {

	/**
	 * Called with message header information to see if this message may
	 * be of interest.  This allows the caller to fetch the message body
	 * only for messages that may be of interest.
	 * 
	 * @param fromAddresses
	 * @return
	 */
	boolean mayProcessMessage(Set<String> fromAddresses);
	
	boolean processPlainTextMessage(Session session, Date date, String content) throws UnexpectedContentException;

	boolean processHtmlMessage(Session session, Date date, String content) throws UnexpectedContentException;

	/**
	 * Called on a message that may or may not contain
	 * content that can be extracted.
	 * <P>
	 * The return value controls whether the e-mail is to be
	 * deleted after it has been read.  Generally <code>true</code>
	 * is returned if content was extracted and <code>false</code>
	 * if not.  However <code>true</code> may be returned even if no
	 * data were extracted, for example if this is an e-mail of a type
	 * that sometimes contains information and sometimes only junk, or it
	 * may be that even though information was extracted the e-mail still contains
	 * other information that may be useful to a human reader or to other
	 * plug-ins.
	 * 
	 * @return true if the e-mail contains nothing further worth keeping and should
	 * 		be deleted, false if the e-mail is to be kept and passed on to other
	 * 		plug-ins
	 * @throws UnexpectedContentException 
	 */
	boolean processMimeMultipartMessage(Session session, Date date, IContentReader content) throws UnexpectedContentException;
}
