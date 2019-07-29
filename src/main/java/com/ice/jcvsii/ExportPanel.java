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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import com.ice.cvsc.CVSArgumentVector;
import com.ice.cvsc.CVSClient;
import com.ice.cvsc.CVSEntryVector;
import com.ice.cvsc.CVSProject;
import com.ice.cvsc.CVSRequest;
import com.ice.cvsc.CVSResponse;
import com.ice.cvsc.CVSScramble;
import com.ice.cvsc.CVSUserInterface;
import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;


public
class		ExportPanel
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
	ExportPanel( final MainPanel parent )
		{
		super( parent );
		this.establishContents();
		}

	public void
	loadPreferences()
		{
		this.info.loadPreferences( "export" );
		}

	@Override
	public void
	savePreferences()
		{
		this.info.savePreferences( "export" );
		}

	@Override
	public void
	actionPerformed( final ActionEvent event )
		{
		final String command = event.getActionCommand();

		if ( command.equalsIgnoreCase( "EXPORT" ) )
			{
			this.performExport();
			}
		else if ( command.equalsIgnoreCase( "CANCEL" ) )
			{
			this.cancelExport();
			}
		}

	private void
	cancelExport()
		{
		this.client.setCanceled( true );
		}

	private void
	performExport()
		{
		final Config cfg = Config.getInstance();
		final UserPrefs prefs = Config.getPreferences();
		final ResourceMgr rmgr = ResourceMgr.getInstance();

		CVSProject		project;
		CVSRequest		request;
		Point			location;

		location = this.getLocationOnScreen();

		final String	argumentStr = this.info.getArguments();

		final CVSEntryVector entries = new CVSEntryVector();

		final CVSArgumentVector arguments =
			CVSArgumentVector.parseArgumentString( argumentStr );

		final String userName = this.info.getUserName();
		final String passWord = this.info.getPassword();
		final String hostname = this.info.getServer();
		final String repository = this.info.getModule();
		final String rootDirectory = this.info.getRepository();
		final String exportDirectory = this.info.getExportDirectory();

		final boolean isPServer = this.info.isPServer();

		final int connMethod = this.info.getConnectionMethod();

		final int cvsPort =
			CVSUtilities.computePortNum
				( hostname, connMethod, isPServer );

		//
		// SANITY
		//
		if ( arguments.size() < 2
			|| ! arguments.containsArgument( "-r" )
				&& ! arguments.containsArgument( "-D" ) )
			{
			final String msg = rmgr.getUIString( "export.needs.option.msg" );
			final String title = rmgr.getUIString( "export.needs.option.title" );
			JOptionPane.showMessageDialog
				( this.getTopLevelAncestor(),
					msg, title, JOptionPane.ERROR_MESSAGE );
			return;
			}

		if ( hostname.length() < 1 || repository.length() < 1
				|| rootDirectory.length() < 1
					|| exportDirectory.length() < 1 )
			{
			final String[] fmtArgs = new String[1];
			fmtArgs[0] =
				hostname.length() < 1
					? rmgr.getUIString( "name.for.cvsserver" ) :
				repository.length() < 1
					? rmgr.getUIString( "name.for.cvsmodule" ) :
				rootDirectory.length() < 1
					? rmgr.getUIString( "name.for.cvsrepos" )
					: rmgr.getUIString( "name.for.exportdir" );

			final String msg = rmgr.getUIFormat( "export.needs.input.msg", fmtArgs );
			final String title = rmgr.getUIString( "export.needs.input.title" );
			JOptionPane.showMessageDialog
				( this.getTopLevelAncestor(),
					msg, title, JOptionPane.ERROR_MESSAGE );
			return;
			}

		if ( userName.length() < 1
				&& (connMethod == CVSRequest.METHOD_RSH
					|| connMethod == CVSRequest.METHOD_SSH) )
			{
			final String msg = rmgr.getUIString("common.rsh.needs.user.msg" );
			final String title = rmgr.getUIString("common.rsh.needs.user.title" );
			JOptionPane.showMessageDialog
				( this.getTopLevelAncestor(),
					msg, title, JOptionPane.ERROR_MESSAGE );
			return;
			}

		final File localRootDir = new File( exportDirectory );

		if ( ! localRootDir.exists() )
			{
			final String[] fmtArgs = { localRootDir.getPath() };
			final String prompt =
				rmgr.getUIFormat
					( "export.create.directory.prompt", fmtArgs );
			String title =
				rmgr.getUIString( "export.create.directory.title" );

			if ( JOptionPane.showConfirmDialog
					( this.getTopLevelAncestor(), prompt,
						title, JOptionPane.YES_NO_OPTION )
					== JOptionPane.NO_OPTION )
				{
				return;
				}

			if ( ! localRootDir.mkdirs() )
				{
				final String[] failArgs = { localRootDir.getPath() };
				final String msg = rmgr.getUIFormat
					( "export.create.directory.failed.msg", failArgs );
				title = rmgr.getUIString
					( "export.create.directory.failed.title" );
				JOptionPane.showMessageDialog
					( this.getTopLevelAncestor(),
						msg, title, JOptionPane.ERROR_MESSAGE );
				return;
				}
			}

		//
		// DO IT
		//

		this.getMainPanel().setAllTabsEnabled( false );

		this.client = CVSUtilities.createCVSClient( hostname, cvsPort );
		project = new CVSProject( this.client );

		project.setUserName( userName );

		project.setTempDirectory( cfg.getTemporaryDirectory() );
		project.setRepository( repository );
		project.setRootDirectory( rootDirectory );
		project.setLocalRootDirectory( exportDirectory );
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
			( prefs.getBoolean( ConfigConstants.GLOBAL_ALLOWS_FILE_GZIP, false ) );

		project.setGzipStreamLevel
			( prefs.getInteger( ConfigConstants.GLOBAL_GZIP_STREAM_LEVEL, 0 ) );

		if ( isPServer )
			{
			final String scrambled =
				CVSScramble.scramblePassword( passWord, 'A' );

			project.setPassword( scrambled );
			}
		else if ( connMethod == CVSRequest.METHOD_SSH )
			{
			project.setPassword( passWord );
			}

		// Finally, we must make sure that the Project has its root entry, as
		// CVSProject will not be able to create it from the context that the
		// server will send with the checkout.

		project.establishRootEntry( rootDirectory );

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
		request.setHostName( this.client.getHostName() );

		request.setRepository( repository );
		request.setRootDirectory( rootDirectory );
		request.setRootRepository( rootDirectory );
		request.setLocalDirectory( localRootDir.getPath() );

		request.setSetVariables( project.getSetVariables() );

		request.setCommand( "export" );
		request.sendModule = false;
		request.sendArguments = true;
		request.handleUpdated = true;
		request.allowOverWrites = true;
		request.queueResponse = false;
		request.responseHandler = project;
		request.includeNotifies = false;

		request.traceRequest = CVSProject.overTraceRequest;
		request.traceResponse = CVSProject.overTraceResponse;
		request.traceTCPData = CVSProject.overTraceTCP;
		request.traceProcessing = CVSProject.overTraceProcessing;

		request.allowGzipFileMode = project.allowsGzipFileMode();
		request.setGzipStreamLevel( project.getGzipStreamLevel() );

		// REVIEW - should the user be able to override? Docs say no.
		// arguments.appendArgument( "-kv" );

		request.setEntries( entries );

		// Add the repository name to export as an argument.
		arguments.appendArgument( repository );

		request.appendArguments( arguments );

		request.setUserInterface( this );

		final CVSResponse response = new CVSResponse();

		final CVSThread thread =
			new CVSThread( "Export",
				this.new MyRunner( project, this.client, request, response ),
					this.new MyMonitor( request, response ) );

		thread.start();
		}

	private
	class		MyRunner
	implements	Runnable
		{
		private final CVSClient client;
		private final CVSProject project;
		private final CVSRequest request;
		private final CVSResponse response;

		public
		MyRunner( final CVSProject project, final CVSClient client,
					final CVSRequest request, final CVSResponse response )
			{
			this.client = client;
			this.project = project;
			this.request = request;
			this.response = response;
			}

		@Override
		public void
		run()
			{
			this.client.processCVSRequest( this.request, this.response );

			this.project.processCVSResponse( this.request, response );

			if ( this.request.getArguments().containsArgument( "-P" )
					|| this.request.getArguments().containsArgument( "-r" )
					|| this.request.getArguments().containsArgument( "-D" ) )
				{
				this.project.pruneEmptySubDirs( false );
				}
			}
		}

	private
	class		MyMonitor
	implements	CVSThread.Monitor
		{
		private final CVSRequest request;
		private final CVSResponse response;

		public
		MyMonitor( final CVSRequest request, final CVSResponse response )
			{
			this.request = request;
			this.response = response;
			}

		@Override
		public void
		threadStarted()
			{
			actionButton.setText
				( ResourceMgr.getInstance().getUIString
					( "export.cancel.label" ) );
			actionButton.setActionCommand( "CANCEL" );
			}

		@Override
		public void
		threadCanceled()
			{
			}

		@Override
		public void
		threadFinished()
			{
			actionButton.setText
				( ResourceMgr.getInstance().getUIString
					( "export.perform.label" ) );
			actionButton.setActionCommand( "EXPORT" );

			final String resultStr = this.response.getDisplayResults();

			if ( this.response.getStatus() == CVSResponse.OK )
				{
				uiDisplayProgressMsg
					( ResourceMgr.getInstance().getUIString
						( "export.status.success" ) );
				}
			else
				{
				uiDisplayProgressMsg
					( ResourceMgr.getInstance().getUIString
						( "export.status.failure" ) );
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

	@Override
	public void
	uiDisplayProgressMsg( final String message )
		{
		this.feedback.setText( message );
		this.feedback.repaint( 0 );
		}

	@Override
	public void
	uiDisplayProgramError( final String error )
		{
		}

	@Override
	public void
	uiDisplayResponse( final CVSResponse response )
		{
		}

	//
	// END OF CVS USER INTERFACE METHODS
	//

	private void
	establishContents()
		{
		final JLabel		lbl;
		final JPanel		panel;
		final JButton		button;

		this.setLayout( new GridBagLayout() );

		this.info = new ConnectInfoPanel( "export" );
		this.info.setPServerMode( true );
		this.info.setUsePassword( true );

		// ============== INPUT FIELDS PANEL ================

		int row = 0;

		final JSeparator sep;

		AWTUtilities.constrain(
			this, info,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			0, row++, 1, 1, 1.0, 0.0 );

		this.actionButton =
			new JButton
				( ResourceMgr.getInstance().getUIString
					( "export.perform.label" ) );
		this.actionButton.setActionCommand( "EXPORT" );
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
				@Override
				public boolean isFocusTraversable() { return false; }
				};
		this.outputText.setEditable( false );

		final JScrollPane scroller =
			new JScrollPane( this.outputText );
		scroller.setVerticalScrollBarPolicy
			( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );

		AWTUtilities.constrain(
			this, scroller,
			GridBagConstraints.BOTH,
			GridBagConstraints.CENTER,
			0, row++, 1, 1, 1.0, 1.0 );
		}

	}

