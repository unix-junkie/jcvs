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

package com.mindbright.ssh;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import com.isnetworks.ssh.*;

import com.mindbright.sshcommon.SSHConsoleRemote;

import com.mindbright.ssh2.SSH2ConsoleRemote;
import com.mindbright.ssh2.SSH2SFTPFileBrowser;

public class SSHSCPDialog extends Dialog {

    private class Actions implements ActionListener {

	private boolean toRemote;

	public Actions(boolean toRemote) {
	    this.toRemote = toRemote;
	}

	public void actionPerformed(ActionEvent e) {
	    try {
		FileListItem[] selectedItems;
		String         localCWD;
		String         remoteCWD;
		String         fromDir;
		String         toDir;

		localCWD =
		    getLocalFileDisplay().getFileSystemLocationLabelText();
		remoteCWD =
		    getRemoteFileDisplay().getFileSystemLocationLabelText();

		if(toRemote) {
		    selectedItems = mLocalFileDisplay.getSelectedFiles();
		    fromDir = localCWD;
		    toDir   = remoteCWD;
		} else {
		    selectedItems = mRemoteFileDisplay.getSelectedFiles();
		    fromDir = remoteCWD;
		    toDir   = localCWD;
		}

		String[] files = new String[selectedItems.length];

		if(selectedItems.length == 0) {
		    throw new Exception("Please select file(s) to transfer");
		}

		for(int i = 0; i < selectedItems.length; i++ ) {
		    files[i] = fromDir + selectedItems[i].getName();
		}

		String[] files2 = new String[] {
		    toDir
		};

		if(!toRemote) {
		    String[] tmp = files2;
		    files2  = files;
		    files = tmp;
		}

		new SSHSCPGUIThread(client, mOwnerFrame, localCWD,
				    files, files2, true, false, toRemote,
				    SSHSCPDialog.this);
	    } catch (Exception ee) {
		logError(ee);
	    }
	}

    }

    public boolean isSFTP;

    private Button        mUploadButton;
    private Button        mDownloadButton;

    private Panel         mFileDisplayPanel;
    private Panel         mMainBottomSectionPanel;

    /** Text area for error message display */
    private TextArea      mMessageTextArea;

    /** GUI for browsing file systems */
    private FileDisplay   mLocalFileDisplay;
    private FileDisplay   mRemoteFileDisplay;

    /** Back end for browsing file systems */
    private FileBrowser mRemoteFileBrowser;
    private FileBrowser mLocalFileBrowser;

    /** Frame to attach new dialog boxes to */
    private Frame mOwnerFrame;

    private SSHInteractiveClient client;

    /** Constructor 
     *  Overall, the GUI is composed of two parts:
     *    the bottom section (buttons, messages)
     *    the file-display section (two file displays & arrow buttons)
     */
    private SSHSCPDialog(String title, Frame owner, SSHInteractiveClient client)
    {
	super(owner, title, false);
	
	mOwnerFrame = owner;
	
	this.client = client;

	setLayout(new BorderLayout());
	setBackground(Color.lightGray);
	add("South",  getMainBottomSectionPanel());
	add("Center", getFileDisplayPanel());
	pack();
	
	addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
		    setVisible(false);
		    mRemoteFileBrowser.disconnect();			
		}});
    }

    public void setLocalFileBrowser(FileBrowser localBrowser) {
	this.mLocalFileBrowser = localBrowser;
    }

    public void setRemoteFileBrowser(FileBrowser remoteBrowser) {
	this.mRemoteFileBrowser = remoteBrowser;
    }

    /**
     * The main bottom part of the GUI.  Now just contains the error text area
     */
    private Panel getMainBottomSectionPanel() {
	if (mMainBottomSectionPanel == null) {
	    mMainBottomSectionPanel = new Panel(new BorderLayout());
	    getMainBottomSectionPanel().add("Center", getMessageTextArea());
	}
	return mMainBottomSectionPanel;
    }


    /** This is the message text area in the bottom part of the GUI.
     *  It is sized to 3 rows, 30 columns, which drives its preferred
     *  size (the preferred height is respected due to its position in
     *  the GUI.
     */
    private TextArea getMessageTextArea() {
	if (mMessageTextArea == null) {
	    mMessageTextArea = new TextArea( "", 3, 30, TextArea.SCROLLBARS_VERTICAL_ONLY );
	    mMessageTextArea.setEditable( false );
	    mMessageTextArea.setFont( new Font( "Monospaced", Font.PLAIN, 11 ) );
	    mMessageTextArea.setBackground(Color.white);
	}
	return mMessageTextArea;
    }


    /** This is the upper section of the GUI, containing the
     *  local & remote file displays and the direction buttons
     *  It is a big-bad-evil GridBagLayout (tm)
     *  The general idea is that the file displays expand
     *  horizontally to fill the remaining space equally and
     *  the arrow buttons float in the center between the
     *  two file displays.
     */
    private Panel getFileDisplayPanel() {
	if (mFileDisplayPanel == null) {
	    mFileDisplayPanel = new Panel(new GridBagLayout());

	    GridBagConstraints gbc = new GridBagConstraints();
 
	    gbc.gridx      = 0;
	    gbc.gridy      = 0;
	    gbc.gridwidth  = 1;
	    gbc.gridheight = 2;
	    gbc.fill       = GridBagConstraints.BOTH;
	    gbc.anchor     = GridBagConstraints.CENTER;
	    gbc.weightx    = 0.5;
	    gbc.weighty    = 1.0;
	    mFileDisplayPanel.add(getLocalFileDisplay(), gbc);

	    gbc.gridx      = 2;
	    gbc.gridy      = 0;
	    gbc.gridwidth  = 1;
	    gbc.gridheight = 2;
	    gbc.fill       = GridBagConstraints.BOTH;
	    gbc.anchor     = GridBagConstraints.CENTER;
	    gbc.weightx    = 0.5;
	    gbc.weighty    = 1.0;
	    mFileDisplayPanel.add(getRemoteFileDisplay(), gbc);

	    gbc.gridx      = 1;
	    gbc.gridy      = 0;
	    gbc.gridwidth  = 1;
	    gbc.gridheight = 1;
	    gbc.fill       = GridBagConstraints.NONE;
	    gbc.anchor     = GridBagConstraints.SOUTH;
	    gbc.weightx    = 0.0;
	    gbc.weighty    = 0.5;
	    gbc.insets     = new Insets(0, 4, 2, 4);
	    mFileDisplayPanel.add(getDownloadButton(), gbc);

	    gbc.gridx      = 1;
	    gbc.gridy      = 1;
	    gbc.gridwidth  = 1;
	    gbc.gridheight = 1;
	    gbc.fill       = GridBagConstraints.NONE;
	    gbc.anchor     = GridBagConstraints.NORTH;
	    gbc.weightx    = 0.0;
	    gbc.weighty    = 0.5;
	    gbc.insets     = new Insets(2, 4, 0, 4);
	    mFileDisplayPanel.add(getUploadButton(), gbc);
	}
	return mFileDisplayPanel;
    }

    /** An instance of FileDisplay for the local system */
    private FileDisplay getLocalFileDisplay() {
	if(mLocalFileDisplay == null) {
	    mLocalFileDisplay = new FileDisplay(mOwnerFrame, "Local System",
						this);
	}
	return mLocalFileDisplay;
    }

    /** An instance of FileDisplay for the remote system */
    private FileDisplay getRemoteFileDisplay() {
	if(mRemoteFileDisplay == null) {
	    mRemoteFileDisplay = new FileDisplay(mOwnerFrame, "Remote System",
						 this);
	}
	return mRemoteFileDisplay;
    }

    /** A direction button pointing left */
    private Button getDownloadButton() {
	if (mDownloadButton == null) {
	    mDownloadButton = new Button("<--");
	    mDownloadButton.addActionListener(new Actions(false));
	}
	return mDownloadButton;
    }

    /** A direction button pointing right */
    private Button getUploadButton() {
	if (mUploadButton == null) {
	    mUploadButton = new Button("-->");
	    mUploadButton.addActionListener(new Actions(true));
	}
	return mUploadButton;
    }

    public void refresh() {
	try {
	    netscape.security.PrivilegeManager.enablePrivilege("UniversalFileAccess");
	} catch (netscape.security.ForbiddenTargetException e) {
	    // !!!
	}
	try {
	    mRemoteFileBrowser.refresh();
	    mLocalFileBrowser.refresh();
	} catch( SSHException e ) {
	    logError(e);
	}
    }

    /**
     * Initialize the connection to the remote system and
     * start in the SSH home directory on the local system
     */
    public void show() {
	try {
	    netscape.security.PrivilegeManager.enablePrivilege("UniversalFileAccess");
	    netscape.security.PrivilegeManager.enablePrivilege("TerminalEmulator");
	} catch (netscape.security.ForbiddenTargetException e) {
	    // !!!
	}
	setSize(600, 500);
	try {
	    mRemoteFileBrowser.initialize();
	    mLocalFileBrowser.initialize();
	} catch(SSHException e) {
	    logError(e);
	}
	super.show();
    }

    /**
     * An exception happened, so show the user the message in the text area
     */
    public void logError( Exception e ) {
	mMessageTextArea.append( e.getMessage() + "\n" );
    }

    public static void showSCP(String title, Frame p,
			       SSHInteractiveClient client)
    {
	SSHSCPDialog dialog = new SSHSCPDialog(title, p, client);

	SSHConsoleRemote remote = null;

	if(client.isSSH2) {
	    remote = new SSH2ConsoleRemote(client.connection, null);
	} else {
	    try {
		remote =
		    new SSHConsoleClient(client.propsHandler.getSrvHost(),
					 client.propsHandler.getSrvPort(),
					 client.propsHandler, null);
		((SSHConsoleClient)remote).setClientUser(client.propsHandler);
	    } catch (IOException e) {
		client.alert("Error creating scp dialog: " + e.getMessage());
	    }
	}

	dialog.setLocalFileBrowser(new
	    LocalFileBrowser(dialog.getLocalFileDisplay(),
			     client.getPropertyHandler()));

	dialog.setRemoteFileBrowser(new
	    SSHRemoteFileBrowsingConsole(dialog.getRemoteFileDisplay(),
					 client.getPropertyHandler(),
					 dialog,
					 remote));

	dialog.show();
    }

    public static void showSFTP(String title, Frame p,
				SSHInteractiveClient client)
    {
	if(!client.isSSH2) {
	    client.alert("SFTP can only be used with ssh2 currently");
	    return;
	}

	SSHSCPDialog dialog = new SSHSCPDialog(title, p, client);

	dialog.isSFTP = true;

	dialog.setLocalFileBrowser(new
	    LocalFileBrowser(dialog.getLocalFileDisplay(),
			     client.getPropertyHandler()));

	dialog.setRemoteFileBrowser(new
	    SSH2SFTPFileBrowser(client.connection,
				dialog.getRemoteFileDisplay()));

	dialog.show();
    }

}
