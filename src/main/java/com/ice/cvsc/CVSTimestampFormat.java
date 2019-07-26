/*
** Java cvs client library package.
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

package com.ice.cvsc;

import java.lang.*;
import java.text.*;
import java.util.*;

/**
 * The CVSTimestampFormat class implements the code necessary
 * to format and parse CVS Entry timestamps, which come in the
 * flavor of 'Wed Mar  4 1997 15:43:06'.
 *
 * <strong>NOTE</strong> This class <em>explicitly</em> operates
 * entirely in the 'Locale.US' locality. Thus, this class is
 * <em>not</em> useful for display purposes, since the values
 * are not localized.
 *
 * @version $Revision: 2.4 $
 * @author Timothy Gerard Endres, <a href="mailto:time@ice.com">time@ice.com</a>.
 * @see CVSClient
 * @see CVSRequest
 */

public class
CVSTimestampFormat	extends Format
	{																
	static public final String		RCS_ID = "$Id: CVSTimestampFormat.java,v 2.4 1999/04/01 17:50:29 time Exp $";
	static public final String		RCS_REV = "$Revision: 2.4 $";

	static public final String		DEFAULT_GMT_TZID = "GMT";

	static private TimeZone		tz;
	static private String		timezoneID;


	static
		{
		CVSTimestampFormat.timezoneID =
			CVSTimestampFormat.DEFAULT_GMT_TZID;

		CVSTimestampFormat.tz =
			TimeZone.getTimeZone
				( CVSTimestampFormat.timezoneID ); 
		}

    static public final CVSTimestampFormat
	getInstance()
		{
		return new CVSTimestampFormat();
		}

    static public final void
	setTimeZoneID( String timezoneID )
		{
		CVSTimestampFormat.timezoneID = timezoneID;
		CVSTimestampFormat.tz =
			TimeZone.getTimeZone( CVSTimestampFormat.timezoneID );
		}

	public
	CVSTimestampFormat()
		{
		super();
		}

	public String
	format( CVSTimestamp stamp )
		throws IllegalArgumentException
		{
		return
			this.formatTimeZone
				( stamp, CVSTimestampFormat.tz );
		}

	public String
	formatTimeZone( CVSTimestamp stamp, TimeZone tz )
		throws IllegalArgumentException
		{
		Locale loc = Locale.US;
		SimpleDateFormat dateFormat;

		dateFormat =
			new SimpleDateFormat
				( "EEE MMM dd HH:mm:ss yyyy", loc );

		dateFormat.setTimeZone( tz );
		String result = dateFormat.format( stamp );

		return result;
		}

	public StringBuffer
	format( Object stamp, StringBuffer appendTo, FieldPosition fieldPos )
		throws IllegalArgumentException
		{
		// UNDONE - handle fieldPos!
		String tmpFormat = this.format( (CVSTimestamp)stamp );
		appendTo.append( tmpFormat );
		return appendTo;
		}

	public String
	formatTerse( CVSTimestamp stamp )
		{
		return
			this.formatTerseTimeZone
				( stamp, CVSTimestampFormat.tz );
		}

	public String
	formatTerseTimeZone( CVSTimestamp stamp, TimeZone tz )
		{
		Locale loc = Locale.US;
		SimpleDateFormat dateFormat;

		dateFormat = new SimpleDateFormat( "yyMMdd HH:mm", loc );

		dateFormat.setTimeZone( CVSTimestampFormat.tz );

		String result = dateFormat.format( stamp );

		return result;
		}

	public CVSTimestamp
	parse( String source )
		throws ParseException
		{
		return parseTimestamp( source );
		}

	public Object
	parseObject( String source, ParsePosition pos )
		{
		CVSTimestamp stamp = null;

		try {
			stamp = this.parseTimestamp( source );
			}
		catch ( ParseException ex )
			{
			stamp = null;
			}

		return (Object) stamp;
		}

	public CVSTimestamp
	parseTimestamp( String source )  
		throws ParseException
		{
		ParsePosition pos = new ParsePosition(0);
		return this.parseTimestamp( source, pos );
		}

	public CVSTimestamp
	parseTimestamp( String source, ParsePosition pos )  
		throws ParseException
		{
		Locale loc = Locale.US;
		SimpleDateFormat dateFormat;

		dateFormat =
			new SimpleDateFormat
				( "EEE MMM dd HH:mm:ss yyyy", loc );

		dateFormat.setTimeZone( CVSTimestampFormat.tz );

		Date result = dateFormat.parse( source, pos );

		// NOTE SimpleDateFormat IGNORANTLY returns null instead
		//      of throwing a ParseException
		if ( result == null )
			throw new ParseException
				( "invalid timestamp '" + source + "'", 0 );

		return new CVSTimestamp( result );
		}

	public static void
	main( String[] args )
		{
		CVSTimestampFormat fmt = CVSTimestampFormat.getInstance();

		try {
			CVSTimestamp ts =
				fmt.parseTimestamp( args[0], new ParsePosition(0) );

			System.err.println( "TS = " + ts );
			}
		catch ( ParseException ex )
			{
			ex.printStackTrace();
			}
		}

	}

