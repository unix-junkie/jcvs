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

import java.io.*;
import java.lang.*;
import java.util.*;


public class
CVSLog extends Object
	{
	private static final String		RCS_ID = "$Id: CVSLog.java,v 2.6 1999/04/01 17:49:07 time Exp $";
	private static final String		RCS_REV = "$Revision: 2.6 $";
	private static final String		RCS_NAME = "$Name:  $";

	public static final String		DEFAULT_FILENAME = "cvslog.txt";

    public static boolean			debug;
    public static boolean			debugOpen;

	private static String			filename;
	private static FileWriter		file;
	private static PrintWriter		stream;
    private static boolean			checked;
    private static boolean			open;
    private static boolean			echo;
    private static boolean			autoFlush;


	static
		{
		CVSLog.open = false;
		CVSLog.checked = false;
		CVSLog.autoFlush = true;
		CVSLog.debug = false;
		CVSLog.debugOpen = false;

		CVSLog.filename =
			new String( CVSLog.DEFAULT_FILENAME );
		}

	static public void
	setLogFilename( String filename )
		{
		CVSLog.filename = filename;
		CVSLog.checked = false;
		}

    static public void
    setAutoFlush( boolean autoflush )
        {
        CVSLog.autoFlush = autoFlush;
        }

    static public void
    checkLogOpen()
        {
		if ( CVSLog.checked )
			return;

		CVSLog.checked = true;

		if ( ! CVSLog.open && CVSLog.filename != null )
			{
			CVSLog.openLogFile();
			}
        }

	static public void
	openLogFile()
		{
		boolean isok = true;

		if ( CVSLog.debugOpen )
			(new Throwable( "OPEN CVS LOG")).printStackTrace();

		if ( CVSLog.debug)
			System.err.println
				( "CVSLog.openLogFile( " + CVSLog.filename + " )" );

		if ( CVSLog.filename == null )
			return;

		try {
			CVSLog.file = new FileWriter( CVSLog.filename );
			}
		catch ( Exception ex )
			{
			CVSLog.logMsg
				( "error opening log file '" + CVSLog.filename
					+ "', trying 'user.dir' - " + ex.getMessage() );

			String userDirStr = System.getProperty( "user.dir", "" );

			CVSLog.filename =
				userDirStr + File.separator + CVSLog.DEFAULT_FILENAME;

			try {
				CVSLog.file = new FileWriter( CVSLog.filename );
				}
			catch ( Exception ex2 )
				{
				CVSLog.logMsg
					( "error opening log file '" + CVSLog.filename
						+ "' - " + ex2.getMessage() );

				CVSLog.file = null;
				isok = false;
				}
			}

		if ( isok )
			{
			CVSLog.stream = new PrintWriter( CVSLog.file );
			CVSLog.open = true;
			}

		CVSLog.echo = false;
		}

    static public void
    closeLog()
        {
		if ( CVSLog.open )
			{
			CVSLog.open = false;
			if ( CVSLog.stream != null )
				{
				CVSLog.stream.flush();
				CVSLog.stream.close();
				}
			}
		CVSLog.checked = false;
        }

    static public void
    setEcho( boolean setting )
        {
        CVSLog.echo = setting;
        }

	static public void
	traceMsg( Throwable thrown, String msg )
		{
		CVSLog.logMsg( msg );
		CVSLog.logMsg( thrown.getMessage() );

		if ( ! CVSLog.open )
			thrown.printStackTrace( System.err );
		else
			thrown.printStackTrace( CVSLog.stream );

		if ( CVSLog.autoFlush && CVSLog.open )
			CVSLog.stream.flush();
		}


	static public void
	logMsg( String msg )
		{
		CVSLog.checkLogOpen();

		if ( CVSLog.open )
			{
			CVSLog.stream.println( msg );
			if ( CVSLog.autoFlush && CVSLog.open )
				CVSLog.stream.flush();
			}

	    if ( CVSLog.echo )
	        {
	        System.out.println( msg );
	        }
		}

	static public void
	logMsgStdout( String msg )
		{
		CVSLog.checkLogOpen();

		if ( CVSLog.open )
			{
			CVSLog.stream.println( msg );
			if ( CVSLog.autoFlush && CVSLog.open )
				CVSLog.stream.flush();
			}

	    System.out.println( msg );
		}

	static public void
	logMsgStderr( String msg )
		{
		CVSLog.checkLogOpen();

		if ( CVSLog.open )
			{
			CVSLog.stream.println( msg );
			if ( CVSLog.autoFlush && CVSLog.open )
				CVSLog.stream.flush();
			}

	    System.err.println( msg );
		}
	}



