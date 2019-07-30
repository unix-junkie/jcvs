
package com.ice.jcvsii;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import com.ice.cvsc.CVSProject;
import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;


public
class		WorkBenchPanel
extends		MainTabPanel
implements	TreeSelectionListener
	{
		private final JSplitPane				splitter;
	private final WorkBenchTreePanel		treePanel;

		private AbstractAction			newFolderAction;
	private AbstractAction			newProjectAction;
	private AbstractAction			openProjectAction;
	private AbstractAction			deleteAction;
	private AbstractAction			browseAction;


	public
	WorkBenchPanel( final MainPanel parent )
		{
		super( parent );

		this.setLayout( new BorderLayout( 0, 3 ) );
		this.setBorder( new EmptyBorder( 1, 4, 4, 4 ) );

		this.establishActions();

			final JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(true );

		this.populateToolbar(toolBar);

			final WorkBenchDetailPanel detailPanel = new WorkBenchDetailPanel();
		detailPanel.setPreferredSize(new Dimension(350, 250 ) );

		this.treePanel =
			new WorkBenchTreePanel(detailPanel);

		this.treePanel.addTreeSelectionListener( this );
		this.treePanel.setPreferredSize( new Dimension( 175, 125 ) );

		this.splitter =
			new JSplitPane
				(JSplitPane.HORIZONTAL_SPLIT,
				 true, this.treePanel, detailPanel);

		this.add(BorderLayout.NORTH, toolBar);

		this.add( BorderLayout.CENTER, this.splitter );
		}

	public void
	loadPreferences()
		{
		final int divLoc =
			Config.getPreferences().getInteger
				( ConfigConstants.MAIN_PANEL_DIVIDER, -1 );

		if ( divLoc > 15 && divLoc < this.getSize().width - 15 )
			this.splitter.setDividerLocation( divLoc );
		else
			this.splitter.setDividerLocation( 175 );

		this.treePanel.loadPreferences();
		}

	@Override
	public void
	savePreferences()
		{
		Config.getPreferences().setInteger
			( ConfigConstants.MAIN_PANEL_DIVIDER,
				this.splitter.getDividerLocation() );

		this.treePanel.savePreferences();
		}

	private void
	browseProject()
		{
		final Config cfg = Config.getInstance();
		final UserPrefs prefs = Config.getPreferences();

		final String prompt =
			ResourceMgr.getInstance().getUIString( "open.project.prompt" );

		final String localRootDirName =
			ProjectFrame.getUserSelectedProject
				( (Frame)this.getTopLevelAncestor(), prompt, null );

		if ( localRootDirName != null )
			{
			if ( ! ProjectFrameMgr.checkProjectOpen( localRootDirName ) )
				{
				final String entriesPath = CVSProject.getAdminEntriesPath
					( CVSProject.rootPathToAdminPath( localRootDirName ) );

				final File entriesFile = new File( entriesPath );
				final File rootDirFile = new File( localRootDirName );

				ProjectFrame.openProject( rootDirFile, null );
				}
			}
		}

	public void
	addProjectToWorkBench( final CVSProject project )
		{
		this.treePanel.addProjectToWorkBench( project );
		}

	@Override
	public void
	valueChanged( final TreeSelectionEvent event )
		{
		final WorkBenchTreeNode node = this.treePanel.getSelectedNode();

		if ( node == null )
			{
			this.deleteAction.setEnabled( false );
			this.newFolderAction.setEnabled( false );
			this.newProjectAction.setEnabled( false );
			this.openProjectAction.setEnabled( false );
			}
		else
			{
			if ( node.isLeaf() )
				{
				this.deleteAction.setEnabled( true );
				this.newFolderAction.setEnabled( false );
				this.newProjectAction.setEnabled( false );
				this.openProjectAction.setEnabled( true );
				}
			else
				{
				this.deleteAction.setEnabled( ! node.isRoot() );
				this.newFolderAction.setEnabled( true );
				this.newProjectAction.setEnabled( true );
				this.openProjectAction.setEnabled( false );
				}
			}
		}

	private void
	establishActions()
		{
		try {
			Image	img;

			img = AWTUtilities.getImageResource
					( "/com/ice/jcvsii/images/icons/browse.gif" );

			if ( img != null )
				{
				this.browseAction =
					new AbstractAction()
						{
						@Override
						public void
						actionPerformed( final ActionEvent event )
							{ browseProject(); }
						};

				this.browseAction.setEnabled( true );
				this.browseAction.putValue
					( Action.SMALL_ICON, new ImageIcon( img ) );
				}

			img = AWTUtilities.getImageResource
					( "/com/ice/jcvsii/images/icons/openproject.gif" );

			if ( img != null )
				{
				// REVIEW
				// UNDONE
				// The "treePanel.openSelection()" below indicates to me
				// that those actions should be coming from the tree panel!!!
				//
				this.openProjectAction =
					new AbstractAction()
						{
						@Override
						public void
						actionPerformed( final ActionEvent event )
							{ treePanel.openSelection(); }
						};

				this.openProjectAction.setEnabled( false );
				this.openProjectAction.putValue
					( Action.SMALL_ICON, new ImageIcon( img ) );
				}

			img = AWTUtilities.getImageResource
					( "/com/ice/jcvsii/images/icons/newfolder.gif" );

			if ( img != null )
				{
				this.newFolderAction =
					new AbstractAction()
						{
						@Override
						public void
						actionPerformed( final ActionEvent event )
							{ treePanel.addNewFolder(); }
						};

				this.newFolderAction.setEnabled( false );
				this.newFolderAction.putValue
					( Action.SMALL_ICON, new ImageIcon( img ) );
				}

			img = AWTUtilities.getImageResource
					( "/com/ice/jcvsii/images/icons/newproject.gif" );

			if ( img != null )
				{
				this.newProjectAction =
					new AbstractAction()
						{
						@Override
						public void
						actionPerformed( final ActionEvent event )
							{ treePanel.addNewProject(); }
						};

				this.newProjectAction.setEnabled( false );
				this.newProjectAction.putValue
					( Action.SMALL_ICON, new ImageIcon( img ) );
				}

			img = AWTUtilities.getImageResource
					( "/com/ice/jcvsii/images/icons/delete.gif" );

			if ( img != null )
				{
				this.deleteAction =
					new AbstractAction()
						{
						@Override
						public void
						actionPerformed( final ActionEvent event )
							{ treePanel.deleteSelection(); }
						};

				this.deleteAction.setEnabled( false );
				this.deleteAction.putValue
					( Action.SMALL_ICON, new ImageIcon( img ) );
				}
			}
		catch ( final IOException ex )
			{
			new Throwable
				( "could not load icon image: " + ex.getMessage() ).
					printStackTrace();
			}
		}

	private void
	populateToolbar( final JToolBar toolBar )
		{
		String tipText;
		final ResourceMgr rmgr = ResourceMgr.getInstance();

		if ( this.browseAction != null )
			{
			tipText = rmgr.getUIString( "wb.icon.browse.tip" );
			toolBar.add( this.browseAction ).setToolTipText( tipText );
			toolBar.addSeparator();
			}

		if ( this.openProjectAction != null )
			{
			tipText = rmgr.getUIString( "wb.icon.open.tip" );
			toolBar.add( this.openProjectAction ).setToolTipText( tipText );
			toolBar.addSeparator();
			}

		if ( this.newFolderAction != null )
			{
			tipText = rmgr.getUIString( "wb.icon.create.tip" );
			toolBar.add( this.newFolderAction ).setToolTipText( tipText );
			toolBar.addSeparator();
			}

		if ( this.newProjectAction != null )
			{
			tipText = rmgr.getUIString( "wb.icon.new.tip" );
			toolBar.add( this.newProjectAction ).setToolTipText( tipText );
			toolBar.addSeparator();
			}

		if ( this.deleteAction != null )
			{
			tipText = rmgr.getUIString( "wb.icon.delete.tip" );
			toolBar.add( this.deleteAction ).setToolTipText( tipText );
			}
		}

	}

