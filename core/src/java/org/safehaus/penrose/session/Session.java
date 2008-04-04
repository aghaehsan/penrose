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
package org.safehaus.penrose.session;

import org.ietf.ldap.LDAPException;
import org.safehaus.penrose.config.PenroseConfig;
import org.safehaus.penrose.event.*;
import org.safehaus.penrose.filter.Filter;
import org.safehaus.penrose.filter.FilterTool;
import org.safehaus.penrose.ldap.*;
import org.safehaus.penrose.log.Access;
import org.safehaus.penrose.naming.PenroseContext;
import org.safehaus.penrose.partition.Partition;
import org.safehaus.penrose.partition.PartitionManager;
import org.safehaus.penrose.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Endi S. Dewata
 */
public class Session {

    public Logger log = LoggerFactory.getLogger(getClass());
    public boolean warn = log.isWarnEnabled();
    public boolean debug = log.isDebugEnabled();

    public final static String EVENTS_ENABLED              = "eventsEnabled";
    public final static String SEARCH_RESPONSE_BUFFER_SIZE = "searchResponseBufferSize";

    private PenroseConfig penroseConfig;
    private PenroseContext penroseContext;
    private SessionContext sessionContext;

    private SessionManager sessionManager;
    private EventManager eventManager;

    private Object sessionId;

    private DN bindDn;
    private boolean rootUser;

    private Map<String,Object> attributes = new HashMap<String,Object>();
    
    protected boolean eventsEnabled = true;
    protected long bufferSize;

    private Collection<SessionListener> listeners = new ArrayList<SessionListener>();

    public Session(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public void init() {

        log.debug("----------------------------------------------------------------------------------");
        if (warn) log.warn("Creating session "+sessionId+".");

        String s = penroseConfig.getProperty(EVENTS_ENABLED);
        eventsEnabled = s == null || Boolean.valueOf(s);

        s = penroseConfig.getProperty(SEARCH_RESPONSE_BUFFER_SIZE);
        bufferSize = s == null ? 0 : Long.parseLong(s);
    }

    public DN getBindDn() {
        return bindDn;
    }

    public void setBindDn(String bindDn) {
        this.bindDn = new DN(bindDn);
    }

    public void setBindDn(DN bindDn) {
        this.bindDn = bindDn;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Add
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void add(String dn, Attributes attributes) throws LDAPException {
        add(new DN(dn), attributes);
    }

    public void add(DN dn, Attributes attributes) throws LDAPException {
        AddRequest request = new AddRequest();
        request.setDn(dn);
        request.setAttributes(attributes);

        AddResponse response = new AddResponse();

        add(request, response);
    }
    
    public void add(AddRequest request, AddResponse response) throws LDAPException {

        try {
            DN dn = request.getDn();

            PartitionManager partitionManager = penroseContext.getPartitionManager();
            Partition partition = partitionManager.getPartition(dn);
            add(partition, request, response);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw LDAP.createException(e);
        }
    }

    public void add(
            Partition partition,
            AddRequest request,
            AddResponse response
    ) throws LDAPException {
        try {
            Access.log(this, request);

            if (debug) {
                log.debug("----------------------------------------------------------------------------------");
                log.debug("ADD:");
                log.debug(" - Bind DN : "+(bindDn == null ? "" : bindDn));
                log.debug(" - Entry   : "+request.getDn());
                log.debug("");

                log.debug("Attributes:");
                request.getAttributes().print();
                log.debug("");

                log.debug("Controls: "+request.getControls());
                log.debug("");
            }

            if (eventsEnabled) {
            	AddEvent beforeModifyEvent = new AddEvent(this, AddEvent.BEFORE_ADD, this, partition, request, response);
            	eventManager.postEvent(beforeModifyEvent);
            }

            try {
                partition.add(this, request, response);

            } catch (LDAPException e) {
                response.setException(e);
                throw e;

            } finally {
                if (eventsEnabled) {
                    AddEvent addEvent = new AddEvent(this, AddEvent.AFTER_ADD, this, partition, request, response);
                    eventManager.postEvent(addEvent);
                }
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            response.setException(e);
            throw response.getException();

        } finally {
            Access.log(this, response);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Bind
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void bind(String dn, String password) throws LDAPException {
        bind(new DN(dn), password == null ? null : password.getBytes());
    }

    public void bind(String dn, byte[] password) throws LDAPException {
    	bind(new DN(dn), password);
    }

    public void bind(DN dn, String password) throws LDAPException {
        bind(dn, password.getBytes());
    }

    public void bind(DN dn, byte[] password) throws LDAPException {
        BindRequest request = new BindRequest();
        request.setDn(dn);
        request.setPassword(password);

        BindResponse response = new BindResponse();

        bind(request, response);

        LDAPException exception = response.getException();
        if (exception.getResultCode() != LDAP.SUCCESS) {
            throw exception;
        }
    }

    public void bind(BindRequest request, BindResponse response) throws LDAPException {

        try {
            DN dn = request.getDn();

            PartitionManager partitionManager = penroseContext.getPartitionManager();
            Partition partition = partitionManager.getPartition(dn);
            bind(partition, request, response);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw LDAP.createException(e);
        }
    }

    public void bind(
            Partition partition,
            BindRequest request,
            BindResponse response
    ) throws LDAPException {
        try {
            Access.log(this, request);

            if (debug) {
                log.debug("----------------------------------------------------------------------------------");
                log.debug("BIND:");
                log.debug(" - Bind DN       : "+request.getDn());
                log.debug(" - Bind Password : "+(request.getPassword() == null ? "" : new String(request.getPassword())));
                log.debug("");

                log.debug("Controls: "+request.getControls());
            }

            DN dn = request.getDn();
            byte[] password = request.getPassword();

            if (eventsEnabled) {
            	BindEvent beforeBindEvent = new BindEvent(this, BindEvent.BEFORE_BIND, this, partition, request, response);
            	eventManager.postEvent(beforeBindEvent);
        	}
            
            if (dn.isEmpty() || password == null || password.length == 0) {
                log.debug("Bound as anonymous user.");
                bindDn = null;
                rootUser = false;
                return;
            }

            DN rootDn = penroseConfig.getRootDn();
            byte[] rootPassword = penroseConfig.getRootPassword();

            if (rootDn.matches(dn)) {
                if (PasswordUtil.comparePassword(password, rootPassword)) {
                    log.debug("Bound as root user.");
                    bindDn = rootDn;
                    rootUser = true;

                } else {
                    log.debug("Root password doesn't match.");
                    response.setException(LDAP.createException(LDAP.INVALID_CREDENTIALS));
                }
                return;
            }

            try {
                partition.bind(this, request, response);

                if (response.getReturnCode() == LDAP.SUCCESS) {
                    log.debug("Bound as "+dn);
                    bindDn = dn;
                    rootUser = false;

                } else {
                    log.debug("Bind failed.");
                }

            } catch (LDAPException e) {
                response.setException(e);
                throw e;

            } finally {
                if (eventsEnabled) {
                	BindEvent afterBindEvent = new BindEvent(this, BindEvent.AFTER_BIND, this, partition, request, response);
                	eventManager.postEvent(afterBindEvent);
                }
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            response.setException(e);
            throw response.getException();

        } finally {
            Access.log(this, response);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Compare
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean compare(String dn, String attributeName, Object attributeValue) throws LDAPException {
        return compare(new DN(dn), attributeName, attributeValue);
    }

    public boolean compare(DN dn, String attributeName, Object attributeValue) throws LDAPException {
        CompareRequest request = new CompareRequest();
        request.setDn(dn);
        request.setAttributeName(attributeName);
        request.setAttributeValue(attributeValue);

        CompareResponse response = new CompareResponse();

        compare(request, response);

        return response.getReturnCode() == LDAP.COMPARE_TRUE;
    }

    public void compare(CompareRequest request, CompareResponse response) throws LDAPException {

        try {
            DN dn = request.getDn();

            PartitionManager partitionManager = penroseContext.getPartitionManager();
            Partition partition = partitionManager.getPartition(dn);
            compare(partition, request, response);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw LDAP.createException(e);
        }
    }

    public void compare(
            Partition partition,
            CompareRequest request,
            CompareResponse response
    ) throws LDAPException {
        try {
            Access.log(this, request);

            if (debug) {
                log.debug("----------------------------------------------------------------------------------");
                log.debug("COMPARE:");
                log.debug(" - Bind DN         : "+(bindDn == null ? "" : bindDn));
                log.debug(" - DN              : "+request.getDn());
                log.debug(" - Attribute Name  : "+request.getAttributeName());

                Object attributeValue = request.getAttributeValue();

                Object value;
                if (attributeValue instanceof byte[]) {
                    //value = BinaryUtil.encode(BinaryUtil.BIG_INTEGER, (byte[])attributeValue);
                    value = new String((byte[])attributeValue);
                } else {
                    value = attributeValue;
                }

                log.debug(" - Attribute Value : "+value);
                log.debug("");

                log.debug("Controls: "+request.getControls());
            }

            if (eventsEnabled) {
            	CompareEvent beforeCompareEvent = new CompareEvent(this, CompareEvent.BEFORE_COMPARE, this, partition, request, response);
            	eventManager.postEvent(beforeCompareEvent);
            }

            try {
                partition.compare(this, request, response);

            } catch (LDAPException e) {
                response.setException(e);
                throw e;

            } finally {
                if (eventsEnabled) {
                	CompareEvent afterCompareEvent = new CompareEvent(this, CompareEvent.AFTER_COMPARE, this, partition, request, response);
                	eventManager.postEvent(afterCompareEvent);
                }
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            response.setException(e);
            throw response.getException();

        } finally {
            Access.log(this, response);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Delete
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void delete(String dn) throws LDAPException {
        delete(new DN(dn));
    }

    public void delete(DN dn) throws LDAPException {
        DeleteRequest request = new DeleteRequest();
        request.setDn(dn);

        DeleteResponse response = new DeleteResponse();

        delete(request, response);
    }

    public void delete(DeleteRequest request, DeleteResponse response) throws LDAPException {

        try {
            DN dn = request.getDn();

            PartitionManager partitionManager = penroseContext.getPartitionManager();
            Partition partition = partitionManager.getPartition(dn);
            delete(partition, request, response);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw LDAP.createException(e);
        }
    }

    public void delete(
            Partition partition,
            DeleteRequest request,
            DeleteResponse response
    ) throws LDAPException {
        try {
            Access.log(this, request);

            if (debug) {
                log.debug("----------------------------------------------------------------------------------");
                log.debug("DELETE:");
                log.debug(" - Bind DN : "+(bindDn == null ? "" : bindDn));
                log.debug(" - DN      : "+request.getDn());
                log.debug("");

                log.debug("Controls: "+request.getControls());
            }

            if (eventsEnabled) {
            	DeleteEvent beforeDeleteEvent = new DeleteEvent(this, DeleteEvent.BEFORE_DELETE, this, partition, request, response);
            	eventManager.postEvent(beforeDeleteEvent);
            }
            
            try {
                partition.delete(this, request, response);

            } catch (LDAPException e) {
                response.setException(e);
                throw e;

            } finally {
                if (eventsEnabled) {
                	DeleteEvent afterDeleteEvent = new DeleteEvent(this, DeleteEvent.AFTER_DELETE, this, partition, request, response);
                	eventManager.postEvent(afterDeleteEvent);
                }
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            response.setException(e);
            throw response.getException();

        } finally {
            Access.log(this, response);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Modify
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void modify(String dn, Collection<Modification> modifications) throws LDAPException {
        modify(new DN(dn), modifications);
    }

    public void modify(DN dn, Collection<Modification> modifications) throws LDAPException {
        ModifyRequest request = new ModifyRequest();
        request.setDn(dn);
        request.setModifications(modifications);

        ModifyResponse response = new ModifyResponse();

        modify(request, response);
    }

    public void modify(ModifyRequest request, ModifyResponse response) throws LDAPException {

        try {
            DN dn = request.getDn();

            PartitionManager partitionManager = penroseContext.getPartitionManager();
            Partition partition = partitionManager.getPartition(dn);
            modify(partition, request, response);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw LDAP.createException(e);
        }
    }

    public void modify(
            Partition partition,
            ModifyRequest request,
            ModifyResponse response
    ) throws LDAPException {
        try {
            Access.log(this, request);

            if (debug) {
                log.debug("----------------------------------------------------------------------------------");
                log.debug("MODIFY:");
                log.debug(" - Bind DN    : "+(bindDn == null ? "" : bindDn));
                log.debug(" - DN         : "+request.getDn());
                log.debug("");

                log.debug("Modifications:");

                Collection<Modification> modifications = request.getModifications();

                for (Modification modification : modifications) {
                    Attribute attribute = modification.getAttribute();

                    String op = LDAP.getModificationOperation(modification.getType());
                    log.debug("   - " + op + ": " + attribute.getName() + " => " + attribute.getValues());
                }

                log.debug("");

                log.debug("Controls: "+request.getControls());
            }

            if (eventsEnabled) {
            	ModifyEvent beforeModifyEvent = new ModifyEvent(this, ModifyEvent.BEFORE_MODIFY, this, partition, request, response);
            	eventManager.postEvent(beforeModifyEvent);
            }

            try {
                partition.modify(this, request, response);

            } catch (LDAPException e) {
                response.setException(e);
                throw e;

            } finally {
                if (eventsEnabled) {
                	ModifyEvent afterModifyEvent = new ModifyEvent(this, ModifyEvent.AFTER_MODIFY, this, partition, request, response);
                	eventManager.postEvent(afterModifyEvent);
                }
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            response.setException(e);
            throw response.getException();

        } finally {
            Access.log(this, response);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // ModRdn
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void modrdn(String dn, String newRdn, boolean deleteOldRdn) throws LDAPException {
        try {
            modrdn(new DN(dn), new RDN(newRdn), deleteOldRdn);
        } catch (Exception e) {
            throw LDAP.createException(e);
        }
    }

    public void modrdn(DN dn, RDN newRdn, boolean deleteOldRdn) throws LDAPException {
        ModRdnRequest request = new ModRdnRequest();
        request.setDn(dn);
        request.setNewRdn(newRdn);
        request.setDeleteOldRdn(deleteOldRdn);

        ModRdnResponse response = new ModRdnResponse();

        modrdn(request, response);
    }

    public void modrdn(ModRdnRequest request, ModRdnResponse response) throws LDAPException {

        try {
            DN dn = request.getDn();

            PartitionManager partitionManager = penroseContext.getPartitionManager();
            Partition partition = partitionManager.getPartition(dn);
            modrdn(partition, request, response);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw LDAP.createException(e);
        }
    }

    public void modrdn(
            Partition partition,
            ModRdnRequest request,
            ModRdnResponse response
    ) throws LDAPException {
        try {
            Access.log(this, request);

            if (debug) {
                log.debug("----------------------------------------------------------------------------------");
                log.debug("MODRDN:");
                log.debug(" - Bind DN        : "+(bindDn == null ? "" : bindDn));
                log.debug(" - DN             : "+request.getDn());
                log.debug(" - New RDN        : "+request.getNewRdn());
                log.debug(" - Delete old RDN : "+request.getDeleteOldRdn());
                log.debug("");

                log.debug("Controls: "+request.getControls());
            }

            if (eventsEnabled) {
            	ModRdnEvent beforeModRdnEvent = new ModRdnEvent(this, ModRdnEvent.BEFORE_MODRDN, this, partition, request, response);
	            eventManager.postEvent(beforeModRdnEvent);
            }

            try {
                partition.modrdn(this, request, response);

            } catch (LDAPException e) {
                response.setException(e);
                throw e;

            } finally {
                if (eventsEnabled) {
                    ModRdnEvent afterModRdnEvent = new ModRdnEvent(this, ModRdnEvent.AFTER_MODRDN, this, partition, request, response);
                    eventManager.postEvent(afterModRdnEvent);
                }
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            response.setException(e);
            throw response.getException();

        } finally {
            Access.log(this, response);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Search
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public SearchResponse search(
            String baseDn,
            String filter
    ) throws LDAPException {
        return search(baseDn, filter, SearchRequest.SCOPE_SUB);
    }

    public SearchResponse search(
            String baseDn,
            String filter,
            int scope
    ) throws LDAPException {
        try {
            return search(new DN(baseDn), FilterTool.parseFilter(filter), scope);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw LDAP.createException(e);
        }
    }

    public SearchResponse search(
            DN baseDn,
            Filter filter,
            int scope
    ) throws LDAPException {

        SearchRequest request = new SearchRequest();
        request.setDn(baseDn);
        request.setFilter(filter);
        request.setScope(scope);

        SearchResponse response = new SearchResponse();

        search(request, response);

        return response;
    }

    public void search(SearchRequest request, SearchResponse response) throws LDAPException {

        try {
            DN dn = request.getDn();

            PartitionManager partitionManager = penroseContext.getPartitionManager();
            Partition partition = partitionManager.getPartition(dn);
            search(partition, request, response);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw LDAP.createException(e);
        }
    }

    public void search(
            final Partition partition,
            final SearchRequest request,
            final SearchResponse response
    ) throws LDAPException {
        try {
            Access.log(this, request);

            if (debug) {
                log.debug("----------------------------------------------------------------------------------");
                log.debug("SEARCH:");
                log.debug(" - Bind DN    : "+(bindDn == null ? "" : bindDn));
                log.debug(" - Base DN    : "+request.getDn());
                log.debug(" - Scope      : "+LDAP.getScope(request.getScope()));
                log.debug(" - Filter     : "+request.getFilter());
                log.debug(" - Attributes : "+request.getAttributes());
                log.debug("");
                
                log.debug("Controls: "+request.getControls());
            }

            final Session session = this;

            response.setEventsEnabled(eventsEnabled);
            response.setBufferSize(bufferSize);

            if (eventsEnabled) {
                SearchEvent beforeSearchEvent = new SearchEvent(session, SearchEvent.BEFORE_SEARCH, session, partition, request, response);
                eventManager.postEvent(beforeSearchEvent);
            }

            SearchResponse sr = new SearchResponse() {
                public void add(SearchResult result) throws Exception {
                    response.add(result);
                }
                public void addReferral(SearchResult reference) throws Exception {
                    response.addReferral(reference);
                }
                public void setException(LDAPException exception) {
                    response.setException(exception);
                }
                public void close() throws Exception {
                    response.close();

                    if (eventsEnabled) {
                        SearchEvent afterSearchEvent = new SearchEvent(session, SearchEvent.AFTER_SEARCH, session, partition, request, response);
                        eventManager.postEvent(afterSearchEvent);
                    }

                    Access.log(session, response);
                }
            };

            partition.search(this, request, sr);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            response.setException(e);
            try { response.close(); } catch (Exception ex) { log.error(ex.getMessage(), ex); }
            Access.log(this, response);
            throw response.getException();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Unbind
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void unbind() throws LDAPException {
        UnbindRequest request = new UnbindRequest();
        request.setDn(bindDn);

        UnbindResponse response = new UnbindResponse();

        unbind(request, response);
    }

    public void unbind(UnbindRequest request, UnbindResponse response) throws LDAPException {

        try {
            if (!rootUser && bindDn != null) {
                PartitionManager partitionManager = penroseContext.getPartitionManager();
                Partition partition = partitionManager.getPartition(bindDn);
                unbind(partition, request, response);

            } else {
                unbind(null, request, response);
            }
            
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw LDAP.createException(e);
        }
    }

    public void unbind(
            Partition partition,
            UnbindRequest request,
            UnbindResponse response
    ) throws LDAPException {
        try {
            Access.log(this, request);

            if (debug) {
                log.debug("----------------------------------------------------------------------------------");
                log.debug("UNBIND:");
                log.debug(" - Bind DN: "+(bindDn == null ? "" : bindDn));
                log.debug("");

                log.debug("Controls: "+request.getControls());
            }

            if (eventsEnabled) {
            	UnbindEvent beforeUnbindEvent = new UnbindEvent(this, UnbindEvent.BEFORE_UNBIND, this, partition, request, response);
	            eventManager.postEvent(beforeUnbindEvent);
            }

            try {
                if (bindDn == null) {
                    return;
                }

                if (rootUser) {
                    rootUser = false;
                    return;
                }

                partition.unbind(this, request, response);
                bindDn = null;

            } catch (LDAPException e) {
                response.setException(e);
                throw e;

            } finally {
                if (eventsEnabled) {
	                UnbindEvent afterUnbindEvent = new UnbindEvent(this, UnbindEvent.AFTER_UNBIND, this, partition, request, response);
	                eventManager.postEvent(afterUnbindEvent);
                }
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            response.setException(e);
            throw response.getException();

        } finally {
            Access.log(this, response);
        }
    }

    public void close() throws Exception {

        log.debug("----------------------------------------------------------------------------------");
        if (warn) log.warn("Closing session "+sessionId+".");

        for (SessionListener listener : listeners) {
            listener.sessionClosed();
        }

        sessionManager.removeSession(this);
    }

    public Object getSessionId() {
        return sessionId;
    }

    public void setSessionId(Object sessionId) {
        this.sessionId = sessionId;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public void setSessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public void setEventManager(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    public void addAddListener(AddListener listener) {
        eventManager.addAddListener(listener);
    }

    public void removeAddListener(AddListener listener) {
        eventManager.removeAddListener(listener);
    }

    public void addBindListener(BindListener listener) {
        eventManager.addBindListener(listener);
    }

    public void removeBindListener(BindListener listener) {
        eventManager.removeBindListener(listener);
    }

    public void addCompareListener(CompareListener listener) {
        eventManager.addCompareListener(listener);
    }

    public void removeCompareListener(CompareListener listener) {
        eventManager.removeCompareListener(listener);
    }

    public void addDeleteListener(DeleteListener listener) {
        eventManager.addDeleteListener(listener);
    }

    public void removeDeleteListener(DeleteListener listener) {
        eventManager.removeDeleteListener(listener);
    }

    public void addModifyListener(ModifyListener listener) {
        eventManager.addModifyListener(listener);
    }

    public void removeModifyListener(ModifyListener listener) {
        eventManager.removeModifyListener(listener);
    }

    public void addModrdnListener(ModRdnListener listener) {
        eventManager.addModRdnListener(listener);
    }

    public void removeModrdnListener(ModRdnListener listener) {
        eventManager.removeModRdnListener(listener);
    }

    public void addSearchListener(SearchListener listener) {
        eventManager.addSearchListener(listener);
    }

    public void removeSearchListener(SearchListener listener) {
        eventManager.removeSearchListener(listener);
    }

    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public Collection getAttributeNames() {
        return attributes.keySet();
    }

    public Object removeAttribute(String name) {
        return attributes.remove(name);
    }
    
    public boolean isRootUser() {
        return rootUser;
    }

    public void setRootUser(boolean rootUser) {
        this.rootUser = rootUser;
    }

    public PenroseContext getPenroseContext() {
        return penroseContext;
    }

    public void setPenroseContext(PenroseContext penroseContext) {
        this.penroseContext = penroseContext;
    }

    public SessionContext getSessionContext() {
        return sessionContext;
    }

    public void setSessionContext(SessionContext sessionContext) {
        this.sessionContext = sessionContext;
        eventManager = sessionContext.getEventManager();
    }

    public PenroseConfig getPenroseConfig() {
        return penroseConfig;
    }

    public void setPenroseConfig(PenroseConfig penroseConfig) {
        this.penroseConfig = penroseConfig;
    }

    public boolean isEventsEnabled() {
        return eventsEnabled;
    }

    public void setEventsEnabled(boolean eventsEnabled) {
        this.eventsEnabled = eventsEnabled;
    }

    public long getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(long bufferSize) {
        this.bufferSize = bufferSize;
    }

    public Collection<SessionListener> getListeners() {
        return listeners;
    }

    public void setListeners(Collection<SessionListener> listeners) {
        if (this.listeners == listeners) return;
        this.listeners.clear();
        if (listeners == null) return;
        this.listeners.addAll(listeners);
    }

    public void addListener(SessionListener listener) {
        listeners.add(listener);
    }

    public void removeListener(SessionListener listener) {
        listeners.remove(listener);
    }
}
