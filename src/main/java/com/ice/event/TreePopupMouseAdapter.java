
package com.ice.event;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import javax.swing.Action;
import javax.swing.JTree;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeNode;


public
class		TreePopupMouseAdapter
extends		MouseAdapter
	{
	private boolean		isPopupClick = false;

	private Action		action = null;
	private String		actionCommand = "DoubleClick";
	private JTree		tree = null;
	private JPopupMenu	nodePopup = null;
	private JPopupMenu	leafPopup = null;


	public
	TreePopupMouseAdapter
			( JTree tree, JPopupMenu nodePopup,
				JPopupMenu leafPopup, Action action )
		{
		this( tree, nodePopup, leafPopup, action, "DoubleClick" );
		}

	public
	TreePopupMouseAdapter
			( JTree tree, JPopupMenu nodePopup,
				JPopupMenu leafPopup, Action action, String command )
		{
		super();
		this.tree = tree;
		this.action = action;
		this.leafPopup = leafPopup;
		this.nodePopup = nodePopup;
		this.actionCommand = command;
		}

	public void
	setActionCommand( String command )
		{
		this.actionCommand = command;
		}

	public void
	mousePressed( MouseEvent event )
		{
		this.isPopupClick = false;

		if ( event.isPopupTrigger() )
			{
			int selRow =
				this.tree.getRowForLocation
					( event.getX(), event.getY() );

			this.isPopupClick = true;

			if ( selRow != -1 )
				{
				doPopup( selRow, event.getX(), event.getY() );
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
				this.tree.getRowForLocation
					( event.getX(), event.getY() );

			this.isPopupClick = true;

			if ( selRow != -1 )
				{
				doPopup( selRow, event.getX(), event.getY() );
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
	doPopup( int row, int x, int y )
		{
		this.tree.setSelectionRow( row );

		TreePath path = this.tree.getPathForRow( row );
		TreeNode node = (TreeNode) path.getLastPathComponent();

		JPopupMenu popup =
			( node.isLeaf() ? this.leafPopup : this.nodePopup );

		if ( popup != null )
			{
			popup.show( this.tree, x, y );
			}
		}

	private void
	processDoubleClick()
		{
		if ( this.action != null && this.action.isEnabled() )
			{
			SwingUtilities.invokeLater
				( new Runnable()
					{
					public void
					run()
						{
						ActionEvent event =
							new ActionEvent
								( tree, ActionEvent.ACTION_PERFORMED,
									actionCommand );

						action.actionPerformed( event );
						}
					}
				);
			}
		}

	}

