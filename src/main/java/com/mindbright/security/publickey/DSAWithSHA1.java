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

import java.math.BigInteger;

import com.mindbright.jca.security.MessageDigest;
import com.mindbright.jca.security.NoSuchAlgorithmException;
import com.mindbright.jca.security.InvalidKeyException;
import com.mindbright.jca.security.interfaces.DSAParams;
import com.mindbright.jca.security.interfaces.DSAPublicKey;
import com.mindbright.jca.security.interfaces.DSAPrivateKey;

public final class DSAWithSHA1 extends BaseSignature {

    public DSAWithSHA1() {
	super("SHA1");
    }

    protected void initVerify() throws InvalidKeyException {
	if(publicKey == null || !(publicKey instanceof DSAPublicKey)) {
	    throw new InvalidKeyException("Wrong key for DSAWithSHA1 verify: " +
					  publicKey);
	}
    }

    protected void initSign() throws InvalidKeyException {
	if(privateKey == null || !(privateKey instanceof DSAPrivateKey)) {
	    throw new InvalidKeyException("Wrong key for DSAWithSHA1 sign: " +
					  privateKey);
	}
    }

    protected byte[] sign(byte[] data) {
	DSAPrivateKey key  = (DSAPrivateKey)privateKey;
	DSAParams     parm = key.getParams();
	BigInteger    x    = key.getX();
	BigInteger    p    = parm.getP();
	BigInteger    q    = parm.getQ();
	BigInteger    g    = parm.getG();
	return DSAAlgorithm.sign(x, p, q, g, data);
    }

    protected boolean verify(byte[] signature, byte[] data) {
	DSAPublicKey key  = (DSAPublicKey)publicKey;
	DSAParams    parm = key.getParams();
	BigInteger   y    = key.getY();
	BigInteger   p    = parm.getP();
	BigInteger   q    = parm.getQ();
	BigInteger   g    = parm.getG();
	return DSAAlgorithm.verify(y, p, q, g, signature, data);
    }

}
