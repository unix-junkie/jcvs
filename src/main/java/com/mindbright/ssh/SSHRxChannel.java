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

import java.io.*;

public class SSHRxChannel extends SSHChannel {

  protected InputStream  in;
  protected SSHPdu       pduFactory;

  public SSHRxChannel(InputStream in, int channelId) {
    super(channelId);
    this.in = in;
  }

  public void setSSHPduFactory(SSHPdu pduFactory) {
    this.pduFactory = pduFactory;
  }

  public void serviceLoop() throws Exception {
    SSH.logExtra("Starting rx-chan: " + channelId);
    for(;;) {
      SSHPdu pdu;
      pdu = pduFactory.createPdu();
      pdu = listener.prepare(pdu);
      //      pdu = pdu.preProcess(pdu);
      pdu.readFrom(in);
      //      pdu = pdu.postProcess();
      listener.receive(pdu);
    }
  }

  public void forceClose() {
    try {
      in.close();
    } catch (IOException e) {
      // !!!
    }
  }

}
