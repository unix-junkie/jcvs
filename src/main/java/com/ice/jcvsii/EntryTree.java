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

import java.awt.Font;
import java.awt.Dimension;
import java.awt.Rectangle;
import javax.swing.JTree;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.plaf.TreeUI;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;


public
class		EntryTree
extends		JTree
	{
	protected EntryTreeRenderer	renderer;

	public
	EntryTree( EntryTreeModel model )
		{
		this( model, new EntryColumnModel() );
		}

	public
	EntryTree( EntryTreeModel model, EntryColumnModel columnModel )
		{
		super( model );

		this.setRootVisible( false );
		this.setShowsRootHandles( true );
		this.setScrollsOnExpand( false );
		
		// REVIEW
		// NODE DIMENSION CACHE HACK
		//
		// This is a hack to get around the DAMNED cell bounds caching
		// that the JTree's BasicTreeUI is using. Under this presumably
		// "optimized for display speed" design, all tree cell dimensions
		// are cached when they are first computed. From then one, no
		// matter what the renderer says its dimensions are, the cached
		// dimensions are used! This causes very unhappy clipping effects
		// when we dynamically resize the tree's columns.
		//
		// To get around this clipping problem, we need to tell the tree's
		// UI to reset the cached sizes. Furthermore, this operation MUST
		// be generic enough to work with other UI's, and it MUST be FAST
		// enough to work with dynamic resizing. To fill this need, we are
		// using a fixed row height, and "toggling" the height during the
		// dynamic resizing. This set/reset of the row height clears the
		// cache and does so reasonably fast!
		//
		this.setRowHeight( 18 );

		this.renderer = new EntryTreeRenderer
			( model.getEntryRootNode().getLocalRootPath(), columnModel );

		// REVIEW
		// Another pathetic hack to get around the handle offsets...
		// This code does not work! How do I compress the offset of the
		// file nodes?
		//
	/*
		TreeUI ui = this.getUI();
		if ( ui instanceof BasicTreeUI )
			{
			BasicTreeUI bui = (BasicTreeUI) ui;
			int indent = bui.getLeftChildIndent();
			this.renderer.setHandleIndent( 8 );
			}
	*/

		this.setCellRenderer( this.renderer );
		}

	// We need to override this to avoid calling setPrefferedSize()
	// in EntryPanel.columnHeadersResized(). If we do not override
	// then we get the "jumping column headers" effect. This is stopped
	// by calling setPreferredSize() in columnHeadersResized(). However
	// setting the preferred size appears to limit the height of the
	// EntryTree to the preferred height forever, regardless of the
	// changing height caused by expansion and collapse. I have no
	// idea what the hell is causing that problem!! It must be something
	// related to the viewport tracks view settings in the scroll panel.
	// Anyway, this appears to be preferred regardless here...

	public Dimension
	getPreferredSize()
		{
		Dimension sz = super.getPreferredSize();
		sz.width = this.renderer.getPreferredSize().width + 3; // REVIEW where should 3 come from?
		return sz;
		}

	// REVIEW
	// NODE DIMENSION CACHE HACK
	//
	// This method is used to ensure that any node dimensions cached
	// by this tree are reset so that we get proper clipping during
	// dynamic column resizing. The cheapest and most (apparently)
	// generic fashion for doing this is to "toggle" the tree's
	// row height.
	//

	public void
	resetCachedSizes()
		{
		int h = this.getRowHeight();
		this.setRowHeight( h - 1 );
		this.setRowHeight( h );
		}

	/**
	 * Resets the cached display strings so they will be recomputed.
	 */
	public void
	resetDisplayCaches()
		{
		EntryTreeModel model = (EntryTreeModel) this.getModel();
		model.getEntryRootNode().resetDisplayCaches();
		}

	/**
	 * We override setFont() so we can set the row height to match.
	 */
	public void
	setFont( Font f )
		{
		super.setFont( f );
		int h = f.getSize() + 5; // UNDONE Wish I had font metrics here!!!
		if ( h < 18 ) h = 18;
		this.setRowHeight( h );
		}

	public int
	getNameWidth()
		{
		return this.renderer.getNameWidth();
		}

	public void
	setNameWidth( int w )
		{
		this.renderer.setNameWidth( w );
		Dimension sz = this.renderer.getPreferredSize();
		this.setSize( new Dimension( sz.width, this.getSize().height ) );
		}

	public int
	getVersionWidth()
		{
		return this.renderer.getVersionWidth();
		}

	public void
	setVersionWidth( int w )
		{
		this.renderer.setVersionWidth( w );
		Dimension sz = this.renderer.getPreferredSize();
		this.setSize( new Dimension( sz.width, this.getSize().height ) );
		}

	public int
	getTagWidth()
		{
		return this.renderer.getTagWidth();
		}

	public void
	setTagWidth( int w )
		{
		this.renderer.setTagWidth( w );
		Dimension sz = this.renderer.getPreferredSize();
		this.setSize( new Dimension( sz.width, this.getSize().height ) );
		}

	public int
	getModifiedWidth()
		{
		return this.renderer.getModifiedWidth();
		}

	public void
	setModifiedWidth( int w )
		{
		this.renderer.setModifiedWidth( w );
		Dimension sz = this.renderer.getPreferredSize();
		this.setSize( new Dimension( sz.width, this.getSize().height ) );
		}

	}

