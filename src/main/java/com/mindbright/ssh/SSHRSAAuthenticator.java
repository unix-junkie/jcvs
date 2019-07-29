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

import java.net.*;
import java.io.*;

import com.mindbright.jca.security.interfaces.RSAPublicKey;

public class SSHRSAAuthenticator implements SSHAuthenticator {
  protected String fileName;
  protected String username;
  protected String password;
  protected int    cipher;

  public SSHRSAAuthenticator(String username, String password, String fileName, String cipher) {
    this.username = username;
    this.password = password;
    this.cipher   = SSH.getCipherType(cipher);
    this.fileName = fileName;
  }

  public SSHRSAAuthenticator(String username, String password, String fileName) {
    this.username = username;
    this.password = password;
    this.cipher   = SSH.CIPHER_DEFAULT;
    this.fileName = fileName;
  }

  public String getUsername(SSHClientUser origin) {
    return username;
  }

  public String getPassword(SSHClientUser origin) {
    return "";
  }

  public String getChallengeResponse(SSHClientUser origin, String challenge) {
    return "";
  }

  public int[] getAuthTypes(SSHClientUser origin) {
    int[] types = new int[1];
    types[0] = SSH.AUTH_PUBLICKEY;
    return types;
  }

  public int getCipher(SSHClientUser origin) {
    return cipher;
  }

  public String getIdentityPassword(SSHClientUser origin) {
    return password;
  }

  public SSHRSAKeyFile getIdentityFile(SSHClientUser origin) throws IOException {
    return new SSHRSAKeyFile(fileName);
  }

  public boolean verifyKnownHosts(RSAPublicKey hostPub) {
    return true;
  }

}

