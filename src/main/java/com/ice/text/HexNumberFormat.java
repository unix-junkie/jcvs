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

package com.ice.text;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;

/**
 * The HexNumberFormat class implements the code necessary
 * to format and parse Hexidecimal integer numbers.
 *
 * @version $Revision: 1.2 $
 * @author Timothy Gerard Endres, <a href="mailto:time@ice.com">time@ice.com</a>.
 * @see java.text.NumberFormat
 */

public class
HexNumberFormat	extends Format
	{
	public static final String		RCS_ID = "$Id: HexNumberFormat.java,v 1.2 1999/04/01 17:27:42 time Exp $";
	public static final String		RCS_REV = "$Revision: 1.2 $";

	private static final char[]		lowChars;
	private static final char[]		uprChars;

	private final int					count;
		private static char[]		hexChars;

	static
		{
		lowChars = new char[20];
		uprChars = new char[20];

		uprChars[0] = lowChars[0] = '0';
		uprChars[1] = lowChars[1] = '1';
		uprChars[2] = lowChars[2] = '2';
		uprChars[3] = lowChars[3] = '3';
		uprChars[4] = lowChars[4] = '4';
		uprChars[5] = lowChars[5] = '5';
		uprChars[6] = lowChars[6] = '6';
		uprChars[7] = lowChars[7] = '7';
		uprChars[8] = lowChars[8] = '8';
		uprChars[9] = lowChars[9] = '9';
		uprChars[10] = 'A'; lowChars[10] = 'a';
		uprChars[11] = 'B'; lowChars[11] = 'b';
		uprChars[12] = 'C'; lowChars[12] = 'c';
		uprChars[13] = 'D'; lowChars[13] = 'd';
		uprChars[14] = 'E'; lowChars[14] = 'e';
		uprChars[15] = 'F'; lowChars[15] = 'f';
		}

    public static HexNumberFormat
	getInstance()
		{
		return new HexNumberFormat( "XXXXXXXX" );
		}

	public
	HexNumberFormat( final CharSequence pattern )
		{
		super();
			this.count = pattern.length();
		hexChars =
			pattern.charAt(0) == 'X'
				? uprChars
				: lowChars;
		}

	public String
	format( final int hexNum )
		throws IllegalArgumentException
		{
		final FieldPosition pos = new FieldPosition(0);
		final StringBuffer hexBuf = new StringBuffer(8);

		this.format(hexNum, hexBuf, pos );

		return hexBuf.toString();
		}

	@Override
	public StringBuffer
	format( final Object hexInt, final StringBuffer appendTo, final FieldPosition fieldPos )
		throws IllegalArgumentException
		{
		final char[] hexBuf = new char[16];

		final int end = fieldPos.getEndIndex();
		final int beg = fieldPos.getBeginIndex();

		int hexNum = (Integer) hexInt;

		for ( int i = 7 ; i >= 0 ; --i )
			{
			hexBuf[i] = hexChars[ hexNum & 0x0F ];
				hexNum >>= 4;
			}

		for ( int i = 8 - this.count ; i < 8 ; ++i )
			{
			appendTo.append( hexBuf[i] );
			}

		return appendTo;
		}

	public int
	parse( final String source )
		throws ParseException
		{
		throw new ParseException( "unimplemented!", 0 );
		}

	@Override
	public Object
	parseObject( final String source, final ParsePosition pos )
		{
		return null;
		}

	}

