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

import java.util.Enumeration;
import com.ice.cvsc.CVSEntry;
import javax.swing.tree.DefaultTreeModel;


public
class		EntryTreeModel
extends		DefaultTreeModel
	{
    public
	EntryTreeModel( EntryRootNode rootEntry )
		{
		super( rootEntry );
		}

	public EntryRootNode
	getEntryRootNode()
		{
		return (EntryRootNode) this.getRoot();
		}

    //
    // The TreeModel interface
    //

	public void
	fireStructureChanged( EntryNode source )
		{
		Object[] path = source.getPath();
		this.fireTreeStructureChanged( source, path, null, null );
		}

	public void
	fireColumnsResized( boolean isResizing )
		{
		this.fireColumnsResized( getEntryRootNode(), isResizing );
		}

	public void
	fireColumnsResized( EntryNode source, boolean isResizing )
		{
		if ( ! isResizing )
			System.err.println( "listenerList: " + this.listenerList );

		if ( source.hasLoadedChildren() )
			{
			Object[] path = source.getPath();
			int len = source.getChildCount();
			int[] ci = new int[ len ];
			for ( int i = 0 ; i < len ; ++i ) ci[i] = i;
			this.fireTreeNodesChanged( source, path, ci, null );

			Enumeration enum = source.children();
			for ( ; enum.hasMoreElements() ; )
				{
				EntryNode cn = (EntryNode) enum.nextElement();
				if ( ! cn.isLeaf() )
					{
					this.fireColumnsResized( cn, isResizing );
					}
				}
			}
		}

	/**
	 * @param source The node that will parent the inserted node.
	 * @param idx The index of the child node being inserted.
	 * @param child The CVSEntry belonging to the child node being inserted.
	 */
	public void
	fireEntryNodeInserted( EntryNode source, int idx, EntryNode child )
		{
		int[] indices = { idx };
		Object[] children = { child };
		Object[] path = source.getPath();
		this.fireTreeNodesInserted( source, path, indices, children );
		}

	/**
	 * @param source The node that will parent the deleted node.
	 * @param idx The index of the child node being deleted.
	 * @param child The CVSEntry belonging to the child node being deleted.
	 */
	public void
	fireEntryNodeRemoved( EntryNode source, int idx, EntryNode child )
		{
		int[] indices = { idx };
		Object[] children = { child };
		Object[] path = source.getPath();
		this.fireTreeNodesRemoved( source, path, indices, children );
		}

	}

