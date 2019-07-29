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
import java.net.Socket;
import java.net.InetAddress;

public class SSH2TCPChannel extends SSH2StreamChannel {

    protected Socket endpoint;
    protected String originAddr;
    protected int    originPort;
    protected String remoteAddr;
    protected int    remotePort;

    public SSH2TCPChannel(int channelType, SSH2Connection connection,
			  Object creator,
			  Socket endpoint,
			  String remoteAddr, int remotePort,
			  String originAddr, int originPort)
	throws IOException
    {
	super(channelType, connection, creator,
	      endpoint.getInputStream(), endpoint.getOutputStream());
	this.endpoint   = endpoint;
	this.remoteAddr = remoteAddr;
	this.remotePort = remotePort;
	this.originAddr = originAddr;
	this.originPort = originPort;
    }

    protected void outputClosed() {
	if(endpoint != null) {
	    try { endpoint.close(); } catch (IOException e) { /* don't care */ }
	}
	endpoint = null;
    }

    protected boolean openFailureImpl(int reasonCode, String reasonText,
				      String langTag) {
	outputClosed();
	return false;
    }

    public InetAddress getAddress() {
	return endpoint.getInetAddress();
    }

    public int getPort() {
	return endpoint.getPort();
    }

    public String getRemoteAddress() {
	return remoteAddr;
    }

    public int getRemotePort() {
	return remotePort;
    }

    public String getOriginAddress() {
	return originAddr;
    }

    public int getOriginPort() {
	return originPort;
    }

    public String toString() {
	String desc = "<N/A>";
	switch(channelType) {
	case SSH2Connection.CH_TYPE_FWD_TCPIP:
	    desc = "[remote] " + originAddr + ":" + originPort + " <--> " +
		getRemoteAddress() + ":" + getRemotePort() + " <--ssh2--> " +
		getAddress().getHostAddress() + ":" + getPort();
	    break;
	case SSH2Connection.CH_TYPE_DIR_TCPIP:
	    SSH2Listener l = (SSH2Listener)creator;
	    desc = "[local] " + originAddr + ":" + originPort + " <--> " +
		l.getListenHost() + ":" + l.getListenPort() + " <--ssh2--> " +
		getRemoteAddress() + ":" + getRemotePort();
	    break;
	default:
	    System.out.println("!!! NOT SUPPORTED IN SSH2TCPChannel.toString !!!");
	    break;
	}
	return desc;
    }

}
