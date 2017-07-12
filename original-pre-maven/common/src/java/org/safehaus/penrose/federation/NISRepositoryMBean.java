package org.safehaus.penrose.federation;

import org.safehaus.penrose.synchronization.SynchronizationResult;

import java.util.Collection;

/**
 * @author Endi Sukma Dewata
 */
public interface NISRepositoryMBean extends FederationRepositoryMBean {

    public SynchronizationResult synchronize(String repositoryName, Collection<String> mapNames) throws Exception;
}