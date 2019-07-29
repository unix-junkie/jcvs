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

public class SSH2RetryingTCPChannel extends SSH2TCPChannel {

    private int  numOfRetries;
    private long retryDelayTime;

    public SSH2RetryingTCPChannel(int channelType, SSH2Connection connection,
				  Object creator,
				  Socket endpoint,
				  String remoteAddr, int remotePort,
				  String originAddr, int originPort)
	throws IOException
    {
	super(channelType, connection, creator,
	      endpoint, remoteAddr, remotePort, originAddr, originPort);
	this.numOfRetries   = 3;
	this.retryDelayTime = 200L;
    }

    protected void setRetries(int numOfRetries) {
	this.numOfRetries = numOfRetries;
    }

    public void setRetryDelay(long retryDelayTime) {
	this.retryDelayTime = retryDelayTime;
    }

    protected boolean openFailureImpl(int reasonCode, String reasonText,
				      String langTag) {
	boolean retry = true;
	if(numOfRetries > 0) {
	    if(getCreator() instanceof SSH2Listener) {
		try {
		    Thread.sleep(retryDelayTime);
		} catch (InterruptedException e) {
		}
		connection.getLog().notice("SSH2RetryingTCPChannel",
					   "retry (" + numOfRetries +
					   ") connection on ch. #" + getId() +
					   " to " + remoteAddr + ":" + remotePort);
		SSH2Listener listener = (SSH2Listener)getCreator();
		listener.sendChannelOpen(this, endpoint);
	    } else {
		connection.getLog().error("SSH2RetryingTCPChannel",
					  "openFailureImpl",
					  "unexpected use of this class");
	    }
	} else {
	    outputClosed();
	    retry = false;
	}
	numOfRetries--;

	return retry;
    }

}
