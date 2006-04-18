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
package org.safehaus.penrose.schema;

import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.Iterator;
import java.io.FileReader;
import java.io.File;

/**
 * @author Endi S. Dewata
 */
public class SchemaReader {

    Logger log = Logger.getLogger(getClass());

    private String home;

    public SchemaReader() {
    }

    public SchemaReader(String home) {
        this.home = home;
    }

    public Schema read(SchemaConfig schemaConfig) throws Exception {

        String path = (home == null ? "" : home+File.separator)+schemaConfig.getPath();

        log.debug("Loading schema "+path+".");

        Schema schema = new Schema(schemaConfig);

        File file = new File(path);
        if (!file.exists()) return schema;

        FileReader in = new FileReader(file);
        
        SchemaParser parser = new SchemaParser(in);
        Collection c = parser.parse();

        for (Iterator i = c.iterator(); i.hasNext();) {
            Object o = i.next();
            if (o instanceof AttributeType) {
                AttributeType at = (AttributeType) o;
                schema.addAttributeType(at);

            } else if (o instanceof ObjectClass) {
                ObjectClass oc = (ObjectClass) o;
                schema.addObjectClass(oc);

            } else {
                //log.debug(" - ERROR");
            }
        }

        return schema;
    }
}