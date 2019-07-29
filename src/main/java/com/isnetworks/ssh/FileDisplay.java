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
* AWT Panel that represents a file system.  Has buttons for basic
* file administration operations and a list of the files in a
* given directory.
* 
* This code is based on a LayoutManager tutorial on Sun's Java web site.
* http://developer.java.sun.com/developer/onlineTraining/GUI/AWTLayoutMgr/shortcourse.html
*/
package com.isnetworks.ssh;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import com.mindbright.ssh.*;

/** This class represents a small pane which will list the files present
 *  on a given platform.  This pane was made into its own class to allow
 *  easy reuse as both the local and remote file displays
 *  
 *  This GUI is built up using "lazy instantiation" via method calls
 *  for each part of the component.
 */
public class FileDisplay extends Panel {

    public final static int BUT_CHDIR    = 0;
    public final static int BUT_MKDIR    = 1;
    public final static int BUT_RENAME   = 2;
    public final static int BUT_DELETE   = 3;
    public final static int BUT_REFRESH  = 4;
    public final static int ACT_DBLCLICK = 5;

    private class Actions implements ActionListener, ItemListener {
	private int action;

	public Actions(int action) {
	    this.action = action;
	}

	public void actionPerformed(ActionEvent e) {
	    try {
		netscape.security.PrivilegeManager.enablePrivilege("UniversalFileAccess");
	    } catch (netscape.security.ForbiddenTargetException ee) {
		// !!!
	    }

	    try {
		switch(action) {
		case BUT_CHDIR: {
		    String directoryName =
			SSHMiscDialogs.textInput("Change directory", "Directory", mOwnerFrame, getFileSystemLocationLabelText() );
		    if ( directoryName != null ) {
			mBrowser.changeDirectory( directoryName );
		    }
		    break;
		}
		case BUT_MKDIR: {
		    String directoryName =
			SSHMiscDialogs.textInput("Make directory relative to current path", "Directory name", mOwnerFrame );
		    if ( directoryName != null ) {
			mBrowser.makeDirectory( directoryName );
		    }
		    break;
		}
		case BUT_RENAME:
		    FileListItem mFileListItem =
			FileDisplay.this.getSelectedFile();
		    String newName =
			SSHMiscDialogs.textInput("Rename file",
						 "New file name", mOwnerFrame,
						 mFileListItem.getName());
		    if (newName != null) {
			mBrowser.rename(mFileListItem, newName);
		    }
		    break;
		case BUT_DELETE:
		    mBrowser.delete(FileDisplay.this.getSelectedFiles());
		    break;
		case BUT_REFRESH:
		    break;
		case ACT_DBLCLICK:
		    mBrowser.fileDoubleClicked( mFileList.getFileListItem( e.getActionCommand() ) );
		    break;
		}
		mBrowser.refresh();
	    } catch(SSHException ex) {
		mSCPDialog.logError(ex);
	    }
	}

	public void itemStateChanged(ItemEvent e) {
	    enableButtons();
	}

    }

    private FileBrowser mBrowser;

    private Button       mChgDirButton;
    private Button       mDeleteButton;
    private Panel        mFileButtonsInnerPanel;
    private Panel        mFileButtonsPanel;
    private Panel        mFileHeaderPanel;
    private FileList     mFileList;
    private Label        mMachineDescriptionLabel;
    private Label        mFileSystemLocationLabel;
    private Button       mMkDirButton;
    private Button       mRefreshButton;
    private Button       mRenameButton;

    /**
     * Frame to own dialog boxes
     */
    private Frame mOwnerFrame;
	
    /**
     * Reference to SCP main dialog box to send error messages to
     */
    private SSHSCPDialog mSCPDialog;

    /** Constructor 
     *  This defines the overall GUI for this component
     *  It's a BorderLayout with a header, a set of buttons & a list
     */
    public FileDisplay( Frame ownerFrame, String name, SSHSCPDialog scpDialog ) {
	mOwnerFrame = ownerFrame;
	mSCPDialog = scpDialog;
		
	mMachineDescriptionLabel = new Label( name );
		
	setLayout(new BorderLayout());
	setBackground(Color.lightGray);
	add("North",  getFileHeaderPanel());
	add("Center", getFileList());
	add("South",   getFileButtonsPanel());
    }

    /** The header panel -- contains labels for Remote/Local and the current directory */
    private Panel getFileHeaderPanel() {
	if (mFileHeaderPanel == null) {
	    mFileHeaderPanel = new Panel(new BorderLayout());
	    mFileHeaderPanel.add("North", getMachineDescriptionLabel());
	    mFileHeaderPanel.add("South", getFileSystemLocationLabel());
	}
	return mFileHeaderPanel;
    }

    /** The label to show which system this file display refers to */
    private Label getMachineDescriptionLabel() {
	// Created in constructor
	return mMachineDescriptionLabel;
    }

    /** The label to show which directory this display refers to */
    private Label getFileSystemLocationLabel() {
	if (mFileSystemLocationLabel == null) {
	    mFileSystemLocationLabel = new Label("");
	}
	return mFileSystemLocationLabel;
    }

    /** This is merely a wrapper to bind the set of buttons to their
     *   preferred height
     */
    private Panel getFileButtonsPanel() {
	if (mFileButtonsPanel == null) {
	    mFileButtonsPanel = new Panel(new BorderLayout());
	    mFileButtonsPanel.add("North", getFileButtonsInnerPanel());
	}
	return mFileButtonsPanel;
    }

    /** The panel containing the buttons for the file list */
    private Panel getFileButtonsInnerPanel() {
	if (mFileButtonsInnerPanel == null) {
	    mFileButtonsInnerPanel = new Panel(new GridLayout(1,5));
	    mFileButtonsInnerPanel.add(getChgDirButton());
	    mFileButtonsInnerPanel.add(getMkDirButton());
	    mFileButtonsInnerPanel.add(getRenameButton());
	    mFileButtonsInnerPanel.add(getDeleteButton());
	    mFileButtonsInnerPanel.add(getRefreshButton());
	}
	return mFileButtonsInnerPanel;
    }


    //----- Buttons ----- 
    private Button getChgDirButton() {
	if (mChgDirButton == null) {
	    mChgDirButton = new Button("ChgDir");
	    mChgDirButton.addActionListener(new Actions(BUT_CHDIR));
	}
	return mChgDirButton;
    }

    private Button getMkDirButton() {
	if (mMkDirButton == null) {
	    mMkDirButton = new Button("MkDir");
	    mMkDirButton.addActionListener(new Actions(BUT_MKDIR));
	}
	return mMkDirButton;
    }

    private Button getRenameButton() {
	if (mRenameButton == null) {
	    mRenameButton = new Button("Rename");
	    mRenameButton.addActionListener(new Actions(BUT_RENAME));
	}
	return mRenameButton;
    }

    private Button getDeleteButton() {
	if (mDeleteButton == null) {
	    mDeleteButton = new Button("Delete");
	    mDeleteButton.addActionListener(new Actions(BUT_DELETE));
	}
	return mDeleteButton;
    }

    private Button getRefreshButton() {
	if (mRefreshButton == null) {
	    mRefreshButton = new Button( "Refresh" );
	    mRefreshButton.addActionListener(new Actions(BUT_REFRESH));
	}
	return mRefreshButton;
    }

    /** The list of files */
    private FileList getFileList() {
	if (mFileList == null) {
	    mFileList = new FileList();
	    mFileList.setFont( new Font( "Monospaced", Font.PLAIN, 12 ) );
	    mFileList.setMultipleMode( true );
	    mFileList.addActionListener(new Actions(ACT_DBLCLICK));
			
	    mFileList.addItemListener(new Actions(-1));
			
	    mFileList.setBackground(Color.white);
	}
	return mFileList;
    }

    private void enableButtons() {
	mRenameButton.setEnabled( mFileList.getSelectionCount() == 1 );
	mDeleteButton.setEnabled( mFileList.getSelectionCount() > 0 );
    }

    //----- public methods that make the file system label a property -----

    public String getFileSystemLocationLabelText() {
	return getFileSystemLocationLabel().getText();
    }

    public void setFileSystemLocationLabelText(String arg1) {
	getFileSystemLocationLabel().setText(arg1);
    }

    public void setFileList(FileListItem[] files, String directory ) {
	setFileSystemLocationLabelText(directory);

	mFileList.setListItems( files );
	enableButtons();
    }
	
    public void setFileBrowser( FileBrowser browser ) {
	mBrowser = browser;
    }
	
    public FileListItem getSelectedFile() {
	return mFileList.getSelectedFileListItem();
    }
	
    public FileListItem[] getSelectedFiles() {
	return mFileList.getSelectedFileListItems();
    }

}
