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
package org.safehaus.penrose.ad;

import org.safehaus.penrose.util.BinaryUtil;

/**
 * @author Endi S. Dewata
 */
public class ActiveDirectory {

    public static byte[] toUnicodePassword(Object password) throws Exception {
        String newPassword;
        if (password instanceof byte[]) {
            newPassword = "\""+new String((byte[])password)+ "\"";
        } else {
            newPassword = "\""+password+ "\"";
        }

        return newPassword.getBytes("UTF-16LE");
/*
        byte unicodeBytes[] = newPassword.getBytes("Unicode");
        byte bytes[]  = new byte[unicodeBytes.length-2];

        System.arraycopy(unicodeBytes, 2, bytes, 0, unicodeBytes.length-2);

        return bytes;
*/
    }

    public static String getGUID(byte[] guid) {
        try {
            StringBuilder sb = new StringBuilder();

            sb.append(byte2hex(guid[3]));
            sb.append(byte2hex(guid[2]));
            sb.append(byte2hex(guid[1]));
            sb.append(byte2hex(guid[0]));
            sb.append("-");
            sb.append(byte2hex(guid[5]));
            sb.append(byte2hex(guid[4]));
            sb.append("-");
            sb.append(byte2hex(guid[7]));
            sb.append(byte2hex(guid[6]));
            sb.append("-");
            sb.append(byte2hex(guid[8]));
            sb.append(byte2hex(guid[9]));
            sb.append("-");
            sb.append(byte2hex(guid[10]));
            sb.append(byte2hex(guid[11]));
            sb.append(byte2hex(guid[12]));
            sb.append(byte2hex(guid[13]));
            sb.append(byte2hex(guid[14]));
            sb.append(byte2hex(guid[15]));

            return sb.toString();

        } catch (Exception e) {
            return BinaryUtil.encode(BinaryUtil.BIG_INTEGER, guid);
        }
    }

    public static String getSID(byte[] sid) {

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("S-");

             // get version
            int version = sid[0];
            sb.append(Integer.toString(version));
            sb.append("-");

            // get authority

            String rid = "";
            for (int i=6; i>0; i--) {
                rid += byte2hex(sid[i]);
            }

            long authority = Long.parseLong(rid);
            sb.append(Long.toString(authority));

            //next byte is the count of sub-authorities
            int count = sid[7]&0xFF;

            //iterate all the sub-auths
            for (int i=0;i<count;i++) {
                rid = "";
                for (int j=11; j>7; j--) {
                    rid += byte2hex(sid[j+(i*4)]);
                }
                sb.append("-");
                sb.append(Long.parseLong(rid, 16));
            }

            return sb.toString();

        } catch (Exception e) {
            return BinaryUtil.encode(BinaryUtil.BIG_INTEGER, sid);
        }
    }

    public static String byte2hex(byte b) {
        int i = (int)b & 0xFF;
        return (i <= 0x0F) ? "0" + Integer.toHexString(i) : Integer.toHexString(i);
    }

}
