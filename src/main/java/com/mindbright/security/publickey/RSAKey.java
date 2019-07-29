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

package com.mindbright.security.publickey;

import com.mindbright.jca.security.Key;

import java.math.BigInteger;

public abstract class RSAKey
    implements com.mindbright.jca.security.interfaces.RSAKey, Key {

    protected BigInteger modulus;

    protected RSAKey(BigInteger modulus) {
	this.modulus = modulus;
    }

    public BigInteger getModulus() {
	return modulus;
    }

    public String getAlgorithm() {
	return "RSA";
    }

    public byte[] getEncoded() {
	return null;
    }

    public String getFormat() {
	return null;
    }

}
