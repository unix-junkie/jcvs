/*
** Java cvs client library package.
** Copyright (c) 1997-2002 by Timothy Gerard Endres
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
import java.lang.*;
import java.text.*;
import java.util.*;

/**
 * The CVSRequest class is used to encapsulate a complete
 * description of a CVS server request. Capable of parsing
 * a simple string to define a request, CVSRequests are
 * often built by configuration properties. Once a CVRequest
 * is built and filled in, it is handed to a CVSClient for
 * processing. Once the processing is completed, the CVSClient
 * will return a CVSResponse containing the results of the
 * request.
 *
 * @version $Revision: 2.8 $
 * @author Timothy Gerard Endres, <time@gjt.org>.
 * @see CVSClient
 * @see CVSProject
 */

public class
CVSRequest extends Object
	{
	static public final String	RCS_ID = "$Id: CVSRequest.java,v 2.8 2002/02/10 18:01:44 time Exp $";
	static public final String	RCS_REV = "$Revision: 2.8 $";

	static public final int		METHOD_INETD = 1;
	static public final int		METHOD_RSH = 2;
	static public final int		METHOD_SSH = 3;

	static private final int	ES_FIRST = 0;

	static public final int		ES_NONE = ES_FIRST;
	static public final int		ES_ALL = ES_NONE + 1;
	static public final int		ES_SEL = ES_ALL + 1;
	static public final int		ES_SELALL = ES_SEL + 1;
	static public final int		ES_ALLMOD = ES_SELALL + 1;
	static public final int		ES_SELMOD = ES_ALLMOD + 1;
	static public final int		ES_ALLLOST = ES_SELMOD + 1;
	static public final int		ES_SELLOST = ES_ALLLOST + 1;
	static public final int		ES_ALLUNC = ES_SELLOST + 1;
	static public final int		ES_SELUNC = ES_ALLUNC + 1;
	static public final int		ES_USER = ES_SELUNC + 1;
	static public final int		ES_NEW = ES_USER + 1;
	static public final int		ES_POPUP = ES_NEW + 1;

	static private final int	ES_LAST = ES_POPUP;


	private PrintStream			traceStream;

	/**
	* If true, the request phase of the CVS command will be
	* traced on stderr.
	*/
	public boolean				traceRequest;
	/**
	* If true, the response phase of the CVS command will be
	* traced on stderr.
	*/
	public boolean				traceResponse;
	/**
	* If true, the processing phase of the CVS command will be
	* traced on stderr.
	*/
	public boolean				traceProcessing;
	/**
	* If true, all input and output data (TCP bytes) will be traced.
	*/
	public boolean				traceTCPData;

	private String				vfReason;

	/**
	* This is set by the "Valid-requests" cvs request.
	* If true, the server understands 'Unchanged' requests.
	*/
	public boolean				useUnchanged;

	/**
	* This is set by the "Valid-requests" cvs request.
	* If true, the server understands 'Directory' requests.
	*/
	public boolean				useDirectory;

	/**
	* This determines where cvs commands are executed.
	* If true, the command is run in the 'current directory'.
	* If false, the command is run at the 'top level'.
	*/
	public boolean				execInCurDir;

	/**
	* Determines if this request will send the 'Entry' requests.
	*/
	public boolean				sendEntries;

	/**
	* Determines if this request will send 'Modified' requests
	* for files that are modified locally. If true, modified
	* files are uploaded via 'Modified'. If false, modified
	* files are treated as if they were unchanged.
	*/
	public boolean				sendModifieds;

	/**		
	* Determines if this request will send the special empty 'Modified'
	* requests when a file is to be uploaded. If true, all 'Modified'
	* requests send a file size of zero to optimize the protocol when
	* the file's contents are not needed.
	*/
	public boolean				sendEmptyMods;

	/**
	* Determines if this request will send 'Argument 's.
	*/
	public boolean				sendArguments;

	/**
	* Determines if this request will send the 'Entry' list as 'files...'.
	*/
	public boolean				sendEntryFiles;

	/**
	* Determines if this request will send the 'Repository' list as 'module'.
	*/
	public boolean				sendModule;

	/**
	* Determines if this request will send the 'RootDirectory'.
	*/
	public boolean				sendRootDirectory;

	/**
	* Determines if this request should include the 'Notify' requests.
	*/
	public boolean				includeNotifies;

	/**
	* Determines if this request will only verify the login and then return.
	*/
	public boolean				verificationOnly;

	/**
	* Determines if this request will guarantee a '-m message' argument.
	* Actually getting the message from the user is the responsibility
	* of the code that uses the request, since CVSRequests have no clue
	* about how to get messages.
	*/
	public boolean				guaranteeMsg;

	/**
	* Determines if the output of the reponse (stderr & stdout) will
	* be redirected to a user specified file. It is the responsbility
	* of the code using the request to setup the output file.
	*/
	public boolean				redirectOutput;

	/**
	* Determines if this request should display the reponse.
	*/
	public boolean				displayReponse;

	/**
	* Determines if this request will handle 'Updated' responses.
	*/
	public boolean				handleUpdated;

	/**
	* Determines if this request will handle 'Merged' responses.
	*/
	public boolean				handleMerged;

	/**
	* Determines if this request will handle 'Copy-file' responses.
	*/
	public boolean				handleCopyFile;

	/**
	* Determines if this request will handle all entries related
	* responses ('New-entry', 'Remove-entry', etc.).
	*/
	public boolean				handleEntries;

	/**	 
	* Determines if this request will handle all settings related
	* responses ('Set-sticky', 'Clear-static-directory', etc.).
	*/
	public boolean				handleFlags;

	/**	 
	* If true, the reponse's status will be ignored and assumed
	* to be 'ok'. If false, the reponse's status will be set to
	* reflect the status returned by the server.
	*/
	public boolean				ignoreResult;

	/**	 
	* If true, reponses that try to overwrite existing updated
	* files	will be allowed to overwrite if the file is in the
	* list of files sent to the server. If false, normal processing
	* occurs (no overwrites allowed).
	*/
	public boolean				allowOverWrites;

	/**	 
	* If true, the temporary files generated by this request will
	* not be deleted as usual. This is primarily for debugging.
	*/
	public boolean				saveTempFiles;

	/**
	* Reflects the current 'Sticky' setting in this request.
	*/
	public boolean				stickyIsSet;

	/**
	* Reflects the current 'Static-directory' setting in this request.
	*/
	public boolean				staticDirIsSet;

	/**
	* The 'Valid-requests' response string from the server.
	*/
	public String				validRequests;

	/**
	* Determines whether or not the server's reponse is queued.
	* If true, all responses will be queued and handed back in
	* the CVSResponse. If false, responses will be handed to the
	* 'responseHandler' for processing as they come from the server.
	*/
	public boolean				queueResponse;

	/**
	 * The response handler. If this request does not use the
	 * 'queue response' option, then this field must be set to
	 * the CVSResponseHandler that will handle the responses
	 * to this request.
	 */
	public CVSResponseHandler	responseHandler;

	/**
	 * Force every file to go up as 'Modified'.
	 */
	public boolean				forceModifieds;

	/**
	 * If > 0, sets Gzip-stream level. If 0, do not use Gzip-stream mode.
	 */
	public int					gzipStreamLevel;

	/**
	 * If true, allow gzip-file-contents mode, otherwise suppress it.
	 */
	public boolean				allowGzipFileMode;

	/**
	 * If true, send all files using gzip-file-contents mode.
	 */
	public boolean				gzipFileMode;

	/**
	 * The 'Notification' vector. If this vector is not null
	 * then it contains a vector if notification strings of the
	 * format: 'File\tType\tTime\tHost\tWorkingDir\tWatches'.
	 */
	public Vector				notifies;

	/**
	 * The 'Sticky' tags which are based in the 'Tag' file in
	 * the admin directory are provided as a hashtable of tagspecs
	 * where the key is the localDir ready for the 'Directory ' command.
	 */
	private Hashtable			stickys;
	private Hashtable			statics;

	private String[]			setVars;

	private int					connMethod;
	private String				rshProcess;

	private boolean				isPServer;
	private String				userName;
	private String				password; // scrambled!
	private String				serverCommand;

	private String				updateProg;
	private String				checkInProg;

	private int					port;
	private String				hostName;

	private String				repository;
	private String				rootDirectory;
	private String				rootRepository;
	private String				localDirectory;

	private String				command;
	private int					entrySelector;

	/**
	 * Indicates the directory to run cvs command in.
	 * This is used in conjunction with execInCurDir
	 * to indicate the directory in which the command
	 * should be executed.
	 */
	private CVSEntry			dirEntry;

	private CVSEntryVector		entries;
	private CVSArgumentVector	arguments;
	private CVSArgumentVector	globalargs;

	private CVSUserInterface	ui;

	private PrintWriter			redirectWriter;


	/**
	 * Constructs a new CVSRequest object.
	 */
	public CVSRequest()
		{
		super();

		this.vfReason = null;
		this.traceStream = null;
		this.traceRequest = false;
		this.traceResponse = false;
		this.traceProcessing = false;
		this.traceTCPData = false;

		this.connMethod = CVSRequest.METHOD_INETD;
		this.rshProcess = null;

		this.isPServer = false;
		this.userName = "";
		this.password = "";
		this.serverCommand = "";

		this.useUnchanged = false;
		this.useDirectory = true;

		this.execInCurDir = false;

		this.sendEntries = false;
		this.sendModifieds = false;
		this.sendEmptyMods = false;
		this.sendArguments = false;
		this.sendEntryFiles = false;
		this.sendModule = false;
		this.sendRootDirectory = true;
		this.includeNotifies = true;
		this.verificationOnly = false;
		this.forceModifieds = false;
		this.gzipStreamLevel = 0;
		this.gzipFileMode = false;
		this.allowGzipFileMode = true;

		this.guaranteeMsg = false;
		this.redirectOutput = false;
		this.redirectWriter = null;

		this.displayReponse = false;
		this.allowOverWrites = false;
		this.handleUpdated = false;
		this.handleMerged = false;
		this.handleCopyFile = false;
		this.handleEntries = false;
		this.handleFlags = false;
		this.saveTempFiles = false;

		this.queueResponse = true;
		this.responseHandler = null;

		this.stickyIsSet = false;
		this.staticDirIsSet = false;

		this.updateProg = null;
		this.checkInProg = null;

		this.setVars = null;

		this.port = CVSClient.DEFAULT_CVS_PORT;

		this.hostName = null;
		this.repository = null;
		this.rootDirectory = null;
		this.localDirectory = null;

		this.command = null;
		this.entrySelector = CVSRequest.ES_SEL;

		this.ui = null;
		this.dirEntry = null;
		this.entries = null;
		this.arguments = null;
		this.globalargs = null;
		this.stickys = null;
		}

	protected void
	finalize()
		throws Throwable
		{
		super.finalize();
		this.endRedirection();
		}

	/**
	 * Returns the request's server hostname.
	 *
	 * @return The string representing the request's server's hostname.
	 */
	public String
	getHostName()
		{
		return this.hostName;
		}

	/**
	 * Sets the request's server hostname. The hostname
	 * is used to establish the connection with the CVS server.
	 *
	 * @param hostName The new hostname for the request's CVS Server.
	 */
	public void
	setHostName( String hostName )
		{
		this.hostName = hostName;
		}

	/**
	 * Returns the request's server port number.
	 *
	 * @return The request's CVS server port number.
	 */
	public int
	getPort()
		{
		return this.port;
		}

	/**
	 * Sets the request's server port number. The port number
	 * is used to establish the connection with the CVS server.
	 *
	 * @param port The new port number for the request's CVS server.
	 */
	public void
	setPort( int port )
		{
		this.port = port;
		}

	public boolean
	isPServer()
		{
		return this.isPServer;
		}

	public void
	setPServer( boolean isPServer )
		{
		this.isPServer = isPServer;
		}

	public CVSUserInterface
	getUserInterface()
		{
		return this.ui;
		}

	public void
	setUserInterface( CVSUserInterface ui )
		{
		this.ui = ui;
		}

	public String
	getUserName()
		{
		return this.userName;
		}

	public void
	setUserName( String userName )
		{
		this.userName = userName;
		}

	public String
	getPassword()
		{
		return this.password;
		}

	public void
	setPassword( String password )
		{
		this.password = password;
		}

	public String
	getServerCommand()
		{
		return this.serverCommand;
		}

	public void
	setServerCommand( String command )
		{
		this.serverCommand = command;
		}

	public int
	getConnectionMethod()
		{
		return this.connMethod;
		}

	public void
	setConnectionMethod( int method )
		{
		this.connMethod = method;
		}

	public String
	getRshProcess()
		{
		return this.rshProcess;
		}

	public void
	setRshProcess( String rshProcess )
		{
		this.rshProcess = rshProcess;
		}

	public int
	getGzipStreamLevel()
		{
		return this.gzipStreamLevel;
		}

	public void
	setGzipStreamLevel( int level )
		{
		this.gzipStreamLevel = level;
		}

	public CVSEntry
	getDirEntry()
		{
		return this.dirEntry;
		}

	public void
	setDirEntry( CVSEntry dirEntry )
		{
		this.dirEntry = dirEntry;
		}

	/**
	 * Returns the request's local directory, which represents
	 * the project's local directory.
	 *
	 * @return The request's local directory.
	 */
	public String
	getLocalDirectory()
		{
		return this.localDirectory;
		}

	/**
	 * Sets the request's local directory.
	 *
	 * @param localDirectory The new local directory for the request.
	 */
	public void
	setLocalDirectory( String localDirectory )
		{
		this.localDirectory = localDirectory;
		}

	/**
	 * Returns the request's entry's local file.
	 *
	 * @return The request's entry's local file.
	 */
	public File
	getLocalFile( CVSEntry entry )
		{
		File result =
			new File(
				this.localDirectory + "/"
				+ entry.getFullName() );

		if ( false )
		CVSTracer.traceIf( true,
			"CVSRequest.getLocalFile: entry '" + entry.getFullName()
			+ "' localFile '" + result.getPath() + "'" );

		return result;
		}

	/**
	 * Returns the request's root directory, which represents
	 * the project's CVS root directory on the server.
	 *
	 * @return The request's root directory.
	 */
	public String
	getRootDirectory()
		{
		return this.rootDirectory;
		}

	/**
	 * Sets the request's root directory.
	 *
	 * @param rootDirectory The new root directory for the request.
	 */
	public void
	setRootDirectory( String rootDirectory )
		{
		this.rootDirectory = rootDirectory;
		}

	/**
	 * Returns the request's repository, which represents
	 * the project's CVS module on the server.
	 *
	 * @return The request's repository.
	 */
	public String
	getRepository()
		{
		return this.repository;
		}

	/**
	 * Sets the request's repository (or module name).
	 *
	 * @param repository The request's repository.
	 */
	public void
	setRepository( String repository )
		{
		this.repository = repository;
		}

	/**
	 * Returns the request's ROOT repository. This is the
	 * full repository path to the root entry's directory.
	 * This should match rootEntry.getRepository().
	 *
	 * @return The request's ROOT repository.
	 */
	public String
	getRootRepository()
		{
		return this.rootRepository;
		}

	/**
	 * Sets the request's ROOT repository.
	 *
	 * @param repository The request's ROOT repository.
	 */
	public void
	setRootRepository( String repository )
		{
		this.rootRepository = repository;
		}

	/**
	 * Returns the request's response handler.
	 *
	 * @return The request's response handler.
	 */
	public CVSResponseHandler
	getResponseHandler()
		{
		return this.responseHandler;
		}

	/**
	 * Sets the request's response handler.
	 *
	 * @param repository The request's response handler.
	 */
	public void
	setResponseHandler( CVSResponseHandler responseHandler )
		{
		this.responseHandler = responseHandler;
		}

	/**
	 * Returns the request's entry list as a vector.
	 *
	 * @return The request's entry list in a CVSEntryVector.
	 */
	public CVSEntryVector
	getEntries()
		{
		return this.entries;
		}

	/**
	 * Sets the request's entry list.
	 *
	 * @param entries The new list of entries for this request.
	 */
	public void
	setEntries( CVSEntryVector entries )
		{
		this.entries = entries;
		}

	/**
	 * Returns the request's entry selector.
	 *
	 * @return The request's entry selector.
	 */
	public int
	getEntrySelector()
		{
		return this.entrySelector;
		}

	/**
	 * Returns the request's argument list as a vector.
	 *
	 * @return The request's argument list.
	 */
	public CVSArgumentVector
	getArguments()
		{
		return this.arguments;
		}

	/**
	 * Sets the request's argument list.
	 *
	 * @param arguments The new list of argument for this request.
	 */
	public void
	setArguments( CVSArgumentVector arguments )
		{
		this.arguments = arguments;
		}

	/**
	 * Appends an argument list to the request's argument list.
	 *
	 * @param arguments The list of arguments to append.
	 */
	public void
	appendArguments( CVSArgumentVector newArgs )
		{
		if ( this.arguments == null )
			{
			this.arguments = new CVSArgumentVector();
			}

		for ( int i = 0 ; newArgs != null && i < newArgs.size() ; ++i )
			{
			this.arguments.appendArgument( newArgs.argumentAt(i) );
			}
		}

	/**
	 * Returns the request's global argument list as a vector.
	 *
	 * @return The request's argument list.
	 */
	public CVSArgumentVector
	getGlobalArguments()
		{
		return this.globalargs;
		}

	/**
	 * Sets the request's global argument list.
	 *
	 * @param arguments The new list of argument for this request.
	 */
	public void
	setGlobalArguments( CVSArgumentVector arguments )
		{
		this.globalargs = arguments;
		}

	/**
	 * Appends an argument list to the request's global argument list.
	 *
	 * @param arguments The list of arguments to append.
	 */
	public void
	appendGlobalArguments( CVSArgumentVector newArgs )
		{
		if ( this.globalargs == null )
			{
			this.globalargs = new CVSArgumentVector();
			}

		for ( int i = 0 ; i < newArgs.size() ; ++i )
			{
			this.globalargs.appendArgument( newArgs.argumentAt(i) );
			}
		}

	/**
	 * Returns the request's command name.
	 *
	 * @return The request's command name.
	 */
	public String
	getCommand()
		{
		return this.command;
		}

	/**
	 * Sets the request's command.
	 *
	 * @param command The new command for this request.
	 */
	public void
	setCommand( String command )
		{
		this.command = command;
		}

	/**
	 * Returns the request's user set variables.
	 *
	 * @return The request's user set variables.
	 */
	public String[]
	getSetVariables()
		{
		return this.setVars;
		}

	/**
	 * Sets the request's user set variables.
	 *
	 * @param vars The new user set variables.
	 */
	public void
	setSetVariables( String[] vars )
		{
		this.setVars = vars;
		}

	/**
	 * Returns the request's 'Sticky' settings.
	 *
	 * @return The request's 'Sticky' settings Hashtable.
	 */
	public Hashtable
	getStickys()
		{
		return this.stickys;
		}

	/**
	 * Sets the request's 'Sticky' settings.
	 *
	 * @param stickys The new Hashtable of this request's 'Sticky' settings.
	 */
	public void
	setStickys( Hashtable stickys )
		{
		this.stickys = stickys;
		}

	/**
	 * Returns the request's 'Static-directory' settings.
	 *
	 * @return The request's 'Static-directory' settings Hashtable.
	 */
	public Hashtable
	getStatics()
		{
		return this.statics;
		}

	/**
	 * Sets the request's 'Static-directory' settings.
	 *
	 * @param statics The new Hashtable of 'Static-directory' settings.
	 */
	public void
	setStatics( Hashtable statics )
		{
		this.statics = statics;
		}

	/**
	 * Returns the request's 'Checkin-prog' setting.
	 *
	 * @return The request's 'Checkin-prog' program name.
	 */
	public String
	getCheckInProgram()
		{
		return this.checkInProg;
		}

	/**
	 * Sets the request's 'Checkin-prog' setting.
	 * This does <em>not</em> create or delete the 'Checkin.prog'
	 * administration file.	This must be done by the request user.
	 *
	 * @param program The new checkin-program name.
	 */
	public void
	setCheckInProgram( String program )
		{
		this.checkInProg = program;
		}

	/**
	 * Returns the request's 'Update-prog' setting.
	 *
	 * @return The request's 'Update-prog' program name.
	 */
	public String
	getUpdateProgram()
		{
		return this.updateProg;
		}

	/**
	 * Sets the request's 'Update-prog' setting.
	 * This does <em>not</em> create or delete the 'Update.prog'
	 * administration file.	This must be done by the request user.
	 *
	 * @param program The new update-program name.
	 */
	public void
	setUpdateProgram( String program )
		{
		this.updateProg = program;
		}

	public boolean
	isRedirected()
		{
		return ( this.redirectOutput
					&& this.redirectWriter != null );
		}

	public void
	redirectLine( String line )
		{
		if ( this.redirectOutput
				&& this.redirectWriter != null )
			{
			this.redirectWriter.println( line );
			}
		}

	public void
	setRedirectWriter( PrintWriter writer )
		{
		this.redirectWriter = writer;
		this.redirectOutput = ( writer != null );
		}

	public void
	endRedirection()
		{
		if ( this.redirectWriter != null )
			{
			this.redirectWriter.flush();
			this.redirectWriter.close();

			this.redirectWriter = null;
			this.redirectOutput = false;
			}
		}

	static public int
	parseEntriesSelector( char selectCh )
		{
		int		result = CVSRequest.ES_SEL;

		switch ( selectCh )
			{
			case 'N': result = CVSRequest.ES_NONE; break;
			case 'A': result = CVSRequest.ES_ALL; break;
			case 'a': result = CVSRequest.ES_SEL; break;
			case 'e': result = CVSRequest.ES_SELALL; break;
			case 'M': result = CVSRequest.ES_ALLMOD; break;
			case 'm': result = CVSRequest.ES_SELMOD; break;
			case 'L': result = CVSRequest.ES_ALLLOST; break;
			case 'l': result = CVSRequest.ES_SELLOST; break;
			case 'U': result = CVSRequest.ES_ALLUNC; break;
			case 'u': result = CVSRequest.ES_SELUNC; break;
			case 'G': result = CVSRequest.ES_USER; break;
			case 'g': result = CVSRequest.ES_NEW; break;
			case 'p': result = CVSRequest.ES_POPUP; break;
			default:
				result = CVSRequest.ES_SEL;
				CVSLog.logMsg
					( "CVSRequest.parseEntriesSelector: '"
						+ "ERROR bad entries selector '" + selectCh + "'" );
				break;
			}

		return result;
		}

	/**
	 * Process a user provided, or command spec based, argument string.
	 * The syntax is '[global options] command options'.
	 *
	 * @param argStr The argument string to be parsed.
	 */

	public void
	parseArgumentString( String argStr )
		{
		// Check for global options...
		if ( argStr.startsWith( "[" ) )
			{
			int bktidx = argStr.indexOf( "]" );
			if ( bktidx > 0 )
				{
				String gArgStr = argStr.substring( 1, bktidx ).trim();
				argStr = argStr.substring( bktidx + 1 ).trim();
				if ( gArgStr.length() > 0 )
					{
					CVSArgumentVector gArgs =
						CVSArgumentVector.parseArgumentString( gArgStr );

					if ( gArgs != null && gArgs.size() > 0 )
						{
						if ( this.globalargs == null )
							this.globalargs = gArgs;
						else
							this.globalargs.appendArguments( gArgs );
						}
					}
				}
			}

		CVSArgumentVector args =
			CVSArgumentVector.parseArgumentString( argStr );

		if ( args != null && args.size() > 0 )
			{
			if ( this.arguments == null )
				this.arguments = args;
			else
				this.arguments.appendArguments( args );
			}
		}

	/**
	 * Attempts to parse a CVS request specification string. If
	 * the parse succeeds, this request object will be updated
	 * to reflect the request specification, making it ready to
	 * be handed to a CVSClient for processing.
	 *
	 * The string is of the format:
	 * <pre>
	 * :command:select:request:response:arguments
	 * Where:
	 *    command   - is a valid cvs command name (e.g., 'update', 'co', 'diff')
	 *    select    - specifies which entries to apply command to
	 *    request   - is a valid cvs request specification
	 *    reponse   - is a valid cvs reponse handling specification
	 *    arguments - is the remainder of the string taken as command arguments
	 * Refer to the <a href="CVSRequestSpec.html">CVSRequest Specification</a> for details.
	 *
	 * @param specification The CVSRequest Specification string to parse.
	 * @return True if the parse succeeded, false if it failed.
	 */
	public boolean
	parseControlString( String specification )
		{
		int			i, tokenCount;
		boolean		result = true;
		String		commandStr = null;
		String		selectorStr = null;
		String		requestStr = null;
		String		responseStr = null;
		String		argumentStr = null;

		StringTokenizer	toker =
			new StringTokenizer( specification, ":" );

		tokenCount = toker.countTokens();

		if ( tokenCount >= 4 )
			{
			try {
				commandStr = toker.nextToken();
				selectorStr = toker.nextToken();
				requestStr = toker.nextToken();
				responseStr = toker.nextToken();

				// Get the remainder of the string...
				argumentStr = toker.nextToken( "" );
				// Drop the colon left on the front by toker...
				if ( argumentStr != null )
					{
					argumentStr = argumentStr.substring(1);
					}
				}
			catch ( NoSuchElementException ex )
				{
				// UNDONE - report except for missing argumentStr.
				result = false;
				}

			if ( result )
				{
				this.command = commandStr;

				// Process the Entry Selector
				for ( i = 0 ; i < selectorStr.length() ; ++i )
					{
					char selectCh = selectorStr.charAt(i);
					this.entrySelector =
						CVSRequest.parseEntriesSelector( selectCh );
					}

				// Process the REQUEST Flags
				for ( i = 0 ; requestStr != null
						&& i < requestStr.length() ; ++i )
					{
					char cmdChar = requestStr.charAt(i);
					switch ( cmdChar )	
						{
						case 'D': this.sendRootDirectory = false; break;
						case 'E': this.sendEntries = true; break;
						case 'S': this.sendEmptyMods = true; break;
						case 'U': this.sendModifieds = true; break;
						case 'A': this.sendArguments = true; break;
						case 'F': this.sendEntryFiles = true; break;
						case 'M': this.sendModule = true; break;
						case 'N': this.includeNotifies = false; break;
						case 'G': this.guaranteeMsg = true; break;
						case 'P': this.queueResponse = false; break;
						case 'R': this.redirectOutput = true; break;
						case 'V': this.verificationOnly = true; break;
						case 'X': this.execInCurDir = true; break;
						case 'O':
							this.traceRequest = true;
							break;
						case 'I':
							this.traceResponse = true;
							break;
						case 'T':
							this.traceTCPData = true;
							break;
						default:
							CVSLog.logMsg
								( "While parsing CVSRequest '" + specification
								+ "', found invalid request flag '" +cmdChar+ "'" );
							break;
						}
					}

				// Process the REPONSE Flags
				for ( i = 0 ; responseStr != null
						&& i < responseStr.length() ; ++i )
					{
					char cmdChar = responseStr.charAt(i);
					switch ( cmdChar )
						{
						case 'c': this.handleCopyFile = true; break;
						case 'd': this.displayReponse = true; break;
						case 'e': this.handleEntries = true; break;
						case 'f': this.handleFlags = true; break;
						case 'i': this.ignoreResult = true; break;
						case 'm': this.handleMerged = true; break;
						case 'o': this.allowOverWrites = true; break;
						case 'u': this.handleUpdated = true; break;
						case 'k': this.saveTempFiles = true; break;
						case 't':
							this.traceProcessing = true;
							break;
						default:
							CVSLog.logMsg
								( "While parsing CVSRequest '" + specification
								+ "', found invalid response flag '" +cmdChar+ "'" );
							break;
						}
					}

				// Process the ARGUMENTS if there are any...
				if ( argumentStr != null )
					{
					this.parseArgumentString( argumentStr );
					}
				}
			}
		else
			{
			result = false;
			}

		return result;
		}

	/**
	 * Returns the reason for the last verification failure.
	 *
	 * @return The reason for the last verification failure.
	 * @see CVSRequest#verifyRequest
	 * @see CVSRequest#setVerifyFailReason
	 */
	public String
	getVerifyFailReason()
		{
		return this.vfReason;
		}

	/**
	 * Sets the reason for the current verification failure.
	 *
	 * @param reason The reason for the current verification failure.
	 * @see CVSRequest#verifyRequest
	 * @see CVSRequest#getVerifyFailReason
	 */
	public void
	setVerifyFailReason( String reason )
		{
		this.vfReason = reason;
		}

	/**
	 * Verify the current request. This determines if the request
	 * has enough information to be handed to a CVSClient for
	 * processing. It also makes some sanity checks.
	 *
	 * @return True of the request is valid, false if not.
	 * @see CVSRequest#verifyRequest
	 * @see CVSRequest#getVerifyFailReason
	 */
	public boolean
	verifyRequest()
		{
		if ( this.hostName == null )
			{
			this.setVerifyFailReason( "hostname is null" );
			return false;
			}

		if ( this.repository == null )
			{
			this.setVerifyFailReason( "repository name is null" );
			return false;
			}

		if ( this.rootDirectory == null )
			{
			this.setVerifyFailReason( "root directory is null" );
			return false;
			}

		if ( this.localDirectory == null
			&& ( this.sendModifieds
				|| this.handleMerged
				|| this.handleUpdated
				|| this.handleCopyFile ) )
			{
			this.setVerifyFailReason( "local directory is null" );
			return false;
			}

		if ( this.entries == null )
			{
			if ( this.sendEntries
					|| this.sendEntryFiles )
				{
				this.setVerifyFailReason( "entries list is null" );
				return false;
				}
			}
/*
************************** REVIEW
		if ( this.sendEntries && this.sendSpecialMods )
			{
			this.setVerifyFailReason(
				"'send entries' (E or U) and " +
				"'send special mods' (N) " +
				"are mutually exclusive." );
			return false;
			}
**************************
*/
		if ( this.sendArguments && this.arguments == null )
			{
			this.setVerifyFailReason( "arguments list is null." );
			return false;
			}

		if ( this.entrySelector < CVSRequest.ES_FIRST
				|| this.entrySelector > CVSRequest.ES_LAST )
			{
			this.setVerifyFailReason( "invalid entry selector" );
			return false;
			}

		if ( this.sendEntryFiles && this.sendModule )
			{
			this.setVerifyFailReason
				( "can not send both 'files...' and 'module'" );
			return false;
			}

	/*
	*** UNDONE
		if ( !this.sendModule
				&& !this.sendEntryFiles
				&& !this.command.equals( "checkout" )
				&& !this.command.equals( "admin" )
				&& !this.command.equals( "

		this.guaranteeMsg = false;

		this.displayReponse = false;
		this.handleEntries = false;
		this.handleFlags = false;
	***
	*/

		this.setVerifyFailReason( "request is valid" );
		return true;
		}

	/**
	 * Returns a string representation of this request.
	 *
	 * @return String representing request.
	 */
	public String
	toString()
		{
		return "CVSRequest: command=" + this.command;
		}


	}
	   
