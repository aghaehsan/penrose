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
package org.safehaus.penrose.acl;

import org.safehaus.penrose.mapping.Entry;
import org.safehaus.penrose.mapping.EntryMapping;
import org.safehaus.penrose.session.PenroseSession;
import org.safehaus.penrose.session.PenroseSession;
import org.safehaus.penrose.partition.Partition;
import org.safehaus.penrose.handler.SessionHandler;
import org.ietf.ldap.LDAPException;
import org.ietf.ldap.LDAPEntry;
import org.ietf.ldap.LDAPAttributeSet;
import org.ietf.ldap.LDAPAttribute;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * @author Endi S. Dewata
 */
public class ACLEngine {

    public Logger log = Logger.getLogger(getClass());

    public SessionHandler sessionHandler;

    public ACLEngine(SessionHandler sessionHandler) {
        this.sessionHandler = sessionHandler;
    }

    public void addPermission(Set set, String permission) {
        for (int i=0; i<permission.length(); i++) {
            set.add(permission.substring(i, i+1));
        }
    }

    public void addPermission(ACI aci, Set grants, Set denies) {
        if (aci.getAction().equals(ACI.ACTION_GRANT)) {
            addPermission(grants, aci.getPermission());

        } else if (aci.getAction().equals(ACI.ACTION_DENY)) {
            addPermission(denies, aci.getPermission());
        }
    }

    public boolean getObjectPermission(
            String bindDn,
            String targetDn,
            EntryMapping entry,
            String scope,
            String permission) throws Exception {

        //log.debug(" * "+entry.getDn()+":");

        for (Iterator i=entry.getACL().iterator(); i.hasNext(); ) {
            ACI aci = (ACI)i.next();

            if (!aci.getTarget().equals(ACI.TARGET_OBJECT)) continue;
            if (scope != null && !scope.equals(aci.getScope())) continue;
            if (aci.getPermission().indexOf(permission) < 0) continue;

            //log.debug("   - "+aci);
            String subject = sessionHandler.getSchemaManager().normalize(aci.getSubject());

            if (subject.equals(ACI.SUBJECT_USER) && aci.getDn().equals(bindDn)) {
                return aci.getAction().equals(ACI.ACTION_GRANT);

            } else if (subject.equals(ACI.SUBJECT_SELF) && targetDn.equals(bindDn)) {
                return aci.getAction().equals(ACI.ACTION_GRANT);

            } else if (subject.equals(ACI.SUBJECT_ANONYMOUS) && (bindDn == null || bindDn.equals(""))) {
                return aci.getAction().equals(ACI.ACTION_GRANT);

            } else if (subject.equals(ACI.SUBJECT_AUTHENTICATED) && bindDn != null && !bindDn.equals("")) {
                return aci.getAction().equals(ACI.ACTION_GRANT);

            } else if (subject.equals(ACI.SUBJECT_ANYBODY)) {
                return aci.getAction().equals(ACI.ACTION_GRANT);

            }
        }

        Partition partition = sessionHandler.getPartitionManager().getPartitionByDn(entry.getDn());
        if (partition == null) return false;

        entry = partition.getParent(entry);
        if (entry == null) return false;

        return getObjectPermission(bindDn, targetDn, entry, ACI.SCOPE_SUBTREE, permission);
    }

    public boolean getObjectPermission(String bindDn, String targetDn, Entry entry, String target, String permission) throws Exception {

        return getObjectPermission(bindDn, targetDn, entry.getEntryMapping(), null, permission);
    }

    public int checkPermission(PenroseSession session, Entry entry, String permission) throws Exception {
    	
        //log.debug("Evaluating object \""+permission+"\" permission for "+entry.getDn()+(connection == null ? null : " as "+connection.getBindDn()));

        int rc = LDAPException.SUCCESS;
        try {
            if (session == null) {
                return rc;
            }

            String rootDn = sessionHandler.getSchemaManager().normalize(sessionHandler.getRootUserConfig().getDn());
            String bindDn = sessionHandler.getSchemaManager().normalize(session.getBindDn());
            if (rootDn != null && rootDn.equals(bindDn)) {
                return rc;
            }

            String targetDn = sessionHandler.getSchemaManager().normalize(entry.getDn());
            boolean result = getObjectPermission(bindDn, targetDn, entry, ACI.SCOPE_OBJECT, permission);
            //log.debug("Result: "+result);

            if (result) {
                return rc;
            }

            rc = LDAPException.INSUFFICIENT_ACCESS_RIGHTS;
            return rc;

        } finally {
            //log.debug("Result: "+rc);
        }
    }

    public int checkRead(PenroseSession session, Entry entry) throws Exception {
    	return checkPermission(session, entry, ACI.PERMISSION_READ);
    }

    public int checkSearch(PenroseSession session, Entry entry) throws Exception {
    	return checkPermission(session, entry, ACI.PERMISSION_SEARCH);
    }

    public int checkAdd(PenroseSession session, Entry entry) throws Exception {
    	return checkPermission(session, entry, ACI.PERMISSION_ADD);
    }

    public int checkDelete(PenroseSession session, Entry entry) throws Exception {
    	return checkPermission(session, entry, ACI.PERMISSION_DELETE);
    }

    public int checkModify(PenroseSession session, Entry entry) throws Exception {
    	return checkPermission(session, entry, ACI.PERMISSION_WRITE);
    }

    public void addAttributes(Set set, String attributes) {
        //log.debug("Adding attributes: "+attributes);
        StringTokenizer st = new StringTokenizer(attributes, ",");
        while (st.hasMoreTokens()) {
            String attributeName = st.nextToken().trim();
            set.add(attributeName);
            //log.debug("Adding attribute: "+attributeName);
        }
    }

    public void addAttributes(ACI aci, Set grants, Set denies) {
        if (aci.getAction().equals(ACI.ACTION_GRANT)) {
            addAttributes(grants, aci.getAttributes());

        } else if (aci.getAction().equals(ACI.ACTION_DENY)) {
            addAttributes(denies, aci.getAttributes());
        }
    }

    public void getReadableAttributes(
            String bindDn,
            String targetDn,
            EntryMapping entry,
            String scope,
            Set grants,
            Set denies) throws Exception {

        //log.debug(" * "+entry.getDn()+":");

        for (Iterator i=entry.getACL().iterator(); i.hasNext(); ) {
            ACI aci = (ACI)i.next();

            if (!aci.getTarget().equals(ACI.TARGET_ATTRIBUTES)) continue;
            if (scope != null && !scope.equals(aci.getScope())) continue;
            if (aci.getPermission().indexOf(ACI.PERMISSION_READ) < 0) continue;

            //log.debug("   - "+aci);
            String subject = sessionHandler.getSchemaManager().normalize(aci.getSubject());

            if (subject.equals(bindDn)) {
                addAttributes(aci, grants, denies);

            } else if (subject.equals(ACI.SUBJECT_SELF) && targetDn.equals(bindDn)) {
                addAttributes(aci, grants, denies);

            } else if (subject.equals(ACI.SUBJECT_ANONYMOUS) && (bindDn == null || bindDn.equals(""))) {
                addAttributes(aci, grants, denies);

            } else if (subject.equals(ACI.SUBJECT_AUTHENTICATED) && bindDn != null && !bindDn.equals("")) {
                addAttributes(aci, grants, denies);

            } else if (subject.equals(ACI.SUBJECT_ANYBODY)) {
                addAttributes(aci, grants, denies);

            }
        }

        Partition partition = sessionHandler.getPartitionManager().getPartitionByDn(entry.getDn());
        if (partition == null) return;

        entry = partition.getParent(entry);
        if (entry == null) return;

        getReadableAttributes(bindDn, targetDn, entry, ACI.SCOPE_SUBTREE, grants, denies);
    }

    public void getReadableAttributes(
            String bindDn,
            Entry entry,
            Set grants,
            Set denies
            ) throws Exception {

        String rootDn = sessionHandler.getSchemaManager().normalize(sessionHandler.getRootUserConfig().getDn());
    	if (rootDn.equals(bindDn)) {
            grants.add("*");
            return;
        }

        String targetDn = sessionHandler.getSchemaManager().normalize(entry.getDn());

        getReadableAttributes(bindDn, targetDn, entry.getEntryMapping(), null, grants, denies);

        grants.removeAll(denies);
        denies.removeAll(grants);
        
        if (denies.contains("*")) {
            grants.clear();
            denies.clear();
            denies.add("*");
        }
    }

    public LDAPEntry filterAttributes(
            PenroseSession session,
            Entry entry,
            LDAPEntry ldapEntry)
            throws Exception {

        String bindDn = sessionHandler.getSchemaManager().normalize(session.getBindDn());

        Set grants = new HashSet();
        Set denies = new HashSet();

        //log.debug("Evaluating attributes read permission for "+bindDn);

        getReadableAttributes(bindDn, entry, grants, denies);

        //log.debug("Readable attributes: "+grants);
        //log.debug("Unreadable attributes: "+denies);

        LDAPAttributeSet attributeSet = ldapEntry.getAttributeSet();

        Collection list = new ArrayList();
        for (Iterator i=attributeSet.iterator(); i.hasNext(); ) {
            LDAPAttribute attribute = (LDAPAttribute)i.next();
            if (denies.contains("*") || denies.contains(attribute.getName())) list.add(attribute);
            if (grants.contains("*") || grants.contains(attribute.getName())) continue;
            list.add(attribute);
        }
        attributeSet.removeAll(list);

        return ldapEntry;
    }

}
