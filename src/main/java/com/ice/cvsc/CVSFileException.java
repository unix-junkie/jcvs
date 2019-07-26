/*
** Java cvs client library package.
** Copyright (c) 1997 by Timothy Gerard Endres
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

package com.ice.cvsc;

/**
 * This exception is used to indicate general problems with
 * reading and writing files, copies, renames, etc.
 *
 * @version $Revision: 2.1 $
 * @author Timothy Gerard Endres, <a href="mailto:time@ice.com">time@ice.com</a>.
 */

public class
CVSFileException extends Exception
	{
	static public final String		RCS_ID = "$Id: CVSFileException.java,v 2.1 1997/04/19 05:12:05 time Exp $";
	static public final String		RCS_REV = "$Revision: 2.1 $";

	public
	CVSFileException()
		{
		super();
		}

	public
	CVSFileException( String msg )
		{
		super( msg );
		}
	}
