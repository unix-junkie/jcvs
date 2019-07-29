
package com.ice.jcvsii;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
import com.ice.cvsc.CVSCUtilities;
import com.ice.cvsc.CVSClient;
import com.ice.cvsc.CVSEntryVector;
import com.ice.cvsc.CVSProject;
import com.ice.cvsc.CVSProjectDef;
import com.ice.cvsc.CVSRequest;
import com.ice.cvsc.CVSResponse;
import com.ice.cvsc.CVSScramble;
import com.ice.cvsc.CVSUserInterface;
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
	CheckOutPanel( final MainPanel parent )
		{
		super( parent );
		this.establishContents();
		}

	public void
	loadPreferences()
		{
		this.info.loadPreferences( "chkout" );
		}

	@Override
	public void
	savePreferences()
		{
		this.info.savePreferences( "chkout" );
		}

	@Override
	public void
	actionPerformed( final ActionEvent event )
		{
		final String command = event.getActionCommand();

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
		final Config cfg = Config.getInstance();
		final UserPrefs prefs = Config.getPreferences();
		final ResourceMgr rmgr = ResourceMgr.getInstance();

		final String argumentStr = info.getArguments();
		final String userName = this.info.getUserName();
		final String passWord = this.info.getPassword();
		final String hostname = this.info.getServer();
		final String repository = this.info.getModule();
		final String rootDirectory = this.info.getRepository();

		final String localDirectory =
			CVSCUtilities.stripFinalSeparator
				( this.info.getLocalDirectory() );

		final boolean isPServer = this.info.isPServer();

		final int connMethod = this.info.getConnectionMethod();

		final int cvsPort =
			CVSUtilities.computePortNum
				( hostname, connMethod, isPServer );

		final CVSArgumentVector arguments =
			CVSArgumentVector.parseArgumentString( argumentStr );

		if ( arguments.containsArgument( "-c" ) )
			{
			listingModules = true;
			}
		else if ( repository.length() < 1 )
			{
			// SANITY
			final String[] fmtArgs = new String[1];
			fmtArgs[0] = rmgr.getUIString( "name.for.cvsmodule" );

			final String title =
				rmgr.getUIString( "checkout.needs.input.title" );
			final String msg =
				rmgr.getUIFormat( "checkout.needs.input.msg", fmtArgs );

			JOptionPane.showMessageDialog
				( this.getTopLevelAncestor(),
					msg, title, JOptionPane.ERROR_MESSAGE );
			}

		// SANITY
		if ( hostname.length() < 1
				|| rootDirectory.length() < 1
					|| localDirectory.length() < 1 )
			{
			final String[] fmtArgs = new String[1];
			fmtArgs[0] =
				hostname.length() < 1
					? rmgr.getUIString( "name.for.cvsserver" ) :
				rootDirectory.length() < 1
					? rmgr.getUIString( "name.for.cvsrepos" )
					: rmgr.getUIString( "name.for.checkoutdir" );

			final String msg = rmgr.getUIFormat( "checkout.needs.input.msg", fmtArgs );
			final String title = rmgr.getUIString( "checkout.needs.input.title" );
			JOptionPane.showMessageDialog
				( this.getTopLevelAncestor(),
					msg, title, JOptionPane.ERROR_MESSAGE );

			return;
			}

		// SANITY
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

		final File localRootDir = new File( localDirectory );
		if ( ! localRootDir.exists() && ! listingModules )
			{
			if ( ! localRootDir.mkdirs() )
				{
				final String [] fmtArgs = { localRootDir.getPath() };
				final String msg = ResourceMgr.getInstance().getUIFormat
					("checkout.create.dir.failed.msg", fmtArgs );
				final String title = ResourceMgr.getInstance().getUIString
					("checkout.create.dir.failed.title" );
				JOptionPane.showMessageDialog
					( this.getTopLevelAncestor(),
						msg, title, JOptionPane.ERROR_MESSAGE );
				return;
				}
			}

		final CVSRequest request = new CVSRequest();

		final String checkOutCommand =
			prefs.getProperty
				( "global.checkOutCommand", ":co:N:ANP:deou:" );

		if ( ! request.parseControlString( checkOutCommand ) )
			{
			final String [] fmtArgs =
				{ checkOutCommand, request.getVerifyFailReason() };
			final String msg = rmgr.getUIFormat("checkout.cmd.parse.failed.msg", fmtArgs );
			final String title = rmgr.getUIString("checkout.cmd.parse.failed.title" );
			JOptionPane.showMessageDialog
				( this.getTopLevelAncestor(),
					msg, title, JOptionPane.ERROR_MESSAGE );
			return;
			}

		//
		// DO IT
		//
		final CVSEntryVector entries = new CVSEntryVector();

		if ( ! listingModules )
			{
			arguments.appendArgument( repository );
			}

		this.getMainPanel().setAllTabsEnabled( false );

		this.client = CVSUtilities.createCVSClient( hostname, cvsPort );
		final CVSProject project = new CVSProject( this.client );

		final CVSProjectDef projectDef = new CVSProjectDef
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
			final String title = repository + " project";

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

			final CVSResponse response = new CVSResponse();

			final CVSThread thread =
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
		private final CVSClient client;
		private final CVSProject project;
		private final CVSRequest request;
		private final CVSResponse response;
		private final boolean listingMods;

		public
		MyRunner( final CVSProject project, final CVSClient client,
					final CVSRequest request, final CVSResponse response,
					final boolean listingMods )
			{
			this.client = client;
			this.project = project;
			this.request = request;
			this.response = response;
			this.listingMods = listingMods;
			}

		@Override
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
		private final CVSRequest request;
		private final CVSResponse response;
		private final boolean listingMods;

		public
		MyMonitor( final CVSRequest request, final CVSResponse response, final boolean listingMods )
			{
			this.request = request;
			this.response = response;
			this.listingMods = listingMods;
			}

		@Override
		public void
		threadStarted()
			{
			actionButton.setActionCommand( "CANCEL" );
			actionButton.setText
				( ResourceMgr.getInstance().getUIString
					( "checkout.cancel.label" ) );
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
			actionButton.setActionCommand( "CHECKOUT" );
			actionButton.setText
				( ResourceMgr.getInstance().getUIString
					( "checkout.perform.label" ) );

			final String resultStr = this.response.getDisplayResults();

			if ( this.response.getStatus() == CVSResponse.OK )
				{
				uiDisplayProgressMsg
					( ResourceMgr.getInstance().getUIString
						( "checkout.status.success" ) );

				if ( ! this.listingMods )
					{
					final File rootDirFile =
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

		this.info = new ConnectInfoPanel( "checkout" );
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

