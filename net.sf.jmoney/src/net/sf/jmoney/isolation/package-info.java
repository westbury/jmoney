/**
This package contains the classes needed for transactional support
in the API to the model.
<p>
Transactional support is useful in a number of situations:
<ol>
<li>Suppose you have a dialog box that makes changes to the model.
No one outside of the dialog should see any of the changes until the user
presses 'OK'.  This can be done by using delayed databinding that does not
pass changes from the UI controls to the model until the user presses 'OK'.
However that can be difficult to do because changes made by some controls
in the dialog may be making changes in the model that affect other controls
in the dialog.  There may also be nested dialogs that also make changes to
the model.  The proper way of dealing with these issues is to have each dialog
interface to a different 'copy' of the model.  Each dialog is then free to bind
its controls to its 'copy' and make changes to its 'copy'.  When the user presses
'OK', the changes are made to the parent 'copy' and it the user presses 'Cancel'
then the 'copy' is simply garbage collected and no one else ever sees any of the
changes.
</li>
<li>The same applies to editors.  Changes made within an editor may affect other parts
within the editor by data binding through the model but should not be seen by anyone else
until the editor is saved.
</li>
<li>There are other situations where you want every little change to be reflected
in the model so that validation indicators are kept current but you don't want every
little change to update across the client or to update underlying databases.
</ol>

We call 'copy' of the model a transaction.  When the transaction is 'committed', all
the changes are then made to the parent 'copy'.  The top-level copy is typically backed
by a database but it could, for example, be serialized and de-serialized to and from a file.
<p> 
The word 'copy' was used in the above.  However the implementation provided by this
package does not take a complete copy of the entire model each time a transaction
is created.  Instead objects are created lazily.
<p>
Changes are propagated to child transactions as they occur.  For example, if two transactions
are created off the same model then changes in one transaction will not be seen in the other.  But if
one transaction is committed into the top-level model then those changes will be seen in the other
transaction.  This does beg the question of how conflicts are handled.  The proper way to handle conflicts
is through use of the 'stale' state of a property.  When a property value is changed in one transaction,
the property value in the top-level model is set to be 'stale'.  It remains stale until the transaction is
either committed or is disposed.  The 'stale' state is passed on to all child transactions and all further transactions
nested in those transactions.  A well-behaved UI would then put UI controls into read-only mode to the extent necessary
to prevent changes to those properties.  In practice it is rarely necessary to implement control locking because the application
design rarely has such conflicts, but it is good to have the mechanism in place for those cases where conflicts
may happen.
<p>
Because undo/redo fits so well into this layer, it is supported by the implementation in this package.
All changes committed at one time form one change for the purposes of undo/redo.  The 'commit' method takes as a parameter localized text describing the change.
This text is used as the label in the undo/redo framework.  So you get undo/redo almost for free (the only extra effort
is to provide the label).
*/
package net.sf.jmoney.isolation;