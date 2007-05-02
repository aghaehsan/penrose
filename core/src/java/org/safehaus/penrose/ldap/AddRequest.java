package org.safehaus.penrose.ldap;

/**
 * @author Endi S. Dewata
 */
public class AddRequest extends Request {

    protected DN dn;
    protected Attributes attributes;

    public AddRequest() {
    }

    public AddRequest(AddRequest request) {
        super(request);
        dn = request.getDn();
        attributes = request.getAttributes();
    }

    public DN getDn() {
        return dn;
    }

    public void setDn(String dn) throws Exception {
        this.dn = new DN(dn);
    }

    public void setDn(RDN rdn) throws Exception {
        this.dn = new DN(rdn);
    }

    public void setDn(DN dn) {
        this.dn = dn;
    }

    public Attributes getAttributes() {
        return attributes;
    }

    public void setAttributes(Attributes attributes) {
        this.attributes = attributes;
    }
}
