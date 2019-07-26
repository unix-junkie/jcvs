/*
** Java CVS client application package.
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

package com.ice.jcvsii;

import java.awt.*;
import java.io.*;
import java.util.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.activation.*;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.ice.pref.*;
import com.ice.cvsc.CVSProject;
import com.ice.cvsc.CVSCUtilities;
import com.ice.util.AWTUtilities;
import com.ice.util.ResourceUtilities;
import com.ice.util.TempFileManager;


/**
 * The Configuration class.
 *
 * @version $Revision: 1.6 $
 * @author Timothy Gerard Endres,
 *  <a href="mailto:time@ice.com">time@ice.com</a>.
 */

public
class		Config
implements	ConfigConstants, PropertyChangeListener
	{
	static public final String		RCS_ID = "$Id: Config.java,v 1.6 2000/06/12 23:28:29 time Exp $";
	static public final String		RCS_REV = "$Revision: 1.6 $";


	/**
	 * Set to true to get processing debugging on stderr.
	 */
	private boolean					debug;

	/**
	 * The instance of the DEFAULT Config.
	 */
	private static Config			instance;

	/**
	 * The instance of the DEFAULT Preferences.
	 */
	private UserPrefs				defPrefs;

	/**
	 * The instance of the USER Preferences.
	 */
	private UserPrefs				userPrefs;

	/**
	 * The instance of the USER Preferences.
	 */
	private UserPrefs				editSpec;

	/**
	 * The instance of the Server Definition Preferences.
	 */
	private UserPrefs				defServers;

	/**
	 * The instance of the Server Definition Preferences.
	 */
	private UserPrefs				userServers;

	/**
	 * The cached vector of server definitions parsed from the preferences.
	 */
	private Vector					servers;

	/**
	 * The vector of server definitions parsed from the preferences.
	 */
	private String					mimeFileName = null;
	private String					mailcapFileName = null;

	/**
	 * The cached table of exec commands from the preferences.
	 */
	private PrefsTupleTable			execCmdTable;

	/**
	 * The following fields are set based on the OS we are
	 * on. The get initialized in establishOSDistinctions().
	 */
	private String					defMailcapFilename;
	private String					defMimetypesFilename;
	private String					userPrefsFilename;
	private String					userServersFilename;
	private String					lastDitchTempDirname;


	/**
	 * We create an instance when the Class is loaded
	 * and work with the instance.
	 */
	static
		{
		Config.instance = new Config();
		}

	public static Config
	getInstance()
		{
		return Config.instance;
		}

	public static UserPrefs
	getPreferences()
		{
		return Config.getInstance().getPrefs();
		}

	public
	Config()
		{
		this.debug = false;
		this.defPrefs = null;
		this.editSpec = null;
		this.userPrefs = null;
		this.defServers = null;
		this.userServers = null;
		this.execCmdTable = null;
		}

	public void
	setDebug( boolean debug )
		{
		this.debug = debug;
		}

	public UserPrefs
	getPrefs()
		{
		return this.userPrefs;
		}

	public String
	getUserPrefsFilename()
		{
		return this.userPrefsFilename;
		}

	public String
	getUserServersFilename()
		{
		return this.userServersFilename;
		}

	public String
	getDefaultMailcapFilename()
		{
		return this.defMailcapFilename;
		}

	public String
	getDefaultMimetypesFilename()
		{
		return this.defMimetypesFilename;
		}

	/**
	 * This is guarenteeed to return a String which is the best
	 * representation of the temporary directory that we can come
	 * up with.
	 */
	public String
	getTemporaryDirectory()
		{
		return this.getPrefs().getProperty
			( GLOBAL_TEMP_DIR, this.lastDitchTempDirname );
		}

	/**
	 * This method sets up some fields that depend on the
	 * OS we are running on. Yeah, yeah, write once...
	 */
	private void
	establishOSDistinctions()
		{
		String osName = System.getProperty( "os.name" );
		osName = osName.toLowerCase();

		if ( osName.startsWith( "mac os" ) )
			{
			System.err.println( "Assuming a Macintosh platform." );
			this.userPrefsFilename = "jCVS Preferences";
			this.userServersFilename = "jCVS Servers";
			this.defMailcapFilename = "jCVS Mailcap";
			this.defMimetypesFilename = "jCVS Mimetypes";
			this.lastDitchTempDirname = "";
			}
		else if ( osName.startsWith( "windows" ) )
			{
			System.err.println( "Assuming a Windows platform." );
			this.userPrefsFilename = "jcvsii.txt";
			this.userServersFilename = "jcvsdef.txt";
			this.defMailcapFilename = "jcvsmailcap.txt";
			this.defMimetypesFilename = "jcvsmime.txt";
			this.lastDitchTempDirname = ".";
			}
		else if ( osName.startsWith( "os/2" ) )
			{
			System.err.println( "Assuming an OS/2 platform." );
			this.userPrefsFilename = "jcvsii.txt";
			this.userServersFilename = "jcvsdef.txt";
			this.defMailcapFilename = "jcvsmailcap.txt";
			this.defMimetypesFilename = "jcvsmime.txt";
			this.lastDitchTempDirname = ".";
			}
		else // we assume UNIX-style
			{
			System.err.println( "Assuming a UNIX platform." );
			this.userPrefsFilename = ".jcvsii";
			this.userServersFilename = ".jcvsdef";
			this.defMailcapFilename = ".jcvsmailcap";
			this.defMimetypesFilename = ".jcvsmime";
			this.lastDitchTempDirname = ".";
			}
		}

	//
	// NOTE Do not call CVSLog methods in here, as the log file
	//      has not yet been established, because it requires a
	//      property!
	//
	// This is nearly the very first thing that is done at runtime.
	//
	public void
	initializePreferences( String prefix )
		{
		this.establishOSDistinctions();

		//
		// NOTE
		// WARNING !!!
		//
		// These two statements are required with the JRE 1.2.
		// If they are not included, and there is no user preferences
		// file at user.home, then the lack of the preferences being
		// loaded causes a very queer ClassLoader error where the
		// activation classes will not be found by the method in the
		// package itself, even while our package *can* find the
		// classes! Wow! Here is the stack trace:
		//
		// java.lang.NoClassDefFoundError: javax/activation/MailcapCommandMap
		//    at javax.activation.MailcapCommandMap.class$(MailcapCommandMap.java:100)
		//    at javax.activation.MailcapCommandMap.<init>(MailcapCommandMap.java:139)
		//    at javax.activation.MailcapCommandMap.<init>(MailcapCommandMap.java:200)
		//    at com.ice.jcvsii.Config.loadMailCap(Config.java:816)
		//    at com.ice.jcvsii.JCVS$Initiator.run(JCVS.java:223)
		//
		// Anyway, these two lines appear to solve the problem by
		// loading these two "problem classes" before our code
		// (apparently) messes up the activation class loader.
		//

		MailcapCommandMap cMap = new MailcapCommandMap();
		MimetypesFileTypeMap fMap = new MimetypesFileTypeMap();

		// This creates a new UserPrefs with System.getProperties()
		// as its default properties.

		this.defPrefs = new UserPrefs( "jCVSII.Defaults" );

		// This creates a new UserPrefs with this.defPrefs as its
		// default properties. This will be the properties that are
		// stored in the user's home directory as the application's
		// configuration settings.

		this.userPrefs =
			new UserPrefs( "jCVSII.Config", this.defPrefs );

		UserPrefs.setInstance( this.userPrefs );

		// This creates the configuration editor specification properties.

		this.editSpec =
			new UserPrefs( "jCVSII.ConfigSpec", null );

		// This creates the server definitions properties.

		this.defServers =
			new UserPrefs( "jCVSII.DefaultServers", null );

		// This creates the server definitions properties.

		this.userServers =
			new UserPrefs( "jCVSII.UserServers", this.defServers );


		// Set the property prefix in all prefs.
		this.editSpec.setPropertyPrefix( "" );
		this.defPrefs.setPropertyPrefix( prefix );
		this.userPrefs.setPropertyPrefix( prefix );
		this.defServers.setPropertyPrefix( "" );
		this.userServers.setPropertyPrefix( "" );
		}

	public void
	checkCriticalProperties( Frame parent )
		{
		Vector need = new Vector();

		ResourceMgr rmgr = ResourceMgr.getInstance();

		String tempDir =
			this.userPrefs.getProperty( GLOBAL_TEMP_DIR, null );

		if ( tempDir == null )
			{
			need.addElement( GLOBAL_TEMP_DIR );
			}
		else
			{
			File tempDirF = new File( tempDir );

			if ( ! tempDirF.exists() )
				{
				String[] fmtArgs = { tempDirF.getPath() };
				String msg =
					rmgr.getUIFormat( "misc.tempdir.needs.config.msg", fmtArgs );
				String title =
					rmgr.getUIFormat( "misc.tempdir.needs.config.title", fmtArgs );
				JOptionPane.showMessageDialog
					( parent, msg, title, JOptionPane.WARNING_MESSAGE );

				need.addElement( GLOBAL_TEMP_DIR );
				}
			else if ( ! tempDirF.canWrite() )
				{
				String[] fmtArgs = { tempDirF.getPath() };
				String msg =
					rmgr.getUIFormat( "misc.tempdir.cannot.write.msg", fmtArgs );
				String title =
					rmgr.getUIFormat( "misc.tempdir.cannot.write.title", fmtArgs );
				JOptionPane.showMessageDialog
					( parent, msg, title, JOptionPane.WARNING_MESSAGE );

				need.addElement( GLOBAL_TEMP_DIR );
				}
			}

		if ( need.size() > 0 )
			{
			String[] editProps = new String[ need.size() ];
			need.copyInto( editProps );
			this.editConfiguration( parent, editProps );
			}

		tempDir = this.userPrefs.getProperty( GLOBAL_TEMP_DIR, "" );
		TempFileManager.initialize( tempDir, "jcvs", ".tmp" );
		}

	/**
	 * This method sets up properties based on the preferences. These
	 * properties are established at the very end of the configuration
	 * initialization process. It is also responsible for installing
	 * any <em>global</em> property change listeners we need.
	 */

	public void
	initializeGlobalProperties()
		{
		String format =
			this.userPrefs.getProperty
				( PROJECT_MODIFIED_FORMAT, "EEE MMM dd HH:mm:ss yyyy" );

		EntryNode.setTimestampFormat( format );

		this.loadServerDefinitions();

		this.loadExecCmdDefinitions();

		boolean debugSetting;

		debugSetting =
			this.userPrefs.getBoolean
				( GLOBAL_PROJECT_DEEP_DEBUG, false );

		CVSProject.deepDebug = debugSetting;

		debugSetting =
			this.userPrefs.getBoolean
				( GLOBAL_PROJECT_DEBUG_ENTRYIO, false );

		CVSProject.debugEntryIO = debugSetting;

		boolean traceAll =
			this.userPrefs.getBoolean( GLOBAL_CVS_TRACE_ALL, false );

		CVSProject.overTraceTCP = traceAll;
		CVSProject.overTraceRequest = traceAll;
		CVSProject.overTraceResponse = traceAll;
		CVSProject.overTraceProcessing = traceAll;

		// Subscribe to property changes.
		String[] subs =
			{
			GLOBAL_TEMP_DIR,
			GLOBAL_CVS_TRACE_ALL,
			GLOBAL_PROJECT_DEEP_DEBUG,
			GLOBAL_PROJECT_DEBUG_ENTRYIO,
			PROJECT_MODIFIED_FORMAT,
			PLAF_LOOK_AND_FEEL_CLASSNAME
			};

		for ( int i = 0 ; i < subs.length ; ++i )
			{
			this.userPrefs.addPropertyChangeListener( subs[i], this );
			}
		}

	public void
	propertyChange( PropertyChangeEvent evt )
		{
		String propName = evt.getPropertyName();

		if ( propName.equals( GLOBAL_CVS_TRACE_ALL ) )
			{
			boolean newSetting =
				this.userPrefs.getBoolean
					( GLOBAL_CVS_TRACE_ALL, false );

			CVSProject.overTraceTCP = newSetting;
			CVSProject.overTraceRequest = newSetting;
			CVSProject.overTraceResponse = newSetting;
			CVSProject.overTraceProcessing = newSetting;
			}
		else if ( propName.equals( GLOBAL_PROJECT_DEEP_DEBUG ) )
			{
			boolean newSetting =
				this.userPrefs.getBoolean
					( GLOBAL_PROJECT_DEEP_DEBUG, false );

			CVSProject.deepDebug = newSetting;
			}
		else if ( propName.equals( GLOBAL_PROJECT_DEBUG_ENTRYIO ) )
			{
			boolean newSetting =
				this.userPrefs.getBoolean
					( GLOBAL_PROJECT_DEBUG_ENTRYIO, false );

			CVSProject.debugEntryIO = newSetting;
			}
		else if ( propName.equals( GLOBAL_TEMP_DIR ) )
			{
			String tempDir =
				this.userPrefs.getProperty( GLOBAL_TEMP_DIR, "" );

			TempFileManager.clearTemporaryFiles();

			TempFileManager.initialize( tempDir, "jcvs", ".tmp" );
			}
		else if ( propName.equals( PROJECT_MODIFIED_FORMAT ) )
			{
			String format =
				this.userPrefs.getProperty
					( PROJECT_MODIFIED_FORMAT, "EEE MMM dd HH:mm:ss yyyy" );

			EntryNode.setTimestampFormat( format );
			}
		else if ( propName.equals( PLAF_LOOK_AND_FEEL_CLASSNAME ) )
			{
			String plafClassName =
				this.userPrefs.getProperty
					( Config.PLAF_LOOK_AND_FEEL_CLASSNAME, null );

			if ( plafClassName == null
					|| plafClassName.equals( "DEFAULT" ) )
				{
				plafClassName =
					UIManager.getSystemLookAndFeelClassName();
				}

			try { UIManager.setLookAndFeel( plafClassName ); }
				catch ( Exception ex ) { }

			MainFrame frm = JCVS.getMainFrame();
			SwingUtilities.updateComponentTreeUI( frm );

			Enumeration enum = ProjectFrameMgr.enumerateProjectFrames();
			for ( ; enum.hasMoreElements() ; )
				{
				SwingUtilities.updateComponentTreeUI
					( (ProjectFrame) enum.nextElement() );
				}
			}
		}

	public Vector
	getServerDefinitions()
		{
		return this.servers;
		}

	public String
	getExecCommandKey( String verb, String extension )
		{
		return extension + "." + verb;
		}

	public String
	getExecCommandArgs( String verb, String extension )
		{
		String result = null;

		String key = this.getExecCommandKey( verb, extension );

		PrefsTuple tup = this.execCmdTable.getTuple( key );

		if ( tup != null )
			{
			result = tup.getValueAt( EXEC_DEF_CMD_IDX );
			}
		
		return result;
		}

	public String
	getExecCommandEnv( String verb, String extension )
		{
		String result = null;

		String key = this.getExecCommandKey( verb, extension );

		PrefsTuple tup = this.execCmdTable.getTuple( key );

		if ( tup != null )
			{
			result = tup.getValueAt( EXEC_DEF_ENV_IDX );
			}
		
		return result;
		}

	public PrefsTupleTable
	getExecCmdDefinitions()
		{
		return this.execCmdTable;
		}

	public void
	loadExecCmdDefinitions()
		{
		this.execCmdTable =
			this.userPrefs.getTupleTable
				( GLOBAL_EXT_VERB_TABLE, null );

		if ( this.execCmdTable == null )
			{
			this.execCmdTable = new PrefsTupleTable();
			}
		}

	public void
	loadServerDefinitions()
		{
		this.servers = new Vector();

		this.enumerateServerDefinitions( this.defServers.keys() );

		this.enumerateServerDefinitions( this.userServers.keys() );
		}

	/**
	 * This is used to sort the list of hosts presented in the
	 * dialog, although it is a general purpose utility.
	 * Insertionsort - O(n^2) but it is short.
	 *
	 * @author Urban Widmark <urban@svenskatest.se>
	 */
	private void
	sortServerVector( Vector v )
		{
	    for ( int i = 1 ; i < v.size() ; ++i )
			{
			ServerDef B = (ServerDef) v.elementAt( i );

			int j = i;
			for ( ; j > 0 ; --j )
				{
				ServerDef A = (ServerDef) v.elementAt( j - 1 );
				if ( A.compareTo( B ) <= 0 )
					break;
				v.setElementAt( A, j );
				}

			v.setElementAt( B, j );
			}
		}

	public void
	enumerateServerDefinitions( Enumeration enum )
		{
		for ( ; enum.hasMoreElements() ; )
			{
			String key = (String) enum.nextElement();

			if ( ! key.startsWith( "server." ) )
				continue;

			if ( ! this.userServers.getBoolean( key, false ) )
				continue;

			String token = key.substring( "server.".length() );

			String method =
				this.userServers.getProperty
					( "param." + token + ".method", "pserver" );

			String name =
				this.userServers.getProperty( "param." + token + ".name", null );
			String module =
				this.userServers.getProperty( "param." + token + ".module", "" );
			String host =
				this.userServers.getProperty( "param." + token + ".host", "" );
			String user =
				this.userServers.getProperty( "param." + token + ".user", "" );
			String repos =
				this.userServers.getProperty( "param." + token + ".repos", "" );
			String desc =
				this.userServers.getProperty( "param." + token + ".desc", "" );

			if ( name != null )
				this.servers.addElement
					( new ServerDef
						( name, method, module, user, host, repos, desc ) );
			// UNDONE report missing name!
			}

		// Sort the servers so they display nicely.
		// @author Urban Widmark <urban@svenskatest.se>
		sortServerVector( this.servers );
		}

	public void
	loadProjectPreferences( CVSProject project, UserPrefs prefs )
		{
		String propFilename = this.getUserPrefsFilename();

		String prefsPath =
			CVSCUtilities.exportPath(
				CVSProject.getAdminPrefsPath
					( CVSProject.rootPathToAdminPath
						( project.getLocalRootPath() ) ) );

		File prefsF = new File( prefsPath );

		try {
			UserPrefsFileLoader loader = (UserPrefsFileLoader)
				UserPrefsLoader.getLoader( UserPrefsLoader.FILE_LOADER );

			loader.setFile( prefsF );
			loader.loadPreferences( prefs );

			if ( this.debug )
			System.err.println
				( "Loaded project preferences from '"
					+ prefsF.getPath() + "'" );
			}
		catch ( IOException ex )
			{
			if ( this.debug )
			System.err.println
				( "No project preferences found at '"
					+ prefsF.getPath() + "'" );
			}
		}

	public void
	saveProjectPreferences( CVSProject project, UserPrefs prefs )
		{
		String propFilename = this.getUserPrefsFilename();

		String prefsPath =
			CVSCUtilities.exportPath(
				CVSProject.getAdminPrefsPath
					( CVSProject.rootPathToAdminPath
						( project.getLocalRootPath() ) ) );

		File prefsF = new File( prefsPath );

		try {
			UserPrefsFileLoader loader = (UserPrefsFileLoader)
				UserPrefsLoader.getLoader( UserPrefsLoader.FILE_LOADER );

			loader.setFile( prefsF );
			loader.storePreferences( prefs );

			if ( this.debug )
			System.err.println
				( "Saved project preferences into '"
					+ prefsF.getPath() + "'" );
			}
		catch ( IOException ex )
			{
			System.err.println
				( "Failed storing project preferences into '"
					+ prefsF.getPath() + "', " + ex.getMessage() );
			}
		}

	public void
	loadUserPreferences()
		{
		String propFilename = this.getUserPrefsFilename();

		File prefsF =
			new File( this.userPrefs.getUserHome(), propFilename );

		if ( ! prefsF.exists() )
			{
			System.err.println
				( "No user preferences found at '"
					+ prefsF.getPath() + "'" );
			return;
			}

		if ( ! prefsF.canRead() )
			{
			System.err.println
				( "ERROR Can not read user preferences at '"
					+ prefsF.getPath() + "'" );
			return;
			}

		try {
			UserPrefsStreamLoader loader = (UserPrefsStreamLoader)
				UserPrefsLoader.getLoader( UserPrefsConstants.STREAM_LOADER );

			InputStream in = new FileInputStream( prefsF );

			loader.setInputStream( in );
			loader.loadPreferences( this.userPrefs );
			in.close();

			System.err.println
				( "Loaded user preferences from '"
					+ prefsF.getPath() + "'" );
			}
		catch ( IOException ex )
			{
			ex.printStackTrace();
			}
		}

	public void
	loadConfigEditorSpecification()
		{
		String specURL = "/com/ice/jcvsii/configspec.properties";

		InputStream in = null;

		try {
			in = ResourceUtilities.openNamedResource( specURL );

			UserPrefsStreamLoader loader = (UserPrefsStreamLoader)
				UserPrefsLoader.getLoader( UserPrefsConstants.STREAM_LOADER );

			loader.setInputStream( in );
			loader.loadPreferences( this.editSpec );

			System.err.println
				( "Loaded config editor specification from '"
					+ specURL + "'" );
			}
		catch ( IOException ex )
			{
			System.err.println
				( "ERROR loading editor specification from '"
					+ specURL + "'\n      " + ex.getMessage() );
			}
		finally
			{
			if ( in != null )
				{
				try { in.close(); }
				catch ( IOException ex ) { }
				}
			}
		}

	public void
	loadDefaultPreferences()
		{
		String defURL = "/com/ice/jcvsii/defaults.properties";

		UserPrefsStreamLoader loader = (UserPrefsStreamLoader)
			UserPrefsLoader.getLoader( UserPrefsConstants.STREAM_LOADER );

		InputStream in = null;

		try {
			in = ResourceUtilities.openNamedResource( defURL );

			loader.setInputStream( in );
			loader.loadPreferences( this.defPrefs );

			System.err.println
				( "Loaded default preferences from '" + defURL + "'" );
			}
		catch ( IOException ex )
			{
			System.err.println
				( "ERROR loading default preferences from '"
					+ defURL + "'\n      " + ex.getMessage() );
			}
		finally
			{
			if ( in != null )
				{
				try { in.close(); }
				catch ( IOException ex ) { }
				}
			}
		}

	public void
	loadDefaultServerDefinitions()
		{
		String defURL = "/com/ice/jcvsii/servers.properties";

		UserPrefsStreamLoader loader = (UserPrefsStreamLoader)
			UserPrefsLoader.getLoader( UserPrefsConstants.STREAM_LOADER );

		InputStream in = null;

		try {
			in = ResourceUtilities.openNamedResource( defURL );

			loader.setInputStream( in );
			loader.loadPreferences( this.defServers );

			System.err.println
				( "Loaded default server definitions from '" + defURL + "'" );
			}
		catch ( IOException ex )
			{
			System.err.println
				( "ERROR loading default server definitions from '"
					+ defURL + "'\n      " + ex.getMessage() );
			}
		finally
			{
			if ( in != null )
				{
				try { in.close(); }
				catch ( IOException ex ) { }
				}
			}
		}

	public void
	loadUserServerDefinitions()
		{
		String propFilename = this.getUserServersFilename();

		File prefsF =
			new File( this.userPrefs.getUserHome(), propFilename );

		try {
			InputStream in = new FileInputStream( prefsF );

			UserPrefsStreamLoader loader = (UserPrefsStreamLoader)
				UserPrefsLoader.getLoader( UserPrefsConstants.STREAM_LOADER );

			loader.setInputStream( in );
			loader.loadPreferences( this.userServers );
			in.close();

			System.err.println
				( "Loaded user server definitions from '"
					+ prefsF.getPath() + "'" );
			}
		catch ( IOException ex )
			{
			System.err.println
				( "No user server definitions found at '"
					+ prefsF.getPath() + "'" );
			}
		}

	public void
	savePreferences()
		{
		String propFilename = this.getUserPrefsFilename();

		File prefsF =
			new File( this.userPrefs.getUserHome(), propFilename );

		try {
			UserPrefsFileLoader loader = (UserPrefsFileLoader)
				UserPrefsLoader.getLoader( UserPrefsLoader.FILE_LOADER );

			loader.setFile( prefsF );

			loader.storePreferences( this.userPrefs );

			System.err.println
				( "Stored user preferences to '" + prefsF.getPath() + "'" );
			}
		catch ( IOException ex )
			{
			System.err.println
				( "ERROR storing user preferences to '"
					+ prefsF.getPath() + "'\n      " + ex.getMessage() );
			}
		}

	public void
	loadMailCap()
		{
	    File capF;
		InputStream in = null;
		String where = "";

		if ( this.mailcapFileName != null )
			{
			capF = new File( this.mailcapFileName );
			}
		else
			{
			// REVIEW We are using a questionable algorithm here.
			String defMailcapFilename =
				this.getDefaultMailcapFilename();

			String mailcapFileName =
				this.userPrefs.getProperty
					( GLOBAL_MAILCAP_FILE, defMailcapFilename );

			capF = new File
				( this.userPrefs.getUserHome(), mailcapFileName );
			}

		try {
			if ( capF.exists() && capF.isFile() && capF.canRead() )
				{
				where = capF.getPath();
				in = new FileInputStream( capF );
				}
			else
				{
				where = DEFAULT_MAILCAP_FILENAME;
				in = ResourceUtilities.openNamedResource( where );
				}

			if ( in != null )
				{
				System.err.println
					( "Loading mailcap from '" + where + "'" );
				CommandMap.setDefaultCommandMap
					( new MailcapCommandMap( in ) );
				System.err.println
					( "Loaded mailcap from '" + where + "'" );
				}
			}
		catch ( IOException ex )
			{
			CommandMap.setDefaultCommandMap
				( new MailcapCommandMap() );
			System.err.println
				( "Using default mailcap definition." );
			}
		finally
			{
			if ( in != null )
				{
				try { in.close(); }
				catch ( IOException ex ) {}
				}
			}
		}

	public void
	loadMimeTypes()
		{
	    File mimeF;
		InputStream in = null;
		String where = "";

		if ( this.mimeFileName != null )
			{
			mimeF = new File( this.mimeFileName );
			}
		else
			{
			// REVIEW We are using a questionable algorithm here.
			String defMimeFilename =
				this.getDefaultMimetypesFilename();

			String mimeFileName =
				this.userPrefs.getProperty
					( GLOBAL_MIMETYPES_FILE, defMimeFilename );

			mimeF =
				new File( this.userPrefs.getUserHome(), mimeFileName );
			}

		try {
			if ( mimeF.exists() && mimeF.isFile() && mimeF.canRead() )
				{
				where = mimeF.getPath();
				in = new FileInputStream( mimeF );
				}
			else
				{
				where = DEFAULT_MIMETYPES_FILENAME;
				in = ResourceUtilities.openNamedResource( where );
				}

			if ( in != null )
				{
				FileTypeMap.setDefaultFileTypeMap
					( new MimetypesFileTypeMap( in ) );
				System.err.println
					( "Loaded mime types from '" + where + "'" );
				}
			}
		catch ( IOException ex )
			{
			FileTypeMap.setDefaultFileTypeMap
				( new MimetypesFileTypeMap() );
			System.err.println
				( "Using default mime types definition." );
			}
		finally
			{
			if ( in != null )
				{
				try { in.close(); }
				catch ( IOException ex ) {}
				}
			}
		}

	public void
	editConfiguration( Frame parent )
		{
		this.editConfiguration( parent, null );
		}

	public void
	editConfiguration( Frame parent, String[] editProps )
		{
		ConfigDialog dlg = new ConfigDialog
			( parent, "jCVS II", this.userPrefs, this.editSpec );

		dlg.setSize( new Dimension( 500, 440 ) );

		Point location = AWTUtilities.computeDialogLocation( dlg );

		dlg.setLocation( location.x, location.y );

		if ( editProps != null && editProps.length > 0 )
			{
			dlg.editProperties( editProps );
			}

		dlg.setVisible( true );

		if ( dlg.getOKClicked() )
			{
			this.savePreferences();
			}
		}

	}

