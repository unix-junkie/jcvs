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

import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Implements a Vector subclass that handles CVS Arguments used
 * in CVSRequest objects.
 *
 * @version $Revision: 2.3 $
 * @author Timothy Gerard Endres, <a href="mailto:time@ice.com">time@ice.com</a>.
 * @see CVSClient
 * @see CVSProject
 */

public class CVSArgumentVector extends Vector
	{
	public static final String		RCS_ID = "$Id: CVSArgumentVector.java,v 2.3 2003/07/27 01:08:32 time Exp $";
	public static final String		RCS_REV = "$Revision: 2.3 $";

	public CVSArgumentVector()
		{
		super();
		}

	public CVSArgumentVector( final int initCap )
		{
		super( initCap );
		}

	public CVSArgumentVector( final int initCap, final int capIncr )
		{
		super( initCap, capIncr );
		}

	public String
	argumentAt( final int index )
		{
		return (String) this.elementAt( index );
		}

	public void
	appendArgument( final String argument )
		{
		this.addElement( argument );
		}

	public void
	appendArguments( final Vector args )
		{
		for ( int i = 0, sz = args.size() ; i < sz ; ++i )
			this.addElement( args.elementAt(i) );
		}

	public boolean
	containsArgument( final String argument )
		{
		int		i;
		String	argStr;

		for ( i = 0 ; i < this.size() ; ++i )
			{
			argStr = (String) this.elementAt(i);

			if ( argStr.equals( argument ) )
				return true;

			if (!argStr.isEmpty() && argStr.charAt(0) == '-')
				{
				++i; // skip this argument's parameter
				}
			}

		return false;
		}

	public boolean
	containsString( final String string )
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
	parseArgumentString( final String argStr )
		{
		String	token;
		String	newDelim = null;
		boolean	matchQuote = false;

		final CVSArgumentVector result =
			new CVSArgumentVector();

		final StringTokenizer toker =
			new StringTokenizer( argStr, " '\"", true );

		boolean startArg = true;
		final StringBuilder argBuf = new StringBuilder(argStr.length() );

		for ( ; toker.hasMoreTokens() ; )
			{
			try {
				token =
					newDelim == null
						? toker.nextToken()
						: toker.nextToken( newDelim );

				newDelim = null;
				}
			catch ( final NoSuchElementException ex )
				{
				break;
				}

				switch (token) {
				case " ":
					if (!startArg) {
						result.addElement(argBuf.toString());
						argBuf.setLength(0);
					}
					startArg = true;
					break;
				case "'":
					startArg = false;
					if (matchQuote) {
						newDelim = " '\"";
						matchQuote = false;
					} else {
						newDelim = "'";
						matchQuote = true;
					}
					break;
				case "\"":
					startArg = false;
					if (matchQuote) {
						newDelim = " '\"";
						matchQuote = false;
					} else {
						newDelim = "\"";
						matchQuote = true;
					}
					break;
				default:
					startArg = false;
					argBuf.append(token);
					break;
				}
			}

		if ( ! startArg )
			{
			result.addElement( argBuf.toString() );
			}

		return result;
		}

	}
