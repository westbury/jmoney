package net.sf.jmoney.email.handlers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
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
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import net.sf.jmoney.email.Activator;
import net.sf.jmoney.email.IContentReader;
import net.sf.jmoney.email.IMailReader;
import net.sf.jmoney.email.ITextProcessor;
import net.sf.jmoney.email.InstallCert;
import net.sf.jmoney.email.UnexpectedContentException;
import net.sf.jmoney.model2.IDatastoreManager;
import net.sf.jmoney.model2.TransactionManagerForAccounts;
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

import com.sun.mail.util.BASE64DecoderStream;

/**
 * Reads mail, looking for e-mail messages that have information that
 * can be imported into JMoney.
 */
public class EmailImportHandler extends AbstractHandler {

	private static final String MAILBOXES_KEY = "mailboxes";

	private static final String MAIL_IMPORTERS_KEY = "mailimporters";

	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = HandlerUtil.getActiveShellChecked(event);
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		IDatastoreManager sessionManager = (IDatastoreManager)window.getActivePage().getInput();
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
						if (elements[j].getName().equals("messagetype")) {
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
							if (!address.isEmpty()) {
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

						int port = 993;

						Properties properties = new Properties();

						// set this session up to use SSL for IMAP connections
						properties.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
						// don't fallback to normal IMAP connections on failure.
						properties.setProperty("mail.imap.socketFactory.fallback", "false");
						// use the simap port for imap/ssl connections.
						properties.setProperty("mail.imap.socketFactory.port", "993");

						String passphrase = "changeit";
						try {
							File file = InstallCert.getKeyStoreFile();

							KeyStore ks = InstallCert.openKeyStore(file);

							X509Certificate[] chain = InstallCert.fetchCerts(host, port, ks);

							System.out.println();
							System.out.println("Server sent " + chain.length + " certificate(s):");
							System.out.println();
							MessageDigest sha1 = MessageDigest.getInstance("SHA1");
							MessageDigest md5 = MessageDigest.getInstance("MD5");
							for (int i = 0; i < chain.length; i++) {
								X509Certificate cert = chain[i];
								System.out.println
								(" " + (i + 1) + " Subject " + cert.getSubjectDN());
								System.out.println("   Issuer  " + cert.getIssuerDN());
								sha1.update(cert.getEncoded());
								System.out.println("   sha1    " + InstallCert.toHexString(sha1.digest()));
								md5.update(cert.getEncoded());
								System.out.println("   md5     " + InstallCert.toHexString(md5.digest()));
								System.out.println();
							}

							System.out.println("Enter certificate to add to trusted keystore or 'q' to quit: [1]");
							int k = 0;
							InstallCert.saveCert(host, passphrase.toCharArray(), file, ks, chain, k);
						} catch (FileNotFoundException e) {
							// "(.*) Access is denied"
							// if (e.getMessage() )
							// Turn off Windows Defender 
							e.printStackTrace();
						} catch (KeyManagementException
								| NoSuchAlgorithmException | KeyStoreException
								| IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} catch (CertificateException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						properties.setProperty("mail.debug", "com.sun.mail.imap.IMAPSSLStore");
						properties.setProperty("mail.imaps.class", "com.sun.mail.imap.IMAPSSLStore");
						properties.setProperty("mail.store.protocol", "imaps");
						properties.setProperty("mail.imap.starttls.enable", "true");
						try {
							Session session = Session.getInstance(properties,new Authenticator() {
								@Override
								protected PasswordAuthentication getPasswordAuthentication() {
									return new PasswordAuthentication(username, password);
								}
							});

							javax.mail.Store store = session.getStore("imaps");
							store.connect(host, username, password);
							javax.mail.Folder[] folders = store.getDefaultFolder().list("*");

							Folder myFolder = null;

							for (javax.mail.Folder folder : folders) {
								if ((folder.getType() & javax.mail.Folder.HOLDS_MESSAGES) != 0) {
									System.out.println(folder.getFullName() + " has " + folder.getMessageCount() + " messages");
									if (folder.getFullName().equals("INBOX")) {
										myFolder = folder;
									}
								}
							}

							Message[] messages  = readAllMessages(window, store, myFolder, host);
							transform(sessionManager, messages, mailbox.importers);
						} catch (MessagingException e) {
							e.printStackTrace(System.out);
						}
					}
				}
			} catch (CoreException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}

		return null;
	}

	public Message[] readAllMessages(IWorkbenchWindow window, Store store, Folder folder, String host) {

		try {
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

	public void transform(IDatastoreManager datastoreManager, Message[] messages, List<IMailReader> importers) {

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
				TransactionManagerForAccounts transactionManager = new TransactionManagerForAccounts(datastoreManager);
				net.sf.jmoney.model2.Session sessionInTransaction = transactionManager.getSession();

				boolean anyProcessed = false;
				for (IMailReader reader : importers) {

					if (reader.mayProcessMessage(addressesAsString)) {
						System.out.println(messages[i].getContentType());
						try {
							if (messages[i].isMimeType("text/plain")) {
								anyProcessed |= reader.processPlainTextMessage(sessionInTransaction, messages[i].getSentDate(), messages[i].getContent().toString());
							} else if (messages[i].isMimeType("text/html")) {
								anyProcessed |= reader.processHtmlMessage(sessionInTransaction, messages[i].getSentDate(), messages[i].getContent().toString());
							} else {
								Multipart mp = (Multipart) messages[i].getContent();
								IContentReader contentReader = new PartReader(mp);
								anyProcessed |= reader.processMimeMultipartMessage(sessionInTransaction, messages[i].getSentDate(), contentReader);
							}
						} catch (UnexpectedContentException e) {
							// Do we show this as an error to the user or do we silently ignore this,
							// without deleting the message?
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

	private final class PartReader implements IContentReader {

		Multipart mp;

		int m = 0;

		public PartReader(Multipart mp) {
			this.mp = mp;
		}

		@Override
		public void expectPlainText(ITextProcessor textProcessor) throws UnexpectedContentException {
			try {
				if (m >= mp.getCount()) {
					throw new UnexpectedContentException("Plain text part expected at index " + m + " but only " + mp.getCount() + " parts found.");
				}

				BodyPart part = mp.getBodyPart(m);

				if (!part.getContentType().equals("text/plain; charset=utf-8")) {
					throw new UnexpectedContentException("Plain text part expected at index " + m + " but " + part.getContentType() + " found.");
				}

				textProcessor.processText((String)part.getContent());

				m++;
			} catch (MessagingException | IOException e) {
				throw new UnexpectedContentException(e);
			}
		}

		@Override
		public void expectHtml(ITextProcessor textProcessor) throws UnexpectedContentException {
			try {
				if (m >= mp.getCount()) {
					throw new UnexpectedContentException("HTML part expected at index " + m + " but only " + mp.getCount() + " parts found.");
				}

				BodyPart part = mp.getBodyPart(m);

				if (!part.getContentType().equals("text/html; charset=utf-8")) {
					throw new UnexpectedContentException("HTML part expected at index " + m + " but  " + part.getContentType() + " found.");
				}

				if (textProcessor != null) {
					textProcessor.processText((String)part.getContent());
				}
				m++;
			} catch (MessagingException | IOException e) {
				throw new UnexpectedContentException(e);
			}
		}

		@Override
		public void expectBase64() throws UnexpectedContentException {
			try {
				if (m >= mp.getCount()) {
					throw new UnexpectedContentException("Base 64 part expected at index " + m + " but only " + mp.getCount() + " parts found.");
				}

				BodyPart part = mp.getBodyPart(m);

				if (!(part.getContent() instanceof BASE64DecoderStream)) {
					throw new UnexpectedContentException("HTML part expected at index " + m + " but  " + part.getContentType() + " found.");
				}

				// Base64 encoded data is  a text encoding of binary data and is always ignored.

				m++;
			} catch (MessagingException | IOException e) {
				throw new UnexpectedContentException(e);
			}
		}

		@Override
		public void expectMimeMessage(ITextProcessor textProcessor) throws UnexpectedContentException {
			try {
				if (m >= mp.getCount()) {
					throw new UnexpectedContentException("MIME message part expected at index " + m + " but only " + mp.getCount() + " parts found.");
				}

				BodyPart part = mp.getBodyPart(m);



				if (!part.getContentType().equals("message/rfc822")) {
					throw new UnexpectedContentException("MIME Message part expected at index " + m + " but  " + part.getContentType() + " found.");
				}

				MimeMessage mimeMessage = (MimeMessage)part.getContent();
				// TODO pass other data such as mimeMessage.getSender() ???
				textProcessor.processText((String)mimeMessage.getContent());

				m++;
			} catch (MessagingException | IOException e) {
				throw new UnexpectedContentException(e);
			}
		}

		@Override
		public void expectMimeMultipart(IMultipartProcessor multipartProcessor) throws UnexpectedContentException {
			try {
				if (m >= mp.getCount()) {
					throw new UnexpectedContentException("MIME multi-part expected at index " + m + " but only " + mp.getCount() + " parts found.");
				}

				BodyPart part = mp.getBodyPart(m);

				System.out.println(part.getContentType());
				if (!(part.getContent() instanceof MimeMultipart)) {
					throw new UnexpectedContentException("MIME multi-part expected at index " + m + " but  " + part.getContentType() + " found.");
				}

				MimeMultipart stream2 = (MimeMultipart)part.getContent();
				IContentReader contentReader = new PartReader(stream2);

				multipartProcessor.processParts(contentReader);

				m++;
			} catch (MessagingException | IOException e) {
				throw new UnexpectedContentException(e);
			}
		}
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
