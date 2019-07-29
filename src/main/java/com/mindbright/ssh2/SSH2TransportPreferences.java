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

import java.util.Properties;

/**
 * This class is a container for all protocol preferences and the packet version
 * used in the class <code>SSH2Transport</code>. It can be created using a
 * hard-coded list of preferences or it can be created from a
 * <code>java.util.Properties</code> instance. All preferences for algorithms
 * are comma separated lists in order of preference (as defined in the trasport
 * protocol spec.).
 * <p>
 * This class contains the negotiation logic to select preferences from lists of
 * client and server preferences. It also contains the functionality to select a
 * key exchange algorithm given the available algorithms and host key
 * types. These functions are used from the <code>SSH2Transport</code> class.
 * <p>
 * The preferences that can be set are the following:
 * <table border="1">
 * <tr>
 * <th>Property name</th><th>Description</th>
 * </tr>
 * <tr>
 * <td>kex-algorithms</td><td>Key exchange algorithms</td>
 * </tr>
 * <tr>
 * <td>server-host-key-algorithms</td><td>Host key algorithms</td>
 * </tr>
 * <tr>
 * <td>enc-algorithms-cli2srv</td><td>Encryption algorithms client to server</td>
 * </tr>
 * <tr>
 * <td>enc-algorithms-srv2cli</td><td>Encryption algorithms server to client</td>
 * </tr>
 * <tr>
 * <td>mac-algorithms-cli2srv</td><td>Mac algorithms client to server</td>
 * </tr>
 * <tr>
 * <td>mac-algorithms-srv2cli</td><td>Mac algorithms server to client</td>
 * </tr>
 * <tr>
 * <td>comp-algorithms-cli2srv</td><td>Compression algorithms client to server</td>
 * </tr>
 * <tr>
 * <td>comp-algorithms-srv2cli</td><td>Compression algorithms server to client</td>
 * </tr>
 * <tr>
 * <td>languages-cli2srv</td><td>Language tags client to server</td>
 * </tr>
 * <tr>
 * <td>languages-srv2cli</td><td>Language tags server to client</td>
 * </tr>
 * <tr>
 * <td>compression</td><td>Outgoing compression level 0-9 (default 6)</td>
 * </tr>
 * <tr>
 * <td>package-version</td><td>Package version for protocol version string</td>
 * </tr>
 * </table>
 * <p>
 * The available algorithms are the following
 * (provided their classes are included):
 * <table border="1">
 * <tr>
 * <th>Type</th><th>Algorithms</th>
 * </tr>
 * <tr>
 * <td>Key exchange</td>
 * <td>diffie-hellman-group1-sha1,diffie-hellman-group-exchange-sha1</td>
 * </tr>
 * <tr>
 * <td>Host key</td>
 * <td>ssh-dss,ssh-rsa</td>
 * </tr>
 * <tr>
 * <td>Ciphers</td>
 * <td>3des-cbc,blowfish-cbc,aes128-cbc,aes192-cbc,aes256-cbc,twofish128-cbc,
 *     twofish192-cbc,twofish256-cbc,cast128-cbc,idea-cbc,arcfour</td>
 * </tr>
 * <td>Macs</td>
 * <td>hmac-sha1,hmac-md5,hmac-ripemd160,hmac-sha1-96,hmac-md5-96,hmac-ripemd160-96</td>
 * </tr>
 * </table>
 *
 * @see SSH2Transport
 */
public class SSH2TransportPreferences {

    public static final int KEX_ALGORITHMS = 0;
    public static final int HOST_KEY_ALG   = 1;
    public static final int CIPHERS_C2S    = 2;
    public static final int CIPHERS_S2C    = 3;
    public static final int MACS_C2S       = 4;
    public static final int MACS_S2C       = 5;
    public static final int COMP_C2S       = 6;
    public static final int COMP_S2C       = 7;
    public static final int LANG_C2S       = 8;
    public static final int LANG_S2C       = 9;

    public static final int CIPHER      = CIPHERS_C2S;
    public static final int MAC         = MACS_C2S;
    public static final int COMPRESSION = COMP_C2S;
    public static final int LANGUAGE    = LANG_C2S;

    final static String[][] ciphers = {
	{ "3des-cbc",        "3DES/CBC" },
	{ "3des-ecb",        "3DES/ECB" },
	{ "3des-cfb",        "3DES/CFB" },
	{ "3des-ofb",        "3DES/OFB" },
	{ "blowfish-cbc",    "Blowfish/CBC" },
	{ "blowfish-ecb",    "Blowfish/ECB" },
	{ "blowfish-cfb",    "Blowfish/CFB" },
	{ "blowfish-ofb",    "Blowfish/OFB" },
	{ "aes128-cbc",      "AES/CBC" },
	{ "aes192-cbc",      "AES/CBC" },
	{ "aes256-cbc",      "AES/CBC" },
	{ "rijndael128-cbc", "Rijndael/CBC" },
	{ "rijndael192-cbc", "Rijndael/CBC" },
	{ "rijndael256-cbc", "Rijndael/CBC" },
	{ "twofish128-cbc",  "Twofish/CBC" },
	{ "twofish192-cbc",  "Twofish/CBC" },
	{ "twofish256-cbc",  "Twofish/CBC" },
	{ "twofish-cbc",     "Twofish/CBC" },
	{ "twofish-ecb",     "Twofish/ECB" },
	{ "twofish-cfb",     "Twofish/CFB" },
	{ "twofish-ofb",     "Twofish/OFB" },
	{ "cast128-cbc",     "CAST128/CBC" },
	{ "cast128-ecb",     "CAST128/ECB" },
	{ "cast128-cfb",     "CAST128/CFB" },
	{ "cast128-ofb",     "CAST128/OFB" },
	{ "idea-cbc",        "IDEA/CBC" },
	{ "idea-ecb",        "IDEA/ECB" },
	{ "idea-cfb",        "IDEA/CFB" },
	{ "idea-ofb",        "IDEA/OFB" },
	{ "arcfour",         "RC4/OFB" }
    };

    final static String[][] macs = {
	{ "hmac-sha1", "HmacSHA1" },
	{ "hmac-md5", "HmacMD5" },
	{ "hmac-ripemd160", "HmacRIPEMD160" },
	{ "hmac-sha1-96", "HmacSHA1-96" },
	{ "hmac-md5-96", "HmacMD5-96" },
	{ "hmac-ripemd160-96", "HmacRIPEMD160-96" },
	{ "hmac-ripemd160@openssh.com", "HmacRIPEMD160" }
    };

    final static String[] fields = {
	"kex-algorithms",
	"server-host-key-algorithms",
	"enc-algorithms-cli2srv",
	"enc-algorithms-srv2cli",
	"mac-algorithms-cli2srv",
	"mac-algorithms-srv2cli",
	"comp-algorithms-cli2srv",
	"comp-algorithms-srv2cli",
	"languages-cli2srv",
	"languages-srv2cli"
    };

    String[] preferences;

    String pkgVersion;

    String kexAlgorithm;
    String hostKeyAlgorithm;
    String rxCipherName;
    String rxMacName;
    String rxCompName;
    String rxLang;
    String txCipherName;
    String txMacName;
    String txCompName;
    String txLang;

    int compressionLevel;

    boolean sameKEXGuess;
    boolean haveAgreed;

    public SSH2TransportPreferences() {
	preferences = new String[10];
    }

    public SSH2TransportPreferences(String[] preferences) {
	this.preferences = preferences;
    }

    public SSH2TransportPreferences(Properties props) {
	this();
	int i;
	for(i = 0; i < 10; i++) {
	    String v = props.getProperty(fields[i]);
	    preferences[i] = v;
	}
	pkgVersion = props.getProperty("package-version");
	try {
	    compressionLevel =
		Integer.parseInt(props.getProperty("compression"));
	} catch (Exception e) {
	    compressionLevel = 6;
	}
    }

    public void setPackageVersion(String pkgVersion) {
	this.pkgVersion = pkgVersion;
    }

    public String getPackageVersion() {
	return pkgVersion;
    }

    public void readFrom(SSH2TransportPDU pdu) {
	for(int i = 0; i < 10; i++) {
	    preferences[i] = new String(pdu.readString());
	}
    }

    public void writeTo(SSH2TransportPDU pdu) {
	for(int i = 0; i < 10; i++) {
	    pdu.writeString(preferences[i]);
	}
    }

    public boolean sameKEXGuess() {
	return sameKEXGuess;
    }

    public boolean canAgree(SSH2TransportPreferences peerPrefs,
			    boolean weAreAServer) {
	rxCipherName = chooseReceiverPref(CIPHER, peerPrefs, weAreAServer);
	rxMacName    = chooseReceiverPref(MAC, peerPrefs, weAreAServer);
	rxCompName   = chooseReceiverPref(COMPRESSION, peerPrefs, weAreAServer);
	rxLang       = chooseReceiverPref(LANGUAGE, peerPrefs, weAreAServer);

	txCipherName = chooseTransmitterPref(CIPHER, peerPrefs, weAreAServer);
	txMacName    = chooseTransmitterPref(MAC, peerPrefs, weAreAServer);
	txCompName   = chooseTransmitterPref(COMPRESSION, peerPrefs, weAreAServer);
	txLang       = chooseTransmitterPref(LANGUAGE, peerPrefs, weAreAServer);

	if(rxCipherName == null ||
	   rxMacName    == null ||
	   rxCompName   == null ||
	   txCipherName == null ||
	   txMacName    == null ||
	   txCompName   == null) {
	    haveAgreed   = false;
	    sameKEXGuess = false;
	} else {
	    haveAgreed = true;
	}

	return haveAgreed;
    }

    public String getKEXAlgorithm() {
	return kexAlgorithm;
    }

    public String getHostKeyAlgorithm() {
	return hostKeyAlgorithm;
    }

    public String getReceiverCipher() {
	return rxCipherName;
    }

    public String getReceiverMac() {
	return rxMacName;
    }

    public String getReceiverCompression() {
	return rxCompName;
    }

    public int getCompressionLevel() {
	return compressionLevel;
    }

    public String getTransmitterCipher() {
	return txCipherName;
    }

    public String getTransmitterMac() {
	return txMacName;
    }

    public String getTransmitterCompression() {
	return txCompName;
    }

    public String listPreference(int type) {
	return preferences[type];
    }

    public void setPreference(int type, String list) {
	preferences[type] = list;
    }

    public boolean isSupported(int type, String item) {
	String list = listPreference(type);
	return SSH2ListUtil.isInList(list, item);
    }

    public SSH2KeyExchanger
	selectKEXAlgorithm(SSH2TransportPreferences peerPrefs,
			   boolean weAreAServer)
	throws SSH2KEXFailedException
    {
	SSH2KeyExchanger kexImpl = null;
	String           cliKEXList, srvKEXList, cliHKAList, srvHKAList;

	if(weAreAServer) {
	    cliKEXList = peerPrefs.listPreference(KEX_ALGORITHMS);
	    srvKEXList = listPreference(KEX_ALGORITHMS);
	    cliHKAList = peerPrefs.listPreference(HOST_KEY_ALG);
	    srvHKAList = listPreference(HOST_KEY_ALG);
	} else {
	    cliKEXList = listPreference(KEX_ALGORITHMS);
	    srvKEXList = peerPrefs.listPreference(KEX_ALGORITHMS);
	    cliHKAList = listPreference(HOST_KEY_ALG);
	    srvHKAList = peerPrefs.listPreference(HOST_KEY_ALG);
	}

	kexAlgorithm = SSH2ListUtil.getFirstInList(cliKEXList);

	while(kexAlgorithm != null) {
	    kexImpl          = SSH2KeyExchanger.getInstance(kexAlgorithm);
	    hostKeyAlgorithm = chooseHostKeyAlgorithm(cliHKAList, srvHKAList,
					      kexImpl.getHostKeyAlgorithms());
	    if(hostKeyAlgorithm != null) {
		break;
	    }
	    cliKEXList = SSH2ListUtil.removeFirstFromList(cliKEXList,
							  kexAlgorithm);
	    kexAlgorithm = SSH2ListUtil.getFirstInList(cliKEXList);
	}

	if(kexAlgorithm == null) {
	    throw new SSH2KEXFailedException("Client kex algorithms empty");
	}

	sameKEXGuess =
	    kexAlgorithm.equals(SSH2ListUtil.getFirstInList(srvKEXList)) &&
	    hostKeyAlgorithm.equals(SSH2ListUtil.getFirstInList(srvHKAList));

	return kexImpl;
    }

    public String chooseHostKeyAlgorithm(String cliHKAList, String srvHKAList,
					 String kexHKAList) {
	String alg = SSH2ListUtil.chooseFromList(cliHKAList, kexHKAList);
	while(alg != null && !SSH2ListUtil.isInList(srvHKAList, alg)) {
	    cliHKAList = SSH2ListUtil.removeFirstFromList(cliHKAList, alg);
	    alg        = SSH2ListUtil.chooseFromList(cliHKAList, kexHKAList);
	}
	return alg;
    }

    public String chooseTransmitterPref(int type,
					SSH2TransportPreferences peerPrefs,
					boolean weAreAServer) {
	String clientList, serverList;
	if(weAreAServer) {
	    clientList = peerPrefs.listPreference(type + 1);
	    serverList = listPreference(type + 1);
	} else {
	    clientList = listPreference(type);
	    serverList = peerPrefs.listPreference(type);
	}
	return SSH2ListUtil.chooseFromList(clientList, serverList);
    }

    public String chooseReceiverPref(int type,
				     SSH2TransportPreferences peerPrefs,
				     boolean weAreAServer) {
	String clientList, serverList;
	if(weAreAServer) {
	    clientList = peerPrefs.listPreference(type);
	    serverList = listPreference(type);
	} else {
	    clientList = listPreference(type + 1);
	    serverList = peerPrefs.listPreference(type + 1);
	}
	return SSH2ListUtil.chooseFromList(clientList, serverList);
    }

    public static String ssh2ToJCECipher(String prefCipher) {
	for(int i = 0; i < ciphers.length; i++) {
	    if(ciphers[i][0].equals(prefCipher))
		return ciphers[i][1];
	}
	return null;
    }

    public static String ssh2ToJCEMac(String prefMac) {
	for(int i = 0; i < macs.length; i++) {
	    if(macs[i][0].startsWith(prefMac))
		return macs[i][1];
	}
	return null;
    }

    public static int getCipherKeyLen(String cipherName) {
	int len = 128;
	if(cipherName != null) {
	    cipherName = cipherName.toLowerCase();
	    if(cipherName.indexOf("128") != -1) {
		len = 128;
	    } else if(cipherName.indexOf("192") != -1) {
		len = 192;
	    } else if(cipherName.indexOf("256") != -1) {
		len = 256;
	    } else if(cipherName.startsWith("twofish") ||
		      cipherName.startsWith("rijndael") ||
		      cipherName.startsWith("aes")) {
		len = 256;
	    } else if(cipherName.startsWith("3des")) {
		len = 192;
	    }
	}
	return len / 8;
    }

    public static int getMacKeyLen(String macName) {
	int len = 16;
	if(macName != null && ((macName.indexOf("SHA") != -1) ||
			       (macName.indexOf("sha") != -1) ||
			       (macName.indexOf("ripemd160") != -1))) {
	    len = 20;
	}
	return len;
    }

}
