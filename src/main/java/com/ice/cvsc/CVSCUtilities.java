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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Vector;


/**
 * Implements several general utility methods used by the cvs
 * client package.
 *
 * @version $Revision: 2.11 $
 * @author Timothy Gerard Endres, <a href="mailto:time@ice.com">time@ice.com</a>.
 */

public class
CVSCUtilities extends Object
	{
	static public final String		RCS_ID = "$Id: CVSCUtilities.java,v 2.11 2003/07/27 01:08:32 time Exp $";
	static public final String		RCS_REV = "$Revision: 2.11 $";

	static private boolean			redirectOutErr;
	static private PrintStream		out;
	static private PrintStream		err;

	static
		{
		CVSCUtilities.redirectOutErr = false;
		CVSCUtilities.out = null;
		CVSCUtilities.err = null;
		}

	static public boolean
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
	 * This class method determines if a path exists 'within'
	 * another path. In the cvsc package, we are constantly
	 * checking to be sure that a directory is contained within
	 * a given tree, or that a file is in the correct path.
	 * The difficulty is that each platform has different
	 * path separators and 'case' requirements. Worse, on
	 * systems like Windoze, the user can type lower case,
	 * while the FileDialog returns what may be upper case
	 * on the file system. This method is an attempt to
	 * normalize all of this.
	 *
	 * @param path The shorter or <em>parent</em> path.
	 * @param path The longer or <em>child</em> path.
	 * @return True if subPath is a subdirectory of path.
	 */

	static public boolean
	isSubpathInPath( String rootPath, String subPath )
		{
		boolean		result = false;

		if ( rootPath.length() > subPath.length() )
			{
			result = false;
			}
		else
			{
			subPath = CVSCUtilities.importPath( subPath );
			rootPath = CVSCUtilities.importPath( rootPath );

			if ( ! CVSCUtilities.caseSensitivePathNames() )
				{
				subPath = subPath.toLowerCase();
				rootPath = rootPath.toLowerCase();
				}

			result = subPath.startsWith( rootPath );
			}

		if ( ! result )
			{
			CVSTracer.traceIf( true,
				"CVSCUtilities.isSubpathInPath: FALSE result\n"
				+ "  adjusted rootPath '" + rootPath + "'\n"
				+ "  adjusted subPath  '" + subPath + "'" );
			}

		return result;
		}

	static public int
	computeTranslation( final CVSEntry entry )
		{
		final String options = entry.getOptions();

		int trans = CVSClient.TRANSLATE_ASCII;

		if ( options != null && options.length() > 0 )
			{
			// REVIEW
			// UNDONE You know this needs more sophisitication...
			if ( options.startsWith( "-kb" ) )
				{
				trans = CVSClient.TRANSLATE_NONE;
				}
			}

		return trans;
		}

	// NOTE
	// These are not really done. For instance, the Macintosh
	// file system treats "::" as if it were "/../" under UNIX,
	// and DOS has drive letters with colons, and so on...
	// However, for our simply purposes, these translations
	// appear to be adequate.
	//
	static public String
	exportPath( final String path )
		{
		return path.replace( '/', File.separatorChar );
		}

	static public String
	importPath( final String path )
		{
		return path.replace( File.separatorChar, '/' );
		}

	static public String
	ensureFinalSlash( final String path )
		{
		return
			path.endsWith( "/" )
			? path : path + "/";
		}

	static public String
	stripFinalSlash( final String path )
		{
		return
			path.endsWith( "/" )
			? path.substring( 0, path.length() - 1 )
			: path;
		}

	static public String
	stripFinalSeparator( String path )
		{
		for ( ; ; )
			{
			if ( path.endsWith( "/" ) )
				path = path.substring( 0, path.length() - 1 );
			else if ( path.endsWith( File.separator ) )
				path = path.substring( 0, path.length() - 1 );
			else
				break;
			}

		return path;
		}

	/**
	 * Given a localDirectory from a CVSEntry, get the
	 * parent directory of the localDirectory.
	 */
	static public String
	getLocalParent( String localDir )
		{
		localDir = CVSCUtilities.stripFinalSlash( localDir );
		final int index = localDir.lastIndexOf( '/' );
		if ( index > 0 )
			{
			localDir = localDir.substring( 0, index );
			}
		return localDir;
		}

	static public int
	slashCount( final String s )
		{
		int result = 0;
		for ( int cIdx = 0 ; cIdx < s.length() ; ++cIdx )
			{
			if ( s.charAt(cIdx) == '/' )
				result++;
			}
		return result;
		}

	static public boolean
	createEmptyFile( final File f )
		{
		boolean result = true;

		try {
			final FileWriter writer = new FileWriter( f );
			writer.close();
			}
		catch ( final IOException ex )
			{
			result = false;
			CVSTracer.traceWithStack
				( "ERROR creating empty file '" + f.getPath()
					+ "' - " + ex.getMessage() );
			}

		return result;
		}

	static public void
	writeStringFile( final File f, final String str )
		throws IOException
		{
		FileWriter writer = null;

		try {
			writer = new FileWriter( f );

			if ( str != null )
				{
				writer.write( str );
				}
			}
		finally
			{
			if ( writer != null )
				writer.close();
			}
		}

	static public String
	readStringFile( final File f )
		throws IOException
		{
		BufferedReader	in = null;
		String			result = "";

		try {
			in = new BufferedReader( new FileReader( f ) );
			result = in.readLine();
			if ( result == null )
				result = "";
			}
		finally
			{
			if ( in != null )
				try { in.close(); }
					catch ( final IOException ex )
						{ }
			}

		return result;
		}

	static public void
	endRedirectOutput()
		{
		if ( CVSCUtilities.redirectOutErr )
			{
			CVSCUtilities.redirectOutErr = false;
			System.out.flush();
			System.out.close();
			System.setOut( CVSCUtilities.out );
			System.setErr( CVSCUtilities.err );
			CVSCUtilities.out = null;
			CVSCUtilities.err = null;
			}
		}

	public static void
	integrateEntriesLog( final File adminDir )
		throws IOException
		{
		PrintWriter outBak = null;
		BufferedReader logIn = null;
		BufferedReader entIn = null;

	//	System.err.println( "INTEGRATE LOGFILE: " + adminDir.getPath() );

		final File logF = new File( adminDir, "Entries.Log" );

		if ( ! logF.exists() )
			return;

	//	System.err.println
	//		( "Integrating '" + logF.getPath() + "' into 'Entries'" );

		final Vector nameV = new Vector();
		final Vector lineV = new Vector();

		final File entF = new File( adminDir, "Entries" );
		final File bakF = new File( adminDir, "Entries.Backup" );

		try {
			entIn = new BufferedReader( new FileReader( entF ) );

			for ( ; ; )
				{
				final String inLine = entIn.readLine();
				if ( inLine == null )
					break;

				final char ch = inLine.charAt(0);
				if ( ch != '/' && ch != 'D' )
					continue;

				final int begIdx = ch == 'D' ? 2 : 1;
				final int idx = inLine.indexOf( "/", begIdx );
				if ( idx == -1 )
					continue;

				lineV.addElement( inLine );
				nameV.addElement( inLine.substring( begIdx, idx ) );
				}

			entIn.close();
			entIn = null;

		//	System.err.println( "WE'VE READ " + lineV.size() + " entries" );

			logIn = new BufferedReader( new FileReader( logF ) );

			for ( ; ; )
				{
				final String inLine = logIn.readLine();
				if ( inLine == null )
					break;

				if ( inLine.length() < 5 )
					break;

			//	System.err.println( "Processing LOG LINE: " + inLine );

				final char selCh = inLine.charAt(0);
				final char sepCh = inLine.charAt(1);
				if ( selCh != 'A' && selCh != 'R'
						|| sepCh != ' ' )
					{
				//	System.err.println( "IGNORE bad selector: " + inLine );
					continue;
					}

				final char ch = inLine.charAt(2);

				final int begIdx = ch == 'D' ? 4 : 3;
				int idx = inLine.indexOf( "/", begIdx );
				if ( idx == -1 )
					continue;

				final String name = inLine.substring( begIdx, idx );
			//	System.err.println( "Processing LOG NAME: " + name );

				if ( selCh == 'A' )
					{
					if ( nameV.contains( name ) )
						{
					//	System.err.println( "OVERWRITING: " + name );
						idx = nameV.indexOf( name );
						lineV.setElementAt( inLine.substring(2), idx );
						}
					else
						{
					//	System.err.println( "APPENDING: " + name );
						nameV.addElement( name );
						lineV.addElement( inLine.substring(2) );
						}
					}
				else if ( selCh == 'R' )
					{
					if ( nameV.contains( name ) )
						{
					//	System.err.println( "REMOVING: " + name );
						idx = nameV.indexOf( name );
						nameV.removeElementAt( idx );
						lineV.removeElementAt( idx );
						}
					else
						{
					//	System.err.println( "IGNORING: " + name );
						}
					}
				}

			logIn.close();
			logIn = null;

		//	System.err.println( "WRITING BACKUP: " + bakF.getPath() );
		//	System.err.println( "   nameV.size=" + nameV.size()
		//						+ ", lineV.size=" + lineV.size() );

			outBak = new PrintWriter( new FileWriter( bakF ) );

			for ( int i = 0, sz = nameV.size() ; i < sz ; ++i )
				{
				outBak.println( lineV.elementAt(i) );
				}

			outBak.close();
			outBak = null;

		//	System.err.println( "RENAMING BACKUP: to " + entF.getPath() );

			entF.delete();
			if ( bakF.renameTo( entF ) )
				{
			//	System.err.println( "DELETING LOGFILE: " + logF.getPath() );
				logF.delete();
				}
			else
				{
				throw new IOException
					( "RENAME FAILED from '" + bakF.getPath()
						+ "' to '" + entF.getPath() + "'" );
				}

		//	System.err.println( "DONE" );
			}
		finally
			{
			try {
				if ( entIn != null ) entIn.close();
				if ( logIn != null ) logIn.close();
				if ( outBak != null ) outBak.close();
				}
			catch ( final IOException ex )
				{ }
			}
		}

	}


