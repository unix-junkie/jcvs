
package com.ice.jcvsii;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;

import com.ice.cvsc.CVSCUtilities;
import com.ice.cvsc.CVSProject;
import com.ice.cvsc.CVSProjectDef;
import com.ice.event.TreePopupMouseAdapter;
import com.ice.pref.UserPrefs;


public
class		WorkBenchTreePanel
extends		JPanel
implements	ActionListener, FocusListener, TreeSelectionListener
	{
	protected JTree			tree;
	protected Border		actBorder;
	protected Border		deActBorder;
	protected JScrollPane	scroller;

	protected WorkBenchTreeModel	model;
	protected WorkBenchDetailPanel	detailPan;
	protected AbstractAction		dblClickAction;


	public
	WorkBenchTreePanel( final WorkBenchDetailPanel detailPan )
		{
		super();
		this.detailPan = detailPan;
		this.establishContents();
		}

	public void
	loadPreferences()
		{
		}

	public void
	savePreferences()
		{
		this.model.saveWorkBench( Config.getPreferences() );
		}

	@Override
	public void
	actionPerformed( final ActionEvent event )
		{
		final String command = event.getActionCommand();

		if ( command.equals( "OPEN" ) )
			{
			this.openSelection(); // REVIEW
			}
		}

	public WorkBenchTreeNode
	getSelectedNode()
		{
		return (WorkBenchTreeNode)
			this.tree.getLastSelectedPathComponent();
		}

	public void
	addNewFolder()
		{
		final WorkBenchTreeNode parent = (WorkBenchTreeNode)
			this.tree.getLastSelectedPathComponent();

		if ( parent == null )
			return;

		if ( parent.isLeaf() )
			return;

		final String nodePath = parent.getPathString();

		final WorkBenchInfoDialog dlg =
			new WorkBenchInfoDialog
				( (Frame) this.getTopLevelAncestor(),
					parent, true, null, nodePath, "" );

		dlg.show();

		final WorkBenchDefinition wDef =
			dlg.getWorkBenchDefinition();

		if ( wDef != null )
			{
			final WorkBenchTreeNode child =
				new WorkBenchTreeNode( wDef );

			parent.add( child );

			this.model.fireTreeChanged();
			}
		}

	public void
	addNewProject()
		{
		final WorkBenchTreeNode parent = (WorkBenchTreeNode)
			this.tree.getLastSelectedPathComponent();

		if ( parent == null )
			return;

		if ( parent.isLeaf() )
			return;

		final String prompt =
			ResourceMgr.getInstance().getUIString( "wb.add.project.prompt" );

		final String localRootDir =
			ProjectFrame.getUserSelectedProject
				( (Frame)this.getTopLevelAncestor(),
					prompt, null ); // UNDONE initial directory

		if ( localRootDir == null )
			return;

		this.addNewProject( parent, localRootDir );
		}

	public void
	addNewProject( final WorkBenchTreeNode parent, String localRootDir )
		{
		localRootDir = CVSCUtilities.importPath( localRootDir );

		final String rootFilePath =
			CVSProject.getAdminRootPath
				( CVSProject.rootPathToAdminPath( localRootDir ) );

		final String reposFilePath =
			CVSProject.getAdminRepositoryPath
				( CVSProject.rootPathToAdminPath( localRootDir ) );

		final File adminRootFile =
			new File( CVSCUtilities.exportPath( rootFilePath ) );

		final File adminReposFile =
			new File( CVSCUtilities.exportPath( reposFilePath ) );

		try {
			final String rootStr =
				CVSCUtilities.readStringFile( adminRootFile );

			final String reposStr =
				CVSCUtilities.readStringFile( adminReposFile );

			final CVSProjectDef pDef =
				new CVSProjectDef( rootStr, reposStr );

			if ( ! pDef.isValid() )
				throw new IOException
					( "ERROR parsing project specification, "
						+ pDef.getReason() );

			final String nodePath = parent.getPathString();

			String defName = pDef.getRepository();
			final int index = defName.lastIndexOf( "/" );
			if ( index > 0 && index < defName.length() - 1 )
				defName = defName.substring( index + 1 );

			final WorkBenchInfoDialog dlg =
				new WorkBenchInfoDialog
					( (Frame) this.getTopLevelAncestor(),
						parent, false, defName, nodePath, localRootDir );

			dlg.show();

			final WorkBenchDefinition wDef =
				dlg.getWorkBenchDefinition();

			if ( wDef != null )
				{
				final WorkBenchTreeNode child =
					new WorkBenchTreeNode( wDef );

				parent.add( child );

				this.model.fireTreeChanged();
				}
			}
		catch ( final IOException ex )
			{
			ex.printStackTrace();
			}
		}

	public void
	addProjectToWorkBench( final CVSProject project )
		{
		final WorkBenchTreeNode parent = (WorkBenchTreeNode)
			this.tree.getLastSelectedPathComponent();

		boolean showMessage = false;
		if ( parent == null )
			{
			showMessage = true;
			}
		else if ( parent.isLeaf() )
			{
			showMessage = true;
			}

		if ( showMessage )
			{
			final ResourceMgr rmgr = ResourceMgr.getInstance();
			final String msg = rmgr.getUIString( "wb.add.needs.folder.msg" );
			final String title = rmgr.getUIString( "wb.add.needs.folder.title" );
			JOptionPane.showMessageDialog
				( this.getTopLevelAncestor(),
					msg, title, JOptionPane.WARNING_MESSAGE );
			return;
			}

		addNewProject( parent, project.getLocalRootDirectory() );
		}

	public void
	deleteSelection()
		{
		final ResourceMgr rmgr = ResourceMgr.getInstance();

		final WorkBenchTreeNode node = (WorkBenchTreeNode)
			this.tree.getLastSelectedPathComponent();

		if ( ! node.isLeaf() )
			{
			if ( node.getChildCount() > 0 )
				{
				final String msg = rmgr.getUIString( "wb.folder.notempty.msg" );
				final String title = rmgr.getUIString( "wb.folder.notempty.title" );
				JOptionPane.showMessageDialog
					( this.getTopLevelAncestor(),
						msg, title, JOptionPane.WARNING_MESSAGE );
				return;
				}
			}

		if ( node != null )
			{
			final String[] fmtArgs = new String[2];

			fmtArgs[0] = node.toString();
			fmtArgs[1] =
				node.isLeaf()
				? rmgr.getUIString( "name.for.project" )
				: rmgr.getUIString( "name.for.folder" );

			final String title = rmgr.getUIString( "wb.confirm.delete.title" );
			final String msg = rmgr.getUIFormat( "wb.confirm.delete.msg", fmtArgs );
			if ( JOptionPane.showConfirmDialog
					( this.getTopLevelAncestor(), msg, title,
						JOptionPane.YES_OPTION ) == JOptionPane.YES_OPTION )
				{
				node.removeFromParent();
				this.model.fireTreeChanged();
				}
			}
		}

	public void
	openSelection()
		{
		final WorkBenchTreeNode node = (WorkBenchTreeNode)
			this.tree.getLastSelectedPathComponent();

		if ( node != null && node.isLeaf() )
			{
			this.openDefinition( node.getDefinition() );
			}
		}

	private void
	openDefinition( final WorkBenchDefinition def )
		{
		final String localRoot = def.getLocalDirectory();

		if ( ! ProjectFrameMgr.checkProjectOpen( localRoot ) )
			{
			this.tree.setEnabled( false );
			this.setWaitCursor();

			if ( CVSProject.verifyAdminDirectory
					( CVSProject.rootPathToAdminPath( localRoot ) ) )
				{
				ProjectFrame.openProject
					( new File( localRoot ), null );
				}
			else
				{
				final ResourceMgr rmgr = ResourceMgr.getInstance();
				final String[] fmtArgs = { localRoot };
				final String title = rmgr.getUIString
					( "global.invalid.cvsadmin.title" );
				final String msg = rmgr.getUIFormat
					( "global.invalid.cvsadmin.msg", fmtArgs );

				JOptionPane.showMessageDialog
					( this.getTopLevelAncestor(),
						msg, title, JOptionPane.ERROR_MESSAGE );
				}

			this.tree.setEnabled( true );
			this.setDefaultCursor();
			}
		}

	@Override
	public void
	focusGained( final FocusEvent e )
		{
		this.scroller.setBorder( this.actBorder );
		}

	@Override
	public void
	focusLost( final FocusEvent e )
		{
		this.scroller.setBorder( this.deActBorder );
		}

	@Override
	public void
	valueChanged( final TreeSelectionEvent event )
		{
		final WorkBenchTreeNode node = (WorkBenchTreeNode)
			tree.getLastSelectedPathComponent();

		if ( node == null )
			{
			detailPan.clearDefinition();
			this.dblClickAction.setEnabled( false );
			}
		else
			{
			final WorkBenchDefinition def = node.getDefinition();
			detailPan.showDefinition( def );
			this.dblClickAction.setEnabled( true );
			}
		}

	public void
	addTreeSelectionListener( final TreeSelectionListener l )
		{
		this.tree.addTreeSelectionListener( l );
		}

	public void
	removeTreeSelectionListener( final TreeSelectionListener l )
		{
		this.tree.removeTreeSelectionListener( l );
		}

	private void
	establishContents()
		{
		final JLabel lbl;

		this.setLayout( new BorderLayout() );

		final ResourceMgr rmgr = ResourceMgr.getInstance();
		final String display = rmgr.getUIString( "wb.rootnode.display" );
		final String desc = rmgr.getUIString( "wb.rootnode.desc" );

		final WorkBenchDefinition def =
			new WorkBenchDefinition( "root", "", display, desc );

		final WorkBenchTreeNode rootNode = new WorkBenchTreeNode( def );

		this.model = new WorkBenchTreeModel( rootNode );

		this.tree = new JTree( this.model );
		this.tree.addTreeSelectionListener( this );
		this.tree.setShowsRootHandles( true );
		this.tree.setScrollsOnExpand( false );
		this.tree.addFocusListener( this );

		this.tree.putClientProperty( "JTree.lineStyle", "Angled" );

		final DefaultTreeCellRenderer defRend =
			new DefaultTreeCellRenderer()
				{
				/**
				 * Overrides return slightly taller preferred size value.
				 */
				@Override
				public Dimension
				getPreferredSize()
					{
					final Dimension result = super.getPreferredSize();
					if ( result != null ) result.height += 2;
					return result;
					}
				};

		defRend.setLeafIcon( null );

		this.tree.setCellRenderer( defRend );

		// REVIEW I would like to be able to point to "this"
		// for the actionPerformed() here...
		this.dblClickAction =
			new AbstractAction()
				{
				@Override
				public void
				actionPerformed( final ActionEvent event )
					{ openSelection(); }
				};

		this.dblClickAction.setEnabled( false );

		this.tree.addMouseListener
			( new TreePopupMouseAdapter
				( this.tree, null, null, this.dblClickAction, "OPEN" ) );

		final UserPrefs prefs = Config.getPreferences();

		this.model.loadWorkBench( prefs );

		this.actBorder =
			new CompoundBorder
				( new EtchedBorder( EtchedBorder.RAISED ),
					new LineBorder( Color.black, 1 ) );

		this.deActBorder =
			new CompoundBorder
				( new EtchedBorder( EtchedBorder.RAISED ),
					new EmptyBorder( 1, 1, 1, 1 ) );

		this.scroller = new JScrollPane( this.tree );
		this.scroller.setBorder( this.deActBorder );

		this.add( BorderLayout.CENTER, this.scroller );
		}

	private void
	setWaitCursor()
		{
		final Container frame = this.getTopLevelAncestor();
		MainFrame.setWaitCursor( frame, true );
	//	this.getTopLevelAncestor().setCursor
	//		( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );
		}

	private void
	setDefaultCursor()
		{
		final Container frame = this.getTopLevelAncestor();
		MainFrame.setWaitCursor( frame, false );
	//	this.getTopLevelAncestor().setCursor
	//		( Cursor.getDefaultCursor() );
		}

	}

