package org.safehaus.penrose.jdbc.adapter;

import org.safehaus.penrose.ldap.*;
import org.safehaus.penrose.ldap.SourceValues;
import org.safehaus.penrose.mapping.FieldMapping;
import org.safehaus.penrose.mapping.Expression;
import org.safehaus.penrose.interpreter.Interpreter;
import org.safehaus.penrose.jdbc.*;
import org.safehaus.penrose.jdbc.Request;
import org.safehaus.penrose.source.SourceRef;
import org.safehaus.penrose.source.FieldRef;
import org.safehaus.penrose.source.Field;
import org.safehaus.penrose.source.Source;
import org.safehaus.penrose.filter.SimpleFilter;
import org.safehaus.penrose.filter.FilterTool;
import org.safehaus.penrose.filter.Filter;

import java.util.*;

/**
 * @author Endi S. Dewata
 */
public class ModifyRequestBuilder extends RequestBuilder {

    Collection<SourceRef> sourceRefs;

    SourceValues sourceValues;
    Interpreter interpreter;

    ModifyRequest request;
    ModifyResponse response;

    public ModifyRequestBuilder(
            Collection<SourceRef> sourceRefs,
            SourceValues sourceValues,
            Interpreter interpreter,
            ModifyRequest request,
            ModifyResponse response
    ) throws Exception {

        this.sourceRefs = sourceRefs;
        this.sourceValues = sourceValues;

        this.interpreter = interpreter;

        this.request = request;
        this.response = response;
    }

    public Collection<Request> generate() throws Exception {

        boolean first = true;
        for (SourceRef sourceRef : sourceRefs) {

            if (first) {
                generatePrimaryRequest(sourceRef);
                first = false;

            } else {
                generateSecondaryRequests(sourceRef);
            }
        }

        return requests;
    }

    public void generatePrimaryRequest(
            SourceRef sourceRef
    ) throws Exception {

        String sourceName = sourceRef.getAlias();
        if (debug) log.debug("Processing source "+sourceName);

        UpdateStatement statement = new UpdateStatement();

        statement.setSourceRef(sourceRef);

        Collection<Modification> modifications = request.getModifications();
        for (Modification modification : modifications) {

            int type = modification.getType();
            Attribute attribute = modification.getAttribute();

            String attributeName = attribute.getName();
            Collection attributeValues = attribute.getValues();

            if (debug) {
                switch (type) {
                    case Modification.ADD:
                        log.debug("Adding attribute " + attributeName + ": " + attributeValues);
                        break;
                    case Modification.REPLACE:
                        log.debug("Replacing attribute " + attributeName + ": " + attributeValues);
                        break;
                    case Modification.DELETE:
                        log.debug("Deleting attribute " + attributeName + ": " + attributeValues);
                        break;
                }
            }

            Object attributeValue = attribute.getValue(); // use only the first value

            interpreter.set(sourceValues);
            interpreter.set(attributeName, attributeValue);

            switch (type) {
                case Modification.ADD:
                case Modification.REPLACE:
                    for (FieldRef fieldRef : sourceRef.getFieldRefs()) {
                        Field field = fieldRef.getField();

                        FieldMapping fieldMapping = fieldRef.getFieldMapping();

                        Object value = interpreter.eval(fieldMapping);
                        if (value == null) continue;

                        String fieldName = field.getName();
                        if (debug) log.debug("Setting field " + fieldName + " to " + value);

                        statement.addAssignment(new Assignment(fieldRef, value));
                    }
                    break;

                case Modification.DELETE:
                    for (FieldRef fieldRef : sourceRef.getFieldRefs()) {
                        Field field = fieldRef.getField();

                        FieldMapping fieldMapping = fieldRef.getFieldMapping();

                        String variable = fieldMapping.getVariable();
                        if (variable == null) continue;

                        if (!variable.equals(attributeName)) continue;

                        String fieldName = field.getName();
                        if (debug) log.debug("Setting field " + fieldName + " to null");

                        statement.addAssignment(new Assignment(fieldRef, null));
                    }
                    break;
            }

            interpreter.clear();
        }

        if (statement.isEmpty()) return;

        Filter filter = null;

        Attributes attributes = sourceValues.get(sourceName);

        for (String fieldName : attributes.getNames()) {
            if (fieldName.startsWith("primaryKey.")) continue;

            Object value = attributes.getValue(fieldName);

            FieldRef fieldRef = sourceRef.getFieldRef(fieldName);
            Field field = fieldRef.getField();

            SimpleFilter sf = new SimpleFilter(fieldName, "=", value);
            filter = FilterTool.appendAndFilter(filter, sf);
        }

        statement.setFilter(filter);

        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.setStatement(statement);

        requests.add(updateRequest);
    }

    public void generateSecondaryRequests(
            SourceRef sourceRef
    ) throws Exception {

        String sourceName = sourceRef.getAlias();
        if (debug) log.debug("Processing source "+sourceName);

        Collection<Modification> modifications = request.getModifications();
        for (Modification modification : modifications) {

            int type = modification.getType();
            Attribute attribute = modification.getAttribute();

            String attributeName = attribute.getName();
            Collection attributeValues = attribute.getValues();

            if (debug) {
                switch (type) {
                    case Modification.ADD:
                        log.debug("Adding attribute " + attributeName + ": " + attributeValues);
                        break;
                    case Modification.REPLACE:
                        log.debug("Replacing attribute " + attributeName + ": " + attributeValues);
                        break;
                    case Modification.DELETE:
                        log.debug("Deleting attribute " + attributeName + ": " + attributeValues);
                        break;
                }
            }

            if (attributeValues.isEmpty()) {
                for (FieldRef fieldRef : sourceRef.getFieldRefs()) {
                    FieldMapping fieldMapping = fieldRef.getFieldMapping();

                    String variable = fieldMapping.getVariable();
                    if (variable != null) {
                        if (variable.indexOf(".") >= 0) continue; // skip foreign key
                    }

                    if (attributeName.equals(variable)) {
                        generateDeleteStatement(
                                sourceRef
                        );
                        continue;
                    }

                    Expression expression = fieldMapping.getExpression();
                    if (expression == null) continue;

                    String foreach = expression.getForeach();
                    if (foreach == null) continue;

                    if (attributeName.equals(foreach)) {
                        generateDeleteStatement(
                                sourceRef
                        );
                        continue;
                    }

                }
                continue;
            }

            boolean first = true;

            for (Object attributeValue : attributeValues) {
                interpreter.set(sourceValues);
                interpreter.set(attributeName, attributeValue);

                Map<String,Object> values = new HashMap<String,Object>();

                for (FieldRef fieldRef : sourceRef.getFieldRefs()) {
                    FieldMapping fieldMapping = fieldRef.getFieldMapping();

                    String variable = fieldMapping.getVariable();
                    if (variable != null) {
                        if (variable.indexOf(".") >= 0) continue; // skip foreign key
                    }

                    String fieldName = fieldRef.getName();
                    Object value = interpreter.eval(fieldMapping);
                    if (value == null) continue;

                    values.put(fieldName, value);
                }

                if (values.isEmpty()) continue;

                switch (type) {
                    case Modification.ADD:
                        generateInsertStatement(
                                sourceRef,
                                values
                        );
                        break;

                    case Modification.REPLACE:
                        if (first) {
                            generateDeleteStatement(
                                    sourceRef
                            );
                            first = false;
                        }
                        generateInsertStatement(
                                sourceRef,
                                values
                        );
                        break;

                    case Modification.DELETE:
                        generateDeleteStatement(
                                sourceRef,
                                values
                        );
                        break;
                }

                interpreter.clear();
            }

        }
    }

    public void generateInsertStatement(
            SourceRef sourceRef,
            Map<String,Object> values
    ) throws Exception {

        String sourceName = sourceRef.getAlias();
        if (debug) log.debug("Inserting values into "+sourceName);

        InsertStatement statement = new InsertStatement();

        statement.setSource(sourceRef.getSource());

        for (FieldRef fieldRef : sourceRef.getFieldRefs()) {
            Field field = fieldRef.getField();

            FieldMapping fieldMapping = fieldRef.getFieldMapping();

            String variable = fieldMapping.getVariable();
            if (variable == null) continue;

            int i = variable.indexOf(".");
            String sn = variable.substring(0, i);
            String fn = variable.substring(i + 1);

            Attributes fields = sourceValues.get(sn);
            if (fields == null) continue;

            Object value = fields.getValue(fn);
            if (value == null) continue;

            String fieldName = field.getName();

            if (debug) log.debug(" - Field: " + fieldName + ": " + value);
            statement.addAssignment(new Assignment(fieldRef, value));
        }

        for (String fieldName : values.keySet()) {
            Object value = values.get(fieldName);

            FieldRef fieldRef = sourceRef.getFieldRef(fieldName);
            Field field = fieldRef.getField();

            if (debug) log.debug(" - Field: " + fieldName + ": " + value);
            statement.addAssignment(new Assignment(fieldRef, value));
        }

        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.setStatement(statement);

        requests.add(updateRequest);
    }

    public void generateDeleteStatement(
            SourceRef sourceRef
    ) throws Exception {
        generateDeleteStatement(sourceRef, null);
    }

    public void generateDeleteStatement(
            SourceRef sourceRef,
            Map<String,Object> values
    ) throws Exception {

        String sourceName = sourceRef.getAlias();
        if (debug) log.debug("Deleting rows from "+sourceName);

        DeleteStatement statement = new DeleteStatement();

        statement.setSourceRef(sourceRef);

        Filter filter = null;

        for (FieldRef fieldRef : sourceRef.getFieldRefs()) {
            Field field = fieldRef.getField();

            FieldMapping fieldMapping = fieldRef.getFieldMapping();

            String variable = fieldMapping.getVariable();
            if (variable == null) continue;

            int i = variable.indexOf(".");
            String sn = variable.substring(0, i);
            String fn = variable.substring(i + 1);

            Attributes fields = sourceValues.get(sn);
            if (fields == null) continue;

            Object value = fields.getValue(fn);
            if (value == null) continue;

            String fieldName = field.getName();

            SimpleFilter sf = new SimpleFilter(fieldName, "=", value);
            filter = FilterTool.appendAndFilter(filter, sf);

            if (debug) log.debug(" - Field: " + fieldName + ": " + value);
        }

        if (values != null) {
            for (String fieldName : values.keySet()) {
                Object value = values.get(fieldName);

                FieldRef fieldRef = sourceRef.getFieldRef(fieldName);
                Field field = fieldRef.getField();

                SimpleFilter sf = new SimpleFilter(fieldName, "=", value);
                filter = FilterTool.appendAndFilter(filter, sf);

                if (debug) log.debug(" - Field: " + fieldName + ": " + value);
            }
        }

        statement.setFilter(filter);

        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.setStatement(statement);

        requests.add(updateRequest);
    }
}
