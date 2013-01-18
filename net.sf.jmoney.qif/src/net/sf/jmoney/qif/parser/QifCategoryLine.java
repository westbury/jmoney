/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (C) 2008 Nigel Westbury
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
 *  Foundation, 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package net.sf.jmoney.qif.parser;


/**
 * This class is used as the value of the category line in either
 * a transaction or a split transaction.
 */
public class QifCategoryLine {

	private Type type;
	private String name;
	private String subCategoryName;
	
	public enum Type {
		CategoryType,
		SubCategoryType,
		TransferType
	}

	public QifCategoryLine(String value) {
		if (value.charAt(0) == '[') {
			// transfer
			type = Type.TransferType;
			name = value.substring(1, value.length() - 1);
		} else {
			// assumption: a category consists at least of one char
			// either "LCategory" or "LCategory:Subcategory"
			String parts [] = value.split(":");
			if (parts.length == 1) {
				// "LCategory"
				name = value;
				type = Type.CategoryType;
			} else {
				// "LCategory:Subcategory
				name = parts[0];
				subCategoryName = parts[1];
				type = Type.SubCategoryType;
			}
		}
	}
	
	public Type getType() {
		return type;
	}

	public String getName() {
		return name;
	}
	
	public String getSubCategoryName() {
		assert(type == Type.SubCategoryType);
		return subCategoryName;
	}
	
	
}
