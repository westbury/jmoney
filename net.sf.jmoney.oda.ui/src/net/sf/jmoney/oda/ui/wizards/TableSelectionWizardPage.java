/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2007 Nigel Westbury <westbury@users.sourceforge.net>
 *
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package net.sf.jmoney.oda.ui.wizards;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.ListPropertyAccessor;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.PropertySetNotFoundException;
import net.sf.jmoney.model2.SessionInfo;
import net.sf.jmoney.oda.driver.Driver;
import net.sf.jmoney.oda.ui.Messages;

import org.eclipse.datatools.connectivity.oda.IConnection;
import org.eclipse.datatools.connectivity.oda.IDriver;
import org.eclipse.datatools.connectivity.oda.IParameterMetaData;
import org.eclipse.datatools.connectivity.oda.IQuery;
import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.eclipse.datatools.connectivity.oda.design.DataSetDesign;
import org.eclipse.datatools.connectivity.oda.design.DataSetParameters;
import org.eclipse.datatools.connectivity.oda.design.DesignFactory;
import org.eclipse.datatools.connectivity.oda.design.ResultSetColumns;
import org.eclipse.datatools.connectivity.oda.design.ResultSetDefinition;
import org.eclipse.datatools.connectivity.oda.design.ui.designsession.DesignSessionUtil;
import org.eclipse.datatools.connectivity.oda.design.ui.wizards.DataSetWizardPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;

/*
 Connection properties:
        Properties dataSourceProps = getInitializationDesign()
                .getDataSourceDesign().getPublicProperties();
 */

/**
 * Extends the ODA design ui framework to provide a driver-specific
 * custom editor page to create or edit an ODA data set design instance.
 */
public class TableSelectionWizardPage extends DataSetWizardPage
{
	private IObjectList startingPoint = null;

	static String DEFAULT_MESSAGE = Messages.getString( "tableDialog.defaultMessage" ); //$NON-NLS-1$

	private final int DEFAULT_WIDTH = 200;
	private final int DEFAULT_HEIGHT = 200;

//	private IWizardPage nextPage = new ColumnSelectionWizardPage("title 2");

	private Button sessionButton;
	private Button parameterButton;

	Tree parameterClassTree = null;
	Tree objectClassTree = null;

	/** store latest selected list from session object */
	IObjectList selectedItemList;

	/**
	 * @param pageName
	 */
	public TableSelectionWizardPage( String pageName )
	{
		super( pageName );
		setTitle( pageName );
		setMessage( DEFAULT_MESSAGE );

		setPageComplete( false );
	}

	/**
	 * @param pageName
	 * @param title
	 * @param titleImage
	 */
	public TableSelectionWizardPage( String pageName, String title,
			ImageDescriptor titleImage )
	{
		super( pageName, title, titleImage );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.design.ui.wizards.DataSetWizardPage#createPageCustomControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override 
	public void createPageCustomControl( Composite parent )
	{
		setControl( createPageControl( parent ) );
		initializeControl();
	}

	private void initializeControl()
	{
		/* 
		 * Optionally restores the state of a previous design session.
		 * Obtains designer state, using
		 *      getInitializationDesignerState(); 
		 */

		DataSetDesign dataSetDesign = getInitializationDesign();
		if( dataSetDesign == null )
			return; // nothing to initialize

		String queryText = dataSetDesign.getQueryText();
		if (queryText == null || queryText.length() == 0) {
			// No initial query so we are done.
			return;
		}

		Reader reader = new StringReader(queryText);
		IMemento queryMemento;
		try {
			queryMemento = XMLMemento.createReadRoot(reader);
		} catch (WorkbenchException e) {
			/*
			 * As we don't know the likely causes of this, throw a runtime
			 * exception.
			 */
			throw new RuntimeException(e);
		}

		updateValuesFromQuery(queryMemento);

		/*
		 * Optionally honor the request for an editable or
		 * read-only design session
		 *      isSessionEditable();
		 */
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.datatools.connectivity.oda.design.ui.wizards.DataSetWizardPage#collectDataSetDesign(org.eclipse.datatools.connectivity.oda.design.DataSetDesign)
	 */
	@Override 
	protected DataSetDesign collectDataSetDesign( DataSetDesign design )
	{
		if( ! hasValidData() )
			return design;
		savePage( design );
		return design;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.datatools.connectivity.oda.design.internal.ui.DataSetWizardPage#collectResponseState()
	 */
	@Override 
	protected void collectResponseState()
	{        
		super.collectResponseState();
		/* 
		 * Optionally assigns custom response state, for inclusion
		 * in the ODA design session response, using
		 *      setResponseSessionStatus( SessionStatus status )
		 *      setResponseDesignerState( DesignerState customState ); 
		 */
	}

	/* (non-Javadoc)
	 * @see org.eclipse.datatools.connectivity.oda.design.ui.wizards.DataSetWizardPage#canLeave()
	 */
	@Override 
	protected boolean canLeave()
	{
		if(!isPageComplete()) {
			// TODO: Do we need this?  Message should already be set.
			setMessage("Please complete selection", ERROR );
		}
		return isPageComplete();
	}

	private Control createPageControl(Composite parent)
	{
		Composite composite = new Composite( parent, SWT.NULL );

		composite.setLayout(new FormLayout());

		FormData data = new FormData();
		data.left = new FormAttachment(0, 5);
		data.top = new FormAttachment(0, 5);

		Control startingPointControl = createStartingPointControl(composite);
		startingPointControl.setLayoutData(data);

		data = new FormData();
		data.left = new FormAttachment(0, 5);
		data.top = new FormAttachment(startingPointControl, 5);

		Label label = new Label(composite, SWT.NONE);
		label.setText( Messages.getString("tableDialog.selectList")); //$NON-NLS-1$
		label.setLayoutData(data);

		data = new FormData();
		data.top = new FormAttachment(label, 5);
		data.right = new FormAttachment(75, -5);
		data.left = new FormAttachment(0, 5);
		data.bottom = new FormAttachment(100, -5);
		data.width = DEFAULT_WIDTH;
		data.height = DEFAULT_HEIGHT;
		objectClassTree = new Tree(composite, SWT.BORDER
				| SWT.H_SCROLL | SWT.V_SCROLL);

		setStartingPoint(null);

		objectClassTree.setLayoutData(data);
		objectClassTree.addSelectionListener(new SelectionAdapter() {
			@Override 
			public void widgetSelected(SelectionEvent e)
			{
				IObjectList file = (IObjectList) (objectClassTree.getSelection()[0].getData());

				if (!file.equals(selectedItemList)) {
					selectedItemList = file;
					setPageComplete(true);

					setMessage(DEFAULT_MESSAGE);
				}
			}
		});

		return composite;
	}

	private Control createStartingPointControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new FormLayout());

		FormData data = new FormData();
		data.left = new FormAttachment( 0, 5 );
		data.top = new FormAttachment( 0, 5 );

		Label label = new Label(composite, SWT.NONE);
		label.setText("Select the starting point when building this dataset:");
		label.setLayoutData(data);

		Composite areaComposite = new Composite(composite, SWT.NULL);
		data = new FormData();
		data.left = new FormAttachment( 0, 5 );
		data.top = new FormAttachment(label, 5);
		areaComposite.setLayoutData(data);

		areaComposite.setLayout(new GridLayout(1, false));

		// Create the edit controls
		sessionButton = new Button(areaComposite, SWT.RADIO);
		parameterButton = new Button(areaComposite, SWT.RADIO);
		sessionButton.setText("Start at the session object (the root of the data model)");
		parameterButton.setText("Start with an object or list of objects passed as a parameter at runtime");

		sessionButton.addSelectionListener(new SelectionAdapter() {
			@Override 
			public void widgetSelected(SelectionEvent e) {
				setStartingPoint(new ObjectList_SessionObject());
				parameterClassTree.setEnabled(false);
				refreshErrorMessage();
			}
		});
		sessionButton.setLayoutData(new GridData(
				GridData.HORIZONTAL_ALIGN_FILL
				| GridData.VERTICAL_ALIGN_CENTER));

		parameterButton.addSelectionListener(new SelectionAdapter() {
			@Override 
			public void widgetSelected(SelectionEvent e) {
				if (parameterClassTree.getSelection().length == 1) {
					setStartingPoint(new ObjectList_ParameterObject((ExtendablePropertySet)parameterClassTree.getSelection()[0].getData()));
				} else {
					setStartingPoint(null);
				}
				parameterClassTree.setEnabled(true);
				refreshErrorMessage();
			}
		});
		parameterButton.setLayoutData(new GridData(
				GridData.HORIZONTAL_ALIGN_FILL
				| GridData.VERTICAL_ALIGN_CENTER));


		data = new FormData();
		data.top = new FormAttachment(areaComposite, 5);
		data.right = new FormAttachment(75, -5);
		data.left = new FormAttachment(0, 5);
		data.bottom = new FormAttachment(100, -5);
		data.width = DEFAULT_WIDTH;
//		data.height = DEFAULT_HEIGHT;
/*
        final Composite alternativeContentContainer = new Composite(composite, SWT.BORDER);
		alternativeContentContainer.setLayoutData(data);

		final StackLayout alternativeContentLayout = new StackLayout();
		alternativeContentContainer.setLayout(alternativeContentLayout);
 */

		parameterClassTree = new Tree( composite, SWT.BORDER
				| SWT.H_SCROLL | SWT.V_SCROLL );

		/*
		 * Add the property sets to the tree. We use a recursive method to do
		 * this so that the property sets are put into the tree with derived
		 * property sets being child items in the tree.
		 * 
		 * Note that the session property set is excluded. This is because there
		 * is only ever a single session object so there is no point in passing
		 * it as a parameter.
		 */
		for (ExtendablePropertySet propertySet: PropertySet.getAllExtendablePropertySets()) {
			if (propertySet.getBasePropertySet() == null
					&& propertySet != SessionInfo.getPropertySet()) {
				TreeItem item = new TreeItem(parameterClassTree, 0);
				initTreeItem(item, propertySet);
			}
		}

		parameterClassTree.setLayoutData(data);

		parameterClassTree.addSelectionListener(new SelectionAdapter() {
			@Override 
			public void widgetSelected( SelectionEvent e )
			{
				if (parameterClassTree.getSelection().length == 1) {
					setStartingPoint(new ObjectList_ParameterObject((ExtendablePropertySet)parameterClassTree.getSelection()[0].getData()));
				} else {
					setStartingPoint(null);
				}
			}
		});

		parameterClassTree.setEnabled(false);

		return composite;
	}

	protected void setStartingPoint(IObjectList startingPoint) {
		ExtendablePropertySet<?> oldStartingPropertySet =
			this.startingPoint == null
			? null
			: this.startingPoint.getPropertySet();
		
		ExtendablePropertySet<?> newStartingPropertySet =
			startingPoint == null
			? null
			: startingPoint.getPropertySet();
		
		if (newStartingPropertySet != oldStartingPropertySet) {
			objectClassTree.removeAll();

			if (newStartingPropertySet == null) {
				objectClassTree.setEnabled(false);
				setPageComplete(false);
				setMessage("You must select a starting point");
			} else {
				TreeItem rootItem = new TreeItem(objectClassTree, SWT.NONE);
				rootItem.setData(startingPoint);
				rootItem.setText("The " + newStartingPropertySet.getObjectDescription() + " object");
				
				for (ListPropertyAccessor childListAccessor: newStartingPropertySet.getListProperties3()) {
					TreeItem item = new TreeItem(rootItem, SWT.NONE);
					addListPropertyAccessors(item, childListAccessor, startingPoint);
				}

				if (Account.class.isAssignableFrom(newStartingPropertySet.getImplementationClass())) {
					TreeItem item = new TreeItem(rootItem, SWT.NONE);
					item.setData(new ObjectList_EntriesInAccount(startingPoint));
					item.setText("entries in the account");
				}
				
				if (objectClassTree.getItemCount() == 0) {
					setPageComplete(true);
					setMessage(DEFAULT_MESSAGE);
				} else {
					setPageComplete(false);
					setMessage("You must select a list property from the lower list control");
				}
				objectClassTree.setEnabled(true);
			}
		}
	
		this.startingPoint = startingPoint;
	}

	/**
	 * This is a private method that recursively adds
	 * property sets to the tree control, with derived
	 * property sets being children of their base
	 * property set.
	 * 
	 * @param item
	 * @param propertySet
	 */
	private void initTreeItem(TreeItem item, ExtendablePropertySet<?> propertySet) {
		item.setText(propertySet.getObjectDescription());
		item.setData(propertySet);

		for (ExtendablePropertySet derivedPropertySet: propertySet.getDirectlyDerivedPropertySets()) {
			TreeItem childItem = new TreeItem(item, 0);
			initTreeItem(childItem, derivedPropertySet);
		}
	}

	private void addListPropertyAccessors(TreeItem parent, ListPropertyAccessor<?> accessor, IObjectList parentItemList) {

		/*
		 * This list property may already have been used higher up.  This would cause
		 * recursion.  In this situation, we add the list (marked as a recursive use)
		 * but we do not add any child items.  It the user selects this item then the
		 * query execution will result in a recursive evaluation (going back to the
		 * prior use of this list property to process the nested use).
		 */
		if (parentItemList != null && parentItemList.isUsed(accessor)) {
			IObjectList itemList2 = new ObjectList_Recursive(parentItemList, accessor, true);
			parent.setData(itemList2);
			parent.setText(accessor.getName() + " (recursive)");
		} else {
			parent.setText(accessor.getName());

			ObjectList_ListProperty itemList = new ObjectList_ListProperty(parentItemList, accessor);
			parent.setData(itemList);

			ExtendablePropertySet<?> propertySet = accessor.getElementPropertySet();
			
			if (Account.class.isAssignableFrom(propertySet.getImplementationClass())) {
				TreeItem item = new TreeItem(parent, SWT.NONE);
				item.setData(new ObjectList_EntriesInAccount(parentItemList));
				item.setText("entries in the account");
			}
			
			addFilterOnClassItems(parent, itemList, propertySet, "");

			for (ListPropertyAccessor childListAccessor: accessor.getElementPropertySet().getListProperties3()) {
				TreeItem item = new TreeItem(parent, SWT.NONE);
				addListPropertyAccessors(item, childListAccessor, itemList);
			}
		}
	}

	private void addFilterOnClassItems(TreeItem parent, ObjectList_ListProperty parentItemList, ExtendablePropertySet<?> propertySet, String prefix) {
		for (ExtendablePropertySet<?> derivedPropertySet: propertySet.getDirectlyDerivedPropertySets()) {
			TreeItem item = new TreeItem(parent, SWT.NONE);
			IObjectList itemList = new ObjectList_Filter(parentItemList, derivedPropertySet);
			item.setData(itemList);

			item.setText("processing only '" + prefix + derivedPropertySet.getObjectDescription() + "' objects");

			for (ListPropertyAccessor childListAccessor: derivedPropertySet.getListProperties3()) {
				TreeItem item2 = new TreeItem(item, SWT.NONE);
				addListPropertyAccessors(item2, childListAccessor, itemList);
			}

			addFilterOnClassItems(parent, parentItemList, derivedPropertySet, "- " + prefix);
		}
	}

	protected void refreshErrorMessage() {
		if (startingPoint == null) {
			setErrorMessage("You must choose one of the selections for the starting point for the query.");
			return;
		}

		if (startingPoint instanceof ObjectList_ParameterObject
				&& parameterClassTree.getSelection().length == 0) {
			setErrorMessage("You must select the type of object that will be passed as a parameter and used as a starting point for this query.");
			return;
		}

		setErrorMessage(null);
	}

	/**
	 * Attempts to close given ODA connection.
	 * @param conn
	 */
	private void closeConnection( IConnection conn )
	{
		try
		{
			if( conn != null )
				conn.close();
		}
		catch( OdaException e )
		{
			// ignore
		}
	}

	/**
	 * Return query of this data set
	 * 
	 * @return
	 */
	private String getQuery()
	{
		XMLMemento queryRoot = XMLMemento.createWriteRoot("dataset");
		this.selectedItemList.save(queryRoot);

		Writer writer = new StringWriter();
		try {
			queryRoot.save(writer);
		} catch (IOException e1) {
			/*
			 * As we don't know the likely causes of this, throw a runtime
			 * exception.
			 */
			throw new RuntimeException(e1);
		}

		return writer.toString();
	}

	/**
	 * Sets the initial state of the controls depending on the
	 * query text.  The query text has already been converted
	 * to an IMemento, so we extract the data from that.
	 * 
	 * @param query
	 */
	private void updateValuesFromQuery(IMemento queryMemento) {
		TreeItem treeItem = findTreeItem(queryMemento);
/*		
		IMemento tableMemento = queryMemento.getChild("listProperty");
		if (tableMemento != null) {
			treeItem = findTreeItem(tableMemento);
		}
*/
		if (treeItem == null) {
			setPageComplete(false);
		} else {
			objectClassTree.setSelection(treeItem);
			selectedItemList = (IObjectList)treeItem.getData();
			setPageComplete(true);
		}
	}

	private TreeItem findTreeItem(IMemento itemListMemento) {

		IMemento child = itemListMemento.getChild("listProperty");
		if (child != null) {
			TreeItem parentTreeItem = findTreeItem(child);

			String propertySetId = child.getString("filter");
			if (propertySetId != null) {
				for (TreeItem treeItem: parentTreeItem.getItems()) {
					IObjectList itemList = (IObjectList)treeItem.getData();
					if (itemList instanceof ObjectList_Filter
							&& ((ObjectList_Filter)itemList).filterOnClass.getId().equals(propertySetId)) {
						parentTreeItem = treeItem;
						break;
					}
				}
			}

			String listId = itemListMemento.getString("listId");
			for (TreeItem treeItem: parentTreeItem.getItems()) {
				IObjectList itemList = (IObjectList)treeItem.getData();
				if (itemList instanceof ObjectList_ListProperty) {
					if (((ObjectList_ListProperty)itemList).list.getName().equals(listId)) {
						return treeItem;
					}
				}
			}
			
			return null;
		} else {
			child = itemListMemento.getChild("entriesInAccount");
			if (child != null) {
				TreeItem parentTreeItem = findTreeItem(child);

				for (TreeItem treeItem: parentTreeItem.getItems()) {
					IObjectList itemList = (IObjectList)treeItem.getData();
					if (itemList instanceof ObjectList_EntriesInAccount) {
						return treeItem;
					}
				}
				
				return null;
			} else {
				child = itemListMemento.getChild("parameter");
				if (child != null) {
					parameterButton.setSelection(true);
					parameterClassTree.setEnabled(true);

					String propertySetId = child.getString("propertySetId");
					if (propertySetId == null) {
						/*
						 * Query is invalid for the set of installed property sets.
						 * Return null which will result in no initial selection in
						 * the control.
						 */
						setStartingPoint(null);
						return null;
					}

					try {
						ExtendablePropertySet<?> propertySet = PropertySet.getExtendablePropertySet(propertySetId);

						setStartingPoint(new ObjectList_ParameterObject(propertySet));

						/*
						 * Set the selection in the parameter type tree
						 */
						TreeItem item = findParameterTypeTreeItem(propertySet);
						parameterClassTree.setSelection(item);

						/*
						 * Now that the object tree is built, start at the top
						 * level item.
						 *
						 * This memento represents a list in the parameter
						 * object, so scan the top level of the tree control.
						 */
						return objectClassTree.getItem(0);
						
					} catch (PropertySetNotFoundException e) {
						/*
						 * Query is invalid for the set of installed property sets.
						 * Return null which will result in no initial selection in
						 * the control.
						 */
						setStartingPoint(null);
						return null;
					}

				} else {
					sessionButton.setSelection(true);
					parameterClassTree.setEnabled(false);
					
					setStartingPoint(new ObjectList_ParameterObject(SessionInfo.getPropertySet()));

					/*
					 * This memento represents a list in the session
					 * object, so scan the top level of the tree control.
					 */
					return objectClassTree.getItem(0);
				}
			}
		}
	}

	private TreeItem findParameterTypeTreeItem(ExtendablePropertySet propertySet) {
		TreeItem[] treeItems;

		if (propertySet.getBasePropertySet() == null) {
			treeItems = parameterClassTree.getItems();
		} else {
			TreeItem parentItem = findParameterTypeTreeItem(propertySet.getBasePropertySet());
			treeItems = parentItem.getItems();
		}
		
		for (TreeItem treeItem: treeItems) {
			if (treeItem.getData() == propertySet) {
				return treeItem;
			}
		}

		throw new RuntimeException("All extendable property sets should be in the tree control");
	}
	
	private boolean hasValidData()
	{
		// TODO: this is not right.
		if (objectClassTree == null || this.objectClassTree.getSelection().length == 0) {
			setMessage("Please complete selection", ERROR);
			return false;
		}

		if( isPageComplete() )
		{
			return true;
		}
		return false;
	}

	/**
	 * Updates the given dataSetDesign with the query and its metadata defined
	 * in this page.
	 * 
	 * @param dataSetDesign
	 */
	private void savePage(DataSetDesign dataSetDesign)
	{
		String queryText = getQuery();
		dataSetDesign.setQueryText(queryText);

		/*
		 * Obtain query's result set metadata and update the dataSetDesign with
		 * it.
		 */
		IConnection conn = null;
		try {
			IDriver ffDriver = new Driver();
			conn = ffDriver.getConnection(null);

			/*
			 * Obtains the query's result set metadata from the ODA runtime driver by
			 * preparing the given query. Uses the data source connection properties
			 * setting.
			 */
			java.util.Properties prop = new java.util.Properties();

			// Set the properties from the fields here.

			conn.open(prop);

			IQuery query = conn.newQuery(null);
			
			/*
			 * The query needs to be prepared before the metadata is available.
			 */
			query.prepare(queryText);

			setResultSetMetaData(dataSetDesign, query.getMetaData());

			setParameterMetaData(dataSetDesign, query.getParameterMetaData());
		} catch (OdaException e) {
			// no result set definition available, reset in dataSetDesign
			dataSetDesign.setResultSets(null);
			dataSetDesign.setParameters(null);
		} finally {
			if (conn != null) {
				closeConnection(conn);
			}
		}
	}

	private void setResultSetMetaData(DataSetDesign dataSetDesign,
			IResultSetMetaData metadata) throws OdaException
			{
		ResultSetColumns columns = DesignSessionUtil.toResultSetColumnsDesign(metadata);

		ResultSetDefinition resultSetDefn = DesignFactory.eINSTANCE
		.createResultSetDefinition();
		resultSetDefn.setResultSetColumns(columns);

		dataSetDesign.setPrimaryResultSet( resultSetDefn );
		dataSetDesign.getResultSets().setDerivedMetaData( true );
			}

	private void setParameterMetaData(DataSetDesign dataSetDesign,
			IParameterMetaData metadata) throws OdaException
			{
		DataSetParameters parameters = DesignSessionUtil.toDataSetParametersDesign(metadata);
		dataSetDesign.setParameters(parameters);
			}
}
