/******************************************************************************
 *
 * Copyright (c) 1999-2001 AppGate AB. All Rights Reserved.
 * 
 * This file contains Original Code and/or Modifications of Original Code as
 * defined in and that are subject to the MindTerm Public Source License,
 * Version 1.3, (the 'License'). You may not use this file except in compliance
 * with the License.
 * 
 * You should have received a copy of the MindTerm Public Source License
 * along with this software; see the file LICENSE.  If not, write to
 * AppGate AB, Stora Badhusgatan 18-20, 41121 Goteborg, SWEDEN
 *
 *****************************************************************************/

package com.mindbright.util;

import java.math.BigInteger;

public class HexDump {

    /* hexadecimal digits.
     */
    private static final char[] HEX_DIGITS = {
	'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'
    };

    /**
     * Returns a string of 8 hexadecimal digits (most significant
     * digit first) corresponding to the integer <i>n</i>, which is
     * treated as unsigned.
     */
    public static String intToString (int n) {
	char[] buf = new char[8];
	for(int i = 7; i >= 0; i--) {
	    buf[i] = HEX_DIGITS[n & 0x0F];
	    n >>>= 4;
	}
	return new String(buf);
    }

    /**
     * Returns a string of hexadecimal digits from a byte array. Each
     * byte is converted to 2 hex symbols.
     */
    public static String toString(byte[] ba) {
	return toString(ba, 0, ba.length);
    }

    public static String toString(byte[] ba, int offset, int length) {
	char[] buf = new char[length * 2];
	for(int i = offset, j = 0, k; i < offset+length; ) {
	    k = ba[i++];
	    buf[j++] = HEX_DIGITS[(k >>> 4) & 0x0F];
	    buf[j++] = HEX_DIGITS[ k      & 0x0F];
	}
	return new String(buf);
    }

    public static String formatHex(int i, int sz) {
	String str = Integer.toHexString(i);
	while(str.length() < sz) {
	    str = "0" + str;
	}
	return str;
    }

    public static void print(byte[] buf, int off, int len) {
	print(null, true, buf, off, len);
    }

    public static synchronized void print(String header, boolean showAddr,
					  byte[] buf, int off, int len)
    {
	int i, j, jmax;
	int c;

	if(header != null) {
	    System.out.println(header);
	}

	for(i = 0; i < len; i += 0x10) {
	    StringBuffer line = new StringBuffer();

	    if(showAddr) {
		line.append(formatHex(i + off, 8));
		line.append(": ");
	    }

	    jmax = len - i;
	    jmax = jmax > 16 ? 16 : jmax;

	    for(j = 0; j < jmax; j++) {
		c = ((int)buf[off+i+j] + 0x100) % 0x100;
		line.append(formatHex(c, 2));
		if ((j % 2) == 1)
		    line.append(" ");
	    }

	    for(; j < 16; j++) {
		line.append("  ");
		if ((j % 2) == 1)
		    line.append(" ");
	    }

	    line.append(" ");

	    for(j = 0; j < jmax; j++) {
		c = ((int)buf[off+i+j] + 0x100) % 0x100;
		c = c < 32 || c >= 127 ? '.' : c;
		line.append((char)c);
	    }

	    System.out.println(line.toString());
	}
    }

    public static void print(byte[] buf) {
	print(buf, 0, buf.length);
    }
    
    public static void print(BigInteger bi) {
	byte[] raw = bi.toByteArray();
	if(raw.length == 1 && raw[0] == (byte)0x00)
	    raw = new byte[0];
	print(raw);
    }
    

}
