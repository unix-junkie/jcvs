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

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.ServerSocket;

/**
 * This class accepts connections to a single address/port pair for creating
 * channels through port forwards. It contains a thread which basically contains
 * an accept loop in which new connections are accepted and new channels are
 * created along with CHANNEL_OPEN messages to peer. There is one
 * <code>SSH2Listener</code> instance for each local forward.
 *
 * @see SSH2Connection
 */
public final class SSH2Listener implements Runnable {
    private final static int LISTEN_QUEUE_SIZE = 32;

    SSH2Connection          connection;
    SSH2StreamFilterFactory filterFactory;

    private int     acceptTimeout;
    private boolean isLocalForward;
    private int     channelType;

    private String localAddr;
    private int    localPort;
    private String remoteAddr;
    private int    remotePort;

    private ServerSocket listenSocket;

    private int acceptCount;
    private int acceptMax;

    private volatile int numOfRetries;
    private long         retryDelayTime;

    private volatile boolean keepListening;

    private Thread myThread;

    public SSH2Listener(String localAddr, int localPort,
			String remoteAddr, int remotePort,
			SSH2Connection connection,
			SSH2StreamFilterFactory filterFactory,
			int acceptTimeout)
	throws IOException
    {
	try {
	    netscape.security.PrivilegeManager.enablePrivilege("TerminalEmulator");
	} catch (netscape.security.ForbiddenTargetException e) {
	    // !!!
	}

	this.localAddr     = localAddr;
	this.localPort     = localPort;
	this.remoteAddr    = remoteAddr;
	this.remotePort    = remotePort;
	this.connection    = connection;
	this.filterFactory = filterFactory;
	this.keepListening = true;
	this.acceptCount   = 0;
	this.acceptMax     = 0;
	this.acceptTimeout = acceptTimeout;
	this.numOfRetries  = 0;

	this.listenSocket = new ServerSocket(localPort, LISTEN_QUEUE_SIZE,
					     InetAddress.getByName(localAddr));

	if(this.acceptTimeout != 0) {
	    this.listenSocket.setSoTimeout(this.acceptTimeout);
	}

	this.isLocalForward = (remoteAddr != null);

	if(this.isLocalForward) {
	    this.channelType = SSH2Connection.CH_TYPE_DIR_TCPIP;
	} else {
	    this.channelType = SSH2Connection.CH_TYPE_FWD_TCPIP;
	}

	this.myThread = new Thread(this, "SSH2Listener_" + localAddr + ":" +
				   localPort);
	this.myThread.setDaemon(true);
	this.myThread.start();
    }

    public SSH2Listener(String localAddr, int localPort,
			String remoteAddr, int remotePort,
			SSH2Connection connection,
			SSH2StreamFilterFactory filterFactory)
	throws IOException
    {
	this(localAddr, localPort, remoteAddr, remotePort,
	     connection, filterFactory, 0);
    }

    public SSH2Listener(String localAddr, int localPort,
			SSH2Connection connection) throws IOException {
	this(localAddr, localPort, null, -1, connection, null, 0);
    }

    public void run() {
	try {
	    connection.getLog().debug("SSH2Listener",
				      "starting listener on " +
				      localAddr + ":" + localPort);

	    try {
		netscape.security.PrivilegeManager.enablePrivilege("TerminalEmulator");
	    } catch (netscape.security.ForbiddenTargetException e) {
		// !!!
	    }

	    while(keepListening) {
		Socket fwdSocket = null;

		try {
		    fwdSocket = listenSocket.accept();
		} catch (InterruptedIOException e) {
		    continue;
		}

		connection.getEventHandler().listenerAccept(this, fwdSocket);

		acceptCount++;
		synchronized (this) {
		    if(acceptCount == acceptMax) {
			keepListening = false;
		    }
		}
	    }

	} catch(IOException e) {
	    if(keepListening) {
		connection.getLog().error("SSH2Listener", "run",
					  "Error in accept for listener " +
					  localAddr + ":" + localPort + " : " +
					  e.getMessage());
	    }
	} finally {
	    try {
		listenSocket.close();
	    } catch (IOException e) { /* don't care */ }
	    listenSocket = null;

	    connection.getLog().debug("SSH2Listener",
				      "stopping listener on " +
				      localAddr + ":" + localPort);

	}
    }

    public void doConnect(Socket fwdSocket) {
	InetAddress originAddr = fwdSocket.getInetAddress();
	int         originPort = fwdSocket.getPort();

	connection.getEventHandler().listenerConnect(this, fwdSocket);

	connection.getPreferences().setSocketOptions(channelType,
						     fwdSocket);

	try {
	    SSH2TCPChannel channel = null;
	    if(numOfRetries > 0) {
		SSH2RetryingTCPChannel retryChan =
		    new SSH2RetryingTCPChannel(channelType, connection, this,
					       fwdSocket,
					       remoteAddr, remotePort,
					       originAddr.getHostName(),
					       originPort);
		retryChan.setRetries(numOfRetries);
		if(retryDelayTime > 0) {
		    retryChan.setRetryDelay(retryDelayTime);
		}
		channel = retryChan;
	    } else {
		channel =
		    new SSH2TCPChannel(channelType, connection, this,
				       fwdSocket,
				       remoteAddr, remotePort,
				       originAddr.getHostName(), originPort);
	    }

	    connection.getLog().notice("SSH2Listener",
				       "connect from: " +
				       originAddr.getHostAddress() + ":" +
				       originPort + " on " +
				       localAddr + ":" + localPort +
				       ", new ch. #" + channel.getId());

	    if(filterFactory != null) {
		channel.applyFilter(filterFactory.createFilter(connection,
							       channel));
	    }

	    sendChannelOpen(channel, fwdSocket);

	} catch(IOException e) {
	    connection.getLog().error("SSH2Listener", "doConnect",
				      "Error in  " +
				      localAddr + ":" + localPort + " : " +
				      e.getMessage());
	}
    }

    public void sendChannelOpen(SSH2TCPChannel channel, Socket fwdSocket) {
	SSH2TransportPDU pdu = connection.getChannelOpenPDU(channel);

	InetAddress originAddr = fwdSocket.getInetAddress();
	int         originPort = fwdSocket.getPort();

	if(isLocalForward) {
	    pdu.writeString(remoteAddr);
	    pdu.writeInt(remotePort);
	} else {
	    pdu.writeString(localAddr);
	    pdu.writeInt(localPort);
	}

	pdu.writeString(originAddr.getHostAddress());
	pdu.writeInt(originPort);

	connection.transmit(pdu);
    }

    public SSH2Connection getConnection() {
	return connection;
    }

    public synchronized void setAcceptMax(int acceptMax) {
	this.acceptMax = acceptMax;
    }

    public void setRetries(int numOfRetries) {
	this.numOfRetries = numOfRetries;
    }

    public void setRetryDelay(long retryDelayTime) {
	this.retryDelayTime = retryDelayTime;
    }

    public void setThreadPriority(int prio) {
	myThread.setPriority(prio);
    }

    public int getListenPort() {
	return listenSocket.getLocalPort();
    }

    public String getListenHost() {
	return listenSocket.getInetAddress().getHostAddress();
    }

    public int getRemotePort() {
	return remotePort;
    }

    public String getRemoteHost() {
	return remoteAddr;
    }

    public void stop() {
	keepListening = false;
	if(listenSocket != null) {
	    try { listenSocket.close(); }
	    catch (IOException e) { /* don't care */ }
	}
    }

}
