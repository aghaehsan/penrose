package org.safehaus.penrose.ldif.source;

import org.safehaus.penrose.filter.Filter;
import org.safehaus.penrose.filter.FilterTool;
import org.safehaus.penrose.ldap.*;
import org.safehaus.penrose.ldif.LDIFClient;
import org.safehaus.penrose.ldif.connection.LDIFConnection;
import org.safehaus.penrose.session.Session;
import org.safehaus.penrose.session.SessionManager;
import org.safehaus.penrose.source.Field;
import org.safehaus.penrose.source.Source;
import org.safehaus.penrose.util.TextUtil;

import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * @author Endi S. Dewata
 */
public class LDIFSource extends Source {

    public final static String BASE_DN           = "baseDn";
    public final static String SCOPE             = "scope";
    public final static String FILTER            = "filter";
    public final static String OBJECT_CLASSES    = "objectClasses";

    LDIFConnection connection;

    DN baseDn;
    int scope;
    Filter filter;
    String objectClasses;

    long sourceSizeLimit;
    long sourceTimeLimit;

    public LDIFSource() {
    }

    public void init() throws Exception {
        connection = (LDIFConnection)getConnection();

        baseDn = new DN(getParameter(BASE_DN));
        scope = getScope(getParameter(SCOPE));
        filter = FilterTool.parseFilter(getParameter(FILTER));

        objectClasses = getParameter(OBJECT_CLASSES);
    }

    public int getScope(String scope) {
        if ("OBJECT".equals(scope)) {
            return SearchRequest.SCOPE_BASE;

        } else if ("ONELEVEL".equals(scope)) {
            return SearchRequest.SCOPE_ONE;

        } else { // if ("SUBTREE".equals(scope)) {
            return SearchRequest.SCOPE_SUB;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Add
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void add(
            Session session,
            AddRequest request,
            AddResponse response
    ) throws Exception {

        boolean debug = log.isDebugEnabled();
        if (debug) {
            log.debug(TextUtil.displaySeparator(70));
            log.debug(TextUtil.displayLine("Add "+ getName(), 70));
            log.debug(TextUtil.displaySeparator(70));
        }

        DNBuilder db = new DNBuilder();
        db.append(request.getDn());

        db.append(baseDn);

        DN dn = db.toDn();

        Attributes attributes = (Attributes)request.getAttributes().clone();

        if (objectClasses != null) {
            Attribute ocAttribute = new Attribute("objectClass");
            for (StringTokenizer st = new StringTokenizer(objectClasses, ","); st.hasMoreTokens(); ) {
                String objectClass = st.nextToken().trim();
                ocAttribute.addValue(objectClass);
            }
            attributes.set(ocAttribute);
        }

        AddRequest newRequest = new AddRequest(request);
        newRequest.setDn(dn);
        newRequest.setAttributes(attributes);

        if (debug) log.debug("Adding entry "+dn);

        LDIFClient client = connection.getClient(session);

        try {
            client.add(newRequest, response);

        } finally {
            connection.closeClient(session);
        }

        log.debug("Add operation completed.");
     }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Bind
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void bind(
            Session session,
            BindRequest request,
            BindResponse response
    ) throws Exception {

        boolean debug = log.isDebugEnabled();
        if (debug) {
            log.debug(TextUtil.displaySeparator(70));
            log.debug(TextUtil.displayLine("Bind "+getName(), 70));
            log.debug(TextUtil.displaySeparator(70));
        }

        DNBuilder db = new DNBuilder();
        db.append(request.getDn());

        db.append(baseDn);

        DN dn = db.toDn();

        BindRequest newRequest = (BindRequest)request.clone();
        newRequest.setDn(dn);

        if (debug) log.debug("Binding as "+dn);

        LDIFClient client = connection.getClient(session);

        try {
            client.bind(newRequest, response);

        } finally {
            connection.closeClient(session);
        }

        log.debug("Bind operation completed.");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Compare
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void compare(
            Session session,
            CompareRequest request,
            CompareResponse response
    ) throws Exception {

        boolean debug = log.isDebugEnabled();
        if (debug) {
            log.debug(TextUtil.displaySeparator(70));
            log.debug(TextUtil.displayLine("Compare "+ getName(), 70));
            log.debug(TextUtil.displaySeparator(70));
        }

        DNBuilder db = new DNBuilder();
        db.append(request.getDn());

        db.append(baseDn);

        DN dn = db.toDn();

        CompareRequest newRequest = (CompareRequest)request.clone();
        newRequest.setDn(dn);

        if (debug) log.debug("Comparing entry "+dn);

        LDIFClient client = connection.getClient(session);

        try {
            client.compare(newRequest, response);

        } finally {
            connection.closeClient(session);
        }

        log.debug("Compare operation completed.");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Delete
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void delete(
            Session session,
            DeleteRequest request,
            DeleteResponse response
    ) throws Exception {

        boolean debug = log.isDebugEnabled();
        if (debug) {
            log.debug(TextUtil.displaySeparator(70));
            log.debug(TextUtil.displayLine("Delete "+ getName(), 70));
            log.debug(TextUtil.displaySeparator(70));
        }

        DNBuilder db = new DNBuilder();
        db.append(request.getDn());

        db.append(baseDn);

        DN dn = db.toDn();

        DeleteRequest newRequest = new DeleteRequest(request);
        newRequest.setDn(dn);

        if (debug) log.debug("Deleting entry "+dn);

        LDIFClient client = connection.getClient(session);

        try {
            client.delete(newRequest, response);

        } finally {
            connection.closeClient(session);
        }

        log.debug("Delete operation completed.");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Modify
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void modify(
            Session session,
            ModifyRequest request,
            ModifyResponse response
    ) throws Exception {

        boolean debug = log.isDebugEnabled();
        if (debug) {
            log.debug(TextUtil.displaySeparator(70));
            log.debug(TextUtil.displayLine("Modify "+getName(), 70));
            log.debug(TextUtil.displaySeparator(70));
        }

        DNBuilder db = new DNBuilder();
        db.append(request.getDn());

        db.append(baseDn);

        DN dn = db.toDn();

        ModifyRequest newRequest = new ModifyRequest(request);
        newRequest.setDn(dn);

        if (debug) log.debug("Modifying entry "+dn);

        LDIFClient client = connection.getClient(session);

        try {
            client.modify(newRequest, response);

        } finally {
            connection.closeClient(session);
        }

        log.debug("Modify operation completed.");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // ModRDN
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void modrdn(
            Session session,
            ModRdnRequest request,
            ModRdnResponse response
    ) throws Exception {

        boolean debug = log.isDebugEnabled();
        if (debug) {
            log.debug(TextUtil.displaySeparator(70));
            log.debug(TextUtil.displayLine("ModRdn "+getName(), 70));
            log.debug(TextUtil.displaySeparator(70));
        }

        DNBuilder db = new DNBuilder();
        db.append(request.getDn());
        db.append(baseDn);
        DN targetDn = db.toDn();

        ModRdnRequest newRequest = new ModRdnRequest(request);
        newRequest.setDn(targetDn);

        if (debug) log.debug("Renaming entry "+targetDn);

        LDIFClient client = connection.getClient(session);

        try {
            client.modrdn(newRequest, response);

        } finally {
            connection.closeClient(session);
        }

        log.debug("ModRdn operation completed.");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Search
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void search(
            final Session session,
            final SearchRequest request,
            final SearchResponse response
    ) throws Exception {

        final boolean debug = log.isDebugEnabled();
        if (debug) {
            log.debug(TextUtil.displaySeparator(70));
            log.debug(TextUtil.displayLine("Search "+getName(), 70));
            log.debug(TextUtil.displaySeparator(70));
        }

        final DN baseDn = createBaseDn(request);
        final int scope = createScope(request);
        final Filter filter = createFilter(request);

        long sizeLimit = createSizeLimit(request);
        long timeLimit = createTimeLimit(request);

        // Create new request to ensure all attributes returned
        SearchRequest newRequest = new SearchRequest();
        newRequest.setDn(baseDn);
        newRequest.setScope(scope);
        newRequest.setFilter(filter);
        newRequest.setSizeLimit(sizeLimit);
        newRequest.setTimeLimit(timeLimit);
        newRequest.setControls(request.getControls());

        SearchResponse newResponse = new SearchResponse() {
            public void add(SearchResult searchResult) throws Exception {

                if (response.isClosed()) {
                    close();
                    return;
                }

                SearchResult newSearchResult = createSearchResult(baseDn, searchResult);
                if (newSearchResult == null) return;

                if (debug) {
                    newSearchResult.print();
                }

                response.add(newSearchResult);
            }
        };

        newResponse.setSizeLimit(request.getSizeLimit());

        if (debug) log.debug("Searching entry "+baseDn);

        LDIFClient client = connection.getClient(session);

        try {
            client.search(newRequest, newResponse);

        } finally {
            response.close();
            connection.closeClient(session);
        }

        log.debug("Search operation completed.");
    }

    public DN createBaseDn(SearchRequest request) throws Exception {

        DNBuilder db = new DNBuilder();
        db.append(request.getDn());
        db.append(baseDn);
        return db.toDn();
    }

    public int createScope(SearchRequest request) throws Exception {

        if (scope == SearchRequest.SCOPE_BASE) {
            return SearchRequest.SCOPE_BASE;

        } else if (scope == SearchRequest.SCOPE_ONE) {
            DN baseDn = request.getDn();

            if (baseDn != null && !baseDn.isEmpty()) {
                return SearchRequest.SCOPE_BASE;

            } else {
                return SearchRequest.SCOPE_ONE;
            }

        } else {
            return request.getScope();
        }
    }

    public Filter createFilter(final SearchRequest request) throws Exception {

        return FilterTool.appendAndFilter(request.getFilter(), this.filter);
/*
        DN baseDn = request.getDn();
        int scope = request.getScope();
        Filter filter = request.getFilter();

        Filter newFilter = null;

        if (scope == SearchRequest.SCOPE_BASE && baseDn != null) { // use RDN to create filter
            RDN rdn = baseDn.getRdn();
            for (String name : rdn.getNames()) {

                Field field = getField(name);
                if (field == null) return filter;

                Object value = rdn.get(name);

                Filter f = new SimpleFilter(field.getOriginalName(), "=", value);
                newFilter = FilterTool.appendAndFilter(newFilter, f);
            }
        }

        // extract attribute values
        LDAPSourceFilterProcessor fp = new LDAPSourceFilterProcessor(this);
        fp.process(filter);

        for (Field field : getFields()) {
            ItemFilter fieldFilter = fp.getFilter(field);
            if (fieldFilter == null) continue;

            ItemFilter f = (ItemFilter)fieldFilter.clone();
            f.setAttribute(field.getOriginalName());

            newFilter = FilterTool.appendAndFilter(newFilter, f);
        }

        newFilter = FilterTool.appendAndFilter(newFilter, this.filter);

        return newFilter;
*/
    }
/*
    public Filter createFilter(
            final Session session,
            final SearchRequest request,
            final SearchResponse response
    ) throws Exception {

        DN baseDn = request.getDn();
        int scope = request.getScope();
        Filter filter = request.getFilter();

        Filter newFilter;
##
            DNBuilder db = new DNBuilder();
            db.append(request.getDn());
            db.append(sourceBaseDn);
            newBaseDn = db.toDn();

            if ("OBJECT".equals(sourceScope)) {
                newScope = SearchRequest.SCOPE_BASE;

            } else if ("ONELEVEL".equals(sourceScope)) {
                if (request.getDn() == null) {
                    newScope = SearchRequest.SCOPE_ONE;
                } else {
                    newScope = SearchRequest.SCOPE_BASE;
                }

            } else { //if ("SUBTREE".equals(sourceScope)) {
                newScope = SearchRequest.SCOPE_SUB;
            }

            newFilter = filter == null ? null : (Filter)filter.clone();

            FilterProcessor fp = new FilterProcessor() {
                public void process(Stack<Filter> path, Filter filter) throws Exception {
                    if (!(filter instanceof SimpleFilter)) {
                        super.process(path, filter);
                        return;
                    }

                    SimpleFilter sf = (SimpleFilter)filter;

                    String attribute = sf.getAttribute();
                    Field field = LDAPSource.this.getField(attribute);
                    if (field == null) return;

                    sf.setAttribute(field.getOriginalName());
                }
            };

            fp.process(newFilter);

            newFilter = FilterTool.appendAndFilter(newFilter, sourceFilter);
##
        newFilter = filter == null ? null : (Filter)filter.clone();

        if (scope == SearchRequest.SCOPE_BASE) {
            RDN rdn = baseDn.getRdn();
            for (String name : rdn.getNames()) {
                Object value = rdn.get(name);
                newFilter = FilterTool.appendAndFilter(newFilter, new SimpleFilter(name, "=", value));
            }
        }

        FilterProcessor fp = new FilterProcessor() {
            public Filter process(Stack<Filter> path, Filter filter) throws Exception {
                if (!(filter instanceof SimpleFilter)) {
                    return super.process(path, filter);
                }

                SimpleFilter sf = (SimpleFilter)filter;

                String attribute = sf.getAttribute();
                Field field = LDAPSource.this.getField(attribute);
                if (field == null) return filter;

                sf.setAttribute(field.getOriginalName());

                return filter;
            }
        };

        fp.process(newFilter);

        return FilterTool.appendAndFilter(newFilter, sourceFilter);
    }
*/
    public long createSizeLimit(SearchRequest request) {
        long sizeLimit = request.getSizeLimit();
        if (sourceSizeLimit > sizeLimit) sizeLimit = sourceSizeLimit;
        return sizeLimit;
    }

    public long createTimeLimit(SearchRequest request) {
        long timeLimit = request.getTimeLimit();
        if (sourceTimeLimit > timeLimit) timeLimit = sourceTimeLimit;
        return timeLimit;
    }

    public SearchResult createSearchResult(
            DN baseDn,
            SearchResult sr
    ) throws Exception {

        boolean debug = log.isDebugEnabled();
        DN dn = sr.getDn();
        DN newDn = dn.getPrefix(baseDn);
        if (debug) log.debug("Creating search result ["+newDn+"]");

        Attributes attributes = sr.getAttributes();
        Attributes newAttributes;

        if (getFields().isEmpty()) {
            newAttributes = (Attributes)attributes.clone();

        } else {
            newAttributes = new Attributes();

            RDN rdn = dn.getRdn();

            if (rdn != null) {
                for (String name : rdn.getNames()) {
                    Object value = rdn.get(name);
                    newAttributes.addValue("primaryKey." + name, value);
                }
            }

            for (Field field : getFields()) {

                String fieldName = field.getName();
                String originalName = field.getOriginalName();

                if ("dn".equals(originalName)) {
                    newAttributes.addValue(fieldName, dn.toString());

                } else {
                    Attribute attr = attributes.remove(originalName);
                    if (attr == null) continue;

                    newAttributes.addValues(fieldName, attr.getValues());
                }
            }
        }

        return new SearchResult(newDn, newAttributes);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Storage
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void create() throws Exception {

        boolean debug = log.isDebugEnabled();
        if (debug) {
            log.debug(TextUtil.displaySeparator(70));
            log.debug(TextUtil.displayLine("Create "+getName(), 70));
            log.debug(TextUtil.displaySeparator(70));
        }
    }

    public void rename(Source newSource) throws Exception {

        boolean debug = log.isDebugEnabled();
        if (debug) {
            log.debug(TextUtil.displaySeparator(70));
            log.debug(TextUtil.displayLine("Rename "+getName(), 70));
            log.debug(TextUtil.displaySeparator(70));
        }
    }

    public void clear(Session session) throws Exception {

        boolean debug = log.isDebugEnabled();
        if (debug) {
            log.debug(TextUtil.displaySeparator(70));
            log.debug(TextUtil.displayLine("Clear "+getName(), 70));
            log.debug(TextUtil.displaySeparator(70));
        }

        final ArrayList<DN> dns = new ArrayList<DN>();

        SearchRequest request = new SearchRequest();

        SearchResponse response = new SearchResponse() {
            public void add(SearchResult result) throws Exception {
                DN dn = result.getDn();
                if (scope == SearchRequest.SCOPE_ONE && baseDn.matches(dn)) return;
                dns.add(dn);
            }
        };

        search(session, request, response);

        for (int i=dns.size()-1; i>=0; i--) {
            DN dn = dns.get(i);
            delete(session, dn);
        }
    }

    public void drop() throws Exception {

        boolean debug = log.isDebugEnabled();
        if (debug) {
            log.debug(TextUtil.displaySeparator(70));
            log.debug(TextUtil.displayLine("Drop "+getName(), 70));
            log.debug(TextUtil.displaySeparator(70));
        }
    }

    public long getCount(Session session) throws Exception {

        boolean debug = log.isDebugEnabled();
        if (debug) {
            log.debug(TextUtil.displaySeparator(70));
            log.debug(TextUtil.displayLine("Count "+sourceConfig.getName(), 70));
            log.debug(TextUtil.displaySeparator(70));
        }

        SearchRequest request = new SearchRequest();

        String baseDn = getParameter(BASE_DN);
        request.setDn(baseDn);

        String scope = getParameter(SCOPE);
        request.setScope(getScope(scope));

        String filter = getParameter(FILTER);
        request.setFilter(filter);

        request.setAttributes(new String[] { "dn" });
        request.setTypesOnly(true);

        SearchResponse response = new SearchResponse();

        LDIFClient client = connection.getClient(session);

        try {
            client.search(request, response);
            return response.getTotalCount();

        } finally {
            connection.closeClient(session);
        }
    }

    public Session createAdminSession() throws Exception {
        SessionManager sessionManager = partition.getPartitionContext().getSessionManager();
        return sessionManager.createAdminSession();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Clone
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Object clone() throws CloneNotSupportedException {

        LDIFSource source = (LDIFSource)super.clone();

        source.connection       = connection;

        return source;
    }

    public DN getBaseDn() {
        return baseDn;
    }

    public void setBaseDn(DN baseDn) {
        this.baseDn = baseDn;
    }

    public int getScope() {
        return scope;
    }

    public void setScope(int scope) {
        this.scope = scope;
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    public String getObjectClasses() {
        return objectClasses;
    }

    public void setObjectClasses(String objectClasses) {
        this.objectClasses = objectClasses;
    }
}