package net.sf.jmoney.email.handlers;

import java.io.IOException;
import java.security.Security;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import net.sf.jmoney.email.Activator;
import net.sf.jmoney.email.IMailReader;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.DatastoreManager;
import net.sf.jmoney.resources.Messages;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com.sun.mail.pop3.POP3SSLStore;
import com.sun.mail.util.BASE64DecoderStream;
/**
 * Reads mail, looking for e-mail messages that have information that
 * can be imported into JMoney.
 */
public class EmailImportHandler extends AbstractHandler {

	private static final String MAILBOXES_KEY = "mailboxes";

	private static final String MAIL_IMPORTERS_KEY = "mailimporters";

	private Session mailSession;

	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = HandlerUtil.getActiveShellChecked(event);
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		DatastoreManager sessionManager = (DatastoreManager)window.getActivePage().getInput();
		if (sessionManager == null) {
			MessageDialog.openWarning(
					shell,
					Messages.CloseSessionAction_WarningTitle,
					Messages.CloseSessionAction_WarningMessage);
		} else {
	        IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();

	        Map<String, Mailbox> mailboxes = new HashMap<String, Mailbox>();
	        String value = preferenceStore.getString(MAILBOXES_KEY);
	        if (!value.isEmpty()) {
	        	String mailboxValues [] = value.split("~");
	        	for (String mailboxValue : mailboxValues) {
	        		String parts [] = mailboxValue.split(",");
	        		if (parts.length == 3) {
	        			mailboxes.put(parts[1], new Mailbox(parts[0], parts[1], parts[2]));
	        		} else if (parts.length == 2) {
	        			mailboxes.put(parts[1], new Mailbox(parts[0], parts[1], ""));
	        		}
	        	}
	        }

			
	        // Load the extensions
	        IExtensionRegistry registry = Platform.getExtensionRegistry();
	        IExtensionPoint extensionPoint = registry.getExtensionPoint("net.sf.jmoney.mail.mailimporter");
	        IExtension[] extensions = extensionPoint.getExtensions();

	        try {
	        	for (int i = 0; i < extensions.length; i++) {
	        		IConfigurationElement[] elements = extensions[i].getConfigurationElements();
	        		for (int j = 0; j < elements.length; j++) {
	        			if (elements[j].getName().equals("mail")) {
	        				String id = elements[j].getAttribute("id");
	        				String name = elements[j].getAttribute("name");
	        				String description = "";
	        				IConfigurationElement[] descriptionElement = elements[j].getChildren("description");
	        				if (descriptionElement.length == 1) {
	        					description = descriptionElement[0].getValue();
	        				}
	        				IMailReader importer = (IMailReader)elements[j].createExecutableExtension("class");
	        				
	        				String qualifiedId = elements[j].getNamespaceIdentifier() + "." + id;
	        				String address = preferenceStore.getString(MAIL_IMPORTERS_KEY + "." + qualifiedId + ".address");
	        				if (address != null) {
	        					Mailbox mailbox = mailboxes.get(address);
	        					mailbox.addImporter(importer);
	        				}
	        			}
	        		}
	        	}

	        	/*
	        	 * Now loop around all mailboxes and import from each one.
	        	 * We have already grouped the importers by mailbox so each
	        	 * message need be read only once and passed to all importers
	        	 * for its mailbox.
	        	 */
	        	for (Mailbox mailbox : mailboxes.values()) {
	        		if (!mailbox.getImporters().isEmpty()) {
	        			String host = mailbox.host;
	        			String username = mailbox.address.split("@")[0];

						/*
						 * Users may prefer that their passwords are not stored
						 * in plain text in preferences. So if there is no
						 * password, prompt the user now.
						 */
	        			String password;
	        			if (mailbox.password.isEmpty()) {
	        				InputDialog dialog = new InputDialog(shell, "Password Request", "Enter password for " + mailbox.address, "", null);
	        				if (dialog.open() != Dialog.OK) {
	        					continue;
	        				}
	        				password = dialog.getValue();
	        			} else {
	        				password = mailbox.password;
	        			}

	        			int port = 995;

	        			Properties properties = new Properties();
	        			//					properties.setProperty("mail.pop3.host", host);
	        			properties.setProperty("mail.pop3.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
	        			properties.setProperty("mail.pop3.socketFactory.fallback", "false");
	        			properties.setProperty("mail.pop3.port", Integer.toString(port));
	        			properties.setProperty("mail.pop3.socketFactory.port", Integer.toString(port));
	        			properties.setProperty("mail.pop3.ssl", "true");
	        			Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());

	        			mailSession = Session.getInstance(properties, null);

	        			URLName urln = new URLName("pop3", host, port, null, username, password);
	        			Store store = new POP3SSLStore(mailSession, urln);

	        			try {
	        				store.connect(host, username, password);
	        			} catch (MessagingException ex) {
	        				MessageDialog.openInformation(
	        						window.getShell(),
	        						"Mail client Error",
	        						" couldn't connect to mail server ,please make sure you entered to right(username,password). Or make sure of your mail client connect configuration under props/config.properties file.");
	        			}

	        			Message[] messages  = readAllMessages(window, store, host);
	        			transform(sessionManager, messages, mailbox.importers);
	        		}
	        	}
	        } catch (CoreException e) {
	        	e.printStackTrace();
	        	throw new RuntimeException(e);
	        }
		}

		return null;
	}

	public Message[] readAllMessages(IWorkbenchWindow window, Store store, String host) {

		try {

			Folder [] personalFolders = store.getPersonalNamespaces();
			Folder [] sharedFolders = store.getSharedNamespaces();
			
			Folder folder = store.getDefaultFolder();
			folder = folder.getFolder("INBOX");
			folder.open(Folder.READ_ONLY);
			System.out.println("Message Count Found " + folder.getMessageCount());
			System.out.println("New Message Count " + folder.getNewMessageCount());
			System.out.println("=========================================");
			Message[] newmessages = folder.getMessages();
			FetchProfile fp = new FetchProfile();
			fp.add(FetchProfile.Item.ENVELOPE);
			folder.fetch(newmessages, fp);
			return newmessages;
		} catch (MessagingException ex) {
			MessageDialog.openInformation(
					window.getShell(),
					"Mail client Error",
					"Reading the messages INBOX failed -" + ex);
			throw new RuntimeException(ex);
		}
	}

	public void transform(DatastoreManager datastoreManager, Message[] messages, List<IMailReader> importers) {

		for (int i = 47; i < messages.length; i++) {
			try {
				Address[] addressList = messages[i].getFrom();
				for (int j = 0; j < addressList.length; j++) {
					System.out.println("From address #" + j + " : " + addressList[j].toString());
				}

				Set<String> addressesAsString = new HashSet<String>();
				for (Address address : addressList) {
					String x = address.getType();
					addressesAsString.add(address.toString());
				}
				
				
				System.out.println("Receive date :" + messages[i].getSentDate());

				/*
				 * Create a transaction to be used to import the mail message.  This allows the data to
				 * be more efficiently written to the back-end datastore and it also groups
				 * the entire import as a single change for undo/redo purposes.
				 */
				TransactionManager transactionManager = new TransactionManager(datastoreManager);
				net.sf.jmoney.model2.Session sessionInTransaction = transactionManager.getSession();
				
				boolean anyProcessed = false;
				for (IMailReader reader : importers) {
					
					if (reader.mayProcessEmail(addressesAsString)) {
						StringBuffer content = new StringBuffer();
						
						System.out.println(messages[i].getContentType());
						if (messages[i].isMimeType("text/plain")) {
							content.append(messages[i].getContent().toString());
						} else if (messages[i].isMimeType("text/html")) {
							content.append(messages[i].getContent().toString());
						} else {
							handleMultipart(messages[i], content);
						}

						
						if (content.length() != 0) {
							boolean processed = reader.processEmail(sessionInTransaction, messages[i].getSentDate(), content.toString());
							anyProcessed |= processed;
						}
					}
				}

				/*
				 * We don't know whether any extensions have actually done anything, so
				 * we commit the transaction anyway.  This will be a null operation if
				 * no changes were made.  Note the the 'anyProcessed' flag cannot be tested
				 * because this flag is used to indicate the the mail is 'done' and should
				 * be deleted.  An extension may make some changes to the datastore but not
				 * think the message should be deleted. 
				 */
				String transactionDescription = MessageFormat.format("Import Mail {0}", messages[i].getSubject());
				transactionManager.commit(transactionDescription);									

				if (anyProcessed) {
					//This will delete the mail from the inbox after you close the folder
					messages[i].setFlag(Flags.Flag.DELETED, true);
				}
			} catch (MessagingException ex) {
				System.out.println("Messages transformation failed :" + ex);
			} catch (IOException ex) {
				System.out.println("Messages I/O transformation failed : " + ex);
			}
		}
	}

	public void handleMultipart(Message msg, StringBuffer content) {
		try {
			String disposition;
			BodyPart part;
			Multipart mp = (Multipart) msg.getContent();

			int mpCount = mp.getCount();
			for (int m = 0; m < mpCount; m++) {
				part = mp.getBodyPart(m);

				disposition = part.getDisposition();
				Object x = part.getContent();
				Object y = part.getContentType();
				if (!(x instanceof String)) {
					System.out.println("here");
				}
				if (x instanceof BASE64DecoderStream) {
					BASE64DecoderStream stream = (BASE64DecoderStream)x;
				} else if (x instanceof MimeMessage) {
					// content type may be "message/rfc822"
					MimeMessage stream = (MimeMessage)x;
					Address a = stream.getSender();
					Object z = stream.getContent();
					appendContent(z, content);
				} else if (x instanceof MimeMultipart) {
					MimeMultipart stream = (MimeMultipart)x;
					System.out.println(stream.getContentType() + stream.getCount());
					for (int i = 0; i < stream.getCount(); i++) {
						BodyPart p = stream.getBodyPart(i);
						Object z = p.getContent();
						System.out.println(z);
					}
				} else {
				if (disposition != null && disposition.equals(Part.INLINE)) {
					content.append(part.getContent());
				} else {
					content.append(part.getContent());
				}
				}
			}
		} catch (IOException ex) {
			System.out.println("Messages - Parts - Input/output transformation failed :" + ex);
		} catch (MessagingException ex) {
			System.out.println("Messages - Parts - transformation failed :" + ex);
		}
	}

	private void appendContent(Object z, StringBuffer content) throws IOException, MessagingException {
		if (z instanceof MimeMultipart) {
			MimeMultipart stream2 = (MimeMultipart)z;
			System.out.println(stream2.getContentType() + stream2.getCount());
			for (int i = 0; i < stream2.getCount(); i++) {
				BodyPart p = stream2.getBodyPart(i);
				Object z2 = p.getContent();
				content.append(z2);
			}
		}
		System.out.println("");
	}

	static class Mailbox {
		String address;
		String host;
		String password;
		List<IMailReader> importers = new ArrayList<IMailReader>();

		public Mailbox(String host, String address, String password) {
			this.host = host;
			this.address = address;
			this.password = password;
		}

		public Collection<IMailReader> getImporters() {
			return importers;
		}

		public void addImporter(IMailReader importer) {
			importers.add(importer);
		}
	}
}
