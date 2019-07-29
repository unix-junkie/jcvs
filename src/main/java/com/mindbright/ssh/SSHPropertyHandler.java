/******************************************************************************
 *
 * Copyright (c) 1999-2001 AppGate AB. All Rights Reserved.
 * 
 * This file contains Original Code and/or Modifications of Original Code as
 * defined in and that are subject to the MindTerm Public Source License
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
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.Hashtable;
import java.util.Properties;
import java.util.NoSuchElementException;
import java.util.Enumeration;
import java.util.Date;

import com.mindbright.jca.security.MessageDigest;
import com.mindbright.jca.security.interfaces.DSAPublicKey;
import com.mindbright.jca.security.interfaces.RSAPublicKey;

import com.mindbright.net.*;
import com.mindbright.terminal.*;
import com.mindbright.util.EncryptedProperties;
import com.mindbright.util.ASCIIArmour;

import com.mindbright.ssh2.*;

public final class SSHPropertyHandler implements SSHClientUser, SSHAuthenticator, ProxyAuthenticator {

    static public final int PROP_NAME    = 0;
    static public final int PROP_VALUE   = 1;

    static public final String PROPS_FILE_EXT  = ".mtp";
    static public final String DEF_IDFILE      = "identity";

    public static String hostKeyAlgs  = "ssh-rsa,ssh-dss";
    public static String cipherAlgs   = "aes128-cbc,blowfish-cbc,twofish128-cbc,aes192-cbc,aes256-cbc,twofish-cbc,cast128-cbc,3des-cbc,arcfour";
    public static String macAlgs      = "hmac-md5,hmac-sha1,hmac-sha1-96,hmac-md5-96,hmac-ripemd160";
    public static String ciphAlgsSort = "aes128-cbc,aes192-cbc,aes256-cbc,blowfish-cbc,twofish128-cbc,twofish192-cbc,twofish256-cbc,cast128-cbc,3des-cbc,arcfour";

    static public final Properties defaultProperties = new Properties();
    static public final Hashtable  defaultPropNames  = new Hashtable();
    static public final Hashtable  oldPropNames      = new Hashtable();
    static public final String[][] defaultPropDesc = {
	{ "protocol",        "auto"                                },
	{ "server",          null                                  },
	{ "real-server",     null                                  },
	{ "local-bind",      "127.0.0.1"                           },
	{ "port",            String.valueOf(SSH.DEFAULTPORT)       },
	{ "proxy-type",      "none"                                },
	{ "proxy-host",      null                                  },
	{ "proxy-port",      null                                  },
	{ "proxy-user",      null                                  },
	{ "proxy-proto",     null                                  },
	{ "username",        null                                  },
	{ "password",        null                                  },
	{ "tispassword",     null                                  },
	{ "passphrase",      null                                  },
	{ "proxy-password",  null                                  },
	{ "ssh1-cipher",     SSH.getCipherName(SSH.CIPHER_DEFAULT) },
	{ "auth-method",     "password"                            },
	{ "private-key",     DEF_IDFILE                            },
	{ "display",         "localhost:0"                         },
	{ "mtu",             "0"                                   },
	{ "alive",           "0"                                   },
	{ "compression",     "0"                                   },
	{ "x11-forward",     "false"                               },
	{ "prvport",         "false"                               },
	{ "force-pty",       "true"                                },
	{ "remfwd",          "false"                               },
	{ "idhost",          "true"                                },
	{ "portftp",         "false"                               },
	{ "sftpbridge-host", ""                                    },
	{ "sftpbridge-port", ""                                    },
	{ "strict-hostid",   "false"                               },

	{ "kex-algorithms",
	  "diffie-hellman-group1-sha1,diffie-hellman-group-exchange-sha1" },
	{ "server-host-key-algorithms", hostKeyAlgs },
	{ "enc-algorithms-cli2srv", cipherAlgs },
	{ "enc-algorithms-srv2cli", cipherAlgs },
	{ "mac-algorithms-cli2srv", macAlgs },
	{ "mac-algorithms-srv2cli", macAlgs },
	{ "comp-algorithms-cli2srv", "none" },
	{ "comp-algorithms-srv2cli", "none" },
	{ "languages-cli2srv", "" },
	{ "languages-srv2cli", "" },

	{ "package-version", "MindTerm_" + Version.version },
    };

    static {
	for(int i = 0; i < defaultPropDesc.length; i++) {
	    String name  = defaultPropDesc[i][PROP_NAME];
	    String value = defaultPropDesc[i][PROP_VALUE];
	    if(value != null)
		defaultProperties.put(name, value);
	    defaultPropNames.put(name, "");
	}
	oldPropNames.put("realsrv", "real-server");
	oldPropNames.put("localhst", "local-bind");
	oldPropNames.put("usrname", "username");
	oldPropNames.put("passwd", "password");
	oldPropNames.put("rsapassword", "passphrase");
	oldPropNames.put("proxytype", "proxy-type");
	oldPropNames.put("proxyhost", "proxy-host");
	oldPropNames.put("proxyport", "proxy-port");
	oldPropNames.put("proxyuser", "proxy-user");
	oldPropNames.put("prxpassword", "proxy-password");
	oldPropNames.put("cipher", "ssh1-cipher");
	oldPropNames.put("authtyp", "auth-method");
	oldPropNames.put("idfile", "private-key");
	oldPropNames.put("x11fwd", "x11-forward");
	oldPropNames.put("forcpty", "force-pty");
    }

    public static String backwardCompatProp(String key) {
	String newName = (String)oldPropNames.get(key);
	if(newName != null) {
	    key = newName;
	}
	return key;
    }

    public static void setAsDefault(Properties props) {
	Enumeration enum = props.keys();
	while(enum.hasMoreElements()) {
	    String name  = (String)enum.nextElement();
	    String value = props.getProperty(name);
	    name = backwardCompatProp(name);
	    defaultProperties.put(name, value);
	}
    }

    String        sshHomeDir;
    String        knownHosts;
    SSHRSAKeyFile keyFile;

    SSHClient           client;
    SSHInteractor       interactor;
    boolean             activeProps;

    private EncryptedProperties props;

    protected String currentPropsFile;
    protected String currentAlias;

    boolean autoSaveProps;
    boolean autoLoadProps;
    boolean savePasswords;
    boolean readonly;

    private String propertyPassword;

    public Properties initTermProps;

    protected boolean propsChanged;

    public SSHPropertyHandler(Properties initProps, boolean setAsDefault) {
	this.knownHosts = SSH.KNOWN_HOSTS_FILE;

	if(setAsDefault) {
	    setAsDefault(initProps);
	}

	setProperties(initProps);

	this.activeProps  = false;
	this.propsChanged = false;
    }

    public SSHPropertyHandler(SSHPropertyHandler clone) {
	this(clone.props, false);
	this.sshHomeDir       = clone.sshHomeDir;
	this.keyFile          = clone.keyFile;
	this.initTermProps    = clone.initTermProps;
	this.propertyPassword = clone.propertyPassword;
	this.readonly         = true;
    }

    public static SSHPropertyHandler fromFile(String fileName, String password) throws IOException {
	SSHPropertyHandler fileProps = new SSHPropertyHandler(new Properties(),
							      false);
	fileProps.setPropertyPassword(password);
	fileProps.loadAbsoluteFile(fileName, false);

	setAsDefault(fileProps.props);

	return fileProps;
    }

    public void setInteractor(SSHInteractor interactor) {
	this.interactor = interactor;
    }

    public void setClient(SSHClient client) {
	this.client = client;
    }

    public void setAutoLoadProps(boolean value) {
	if(sshHomeDir != null)
	    autoLoadProps = value;
    }

    public void setAutoSaveProps(boolean value) {
	if(sshHomeDir != null)
	    autoSaveProps = value;
    }

    public void setSavePasswords(boolean value) {
	savePasswords = value;
    }

    public void setReadOnly(boolean value) {
	readonly = value;
    }

    public boolean isReadOnly() {
	return readonly;
    }

    public void setPropertyPassword(String password) {
	if(password != null)
	    this.propertyPassword = password;
    }

    public boolean emptyPropertyPassword() {
	return propertyPassword == null;
    }

    public boolean setSSHHomeDir(String sshHomeDir) {
	if(sshHomeDir == null || sshHomeDir.trim().length() == 0) {
	    return true;
	}

	if(sshHomeDir != null && !sshHomeDir.endsWith(File.separator))
	    sshHomeDir += File.separator;

	try {
	    netscape.security.PrivilegeManager.enablePrivilege("UniversalFileAccess");
	} catch (netscape.security.ForbiddenTargetException e) {
	    // !!!
	}

	try {
	    // sshHomeDir always ends with a trailing File.separator. Strip before we
	    // try to create it (some platforms don't like ending 'separator' in name)
	    //
	    File sshDir = new File(sshHomeDir.substring(0, sshHomeDir.length() - 1));
	    if(!sshDir.exists()) {

                ByteArrayOutputStream baos =
                    readResource("/defaults/license.txt");
                if (null == baos || !interactor.licenseDialog(baos.toString())) {
                    return false;
                }

		if(interactor.askConfirmation("MindTerm home directory: '" + sshHomeDir +
					      "' does not exist, create it?", true)) {
		    try {
			sshDir.mkdir();
		    } catch (Throwable t) {
			interactor.alert("Could not create home directory, file operations disabled.");
			sshHomeDir = null;
		    }
		} else {
		    interactor.report("No home directory, file operations disabled.");
		    sshHomeDir = null;
		}
	    }
	} catch (Throwable t) {
	    if(interactor != null && interactor.isVerbose())
		interactor.report("Can't access local file system, file operations disabled.");
	    sshHomeDir = null;
	}
	this.sshHomeDir = sshHomeDir;
	if(this.sshHomeDir == null) {
	    autoSaveProps = false;
	    autoLoadProps = false;
	}

	if(interactor != null)
	    interactor.propsStateChanged(this);

	return true;
    }

    public String getSSHHomeDir() {
	return sshHomeDir;
    }

    public boolean hasHomeDir() {
	return sshHomeDir != null;
    }

    //
    // Methods delegated to Properties and other property related methods
    //
    public void resetToDefaults() {
	clearServerSetting();
	clearAllForwards();
	Enumeration enum = defaultPropNames.keys();
	while(enum.hasMoreElements()) {
	    String name  = (String)enum.nextElement();
	    String value = defaultProperties.getProperty(name);
	    if(value != null) {
		setProperty(name, value);
	    } else {
		props.remove(name);
	    }
	}
	Terminal term = getTerminal();
	if(term != null) {
	    term.resetToDefaults();
	}
    }

    public static boolean isProperty(String key) {
	key = backwardCompatProp(key);
	return defaultPropNames.containsKey(key) ||
	    (key.indexOf("local") == 0) || (key.indexOf("remote") == 0);
    }

    public String getProperty(String key) {
	key = backwardCompatProp(key);
	return props.getProperty(key);
    }

    public String getDefaultProperty(String key) {
	key = backwardCompatProp(key);
	return (String)defaultProperties.get(key);
    }

    public void setDefaultProperty(String key, String value) {
	key = backwardCompatProp(key);
	defaultProperties.put(key, value);
    }

    public void resetProperty(String key) {
	key = backwardCompatProp(key);
	setProperty(key, getDefaultProperty(key));
    }

    public void setProperty(String key, String value)
	throws IllegalArgumentException, NoSuchElementException
    {
	if(value == null)
	    return;

	key = backwardCompatProp(key);

	boolean equalProp  = !(value.equals(getProperty(key)));

	validateProperty(key, value);

	if(activeProps)
	    activateProperty(key, value);

	if(equalProp) {
	    if(interactor != null)
		interactor.propsStateChanged(this);
	    propsChanged = equalProp;
	}

	props.put(key, value);
    }

    final void validateProperty(String key, String value)
	throws IllegalArgumentException, NoSuchElementException {
	//
	// Some sanity checks...
	//
	if(key.equals("auth-method")) {
	    SSH.getAuthTypes(value);
	    //
	} else if(key.equals("x11-forward")  || key.equals("prvport") ||
		  key.equals("force-pty") || key.equals("remfwd")  ||
		  key.equals("stricthostid")  || key.equals("portftp")) {
	    if(!(value.equals("true") || value.equals("false")))
		throw new IllegalArgumentException("Value for " + key + " must be 'true' or 'false'");
	    //
	} else if(key.equals("port") || key.equals("proxy-port") || key.equals("mtu") ||
		  key.equals("alive") || key.equals("compression")) {
	    try {
		int val = Integer.valueOf(value).intValue();
		if((key.equals("port") || key.equals("proxy-port")) && (val > 65535 || val < 0)) {
		    throw new IllegalArgumentException("Not a valid port number: " + value);
		} else if(key.equals("mtu") && val != 0 && (val > (256*1024) || val < 4096)) {
		    throw new IllegalArgumentException("Mtu must be between 4k and 256k");
		} else if(key.equals("alive")) {
		    if(val < 0 || val > 600)
			throw new IllegalArgumentException("Alive interval must be 0-600");
		} else if(key.equals("compression")) {
		    if(val < 0 || val > 9)
			throw new IllegalArgumentException("Compression Level must be 0-9");
		}
	    } catch (NumberFormatException e) {
		throw new IllegalArgumentException("Value for " + key + " must be an integer");
	    }
	    //
	} else if(key.equals("server")) {
	    if(client != null && client.isOpened()) {
		throw new IllegalArgumentException("Server can only be set while not connected");
	    }
	} else if(key.equals("real-server") || key.equals("local-bind")) {
	    try {
		InetAddress.getByName(value);
	    } catch (UnknownHostException e) {
		throw new IllegalArgumentException(key + " address must be a legal/known host name");
	    }
	} else if(key.equals("proxy-type")) {
	    SSH.getProxyType(value);
	} else if(key.startsWith("local") || key.startsWith("remote")) {
	    try {
		if(value.startsWith("/general/"))
		    value = value.substring(9);
		if(key.startsWith("local"))
		    addLocalPortForward(value, false);
		else
		    addRemotePortForward(value, false);
	    } catch (Exception e) {
		throw new IllegalArgumentException("Not a valid port forward: " + key + " : " + value);
	    }
	} else if(!isProperty(key)) {
	    throw new NoSuchElementException("Unknown ssh property '" + key + "'");
	}
    }

    void activateProperty(String key, String value) {
	//
	// The properties that needs an action to "activated"
	//
	if(key.equals("remfwd")) {
	    try {
		SSHListenChannel.setAllowRemoteConnect((new Boolean(value)).booleanValue());
	    } catch (Throwable t) {
		// Ignore if we don't have the SSHListenChannel class
	    }
	} else if(key.equals("portftp")) {
	    client.havePORTFtp = (new Boolean(value)).booleanValue();
	    if(client.havePORTFtp && SSHProtocolPlugin.getPlugin("ftp") != null) {
		SSHProtocolPlugin.getPlugin("ftp").initiate(client);
	    }
	    //
	} else if(key.equals("alive")) {
	    if(client instanceof SSHInteractiveClient &&
	       ((SSHInteractiveClient)client).isSSH2) {
		((SSHInteractiveClient)client).transport.enableKeepAlive(
					 Integer.parseInt(value));
	    } else {
		client.setAliveInterval(Integer.parseInt(value));
	    }
	} else if(key.equals("real-server")) {
	    try {
		if(value != null && value.length() > 0)
		    client.setServerRealAddr(InetAddress.getByName(value));
		else
		    client.setServerRealAddr(null);
	    } catch (UnknownHostException e) {
		// !!!
	    }
	} else if(key.equals("local-bind")) {
	    try {
		client.setLocalAddr(value);
	    } catch (UnknownHostException e) {
		throw new IllegalArgumentException("localhost address must be a legal/known host name");
	    }
	} else if(key.startsWith("local")) {
	    int n = Integer.parseInt(key.substring(5));
	    if(n > client.localForwards.size())
		throw new IllegalArgumentException("Port forwards must be given in unbroken sequence");
	    if(value.startsWith("/general/"))
		value = value.substring(9);
	    try {
		addLocalPortForward(value, true);
	    } catch (IOException e) {
		if(!interactor.askConfirmation("Error setting up tunnel '" +
					       value + "', continue anyway?",
					       true)) {
		    throw new IllegalArgumentException("Error creating tunnel: " + e.getMessage());
		}
	    }
	} else if(key.startsWith("remote")) {
	    try {
		int n = Integer.parseInt(key.substring(6));
		if(n > client.remoteForwards.size())
		    throw new IllegalArgumentException("Port forwards must be given in unbroken sequence");
		if(value.startsWith("/general/"))
		    value = value.substring(9);
		addRemotePortForward(value, true);
	    } catch (Exception e) {
		throw new IllegalArgumentException("Not a valid port forward: " + key + " : " + value);
	    }
	}
    }

    public void setProperties(Properties newProps) throws IllegalArgumentException,
    NoSuchElementException
    {
	props = new EncryptedProperties(defaultProperties);
	mergeProperties(newProps);
    }

    public Properties getProperties() {
	return props;
    }

    public void mergeProperties(Properties newProps)
	throws IllegalArgumentException, NoSuchElementException
    {
	String name, value;
	Enumeration enum;
	int i;

	enum = newProps.propertyNames();
	while(enum.hasMoreElements()) {
	    name  = (String)enum.nextElement();
	    value = newProps.getProperty(name);
	    name  = backwardCompatProp(name);
	    if(!isProperty(name))
		throw new NoSuchElementException("Unknown ssh property '" + name + "'");
	    props.put(name, value);
	}
    }

    public Properties getInitTerminalProperties() {
	return initTermProps;
    }

    public void activateProperties() {
	if(activeProps)
	    return;

	String name, value;
	Enumeration enum = defaultPropNames.keys();

	activeProps = true;

	while(enum.hasMoreElements()) {
	    name  = (String)enum.nextElement();
	    value = props.getProperty(name);
	    if(value != null)
		activateProperty(name, value);
	}
	int i = 0;
	while((value = props.getProperty("local" + i)) != null) {
	    activateProperty("local" + i, value);
	    i++;
	}
	i = 0;
	while((value = props.getProperty("remote" + i)) != null) {
	    activateProperty("remote" + i, value);
	    i++;
	}
    }

    public void passivateProperties() {
	activeProps = false;
    }

    private void saveProperties(String fname) throws IOException {
	FileOutputStream f;
	Terminal         term      = getTerminal();
	Properties       termProps = (term != null ? term.getProperties() : null);

	try {
	    netscape.security.PrivilegeManager.enablePrivilege("UniversalFileAccess");
	} catch (netscape.security.ForbiddenTargetException e) {
	    // !!!
	}

	if(termProps != null) {
	    Enumeration e = termProps.keys();
	    while(e.hasMoreElements()) {
		String key = (String)e.nextElement();
		String val = termProps.getProperty(key);
		props.put(key, val);
	    }
	}

	f = new FileOutputStream(fname);

	if(savePasswords) {
	    // !!! REMOVE
	    if(propertyPassword == null) {
		propertyPassword = "";
	    }
	    // TODO: should take default cipher from defaultProperties
	    props.save(f, "MindTerm ssh settings",
		       propertyPassword, SSH.cipherClasses[SSH.CIPHER_DEFAULT][0]);
	} else {
	    String prxPwd, stdPwd, tisPwd, rsaPwd;
	    stdPwd = props.getProperty("password");
	    prxPwd = props.getProperty("proxy-password");
	    tisPwd = props.getProperty("tispassword");
	    rsaPwd = props.getProperty("passphrase");
	    clearPasswords();
	    props.save(f, "MindTerm ssh settings");
	    if(stdPwd != null) props.put("password", stdPwd);
	    if(prxPwd != null) props.put("proxy-password", prxPwd);
	    if(tisPwd != null) props.put("tispassword", tisPwd);
	    if(rsaPwd != null) props.put("passphrase", rsaPwd);
	}

	f.close();

	propsChanged = false;
	if(term != null)
	    term.setPropsChanged(false);

	interactor.propsStateChanged(this);
    }

    private void loadProperties(String fname, boolean promptPwd) throws IOException {
	Terminal term = getTerminal();

	try {
	    netscape.security.PrivilegeManager.enablePrivilege("UniversalFileAccess");
	} catch (netscape.security.ForbiddenTargetException e) {
	    // !!!
	}

	FileInputStream f     = new FileInputStream(fname);
	byte[]          bytes = new byte[f.available()];
	f.read(bytes);
	ByteArrayInputStream bytein = new ByteArrayInputStream(bytes);
	f.close();

	EncryptedProperties loadProps = new EncryptedProperties();

	try {
	    loadProps.load(bytein, "");
	} catch (SSHAccessDeniedException e) {
	    try {
		bytein.reset();
		loadProps.load(bytein, propertyPassword);
	    } catch (SSHAccessDeniedException ee) {
		try {
		    if(promptPwd) {
			bytein.reset();
			propertyPassword = interactor.promptPassword("File " + fname + " password: ");
			loadProps.load(bytein, propertyPassword);
		    } else {
			throw new SSHAccessDeniedException("");
		    }
		} catch (SSHAccessDeniedException eee) {
		    clearServerSetting();
		    throw new SSHClient.AuthFailException("Access denied for '" + fname + "'");
		}
	    }
	}

	savePasswords = !loadProps.isNormalPropsFile();

	Enumeration enum;
	String      name;
	String      value;

	Properties sshProps  = new Properties();
	Properties termProps = new Properties();

	enum = loadProps.keys();
	while(enum.hasMoreElements()) {
	    name  = (String)enum.nextElement();
	    value = loadProps.getProperty(name);
	    if(isProperty(name)) {
		name = backwardCompatProp(name);
		sshProps.put(name, value);
	    } else if(TerminalDefProps.isProperty(name)) {
		name = TerminalDefProps.backwardCompatProp(name);
		termProps.put(name, value);
	    } else {
		if(interactor != null)
		    interactor.report("Unknown property '" + name + "' found in file: " + fname);
		else
		    System.out.println("Unknown property '" + name + "' found in file: " + fname);
	    }
	}

	if(client != null)
	    client.clearAllForwards();

	passivateProperties();

	setProperties(sshProps);

	initTermProps = termProps;

	if(term != null) {
	    term.setProperties(initTermProps, false);
	    term.setPropsChanged(false);
	}

	propsChanged = false;
	if(interactor != null)
	    interactor.propsStateChanged(this);
    }

    final void clearPasswords() {
	props.remove("password");
	props.remove("tispassword");
	props.remove("passphrase");
	props.remove("proxy-password");
    }

    final void clearServerSetting() {
	setProperty("server", "");
	currentPropsFile = null;
	currentAlias     = null;
	if(interactor != null)
	    interactor.propsStateChanged(this);
    }

    final void clearAllForwards() {
	int i = 0;
	if(client != null)
	    client.clearAllForwards();
	for(i = 0; i < 1024; i++) {
	    String key = "local" + i;
	    if(!props.containsKey(key))
		break;
	    props.remove(key);
	}
	for(i = 0; i < 1024; i++) {
	    String key = "remote" + i;
	    if(!props.containsKey(key))
		break;
	    props.remove(key);
	}
    }

    public boolean wantSave() {
	boolean somePropsChanged = (propsChanged ||
				    (getTerminal() != null ?
				     getTerminal().getPropsChanged() : false));
	return (!isReadOnly() && somePropsChanged && sshHomeDir != null &&
		currentAlias != null);
    }

    public final void checkSave() throws IOException {
	if(autoSaveProps) {
	    saveCurrentFile();
	}
    }

    public void saveCurrentFile() throws IOException {
	if(currentPropsFile != null && wantSave())
	    saveProperties(currentPropsFile);
    }

    public void saveAsCurrentFile(String fileName) throws IOException {
	propsChanged     = true;
	currentPropsFile = fileName;
	saveCurrentFile();
	currentAlias     = null;
    }

    public void loadAbsoluteFile(String fileName, boolean promptPwd) throws IOException {
	currentAlias     = null;
	currentPropsFile = fileName;

	loadProperties(currentPropsFile, promptPwd);
	if(interactor != null)
	    interactor.propsStateChanged(this);
    }

    public void setAlias(String alias) {
	if(sshHomeDir == null)
	    return;
	currentAlias     = alias;
	currentPropsFile = sshHomeDir + alias + PROPS_FILE_EXT;
    }

    public String getAlias() {
	return currentAlias;
    }

    public void loadAliasFile(String alias, boolean promptPwd) throws IOException {
	String oldAlias = currentAlias;
	setAlias(alias);
	if(oldAlias == null || !oldAlias.equals(alias)) {
	    loadProperties(currentPropsFile, promptPwd);
	}
    }

    public String[] availableAliases() {
	if(sshHomeDir == null)
	    return null;

	try {
	    netscape.security.PrivilegeManager.enablePrivilege("UniversalFileAccess");
	} catch (netscape.security.ForbiddenTargetException e) {
	    // !!!
	}

	// sshHomeDir always ends with a trailing File.separator. Strip before we
	// try to create it (some platforms don't like ending 'separator' in name)
	//
	File dir = new File(sshHomeDir.substring(0, sshHomeDir.length() - 1));
	String[] list, alist;
	int  i, cnt = 0;

	list = dir.list();
	for(i = 0; i < list.length; i++) {
	    if(!list[i].endsWith(PROPS_FILE_EXT)) {
		list[i] = null;
		cnt++;
	    }
	}
	if(cnt == list.length)
	    return null;
	alist = new String[list.length - cnt];
	cnt = 0;
	for(i = 0; i < list.length; i++) {
	    if(list[i] != null) {
		int pi = list[i].lastIndexOf(PROPS_FILE_EXT);
		alist[cnt++] = list[i].substring(0, pi);
	    }
	}

	return alist;
    }

    public boolean isAlias(String alias) {
	String[] aliases = availableAliases();
	boolean  isAlias = false;
	if(aliases != null) {
	    for(int i = 0; i < aliases.length; i++)
		if(alias.equals(aliases[i])) {
		    isAlias = true;
		    break;
		}
	}
	return isAlias;
    }

    public boolean isAbsolutFile(String fileName) {
	if(sshHomeDir == null)
	    return false;

	try {
	    netscape.security.PrivilegeManager.enablePrivilege("UniversalFileAccess");
	} catch (netscape.security.ForbiddenTargetException e) {
	    // !!!
	}

	File file = new File(fileName);
	return (file.isFile() && file.exists());
    }

    public Terminal getTerminal() {
	if(client == null || client.console == null)
	    return null;
	Terminal term = client.console.getTerminal();
	return term;
    }

    public void removeLocalTunnelAt(int idx, boolean kill) {
	int i, sz = client.localForwards.size();
	props.remove("local" + idx);
	for(i = idx; i < sz - 1; i++) {
	    props.put("local" + i, props.get("local" + (i + 1)));
	    props.remove("local" + (i + 1));
	}
	propsChanged = true;
	if(kill) {
	    SSHClient.LocalForward fwd = (SSHClient.LocalForward)client.localForwards.elementAt(idx);
	    client.delLocalPortForward(fwd.localHost, fwd.localPort);
	} else {
	    client.localForwards.removeElementAt(idx);
	}
    }

    public void removeRemoteTunnelAt(int idx) {
	int i, sz = client.remoteForwards.size();
	props.remove("remote" + idx);
	for(i = idx; i < sz - 1; i++) {
	    props.put("remote" + i, props.get("remote" + (i + 1)));
	    props.remove("remote" + (i + 1));
	}
	propsChanged = true;
	if(client instanceof SSHInteractiveClient &&
	   ((SSHInteractiveClient)client).isSSH2) {
	    SSHClient.RemoteForward fwd = (SSHClient.RemoteForward)
		client.remoteForwards.elementAt(idx);
	    if(fwd != null) {
		client.delRemotePortForward(fwd.remotePort);
	    }
	} else {
	    client.remoteForwards.removeElementAt(idx);
	}
    }

    public void addLocalPortForward(String fwdSpec, boolean commit) throws IllegalArgumentException,
    IOException {
	int    localPort;
	String remoteHost;
	int    remotePort;
	int    d1, d2, d3;
	String tmp, plugin;
	String localHost = null;

	if(fwdSpec.charAt(0) == '/') {
	    int i = fwdSpec.lastIndexOf('/');
	    if(i == 0)
		throw new IllegalArgumentException("Invalid port forward spec. " + fwdSpec);
	    plugin = fwdSpec.substring(1, i);
	    fwdSpec = fwdSpec.substring(i + 1);
	} else
	    plugin = "general";

	d1 = fwdSpec.indexOf(':');
	d2 = fwdSpec.lastIndexOf(':');
	if(d1 == d2)
	    throw new IllegalArgumentException("Invalid port forward spec. " + fwdSpec);

	d3 = fwdSpec.indexOf(':', d1 + 1);

	if(d3 != d2) {
	    localHost = fwdSpec.substring(0, d1);
	    localPort = Integer.parseInt(fwdSpec.substring(d1 + 1, d3));
	    remoteHost = fwdSpec.substring(d3 + 1, d2);
	} else {
	    localPort = Integer.parseInt(fwdSpec.substring(0, d1));
	    remoteHost = fwdSpec.substring(d1 + 1, d2);
	}

	tmp        = fwdSpec.substring(d2 + 1);
	remotePort = Integer.parseInt(tmp);
	if(commit) {
	    if(localHost == null)
		client.addLocalPortForward(localPort, remoteHost, remotePort, plugin);
	    else
		client.addLocalPortForward(localHost, localPort, remoteHost, remotePort, plugin);
	}
    }

    public void addRemotePortForward(String fwdSpec, boolean commit) throws IllegalArgumentException {
	int    remotePort;
	int    localPort;
	String localHost;
	int    d1, d2;
	String tmp, plugin;

	if(fwdSpec.charAt(0) == '/') {
	    int i = fwdSpec.lastIndexOf('/');
	    if(i == 0)
		throw new IllegalArgumentException("Invalid port forward spec.");
	    plugin = fwdSpec.substring(1, i);
	    fwdSpec = fwdSpec.substring(i + 1);
	} else
	    plugin = "general";

	d1 = fwdSpec.indexOf(':');
	d2 = fwdSpec.lastIndexOf(':');
	if(d1 == d2)
	    throw new IllegalArgumentException("Invalid port forward spec.");

	tmp        = fwdSpec.substring(0, d1);
	remotePort = Integer.parseInt(tmp);
	localHost  = fwdSpec.substring(d1 + 1, d2);
	tmp        = fwdSpec.substring(d2 + 1);
	localPort  = Integer.parseInt(tmp);
	if(commit) {
	    client.addRemotePortForward(remotePort, localHost, localPort, plugin);
	}
    }

    //
    // SSHAuthenticator interface
    //
    public String getUsername(SSHClientUser origin) throws IOException {
	String username = getProperty("username");
	if(kludgeSrvPrompt ||
	   !interactor.quietPrompts() ||
	   (username == null || username.equals(""))) {
	    String username2 = interactor.promptLine(getProperty("server") + " login: ", username);
	    if(!username2.equals(username)) {
		clearPasswords();
		username = username2;
	    }
	    setProperty("username", username); // Changing the user name does not save new properties...
	}
	return username;
    }

    public String getPassword(SSHClientUser origin) throws IOException {
	String password = getProperty("password");
	if(password == null) {
	    password = interactor.promptPassword(getProperty("username") + "@" +
						 getProperty("server") + "'s password: ");
	    setProperty("password", password);
	}
	return password;
    }

    public String getChallengeResponse(SSHClientUser origin, String challenge) throws IOException {
	String tisPassword = getProperty("tispassword");
	if(tisPassword == null) {
	    tisPassword = interactor.promptPassword(challenge);
	    setProperty("tispassword", tisPassword);
	}
	return tisPassword;
    }

    public int[] getAuthTypes(SSHClientUser origin) {
	return SSH.getAuthTypes(getProperty("auth-method"));
    }

    public int getCipher(SSHClientUser origin) {
	int cipher = SSH.getCipherType(getProperty("ssh1-cipher"));
	if(cipher == SSH.CIPHER_NOTSUPPORTED) {
	    interactor.report("Cipher '" + getProperty("ssh1-cipher") +
			      "' not supported in ssh1, using default");
	    resetProperty("ssh1-cipher");
	}
	return SSH.getCipherType(getProperty("ssh1-cipher"));
    }

    public SSHRSAKeyFile getIdentityFile(SSHClientUser origin) throws IOException {
	String idFile = getProperty("private-key");
	if(idFile.indexOf(File.separator) == -1) {
	    idFile = sshHomeDir + idFile;
	}

	try {
	    netscape.security.PrivilegeManager.enablePrivilege("UniversalFileAccess");
	} catch (netscape.security.ForbiddenTargetException e) {
	    // !!!
	}

	keyFile = new SSHRSAKeyFile(idFile);
	return keyFile;
    }

    public String getIdentityPassword(SSHClientUser origin) throws IOException {
	String rsaPassword = getProperty("passphrase");
	if(rsaPassword == null) {
	    rsaPassword = interactor.promptPassword("key file '" + keyFile.getComment() +
						    "' password: ");
	    setProperty("passphrase", rsaPassword);
	}
	return rsaPassword;
    }

    public String getIdentityPassword(String prompt)
	throws IOException
    {
	String rsaPassword = getProperty("passphrase");
	if(rsaPassword == null) {
	    rsaPassword = interactor.promptPassword(prompt);
	    setProperty("passphrase", rsaPassword);
	}
	return rsaPassword;
    }

    // !!! TODO Make SSHHostKeyVerify which can do both ssh1 and ssh2
    // !!! verifyHostKey(PublicKey key, byte[] keyBlob, String type)
    //
    public boolean verifyKnownHosts(RSAPublicKey hostPub) throws IOException {
	File        tmpFile;
	String      fileName     = null;
	InputStream knownHostsIn = null;
	int         hostCheck    = 0;
	boolean     confirm      = true;
	boolean     strict       = strictHostKeyCheck();

	byte[] rawN = hostPub.getModulus().toByteArray();
	byte[] rawE = hostPub.getPublicExponent().toByteArray();
	int nCutZero = ((rawN[0] == 0) ? 1 : 0);
	int eCutZero = ((rawE[0] == 0) ? 1 : 0);

	byte[] blob = new byte[rawN.length + rawE.length - nCutZero - eCutZero];
	System.arraycopy(rawN, nCutZero, blob , 0, rawN.length - nCutZero);
	System.arraycopy(rawE, eCutZero, blob , rawN.length - nCutZero,
			 rawE.length - eCutZero);
	showFingerprint(blob, "rsa1");

	SSHRSAPublicKeyFile file = null;

	knownHostsIn = this.getClass().getResourceAsStream("/defaults/known_hosts.txt");

	try {
	    boolean tryingResource = true;
	    while(tryingResource) {
		if(knownHostsIn != null) {
		    fileName = "<resource>/defaults/known_hosts.txt";
		    if(interactor.isVerbose())
			interactor.report("Found preinstalled 'known_hosts' file.");
		} else {
		    tryingResource = false;
		    if(sshHomeDir == null && !strict) {
			if(interactor.isVerbose())
			    interactor.report("File operations disabled, server identity can't be verified");
			return true;
		    }

		    try {
			netscape.security.PrivilegeManager.enablePrivilege("UniversalFileAccess");
		    } catch (netscape.security.ForbiddenTargetException e) {
			// !!!
		    }

		    fileName = sshHomeDir + knownHosts;
		    tmpFile = new File(fileName);

		    if(!tmpFile.exists()) {
			if(interactor.askConfirmation("File '"  + fileName + "' not found, create it?", true)) {
			    FileOutputStream f = new FileOutputStream(tmpFile);
			    f.close();
			} else if(!strict) {
			    interactor.report("Verification of server key disabled in this session.");
			    return true;
			}
		    }

		    knownHostsIn = new FileInputStream(fileName);
		}

		file = new SSHRSAPublicKeyFile(knownHostsIn, fileName, true);

		if((hostCheck = file.checkPublic(hostPub.getModulus(), getProperty("server"))) ==
		   SSH.SRV_HOSTKEY_KNOWN)
		    return true;

		if(tryingResource) {
		    if(!interactor.askConfirmation("Host was not found in preinstalled 'known_hosts' file! Continue anyway?", false))
			return false;
		}

		knownHostsIn = null;
	    }

	    if(strict) {
		strictHostFailed();
		return false;
	    }

	    if(hostCheck == SSH.SRV_HOSTKEY_NEW) {
		if(!askSaveKeyConfirmation(fileName)) {
		    return true;
		}
		confirm = true;
	    } else {
		confirm = askChangeKeyConfirmation();
		file.removePublic(getProperty("server"));
	    }

	    if(confirm) {
		file.addPublic(getProperty("server"), null,
			       hostPub.getPublicExponent(),
			       hostPub.getModulus());
		tmpFile      = new File(fileName + ".tmp");
		File oldFile = new File(fileName);
		oldFile.renameTo(tmpFile);
		try {
		    file.saveToFile(fileName);
		} catch (IOException e) {
		    oldFile = new File(fileName);
		    tmpFile.renameTo(oldFile);
		    throw e;
		}
		tmpFile.delete();
	    } else {
		return false;
	    }
	} finally {
	    try { knownHostsIn.close(); } catch (Exception e) {}  
	}

	return true;
    }

    public boolean verifyKnownSSH2Hosts(SSHInteractiveClient cli,
					SSH2Signature serverHostKey)
	throws IOException, SSH2Exception
    {
	File        tmpFile;
	String      fileName     = null;
	InputStream knownHostsIn = null;
	boolean     strict       = strictHostKeyCheck();

	showFingerprint(serverHostKey.getPublicKeyBlob(),
			serverHostKey.getAlgorithmName());

	if(sshHomeDir == null && !strict) {
	    if(interactor.isVerbose())
		interactor.report("File operations disabled, server identity can't be verified");
	    return true;
	}

	try {
	    netscape.security.PrivilegeManager.enablePrivilege("UniversalFileAccess");
	} catch (netscape.security.ForbiddenTargetException e) {
	    // !!!
	}

	fileName = sshHomeDir + "hostkeys";
	tmpFile = new File(fileName);

	if(!tmpFile.exists() && !strict) {
	    if(interactor.askConfirmation("Known hosts directory: '" +
					  fileName +
					  "' does not exist, create it?", true)) {
		try {
		    tmpFile.mkdir();
		} catch (Throwable t) {
		    interactor.alert("Could not create known hosts directory.");
		}
	    }
	}

	if(!strict && (!tmpFile.exists() || !tmpFile.isDirectory())) {
	    return interactor.askConfirmation("No hostkeys directory, can't verify host, continue anyway?", false);
	}

	fileName += File.separator +
	    "key_" + getProperty("port") + "_" + getProperty("server") + ".pub";
	tmpFile = new File(fileName);

	if(!tmpFile.exists()) {
	    if(strict) {
		strictHostFailed();
		return false;
	    }
	    if(!askSaveKeyConfirmation(fileName)) {
		return true;
	    }
	} else {
	    SSH2PublicKeyFile pkif      = new SSH2PublicKeyFile();
	    String            keyFormat = serverHostKey.getAlgorithmName();

	    pkif.load(fileName);

	    if(pkif.getAlgorithmName().equals(keyFormat)) {
		if(keyFormat.equals("ssh-dss")) {
		    DSAPublicKey keyWire   = (DSAPublicKey)serverHostKey.getPublicKey();
		    DSAPublicKey keyStored = (DSAPublicKey)pkif.getPublicKey();
		    if(keyWire.getY().equals(keyStored.getY()) &&
		       keyWire.getParams().getG().equals(keyStored.getParams().getG())
		       &&
		       keyWire.getParams().getP().equals(keyStored.getParams().getP())
		       &&
		       keyWire.getParams().getQ().equals(keyStored.getParams().getQ())) {
			return true;
		    }
		} else if(keyFormat.equals("ssh-rsa")) {
		    RSAPublicKey keyWire   = (RSAPublicKey)serverHostKey.getPublicKey();
		    RSAPublicKey keyStored = (RSAPublicKey)pkif.getPublicKey();
		    if(keyWire.getPublicExponent().equals(keyStored.getPublicExponent()) &&
		       keyWire.getModulus().equals(keyStored.getModulus())) {
			return true;
		    }
		} else {
		    throw new IOException("Unknown host-key format: " + keyFormat);
		}
	    } else {
		if(strict) {
		    strictHostFailed();
		    return false;
		} else {
		    interactor.report("Host key format has changed to '" +
				      keyFormat +
				      "', please verify this with your sysadmin.");
		}
	    }

	    if(!askChangeKeyConfirmation()) {
		return false;
	    }

	    tmpFile.delete();
	}

	String  user = getProperty("username");
	// !!! OUCH
	if(user == null) {
	    user = SSH.VER_MINDTERM;
	}

	SSH2PublicKeyFile pkif =
	    new SSH2PublicKeyFile(serverHostKey.getPublicKey(),
				  user,
				  "\"host key for " + getProperty("server") +
				  ", accepted by " + user + " " +
				  (new Date()) + "\"");
	pkif.store(fileName);

	return true;
    }

    boolean strictHostKeyCheck() {
	return Boolean.valueOf(getProperty("stricthostid")).booleanValue();
    }

    void strictHostFailed() {
	interactor.report("Strict host key checking enabled, please add host key.");
    }

    boolean askSaveKeyConfirmation(String fileName) {
	if(interactor.isVerbose())
	    interactor.report("Host key not found in '" + fileName + "'");
	if(!interactor.askConfirmation("Do you want to add this host to your set of known hosts (check fingerprint)", true)) {
	    interactor.report("Verification of server key disabled in this session.");
	    return false;
	}
	return true;
    }

    boolean askChangeKeyConfirmation() {
	interactor.alert("WARNING: HOST IDENTIFICATION HAS CHANGED! " +
			 "IT IS POSSIBLE THAT SOMEONE IS DOING SOMETHING NASTY, "
			 + "ONLY PROCEED IF YOU KNOW WHAT YOU ARE DOING!");
	return interactor.askConfirmation("Do you want to replace the identification of this host?",
					  false);
    }

    // !!! TODO move out into separate fingerprint handling
    //
    void showFingerprint(byte[] blob, String type) throws IOException {
	try {
	  MessageDigest md5 = MessageDigest.getInstance("MD5");
	  md5.update(blob);
	  byte[] material = md5.digest();

	  StringBuffer msg = new StringBuffer();

	  msg.append("\r\nServer's hostkey (" + type + ") fingerprint:\r\n");
	  msg.append("openssh md5:  ");
	  msg.append(fingerprintHex(material));

	  MessageDigest sha1 = MessageDigest.getInstance("SHA1");
	  sha1.update(blob);
	  material = sha1.digest();

	  msg.append("\r\nbubblebabble: ");
	  msg.append(fingerprintBubblebabble(material));

	  interactor.report(msg.toString());

	} catch (Exception e) {
	    throw new IOException("SHA1 not implemented");
	}
    }

    String fingerprintHex(byte[] raw) {
	String       hex = com.mindbright.util.HexDump.toString(raw);
	StringBuffer fps = new StringBuffer();
	for(int i = 0; i < hex.length(); i += 2) {
	      fps.append(hex.substring(i, i + 2));
	      if(i < hex.length() - 2) {
		  fps.append(":");
	      }
	}
	return fps.toString();
    }

    String fingerprintBubblebabble(byte[] raw) {
	StringBuffer retval     = new StringBuffer();
	char[]       consonants = { 'b', 'c', 'd', 'f', 'g', 'h', 'k', 'l', 'm',
				    'n', 'p', 'r', 's', 't', 'v', 'z', 'x' };
	char[]       vowels     = { 'a', 'e', 'i', 'o', 'u', 'y' };
	int          rounds     = (raw.length / 2) + 1;
	int          seed       = 1;

	retval.append('x');

	for(int i = 0; i < rounds; i++) {
	    int idx0, idx1, idx2, idx3, idx4;
	    if((i + 1 < rounds) || ((raw.length % 2) != 0)) {
		idx0 = ((((((int)(raw[2 * i])) & 0xff) >>> 6) & 3) + seed) % 6;
		idx1 = ((((int)(raw[2 * i])) & 0xff) >>> 2) & 15;
		idx2 = (((((int)(raw[2 * i])) & 0xff) & 3) + (seed / 6)) % 6;
		retval.append(vowels[idx0]);
		retval.append(consonants[idx1]);
		retval.append(vowels[idx2]);
		if((i + 1) < rounds) {
		    idx3 = ((((int)(raw[(2 * i) + 1])) & 0xff) >>> 4) & 15;
		    idx4 = (((int)(raw[(2 * i) + 1])) & 0xff) & 15;
		    retval.append(consonants[idx3]);
		    retval.append('-');
		    retval.append(consonants[idx4]);
		    seed = ((seed * 5) +
			    (((((int)(raw[2 * i])) & 0xff) * 7) +
			     (((int)(raw[(2 * i) + 1])) & 0xff))) % 36;
		}
	    } else {
		idx0 = seed % 6;
		idx1 = 16;
		idx2 = seed / 6;
		retval.append(vowels[idx0]);
		retval.append(consonants[idx1]);
		retval.append(vowels[idx2]);
	    }
	}
	retval.append('x');

	return retval.toString();
    }

    //
    // ProxyAuthenticator interface
    //

    public String getProxyUsername(String type, String challenge) throws IOException {
	String username = getProperty("proxy-user");
	if(!interactor.quietPrompts() || (username == null || username.equals(""))) {
	    String chStr = (challenge != null ? (" '" + challenge + "'") : "");
	    username = interactor.promptLine(type + chStr + " username: ", username);
	    setProperty("proxy-user", username);
	}
	return username;
    }

    public String getProxyPassword(String type, String challenge) throws IOException {
	String prxPassword = getProperty("proxy-password");
	if(prxPassword == null) {
	    String chStr = (challenge != null ? (" '" + challenge + "'") : "");
	    prxPassword = interactor.promptPassword(type + chStr + " password: ");
	    setProperty("proxy-password", prxPassword);
	}
	return prxPassword;
    }

    //
    // SSHClientUser interface
    //

    boolean kludgeSrvPrompt;

    public String getSrvHost() throws IOException {
	String host  = getProperty("server");
	String alias;

	kludgeSrvPrompt = false;

	if(!interactor.quietPrompts() || (host == null || host.equals(""))) {
	    if(currentAlias != null)
		host = currentAlias;

	    host = interactor.promptLine("\r\33[2KSSH Server/Alias: ", host);
	    host = host.trim();

	    if("".equals(host)) {
		throw new SSHStdIO.SSHExternalMessage("");
	    }

	    if(client instanceof SSHInteractiveClient) {
		((SSHInteractiveClient)client).hideLogo();
	    }

	    alias = host;

	    int i;
	    if((i = host.indexOf(':')) > 0) {
		int port = Integer.parseInt(host.substring(i + 1));
		host = host.substring(0, i);
		setProperty("port", String.valueOf(port));
		alias = host  + "_" + port;
	    }

	    if(autoLoadProps) {
		if(isAlias(alias)) {
		    loadAliasFile(alias, true);
		} else if(isAbsolutFile(alias)) {
		    loadAbsoluteFile(alias, true);
		} else if(sshHomeDir != null) {
		    String pwdChk = "";

		    try {
			do {
			    alias =
				interactor.promptLine("No settings file for " +
						      host +
						      " found.\n\r" + 
				      "(^C = cancel, ^D or empty = don't save)\n\r" +
						      "Save as alias : ", alias);
			    alias = alias.trim();
			    if(alias.length() > 0 && savePasswords) {
				pwdChk = interactor.promptPassword(alias + " file password: ");
				if(pwdChk.length() > 0)
				    propertyPassword = interactor.promptPassword(alias + " password again: ");
			    }
			} while ((!pwdChk.equals("") && !pwdChk.equals(propertyPassword)));
		    } catch (SSHStdIO.SSHExternalMessage e) {
			if(e.ctrlC) {
			    throw e;
			}
			alias = "";
		    }

		    alias = alias.trim();

		    setProperty("server", host);

		    if(alias.length() == 0) {
			interactor.report("\r\33[2KNo alias set, disabled automatic saving (use 'Save Settings As...' to save)");
		    } else {
			setAlias(alias);
		    }

		    // Might be same host/user/pwd but we don't know, it's a
		    // different alias so we better clear stuff here so the user
		    // can change "identity" in another alias (otherwise if
		    // quietPrompts are used the user might not get a chance to
		    // do this). Also, tunnels are no longer "auto-transfered"
		    // between aliases.
		    //
		    clearPasswords();
		    clearAllForwards();
		    propsChanged = true;
		}
		host = getProperty("server");
	    } else {
		setProperty("server", host);
	    }

	    kludgeSrvPrompt = true;

	} else if(client instanceof SSHInteractiveClient) {
	    interactor.report("");
	    ((SSHInteractiveClient)client).hideLogo();
	}

	activateProperties();

	if(currentPropsFile != null) {
	    interactor.report("Current settings file: '" +
			      currentPropsFile + "'");
	}

	return host;
    }

    public int getSrvPort() {
	return Integer.valueOf(getProperty("port")).intValue();
    }

    public Socket getProxyConnection() throws IOException {
	String proxyType  = getProperty("proxy-type");
	int proxyTypeId   = SSH.PROXY_NONE;

	try {
	    proxyTypeId = SSH.getProxyType(proxyType);
	} catch (IllegalArgumentException e) {
	    throw new IOException(e.getMessage());
	}

	if(proxyTypeId == SSH.PROXY_NONE) {
	    return null;
	}

	String prxHost = getProperty("proxy-host");
	int    prxPort = -1;

	try {
	    prxPort = Integer.valueOf(getProperty("proxy-port")).intValue();
	} catch (Exception e) {
	    prxPort = -1;
	}

	if(prxHost == null || prxPort == -1) {
	    throw new IOException("When 'proxytype' is set, 'proxyhost' and 'proxyport' must also be set");
	}

	String sshHost = getProperty("server");
	int    sshPort = getSrvPort();
	String prxProt = getProperty("proxyproto");

	Socket proxySocket = null;

	switch(proxyTypeId) {
	case SSH.PROXY_HTTP:
	    proxySocket = WebProxyTunnelSocket.getProxy(sshHost, sshPort, prxHost, prxPort, prxProt,
							this, "MindTerm/" + SSH.CVS_NAME);
	    break;
	case SSH.PROXY_SOCKS4:
	    proxySocket = SocksProxySocket.getSocks4Proxy(sshHost, sshPort, prxHost, prxPort,
							  getProxyUsername("SOCKS4", null));
	    break;
	case SSH.PROXY_SOCKS5_DNS:
	    proxySocket = SocksProxySocket.getSocks5Proxy(sshHost, sshPort,
							  prxHost, prxPort,
							  false, this);
	    break;
	case SSH.PROXY_SOCKS5_IP:
	    proxySocket = SocksProxySocket.getSocks5Proxy(sshHost, sshPort,
							  prxHost, prxPort,
							  true, this);
	    break;
	}

	return proxySocket;
    }

    public ByteArrayOutputStream readResource(String name) {
        InputStream in = getClass().getResourceAsStream(name);
        ByteArrayOutputStream baos = null;
        if(in != null) {
            baos = new ByteArrayOutputStream(50000);
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

    public String getDisplay() {
	return getProperty("display");
    }

    public int getMaxPacketSz() {
	return Integer.valueOf(getProperty("mtu")).intValue();
    }

    public int getAliveInterval() {
	return Integer.valueOf(getProperty("alive")).intValue();
    }

    public int getCompressionLevel() {
	return Integer.valueOf(getProperty("compression")).intValue();
    }

    public boolean wantX11Forward() {
	return Boolean.valueOf(getProperty("x11-forward")).booleanValue();
    }

    public boolean wantPrivileged() {
	return Boolean.valueOf(getProperty("prvport")).booleanValue();
    }

    public boolean wantPTY() {
	return Boolean.valueOf(getProperty("force-pty")).booleanValue();
    }

    public SSHInteractor getInteractor() {
	return interactor;
    }

}
