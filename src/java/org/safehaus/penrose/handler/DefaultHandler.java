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
package org.safehaus.penrose.handler;


/**
 * @author Endi S. Dewata
 */
public class DefaultHandler extends Handler {


    /**
     * Load entries into the temporary table in the cache (synchronized)
     *
     * @param con the JDBC connection
     * @param source the source (from config)
     * @param filter the filter (from config)
     * @param attributeNames collection of attribute names
     * @param temporary whether we are using temporary tables
     * @throws Exception
     */
/*
    public void loadEntries(Source source, Filter filter, Collection attributeNames, boolean temporary) throws Exception {
        SourceDefinition sourceConfig = (SourceDefinition)penrose.getConfig().getSources().get(source.getName());
    	String tableName = cache.getEntryTableName(sourceConfig, false);
    	Boolean locked = (Boolean)tableStatus.get(tableName);
    	while (locked == Boolean.TRUE) {
    		// need to wait
    		try {
    			wait();
    		} catch (InterruptedException ex) {}
    		locked = (Boolean)tableStatus.get(tableName);
    	}
    	tableStatus.put(tableName, Boolean.TRUE);
        tableStatus.remove(tableName);
        notifyAll();
    }

    public void loadResults() throws Exception {

        Map entries = config.getEntryDefinitions();

        Iterator iter = entries.keySet().iterator();
        while (iter.hasNext()) {
            String dn = (String) iter.next();
            EntryDefinition entry = config.getDn(dn);

            // See if the result is dirty
            boolean dirty = false;
            List sources = entry.getSources();
            log.debug("dn: " + dn);

            if (entry.isDynamic() && dirty) {
                String searchDn = dn.replace("...", "*");
                log.debug("Loading results table for dn: " + searchDn);
                Entry sr = penrose.getSearchHandler().loadObject(null, searchDn, new ArrayList());
            }
        }
    }


    public Date getModifyTime(EntryDefinition entry, SourceDefinition sourceConfig, String filter) throws Exception {

        String t1 = cache.getEntryTableName(sourceConfig, true);
        SourceHome s1 = (SourceHome)homes.get(t1);
        return s1.getModifyTime(filter);
    }

    public void updateExpiration(SourceDefinition sourceConfig, Calendar calendar) throws Exception {

        int defaultCacheExpiration = config.getSourceCache().getCacheExpiration();
        String s = sourceConfig.getParameter(SourceDefinition.CACHE_EXPIRATION);

        Integer cacheExpiration = s == null ? new Integer(defaultCacheExpiration) : new Integer(s);
        if (cacheExpiration.intValue() < 0) cacheExpiration = new Integer(Integer.MAX_VALUE);

        Calendar c = (Calendar) calendar.clone();
        c.add(Calendar.MINUTE, cacheExpiration.intValue());

        sourceExpirationHome.setExpiration(sourceConfig, cacheExpiration.intValue() == 0 ? null : c.getTime());
        //sourceExpirationHome.setModifyTime(sourceConfig, calendar.getTime());
    }

    public boolean isExpired(SourceDefinition sourceConfig, Calendar calendar) throws Exception {

        Date expiration = sourceExpirationHome.getExpiration(sourceConfig);
        return expiration == null || expiration.before(calendar.getTime());
    }

	public String toString(int[] x) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; x != null && i < x.length; i++) {
			sb.append(x[i] + " ");
		}
		return sb.toString();
	}

	public void updateDirtyEntries(
            SourceDefinition sourceConfig,
			Filter filter,
            Collection attributeNames,
            boolean temporary)
			throws Exception {
		log.info("--------------------------------------------------------------------------------");
		log.info("LOAD DIRTY ENTRIES");
		log.info(" - source: " + sourceConfig.getName());
		log.info(" - filter: " + filter);
		log.info(" - attributeNames: " + attributeNames);
		log.info(" - temporary: " + temporary);

		CacheEvent beforeEvent = new CacheEvent(penrose, sourceConfig,
				CacheEvent.BEFORE_LOAD_ENTRIES);
		postCacheEvent(sourceConfig, beforeEvent);

		ConnectionConfig connection = (ConnectionConfig)config.connections.get(sourceConfig.getConnectionName());
		Adapter adapter = (Adapter) penrose.getAdapterConfigs().get(connection.getAdapterName());

		SearchResults results = adapter.search(sourceConfig, null);

		log.debug("Rows:");
		for (Iterator j = results.iterator(); j.hasNext();) {
			Map row = (Map) j.next();

			log.debug(" - " + row);
			// TODO update this
			//updateRow(sourceConfig, row, temporary);
		}

		CacheEvent afterEvent = new CacheEvent(penrose, sourceConfig, CacheEvent.AFTER_LOAD_ENTRIES);
		postCacheEvent(sourceConfig, afterEvent);
	}

	public void setValidity(EntryDefinition entry, Map values, boolean validity) throws Exception {

        Collection rows = penrose.getTransformEngine().generateCrossProducts(values);

        for (Iterator i=rows.iterator(); i.hasNext(); ) {
            Map row = (Map)i.next();
            if (validity) {
                log.debug("Validating cache: "+row);
            } else {
                log.debug("Invalidating cache: "+row);
            }

            String tableName = getEntryTableName(entry, false);
            EntryHome resultHome = (EntryHome)homes.get(tableName);
            //resultHome.setValidity(row, validity);
        }

        Date now = new Date();
        resultExpirationHome.setModifyTime(entry, now);
	}

	public void updateResult(String operation, EntryDefinition entry, Map row, boolean temporary) throws Exception {

		log.debug("UPDATE RESULT called ========================");
		log.debug("operation = " + operation);
		log.debug("entry = " + entry);
		log.debug("row = " + row);
		log.debug("temporary = " + temporary);

        String tableName = getEntryTableName(entry, temporary);
        EntryHome resultHome = (EntryHome)homes.get(tableName);

        log.debug("row = " + row);

        // build keys list
        List keys = new ArrayList();
        keys.addAll(row.keySet());
        log.debug("keys = " + keys);

        // build values (list of list)
        List values = new ArrayList();
        for (Iterator i = row.keySet().iterator(); i.hasNext(); ) {
            String key = (String)i.next();

            Collection c = (Collection) row.get(key);

            List mvalues = new ArrayList();
            if (entry.getAttributeValues().containsKey(key)) {
                mvalues.addAll(c);

            } else if (c.size() >= 1) {
                Object o = c.iterator().next();
                mvalues.add(o);
            }

            values.add(mvalues);
        }
        log.debug("values = " + values);

        // indices
        int[] indices = new int[keys.size()];

        boolean keepgoing = true;
        while (keepgoing) {
            // update
            Map mrow = new HashMap();
            log.debug("indices = " + toString(indices));

            for (int i = 0; i < keys.size() && i < values.size(); i++) {
                try {
                    String key = (String) keys.get(i);
                    mrow.put(key, ((List) values.get(i)).get(indices[i]));
                } catch (IndexOutOfBoundsException ex) {
                    log.debug(ex.toString());
                } catch (Exception ex) {
                    log.error(ex.toString());
                }
            }
            log.debug("operation = " + operation);
            log.debug("mrow = " + mrow);

            if ("insert".equals(operation)) {
                resultHome.insert(mrow);

            } else if ("delete".equals(operation)) {
                resultHome.delete(mrow);

            } else if ("invalidate".equals(operation)) {
                resultHome.setValidity(mrow, false);

            } else if ("validate".equals(operation)) {
                resultHome.setValidity(mrow, true);
            }

            // advance to next
            for (int i = indices.length - 1; i >= 0; i--) {
                if (indices[i] < ((List) values.get(i)).size() - 1) {
                    indices[i]++;
                    for (int j = i + 1; j < indices.length; j++) {
                        indices[j] = 0;
                    }
                    break;
                } else if (i == 0) {
                    keepgoing = false;
                }
            }
        }

        // Update the expiration and modification time
        Date now = new Date();
        resultExpirationHome.setModifyTime(entry, now);
	}
*/
}