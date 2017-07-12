/**
 * Copyright 2009 Red Hat, Inc.
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
package org.safehaus.penrose.ldapbackend.apacheds;

import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.ietf.ldap.LDAPException;

import javax.naming.NamingException;

/**
 * @author Endi S. Dewata
 */
public class ExceptionTool {

    public static NamingException createNamingException(Exception e) {
        if (e instanceof NamingException) return (NamingException)e;

        if (e instanceof LDAPException) {
            LDAPException ldapException = (LDAPException)e;
            return createNamingException(ldapException.getResultCode(), ldapException.getMessage());
        }

        ResultCodeEnum rce = ResultCodeEnum.getResultCode(e);
        return new LdapNamingException(e.getMessage(), rce);
    }

    public static NamingException createNamingException(int rc) {
        return ExceptionTool.createNamingException(rc, null);
    }

    public static NamingException createNamingException(int rc, String message) {
        ResultCodeEnum rce = ResultCodeEnum.getResultCodeEnum(rc);
        return new LdapNamingException(message, rce);
    }
}
