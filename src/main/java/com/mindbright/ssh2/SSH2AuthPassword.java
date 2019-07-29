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
 * This class implements a module for password authentication as defined in the
 * userauth protocol spec. It can either be used passively (i.e. the password is
 * known beforehand), or it can be used interactively through the
 * <code>SSH2Interactor</code> callback interface.
 *
 * @see SSH2AuthModule
 */
public class SSH2AuthPassword implements SSH2AuthModule {
    private String password;
    private String newPassword;
    private SSH2Interactor interactor;

    public SSH2AuthPassword(String password) {
	setPassword(password);
    }

    public SSH2AuthPassword(SSH2Interactor interactor) {
	this.interactor = interactor;
    }

    public void setPassword(String password) {
	this.password = password;
    }

    public void setNewPassword(String newPassword) {
	this.newPassword = newPassword;
    }

    protected String getPassword() throws SSH2UserCancelException {
	if(password != null)
	    return password;
	if(interactor != null) {
	    password = interactor.promptLine("Password: ", false);
	} else {
	    password = "";
	}
	return password;
    }

    protected String getNewPassword(String prompt, String language)
	throws SSH2UserCancelException
    {
	if(newPassword != null)
	    return newPassword;
	if(interactor != null) {
	    // !!! TODO how is one expected to give user a chance to
	    // rewrite password... (given only one prompt)
	    //
	    // TODO 2 language tag
	    //
	    newPassword = interactor.promptLine(prompt, false);
	} else {
	    newPassword = "";
	}
	return newPassword;
    }

    public SSH2TransportPDU processMethodMessage(SSH2UserAuth userAuth,
						 SSH2TransportPDU pdu)
	throws SSH2UserCancelException
    {
	switch(pdu.getType()) {
	case SSH2.MSG_USERAUTH_PASSWD_CHANGEREQ:
	    String prompt   = pdu.readJavaString();
	    String language = pdu.readJavaString();
	    pdu = createChangeRequest(userAuth, prompt, language);
	    break;

	default:
	    userAuth.getTransport().getLog().
		warning("SSH2AuthPassword",
			"received unexpected packet of type: " + pdu.getType());
	    pdu = null;
	}

	return pdu;
    }

    public SSH2TransportPDU startAuthentication(SSH2UserAuth userAuth)
	throws SSH2UserCancelException
    {
	SSH2TransportPDU pdu = userAuth.createUserAuthRequest("password");
	pdu.writeBoolean(false);
	pdu.writeString(getPassword());
	return pdu;
    }

    private SSH2TransportPDU createChangeRequest(SSH2UserAuth userAuth,
						 String prompt,
						 String language)
	throws SSH2UserCancelException 
    {
	SSH2TransportPDU pdu = userAuth.createUserAuthRequest("password");
	pdu.writeBoolean(true);
	pdu.writeString(getPassword());
	pdu.writeString(getNewPassword(prompt, language));
	return pdu;
    }

}
