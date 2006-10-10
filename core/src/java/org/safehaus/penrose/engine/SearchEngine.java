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
package org.safehaus.penrose.engine;

import org.safehaus.penrose.mapping.*;
import org.safehaus.penrose.filter.Filter;
import org.safehaus.penrose.filter.FilterTool;
import org.safehaus.penrose.interpreter.Interpreter;
import org.safehaus.penrose.util.Formatter;
import org.safehaus.penrose.util.EntryUtil;
import org.safehaus.penrose.session.PenroseSearchResults;
import org.safehaus.penrose.session.PenroseSearchControls;
import org.safehaus.penrose.partition.Partition;
import org.safehaus.penrose.partition.SourceConfig;
import org.safehaus.penrose.pipeline.PipelineAdapter;
import org.safehaus.penrose.pipeline.PipelineEvent;
import org.safehaus.penrose.connector.Connector;
import org.ietf.ldap.LDAPException;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.*;

/**
 * @author Endi S. Dewata
 */
public class SearchEngine {

    Logger log = LoggerFactory.getLogger(getClass());

    private Engine engine;

    public SearchEngine(Engine engine) {
        this.engine = engine;
    }

    public void search(
            Partition partition,
            final AttributeValues parentSourceValues,
            final EntryMapping entryMapping,
            final Filter filter,
            final PenroseSearchResults results
            ) throws Exception {

        log.info("Searching "+entryMapping.getDn()+" for "+filter+".");

        boolean staticEntry = engine.isStatic(partition, entryMapping);
        if (staticEntry) {
            log.debug("Returning static entries.");
            searchStatic(partition, parentSourceValues, entryMapping, filter, results);
            return;
        }

        boolean unique = engine.isUnique(partition, entryMapping);
        log.debug("Entry "+entryMapping.getDn()+" "+(unique ? "is" : "is not")+" unique.");

        Collection sources = entryMapping.getSourceMappings();
        Collection sourceNames = new ArrayList();
        for (Iterator i=sources.iterator(); i.hasNext(); ) {
            SourceMapping sm = (SourceMapping)i.next();
            sourceNames.add(sm.getName());
        }
        log.debug("Sources: "+sourceNames);

        Collection effectiveSources = partition.getEffectiveSourceMappings(entryMapping);
        Collection effectiveSourceNames = new ArrayList();
        for (Iterator i=effectiveSources.iterator(); i.hasNext(); ) {
            SourceMapping sm = (SourceMapping)i.next();
            effectiveSourceNames.add(sm.getName());
        }
        log.debug("Effective Sources: "+effectiveSourceNames);

        if (unique && effectiveSources.size() == 1) {
            try {
                simpleSearch(partition, parentSourceValues, entryMapping, filter, results);

            } catch (Throwable e) {
                log.error(e.getMessage(), e);
                results.setReturnCode(LDAPException.OPERATIONS_ERROR);
            }
            return;
        }

        try {
            searchDynamic(partition, parentSourceValues, entryMapping, filter, results);

        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            results.setReturnCode(LDAPException.OPERATIONS_ERROR);
        }
    }

    public void searchStatic(
            final Partition partition,
            final AttributeValues parentSourceValues,
            final EntryMapping entryMapping,
            final Filter filter,
            final PenroseSearchResults results
            ) throws Exception {

        Interpreter interpreter = engine.getInterpreterManager().newInstance();

        Collection list = engine.computeDns(partition, interpreter, entryMapping, parentSourceValues);
        for (Iterator j=list.iterator(); j.hasNext(); ) {
            String dn = (String)j.next();
            log.debug("Static entry "+dn);

            EntryData map = new EntryData();
            map.setDn(entryMapping.getDn());
            map.setMergedValues(parentSourceValues);
            results.add(map);
        }
        results.close();
    }

    public void searchDynamic(
            Partition partition,
            final AttributeValues parentSourceValues,
            final EntryMapping entryMapping,
            final Filter filter,
            final PenroseSearchResults results)
            throws Exception {

        //boolean unique = engine.isUnique(entryMapping  //log.debug("Entry "+entryMapping" "+(unique ? "is" : "is not")+" unique.");

        EntryMapping parentMapping = partition.getParent(entryMapping);

        Interpreter interpreter = engine.getInterpreterManager().newInstance();

        PenroseSearchResults values = new PenroseSearchResults();
        searchSources(partition, parentSourceValues, entryMapping, filter, values);
        values.close();

        Map sourceValues = new TreeMap();
        Map rows = new TreeMap();

        Collection dns = new TreeSet();
        Map childDns = new HashMap();

        //log.debug("Search results for "+entryMapping.getDn()+":");
        for (Iterator i=values.iterator(); i.hasNext(); ) {
            AttributeValues sv = (AttributeValues)i.next();
            //log.debug("==> "+sv);

            Collection list = engine.computeDns(partition, interpreter, entryMapping, sv);
            for (Iterator j=list.iterator(); j.hasNext(); ) {
                String dn = (String)j.next();
                //log.debug("     - "+dn);

                dns.add(dn);

                Row rdn = EntryUtil.getRdn(dn);
                String parentDn = EntryUtil.getParentDn(dn);

                AttributeValues av = (AttributeValues)sourceValues.get(dn);
                if (av == null) {
                    av = new AttributeValues();
                    sourceValues.put(dn, av);
                }
                av.add(sv);

                Collection r = (Collection)rows.get(dn);
                if (r == null) {
                    r = new ArrayList();
                    rows.put(dn, r);
                }
                r.add(sv);

                Collection c = (Collection)childDns.get(parentDn);
                if (c == null) {
                    c = new TreeSet();
                    childDns.put(parentDn, c);
                }
                c.add(dn);
            }
        }

        if (parentMapping != null) {
            log.debug("Storing "+filter+" in entry filter cache:");
            for (Iterator i=childDns.keySet().iterator(); i.hasNext(); ) {
                String parentDn = (String)i.next();
                Collection c = (Collection)childDns.get(parentDn);

                log.debug(" - "+parentDn+":");
                for (Iterator j=c.iterator(); j.hasNext(); ) {
                    String dn = (String)j.next();
                    log.debug("   - DN: "+dn);
                }

            }
        }

        log.debug("Results:");
        for (Iterator i=sourceValues.keySet().iterator(); i.hasNext(); ) {
            String dn = (String)i.next();
            AttributeValues sv = (AttributeValues)sourceValues.get(dn);
            Collection r = (Collection)rows.get(dn);

            log.debug(" - "+dn);
            //log.debug("   sources: "+sv);
            //log.debug("   rows:");

            for (Iterator j=r.iterator(); j.hasNext(); ) {
                AttributeValues row = (AttributeValues)j.next();
                //log.debug("    - "+row);
            }

            EntryData map = new EntryData();
            map.setDn(dn);
            map.setMergedValues(sv);
            map.setRows(r);
            results.add(map);
        }

        int rc = values.getReturnCode();

        if (rc != LDAPException.SUCCESS) {
            log.debug("RC: "+rc);
            results.setReturnCode(rc);
        }
    }

    public void simpleSearch(
            final Partition partition,
            final AttributeValues parentSourceValues,
            final EntryMapping entryMapping,
            final Filter filter,
            final PenroseSearchResults results) throws Exception {

        SearchPlanner planner = new SearchPlanner(
                engine,
                partition,
                entryMapping,
                filter,
                parentSourceValues);

        planner.run();

        final SourceMapping sourceMapping = engine.getPrimarySource(entryMapping);

        if (log.isDebugEnabled()) {
            log.debug(Formatter.displaySeparator(80));
            log.debug(Formatter.displayLine("SIMPLE SEARCH", 80));
            log.debug(Formatter.displaySeparator(80));
        }

        Map filters = planner.getFilters();
        Filter newFilter = (Filter)filters.get(sourceMapping);

        String s = sourceMapping.getParameter(SourceMapping.FILTER);
        if (s != null) {
            Filter sourceFilter = FilterTool.parseFilter(s);
            newFilter = FilterTool.appendAndFilter(newFilter, sourceFilter);
        }

        SourceConfig sourceConfig = partition.getSourceConfig(sourceMapping.getSourceName());

        final Interpreter interpreter = engine.getInterpreterManager().newInstance();

        PenroseSearchControls sc = new PenroseSearchControls();
        final PenroseSearchResults sr = new PenroseSearchResults();

        sr.addListener(new PipelineAdapter() {
            public void objectAdded(PipelineEvent event) {
                AttributeValues av = (AttributeValues)event.getObject();

                try {
                    AttributeValues sv = new AttributeValues();
                    sv.add(parentSourceValues);
                    sv.add(sourceMapping.getName(), av);

                    Collection list = engine.computeDns(partition, interpreter, entryMapping, sv);
                    for (Iterator j=list.iterator(); j.hasNext(); ) {
                        String dn = (String)j.next();
                        log.debug("Generated DN: "+dn);

                        EntryData data = new EntryData();
                        data.setDn(dn);
                        data.setMergedValues(sv);
                        data.setComplete(true);
                        results.add(data);
                    }
                    
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }

            public void pipelineClosed(PipelineEvent event) {
                results.setReturnCode(sr.getReturnCode());
                results.close();
            }
        });

        Connector connector = engine.getConnector(sourceConfig);
        connector.search(partition, sourceConfig, null, newFilter, sc, sr);
    }

    public void searchSources(
            Partition partition,
            final AttributeValues sourceValues,
            final EntryMapping entryMapping,
            final Filter filter,
            final PenroseSearchResults results)
            throws Exception {

        try {
            searchSourcesInBackground(partition, sourceValues, entryMapping, filter, results);

        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            results.setReturnCode(LDAPException.OPERATIONS_ERROR);
        }
    }

    public void searchSourcesInBackground(
            Partition partition,
            AttributeValues sourceValues,
            EntryMapping entryMapping,
            Filter filter,
            PenroseSearchResults results)
            throws Exception {

        SearchPlanner planner = new SearchPlanner(
                engine,
                partition,
                entryMapping,
                filter,
                sourceValues);

        planner.run();

        Collection connectingSources = planner.getConnectingSources();

        SourceMapping primarySourceMapping = engine.getPrimarySource(entryMapping);

        if (primarySourceMapping == null || entryMapping.getSourceMapping(primarySourceMapping.getName()) == null) {
            log.debug("Primary source is not local");
            PenroseSearchResults localResults = new PenroseSearchResults();
            localResults.add(sourceValues);
            localResults.close();

            Collection parentResults = searchParent(
                    partition,
                    entryMapping,
                    planner,
                    localResults,
                    connectingSources
            );

            results.addAll(parentResults);
            return;
        }

        PenroseSearchResults localResults = searchLocal(
                partition,
                entryMapping,
                sourceValues,
                planner,
                connectingSources
        );

        int rc = localResults.getReturnCode();

        //log.debug("Size: "+localResults.size());
/*
        if (localResults.isEmpty()) {
            log.debug("Result is empty");
            return;
        }
*/
        Collection parentResults = searchParent(
                partition,
                entryMapping,
                planner,
                localResults,
                connectingSources
        );

        results.addAll(parentResults);
        results.setReturnCode(rc);

        if (rc != LDAPException.SUCCESS) {
            log.debug("RC: "+rc);
        }

        return;
    }

    public PenroseSearchResults searchLocal(
            Partition partition,
            EntryMapping entryMapping,
            AttributeValues sourceValues,
            SearchPlanner planner,
            Collection connectingSources) throws Exception {

        SourceMapping primarySourceMapping = engine.getPrimarySource(entryMapping);

        Map filters = planner.getFilters();

        Map map = null;
        for (Iterator i=connectingSources.iterator(); i.hasNext(); ) {
            Map m = (Map)i.next();

            SourceMapping fromSourceMapping = (SourceMapping)m.get("fromSource");
            SourceMapping toSourceMapping = (SourceMapping)m.get("toSource");
            Collection relationships = (Collection)m.get("relationships");

            Filter toFilter = (Filter)filters.get(toSourceMapping);
            Filter tf = engine.generateFilter(toSourceMapping, relationships, sourceValues);
            toFilter = FilterTool.appendAndFilter(toFilter, tf);
            filters.put(toSourceMapping, toFilter);

            log.debug("Filter for "+toSourceMapping.getName()+": "+toFilter);

            if (toFilter == null && map == null) continue;

            map = m;
        }

        if (map == null) {
            // if there's no parent source that can be used as filters
            // start from any local source that has a filter

            for (Iterator i=entryMapping.getSourceMappings().iterator(); i.hasNext(); ) {
                SourceMapping sourceMapping = (SourceMapping)i.next();
                Filter f = (Filter)filters.get(sourceMapping);
                if (f == null) continue;

                map = new HashMap();
                map.put("toSource", sourceMapping);
                map.put("relationships", new ArrayList());

                break;
            }
        }

        // start from the primary source
        if (map == null) {
            map = new HashMap();
            map.put("toSource", primarySourceMapping);
            map.put("relationships", new ArrayList());
        }

        SourceMapping startingSourceMapping = (SourceMapping)map.get("toSource");
        Collection relationships = (Collection)map.get("relationships");

        if (log.isDebugEnabled()) {
            log.debug(Formatter.displaySeparator(80));
            log.debug(Formatter.displayLine("SEARCH LOCAL", 80));
            log.debug(Formatter.displayLine("Parent source values:", 80));

            for (Iterator j=sourceValues.getNames().iterator(); j.hasNext(); ) {
                String name = (String)j.next();
                Collection v = sourceValues.get(name);
                log.debug(Formatter.displayLine(" - "+name+": "+v, 80));
            }

            log.debug(Formatter.displayLine("Starting source: "+startingSourceMapping.getName(), 80));
            log.debug(Formatter.displayLine("Relationships:", 80));

            for (Iterator j=relationships.iterator(); j.hasNext(); ) {
                Relationship relationship = (Relationship)j.next();
                log.debug(Formatter.displayLine(" - "+relationship, 80));
            }

            log.debug(Formatter.displaySeparator(80));
        }

        SearchLocalRunner runner = new SearchLocalRunner(
                engine,
                partition,
                entryMapping,
                sourceValues,
                planner,
                startingSourceMapping,
                relationships);

        runner.run();

        Collection list = runner.getResults();
        log.debug("Got "+list.size()+" entries");

        int rc = runner.getReturnCode();
/*
        if (rc != LDAPException.SUCCESS) {
            log.debug("RC: "+rc);
            PenroseSearchResults results = new PenroseSearchResults();
            results.setReturnCode(rc);
            results.close();
            return results;
        }
*/
        SearchCleaner cleaner = new SearchCleaner(
                engine,
                partition,
                entryMapping,
                planner,
                primarySourceMapping);

        for (Iterator i=connectingSources.iterator(); i.hasNext(); ) {
            Map m = (Map)i.next();
            SourceMapping sourceMapping = (SourceMapping)m.get("toSource");
            cleaner.run(sourceMapping);
        }

        cleaner.clean(list);
/*
        log.debug("Search local results:");

        int counter = 1;
        for (Iterator i=list.iterator(); i.hasNext(); counter++) {
            AttributeValues av = (AttributeValues)i.next();
            log.debug("Result #"+counter);
            for (Iterator j=av.getNames().iterator(); j.hasNext(); ) {
                String name = (String)j.next();
                Collection v = av.get(name);
                log.debug(" - "+name+": "+v);
            }
        }
*/
        PenroseSearchResults results = new PenroseSearchResults();
        results.addAll(list);
        results.setReturnCode(rc);
        results.close();

        return results;
    }

    public Collection searchParent(
            Partition partition,
            EntryMapping entryMapping,
            SearchPlanner planner,
            PenroseSearchResults localResults,
            Collection startingSources) throws Exception {

        Collection results = new ArrayList();
        results.addAll(localResults.getAll());

        AttributeValues sourceValues = new AttributeValues();
        for (Iterator i=results.iterator(); i.hasNext(); ) {
            AttributeValues sv = (AttributeValues)i.next();
            sourceValues.add(sv);
        }

        if (log.isDebugEnabled()) {
            log.debug(Formatter.displaySeparator(80));
            log.debug(Formatter.displayLine("SEARCH PARENT", 80));

            log.debug(Formatter.displayLine("Local source values:", 80));

            int counter = 1;
            for (Iterator i=results.iterator(); i.hasNext() && counter<=20; counter++) {
                AttributeValues sv = (AttributeValues)i.next();

                log.debug(Formatter.displayLine("Record #"+counter, 80));
                for (Iterator j=sv.getNames().iterator(); j.hasNext(); ) {
                    String name = (String)j.next();
                    Collection values = sv.get(name);
                    log.debug(Formatter.displayLine(" - "+name+": "+values, 80));
                }
            }

            log.debug(Formatter.displaySeparator(80));
        }

        Map filters = planner.getFilters();
        
        for (Iterator i=startingSources.iterator(); i.hasNext(); ) {
            Map m = (Map)i.next();

            SourceMapping fromSourceMapping = (SourceMapping)m.get("fromSource");
            SourceMapping toSourceMapping = (SourceMapping)m.get("toSource");
            Collection relationships = (Collection)m.get("relationships");

            Filter fromFilter = (Filter)filters.get(fromSourceMapping);
            Filter tf = engine.generateFilter(fromSourceMapping, relationships, sourceValues);
            fromFilter = FilterTool.appendAndFilter(fromFilter, tf);
            filters.put(fromSourceMapping, fromFilter);

            log.debug("Filter for "+fromSourceMapping.getName()+": "+fromFilter);
        }

        if (startingSources.isEmpty()) {
            log.debug("No connecting sources");

            EntryMapping parentMapping = partition.getParent(entryMapping);

            while (parentMapping != null) {
                log.debug("Checking: "+parentMapping.getDn());

                SourceMapping sourceMapping = engine.getPrimarySource(parentMapping);
                log.debug("Primary source: "+sourceMapping);

                if (sourceMapping != null) {
                    Map map = new HashMap();
                    map.put("fromSource", sourceMapping);
                    map.put("relationships", new ArrayList());

                    startingSources.add(map);
                    break;
                }

                parentMapping = partition.getParent(parentMapping);
            }

            if (parentMapping == null && results.size() == 0) {
                results.add(new AttributeValues());
            }
        }

        for (Iterator i=startingSources.iterator(); i.hasNext(); ) {
            Map m = (Map)i.next();

            SourceMapping fromSourceMapping = (SourceMapping)m.get("fromSource");
            SourceMapping toSourceMapping = (SourceMapping)m.get("toSource");
            Collection relationships = (Collection)m.get("relationships");

            log.debug("Starting source: "+fromSourceMapping.getName());
            log.debug("Relationships:");

            for (Iterator j=relationships.iterator(); j.hasNext(); ) {
                Relationship relationship = (Relationship)j.next();
                log.debug(" - "+relationship);
            }

            Filter filter = (Filter)filters.get(fromSourceMapping);
            log.debug("Filter: "+filter);

            SearchParentRunner runner = new SearchParentRunner(
                    engine,
                    partition,
                    entryMapping,
                    results,
                    sourceValues,
                    planner,
                    fromSourceMapping,
                    relationships);

            runner.run();
        }

        if (log.isDebugEnabled()) {
            log.debug(Formatter.displaySeparator(80));
            log.debug(Formatter.displayLine("SEARCH PARENT RESULTS", 80));

            int counter = 1;
            for (Iterator j=results.iterator(); j.hasNext() && counter<=20; counter++) {
                AttributeValues av = (AttributeValues)j.next();
                log.debug(Formatter.displayLine("Result #"+counter, 80));
                for (Iterator k=av.getNames().iterator(); k.hasNext(); ) {
                    String name = (String)k.next();
                    Collection values = av.get(name);
                    log.debug(Formatter.displayLine(" - "+name+": "+values, 80));
                }
            }

            log.debug(Formatter.displaySeparator(80));
        }

        return results;
    }

    public Engine getEngine() {
        return engine;
    }

    public void setEngine(Engine engine) {
        this.engine = engine;
    }
}