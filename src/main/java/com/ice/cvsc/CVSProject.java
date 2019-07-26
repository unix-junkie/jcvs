/*
** Java cvs client library package.
** Copyright (c) 1997 by Timothy Gerard Endres
** 
** This program is free software.
** 
** You may redistribute it and/or modify it under the terms of the GNU
** General Public License as published by the Free Software Foundation.
** Version 2 of the license should be included with this distribution in
** the file LICENSE, as well as License.html. If the license is not
** included	with this distribution, you may find a copy at the FSF web
** site at 'www.gnu.org' or 'www.fsf.org', or you may write to the
** Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139 USA.
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
import java.util.zip.*;

/**
 * The CVSProject class implements the concept of a local
 * CVS project directory. A local project directory can be
 * thought of as a local source code working directory that
 * contains a CVS directory containing CVS administration files.
 *
 * Combined with CVSClient, this class provides everything
 * you need to communicate with a CVS Server and maintain
 * local working directories for CVS repositories.
 *
 * @version $Revision: 2.24 $
 * @author Timothy Gerard Endres, <a href="mailto:time@ice.com">time@ice.com</a>.
 * @see CVSClient
 *
 */

public class
CVSProject extends Object
		implements CVSResponseHandler
	{
	static public final String		RCS_ID = "$Id: CVSProject.java,v 2.24 2000/06/11 05:07:06 time Exp $";
	static public final String		RCS_REV = "$Revision: 2.24 $";

	static private final String		INFO_PREFIX		= "#   ";
	static private final String		ERROR_PREFIX	= "*** ";
	static private final String		NOTICE_PREFIX	= "==> ";

	static public boolean		overTraceRequest	= false;
	static public boolean		overTraceResponse	= false;
	static public boolean		overTraceProcessing	= false;
	static public boolean		overTraceTCP		= false;

	static public boolean		deepDebug = false;
	static public boolean		debugEntryIO = false;


	private boolean			valid;
	private boolean			isPServer;
	private boolean			allowGzipFileMode;
	private int				gzipStreamLevel;

	private int				connMethod;
	private int				connPort;
	private String			serverCommand;
	private String			rshProcess;
	private String			userName;
	private String			password;

	private String			tempPath;
	private String			repository;
	private String			rootDirectory;
	private String			localRootDirectory;

	private String[]		setVars;

	private File			localRootDirFile;
	private File			localAdminDirFile;

	private CVSClient		client;
	private CVSIgnore		ignore;

	private CVSEntry		rootEntry;
	private Hashtable		pathTable;


	/**
	 * Determines if a pathname, provided by the dirName
	 * parameter, is a valid CVS administration directory
	 * (i.e., is a directory named 'CVS').
	 *
	 * @param dirName the pathname of the directory in question
	 */
	// UNDONE separator
	public static boolean
	isValidAdminPath( String dirName )
		{
		if ( ! CVSCUtilities.caseSensitivePathNames() )
			{
			dirName = dirName.toUpperCase();
			CVSTracer.traceIf( CVSProject.deepDebug,
				"CVSProject.isValidAdminPath:\n"
				+ "   adjusted dirName to '" + dirName + "'" );
			}

		return
			( dirName.endsWith( "/CVS" )
				|| dirName.endsWith( "/CVS/" ) );
		}

	/**
	 * Given a root path, returns the administration directory
	 * path corresponding to root's project.
	 *
	 * @param dirName the pathname of the root directory
	 */
	// UNDONE separator
	public static String
	rootPathToAdminPath( String dirName )
		{
		return
			dirName
			+ ( dirName.endsWith( "/" ) ? "" : "/" )
			+ "CVS";
		}

	/**
	 * Parses a valid CVS Administration Directory path
	 * and returns the pathname of the working directory
	 * that the administration directory belongs to. In
	 * other words, it returns the directory's parent.
	 *
	 * @param dirName the pathname of the admin directory
	 */
	// UNDONE separator
	public static String
	adminPathToRootPath( String dirName )
		{
		String path = dirName;

		if ( path.endsWith( "/" ) )
			{
			path = path.substring( 0, (path.length() - 1) );
			}

		int index = path.lastIndexOf( '/' );

		if ( index < 0 )
			{
			return path;
			}
		else
			{
			return path.substring( 0, index );
			}
		}

	/**
	 * Parses a valid CVS Entries File pathname and
	 * returns the pathname of the admin directory
	 * that the entries files belongs to. In other
	 * words, it returns the directory's parent.
	 *
	 * @param entriesPath The pathname of the Entries file.
	 */
	// UNDONE separator
	public static String
	entriesPathToAdminPath( String entriesPath )
		{
		int	index = entriesPath.lastIndexOf( '/' );

		if ( index < 0 )
			{
			// UNDONE
			return null;
			}

		return entriesPath.substring( 0, index );
		}

	/**
	 * Verifies that a directory path is a valid CVS
	 * administration directory. This checks for the
	 * correct name ('CVS'), and that the necessary
	 * files ('Entries', 'Root' and 'Repository') are
	 * present.
	 *
	 * @param dirName the pathname of the admin directory
	 * @return true if directory is valid, otherwise false
	 */

	public static boolean
	verifyAdminDirectory( String dirName )
		{
		File	file;

		CVSTracer.traceIf(
			(CVSProject.deepDebug || CVSProject.debugEntryIO),
			"CVSProject.verifyAdminDirectory:\n"
			+ "   dirName = '" + dirName + "'" );

		if ( ! CVSProject.isValidAdminPath( dirName ) )
			{
			CVSTracer.traceIf( (CVSProject.deepDebug || CVSProject.debugEntryIO),
				"CVSProject.verifyAdminDirectory:\n"
				+ "   IS NOT a valid admin directory." );
			return false;
			}

		// NOTE
		// Do NOT export until after the verify, as it uses slashes!
		//
		dirName =
			CVSCUtilities.exportPath
				( CVSCUtilities.stripFinalSlash( dirName ) );

		file = new File( dirName, "Entries" );
		if ( ! file.exists() )
			{
			CVSTracer.traceIf(
				(CVSProject.deepDebug || CVSProject.debugEntryIO),
				"CVSProject.verifyAdminDirectory:\n"
				+ "   DOES NOT EXIST --> 'Entries'." );
			return false;
			}

		file = new File( dirName, "Repository" );
		if ( ! file.exists() )
			{
			CVSTracer.traceIf(
				(CVSProject.deepDebug || CVSProject.debugEntryIO),
				"CVSProject.verifyAdminDirectory:\n"
				+ "   DOES NOT EXIST --> 'Repository'." );
			return false;
			}

		file = new File( dirName, "Root" );
		if ( ! file.exists() )
			{
			CVSTracer.traceIf(
				(CVSProject.deepDebug || CVSProject.debugEntryIO),
				"CVSProject.verifyAdminDirectory:\n"
				+ "   DOES NOT EXIST --> 'Root'." );
			return false;
			}

		return true;
		}

	/**
	 * Given the administrative directory pathname, return
	 * the full pathname of the 'Entries' file.
	 *
	 * @param adminDirPath The pathname of the admin ('CVS') directory.
	 */
	// UNDONE separator
	public static String
	getAdminEntriesPath( String adminDirPath )
		{
		return ( adminDirPath + "/Entries" );
		}

	/**
	 * Given the administrative directory pathname, return
	 * the full pathname of the 'Repository' file.
	 *
	 * @param adminDirPath The pathname of the admin ('CVS') directory.
	 */
	// UNDONE separator
	public static String
	getAdminRepositoryPath( String adminDirPath )
		{
		return ( adminDirPath + "/Repository" );
		}

	/**
	 * Given the administrative directory pathname, return
	 * the full pathname of the 'Root' file.
	 *
	 * @param adminDirPath The pathname of the admin ('CVS') directory.
	 */
	// UNDONE separator
	public static String
	getAdminRootPath( String adminDirPath )
		{
		return ( adminDirPath + "/Root" );
		}

	/**
	 * Given the administrative directory pathname, return
	 * the full pathname of the 'Notify' file.
	 *
	 * @param adminDirPath The pathname of the admin ('CVS') directory.
	 */
	// UNDONE separator
	public static String
	getAdminNotifyPath( String adminDirPath )
		{
		return ( adminDirPath + "/Notify" );
		}

	/**
	 * Given the administrative directory pathname, return
	 * the full pathname of the project preferences file.
	 *
	 * @param adminDirPath The pathname of the admin ('CVS') directory.
	 */
	// UNDONE separator
	public static String
	getAdminPrefsPath( String adminDirPath )
		{
		return ( adminDirPath + "/jcvs.txt" );
		}

	/**
	 * Constructs a new CVSProject object.
	 */
	public
	CVSProject()
		{
		super();

		this.initFields();

		this.client = null;
		}

	/*
	 * Constructs a new CVSProject object with the
	 * provided pro.
	 *
	public
	CVSProject( String projectName )
		{
		super();

		this.initFields();

		this.projectName = projectName;
		}
	 *
	 */

	/**
	 * Constructs a new CVSProject object, setting the
	 * project's client to the one provided.
	 *
	 * @param client A CVSClient object to be used by this
	 * project for all CVS server requests.
	 */
	public
	CVSProject( CVSClient client )
		{
		super();

		this.initFields();

		this.client = client;
		}

	/**
	 * Internal nethod used by constructors to initialize
	 * the project's fields.
	 */
	private void
	initFields()
		{
		this.valid = false;
		this.isPServer = false;
		this.allowGzipFileMode = true;
		this.gzipStreamLevel = 0;

		this.userName = "";

		// NOTE password == 'null' indicates "no login yet"
		this.password = null;

		this.connMethod = CVSRequest.METHOD_RSH;
		this.connPort = CVSClient.DEFAULT_CVS_PORT;
		this.serverCommand = "cvs server";
		this.rshProcess = null;

		this.repository = null;
		this.rootDirectory = null;
		this.localRootDirectory = null;

		this.client = null;

		this.setVars = null;

		this.ignore = new CVSIgnore();

		this.rootEntry = null;
		this.pathTable = new Hashtable();

		this.tempPath = null;

		this.localRootDirFile = null;
		this.localAdminDirFile = null;
		}

	/**
	 * Returns the client this project is set to use.
	 * 
	 * @return the project's client.
	 * @see CVSClient
	 */
	
	public CVSClient
	getClient()
		{
		return this.client;
		}

	public void
	setClient( CVSClient client )
		{
		this.client = client;
		}
	
	public String
	getRepository()
		{
		return this.repository;
		}
	
	public void
	setRepository( String repository )
		{
		this.repository = repository;
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
	
	public boolean
	allowsGzipFileMode()
		{
		return this.allowGzipFileMode;
		}

	public void
	setAllowsGzipFileMode( boolean allow )
		{
		this.allowGzipFileMode = allow;
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

	public String
	getUserName()
		{
		return this.userName;
		}
	
	public void
	setUserName( String name )
		{
		this.userName = name;
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
	getRootDirectory()
		{
		return this.rootDirectory;
		}
	
	public void
	setRootDirectory( String rootDirectory )
		{
		this.rootDirectory = rootDirectory;
		}
	
	/**
	 * Returns the <em>full</em> local pathname for the
	 * root directory of this project.
	 *
	 * @return Full pathname of project's local root directory.
	 */
	
	public String
	getLocalRootPath()
		{
		return this.localRootDirectory;
		//		+ "/" + this.rootEntry.getName();
		}
	
	public String
	getLocalRootDirectory()
		{
		return this.localRootDirectory;
		}
	
	public void
	setLocalRootDirectory( String dirName )
		{
		this.localRootDirectory = dirName;

		this.localRootDirFile = new File( dirName );

		this.localAdminDirFile = // UNDONE separator
			new File( dirName + "/CVS" );
		}

	public String
	getTempDirectory()
		{
		return this.tempPath;
		}
	
	public void
	setTempDirectory( String dirName )
		{
		this.tempPath = dirName;
		if ( this.client != null )
			{
			this.client.setTempDirectory( dirName );
			}
		}

	public int
	getConnectionPort()
		{
		return this.connPort;
		}

	public void
	setConnectionPort( int port )
		{
		this.connPort = port;
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
	getServerCommand()
		{
		return this.serverCommand;
		}

	public void
	setServerCommand( String command )
		{
		this.serverCommand = command;
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

	/**
	 * Returns the project's user set variables.
	 *
	 * @return The project's user set variables.
	 */
	public String[]
	getSetVariables()
		{
		return this.setVars;
		}

	/**
	 * Sets the project's user set variables.
	 *
	 * @param vars The new user set variables.
	 */
	public void
	setSetVariables( String[] vars )
		{
		this.setVars = vars;
		}

	public CVSEntry
	getRootEntry()
		{
		return this.rootEntry;
		}

	public File
	getEntryFile( CVSEntry entry )
		{
		String	relPath;

		relPath = entry.getFullName();

		File file = new
			File( CVSCUtilities.exportPath
						( this.localRootDirFile.getPath() ),
					CVSCUtilities.exportPath
						( entry.getFullPathName() ) );

		if ( CVSProject.deepDebug )
		CVSTracer.traceIf( false,
			"CVSProject.getEntryFile: relPath '" +relPath+
				"' localRootDir '" +this.localRootDirFile.getPath()+ 
				"' result '" +file.getPath()+ "'" );

		return file;
		}

	public boolean
	hasValidLogin( String userName )
		{
		if ( this.userName.equals( userName ) )
			if ( this.password != null )
				return true;

		return false;
		}

	public void
	addEntryNotify( CVSEntryVector entries, String type, String options )
		{
		PrintWriter	out;
		String		noteLine;

		// REVIEW
		// UNDONE
		// We are INCOMPATIBLE with the cvs command line here!!!
		// The command line stores this file in each working directory's
		// admin directory, NOT in the root admin like we are! This means
		// that the command line will get all of our notifies, ( will it
		// choke on them?), but we will not get all of the command line's.
		// Why?! Performance. Does it matter?
		// 
		String	fileName =
 			CVSProject.getAdminNotifyPath(
				CVSProject.rootPathToAdminPath
					( this.getLocalRootPath() ) );

		try
			{
			out = new PrintWriter(
					new FileWriter( fileName, true ) );
			}
		catch ( IOException ex )
			{
			CVSTracer.traceWithStack(
				"ERROR opening Notification file '"
				+ fileName + "' for append" );
			return;
			}

		CVSTimestamp now = new CVSTimestamp();
		CVSTimestampFormat	stamper =
			CVSTimestampFormat.getInstance();

		String stampStr = stamper.format( now );

		for ( int eIdx = 0 ; entries != null
				&& eIdx < entries.size() ; ++eIdx )
			{
			CVSEntry entry = entries.entryAt(eIdx);
			if ( entry != null )
				{
				out.println(
					type + entry.getName()
					+ "\t" + stamper.format( now ) + " GMT"
					+ "\t" + "remote.via.jCVS"
					+ "\t" + entry.getLocalDirectory()
					+ "\t" + options
					);
				}
			else
				{
				CVSTracer.traceWithStack
					( "NULL ENTRY["+eIdx+"] on index '" + eIdx + "'" );
				}
			}

		out.flush();
		out.close();
		}

	public void
	includeNotifies( CVSRequest request )
		{
		BufferedReader	in;
		String			noteLine;

		request.notifies = new Vector();

		if ( this.rootEntry == null )
			return;

		File notFile = new File(
			CVSProject.getAdminNotifyPath(
				CVSProject.rootPathToAdminPath
					( this.getLocalRootPath() ) ) );

		if ( notFile.exists() )
			{
			try
				{
				in = new BufferedReader(
						new FileReader( notFile ) );
				}
			catch ( IOException ex )
				{
				CVSLog.logMsg
					( "ERROR opening Notification file '"
						+ notFile.getPath() + "'" );
				return;
				}

			for ( ; ; )
				{
				try { noteLine = in.readLine(); }
				catch ( IOException ex )
					{
					CVSLog.logMsg
						( "ERROR reading Notification file '"
							+ notFile.getPath() + "'" );
					noteLine = null;
					}
				
				if ( noteLine == null )
					break;

				CVSNotifyItem notifyItem =
					parseNotifyLine( noteLine );

				if ( notifyItem != null )
					{
					request.notifies.addElement( notifyItem );
					}
				else
					{
					 CVSLog.logMsg
						("ERROR bad 'CVS/Notify' line:\n"
							+ "   " + noteLine );
					}
				}

			try { in.close(); }
				catch ( IOException ex ) { }
			}
		}

	public boolean
	verifyPassword( CVSUserInterface ui, String userName, String password, boolean trace )
		{
		CVSRequest	request;
		boolean		result = false;

		if ( ! this.isPServer() )
			return true;

		if ( this.hasValidLogin( userName ) )
			return true;

		String scrambled =
			CVSScramble.scramblePassword( password, 'A' );

		request = new CVSRequest();

		request.setPServer( true );
		request.setUserName( userName );
		request.setPassword( scrambled );

		request.setPort( this.getClient().getPort() );
		request.setHostName( this.getClient().getHostName() );

		request.setRepository( this.repository );
		request.setRootDirectory( this.rootDirectory );
		request.setLocalDirectory( this.localRootDirectory );

		request.verificationOnly = true;

		request.traceRequest = trace;
		request.traceResponse = trace;
		request.traceProcessing = trace;
		request.traceTCPData = trace;
		request.allowGzipFileMode = this.allowGzipFileMode;
		request.gzipStreamLevel = this.gzipStreamLevel;

		request.setUserInterface( ui );

		CVSResponse response =
			client.processCVSRequest( request );

		if ( response.getStatus() == CVSResponse.OK )
			{
			result = true;
			this.setUserName( userName );
			this.setPassword( scrambled );
			response.appendStdout
				( "Authentication of '" +userName+ "' succeeded.\n" );
			}
		else
			{
			result = false;
			this.password = null;
			response.appendStdout
				( "Authentication of '" +userName+ "' failed.\n" );
			}
		
		if ( ui != null && response != null )
			ui.uiDisplayResponse( response );

		if ( response != null
				&& ! request.saveTempFiles )
			response.deleteTempFiles();

		return result;
		}

	/**
	 * Given a repository path, which was not found in the
	 * pathTable, determine if the path is in the table if
	 * case is ignored. This is to support platforms which
	 * have case insensitive path names.
	 *
	 * @param subPath The path to check for in the table.
	 * @return The CVSEntry representing the path's directory.
	 */
	CVSEntry 
	getPathIgnoringCase( String subPath )
		{
		Enumeration enum = this.pathTable.keys();

		for ( ; enum.hasMoreElements() ; )
			{
			String key = (String) enum.nextElement();

			if ( key.equalsIgnoreCase( subPath ) )
				{
				return (CVSEntry) this.pathTable.get( key );
				}
			}

		return null;
		}

	/**
	 * Given a 'local directory' (in the protocol sense), get the
	 * corresponding directory CVSEntry. This method will return
	 * null if the directory hierarchy has not been "ensured" yet.
	 *
	 * @param localDir The directory's 'local directory' name.
	 */

	public CVSEntry
	getDirEntryForLocalDir( String localDir )
		{
		return this.getPathTableEntry( localDir );
		}

	private CVSEntry
	getPathTableEntry( String path )
		{
		CVSEntry result = null;

		result = (CVSEntry) this.pathTable.get( path );

		if ( result == null && ! CVSCUtilities.caseSensitivePathNames() )
			{
			result = this.getPathIgnoringCase( path );
			if ( CVSProject.deepDebug )
				CVSTracer.traceIf( true,
					"getPathTableEntry: CASE INsensitive TABLE CHECK\n"
					+ "   result    '"
					+ (result == null ? "(null)" : result.getName())
					+ "'\n"
					+ "   reposirory  '"
					+ (result != null ? result.getRepository() : "null")
					+ "'" );
			}

		return result;
		}

	private CVSEntry
	reversePathTableEntry( String repository )
		{
		CVSEntry result = null;

		Enumeration enum = this.pathTable.keys();
		for ( boolean match = false ; !match && enum.hasMoreElements() ; )
			{
			String localDir = (String) enum.nextElement();
			CVSEntry tblEntry = (CVSEntry) this.pathTable.get( localDir );

			if ( CVSCUtilities.caseSensitivePathNames() )
				match = repository.equals( tblEntry.getRepository() );
			else
				match = repository.equalsIgnoreCase( tblEntry.getRepository() );

			if ( match )
				result = tblEntry;
			}

		if ( CVSProject.deepDebug )
			CVSTracer.traceIf( true,
				"CVSProject.reversePathTableEntry:\n"
				+ "   repository = '" + repository + "'\n"
				+ "   RESULT =\n"
				+ (result == null ? "(null)" : result.dumpString( "   " )) );

		return result;
		}

	/**
	 * Guarentees that the repository contains the path specified.
	 * This will in turn invoke server commands to create the
	 * directories needed to make the path exist, so this can and
	 * will change the repository on the server. The repositoryPath
	 * is relative to the repository's root directory.
	 *
	 * @param ui The CVS User Interface to display the progress.
	 * @param localDirectory The <em>relative</em> path to ensure.
	 * @return A CVSResponse with the results of each directory 'add'.
	 */
	public CVSResponse
	ensureRepositoryPath
			( CVSUserInterface ui, String localDirectory, CVSResponse resultResp )
		{
		int			index;
		CVSEntry	dirEntry;
		CVSRequest	request;
		boolean		result;

		CVSTracer.traceIf( CVSProject.deepDebug,
			"CVSProject.ensureRepositoryPath: \n"
				+ "   localDirectory '" + localDirectory + "'" );
		
		CVSEntryVector entries = new CVSEntryVector();

		// Since we will be re-using this vector possibly many times,
		// we can't keep appending. Thus, we append once here to fill
		// in slots zero and one, then use setEntry() below
		//
		entries.appendEntry( null );
		entries.appendEntry( null );

		CVSTracer.traceIf( CVSProject.deepDebug,
			"ensureRepositoryPath: ROOT =\n   " + this.rootEntry.dumpString() );

		// The root directory has to exist by this point.
		String repository = this.rootEntry.getRepository();
		CVSTracer.traceIf( CVSProject.deepDebug,
			"ensureRepositoryPath: rootEntry repository = '"
			+ repository + "'" );

		resultResp.setStatus( CVSResponse.OK );

		CVSEntry parentEntry = this.rootEntry;

		for ( int offset = 2 ; ; )
			{
			index = localDirectory.indexOf( '/', offset );

			CVSTracer.traceIf( CVSProject.deepDebug,
				"ensureRepositoryPath: indexOf( '/',"
					+ offset + " ) = " + index );

			if ( index < 0 )
				{
				CVSTracer.traceIf( CVSProject.deepDebug,
					"ensureRepositoryPath: DONE w/ REMAINDER '"
					+ localDirectory.substring( offset ) + "'" );
				break;
				}

			offset = index + 1;
			String localDir = localDirectory.substring( 0, index + 1 );
			dirEntry = this.getPathTableEntry( localDir );

			CVSTracer.traceIf( CVSProject.deepDebug,
				"ensureRepositoryPath: localDir '" + localDir + "' returns "
				+ ( dirEntry==null ? "null" : dirEntry.dumpString() ) );

			if ( dirEntry != null )
				{
				if ( CVSProject.deepDebug )
				CVSTracer.traceIf( true,
					"ensureRepositoryPath: EXISTING DIRECTORY '" + localDir + "'\n"
					+ "   localDir    '" + dirEntry.getLocalDirectory() + "'\n"
					+ "   repository  '" + dirEntry.getRepository() + "'" );
				parentEntry = dirEntry;
				continue;
				}
			
			CVSTracer.traceIf( CVSProject.deepDebug,
				"ensureRepositoryPath: NEW CVS DIRECTORY '" + localDir + "'\n"
				+ "   Parent LocalDirectory '" + parentEntry.getLocalDirectory() + "'\n"
				+ "   Parent Repository     '" + parentEntry.getRepository() + "'" );

			request = new CVSRequest();

			String name = localDir.substring( 0, localDir.length() - 1 );
			index = name.lastIndexOf( '/' );
			if ( index >= 0 && index < (name.length() - 1) )
				{
				name = name.substring( index + 1 );
				}

			String rootDir =
				CVSCUtilities.ensureFinalSlash( this.getRootDirectory() );

			dirEntry = new CVSEntry();
			dirEntry.setName( name );
			dirEntry.setLocalDirectory( localDir );
			dirEntry.setRepository
				( parentEntry.getRepository() + "/" + name );
			// We need this next line to mark dirEntry as a directory!!
			dirEntry.setDirectoryEntryList( new CVSEntryVector() );

			entries.setElementAt( parentEntry, 0 );
			entries.setElementAt( dirEntry, 1 );
			request.setEntries( entries );

			CVSTracer.traceIf( CVSProject.deepDebug,
				"ensureRepositoryPath: DIR ENTRY\n"
				+ "   Name       " + dirEntry.getName() + "\n"
				+ "   LocalDir   " + dirEntry.getLocalDirectory() + "\n"
				+ "   Repository " + dirEntry.getRepository() );

			CVSTracer.traceIf( CVSProject.deepDebug,
				"ensureRepositoryPath: PARENT ENTRY\n"
				+ "   Name       " + parentEntry.getName() + "\n"
				+ "   LocalDir   " + parentEntry.getLocalDirectory() + "\n"
				+ "   Reposirory " + parentEntry.getRepository() );

			request.execInCurDir = true;
			request.setDirEntry( parentEntry );

			request.sendEntries = true;
			request.sendArguments = true;
			request.sendEntryFiles = false;

			request.traceRequest = true; // CVSProject.overTraceRequest;
			request.traceResponse = true; // CVSProject.overTraceResponse;
			request.traceTCPData = true; // CVSProject.overTraceTCP;
			request.traceProcessing = true; // CVSProject.overTraceProcessing;

			request.allowGzipFileMode = this.allowGzipFileMode;
			request.gzipStreamLevel = this.gzipStreamLevel;

			request.setUserName( this.userName );
			request.setPServer( this.isPServer() );
			if ( this.isPServer() )
				{
				request.setPassword( this.password );
				}

			request.setPort( this.getClient().getPort() );
			request.setHostName( this.getClient().getHostName() );
			request.setRshProcess( this.getRshProcess() );
			request.setPort( this.getConnectionPort() );
			request.setConnectionMethod( this.getConnectionMethod() );

			request.setRepository( this.repository );
			request.setRootRepository( this.rootEntry.getRepository() );
			request.setRootDirectory( this.rootDirectory );
			request.setLocalDirectory( this.localRootDirectory );

			request.setServerCommand( this.getServerCommand() );
			request.setSetVariables( this.setVars );
			this.establishNewDirSticky( request, dirEntry );
			this.establishStickys( request );
			this.establishStatics( request );

			request.setCommand( "add" );
			
			request.setUserInterface
				( (ui == null) ? (CVSUserInterface)this : ui );
			request.includeNotifies = false;
			request.queueResponse = true;

			CVSArgumentVector arguments = new CVSArgumentVector();
			arguments.appendArgument( name );
			request.setArguments( arguments );

			CVSResponse response =
				client.processCVSRequest( request );

			response.deleteTempFiles(); // There shouldn't be any...

			String err = response.getStderr();
			if ( err != null && err.length() > 0 )
				resultResp.appendStderr( err );

			String out = response.getStdout();
			if ( out != null && out.length() > 0 )
				resultResp.appendStdout( out );

			if ( response.getStatus() == CVSResponse.OK )
				{
				CVSTracer.traceIf( CVSProject.deepDebug,
					"ensureRepositoryPath: ensureEntryHierarchy( "
						+ dirEntry.getLocalDirectory() + ", "
						+ dirEntry.getRepository() + " )" );

				this.ensureEntryHierarchy
					( dirEntry.getLocalDirectory(),
						dirEntry.getRepository() );

				dirEntry = this.getPathTableEntry( localDir );
				if ( dirEntry == null )
					{
					CVSTracer.traceWithStack
						( "WHAT?! ensured, but no pathTable entry '"
							+ localDir + "'?!?!" );
					}
				else
					{
					dirEntry.setDirty( true );
					}

				this.ensureProperWorkingDirectory
					( this.localRootDirectory, localDir, true );
				}
			else
				{
				resultResp.setStatus( CVSResponse.ERROR );
				resultResp.appendStdOut( response.getStdout() );
				resultResp.appendStdErr( response.getStderr() );

				if ( request.getUserInterface() != null )
					request.getUserInterface().uiDisplayResponse( resultResp );

				CVSTracer.traceIf( true,
					"ensureRepositoryPath: ERROR! SERVER RESPONSE:\n"
					+ response.getStderr() + "\n" + response.getStdout() );

				break;
				}
			
			repository = repository + "/" + name;
			}

		return resultResp;
		}

	private String
	getStickyTagspec( CVSEntry entry )
		{
		String result = "";

		String rootPath =
			CVSProject.rootPathToAdminPath
				( this.getLocalRootDirectory()
					+ "/" + entry.getLocalPathName() );

		File stickyFile = new File( rootPath, "Tag" );
		if ( stickyFile.exists() )
			{
			try {
				result = CVSCUtilities.readStringFile( stickyFile );
				}
			catch ( IOException ex )
				{
				result = "";
				}

			if ( ! ( result.startsWith( "D" )
					|| result.startsWith( "T" )
						// REVIEW - Where does N enter the picture?!
						//          appears to be on "cvs add dir".
						|| result.startsWith( "N" ) ) )
				{
				result = "";
				}
			}

		return result;
		}

	/**
	 * This methods deal with adding a new directory which is not
	 * yet in the repository, and yet, whose parent directory has
	 * a sticky tag set. We wish to "inherit" that tag...
	 */

	public void
	establishNewDirSticky( CVSRequest request, CVSEntry entry )
		{
		Hashtable stickys = request.getStickys();
		if ( stickys == null )
			stickys = new Hashtable();

		String localDir = entry.getLocalDirectory();
		String parentDir = CVSCUtilities.getLocalParent( localDir );

		String rootPath =
			CVSProject.rootPathToAdminPath
				( this.getLocalRootDirectory() + "/" + parentDir );

		String tagSpec = "";
		File stickyFile = new File( rootPath, "Tag" );
		if ( stickyFile.exists() )
			{
			try { tagSpec = CVSCUtilities.readStringFile( stickyFile ); }
				catch ( IOException ex ) { ex.printStackTrace(); }
			}

		if ( tagSpec.length() > 0 )
			{
			rootPath =
				CVSProject.rootPathToAdminPath
					( this.getLocalRootDirectory() + "/" + localDir );

			File adminDir = new File( rootPath );
			adminDir.mkdirs();
			stickyFile = new File( rootPath, "Tag" );
			try {
				if ( ! stickyFile.exists() )
					{
					CVSCUtilities.writeStringFile( stickyFile, tagSpec );
					}
				}
			catch ( IOException ex ) { ex.printStackTrace(); }
			}

		stickys.put( localDir, tagSpec );
		stickys.put( parentDir, tagSpec );
		request.setStickys( stickys );
		}

	public void
	establishStickys( CVSRequest request )
		{
		Hashtable stickys = new Hashtable();

		CVSEntryVector entries = request.getEntries();
		for ( int i = 0, sz = entries.size() ; i < sz ; ++i )
			{
			CVSEntry entry = (CVSEntry) entries.elementAt(i);
			String localDir = entry.getLocalDirectory();
			if ( stickys.get( localDir ) == null )
				{
				String tagSpec = this.getStickyTagspec( entry );
				stickys.put( localDir, tagSpec );
				}
			}

		if ( stickys.size() > 0 )
			request.setStickys( stickys );
		}

	private boolean
	isStaticDirectory( CVSEntry entry )
		{
		String rootPath =
			CVSProject.rootPathToAdminPath
				( this.getLocalRootDirectory()
					+ "/" + entry.getLocalPathName() );

		File staticFile = new File( rootPath, "Entries.static" );

		return staticFile.exists();
		}

	public void
	establishStatics( CVSRequest request )
		{
		Hashtable statics = new Hashtable();

		CVSEntryVector entries = request.getEntries();
		for ( int i = 0, sz = entries.size() ; i < sz ; ++i )
			{
			CVSEntry entry = (CVSEntry) entries.elementAt(i);
			String localDir = entry.getLocalDirectory();
			if ( statics.get( localDir ) == null )
				{
				if ( this.isStaticDirectory( entry ) )
					statics.put( localDir, "" );
				}
			}

		if ( statics.size() > 0 )
			request.setStatics( statics );
		}

	public boolean
	performCVSRequest( CVSRequest request )
		{
		return this.performCVSRequest( request, new CVSResponse() );
		}

	public boolean
	performCVSRequest( CVSRequest request, CVSResponse response )
		{
		boolean result = true;

		request.setUserName( this.userName );
		request.setPServer( this.isPServer() );

		if ( this.isPServer() )
			{
			request.setPassword( this.password );
			}

		String	rootRepository;
		if ( this.rootEntry != null )
			{
			rootRepository = this.rootEntry.getRepository();

			//
			// SPECIAL CASE
			// When we are asked to send the "module name" as an argment,
			// we have a problem. The module name "must" always be "."
			// (see CVSClient.c) to be correct. However, if the rootRepository
			// is sent from the root, then "." will be wrong. Thus, we will
			// override it here to make it right for this one case.
			//
			// NOTE Release 5.0.8: I do not know the source of this original
			//      "fix", but it breaks the "Update" module command (nothing
			//      happens at the top level). Removing this did not appear
			//      to break anything. So far...
			//
			/*
			if ( request.sendModule )
				{
				CVSEntry repEnt = this.rootEntry.getEntryList().getEntryAt(0);
				CVSTracer.traceIf( this.deepDebug,
					"CVSProject.performCVSRequest: APPLY MODULE NAME HACK\n"
					+ "   repEnt =\n"
					+ (repEnt==null?"NULL":repEnt.dumpString( "   " )) );

				if ( repEnt != null )
					{
					rootRepository = repEnt.getRepository();
					}
				}
			*/
			}
		else
			{
			// This is 'checkout' or 'export' case.
			if ( this.repository.equals( "." ) )
				rootRepository = this.rootDirectory;
			else
				rootRepository =
					this.rootDirectory + "/" + this.repository;
			}

		request.setHostName( this.getClient().getHostName() );
		request.setRepository( this.repository );
		request.setRootRepository( rootRepository );
		request.setRootDirectory( this.rootDirectory );
		request.setLocalDirectory( this.localRootDirectory );
		request.setPort( this.getConnectionPort() );
		request.setConnectionMethod( this.getConnectionMethod() );
		request.setServerCommand( this.getServerCommand() );
		request.setRshProcess( this.getRshProcess() );
		
		request.setSetVariables( this.setVars );
		
		this.establishStickys( request );

		this.establishStatics( request );

		if ( request.includeNotifies )
			{
			this.includeNotifies( request );
			}

		if ( ! request.queueResponse )
			if ( request.responseHandler == null )
				request.responseHandler = this;

		if ( CVSProject.overTraceRequest )
			request.traceRequest = CVSProject.overTraceRequest;
		if ( CVSProject.overTraceResponse )
			request.traceResponse = CVSProject.overTraceResponse;
		if ( CVSProject.overTraceProcessing )
			request.traceProcessing = CVSProject.overTraceProcessing;
		if ( CVSProject.overTraceTCP )
			request.traceTCPData = CVSProject.overTraceTCP;

		request.allowGzipFileMode = this.allowGzipFileMode;
		request.gzipStreamLevel = this.gzipStreamLevel;

		if ( ! request.verifyRequest() )
			{
			CVSLog.logMsg
				( "CVSProject.performCVSRequest: BAD CVSRequest: '"
					+ request.getVerifyFailReason() + "'" );
			return false;
			}
		else
			{
			this.client.processCVSRequest( request, response );

			this.processCVSResponse( request, response );

			if ( request.getCommand().equals( "update" )
				&&	( request.getArguments().containsArgument( "-P" )
					|| request.getArguments().containsArgument( "-r" )
					|| request.getArguments().containsArgument( "-D" ) ) )
				{
				this.pruneEmptySubDirs( request.handleEntries );
				}

			if ( request.getUserInterface() != null && response != null )
				request.getUserInterface().uiDisplayResponse( response );

			if ( response != null && ! request.saveTempFiles )
				response.deleteTempFiles();

			return ( response.getStatus() == CVSResponse.OK );
			}
		}

	public CVSEntry
	entryLineToEntry( String entryLine )
		{
		CVSEntry entry = new CVSEntry();

		try {
			entry.parseEntryLine( entryLine, true );
			}
		catch ( ParseException ex )
			{
			entry = null;
			CVSLog.traceMsg
				( ex, "CVSProject.entryFromEntryLine: ERROR "
					+ "could not process entry line '" + entryLine );
			}
		
		return entry;
		}

	public File
	getLocalEntryFile( CVSEntry entry )
		{
		File result = new File
			( CVSCUtilities.exportPath( this.localRootDirectory ),
				CVSCUtilities.exportPath( entry.getFullPathName() ) );

		return result;
		}

	/**
	 * Given a local-directory returned from the server,
	 * make sure the local-directory is in a format that
	 * jCVS can make use of (i.e., via the pathTable).
	 * Currently, the only case handled is when local-directory
	 * is './', which forces us to locate from pathTable.
	 *
	 * @param pathName The local-directory from the server.
	 * @param repository The repository the server sent with this local-directory.
	 * @return The normalized local-directory, or null if it does not exist.
	 */

	public String
	normalizeLocalDirectory( String pathName, String repository )
		{
		String result = pathName;

		if ( pathName.equals( "./" ) )
			{
			CVSTracer.traceIf( this.deepDebug,
				"normalizeLocalDirectory: SPECIAL './' CASE.\n"
				+ "    pathName '" + pathName + "'\n"
				+ "  repository '" + repository + "'" );

			// SPECIAL CASE
			// Here, we have a case where a command executed
			// in a subdirectory (or root), and instead of the
			// usual root-based local-directory, we get this.
			// We need to take the repository and reverse lookup
			// the local-directory.

			CVSEntry revEntry =
				this.reversePathTableEntry( repository );

			if ( revEntry != null )
				{
				result = revEntry.getLocalDirectory();
				}
			else
				{
				result = null;
				CVSTracer.traceIf( true,
					"COULD NOT RESOLVE '" + pathName
					+ "' with '" + repository + "'" );
				CVSTracer.traceWithStack(
					"COULD NOT RESOLVE '" + pathName
					+ "' with '" + repository + "'" );
				}
			}

		CVSTracer.traceIf( this.deepDebug,
			"normalizeLocalDirectory: RESULT '"
			+ pathName + "' ---> '" + result + "'" );

		return result;
		}

	public CVSEntry
	createItemEntry( CVSResponseItem item )
		{
		CVSEntry	entry;
		String		entryLine = item.getEntriesLine();

		CVSTracer.traceIf( this.deepDebug,
			"createItemEntry:\n"
			+ "   item.getPathName    '" + item.getPathName() + "'\n"
			+ "   item.repositoryName '" + item.getRepositoryName() + "'\n"
			+ "   item.getEntriesLine '" + item.getEntriesLine() + "'" );

		// NOTE
		// When the entryLine is null, all we are interested
		// in is the name, localDirectory, and repository...
		//
		if ( entryLine == null )
			entry = new CVSEntry();
		else
			entry = this.entryLineToEntry( item.getEntriesLine() );

		if ( entry != null )
			{
			String repos = item.getRepositoryName();
			int index = repos.lastIndexOf( '/' );

			if ( index < 0 )
				{
				CVSTracer.traceWithStack(
					"CVSProject.createItemEntry: ERROR "
					+ "repository '" + repos + "' has no slash!" );
				entry.setName( repos );
				entry.setRepository( "" );
				}
			else
				{
				entry.setName( repos.substring( index + 1 ) );
				entry.setRepository( repos.substring( 0, index ) );
				}

			String localDir =
				this.normalizeLocalDirectory
					( item.getPathName(), entry.getRepository() );

			entry.setLocalDirectory( localDir );
			}

		return entry;
		}

	public boolean
	handleResponseItem(
			CVSRequest request, CVSResponse response, CVSResponseItem item )
		{
		boolean result;

		CVSTracer.traceIf( request.traceProcessing,
			"CVSProject.handleResponseItem:\n   " + item.toString() );

		result = this.processResponseItem( request, response, item );

		if ( ! request.saveTempFiles )
			{
			item.deleteFile();
			}

		return result;
		}

	public boolean
	processCVSResponse( CVSRequest request, CVSResponse response )
		{
		int			idx;
		boolean		ok;
		CVSEntry	entry = null;
		boolean		result = true;
		File		localFile = null;
		CVSResponseItem	item = null;

		if ( response == null )
			return true;

		// NOTE
		// We process the item list, EVEN when !queueResponse,
		// since the responseHandler may have queued some of the
		// response items for processing here!!!
		//
		CVSRespItemVector items = response.getItemList();

		for ( idx = 0 ; result && idx < items.size() ; ++idx )
			{
			item = items.itemAt( idx );

			CVSTracer.traceIf( request.traceProcessing,
				"CVSResponse: item[" +idx+ "] type '"
				+ item.getType() + "'");

			result =
				this.processResponseItem
					( request, response, item );
			}

		if ( response.getStatus() != CVSResponse.OK )
			{
			if ( request.traceProcessing )
				CVSTracer.traceIf( true,
					"CVSProject.processCVSResponse: ERROR errorCode '"
					+ response.getErrorCode() + "' errorText '"
					+ response.getErrorText() + "'" );

			if ( response.getErrorCode().length() > 0
					|| response.getErrorText().length() > 0 )
				{
				response.appendStderr
					( "\nError Code '" + response.getErrorCode() + "'"
						+ " Message '" + response.getErrorText() + "'\n" );
				}
			}
		else
			{
			CVSTracer.traceIf( request.traceProcessing,
				"CVSProject.processCVSResponse: OK" );

			if ( request.handleEntries )
				{
				this.writeAdminFiles();
				}
			}

		// REVIEW - Should error results with empty code and text
		//          be ignored by default? This is the 'Diff' case!
		//
		//          Actually, I think I want to special case 'diff'
		//          here and use the 'ERROR' status to indicate that
		//          there were 'no differences'. I think I can check
		//          for an empty 'code' or 'message' to confirm that
		//          it is 'no diffs' case and not 'some diff error'.

		if ( request.ignoreResult )
			{
			response.setStatus( CVSResponse.OK );
			}
		
		return result;
		}


	private boolean
	processResponseItem(
			CVSRequest request, CVSResponse response, CVSResponseItem item )
		{
		int			idx;
		boolean		ok;
		CVSEntry	entry = null;
		boolean		result = true;
		File		localFile = null;

		//
		// HACK
		// NOTE
		//
		// This is a special hack to accomodate the one compromise we needed
		// to make to get all of the path handling to work. We wrote the one
		// directive that
		//
		//      ALL LOCAL DIRECTORY NAMES MUST BEGIN WITH "./"
		//
		// This make every case of the hideous paths returned by the server
		// work for us, since we are not like UNIX which works in a strickly
		// "relative" sense. We work from an "absolute" sense, for better or
		// worse...
		//
		// SPECIAL CASE
		//
		// There are times when the server will return a response item with
		// a repositry path ending with "./". This is usually our bad in the
		// protocol, but it is easy to catch and fix, so...
		//
		if ( item.getPathName().endsWith( "./" ) )
			{
			item.setPathName
				( item.getPathName().substring
					( 0, item.getPathName().length() - 2 ) );

			CVSTracer.traceIf( this.deepDebug,
				"\nPROCESSResponseItem: STRIPPED FINAL './' CASE\n"
				+ "   item.pathName = '" + item.getPathName() + "'" );
			}

		if ( ! item.getPathName().startsWith( "./" ) )
			{
			String itemRepos = item.getRepositoryName();
			int slashIdx = itemRepos.lastIndexOf( "/" );
			if ( slashIdx != -1 )
				{
				itemRepos = itemRepos.substring( 0, slashIdx );
				}

			CVSEntry hackEntry =
				this.reversePathTableEntry( itemRepos );

			CVSTracer.traceIf( this.deepDebug,
				"\nPROCESSResponseItem: APPLY ITEM PATHNAME HACK\n"
				+ "   item.pathName = '" + item.getPathName() + "'\n"
				+ "   item.repos    = '" + item.getRepositoryName() + "'\n"
				+ " lookup repos    = '" + itemRepos + "'\n"
				+ "   pathTable.entry:\n"
				+ (hackEntry==null?"   NULL":hackEntry.dumpString("   ")) );

			if ( hackEntry != null )
				{
				item.setPathName( hackEntry.getLocalDirectory() );
				CVSTracer.traceIf( this.deepDebug,
					"\nPROCESSResponseItem: ITEM PATH set to '"
					+ hackEntry.getLocalDirectory() + "'\n" );
				}
			else
				{
				//
				// NOTE
				// If we did not find the repository pathname, then this item
				// is something we have never seen before. This should ONLY
				// happen during things like checkout, where the tree does not
				// exist yet. In these cases. prepending "./" to the local
				// directory appears to be the correct answer.
				//
				item.setPathName( "./" + item.getPathName() );
				CVSTracer.traceIf( this.deepDebug,
					"\nPROCESSResponseItem: NO PATH TABLE ENTRY, PREFIX w/ './'\n"
					+ "   ITEM PATH set to '" + item.getPathName() + "'" );
				}
			}

		CVSTracer.traceIf( this.deepDebug,
			"PROCESSResponseItem:\n"
			+ "   item.getType        '" + item.getType() + "'\n"
			+ "   item.getPathName    '" + item.getPathName() + "'\n"
			+ "   item.repositoryName '" + item.getRepositoryName() + "'\n"
			+ "   item.getModeLine    '" + item.getModeLine() + "'\n"
			+ "   item.getEntriesLine '" + item.getEntriesLine() + "'" );

		switch ( item.getType() )
			{
			case CVSResponseItem.CHECKED_IN:
				// Checked-in implies the file is up-to-date
				CVSTracer.traceIf( request.traceProcessing,
					"CHECKED_IN: pathName '" + item.getPathName()
					+ "'\n   repository " + item.getRepositoryName()
					+ "'\n   entryLine " + item.getEntriesLine() );

				if ( request.handleEntries )
					{
					entry = this.createItemEntry( item );
					if ( entry != null )
						{
						CVSTracer.traceIf( request.traceProcessing,
							"CHECKED_IN: entry '"
							+ entry.getFullName() + "'" );
						
						localFile = this.getEntryFile( entry );

						entry.setTimestamp( localFile );

						this.updateEntriesItem( entry );
						}
					}
				break;

			case CVSResponseItem.NOTIFIED:
				CVSTracer.traceIf( request.traceProcessing,
					"NOTIFIED: pathName '" + item.getPathName()
					+ "'\n          repository '"
					+ item.getRepositoryName() + "'" );

				this.processNotified( item );
				break;

			case CVSResponseItem.CHECKSUM:
					// UNDONE
				break;

			case CVSResponseItem.COPY_FILE:
				CVSTracer.traceIf( request.traceProcessing,
					"COPY-FILE: pathName '" + item.getPathName()
					+ "'\n           newName '"
					+ item.getNewName() + "'" );

				//
				// UNDONE - it would be nice if we had a better
				//          error report, but we do not have the
				//          response object available deep in the
				//          method call, and we do not throw an
				//          exception, which may have been a better
				//          choice than returning false.... (duh)
				//
				if ( ! this.performCopyFile( item ) )
					{
					response.appendStderr
						( "ERROR copying file '" + item.getPathName()
							+ "' to '" + item.getNewName() + "'." );
					}
				break;

			case CVSResponseItem.CLEAR_STICKY:
				CVSTracer.traceIf( request.traceProcessing,
					"Clear-sticky: pathName '"
					+ item.getPathName() + "'\n" );
				this.setSticky( item, false, request.handleEntries );
				break;

			case CVSResponseItem.SET_STICKY:
				CVSTracer.traceIf( request.traceProcessing,
					"Set-sticky: pathName '"
					+ item.getPathName() + "'\n" );
				this.setSticky( item, true, request.handleEntries );
				break;

			case CVSResponseItem.CLEAR_STATIC_DIR:
				CVSTracer.traceIf( request.traceProcessing,
					"Clear-static-directory: pathName '"
					+ item.getPathName() + "'\n" );
				this.setStaticDirectory( item, false, request.handleEntries );
				break;

			case CVSResponseItem.SET_STATIC_DIR:
				CVSTracer.traceIf( request.traceProcessing,
					"Set-static-directory: pathName '"
					+ item.getPathName() + "'\n" );
				this.setStaticDirectory( item, true, request.handleEntries );
				break;

			case CVSResponseItem.MODULE_EXPANSION:
					// UNDONE
				break;

			case CVSResponseItem.NEW_ENTRY:
				// New-entry implies the file is still NOT up-to-date
				CVSTracer.traceIf( request.traceProcessing,
					"NEW_ENTRY: name '" + item.getPathName()
					+ "' entryLine '" + item.getEntriesLine() + "'" );

				if ( request.handleEntries )
					{
					entry = this.createItemEntry( item );
					if ( entry != null )
						{
						this.updateEntriesItem( entry );
						}
					}
				break;

			case CVSResponseItem.REMOVED:
				CVSTracer.traceIf( request.traceProcessing,
					"REMOVED: " + item.getPathName() );

				if ( request.handleEntries )
					{
					this.removeEntriesItem( item );
					}
				break;

			case CVSResponseItem.REMOVE_ENTRY:
				CVSTracer.traceIf( request.traceProcessing,
					"REMOVE_ENTRY: " + item.getPathName() );

				if ( request.handleEntries )
					{
					this.removeEntriesItem( item );
					}
				break;

			case CVSResponseItem.VALID_REQUESTS:
					// clients don't implement this.
				break;

			case CVSResponseItem.SET_CHECKIN_PROG:
				if ( request.handleFlags )
					{
					request.setCheckInProgram
							( item.getPathName() );
					}
				break;

			case CVSResponseItem.SET_UPDATE_PROG:
				if ( request.handleFlags )
					{
					request.setUpdateProgram
							( item.getPathName() );
					}
				break;

			case CVSResponseItem.PATCHED:
				CVSTracer.traceIf( true,
					"CVSProject.CVSResponseItem.PATCHED '"
					+ item.getEntriesLine() + "' "
					+ "PATCHED currently unimplemented.\n"
					+ "WE SHOULD NOT BE GETTING THIS!!!" );

				response.appendStderr
					( "The 'Patched' response is not implemented:\n" +
						"    '" + item.getEntriesLine() + "'" );
				break;

			case CVSResponseItem.CREATED:
			case CVSResponseItem.MERGED:
			case CVSResponseItem.UPDATED:
			case CVSResponseItem.UPDATE_EXISTING:
				if ( request.handleUpdated )
					{
					String cmdName =
						( item.getType() == CVSResponseItem.CREATED
						? "Created"
						: ( item.getType() == CVSResponseItem.MERGED
						? "Merged"
						: ( item.getType() == CVSResponseItem.UPDATED
						? "Updated" : "Updated existing" ) ) );

					entry = this.createItemEntry( item );
					if ( entry != null )
						{
						// We have to save this state, since the set
						// of the timestamp from the local file will
						// clear it in the entry.
						boolean isInConflict = entry.isInConflict();

						ok = this.ensureEntryHierarchy
								( item.getPathName(),
									item.getRepositoryPath() );

						localFile = this.getEntryFile( entry );

						if ( ok )
							{
							ok = this.ensureLocalTree
								( localFile, request.handleEntries );
							}

						if ( localFile.exists() )
							{
							entry.setTimestamp( localFile );
							}

						if ( ok )
							{
							request.getUserInterface().uiDisplayProgressMsg
								( cmdName + " local file '"
									+ localFile.getPath() + "'." );

							// UNDONE try/catch for better messaging!!!
							ok = this.updateLocalFile
									( item, entry, localFile );
							}

						if ( ok )
							{
							if ( isInConflict )
								{
								entry.setConflict( localFile );
								}
							else if ( item.getType() == CVSResponseItem.MERGED )
								{
								entry.setTimestamp( "Result of merge" );
								}
							else
								{
								entry.setTimestamp( localFile );
								}

							if ( request.handleEntries )
								{
								this.updateEntriesItem( entry );
								}
							}
						else
							{
							CVSLog.logMsg
								( "CVSResponse: ERROR merging local file '"
									+ entry.getFullName() + "'" );

							response.appendStderr
								( "ERROR failed updating local file '"
									+ localFile.getPath() + "'." );

							result = false;
							}
						}
					else
						{
						CVSLog.logMsg
							( "CVSResponse: ERROR creating item entry '"
								+ item.toString() + "'" );
						result = false;
						}
					}
				break;

			} // end of switch ( item type )

		return result;
		}

	public boolean
	performCopyFile( CVSResponseItem item )
		{
		boolean result = true;

		CVSEntry entry = this.createItemEntry( item );
		
		if ( entry != null )
			{
			File fromFile = this.getEntryFile( entry );

			entry.setName( item.getNewName() );

			File toFile = this.getEntryFile( entry );

			if ( fromFile.exists() )
				{
				// REVIEW - with Jim Kingdon
				// wouldn't it simply be more efficient to rename?
				// boolean err = fromFile.renameTo( toFile );
				result = this.copyFileRaw
					( fromFile, toFile, item.isGZIPed() );

				if ( ! result )
					{
					CVSLog.logMsg
						( "CVSProject.performCopyFile: ERROR renaming '"
							+ fromFile.getPath() + "' to '"
							+ toFile.getPath() + "'" );
					}
				}
			else
				{
				CVSLog.logMsg
					( "CVSProject.performCopyFile: file '"
						+ fromFile.getPath() + "' does not exist!" );
				}
			}
		else
			{
			CVSTracer.traceWithStack(
				"WHY is this entry NULL?! item '"
					+ item.toString() + "'" );
			}

		return result;
		}

	public boolean
	setSticky( CVSResponseItem item, boolean isSet, boolean writeFile )
		{
		boolean		result;

		String localDir =
			this.normalizeLocalDirectory
				( item.getPathName(), item.getRepositoryPath() );

		result =
			this.ensureEntryHierarchy
				( localDir, item.getRepositoryPath() );

		if ( result )
			{
			result =
				this.ensureProperWorkingDirectory
					( this.localRootDirectory, localDir, writeFile );
			}

		if ( result && writeFile )
			{
			CVSEntry entry = this.createItemEntry( item );
			if ( entry != null )
				{
				entry.setName( "CVS/Tag" );
				File file = this.getEntryFile( entry );

				if ( isSet )
					{
					if ( ! file.exists() )
						{
						try {
							CVSCUtilities.writeStringFile
								( file, item.getTagSpec() );
							}
						catch ( IOException ex )
							{
							CVSTracer.traceWithStack
								( "ERROR writing sticky tag file '"
									+ file.getPath() + "', " + ex.getMessage() );
							}
						}
					}
				else
					{
					if ( file.exists() )
						{
						file.delete();
						}
					}
				}
			}
		else if ( ! result )
			{
			CVSTracer.traceWithStack(
				"ensureEntryHierarchy( '" + item.getPathName()
				+ "', '" + item.getRepositoryPath() + "' ) FAILED" );
			}

		return result;
		}

	public boolean
	setStaticDirectory( CVSResponseItem item, boolean isSet, boolean writeFile )
		{
		boolean		result;

		result =
			this.ensureEntryHierarchy
				( item.getPathName(), item.getRepositoryPath() );

		if ( result )
			{
			result =
				this.ensureProperWorkingDirectory
					( this.localRootDirectory,
						this.normalizeLocalDirectory
							( item.getPathName(), item.getRepositoryPath() ),
						writeFile );
			}

		if ( result && writeFile )
			{
			CVSEntry entry = this.createItemEntry( item );
			if ( entry != null )
				{
				entry.setName( "CVS/Entries.static" );
				File file = this.getEntryFile( entry );

				if ( isSet )
					{
					if ( ! file.exists() )
						{
						CVSCUtilities.createEmptyFile( file );
						}
					}
				else
					{
					if ( file.exists() )
						{
						file.delete();
						}
					}
				}
			}
		else if ( ! result )
			{
			CVSTracer.traceWithStack(
				"ensureEntryHierarchy( '" + item.getPathName()
				+ "', '" + item.getRepositoryPath() + "' ) FAILED" );
			}

		return result;
		}

	public CVSNotifyItem 
	parseNotifyLine( String notifyLine )
		{
		CVSNotifyItem	result = null;

		String notType = notifyLine.substring( 0, 1 );
		notifyLine = notifyLine.substring( 1 );

		StringTokenizer toker =
			new StringTokenizer( notifyLine, "\t" );

		int count = toker.countTokens();

		if ( count > 3 )
			{
			String name = null;
			String time = null;
			String host = null;
			String wdir = null;
			String watches = null;

			try {
				name = toker.nextToken();
				time = toker.nextToken();
				host = toker.nextToken();
				wdir = toker.nextToken();
				}
			catch ( NoSuchElementException ex )
				{
				name = null;
				}

			try { watches = toker.nextToken(); }
				catch ( NoSuchElementException ex )
					{ watches = null; }

			if ( name != null && time != null && host != null && wdir != null )
				{
				CVSEntry entry =
					(CVSEntry) this.pathTable.get( wdir );

				if ( entry != null )
					{
					result = new CVSNotifyItem
						( notType, name, time, host, wdir,
							( watches == null ? "" : watches ),
							entry.getRepository() );
					}
				}
			}

		return result;
		}

	protected boolean
	processNotified( CVSResponseItem item )
		{
		boolean			result = true;
		BufferedReader	read;
		PrintWriter		write;
		String			inline;

		CVSEntry entry = this.createItemEntry( item );

		String itemPath = entry.getFullName();

		String	fileName =
 			CVSProject.getAdminNotifyPath(
				CVSProject.rootPathToAdminPath
					( this.getLocalRootPath() ) );

		File notFile = new File( fileName );
		File tmpFile = new File( fileName + ".tmp" );

		try
			{
			read = new BufferedReader( new FileReader( notFile ) );
			write = new PrintWriter( new FileWriter( tmpFile ) );
			}
		catch ( IOException ex )
			{
			String msg =
				"ERROR opening Notification file '"
				+ fileName + "' for Notified response.";
			CVSLog.logMsg( msg );
			CVSTracer.traceWithStack( msg );
			return false;
			}

		int count = 0;
		for ( boolean chk = true ; ; )
			{
			try { inline = read.readLine(); }
			catch ( IOException ex )
				{
				String msg =
					"ERROR reading Notification file "
						+ "during Notified response.";
				CVSLog.logMsg( msg );
				CVSTracer.traceWithStack( msg );
				inline = null;
				}

			if ( inline == null )
				break;

			if ( ! chk )
				{
				write.println( inline );
				count++;
				}
			else
				{
				CVSNotifyItem notifyItem = 
					this.parseNotifyLine( inline );

				if ( notifyItem != null )
					{
					String fullName =
						notifyItem.getWorkingDirectory() + notifyItem.getName();

					if ( ! itemPath.equals( fullName ) )
						{
						write.println( inline );
						chk = false;
						count++;
						}
					}
				else
					{
					CVSLog.logMsg
						( "ERROR, bad line in 'CVS/Notify':\n"
							+ "   File: '" + fileName + "'\n"
							+ "   Line: " + inline );
					}
				}
			}

		try { read.close(); }
			catch ( IOException ex ) { }
		
		write.flush();
		write.close();

		if ( result )
			{
			result = notFile.delete();
			if ( result )
				{
				if ( count > 0 )
					result = tmpFile.renameTo( notFile );
				else
					tmpFile.delete();
				}
			}

		return result;
		}

	public String
	readRootDirectory( File rootFile )
		{
		String result = null;
		try {
			result =
				CVSCUtilities.readStringFile( rootFile );
			}
		catch ( IOException ex )
			{
			result = null;
			}

		return result;
		}

	public String
	readRepository( File reposFile )
		{
		String result = null;
		try {
			result =
				CVSCUtilities.readStringFile( reposFile );
			}
		catch ( IOException ex )
			{
			result = null;
			}

		return result;
		}

	/**
	 *
	 * @param repository The server's repository pathname for the root.
	 */
	public void
	establishRootEntry( String repository )
		{
		if ( CVSProject.deepDebug )
		CVSTracer.traceIf( true,
			"CVSProject.establishRootEntry: " 
			+ "repository  '" + repository + "'" );

		CVSEntry rootEntry = new CVSEntry();
		
		rootEntry.setDirty( true );
		rootEntry.setName( "." );
		rootEntry.setRepository( repository );
		rootEntry.setLocalDirectory( "./" );

		// We need to set the Entry List to mark this as a directory.
		rootEntry.setDirectoryEntryList( new CVSEntryVector() );

		this.pathTable.put( rootEntry.getLocalDirectory(), rootEntry );

		if ( CVSProject.deepDebug )
		CVSTracer.traceIf( true,
			"CVSProject.establishRootEntry: ROOT ESTABLISHED:\n" 
			+ rootEntry.dumpString( "   " ) );

		this.rootEntry = rootEntry;
		}

	public void
	openProject( File localRootFile )
		throws IOException
		{
		String	repositoryStr;
		String	rootDirectoryStr;

		if ( this.deepDebug )
		CVSTracer.traceIf( true,
			"CVSProject.openProject: OPEN PROJECT '"
			+ localRootFile.getPath() + "'" );

		File adminDirFile =
			new File( localRootFile.getPath(), "CVS" );

		if ( this.deepDebug )
		CVSTracer.traceIf( true,
			"CVSProject.openProject: adminDirFile '"
			+ adminDirFile.getPath() + "'" );

		if ( ! adminDirFile.exists() )
			throw new IOException
				( "admin directory '"
					+ adminDirFile.getPath() + "' does not exist" );

		String rootPath =
			CVSProject.getAdminRootPath
				( CVSCUtilities.importPath( adminDirFile.getPath() ) );

		File adminRootFile =
			new File( CVSCUtilities.exportPath( rootPath ) );

		if ( this.deepDebug )
		CVSTracer.traceIf( true,
			"CVSProject.openProject: adminRootFile '"
			+ adminRootFile.getPath() + "'" );

		if ( ! adminRootFile.exists() )
			throw new IOException
				( "admin Root file '" + adminRootFile.getPath()
					+ "' does not exist" );

		String reposPath =
			CVSProject.getAdminRepositoryPath( adminDirFile.getPath() );

		File adminRepositoryFile = new File( reposPath );

		if ( this.deepDebug )
		CVSTracer.traceIf( true,
			"CVSProject.openProject: adminRepositoryFile '"
			+ adminRepositoryFile.getPath() + "'" );

		if ( ! adminRepositoryFile.exists() )
			throw new IOException
				( "admin Repository file '"
					+ adminRepositoryFile.getPath()
					+ "' does not exist" );

		rootDirectoryStr = this.readRootDirectory( adminRootFile );
		if ( rootDirectoryStr == null )
			throw new IOException
				( "could not read admin Root file '"
					+ adminRootFile.getPath() + "'" );

		repositoryStr = this.readRepository( adminRepositoryFile );
		if ( repositoryStr == null )
			throw new IOException
				( "could not read admin Repository file '"
					+ adminRepositoryFile.getPath()
					+ "'" );

		if ( this.deepDebug )
		CVSTracer.traceIf( true,
			"CVSProject.openProject: Read Admin directory\n"
			+ "   rootPath   '" + rootPath + "'\n"
			+ "   reposPath  '" + reposPath + "'\n"
			+ "   rootDirStr '" + rootDirectoryStr + "'\n"
			+ "   reposStr   '" + repositoryStr + "'" );

		CVSProjectDef def =
			new CVSProjectDef( rootDirectoryStr, repositoryStr );

		if ( ! def.isValid() )
			throw new IOException
				( "could not parse project specification, "
					+ def.getReason() );

		this.isPServer = def.isPServer();
		this.connMethod = def.getConnectMethod();
		this.userName = def.getUserName();
		this.getClient().setHostName( def.getHostName() );

		rootDirectoryStr = def.getRootDirectory();

		//
		// REVIEW
		// Should I not remove the check for the '/' or 'C:/'
		// at the beginning and just make the "starts with root
		// directory" check in stead, and then if that does not
		// match, see if the string starts with 'C:/' and if not
		// assume relative at that point? Would that not make
		// the code less dependent on what are valid "starts"?
		//
		// The previous check, that is started with '/', does
		// not work for the case of a Win32 server, since it
		// will have a root such as 'C:/cvs'. Thus, we now
		// check for "starts with /", as well as "starts with
		// drive letter colon slash".
		//
		// Thanks to Manfred Usselmann <Usselmann.M@icg-online.de>
		// for these patches.
		//
		// OLD CODE:
		// if ( repositoryStr.startsWith( "/" ) )
		//
		// Contributed CODE:
		// char ch0 = repositoryStr.charAt(0);
		// char ch1 = repositoryStr.charAt(1);
		// char ch2 = repositoryStr.charAt(2);
		// if ( ch0 == '/'
		// 		// IF there a colon for the drive letter
		// 		|| ( ch1 == ':'
		// 			// AND there is a valid drive letter
		// 			&& ( ( ch0 >= 'a' && ch0 <= 'z' )
		// 				|| ( ch0 >= 'A' && ch0 <= 'Z' ) )
		// 			// AND there is a slash or backslash
		// 			&& ( ch2 == '/' || ch2 == '\\' ) ) )
		// 	{
		// 	if ( ! repositoryStr.startsWith( rootDirectoryStr ) )
		// 		{
		// 		throw new IOException
		// 			( "full repository path '" + repositoryStr
		// 				+ "' does not start with Root path '"
		// 				+ rootDirectoryStr + "'" );
		// 		}
		// 	}
		// else
		// 	{
		// 	// The relative pathname case, prepend with the root.
		// 	repositoryStr = rootDirectoryStr + "/" + repositoryStr;
		// 	}
		//

		if ( repositoryStr.startsWith( rootDirectoryStr ) )
			{
			// The full pathname case. No adjustment needed.
			}
		else
			{
			// The relative pathname case, prepend with the root.
			repositoryStr = rootDirectoryStr + "/" + repositoryStr;
			}

		//
		// REVIEW
		// UNDONE - need 'computeParentDirectory()' here.
		//          File.getParent() does not seem to work.
		//          I suspect because it uses the local separator?
		//
		// Should really just get the module name from the local dir name.?
		//
		String localRootStr =
			CVSCUtilities.importPath( localRootFile.getPath() );

		int index = localRootStr.lastIndexOf( '/' );

		// This will include the beginning slash is there is any string at all...
		String repos =
			repositoryStr.substring
				( rootDirectoryStr.length() );
	
		if ( this.deepDebug )
		CVSTracer.traceIf( true,
			"CVSProject.openProject: LOCAL ROOT CHECK\n"
			+ "   localRootStr  '" + localRootStr + "'\n"
			+ "   repos         '" + repos + "'" );
/*
** REL 5.0.7
**
		if ( repos.length() > 0 && localRootStr.endsWith( repos ) )
			{
			localRootStr =
				localRootStr.substring
					( 0, ( localRootStr.length() - repos.length() ) );
			}
**
*/
		if ( this.deepDebug )
		CVSTracer.traceIf( true,
			"CVSProject.openProject: Establish ROOT\n"
			+ "   localRootStr  '" + localRootStr + "'\n"
			+ "   repositoryStr '" + repositoryStr + "'\n"
			+ "   rootDirStr    '" + rootDirectoryStr + "'\n"
			+ "   repos         '" + repos + "'" );

		if ( repos.startsWith( "/" ) )
			repos = repos.substring(1);
		if ( repos.length() < 1 )
			repos = ".";

		this.setRepository( repos ); // This is just a "name" now...

		this.setLocalRootDirectory( localRootStr );

		this.setRootDirectory( rootDirectoryStr );

		this.establishRootEntry( rootDirectoryStr );
/*
** REL5.0.6
**
		if ( this.deepDebug )
		CVSTracer.traceIf( true,
			"CVSProject.openProject: Establish Child?\n"
			+ "   repos         '" + repos + "'\n"
			+ "   repositoryStr '" + repositoryStr + "'" );

		if ( repos.length() > 0 )
			{
			CVSEntry childEntry = new CVSEntry();
			
			int slashIdx = repos.lastIndexOf( "/" );

			if ( slashIdx == -1 )
				childEntry.setName( repos );
			else
				childEntry.setName( repos.substring( slashIdx + 1 ) );

			childEntry.setRepository( repositoryStr );
			childEntry.setLocalDirectory( "." + repos + "/" );
			childEntry.setDirectoryEntryList( new CVSEntryVector() );

			CVSEntryVector eV = new CVSEntryVector();
			eV.appendEntry( childEntry );

			if ( this.deepDebug )
			CVSTracer.traceIf( true,
				"CVSProject.openProject: ROOT CHILD: \n"
				+ childEntry.dumpString() );

			this.rootEntry.setDirectoryEntryList( eV );
			}
**
*/
		if ( CVSProject.deepDebug )
		CVSTracer.traceIf( true,
			"CVSProject.openProject:\n"
			+ "   Root Directory:  " + this.rootDirectory + "\n"
			+ "   Repository:      " + this.repository + "\n"
			+ "   rootRepos:       " + repositoryStr + "\n"
			+ "   Local Root:      " + this.localRootDirectory + "\n" );

		if ( CVSProject.deepDebug )
		CVSTracer.traceIf( true,
			"CVSProject.openProject: ROOT ENTRY\n"
			+ this.rootEntry.dumpString() );

		if ( ! readEntries() )
			{
			throw new IOException
				( "ERROR reading 'Entries' file " );
			}

		if ( CVSProject.deepDebug )
			{
			StringBuffer buf = new StringBuffer();

			this.dumpCVSProject( buf, "Project Open" );

			CVSLog.logMsg( buf.toString() );
			}
		}

	public void
	removeAllEntries()
		{
		this.rootEntry.removeAllEntries();
		}

	public void
	addNewEntry( CVSEntry entry )
		{
		if ( this.rootEntry == null )
			{
			CVSTracer.traceWithStack
				( "CVSProject.addNewEntry: NULL ROOT ENTRY!!!!" );
			}

		String name = entry.getName();
		String localDirectory = entry.getLocalDirectory();
		String repository = entry.getRepository();

		this.ensureEntryHierarchy( localDirectory, repository );

		CVSEntry parentEntry =
			(CVSEntry) this.getPathTableEntry( localDirectory );
		
		if ( parentEntry == null )
			{
			CVSTracer.traceWithStack
				( "ENTRY '" + entry.getFullName() + "' NO PARENT!" );
			return;
			}

		parentEntry.appendEntry( entry );		 
		}

	public String
	reposNameToRepository( String fullRepos )
		{
		int index = fullRepos.lastIndexOf( '/' );

		if ( index < 0 )
			{
			CVSTracer.traceWithStack(
				"CVSProject.reposNameToRepository: ERROR "
				+ "repository '" + fullRepos + "' has no slash!" );
			return fullRepos;
			}
		else
			{
			return fullRepos.substring( 0, index );
			}
		}

	public String
	reposNameToFileName( String fullRepos )
		{
		int index = fullRepos.lastIndexOf( '/' );

		if ( index < 0 )
			{
			CVSTracer.traceWithStack(
				"CVSProject.reposNameToFileName: ERROR "
				+ "repository '" + fullRepos + "' has no slash!" );
			return fullRepos;
			}
		else
			{
			return fullRepos.substring( index + 1 );
			}
		}

	public boolean
	removeEntriesItem( CVSResponseItem item )
		{
		CVSEntryVector	entries;
		boolean			result = true;

		if ( CVSProject.deepDebug )
		CVSTracer.traceIf( true,
			"CVSProject.removeEntriesItem: pathName '"
			+ item.getPathName() + "'" );

		String localDirectory =
			this.normalizeLocalDirectory
				( item.getPathName(),
					this.reposNameToRepository
						( item.getRepositoryName() ) );

		CVSEntry parentEntry =
			(CVSEntry) this.getPathTableEntry( localDirectory );

		if ( parentEntry == null )
			{
			result = false;
			CVSTracer.traceWithStack
				( "CVSProject.removeEntriesItem: NO PARENT! pathName '"
					+ item.getPathName() + "' (localDir '"
					+ localDirectory + "')." );
			}
		else
			{
			String entryName =
				this.reposNameToFileName
					( item.getRepositoryName() );

			result = parentEntry.removeEntry( entryName );
			}

		return result;
		}

	/**
	 * Given an entry, update the entry in our project. If the
	 * entry is new (not found), then add it to our project.
	 *
	 * @param newEntry The entry to update.
	 */

	public void
	updateEntriesItem( CVSEntry newEntry )
		{
		CVSEntry	entry;
		boolean		result;
		
		if ( CVSProject.deepDebug )
		CVSTracer.traceIf( true,
			"CVSProject.UPDATEEntriesItem: newEntry\n"
			+ "   getFullName       '" + newEntry.getFullName() + "'\n"
			+ "   getLocalDirectory '" + newEntry.getLocalDirectory() + "'\n"
			+ "   getAdminEntryLine '" + newEntry.getAdminEntryLine() + "'" );

		// REVIEW
		// UNDONE
		//
		// When we get these from the server, typically they
		// have only the first two fields filled in.
		// 
		// Therefore, I think it is correct to simply bother
		// with the possible effects that these two first
		// fields can have on our local Entries.
		//
		// Note: When the add command is used, it sends back
		//       a 'Checked-in' with an Entry '/name/0///'.
		//       CVSEntries are smart enough to recognize
		//       new user files and set conflict to 'Initial...'
		//       when needed.
		//
		//       Also, when the 'remove' command is performed,
		//       it sends back a 'Checked-in' with an Entry
		//       '/name/-version///'. We have to be smart enough
		//       here to pickup the "marked for removal" aspect
		//       only if our version match. God only knows why
		//       we would ever get one that did not match our Entry... 

		String name = newEntry.getName();
		String localDirectory = newEntry.getLocalDirectory();

		if ( CVSProject.deepDebug )
		CVSTracer.traceIf( true,
			"CVSProject.updateEntriesItem: localDirectory '"
			+ localDirectory + "' name '" + name + "'  ENTRY '"
			+ newEntry + "'" );

		CVSEntry parentEntry =
			(CVSEntry) this.getPathTableEntry( localDirectory );
		
		entry = null;
		if ( parentEntry != null )
			{
			entry = parentEntry.locateEntry( name );
			}
		
		if ( CVSProject.deepDebug )
		CVSTracer.traceIf( true,
			"CVSProject.updateEntriesItem: Parent '"
			+ ( parentEntry == null
				? "(null)" : parentEntry.getFullName() )
			+ "'" );

		if ( entry != null )
			{
			// New user files are special here, since typically
			// their version string is empty (null). They can not
			// really have a conflict

			if ( CVSProject.deepDebug )
			CVSTracer.traceIf( true,
				"CVSProject.updateEntriesItem: newUserfile? '"
					+(newEntry.isNewUserFile()?"yes":"no")+	"'" );

			if ( ! newEntry.isNewUserFile()
					&& ! entry.getVersion().equals( newEntry.getVersion() ) )
				{
				if ( CVSProject.deepDebug )
				CVSTracer.traceIf( true,
					"CVSProject.updateEntriesItem: " +
						"WARNING: version mismatch: Entry '" +
						newEntry.getFullName() + "' New '" +
						newEntry.getVersion() + "' Existing: '" +
						entry.getVersion() + "'" );
				}

			// If the new entry's conflict field is not null,
			// then set the existing entry's field to the same
			// value. Since we are checking the inConflict flag
			// below, we can just deal with the string part of
			// the conflict field. The same goes for the version.

			if ( newEntry.getVersion() != null )
				entry.setVersion( newEntry.getVersion() );

			if ( newEntry.isNewUserFile() )
				entry.setNewUserFile( true );

			if ( newEntry.isToBeRemoved() )
				entry.setToBeRemoved( true );

			// completeTimestamp() returns the 'timestamp+conflict' format,
			// which setTimestamp() will properly parse for conflict info.
			if ( newEntry.completeTimestamp() != null )
				entry.setTimestamp( newEntry.completeTimestamp() );

			if ( newEntry.getOptions() != null
					&& newEntry.getOptions().length() > 0 )
				entry.setOptions( newEntry.getOptions() );
			
			if ( newEntry.getTag() != null )
				{
				entry.setTag( newEntry.getTag() );
				}
			else if ( newEntry.getDate() != null )
				{
				entry.setDate( newEntry.getDate() );
				}
			else
				{
				// This one call nulls both tag and date.
				entry.setTag( null );
				}
			
			entry.setDirty( true );

			if ( CVSProject.deepDebug )
			CVSTracer.traceIf( true,
				"CVSProject.updateEntriesItem: FINAL:\n"
					+ "   getFullName       '" + entry.getFullName() + "'\n"
					+ "   getAdminEntryLine '" + entry.getAdminEntryLine() + "'\n"
					+ "   getLocalDirectory '" + entry.getLocalDirectory() + "'" );
			}
		else
			{
			if ( parentEntry == null )
				{
				CVSTracer.traceIf( true,
					"CVSProject.updateEntriesItem: PARENT IS NULL!!!" );
				CVSTracer.traceWithStack( "NULL PARENT!" );
				}
			this.addNewEntry( newEntry );
			newEntry.setDirty( true );
			}
		}

	// REVIEW
	// This method should be throwing exceptions! not returning false.

	public boolean
	readEntries()
		{
		boolean ok = true;

		if ( CVSProject.debugEntryIO )
		CVSTracer.traceIf( true,
			"CVSProject.readEntries:\n"
			+ "   locaRootPath '" +  this.getLocalRootPath() + "'\n"
			+ "   ROOT ENTRY   '" +  this.rootEntry.dumpString() + "'" );

		String rootStr =
			CVSCUtilities.exportPath( this.getLocalRootPath() );

		File workingDirectory = new File( rootStr );

		if ( CVSProject.debugEntryIO )
		CVSTracer.traceIf( true,
			"CVSProject.readEntries:\n"
			+ "   WkgDirPath '" +  workingDirectory.getPath() + "'" );

		CVSEntryVector entries =
			this.readEntriesFile( this.rootEntry, workingDirectory );

		if ( entries != null )
			this.rootEntry.setDirectoryEntryList( entries );
		else
			return false;

		return true;
		}

	/**
	 * @param dirEntry The entry of the directory being loaded.
	 * @param workingDirectory The local file system directory of dirEntry.
	 */

	public CVSEntryVector
	readEntriesFile( CVSEntry dirEntry, File workingDirectory )
		{
		int			linenum = 0;
		String		line = null;
		boolean		ok = true;
		boolean		isDir = false;
		BufferedReader in = null;
		
		CVSEntryVector entries = new CVSEntryVector();

		// Compute the 'local directory' that this Entry will exchange
		// with the server during the protocol...

		String localDirectory =
			CVSCUtilities.importPath
				( workingDirectory.getPath().substring
					( this.localRootDirectory.length() ) );

		if ( localDirectory.startsWith( "/" ) )
			localDirectory = localDirectory.substring(1);

		localDirectory =
			CVSCUtilities.ensureFinalSlash( "./" + localDirectory );

		if ( CVSProject.debugEntryIO )
		CVSTracer.traceIf( true,
			"CVSProject.readEntriesFile: ENTER\n"
			+ "   wkgDir    '" + workingDirectory.getPath() + "'\n"
			+ "   localDir  '" + localDirectory + "'\n"
			+ "   dirEntry\n" + dirEntry.dumpString( "   " ) );


		// ===============  ROOT  ======================
		String adminRootPath =
			CVSProject.rootPathToAdminPath
				( CVSCUtilities.importPath( workingDirectory.getPath() ) );

		File adminRootFile = new File
			( CVSCUtilities.exportPath
				( CVSProject.getAdminRootPath( adminRootPath ) ) );

		if ( CVSProject.debugEntryIO )
		CVSTracer.traceIf( true,
			"CVSProject.readEntriesFile: adminRootFile '"
			+ adminRootFile.getPath() + "'\n" );

		String rootDirectoryStr =
			this.readRootDirectory( adminRootFile );

		if ( rootDirectoryStr == null )
			{
			CVSLog.logMsg(
				"ERROR admin 'Root' file '"
				+ adminRootFile.getPath() +"' is empty!" );
			return null;
			}

		//
		// We had to modify this code to accomodate Win32 servers.
		// They have root directories such as 'C:\projects\cvs'.
		// Thus, you end up with cvs specifications such as this:
		//       :pserver:user@host.domain:C:/src/cvs
		//
		// In that case, the following commented line of code would
		// incorrectly use the path *after* the drive letter + colon,
		// which of course if not correct and hoses the server and
		// all of our root prefix checks!
		//
		// To fix the code, we change the code to count three colons
		// to the right from the left, as opposed to the old code
		// which counted one left from the right.
		//
		// Thanks to Manfred Usselmann <Usselmann.M@icg-online.de>
		// for solving this and providing the patch.
		//
		// int index = rootDirectoryStr.lastIndexOf( ':' );
		//
		int index = -1;
		for ( int i = 0 ; i < 3; ++i )
			{
			index = rootDirectoryStr.indexOf( ':', index + 1 );
			if ( index == -1 )
				break;
			}

		if ( index >= 0 )
			{
			rootDirectoryStr =
				rootDirectoryStr.substring( index + 1 );
			}
		else
			{
			CVSLog.logMsg
				( "ERROR admin 'Root' file is MISSING COLONS!" );
			}


		// ============  REPOSITORY  ===================
		File adminRepositoryFile = new File
			( CVSCUtilities.exportPath
				( CVSProject.getAdminRepositoryPath( adminRootPath ) ) );

		if ( CVSProject.debugEntryIO )
		CVSTracer.traceIf( true,
			"CVSProject.readEntriesFile: adminRepositoryFile '"
			+ adminRepositoryFile.getPath() + "'\n" );

		String repositoryStr = this.readRepository( adminRepositoryFile );
		if ( repositoryStr == null )
			{
			CVSLog.logMsg(
				"ERROR admin 'Repository' file '"
				+ adminRepositoryFile.getPath() +"' is empty!" );
			return null;
			}

		// NOTE
		// It appears that when the CVS command line checks out a
		// working directory, it sometimes (some versions?) sets the
		// repository to a path *relative* to the root directory.
		// Since jCVS expects a full pathname, we normalize here.
		//
		// Thanks to Thorsten Ludewig <Thorsten.Ludewig@FH-Wolfenbuettel.DE>
		// for sending the patch for this.
		//
		if ( ! repositoryStr.startsWith( rootDirectoryStr ) )
			{
			repositoryStr = rootDirectoryStr + "/" + repositoryStr;
			}

		// ============  TABLE ENTRY  ===================
		dirEntry.setRepository( repositoryStr );
		this.pathTable.put( localDirectory, dirEntry );

		if ( CVSProject.debugEntryIO )
		CVSTracer.traceIf( true,
			"READENTRIES: ADDED PATH TABLE ENTRY\n"
			+ "   dirEntry:       " + dirEntry.getFullName() + "\n"
			+ "   localDirectory: " + localDirectory + "\n"
			+ "   repository:     " + repositoryStr );


		// ==============  ENTRIES  ===================

		// First, make sure we pick up and process Entries.Log
		//
		try {
			CVSCUtilities.integrateEntriesLog
				( new File( CVSCUtilities.exportPath( adminRootPath ) ) );
			}
		catch ( IOException ex )
			{
			CVSLog.logMsg
				( "ERROR integrating 'Entries.Log' file in Admin Path '"
					+ adminRootPath + "', " + ex.getMessage() );
			ex.printStackTrace();
			}

		File entriesFile = new File
			( CVSCUtilities.exportPath
				( CVSProject.getAdminEntriesPath( adminRootPath ) ) );

		if ( CVSProject.debugEntryIO )
		CVSTracer.traceIf( true,
			"CVSProject.readEntriesFile: entriesFile '"
			+ entriesFile.getPath() + "'\n" );

		try {
			in = new BufferedReader
						( new FileReader( entriesFile ) );
			}
		catch ( IOException ex )
			{
			in = null;
			ok = false;
			}

		for ( linenum = 1 ; ok ; ++linenum )
			{
			try { line = in.readLine();	}
			catch ( IOException ex )
				{
				line = null;
				break;
				}

			if ( line == null ) break;
			
			// Lines that starts with "D" are directories!
			if ( line.startsWith( "D" ) )
				{
				isDir = true;
				line = line.substring( 1 );
				}

			if ( line.startsWith( "/" ) )
				{
				CVSEntry entry = new CVSEntry();

				try {
					entry.parseEntryLine( line, false );
					}
				catch ( ParseException ex )
					{
					// UNDONE
					CVSLog.logMsg
						( "Bad admin 'Entries' line " +linenum+ ", '" +line+
							"' isDir '" +isDir+ "' - " + ex.getMessage() );
					ok = false;
					}

				if ( ok )
					{
					if ( CVSProject.debugEntryIO )
					CVSTracer.traceIf( true,
						"CVSProject.readEntriesFile: PARSED ENTRY\n"
						+ "   entry:          " + entry.getName() + "\n"
						+ "   repository:     " + repositoryStr + "\n"
						+ "   localDirectory: " + localDirectory );

					entry.setRepository( repositoryStr );
					entry.setLocalDirectory( localDirectory );

					entries.appendEntry( entry );

					if ( isDir )
						{
						String newLocal =
							localDirectory + entry.getName() + "/";

						String newRepos =
							repositoryStr + "/" + entry.getName();

						entry.setRepository( newRepos );
						entry.setLocalDirectory( newLocal );

						String newWkgPath =
							workingDirectory.getPath()
								+ File.separator + entry.getName()
								+ File.separator;

						String adminPath = newLocal + "CVS";

						File newWorking =
							new File( workingDirectory, entry.getName() );
							//	this.localRootDirectory + "/"
							//	+ localDirectory + entry.getName()
							//	);
						
						if ( CVSProject.debugEntryIO )
						CVSTracer.traceIf ( true,
							"readEntriesFile: IS DIRECTORY:\n"
								+ "   entriesFile   '" + entriesFile.getPath() + "'\n"
								+ "   NewWorkingDir '" + newWorking.getPath() + "'\n"
								+ "   newRepos      '" + newRepos + "'\n"
								+ "   newLocal      '" + newLocal + "'" );

						CVSEntryVector newEntries =
							readEntriesFile( entry, newWorking );
						
						if ( newEntries == null )
							{
							CVSLog.logMsg
								( "ERROR failed reading Entries file from '"
									+ newWorking.getPath() + "'" );
							
							newEntries = new CVSEntryVector();
							}
							
						entry.setDirectoryEntryList( newEntries );
						}
					}

				isDir = false; // reset the directory flag.
				}
			}

		if ( in != null )
			{
			try { in.close(); }
				catch ( IOException ex ) {}
			}

		return entries;
		}

	public boolean
	writeAdminFiles()
		{
		boolean		result = false;

		String localPath = this.getLocalRootDirectory();

		if ( CVSProject.debugEntryIO )
		CVSTracer.traceIf( true,
			"CVSProject.writeAdminFiles: WRITE ADMIN FILES\n"
				+ "   localPath   '" + localPath + "'\n"
				+ "   rootEntry    " + this.rootEntry.dumpString() );

		result =
			this.writeAdminAndDescend
				( localPath, this.rootEntry );

		if ( ! result )
			{
			// UNDONE - can we report better here?
			CVSLog.logMsg
				( "CVSProject.writeAdminFiles:\n"
					+ "  ERROR Writing the CVS administrative files FAILED!" );
			}

		return result;
		}

	private boolean
	writeAdminAndDescend( String localRoot, CVSEntry dirEntry )
		{
		int			i;
		boolean		result = true;

		String localDir = dirEntry.getLocalDirectory();

		String localPath = localRoot;
		if ( localDir.length() > 2 )
			{
			localPath = localPath + "/" + localDir.substring(2);
			}

		if ( CVSProject.debugEntryIO )
		CVSTracer.traceIf( true,
			"CVSProject.writeAdminFiles: WRITE AND DESCEND LOCAL PATH\n"
				+ "   localPath   '" + localPath + "'" );

		String adminRootPath =
			CVSProject.rootPathToAdminPath( localPath );

		File adminFile = new File
			( CVSCUtilities.exportPath( adminRootPath ) );

		File rootFile = new File
			( CVSCUtilities.exportPath
				( CVSProject.getAdminRootPath( adminRootPath ) ) );

		File reposFile = new File
			( CVSCUtilities.exportPath
				( CVSProject.getAdminRepositoryPath( adminRootPath ) ) );

		File entriesFile = new File
			( CVSCUtilities.exportPath
				( CVSProject.getAdminEntriesPath( adminRootPath ) ) );

		CVSEntryVector entries = dirEntry.getEntryList();

		if ( CVSProject.debugEntryIO )
		{
		CVSTracer.traceIf( true,
			"===================================="
			+ "====================================" );
		CVSTracer.traceIf( true,
			"CVSProject.writeAdminAndDescend:\n"
				+ "   dirEntry      '" + dirEntry.getFullName() + "'\n"
				+ "   isDirty       '" + dirEntry.isDirty() + "'\n"
				+ "   dirRepos      '" + dirEntry.getRepository() + "'\n"
				+ "   localRoot     '" + localPath + "'\n"
				+ "   localDir      '" + localDir + "'\n"
				+ "   adminFile     '" + adminFile.getPath() + "'\n"
				+ "   rootFile      '" + rootFile.getPath() + "'\n"
				+ "   reposFile     '" + reposFile.getPath() + "'\n"
				+ "   entriesFile   '" + entriesFile.getPath() + "'\n"
				+ "   entries.size  '" + entries.size() + "'\n"
				+ "   entries.dirty '" + entries.isDirty() + "'" );
		}

		if ( ! dirEntry.isDirty() && ! entries.isDirty() )
			{
			if ( CVSProject.debugEntryIO )
			CVSTracer.traceIf( true,
				"\nCVSProject.writeAdminAndDescend: "
					+ "NO DIRTY ENTRIES --> SKIP WRITE\n" );
			}
		else
			{
			if ( ! adminFile.exists() )
				{
				if ( ! adminFile.mkdir() )
					{
					CVSTracer.traceWithStack(
						"ERROR could not create the admin directory '"
						+ adminFile.getPath() + "'" );
					}
				}

			// ==============    ENTRIES   ==================
			result = this.writeAdminEntriesFile( entriesFile, entries );

			if ( result )
				{
				// ==============    ROOT   ==================
				String connMethod;
				if ( this.getConnectionMethod()
						== CVSRequest.METHOD_RSH )
					{
					connMethod = "server";
					}
				else if ( this.isPServer() )
					{
					connMethod = "pserver";
					}
				else
					{
					connMethod = "direct";
					}

				String rootDirStr =
					":" + connMethod + ":"
					+ ( (this.userName.length() > 0) ? ( this.userName + "@") : "" )
					+ this.getClient().getHostName()
					+ ":"
					+ this.rootDirectory;

				if ( CVSProject.debugEntryIO )
				CVSTracer.traceIf( true,
					"CVSProject.writeAdminAndDescend: WRITE ROOT FILE\n"
						+ "   rootFile   '" + rootFile.getPath() + "'\n"
						+ "   " + rootDirStr );

				result = this.writeAdminRootFile( rootFile, rootDirStr );

				// ==============    REPOSITORY   ==================
				if ( result )
					{
					if ( CVSProject.debugEntryIO )
					CVSTracer.traceIf( true,
						"CVSProject.writeAdminAndDescend: WRITE REPOSITORYy FILE\n"
							+ "   reposFile  '" + reposFile.getPath() + "'\n"
							+ "   " + dirEntry.getRepository() );

					result =
						this.writeAdminRepositoryFile
							( reposFile, dirEntry.getRepository() );
					}
				}
			}

		if ( ! result )
			{
			CVSLog.logMsg
				( "CVSProject.writeAdminAndDescend: " +
					"ERROR writing admin files '" +entriesFile.getPath()+
					"' et.al." );
			result = false;
			}

		for ( i = 0 ; result && i < entries.size() ; ++i )
			{
			CVSTracer.traceIf( CVSProject.debugEntryIO,
				"CVSProject.writeAdminAndDescend: LOOP i = " + i );

			CVSEntry entry = entries.entryAt(i);

			CVSTracer.traceIf( CVSProject.debugEntryIO,
				"CVSProject.writeAdminAndDescend: "
					+ "LOOP["+ i +"] repository '"+	repository
					+ "' entry '" + entry.getName() + "'" );
			
			if ( entry.isDirectory() )
				{
				// REVIEW I know this is gonna fail on subtrees!!!
				//
				CVSTracer.traceIf( CVSProject.debugEntryIO,
					"CVSProject.writeAdminAndDescend: "
					+ "DESCEND into '" + entry.getFullName() + "'" );

				result = this.writeAdminAndDescend( localRoot, entry );

				CVSTracer.traceIf( CVSProject.debugEntryIO,
					"CVSProject.writeAdminAndDescend: "
					+ "RETURNED from '" + entry.getFullName()
					+ "' with '" + result + "'" );
				}
			} 

		if ( result )
			{
			entries.setDirty( false );
			dirEntry.setDirty( false );
			}

		CVSTracer.traceIf( CVSProject.debugEntryIO,
	"- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -" );
		return result;
		}

	public boolean
	writeAdminEntriesFile( File entriesFile, CVSEntryVector entries )
		{
		boolean		ok = true;
		boolean		result = true;
		CVSEntry	entry = null;
		BufferedWriter	out = null;

		try {
			out = new BufferedWriter(
					new FileWriter( entriesFile ) );
			}
		catch ( Exception ex )
			{
			CVSLog.logMsg
				( "CVSProject.writeAdminEntriesFile: "
					+ "ERROR opening entries file '"
					+ entriesFile.getPath() + "' - "
					+ ex.getMessage() );

			return false;
			}

		for ( int i = 0 ; result && i < entries.size() ; ++i )
			{
			entry = entries.entryAt( i );

			try {
				out.write( entry.getAdminEntryLine() );
				out.newLine();
				}
			catch ( IOException ex )
				{
				CVSLog.logMsg
					( "CVSProject.writeAdminEntriesFile: "
						+ "ERROR writing entries file '"
						+ entriesFile.getPath() + "' - "
						+ ex.getMessage() );

				result = false;
				}
			}

		try {
			out.close();
			}
		catch ( IOException ex )
			{
			CVSLog.logMsg
				( "CVSProject.writeAdminEntriesFile: "
					+ "ERROR closing entries file '"
					+ entriesFile.getPath() + "' - "
					+ ex.getMessage() );

			result = false;
			}

		return result;
		}

	public boolean
	writeAdminRootFile( File rootFile, String rootDirectoryStr )
		{
		boolean			result = true;
		BufferedWriter	out = null;

		try {
			out = new BufferedWriter(
					new FileWriter( rootFile ) );
			}
		catch ( Exception ex )
			{
			CVSLog.logMsg
				( "CVSProject.writeAdminRootFile: "
					+ "failed opening 'Root' file to '"
					+ rootFile.getPath() + "' - " + ex.getMessage() );
			result = false;
			}

		if ( result )
			{
			try {
				out.write( rootDirectoryStr );
				out.newLine();
				out.close();
				}
			catch ( IOException ex )
				{
				CVSLog.logMsg
					( "CVSProject.writeAdminRootFile: "
						+ "failed writing 'Root' file to '"
						+ rootFile.getPath() + "' - " + ex.getMessage() );
				result = false;
				}
			}

		return result;
		}

	public boolean
	writeAdminRepositoryFile( File repFile, String repository )
		{
		boolean			result = true;
		BufferedWriter	out = null;

		if ( CVSProject.deepDebug )
		CVSTracer.traceIf( true,
			"CVSProject.writeAdminRepositoryFile:\n"
			+ "   File:   " + repFile.getPath() + "\n"
			+ "   Repos:  " + repository );

		try {
			out = new BufferedWriter(
						new FileWriter( repFile ) );
			}
		catch ( Exception ex )
			{
			CVSLog.logMsg
				( "CVSProject.writeAdminRepositoryFile: "
					+ "failed opening 'Repository' file to '"
					+ repFile.getPath() + "' - " + ex.getMessage() );
			result = false;
			}

		if ( result )
			{
			try {
				out.write( repository );
				out.newLine();
				out.close();
				}
			catch ( IOException ex )
				{
				CVSLog.logMsg
					( "CVSProject.writeAdminRepositoryFile: "
						+ "failed writing 'Repository' file to '"
						+ repFile.getPath() + "' - " + ex.getMessage() );
				result = false;
				}
			}

		return result;
		}

	public boolean
	isLocalFileModified( CVSEntry entry )
		{
		File entryFile = this.getEntryFile( entry );
		return entry.isLocalFileModified( entryFile );
		}

	/**
	 * This is used for the 'release' command to determine
	 * if the project has any modifications the user might
	 * not want to lose.
	 *
	 * @return True if the project has any changes user might want to save.
	 */

	public boolean
	checkReleaseStatus(
			CVSIgnore ignore,
			Vector mods, Vector adds, Vector rems, Vector unks )
		{
		//
		// NOTE
		// WARNING !!!
		// 
		// THESE METHODS that operate on the localRootDirectory
		// MUST BE VERY CAREFUL!
		//
		// The problem is that the root entry of the project always
		// point to the very root of the project, even if the user
		// "sees" some subtree of the repository in question. When
		// the user is looking at a subtree, and they do a "release",
		// they do not expect the top level to be removed!!!!!!!!
		// They expect the "root level entries" to be released!
		//
		// Ergo, we must compute the local root directory for the
		// release based on both the localRootDirectory and the
		// local directory of one of the root level entries.
		//
		CVSEntryVector rootEntries = this.getRootEntry().getEntryList();
		if ( rootEntries == null || rootEntries.size() == 0 )
			{
			CVSTracer.traceWithStack( "THIS SHOULD NEVER HAPPEN!!" );
			return true;
			}

		this.checkReleaseAndDescend
			( this.getRootEntry(), ignore, mods, adds, rems, unks );

		return ( mods.size() > 0 || adds.size() > 0
					|| rems.size() > 0 || unks.size() > 0 );
		}

	private void
	checkReleaseAndDescend(
			CVSEntry parent, CVSIgnore ignore,
			Vector mods, Vector adds, Vector rems, Vector unks )
		{
		CVSEntryVector entries = parent.getEntryList();

		File dirF = this.getLocalEntryFile( parent );
		String[] list = dirF.list();
		Vector fileV =
			new Vector( (list == null) ? 0 : list.length );

		if ( list != null )
			{
			for ( int i = 0 ; i < list.length ; ++i )
				fileV.addElement( list[i] );
			}

		for ( int i = 0 ; i < entries.size() ; ++i )
			{
			CVSEntry entry = entries.entryAt(i);

			// Anything we have an entry for is not unknown
			fileV.removeElement( entry.getName() );

			if ( entry.isDirectory() )
				{
				this.checkReleaseAndDescend
					( entry, ignore,
						mods, adds, rems, unks );
				}
			else
				{
				if ( entry.isNewUserFile() )
					adds.addElement( entry.getFullName() );
				else if ( entry.isToBeRemoved() )
					rems.addElement( entry.getFullName() );
				else if ( entry.isInConflict() )
					mods.addElement( entry.getFullName() );
				else if ( this.isLocalFileModified( entry ) )
					mods.addElement( entry.getFullName() );
				else if ( this.isLocalFileModified( entry ) )
					mods.addElement( entry.getFullName() );
				}
			}

		for ( int i = 0, sz = fileV.size() ; i < sz ; ++i )
			{
			String fileName = (String) fileV.elementAt( i );
			if ( ! ignore.isFileToBeIgnored( fileName ) )
				{
				// parent is a dir entry, which always has a '/'
				// on the end of its fullname.
				unks.addElement( parent.getFullName() + fileName );
				}
			}
		}

	public void
	pruneEmptySubDirs( boolean saveAdminFiles )
		{
		this.pruneEmptySubDirs( this.getRootEntry() );

		if ( saveAdminFiles )
			{
			this.writeAdminFiles();
			}
		}

	public void
	pruneEmptySubDirs( CVSEntry parent )
		{
		CVSEntryVector entries = parent.getEntryList();
		for ( int i = entries.size() - 1 ; i >= 0  ; --i )
			{
			CVSEntry entry = entries.getEntryAt( i );
			if ( entry.isDirectory() )
				{
				File dirF = this.getEntryFile( entry );
				String[] list = dirF.list();
				if ( list.length == 0 )
					{
					this.descendAndDelete( dirF );
					parent.removeEntry( entry );
					}
				else if ( list.length == 1 && list[0].equals( "CVS" ) )
					{
					File cvsF = new File( dirF, "CVS" );
					if ( ( ! cvsF.exists() ) || cvsF.isDirectory() )
						{
						this.descendAndDelete( dirF );
						parent.removeEntry( entry );
						}
					}
				else
					{
					this.pruneEmptySubDirs( entry );
					}
				}
			}
		}

	public void
	releaseProject()
		{
		//
		// NOTE
		// WARNING !!!
		// 
		// THESE METHODS that operate on the localRootDirectory
		// MUST BE VERY CAREFUL!
		//
		// The problem is that the root entry of the project always
		// point to the very root of the project, even if the user
		// "sees" some subtree of the repository in question. When
		// the user is looking at a subtree, and they do a "release",
		// they do not expect the top level to be removed!!!!!!!!
		// They expect the "root level entries" to be released!
		//
		// Ergo, we must compute the local root directory for the
		// release based on both the localRootDirectory and the
		// local directory of one of the root level entries.
		//
		CVSEntryVector rootEntries = this.getRootEntry().getEntryList();
		if ( rootEntries == null || rootEntries.size() == 0 )
			{
			CVSTracer.traceWithStack( "THIS SHOULD NEVER HAPPEN!!" );
			return;
			}

		for ( int i = rootEntries.size() - 1 ; i >= 0  ; --i )
			{
			CVSEntry entry = rootEntries.getEntryAt( i );

			File eFile = this.getEntryFile( entry );

			//
			// SANITY CHECK
			// To at least make CERTAIN that we never delete anythig
			// above the localRootDirectory...
			//

			if ( ! CVSCUtilities.isSubpathInPath
					( this.getLocalRootPath(), eFile.getPath() ) )
				{
				String msg =
					"ROOT '" + this.getLocalRootPath()
					+ "' NOT parent of '" + eFile.getPath() + "'";
				CVSTracer.traceWithStack( msg );
				return;
				}

			this.descendAndDelete( eFile );
			}

		// UNDONE We need to adjust the "top level Entries" here...

		File rootDir = new File( this.getLocalRootDirectory() );
		if ( rootDir.exists() )
			{
			String[] files = rootDir.list();
			if ( files == null || files.length < 2 )
				{
				// NOTE
				// This is very important! As long as we have
				// the top level CVS admin directory, it is critical
				// that we only delete it if it is the ONLY file 
				// left at the top level!
				//
				// UNTIL, that is, we fix the above UNDONE wrt Entries.
				//
				boolean doit = true;
				if ( files.length == 1 )
					{
					if ( files[0].equals( "CVS" ) )
						{
						File cvsDir = new File( rootDir, "CVS" );

						files = cvsDir.list();
						for ( int c = 0 ; c < files.length ; ++c )
							{
							File dFile = new File( cvsDir, files[c] );
							dFile.delete();
							}

						cvsDir.delete();
						}
					else
						{
						doit = false;
						}
					}

				if ( doit )
					{
					rootDir.delete();
					}
				}
			}
		}

	private void
	descendAndDelete( File eFile )
		{
		if ( eFile.isDirectory() )
			{
			String[] files = eFile.list();

			if ( files != null )
				{
				for ( int i = 0 ; i < files.length ; ++i )
					{
					File f = new File( eFile, files[i] );
					if ( f.exists() )
						{
						this.descendAndDelete( f );
						}
					}
				}
			}

		eFile.delete();
		}

	// 
	// This is tricky.
	// We have to go back to the project's entries list
	// to make the check, since the entry sent from the
	// server will not reflect the local timestamp!
	//
	public boolean
	checkOverwrite( CVSEntry entry, File file )
		{
		// UNDONE
		// The current algorithm is very weak.
		boolean result = true;

		if ( CVSProject.deepDebug )
		CVSTracer.traceIf( true,
			"CVSProject.checkOverWrite( "
				+ entry.getFullName() + ", "
				+ file.getPath() + " )" );

		if ( ! file.exists() )
			{
			// Does not exist, no problem overwriting...
			 CVSTracer.trace
				( "CVSProject.checkOverWrite: FILE '"
					+ file.getPath() + "' DOES NOT EXIST" );
			return true;
			}

		CVSEntry checkEntry =
			this.locateEntry( entry.getFullName() );

		if ( checkEntry == null )
			{
			NullPointerException ex =
				new NullPointerException
					( "locateEntry(" + entry.getFullName() + ") returns null!" );
			CVSLog.traceMsg( ex, "CVSProject.checkOverWrite:" );
			return false;
			}

		// Check the timstamps...
		result = ! checkEntry.isLocalFileModified( file );

		 CVSTracer.traceIf( false,
			"CVSProject.checkOverWrite: RESULT '" +result+ "'" );

		return result;
		}

	public CVSEntry
	locateEntry( String fullPath )
		{
		CVSEntry	entry = null;

		if ( CVSProject.deepDebug )
		CVSTracer.traceIf( true,
			"CVSProject.locateEntry( " + fullPath + " )" );

		int index = fullPath.lastIndexOf( '/' );
		if ( index < 0 )
			{
			CVSTracer.traceWithStack
				( "CVSProject.locateEntry: NO SLASH IN '" +fullPath+ "'" );
			entry = this.rootEntry.locateEntry( fullPath );
			}
		else
			{
			String name = fullPath.substring( index + 1 );
			String localDirectory =
				fullPath.substring( 0, index + 1 );

			CVSEntry parentEntry =
				this.getPathTableEntry( localDirectory );

			if ( parentEntry == null )
				{
				CVSTracer.traceWithStack(
					"CVSProject.locateEntry: LOCAL DIRECTORY '"
					+ localDirectory + "' NOT IN TABLE" );
				}
			else
				{
				if ( false )
				CVSTracer.traceIf( false,
					"CVSProject.locateEntry: PARENT '"
					+ parentEntry.getFullName() + "'" );

				entry = parentEntry.locateEntry( name );
				}
			}

		if ( CVSProject.deepDebug )
		CVSTracer.traceIf( true,
			"CVSProject.locateEntry: fullPath '"
				+ fullPath + "' resulting entry '" +
				( entry == null ? "(null)" : entry.getFullName() ) );

		return entry;
		}

	public boolean
	ensureEntryHierarchy( String localDirectory, String repository )
		{
		if ( CVSProject.deepDebug )
		CVSTracer.traceIf( true,
			"CVSProject.ENSUREEntryHierarchy:\n" +
			"   localDirectory '" + localDirectory + "'\n" +
			"   repository '" + repository );

		if ( localDirectory.equals( "./" ) )
			{
			if ( this.rootEntry == null )
				{
				if ( CVSProject.deepDebug )
				CVSTracer.traceIf( true,
					"CVSProject.ENSUREEntryHierarchy: ESTABLISH '.' ROOT ENTRY\n" +
					"   repository '" + repository );

				this.establishRootEntry( repository );

				return true;
				}
			else
				{
				// REVIEW
				// Should this verify the 'repository' exists?!
				// Given the note below, I do not think there is
				// any methd that would properly parse this case.
				// Ergo, we hope it can only occur if the entry
				// already exists.
				//
				CVSTracer.traceIf( true,
					"CVSProject.ENSUREEntryHierarchy: IGNORING '.' localDirectory!\n" +
					"   repository '" + repository );
				return true;
				}
			}

		CVSEntry lookupEntry =
			this.getPathTableEntry( localDirectory );

		// The local directory is the Path Table.
		// Thus, the entry must already exist, return.
		//
		if ( lookupEntry != null )
			return true;

		//
		// We are going to get response items that sometimes have no
		// corresponding entry in the Path Table. Further, these entries
		// have no corresponding hierarchy in the CVSEntry tree, which
		// in turn implies no nodes in the EntryTree of jCVS.
		//
		// This code will attempt to parse the incoming item and ensure
		// that the CVSEntry's in the item's path exist.
		//
		// We have updated this code to make a HUGE NEW ASSUMPTION!!!!!
		//
		// We are assuming that we will never see a request to ensure a
		// hierarchy in which the parent nodes do not exist. In other
		// words, we should never see a case where the 'local directory'
		// of the item contains more than one subdirectory beyond what
		// is "currently ensured". In other words, items will not be
		// seen for a subdirectory that is more than one level below
		// a level we already have ensured.
		//
		// If that assumption is wrong, we are going to have to make
		// another assumption that the only time these paths would be
		// out of sync is when a module alias is used. In these cases,
		// the paths MUST be in sync when working back from the end.
		// I can not think of a case in which this would not be true.
		//
		// Example: ( module alias "api api/machdep/linux" )
		//
		//   Entry           LocalDir               Repository
		//   ROOT            ./                     /cvs/
		//     api           ./api/                 /cvs/api/machdep/linux
		//        include    ./api/include/         /cvs/api/machdep/linux/include
		//
		// In that example, if we got 'include' before we got 'api', we would have
		// to work back from include up to './', and assume that the top level entry
		// subsumes the remainder of the repository path as its own, since only
		// aliases should have this case, and aliases can only applt to the top level.
		//

		if ( CVSProject.deepDebug )
		CVSTracer.traceIf( true,
			"CVSProject.ensureEntryHierarchy: START LOOP\n"
			+ "   localDirectory '" + localDirectory + "'\n"
			+ "   repository     '" + repository + "'" );

		CVSEntry curEntry = this.rootEntry;

		// FIRST, we work FORWARD eliminating as much of the path as
		//        we can find in the Path Table.
		//
		for ( ; ; )
			{
			int length = curEntry.getLocalDirectory().length();
			int slashIdx = localDirectory.indexOf( "/", length );

			if ( CVSProject.deepDebug )
			CVSTracer.traceIf( true,
				"CVSProject.ensureEntryHierarchy: TOP LOOP\n"
				+ "   length = " + length + "  slashIdx = " + slashIdx + "\n"
				+ "   curEntry: " + curEntry.dumpString( "   " ) );

			if ( slashIdx == -1 )
				break;

			String subLocal = localDirectory.substring( 0, slashIdx + 1 );
			CVSEntry pathEntry = this.getPathTableEntry( subLocal );

			if ( CVSProject.deepDebug )
			CVSTracer.traceIf( true,
				"CVSProject.ensureEntryHierarchy: "
				+ "LOOP lookup path '" + subLocal + "' returns:\n"
				+ ( pathEntry == null
					? "NULL" : pathEntry.dumpString( "   " ) ) );

			if ( pathEntry == null )
				break;

			curEntry = pathEntry;
			}
/*
		index slashCnt = CVSCUtilities.getSlashCount( subLocal );
		if ( slashCnt == 1 )
			{
			// GREAT! We have a simple one level entry, which is simple!
			this.pathTable.put( localDirectory, repository );

			String name =
				CVSCUtilities.stripFinalSlash
					( localDirectory.substring
						( curEntry.getRepository().length() ) );

			if ( CVSProject.deepDebug )
			CVSTracer.traceIf( true,
				"CVSProject.ensureEntryHierarchy: ADD NEW ENTRY:\n"
				+ "   name       '" + name + "\n"
				+ "   repository '" + repository + "'\n"
				+ "   localdir   '" + localDirectory + "'" );

			CVSEntry newEntry = new CVSEntry();
			newEntry.setName( name );
			newEntry.setRepository( repository );
			newEntry.setLocalDirectory( localDirectory );
			newEntry.setVersion( "" );
			newEntry.setTimestamp( "" );
			newEntry.setOptions( "" );
			newEntry.setTag( "" );
			newEntry.setDirty( true );

			// NOTE setDirectoryEntryList() sets 'isDir'-ness of entry.
			newEntry.setDirectoryEntryList( new CVSEntryVector() );

			if ( CVSProject.deepDebug )
			CVSTracer.traceIf( true,
				"CVSProject.ensureEntryHierarchy: NEW ENTRY =\n"
				+ newEntry.dumpSting( "   " ) );

			curEntry.appendEntry( newEntry );
			return true;
			}
*/
		//
		// Ok. We now have a 'local directory' subpath that does not exist
		// yet. We will work backwards building the elements in the path,
		// into a Vector. Then, we will roll then out forwards.
		//

		Vector elements = new Vector();

		String workingRepos =
			CVSCUtilities.ensureFinalSlash( repository );

		int reposIdx = workingRepos.length() - 1;
		int localIdx = localDirectory.length() - 1;

		if ( CVSProject.deepDebug )
		CVSTracer.traceIf( true,
			"CVSProject.ensureEntryHierarchy: START MULTI LEVEL LOOP\n"
			+ "         reposIdx =  " + reposIdx
			+ "  localIdx =  " + localIdx + "\n"
			+ "   localDirectory = '" + localDirectory + "'\n"
			+ "     workingRepos = '" + workingRepos + "'\n"
			+ "         curEntry = \n" + curEntry.dumpString( "   " ) );

		for ( ; ; )
			{
			int newRepIdx = workingRepos.lastIndexOf( "/", reposIdx - 1 );
			int newLocIdx = localDirectory.lastIndexOf( "/", localIdx - 1 );

			if ( CVSProject.deepDebug )
			CVSTracer.traceIf( true,
				"CVSProject.ensureEntryHierarchy: PARSE PATH LOOP\n"
				+ "    reposIdx =  " + reposIdx
				+ "    localIdx =  " + localIdx + "\n"
				+ "   newRepIdx =  " + newRepIdx
				+ "   newLocIdx =  " + newLocIdx );

			String name = localDirectory.substring( newLocIdx + 1, localIdx );
			String subRepos = workingRepos.substring( 0, reposIdx );		// drop final slash
			String subLocal = localDirectory.substring( 0, localIdx + 1 );	// include final slash

			if ( CVSProject.deepDebug )
			CVSTracer.traceIf( true,
				"CVSProject.ensureEntryHierarchy: CHECK PATH\n"
				+ "       name = '" + name + "'\n"
				+ "   subRepos = '" + subRepos + "'\n"
				+ "   subLocal = '" + subLocal + "'" );

			if ( subLocal.equals( curEntry.getLocalDirectory() ) )
				{
				if ( CVSProject.deepDebug )
				CVSTracer.traceIf( true,
					"CVSProject.ensureEntryHierarchy: HIT CURENTRY, BREAK" );
				break;
				}

			if ( CVSProject.deepDebug )
			CVSTracer.traceIf( true,
				"CVSProject.ensureEntryHierarchy: ADDED PATH!" );

			String[] parms = { name, subRepos, subLocal };
			elements.addElement( parms );

			reposIdx = newRepIdx;
			localIdx = newLocIdx;
			}

		for ( int i = elements.size() - 1 ; i >= 0 ; --i )
			{
			String[] parms = (String[]) elements.elementAt(i);

			CVSEntry newEntry = new CVSEntry();
			newEntry.setDirty( true );
			newEntry.setName( parms[0] );
			newEntry.setRepository( parms[1] );
			newEntry.setLocalDirectory( parms[2] );
			newEntry.setVersion( "" );
			newEntry.setTimestamp( "" );
			newEntry.setOptions( "" );
			newEntry.setTag( "" );

			// NOTE setDirectoryEntryList() sets 'isDir'-ness of entry.
			newEntry.setDirectoryEntryList( new CVSEntryVector() );

			if ( CVSProject.deepDebug )
			CVSTracer.traceIf( true,
				"CVSProject.ensureEntryHierarchy: "
				+ "MULTI LEVEL APPEND NEW ENTRY\n"
				+ "   CUR ENTRY:" + curEntry.dumpString( "   " ) + "\n"
				+ "   NEW ENTRY:" + newEntry.dumpString( "   " ) );

			this.pathTable.put( newEntry.getLocalDirectory(), newEntry );

			curEntry.appendEntry( newEntry );
			curEntry = newEntry;
			}

		return true;
		}

	// subpath is the local path up to the name.
	public boolean
	ensureProperWorkingDirectory
			( String localRoot, String subPath, boolean ensureAdmin )
		{
		int		index;
		boolean result = true;
		
		if ( CVSProject.deepDebug )
		CVSTracer.traceIf( true,
			"CVSClient.ENSURE Proper WORKING Directory:\n"
			+ "    localRoot '" + localRoot + "'\n"
			+ "      subPath '" + subPath + "'\n"
			+ "    ensureAdm '" + ensureAdmin + "'" );

		subPath = CVSCUtilities.stripFinalSlash( subPath );
		if ( subPath.startsWith( "./" ) )
			subPath = subPath.substring(2);
		
		String remainder = subPath;

		for ( ; result && remainder.length() > 0 ; )
			{
			index = remainder.indexOf( '/' );
			if ( index < 0 )
				{
				subPath = remainder;
				remainder = "";
				}
			else
				{
				subPath = remainder.substring( 0, index );
				remainder = remainder.substring( index + 1 );
				}

			// UNDONE separator
			File dir = new File( localRoot + "/" + subPath );
			
			if ( ! dir.exists() )
				dir.mkdir();

			if ( ! dir.exists() )
				{
				result = false;
				CVSLog.logMsg(
					"ERROR could not create local path '"
					+ dir.getPath() + "'" );
				}
			else if ( ! dir.isDirectory() )
				{
				result = false;
				CVSLog.logMsg(
					"ERROR local directory '" + dir.getPath()
					+ "' is not a directory!" );
				}
			else if ( result && ensureAdmin )
				{
				File adminDir = // UNDONE separator
					new File( dir.getPath() + "/CVS" );

				if ( CVSProject.deepDebug )
				CVSTracer.traceIf( true,
					"CVSClient.ensureProperWorkingDirectory: ADMINDIR '"
					+ adminDir.getPath() + "'" );

				if ( ! adminDir.exists() )
					adminDir.mkdir();
				
				if ( ! adminDir.exists() )
					{
					result = false;
					CVSLog.logMsg(
						"ERROR could not create Admin path '"
						+ this.localAdminDirFile.getPath() + "'" );
					}
				}

			// UNDONE separator
			localRoot = localRoot + "/" + subPath;
			}

		return result;
		}

	public boolean
	ensureLocalTree( File localFile, boolean ensureAdmin )
		{ 
		int		index;
		boolean result = true;

		String	localPath = localFile.getPath();

		String	localRoot =
			CVSCUtilities.exportPath( this.localRootDirectory );

		index = localPath.lastIndexOf( File.separatorChar );
		if ( index < 0 )
			{
			return true;
			}
		
		String localSub = localPath.substring( 0, index );

		if ( ! CVSCUtilities.isSubpathInPath( localRoot, localSub ) )
			{
			CVSLog.logMsg(
				"CVSClient.ensureLocalTree:  LOCAL SUBDIR IS NOT IN ROOT!!\n"
				+ "   localRoot   '" +localRootDirectory+ "'\n"
				+ "   localSubDir '" +localSub+ "'" );
			result = false;
			}

		// REVIEW
		//
		// Per Thorsten Ludewig <T.Ludewig@FH-Wolfenbuettel.DE>, who
		// reported that this eliminated an index out of bounds exception,
		// which I think other users were reporting also.
		//
		// Need to review to make sure that "./" is the correct substitution.
		//
		if ( localSub.equals( localRoot ) )
			{
			localSub = "./";
			}
		else
			{
			localSub = localSub.substring( localRoot.length() + 1 );
			}

		if ( CVSProject.deepDebug )
		CVSTracer.traceIf( true,
			"CVSClient.ensureLocalTree: tempFile '"
				+ localFile.getPath() + "' localPath '"
				+ localPath + "' --> '" + localSub + "'" );

		result =
			this.ensureProperWorkingDirectory
				( this.localRootDirectory, localSub, ensureAdmin );

		return result;
		}

	public void
	moveLocalFile( File localFile, String versionStr )
			throws CVSFileException
		{
		//
		// REVIEW
		// UNDONE
		// Should we capitualate and use the 'standard' notation
		// of '.#name.version' (e.g., '.#main.c.1.4')? I prefer
		// this naming scheme (e.g., '#main.c.1.4').
		//
		String localPath = localFile.getPath();

		String base = "";
		String name = localPath;
		int index = localPath.lastIndexOf( '/' );
		if ( index >= 0 )
			{
			base = localPath.substring( 0, index );
			name = localPath.substring( index + 1 );
			}

		String newPath = base + "/" + "#" + name + "." + versionStr;

		CVSTracer.traceIf( CVSProject.overTraceProcessing,
			"CVSProject.moveLocalFile: move '" +localFile.getPath()
			+ "' to '" +newPath+ "'" );

		File toFile = new File( newPath );

		boolean result = localFile.renameTo( toFile );

		CVSTracer.traceIf( false,
			"CVSProject.moveLocalFile: rename returns '" +result+ "'" );

		if ( ! result )
			throw new CVSFileException
				( "failed renaming '" + localFile.getPath()
					+ "' to '" + toFile.getPath() + "'" );
		}

	public boolean
	updateLocalFile( CVSResponseItem item, CVSEntry entry, File localFile )
		{
		boolean result = true;

		int trans = CVSCUtilities.computeTranslation( entry );
		
		result = this.copyFile
			( item.getFile(), localFile, trans, item.isGZIPed() );

		return result;
		}

	public boolean
	copyFile( File from, File to, int translation, boolean isGZIPed )
		{
		boolean result = false;

		CVSTracer.traceIf( CVSProject.overTraceProcessing,
			"CVSProject.copyFile: from '" + from.getPath()
			+ "' to '" + to.getPath() + "' trans '"
			+ (translation==CVSClient.TRANSLATE_ASCII ? "ASCII" : "NONE")
			+ "' gzip-ed? '" + isGZIPed + "'" );

		switch ( translation )
			{
			case CVSClient.TRANSLATE_ASCII:
				result = this.copyFileAscii( from, to, isGZIPed );
				break;

			case CVSClient.TRANSLATE_NONE:
			default:
				result = this.copyFileRaw( from, to, isGZIPed );
				break;
			}

		return result;
		}

	public boolean
	copyFileAscii( File from, File to, boolean isGZIPed )
		{
		boolean	ok = true;

		BufferedReader	in = null;
		BufferedWriter	out = null;

		String line = null;

		try {
			if ( isGZIPed )
				{
				in = this.new NewLineReader
					( new InputStreamReader
						( new GZIPInputStream
							( new FileInputStream( from ) ) ) );
				}
			else
				{
				in = new NewLineReader( new FileReader( from ) );
				}
			}
		catch ( IOException ex )
			{
			in = null;
			ok = false;
			CVSLog.logMsg
				( "CVSProject.copyFileAscii: failed creating in reader: "
					+ ex.getMessage() );
			}

		if ( ok )
		try {
			out = new BufferedWriter( new FileWriter( to ) );
			}
		catch ( IOException ex )
			{
			out = null;
			ok = false;
			CVSLog.logMsg
				( "CVSProject.copyFileAscii: failed creating out writer: "
					+ ex.getMessage() );
			}

		if ( out == null || in == null )
			{
			ok = false;
			CVSLog.logMsg
				( "CVSProject.copyFileAscii: failed creating '"
					+ (out == null ? "output writer" : "input reader") + "'" );
			}

		if ( ok )
			{
			for ( ; ; )
				{
				try {
					line = in.readLine();

					if ( line == null ) break;

					out.write( line );
					out.newLine();
					}
				catch ( IOException ex )
					{
					CVSLog.logMsg
						( "CVSProject.copyFileAscii: failed during copy: "
							+ ex.getMessage() );
					ok = false;
					break;
					}
				}

			try { out.flush(); }
			catch ( IOException ex )
				{
				CVSLog.logMsg
					( "CVSProject.copyFileAscii: failed flushing output: "
						+ ex.getMessage() );
				ok = false;
				}
			}

		try {
			if ( in != null ) in.close();
			if ( out != null ) out.close();
			}
		catch ( IOException ex )
			{
			CVSLog.logMsg
				( "CVSProject.copyFileAscii: failed closing files: "
					+ ex.getMessage() );
			ok = false;
			}

		return ok;
		}

	public boolean
	copyFileRaw( File from, File to, boolean isGZIPed )
		{
		int		bytes;
		long	fileSize;
		boolean	ok = true;

		BufferedInputStream		in = null;
		BufferedOutputStream	out = null;

		String line = null;

		try {
			if ( isGZIPed )
				{
				in = new BufferedInputStream
						( new GZIPInputStream
							( new FileInputStream( from ) ) );
				}
			else
				{
				in = new BufferedInputStream(
						new FileInputStream( from ) );
				}
			}
		catch ( Exception ex )
			{
			in = null;
			ok = false;
			CVSLog.logMsg
				( "CVSProject.copyFileRaw: failed creating in reader: "
					+ ex.getMessage() );
			}

		if ( ok )
			{
			try {
				out = new BufferedOutputStream(
						new FileOutputStream( to ) );
				}
			catch ( Exception ex )
				{
				out = null;
				ok = false;
				CVSLog.logMsg
					( "CVSProject.copyFileRaw: failed creating out writer: "
						+ ex.getMessage() );
				}
			}

		if ( out == null || in == null )
			{
			ok = false;
			CVSLog.logMsg
				( "CVSProject.copyFileRaw: failed creating '"
					+ (out == null ? "output writer" : "input reader") + "'" );
			}

		if ( ok )
			{
			byte[]	buffer;
			buffer = new byte[8192];

			fileSize = from.length();
			for ( ; ; )
				{
				try {
					bytes = in.read( buffer, 0, 8192 );
					}
				catch ( IOException ex )
					{
					ok = false;
					CVSLog.logMsg
						( "CVSProject.copyFileRaw: "
							+ "ERROR reading file data:\n   "
							+ ex.getMessage() );
					break;
					}

				if ( bytes < 0 )
					break;

				try { out.write( buffer, 0, bytes ); }
				catch ( IOException ex )
					{
					ok = false;
					CVSLog.logMsg
						( "CVSProject.copyFileRaw: "
							+ "ERROR writing output file:\n   "
							+ ex.getMessage() );
					break;
					}
				}
			}

		try {
			if ( in != null ) in.close();
			if ( out != null ) out.close();
			}
		catch ( IOException ex )
			{
			CVSLog.logMsg
				( "CVSProject.copyFileRaw: failed closing files: "
					+ ex.getMessage() );
			ok = false;
			}

		return ok;
		}

	private
	class		NewLineReader
	extends		BufferedReader
		{
		public
		NewLineReader( Reader in )
			{
			super( in );
			}

		public String
		readLine()
			{
			char ch;
			StringBuffer line =
				new StringBuffer( 132 );

			try {
				for ( ; ; )
					{
					int inByte = this.read();
					if ( inByte == -1 )
						{
						if ( line.length() == 0 )
							line = null;
						break;
						}

					ch = (char) inByte;
					if ( ch == 0x0A )
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

			return ( line != null ? line.toString() : null );
			}
		}

	//
	// CVS USER INTERFACE METHODS
	//

	// Currently, we stub these out.
	public void
	uiDisplayProgressMsg( String message )
		{
		}

	public void
	uiDisplayProgramError( String error )
		{
		}

	public void
	uiDisplayResponse( CVSResponse response )
		{
		}

	//
	// END OF CVS USER INTERFACE METHODS
	//


	public String
	toString()
		{
		return "CVSProject: name '" + this.repository + "'";
		}

	public StringBuffer
	dumpCVSProject( StringBuffer buf, String description )
		{
		buf.append( "##############################################################\n" );
		buf.append( "#\n" );
		buf.append( "# CVSProject  '" ).append( this.repository ).append( "'\n" );
		buf.append( "#\n" );
		buf.append( "# Description '" ).append( description ).append( "'\n" );
		buf.append( "#\n" );
		buf.append( "##############################################################\n" );

		buf.append( "\n" );

		buf.append( "  valid?      '" ).append( this.valid ).append( "'\n" );
		buf.append( "  isPServer?  '" ).append( this.isPServer ).append( "'\n" );
		buf.append( "  allowsGzip? '" ).append( this.allowGzipFileMode ).append( "'\n" );
		buf.append( "  gzipLevel   '" ).append( this.gzipStreamLevel ).append( "'\n" );
		buf.append( "  connMethod  '" ).append( this.connMethod ).append( "'\n" );
		buf.append( "  serverCmd   '" ).append( this.serverCommand ).append( "'\n" );
		buf.append( "  rshProcess  '" ).append( this.rshProcess ).append( "'\n" );
		buf.append( "  userName    '" ).append( this.userName ).append( "'\n" );
		buf.append( "  tempPath    '" ).append( this.tempPath ).append( "'\n" );
		buf.append( "  repository  '" ).append( this.repository ).append( "'\n" );
		buf.append( "  rootDir     '" ).append( this.rootDirectory ).append( "'\n" );
		buf.append( "  localRoot   '" ).append( this.localRootDirectory ).append( "'\n" );

		buf.append( "\n" );

		buf.append( "------- Path Table -------\n" );

		buf.append( "\n" );

		Enumeration enum = this.pathTable.keys();
		for ( ; enum.hasMoreElements() ; )
			{
			String key = (String) enum.nextElement();
			CVSEntry val = (CVSEntry) this.pathTable.get( key );
			buf.append( key ).append( " =\n\n   " );
			buf.append( val.dumpString() ).append( "\n\n" );
			}

		buf.append( "\n" );

		buf.append( "------- Root Entry -------\n" );
		if ( this.rootEntry == null )
			{
			buf.append( "   Root Entry Is Null.\n" );
			}
		else
			{
			buf.append( "  " ).append
				( this.rootEntry.dumpString() ).append( "\n" );
			}

		buf.append( "\n" );

		buf.append( "------- ENTRY TREE -------\n" );

		buf.append( "\n" );
		
		buf.append( "" ).append( "./" ).append( "\n" );
		this.dumpEntry( buf, "   ", this.rootEntry );

		// DUMP SET VARIABLES

		// DUMP CVSIGNORE

		// DUMP ENTRY TREE...

		// DUMP CLIENT

		return buf;
		}


	public StringBuffer
	dumpEntry( StringBuffer buf, String prefix, CVSEntry dirEntry )
		{
		CVSEntryVector entries = dirEntry.getEntryList();

		for ( int i = 0, sz = entries.size() ; i < sz ; ++i )
			{
			CVSEntry entry = entries.getEntryAt(i);

			buf.append( prefix ).append( entry.getFullName() ).append( "\n" );

			if ( entry.isDirectory() )
				{
				this.dumpEntry( buf, prefix + "   ", entry );
				}
			}

		return buf;
		}

	}
