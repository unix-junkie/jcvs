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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;


final class
FileUtilities
	{
	public static final String		RCS_ID = "$Id: FileUtilities.java,v 1.4 1999/03/09 19:44:39 time Exp $";
	public static final String		RCS_REV = "$Revision: 1.4 $";
	public static final String		RCS_NAME = "$Name:  $";

		private FileUtilities() {
		}

		public static void
	copyFile( final File from, final File to )
		throws IOException
		{
		int		bytes;
		long	length;
		final long	fileSize;

		BufferedInputStream		in = null;
		BufferedOutputStream	out = null;

		try {
			in = new BufferedInputStream(
					new FileInputStream( from ) );
			}
		catch ( final IOException ex )
			{
			throw new IOException
				( "FileUtilities.copyFile: opening input stream '"
					+ from.getPath() + "', " + ex.getMessage() );
			}

		try {
			out = new BufferedOutputStream(
					new FileOutputStream( to ) );
			}
		catch ( final Exception ex )
			{
			try { in.close(); }
				catch ( final IOException ex1 ) { }
			throw new IOException
				( "FileUtilities.copyFile: opening output stream '"
					+ to.getPath() + "', " + ex.getMessage() );
			}

		final byte[]	buffer;
		buffer = new byte[8192];
		fileSize = from.length();

		for ( length = fileSize ; length > 0 ; )
			{
			bytes = (int)(length > 8192 ? 8192 : length);

			try {
				bytes = in.read( buffer, 0, bytes );
				}
			catch ( final IOException ex )
				{
				try { in.close(); out.close(); }
					catch ( final IOException ex1 ) { }
				throw new IOException
					( "FileUtilities.copyFile: reading input stream, "
						+ ex.getMessage() );
				}

			if ( bytes < 0 )
				break;

			length -= bytes;

			try { out.write( buffer, 0, bytes ); }
			catch ( final IOException ex )
				{
				try { in.close(); out.close(); }
					catch ( final IOException ex1 ) { }
				throw new IOException
					( "FileUtilities.copyFile: writing output stream, "
						+ ex.getMessage() );
				}
			}

		try { in.close(); out.close(); }
		catch ( final IOException ex )
			{
			throw new IOException
				( "FileUtilities.copyFile: closing file streams, "
					+ ex.getMessage() );
			}
		}

	public static boolean
	fileEqualsExtension( final String fileName, final String extension )
		{
		boolean result = false;

		final int fnLen = fileName.length();
		final int exLen = extension.length();

		if ( fnLen > exLen )
			{
			final String fileSuffix =
				fileName.substring( fnLen - exLen );

				result = caseSensitivePathNames() ? fileSuffix.equals(extension) : fileSuffix.equalsIgnoreCase(extension);
			}

		return result;
		}

	private static boolean
	caseSensitivePathNames()
		{
		boolean result = true;

		final String osname = System.getProperty( "os.name" );

		if ( osname != null )
			{
			if ( osname.startsWith( "macos" ) )
				result = false;
			else if ( osname.startsWith( "Windows" ) )
				result = false;
			}

		return result;
		}

	/**
	 * Determines if a filename matches a 'globbing' pattern.
	 * The pattern can contain the following special symbols:
	 * <ul>
	 * <li> * - Matches zero or more of any character
	 * <li> ? - Matches exactly one of any character
	 * <li> [...] - Matches one of any character in the list or range
	 * </ul>
	 *
	 * @param fileName The name of the file to check.
	 * @param matchExpr The expression to check against.
	 * @return If the file name matches the expression, true, else false.
	 */
	public static boolean
	isPatternString( final String pattern )
		{
		if (pattern.indexOf('*') >= 0 ) return true;
		if (pattern.indexOf('?') >= 0 ) return true;

		final int index = pattern.indexOf('[');
			return index >= 0 && pattern.indexOf(']') > index + 1;

		}

	public static boolean
	matchPattern( final String fileName, final String pattern )
		{
		return
			recurseMatchPattern
				( fileName, pattern, 0, 0 );
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
	private static boolean
	recurseMatchPattern( final String string, final String pattern, int sIdx, int pIdx )
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
					if ( recurseMatchPattern
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

	public static String
	getUserHomeDirectory()
		{
		String userDirName =
			System.getProperty( "user.home", null );

		if ( userDirName == null )
			{
			userDirName = System.getProperty( "user.dir", null );
			}

		return userDirName;
		}

	}

