package org.safehaus.penrose.test.mapping.join;

import org.safehaus.penrose.test.jdbc.JDBCTestCase;
import org.safehaus.penrose.partition.*;
import org.safehaus.penrose.PenroseFactory;
import org.safehaus.penrose.Penrose;
import org.safehaus.penrose.directory.EntryConfig;
import org.safehaus.penrose.directory.AttributeMapping;
import org.safehaus.penrose.directory.FieldMapping;
import org.safehaus.penrose.directory.SourceMapping;
import org.safehaus.penrose.config.PenroseConfig;
import org.safehaus.penrose.connection.ConnectionConfig;
import org.safehaus.penrose.source.SourceConfig;
import org.safehaus.penrose.source.FieldConfig;
import org.safehaus.penrose.naming.PenroseContext;

/**
 * @author Endi S. Dewata
 */
public class JoinTestCase extends JDBCTestCase {

    public Penrose penrose;
    public PenroseConfig penroseConfig;

    public JoinTestCase() throws Exception {
    }

    public void setUp() throws Exception {

        executeUpdate("create table groups ("+
                "groupname varchar(10), "+
                "description varchar(30), "+
                "primary key (groupname))"
        );

        executeUpdate("create table usergroups ("+
                "groupname varchar(10), "+
                "username varchar(10), "+
                "primary key (groupname, username))"
        );

        PenroseFactory penroseFactory = PenroseFactory.getInstance();
        penrose = penroseFactory.createPenrose();
        penrose.start();

        penroseConfig = penrose.getPenroseConfig();

        PartitionConfig partitionConfig = new PartitionConfig("DEFAULT");

        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setAdapterName("JDBC");
        connectionConfig.setName("HSQLDB");
        connectionConfig.setParameter("driver", driver);
        connectionConfig.setParameter("url", url);
        connectionConfig.setParameter("user", user);
        connectionConfig.setParameter("password", password);
        partitionConfig.getConnectionConfigManager().addConnectionConfig(connectionConfig);

        SourceConfig groupsSource = new SourceConfig();
        groupsSource.setName("groups");
        groupsSource.setConnectionName("HSQLDB");
        groupsSource.setParameter("table", "groups");
        groupsSource.addFieldConfig(new FieldConfig("groupname", true));
        groupsSource.addFieldConfig(new FieldConfig("description"));
        partitionConfig.getSourceConfigManager().addSourceConfig(groupsSource);

        SourceConfig usergroupsSource = new SourceConfig();
        usergroupsSource.setName("usergroups");
        usergroupsSource.setConnectionName("HSQLDB");
        usergroupsSource.setParameter("table", "usergroups");
        usergroupsSource.addFieldConfig(new FieldConfig("groupname", true));
        usergroupsSource.addFieldConfig(new FieldConfig("username", true));
        partitionConfig.getSourceConfigManager().addSourceConfig(usergroupsSource);

        EntryConfig ou = new EntryConfig("ou=Groups,dc=Example,dc=com");
        ou.addObjectClass("organizationalUnit");
        ou.addAttributeMapping(new AttributeMapping("ou", AttributeMapping.CONSTANT, "Groups", true));
        partitionConfig.getDirectoryConfig().addEntryConfig(ou);

        EntryConfig groups = new EntryConfig("cn=...,ou=Groups,dc=Example,dc=com");
        groups.addObjectClass("groupOfUniqueNames");
        groups.addAttributeMapping(new AttributeMapping("cn", AttributeMapping.VARIABLE, "g.groupname", true));
        groups.addAttributeMapping(new AttributeMapping("description", AttributeMapping.VARIABLE, "g.description"));
        groups.addAttributeMapping(new AttributeMapping("uniqueMember", AttributeMapping.VARIABLE, "ug.username"));

        SourceMapping groupsMapping = new SourceMapping();
        groupsMapping.setName("g");
        groupsMapping.setSourceName("groups");
        groupsMapping.addFieldMapping(new FieldMapping("groupname", FieldMapping.VARIABLE, "cn"));
        groupsMapping.addFieldMapping(new FieldMapping("description", FieldMapping.VARIABLE, "description"));
        groups.addSourceMapping(groupsMapping);

        SourceMapping usergroupsMapping = new SourceMapping();
        usergroupsMapping.setName("ug");
        usergroupsMapping.setSourceName("usergroups");
        usergroupsMapping.addFieldMapping(new FieldMapping("groupname", FieldMapping.VARIABLE, "g.groupname"));
        usergroupsMapping.addFieldMapping(new FieldMapping("username", FieldMapping.VARIABLE, "uniqueMember"));
        usergroupsMapping.setSearch(SourceMapping.REQUIRED);
        groups.addSourceMapping(usergroupsMapping);

        partitionConfig.getDirectoryConfig().addEntryConfig(groups);

        PenroseContext penroseContext = penrose.getPenroseContext();
        PartitionManager partitionManager = penrose.getPartitionManager();

        PartitionFactory partitionFactory = new PartitionFactory();
        partitionFactory.setPenroseConfig(penroseConfig);
        partitionFactory.setPenroseContext(penroseContext);

        Partition partition = partitionFactory.createPartition(partitionConfig);

        partitionManager.addPartition(partition);
    }

    public void tearDown() throws Exception {
        penrose.stop();

        executeUpdate("drop table groups");
        executeUpdate("drop table usergroups");
    }
}
