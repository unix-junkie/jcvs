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

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

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
 * @version $Revision: 2.5 $
 * @author Timothy Gerard Endres, <a href="mailto:time@ice.com">time@ice.com</a>.
 * @see CVSClient
 * @see CVSRequest
 */

public class
CVSTimestampFormat	extends Format
	{
	public static final String		RCS_ID = "$Id: CVSTimestampFormat.java,v 2.5 2003/07/27 01:08:32 time Exp $";
	public static final String		RCS_REV = "$Revision: 2.5 $";

	private static final String		DEFAULT_GMT_TZID = "GMT";

	private static TimeZone		tz;
	private static String		timezoneID;


	static
		{
		timezoneID =
			DEFAULT_GMT_TZID;

		tz =
			TimeZone.getTimeZone
				( timezoneID );
		}

    public static CVSTimestampFormat
	getInstance()
		{
		return new CVSTimestampFormat();
		}

    public static void
	setTimeZoneID( final String timezoneID )
		{
		CVSTimestampFormat.timezoneID = timezoneID;
		tz =
			TimeZone.getTimeZone( CVSTimestampFormat.timezoneID );
		}

		public String
	format( final Date stamp )
		throws IllegalArgumentException
		{
		return
			this.formatTimeZone
				( stamp, tz );
		}

	private String
	formatTimeZone(final Date stamp, final TimeZone tz)
		throws IllegalArgumentException
		{
		final Locale loc = Locale.US;
		final SimpleDateFormat dateFormat;

		dateFormat =
			new SimpleDateFormat
				( "EEE MMM dd HH:mm:ss yyyy", loc );

		dateFormat.setTimeZone( tz );

			return dateFormat.format(stamp );
		}

	@Override
	public StringBuffer
	format( final Object stamp, final StringBuffer appendTo, final FieldPosition fieldPos )
		throws IllegalArgumentException
		{
		// UNDONE - handle fieldPos!
		final String tmpFormat = this.format((Date) stamp);
		appendTo.append( tmpFormat );
		return appendTo;
		}

	public String
	formatTerse( final Date stamp )
		{
		return
			this.formatTerseTimeZone
				( stamp, tz );
		}

	private String
	formatTerseTimeZone(final Date stamp, final TimeZone tz)
		{
		final Locale loc = Locale.US;
		final SimpleDateFormat dateFormat;

		dateFormat = new SimpleDateFormat( "yyMMdd HH:mm", loc );

		dateFormat.setTimeZone( CVSTimestampFormat.tz );

			return dateFormat.format(stamp );
		}

	public CVSTimestamp
	parse( final String source )
		throws ParseException
		{
		return parseTimestamp( source );
		}

	@Override
	public Object
	parseObject( final String source, final ParsePosition pos )
		{
		CVSTimestamp stamp = null;

		try {
			stamp = this.parseTimestamp( source );
			}
		catch ( final ParseException ex )
			{
			stamp = null;
			}

		return stamp;
		}

	private CVSTimestamp
	parseTimestamp(final String source)
		throws ParseException
		{
		final ParsePosition pos = new ParsePosition(0);
		return this.parseTimestamp( source, pos );
		}

	private CVSTimestamp
	parseTimestamp(final String source, final ParsePosition pos)
		throws ParseException
		{
		final Locale loc = Locale.US;
		final SimpleDateFormat dateFormat;

		dateFormat =
			new SimpleDateFormat
				( "EEE MMM dd HH:mm:ss yyyy", loc );

		dateFormat.setTimeZone( tz );

		final Date result = dateFormat.parse( source, pos );

		// NOTE SimpleDateFormat IGNORANTLY returns null instead
		//      of throwing a ParseException
		if ( result == null )
			throw new ParseException
				("invalid timestamp '" + source + '\'', 0 );

		return new CVSTimestamp( result );
		}

	public static void
	main( final String... args )
		{
		final CVSTimestampFormat fmt = getInstance();

		try {
			final CVSTimestamp ts =
				fmt.parseTimestamp( args[0], new ParsePosition(0) );

			System.err.println( "TS = " + ts );
			}
		catch ( final ParseException ex )
			{
			ex.printStackTrace();
			}
		}

	}

