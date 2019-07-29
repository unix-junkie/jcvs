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

package com.mindbright.ssh;

import com.mindbright.security.cipher.DES;

public final class SSHDES3 extends SSHCipher {
    SSHDES des1 = new SSHDES();
    SSHDES des2 = new SSHDES();
    SSHDES des3 = new SSHDES();

    public synchronized void encrypt(byte[] src, int srcOff, byte[] dest, int destOff, int len) {
	des1.encrypt(src, srcOff, dest, destOff, len);
	des2.decrypt(dest, destOff, dest, destOff, len);
	des3.encrypt(dest, destOff, dest, destOff, len);
    }

    public synchronized void decrypt(byte[] src, int srcOff, byte[] dest, int destOff, int len) {
	des3.decrypt(src, srcOff, dest, destOff, len);
	des2.encrypt(dest, destOff, dest, destOff, len);
	des1.decrypt(dest, destOff, dest, destOff, len);
    }

    public void setKey(byte[] key) {
	byte[] subKey = new byte[8];
	des1.setKey(key);
	System.arraycopy(key, 8, subKey, 0, 8);
	des2.setKey(subKey);
	System.arraycopy(key, 16, subKey, 0, 8);
	des3.setKey(subKey);
    }

}
