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

package com.mindbright.gui;

import java.awt.*;
import java.awt.event.*;

public abstract class AWTConvenience {

    public static class CloseAction implements ActionListener {
	Dialog dialog;
	public CloseAction(Dialog dialog) {
	    this.dialog = dialog;
	}
	public void actionPerformed(ActionEvent e) {
	    dialog.setVisible(false);
	}
    }

    public static class CloseAdapter extends WindowAdapter {
	Button b;
	public CloseAdapter(Button b) {
	    this.b = b;
	}
	public void windowClosing(WindowEvent e) {
	    b.dispatchEvent(new ActionEvent(b, ActionEvent.ACTION_PERFORMED,
					    b.getActionCommand()));
	}
    }

    public static class OKCancelAdapter extends KeyAdapter {
    
	protected static boolean isMRJ = false;
  
	static {
	    try { // see <http://developer.apple.com/qa/java/java17.html>
		isMRJ = (System.getProperty("mrj.version") != null);
	    } catch (Throwable e) {
		// applets may not be able to do this
	    }
	}
 
	Button butOK;
	Button butCancel;
  
	public OKCancelAdapter(Button ok, Button cancel) {
	    this.butOK = ok;
	    this.butCancel = cancel;
	}
  
	protected void pushButton(Button target) {
	    if (isMRJ) { // see <http://developer.apple.com/qa/java/java01.html>
		target.dispatchEvent(new KeyEvent(target, KeyEvent.KEY_PRESSED, 
						  System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, 
						  (char)KeyEvent.VK_ENTER));
	    } else { // still can work, just no visual feedback
		target.dispatchEvent(new ActionEvent(target, ActionEvent.ACTION_PERFORMED,
						     target.getActionCommand()));
	    }
	}
  
	public void keyReleased(KeyEvent e) {
	    switch(e.getKeyCode()) {
	    case KeyEvent.VK_ENTER : if (butOK != null) pushButton(butOK); break;
	    case KeyEvent.VK_ESCAPE : if (butCancel != null) pushButton(butCancel); break;
	    }
	}
    }

    public final static void placeDialog(Dialog diag) {
	Dimension sDim = Toolkit.getDefaultToolkit().getScreenSize();
	Dimension mDim = diag.getSize();
	int x, y;
	x = ((sDim.width / 2) - (mDim.width / 2));
	y = ((sDim.height / 2) - (mDim.height / 2));
	diag.setLocation(x, y);
    }

    public final static void setBackgroundOfChildren(Container container) {
	Component[] children = container.getComponents();
	container.setBackground(SystemColor.menu);
	for(int i = 0; i < children.length; i++) {
	    if(children[i] instanceof Choice)
		continue;
	    children[i].setBackground(SystemColor.menu);

	    if(children[i] instanceof Container) {
		setBackgroundOfChildren((Container)children[i]);
	    } else if(children[i] instanceof Choice) {
		continue;
	    } else if(children[i] instanceof TextField || children[i] instanceof List) {
		children[i].setBackground(SystemColor.text);
	    } else {
		children[i].setBackground(SystemColor.menu);
	    }
	}
    }

    public final static void setKeyListenerOfChildren(Container container, KeyListener listener,
						      Class typeOfChild) {
	Component[] children = container.getComponents();
	for(int i = 0; i < children.length; i++) {
	    if(children[i] instanceof Choice)
		continue;
	    if(children[i] instanceof Container) {
		setKeyListenerOfChildren((Container)children[i], listener, typeOfChild);
	    } else if(children[i] != null && (typeOfChild == null ||
					      typeOfChild.isInstance(children[i]))) {
		children[i].addKeyListener(listener);
	    }
	}
    }

    public final static Choice newChoice(String[] opts) {
	Choice c = new Choice();
	for(int i = 0; i < opts.length; i++) {
	    c.add(opts[i]);
	}
	return c;
    }

}
