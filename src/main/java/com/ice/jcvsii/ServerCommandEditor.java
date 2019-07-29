/*
** Java cvs client application package.
** Copyright (c) 1999 by Timothy Gerard Endres
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

import com.ice.config.editor.ConfigTupleTableEditor;
import com.ice.pref.PrefsTuple;
import com.ice.pref.PrefsTupleTable;


public
class		ServerCommandEditor
extends		ConfigTupleTableEditor
	{
	private static int cntr = 0;

	public
	ServerCommandEditor()
		{
		super( "Server Commands" );
		}

	private String
	getNewName()
		{
		cntr++;
		return "cvs" + cntr + ".domain.com";
		}

	private void
	createTable()
		{
		final String[] val = { "cvs server" };
		final PrefsTuple tup = new PrefsTuple( this.getNewName(), val );
		final PrefsTupleTable table = new PrefsTupleTable();
		table.appendTuple( tup );
		this.model.setData( table );
		}

	public void
	insertElement()
		{
		if ( this.model.getData() == null )
			{
			this.createTable();
			this.table.setRowSelectionInterval( 0, 0 );
			}
		else
			{
			final String[] vals = { "" };
			final int row = this.table.getSelectedRow();
			final PrefsTuple tup = new PrefsTuple( this.getNewName(), vals );
			this.model.insertElement( tup, row );
			this.table.setRowSelectionInterval( row, row );
			}

		this.table.repaint( 250 );
		}

	public void
	appendElement()
		{
		if ( this.model.getData() == null )
			{
			this.createTable();
			this.table.setRowSelectionInterval( 0, 0 );
			}
		else
			{
			final String[] vals = { "" };
			final PrefsTuple tup = new PrefsTuple( this.getNewName(), vals );
			this.model.appendElement( tup );
			final int row = this.model.getRowCount() - 1;
			this.table.setRowSelectionInterval( row, row );
			}

		this.table.repaint( 250 );
		}

	public void
	deleteElement()
		{
		if ( this.model.getData() == null )
			return;
		else
			super.deleteElement();
		}

	}

