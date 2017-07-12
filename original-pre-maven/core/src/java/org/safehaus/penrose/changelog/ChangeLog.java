package org.safehaus.penrose.changelog;

import org.safehaus.penrose.ldap.Request;
import org.safehaus.penrose.ldap.SearchResult;

/**
 * @author Endi S. Dewata
 */
public class ChangeLog extends SearchResult {

    public final static int ADD    = 1;
    public final static int MODIFY = 2;
    public final static int MODRDN = 3;
    public final static int DELETE = 4;

    protected Number changeNumber;
    protected Object changeTime;
    protected int changeAction;
    protected String changeUser;

    protected Request request;

    public Number getChangeNumber() {
        return changeNumber;
    }

    public void setChangeNumber(Number changeNumber) {
        this.changeNumber = changeNumber;
    }

    public Object getChangeTime() {
        return changeTime;
    }

    public void setChangeTime(Object changeTime) {
        this.changeTime = changeTime;
    }

    public int getChangeAction() {
        return changeAction;
    }

    public void setChangeAction(int changeAction) {
        this.changeAction = changeAction;
    }

    public String getChangeUser() {
        return changeUser;
    }

    public void setChangeUser(String changeUser) {
        this.changeUser = changeUser;
    }

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }
}
