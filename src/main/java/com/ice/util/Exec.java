/*
** Copyright (c) 1998 by Timothy Gerard Endres
** <mailto:time@ice.com>  <http://www.ice.com>
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

import java.io.*;
import java.util.*;


/**
 * Implements a consistent exec() interface.
 *
 * @author Tim Endres,
 *  <a href="mailto:time@gjt.org">time@gjt.org</a>
 */

public
class		Exec
extends		Thread
	{
	private String[]		argv;
	private String[]		envp;
	private StringBuffer	results;

	private BufferedReader	errRdr;
	private BufferedReader	outRdr;
	private Process			proc;
	private String			lnSep;

	public
	Exec( StringBuffer resultBuf, String[] argv, String[] envp )
		{
		this.argv = argv;
		this.envp = envp;
		this.results = resultBuf;
		this.lnSep =
			System.getProperty( "line.separator", "\n" );
		}

	public void
	exec()
		throws ExecException
		{
		try {
			this.proc =
				Runtime.getRuntime().exec( this.argv, this.envp );

			this.start();

			try { this.join(); }
			catch ( InterruptedException ex )
				{
				this.appendEx( ex, "joining exec stdout thread" );
				}

			try { proc.waitFor(); }
			catch ( InterruptedException ex )
				{
				this.appendEx( ex, "waiting for exec process" );
				}

			int exitVal = proc.exitValue();

			if ( this.results != null )
				{
				this.append
					( "& Exit status = '" + exitVal + "'." );
				}

			if ( exitVal != 0 )
				{
				throw new ExecException
					( "non-zero exist status", exitVal );
				}

			}
		catch ( IOException ex )
			{
			throw new ExecException
				( "IO exception exec-ing '" + argv[0] + "', "
					+ ex.getMessage(), -1 );
			}
		}

	// UNDONE
	// Should I not be using the File.separator for the "\n"'s below?

	public void
	run()
		{
		try {
			this.proc.getOutputStream().close();

			// STDERR
			this.errRdr =
				new BufferedReader
					( new InputStreamReader
						( this.proc.getErrorStream() ) );

			Thread t = new Thread(
				new Runnable()
					{
					public void
					run()
						{
						try {
							for ( ; ; )
								{
								String ln = errRdr.readLine();
								if ( ln == null )
									break;
								append( ". " + ln );
								}

							errRdr.close();
							}
						catch ( IOException ex )
							{
							appendEx( ex, "reading exec stderr stream" );
							}
						}
					}
				);

			t.start();

			// STDOUT
			this.outRdr =
				new BufferedReader
					( new InputStreamReader
						( this.proc.getInputStream() ) );

			for ( ; this.results != null ; )
				{
				String ln = this.outRdr.readLine();
				if ( ln == null )
					break;				
				this.append( "o " + ln );
				}

			this.outRdr.close();

			try { t.join(); }
			catch ( InterruptedException ex )
				{
				this.appendEx( ex, "joining exec stderr thread" );
				}
			}
		catch ( IOException ex )
			{
			this.appendEx( ex, "reading exec stdout stream" );
			}
		}

	private void
	append( String str )
		{
		if ( this.results != null )
			this.results.append( str + this.lnSep );
		}

	private void
	appendEx( Exception ex, String msg )
		{
		if ( this.results != null )
			{
			StringWriter sW = new StringWriter();
			PrintWriter pW = new PrintWriter( sW );
			ex.printStackTrace( pW );
			this.results.append( "* " + msg + this.lnSep );
			this.results.append( sW.toString() );
			}
		}

	public void
	dump()
		{
		System.err.println( "EXEC PARAMETERS:" );

		for ( int i = 0 ; i < this.argv.length ; ++i )
			System.err.println
				( "   ARGS["+i+"] '" + this.argv[i] + "'" );

		for ( int i = 0 ; i < this.envp.length ; ++i )
			System.err.println
				( "   ENVP["+i+"] '" + this.envp[i] + "'" );
		}

	public void
	debugExec( StringBuffer buf )
		{
		buf.append( "EXEC PARAMETERS: ----------------------" + this.lnSep );
		for ( int i = 0 ; i < this.argv.length ; ++i )
			buf.append( "   ARGS["+i+"] '"+this.argv[i]+"'" + this.lnSep );
		for ( int i = 0 ; i < this.envp.length ; ++i )
			buf.append( "   ENVP["+i+"] '"+this.envp[i]+"'" + this.lnSep );
		}

	}

