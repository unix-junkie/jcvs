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

package com.mindbright.ssh2;

/**
 * This interface defines the different types of prompts which are needed for
 * interactive authentication. It's made generic to be able to allow flexibility
 * in the level of sofistication one wants for user interaction.
 */
public interface SSH2Interactor {
    public String promptLine(String prompt, boolean echo)
	throws SSH2UserCancelException;
    public String[] promptMulti(String[] prompts, boolean[] echos)
	throws SSH2UserCancelException;
    public String[] promptMultiFull(String name, String instruction,
			     String[] prompts, boolean[] echos)
	throws SSH2UserCancelException;
    public int promptList(String name, String instruction, String[] choices)
	throws SSH2UserCancelException;
}
