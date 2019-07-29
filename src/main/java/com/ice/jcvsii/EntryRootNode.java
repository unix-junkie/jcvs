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

import com.ice.cvsc.CVSEntry;


public
class		EntryRootNode
extends		EntryNode
	{
	protected String		localRoot;
	protected EntryTree		tree;


	public
	EntryRootNode( final CVSEntry entry, final String rootPath )
		{
		super( entry );
		this.tree = null;
		this.localRoot = rootPath;
		}

	/**
	 * Returns this node's CVSEntry.
	 */
	public String
	getLocalRootPath()
		{
		return this.localRoot;
		}

	/**
	 * Returns this node's CVSEntry.
	 */
	public EntryTree
	getEntryTree()
		{
		return this.tree;
		}

	/**
	 * Returns this node's CVSEntry.
	 */
	public void
	setEntryTree( final EntryTree tree )
		{
		this.tree = tree;
		}

	@Override
	public String
	toString()
		{
		return "[EntryRootNode tree=" +tree+ "]";
		}

    }

