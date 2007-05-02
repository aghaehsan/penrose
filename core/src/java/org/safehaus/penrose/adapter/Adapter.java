/**
 * Copyright (c) 2000-2006, Identyx Corporation.
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
package org.safehaus.penrose.adapter;

import org.safehaus.penrose.mapping.*;
import org.safehaus.penrose.filter.Filter;
import org.safehaus.penrose.filter.SubstringFilter;
import org.safehaus.penrose.partition.Partition;
import org.safehaus.penrose.util.ExceptionUtil;
import org.safehaus.penrose.entry.SourceValues;
import org.safehaus.penrose.connection.Connection;
import org.safehaus.penrose.config.PenroseConfig;
import org.safehaus.penrose.naming.PenroseContext;
import org.safehaus.penrose.source.*;
import org.safehaus.penrose.ldap.*;
import org.safehaus.penrose.interpreter.Interpreter;
import org.safehaus.penrose.adapter.FilterBuilder;
import org.ietf.ldap.LDAPException;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Map;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * @author Endi S. Dewata 
 */
public abstract class Adapter {

    public Logger log = LoggerFactory.getLogger(getClass());

    protected PenroseConfig penroseConfig;
    protected PenroseContext penroseContext;

    protected Partition partition;
    protected AdapterConfig adapterConfig;
    protected Connection connection;

    /**
     * Initialize.
     *
     * @throws Exception
     */
    public void init() throws Exception {
    }

    public void start() throws Exception {
    }

    public void stop() throws Exception {
    }

    public void dispose() throws Exception {
    }

    public boolean isJoinSupported() {
        return false;
    }

    public String getSyncClassName() {
        return SourceSync.class.getName();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Table
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void create(Source source) throws Exception {
        throw ExceptionUtil.createLDAPException(LDAPException.OPERATIONS_ERROR);
    }

    public void rename(Source oldSource, Source newSource) throws Exception {
        throw ExceptionUtil.createLDAPException(LDAPException.OPERATIONS_ERROR);
    }

    public void drop(Source source) throws Exception {
        throw ExceptionUtil.createLDAPException(LDAPException.OPERATIONS_ERROR);
    }

    public void clean(Source source) throws Exception {
        throw ExceptionUtil.createLDAPException(LDAPException.OPERATIONS_ERROR);
    }

    public void status(Source source) throws Exception {
        throw ExceptionUtil.createLDAPException(LDAPException.OPERATIONS_ERROR);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Add
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void add(
            Source source,
            AddRequest request,
            AddResponse response
    ) throws Exception {
        throw ExceptionUtil.createLDAPException(LDAPException.OPERATIONS_ERROR);
    }

    public void add(
            EntryMapping entryMapping,
            Collection sourceRefs,
            SourceValues sourceValues,
            AddRequest request,
            AddResponse response
    ) throws Exception {

        SourceRef sourceRef = (SourceRef) sourceRefs.iterator().next();
        Source source = sourceRef.getSource();

        Interpreter interpreter = penroseContext.getInterpreterManager().newInstance();
        interpreter.set(sourceValues);

        RDN rdn = request.getDn().getRdn();
        for (Iterator i=rdn.getNames().iterator(); i.hasNext(); ) {
            String attributeName = (String)i.next();
            Object attributeValue = rdn.get(attributeName);

            interpreter.set(attributeName, attributeValue);
        }

        Attributes attributes = request.getAttributes();
        for (Iterator i=attributes.getAll().iterator(); i.hasNext(); ) {
            Attribute attribute = (Attribute)i.next();

            String attributeName = attribute.getName();
            Object attributeValue = attribute.getValue(); // use only the first value

            interpreter.set(attributeName, attributeValue);
        }

        Attributes newAttributes = new Attributes();
        RDNBuilder rb = new RDNBuilder();

        for (Iterator i= sourceRef.getFieldRefs().iterator(); i.hasNext(); ) {
            FieldRef fieldRef = (FieldRef)i.next();

            FieldMapping fieldMapping = fieldRef.getFieldMapping();
            Object value = interpreter.eval(fieldMapping);
            if (value == null) continue;

            Field field = fieldRef.getField();
            newAttributes.addValue(field.getOriginalName(), value);

            if (field.isPrimaryKey()) {
                rb.set(field.getOriginalName(), value);
            }
        }

        AddRequest newRequest = new AddRequest(request);
        newRequest.setDn(new DN(rb.toRdn()));
        newRequest.setAttributes(newAttributes);

        add(source, newRequest, response);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Bind
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void bind(
            Source source,
            BindRequest request,
            BindResponse response
    ) throws Exception {
        throw ExceptionUtil.createLDAPException(LDAPException.INVALID_CREDENTIALS);
    }

    public void bind(
            EntryMapping entryMapping,
            Collection sourceRefs,
            SourceValues sourceValues,
            BindRequest request,
            BindResponse response
    ) throws Exception {

        SourceRef sourceRef = (SourceRef) sourceRefs.iterator().next();
        Source source = sourceRef.getSource();

        Interpreter interpreter = penroseContext.getInterpreterManager().newInstance();
        interpreter.set(sourceValues);

        RDN rdn = request.getDn().getRdn();
        for (Iterator i=rdn.getNames().iterator(); i.hasNext(); ) {
            String attributeName = (String)i.next();
            Object attributeValue = rdn.get(attributeName);

            interpreter.set(attributeName, attributeValue);
        }

        RDNBuilder rb = new RDNBuilder();

        for (Iterator i= sourceRef.getFieldRefs().iterator(); i.hasNext(); ) {
            FieldRef fieldRef = (FieldRef)i.next();
            if (!fieldRef.isPrimaryKey()) continue;

            FieldMapping fieldMapping = fieldRef.getFieldMapping();
            Object value = interpreter.eval(fieldMapping);
            if (value == null) continue;

            Field field = fieldRef.getField();
            rb.set(field.getOriginalName(), value);
        }

        BindRequest newRequest = new BindRequest(request);
        newRequest.setDn(new DN(rb.toRdn()));

        bind(source, newRequest, response);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Delete
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void delete(
            Source source,
            DeleteRequest request,
            DeleteResponse response
    ) throws Exception {
        throw ExceptionUtil.createLDAPException(LDAPException.OPERATIONS_ERROR);
    }

    public void delete(
            EntryMapping entryMapping,
            Collection sourceRefs,
            SourceValues sourceValues,
            DeleteRequest request,
            DeleteResponse response
    ) throws Exception {

        SourceRef sourceRef = (SourceRef) sourceRefs.iterator().next();
        Source source = sourceRef.getSource();

        Interpreter interpreter = penroseContext.getInterpreterManager().newInstance();
        interpreter.set(sourceValues);

        RDN rdn = request.getDn().getRdn();
        for (Iterator i=rdn.getNames().iterator(); i.hasNext(); ) {
            String attributeName = (String)i.next();
            Object attributeValue = rdn.get(attributeName);

            interpreter.set(attributeName, attributeValue);
        }

        RDNBuilder rb = new RDNBuilder();

        for (Iterator i= sourceRef.getFieldRefs().iterator(); i.hasNext(); ) {
            FieldRef fieldRef = (FieldRef)i.next();
            if (!fieldRef.isPrimaryKey()) continue;

            FieldMapping fieldMapping = fieldRef.getFieldMapping();
            Object value = interpreter.eval(fieldMapping);
            if (value == null) continue;

            Field field = fieldRef.getField();
            rb.set(field.getOriginalName(), value);
        }

        DeleteRequest newRequest = new DeleteRequest(request);
        newRequest.setDn(new DN(rb.toRdn()));


        delete(source, newRequest, response);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Modify
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void modify(
            Source source,
            ModifyRequest request,
            ModifyResponse response
    ) throws Exception {
        throw ExceptionUtil.createLDAPException(LDAPException.OPERATIONS_ERROR);
    }

    public void modify(
            EntryMapping entryMapping,
            Collection sourceRefs,
            SourceValues sourceValues,
            ModifyRequest request,
            ModifyResponse response
    ) throws Exception {

        boolean debug = log.isDebugEnabled();

        SourceRef sourceRef = (SourceRef) sourceRefs.iterator().next();
        Source source = sourceRef.getSource();

        Interpreter interpreter = penroseContext.getInterpreterManager().newInstance();
        interpreter.set(sourceValues);

        RDN rdn = request.getDn().getRdn();
        for (Iterator i=rdn.getNames().iterator(); i.hasNext(); ) {
            String attributeName = (String)i.next();
            Object attributeValue = rdn.get(attributeName);

            interpreter.set(attributeName, attributeValue);
        }

        RDNBuilder rb = new RDNBuilder();

        for (Iterator i= sourceRef.getFieldRefs().iterator(); i.hasNext(); ) {
            FieldRef fieldRef = (FieldRef)i.next();
            if (!fieldRef.isPrimaryKey()) continue;

            FieldMapping fieldMapping = fieldRef.getFieldMapping();
            Object value = interpreter.eval(fieldMapping);
            if (value == null) continue;

            Field field = fieldRef.getField();
            rb.set(field.getOriginalName(), value);
        }

        DN newDn = new DN(rb.toRdn());

        Collection<Modification> newModifications = new ArrayList<Modification>();

        Collection<Modification> modifications = request.getModifications();
        for (Iterator i=modifications.iterator(); i.hasNext(); ) {
            Modification modification = (Modification)i.next();

            int type = modification.getType();
            Attribute attribute = modification.getAttribute();

            String attributeName = attribute.getName();
            Collection attributeValues = attribute.getValues();

            if (debug) {
                switch (type) {
                    case Modification.ADD:
                        log.debug("Adding attribute "+attributeName+": "+attributeValues);
                        break;
                    case Modification.REPLACE:
                        log.debug("Replacing attribute "+attributeName+": "+attributeValues);
                        break;
                    case Modification.DELETE:
                        log.debug("Deleting attribute "+attributeName+": "+attributeValues);
                        break;
                }
            }

            interpreter.clear();
            interpreter.set(sourceValues);
            interpreter.set(attributeName, attributeValues);

            switch (type) {
                case Modification.ADD:
                case Modification.REPLACE:
                    for (Iterator j= sourceRef.getFieldRefs().iterator(); j.hasNext(); ) {
                        FieldRef fieldRef = (FieldRef)j.next();
                        FieldMapping fieldMapping = fieldRef.getFieldMapping();
                        String fieldName = fieldRef.getName();
                        if (fieldRef.isPrimaryKey()) continue;

                        Object value = interpreter.eval(fieldMapping);
                        if (value == null) continue;

                        if (debug) log.debug("Setting field "+fieldName+" to "+value);

                        Attribute newAttribute = new Attribute(fieldRef.getOriginalName());
                        if (value instanceof Collection) {
                            for (Iterator k=((Collection)value).iterator(); k.hasNext(); ) {
                                Object v = k.next();
                                newAttribute.addValue(v);
                            }
                        } else {
                            newAttribute.addValue(value);
                        }
                        newModifications.add(new Modification(type, newAttribute));
                    }
                    break;

                case Modification.DELETE:
                    for (Iterator j= sourceRef.getFieldRefs().iterator(); j.hasNext(); ) {
                        FieldRef fieldRef = (FieldRef)j.next();
                        FieldMapping fieldMapping = fieldRef.getFieldMapping();

                        String fieldName = fieldRef.getName();

                        String variable = fieldMapping.getVariable();
                        if (variable == null) {
                            Object value = interpreter.eval(fieldMapping);
                            if (value == null) continue;

                            if (debug) log.debug("Setting field "+fieldName+" to null");

                            Attribute newAttribute = new Attribute(fieldRef.getOriginalName());
                            newAttribute.addValue(value);
                            newModifications.add(new Modification(type, newAttribute));

                        } else {
                            if (!variable.equals(attributeName)) continue;

                            Attribute newAttribute = new Attribute(fieldRef.getOriginalName());
                            for (Iterator k=attributeValues.iterator(); k.hasNext(); ) {
                                Object value = k.next();
                                newAttribute.addValue(value);
                            }
                            newModifications.add(new Modification(type, newAttribute));
                        }

                    }
                    break;
            }
        }

        ModifyRequest newRequest = new ModifyRequest();
        newRequest.setDn(newDn);
        newRequest.setModifications(newModifications);

        modify(source, newRequest, response);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // ModRDN
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void modrdn(
            Source source,
            ModRdnRequest request,
            ModRdnResponse response
    ) throws Exception {
        throw ExceptionUtil.createLDAPException(LDAPException.OPERATIONS_ERROR);
    }

    public void modrdn(
            EntryMapping entryMapping,
            Collection sourceRefs,
            SourceValues sourceValues,
            ModRdnRequest request,
            ModRdnResponse response
    ) throws Exception {

        SourceRef sourceRef = (SourceRef) sourceRefs.iterator().next();
        Source source = sourceRef.getSource();

        Interpreter interpreter = penroseContext.getInterpreterManager().newInstance();
        interpreter.set(sourceValues);

        RDN rdn = request.getDn().getRdn();
        for (Iterator i=rdn.getNames().iterator(); i.hasNext(); ) {
            String attributeName = (String)i.next();
            Object attributeValue = rdn.get(attributeName);

            interpreter.set(attributeName, attributeValue);
        }

        RDNBuilder rb = new RDNBuilder();

        for (Iterator k= sourceRef.getFieldRefs().iterator(); k.hasNext(); ) {
            FieldRef fieldRef = (FieldRef)k.next();
            if (!fieldRef.isPrimaryKey()) continue;

            FieldMapping fieldMapping = fieldRef.getFieldMapping();
            Object value = interpreter.eval(fieldMapping);
            if (value == null) continue;

            Field field = fieldRef.getField();
            rb.set(field.getOriginalName(), value);
        }

        DN newDn = new DN(rb.toRdn());

        interpreter.clear();
        interpreter.set(sourceValues);

        RDN newRdn = request.getNewRdn();
        for (Iterator i=newRdn.getNames().iterator(); i.hasNext(); ) {
            String attributeName = (String)i.next();
            Object attributeValue = newRdn.get(attributeName);

            interpreter.set(attributeName, attributeValue);
        }

        rb.clear();

        for (Iterator k= sourceRef.getFieldRefs().iterator(); k.hasNext(); ) {
            FieldRef fieldRef = (FieldRef)k.next();
            if (!fieldRef.isPrimaryKey()) continue;

            FieldMapping fieldMapping = fieldRef.getFieldMapping();
            Object value = interpreter.eval(fieldMapping);
            if (value == null) continue;

            Field field = fieldRef.getField();
            rb.set(field.getOriginalName(), value);
        }

        ModRdnRequest newRequest = new ModRdnRequest(request);
        newRequest.setDn(newDn);
        newRequest.setNewRdn(rb.toRdn());

        modrdn(source, newRequest, response);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Search
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void search(
            Source source,
            SearchRequest request,
            SearchResponse<SearchResult> response
    ) throws Exception {
        throw ExceptionUtil.createLDAPException(LDAPException.OPERATIONS_ERROR);
    }

    public void search(
            final EntryMapping entryMapping,
            final Collection sourceRefs,
            final SourceValues sourceValues,
            final SearchRequest request,
            final SearchResponse<SearchResult> response
    ) throws Exception {

        boolean debug = log.isDebugEnabled();

        final SourceRef sourceRef = (SourceRef) sourceRefs.iterator().next();
        Source source = sourceRef.getSource();

        Interpreter interpreter = penroseContext.getInterpreterManager().newInstance();

        FilterBuilder filterBuilder = new FilterBuilder(
                partition,
                entryMapping,
                sourceRefs,
                sourceValues,
                interpreter
        );

        Filter filter = filterBuilder.getFilter();
        if (debug) log.debug("Base filter: "+filter);

        filterBuilder.append(request.getFilter());
        filter = filterBuilder.getFilter();
        if (debug) log.debug("Added search filter: "+filter);

        SearchRequest newRequest = new SearchRequest();
        newRequest.setFilter(filter);

        SearchResponse<SearchResult> newResponse = new SearchResponse<SearchResult>() {
            public void add(SearchResult result) throws Exception {

                SearchResult searchResult = new SearchResult();
                searchResult.setDn(result.getDn());
                searchResult.setEntryMapping(entryMapping);
                searchResult.setSourceAttributes(sourceRef.getAlias(), result.getAttributes());

                response.add(searchResult);
            }
            public void close() throws Exception {
                response.close();
            }
        };

        search(source, newRequest, newResponse);
    }

    public Object openConnection() throws Exception {
        return null;
    }

    public AdapterConfig getAdapterConfig() {
        return adapterConfig;
    }

    public void setAdapterConfig(AdapterConfig adapterConfig) {
        this.adapterConfig = adapterConfig;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public String getParameter(String name) {
        return connection.getParameter(name);
    }

    public Map getParameters() {
        return connection.getParameters();
    }

    public Collection getParameterNames() {
        return connection.getParameterNames();
    }

    public String removeParameter(String name) {
        return connection.removeParameter(name);
    }

    public String getAdapterName() {
        return adapterConfig.getName();
    }

    public String getConnectionName() {
        return connection.getConnectionName();
    }

    public Filter convert(EntryMapping entryMapping, SubstringFilter filter) throws Exception {
        return filter;
    }

    public PenroseConfig getPenroseConfig() {
        return penroseConfig;
    }

    public void setPenroseConfig(PenroseConfig penroseConfig) {
        this.penroseConfig = penroseConfig;
    }

    public PenroseContext getPenroseContext() {
        return penroseContext;
    }

    public void setPenroseContext(PenroseContext penroseContext) {
        this.penroseContext = penroseContext;
    }

    public Partition getPartition() {
        return partition;
    }

    public void setPartition(Partition partition) {
        this.partition = partition;
    }
}
