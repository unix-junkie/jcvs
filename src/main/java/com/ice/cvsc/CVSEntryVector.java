/*
** Java cvs client library package.
** Copyright (c) 1997-2002 by Timothy Gerard Endres
** 
** This program is free software.
** 
** You may redistribute it and/or modify it under the terms of the GNU
** Library General Public License (LGPL) as published by the Free Software
** Foundation.
**
** Version 2 of the license should be included with this distribution in
** the file LICENSE.txt, as well as License.html. If the license is not
** included	with this distribution, you may find a copy at the FSF web
** site at 'www.gnu.org' or 'www.fsf.org', or you may write to the Free
** Software Foundation at 59 Temple Place - Suite 330, Boston, MA 02111 USA.
**
** THIS SOFTWARE IS PROVIDED AS-IS WITHOUT WARRANTY OF ANY KIND,
** NOT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY. THE AUTHOR
** OF THIS SOFTWARE, ASSUMES _NO_ RESPONSIBILITY FOR ANY
** CONSEQUENCE RESULTING FROM THE USE, MODIFICATION, OR
** REDISTRIBUTION OF THIS SOFTWARE. 
** 
*/

package com.ice.cvsc;

import java.io.*;
import java.lang.*;
import java.util.*;

/**
 * The CVSEntryVector class subclasses Vector to specifically
 * handle CVSEntry ocjects. This subclass adds several convenience
 * methods for adding and retrieving CVSEntry objects quickly.
 *
 * @version $Revision: 2.2 $
 * @author Timothy Gerard Endres, <a href="mailto:time@ice.com">time@ice.com</a>.
 * @see CVSClient
 * @see CVSProject
 */

public class
CVSEntryVector extends Vector
	{
	static public final String		RCS_ID = "$Id: CVSEntryVector.java,v 2.2 1999/04/01 17:48:22 time Exp $";
	static public final String		RCS_REV = "$Revision: 2.2 $";

	static public  boolean			traceLocate = false;
	static public  boolean			traceLocatePath = false;

	/**
	 * Indicates if this entry vector is 'dirty'. If this
	 * is true, then an entry was removed from this vector.
	 * Since that entry can not indicate a dirty condition
	 * because it is gone, we must record that state in the
	 * vector itself. We also pick out the added entry case
	 * in the event that the adder forgot to dirty the newly
	 * added entry.
	 */
	private boolean		isDirty;


	public CVSEntryVector()
		{
		super();
		this.isDirty = false;
		}

	public CVSEntryVector( int initCap )
		{
		super( initCap );
		this.isDirty = false;
		}

	public CVSEntryVector( int initCap, int capIncr )
		{
		super( initCap, capIncr );
		this.isDirty = false;
		}
	
	// UNDONE - finalize should removeAllEntries!!!

	public void
	removeAllEntries()
		{
		// Since we can contain other CVSEntryVector, we
		// need to recurse on those to be sure all is freed!
		//
		for ( int i = 0 ; i < this.size() ; ++i )
			{
			CVSEntry entry = this.entryAt(i);
			if ( entry.isDirectory() )
				{
				entry.removeAllEntries();
				}
			}

		this.removeAllElements();
		}

	public CVSEntry
	entryAt( int index )
		{
		return (CVSEntry) this.elementAt( index );
		}

	public CVSEntry
	getEntryAt( int index )
		{
		return (CVSEntry) this.elementAt( index );
		}

	public void
	appendEntry( CVSEntry entry )
		{
		this.addElement( entry );
		this.isDirty = true;
		}

	private boolean
	removeEntry( CVSEntry entry )
		{
		boolean result;

		result = this.removeElement( entry );
		if ( result )
			{
			this.isDirty = true;
			}

		return result;
		}

	private boolean
	removeEntry( String entryName )
		{
		for ( int i = 0 ; i < this.size() ; ++i )
			{
			CVSEntry entry = this.entryAt(i);

			if ( entryName.equals( entry.getName() ) )
				{
				this.removeElementAt(i);
				this.isDirty = true;
				return true;
				}
			}

		return false;
		}

	/**
	 * Check to see if any entries in this vector are dirty.
	 *
	 * @return If any entry is dirty, returns true, else false.
	 */

	public boolean
	isDirty()
		{
		if ( this.isDirty )
			return true;

		for ( int i = 0 ; i < this.size() ; ++i )
			{
			CVSEntry entry = (CVSEntry)this.elementAt(i);
			if ( entry.isDirty() )
				return true;
			}

		return false;
		}

	/**
	 * Check to see if any entries in this vector are dirty.
	 *
	 * @return If any entry is dirty, returns true, else false.
	 */

	public void
	setDirty( boolean dirty )
		{
		this.isDirty = dirty;

		for ( int i = 0 ; i < this.size() ; ++i )
			{
			CVSEntry entry = (CVSEntry)this.elementAt(i);
			entry.setDirty( dirty );
			}
		}

	/**
	 * Locate an entry in this entry vector with the given name.
	 *
	 * @param name The entry's name (without any path).
	 * @return The entry corresponding to name, or null if not found.
	 */

	public CVSEntry
	locateEntry( String name )
		{
		CVSTracer.traceIf( CVSEntryVector.traceLocate,
			"===== CVSEntryVector.locateEntry: "
			+ "name '" + name + "' =====" );		

		for ( int i = 0 ; i < this.size() ; ++i )
			{
			CVSEntry entry = (CVSEntry)this.elementAt(i);

			CVSTracer.traceIf( CVSEntryVector.traceLocate,
				"CVSEntryVector.locateEntry: ENTRY '"
				+ entry.getFullName()
				+ "' isDir '" + entry.isDirectory() + "'" );

			if ( name.equals( entry.getName() ) )
				{
				CVSTracer.traceIf( CVSEntryVector.traceLocate,
					"CVSEntryVector.locateEntry: '"
					+ entry.getFullName() + "' FOUND." );
				return entry;
				}
			}

		CVSTracer.traceIf( CVSEntryVector.traceLocate,
			"CVSEntryVector.locateEntry: '" + name + "' NOT FOUND." );

		return null;
		}

	}
