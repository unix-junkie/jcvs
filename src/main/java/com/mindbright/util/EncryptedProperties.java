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

package com.mindbright.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Enumeration;

import com.mindbright.jca.security.MessageDigest;

// !!! TODO, change this to real cipher
import com.mindbright.ssh.SSHCipher;
import com.mindbright.ssh.SSHAccessDeniedException;

public class EncryptedProperties extends Properties {
    public final static String HASH_KEY     = "EncryptedProperties.hash";
    public final static String CIPHER_KEY   = "EncryptedProperties.cipher";
    public final static String CONTENTS_KEY = "EncryptedProperties.contents";
    public final static String SIZE_KEY     = "EncryptedProperties.size";
    public final static String PROPS_HEADER = "Sealed with com.mindbright.util.EncryptedProperties" +
	"(ver. $Name:  $" + "$Date: 2001/09/17 09:35:44 $)";

    private boolean isNormalPropsFile;

    public EncryptedProperties() {
	super();
	isNormalPropsFile = false;
    }

    public EncryptedProperties(Properties defaultProperties) {
	super(defaultProperties);
	isNormalPropsFile = false;
    }

    public boolean isNormalPropsFile() {
	return isNormalPropsFile;
    }

    public synchronized void save(OutputStream out, String header, String password,
				  String cipherName) throws IOException {
	ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
	Properties            encProps = new Properties();
	byte[]                contents, hash;
	String                hashStr;
	SSHCipher             cipher = SSHCipher.getInstance(cipherName);
	int                   size;

	if(cipher == null)
	    throw new IOException("Unknown cipher '" + cipherName + "'");

	save(bytesOut, header);

	contents = bytesOut.toByteArray();
	size = contents.length;
	try {
	    MessageDigest md5 = MessageDigest.getInstance("MD5");
	    md5.update(contents);
	    hash = md5.digest();
	} catch(Exception e) {
	    throw new IOException("MD5 not implemented, can't generate session-id");
	}

	hash    = Base64.encode(hash);
	hashStr = new String(hash);

	// Assume cipher-block length no longer than 8
	//
	byte[] tmp = new byte[contents.length + (8 - (contents.length % 8))];
	System.arraycopy(contents, 0, tmp, 0, contents.length);
	contents = new byte[tmp.length];

	cipher.setKey(hashStr + password);
	cipher.encrypt(tmp, 0, contents, 0, contents.length);

	contents = Base64.encode(contents);

	encProps.put(HASH_KEY, new String(hash));
	encProps.put(CIPHER_KEY, cipherName.substring(3)); // !!! Cut pre 'SSH'
	encProps.put(CONTENTS_KEY, new String(contents));
	encProps.put(SIZE_KEY, String.valueOf(size));
	encProps.save(out, PROPS_HEADER);
	out.flush();
    }

    public synchronized void load(InputStream in, String password) throws IOException, SSHAccessDeniedException {
	Properties encProps = new Properties();
	String     hashStr, cipherName, contentsStr, sizeStr;
	byte[]     contents, hash, hashCalc;
	SSHCipher  cipher;
	int        size;

	encProps.load(in);

	hashStr     = encProps.getProperty(HASH_KEY);
	cipherName  = "SSH" + encProps.getProperty(CIPHER_KEY);
	contentsStr = encProps.getProperty(CONTENTS_KEY);
	sizeStr     = encProps.getProperty(SIZE_KEY);

	// Assume normal properties if our keys are not found (i.e. for
	// "backwards compatible" reading of properties which will be encrypted
	// if saved)
	//
	if(hashStr == null || cipherName == null ||
	   contentsStr == null || sizeStr == null) {
	    isNormalPropsFile = true;
	    Enumeration keys = encProps.keys();
	    while(keys.hasMoreElements()) {
		String key = (String)keys.nextElement();
		put(key, encProps.getProperty(key));
	    }
	    return;
	}

	size = Integer.parseInt(sizeStr);

	hash     = Base64.decode(hashStr.getBytes());
	contents = Base64.decode(contentsStr.getBytes());

	cipher = SSHCipher.getInstance(cipherName);
	if(cipher == null)
	    throw new IOException("Unknown cipher '" + cipherName + "'");

	cipher.setKey(hashStr + password);
	cipher.decrypt(contents, 0, contents, 0, contents.length);

	byte[] tmp = new byte[size];
	System.arraycopy(contents, 0, tmp, 0, size);
	contents = tmp;

	try {
	    MessageDigest md5 = MessageDigest.getInstance("MD5");
	    md5.update(contents);
	    hashCalc = md5.digest();
	} catch(Exception e) {
	    throw new IOException("MD5 not implemented, can't generate session-id");
	}

	for(int i = 0; i < hash.length; i++) {
	    if(hash[i] != hashCalc[i])
		throw new SSHAccessDeniedException("Access denied");
	}

	ByteArrayInputStream bytesIn = new ByteArrayInputStream(contents);
	load(bytesIn);
    }

    /* !!! DEBUG
    public static void main(String[] argv) {
	EncryptedProperties test = new EncryptedProperties();

	test.put("Foo", "bar");
	test.put("foo", "bAR");
	test.put("bar", "FOO");
	test.put("BAR", "foo");

	try {
	    test.save(new java.io.FileOutputStream("/tmp/fooprops"), "These are some meaningless props...",
		      "foobar", "Blowfish");
	    test = new EncryptedProperties();
	    test.load(new java.io.FileInputStream("/tmp/fooprops"), "foobar");

	    System.out.println("test: " + test.getProperty("BAR") + test.getProperty("Foo"));

	} catch (Exception e) {
	    System.out.println("Error:" + e);
	    e.printStackTrace();
	}
    }
    */

}
