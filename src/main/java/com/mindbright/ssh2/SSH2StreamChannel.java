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

import com.mindbright.util.Queue;

public class SSH2StreamChannel extends SSH2Channel {
    protected InputStream  in;
    protected OutputStream out;

    protected Thread transmitter;
    protected Thread receiver;
    protected Queue  rxQueue;
    protected long   txCounter;
    protected long   rxCounter;

    private final static boolean QUEUED_RX_CHAN = true;

    protected SSH2StreamChannel(int channelType, SSH2Connection connection,
				Object creator,
				InputStream in, OutputStream out) {
	super(channelType, connection, creator);
	this.in  = in;
	this.out = out;
	createStreams();
    }

    public void applyFilter(SSH2StreamFilter filter) {
	if(filter != null) {
	    in  = filter.getInputFilter(in);
	    out = filter.getOutputFilter(out);
	}
    }

    private void channelTransmitLoop() {
	connection.getLog().debug("SSH2StreamChannel",
				  "starting ch. #" + channelId +
				  " (" + getType() + ") transmitter");
	Thread.yield();
	try {
	    SSH2TransportPDU pdu;
	    int              maxSz = 0;
	    int              rcvSz = 0;
	    while(!eofSent) {
		pdu =
		    SSH2TransportPDU.createOutgoingPacket(SSH2.MSG_CHANNEL_DATA,
							  txMaxPktSz + 256);
		pdu.writeInt(peerChanId);
		maxSz = checkTxWindowSize(rcvSz);
		rcvSz = in.read(pdu.data, pdu.wPos + 4, maxSz);
		if(rcvSz == -1) {
		    sendEOF();
		} else {
		    pdu.writeInt(rcvSz);
		    pdu.wPos  += rcvSz;
		    txCounter += rcvSz;
		    transmit(pdu);
		}
	    }
	} catch (IOException e) {
	    if(!eofSent) {
		connection.getLog().error("SSH2StreamChannel",
					  "channelTransmitLoop",
					  e.toString());
	    }
	} finally {
	    try { in.close(); } catch (IOException e) { /* don't care */ };
	    sendClose();
	}
	connection.getLog().debug("SSH2StreamChannel",
				  "exiting ch. #" +
				  channelId + " (" + getType() +
				  ") transmitter, " + txCounter +
				  " bytes tx");
    }

    private void channelReceiveLoop() {
	connection.getLog().debug("SSH2StreamChannel",
				  "starting ch. #" + channelId +
				  " (" + getType() + ") receiver");
	Thread.yield();
	try {
	    SSH2TransportPDU pdu;
	    while((pdu = (SSH2TransportPDU)rxQueue.getFirst()) != null) {
		rxWrite(pdu);
	    }
	} catch (IOException e) {
	    connection.getLog().error("SSH2StreamChannel",
				      "channelReceiveLoop",
				      e.toString());
	} finally {
	    rxClosing();
	}
	connection.getLog().debug("SSH2StreamChannel",
				  "exiting ch. #" +
				  channelId + " (" + getType() +
				  ") receiver, " + rxCounter +
				  " bytes rx");
    }

    private final void rxWrite(SSH2TransportPDU pdu) throws IOException {
	int len = pdu.readInt();
	int off = pdu.getRPos();
	rxCounter += len;
	out.write(pdu.data, off, len);
	pdu.release();
	checkRxWindowSize(len);
    }

    private final void rxClosing() {
	// Signal to transmitter that this is an orderly shutdown
	//
	eofSent = true;
	try { out.close(); } catch (IOException e) { /* don't care */ }
	try { in.close(); } catch (IOException e) { /* don't care */ }
	outputClosed();

	// there is a slight chance that the transmitter is waiting for
	// window adjust in which case we must interrupt it here so it
	// doesn't hang
	//
	if(txCurrWinSz == 0) {
	    txCurrWinSz = -1;
	    transmitter.interrupt();
	}
    }

    private final synchronized int checkTxWindowSize(int lastSz) {
	txCurrWinSz -= lastSz;
	while(txCurrWinSz == 0) {
	    // Our window is full, wait for ACK from peer
	    try {
		this.wait();
	    } catch (InterruptedException e) {
		if(!eofSent) {
		    connection.getLog().error("SSH2StreamChannel",
					      "checkTxWindowSize",
					      "window adjust wait interrupted");
		}
	    }
	}
	// Try sending remaining window size or max packet size before ACK
	//
	int dataSz = (txCurrWinSz < txMaxPktSz ? txCurrWinSz : txMaxPktSz);
	return dataSz;
    }

    private final void checkRxWindowSize(int len) {
	rxCurrWinSz -= len;
	if(rxCurrWinSz < 0) {
	    connection.fatalDisconnect(SSH2.DISCONNECT_PROTOCOL_ERROR,
				       "Peer overflowed window");
	} else if(rxCurrWinSz <= (rxInitWinSz >>> 1)) {
	    // ACK on >= 50% of window received
	    SSH2TransportPDU pdu =
		SSH2TransportPDU.createOutgoingPacket(SSH2.MSG_CHANNEL_WINDOW_ADJUST);
	    pdu.writeInt(peerChanId);
	    pdu.writeInt(rxInitWinSz - rxCurrWinSz);
	    transmit(pdu);
	    rxCurrWinSz = rxInitWinSz; 
	}
    }

    protected void data(SSH2TransportPDU pdu) {
	if(QUEUED_RX_CHAN) {
	    rxQueue.putLast(pdu);
	} else {
	    try {
		rxWrite(pdu);
	    } catch (IOException e) {
		connection.getLog().error("SSH2StreamChannel",
					  "data",
					  e.toString());
		rxClosing();
	    }
	}
    }

    protected void openConfirmationImpl(SSH2TransportPDU pdu) {
	startStreams();
    }

    protected boolean openFailureImpl(int reasonCode, String reasonText,
				   String langTag) {
	// Just return false since we don't want to keep the channel,
	// handle in derived class if needed
	return false;
    }

    protected synchronized void windowAdjustImpl(int inc) {
	txCurrWinSz += inc;
	this.notify();
    }

    protected void eofImpl() {
	if(QUEUED_RX_CHAN) {
	    rxQueue.setBlocking(false);
	} else {
	    rxClosing();
	}
    }

    protected void closeImpl() {
	eofImpl();
    }

    protected void outputClosed() {
	// Do nothing, handle in derived class if needed
    }

    protected void handleRequestImpl(String type, boolean wantReply,
				     SSH2TransportPDU pdu) {
	// Do nothing, handle in derived class if needed
    }

    protected void createStreams() {
	if(QUEUED_RX_CHAN) {
	    receiver = new Thread(new Runnable() {
		    public void run() {
			channelReceiveLoop();
		    }
		}, "SSH2StreamRX_" + getType() + "_" + channelId);
	    receiver.setDaemon(false);
	    rxQueue = new Queue();
	}
	transmitter = new Thread(new Runnable() {
		public void run() {
		    channelTransmitLoop();
		}
	    }, "SSH2StreamTX_" + getType() + "_" + channelId);
	transmitter.setDaemon(false);
    }

    protected void startStreams() {
	transmitter.start();
	if(QUEUED_RX_CHAN) {
	    receiver.start();
	}
    }
}


