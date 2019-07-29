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

import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;

public class SSH2KEXDHGroupXSHA1 extends SSH2KEXDHGroup1SHA1 {

    private int        reqBits;
    private BigInteger p;
    private BigInteger g;

    public void init(SSH2Transport transport) throws SSH2Exception {
	this.transport = transport;
	this.sha1      = createHash();
	if(!transport.isServer()) {
	    sendGEXRequest();
	}
    }

    public void processKEXMethodPDU(SSH2TransportPDU pdu) throws SSH2Exception {
	switch(pdu.getType()) {
	case SSH2.MSG_KEXDH_GEX_REQUEST:
	    // !!! TODO
	    break;

	case SSH2.MSG_KEXDH_GEX_GROUP:
	    if(transport.isServer()) {
		throw new SSH2KEXFailedException("Unexpected KEXDH_GEX_GROUP");
	    }
	    p = pdu.readBigInt();
	    g = pdu.readBigInt();

	    DHParameterSpec dhParams = new DHParameterSpec(p, g);
	    generateDHKeyPair(dhParams);

	    sendDHINIT(SSH2.MSG_KEXDH_GEX_INIT);
	    break;

	case SSH2.MSG_KEXDH_GEX_INIT:
	    // !!! TODO
	    break;

	case SSH2.MSG_KEXDH_GEX_REPLY:
	    if(transport.isServer()) {
		throw new SSH2KEXFailedException("Unexpected KEXDH_GEX_REPLY");
	    }
	    serverHostKey      = pdu.readString();
	    serverF            = pdu.readBigInt();
	    byte[] serverSigH  = pdu.readString();

	    DHPublicKeySpec srvPubSpec = new DHPublicKeySpec(serverF, p, g);

	    computeSharedSecret_K(srvPubSpec);
	    computeExchangeHash_H();

	    transport.authenticateHost(serverHostKey, serverSigH,
				       exchangeHash_H);

	    transport.sendNewKeys();
	    break;
	}
    }

    protected void computeExchangeHash_H() {
	SSH2DataBuffer buf = new SSH2DataBuffer(8192);

	if(transport.isServer()) {
	    serverF = dhPublicKey.getY();
	} else {
	    clientE = dhPublicKey.getY();
	}

	buf.writeString(transport.getClientVersion());
	buf.writeString(transport.getServerVersion());
	buf.writeString(transport.getClientKEXINITPDU().getData(),
			transport.getClientKEXINITPDU().getPayloadOffset(),
			transport.getClientKEXINITPDU().getPayloadLength());
	buf.writeString(transport.getServerKEXINITPDU().getData(),
			transport.getServerKEXINITPDU().getPayloadOffset(),
			transport.getServerKEXINITPDU().getPayloadLength());
	buf.writeString(serverHostKey);
	buf.writeInt(reqBits);
	buf.writeBigInt(p);
	buf.writeBigInt(g);
	buf.writeBigInt(clientE);
	buf.writeBigInt(serverF);
	buf.writeString(sharedSecret_K);

	sha1.reset();
	sha1.update(buf.getData(), 0, buf.getWPos());
	exchangeHash_H = sha1.digest();

	transport.getLog().debug2("SSH2KEXDHGroup1SHA1",
				  "computeExchangeHash_H", "E: ",
				  clientE.toByteArray());
	transport.getLog().debug2("SSH2KEXDHGroup1SHA1",
				  "computeExchangeHash_H", "F: ",
				  serverF.toByteArray());
	transport.getLog().debug2("SSH2KEXDHGroup1SHA1",
				  "computeExchangeHash_H", "K: ",
				  sharedSecret_K);
	transport.getLog().debug2("SSH2KEXDHGroup1SHA1",
				  "computeExchangeHash_H", "Hash over: ",
				  buf.getData(), 0, buf.getWPos());
	transport.getLog().debug2("SSH2KEXDHGroup1SHA1",
				  "computeExchangeHash_H", "H: ",
				  exchangeHash_H);
    }

    protected void sendGEXRequest() throws SSH2Exception {
	SSH2TransportPDU pdu =
	    SSH2TransportPDU.createOutgoingPacket(SSH2.MSG_KEXDH_GEX_REQUEST);

	reqBits = estimateGroupBits();
	pdu.writeInt(reqBits);

	transport.transmitInternal(pdu);
    }

    /*
     * Estimates the group order for a Diffie-Hellman group that has an attack
     * complexity approximately the same as O(2**bits) where bits is the longest
     * of transmitter and receiver cipher keys.
     *
     * Estimate with: O(exp(1.9223 * (ln q)^(1/3) (ln ln q)^(2/3)))
     *
     * (This code snippet is from OpenSSH2)
     */
    private int estimateGroupBits() {
	SSH2TransportPreferences pref = transport.getOurPreferences();
	int rKLen = pref.getCipherKeyLen(pref.getReceiverCipher());
	int tKLen = pref.getCipherKeyLen(pref.getTransmitterCipher());
	int bits  = (rKLen > tKLen ? rKLen : tKLen) * 8;

	if (bits < 64)
	    return (512);	/* O(2**63) */
	if (bits < 128)
	    return (1024);	/* O(2**86) */
	if (bits < 192)
	    return (2048);	/* O(2**116) */
	return (4096);		/* O(2**156) */
    }

}
