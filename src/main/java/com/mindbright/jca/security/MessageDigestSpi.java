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

public abstract class MessageDigestSpi {

    public MessageDigestSpi() {
    }

    protected int engineGetDigestLength() {
	return 0;
    }

    protected abstract byte[] engineDigest();

    protected int engineDigest(byte[] buf, int offset, int len)
	throws DigestException
    {
	byte[] digest = engineDigest();
	if(len < digest.length) {
	    throw new DigestException("MessageDigestSpi, buffer too short");
	}
	System.arraycopy(digest, 0, buf, offset, digest.length);
	return digest.length;
    }

    protected abstract void engineReset();

    protected abstract void engineUpdate(byte input);

    protected abstract void engineUpdate(byte[] input, int offset, int len);

    public Object clone() throws CloneNotSupportedException {
	if(this instanceof Cloneable) {
	    return super.clone();
	} else {
	    throw new CloneNotSupportedException();
	}
    }

}
