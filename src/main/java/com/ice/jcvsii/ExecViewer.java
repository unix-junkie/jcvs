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

package com.ice.jcvsii;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Hashtable;

import javax.activation.CommandObject;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.swing.JOptionPane;

import com.ice.cvsc.CVSLog;
import com.ice.util.StringUtilities;


/**
 * Implements a consistent exec() interface.
 *
 * @author Tim Endres,
 *  <a href="mailto:time@gjt.org">time@gjt.org</a>
 */

//
// UW-sep-char  Urban Widmark <urban@svenskatest.se>
// Excel, and other applications, insist that any path passed
// to them be in native format. Thus, we normalize...
//

class		ExecViewer
extends		Thread
implements	CommandObject
	{
	private Process			proc;

	private BufferedReader errRdr;


		/**
	 * the CommandObject method to accept our DataHandler
	 * @param dh The datahandler used to get the content.
	 */
	@Override
	public void
	setCommandContext( final String verb, final DataHandler dh )
		throws IOException
		{
		final DataSource ds = dh.getDataSource();

		// REVIEW
		// UNDONE
		// This code is worthless, fix it!
		//
		String fileName = "unknown";
		if ( ds instanceof FileDataSource )
			{
			final FileDataSource fds = (FileDataSource) ds;
			fileName = fds.getFile().getPath();
			}

		this.exec( verb, dh );
		}

	public void
	exec( final String verb, final DataHandler dh )
		{
		final String cmdSpec = null;
		String extension = null;

		final DataSource ds = dh.getDataSource();

		if ( ! (ds instanceof FileDataSource) )
			{
			// UNDONE
			return;
			}

		final FileDataSource fds = (FileDataSource) ds;
		final File file = fds.getFile();

		final String name = file.getName();
		String path = file.getParent();
		String fileName = file.getAbsolutePath();
		String cwdPath = Config.getPreferences().getCurrentDirectory();

		// UW-sep-char
		// Some programs (namely windows) like Excel do not like / in
		// pathnames, so we replace / with the platform file separator.
		//
		// @author Urban Widmark <urban@svenskatest.se>
		//
		path = path.replace( '/', File.separatorChar );
		cwdPath = cwdPath.replace( '/', File.separatorChar );
		fileName = fileName.replace( '/', File.separatorChar );

		String envSpec = null;
		String argSpec = null;

		String[] env = null;
		String[] args = null;

		final Config cfg = Config.getInstance();

		final int index = name.lastIndexOf('.');
		if ( index != -1 && index < name.length() - 1 )
			{
			extension = name.substring( index );
			envSpec = cfg.getExecCommandEnv( verb, extension );
			argSpec = cfg.getExecCommandArgs( verb, extension );
			}
		else
			{
			envSpec = cfg.getExecCommandEnv(verb, '.' + name );
			argSpec = cfg.getExecCommandArgs(verb, '.' + name );
			}

		if ( argSpec == null )
			{
			envSpec = cfg.getExecCommandEnv( verb, "._DEF_" );
			argSpec = cfg.getExecCommandArgs( verb, "._DEF_" );
			}

		if ( argSpec == null )
			{
			final String[] fmtArgs = { verb, fileName, extension };
			final String msg = ResourceMgr.getInstance().getUIFormat
				( "execviewer.not.found.msg", fmtArgs );
			final String title = ResourceMgr.getInstance().getUIString
				( "execviewer.not.found.title" );
			JOptionPane.showMessageDialog
				( null, msg, title, JOptionPane.ERROR_MESSAGE );
			return;
			}

		final Hashtable subHash = new Hashtable();

		// UW-path-spaces
		// Some platforms (namely Windows) encourage spaces in their
		// filenames, but their applications expect them wrapped inside
		// quotes.
		//
		// @author Urban Widmark <urban@svenskatest.se>
		//
		if ( cfg.isWindows() && fileName.indexOf( ' ' ) > -1 )
			{
			fileName = '"' + fileName + '"';
			}

		subHash.put( "FILE", fileName );
		subHash.put( "PATH", path );
		subHash.put( "NAME", name );
		subHash.put( "CWD", cwdPath );

		if ( false )
			System.err.println( "EXECVIEWER:  VARS =" + subHash );

		env = this.parseCommandEnv( envSpec, subHash );
		args = this.parseCommandArgs( argSpec, subHash );

		if ( false )
		for ( int ai = 0 ; ai < args.length ; ++ai )
			System.err.println( "EXECVIEWER:  args["+ai+"] =" + args[ai] );

		if ( false )
		for ( int ei = 0 ; ei < env.length ; ++ei )
			System.err.println( "EXECVIEWER:  env["+ei+"] =" + env[ei] );

		try {
			this.proc = env.length < 1 ? Runtime.getRuntime().exec(args) : Runtime.getRuntime().exec(args, env);

			this.start();
			}
		catch ( final IOException ex )
			{
			final String[] fmtArgs = { verb, fileName, ex.getMessage() };
			final String msg = ResourceMgr.getInstance().getUIFormat
				( "execviewer.exec.error.msg", fmtArgs );
			final String title = ResourceMgr.getInstance().getUIString
				( "execviewer.exec.error.title" );
			JOptionPane.showMessageDialog
				( null, msg, title, JOptionPane.ERROR_MESSAGE );
			}
		}

	private String[]
	parseCommandArgs(final String argStr, final Hashtable subHash)
		{
		if ( argStr == null || argStr.isEmpty())
			return new String[0];

		final String[] args = StringUtilities.parseArgumentString( argStr );
		return StringUtilities.argumentSubstitution( args, subHash );
		}

	private String[]
	parseCommandEnv(final String envStr, final Hashtable subHash)
		{
		if ( envStr == null || envStr.isEmpty())
			return new String[0];

		final String[] env = StringUtilities.parseArgumentString( envStr );
		return StringUtilities.argumentSubstitution( env, subHash );
		}

	@Override
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

			final Thread t = new Thread(() ->
						{
						try {
							for ( ; ; )
								{
								final String ln = errRdr.readLine();
								if ( ln == null )
									break;
								}

							errRdr.close();
							}
						catch ( final IOException ex )
							{
							CVSLog.traceMsg
								( ex, "reading exec stderr stream" );
							}
						}
				);

			t.start();

			// STDOUT
			final BufferedReader outRdr = new BufferedReader
					(new InputStreamReader
							 (this.proc.getInputStream()));

			for ( ; ; )
				{
				final String ln = outRdr.readLine();
				if ( ln == null )
					break;
				}

			outRdr.close();

			try { t.join(); }
			catch ( final InterruptedException ex )
				{
				CVSLog.traceMsg
					( ex, "interrupted joining the stderr reader" );
				}
			}
		catch ( final IOException ex )
			{
			CVSLog.traceMsg
				( ex, "reading exec stdout stream" );
			}

		try { proc.waitFor(); }
		catch ( final InterruptedException ex )
			{
			CVSLog.traceMsg
				( ex, "interrupted waiting for process" );
			}

		final int exitVal = proc.exitValue();
		}

	}

