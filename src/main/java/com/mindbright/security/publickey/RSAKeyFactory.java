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

import com.mindbright.jca.security.Key;
import com.mindbright.jca.security.PublicKey;
import com.mindbright.jca.security.PrivateKey;
import com.mindbright.jca.security.KeyFactorySpi;
import com.mindbright.jca.security.InvalidKeyException;
import com.mindbright.jca.security.spec.KeySpec;
import com.mindbright.jca.security.spec.RSAPublicKeySpec;
import com.mindbright.jca.security.spec.RSAPrivateKeySpec;
import com.mindbright.jca.security.spec.RSAPrivateCrtKeySpec;
import com.mindbright.jca.security.spec.InvalidKeySpecException;

public class RSAKeyFactory extends KeyFactorySpi {

    protected PublicKey engineGeneratePublic(KeySpec keySpec)
	throws InvalidKeySpecException
    {
	if(!(keySpec instanceof RSAPublicKeySpec)) {
	    throw new InvalidKeySpecException("KeySpec " + keySpec +
					      ", not supported");
	}
	RSAPublicKeySpec rsaPub = (RSAPublicKeySpec)keySpec;
	return new RSAPublicKey(rsaPub.getModulus(),
				rsaPub.getPublicExponent());
    }

    protected PrivateKey engineGeneratePrivate(KeySpec keySpec)
	throws InvalidKeySpecException
    {
	if(!(keySpec instanceof RSAPrivateKeySpec)) {
	    throw new InvalidKeySpecException("KeySpec " + keySpec +
					      ", not supported");
	}

	if(keySpec instanceof RSAPrivateCrtKeySpec) {
	    RSAPrivateCrtKeySpec rsaPrv = (RSAPrivateCrtKeySpec)keySpec;
	    return new RSAPrivateCrtKey(rsaPrv.getModulus(),
					rsaPrv.getPublicExponent(),
					rsaPrv.getPrivateExponent(),
					rsaPrv.getPrimeP(),
					rsaPrv.getPrimeQ(),
					rsaPrv.getPrimeExponentP(),
					rsaPrv.getPrimeExponentQ(),
					rsaPrv.getCrtCoefficient());
	} else {
	    RSAPrivateKeySpec rsaPrv = (RSAPrivateKeySpec)keySpec;
	    return new RSAPrivateKey(rsaPrv.getModulus(),
				     rsaPrv.getPrivateExponent());
	}
    }

    protected KeySpec engineGetKeySpec(Key key, Class keySpec)
	throws InvalidKeySpecException {
	// !!! TODO
	return null;
    }

    protected Key engineTranslateKey(Key key)
	throws InvalidKeyException {
	// !!! TODO
	return null;
    }

}
