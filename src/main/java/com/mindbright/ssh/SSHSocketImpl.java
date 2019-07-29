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

import com.mindbright.util.Queue;

public class SSHSocketImpl extends SocketImpl {

  public final static int SO_TIMEOUT   = 0x1006; // !!!

  protected SSHSocketFactory factory;

  SSHClient       client;
  SSHSocketTunnel tunnel;
  Queue           cnQueue;

  boolean isClosed;

  protected SSHSocketImpl() {
    // !!! Used to indicate to create if we have done bind() or not...
    // (i.e. if we are an implementation of a SSHServerSocket or a SSHSocket)
    localport = SSHSocketFactory.NOLISTENPORT;
  }

  protected void setFactory(SSHSocketFactory factory) {
    this.factory = factory;
  }

  protected void create(boolean stream) {
    // !!! Not used, but abstract in SocketImpl
  }

  protected void create(SSHClient client, boolean acceptor) throws IOException {
    InetAddress localHost = InetAddress.getLocalHost();
    
    this.client = client;
    client.addRef();

    if(acceptor) {
      cnQueue = client.controller.getCnQueue();
    } else {
      tunnel = new SSHSocketTunnel(client.controller, this);
      tunnel.setLocalAddress(localHost);
    }
  }

  protected void connect(String host, int port) throws IOException {
    try {
      address = InetAddress.getByName(host);
    } catch (Exception e) {
      address = InetAddress.getLocalHost();
    }

    tunnel.connect(host, port);
  }

  protected void connect(InetAddress address, int port) throws IOException {
    connect(address.getHostAddress(), port);
  }

  protected void bind(InetAddress host, int port) throws IOException {
    localport = port;
    // !!! This is done elsewhere (also, the local address is not important...)
    // !!! tunnel.setLocalAddress(host);
  }

  protected void listen(int backlog) throws IOException {
    cnQueue.setMaxDepth(backlog);
  }

  protected void accept(SocketImpl s) throws IOException {
    SSHPduOutputStream respPdu;
    SSHPduInputStream  inPdu;
    SSHSocketImpl      aImpl = (SSHSocketImpl)s;
    int                remoteChannel;
    inPdu = (SSHPduInputStream) cnQueue.getFirst();
    if(inPdu == null)
      throw new IOException("Socket closed");
    remoteChannel = inPdu.readInt();
    aImpl.tunnel.setRemoteChannelId(remoteChannel);
    respPdu = new SSHPduOutputStream(SSH.MSG_CHANNEL_OPEN_CONFIRMATION,
				     client.controller.sndCipher,
				     client.controller.sndComp,
				     client.rand);
    respPdu.writeInt(remoteChannel);
    respPdu.writeInt(aImpl.tunnel.channelId);
    client.controller.transmit(respPdu);
    client.controller.addTunnel(aImpl.tunnel);

    /* !!!
    } catch (Exception e) {
      respPdu = new SSHPduOutputStream(SSH.MSG_CHANNEL_OPEN_FAILURE,
      client.controller.sndCipher, client.rand);
      respPdu.writeInt(remoteChannel);
      client.controller.transmit(respPdu);
      throw new IOException(e.getMessage());
    }
    */
  }

  protected InputStream getInputStream() throws IOException {
    return tunnel.in;
  }

  protected OutputStream getOutputStream() throws IOException {
    return tunnel.out;
  }

  protected int available() throws IOException {
    return tunnel.available();
  }

  protected void close() throws IOException {
    if(localport == SSHSocketFactory.NOLISTENPORT) {
      if(tunnel != null && !tunnel.terminated())
	tunnel.close();
    } else {
      factory.closePseudoUser(client, this);
    }
  }

  protected void finalize() throws IOException {
    if(!isClosed)
      close();
  }

  public void setOption(int optID, Object value) throws SocketException {
    throw new SocketException("Not implemented");
  }

  public Object getOption(int optID) throws SocketException {
    throw new SocketException("Not implemented");
  }

  protected InetAddress getInetAddress() {
    return address;
  }

  protected int getPort() {
    return port;
  }

  protected int getLocalPort() {
    return localport;
  }

}
