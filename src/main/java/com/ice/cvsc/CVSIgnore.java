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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;


public class CVSIgnore {
	public static final String		RCS_ID = "$Id: CVSIgnore.java,v 2.4 2003/07/27 01:08:32 time Exp $";
	public static final String		RCS_REV = "$Revision: 2.4 $";

	private static final String		DEFAULT_IGNORE_SPEC =
		"RCSLOG RCS SCCS CVS cvslog.*" +
		"tags TAGS *~ #* ,*" +
		"*.old *.bak *.orig *.rej .del-*" +
		"*.a *.o *.elc *.ln core" +
		"*.zip *.tar *.gz *.z *.Z";

	private Vector		specs;


	/**
	 * Constructs a new CVSIgnore object.
	 */
	public
	CVSIgnore()
		{
		super();
		this.specs = null;
		this.setIgnoreSpec( DEFAULT_IGNORE_SPEC );
		}

	/**
	 * Constructs a new CVSIgnore object, setting the
	 * ignore's list to that specified by the parameter.
	 *
	 * @param default_spec The default ignore specs.
	 */
	public
	CVSIgnore( final String default_spec )
		{
		super();
		this.specs = null;
		this.setIgnoreSpec( default_spec );
		}

	public int
	size()
		{
		return this.specs.size();
		}

	/**
	 * Adds the cvsignore specifications to the current list
	 * of ignored files.
	 *
	 * @param spec The string listing the specs to add.
	 */
	public void
	addIgnoreSpec( final String spec )
		{
		if ( spec == null )
			return;

		String	toke;
		int		i;
			final int count;

			if ( this.specs == null )
			{
			this.specs = new Vector();
			}

		final StringTokenizer toker
			= new StringTokenizer( spec );

		count = toker.countTokens();

		for ( i = 0 ; i < count ; ++i )
			{
			try { toke = toker.nextToken(); }
			catch ( final NoSuchElementException ex )
				{
				break;
				}

			if ( toke.equals( "!" ) )
				{
				this.specs = new Vector();
				}
			else
				{
				this.specs.addElement( toke );
				}
			}
		}

	/**
	 * Adds the cvsignore specifications from the file
	 * provided to the current list of ignored files.
	 *
	 * @param ignoreFile The file containing the ignore specs.
	 */
	public void
	addIgnoreFile( final File ignoreFile )
		{
		if ( ignoreFile == null )
			return;

		String	line;
		boolean	ok = true;
		BufferedReader in = null;

		if ( this.specs == null )
			{
			this.specs = new Vector();
			}

		try {
			in = new BufferedReader
				( new FileReader( ignoreFile ) );
			}
		catch ( final IOException ex )
			{
			in = null;
			ok = false;
			}

		for ( ; ok ; )
			{
			try { line = in.readLine();	}
			catch ( final IOException ex )
				{ line = null; }

			if ( line == null ) break;

			this.addIgnoreSpec( line );
			}

		if ( in != null )
			{
			try { in.close(); }
				catch ( final IOException ex ) { }
			}
		}

	/**
	 * Replaces all current ignore specs with those passed in.
	 *
	 * @param spec The string listing the specs to replace with.
	 */
	private void
	setIgnoreSpec(final String spec)
		{
		if ( this.specs != null )
			{
			this.specs.removeAllElements();
			}

		this.addIgnoreSpec( spec );
		}

	/**
	 * Determines if a file is to be ignored.
	 *
	 * @param name The name of the file to check.
	 * @return If the file is to be ignored, true, else false.
	 */
	public boolean
	isFileToBeIgnored( final String name )
		{
		int		i;

		for ( i = 0 ; i < this.specs.size() ; ++i )
			{
			final String spec = (String)this.specs.elementAt(i);
			if ( fileMatchesExpr( name, spec ) )
				{
				return true;
				}
			}

		return false;
		}

	/**
	 * Determines if a filename matches an expression.
	 *
	 * @param fileName The name of the file to check.
	 * @param matchExpr The expression to check against.
	 * @return If the file name matches the expression, true, else false.
	 */
	private boolean
	fileMatchesExpr( final String fileName, final String matchExpr )
		{
		return this.matchExprRecursor( fileName, matchExpr, 0, 0 );
		}

	/**
	 * An internal routine to implement expression matching.
	 * This routine is based on a self-recursive algorithm.
	 *
	 * @param string The string to be compared.
	 * @param pattern The expression to compare <em>string</em> to.
	 * @param sIdx The index of where we are in <em>string</em>.
	 * @param pIdx The index of where we are in <em>pattern</em>.
	 * @return True if <em>string</em> matched pattern, else false.
	 */
	private boolean
	matchExprRecursor( final String string, final String pattern, int sIdx, int pIdx )
		{
		final int		pLen = pattern.length();
		final int		sLen = string.length();

		for ( ; ; )
			{

			if ( pIdx >= pLen )
				{
					return sIdx >= sLen;
				}

			if ( sIdx >= sLen && pattern.charAt(pIdx) != '*' )
				{
				return false;
				}

			// Check for a '*' as the next pattern char.
			// This is handled by a recursive call for
			// each postfix of the name.
			if ( pattern.charAt(pIdx) == '*' )
				{
				if ( ++pIdx >= pLen )
					return true;

				for ( ; ; )
					{
					if ( this.matchExprRecursor
							( string, pattern, sIdx, pIdx ) )
						return true;

					if ( sIdx >= sLen )
						return false;

					++sIdx;
					}
				}

			// Check for '?' as the next pattern char.
			// This matches the current character.
			if ( pattern.charAt(pIdx) == '?' )
				{
				++pIdx;
				++sIdx;
				continue;
				}

			// Check for '[' as the next pattern char.
			// This is a list of acceptable characters,
			// which can include character ranges.
			if ( pattern.charAt(pIdx) == '[' )
				{
				for ( ++pIdx ; ; ++pIdx )
					{
					if ( pIdx >= pLen || pattern.charAt(pIdx) == ']' )
						return false;

					if ( pattern.charAt(pIdx) == string.charAt(sIdx) )
						break;

					if ( pIdx < pLen - 1
							&& pattern.charAt(pIdx + 1) == '-' )
						{
						if ( pIdx >= pLen - 2 )
							return false;

						final char chStr = string.charAt(sIdx);
						final char chPtn = pattern.charAt(pIdx);
						final char chPtn2 = pattern.charAt(pIdx+2);

						if ( chPtn <= chStr && chPtn2 >= chStr )
							break;

						if ( chPtn >= chStr && chPtn2 <= chStr )
							break;

						pIdx += 2;
						}
					}

				for ( ; pattern.charAt(pIdx) != ']' ; ++pIdx )
					{
					if ( pIdx >= pLen )
						{
						--pIdx;
						break;
						}
					}

				++pIdx;
				++sIdx;
				continue;
				}

			// Check for backslash escapes
			// We just skip over them to match the next char.
			if ( pattern.charAt(pIdx) == '\\' )
				{
				if ( ++pIdx >= pLen )
					return false;
				}

			if ( pIdx < pLen && sIdx < sLen )
				if ( pattern.charAt(pIdx) != string.charAt(sIdx) )
					return false;

			++pIdx;
			++sIdx;
			}
		}

	public void
	dumpIgnoreList( final String message )
		{
		if ( message != null )
			CVSLog.logMsg( message );

		for ( int i = 0 ; i < this.specs.size() ; ++i )
			{
			final String spec =
				(String) this.specs.elementAt(i);

			CVSLog.logMsg
				("Ignore[" + i + "] '" + spec + '\'');
			}
		}
	}
