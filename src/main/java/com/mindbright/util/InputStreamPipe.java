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

package com.mindbright.util;

import java.io.InputStream;
import java.io.IOException;

public final class InputStreamPipe extends InputStream {

    private static final int DEFAULT_BUFFER_SIZE = 8192;
    
    private OutputStreamPipe source;
    private byte[]           circBuf;
    private int              rOffset;
    private int              wOffset;
    private boolean          isWaitGet;
    private boolean          isWaitPut;
    private boolean          eof;
    private boolean          closed;

    public InputStreamPipe(int bufferSize) {
	this.circBuf    = new byte[bufferSize];
    	this.isWaitGet  = false;
	this.isWaitPut  = false;
	this.rOffset    = 0;
	this.wOffset    = 0;
    }

    public InputStreamPipe() {
	this(DEFAULT_BUFFER_SIZE);
    }

    public InputStreamPipe(OutputStreamPipe source) throws IOException {
	this();
	connect(source);
    }

    public void connect(OutputStreamPipe source) throws IOException {
	if(this.source == source) {
	    return;
	}
	if(this.source != null) {
	    throw new IOException("Pipe already connected");
	}
	this.source = source;
	source.connect(this);
    }

    public synchronized int read() throws IOException {
	while(isEmpty()) {
	    if(closed) {
		throw new IOException("InputStreamPipe closed");
	    }
	    if(eof) {
		return -1;
	    }
	    isWaitGet = true;
	    try {
		this.wait();
	    } catch (InterruptedException e) {
		// !!!
	    }
	}
	isWaitGet = false;

	int b = (circBuf[rOffset++] & 0xff);

	if(rOffset == circBuf.length)
	    rOffset = 0;

	if(isWaitPut) {
	    this.notifyAll();
	    isWaitPut = false;
	}

	return b;
    }

    public synchronized int read(byte[] buf, int off, int len)
	throws IOException
    {
	while(isEmpty()) {
	    if(closed) {
		throw new IOException("InputStreamPipe closed");
	    }
	    if(eof) {
		return -1;
	    }
	    isWaitGet = true;
	    try {
		this.wait();
	    } catch (InterruptedException e) {
		// !!!
	    }
	}
	isWaitGet = false;

	int n = available();
	n = (n > len ? len : n);

	if(rOffset < wOffset) {
	    System.arraycopy(circBuf, rOffset, buf, off, n);
	} else {
	    int rest = circBuf.length - rOffset;
	    if(rest < n) {
		System.arraycopy(circBuf, rOffset, buf, off, rest);
		System.arraycopy(circBuf, 0, buf, off + rest, n - rest);
	    } else {
		System.arraycopy(circBuf, rOffset, buf, off, n);
	    }
	}

	rOffset += n;
	if(rOffset >= circBuf.length)
	    rOffset -= circBuf.length;

	if(isWaitPut) {
	    this.notifyAll();
	    isWaitPut = false;
	}

	return n;
    }

    public int available() {
	return circBuf.length - freeSpace() - 1;
    }

    public synchronized void close() throws IOException {
	closed = true;
	this.notifyAll();
    }

    public synchronized void flush() {
	this.notifyAll();
    }

    protected synchronized void put(int b) throws IOException {
	putFlowControl();
	circBuf[wOffset++] = (byte)b;
	if(wOffset == circBuf.length)
	    wOffset = 0;
	if(isWaitGet)
	    this.notify();
    }

    protected synchronized void put(byte[] buf, int off, int len)
	throws IOException
    {
	while(len > 0) {
	    putFlowControl();
	    int n = freeSpace();
	    n = (n > len ? len : n);

	    if(wOffset < rOffset) {
		System.arraycopy(buf, off, circBuf, wOffset, n);
	    } else {
		int rest = circBuf.length - wOffset;
		if(rest < n) {
		    System.arraycopy(buf, off, circBuf, wOffset, rest);
		    System.arraycopy(buf, off + rest, circBuf, 0, n - rest);
		} else {
		    System.arraycopy(buf, off, circBuf, wOffset, n);
		}
	    }

	    wOffset += n;
	    if(wOffset >= circBuf.length) {
		wOffset -= circBuf.length;
	    }
	    len -= n;
	    off += n;

	    if(isWaitGet)
		this.notify();
	}
    }

    protected synchronized void eof() {
	eof = true;
	this.notify();
    }

    private int freeSpace() {
	int fSpc = rOffset - wOffset;
	if(fSpc <= 0)
	    fSpc += circBuf.length;
	fSpc--;
	return fSpc;
    }

    private synchronized boolean isEmpty() {
	return (rOffset == wOffset) || closed;
    }

    private void putFlowControl() throws IOException {
	if(eof) {
	    throw new IOException("InputStreamPipe already got eof");
	}
	if(closed) {
	    throw new IOException("InputStreamPipe closed");
	}
	int fs = freeSpace();
	isWaitPut = (fs == 0);
	if(isWaitPut) {
	    try {
		this.wait();
	    } catch (InterruptedException e) {
		// !!!
	    }
	}
    }

    /* DEBUG/Test
    public static void main(String[] argv) {
	try {
	    final InputStreamPipe  in  = new InputStreamPipe();
	    final OutputStreamPipe out = new OutputStreamPipe(in);
	    //final java.io.PipedInputStream  in  = new java.io.PipedInputStream();
	    //final java.io.PipedOutputStream out = new java.io.PipedOutputStream(in);

	    final byte[] msg = new byte[4711];
	    for(int i = 0; i < 4711; i++) {
		msg[i] = (byte)((i * i) ^ (i + i));
	    }
	    Thread w = new Thread(new Runnable() {
		    public void run() {
			try {
			    for(int i = 0; i < 1000; i++) {
				out.write(msg);
			    }
			} catch (IOException e) {
			    System.out.println("Error in w: " + e);
			    e.printStackTrace();
			}
		    }
		});
	    Thread r = new Thread(new Runnable() {
		    public void run() {
			try {
			    byte[] imsg = new byte[4711];
			    for(int i = 0; i < 1000; i++) {
				int l = 4711;
				int o = 0;
				while(o < 4711) {
				    int n = in.read(imsg, o, l);
				    o += n;
				    l -= n;
				}
			    }
			} catch (IOException e) {
			    System.out.println("Error in w: " + e);
			    e.printStackTrace();
			}
		    }
		});
	    long start = System.currentTimeMillis();
	    System.out.println("Start: " + (start / 1000));
	    w.start();
	    r.start();
	    r.join();
	    long now = System.currentTimeMillis();
	    System.out.println("End: " + (now / 1000));
	    System.out.println("Lapsed: " + (now - start));

	} catch (Exception e) {
	    System.out.println("Error: " + e);
	    e.printStackTrace();
	}
    }
    */

}
