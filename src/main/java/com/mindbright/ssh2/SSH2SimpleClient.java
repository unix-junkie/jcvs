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
import java.util.Properties;

import com.mindbright.sshcommon.SSHConsoleRemote;

import com.mindbright.util.SecureRandomAndPad;

/**
 * This class implements the most basic variant of a ssh2 client. It creates the
 * transport, userauth, and connection layers (i.e. instances of
 * <code>SSH2Transport</code>, <code>SSH2UserAuth</code>, and
 * <code>SSH2Connection</code>). The only thing which needs to be provided is a
 * connected socket to the server, user authentication data, and preferences for
 * the transport layer (provided as ordinary <code>java.util.Properties</code>
 * for convenience). The constructor is active in that it does all the required
 * work to set up the complete protocol stack, hence it can throw exceptions
 * which can occur.
 * <p>
 * This simple client can easily be used as the basis for example to build
 * tunneling capabilities into any java app. requiring secure connections. For
 * doing remote command execution and/or controlling input/output of a command
 * or shell the class <code>SSH2ConsoleRemote</code> can be used to have easy
 * access to command execution and/or input/output as
 * <code>java.io.InputStream</code> and <code>java.io.OutpuStream</code>
 *
 * @see SSH2Transport
 * @see SSH2Connection
 * @see SSH2ConsoleRemote
 */
public class SSH2SimpleClient {

    protected SSH2Transport  transport;
    protected SSH2Connection connection;
    protected SSH2UserAuth   userAuth;

    /**
     * Simple constructor to use when password authentication is sufficient.
     *
     * @param socket   connected socket to ssh2 server
     * @param rand     source of randomness for padding and keys
     * @param username name of user
     * @param password password of user
     * @param prefs    preferences for transport layer
     *
     * @see SSH2TransportPreferences
     */
    public SSH2SimpleClient(Socket socket, SecureRandomAndPad rand,
			    String username, String password,
			    Properties prefs)
	throws SSH2Exception
    {
	this(socket, rand,
	     username, "password", new SSH2AuthPassword(password),
	     prefs);
    }

    /**
     * Constructor to use when other authentication than password is needed.
     *
     * @param socket     connected socket to ssh2 server
     * @param rand       source of randomness for padding and keys
     * @param username   name of user
     * @param authType   type of authentication (e.g. publickey)
     * @param authModule authentication module (e.g. <code>SSH2AuthPublicKey</code>)
     * @param prefs      preferences for transport layer
     *
     * @see SSH2TransportPreferences
     */
    public SSH2SimpleClient(Socket socket, SecureRandomAndPad rand,
			    String username,
			    String authType, SSH2AuthModule authModule,
			    Properties prefs)
	throws SSH2Exception
    {
	transport = new SSH2Transport(socket,
				      new SSH2TransportPreferences(prefs),
				      rand);
	transport.boot();

	if(!transport.waitForKEXComplete()) {
	    throw new SSH2FatalException("KEX failed");
	}

	SSH2Authenticator authenticator = new SSH2Authenticator(username);

	authenticator.addModule(authType, authModule);

	userAuth = new SSH2UserAuth(transport, authenticator);
	if(!userAuth.authenticateUser("ssh-connection")) {
	    throw new SSH2FatalException("Permission denied");
	}

	connection = new SSH2Connection(userAuth, transport);
	transport.setConnection(connection);
    }

    public SSH2Transport getTransport() {
	return transport;
    }

    public SSH2Connection getConnection() {
	return connection;
    }

}
