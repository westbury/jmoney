/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2004 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.MalformedPluginException;
import net.sf.jmoney.model2.PageEntry;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.PropertySetNotFoundException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

/*
 * The content provider class is responsible for
 * providing objects to the view. It can wrap
 * existing objects in adapters or simply return
 * objects as-is. These objects may be sensitive
 * to the current input of the view, or ignore
 * it and always show the same content 
 * (like Task List, for example).
 */
public class TreeNode implements IAdaptable {
	
	/** 
	 * Maps the full id of the node to the TreeNode.
	 */
	private static Map<String, TreeNode> idToNodeMap = new HashMap<String, TreeNode>();
	
	private static TreeNode invisibleRoot;

	private String id;
	private String label;
	private Image image = null;
	private ImageDescriptor imageDescriptor;
	private TreeNode parent;
	private String parentId;
	private int position;
	private IDynamicTreeNode dynamicTreeNode;
	protected ArrayList<Object> children = null;
	
	private Vector<PageEntry> pageFactories = new Vector<PageEntry>();

	/**
	 * Initialize the navigation tree nodes.
	 * <P>
	 * The initialization of the navigation nodes depends on
	 * the property sets.  Therefore PropertySet.init must be
	 * called before this method. 
	 */
	public static void init() {
		// Load the extensions
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		for (IConfigurationElement element: registry.getConfigurationElementsFor("net.sf.jmoney.pages")) { //$NON-NLS-1$
			if (element.getName().equals("node")) { //$NON-NLS-1$

				String label = element.getAttribute("label"); //$NON-NLS-1$
				String icon = element.getAttribute("icon"); //$NON-NLS-1$
				String id = element.getAttribute("id"); //$NON-NLS-1$
				String parentNodeId = element.getAttribute("parent"); //$NON-NLS-1$
				String position = element.getAttribute("position"); //$NON-NLS-1$

				if (id != null && id.length() != 0) {
					String fullNodeId = element.getNamespaceIdentifier() + '.' + id;
					ImageDescriptor descriptor = null;
					if (icon != null) {
						// Try getting the image from this plug-in.
						descriptor = JMoneyPlugin.imageDescriptorFromPlugin(element.getContributor().getName(), icon); 
						if (descriptor == null) {
							// try getting the image from the JMoney plug-in. 
							descriptor = JMoneyPlugin.imageDescriptorFromPlugin("net.sf.jmoney", icon); //$NON-NLS-1$
						}
					}

					int positionNumber = 800;
					if (position != null) {
						positionNumber = Integer.parseInt(position);
					}

					IDynamicTreeNode dynamicTreeNode = null;
					try {
						Object listener = element.createExecutableExtension("class"); //$NON-NLS-1$
						if (!(listener instanceof IDynamicTreeNode)) {
							throw new MalformedPluginException(
									"Plug-in " + element.getContributor().getName() //$NON-NLS-1$
									+ " extends the net.sf.jmoney.pages extension point. " //$NON-NLS-1$
									+ "However, the class specified by the class attribute in the node element " //$NON-NLS-1$
									+ "(" + listener.getClass().getName() + ") " //$NON-NLS-1$ //$NON-NLS-2$
									+ "does not implement the IDynamicTreeNode interface. " //$NON-NLS-1$
									+ "This interface must be implemented by all classes referenced " //$NON-NLS-1$
									+ "by the class attribute."); //$NON-NLS-1$
						}

						dynamicTreeNode = (IDynamicTreeNode)listener;
					} catch (CoreException e) {
						if (e.getStatus().getException() == null) {
							/*
							 * The most likely situation is that no class is specified.
							 * This is valid because the class attribute is optional.
							 * It this situation we contruct a TreeNode.
							 */
						} else {
							if (e.getStatus().getException() instanceof ClassNotFoundException) {
								ClassNotFoundException e2 = (ClassNotFoundException)e.getStatus().getException();
								throw new MalformedPluginException(
										"Plug-in " + element.getContributor().getName() //$NON-NLS-1$
										+ " extends the net.sf.jmoney.pages extension point. " //$NON-NLS-1$
										+ "However, the class specified by the class attribute in the node element " //$NON-NLS-1$
										+ "(" + e2.getMessage() + ") " //$NON-NLS-1$ //$NON-NLS-2$
										+ "could not be found. " //$NON-NLS-1$
										+ "The class attribute must specify a class that implements the " //$NON-NLS-1$
										+ "IDynamicTreeNode interface."); //$NON-NLS-1$
							}
							e.printStackTrace();
							continue;
						}
					}

					TreeNode node = new TreeNode(fullNodeId, label, descriptor, parentNodeId, positionNumber, dynamicTreeNode);
					idToNodeMap.put(fullNodeId, node);
				}
			}
		}
		
		// Set each node's parent.  If no node exists
		// with the given parent node id then the node
		// is placed at the root.

		invisibleRoot = new TreeNode("root", "", null, "", 0, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		for (TreeNode treeNode: idToNodeMap.values()) {
			TreeNode parentNode;
			if (treeNode.getParentId() != null) {
				parentNode = idToNodeMap.get(treeNode.getParentId());
				if (parentNode == null) {
					parentNode = invisibleRoot;
				}
			} else {
				parentNode = invisibleRoot;
			}
			treeNode.setParent(parentNode);
			parentNode.addChild(treeNode);
		}	
		
		// Set the list of pages for each node.
		for (IConfigurationElement element: registry.getConfigurationElementsFor("net.sf.jmoney.pages")) { //$NON-NLS-1$
			if (element.getName().equals("pages")) { //$NON-NLS-1$
				// TODO: remove plug-in as bad if the id is not unique.
				String id = element.getAttribute("id"); //$NON-NLS-1$
				String pageId = element.getNamespaceIdentifier() + '.' + id;
				String nodeId = element.getAttribute("node"); //$NON-NLS-1$

				String position = element.getAttribute("position"); //$NON-NLS-1$
				int pos = 5;
				if (position != null) {
					pos = Integer.parseInt(position);
				}

				if (nodeId != null && nodeId.length() != 0) {
					TreeNode node = idToNodeMap.get(nodeId);
					if (node != null) {
						node.addPage(pageId, element, pos);
					} else {
						// No node found with given id, so the
						// page listener is dropped.
						// TODO Log missing node.
					}
				} else {
					// No 'node' attribute so see if we have
					// an 'extendable-property-set' attribute.
					// (This means the page should be supplied if
					// the node represents an object that contains
					// the given property set).
					String propertySetId = element.getAttribute("extendable-property-set"); //$NON-NLS-1$
					if (propertySetId != null) {
						try {
							ExtendablePropertySet<?> pagePropertySet = PropertySet.getExtendablePropertySet(propertySetId);
							PageEntry pageEntry = new PageEntry(pageId, element, pos);  

							for (ExtendablePropertySet<?> derivedPropertySet: pagePropertySet.getDerivedPropertySets()) {
								derivedPropertySet.addPage(pageEntry);
							}
						} catch (PropertySetNotFoundException e1) {
							// This is a plug-in error.
							// TODO implement properly.
							e1.printStackTrace();
						}
					}
				}
			}
		}

		// If a node has no child nodes and no page listeners
		// then the node is removed.  This allows nodes to be
		// created by the framework or the more general plug-ins
		// that have no functionality provided by the plug-in that
		// created the node but that can be extended by other
		// plug-ins.  By doing this, rather than expecting plug-ins
		// to create their own nodes, it is more likely that
		// different plug-in developers will share nodes, and
		// thus avoiding hundreds of root nodes in the navigation
		// tree, each with a single tab view. 

		// TODO: implement this
	}
	
	public static TreeNode getInvisibleRoot() {
		return invisibleRoot;
	}

	/**
	 * @param nodeId the full id of a node
	 * @return the node, or null if no node with the given id exists
	 */
	public static TreeNode getTreeNode(String nodeId) {
		return idToNodeMap.get(nodeId);
	}
	
	public TreeNode(String id, String label, ImageDescriptor imageDescriptor, String parentId, int position, IDynamicTreeNode dynamicTreeNode) {
		this.id = id;
		this.label = label;
		this.imageDescriptor = imageDescriptor;
		this.parentId = parentId;
		this.position = position;
		this.dynamicTreeNode = dynamicTreeNode;
	}

	/**
	 * @return
	 */
	public String getId() {
		return id;
	}

	public String getLabel() {
		return label;
	}

	public TreeNode getParent() {
		return parent;
	}

	int getPosition() {
		return position;
	}

	@Override
	public String toString() {
		return getLabel();
	}

	@Override
	public Object getAdapter(Class key) {
		return null;
	}

	public Image getImage() {
		if (image == null && imageDescriptor != null) {
			image = imageDescriptor.createImage();
		}
		return image;
	}

	public void addChild(Object child) {
		if (children == null) {
			children = new ArrayList<Object>();
		}
		children.add(child);
	}
	
	public void removeChild(Object child) {
		children.remove(child);
	}
	
	public Object [] getChildren() {
		if (children == null) {
			if (dynamicTreeNode == null) {
				return new Object[0];
			} else {
				return dynamicTreeNode.getChildren().toArray();
			}
		} else {
			if (dynamicTreeNode == null) {
				return children.toArray();
			} else {
				ArrayList<Object> combinedChildren = new ArrayList<Object>();
				combinedChildren.addAll(dynamicTreeNode.getChildren());
				combinedChildren.addAll(children);
				return combinedChildren.toArray();
			}
		}
	}
	
	public boolean hasChildren() {
		return (children != null && children.size() > 0)
		|| (dynamicTreeNode != null && dynamicTreeNode.hasChildren());
	}
	
	/**
	 * @return
	 */
	public Object getParentId() {
		return parentId;
	}

	/**
	 * @param parentNode
	 */
	public void setParent(TreeNode parent) {
		this.parent = parent;
	}

	/**
	 * @param pageListener
	 */
	public void addPage(String pageId, IConfigurationElement pageElement, int pos) {
		PageEntry pageEntry = new PageEntry(pageId, pageElement, pos);  
		pageFactories.add(pageEntry);
	}

	/**
	 * @return An array of objects that implement the IBookkeepingPage
	 * 		interface.  The returned value is never null but the Vector may
	 * 		be empty if there are no pages for this node.
	 */
	public Vector<PageEntry> getPageFactories() {
		return pageFactories;
	}
}


