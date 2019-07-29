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
 * This class implements a module for keyboard-interactive authentication as
 * defined in the auth-kbdinteract protocol spec. It uses the interface
 * <code>SSH2Interactor</code> for all interactions generated in the
 * authentication process. The keyboard-interactive method is suitable for any
 * authentication mechanism where the user enters authentication data via the
 * keyboard (e.g. SecureID and CryptoCard). No specifics about the
 * authentication mechanism is needed in the authentication module itself making
 * it a very flexible way of authentication.
 *
 * @see SSH2AuthModule
 * @see SSH2Interactor
 */
public class SSH2AuthKbdInteract implements SSH2AuthModule {
    SSH2Interactor interactor;
    String         language;
    String         submethods;

    public SSH2AuthKbdInteract(SSH2Interactor interactor) {
	this(interactor, null, null);
    }

    public SSH2AuthKbdInteract(SSH2Interactor interactor, String language,
			       String submethods) {
	if(language == null)
	    language = "";
	if(submethods == null)
	    submethods = "";
	this.interactor = interactor;
	this.language   = language;
	this.submethods = submethods;
    }

    public SSH2TransportPDU processMethodMessage(SSH2UserAuth userAuth,
						 SSH2TransportPDU pdu)
	throws SSH2UserCancelException
    {
	switch(pdu.getType()) {
	case SSH2.MSG_USERAUTH_INFO_REQUEST:
	    String name        = new String(pdu.readString());
	    String instruction = new String(pdu.readString());
	    String peerLang    = new String(pdu.readString());
	    int    numPrompts  = pdu.readInt();
	    int    i;

	    if(numPrompts > 128) {
		numPrompts = 128;
	    }
	    String[]  prompts = new String[numPrompts];
	    boolean[] echos   = new boolean[numPrompts];
	    for(i = 0; i < numPrompts; i++) {
		prompts[i] = new String(pdu.readString());
		echos[i]   = pdu.readBoolean();
	    }

	    String[] answers = interactor.promptMultiFull(name, instruction,
							  prompts, echos);
	    pdu = SSH2TransportPDU.
		createOutgoingPacket(SSH2.MSG_USERAUTH_INFO_RESPONSE);
	    pdu.writeInt(answers.length);
	    for(i = 0; i < answers.length; i++) {
		pdu.writeString(answers[i]);
	    }
	    break;

	default:
	    userAuth.getTransport().getLog().
		warning("SSH2AuthKbdInteract",
			"received unexpected packet of type: " + pdu.getType());
	    pdu = null;
	}
	return pdu;
    }

    public SSH2TransportPDU startAuthentication(SSH2UserAuth userAuth) {
	SSH2TransportPDU pdu = userAuth.createUserAuthRequest("keyboard-interactive");
	pdu.writeString(language);
	pdu.writeString(submethods);
	return pdu;
    }

}
