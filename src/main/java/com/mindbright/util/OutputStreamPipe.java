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

import java.io.OutputStream;
import java.io.IOException;

public final class OutputStreamPipe extends OutputStream {

    private InputStreamPipe sink;

    public OutputStreamPipe(InputStreamPipe sink)  throws IOException {
	connect(sink);
    }

    public OutputStreamPipe() {
    }

    public void connect(InputStreamPipe sink) throws IOException {
	if(this.sink == sink) {
	    return;
	}
	if(this.sink != null) {
	    throw new IOException("Already connected");
	}
	this.sink = sink;
    }

    public void write(int b) throws IOException {
	sink.put(b);
    }

    public void write(byte b[], int off, int len) throws IOException {
	sink.put(b, off, len);
    }

    public void flush() {
	if(sink != null) {
	    sink.flush();
	}
    }

    public void close() throws IOException {
	sink.eof();
    }

}
