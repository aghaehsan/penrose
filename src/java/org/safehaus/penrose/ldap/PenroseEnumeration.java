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
package org.safehaus.penrose.ldap;

import org.safehaus.penrose.session.PenroseSearchResults;
import org.safehaus.penrose.util.ExceptionUtil;
import org.safehaus.penrose.util.ActiveDirectoryUtil;
import org.safehaus.penrose.util.BinaryUtil;
import org.ietf.ldap.LDAPEntry;
import org.ietf.ldap.LDAPAttributeSet;
import org.ietf.ldap.LDAPAttribute;
import org.ietf.ldap.LDAPException;
import org.apache.log4j.Logger;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.*;

/**
 * @author Endi S. Dewata
 */
public class PenroseEnumeration implements NamingEnumeration {

    Logger log = Logger.getLogger(getClass());

    public Hashtable environment;
    public PenroseSearchResults searchResults;

    public Collection binaryAttributes = new HashSet();

    public PenroseEnumeration(PenroseSearchResults searchResults) {
        this.searchResults = searchResults;
    }

    public PenroseEnumeration(Hashtable environment, PenroseSearchResults searchResults) {
        this.environment = environment;
        this.searchResults = searchResults;
/*
        log.debug("Environment:");
        for (Iterator i=environment.keySet().iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            log.debug(" - "+name+": "+environment.get(name));
        }
*/
        Collection c = (Collection)environment.get("java.naming.ldap.attributes.binary");
        if (c != null) {
            for (Iterator i=c.iterator(); i.hasNext(); ) {
                String name = (String)i.next();
                binaryAttributes.add(name.toLowerCase());
            }
        }
    }

    public void close() throws NamingException {
    }

    public boolean hasMore() throws NamingException {

        boolean hasNext = searchResults.hasNext();
        if (hasNext) return true;

        int rc = searchResults.getReturnCode();
        if (rc != LDAPException.SUCCESS) {
            ExceptionUtil.throwNamingException(rc, "RC: "+rc);
        }

        return false;
    }

    public Object next() throws NamingException {
        LDAPEntry result = (LDAPEntry)searchResults.next();

        LDAPAttributeSet attributeSet = result.getAttributeSet();
        Attributes attributes = new BasicAttributes();

        //log.debug("Entry "+result.getDN());
        for (Iterator j = attributeSet.iterator(); j.hasNext(); ) {
            LDAPAttribute attribute = (LDAPAttribute)j.next();
            String name = attribute.getName();
            Attribute attr = new BasicAttribute(name);

            if (binaryAttributes.contains(name.toLowerCase())) {
                for (Enumeration k=attribute.getByteValues(); k.hasMoreElements(); ) {
                    byte[] value = (byte[])k.nextElement();
                    attr.add(value);
                    //log.debug("- "+name+": binary");
                }

            } else {
                for (Enumeration k=attribute.getStringValues(); k.hasMoreElements(); ) {
                    String value = (String)k.nextElement();
                    attr.add(value);
                    //log.debug("- "+name+": "+value);
                }
            }

            attributes.put(attr);
        }

        SearchResult sr = new SearchResult(
                result.getDN(),
                result,
                attributes
        );

        return sr;
    }

    public boolean hasMoreElements() {
        try {
            return hasMore();
        } catch (Exception e) {
            return false;
        }
    }

    public Object nextElement() {
        try {
            return next();
        } catch (Exception e) {
            return null;
        }
    }
}