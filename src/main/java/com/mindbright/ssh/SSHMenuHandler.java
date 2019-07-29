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

import java.awt.Frame;
import java.awt.MenuBar;
import java.awt.PopupMenu;

import com.mindbright.application.MindTerm;
import com.mindbright.terminal.TerminalMenuListener;
import com.mindbright.terminal.TerminalMenuHandler;
import com.mindbright.terminal.TerminalWin;

public class SSHMenuHandler implements TerminalMenuListener {
    boolean havePopupMenu = false;

    public void init(MindTerm mindterm, SSHInteractiveClient client, Frame parent, TerminalWin term) {
    }
    public void update() {
    }
    public void close(TerminalMenuHandler originMenu) {
    }
    public void setPopupButton(int popButtonNum) {
    }
    public void prepareMenuBar(MenuBar mb) {
    }
    public void preparePopupMenu(PopupMenu popupmenu) {
    }
    public int getPopupButton() {
	return 0;
    }
    public boolean confirmDialog(String message, boolean defAnswer) {
	return false;
    }
    public void alertDialog(String message) {
    }
    public void textDialog(String head, String text, int rows, int cols, boolean scrollbar) {
    }
}
