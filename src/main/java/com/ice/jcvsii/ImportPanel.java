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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import com.ice.cvsc.CVSArgumentVector;
import com.ice.cvsc.CVSClient;
import com.ice.cvsc.CVSEntry;
import com.ice.cvsc.CVSEntryVector;
import com.ice.cvsc.CVSIgnore;
import com.ice.cvsc.CVSMode;
import com.ice.cvsc.CVSProject;
import com.ice.cvsc.CVSRequest;
import com.ice.cvsc.CVSResponse;
import com.ice.cvsc.CVSScramble;
import com.ice.cvsc.CVSTracer;
import com.ice.cvsc.CVSUserInterface;
import com.ice.jcvsii.CVSThread.Monitor;
import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;


public
class		ImportPanel
extends		MainTabPanel
implements	ActionListener, CVSUserInterface
	{
	private CVSClient			client;
	protected JTextField		argumentsText;
	protected JTextField		releaseText;
	protected JTextField		vendorText;
	private JTextArea			outputText;
	protected JTextArea			messageText;
	protected JTextArea			ignoreText;
	protected JTextArea			binariesText;
	protected JCheckBox			descendCheck;
	private JLabel			feedback;
	private JButton			actionButton;
	private final StringBuffer		scanText;
	private String			ignoreName;

		private AdditionalInfoPanel	addPan;
	private ConnectInfoPanel		infoPan;


	public
	ImportPanel( final MainPanel parent )
		{
		super( parent );
		this.scanText = new StringBuffer();
		this.establishContents();
		}

	public void
	loadPreferences()
		{
		this.addPan.loadPreferences( "importadd" );
		this.infoPan.loadPreferences( "import" );
		}

	@Override
	public void
	savePreferences()
		{
		this.addPan.savePreferences( "importadd" );
		this.infoPan.savePreferences( "import" );
		}

	@Override
	public void
	actionPerformed( final ActionEvent event )
		{
		final String command = event.getActionCommand();

		if ( command.equalsIgnoreCase( "IMPORT" ) )
			{
			this.performImport();
			}
		else if ( command.equalsIgnoreCase( "CANCEL" ) )
			{
			this.cancelImport();
			}
		}

	private void
	cancelImport()
		{
		this.client.setCanceled( true );
		}

	private void
	performImport()
		{
		final Config cfg = Config.getInstance();
		final UserPrefs prefs = Config.getPreferences();
		final ResourceMgr rmgr = ResourceMgr.getInstance();

		final CVSRequest		request;
		boolean			allok = true;

		final CVSEntryVector		entries = new CVSEntryVector();
		final CVSEntryVector		binEntries = new CVSEntryVector();

		final CVSArgumentVector arguments =
			CVSArgumentVector.parseArgumentString
				( this.infoPan.getArguments() );

		final String userName = this.infoPan.getUserName();
		final String passWord = this.infoPan.getPassword();
		final String hostname = this.infoPan.getServer();
		String repository = this.infoPan.getModule();
		String rootDirectory = this.infoPan.getRepository();
		final String importDirectory = this.infoPan.getImportDirectory();

		final String vendorTag = this.addPan.getVendorTag();
		final String releaseTag = this.addPan.getReleaseTag();
		final String messageStr = this.addPan.getLogMessage();

		if (!repository.isEmpty() && repository.charAt(0) == '/')
			repository = repository.substring( 1 );

		if (!repository.isEmpty() && repository.charAt(repository.length() - 1) == '/')
			repository = repository.substring( 0, repository.length()-1 );

		if (!rootDirectory.isEmpty() && rootDirectory.charAt(rootDirectory.length() - 1) == '/')
			rootDirectory =
				rootDirectory.substring( 0, rootDirectory.length()-1 );

		final String	rootRepository = rootDirectory + '/' + repository;

		final boolean isPServer = this.infoPan.isPServer();

		final int connMethod = this.infoPan.getConnectionMethod();

		final int cvsPort =
			CVSUtilities.computePortNum
				( hostname, connMethod, isPServer );

		//
		// SANITY
		//
		if ( hostname.length() < 1 || repository.length() < 1
				|| rootDirectory.length() < 1 || vendorTag.length() < 1
				|| releaseTag.length() < 1 || messageStr.length() < 1
					|| importDirectory.length() < 1 )
			{
			final String[] fmtArgs = new String[1];
			fmtArgs[0] =
				hostname.length() < 1
					? rmgr.getUIString( "name.for.cvsserver" ) :
				repository.length() < 1
					? rmgr.getUIString( "name.for.cvsmodule" ) :
				rootDirectory.length() < 1
					? rmgr.getUIString( "name.for.cvsrepos" ) :
				importDirectory.length() < 1
					? rmgr.getUIString( "name.for.importdir" ) :
				vendorTag.length() < 1
					? rmgr.getUIString( "name.for.vendortag" ) :
				releaseTag.length() < 1
					? rmgr.getUIString( "name.for.releasetag" )
					: rmgr.getUIString( "name.for.logmsg" );

			final String msg = rmgr.getUIFormat( "import.needs.input.msg", fmtArgs );
			final String title = rmgr.getUIString( "import.needs.input.title" );
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

		this.scanText.setLength( 0 );

		this.ignoreName =
			prefs.getProperty( ConfigConstants.GLOBAL_IGNORE_FILENAME, null );

		final CVSIgnore ignore = new CVSIgnore();

		final String userIgnores =
			prefs.getProperty( ConfigConstants.GLOBAL_USER_IGNORES, null );

		if ( userIgnores != null )
			{
			ignore.addIgnoreSpec( userIgnores );
			}

		final String ignoreStr = this.addPan.getIgnores();
		if (!ignoreStr.isEmpty())
			{
			ignore.addIgnoreSpec( ignoreStr );
			}

		// We leverage the ignores mechanism to indicate binaries!
		final CVSIgnore binaries = new CVSIgnore( "" );

		final String binariesStr = this.addPan.getBinaries();
		if (!binariesStr.isEmpty())
			{
			binaries.addIgnoreSpec( binariesStr );
			}

		final boolean descend = this.addPan.isDescendChecked();

		allok =
			this.importScan
				( rootDirectory, repository, importDirectory,
					descend, entries, ignore, binEntries, binaries );

		if ( ! allok )
			{
			final String msg = rmgr.getUIString( "import.scan.error.msg" );
			final String title = rmgr.getUIString( "import.scan.error.title" );
			JOptionPane.showMessageDialog
				( this.getTopLevelAncestor(),
					msg, title, JOptionPane.ERROR_MESSAGE );
			this.outputText.setText( this.scanText.toString() );
			this.outputText.repaint( 500 );
			return;
			}

		final String serverCommand =
			CVSUtilities.establishServerCommand
				( hostname, connMethod, isPServer );

		this.client = CVSUtilities.createCVSClient( hostname, cvsPort );

		this.client.setTempDirectory( cfg.getTemporaryDirectory() );

		request = new CVSRequest();

		request.setPServer( isPServer );
		request.setUserName( userName );

		if ( isPServer )
			{
			final String scrambled =
				CVSScramble.scramblePassword( passWord, 'A' );

			request.setPassword( scrambled );
			}
		else if ( connMethod == CVSRequest.METHOD_SSH )
			{
			request.setPassword( passWord );
			}

		request.setConnectionMethod( connMethod );
		request.setServerCommand( serverCommand );

		if ( connMethod == CVSRequest.METHOD_RSH )
			CVSUtilities.establishRSHProcess( request );

		request.setPort( this.client.getPort() );
		request.setHostName( this.client.getHostName() );

		request.setRepository( repository );
		request.setRootDirectory( rootDirectory );
		request.setRootRepository( rootRepository );

		request.setLocalDirectory( importDirectory );

		request.setSetVariables
			( CVSUtilities.getUserSetVariables( this.client.getHostName() ) );

		request.setCommand( "import" );
		// NOTE DO NOT use 'sendModule' here!
		request.sendModifieds = true;
		request.sendArguments = true;
		request.includeNotifies = false;

		request.traceRequest = CVSProject.overTraceRequest;
		request.traceResponse = CVSProject.overTraceResponse;
		request.traceTCPData = CVSProject.overTraceTCP;
		request.traceProcessing = CVSProject.overTraceProcessing;

		request.allowGzipFileMode =
			prefs.getBoolean( ConfigConstants.GLOBAL_ALLOWS_FILE_GZIP, false );

		request.setGzipStreamLevel
			( prefs.getInteger( ConfigConstants.GLOBAL_GZIP_STREAM_LEVEL, 0 ) );

		arguments.appendArgument( "-m" );
		arguments.appendArgument( messageStr );

		arguments.appendArgument( repository );

		arguments.appendArgument( vendorTag );

		arguments.appendArgument( releaseTag );

		request.setEntries( entries );

		request.setArguments( arguments );

		request.setUserInterface( this );

		final CVSResponse response = new CVSResponse();

		final Thread thread =
			new CVSThread( "Import",
				this.new MyRunner
						( this.client, request, response, binEntries ),
					this.new MyMonitor( request, response ) );

		thread.start();
		}

	private final
	class		MyRunner
	implements	Runnable
		{
		private final CVSClient client;
		private CVSProject project;
		private final CVSRequest request;
		private final CVSResponse response;
		private final CVSEntryVector binEntries;


		private MyRunner(final CVSClient client, final CVSRequest request,
				 final CVSResponse response, final CVSEntryVector binEntries)
			{
			this.client = client;
			this.request = request;
			this.response = response;
			this.binEntries = binEntries;
			}

		@Override
		public void
		run()
			{
			this.client.processCVSRequest( this.request, this.response );

			this.response.appendStderr( scanText.toString() );

			boolean success =
				response.getStatus() == CVSResponse.OK;

			if (!this.binEntries.isEmpty())
				{
				final CVSResponse binResponse = new CVSResponse();

				this.request.setEntries( this.binEntries );

				this.request.getArguments().insertElementAt( "-kb", 0 );

				client.processCVSRequest( this.request, binResponse );

				if ( binResponse.getStatus() != CVSResponse.OK )
					success = false;

				this.response.appendStdout
					( "\n\n--------- "
						+ ResourceMgr.getInstance().getUIString
							( "name.for.binary.files" )
						+ " ---------\n" );

				this.response.appendStdout
					( binResponse.getDisplayResults() );

				if ( ! this.request.saveTempFiles )
					{
					binResponse.deleteTempFiles();
					}
				}

			if ( success )
				{
				uiDisplayProgressMsg
					( ResourceMgr.getInstance().getUIString
					( "import.perform.label" ) );
				}
			else
				{
				uiDisplayProgressMsg
					( ResourceMgr.getInstance().getUIString
						( "import.status.failure" ) );
				}

			if ( ! this.request.saveTempFiles )
				{
				this.response.deleteTempFiles();
				}
			}
		}

	private final
	class		MyMonitor
	implements	Monitor
		{
		private final CVSRequest request;
		private final CVSResponse response;

		private MyMonitor(final CVSRequest request, final CVSResponse response)
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
					( "import.cancel.label" ) );
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
					( "import.perform.label" ) );

			final String resultStr = this.response.getDisplayResults();

			if ( this.response.getStatus() == CVSResponse.OK )
				{
				uiDisplayProgressMsg
					( ResourceMgr.getInstance().getUIString
						( "import.status.success" ) );
				}
			else
				{
				uiDisplayProgressMsg
					( ResourceMgr.getInstance().getUIString
						( "import.status.failure" ) );
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

	private boolean
	importScan(
			final String repository, final String module, final String importPath,
			final boolean descend, final CVSEntryVector entries, final CVSIgnore ignore,
			final CVSEntryVector binEntries, final CVSIgnore binaries)
		{
		boolean result = true;

		final File dirFile = new File( importPath );

		if ( ! dirFile.exists() )
			{
			result = false;
			final String[] fmtArgs = { dirFile.getPath() };
			this.scanText.append
				( ResourceMgr.getInstance().getUIFormat
						( "import.scan.dir.doesnotexist", fmtArgs ) );
			this.scanText.append("   ").append(ResourceMgr.getInstance().getUIString
					("import.scan.aborted"));
			}
		else if ( ! dirFile.isDirectory() )
			{
			result = false;
			final String[] fmtArgs = { dirFile.getPath() };
			this.scanText.append
				( ResourceMgr.getInstance().getUIFormat
						( "import.scan.dir.notdir", fmtArgs ) );
			this.scanText.append("   ").append(ResourceMgr.getInstance().getUIString
					("import.scan.aborted"));
			}
		else
			{
			result = this.importScanDescend
				( repository, module, "", dirFile,
					descend, entries, ignore, binEntries, binaries );
			}

		return result;
		}

	/**
	 * Descends a local source tree looking for files to
	 * be imported in this command.
	 *
	 * @param repository The repository's root directory.
	 * @param module The 'single' module name of the repository
	 * @param subModule is the 'aliased' module name.
	 *     subModule is only different from 'module' in the case
	 *     of aliases, and is the alias's path. This allows
	 *     us to deal with an alias 'jcvs com/ice/jcvs' in
	 *     that module will be 'jcvs' and subModule 'com/ice/jcvs'.
	 * @param localDirectory The 'local-directory' of imported entries.
	 * @param dirFile The current import directory's 'File'.
	 * @param descend Determines if the scanning descend into subdirectories.
	 * @param entries The CVSEntryVector in which to place the imported entries.
	 * @param ignore The globals ignores.
	 */

	private boolean
	importScanDescend(
			final String repository, final String module,
			final String localDirectory, final File dirFile, final boolean descend,
			final CVSEntryVector entries, final CVSIgnore ignore,
			final CVSEntryVector binEntries, final CVSIgnore binaries )
		{
		boolean result = true;
		final String[] contents = dirFile.list();

		if ( contents == null )
			{
			// REVIEW Why does this occur?!
			return true;
			}

		CVSIgnore	dirIgnore = null;

		if ( false )
		CVSTracer.traceIf( true,
			"ImportScanDescend: \n"
			+ "   Repository     '" + repository + "'\n"
			+ "   Module         '" + module + "'\n"
			+ "   localDirectory '" + localDirectory + "'\n"
			+ "   dirFile (path) '" + dirFile.getPath() + "'\n"
			+ "   descend        '" + descend + "'\n" );

		File	ignFile;
		// TODO should I have a loop here and a space separated property?!
		// This would allow for multiple ignore file possibilities.

		ignFile = new File( dirFile, this.ignoreName );
		if ( ignFile.exists() )
			{
			dirIgnore = new CVSIgnore( "" );
			dirIgnore.addIgnoreFile( ignFile );
			CVSTracer.traceIf( false,
				"ImportDescend: DIRECTORY IGNORE '" + ignFile.getPath()
				+ "' added '" + dirIgnore.size() + "' ignores." );
			}

		ignFile = new File( dirFile, ".cvsignore" );
		if ( ignFile.exists() )
			{
			dirIgnore = new CVSIgnore( "" );
			dirIgnore.addIgnoreFile( ignFile );
			CVSTracer.traceIf( false,
				"ImportDescend: DIRECTORY IGNORE '" + ignFile.getPath()
				+ "' added '" + dirIgnore.size() + "' ignores." );
			}

		for ( int i = 0 ; result && i < contents.length ; ++i )
			{
			final String	fileName = contents[i];

			final File file = new File( dirFile, fileName );

			CVSTracer.traceIf( false,
					   "ImportDescend[" + i + "] fileName '"
					   + fileName + "' isDir '"
					   + file.isDirectory() + "' filePath '"
					   + file.getPath() + '\'');

			if ( fileName.equals( this.ignoreName ) )
				continue;
			if ( fileName.equals( ".cvsignore" ) )
				continue;

			if ( ignore.isFileToBeIgnored( fileName )
				|| dirIgnore != null
						&& dirIgnore.isFileToBeIgnored( fileName ) )
				{
				CVSTracer.traceIf( false,
						   "ImportDescend[" + i + "] IGNORE '" + fileName + '\'');

				this.scanText.append("I ").append(localDirectory).append(fileName).append('\n');

				continue;
				}

			if ( file.isDirectory() )
				{
				final String newLocal =
						localDirectory + fileName + '/';

				if ( false )
				CVSTracer.traceIf( true,
						   "ImportDescend[" + i + "] DIRECTORY\n"
						   + "  newLocal '" + newLocal + "'\n"
						   + "  newDir   '" + file.getPath() + '\'');

				if ( descend )
					{
					result =
						this.importScanDescend
							( repository, module, newLocal, file, descend,
								entries, ignore, binEntries, binaries );
					}
				}
			else
				{
				final CVSEntry entry = new CVSEntry();

				final String modPath = module + '/' + localDirectory;

				String localDir = localDirectory;
				if (localDir.isEmpty())
					localDir = "./";

				String reposPath = repository + '/' + module;
				if (!localDirectory.isEmpty())
					{
					reposPath = reposPath + '/' +
						    localDirectory.substring
							( 0, localDirectory.length() - 1 );
					}

				entry.setName( fileName );
				entry.setLocalDirectory( localDir );
				entry.setRepository( reposPath );
				entry.setMode( new CVSMode() );
				entry.setNewUserFile( true );

				if ( false )
				CVSTracer.traceIf( true,
						   "ImportDescend[" + i + "] ENTRY\n"
						   + "  name '" + entry.getName() + "'\n"
						   + "  fullName '" + entry.getFullName() + "'\n"
						   + "  localDir '" + entry.getLocalDirectory() + "'\n"
						   + "  repos   '" + entry.getRepository() + '\'');

				if ( binaries.isFileToBeIgnored( fileName )	)
					{
					entry.setOptions( "-kb" );
					binEntries.appendEntry( entry );
					}
				else
					{
					entries.appendEntry( entry );
					}
				}
			}

		return result;
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

		final ResourceMgr rmgr = ResourceMgr.getInstance();

		this.setLayout( new GridBagLayout() );

			final JTabbedPane tabbed = new JTabbedPane();

		this.infoPan = new ConnectInfoPanel( "import" );
		this.infoPan.setPServerMode( true );
		this.infoPan.setUsePassword( true );

		tabbed.addTab
			( rmgr.getUIString( "import.tab.connection" ),
				null, this.infoPan );

		this.addPan = new AdditionalInfoPanel();
		tabbed.addTab
			( rmgr.getUIString( "import.tab.additional" ),
				null, this.addPan );

		// ============== INPUT FIELDS PANEL ================

		int row = 0;

		final JSeparator sep;

		AWTUtilities.constrain(
				this, tabbed,
				GridBagConstraints.HORIZONTAL,
				GridBagConstraints.WEST,
				0, row++, 1, 1, 1.0, 0.0 );

		this.actionButton =
			new JButton( rmgr.getUIString( "import.perform.label" ) );
		this.actionButton.setActionCommand( "IMPORT" );
		this.actionButton.addActionListener( this );
		AWTUtilities.constrain(
			this, this.actionButton,
			GridBagConstraints.NONE,
			GridBagConstraints.CENTER,
			0, row++, 1, 1, 0.0, 0.0,
			new Insets( 5, 5, 5, 5 ) );

		this.feedback =
			new JLabel( rmgr.getUIString( "name.for.ready" ) );
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

	private static final
	class		AdditionalInfoPanel
	extends		JPanel
		{
		private final JTextArea		ignores;
		private final JTextArea		binaries;
		private final JTextArea		logmsg;
		private final JTextField		vendor;
		private final JTextField		release;
		private final JCheckBox		descend;

		String
		getIgnores()
			{
			return this.ignores.getText();
			}

		void
		setIgnores(final String ignoreText)
			{
			this.ignores.setText( ignoreText );
			}

		String
		getBinaries()
			{
			return this.binaries.getText();
			}

		void
		setBinaries(final String binText)
			{
			this.binaries.setText( binText );
			}

		String
		getLogMessage()
			{
			return this.logmsg.getText();
			}

		void
		setLogMessage(final String logText)
			{
			this.logmsg.setText( logText );
			}

		String
		getVendorTag()
			{
			return this.vendor.getText();
			}

		void
		setVendorTag(final String tag)
			{
			this.vendor.setText( tag );
			}

		String
		getReleaseTag()
			{
			return this.release.getText();
			}

		void
		setReleaseTag(final String tag)
			{
			this.release.setText( tag );
			}

		boolean
		isDescendChecked()
			{
			return this.descend.isSelected();
			}

		void
		loadPreferences(final String panName)
			{
			final UserPrefs prefs = Config.getPreferences();

			this.setIgnores
				( prefs.getProperty
					(panName + '.' + ConfigConstants.IMPADDPAN_IGNORES, "" ) );
			this.setBinaries
				( prefs.getProperty
					(panName + '.' + ConfigConstants.IMPADDPAN_BINARIES, "" ) );

			this.setLogMessage
				( prefs.getProperty
					(panName + '.' + ConfigConstants.IMPADDPAN_LOGMSG, "" ) );

			this.setVendorTag
				( prefs.getProperty
					(panName + '.' + ConfigConstants.IMPADDPAN_VENDOR_TAG, "" ) );
			this.setReleaseTag
				( prefs.getProperty
					(panName + '.' + ConfigConstants.IMPADDPAN_RELEASE_TAG, "" ) );
			}

		void
		savePreferences(final String panName)
			{
			final UserPrefs prefs = Config.getPreferences();

			prefs.setProperty
				(panName + '.' + ConfigConstants.IMPADDPAN_IGNORES,
					this.getIgnores() );
			prefs.setProperty
				(panName + '.' + ConfigConstants.IMPADDPAN_BINARIES,
					this.getBinaries() );

			prefs.setProperty
				(panName + '.' + ConfigConstants.IMPADDPAN_LOGMSG,
					this.getLogMessage() );

			prefs.setProperty
				(panName + '.' + ConfigConstants.IMPADDPAN_VENDOR_TAG,
					this.getVendorTag() );
			prefs.setProperty
				(panName + '.' + ConfigConstants.IMPADDPAN_RELEASE_TAG,
					this.getReleaseTag() );
			}

		private AdditionalInfoPanel()
			{
			super();
			this.setLayout( new GridLayout( 2, 2, 4, 4 ) );
			final ResourceMgr rmgr = ResourceMgr.getInstance();

			final Container tagPan = new JPanel();
			tagPan.setLayout( new GridBagLayout() );

			int row = 0;

			this.descend =
				new JCheckBox
					( rmgr.getUIString( "import.subdir.checkbox.label" ) );

			this.descend.setSelected( true );
			AWTUtilities.constrain(
				tagPan, this.descend,
				GridBagConstraints.NONE,
				GridBagConstraints.CENTER,
				0, row++, 2, 1, 1.0, 0.0,
				new Insets( 1, 3, 1, 3 ) );

			AWTUtilities.constrain(
				tagPan, new JLabel( rmgr.getUIString( "import.vendortag.label" ) ),
				GridBagConstraints.NONE,
				GridBagConstraints.WEST,
				0, row, 1, 1, 0.0, 0.0,
				new Insets( 0, 3, 1, 0 ) );

			this.vendor = new JTextField();
			AWTUtilities.constrain(
				tagPan, this.vendor,
				GridBagConstraints.HORIZONTAL,
				GridBagConstraints.CENTER,
				1, row++, 1, 1, 1.0, 0.0,
				new Insets( 3, 3, 3, 3 ) );

			AWTUtilities.constrain(
				tagPan,
				new JLabel
					( rmgr.getUIString( "import.releasetag.label" ) ),
				GridBagConstraints.NONE,
				GridBagConstraints.WEST,
				0, row, 1, 1, 0.0, 0.0,
				new Insets( 0, 3, 1, 0 ) );

			this.release = new JTextField();
			AWTUtilities.constrain(
				tagPan, this.release,
				GridBagConstraints.HORIZONTAL,
				GridBagConstraints.CENTER,
				1, row++, 1, 1, 1.0, 0.0,
				new Insets( 1, 3, 3, 3 ) );

			this.logmsg = new JTextArea();
			final JComponent logPan = new JPanel();
			logPan.setLayout( new BorderLayout() );
			logPan.add( BorderLayout.CENTER, this.logmsg );
			logPan.setBorder
				( new TitledBorder
					( new EtchedBorder
						( EtchedBorder.RAISED ),
						rmgr.getUIString( "import.logmsg.label" ) ) );

			this.ignores = new JTextArea();
			final JComponent ignPan = new JPanel();
			ignPan.setLayout( new BorderLayout() );
			ignPan.add( BorderLayout.CENTER, this.ignores );
			ignPan.setBorder
				( new TitledBorder
					( new EtchedBorder
						( EtchedBorder.RAISED ),
						rmgr.getUIString( "import.ignores.label" ) ) );

			this.binaries = new JTextArea();
			final JComponent binPan = new JPanel();
			binPan.setLayout( new BorderLayout() );
			binPan.add( BorderLayout.CENTER, this.binaries );
			binPan.setBorder
				( new TitledBorder
					( new EtchedBorder
						( EtchedBorder.RAISED ),
						rmgr.getUIString( "import.binaries.label" ) ) );

			this.add( tagPan );
			this.add( logPan );
			this.add( ignPan );
			this.add( binPan );
			}
		}

	}

