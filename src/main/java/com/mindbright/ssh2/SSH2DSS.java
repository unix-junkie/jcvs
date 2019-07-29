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
import com.mindbright.jca.security.spec.DSAPublicKeySpec;
import com.mindbright.jca.security.interfaces.DSAPublicKey;
import com.mindbright.jca.security.interfaces.DSAParams;

public final class SSH2DSS extends SSH2SimpleSignature {
    public final static String SSH2_KEY_FORMAT = "ssh-dss";

    public SSH2DSS() {
	super("SHA1withDSA", SSH2_KEY_FORMAT);
    }

    public byte[] encodePublicKey(PublicKey publicKey) throws SSH2Exception {
	SSH2DataBuffer buf = new SSH2DataBuffer(8192);

	if(!(publicKey instanceof DSAPublicKey)) {
	    throw new SSH2FatalException("SSH2DSS, invalid public key type: " +
					 publicKey);
	}

	DSAPublicKey dsaPubKey = (DSAPublicKey)publicKey;
	DSAParams    dsaParams = dsaPubKey.getParams();

	buf.writeString(SSH2_KEY_FORMAT);
	buf.writeBigInt(dsaParams.getP());
	buf.writeBigInt(dsaParams.getQ());
	buf.writeBigInt(dsaParams.getG());
	buf.writeBigInt(dsaPubKey.getY());

	return buf.readRestRaw();
    }

    public PublicKey decodePublicKey(byte[] pubKeyBlob) throws SSH2Exception {
	BigInteger p, q, g, y;
	SSH2DataBuffer buf = new SSH2DataBuffer(pubKeyBlob.length);

	buf.writeRaw(pubKeyBlob);

	String type = buf.readJavaString();
	if(!type.equals(SSH2_KEY_FORMAT)) {
	    throw new SSH2FatalException("SSH2DSS, keyblob type mismatch, got '"
					 + type + ", (execpted + '" +
					 SSH2_KEY_FORMAT + "')");
	}

	p = buf.readBigInt();
	q = buf.readBigInt();
	g = buf.readBigInt();
	y = buf.readBigInt();

	try {
	    KeyFactory       dsaKeyFact = KeyFactory.getInstance("DSA");
	    DSAPublicKeySpec dsaPubSpec = new DSAPublicKeySpec(y, p, q, g);

	    return dsaKeyFact.generatePublic(dsaPubSpec);

	} catch (Exception e) {
	    throw new SSH2FatalException("SSH2DSS, error decoding public key blob: " +
					 e);
	}
    }

}
