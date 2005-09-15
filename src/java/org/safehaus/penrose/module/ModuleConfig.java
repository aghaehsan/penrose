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
package org.safehaus.penrose.module;

import java.util.LinkedHashMap;
import java.util.Collection;

/**
 * @author Endi S. Dewata
 */
public class ModuleConfig implements Cloneable {

    public String moduleName;
    public String moduleClass;
    public LinkedHashMap parameters = new LinkedHashMap();

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public void clearParameters() {
        parameters.clear();
    }

    public void setParameter(String name, String value) {
        parameters.put(name, value);
    }

    public void removeParameter(String name) {
        parameters.remove(name);
    }

    public String getParameter(String name) {
        return (String)parameters.get(name);
    }

    public Collection getParameterNames() {
        return parameters.keySet();
    }

    public String getModuleClass() {
        return moduleClass;
    }

    public void setModuleClass(String moduleClass) {
        this.moduleClass = moduleClass;
    }

    public int hashCode() {
        return moduleName.hashCode() + moduleClass.hashCode() + parameters.hashCode();
    }

    public boolean equals(Object object) {
        boolean value = false;
        try {
            if (this == object) {
                value = true;
                return value;
            }

            if((object == null) || (object.getClass() != this.getClass())) {
                value = false;
                return value;
            }

            ModuleConfig moduleConfig = (ModuleConfig)object;
            if (!moduleName.equals(moduleConfig.getModuleName())) {
                value = false;
                return value;
            }

            if (!moduleClass.equals(moduleConfig.getModuleClass())) {
                value = false;
                return value;
            }

            value = true;
            return value;

        } finally {
            //System.out.println("["+this+"] equals("+object+") => "+value);
        }
    }

    public Object clone() {
        ModuleConfig config = new ModuleConfig();
        config.moduleName = moduleName;
        config.moduleClass = moduleClass;
        config.parameters.putAll(parameters);
        return config;
    }

    public String toString() {
        return "ModuleConfig("+moduleName+")";
    }
}
