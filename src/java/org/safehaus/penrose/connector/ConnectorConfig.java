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
package org.safehaus.penrose.connector;

import org.safehaus.penrose.cache.CacheConfig;

import java.io.Serializable;
import java.util.Properties;
import java.util.Collection;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * @author Endi S. Dewata
 */
public class ConnectorConfig implements Serializable {

    public final static String THREAD_POOL_SIZE = "threadPoolSize";

    public final static int DEFAULT_THREAD_POOL_SIZE = 20;

    private String connectorName = "DEFAULT";
    private String connectorClass = Connector.class.getName();
    private String description;

    private Properties parameters = new Properties();

    private Map cacheConfigs = new LinkedHashMap();

    public String getConnectorClass() {
        return connectorClass;
    }

    public void setConnectorClass(String connectorClass) {
        this.connectorClass = connectorClass;
    }

    public void setParameter(String name, String value) {
        parameters.setProperty(name, value);
    }

    public void removeParameter(String name) {
        parameters.remove(name);
    }

    public Collection getParameterNames() {
        return parameters.keySet();
    }

    public String getParameter(String name) {
        return parameters.getProperty(name);
    }

    public String getConnectorName() {
        return connectorName;
    }

    public void setConnectorName(String connectorName) {
        this.connectorName = connectorName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void addCacheConfig(CacheConfig cacheConfig) {
    	cacheConfigs.put(cacheConfig.getCacheName(), cacheConfig);
    }

    public CacheConfig removeCacheConfig(String name) {
        return (CacheConfig)cacheConfigs.remove(name);
    }

    public CacheConfig getCacheConfig() {
        return (CacheConfig)cacheConfigs.get("DEFAULT");
    }

    public CacheConfig getCacheConfig(String name) {
        return (CacheConfig)cacheConfigs.get(name);
    }

    public Collection getCacheConfigs() {
    	return cacheConfigs.values();
    }

}