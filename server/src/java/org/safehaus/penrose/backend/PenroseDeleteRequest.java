package org.safehaus.penrose.backend;

import org.safehaus.penrose.ldap.DeleteRequest;
import com.identyx.javabackend.DN;

/**
 * @author Endi S. Dewata
 */
public class PenroseDeleteRequest
        extends PenroseRequest
        implements com.identyx.javabackend.DeleteRequest {

    DeleteRequest deleteRequest;

    public PenroseDeleteRequest(DeleteRequest deleteRequest) {
        super(deleteRequest);
        this.deleteRequest = deleteRequest;
    }

    public void setDn(DN dn) throws Exception {
        PenroseDN penroseDn = (PenroseDN)dn;
        deleteRequest.setDn(penroseDn.getDn());
    }

    public DN getDn() throws Exception {
        return new PenroseDN(deleteRequest.getDn());
    }

    public DeleteRequest getDeleteRequest() {
        return deleteRequest;
    }
}
