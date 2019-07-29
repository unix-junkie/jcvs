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
import java.awt.event.*;
import java.io.File;
import javax.swing.*;
import javax.swing.border.*;

import com.ice.cvsc.*;
import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;


public
class		CreatePanel
extends		MainTabPanel
implements	ActionListener, CVSUserInterface
	{
	protected CVSClient			client;
	protected ConnectInfoPanel	info;
	protected JTextField		argumentsText;
	protected JTextArea			outputText;
	protected JLabel			feedback;
	protected JButton			actionButton;


	public
	CreatePanel( MainPanel parent )
		{
		super( parent );
		this.establishContents();
		}

	public void
	loadPreferences()
		{
		this.info.loadPreferences( "create" );
		}

	public void
	savePreferences()
		{
		this.info.savePreferences( "create" );
		}

	public void
	actionPerformed( ActionEvent event )
		{
		String command = event.getActionCommand();

		if ( command.equalsIgnoreCase( "CREATE" ) )
			{
			this.performCreate();
			}
		else if ( command.equalsIgnoreCase( "CANCEL" ) )
			{
			this.cancelCreate();
			}
		}

	private void
	cancelCreate()
		{
		this.client.setCanceled( true );
		}

	private void
	performCreate()
		{
		Config cfg = Config.getInstance();
		UserPrefs prefs = cfg.getPreferences();
		ResourceMgr rmgr = ResourceMgr.getInstance();

		CVSClient		client;
		CVSProject		project;
		CVSRequest		request;
		Point			location;

		CVSEntryVector entries = new CVSEntryVector();

		CVSArgumentVector arguments = new CVSArgumentVector();

		String userName = this.info.getUserName();
		String passWord = this.info.getPassword();
		String hostname = this.info.getServer();
		String repository = this.info.getModule();
		String rootDirectory = this.info.getRepository();

		String vendorTag = "vendor-tag";
		String releaseTag = "release-tag";
		String message = "Creating new repository.";

		boolean isPServer = this.info.isPServer();

		int connMethod = this.info.getConnectionMethod();

		int cvsPort =
			CVSUtilities.computePortNum
				( hostname, connMethod, isPServer );

		//
		// SANITY
		//
		if ( hostname.length() < 1 || repository.length() < 1
				|| rootDirectory.length() < 1 )
			{
			String[] fmtArgs = new String[1];
			fmtArgs[0] =
				( hostname.length() < 1
					? rmgr.getUIString( "name.for.cvsserver" ) :
				( repository.length() < 1
					? rmgr.getUIString( "name.for.cvsmodule" )
					: rmgr.getUIString( "name.for.cvsrepos" ) ));

			String msg = rmgr.getUIFormat( "create.needs.input.msg", fmtArgs );
			String title = rmgr.getUIString( "create.needs.input.title" );
			JOptionPane.showMessageDialog
				( (Frame)this.getTopLevelAncestor(),
					msg, title, JOptionPane.ERROR_MESSAGE );
			return;
			}

		if ( userName.length() < 1
				&& (connMethod == CVSRequest.METHOD_RSH
					|| connMethod == CVSRequest.METHOD_SSH) )
			{
			String msg = rmgr.getUIString("common.rsh.needs.user.msg" );
			String title = rmgr.getUIString("common.rsh.needs.user.title" );
			JOptionPane.showMessageDialog
				( (Frame)this.getTopLevelAncestor(),
					msg, title, JOptionPane.ERROR_MESSAGE );
			return;
			}

		//
		// DO IT
		//
		this.getMainPanel().setAllTabsEnabled( false );

		client = CVSUtilities.createCVSClient( hostname, cvsPort );
		project = new CVSProject( client );
				
		project.setUserName( userName );

		project.setTempDirectory( cfg.getTemporaryDirectory() );
		project.setRepository( repository );
		project.setRootDirectory( rootDirectory );
		project.setLocalRootDirectory( prefs.getCurrentDirectory() );
		project.setPServer( isPServer );
		project.setConnectionPort( cvsPort );
		project.setConnectionMethod( connMethod );

		if ( connMethod == CVSRequest.METHOD_RSH )
			CVSUtilities.establishRSHProcess( project );

		project.setSetVariables
			( CVSUtilities.getUserSetVariables( hostname ) );

		project.setServerCommand(
			CVSUtilities.establishServerCommand
				( hostname, connMethod, isPServer ) );

		project.setAllowsGzipFileMode
			( prefs.getBoolean( Config.GLOBAL_ALLOWS_FILE_GZIP, false ) );

		project.setGzipStreamLevel
			( prefs.getInteger( Config.GLOBAL_GZIP_STREAM_LEVEL, 0 ) );

		if ( isPServer )
			{
			String scrambled =
				CVSScramble.scramblePassword( passWord, 'A' );

			project.setPassword( scrambled );
			}
		else if ( connMethod == CVSRequest.METHOD_SSH )
			{
			project.setPassword( passWord );
			}

		request = new CVSRequest();


		// NOTE that all of these redundant setters on request are
		//      needed because we are not using the typicall call to
		//      CVSProject.performCVSCommand(), which calls most of
		//      these setters for us.

		request.setPServer( isPServer );
		request.setUserName( userName );

		if ( isPServer || connMethod == CVSRequest.METHOD_SSH )
			{
			request.setPassword( project.getPassword() );
			}

		request.setConnectionMethod( connMethod );
		request.setServerCommand( project.getServerCommand() );
		request.setRshProcess( project.getRshProcess() );

		request.setPort( cvsPort );
		request.setHostName( client.getHostName() );

		request.setRepository( repository );
		request.setRootDirectory( rootDirectory );
		request.setRootRepository( rootDirectory );
		request.setLocalDirectory( prefs.getCurrentDirectory() );

		request.setSetVariables( project.getSetVariables() );

		request.setCommand( "import" );
		request.sendModule = false;
		request.sendArguments = true;
		request.handleUpdated = false;
		request.allowOverWrites = false;
		request.queueResponse = false;
		request.responseHandler = project;
		request.includeNotifies = false;

		request.traceRequest = CVSProject.overTraceRequest;
		request.traceResponse = CVSProject.overTraceResponse;
		request.traceTCPData = CVSProject.overTraceTCP;
		request.traceProcessing = CVSProject.overTraceProcessing;

		request.allowGzipFileMode = project.allowsGzipFileMode();
		request.setGzipStreamLevel( project.getGzipStreamLevel() );

		arguments.appendArgument( "-m" );
		arguments.appendArgument( message );

		arguments.appendArgument( repository );

		arguments.appendArgument( vendorTag );

		arguments.appendArgument( releaseTag );

		arguments.appendArgument( repository );

		request.setEntries( entries );

		request.setArguments( arguments );

		request.setUserInterface( this );

		CVSResponse response = new CVSResponse();

		CVSThread thread =
			new CVSThread( "Create",
				this.new MyRunner( project, client, request, response ),
					this.new MyMonitor( request, response ) );

		thread.start();
		}

	private
	class		MyRunner
	implements	Runnable
		{
		private CVSClient client;
		private CVSProject project;
		private CVSRequest request;
		private CVSResponse response;

		public
		MyRunner( CVSProject project, CVSClient client,
					CVSRequest request, CVSResponse response )
			{
			this.client = client;
			this.project = project;
			this.request = request;
			this.response = response;
			}

		public void
		run()
			{
			this.client.processCVSRequest( this.request, this.response );
			this.project.processCVSResponse( this.request, response );
			}
		}

	private
	class		MyMonitor
	implements	CVSThread.Monitor
		{
		private CVSRequest request;
		private CVSResponse response;

		public
		MyMonitor( CVSRequest request, CVSResponse response )
			{
			this.request = request;
			this.response = response;
			}

		public void
		threadStarted()
			{
			actionButton.setActionCommand( "CANCEL" );
			actionButton.setText
				( ResourceMgr.getInstance().getUIString
					( "create.cancel.label" ) );
			}

		public void
		threadCanceled()
			{
			}

		public void
		threadFinished()
			{
			actionButton.setActionCommand( "CREATE" );
			actionButton.setText
				( ResourceMgr.getInstance().getUIString
					( "create.perform.label" ) );

			String resultStr = this.response.getDisplayResults();

			if ( this.response.getStatus() == CVSResponse.OK )
				{
				uiDisplayProgressMsg
					( ResourceMgr.getInstance().getUIString
						( "create.status.success" ) );
				}
			else
				{
				uiDisplayProgressMsg
					( ResourceMgr.getInstance().getUIString
						( "create.status.failure" ) );
				}

			outputText.setText( resultStr );
			outputText.revalidate();
			outputText.repaint();

			if ( this.response != null
					&& ! this.request.saveTempFiles )
				{
				this.response.deleteTempFiles();
				}

			getMainPanel().setAllTabsEnabled( true );
			}

		}

	//
	// CVS USER INTERFACE METHODS
	//

	public void
	uiDisplayProgressMsg( String message )
		{
		this.feedback.setText( message );
		this.feedback.repaint( 0 );
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

	private void
	establishContents()
		{
		JLabel		lbl;
		JPanel		panel;
		JButton		button;

		this.setLayout( new GridBagLayout() );

		this.info = new ConnectInfoPanel( "create" );
		this.info.setPServerMode( true );
		this.info.setUsePassword( true );

		// ============== INPUT FIELDS PANEL ================

		int row = 0;

		JSeparator sep;

		AWTUtilities.constrain(
			this, info,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			0, row++, 1, 1, 1.0, 0.0 );

		this.actionButton =
			new JButton
				( ResourceMgr.getInstance().getUIString
					( "create.perform.label" ) );
		this.actionButton.setActionCommand( "CREATE" );
		this.actionButton.addActionListener( this );
		AWTUtilities.constrain(
			this, this.actionButton,
			GridBagConstraints.NONE,
			GridBagConstraints.CENTER,
			0, row++, 1, 1, 0.0, 0.0,
			new Insets( 5, 5, 5, 5 ) );

		this.feedback =
			new JLabel
				( ResourceMgr.getInstance().getUIString
					( "name.for.ready" ) );
		this.feedback.setOpaque( true );
		this.feedback.setBackground( Color.white );
		this.feedback.setBorder
			( new CompoundBorder
				( new LineBorder( Color.darkGray ),
					new EmptyBorder( 1, 3, 1, 3 ) ) );

		AWTUtilities.constrain(
			this, this.feedback,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.CENTER,
			0, row++, 1, 1, 1.0, 0.0,
			new Insets( 4, 0, 3, 0 ) );

		this.outputText =
			new JTextArea()
				{
				public boolean isFocusTraversable() { return false; }
				};
		this.outputText.setEditable( false );

		JScrollPane scroller =
			new JScrollPane( this.outputText );
		scroller.setVerticalScrollBarPolicy
			( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS );

		AWTUtilities.constrain(
			this, scroller,
			GridBagConstraints.BOTH,
			GridBagConstraints.CENTER,
			0, row++, 1, 1, 1.0, 1.0 );
		}

	}

