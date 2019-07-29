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

public final class Queue {

    final static int QUEUE_DEPTH   = 256;
    final static int QUEUE_HIWATER = 192;

    Object[]   queue;
    boolean    isWaitGet;
    boolean    isWaitPut;
    boolean    isBlocking;
    int        rOffset;
    int        wOffset;
    int        maxQueueDepth;

    // Copies used for saving real values when disabling queue
    //
    int        rOffsetCP;
    int        wOffsetCP;
    int        maxQueueDepthCP;

    public Queue() {
	this.queue      = new Object[QUEUE_DEPTH + 1];
	this.isWaitGet  = false;
	this.isWaitPut  = false;
	this.isBlocking = true;
	this.rOffset    = 0;
	this.wOffset    = 0;
	this.maxQueueDepth = QUEUE_DEPTH;
    }

    public synchronized void setMaxDepth(int maxDepth) {
	maxQueueDepth = maxDepth;
    }

    public synchronized void putLast(Object obj) {
	putFlowControl();
	queue[wOffset++] = obj;
	if(wOffset == (QUEUE_DEPTH + 1))
	    wOffset = 0;
	if(isWaitGet)
	    this.notify();
    }

    public synchronized void putFirst(Object obj) {
	putFlowControl();
	rOffset--;
	if(rOffset == -1)
	    rOffset = QUEUE_DEPTH;
	queue[rOffset] = obj;
	if(isWaitGet)
	    this.notify();
    }

    public synchronized void release() {
	if(isWaitGet)
	    this.notify();
    }

    public synchronized void disable() {
	rOffsetCP       = rOffset;
	wOffsetCP       = wOffset;
	maxQueueDepthCP = maxQueueDepth;
	rOffset         = 0;
	wOffset         = 0;
	maxQueueDepth   = 0;
    }

    public synchronized void enable() {
	rOffset       = rOffsetCP;
	wOffset       = wOffsetCP;
	maxQueueDepth = maxQueueDepthCP;
	if(!isEmpty()) {
	    this.release();
	}
	if(isWaitPut && (freeSpace() > (QUEUE_DEPTH - QUEUE_HIWATER))) {
	    this.notifyAll();
	    isWaitPut = false;
	}
    }

    public synchronized boolean isBlocked() {
	return isWaitGet;
    }

    public void waitUntilBlocked() {
    blockLoop:
	while(!isBlocked()) {
	    try {
		Thread.sleep(100);
	    } catch (InterruptedException e) {
		break blockLoop;
	    }
	}
    }

    public synchronized void setBlocking(boolean block) {
	isBlocking = block;
	release();
    }

    public synchronized boolean isEmpty() {
	return (rOffset == wOffset);
    }

    private final void putFlowControl() {
	int fs = freeSpace();
	if(fs == (QUEUE_DEPTH - maxQueueDepth)) {
	    isWaitPut = true;
	}
	if(isWaitPut) {
	    try {
		this.wait();
	    } catch (InterruptedException e) {
		// !!!
	    }
	}
    }

    private final int freeSpace() {
	int fSpc = rOffset - wOffset;
	if(fSpc <= 0)
	    fSpc += (QUEUE_DEPTH + 1);
	fSpc--;
	return fSpc;
    }

    public synchronized Object getFirst() {
	Object obj = null;
	while(isEmpty()) {
	    if(!isBlocking) {
		return null;
	    }
	    isWaitGet = true;
	    try {
		this.wait();
	    } catch (InterruptedException e) {
		// !!!
	    }
	}
	isWaitGet = false;
	obj = queue[rOffset];
	queue[rOffset++] = null;
	if(rOffset == (QUEUE_DEPTH + 1))
	    rOffset = 0;
	if(isWaitPut && (freeSpace() > (QUEUE_DEPTH - QUEUE_HIWATER))) {
	    this.notifyAll();
	    isWaitPut = false;
	}
	return obj;
    }
}
