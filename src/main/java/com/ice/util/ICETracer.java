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

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * The ICETracer class implements the a stack tracing mechanism
 * for debugging use. This is a <strong>strictly</strong> class
 * based interface. There are no instance methods.
 *
 * @version $Revision: 1.4 $
 * @author Timothy Gerard Endres,
 *    <a href="mailto:time@ice.com">time@ice.com</a>.
 *
 * @see UserProperties
 *
 */

final class
ICETracer {
	public static final String		RCS_ID = "$Id: ICETracer.java,v 1.4 1998/04/29 16:30:13 time Exp $";
	public static final String		RCS_REV = "$Revision: 1.4 $";

	private static PrintWriter		out;

	private static boolean			state;
	private static final boolean			ifOverOn = true;

	private static boolean			outIsSystem;

	private static boolean			echoAccum;
	private static StringBuffer		outBuffer;

	private ICETracer() {
	}


	public static void
	setTraceState( final boolean state )
		{
		ICETracer.state = state;
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
			System.err.println( line );
			}
		}

	public static void
	trace( final String line )
		{
		if ( line == null )
			return;

		if ( state )
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
	traceWithStack( final String line )
		{
		if ( line == null )
			return;

		final Throwable thrower = new Throwable( line );

		if ( state )
			{
			println( line );
			}

		if ( out == null )
			thrower.printStackTrace( System.err );
		else
			thrower.printStackTrace( out );
		}

	private static String
	getStackLines(final Throwable thrower)
		{
		final StringWriter sWrtr = new StringWriter();
		final PrintWriter pWrtr = new PrintWriter( sWrtr );
		thrower.printStackTrace( pWrtr );
		return sWrtr.toString();
		}

	private static String
	getStackLines(final Throwable thrower, final int maxLines)
		{
		if ( maxLines == 0 )
			return getStackLines( thrower );

		final StringWriter sWrtr = new StringWriter();
		final PrintWriter pWrtr = new PrintWriter( sWrtr );

		thrower.printStackTrace( pWrtr );

		final String trcStr = sWrtr.getBuffer().toString();

		final String sep = System.getProperty( "line.separator", "\n" );

		int offset = 0;
		int index = trcStr.length();
		for ( int ln = 0 ; ln < maxLines ; ++ln )
			{
			final int idx = trcStr.indexOf( sep, offset );
			if ( idx == -1 )
				break;

			index = idx;
			offset = idx + 1;
			}

		return trcStr.substring( 0, index );
		}

	public static void
	traceWithStack( final int maxPrintLines, final String line )
		{
		if ( line == null || maxPrintLines < 1 )
			return;

		final Throwable thrower = new Throwable( line );

		if ( state )
			{
			println( line );
			}

		final String outStr =
			getStackLines( thrower, maxPrintLines );

		if ( out == null )
			System.err.println( outStr );
		else
			out.println( outStr );
		}

	public static void
	traceWithStack( final Throwable thrower, final String line )
		{
		if ( thrower == null && line == null )
			return;

		if ( line != null )
			println( line );

		final String outStr = getStackLines( thrower, 0 );

		if ( out == null )
			System.err.println( outStr );
		else
			out.println( outStr );
		}

	public static void
	traceWithStack( final Throwable thrower, final int lines, final String line )
		{
		if ( thrower == null && line == null )
			return;

		if ( line != null )
			println( line );

		final String outStr =
			getStackLines( thrower, lines );

		if ( out == null )
			System.err.println( outStr );
		else
			out.println( outStr );
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

