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

import java.net.Socket;

/**
 * This interface is an event callback interface used to monitor the connection
 * layer of an ssh2 connection. It is used with the class
 * <code>SSH2Connection</code> to get info on the progress and status of all
 * forwards and resulting channels through the connection.
 * <p>
 * All callback methods which indicates channel open confirmation or channel
 * open failure uses the naming convention that when it begins with "local"
 * (e.g. <code>localDirectConnect</code>) it is coupled to a channel originating
 * locally and conversly when it begins with "remote"
 * (e.g. <code>remoteForwardedConnect</code>) it is coupled to a channel
 * originating remotely. The naming of the channel types in the connection
 * protocol specification is used to identify whether a channel is a local
 * forward or a remote forward. This means that local forwards are called
 * "direct" and remote forwards are called "forwarded". This naming can be
 * somewhat confusing for example the method <code>localForwardedConnect</code>
 * might be the one might expect to be called when a local forward channel is
 * confirmed to be open, instead the call is
 * <code>localDirectConnect</code>. The reason for this is to have a symmetrical
 * naming for all callbacks which are valid on both client and server side,
 * hence check each callback and see if it applies to youe need (i.e. client or
 * server).
 *
 * @see SSH2Connection
 * @see SSH2ConnectionEventAdapter
 * @see SSH2Listener
 * @see SSH2Channel
 */
public interface SSH2ConnectionEventHandler
{
    // !!! TODO add globalRequest<type> calls... ???
    // public void globalRequest(SSH2Connection conn, String type);

    /**
     * Called when a new channel is added (i.e. a new channel has been opened
     * through a port forward).
     *
     * @param connection the connection layer responsible
     * @param channel    the channel which was added
     *
     */
    public void channelAdded(SSH2Connection connection, SSH2Channel channel);

    /**
     * Called when a channel is deleted (i.e. a channel has been finally
     * removed).
     *
     * @param connection the connection layer responsible
     * @param channel    the channel which was deleted
     */
    public void channelDeleted(SSH2Connection connection, SSH2Channel channel);

    /**
     * Called when a channel is closed (i.e. the channel has been closed and
     * will be flushed and then removed).
     *
     * @param connection the connection layer responsible
     * @param channel the channel which was deleted
     */
    public void channelClosed(SSH2Connection connection, SSH2Channel channel);

    /**
     * Called when a listener accepts a new connection (i.e. a local forward is
     * opened).
     *
     * @param listener  the responsible listener
     * @param fwdSocket the socket which resulted
     */
    public void listenerAccept(SSH2Listener listener, Socket fwdSocket);

    /**
     * Called when a listener connects a channel through a local forward by
     * creating the new channel instance and sending a CHANNEL_OPEN request to
     * the server.
     *
     * @param listener  the responsible listener
     * @param fwdSocket the socket which resulted
     */
    public void listenerConnect(SSH2Listener listener, Socket fwdSocket);

    /**
     * Called on the server side when a remote forward channel is confirmed to
     * be open.
     *
     * @param connection the connection layer responsible
     * @param listener   the responsible listener
     * @param channel the channel which was opened
     */
    public void localForwardedConnect(SSH2Connection connection,
				    SSH2Listener listener,
				    SSH2Channel channel);
    /**
     * Called on the client side when a local forward channel is confirmed to
     * be open.
     *
     * @param connection the connection layer responsible
     * @param listener   the responsible listener
     * @param channel the channel which was opened
     */
    public void localDirectConnect(SSH2Connection connection,
				   SSH2Listener listener,
				   SSH2Channel channel);

    /**
     * Called on the client side when a session channel is confirmed to be open.
     *
     * @param connection the connection layer responsible
     * @param channel the channel which was opened
     */
    public void localSessionConnect(SSH2Connection connection,
				    SSH2Channel channel);

    /**
     * Called on the server side when an X11 channel is confirmed to be open.
     *
     * @param connection the connection layer responsible
     * @param listener   the responsible listener
     * @param channel the channel which was opened
     */
    public void localX11Connect(SSH2Connection connection,
				SSH2Listener listener,
				SSH2Channel channel);

    /**
     * Called on either side when a locally originating channel gets a channel
     * open failure indication from peer. See the class <code>SSH2</code> for
     * reason codes.
     *
     * @param connection  the connection layer responsible
     * @param channel     the channel which was opened
     * @param reasonCode  the reason code 
     * @param reasonText
     * @param languageTag
     *
     * @see SSH2
     */
    public void localChannelOpenFailure(SSH2Connection connection,
					SSH2Channel channel,
					int reasonCode, String reasonText,
					String languageTag);

    /**
     * Called on the client side when a remote forward channel has been
     * confirmed to be open.
     *
     * @param connection the connection layer responsible
     * @param remoteAddr
     * @param remotePort
     * @param channel    the channel which was opened
     */
    public void remoteForwardedConnect(SSH2Connection connection,
				       String remoteAddr, int remotePort,
				       SSH2Channel channel);

    public void remoteDirectConnect(SSH2Connection connection,
				    SSH2Channel channel);

    public void remoteSessionConnect(SSH2Connection connection,
				     String remoteAddr, int remotePort,
				     SSH2Channel channel);

    public void remoteX11Connect(SSH2Connection connection,
				 SSH2Channel channel);

    /**
     * Called on either side when there is a problem opening a remotely
     * originating channel resulting in a channel open failure indication beeing
     * sent back to peer. The exception which was the cause of the problem is
     * provided aswell as the type of channel and relevant addresses and ports.
     *
     * @param connection  the connection layer responsible
     * @param channelType the type of channel
     * @param targetAddr  the address which should have been connected to
     * @param targetPort  the port which should have been connected to
     * @param originAddr  the address where the channel originated (depends on type)
     * @param originPort  the port where the channel originated (depends on type)
     * @param cause       the exception which was the cause of the problem
     */
    public void remoteChannelOpenFailure(SSH2Connection connection,
					 String channelType,
					 String targetAddr, int targetPort,
					 String originAddr, int originPort,
					 SSH2Exception cause);

}
