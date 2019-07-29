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

import com.mindbright.gui.AWTConvenience;
import com.mindbright.gui.AWTGridBagContainer;

public final class SSHTunnelDialog {

    protected final static int ACT_LIST_CLICK = 0;
    protected final static int ACT_ADD        = 1;
    protected final static int ACT_DEL        = 2;

    private static class Actions implements ActionListener, ItemListener {
	private int action;

	public Actions(int action) {
	    this.action = action;
	}

	public void actionPerformed(ActionEvent e) {
	    switch(action) {
	    case ACT_LIST_CLICK: {
		int i = tunnelList.getSelectedIndex();
		if(i != -1) {
		    SSHClient.LocalForward fwd = (SSHClient.LocalForward) client.localForwards.elementAt(i);
		    localPort.setText(String.valueOf(fwd.localPort));
		    remotePort.setText(String.valueOf(fwd.remotePort));
		    remoteHost.setText(fwd.remoteHost);
		    for(i = 1; i < servs.length; i++) {
			if(fwd.remotePort == servs[i]) {
			    protoChoice.select(protos[i]);
			    break;
			}
		    }
		    if(i == servs.length)
			protoChoice.select("general");
		}
		break;
	    }
	    case ACT_ADD: {
		String rh = remoteHost.getText();
		String plug = "general";
		int    lp = -1, rp = -1;
		try {
		    lp = Integer.valueOf(localPort.getText()).intValue();
		    rp = Integer.valueOf(remotePort.getText()).intValue();
		    if(lp < 1 || lp > 65535) {
			lp = -1;
			throw new NumberFormatException();
		    }
		    if(rp < 1 || rp > 65535) {
			rp = -1;
			throw new NumberFormatException();
		    }
		} catch (NumberFormatException ee) {
		    if(lp == -1) {
			localPort.setText("");
			localPort.requestFocus();
		    } else {
			remotePort.setText("");
			remotePort.requestFocus();
		    }
		    return;
		}
		if(protoChoice.getSelectedItem().equals("ftp"))
		    plug = "ftp";
		try {
		    propsHandler.setProperty("local" + client.localForwards.size(),
					     "/" + plug + "/" + lp + ":" + rh + ":" +  rp);
		    if(client.isOpened())
			SSHMiscDialogs.alert("Tunnel Notice",
					     "Tunnel is now open and operational",
					     parent);
		} catch (Throwable ee) {
		    SSHMiscDialogs.alert("Tunnel Notice",
					 "Could not open tunnel: " +
					 ee.getMessage(), parent);
		}
		updateTunnelList();
		break;
	    }
	    case ACT_DEL: {
		int i = tunnelList.getSelectedIndex();
		if(i != -1) {
		    propsHandler.removeLocalTunnelAt(i, true);
		}
		updateTunnelList();
		break;
	    }
	    }
	}

	public void itemStateChanged(ItemEvent e) {
	    String it = (String)e.getItem();
	    int i;
	    for(i = 0; i < protos.length; i++)
		if(it.equals(protos[i]))
		    break;
	    if(i > 0) {
		remotePort.setText(String.valueOf(servs[i]));
	    }
	}

    }

    private static Dialog    basicTunnelsDialog = null;
    private static List      tunnelList;
    private static TextField remoteHost, remotePort, localPort;
    private static Choice    protoChoice;
    private final static String[] protos = { "general", "ftp", "telnet", "smtp", "http", "pop2", "pop3", "nntp", "imap" };
    final static int[]    servs  = {  0, 21, 23, 25, 80, 109, 110, 119, 143 };

    private static SSHPropertyHandler propsHandler;
    private static Frame              parent;
    private static SSHClient          client;

    public static void show(String title, SSHClient cli,
			    SSHPropertyHandler props, Frame p) {
	propsHandler = props;
	parent       = p;
	client       = cli;
	if(basicTunnelsDialog == null) {
	    basicTunnelsDialog = new Dialog(parent, title, true);

	    Label               lbl;
	    Button              b;
	    ActionListener      al;
	    AWTGridBagContainer grid =
		new AWTGridBagContainer(basicTunnelsDialog);

	    grid.getConstraints().fill = GridBagConstraints.NONE;

	    lbl = new Label("Current local tunnels:");
	    grid.add(lbl, 0, 2);

	    grid.getConstraints().fill    = GridBagConstraints.BOTH;
	    grid.getConstraints().insets  = new Insets(4, 4, 4, 4);
	    grid.getConstraints().weightx = 1.0;
	    grid.getConstraints().weighty = 1.0;

	    tunnelList = new List(8);
	    grid.add(tunnelList, 1, 4);

	    tunnelList.addActionListener(new Actions(ACT_LIST_CLICK));

	    grid.getConstraints().fill    = GridBagConstraints.NONE;
	    grid.getConstraints().weighty = 0;

	    lbl = new Label("Local port:");
	    grid.add(lbl, 2, 1);

	    grid.getConstraints().fill = GridBagConstraints.HORIZONTAL;
	    localPort = new TextField("", 5);
	    grid.add(localPort, 2, 1);

	    grid.getConstraints().fill = GridBagConstraints.NONE;

	    lbl = new Label("Protocol:");
	    grid.add(lbl, 2, 1);

	    protoChoice = AWTConvenience.newChoice(protos);
	    protoChoice.select("general");
	    grid.add(protoChoice, 2, 1);

	    protoChoice.addItemListener(new Actions(-1));

	    lbl = new Label("Remote host:");
	    grid.add(lbl, 3, 1);

	    grid.getConstraints().fill = GridBagConstraints.HORIZONTAL;

	    remoteHost = new TextField("", 16);
	    grid.add(remoteHost, 3, 3);

	    grid.getConstraints().fill = GridBagConstraints.NONE;

	    lbl = new Label("Remote port:");
	    grid.add(lbl, 4, 1);

	    grid.getConstraints().fill    = GridBagConstraints.HORIZONTAL;
	    grid.getConstraints().weightx = 0.9;

	    remotePort = new TextField("", 5);
	    grid.add(remotePort, 4, 1);

	    b = new Button("Add");
	    b.addActionListener(new Actions(ACT_ADD));

	    grid.getConstraints().weightx = 0.1;

	    grid.add(b, 4, 1);

	    b = new Button("Delete");
	    b.addActionListener(new Actions(ACT_DEL));

	    grid.add(b, 4, 1);
      
	    b = new Button("Close Dialog");
	    b.addActionListener(new AWTConvenience.CloseAction(basicTunnelsDialog));

	    grid.getConstraints().fill   = GridBagConstraints.NONE;
	    grid.getConstraints().anchor = GridBagConstraints.CENTER;
	    grid.getConstraints().ipady  = 2;
	    grid.getConstraints().ipadx  = 2;

	    grid.add(b, 5, GridBagConstraints.REMAINDER);

	    basicTunnelsDialog.addWindowListener(new AWTConvenience.CloseAdapter(b));

	    AWTConvenience.setBackgroundOfChildren(basicTunnelsDialog);

	    basicTunnelsDialog.setResizable(true);
	    basicTunnelsDialog.pack();
	}
	updateTunnelList();

	basicTunnelsDialog.setTitle(title);

	AWTConvenience.placeDialog(basicTunnelsDialog);
	localPort.requestFocus();
	basicTunnelsDialog.setVisible(true);
    }

    private static void updateTunnelList() {
	tunnelList.removeAll();
	for(int i = 0; i < client.localForwards.size(); i++) {
	    SSHClient.LocalForward fwd = (SSHClient.LocalForward) client.localForwards.elementAt(i);
	    String plugStr = (fwd.plugin.equals("general") ? "" : " (plugin: " + fwd.plugin + ")");
	    tunnelList.add("local: " + fwd.localPort + " -> remote: " + fwd.remoteHost + "/" +
			   fwd.remotePort + plugStr);
	}
    }

}
