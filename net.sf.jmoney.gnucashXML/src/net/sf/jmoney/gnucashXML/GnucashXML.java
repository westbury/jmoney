/*
 * Created on 10 juil. 2004
 */

package net.sf.jmoney.gnucashXML;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.isolation.ReferenceViolationException;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.BankAccount;
import net.sf.jmoney.model2.BankAccountInfo;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.IncomeExpenseAccountInfo;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;
import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLDocument;
import oracle.xml.parser.v2.XMLElement;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IWorkbenchWindow;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Singleton-Class to implement the import/export of GnuCash files.
 * 
 * @author Faucheux
 */
public final class GnucashXML implements FileFormat, IRunnableWithProgress {
	private NumberFormat number = NumberFormat.getInstance(Locale.US);

	private IWorkbenchWindow window;

	private DateFormat gnucashDateFormat = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss Z");

	private Hashtable<String, Account> accountsGUIDTable;

	private Hashtable<String, Integer> accountsTypeTable;

	private Session session;

	private File file;

	private Document doc;

	private Element bookElement;

	static private GnucashXML myInstance = null;

	/**
	 * Create a new GnucashXML Class
	 */
	private GnucashXML(IWorkbenchWindow window) {
		this.window = window;
		number.setMinimumFractionDigits(2);
		number.setMaximumFractionDigits(2);
	}

	static public GnucashXML getSingleton(IWorkbenchWindow window) {
		if (myInstance == null)
			myInstance = new GnucashXML(window);
		return myInstance;
	}

	/**
	 * FileFiter which the OpenBox offers to fiter the Gnucash Files
	 */
	public FileFilter fileFilter() {
		return null;
	}

	public void importFile(Session session, File file) {
		ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(window
				.getShell());
		this.session = session;
		this.file = file;
		try {
			progressDialog.run(true, false, this);
		} catch (InvocationTargetException e) {
			JMoneyPlugin.log(e);
		} catch (InterruptedException e) {
			JMoneyPlugin.log(e);
		}
	}

	/**
	 * import of the GnuCash File The XML File has following structure: <?xml
	 * version="1.0"?> <gnc-v2> <count-data type="account">63</count-data>
	 * <count-data type="transaction">1660</count-data> <account
	 * version="2.0.0"> <!-- Cf accounts --> </account> <account
	 * version="2.0.0"> </account> <transaction version="2.0.0"> <!-- Cf
	 * transaction --> </transaction> <transaction version="2.0.0"> <!-- Cf
	 * transaction --> </transaction> </gnc-v2>
	 */
	public void run(IProgressMonitor monitor) {
		DOMParser parser = new DOMParser();
		accountsGUIDTable = new Hashtable<String, Account>(); // hash
		accountsTypeTable = new Hashtable<String, Integer>(); // hash
		// between
		// GUID
		// of
		// the
		// accounts (hash) and their
		// names (value).

		try {

			// Set various parser options; validation off,
			// warnings shown, error stream set to stderr.
			parser.setErrorStream(System.err);
			parser.setValidationMode(DOMParser.NONVALIDATING);
			parser.showWarnings(true);

			// set the DTD
			monitor.beginTask("Reading the file...", 0);
			URL urlDTD = this.getClass().getResource("resources/gnucash.dtd");
			parser.parseDTD(urlDTD, "gnc-v2");
			parser.setDoctype(parser.getDoctype());

			// parse the document
			monitor.beginTask("Parsing the document...", 1);
			URI uri = file.toURI();
			URL url = uri.toURL();

			parser.parse(url);

			// Obtain the document
			XMLDocument doc = parser.getDocument();

			// Create the accounts
			monitor.beginTask("Creating the accounts...", 2);
			createAccounts(doc);

			// Create the transactions
			monitor.beginTask("Importing the transactions...", 3);
			createTransactions(doc);

			// Commit the changes to the datastore
			/*
			 * TODO: decide if we are going to support import as an undoable
			 * operation. This old mechanism is no longer supported, and
			 * building a full undo history could be expensive in this case.
			 * session.registerUndoableChange(GnucashXMLPlugin
			 * .getResourceString("importDescription"));
			 */
		} catch (MalformedURLException e) {
			JMoneyPlugin.log(e);
		} catch (IOException e) {
			JMoneyPlugin.log(e);
		} catch (SAXException e) {
			JMoneyPlugin.log(e);
		} catch (LessThanTwoSplitsException e) {
			JMoneyPlugin.log(e);
		} catch (MoreThanTwoSplitsException e) {
			JMoneyPlugin.log(e);
		} catch (ParseException e) {
			JMoneyPlugin.log(e);
		} finally {
			monitor.done();
		}
	}

	/**
	 * Create the accounts. Each XML-Account has following structure: <account
	 * version="2.0.0"> <name>Sorties culturelles</name> <id
	 * type="guid">3ce87b30bc7b5fc69b7ccba0ddab4d72</id> <type>EXPENSE</type>
	 * <currency> <space>ISO4217</space> <id>EUR</id> </currency>
	 * <currency-scu>100</currency-scu> <parent
	 * type="guid">210104bdd4a8a79fd297ea233e1966c9</parent> </account>
	 * 
	 * @param doc =
	 *            handle to the file to read it has to have the form: <?xml
	 *            version="1.0"?> <gnc-v2> <something></something> <account
	 *            version="2.0.0"> <!-- Cf accounts --> </account> <account
	 *            version="2.0.0"> <!-- Cf accounts --> </account> <something></something>
	 *            </gnc-v2>
	 * @author Olivier Faucheux TODO: Faucheux - treats the type (EXPENSE, BANK,
	 *         CASH, CURRENCY, ...) - treats the currency (for the time, the
	 *         standard currency is always used) - when (or if ever) jmoney
	 *         accepts it, treats the parent.
	 */
	private void createAccounts(XMLDocument doc) {
		Node node; // First child of <gnc-v2>
		final Hashtable<String, String> childParent = new Hashtable<String, String>();
		List<String> accountToRecreate = new LinkedList<String>();
		String rootGUID = null;
		final int BANKACCTTYPE = 0;
		final int CATEGORYTYPE = 1;

		// For each account of the file
		for (node = doc.getFirstChild().getNextSibling().getFirstChild()
				.getNextSibling().getFirstChild(); node != null; node = (Element) node
				.getNextSibling()) {
			if (GnucashXMLPlugin.DEBUG)
				System.out.println("Node: " + node.getNodeName());
			if (node.getNodeName().compareToIgnoreCase("gnc:Account") == 0) {
				String accountName = null;
				String accountGUID = null;
				String parentGUID = null;
				String gnucashType = null;
				int accountType = BANKACCTTYPE; // 0 = Account type; 1 =
												// Category type

				NodeList childNodes = node.getChildNodes();
				int thecount = childNodes.getLength();
				for (int j = 0; j < thecount; j++) {
					// if (GnucashXMLPlugin.DEBUG) System.out.println(" Subnode
					// : " + childNodes.item(j).getNodeName() + ": " +
					// childNodes.item(j).getFirstChild().getNodeValue());
					if (childNodes.item(j).getNodeName().equalsIgnoreCase(
							"act:name")) {
						accountName = childNodes.item(j).getFirstChild()
								.getNodeValue();
					} else if (childNodes.item(j).getNodeName()
							.equalsIgnoreCase("act:id")) {
						accountGUID = childNodes.item(j).getFirstChild()
								.getNodeValue();

					} else if (childNodes.item(j).getNodeName()
							.equalsIgnoreCase("act:parent")) {
						parentGUID = childNodes.item(j).getFirstChild()
								.getNodeValue();
					} else if (childNodes.item(j).getNodeName()
							.equalsIgnoreCase("act:type")) {
						gnucashType = childNodes.item(j).getFirstChild()
								.getNodeValue();
						if (gnucashType.equalsIgnoreCase("INCOME")
								|| gnucashType.equalsIgnoreCase("EXPENSE"))
							accountType = CATEGORYTYPE;
						else
							accountType = BANKACCTTYPE;
					}
					if (accountName.equals("Root Account") && rootGUID == null) {
						rootGUID = accountGUID;
					}
				}

				// Create the account
				if (accountName != null) {
					if (!accountName.equalsIgnoreCase("Root Account")) {
						if (GnucashXMLPlugin.DEBUG) {
							System.out.println("I'm creating the account >"
									+ accountName + "< with guid >"
									+ accountGUID + "<");
						}
						if (accountType == BANKACCTTYPE) {
							ExtendablePropertySet<BankAccount> propset = BankAccountInfo
									.getPropertySet();
							CapitalAccount account = session
									.createAccount(propset);
							account.setName(accountName);
							accountsGUIDTable.put(accountGUID, account);
							accountsTypeTable.put(accountGUID, BANKACCTTYPE);
						} else {
							ExtendablePropertySet<IncomeExpenseAccount> propset = IncomeExpenseAccountInfo
									.getPropertySet();
							IncomeExpenseAccount account = session
									.createAccount(propset);
							account.setName(accountName);
							account.setCurrency(session.getDefaultCurrency());
							accountsGUIDTable.put(accountGUID, account);
							accountsTypeTable.put(accountGUID, CATEGORYTYPE);
						}
						if (parentGUID != null
								&& !parentGUID.equalsIgnoreCase(rootGUID)) {
							childParent.put(accountGUID, parentGUID);
							accountToRecreate.add(accountGUID);
						}
					}
				} else {
					JMoneyPlugin
							.log(new RuntimeException(
									"Error while importing: Account without any name found!"));
				}
			}

		}

		// Now link childs and parents. We have to recreate the parents before
		// the children.
		// Therefore, first sort the list
		Collections.sort(accountToRecreate, new Comparator<String>() {
			public int compare(String GUIDA, String GUIDB) {
				String GUIDParentOfA = childParent.get(GUIDA);
				String GUIDParentOfB = childParent.get(GUIDB);

				// case we compare two root-accounts
				if (GUIDParentOfA == null & GUIDParentOfB == null)
					return 0;

				// case A is root account
				if (GUIDParentOfA == null)
					return compare(GUIDA, GUIDParentOfB);

				// case B is root account
				if (GUIDParentOfB == null)
					return compare(GUIDParentOfA, GUIDB);

				// case neither A nor B are root accounts
				if (GUIDA.equals(GUIDParentOfB))
					return -1;
				if (GUIDB.equals(GUIDParentOfA))
					return 1;

				return compare(GUIDParentOfA, GUIDB);
			}
		});

		// Now recreate some accounts.
		for (String childGUID : accountToRecreate) {
			String parentGUID = childParent.get(childGUID);
			Account child = getAccountFromGUID(childGUID);
			Integer accountType = getAccountTypeFromGUID(childGUID);
			if (accountType == BANKACCTTYPE) {
				CapitalAccount parent = (CapitalAccount) getAccountFromGUID(parentGUID);

				try {
					session.deleteAccount(child);
				} catch (ReferenceViolationException e) {
					/*
					 * The account can't be deleted because there are references to it.
					 * This probably can never happen.
					 */
					throw new RuntimeException("Internal Error", e);
				}
				CapitalAccount newChild = parent
						.createSubAccount(BankAccountInfo.getPropertySet());
				accountsGUIDTable.remove(childGUID);
				accountsGUIDTable.put(childGUID, newChild);
				accountsTypeTable.remove(childGUID);
				accountsTypeTable.put(childGUID, BANKACCTTYPE);

				newChild.setName(child.getName());
			} else {
				IncomeExpenseAccount parent = (IncomeExpenseAccount) getAccountFromGUID(parentGUID);

				try {
					session.deleteAccount(child);
				} catch (ReferenceViolationException e) {
					/*
					 * The account can't be deleted because there are references to it.
					 * This probably can never happen.
					 */
					throw new RuntimeException("Internal Error", e);
				}
				IncomeExpenseAccount newChild = parent.createSubAccount();
				newChild.setCurrency(session.getDefaultCurrency());
				accountsGUIDTable.remove(childGUID);
				accountsGUIDTable.put(childGUID, newChild);
				accountsTypeTable.remove(childGUID);
				accountsTypeTable.put(childGUID, CATEGORYTYPE);

				newChild.setName(child.getName());
			}
		}
	}

	/**
	 * Add all the transactions of the XML-File. A transaction looks as
	 * following: <transaction version="2.0.0"> <id
	 * type="guid">66e591ba1b00dab33628d58390973e33</id> <date-posted>
	 * <date>2003-10-31 000000 +0000</date> </date-posted> <date-entered>
	 * <date>2003-11-03 070741 +0000</date> </date-entered>
	 * <description>Geldkarte</description> <splits> <split> <id
	 * type="guid">73fd69691319ea2872565aad65e26cde</id> <reconciled-state>n</reconciled-state>
	 * <value>-2600/100</value> <quantity>-2600/100</quantity> <account
	 * type="guid">c192cbb8d5980c690c0d44c188fede4b</account> </split> <split>
	 * <id type="guid">396f463aaea2482a4c80da8b1eb2bcfa</id>
	 * <reconciled-state>n</reconciled-state> <value>2600/100</value>
	 * <quantity>2600/100</quantity> <account
	 * type="guid">00a629b2ed01633286b2c9782a17757c</account> </split>
	 * </splits> </transaction> TODO Faucheux: - can we store the "date-entered"
	 * in jmoney too? - when we have two "splits", it's a simple double Entry.
	 * When more, it's a splitted one. For the time, only "simple double"
	 * Entries works.
	 * 
	 * @param doc
	 * @throws MoreThanTwoSplitsException
	 * @throws LessThanTwoSplitsException
	 * @throws ParseException
	 */
	private void createTransactions(XMLDocument doc)
			throws MoreThanTwoSplitsException, LessThanTwoSplitsException,
			ParseException {

		Node transactionElement; /* Currently treated Transaction node */

		// For each Transaction of the XML file

		for (transactionElement = doc.getFirstChild().getNextSibling()
				.getFirstChild().getNextSibling().getFirstChild(); transactionElement != null; transactionElement = (Element) transactionElement
				.getNextSibling()) {

			if (GnucashXMLPlugin.DEBUG)
				System.out.println("Node: " + transactionElement.getNodeName());

			if (transactionElement.getNodeName().equalsIgnoreCase(
					"gnc:transaction")) {

				// if (GnucashXMLPlugin.DEBUG) System.out.println("New
				// Transaction");
				treatTransaction(transactionElement);

			}
		}
	}

	private long getLong(String s) {
		int posDivision = s.indexOf("/");
		long l1 = Long.parseLong(s.substring(0, posDivision));
		// long l2 = Long.parseLong(s.substring(posDivision + 1));

		// TODO: Faucheux - understand why return (l1/l2) is not the good one;
		return l1;

	}

	/**
	 * Add a simple transaction. Simple transaction means here "double" one, but
	 * not splitted
	 * 
	 * @param transactionElement
	 * @throws ParseException
	 * @author Olivier Faucheux
	 * 
	 * private void treatSimpleTransaction(Element propertyElement, Transaction
	 * t) throws ParseException {
	 * 
	 * String firstAccountName = null; String firstAccountGUID = null; String
	 * secondAccountName = null; String secondAccountGUID = null; String
	 * description = null; XMLElement transactionNode;
	 * 
	 * transactionNode = (XMLElement) propertyElement.getParentNode();
	 * 
	 * try { description = transactionNode.getElementsByTagName("description")
	 * .item(0).getFirstChild().getNodeValue(); } catch (NullPointerException e) { //
	 * No description }
	 * 
	 * Element firstAccoutElement = (Element) propertyElement
	 * .getElementsByTagName("split").item(0); firstAccountGUID =
	 * firstAccoutElement.getElementsByTagName("account")
	 * .item(0).getFirstChild().getNodeValue(); Account firstAccount =
	 * getAccountFromGUID(firstAccountGUID); Element secondAccoutElement =
	 * (Element) propertyElement .getElementsByTagName("split").item(1);
	 * secondAccountGUID = secondAccoutElement.getElementsByTagName("account")
	 * .item(0).getFirstChild().getNodeValue(); Account secondAccount =
	 * getAccountFromGUID(secondAccountGUID);
	 * 
	 * String Value = firstAccoutElement.getElementsByTagName("value").item(0)
	 * .getFirstChild().getNodeValue();
	 * 
	 * Entry e1 = t.createEntry(); e1.setAmount(-getLong(Value));
	 * e1.setAccount(secondAccount); e1.setDescription(description);
	 * 
	 * Entry e2 = t.createEntry(); e2.setAmount(getLong(Value));
	 * e2.setAccount(firstAccount); e2.setDescription(description); //
	 * t.addEntry(e1); // t.addEntry(e2); // TODO: Faucheux to check }
	 */

	private void treatTransaction(Node transactionElement)
			throws ParseException {
		String CheckNum = "";

		Transaction t = session.createTransaction();

		// For each property of the node
		for (Element propertyElement = (Element) transactionElement
				.getFirstChild(); propertyElement != null; propertyElement = (Element) propertyElement
				.getNextSibling()) {
			String propertyElementName = propertyElement.getNodeName();
			// if (GnucashXMLPlugin.DEBUG) System.out.println("New property : >"
			// + propertyElementName + "<" + " Value >" + propertyElementValue +
			// "<");

			if (propertyElementName.equalsIgnoreCase("trn:num")) {
				CheckNum = propertyElement.getFirstChild().getNodeValue();
			}

			if (propertyElementName.equalsIgnoreCase("trn:date-posted")) {
				t.setDate(gnucashDateFormat.parse(propertyElement
						.getFirstChild().getFirstChild().getNodeValue()));
			} else if (propertyElementName.equalsIgnoreCase("trn:splits")) {

				if (propertyElement.getElementsByTagName("split").getLength() < 2) {
					// TODO Faucheux
				} else {
					treatSplittedTransaction(propertyElement, t, CheckNum);
				}

			}

		} // Treatement of properties

	}

	public void exportAccount(Session session, CapitalAccount account, File file) {
		throw new RuntimeException(
				"exportAccount for GnucashXML not implemented !");
	};

	private Account getAccountFromGUID(String GUID) {
		// if (GnucashXMLPlugin.DEBUG) System.out.println("Looking for an
		// account with the GUID " + GUID);
		return accountsGUIDTable.get(GUID);
	}

	private Integer getAccountTypeFromGUID(String GUID) {
		// if (GnucashXMLPlugin.DEBUG) System.out.println("Looking for the
		// account type with the GUID " + GUID);
		return accountsTypeTable.get(GUID);
	}

	/**
	 * Add a splitted transaction. A splitted transaction is a transaction with
	 * more than two entries
	 * 
	 * @param transactionElement
	 * @throws ParseException
	 * @author Olivier Faucheux
	 */
	private void treatSplittedTransaction(Element propertyElement,
			Transaction t, String CheckNum) throws ParseException {
		String accountGUID = null;
		String transactionDescription = null;
		XMLElement transactionNode;
		NodeList entriesNodes = null;

		transactionNode = (XMLElement) propertyElement.getParentNode();

		try {
			transactionDescription = transactionNode.getElementsByTagName(
					"description").item(0).getFirstChild().getNodeValue();
		} catch (NullPointerException e) { /* No description */
		}

		entriesNodes = propertyElement.getElementsByTagName("split");
		for (int i = 0; i < entriesNodes.getLength(); i++) {

			Element accountElement = (Element) entriesNodes.item(i);
			accountGUID = accountElement.getElementsByTagName("account")
					.item(0).getFirstChild().getNodeValue();

			String value = accountElement.getElementsByTagName("value").item(0)
					.getFirstChild().getNodeValue();

			if (getLong(value) != 0) {
				// Yes, I found entries with an amount = 0 and as accountGUID
				// "0000000000000000000"
				// I have to protect against it -- Faucheux
				Account account = getAccountFromGUID(accountGUID);
				Entry e = t.createEntry();
				e.setAmount(getLong(value));
				e.setAccount(account);
				e.setMemo(transactionDescription);
				if (!CheckNum.isEmpty())
					e.setCheck(CheckNum);

				// if (GnucashXMLPlugin.DEBUG) System.out.println("Added amount:
				// " + getLong(value) + " for " + account.toString() + " for >"
				// + transactionDescription + "<" );
			}

		}

		// TODO: Faucheux to check
	}

	/**
	 * export of the GnuCash File For the structure of the XML-File,
	 * 
	 * @see GnucashXML
	 */
	public void export(Session session, String toFile) {

		// Prepare the tools
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder parser = null;
		DOMImplementation impl = null;
		try {
			parser = factory.newDocumentBuilder();
			impl = parser.getDOMImplementation();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}

		// Create the document
		this.session = session;
		Element documentRoot;
		doc = impl.createDocument(null, "gnc-v2", null);
		documentRoot = doc.getDocumentElement();

		// Create the header
		Element e1 = doc.createElement("gnc:count-data");
		e1.setAttribute("cd:type", "book");
		e1.appendChild(doc.createTextNode("1"));
		documentRoot.appendChild(e1);

		bookElement = doc.createElement("gnc:book");
		bookElement.setAttribute("version", "2.0.0");
		documentRoot.appendChild(bookElement);

		e1 = doc.createElement("book:id");
		e1.setAttribute("type", "guid");
		e1.appendChild(doc.createTextNode(e1.getNodeValue()));
		bookElement.appendChild(e1);

		e1 = doc.createElement("book:slots");
		bookElement.appendChild(e1);

		// Resume the content
		Integer numberAccounts = new Integer(session.getAllAccounts().size());
		Integer numberTransaction = new Integer(session
				.getTransactionCollection().size());
		Element element;
		// Number of accounts
		element = doc.createElement("gnc:count-data");
		element.setAttribute("type", "account");
		element.appendChild(doc.createTextNode(numberAccounts.toString()));
		bookElement.appendChild(element);
		// Number of transactions
		element = doc.createElement("gnc:count-data");
		element.setAttribute("type", "transaction");
		element.appendChild(doc.createTextNode(numberTransaction.toString()));
		bookElement.appendChild(element);

		// add each account
		for (Account account : session.getAllAccounts()) {
			exportAccount(account);
		}

		// add each transaction
		for (Transaction transaction : session.getTransactionCollection()) {
			exportTransaction(transaction);
		}

		// Prepare the output of the result
		Transformer transformer = null;
		try {
			transformer = (TransformerFactory.newInstance()).newTransformer();
		} catch (Throwable t) {
			t.printStackTrace();
		}

		//
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		// TODO org.apache.xalan.templates.OutputProperties not available in
		// Java 1.5
		// transformer.setOutputProperty(OutputPropertiesFactory.S_KEY_INDENT_AMOUNT,
		// "2");

		Source input = new DOMSource(doc);
		Result output = new StreamResult(new File(toFile));
		try {
			transformer.transform(input, output);
		} catch (TransformerException e) {
			e.printStackTrace();
		}

	}

	/*
	 * The XML File has following structur: <?xml version="1.0"?> <gnc-v2>
	 * <count-data type="account">63</count-data> <count-data
	 * type="transaction">1660</count-data> <account version="2.0.0"> <!-- Cf
	 * accounts --> </account> <account version="2.0.0"> </account> <transaction
	 * version="2.0.0"> <!-- Cf transaction --> </transaction> <transaction
	 * version="2.0.0"> <!-- Cf transaction --> </transaction> </gnc-v2>
	 */

	/**
	 * @author Faucheux
	 */
	private void exportAccount(Account account) {
		if (GnucashXMLPlugin.DEBUG)
			System.out
					.println("Export account " + account.getFullAccountName());

		// create the entry for the account
		Element e = doc.createElement("gnc:account");
		e.setAttribute("version", "2.0.0");

		// give the information of the account
		Element e2, e3;

		e2 = doc.createElement("act:name");
		e2.appendChild(doc.createTextNode(account.getName()));
		e.appendChild(e2);

		String guid = Integer.toHexString(account.hashCode());
		e2 = doc.createElement("act:id");
		e2.setAttribute("type", "guid");
		e2.appendChild(doc.createTextNode(guid));
		e.appendChild(e2);

		e2 = doc.createElement("act:type");
		e2.appendChild(doc.createTextNode("EXPENSE")); // TODO Faucheux -
		// Change this
		e.appendChild(e2);

		e2 = doc.createElement("act:commodity");
		e3 = doc.createElement("cmdty:space");
		e3.appendChild(doc.createTextNode("ISO4217"));
		e2.appendChild(e3);
		e3 = doc.createElement("cmdty:id");
		e3.appendChild(doc.createTextNode("EUR")); // TODO Faucheux - Change
		// this
		e2.appendChild(e3);
		e.appendChild(e2);

		if (account.getParent() != null) {
			e2 = doc.createElement("act:parent");
			e2.setAttribute("type", "guid");
			e2.appendChild(doc.createTextNode(Integer.toHexString(account
					.getParent().hashCode())));
			e.appendChild(e2);
		}

		// add the account to the XML file
		bookElement.appendChild(e);
	}

	/**
	 * Add the transaction to the document
	 * 
	 * @author Faucheux
	 */
	@SuppressWarnings("unchecked")
	private void exportTransaction(Transaction transaction) {
		if (GnucashXMLPlugin.DEBUG)
			System.out.println("Export transaction " + transaction.hashCode());

		// create the entry for the transaction
		Element e = doc.createElement("gnc:transaction");
		e.setAttribute("version", "2.0.0");

		// give the information of the account
		Element e2;

		String guid = Integer.toHexString(transaction.hashCode());
		e2 = doc.createElement("trn:id");
		e2.setAttribute("type", "guid");
		e2.appendChild(doc.createTextNode(guid));
		e.appendChild(e2);

		e2 = doc.createElement("trn:date-posted");
		addDate(e2, transaction.getDate());
		e.appendChild(e2);

		e2 = doc.createElement("trn:date-entered");
		addDate(e2, (new Date(/* Now */))); // TODO: add the property;
		e.appendChild(e2);

		e2 = doc.createElement("trn:description");
		e2.appendChild(doc.createTextNode(getDescription(transaction)));
		e.appendChild(e2);

		e2 = doc.createElement("trn:splits");
		Iterator entryIt = transaction.getEntryCollection().iterator();
		while (entryIt.hasNext()) {
			Entry entry = (Entry) entryIt.next();
			exportEntry(e2, entry);
		}
		e.appendChild(e2);

		// add the account to the XML file
		bookElement.appendChild(e);
	}

	/**
	 * Add the information of the entry to the "splits" XML-Element of the
	 * transaction.
	 * 
	 * @param splitsElement
	 * @param entry
	 * @author Olivier Faucheux
	 */
	private void exportEntry(Element splitsElement, Entry entry) {
		if (GnucashXMLPlugin.DEBUG)
			System.out.println("Export entry " + entry.hashCode());

		Element entryElement;
		Element e;

		// create the element of the entry and add it the the transaction
		entryElement = doc.createElement("trn:split");
		splitsElement.appendChild(entryElement);

		// full the properties of this event

		String guid = Integer.toHexString(entry.hashCode());
		e = doc.createElement("split:id");
		e.setAttribute("type", "guid");
		e.appendChild(doc.createTextNode(guid));
		entryElement.appendChild(e);

		e = doc.createElement("split:reconciled-state");
		e.appendChild(doc.createTextNode("n")); // TODO - Faucheux: No idea what
		// it is.
		entryElement.appendChild(e);

		e = doc.createElement("split:value");
		String s = entry.getAmount() + "/"
				+ ((entry.getCommodityInternal() == null) ? 2 : entry.getCommodityInternal().getScaleFactor());
		e.appendChild(doc.createTextNode(s));
		entryElement.appendChild(e);

		e = doc.createElement("split:quantity");
		s = entry.getAmount() + "/"
				+ ((entry.getCommodityInternal() == null) ? 2 : entry.getCommodityInternal().getScaleFactor());
		e.appendChild(doc.createTextNode(s));
		entryElement.appendChild(e);

		e = doc.createElement("split:account");
		guid = Integer.toHexString(entry.getAccount().hashCode());
		e.setAttribute("type", "guid");
		e.appendChild(doc.createTextNode(guid));
		entryElement.appendChild(e);

	}

	private void addDate(Element e, Date date) {
		Element d = doc.createElement("ts:date");
		d.appendChild(doc.createTextNode(gnucashDateFormat.format(date)));
		e.appendChild(d);
	}

	/**
	 * Look the description of each Entry of the transaction to determine which
	 * description the transaction has to have.
	 * 
	 * @param t
	 *            the transaction
	 * @return a description
	 * @author Olivier Faucheux
	 */
	@SuppressWarnings("unchecked")
	private String getDescription(Transaction t) {
		String s = null;
		Iterator it = t.getEntryCollection().iterator();
		while (it.hasNext()) {
			Entry e = (Entry) it.next();
			if (s == null)
				s = e.getMemo();
			else if (e.getMemo() != s)
				s = new String("Splitted!");
		}

		if (s == null)
			s = new String("No Entry");

		return s;
	}

}
