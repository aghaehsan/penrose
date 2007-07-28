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
package org.safehaus.penrose.partition;

import org.safehaus.penrose.mapping.EntryMapping;
import org.safehaus.penrose.mapping.SourceMapping;
import org.safehaus.penrose.ldap.DN;
import org.safehaus.penrose.connection.ConnectionConfig;
import org.safehaus.penrose.source.SourceConfig;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.*;
import java.io.File;

/**
 * @author Endi S. Dewata
 */
public class PartitionConfigs implements PartitionConfigsMBean {

    public Logger log = LoggerFactory.getLogger(getClass());
    public Logger errorLog = org.safehaus.penrose.log.Error.log;
    public boolean debug = log.isDebugEnabled();

    private PartitionReader partitionReader = new PartitionReader();

    private Map<String,PartitionConfig> partitionConfigs = new LinkedHashMap<String,PartitionConfig>();

    public PartitionConfigs() {
    }

    public PartitionConfig load(File dir) throws Exception {
        log.debug("Loading partition from "+dir.getAbsolutePath()+".");

        PartitionConfig partitionConfig = partitionReader.read(dir);

        addPartitionConfig(partitionConfig);

        return partitionConfig;
    }

    public void store(String home, Collection<PartitionConfig> partitionConfigs) throws Exception {
        for (PartitionConfig partitionConfig : partitionConfigs) {
            store(home, partitionConfig);
        }
    }

    public void store(String home, PartitionConfig partitionConfig) throws Exception {

        String path = (home == null ? "" : home+File.separator)+"partitions"+File.separator+partitionConfig.getName();

        if (debug) log.debug("Storing "+partitionConfig.getName()+" partition into "+path+".");

        PartitionWriter partitionWriter = new PartitionWriter(path);
        partitionWriter.write(partitionConfig);
    }

    public PartitionConfig removePartitionConfig(String name) throws Exception {
        return partitionConfigs.remove(name);
    }

    public void clear() throws Exception {
        partitionConfigs.clear();
    }

    public PartitionConfig getPartitionConfig(SourceMapping sourceMapping) throws Exception {

        if (sourceMapping == null) return null;

        String sourceName = sourceMapping.getSourceName();
        for (PartitionConfig partitionConfig : partitionConfigs.values()) {
            if (partitionConfig.getSourceConfigs().getSourceConfig(sourceName) != null) return partitionConfig;
        }
        return null;
    }

    public PartitionConfig getPartitionConfig(SourceConfig sourceConfig) throws Exception {

        if (sourceConfig == null) return null;

        String connectionName = sourceConfig.getConnectionName();
        for (PartitionConfig partitionConfig : partitionConfigs.values()) {
            if (partitionConfig.getConnectionConfigs().getConnectionConfig(connectionName) != null) return partitionConfig;
        }
        return null;
    }

    public PartitionConfig getPartitionConfig(ConnectionConfig connectionConfig) throws Exception {

        if (connectionConfig == null) return null;

        String connectionName = connectionConfig.getName();
        for (PartitionConfig partitionConfig : partitionConfigs.values()) {
            if (partitionConfig.getConnectionConfigs().getConnectionConfig(connectionName) != null) return partitionConfig;
        }
        return null;
    }

    public PartitionConfig getPartitionConfig(EntryMapping entryMapping) throws Exception {

        if (entryMapping == null) return null;

        for (PartitionConfig partitionConfig : partitionConfigs.values()) {
            if (partitionConfig.getDirectoryConfigs().contains(entryMapping)) {
                return partitionConfig;
            }
        }

        return null;
    }

    public PartitionConfig getPartitionConfig(DN dn) throws Exception {

        if (debug) log.debug("Finding partition for \""+dn+"\".");

        if (dn == null) {
            log.debug("DN is null.");
            return null;
        }

        PartitionConfig p = null;
        DN s = null;

        for (PartitionConfig partitionConfig : partitionConfigs.values()) {
            if (debug) log.debug("Checking "+partitionConfig.getName()+" partition.");

            Collection<DN> suffixes = partitionConfig.getDirectoryConfigs().getSuffixes();
            for (DN suffix : suffixes) {
                if (suffix.isEmpty() && dn.isEmpty() // Root DSE
                        || dn.endsWith(suffix)) {

                    if (s == null || s.getSize() < suffix.getSize()) {
                        p = partitionConfig;
                        s = suffix;
                    }
                }
            }
        }

        if (debug) {
            if (p == null) {
                log.debug("Partition not found.");
            } else {
                log.debug("Found "+p.getName()+" partition.");
            }
        }

        return p;
    }

    public Collection<String> getPartitionNames() {
        return partitionConfigs.keySet();
    }

    public void addPartitionConfig(PartitionConfig partitionConfig) {
        partitionConfigs.put(partitionConfig.getName(), partitionConfig);
    }

    public Collection<PartitionConfig> getPartitionConfigs() {
        return partitionConfigs.values();
    }

    public PartitionConfig getPartitionConfig(String name) {
        return partitionConfigs.get(name);
    }
}
