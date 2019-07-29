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

public class SSHServerSocket {
  //
  // !!! Can't extend ServerSocket since it's not intended to be used
  // in this way... (i.e. it's only for a "vm-global" switch of
  // socket-implementation)
  //
  // If we extend ServerSocket we waste a local port for each remote
  // port we listen to, this is perhaps not too bad, BUT since applets
  // can't listen to local sockets we are doomed if we want to use it
  // in one... There is not much gain in extension here, I can't think
  // of a real case where it would really matter.
  //
  public static final int DEFAULT_BACKLOG = 25;

  protected SSHSocketFactory factory;
  protected SSHSocketImpl    impl;

  protected SSHServerSocket(SSHSocketImpl impl) {
    this.impl = impl;
  }

  protected void finalize() throws IOException {
    impl.close();
  }

  protected void setSocketFactory(SSHSocketFactory factory) {
    this.factory = factory;
  }

  public InetAddress getInetAddress() {
    return impl.getInetAddress();
  }

  protected int getLocalPort() {
    return impl.getLocalPort();
  }

  public SSHSocket accept() throws IOException {
    SSHSocketImpl aImpl = factory.createSocketImpl(impl.client, false);
    SSHSocket     aSock;

    impl.accept(aImpl);
    aSock = new SSHSocket(aImpl);

    return aSock;
  }

  public void close() throws IOException {
    impl.close();
  }

  public synchronized void setSoTimeout(int timeout) throws SocketException {
    impl.setOption(SSHSocketImpl.SO_TIMEOUT, new Integer(timeout));
  }

  public synchronized int getSoTimeout() throws IOException {
    Object o = impl.getOption(SSHSocketImpl.SO_TIMEOUT);
    /* extra type safety */
    if (o instanceof Integer) {
      return ((Integer) o).intValue();
    } else {
      return 0;
    }
  }

  public String toString() {
    return "ServerSocket[addr=" + impl.getInetAddress() +
      ",port=" + impl.getPort() +
      ",localport=" + impl.getLocalPort()  + "]";
  }

}
