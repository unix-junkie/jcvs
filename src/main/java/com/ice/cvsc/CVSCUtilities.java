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

import java.awt.*;
import java.io.*;
import java.util.*;


/**
 * Implements several general utility methods used by the cvs
 * client package.
 *
 * @version $Revision: 2.10 $
 * @author Timothy Gerard Endres, <a href="mailto:time@ice.com">time@ice.com</a>.
 */

public class
CVSCUtilities extends Object
	{
	static public final String		RCS_ID = "$Id: CVSCUtilities.java,v 2.10 2000/06/11 00:01:55 time Exp $";
	static public final String		RCS_REV = "$Revision: 2.10 $";

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

		String osname = System.getProperty( "os.name" );
		
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
	computeTranslation( CVSEntry entry )
		{
		String options = entry.getOptions();

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
	exportPath( String path )
		{
		return path.replace( '/', File.separatorChar );
		}

	static public String
	importPath( String path )
		{
		return path.replace( File.separatorChar, '/' );
		}

	static public String
	ensureFinalSlash( String path )
		{
		return
			( path.endsWith( "/" )
				? path : path + "/" );
		}

	static public String
	stripFinalSlash( String path )
		{
		return
			( path.endsWith( "/" )
				? path.substring( 0, (path.length() - 1) )
				: path );
		}

	static public String
	stripFinalSeparator( String path )
		{
		for ( ; ; )
			{
			if ( path.endsWith( "/" ) )
				path = path.substring( 0, (path.length() - 1) );
			else if ( path.endsWith( File.separator ) )
				path = path.substring( 0, (path.length() - 1) );
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
		int index = localDir.lastIndexOf( '/' );
		if ( index > 0 )
			{
			localDir = localDir.substring( 0, index );
			}
		return localDir;
		}

	static public int
	slashCount( String s )
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
	createEmptyFile( File f )
		{
		boolean result = true;

		try {
			FileWriter writer = new FileWriter( f );
			writer.close();
			}
		catch ( IOException ex )
			{
			result = false;
			CVSTracer.traceWithStack
				( "ERROR creating empty file '" + f.getPath()
					+ "' - " + ex.getMessage() );
			}

		return result;
		}

	static public void
	writeStringFile( File f, String str )
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
	readStringFile( File f )
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
					catch ( IOException ex )
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
	integrateEntriesLog( File adminDir )
		throws IOException
		{
		PrintWriter outBak = null;
		BufferedReader logIn = null;
		BufferedReader entIn = null;

	//	System.err.println( "INTEGRATE LOGFILE: " + adminDir.getPath() );

		File logF = new File( adminDir, "Entries.Log" );

		if ( ! logF.exists() )
			return;

	//	System.err.println
	//		( "Integrating '" + logF.getPath() + "' into 'Entries'" );

		Vector nameV = new Vector();
		Vector lineV = new Vector();

		File entF = new File( adminDir, "Entries" );
		File bakF = new File( adminDir, "Entries.Backup" );

		try {
			entIn = new BufferedReader( new FileReader( entF ) );

			for ( ; ; )
				{
				String inLine = entIn.readLine();
				if ( inLine == null )
					break;

				char ch = inLine.charAt(0);
				if ( ch != '/' && ch != 'D' )
					continue;

				int begIdx = ( ch == 'D' ? 2 : 1 );
				int idx = inLine.indexOf( "/", begIdx );
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
				String inLine = logIn.readLine();
				if ( inLine == null )
					break;

				if ( inLine.length() < 5 )
					break;

			//	System.err.println( "Processing LOG LINE: " + inLine );

				char selCh = inLine.charAt(0);
				char sepCh = inLine.charAt(1);
				if ( (selCh != 'A' && selCh != 'R')
						|| sepCh != ' ' )
					{
				//	System.err.println( "IGNORE bad selector: " + inLine );
					continue;
					}

				char ch = inLine.charAt(2);

				int begIdx = (ch == 'D' ? 4 : 3);
				int idx = inLine.indexOf( "/", begIdx );
				if ( idx == -1 )
					continue;

				String name = inLine.substring( begIdx, idx );
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
			catch ( IOException ex )
				{ }
			}
		}

	}


