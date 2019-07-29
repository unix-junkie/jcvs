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

public final class SSHProxyDialog {
    private static Dialog     proxyDialog = null;
    private static Choice     choicePrxType;
    private static Checkbox   cbNeedAuth;
    private static TextField  textPrxHost, textPrxPort, textPrxUser, textPrxPasswd;
    private static String[]   prxTypes;

    private static SSHPropertyHandler propsHandler;

    public static void show(String title, Frame parent,
			    SSHPropertyHandler props) {
	propsHandler = props;
	if(proxyDialog == null) {
	    prxTypes = SSH.getProxyTypes();

	    proxyDialog = new Dialog(parent, title, true);

	    Label              lbl;
	    Button             b;
	    ItemListener       il;

	    AWTGridBagContainer grid = new AWTGridBagContainer(proxyDialog);

	    grid.getConstraints().fill   = GridBagConstraints.HORIZONTAL;
	    grid.getConstraints().anchor = GridBagConstraints.WEST;
	    grid.getConstraints().insets = new Insets(4, 4, 0, 4);

	    lbl = new Label("Proxy type:");
	    grid.add(lbl, 0, 2);

	    choicePrxType = AWTConvenience.newChoice(prxTypes);
	    choicePrxType.addItemListener(il = new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
		    if(e.getSource() == choicePrxType) {
			textPrxPort.setText(String.valueOf(SSH.defaultProxyPorts[SSH.getProxyType(choicePrxType.getSelectedItem())]));
		    }
		    updateFromType();
		}
	    });
	    grid.add(choicePrxType, 0, 2);

	    lbl = new Label("Server:");
	    grid.add(lbl, 1, 2);

	    textPrxHost = new TextField("", 16);
	    grid.add(textPrxHost, 1, 4);

	    lbl = new Label("Port:");
	    grid.add(lbl, 1, 1);

	    textPrxPort = new TextField("", 4);
	    grid.add(textPrxPort, 1, 1);

	    lbl = new Label("Username:");
	    grid.add(lbl, 2, 2);

	    textPrxUser = new TextField("", 10);
	    grid.add(textPrxUser, 2, 2);

	    lbl = new Label("Password:");
	    grid.add(lbl, 2, 2);

	    textPrxPasswd = new TextField("", 10);
	    textPrxPasswd.setEchoChar('*');
	    grid.add(textPrxPasswd, 2, 2);

	    cbNeedAuth = new Checkbox("Need authentication");
	    grid.add(cbNeedAuth, 3, 4);
	    cbNeedAuth.addItemListener(il);

	    Panel bp = new Panel(new FlowLayout());
	    bp.add(b = new Button("OK"));
	    b.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    try {
			String prxTypeStr = choicePrxType.getSelectedItem();
			propsHandler.setProperty("proxytype", prxTypeStr);
			if(!"none".equalsIgnoreCase(prxTypeStr)) {
			    propsHandler.setProperty("proxyhost", textPrxHost.getText());
			    propsHandler.setProperty("proxyport", textPrxPort.getText());
			}
			if(cbNeedAuth.getState()) {
			    propsHandler.setProperty("proxyuser", textPrxUser.getText());
			    propsHandler.setProperty("prxpassword", textPrxPasswd.getText());
			} else if("socks4".equals(prxTypeStr)) {
			    propsHandler.setProperty("proxyuser", textPrxUser.getText());
			}
			proxyDialog.setVisible(false);
		    } catch (Exception ee) {
			// !!!
		    }
		}
	    });
	    bp.add(b = new Button("Cancel"));
	    b.addActionListener(new AWTConvenience.CloseAction(proxyDialog));

	    grid.getConstraints().anchor = GridBagConstraints.CENTER;
	    grid.getConstraints().fill   = GridBagConstraints.NONE;

	    grid.add(bp, 4, GridBagConstraints.REMAINDER);

	    proxyDialog.addWindowListener(new AWTConvenience.CloseAdapter(b));

	    AWTConvenience.setBackgroundOfChildren(proxyDialog);

	    proxyDialog.setResizable(true);
	    proxyDialog.pack();
	}

	proxyDialog.setTitle(title);

	String prxType = propsHandler.getProperty("proxytype");
	choicePrxType.select(prxType);
	String prxUser = propsHandler.getProperty("proxyuser");
	boolean needAuth = (prxUser != null && (prxUser.trim().length() > 0));
	cbNeedAuth.setState(needAuth);
	textPrxHost.setText(propsHandler.getProperty("proxyhost"));
	textPrxPort.setText(propsHandler.getProperty("proxyport"));
	textPrxUser.setText(propsHandler.getProperty("proxyuser"));

	updateFromType();

	AWTConvenience.placeDialog(proxyDialog);

	proxyDialog.setVisible(true);
    }

    private static void updateFromType() {
	boolean proxyEnable = false;
	boolean authEnable  = false;
	String  proxyType   = choicePrxType.getSelectedItem();
	int     type        = 0;

	try {
	    type = SSH.getProxyType(proxyType);
	    switch(type) {
	    case SSH.PROXY_NONE:
		break;
	    case SSH.PROXY_HTTP:
	    case SSH.PROXY_SOCKS5_DNS:
	    case SSH.PROXY_SOCKS5_IP:
		authEnable = true;
		// Fall through
	    case SSH.PROXY_SOCKS4:
		proxyEnable = true;
		break;
	    }
	} catch (Exception ee) {
	    // !!!
	}
	textPrxHost.setEnabled(proxyEnable);
	textPrxPort.setEnabled(proxyEnable);
	cbNeedAuth.setEnabled(authEnable);

	if(!authEnable)
	    cbNeedAuth.setState(false);

	boolean needAuth = cbNeedAuth.getState();

	textPrxUser.setEnabled(needAuth);
	textPrxPasswd.setEnabled(needAuth);

	if(proxyEnable) {
	    if(textPrxHost.getText().length() == 0)
		textPrxHost.setText(propsHandler.getProperty("proxyhost"));
	    if(textPrxPort.getText().length() == 0)
		textPrxPort.setText(propsHandler.getProperty("proxyport"));
	} else {
	    textPrxHost.setText("");
	    textPrxPort.setText("");
	}

	if(needAuth) {
	    if(textPrxUser.getText().length() == 0)
		textPrxUser.setText(propsHandler.getProperty("proxyuser"));
	} else {
	    textPrxUser.setText("");
	    textPrxPasswd.setText("");
	}

	if(type == SSH.PROXY_SOCKS4) {
	    textPrxUser.setEnabled(true);
	    String user = propsHandler.getProperty("proxyuser");
	    if(textPrxUser.getText().length() == 0) {
		if(user == null)
		    user = "anonymous";
		textPrxUser.setText(user);
	    }
	}

	if(proxyEnable)
	    textPrxHost.requestFocus();
    }
}
