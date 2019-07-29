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

public interface SSHClientUser {
  public String  getSrvHost() throws IOException;
  public int     getSrvPort();
  public Socket  getProxyConnection() throws IOException;
  public String  getDisplay();
  public int     getMaxPacketSz();
  public int     getAliveInterval();
  public int     getCompressionLevel();

  public boolean wantX11Forward();
  public boolean wantPrivileged();
  public boolean wantPTY();

  public SSHInteractor getInteractor();
}
