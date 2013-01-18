/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2010 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.importer.resources;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "net.sf.jmoney.importer.resources.messages"; //$NON-NLS-1$
	public static String AccountAssociationInfo_Id;
	public static String AccountAssociationInfo_Account;
	public static String Entry_UniqueIdShort;
	public static String ImportDialog_Title;
	public static String Error_AccountNotConfigured;
	
	public static String MemoPatternInfo_EntryCheck;
	public static String MemoPatternInfo_EntryDescription;
	public static String MemoPatternInfo_EntryCategory;
	public static String MemoPatternInfo_EntryMemo;
	public static String MemoPatternInfo_EntryCurrency;

	public static String Account_Import;
	public static String Account_Import_Patterns;
	public static String Account_Import_DefaultCategory;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
