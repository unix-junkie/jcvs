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

public class Provider extends Properties {
    String name;
    double version;
    String info;

    protected Provider(String name, double version, String info) {
	this.name    = name;
	this.version = version;
	this.info    = info;
    }

    public void clear() {
    }

    /*
    public Set entrySet() {
    }
    */

    public String getInfo() {
	return info;
    }

    public String getName() {
	return name;
    }

    public double getVersion() {
	return version;
    }

    /*
    public Set keySet() {
    }
    */

    /*
    public void load(InputStream inStream) {
    }
    */

    /*
    public Object put(Object key, Object value) {
	return null;
    }
    */

    /*
    public void putAll(Map t) {
    }
    */

    public Object remove(Object key) {
	return null;
    }

    public String toString() {
	return name + " (" + version + ")";
    }

    /*
    public Collection values() {
    }
    */
}
