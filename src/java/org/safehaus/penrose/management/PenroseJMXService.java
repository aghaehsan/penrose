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
package org.safehaus.penrose.management;

import mx4j.tools.naming.NamingService;
import mx4j.tools.adaptor.http.HttpAdaptor;
import mx4j.tools.adaptor.http.XSLTProcessor;
import mx4j.log.Log4JLogger;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.MBeanServerFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.JMXConnectorServerFactory;
import java.util.HashMap;

import org.safehaus.penrose.config.PenroseConfig;
import org.safehaus.penrose.PenroseServer;
import org.safehaus.penrose.Penrose;
import org.safehaus.penrose.service.Service;
import org.apache.log4j.Logger;

/**
 * @author Endi S. Dewata
 */
public class PenroseJMXService extends Service {

    public Logger log = Logger.getLogger(PenroseJMXService.class);

    private PenroseServer penroseServer;
    private Penrose penrose;

    PenroseJMXAuthenticator jmxAuthenticator;

    MBeanServer mbeanServer;

    ObjectName penroseAdminName = ObjectName.getInstance(PenroseClient.MBEAN_NAME);
    PenroseAdmin penroseAdmin;

    ObjectName registryName = ObjectName.getInstance("naming:type=rmiregistry");
    NamingService registry;

    ObjectName rmiConnectorName = ObjectName.getInstance("connectors:type=rmi,protocol=jrmp");
    JMXConnectorServer rmiConnector;

    ObjectName httpConnectorName = ObjectName.getInstance("connectors:type=http");
    HttpAdaptor httpConnector;

    ObjectName xsltProcessorName = ObjectName.getInstance("connectors:type=http,processor=xslt");
    XSLTProcessor xsltProcessor;

    static {
        System.setProperty("jmx.invoke.getters", "true");
        System.setProperty("javax.management.builder.initial", "mx4j.server.MX4JMBeanServerBuilder");
    }

    public PenroseJMXService() throws Exception {

        mx4j.log.Log.redirectTo(new Log4JLogger());
    }

    public void start() throws Exception {

        PenroseConfig penroseConfig = penrose.getPenroseConfig();

        mbeanServer = MBeanServerFactory.createMBeanServer();

        if (penroseServer != null) {
            penroseAdmin = new PenroseAdmin();
            penroseAdmin.setPenroseServer(penroseServer);

            mbeanServer.registerMBean(penroseAdmin, penroseAdminName);
        }

        if (penroseConfig.getJmxRmiPort() >= 0) {

            registry = new NamingService(penroseConfig.getJmxRmiPort());
            mbeanServer.registerMBean(registry, registryName);
            registry.start();

            JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://localhost/jndi/rmi://localhost:"+penroseConfig.getJmxRmiPort()+"/jmx");
            jmxAuthenticator = new PenroseJMXAuthenticator(penrose);

            HashMap environment = new HashMap();
            environment.put("jmx.remote.authenticator", jmxAuthenticator);

            rmiConnector = JMXConnectorServerFactory.newJMXConnectorServer(url, environment, null);
            mbeanServer.registerMBean(rmiConnector, rmiConnectorName);
            rmiConnector.start();

            log.warn("Listening to port "+penroseConfig.getJmxRmiPort()+".");
        }
/*
        xsltProcessor = new XSLTProcessor();
        mbeanServer.registerMBean(xsltProcessor, xsltProcessorName);

        httpConnector = new HttpAdaptor(8112, "localhost");
        httpConnector.setProcessorName(xsltProcessorName);
        mbeanServer.registerMBean(httpConnector, httpConnectorName);
        httpConnector.start();

        log.warn("Listening to port "+penroseConfig.getJmxHttpPort()+".");
*/
    }

    public void stop() throws Exception {

        PenroseConfig penroseConfig = penrose.getPenroseConfig();
/*
        httpConnector.stop();
        mbeanServer.unregisterMBean(httpConnectorName);
        mbeanServer.unregisterMBean(xsltProcessorName);
*/
        if (penroseConfig.getJmxRmiPort() >= 0) {
            rmiConnector.stop();
            mbeanServer.unregisterMBean(rmiConnectorName);

            registry.stop();
            mbeanServer.unregisterMBean(registryName);
        }


        mbeanServer.unregisterMBean(penroseAdminName);
        MBeanServerFactory.releaseMBeanServer(mbeanServer);

        log.warn("JMX service has been shutdown.");
    }

    public PenroseServer getPenroseServer() {
        return penroseServer;
    }

    public void setPenroseServer(PenroseServer penroseServer) {
        this.penroseServer = penroseServer;
    }

    public Penrose getPenrose() {
        return penrose;
    }

    public void setPenrose(Penrose penrose) {
        this.penrose = penrose;
    }
}