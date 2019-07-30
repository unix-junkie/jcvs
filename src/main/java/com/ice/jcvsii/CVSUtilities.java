/*
** Java cvs client application package.
** Copyright (c) 1997-2002 by Timothy Gerard Endres
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import com.ice.cvsc.CVSClient;
import com.ice.cvsc.CVSLog;
import com.ice.cvsc.CVSProject;
import com.ice.cvsc.CVSRequest;
import com.ice.pref.PrefsTuple;
import com.ice.pref.PrefsTupleTable;
import com.ice.pref.UserPrefs;


final class
CVSUtilities {

	private CVSUtilities() {
	}

	public static CVSClient
	createCVSClient()
		{
		final CVSClient client = new CVSClient();

		client.setMultipleInterfaceSupport
			( Config.getInstance().getPrefs().getBoolean
				( ConfigConstants.GLOBAL_MULTI_INTF, false ) );

		return client;
		}

	public static CVSClient
	createCVSClient( final String cvsHost, final int cvsPort )
		{
		final CVSClient client = createCVSClient();

		client.setHostName( cvsHost );
		client.setPort( cvsPort );

		return client;
		}

	public static String
	establishServerCommand( final String hostname, final int connMethod, final boolean pServer )
		{
		PrefsTuple tup;

		final UserPrefs prefs = Config.getPreferences();

		String command = "(not applicable)";

		if ( connMethod == CVSRequest.METHOD_RSH )
			{
			command = "cvs server";

			final PrefsTupleTable table =
				prefs.getTupleTable
					( ConfigConstants.GLOBAL_SVRCMD_TABLE, null );

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
		else if ( connMethod == CVSRequest.METHOD_SSH )
			{
			command = "cvs server";

			final PrefsTupleTable table =
				prefs.getTupleTable
					( ConfigConstants.GLOBAL_SVRCMD_TABLE, null );

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

	public static void
	establishRSHProcess( final CVSRequest request )
		{
		final UserPrefs prefs = Config.getPreferences();

		final String rshCommand =
			prefs.getProperty( ConfigConstants.GLOBAL_RSH_COMMAND, null );

		if ( rshCommand != null && !rshCommand.isEmpty())
			{
			request.setRshProcess( rshCommand );
			}
		}

	public static void
	establishRSHProcess( final CVSProject project )
		{
		final UserPrefs prefs = Config.getPreferences();

		final String rshCommand =
			prefs.getProperty( ConfigConstants.GLOBAL_RSH_COMMAND, null );

		if ( rshCommand != null && !rshCommand.isEmpty())
			{
			project.setRshProcess( rshCommand );
			}
		}

	public static int
	computePortNum( final String hostname, final int connMethod, final boolean isPServer )
		{
		int defPort;
		final int cvsPort;

		final UserPrefs prefs = Config.getPreferences();

		final StringBuilder prefName = new StringBuilder("portNum." );

		if ( connMethod == CVSRequest.METHOD_RSH )
			{
			prefName.append( "server." );
			defPort = prefs.getInteger( ConfigConstants.GLOBAL_RSH_PORT, 0 );
			if ( defPort == 0 )
				defPort = CVSClient.DEFAULT_RSH_PORT;
			}
		else if ( connMethod == CVSRequest.METHOD_SSH )
			{
			prefName.append( "ext." );
			defPort = prefs.getInteger( ConfigConstants.GLOBAL_SSH_PORT, 0 );
			if ( defPort == 0 )
				defPort = CVSClient.DEFAULT_SSH_PORT;
			}
		else if ( isPServer )
			{
			prefName.append( "pserver." );
			defPort = prefs.getInteger( ConfigConstants.GLOBAL_PSERVER_PORT, 0 );
			if ( defPort == 0 )
				defPort = CVSClient.DEFAULT_CVS_PORT;
			}
		else
			{
			prefName.append( "direct." );
			defPort = prefs.getInteger( ConfigConstants.GLOBAL_DIRECT_PORT, 0 );
			if ( defPort == 0 )
				defPort = CVSClient.DEFAULT_DIR_PORT;
			}

		cvsPort = prefs.getInteger( prefName + hostname, defPort );

		return cvsPort;
		}

	public static String[]
	getUserSetVariables( final String hostname )
		{
		int		i;
		String	prop;
		int		count = 0;
		final String	prefix = "setVars.";
		final UserPrefs prefs = Config.getPreferences();

		// First, get a count...
		for ( i = 0 ; ; ++i, ++count )
			{
			if ( prefs.getProperty
					( prefix + "all." + i, null ) == null )
				break;
			}

		for ( i = 0 ; ; ++i, ++count )
			{
			if (prefs.getProperty
					(prefix + hostname + '.' + i, null ) == null )
				break;
			}

		if ( count == 0 )
			return null;

		final String[] result = new String[ count ];

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
					(prefix + hostname + '.' + i, null );
			if ( prop == null )
				break;
			result[idx++] = prop;
			}

		return result;
		}

	private static String
	getFilePath(final File file)
		{
		int		index;
		final String	newName;
		String	parent = null;

		final String	pathName = file.getPath();

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

	private static String
	getFileName(final File file)
		{
		return getFileName( file.getPath() );
		}

	public static String
	getFileName( final String path )
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

	public static boolean
	renameFile( final File entryFile, final String pattern, final boolean overWrite )
		{
		final int		i;
		final boolean	result;
		final String	newName;
		final String	fileName;
		final String	rootPath;

		rootPath = getFilePath( entryFile );
		fileName = getFileName( entryFile );

		final int index = pattern.indexOf( '@' );
			// If there is no '@', pattern is a suffix.
			// Otherwise, replace the '@' with the filename.
			newName = index < 0 ? fileName + pattern : pattern.substring(0, index)
								   + fileName
								   + pattern.substring(index + 1);

		final File backFile = // UNDONE separator
			new File(rootPath + '/' + newName );

		if ( overWrite && backFile.exists() )
			backFile.delete();

		result = entryFile.renameTo( backFile );

		return result;
		}

	public static boolean
	copyFile( final File entryFile, final String pattern )
		{
		final int		i;
		int		bytes;
		long	length;
		final long	fileSize;
		boolean	result = true;
		final String	newName;
		final String	fileName;
		final String	rootPath;
 		BufferedInputStream		in = null;
		BufferedOutputStream	out = null;

		rootPath = getFilePath( entryFile );
		fileName = getFileName( entryFile );

		final int index = pattern.indexOf( '@' );
			// If there is no '@', pattern is a suffix.
			// Otherwise, replace the '@' with the filename.
			newName = index < 0 ? fileName + pattern : pattern.substring(0, index)
								   + fileName
								   + pattern.substring(index + 1);

		final File copyFile = // UNDONE separator
			new File(rootPath + '/' + newName );

		try {
			in = new BufferedInputStream(
					new FileInputStream( entryFile ) );
			}
		catch ( final Exception ex )
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
		catch ( final Exception ex )
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
				("CVSUtilities.copyFile: failed creating '"
				 + (out == null ? "output writer" : "input reader") + '\'');
			}

		if ( result )
			{
			final byte[]	buffer;
			buffer = new byte[8192];

			fileSize = entryFile.length();
			for ( length = fileSize ; length > 0 ; )
				{
				bytes = (int)(length > 8192 ? 8192 : length);

				try {
					bytes = in.read( buffer, 0, bytes );
					}
				catch ( final IOException ex )
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
				catch ( final IOException ex )
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
		catch ( final IOException ex )
			{
			CVSLog.logMsg
				( "CVSUtilities.copyFile: failed closing files: "
					+ ex.getMessage() );
			result = false;
			}

		return result;
		}

	}


