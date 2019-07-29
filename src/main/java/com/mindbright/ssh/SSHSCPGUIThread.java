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
import java.io.File;
import java.io.OutputStream;
import java.io.IOException;

import com.mindbright.gui.ProgressBar;
import com.mindbright.gui.AWTConvenience;
import com.mindbright.gui.AWTGridBagContainer;

import com.mindbright.sshcommon.SSHFileTransfer;
import com.mindbright.sshcommon.SSHFileTransferProgress;

import com.mindbright.ssh2.SSH2SCP1Client;
import com.mindbright.ssh2.SSH2SFTPTransfer;

public final class SSHSCPGUIThread extends Thread
    implements SSHFileTransferProgress
{
    String           curDir, localFile, remoteFile;
    String           remoteHost;
    int              remotePort;
    boolean          recursive, background, toRemote;
    Frame            parent;
    SSHSCPDialog     browseDialog;

    SSHInteractiveClient client;

    String[]    localFileList;
    String[]    remoteFileList;

    Dialog          copyIndicator;
    ProgressBar     progress;
    SSHFileTransfer fileXfer;
    Thread          copyThread;
    Label           srcLbl, dstLbl, sizeLbl, nameLbl, speedLbl;
    Button          cancB;
    long            startTime;
    long            lastTime;
    long            totTransSize;
    long            fileTransSize;
    long            curFileSize;
    long            lastSize;
    int             fileCnt;
    boolean         doneCopying;

    public SSHSCPGUIThread(SSHInteractiveClient client,
			   Frame parent,
			   String curDir,
			   String[] localFileList, String[] remoteFileList,
			   boolean recursive, boolean background,
			   boolean toRemote, SSHSCPDialog browseDialog)
	throws Exception
    {
	try {
	    netscape.security.PrivilegeManager.enablePrivilege("UniversalFileAccess");
	} catch (netscape.security.ForbiddenTargetException e) {
	    // !!!
	}
    
	this.localFileList  = localFileList;
	this.remoteFileList = remoteFileList;

	if(!toRemote) {
	    if(localFileList.length > 1) {
		throw new Exception("Ambiguos local target");
	    }
	} else {
	    if(remoteFileList.length > 1) {
		throw new Exception("Ambiguos remote target");
	    }
	}
	localFile  = localFileList[0];
	remoteFile = remoteFileList[0];

	this.client        = client;
	this.remoteHost    = client.propsHandler.getSrvHost();
	this.remotePort    = client.propsHandler.getSrvPort();
	this.curDir        = curDir;
	this.parent        = parent;
	this.localFile     = localFile;
	this.remoteFile    = remoteFile;
	this.recursive     = recursive;
	this.background    = background;
	this.toRemote      = toRemote;
	this.fileCnt       = 0;
	this.doneCopying   = false;
	this.startTime     = 0;
	this.lastTime      = 0;
	this.totTransSize  = 0;
	this.fileTransSize = 0;
	this.lastSize      = 0;
	this.browseDialog  = browseDialog;
	this.start();
    }

    public void run() {
	String sourceFile = "localhost:" + unQuote(localFile);
	String destFile   = remoteHost + ":" + unQuote(remoteFile);

	if(!toRemote) {
	    String tmp;
	    tmp        = sourceFile;
	    sourceFile = destFile;
	    destFile   = tmp;
	}

	copyIndicator = new Dialog(parent, "MindTerm - File Transfer", false);
	      
	Label               lbl;
	Button              b;
	AWTGridBagContainer grid  = new AWTGridBagContainer(copyIndicator);

	lbl = new Label("Source:");
	grid.add(lbl, 0, 1);
	  
	srcLbl = new Label(cutName(sourceFile, 32));
	grid.add(srcLbl, 0, 4);

	lbl = new Label("Destination:");
	grid.add(lbl, 1, 1);
	  
	dstLbl = new Label(cutName(destFile, 32));
	grid.add(dstLbl, 1, 4);

	lbl= new Label("Current:");
	grid.add(lbl, 2, 1);

	nameLbl= new Label("connecting...");
	grid.add(nameLbl, 2, 3);

	sizeLbl= new Label("");
	grid.add(sizeLbl, 2, 1);

	grid.getConstraints().fill   = GridBagConstraints.NONE;
	grid.getConstraints().anchor = GridBagConstraints.CENTER;
	grid.getConstraints().insets = new Insets(4, 12, 4, 4);

	progress = new ProgressBar(512, 160, 20);
	grid.add(progress, 3, 3);

	grid.getConstraints().fill   = GridBagConstraints.HORIZONTAL;
	grid.getConstraints().insets = new Insets(4, 4, 4, 4);

	speedLbl = new Label("0.0 kB/sec", Label.CENTER);
	grid.add(speedLbl, 3, GridBagConstraints.REMAINDER);

	cancB = new Button("Cancel");
	cancB.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		if(!doneCopying) {
		    if(copyThread != null)
			copyThread.stop();
		    if(fileXfer != null)
			fileXfer.abort();
		}
		copyIndicator.setVisible(false);
	    }
	});

	grid.getConstraints().fill  = GridBagConstraints.NONE;
	grid.getConstraints().ipadx = 2;
	grid.getConstraints().ipady = 2;

	grid.add(cancB, 4, GridBagConstraints.REMAINDER);

	AWTConvenience.setBackgroundOfChildren(copyIndicator);

	Dimension d = speedLbl.getSize();
	d.width += d.width * 2;
	speedLbl.setSize(d);
	sizeLbl.setSize(d);

	copyIndicator.setResizable(true);
	copyIndicator.pack();
	AWTConvenience.placeDialog(copyIndicator);

	copyThread = new Thread(new Runnable() {
	    public void run() {
		try {
		    netscape.security.PrivilegeManager.enablePrivilege("UniversalFileAccess");
		    netscape.security.PrivilegeManager.enablePrivilege("TerminalEmulator");
		} catch (netscape.security.ForbiddenTargetException e) {
		    // !!!
		}

		try {
		    SSHInteractor interactAdapter = new SSHInteractorAdapter() {
			    public void open(SSHClient client) {
				nameLbl.setText("...connected");
			    }
			    public void disconnected(SSHClient client,
						     boolean graceful) {
				fileXfer.abort();
			    }
			    public void alert(String msg) {
				client.alert(msg);
			    }
			};

		    if(browseDialog.isSFTP) {
			fileXfer = new SSH2SFTPTransfer(new File(curDir),
							client.connection);
		    } else {
			if(client.isSSH2) {
			    OutputStream alertOutput = new OutputStream() {
				    public void write(int bb) throws IOException {
					byte[] buf = new byte[] { (byte)bb };
					write(buf);
				    }
				    public void write(byte bb[], int off, int len)
					throws IOException {
					client.alert("Remote warning/error: " +
						     new String(bb, off, len));
				    }
				};
			    SSH2SCP1Client scpClient2 =
				new SSH2SCP1Client(new File(curDir),
						   client.connection,
						   alertOutput,
						   SSH.DEBUG);
			    fileXfer = scpClient2.scp1();
			} else {
			    SSHSCPClient scpClient =
				new SSHSCPClient(remoteHost, remotePort,
						 client.propsHandler,
						 interactAdapter,
						 new File(curDir),
						 SSH.DEBUG);
			    scpClient.setClientUser(client.propsHandler);
			    fileXfer = scpClient.scp1();
			}
		    }

		    fileXfer.setProgress(SSHSCPGUIThread.this);
		    if(toRemote) {
			fileXfer.copyToRemote(localFileList, remoteFileList[0],
					      recursive);
		    } else {
			fileXfer.copyToLocal(localFileList[0], remoteFileList,
					     recursive);
		    }

		    copyThread.setPriority(Thread.NORM_PRIORITY);
		    Toolkit.getDefaultToolkit().beep();
		} catch (Exception e) {
		    client.alert("SCP Error: " + e.getMessage());
		    if(SSH.DEBUGMORE) {
			System.out.println("SCP Error:");
			e.printStackTrace();
		    }
		}
		nameLbl.setText("Copied " + fileCnt + " file" + (fileCnt != 1 ? "s" : "") + ".");
		double kSize = (double)totTransSize / 1024;
		sizeLbl.setText(round(kSize) + " kB");
		doneCopying = true;
		cancB.setLabel("Done");

		browseDialog.refresh();

		AWTConvenience.setKeyListenerOfChildren(copyIndicator,
			   new AWTConvenience.OKCancelAdapter(cancB, cancB),
							null);

	    }
	});

	if(background) {
	    copyThread.setPriority(Thread.MIN_PRIORITY);
	}

	copyThread.start();

	copyIndicator.setVisible(true);
    }

    static int addUnique(String[] list, String str, int last) {
	int i;
	for(i = 0; i < last; i++)
	    if(list[i].equals(str))
		break;
	if(i == last)
	    list[last++] = str;
	return last;
    }

    public void startFile(String file, long size) {
	double kSize = (double)size / 1024;
	sizeLbl.setText(round(kSize) + " kB");
	nameLbl.setText(unQuote(file));
	progress.setMax(size, true);
	lastTime = System.currentTimeMillis();
	if(startTime == 0)
	    startTime = lastTime;
	curFileSize   = size;
	fileTransSize = 0;
	fileCnt++;
    }
    public void startDir(String file) {
	if(startTime == 0)
	    startTime = System.currentTimeMillis();
	if(file.length() > curDir.length())
	    file = file.substring(curDir.length());
	if(toRemote) {
	    srcLbl.setText(cutName("localhost:" + unQuote(file), 32));
	} else {
	    dstLbl.setText(cutName("localhost:" + unQuote(file), 32));
	}
    }

    public void endFile() {
	progress.setValue(curFileSize, true);
    }

    public void endDir() {
    }

    public void progress(int size) {
	totTransSize  += (long)size;
	fileTransSize += (long)size;
	if((curFileSize > 0) &&
	   ((((totTransSize - lastSize) * 100) / curFileSize) >= 1)) {
	    progress.setValue(fileTransSize, !background);
	    long   now      = System.currentTimeMillis();
	    long   elapsed  = (now - startTime);
	    if(elapsed == 0) {
		elapsed = 1;
	    }
	    int curSpeed = (int)((double)totTransSize /
				 ((double)elapsed / 1000));
	    elapsed = (now - lastTime);
	    if(elapsed == 0) {
		elapsed = 1;
	    }
	    curSpeed += (int)((double)(totTransSize - lastSize) /
			      ((double)elapsed / 1000));
	    curSpeed >>>= 1;
	    curSpeed >>>= 10;
	    speedLbl.setText(curSpeed + " kB/sec");
	    lastSize = totTransSize;
	    lastTime = now;
	}
    }

    double round(double val) {
	val = val * 10.0;
	val = Math.floor(val);
	val = val / 10.0;
	return val;
    }

    String cutName(String name, int len) {
	if(name.length() > len) {
	    len -= 3;
	    String pre = name.substring(0, len / 2);
	    String suf = name.substring(name.length() - (len / 2));
	    name = pre + "..." + suf;
	}
	return name;
    }

    String unQuote(String str) {
	if(str.charAt(0) == '"') {
	    str = str.substring(1, str.length() - 1);
	}
	return str;
    }

}
