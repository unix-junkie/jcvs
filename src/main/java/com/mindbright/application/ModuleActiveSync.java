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

package com.mindbright.application;

import java.io.IOException;
import java.net.Socket;
import java.util.Properties;

import java.awt.Dialog;
import java.awt.TextField;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Frame;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Button;
import java.awt.Insets;
import java.awt.event.ActionListener;

import com.mindbright.ssh2.SSH2Listener;
import com.mindbright.ssh2.SSH2ConnectionEventAdapter;

public class ModuleActiveSync implements MindTermLite.Module {

    protected String remoteAddr;
    protected String localAddr;
    protected String bindAddr;

    protected static SSH2Listener listener999;
    protected static boolean      started999;

    public void init(MindTermLite mindterm) {
	bindAddr   = mindterm.getProperty("as_bind");
	localAddr  = mindterm.getProperty("as_local");
	remoteAddr = mindterm.getProperty("as_remote");
    }

    public void activate(final MindTermLite mindterm) {
    }

    public String getLabel() {
	return null;
    }

    public boolean isAvailable(MindTermLite mindterm) {
	return false;
    }

    public void connected(MindTermLite mindterm) {
	started999 = false;
	if(listener999 != null) {
	    listener999.stop();
	}
	mindterm.getClient().getConnection().
	    setEventHandler(new SSH2ConnectionEventAdapter() {
		    public void listenerConnect(final SSH2Listener listener, Socket fwdSocket) {
			if(listener.getRemotePort() == 5678 && !started999) {
			    (new Thread(new Runnable() {
				    public void run() {
					try {
					    Thread.sleep(3000);
					    listener999 = 
						listener.getConnection().newLocalForward(bindAddr,
											 999,
											 remoteAddr,
											 999);
					} catch (Exception e) {
					    // !!!
					    System.out.println("ERROR when setting up 999: " + e);
					}
				    }
				})).start();
			    started999 = true;
			}
		    }
		});

	try {
	    SSH2Listener l;
	    l = mindterm.getClient().getConnection().
		newLocalForward(bindAddr, 5678, remoteAddr, 5678);
	    l = mindterm.getClient().getConnection().
		newLocalForward(bindAddr, 5679, remoteAddr, 5679);
	    mindterm.getClient().getConnection().
		newRemoteForward("0.0.0.0", 990, localAddr, 990);
	} catch (Exception e) {
	    // !!!
	    System.out.println("ERROR: " + e);
	    mindterm.alert("Error: " + e);
	}

    }

}
