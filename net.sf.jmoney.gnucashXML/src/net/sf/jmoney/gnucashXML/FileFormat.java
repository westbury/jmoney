/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2001-2003 Johann Gyger <jgyger@users.sf.net>
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

package net.sf.jmoney.gnucashXML;

import java.io.File;

import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Session;

/**
 * Operations to import from (export to) a specific file format.
 * 
 * @author Jan-Pascal van Best
 * @author Johann Gyger
 * 
 * TODO: This interface defines what an Export/Import tool has to do.
 * It should be directly defined in the jmoney-Plugin, not in the gnucashXML 
 */
public interface FileFormat {

    /**
     * Import data from "file" into "session".
     * 
     * @param session Session where data is being imported
     * @param file Import file
     */
    public void importFile(Session session, File file);
    
    /**
     * Export "account" data from "session" into "file".
     * 
     * @param session Session that contains account
     * @param account CapitalAccount to export
     * @param file Export file
     */
    void exportAccount(Session session, CapitalAccount account, File file);
}
