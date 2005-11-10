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

import org.apache.commons.dbcp.*;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.ietf.ldap.LDAPException;
import org.safehaus.penrose.SearchResults;
import org.safehaus.penrose.engine.TransformEngine;
import org.safehaus.penrose.util.Formatter;
import org.safehaus.penrose.util.Formatter;
import org.safehaus.penrose.config.Config;
import org.safehaus.penrose.filter.Filter;
import org.safehaus.penrose.mapping.*;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.*;

/**
 * @author Endi S. Dewata
 */
public class JDBCAdapter extends Adapter {

    public final static String DRIVER     = "driver";
    public final static String URL        = "url";
    public final static String USER       = "user";
    public final static String PASSWORD   = "password";

    public final static String TABLE_NAME = "tableName";
    public final static String FILTER     = "filter";

    public DataSource ds;

    public JDBCFilterTool filterTool;

    public void init() throws Exception {
        String name = getConnectionName();

    	//log.debug("-------------------------------------------------------------------------------");
    	//log.debug("Initializing JDBC connection "+name+":");

        String driver = getParameter(DRIVER);
        String url = getParameter(URL);

        Class.forName(driver);

        Properties properties = new Properties();
        for (Iterator i=getParameterNames().iterator(); i.hasNext(); ) {
            String param = (String)i.next();
            String value = getParameter(param);
            //log.debug(" - "+param+": "+value);
            properties.setProperty(param, value);
        }

        GenericObjectPool connectionPool = new GenericObjectPool(null);
        //connectionPool.setMaxActive(0);

        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(url, properties);

        PoolableConnectionFactory poolableConnectionFactory =
                new PoolableConnectionFactory(
                        connectionFactory,
                        connectionPool,
                        null, // statement pool factory
                        null, // test query
                        false, // read only
                        true // auto commit
                );

        ds = new PoolingDataSource(connectionPool);

        filterTool = new JDBCFilterTool();
    }

    public String getFieldNames(SourceDefinition sourceDefinition) throws Exception {
        StringBuffer sb = new StringBuffer();

        Collection fields = sourceDefinition.getFieldDefinitions();
        for (Iterator i=fields.iterator(); i.hasNext(); ) {
            FieldDefinition fieldDefinition = (FieldDefinition)i.next();

            if (sb.length() > 0) sb.append(", ");
            sb.append(fieldDefinition.getOriginalName());
        }

        return sb.toString();
    }

    public String getPkFieldNames(SourceDefinition sourceDefinition) throws Exception {
        StringBuffer sb = new StringBuffer();

        Collection fields = sourceDefinition.getFieldDefinitions();
        for (Iterator i=fields.iterator(); i.hasNext(); ) {
            FieldDefinition fieldDefinition = (FieldDefinition)i.next();
            if (!fieldDefinition.isPrimaryKey()) continue;

            if (sb.length() > 0) sb.append(", ");
            sb.append(fieldDefinition.getOriginalName());
        }

        return sb.toString();
    }

    public SearchResults search(SourceDefinition sourceDefinition, Filter filter, long sizeLimit) throws Exception {

        log.debug("Searching JDBC source "+sourceDefinition.getConnectionName()+"/"+sourceDefinition.getName());

        SearchResults results = new SearchResults();

        String tableName = sourceDefinition.getParameter(TABLE_NAME);
        String sqlFilter = sourceDefinition.getParameter(FILTER);
        log.debug("tableName: "+tableName);
        log.debug("filter: "+sqlFilter);

        StringBuffer sb = new StringBuffer();
        sb.append("select ");
        sb.append(getPkFieldNames(sourceDefinition));
        sb.append(" from ");
        sb.append(tableName);

        StringBuffer sb2 = new StringBuffer();
        if (sqlFilter != null) {
            sb2.append(sqlFilter);
        }

        List parameters = new ArrayList();
        if (filter != null) {
            if (sb2.length() > 0) sb2.append(" and ");
            sb2.append(filterTool.convert(sourceDefinition, filter, parameters));
        }

        if (sb2.length() > 0) {
            sb.append(" where ");
            sb.append(sb2);
        }

        sb.append(" order by ");
        sb.append(getPkFieldNames(sourceDefinition));

        String sql = sb.toString();

        java.sql.Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = ds.getConnection();

            log.debug(Formatter.displaySeparator(80));
            log.debug(Formatter.displayLine(sql, 80));
            log.debug(Formatter.displaySeparator(80));

            ps = con.prepareStatement(sql);

            log.debug(Formatter.displayLine("Parameters:", 80));

            int counter = 0;
            for (Iterator i=parameters.iterator(); i.hasNext(); ) {
                Object param = i.next();
                ps.setObject(++counter, param);
                log.debug(Formatter.displayLine(" - "+counter+" = "+param, 80));
            }

            log.debug(Formatter.displaySeparator(80));

            rs = ps.executeQuery();

            int width = 0;
            boolean first = true;

            for (int i=0; rs.next() && (sizeLimit == 0 || i<sizeLimit); i++) {
                Row row = getPkValues(sourceDefinition, rs);
                results.add(row);

                if (first) {
                    width = printHeader(sourceDefinition);
                    first = false;
                }

                printValues(sourceDefinition, row);
            }

            if (width > 0) printFooter(width);

            if (rs.next()) {
                log.debug("RC: size limit exceeded.");
                results.setReturnCode(LDAPException.SIZE_LIMIT_EXCEEDED);
            }

        } finally {
            if (rs != null) try { rs.close(); } catch (Exception e) {}
            if (ps != null) try { ps.close(); } catch (Exception e) {}
            if (con != null) try { con.close(); } catch (Exception e) {}
        }

        results.close();

        return results;
    }

    public SearchResults load(SourceDefinition sourceDefinition, Filter filter, long sizeLimit) throws Exception {

        log.debug("Loading JDBC source "+sourceDefinition.getConnectionName()+"/"+sourceDefinition.getName());

        SearchResults results = new SearchResults();

        String tableName = sourceDefinition.getParameter(TABLE_NAME);
        String s = sourceDefinition.getParameter(FILTER);

        StringBuffer sqlFilter = new StringBuffer();
        if (s != null) sqlFilter.append(s);

        StringBuffer sb = new StringBuffer();

        StringBuffer select = new StringBuffer();
        select.append("select ");
        select.append(getFieldNames(sourceDefinition));
        sb.append(select);

        StringBuffer from = new StringBuffer();
        from.append("from ");
        from.append(tableName);
        sb.append(" ");
        sb.append(from);

        List parameters = new ArrayList();
        if (filter != null) {
            if (sqlFilter.length() > 0) sqlFilter.append(" and ");
            sqlFilter.append(filterTool.convert(sourceDefinition, filter, parameters));
        }

        StringBuffer whereClause = new StringBuffer();
        if (sqlFilter.length() > 0) {
            whereClause.append("where ");
            whereClause.append(sqlFilter);
            sb.append(" ");
            sb.append(whereClause);
        }

        StringBuffer orderBy = new StringBuffer();
        orderBy.append("order by ");
        orderBy.append(getPkFieldNames(sourceDefinition));
        sb.append(" ");
        sb.append(orderBy);

        String sql = sb.toString();

        java.sql.Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = ds.getConnection();

            log.debug(Formatter.displaySeparator(80));
            Collection lines = Formatter.split(sql, 80);
            for (Iterator i=lines.iterator(); i.hasNext(); ) {
                String line = (String)i.next();
                log.debug(Formatter.displayLine(line, 80));
            }
            log.debug(Formatter.displaySeparator(80));

            ps = con.prepareStatement(sql);

            if (parameters.size() > 0) {
                log.debug(Formatter.displayLine("Parameters:", 80));

                int counter = 0;
                for (Iterator i=parameters.iterator(); i.hasNext(); ) {
                    Object param = i.next();
                    ps.setObject(++counter, param);
                    log.debug(Formatter.displayLine(" - "+counter+" = "+param, 80));
                }

                log.debug(Formatter.displaySeparator(80));
            }

            rs = ps.executeQuery();

            int width = 0;
            boolean first = true;

            for (int i=0; rs.next() && (sizeLimit == 0 || i<sizeLimit); i++) {
                AttributeValues av = getValues(sourceDefinition, rs);
                results.add(av);

                if (first) {
                    width = printHeader(sourceDefinition);
                    first = false;
                }

                printValues(sourceDefinition, av);
            }

            if (width > 0) printFooter(width);

            if (rs.next()) {
                log.debug("RC: size limit exceeded.");
                results.setReturnCode(LDAPException.SIZE_LIMIT_EXCEEDED);
            }

        } finally {
            if (rs != null) try { rs.close(); } catch (Exception e) {}
            if (ps != null) try { ps.close(); } catch (Exception e) {}
            if (con != null) try { con.close(); } catch (Exception e) {}
        }

        results.close();

        return results;
    }

    public Row getPkValues(SourceDefinition sourceDefinition, ResultSet rs) throws Exception {

        Row row = new Row();
        int c = 1;

        ResultSetMetaData rsmd = rs.getMetaData();
        int count = rsmd.getColumnCount();

        Collection fields = sourceDefinition.getPrimaryKeyFieldDefinitions();

        for (Iterator i=fields.iterator(); i.hasNext() && c<=count; c++) {
            FieldDefinition fieldDefinition = (FieldDefinition)i.next();

            Object value = rs.getObject(c);
            if (value == null) continue;

            row.set(fieldDefinition.getName(), value);
        }

        //log.debug("=> values: "+row);

        return row;
    }

    public Row getChanges(SourceDefinition sourceDefinition, ResultSet rs) throws Exception {

        Row row = new Row();
        int c = 1;

        row.set("changeNumber", rs.getObject(c++));
        row.set("changeTime", rs.getObject(c++));
        row.set("changeAction", rs.getObject(c++));
        row.set("changeUser", rs.getObject(c++));

        Collection fields = sourceDefinition.getPrimaryKeyFieldDefinitions();
        for (Iterator i=fields.iterator(); i.hasNext(); c++) {
            FieldDefinition fieldDefinition = (FieldDefinition)i.next();

            Object value = rs.getObject(c);
            if (value == null) continue;

            row.set(fieldDefinition.getName(), value);
        }

        return row;
    }

    public AttributeValues getValues(SourceDefinition sourceDefinition, ResultSet rs) throws Exception {

        AttributeValues row = new AttributeValues();

        ResultSetMetaData rsmd = rs.getMetaData();
        int count = rsmd.getColumnCount();

        int c = 1;

        Collection fields = sourceDefinition.getFieldDefinitions();

        for (Iterator i=fields.iterator(); i.hasNext() && c<=count; c++) {
            FieldDefinition fieldDefinition = (FieldDefinition)i.next();
            
            Object value = rs.getObject(c);
            if (value == null) continue;

            row.add(fieldDefinition.getName(), value);
        }

        //log.debug("=> values: "+row);

        return row;
    }

    public int bind(SourceDefinition sourceDefinition, AttributeValues values, String cred) throws Exception {
        return LDAPException.INVALID_CREDENTIALS;
    }

    public int add(SourceDefinition sourceDefinition, AttributeValues sourceValues) throws Exception {

        // convert sets into single values
        Collection rows = TransformEngine.convert(sourceValues);
    	Row row = (Row)rows.iterator().next();

        String tableName = sourceDefinition.getParameter(TABLE_NAME);

        java.sql.Connection con = null;
        PreparedStatement ps = null;

        try {
            con = ds.getConnection();

            StringBuffer sb = new StringBuffer();
            StringBuffer sb2 = new StringBuffer();

            Collection fields = sourceDefinition.getFieldDefinitions();
            Collection parameters = new ArrayList();
            for (Iterator i=fields.iterator(); i.hasNext(); ) {
                FieldDefinition fieldDefinition = (FieldDefinition)i.next();

                if (sb.length() > 0) {
                    sb.append(", ");
                    sb2.append(", ");
                }

                sb.append(fieldDefinition.getOriginalName());
                sb2.append("?");

                Object obj = row.get(fieldDefinition.getName());
                parameters.add(obj);
            }

            String sql = "insert into "+tableName+" ("+sb+") values ("+sb2+")";

            log.debug(Formatter.displaySeparator(80));
            log.debug(Formatter.displayLine(sql, 80));
            log.debug(Formatter.displaySeparator(80));

            ps = con.prepareStatement(sql);

            log.debug(Formatter.displayLine("Parameters:", 80));

            int c = 1;
            for (Iterator i=parameters.iterator(); i.hasNext(); c++) {
                Object obj = i.next();
                ps.setObject(c, obj);
                log.debug(Formatter.displayLine(" - "+c+" = "+(obj == null ? null : obj.toString()), 80));
            }

            log.debug(Formatter.displaySeparator(80));

            ps.executeUpdate();

        } catch (Exception e) {
            log.debug("Add failed: ("+e.getClass().getName()+") "+e.getMessage());
            return LDAPException.ENTRY_ALREADY_EXISTS;

        } finally {
            if (ps != null) try { ps.close(); } catch (Exception e) {}
            if (con != null) try { con.close(); } catch (Exception e) {}
        }

        return LDAPException.SUCCESS;
    }

    public int delete(SourceDefinition sourceDefinition, AttributeValues sourceValues) throws Exception {

        Row pk = sourceDefinition.getPrimaryKeyValues(sourceValues);
        //log.debug("Deleting entry "+pk);

        String tableName = sourceDefinition.getParameter(TABLE_NAME);

        java.sql.Connection con = null;
        PreparedStatement ps = null;

        try {
            con = ds.getConnection();

            StringBuffer sb = new StringBuffer();
            for (Iterator i=pk.getNames().iterator(); i.hasNext(); ) {
                String name = (String)i.next();

                if (sb.length() > 0) sb.append(" and ");

                sb.append(name);
                sb.append("=?");
            }

            String sql = "delete from "+tableName+" where "+sb;

            log.debug(Formatter.displaySeparator(80));
            log.debug(Formatter.displayLine(sql, 80));
            log.debug(Formatter.displaySeparator(80));

            ps = con.prepareStatement(sql);

            log.debug(Formatter.displayLine("Parameters:", 80));

            int c = 1;
            for (Iterator i=pk.getNames().iterator(); i.hasNext(); c++) {
                String name = (String)i.next();
                Object value = pk.get(name);
                ps.setObject(c, value);
                log.debug(Formatter.displayLine(" - "+c+" = "+value, 80));
            }

            log.debug(Formatter.displaySeparator(80));

            int count = ps.executeUpdate();
            if (count == 0) return LDAPException.NO_SUCH_OBJECT;

        } finally {
            if (ps != null) try { ps.close(); } catch (Exception e) {}
            if (con != null) try { con.close(); } catch (Exception e) {}
        }

        return LDAPException.SUCCESS;
    }

    public int modify(SourceDefinition sourceDefinition, AttributeValues oldEntry, AttributeValues newEntry) throws Exception {

        // convert sets into single values
        Collection oldRows = TransformEngine.convert(oldEntry);
        Collection newRows = TransformEngine.convert(newEntry);

        Row oldRow = (Row)oldRows.iterator().next();
        Row newRow = (Row)newRows.iterator().next();

        //log.debug("Modifying source "+source.getName()+": "+oldRow+" with "+newRow);

        String tableName = sourceDefinition.getParameter(TABLE_NAME);

        java.sql.Connection con = null;
        PreparedStatement ps = null;

        try {
            con = ds.getConnection();

            StringBuffer columns = new StringBuffer();
            StringBuffer whereClause = new StringBuffer();
            Collection parameters = new ArrayList();

            Collection fields = sourceDefinition.getFieldDefinitions();
            for (Iterator i=fields.iterator(); i.hasNext(); ) {
                FieldDefinition fieldDefinition = (FieldDefinition)i.next();

                if (columns.length() > 0) columns.append(", ");

                columns.append(fieldDefinition.getOriginalName());
                columns.append("=?");

                Object value = newRow.get(fieldDefinition.getName());
                parameters.add(value);
            }

            for (Iterator i=fields.iterator(); i.hasNext(); ) {
                FieldDefinition fieldDefinition = (FieldDefinition)i.next();
                if (!fieldDefinition.isPrimaryKey()) continue;

                if (whereClause.length() > 0) whereClause.append(" and ");

                whereClause.append(fieldDefinition.getOriginalName());
                whereClause.append("=?");

                Object value = oldRow.get(fieldDefinition.getName());
                parameters.add(value);
            }

            String sql = "update "+tableName+" set "+columns+" where "+whereClause;

            log.debug(Formatter.displaySeparator(80));
            log.debug(Formatter.displayLine("update "+tableName, 80));
            log.debug(Formatter.displayLine("set "+columns, 80));
            log.debug(Formatter.displayLine("where "+whereClause, 80));
            log.debug(Formatter.displaySeparator(80));

            ps = con.prepareStatement(sql);

            log.debug(Formatter.displayLine("Parameters:", 80));

            int c = 1;
            for (Iterator i=parameters.iterator(); i.hasNext(); c++) {
                Object value = i.next();
                ps.setObject(c, value);
                log.debug(Formatter.displayLine(" - "+c+" = "+value, 80));
            }

            log.debug(Formatter.displaySeparator(80));

            int count = ps.executeUpdate();
            if (count == 0) return LDAPException.NO_SUCH_OBJECT;

        } finally {
            if (ps != null) try { ps.close(); } catch (Exception e) {}
            if (con != null) try { con.close(); } catch (Exception e) {}
        }
        
        return LDAPException.SUCCESS;
    }

    public int modrdn(
            SourceDefinition sourceDefinition,
            Row oldRdn,
            Row newRdn)
            throws Exception {

        //log.debug("Renaming source "+source.getName()+": "+oldRdn+" with "+newRdn);

        String tableName = sourceDefinition.getParameter(TABLE_NAME);

        java.sql.Connection con = null;
        PreparedStatement ps = null;

        try {
            con = ds.getConnection();

            StringBuffer columns = new StringBuffer();
            StringBuffer whereClause = new StringBuffer();
            Collection parameters = new ArrayList();

            Collection fields = sourceDefinition.getFieldDefinitions();
            for (Iterator i=fields.iterator(); i.hasNext(); ) {
                FieldDefinition fieldDefinition = (FieldDefinition)i.next();
                if (!fieldDefinition.isPrimaryKey()) continue;

                if (columns.length() > 0) columns.append(", ");

                columns.append(fieldDefinition.getOriginalName());
                columns.append("=?");

                Object value = newRdn.get(fieldDefinition.getName());
                parameters.add(value);
            }

            for (Iterator i=fields.iterator(); i.hasNext(); ) {
                FieldDefinition fieldDefinition = (FieldDefinition)i.next();
                if (!fieldDefinition.isPrimaryKey()) continue;

                if (whereClause.length() > 0) whereClause.append(" and ");

                whereClause.append(fieldDefinition.getOriginalName());
                whereClause.append("=?");

                Object value = oldRdn.get(fieldDefinition.getName());
                parameters.add(value);
            }

            String sql = "update "+tableName+" set "+columns+" where "+whereClause;

            log.debug(Formatter.displaySeparator(80));
            log.debug(Formatter.displayLine("update "+tableName, 80));
            log.debug(Formatter.displayLine("set "+columns, 80));
            log.debug(Formatter.displayLine("where "+whereClause, 80));
            log.debug(Formatter.displaySeparator(80));

            ps = con.prepareStatement(sql);

            log.debug(Formatter.displayLine("Parameters:", 80));

            int c = 1;
            for (Iterator i=parameters.iterator(); i.hasNext(); c++) {
                Object value = i.next();
                ps.setObject(c, value);
                log.debug(Formatter.displayLine(" - "+c+" = "+value, 80));
            }

            log.debug(Formatter.displaySeparator(80));

            int count = ps.executeUpdate();
            if (count == 0) return LDAPException.NO_SUCH_OBJECT;

        } finally {
            if (ps != null) try { ps.close(); } catch (Exception e) {}
            if (con != null) try { con.close(); } catch (Exception e) {}
        }

        return LDAPException.SUCCESS;
    }

    public int getLastChangeNumber(SourceDefinition sourceDefinition) throws Exception {
        String tableName = sourceDefinition.getParameter(TABLE_NAME);
        String sql = "select max(changeNumber) from "+tableName+"_changes";

        java.sql.Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = ds.getConnection();

            log.debug(Formatter.displaySeparator(80));
            log.debug(Formatter.displayLine(sql, 80));
            log.debug(Formatter.displaySeparator(80));

            ps = con.prepareStatement(sql);
            rs = ps.executeQuery();

            if (!rs.next()) return 0;

            Integer value = (Integer)rs.getObject(1);
            log.debug("Last change number: "+value);
            
            if (value == null) return 0;

            return value.intValue();

        } catch (Exception e) {
            return 0;

        } finally {
            if (rs != null) try { rs.close(); } catch (Exception e) {}
            if (ps != null) try { ps.close(); } catch (Exception e) {}
            if (con != null) try { con.close(); } catch (Exception e) {}
        }
    }

    public SearchResults getChanges(SourceDefinition sourceDefinition, int lastChangeNumber) throws Exception {

        log.debug("Searching JDBC source "+sourceDefinition.getConnectionName()+"/"+sourceDefinition.getName());

        SearchResults results = new SearchResults();

        String tableName = sourceDefinition.getParameter(TABLE_NAME);
        int sizeLimit = 100;

        StringBuffer columns = new StringBuffer();
        columns.append("select changeNumber, changeTime, changeAction, changeUser, ");
        columns.append(getPkFieldNames(sourceDefinition));

        StringBuffer table = new StringBuffer();
        table.append("from ");
        table.append(tableName+"_changes");

        StringBuffer whereClause = new StringBuffer();
        whereClause.append("where changeNumber > ? order by changeNumber");

        List parameters = new ArrayList();
        parameters.add(new Integer(lastChangeNumber));

        String sql = columns+" "+table+" "+whereClause;

        java.sql.Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = ds.getConnection();

            log.debug(Formatter.displaySeparator(80));
            Collection lines = Formatter.split(sql, 80);
            for (Iterator i=lines.iterator(); i.hasNext(); ) {
                String line = (String)i.next();
                log.debug(Formatter.displayLine(line, 80));
            }
            log.debug(Formatter.displaySeparator(80));

            ps = con.prepareStatement(sql);

            log.debug(Formatter.displayLine("Parameters:", 80));

            int counter = 0;
            for (Iterator i=parameters.iterator(); i.hasNext(); ) {
                Object param = i.next();
                ps.setObject(++counter, param);
                log.debug(Formatter.displayLine(" - "+counter+" = "+param, 80));
            }

            log.debug(Formatter.displaySeparator(80));

            rs = ps.executeQuery();

            int width = 0;
            boolean first = true;

            for (int i=0; rs.next() && (sizeLimit == 0 || i<sizeLimit); i++) {
                Row row = getChanges(sourceDefinition, rs);
                results.add(row);

                if (first) {
                    width = printChangesHeader(sourceDefinition);
                    first = false;
                }

                printChanges(sourceDefinition, row);
            }

            if (width > 0) printFooter(width);

            if (rs.next()) {
                log.debug("RC: size limit exceeded.");
                results.setReturnCode(LDAPException.SIZE_LIMIT_EXCEEDED);
            }

        } finally {
            if (rs != null) try { rs.close(); } catch (Exception e) {}
            if (ps != null) try { ps.close(); } catch (Exception e) {}
            if (con != null) try { con.close(); } catch (Exception e) {}
        }

        results.close();

        return results;
    }

    public int printHeader(SourceDefinition sourceDefinition) throws Exception {

        StringBuffer resultHeader = new StringBuffer();
        resultHeader.append("|");

        Collection fields = sourceDefinition.getFieldDefinitions();
        for (Iterator j=fields.iterator(); j.hasNext(); ) {
            FieldDefinition fieldDefinition = (FieldDefinition)j.next();

            String name = fieldDefinition.getName();
            int length = fieldDefinition.getLength() > 15 ? 15 : fieldDefinition.getLength();

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

    public int printChangesHeader(SourceDefinition sourceDefinition) throws Exception {

        StringBuffer resultHeader = new StringBuffer();
        resultHeader.append("| ");
        resultHeader.append(Formatter.rightPad("#", 5));
        resultHeader.append(" | ");
        resultHeader.append(Formatter.rightPad("time", 19));
        resultHeader.append(" | ");
        resultHeader.append(Formatter.rightPad("action", 10));
        resultHeader.append(" | ");
        resultHeader.append(Formatter.rightPad("user", 10));
        resultHeader.append(" |");

        Collection fields = sourceDefinition.getPrimaryKeyFieldDefinitions();
        for (Iterator j=fields.iterator(); j.hasNext(); ) {
            FieldDefinition fieldDefinition = (FieldDefinition)j.next();

            String name = fieldDefinition.getName();
            int length = fieldDefinition.getLength() > 15 ? 15 : fieldDefinition.getLength();

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

    public void printValues(SourceDefinition sourceDefinition, AttributeValues av) throws Exception {

        Row row = new Row();

        for (Iterator i=av.getNames().iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            Collection c = av.get(name);

            Object value;
            if (c == null) {
                value = null;
            } else if (c.size() == 1) {
                value = c.iterator().next().toString();
            } else {
                value = c.toString();
            }

            row.set(name, value);
        }

        printValues(sourceDefinition, row);
    }

    public void printValues(SourceDefinition sourceDefinition, Row row) throws Exception {
        StringBuffer resultFields = new StringBuffer();
        resultFields.append("|");

        Collection fields = sourceDefinition.getFieldDefinitions();
        for (Iterator j=fields.iterator(); j.hasNext(); ) {
            FieldDefinition fieldDefinition = (FieldDefinition)j.next();

            Object value = row.get(fieldDefinition.getName());
            int length = fieldDefinition.getLength() > 15 ? 15 : fieldDefinition.getLength();

            resultFields.append(" ");
            resultFields.append(Formatter.rightPad(value == null ? "null" : value.toString(), length));
            resultFields.append(" |");
        }

        log.debug(resultFields.toString());
    }

    public void printChanges(SourceDefinition sourceDefinition, Row row) throws Exception {
        StringBuffer resultFields = new StringBuffer();
        resultFields.append("| ");
        resultFields.append(Formatter.rightPad(row.get("changeNumber").toString(), 5));
        resultFields.append(" | ");
        resultFields.append(Formatter.rightPad(row.get("changeTime").toString(), 19));
        resultFields.append(" | ");
        resultFields.append(Formatter.rightPad(row.get("changeAction").toString(), 10));
        resultFields.append(" | ");
        resultFields.append(Formatter.rightPad(row.get("changeUser").toString(), 10));
        resultFields.append(" |");

        Collection fields = sourceDefinition.getPrimaryKeyFieldDefinitions();
        for (Iterator j=fields.iterator(); j.hasNext(); ) {
            FieldDefinition fieldDefinition = (FieldDefinition)j.next();

            Object value = row.get(fieldDefinition.getName());
            int length = fieldDefinition.getLength() > 15 ? 15 : fieldDefinition.getLength();

            resultFields.append(" ");
            resultFields.append(Formatter.rightPad(value == null ? "null" : value.toString(), length));
            resultFields.append(" |");
        }

        log.debug(resultFields.toString());
    }

    public void printFooter(int width) throws Exception {
        log.debug(Formatter.displaySeparator(width));
    }

}
