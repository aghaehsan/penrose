package org.safehaus.penrose.federation;

import org.safehaus.penrose.ldap.DN;
import org.safehaus.penrose.ldap.Attributes;
import org.safehaus.penrose.ldap.SearchResult;
import org.safehaus.penrose.ldap.SearchRequest;
import org.safehaus.penrose.federation.IdentityLinkingResult;

import java.util.Collection;

/**
 * @author Endi Sukma Dewata
 */
public interface IdentityLinkingMBean {

    public Collection<IdentityLinkingResult> search(SearchRequest request) throws Exception;
    public Collection<SearchResult> searchLinks(SearchResult sourceEntry) throws Exception;
    
    public void linkEntry(DN sourceDn, DN targetDn) throws Exception;
    public void unlinkEntry(DN sourceDn, DN targetDn) throws Exception;

    public SearchResult importEntry(SearchResult sourceEntry) throws Exception;
    public SearchResult importEntry(DN sourceDn, SearchResult targetEntry) throws Exception;

    public void addEntry(DN targetDn, Attributes targetAttributes) throws Exception;
    public void deleteEntry(DN sourceDn, DN targetDn) throws Exception;
}
