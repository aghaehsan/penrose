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
package org.safehaus.penrose.config;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.xmlrules.DigesterLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.safehaus.penrose.mapping.EntryDefinition;
import org.safehaus.penrose.mapping.MappingRule;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

/**
 * @author Endi S. Dewata
 */
public class ConfigReader {

    Logger log = LoggerFactory.getLogger(getClass());

    public ConfigReader() {
    }

    public Config read(String directory) throws Exception {
        Config config = new Config();
        loadSourcesConfig(directory+File.separator+"sources.xml", config);
        loadMappingConfig(directory+File.separator+"mapping.xml", config);
        loadModulesConfig(directory+File.separator+"modules.xml", config);
        return config;
    }

    /**
     * Load mapping configuration from a file
     *
     * @param filename the configuration file (ie. mapping.xml)
     * @throws Exception
     */
    public void loadMappingConfig(String filename, Config config) throws Exception {
        MappingRule mappingRule = new MappingRule();
        mappingRule.setFile(filename);
        loadMappingConfig(null, null, mappingRule, config);
    }

    /**
     * Load mapping configuration from a file
     *
     * @param dir
     * @param baseDn
     * @param mappingRule
     * @throws Exception
     */
    public void loadMappingConfig(File dir, String baseDn, MappingRule mappingRule, Config config) throws Exception {
        File file = new File(dir, mappingRule.getFile());
        log.debug("Loading mapping rule from: "+file.getAbsolutePath());

        ClassLoader cl = getClass().getClassLoader();
        URL url = cl.getResource("org/safehaus/penrose/config/mapping-digester-rules.xml");
		Digester digester = DigesterLoader.createDigester(url);
		digester.setValidating(false);
        digester.setClassLoader(cl);
		digester.push(mappingRule);
		digester.parse(file);

        if (mappingRule.getBaseDn() != null) {
            baseDn = mappingRule.getBaseDn();
        }

        Collection contents = mappingRule.getContents();
        for (Iterator i=contents.iterator(); i.hasNext(); ) {
            Object object = i.next();

            if (object instanceof MappingRule) {

                MappingRule mr = (MappingRule)object;
                loadMappingConfig(file.getParentFile(), baseDn, mr, config);

            } else if (object instanceof EntryDefinition) {

                EntryDefinition ed = (EntryDefinition)object;
                if (ed.getDn() == null) {
                    ed.setDn(baseDn);

                } else if (baseDn != null) {
                    String parentDn = ed.getParentDn();
                    ed.setParentDn(parentDn == null ? baseDn : parentDn+","+baseDn);
                }
                config.addEntryDefinition(ed);

                Collection childDefinitions = ed.getChildDefinitions();
                for (Iterator j=childDefinitions.iterator(); j.hasNext(); ) {
                    MappingRule mr = (MappingRule)j.next();
                    loadMappingConfig(file.getParentFile(), ed.getDn(), mr, config);
                }
            }
        }
    }

    /**
     * Load modules configuration from a file
     *
     * @param filename the configuration file (ie. modules.xml)
     * @throws Exception
     */
    public void loadModulesConfig(String filename, Config config) throws Exception {
        if (filename == null) return;
        File file = new File(filename);
        loadModulesConfig(file, config);
    }

    /**
     * Load mapping configuration from a file
     *
     * @param file the configuration file (ie. modules.xml)
     * @throws Exception
     */
	public void loadModulesConfig(File file, Config config) throws Exception {
        log.debug("Loading modules configuration from: "+file.getAbsolutePath());
        ClassLoader cl = getClass().getClassLoader();
        URL url = cl.getResource("org/safehaus/penrose/config/modules-digester-rules.xml");
		Digester digester = DigesterLoader.createDigester(url);
		digester.setValidating(false);
        digester.setClassLoader(cl);
		digester.push(config);
		digester.parse(file);
	}

    /**
     * Load sources configuration from a file
     *
     * @param filename the configuration file (ie. sources.xml)
     * @throws Exception
     */
    public void loadSourcesConfig(String filename, Config config) throws Exception {
        File file = new File(filename);
        loadSourcesConfig(file, config);
    }

	/**
	 * Load sources configuration from a file
	 *
	 * @param file the configuration file (ie. sources.xml)
	 * @throws Exception
	 */
	public void loadSourcesConfig(File file, Config config) throws Exception {
		log.debug("Loading source configuration from: "+file.getAbsolutePath());
        ClassLoader cl = getClass().getClassLoader();
        URL url = cl.getResource("org/safehaus/penrose/config/sources-digester-rules.xml");
		Digester digester = DigesterLoader.createDigester(url);
        digester.setValidating(false);
        digester.setClassLoader(cl);
        digester.push(config);
        digester.parse(file);
	}
}
