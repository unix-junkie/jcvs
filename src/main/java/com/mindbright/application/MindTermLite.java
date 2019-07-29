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
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

import java.net.Socket;

import java.applet.Applet;

import java.util.Properties;

import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;

import com.mindbright.jca.security.SecureRandom;

import com.mindbright.ssh2.SSH2;
import com.mindbright.ssh2.SSH2SimpleClient;
import com.mindbright.ssh2.SSH2AuthModule;
import com.mindbright.ssh2.SSH2AuthPassword;
import com.mindbright.ssh2.SSH2AuthKbdInteract;
import com.mindbright.ssh2.SSH2Interactor;
import com.mindbright.ssh2.SSH2Exception;
import com.mindbright.ssh2.SSH2FatalException;
import com.mindbright.ssh2.SSH2UserCancelException;

import com.mindbright.util.SecureRandomAndPad;
import com.mindbright.util.RandomSeed;

import com.mindbright.gui.AWTGridBagContainer;
import com.mindbright.gui.AWTConvenience;
import com.mindbright.gui.Logo;

public class MindTermLite extends Applet implements SSH2Interactor {

    public interface Module {
	public void init(MindTermLite mindterm);
	public void activate(MindTermLite mindterm);
	public String getLabel();
	public boolean isAvailable(MindTermLite mindterm);
	public void connected(MindTermLite mindterm);
    }

    private class Actions implements ActionListener, ItemListener {
	private int       action;

	public Actions(int action) {
	    this.action = action;
	}

	public void actionPerformed(ActionEvent e) {
	    if(action == -1) {
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

		    enableButtons();

		    textPwd.setText("");

		} catch (Exception ee) {
		    System.out.println("Error connecting: " + ee);
		    lblAlert.setText("Error: " + ee.getMessage());
		}
	    } else if(action == -2) {
		try {
		    client.getTransport().normalDisconnect("Disconnect by user");
		    lblAlert.setText("");

		    enableButtons();

		} catch (Exception ee) {
		    // !!!
		}
	    } else {
		modules[action].activate(MindTermLite.this);
	    }
	}

	public void itemStateChanged(ItemEvent e) {
	    if("password".equals(choiceAuthTyp.getSelectedItem())) {
		textPwd.setEnabled(true);
	    } else {
		textPwd.setEnabled(false);
	    }
	    textPwd.setText("");
	}

    }

    Container container;
    Frame     frame;
    Logo      logo;

    Properties settings;

    String host;
    int    port;
    String username;
    String password;

    SSH2SimpleClient client;

    Module[] modules;
    int      modCnt;

    Choice     choiceAuthTyp;
    TextField  textUser, textSrv, textPwd;
    Button     connBut, discBut;
    Button[]   modButs;
    Label      lblAlert;

    public static RandomSeed         randomSeed;
    public static SecureRandomAndPad secureRandom;

    public MindTermLite() {
	settings = new Properties();

	settings.put("kex-algorithms", "diffie-hellman-group1-sha1");
	settings.put("server-host-key-algorithms", "ssh-dss");
	settings.put("enc-algorithms-cli2srv", "blowfish-cbc,aes128-cbc");
	settings.put("enc-algorithms-srv2cli", "blowfish-cbc,aes128-cbc");
	settings.put("mac-algorithms-cli2srv", "hmac-md5,hmac-sha1");
	settings.put("mac-algorithms-srv2cli", "hmac-md5,hmac-sha1");
	settings.put("comp-algorithms-cli2srv", "none");
	settings.put("comp-algorithms-srv2cli", "none");
	settings.put("languages-cli2srv", "");
	settings.put("languages-srv2cli", "");
	settings.put("package-version",
		     SSH2.getPackageVersion("MindTermLite",
					    1, 0, "(non-commercial)"));
	modButs = new Button[16];
	modules = new Module[16];
	modCnt  = 0;
	logo    = null;
    }

    public void init() {
	container = this;
	startMeUp();
    }

    public static void main(String[] argv) {
	final MindTermLite mindterm = new MindTermLite();
	mindterm.frame = new Frame();
	mindterm.frame.addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e)  { mindterm.windowClosing(e); }
	    });
	mindterm.frame.setTitle("MindTermLite 1.0");

	mindterm.container = mindterm.frame;

	mindterm.startMeUp();
    }

    public void startMeUp() {
	try {
	    InputStream in =
		getClass().getResourceAsStream("/defaults/settings.txt");
	    if(in != null) {
		settings.load(in);
	    }

	    installLogo();

	    initSeedGenerator();

	    Label lbl;
	    AWTGridBagContainer grid = new AWTGridBagContainer(container);

	    if(logo != null) {
		grid.getConstraints().fill      = GridBagConstraints.NONE;
		grid.getConstraints().anchor    = GridBagConstraints.CENTER;
		grid.add(logo, 0, GridBagConstraints.REMAINDER);
	    }

	    grid.getConstraints().anchor = GridBagConstraints.WEST;

	    lbl = new Label("Server[:port] :");
	    grid.add(lbl, 1, 2);

	    textSrv = new TextField("", 16);
	    grid.add(textSrv, 1, 2);

	    lbl = new Label("Username :");
	    grid.add(lbl, 2, 2);
	    textUser = new TextField("", 16);
	    grid.add(textUser, 2, 2);

	    lbl = new Label("Authentication :");
	    grid.add(lbl, 3, 2);

	    choiceAuthTyp = AWTConvenience.newChoice(new String[] { "password", "kbd-interactive"});
	    choiceAuthTyp.addItemListener(new Actions(-3));
	    grid.add(choiceAuthTyp, 3, 2);

	    lbl = new Label("Password :");
	    grid.add(lbl, 4, 2);

	    textPwd = new TextField("", 16);
	    textPwd.setEchoChar('*');
	    grid.add(textPwd, 4, 2);

	    grid.getConstraints().anchor = GridBagConstraints.CENTER;
	    grid.getConstraints().fill   = GridBagConstraints.HORIZONTAL;

	    lblAlert = new Label("", Label.CENTER);
	    grid.add(lblAlert, 5, GridBagConstraints.REMAINDER);

	    Panel bp = new Panel(new FlowLayout());
	    bp.add(connBut = new Button("Connect"));
	    connBut.addActionListener(new Actions(-1));
	    bp.add(discBut = new Button("Disconnect"));
	    discBut.addActionListener(new Actions(-2));

	    grid.getConstraints().insets = new Insets(4, 24, 12, 24);

	    Panel modPanel = getModulesPanel();
	    if(modPanel != null) {
		grid.add(modPanel, 6, 4);
	    }

	    grid.getConstraints().insets = new Insets(4, 4, 0, 4);

	    grid.add(bp, 7, 4);

	    enableButtons();

	    String server = settings.getProperty("server");
	    String user   = settings.getProperty("username");
	    String auth   = settings.getProperty("auth-method");
	    if(server != null) {
		textSrv.setText(server);
	    }
	    if(user != null) {
		textUser.setText(user);
	    }
	    if(frame != null) {
		frame.pack();
		frame.show();
	    }

	    if(server == null) {
		textSrv.requestFocus();
	    } else if(user == null) {
		textUser.requestFocus();
	    } else if(auth == null || auth.equals("password")) {
		textPwd.requestFocus();
	    } else {
		choiceAuthTyp.select(auth);
		connBut.requestFocus();
		(new Actions(0)).itemStateChanged(null);
	    }

	} catch (Throwable t) {
	    t.printStackTrace();
	}
    }

    private void enableButtons() {
	boolean isConnected = isConnected();
	connBut.setEnabled(!isConnected);
	discBut.setEnabled(isConnected);
	for(int i = 0; i < modCnt; i++) {
	    if(modules[i].getLabel() != null) {
		modButs[i].setEnabled(modules[i].isAvailable(this));
	    }
	}
    }

    private Panel getModulesPanel() {
	int i;
	for(i = 0; i < 16; i++) {
	    String     className = settings.getProperty("module" + i);
	    if(className == null) {
		break;
	    }
	    try {
		modules[i] = (Module)Class.forName(className).newInstance();
		modules[i].init(this);
		modCnt++;
	    } catch (Exception e) {
		// !!! TODO
		throw new Error("Module class '" + className + "' not found");
	    }
	}
	Panel p = null;
	if(modCnt > 0) {
	    p = new Panel(new GridLayout(modCnt, 1));
	    for(i = 0; i < modCnt; i++) {
		String label = modules[i].getLabel();
		if(label != null) {
		    modButs[i] = new Button(label);
		    modButs[i].addActionListener(new Actions(i));
		    p.add(modButs[i]);
		}
	    }
	}
	return p;
    }

    public void connect() throws SSH2Exception, IOException {
	System.out.println("** Connecting to SSH2 Server " + host + ":" + port);

	SSH2AuthModule authModule = null;
	String         authType   = null;

	if("password".equals(choiceAuthTyp.getSelectedItem())) {
	    authType   = "password";
	    authModule = new SSH2AuthPassword(password);
	} else {
	    authType   = "keyboard-interactive";
	    authModule = new SSH2AuthKbdInteract(this);
	}

	client = new SSH2SimpleClient(new Socket(host, port),
				      secureRandom,
				      username,
				      authType,
				      authModule,
				      settings);

	for(int i = 0; i < modCnt; i++) {
	    modules[i].connected(this);
	}
    }

    public synchronized void windowClosing(WindowEvent e) {
	if(!isConnected()) {
	    frame.dispose();
	    System.exit(0);
	}
    }

    public boolean isConnected() {
	return (client != null && client.getTransport() != null &&
		client.getTransport().isConnected());
    }

    public SSH2SimpleClient getClient() {
	return client;
    }

    public String getProperty(String name) {
	return settings.getProperty(name);
    }

    public void alert(String message) {
	lblAlert.setText(message);
    }

    public static RandomSeed randomSeed() {
	if(randomSeed == null) {
	    randomSeed = new RandomSeed("/dev/random", "/dev/urandom");
	}
	return randomSeed;
    }

    public static void initSeedGenerator() {
	System.out.print("Generating random seed...");
	RandomSeed seed = randomSeed();
	if(secureRandom == null) {
	    byte[] s = seed.getBytesBlocking(20, false);
	    secureRandom = new SecureRandomAndPad(new SecureRandom(s));
	} else {
	    int bytes = seed.getAvailableBits() / 8;
	    secureRandom.setSeed(seed.getBytesBlocking(bytes > 20 ? 20 : bytes));
	}
	secureRandom.setPadSeed(seed.getBytes(20));
	System.out.println("done.");
    }

    public ByteArrayOutputStream readResource(String name) {
	InputStream in = getClass().getResourceAsStream(name);
	ByteArrayOutputStream baos = null;
	if(in != null) {
	    baos = new ByteArrayOutputStream();
	    try {
		int c;
		while((c = in.read()) >= 0)
		    baos.write(c);
	    } catch(IOException e) {
		// !!!
		System.err.println("ERROR reading resource " + name + " : " + e);
	    }
	}
	return baos;
    }

    public void installLogo() {
	ByteArrayOutputStream baos = readResource("/defaults/logo_lite.gif");

	if(baos != null) {
	    byte[] raw = baos.toByteArray();
	    Image  img = Toolkit.getDefaultToolkit().createImage(raw);
	    logo       = new Logo(img);
	}
    }

    public Frame getParentFrame() {
	Frame parent = frame;
	if(parent == null) {
	    Component comp = this;
	    do {
		comp = comp.getParent();
	    } while(!(comp instanceof Frame));
	    parent = (Frame)comp;
	}

	return parent;
    }

    public String promptLine(String prompt, boolean echo)
	throws SSH2UserCancelException
    {
	String[] answer =
	    promptMulti(new String[] { prompt }, new boolean[] { echo });;
	return answer[0];
    }

    public String[] promptMulti(String[] prompts, boolean[] echos)
	throws SSH2UserCancelException
    {
	return promptMultiFull("MindTermLite", null, prompts, echos);
    }

    // !!! OUCH Clean out and mote to gui or sshcommon or some such
    static boolean pressedCancel;
    public String[] promptMultiFull(String name, String instruction,
			     String[] prompts, boolean[] echos)
	throws SSH2UserCancelException
    {
	final Dialog promptDialog = new Dialog(getParentFrame(), name, true);
	AWTGridBagContainer grid  = new AWTGridBagContainer(promptDialog);

	Label               lbl;
	Button              b;
	int                 i;
	TextField[]         fields = new TextField[prompts.length];
	ActionListener      al;

	if(instruction != null) {
	    if(instruction.length() > 32) {
		grid.add(new TextArea(instruction, 6, 32,
				      TextArea.SCROLLBARS_VERTICAL_ONLY), 0, 4);
	    } else {
		grid.add(new Label(instruction), 0, 4);
	    }
	}

	for(i = 0; i < prompts.length; i++) {
	    lbl = new Label(prompts[i]);
	    grid.add(lbl, 1 + i, 2);
	    fields[i] = new TextField("", 16);
	    grid.add(fields[i], 1 + i, 2);
	}

	Button okBut, cancBut;
	Panel bp = new Panel(new FlowLayout());
	bp.add(okBut = new Button("OK"));
	okBut.addActionListener(al = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    if(e.getActionCommand().equals("Cancel")) {
			pressedCancel = true;
		    } else {
			pressedCancel = false;
		    }
		    promptDialog.setVisible(false);
		}
	    });

	bp.add(cancBut = new Button("Cancel"));
	cancBut.addActionListener(al);

	grid.add(bp, prompts.length + 2, GridBagConstraints.REMAINDER);

	promptDialog.setResizable(true);
	promptDialog.pack();
	promptDialog.setVisible(true);

	if(pressedCancel) {
	    throw new SSH2UserCancelException("User cancel");
	}

	String[] answers = new String[prompts.length];
	for(i = 0; i < answers.length; i++) {
	    answers[i] = fields[i].getText();
	}

	return answers;
    }

    public int promptList(String name, String instruction, String[] choices)
	throws SSH2UserCancelException
    {
	// !!! TODO
	return 0;
    }

}
