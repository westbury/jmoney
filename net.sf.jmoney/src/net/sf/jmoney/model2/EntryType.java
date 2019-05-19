package net.sf.jmoney.model2;

/**
 * Most transactions contain two entries, a debit and a credit entry.  Quite a few
 * transactions contain muliple credit entries and/or multiple debit entries.  These are
 * often known as split entries, though in JMoney we don't use the term 'split entry' because
 * it is not a special case.  All transactions are just a list of two or more entries that add
 * up to zero (or may not add up to zero if entries involve different currencies or commodities).
 * 
 * There are some forms of transactions that contain standard sets of entries.  A good example is
 * a stock sale.  Each transaction will contain:
 * <UL>
 * <LI> A credit entry in the account to which the net sale proceeds are deposited
 * <LI> A debit of a quantity of stock from the stock account
 * <LI> A credit to the commission expense account
 * <LI> Possibly a credit to a tax account, if there is tax withholding
 * <LI> Possibly credits to other expense accounts for taxes and fees that may have been charged
 * <UL>
 * If one were entering such a transaction manually then one could enter this as a transaction with multiple entries,
 * manually adding each entry and selecting the appropriate account for each entry.  A far better user interface would
 * be for the user to select the transaction type ('stock sale' in this case), which results in a form being
 * presented to the user into which the user can enter the stock, the quantity, the commission amount and so on.  The account
 * to use for each has been preconfigured.
 * <P>
 * The transactions are still stored internally as a list of entries each entry with an amount and account.  It is important
 * that this is so because other plugins will not know about stock transactions but they must still be able to process the transactions.
 * For example a plugin that shows expenses must still be able to include stock sale commissions even though the plugin knows
 * nothing about stock.
 * <P>
 * An implication of this is that each transaction must be analyzed to identify its type and to find entries that may match the various
 * known entry types in the transaction type.  For example, if the transaction is found to have an entry for a decrease of stock and a credit
 * of currency then it is assumed to be a stock sale.  Then a search of the entries
 * is made for any entry that has an account that is the preconfigured commission account.
 * <P>
 * This process of analyzing the entries and accounts in a transaction to guess the transaction type does work.  However
 * it is considered a little hacky and fragile.  As we start supporting more exotic transaction types such as takeovers and stock splits,
 * it is felt we need to be more explicit when storing the transaction. 
 * <P>
 * We don't actually store the transaction type in the transaction as one might expect.  The reason is that one transaction
 * may actually contain multiple transaction types.  For example, suppose a broker bought one stock, sold another, and credited the difference
 * to your bank account.  It may be we are trying to be over flexible, but better to be over flexible and then see how we can use entry types
 * than to be too restrictive.  Considered also that the broker may sell two different stocks and buy another stock, before sending the proceeds as
 * a single credit to your bank account.  So we want to cope with the case where the same transaction type may occur multiple times
 * in a single transaction.
 * <P>
 * Each entry can have multiple entry types.  However this is really only because JMoney supports plugins that don't know about each other,
 * so one plugin does not need to wipe out an entry type set by another plugin, and also because of the situation above with multiple purchases
 * and sales in a single transaction.
 * <P>
 * There may be more than one entry with the same entry type in the same transaction type.  In that situation the total of all the entries
 * is calculated and used as the amount for that entry type.  This is like a split entry.  For example, suppose a broker is instructed to pay
 * all dividends equally into two bank accounts, half in each.  There would then be two entries with the 'net dividend payment' type.  The UI
 * would typically show the total of the two amounts as the net dividend.  If the UI also showed details of where dividends are being paid then
 * the UI would need to support split entries.
 * <P>
 * So, within the scope of a transaction:
 * 
 * <transaction-type>:<instance-id>:<entry-type>
 * 
 * <transaction-type> := an id, fully qualified with the plugin id.  Letters, digits, . or -
 * <instance-id> := may be empty, use only if a given transaction type has multiple occurrences in a single transaction
 * <entry-type> := fixed type with names unique only within the namespace of a transaction type
 * 
 * plus multiple can be separated by commas.
 * 
 * @author Nigel
 *
 */
public class EntryType {

}
