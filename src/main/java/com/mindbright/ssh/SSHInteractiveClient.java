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
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Vector;
import java.util.Enumeration;

import java.awt.Image;
import java.awt.Toolkit;

import com.mindbright.jca.security.NoSuchAlgorithmException;

import com.mindbright.ssh2.*;

import com.mindbright.terminal.*;

import com.mindbright.net.*;

import com.mindbright.gui.Logo;

public final class SSHInteractiveClient extends SSHClient
  implements Runnable, SSHInteractor, SSH2Interactor {

    public boolean       isSSH2 = false;
    public SSH2Transport transport;
    SSH2Connection       connection;
    SSH2TerminalAdapter  termAdapter;

  public static boolean wantHelpInfo       = true;
  public static String  customStartMessage = null;

  SSHMenuHandler     menus;
  SSHStdIO           sshStdIO;
  SSHPropertyHandler propsHandler;

  public boolean quiet;
  public boolean exitOnLogout;
  boolean        initQuiet;

  public SSHInteractiveClient(boolean quiet, boolean exitOnLogout,
			      SSHPropertyHandler propsHandler) {
    super(propsHandler, propsHandler);

    this.propsHandler = propsHandler;
    this.interactor   = this; // !!! OUCH

    propsHandler.setInteractor(this);
    propsHandler.setClient(this);

    this.quiet        = quiet;
    this.exitOnLogout = exitOnLogout;
    this.initQuiet    = quiet;

    setConsole(new SSHStdIO());
    sshStdIO = (SSHStdIO)console;
    sshStdIO.setClient(this);
  }

  public SSHInteractiveClient(SSHInteractiveClient clone) {
    this(true, true, new SSHPropertyHandler(clone.propsHandler));

    this.activateTunnels = false;

    this.wantHelpInfo       = clone.wantHelpInfo;
    this.customStartMessage = clone.customStartMessage;
  }

  public void setMenus(SSHMenuHandler menus) {
    this.menus = menus;
  }

  public SSHPropertyHandler getPropertyHandler() {
      return propsHandler;
  }

  public void updateMenus() {
    if(menus != null)
      menus.update();
  }

  public void splashScreen() {
      TerminalWin t = getTerminalWin();

      if(t != null) {
	  t.clearScreen();
	  t.cursorSetPos(0, 0, false);
      }

      console.println(Version.copyright);
      console.println(Version.licenseMessage);

      /* !!! REMOVE
      int col = (t.cols() / 2) - (copyright.length() / 2) - 1;
      t.cursorSetPos(t.rows() - 5, col, false);
      */

      showLogo();

      if((menus != null) && menus.havePopupMenu) {
	  if(t != null) {
	      t.cursorSetPos(t.rows() - 3, 0, false);
	  }
	  console.println("\r\33[2Kpress <ctrl> + <mouse-" + menus.getPopupButton() + "> for Menu");
      }
      if(propsHandler.getSSHHomeDir() != null) {
	  if(t != null) {
	      t.cursorSetPos(t.rows() - 2, 0, false);
	  }
	  console.println("\r\33[2KMindTerm home: " + propsHandler.getSSHHomeDir());
      }
      if(t != null) {
	  t.cursorSetPos(t.rows() - 1, 0, false);
      }
  }

  public boolean installLogo() {
      boolean isPresent = false;

      TerminalWin t = getTerminalWin();

      if(t != null) {
	  ByteArrayOutputStream baos = readResource("/defaults/logo.gif");
	  if(baos != null) {
	      byte[] img = baos.toByteArray();
	      Image logo = Toolkit.getDefaultToolkit().createImage(img);
	      int width  = -1;
	      int height = -1;
	      boolean ready = false;

	      while (!ready) {
		  width  = logo.getWidth(null);
		  height = logo.getHeight(null);
		  if(width != -1 && height != -1) {
		      ready = true;
		  }
		  Thread.yield();
	      }

	      t.setLogo(logo, -1, -1, width, height);

	      isPresent = true;
	  }
      }

      return isPresent;
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

  void initRandomSeed() {
      if(!SSH.haveSecureRandom()) {
	  console.print("Initializing random generator, please wait...");
	  SSH.initSeedGenerator();
	  console.print("done");
      }
  }

    public void doSingleCommand(String commandLine)
	throws Exception
    {
	boolean haveDumbConsole = (propsHandler.wantPTY() && isDumb());

	this.commandLine = commandLine;

	try {
	    netscape.security.PrivilegeManager.enablePrivilege("TerminalEmulator");
	} catch (netscape.security.ForbiddenTargetException e) {
	    // !!! REMOVE
	    console.println("Unsigned applet, can only connect to www host, tunneling can't be used");
	    console.println("");
	}

	installLogo();

	splashScreen();
	initRandomSeed();
	startSSHClient(false);
  }

  public void run() {
      boolean gotExtMsg;

      try {
	  netscape.security.PrivilegeManager.enablePrivilege("TerminalEmulator");
      } catch (netscape.security.ForbiddenTargetException e) {
	  // !!! REMOVE
	  console.println("Unsigned applet, can only connect to www host, tunneling can't be used");
	  console.println("");
      }

      installLogo();

      boolean keepRunning = true;
      while(keepRunning) {
	  gotExtMsg = false;
	  try {
	      splashScreen();

	      initRandomSeed();

	      startSSHClient(true);

	      if(sshStdIO.isConnected()) {
		  // Server died on us without sending disconnect
		  sshStdIO.serverDisconnect("\n\r\n\rServer died or connection lost");
		  disconnect(false);
		  propsHandler.clearServerSetting();
	      }

	      // !!! Wait for last session to close down entirely (i.e. so
	      // disconnected gets a chance to be called...)
	      //
	      Thread.sleep(1000);

	      try {
		  propsHandler.checkSave();
	      } catch (IOException e) {
		  alert("Error saving settings!");
	      }

	  } catch(SSHClient.AuthFailException e) {
	      alert("Authentication failed, " + e.getMessage());
	      propsHandler.clearPasswords();

	  } catch(WebProxyException e) {
	      alert(e.getMessage());
	      propsHandler.clearPasswords();
	      
	  } catch(SSHStdIO.SSHExternalMessage e) {
	      gotExtMsg = true;
	      String msg = e.getMessage();

	      // !!! REMOVE
	      if(msg != null && msg.trim().length() > 0) {
		  alert(e.getMessage());
	      }

	  } catch(UnknownHostException e) {
	      String host = e.getMessage();
	      if(propsHandler.getProperty("proxytype").equals("none")) {
		  alert("Unknown host: " + host);
	      } else {
		  alert("Unknown proxy host: " + host);
	      }
	      propsHandler.clearServerSetting();

	  } catch(FileNotFoundException e) {
	      alert("File not found: " + e.getMessage());

	  } catch(Exception e) {
	      String msg = e.getMessage();
	      if(msg == null || msg.trim().length() == 0)
		  msg = e.toString();
	      msg = "Error connecting to " + propsHandler.getProperty("server") + ", reason:\n" +
		  "-> " + msg;
	      alert(msg);
	      if(SSH.DEBUGMORE) {
		  System.err.println("If an error occured, please send the below stacktrace to mats@mindbright.se");
		  e.printStackTrace();
	      }

	  } catch(ThreadDeath death) {
	      if(controller != null)
		  controller.killAll();
	      controller = null;
	      throw death;
	  }

	  propsHandler.passivateProperties();
	  activateTunnels = true;

	  if(!gotExtMsg) {
	      if(!propsHandler.savePasswords || usedOTP) {
		  propsHandler.clearPasswords();
	      }
	      propsHandler.currentPropsFile = null;
	      if(!propsHandler.autoLoadProps) {
		  propsHandler.clearPasswords();
		  initQuiet = false;
	      }
	      quiet = false;
	  }

	  controller = null;

	  TerminalWin t = getTerminalWin();
	  if(t != null)
	      t.setTitle(null);

	  keepRunning = !exitOnLogout;
      }
  }

    private void startSSHClient(boolean shell)
	throws Exception
    {
	// This starts a connection to the sshd and all the related stuff...
	//
	bootSSH(shell, true);

	int    major = 2;
	String proto = propsHandler.getProperty("protocol");

	if("auto".equals(proto)) {
	    sshSocket = new SSHVersionSpySocket(sshSocket);
	    major = ((SSHVersionSpySocket)sshSocket).getMajorVersion();
	} else if("ssh1".equals(proto)) {
	    major = 1;
	}

	if(major == 1) {
	    console.println("Warning connecting using ssh1, consider upgrading server!");
	    console.println("");
	    boot(shell, sshSocket);

	    // !!! REMOVE
	    if(isDumb())
		System.out.println("No console...");

	    // Join main receiver channel thread and wait for session to end
	    //
	    controller.waitForExit();
	} else {
	    runSSH2Client();
	}
    }

  public boolean isDumb() {
    return (console.getTerminal() == null);
  }

  public TerminalWin getTerminalWin() {
    Terminal term = console.getTerminal();
    if(term != null && term instanceof TerminalWin)
      return (TerminalWin)term;
    return null;
  }

  public void showLogo() {
      TerminalWin t = getTerminalWin();
      if(t != null) {
	  t.showLogo();
      }
  }

  public void hideLogo() {
      TerminalWin t = getTerminalWin();
      if(t != null) {
	  t.hideLogo();
      }
  }

  public Logo getLogo() {
      Logo logo = null;
      TerminalWin t = getTerminalWin();
      if(t != null) {
	  Image img = t.getLogo();
	  logo = new Logo(img);
      }
      return logo;
  }

  public void updateTitle() {
    sshStdIO.updateTitle();
  }

  //
  // SSH2Interactor interface
  //
    public String promptLine(String prompt, boolean echo)
	throws SSH2UserCancelException {
	try {
	    if(echo) {
		return promptLine(prompt, "");
	    } else {
		return promptPassword(prompt);
	    }
	} catch (IOException e) {
	    throw new SSH2UserCancelException(e.getMessage());
	}
    }

    public String[] promptMulti(String[] prompts, boolean[] echos)
	throws SSH2UserCancelException {
	return promptMultiFull(null, null, prompts, echos);
    }

    public String[] promptMultiFull(String name, String instruction,
			     String[] prompts, boolean[] echos)
	throws SSH2UserCancelException {
	try {
	    console.println(name);
	    console.println(instruction);
	    String[] resp = new String[prompts.length];
	    for(int i = 0; i < prompts.length; i++) {
		if(echos[i]) {
		    resp[i] = promptLine(prompts[i], "");
		} else {
		    resp[i] = promptPassword(prompts[i]);
		}
	    }
	    return resp;
	} catch (IOException e) {
	    throw new SSH2UserCancelException(e.getMessage());
	}
    }

    public int promptList(String name, String instruction, String[] choices)
	throws SSH2UserCancelException {
	try {
	    console.println(name);
	    console.println(instruction);
	    for(int i = 0; i < choices.length; i++) {
		console.println(i + ") " + choices[i]);
	    }
	    String choice = promptLine("Choice", "0");
	    return Integer.parseInt(choice);
	} catch (Exception e) {
	    throw new SSH2UserCancelException(e.getMessage());
	}
    }
    
  //
  // SSHInteractor interface
  //
  public void propsStateChanged(SSHPropertyHandler props) {
      updateMenus();
  }

  public void startNewSession(SSHClient client) {
      // !!! REMOVE
      // Here we can have a login-dialog with proxy-info also (or configurable more than one method)
      // !!!
  }

  public void sessionStarted(SSHClient client) {
      quiet = initQuiet;
  }

  public boolean quietPrompts() {
      return (commandLine != null || quiet);
  }

  public boolean isVerbose() {
      return wantHelpInfo;
  }

  public String promptLine(String prompt, String defaultVal) throws IOException {
    return sshStdIO.promptLine(prompt, defaultVal, false);
  }

  public String promptPassword(String prompt) throws IOException {
      return sshStdIO.promptLine(prompt, "", true);
  }

  public boolean askConfirmation(String message, boolean defAnswer) {
    boolean confirm = false;
    try {
      confirm = askConfirmation(message, true, defAnswer);
    } catch (IOException e) {
	// !!!
    }
    return confirm;
  }

  public boolean askConfirmation(String message, boolean preferDialog,
				 boolean defAnswer)
      throws IOException {
    boolean confirm = false;
    if(menus != null && preferDialog) {
      confirm = menus.confirmDialog(message, defAnswer);
    } else {
      String answer = promptLine(message + (defAnswer ? " ([yes]/no) " : "(yes/[no]) "), "");
      if(answer.equalsIgnoreCase("yes") || answer.equals("y")) {
	confirm = true;
      } else if(answer.equals("")) {
	confirm = defAnswer;
      }
    }
    return confirm;
  }

  public boolean licenseDialog(String license) {
      if(license != null && menus instanceof SSHMenuHandlerFull) {
          return SSHMiscDialogs.confirm("MindTerm - License agreeement",
                                        license,
                                        24, 80, "Accept", "Decline",
                                        false,
                                        ((SSHMenuHandlerFull)menus).parent,
                                        true);
      }
      return false;
  }

  public void connected(SSHClient client) {
      updateMenus();
      console.println("Connected to server running " + srvVersionStr);
  }

  public void open(SSHClient client) {
      updateMenus();
      updateTitle();
  }

  public void disconnected(SSHClient client, boolean graceful) {
      sshStdIO.breakPromptLine("Login aborted by user");
      updateMenus();
      updateTitle();
  }

  public void report(String msg) {
      if(msg != null && msg.length() > 0) {
	  console.println(msg);
      }
      console.println("");
  }

  public void alert(String msg) {
      if(menus != null) {
	  if(msg.length() < 50)
	      menus.alertDialog(msg);
	  else
	      menus.textDialog("MindTerm - Alert", msg, 4, 38, true);
      } else {
	  report(msg);
      }
  }

    public void forcedDisconnect() {
	if(isSSH2) {
	    transport.normalDisconnect("Closed by user");
	} else {
	    super.forcedDisconnect();
	}
    }

    public void requestLocalPortForward(String localHost, int localPort,
					String remoteHost, int remotePort,
					String plugin)
	throws IOException
    {
	if(isSSH2) {
	    SSH2StreamFilterFactory filter = null;
	    if("ftp".equals(plugin)) {
		filter = new SSH2FTPProxyFilter(localHost);
	    }
	    connection.newLocalForward(localHost, localPort,
				       remoteHost, remotePort, filter);
	} else {
	    super.requestLocalPortForward(localHost, localPort,
					  remoteHost, remotePort, plugin);
	}
    }

    public void addRemotePortForward(int remotePort, String localHost, int localPort, String plugin) {
	super.addRemotePortForward(remotePort, localHost, localPort, plugin);
	if(isSSH2) {
	    connection.newRemoteForward("127.0.0.1", remotePort,
					localHost, localPort);
	}
    }

    public void delLocalPortForward(String localHost, int port) {
	boolean isop = isOpened;
	if(isSSH2) {
	    connection.deleteLocalForward(localHost, port);
	    isOpened = false;
	}
	super.delLocalPortForward(localHost, port);
	isOpened = isop;
    }

    public void delRemotePortForward(int port) {
	if(isSSH2) {
	    connection.deleteRemoteForward("127.0.0.1", port);
	}
	super.delRemotePortForward(port);
    }

    void runSSH2Client() throws IOException {
	try {
	    SSH2TransportPreferences prefs;
	    isSSH2 = true;
	    prefs  = new SSH2TransportPreferences(propsHandler.getProperties());

	    transport = new SSH2Transport(sshSocket,
					  prefs,
					  secureRandom());

	    transport.setEventHandler(new SSH2TransportEventAdapter() {
		    public boolean kexAuthenticateHost(SSH2Transport tp,
					       SSH2Signature serverHostKey) {
			try {
			    return propsHandler.verifyKnownSSH2Hosts(
						     SSHInteractiveClient.this,
						     serverHostKey);
			} catch (SSH2Exception e) {
			    transport.getLog().error("SSHInteractiveClient",
						     "verifyKnownSSH2Hosts",
						     "Error " + e.getMessage());
			} catch (IOException e) {
			    transport.getLog().error("SSHInteractiveClient",
						     "verifyKnownSSH2Hosts",
						     "Error " + e.getMessage());
			}
			return false;
		    }
		});


	    transport.boot();

	    srvVersionStr = transport.getServerVersion();
	    connected(null);

	    transport.waitForKEXComplete();

	    if(!transport.isConnected()) {
		throw new IOException("Error in key exchange");
	    }

	    isConnected = true;

	    SSH2Authenticator authenticator =
		new SSH2Authenticator() {
			public void peerMethods(String methods) {
			    addAuthModules(this, methods);
			}
		    };

	    authenticator.setUsername(propsHandler.getUsername(null));

	    SSH2UserAuth userAuth = new SSH2UserAuth(transport, authenticator);
	    if(!userAuth.authenticateUser("ssh-connection")) {
		throw new AuthFailException("permission denied");
	    }

	    connection = new SSH2Connection(userAuth, transport, null, null);
	    connection.setEventHandler(new SSH2ConnectionEventAdapter() {
		    public void localSessionConnect(SSH2Connection connection,
						    SSH2Channel channel) {
			// !!! REMOVE
		    }
		    public void localDirectConnect(SSH2Connection connection,
						   SSH2Listener listener,
						   SSH2Channel channel) {
			tunnels.addElement(channel);
		    }
		    public void remoteForwardConnect(SSH2Connection connection,
						     String remoteAddr, int remotePort,
						     SSH2Channel channel) {
			tunnels.addElement(channel);
		    }
		    public void channelClosed(SSH2Connection connection,
					      SSH2Channel channel) {
			tunnels.removeElement(channel);
		    }
		});
	    transport.setConnection(connection);

	    if(console != null)
		console.serverConnect(null, null);
	    isOpened = true;
	    open(null);

	    // !!! Ouch
	    // Activate tunnels at this point
	    //
	    propsHandler.passivateProperties();
	    propsHandler.activateProperties();

	    // !!! Ouch
	    // Start ftpd if doing ftp to sftp bridge
	    //
	    String ftpdHost = propsHandler.getProperty("sftpbridge-host");
	    String ftpdPort = propsHandler.getProperty("sftpbridge-port");
	    if(menus != null &&
	       menus instanceof SSHMenuHandlerFull &&
	       ftpdHost != null && ftpdHost.trim().length() > 0) {
		try {
		    ((SSHMenuHandlerFull)menus).startFtpdLoop(ftpdHost,
							      ftpdPort);
		    console.println("");
		    console.println("Starting ftp to sftp bridge on " +
				    ftpdHost + ":" + ftpdPort);
		    console.println("");
		} catch (Exception e) {
		    console.println("");
		    console.println("Error starting ftp to sftp bridge on " +
				    ftpdHost + ":" + ftpdPort + " - " +
				    e.getMessage());
		    console.println("");
		}
	    }

	    TerminalWin           terminal   = getTerminalWin();
	    TerminalInputListener inListener = null;

	    SSH2SessionChannel session;
	    if(terminal != null) {
		terminal.addInputListener(inListener =
					  new TerminalInputAdapter() {
						  public void signalWindowChanged(int rows, int cols,
										  int vpixels,
										  int hpixels) {
						      updateTitle();
						  }
					      });
		termAdapter = new SSH2TerminalAdapterImpl(terminal);
		session = connection.newTerminal(termAdapter);
		if(session.openStatus() != SSH2Channel.STATUS_OPEN) {
		    System.err.println("** Failed to open ssh2 session channel");
		    throw new IOException("Failed to open ssh2 session channel");
		}

		if(user.wantX11Forward()) {
		    System.err.println("** Got X11 forward? " +
				       session.requestX11Forward(false, 0));
		}
		if(user.wantPTY()) {
		    System.err.println("** Got pty? " + session.requestPTY("xterm",
									   terminal.rows(),
									   terminal.cols(),
									   null));
		}
		if(commandLine != null) {
		    System.err.println("** Could run command? " +
				       session.doSingleCommand(commandLine));
		} else {
		    System.err.println("** Got shell? " + session.doShell());
		}
	    } else {
		session = connection.newSession();
	    }

	    int status = session.waitForExit(0);

	    if(menus != null && menus instanceof SSHMenuHandlerFull) {
		((SSHMenuHandlerFull)menus).stopFtpdLoop();
	    }

	    if(terminal != null) {
		terminal.removeInputListener(inListener);
	    }
	    termAdapter.detach();

	    transport.normalDisconnect("Disconnect by user");

	    console.serverDisconnect(getServerAddr().getHostName() + " disconnected: " + status);
	    disconnect(true);

	    if(propsHandler.getCompressionLevel() != 0) {
		SSH2Compressor comp;
		for(int i = 0; i < 2; i++) {
		    comp = (i == 0 ? transport.getTxCompressor() :
			    transport.getRxCompressor());
		    if(comp != null) {
			String msg;
			long compressed, uncompressed;
			compressed   = comp.numOfCompressedBytes();
			uncompressed = (comp.numOfUncompressedBytes() > 0 ?
					comp.numOfUncompressedBytes() : 1);
			msg = " raw data (bytes) = " + uncompressed +
			    ", compressed = " + compressed + " (" +
			    ((compressed * 100) / uncompressed) + "%)";
			console.println((i == 0 ? "outgoing" : "incoming") +
					msg);
		    }
		}
	    }

	    sshStdIO.setTerminal(terminal);
	} catch (IOException e) {
	    disconnect(false);
	    throw e;
	} catch (Exception e) {
	    System.err.println("** Error in ssh2: ");
	    e.printStackTrace();
	    disconnect(false);
	    throw new IOException("Error in ssh2: " + e.getMessage());
	} finally {
	    isSSH2 = false;
	}
    }

    public void addAuthModules(SSH2Authenticator authenticator, String methods)
    {
	try {
	    int[] authTypes = propsHandler.getAuthTypes(null);
	    for(int i = 0; i < authTypes.length; i++) {
		int type = authTypes[i];
		if(!SSH2ListUtil.isInList(methods, SSH.getAuthName(type)) &&
		   !SSH2ListUtil.isInList(methods, SSH.getAltAuthName(type))) {
		    report("Authentication method '" + SSH.getAuthName(type) +
			   "' not supported by server.");
		    continue;
		}
		switch(type) {
		case AUTH_PUBLICKEY:
		    String keyFile = propsHandler.getProperty("idfile");
		    if(keyFile.indexOf(File.separator) == -1) {
			keyFile = propsHandler.getSSHHomeDir() + keyFile;
		    }
		    
		    try {
			netscape.security.PrivilegeManager.enablePrivilege("UniversalFileAccess");
		    } catch (netscape.security.ForbiddenTargetException e) {
			// !!!
		    }

		    SSH2KeyPairFile kpf = new SSH2KeyPairFile();
		    try {
			kpf.load(keyFile, "");
		    } catch (SSH2FatalException e) {
			throw new IOException(e.getMessage());
		    } catch (SSH2AccessDeniedException e) {
			String prompt = "key file '" + keyFile + "' password: ";
			String passwd =
			    propsHandler.getIdentityPassword(prompt);
			kpf.load(keyFile, passwd);
		    }

		    String        alg  = kpf.getAlgorithmName();
		    SSH2Signature sign = SSH2Signature.getInstance(alg);

		    sign.initSign(kpf.getKeyPair().getPrivate());
		    sign.setPublicKey(kpf.getKeyPair().getPublic());

		    authenticator.addModule("publickey",
					    new SSH2AuthPublicKey(sign));

		    break;
		case AUTH_PASSWORD:
		    authenticator.addModule("password",
					    new SSH2AuthPassword(propsHandler.getPassword(null)));
		    break;
		case AUTH_SDI:
		case AUTH_TIS:
		case AUTH_CRYPTOCARD:
		case AUTH_KBDINTERACT:
		    authenticator.addModule("keyboard-interactive",
					    new SSH2AuthKbdInteract(this));
		    break;
		default:
		    throw new IOException("Authentication type " +
					  authTypeDesc[authTypes[i]] +
					  " is not supported in SSH2");
		}
	    }
	} catch (Exception e) {
	    alert("Error when setting up authentication: " + e.getMessage());
	}
    }

    public void newShell() {
	java.awt.MenuBar menubar = new java.awt.MenuBar();
	final java.awt.Frame   frame = new java.awt.Frame();

	final TerminalWin terminal;

	frame.setMenuBar(menubar);
	frame.addNotify();
	frame.validate();

	terminal = new TerminalWin(frame, new TerminalXTerm(),
				   console.getTerminal().getProperties());
	SSH2TerminalAdapter termAdapter = new SSH2TerminalAdapterImpl(terminal);

	terminal.addInputListener(new TerminalInputAdapter() {
		public void signalWindowChanged(int rows, int cols,
						int vpixels, int hpixels) {
		    if(terminal.getTitle() != null) {
			String title = propsHandler.getProperty("usrname");
			title += "@" + propsHandler.getProperty("server");
			title += " <CLONE>";
			title += " [" + cols + "x" + rows + "]";
			frame.setTitle(title);
		    }
		}
	    });

	final SSH2SessionChannel session = connection.newTerminal(termAdapter);

	TerminalMenuHandlerFull tmenus = new TerminalMenuHandlerFull("MindTerm");
	tmenus.setTerminalWin(terminal);
	terminal.setMenus(tmenus);
	menubar.add(tmenus.getMenu(0));
	menubar.add(tmenus.getMenu(1));
	menubar.add(tmenus.getMenu(2));
	tmenus.setTerminalMenuListener(new TerminalMenuListener() {
		public void update() {
		}
		public void close(TerminalMenuHandler originMenu) {
		    session.doExit(0, false);
		}
	    });

	terminal.setClipboard(GlobalClipboard.getClipboardHandler(tmenus));

	SSH.randomSeed.addEntropyGenerator(terminal);

	frame.setLayout(new java.awt.BorderLayout());
	frame.add(terminal.getPanelWithScrollbar(),
		  java.awt.BorderLayout.CENTER);

	frame.pack();
	frame.show();

	frame.addWindowListener(new java.awt.event.WindowAdapter() {
	    public void windowClosing(java.awt.event.WindowEvent e) {
		session.doExit(0, false);
	    }
	});

	if(session.openStatus() != SSH2Channel.STATUS_OPEN) {
	    System.err.println("** Failed to open ssh2 session channel");
	    frame.dispose();
	    return;
	}

	if(user.wantX11Forward()) {
	    transport.getLog().info("MindTerm2", "got X11 forward? " +
				    session.requestX11Forward(false, 0));
	}
	if(user.wantPTY()) {
	    transport.getLog().info("MindTerm2", "got pty? " +
				    session.requestPTY("xterm",
						       terminal.rows(),
						       terminal.cols(),
						       null));
	}

	transport.getLog().info("MindTerm2", "got shell? " + session.doShell());

	session.waitForExit(0);

	GlobalClipboard.getClipboardHandler().removeMenuHandler(tmenus);

	frame.dispose();
    }

    public String getVersionId(boolean client) {
	String idStr = "SSH-" + SSH_VER_MAJOR + "." + SSH_VER_MINOR + "-";
	idStr += propsHandler.getProperty("package-version");
	return idStr;
    }

    public void closeTunnelFromList(int listIdx) {
	if(isSSH2) {
	    SSH2Channel c = (SSH2Channel)tunnels.elementAt(listIdx);
	    c.close();
	} else {
	    controller.closeTunnelFromList(listIdx);
	}
    }

    private Vector tunnels = new Vector();
    public String[] listTunnels() {
	if(isSSH2) {
	    String[] list   = new String[tunnels.size()];
	    Enumeration e   = tunnels.elements();
	    int         cnt = 0;
	    while(e.hasMoreElements()) {
		SSH2TCPChannel c = (SSH2TCPChannel)e.nextElement();
		list[cnt++] = c.toString();
	    }
	    return list;
	} else {
	    return controller.listTunnels();
	}
    }

}
