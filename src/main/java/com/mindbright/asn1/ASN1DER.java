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

package com.mindbright.asn1;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;

public class ASN1DER implements ASN1Encoder, ASN1Decoder {

    private final static byte[] BOOL_TRUE  = { (byte)0xff };
    private final static byte[] BOOL_FALSE = { (byte)0x00 };

    public int encode(OutputStream out, ASN1Object object) throws IOException {
	ByteArrayOutputStream encVal = new ByteArrayOutputStream();

	int tag = object.getTag();
	int len = object.encodeValue(this, encVal);
	int cnt = len;

	out.write((byte)tag);
	cnt++;

	if(len < 0x80) {
	    out.write((byte)len);
	    cnt++;
	} else {
	    byte[] encLen = new byte[5];
	    int i = 4;
	    do {
		encLen[i--] = (byte)(len & 0xff);
		len >>>= 8;
	    } while(len > 0);
	    int c = (5 - i);
	    encLen[i] = (byte)(0x80 | (c - 1));
	    out.write(encLen, i, c);
	    cnt += c;
	}

	encVal.writeTo(out);

	return cnt;
    }

    public int encodeBoolean(OutputStream out, boolean b) throws IOException {
	out.write(b ? BOOL_TRUE : BOOL_FALSE);
	return 1;
    }

    public int encodeInteger(OutputStream out, BigInteger i)
	throws IOException
    {
	byte[] enc = i.toByteArray();
	out.write(enc);
	return enc.length;
    }

    public int encodeNull(OutputStream out) throws IOException {
	return 0;
    }

    public int encodeOID(OutputStream out, int[] oid) throws IOException {
	int cnt = 1;
	out.write((40 * oid[0]) + oid[1]);
	for(int i = 2; i < oid.length; i++) {
	    int v = oid[i];
	    if(v < 0x80) {
		out.write((byte)v);
		cnt++;
	    } else {
		byte[] b = new byte[4];
		int    j = 4;
		do {
		    j--;
		    b[j] = (byte)(v & 0x7f);
		    v >>>= 7;
		    if(j < 3) {
			b[j] = (byte)(((int)b[j] & 0xff) | 0x80);
		    }
		} while(v > 0);
		while(j < 4) {
		    out.write(b[j++]);
		    cnt++;
		}
	    }
	}
	return cnt;
    }

    public int encodeString(OutputStream out, byte[] string)
	throws IOException
    {
	out.write(string);
	return string.length;
    }

    public int encodeStructure(OutputStream out, ASN1Structure struct)
	throws IOException
    {
	int count = struct.getCount();
	int size  = 0;
	for(int i = 0; i < count; i++) {
	    ASN1Object component = struct.getComponent(i);
	    size += encode(out, component);
	}
	return size;
    }

    public int decode(InputStream in, ASN1Object object) throws IOException {
	int tag = decodeTag(in);
	if(tag != object.getTag()) {
	    // !!! TODO Handle OPTIONAL/CHOICE/ANY here
	    throw new IOException("Invalid DER encoding");
	}
	int[] dl = decodeLength(in);
	decodeValue(in, dl[0], object);

	return 1 + dl[1] + dl[0];
    }

    public int decodeTag(InputStream in) throws IOException {
	int tag = in.read();
	if((tag & ASN1.MASK_NUMBER) == ASN1.MASK_NUMBER) {
	    throw new IOException("Long form of tags not supported yet");
	}
	return tag;
    }

    public int[] decodeLength(InputStream in) throws IOException {
	int cnt = 1;
	int len = in.read();

	if(len > 0x80) {
	    int n = (len & 0x7f);
	    len = 0;
	    for(int i = 0; i < n; i++) {
		len <<= 8;
		len += in.read();
		cnt++;
	    }
	}

	return new int[] { len, cnt };
    }

    public void decodeValue(InputStream in, int len, ASN1Object object)
	throws IOException
    {
	object.decodeValue(this, in, len);
    }

    public boolean decodeBoolean(InputStream in, int len) throws IOException {
	byte b = (byte)in.read();
	if(len != 1 ||
	   (b != BOOL_FALSE[0] && b != BOOL_TRUE[0])) {
	    throw new IOException("Invalid DER encoding of boolean");
	}
	return (b == BOOL_TRUE[0]);
    }

    public BigInteger decodeInteger(InputStream in, int len)
	throws IOException
    {
	byte[] enc = new byte[len];
	readNextN(in, enc, len);

	return new BigInteger(enc);
    }

    public void decodeNull(InputStream in, int len) throws IOException {
	if(len != 0) {
	    throw new IOException("Invalid DER encoding of NULL");
	}
    }

    public int[] decodeOID(InputStream in, int len) throws IOException {
	int[] oid = new int[len + 1];

	if(len < 1) {
	    throw new IOException("Invalid DER encoding of OID");
	}

	oid[0] = in.read();
	len--;
	if(oid[0] > ((2 * 40) + 39)) {
	    oid[1] = oid[0] - (2 * 40);
	    oid[0] = 2;
	} else {
	    oid[1] = oid[0] % 40;
	    oid[0] = oid[0] / 40;
	}

	int i = 2;
	while(len > 0) {
	    int v = 0;
	    int d = in.read();
	    len--;
	    while(d > 0x80) {
		d &= 0x7f;
		v <<= 7;
		v |= d;
		d = in.read();
		len--;
	    }
	    v <<= 7;
	    v |= d;
	    oid[i++] = v;
	}

	int[] tmp = oid;
	oid = new int[i];
	System.arraycopy(tmp, 0, oid, 0, i);

	return oid;
    }

    public byte[] decodeString(InputStream in, int len) throws IOException {
	byte[] string = new byte[len];
	readNextN(in, string, len);
	return string;
    }

    public void decodeStructure(InputStream in, int len, ASN1Structure struct)
	throws IOException
    {
	int count = struct.getCount();
	int i     = 0;
	while(len > 0) {
	    int   t  = decodeTag(in);
	    int[] dl = decodeLength(in);

	    len -= 1 + dl[1];

	    if(dl[0] > len) {
		throw new IOException("DER struct ended prematurely");
	    }

	    // !!! TODO check count / max count constraint here

	    ASN1Object component;

	    if(struct instanceof ASN1Set) {
		component = struct.getDistinctComponent(t);
	    } else {
		component = struct.getComponent(i);
	    }

	    decodeValue(in, dl[0], component);

	    i++;
	    len -= dl[0];
	}
    }

    private void readNextN(InputStream in, byte[] buf, int n)
	throws IOException
    {
	int wPos = 0;
	while(wPos < n) {
	    int s = in.read(buf, wPos, n - wPos);
	    if(s == -1)
		throw new IOException("DER encoding ended prematurely");
	    wPos += s;
	}
    }

    /* !!! DEBUG
    public static void main(String[] argv) {
	try {
	    ASN1DER der = new ASN1DER();
	    ByteArrayOutputStream ba = new ByteArrayOutputStream();

	    ASN1SequenceOf so = new ASN1SequenceOf(ASN1OID.class);
	    ASN1OID o = new ASN1OID();
	    o = (ASN1OID)so.getComponent(0);
	    o.setString("1.2.840.113549.2.5");
	    o = (ASN1OID)so.getComponent(1);
	    o.setString("1.2.840.113549.2.2");
	    o = (ASN1OID)so.getComponent(2);
	    o.setString("1.3.14.3.2.26");
	    o = (ASN1OID)so.getComponent(3);
	    o.setString("1.3.36.3.2.1");

	    der.encode(ba, so);
	    com.mindbright.util.HexDump.print(ba.toByteArray());

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
    */

}
