/*
** Tim Endres' utilities package.
** Copyright (c) 1997 by Tim Endres
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

package com.ice.util;

import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;


public class
StringUtilities
	{
	static public String[]
	vectorToStringArray( final Vector sV )
		{
		final int			sz = sV.size();
		final String[]	result = new String[ sz ];

		for ( int i = 0 ; i < sz ; ++i )
			{
			result[i] = (String) sV.elementAt(i);
			}

		return result;
		}

	/**
	 * Split a string into a string array containing the substrings
	 * between the delimiters.
	 *
	 * NOTE This method WILL <strong>NOT</strong> return an empty
	 * token at the end of the array that is returned, if the string
	 * ends with the delimiter. If you wish to have a property string
	 * array that ends with the delimiter return an empty string at
	 * the end of the array, use <code>vectorString()</code>.
	 */

	static public String[]
	splitString( final String splitStr, final String delim )
		{
		int				i, count;
		String[]		result;
		StringTokenizer toker;

		toker = new StringTokenizer( splitStr, delim );

		count = toker.countTokens();

		result = new String[ count ];

		for ( i = 0 ; i < count ; ++i )
			{
			try { result[i] = toker.nextToken(); }
			catch ( final NoSuchElementException ex )
				{
				result = null;
				break;
				}
			}

		return result;
		}

	/**
	 * Split a string into a string Vector containing the substrings
	 * between the delimiters.
	 *
	 * NOTE This method WILL return an empty
	 * token at the end of the array that is returned, if the string
	 * ends with the delimiter.
	 */

	static public Vector
	vectorString( final String splitStr, final String delim )
		{
		boolean		tokeWasDelim = false;
		int			i, count;
		StringTokenizer toker;

		final Vector			result = new Vector();

		toker = new StringTokenizer( splitStr, delim, true );
		count = toker.countTokens();

		for ( i = 0 ; i < count ; ++i )
			{
			String toke;

			try { toke = toker.nextToken(); }
			catch ( final NoSuchElementException ex )
				{ break; }

			if ( toke.equals( delim ) )
				{
				if ( tokeWasDelim )
					result.addElement( "" );
				tokeWasDelim = true;
				}
			else
				{
				result.addElement( toke );
				tokeWasDelim = false;
				}
			}

		if ( tokeWasDelim )
			result.addElement( "" );

		return result;
		}

	public static String
	join( final String[] strings, final String sep )
		{
		final StringBuffer result = new StringBuffer();

		for ( int i = 0 ; strings != null && i < strings.length ; ++i )
			{
			if ( i > 0 ) result.append( sep );
			result.append( strings[i] );
			}

		return result.toString();
		}

	public static String[]
	argumentSubstitution( final String[] args, final Hashtable vars )
		{
		final StringBuffer argBuf = new StringBuffer();

		final String[] result = new String[ args.length ];

		for ( int aIdx = 0 ; aIdx < args.length ; ++aIdx )
			{
			final String argStr = args[ aIdx ];

			final int index = argStr.indexOf( '$' );

			if ( index < 0 )
				{
				result[ aIdx ] = argStr;
				continue;
				}
			else
				{
				result[ aIdx ] =
					StringUtilities.stringSubstitution( argStr, vars );
				}
			}

		return result;
		}

	public static String
	stringSubstitution( final String argStr, final Hashtable vars )
		{
		final StringBuffer argBuf = new StringBuffer();

		for ( int cIdx = 0 ; cIdx < argStr.length() ; )
			{
			char ch = argStr.charAt( cIdx );

			switch ( ch )
				{
				case '$':
					final StringBuffer nameBuf = new StringBuffer();
					for ( ++cIdx ; cIdx < argStr.length() ; ++cIdx )
						{
						ch = argStr.charAt( cIdx );
						if ( ch == '_' || Character.isLetterOrDigit( ch ) )
							nameBuf.append( ch );
						else
							break;
						}

					if ( nameBuf.length() > 0 )
						{
						final String value = (String)
							vars.get( nameBuf.toString() );

						if ( value != null )
							{
							argBuf.append( value );
							}
						}
					break;

				default:
					argBuf.append( ch );
					++cIdx;
					break;
				}
			}

		return argBuf.toString();
		}

	public static String[]
	parseArgumentString( final String argStr )
		{
		String[] result = null;

		final Vector vector = StringUtilities.parseArgumentVector( argStr );

		if ( vector != null && vector.size() > 0 )
			{
			result = new String[ vector.size() ];
			vector.copyInto( result );
			}

		return result;
		}

	public static Vector
	parseArgumentVector( final String argStr )
		{
		final Vector			result = new Vector();
		final StringBuffer	argBuf = new StringBuffer();

		boolean backSlash = false;
		boolean matchSglQuote = false;
		boolean matchDblQuote = false;

		for ( int cIdx = 0 ; cIdx < argStr.length() ; ++cIdx )
			{
			final char ch = argStr.charAt( cIdx );

			switch ( ch )
				{
				//
				// W H I T E S P A C E
				//
				case ' ':
				case '\t':
				case '\n':
				case '\r':
					if ( backSlash )
						{
						argBuf.append( ch );
						backSlash = false;
						}
					else if ( matchSglQuote || matchDblQuote )
						{
						argBuf.append( ch );
						}
					else if ( argBuf.length() > 0 )
						{
						result.addElement( argBuf.toString() );
						argBuf.setLength( 0 );
						}
					break;

				case '\\':
					if ( backSlash )
						{
						argBuf.append( "\\" );
						}
					backSlash = ! backSlash;
					break;

				case '\'':
					if ( backSlash )
						{
						argBuf.append( "'" );
						backSlash = false;
						}
					else if ( matchSglQuote )
						{
						result.addElement( argBuf.toString() );
						argBuf.setLength( 0 );
						matchSglQuote = false;
						}
					else if ( ! matchDblQuote )
						{
						matchSglQuote = true;
						}
					break;

				case '"':
					if ( backSlash )
						{
						argBuf.append( "\"" );
						backSlash = false;
						}
					else if ( matchDblQuote )
						{
						result.addElement( argBuf.toString() );
						argBuf.setLength( 0 );
						matchDblQuote = false;
						}
					else if ( ! matchSglQuote )
						{
						matchDblQuote = true;
						}
					break;

				default:
					if ( backSlash )
						{
						switch ( ch )
							{
							case 'b': argBuf.append( '\b' ); break;
							case 'f': argBuf.append( '\f' ); break;
							case 'n': argBuf.append( '\n' ); break;
							case 'r': argBuf.append( '\r' ); break;
							case 't': argBuf.append( '\t' ); break;

							default:
								final char ch2 = argStr.charAt( cIdx+1 );
								final char ch3 = argStr.charAt( cIdx+2 );
								if ( ch >= '0' && ch <= '7'
										&& ch2 >= '0' && ch2 <= '7'
										&& ch3 >= '0' && ch3 <= '7' )
									{
									final int octal =
										(ch - '0') * 64
										+ (ch2 - '0') * 8
											+ ch3 - '0';
									argBuf.append( (char) octal );
									cIdx += 2;
									}
								else if ( ch == '0' )
									{
									argBuf.append( '\0' );
									}
								else
									{
									argBuf.append( ch );
									}
								break;
							}
						}
					else
						{
						argBuf.append( ch );
						}

					backSlash = false;
					break;
				}
			}

		if ( argBuf.length() > 0 )
			{
			result.addElement( argBuf.toString() );
			}

		return result;
		}

	}

