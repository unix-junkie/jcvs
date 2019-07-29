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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import com.mindbright.util.InputStreamPipe;
import com.mindbright.util.OutputStreamPipe;

/**
 * This class implements session channels as defined in the connection protocol
 * spec. It can be used to start shells, commands, and subsystems on the server.
 * An instance of this class is created with the <code>newSession</code> methods
 * found in <code>SSH2Connection</code>.
 *
 * @see SSH2Channel
 * @see SSH2Connection
 */
public final class SSH2SessionChannel extends SSH2StreamChannel {

    private final static int DEFAULT_STDIO_BUFFER_SIZE = 65536;

    private class StdErrDummyStream extends OutputStream {
	public void write(int b) {
	    write(new byte[] { (byte)b }, 0, 1);	    
	}
	public void write(byte[] buf, int off, int len) {
	    connection.getLog().info("StdErrDummyStream",
				     new String(buf, off, len));
	}
    }

    public static final int EXIT_ON_CLOSE   = -1;
    public static final int EXIT_ON_FAILURE = -2;

    protected boolean started;
    protected boolean blocking;
    protected boolean exited;
    protected Object  exitMonitor;
    protected Object  reqMonitor;
    protected boolean exitedOnSignal;
    protected int     exitStatus;
    protected boolean reqStatus;
    protected boolean x11Mapping;

    protected InputStream      stdout;
    protected OutputStream     stdin;
    protected InputStreamPipe  stderr;
    protected OutputStream     stderrW;

    protected SSH2SessionChannel(SSH2Connection connection) {
	super(SSH2Connection.CH_TYPE_SESSION, connection, connection,
	      new InputStreamPipe(DEFAULT_STDIO_BUFFER_SIZE),
	      new OutputStreamPipe());
	this.rxInitWinSz = 16384;
	this.rxCurrWinSz = 16384;
	this.rxMaxPktSz  = 8192;
	this.started     = false;
	this.exited      = false;
	this.blocking    = true;
	this.reqStatus   = true;
	this.x11Mapping  = false;
	this.exitMonitor = new Object();
	this.reqMonitor  = new Object();

	try {
	    this.stdin   = new OutputStreamPipe();
	    this.stdout  = new InputStreamPipe(DEFAULT_STDIO_BUFFER_SIZE);
	    this.stderrW = new StdErrDummyStream();
	    ((InputStreamPipe)this.stdout).connect((OutputStreamPipe)out);
	    ((OutputStreamPipe)this.stdin).connect((InputStreamPipe)in);
	} catch (IOException e) {
	    connection.getLog().error("SSH2SessionChannel", "<constructor>",
				      "can't happen, bug somewhere!?!");
	}
    }

    public boolean doShell() {
	if(started || openStatus() != SSH2Channel.STATUS_OPEN) {
	    return false;
	}
	SSH2TransportPDU pdu = getRequestPDU(SSH2Connection.CH_REQ_SHELL);
	started = sendAndBlockUntilReply(pdu);
	return started;
    }

    public boolean doSingleCommand(String command) {
	if(started || openStatus() != SSH2Channel.STATUS_OPEN) {
	    return false;
	}
	SSH2TransportPDU pdu = getRequestPDU(SSH2Connection.CH_REQ_EXEC);
	pdu.writeString(command);
	started = sendAndBlockUntilReply(pdu);
	return started;
    }

    public boolean doSubsystem(String subsystem) {
	if(started || openStatus() != SSH2Channel.STATUS_OPEN) {
	    return false;
	}
	SSH2TransportPDU pdu = getRequestPDU(SSH2Connection.CH_REQ_SUBSYSTEM);
	pdu.writeString(subsystem);
	started = sendAndBlockUntilReply(pdu);
	return started;
    }

    public int waitForExit(long timeout) {
	synchronized (exitMonitor) {
	    if(!exited) {
		try { exitMonitor.wait(timeout); }
		catch (InterruptedException e) { /* don't care */ }
	    }
	    // !!! TODO: Handle signals, maybe should throw exception ???
	    return exitStatus;
	}
    }

    public void changeStdOut(OutputStream out) {
	this.out = out;
    }

    public void changeStdIn(InputStream in) {
	this.in = in;
    }

    public void changeStdErr(OutputStream stderrW) {
	this.stderrW = stderrW;
    }

    public void enableStdErr() {
	this.stderrW = new OutputStreamPipe();
	this.stderr  = new InputStreamPipe();
	try {
	    this.stderr.connect((OutputStreamPipe)stderrW);
	} catch (IOException e) {
	    connection.getLog().error("SSH2SessionChannel", "enableStdErr",
				      "can't happen, bug somewhere!?!");
	}
    }

    public InputStream getStdOut() {
	return stdout;
    }

    public OutputStream getStdIn() {
	return stdin;
    }

    public InputStream getStdErr() {
	return stderr;
    }

    public void stdinWriteNoLatency(String str) {
	byte[] b = str.getBytes();
	stdinWriteNoLatency(b, 0, b.length);
    }

    public void stdinWriteNoLatency(byte[] buf, int off, int len) {
	SSH2TransportPDU pdu =
	    SSH2TransportPDU.createOutgoingPacket(SSH2.MSG_CHANNEL_DATA,
						  len + 128);
	pdu.writeInt(peerChanId);
	pdu.writeInt(len);
	pdu.writeRaw(buf, off, len);
	transmit(pdu);
	txCounter += len;
    }

    public void stdinWriteNoLatency(int c) {
	stdinWriteNoLatency(new byte[] { (byte)c }, 0, 1);
    }

    public void setBlocking(boolean value) {
	synchronized (reqMonitor) {
	    this.blocking = value;
	}
    }

    public boolean requestPTY(String termType, int rows, int cols,
			      byte[] terminalModes) {
	if(openStatus() != SSH2Channel.STATUS_OPEN) {
	    return false;
	}

	SSH2TransportPDU pdu = getRequestPDU(SSH2Connection.CH_REQ_PTY);
	pdu.writeString(termType);
	pdu.writeInt(cols);
	pdu.writeInt(rows);
	pdu.writeInt(0);
	pdu.writeInt(0);
	if(terminalModes == null)
	    terminalModes = new byte[] { 0 };
	pdu.writeString(terminalModes);
	return sendAndBlockUntilReply(pdu);
    }

    public boolean requestX11Forward(String localAddr, int localPort,
				     byte[] cookie, boolean single, int screen) {
	connection.getPreferences().setX11LocalAddr(localAddr);
	connection.getPreferences().setX11LocalPort(localPort);
	connection.getPreferences().setX11Cookie(cookie);
	return requestX11Forward(single, screen);
    }

    public boolean requestX11Forward(boolean single, int screen) {
	if(openStatus() != SSH2Channel.STATUS_OPEN ||
	   x11Mapping) {
	    if(x11Mapping)
		connection.getLog().warning("SSH2SessionChannel",
				    "requesting x11 forward multiple times");
	    return false;
	}

	byte[] x11FakeCookie = connection.getX11FakeCookie();
	StringBuffer cookieBuf = new StringBuffer();
	for(int i = 0; i < 16; i++) {
	    String b = Integer.toHexString(x11FakeCookie[i] & 0xff);
	    if(b.length() == 1) {
		b = "0" + b;
	    }
	    cookieBuf.append(b);
	}
	String           cookie = cookieBuf.toString();
	SSH2TransportPDU pdu    = getRequestPDU(SSH2Connection.CH_REQ_X11);

	pdu.writeBoolean(single);
	pdu.writeString("MIT-MAGIC-COOKIE-1");
	pdu.writeString(cookie);
	pdu.writeInt(screen);

	x11Mapping = sendAndBlockUntilReply(pdu);

	if(x11Mapping) {
	    connection.setX11Mapping(single);
	}

	return x11Mapping;
    }

    public boolean setEnvironment(String name, String value) {
	if(openStatus() != SSH2Channel.STATUS_OPEN) {
	    return false;
	}

	SSH2TransportPDU pdu = getRequestPDU(SSH2Connection.CH_REQ_ENV);
	pdu.writeString(name);
	pdu.writeString(value);
	return sendAndBlockUntilReply(pdu);
    }

    public void sendWindowChange(int rows, int cols) {
	SSH2TransportPDU pdu =
	    getNoReplyRequestPDU(SSH2Connection.CH_REQ_WINCH);
	pdu.writeInt(cols);
	pdu.writeInt(rows);
	pdu.writeInt(0);
	pdu.writeInt(0);
	transmit(pdu);
    }

    public void sendSignal(int signal) {
	SSH2TransportPDU pdu =
	    getNoReplyRequestPDU(SSH2Connection.CH_REQ_SIGNAL);
	pdu.writeInt(signal);
	transmit(pdu);
    }

    public void doExit(int status, boolean onSignal) {
	synchronized (exitMonitor) {
	    if(!exited) {
		exited = true;
		if(x11Mapping) {
		    x11Mapping = false;
		    connection.clearX11Mapping();
		}
		this.exitedOnSignal = onSignal;
		this.exitStatus     = status;
		exitMonitor.notifyAll();
	    }
	}
    }

    protected void extData(SSH2TransportPDU pdu) {
	int type = pdu.readInt();
	if(type != SSH2.EXTENDED_DATA_STDERR) {
	    connection.getLog().error("SSH2SessionChannel", "extData",
				      "extended data of unknown type: " + type);
	} else {
	    try {
		int len = pdu.readInt();
		stderrW.write(pdu.getData(), pdu.getRPos(), len);
	    } catch (IOException e) {
		connection.getLog().error("SSH2SessionChannel", "extData",
					  "error writing to stderr: " +
					  e.getMessage());
	    }
	}
    }

    protected void closeImpl() {
	super.closeImpl();
	doExit(EXIT_ON_CLOSE, false);
	//
	// Just to make sure everybody gets released
	//
	requestFailure((SSH2TransportPDU)null);
    }

    protected boolean openFailureImpl(int reasonCode, String reasonText,
				   String langTag) {
	doExit(EXIT_ON_FAILURE, false);
	return false;
    }

    protected void requestSuccess(SSH2TransportPDU pdu) {
	synchronized (reqMonitor) {
	    reqStatus = true;
	    reqMonitor.notify();
	}
    }

    protected void requestFailure(SSH2TransportPDU pdu) {
	synchronized (reqMonitor) {
	    reqStatus = false;
	    reqMonitor.notify();
	}
    }

    protected void handleRequestImpl(String type, boolean wantReply,
				     SSH2TransportPDU pdu) {

	// !!! TODO: Handle exit properly...

	if(type.equals(SSH2Connection.CH_REQ_EXIT_STAT)) {
	    int status = pdu.readInt();
	    doExit(status, false);

	    connection.getLog().notice("SSH2SessionChannel",
				       "session exit with status " + status);

	} else if(type.equals(SSH2Connection.CH_REQ_EXIT_SIG)) {
	    int     sig  = pdu.readInt();
	    boolean core = pdu.readBoolean();
	    String  msg  = new String(pdu.readString());
	    doExit(sig, true);

	    // !!! TODO: store msg/core also !!!

	    connection.getLog().notice("SSH2SessionChannel",
				       "session exit with signal " + sig +
				       ", " + msg + ", core dumped? " + core);
	} else {
	    connection.getLog().error("SSH2SessionChannel", "handleRequestImpl",
				      "got unknown channel-request: " + type);
            if(wantReply) {
		SSH2TransportPDU reply =
		    SSH2TransportPDU.createOutgoingPacket(SSH2.MSG_CHANNEL_FAILURE);
                reply.writeInt(peerChanId);
                transmit(reply);
            }
	}
    }

    private boolean sendAndBlockUntilReply(SSH2TransportPDU pdu) {
	synchronized (reqMonitor) {
	    transmit(pdu);
	    try {
		if(blocking)
		    reqMonitor.wait(); 
	    } catch (InterruptedException e) {
		connection.getLog().error("SSH2SessionChannel",
					  "sendAndBlockUntilReply",
					  "wait for reply interrupted");
	    }
	    boolean s = reqStatus;
	    reqStatus = true;
	    return s;
	}
    }

    private SSH2TransportPDU getRequestPDU(String type) {
	SSH2TransportPDU pdu =
	    SSH2TransportPDU.createOutgoingPacket(SSH2.MSG_CHANNEL_REQUEST);
	pdu.writeInt(peerChanId);
	pdu.writeString(type);
	synchronized (reqMonitor) {
	    pdu.writeBoolean(blocking);
	}
	return pdu;
    }

    private SSH2TransportPDU getNoReplyRequestPDU(String type) {
	SSH2TransportPDU pdu =
	    SSH2TransportPDU.createOutgoingPacket(SSH2.MSG_CHANNEL_REQUEST);
	pdu.writeInt(peerChanId);
	pdu.writeString(type);
	pdu.writeBoolean(false);
	return pdu;
    }

}
