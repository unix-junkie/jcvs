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

package com.mindbright.application;

import java.awt.Frame;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import com.mindbright.ssh2.SSH2TerminalAdapter;
import com.mindbright.ssh2.SSH2TerminalAdapterImpl;
import com.mindbright.ssh2.SSH2SessionChannel;
import com.mindbright.ssh2.SSH2Channel;
import com.mindbright.terminal.TerminalWin;
import com.mindbright.terminal.TerminalXTerm;
import com.mindbright.terminal.GlobalClipboard;

public class ModuleTerminal implements MindTermLite.Module {

    public void init(MindTermLite mindterm) {
    }

    public void activate(final MindTermLite mindterm) {
	(new Thread(new Runnable() {
		public void run() {
		    newShell(mindterm);
		}
	    })).start();
    }

    public void newShell(MindTermLite mindterm) {
	Frame frame = new Frame();
	TerminalWin terminal;
	terminal = new TerminalWin(frame, new TerminalXTerm());

	terminal.setClipboard(GlobalClipboard.getClipboardHandler());

	frame.setLayout(new BorderLayout());
	frame.add(terminal.getPanelWithScrollbar(), BorderLayout.CENTER);

	frame.setTitle(mindterm.username + "@" + mindterm.host +
		       (mindterm.port != 22 ? (":" + mindterm.port) : ""));

	frame.pack();
	frame.show();

	SSH2TerminalAdapter termAdapter = new SSH2TerminalAdapterImpl(terminal);

	final SSH2SessionChannel session = mindterm.client.getConnection().newTerminal(termAdapter);

	frame.addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent e)  { 
		session.doExit(0, false);
	    }
	});

	if(session.openStatus() != SSH2Channel.STATUS_OPEN) {
	    System.out.println("** Failed to open session channel");
	    frame.dispose();
	    return;
	}

	mindterm.client.getTransport().getLog().info("MindTerm2", "got X11 forward? " +
				session.requestX11Forward(false, 0));
	mindterm.client.getTransport().getLog().info("MindTerm2", "got pty? " +
				session.requestPTY("xterm",
						   terminal.rows(),
						   terminal.cols(),
						   null));
	mindterm.client.getTransport().getLog().info("MindTerm2", "got shell? " + session.doShell());

	session.waitForExit(0);
	frame.dispose();
    }

    public String getLabel() {
	return "New Terminal";
    }

    public boolean isAvailable(MindTermLite mindterm) {
	return mindterm.isConnected();
    }

    public void connected(MindTermLite mindterm) {
	
    }

}

