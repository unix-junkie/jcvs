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

public class SSH2TransportPDUPool extends SSH2TransportPDU {

    final static int POOL_SIZE = 32;

    protected class InPDU extends SSH2TransportPDU {
	protected InPDU(int pktType, int bufSize) {
	    super(pktType, bufSize);
	}
	public void release() {
	    this.reset();
	    this.pktSize = 0;
	    releaseIn(this);
	}
    }

    protected class OutPDU extends SSH2TransportPDU {
	protected OutPDU(int pktType, int bufSize) {
	    super(pktType, bufSize);
	}
	public void release() {
	    this.reset();
	    this.pktSize = 0;
	    releaseOut(this);
	}
    }

    int inCnt;
    int outCnt;

    SSH2TransportPDU[] inPool;
    SSH2TransportPDU[] outPool;

    protected SSH2TransportPDUPool() {
	inPool  = new SSH2TransportPDU[POOL_SIZE];
	outPool = new SSH2TransportPDU[POOL_SIZE];
	inCnt   = POOL_SIZE;
	outCnt  = POOL_SIZE;
	for(int i = 0; i < POOL_SIZE; i++) {
	    inPool[i]  = new InPDU(0, PACKET_DEFAULT_SIZE);
	    outPool[i] = new OutPDU(0, PACKET_DEFAULT_SIZE * 2);
	}
    }

    protected SSH2TransportPDU createInPDU(int bufSize) {
	synchronized(inPool) {
	    if(inCnt == 0) {
		return new InPDU(0, PACKET_DEFAULT_SIZE);
	    } else {
		return inPool[--inCnt];
	    }
	}
    }

    protected SSH2TransportPDU createOutPDU(int pktType, int bufSize) {
	if(bufSize > (PACKET_DEFAULT_SIZE * 2)) {
	    return new SSH2TransportPDU(pktType, bufSize);
	}
	SSH2TransportPDU pdu = null;
	synchronized(outPool) {
	    if(outCnt == 0) {
		return new OutPDU(pktType, (PACKET_DEFAULT_SIZE * 2));
	    } else {
		pdu = outPool[--outCnt];
	    }
	}
	pdu.pktType = pktType;
	return pdu;
    }

    protected void releaseIn(InPDU pdu) {
	synchronized(inPool) {
	    if(inCnt < POOL_SIZE) {
		inPool[inCnt++] = pdu;
	    } else {
		/* stat */
	    }
	}
    }

    protected void releaseOut(OutPDU pdu) {
	synchronized(outPool) {
	    if(outCnt < POOL_SIZE) {
		outPool[outCnt++] = pdu;
	    } else {
		/* stat */
	    }
	}
    }

}
