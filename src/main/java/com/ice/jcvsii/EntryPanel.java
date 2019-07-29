/*
** Copyright (c) 1998 by Timothy Gerard Endres
** <mailto:time@ice.com>  <http://www.ice.com>
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
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Collections;
import java.util.Enumeration;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ToolTipManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.ice.cvsc.CVSEntry;
import com.ice.cvsc.CVSEntryVector;
import com.ice.pref.MenuPrefs;
import com.ice.pref.UserPrefs;
import com.ice.util.JFCUtilities;


public
class		EntryPanel
extends		JPanel
implements	ActionListener, FocusListener,
			PropertyChangeListener, ColumnHeader.ResizeListener
	{
	private final ColumnHeader		entriesHeader;
	private final EntryTree			entriesTree;
	private final EntryTreeModel		entriesModel;
	private final EntryColumnModel	columnModel;
	private final JScrollPane			entriesScroller;

	private final JPopupMenu			dirPopup;
	private final JPopupMenu			filePopup;

	private final ActionListener		popupListener;

	private final Border				actBorder;
	private final Border				deActBorder;


	public
	EntryPanel( final CVSEntry rootEntry, final String localRoot, final ActionListener popupListener )
		{
		super();

		this.setLayout( new GridBagLayout() );

		final EntryRootNode rootNode =
			new EntryRootNode( rootEntry, localRoot );

		this.columnModel = new EntryColumnModel();

		this.entriesModel = new EntryTreeModel( rootNode );

		this.entriesTree =
			new EntryTree( this.entriesModel, this.columnModel );

		final UserPrefs prefs = Config.getPreferences();

		final String lineStyle =
			prefs.getProperty( ConfigConstants.PROJECT_TREE_LINESTYLE, "Angled" );

		this.entriesTree.putClientProperty
			( "JTree.lineStyle", lineStyle );

		this.popupListener = popupListener;

		this.dirPopup =
			MenuPrefs.loadPopupMenu
				( prefs, "dirPopup", this );

		this.filePopup =
			MenuPrefs.loadPopupMenu
				( prefs, "filePopup", this );

		this.entriesTree.addMouseListener( this.new EntryPanelMouser() );

		rootNode.setEntryTree( this.entriesTree );

		this.entriesHeader = new ColumnHeader( this.columnModel );
		this.entriesHeader.addResizeListener( this );

		this.actBorder =
			new CompoundBorder
				( new EtchedBorder( EtchedBorder.RAISED ),
					new LineBorder( Color.black, 1 ) );

		this.deActBorder =
			new CompoundBorder
				( new EtchedBorder( EtchedBorder.RAISED ),
					new EmptyBorder( 1, 1, 1, 1 ) );

		this.entriesScroller = new JScrollPane( this.entriesTree );
		this.entriesScroller.setColumnHeaderView( this.entriesHeader );
		this.entriesScroller.setBorder( this.deActBorder );

		this.setLayout( new BorderLayout() );
		this.add( "Center", this.entriesScroller );

		this.entriesTree.addFocusListener( this );
		ToolTipManager.sharedInstance().registerComponent( this.entriesTree );

		prefs.addPropertyChangeListener
			( ConfigConstants.PROJECT_TREE_FONT, this );

		prefs.addPropertyChangeListener
			( ConfigConstants.PROJECT_TREE_LINESTYLE, this );

		prefs.addPropertyChangeListener
			( ConfigConstants.PROJECT_MODIFIED_FORMAT, this );
		}

	public void
	loadPreferences( final UserPrefs prefs )
		{
		int w, totalW = 0;

		final Font f =
			prefs.getFont
				( ConfigConstants.PROJECT_TREE_FONT, this.getFont() );

		this.entriesTree.setFont( f );

		w = prefs.getInteger( ConfigConstants.PROJECT_NAME_WIDTH, 275 );
		this.columnModel.setNameWidth( w );
		totalW += w;

		w = prefs.getInteger( ConfigConstants.PROJECT_VERSION_WIDTH, 50 );
		this.columnModel.setVersionWidth( w );
		totalW += w;

		w = prefs.getInteger( ConfigConstants.PROJECT_TAG_WIDTH, 50 );
		this.columnModel.setTagWidth( w );
		totalW += w;

		w = prefs.getInteger( ConfigConstants.PROJECT_MODIFIED_WIDTH, 175 );
		this.columnModel.setModifiedWidth( w );
		totalW += w;

		this.entriesTree.resetCachedSizes();
		this.entriesTree.revalidate();
		this.entriesHeader.revalidate();

		this.repaint();
		}

	public void
	savePreferences( final UserPrefs prefs )
		{
		prefs.setInteger
			( ConfigConstants.PROJECT_NAME_WIDTH,
				this.columnModel.getNameWidth() );

		prefs.setInteger
			( ConfigConstants.PROJECT_VERSION_WIDTH,
				this.columnModel.getVersionWidth() );

		prefs.setInteger
			( ConfigConstants.PROJECT_TAG_WIDTH,
				this.columnModel.getTagWidth() );

		prefs.setInteger
			( ConfigConstants.PROJECT_MODIFIED_WIDTH,
				this.columnModel.getModifiedWidth() );

		Config.getPreferences().removePropertyChangeListener
			( ConfigConstants.PROJECT_TREE_FONT, this );

		Config.getPreferences().removePropertyChangeListener
			( ConfigConstants.PROJECT_TREE_LINESTYLE, this );

		Config.getPreferences().removePropertyChangeListener
			( ConfigConstants.PROJECT_MODIFIED_FORMAT, this );

		ToolTipManager.sharedInstance().
			unregisterComponent( this.entriesTree );
		}

	@Override
	public void
	propertyChange( final PropertyChangeEvent evt )
		{
		final String propName = evt.getPropertyName();
		final UserPrefs p = (UserPrefs) evt.getSource();
		if ( propName.equals( ConfigConstants.PROJECT_TREE_FONT ) )
			{
			final Font f =
				p.getFont
				( ConfigConstants.PROJECT_TREE_FONT,
					this.entriesTree.getFont() );

			this.entriesTree.setFont( f );
			this.entriesTree.revalidate();
			this.entriesTree.repaint();
			}
		else if ( propName.equals( ConfigConstants.PROJECT_TREE_LINESTYLE ) )
			{
			this.entriesTree.putClientProperty
				( "JTree.lineStyle", evt.getNewValue() );
			this.entriesTree.repaint();
			}
		else if ( propName.equals( ConfigConstants.PROJECT_MODIFIED_FORMAT ) )
			{
			this.entriesTree.resetDisplayCaches();
			this.entriesTree.repaint();
			}
		}

	@Override
	public void
	focusGained( final FocusEvent e )
		{
		this.entriesScroller.setBorder( this.actBorder );
		}

	@Override
	public void
	focusLost( final FocusEvent e )
		{
		this.entriesScroller.setBorder( this.deActBorder );
		}

	public void
	clearSelection()
		{
		this.entriesTree.clearSelection();
		}

	public void
	clearSelection( final TreePath selPath )
		{
		this.entriesTree.removeSelectionPath( selPath );
		}

	public void
	selectAll()
		{
		this.selectAll( this.entriesModel.getEntryRootNode() );
		}

	public void
	selectAll( final EntryNode root )
		{
		// NOTE This call to getChildCount() is REQUIRED
		//      in order to get the child nodes loaded so
		//      that the children() enumerator will not be
		//      empty.
		final int cnt = root.getChildCount();

		final TreePath rootPath = new TreePath( root.getPath() );
		this.entriesTree.expandPath( rootPath );

		for ( final EntryNode node : Collections.list((Enumeration<EntryNode>) root.children()) )
			{
			if ( node.isLeaf() )
				{
				final TreePath path = new TreePath( node.getPath() );
				this.entriesTree.addSelectionPath( path );
			//	this.entriesTree.expandPath( path );
				}
			else
				{
				this.selectAll( node );
				}
			}
		}

	public void
	selectModified()
		{
		// Can not use the depth enumeration method for expansion,
		// as it only traverses *open* nodes!
		this.selectModified( this.entriesModel.getEntryRootNode() );
		}

	public void
	selectModified( final EntryNode root )
		{
		// NOTE This call to getChildCount() is REQUIRED
		//      in order to get the child nodes loaded so
		//      that the children() enumerator will not be
		//      empty.
		final int cnt = root.getChildCount();
		for ( final EntryNode node : Collections.list((Enumeration<EntryNode>) root.children()) )
			{
			if ( node.isLeaf() )
				{
				final CVSEntry entry = node.getEntry();
				if ( entry.isLocalFileModified( node.getLocalFile() )
						|| entry.isNewUserFile()
						|| entry.isToBeRemoved()
						|| entry.isInConflict() )
					{
					final TreePath path = new TreePath( node.getPath() );
					this.entriesTree.addSelectionPath( path );
					this.entriesTree.expandPath( path );
					}
				}
			else
				{
				this.selectModified( node );
				}
			}
		}

	public void
	expandAll( final boolean expand )
		{
		final EntryNode root = this.entriesModel.getEntryRootNode();
		// Can not use the enumeration method for expansion, as it
		// only traverses *open* nodes! 8^)
		//
		if ( expand )
			{
			this.expandAll( root );
			}
		else
			{
			// If the tree is not totally expanded,
			// this approach quicker.
			for ( final EntryNode node : Collections.list((Enumeration<EntryNode>) root.depthFirstEnumeration()) )
				{
				if ( node == root )
					continue;
				if ( ! node.isLeaf() )
					this.entriesTree.collapsePath
						( new TreePath( node.getPath() ) );
				}
			}
		}

	public void
	expandAll( final EntryNode root )
		{
		// NOTE getChildCount() is used to force load the children.
		final int cnt = root.getChildCount();
		for ( final EntryNode node : Collections.list((Enumeration<EntryNode>) root.children()) )
			{
			if ( ! node.isLeaf() )
				{
				this.entriesTree.expandPath
					( new TreePath( node.getPath() ) );
				this.expandAll( node );
				}
			}
		}

	@Override
	public void
	actionPerformed( final ActionEvent event )
		{
		final Object	source = event.getSource();
	    String command = event.getActionCommand();

		if ( source instanceof JMenuItem
				&& this.popupListener != null )
			{
			final TreePath[] selPaths = this.getSelectionPaths();

			// There had BETTER be one and only one.
			if ( selPaths != null && selPaths.length > 0 )
				{
				final EntryNode node = (EntryNode)
					selPaths[0].getLastPathComponent();

				//
				// Dir popup command specs have an extra field at the start.
				// It is a single character followed by a colon. As follows:
				// 'D' add the directory popped up on as the entry;
				// 'L' add the directory's entry list as the entries.
				// 'R' add the directory's entry tree (Recurse) as the entries.
				// 'F' add the file popped up on as the entry.
				// 'X' add the dir entry and the dir's file entries.
				//
				// The 'X' is used when the command is using the 'execute in dir'
				// option. This option requires the first entry to be the dir that
				// is to be "executed within". However, the entries MUST follow for
				// commands such as 'update', or else we will cause the bug of
				// update overwriting all the locally modified files, since it
				// does not know they are there!!!
				//
				final char selectCh = command.charAt(0);
				command = command.substring(2);
				final CVSEntryVector vector = new CVSEntryVector();

				if ( selectCh == 'L' )
					{
					node.getEntry().addFileEntries( vector );
					}
				else if ( selectCh == 'R' )
					{
					node.getEntry().addAllSubTreeEntries( vector );
					}
				else if ( selectCh == 'X' )
					{
					vector.addElement( node.getEntry() );
					node.getEntry().addFileEntries( vector );
					}
				else if ( selectCh == 'Z' )
					{
					vector.addElement( node.getEntry() );
					node.getEntry().addAllSubTreeEntries( vector );
					}
				else // 'D' and 'F'
					{
					vector.addElement( node.getEntry() );
					}

				if ( this.popupListener != null )
					{
					final ActionEvent aEvent = new ActionEvent
						( vector, ActionEvent.ACTION_PERFORMED,
							"POPUP:" + command );

					this.popupListener.actionPerformed( aEvent );
					}
				}
			}
		}

	//
	// COLUMN RESIZE LISTENER INTERFACE BEGIN
	//
	@Override
	public void
	columnHeadersNeedUpdate( final ColumnHeader.ResizeEvent event )
		{
		this.entriesTree.revalidate();
		this.entriesTree.repaint();
		}

	@Override
	public void
	columnHeadersResized( final ColumnHeader.ResizeEvent event )
		{
		this.entriesTree.resetCachedSizes();
		this.entriesTree.revalidate();
		}
	//
	// COLUMN RESIZE LISTENER INTERFACE END
	//

	public void
	setRoot( final TreeNode root )
		{
		this.entriesModel.setRoot( root );
		}

	public EntryRootNode
	getRootNode()
		{
		return this.entriesModel.getEntryRootNode();
		}

	public TreePath[]
	getSelectionPaths()
		{
		return this.entriesTree.getSelectionPaths();
		}

	public void
	setTreeEntries( final CVSEntry root )
		{
		this.setRoot( new EntryNode( root ) );
		this.entriesTree.repaint( 500 );
		}

	private EntryNode
	getSelectedNode()
		{
		EntryNode result = null;

		final TreePath path = this.entriesTree.getSelectionPath();

		if ( path != null )
			{
			final Object obj = path.getLastPathComponent();
			result = (EntryNode) obj;
			}

		return result;
		}

	private EntryNode[]
	getSelectedNodes()
		{
		TreePath[] paths = this.entriesTree.getSelectionPaths();
		if ( paths == null )
			paths = new TreePath[0];

		final EntryNode[] result = new EntryNode[ paths.length ];
		for ( int i = 0 ; i < paths.length ; ++i )
			{
			result[i] = (EntryNode) paths[i].getLastPathComponent();
			}

		return result;
		}

	private class
	EntryPanelMouser extends MouseAdapter
		{
		private boolean		isPopupClick = false;

		public
		EntryPanelMouser()
			{
			super();
			}

		@Override
		public void
		mousePressed( final MouseEvent event )
			{
			this.isPopupClick = false;

			if ( event.isPopupTrigger() )
				{
				final int selRow =
					entriesTree.getRowForLocation
						( event.getX(), event.getY() );

				this.isPopupClick = true;

				if ( selRow != -1 )
					{
					entriesTree.setSelectionRow( selRow );

					final JPopupMenu popup =
						getSelectedNode().isLeaf()
						? filePopup
						: dirPopup;

					final Point pt =
						JFCUtilities.computePopupLocation
							( event, (Component) event.getSource(), popup );

					popup.show( entriesTree, pt.x, pt.y );
					}
				}
			}

		@Override
		public void
		mouseReleased( final MouseEvent event )
			{
			if ( this.isPopupClick )
				return;

			if ( event.isPopupTrigger() )
				{
				final int selRow =
					entriesTree.getRowForLocation
						( event.getX(), event.getY() );

				this.isPopupClick = true;

				if ( selRow != -1 )
					{
					entriesTree.setSelectionRow( selRow );

					final JPopupMenu popup =
						getSelectedNode().isLeaf()
						? filePopup
						: dirPopup;

					final Point pt =
						JFCUtilities.computePopupLocation
							( event, (Component) event.getSource(), popup );

					popup.show( entriesTree, pt.x, pt.y );
					}
				}
			}

		@Override
		public void
		mouseClicked( final MouseEvent event )
			{
			if ( this.isPopupClick )
				{
				this.isPopupClick = false;
				return;
				}

			if ( event.getClickCount() == 2 )
				{
				this.processDoubleClick();
				}
			}

		private void
		processDoubleClick()
			{
			final EntryNode node = getSelectedNode();

			if ( node == null )
				return;

			if ( ! node.isLeaf() )
				return;

			final File selF = node.getLocalFile();

			final String verb =
				Config.getPreferences().getProperty
					( ConfigConstants.PROJECT_DOUBLE_CLICK_VERB, "open" );

			JAFUtilities.openFile
				( node.getEntry().getName(), selF, verb );
			}
		}

	}

