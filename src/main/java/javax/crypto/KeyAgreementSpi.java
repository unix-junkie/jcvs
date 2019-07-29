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

package javax.crypto;

import java.math.BigInteger;

import com.mindbright.jca.security.Provider;
import com.mindbright.jca.security.SecureRandom;
import com.mindbright.jca.security.Key;
import com.mindbright.jca.security.NoSuchAlgorithmException;
import com.mindbright.jca.security.InvalidKeyException;
import com.mindbright.jca.security.InvalidAlgorithmParameterException;
import com.mindbright.jca.security.spec.AlgorithmParameterSpec;

public abstract class KeyAgreementSpi {

    public KeyAgreementSpi() {
    }

    protected abstract void engineInit(Key key, SecureRandom random)
	throws InvalidKeyException;

    protected abstract void engineInit(Key key, AlgorithmParameterSpec params,
				       SecureRandom random)
	throws InvalidKeyException, InvalidAlgorithmParameterException;

    protected abstract Key engineDoPhase(Key key, boolean lastPhase)
	throws InvalidKeyException, IllegalStateException;

    protected abstract byte[] engineGenerateSecret()
	throws IllegalStateException;

    protected abstract int engineGenerateSecret(byte[] sharedSecret, int offset)
	throws IllegalStateException, ShortBufferException;

    protected abstract SecretKey engineGenerateSecret(String algorithm)
	throws IllegalStateException, NoSuchAlgorithmException,
	       InvalidKeyException;

}
