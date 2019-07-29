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

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Vector;

import javax.activation.CommandMap;
import javax.activation.FileTypeMap;
import javax.activation.MailcapCommandMap;
import javax.activation.MimetypesFileTypeMap;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.ice.cvsc.CVSCUtilities;
import com.ice.cvsc.CVSProject;
import com.ice.pref.PrefsTuple;
import com.ice.pref.PrefsTupleTable;
import com.ice.pref.UserPrefs;
import com.ice.pref.UserPrefsConstants;
import com.ice.pref.UserPrefsFileLoader;
import com.ice.pref.UserPrefsLoader;
import com.ice.pref.UserPrefsStreamLoader;
import com.ice.util.AWTUtilities;
import com.ice.util.ResourceUtilities;
import com.ice.util.TempFileManager;


/**
 * The Configuration class.
 *
 * @version $Revision: 1.7 $
 * @author Timothy Gerard Endres,
 *  <a href="mailto:time@ice.com">time@ice.com</a>.
 */

public
class		Config
implements	ConfigConstants, PropertyChangeListener
	{
	static public final String		RCS_ID = "$Id: Config.java,v 1.7 2003/07/27 04:39:06 time Exp $";
	static public final String		RCS_REV = "$Revision: 1.7 $";


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
	private final String					mimeFileName = null;
	private final String					mailcapFileName = null;

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

	private boolean					isMacintoshF = false;
	private boolean					isOS2F = false;
	private boolean					isWindowsF = false;


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

	public boolean
	isMacintosh()
		{
		return this.isMacintoshF;
		}

	public boolean
	isWindows()
		{
		return this.isWindowsF;
		}

	public void
	setDebug( final boolean debug )
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
			this.isMacintoshF = true;
			}
		else if ( osName.startsWith( "windows" ) )
			{
			System.err.println( "Assuming a Windows platform." );
			this.userPrefsFilename = "jcvsii.txt";
			this.userServersFilename = "jcvsdef.txt";
			this.defMailcapFilename = "jcvsmailcap.txt";
			this.defMimetypesFilename = "jcvsmime.txt";
			this.lastDitchTempDirname = ".";
			this.isWindowsF = true;
			}
		else if ( osName.startsWith( "os/2" ) )
			{
			System.err.println( "Assuming an OS/2 platform." );
			this.userPrefsFilename = "jcvsii.txt";
			this.userServersFilename = "jcvsdef.txt";
			this.defMailcapFilename = "jcvsmailcap.txt";
			this.defMimetypesFilename = "jcvsmime.txt";
			this.lastDitchTempDirname = ".";
			this.isOS2F = true;
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
	initializePreferences( final String prefix )
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

		final MailcapCommandMap cMap = new MailcapCommandMap();
		final MimetypesFileTypeMap fMap = new MimetypesFileTypeMap();

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
	checkCriticalProperties( final Frame parent )
		{
		final Vector need = new Vector();

		final ResourceMgr rmgr = ResourceMgr.getInstance();

		String tempDir =
			this.userPrefs.getProperty( GLOBAL_TEMP_DIR, null );

		if ( tempDir == null )
			{
			need.addElement( GLOBAL_TEMP_DIR );
			}
		else
			{
			final File tempDirF = new File( tempDir );

			if ( ! tempDirF.exists() )
				{
				final String[] fmtArgs = { tempDirF.getPath() };
				final String msg =
					rmgr.getUIFormat( "misc.tempdir.needs.config.msg", fmtArgs );
				final String title =
					rmgr.getUIFormat( "misc.tempdir.needs.config.title", fmtArgs );
				JOptionPane.showMessageDialog
					( parent, msg, title, JOptionPane.WARNING_MESSAGE );

				need.addElement( GLOBAL_TEMP_DIR );
				}
			else if ( ! tempDirF.canWrite() )
				{
				final String[] fmtArgs = { tempDirF.getPath() };
				final String msg =
					rmgr.getUIFormat( "misc.tempdir.cannot.write.msg", fmtArgs );
				final String title =
					rmgr.getUIFormat( "misc.tempdir.cannot.write.title", fmtArgs );
				JOptionPane.showMessageDialog
					( parent, msg, title, JOptionPane.WARNING_MESSAGE );

				need.addElement( GLOBAL_TEMP_DIR );
				}
			}

		if ( need.size() > 0 )
			{
			final String[] editProps = new String[ need.size() ];
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
		final String format =
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

		final boolean traceAll =
			this.userPrefs.getBoolean( GLOBAL_CVS_TRACE_ALL, false );

		CVSProject.overTraceTCP = traceAll;
		CVSProject.overTraceRequest = traceAll;
		CVSProject.overTraceResponse = traceAll;
		CVSProject.overTraceProcessing = traceAll;

		// Subscribe to property changes.
		final String[] subs =
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
	propertyChange( final PropertyChangeEvent evt )
		{
		final String propName = evt.getPropertyName();

		if ( propName.equals( GLOBAL_CVS_TRACE_ALL ) )
			{
			final boolean newSetting =
				this.userPrefs.getBoolean
					( GLOBAL_CVS_TRACE_ALL, false );

			CVSProject.overTraceTCP = newSetting;
			CVSProject.overTraceRequest = newSetting;
			CVSProject.overTraceResponse = newSetting;
			CVSProject.overTraceProcessing = newSetting;
			}
		else if ( propName.equals( GLOBAL_PROJECT_DEEP_DEBUG ) )
			{
			final boolean newSetting =
				this.userPrefs.getBoolean
					( GLOBAL_PROJECT_DEEP_DEBUG, false );

			CVSProject.deepDebug = newSetting;
			}
		else if ( propName.equals( GLOBAL_PROJECT_DEBUG_ENTRYIO ) )
			{
			final boolean newSetting =
				this.userPrefs.getBoolean
					( GLOBAL_PROJECT_DEBUG_ENTRYIO, false );

			CVSProject.debugEntryIO = newSetting;
			}
		else if ( propName.equals( GLOBAL_TEMP_DIR ) )
			{
			final String tempDir =
				this.userPrefs.getProperty( GLOBAL_TEMP_DIR, "" );

			TempFileManager.clearTemporaryFiles();

			TempFileManager.initialize( tempDir, "jcvs", ".tmp" );
			}
		else if ( propName.equals( PROJECT_MODIFIED_FORMAT ) )
			{
			final String format =
				this.userPrefs.getProperty
					( PROJECT_MODIFIED_FORMAT, "EEE MMM dd HH:mm:ss yyyy" );

			EntryNode.setTimestampFormat( format );
			}
		else if ( propName.equals( PLAF_LOOK_AND_FEEL_CLASSNAME ) )
			{
			String plafClassName =
				this.userPrefs.getProperty
					( ConfigConstants.PLAF_LOOK_AND_FEEL_CLASSNAME, null );

			if ( plafClassName == null
					|| plafClassName.equals( "DEFAULT" ) )
				{
				plafClassName =
					UIManager.getSystemLookAndFeelClassName();
				}

			try { UIManager.setLookAndFeel( plafClassName ); }
				catch ( final Exception ex ) { }

			final MainFrame frm = JCVS.getMainFrame();
			SwingUtilities.updateComponentTreeUI( frm );

			final Enumeration enumeration = ProjectFrameMgr.enumerateProjectFrames();
			for ( ; enumeration.hasMoreElements() ; )
				{
				SwingUtilities.updateComponentTreeUI
					( (ProjectFrame) enumeration.nextElement() );
				}
			}
		}

	public Vector
	getServerDefinitions()
		{
		return this.servers;
		}

	public String
	getExecCommandKey( final String verb, final String extension )
		{
		return extension + "." + verb;
		}

	public String
	getExecCommandArgs( final String verb, final String extension )
		{
		String result = null;

		final String key = this.getExecCommandKey( verb, extension );

		final PrefsTuple tup = this.execCmdTable.getTuple( key );

		if ( tup != null )
			{
			result = tup.getValueAt( EXEC_DEF_CMD_IDX );
			}

		return result;
		}

	public String
	getExecCommandEnv( final String verb, final String extension )
		{
		String result = null;

		final String key = this.getExecCommandKey( verb, extension );

		final PrefsTuple tup = this.execCmdTable.getTuple( key );

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
	sortServerVector( final Vector v )
		{
	    for ( int i = 1 ; i < v.size() ; ++i )
			{
			final ServerDef B = (ServerDef) v.elementAt( i );

			int j = i;
			for ( ; j > 0 ; --j )
				{
				final ServerDef A = (ServerDef) v.elementAt( j - 1 );
				if ( A.compareTo( B ) <= 0 )
					break;
				v.setElementAt( A, j );
				}

			v.setElementAt( B, j );
			}
		}

	public void
	enumerateServerDefinitions( final Enumeration enumeration )
		{
		for ( ; enumeration.hasMoreElements() ; )
			{
			final String key = (String) enumeration.nextElement();

			if ( ! key.startsWith( "server." ) )
				continue;

			if ( ! this.userServers.getBoolean( key, false ) )
				continue;

			final String token = key.substring( "server.".length() );

			final String method =
				this.userServers.getProperty
					( "param." + token + ".method", "pserver" );

			final String name =
				this.userServers.getProperty( "param." + token + ".name", null );
			final String module =
				this.userServers.getProperty( "param." + token + ".module", "" );
			final String host =
				this.userServers.getProperty( "param." + token + ".host", "" );
			final String user =
				this.userServers.getProperty( "param." + token + ".user", "" );
			final String repos =
				this.userServers.getProperty( "param." + token + ".repos", "" );
			final String desc =
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
	loadProjectPreferences( final CVSProject project, final UserPrefs prefs )
		{
		final String propFilename = this.getUserPrefsFilename();

		final String prefsPath =
			CVSCUtilities.exportPath(
				CVSProject.getAdminPrefsPath
					( CVSProject.rootPathToAdminPath
						( project.getLocalRootPath() ) ) );

		final File prefsF = new File( prefsPath );

		try {
			final UserPrefsFileLoader loader = (UserPrefsFileLoader)
				UserPrefsLoader.getLoader( UserPrefsConstants.FILE_LOADER );

			loader.setFile( prefsF );
			loader.loadPreferences( prefs );

			if ( this.debug )
			System.err.println
				( "Loaded project preferences from '"
					+ prefsF.getPath() + "'" );
			}
		catch ( final IOException ex )
			{
			if ( this.debug )
			System.err.println
				( "No project preferences found at '"
					+ prefsF.getPath() + "'" );
			}
		}

	public void
	saveProjectPreferences( final CVSProject project, final UserPrefs prefs )
		{
		final String propFilename = this.getUserPrefsFilename();

		final String prefsPath =
			CVSCUtilities.exportPath(
				CVSProject.getAdminPrefsPath
					( CVSProject.rootPathToAdminPath
						( project.getLocalRootPath() ) ) );

		final File prefsF = new File( prefsPath );

		try {
			final UserPrefsFileLoader loader = (UserPrefsFileLoader)
				UserPrefsLoader.getLoader( UserPrefsConstants.FILE_LOADER );

			loader.setFile( prefsF );
			loader.storePreferences( prefs );

			if ( this.debug )
			System.err.println
				( "Saved project preferences into '"
					+ prefsF.getPath() + "'" );
			}
		catch ( final IOException ex )
			{
			System.err.println
				( "Failed storing project preferences into '"
					+ prefsF.getPath() + "', " + ex.getMessage() );
			}
		}

	public void
	loadUserPreferences()
		{
		final String propFilename = this.getUserPrefsFilename();

		final File prefsF =
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
			final UserPrefsStreamLoader loader = (UserPrefsStreamLoader)
				UserPrefsLoader.getLoader( UserPrefsConstants.STREAM_LOADER );

			final InputStream in = new FileInputStream( prefsF );

			loader.setInputStream( in );
			loader.loadPreferences( this.userPrefs );
			in.close();

			System.err.println
				( "Loaded user preferences from '"
					+ prefsF.getPath() + "'" );
			}
		catch ( final IOException ex )
			{
			ex.printStackTrace();
			}
		}

	public void
	loadConfigEditorSpecification()
		{
		final String specURL = "/com/ice/jcvsii/configspec.properties";

		InputStream in = null;

		try {
			in = ResourceUtilities.openNamedResource( specURL );

			final UserPrefsStreamLoader loader = (UserPrefsStreamLoader)
				UserPrefsLoader.getLoader( UserPrefsConstants.STREAM_LOADER );

			loader.setInputStream( in );
			loader.loadPreferences( this.editSpec );

			System.err.println
				( "Loaded config editor specification from '"
					+ specURL + "'" );
			}
		catch ( final IOException ex )
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
				catch ( final IOException ex ) { }
				}
			}
		}

	public void
	loadDefaultPreferences()
		{
		final String defURL = "/com/ice/jcvsii/defaults.properties";

		final UserPrefsStreamLoader loader = (UserPrefsStreamLoader)
			UserPrefsLoader.getLoader( UserPrefsConstants.STREAM_LOADER );

		InputStream in = null;

		try {
			in = ResourceUtilities.openNamedResource( defURL );

			loader.setInputStream( in );
			loader.loadPreferences( this.defPrefs );

			System.err.println
				( "Loaded default preferences from '" + defURL + "'" );
			}
		catch ( final IOException ex )
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
				catch ( final IOException ex ) { }
				}
			}
		}

	public void
	loadDefaultServerDefinitions()
		{
		final String defURL = "/com/ice/jcvsii/servers.properties";

		final UserPrefsStreamLoader loader = (UserPrefsStreamLoader)
			UserPrefsLoader.getLoader( UserPrefsConstants.STREAM_LOADER );

		InputStream in = null;

		try {
			in = ResourceUtilities.openNamedResource( defURL );

			loader.setInputStream( in );
			loader.loadPreferences( this.defServers );

			System.err.println
				( "Loaded default server definitions from '" + defURL + "'" );
			}
		catch ( final IOException ex )
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
				catch ( final IOException ex ) { }
				}
			}
		}

	public void
	loadUserServerDefinitions()
		{
		final String propFilename = this.getUserServersFilename();

		final File prefsF =
			new File( this.userPrefs.getUserHome(), propFilename );

		try {
			final InputStream in = new FileInputStream( prefsF );

			final UserPrefsStreamLoader loader = (UserPrefsStreamLoader)
				UserPrefsLoader.getLoader( UserPrefsConstants.STREAM_LOADER );

			loader.setInputStream( in );
			loader.loadPreferences( this.userServers );
			in.close();

			System.err.println
				( "Loaded user server definitions from '"
					+ prefsF.getPath() + "'" );
			}
		catch ( final IOException ex )
			{
			System.err.println
				( "No user server definitions found at '"
					+ prefsF.getPath() + "'" );
			}
		}

	public void
	savePreferences()
		{
		final String propFilename = this.getUserPrefsFilename();

		final File prefsF =
			new File( this.userPrefs.getUserHome(), propFilename );

		try {
			final UserPrefsFileLoader loader = (UserPrefsFileLoader)
				UserPrefsLoader.getLoader( UserPrefsConstants.FILE_LOADER );

			loader.setFile( prefsF );

			loader.storePreferences( this.userPrefs );

			System.err.println
				( "Stored user preferences to '" + prefsF.getPath() + "'" );
			}
		catch ( final IOException ex )
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
			final String defMailcapFilename =
				this.getDefaultMailcapFilename();

			final String mailcapFileName =
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
		catch ( final IOException ex )
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
				catch ( final IOException ex ) {}
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
			final String defMimeFilename =
				this.getDefaultMimetypesFilename();

			final String mimeFileName =
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
		catch ( final IOException ex )
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
				catch ( final IOException ex ) {}
				}
			}
		}

	public void
	editConfiguration( final Frame parent )
		{
		this.editConfiguration( parent, null );
		}

	public void
	editConfiguration( final Frame parent, final String[] editProps )
		{
		final ConfigDialog dlg = new ConfigDialog
			( parent, "jCVS II", this.userPrefs, this.editSpec );

		dlg.setSize( new Dimension( 500, 440 ) );

		final Point location = AWTUtilities.computeDialogLocation( dlg );

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

