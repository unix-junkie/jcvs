
package com.ice.event;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;


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
			( final JTree tree, final JPopupMenu nodePopup,
				final JPopupMenu leafPopup, final Action action )
		{
		this( tree, nodePopup, leafPopup, action, "DoubleClick" );
		}

	public
	TreePopupMouseAdapter
			( final JTree tree, final JPopupMenu nodePopup,
				final JPopupMenu leafPopup, final Action action, final String command )
		{
		super();
		this.tree = tree;
		this.action = action;
		this.leafPopup = leafPopup;
		this.nodePopup = nodePopup;
		this.actionCommand = command;
		}

	public void
	setActionCommand( final String command )
		{
		this.actionCommand = command;
		}

	@Override
	public void
	mousePressed( final MouseEvent event )
		{
		this.isPopupClick = false;

		if ( event.isPopupTrigger() )
			{
			final int selRow =
				this.tree.getRowForLocation
					( event.getX(), event.getY() );

			this.isPopupClick = true;

			if ( selRow != -1 )
				{
				doPopup( selRow, event.getX(), event.getY() );
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
				this.tree.getRowForLocation
					( event.getX(), event.getY() );

			this.isPopupClick = true;

			if ( selRow != -1 )
				{
				doPopup( selRow, event.getX(), event.getY() );
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
	doPopup( final int row, final int x, final int y )
		{
		this.tree.setSelectionRow( row );

		final TreePath path = this.tree.getPathForRow( row );
		final TreeNode node = (TreeNode) path.getLastPathComponent();

		final JPopupMenu popup =
			node.isLeaf() ? this.leafPopup : this.nodePopup;

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
						final ActionEvent event =
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

