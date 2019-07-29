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

public abstract class SSH2Exception extends Exception {

    protected Throwable rootCause;

    public SSH2Exception(String message) {
        this(message, null);
    }

    public SSH2Exception(String message, Throwable rootCause) {
        super(message);
        this.rootCause = rootCause;
    }

    public Throwable getRootCause() {
        return rootCause;
    }

}

