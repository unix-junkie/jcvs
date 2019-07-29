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

public abstract class SSH2SimpleSignature extends SSH2Signature {

    protected String signatureAlgorithm;
    protected String ssh2KeyFormat;

    protected boolean draftIncompatibleSignature;

    protected SSH2SimpleSignature(String signatureAlgorithm,
				  String ssh2KeyFormat)
    {
	super();
	this.signatureAlgorithm = signatureAlgorithm;
	this.ssh2KeyFormat      = ssh2KeyFormat;
    }

    public static SSH2Signature getVerifyInstance(byte[] pubKeyBlob)
	throws SSH2Exception
    {
	String keyFormat        = getKeyFormat(pubKeyBlob);
	SSH2Signature signature = SSH2Signature.getInstance(keyFormat);
	signature.initVerify(pubKeyBlob);
	return signature;
    }

    public final String getSignatureAlgorithm() {
	return signatureAlgorithm;
    }

    public byte[] encodeSignature(byte[] sigRaw) {
	if(draftIncompatibleSignature) {
	    return sigRaw;
	} else {
	    SSH2DataBuffer buf = new SSH2DataBuffer(sigRaw.length + 4 +
						    ssh2KeyFormat.length() + 4);
	    buf.writeString(ssh2KeyFormat);
	    buf.writeString(sigRaw);
	    return buf.readRestRaw();
	}
    }

    public byte[] decodeSignature(byte[] sigBlob)
	throws SSH2SignatureException
    {
	if(draftIncompatibleSignature) {
	    return sigBlob;
	} else {
	    SSH2DataBuffer buf = new SSH2DataBuffer(sigBlob.length);
	    buf.writeRaw(sigBlob);

	    int len = buf.readInt();
	    if(len <= 0 || len > sigBlob.length) {
		// This is probably an undetected buggy implemenation
		// !!! TODO: might want to report this...
		return sigBlob;
	    }

	    buf.setRPos(buf.getRPos() - 4); // undo above readInt

	    String type = buf.readJavaString();
	    if(!type.equals(ssh2KeyFormat)) {
		throw new SSH2SignatureException(ssh2KeyFormat +
						 ", signature blob type " +
						 "mismatch, got '" + type);
	    }

	    return buf.readString();
	}
    }

    public static String getKeyFormat(byte[] pubKeyBlob) {
	SSH2DataBuffer buf = new SSH2DataBuffer(pubKeyBlob.length);
	buf.writeRaw(pubKeyBlob);
	return buf.readJavaString();
    }

    public void setIncompatibility(SSH2Transport transport) {
	draftIncompatibleSignature = transport.incompatibleSignature;
    }

}

