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

import java.math.BigInteger;

import com.mindbright.jca.security.KeyFactory;
import com.mindbright.jca.security.PublicKey;
import com.mindbright.jca.security.spec.RSAPublicKeySpec;
import com.mindbright.jca.security.interfaces.RSAPublicKey;

public final class SSH2RSA extends SSH2SimpleSignature {
    public final static String SSH2_KEY_FORMAT = "ssh-rsa";

    public SSH2RSA() {
	super("SHA1withRSA", SSH2_KEY_FORMAT);
    }

    public byte[] encodePublicKey(PublicKey publicKey) throws SSH2Exception {
	SSH2DataBuffer buf = new SSH2DataBuffer(8192);

	if(!(publicKey instanceof RSAPublicKey)) {
	    throw new SSH2FatalException("SSH2RSA, invalid public key type: " +
					 publicKey);
	}

	RSAPublicKey rsaPubKey = (RSAPublicKey)publicKey;

	buf.writeString(SSH2_KEY_FORMAT);
	buf.writeBigInt(rsaPubKey.getPublicExponent());
	buf.writeBigInt(rsaPubKey.getModulus());

	return buf.readRestRaw();
    }

    public PublicKey decodePublicKey(byte[] pubKeyBlob) throws SSH2Exception {
	BigInteger e, n;
	SSH2DataBuffer buf = new SSH2DataBuffer(pubKeyBlob.length);

	buf.writeRaw(pubKeyBlob);

	String type = buf.readJavaString();
	if(!type.equals(SSH2_KEY_FORMAT)) {
	    throw new SSH2FatalException("SSH2RSA, keyblob type mismatch, got '"
					 + type + ", (execpted + '" +
					 SSH2_KEY_FORMAT + "')");
	}

	e = buf.readBigInt();
	n = buf.readBigInt();

	try {
	    KeyFactory       rsaKeyFact = KeyFactory.getInstance("RSA");
	    RSAPublicKeySpec rsaPubSpec = new RSAPublicKeySpec(n, e);

	    return rsaKeyFact.generatePublic(rsaPubSpec);

	} catch (Exception ee) {
	    throw new SSH2FatalException("SSH2RSA, error decoding public key blob: " +
					 ee);
	}
    }

}
