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

package com.mindbright.ssh;

import java.io.*;
import java.util.*;

import com.mindbright.terminal.Terminal;
import com.mindbright.sshcommon.SSHConsoleRemote;

import com.isnetworks.ssh.*;
import java.awt.event.*;

/**
 * Browser for files on remote machine.  When initialized it kicks up a new SSH
 * connection to the remote machine.  The file list is populated using results
 * from the "ls" command and manipulations are done with simple shell commands.
 * The response to each command is also processed by this class and the GUI is
 * updated asynchronously.
 */
public class SSHRemoteFileBrowsingConsole extends AbstractFileBrowser {

    private final class StdOutParser extends OutputStream {

	private String mOutput = "";

	public void write(int b) throws IOException {
	    write(new byte[] { (byte)b }, 0, 1);
	}

	public void write(byte[] b, int off, int len) throws IOException {
	    mOutput = mOutput + new String(b, off, len);
	    // Check if there's enough output to parse yet
	    while ( mOutput.indexOf( "****END LS****" ) != -1 ) {
		StringTokenizer st = new StringTokenizer( mOutput, "\n" );
		
		//  Ignore "****START PWD****"
		String line = st.nextToken();
		while(line.indexOf("****START PWD****") == -1) {
		    line = st.nextToken();
		}

		mCurrentDirectory = st.nextToken();
		if ( !mCurrentDirectory.endsWith( "/" ) ) {
		    mCurrentDirectory += "/";
		}

		//  Ignore "****END PWD****"
		line = st.nextToken();

		Vector v = new Vector();

		if(!mCurrentDirectory.equals("/")) {
		    v.addElement(new FileListItem("..", mCurrentDirectory,
						   true, "/" ));
		}

		// Parse all the file listings, knowing we're done when we hit a "****"
		while ( !( line = st.nextToken() ).startsWith( "****" ) ) {
		    boolean directory = line.endsWith( "/" );
		    String name = line;
		    if ( directory ) {
			name = line.substring( 0, line.length() - 1 );
		    }
		    v.addElement(new FileListItem(name, mCurrentDirectory,
						  directory, "/"));
		}

		FileListItem[] list = new FileListItem[v.size()];
		for(int i = 0; i < list.length; i++) {
		    list[i] = (FileListItem)v.elementAt(i);
		}

		// Sort the array
		FileListItem.sort(list);
		mFileDisplay.setFileList(list, mCurrentDirectory );

		// Delete the output we've already consumed
		mOutput = mOutput.substring( mOutput.indexOf( "****END LS****" ) + "****END LS****".length() );
	    }
	}
    }

    /** Console on remote machine */
    private SSHConsoleRemote remote;
    private OutputStream     stdin;

    /** Name of current directory on remote machine */
    private String mCurrentDirectory;

    /** Place to report errors to */
    private SSHSCPDialog mErrorLog;

    public SSHRemoteFileBrowsingConsole(FileDisplay fileDisplay,
					SSHPropertyHandler propertyHandler,
					SSHSCPDialog errorLog,
					SSHConsoleRemote remote) {
	super(fileDisplay, propertyHandler);
	this.mErrorLog = errorLog;
	this.remote    = remote;
    }

    /**
     * Kick up a new connection to the remote machine, killing the current
     * one if it's still active
     */
    public void initialize() throws SSHException {
	if(!remote.connect()) {
	    throw
		new SSHException( "Error when connecting with remote machine");
	}
	stdin = remote.getStdIn();
	remote.changeStdOut(new StdOutParser());
	refresh();
    }

    /**
     * Shut down the connection to the remote machine if it's active
     */
    public void disconnect() {
	remote.close();
    }

    /**
     * Rather ugly way to get the current directory on the server and a list
     * of files
     */
    public void refresh() throws SSHException {
	StringBuffer command = new StringBuffer();
	command.append("echo \"****START PWD****\"\n");
	command.append("pwd\n");
	command.append("echo \"****END PWD****\"\n");
	command.append("ls -A -L -p -1\n");
	command.append("echo \"****END LS****\"\n");
	doCommand(command);
    }

    /**
     * Executes a "mkdir" on the remote machine
     */
    public void makeDirectory( String directoryName ) throws SSHException {
	StringBuffer command = new StringBuffer();
	command.append( "mkdir \"" );
	command.append( directoryName );
	command.append( "\"\n" );
	doCommand(command);	
    }

    /**
     * Executes a "mv" on the remote machine
     */
    public void rename( FileListItem oldFile, String newName ) throws SSHException {
	StringBuffer command = new StringBuffer();
	command.append("mv \"");
	command.append(oldFile.getAbsolutePath());
	command.append("\" \"");
	command.append(oldFile.getParent());
	command.append(newName);
	command.append("\"\n");
	doCommand(command);
    }

    /**
     * Does a "cd" on the remote machine
     */
    public void changeDirectory( String directoryName ) throws SSHException {
	StringBuffer command = new StringBuffer();
	command.append( "cd \"" );
	command.append( directoryName );
	command.append( "\"\n" );
	doCommand(command);	
    }

    /**
     * Does a "rmdir" for directories in the array and a "rm" for files
     * Will not delete non-empty directories
     */
    public void delete( FileListItem[] fileListItem ) throws SSHException {
	StringBuffer command = new StringBuffer();
	for( int i = 0; i < fileListItem.length; i++ ) {
	    if ( fileListItem[ i ].isDirectory() ) {
		command.append( "rmdir \"" );
	    }
	    else {
		command.append( "rm -f \"" );
	    }
	    command.append( fileListItem[ i ].getAbsolutePath() );
	    command.append( "\"\n" );
	}
	doCommand(command);	
    }

    public void doCommand(StringBuffer command) throws SSHException {
	try {
	    stdin.write(command.toString().getBytes());
	} catch (IOException e) {
	    throw new SSHException("Error sending command to remote machine");
	}
    }

    /**
     * An error occurred when executing a server command, report it
     * to the user
     */
    // !!! REMOVE TODO, move to interactor or something (i.e. changeStdErr...)
    public void stderrWriteString(byte[] str) {
	String errorMessage = new String( str );
	mErrorLog.logError( new SSHException( "Error: " + errorMessage.trim() ) );
    }

    /**
     * User double clicked on a file in the list.  Check if it's a directory
     * and change to it if it is.
     */
    public void fileDoubleClicked( FileListItem fileListItem )
	throws SSHException
    {
	if(fileListItem.isDirectory()) {
	    StringBuffer command = new StringBuffer();
	    command.append( "cd \"" );
	    command.append( fileListItem.getAbsolutePath() );
	    command.append( "\"\n" );
	    doCommand(command);
	}
    }
}
