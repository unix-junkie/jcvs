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

package com.mindbright.net.ftp;

import java.io.InputStream;
import java.io.OutputStream;

public interface FTPServerEventHandler {
    public boolean login(String user, String pass);
    public void quit();

    public boolean isPlainFile(String file);
    public void changeDirectory(String dir) throws FTPException;
    public void renameFrom(String from) throws FTPException;
    public void renameTo(String to) throws FTPException;
    public void delete(String file) throws FTPException;
    public void rmdir(String dir) throws FTPException;
    public void mkdir(String dir) throws FTPException;
    public String pwd();
    public String system();
    public long modTime(String file) throws FTPException;
    public long size(String file) throws FTPException;

    //
    // !!! TODO, store/retrieve can return size and we can do some stats...
    //

    public void store(String file, InputStream data, boolean binary)
	throws FTPException;
    public void retrieve(String file, OutputStream data, boolean binary)
	throws FTPException;
    public void list(String path, OutputStream data) throws FTPException;
    public void nameList(String path, OutputStream data) throws FTPException;
    public void abort();
}
