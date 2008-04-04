package org.safehaus.penrose.partition;

import org.safehaus.penrose.config.PenroseConfig;
import org.safehaus.penrose.connection.ConnectionConfig;
import org.safehaus.penrose.directory.Directory;
import org.safehaus.penrose.directory.Entry;
import org.safehaus.penrose.directory.SourceMapping;
import org.safehaus.penrose.ldap.DN;
import org.safehaus.penrose.naming.PenroseContext;
import org.safehaus.penrose.source.SourceConfig;
import org.safehaus.penrose.adapter.AdapterConfig;
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

        for (String partitionName : getAvailablePartitionNames()) {
            try {
                loadPartition(partitionName);

            } catch (Exception e) {
                errorLog.error(e.getMessage(), e);
            }
        }

        for (String partitionName : partitionConfigManager.getLoadOrder()) {
            try {
                startPartition(partitionName);

            } catch (Exception e) {
                errorLog.error(e.getMessage(), e);
            }
        }
    }

    public void loadDefaultPartition() throws Exception {

        if (debug) log.debug("----------------------------------------------------------------------------------");
        log.debug("Loading partition DEFAULT.");

        DefaultPartitionConfig partitionConfig = new DefaultPartitionConfig();

        for (AdapterConfig adapterConfig : penroseConfig.getAdapterConfigs()) {
            partitionConfig.addAdapterConfig(adapterConfig);
        }

        partitionConfig.load(home);

        partitionConfigManager.addPartitionConfig(partitionConfig);

        if (debug) log.debug("Partition DEFAULT loaded.");
    }

    public void loadPartition(String partitionName) throws Exception {

        log.debug("----------------------------------------------------------------------------------");
        log.debug("Loading partition "+partitionName+".");

        File partitionDir = new File(partitionsDir, partitionName);

        PartitionConfig partitionConfig = new PartitionConfig(partitionName);
        partitionConfig.load(partitionDir);

        partitionConfigManager.addPartitionConfig(partitionConfig);

        if (debug) log.debug("Partition "+partitionName+" loaded.");
    }

    public void startPartition(String name) throws Exception {

        log.debug("----------------------------------------------------------------------------------");
        log.debug("Starting partition "+name+".");

        PartitionConfig partitionConfig = partitionConfigManager.getPartitionConfig(name);

        if (!partitionConfig.isEnabled()) {
            if (debug) log.debug("Partition "+name+" disabled.");
            return;
        }

/*
        Collection<PartitionValidationResult> results = partitionValidator.validate(partitionConfig);

        for (PartitionValidationResult result : results) {
            if (result.getType().equals(PartitionValidationResult.ERROR)) {
                errorLog.error("ERROR: " + result.getMessage() + " [" + result.getSource() + "]");
            } else {
                errorLog.warn("WARNING: " + result.getMessage() + " [" + result.getSource() + "]");
            }
        }
*/

        PartitionFactory partitionFactory = new PartitionFactory();
        partitionFactory.setPartitionsDir(partitionsDir);
        partitionFactory.setPenroseConfig(penroseConfig);
        partitionFactory.setPenroseContext(penroseContext);

        Partition partition = partitionFactory.createPartition(partitionConfig);

        partitions.put(name, partition);

        log.debug("Partition "+name+" started.");
    }

    public void stopPartitions() throws Exception {

        List<String> stopOrder = new ArrayList<String>();
        for (String partitionName : partitionConfigManager.getLoadOrder()) {
            stopOrder.add(0, partitionName);
        }

        for (String partitionName : stopOrder) {
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
            log.debug("Partition "+name+" not started.");
            return;
        }

        partition.destroy();

        partitions.remove(name);

        log.debug("Partition "+name+" stopped.");
    }

    public void unloadPartition(String name) throws Exception {
        partitionConfigManager.removePartitionConfig(name);

        log.debug("Partition "+name+" unloaded.");
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
    
    public Partition getPartition(SourceMapping sourceMapping) throws Exception {

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

    public Collection<Entry> findEntries(DN dn) throws Exception {

        Collection<Entry> results = new ArrayList<Entry>();

        for (Partition partition : partitions.values()) {

            Directory directory = partition.getDirectory();

            for (Entry entry : directory.getRootEntries()) {
                Collection<Entry> list = entry.findEntries(dn);
                results.addAll(list);
            }
        }

        return results;
    }

    public Partition getPartition(DN dn) throws Exception {

        Collection<Partition> results = getPartitions(dn);
        if (results.isEmpty()) return getPartition("DEFAULT");

        return results.iterator().next();
    }

    public Collection<Partition> getPartitions(DN dn) throws Exception {

        Collection<Partition> results = new HashSet<Partition>();

        for (Entry entry : findEntries(dn)) {
            Partition partition = entry.getPartition();
            results.add(partition);
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
}
