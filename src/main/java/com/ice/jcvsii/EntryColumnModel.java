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

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;


class		EntryColumnModel
extends		DefaultTableColumnModel
	{
	private static final int		IDX_NAME  = 0;
	private static final int		IDX_REV   = 1;
	private static final int		IDX_TAG   = 2;
	private static final int		IDX_MOD   = 3;


	EntryColumnModel()
		{
		String titleStr;
		TableColumn tblCol;

		final ResourceMgr rmgr = ResourceMgr.getInstance();

		titleStr = rmgr.getUIString( "project.tree.entry.header" );
		tblCol = new TableColumn( IDX_NAME, 275 );
		tblCol.setHeaderValue( titleStr );
		this.addColumn( tblCol );

		titleStr = rmgr.getUIString( "project.tree.rev.header" );
		tblCol = new TableColumn( IDX_REV, 50 );
		tblCol.setHeaderValue( titleStr );
		this.addColumn( tblCol );

		titleStr = rmgr.getUIString( "project.tree.tag.header" );
		tblCol = new TableColumn( IDX_TAG, 50 );
		tblCol.setHeaderValue( titleStr );
		this.addColumn( tblCol );

		titleStr = rmgr.getUIString( "project.tree.mod.header" );
		tblCol = new TableColumn( IDX_MOD, 150 );
		tblCol.setHeaderValue( titleStr );
		this.addColumn( tblCol );
		}

	//
	// NOTE
	// WARNING !!!
	//
	// If we do not call **BOTH** setWidth() and setPreferredWidth()
	// in these calls below, a very strange thing happens. Anytime you
	// scroll to the right, then start to resize the column headers,
	// the headers start jumping all over the place, jittery updates,
	// and random sizing. It looks horrible!
	//
	// Well, apparently what is happening is that the JScrollPane is
	// doing something very stupid with respect to the preferred size
	// width of the header being less than the width of the viewport,
	// because the header will never "stay" at the size we set with
	// the dragging resizing, but will continually "resize" down to
	// the preferred.
	//
	// By explicitly setting the preferred width to be the same as
	// the actual width, this strange and undesirable behavior goes
	// away, and the header adjusts smotthly.
	//

	public int
	getNameWidth()
		{
		return this.getColumn(IDX_NAME).getWidth();
		}

	public void
	setNameWidth( final int w )
		{
		this.getColumn(IDX_NAME).setWidth( w );
		this.getColumn(IDX_NAME).setPreferredWidth( w );
		this.recalcWidthCache();
		}

	public int
	getVersionWidth()
		{
		return this.getColumn(IDX_REV).getWidth();
		}

	public void
	setVersionWidth( final int w )
		{
		this.getColumn(IDX_REV).setWidth( w );
		this.getColumn(IDX_REV).setPreferredWidth( w );
		this.recalcWidthCache();
		}

	public int
	getTagWidth()
		{
		return this.getColumn(IDX_TAG).getWidth();
		}

	public void
	setTagWidth( final int w )
		{
		this.getColumn(IDX_TAG).setWidth( w );
		this.getColumn(IDX_TAG).setPreferredWidth( w );
		this.recalcWidthCache();
		}

	public int
	getModifiedWidth()
		{
		return this.getColumn(IDX_MOD).getWidth();
		}

	public void
	setModifiedWidth( final int w )
		{
		this.getColumn(IDX_MOD).setWidth( w );
		this.getColumn(IDX_MOD).setPreferredWidth( w );
		this.recalcWidthCache();
		}

	/**
	 * We had to implement our own column indexer because the
	 * one in ColumnModel was not working for the right hand
	 * edge of the last column. I do not know why, but I am
	 * sure that it was related to the margin, since the
	 * "error" seemed to increase with the number of columns.
	 */

	@Override
	public int
	getColumnIndexAtX( final int x )
		{
		int width = 0;
		final int cnt = this.getColumnCount();
		final int mgn = this.getColumnMargin();

        for ( int colIdx = 0 ; colIdx < cnt ; colIdx++ )
			{
			final TableColumn col = this.getColumn( colIdx );
			width += col.getWidth() + mgn;
			if ( x < width )
				return colIdx;
			}

		return -1;
		}

	/**
	 * We had to implement our own column sizer because the
	 * one in ColumnModel was not working for the right hand
	 * edge of the last column. I do not know why, but I am
	 * sure that it was related to the margin, since the
	 * "error" seemed to increase with the number of columns.
	 */

	@Override
	public int
	getTotalColumnWidth()
		{
		int width = 0;
		final int cnt = this.getColumnCount();
		final int mgn = this.getColumnMargin();

        for ( int colIdx = 0 ; colIdx < cnt ; colIdx++ )
			{
			final TableColumn col = this.getColumn( colIdx );
			width += col.getWidth() + mgn;
			}

		return width;
		}

	}

