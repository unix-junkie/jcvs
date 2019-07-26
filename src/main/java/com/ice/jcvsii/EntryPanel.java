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

import java.io.*;
import java.awt.*;
import java.awt.Font;
import java.awt.event.*;
import java.util.Enumeration;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.tree.*;

import com.ice.cvsc.*;
import com.ice.pref.UserPrefs;
import com.ice.pref.MenuPrefs;
import com.ice.util.AWTUtilities;
import com.ice.util.JFCUtilities;


public
class		EntryPanel
extends		JPanel
implements	ActionListener, FocusListener,
			PropertyChangeListener, ColumnHeader.ResizeListener
	{
	private ColumnHeader		entriesHeader;
	private EntryTree			entriesTree;
	private EntryTreeModel		entriesModel;
	private EntryColumnModel	columnModel;
	private JScrollPane			entriesScroller;

	private JPopupMenu			dirPopup;
	private JPopupMenu			filePopup;

	private ActionListener		popupListener;

	private Border				actBorder;
	private Border				deActBorder;


	public
	EntryPanel( CVSEntry rootEntry, String localRoot, ActionListener popupListener )
		{
		super();

		this.setLayout( new GridBagLayout() );

		EntryRootNode rootNode =
			new EntryRootNode( rootEntry, localRoot );

		this.columnModel = new EntryColumnModel();

		this.entriesModel = new EntryTreeModel( rootNode );

		this.entriesTree =
			new EntryTree( this.entriesModel, this.columnModel );

		UserPrefs prefs = Config.getPreferences();

		String lineStyle =
			prefs.getProperty( Config.PROJECT_TREE_LINESTYLE, "Angled" );
		
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
			( Config.PROJECT_TREE_FONT, this );

		prefs.addPropertyChangeListener
			( Config.PROJECT_TREE_LINESTYLE, this );

		prefs.addPropertyChangeListener
			( Config.PROJECT_MODIFIED_FORMAT, this );
		}

	public void
	loadPreferences( UserPrefs prefs )
		{
		int w, totalW = 0;

		Font f =
			prefs.getFont
				( Config.PROJECT_TREE_FONT, this.getFont() );

		this.entriesTree.setFont( f );

		w = prefs.getInteger( Config.PROJECT_NAME_WIDTH, 275 );
		this.columnModel.setNameWidth( w );
		totalW += w;

		w = prefs.getInteger( Config.PROJECT_VERSION_WIDTH, 50 );
		this.columnModel.setVersionWidth( w );
		totalW += w;

		w = prefs.getInteger( Config.PROJECT_MODIFIED_WIDTH, 175 );
		this.columnModel.setModifiedWidth( w );
		totalW += w;

		this.entriesTree.resetCachedSizes();
		this.entriesTree.revalidate();
		this.entriesHeader.revalidate();

		this.repaint();
		}

	public void
	savePreferences( UserPrefs prefs )
		{
		prefs.setInteger
			( Config.PROJECT_NAME_WIDTH,
				this.columnModel.getNameWidth() );

		prefs.setInteger
			( Config.PROJECT_VERSION_WIDTH,
				this.columnModel.getVersionWidth() );

		prefs.setInteger
			( Config.PROJECT_MODIFIED_WIDTH,
				this.columnModel.getModifiedWidth() );

		Config.getPreferences().removePropertyChangeListener
			( Config.PROJECT_TREE_FONT, this );

		Config.getPreferences().removePropertyChangeListener
			( Config.PROJECT_TREE_LINESTYLE, this );

		Config.getPreferences().removePropertyChangeListener
			( Config.PROJECT_MODIFIED_FORMAT, this );

		ToolTipManager.sharedInstance().
			unregisterComponent( this.entriesTree );
		}

	public void
	propertyChange( PropertyChangeEvent evt )
		{
		String propName = evt.getPropertyName();
		UserPrefs p = (UserPrefs) evt.getSource();
		if ( propName.equals( Config.PROJECT_TREE_FONT ) )
			{
			Font f =
				( p.getFont
					( Config.PROJECT_TREE_FONT,
						this.entriesTree.getFont() ) );

			this.entriesTree.setFont( f );
			this.entriesTree.revalidate();
			this.entriesTree.repaint();
			}
		else if ( propName.equals( Config.PROJECT_TREE_LINESTYLE ) )
			{
			this.entriesTree.putClientProperty
				( "JTree.lineStyle", evt.getNewValue() );
			this.entriesTree.repaint();
			}
		else if ( propName.equals( Config.PROJECT_MODIFIED_FORMAT ) )
			{
			this.entriesTree.resetDisplayCaches();
			this.entriesTree.repaint();
			}
		}

	public void
	focusGained( FocusEvent e )
		{
		this.entriesScroller.setBorder( this.actBorder );
		}

	public void
	focusLost( FocusEvent e )
		{
		this.entriesScroller.setBorder( this.deActBorder );
		}

	public void
	clearSelection()
		{
		this.entriesTree.clearSelection();
		}

	public void
	clearSelection( TreePath selPath )
		{
		this.entriesTree.removeSelectionPath( selPath );
		}

	public void
	selectAll()
		{
		this.selectAll( this.entriesModel.getEntryRootNode() );
		}

	public void
	selectAll( EntryNode root )
		{
		// NOTE This call to getChildCount() is REQUIRED
		//      in order to get the child nodes loaded so
		//      that the children() enumerator will not be
		//      empty.
		int cnt = root.getChildCount();
	
		TreePath rootPath = new TreePath( root.getPath() );
		this.entriesTree.expandPath( rootPath );

		Enumeration enum = root.children();
		for ( ; enum.hasMoreElements() ; )
			{
			EntryNode node = (EntryNode) enum.nextElement();
			if ( node.isLeaf() )
				{
				TreePath path = new TreePath( node.getPath() );
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
	selectModified( EntryNode root )
		{
		// NOTE This call to getChildCount() is REQUIRED
		//      in order to get the child nodes loaded so
		//      that the children() enumerator will not be
		//      empty.
		int cnt = root.getChildCount();
		Enumeration enum = root.children();
		for ( ; enum.hasMoreElements() ; )
			{
			EntryNode node = (EntryNode) enum.nextElement();
			if ( node.isLeaf() )
				{
				CVSEntry entry = node.getEntry();
				if ( entry.isLocalFileModified( node.getLocalFile() )
						|| entry.isNewUserFile()
						|| entry.isToBeRemoved()
						|| entry.isInConflict() )	
					{
					TreePath path = new TreePath( node.getPath() );
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
	expandAll( boolean expand )
		{
		EntryNode root = this.entriesModel.getEntryRootNode();
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
			Enumeration enum = root.depthFirstEnumeration();
			for ( ; enum.hasMoreElements() ; )
				{
				EntryNode node = (EntryNode) enum.nextElement();
				if ( node == root )
					continue;
				if ( ! node.isLeaf() )
					this.entriesTree.collapsePath
						( new TreePath( node.getPath() ) );
				}
			}
		}

	public void
	expandAll( EntryNode root )
		{
		// NOTE getChildCount() is used to force load the children.
		int cnt = root.getChildCount();
		Enumeration enum = root.children();
		for ( ; enum.hasMoreElements() ; )
			{
			EntryNode node = (EntryNode) enum.nextElement();
			if ( ! node.isLeaf() )
				{
				this.entriesTree.expandPath
					( new TreePath( node.getPath() ) );
				this.expandAll( node );
				}
			}
		}

	public void
	actionPerformed( ActionEvent event )
		{
		Object	source = event.getSource();
	    String command = event.getActionCommand();

		if ( source instanceof JMenuItem
				&& this.popupListener != null )
			{
			TreePath[] selPaths = this.getSelectionPaths();
			
			// There had BETTER be one and only one.
			if ( selPaths != null && selPaths.length > 0 )
				{
				EntryNode node = (EntryNode)
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
				char selectCh = command.charAt(0);
				command = command.substring(2);
				CVSEntryVector vector = new CVSEntryVector();

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
					ActionEvent aEvent = new ActionEvent
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
	public void
	columnHeadersNeedUpdate( ColumnHeader.ResizeEvent event )
		{
		this.entriesTree.revalidate();
		this.entriesTree.repaint();
		}

	public void
	columnHeadersResized( ColumnHeader.ResizeEvent event )
		{
		this.entriesTree.resetCachedSizes();
		this.entriesTree.revalidate();
		}
	//
	// COLUMN RESIZE LISTENER INTERFACE END
	//

	public void
	setRoot( TreeNode root )
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
	setTreeEntries( CVSEntry root )
		{
		this.setRoot( new EntryNode( root ) );
		this.entriesTree.repaint( 500 );
		}

	private EntryNode
	getSelectedNode()
		{
		EntryNode result = null;

		TreePath path = this.entriesTree.getSelectionPath();

		if ( path != null )
			{
			Object obj = path.getLastPathComponent();
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

		EntryNode[] result = new EntryNode[ paths.length ];
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

		public void
		mousePressed( MouseEvent event )
			{
			this.isPopupClick = false;

			if ( event.isPopupTrigger() )
				{
				int selRow =
					entriesTree.getRowForLocation
						( event.getX(), event.getY() );

				this.isPopupClick = true;

				if ( selRow != -1 )
					{
					entriesTree.setSelectionRow( selRow );

					JPopupMenu popup =
						( getSelectedNode().isLeaf()
							? filePopup
							: dirPopup );

					Point pt =
						JFCUtilities.computePopupLocation
							( event, (Component) event.getSource(), popup );

					popup.show( entriesTree, pt.x, pt.y );
					}
				}
			}

		public void
		mouseReleased( MouseEvent event )
			{
			if ( this.isPopupClick )
				return;

			if ( event.isPopupTrigger() )
				{
				int selRow =
					entriesTree.getRowForLocation
						( event.getX(), event.getY() );

				this.isPopupClick = true;

				if ( selRow != -1 )
					{
					entriesTree.setSelectionRow( selRow );

					JPopupMenu popup =
						( getSelectedNode().isLeaf()
							? filePopup
							: dirPopup );

					Point pt =
						JFCUtilities.computePopupLocation
							( event, (Component) event.getSource(), popup );

					popup.show( entriesTree, pt.x, pt.y );
					}
				}
			}

		public void
		mouseClicked( MouseEvent event )
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
			EntryNode node = getSelectedNode();

			if ( node == null )
				return;

			if ( ! node.isLeaf() )
				return;

			File selF = node.getLocalFile();

			String verb =
				Config.getPreferences().getProperty
					( Config.PROJECT_DOUBLE_CLICK_VERB, "open" );

			JAFUtilities.openFile
				( node.getEntry().getName(), selF, verb );
			}
		}

	}

