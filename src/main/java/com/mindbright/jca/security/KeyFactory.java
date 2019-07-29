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

import com.mindbright.jca.security.spec.KeySpec;
import com.mindbright.jca.security.spec.InvalidKeySpecException;

public class KeyFactory {

    private KeyFactorySpi keyFactorySpi;
    private Provider      provider;
    private String        algorithm;

    protected KeyFactory(KeyFactorySpi keyFactorySpi, Provider provider,
			 String algorithm) {
	this.keyFactorySpi = keyFactorySpi;
	this.provider      = provider;
	this.algorithm     = algorithm;
    }

    public static KeyFactory getInstance(String algorithm)
	throws NoSuchAlgorithmException {
	try {
	    String provider =
		ProviderLookup.findImplementingProvider("KeyFactory",
							algorithm);
	    return getInstance(algorithm, provider);
	} catch(NoSuchProviderException e) {
	    throw new NoSuchAlgorithmException(algorithm + " not found");
	}
    }

    public static KeyFactory getInstance(String algorithm, String provider)
	throws NoSuchAlgorithmException, NoSuchProviderException
    {
	ProviderLookup pl = ProviderLookup.getImplementation("KeyFactory",
							     algorithm,
							     provider);
	return new KeyFactory((KeyFactorySpi)pl.getImpl(),
			      pl.getProvider(), algorithm);
    }

    public final Provider getProvider() {
	return provider;
    }

    public final String getAlgorithm() {
	return algorithm;
    }

    public final PublicKey generatePublic(KeySpec keySpec)
        throws InvalidKeySpecException
    {
	return keyFactorySpi.engineGeneratePublic(keySpec);
    }

    public final PrivateKey generatePrivate(KeySpec keySpec)
        throws InvalidKeySpecException
    {
	return keyFactorySpi.engineGeneratePrivate(keySpec);
    }

    public final KeySpec getKeySpec(Key key, Class keySpec)
	throws InvalidKeySpecException
    {
	return keyFactorySpi.engineGetKeySpec(key, keySpec);
    }

    public final Key translateKey(Key key) throws InvalidKeyException {
	return keyFactorySpi.engineTranslateKey(key);
    }

}
