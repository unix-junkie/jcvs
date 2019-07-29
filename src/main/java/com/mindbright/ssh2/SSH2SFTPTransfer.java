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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import com.mindbright.sshcommon.SSHFileTransfer;
import com.mindbright.sshcommon.SSHFileTransferProgress;

public class SSH2SFTPTransfer implements SSHFileTransfer,
					 SSH2SFTP.AsyncListener
{

    private SSHFileTransferProgress progress;
    private SSH2SFTPClient          client;
    private File                    cwd;

    public SSH2SFTPTransfer(File cwd, SSH2Connection connection)
	throws SSH2Exception
    {
	try {
	    this.cwd    = cwd;
	    this.client = new SSH2SFTPClient(connection, false);
	} catch (SSH2SFTP.SFTPException e) {
	    throw new SSH2FatalException("Could not start sftp session", e);
	}
    }

    public void setProgress(SSHFileTransferProgress progress) {
	this.progress = progress;
    }

    public void copyToRemote(String[] localFiles, String remoteFile,
			     boolean recursive)
	throws IOException
    {
	if(remoteFile == null || remoteFile.equals(""))
	    remoteFile = ".";

	for(int i = 0; i < localFiles.length; i++) {
	    File lf = new File(localFiles[i]);
	    if(!lf.isAbsolute())
		lf = new File(cwd, localFiles[i]);
	    if(!lf.isFile() && !lf.isDirectory()) {
		throw new IOException("File: " + lf.getName() +
				      " is not a regular file or directory");
	    }
	    try {
		writeFileToRemote(lf, remoteFile, recursive);
	    } catch (SSH2SFTP.SFTPException e) {
		throw new IOException("Error writing file: " + e.getMessage());
	    }
	}
    }

    public void copyToLocal(String localFile, String remoteFiles[],
			    boolean recursive)
	throws IOException
    {
	if(localFile == null || localFile.equals(""))
	    localFile = ".";

	File lf = new File(localFile);
	if(!lf.isAbsolute())
	    lf = new File(cwd, localFile);

	if(lf.exists() && !lf.isFile() && !lf.isDirectory()) {
	    throw new IOException("File: " + localFile +
				  " is not a regular file or directory");
	}

	for(int i = 0; i < remoteFiles.length; i++) {
	    String fName = remoteFiles[i];
	    try {
		readFileFromRemote(fName, localFile, recursive);
	    } catch (SSH2SFTP.SFTPException e) {
		throw new IOException(e.getMessage());
	    }
	}
    }

    private void writeFileToRemote(File file, String remoteFile,
				   boolean recursive)
	throws IOException, SSH2SFTP.SFTPException
    {
	String fName = file.getName();
	if(file.isDirectory() && recursive) {
	    writeDirToRemote(file, remoteFile);
	} else if(file.isFile()) {
	    if(progress != null)
		progress.startFile(fName, file.length());

	    FileInputStream     fin = new FileInputStream(file);
	    SSH2SFTP.FileHandle fh  = null;
	    try {
		fh = client.open(remoteFile + fName,
				 SSH2SFTP.SSH_FXF_WRITE |
				 SSH2SFTP.SSH_FXF_TRUNC |
				 SSH2SFTP.SSH_FXF_CREAT,
				 new SSH2SFTP.FileAttributes());
		fh.addAsyncListener(this);
		client.writeFully(fh, fin);
	    } finally {
		fin.close();
	    }

	    if(progress != null)
		progress.endFile();
	} else {
	    throw new IOException("Not ordinary file: " + fName);
	}
    }

    private void writeDirToRemote(File dir, String remoteDir)
	throws IOException, SSH2SFTP.SFTPException
    {
	if(progress != null)
	    progress.startDir(dir.getAbsolutePath());
	remoteDir += dir.getName();
	if(!remoteDir.endsWith("/")) {
	    remoteDir += "/";
	}
	try {
	    client.stat(remoteDir);
	} catch (SSH2SFTP.SFTPException e) {
	    client.mkdir(remoteDir, new SSH2SFTP.FileAttributes());
	}
	String[] dirList = dir.list();
	for(int i = 0; i < dirList.length; i++) {
	    File f = new File(dir, dirList[i]);
	    writeFileToRemote(f, remoteDir, true);
	}
	if(progress != null)
	    progress.endDir();
    }

    private void readFileFromRemote(String fName, String localFile,
				    boolean recursive)
	throws IOException, SSH2SFTP.SFTPException
    {
	SSH2SFTP.FileAttributes attrs = client.stat(fName);
	SSH2SFTP.FileHandle     fh    = null;

	File targetFile = new File(localFile);
	if(targetFile.isDirectory()) {
	    String f = fName;
	    int    n = fName.lastIndexOf('/');
	    if(n != -1) {
		f = fName.substring(n + 1);
	    }
	    targetFile = new File(targetFile, f);
	}

	if(attrs.isDirectory() && recursive) {
	    if(targetFile.exists()) {
		if(!targetFile.isDirectory()) {
		    throw new IOException("Invalid target " +
					  targetFile.getName() +
					  ", must be a directory");
		}
	    } else {
		if(!targetFile.mkdir()) {
		    throw new IOException("Could not create directory: " +
					  targetFile.getName());
		}
	    }
	    fh = client.opendir(fName);
	    SSH2SFTP.FileAttributes[] list = client.readdir(fh);
	    for(int i = 0; i < list.length; i++) {
		String name = list[i].name;
		if("..".equals(name) || ".".equals(name)) {
		    continue;
		}
		readFileFromRemote(fName + "/" + name,
				   targetFile.getAbsolutePath(), recursive);
	    }
	} else if(attrs.isFile()) {
	    FileOutputStream fout = new FileOutputStream(targetFile);
	    try {
		fh = client.open(fName, SSH2SFTP.SSH_FXF_READ,
				 new SSH2SFTP.FileAttributes());
		if(progress != null)
		    progress.startFile(targetFile.getName(), attrs.size);
		fh.addAsyncListener(this);
		client.readFully(fh, fout);
		if(progress != null)
		    progress.endFile();
	    } finally {
		fout.close();
	    }
	} else {
	    throw new IOException("Not ordinary file: " + fName);
	}
    }

    public void abort() {
	if(client != null) {
	    client.terminate();
	}
    }

    public void progress(long size) {
	progress.progress((int)size);
    }

}
