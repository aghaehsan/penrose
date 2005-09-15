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
package org.safehaus.penrose.connection;


import javax.naming.directory.*;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.*;
import java.util.*;

import org.ietf.ldap.LDAPException;
import org.safehaus.penrose.util.PasswordUtil;
import org.safehaus.penrose.filter.Filter;
import org.safehaus.penrose.mapping.*;
import org.safehaus.penrose.SearchResults;
import org.safehaus.penrose.config.Config;

/**
 * @author Endi S. Dewata
 */
public class JNDIAdapter extends Adapter {

    public final static String BASE_DN = "baseDn";
    public final static String SCOPE   = "scope";
    public final static String FILTER  = "filter";

    Hashtable parameters;

    public String url;
    public String suffix;

    public void init() throws Exception {
        String name = getConnectionName();

    	log.debug("-------------------------------------------------------------------------------");
    	log.debug("Initializing JNDI connection "+name+":");

        parameters = new Hashtable();
        for (Iterator i=getParameterNames().iterator(); i.hasNext(); ) {
            String param = (String)i.next();
            String value = getParameter(param);
            log.debug(param+": "+value);

            if (param.equals(Context.PROVIDER_URL)) {

                int index = value.indexOf("://");
                index = value.indexOf("/", index+3);
                if (index >= 0) { // extract suffix from url
                    suffix = value.substring(index+1);
                    url = value.substring(0, index);
                } else {
                    suffix = "";
                    url = value;
                }
                parameters.put(param, url);

            } else {
                parameters.put(param, value);
            }
        }

        parameters.put("com.sun.jndi.ldap.connect.pool", "true");

        //log.debug("URL: "+url);
        //log.debug("Suffix: "+suffix);
    }

    public SearchResults search(Source source, Filter filter, long sizeLimit) throws Exception {

        log.debug("JNDI Search:");

        SearchResults results = new SearchResults();

        //log.debug("--------------------------------------------------------------------------------------");
        log.debug("JNDI Source: "+source.getConnectionName());

        Config config = getAdapterContext().getConfig(source);
        ConnectionConfig connectionConfig = config.getConnectionConfig(source.getConnectionName());
        SourceDefinition sourceDefinition = connectionConfig.getSourceDefinition(source.getSourceName());

        String ldapBase = sourceDefinition.getParameter(BASE_DN);
        if ("".equals(ldapBase)) {
            ldapBase = suffix;
        } else {
            ldapBase = ldapBase+","+suffix;
        }

        String ldapScope = sourceDefinition.getParameter(SCOPE);
        String ldapFilter = sourceDefinition.getParameter(FILTER);

        if (filter != null) {
            ldapFilter = "(&"+ldapFilter+filter+")";
        }

        log.debug("base: "+ldapBase);
        log.debug("filter: "+ldapFilter);

        SearchControls ctls = new SearchControls();
        ctls.setReturningAttributes(new String[] { "dn" });
        if ("OBJECT".equals(ldapScope)) {
        	ctls.setSearchScope(SearchControls.OBJECT_SCOPE);
        	
        } else if ("ONELEVEL".equals(ldapScope)) {
        	ctls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
        	
        } else if ("SUBTREE".equals(ldapScope)) {
        	ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        }

        DirContext ctx = new InitialDirContext(parameters);
        NamingEnumeration ne = ctx.search(ldapBase, ldapFilter, ctls);

        log.debug("Result:");

        while (ne.hasMore()) {
            javax.naming.directory.SearchResult sr = (javax.naming.directory.SearchResult)ne.next();
            log.debug(" - "+sr.getName()+","+ldapBase);

            Row row = getPkValues(source, sr);
            results.add(row);
        }

        results.close();
        
        return results;
    }

    public SearchResults load(Source source, Filter filter, long sizeLimit) throws Exception {

        log.debug("JNDI Search:");

        SearchResults results = new SearchResults();

        //log.debug("--------------------------------------------------------------------------------------");
        log.debug("JNDI Source: "+source.getConnectionName());

        Config config = getAdapterContext().getConfig(source);
        ConnectionConfig connectionConfig = config.getConnectionConfig(source.getConnectionName());
        SourceDefinition sourceDefinition = connectionConfig.getSourceDefinition(source.getSourceName());

        String ldapBase = sourceDefinition.getParameter(BASE_DN);
        if ("".equals(ldapBase)) {
            ldapBase = suffix;
        } else {
            ldapBase = ldapBase+","+suffix;
        }

        String ldapScope = sourceDefinition.getParameter(SCOPE);
        String ldapFilter = sourceDefinition.getParameter(FILTER);

        if (filter != null) {
            ldapFilter = "(&"+ldapFilter+filter+")";
        }

        log.debug("base: "+ldapBase);
        log.debug("filter: "+ldapFilter);

        SearchControls ctls = new SearchControls();
        if ("OBJECT".equals(ldapScope)) {
        	ctls.setSearchScope(SearchControls.OBJECT_SCOPE);

        } else if ("ONELEVEL".equals(ldapScope)) {
        	ctls.setSearchScope(SearchControls.ONELEVEL_SCOPE);

        } else if ("SUBTREE".equals(ldapScope)) {
        	ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        }

        DirContext ctx = new InitialDirContext(parameters);
        NamingEnumeration ne = ctx.search(ldapBase, ldapFilter, ctls);

        log.debug("Result:");

        while (ne.hasMore()) {
            javax.naming.directory.SearchResult sr = (javax.naming.directory.SearchResult)ne.next();
            log.debug(" - "+sr.getName()+","+ldapBase);

            AttributeValues av = getValues(source, sr);
            results.add(av);
        }

        results.close();

        return results;
    }

    public Row getPkValues(Source source, javax.naming.directory.SearchResult sr) throws Exception {

        Config config = getAdapterContext().getConfig(source);
        ConnectionConfig connectionConfig = config.getConnectionConfig(source.getConnectionName());
        SourceDefinition sourceDefinition = connectionConfig.getSourceDefinition(source.getSourceName());

        Row row = new Row();

        Attributes attrs = sr.getAttributes();
        Collection fields = source.getFields();
        for (Iterator i=fields.iterator(); i.hasNext(); ) {
            Field field = (Field)i.next();
            String name = field.getName();
            if (name.equals("objectClass")) continue;

            FieldDefinition fieldDefinition = sourceDefinition.getFieldDefinition(name);
            if (!fieldDefinition.isPrimaryKey()) continue;

            javax.naming.directory.Attribute attr = attrs.get(fieldDefinition.getOriginalName());
            if (attr == null) continue;

            boolean binary = false;
            try {
                attr.getAttributeSyntaxDefinition();
            } catch (Exception e) {
                binary = "SyntaxDefinition/1.3.6.1.4.1.1466.115.121.1.40".equals(e.getMessage());
            }

            NamingEnumeration attributeValues = attr.getAll();
            while (attributeValues.hasMore()) {
                Object value = attributeValues.next();
                row.set(name, value);
            }
        }

        return row;
    }

    public AttributeValues getValues(Source source, javax.naming.directory.SearchResult sr) throws Exception {

        Config config = getAdapterContext().getConfig(source);
        ConnectionConfig connectionConfig = config.getConnectionConfig(source.getConnectionName());
        SourceDefinition sourceDefinition = connectionConfig.getSourceDefinition(source.getSourceName());

        AttributeValues av = new AttributeValues();

        Attributes attrs = sr.getAttributes();
        Collection fields = source.getFields();
        for (Iterator i=fields.iterator(); i.hasNext(); ) {
            Field field = (Field)i.next();
            String name = field.getName();
            if (name.equals("objectClass")) continue;

            FieldDefinition fieldDefinition = sourceDefinition.getFieldDefinition(name);

            javax.naming.directory.Attribute attr = attrs.get(fieldDefinition.getOriginalName());
            if (attr == null) continue;

            boolean binary = false;
            try {
                attr.getAttributeSyntaxDefinition();
            } catch (Exception e) {
                binary = "SyntaxDefinition/1.3.6.1.4.1.1466.115.121.1.40".equals(e.getMessage());
            }

            NamingEnumeration attributeValues = attr.getAll();
            while (attributeValues.hasMore()) {
                Object value = attributeValues.next();
                av.add(name, value);
            }
        }

        return av;
    }

    public int bind(Source source, AttributeValues values, String password) throws Exception {

        log.debug("JNDI Bind:");

        if (!values.contains("objectClass")) {
            return LDAPException.INVALID_CREDENTIALS;
        }

        String dn = getDn(source, values);
        log.debug("Binding as "+dn);

        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, getParameter(Context.INITIAL_CONTEXT_FACTORY));
        env.put(Context.PROVIDER_URL, url);
        env.put(Context.SECURITY_PRINCIPAL, dn);
        env.put(Context.SECURITY_CREDENTIALS, password);

        try {
            DirContext c = new InitialDirContext(env);
            c.close();
        } catch (AuthenticationException e) {
            log.debug("Error: "+e.getMessage());
            return LDAPException.INVALID_CREDENTIALS;
        }

        return LDAPException.SUCCESS;
    }

    public int add(Source source, AttributeValues entry) throws Exception {
        log.debug("JNDI Add:");

        //if (!entry.contains("objectClass")) {
        //    return modifyAdd(source, entry);
        //}

        String dn = getDn(source, entry);

        Attributes attrs = new BasicAttributes();

        for (Iterator i=entry.getNames().iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            Set set = (Set)entry.get(name);
            if (set.isEmpty()) continue;

            Attribute attr = new BasicAttribute(name);
            for (Iterator j=set.iterator(); j.hasNext(); ) {
                String v = (String)j.next();

                if ("unicodePwd".equals(name)) {
                    attr.add(PasswordUtil.toUnicodePassword(v));
                } else {
                    attr.add(v);
                }
                log.debug(" - "+name+": "+v);
            }
            attrs.put(attr);
        }

        log.debug("Adding "+dn);
        try {
            DirContext ctx = new InitialDirContext(parameters);
            ctx.createSubcontext(dn, attrs);
        } catch (NameAlreadyBoundException e) {
            return modifyAdd(source, entry);
            //log.debug("Error: "+e.getMessage());
            //return LDAPException.ENTRY_ALREADY_EXISTS;
        }

        return LDAPException.SUCCESS;
    }

    public int modifyDelete(Source source, AttributeValues entry) throws Exception {

        Config config = getAdapterContext().getConfig(source);
        ConnectionConfig connectionConfig = config.getConnectionConfig(source.getConnectionName());
        SourceDefinition sourceDefinition = connectionConfig.getSourceDefinition(source.getSourceName());

        log.debug("JNDI Modify Delete:");

        String dn = getDn(source, entry);
        log.debug("Deleting attributes in "+dn);

        List list = new ArrayList();
        Collection fields = source.getFields();

        for (Iterator i=entry.getNames().iterator(); i.hasNext(); ) {
            String name = (String)i.next();

            boolean primaryKey = false;
            for (Iterator j=fields.iterator(); j.hasNext(); ) {
                Field field = (Field)j.next();
                FieldDefinition fieldDefinition = sourceDefinition.getFieldDefinition(field.getName());
                if (!fieldDefinition.isPrimaryKey()) continue;
                if (!field.getName().equals(name)) continue;
                primaryKey = true;
                break;
            }

            if (primaryKey) continue; // don't delete primary key

            Set set = (Set)entry.get(name);
            Attribute attribute = new BasicAttribute(name);
            for (Iterator j = set.iterator(); j.hasNext(); ) {
                String value = (String)j.next();
                log.debug(" - "+name+": "+value);
                attribute.add(value);
            }
            list.add(new ModificationItem(DirContext.REMOVE_ATTRIBUTE, attribute));
        }

        ModificationItem mods[] = (ModificationItem[])list.toArray(new ModificationItem[list.size()]);

        DirContext ctx = new InitialDirContext(parameters);
        ctx.modifyAttributes(dn, mods);

        return LDAPException.SUCCESS;
    }

    public int delete(Source source, AttributeValues entry) throws Exception {
        log.debug("JNDI Delete:");

        if (!entry.contains("objectClass")) {
            return modifyDelete(source, entry);
        }

        String dn = getDn(source, entry);
        log.debug("Deleting entry "+dn);

        try {
            DirContext ctx = new InitialDirContext(parameters);
            ctx.destroySubcontext(dn);
            
        } catch (NameNotFoundException e) {
            return LDAPException.NO_SUCH_OBJECT;
        }

        return LDAPException.SUCCESS;
    }

    public int modify(Source source, AttributeValues oldEntry, AttributeValues newEntry) throws Exception {
        log.debug("JNDI Modify:");

        Config config = getAdapterContext().getConfig(source);
        ConnectionConfig connectionConfig = config.getConnectionConfig(source.getConnectionName());
        SourceDefinition sourceDefinition = connectionConfig.getSourceDefinition(source.getSourceName());

        String dn = getDn(source, newEntry);
        log.debug("Replacing attributes "+dn);

        List list = new ArrayList();
        Collection fields = source.getFields();

        Set addAttributes = new HashSet(newEntry.getNames());
        addAttributes.removeAll(oldEntry.getNames());
        log.debug("Attributes to add: " + addAttributes);

        Set removeAttributes = new HashSet(oldEntry.getNames());
        removeAttributes.removeAll(newEntry.getNames());
        log.debug("Attributes to remove: " + removeAttributes);

        Set replaceAttributes = new HashSet(oldEntry.getNames());
        replaceAttributes.retainAll(newEntry.getNames());
        log.debug("Attributes to replace: " + replaceAttributes);

        for (Iterator i=addAttributes.iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            if ("objectClass".equals(name)) continue; // don't add objectClass

            boolean primaryKey = false;
            for (Iterator j=fields.iterator(); j.hasNext(); ) {
                Field field = (Field)j.next();
                FieldDefinition fieldDefinition = sourceDefinition.getFieldDefinition(field.getName());
                if (!fieldDefinition.isPrimaryKey()) continue;
                if (!field.getName().equals(name)) continue;
                primaryKey = true;
                break;
            }

            if (primaryKey) continue; // don't add primary key

            Set set = (Set)newEntry.get(name);
            Attribute attribute = new BasicAttribute(name);
            for (Iterator j = set.iterator(); j.hasNext(); ) {
                String value = (String)j.next();
                log.debug(" - add "+name+": "+value);

                if ("unicodePwd".equals(name)) { // need to encode unicodePwd
                    attribute.add(PasswordUtil.toUnicodePassword(value));
                } else {
                    attribute.add(value);
                }

            }

            list.add(new ModificationItem(DirContext.ADD_ATTRIBUTE, attribute));
        }

        for (Iterator i=removeAttributes.iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            if ("objectClass".equals(name)) continue; // don't remove objectClass
            if ("unicodePwd".equals(name)) continue; // can't remove unicodePwd

            boolean primaryKey = false;
            for (Iterator j=fields.iterator(); j.hasNext(); ) {
                Field field = (Field)j.next();
                FieldDefinition fieldDefinition = sourceDefinition.getFieldDefinition(field.getName());
                if (!fieldDefinition.isPrimaryKey()) continue;
                if (!field.getName().equals(name)) continue;
                primaryKey = true;
                break;
            }

            if (primaryKey) continue; // don't remove primary key

            Set set = (Set)newEntry.get(name);
            Attribute attribute = new BasicAttribute(name);
            for (Iterator j = set.iterator(); j.hasNext(); ) {
                String value = (String)j.next();
            	log.debug(" - remove "+name+": "+value);

                attribute.add(value);
            }

            list.add(new ModificationItem(DirContext.REMOVE_ATTRIBUTE, attribute));
        }

        for (Iterator i=replaceAttributes.iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            if ("objectClass".equals(name)) continue; // don't replace objectClass

            boolean primaryKey = false;
            for (Iterator j=fields.iterator(); j.hasNext(); ) {
                Field field = (Field)j.next();
                FieldDefinition fieldDefinition = sourceDefinition.getFieldDefinition(field.getName());
                if (!fieldDefinition.isPrimaryKey()) continue;
                if (!field.getName().equals(name)) continue;
                primaryKey = true;
                break;
            }

            if (primaryKey) continue; // don't replace primary key

            Set set = (Set)newEntry.get(name);
            Attribute attribute = new BasicAttribute(name);
            for (Iterator j = set.iterator(); j.hasNext(); ) {
                String value = (String)j.next();
                log.debug(" - replace "+name+": "+value);

                if ("unicodePwd".equals(name)) { // need to encode unicodePwd
                    attribute.add(PasswordUtil.toUnicodePassword(value));
                } else {
                    attribute.add(value);
                }

            }

            list.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, attribute));
        }

        ModificationItem mods[] = (ModificationItem[])list.toArray(new ModificationItem[list.size()]);

        DirContext ctx = new InitialDirContext(parameters);
        ctx.modifyAttributes(dn, mods);

        return LDAPException.SUCCESS;
    }

    public int modifyAdd(Source source, AttributeValues entry) throws Exception {
        log.debug("JNDI Modify Add:");

        Config config = getAdapterContext().getConfig(source);
        ConnectionConfig connectionConfig = config.getConnectionConfig(source.getConnectionName());
        SourceDefinition sourceDefinition = connectionConfig.getSourceDefinition(source.getSourceName());

        String dn = getDn(source, entry);
        log.debug("Replacing attributes "+dn);

        Collection fields = source.getFields();
        List list = new ArrayList();

        for (Iterator i=entry.getNames().iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            Set set = (Set)entry.get(name);
            if (set.isEmpty()) continue;

            boolean primaryKey = false;
            for (Iterator j=fields.iterator(); j.hasNext(); ) {
                Field field = (Field)j.next();
                FieldDefinition fieldDefinition = sourceDefinition.getFieldDefinition(field.getName());
                if (!fieldDefinition.isPrimaryKey()) continue;
                if (!field.getName().equals(name)) continue;
                primaryKey = true;
                break;
            }

            if (primaryKey) continue; // don't add primary key

            Attribute attribute = new BasicAttribute(name);
            for (Iterator j = set.iterator(); j.hasNext(); ) {
                String v = (String)j.next();
                if ("unicodePwd".equals(name)) {
                    attribute.add(PasswordUtil.toUnicodePassword(v));
                } else {
                    attribute.add(v);
                }
                log.debug(" - "+name+": "+v);
            }
            list.add(new ModificationItem(DirContext.ADD_ATTRIBUTE, attribute));
        }

        log.debug("Updating "+dn);

        ModificationItem mods[] = (ModificationItem[])list.toArray(new ModificationItem[list.size()]);
        DirContext ctx = new InitialDirContext(parameters);
        ctx.modifyAttributes(dn, mods);

        return LDAPException.SUCCESS;
    }

    public String getDn(Source source, AttributeValues columnValues) throws Exception {

        Config config = getAdapterContext().getConfig(source);
        ConnectionConfig connectionConfig = config.getConnectionConfig(source.getConnectionName());
        SourceDefinition sourceDefinition = connectionConfig.getSourceDefinition(source.getSourceName());

        String baseDn = sourceDefinition.getParameter(BASE_DN);

        Collection fields= source.getFields();
        StringBuffer sb = new StringBuffer();

        for (Iterator i=fields.iterator(); i.hasNext(); ) {
            Field field = (Field)i.next();

            FieldDefinition fieldDefinition = sourceDefinition.getFieldDefinition(field.getName());
            if (!fieldDefinition.isPrimaryKey()) continue;

            if (sb.length() > 0) sb.append("+");

            String name = field.getName();
            sb.append(name);
            sb.append("=");

            Object value = columnValues.get(name);
            if (value instanceof Collection) {
                Collection c = (Collection)value;
                value = c.iterator().next();
            }

            sb.append(value);
        }

        if ("".equals(baseDn)) {
            baseDn = suffix;
        } else {
            baseDn = baseDn+","+suffix;
        }

        return sb+","+baseDn;
    }
}
