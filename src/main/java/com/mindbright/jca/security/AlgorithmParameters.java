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

import com.mindbright.jca.security.spec.AlgorithmParameterSpec;

// !!! TODO

public class AlgorithmParameters {
    protected AlgorithmParameters(AlgorithmParametersSpi paramSpi,
				  Provider provider,
				  String algorithm) {
    }

    public final String getAlgorithm() {
	return null;
    }

    public final byte[] getEncoded() {
	return null;
    }

    public final byte[] getEncoded(String format) {
	return null;
    }

    public final static AlgorithmParameters getInstance(String algorithm) {
	return null;
    }

    public final static AlgorithmParameters getInstance(String algorithm,
							String provider) {
	return null;
    }

    public final AlgorithmParameterSpec getParameterSpec(Class paramSpec) {
	return null;
    }

    public final Provider getProvider() {
	return null;
    }

    public final void init(AlgorithmParameterSpec paramSpec) {
    }

    public final void init(byte[] params) {
    }

    public final void init(byte[] params, String format) {
    }

    public final String toString() {
	return null;
    }
}
