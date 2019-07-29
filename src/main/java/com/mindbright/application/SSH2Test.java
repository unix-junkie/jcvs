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

import java.util.Properties;
import java.io.FileInputStream;
import java.net.Socket;

import java.awt.*;
import java.awt.event.*;

import com.mindbright.ssh2.*;

public final class SSH2Test {
    static String host;
    static int    port;

    public static void main(String[] argv) {
	try {
	    if(argv.length < 2 || !argv[0].equals("--f"))
		throw new Exception("use --f to give property-file with settings");
	    String propsFile = argv[1];
	    final Properties props = new Properties();
	    props.load(new FileInputStream(propsFile));

	    final SSH2TransportPreferences prefs =
		new SSH2TransportPreferences(props);

	    host = props.getProperty("server");
	    port = Integer.valueOf(props.getProperty("port")).intValue();

	    System.out.println("** SSH2 Server " + host + ":" + port);

	    final SSH2Transport transport =
		new SSH2Transport(new Socket(host, port),
				  prefs,
				  new com.mindbright.util.SecureRandomAndPad());

	    transport.boot();
	    transport.waitForKEXComplete();

	    SSH2Authenticator authenticator =
		new SSH2Authenticator(props.getProperty("user"));

	    String  keyFile = props.getProperty("privatekey");
	    if(keyFile != null) {
		SSH2DSS dss = null;
		try {
		    SSH2KeyPairFile kpf = new SSH2KeyPairFile();
		    kpf.load(keyFile, props.getProperty("passphrase"));

		    String        alg  = kpf.getAlgorithmName();
		    SSH2Signature sign = SSH2Signature.getInstance(alg);

		    sign.initSign(kpf.getKeyPair().getPrivate());
		    sign.setPublicKey(kpf.getKeyPair().getPublic());

		    authenticator.addModule("publickey",
					    new SSH2AuthPublicKey(sign));
		} catch (Exception e) {
		    System.out.println("Error getting private file '" + keyFile +
				       "': " + e);
		}
	    }

	    authenticator.addModule("password",
				    new SSH2AuthPassword(props.getProperty("password")));
							  
	    SSH2UserAuth userAuth = new SSH2UserAuth(transport,
						     authenticator);
	    userAuth.authenticateUser("ssh-connection");


	    final SSH2Connection connection = new SSH2Connection(userAuth, transport);
	    transport.setConnection(connection);


	    int i;
	    for(i = 0; i < 32; i++) {
		String spec = props.getProperty("local" + i);
		if(spec == null)
		    break;
		Object[] components = parseForwardSpec(spec);
		connection.newLocalForward((String)components[0],
					   ((Integer)components[1]).intValue(),
					   (String)components[2],
					   ((Integer)components[3]).intValue());
	    }
	    for(i = 0; i < 32; i++) {
		String spec = props.getProperty("remote" + i);
		if(spec == null)
		    break;
		Object[] components = parseForwardSpec(spec);
		connection.newRemoteForward((String)components[0],
					    ((Integer)components[1]).intValue(),
					    (String)components[2],
					    ((Integer)components[3]).intValue());
	    }


	    final TextField    textUser, textSrv, textPwd;
	    final Choice       choiceAuthTyp;
	    Frame              frame = new Frame();
	    Label              lbl;
	    GridBagLayout      grid  = new GridBagLayout();
	    GridBagConstraints gridc = new GridBagConstraints();
	    Button             okBut, cancBut, b;
	    ItemListener       il;

	    frame.setLayout(grid);

	    gridc.fill   = GridBagConstraints.HORIZONTAL;
	    gridc.anchor = GridBagConstraints.WEST;
	    gridc.gridwidth = 2;
	    gridc.insets = new Insets(4, 4, 0, 4);

	    gridc.gridy = 0;
	    lbl = new Label("Server[:port] :");
	    grid.setConstraints(lbl, gridc);
	    frame.add(lbl);
	    textSrv = new TextField("", 12);
	    grid.setConstraints(textSrv, gridc);
	    frame.add(textSrv);

	    gridc.gridy = 1;
	    lbl = new Label("Username :");
	    grid.setConstraints(lbl, gridc);
	    frame.add(lbl);
	    textUser = new TextField("", 12);
	    grid.setConstraints(textUser, gridc);
	    frame.add(textUser);

	    gridc.gridy = 2;
	    lbl = new Label("Authentication :");
	    grid.setConstraints(lbl, gridc);
	    frame.add(lbl);
	    choiceAuthTyp = new Choice();
	    choiceAuthTyp.add("password");
	    choiceAuthTyp.add("public");

	    choiceAuthTyp.addItemListener(il = new ItemListener() {
		    public void itemStateChanged(ItemEvent e) {
			updateChoices(choiceAuthTyp.getSelectedItem());
		    }
		});

	    gridc.gridy = 3;
	    lbl = new Label("Password :");
	    grid.setConstraints(lbl, gridc);
	    frame.add(lbl);
	    textPwd = new TextField("", 12);
	    textPwd.setEchoChar('*');
	    grid.setConstraints(textPwd, gridc);
	    frame.add(textPwd);

	    b = new Button("New Terminal");
	    b.addActionListener(new java.awt.event.ActionListener() {
		public void actionPerformed(java.awt.event.ActionEvent e) {
		    (new Thread(new Runnable() {
			public void run() {
			    newShell(connection, prefs);
			}
		    })).start();
		}
	    });

	    gridc.gridy = 4;
	    gridc.fill      = GridBagConstraints.NONE;
	    gridc.gridwidth = 2;
	    grid.setConstraints(b, gridc);
	    frame.add(b);

	    b = new java.awt.Button("New SFTP Shell");
	    b.addActionListener(new java.awt.event.ActionListener() {
		public void actionPerformed(java.awt.event.ActionEvent e) {
		    new SSH2SimpleSFTPShell(connection, "SFTP File Transfer");
		}
	    });

	    gridc.fill      = GridBagConstraints.NONE;
	    gridc.gridwidth = 2;
	    grid.setConstraints(b, gridc);
	    frame.add(b);

	    b = new java.awt.Button("Exchange keys");
	    b.addActionListener(new java.awt.event.ActionListener() {
		public void actionPerformed(java.awt.event.ActionEvent e) {
		    try {
			transport.startKeyExchange();
		    } catch (Exception ee) {
			System.out.println("** Error: " + ee);
		    }
		}
	    });

	    gridc.fill      = GridBagConstraints.NONE;
	    gridc.gridwidth = 2;
	    grid.setConstraints(b, gridc);
	    frame.add(b);

	    frame.pack();
	    frame.setTitle("SSH2Test");
	    frame.show();

	} catch (Exception e) {
	    System.out.println("Error: " + e);
	    e.printStackTrace();
	}
    }

    static void updateChoices(String at) {
	if(at.equals("password")) {
	    
	} else {
	    
	}
    }

    public static void newShell(SSH2Connection connection,
				SSH2TransportPreferences prefs) {
	java.awt.Frame frame = new java.awt.Frame();
	com.mindbright.terminal.TerminalWin terminal;
	terminal = new com.mindbright.terminal.TerminalWin(frame,
		       new com.mindbright.terminal.TerminalXTerm());

	frame.setLayout(new java.awt.BorderLayout());
	frame.add(terminal.getPanelWithScrollbar(),
		  java.awt.BorderLayout.CENTER);

	frame.setTitle("MindTerm2 - " + host + ":" + port);

	frame.pack();
	frame.show();

	SSH2TerminalAdapter termAdapter = new SSH2TerminalAdapterImpl(terminal);

	SSH2SessionChannel session = connection.newTerminal(termAdapter);

	if(session.openStatus() != SSH2Channel.STATUS_OPEN) {
	    System.out.println("** Failed to open session channel");
	    frame.dispose();
	    return;
	}

	System.out.println("** Got pty? " + session.requestPTY("xterm",
							       terminal.rows(),
							       terminal.cols(),
							       null));
	System.out.println("** Got shell? " + session.doShell());

	session.waitForExit(0);
	frame.dispose();
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

}
