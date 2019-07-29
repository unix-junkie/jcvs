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

package com.mindbright.jca.security;

import com.mindbright.jca.security.spec.AlgorithmParameterSpec;

public abstract class SignatureSpi {

    protected SecureRandom appRandom;

    public SignatureSpi() {
    }

    protected abstract void engineInitVerify(PublicKey publicKey)
	throws InvalidKeyException;

    protected abstract void engineInitSign(PrivateKey privateKey)
	throws InvalidKeyException;

    protected abstract void engineInitSign(PrivateKey privateKey,
				  SecureRandom random)
	throws InvalidKeyException;

    protected abstract void engineUpdate(byte b)
	throws SignatureException;

    protected abstract void engineUpdate(byte[] b,
					 int off,
					 int len)
	throws SignatureException;

    protected abstract byte[] engineSign()
	throws SignatureException;

    protected int engineSign(byte[] outbuf, int offset, int len)
	throws SignatureException
    {
	byte[] signature = engineSign();
	if(signature.length < len) {
	    len = signature.length;
	}
	System.arraycopy(signature, 0, outbuf, offset, len);
	return len;
    }

    protected abstract boolean engineVerify(byte[] sigBytes)
	throws SignatureException;

    protected abstract void engineSetParameter(String param,
					       Object value)
	throws InvalidParameterException;

    protected void engineSetParameter(AlgorithmParameterSpec params)
	throws InvalidAlgorithmParameterException
    {
	throw new InvalidAlgorithmParameterException("Not supported: " +
						     params);
    }

    protected abstract Object engineGetParameter(String param)
	throws InvalidParameterException;

    public Object clone() throws CloneNotSupportedException {
	// !!! REMOVE TODO
	throw new CloneNotSupportedException();
    }

}
