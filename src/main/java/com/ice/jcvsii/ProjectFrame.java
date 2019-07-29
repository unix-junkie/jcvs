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

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.tree.TreePath;

import com.ice.cvsc.*;
import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;
import com.ice.util.StringUtilities;


/**
 * This is the frame that implements the 'Project Window' in jCVS.
 * This frame will display the project's icon list, the arguments
 * text area, the user feedback display area, and a series of menus.
 * The primary unit of display in this class is a CVSProject.
 *
 * @version $Id: ProjectFrame.java,v 1.11 2002/02/10 18:04:15 time Exp $
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

public
class		ProjectFrame
extends		JFrame
implements	ActionListener, CVSUserInterface
	{
	static public final String		RCS_ID = "$Id: ProjectFrame.java,v 1.11 2002/02/10 18:04:15 time Exp $";
	static public final String		RCS_REV = "$Revision: 1.11 $";

	private CVSProject		project;
	private OutputFrame		output;
	private UserPrefs		prefs;

	private CVSEntryVector	entries;
	private CVSEntryVector	popupEntries;

	private String			displayStdout;
	private String			displayStderr;

	private String			lastUserFileDir;

	private boolean			briefArgs;
	private JPanel			argumentsPan;
	private JTextArea		argumentText;

	private JLabel			feedback; 

	private EntryPanel		entryPanel;

	private JMenuBar		mBar;
	private JMenu			mFile;

	private JCheckBoxMenuItem	traceCheckItem;

	private Cursor			saveCursor;

	private boolean			traceReq = false;
	private boolean			traceResp = false;
	private boolean			traceProc = false;
	private boolean			traceTCP = false;

	/**
	 * We set this to true when we release so that we do not try to
	 * save the preferences, which will fail because 'CVS/' is gone.
	 */
	private boolean			releasingProject = false;

	private boolean			prettyDiffs = false;

	
	public
	ProjectFrame( String title, CVSProject project )
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
				public void
					windowClosing( WindowEvent e )
						{ dispose(); }

				public void
					windowClosed( WindowEvent e )
						{ windowBeingClosed(); }
				}
			);
		}

	private void
	initialize( CVSProject project )
		{
		this.project = project;
		this.output = null;

		this.entries = null;
		this.popupEntries = null;

 		this.briefArgs = true;

		this.feedback = null;
		this.argumentsPan = null;
		this.saveCursor = null;
		this.displayStdout = "";
		this.displayStderr = "";
		this.lastUserFileDir = null;
		this.releasingProject = false;

		Config cfg = Config.getInstance();

		this.prefs =
			new UserPrefs( project.getRepository(), cfg.getPrefs() );

		this.prefs.setPropertyPrefix( "jcvsii." );

		cfg.loadProjectPreferences( project, this.prefs );

		this.traceReq =
			this.prefs.getBoolean( Config.GLOBAL_CVS_TRACE_ALL, false );

		this.traceResp = this.traceReq;
		this.traceProc = this.traceReq;
		this.traceTCP = this.traceReq;
		}

	public void
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

	public void
	loadPreferences()
		{
		this.entryPanel.loadPreferences( this.prefs );

		Rectangle bounds =
			this.prefs.getBounds
				( Config.PROJECT_WINDOW_BOUNDS,
					new Rectangle( 20, 40, 525, 440 ) );

		this.setBounds( bounds );
		}

	public void
	savePreferences()
		{
		Rectangle bounds = this.getBounds();

		if ( bounds.x >= 0 && bounds.y >= 0
				&& bounds.width > 0 && bounds.height > 0 )
			{
			this.prefs.setBounds
				( Config.PROJECT_WINDOW_BOUNDS, bounds );
			}

		this.entryPanel.savePreferences( this.prefs );
		}

	public void
	actionPerformed( ActionEvent evt )
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
		int index = command.indexOf( '&' );
		if ( index < 0 )
			{
			this.performActionLine( command, evt );
			}
		else
			{
			StringTokenizer toker =
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
				catch ( NoSuchElementException ex )
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
	
	public void
	performActionLine( String command, ActionEvent event )
		{
		String	subCmd;

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

	protected boolean
	performJCVSCommand( String command )
		{
		int		i, count;
		boolean	result = true;

		if ( command.startsWith( "FMSG:" ) )
			{
			String message = command.substring( 5 );
			this.showFeedback( message );
			}
		else if ( command.startsWith( "NOTE:" ) )
			{
			String message = command.substring( 5 );
			CVSUserDialog.Note( this, message );
			}
		else if ( command.startsWith( "ERROR:" ) )
			{
			String message = command.substring( 6 );
			CVSUserDialog.Error( this, message );
			}
		else if ( command.startsWith( "PDIFF:" ) )
			{
			String subCmd = command.substring( 6 );
			this.prettyDiffs = true;
			this.performCVSCommand( subCmd );
			}
		else if ( command.equals( "TRACE" ) )
			{
			SwingUtilities.invokeLater
				( new Runnable()
					{
					public void run()
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
					}
				);
			}
		else if ( command.startsWith( "DisplayUnkFiles:" ) )
			{
			String subCmd = command.substring( "DisplayUnkFiles:".length() );
			this.processUnknownFiles( subCmd );
			}
		else if ( command.equalsIgnoreCase( "DisplayUnkDirs" ) )
			{
			this.processUnknownDirs();
			}
		else if ( command.equalsIgnoreCase( "Close" ) )
			{
			SwingUtilities.invokeLater
				( new Runnable()
					{ public void run() { dispose(); } }
				);
			}
		else if ( command.equalsIgnoreCase( "HideOutputWindow" ) )	 
			{
			if ( this.output != null )
				{
				SwingUtilities.invokeLater
					( new Runnable()
						{ public void run() { output.setVisible( false ); } }
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
			SwingUtilities.invokeLater
				( new Runnable()
					{ public void run() { displayProjectDetails(); } }
				);
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
			SwingUtilities.invokeLater
				( new Runnable()
					{ public void run() { clearArgumentsText(); } }
				);
			}
		else if ( command.equalsIgnoreCase( "PerformLogin" ) )
			{
			SwingUtilities.invokeLater
				( new Runnable()
					{ public void run() { performLogin(); } }
				);
			}
		else if ( command.equalsIgnoreCase( "AddToWorkBench" ) )
			{
			SwingUtilities.invokeLater
				( new Runnable()
					{ public void run() { addToWorkBench(); } }
				);
			}
		else if ( command.equalsIgnoreCase( "ExpandBelow" ) )
			{
			TreePath[] selPaths = this.entryPanel.getSelectionPaths();
			if ( selPaths != null )
				{
				for ( int nIdx = 0 ; nIdx < selPaths.length ; ++nIdx )
					{
					TreePath selPath = selPaths[ nIdx ];
					EntryNode node = (EntryNode)
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
			TreePath[] selPaths = this.entryPanel.getSelectionPaths();
			if ( selPaths != null )
				{
				for ( int nIdx = 0 ; nIdx < selPaths.length ; ++nIdx )
					{
					TreePath selPath = selPaths[ nIdx ];
					EntryNode node = (EntryNode)
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
			int selector =
				CVSRequest.parseEntriesSelector( command.charAt(5) );

			String verb = "edit";
			if ( command.length() > 7 )
				{
				verb = command.substring( 7 );
				}

			CVSEntryVector entries =
				this.getEntriesToActUpon( selector );
			
			for ( int eIdx = 0 ; entries != null
					&& eIdx < entries.size() ; ++eIdx )
				{
				CVSEntry entry = entries.entryAt(eIdx);
				if ( entry != null )
					{
					File entryFile =
						this.project.getLocalEntryFile( entry );

					JAFUtilities.openFile
						( entry.getName(), entryFile, verb );
					}
				else
					{
					(new Throwable
						( "NULL ENTRY["+eIdx+"] on command '" + command + "'" )).
							printStackTrace();
					}
				}
			}
		else if ( command.startsWith( "MOVE:" ) )
			{
			String backupPattern = command.substring(7);

			int selector =
				CVSRequest.parseEntriesSelector( command.charAt(5) );

			CVSEntryVector entries =
				this.getEntriesToActUpon( selector );

			for ( int eIdx = 0 ; entries != null
					&& eIdx < entries.size() ; ++eIdx )
				{
				CVSEntry entry = entries.entryAt(eIdx);
				if ( entry != null )
					{
					File entryFile =
						this.project.getLocalEntryFile( entry );

					if ( ! CVSUtilities.renameFile( entryFile, backupPattern, true ) )
						{
						String[] fmtArgs = { entryFile.getPath() };
						String msg = ResourceMgr.getInstance().getUIFormat
							( "project.rename.failed.msg", fmtArgs );
						String title = ResourceMgr.getInstance().getUIString
							( "project.rename.failed.title" );
						JOptionPane.showMessageDialog
							( this, msg, title, JOptionPane.ERROR_MESSAGE );
						break;
						}
					}
				else
					{
					(new Throwable
						( "NULL ENTRY["+eIdx+"] on command '" + command + "'" )).
							printStackTrace();
					}
				}
			}
		else if ( command.startsWith( "COPY:" ) )
			{
			String copyPattern = command.substring(7);

			int selector =
				CVSRequest.parseEntriesSelector( command.charAt(5) );

			CVSEntryVector entries =
				this.getEntriesToActUpon( selector );

			for ( int eIdx = 0 ; entries != null
					&& eIdx < entries.size() ; ++eIdx )
				{
				CVSEntry entry = entries.entryAt(eIdx);
				if ( entry != null )
					{
					File entryFile =
						this.project.getLocalEntryFile( entry );

					if ( ! CVSUtilities.copyFile( entryFile, copyPattern ) )
						{
						String[] fmtArgs = { entryFile.getPath() };
						String msg = ResourceMgr.getInstance().getUIFormat
							( "project.copy.failed.msg", fmtArgs );
						String title = ResourceMgr.getInstance().getUIString
							( "project.copy.failed.title" );
						JOptionPane.showMessageDialog
							( this, msg, title, JOptionPane.ERROR_MESSAGE );
						break;
						}
					}
				else
					{
					(new Throwable
						( "NULL ENTRY["+eIdx+"] on command '" + command + "'" )).
							printStackTrace();
					}
				}
			}
		else if ( command.startsWith( "CMDLINE:" ) )
			{
			String subCmd = command.substring( 8 );
			this.performCVSCommandLine( subCmd );
			}
		else if ( command.startsWith( "ADDDIR:" ) )
			{
			String subCmd = command.substring( 8 );
			this.performAddDirectory( subCmd );
			}

		return result;
		} 

	private void
	addToWorkBench()
		{
		JCVS.getMainFrame().addProjectToWorkBench( this.project );
		}

	public void
	displayProjectDetails()
		{
		String type =
			Config.getPreferences().getProperty
				( Config.PROJECT_DETAILS_TYPE, "text/plain" );

		if ( type.equalsIgnoreCase( "text/html" ) )
			{
			this.displayProjectDetailsHTML();
			}
		else
			{
			this.displayProjectDetailsPlain();
			}
		}

	public void
	displayProjectDetailsHTML()
		{
		Object[] fmtArgs =
			{
			this.project.getRepository(),
			this.project.getRootDirectory(),
			this.project.getClient().getHostName(),
			new Integer( this.project.getClient().getPort() ),
			this.project.getLocalRootDirectory()
			};

		String msgStr =
			ResourceMgr.getInstance().getUIFormat
				( "project.details.dialog.html", fmtArgs );
		String title =
			ResourceMgr.getInstance().getUIString
				( "project.details.dialog.title" );

		( new HTMLDialog( this, title, true, msgStr ) ).show();
		}

	public void
	displayProjectDetailsPlain()
		{
		Object[] fmtArgs =
			{
			this.project.getRepository(),
			this.project.getRootDirectory(),
			this.project.getClient().getHostName(),
			new Integer( this.project.getClient().getPort() ),
			this.project.getLocalRootDirectory()
			};

		String msgStr =
			ResourceMgr.getInstance().getUIFormat
				( "project.details.dialog.text", fmtArgs );
		String title =
			ResourceMgr.getInstance().getUIString
				( "project.details.dialog.title" );

		JOptionPane.showMessageDialog
			( this, msgStr, title, JOptionPane.INFORMATION_MESSAGE );
		}

	public synchronized void
	showFeedback( String message )
		{
		this.feedback.setText( message );
		this.feedback.repaint( 0 );
		}

	public void
	verifyLogin()
		{
		if ( ! this.project.isPServer()
				&& ! this.project.isSSHServer() )
			return;

		boolean valid;
		String password = this.project.getPassword();

		if ( password == null )
			{
			this.performLogin();
			}
		}

	public void
	performLogin()
		{
		if ( ! this.project.isPServer()
				&& ! this.project.isSSHServer() )
			return;

		String password;
		String userName = this.project.getUserName();

		PasswordDialog passDialog =
			new PasswordDialog( this, userName );

		passDialog.show();

		userName = passDialog.getUserName();
		password = passDialog.getPassword();

		if ( userName != null && password != null )
			{
			this.setWaitCursor();

			boolean valid =
				this.project.verifyPassword
					( this, userName, password, this.traceReq );

			this.resetCursor();
			
			if ( ! valid )
				{
				String[] fmtArgs = { userName };
				String msg = ResourceMgr.getInstance().getUIFormat
					( "project.login.failed.msg", fmtArgs );
				String title = ResourceMgr.getInstance().getUIString
					( "project.login.failed.title" );
				JOptionPane.showMessageDialog
					( this, msg, title, JOptionPane.ERROR_MESSAGE );
				}
			}
		}

	public void
	performCheckOut( String checkOutCommand )
		{
		boolean result =
			this.commonCVSCommand( checkOutCommand, null, null );

		if ( result )
			{
			this.project.writeAdminFiles();
			}
		else
			{
			String msg = ResourceMgr.getInstance().getUIString
				( "project.checkout.failed.msg" );
			String title = ResourceMgr.getInstance().getUIString
				( "project.checkout.failed.title" );
			JOptionPane.showMessageDialog
				( this, msg, title, JOptionPane.ERROR_MESSAGE );
			}
		}

	protected void
	performAddDirectory( String commandSpec )
		{
		CVSResponse		result;

		String prompt =
			ResourceMgr.getInstance().getUIString
				( "global.directory.name.prompt" );

		String dirPath = JOptionPane.showInputDialog( prompt );

		if ( dirPath != null )
			{
			CVSEntryVector entries =
				this.getEntriesToActUpon( CVSRequest.ES_POPUP );

			if ( entries != null && entries.size() > 0 )
				{
				StringBuffer addPath = new StringBuffer();
				CVSEntry dirEntry = entries.entryAt(0);

				// The root entry has a 'bad' fullname.
				// UNDONE
				// REVIEW
				// Should entries have a "isRoot" flag, and
				// make this adjustment for me in getFullName()?
				//
				
				if ( dirEntry == this.project.getRootEntry() )
					addPath.append
						( dirEntry.getLocalDirectory() + dirPath );
				else
					addPath.append
						( dirEntry.getFullName() + "/" + dirPath );

				if ( ! addPath.toString().endsWith( "/" ) )
					{
					addPath.append( "/" );
					}
				addPath.append( "." );
				
				result =
					this.project.ensureRepositoryPath
						( this, addPath.toString(), new CVSResponse() );

				if ( result.getStatus() != CVSResponse.OK )
					{
					String[] fmtArgs =
						{
						dirPath,
						result.getStderr(),
						result.getStdout()
						};

					String msg = ResourceMgr.getInstance().getUIFormat
						( "project.diradd.failed.msg", fmtArgs );
					String title = ResourceMgr.getInstance().getUIString
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
				( new Throwable( "The entries list is EMPTY!!!" )).
					printStackTrace();
				}
			}
		}

	protected void
	performCVSCommandLine( String commandSpec )
		{
		String command = null;

		String prompt =
			ResourceMgr.getInstance().getUIString
				( "project.cvs.command.prompt" );

		String commandLine =
			JOptionPane.showInputDialog( prompt );

		if ( commandLine != null )
			{
			CVSArgumentVector arguments =
				CVSArgumentVector.parseArgumentString( commandLine );

			if ( arguments.size() > 0 )
				{
				command = (String) arguments.elementAt(0);

				arguments.removeElementAt(0);

				this.commonCVSCommand
					( command + ":" + commandSpec, null, arguments );
				}
			}

		}

	protected boolean
	performCVSCommand( String command )
		{
		boolean result = true;

		if ( false )
		CVSTracer.traceIf( true,
			"CVSProjectFrame.performCVSCommand: '" + command + "'" );

		if ( command.startsWith( "Notify:" ) )
			{
			// UNDONE
			// We really should have 'Unedit' remove an existing
			// 'Edit' entry in 'CVS/Notify', preventing both going
			// up. However, this is more complicated than that, since
			// the real client moves a backup of the file back into
			// place to replace any modifications to the file.
			// 
			int selectCh =
				CVSRequest.parseEntriesSelector( command.charAt(7) );

			String options = "";
			String noteType = command.substring( 9, 10 );
			if ( noteType.equals( "E" ) )
				options = command.substring( 11 );

			CVSEntryVector entries =
				this.getEntriesToActUpon( selectCh );
			
			this.project.addEntryNotify( entries, noteType, options );
			}
		else if ( command.startsWith( "release:" ) )
			{
			boolean doit = true;

			Vector mods = new Vector();
			Vector adds = new Vector();
			Vector rems = new Vector();
			Vector unks = new Vector();

			CVSIgnore ignore = new CVSIgnore();

			Config cfg = Config.getInstance();
			UserPrefs prefs = cfg.getPreferences();
			String userIgnores =
				prefs.getProperty( Config.GLOBAL_USER_IGNORES, null );

			if ( userIgnores != null )
				{
				ignore.addIgnoreSpec( userIgnores );
				}

			if ( this.project.checkReleaseStatus
					( ignore, mods, adds, rems, unks ) )
				{
				ReleaseDetailsDialog dlg = new ReleaseDetailsDialog
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
				String prompt =
					ResourceMgr.getInstance().getUIString
						( "project.confirm.release.clean.prompt" );
				String title =
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

		return result;
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
			String command, CVSEntryVector entries,
			CVSArgumentVector arguments )
		{
		String fdbkStr;
		boolean allok = true;

		this.setWaitCursor();

		ResourceMgr rmgr = ResourceMgr.getInstance();

		fdbkStr = rmgr.getUIString( "project.fdbk.buildcvsreq" );
		this.showFeedback( fdbkStr );

		CVSRequest request = new CVSRequest();

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
			String argStr = this.getArgumentString();
			request.parseArgumentString( argStr.trim() );
			}

		allok = request.parseControlString( command );

		if ( ! allok )
			{
			String[] fmtArgs =
				{ command, request.getVerifyFailReason() };
			String msg = ResourceMgr.getInstance().getUIFormat
				( "project.cmdparse.failed.msg", fmtArgs );
			String title = ResourceMgr.getInstance().getUIString
				( "project.cmdparse.failed.title" );
			JOptionPane.showMessageDialog
				( this, msg, title, JOptionPane.ERROR_MESSAGE );
			}

		int portNum =
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
				int selector = request.getEntrySelector();
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
					CVSEntry dirEnt = entries.entryAt(0);
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
			String[] fmtArgs = { request.getCommand() };
			String msg = ResourceMgr.getInstance().getUIFormat
				( "project.no.selection.msg", fmtArgs );
			String title = ResourceMgr.getInstance().getUIString
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
				CVSArgumentVector args = request.getArguments();
				if ( ! args.containsArgument( "-m" ) )
					{
					String[] fmtArgs = { request.getCommand() };
					String prompt = ResourceMgr.getInstance().getUIFormat
						( "project.message.required.prompt", fmtArgs );

					String msgStr = this.requestMessageArgument( prompt );

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

			CVSResponse response = new CVSResponse();

			CVSThread thread =
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

	private
	class		MyRunner
	implements	Runnable
		{
		private CVSRequest request;
		private CVSResponse response;

		public
		MyRunner( CVSRequest req, CVSResponse resp )
			{
			this.request = req;
			this.response = resp;
			}

		public void
		run()
			{
			boolean fail = false;

			if ( "add".equals( this.request.getCommand() ) )
				{
				CVSEntry entry = this.request.getEntries().entryAt( 0 );

				CVSResponse addResponse =
					project.ensureRepositoryPath
						( ProjectFrame.this,
							entry.getFullName(), this.response );
				
				if ( addResponse.getStatus() != CVSResponse.OK )
					{
					fail = true;
					String fdbkStr =
						ResourceMgr.getInstance().getUIString
							( "project.fdbk.errcreate" );

					showFeedback( fdbkStr );
					this.response.appendStderr
						( "An error occurred while creating '"
							+ entry.getFullName() + "'" );
					}
				else
					{
					CVSEntry dirEntry =
						project.getDirEntryForLocalDir
							( entry.getLocalDirectory() );

					if ( dirEntry == null )
						{
						CVSLog.logMsg
							( "ADD FILE COULD NOT FIND PARENT DIRECTORY" );
						CVSLog.logMsg
							( "    locaDirectory = "
								+ entry.getLocalDirectory() );
						(new Throwable( "COULD NOT FIND THE DIRECTORY!" )).
							printStackTrace();

						fail = true;
						String fdbkStr =
							ResourceMgr.getInstance().getUIString
								( "project.fdbk.errcreate" );

						showFeedback( fdbkStr );
						this.response.appendStderr
							( "An error occurred while creating '"
								+ entry.getFullName() + "'" );
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

	private
	class		MyMonitor
	implements	CVSThread.Monitor
		{
		private CVSRequest request;
		private CVSResponse response;

		public
		MyMonitor( CVSRequest req, CVSResponse resp )
			{
			this.request = req;
			this.response = resp;
			}

		public void
		threadStarted()
			{
		//	actionButton.setText( "Cancel Export" );
			}

		public void
		threadCanceled()
			{
			}

		public void
		threadFinished()
			{
		//	actionButton.setText( "Perform Export" );

			boolean allok =
				( this.response.getStatus() == CVSResponse.OK );

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
						new Runnable()
							{ public void run() { dispose(); } }
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
	public void
	setUIAvailable( boolean avail )
		{
	//	this.argumentText.setEnabled( avail );
	//	this.entryPanel.setEnabled( avail );
		}

	protected void
	focusArguments()
		{
		this.argumentText.requestFocus();
		}

	protected void
	clearArgumentsText()
		{
		this.argumentText.setText( "" );
		this.argumentText.requestFocus();
		}

	protected String
	getArgumentString()
		{
		return this.argumentText.getText();
		}
	
	protected void
	setWaitCursor()
		{
		this.setCursor
			( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );
		}

	protected void
	resetCursor()
		{
		this.setCursor
			( Cursor.getPredefinedCursor( Cursor.DEFAULT_CURSOR ) );
		}

	public void
	selectNoEntries()
		{
		this.entryPanel.clearSelection();
		}

	public void
	selectAllEntries()
		{
		this.entryPanel.selectAll();
		}

	public void
	selectModifiedEntries()
		{
		this.entryPanel.selectModified();
		}

	public void
	openAllEntries()
		{
		this.entryPanel.expandAll( true );
		}

	public void
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

	protected CVSEntryVector
	getSelectedEntries( boolean expandDirEntries )
		{
		CVSEntryVector entries = new CVSEntryVector();

		TreePath[] selPaths = this.entryPanel.getSelectionPaths();

		if ( selPaths != null )
			{
			for ( int i = 0 ; i < selPaths.length ; ++i )
				{
				EntryNode node = (EntryNode)
					selPaths[i].getLastPathComponent();

				if ( node.isLeaf() || ! expandDirEntries )
					{
					entries.appendEntry( node.getEntry() );
					}
				else
					{
					Enumeration chEnum = node.children();
					for ( ; chEnum.hasMoreElements() ; )
						{
						EntryNode chNode =
							(EntryNode) chEnum.nextElement();
						entries.appendEntry( chNode.getEntry() );
						}
					}
				}
			}

		return entries;
		}

	private CVSEntry
	createAddFileEntry( String entryName, String localDirectory, String repository )
		{
		CVSEntry entry = new CVSEntry();

		entry.setName( entryName );
		entry.setLocalDirectory( localDirectory );
		entry.setRepository( repository );
		entry.setTimestamp( this.project.getEntryFile( entry ) );

		// that is a 'zero' to indicate 'New User File'
		entry.setVersion( "0" );

		return entry;
		}

	public CVSEntryVector
	getEntriesToActUpon( int selector )
		{
		int				i, index;
		File			entryFile;
		String			localPath;
		CVSEntry		entry = null;
		CVSEntryVector	entries = null;

		// The 'User Selected File' selector is unique, handle it here.
		if ( selector == CVSRequest.ES_USER )
			{
			entries = this.getUserSelectedFile();
			}
		else if ( selector == CVSRequest.ES_NEW )
			{
			entries = this.getNewlyAddedFiles();
			}
		else if ( selector == CVSRequest.ES_POPUP )
			{
			entries = this.popupEntries;
			}
		else
			{
			if (   selector == CVSRequest.ES_SEL
				|| selector == CVSRequest.ES_SELALL
				|| selector == CVSRequest.ES_SELMOD
				|| selector == CVSRequest.ES_SELLOST 
				|| selector == CVSRequest.ES_SELUNC ) 
				{
				entries = this.getSelectedEntries( true );
				if ( entries.size() == 0 )
					{
					this.project.getRootEntry().addAllSubTreeEntries
						( entries = new CVSEntryVector() );
					}
				}
			else
				{
				this.project.getRootEntry().addAllSubTreeEntries
					( entries = new CVSEntryVector() );
				}

			if ( entries != null )
				{
				for ( i = 0 ; i < entries.size() ; ++i )
					{
					entry = entries.entryAt(i);
					entryFile = this.project.getEntryFile( entry );

					if ( selector == CVSRequest.ES_ALLMOD
							|| selector == CVSRequest.ES_SELMOD )
						{
						if ( ! this.project.isLocalFileModified( entry ) )
							{
							entries.removeElementAt(i);
							--i;
							}
						}
					else if ( selector == CVSRequest.ES_ALLLOST
							|| selector == CVSRequest.ES_SELLOST )
						{
						if ( ! entryFile.exists() )
							{
							entries.removeElementAt(i);
							--i;
							}
						}
					else if ( selector == CVSRequest.ES_ALLUNC
							|| selector == CVSRequest.ES_SELUNC )
						{
						if ( ! entryFile.exists()
								|| this.project.isLocalFileModified( entry ) )
							{
							entries.removeElementAt(i);
							--i;
							}
						}
					} // for ( i... )
				} // if ( entries != null )
			} // if ( ! user file special case )

		return entries;
		}

	public CVSEntryVector
	getNewlyAddedFiles()
		{
		CVSEntry dirEntry = null;

		entries = this.getSelectedEntries( false );

		if ( entries == null || entries.size() == 0 )
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
			String msg = ResourceMgr.getInstance().getUIString
				( "project.new.one.entry.msg" );
			String title = ResourceMgr.getInstance().getUIString
				( "project.new.one.entry.title" );
			JOptionPane.showMessageDialog
				( this, msg, title, JOptionPane.ERROR_MESSAGE );
			return null;
			}
		else
			{
			dirEntry = entries.entryAt( 0 );
			}

		String entryLocal = dirEntry.getLocalDirectory();
		String entryRepos = dirEntry.getRepository();

		File dirF = null;
		if ( entryLocal.equals( "./" ) )
			{
			// Root Entry...
			dirF = new File
				( CVSCUtilities.exportPath
					( this.project.getLocalRootDirectory() ) );
			}
		else
			{
			dirF = new File
				( CVSCUtilities.exportPath
						( this.project.getLocalRootDirectory() ),
					CVSCUtilities.exportPath( entryLocal.substring(2) ) );
			}

		String prompt = ResourceMgr.getInstance().getUIString
			( "project.new.files.dialog.prompt" );

		NewFilesDialog dlg =
			new NewFilesDialog( this, true, prompt );

		dlg.refreshFileList( dirF, dirEntry );

		dlg.show();
  
		String[] files = dlg.getSelectedFiles();
		if ( files.length == 0 )
			return null;

		CVSEntryVector result = new CVSEntryVector();

		for ( int i = 0 ; i < files.length ; ++i )
			{
			CVSEntry entry =
				this.createAddFileEntry
					( files[i], entryLocal, entryRepos );
			result.appendEntry( entry );
			}

		return result;
		}

	public CVSEntryVector
	getUserSelectedFile()
		{
		String localPath;
		CVSEntryVector result = null;

		String prompt =
			ResourceMgr.getInstance().getUIString
				( "project.addfile.prompt" );

		FileDialog dialog = new
			FileDialog( this, prompt, FileDialog.LOAD );

		if ( this.lastUserFileDir != null )
			{
			localPath = this.lastUserFileDir;
			}
		else
			{
			localPath =
				CVSCUtilities.exportPath
					( this.project.getLocalRootDirectory() );
			}

		dialog.setDirectory( localPath );

		dialog.show();

		String fileName = dialog.getFile();
		
		if ( fileName != null )
			{
			this.lastUserFileDir = dialog.getDirectory();

			localPath =
				CVSCUtilities.ensureFinalSlash
					( CVSCUtilities.importPath( this.lastUserFileDir ) );

			String rootRepos =
				CVSCUtilities.ensureFinalSlash
					( this.project.getRootDirectory() );

			String localRootDir =
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
				String entryRepos = rootRepos + entryLocal;

				entryLocal =
					CVSCUtilities.ensureFinalSlash
						( "./" + entryLocal );

				CVSEntry entry =
					this.createAddFileEntry
						( fileName, entryLocal, entryRepos );

				result.addElement( entry );
				}
			else
				{
				String[] fmtArgs = { localPath, localRootDir };
				String msg = ResourceMgr.getInstance().getUIFormat
					( "project.add.not.subtree.msg", fmtArgs );
				String title = ResourceMgr.getInstance().getUIString
					( "project.add.not.subtree.title" );
				JOptionPane.showMessageDialog
					( this, msg, title, JOptionPane.ERROR_MESSAGE );
				}
			}

		return result;
		}

	protected void
	displayFinalResults( boolean ok )
		{
		String resultLine;
		StringBuffer finalResult = new StringBuffer("");

		if ( ok )
			{
			resultLine = ResourceMgr.getInstance().getUIString
				( "project.fdbk.result.ok" );
			}
		else
			{
			resultLine = ResourceMgr.getInstance().getUIString
				( "project.fdbk.result.err" );
			}

		if ( ! ok || this.displayStderr.length() > 0
				|| this.displayStdout.length() > 0 )
			{
			if ( this.displayStderr.length() > 0 )
				{
				finalResult.append( this.displayStderr );
				if ( this.displayStdout.length() > 0 )
					finalResult.append( "\n" );
				}

			if ( this.displayStdout.length() > 0 )
				{
				finalResult.append( this.displayStdout );
				}

			finalResult.append( "\n" + resultLine );

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

	protected void
	displayPrettyDiffs( boolean ok )
		{
		String resultLine =
			ResourceMgr.getInstance().getUIString( "project.fdbk.result.ok" );

		// NOTE
		// REVIEW
		// The only time that the stdout has zero length is when there was
		// an error, in which case stderr will have a non-zero length. If
		// both stdout and stderr are empty, then there were no diffs. In
		// the case of an error, we will use the normal results display.

		if ( ! ok || displayStderr.length() > 0 )
			{
			this.displayFinalResults( ok );
			resultLine = ResourceMgr.getInstance().getUIString
				( "project.fdbk.result.err" );
			}
		else if ( ok && this.displayStdout.length() > 0 )
			{
			PrettyDiffFrame diffFrame =
				new PrettyDiffFrame( this, "Diffs", null, this.displayStdout, null, null );

			Dimension sz = this.getSize();
			Point loc = this.getLocationOnScreen();
			Rectangle defBounds =
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
		int			indent = 21;
		Dimension	sz = this.getSize();
		JButton		button;
		JLabel		label;
		
		UserPrefs prefs = Config.getPreferences();

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

		Font argFont =
			prefs.getFont
				( "projectWindow.argumentFont",
					new Font( "Monospaced", Font.BOLD, 12 ) );

		this.argumentText = new JTextArea();
		this.argumentText.setEditable( true );
		this.argumentText.setBackground( Color.white );
 		this.argumentText.setFont( argFont );

		this.argumentText.setVisible( true );
		this.argumentText.setBorder( new LineBorder( Color.black ) );

		String lblStr = ResourceMgr.getInstance().getUIString
			( "project.arguments.label" );
		JLabel argsLbl = new JLabel( lblStr );
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
			Image iEraser =
				AWTUtilities.getImageResource
					( "/com/ice/jcvsii/images/icons/eraser.gif" );
			Icon eraserIcon = new ImageIcon( iEraser );
			eraserButton = new JButton( eraserIcon )
				{
				public boolean isFocusTraversable() { return false; }
				};
			eraserButton.setOpaque( false );
			eraserButton.setMargin( new Insets( 1,1,1,1 ) );
			}
		catch ( IOException ex )
			{
			eraserButton = new JButton( "x" );
			}

		String tipStr = ResourceMgr.getInstance().getUIString
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

		JPanel feedPan = new JPanel();
		feedPan.setLayout( new BorderLayout( 0, 0 ) );
		feedPan.setBorder( new EtchedBorder( EtchedBorder.RAISED ) );

		feedPan.add( "Center", this.feedback );

		/* ============ Final Layout =============== */

		Container content = this.getContentPane();

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
		MenuShortcut	accel;

		ResourceMgr rmgr = ResourceMgr.getInstance();

		this.mBar = new JMenuBar();

		name = rmgr.getUIString( "menu.projW.file.name" );
		this.mFile = new JMenu( name );
		this.mBar.add( this.mFile );

		name = rmgr.getUIString( "menu.projW.file.hide.name" );
		mItem = new JMenuItem( name );
		this.mFile.add( mItem );
		mItem.addActionListener( this );
		mItem.setActionCommand( "JCVS:HideOutputWindow" );
		mItem.setAccelerator
			( KeyStroke.getKeyStroke
				( KeyEvent.VK_H, Event.CTRL_MASK ) );

		name = rmgr.getUIString( "menu.projW.file.show.name" );
		mItem = new JMenuItem( name );
		this.mFile.add( mItem );
		mItem.addActionListener( this );
		mItem.setActionCommand( "JCVS:ShowOutputWindow" );
		mItem.setAccelerator
			( KeyStroke.getKeyStroke
				( KeyEvent.VK_S, Event.CTRL_MASK ) );

		this.mFile.addSeparator();

		name = rmgr.getUIString( "menu.projW.file.trace.name" );
		this.traceCheckItem = new JCheckBoxMenuItem( name );
		this.mFile.add( this.traceCheckItem );
		this.traceCheckItem.setState( this.traceReq );
		this.traceCheckItem.addActionListener( this );
		this.traceCheckItem.setActionCommand( "JCVS:TRACE" );
		this.traceCheckItem.setAccelerator
			( KeyStroke.getKeyStroke
				( KeyEvent.VK_T, Event.CTRL_MASK ) );

		this.mFile.addSeparator();

		if ( this.project.isPServer() || this.project.isSSHServer() )
			{
			name = rmgr.getUIString( "menu.projW.file.login.name" );
			mItem = new JMenuItem( name );
			this.mFile.add( mItem );
			mItem.addActionListener( this );
			mItem.setActionCommand( "JCVS:PerformLogin" );
			mItem.setAccelerator
				( KeyStroke.getKeyStroke
					( KeyEvent.VK_L, Event.CTRL_MASK ) );

			this.mFile.addSeparator();
			}

		name = rmgr.getUIString( "menu.projW.file.details.name" );
		mItem = new JMenuItem( name );
		this.mFile.add( mItem );
		mItem.addActionListener( this );
		mItem.setActionCommand( "JCVS:ShowDetails" );

		name = rmgr.getUIString( "menu.projW.file.addto.name" );
		mItem = new JMenuItem( name );
		this.mFile.add( mItem );
		mItem.addActionListener( this );
		mItem.setActionCommand( "JCVS:AddToWorkBench" );

		this.mFile.addSeparator();

		name = rmgr.getUIString( "menu.projW.file.close.name" );
		mItem = new JMenuItem( name );
		this.mFile.add( mItem );
		mItem.addActionListener( this );
		mItem.setActionCommand( "Close" );
		mItem.setAccelerator
			( KeyStroke.getKeyStroke
				( KeyEvent.VK_W, Event.CTRL_MASK ) );

		this.addAdditionalMenus( mBar );

		String menuBarStr =
			Config.getPreferences().getProperty( "projectMenuBar", null );

		if ( menuBarStr != null )
			{
			this.buildCommandMenus( menuBarStr );
			}

		this.setJMenuBar( this.mBar );
		}

	// This is intended to be overwritten by subclasses!
	public void
	addAdditionalMenus( JMenuBar menuBar )
		{
		}

	private void
	buildCommandMenus( String menuBarSpec )
		{
		int			mIdx, iIdx;
		String		itemString;
		String		menuString;
		String[]	menuList;
		String[]	itemList;

		UserPrefs prefs = Config.getPreferences();

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

			JMenu menu = new JMenu( menuList[mIdx], true );

			for ( iIdx = 0 ; iIdx < itemList.length ; ++iIdx )
				{
				itemString =
					prefs.getProperty
						( "projectMenuItem."
							+ menuList[mIdx] + "." + itemList[iIdx], null );

				if ( itemString == null )
					{
					CVSLog.logMsg
						( "ProjectFrame.buildCommandMenus: Menu '"
							+ menuList[mIdx] + "' is missing item string '"
							+ itemList[iIdx] + "'" );
					continue;
					}

				int colonIdx = itemString.indexOf( ':' );
				if ( colonIdx < 0 )
					{
					CVSLog.logMsg
						( "CVSProjectFrame.buildCommandMenus: Menu '"
							+ menuList[mIdx] + "' Item '" + itemList[iIdx]
							+ "' has an invalid definition [title:cvs]." );
					continue;
					}

				String title = itemString.substring( 0, colonIdx );
				String command = itemString.substring( colonIdx + 1 );

				if ( title.equals( "-" ) )
					{
					menu.addSeparator();
					}
				else
					{
					JMenuItem mItem = new JMenuItem( title );
					mItem.setActionCommand( command );
					mItem.addActionListener
						( new ActionListener()
							{
							public void
							actionPerformed( ActionEvent e )
								{
								SwingUtilities.invokeLater
									( ProjectFrame.this.new PJInvoker( e ) );
								}
							}
						);
					menu.add( mItem );
					}
				}

			this.mBar.add( menu );
			} 
		}

	private
	class		PJInvoker
	implements	Runnable
		{
		private ActionEvent event;
		public
		PJInvoker( ActionEvent e )
			{
			this.event = e;
			}

		public void
		run()
			{
			ProjectFrame.this.actionPerformed( this.event );
			}
		}

	//
	// CVS USER INTERFACE METHODS
	//

	public void
	uiDisplayProgressMsg( String message )
		{
		this.showFeedback( message );
		}

	public void
	uiDisplayProgramError( String error )
		{
		CVSLog.logMsg( error );
		CVSUserDialog.Error( error );
		CVSTracer.traceWithStack
			( "CVSProjectFrame.uiDisplayProgramError: " + error );
		}

	public void
	uiDisplayResponse( CVSResponse response )
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
			String name =
				ProjectFrame.getProjectDisplayName
					( this.project, this.project.getLocalRootDirectory() );

			this.output =
				new OutputFrame( this, name + " Output" );

			Dimension sz = this.getSize();
			Point loc = this.getLocationOnScreen();

			Rectangle defBounds =
				new Rectangle( loc.x + 15, loc.y + 15, sz.width, sz.height );

			this.output.loadPreferences( defBounds );
			}
		}

	public void
	outputIsClosing()
		{
		this.output = null;
		}

	public boolean
	setRedirectWriter( CVSRequest request )
		{
		boolean result = true;

		FileDialog dialog = new
			FileDialog( this, "Redirect Output", FileDialog.SAVE );

		String localDir =
			CVSCUtilities.exportPath
				( this.project.getLocalRootDirectory() );

		dialog.setDirectory( localDir );

		dialog.show();

		String dirName = dialog.getDirectory();
		String fileName = dialog.getFile();
		
		if ( dirName != null && fileName != null )
			{
			File outputFile =
				new File( dirName, fileName );
		
			PrintWriter	pWriter = null;

			try {
				pWriter = new PrintWriter
					( new FileWriter( outputFile ) );
				}
			catch ( IOException ex )
				{
				pWriter = null;
				result = false;
				String[] fmtArgs =
					{ outputFile.getPath(), ex.getMessage() };
				String msg = ResourceMgr.getInstance().getUIFormat
					( "project.redirect.failed.msg", fmtArgs );
				String title = ResourceMgr.getInstance().getUIString
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
 
	protected String
	requestMessageArgument( String prompt )
		{
		MessageDialog dlg =
			new MessageDialog( this, true, prompt );

		dlg.show();

		return dlg.getMessage();
		}

	private static String
	getProjectDisplayName( CVSProject project, String localRootPath )
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
					|| path.endsWith( File.separator + "." ) )
				{
				path = path.substring( 0, path.length() - 2 );
				}

			name = CVSUtilities.getFileName( path );
			}

		return name;
		}

	public static void
	openProject( File rootDirFile, String password )
		{
		Config cfg = Config.getInstance();
		UserPrefs prefs = cfg.getPreferences();

		CVSClient client = CVSUtilities.createCVSClient();
		CVSProject project = new CVSProject( client );

		project.setTempDirectory( cfg.getTemporaryDirectory() );

		project.setAllowsGzipFileMode
			( prefs.getBoolean( Config.GLOBAL_ALLOWS_FILE_GZIP, true ) );

		project.setGzipStreamLevel
			( prefs.getInteger( Config.GLOBAL_GZIP_STREAM_LEVEL, 0 ) );

		try {
			project.openProject( rootDirFile );

			int cvsPort =
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

			String name =
				ProjectFrame.getProjectDisplayName
					( project, rootDirFile.getPath() );

			String title = name + " Project";

			ProjectFrame frame = new ProjectFrame( title, project );

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
		catch ( IOException ex )
			{
			String[] fmtArgs =
				{ rootDirFile.getPath(), ex.getMessage() };
			String msg = ResourceMgr.getInstance().getUIFormat
				( "project.openproject.failed.msg", fmtArgs );
			String title = ResourceMgr.getInstance().getUIString
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
	getUserSelectedProject( Frame parent, String prompt, String initDir )
		{
		String result = null;

		UserPrefs prefs = Config.getPreferences();

		for ( ; ; )
			{
			FileDialog dialog = new
				FileDialog( parent, prompt, FileDialog.LOAD );

			dialog.setFile( "Entries" );

			if ( initDir != null )
				dialog.setDirectory( initDir );

			dialog.show();

			String fileName = dialog.getFile();
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
					String[] fmtArgs = { fileName, dirName };
					String msg = ResourceMgr.getInstance().getUIFormat
						( "project.select.verify.failed.msg", fmtArgs );
					String title = ResourceMgr.getInstance().getUIString
						( "project.select.verify.failed.title" );
					JOptionPane.showMessageDialog
						( parent, msg, title, JOptionPane.ERROR_MESSAGE );
					}
				}
			else
				{
				String msg = ResourceMgr.getInstance().getUIString
					( "project.select.help.msg" );
				String title = ResourceMgr.getInstance().getUIString
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

	public void
	processUnknownFiles( String cvsCommandSpec )
		{
		Vector unks = new Vector();
		Vector mods = new Vector();
		Vector adds = new Vector();
		Vector rems = new Vector();
		CVSIgnore ignore = new CVSIgnore();
		Config cfg = Config.getInstance();
		UserPrefs prefs = cfg.getPreferences();

		String userIgnores =
			prefs.getProperty( Config.GLOBAL_USER_IGNORES, null );

		// this finds all the unknown files
		this.project.checkReleaseStatus( ignore, mods, adds, rems, unks );

		if ( unks.size() > 0 )
			{
			String root = this.project.getLocalRootDirectory();
			if ( root == null )
				return;

			root = root.trim();
			char ch = root.charAt( root.length()-1 );
			if ( ch != '/' && ch != '\\' && ch != File.separatorChar )
				root = root + File.separatorChar;

			// sift out directories and files from the unknowns list
			int size = unks.size();
			Vector unkFiles = new Vector();
			for ( int i=0 ; i < size ; i++ )
				{
				String nextStr = (String) unks.elementAt(i);
				if ( nextStr.startsWith( "./" ) )
					nextStr = nextStr.substring( 2, nextStr.length() );

				File f = new File( root + nextStr );
				if ( f.exists() && ! f.isDirectory() )
					{
					unkFiles.add( f );
					}
				}

			if ( unkFiles.size() > 0 )
				{
				// this displays a dialog for the user to choose which ones to delete
				UnknownFilesDialog dialog =
					new UnknownFilesDialog( this, unkFiles, this.getTitle(), false );

				if ( dialog.isCancelAction() )
					{
					showFeedback( "User cancelled." );
					}
				else
					{
					File array[] = dialog.selectFiles();
					if ( array.length == 0 )
						{
						showFeedback( "No selection to operate on." );
						}
					else if ( dialog.isDeleteAction() )
						{
						// this deletes selected files
						for ( int i = 0 ; i < array.length ; i++ )
							{
							if ( array[i].delete() )
								{
								String msg =
									"File "+array[i].getAbsolutePath()+" deleted.";

								this.showFeedback( msg );
								}
							else
								{
								String msg =
									"Failed to delete file "
									+ array[i].getAbsolutePath() + ".";

								this.showFeedback( msg );
								}
							}

						this.showFeedback
							( "Finished deleting selected unknown files." );
						}
					else if ( dialog.isAddAction() )
						{
						CVSEntryVector entries = new CVSEntryVector();

						for ( int i=0 ; i < array.length ; i++ )
							{
							CVSEntry tempEntry = this.toCVSEntry( array[i] );
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
			String msg = ResourceMgr.getInstance().getUIString
				( "project.no.unknowns.msg" );
			String title = ResourceMgr.getInstance().getUIString
				( "project.no.unknowns.title" );

			JOptionPane.showMessageDialog
				( this, msg, title, JOptionPane.INFORMATION_MESSAGE );
			}
		}

	public void
	processUnknownDirs()
		{
		Vector unks = new Vector();
		Vector mods = new Vector();
		Vector adds = new Vector();
		Vector rems = new Vector();
		CVSIgnore ignore = new CVSIgnore();
		Config cfg = Config.getInstance();
		UserPrefs prefs = cfg.getPreferences();
		StringBuffer resultBuffer = new StringBuffer( 4 * 1024 );

		String userIgnores =
			prefs.getProperty( Config.GLOBAL_USER_IGNORES, null );

		// this finds all the unknown files
		this.project.checkReleaseStatus( ignore, mods, adds, rems, unks );

		if ( unks.size() > 0 )
			{
			String root = this.project.getLocalRootDirectory();
			if ( root == null )
				return;

			root = root.trim();
			char ch = root.charAt( root.length()-1 );
			if ( ch != '/' && ch != '\\' && ch != File.separatorChar )
				root = root + File.separatorChar;

			// sift out directories and files from the unknowns list
			int size = unks.size();
			Vector unkDirs = new Vector();
			for ( int i=0 ; i < size ; i++ )
				{
				String nextStr = (String) unks.elementAt(i);
				if ( nextStr.startsWith( "./" ) )
					nextStr = nextStr.substring( 2, nextStr.length() );

				File f = new File( root + nextStr );
				if ( f.exists() && f.isDirectory() )
					{
					unkDirs.add( f );
					}
				}

			//
			// DIRECTORIES
			//
			if ( unkDirs.size() > 0 )
				{
				// this displays a dialog for the user to choose which ones to delete
				UnknownFilesDialog dialog =
					new UnknownFilesDialog( this, unkDirs,
						"Directories in " + this.getTitle(), true );

				if ( dialog.isCancelAction() )
					{
					showFeedback( "User cancelled." );
					}
				else
					{
					File array[] = dialog.selectFiles();
					if ( array.length == 0 )
						{
						showFeedback( "No selection to operate on." );
						}
					else
						{
						for ( int i = 0 ; i < array.length ; i++ )
							{
							String path = array[i].getPath();
							if ( CVSCUtilities.isSubpathInPath( root, path ) )
								{
								String dirPath = path.substring( root.length() );
								dirPath = CVSCUtilities.importPath( dirPath );
								this.addUnknownDirectory( resultBuffer, dirPath );
								}
							else
								{
								String msg =
									"Directory " + path
									+ " not under root directory!";

								resultBuffer.append( msg ).append( "\n" );
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
			String msg = ResourceMgr.getInstance().getUIString
				( "project.no.unknowns.msg" );
			String title = ResourceMgr.getInstance().getUIString
				( "project.no.unknowns.title" );

			JOptionPane.showMessageDialog
				( this, msg, title, JOptionPane.INFORMATION_MESSAGE );
			}
		}

	protected void
	addUnknownDirectory( StringBuffer resultBuffer, String dirPath )
		{
		CVSResponse		result;
		StringBuffer	addPath = new StringBuffer();

		CVSEntry dirEntry = this.project.getRootEntry();

		// The root entry has a 'bad' fullname, so we use getLocalDirectory().
		addPath.append( dirEntry.getLocalDirectory() + dirPath );
		// Make sure that it ends with "/."
		if ( addPath.charAt( addPath.length() - 1 ) != '/' )
			{
			addPath.append( "/" );
			}
		addPath.append( "." );

		result =
			this.project.ensureRepositoryPath
				( this, addPath.toString(), new CVSResponse() );

		resultBuffer.append( result.getResultText() );
		}

	/**
	 * Given a local file, return a CVSEntry describing it.
	 */

	public CVSEntry
	toCVSEntry( File f )
		{
		CVSEntry result = null;
		String fileName = f.getName();

		if ( fileName != null )
			{
			String localPath =
				CVSCUtilities.ensureFinalSlash
					( CVSCUtilities.importPath( f.getParent() ) );

			String repos = this.project.getRepository();

			String rootDir =
				CVSCUtilities.ensureFinalSlash
					( this.project.getRootDirectory() );

			String localRootDir =
				CVSCUtilities.ensureFinalSlash
					( this.project.getLocalRootDirectory() );

			if ( CVSCUtilities.isSubpathInPath( localRootDir, localPath ) )
				{
				String entryLocal = localPath.substring( localRootDir.length() );
				String entryRepos = rootDir + repos + "/" + entryLocal;

				entryLocal =
					CVSCUtilities.ensureFinalSlash
						( "./" + /* repos + "/" + */ entryLocal );

				result = this.createAddFileEntry( fileName, entryLocal, entryRepos );
				}
			}

		return result;
		}

	}
