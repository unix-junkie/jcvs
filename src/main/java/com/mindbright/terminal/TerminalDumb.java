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

public final class TerminalDumb extends TerminalInterpreter {

  public String terminalType() {
    return "DUMB";
  }

  public int interpretChar(char c) {
    switch(c) {
    case 7: // BELL
      term.doBell();
      break;
    case 8: // BS/CTRLH
      term.doBS();
      break;
    case '\t':
      term.doTab();
      break;
    case '\r':
      term.doLF();
      break;
    case '\n':
      term.doCR();
      break;
    default:
      return (int)c;
    }
    return IGNORE;
  }

}
