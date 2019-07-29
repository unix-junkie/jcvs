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

package com.mindbright.util;

import com.mindbright.jca.security.SecureRandom;
import com.mindbright.jca.security.MessageDigest;

public class SecureRandomAndPad extends SecureRandom {

    private SecureRandom random;
    private byte[]       state;
    private int          x;
    private int          y;

    private int arcfour_byte() {
	int x, y, sx, sy;
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

    public SecureRandomAndPad() {
	this(new SecureRandom());
    }

    public SecureRandomAndPad(SecureRandom random) {
	this.random = random;
	this.state  = new byte[256];
	for(int i = 0; i < 256; i++) {
	    this.state[i] = (byte)i;
	}
    }

    public void setPadSeed(byte[] seed) {
	int seedindex  = 0;
	int stateindex = 0;
	int t, u;
	for(int counter = 0; counter < 256; counter++) {
	    t = (int)state[counter];
	    stateindex = (stateindex + seed[seedindex] + t) & 0xff;
	    u = (int)state[stateindex];
	    state[stateindex] = (byte)(t & 0xff);
	    state[counter] = (byte)(u & 0xff);
	    if(++seedindex >= seed.length)
		seedindex = 0;
	}
    }

    public void nextPadBytes(byte[] bytes, int off, int len) {
	int end = off + len;
	for(int i = off; i < end; i++) {
	    bytes[i] = (byte)(((int)bytes[i] ^ arcfour_byte()) & 0xff);
	}
    }

    public byte[] generateSeed(int numBytes) {
	return random.generateSeed(numBytes);
    }

    public void nextBytes(byte[] bytes) {
	random.nextBytes(bytes);
    }

    public void setSeed(byte[] seed) {
	random.setSeed(seed);
    }

}
