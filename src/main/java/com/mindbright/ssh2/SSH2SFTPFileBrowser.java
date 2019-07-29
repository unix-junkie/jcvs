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

import com.isnetworks.ssh.AbstractFileBrowser;
import com.isnetworks.ssh.SSHException;
import com.isnetworks.ssh.FileListItem;
import com.isnetworks.ssh.FileDisplay;

public class SSH2SFTPFileBrowser extends AbstractFileBrowser {

    private SSH2Connection connection;
    private SSH2SFTPClient client;
    private String         cwd;

    public SSH2SFTPFileBrowser(SSH2Connection connection,
			       FileDisplay fileDisplay)
    {
	super(fileDisplay, null);
	this.connection = connection;
    }

    public void fileDoubleClicked(FileListItem file) throws SSHException {
	if(file.isDirectory()) {
	    changeDirectory(cwd + "/" + file.getName());
	}
    }

    public void refresh() throws SSHException {
	SSH2SFTP.FileHandle handle = null;
	try {
	    handle = client.opendir(cwd);
	    SSH2SFTP.FileAttributes[] list = client.readdir(handle);
	    FileListItem[] files = new FileListItem[list.length];
	    int li = 0;

	    if(!cwd.equals("/") && !cwd.equals("")) {
		files[li++] = new FileListItem("..", "", true, "/");
	    }

	    for(int i = 0; i < list.length; i++) {
		String name = list[i].name;
		if(!("..".equals(name)) && !(".".equals(name))) {
		    files[li++] = new FileListItem(name, cwd,
						   list[i].isDirectory(),
						   "/", list[i].size);
		}
	    }

	    FileListItem[] tmp = new FileListItem[li];
	    System.arraycopy(files, 0, tmp, 0, li);
	    files = tmp;

	    FileListItem.sort(files);
	    String dir = cwd;
	    if(!dir.endsWith("/")) {
		dir += "/";
	    }
	    mFileDisplay.setFileList(files, dir);
	} catch (Exception e) {
	    throw new SSHException(e.getMessage());
	} finally {
	    try { client.close(handle); }
	    catch (Exception e) { /* don't care */ }
	}
    }

    public void delete(FileListItem[] files) throws SSHException {
	String file = null;
	try {
	    for(int i = 0; i < files.length; i++) {
		file = files[i].getAbsolutePath();
		SSH2SFTP.FileAttributes attrs = client.stat(file);
		if(attrs.isDirectory()) {
		    client.rmdir(file);
		} else {
		    client.remove(file);
		}
	    }
	} catch (SSH2SFTP.SFTPException e) {
	    throw new SSHException("Unable to delete " + file +
				   " - may not have permission or directory may not be empty");
	}
    }

    public void initialize() throws SSHException {
	try {
	    client = new SSH2SFTPClient(connection, true);
	    SSH2SFTP.FileAttributes attrs = client.realpath(".");
	    cwd = attrs.lname;
	    refresh();
	} catch (SSH2SFTP.SFTPException e) {
	    throw new SSHException("Could not start sftp session: " +
				   e.getMessage());
	}
    }

    public void makeDirectory(String directoryName) throws SSHException {
	try {
	    if(!directoryName.startsWith("/")) {
		directoryName = cwd + "/" + directoryName;
	    }
	    client.mkdir(directoryName, new SSH2SFTP.FileAttributes());
	} catch (SSH2SFTP.SFTPException e) {
	    throw new SSHException(e.getMessage());
	}
    }

    public void rename(FileListItem file, String newFileName)
	throws SSHException
    {
	try {
	    client.rename(file.getAbsolutePath(), file.getParent() + "/" +
			  newFileName);
	} catch (SSH2SFTP.SFTPException e) {
	    throw new SSHException(e.getMessage());
	}
    }

    public void changeDirectory(String newDir) throws SSHException {
	try {
	    if(!newDir.startsWith("/")) {
		newDir = cwd + "/" + newDir;
	    }
	    SSH2SFTP.FileAttributes attrs  = client.realpath(newDir);
	    SSH2SFTP.FileHandle     handle = client.opendir(newDir);
	    newDir = attrs.lname;
	    client.close(handle);
	} catch (SSH2SFTP.SFTPException e) {
	    newDir = cwd;
	}
	cwd = newDir;
    }

    public void disconnect() {
	if(client != null) {
	    client.terminate();
	}
    }

}
