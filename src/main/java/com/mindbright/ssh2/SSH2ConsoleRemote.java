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
import java.io.File;

import com.mindbright.sshcommon.SSHConsoleRemote;

/**
 * This class implements a console to a remote command or shell. The underlying
 * mechanism is a session channel which is created on demand from the provided
 * connection layer. It can be used to execute single commands and/or control
 * input/output to/from a shell. It is for example extended to control an SCP1
 * client in the class <code>SSH2SCP1Client</code>.
 * <p>
 * To create a <code>SSH2ConsoleRemote</code> instance a complete connected ssh2
 * stack is needed from which one provides the <code>SSH2Connection</code> to
 * the constructor.
 *
 * @see SSH2Connection
 * @see SSH2SimpleClient
 */
public class SSH2ConsoleRemote implements SSHConsoleRemote {

    protected SSH2Connection     connection;
    protected SSH2SessionChannel session;
    protected OutputStream       stderr;

    /**
     * Basic constrctor.
     *
     * @param connection connected connection layer
     */
    public SSH2ConsoleRemote(SSH2Connection connection) {
	this(connection, null);
    }

    /**
     * Constrctor which takes an extra argument for specifying a stderr stream.
     *
     * @param connection connected connection layer
     * @param stderr     output stream where stder should be sent
     */
    public SSH2ConsoleRemote(SSH2Connection connection, OutputStream stderr) {
	this.connection = connection;
	this.stderr     = stderr;
    }

    /**
     * Runs single command on server.
     *
     * @param command command line to run
     *
     * @return a boolean indicating success or failure
     */
    public boolean command(String command) {
	session = connection.newSession();
	if(stderr != null) {
	    session.changeStdErr(stderr);
	}
	return session.doSingleCommand(command);
    }

    /**
     * Starts an interactive shell on the server
     *
     * @return a boolean indicating success or failure
     */
    public boolean connect() {
	session = connection.newSession();
	return session.doShell();
    }

    /**
     * Closes the session channel.
     */
    public void close() {
	session.close();
	session = null;
    }

    /**
     * Changes the output stream where stdout is written to in the underlying
     * session channel.
     *
     * @param out new stdout stream
     */
    public void changeStdOut(OutputStream out) {
	session.changeStdOut(out);
    }

    /**
     * Gets the stdout stream of the underlying session channel. Note, this is
     * an input stream since one wants to read from stdout.
     *
     * @return the input stream of stdout stream
     */
    public InputStream getStdOut() {
	return session.getStdOut();
    }

    /**
     * Gets the stdin stream of the underlying session channel. Note, this is
     * an output stream since one wants to write to stdin.
     *
     * @return the input stream of stdout stream
     */
    public OutputStream getStdIn() {
	return session.getStdIn();
    }

}
