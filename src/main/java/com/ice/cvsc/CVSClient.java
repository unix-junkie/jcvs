/*
** Java cvs client library package.
** Copyright (c) 1997-2003 by Timothy Gerard Endres
** 
** This program is free software.
** 
** You may redistribute it and/or modify it under the terms of the GNU
** Library General Public License (LGPL) as published by the Free Software
** Foundation.
**
** Version 2 of the license should be included with this distribution in
** the file LICENSE.txt, as well as License.html. If the license is not
** included	with this distribution, you may find a copy at the FSF web
** site at 'www.gnu.org' or 'www.fsf.org', or you may write to the Free
** Software Foundation at 59 Temple Place - Suite 330, Boston, MA 02111 USA.
**
** THIS SOFTWARE IS PROVIDED AS-IS WITHOUT WARRANTY OF ANY KIND,
** NOT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY. THE AUTHOR
** OF THIS SOFTWARE, ASSUMES _NO_ RESPONSIBILITY FOR ANY
** CONSEQUENCE RESULTING FROM THE USE, MODIFICATION, OR
** REDISTRIBUTION OF THIS SOFTWARE. 
** 
*/


package com.ice.cvsc;

import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.zip.*;
import java.applet.*;


import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.session.SessionChannelClient;
import com.sshtools.j2ssh.authentication.AuthenticationProtocolState;
import com.sshtools.j2ssh.authentication.PasswordAuthenticationClient;
import com.sshtools.j2ssh.configuration.SshConnectionProperties;
import com.sshtools.j2ssh.transport.HostKeyVerification;
import com.sshtools.j2ssh.transport.TransportProtocolException;
import com.sshtools.j2ssh.transport.publickey.SshPublicKey;


/**
 * Implements the client side of the CVS server-client protocol.
 * This object is used by a CVSProject object to implement the
 * protocols required to communicate with the CVS server and
 * complete CVS requests. CVSClient's use TCP communications
 * to a specified host and port (default is 2401). CVSClients
 * <em>can</em> stand on their own, however, there is not much
 * interesting that can be accomplished without the information
 * contained in a CVSProject. Typically, you use a CVSClient
 * by handing it a CVSRequest, and it will hand back a CVSResponse.
 *
 * Thanks to Wes Sonnenreich <wes@sonnenreich.com> for his original
 * attempt at the integration of MindBright's SSH package into this
 * client. The effort was most helpful in understanding the package.
 *
 * @version $Revision: 2.20 $
 * @author Timothy Gerard Endres, <a href="mailto:time@ice.com">time@ice.com</a>.
 * @see CVSRequest
 * @see CVSResponse
 * @see CVSProject
 */

//
// NOTES in code:
//
// EH-null-ui  Etienne-Hugues Fortin <ehfortin@sympatico.ca>
//   Added a new "NullCVSUI" to ensure that the CVS UI interface is
//   always established.
//
// SW-flush-out  Shawn Willden <shawn@willden.org>
//   Since out GZIP stream was wrapped in a BufferedOutputStream
//   we need to flush() to move it all out. Otherwise, the command
//   data sat buffered and the server never responded.
//



public
class		CVSClient
extends		Object
implements	HostKeyVerification
	{
	static public final String		RCS_ID = "$Id: CVSClient.java,v 2.20 2003/07/27 04:32:56 time Exp $";
	static public final String		RCS_REV = "$Revision: 2.20 $";

    public static final int			DEFAULT_SSH_PORT = 22;
    public static final int			DEFAULT_RSH_PORT = 514;
	public static final int			DEFAULT_CVS_PORT = 2401;
	public static final int			DEFAULT_DIR_PORT = 2402;
	public static final String		DEFAULT_TEMP_PATH = ".";


	/**
	 * Used to indicate that an ascii file is being transferred.
	 */
	public static final int			TRANSLATE_NONE = 0;
	/**
	 * Used to indicate that a binary file is being transferred.
	 */
	public static final int			TRANSLATE_ASCII = 1;
	
	/**
	 * The minimum size before a file is gzip-ed in 'gzip-file-contents' mode.
	 */
	private static final int		MIN_GZIP_SIZE = 1024;

	private static final int		MAX_FILE_SIZE = 1000000;
	private static final boolean	LIMIT_FILE_SIZE = false;

	private Object				canLock;
	private boolean				canceled;

	private String				hostName;
	private int					port;
	private int					tempCounter;

	private boolean				usingGZIP;
	private boolean				serverIsOpen;
	private boolean				tracingTCPData;

	private Process				process;
	private Socket				socket;
	private InputStream			instream;
	private OutputStream		outstream;

	private String				tempPath;
	private String				reason;
	private String				recentEntryRepository;

	/**
	 * Hashtable of all 'Directory' commands that we have sent
	 * to above sending redundant commands.
	 */
	private Hashtable			dirHash;

	/**
	 * Flag that determines whether or not we make the extra effort
	 * to support multiple interface machines. The means we use to
	 * determine the correct interface involves multiple socket opens,
	 * which is expensive, so we only do it if necessary.
	 *
	 */
	private boolean				supportMultipleInterfaces = false;

	/**
	 * SSH supporting fields
	 */
	SshClient					sshClient = null;
	SessionChannelClient		sshSession = null;

	/**
	 * Creates a CVS client.
	 * The client is unusable, however, until
	 * the hostname and port number are established.
	 *
	 * @param adminDirPath The pathname of the admin ('CVS') directory.
	 */
	public CVSClient()
		{
		super();

		InitFields();
		}

	/**
	 * Creates a CVS client using the provided hostname and port number.
	 *
	 * @param hostName The hostname of the cvs server.
	 * @param port The port number of the cvs server (typically 2401).
	 */
	public CVSClient( String hostName, int port )
		{
		super();

		InitFields();

		this.port = port;
		this.hostName = hostName;
		}

	/**
	 * Common initializer for all our contructors.
	 */
	private void
	InitFields()
		{
		this.canceled = false;
		this.canLock = new Object();

		this.hostName = null;

		this.port = CVSClient.DEFAULT_CVS_PORT;

		this.tempPath = CVSClient.DEFAULT_TEMP_PATH;

		this.tempCounter =
			(int)( System.currentTimeMillis() % (long)0x0FFFFFFF ); 

		this.serverIsOpen = false;
		this.tracingTCPData = false;

		this.socket = null;
		this.instream = null;
		this.outstream = null;

		this.reason = "";
		this.recentEntryRepository = "";
		}
	
	/**
	 * Returns the hostname of the cvs server.
	 */
	public String
	getHostName()
		{
		return this.hostName;
		}
	
	/**
	 * Sets the hostname of the cvs server.
	 *
	 * @param hostName The hostname of the cvs server.
	 */
	public void
	setHostName( String hostName )
		{
		this.hostName = hostName;
		}
	
	/**
	 * Returns the port number of the cvs server.
	 */
	public int
	getPort()
		{
		return this.port;
		}

	/**
	 * Sets the port number of the cvs server.
	 *
	 * @param port The port number of the cvs server (typically 2401).
	 */
	public void
	setPort( int port )
		{
		this.port = port;
		}

	/**
	 * Returns the port number of the cvs server.
	 */
	public boolean
	getMultipleInterfaceSupport()
		{
		return this.supportMultipleInterfaces;
		}

	/**
	 * Sets the port number of the cvs server.
	 *
	 * @param port The port number of the cvs server (typically 2401).
	 */
	public void
	setMultipleInterfaceSupport( boolean flag )
		{
		this.supportMultipleInterfaces = flag;
		CVSTracer.traceIf( flag, "Supporting multiple interfaces." );
		}

	/**
	 * Returns the pathname of the directory in which temporary files are created.
	 */
	public String
	getTempDirectory()
		{
		return this.tempPath;
		}
	
	/**
	 * Sets the pathname of the directory in which temporary files are created.
	 *
	 * @param tempPath The full pathname of the temporary directory.
	 */
	public void
	setTempDirectory( String tempPath )
		{
		this.tempPath = tempPath;
		}

	//
	// REVIEW
	// UNDONE
	// Should we use a StringBuffer for reason, and provide
	// a "appendReason()" method?
	//

	/**
	 * Returns the reason for the last error.
	 */
	public String
	getReason()
		{
		return this.reason;
		}

	/**
	 * Sets the resaon for the last error.
	 *
	 * @param reason The string describing the reason.
	 */
	public void
	setReason( String reason )
		{
		this.reason = reason;
		}

	/**
	 * Indicates whether or not the connection to the server is established.
	 */
	public boolean
	isServerOpen()
		{
		return this.serverIsOpen;
		}

	public boolean
	sendCVSRootDirectory( CVSRequest request )
		{
		boolean result = true;

		result =
			this.sendLine
				( "Root " + request.getRootDirectory() );

		return result;
		}

	/**
	 * Send the "root" of our repository. Since all of our commands
	 * now work with the assumption that everything is relative to
	 * "./", we need to properly establish 'Directory .' for the
	 * module that we are working with.
	 *
	 */

	public boolean
	sendRootRepository( CVSRequest request )
		{
		boolean result = true;

		result = this.sendLine( "Directory ." );
		result = this.sendLine( request.getRootRepository() );

		return result;
		}

	/**
	 * This method is used to send the 'Directory' command before
	 * an entry is sent, to set the "context" of the entry (i.e.,
	 * the entry's directory).
	 *
	 * <strong>Note</strong> that jCVS has a peculiarity. We only
	 * send the entries the user has selected in many cases. Thus,
	 * if we refer to a file 'com/ice/cvsc/CVSLog.java', we send
	 * that 'Directory' command, but none for the directories 'com',
	 * and 'ice'. In most cases, this is not an issue, but for a top
	 * level command like Update, this causes entire branches of the
	 * project hierarchy to be skipped because we had not sent the
	 * 'Directory' command for that level. To solve this, whenever
	 * we send a 'Directory' command, we send all of the intermediate
	 * levels as well. In order to minimize the redundancy, we keep
	 * a list of what has already been sent in 'this.dirHash'.
	 *
	 * @param request The current request.
	 * @param entry The entry for which to send the command.
	 * @return True if successful, else failure.
	 */

	public boolean
	sendEntryRepository( CVSRequest request, CVSEntry entry )
		{
		boolean result = true;

		CVSTracer.traceIf( request.traceRequest,
			"sendEntryRepository: " + entry.dumpString() );

		String localDir =
			CVSCUtilities.stripFinalSlash( entry.getLocalDirectory() );

		CVSTracer.traceIf( request.traceRequest,
			"sendEntryRepository: localDir = '" + localDir + "'" );

		if ( ! localDir.equals( this.recentEntryRepository ) )
			{
			String stickyStr;
			String dirStr = localDir;
			String repStr = entry.getRepository();

			CVSTracer.traceIf( request.traceRequest,
				"sendEntryRepository: INITIAL \n" +
				"  dirStr = '" + dirStr + "'\n" +
				"  repStr = '" + repStr + "'" );

			Vector v = new Vector();
			stickyStr = this.getStickTag( request, dirStr );
			if ( stickyStr.length() > 0 )
				v.addElement( stickyStr );
			v.addElement( entry.getRepository() );
			v.addElement( "Directory " + localDir );
			this.dirHash.put( localDir, entry.getRepository() );

			for ( int pi = 0 ; ; ++pi )
				{
				int idxD = dirStr.lastIndexOf( "/" );
				int idxR = repStr.lastIndexOf( "/" );

				if ( idxD < 0 || idxR < 0 )
					{
					for ( int i = v.size() - 1 ; i >= 0 ; --i )
						{
						result = this.sendLine( (String)v.elementAt(i) );
						}
					break;
					}
				else
					{
					dirStr = dirStr.substring( 0, idxD );
					repStr = repStr.substring( 0, idxR );

					CVSTracer.traceIf( request.traceRequest,
						"sendEntryRepository: PART [" + pi + "]\n" +
						"  dirStr = '" + dirStr + "'\n" +
						"  repStr = '" + repStr + "'" );

					if ( this.dirHash.get( dirStr ) == null )
						{
						// NOTE These MUST be in reverse order!!!
						stickyStr = this.getStickTag( request, dirStr );
						if ( stickyStr.length() > 0 )
							v.addElement( stickyStr );
						v.addElement( repStr );
						v.addElement( "Directory " + dirStr );
						this.dirHash.put( dirStr, repStr );
						}
					}
				}

			result = this.sendSticky( request, entry );
			result = this.sendStatic( request, entry );

			this.recentEntryRepository = localDir;
			}

		return result;
		}

	public boolean
	sendCVSArgument( String argument )
		{
		boolean result = true;

		result = this.sendLine( "Argument " + argument );

		return result;
		}

	// REVIEW
	// Should be be computing it via the rootRepository and
	// rootDirectory?
	//
	public boolean
	sendCVSModule( CVSRequest request )
		{
		boolean result = true;

		result = this.sendCVSArgument( "." );

		return result;
		}

	public boolean
	sendSetVariables( CVSRequest request )
		{
		boolean result = true;

		String[] vars = request.getSetVariables();

		if ( vars != null )
			for ( int i = 0 ; result && i < vars.length ; ++i )
				result = this.sendLine( "Set " + vars[i] );

		return result;
		}

	public boolean
	sendModified(
			CVSRequest request, CVSEntry entry,
			File entryFile, boolean empty, int trans )
		{
		boolean		result = true;

		result = this.sendEntryRepository( request, entry );

		if ( result )
			result = this.sendLine( "Modified " + entry.getName() );

		if ( result )
 			result = this.sendLine( entry.getModeLine() );

		if ( result )
			{
			if ( empty )
				{
				result = this.sendLine( "0" );
				}
			else
			switch ( trans )
				{
				case CVSClient.TRANSLATE_ASCII:
					result = this.sendFileAscii
						( entry, entryFile, request.gzipFileMode );
					break;

				default:
					result = this.sendFileRaw
						( entry, entryFile, request.gzipFileMode );
					break;
				}
			}

		if ( ! result )
			{
			CVSLog.logMsg
				( "CVSClient.sendModified: ERROR sending file: "
					+ this.getReason() );
			}

		return result;
		}

	public boolean
	sendLostEntry( CVSRequest request, CVSEntry entry, boolean useUnchanged )
		{
		boolean result = true;
		
		CVSTracer.trace(
			 "sendLostEntry: '" + entry.getName() + "'" );
		//
		// if ( request.useUnchanged == false )
		// If 'UseUnchanged' has NOT been sent, then lost
		// entries _must_ have a 'Lost' request sent.
		//
		// if ( request.useUnchanged == true )
		// If 'UseUnchanged' has been sent, then lost
		// entries are indicated by nothing being sent.
		//
		if ( ! useUnchanged )
			{
			result = this.sendEntryRepository( request, entry );
			if ( result )
				{
				result = this.sendLine
					( "Lost " + entry.getName() );
				}
			}
		
		return result;
		}

	public boolean
	sendUnchangedEntry( CVSRequest request, CVSEntry entry, boolean useUnchanged )
		{
		boolean result = true;
		
		CVSTracer.trace(
			"sendUnchangedEntry: '" + entry.getName() + "'" );

		//
		// if ( request.useUnchanged == true )
		// If 'UseUnchanged' has been sent, then unchanged
		// entries _must_ have an 'Unchanged' request sent.
		//
		// if ( request.useUnchanged == false )
		// If 'UseUnchanged' has NOT been sent, then the
		// 'Unchanged' line is verbotten, and unchanged
		// entries are indicated by nothing being sent.
		//
		if ( useUnchanged )
			{
			result = this.sendEntryRepository( request, entry );
			if ( result )
				result = this.sendLine
					( "Unchanged " + entry.getName() );
			}
		
		return result;
		}

	public boolean
	sendCVSEntry( CVSRequest request, CVSEntry entry, File entryFile )
		{
		boolean		result = true;
		boolean		fileExists = false;
		boolean		fileIsModified = false;

		CVSTracer.traceIf
			( request.traceRequest, "sendCVSEntry: " + entry.dumpString() );

		// SPECIAL CASE for directories. This is currently only used when we
		// are adding directories, usually to support adding new files.
		
		if ( entry.isDirectory() )
			{
			result = this.sendEntryRepository( request, entry );

			if ( result )
				{
				//
				// SPECIAL CASE
				// In the case of directories, we do not send 'Entry'.
				// We send the 'Directory' command, which is the equivalent
				// of 'Entry' for directories.
				//
				result = this.sendLine
					( "Directory " +
						CVSCUtilities.stripFinalSlash
							( entry.getFullName() ) );

				if ( result )
					{
					String localDir =
						CVSCUtilities.stripFinalSlash
							( entry.getLocalDirectory() );

					result = this.sendLine( entry.getRepository() );
					result = this.sendSticky( request, localDir );
					}

				this.recentEntryRepository = entry.getFullName();
				}

			return result;
			}

		if ( entryFile.exists() )
			{
			fileExists = true;
			fileIsModified = entry.isLocalFileModified( entryFile );
			}

		int trans = CVSCUtilities.computeTranslation( entry );

		// SPECIAL CASE when no 'Entry' lines go up...

		if ( ! request.sendEntries )
			{
			// If no 'Entry' lines, the only thing that _can_
			// happen is 'Modified's...
			if ( fileIsModified || entry.isNewUserFile()
					|| request.forceModifieds )
				{
				if ( request.sendModifieds || request.forceModifieds )
					{
					result = this.sendEntryRepository( request, entry );

					if ( result )
						{
						request.getUserInterface().uiDisplayProgressMsg
							( "Uploading file '" + entry.getFullName() + "'..." );

						result = this.sendModified
									( request, entry, entryFile,
										request.sendEmptyMods, trans );
						}
					}
				}

			return result;
			}

		// Normal case...

		String entryStr =
			entry.getServerEntryLine
				( entryFile.exists(), fileIsModified );

		result = this.sendEntryRepository( request, entry );

		if ( result )
			{
			result = this.sendLine( "Entry " + entryStr );
			}

		if ( result )
			{
			if ( fileExists )
				{
				if ( fileIsModified || entry.isNewUserFile()
						|| request.forceModifieds )
					{
					if ( request.sendModifieds
							|| entry.isNewUserFile()
							|| request.forceModifieds )
						{
						request.getUserInterface().uiDisplayProgressMsg
							( "Uploading file '" + entry.getName() + "'..." );

						//
						// REVIEW
						// Here we override the 'Special Mods' flag
						// when there is a conflict, which appears
						// to be the only case where this optimization
						// does not work properly. However, we better
						// make a more thorough analysis to be certain.
						//
						boolean sendEmpties = request.sendEmptyMods;
						if ( entry.isInConflict() )
							sendEmpties = false;

						result =
							this.sendModified
								( request, entry, entryFile, sendEmpties, trans );
						}
					else
						{
						result =
							this.sendUnchangedEntry
								( request, entry, request.useUnchanged );
						}
					}
				else
					{
					result =
						this.sendUnchangedEntry
							( request, entry, request.useUnchanged );
					}
				}
			else
				{
				result =
					this.sendLostEntry
						( request, entry, request.useUnchanged );
				}
			}

		return result;
		}
   
	public boolean
	sendCVSEntries( CVSRequest request )
		{
		int			i, count;
		File		entryFile;
		CVSEntry	entry;
		CVSEntryVector	entries;
		boolean		result = true;

		count = request.getEntries().size();
		entries = request.getEntries();
		
		for ( i = 0 ; result && i < count ; ++i )
			{
			entry = (CVSEntry) entries.elementAt( i );

			entryFile = request.getLocalFile( entry );

			result = this.sendCVSEntry( request, entry, entryFile );

			if ( this.isCanceled() )
				break;
			}

		return result;
		}

	public String
	getStickTag( CVSRequest request, String localDir )
		{
		String	result = "";
		Hashtable stickys = request.getStickys();
		if ( stickys != null )
			result = (String) stickys.get( localDir );
		return ( result == null ? "" : result );
		}

	public boolean
	sendSticky( CVSRequest request, CVSEntry entry )
		{
		return this.sendSticky( request, entry.getLocalDirectory() );
		}

	public boolean
	sendSticky( CVSRequest request, String localDir )
		{
		boolean	result = true;

		Hashtable stickys = request.getStickys();

		if ( stickys != null )
			{
			String tagSpec = (String) stickys.get( localDir );
			if ( tagSpec != null && tagSpec.length() > 1 )
				{
				result = this.sendLine( "Sticky " + tagSpec );
				}
			}

		return result;
		}

	public boolean
	sendStatic( CVSRequest request, CVSEntry entry )
		{
		boolean	result = true;

		Hashtable statics = request.getStatics();
		if ( statics != null )
			{
			String isStatic = (String)
				statics.get( entry.getLocalDirectory() );

			if ( isStatic != null )
				{
				result = this.sendLine( "Static-directory" );
				}
			}

		return result;
		}

	public boolean
	sendGlobalArguments( CVSArgumentVector arguments )
		{
		int		i;
		boolean	result = true;

		for ( i = 0 ; i < arguments.size() ; ++i )
			{
			String argStr = arguments.argumentAt(i);
			result = this.sendLine( "Global_option " + argStr );
			}

		return result;
		}

	public boolean
	sendArguments( CVSArgumentVector arguments )
		{
		int		i;
		String	argLine;
		boolean xArg = false;
		boolean	result = true;

		for ( i = 0 ; i < arguments.size() ; ++i )
			{
			String argStr = arguments.argumentAt(i);

			if ( argStr.indexOf( '\n' ) < 0 )
				{
				result = this.sendLine( "Argument " + argStr );
				}
			else
				{
				xArg = false;

				StringTokenizer toker =
					new StringTokenizer( argStr, "\n" );

				for ( ; result ; )
					{
					try { argLine = toker.nextToken(); }
					catch ( NoSuchElementException ex )
						{
						break;
						}

					String prefix =
						( xArg ? "Argumentx " : "Argument ");

					result = this.sendLine( prefix + argLine );
					xArg = true;
					}
				}
			}

		return result;
		}

	public boolean
	sendEntriesArguments( CVSRequest request )
		{
		int			i;
		CVSEntry	entry;
		boolean		result = true;

		CVSArgumentVector	args;
		CVSEntryVector		entries = request.getEntries();
		
		if ( entries.size() < 1 )
			return true;

		args = new CVSArgumentVector( entries.size() );

		for ( i = 0 ; i < entries.size() ; ++i )
			{
			entry = (CVSEntry) entries.elementAt(i);

			String argName =
				request.execInCurDir
					? entry.getName()
					: entry.getArgumentName();

			// NOTE, if we leave the trailing slash on dir
			//       names (e.g. './subdir/'), then the server
			//       sends use names with double slashes '//'
			//       in the responses.
			//
			argName = CVSCUtilities.stripFinalSlash( argName );

			args.addElement( argName );
			}

		result = sendArguments( args );

		return result;
		}

	public boolean
	sendNotifies( CVSRequest request )
		{
		String		lastWDir = "";
		boolean		result = true;
		int			num = request.notifies.size();

		for ( int i = 0 ; result && i < num ; ++i )
			{
			CVSNotifyItem notify =
				(CVSNotifyItem) request.notifies.elementAt(i);

			String dir = notify.getWorkingDirectory();
			if ( dir.endsWith( "/" ) )
				dir = dir.substring( 0, dir.length() - 1 );

			if ( ! lastWDir.equals( dir ) )
				{
				lastWDir = dir;
				result = this.sendLine( "Directory ." );
				result = this.sendLine( notify.getRepository() );
				}

			result = this.sendLine( "Notify " + notify.getName() );

			if ( result ) 
				result = this.sendLine( notify.getServerExtra() );
			}

		return result;
		}

	public CVSResponse
	buildErrorResponse( CVSRequest request, CVSResponse response, String message )
		{
		response.setStatus( CVSResponse.ERROR );

		response.appendStderr
			( "The CVS Request failed.\n" );

		if ( message.length() > 0 )
			{
			response.appendStderr( message + "\n" );
			}

		if ( this.getReason().length() > 0 )
			{
			response.appendStderr( this.getReason() + "\n" );
			}

		CVSTracer.traceIf( request.traceRequest,
			"CVSClient.buildErrorReponse: " + response.getStderr() );
		
		return response;
		}

	public boolean
	performLogin( CVSRequest request )
		{
		CVSTracer.traceIf( request.traceRequest,
			"AUTHENTICATE: verifyOnly? '"
				+ request.verificationOnly + "' userName '"
				+ request.getUserName() + "' password '"
				+ request.getPassword() + "'" );

		this.sendLine(
			"BEGIN "
			+ ( request.verificationOnly ? "VERIFICATION" : "AUTH" )
			+ " REQUEST" );

		this.sendLine( request.getRootDirectory() );
		this.sendLine( request.getUserName() );
		this.sendLine( request.getPassword() );

		this.sendLine(
			"END "
			+ ( request.verificationOnly ? "VERIFICATION" : "AUTH" )
			+ " REQUEST" );

		String reply = this.readLine();

		CVSTracer.traceIf( request.traceRequest,
			"AUTHENTICATE: REPLY: '" + reply + "'" );

		if ( reply != null )
			if ( reply.startsWith( "I LOVE YOU" ) )
				return true;

		if ( reply != null && reply.length() > 0 )
			{
			this.setReason( reply );
			}

		return false;
		}

	public boolean
	requestValidRequests( CVSRequest request )
		{
		boolean result = true;

		request.validRequests = null;
		request.useUnchanged = false;
		request.useDirectory = true;

		this.sendLine( "valid-requests" );

		// REVIEW
		// Should we clone the request and work with a copy?!
		//
		boolean saveQueue = request.queueResponse;
		request.queueResponse = true;

		CVSResponse validResponse = new CVSResponse();

		this.readAndParseResponse( request, validResponse );

		request.queueResponse = saveQueue;

		if ( validResponse.getStatus() == CVSResponse.OK )
			{
			CVSResponseItem item =
				validResponse.getFirstItemByType
					( CVSResponseItem.VALID_REQUESTS );

			if ( item == null )
				{
				CVSTracer.traceIf( false,
					"REQUEST-VALID-REQUESTS: NO VALID-REQUESTS ITEM!!" );
				}
			else
				{
				String valids = item.getValidRequests();
				request.validRequests = valids;

				int index;

				index = valids.indexOf( "Directory" );
				if ( index >= 0 )
					{
					request.useDirectory = true;
					}
				else
					{
					result = false;
					CVSTracer.traceIf( true,
						"WARNING: This server does not support "
						+ "the 'Directory' request.\n"
						+ "jCVS will not operate properly with this server.\n"
						+ "Please update your cvs server to release "
						+ "1.9 or later." );
					}

				index = valids.indexOf( "UseUnchanged" );
				if ( index >= 0 )
					{
					request.useUnchanged = true;
					}
				}
			}
		else
			{
			request.useDirectory = true;
			CVSTracer.traceIf( true,
				"Recevied an error from the cvs server while\n"
				+ "requesting 'valid-requests'. This is not a good sign.\n\n"
				+ validResponse.getStdout() + "\n" + validResponse.getStderr() );
			}

		return result;
		}

	/**
	 * This method is the <em>heart</em> of the CVSClient class.
	 * Given a CVSRequest, this method will perform all of the
	 * processing required to open the connection, authenticate,
	 * send all requests, read all responses, and package the
	 * responses into a CVSResponse object, which is returned
	 * as the result of this method. The result is guaranteed
	 * to not be null, and will have its status set to indicate
	 * the status of the reuest. The resulting response object
	 * should be handed into the CVSProject's processCVSResponse()
	 * method to process the server's reponses on the local
	 * project contents.
	 *
	 * @param request The CVSRequest describing our request.
	 */
	public CVSResponse
	processCVSRequest( CVSRequest request )
		{
		return this.processCVSRequest( request, new CVSResponse() );
		}

	public boolean
	isCanceled()
		{
		synchronized ( this.canLock )
			{
			return this.canceled;
			}
		}

	public void
	setCanceled( boolean can )
		{
		synchronized ( this.canLock )
			{
			this.canceled = can;
			}
		}

	public boolean
	checkForCancel( CVSResponse response )
		{
		if ( this.isCanceled() )
			{
			response.setStatus( CVSResponse.ERROR );
			response.appendStderr
				( "\n*** The CVS request was canceled.\n" );
			if ( this.serverIsOpen )
				this.closeServer();
			return true;
			}
		else
			{
			return false;
			}
		}

	public CVSResponse
	processCVSRequest( CVSRequest request, CVSResponse response )
		{
		this.setCanceled( false );

		boolean				isok = true;
		CVSEntryVector		entries;
		CVSArgumentVector	arguments;
		CVSArgumentVector	globalargs;

		String[] vars = request.getSetVariables();

		this.usingGZIP = false;
		this.setReason( "" );
		this.recentEntryRepository = "";
		this.dirHash = new Hashtable();

		CVSUserInterface ui = request.getUserInterface();

		// EH-null-ui  Etienne-Hugues Fortin <ehfortin@sympatico.ca>
		if ( ui == null )
			{
			ui = this.new NullCVSUI();
			}

		this.tracingTCPData = request.traceTCPData;

		entries = request.getEntries();
		arguments = request.getArguments();
		globalargs = request.getGlobalArguments();

		if ( request.traceRequest )
		{
		CVSTracer.traceIf( true,
				"========================"
				+ " CVSClient.processCVSRequest "
				+ "========================" );
		CVSTracer.traceIf( true,
				"   Command:        " + request.getCommand() );
		CVSTracer.traceIf( true,
				"   Repository:     " + request.getRepository() );
		CVSTracer.traceIf( true,
				"   RootRepository: " + request.getRootRepository() );
		CVSTracer.traceIf( true,
				"   CVSServer:      "
				+ request.getPort() + "@" + request.getHostName() );
		CVSTracer.traceIf( true,
				"   RootDirectory:  " + request.getRootDirectory() );
		CVSTracer.traceIf( true,
				"   LocalDirectory: " + request.getLocalDirectory() );
		CVSTracer.traceIf( true,
				"   Connect Method: " +
					( request.getConnectionMethod()==CVSRequest.METHOD_RSH
						? "RSH"
						: ( request.getConnectionMethod()==CVSRequest.METHOD_SSH
							? "SSH"
							: "INETD" ) ) );
		CVSTracer.traceIf( true,
				"   Rsh Command:    " + request.getRshProcess() );
		CVSTracer.traceIf( true,
				"   Server Command: " + request.getServerCommand() );
		CVSTracer.traceIf( true,
				"   isPServer?      '"
				+ (request.isPServer()?"true ":"false") + "'"
				+ "   user '" + request.getUserName() + "'"
				+ "   pass '" + request.getPassword() + "'" );
		CVSTracer.traceIf( true,
				"   There are "
				+ (vars==null ? "no" : (""+vars.length))
				+ " user set variables." );
		CVSTracer.traceIf( true,
				"   NumEntries:      "
				+ (entries==null ? 0 : entries.size())
				+ "        NumArguments:     "
				+ (arguments==null ? 0 : arguments.size()) );
		CVSTracer.traceIf( true,
				"   GlobalOptions:   "
				+ (globalargs==null ? 0 : globalargs.size())
				+ "        GzipStreamLevel:  "
				+ request.getGzipStreamLevel() );
		CVSTracer.traceIf( true,
				"   redirectOutput  '"
				+ (request.redirectOutput?"true ":"false") + "'"
				+ "   execInCurDir    '"
				+ (request.execInCurDir?"true ":"false") + "'" );
		CVSTracer.traceIf( true,
				"   sendEntries     '"
				+ (request.sendEntries?"true ":"false") + "'"
				+ "   sendEntryfiles  '"
				+ (request.sendEntryFiles?"true ":"false") + "'" );
		CVSTracer.traceIf( true,
				"   sendModifieds   '"
				+ (request.sendModifieds?"true ":"false") + "'"
				+ "   sendEmptyMods   '"
				+ (request.sendEmptyMods?"true ":"false") + "'"	);
		CVSTracer.traceIf( true,
				"   sendArguments   '"
				+ (request.sendArguments?"true ":"false") + "'"
				+ "   ignoreResult    '"
				+ (request.ignoreResult?"true ":"false") + "'" );
		CVSTracer.traceIf( true,
				"   sendModule      '"
				+ (request.sendModule?"true ":"false") + "'"
				+ "   allowOverWrites '"
				+ (request.allowOverWrites?"true ":"false") + "'" );
		CVSTracer.traceIf( true,
				"   displayReponse  '"
				+ (request.displayReponse?"true ":"false") + "'"
				+ "   handleUpdated   '"
				+ (request.handleUpdated?"true ":"false") + "'" );
		CVSTracer.traceIf( true,
				"   handleMerged    '"
				+ (request.handleMerged?"true ":"false") + "'"
				+ "   handleCopyFile  '"
				+ (request.handleCopyFile?"true ":"false") + "'" );
		CVSTracer.traceIf( true,
				"   handleEntries   '"
				+ (request.handleEntries?"true ":"false") + "'"
				+ "   handleFlags     '"
				+ (request.handleFlags?"true ":"false") + "'" );
		CVSTracer.traceIf( true,
				"   queueResponse   '"
				+ (request.queueResponse?"true ":"false") + "'"
				+ "   responseHandler '"
				+ (request.responseHandler==null?"null ":
					request.responseHandler.getClass().getName()) + "'" );
		CVSTracer.traceIf( true,
				"   includeNotifies '"
				+ (request.includeNotifies?"true ":"false") + "'"
				+ "   notifiesSize    '"
				+ (request.notifies == null
					? "null" : (""+request.notifies.size()) ) + "'" );

		CVSTracer.traceIf( request.traceRequest,
				"***************************************"
					+ "**************************************" );
		} // if ( request.traceRequest )

		// SPECIAL HACKS
		//
		// For the "ci" command (commit), if the user has provided
		// the '-f' option, then we need to force all files to go
		// up as 'Modified'.
		//
		if ( "ci".equals( request.getCommand() ) )
			{
			if ( request.getArguments().containsArgument( "-f" ) )
				{
				CVSTracer.traceIf( request.traceRequest,
					"SPECIAL CASE: Forcing all files to be "
					+ "'Modified' for '-f' commit." );
				request.forceModifieds = true;
				}
			}

		int portNum = request.getPort();

		if ( portNum == 0 )
			{
			CVSTracer.traceIf( request.traceRequest,
				"CVSRequest: default port number to '" + this.port + "'" );
			portNum = this.port;
			}

		if ( this.checkForCancel( response ) )
			{
			return response;
			}

		CVSTracer.traceIf( request.traceRequest,
			"CVSRequest: opening server..." );

		ui.uiDisplayProgressMsg
			( "Opening server '" + request.getPort()
				+ "@" + request.getHostName() + "'..." );

		isok = this.openServer( request );

		CVSTracer.traceIf( request.traceRequest,
			"CVSRequest: server is " + (isok?"":"not ") + "open." );

		if ( ! isok )
			{
			String why = this.getReason();

			this.buildErrorResponse
				( request, response,
					"Failed to open socket to connect to cvs server '"
					+ request.getPort() + "@" + request.getHostName()
					+ "'.\n" + why );

			ui.uiDisplayProgressMsg
				( "Failed to open '" + request.getPort()
					+ "@" + request.getHostName() + "'." );

			return response;
			}

		if ( this.checkForCancel( response ) )
			{
			return response;
			}

		if ( request.isPServer() )
			{
 			if ( request.getUserName() == null
					|| request.getPassword() == null )
				{
				this.buildErrorResponse
					( request, response,
						"Attempted to connect to a password"
						+ " cvs server without a "
						+ ( (request.getUserName() == null)
							? "username" : "password" )
						+ ".\n" );

				ui.uiDisplayProgressMsg
					( "Incomplete login. Request canceled." );

				return response;
				}

			ui.uiDisplayProgressMsg
				( "Authenticating '" + request.getUserName()
					+ "@" + request.getHostName() + "'..." );

			if ( ! this.performLogin( request ) )
				{
				this.buildErrorResponse
					( request, response,
						"Failed authentication with the user name '"
						+ request.getUserName()
						+ "'.\n" );

				ui.uiDisplayProgressMsg
					( "Authentication of '" + request.getUserName()
						+ "@" + request.getHostName() + "' failed." );

				return response;
				}
			}

		if ( request.verificationOnly )
			{
			String authResultStr = 
				"Authentication of '" + request.getUserName()
				+ "@" + request.getHostName() + "' succeeded.";
		
			ui.uiDisplayProgressMsg( authResultStr );
		
			response.setStatus( CVSResponse.OK );
			response.appendStderr( authResultStr );

			this.closeServer();

			return response;
			}

		if ( this.checkForCancel( response ) )
			{
			return response;
			}

		if ( isok )
			{
			isok = this.requestValidRequests( request );
			}

		CVSTracer.traceIf( request.traceRequest,
				"Valid Requests:  useUnchanged '"
				+ (request.useUnchanged?"true":"false") + "'"
				+ "   useDirectory '"
				+ (request.useDirectory?"true":"false") + "'" );

		if ( this.checkForCancel( response ) )
			{
			return response;
			}

		if ( isok && request.sendRootDirectory )
			{
			CVSTracer.traceIf( request.traceRequest,
				"CVSRequest: send root directory..." );

			// SPECIAL-CASE (?)
			// If there's no rootDirectory, then don't
			// send the command. This is only used by the
			// 'noop' used by the Test Connection dialog.
			//
			if ( request.getRootDirectory().length() > 0 )
				{
				isok = this.sendCVSRootDirectory( request );
				}
			}

		// Establish GzipStream is requested (level > 0).
		//
		if ( isok && request.gzipStreamLevel > 0
				&& request.validRequests != null
				&& request.validRequests.indexOf( "Gzip-stream" ) >= 0 )
			{
			CVSTracer.traceIf( request.traceRequest,
					"Utilitizing Gzip-stream mode at level 6." );
			this.usingGZIP = true;
			this.sendLine( "Gzip-stream 6" );
			this.instream = new InflaterInputStream( this.instream );
			this.outstream = new DeflaterOutputStream( this.outstream );
			}

		if ( isok )
			{
			isok = this.sendSetVariables( request );
			}

		ui.uiDisplayProgressMsg
			( "Negotiating cvs protocol..." );

		this.sendValidResponses( request, "" );

		ui.uiDisplayProgressMsg
			( "Sending command request, '" +request.getCommand()+ "'..." );

		if ( isok && request.allowGzipFileMode
				&& ( ! this.usingGZIP )
				&& request.validRequests != null
				&& request.validRequests.indexOf
					( "gzip-file-contents" ) >= 0 )
			{
			CVSTracer.traceIf( request.traceRequest,
				"Utilitizing gzip-file-contents mode at level 6." );

			this.sendLine( "gzip-file-contents 6" );
			request.gzipFileMode = true;
			}

		if ( this.checkForCancel( response ) )
			{
			return response;
			}

		CVSArgumentVector globalArgs = request.getGlobalArguments();
		if ( isok & globalArgs != null && globalArgs.size() > 0 )
			{
			isok = this.sendGlobalArguments( globalArgs );
			}

		if ( isok & request.notifies != null
				&& request.notifies.size() > 0 )
			{
			isok = this.sendNotifies( request );
			}

		// NOTE The "request.sendEntries" flag is not checked here!
		//      It is utilized inside sendCVSEntries().
		if ( isok )
			{
			CVSTracer.traceIf( request.traceRequest,
				"CVSRequest: send entries..." );
			isok = this.sendCVSEntries( request );
			}

		if ( this.checkForCancel( response ) )
			{
			return response;
			}

		if ( isok && request.sendRootDirectory )
			{
			if ( request.execInCurDir
					&& request.getDirEntry() != null )
				{
				// Set the 'current directory'...
				CVSTracer.traceIf( request.traceRequest,
					"CVSRequest: send 'current' directory..." );
				this.recentEntryRepository = ""; // make sure it goes...
				isok =
					this.sendEntryRepository
						( request, request.getDirEntry() );
				}
			else
				{
				// Reset the 'current directory' to the top level...
				CVSTracer.traceIf( request.traceRequest,
					"CVSRequest: send root repository..." );

				isok = this.sendRootRepository( request );
				}
			}

		if ( this.checkForCancel( response ) )
			{
			return response;
			}

		if ( isok && request.sendArguments )
			{
			CVSTracer.traceIf( request.traceRequest,
					"CVSRequest: send arguments..." );
			isok = this.sendArguments( request.getArguments() );
			}

		if ( this.checkForCancel( response ) )
			{
			return response;
			}

		if ( isok && request.sendEntryFiles )
			{
			CVSTracer.traceIf( request.traceRequest,
					"CVSRequest: send files..." );
			isok = this.sendEntriesArguments( request );
			}

		if ( this.checkForCancel( response ) )
			{
			return response;
			}

		if ( isok && request.sendModule )
			{
			CVSTracer.traceIf( request.traceRequest,
					"CVSRequest: send module name..." );
			isok = this.sendCVSModule( request );
			}

		if ( isok )
			{
			CVSTracer.traceIf( request.traceRequest,
				"CVSRequest: send command '"
				+ request.getCommand() + "'" );

			isok = this.sendLine( request.getCommand() );
			}

		if ( this.checkForCancel( response ) )
			{
			return response;
			}

		try {
			if ( usingGZIP )
				{
				((DeflaterOutputStream) this.outstream).finish();

				// SW-flush-output
				// Since out GZIP stream was wrapped in a BufferedOutputStream
				// we need to flush() to move it all out.
				//
				outstream.flush();
				}
			}
		catch ( IOException ex )
			{ ex.printStackTrace(); }

		if ( isok )
			{
			CVSTracer.traceIf( request.traceRequest,
				"CVSRequest: reading response..." );

			ui.uiDisplayProgressMsg
				( "Reading server response..." );

			this.readAndParseResponse( request, response );
			}
		else
			{
			CVSTracer.traceIf( request.traceRequest,
				"CVSRequest: Error sending command request." );

			ui.uiDisplayProgressMsg
				( "Error sending command request..." );

			this.buildErrorResponse
					( request,  response,
						"during processing of cvs request" +
						( (this.getReason().length() < 1)
							? ""
							: ( ": {" + this.getReason() + "}" ) ) );
			}

		ui.uiDisplayProgressMsg
			( "Closing CVS server connection." );

		this.closeServer();

		if ( this.checkForCancel( response ) )
			{
			return response;
			}

		CVSTracer.traceIf( request.traceRequest,
			"**====================================="
				+ "====================================**" );

		ui.uiDisplayProgressMsg
			( "Command completed with '"
				+ ( response.getStatus() == CVSResponse.OK
					? "ok" : "error" )
				+ "' status." );

		return response;
		}

	private String
	generateTempName()
		{
		this.tempCounter++;

		String	randStr =
			Long.toHexString( this.tempCounter % 0x0FFFFFFF );

		if ( randStr.length() > 7 )
			{
			randStr = randStr.substring( randStr.length() - 7 );
			}

		String result = "T" + randStr + ".cvs";

		CVSTracer.traceIf( false,
			"TEMPFILE: counter '" + this.tempCounter
			+ "' name '" + result + "'" );

		return result;
		}

	public String
	generateTempPath()
		{
		String	path = null;

		for ( ; ; )
			{
			path = this.tempPath + "/"
					+ this.generateTempName();

			File tFile = new File( path );

			if ( ! tFile.exists() )
				break;
			
			if ( true )
			CVSTracer.traceWithStack(
				"CVSClient.generateTempPath: ERROR '"
					+path+ "' exists!" );
			}

		return path;
		}

	private boolean
	requestIsQueued( CVSRequest request )
		{
		return ( request.queueResponse ||
					request.responseHandler == null );
		}

	private boolean
	processResponseItem(
			CVSRequest request, CVSResponse response, CVSResponseItem item )
		{
		boolean result = true;

		// NOTE
		// SPECIAL CASE
		// We need to handle the local directories returned when we use
		// the "exec in directory" feature. This is because the  paths
		// being returned are relative to this directory, and correcting
		// the path here is the simplest and best way to fix it.
		//
		if ( request.execInCurDir
				&& request.getDirEntry() != null )
			{
			String itemPath = item.getPathName();
			String pfxPath = request.getDirEntry().getLocalPathName();

			if ( itemPath != null )
				{
				if ( itemPath.startsWith( "./" ) )
					itemPath = pfxPath + itemPath.substring(2);
				else
					itemPath = pfxPath + itemPath;

				item.setPathName( itemPath );
				}
			}

		if ( this.requestIsQueued( request ) )
			{
			response.addResponseItem( item );
			}
		else
			{
			result =
				request.responseHandler.handleResponseItem
					( request, response, item );
			}

		return result;
		}

	public CVSResponse
	readAndParseResponse( CVSRequest request, CVSResponse response )
		{
		boolean		isok;
		int			status = CVSResponse.OK;
		boolean		gotStatus = false;
		int			fileSize;
		int			index;
		String		line = null;

		CVSResponseItem	currItem = null;

		for ( isok = true ; isok ; )
			{
			if ( this.isCanceled() )
				break;

			line = this.readLine();
			
			if ( line == null )
				{
				CVSTracer.traceIf( request.traceResponse, 
					"PARSE: End of file on input stream." );
				break;
				}

			CVSTracer.traceIf( false,
				"CVSClient.readAndParseResponse: INLINE '"
				+ line + "' currItem '"
				+ ( currItem==null
					? "(null)" : currItem.toString() ) + "'" );

			if ( currItem != null )
				{
				int itemType = currItem.getType();

				if ( currItem.getAddState() == CVSResponseItem.GET_FULL_PATH )
					{
					CVSTracer.traceIf( request.traceResponse,
						"PARSE: FullPath '" +line+ "'" );

					if ( line.endsWith( "/./" ) )
						{
						// SPECIAL CASE
						// When the user does something like "-d ." on
						// an update or checkout, we will get repository
						// names that look like "/usr/cvsroot/path/./".
						// This confuses our code, so we adjust here...
						line = line.substring( 0, line.length() - 2 );
						CVSTracer.traceIf( request.traceResponse,
							"PARSE: Adjusted FullPath '" +line+ "'" );
						}

					currItem.setRepositoryName( line );
					}
				else
				if ( currItem.getAddState() == CVSResponseItem.GET_ENTRIES_LINE )
					{
					CVSTracer.traceIf( request.traceResponse,
						"PARSE: Entry '" +line+ "'" );
					currItem.setEntriesLine( line );
					}
				else
				if ( currItem.getAddState() == CVSResponseItem.GET_MODE_LINE )
					{
					CVSTracer.traceIf( request.traceResponse,
						"PARSE: Mode '" +line+ "'" );
					currItem.setModeLine( line );
					}
				else
				if ( currItem.getAddState() == CVSResponseItem.GET_TAG_SPEC )
					{
					CVSTracer.traceIf( request.traceResponse,
						"PARSE: Tag Spec '" +line+ "'" );
					currItem.setTagSpec( line );
					}
				else
				if ( currItem.getAddState() == CVSResponseItem.GET_PROGRAM )
					{
					CVSTracer.traceIf( request.traceResponse,
						"PARSE: Program Name '" +line+ "'" );
					currItem.setProgram( line );
					}			   
				else
				if ( currItem.getAddState() == CVSResponseItem.GET_NEW_NAME )
					{
					CVSTracer.traceIf( request.traceResponse,
						"PARSE: New Name '" +line+ "'" );
					currItem.setNewName( line );
					}			   

				switch ( itemType )
					{
					case CVSResponseItem.CREATED:
					case CVSResponseItem.MERGED:
					case CVSResponseItem.PATCHED:
					case CVSResponseItem.UPDATED:
					case CVSResponseItem.UPDATE_EXISTING:
						String itemCmdName =
							( itemType == CVSResponseItem.CREATED
							? "Created"
							: ( itemType == CVSResponseItem.MERGED
							? "Merged"
							: ( itemType == CVSResponseItem.PATCHED
							? "Patched"
							: ( itemType == CVSResponseItem.UPDATED
							? "Updated"
							: "Update-existing" ) ) ) );
						 
						switch ( currItem.getAddState() )
							{
							case CVSResponseItem.GET_FULL_PATH:
								currItem.setAddState
									( CVSResponseItem.GET_ENTRIES_LINE );
								break;

							case CVSResponseItem.GET_ENTRIES_LINE:
								currItem.setAddState
									( CVSResponseItem.GET_MODE_LINE );
								break;

							case CVSResponseItem.GET_MODE_LINE:
								currItem.setAddState
									( CVSResponseItem.GET_FILE );

								File file = new
									File( this.generateTempPath() );
								
								String name = currItem.getRepositoryName();
								index = name.lastIndexOf( '/' );
								if ( index >= 0 )
									name = name.substring( index + 1 );
								name = currItem.getPathName() + name;
								
								// Only display this when queue-ing, since
								// the processing typically follows immediately
								// with its own message...
								if ( this.requestIsQueued( request ) )
									request.getUserInterface().uiDisplayProgressMsg
										( "Downloading file '" + name + "'..." );

								if ( this.retrieveFile( currItem, file ) )
									{
									currItem.setFile( file );
									isok = this.processResponseItem
										( request, response, currItem );
									}
								else
									{
									response.appendStdErr
										( "ERROR downloading '" + itemCmdName
										+ "' file '" + name
										+ "'\n      into temporary file '"
										+ file.getPath() + "'.\n" );
									response.appendStdErr
										( "REASON "	+ this.getReason() + "\n" );

									status = CVSResponse.ERROR;
									}

								currItem = null;
								break;
							}
						break;

					case CVSResponseItem.CHECKED_IN:
					case CVSResponseItem.NEW_ENTRY:
						if ( currItem.getAddState()
								== CVSResponseItem.GET_FULL_PATH )
							{
							currItem.setAddState
								( CVSResponseItem.GET_ENTRIES_LINE );
							}
						else
							{
							isok = this.processResponseItem
								( request, response, currItem );
							currItem = null;
							}
						break;

					case CVSResponseItem.COPY_FILE:
						if ( currItem.getAddState()
								== CVSResponseItem.GET_FULL_PATH )
							{
							currItem.setAddState
								( CVSResponseItem.GET_NEW_NAME );
							}
						else
							{
							isok = this.processResponseItem
								( request, response, currItem );
							currItem = null;
							}
						break;

					case CVSResponseItem.SET_STICKY:
						if ( currItem.getAddState()
								== CVSResponseItem.GET_FULL_PATH )
							{
							currItem.setAddState
								( CVSResponseItem.GET_TAG_SPEC );
							}
						else
							{
							isok = this.processResponseItem
								( request, response, currItem );
							currItem = null;
							}
						break;

					case CVSResponseItem.NOTIFIED:
					case CVSResponseItem.REMOVED:
					case CVSResponseItem.REMOVE_ENTRY:

					case CVSResponseItem.SET_CHECKIN_PROG:
					case CVSResponseItem.SET_UPDATE_PROG:

					case CVSResponseItem.CLEAR_STICKY:
					case CVSResponseItem.SET_STATIC_DIR:
					case CVSResponseItem.CLEAR_STATIC_DIR:

						isok = this.processResponseItem
							( request, response, currItem );
						currItem = null;

						break;

					default:
						CVSLog.logMsg
							( "PARSE: ERROR unknown currentItem type '"
								+ currItem.getType() + "'" );
						break;
					}
				}
			else if ( line.startsWith( "ok" ) )
				{
				CVSTracer.traceIf( request.traceResponse, "PARSE: ok" );
				response.setStatus( CVSResponse.OK );
				gotStatus = true;
				break;
				}
			else if ( line.startsWith( "error" ) )
				{
				CVSTracer.traceIf
					( request.traceResponse, "PARSE: error '" +line+ "'" );
				
				gotStatus = true;

				String errCodeStr = "";
				String errTextStr = "";

				if ( line.length() > 6 )
					{
					line = line.substring(6);

					if ( line.startsWith( " " ) )
						{
						errCodeStr = "";
						errTextStr = line.substring(1);
						}
					else
						{
						index = line.indexOf( ' ' );
						if ( index > 0 )
							{
							errCodeStr = line.substring( 0, index );
							errTextStr = line.substring( index + 1 );
							}
						else
							{
							errCodeStr = "";
							errTextStr = line;
							}
						}
					}

				response.setErrorStatus( errCodeStr, errTextStr );

				break;
				}
			else if ( line.startsWith( "I LOVE YOU" ) )
				{
				// NOTE
				// We pick up these here, since there might be a
				// case where we failed to recognize we're a pserver?
				CVSLog.logMsg
					( "PARSE: GOT LOVE MESSAGE '" +line+ "'" );
				// continue on, since we are loved
				}
			else if ( line.startsWith( "I HATE YOU" ) )
				{
				// NOTE
				// We pick up these here, since there might be a
				// case where we failed to recognize we're a pserver?
				CVSLog.logMsg
					( "PARSE: GOT HATE MESSAGE '" +line+ "'" );

				gotStatus = true;
				response.setErrorStatus
					( "-1", "INVALID LOGIN" );

				// We SHOULD break, since there may never be more data... 
				break;
				}
			else if ( line.startsWith( "Updated " ) )
				{
				String pathName = line.substring( 8 );
				CVSTracer.traceIf( request.traceResponse, 
					"PARSE: Update '" +pathName+ "'" );

				CVSResponseItem newItem = new
					CVSResponseItem( CVSResponseItem.UPDATED );

				newItem.setPathName( pathName );

				newItem.setAddState(
					request.useDirectory
						? CVSResponseItem.GET_FULL_PATH
						: CVSResponseItem.GET_ENTRIES_LINE );

				// UNDONE - its an ERROR IF currentItem is NOT NULL!!
				currItem = newItem;
				}
			else if ( line.startsWith( "Merged " ) )
				{
				String pathName = line.substring( 7 );
				CVSTracer.traceIf( request.traceResponse,
					"PARSE: Merged '" +pathName+ "'" );

				CVSResponseItem newItem = new
					CVSResponseItem( CVSResponseItem.MERGED );

				newItem.setPathName( pathName );

				newItem.setAddState(
					request.useDirectory
						? CVSResponseItem.GET_FULL_PATH
						: CVSResponseItem.GET_ENTRIES_LINE );

				currItem = newItem;
				}
			else if ( line.startsWith( "Update-existing " ) )
				{
				String pathName = line.substring( 16 );
				CVSTracer.traceIf( request.traceResponse,
					"PARSE: Update-existing '" +pathName+ "'" );

				CVSResponseItem newItem = new
					CVSResponseItem( CVSResponseItem.UPDATE_EXISTING );

				newItem.setPathName( pathName );

				newItem.setAddState(
					request.useDirectory
						? CVSResponseItem.GET_FULL_PATH
						: CVSResponseItem.GET_ENTRIES_LINE );

				currItem = newItem;
				}
			else if ( line.startsWith( "Created " ) )
				{
				String pathName = line.substring( 8 );
				CVSTracer.traceIf( request.traceResponse,
					"PARSE: Created '" +pathName+ "'" );

				CVSResponseItem newItem = new
					CVSResponseItem( CVSResponseItem.CREATED );

				newItem.setPathName( pathName );

				newItem.setAddState(
					request.useDirectory
						? CVSResponseItem.GET_FULL_PATH
						: CVSResponseItem.GET_ENTRIES_LINE );

				currItem = newItem;
				}
			else if ( line.startsWith( "Patched " ) )
				{
				String pathName = line.substring( 8 );
				CVSTracer.traceIf( request.traceResponse,
					"PARSE: Patched '" +pathName+ "'" );

				CVSResponseItem newItem = new
					CVSResponseItem( CVSResponseItem.PATCHED );

				newItem.setPathName( pathName );

				newItem.setAddState(
					request.useDirectory
						? CVSResponseItem.GET_FULL_PATH
						: CVSResponseItem.GET_ENTRIES_LINE );

				currItem = newItem;
				}
			else if ( line.startsWith( "Checksum " ) )
				{
				String sumStr = line.substring( 9 );
				CVSTracer.traceIf( request.traceResponse, 
					"PARSE: Checksum '" +sumStr+ "'" );

				CVSResponseItem newItem = new
					CVSResponseItem( CVSResponseItem.CHECKSUM );

				newItem.setChecksum( sumStr );

				isok = this.processResponseItem
					( request, response, newItem );
				}
			else if ( line.startsWith( "Module-expansion " ) )
				{
				String pathName = line.substring( 17 );
				CVSTracer.traceIf( request.traceResponse, 
					"PARSE: Module-expansion '" +pathName+ "'" );

				CVSResponseItem newItem = new
					CVSResponseItem( CVSResponseItem.MODULE_EXPANSION );

				newItem.setPathName( pathName );
				//
				// UNDONE
				// REVIEW
				// If module-expansions send two line pathnames when
				// 'Directory' is in use, we will be out of sync!!!
				//
				isok = this.processResponseItem
					( request, response, newItem );
				}
			else if ( line.startsWith( "Notified " ) )
				{
				String pathName = line.substring( 9 );
				CVSTracer.traceIf( request.traceResponse, 
					"PARSE: Notified '" +pathName+ "'" );

				CVSResponseItem newItem = new
					CVSResponseItem( CVSResponseItem.NOTIFIED );

				newItem.setPathName( pathName );

				if ( request.useDirectory )
					{
					currItem = newItem;
					newItem.setAddState( CVSResponseItem.GET_FULL_PATH );
					}
				else
					{
					isok = this.processResponseItem
						( request, response, newItem );
					}
				}
			else if ( line.startsWith( "Removed " ) )
				{
				String pathName = line.substring( 8 );
				CVSTracer.traceIf( request.traceResponse, 
					"PARSE: Removed '" +pathName+ "'" );

				CVSResponseItem newItem = new
					CVSResponseItem( CVSResponseItem.REMOVED );

				newItem.setPathName( pathName );

				if ( request.useDirectory )
					{
					currItem = newItem;
					newItem.setAddState( CVSResponseItem.GET_FULL_PATH );
					}
				else
					{
					isok = this.processResponseItem
						( request, response, newItem );
					}
				}
			else if ( line.startsWith( "Remove-entry " ) )
				{
				String pathName = line.substring( 13 );
				CVSTracer.traceIf( request.traceResponse, 
					"PARSE: Remove-entry '" +pathName+ "'" );

				CVSResponseItem newItem = new
					CVSResponseItem( CVSResponseItem.REMOVE_ENTRY );

				newItem.setPathName( pathName );

				if ( request.useDirectory )
					{
					currItem = newItem;
					newItem.setAddState( CVSResponseItem.GET_FULL_PATH );
					}
				else
					{
					isok = this.processResponseItem
						( request, response, newItem );
					}
				}
			else if ( line.startsWith( "Checked-in " ) )
				{
				String pathName = line.substring( 11 );
				CVSTracer.traceIf( request.traceResponse, 
					"PARSE: Checked-in '" +pathName+ "'" );

				CVSResponseItem newItem = new
					CVSResponseItem( CVSResponseItem.CHECKED_IN );

				newItem.setPathName( pathName );

				newItem.setAddState(
					request.useDirectory
						? CVSResponseItem.GET_FULL_PATH
						: CVSResponseItem.GET_ENTRIES_LINE );

				currItem = newItem;
				}
			else if ( line.startsWith( "New-entry " ) )
				{
				String pathName = line.substring( 10 );
				CVSTracer.traceIf( request.traceResponse, 
					"PARSE: New-entry '" +pathName+ "'" );

				CVSResponseItem newItem = new
					CVSResponseItem( CVSResponseItem.NEW_ENTRY );

				newItem.setPathName( pathName );

				newItem.setAddState(
					request.useDirectory
						? CVSResponseItem.GET_FULL_PATH
						: CVSResponseItem.GET_ENTRIES_LINE );

				currItem = newItem;
				}
			else if ( line.startsWith( "Copy-file " ) )
				{
				String pathName = line.substring( 10 );
				CVSTracer.traceIf( request.traceResponse, 
					"PARSE: Copy-file '" +pathName+ "'" );

				CVSResponseItem newItem = new
					CVSResponseItem( CVSResponseItem.COPY_FILE );

				newItem.setPathName( pathName );

				newItem.setAddState(
					request.useDirectory
						? CVSResponseItem.GET_FULL_PATH
						: CVSResponseItem.GET_NEW_NAME );

				currItem = newItem;
				}
			else if ( line.startsWith( "Set-sticky " ) )
				{
				String pathName = line.substring( 11 );
				CVSTracer.traceIf( request.traceResponse, 
					"PARSE: Set-sticky '" +pathName+ "'" );

				CVSResponseItem newItem = new
					CVSResponseItem( CVSResponseItem.SET_STICKY );

				newItem.setPathName( pathName );

				newItem.setAddState(
					request.useDirectory
						? CVSResponseItem.GET_FULL_PATH
						: CVSResponseItem.GET_TAG_SPEC );

				currItem = newItem;
				}
			else if ( line.startsWith( "Clear-sticky " ) )
				{
				String pathName = line.substring( 13 );
				CVSTracer.traceIf( request.traceResponse, 
					"PARSE: Clear-sticky '" +pathName+ "'" );

				CVSResponseItem newItem = new
					CVSResponseItem( CVSResponseItem.CLEAR_STICKY );

				newItem.setPathName( pathName );

				if ( request.useDirectory )
					{
					currItem = newItem;
					newItem.setAddState( CVSResponseItem.GET_FULL_PATH );
					}
				else
					{
					isok = this.processResponseItem
						( request, response, newItem );
					}
				}
			else if ( line.startsWith( "Set-static-directory " ) )
				{
				String pathName = line.substring( 21 );
				CVSTracer.traceIf( request.traceResponse, 
					"PARSE: Set-static-directory '" +pathName+ "'" );

				CVSResponseItem newItem = new
					CVSResponseItem( CVSResponseItem.SET_STATIC_DIR );

				newItem.setPathName( pathName );

				if ( request.useDirectory )
					{
					currItem = newItem;
					newItem.setAddState( CVSResponseItem.GET_FULL_PATH );
					}
				else
					{
					isok = this.processResponseItem
						( request, response, newItem );
					}
				}
			else if ( line.startsWith( "Clear-static-directory " ) )
				{
				String pathName = line.substring( 23 );
				CVSTracer.traceIf( request.traceResponse, 
					"PARSE: Clear-static-directory '" +pathName+ "'" );

				CVSResponseItem newItem = new
					CVSResponseItem( CVSResponseItem.CLEAR_STATIC_DIR );

				newItem.setPathName( pathName );

				if ( request.useDirectory )
					{
					currItem = newItem;
					newItem.setAddState( CVSResponseItem.GET_FULL_PATH );
					}
				else
					{
					isok = this.processResponseItem
						( request, response, newItem );
					}
				}
			else if ( line.startsWith( "Set-checkin-prog " ) )
				{
				String pathName = line.substring( 17 );
				CVSTracer.traceIf( request.traceResponse, 
					"PARSE: Set-checkin-prog '" +pathName+ "'" );

				CVSResponseItem newItem = new
					CVSResponseItem( CVSResponseItem.SET_CHECKIN_PROG );

				newItem.setPathName( pathName );

				newItem.setAddState( CVSResponseItem.GET_PROGRAM );

				currItem = newItem;
				}
			else if ( line.startsWith( "Set-update-prog " ) )
				{
				String pathName = line.substring( 16 );
				CVSTracer.traceIf( request.traceResponse, 
					"PARSE: Set-update-prog '" +pathName+ "'" );

				CVSResponseItem newItem = new
					CVSResponseItem( CVSResponseItem.SET_UPDATE_PROG );

				newItem.setPathName( pathName );

				newItem.setAddState( CVSResponseItem.GET_PROGRAM );

				currItem = newItem;
				}
			// REVIEW should these two be passed to 'responseHandler'?
			else if ( line.startsWith( "E " ) )
				{
			//	response.appendStdErr( line.substring(2) + "\n" );
				if ( request.isRedirected() )
					{
					request.redirectLine( line.substring(2) );
					}
				else
					{
					response.appendStdErr( line.substring(2) + "\n" );
					}
				}
			else if ( line.startsWith( "M " ) )
				{
				if ( request.isRedirected() )
					{
					request.redirectLine( line.substring(2) );
					}
				else
					{
					response.appendStdOut( line.substring(2) + "\n" );
					}
				}
			else if ( line.startsWith( "Valid-requests " ) )
				{
				CVSResponseItem newItem = new
					CVSResponseItem( CVSResponseItem.VALID_REQUESTS );

				newItem.setValidRequests( line.substring(15) );
				
				isok = this.processResponseItem
					( request, response, newItem );
				}
			else
				{
				response.appendStdErr
					( "WARNING: stray line:\n   '" +line+ "'\n" );
				}
			}


		if ( ! this.isCanceled() )
			{
			if ( gotStatus )
				{
				if ( status != CVSResponse.OK )
					{
					response.setStatus( CVSResponse.ERROR );
					}
				}
			else
				{
				response.appendStdErr(
					"\n" + this.getReason()
					+ "\nShort response, no status response from server.\n" );
				response.setStatus( CVSResponse.ERROR );
				}
			}

		return response;
		}

	public boolean
	retrieveFile( CVSResponseItem item, File file )
		{
		boolean	ok = true;
		boolean	use_gzip = false;

		FileOutputStream	out = null;

		int fileSize = 0;
		int bytes = 0;
		int length;

		String line = null;

		line = this.readLine();

		if ( line == null )
			{
			this.setReason
				( "CVSClient.retrieveFile: ERROR size line is null!" );
			CVSLog.logMsg( this.getReason() );
			ok = false;
			}
		
		if ( ok )
			{
			if ( line.startsWith( "z" ) )
				{
				item.setGZIPed( true );
				line = line.substring( 1 );
				}

			try {
				fileSize = Integer.valueOf( line ).intValue();
				}
			catch ( NumberFormatException ex )
				{
				this.setReason
					( "CVSClient.retrieveFile: ERROR size line is invalid '"
						+ line + "'" );
				CVSLog.logMsg( this.getReason() );
				ok = false;
				}
			}

		if ( CVSClient.LIMIT_FILE_SIZE )
			{
			if ( ok )
				{
				if ( fileSize > CVSClient.MAX_FILE_SIZE )
					{
					this.setReason
						( "CVSClient.retrieveFile: ERROR size limit of '"
							+ CVSClient.MAX_FILE_SIZE + "' exceeded by '"
							+ fileSize + "'" );
					CVSLog.logMsg( this.getReason() );
					ok = false;
					}
				}
			}

		if ( ok )
			{
			try {
				out = new FileOutputStream( file );
				}
			catch ( IOException ex )
				{
				this.setReason
					( "CVSClient.retrieveFile: ERROR opening output file '"
						+ file.getPath() + "'\n    " + ex.getMessage() );
				CVSLog.logMsg( this.getReason() );
				
				// NOTE we do not set 'ok = false;' here, as we need
				// to process the downcoming data!!! So, we will have
				// to overload with 'out == null' for closure.
				}
			}

		if ( ok )
			{
			int i;
			byte[]	buffer;
			buffer = new byte[8192];

			for ( length = fileSize ; length > 0 ; )
				{
				bytes = (length > 8192 ? 8192 : length);

				try {
					bytes = this.instream.read( buffer, 0, bytes );
					}
				catch ( IOException ex )
					{
					ok = false;
					this.setReason
						( "CVSClient.retrieveFile: "
							+ "ERROR reading file data:\n   "
							+ ex.getMessage() );
					CVSLog.logMsg( this.getReason() );
					break;
					}

				if ( bytes < 0 )
					break;

				length -= bytes;

				if ( out != null )
					{
					try { out.write( buffer, 0, bytes ); }
					catch ( IOException ex )
						{
						ok = false;
						this.setReason
							( "CVSClient.retrieveFile: "
								+ "ERROR writing output file:\n   "
								+ ex.getMessage() );
						CVSLog.logMsg( this.getReason() );

						try { out.close(); }
							catch ( IOException ex2 ) { }

						out = null;

						break;
						}
					}

				if ( this.isCanceled() )
					break;
				}

			if ( out != null )
				{
				// Do NOT set out to null here! See NOTE above.
				try { out.close(); }
				catch ( IOException ex )
					{
					this.setReason
						( "CVSClient.retrieveFile: "
							+ "ERROR closing output file:\n   "
							+ ex.getMessage() );
					CVSLog.logMsg( this.getReason() );
					ok = false;
					}
				}
			}	

		// If out is null, an error occurred!
		return ( ( ok && (out != null) ) ? true : false );
		}

	public boolean
	sendFileContents( InputStream in )
		{
		int			bytes;
		boolean		result = true;
		byte[]		buffer = new byte[ 16 * 1024 ];

		for ( ; result ; )
			{
			try {
				bytes = in.read( buffer, 0, buffer.length );
				}
			catch ( IOException ex )
				{
				result = false;
				this.setReason
					( "sendFileRaw: ERROR reading input file: "
						+ ex.getMessage() );
				CVSLog.logMsg( this.getReason() );
				break;
				}

			if ( bytes < 0 )
				break;

			try {
				this.outstream.write( buffer, 0, bytes );
				}
			catch ( IOException ex )
				{
				result = false;
				this.setReason
					( "sendFileRaw: ERROR writing file data: "
						+ ex.getMessage() );
				CVSLog.logMsg( this.getReason() );
				break;
				}

			if ( this.isCanceled() )
				break;
			}

		try { this.outstream.flush(); }
		catch ( IOException ex )
			{
			result = false;
			this.setReason
				( "sendFileRaw: ERROR flushing server connection: "
					+ ex.getMessage() );
			CVSLog.logMsg( this.getReason() );
			}

		try { in.close(); }
		catch ( IOException ex )
			{
			result = false;
			this.setReason
				( "sendFileRaw: ERROR closing input file: "
					+ ex.getMessage() );
			CVSLog.logMsg( this.getReason() );
			}

		return result;
		}

	public boolean
	sendFileRaw( CVSEntry entry, File entryFile, boolean useGzipFile )
		{
		int			bytes;
		Long		sizeLong;
		long		fileSize, length;
		boolean		result = true;
		boolean		usingGzip = false;
		File		gzipFile = null;
		BufferedInputStream	in = null;
		byte[]		buffer = new byte[ 16 * 1024 ];

		fileSize = entryFile.length();
		long beginMillis = System.currentTimeMillis();

		try {
			in = new BufferedInputStream(
					new FileInputStream( entryFile ) );
			}
		catch ( IOException ex )
			{
			result = false;
			}

		usingGzip = (useGzipFile && (fileSize > MIN_GZIP_SIZE));

		if ( result && usingGzip )
			{
			try {
				gzipFile = new File( this.generateTempPath() );
				
				BufferedOutputStream out =
					new BufferedOutputStream
						( new GZIPOutputStream
							( new FileOutputStream( gzipFile ) ) );

				for ( ; ; )
					{
					bytes = in.read( buffer, 0, buffer.length );
					if ( bytes < 0 )
						break;
					
					out.write( buffer, 0, bytes );

					if ( this.isCanceled() )
						break;
					}

				in.close();
				out.close();

				in = new BufferedInputStream(
						new FileInputStream( gzipFile ) );

				fileSize = gzipFile.length();
				}
			catch ( IOException ex )
				{
				ex.printStackTrace( System.err );
				result = false;
				}
			}

		long endMillis = System.currentTimeMillis();

		if ( false )
			System.err.println
				( "CVSClient.sendFileRaw: TIME = '"
					+ (endMillis - beginMillis) + "' millis." );

		if ( result )
			{
			sizeLong = new Long( fileSize );
			String sizeStr = sizeLong.toString();

			if ( usingGzip )
				sizeStr = "z" + sizeStr;

			result = this.sendLine( sizeStr );
			if ( result )
				{
				result = this.sendFileContents( in );
				}
			else
				{
				this.setReason
					( "sendFileRaw: ERROR writing file size: "
						+ this.getReason() );
				CVSLog.logMsg( this.getReason() );
				}
			}

			
		if ( usingGzip && gzipFile != null && gzipFile.exists() )
			{
			try { gzipFile.delete(); }
			catch ( SecurityException ex )
				{
				// we will leave result, since upload has succeeded...
				CVSLog.logMsg
					( "sendFileRaw: WARNING deleting temp file: "
						+ ex.getMessage() );
				}
			}

		return result;
		}

	public boolean
	sendFileAscii( CVSEntry entry, File entryFile, boolean gzipFileMode )
		{
		String			inLine;
		File			tempFile;
		BufferedReader	in = null;
		BufferedOutputStream	out = null;
		boolean			usingGzip = false;
		boolean			result = true;

		long beginMillis = System.currentTimeMillis();

		try {
			in = new BufferedReader(
					new FileReader( entryFile ) );
			}
		catch ( IOException ex )
			{
			this.setReason
				( "sendFileAscii: can not open input file '"
					+ entryFile.getPath() + "' " + ex.getMessage() );
			result = false;
			}

		tempFile = new File( this.generateTempPath() );

		try {
			if ( gzipFileMode && (entryFile.length() > MIN_GZIP_SIZE) )
				{
				usingGzip = true;
				out = new BufferedOutputStream
						( new GZIPOutputStream
							( new FileOutputStream( tempFile ) ) );
				}
			else
				{
				out = new BufferedOutputStream
						( new FileOutputStream( tempFile ) );
				}
			}
		catch ( IOException ex )
			{
			result = false;
			this.setReason
				( "sendFileAscii: can not open output file '"
					+ tempFile.getPath() + "' " + ex.getMessage() );
			CVSLog.logMsg( this.getReason() );
			}

		if ( result )
			{
			for ( ; ; )
				{
				try {
				//	inLine = in.readLine();
					inLine = this.readAsciiLine( in );
					if ( inLine == null )
						break;

					out.write( inLine.getBytes() );
					out.write( '\012' );
					}
				catch ( IOException ex )
					{
					result = false;
					this.setReason
						( "sendFileAscii: failed converting into temp file '"
							+ tempFile.getPath() + "' " + ex.getMessage() );
					CVSLog.logMsg( this.getReason() );
					}

				if ( this.isCanceled() )
					break;
				}

			try { in.close(); }
			catch ( IOException ex )
				{
				result = false;
				this.setReason
					( "sendFileAscii: ERROR closing input file: "
						+ ex.getMessage() );
				CVSLog.logMsg( this.getReason() );
				}
			try { out.close(); }
			catch ( IOException ex )
				{
				result = false;
				this.setReason
					( "sendFileAscii: ERROR closing input file: "
						+ ex.getMessage() );
				CVSLog.logMsg( this.getReason() );
				}

			long endMillis = System.currentTimeMillis();

			if ( false )
				System.err.println
					( "CVSClient.sendFileAscii: TIME = '"
						+ (endMillis - beginMillis) + "' millis.");

			if ( result )
				{
				Long sizeLong = new Long( tempFile.length() );
				String sizeStr = sizeLong.toString();

				if ( usingGzip )
					sizeStr = "z" + sizeStr;

				result = this.sendLine( sizeStr );
				if ( result )
					{
					try {
						BufferedInputStream content =
							new BufferedInputStream(
								new FileInputStream( tempFile ) );

						result = this.sendFileContents( content );
						}
					catch ( FileNotFoundException ex )
						{
						result = false;
						this.setReason
							( "sendFileAscii: failed re-opening temp file '"
								+ tempFile.getPath() + "' " + ex.getMessage() );
						CVSLog.logMsg( this.getReason() );
						}
					}
				else
					{
					this.setReason
						( "sendFileRaw: ERROR writing file size: "
							+ this.getReason() );
					CVSLog.logMsg( this.getReason() );
					}

				try { tempFile.delete(); }
				catch ( SecurityException ex )
					{
					// we will leave result, since upload has succeeded...
					CVSLog.logMsg
						( "sendFileAscii: WARNING deleting temp file: "
							+ ex.getMessage() );
					}
				}
			}

		return result;
		}

	public String
	readAsciiLine( Reader in )
		{
		String ls = System.getProperty( "line.separator" );

		int lineSepIdx = 0;
		int lineSepLen = ls.length();

		char[] lineSep = new char[ lineSepLen ];
		ls.getChars( 0, lineSepLen, lineSep, 0 );

		char ch;
		StringBuffer line =
			new StringBuffer( 132 );

		try {
			for ( ; ; )
				{
				int inByte = in.read();
				if ( inByte == -1 )
					{
					// Be sure to grab anything in the lineSep buf...
					for ( int i = 0 ; i < lineSepIdx ; ++i )
						line.append( lineSep[i] );

					if ( line.length() == 0 )
						line = null;

					break;
					}

				ch = (char) inByte;
				if ( ch == lineSep[ lineSepIdx ] )
					{
					// check for completed line separator.
					if ( ++lineSepIdx >= lineSepLen )
						break;
					}
				else
					{
					// Append any buffered separator chars
					for ( int i = 0 ; i < lineSepIdx ; ++i )
						line.append( lineSep[i] );

					// Reset line separator index
					lineSepIdx = 0;

					if ( ch == lineSep[ lineSepIdx ] )
						{
						// check for completed line separator.
						if ( ++lineSepIdx >= lineSepLen )
							break;
						}
					else
						{
						// Append read char
						line.append( ch );
						}
					}
				}
			}
		catch ( IOException ex )
			{
			line = null;
			}

		return ( line != null ? line.toString() : null );
		}

	public boolean
	sendValidResponses( CVSRequest request, String additional )
		{
		boolean result = true;

		result =
			this.sendLine(
				"Valid-responses "
					+ "E M ok error Valid-requests "
					+ "Created Merged Updated Update-existing "
					+ "Removed Remove-entry New-entry "
					+ "Checked-in Checksum Copy-file Notified "
					+ "Clear-sticky Set-sticky "
					+ "Clear-static-directory Set-static-directory "
					+ additional );

		if ( request.useUnchanged )
			{
			this.sendLine( "UseUnchanged" );
			}

		return result;
		}

	public boolean
	sendString( String string )
		{
		boolean result = true;

		CVSTracer.traceIf( this.tracingTCPData,
			"CVSClient.SENDString: '" +string+ "'" );

		try {
			this.outstream.write( string.getBytes() );
			this.outstream.flush();
			}
		catch ( IOException ex )
			{
			result = false;
			}

		return result;
		}
	
	public boolean
	sendLine( String line )
		{
		boolean result = true;

		CVSTracer.traceIf( this.tracingTCPData,
			"CVSClient.SENDLine: '" +line+ "'" );

		try {
			this.outstream.write( (line + "\012").getBytes() );
			this.outstream.flush();
			}
		catch ( IOException ex )
			{
			CVSTracer.traceException( "SENDLINE: " + line, ex );
			result = false;
			}

		return result;
		}
	
	public String
	readLine()
		{
		char ch;
		StringBuffer line = new StringBuffer( 512 ); // REVIEW Better number? Avg?

		try {
			for ( ; ; )
				{
				int inByte = this.instream.read();
				if ( inByte == -1 )
					{
					if ( line.length() == 0 )
						line = null;
					break;
					}

				ch = (char) inByte;
				if ( ch == '\012' )
					{
					break;
					}

				line.append( ch	);
				}
			}
		catch ( IOException ex )
			{
			line = null;
			}

		CVSTracer.traceIf( this.tracingTCPData,
			"CVSClient.READLine: '"
			+ ((line==null) ? "(null)" : line.toString()) + "'" );

		return ( line != null ? line.toString() : null );
		}

	public String
	readResponse()
		{
		String			line;
		StringBuffer	result = new StringBuffer( "" );

		for ( ; ; )
			{
			line = this.readLine();

			if ( line == null ) break;

			CVSTracer.traceIf( this.tracingTCPData,
				"CVSClient.READLine: '" +line+ "'" );

			result.append( line );
			result.append( "\n" );

			if ( line.startsWith( "ok" ) )
				{
				break;
				}

			if ( line.startsWith( "error" ) )
				{
				break;
				}
			}

		return result.toString();
		}


	private InetAddress
	getInterfaceAddress( String host, int port )
		throws IOException
		{
		InetAddress interfaceAddress = null;

		//
		// The following code is used for the case where we have
		// multiple interfaces on the system. When there is more
		// than one interface, InetAddress.getLocalHost() may not
		// return the interface that we need to route to the cvs
		// server. Thus, we instead first make a connection, then
		// we use that connection to determine the interface, and
		// ergo the ip address to use, that routes to the server.
		//
		// REVIEW
		// This does cost us a double connect, which is expensive.
		// I am also not sure what cost it is to the server, which
		// will reject the rsh because it is not on a priveleged
		// port. We may eventually want a property to be able to
		// turn this on and off.
		//
		// Thanks to Roger Vaughn <rvaughn@pobox.com> for solving
		// this problem and providing this code!
		//

		try {
			Socket probe = new Socket( host, port );
			interfaceAddress = probe.getLocalAddress();
			probe.close();
			}
		catch ( IOException ex )
			{
			// Didn't work! Use the "default" instead.
			interfaceAddress = InetAddress.getLocalHost();
			}

		return interfaceAddress;
		}

	private Socket
	bindLocalSocket( CVSRequest request, InetAddress localhost, String host, int port )
		throws IOException
		{
		Socket sock = null;
		String errMessage = "could not bind socket between 1025-1152 locally";

		for ( int local = 1025 ; sock == null && local < 1152 ; ++local )
			{
			if ( this.isCanceled() )
				break;

			CVSTracer.traceIf( request.traceRequest,
				"bindLocalSocket() trying port " + local );

			try {
				sock = new Socket( host, port, localhost, local );
				}
			catch ( IOException ex )
				{
				socket = null;
				CVSTracer.traceIf( request.traceRequest,
					"bindLocalSocket() exception message: " + ex.getMessage() );

				//
				// HACK
				// If we attempt to run through each of these sockets, while
				// we are going over the wire (we don't get "Address in use",
				// we get "Connection refused"), then we are going to spend
				// one hell of a time in this loop. The hack is that we depend
				// on the word "refused" to be in the correct error message. If
				// not, then we only revert back to the infinite wait while we
				// loop over all of these ports.
				//
				if ( ex.getMessage().indexOf( "refused" ) > -1
						|| ex.getMessage().indexOf( "timed out" ) > -1 )
					{
					errMessage = ex.getMessage();
					break;
					}
				}
			}

		if ( sock == null )
			{
			throw new IOException( errMessage );
			}

		return sock;
		}

	//
	// UNDONE HIGH
	// There *is* a .ssh_known_hosts file, especially w/ cygwin.
	//
	public boolean
	verifyHost( String host, SshPublicKey pk )
		throws TransportProtocolException
		{
		CVSTracer.traceIf( false,
			"CVSClient.verifyHost: host '" + host + "', Pk '" + pk + "'" );

		return true;
		}

	private void
	establishSSHConnection( CVSRequest request )
		throws IOException
		{
		SshConnectionProperties properties = new SshConnectionProperties();

		CVSTracer.traceIf( request.traceRequest,
			"CVSClient.establishSSHConnection: creating connection..." );

		InetAddress localhost =
			this.getInterfaceAddress
				( request.getHostName(), request.getPort() );

		properties.setHost( request.getHostName() );
		properties.setPort( request.getPort() );

		CVSTracer.traceIf( request.traceRequest,
			"CVSClient.establishSSHConnection: localHost=" + localhost );

		this.sshClient = new SshClient();

		CVSTracer.traceIf( request.traceRequest,
			"CVSClient.establishSSHConnection: sshClient=" + this.sshClient );

		this.sshClient.connect( properties, this );

		CVSTracer.traceIf( request.traceRequest,
			"CVSClient.establishSSHConnection: connected" );

		PasswordAuthenticationClient pwdAuth =
			new PasswordAuthenticationClient();

		pwdAuth.setUsername( request.getUserName() );
		pwdAuth.setPassword( request.getPassword() );

		int result = sshClient.authenticate( pwdAuth );

		CVSTracer.traceIf( request.traceRequest,
			"CVSClient.authenticate: result=" + result );

		if ( result==AuthenticationProtocolState.FAILED )
			{
			CVSTracer.traceIf( request.traceRequest,
				"The authentication failed" );
			throw new IOException( "ssh authentication failure" );
			}
		else if ( result==AuthenticationProtocolState.PARTIAL )
			{
			CVSTracer.traceIf( request.traceRequest,
				"The authentication succeeded but another"
				+ "authentication is required");
			throw new IOException( "ssh authentication partial" );
			}
		else if ( result==AuthenticationProtocolState.COMPLETE )
			{
			CVSTracer.traceIf( request.traceRequest,
				"The authentication is complete");
			}

	//	String srvVersionStr = this.sshTransport.getServerVersion();

	//	CVSTracer.traceIf( request.traceRequest,
	//		"CVSClient.establishSSHConnection: SVR VERSION '" + srvVersionStr + "'" );

		this.sshSession = this.sshClient.openSessionChannel();

		CVSTracer.traceIf( request.traceRequest,
			"CVSClient.establishSSHConnection: sshSession=" + this.sshSession );

		boolean ok = this.sshSession.executeCommand( request.getServerCommand() );

		CVSTracer.traceIf( request.traceRequest,
			"CVSClient.establishSSHConnection: command("
			+ request.getServerCommand() + ") = " + ok );

		if ( ok )
			{
			CVSTracer.traceIf( request.traceRequest,
				"CVSClient.establishSSHConnection: command session established" );
			}
		else
			{
			CVSTracer.traceIf( request.traceRequest,
				"CVSClient.establishSSHConnection: executeCommand( '"
				+ request.getServerCommand() + "' failed." );

			throw new IOException
				( "failed to execute command '"
					+ request.getServerCommand() + "'" );
			}
		}

	private Socket
	establishRSHSocket( CVSRequest request )
		throws IOException
		{
		Socket sock = null;

		InetAddress localhost =
			this.getInterfaceAddress
				( request.getHostName(), request.getPort() );

		for ( int local = 512 ; sock == null && local < 1024 ; ++local )
			{
			try {
				sock = new Socket
					( request.getHostName(),
						request.getPort(), localhost, local );
				}
			catch ( IOException ex )
				{
				socket = null;
				}
			}

		if ( sock == null )
			{
			throw new IOException
				( "Could not bind rsh socket between 512-1023 locally." );
			}

		return sock;
		}

	private boolean
	openServer( CVSRequest request )
		{
		boolean result;

		this.socket = null;
		this.process = null;
		this.serverIsOpen = false;

		// Create the socket used for the event server connection
		try
			{
			CVSTracer.traceIf( request.traceRequest,
				"CVSClient.openServer: creating connection..." );

			result = true;
			switch ( request.getConnectionMethod() )
				{
				case CVSRequest.METHOD_INETD:
					this.socket = new Socket
						( request.getHostName(), request.getPort() );
					break;

				case CVSRequest.METHOD_SSH:
					this.establishSSHConnection( request );
					break;

				case CVSRequest.METHOD_RSH:
					if ( request.getRshProcess() != null )
						{
						int index =
							request.getServerCommand().indexOf( ' ' );

						String[] argv = new String[6];

						argv[0] = request.getRshProcess();
						argv[1] = request.getHostName();
						argv[2] = "-l";
						argv[3] = request.getUserName();
						if ( index < 0 )
							{
							argv[4] = request.getServerCommand();
							argv[5] = "server";
							}
						else
							{
							argv[4] =
								request.getServerCommand().substring
									( 0, index );
							argv[5] = 
								request.getServerCommand().substring
									( index + 1 );
							}

						if ( request.traceRequest )
							{
							for ( int i = 0 ; i < argv.length ; ++i )
								CVSTracer.traceIf( true,
									"CVSClient.openServer: RSH argv["
										+ i + "] = '" + argv[i] + "'" );
							}

						this.process = Runtime.getRuntime().exec( argv );
						}
					else
						{ 
						this.socket = this.establishRSHSocket( request );
						}
					break;
				}

			CVSTracer.traceIf( request.traceRequest,
				"CVSClient.openServer: creating i/o streams..." );

			if ( this.sshSession != null )
				{
				this.instream =
					new DataInputStream
						( this.sshSession.getInputStream() );

				this.outstream =
					new DataOutputStream
						( this.sshSession.getOutputStream() );
				}
			else if ( this.process != null )
				{
				this.instream =
					new DataInputStream
						( this.process.getInputStream() );

				this.outstream =
					new DataOutputStream
						( this.process.getOutputStream() );
				}
			else if ( this.socket != null )
				{
				this.instream =
					new DataInputStream
						( this.socket.getInputStream() );

				this.outstream =
					new DataOutputStream
						( this.socket.getOutputStream() );
				}
			else
				{
				CVSTracer.traceIf( request.traceRequest,
					"CVSClient.openServer: failed to establish connection." );
				result = false;
				}

			if ( result )
				{
				CVSTracer.traceIf( request.traceRequest,
					"CVSClient.openServer: server is open." );
				this.serverIsOpen = true;
				}
			}
		catch ( IOException ex )
			{
			this.serverIsOpen = false;
			int meth = request.getConnectionMethod();

			this.setReason
				( "could not create "
					+ ( meth == CVSRequest.METHOD_INETD
						? "INETD"
						: ( meth == CVSRequest.METHOD_RSH
							? "RSH" : "SSH" ) )
					+ " connection for '" + port
					+ "@" + request.getHostName()
					+ "' --> " + ex.getMessage() );
			
			CVSLog.logMsg( this.getReason() );
			}

		if ( this.serverIsOpen
				&& this.socket != null
				&& ( request.getConnectionMethod()
						== CVSRequest.METHOD_RSH) )
			{
			CVSTracer.traceIf( request.traceRequest,
				"CVSClient.openServer: performing rsh protocol initialization." );

			result =
				this.performRSHProtocol
					( request.getUserName(),
						request.getServerCommand() );

			if ( result == false )
				{
				this.closeServer();
				}
			}

		return this.serverIsOpen;
		}

	public boolean
	performRSHProtocol( String remoteUserName, String serverCommand )
		{
		int		status;

		// ------------- STDERR PORT ---------------
		try {
			this.outstream.write( 48 ); // 48 = "0"
			this.outstream.write( 0 );
			}
		catch ( IOException ex )
			{
			CVSLog.logMsg( "RSH: FAIL-ed writing stderr port" );
			return false;
			}

		// ------------- LOCAL USER NAME ---------------
		// Use the Java system property "user.name" for the local
		// name, but default to remoteUserName is not set.
		String localUserName =
			System.getProperty
				( "user.name", remoteUserName );

		try {
			this.outstream.write( localUserName.getBytes() );
			this.outstream.write( 0 );
			}
		catch ( IOException ex )
			{
			CVSLog.logMsg( "RSH: FAIL-ed writing local user" );
			return false;
			}

		// ------------- REMOTE USER NAME ---------------
		try {
			this.outstream.write( remoteUserName.getBytes() );
			this.outstream.write( 0 );
			}
		catch ( IOException ex )
			{
			CVSLog.logMsg( "RSH: FAIL-ed writing remote user" );
			return false;
			}

		// ------------- COMMAND LINE ---------------
		try {
			this.outstream.write( serverCommand.getBytes() );
			this.outstream.write( 0 );
			this.outstream.flush();
			}
		catch ( IOException ex )
			{
			CVSLog.logMsg( "RSH: FAIL-ed writing command" );
			return false;
			}
		
		// ------------- READ STATUS BYTE ---------------
		try {
			status = this.instream.read();
			}
		catch ( IOException ex )
			{
			CVSLog.logMsg( "RSH: FAIL-ed reading status" );
			return false;
			}

		if ( status != 0 )
			return false;

		return true;
		}

	public boolean
	closeServer()
		{
		boolean result = true;

		if ( this.serverIsOpen )
			{
			try
				{
				if ( this.sshSession != null )
					{
					this.sshSession.close();
					this.sshClient.disconnect();
					}
				else
					{
					this.instream.close();
					this.outstream.close();
					}

				if ( this.socket != null )
					{
					this.socket.close();
					}
				else if ( this.process != null )
					{
					this.process.destroy();
					}
				}
			catch ( IOException ex )
				{
				result = false;
				this.setReason
					( "could not close socket - " + ex.getMessage() );
				CVSLog.logMsg( this.getReason() );
				}

			this.socket = null;
			this.sshClient = null;
			this.sshSession = null;
			this.instream = null;
			this.outstream = null;
			this.serverIsOpen = false;
			}

		return result; 
		}

	// EH-null-ui  Etienne-Hugues Fortin <ehfortin@sympatico.ca>
	private
	class	NullCVSUI
		implements CVSUserInterface
		{
		public void uiDisplayProgressMsg( String message ) { }
		public void uiDisplayProgramError( String error ) { }
		public void uiDisplayResponse( CVSResponse response ) { }
		}

	}
