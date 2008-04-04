package org.safehaus.penrose.jdbc.connection;

import org.safehaus.penrose.filter.*;
import org.safehaus.penrose.partition.Partition;
import org.safehaus.penrose.source.FieldConfig;
import org.safehaus.penrose.source.SourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Endi S. Dewata
 */
public class JDBCFilterBuilder {

    public Logger log = LoggerFactory.getLogger(getClass());
    public boolean debug = log.isDebugEnabled();

    protected Map<String,String> sourceNames = new LinkedHashMap<String,String>(); // need to maintain order

    Partition partition;
    private String sql;
    private Collection<Object> parameters = new ArrayList<Object>();

    private String quote;
    private boolean extractValues = true;
    private boolean appendSourceAlias = true;

    private boolean allowCaseSensitive = true;

    public JDBCFilterBuilder(Partition partition) throws Exception {
        this.partition = partition;
    }

    public void generate(Filter filter) throws Exception {
        StringBuilder sb = new StringBuilder();
        generate(filter, sb);
        sql = sb.toString();
    }

    public void generate(Filter filter, StringBuilder sb) throws Exception {

        if (filter instanceof NotFilter) {
            generate((NotFilter)filter, sb);

        } else if (filter instanceof AndFilter) {
            generate((AndFilter)filter, sb);

        } else if (filter instanceof OrFilter) {
            generate((OrFilter)filter, sb);

        } else if (filter instanceof SimpleFilter) {
            generate((SimpleFilter)filter, sb);

        } else if (filter instanceof SubstringFilter) {
            generate((SubstringFilter)filter, sb);

        } else if (filter instanceof PresentFilter) {
            generate((PresentFilter)filter, sb);
        }
    }

    public void generate(
            SubstringFilter filter,
            StringBuilder sb
    ) throws Exception {

        String name = filter.getAttribute();
        Collection<Object> substrings = filter.getSubstrings();

        if (debug) {
            log.debug("Substring Filter: "+name+" like "+substrings);
        }

        StringBuilder sb1 = new StringBuilder();

        String lsourceAlias;
        String lfieldName;

        int i = name.indexOf('.');
        if (i < 0) {
            lsourceAlias = sourceNames.keySet().iterator().next();
            lfieldName = name;
        } else {
            lsourceAlias = name.substring(0, i);
            lfieldName = name.substring(i+1);
        }

        if (appendSourceAlias) {
            sb1.append(lsourceAlias);
            sb1.append(".");
        }

        String lsourceName = sourceNames.get(lsourceAlias);
        SourceConfig ls = partition.getPartitionConfig().getSourceConfigManager().getSourceConfig(lsourceName);
        //SourceRef lsourceRef = sourceRefs.get(lsourceName);
        //Source ls = lsourceRef.getSource();

        //Field lField = ls.getField(lfieldName);
        FieldConfig lField = ls.getFieldConfig(lfieldName);
        if (lField == null) throw new Exception("Unknown field: "+name);

        if (quote != null) sb1.append(quote);
        sb1.append(lField.getOriginalName());
        if (quote != null) sb1.append(quote);

        String lhs = sb1.toString();

        StringBuilder sb2 = new StringBuilder();

        for (Object o : substrings) {
            if (o.equals(SubstringFilter.STAR)) {
                sb2.append('%');
            } else {
                sb2.append(o);
            }
        }

        String value = sb2.toString();

        String rhs;

        if (extractValues) {

            if (lField.getCastType() != null) {
                StringBuilder sb3 = new StringBuilder();
                sb3.append("cast(? as ");
                sb3.append(lField.getCastType());

                if (lField.getLength() > 0) {
                    sb3.append("(");
                    sb3.append(lField.getLength());
                    sb3.append(")");
                }

                sb3.append(")");
                rhs = sb3.toString();

            } else {
                rhs = "?";
            }

            parameters.add(value);

        } else {
            rhs = "\""+value+"\"";
        }

        if (allowCaseSensitive && lField.isText() && !lField.isCaseSensitive()) {
            sb.append("lower(");
            sb.append(lhs);
            sb.append(") like lower(");
            sb.append(rhs);
            sb.append(")");

        } else {
            sb.append(lhs);
            sb.append(" like ");
            sb.append(rhs);
        }
    }

    public void generate(
            SimpleFilter filter,
            StringBuilder sb
    ) throws Exception {

        String name = filter.getAttribute();
        String operator = filter.getOperator();
        Object value = filter.getValue();

        if (debug) {
            String v;
            if (value instanceof byte[]) {
                v = new String((byte[])value);
            } else {
                v = value.toString();
            }
            
            log.debug("Simple Filter: "+name+" "+operator+" "+v);
        }

        StringBuilder sb1 = new StringBuilder();

        String lsourceAlias;
        String lfieldName;

        int i = name.indexOf('.');
        if (i < 0) {
            lsourceAlias = sourceNames.keySet().iterator().next();
            lfieldName = name;
        } else {
            lsourceAlias = name.substring(0, i);
            lfieldName = name.substring(i+1);
        }

        if (appendSourceAlias) {
            sb1.append(lsourceAlias);
            sb1.append(".");
        }

        String lsourceName = sourceNames.get(lsourceAlias);
        SourceConfig ls = partition.getPartitionConfig().getSourceConfigManager().getSourceConfig(lsourceName);
        //SourceRef lsourceRef = sourceRefs.get(lsourceAlias);
        //Source ls = lsourceRef.getSource();

        //Field lField = ls.getField(lfieldName);
        FieldConfig lField = ls.getFieldConfig(lfieldName);
        if (lField == null) throw new Exception("Unknown field: "+name);

        if (quote != null) sb1.append(quote);
        sb1.append(lField.getOriginalName());
        if (quote != null) sb1.append(quote);

        String lhs = sb1.toString();

        String rhs;

        if (extractValues) {

            if (lField.getCastType() != null) {
                StringBuilder sb3 = new StringBuilder();
                sb3.append("cast(? as ");
                sb3.append(lField.getCastType());

                if (lField.getLength() > 0) {
                    sb3.append("(");
                    sb3.append(lField.getLength());
                    sb3.append(")");
                }

                sb3.append(")");
                rhs = sb3.toString();

            } else {
                rhs = "?";
            }

            parameters.add(value);

        } else {
            rhs = value.toString();

            int j = rhs.indexOf('.');
            String rsourceAlias = rhs.substring(0, j);
            String rfieldName = rhs.substring(j+1);

            String rsourceName = sourceNames.get(rsourceAlias);
            SourceConfig rs = partition.getPartitionConfig().getSourceConfigManager().getSourceConfig(rsourceName);
            //SourceRef rsourceRef = sourceRefs.get(rsourceAlias);
            //Source rs = rsourceRef.getSource();

            //Field rField = rs.getField(rfieldName);
            FieldConfig rField = rs.getFieldConfig(rfieldName);
            if (rField == null) throw new Exception("Unknown field: "+rhs);

            StringBuilder sb2 = new StringBuilder();
            sb2.append(rsourceAlias);
            sb2.append(".");

            if (quote != null) sb2.append(quote);
            sb2.append(rField.getOriginalName());
            if (quote != null) sb2.append(quote);

            rhs = sb2.toString();
        }

        if (allowCaseSensitive && lField.isText() && !lField.isCaseSensitive()) {
            sb.append("lower(");
            sb.append(lhs);
            sb.append(") ");
            sb.append(operator);
            sb.append(" lower(");
            sb.append(rhs);
            sb.append(")");

        } else {
            sb.append(lhs);
            sb.append(" ");
            sb.append(operator);
            sb.append(" ");
            sb.append(rhs);
        }
    }

    public void generate(
            PresentFilter filter,
            StringBuilder sb
    ) throws Exception {

        String name = filter.getAttribute();

        sb.append(name);
        sb.append(" is not null");
    }

    public void generate(
            NotFilter filter,
            StringBuilder sb
    ) throws Exception {

        StringBuilder sb2 = new StringBuilder();

        Filter f = filter.getFilter();

        generate(
                f,
                sb2
        );

        sb.append("not (");
        sb.append(sb2);
        sb.append(")");
    }

    public void generate(
            AndFilter filter,
            StringBuilder sb
    ) throws Exception {

        StringBuilder sb2 = new StringBuilder();
        for (Filter f : filter.getFilters()) {

            StringBuilder sb3 = new StringBuilder();

            generate(
                    f,
                    sb3
            );

            if (sb2.length() > 0 && sb3.length() > 0) {
                sb2.append(" and ");
            }

            sb2.append(sb3);
        }

        if (sb2.length() == 0) return;

        sb.append("(");
        sb.append(sb2);
        sb.append(")");
    }

    public void generate(
            OrFilter filter,
            StringBuilder sb
    ) throws Exception {

        StringBuilder sb2 = new StringBuilder();
        for (Filter f : filter.getFilters()) {

            StringBuilder sb3 = new StringBuilder();

            generate(
                    f,
                    sb3
            );

            if (sb2.length() > 0 && sb3.length() > 0) {
                sb2.append(" or ");
            }

            sb2.append(sb3);
        }

        if (sb2.length() == 0) return;

        sb.append("(");
        sb.append(sb2);
        sb.append(")");
    }

    public Collection<String> getSourceAliases() {
        return sourceNames.keySet();
    }

    public void addSourceName(String alias, String sourceName) {
        sourceNames.put(alias, sourceName);
    }

    public Collection<Object> getParameters() {
        return parameters;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getQuote() {
        return quote;
    }

    public void setQuote(String quote) {
        this.quote = quote;
    }

    public boolean isExtractValues() {
        return extractValues;
    }

    public void setExtractValues(boolean extractValues) {
        this.extractValues = extractValues;
    }

    public boolean isAppendSourceAlias() {
        return appendSourceAlias;
    }

    public void setAppendSourceAlias(boolean appendSourceAlias) {
        this.appendSourceAlias = appendSourceAlias;
    }

    public boolean isAllowCaseSensitive() {
        return allowCaseSensitive;
    }

    public void setAllowCaseSensitive(boolean allowCaseSensitive) {
        this.allowCaseSensitive = allowCaseSensitive;
    }
}
