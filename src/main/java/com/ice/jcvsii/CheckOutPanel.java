
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
class		CheckOutPanel
extends		MainTabPanel
implements	ActionListener, CVSUserInterface
	{
	protected CVSClient			client;
	protected ConnectInfoPanel	info;
	protected JTextField		argumentsText;
	protected JTextField		localDirText;

	protected JTextArea			outputText;
	protected JLabel			feedback;

	protected JButton			actionButton;


	public
	CheckOutPanel( MainPanel parent )
		{
		super( parent );
		this.establishContents();
		}

	public void
	loadPreferences()
		{
		this.info.loadPreferences( "chkout" );
		}

	public void
	savePreferences()
		{
		this.info.savePreferences( "chkout" );
		}

	public void
	actionPerformed( ActionEvent event )
		{
		String command = event.getActionCommand();

		if ( command.equalsIgnoreCase( "CHECKOUT" ) )
			{
			this.performCheckout();
			}
		else if ( command.equalsIgnoreCase( "CANCEL" ) )
			{
			this.cancelCheckout();
			}
		}

	private void
	cancelCheckout()
		{
		this.client.setCanceled( true );
		}

	private void
	performCheckout()
		{
		boolean listingModules = false;
		Config cfg = Config.getInstance();
		UserPrefs prefs = cfg.getPreferences();
		ResourceMgr rmgr = ResourceMgr.getInstance();

		String argumentStr = info.getArguments();
		String userName = this.info.getUserName();
		String passWord = this.info.getPassword();
		String hostname = this.info.getServer();
		String repository = this.info.getModule();
		String rootDirectory = this.info.getRepository();

		String localDirectory =
			CVSCUtilities.stripFinalSeparator
				( this.info.getLocalDirectory() );

		boolean isPServer = this.info.isPServer();

		int connMethod = this.info.getConnectionMethod();

		int cvsPort =
			CVSUtilities.computePortNum
				( hostname, connMethod, isPServer );

		CVSArgumentVector arguments =
			CVSArgumentVector.parseArgumentString( argumentStr );

		if ( arguments.containsArgument( "-c" ) )
			{
			listingModules = true;
			}
		else if ( repository.length() < 1 )
			{
			// SANITY
			String[] fmtArgs = new String[1];
			fmtArgs[0] = rmgr.getUIString( "name.for.cvsmodule" );

			String title =
				rmgr.getUIString( "checkout.needs.input.title" );
			String msg =
				rmgr.getUIFormat( "checkout.needs.input.msg", fmtArgs );

			JOptionPane.showMessageDialog
				( (Frame)this.getTopLevelAncestor(),
					msg, title, JOptionPane.ERROR_MESSAGE );
			}

		// SANITY
		if ( hostname.length() < 1
				|| rootDirectory.length() < 1
					|| localDirectory.length() < 1 )
			{
			String[] fmtArgs = new String[1];
			fmtArgs[0] =
				( hostname.length() < 1
					? rmgr.getUIString( "name.for.cvsserver" ) :
				( rootDirectory.length() < 1
					? rmgr.getUIString( "name.for.cvsrepos" )
					: rmgr.getUIString( "name.for.checkoutdir" ) ));

			String msg = rmgr.getUIFormat( "checkout.needs.input.msg", fmtArgs );
			String title = rmgr.getUIString( "checkout.needs.input.title" );
			JOptionPane.showMessageDialog
				( (Frame)this.getTopLevelAncestor(),
					msg, title, JOptionPane.ERROR_MESSAGE );

			return;
			}

		// SANITY
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

		File localRootDir = new File( localDirectory );
		if ( ! localRootDir.exists() && ! listingModules )
			{
			if ( ! localRootDir.mkdirs() )
				{
				String [] fmtArgs = { localRootDir.getPath() };
				String msg = ResourceMgr.getInstance().getUIFormat
					("checkout.create.dir.failed.msg", fmtArgs );
				String title = ResourceMgr.getInstance().getUIString
					("checkout.create.dir.failed.title" );
				JOptionPane.showMessageDialog
					( (Frame)this.getTopLevelAncestor(),
						msg, title, JOptionPane.ERROR_MESSAGE );
				return;
				}
			}

		CVSRequest request = new CVSRequest();

		String checkOutCommand =
			prefs.getProperty
				( "global.checkOutCommand", ":co:N:ANP:deou:" );

		if ( ! request.parseControlString( checkOutCommand ) )
			{
			String [] fmtArgs =
				{ checkOutCommand, request.getVerifyFailReason() };
			String msg = rmgr.getUIFormat("checkout.cmd.parse.failed.msg", fmtArgs );
			String title = rmgr.getUIString("checkout.cmd.parse.failed.title" );
			JOptionPane.showMessageDialog
				( (Frame)this.getTopLevelAncestor(),
					msg, title, JOptionPane.ERROR_MESSAGE );
			return;
			}

		//
		// DO IT
		//
		CVSEntryVector entries = new CVSEntryVector();

		if ( ! listingModules )
			{
			arguments.appendArgument( repository );
			}

		this.getMainPanel().setAllTabsEnabled( false );

		this.client = CVSUtilities.createCVSClient( hostname, cvsPort );
		CVSProject project = new CVSProject( this.client );

		CVSProjectDef projectDef = new CVSProjectDef
			( connMethod, isPServer, false,
				hostname, userName, rootDirectory, repository );

		project.setProjectDef( projectDef );

		project.setUserName( userName );

		project.setTempDirectory( cfg.getTemporaryDirectory() );
		project.setRepository( repository );
		project.setRootDirectory( rootDirectory );
		project.setLocalRootDirectory( localDirectory );
		project.setPServer( isPServer );
		project.setConnectionPort( cvsPort );
		project.setConnectionMethod( connMethod );

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

		if ( connMethod == CVSRequest.METHOD_RSH )
			{
			CVSUtilities.establishRSHProcess( project );
			}

		// Finally, we must make sure that the Project has its root entry, as
		// CVSProject will not be able to create it from the context that the
		// server will send with the checkout.

		project.establishRootEntry( rootDirectory );

		// UNDONE
		// IF IT IS ALREADY OPEN, we should tell the ProjectFrame to do this!!!
		//
		if ( ! ProjectFrameMgr.checkProjectOpen
				( project.getLocalRootDirectory() ) )
			{
			String title = repository + " project";

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

			request.responseHandler = project;

			request.traceRequest = CVSProject.overTraceRequest;
			request.traceResponse = CVSProject.overTraceResponse;
			request.traceTCPData = CVSProject.overTraceTCP;
			request.traceProcessing = CVSProject.overTraceProcessing;

			request.allowGzipFileMode = project.allowsGzipFileMode();
			request.setGzipStreamLevel( project.getGzipStreamLevel() );

			request.setEntries( entries );

			request.appendArguments( arguments );

			request.setUserInterface( this );

			CVSResponse response = new CVSResponse();

			CVSThread thread =
				new CVSThread
					( "CheckOut",
						this.new MyRunner
							( project, this.client,
								request, response, listingModules ),
						this.new MyMonitor
							( request, response, listingModules ) );

			thread.start();
			}
		}

	private
	class		MyRunner
	implements	Runnable
		{
		private CVSClient client;
		private CVSProject project;
		private CVSRequest request;
		private CVSResponse response;
		private boolean listingMods;

		public
		MyRunner( CVSProject project, CVSClient client,
					CVSRequest request, CVSResponse response,
					boolean listingMods )
			{
			this.client = client;
			this.project = project;
			this.request = request;
			this.response = response;
			this.listingMods = listingMods;
			}

		public void
		run()
			{
			this.client.processCVSRequest( this.request, this.response );
			if ( ! this.listingMods )
				{
				this.project.processCVSResponse( this.request, response );
				if ( this.request.getArguments().containsArgument( "-P" )
						|| this.request.getArguments().containsArgument( "-r" )
						|| this.request.getArguments().containsArgument( "-D" ) )
					{
					this.project.pruneEmptySubDirs
						( this.request.handleEntries );
					}
				}
			}
		}

	private
	class		MyMonitor
	implements	CVSThread.Monitor
		{
		private CVSRequest request;
		private CVSResponse response;
		private boolean listingMods;

		public
		MyMonitor( CVSRequest request, CVSResponse response, boolean listingMods )
			{
			this.request = request;
			this.response = response;
			this.listingMods = listingMods;
			}

		public void
		threadStarted()
			{
			actionButton.setActionCommand( "CANCEL" );
			actionButton.setText
				( ResourceMgr.getInstance().getUIString
					( "checkout.cancel.label" ) );
			}

		public void
		threadCanceled()
			{
			}

		public void
		threadFinished()
			{
			actionButton.setActionCommand( "CHECKOUT" );
			actionButton.setText
				( ResourceMgr.getInstance().getUIString
					( "checkout.perform.label" ) );

			String resultStr = this.response.getDisplayResults();

			if ( this.response.getStatus() == CVSResponse.OK )
				{
				uiDisplayProgressMsg
					( ResourceMgr.getInstance().getUIString
						( "checkout.status.success" ) );

				if ( ! this.listingMods )
					{
					File rootDirFile =
						new File( request.getLocalDirectory()
									+ "/" + request.getRepository() );

					ProjectFrame.openProject
						( rootDirFile, request.getPassword() );
					}
				}
			else
				{
				uiDisplayProgressMsg
					( ResourceMgr.getInstance().getUIString
						( "checkout.status.failure" ) );
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

		this.info = new ConnectInfoPanel( "checkout" );
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
					( "checkout.perform.label" ) );

		this.actionButton.setActionCommand( "CHECKOUT" );
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

