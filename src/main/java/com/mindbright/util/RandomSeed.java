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

import java.io.PrintWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Properties;
import java.util.Enumeration;

import java.awt.Component;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;
import java.awt.event.KeyListener;
import java.awt.event.FocusListener;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.FocusEvent;

import com.mindbright.jca.security.MessageDigest;

public final class RandomSeed
    implements MouseMotionListener, MouseListener,
	       KeyListener, FocusListener, ComponentListener
{
    private InputStream  devRand;
    private InputStream  devURand;
    private String       devRandName;
    private String       devURandName;
    private byte[]       entropyPool;
    private int          entropyRIdx;
    private int          entropyWIdx;
    private volatile int entropyCount;

    private boolean haveEntropyGenerator;

    private long tickT;
    private int  evtCnt;
    private int  mouseCnt;
    private int  evtHash;
    private int  keyHash;
    private int  mouseHash;
    private int  lastX;
    private int  lastY;

    private Progress progress;

    private static class Sleeper extends Thread {
	long sleepTime;

	public Sleeper(long sleepTime) {
	    this.sleepTime = sleepTime;
	    this.start();
	}

	public void run() {
	    Thread.yield();
	    try { 
		Thread.sleep(sleepTime);
	    } catch (InterruptedException ex) {
	    }
	}
    }

    public RandomSeed() {
	init();
	// Generate 64 bits of entropy as default
	for(int i = 0; i < 16; i++) {
	    addEntropyBits((byte)spin(8), 4);
	}
    }

    public RandomSeed(String devRandName, String devURandName) {
	init();
	this.devRandName  = devRandName;
	this.devURandName = devURandName;
	this.devRand      = openRandFile(devRandName);
	this.devURand     = openRandFile(devURandName);
    }

    private void init() {
	entropyPool  = new byte[1200];
	entropyCount = 16;
	entropyRIdx  = 0;
	entropyWIdx  = 0;
	tickT        = 0L;
	evtCnt       = 0;
	mouseCnt     = 0;
	devRand      = null;
	devURand     = null;
	progress     = null;
	byte[] sysState = getSystemStateHash();
	System.arraycopy(sysState, 0, entropyPool, 0, sysState.length);
    }

    private InputStream openRandFile(String name) {
	InputStream in = null;
	try {
	    File file = new File(name);
    	    if(file.exists() && file.canRead()) {
		in = new FileInputStream(file);
	    }
	} catch (Throwable t) {
	    in = null;
	}
	return in;
    }

    public void addProgress(Progress progress) {
	this.progress = progress;
    }

    public void removeProgress() {
	this.progress = null;
    }

    // !!! TODO, handle removal of generator
    public void addEntropyGenerator(Component c) {
	c.addComponentListener(this);
	c.addKeyListener(this);
	c.addFocusListener(this);
	c.addMouseMotionListener(this);
	c.addMouseListener(this);
	haveEntropyGenerator = true;
    }

    public void addEntropyBits(byte bits, int count) {
	entropyPool[entropyWIdx++] ^= bits;
	if(entropyWIdx == entropyPool.length) {
	    entropyWIdx = 0;
	}
	entropyCount += count;
	if(progress != null) {
	    progress.progress(entropyCount);
	}
    }

    public boolean haveDevRandom() {
	return (devRand != null);
    }

    public boolean haveDevURandom() {
	return (devURand != null);
    }

    public boolean haveEntropyGenerator() {
	return haveEntropyGenerator;
    }

    public int getAvailableBits() {
	return entropyCount;
    }

    public void resetEntropyCount() {
	entropyCount = 0;
    }

    public byte[] getBytes(int numBytes) {
	if(haveDevURandom()) {
	    try {
		for(int i = 0; i < numBytes; i++) {
		    int bits = devURand.read();
		    addEntropyBits((byte)bits, 8);
		}
	    } catch (IOException e) {
		throw new Error("Error reading '" + devURandName + "'");
	    }
	}

	return getBytesInternal(numBytes);
    }

    public byte[] getBytesBlocking(int numBytes) {
	return getBytesBlocking(numBytes, true);
    }

    public byte[] getBytesBlocking(int numBytes, boolean generatorIfPresent) {
	int bits = (numBytes * 8);
	while(entropyCount < bits) {
	    if(haveDevRandom()) {
		try {
		    int b = devRand.read();
		    addEntropyBits((byte)b, 8);
		} catch (IOException e) {
		    throw new Error("Error reading '" + devRandName + "'");
		}
	    } else if(generatorIfPresent && haveEntropyGenerator) {
		try {
		    Thread.sleep(500);
		} catch (InterruptedException ex) {
		}
	    } else {
		addEntropyBits((byte)spin(8), 4);
	    }
	}

	return getBytesInternal(numBytes);
    }

    private byte[] getBytesInternal(int numBytes) {
	MessageDigest sha1 = null;
	try {
	    sha1 = MessageDigest.getInstance("SHA1");
    	} catch (Exception e) {
	    throw new Error("Error in RandomSeed, no sha1 hash");
	}

	int    curLen = 0;
	byte[] bytes  = new byte[numBytes];
	int    offset = entropyRIdx;
	while(curLen < numBytes) {
	    sha1.update((byte)System.currentTimeMillis());
	    sha1.update(entropyPool, offset, 40); // estimate 4 bits/byte
	    sha1.update((byte)evtCnt);
	    byte[] material = sha1.digest();
	    System.arraycopy(material, 0, bytes, curLen,
			     ((numBytes - curLen > material.length) ?
			      material.length : (numBytes - curLen)));
	    curLen += material.length;
	    offset += 40;
	    offset %= entropyPool.length;
	}

	entropyRIdx  = offset;
	entropyCount -= (numBytes * 8);
	if(entropyCount < 0) {
	    entropyCount = 0;
	}

	return bytes;
    }

    public static byte[] getSystemStateHash() {
	MessageDigest sha1;
	try {
    	    sha1 = MessageDigest.getInstance("SHA1");
    	} catch (Exception e) {
	    throw new Error("Error in RandomSeed, no sha1 hash");
	}

	sha1.update((byte)System.currentTimeMillis());
	sha1.update((byte)Runtime.getRuntime().totalMemory());
	sha1.update((byte)Runtime.getRuntime().freeMemory());
	sha1.update(stackDump(new Throwable()));

	try {
	    Properties  props = System.getProperties();
	    Enumeration names = props.propertyNames();
	    while(names.hasMoreElements()) {
		String name = (String)names.nextElement();
		sha1.update(name.getBytes());
		sha1.update(props.getProperty(name).getBytes());
	    }
	} catch (Throwable t) {
	    sha1.update(stackDump(t));
	}
	sha1.update((byte)System.currentTimeMillis());

	try {
	    sha1.update(InetAddress.getLocalHost().toString().getBytes());
	} catch (Throwable t) {
	    sha1.update(stackDump(t));
	}
	sha1.update((byte)System.currentTimeMillis());

	Runtime.getRuntime().gc();
	sha1.update((byte)Runtime.getRuntime().freeMemory());
	sha1.update((byte)System.currentTimeMillis());

	return sha1.digest();
    }

    public static int spin(long t) {
        int counter = 0;
        Sleeper s = new Sleeper(t);
        do {
            counter++;
            Thread.yield();
        } while(s.isAlive());
        return counter;
    }

    public void keyPressed(KeyEvent e) {
	keyHash ^= e.getModifiers();
	keyHash += (e.getKeyCode() ^ evtHash);
	eventTick(e);
    }

    public void keyReleased(KeyEvent e) {
	eventTick(e);
    }

    public void keyTyped(KeyEvent e) {
	keyHash ^= e.getKeyChar();
	keyHash ^= e.hashCode();
	if((evtCnt % 7) == 0) {
	    addEntropyBits((byte)keyHash, 4);
	}
    }

    public void componentHidden(ComponentEvent e) {
	eventTick(e);
    }

    public void componentMoved(ComponentEvent e) {
	eventTick(e);
    }

    public void componentResized(ComponentEvent e) {
	eventTick(e);
    }

    public void componentShown(ComponentEvent e) {
	eventTick(e);
    }

    public void focusGained(FocusEvent e) {
	eventTick(e);
    }

    public void focusLost(FocusEvent e) {
	eventTick(e);
    }

    public void mouseClicked(MouseEvent e) {
	eventTick(e);
    }

    public void mouseEntered(MouseEvent e) {
	eventTick(e);
    }

    public void mouseExited(MouseEvent e) {
	eventTick(e);
    }

    public void mousePressed(MouseEvent e) {
	eventTick(e);
    }

    public void mouseReleased(MouseEvent e) {
	eventTick(e);
    }

    public void mouseDragged(MouseEvent e) {
	mouseMoved(e);
    }

    public void mouseMoved(MouseEvent e) {
	mouseCnt++;
	int dX = lastX - e.getX();
	int dY = lastY - e.getY();
	lastX  = e.getX();
	lastY  = e.getY();

	mouseHash ^= e.hashCode();
	mouseHash ^= e.getX();
	mouseHash ^= dY;
	mouseHash ^= e.getY();
	mouseHash ^= dX;

	if((mouseCnt % 3) == 0) {
	    addEntropyBits((byte)mouseHash, 4);
	}
    }

    public static byte[] stackDump(Throwable t) {
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	PrintWriter pw = new PrintWriter(baos);
	t.printStackTrace(pw);
	return baos.toByteArray();
    }

    private void eventTick(Object o) {
	evtCnt++;
	evtHash ^= o.hashCode();
	if((evtCnt % 5) == 0) {
	    long now = System.currentTimeMillis();
	    addEntropyBits((byte)(now - tickT), 4);
	    tickT = now;
	    evtHash ^= now;
	}
	if((evtCnt % 17) == 0) {
	    addEntropyBits((byte)evtHash, 4);
	}
    }

    /* !!! REMOVE DEBUG
    public static void main(String[] argv) {
	RandomSeed seed = new RandomSeed();
	java.awt.Frame frame = new java.awt.Frame();
	com.mindbright.terminal.TerminalWin terminal =
	    new com.mindbright.terminal.TerminalWin(frame,
	    new com.mindbright.terminal.TerminalXTerm());

	seed.addEntropyGenerator(terminal);

	frame.add(terminal.getPanelWithScrollbar(), java.awt.BorderLayout.CENTER);
	frame.pack();
	frame.show();
    }
    */

}
