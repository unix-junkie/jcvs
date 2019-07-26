/*
** Java cvs client application package.
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

package com.ice.jcvsii;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.net.URL;
import java.net.URLConnection;

import com.ice.cvsc.*;
import com.ice.pref.UserPrefs;
import com.ice.pref.PrefsTuple;
import com.ice.pref.PrefsTupleTable;


public class
CVSUtilities extends Object
	{
	static public String
	establishServerCommand( String hostname, int connMethod, boolean pServer )
		{
		PrefsTuple tup;

		UserPrefs prefs = Config.getPreferences();

		String command = "(not applicable)";

		if ( connMethod == CVSRequest.METHOD_RSH )
			{
			command = "cvs server";

			PrefsTupleTable table =
				prefs.getTupleTable
					( Config.GLOBAL_SVRCMD_TABLE, null );

			if ( table != null )
				{
				tup = table.getTuple( hostname );
				if ( tup != null )
					command = tup.getValueAt(0);

				if ( command == null )
					{
					tup = table.getTuple( "DEFAULT" );
					if ( tup != null )
						command = tup.getValueAt(0);
					}
				}
			}

		return command;
		}

	static public void
	establishRSHProcess( CVSRequest request )
		{
		UserPrefs prefs = Config.getPreferences();

		String rshCommand =
			prefs.getProperty( Config.GLOBAL_RSH_COMMAND, null );
		
		if ( rshCommand != null && rshCommand.length() > 0 )
			{
			request.setRshProcess( rshCommand );
			} 
		}

	static public void
	establishRSHProcess( CVSProject project )
		{
		UserPrefs prefs = Config.getPreferences();

		String rshCommand =
			prefs.getProperty( Config.GLOBAL_RSH_COMMAND, null );
		
		if ( rshCommand != null && rshCommand.length() > 0 )
			{
			project.setRshProcess( rshCommand );
			} 
		}

	static public int
	computePortNum( String hostname, int connMethod, boolean isPServer )
		{
		int defPort;
		int cvsPort;

		UserPrefs prefs = Config.getPreferences();

		StringBuffer prefName = new StringBuffer( "portNum." );

		// NOTE
		//  isPServer: 'portNum.pserver.cvs.ice.com=2401'
		//  !isPServer: 'portNum.server.cvs.ice.com=2402'

		if ( connMethod == CVSRequest.METHOD_RSH )
			{
			defPort = 514;
			prefName.append( "server." );
			}
		else if ( isPServer )
			{
			defPort = 2401;
			prefName.append( "pserver." );
			}
		else
			{
			defPort = 2402;
			prefName.append( "direct." ); 
			}
		 
		cvsPort =
			prefs.getInteger
				( (prefName + hostname), 0 );

		if ( cvsPort == 0 )
			{
			// -------- SET DEFAULT PORT VALUE ------------
			//  RSH: 'jCVS.portNum.server.default=514'
			//  !RSH:
			//    isPServer: 'portNum.pserver.default=2401'
			//    !isPServer: 'portNum.direct.default=2402'
				
			cvsPort =
				prefs.getInteger
					( prefName.append( "default" ).toString(), defPort );
			}

		return cvsPort;
		}

	static public String[]
	getUserSetVariables( String hostname )
		{
		int		i;
		String	prop;
		int		count = 0;
		String	prefix = "setVars.";
		UserPrefs prefs = Config.getPreferences();

		// First, get a count...
		for ( i = 0 ; ; ++i, ++count )
			{
			if ( prefs.getProperty
					( prefix + "all." + i, null ) == null )
				break;
			}

		for ( i = 0 ; ; ++i, ++count )
			{
			if ( prefs.getProperty
					( prefix + hostname + "." + i, null ) == null )
				break;
			}

		if ( count == 0 )
			return null;

		String[] result = new String[ count ];

		// Now fill it in...
		int idx = 0;
		for ( i = 0 ; idx < count ; ++i )
			{
			prop = prefs.getProperty( prefix + "all." + i, null );
			if ( prop == null )
				break;
			result[idx++] = prop;
			}

		for ( i = 0 ; idx < count ; ++i )
			{
			prop =
				prefs.getProperty
					( prefix + hostname + "." + i, null );
			if ( prop == null )
				break;
			result[idx++] = prop;
			}

		return result;
		}

	static public String
	getFilePath( File file )
		{
		int		index;
		String	newName;
		String	parent = null;

		String	pathName = file.getPath();

		index = pathName.lastIndexOf( File.separatorChar );
		if ( index < 0 )
			{
			index = pathName.lastIndexOf( '/' );
			if ( index >= 0 )
				{
				parent = pathName.substring( 0, index );
				}
			}
		else
			{
			parent = pathName.substring( 0, index );
			}

		return parent;
		}

	static public String
	getFileName( File file )
		{
		return CVSUtilities.getFileName( file.getPath() );
		}

	static public String
	getFileName( String path )
		{
		int		index;
		String	newName = path;

		index = newName.lastIndexOf( File.separatorChar );
		if ( index < 0 )
			{
			index = newName.lastIndexOf( '/' );
			if ( index >= 0 )
				{
				newName = newName.substring( index + 1 );
				}
			}
		else
			{
			newName = newName.substring( index + 1 );
			}

		return newName;
		}

	static public boolean
	renameFile( File entryFile, String pattern, boolean overWrite )
		{
		int		i;
		boolean	result;
		String	newName;
		String	fileName;
		String	rootPath;

		rootPath = CVSUtilities.getFilePath( entryFile );
		fileName = CVSUtilities.getFileName( entryFile );

		int index = pattern.indexOf( '@' );
		if ( index < 0 )
			{
			// If there is no '@', pattern is a suffix.
			newName = fileName + pattern;
			}
		else
			{
			// Otherwise, replace the '@' with the filename.
			newName =
				pattern.substring( 0, index )
				+ fileName
				+ pattern.substring( index + 1 );
			}

		File backFile = // UNDONE separator
			new File( rootPath + "/" + newName );

		if ( overWrite && backFile.exists() )
			backFile.delete();

		result = entryFile.renameTo( backFile );
		
		return result;
		}

	static public boolean
	copyFile( File entryFile, String pattern )
		{
		int		i;
		int		bytes;
		long	length;
		long	fileSize;
		boolean	result = true;
		String	newName;
		String	fileName;
		String	rootPath;
 		BufferedInputStream		in = null;
		BufferedOutputStream	out = null;

		rootPath = CVSUtilities.getFilePath( entryFile );
		fileName = CVSUtilities.getFileName( entryFile );

		int index = pattern.indexOf( '@' );
		if ( index < 0 )
			{
			// If there is no '@', pattern is a suffix.
			newName = fileName + pattern;
			}
		else
			{
			// Otherwise, replace the '@' with the filename.
			newName =
				pattern.substring( 0, index )
				+ fileName
				+ pattern.substring( index + 1 );
			}

		File copyFile = // UNDONE separator
			new File( rootPath + "/" + newName );

		try {
			in = new BufferedInputStream(
					new FileInputStream( entryFile ) );
			}
		catch ( Exception ex )
			{
			in = null;
			result = false;
			CVSLog.logMsg
				( "CVSUtilities.copyFile: failed creating in reader: "
					+ ex.getMessage() );
			}

		if ( result )
		try {
			out = new BufferedOutputStream(
					new FileOutputStream( copyFile ) );
			}
		catch ( Exception ex )
			{
			out = null;
			result = false;
			CVSLog.logMsg
				( "CVSUtilities.copyFile: failed creating out writer: "
					+ ex.getMessage() );
			}

		if ( out == null || in == null )
			{
			result = false;
			CVSLog.logMsg
				( "CVSUtilities.copyFile: failed creating '"
					+ (out == null ? "output writer" : "input reader") + "'" );
			}

		if ( result )
			{
			byte[]	buffer;
			buffer = new byte[8192];

			fileSize = entryFile.length();
			for ( length = fileSize ; length > 0 ; )
				{
				bytes = (int)(length > 8192 ? 8192 : length);

				try {
					bytes = in.read( buffer, 0, bytes );
					}
				catch ( IOException ex )
					{
					result = false;
					CVSLog.logMsg
						( "CVSUtilities.copyFile: "
							+ "ERROR reading in file:\n   "
							+ ex.getMessage() );
					break;
					}

				if ( bytes < 0 )
					break;

				length -= bytes;

				try { out.write( buffer, 0, bytes ); }
				catch ( IOException ex )
					{
					result = false;
					CVSLog.logMsg
						( "CVSUtilities.copyFile: "
							+ "ERROR writing out file:\n   "
							+ ex.getMessage() );
					break;
					}
				}
			}

		try {
			if ( in != null ) in.close();
			if ( out != null ) out.close();
			}
		catch ( IOException ex )
			{
			CVSLog.logMsg
				( "CVSUtilities.copyFile: failed closing files: "
					+ ex.getMessage() );
			result = false;
			}
		
		return result;
		}

	}


