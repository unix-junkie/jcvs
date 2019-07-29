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

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.ShortBufferException;

import com.mindbright.util.SecureRandomAndPad;

public class SSH2TransportPDU extends SSH2DataBuffer {
    public final static int PACKET_DEFAULT_SIZE = 8192 + 256;
    public final static int PACKET_MIN_SIZE     = 16;
    public final static int PACKET_MAX_SIZE     = 35000;

    // !!! public static SSH2TransportPDU factoryInstance = new SSH2TransportPDU();
    public static SSH2TransportPDU factoryInstance = new SSH2TransportPDUPool();

    byte[] macTmpBuf;

    int    pktSize;
    int    padSize;
    int    pktType;

    protected SSH2TransportPDU() {
	/* Factory instance constructor */
    }

    protected SSH2TransportPDU(int pktType, int bufSize) {
	super(bufSize);
	this.pktType   = pktType;
	this.pktSize   = 0;
	this.padSize   = 0;
	this.macTmpBuf = new byte[128];
    }

    protected SSH2TransportPDU createInPDU(int bufSize) {
	return new SSH2TransportPDU(0, bufSize);
    }

    protected SSH2TransportPDU createOutPDU(int pktType, int bufSize) {
	return new SSH2TransportPDU(pktType, bufSize);
    }

    public final static void setFactoryInstance(SSH2TransportPDU factory) {
	factoryInstance = factory;
    }

    public final static SSH2TransportPDU createIncomingPacket(int bufSize) {
	return factoryInstance.createInPDU(bufSize);
    }

    public final static SSH2TransportPDU createIncomingPacket() {
	return createIncomingPacket(PACKET_DEFAULT_SIZE);
    }

    public final static SSH2TransportPDU createOutgoingPacket(int pktType,
							      int bufSize) {
	SSH2TransportPDU pdu = factoryInstance.createOutPDU(pktType, bufSize);
	pdu.writeInt(0);  // dummy sequence number
	pdu.writeInt(0);  // dummy length
	pdu.writeByte(0); // dummy pad-length
	pdu.writeByte(pktType);
	return pdu;
    }

    public final static SSH2TransportPDU createOutgoingPacket(int pktType) {
	return createOutgoingPacket(pktType, PACKET_DEFAULT_SIZE);
    }

    public void release() {
    }

    public SSH2TransportPDU makeCopy() {
	SSH2TransportPDU copy = factoryInstance.createOutPDU(this.pktType,
							     this.data.length);
	System.arraycopy(this.data, 0, copy.data, 0, this.data.length);
	copy.pktSize = this.pktSize;
	copy.padSize = this.padSize;
	copy.rPos    = this.rPos;
	copy.wPos    = this.wPos;

	return copy;
    }

    public int getType() {
	return pktType;
    }

    public int getPayloadLength() {
	int plSz;
	if(pktSize == 0) {
	    plSz = wPos - getPayloadOffset();
	} else {
	    plSz = pktSize - padSize - 1;
	}
	return plSz;
    }

    public int getPayloadOffset() {
	return 4 + 4 + 1; // Skip sequence, length and padsize
    }

    protected void readFrom(InputStream in, int seqNum,
			    Mac mac, Cipher cipher, SSH2Compressor compressor)
	throws IOException, SSH2Exception, ShortBufferException
    {
	writeInt(seqNum);  // Not received, used for MAC calculation
	rPos = 4;          // Skip it also (i.e. we don't want to read it)
	int bs = 8;
	int macSize = 0;
	if(cipher != null) {
	    bs = cipher.getBlockSize();
	    bs = (bs > 8 ? bs : 8);
	    readNextNFrom(in, bs);
	    cipher.doFinal(data, 4, bs, data, 4); // Skip seqNum
	} else {
	    readNextNFrom(in, 8);
	}
	bs -= 4; // The part of body pre-read above (i.e. subtract length-field)
	pktSize = readInt();

	if(mac != null) {
	    macSize = mac.getMacLength();
	}

	int totPktSz = (pktSize + 4 + macSize);
	if(totPktSz > PACKET_MAX_SIZE || totPktSz < PACKET_MIN_SIZE) {
	    throw new SSH2CorruptPacketException("Invalid packet size: " +
						 pktSize);
	}

	readNextNFrom(in, pktSize - bs); // Allready read bs bytes of body

	if(cipher != null) {
	    cipher.doFinal(data, 8 + bs, pktSize - bs, data, 8 + bs);
	}

	if(mac != null) {
	    readNextNFrom(in, macSize);
	    checkMac(seqNum, mac, macSize);
	}

	padSize = readByte();

	if(compressor != null) {
	    // Update pktSize so getPayloadLength() calculates right value
	    pktSize = compressor.uncompress(this, pktSize - padSize - 1);
	    pktSize += padSize + 1;
	}

	pktType = readByte();
    }

    protected void checkMac(int seqNum, Mac mac, int macSize)
	throws SSH2MacCheckException, ShortBufferException
    {
	mac.update(data, 0, 8 + pktSize);
	mac.doFinal(macTmpBuf, 0);
	int dOff = 8 + pktSize;
	for(int i = 0; i < macSize; i++) {
	    if(macTmpBuf[i] != data[dOff++]) {
		throw new SSH2MacCheckException("MAC check failed");
	    }
	}
    }

    private final void readNextNFrom(InputStream in, int n)
	throws IOException, SSH2EOFException
    {
	if((data.length - wPos) < n) {
	    byte[] tmp   = data;
	    int    newSz = data.length * 2;
	    if(newSz - wPos < n) {
		newSz = wPos + n + (wPos >>> 1);
	    }
	    data = new byte[newSz];
	    System.arraycopy(tmp, 0, data, 0, tmp.length);
	}
	n += wPos;
	while(wPos < n) {
	    int s = in.read(data, wPos, n - wPos);
	    if(s == -1)
		throw new SSH2EOFException("Server closed connection");
	    wPos += s;
	}
    }

    protected void writeTo(OutputStream out, int seqNum,
			   Mac mac, Cipher cipher, SSH2Compressor compressor,
			   SecureRandomAndPad rand)
	throws IOException, ShortBufferException, SSH2CompressionException
    {
	int macSize = 0;
	int bs      = 8;

	if(compressor != null) {
	    compressor.compress(this);
	}

	if(cipher != null) {
	    bs = cipher.getBlockSize();
	    bs = (bs > 8 ? bs : 8);
	}

	// Subtract dummy sequence number since it is not sent
	//
	padSize = bs - ((wPos - 4) % bs);
	if(padSize < 4)
	    padSize += bs;

	// sequence + length fields not counted in packet-length
	//
	pktSize = wPos + padSize - 8;
	rand.nextPadBytes(data, wPos, padSize);

	wPos = 0;
	writeInt(seqNum); // Not transmitted, used for MAC calculation
	writeInt(pktSize);
	writeByte(padSize);
	int totPktSz = pktSize + 4; // packet size including length field

	if(mac != null) {
	    // The MAC is calculated on full packet including sequence number
	    //
	    int macOffset = 4 + totPktSz;
	    mac.update(data, 0, macOffset);
	    mac.doFinal(data, macOffset);
	    macSize = mac.getMacLength();
	}

	if(cipher != null) {
	    cipher.doFinal(data, 4, totPktSz, data, 4);
	}

	out.write(data, 4, totPktSz + macSize);
	out.flush(); // !!! REMOVE(?)
	release();
    }

    public String toString() {
	return "pdu: buf-sz = " + data.length +
	    ", rPos = " + rPos +
	    ", wPos = " + wPos +
	    ", pktSize = " + pktSize +
	    ", padSize = " + padSize +
	    ", pktType = " + pktType;
    }

}
