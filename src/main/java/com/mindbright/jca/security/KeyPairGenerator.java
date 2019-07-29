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

import com.mindbright.jca.security.Provider;
import com.mindbright.jca.security.ProviderLookup;
import com.mindbright.jca.security.SecureRandom;
import com.mindbright.jca.security.KeyPair;
import com.mindbright.jca.security.NoSuchAlgorithmException;
import com.mindbright.jca.security.NoSuchProviderException;
import com.mindbright.jca.security.InvalidAlgorithmParameterException;
import com.mindbright.jca.security.spec.AlgorithmParameterSpec;

public class KeyPairGenerator extends KeyPairGeneratorSpi {

    KeyPairGeneratorSpi keyPairGenSpi;
    Provider            provider;
    String              algorithm;

    protected KeyPairGenerator(String algorithm) {
	this.algorithm = algorithm;
    }

    public String getAlgorithm() {
	return algorithm;
    }

    public static KeyPairGenerator getInstance(String algorithm)
	throws NoSuchAlgorithmException
    {
	try {
	    String provider =
		ProviderLookup.findImplementingProvider("KeyPairGenerator",
							algorithm);
	    return getInstance(algorithm, provider);
	} catch(NoSuchProviderException e) {
	    throw new NoSuchAlgorithmException(algorithm + " not found");
	}
    }

    public static KeyPairGenerator getInstance(String algorithm,
					       String provider)
	throws NoSuchAlgorithmException, NoSuchProviderException
    {
	ProviderLookup pl = ProviderLookup.getImplementation("KeyPairGenerator",
							     algorithm,
							     provider);

	KeyPairGeneratorSpi keyPairGenSpi = (KeyPairGeneratorSpi)pl.getImpl();
	KeyPairGenerator    keyPairGen    = new KeyPairGenerator(algorithm);

	keyPairGen.provider      = pl.getProvider();
	keyPairGen.keyPairGenSpi = keyPairGenSpi;

	return keyPairGen;
    }

    public final Provider getProvider() {
	return provider;
    }

    public void initialize(int keysize) {
	initialize(keysize, null);
    }

    public void initialize(int keysize, SecureRandom random) {
	keyPairGenSpi.initialize(keysize, random);
    }

    public void initialize(AlgorithmParameterSpec params)
	throws InvalidAlgorithmParameterException
    {
	initialize(params, null);
    }

    public void initialize(AlgorithmParameterSpec params, SecureRandom random)
	throws InvalidAlgorithmParameterException
    {
	keyPairGenSpi.initialize(params, random);
    }

    public final KeyPair genKeyPair() {
	return generateKeyPair();
    }

    public KeyPair generateKeyPair() {
	return keyPairGenSpi.generateKeyPair();
    }

}
