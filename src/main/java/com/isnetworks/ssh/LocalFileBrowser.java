/*
 * ====================================================================
 *
 * License for ISNetworks' MindTerm SCP modifications
 *
 * Copyright (c) 2001 ISNetworks, LLC.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include an acknowlegement that the software contains
 *    code based on contributions made by ISNetworks, and include
 *    a link to http://www.isnetworks.com/.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 */

/**
* Browser to handle file manipulation on the local machine
*/
package com.isnetworks.ssh;

import java.io.*;
import com.mindbright.ssh.*;


public class LocalFileBrowser extends AbstractFileBrowser {
	
    private File cwd;

    public LocalFileBrowser( FileDisplay fileDisplay, SSHPropertyHandler propertyHandler ) {
	super( fileDisplay, propertyHandler );
    }
	
    /**
     * Jump to the default SSH home directory
     */
    public void initialize() throws SSHException {
	changeDirectory(mPropertyHandler.getSSHHomeDir());
	refresh();
    }

    public void refresh() throws SSHException {
	int            li        = 0;
	String[]       fileNames = cwd.list();
	boolean        isRoot    = cwd.getParent() == null;
	FileListItem[] list      = new FileListItem[fileNames.length +
						   (isRoot ? 0 : 1)];

	// Add link to parent directory if we're not already at the root
	if(!isRoot) {
	    list[li++] = new FileListItem("..", "", true, File.separator);
	}

	String dir = null;

	try {
	    dir = cwd.getCanonicalPath();
	} catch(IOException e) {
	    throw new SSHException( "Unable to refresh file list" );
	}

	// Add each file and directory in the list
	for(int i = 0; i < fileNames.length; i++) {
	    File f = new File(cwd, fileNames[i]);
	    list[li++] = new FileListItem(fileNames[i], dir,
					  f.isDirectory(), File.separator,
					  f.length());
	}

	// Sort the array since File.list() does not define an order for the results
	FileListItem.sort(list);
		
	// Set list in the GUI
	if(!dir.endsWith(File.separator)) {
	    dir += File.separator;
	}
	mFileDisplay.setFileList(list, dir);
    }

    public void makeDirectory( String directoryName ) throws SSHException {
	File newDirectory = new File( cwd, directoryName );
	if ( !newDirectory.mkdirs() ) {
	    throw new SSHException( "Unable to make directory: " + newDirectory.getAbsolutePath() );
	}
    }

    public void delete( FileListItem[] fileListItem ) throws SSHException {
	for( int i = 0; i < fileListItem.length; i++ ) {
	    File deleteFile = new File( fileListItem[ i ].getParent(), fileListItem[ i ].getName() );
	    if ( !deleteFile.delete() ) {
		throw new SSHException( "Unable to delete " + fileListItem[ i ].getAbsolutePath() + " - may not have permission or directory may not be empty" );
	    }
	}
		
    }

    public void changeDirectory( String directoryName ) throws SSHException {
	File newDirectory = new File( directoryName );
	if ( !newDirectory.exists() ) {
	    throw new SSHException( "Directory " + directoryName + " does not exist or you do not have permission to access it." );		
	}
	if( newDirectory.isFile() ) {
	    throw new SSHException( directoryName + " a file, not a directory." );
	}
	// This is the right way to do it, but it doesn't work under Netscape
	// if the directory has a space in its name.  Nice work, Netscape!
	//		if( !newDirectory.isDirectory() ) {
	//			throw new SSHException( directoryName + " is not a directory." );
	//		}
	cwd = newDirectory;
    }

    public void rename(FileListItem oldFileListItem, String newName)
	throws SSHException
    {
	File oldFile = new File(oldFileListItem.getParent(),
				oldFileListItem.getName());
	File newFile = null;
	if(!newName.startsWith(File.separator)) {
	    newFile = new File(cwd, newName);
	} else {
	    newFile = new File(newName);
	}

	if(!oldFile.renameTo(newFile)) {
	    throw new SSHException("Unable to rename file " +
				   oldFileListItem.getAbsolutePath() +
				   " to " + newName);
	}
		
    }

    public void fileDoubleClicked( FileListItem fileListItem )
	throws SSHException
    {

	if(fileListItem.isDirectory()) {
	    File newDirectory = null;

	    if(fileListItem.getName().equals( ".." )) {
		newDirectory = new File(cwd.getParent());
	    } else {
		newDirectory = new File(fileListItem.getParent(),
					fileListItem.getName());
	    }

	    if (!newDirectory.exists() || !newDirectory.isDirectory()) {
		throw new SSHException("Unable to open directory: " +
				       newDirectory.getAbsolutePath());
	    }

	    cwd = newDirectory;

	}	
    }

}
