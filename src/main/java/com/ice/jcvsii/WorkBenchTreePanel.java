
package com.ice.jcvsii;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import com.ice.cvsc.CVSProject;
import com.ice.cvsc.CVSProjectDef;
import com.ice.cvsc.CVSCUtilities;
import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;
import com.ice.event.TreePopupMouseAdapter;


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
	WorkBenchTreePanel( WorkBenchDetailPanel detailPan )
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

	public void
	actionPerformed( ActionEvent event )
		{
		String command = event.getActionCommand();

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
		WorkBenchTreeNode parent = (WorkBenchTreeNode)
			this.tree.getLastSelectedPathComponent();

		if ( parent == null )
			return;

		if ( parent.isLeaf() )
			return;

		String nodePath = parent.getPathString();

		WorkBenchInfoDialog dlg =
			new WorkBenchInfoDialog
				( (Frame) this.getTopLevelAncestor(),
					parent, true, null, nodePath, "" );

		dlg.show();

		WorkBenchDefinition wDef =
			dlg.getWorkBenchDefinition();

		if ( wDef != null )
			{
			WorkBenchTreeNode child =
				new WorkBenchTreeNode( wDef );

			parent.add( child );

			this.model.fireTreeChanged();
			}
		}

	public void
	addNewProject()
		{
		WorkBenchTreeNode parent = (WorkBenchTreeNode)
			this.tree.getLastSelectedPathComponent();

		if ( parent == null )
			return;

		if ( parent.isLeaf() )
			return;

		String prompt =
			ResourceMgr.getInstance().getUIString( "wb.add.project.prompt" );

		String localRootDir =
			ProjectFrame.getUserSelectedProject
				( (Frame)this.getTopLevelAncestor(),
					prompt, null ); // UNDONE initial directory

		if ( localRootDir == null )
			return;

		this.addNewProject( parent, localRootDir );
		}

	public void
	addNewProject( WorkBenchTreeNode parent, String localRootDir )
		{
		localRootDir = CVSCUtilities.importPath( localRootDir );

		String rootFilePath =
			CVSProject.getAdminRootPath
				( CVSProject.rootPathToAdminPath( localRootDir ) );

		String reposFilePath =
			CVSProject.getAdminRepositoryPath
				( CVSProject.rootPathToAdminPath( localRootDir ) );

		File adminRootFile =
			new File( CVSCUtilities.exportPath( rootFilePath ) );

		File adminReposFile =
			new File( CVSCUtilities.exportPath( reposFilePath ) );

		try {
			String rootStr =
				CVSCUtilities.readStringFile( adminRootFile );

			String reposStr =
				CVSCUtilities.readStringFile( adminReposFile );

			CVSProjectDef pDef =
				new CVSProjectDef( rootStr, reposStr );

			if ( ! pDef.isValid() )
				throw new IOException
					( "ERROR parsing project specification, "
						+ pDef.getReason() );

			String nodePath = parent.getPathString();

			String defName = pDef.getRepository();
			int index = defName.lastIndexOf( "/" );
			if ( index > 0 && index < (defName.length() - 1 ) )
				defName = defName.substring( index + 1 );

			WorkBenchInfoDialog dlg =
				new WorkBenchInfoDialog
					( (Frame) this.getTopLevelAncestor(),
						parent, false, defName, nodePath, localRootDir );

			dlg.show();

			WorkBenchDefinition wDef =
				dlg.getWorkBenchDefinition();

			if ( wDef != null )
				{
				WorkBenchTreeNode child =
					new WorkBenchTreeNode( wDef );

				parent.add( child );

				this.model.fireTreeChanged();
				}
			}
		catch ( IOException ex )
			{
			ex.printStackTrace();
			}
		}

	public void
	addProjectToWorkBench( CVSProject project )
		{
		WorkBenchTreeNode parent = (WorkBenchTreeNode)
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
			ResourceMgr rmgr = ResourceMgr.getInstance();
			String msg = rmgr.getUIString( "wb.add.needs.folder.msg" );
			String title = rmgr.getUIString( "wb.add.needs.folder.title" );
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
		ResourceMgr rmgr = ResourceMgr.getInstance();

		WorkBenchTreeNode node = (WorkBenchTreeNode)
			this.tree.getLastSelectedPathComponent();

		if ( ! node.isLeaf() )
			{
			if ( node.getChildCount() > 0 )
				{
				String msg = rmgr.getUIString( "wb.folder.notempty.msg" );
				String title = rmgr.getUIString( "wb.folder.notempty.title" );
				JOptionPane.showMessageDialog
					( this.getTopLevelAncestor(),
						msg, title, JOptionPane.WARNING_MESSAGE );
				return;
				}
			}

		if ( node != null )
			{
			String[] fmtArgs = new String[2];

			fmtArgs[0] = node.toString();
			fmtArgs[1] =
				node.isLeaf()
				? rmgr.getUIString( "name.for.project" )
				: rmgr.getUIString( "name.for.folder" );

			String title = rmgr.getUIString( "wb.confirm.delete.title" );
			String msg = rmgr.getUIFormat( "wb.confirm.delete.msg", fmtArgs );
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
		WorkBenchTreeNode node = (WorkBenchTreeNode)
			this.tree.getLastSelectedPathComponent();

		if ( node != null && node.isLeaf() )
			{
			this.openDefinition( node.getDefinition() );
			}
		}

	private void
	openDefinition( WorkBenchDefinition def )
		{
		String localRoot = def.getLocalDirectory();

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
				ResourceMgr rmgr = ResourceMgr.getInstance();
				String[] fmtArgs = { localRoot };
				String title = rmgr.getUIString
					( "global.invalid.cvsadmin.title" );
				String msg = rmgr.getUIFormat
					( "global.invalid.cvsadmin.msg", fmtArgs );

				JOptionPane.showMessageDialog
					( (Frame)this.getTopLevelAncestor(),
						msg, title, JOptionPane.ERROR_MESSAGE );
				}

			this.tree.setEnabled( true );
			this.setDefaultCursor();
			}
		}

	public void
	focusGained( FocusEvent e )
		{
		this.scroller.setBorder( this.actBorder );
		}

	public void
	focusLost( FocusEvent e )
		{
		this.scroller.setBorder( this.deActBorder );
		}

	public void
	valueChanged( TreeSelectionEvent event )
		{
		WorkBenchTreeNode node = (WorkBenchTreeNode)
			tree.getLastSelectedPathComponent();

		if ( node == null )
			{
			detailPan.clearDefinition();
			this.dblClickAction.setEnabled( false );
			}
		else
			{
			WorkBenchDefinition def = node.getDefinition();
			detailPan.showDefinition( def );
			this.dblClickAction.setEnabled( true );
			}
		}

	public void
	addTreeSelectionListener( TreeSelectionListener l )
		{
		this.tree.addTreeSelectionListener( l );
		}

	public void
	removeTreeSelectionListener( TreeSelectionListener l )
		{
		this.tree.removeTreeSelectionListener( l );
		}

	private void
	establishContents()
		{
		JLabel lbl;

		this.setLayout( new BorderLayout() );

		ResourceMgr rmgr = ResourceMgr.getInstance();
		String display = rmgr.getUIString( "wb.rootnode.display" );
		String desc = rmgr.getUIString( "wb.rootnode.desc" );

		WorkBenchDefinition def =
			new WorkBenchDefinition( "root", "", display, desc );

		WorkBenchTreeNode rootNode = new WorkBenchTreeNode( def );

		this.model = new WorkBenchTreeModel( rootNode );

		this.tree = new JTree( this.model );
		this.tree.addTreeSelectionListener( this );
		this.tree.setShowsRootHandles( true );
		this.tree.setScrollsOnExpand( false );
		this.tree.addFocusListener( this );

		this.tree.putClientProperty( "JTree.lineStyle", "Angled" );

		DefaultTreeCellRenderer defRend =
			new DefaultTreeCellRenderer()
				{
				/**  
				 * Overrides return slightly taller preferred size value.
				 */
				public Dimension
				getPreferredSize()
					{
					Dimension result = super.getPreferredSize();
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
				public void
				actionPerformed( ActionEvent event )
					{ openSelection(); }
				};

		this.dblClickAction.setEnabled( false );

		this.tree.addMouseListener
			( new TreePopupMouseAdapter
				( this.tree, null, null, this.dblClickAction, "OPEN" ) );

		UserPrefs prefs = Config.getPreferences();

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
		Container frame = this.getTopLevelAncestor();
		MainFrame.setWaitCursor( frame, true );
	//	this.getTopLevelAncestor().setCursor
	//		( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );
		}

	private void
	setDefaultCursor()
		{
		Container frame = this.getTopLevelAncestor();
		MainFrame.setWaitCursor( frame, false );
	//	this.getTopLevelAncestor().setCursor
	//		( Cursor.getDefaultCursor() );
		}

	}

