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

import com.mindbright.jca.security.MessageDigest;

public abstract class SSH2KeyExchanger {

    private static Hashtable algorithms;

    static {
	algorithms = new Hashtable();

	algorithms.put("diffie-hellman-group1-sha1",
		       SSH2KEXDHGroup1SHA1.class);
	algorithms.put("diffie-hellman-group-exchange-sha1",
		       SSH2KEXDHGroupXSHA1.class);
    }

    protected SSH2KeyExchanger() {
    }

    public static SSH2KeyExchanger getInstance(String algorithm)
	throws SSH2KEXFailedException
    {
	Class            alg = (Class)algorithms.get(algorithm);
	SSH2KeyExchanger kex = null;
	if(alg != null) {
	    try {
		kex = (SSH2KeyExchanger)alg.newInstance();
	    } catch (Throwable t) {
		kex = null;
	    }
	}
	if(kex == null) {
	    throw new SSH2KEXFailedException("Unknown kex algorithm: " +
					     algorithm);
	}
	return kex;
    }

    public abstract void init(SSH2Transport transport) throws SSH2Exception;

    public abstract void processKEXMethodPDU(SSH2TransportPDU pdu)
	throws SSH2Exception;

    public abstract MessageDigest getExchangeHashAlgorithm();

    public abstract byte[] getSharedSecret_K();

    public abstract byte[] getExchangeHash_H();

    public abstract String getHostKeyAlgorithms();

}
