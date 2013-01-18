/*
 * 
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2009 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.ofx;

import java.io.File;

import net.sf.jmoney.IDroppedFileImporter;

import org.eclipse.ui.IWorkbenchWindow;

public class DroppedFileImporter implements IDroppedFileImporter {

	@Override
	public void importFile(File file, IWorkbenchWindow window) {
		OfxImporter importer = new OfxImporter(window);
		importer.importFile(file);
	}
}
