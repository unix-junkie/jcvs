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

import java.lang.*;
import java.util.*;

/**
 * Implements a Vector subclass that handles CVS Arguments used
 * in CVSRequest objects.
 *
 * @version $Revision: 2.2 $
 * @author Timothy Gerard Endres, <a href="mailto:time@ice.com">time@ice.com</a>.
 * @see CVSClient
 * @see CVSProject
 */

public class CVSArgumentVector extends Vector
	{
	static public final String		RCS_ID = "$Id: CVSArgumentVector.java,v 2.2 1998/07/05 00:02:19 time Exp $";
	static public final String		RCS_REV = "$Revision: 2.2 $";

	public CVSArgumentVector()
		{
		super();
		}

	public CVSArgumentVector( int initCap )
		{
		super( initCap );
		}

	public CVSArgumentVector( int initCap, int capIncr )
		{
		super( initCap, capIncr );
		}

	public String
	argumentAt( int index )
		{
		return (String) this.elementAt( index );
		}

	public void
	appendArgument( String argument )
		{
		this.addElement( argument );
		}

	public void
	appendArguments( Vector args )
		{
		for ( int i = 0, sz = args.size() ; i < sz ; ++i )
			this.addElement( args.elementAt(i) );
		}

	public boolean
	containsArgument( String argument )
		{
		int		i;
		String	argStr;

		for ( i = 0 ; i < this.size() ; ++i )
			{
			argStr = (String) this.elementAt(i);

			if ( argStr.equals( argument ) )
				return true;

			if ( argStr.startsWith( "-" ) )
				{
				++i; // skip this argument's parameter
				}
			}

		return false;
		}

	public boolean
	containsString( String string )
		{
		int		i;
		String	argStr;

		for ( i = 0 ; i < this.size() ; ++i )
			{
			argStr = (String) this.elementAt(i);

			if ( argStr.equals( string ) )
				return true;
			}

		return false;
		}

	public static CVSArgumentVector
	parseArgumentString( String argStr )
		{
		String	token;
		String	newDelim = null;
		boolean	matchQuote = false;

		CVSArgumentVector result =
			new CVSArgumentVector();

		StringTokenizer toker =
			new StringTokenizer( argStr, " '\"", true );

		for ( ; toker.hasMoreTokens() ; )
			{
			try {
				token =
					( newDelim == null
						? toker.nextToken()
						: toker.nextToken( newDelim ) );
				
				newDelim = null;
				}
			catch ( NoSuchElementException ex )
				{
				break;
				}

			if ( token.equals( " " ) )
				{
				continue;
				}
			else if ( token.equals( "'" ) )
				{
				if ( matchQuote )
					{
					newDelim = " '\"";
					matchQuote = false;
					}
				else
					{
					newDelim = "'";
					matchQuote = true;
					}
				}
			else if ( token.equals( "\"" ) )
				{
				if ( matchQuote )
					{
					newDelim = " '\"";
					matchQuote = false;
					}
				else
					{
					newDelim = "\"";
					matchQuote = true;
					}
				}
			else
				{
				result.addElement( token );
				}
			}

		return result;
		}

	}
