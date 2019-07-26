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

import javax.swing.table.TableColumn;
import javax.swing.table.DefaultTableColumnModel;


public
class		EntryColumnModel
extends		DefaultTableColumnModel
	{
	public
	EntryColumnModel()
		{
		String titleStr;
		TableColumn tblCol;

		ResourceMgr rmgr = ResourceMgr.getInstance();

		titleStr = rmgr.getUIString( "project.tree.entry.header" );
		tblCol = new TableColumn( 0, 275 );
		tblCol.setHeaderValue( titleStr );
		this.addColumn( tblCol );

		titleStr = rmgr.getUIString( "project.tree.rev.header" );
		tblCol = new TableColumn( 1, 50 );
		tblCol.setHeaderValue( titleStr );
		this.addColumn( tblCol );

		titleStr = rmgr.getUIString( "project.tree.mod.header" );
		tblCol = new TableColumn( 2, 150 );
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
		return this.getColumn(0).getWidth();
		}

	public void
	setNameWidth( int w )
		{
		this.getColumn(0).setWidth( w );
		this.getColumn(0).setPreferredWidth( w );
		}

	public int
	getVersionWidth()
		{
		return this.getColumn(1).getWidth();
		}

	public void
	setVersionWidth( int w )
		{
		this.getColumn(1).setWidth( w );
		this.getColumn(1).setPreferredWidth( w );
		}

	public int
	getModifiedWidth()
		{
		return this.getColumn(2).getWidth();
		}

	public void
	setModifiedWidth( int w )
		{
		this.getColumn(2).setWidth( w );
		this.getColumn(2).setPreferredWidth( w );
		}
	}

