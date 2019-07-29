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

package javax.crypto.spec;

import com.mindbright.jca.security.spec.AlgorithmParameterSpec;

public class IvParameterSpec implements AlgorithmParameterSpec {
    byte[] iv;

    public IvParameterSpec(byte[] iv) {
	this(iv, 0, iv.length);
    }

    public IvParameterSpec(byte[] iv, int offset, int len) {
	this.iv = new byte[len];
	System.arraycopy(iv, offset, this.iv, 0, len);
    }

    public byte[] getIV() {
	byte[] ivc = new byte[iv.length];
	System.arraycopy(iv, 0, ivc, 0, iv.length);
	return ivc;
    }
}
