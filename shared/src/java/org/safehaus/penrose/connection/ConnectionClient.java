package org.safehaus.penrose.connection;

import org.safehaus.penrose.connection.ConnectionMBean;
import org.safehaus.penrose.connection.ConnectionConfig;
import org.safehaus.penrose.connection.ConnectionCounter;
import org.safehaus.penrose.client.PenroseClient;

import javax.management.ObjectName;
import javax.management.MBeanServerConnection;
import java.util.Collection;
import java.util.Iterator;
import java.util.Arrays;

/**
 * @author Endi S. Dewata
 */
public class ConnectionClient implements ConnectionMBean {

    PenroseClient client;
    ObjectName objectName;

    public ConnectionClient(PenroseClient client, String partitionName, String connectionName) throws Exception {
        this.client = client;
        this.objectName = new ObjectName("Penrose Connections:name="+connectionName +",partition="+partitionName+",type=Connection");
    }

    public PenroseClient getClient() {
        return client;
    }

    public void setClient(PenroseClient client) {
        this.client = client;
    }

    public String getName() throws Exception {
        MBeanServerConnection connection = client.getConnection();
        return (String)connection.getAttribute(objectName, "Name");
    }

    public String getAdapterName() throws Exception {
        MBeanServerConnection connection = client.getConnection();
        return (String)connection.getAttribute(objectName, "AdapterName");
    }

    public String getDescription() throws Exception {
        MBeanServerConnection connection = client.getConnection();
        return (String)connection.getAttribute(objectName, "Description");
    }

    public String getStatus() throws Exception {
        MBeanServerConnection connection = client.getConnection();
        return (String)connection.getAttribute(objectName, "Status");
    }

    public ConnectionCounter getCounter() throws Exception {
        MBeanServerConnection connection = client.getConnection();
        return (ConnectionCounter)connection.getAttribute(objectName, "Counter");
    }

    public void start() throws Exception {
        MBeanServerConnection connection = client.getConnection();
        connection.invoke(
                objectName,
                "start",
                new Object[] { },
                new String[] { }
        );
    }

    public void stop() throws Exception {
        MBeanServerConnection connection = client.getConnection();
        connection.invoke(
                objectName,
                "stop",
                new Object[] { },
                new String[] { }
        );
    }

    public void restart() throws Exception {
        MBeanServerConnection connection = client.getConnection();
        connection.invoke(
                objectName,
                "restart",
                new Object[] { },
                new String[] { }
        );
    }

    public ConnectionConfig getConnectionConfig() throws Exception {
        MBeanServerConnection connection = client.getConnection();
        return (ConnectionConfig)connection.getAttribute(objectName, "ConnectionConfig");
    }

    public Collection getParameterNames() throws Exception {
        MBeanServerConnection connection = client.getConnection();
        Object object = connection.getAttribute(objectName, "ParameterNames");

        if (object instanceof Object[]) {
            return Arrays.asList((Object[])object);

        } else if (object instanceof Collection) {
            return (Collection)object;

        } else {
            return null;
        }
    }

    public String getParameter(String name) throws Exception {
        MBeanServerConnection connection = client.getConnection();
        return (String)connection.invoke(
                objectName,
                "getParameter",
                new Object[] { name },
                new String[] { String.class.getName() }
        );
    }

    public void setParameter(String name, String value) throws Exception {
        MBeanServerConnection connection = client.getConnection();
        connection.invoke(
                objectName,
                "setParameter",
                new Object[] { name, value },
                new String[] { String.class.getName(), String.class.getName() }
        );
    }

    public String removeParameter(String name) throws Exception {
        MBeanServerConnection connection = client.getConnection();
        return (String)connection.invoke(
                objectName,
                "removeParameter",
                new Object[] { name },
                new String[] { String.class.getName() }
        );
    }
}