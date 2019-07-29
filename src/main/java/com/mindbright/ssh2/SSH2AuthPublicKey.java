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

/**
 * This class implements a module for publickey authentication as defined in the
 * userauth protocol spec. It uses the interface <code>SSH2PKISigner</code> to
 * access an abstract PKI signing mechanism (e.g. implemented with simple file
 * based public/private keys without certificates).
 *
 * @see SSH2AuthModule
 * @see SSH2PKISigner
 */
public class SSH2AuthPublicKey implements SSH2AuthModule {

    private SSH2PKISigner signer;
    private boolean       test;

    public SSH2AuthPublicKey(SSH2PKISigner signer) {
	this(signer, true);
    }

    public SSH2AuthPublicKey(SSH2PKISigner signer, boolean test) {
	this.signer = signer;
	this.test   = test;
    }

    protected SSH2PKISigner getSigner() {
	return signer;
    }

    public SSH2TransportPDU processMethodMessage(SSH2UserAuth userAuth,
						 SSH2TransportPDU pdu) {
	switch(pdu.getType()) {
	case SSH2.MSG_USERAUTH_PK_OK:
	    try {
		pdu = createRequest(userAuth, false);
	    } catch (SSH2SignatureException e) {
		// !!! TODO how do we want to handle this?
		pdu = null;
	    }
	    break;

	default:
	    userAuth.getTransport().getLog().
		warning("SSH2AuthPublicKey",
			"received unexpected packet of type: " + pdu.getType());
	    pdu = null;
	}
	return pdu;
    }

    public SSH2TransportPDU startAuthentication(SSH2UserAuth userAuth)
	throws SSH2SignatureException
    {
	return createRequest(userAuth, test);
    }

    private SSH2TransportPDU createRequest(SSH2UserAuth userAuth, boolean test)
	throws SSH2SignatureException
    {
	SSH2TransportPDU pdu     = userAuth.createUserAuthRequest("publickey");
	SSH2PKISigner    signer  = getSigner();
	byte[]           keyBlob = signer.getPublicKeyBlob();

	pdu.writeBoolean(!test);
	pdu.writeString(signer.getAlgorithmName());
	pdu.writeString(keyBlob);

	if(!test) {
	    signPDU(userAuth, pdu, signer, keyBlob);
	}

	return pdu;
    }

    private void signPDU(SSH2UserAuth userAuth, SSH2TransportPDU targetPDU,
			 SSH2PKISigner signer, byte[] keyBlob)
	throws SSH2SignatureException
    {
	SSH2TransportPDU sigPDU = targetPDU;

	if(userAuth.getTransport().incompatiblePublicKeyAuth) {
	    sigPDU =
		SSH2TransportPDU.createOutgoingPacket(SSH2.MSG_USERAUTH_REQUEST);
	    sigPDU.writeString(userAuth.user);
	    sigPDU.writeString("ssh-userauth");
	    sigPDU.writeString("publickey");
	    sigPDU.writeBoolean(true);
	    sigPDU.writeString(signer.getAlgorithmName());
	    sigPDU.writeString(keyBlob);
	}

	byte[] sessionId = userAuth.getTransport().getSessionId();

	int    payloadLength = sigPDU.wPos - sigPDU.getPayloadOffset();
	byte[] signData      = new byte[payloadLength + sessionId.length];

	System.arraycopy(sessionId, 0, signData, 0, sessionId.length);
	System.arraycopy(sigPDU.data, sigPDU.getPayloadOffset(),
			 signData, sessionId.length, payloadLength);

	signer.setIncompatibility(userAuth.getTransport());

	byte[] sig = signer.sign(signData);

	targetPDU.writeString(sig);
    }

}
