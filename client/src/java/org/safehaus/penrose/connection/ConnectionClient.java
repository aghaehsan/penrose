package org.safehaus.penrose.connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.safehaus.penrose.management.BaseClient;
import org.safehaus.penrose.management.PenroseClient;
import org.safehaus.penrose.management.connection.ConnectionServiceMBean;

/**
 * @author Endi Sukma Dewata
 */
public class ConnectionClient extends BaseClient implements ConnectionServiceMBean {

    public static Logger log = LoggerFactory.getLogger(ConnectionClient.class);

    protected String partitionName;

    public ConnectionClient(PenroseClient client, String partitionName, String name) throws Exception {
        super(client, name, getStringObjectName(partitionName, name));

        this.partitionName = partitionName;
    }

    public ConnectionConfig getConnectionConfig() throws Exception {
        return (ConnectionConfig)connection.getAttribute(objectName, "ConnectionConfig");
    }
    
    public static String getStringObjectName(String partitionName, String connectionName) {
        return "Penrose:type=connection,partition="+partitionName+",name="+connectionName;
    }

    public String getPartitionName() {
        return partitionName;
    }

    public void setPartitionName(String partitionName) {
        this.partitionName = partitionName;
    }

    public void start() throws Exception {
        invoke("start", new Object[] {}, new String[] {});
    }

    public void stop() throws Exception {
        invoke("stop", new Object[] {}, new String[] {});
    }

    public void restart() throws Exception {
        invoke("restart", new Object[] {}, new String[] {});
    }

    public String getAdapterName() throws Exception {
        return (String)connection.getAttribute(objectName, "AdapterName");
    }
}
