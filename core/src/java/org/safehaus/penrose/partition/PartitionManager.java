package org.safehaus.penrose.partition;

import org.safehaus.penrose.adapter.AdapterConfig;
import org.safehaus.penrose.config.PenroseConfig;
import org.safehaus.penrose.connection.ConnectionConfig;
import org.safehaus.penrose.directory.Directory;
import org.safehaus.penrose.directory.Entry;
import org.safehaus.penrose.directory.EntrySourceConfig;
import org.safehaus.penrose.ldap.DN;
import org.safehaus.penrose.naming.PenroseContext;
import org.safehaus.penrose.partition.event.PartitionEvent;
import org.safehaus.penrose.partition.event.PartitionListener;
import org.safehaus.penrose.source.SourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * @author Endi S. Dewata
 */
public class PartitionManager {

    public Logger log      = LoggerFactory.getLogger(getClass());
    public Logger errorLog = org.safehaus.penrose.log.Error.log;
    public boolean debug   = log.isDebugEnabled();

    protected File home;
    protected File partitionsDir;
    protected File confDir;

    PenroseConfig penroseConfig;
    PenroseContext penroseContext;

    Map<String,Partition> partitions = new LinkedHashMap<String,Partition>();
    PartitionConfigManager partitionConfigManager = new PartitionConfigManager();

    Collection<PartitionListener> listeners = new LinkedHashSet<PartitionListener>();

    public PartitionManager(File home, PenroseConfig penroseConfig, PenroseContext penroseContext) {
        this.home           = home;
        this.penroseConfig  = penroseConfig;
        this.penroseContext = penroseContext;

        this.partitionsDir  = new File(home, "partitions");
        this.confDir        = new File(home, "conf");
    }

    public PartitionConfigManager getPartitionConfigManager() {
        return partitionConfigManager;
    }

    public void addPartitionConfig(PartitionConfig partitionConfig) {
        partitionConfigManager.addPartitionConfig(partitionConfig);
    }

    public File getPartitionsDir() {
        return partitionsDir;
    }
    
    public void addPartition(Partition partition) {
        partitions.put(partition.getName(), partition);
    }

    public Partition removePartition(String name) {
        return partitions.remove(name);
    }

    public void startPartitions() throws Exception {
        
        loadDefaultPartition();

        startPartition("DEFAULT");

        for (String partitionName : getAvailablePartitionNames()) {
            try {
                loadPartition(partitionName);

            } catch (Exception e) {
                errorLog.error("Failed loading "+partitionName+" partition.", e);
            }
        }

        for (String partitionName : partitionConfigManager.getPartitionNames()) {
            if ("DEFAULT".equals(partitionName)) continue;
            
            try {
                startPartition(partitionName);

            } catch (Exception e) {
                errorLog.error("Failed starting "+partitionName+" partition.", e);
            }
        }
    }

    public void loadDefaultPartition() throws Exception {

        if (debug) log.debug("----------------------------------------------------------------------------------");
        log.debug("Loading DEFAULT partition.");

        DefaultPartitionConfig partitionConfig = new DefaultPartitionConfig();

        for (AdapterConfig adapterConfig : penroseConfig.getAdapterConfigs()) {
            partitionConfig.addAdapterConfig(adapterConfig);
        }

        partitionConfig.load(home);

        partitionConfigManager.addPartitionConfig(partitionConfig);

        if (debug) log.debug("DEFAULT partition loaded.");
    }

    public void loadPartition(String partitionName) throws Exception {

        log.debug("----------------------------------------------------------------------------------");
        log.debug("Loading "+partitionName+" partition.");

        File partitionDir = new File(partitionsDir, partitionName);

        PartitionConfig partitionConfig = new PartitionConfig(partitionName);
        partitionConfig.load(partitionDir);

        partitionConfigManager.addPartitionConfig(partitionConfig);

        if (debug) log.debug(partitionName+" partition loaded.");
    }

    public void startPartition(String name) throws Exception {

        Partition partition = partitions.get(name);
        if (partition != null) {
            log.debug(name+" partition already started.");
            return;
        }

        PartitionConfig partitionConfig = partitionConfigManager.getPartitionConfig(name);
        if (partitionConfig == null) {
            log.error("Can't start "+name+" partition: Partition not found.");
            return;
        }

        if (!partitionConfig.isEnabled()) {
            log.debug(name+" partition disabled.");
            return;
        }

        for (String depend : partitionConfig.getDepends()) {
            if (partitionConfigManager.getPartitionConfig(depend) == null) {
                log.error("Can't start "+name+" partition: Missing dependency ["+depend+"].");
                return;
            }
            startPartition(depend);
        }

        log.debug("----------------------------------------------------------------------------------");
        log.debug("Starting "+name+" partition.");

        PartitionFactory partitionFactory = new PartitionFactory();
        partitionFactory.setPartitionsDir(partitionsDir);
        partitionFactory.setPenroseConfig(penroseConfig);
        partitionFactory.setPenroseContext(penroseContext);

        partition = partitionFactory.createPartition(partitionConfig);

        partitions.put(name, partition);

        PartitionEvent event = new PartitionEvent(PartitionEvent.PARTITION_STARTED, partition);
        for (PartitionListener listener : listeners) {
            listener.partitionStarted(event);
        }

        log.debug(name+" partition started.");
    }

    public void stopPartitions() throws Exception {

        List<String> list = new ArrayList<String>();
        for (String partitionName : partitions.keySet()) {
            list.add(0, partitionName);
        }

        for (String partitionName : list) {
            try {
                stopPartition(partitionName);
                unloadPartition(partitionName);

            } catch (Exception e) {
                errorLog.error(e.getMessage(), e);
            }
        }
    }

    public void stopPartition(String name) throws Exception {

        log.debug("----------------------------------------------------------------------------------");
        log.debug("Stopping "+name+" partition.");

        Partition partition = partitions.get(name);

        if (partition == null) {
            log.debug(name+" partition not started.");
            return;
        }

        partition.destroy();

        partitions.remove(name);

        PartitionEvent event = new PartitionEvent(PartitionEvent.PARTITION_STOPPED, partition);
        for (PartitionListener listener : listeners) {
            listener.partitionStopped(event);
        }

        log.debug(name+" partition stopped.");
    }

    public void unloadPartition(String name) throws Exception {
        partitionConfigManager.removePartitionConfig(name);

        log.debug(name+" partition unloaded.");
    }

    public void clear() throws Exception {
        partitionConfigManager.clear();
        partitions.clear();
    }

    public Partition getPartition(String name) {
        return partitions.get(name);
    }

    public PartitionConfig getPartitionConfig(String name) {
        return partitionConfigManager.getPartitionConfig(name);
    }
    
    public Partition getPartition(EntrySourceConfig sourceMapping) throws Exception {

        if (sourceMapping == null) return null;

        String sourceName = sourceMapping.getSourceName();
        for (Partition partition : partitions.values()) {
            PartitionConfig partitionConfig = partition.getPartitionConfig();
            if (partitionConfig.getSourceConfigManager().getSourceConfig(sourceName) != null) return partition;
        }
        return null;
    }

    public Partition getPartition(SourceConfig sourceConfig) throws Exception {

        if (sourceConfig == null) return null;

        String connectionName = sourceConfig.getConnectionName();
        for (Partition partition : partitions.values()) {
            PartitionConfig partitionConfig = partition.getPartitionConfig();
            if (partitionConfig.getConnectionConfigManager().getConnectionConfig(connectionName) != null) return partition;
        }
        return null;
    }

    public Partition getPartition(ConnectionConfig connectionConfig) throws Exception {

        if (connectionConfig == null) return null;

        String connectionName = connectionConfig.getName();
        for (Partition partition : partitions.values()) {
            PartitionConfig partitionConfig = partition.getPartitionConfig();
            if (partitionConfig.getConnectionConfigManager().getConnectionConfig(connectionName) != null) return partition;
        }
        return null;
    }

    public Partition getPartition(DN dn) throws Exception {

        if (debug) log.debug("Searching partition for \""+dn+"\".");

        Collection<Partition> results = getPartitions(dn);

        if (results.isEmpty()) {
            if (debug) log.debug("Returning DEFAULT partition.");
            return getPartition("DEFAULT");
        }

        Partition partition = results.iterator().next();
        if (debug) log.debug("Returning "+partition.getName()+" partition.");
        
        return partition;
    }

    public Collection<Partition> getPartitions(DN dn) throws Exception {

        Collection<Partition> results = new HashSet<Partition>();

        for (Entry entry : findEntries(dn)) {
            Partition partition = entry.getPartition();
            results.add(partition);
        }

        return results;
    }

    public Entry getEntry(DN dn) throws Exception {
        for (Partition partition : partitions.values()) {
            Directory directory = partition.getDirectory();
            directory.getEntry(dn);
        }
        return null;
    }

    public Collection<Entry> findEntries(DN dn) throws Exception {

        Collection<Entry> results = new ArrayList<Entry>();

        for (Partition partition : partitions.values()) {

            //if (debug) log.debug("Searching for \""+dn+"\" in "+partition.getName()+".");

            Directory directory = partition.getDirectory();
            results.addAll(directory.findEntries(dn));
        }

        return results;
    }
/*
    public Collection<Partition> getPartitions(DN dn) throws Exception {

        Collection<Partition> results = new ArrayList<Partition>();

        if (debug) log.debug("Finding partitions for \""+dn+"\".");

        if (dn == null) {
            log.debug("DN is null.");
            //results.add(getPartition("DEFAULT"));
            return results;
        }

        Partition p = getPartition("DEFAULT");

        if (dn.isEmpty()) {
            log.debug("Root DSE.");
            results.add(p);
            return results;
        }

        DN s = null;

        for (Partition partition : partitions.values()) {
            if (debug) log.debug("Checking "+partition.getName()+" partition.");

            PartitionConfig partitionConfig = partition.getPartitionConfig();
            Collection<DN> suffixes = partitionConfig.getDirectoryConfig().getSuffixes();
            for (DN suffix : suffixes) {
                if (suffix.isEmpty() && dn.isEmpty() // Root DSE
                        || dn.endsWith(suffix)) {

                    if (s == null || s.getSize() < suffix.getSize()) {
                        p = partition;
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

        return results;
    }
*/
    public Collection<Partition> getPartitions() {
        return partitions.values();
    }

    public Collection<String> getAvailablePartitionNames() throws Exception {
        Collection<String> list = new ArrayList<String>();
        for (File partitionDir : partitionsDir.listFiles()) {
            list.add(partitionDir.getName());
        }
        return list;
    }

    public Collection<String> getPartitionNames() {
        return partitions.keySet();
    }

    public int size() {
        return partitions.size();
    }

    public File getHome() {
        return home;
    }

    public void setHome(File home) {
        this.home = home;
    }

    public void setPartitionsDir(File partitionsDir) {
        this.partitionsDir = partitionsDir;
    }

    public File getConfDir() {
        return confDir;
    }

    public void setConfDir(File confDir) {
        this.confDir = confDir;
    }

    public void addListener(PartitionListener listener) {
        listeners.add(listener);
    }

    public void removeListener(PartitionListener listener) {
        listeners.remove(listener);
    }
}
