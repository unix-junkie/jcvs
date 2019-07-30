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

import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * The CVSTracer class implements the a tracing mechanism for
 * the CVS package. This allows for more control and details
 * than a simple 'CVSLog.logMsg()' provides. This is a
 * <strong>strictly</strong> class based interface. There are
 * no instance methods.
 *
 * @version $Revision: 2.4 $
 * @author Timothy Gerard Endres, <a href="mailto:time@ice.com">time@ice.com</a>.
 * @see CVSClient
 *
 */

public final class
CVSTracer {
	public static final String		RCS_ID = "$Id: CVSTracer.java,v 2.4 2003/07/27 01:08:32 time Exp $";
	public static final String		RCS_REV = "$Revision: 2.4 $";

	private static PrintWriter		out;

	private static boolean			on;
	private static final boolean			ifOverOn = true;

	private static boolean			outIsSystem;

	private static boolean			echoAccum;
	private static StringBuffer		outBuffer;

	private CVSTracer() {
	}


	public static void
	turnOn()
		{
		on = true;
		}

	public static void
	turnOff()
		{
		on = false;
		}

	public static void
	setEchoAccumulation( final boolean state )
		{
		echoAccum = state;
		}

	public static void
	accumulateInBuffer( final StringBuffer buffer )
		{
		outBuffer = buffer;
		}

	public static void
	turnOffAccumulation()
		{
		outBuffer = null;
		}

	public static StringBuffer
	getAccumulationBuffer()
		{
		return outBuffer;
		}

	private static void
	println(final String line)
		{
		if ( line == null )
			return;

		if ( outBuffer != null )
			{
			outBuffer.append( line );
			outBuffer.append('\n');

			if ( ! echoAccum )
				return;
			}

		if ( out != null )
			{
			out.println( line );
			}
		else
			{
			CVSLog.logMsg( line );
			}
		}

	public static void
	trace( final String line )
		{
		if ( line == null )
			return;

		if ( on )
			{
			println( line );
			}
		}

	public static void
	traceIf( final boolean flag, final String line )
		{
		if ( ! flag || line == null )
			return;

		if ( ifOverOn )
			{
			println( line );
			}
		}

	public static void
	traceException( final String line, final Throwable ex )
		{
		if ( line == null )
			return;

		if ( on )
			{
			println( line );
			}

		if ( out == null )
			ex.printStackTrace( System.err );
		else
			ex.printStackTrace( out );
		}

	public static void
	traceWithStack( final String line )
		{
		if ( line == null )
			return;

		final Throwable thrower = new Throwable( line );

		if ( on )
			{
			println( line );
			}

		if ( out == null )
			thrower.printStackTrace( System.err );
		else
			thrower.printStackTrace( out );
		}

	private static void
	checkClose()
		{
		if ( out != null )
			{
			if ( ! outIsSystem )
				{
				out.close();
				out = null;
				outIsSystem = false;
				}
			}
		}

	/**
	 * Sets the tracer's output writer to the BufferedWriter
	 * passed in. The new writer <em>newOut</em> <strong>must never</strong>
	 * be System.err or System.err, since the writer will be
	 * closed at some point.
	 *
	 * @param newOut The new buffered writer to send trace output to.
	 */

	public static void
	setWriter( final PrintWriter newOut )
		{
		checkClose();

		out = newOut;
		outIsSystem = false;

		outBuffer = null;
		}

	public static void
	setWriterToStdout()
		{
		final PrintWriter newOut =
			new PrintWriter(
				new OutputStreamWriter( System.out ) );

		if ( newOut != null )
			{
			checkClose();
			out = newOut;
			outIsSystem = true;

			outBuffer = null;
			}
		}

	public static void
	setWriterToStderr()
		{
		final PrintWriter newOut =
			new PrintWriter(
				new OutputStreamWriter( System.err ) );

		if ( newOut != null )
			{
			checkClose();
			out = newOut;
			outIsSystem = true;

			outBuffer = null;
			}
		}

	}

