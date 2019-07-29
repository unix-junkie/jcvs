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

/**
 * This is the abstract base class for all channels as defined in the connection
 * protocol spec. Each channel has a specific type (e.g. session). Each channel
 * type is implemented by a subclass to this class. This base class makes no
 * assumptions as to how data is handled by the channel it only implements
 * methods which are used from the multiplexing code in
 * <code>SSH2Connection</code>.
 * <p>
 * When implementing a new channel type or implementing an existing one
 * differntly from what available one typically subclasses
 * <code>SSH2StreamChannel</code> instead of <code>SSH2Channel</code> directly
 * since it implements the notion of streams (through use of
 * <code>java.io.InputStream</code> and <code>java.io.OutputStream</code>) and
 * flow control. Only a very specific implementation of a channel would need to
 * subclass <code>SSH2Channel</code> directly.

 *
 * @see SSH2Connection
 * @see SSH2StreamChannel
 */
public abstract class SSH2Channel {
    public final static int STATUS_UNDEFINED = 0;
    public final static int STATUS_OPEN      = 1;
    public final static int STATUS_CLOSED    = 2;
    public final static int STATUS_FAILED    = 3;

    protected SSH2Connection connection;

    protected int channelType;
    
    protected int channelId;
    protected int peerChanId;

    protected int rxMaxPktSz;
    protected int rxInitWinSz;
    protected int rxCurrWinSz;

    protected int txInitWinSz;
    protected int txCurrWinSz;
    protected int txMaxPktSz;

    protected volatile boolean eofSent;
    protected volatile boolean eofReceived;
    protected volatile boolean closeReceived;
    protected volatile boolean closeSent;
    protected volatile boolean deleted;

    protected Object creator;

    protected Object openMonitor;
    protected int    openStatus;

    protected SSH2Channel(int channelType, SSH2Connection connection,
			  Object creator)
    {
	SSH2ConnectionPreferences prefs = connection.getPreferences();
	this.channelType = channelType;
	this.connection  = connection;
	this.creator     = creator;
	this.rxInitWinSz = prefs.getRxInitWinSz(channelType);
	this.rxCurrWinSz = this.rxInitWinSz;
	this.rxMaxPktSz  = prefs.getRxMaxPktSz(channelType);
	this.openStatus  = STATUS_UNDEFINED;
	this.openMonitor = new Object();
	connection.addChannel(this);
    }

    protected synchronized final void openConfirmation(SSH2TransportPDU pdu) {
	int peerChanId  = pdu.readInt();
	int txInitWinSz = pdu.readInt();
	int txMaxPktSz  = pdu.readInt();
	init(peerChanId, txInitWinSz, txMaxPktSz);
	openConfirmationImpl(pdu);

	switch(channelType) {
	case SSH2Connection.CH_TYPE_FWD_TCPIP:
	    connection.getEventHandler().localForwardedConnect(connection,
						       (SSH2Listener)creator,
							       this);
	    break;
	case SSH2Connection.CH_TYPE_DIR_TCPIP:
	    connection.getEventHandler().localDirectConnect(connection,
						    (SSH2Listener)creator,
							    this);
	    break;
	case SSH2Connection.CH_TYPE_SESSION:
	    connection.getEventHandler().localSessionConnect(connection,
							     this);
	    break;
	case SSH2Connection.CH_TYPE_X11:
	    connection.getEventHandler().localX11Connect(connection,
							 (SSH2Listener)creator,
							 this);
	    break;
	    /* !!! TODO
	       case SSH2Connection.CH_TYPE_AUTH_AGENT:
	           connection.getEventHandler().localDirectConnect(connection,
	               (SSH2Listener)creator,
		       this);
	           break; */
	}

	synchronized (openMonitor) {
	    this.openStatus  = STATUS_OPEN;
	    openMonitor.notifyAll();
	}

	connection.getLog().notice("SSH2Channel",
				   "open confirmation, ch. #" + channelId +
				   ", init-winsz = " + txInitWinSz +
				   ", max-pktsz = " + txMaxPktSz);
    }

    protected synchronized final void openFailure(int reasonCode,
						  String reasonText,
						  String langTag) {
	closeSent = true;
	eofSent   = true;

	boolean keepChannel = openFailureImpl(reasonCode, reasonText, langTag);

	connection.getEventHandler().localChannelOpenFailure(connection,
							     this,
							     reasonCode,
							     reasonText,
							     langTag);
	if(!keepChannel) {
	    connection.delChannel(this);
	}

	synchronized (openMonitor) {
	    this.openStatus  = STATUS_FAILED;
	    openMonitor.notifyAll();
	}

	connection.getLog().notice("SSH2Channel", "open failure on ch. #" +
				   channelId + ", reason: " + reasonText);
    }

    protected final void windowAdjust(SSH2TransportPDU pdu) {
	int inc = pdu.readInt();
	windowAdjustImpl(inc);
    }

    protected void data(SSH2TransportPDU pdu) {
    }

    protected void extData(SSH2TransportPDU pdu) {
    }

    protected final void handleRequest(SSH2TransportPDU pdu) {
	String  reqType   = new String(pdu.readString());
	boolean wantReply = pdu.readBoolean();
	handleRequestImpl(reqType, wantReply, pdu);
    }

    protected void requestSuccess(SSH2TransportPDU pdu) {
    }

    protected void requestFailure(SSH2TransportPDU pdu) {
    }

    private final void checkTermination() {
	if(closeSent && closeReceived && !deleted) {
	    deleted = true;
	    connection.delChannel(this);
	}
    }

    protected final void recvEOF() {
	if(eofReceived) {
	    connection.getLog().debug("SSH2Channel", "ch. # " + channelId +
				      " received multiple EOFs");
	}
	eofReceived = true;
	eofImpl();
	if(eofSent) {
	    sendClose();
	}
    }

    protected final synchronized void recvClose() {
	if(!closeReceived) {
	    closeReceived = true;
	    eofSent       = true;
	    closeImpl();
	    sendClose();
	    connection.getLog().debug("SSH2Channel",
				      "closing ch. #" + channelId +
				      " (" + getType() + ")");
	    connection.getEventHandler().channelClosed(connection, this);
	}
	checkTermination();
    }

    protected final void sendEOF() {
	if(!eofSent && !closeSent) {
	    SSH2TransportPDU pdu =
		SSH2TransportPDU.createOutgoingPacket(SSH2.MSG_CHANNEL_EOF);
	    pdu.writeInt(peerChanId);
	    connection.transmit(pdu);
	    eofSent = true;
	}
    }

    protected final synchronized void sendClose() {
	if(!closeSent) {
	    SSH2TransportPDU pdu =
		SSH2TransportPDU.createOutgoingPacket(SSH2.MSG_CHANNEL_CLOSE);
	    pdu.writeInt(peerChanId);
	    connection.transmit(pdu);
	    closeSent = true;
	    synchronized (openMonitor) {
		openStatus = STATUS_CLOSED;
		openMonitor.notifyAll();
	    }
	}
	checkTermination();
    }

    protected void init(int peerChanId, int txInitWinSz, int txMaxPktSz) {
	this.peerChanId  = peerChanId;
	this.txInitWinSz = txInitWinSz;
	this.txMaxPktSz  = txMaxPktSz;
	this.txCurrWinSz = txInitWinSz;
    }

    protected void transmit(SSH2TransportPDU pdu) {
	if(!closeSent) {
	    connection.transmit(pdu);
	}
    }

    public int openStatus() {
	synchronized (openMonitor) {
	    if(openStatus == STATUS_UNDEFINED) {
		try {
		    openMonitor.wait();
		} catch (InterruptedException e) {
		    /* don't care, someone interrupted us on purpose */
		}
	    }
	    return openStatus;
	}
    }

    public final synchronized void close() {
	if(!connection.getTransport().isConnected()) {
	    recvClose();
	}
	sendClose();
    }

    public String getType() {
	return SSH2Connection.channelTypes[channelType];
    }

    public int getId() {
	return channelId;
    }

    public int getPeerId() {
	return peerChanId;
    }

    public Object getCreator() {
	return creator;
    }

    protected abstract void openConfirmationImpl(SSH2TransportPDU pdu);
    protected abstract boolean openFailureImpl(int reasonCode,
					       String reasonText,
					       String langTag);
    protected abstract void windowAdjustImpl(int inc);
    protected abstract void eofImpl();
    protected abstract void closeImpl();
    protected abstract void handleRequestImpl(String reqType, boolean wantReply,
					      SSH2TransportPDU pdu);

}
