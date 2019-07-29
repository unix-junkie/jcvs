/*
** User Preferences Package.
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

package com.ice.pref;

import java.util.Vector;

import com.ice.util.StringUtilities;

public
class		PrefsTuple
	{
	protected String		key = null;
	protected String[]		values = null;

	private
	PrefsTuple()
		{
		}

	public
	PrefsTuple( final String key, final String[] values )
		{
		super();
		this.key = key;
		this.values = values;
		}

	public
	PrefsTuple( final String key, final Vector values )
		{
		super();
		this.key = key;
		this.values = new String[ values.size() ];
		values.copyInto( this.values );
		}

	public boolean
	equals( final PrefsTuple that )
		{
		if ( ! this.key.equals( that.key ) )
			return false;

		if ( this.values.length != that.values.length )
			return false;

		for ( int i = 0 ; i < this.values.length ; ++i )
			if ( ! this.values[i].equals( that.values[i] ) )
				return false;

		return true;
		}

	public String
	getKey()
		{
		return this.key;
		}

	public String[]
	getValues()
		{
		return this.values;
		}

	public void
	setValues( final String[] values )
		{
		this.values = values;
		}

	public String
	getValueAt( final int idx )
		{
		return this.values[ idx ];
		}

	public void
	setValueAt( final String value, final int idx )
		{
		this.values[ idx ] = value;
		}

	public int
	length()
		{
		return
			this.values == null ? 0 : this.values.length;
		}

	public String
	toString()
		{
		return
			"PrefsTuple[key="
			+ this.key
			+ ", values=["
			+ StringUtilities.join( this.values, "," )
			+ "]]";
		}

	}

