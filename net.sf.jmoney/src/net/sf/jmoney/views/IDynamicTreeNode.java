package net.sf.jmoney.views;

import java.util.Collection;

public interface IDynamicTreeNode {

	public abstract boolean hasChildren();

	public abstract Collection<Object> getChildren();

}