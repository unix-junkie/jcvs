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

import java.io.OutputStream;
import java.io.IOException;

import com.mindbright.terminal.TerminalWin;
import com.mindbright.terminal.TerminalInputListener;

public class SSH2TerminalAdapterImpl implements SSH2TerminalAdapter,
						TerminalInputListener {
    TerminalWin        terminal;
    SSH2SessionChannel session;
    TerminalOutStream  stdout;

    final class TerminalOutStream extends OutputStream {
	public void write(int b) throws IOException {
	    terminal.write((char)b);
	}
	public void write(byte b[], int off, int len) throws IOException {
	    terminal.write(b, off, len);
	}
    }

    public SSH2TerminalAdapterImpl(TerminalWin terminal) {
	this.terminal = terminal;
	this.stdout   = new TerminalOutStream();
    }

    public void attach(SSH2SessionChannel session) {
	this.session = session;
	session.changeStdOut(this.stdout);
	terminal.addInputListener(this);
    }

    public void detach() {
	if(terminal != null) {
	    terminal.removeInputListener(this);
	}
	// !!! TODO want to do this ?
	// session.changeStdOut(
    }

    public void typedChar(char c) {
	try {
	    session.stdin.write((int)c);
	} catch (IOException e) {
	    session.connection.getLog().error("SSH2TerminalAdapterImpl",
					      "typedChar",
					      "error writing to stdin: " +
					      e.getMessage());
	}
    }

    public void sendBytes(byte[] b) {
	try {
	    session.stdin.write(b, 0, b.length);
	} catch (IOException e) {
	    session.connection.getLog().error("SSH2TerminalAdapterImpl",
					      "sendBytes",
					      "error writing to stdin: " +
					      e.getMessage());
	}
    }

    public void signalWindowChanged(int rows, int cols,
				    int vpixels, int hpixels) {
	session.sendWindowChange(rows, cols);
    }

}
