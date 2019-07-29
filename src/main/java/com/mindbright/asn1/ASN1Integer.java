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
import java.io.IOException;
import java.math.BigInteger;

public class ASN1Integer extends ASN1Object {

    private BigInteger value;

    public ASN1Integer() {
	super(ASN1.TAG_INTEGER);
    }

    public int encodeValue(ASN1Encoder encoder, OutputStream out)
    	throws IOException
    {
	return encoder.encodeInteger(out, value);
    }

    public void decodeValue(ASN1Decoder decoder, InputStream in, int len)
    	throws IOException
    {
	value = decoder.decodeInteger(in, len);
    }

    public void setValue(BigInteger value) {
	this.value = value;
    }

    public void setValue(long value) {
	this.value = BigInteger.valueOf(value);
    }

    public BigInteger getValue() {
	return value;
    }

}
