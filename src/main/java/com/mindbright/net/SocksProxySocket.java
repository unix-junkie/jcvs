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

package com.mindbright.net;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.SocketException;

public class SocksProxySocket extends Socket {

    private final static String[] replyErrorV5 = {
	"Success",
	"General SOCKS server failure",
	"Connection not allowed by ruleset",
	"Network unreachable",
	"Host unreachable",
	"Connection refused",
	"TTL expired",
	"Command not supported",
	"Address type not supported"
    };

    private final static String[] replyErrorV4 = {
        "Request rejected or failed",
        "SOCKS server cannot connect to identd on the client",
        "The client program and identd report different user-ids"
    };

    private String proxyHost;
    private int    proxyPort;
    private String targetHost;
    private int    targetPort;

    String     serverDesc;

    public String getServerDesc() {
	return serverDesc;
    }

    private SocksProxySocket(String targetHost, int targetPort,
			     String proxyHost, int proxyPort)
	throws IOException, UnknownHostException
    {
	super(proxyHost, proxyPort);

	this.proxyHost  = proxyHost;
	this.proxyPort  = proxyPort;
	this.targetHost = targetHost;
	this.targetPort = targetPort;
    }

    public static SocksProxySocket getSocks4Proxy(String targetHost,
						  int targetPort,
						  String proxyHost,
						  int proxyPort, String userId)
	throws IOException, UnknownHostException
    {
	SocksProxySocket proxySocket =
	    new SocksProxySocket(targetHost, targetPort, proxyHost, proxyPort);

	try {
	    InputStream  proxyIn  = proxySocket.getInputStream();
	    OutputStream proxyOut = proxySocket.getOutputStream();
	    InetAddress  hostAddr = InetAddress.getByName(targetHost);

	    proxyOut.write(0x04); // V4
	    proxyOut.write(0x01); // CONNECT
	    proxyOut.write((targetPort >>> 8) & 0xff);
	    proxyOut.write(targetPort & 0xff);
	    proxyOut.write(hostAddr.getAddress());
	    proxyOut.write(userId.getBytes());
	    proxyOut.write(0x00); // NUL terminate userid string
	    proxyOut.flush();

	    
	    int res = proxyIn.read();
	    if(res == -1) {
		throw new IOException("SOCKS4 server " + proxyHost + ":" + proxyPort +
				      " disconnected");
	    }
	    if(res != 0x00)
		throw new IOException("Invalid response from SOCKS4 server (" + res + ") " +
				      proxyHost + ":" + proxyPort);

	    int code = proxyIn.read();
	    if(code != 90) {
		if(code > 90 && code < 93)
		    throw new IOException("SOCKS4 server unable to connect, reason: " +
					  replyErrorV4[code - 91]);
		else
		    throw new IOException("SOCKS4 server unable to connect, reason: " +
					  code);
	    }

	    byte[] data = new byte[6];

	    if(proxyIn.read(data, 0, 6) != 6)
		throw new IOException("SOCKS4 error reading destination address/port");

	    proxySocket.serverDesc = data[2]  + "." + data[3] +  "." + data[4] +
		"." + data[5] + ":" + ((data[0] << 8) | data[1]);

	} catch (SocketException e) {
	    throw new SocketException("Error communicating with SOCKS4 server " +
				      proxyHost + ":" + proxyPort + ", " +
				      e.getMessage());
	}

	return proxySocket;
    }

    public static SocksProxySocket getSocks5Proxy(String targetHost,
						  int targetPort,
						  String proxyHost,
						  int proxyPort,
						  boolean localLookup,
						  ProxyAuthenticator authenticator)
	throws IOException, UnknownHostException
    {
	SocksProxySocket proxySocket =
	    new SocksProxySocket(targetHost, targetPort, proxyHost, proxyPort);

	try {
	    InputStream  proxyIn  = proxySocket.getInputStream();
	    OutputStream proxyOut = proxySocket.getOutputStream();

	    // Simplest form, only no-auth and cleartext username/password
	    //
	    byte[] request = { (byte) 0x05, (byte) 0x02, (byte) 0x00, (byte) 0x02 };
	    byte[] reply  = new byte[2];

	    proxyOut.write(request);
	    proxyOut.flush();

	    int res = proxyIn.read();
	    if(res == -1) {
		throw new IOException("SOCKS5 server " + proxyHost + ":" + proxyPort +
				      " disconnected");
	    }
	    if(res != 0x05) {
		throw new IOException("Invalid response from SOCKS5 server (" + res + ") " +
				      proxyHost + ":" + proxyPort);
	    }
	    
	    int method = proxyIn.read();
	    switch(method) {
	    case 0x00:
		break;
	    case 0x02:
		doAuthentication(proxyIn, proxyOut, authenticator, proxyHost, proxyPort);
		break;
	    default:
		throw new IOException("SOCKS5 server does not support our authentication methods");
	    }

	    if(localLookup) {
		// Request connect to targetHost (as 'ip-number') : targetPort
		//
		InetAddress hostAddr;
		try {
		    hostAddr = InetAddress.getByName(targetHost);
		} catch (UnknownHostException e) {
		    throw new IOException("Can't do local lookup on: " +
					  targetHost +
					  ", try socks5 without local lookup");
		}
		request = new byte[] { (byte) 0x05, (byte) 0x01, (byte) 0x00, (byte) 0x01 };
		proxyOut.write(request);
		proxyOut.write(hostAddr.getAddress());
	    } else {
		// Request connect to targetHost (as 'domain-name') : targetPort
		//
		request = new byte[] { (byte) 0x05, (byte) 0x01, (byte) 0x00, (byte) 0x03 };
		proxyOut.write(request);
		proxyOut.write(targetHost.length());
		proxyOut.write(targetHost.getBytes());
	    }
	    proxyOut.write((targetPort >>> 8) & 0xff);
	    proxyOut.write(targetPort & 0xff);
	    proxyOut.flush();

	    res = proxyIn.read();
	    if(res != 0x05)
		throw new IOException("Invalid response from SOCKS5 server (" + res + ") " +
				      proxyHost + ":" + proxyPort);

	    int status = proxyIn.read();
	    if(status != 0x00) {
		if(status > 0 && status < 9)
		    throw new IOException("SOCKS5 server unable to connect, reason: " + 
					  replyErrorV5[status]);
		else
		    throw new IOException("SOCKS5 server unable to connect, reason: " + status);
	    }

	    proxyIn.read(); // 0x00 RSV

	    int aType = proxyIn.read();
	    byte[] data = new byte[255];
	    switch(aType) {
	    case 0x01:
		if(proxyIn.read(data, 0, 4) != 4)
		    throw new IOException("SOCKS5 error reading address");
		proxySocket.serverDesc = data[0]  + "." + data[1] +  "." + data[2] +
		    "." + data[3];
		break;
	    case 0x03:
		int n = proxyIn.read();
		if(proxyIn.read(data, 0, n) != n)
		    throw new IOException("SOCKS5 error reading address");
		proxySocket.serverDesc = new String(data);
		break;
	    default:
		throw new IOException("SOCKS5 gave unsupported address type: " + aType);
	    }

	    if(proxyIn.read(data, 0, 2) != 2)
		throw new IOException("SOCKS5 error reading port");
	    proxySocket.serverDesc += ":" + ((data[0] << 8) | data[1]);

	} catch (SocketException e) {
	    throw new SocketException("Error communicating with SOCKS5 server " +
				      proxyHost + ":" + proxyPort + ", " +
				      e.getMessage());
	}

	return proxySocket;
    }

    static void doAuthentication(InputStream proxyIn, OutputStream proxyOut,
				 ProxyAuthenticator authenticator,
				 String proxyHost, int proxyPort)
	throws IOException
    {
	String username = authenticator.getProxyUsername("SOCKS5", null);
	String password = authenticator.getProxyPassword("SOCKS5", null);

	proxyOut.write(0x01);
	proxyOut.write(username.length());
	proxyOut.write(username.getBytes());
	proxyOut.write(password.length());
	proxyOut.write(password.getBytes());

	int res = proxyIn.read();

	/*
	 * We accept both 0x05 and 0x01 as version in the response here. 0x01
	 * is the right response but some buggy servers will respond with 0x05
	 * (i.e. not complying with rfc1929).
	 */
	if(res != 0x01 && res != 0x05)
	    throw new IOException("Invalid response from SOCKS5 server (" + res + ") " +
				  proxyHost + ":" + proxyPort);

	if(proxyIn.read() != 0x00)
	    throw new IOException("Invalid username/password for SOCKS5 server");
    }

    public String toString() {
	return "SocksProxySocket[addr=" + getInetAddress() +
	    ",port=" + getPort() +
	    ",localport=" + getLocalPort() + "]";
    }

}
