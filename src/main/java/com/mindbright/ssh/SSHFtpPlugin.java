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

import java.util.Random;
import java.io.IOException;

public class SSHFtpPlugin extends SSHProtocolPlugin {

  public void initiate(SSHClient client) {
    client.delRemotePortForward("ftp");
    if(client.havePORTFtp) {
      Random rnd = new Random();
      int rndval;
      while((rndval = (rnd.nextInt() & 0xfff0)) < 8192)
	;
      client.firstFTPPort = rndval;
      for(int i = 0; i < SSHFtpTunnel.MAX_REMOTE_LISTEN; i++) {
	client.addRemotePortForward(client.firstFTPPort + i,
				    SSHFtpTunnel.TUNNEL_NAME + i,
				    client.firstFTPPort + i, "ftp");
      }
    }
  }

  public SSHListenChannel localListener(String localHost, int localPort,
					String remoteHost, int remotePort,
					SSHChannelController controller) throws IOException {
    return new SSHFtpListenChannel(localHost, localPort, remoteHost, remotePort, controller);
  }

}
