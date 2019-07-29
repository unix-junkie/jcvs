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

public class Log {
    public final static int LEVEL_EMERG   = 0;
    public final static int LEVEL_ALERT   = 1;
    public final static int LEVEL_ERROR   = 2;
    public final static int LEVEL_WARNING = 3;
    public final static int LEVEL_NOTICE  = 4;
    public final static int LEVEL_INFO    = 5;
    public final static int LEVEL_DEBUG   = 6;
    public final static int LEVEL_DEBUG2  = 7;

    volatile int currentLevel = 0;

    public Log(int level) {
	this.currentLevel = level;
    }

    public void message(int level, String callClass, String message) {
	message(level, callClass, null, message);
    }

    public synchronized void message(int level, String callClass,
				     String callMethod,
				     String message) {
	if(level <= currentLevel) {
	    String methStr = (callMethod != null ? "." + callMethod + "()" :
			      "");
	    System.err.println("** " + callClass + methStr + " : '" +
			       message + "'");
	}
    }

    public void error(String callClass, String callMethod, String message) {
	message(LEVEL_ERROR, callClass, callMethod, message);
    }

    public void warning(String callClass, String message) {
	message(LEVEL_ERROR, callClass, null, message);
    }

    public void notice(String callClass, String message) {
	message(LEVEL_NOTICE, callClass, null, message);
    }

    public void info(String callClass, String message) {
	message(LEVEL_INFO, callClass, null, message);
    }

    public void debug(String callClass, String callMethod, String message) {
	message(LEVEL_DEBUG, callClass, callMethod, message);
    }

    public void debug(String callClass, String message) {
	message(LEVEL_DEBUG, callClass, null, message);
    }

    public void debug2(String callClass, String callMethod, String message) {
	message(LEVEL_DEBUG2, callClass, callMethod, message);
    }

    public synchronized void debug2(String callClass, String callMethod,
				    String message,
				    byte[] dumpBuf, int off, int len) {
	message(LEVEL_DEBUG2, callClass, callMethod, message);
	if(currentLevel == LEVEL_DEBUG2) {
	    HexDump.print(dumpBuf, off, len);
	}
    }

    public void debug2(String callClass, String callMethod, String message,
		       byte[] dumpBuf) {
	debug2(callClass, callMethod, message, dumpBuf, 0, dumpBuf.length);
    }

    public void setLevel(int level) {
	currentLevel = level;
    }
}
