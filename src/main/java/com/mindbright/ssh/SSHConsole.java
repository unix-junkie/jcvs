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

import com.mindbright.terminal.Terminal;

public interface SSHConsole {
  public Terminal getTerminal();

  public void stdoutWriteString(byte[] str);
  public void stderrWriteString(byte[] str);

  public void print(String str);
  public void println(String str);

  public void serverConnect(SSHChannelController controller,
			    SSHCipher sndCipher);
  public void serverDisconnect(String reason);
}
