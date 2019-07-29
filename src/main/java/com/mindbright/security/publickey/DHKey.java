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

import javax.crypto.spec.DHParameterSpec;

public class DHKey extends DHParameterSpec
    implements javax.crypto.interfaces.DHKey, Key
{

    protected DHKey(BigInteger p, BigInteger g) {
	super(p, g);
    }

    public String getAlgorithm() {
	return "DiffieHellman";
    }

    public String getFormat() {
	return null;
    }

    public byte[] getEncoded() {
	return null;
    }

    public DHParameterSpec getParams() {
	return this;
    }

}
