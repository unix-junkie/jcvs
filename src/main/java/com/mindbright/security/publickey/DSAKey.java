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
import com.mindbright.jca.security.spec.DSAParameterSpec;
import com.mindbright.jca.security.interfaces.DSAParams;

public class DSAKey extends DSAParameterSpec
    implements com.mindbright.jca.security.interfaces.DSAKey, Key {

    protected DSAKey(BigInteger p, BigInteger q, BigInteger g) {
	super(p, q, g);
    }

    public String getAlgorithm() {
	return "DSA";
    }

    public byte[] getEncoded() {
	return null;
    }

    public String getFormat() {
	return null;
    }

    public DSAParams getParams() {
	return this;
    }

}
