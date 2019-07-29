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

package com.mindbright.jca.security.spec;

import java.math.BigInteger;

public class RSAPrivateCrtKeySpec extends RSAPrivateKeySpec {

    protected BigInteger publicExponent;
    protected BigInteger primeP;
    protected BigInteger primeQ;
    protected BigInteger primeExponentP;
    protected BigInteger primeExponentQ;
    protected BigInteger crtCoefficient;

    public RSAPrivateCrtKeySpec(BigInteger modulus,
				BigInteger publicExponent,
				BigInteger privateExponent,
				BigInteger primeP, BigInteger primeQ,
				BigInteger primeExponentP,
				BigInteger primeExponentQ,
				BigInteger crtCoefficient)
    {
	super(modulus, privateExponent);
	this.publicExponent = publicExponent;
	this.primeP         = primeP;
	this.primeQ         = primeQ;
	this.primeExponentP = primeExponentP;
	this.primeExponentQ = primeExponentQ;
	this.crtCoefficient = crtCoefficient;
    }

    public BigInteger getPublicExponent() {
	return publicExponent;
    }

    public BigInteger getPrimeP() {
	return primeP;
    }

    public BigInteger getPrimeQ() {
	return primeQ;
    }

    public BigInteger getPrimeExponentP() {
	return primeExponentP;
    }

    public BigInteger getPrimeExponentQ() {
	return primeExponentQ;
    }

    public BigInteger getCrtCoefficient() {
	return crtCoefficient;
    }

}
