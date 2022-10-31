/*
** Java cvs client application package.
** Copyright (c) 1997-2002 by Timothy Gerard Endres
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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MenuShortcut;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.ice.cvsc.CVSArgumentVector;
import com.ice.cvsc.CVSCUtilities;
import com.ice.cvsc.CVSClient;
import com.ice.cvsc.CVSEntry;
import com.ice.cvsc.CVSEntryVector;
import com.ice.cvsc.CVSIgnore;
import com.ice.cvsc.CVSLog;
import com.ice.cvsc.CVSProject;
import com.ice.cvsc.CVSRequest;
import com.ice.cvsc.CVSResponse;
import com.ice.cvsc.CVSTracer;
import com.ice.cvsc.CVSUserInterface;
import com.ice.jcvsii.CVSThread.Monitor;
import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;
import com.ice.util.StringUtilities;


/**
 * This is the frame that implements the 'Project Window' in jCVS.
 * This frame will display the project's icon list, the arguments
 * text area, the user feedback display area, and a series of menus.
 * The primary unit of display in this class is a CVSProject.
 *
 * @version $Id: ProjectFrame.java,v 1.12 2003/07/27 04:53:06 time Exp $
 * @author Timothy Gerard Endres, <a href="mailto:time@ice.com">time@ice.com</a>.
 *
 */

//
// CONTRIBUTIONS
//
// SK-unknown by Sherali Karimov <sherali.karimov@proxima-tech.com>
// This code implements a dialog that displays the unmanaged (unknown)
// files and allows the user to delete or add them to the project.
//

public final
class		ProjectFrame
extends		JFrame
implements	ActionListener, CVSUserInterface
	{
	public static final String		RCS_ID = "$Id: ProjectFrame.java,v 1.12 2003/07/27 04:53:06 time Exp $";
	public static final String		RCS_REV = "$Revision: 1.12 $";

	private CVSProject		project;
	private OutputFrame		output;
	private UserPrefs		prefs;

	private CVSEntryVector	entries;
	private CVSEntryVector	popupEntries;

	private String			displayStdout;
	private String			displayStderr;

	private String			lastUserFileDir;

		private JPanel			argumentsPan;
	private JTextArea		argumentText;

	private JLabel			feedback;

	private EntryPanel		entryPanel;

	private JMenuBar		mBar;

		private JCheckBoxMenuItem	traceCheckItem;

		private boolean			traceReq;
	private boolean			traceResp;
	private boolean			traceProc;
	private boolean			traceTCP;

	/**
	 * We set this to true when we release so that we do not try to
	 * save the preferences, which will fail because 'CVS/' is gone.
	 */
	private boolean			releasingProject;

	private boolean			prettyDiffs;


	private ProjectFrame(final String title, final CVSProject project)
		{
		super( title );

		this.initialize( project );

		this.establishMenuBar();

		this.establishContents();

		this.loadPreferences();

		this.show();

		this.addWindowListener
			(
			new WindowAdapter()
				{
				@Override
				public void
					windowClosing( final WindowEvent e )
						{ dispose(); }

				@Override
				public void
					windowClosed( final WindowEvent e )
						{ windowBeingClosed(); }
				}
			);
		}

	private void
	initialize( final CVSProject project )
		{
		this.project = project;
		this.output = null;

		this.entries = null;
		this.popupEntries = null;

			final boolean briefArgs = true;

		this.feedback = null;
		this.argumentsPan = null;
			final Cursor saveCursor = null;
		this.displayStdout = "";
		this.displayStderr = "";
		this.lastUserFileDir = null;
		this.releasingProject = false;

		final Config cfg = Config.getInstance();

		this.prefs =
			new UserPrefs( project.getRepository(), cfg.getPrefs() );

		this.prefs.setPropertyPrefix( "jcvsii." );

		cfg.loadProjectPreferences( project, this.prefs );

		this.traceReq =
			this.prefs.getBoolean( ConfigConstants.GLOBAL_CVS_TRACE_ALL, false );

		this.traceResp = this.traceReq;
		this.traceProc = this.traceReq;
		this.traceTCP = this.traceReq;
		}

	private void
	windowBeingClosed()
		{
		ProjectFrameMgr.removeProject
			( this, this.project.getLocalRootPath() );

		this.savePreferences();

		if ( this.output != null )
			{
			// NOTE We are forced to savePreferences() here, since
			//      the dispose() will not fire the windowClosed()
			//      event until we are out of here, and by that time
			//      the preferences are saved and it is too late!
			//
			this.output.savePreferences();
			this.output.dispose();
			this.output = null;
			}

		if ( ! this.releasingProject )
			{
			Config.getInstance().saveProjectPreferences
				( this.project, this.prefs );
			}
		}

	public UserPrefs
	getPreferences()
		{
		return this.prefs;
		}

	private void
	loadPreferences()
		{
		this.entryPanel.loadPreferences( this.prefs );

		final Rectangle bounds =
			this.prefs.getBounds
				( ConfigConstants.PROJECT_WINDOW_BOUNDS,
					new Rectangle( 20, 40, 525, 440 ) );

		this.setBounds( bounds );
		}

	private void
	savePreferences()
		{
		final Rectangle bounds = this.getBounds();

		if ( bounds.x >= 0 && bounds.y >= 0
				&& bounds.width > 0 && bounds.height > 0 )
			{
			this.prefs.setBounds
				( ConfigConstants.PROJECT_WINDOW_BOUNDS, bounds );
			}

		this.entryPanel.savePreferences( this.prefs );
		}

	@Override
	public void
	actionPerformed( final ActionEvent evt )
        {
		String	token;
	    String	command = evt.getActionCommand();

		if ( command.startsWith( "POPUP:" ) )
			{
			command = command.substring( 6 );
			this.popupEntries =
				(CVSEntryVector) evt.getSource();
			}

		// Check for the simple 'one command' special case...
		final int index = command.indexOf( '&' );
		if ( index < 0 )
			{
			this.performActionLine( command, evt );
			}
		else
			{
			final StringTokenizer toker =
				new StringTokenizer( command, "&" );

			// UNDONE
			// Should have a "JCVS:AskYesNo:Prompt..." command, that
			// will terminate a multi-command command if 'No' is selected.
			// Also some "generic" argument spec, for instance, a way to
			// say 'make sure a tag release' is provided, or 'make sure
			// there are three arguments that are not options (-)'.
			//
			for ( ; toker.hasMoreTokens() ; )
				{
				try { token = toker.nextToken(); }
				catch ( final NoSuchElementException ex )
					{
					break;
					}

				if ( token == null || token.length() < 1 )
					{
					break;
					}

				this.performActionLine( token, evt );
				}
			}

		this.popupEntries = null;
        }

	private void
	performActionLine(final String command, final ActionEvent event)
		{
		final String	subCmd;

		if ( command.startsWith( "CVS:" ) )
			{
			subCmd = command.substring( 4 );

			this.prettyDiffs = false;
			this.performCVSCommand( subCmd );
			}
		else if ( command.startsWith( "JCVS:" ) )
			{
			subCmd = command.substring( 5 );

			this.performJCVSCommand( subCmd );
			}
		else if ( command.equalsIgnoreCase( "Close" ) )
			{
			this.performJCVSCommand( "Close" );
			}
		}

	private void
	performJCVSCommand(final String command)
		{
		final int		i;
			final int count;
			final boolean	result = true;

		if ( command.startsWith( "FMSG:" ) )
			{
			final String message = command.substring( 5 );
			this.showFeedback( message );
			}
		else if ( command.startsWith( "NOTE:" ) )
			{
			final String message = command.substring( 5 );
			CVSUserDialog.Note( this, message );
			}
		else if ( command.startsWith( "ERROR:" ) )
			{
			final String message = command.substring( 6 );
			CVSUserDialog.Error( this, message );
			}
		else if ( command.startsWith( "PDIFF:" ) )
			{
			final String subCmd = command.substring( 6 );
			this.prettyDiffs = true;
			this.performCVSCommand( subCmd );
			}
		else if ( command.equals( "TRACE" ) )
			{
			SwingUtilities.invokeLater
				( () ->
						{
						if ( traceCheckItem.getState() )
							{
							traceReq = true;
							traceResp = true;
							traceProc = true;
							traceTCP = true;
							}
						else
							{
							traceReq = false;
							traceResp = false;
							traceProc = false;
							traceTCP = false;
							}
						}
				);
			}
		else if ( command.startsWith( "DisplayUnkFiles:" ) )
			{
			final String subCmd = command.substring( "DisplayUnkFiles:".length() );
			this.processUnknownFiles( subCmd );
			}
		else if ( command.equalsIgnoreCase( "DisplayUnkDirs" ) )
			{
			this.processUnknownDirs();
			}
		else if ( command.equalsIgnoreCase( "Close" ) )
			{
			SwingUtilities.invokeLater(this::dispose);
			}
		else if ( command.equalsIgnoreCase( "HideOutputWindow" ) )
			{
			if ( this.output != null )
				{
				SwingUtilities.invokeLater
					( () -> output.setVisible( false )
					);
				}
			}
		else if ( command.equalsIgnoreCase( "CloseOutputWindow" ) )
			{
			if ( this.output != null )
				{
				this.output.dispose();
				this.output = null;
				}
			}
		else if ( command.equalsIgnoreCase( "ShowOutputWindow" ) )
			{
			ensureOutputAvailable();

			if ( this.output != null )
				{
				this.output.show();
				this.output.toFront();
				this.output.requestFocus();
				}
			}
		else if ( command.equalsIgnoreCase( "OpenAllEntries" ) )
			{
			this.openAllEntries();
			}
		else if ( command.equalsIgnoreCase( "CloseAllEntries" ) )
			{
			this.closeAllEntries();
			}
		else if ( command.equalsIgnoreCase( "SelectNoEntries" ) )
			{
			this.selectNoEntries();
			}
		else if ( command.equalsIgnoreCase( "SelectAllEntries" ) )
			{
			this.selectAllEntries();
			}
		else if ( command.equalsIgnoreCase( "SelectModifiedEntries" ) )
			{
			this.selectModifiedEntries();
			}
		else if ( command.equalsIgnoreCase( "ShowDetails" ) )
			{
			SwingUtilities.invokeLater(this::displayProjectDetails);
			}
		else if ( command.equalsIgnoreCase( "ClearResults" ) )
			{
			this.ensureOutputAvailable();
			if ( this.output != null )
				{
				this.output.setText( "" );
				}
			}
		else if ( command.equalsIgnoreCase( "ClearArgText" ) )
			{
			SwingUtilities.invokeLater(this::clearArgumentsText);
			}
		else if ( command.equalsIgnoreCase( "PerformLogin" ) )
			{
			SwingUtilities.invokeLater(this::performLogin);
			}
		else if ( command.equalsIgnoreCase( "AddToWorkBench" ) )
			{
			SwingUtilities.invokeLater(this::addToWorkBench);
			}
		else if ( command.equalsIgnoreCase( "ExpandBelow" ) )
			{
			final TreePath[] selPaths = this.entryPanel.getSelectionPaths();
			if ( selPaths != null )
				{
				for ( final TreePath selPath : selPaths )
					{
					final EntryNode node = (EntryNode)
						selPath.getLastPathComponent();
					if ( ! node.isLeaf() )
						{
						this.entryPanel.expandAll( node );
						this.entryPanel.clearSelection( selPath );
						}
					}
				}
			}
		else if ( command.equalsIgnoreCase( "SelectBelow" ) )
			{
			final TreePath[] selPaths = this.entryPanel.getSelectionPaths();
			if ( selPaths != null )
				{
				for ( final TreePath selPath : selPaths )
					{
					final EntryNode node = (EntryNode)
						selPath.getLastPathComponent();
					if ( ! node.isLeaf() )
						{
						this.entryPanel.selectAll( node );
						this.entryPanel.clearSelection( selPath );
						}
					}
				}
			}
		else if ( command.startsWith( "OPEN:" ) )
			{
			final int selector =
				CVSRequest.parseEntriesSelector( command.charAt(5) );

			String verb = "edit";
			if ( command.length() > 7 )
				{
				verb = command.substring( 7 );
				}

			final CVSEntryVector entries =
				this.getEntriesToActUpon( selector );

			for ( int eIdx = 0 ; entries != null
					&& eIdx < entries.size() ; ++eIdx )
				{
				final CVSEntry entry = entries.entryAt(eIdx);
				if ( entry != null )
					{
					final File entryFile =
						this.project.getLocalEntryFile( entry );

					JAFUtilities.openFile
						( entry.getName(), entryFile, verb );
					}
				else
					{
					new Throwable
						("NULL ENTRY[" + eIdx + "] on command '" + command + '\'').
							printStackTrace();
					}
				}
			}
		else if ( command.startsWith( "MOVE:" ) )
			{
			final String backupPattern = command.substring(7);

			final int selector =
				CVSRequest.parseEntriesSelector( command.charAt(5) );

			final CVSEntryVector entries =
				this.getEntriesToActUpon( selector );

			for ( int eIdx = 0 ; entries != null
					&& eIdx < entries.size() ; ++eIdx )
				{
				final CVSEntry entry = entries.entryAt(eIdx);
				if ( entry != null )
					{
					final File entryFile =
						this.project.getLocalEntryFile( entry );

					if ( ! CVSUtilities.renameFile( entryFile, backupPattern, true ) )
						{
						final String[] fmtArgs = { entryFile.getPath() };
						final String msg = ResourceMgr.getInstance().getUIFormat
							( "project.rename.failed.msg", fmtArgs );
						final String title = ResourceMgr.getInstance().getUIString
							( "project.rename.failed.title" );
						JOptionPane.showMessageDialog
							( this, msg, title, JOptionPane.ERROR_MESSAGE );
						break;
						}
					}
				else
					{
					new Throwable
						("NULL ENTRY[" + eIdx + "] on command '" + command + '\'').
							printStackTrace();
					}
				}
			}
		else if ( command.startsWith( "COPY:" ) )
			{
			final String copyPattern = command.substring(7);

			final int selector =
				CVSRequest.parseEntriesSelector( command.charAt(5) );

			final CVSEntryVector entries =
				this.getEntriesToActUpon( selector );

			for ( int eIdx = 0 ; entries != null
					&& eIdx < entries.size() ; ++eIdx )
				{
				final CVSEntry entry = entries.entryAt(eIdx);
				if ( entry != null )
					{
					final File entryFile =
						this.project.getLocalEntryFile( entry );

					if ( ! CVSUtilities.copyFile( entryFile, copyPattern ) )
						{
						final String[] fmtArgs = { entryFile.getPath() };
						final String msg = ResourceMgr.getInstance().getUIFormat
							( "project.copy.failed.msg", fmtArgs );
						final String title = ResourceMgr.getInstance().getUIString
							( "project.copy.failed.title" );
						JOptionPane.showMessageDialog
							( this, msg, title, JOptionPane.ERROR_MESSAGE );
						break;
						}
					}
				else
					{
					new Throwable
						("NULL ENTRY[" + eIdx + "] on command '" + command + '\'').
							printStackTrace();
					}
				}
			}
		else if ( command.startsWith( "CMDLINE:" ) )
			{
			final String subCmd = command.substring( 8 );
			this.performCVSCommandLine( subCmd );
			}
		else if ( command.startsWith( "ADDDIR:" ) )
			{
			final String subCmd = command.substring( 8 );
			this.performAddDirectory( subCmd );
			}

		}

	private void
	addToWorkBench()
		{
		JCVS.getMainFrame().addProjectToWorkBench( this.project );
		}

	private void
	displayProjectDetails()
		{
		final String type =
			Config.getPreferences().getProperty
				( ConfigConstants.PROJECT_DETAILS_TYPE, "text/plain" );

		if ( type.equalsIgnoreCase( "text/html" ) )
			{
			this.displayProjectDetailsHTML();
			}
		else
			{
			this.displayProjectDetailsPlain();
			}
		}

	private void
	displayProjectDetailsHTML()
		{
		final Object[] fmtArgs =
			{
			this.project.getRepository(),
			this.project.getRootDirectory(),
			this.project.getClient().getHostName(),
			this.project.getClient().getPort(),
			this.project.getLocalRootDirectory()
			};

		final String msgStr =
			ResourceMgr.getInstance().getUIFormat
				( "project.details.dialog.html", fmtArgs );
		final String title =
			ResourceMgr.getInstance().getUIString
				( "project.details.dialog.title" );

		new HTMLDialog( this, title, true, msgStr ).show();
		}

	private void
	displayProjectDetailsPlain()
		{
		final Object[] fmtArgs =
			{
			this.project.getRepository(),
			this.project.getRootDirectory(),
			this.project.getClient().getHostName(),
			this.project.getClient().getPort(),
			this.project.getLocalRootDirectory()
			};

		final String msgStr =
			ResourceMgr.getInstance().getUIFormat
				( "project.details.dialog.text", fmtArgs );
		final String title =
			ResourceMgr.getInstance().getUIString
				( "project.details.dialog.title" );

		JOptionPane.showMessageDialog
			( this, msgStr, title, JOptionPane.INFORMATION_MESSAGE );
		}

	private synchronized void
	showFeedback(final String message)
		{
		this.feedback.setText( message );
		this.feedback.repaint( 0 );
		}

	private void
	verifyLogin()
		{
		if ( ! this.project.isPServer()
				&& ! this.project.isSSHServer() )
			return;

		final boolean valid;
		final String password = this.project.getPassword();

		if ( password == null )
			{
			this.performLogin();
			}
		}

	private void
	performLogin()
		{
		if ( ! this.project.isPServer()
				&& ! this.project.isSSHServer() )
			return;

		final String password;
		String userName = this.project.getUserName();

		final PasswordDialog passDialog =
			new PasswordDialog( this, userName );

		passDialog.show();

		userName = passDialog.getUserName();
		password = passDialog.getPassword();

		if ( userName != null && password != null )
			{
			this.setWaitCursor();

			final boolean valid =
				this.project.verifyPassword
					( this, userName, password, this.traceReq );

			this.resetCursor();

			if ( ! valid )
				{
				final String[] fmtArgs = { userName };
				final String msg = ResourceMgr.getInstance().getUIFormat
					( "project.login.failed.msg", fmtArgs );
				final String title = ResourceMgr.getInstance().getUIString
					( "project.login.failed.title" );
				JOptionPane.showMessageDialog
					( this, msg, title, JOptionPane.ERROR_MESSAGE );
				}
			}
		}

	public void
	performCheckOut( final String checkOutCommand )
		{
		final boolean result =
			this.commonCVSCommand( checkOutCommand, null, null );

		if ( result )
			{
			this.project.writeAdminFiles();
			}
		else
			{
			final String msg = ResourceMgr.getInstance().getUIString
				( "project.checkout.failed.msg" );
			final String title = ResourceMgr.getInstance().getUIString
				( "project.checkout.failed.title" );
			JOptionPane.showMessageDialog
				( this, msg, title, JOptionPane.ERROR_MESSAGE );
			}
		}

	private void
	performAddDirectory(final String commandSpec)
		{
		final CVSResponse		result;

		final String prompt =
			ResourceMgr.getInstance().getUIString
				( "global.directory.name.prompt" );

		final String dirPath = JOptionPane.showInputDialog( prompt );

		if ( dirPath != null )
			{
			final CVSEntryVector entries =
				this.getEntriesToActUpon( CVSRequest.ES_POPUP );

			if ( entries != null && !entries.isEmpty())
				{
				final StringBuilder addPath = new StringBuilder();
				final CVSEntry dirEntry = entries.entryAt(0);

				// The root entry has a 'bad' fullname.
				// UNDONE
				// REVIEW
				// Should entries have a "isRoot" flag, and
				// make this adjustment for me in getFullName()?
				//

				if ( dirEntry == this.project.getRootEntry() )
					addPath.append(dirEntry.getLocalDirectory()).append(dirPath);
				else
					addPath.append(dirEntry.getFullName()).append('/').append(dirPath);

				if ( !(!addPath.toString().isEmpty() && addPath.toString().charAt(addPath.toString().length() - 1) == '/'))
					{
					addPath.append('/');
					}
				addPath.append('.');

				result =
					this.project.ensureRepositoryPath
						( this, addPath.toString(), new CVSResponse() );

				if ( result.getStatus() != CVSResponse.OK )
					{
					final String[] fmtArgs =
						{
						dirPath,
						result.getStderr(),
						result.getStdout()
						};

					final String msg = ResourceMgr.getInstance().getUIFormat
						( "project.diradd.failed.msg", fmtArgs );
					final String title = ResourceMgr.getInstance().getUIString
						( "project.diradd.failed.title" );
					JOptionPane.showMessageDialog
						( this, msg, title, JOptionPane.ERROR_MESSAGE );
					}
				else
					{
					this.showFeedback( "Updating entries list..." );
				//	this.entryPanel.syncTreeEntries
				//		( this.project.getRootEntry() );
					this.project.writeAdminFiles();
					this.showFeedback( "Done." );
					this.entryPanel.repaint( 500 );
					}
				}
			else
				{
				new Throwable( "The entries list is EMPTY!!!" ).
					printStackTrace();
				}
			}
		}

	private void
	performCVSCommandLine(final String commandSpec)
		{
		String command = null;

		final String prompt =
			ResourceMgr.getInstance().getUIString
				( "project.cvs.command.prompt" );

		final String commandLine =
			JOptionPane.showInputDialog( prompt );

		if ( commandLine != null )
			{
			final CVSArgumentVector arguments =
				CVSArgumentVector.parseArgumentString( commandLine );

			if (!arguments.isEmpty())
				{
				command = (String) arguments.elementAt(0);

				arguments.removeElementAt(0);

				this.commonCVSCommand
					(command + ':' + commandSpec, null, arguments );
				}
			}

		}

	private void
	performCVSCommand(final String command)
		{
		final boolean result = true;

		if ( false )
		CVSTracer.traceIf( true,
				   "CVSProjectFrame.performCVSCommand: '" + command + '\'');

		if ( command.startsWith( "Notify:" ) )
			{
			// UNDONE
			// We really should have 'Unedit' remove an existing
			// 'Edit' entry in 'CVS/Notify', preventing both going
			// up. However, this is more complicated than that, since
			// the real client moves a backup of the file back into
			// place to replace any modifications to the file.
			//
			final int selectCh =
				CVSRequest.parseEntriesSelector( command.charAt(7) );

			String options = "";
			final String noteType = command.substring( 9, 10 );
			if ( noteType.equals( "E" ) )
				options = command.substring( 11 );

			final CVSEntryVector entries =
				this.getEntriesToActUpon( selectCh );

			this.project.addEntryNotify( entries, noteType, options );
			}
		else if ( command.startsWith( "release:" ) )
			{
			boolean doit = true;

			final Vector mods = new Vector();
			final Vector adds = new Vector();
			final Vector rems = new Vector();
			final Vector unks = new Vector();

			final CVSIgnore ignore = new CVSIgnore();

			final Config cfg = Config.getInstance();
			final UserPrefs prefs = Config.getPreferences();
			final String userIgnores =
				prefs.getProperty( ConfigConstants.GLOBAL_USER_IGNORES, null );

			if ( userIgnores != null )
				{
				ignore.addIgnoreSpec( userIgnores );
				}

			if ( this.project.checkReleaseStatus
					( ignore, mods, adds, rems, unks ) )
				{
				final ReleaseDetailsDialog dlg = new ReleaseDetailsDialog
					( this, adds, mods, rems, unks );
				dlg.show();
				doit = dlg.clickedOk();
			/*
				Object[] fmtArgs =
					{
					new Integer( adds.size() ),
					new Integer( mods.size() ),
					new Integer( rems.size() ),
					new Integer( unks.size() )
					};

				String prompt =
					ResourceMgr.getInstance().getUIFormat
						( "project.confirm.release.prompt", fmtArgs );

				String title =
					ResourceMgr.getInstance().getUIString
						( "project.confirm.release.title" );

				doit =
					JOptionPane.showConfirmDialog
						( this, prompt, title,
							JOptionPane.YES_NO_OPTION,
								JOptionPane.QUESTION_MESSAGE )
					== JOptionPane.YES_OPTION;
			*/
				}
			else
				{
				final String prompt =
					ResourceMgr.getInstance().getUIString
						( "project.confirm.release.clean.prompt" );
				final String title =
					ResourceMgr.getInstance().getUIString
						( "project.confirm.release.clean.title" );
				doit =
					JOptionPane.showConfirmDialog
						( this, prompt, title,
							JOptionPane.YES_NO_OPTION,
								JOptionPane.QUESTION_MESSAGE )
					== JOptionPane.YES_OPTION;
				}

			if ( doit )
				{
				this.releasingProject = true;
				this.commonCVSCommand( command, null, null );
				}
			}
		else
			{
			this.commonCVSCommand( command, null, null );
			}

		}

	/**
	 * This is the 'common' CVS Command method, through whom all
	 * other CVS commands ultimately pass to get the work done.
	 *
	 * @param command The CVS Command Spec command string.
	 * @param entries The entries to apply the command to.
	 * @param arguments If not null, set command arguments to
	 *        <em>only</em> these arguments.
	 */

	private boolean
	commonCVSCommand(
			final String command, CVSEntryVector entries,
			final CVSArgumentVector arguments )
		{
		String fdbkStr;
		boolean allok = true;

		this.setWaitCursor();

		final ResourceMgr rmgr = ResourceMgr.getInstance();

		fdbkStr = rmgr.getUIString( "project.fdbk.buildcvsreq" );
		this.showFeedback( fdbkStr );

		final CVSRequest request = new CVSRequest();

		request.setArguments( new CVSArgumentVector() );
		request.setGlobalArguments( new CVSArgumentVector() );

		request.traceRequest = this.traceReq;
		request.traceResponse = this.traceResp;
		request.traceProcessing = this.traceProc;
		request.traceTCPData = this.traceTCP;

		if ( arguments != null )
			{
			request.appendArguments( arguments );
			}
		else
			{
			final String argStr = this.getArgumentString();
			request.parseArgumentString( argStr.trim() );
			}

		allok = request.parseControlString( command );

		if ( ! allok )
			{
			final String[] fmtArgs =
				{ command, request.getVerifyFailReason() };
			final String msg = ResourceMgr.getInstance().getUIFormat
				( "project.cmdparse.failed.msg", fmtArgs );
			final String title = ResourceMgr.getInstance().getUIString
				( "project.cmdparse.failed.title" );
			JOptionPane.showMessageDialog
				( this, msg, title, JOptionPane.ERROR_MESSAGE );
			}

		final int portNum =
			CVSUtilities.computePortNum
				( this.project.getClient().getHostName(),
					this.project.getConnectionMethod(),
					this.project.isPServer() );

		// Establish the request's response handler if it is
		// not to be queued.
		if ( ! request.queueResponse )
			{
			request.responseHandler = this.project;
			}

		request.setPort( portNum );

		if ( request.redirectOutput )
			{
			if ( ! this.setRedirectWriter( request ) )
				{
				fdbkStr = rmgr.getUIString( "project.fdbk.canceled" );
				this.showFeedback( fdbkStr );
				this.resetCursor();
				// NOTE We return true here to indicate that the
				//      command is "ok", just canceled.
				return true;
				}
			}

		// Handle Entries selection
		// If entries is not null, use what was passed in.
		// Otherwise, fill entries according to the spec...
		if ( entries == null )
			{
			if ( request.getEntrySelector() == CVSRequest.ES_NONE )
				{
				entries = new CVSEntryVector();
				}
			else
				{
				entries =
					this.getEntriesToActUpon
						( request.getEntrySelector() );

				// Special case for 'Get User File' and 'Get New Files' canceling...
				final int selector = request.getEntrySelector();
				if ( ( selector == CVSRequest.ES_USER
						|| selector == CVSRequest.ES_NEW )
							&& entries == null )
					{
					fdbkStr = rmgr.getUIString( "project.fdbk.canceled" );
					this.showFeedback( fdbkStr );
					this.resetCursor();
					// NOTE We return true here to indicate that the
					//      command is "ok", just canceled.
					return true;
					}
/*
CVSEntry entry = entries.entryAt(0);
if( entry != null )
CVSTracer.traceIf( true,
	"@@@ " + request.getCommand() + "\n"
	+ "  EName '" + entry.getName() + "'\n"
	+ "  EFull '" + entry.getFullName() + "'\n"
	+ "  ERepos '" + entry.getRepository() + "'\n"
	+ "  ELocal '" + entry.getLocalDirectory() + "'\n"
	+ "  PRoot '" + this.project.getRootDirectory() + "'\n"
	+ "  PRepos '" + this.project.getRepository() + "'\n"
	+ "  PLocal '" + this.project.getLocalRootDirectory() + "'" );
*/
				if ( request.execInCurDir
						&& request.getEntrySelector()
								== CVSRequest.ES_POPUP )
					{
					final CVSEntry dirEnt = entries.entryAt(0);
					if ( dirEnt != null && dirEnt.isDirectory() )
						{
						request.setDirEntry( entries.entryAt(0) );
						}
					else
						{
						CVSTracer.traceWithStack( "dirEnt is WRONG!" );
						}
					}
				}
			}

		if ( entries == null )
			{
			final String[] fmtArgs = { request.getCommand() };
			final String msg = ResourceMgr.getInstance().getUIFormat
				( "project.no.selection.msg", fmtArgs );
			final String title = ResourceMgr.getInstance().getUIString
				( "project.no.selection.title" );
			JOptionPane.showMessageDialog
				( this, msg, title, JOptionPane.ERROR_MESSAGE );
			allok = false;
			}

		// Handle guarantee of a message argument...
		if ( allok )
			{
			if ( request.guaranteeMsg )
				{
				final CVSArgumentVector args = request.getArguments();
				if ( ! args.containsArgument( "-m" ) )
					{
					final String[] fmtArgs = { request.getCommand() };
					final String prompt = ResourceMgr.getInstance().getUIFormat
						( "project.message.required.prompt", fmtArgs );

					final String msgStr = this.requestMessageArgument( prompt );

					if ( msgStr != null )
						{
						args.addElement( "-m" );
						args.addElement( msgStr );
						}
					else
						{
						allok = false;
						}
					}
				}
			}

		//  UNDONE - it would be nice to "verifyRequest" here,
		//           but it is not _fully_ built (hostname, et.al.).
		if ( allok )
			{
			this.setWaitCursor();
			this.setUIAvailable( false );

			request.setEntries( entries );

			fdbkStr = rmgr.getUIString( "project.fdbk.sendreq" );
			this.showFeedback( fdbkStr );

			clearArgumentsText();

			request.setUserInterface( this );

			final CVSResponse response = new CVSResponse();

			final Thread thread =
				new CVSThread( request.getCommand(),
					this.new MyRunner( request, response ),
						this.new MyMonitor( request, response ) );

			thread.start();
			}
		else
			{
			this.resetCursor();
			}

		return allok;
		}

	private final
	class		MyRunner
	implements	Runnable
		{
		private final CVSRequest request;
		private final CVSResponse response;

		private MyRunner(final CVSRequest req, final CVSResponse resp)
			{
			this.request = req;
			this.response = resp;
			}

		@Override
		public void
		run()
			{
			boolean fail = false;

			if ( "add".equals( this.request.getCommand() ) )
				{
				final CVSEntry entry = this.request.getEntries().entryAt( 0 );

				final CVSResponse addResponse =
					project.ensureRepositoryPath
						( ProjectFrame.this,
							entry.getFullName(), this.response );

				if ( addResponse.getStatus() != CVSResponse.OK )
					{
					fail = true;
					final String fdbkStr =
						ResourceMgr.getInstance().getUIString
							( "project.fdbk.errcreate" );

					showFeedback( fdbkStr );
					this.response.appendStderr
						("An error occurred while creating '"
						 + entry.getFullName() + '\'');
					}
				else
					{
					final CVSEntry dirEntry =
						project.getDirEntryForLocalDir
							( entry.getLocalDirectory() );

					if ( dirEntry == null )
						{
						CVSLog.logMsg
							( "ADD FILE COULD NOT FIND PARENT DIRECTORY" );
						CVSLog.logMsg
							( "    locaDirectory = "
								+ entry.getLocalDirectory() );
						new Throwable( "COULD NOT FIND THE DIRECTORY!" ).
							printStackTrace();

						fail = true;
						final String fdbkStr =
							ResourceMgr.getInstance().getUIString
								( "project.fdbk.errcreate" );

						showFeedback( fdbkStr );
						this.response.appendStderr
							("An error occurred while creating '"
							 + entry.getFullName() + '\'');
						}
					else
						{
						this.request.setDirEntry( dirEntry );
						//
						// NOTE
						// SPECIAL CASE
						// SEE "ADD SPECIAL CASE" BELOW
						//
						// In this special case, the user has selected a file using
						// the FileDialog. Ergo, we have no context other than its
						// local directory. And when we were creating it, we were not
						// even sure if its parent directory existed yet!
						//
						// However, at this point, we have "ensureRepositoryPath()-ed"
						// the entry's full name. This means that the entry's parent
						// directory exists. This is critical, since that directory
						// entry has the one piece of information that we lacked when
						// we created the entry to add - the repository string. But now,
						// we can get the parent and get the repository from it!
						//
						if ( request.getEntrySelector() == CVSRequest.ES_USER )
							{
							entry.setRepository( dirEntry.getRepository() );
							}
						}
					}
				}

			if ( ! fail )
				{
				//
				// SK-unknown If the user did an update, display the Unkowns dialog.
				// UNDONE disabled until configurable. - TGE
				//
				if ( project.performCVSRequest( this.request, this.response ) )
					{
					/*
					if ( false && this.request.getCommand().equals("update") )
						{
						processUnknownFiles();
						}
					*/
					}
				}
			}
		}

	private final
	class		MyMonitor
	implements	Monitor
		{
		private final CVSRequest request;
		private final CVSResponse response;

		private MyMonitor(final CVSRequest req, final CVSResponse resp)
			{
			this.request = req;
			this.response = resp;
			}

		@Override
		public void
		threadStarted()
			{
		//	actionButton.setText( "Cancel Export" );
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
		//	actionButton.setText( "Perform Export" );

			final boolean allok =
				this.response.getStatus() == CVSResponse.OK;

			setUIAvailable( true );
			resetCursor();
			entryPanel.repaint( 500 );

			if ( releasingProject )
				{
				if ( allok )
					{
					project.releaseProject();
					SwingUtilities.invokeLater
						(
						ProjectFrame.this::dispose
						);
					}
				else
					{
					displayFinalResults( allok );
					JOptionPane.showMessageDialog
						( ProjectFrame.this,
							"The CVS command to release this project failed.",
							"WARNING", JOptionPane.WARNING_MESSAGE );
					}
				}
			else
				{
				if ( prettyDiffs )
					{
					displayPrettyDiffs( allok );
					}
				else
					{
					displayFinalResults( allok );
					}

				if ( request.isRedirected() )
					{
					request.endRedirection();
					}
				}
			}
		}

	// UNDONE This appears to not work / be broken
	private void
	setUIAvailable(final boolean avail)
		{
	//	this.argumentText.setEnabled( avail );
	//	this.entryPanel.setEnabled( avail );
		}

	private void
	focusArguments()
		{
		this.argumentText.requestFocus();
		}

	private void
	clearArgumentsText()
		{
		this.argumentText.setText( "" );
		this.argumentText.requestFocus();
		}

	private String
	getArgumentString()
		{
		return this.argumentText.getText();
		}

	private void
	setWaitCursor()
		{
		this.setCursor
			( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );
		}

	private void
	resetCursor()
		{
		this.setCursor
			( Cursor.getPredefinedCursor( Cursor.DEFAULT_CURSOR ) );
		}

	private void
	selectNoEntries()
		{
		this.entryPanel.clearSelection();
		}

	private void
	selectAllEntries()
		{
		this.entryPanel.selectAll();
		}

	private void
	selectModifiedEntries()
		{
		this.entryPanel.selectModified();
		}

	private void
	openAllEntries()
		{
		this.entryPanel.expandAll( true );
		}

	private void
	closeAllEntries()
		{
		this.entryPanel.expandAll( false );
		}

	/**
	 * Get the currently selected entries.
	 *
	 * @param expandDirEntries If true, a selected directory will be
	 *        expand to include the files within, if false, then only
	 *        the directory is returned.
	 */

	@SuppressWarnings("RedundantCast")
	private CVSEntryVector
	getSelectedEntries(final boolean expandDirEntries)
		{
		final CVSEntryVector entries = new CVSEntryVector();

		final TreePath[] selPaths = this.entryPanel.getSelectionPaths();

		if ( selPaths != null )
			{
			for ( final TreePath selPath : selPaths )
				{
				final EntryNode node = (EntryNode)
					selPath.getLastPathComponent();

				if ( node.isLeaf() || ! expandDirEntries )
					{
					entries.appendEntry( node.getEntry() );
					}
				else
					{
					for ( final TreeNode chNode : Collections.list( (Enumeration<TreeNode>) node.children() ) )
						{
						entries.appendEntry( ((EntryNode) chNode).getEntry() );
						}
					}
				}
			}

		return entries;
		}

	private CVSEntry
	createAddFileEntry( final String entryName, final String localDirectory, final String repository )
		{
		final CVSEntry entry = new CVSEntry();

		entry.setName( entryName );
		entry.setLocalDirectory( localDirectory );
		entry.setRepository( repository );
		entry.setTimestamp( this.project.getEntryFile( entry ) );

		// that is a 'zero' to indicate 'New User File'
		entry.setVersion( "0" );

		return entry;
		}

	private CVSEntryVector
	getEntriesToActUpon(final int selector)
		{
		int				i;
		final int index;
		File			entryFile;
		final String			localPath;
		CVSEntry		entry = null;
		CVSEntryVector	entries = null;

		// The 'User Selected File' selector is unique, handle it here.
			switch (selector) {
			case CVSRequest.ES_USER:
				entries = this.getUserSelectedFile();
				break;
			case CVSRequest.ES_NEW:
				entries = this.getNewlyAddedFiles();
				break;
			case CVSRequest.ES_POPUP:
				entries = this.popupEntries;
				break;
			default:
				if (selector == CVSRequest.ES_SEL
				    || selector == CVSRequest.ES_SELALL
				    || selector == CVSRequest.ES_SELMOD
				    || selector == CVSRequest.ES_SELLOST
				    || selector == CVSRequest.ES_SELUNC) {
					entries = this.getSelectedEntries(true);
					if (entries.isEmpty()) {
						this.project.getRootEntry().addAllSubTreeEntries
								(entries = new CVSEntryVector());
					}
				} else {
					this.project.getRootEntry().addAllSubTreeEntries
							(entries = new CVSEntryVector());
				}

				if (entries != null) {
					for (i = 0; i < entries.size(); ++i) {
						entry = entries.entryAt(i);
						entryFile = this.project.getEntryFile(entry);

						switch (selector) {
						case CVSRequest.ES_ALLMOD:
						case CVSRequest.ES_SELMOD:
							if (!this.project.isLocalFileModified(entry)) {
								entries.removeElementAt(i);
								--i;
							}
							break;
						case CVSRequest.ES_ALLLOST:
						case CVSRequest.ES_SELLOST:
							if (!entryFile.exists()) {
								entries.removeElementAt(i);
								--i;
							}
							break;
						case CVSRequest.ES_ALLUNC:
						case CVSRequest.ES_SELUNC:
							if (!entryFile.exists()
							    || this.project.isLocalFileModified(entry)) {
								entries.removeElementAt(i);
								--i;
							}
							break;
						}
					} // for ( i... )
				} // if ( entries != null )
				break;
			}

		return entries;
		}

	private CVSEntryVector
	getNewlyAddedFiles()
		{
		CVSEntry dirEntry = null;

		entries = this.getSelectedEntries( false );

		if ( entries == null || entries.isEmpty())
			{
			// No directory is selected. This is the only way
			// for a user to add files to the top level of the
			// project, therefore, we assume that the user wants
			// to use the root entry.
			dirEntry =
				this.entryPanel.getRootNode().getEntry();
			}
		else if ( entries.size() != 1
					|| ! entries.entryAt(0).isDirectory() )
			{
			final String msg = ResourceMgr.getInstance().getUIString
				( "project.new.one.entry.msg" );
			final String title = ResourceMgr.getInstance().getUIString
				( "project.new.one.entry.title" );
			JOptionPane.showMessageDialog
				( this, msg, title, JOptionPane.ERROR_MESSAGE );
			return null;
			}
		else
			{
			dirEntry = entries.entryAt( 0 );
			}

		final String entryLocal = dirEntry.getLocalDirectory();
		final String entryRepos = dirEntry.getRepository();

		File dirF = null;
			// Root Entry...
			dirF = entryLocal.equals("./") ? new File
					(CVSCUtilities.exportPath
							(this.project.getLocalRootDirectory())) : new File
					       (CVSCUtilities.exportPath
							       (this.project.getLocalRootDirectory()),
						CVSCUtilities.exportPath(entryLocal.substring(2)));

		final String prompt = ResourceMgr.getInstance().getUIString
			( "project.new.files.dialog.prompt" );

		final NewFilesDialog dlg =
			new NewFilesDialog( this, true, prompt );

		dlg.refreshFileList( dirF, dirEntry );

		dlg.show();

		final String[] files = dlg.getSelectedFiles();
		if ( files.length == 0 )
			return null;

		final CVSEntryVector result = new CVSEntryVector();

		for ( final String file : files )
			{
			final CVSEntry entry =
				this.createAddFileEntry
					( file, entryLocal, entryRepos );
			result.appendEntry( entry );
			}

		return result;
		}

	private CVSEntryVector
	getUserSelectedFile()
		{
		String localPath;
		CVSEntryVector result = null;

		final String prompt =
			ResourceMgr.getInstance().getUIString
				( "project.addfile.prompt" );

		final FileDialog dialog = new
			FileDialog( this, prompt, FileDialog.LOAD );

			localPath = this.lastUserFileDir != null ? this.lastUserFileDir : CVSCUtilities.exportPath
					(this.project.getLocalRootDirectory());

		dialog.setDirectory( localPath );

		dialog.show();

		final String fileName = dialog.getFile();

		if ( fileName != null )
			{
			this.lastUserFileDir = dialog.getDirectory();

			localPath =
				CVSCUtilities.ensureFinalSlash
					( CVSCUtilities.importPath( this.lastUserFileDir ) );

			final String rootRepos =
				CVSCUtilities.ensureFinalSlash
					( this.project.getRootDirectory() );

			final String localRootDir =
				CVSCUtilities.ensureFinalSlash
					( this.project.getLocalRootDirectory() );

			if ( CVSCUtilities.isSubpathInPath( localRootDir, localPath ) )
				{
				result = new CVSEntryVector();

				String entryLocal =
					localPath.substring( localRootDir.length() );

				// This is a stop gap! It will be filled in for real
				// after the repository path is ensured.
				//
				final String entryRepos = rootRepos + entryLocal;

				entryLocal =
					CVSCUtilities.ensureFinalSlash
						( "./" + entryLocal );

				final CVSEntry entry =
					this.createAddFileEntry
						( fileName, entryLocal, entryRepos );

				result.addElement( entry );
				}
			else
				{
				final String[] fmtArgs = { localPath, localRootDir };
				final String msg = ResourceMgr.getInstance().getUIFormat
					( "project.add.not.subtree.msg", fmtArgs );
				final String title = ResourceMgr.getInstance().getUIString
					( "project.add.not.subtree.title" );
				JOptionPane.showMessageDialog
					( this, msg, title, JOptionPane.ERROR_MESSAGE );
				}
			}

		return result;
		}

	private void
	displayFinalResults(final boolean ok)
		{
		final String resultLine;
		final StringBuilder finalResult = new StringBuilder();

			resultLine = ok ? ResourceMgr.getInstance().getUIString
					("project.fdbk.result.ok") : ResourceMgr.getInstance().getUIString
					("project.fdbk.result.err");

		if (! ok || !this.displayStderr.isEmpty()
		    || !this.displayStdout.isEmpty())
			{
			if (!this.displayStderr.isEmpty())
				{
				finalResult.append( this.displayStderr );
				if (!this.displayStdout.isEmpty())
					finalResult.append('\n');
				}

			if (!this.displayStdout.isEmpty())
				{
				finalResult.append( this.displayStdout );
				}

			finalResult.append('\n').append(resultLine);

			this.ensureOutputAvailable();

			this.output.setText( finalResult.toString() );

			// REVIEW - should it be configurable whether or not
			//          output is shown initially?
			this.output.setVisible( true );
			this.output.requestFocus();
			}
		else
			{
			if ( this.output != null )
				{
				this.output.setText( resultLine );
				}
			}

		this.showFeedback( resultLine );
		}

	private void
	displayPrettyDiffs(final boolean ok)
		{
		String resultLine =
			ResourceMgr.getInstance().getUIString( "project.fdbk.result.ok" );

		// NOTE
		// REVIEW
		// The only time that the stdout has zero length is when there was
		// an error, in which case stderr will have a non-zero length. If
		// both stdout and stderr are empty, then there were no diffs. In
		// the case of an error, we will use the normal results display.

		if (! ok || !displayStderr.isEmpty())
			{
			this.displayFinalResults( ok );
			resultLine = ResourceMgr.getInstance().getUIString
				( "project.fdbk.result.err" );
			}
		else if (ok && !this.displayStdout.isEmpty())
			{
			final PrettyDiffFrame diffFrame =
				new PrettyDiffFrame( this, "Diffs", null, this.displayStdout, null, null );

			final Dimension sz = this.getSize();
			final Point loc = this.getLocationOnScreen();
			final Rectangle defBounds =
				new Rectangle( loc.x + 15, loc.y + 15, 600, 440 );

			diffFrame.loadPreferences( defBounds );

			diffFrame.show();
			}

		this.showFeedback( resultLine );
		}

	private void
	establishContents()
		{
		int			row;
		final int			indent = 21;
		final Dimension	sz = this.getSize();
		final JButton		button;
		final JLabel		label;

		final UserPrefs prefs = Config.getPreferences();

		this.setBackground
			( prefs.getColor
				( "projectWindow.bg",
					new Color( 200, 215, 250 ) ) );

		/* ============ Arguments Panel =============== */

		this.argumentsPan = new JPanel();
		this.argumentsPan.setBorder(
			new CompoundBorder(
				new EtchedBorder( EtchedBorder.RAISED ),
				new EmptyBorder( 3, 3, 3, 3 )
			) );

		argumentsPan.setLayout( new GridBagLayout() );

		final Font argFont =
			prefs.getFont
				( "projectWindow.argumentFont",
					new Font( "Monospaced", Font.BOLD, 12 ) );

		this.argumentText = new JTextArea();
		this.argumentText.setEditable( true );
		this.argumentText.setBackground( Color.white );
 		this.argumentText.setFont( argFont );

		this.argumentText.setVisible( true );
		this.argumentText.setBorder( new LineBorder( Color.black ) );

		final String lblStr = ResourceMgr.getInstance().getUIString
			( "project.arguments.label" );
		final JComponent argsLbl = new JLabel(lblStr );
		argsLbl.setFont( new Font( "Monospaced", Font.BOLD, 12 ) );
		argsLbl.setForeground( Color.black );
		AWTUtilities.constrain(
			this.argumentsPan, argsLbl,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.NORTHWEST,
			0, 0, 1, 1, 1.0, 0.0 );

		AWTUtilities.constrain(
			this.argumentsPan, this.argumentText,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.NORTHWEST,
			0, 1, 1, 1, 1.0, 0.0 );

		JButton eraserButton = null;

		try {
			final Image iEraser =
				AWTUtilities.getImageResource
					( "/com/ice/jcvsii/images/icons/eraser.gif" );
			final Icon eraserIcon = new ImageIcon( iEraser );
			eraserButton = new JButton( eraserIcon )
				{
				@Override
				public boolean isFocusTraversable() { return false; }
				};
			eraserButton.setOpaque( false );
			eraserButton.setMargin( new Insets( 1,1,1,1 ) );
			}
		catch ( final IOException ex )
			{
			eraserButton = new JButton( "x" );
			}

		final String tipStr = ResourceMgr.getInstance().getUIString
			( "project.eraser.tip" );

		eraserButton.setToolTipText( tipStr );
		eraserButton.addActionListener( this );
		eraserButton.setActionCommand( "JCVS:ClearArgText" );
		AWTUtilities.constrain(
			this.argumentsPan, eraserButton,
			GridBagConstraints.NONE,
			GridBagConstraints.SOUTH,
			1, 0, 1, 2, 0.0, 0.0,
			new Insets( 1,5,0,3 ) );

		/* ============ Entries Tree =============== */

		this.entryPanel =
			new EntryPanel
				( this.project.getRootEntry(),
					this.project.getLocalRootDirectory(), this );

		/* ============ Feedback Label =============== */

		this.feedback = new JLabel( "jCVS II - TLYT == TLYM" );
		this.feedback.setOpaque( true );
		this.feedback.setBackground( Color.white );
 		this.feedback.setFont
			( prefs.getFont
				( "projectWindow.feedback.font",
					new Font( "Serif", Font.BOLD, 12 ) ) );

		final JComponent feedPan = new JPanel();
		feedPan.setLayout( new BorderLayout( 0, 0 ) );
		feedPan.setBorder( new EtchedBorder( EtchedBorder.RAISED ) );

		feedPan.add( "Center", this.feedback );

		/* ============ Final Layout =============== */

		final Container content = this.getContentPane();

		content.setLayout( new GridBagLayout() );

		row = 0;
		AWTUtilities.constrain(
			content, argumentsPan,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.CENTER,
			0, row++, 1, 1, 1.0, 0.0 );

		AWTUtilities.constrain(
			content, this.entryPanel,
			GridBagConstraints.BOTH,
			GridBagConstraints.CENTER,
			0, row++, 1, 1, 1.0, 1.0 );

		AWTUtilities.constrain(
			content, feedPan,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			0, row++, 1, 1, 1.0, 0.0 );
		}

	private void
	establishMenuBar()
		{
		String			name;
		JMenuItem		mItem;
		final MenuShortcut	accel;

		final ResourceMgr rmgr = ResourceMgr.getInstance();

		this.mBar = new JMenuBar();

		name = rmgr.getUIString( "menu.projW.file.name" );
			final JMenu mFile = new JMenu(name);
		this.mBar.add(mFile);

		name = rmgr.getUIString( "menu.projW.file.hide.name" );
		mItem = new JMenuItem( name );
		mFile.add(mItem );
		mItem.addActionListener( this );
		mItem.setActionCommand( "JCVS:HideOutputWindow" );
		mItem.setAccelerator
			( KeyStroke.getKeyStroke
				( KeyEvent.VK_H, Event.CTRL_MASK ) );

		name = rmgr.getUIString( "menu.projW.file.show.name" );
		mItem = new JMenuItem( name );
		mFile.add(mItem );
		mItem.addActionListener( this );
		mItem.setActionCommand( "JCVS:ShowOutputWindow" );
		mItem.setAccelerator
			( KeyStroke.getKeyStroke
				( KeyEvent.VK_S, Event.CTRL_MASK ) );

		mFile.addSeparator();

		name = rmgr.getUIString( "menu.projW.file.trace.name" );
		this.traceCheckItem = new JCheckBoxMenuItem( name );
		mFile.add(this.traceCheckItem );
		this.traceCheckItem.setState( this.traceReq );
		this.traceCheckItem.addActionListener( this );
		this.traceCheckItem.setActionCommand( "JCVS:TRACE" );
		this.traceCheckItem.setAccelerator
			( KeyStroke.getKeyStroke
				( KeyEvent.VK_T, Event.CTRL_MASK ) );

		mFile.addSeparator();

		if ( this.project.isPServer() || this.project.isSSHServer() )
			{
			name = rmgr.getUIString( "menu.projW.file.login.name" );
			mItem = new JMenuItem( name );
			mFile.add(mItem );
			mItem.addActionListener( this );
			mItem.setActionCommand( "JCVS:PerformLogin" );
			mItem.setAccelerator
				( KeyStroke.getKeyStroke
					( KeyEvent.VK_L, Event.CTRL_MASK ) );

			mFile.addSeparator();
			}

		name = rmgr.getUIString( "menu.projW.file.details.name" );
		mItem = new JMenuItem( name );
		mFile.add(mItem );
		mItem.addActionListener( this );
		mItem.setActionCommand( "JCVS:ShowDetails" );

		name = rmgr.getUIString( "menu.projW.file.addto.name" );
		mItem = new JMenuItem( name );
		mFile.add(mItem );
		mItem.addActionListener( this );
		mItem.setActionCommand( "JCVS:AddToWorkBench" );

		mFile.addSeparator();

		name = rmgr.getUIString( "menu.projW.file.close.name" );
		mItem = new JMenuItem( name );
		mFile.add(mItem );
		mItem.addActionListener( this );
		mItem.setActionCommand( "Close" );
		mItem.setAccelerator
			( KeyStroke.getKeyStroke
				( KeyEvent.VK_W, Event.CTRL_MASK ) );

		this.addAdditionalMenus( mBar );

		final String menuBarStr =
			Config.getPreferences().getProperty( "projectMenuBar", null );

		if ( menuBarStr != null )
			{
			this.buildCommandMenus( menuBarStr );
			}

		this.setJMenuBar( this.mBar );
		}

	// This is intended to be overwritten by subclasses!
	private void
	addAdditionalMenus(final JMenuBar menuBar)
		{
		}

	private void
	buildCommandMenus( final String menuBarSpec )
		{
		int			mIdx;
			int iIdx;
			String		itemString;
		String		menuString;
		final String[]	menuList;
		String[]	itemList;

		final UserPrefs prefs = Config.getPreferences();

		menuList = StringUtilities.splitString( menuBarSpec, ":" );
		if ( menuList == null )
			return;

		for ( mIdx = 0 ; mIdx < menuList.length ; ++mIdx )
			{
			menuString =
				prefs.getProperty
					( "projectMenu." + menuList[mIdx], null );

			if ( menuString == null )
				{
				CVSLog.logMsg
					( "ProjectFrame.buildCommandMenus: Menu Definition '"
						+ menuList[mIdx] + "' is missing." );
				continue;
				}

			itemList = StringUtilities.splitString( menuString, ":" );
			if ( itemList == null )
				continue;

			final JMenu menu = new JMenu( menuList[mIdx], true );

			for ( iIdx = 0 ; iIdx < itemList.length ; ++iIdx )
				{
				itemString =
					prefs.getProperty
						("projectMenuItem."
						 + menuList[mIdx] + '.' + itemList[iIdx], null );

				if ( itemString == null )
					{
					CVSLog.logMsg
						("ProjectFrame.buildCommandMenus: Menu '"
						 + menuList[mIdx] + "' is missing item string '"
						 + itemList[iIdx] + '\'');
					continue;
					}

				final int colonIdx = itemString.indexOf( ':' );
				if ( colonIdx < 0 )
					{
					CVSLog.logMsg
						( "CVSProjectFrame.buildCommandMenus: Menu '"
							+ menuList[mIdx] + "' Item '" + itemList[iIdx]
							+ "' has an invalid definition [title:cvs]." );
					continue;
					}

				final String title = itemString.substring( 0, colonIdx );
				final String command = itemString.substring( colonIdx + 1 );

				if ( title.equals( "-" ) )
					{
					menu.addSeparator();
					}
				else
					{
					final JMenuItem mItem = new JMenuItem( title );
					mItem.setActionCommand( command );
					mItem.addActionListener
						( e ->
								SwingUtilities.invokeLater
									( ProjectFrame.this.new PJInvoker( e ) )
						);
					menu.add( mItem );
					}
				}

			this.mBar.add( menu );
			}
		}

	private final
	class		PJInvoker
	implements	Runnable
		{
		private final ActionEvent event;
		private PJInvoker(final ActionEvent e)
			{
			this.event = e;
			}

		@Override
		public void
		run()
			{
			ProjectFrame.this.actionPerformed( this.event );
			}
		}

	//
	// CVS USER INTERFACE METHODS
	//

	@Override
	public void
	uiDisplayProgressMsg( final String message )
		{
		this.showFeedback( message );
		}

	@Override
	public void
	uiDisplayProgramError( final String error )
		{
		CVSLog.logMsg( error );
		CVSUserDialog.Error( error );
		CVSTracer.traceWithStack
			( "CVSProjectFrame.uiDisplayProgramError: " + error );
		}

	@Override
	public void
	uiDisplayResponse( final CVSResponse response )
		{
		this.displayStdout = response.getStdout();
		this.displayStderr = response.getStderr();
		}

	//
	// END OF CVS USER INTERFACE METHODS
	//

	private void
	ensureOutputAvailable()
		{
		if ( this.output == null )
			{
			final String name =
				getProjectDisplayName
					( this.project, this.project.getLocalRootDirectory() );

			this.output =
				new OutputFrame( this, name + " Output" );

			final Dimension sz = this.getSize();
			final Point loc = this.getLocationOnScreen();

			final Rectangle defBounds =
				new Rectangle( loc.x + 15, loc.y + 15, sz.width, sz.height );

			this.output.loadPreferences( defBounds );
			}
		}

	public void
	outputIsClosing()
		{
		this.output = null;
		}

	private boolean
	setRedirectWriter(final CVSRequest request)
		{
		boolean result = true;

		final FileDialog dialog = new
			FileDialog( this, "Redirect Output", FileDialog.SAVE );

		final String localDir =
			CVSCUtilities.exportPath
				( this.project.getLocalRootDirectory() );

		dialog.setDirectory( localDir );

		dialog.show();

		final String dirName = dialog.getDirectory();
		final String fileName = dialog.getFile();

		if ( dirName != null && fileName != null )
			{
			final File outputFile =
				new File( dirName, fileName );

			PrintWriter	pWriter = null;

			try {
				pWriter = new PrintWriter
					( new FileWriter( outputFile ) );
				}
			catch ( final IOException ex )
				{
				pWriter = null;
				result = false;
				final String[] fmtArgs =
					{ outputFile.getPath(), ex.getMessage() };
				final String msg = ResourceMgr.getInstance().getUIFormat
					( "project.redirect.failed.msg", fmtArgs );
				final String title = ResourceMgr.getInstance().getUIString
					( "project.redirect.failed.title" );
				JOptionPane.showMessageDialog
					( this, msg, title, JOptionPane.ERROR_MESSAGE );
				}

			// setRedirectWriter() will take a null writer and
			// turn the redirect off.
			request.setRedirectWriter( pWriter );
			}
		else
			{
			result = false; // cancel
			}

		return result;
		}

	private String
	requestMessageArgument(final String prompt)
		{
		final MessageDialog dlg =
			new MessageDialog( this, true, prompt );

		dlg.show();

		return dlg.getMessage();
		}

	private static String
	getProjectDisplayName( final CVSProject project, final String localRootPath )
		{
		String name = project.getRepository();

		//
		// If the user checked out the top ( "." ), then we will
		// get a project name of '.', which is not very informative.
		// Thus, we will grab a folder name to make is more meaningful.
		//
		if ( name.equals( "." ) )
			{
			String path = localRootPath;

			if ( path.endsWith( "/." )
					|| path.endsWith(File.separator + '.') )
				{
				path = path.substring( 0, path.length() - 2 );
				}

			name = CVSUtilities.getFileName( path );
			}

		return name;
		}

	public static void
	openProject( final File rootDirFile, final String password )
		{
		final Config cfg = Config.getInstance();
		final UserPrefs prefs = Config.getPreferences();

		final CVSClient client = CVSUtilities.createCVSClient();
		final CVSProject project = new CVSProject( client );

		project.setTempDirectory( cfg.getTemporaryDirectory() );

		project.setAllowsGzipFileMode
			( prefs.getBoolean( ConfigConstants.GLOBAL_ALLOWS_FILE_GZIP, true ) );

		project.setGzipStreamLevel
			( prefs.getInteger( ConfigConstants.GLOBAL_GZIP_STREAM_LEVEL, 0 ) );

		try {
			project.openProject( rootDirFile );

			final int cvsPort =
				CVSUtilities.computePortNum
					( project.getClient().getHostName(),
						project.getConnectionMethod(),
							project.isPServer() );

			// We establish "defaults" at both the client and
			// project levels. Project is important for adds,
			// which do not have the initial request when the
			// "ensureRepositoryPath()" method is called. The
			// client is not important, but is set for the sake
			// of consistency.

			project.setConnectionPort( cvsPort );
			project.getClient().setPort( cvsPort );

			if ( project.getConnectionMethod()
						== CVSRequest.METHOD_RSH )
				{
				CVSUtilities.establishRSHProcess( project );
				}

			project.setServerCommand(
				CVSUtilities.establishServerCommand
					( project.getClient().getHostName(),
						project.getConnectionMethod(),
							project.isPServer() ) );

			project.setSetVariables
				( CVSUtilities.getUserSetVariables
					( project.getClient().getHostName() ) );

			final String name =
				getProjectDisplayName
					( project, rootDirFile.getPath() );

			final String title = name + " Project";

			final ProjectFrame frame = new ProjectFrame( title, project );

			ProjectFrameMgr.addProject
				( frame, rootDirFile.getPath() );

			frame.toFront();
			frame.requestFocus();

			if ( password != null )
				{
				project.setPassword( password );
				}
			else
				{
				frame.verifyLogin();
				}
			}
		catch ( final IOException ex )
			{
			final String[] fmtArgs =
				{ rootDirFile.getPath(), ex.getMessage() };
			final String msg = ResourceMgr.getInstance().getUIFormat
				( "project.openproject.failed.msg", fmtArgs );
			final String title = ResourceMgr.getInstance().getUIString
				( "project.openproject.failed.title" );
			JOptionPane.showMessageDialog
				( null, msg, title, JOptionPane.ERROR_MESSAGE );
			}
		}

	/**
	 * Show a FileDialog and prompt the user for the Entries file of
	 * a local project. If the user cancel's return null, otherwise
	 * returns the path to the root (just above 'CVS/').
	 *
	 * @param parent The FileDialog's parent.
	 * @param prompt The FileDialog prompt.
	 * @param initDir The initial directory of the FileDialog, or null.
	 * @return The path to the root directory of the project.
	 *
	 */

	public static String
	getUserSelectedProject( final Frame parent, final String prompt, final String initDir )
		{
		String result = null;

		final UserPrefs prefs = Config.getPreferences();

		for ( ; ; )
			{
			final FileDialog dialog = new
				FileDialog( parent, prompt, FileDialog.LOAD );

			dialog.setFile( "Entries" );

			if ( initDir != null )
				dialog.setDirectory( initDir );

			dialog.show();

			final String fileName = dialog.getFile();
			String dirName = dialog.getDirectory();

			if ( fileName == null )
				break;

			if ( fileName.equalsIgnoreCase( "Entries" ) )
				{
				dirName = CVSCUtilities.importPath( dirName );
				if ( CVSProject.verifyAdminDirectory( dirName ) )
					{
					result = CVSProject.adminPathToRootPath( dirName );
					break;
					}
				else
					{
					final String[] fmtArgs = { fileName, dirName };
					final String msg = ResourceMgr.getInstance().getUIFormat
						( "project.select.verify.failed.msg", fmtArgs );
					final String title = ResourceMgr.getInstance().getUIString
						( "project.select.verify.failed.title" );
					JOptionPane.showMessageDialog
						( parent, msg, title, JOptionPane.ERROR_MESSAGE );
					}
				}
			else
				{
				final String msg = ResourceMgr.getInstance().getUIString
					( "project.select.help.msg" );
				final String title = ResourceMgr.getInstance().getUIString
					( "project.select.help.title" );
				JOptionPane.showMessageDialog
					( parent, msg, title, JOptionPane.INFORMATION_MESSAGE );
				}
			}

		return result;
		}

	//
	// SK-unknown
	//
	/**
	 * This method will determine the unknown files (not managed by cvs),
	 * and display them to the user in a dialog to allow the user to either
	 * delete the files, or add them to the project.
	 *
	 * Thanks to Sherali Karimov <sherali.karimov@proxima-tech.com> for this code!
	 *
	 */

	private void
	processUnknownFiles(final String cvsCommandSpec)
		{
		final Vector unks = new Vector();
		final Vector mods = new Vector();
		final Vector adds = new Vector();
		final Vector rems = new Vector();
		final CVSIgnore ignore = new CVSIgnore();
		final Config cfg = Config.getInstance();
		final UserPrefs prefs = Config.getPreferences();

		final String userIgnores =
			prefs.getProperty( ConfigConstants.GLOBAL_USER_IGNORES, null );

		// this finds all the unknown files
		this.project.checkReleaseStatus( ignore, mods, adds, rems, unks );

		if (!unks.isEmpty())
			{
			String root = this.project.getLocalRootDirectory();
			if ( root == null )
				return;

			root = root.trim();
			final char ch = root.charAt( root.length()-1 );
			if ( ch != '/' && ch != '\\' && ch != File.separatorChar )
				root += File.separatorChar;

			// sift out directories and files from the unknowns list
			final int size = unks.size();
			final Vector unkFiles = new Vector();
			for ( int i=0 ; i < size ; i++ )
				{
				String nextStr = (String) unks.elementAt(i);
				if ( nextStr.startsWith( "./" ) )
					nextStr = nextStr.substring( 2);

				final File f = new File( root + nextStr );
				if ( f.exists() && ! f.isDirectory() )
					{
					unkFiles.add( f );
					}
				}

			if (!unkFiles.isEmpty())
				{
				// this displays a dialog for the user to choose which ones to delete
				final UnknownFilesDialog dialog =
					new UnknownFilesDialog( this, unkFiles, this.getTitle(), false );

				if ( dialog.isCancelAction() )
					{
					showFeedback( "User cancelled." );
					}
				else
					{
					final File array[] = dialog.selectFiles();
					if ( array.length == 0 )
						{
						showFeedback( "No selection to operate on." );
						}
					else if ( dialog.isDeleteAction() )
						{
						// this deletes selected files
						for ( final File element : array )
							{
							if ( element.delete() )
								{
								final String msg =
									"File "+element.getAbsolutePath()+" deleted.";

								this.showFeedback( msg );
								}
							else
								{
								final String msg =
										"Failed to delete file "
										+ element.getAbsolutePath() + '.';

								this.showFeedback( msg );
								}
							}

						this.showFeedback
							( "Finished deleting selected unknown files." );
						}
					else if ( dialog.isAddAction() )
						{
						final CVSEntryVector entries = new CVSEntryVector();

						for ( final File element : array )
							{
							final CVSEntry tempEntry = this.toCVSEntry( element );
							if ( tempEntry != null )
								{
								entries.appendEntry( tempEntry );
								}
							}

						this.commonCVSCommand( cvsCommandSpec, entries, null );
						}
					}
				}
			}
		else
			{
			final String msg = ResourceMgr.getInstance().getUIString
				( "project.no.unknowns.msg" );
			final String title = ResourceMgr.getInstance().getUIString
				( "project.no.unknowns.title" );

			JOptionPane.showMessageDialog
				( this, msg, title, JOptionPane.INFORMATION_MESSAGE );
			}
		}

	private void
	processUnknownDirs()
		{
		final Vector unks = new Vector();
		final Vector mods = new Vector();
		final Vector adds = new Vector();
		final Vector rems = new Vector();
		final CVSIgnore ignore = new CVSIgnore();
		final Config cfg = Config.getInstance();
		final UserPrefs prefs = Config.getPreferences();
		final StringBuffer resultBuffer = new StringBuffer( 4 * 1024 );

		final String userIgnores =
			prefs.getProperty( ConfigConstants.GLOBAL_USER_IGNORES, null );

		// this finds all the unknown files
		this.project.checkReleaseStatus( ignore, mods, adds, rems, unks );

		if (!unks.isEmpty())
			{
			String root = this.project.getLocalRootDirectory();
			if ( root == null )
				return;

			root = root.trim();
			final char ch = root.charAt( root.length()-1 );
			if ( ch != '/' && ch != '\\' && ch != File.separatorChar )
				root += File.separatorChar;

			// sift out directories and files from the unknowns list
			final int size = unks.size();
			final Vector unkDirs = new Vector();
			for ( int i=0 ; i < size ; i++ )
				{
				String nextStr = (String) unks.elementAt(i);
				if ( nextStr.startsWith( "./" ) )
					nextStr = nextStr.substring( 2);

				final File f = new File( root + nextStr );
				if ( f.exists() && f.isDirectory() )
					{
					unkDirs.add( f );
					}
				}

			//
			// DIRECTORIES
			//
			if (!unkDirs.isEmpty())
				{
				// this displays a dialog for the user to choose which ones to delete
				final UnknownFilesDialog dialog =
					new UnknownFilesDialog( this, unkDirs,
						"Directories in " + this.getTitle(), true );

				if ( dialog.isCancelAction() )
					{
					showFeedback( "User cancelled." );
					}
				else
					{
					final File array[] = dialog.selectFiles();
					if ( array.length == 0 )
						{
						showFeedback( "No selection to operate on." );
						}
					else
						{
						for ( final File element : array )
							{
							final String path = element.getPath();
							if ( CVSCUtilities.isSubpathInPath( root, path ) )
								{
								String dirPath = path.substring( root.length() );
								dirPath = CVSCUtilities.importPath( dirPath );
								this.addUnknownDirectory( resultBuffer, dirPath );
								}
							else
								{
								final String msg =
									"Directory " + path
									+ " not under root directory!";

								resultBuffer.append( msg ).append('\n');
								this.showFeedback( msg );
								}
							}

						this.project.writeAdminFiles();
						}
					}

				if ( resultBuffer.length() > 0 )
					{
					this.ensureOutputAvailable();
					this.output.setText( resultBuffer.toString() );
					this.output.setVisible( true );
					this.output.requestFocus();
					}
				}
			}
		else
			{
			final String msg = ResourceMgr.getInstance().getUIString
				( "project.no.unknowns.msg" );
			final String title = ResourceMgr.getInstance().getUIString
				( "project.no.unknowns.title" );

			JOptionPane.showMessageDialog
				( this, msg, title, JOptionPane.INFORMATION_MESSAGE );
			}
		}

	private void
	addUnknownDirectory(final StringBuffer resultBuffer, final String dirPath)
		{
		final CVSResponse		result;
		final StringBuilder addPath = new StringBuilder();

		final CVSEntry dirEntry = this.project.getRootEntry();

		// The root entry has a 'bad' fullname, so we use getLocalDirectory().
		addPath.append(dirEntry.getLocalDirectory()).append(dirPath);
		// Make sure that it ends with "/."
		if ( addPath.charAt( addPath.length() - 1 ) != '/' )
			{
			addPath.append('/');
			}
		addPath.append('.');

		result =
			this.project.ensureRepositoryPath
				( this, addPath.toString(), new CVSResponse() );

		resultBuffer.append( result.getResultText() );
		}

	/**
	 * Given a local file, return a CVSEntry describing it.
	 */

	private CVSEntry
	toCVSEntry(final File f)
		{
		CVSEntry result = null;
		final String fileName = f.getName();

		if ( fileName != null )
			{
			final String localPath =
				CVSCUtilities.ensureFinalSlash
					( CVSCUtilities.importPath( f.getParent() ) );

			final String repos = this.project.getRepository();

			final String rootDir =
				CVSCUtilities.ensureFinalSlash
					( this.project.getRootDirectory() );

			final String localRootDir =
				CVSCUtilities.ensureFinalSlash
					( this.project.getLocalRootDirectory() );

			if ( CVSCUtilities.isSubpathInPath( localRootDir, localPath ) )
				{
				String entryLocal = localPath.substring( localRootDir.length() );
				final String entryRepos = rootDir + repos + '/' + entryLocal;

				entryLocal =
					CVSCUtilities.ensureFinalSlash
						( "./" + /* repos + "/" + */ entryLocal );

				result = this.createAddFileEntry( fileName, entryLocal, entryRepos );
				}
			}

		return result;
		}

	}
