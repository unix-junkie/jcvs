
package com.ice.jcvsii;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
import com.ice.cvsc.CVSTracer;
import com.ice.cvsc.CVSUserInterface;
import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;


public
class		TestConnectPanel
extends		MainTabPanel
implements	ActionListener, CVSUserInterface
	{
	protected ConnectInfoPanel	info;
	protected CVSClient			client;
	protected CVSRequest		request;
	protected JTextField		argumentsText;
	protected JTextField		exportDirText;
	protected JTextArea			outputText;
	protected JLabel			feedback;
	protected JButton			actionButton;


	public
	TestConnectPanel( final MainPanel parent )
		{
		super( parent );
		this.establishContents();
		}

	public void
	loadPreferences()
		{
		this.info.loadPreferences( "testconn" );
		}

	public void
	savePreferences()
		{
		this.info.savePreferences( "testconn" );
		}

	public void
	actionPerformed( final ActionEvent event )
		{
		final String command = event.getActionCommand();

		if ( command.equalsIgnoreCase( "TESTCONN" ) )
			{
			this.performTest();
			}
		else if ( command.equalsIgnoreCase( "CANCEL" ) )
			{
			this.cancelTest();
			}
		}

	private void
	cancelTest()
		{
		this.client.setCanceled( true );
		}

	private void
	performTest()
		{
		final Config cfg = Config.getInstance();
		final UserPrefs prefs = Config.getPreferences();

		final CVSProject		project;
		Point			location;

		location = this.getLocationOnScreen();

		final String userName = this.info.getUserName();
		final String passWord = this.info.getPassword();
		final String hostname = this.info.getServer();
		final String rootDirectory = this.info.getRepository();

		final boolean isPServer = this.info.isPServer();

		final int connMethod = this.info.getConnectionMethod();

		final int cvsPort =
			CVSUtilities.computePortNum
				( hostname, connMethod, isPServer );

		//
		// SANITY
		//
		if ( hostname.length() < 1
				|| rootDirectory.length() < 1 )
			{
			JOptionPane.showMessageDialog
				( this.getTopLevelAncestor(),
					"The connection test requires that the " +
						( hostname.length() < 1
							? "cvs server hostname"
							: "cvs repository" ) + " be specified.",
				"Notice", JOptionPane.ERROR_MESSAGE );
			return;
			}

		if ( userName.length() < 1
				&& (connMethod == CVSRequest.METHOD_RSH
					|| connMethod == CVSRequest.METHOD_SSH) )
			{
			JOptionPane.showMessageDialog
				( this.getTopLevelAncestor(),
				"You must enter a user name for 'rsh' connections.",
					"Error", JOptionPane.ERROR_MESSAGE );
			return;
			}

		//
		// DO IT
		//
		this.getMainPanel().setAllTabsEnabled( false );

		this.client = CVSUtilities.createCVSClient( hostname, cvsPort );

		this.request = new CVSRequest();

		request.setPServer( isPServer );
		request.setUserName( userName );
		request.setHostName( client.getHostName() );

		if ( isPServer )
			{
			final String scrambled =
				CVSScramble.scramblePassword
					( new String( passWord ), 'A' );

			request.setPassword( scrambled );
			}
		else if ( connMethod == CVSRequest.METHOD_SSH )
			{
			request.setPassword( passWord );
			}

		request.setConnectionMethod( connMethod );
		request.setServerCommand
			( CVSUtilities.establishServerCommand
				( hostname, connMethod, isPServer ) );

		if ( connMethod == CVSRequest.METHOD_RSH )
			{
			CVSUtilities.establishRSHProcess( request );
			}

		request.setPort( client.getPort() );

		request.setRepository( "" );
		request.setRootDirectory( rootDirectory );
		request.setRootRepository( rootDirectory );
		request.setLocalDirectory( "" );

		request.setCommand( "noop" );

		request.includeNotifies = false;

		// This will avoid the 'Directory' reset before the noop command
		request.execInCurDir = true;
		// This will avoid the 'Directory' to the 'execInCurDir directory'
		request.setDirEntry( null );

		request.traceRequest = true;
		request.traceResponse = true;
		request.traceProcessing = true;
		request.traceTCPData = true;

		request.allowGzipFileMode =
			prefs.getBoolean( ConfigConstants.GLOBAL_ALLOWS_FILE_GZIP, true );

		request.setGzipStreamLevel
			( prefs.getInteger( ConfigConstants.GLOBAL_GZIP_STREAM_LEVEL, 0 ) );

		if ( connMethod == CVSRequest.METHOD_SSH )
			{
			// UNDONE Inform user!
			request.setGzipStreamLevel( 0 );
			request.allowGzipFileMode = false;
			}

		request.setEntries( new CVSEntryVector() );

		request.setArguments( new CVSArgumentVector() );

		request.setUserInterface( this );

		this.setWaitCursor();

		final StringBuffer outputBuf = new StringBuffer();

		CVSTracer.accumulateInBuffer( outputBuf );

		CVSTracer.setEchoAccumulation( true );

		final CVSResponse response = new CVSResponse();

		final CVSThread thread =
			new CVSThread( "TestConnect",
				this.new MyRunner( client, request, response ),
					this.new MyMonitor( request, response, outputBuf ) );

		thread.start();
		}

	private
	class		MyRunner
	implements	Runnable
		{
		private final CVSClient client;
		private final CVSRequest request;
		private final CVSResponse response;

		public
		MyRunner( final CVSClient client, final CVSRequest request, final CVSResponse response )
			{
			this.client = client;
			this.request = request;
			this.response = response;
			}

		public void
		run()
			{
			this.client.processCVSRequest( this.request, this.response );
			}
		}

	private
	class		MyMonitor
	implements	CVSThread.Monitor
		{
		private final StringBuffer buf;
		private final CVSRequest request;
		private final CVSResponse response;

		public
		MyMonitor( final CVSRequest request, final CVSResponse response, final StringBuffer buf )
			{
			this.buf = buf;
			this.request = request;
			this.response = response;
			}

		public void
		threadStarted()
			{
			actionButton.setText( "Cancel Test" );
			actionButton.setActionCommand( "CANCEL" );
			}

		public void
		threadCanceled()
			{
			}

		public void
		threadFinished()
			{
			CVSTracer.turnOffAccumulation();

			actionButton.setText( "Perform Test" );
			actionButton.setActionCommand( "TESTCONN" );

			if ( this.response.getStatus() == CVSResponse.OK )
				{
				uiDisplayProgressMsg
					( "The test was completed successfully." );
				}
			else
				{
				uiDisplayProgressMsg
					( "The test encountered an error." );
				}

			outputText.setText
				( this.buf + "\n" + response.getDisplayResults() );

			outputText.revalidate();
			outputText.repaint();

			if ( this.response != null
					&& ! this.request.saveTempFiles )
				{
				this.response.deleteTempFiles();
				}

			getMainPanel().setAllTabsEnabled( true );

			resetCursor();
			}

		}

	//
	// CVS USER INTERFACE METHODS
	//

	public void
	uiDisplayProgressMsg( final String message )
		{
		this.feedback.setText( message );
		this.feedback.repaint( 0 );
		}

	public void
	uiDisplayProgramError( final String error )
		{
		}

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

		this.info = new ConnectInfoPanel( "test" );
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

		this.actionButton = new JButton( "Perform Test" );
		this.actionButton.setActionCommand( "TESTCONN" );
		this.actionButton.addActionListener( this );
		AWTUtilities.constrain(
			this, this.actionButton,
			GridBagConstraints.NONE,
			GridBagConstraints.CENTER,
			0, row++, 1, 1, 0.0, 0.0,
			new Insets( 5, 5, 5, 5 ) );

		this.feedback = new JLabel( "Ready." );
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

