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

import java.util.StringTokenizer;

// !!! TODO, rewrite this mess to a real list handling class instead !!!
//
public final class SSH2ListUtil {
    public static String chooseFromList(String clientList, String serverList) {
	String[] cliL = arrayFromList(clientList);
	String[] srvL = arrayFromList(serverList);
	for(int i = 0; i < cliL.length; i++) {
	    for(int j = 0; j < srvL.length; j++) {
		if(cliL[i].equals(srvL[j])) {
		    return cliL[i];
		}
	    }
	}
	return null;
    }

    public static String removeAllFromList(String list, String element) {
	StringBuffer buf = new StringBuffer();
	String[]     arr = arrayFromList(list);
	for(int i = 0; i < arr.length; i++) {
	    if(arr[i].equals(element))
		continue;
	    if(buf.length() > 0)
		buf.append(",");
	    buf.append(arr[i]);
	}
	return buf.toString();
    }

    public static String removeAllPrefixFromList(String list, String prefix)
    {
	StringBuffer buf = new StringBuffer();
	String[]     arr = arrayFromList(list);
	for(int i = 0; i < arr.length; i++) {
	    if(arr[i].startsWith(prefix))
		continue;
	    if(buf.length() > 0)
		buf.append(",");
	    buf.append(arr[i]);
	}
	return buf.toString();
    }

    public static String removeFirstFromList(String list, String element) {
	StringBuffer buf = new StringBuffer();
	String[]     arr = arrayFromList(list);
	for(int i = 0; i < arr.length; i++) {
	    if(arr[i].equals(element)) {
		element = "";
		continue;
	    }
	    if(buf.length() > 0)
		buf.append(",");
	    buf.append(arr[i]);
	}
	return buf.toString();
    }

    public static String getFirstInList(String list) {
	String[] arr = arrayFromList(list);
	if(arr.length == 0) {
	    return null;
	}
	return arr[0];
    }

    public static boolean isInList(String list, String element) {
	String[] arr = arrayFromList(list);
	for(int i = 0; i < arr.length; i++) {
	    if(arr[i].equals(element)) {
		return true;
	    }
	}
	return false;
    }

    public static boolean isPrefixInList(String list, String prefix) {
	String[] arr = arrayFromList(list);
	for(int i = 0; i < arr.length; i++) {
	    if(arr[i].startsWith(prefix)) {
		return true;
	    }
	}
	return false;
    }

    public static String sortList(String list) {
	String[] arr = arrayFromList(list);
	for(int i = 0; i < arr.length; i++) {
	    for(int j = i; j < arr.length; j++) {
		if(!(arr[i].compareTo(arr[j])< 0)) {
		    String tmp = arr[j];
		    arr[j] = arr[i];
		    arr[i] = tmp;
		}
	    }
	}
	return listFromArray(arr);
    }

    public static String[] arrayFromList(String list) {
	if(list == null) {
	    return new String[0];
	}
	StringTokenizer st = new StringTokenizer(list, ",");
	String[] sa = new String[32];
	int cnt = 0;
	while(st.hasMoreTokens()) {
	    sa[cnt++] = st.nextToken().trim();
	}
	String[] tmp = new String[cnt];
	for(cnt = 0; cnt < tmp.length; cnt++) {
	    tmp[cnt] = sa[cnt];
	}
	return tmp;
    }

    public static String listFromArray(String[] arr) {
	StringBuffer buf = new StringBuffer();
	for(int i = 0; i < arr.length; i++) {
	    if(arr[i] == null || arr[i].equals("")) {
		continue;
	    }
	    if(buf.length() > 0)
		buf.append(",");
	    buf.append(arr[i]);
	}
	return buf.toString();
    }

}
