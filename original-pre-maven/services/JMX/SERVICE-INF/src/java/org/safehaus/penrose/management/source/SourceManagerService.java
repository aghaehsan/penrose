package org.safehaus.penrose.management.source;

import org.safehaus.penrose.management.BaseService;
import org.safehaus.penrose.management.PenroseJMXService;
import org.safehaus.penrose.source.*;
import org.safehaus.penrose.partition.PartitionManager;
import org.safehaus.penrose.partition.PartitionConfig;
import org.safehaus.penrose.partition.Partition;
import org.safehaus.penrose.directory.Directory;
import org.safehaus.penrose.directory.Entry;
import org.safehaus.penrose.util.TextUtil;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * @author Endi Sukma Dewata
 */
public class SourceManagerService extends BaseService implements SourceManagerServiceMBean {

    private PartitionManager partitionManager;
    private String partitionName;

    Map<String,SourceService> sourceServices = new LinkedHashMap<String,SourceService>();

    public SourceManagerService(PenroseJMXService jmxService, PartitionManager partitionManager, String partitionName) throws Exception {

        this.jmxService = jmxService;
        this.partitionManager = partitionManager;
        this.partitionName = partitionName;
    }

    public String getObjectName() {
        return SourceManagerClient.getStringObjectName(partitionName);
    }

    public Object getObject() {
        return getSourceManager();
    }

    public PartitionConfig getPartitionConfig() {
        return partitionManager.getPartitionConfig(partitionName);
    }

    public Partition getPartition() {
        return partitionManager.getPartition(partitionName);
    }

    public SourceConfigManager getSourceConfigManager() {
        PartitionConfig partitionConfig = getPartitionConfig();
        if (partitionConfig == null) return null;
        return partitionConfig.getSourceConfigManager();
    }

    public SourceManager getSourceManager() {
        Partition partition = getPartition();
        if (partition == null) return null;
        return partition.getSourceManager();
    }

    public void createSourceService(String sourceName) throws Exception {

        SourceService sourceService = new SourceService(jmxService, partitionManager, partitionName, sourceName);
        sourceService.init();

        sourceServices.put(sourceName, sourceService);
    }

    public SourceService getSourceService(String sourceName) throws Exception {
        return sourceServices.get(sourceName);
    }

    public void removeSourceService(String sourceName) throws Exception {
        SourceService sourceService = sourceServices.remove(sourceName);
        if (sourceService == null) return;

        sourceService.destroy();
    }

    public void init() throws Exception {

        super.init();

        SourceConfigManager sourceConfigManager = getSourceConfigManager();
        for (String sourceName : sourceConfigManager.getSourceNames()) {
            createSourceService(sourceName);
        }
    }

    public void destroy() throws Exception {
        SourceConfigManager sourceConfigManager = getSourceConfigManager();
        for (String sourceName : sourceConfigManager.getSourceNames()) {
            removeSourceService(sourceName);
        }

        super.destroy();
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Sources
    ////////////////////////////////////////////////////////////////////////////////

    public Collection<String> getSourceNames() throws Exception {

        PartitionConfig partitionConfig = getPartitionConfig();
        SourceConfigManager sourceConfigManager = partitionConfig.getSourceConfigManager();

        Collection<String> list = new ArrayList<String>();
        list.addAll(sourceConfigManager.getSourceNames());

        return list;
    }

    public void createSource(SourceConfig sourceConfig) throws Exception {

        String sourceName = sourceConfig.getName();

        PartitionConfig partitionConfig = getPartitionConfig();
        SourceConfigManager sourceConfigManager = partitionConfig.getSourceConfigManager();
        sourceConfigManager.addSourceConfig(sourceConfig);

        Partition partition = getPartition();
        if (partition != null) {
            SourceManager sourceManager = partition.getSourceManager();
            sourceManager.startSource(sourceName);
        }

        createSourceService(sourceName);
    }

    public void renameSource(String name, String newName) throws Exception {

        boolean debug = log.isDebugEnabled();
        if (debug) {
            log.debug(TextUtil.repeat("-", 70));
            log.debug("Renaming source "+name+" to "+newName+".");
        }

        Partition partition = getPartition();
        boolean running = false;

        if (partition != null) {
            SourceManager sourceManager = partition.getSourceManager();
            running = sourceManager.isRunning(name);
            if (running) sourceManager.stopSource(name);
        }

        removeSourceService(name);

        PartitionConfig partitionConfig = getPartitionConfig();
        SourceConfigManager sourceConfigManager = partitionConfig.getSourceConfigManager();
        sourceConfigManager.renameSourceConfig(name, newName);

        if (partition != null) {
            SourceManager sourceManager = partition.getSourceManager();
            if (running) sourceManager.startSource(newName);
        }

        createSourceService(newName);
    }

    public void updateSource(String sourceName, SourceConfig sourceConfig) throws Exception {

        boolean debug = log.isDebugEnabled();

        if (debug) {
            log.debug(TextUtil.repeat("-", 70));
            log.debug("Updating source "+sourceName+".");
        }

        Partition partition = getPartition();
        if (partition == null) {
            PartitionConfig partitionConfig = getPartitionConfig();
            SourceConfigManager sourceConfigManager = partitionConfig.getSourceConfigManager();
            sourceConfigManager.updateSourceConfig(sourceConfig);

        } else {
            SourceManager sourceManager = partition.getSourceManager();
            sourceManager.updateSource(sourceConfig);
        }
/*
        Partition partition = getPartition();
        boolean running = false;

        if (partition != null) {
            SourceManager sourceManager = partition.getSourceManager();
            running = sourceManager.isRunning(sourceName);
            if (running) sourceManager.stopSource(sourceName);
        }

        PartitionConfig partitionConfig = getPartitionConfig();
        SourceConfigManager sourceConfigManager = partitionConfig.getSourceConfigManager();
        sourceConfigManager.updateSourceConfig(sourceConfig);

        if (partition != null) {
            SourceManager sourceManager = partition.getSourceManager();
            if (running) sourceManager.startSource(sourceName);
        }
*/
    }

    public void removeSource(String name) throws Exception {

        Partition partition = getPartition();

        Directory directory = partition.getDirectory();
        Collection<Entry> entries = directory.getEntriesBySourceName(name);
        if (entries != null && !entries.isEmpty()) {
            throw new Exception("Source "+name+" is in use.");
        }

        SourceManager sourceManager = partition.getSourceManager();
        sourceManager.stopSource(name);

        SourceConfigManager sourceConfigManager = sourceManager.getSourceConfigManager();
        sourceConfigManager.removeSourceConfig(name);

        removeSourceService(name);
    }
}
