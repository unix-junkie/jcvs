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

package com.mindbright.security.cipher;

import com.mindbright.jca.security.SecureRandom;
import com.mindbright.jca.security.Key;
import com.mindbright.jca.security.InvalidKeyException;
import com.mindbright.jca.security.spec.AlgorithmParameterSpec;

import javax.crypto.CipherSpi;
import javax.crypto.spec.SecretKeySpec;

public final class ArcFour extends CipherSpi {
    int    x;
    int    y;
    byte[] state = new byte[256];

    int arcfour_byte() {
	int x;
	int y;
	int sx, sy;
	x = (this.x + 1) & 0xff;
	sx = (int)state[x];
	y = (sx + this.y) & 0xff;
	sy = (int)state[y];
	this.x = x;
	this.y = y;
	state[y] = (byte)(sx & 0xff);
	state[x] = (byte)(sy & 0xff);
	return (int)state[((sx + sy) & 0xff)];
    }

    protected int engineDoFinal(byte[] input, int inputOffset, int inputLen,
				byte[] output, int outputOffset) {
	int end = inputOffset + inputLen;
	for(int si = inputOffset, di = outputOffset; si < end; si++, di++)
	    output[di] = (byte)(((int)input[si] ^ arcfour_byte()) & 0xff);
	return inputLen;
    }

    public void initializeKey(byte[] key) {
	int t, u;
	int keyindex;
	int stateindex;
	int counter;
    
	for(counter = 0; counter < 256; counter++)
	    state[counter] = (byte)counter;
	keyindex = 0;
	stateindex = 0;
	for(counter = 0; counter < 256; counter++) {
	    t = (int)state[counter];
	    stateindex = (stateindex + key[keyindex] + t) & 0xff;
	    u = (int)state[stateindex];
	    state[stateindex] = (byte)(t & 0xff);
	    state[counter] = (byte)(u & 0xff);
	    if(++keyindex >= key.length)
		keyindex = 0;
	}
    }

    protected int engineGetBlockSize() {
	return 1;
    }

    protected byte[] engineGetIV() {
	return null;
    }

    protected int engineGetOutputSize(int inputLen) {
	return inputLen;
    }

    protected void engineInit(int opmode, Key key,
			      AlgorithmParameterSpec params,
			      SecureRandom random)
	throws InvalidKeyException
    {
	initializeKey(((SecretKeySpec)key).getEncoded());
    }

    protected void engineInit(int opmode, Key key,
			      SecureRandom random) throws InvalidKeyException {
	engineInit(opmode, key, (AlgorithmParameterSpec)null, random);
    }

    protected void engineSetMode(String mode) {
    }

    protected void engineSetPadding(String padding) {
    }

}
