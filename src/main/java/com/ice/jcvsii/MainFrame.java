/*
** Copyright (c) 1998 by Timothy Gerard Endres
** <mailto:time@ice.com>  <http://www.ice.com>
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

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import javax.help.*;
import javax.swing.*;
import javax.swing.border.*;

import com.ice.cvsc.CVSLog;
import com.ice.cvsc.CVSProject;
import com.ice.cvsc.CVSCUtilities;
import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;


public
class		MainFrame
extends		JFrame
implements	ActionListener
	{
	private JCVS			app;
	private JMenuBar		menuBar;
	private JMenu			fileMenu;
	private JMenu			helpMenu;
	private MainPanel		mainPanel;

	private String			lastBrowseDirectory = null;
	

	public
	MainFrame( JCVS jcvs, String title, Rectangle bounds )
		{
		super( title );

		this.app = jcvs;

		this.mainPanel = new MainPanel( this );
		
		this.getContentPane().add( mainPanel );

		this.establishMenuBar();

		this.addWindowListener(
			new WindowAdapter()
				{
				public void
				windowClosing( WindowEvent e )
					{ app.performShutDown(); }
				}
			);

		this.pack();

		if ( bounds != null )
			{
			this.setBounds( bounds );
			}
		}

	public void
	loadPreferences()
		{
		this.mainPanel.loadPreferences();
		}

	public void
	savePreferences()
		{
		Rectangle bounds = this.getBounds();

		if ( bounds.x >= 0 && bounds.y >= 0
				&& bounds.width > 0 && bounds.height > 0 )
			{
			Config.getPreferences().setBounds
				( Config.MAIN_WINDOW_BOUNDS, bounds );
			}

		this.mainPanel.savePreferences();
		}

	public void
	addProjectToWorkBench( CVSProject project )
		{
		this.mainPanel.addProjectToWorkBench( project );
		}

	public void
	actionPerformed( ActionEvent event )
		{
		String command = event.getActionCommand();

		if ( command.equals( "QUIT" ) )
			{
			SwingUtilities.invokeLater(
				new Runnable()
					{
					public void
					run()
						{
						app.performShutDown();
						}
					}
				);
			}
		else if ( command.equals( "ABOUT" ) )
			{
			SwingUtilities.invokeLater(
				new Runnable()
					{
					public void
					run()
						{
						AboutDialog dlg =
							new AboutDialog( MainFrame.this );
						dlg.show();
						}
					}
				);
			}
		else if ( command.equals( "BUGREPORT" ) )
			{
			SwingUtilities.invokeLater(
				new Runnable()
					{
					public void
					run()
						{
						showHTMLDialog
							( "info.howto.report.bug.title",
								"info.howto.report.bug.html" );
						}
					}
				);
			}
		else if ( command.equals( "HOMEPAGE" ) )
			{
			SwingUtilities.invokeLater(
				new Runnable()
					{
					public void
					run()
						{
						showHTMLDialog
							( "info.homepage.title",
								"info.homepage.html" );
						}
					}
				);
			}
		else if ( command.equals( "MAILLIST" ) )
			{
			SwingUtilities.invokeLater(
				new Runnable()
					{
					public void
					run()
						{
						showHTMLDialog
							( "info.maillist.title",
								"info.maillist.html" );
						}
					}
				);
			}
		else if ( command.equals( "BROWSE" ) )
			{
			this.performBrowse();
			}
		else if ( command.equals( "CONFIG" ) )
			{
			SwingUtilities.invokeLater(
				new Runnable()
					{
					public void
					run()
						{
						Config.getInstance().editConfiguration
							( MainFrame.this );
						}
					}
				);
			}
		else
			{
			System.err.println
				( "UNKNOWN Command '" + command + "'" );
			}
		}

	public void
	showHTMLDialog( String titleKey, String msgKey )
		{
		String msgStr =
			ResourceMgr.getInstance().getUIString( msgKey );

		String title =
			ResourceMgr.getInstance().getUIString( titleKey );

		HTMLDialog dlg =
			new HTMLDialog
				( null, title, true, msgStr );

		Dimension newSz = new Dimension( 560, 420 );
		dlg.setSize( newSz );
		Point location =
			AWTUtilities.computeDialogLocation( dlg );
		dlg.setLocation( location.x, location.y );

		dlg.show();
		}

	public void
	performBrowse()
		{
		Config cfg = Config.getInstance();
		UserPrefs prefs = cfg.getPreferences();

		String prompt =
			ResourceMgr.getInstance().getUIString( "open.project.prompt" );

		String localRootDirName =
			ProjectFrame.getUserSelectedProject
				( this, prompt, this.lastBrowseDirectory );

		if ( localRootDirName != null )
			{
			this.lastBrowseDirectory =
				CVSCUtilities.exportPath( localRootDirName );

			if ( ! ProjectFrameMgr.checkProjectOpen( localRootDirName ) )
				{
				String entriesPath = CVSProject.getAdminEntriesPath
					( CVSProject.rootPathToAdminPath( localRootDirName ) );

				File entriesFile = new File( entriesPath );
				File rootDirFile = new File( localRootDirName );
				
				ProjectFrame.openProject( rootDirFile, null );
				}
			}
		}

	private void
	establishMenuBar()
		{
		JMenuItem	item;

		this.menuBar = new JMenuBar();

		this.addFileMenu( this.menuBar );

		this.addHelpMenu( this.menuBar );

		this.setJMenuBar( this.menuBar );
		}

	private void
	addFileMenu( JMenuBar mbar )
		{
		JMenuItem	item;

		ResourceMgr rmgr = ResourceMgr.getInstance();

		this.fileMenu = new JMenu( rmgr.getUIString( "menu.file.name" ) );
		mbar.add( this.fileMenu );

		item = new JMenuItem( rmgr.getUIString( "menu.file.open.name" ) );
		this.fileMenu.add( item );
		item.addActionListener( this );
		item.setActionCommand( "BROWSE" );
		item.setAccelerator
			( KeyStroke.getKeyStroke
				( KeyEvent.VK_O, Event.CTRL_MASK ) );

		this.fileMenu.addSeparator();

		item = new JMenuItem( rmgr.getUIString( "menu.file.edit.name" ) );
		this.fileMenu.add( item );
		item.addActionListener( this );
		item.setActionCommand( "CONFIG" );

		this.fileMenu.addSeparator();

		item = new JMenuItem( rmgr.getUIString( "menu.file.quit.name" ) );
		this.fileMenu.add( item );
		item.addActionListener( this );
		item.setActionCommand( "QUIT" );
		item.setAccelerator
			( KeyStroke.getKeyStroke
				( KeyEvent.VK_Q, Event.CTRL_MASK ) );
		}

	private void
	addHelpMenu( JMenuBar mbar )
		{
		JMenuItem	item;

		ResourceMgr rmgr = ResourceMgr.getInstance();

		this.helpMenu = new JMenu( rmgr.getUIString( "menu.help.name" ) );
		mbar.add( this.helpMenu );

		boolean haveJH = false;
		try {
			Class cls = Class.forName( "javax.help.HelpSet" );
			haveJH = true;
			}
		catch ( ClassNotFoundException ex )
			{
			haveJH = false;
			}

		if ( haveJH )
			{
			this.addJavaHelpItem();
			}
		else
			{
			CVSLog.logMsgStderr( "JavaHelp is not available." );
			}

		item = new JMenuItem( rmgr.getUIString( "menu.help.homepage.name" ) );
		this.helpMenu.add( item );
		item.addActionListener( this );
		item.setActionCommand( "HOMEPAGE" );

		item = new JMenuItem( rmgr.getUIString( "menu.help.maillist.name" ) );
		this.helpMenu.add( item );
		item.addActionListener( this );
		item.setActionCommand( "MAILLIST" );

		item = new JMenuItem( rmgr.getUIString( "menu.help.bugreport.name" ) );
		this.helpMenu.add( item );
		item.addActionListener( this );
		item.setActionCommand( "BUGREPORT" );

		this.helpMenu.addSeparator();

		item = new JMenuItem( rmgr.getUIString( "menu.help.about.name" ) );
		this.helpMenu.add( item );
		item.addActionListener( this );
		item.setActionCommand( "ABOUT" );
		}

	/**
	 * We push this down to prevent JIT's from dying without JavaHelp classes.
	 */

	private void
	addJavaHelpItem()
		{
		JMenuItem	item;
		String helpSetUrlName = "com/ice/jcvsii/doc/help/help.hs";
		ResourceMgr rmgr = ResourceMgr.getInstance();

		try {
			ClassLoader loader =
				MainFrame.class.getClassLoader();

			URL hsURL =
				HelpSet.findHelpSet( loader, helpSetUrlName );

			if ( hsURL == null )
				{
				throw new Exception
					( "HelpSet URL is null (not found?)" );
				}

			HelpSet hs =
				new HelpSet( loader, hsURL );

			HelpBroker hb = hs.createHelpBroker();

			item = new JMenuItem( rmgr.getUIString( "menu.help.javahelp.name" ) );
			this.helpMenu.add( item );
			item.addActionListener( new CSH.DisplayHelpFromSource( hb ) );

			this.helpMenu.addSeparator();
			}
		catch ( Exception ex )
			{
			JOptionPane.showMessageDialog( this,
				"Could not open HelpSet '" + helpSetUrlName
					+ "',\n" + ex.getMessage(),
				"Warning", JOptionPane.WARNING_MESSAGE );
			}
		}


	public static void
	setWaitCursor( Container cont, boolean busy )
		{
		Cursor curs =
			busy
				? Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR )
				: Cursor.getPredefinedCursor( Cursor.DEFAULT_CURSOR );

		cont.setCursor( curs );

		for ( int i = 0, sz = cont.getComponentCount() ; i < sz ; ++i )
			{
			Component comp = cont.getComponent( i );
			Class contCls = Container.class;
			Class compCls = comp.getClass();
			if ( contCls.isAssignableFrom( compCls ) )
				{
				MainFrame.setWaitCursor( (Container)comp, busy );
				}
			else
				{
				comp.setCursor( curs );
				}
			}
		}

	}

