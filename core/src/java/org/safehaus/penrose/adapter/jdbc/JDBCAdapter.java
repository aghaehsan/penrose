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
package org.safehaus.penrose.adapter.jdbc;

import org.safehaus.penrose.util.Formatter;
import org.safehaus.penrose.filter.Filter;
import org.safehaus.penrose.filter.SubstringFilter;
import org.safehaus.penrose.filter.SimpleFilter;
import org.safehaus.penrose.filter.FilterTool;
import org.safehaus.penrose.mapping.*;
import org.safehaus.penrose.partition.FieldConfig;
import org.safehaus.penrose.partition.SourceConfig;
import org.safehaus.penrose.entry.*;
import org.safehaus.penrose.adapter.Adapter;
import org.safehaus.penrose.jdbc.*;
import org.safehaus.penrose.source.SourceRef;
import org.safehaus.penrose.source.FieldRef;
import org.safehaus.penrose.source.Source;
import org.safehaus.penrose.source.Field;
import org.safehaus.penrose.ldap.*;

import java.sql.ResultSet;
import java.util.*;

/**
 * @author Endi S. Dewata
 */
public class JDBCAdapter extends Adapter {

    public JDBCClient client;

    public void init() throws Exception {

        client = new JDBCClient(getParameters());
        client.connect();
    }

    public void dispose() throws Exception {
        client.close();
    }

    public Object openConnection() throws Exception {
        return client.getConnection();
    }

    public String getFieldNames(SourceConfig sourceConfig) throws Exception {
        StringBuilder sb = new StringBuilder();

        Collection fields = sourceConfig.getFieldConfigs();
        for (Iterator i=fields.iterator(); i.hasNext(); ) {
            FieldConfig fieldConfig = (FieldConfig)i.next();

            if (sb.length() > 0) sb.append(", ");
            sb.append(fieldConfig.getOriginalName());
        }

        return sb.toString();
    }

    public String getOringialPrimaryKeyFieldNamesAsString(SourceConfig sourceConfig) throws Exception {
        StringBuilder sb = new StringBuilder();

        Collection fields = sourceConfig.getOriginalPrimaryKeyNames();
        for (Iterator i=fields.iterator(); i.hasNext(); ) {
            String name = (String)i.next();

            if (sb.length() > 0) sb.append(", ");
            sb.append(name);
        }

        return sb.toString();
    }

    public String getTableName(SourceConfig sourceConfig) {
        String catalog = sourceConfig.getParameter(JDBCClient.CATALOG);
        String schema = sourceConfig.getParameter(JDBCClient.SCHEMA);
        String table = sourceConfig.getParameter(JDBCClient.TABLE);

        if (table == null) table = sourceConfig.getParameter(JDBCClient.TABLE_NAME);
        if (catalog != null) table = catalog +"."+table;
        if (schema != null) table = schema +"."+table;

        return table;
    }

    public boolean isJoinSupported() {
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Add
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void add(
            final Source source,
            final AddRequest request,
            final AddResponse response
    ) throws Exception {

        final boolean debug = log.isDebugEnabled();

        if (debug) {
            log.debug(Formatter.displaySeparator(80));
            log.debug(Formatter.displayLine("Add "+source.getName(), 80));
            log.debug(Formatter.displaySeparator(80));
        }

        InsertStatement statement = new InsertStatement();

        statement.setSource(source);

        Attributes attributes = request.getAttributes();
        for (Iterator i=attributes.getNames().iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            Object value = attributes.getValue(name);

            Field field = source.getField(name);
            statement.addAssignment(new Assignment(field, value));
        }

        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.setStatement(statement);

        UpdateResponse updateResponse = new UpdateResponse();

        client.executeUpdate(updateRequest, updateResponse);

        log.debug("Add operation completed.");
    }

    public void add(
            EntryMapping entryMapping,
            Collection sourceRefs,
            AttributeValues sourceValues,
            AddRequest request,
            AddResponse response
    ) throws Exception {

        boolean debug = log.isDebugEnabled();

        if (debug) {
            log.debug(Formatter.displaySeparator(80));
            log.debug(Formatter.displayLine("Add "+ sourceRefs, 80));
            log.debug(Formatter.displaySeparator(80));

            log.debug("Source values:");
            sourceValues.print();
        }

        AddRequestBuilder builder = new AddRequestBuilder(
                sourceRefs,
                sourceValues,
                penroseContext.getInterpreterManager().newInstance(),
                request,
                response
        );

        Collection requests = builder.generate();
        for (Iterator i=requests.iterator(); i.hasNext(); ) {
            UpdateRequest updateRequest = (UpdateRequest)i.next();
            UpdateResponse updateResponse = new UpdateResponse();

            client.executeUpdate(updateRequest, updateResponse);
        }

        log.debug("Add operation completed.");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Delete
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void delete(
            final Source source,
            final DeleteRequest request,
            final DeleteResponse response
    ) throws Exception {

        final boolean debug = log.isDebugEnabled();

        if (debug) {
            log.debug(Formatter.displaySeparator(80));
            log.debug(Formatter.displayLine("Delete "+source.getName(), 80));
            log.debug(Formatter.displaySeparator(80));
        }

        DeleteStatement statement = new DeleteStatement();

        statement.setSource(source);

        RDN rdn = request.getDn().getRdn();
        Filter filter = null;
        for (Iterator i=rdn.getNames().iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            Object value = rdn.get(name);

            SimpleFilter sf = new SimpleFilter(name, "=", value);
            filter = FilterTool.appendAndFilter(filter, sf);
        }

        statement.setFilter(filter);

        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.setStatement(statement);

        UpdateResponse updateResponse = new UpdateResponse();

        client.executeUpdate(updateRequest, updateResponse);

        log.debug("Delete operation completed.");
    }

    public void delete(
            EntryMapping entryMapping,
            Collection sourceRefs,
            AttributeValues sourceValues,
            DeleteRequest request,
            DeleteResponse response
    ) throws Exception {

        boolean debug = log.isDebugEnabled();

        if (debug) {
            log.debug(Formatter.displaySeparator(80));
            log.debug(Formatter.displayLine("Delete "+ sourceRefs, 80));
            log.debug(Formatter.displaySeparator(80));

            log.debug("Source values:");
            sourceValues.print();
        }

        DeleteRequestBuilder builder = new DeleteRequestBuilder(
                sourceRefs,
                sourceValues,
                penroseContext.getInterpreterManager().newInstance(),
                request,
                response
        );

        Collection requests = builder.generate();
        for (Iterator i=requests.iterator(); i.hasNext(); ) {
            UpdateRequest updateRequest = (UpdateRequest)i.next();
            UpdateResponse updateResponse = new UpdateResponse();

            client.executeUpdate(updateRequest, updateResponse);
        }

        log.debug("Delete operation completed.");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Modify
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void modify(
            final Source source,
            final ModifyRequest request,
            final ModifyResponse response
    ) throws Exception {

        final boolean debug = log.isDebugEnabled();

        if (debug) {
            log.debug(Formatter.displaySeparator(80));
            log.debug(Formatter.displayLine("Modify "+source.getName(), 80));
            log.debug(Formatter.displaySeparator(80));
        }

        UpdateStatement statement = new UpdateStatement();

        statement.setSource(source);

        Collection<Modification> modifications = request.getModifications();
        for (Iterator i=modifications.iterator(); i.hasNext(); ) {
            Modification modification = (Modification)i.next();

            int type = modification.getType();
            Attribute attribute = modification.getAttribute();
            Field field = source.getField(attribute.getName());

            switch (type) {
                case Modification.ADD:
                case Modification.REPLACE:
                    statement.addAssignment(new Assignment(field, attribute.getValue()));
                    break;

                case Modification.DELETE:
                    statement.addAssignment(new Assignment(field, null));
                    break;
            }
        }

        RDN rdn = request.getDn().getRdn();
        Filter filter = null;
        for (Iterator i=rdn.getNames().iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            Object value = rdn.get(name);

            SimpleFilter sf = new SimpleFilter(name, "=", value);
            filter = FilterTool.appendAndFilter(filter, sf);
        }

        statement.setFilter(filter);

        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.setStatement(statement);

        UpdateResponse updateResponse = new UpdateResponse();

        client.executeUpdate(updateRequest, updateResponse);

        log.debug("Modify operation completed.");
    }

    public void modify(
            EntryMapping entryMapping,
            Collection sourceRefs,
            AttributeValues sourceValues,
            ModifyRequest request,
            ModifyResponse response
    ) throws Exception {

        boolean debug = log.isDebugEnabled();

        if (debug) {
            log.debug(Formatter.displaySeparator(80));
            log.debug(Formatter.displayLine("Modify "+ sourceRefs, 80));
            log.debug(Formatter.displaySeparator(80));

            log.debug("Source values:");
            sourceValues.print();
        }

        ModifyRequestBuilder builder = new ModifyRequestBuilder(
                sourceRefs,
                sourceValues,
                penroseContext.getInterpreterManager().newInstance(),
                request,
                response
        );

        Collection requests = builder.generate();
        for (Iterator i=requests.iterator(); i.hasNext(); ) {
            UpdateRequest updateRequest = (UpdateRequest)i.next();
            UpdateResponse updateResponse = new UpdateResponse();

            client.executeUpdate(updateRequest, updateResponse);
        }

        log.debug("Modify operation completed.");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // ModRDN
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void modrdn(
            EntryMapping entryMapping,
            Collection sourceRefs,
            AttributeValues sourceValues,
            ModRdnRequest request,
            ModRdnResponse response
    ) throws Exception {

        boolean debug = log.isDebugEnabled();

        if (debug) {
            log.debug(Formatter.displaySeparator(80));
            log.debug(Formatter.displayLine("ModRdn "+ sourceRefs, 80));
            log.debug(Formatter.displaySeparator(80));

            log.debug("Source values:");
            sourceValues.print();
        }

        ModRdnRequestBuilder builder = new ModRdnRequestBuilder(
                sourceRefs,
                sourceValues,
                penroseContext.getInterpreterManager().newInstance(),
                request,
                response
        );

        Collection requests = builder.generate();
        for (Iterator i=requests.iterator(); i.hasNext(); ) {
            UpdateRequest updateRequest = (UpdateRequest)i.next();
            UpdateResponse updateResponse = new UpdateResponse();

            client.executeUpdate(updateRequest, updateResponse);
        }

        log.debug("ModRdn operation completed.");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Search
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void search(
            final Source source,
            final SearchRequest request,
            final SearchResponse response
    ) throws Exception {

        final boolean debug = log.isDebugEnabled();

        if (debug) {
            log.debug(Formatter.displaySeparator(80));
            log.debug(Formatter.displayLine("Search "+source.getName(), 80));
            log.debug(Formatter.displaySeparator(80));
        }

        SelectStatement statement = new SelectStatement();

        SourceRef sourceRef = new SourceRef(source);

        Filter filter = null;

        DN dn = request.getDn();
        if (dn != null) {
            RDN rdn = dn.getRdn();
            for (Iterator i=rdn.getNames().iterator(); i.hasNext(); ) {
                String name = (String)i.next();
                Object value = rdn.get(name);

                SimpleFilter sf = new SimpleFilter(name, "=", value);
                filter = FilterTool.appendAndFilter(filter, sf);
            }
        }

        filter = FilterTool.appendAndFilter(filter, request.getFilter());

        statement.addFieldRefs(sourceRef.getFieldRefs());
        statement.addSourceRef(sourceRef);
        statement.setFilter(filter);
        statement.setOrders(sourceRef.getPrimaryKeyFieldRefs());

        QueryRequest queryRequest = new QueryRequest();
        queryRequest.setStatement(statement);

        QueryResponse queryResponse = new QueryResponse() {
            public void add(Object object) throws Exception {
                ResultSet rs = (ResultSet)object;
                Entry entry = createEntry(source, rs);
                response.add(entry);
            }
            public void close() throws Exception {
                response.close();
            }
        };

        client.executeQuery(queryRequest, queryResponse);

        log.debug("Search operation completed.");
    }

    public void search(
            final EntryMapping entryMapping,
            final Collection sourceRefs,
            final AttributeValues sourceValues,
            final SearchRequest request,
            final SearchResponse response
    ) throws Exception {

        final boolean debug = log.isDebugEnabled();

        if (debug) {
            log.debug(Formatter.displaySeparator(80));
            log.debug(Formatter.displayLine("Search "+ sourceRefs, 80));
            log.debug(Formatter.displaySeparator(80));

            log.debug("Source values:");
            sourceValues.print();
        }

        SearchRequestBuilder builder = new SearchRequestBuilder(
                partition,
                entryMapping,
                sourceRefs,
                sourceValues,
                penroseContext.getInterpreterManager().newInstance(),
                request,
                response
        );

        QueryRequest queryRequest = builder.generate();
        QueryResponse queryResponse = new QueryResponse() {

            Entry lastEntry;

            public void add(Object object) throws Exception {
                ResultSet rs = (ResultSet)object;
                Entry entry = createEntry(entryMapping, sourceRefs, rs);

                if (lastEntry == null) {
                    lastEntry = entry;

                } else if (entry.getDn().equals(lastEntry.getDn())) {
                    mergeEntry(entry, lastEntry);

                } else {
                    response.add(lastEntry);
                    lastEntry = entry;
                }

                if (debug) {
                    JDBCFormatter.printEntry(entry);
                }
            }

            public void close() throws Exception {
                if (lastEntry != null) {
                    response.add(lastEntry);
                }
                response.close();
            }
        };

        client.executeQuery(queryRequest, queryResponse);

        log.debug("Search operation completed.");
    }

    public Entry createEntry(
            Source source,
            ResultSet rs
    ) throws Exception {

        Entry entry = new Entry();

        Attributes attributes = new Attributes();
        RDNBuilder rb = new RDNBuilder();

        int column = 1;
        for (Iterator i= source.getFields().iterator(); i.hasNext(); column++) {
            Field field = (Field)i.next();

            Object value = rs.getObject(column);
            if (value == null) continue;

            String fieldName = field.getName();
            attributes.addValue(fieldName, value);

            if (field.isPrimaryKey()) rb.set(fieldName, value);
        }

        entry.setAttributes(attributes);
        entry.setDn(rb.toRdn());

        return entry;
    }

    public Entry createEntry(
            EntryMapping entryMapping,
            Collection sources,
            ResultSet rs
    ) throws Exception {

        Entry entry = new Entry();
        entry.setEntryMapping(entryMapping);

        RDNBuilder rb = new RDNBuilder();

        boolean first = true;
        int column = 1;

        for (Iterator i=sources.iterator(); i.hasNext(); ) {
            SourceRef sourceRef = (SourceRef)i.next();
            String sourceName = sourceRef.getAlias();

            Attributes sourceValues = new Attributes();

            for (Iterator j= sourceRef.getFieldRefs().iterator(); j.hasNext(); column++) {
                FieldRef fieldRef = (FieldRef)j.next();

                Object value = rs.getObject(column);
                if (value == null) continue;

                String fieldName = fieldRef.getName();
                String name = sourceName+"."+fieldName;
                sourceValues.addValue(fieldName, value);

                if (first && fieldRef.isPrimaryKey()) {
                    rb.set(name, value);
                    sourceValues.addValue("primaryKey."+fieldName, value);
                    first = false;
                }
            }

            entry.setSourceValues(sourceName, sourceValues);
        }

        entry.setDn(rb.toRdn());

        return entry;
    }

    public void mergeEntry(Entry source, Entry destination) {
        for (Iterator i=source.getSourceNames().iterator(); i.hasNext(); ) {
            String sourceName = (String)i.next();

            Attributes sourceValues = source.getSourceValues(sourceName);

            Attributes destinationValues = destination.getSourceValues(sourceName);
            if (destinationValues == null) {
                destinationValues = new Attributes();
                destination.setSourceValues(sourceName, destinationValues);
            }

            destinationValues.add(sourceValues);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Change Log
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Long getLastChangeNumber(Source source) throws Exception {

        String table = getTableName(source.getSourceConfig());

        String sql = "select max(changeNumber) from "+table+"_changelog";
        Collection parameters = new ArrayList();

        QueryResponse queryResponse = new QueryResponse() {
            public void add(Object object) throws Exception {
                ResultSet rs = (ResultSet)object;
                Integer changeNumber = (Integer)rs.getObject(1);
                super.add(changeNumber);
            }
        };

        client.executeQuery(sql, parameters, queryResponse);

        if (!queryResponse.hasNext()) return null;

        Number changeNumber = (Number)queryResponse.next();
        if (changeNumber == null) return null;

        log.debug("Last change number: "+changeNumber);
        return new Long(changeNumber.longValue());
    }

    public SearchResponse getChanges(
            final Source source,
            final Long lastChangeNumber
    ) throws Exception {

        final boolean debug = log.isDebugEnabled();
        //log.debug("Searching JDBC source "+sourceConfig.getConnectionName()+"/"+sourceConfig.getName());

        final SearchResponse response = new SearchResponse();

        String table = getTableName(source.getSourceConfig());

        int sizeLimit = 100;

        StringBuilder columns = new StringBuilder();
        columns.append("select changeNumber, changeTime, changeAction, changeUser");

        StringBuilder sb = new StringBuilder();
        sb.append("from ");
        sb.append(table);
        sb.append("_changelog");

        for (Iterator i= source.getPrimaryKeyFields().iterator(); i.hasNext(); ) {
            Field field = (Field)i.next();

            columns.append(", ");
            columns.append(field.getOriginalName());
        }

        StringBuilder whereClause = new StringBuilder();
        whereClause.append("where changeNumber > ? order by changeNumber");

        Collection parameters = new ArrayList();
        parameters.add(lastChangeNumber);

        String sql = columns+" "+sb+" "+whereClause;

        QueryResponse queryResponse = new QueryResponse() {
            public void add(Object object) throws Exception {
                ResultSet rs = (ResultSet)object;
                RDN rdn = getChanges(source, rs);

                if (debug) printChanges(source, rdn);

                response.add(rdn);
            }

            public void close() throws Exception {
                response.close();
            }
        };

        client.executeQuery(sql, parameters, queryResponse);

        return response;
    }

    public RDN getChanges(Source source, ResultSet rs) throws Exception {

        RDNBuilder rb = new RDNBuilder();
        rb.set("changeNumber", rs.getObject("changeNumber"));
        rb.set("changeTime", rs.getObject("changeTime"));
        rb.set("changeAction", rs.getObject("changeAction"));
        rb.set("changeUser", rs.getObject("changeUser"));

        int counter = 5;
        for (Iterator i=source.getPrimaryKeyNames().iterator(); i.hasNext(); ) {
            String name = (String)i.next();

            Object value = rs.getObject(counter++);
            if (value == null) continue;

            rb.set(name, value);
        }

        return rb.toRdn();
    }

    public int printChangesHeader(SourceConfig sourceConfig) throws Exception {

        StringBuilder resultHeader = new StringBuilder();
        resultHeader.append("| ");
        resultHeader.append(Formatter.rightPad("#", 5));
        resultHeader.append(" | ");
        resultHeader.append(Formatter.rightPad("time", 19));
        resultHeader.append(" | ");
        resultHeader.append(Formatter.rightPad("action", 10));
        resultHeader.append(" | ");
        resultHeader.append(Formatter.rightPad("user", 10));
        resultHeader.append(" |");

        Collection fields = sourceConfig.getPrimaryKeyFieldConfigs();
        for (Iterator j=fields.iterator(); j.hasNext(); ) {
            FieldConfig fieldConfig = (FieldConfig)j.next();

            String name = fieldConfig.getName();
            int length = fieldConfig.getLength() > 15 ? 15 : fieldConfig.getLength();

            resultHeader.append(" ");
            resultHeader.append(Formatter.rightPad(name, length));
            resultHeader.append(" |");
        }

        int width = resultHeader.length();

        log.debug("Results:");
        log.debug(Formatter.displaySeparator(width));
        log.debug(resultHeader.toString());
        log.debug(Formatter.displaySeparator(width));

        return width;
    }

    public void printChanges(Source source, RDN rdn) throws Exception {
        StringBuilder resultFields = new StringBuilder();
        resultFields.append("| ");
        resultFields.append(Formatter.rightPad(rdn.get("changeNumber").toString(), 5));
        resultFields.append(" | ");
        resultFields.append(Formatter.rightPad(rdn.get("changeTime").toString(), 19));
        resultFields.append(" | ");
        resultFields.append(Formatter.rightPad(rdn.get("changeAction").toString(), 10));
        resultFields.append(" | ");
        resultFields.append(Formatter.rightPad(rdn.get("changeUser").toString(), 10));
        resultFields.append(" |");

        Collection fields = source.getPrimaryKeyFields();
        for (Iterator j=fields.iterator(); j.hasNext(); ) {
            Field field = (Field)j.next();

            Object value = rdn.get(field.getName());
            int length = field.getLength() > 15 ? 15 : field.getLength();

            resultFields.append(" ");
            resultFields.append(Formatter.rightPad(value == null ? "null" : value.toString(), length));
            resultFields.append(" |");
        }

        log.debug(resultFields.toString());
    }

    public Filter convert(EntryMapping entryMapping, SubstringFilter filter) throws Exception {

        String attributeName = filter.getAttribute();
        Collection substrings = filter.getSubstrings();

        AttributeMapping attributeMapping = entryMapping.getAttributeMapping(attributeName);
        String variable = attributeMapping.getVariable();

        if (variable == null) return null;

        int index = variable.indexOf(".");
        String sourceName = variable.substring(0, index);
        String fieldName = variable.substring(index+1);

        StringBuilder sb = new StringBuilder();
        for (Iterator i=substrings.iterator(); i.hasNext(); ) {
            Object o = i.next();
            if (o.equals(SubstringFilter.STAR)) {
                sb.append("%");
            } else {
                String substring = (String)o;
                sb.append(substring);
            }
        }

        return new SimpleFilter(fieldName, "like", sb.toString());
    }
}
