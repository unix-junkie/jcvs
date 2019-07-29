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

package com.mindbright.ssh2;

import java.util.Hashtable;

import com.mindbright.jca.security.Signature;
import com.mindbright.jca.security.PublicKey;
import com.mindbright.jca.security.PrivateKey;
import com.mindbright.jca.security.InvalidKeyException;
import com.mindbright.jca.security.SignatureException;

public abstract class SSH2Signature implements SSH2PKISigner {

    private static Hashtable algorithms;

    static {
	algorithms = new Hashtable();
	algorithms.put("ssh-dss", "com.mindbright.ssh2.SSH2DSS");
	algorithms.put("ssh-rsa", "com.mindbright.ssh2.SSH2RSA");
    };

    protected String     algorithm;
    protected Signature  signature;
    protected PrivateKey privateKey;
    protected PublicKey  publicKey;
    protected byte[]     pubKeyBlob;

    public static SSH2Signature getInstance(String algorithm)
	throws SSH2Exception
    {
	SSH2Signature impl = getEncodingInstance(algorithm);
	impl.init(algorithm);
	return impl;
    }

    public static SSH2Signature getEncodingInstance(String algorithm)
	throws SSH2Exception
    {
	SSH2Signature impl      = null;
	String        className = (String)algorithms.get(algorithm);
	try {
	    impl = (SSH2Signature)Class.forName(className).newInstance();
	} catch (Exception e) {
	    // !!! TODO
	    throw new SSH2FatalException("Public key algorithm '" + algorithm +
					 "' not supported");
	}
	return impl;
    }

    private void init(String algorithm) throws SSH2Exception {
	this.algorithm = algorithm;
	String sigAlg  = getSignatureAlgorithm();
	try {
	    signature = Signature.getInstance(sigAlg);
	} catch (Exception e) {
	    // !!! TODO
	    throw new SSH2FatalException("Error initializing SSH2Signature: " +
					 algorithm + "/" + sigAlg + " - " + e);
	}
    }

    protected SSH2Signature() {
    }

    public final String getAlgorithmName() {
	return algorithm;
    }

    public final byte[] getPublicKeyBlob() throws SSH2SignatureException {
	if(pubKeyBlob == null) {
	    try {
		pubKeyBlob = encodePublicKey(publicKey);
	    } catch (SSH2Exception e) {
		throw new SSH2SignatureException(e.getMessage());
	    }
	}
	return pubKeyBlob;
    }

    public final PublicKey getPublicKey() throws SSH2SignatureException {
	if(publicKey == null) {
	    try {
		publicKey = decodePublicKey(pubKeyBlob);
	    } catch (SSH2Exception e) {
		throw new SSH2SignatureException(e.getMessage());
	    }
	}
	return publicKey;
    }

    public final void setPublicKey(PublicKey publicKey) {
	this.publicKey = publicKey;
    }

    public void setIncompatibility(SSH2Transport transport) {
	// Do nothing here, derived class might be interested...
    }

    public final void initSign(PrivateKey privateKey) throws SSH2Exception {
	this.privateKey = privateKey;
	try {
	    signature.initSign(privateKey);
	} catch (InvalidKeyException e) {
	    throw new SSH2FatalException("SSH2Signature.initSign, invalid key: "
					 + e.getMessage());
	}
    }

    public final void initVerify(PublicKey publicKey) throws SSH2Exception {
	initVerify(encodePublicKey(publicKey));
    }

    public final void initVerify(byte[] pubKeyBlob) throws SSH2Exception {
	this.pubKeyBlob = pubKeyBlob;
	this.publicKey  = decodePublicKey(pubKeyBlob);
	try {
	    signature.initVerify(publicKey);
	} catch (InvalidKeyException e) {
	    throw new SSH2FatalException("SSH2Signature.initSign, invalid key: "
					 + e.getMessage());
	}
    }

    public final byte[] sign(byte[] data) throws SSH2SignatureException {
	try {
	    signature.update(data);
	    byte[] sigRaw = signature.sign();
	    return encodeSignature(sigRaw);
	} catch (SignatureException e) {
	    throw new SSH2SignatureException("Error in " + algorithm +
					     " sign: " + e.getMessage());
	}
    }

    public final boolean verify(byte[] sigBlob, byte[] data)
	throws SSH2SignatureException
    {
	try {
	    byte[] sigRaw = decodeSignature(sigBlob);
	    signature.update(data);
	    return signature.verify(sigRaw);
	} catch (SignatureException e) {
	    throw new SSH2SignatureException("Error in " + algorithm +
					     " verify: " + e.getMessage());
	}
    }

    protected abstract String getSignatureAlgorithm();

    protected abstract byte[] encodePublicKey(PublicKey publicKey)
	throws SSH2Exception;

    protected abstract PublicKey decodePublicKey(byte[] pubKeyBlob)
	throws SSH2Exception;

    protected abstract byte[] encodeSignature(byte[] sigRaw);

    protected abstract byte[] decodeSignature(byte[] sigBlob)
	throws SSH2SignatureException;


}
