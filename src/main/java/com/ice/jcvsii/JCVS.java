/*
** Java CVS client application package.
** Copyright (c) 1997-2003 by Timothy Gerard Endres, <time@jcvs.org>
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

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.UIManager;

import com.ice.cvsc.CVSLog;
import com.ice.cvsc.CVSTimestampFormat;
import com.ice.pref.UserPrefs;


/**
 * The jCVS application class.
 *
 * @version $Revision: 1.9 $
 * @author Timothy Gerard Endres, <a href="mailto:time@ice.com">time@ice.com</a>.
 */

public
class		JCVS
	{
	static public final String		RCS_ID = "$Id: JCVS.java,v 1.9 2003/07/27 04:50:28 time Exp $";
	static public final String		RCS_REV = "$Revision: 1.9 $";

	static public final String		VERSION_STR = "5.4.2";

	static private JCVS		instance;

	private MainFrame		mainFrame;


	static public void
	main( final String[] argv )
		{
		final JCVS app = new JCVS();
		JCVS.instance = app;
		app.instanceMain( argv );
		}

	static public String
	getVersionString()
		{
		return VERSION_STR;
		}

	static public MainFrame
	getMainFrame()
		{
		return JCVS.instance.mainFrame;
		}

	private void
	instanceMain( final String[] argv )
		{
		this.processArguments( argv );

		// NOTE
		// The new j2ssh package uses the commons-logging package from Apache.
		// This sucks, because now we get a bunch of logging to stderr, and I
		// do not currently have a way to put it into the jCVS log. I suspect
		// that I should upgrade jCVS to JDK1.4, and use java.util.logging for
		// all jCVS logging. But that is another issue. For now, we set the
		// level to ERROR.
		//
		final LogManager logMgr = LogManager.getLogManager();
		final Enumeration loggers = logMgr.getLoggerNames();
		for ( ; loggers.hasMoreElements() ; )
			{
			final String nm = (String) loggers.nextElement();
			final Logger l = logMgr.getLogger( nm );
			l.setLevel( Level.WARNING  );
			}

		final DefaultBoundedRangeModel model =
			new DefaultBoundedRangeModel( 0, 0, 0, 100 );

		final JCVSSplash splash = new JCVSSplash( "jCVS II", model );

		splash.show();

		// NOTE This focus request must be here after
		//      showing the parent to get keystrokes properly.
		splash.requestFocus();

		this.new Initiator( splash, model ).start();
		}

	public void
	performShutDown()
		{
		ProjectFrameMgr.closeAllProjects();

		this.mainFrame.savePreferences();

		Config.getInstance().savePreferences();

		System.exit( 0 );
		}

	public void
	actionPerformed( final ActionEvent event )
		{
		final String command = event.getActionCommand();

		if ( command.equals( "QUIT" ) )
			{
			this.performShutDown();
			}
		}

	private void
	processArguments( final String[] argv )
		{
		final UserPrefs prefs = Config.getPreferences();

		for ( int i = 0 ; i < argv.length ; ++i )
			{
			if ( argv[i].equals( "-osname" ) )
				{
				prefs.setOSSuffix( argv[++i] );
				}
			else if ( argv[i].equals( "-user" ) )
				{
				prefs.setUserSuffix( argv[++i] );
				}
			else if ( argv[i].equals( "-home" ) )
				{
				prefs.setUserHome( argv[++i] );
				}
			else
				{
				System.err.println
					( "   argv[" +i+ "] '" +argv[i]+ "' ignored." );
				}
			}
		}

	private
	class		Initiator
	extends		Thread
		{
		JCVSSplash splash;
		BoundedRangeModel model;

		public
		Initiator( final JCVSSplash s, final BoundedRangeModel m )
			{
			super( "Model" );
			this.splash = s;
			this.model = m;
			}

		public void
		run()
			{
			// This sleep is used to give the repaint thread time
			// to properly refresh the Splash window before we race
			// along and finish initializing before the progress bar
			// can even begin to track out operation.
			//
			try { Thread.sleep( 100 ); }
				catch ( final InterruptedException ex ) { }

			int proval = 0;
			final int procnt = 13;
			final int proincr = this.model.getMaximum() / procnt;

			this.model.setValue( proval += proincr );

			final Config cfg = Config.getInstance();

			cfg.initializePreferences( "jcvsii." );

			final UserPrefs prefs = Config.getPreferences();

			this.model.setValue( proval += proincr );

			cfg.loadDefaultPreferences();

			this.model.setValue( proval += proincr );

			cfg.loadUserPreferences();

			this.model.setValue( proval += proincr );

			//
			// NOTE
			// Resources should be loaded after the user preferences, as
			// their initialization may depend on something that the user
			// has configured. On the other hand, the should be loaded
			// before any other configuration initialization, which in
			// turn may be dependent on the resources!
			//

			ResourceMgr.initializeResourceManager( "jcvsii" );

			this.model.setValue( proval += proincr );

			cfg.loadConfigEditorSpecification();

			this.model.setValue( proval += proincr );

			if ( prefs.getBoolean( ConfigConstants.GLOBAL_LOAD_SERVERS, false ) )
				{
				cfg.loadDefaultServerDefinitions();
				}

			this.model.setValue( proval += proincr );

			cfg.loadMimeTypes();

			this.model.setValue( proval += proincr );

			cfg.loadMailCap();

			this.model.setValue( proval += proincr );

			cfg.loadUserServerDefinitions();

			this.model.setValue( proval += proincr );

			cfg.initializeGlobalProperties();

			this.model.setValue( proval += proincr );

			// NOTE Make sure that there is no CVSLog-ing before this point!
			CVSLog.setLogFilename
				( prefs.getProperty
					( ConfigConstants.GLOBAL_CVS_LOG_FILE,
						CVSLog.DEFAULT_FILENAME ) );

			this.model.setValue( proval += proincr );

			CVSLog.checkLogOpen();

			CVSLog.logMsgStderr( "jCVS II Version " + VERSION_STR );
			CVSLog.logMsgStderr
				( "Licensed under the GNU General Public License." );
			CVSLog.logMsgStderr
				( "License is available at <http://www.gjt.org/doc/gpl/>" );

			CVSLog.logMsgStderr
				( "Property 'os.name' = '" + prefs.getOSSuffix() + "'" );
			CVSLog.logMsgStderr
				( "Property 'user.name' = '" + prefs.getUserSuffix() + "'" );
			CVSLog.logMsgStderr
				( "Property 'user.home' = '" + prefs.getUserHome() + "'" );
			CVSLog.logMsgStderr
				( "Property 'user.dir' = '" + prefs.getCurrentDirectory() + "'" );

			// Establish the CVSTimestamp Formatting Timezone
			final String tzPropStr =
				prefs.getProperty( ConfigConstants.GLOBAL_CVS_TIMEZONE, null );

			if ( tzPropStr != null )
				{
				CVSTimestampFormat.setTimeZoneID( tzPropStr );
				CVSLog.logMsgStderr
					( "CVS Timestamp timezone set to '" + tzPropStr + "'" );
				}

			this.model.setValue( proval += proincr );

			String plafClassName =
				prefs.getProperty
					( ConfigConstants.PLAF_LOOK_AND_FEEL_CLASSNAME, null );

			if ( plafClassName == null
					|| plafClassName.equals( "DEFAULT" ) )
				{
				plafClassName =
					UIManager.getSystemLookAndFeelClassName();
				}

			try { UIManager.setLookAndFeel( plafClassName ); }
				catch ( final Exception ex ) { }

			final Rectangle bounds =
				prefs.getBounds
					( ConfigConstants.MAIN_WINDOW_BOUNDS,
						new Rectangle( 20, 20, 540, 360 ) );

			mainFrame = new MainFrame( JCVS.this, "jCVS II", bounds );

			this.model.setValue( this.model.getMaximum() );

			try { Thread.sleep( 500 ); }
				catch ( final InterruptedException ex ) {}

			this.splash.dispose();

			mainFrame.loadPreferences();

			mainFrame.show();

			mainFrame.repaint( 100 );

			cfg.checkCriticalProperties( mainFrame );
			}
		}
	}

