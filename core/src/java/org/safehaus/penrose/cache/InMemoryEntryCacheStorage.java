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
package org.safehaus.penrose.cache;

import org.safehaus.penrose.filter.Filter;
import org.safehaus.penrose.partition.SourceConfig;
import org.safehaus.penrose.ldap.SearchResponse;
import org.safehaus.penrose.entry.Entry;
import org.safehaus.penrose.entry.DN;
import org.safehaus.penrose.entry.AttributeValues;
import org.safehaus.penrose.entry.RDN;

import java.util.*;

/**
 * @author Endi S. Dewata
 */
public class InMemoryEntryCacheStorage extends EntryCacheStorage {

    Map queryMap = new TreeMap();
    Map queryExpirationMap = new LinkedHashMap();

    Map dataMap = new TreeMap();
    Map dataExpirationMap = new LinkedHashMap();

    public InMemoryEntryCacheStorage() throws Exception {
    }

    public Entry get(DN dn) throws Exception {

        log.debug("get("+dn+")");

        Entry entry = (Entry)dataMap.get(dn);
        Date date = (Date)dataExpirationMap.get(dn);

        if (date == null || date.getTime() <= System.currentTimeMillis()) {
            //log.debug("Cache is expired.");
            dataMap.remove(dn);
            dataExpirationMap.remove(dn);
            return null;
        }

        log.debug("get("+dn+") => "+(entry != null));

        return entry;
    }

    public Map getExpired() throws Exception {
        Map results = new TreeMap();
        return results;
    }

    public void put(DN dn, Entry entry) throws Exception {

        log.debug("put("+dn+")");

        if (getSize() == 0) return;

        while (dataMap.get(dn) == null && dataMap.size() >= getSize()) {
            //log.debug("Trimming entry cache ("+dataMap.size()+").");
            Object k = dataExpirationMap.keySet().iterator().next();
            dataMap.remove(k);
            dataExpirationMap.remove(k);
        }

        //log.debug("Storing entry cache ("+dataMap.size()+"): "+dn);
        dataMap.put(dn, entry);
        dataExpirationMap.put(dn, new Date(System.currentTimeMillis() + getExpiration() * 60 * 1000));
    }

    public void remove(DN dn) throws Exception {

        log.debug("remove("+dn+")");

        dataMap.remove(dn);
        dataExpirationMap.remove(dn);

        invalidate();
    }

    public boolean contains(DN baseDn, Filter filter) throws Exception {

        log.debug("contains("+baseDn+", "+filter+")");

        String key = filter == null ? "" : filter.toString();
        boolean result = queryMap.containsKey(key);

        log.debug("contains("+baseDn+", "+filter+") => "+result);

        return result;
    }

    public boolean search(
            DN baseDn,
            Filter filter,
            SearchResponse response
    ) throws Exception {

        log.debug("search("+baseDn+", "+filter+")");

        try {
            String key = filter == null ? "" : filter.toString();

            Collection dns = (Collection)queryMap.get(key);
            if (dns == null) return false;

            Collection entries = new ArrayList();
            for (Iterator i=dns.iterator(); i.hasNext(); ) {
                DN dn = (DN)i.next();
                if (baseDn != null && !dn.endsWith(baseDn)) continue;

                Entry entry = get(dn);
                if (entry == null) return false;

                entries.add(entry);
            }

            for (Iterator i=entries.iterator(); i.hasNext(); ) {
                Entry entry = (Entry)i.next();
                log.debug("search("+baseDn+", "+filter+") => "+entry.getDn());
                response.add(entry);
            }

            Date date = (Date)queryExpirationMap.get(key);

            if (date == null || date.getTime() <= System.currentTimeMillis()) {
                //log.debug("Filter cache has expired.");
                queryMap.remove(key);
                queryExpirationMap.remove(key);
            }

            return true;

        } finally {
            response.close();
        }
    }

    public void search(SourceConfig sourceConfig, RDN filter, SearchResponse response) throws Exception {

        log.debug("search("+sourceConfig.getName()+", "+filter+")");

        for (Iterator i=dataMap.keySet().iterator(); i.hasNext(); ) {
            String dn = (String)i.next();

            Date date = (Date)dataExpirationMap.get(dn);
            if (date == null || date.getTime() <= System.currentTimeMillis()) continue;

            Entry entry = (Entry)dataMap.get(dn);
            AttributeValues sv = null; // entry.getSourceValues();
            if (!sv.contains(filter)) continue;

            log.debug("search("+sourceConfig.getName()+", "+filter+") => "+dn);
            response.add(dn);
        }

        response.close();
    }

    public void add(DN baseDn, Filter filter, DN dn) throws Exception {

        log.debug("add("+filter+", "+dn+")");

        String key = filter == null ? "" : filter.toString();

        Collection dns = (Collection)queryMap.get(key);

        while (dns == null && queryMap.size() >= getSize()) {
            //log.debug("Trimming entry filter cache ("+queryMap.size()+").");
            Object k = queryExpirationMap.keySet().iterator().next();
            queryMap.remove(k);
            queryExpirationMap.remove(k);
        }

        if (dns == null) {
            //log.debug("Creating new storage.");
            dns = new TreeSet();
        }

        dns.add(dn);

        queryMap.put(key, dns);
        queryExpirationMap.put(key, new Date(System.currentTimeMillis() + getExpiration() * 60 * 1000));

        //log.debug("cache content: "+dns);
    }

    public void put(Filter filter, Collection dns) throws Exception {

        log.debug("put("+filter+", "+dns+")");

        if (getSize() == 0) return;

        String key = filter == null ? "" : filter.toString();

        Object object = (Collection)queryMap.get(key);

        while (object == null && queryMap.size() >= getSize()) {
            //log.debug("Trimming entry filter cache ("+queryMap.size()+").");
            Object k = queryExpirationMap.keySet().iterator().next();
            queryMap.remove(k);
            queryExpirationMap.remove(k);
        }

        //log.debug("Storing entry filter cache ("+queryMap.size()+"): "+key);
        queryMap.put(key, dns);
        queryExpirationMap.put(key, new Date(System.currentTimeMillis() + getExpiration() * 60 * 1000));
    }

    public void invalidate() throws Exception {
        queryMap.clear();
        queryExpirationMap.clear();
    }

}
