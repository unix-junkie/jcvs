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

package com.mindbright.ssh;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.OutputStream;

import java.awt.*;
import java.awt.event.*;

import com.mindbright.jca.security.KeyPair;
import com.mindbright.jca.security.PublicKey;
import com.mindbright.jca.security.SecureRandom;
import com.mindbright.jca.security.KeyPairGenerator;
import com.mindbright.jca.security.NoSuchAlgorithmException;
import com.mindbright.jca.security.interfaces.RSAPrivateCrtKey;

import com.mindbright.util.RandomSeed;
import com.mindbright.util.ASCIIArmour;
import com.mindbright.util.Progress;

import com.mindbright.gui.AWTConvenience;
import com.mindbright.gui.AWTGridBagContainer;
import com.mindbright.gui.ProgressBar;

import com.mindbright.terminal.Terminal;
import com.mindbright.terminal.TerminalWin;

import com.mindbright.ssh2.SSH2PublicKeyFile;
import com.mindbright.ssh2.SSH2KeyPairFile;
import com.mindbright.ssh2.SSH2Exception;
import com.mindbright.ssh2.SSH2AccessDeniedException;

// !!! TODO Cleanout !!!

public final class SSHKeyGenerationDialog {

    private final static String keyGenerationHelp =
	"The key is generated using a random number generator, which " +
	"is seeded by mouse movement in the field containing this text. " +
	"Please move the mouse around in here until the progress bar below " +
	"registers 100%.\n" +
	"\n" +
	"This will create a publickey identity which can be used with the " +
	"publickey authentication method. Your identity will consist of two " +
	"parts: public and private keys. Your private key will be saved " +
	"in the location which you specify; the corresponding public key " +
	"is saved in a file with an identical name but with an extension of " +
	"'.pub' added to it.\n" +
	"\n" +
	"Your private key is protected by encryption, if you entered a " +
	"password. If you left the password field blank, the key will " +
	"not be encrypted. This should only be used in protected " +
	"environments where unattended logins are desired. The contents " +
	"of the 'comment' field are stored with your key, and displayed " +
	"each time you are prompted for the key's password.";

    private final static String keyGenerationComplete =
	"Key Generation Complete\n\n" +
	"To use the key, you must transfer the '.pub' public key " +
	"file to an SSH server and add it to the set of authorized keys. " +
	"See your server documentation for details on this.\n\n" +
	"For convenience, your public key has been copied to the clipboard.\n\n" +
	"Examples:\n" +
	"In ssh2 the '.pub' file should be pointed out in the file " +
	"'authorization' in the config directory (e.g. ~/.ssh2)\n\n" +
	"In OpenSSH's ssh2 the contents of the '.pub' file should be added " +
	"to the file 'authorized_keys2' in your config directory (e.g. ~/.ssh) " +
	"on the server.\n\n" +
	"In ssh1 the contents of the '.pub' file should be added to the " +
	"file 'authorized_keys' in your ssh directory (e.g. ~/.ssh).\n\n" +
	"Press 'Back' to generate a new keypair.";

    private static Dialog      keyGenerationDialog;
    private static FileDialog  keyGenFD;
    private static Choice      choiceBits, choiceType;
    private static TextField   fileText;
    private static TextField   pwdText;
    private static TextField   pwdText2;
    private static TextField   commText;
    private static TextArea    descText;
    private static ProgressBar progBar;
    private static Button      okBut;
    private static Checkbox    cbOpenSSH;
    private static Panel       cardPanel;
    private static CardLayout  cardLayout;
    private static Frame       parent;
    private static boolean     generatedAndSaved;
    private static SSHInteractiveClient client;

    public static void show(Frame par, SSHInteractiveClient cli) {
	client = cli;
	parent = par;

	try {
	    netscape.security.PrivilegeManager.enablePrivilege("UniversalFileAccess");
	} catch (netscape.security.ForbiddenTargetException e) {
	    // !!!
	}

	if(keyGenerationDialog == null) {
	    keyGenerationDialog = new Dialog(parent, "MindTerm - Publickey Keypair Generation", true);

	    Button b;
	    AWTGridBagContainer grid = new AWTGridBagContainer(keyGenerationDialog);

	    keyGenFD = new FileDialog(parent, "MindTerm - Select file to save identity to", FileDialog.SAVE);
	    keyGenFD.setDirectory(client.propsHandler.getSSHHomeDir());

	    createCardPanel();

	    grid.add(cardPanel, 0, GridBagConstraints.REMAINDER);

	    grid.getConstraints().anchor = GridBagConstraints.CENTER;

	    Panel bp = new Panel(new FlowLayout());
	    bp.add(okBut = new Button("Generate"));
	    okBut.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			if(generatedAndSaved) {
			    resetValues();
			} else {
			    if(checkValues(pwdText.getText(),
					   pwdText2.getText(),
					   fileText.getText())) {
				cardLayout.show(cardPanel, "second");
				okBut.setEnabled(false);
			    }
			}
		    }
		});
	    bp.add(b = new Button("Close"));
	    b.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			keyGenerationDialog.setVisible(false);
			resetValues();
		    }
		});

	    grid.add(bp, 2, GridBagConstraints.REMAINDER);

	    keyGenerationDialog.addWindowListener(new AWTConvenience.CloseAdapter(b));

	    AWTConvenience.setBackgroundOfChildren(keyGenerationDialog);

	    keyGenerationDialog.setResizable(true);
	    keyGenerationDialog.pack();
	}

	resetValues();

	RandomSeed seed = client.randomSeed();
	seed.addEntropyGenerator(descText);

	AWTConvenience.placeDialog(keyGenerationDialog);
	choiceBits.requestFocus();
	keyGenerationDialog.setVisible(true);
    }

    private static void alert(String msg) {
	SSHMiscDialogs.alert("MindTerm - Alert", msg, parent);
    }

    private static void setDefaultFileName(SSHInteractiveClient client) {
	try {
	    String fn = client.propsHandler.getSSHHomeDir() + SSHPropertyHandler.DEF_IDFILE;
	    File   f  = new File(fn);
	    int    fi = 0;
	    while(f.exists()) {
		fn = client.propsHandler.getSSHHomeDir() + SSHPropertyHandler.DEF_IDFILE + fi;
		f  = new File(fn);
		fi++;
	    }
	    fi--;
	    fileText.setText(SSHPropertyHandler.DEF_IDFILE + (fi >= 0 ? String.valueOf(fi) : ""));
	} catch (Throwable t) {
	    // !!!
	    // Don't care...
	}
    }

    private static void createCardPanel() {
	cardPanel  = new Panel();
	cardLayout = new CardLayout();
	cardPanel.setLayout(cardLayout);

	Button b;
	Label  label;
	Panel  p;

	p = new Panel();
	AWTGridBagContainer grid = new AWTGridBagContainer(p);

	grid.getConstraints().fill = GridBagConstraints.BOTH;

	descText = new TextArea(keyGenerationHelp, 12, 34, TextArea.SCROLLBARS_VERTICAL_ONLY);
	descText.setEditable(false);
	grid.add(descText, 2, 8);
	descText.addMouseMotionListener(new MouseMotionAdapter() {
		public void mouseMoved(MouseEvent e) {
		    if(progBar.isFinished()) {
			try {
			    client.randomSeed().removeProgress();
			    progBar.setValue(0);
			    int     bits = Integer.valueOf(choiceBits.getSelectedItem()).intValue();
			    String  alg  = choiceType.getSelectedItem().substring(0, 3);

			    descText.setText("Generating keypair, please wait...");
			    Thread.yield();

			    KeyPair kp = generateKeyPair(alg, bits);
			    saveKeyPair(kp);

			    okBut.setEnabled(true);
			    okBut.setLabel("Back");
			    descText.setText(keyGenerationComplete);
			    generatedAndSaved = true;

			} catch (Throwable t) {
			    alert("Error while generating/saving key pair: " +
				  t.getMessage());
			    cardLayout.show(cardPanel, "first");
			}
		    }
		}
	    });

	grid.getConstraints().fill   = GridBagConstraints.NONE;
	grid.getConstraints().anchor = GridBagConstraints.CENTER;

	progBar = new ProgressBar(512, 150, 20);
	grid.add(progBar, 3, 8);

	cardPanel.add(p, "second");

	p = new Panel();
	grid = new AWTGridBagContainer(p);

	label = new Label("Key type/format:");
	grid.add(label, 0, 2);

	choiceType = AWTConvenience.newChoice(new String[] { "DSA (ssh2)",
							     "RSA (ssh2)",
							     "RSA (ssh1)" });
	grid.add(choiceType, 0, 2);
	label = new Label("Key length (bits):");
	grid.add(label, 1, 2);
	choiceBits = AWTConvenience.newChoice(new String[] { "768",
							     "1024",
							     "1536" });
	grid.add(choiceBits, 1, 2);

	label = new Label("Identity file:");
	grid.add(label, 2, 2);

	fileText = new TextField("", 18);
	grid.add(fileText, 2, 2);

	b = new Button("...");
	b.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    keyGenFD.setVisible(true);
		    if(keyGenFD.getFile() != null && keyGenFD.getFile().length() > 0)
			fileText.setText(keyGenFD.getDirectory() + keyGenFD.getFile());
		}
	    });
	grid.getConstraints().fill = GridBagConstraints.NONE;
	grid.add(b, 2, 1);

	grid.getConstraints().fill = GridBagConstraints.HORIZONTAL;

	label = new Label("Password:");
	grid.add(label, 3, 2);

	pwdText = new TextField("", 18);
	pwdText.setEchoChar('*');

	grid.add(pwdText, 3, 2);

	label = new Label("Password again:");
	grid.add(label, 4, 2);

	pwdText2 = new TextField("", 18);
	pwdText2.setEchoChar('*');
	grid.add(pwdText2, 4, 2);

	label = new Label("Comment:");
	grid.add(label, 5, 2);

	commText = new TextField("", 18);
	grid.add(commText, 5, 2);

	cbOpenSSH = new Checkbox("OpenSSH .pub format");
	grid.add(cbOpenSSH, 6, 4);

	cardPanel.add(p, "first");
    }

    public static KeyPair generateKeyPair(String alg, int bits)
	throws NoSuchAlgorithmException
    {
	KeyPairGenerator kpg = KeyPairGenerator.getInstance(alg);
	kpg.initialize(bits, client.secureRandom());
	return kpg.generateKeyPair();
    }

    private static void saveKeyPair(KeyPair kp)
	throws IOException, SSH2Exception, NoSuchAlgorithmException
    {
	String passwd    = pwdText.getText();
	String fileName  = fileText.getText();
	String subject   = client.propsHandler.getProperty("usrname");
	String comment   = commText.getText();
	String pubKeyStr = null;

	if(subject == null) {
	    subject = SSH.VER_MINDTERM;
	}

	if("RSA (ssh1)".equals(choiceType.getSelectedItem())) {
	    pubKeyStr = SSH.generateKeyFiles((RSAPrivateCrtKey)kp.getPrivate(),
					     expandFileName(fileName), passwd,
					     comment);
	} else {
	    SSH2PublicKeyFile pkif = new SSH2PublicKeyFile(kp.getPublic(),
							   subject, comment);

	    // When key is unencrypted OpenSSH doesn't tolerate headers...
	    //
	    if(passwd == null || passwd.length() == 0) {
		subject = null;
		comment = null;
	    }

	    SSH2KeyPairFile kpf = new SSH2KeyPairFile(kp, subject, comment);

	    kpf.store(expandFileName(fileName), SSH.secureRandom(), passwd);

	    pubKeyStr = pkif.store(expandFileName(fileName + ".pub"),
				   !cbOpenSSH.getState());
	}

	Terminal term = client.sshStdIO.getTerminal();
	if(term instanceof TerminalWin) {
	    ((TerminalWin)term).getClipboard().setSelection(pubKeyStr);
	}

	okBut.setEnabled(true);
	pwdText.setText("");
	pwdText2.setText("");
	progBar.setValue(0);
	setDefaultFileName(client);
    }

    private static void resetValues() {
	okBut.setEnabled(true);
	choiceBits.select("1024");
	setDefaultFileName(client);
	generatedAndSaved = false;
	pwdText.setText("");
	pwdText2.setText("");
	descText.setText(keyGenerationHelp);
	okBut.setLabel("Generate");
	cardLayout.show(cardPanel, "first");

	RandomSeed seed = client.randomSeed();
	seed.resetEntropyCount();
	progBar.setValue(0);
	seed.addProgress(progBar);
    }

    private static boolean checkValues(String passwd, String passwd2,
				       String fileName)
    {
	if(!passwd.equals(passwd2)) {
	    alert("Please give same password twice");
	    return false;
	}
	if(fileName.length() == 0) {
	    alert("Filename can't be empty");
	    return false;
	}

	OutputStream out = getOutput(fileName);
	if(out == null) {
	    alert("Can't open '" + fileName + "' for saving.");
	    return false;
	}
	try { out.close(); } catch (Exception e) { /* don't care */ }

	return true;
    }

    private static OutputStream getOutput(String fileName) {
	fileName = expandFileName(fileName);
	FileOutputStream f;
	try {
	    f = new FileOutputStream(fileName);
	    return f;
	} catch (Throwable t) {
	    return null;
	}
    }

    private static String expandFileName(String fileName) {
	if(fileName.indexOf(File.separator) == -1)
	    fileName = client.propsHandler.getSSHHomeDir() + fileName;
	return fileName;
    }

    private static Dialog     editKeyDialog;
    private static FileDialog editKeyLoad;
    private static TextField  fileTextEd;
    private static TextField  pwdTextEd;
    private static TextField  pwdText2Ed;
    private static TextField  subjTextEd;
    private static TextField  commTextEd;
    private static Label      typeLbl;
    private static Label      bitLbl;
    private static Checkbox   cbOpenSSHEd;
    private static Checkbox   cbSSHComEd;
    private static Button     okButEd;
    private static Button     cancButEd;

    private static SSH2KeyPairFile   kpf = new SSH2KeyPairFile();
    private static SSH2PublicKeyFile pkf = new SSH2PublicKeyFile();

    public static void editKeyDialog(Frame par, SSHInteractiveClient cli) {
	parent = par;
	client = cli;
	if(editKeyLoad == null) {
	    editKeyLoad = new FileDialog(parent, "MindTerm - Select key file to edit", FileDialog.LOAD);
	}
	editKeyLoad.setDirectory(client.propsHandler.getSSHHomeDir());
	editKeyLoad.setVisible(true);

	String fileName = editKeyLoad.getFile();
	String dirName  = editKeyLoad.getDirectory();
	String passwd   = null;

	kpf = new SSH2KeyPairFile();
	pkf = new SSH2PublicKeyFile();

	if(fileName != null && fileName.length() > 0) {
	    if(!dirName.endsWith(File.separator)) {
		dirName += File.separator;
	    }
	    fileName = dirName + fileName;

	    try {
		pkf.load(fileName + ".pub");
	    } catch (Exception e) {
		pkf = null;
	    }
	    boolean retryPasswd = false;
	    do {
		try {
		    kpf.load(fileName, passwd);
		    break;
		} catch(SSH2AccessDeniedException e) {
		    /* Retry... */
		    retryPasswd = true;
		} catch(Exception e) {
		    alert("Error loading key file: " + e.getMessage());
		}
	    } while((passwd = SSHMiscDialogs.password("MindTerm - File Password",
						      "Please give password for " +
						      fileName,
						   parent)) != null);
	    if(retryPasswd && passwd == null) {
		return;
	    }
	} else {
	    return;
	}

	if(pkf == null) {
	    pkf = new SSH2PublicKeyFile(kpf.getKeyPair().getPublic(),
					kpf.getSubject(), kpf.getComment());
	}

	if(editKeyDialog == null) {
	    editKeyDialog = new Dialog(parent, "MindTerm - Publickey Keypair Edit", true);

	    AWTGridBagContainer grid = new AWTGridBagContainer(editKeyDialog);

	    Label label = new Label("Key type/format:");
	    grid.add(label, 0, 2);

	    typeLbl = new Label("DSA");
	    grid.add(typeLbl, 0, 2);

	    label = new Label("Key length (bits):");
	    grid.add(label, 1, 2);

	    bitLbl = new Label("1024");
	    grid.add(bitLbl, 1, 2);

	    label = new Label("Identity file:");
	    grid.add(label, 2, 2);

	    fileTextEd = new TextField("", 18);
	    grid.add(fileTextEd, 2, 2);

	    grid.getConstraints().fill = GridBagConstraints.HORIZONTAL;

	    label = new Label("Password:");
	    grid.add(label, 3, 2);

	    pwdTextEd = new TextField("", 18);
	    pwdTextEd.setEchoChar('*');

	    grid.add(pwdTextEd, 3, 2);

	    label = new Label("Password again:");
	    grid.add(label, 4, 2);

	    pwdText2Ed = new TextField("", 18);
	    pwdText2Ed.setEchoChar('*');
	    grid.add(pwdText2Ed, 4, 2);

	    label = new Label("Subject:");
	    grid.add(label, 5, 2);

	    subjTextEd = new TextField("", 18);
	    grid.add(subjTextEd, 5, 2);

	    label = new Label("Comment:");
	    grid.add(label, 6, 2);

	    commTextEd = new TextField("", 18);
	    grid.add(commTextEd, 6, 2);

	    cbSSHComEd = new Checkbox("SSH Comm. private file format");
	    grid.add(cbSSHComEd, 7, 4);

	    cbOpenSSHEd = new Checkbox("OpenSSH public public format");
	    grid.add(cbOpenSSHEd, 8, 4);

	    Panel bp = new Panel(new FlowLayout());

	    bp.add(okButEd = new Button("Save"));
	    okButEd.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    String fName = fileTextEd.getText();
		    String pwd   = pwdTextEd.getText();
		    if(checkValues(pwd, pwdText2Ed.getText(),
				   fName)) {
			fName = expandFileName(fName);
			try {
			    String s = subjTextEd.getText();
			    String c = commTextEd.getText();
			    pkf.setSubject(s);
			    pkf.setComment(c);
			    pkf.store(fName + ".pub", !cbOpenSSHEd.getState());
			    if(!cbSSHComEd.getState() &&
			       (pwd == null || pwd.length() == 0)) {
				s = null;
				c = null;
			    }
			    kpf.setSubject(s);
			    kpf.setComment(c);
			    kpf.store(fName, SSH.secureRandom(), pwd,
				      cbSSHComEd.getState());
			    editKeyDialog.setVisible(false);
			} catch (Exception ee) {
			    alert("Error saving files: " + ee.getMessage());
			}
		    }
		}
	    });
	    bp.add(cancButEd = new Button("Cancel"));

	    cancButEd.addActionListener(new AWTConvenience.CloseAction(editKeyDialog));;

	    grid.add(bp, 9, GridBagConstraints.REMAINDER);

	    editKeyDialog.addWindowListener(new AWTConvenience.CloseAdapter(cancButEd));

	    AWTConvenience.setBackgroundOfChildren(editKeyDialog);
	    AWTConvenience.setKeyListenerOfChildren(editKeyDialog,
						    new AWTConvenience.OKCancelAdapter(okButEd, cancButEd),
						    null);
	    editKeyDialog.pack();
	}

	fileTextEd.setText(fileName);
	pwdTextEd.setText(passwd);
	pwdText2Ed.setText(passwd);
	typeLbl.setText(kpf.getAlgorithmName());
	bitLbl.setText(String.valueOf(kpf.getBitLength()));
	subjTextEd.setText(kpf.getSubject());
	commTextEd.setText(kpf.getComment());
	cbSSHComEd.setState(kpf.isSSHComFormat());
	cbOpenSSHEd.setState(!pkf.isSSHComFormat());

	AWTConvenience.placeDialog(editKeyDialog);

	editKeyDialog.setVisible(true);
    }

}
