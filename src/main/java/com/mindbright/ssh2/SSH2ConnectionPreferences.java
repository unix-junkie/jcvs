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

import java.net.Socket;

/**
 * This class is used to control the preferences of the connection
 * layer. Besides controlling pure preferences this class controls socket
 * options on sockets used in port forward channels, this behaviour must be
 * implemented in a subclass though.
 *
 * @see SSH2Connection
 */
public class SSH2ConnectionPreferences {

    public static final int DEFAULT_INIT_WINSZ = 32768;
    public static final int DEFAULT_MAX_PKTSZ  = 8192;

    public static final String DEFAULT_X11_LOCALADDR = "127.0.0.1";
    public static final int    DEFAULT_X11_LOCALPORT = 6000;

    private int initWinSz;
    private int maxPktSz;

    private String x11LocalAddr;
    private int    x11LocalPort;
    private byte[] x11Cookie;

    /**
     * Constructor setting default values to all preferences. The defaults are:
     * 
     * <table border="1">
     * <tr>
     * <th>Preference</th><th>Value</th>
     * </tr>
     * <tr>
     * <td>Initial window size</td><td>32768 bytes</td>
     * </tr>
     * <tr>
     * <td>Max packet size</td><td>8192 bytes</td>
     * </tr>
     * <tr>
     * <td>Local address for X11 display</td><td>127.0.0.1</td>
     * </tr>
     * <tr>
     * <td>Local port for X11 display</td><td>6000</td>
     * </tr>
     * </table>
     */
    public SSH2ConnectionPreferences() {
	this.initWinSz    = DEFAULT_INIT_WINSZ;
	this.maxPktSz     = DEFAULT_MAX_PKTSZ;
	this.x11LocalAddr = DEFAULT_X11_LOCALADDR;
	this.x11LocalPort = DEFAULT_X11_LOCALPORT;
	this.x11Cookie    = null;
    }

    /**
     * Called to set socket options on newly connected port forward channels
     *
     * @param channelType the type of the channel
     * @param s           socket to manipulate
     */
    public void setSocketOptions(int channelType, Socket s) {
	// Do nothing by default, derive this class to change
    }

    /**
     * Sets initial window size for the given channel type. This class doesn't
     * implement different sizes for different types, it must be sub classed to
     * implement this behaviour.
     *
     * @param channelType the type of the channel
     * @param initWinSz   new initial window size
     */
    public void setRxInitWinSz(int channelType, int initWinSz) {
	this.initWinSz = initWinSz;
    }

    /**
     * Gets initial window size for the given channel type. This class doesn't
     * implement different sizes for different types, it must be sub classed to
     * implement this behaviour.
     *
     * @return the initial window size
     */
    public int getRxInitWinSz(int channelType) {
	return initWinSz;
    }

    /**
     * Sets max packet size for the given channel type. This class doesn't
     * implement different sizes for different types, it must be sub classed to
     * implement this behaviour.
     *
     * @param channelType the type of the channel
     * @param maxPktSz    new max packet size
     */
    public void setRxMaxPktSz(int channelType, int maxPktSz) {
	this.maxPktSz = maxPktSz;
    }

    /**
     * Gets max packet size for the given channel type. This class doesn't
     * implement different sizes for different types, it must be sub classed to
     * implement this behaviour.
     *
     * @return the max packet size
     */
    public int getRxMaxPktSz(int channelType) {
	return maxPktSz;
    }

    /**
     * Sets the address for the local X11 display.
     *
     * @param x11LocalAddr the local address to use
     */
    public void setX11LocalAddr(String x11LocalAddr) {
	if(x11LocalAddr != null) {
	    this.x11LocalAddr = x11LocalAddr;
	}
    }

    /**
     * Gets the local address of the X11 display.
     *
     * @return the local address of the X11 display
     */
    public String getX11LocalAddr() {
	return x11LocalAddr;
    }

    /**
     * Sets the port for the local X11 display.
     *
     * @param x11LocalPort the local port to use
     */
    public void setX11LocalPort(int x11LocalPort) {
	if(x11LocalPort > 0) {
	    this.x11LocalPort = x11LocalPort;
	}
    }

    /**
     * Gets the local port of the X11 display.
     *
     * @return the local port of the X11 display
     */
    public int getX11LocalPort() {
	return x11LocalPort;
    }

    /**
     * Sets the X11 authentication cookie as a byte array.
     *
     * @param x11Cookie a byte array containing the authentication cookie
     */
    public void setX11Cookie(byte[] x11Cookie) {
	this.x11Cookie = x11Cookie;
    }

    /**
     * Gets the X11 authentication cookie.
     *
     * @return a byte array containing the X11 authentication cookie
     */
    public byte[] getX11Cookie() {
	return x11Cookie;
    }

}
