package org.safehaus.penrose.naming;

import org.safehaus.penrose.config.PenroseConfig;
import org.safehaus.penrose.filter.FilterEvaluator;
import org.safehaus.penrose.logger.LoggerManager;
import org.safehaus.penrose.partition.PartitionManager;
import org.safehaus.penrose.partition.PartitionValidator;
import org.safehaus.penrose.schema.SchemaManager;
import org.safehaus.penrose.session.SessionContext;
import org.safehaus.penrose.thread.ThreadManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * @author Endi S. Dewata
 */
public class PenroseContext {

    public Logger log = LoggerFactory.getLogger(getClass());
    public Logger errorLog = org.safehaus.penrose.log.Error.log;
    public boolean debug = log.isDebugEnabled();

    public final static String THREAD_MANAGER      = "java:comp/org/safehaus/penrose/thread/ThreadManager";
    public final static String SCHEMA_MANAGER      = "java:comp/org/safehaus/penrose/schema/SchemaManager";

    public final static String SESSION_MANAGER     = "java:comp/org/safehaus/penrose/session/SessionManager";
    public final static String HANDLER_MANAGER     = "java:comp/org/safehaus/penrose/handler/HandlerManager";
    public final static String EVENT_MANAGER       = "java:comp/org/safehaus/penrose/event/EventManager";

    public final static String INTERPRETER_MANAGER = "java:comp/org/safehaus/penrose/interpreter/InterpreterManager";
    public final static String CONNECTOR_MANAGER   = "java:comp/org/safehaus/penrose/connector/ConnectorManager";

    public final static String PARTITION_MANAGER   = "java:comp/org/safehaus/penrose/partition/PartitionManager";

    protected File               home;
    protected PenroseConfig      penroseConfig;

    protected ThreadManager      threadManager;
    protected SchemaManager      schemaManager;
    protected FilterEvaluator    filterEvaluator;

    protected PartitionManager   partitionManager;
    protected LoggerManager      loggerManager = new LoggerManager();

    protected SessionContext     sessionContext;

    public PenroseContext(File home) {
        this.home = home;
    }

    public ThreadManager getThreadManager() {
        return threadManager;
    }

    public void setThreadManager(ThreadManager threadManager) {
        this.threadManager = threadManager;
    }

    public SchemaManager getSchemaManager() {
        return schemaManager;
    }

    public void setSchemaManager(SchemaManager schemaManager) {
        this.schemaManager = schemaManager;
    }

    public PartitionManager getPartitionManager() {
        return partitionManager;
    }

    public void setPartitions(PartitionManager partitionManager) {
        this.partitionManager = partitionManager;
    }

    public SessionContext getSessionContext() {
        return sessionContext;
    }

    public void setSessionContext(SessionContext sessionContext) {
        this.sessionContext = sessionContext;
    }

    public void init(PenroseConfig penroseConfig) throws Exception {
        this.penroseConfig = penroseConfig;

        for (String name : penroseConfig.getSystemPropertyNames()) {
            String value = penroseConfig.getSystemProperty(name);

            System.setProperty(name, value);
        }

        partitionManager = new PartitionManager(
                home,
                penroseConfig,
                this
        );

        threadManager = new ThreadManager();
        threadManager.setPenroseConfig(penroseConfig);
        threadManager.setPenroseContext(this);

        schemaManager = new SchemaManager(home);
        schemaManager.setPenroseConfig(penroseConfig);
        schemaManager.setPenroseContext(this);
        schemaManager.loadSchemas();

        filterEvaluator = new FilterEvaluator();
        filterEvaluator.setSchemaManager(schemaManager);

        PartitionValidator partitionValidator = new PartitionValidator();
        partitionValidator.setPenroseConfig(penroseConfig);
        partitionValidator.setPenroseContext(this);
    }

    public void start() throws Exception {
        threadManager.start();
    }

    public void stop() throws Exception {
        threadManager.stop();
    }

    public boolean isRunning() {
        return threadManager.isRunning();
    }
    
    public void clear() throws Exception {
        schemaManager.clear();
    }

    public PenroseConfig getPenroseConfig() {
        return penroseConfig;
    }

    public void setPenroseConfig(PenroseConfig penroseConfig) {
        this.penroseConfig = penroseConfig;
    }

    public FilterEvaluator getFilterEvaluator() {
        return filterEvaluator;
    }

    public void setFilterEvaluator(FilterEvaluator filterEvaluator) {
        this.filterEvaluator = filterEvaluator;
    }

    public File getHome() {
        return home;
    }

    public void setHome(File home) {
        this.home = home;
    }
}
