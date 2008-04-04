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
package org.safehaus.penrose.jdbc;

import java.sql.*;
import java.util.*;

import org.safehaus.penrose.source.*;
import org.safehaus.penrose.util.Formatter;
import org.safehaus.penrose.ldap.LDAP;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class JDBCClient {

    public Logger log = LoggerFactory.getLogger(getClass());
    public boolean debug = log.isDebugEnabled();

    public final static String DRIVER       = "driver";
    public final static String URL          = "url";
    public final static String USER         = "user";
    public final static String PASSWORD     = "password";
    public final static String QUOTE        = "quote";

    public final static String CATALOG      = "catalog";
    public final static String SCHEMA       = "schema";
    public final static String TABLE        = "table";
    public final static String FILTER       = "filter";
    public final static String CREATE       = "create";

    public final static String INITIAL_SIZE                         = "initialSize";
    public final static String MAX_ACTIVE                           = "maxActive";
    public final static String MAX_IDLE                             = "maxIdle";
    public final static String MIN_IDLE                             = "minIdle";
    public final static String MAX_WAIT                             = "maxWait";

    public final static String VALIDATION_QUERY                     = "validationQuery";
    public final static String TEST_ON_BORROW                       = "testOnBorrow";
    public final static String TEST_ON_RETURN                       = "testOnReturn";
    public final static String TEST_WHILE_IDLE                      = "testWhileIdle";
    public final static String TIME_BETWEEN_EVICTION_RUNS_MILLIS    = "timeBetweenEvictionRunsMillis";
    public final static String NUM_TESTS_PER_EVICTION_RUN           = "numTestsPerEvictionRun";
    public final static String MIN_EVICTABLE_IDLE_TIME_MILLIS       = "minEvictableIdleTimeMillis";

    public final static String SOFT_MIN_EVICTABLE_IDLE_TIME_MILLIS  = "softMinEvictableIdleTimeMillis";
    public final static String WHEN_EXHAUSTED_ACTION                = "whenExhaustedAction";

    public Properties properties = new Properties();
    public String quote;

    Connection connection;

    public JDBCClient(Map<String,?> properties) throws Exception {

        Driver driver = null;
        String url = null;

        for (String key : properties.keySet()) {
            Object value = properties.get(key);

            if (QUOTE.equals(key)) {
                quote = (String)value;

            } else if (DRIVER.equals(key)) {
                String driverClass = (String)properties.get(DRIVER);
                Class clazz = Class.forName(driverClass);
                driver = (Driver)clazz.newInstance();

            } else if (URL.equals(key)) {
                url = (String)properties.get(URL);

            } else {
                this.properties.put(key, value);
            }
        }

        if (driver == null) throw new Exception("Missing driver.");
        if (url == null) throw new Exception("Missing URL.");

        connection = driver.connect(url, this.properties);
    }

    public JDBCClient(Driver driver, Map<String,?> properties) throws Exception {

        String url = null;

        for (String key : properties.keySet()) {
            Object value = properties.get(key);

            if (QUOTE.equals(key)) {
                quote = (String)value;

            } else if (DRIVER.equals(key)) {

            } else if (URL.equals(key)) {
                url = (String)properties.get(URL);

            } else {
                this.properties.put(key, value);
            }
        }

        connection = driver.connect(url, this.properties);
    }

    public JDBCClient(
            String driverClass,
            String url,
            String username,
            String password
    ) throws Exception {

        Class clazz = Class.forName(driverClass);
        Driver driver = (Driver)clazz.newInstance();

        properties.put(USER, username);
        properties.put(PASSWORD, password);

        connection = driver.connect(url, this.properties);
    }

    public JDBCClient(
            Driver driver,
            String url,
            String username,
            String password
    ) throws Exception {

        properties.put(USER, username);
        properties.put(PASSWORD, password);

        connection = driver.connect(url, this.properties);
    }

    public JDBCClient(Connection connection, Map<String,?> properties) throws Exception {

        this.connection = connection;

        for (String key : properties.keySet()) {
            Object value = properties.get(key);

            if (QUOTE.equals(key)) {
                quote = (String)value;

            } else {
                this.properties.put(key, value);
            }
        }
    }

    public Connection getConnection() throws Exception {
        return connection;
    }

    public void close() throws Exception {
        connection.close();
    }

    public String getTypeName(int type) throws Exception {
        java.lang.reflect.Field fields[] = Types.class.getFields();
        for (java.lang.reflect.Field field : fields) {
            if (field.getInt(null) != type) continue;
            return field.getName();
        }
        return "UNKNOWN";
    }

    public Collection<FieldConfig> getColumns(String tableName) throws Exception {
        return getColumns(null, null, tableName);
    }

    public Collection<FieldConfig> getColumns(String catalog, String schema, String tableName) throws Exception {

        log.debug("Getting column names for "+tableName+" "+catalog+" "+schema);

        Map<String,FieldConfig> columns = new HashMap<String,FieldConfig>();

        Connection connection = getConnection();
        DatabaseMetaData dmd = connection.getMetaData();

        ResultSet rs = null;

        try {
            rs = dmd.getColumns(catalog, schema, tableName, "%");

            while (rs.next()) {
                //String tableCatalog = rs.getString(1);
                //String tableSchema = rs.getString(2);
                //String tableNm = rs.getString(3);
                String columnName = rs.getString(4);
                String columnType = getTypeName(rs.getInt(5));
                int length = rs.getInt(7);
                int precision = rs.getInt(9);

                log.debug(" - "+columnName+" "+columnType+" ("+length+","+precision+")");

                FieldConfig field = new FieldConfig(columnName);
                field.setOriginalName(columnName);
                field.setType(columnType);
                field.setLength(length);
                field.setPrecision(precision);

                columns.put(columnName, field);
            }

        } finally {
            if (rs != null) try { rs.close(); } catch (Exception e) { log.error(e.getMessage(), e); }
        }

        rs = null;
        try {
            rs = dmd.getPrimaryKeys(catalog, schema, tableName);

            while (rs.next()) {
                String name = rs.getString(4);

                FieldConfig field = columns.get(name);
                field.setPrimaryKey(true);
            }

        } finally {
            if (rs != null) try { rs.close(); } catch (Exception e) { log.error(e.getMessage(), e); }
        }

        return columns.values();
    }

    public Collection<String> getCatalogs() throws Exception {

        log.debug("Getting catalogs");

        Collection<String> catalogs = new ArrayList<String>();

        Connection connection = getConnection();
        ResultSet rs = null;

        try {
            DatabaseMetaData dmd = connection.getMetaData();

            rs = dmd.getCatalogs();

            while (rs.next()) {
                String catalogName = rs.getString(1);
                log.debug(" - "+catalogName);
                catalogs.add(catalogName);
            }

        } finally {
            if (rs != null) try { rs.close(); } catch (Exception e) { log.error(e.getMessage(), e); }
        }

        return catalogs;
    }

    public Collection<String> getSchemas() throws Exception {

        log.debug("Getting schemas");

        Collection<String> schemas = new ArrayList<String>();

        Connection connection = getConnection();
        ResultSet rs = null;

        try {
            DatabaseMetaData dmd = connection.getMetaData();

            rs = dmd.getSchemas();

            while (rs.next()) {
                String schemaName = rs.getString(1);
                log.debug(" - "+schemaName);
                schemas.add(schemaName);
            }

        } finally {
            if (rs != null) try { rs.close(); } catch (Exception e) { log.error(e.getMessage(), e); }
        }

        return schemas;
    }

    public Collection<Table> getTables() throws Exception {
        return getTables(null, null);
    }

    public Collection<Table> getTables(String catalog, String schema) throws Exception {

        log.debug("Getting table names for "+catalog+" "+schema);

        Collection<Table> tables = new TreeSet<Table>();

        Connection connection = getConnection();
        ResultSet rs = null;

        try {
            DatabaseMetaData dmd = connection.getMetaData();

            // String[] tableTypes = { "TABLE", "VIEW", "ALIAS", "SYNONYM", "GLOBAL
            // TEMPORARY", "LOCAL TEMPORARY", "SYSTEM TABLE" };
            String[] tableTypes = { "TABLE", "VIEW", "ALIAS", "SYNONYM" };
            rs = dmd.getTables(catalog, schema, "%", tableTypes);

            while (rs.next()) {
                String tableCatalog = rs.getString(1);
                String tableSchema = rs.getString(2);
                String tableName = rs.getString(3);
                String tableType = rs.getString(4);
                //String remarks = rs.getString(5);

                log.debug(" - "+tableCatalog+" "+tableSchema+" "+tableName);
                Table table = new Table(tableName, tableType, tableCatalog, tableSchema);
                tables.add(table);
            }

        } finally {
            if (rs != null) try { rs.close(); } catch (Exception e) { log.error(e.getMessage(), e); }
        }

        return tables;
    }

    public int executeUpdate(
            String sql
    ) throws Exception {
        UpdateResponse response = new UpdateResponse();
        executeUpdate(sql, null, response);
        return response.getRowCount();
    }

    public int executeUpdate(
            String sql,
            Object[] parameters
    ) throws Exception {
        UpdateResponse response = new UpdateResponse();
        executeUpdate(sql, Arrays.asList(parameters), response);
        return response.getRowCount();
    }

    public int executeUpdate(
            String sql,
            Collection<Object> parameters
    ) throws Exception {
        UpdateResponse response = new UpdateResponse();
        executeUpdate(sql, parameters, response);
        return response.getRowCount();
    }

    public void executeUpdate(
            String sql,
            Collection<Object> parameters,
            UpdateResponse response
    ) throws Exception {

        if (debug) {
            log.debug(org.safehaus.penrose.util.Formatter.displaySeparator(80));
            Collection<String> lines = org.safehaus.penrose.util.Formatter.split(sql, 80);
            for (String line : lines) {
                log.debug(org.safehaus.penrose.util.Formatter.displayLine(line, 80));
            }
            log.debug(org.safehaus.penrose.util.Formatter.displaySeparator(80));

            if (parameters != null && !parameters.isEmpty()) {
                log.debug(org.safehaus.penrose.util.Formatter.displayLine("Parameters:", 80));
                int counter = 1;
                for (Object value : parameters) {
                    log.debug(org.safehaus.penrose.util.Formatter.displayLine(" - "+counter+" = "+value, 80));
                    counter++;
                }
                log.debug(org.safehaus.penrose.util.Formatter.displaySeparator(80));
            }
        }

        Connection connection = getConnection();
        PreparedStatement ps = null;

        try {
            ps = connection.prepareStatement(sql);

            if (parameters != null && !parameters.isEmpty()) {
                int counter = 1;
                for (Object value : parameters) {
                    setParameter(ps, counter, value);
                    counter++;
                }
            }

            int count = ps.executeUpdate();
            response.setRowCount(count);

        } finally {
            if (ps != null) try { ps.close(); } catch (Exception e) { log.error(e.getMessage(), e); }
        }
    }

    public void executeQuery(String sql, QueryResponse response) throws Exception {
        executeQuery(sql, (Collection<Object>)null, response);
    }

    public void executeQuery(
            String sql,
            Object[] parameters,
            QueryResponse response
    ) throws Exception {
        executeQuery(sql, Arrays.asList(parameters), response);
    }

    public void executeQuery(
            String sql,
            Collection<Object> parameters,
            QueryResponse response
    ) throws Exception {

        if (debug) {
            log.debug(org.safehaus.penrose.util.Formatter.displaySeparator(80));
            Collection<String> lines = org.safehaus.penrose.util.Formatter.split(sql, 80);
            for (String line : lines) {
                log.debug(Formatter.displayLine(line, 80));
            }
            log.debug(org.safehaus.penrose.util.Formatter.displaySeparator(80));

           if (parameters != null && !parameters.isEmpty()) {
                log.debug(org.safehaus.penrose.util.Formatter.displayLine("Parameters:", 80));
                int counter = 1;
                for (Object value : parameters) {

                    String v;
                    if (value instanceof byte[]) {
                        v = new String((byte[])value);
                    } else {
                        v = value.toString();
                    }

                    log.debug(org.safehaus.penrose.util.Formatter.displayLine(" - "+counter+" = "+v, 80));

                    counter++;
                }
                log.debug(org.safehaus.penrose.util.Formatter.displaySeparator(80));
           }
        }

        Connection connection = getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = connection.prepareStatement(sql);

            if (parameters != null && !parameters.isEmpty()) {
                int counter = 1;
                for (Object value : parameters) {
                    setParameter(ps, counter, value);
                    counter++;
                }
            }

            rs = ps.executeQuery();

            while (rs.next()) {
                if (response.isClosed()) return;
                response.add(rs);
            }

        } finally {
            if (rs != null) try { rs.close(); } catch (Exception e) { log.error(e.getMessage(), e); }
            if (ps != null) try { ps.close(); } catch (Exception e) { log.error(e.getMessage(), e); }

            response.close();
        }
    }

    public void setParameter(PreparedStatement ps, int paramIndex, Object object) throws Exception {
    	ps.setObject(paramIndex, object);
    }

    public boolean checkDatabase(String database) throws Exception {
        try {
            createDatabase(database);
            dropDatabase(database);
            return false;
            
        } catch (Exception e) {
            return true;
        }
    }

    public void createDatabase(String database) throws Exception {
        executeUpdate("create database "+database);
    }

    public void dropDatabase(String database) throws Exception {
        executeUpdate("drop database "+database);
    }

    public String getTableName(String catalog, String schema, String table)  {

        StringBuilder sb = new StringBuilder();

        if (catalog != null) {
            if (quote != null) sb.append(quote);
            sb.append(catalog);
            if (quote != null) sb.append(quote);
            sb.append(".");
        }

        if (schema != null) {
            if (quote != null) sb.append(quote);
            sb.append(schema);
            if (quote != null) sb.append(quote);
            sb.append(".");
        }

        if (quote != null) sb.append(quote);
        sb.append(table);
        if (quote != null) sb.append(quote);

        return sb.toString();
    }

    public void createTable(SourceConfig sourceConfig) throws Exception {

        String catalog = sourceConfig.getParameter(JDBCClient.CATALOG);
        String schema = sourceConfig.getParameter(JDBCClient.SCHEMA);
        String table = sourceConfig.getParameter(JDBCClient.TABLE);

        StringBuilder sb = new StringBuilder();

        sb.append("create table ");
        sb.append(getTableName(catalog, schema, table));
        sb.append(" (");

        boolean first = true;
        for (FieldConfig fieldConfig : sourceConfig.getFieldConfigs()) {

            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }

            sb.append(fieldConfig.getName());
            sb.append(" ");

            if (fieldConfig.getOriginalType() == null) {

                sb.append(fieldConfig.getType());

                if (fieldConfig.getLength() > 0) {
                    sb.append("(");
                    sb.append(fieldConfig.getLength());
                    sb.append(")");
                }

                if (fieldConfig.isCaseSensitive()) {
                    sb.append(" binary");
                }

                if (fieldConfig.isAutoIncrement()) {
                    sb.append(" auto_increment");
                }

            } else {
                sb.append(fieldConfig.getOriginalType());
            }
        }
/*
        Collection<String> indexFieldNames = sourceConfig.getIndexFieldNames();
        for (String fieldName : indexFieldNames) {
            sb.append(", index (");
            sb.append(fieldName);
            sb.append(")");
        }
*/
        Collection<String> primaryKeyNames = sourceConfig.getPrimaryKeyNames();
        if (!primaryKeyNames.isEmpty()) {
            sb.append(", primary key (");

            first = true;
            for (String fieldName : primaryKeyNames) {

                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }

                sb.append(fieldName);
            }

            sb.append(")");
        }

        sb.append(")");

        String sql = sb.toString();

        executeUpdate(sql);
    }

    public void renameTable(SourceConfig oldSourceConfig, SourceConfig newSourceConfig) throws Exception {

        String oldCatalog = oldSourceConfig.getParameter(JDBCClient.CATALOG);
        String oldSchema = oldSourceConfig.getParameter(JDBCClient.SCHEMA);
        String oldTable = oldSourceConfig.getParameter(JDBCClient.TABLE);

        String newCatalog = newSourceConfig.getParameter(JDBCClient.CATALOG);
        String newSchema = newSourceConfig.getParameter(JDBCClient.SCHEMA);
        String newTable = newSourceConfig.getParameter(JDBCClient.TABLE);

        StringBuilder sb = new StringBuilder();

        sb.append("rename table ");
        sb.append(getTableName(oldCatalog, oldSchema, oldTable));
        sb.append(" to ");
        sb.append(getTableName(newCatalog, newSchema, newTable));

        String sql = sb.toString();

        executeUpdate(sql);
    }

    public void dropTable(SourceConfig sourceConfig) throws Exception {

        String catalog = sourceConfig.getParameter(JDBCClient.CATALOG);
        String schema = sourceConfig.getParameter(JDBCClient.SCHEMA);
        String table = sourceConfig.getParameter(JDBCClient.TABLE);

        StringBuilder sb = new StringBuilder();

        sb.append("drop table ");
        sb.append(getTableName(catalog, schema, table));

        String sql = sb.toString();

        executeUpdate(sql);
    }

    public void cleanTable(SourceConfig sourceConfig) throws Exception {

        String catalog = sourceConfig.getParameter(JDBCClient.CATALOG);
        String schema = sourceConfig.getParameter(JDBCClient.SCHEMA);
        String table = sourceConfig.getParameter(JDBCClient.TABLE);

        StringBuilder sb = new StringBuilder();

        sb.append("delete from ");
        sb.append(getTableName(catalog, schema, table));

        String sql = sb.toString();

        executeUpdate(sql);
    }

    public void showStatus(final SourceConfig sourceConfig) throws Exception {

        String catalog = sourceConfig.getParameter(JDBCClient.CATALOG);
        String schema = sourceConfig.getParameter(JDBCClient.SCHEMA);
        String table = sourceConfig.getParameter(JDBCClient.TABLE);

        final String tableName = getTableName(catalog, schema, table);

        StringBuilder sb = new StringBuilder();

        sb.append("select count(*) from ");
        sb.append(tableName);

        String sql = sb.toString();

        QueryResponse response = new QueryResponse() {
            public void add(Object object) throws Exception {
                ResultSet rs = (ResultSet)object;
                log.error("Table "+tableName+": "+rs.getObject(1));
            }
        };

        executeQuery(sql, response);

        sb = new StringBuilder();

        sb.append("select ");

        boolean first = true;
        for (FieldConfig fieldConfig : sourceConfig.getFieldConfigs()) {

            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }

            sb.append("max(length(");
            sb.append(fieldConfig.getOriginalName());
            sb.append("))");
        }

        sb.append(" from ");
        sb.append(tableName);

        sql = sb.toString();

        response = new QueryResponse() {
            public void add(Object object) throws Exception {
                ResultSet rs = (ResultSet)object;

                int index = 1;
                for (FieldConfig fieldConfig : sourceConfig.getFieldConfigs()) {
                    Object length = rs.getObject(index++);
                    int maxLength = fieldConfig.getLength();
                    log.error(" - Field " + fieldConfig.getName() + ": " + length + (maxLength > 0 ? "/" + maxLength : ""));
                }
            }
        };

        executeQuery(sql, response);
    }

    public long getCount(final SourceConfig sourceConfig) throws Exception {

        String catalog = sourceConfig.getParameter(JDBCClient.CATALOG);
        String schema = sourceConfig.getParameter(JDBCClient.SCHEMA);
        String table = sourceConfig.getParameter(JDBCClient.TABLE);

        final String tableName = getTableName(catalog, schema, table);

        StringBuilder sb = new StringBuilder();

        sb.append("select count(*) from ");
        sb.append(tableName);

        String sql = sb.toString();

        QueryResponse response = new QueryResponse() {
            public void add(Object object) throws Exception {
                ResultSet rs = (ResultSet)object;
                Long count = rs.getLong(1);
                super.add(count);
            }
        };

        executeQuery(sql, response);

        if (!response.hasNext()) {
            throw LDAP.createException(LDAP.OPERATIONS_ERROR);
        }

        Long count = (Long)response.next();
        log.error("Table "+tableName+": "+count);

        return count;
    }

    public String getQuote() {
        return quote;
    }

    public void setQuote(String quote) {
        this.quote = quote;
    }
}