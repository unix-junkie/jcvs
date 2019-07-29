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

/**
 * This interface is a simple abstraction of a PKI signing mechanism. An
 * implementation of this interface can use certificates or plain public keys,
 * this is something which is defined by the ssh2 specific algorithm name used
 * to identify it.
 *
 * @see SSH2AuthPublicKey
 */
public interface SSH2PKISigner {
    public String getAlgorithmName();
    public byte[] getPublicKeyBlob() throws SSH2SignatureException;
    public byte[] sign(byte[] data) throws SSH2SignatureException;
    public void setIncompatibility(SSH2Transport transport);
}
