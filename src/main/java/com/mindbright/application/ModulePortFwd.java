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

public class ModulePortFwd implements MindTermLite.Module {

    private boolean nonInteractive;

    public void init(MindTermLite mindterm) {
	String nonInt = mindterm.getProperty("portfwd_nonint");
	if(nonInt != null && nonInt.equals("true")) {
	    nonInteractive = true;
	}
    }

    Dialog fwdDialog;
    TextField textFwd;
    Checkbox cbRem, cbLoc;
    CheckboxGroup cbg;
    public void activate(final MindTermLite mindterm) {
	if(fwdDialog == null) {
	    Frame parent = mindterm.getParentFrame();
	    fwdDialog = new Dialog(parent, "Port Forward Setup", false);

	    GridBagLayout       grid  = new GridBagLayout();
	    GridBagConstraints  gridc = new GridBagConstraints();
	    Label               lbl;
	    Button              b;

	    fwdDialog.setLayout(grid);

	    gridc.fill      = GridBagConstraints.HORIZONTAL;
	    gridc.anchor    = GridBagConstraints.WEST;
	    gridc.gridy     = 0;
	    gridc.gridwidth = 1;
	    gridc.insets    = new Insets(4, 4, 4, 4);

	    lbl = new Label("[<src-host>:]<src-port>:<dest-host>:<dest-port>");
	    grid.setConstraints(lbl, gridc);
	    fwdDialog.add(lbl);

	    textFwd = new TextField("", 20);
	    gridc.gridy     = 1;
	    grid.setConstraints(textFwd, gridc);
	    fwdDialog.add(textFwd);

	    cbg = new CheckboxGroup();

	    Panel p = new Panel(new FlowLayout());
	    p.add(cbLoc = new Checkbox("Local", cbg, true));
	    p.add(cbRem = new Checkbox("Remote", cbg, false));

	    gridc.anchor = GridBagConstraints.CENTER;
	    gridc.gridy     = 2;
	    gridc.gridwidth  = 2;
	    gridc.fill = GridBagConstraints.NONE;
	    gridc.gridwidth = GridBagConstraints.REMAINDER;
	    grid.setConstraints(p, gridc);
	    fwdDialog.add(p);

	    p = new Panel(new FlowLayout());

	    p.add(b = new Button("Ok"));
	    b.addActionListener(new java.awt.event.ActionListener() {
		public void actionPerformed(java.awt.event.ActionEvent e) {
		    try {
			String fwdSpec = textFwd.getText();
			if(cbLoc.getState()) {
			    newLocalForward(mindterm, fwdSpec);
			} else {
			    newRemoteForward(mindterm, fwdSpec);
			}
			fwdDialog.setVisible(false);
		    } catch(Exception ee) {
			mindterm.alert("Error in forward: " + ee.getMessage());
			// !!! REMOVE
			System.out.println("Error in forward: " + ee);
			ee.printStackTrace();
		    }
		}
	    });
	    p.add(b = new Button("Cancel"));
	    b.addActionListener(new java.awt.event.ActionListener() {
		public void actionPerformed(java.awt.event.ActionEvent e) {
		    fwdDialog.setVisible(false);
		}
	    });

	    gridc.gridy     = 3;
	    grid.setConstraints(p, gridc);
	    fwdDialog.add(p);
	    fwdDialog.setResizable(true);
	    fwdDialog.pack();
	}

	fwdDialog.setVisible(true);
    }

    public String getLabel() {
	if(nonInteractive) {
	    return null;
	}
	return "New Tunnel";
    }

    public boolean isAvailable(MindTermLite mindterm) {
	return mindterm.isConnected();
    }

    public void connected(MindTermLite mindterm) {
	int i;
	try {
	    for(i = 0; i < 32; i++) {
		String spec = mindterm.getProperty("local" + i);
		if(spec == null)
		    break;
		newLocalForward(mindterm, spec);
	    }
	    for(i = 0; i < 32; i++) {
		String spec = mindterm.getProperty("remote" + i);
		if(spec == null)
		    break;
		newRemoteForward(mindterm, spec);
	    }
	} catch (Exception e) {
	    mindterm.alert("Error in forward: " + e.getMessage());
	    // !!! REMOVE
	    System.out.println("Error in forward: " + e.getMessage());
	    e.printStackTrace();

	}
    }

    public void newLocalForward(MindTermLite mindterm, String fwdSpec)
	throws IOException, IllegalArgumentException
    {
	Object[] components = parseForwardSpec(fwdSpec);

	mindterm.getClient().getConnection().
	    newLocalForward((String)components[0],
			    ((Integer)components[1]).intValue(),
			    (String)components[2],
			    ((Integer)components[3]).intValue());
    }

    public void newRemoteForward(MindTermLite mindterm, String fwdSpec)
	throws IllegalArgumentException
    {
	Object[] components = parseForwardSpec(fwdSpec);
	mindterm.getClient().getConnection().
	    newRemoteForward((String)components[0],
			     ((Integer)components[1]).intValue(),
			     (String)components[2],
			     ((Integer)components[3]).intValue());
    }

    public static Object[] parseForwardSpec(String spec)
	throws IllegalArgumentException
    {
	int    d1, d2, d3;
	String tmp;
	Object[] components = new Object[4];

	d1 = spec.indexOf(':');
	d2 = spec.lastIndexOf(':');
	if(d1 == d2)
	    throw new IllegalArgumentException("Invalid port forward spec. " +
					       spec);

	d3 = spec.indexOf(':', d1 + 1);

	if(d3 != d2) {
	    components[0] = spec.substring(0, d1);
	    components[1] = Integer.valueOf(spec.substring(d1 + 1, d3));
	    components[2] = spec.substring(d3 + 1, d2);
	} else {
	    components[0] = "127.0.0.1";
	    components[1] = Integer.valueOf(spec.substring(0, d1));
	    components[2] = spec.substring(d1 + 1, d2);
	}

	tmp = spec.substring(d2 + 1);
	components[3] = Integer.valueOf(tmp);

	return components;
    }

}
