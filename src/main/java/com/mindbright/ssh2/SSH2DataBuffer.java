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

import java.math.BigInteger;

/**
 * This class implements a read/write buffer with all protocol specific
 * formatting (as defined in the architecture spec.). It is mainly used in the
 * form of a protocol data unit (derived class <code>SSH2TransportPDU</code>).
 */
public class SSH2DataBuffer {
    public final static int BOOLEAN_TRUE  = 1;
    public final static int BOOLEAN_FALSE = 0;

    protected byte[] data;
    protected int    rPos;
    protected int    wPos;

    protected SSH2DataBuffer() {
	this(0);
    }

    public SSH2DataBuffer(int bufSize) {
	this.data = new byte[bufSize];
	reset();
    }

    public final void reset() {
	this.rPos = 0;
	this.wPos = 0;
    }

    public final byte[] getData() {
	return data;
    }

    public final void setData(byte[] data) {
	this.data = data;
    }

    public final void setWPos(int wPos) {
	this.wPos = wPos;
    }

    public final int getWPos() {
	return wPos;
    }

    public final void setRPos(int rPos) {
	this.rPos = rPos;
    }

    public final int getRPos() {
	return rPos;
    }

    public final int getMaxReadSize() {
	return wPos - rPos;
    }

    public final int getMaxWriteSize() {
	return data.length - wPos;
    }

    public final int readByte() {
	return ((int)data[rPos++]) & 0xff;
    }

    public final void writeByte(int b) {
	data[wPos++] = (byte)b;
    }

    public final boolean readBoolean() {
	if(readByte() != BOOLEAN_FALSE)
	    return true;
	return false;
    }

    public final void writeBoolean(boolean b) {
	if(b) {
	    writeByte(BOOLEAN_TRUE);
	} else {
	    writeByte(BOOLEAN_FALSE);
	}
    }

    public final int readInt() {
	int b1 = readByte();
	int b2 = readByte();
	int b3 = readByte();
	int b4 = readByte();
	return ((b1 << 24) + (b2 << 16) + (b3 << 8) + (b4 << 0));
    }

    public final void writeInt(int i) {
	writeByte((i >>> 24) & 0xFF);
	writeByte((i >>> 16) & 0xFF);
	writeByte((i >>>  8) & 0xFF);
	writeByte((i >>>  0) & 0xFF);
    }

    public final BigInteger readBigInt() {
	byte[] raw = readString();
	if(raw.length > 0)
	    return new BigInteger(raw);
	return BigInteger.valueOf(0);
    }

    public final BigInteger readBigIntBits() {
	int    bits = readInt();
	byte[] raw  = new byte[(bits + 7) / 8 + 1];

	raw[0] = 0;
	readRaw(raw, 1, raw.length - 1);
	return new BigInteger(raw);
    }

    public final void writeBigInt(BigInteger bi) {
	byte[] raw = bi.toByteArray();
	if(raw.length == 1 && raw[0] == (byte)0x00)
	    raw = new byte[0];
	writeString(raw);
    }

    public final void writeBigIntBits(BigInteger bi) {
        int    bytes = ((bi.bitLength() + 7) / 8);
	byte[] raw   = bi.toByteArray();
	if(raw.length == 1 && raw[0] == (byte)0x00) {
	    writeInt(0);
	    return;
	}
	writeInt(bi.bitLength());
	if(raw[0] == 0) {
	    writeRaw(raw, 1, bytes);
	} else {
	    writeRaw(raw, 0, bytes);
	}
    }

    public final String readJavaString() {
	return new String(readString());
    }

    public final byte[] readString() {
	int len = readInt();
	if(len < 0 || len > (data.length - rPos)) {
	    throw new Error("Error in SSH2DataBuffer, corrupt string on read");
	}
	byte[] str = new byte[len];
	System.arraycopy(data, rPos, str, 0, len);
	rPos += len;
	return str;
    }

    public final int readString(byte[] str, int off) {
	int len = readInt();
	System.arraycopy(data, rPos, str, off, len);
	rPos += len;
	return len;
    }

    public final void writeString(String str) {
	writeString(str.getBytes());
    }

    public final void writeString(byte[] str) {
	writeString(str, 0, str.length);
    }

    public final void writeString(byte[] str, int off, int len) {
	writeInt(len);
	System.arraycopy(str, off, data, wPos, len);
	wPos += len;
    }

    public final byte[] readRestRaw() {
	return readRaw(wPos - rPos);
    }

    public final byte[] readRaw(int len) {
	byte[] raw = new byte[len];
	readRaw(raw, 0, len);
	return raw;
    }

    public final void readRaw(byte[] raw, int off, int len) {
	System.arraycopy(data, rPos, raw, off, len);
	rPos += len;
    }

    public final void writeRaw(byte[] raw) {
	writeRaw(raw, 0, raw.length);
    }

    public final void writeRaw(byte[] raw, int off, int len) {
	System.arraycopy(raw, off, data, wPos, len);
	wPos += len;
    }

}
