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

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

public final class
CVSLog {
	public static final String		DEFAULT_FILENAME = "cvslog.txt";

    private static boolean			debug;
    private static boolean			debugOpen;

	private static String			filename = DEFAULT_FILENAME;
	private static PrintWriter		stream;
    private static boolean			checked;
    private static boolean			open;
    private static boolean			echo;
    private static final boolean			autoFlush = true;

	private CVSLog() {
	}


	public static void
	setLogFilename( final String filename )
		{
		CVSLog.filename = filename;
		checked = false;
		}

    public static void
    checkLogOpen()
        {
		if ( checked )
			return;

		checked = true;

		if ( ! open && filename != null )
			{
			openLogFile();
			}
        }

	private static void
	openLogFile()
		{
		boolean isok = true;

		if ( debugOpen )
			new Throwable( "OPEN CVS LOG").printStackTrace();

		if ( debug)
			System.err.println
				( "CVSLog.openLogFile( " + filename + " )" );

		if ( filename == null )
			return;

			FileWriter file;
			try {
				file = new FileWriter(filename );
			}
		catch ( final Exception ex )
			{
			logMsg
				( "error opening log file '" + filename
					+ "', trying 'user.dir' - " + ex.getMessage() );

			final String userDirStr = System.getProperty( "user.dir", "" );

			filename =
				userDirStr + File.separator + DEFAULT_FILENAME;

			try {
				file = new FileWriter(filename );
				}
			catch ( final Exception ex2 )
				{
				logMsg
					( "error opening log file '" + filename
						+ "' - " + ex2.getMessage() );

					file = null;
				isok = false;
				}
			}

		if ( isok )
			{
			stream = new PrintWriter(file);
			open = true;
			}

		echo = false;
		}

	public static void
	traceMsg( final Throwable thrown, final String msg )
		{
		logMsg( msg );
		logMsg( thrown.getMessage() );

		if ( ! open )
			thrown.printStackTrace( System.err );
		else
			thrown.printStackTrace( stream );

		if ( autoFlush && open )
			stream.flush();
		}


	public static void
	logMsg( final String msg )
		{
		checkLogOpen();

		if ( open )
			{
			stream.println( msg );
			if ( autoFlush && open )
				stream.flush();
			}

	    if ( echo )
	        {
	        System.out.println( msg );
	        }
		}

	public static void
	logMsgStderr( final String msg )
		{
		checkLogOpen();

		if ( open )
			{
			stream.println( msg );
			if ( autoFlush && open )
				stream.flush();
			}

	    System.err.println( msg );
		}
	}



