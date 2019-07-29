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

import java.io.IOException;

import java.net.Socket;

import java.applet.Applet;

import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;

import com.mindbright.jca.security.SecureRandom;

import com.mindbright.ssh2.SSH2;
import com.mindbright.ssh2.SSH2Exception;
import com.mindbright.ssh2.SSH2FatalException;
import com.mindbright.ssh2.SSH2Transport;
import com.mindbright.ssh2.SSH2Connection;
import com.mindbright.ssh2.SSH2Channel;
import com.mindbright.ssh2.SSH2SessionChannel;
import com.mindbright.ssh2.SSH2TransportPreferences;
//import com.mindbright.ssh2.SSH2TerminalAdapter;
//import com.mindbright.ssh2.SSH2TerminalAdapterImpl;
import com.mindbright.ssh2.SSH2Authenticator;
import com.mindbright.ssh2.SSH2AuthPassword;
import com.mindbright.ssh2.SSH2AuthPublicKey;
import com.mindbright.ssh2.SSH2UserAuth;
//import com.mindbright.ssh2.SSH2SimpleSFTPShell;

//import com.mindbright.terminal.TerminalWin;
//import com.mindbright.terminal.TerminalXTerm;
//import com.mindbright.terminal.GlobalClipboard;

import com.mindbright.util.SecureRandomAndPad;
import com.mindbright.util.RandomSeed;

public class SSH2Tunneling extends Applet {

    public static final boolean expires  = false;
    public static final long validFrom = 970207221384L; // 
    public static final long validTime = (33L * 24L * 60L * 60L * 1000L);

    Container container;
    Frame     frame;

    String host;
    int    port;
    String username;
    String password;

    SSH2TransportPreferences prefs;
    SSH2Transport            transport;
    SSH2Connection           connection;

    TextField textUser, textSrv, textPwd;
    Button    connBut, discBut, fwdBut, keyExch;
    // , termBut;
    // sftpBut, 
    Label     lblAlert;

    public static RandomSeed         randomSeed;
    public static SecureRandomAndPad secureRandom;

    public void init() {
	container = this;
	startMeUp();
    }

    public static void main(String[] argv) {
	final SSH2Tunneling mindterm2 = new SSH2Tunneling();
	mindterm2.frame = new Frame();
	mindterm2.frame.addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent e)  { mindterm2.windowClosing(e); }
	});
	mindterm2.frame.setTitle("SSH2 Tunneling Test");

	mindterm2.container = mindterm2.frame;

	try {
	    mindterm2.startMeUp();
	} catch (Throwable t) {
	    t.printStackTrace();
	}
    }

    public void startMeUp() {
	Label lbl;
	GridBagLayout      grid  = new GridBagLayout();
	GridBagConstraints gridc = new GridBagConstraints();

	System.out.println("Generating random seed...");
	initSeedGenerator();
	System.out.println("done.");

	container.setLayout(grid);

	gridc.fill   = GridBagConstraints.HORIZONTAL;
	gridc.anchor = GridBagConstraints.WEST;
	gridc.gridwidth  = 2;
	gridc.gridheight = 1;
	gridc.insets = new Insets(4, 4, 0, 4);

	gridc.gridy = 0;
	lbl = new Label("Server[:port] :");
	grid.setConstraints(lbl, gridc);
	container.add(lbl);
	textSrv = new TextField("", 16);
	grid.setConstraints(textSrv, gridc);
	container.add(textSrv);

	gridc.gridy = 1;
	lbl = new Label("Username :");
	grid.setConstraints(lbl, gridc);
	container.add(lbl);
	textUser = new TextField("", 16);
	grid.setConstraints(textUser, gridc);
	container.add(textUser);

	gridc.gridy = 2;
	lbl = new Label("Password :");
	grid.setConstraints(lbl, gridc);
	container.add(lbl);
	textPwd = new TextField("", 16);
	textPwd.setEchoChar('*');
	grid.setConstraints(textPwd, gridc);
	container.add(textPwd);

	gridc.gridy = 3;
	gridc.gridwidth = 4;
	gridc.anchor = GridBagConstraints.CENTER;
	lblAlert = new Label("", Label.CENTER);
	grid.setConstraints(lblAlert, gridc);
	container.add(lblAlert);

	Panel bp = new Panel(new FlowLayout());
	bp.add(connBut = new Button("Connect"));
	connBut.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		try {
		    lblAlert.setText("");

		    String srvStr = textSrv.getText();
		    int i;
		    if((i = srvStr.indexOf(':')) > 0) {
			host = srvStr.substring(0, i);
			port = Integer.parseInt(srvStr.substring(i + 1));
		    } else {
			host = srvStr;
			port = 22;
		    }
		    username = textUser.getText();
		    password = textPwd.getText();

		    lblAlert.setText("connecting...");

		    connect();

		    lblAlert.setText("...connected");

		    connBut.setEnabled(false);
		    discBut.setEnabled(true);
		    //termBut.setEnabled(true);
		    //		    sftpBut.setEnabled(true);
		    fwdBut.setEnabled(true);
		    keyExch.setEnabled(true);
		    textPwd.setText("");

		} catch (Exception ee) {
		    System.out.println("Error connecting: " + ee);
		    lblAlert.setText("Error: " + ee.getMessage());
		    connection = null;
		}
	    }
	});
	bp.add(discBut = new Button("Disconnect"));
	discBut.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		try {
		    transport.normalDisconnect("Disconnect by user");
		    lblAlert.setText("");
		    connBut.setEnabled(true);
		    discBut.setEnabled(false);
		    // termBut.setEnabled(false);
		    //		    sftpBut.setEnabled(false);
		    fwdBut.setEnabled(false);
		    keyExch.setEnabled(false);
		    connection = null;
		} catch (Exception ee) {
		    // !!!
		}
	    }
	});

	gridc.anchor = GridBagConstraints.CENTER;
	gridc.gridwidth  = 2;
	gridc.fill = GridBagConstraints.NONE;
	gridc.gridwidth = GridBagConstraints.REMAINDER;

	/* 
	termBut = new Button("New Terminal");
	termBut.addActionListener(new java.awt.event.ActionListener() {
	    public void actionPerformed(java.awt.event.ActionEvent e) {
		(new Thread(new Runnable() {
		    public void run() {
			newShell();
		    }
		})).start();
	    }
	});
	gridc.gridy = 4;
	grid.setConstraints(termBut, gridc);
	container.add(termBut);
	*/

	/*
	sftpBut = new java.awt.Button("New SFTP Shell");
	sftpBut.addActionListener(new java.awt.event.ActionListener() {
	    public void actionPerformed(java.awt.event.ActionEvent e) {
		SSH2SimpleSFTPShell sftp = new SSH2SimpleSFTPShell(connection,
							   "MindTerm2 - SFTP");
		sftp.getTerminal().setClipboard(
					GlobalClipboard.getClipboardHandler());
	    }
	});
	gridc.gridy = 5;
	grid.setConstraints(sftpBut, gridc);
	container.add(sftpBut);
	*/

	fwdBut = new Button("New Port Forward");
	fwdBut.addActionListener(new java.awt.event.ActionListener() {
	    public void actionPerformed(java.awt.event.ActionEvent e) {
		newForward();
	    }
	});

	gridc.gridy = 5;
	grid.setConstraints(fwdBut, gridc);
	container.add(fwdBut);

	keyExch = new Button("Key Re-Exchange");
	keyExch.addActionListener(new java.awt.event.ActionListener() {
	    public void actionPerformed(java.awt.event.ActionEvent e) {
		try {
		    prefs = new SSH2TransportPreferences(new String[] {
			"diffie-hellman-group1-sha1",
			"ssh-dss",
			"blowfish-cbc",
			"blowfish-cbc",
			"hmac-sha1,hmac-md5,hmac-md5-96",
			"hmac-md5,hmac-sha1,hmac-sha1-96",
			"none",
			"none",
			"", "" });

		    transport.startKeyExchange(prefs);
		} catch (Exception ee) {
		    System.out.println("Error: " + ee);
		    ee.printStackTrace();
		}
	    }
	});

	gridc.gridy = 6;
	grid.setConstraints(keyExch, gridc);
	container.add(keyExch);

	gridc.gridy = 7;
	grid.setConstraints(bp, gridc);
	container.add(bp);

	connBut.setEnabled(true);
	discBut.setEnabled(false);
	// termBut.setEnabled(false);
	//	sftpBut.setEnabled(false);
	keyExch.setEnabled(false);
	fwdBut.setEnabled(false);

	if(frame != null) {
	    frame.pack();
	    frame.show();
    	}

	textSrv.requestFocus();
    }

    public void connect() throws SSH2Exception, IOException {
	if(hasExpired()) {
	    throw new SSH2FatalException("demo has expired...");
	}

	System.out.println("** Connecting to SSH2 Server " + host + ":" + port);

	prefs = new SSH2TransportPreferences(new String[] {
	    "diffie-hellman-group1-sha1",
	    "ssh-dss",
	    "blowfish-cbc",
	    "blowfish-cbc",
	    "hmac-sha1,hmac-md5",
	    "hmac-sha1,hmac-md5",
	    "none",
	    "none",
	    "", "" });

	transport = new SSH2Transport(new Socket(host, port),
				      prefs, secureRandom);
	transport.boot();

	if(!transport.waitForKEXComplete()) {
	    throw new SSH2FatalException("KEX failed");
	}

	SSH2Authenticator authenticator = new SSH2Authenticator(username);

	/*
	String  keyFile = "/home/mats/.ssh2/id_dsa_1024_b";
	com.mindbright.ssh2.SSH2DSS dss     = null;
	try {
	    java.io.FileInputStream f =
		new java.io.FileInputStream(keyFile);
	    dss = com.mindbright.ssh2.SSH2DSS.createFromPrivateKeyFile("foobar", f);
	    authenticator.addModule("publickey", new SSH2AuthPublicKey(dss));
	} catch (Exception e) {
	    System.out.println("Error getting private file '" + keyFile +
			       "': " + e);
	}
	*/

	authenticator.addModule("password", new SSH2AuthPassword(password));
	SSH2UserAuth userAuth = new SSH2UserAuth(transport, authenticator);
	if(!userAuth.authenticateUser("ssh-connection")) {
	    throw new SSH2FatalException("Permission denied");
	}

	connection = new SSH2Connection(userAuth, transport, null, null);
	transport.setConnection(connection);


	// !!! REMOVE TESTING
	// connection.newLocalForward("127.0.0.1", 4747, "127.0.0.1", 21,
	//     new com.mindbright.ssh2.SSH2FTPProxyFilter());


    }

    /*
    public void newShell() {
	Frame frame = new Frame();
	TerminalWin terminal;
	terminal = new TerminalWin(frame, new TerminalXTerm());

	// terminal.setClipboard(GlobalClipboard.getClipboardHandler());

	frame.setLayout(new java.awt.BorderLayout());
	frame.add(terminal.getPanelWithScrollbar(), BorderLayout.CENTER);

	frame.setTitle(username + "@" + host + (port != 22 ? (":" + port) : ""));

	frame.pack();
	frame.show();

	SSH2TerminalAdapter termAdapter = new SSH2TerminalAdapterImpl(terminal);

	final SSH2SessionChannel session = connection.newTerminal(termAdapter);

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

	transport.getLog().info("MindTerm2", "got X11 forward? " +
				session.requestX11Forward(false, 0));
	transport.getLog().info("MindTerm2", "got pty? " +
				session.requestPTY("xterm",
						   terminal.rows(),
						   terminal.cols(),
						   null));
	transport.getLog().info("MindTerm2", "got shell? " + session.doShell());

	session.waitForExit(0);
	frame.dispose();
    }
    */

    Dialog fwdDialog;
    TextField textFwd;
    Checkbox cbRem, cbLoc;
    CheckboxGroup cbg;
    public void newForward() {
	if(fwdDialog == null) {
	    Frame parent = frame;
	    if(parent == null) {
		Component comp = this;
		do {
		    comp = comp.getParent();
		} while(!(comp instanceof Frame));
		parent = (Frame)comp;
	    }
	    fwdDialog = new Dialog(parent, "Port Forward Setup", false);

	    GridBagLayout       grid  = new GridBagLayout();
	    GridBagConstraints  gridc = new GridBagConstraints();
	    Label               lbl;
	    Button              b;

	    fwdDialog.setLayout(grid);

	    gridc.fill      = GridBagConstraints.HORIZONTAL;
	    gridc.anchor    = GridBagConstraints.WEST;
	    gridc.gridy     = 0;
	    gridc.gridwidth = 1;
	    gridc.insets    = new Insets(4, 4, 4, 4);

	    lbl = new Label("[<src-host>:]<src-port>:<dest-host>:<dest-port>");
	    grid.setConstraints(lbl, gridc);
	    fwdDialog.add(lbl);

	    textFwd = new TextField("", 20);
	    gridc.gridy     = 1;
	    grid.setConstraints(textFwd, gridc);
	    fwdDialog.add(textFwd);

	    cbg = new CheckboxGroup();

	    Panel p = new Panel(new FlowLayout());
	    p.add(cbLoc = new Checkbox("Local", cbg, true));
	    p.add(cbRem = new Checkbox("Remote", cbg, false));

	    gridc.anchor = GridBagConstraints.CENTER;
	    gridc.gridy     = 2;
	    gridc.gridwidth  = 2;
	    gridc.fill = GridBagConstraints.NONE;
	    gridc.gridwidth = GridBagConstraints.REMAINDER;
	    grid.setConstraints(p, gridc);
	    fwdDialog.add(p);

	    p = new Panel(new FlowLayout());

	    p.add(b = new Button("Ok"));
	    b.addActionListener(new java.awt.event.ActionListener() {
		public void actionPerformed(java.awt.event.ActionEvent e) {
		    try {
			Object[] components = parseForwardSpec(textFwd.getText());
			if(cbLoc.getState()) {
			    connection.newLocalForward((String)components[0],
						       ((Integer)components[1]).intValue(),
						       (String)components[2],
						       ((Integer)components[3]).intValue());
			} else {
			    connection.newRemoteForward((String)components[0],
						       ((Integer)components[1]).intValue(),
						       (String)components[2],
						       ((Integer)components[3]).intValue());
			}
			fwdDialog.setVisible(false);
		    } catch(Exception ee) {

			System.out.println("Error in forward: " + ee);
			ee.printStackTrace();

			// !!!
		    }
		}
	    });
	    p.add(b = new Button("Cancel"));
	    b.addActionListener(new java.awt.event.ActionListener() {
		public void actionPerformed(java.awt.event.ActionEvent e) {
		    fwdDialog.setVisible(false);
		}
	    });

	    gridc.gridy     = 3;
	    grid.setConstraints(p, gridc);
	    fwdDialog.add(p);
	    fwdDialog.setResizable(true);
	    fwdDialog.pack();
	}

	fwdDialog.setVisible(true);
    }

    public synchronized void windowClosing(WindowEvent e) {
	if(connection == null) {
	    frame.dispose();
	    System.exit(0);
	}
    }

    public static Object[] parseForwardSpec(String spec)
	throws IllegalArgumentException
    {
	int    d1, d2, d3;
	String tmp;
	Object[] components = new Object[4];

	d1 = spec.indexOf(':');
	d2 = spec.lastIndexOf(':');
	if(d1 == d2)
	    throw new IllegalArgumentException("Invalid port forward spec. " +
					       spec);

	d3 = spec.indexOf(':', d1 + 1);

	if(d3 != d2) {
	    components[0] = spec.substring(0, d1);
	    components[1] = Integer.valueOf(spec.substring(d1 + 1, d3));
	    components[2] = spec.substring(d3 + 1, d2);
	} else {
	    components[0] = "127.0.0.1";
	    components[1] = Integer.valueOf(spec.substring(0, d1));
	    components[2] = spec.substring(d1 + 1, d2);
	}

	tmp = spec.substring(d2 + 1);
	components[3] = Integer.valueOf(tmp);

	return components;
    }

    boolean hasExpired() {
	boolean expired = false;
	long now = System.currentTimeMillis();

	if(expires) {
	    int daysRemaining = (int)((validTime - (now - validFrom)) / (1000L * 60L * 60L * 24L));
	    if(daysRemaining <= 0) {
		expired = true;
	    } else {
		System.out.println("** This is a demo, it will expire in " +
				   daysRemaining + " days.");
	    }
	}
	return expired;
  }

  public static RandomSeed randomSeed() {
      if(randomSeed == null) {
	  randomSeed = new RandomSeed("/dev/random", "/dev/urandom");
      }
      return randomSeed;
  }

    public static void initSeedGenerator() {
	RandomSeed seed = randomSeed();
	if(secureRandom == null) {
	    byte[] s = seed.getBytesBlocking(20, false);
	    secureRandom = new SecureRandomAndPad(new SecureRandom(s));
	} else {
	    int bytes = seed.getAvailableBits() / 8;
	    secureRandom.setSeed(seed.getBytesBlocking(bytes > 20 ? 20 : bytes));
	}
	secureRandom.setPadSeed(seed.getBytes(20));
    }

}
