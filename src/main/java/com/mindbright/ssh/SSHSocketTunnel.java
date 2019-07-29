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

public final class SSHSocketTunnel extends SSHTunnel {

  public final static class SSHSocketIS extends InputStream {
    protected SSHSocketTunnel tunnel;
    protected SSHSocketIS(SSHSocketTunnel tunnel) {
      this.tunnel = tunnel;
    }
    public int read() throws IOException {
      byte[] b = new byte[1];
      if(read(b, 0, 1) == -1)
	return -1;
      return (int) b[0];
    }
    public int read(byte b[], int off, int len) throws IOException {
      return tunnel.read(b, off, len);
    }
    public void close() throws IOException {
      tunnel.closein(true);
    }
  }

  public final static class SSHSocketOS extends OutputStream {
    protected SSHSocketTunnel tunnel;
    protected SSHSocketOS(SSHSocketTunnel tunnel) {
      this.tunnel = tunnel;
    }
    public void write(int b) throws IOException {
      byte[] ba = new byte[1];
      ba[0] = (byte) b;
      tunnel.write(ba, 0, 1);
    }
    public void write(byte b[], int off, int len) throws IOException {
      tunnel.write(b, off, len);
    }
    public void close() throws IOException {
      tunnel.closeout();
    }
  }

  Object  lock;
  boolean inputClosePending;
  boolean inputExplicitClosed;
  boolean outputClosed;
  boolean terminated;
  boolean openFail;

  protected SSHPdu      rest;
  protected SSHSocketIS in;
  protected SSHSocketOS out;
  protected InetAddress localAddress;

  protected SSHSocketImpl impl;

  public SSHSocketTunnel(SSHChannelController controller, SSHSocketImpl impl) throws IOException {
    super(null, controller.newChannelId(), SSH.UNKNOWN_CHAN_NUM, controller);

    this.lock    = new Object();

    this.inputClosePending   = false;
    this.inputExplicitClosed = false;
    this.outputClosed        = false;
    this.terminated          = false;
    this.openFail            = false;

    this.txQueue = new Queue();
    this.in      = new SSHSocketIS(this);
    this.out     = new SSHSocketOS(this);
    this.impl    = impl;
  }

  public void start() {
    synchronized(lock) {
      lock.notify();
    }
  }

  public void openFailure() {
    openFail = true;
    start();
  }

  public int read(byte b[], int off, int len) throws IOException {
    SSHPdu pdu = null;
    int    actLen;

    synchronized(this) {
	if(inputExplicitClosed)
	    throw new SocketException("Socket closed");
    }

    // We reuse the connect-lock since it is only used before we
    // start, after that it becomes the read-synchronization lock
    //
    synchronized(lock) {
	if(rest != null) {
	    pdu  = rest;
	    rest = null;
	} else if(inputClosePending && txQueue.isEmpty()) {
	    pdu = null;
	} else {
	    pdu = (SSHPdu)txQueue.getFirst();
	}

	if(pdu == null)
	    return -1;

	int rawLen = pdu.rawSize();
	if(len < rawLen) {
	    rest   = pdu;
	    actLen = len;
	} else {
	    actLen = rawLen;
	}

	System.arraycopy(pdu.rawData(), pdu.rawOffset(), b, off, actLen);

	if(rest != null) {
	    rest.rawAdjustSize(rawLen - len);
	}
    }

    return actLen;
  }

  public void write(byte b[], int off, int len) throws IOException {
    SSHPduOutputStream pdu;

    synchronized(this) {
	if(outputClosed)
	    throw new IOException("Resource temporarily unavailable");
    }

    pdu = new SSHPduOutputStream(SSH.MSG_CHANNEL_DATA,
				 controller.sndCipher, controller.sndComp,
				 SSH.secureRandom());
    pdu = (SSHPduOutputStream)prepare(pdu);
    pdu.writeInt(len);
    pdu.write(b, off, len);
    controller.transmit(pdu);
  }

  public void connect(String host, int port) throws IOException {
    SSHPduOutputStream respPdu;

    setRemoteDesc(host + ":" + port);

    respPdu = new SSHPduOutputStream(SSH.MSG_PORT_OPEN,
				     controller.sndCipher, controller.sndComp,
				     SSH.secureRandom());
    controller.addTunnel(this);
    respPdu.writeInt(channelId);
    respPdu.writeString(host);
    respPdu.writeInt(port);
    respPdu.writeString(localAddress.getHostAddress());
    controller.transmit(respPdu);

    // Wait for start() to be called (i.e. the channel to be confirmed open)
    //
    synchronized(lock) {
      try {
	lock.wait();
      } catch(InterruptedException e) {
	// !!!
      }
    }

    if(openFail)
      throw new ConnectException("Connection Refused");
  }

// TGE begin support for JDK1.4

  // UNDONE I simply do not know how to handle the timeout request. It is
  // simple enough to place it into the lock.wait() call, but then if it
  // timesout, how do we know the state of the connection?! Further, how
  // do we terminate it? I think MindBright needs to deal with this one...

  public void connect(String host, int port, int timeoutMillis) throws IOException {
    this.connect( host, port );
  }

// TGE end

  public void close() throws IOException {
    closein(true);
    closeout();
  }

  public synchronized void closeout() {
    outputClosed = true;
    sendInputEOF();
  }

  public void closein(boolean explicit) {
    txQueue.release();
    synchronized(this) {
      inputClosePending   = true;
      inputExplicitClosed = explicit;
      sendOutputClosed();
    }
  }

  public int available() throws IOException {
    if(rest != null)
      return rest.rawSize();
    else
      return 0;
  }

  protected void setLocalAddress(InetAddress localAddress) {
    this.localAddress = localAddress;
  }

  public synchronized void checkTermination() {
    if(sentInputEOF && sentOutputClosed &&
       receivedInputEOF && receivedOutputClosed) {
      terminated = true;
      controller.delTunnel(channelId);
      impl.factory.closePseudoUser(controller.sshAsClient(), impl);
    }
  }

  public synchronized boolean terminated() {
    return terminated;
  }

  public void receiveOutputClosed() {
    super.receiveOutputClosed();
    closeout();
  }

  public void receiveInputEOF() {
    // !!! NOTE: we do not call super's method...
    receivedInputEOF = true;
    closein(false);
    closeout();
    checkTermination();
  }

}
