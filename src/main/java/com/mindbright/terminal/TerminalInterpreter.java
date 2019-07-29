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

package com.mindbright.terminal;

public abstract class TerminalInterpreter {

  protected Terminal term;

  public final static int IGNORE = -1;

  abstract public String terminalType();
  abstract public int interpretChar(char c);

  public void vtReset() {
  }

  public void keyHandler(int virtualKey, int gMode) {
  }

  public void mouseHandler(int x, int y, boolean press, int modifiers) {
  }

  public final void setTerminal(Terminal term) {
    this.term = term;
  }

}
