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
 * @see com.ice.util.UserProperties
 *
 */

public class
ICETracer extends Object
	{
	static public final String		RCS_ID = "$Id: ICETracer.java,v 1.4 1998/04/29 16:30:13 time Exp $";
	static public final String		RCS_REV = "$Revision: 1.4 $";

	static private PrintWriter		out = null;

	static private boolean			state = false;
	static private boolean			ifOverOn = true;

	static private int				traceState = 0;

	static private boolean			outIsSystem = false;

	static private boolean			echoAccum = false;
	static private StringBuffer		outBuffer = null;


	static public void
	setTraceState( final boolean state )
		{
		ICETracer.state = state;
		}

	static public void
	setEchoAccumulation( final boolean state )
		{
		ICETracer.echoAccum = state;
		}

	static public void
	accumulateInBuffer( final StringBuffer buffer )
		{
		ICETracer.outBuffer = buffer;
		}

	static public void
	turnOffAccumulation()
		{
		ICETracer.outBuffer = null;
		}

	static public StringBuffer
	getAccumulationBuffer()
		{
		return ICETracer.outBuffer;
		}

	static public void
	println( final String line )
		{
		if ( line == null )
			return;

		if ( ICETracer.outBuffer != null )
			{
			ICETracer.outBuffer.append( line );
			ICETracer.outBuffer.append( "\n" );

			if ( ! ICETracer.echoAccum )
				return;
			}

		if ( out != null )
			{
			ICETracer.out.println( line );
			}
		else
			{
			System.err.println( line );
			}
		}

	static public void
	trace( final String line )
		{
		if ( line == null )
			return;

		if ( ICETracer.state )
			{
			ICETracer.println( line );
			}
		}

	static public void
	traceIf( final boolean flag, final String line )
		{
		if ( ! flag || line == null )
			return;

		if ( ICETracer.ifOverOn )
			{
			ICETracer.println( line );
			}
		}

	static public void
	traceWithStack( final String line )
		{
		if ( line == null )
			return;

		final Throwable thrower = new Throwable( line );

		if ( ICETracer.state )
			{
			ICETracer.println( line );
			}

		if ( ICETracer.out == null )
			thrower.printStackTrace( System.err );
		else
			thrower.printStackTrace( ICETracer.out );
		}

	static public String
	getStackLines( final Throwable thrower )
		{
		final StringWriter sWrtr = new StringWriter();
		final PrintWriter pWrtr = new PrintWriter( sWrtr );
		thrower.printStackTrace( pWrtr );
		return sWrtr.toString();
		}

	static public String
	getStackLines( final Throwable thrower, final int maxLines )
		{
		if ( maxLines == 0 )
			return ICETracer.getStackLines( thrower );

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

	static public void
	traceWithStack( final int maxPrintLines, final String line )
		{
		if ( line == null || maxPrintLines < 1 )
			return;

		final Throwable thrower = new Throwable( line );

		if ( ICETracer.state )
			{
			ICETracer.println( line );
			}

		final String outStr =
			ICETracer.getStackLines( thrower, maxPrintLines );

		if ( ICETracer.out == null )
			System.err.println( outStr );
		else
			ICETracer.out.println( outStr );
		}

	static public void
	traceWithStack( final Throwable thrower, final String line )
		{
		if ( thrower == null && line == null )
			return;

		if ( line != null )
			ICETracer.println( line );

		final String outStr = ICETracer.getStackLines( thrower, 0 );

		if ( ICETracer.out == null )
			System.err.println( outStr );
		else
			ICETracer.out.println( outStr );
		}

	static public void
	traceWithStack( final Throwable thrower, final int lines, final String line )
		{
		if ( thrower == null && line == null )
			return;

		if ( line != null )
			ICETracer.println( line );

		final String outStr =
			ICETracer.getStackLines( thrower, lines );

		if ( ICETracer.out == null )
			System.err.println( outStr );
		else
			ICETracer.out.println( outStr );
		}

	static private void
	checkClose()
		{
		if ( ICETracer.out != null )
			{
			if ( ! ICETracer.outIsSystem )
				{
				ICETracer.out.close();
				ICETracer.out = null;
				ICETracer.outIsSystem = false;
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

	static public void
	setWriter( final PrintWriter newOut )
		{
		ICETracer.checkClose();

		ICETracer.out = newOut;
		ICETracer.outIsSystem = false;

		ICETracer.outBuffer = null;
		}

	static public void
	setWriterToStdout()
		{
		final PrintWriter newOut =
			new PrintWriter(
				new OutputStreamWriter( System.out ) );

		if ( newOut != null )
			{
			ICETracer.checkClose();
			ICETracer.out = newOut;
			ICETracer.outIsSystem = true;

			ICETracer.outBuffer = null;
			}
		}

	static public void
	setWriterToStderr()
		{
		final PrintWriter newOut =
			new PrintWriter(
				new OutputStreamWriter( System.err ) );

		if ( newOut != null )
			{
			ICETracer.checkClose();
			ICETracer.out = newOut;
			ICETracer.outIsSystem = true;

			ICETracer.outBuffer = null;
			}
		}

	}

