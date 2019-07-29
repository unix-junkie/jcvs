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

package com.mindbright.jca.security;

import java.util.Properties;
import java.util.Vector;
import java.util.Enumeration;

public final class Security {

    private static Vector providers;

    static {
	providers = new Vector();
    };

    public static int addProvider(Provider provider) {
	if(getProvider(provider.getName()) != null)
	    return -1;
	providers.addElement(provider);
	return providers.size();
    }

    public static Provider getProvider(String name) {

	// !!! REMOVE !!! shortcircuit
	//
	if(providers.size() == 0) {
	    providers.addElement(new com.mindbright.security.Mindbright());
	}

	Enumeration enum = providers.elements();
	while(enum.hasMoreElements()) {
	    Provider prov = (Provider)enum.nextElement();
	    if(prov.getName().equals(name)) {
		return prov;
	    }
	}
	return null;
    }

    public static String getAlgorithmProperty(String algName, String propName) {
	return null;
    }

    public static String getProperty(String key) {
	return null;
    }

    public static Provider[] getProviders() {
	return null;
    }

    /*
    public static Provider[] getProviders(Map filter) {
    }
    */

    public static Provider[] getProviders(String filter) {
	return null;
    }

    public static int insertProviderAt(Provider provider, int position) {
	return 0;
    }

    public static void removeProvider(String name) {
    }

    public static void setProperty(String key, String datum) {
    }

}
