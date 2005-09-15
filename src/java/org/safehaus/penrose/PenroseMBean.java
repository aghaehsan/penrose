/**
 * Copyright (c) 2000-2005, Identyx Corporation.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.safehaus.penrose;

import java.io.IOException;
import java.util.*;

import org.slf4j.Logger;
import org.safehaus.penrose.config.Config;
import org.safehaus.penrose.acl.ACLEngine;

/**
 * @author Administrator
 */
public interface PenroseMBean {
	
	// ------------------------------------------------
	// Functional Methods
	// ------------------------------------------------
	public int init() throws Exception;
	public void addConfig(Config config) throws Exception;
	public void stop();

	/*
    public int bind(PenroseConnection connection, String dn, String password) throws Exception;
	public int unbind(PenroseConnection connection) throws Exception;
    public SearchResults search(PenroseConnection connection, String base, int scope,
            String filter, Collection attributeNames)
            throws Exception;
    public SearchResults search(PenroseConnection connection, String base, int scope,
            int deref, String filter, Collection attributeNames)
            throws Exception;
    public int add(PenroseConnection connection, LDAPEntry entry) throws Exception;
    public int delete(PenroseConnection connection, String dn) throws Exception;
    public int modify(PenroseConnection connection, String dn, List modifications) throws Exception;
	public int compare(PenroseConnection connection, String dn, String attributeName,
			String attributeValue) throws Exception;
	*/
	
	// ------------------------------------------------
	// Listeners
	// ------------------------------------------------
	/*
	public void addConnectionListener(ConnectionListener l);
	public void removeConnectionListener(ConnectionListener l);
	public void addBindListener(BindListener l);
	public void removeBindListener(BindListener l);
	public void addSearchListener(SearchListener l);
	public void removeSearchListener(SearchListener l);
	public void addCompareListener(CompareListener l);
	public void removeCompareListener(CompareListener l);
	public void addAddListener(AddListener l);
	public void removeAddListener(AddListener l);
	public void addDeleteListener(DeleteListener l);
	public void removeDeleteListener(DeleteListener l);
	public void addModifyListener(ModifyListener l);
	public void removeModifyListener(ModifyListener l);
	*/
	
	// ------------------------------------------------
	// Getters and Setters
	// ------------------------------------------------
	public void setSuffix(String suffixes[]);
	public ACLEngine getACLEngine();
	public void setACLEngine(ACLEngine aclEngine);
	public Collection getEngines();
	public PenroseConnectionPool getConnectionPool();
	public void setConnectionPool(PenroseConnectionPool connectionPool);
	public Logger getLog();
	public void setLog(Logger log);
	public List getNormalizedSuffixes();
	public void setNormalizedSuffixes(List normalizedSuffixes);
	public String getRootDn();
	public void setRootDn(String rootDn);
	public String getRootPassword();
	public void setRootPassword(String rootPassword);
	public boolean isStopRequested();
	public void setStopRequested(boolean stopRequested);
	public List getSuffixes();
	public void setSuffixes(List suffixes);
	public byte[] download(String filename) throws IOException;
	public void upload(String filename, byte content[]) throws IOException;
    public Collection listFiles(String directory) throws Exception;

}
