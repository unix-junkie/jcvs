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

/**
 * The CVSIgnore class implements the concept of a '.cvsignore' file.
 * This is used by CVSProject's to handling CVS's concept of files that
 * should be ignored during processing.	Currently, we only need this for
 * the 'import' function, since in all other cases we have a list of
 * entries to work from. In other words, 'import' is the only function
 * we implement that scans directories, and thus cares about ignoring
 * certain files.
 *
 * @version $Revision: 2.3 $
 * @author Timothy Gerard Endres, <a href="mailto:time@ice.com">time@ice.com</a>.
 * @see CVSProject
 *
 */

import java.lang.*;
import java.io.*;
import java.util.*;


public class CVSIgnore extends Object 
	{
	static public final String		RCS_ID = "$Id: CVSIgnore.java,v 2.3 1998/07/03 15:56:40 time Exp $";
	static public final String		RCS_REV = "$Revision: 2.3 $";

	static private final String		DEFAULT_IGNORE_SPEC =
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
	CVSIgnore( String default_spec )
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
	addIgnoreSpec( String spec )
		{
		if ( spec == null )
			return;

		String	toke;
		int		i, count;
		
		if ( this.specs == null )
			{
			this.specs = new Vector();
			}

		StringTokenizer toker
			= new StringTokenizer( spec );
		
		count = toker.countTokens();
		
		for ( i = 0 ; i < count ; ++i )
			{
			try { toke = toker.nextToken(); }
			catch ( NoSuchElementException ex )
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
	addIgnoreFile( File ignoreFile )
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
		catch ( IOException ex )
			{
			in = null;
			ok = false;
			}

		for ( ; ok ; )
			{
			try { line = in.readLine();	}
			catch ( IOException ex )
				{ line = null; }

			if ( line == null ) break;
			
			this.addIgnoreSpec( line );
			}

		if ( in != null )
			{
			try { in.close(); }
				catch ( IOException ex ) { }
			}
		}

	/**
	 * Replaces all current ignore specs with those passed in.
	 * 
	 * @param spec The string listing the specs to replace with.
	 */
	public void
	setIgnoreSpec( String spec )
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
	isFileToBeIgnored( String name )
		{
		int		i;

		for ( i = 0 ; i < this.specs.size() ; ++i )
			{
			String spec = (String)this.specs.elementAt(i);
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
	fileMatchesExpr( String fileName, String matchExpr )
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
	matchExprRecursor( String string, String pattern, int sIdx, int pIdx )
		{
		int		pLen = pattern.length();
		int		sLen = string.length();

		for ( ; ; )
			{

			if ( pIdx >= pLen )
				{
				if ( sIdx >= sLen )
					return true;
				else
					return false;
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

					if ( pIdx < (pLen - 1)
							&& pattern.charAt(pIdx + 1) == '-' )
						{
						if ( pIdx >= (pLen - 2) )
							return false;

						char chStr = string.charAt(sIdx);
						char chPtn = pattern.charAt(pIdx);
						char chPtn2 = pattern.charAt(pIdx+2);

						if ( ( chPtn <= chStr ) && ( chPtn2 >= chStr ) )
							break;

						if ( ( chPtn >= chStr ) && ( chPtn2 <= chStr ) )
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
	dumpIgnoreList( String message )
		{
		if ( message != null )
			CVSLog.logMsg( message );

		for ( int i = 0 ; i < this.specs.size() ; ++i )
			{
			String spec =
				(String) this.specs.elementAt(i);

			CVSLog.logMsg
				( "Ignore[" +i+ "] '" +spec+ "'" );
			}
		}
	}
