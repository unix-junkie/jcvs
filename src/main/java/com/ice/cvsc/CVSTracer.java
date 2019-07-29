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
import java.text.*;
import java.util.*;

/**
 * The CVSTracer class implements the a tracing mechanism for
 * the CVS package. This allows for more control and details
 * than a simple 'CVSLog.logMsg()' provides. This is a
 * <strong>strictly</strong> class based interface. There are
 * no instance methods.
 *
 * @version $Revision: 2.3 $
 * @author Timothy Gerard Endres, <a href="mailto:time@ice.com">time@ice.com</a>.
 * @see CVSClient
 *
 */

public class
CVSTracer extends Object
	{
	static public final String		RCS_ID = "$Id: CVSTracer.java,v 2.3 1999/04/01 17:50:50 time Exp $";
	static public final String		RCS_REV = "$Revision: 2.3 $";

	static private PrintWriter		out = null;
									
	static private boolean			on = false;
	static private boolean			ifOverOn = true;

	static private int				traceState = 0;

	static private boolean			outIsSystem = false;

	static private boolean			echoAccum = false;
	static private StringBuffer		outBuffer = null;


	static public void
	turnOn()
		{
		CVSTracer.on = true;
		}

	static public void
	turnOff()
		{
		CVSTracer.on = false;
		}

	static public void
	setEchoAccumulation( boolean state )
		{
		CVSTracer.echoAccum = state;
		}

	static public void
	accumulateInBuffer( StringBuffer buffer )
		{
		CVSTracer.outBuffer = buffer;
		}

	static public void
	turnOffAccumulation()
		{
		CVSTracer.outBuffer = null;
		}

	static public StringBuffer
	getAccumulationBuffer()
		{
		return CVSTracer.outBuffer;
		}

	static public void
	println( String line )
		{
		if ( line == null )
			return;

		if ( CVSTracer.outBuffer != null )
			{
			CVSTracer.outBuffer.append( line );
			CVSTracer.outBuffer.append( "\n" );

			if ( ! CVSTracer.echoAccum )
				return;
			}

		if ( out != null )
			{
			CVSTracer.out.println( line );
			}
		else
			{
			CVSLog.logMsg( line );
			}
		}

	static public void
	trace( String line )
		{
		if ( line == null )
			return;

		if ( CVSTracer.on )
			{
			CVSTracer.println( line );
			}
		}

	static public void
	traceIf( boolean flag, String line )
		{
		if ( (! flag) || (line == null) )
			return;

		if ( CVSTracer.ifOverOn )
			{
			CVSTracer.println( line );
			}
		}

	static public void
	traceException( String line, Exception ex )
		{
		if ( line == null )
			return;

		if ( CVSTracer.on )
			{
			CVSTracer.println( line );
			}

		if ( CVSTracer.out == null )
			ex.printStackTrace( System.err );
		else
			ex.printStackTrace( CVSTracer.out );
		}

	static public void
	traceWithStack( String line )
		{
		if ( line == null )
			return;

		Throwable thrower = new Throwable( line );

		if ( CVSTracer.on )
			{
			CVSTracer.println( line );
			}

		if ( CVSTracer.out == null )
			thrower.printStackTrace( System.err );
		else
			thrower.printStackTrace( CVSTracer.out );
		}

	static private void
	checkClose()
		{
		if ( CVSTracer.out != null )
			{
			if ( ! CVSTracer.outIsSystem )
				{
				CVSTracer.out.close();
				CVSTracer.out = null;
				CVSTracer.outIsSystem = false;
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
	setWriter( PrintWriter newOut )
		{
		CVSTracer.checkClose();

		CVSTracer.out = newOut;
		CVSTracer.outIsSystem = false;

		CVSTracer.outBuffer = null;
		}

	static public void
	setWriterToStdout()
		{
		PrintWriter newOut =
			new PrintWriter(
				new OutputStreamWriter( System.out ) );

		if ( newOut != null )
			{
			CVSTracer.checkClose();
			CVSTracer.out = newOut;
			CVSTracer.outIsSystem = true;

			CVSTracer.outBuffer = null;
			}
		}

	static public void
	setWriterToStderr()
		{
		PrintWriter newOut =
			new PrintWriter(
				new OutputStreamWriter( System.err ) );

		if ( newOut != null )
			{
			CVSTracer.checkClose();
			CVSTracer.out = newOut;
			CVSTracer.outIsSystem = true;

			CVSTracer.outBuffer = null;
			}
		}

	}

