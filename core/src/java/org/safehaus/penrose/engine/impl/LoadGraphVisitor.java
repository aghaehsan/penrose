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
package org.safehaus.penrose.engine.impl;

import org.safehaus.penrose.mapping.*;
import org.safehaus.penrose.graph.GraphVisitor;
import org.safehaus.penrose.graph.Graph;
import org.safehaus.penrose.graph.GraphIterator;
import org.safehaus.penrose.filter.Filter;
import org.safehaus.penrose.filter.FilterTool;
import org.safehaus.penrose.util.Formatter;
import org.safehaus.penrose.ldap.SearchResponse;
import org.safehaus.penrose.ldap.SearchRequest;
import org.safehaus.penrose.partition.Partition;
import org.safehaus.penrose.partition.SourceConfig;
import org.safehaus.penrose.connector.Connector;
import org.safehaus.penrose.entry.SourceValues;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.ietf.ldap.LDAPException;

import java.util.*;

/**
 * @author Endi S. Dewata
 */
public class LoadGraphVisitor extends GraphVisitor {

    Logger log = LoggerFactory.getLogger(getClass());

    private Partition partition;
    private Graph graph;
    private EngineImpl engine;
    private EntryMapping entryMapping;
    private SourceValues sourceValues;
    private SourceMapping primarySourceMapping;

    private Stack stack = new Stack();

    private SourceValues loadedSourceValues = new SourceValues();
    private int returnCode;

    public LoadGraphVisitor(
            EngineImpl engine,
            Partition partition,
            EntryMapping entryMapping,
            SourceValues sourceValues,
            Collection primaryKeys,
            Filter filter
    ) throws Exception {

        this.engine = engine;
        this.partition = partition;
        this.entryMapping = entryMapping;
        this.sourceValues = sourceValues;

        graph = engine.getGraph(entryMapping);
        primarySourceMapping = engine.getPrimarySource(entryMapping);

        filter = engine.engineFilterTool.convert(partition, sourceValues, entryMapping, primarySourceMapping, filter);

        Map map = new HashMap();
        map.put("primaryKeys", primaryKeys);
        map.put("filter", filter);

        stack.push(map);
        //loadedSourceValues.add(sourceValues);
    }

    public void run() throws Exception {
        graph.traverse(this, primarySourceMapping);
    }

    public void visitNode(GraphIterator graphIterator, Object node) throws Exception {

        SourceMapping sourceMapping = (SourceMapping)node;

        log.debug(Formatter.displaySeparator(60));
        log.debug(Formatter.displayLine("Visiting "+sourceMapping.getName(), 60));
        log.debug(Formatter.displaySeparator(60));
/*
        if (source == primarySource) {
            graphIterator.traverseEdges(node);
            return;
        }
*/
        Map map = (Map)stack.peek();
        Collection primaryKeys = (Collection)map.get("primaryKeys");
        Filter filter = (Filter)map.get("filter");
        Collection relationships = (Collection)map.get("relationships");

        log.debug("Primary Keys: "+primaryKeys);
        log.debug("Filter: "+filter);
        log.debug("Relationships: "+relationships);

        String s = sourceMapping.getParameter(SourceMapping.FILTER);
        if (s != null) {
            Filter sourceFilter = FilterTool.parseFilter(s);
            filter = FilterTool.appendAndFilter(filter, sourceFilter);
        }

        if (sourceValues.contains(sourceMapping.getName())) {
            log.debug("Source "+sourceMapping.getName()+" has been loaded.");
            graphIterator.traverseEdges(node);
            return;
        }

        log.debug("Loading source "+sourceMapping.getName()+" with filter "+filter);

        SourceConfig sourceConfig = partition.getSources().getSourceConfig(sourceMapping.getSourceName());

        SearchRequest request = new SearchRequest();
        SearchResponse response = new SearchResponse();
        
        Connector connector = engine.getConnector(sourceConfig);
/*
        connector.search(
                partition,
                entryMapping,
                sourceMapping,
                sourceConfig,
                primaryKeys,
                filter,
                request,
                response
        );
*/
        Collection list = new ArrayList();
        while (response.hasNext()) {
            SourceValues av = (SourceValues)response.next();

            SourceValues sv = new SourceValues();
            //sv.add(sourceMapping.getName(), av);
            list.add(sv);

            //sourceValues.add(sv);
        }

        //loadedSourceValues.set(sourceMapping.getName(), list);

        int rc = response.getReturnCode();
        if (rc != LDAPException.SUCCESS) {
            returnCode = rc;
        }
        
        graphIterator.traverseEdges(node);
    }

    public void visitEdge(GraphIterator graphIterator, Object node1, Object node2, Object object) throws Exception {

        SourceMapping fromSourceMapping = (SourceMapping)node1;
        SourceMapping toSourceMapping = (SourceMapping)node2;
        Collection relationships = (Collection)object;

        log.debug(Formatter.displaySeparator(60));
        log.debug(Formatter.displayLine(fromSourceMapping.getName()+"-"+toSourceMapping.getName()+" relationship:", 60));
        for (Iterator i=relationships.iterator(); i.hasNext(); ) {
            Relationship relationship = (Relationship)i.next();
            log.debug(Formatter.displayLine(relationship.toString(), 60));
        }
        log.debug(Formatter.displaySeparator(60));

        if (entryMapping.getSourceMapping(toSourceMapping.getName()) == null) {
            log.debug("Source "+toSourceMapping.getName()+" is not defined in entry "+entryMapping.getDn());
            return;
        }

        Filter filter = null;
/*
        Collection list = loadedSourceValues.get(fromSourceMapping.getName());
        if (list != null) {
            log.debug("Generating filters:");
            for (Iterator i=list.iterator(); i.hasNext(); ) {
                SourceValues av = (SourceValues)i.next();

                Filter f = engine.generateFilter(toSourceMapping, relationships, av);
                log.debug(" - "+f);

                filter = FilterTool.appendOrFilter(filter, f);
            }
        }
*/
        Map map = new HashMap();
        map.put("filter", filter);
        map.put("relationships", relationships);

        stack.push(map);

        graphIterator.traverse(node2);

        stack.pop();
    }

    public SourceValues getLoadedSourceValues() {
        return loadedSourceValues;
    }

    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }
}