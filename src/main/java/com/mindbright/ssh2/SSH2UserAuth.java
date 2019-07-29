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

import java.util.Hashtable;

import com.mindbright.util.Queue;

/**
 * This class implements the user authentication layer of the secure shell
 * version 2 (ssh2) protocol stack. It operates on a connected
 * <code>SSH2Transport</code> and uses a <code>SSH2Authenticator</code> which
 * contains the user's name and a list of one or more authentication methods
 * (each coupled to a <code>SSH2AuthModule</code> instance) to try to
 * authenticate the user.
 * <p>
 * To create a <code>SSH2UserAuth</code> instance a connected
 * <code>SSH2Transport</code> and a <code>SSH2Authenticator</code> must be
 * created first to be passed to the constructor. The constructor is passive in
 * that it doesn't start any communication. To start the authentication process
 * the method <code>authenticateUser</code> must be called. This method blocks
 * (the authentication process is run in the calling thread) until either the
 * user is authenticated or authentication fails.
 * <p>
 * While the authentication process runs events are reported through callbacks
 * to the <code>SSH2Authenticator</code>. Each <code>SSH2AuthModule</code>
 * instance (one active at a time) handles the actual processing and formatting
 * of the packets specific to the authentication method it represents.
 *
 * @see SSH2Transport
 * @see SSH2Authenticator
 * @see SSH2AuthModule
 */
public final class SSH2UserAuth {
    private SSH2Transport     transport;
    private SSH2Authenticator authenticator;
    private SSH2AuthModule    authModule;

    private volatile boolean isAuthenticated;

    String  currentMethod;

    String service;
    String user;
    String ourMethods;

    Queue procQueue;

    /**
     * This is the constructor. It uses the transport layer. It takes a
     * <code>SSH2Authenticator</code> which contains the user to authenticate
     * and provides a list of the authentication methods to try. It is also used
     * to report authentication events.
     *
     * @param transport     the transport layer
     * @param authenticator the authenticator containing authentication info for
     * the user it represents.
     */
    public SSH2UserAuth(SSH2Transport transport,
			SSH2Authenticator authenticator) {
	this.transport       = transport;
	this.authenticator   = authenticator;
	this.isAuthenticated = false;
	this.procQueue       = new Queue();
    }

    /**
     * Gets our transport layer.
     *
     * @return the transport layer
     */
    public SSH2Transport getTransport() {
	return transport;
    }

    /**
     * Gets our authenticator.
     *
     * @return the authenticator in use
     */
    public SSH2Authenticator getAuthenticator() {
	return authenticator;
    }

    /**
     * Authenticates the user represented by the authenticator to run the given
     * service (currently "ssh-connection" is the only defined service). The
     * authentication process is run in the callers thread hence the call blocks
     * until either the user is authenticated or the authentication fails.
     *
     * @return a boolean indicating whether authentication succeeded or not
     */
    public boolean authenticateUser(String service) {
	this.service       = service;
	this.user          = authenticator.getUsername();
	this.currentMethod = null;

	transport.setUserAuth(this);
	transport.requestService("ssh-userauth");

	String  peerMethods = null;
	boolean retry       = false;
	boolean partial     = false;
	int     lastType    = -1;

    authLoop:
	while(!isAuthenticated) {
	    SSH2TransportPDU pdu = null;

	    if(!retry) {
		pdu = (SSH2TransportPDU)procQueue.getFirst();
		if(pdu == null) {
		    authenticator.authError();
		    break authLoop;
		}
		lastType = pdu.getType();
	    }

	    try {
		switch(lastType) {
		case SSH2.MSG_USERAUTH_FAILURE:
		    if(!retry) {
			peerMethods = new String(pdu.readString());
			partial     = pdu.readBoolean();
			authenticator.peerMethods(peerMethods);
		    }
		    retry = false;

		    transport.getLog().notice("SSH2UserAuth",
					      "failure continuation: " +
					      peerMethods +
					      " (partial: " + partial + ")");

		    if(currentMethod != null) {
			ourMethods =
			    SSH2ListUtil.removeFirstFromList(ourMethods,
							     currentMethod);
			authenticator.authFailure(currentMethod, partial);
		    } else {
			ourMethods = authenticator.getMethods();
		    }

		    transport.getLog().notice("SSH2UserAuth",
					      "our remaining methods: " +
					      (ourMethods.length() > 0 ?
					       ourMethods : "<none>"));

		    currentMethod = SSH2ListUtil.chooseFromList(ourMethods,
								peerMethods);

		    if(currentMethod != null) {
			transport.getLog().notice("SSH2UserAuth",
						  "trying method: " +
						  currentMethod);
			authModule = authenticator.getModule(currentMethod);
			SSH2TransportPDU modPDU =
			    authModule.startAuthentication(this);
			transport.transmit(modPDU);
		    } else {
			authenticator.noMoreMethods();
			transport.getLog().notice("SSH2UserAuth",
				  "no more authentication methods, giving up");
			break authLoop;
		    }
		    break;

		case SSH2.MSG_USERAUTH_SUCCESS:
		    transport.getLog().notice("SSH2UserAuth",
				      "successful authentication with " +
					      currentMethod);
		    isAuthenticated = true;
		    authenticator.authSuccess(currentMethod);
		    break;

		case SSH2.MSG_USERAUTH_BANNER:
		    String msg = new String(pdu.readString());
		    transport.getLog().warning("SSH2UserAuth", "banner: " +
					       msg);
		    authenticator.displayBanner(msg);
		    break;

		case SSH2.MSG_SERVICE_ACCEPT:
		    if(transport.incompatibleServiceAccept) {
			transport.getLog().notice("SSH2UserAuth",
						  "server accepted " +
						  service +
						  " (draft incompatible)");
		    } else {
			transport.getLog().notice("SSH2UserAuth",
						  "server accepted: " +
					  new String(pdu.readString()));
		    }
		    doNoneAuth();
		    break;

		case SSH2.FIRST_USERAUTH_METHOD_PACKET:
		case 61:
		case 62:
		case 63:
		case 64:
		case 65:
		case 66:
		case 67:
		case 68:
		case 69:
		case 70:
		case 71:
		case 72:
		case 73:
		case 74:
		case 75:
		case 76:
		case 77:
		case 78:
		case SSH2.LAST_USERAUTH_METHOD_PACKET:
		    if(authModule != null) {
			SSH2TransportPDU modPDU =
			    authModule.processMethodMessage(this, pdu);
			if(modPDU != null)
			    transport.transmit(modPDU);
		    } else {
			transport.fatalDisconnect(SSH2.DISCONNECT_PROTOCOL_ERROR,
						  "Received userauth method " +
						  "packet when no method selected");
			break authLoop;
		    }
		    break;
		}
	    } catch (SSH2UserCancelException e) {
		String msg = e.getMessage();
		authenticator.moduleCancel(currentMethod, msg);
		transport.fatalDisconnect(
				  SSH2.DISCONNECT_AUTH_CANCELLED_BY_USER,
				  "User cancel: " + msg);
		transport.getLog().
		    notice("SSH2UserAuth",
			   "user canceled authentication: "
			   + msg);
		break authLoop;
	    } catch (SSH2Exception e) {
		transport.getLog().error("SSH2UserAuth",
					 "authenticateUser",
					 "error in module '" +
					 currentMethod + "': " +
					 e.getMessage());
		authenticator.moduleFailure(currentMethod, e);
		partial  = false;
		retry    = true;
		lastType = SSH2.MSG_USERAUTH_FAILURE;
	    }
	    if(pdu != null) {
		pdu.release();
	    }
	}

	return isAuthenticated;
    }

    /**
     * Creates a packet of type USERAUTH_REQUEST (as defined in the userauth
     * protocol spec.). This is a convenience method which creates the whole
     * packet given the method name (i.e. fills in username and service). It is
     * typically used by <code>SSH2AuthModule</code> implementors to create the
     * packet to return from the method <code>startAuthentication</code>.
     *
     * @param method the name of the authentication method
     *
     * @return the complete USERAUTH_REQUEST packet
     */
    public SSH2TransportPDU createUserAuthRequest(String method) {
	SSH2TransportPDU pdu =
	    SSH2TransportPDU.createOutgoingPacket(SSH2.MSG_USERAUTH_REQUEST);
	pdu.writeString(user);
	pdu.writeString(service);
	pdu.writeString(method);
	return pdu;
    }

    /**
     * Terminates the authentication process.
     */
    public void terminate() {
	procQueue.setBlocking(false);
    }

    /**
     * Checks if the user represented by the <code>SSH2Authenticator</code> we
     * process has been authenticated yet.
     *
     * @return a boolean indicating if the user is authenticated or not
     */
    public boolean isAuthenticated() {
	return isAuthenticated;
    }

    private void doNoneAuth() {
	SSH2TransportPDU pdu = createUserAuthRequest("none");
	transport.transmit(pdu);
    }

    void processMessage(SSH2TransportPDU pdu) {
	procQueue.putLast(pdu);
    }

}

