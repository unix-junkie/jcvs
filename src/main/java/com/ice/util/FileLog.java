/*
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


package com.ice.util;

import java.io.FileWriter;
import java.io.PrintWriter;


public class
FileLog extends Object
	{
	private static final String		RCS_ID = "$Id: FileLog.java,v 1.3 1998/12/26 20:59:06 time Exp $";
	private static final String		RCS_REV = "$Revision: 1.3 $";
	private static final String		RCS_NAME = "$Name:  $";

	private static final String		DEFAULT_FILENAME = "log.txt";

	private static FileLog			defaultLogger = null;

	private String			filename;
	private FileWriter		file;
	private PrintWriter		stream;
    private boolean			open;
    private boolean			echo;
    private boolean			autoFlush;


	static public FileLog
	getDefaultLogger()
		{
		return FileLog.defaultLogger;
		}

	static public FileLog
	setDefaultLogger( final FileLog logger )
		{
		final FileLog	old = FileLog.defaultLogger;
		FileLog.defaultLogger = logger;
		return old;
		}

	static public void
	checkDefaultLogger()
		{
		if ( FileLog.defaultLogger == null )
			{
			FileLog.defaultLogger =
				new FileLog( FileLog.DEFAULT_FILENAME );
			}
		}

	public
	FileLog( final String filename )
		{
		this.open = false;
		this.echo = false;
		this.autoFlush = true;
		this.filename = filename;
		this.file = null;
		this.stream = null;
		}

    public void
    setLogFilename( final String filename )
        {
		this.filename = filename;
        }

	public void
    setAutoFlush( final boolean autoflush )
        {
        this.autoFlush = autoFlush;
        }

    public void
    checkLogOpen()
        {
		if ( ! this.open )
			{
			this.openLogFile();
			}
        }

	public void
	openLogFile()
		{
		boolean isok = true;

		try {
			this.file = new FileWriter( this.filename );
			}
		catch ( final Exception ex )
			{
			System.err.println
				( "error opening log file '" + this.filename
					+ "' - " + ex.getMessage() );
			this.file = null;
			isok = false;
			}

		if ( isok )
			{
			this.stream = new PrintWriter( this.file );
			this.open = true;
			}

		this.echo = false;
		}

    public void
    closeLog()
        {
		if ( this.open )
			{
			this.open = false;
			if ( this.stream != null )
				{
				this.stream.flush();
				this.stream.close();
				this.stream = null;
				}
			}
        }

    public void
    setEcho( final boolean setting )
        {
        this.echo = setting;
        }

	public void
	traceMsg( final Throwable thrown, final String msg )
		{
		this.logMsg( msg );
		this.logMsg( thrown.getMessage() );

		if ( ! this.open )
			thrown.printStackTrace( System.err );
		else
			thrown.printStackTrace( this.stream );

		if ( this.autoFlush && this.open )
			this.stream.flush();
		}


	static public void
	defLogMsg( final String msg )
		{
		FileLog.checkDefaultLogger();
		if ( FileLog.defaultLogger != null )
			FileLog.defaultLogger.logMsg( msg );
		}

	public void
	logMsg( final String msg )
		{
		this.checkLogOpen();

		if ( this.open )
			{
			this.stream.println( msg );
			if ( this.autoFlush && this.open )
				this.stream.flush();
			}

	    if ( this.echo )
	        {
	        System.out.println( msg );
	        }
		}

	static public void
	defLogMsgStdout( final String msg )
		{
		FileLog.checkDefaultLogger();
		if ( FileLog.defaultLogger != null )
			FileLog.defaultLogger.logMsgStdout( msg );
		}

	public void
	logMsgStdout( final String msg )
		{
		this.checkLogOpen();

		if ( this.open )
			{
			this.stream.println( msg );
			if ( this.autoFlush && this.open )
				this.stream.flush();
			}

	    System.out.println( msg );
		}

	static public void
	defLogMsgStderr( final String msg )
		{
		FileLog.checkDefaultLogger();
		if ( FileLog.defaultLogger != null )
			FileLog.defaultLogger.logMsgStderr( msg );
		}

	public void
	logMsgStderr( final String msg )
		{
		this.checkLogOpen();

		if ( this.open )
			{
			this.stream.println( msg );
			if ( this.autoFlush && this.open )
				this.stream.flush();
			}

	    System.err.println( msg );
		}
	}



