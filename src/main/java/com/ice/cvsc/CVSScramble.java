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
 * The CVSProject class implements the concept of a local
 * CVS project directory. A local project directory can be
 * thought of as a local source code working directory that
 * contains a CVS directory containing CVS administration files.
 *
 * Combined with CVSClient; sh[i++] = this class provides everything
 * you need to communicate with a CVS Server and maintain
 * local working directories for CVS repositories.
 *
 * @version $Revision: 2.2 $
 * @author Timothy Gerard Endres, <a href="mailto:time@ice.com">time@ice.com</a>.
 * @see CVSClient
 *
 */

/*
 * From src/scramble.c in the cvs distribution.
 *
 * Map characters to each other randomly and symmetrically, A <--> B.
 *
 * We divide the ASCII character set into 3 domains: control chars (0
 * thru 31), printing chars (32 through 126), and "meta"-chars (127
 * through 255).  The control chars map _to_ themselves, the printing
 * chars map _among_ themselves, and the meta chars map _among_
 * themselves.  Why is this thus?
 *
 * No character in any of these domains maps to a character in another
 * domain, because I'm not sure what characters are legal in
 * passwords, or what tools people are likely to use to cut and paste
 * them.  It seems prudent not to introduce control or meta chars,
 * unless the user introduced them first.  And having the control
 * chars all map to themselves insures that newline and
 * carriage-return are safely handled.
 *
	static unsigned char
	shifts[] = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
	17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 114, 120,
	53, 79, 96, 109, 72, 108, 70, 64, 76, 67, 116, 74, 68, 87, 111, 52,
	75, 119, 49, 34, 82, 81, 95, 65, 112, 86, 118, 110, 122, 105, 41, 57,
	83, 43, 46, 102, 40, 89, 38, 103, 45, 50, 42, 123, 91, 35, 125, 55,
	54, 66, 124, 126, 59, 47, 92, 71, 115, 78, 88, 107, 106, 56, 36, 121,
	117, 104, 101, 100, 69, 73, 99, 63, 94, 93, 39, 37, 61, 48, 58, 113,
	32, 90, 44, 98, 60, 51, 33, 97, 62, 77, 84, 80, 85, 223, 225, 216,
	187, 166, 229, 189, 222, 188, 141, 249, 148, 200, 184, 136, 248, 190,
	199, 170, 181, 204, 138, 232, 218, 183, 255, 234, 220, 247, 213, 203,
	226, 193, 174, 172, 228, 252, 217, 201, 131, 230, 197, 211, 145, 238,
	161, 179, 160, 212, 207, 221, 254, 173, 202, 146, 224, 151, 140, 196,
	205, 130, 135, 133, 143, 246, 192, 159, 244, 239, 185, 168, 215, 144,
	139, 165, 180, 157, 147, 186, 214, 176, 227, 231, 219, 169, 175, 156,
	206, 198, 129, 164, 150, 210, 154, 177, 134, 127, 182, 128, 158, 208,
	162, 132, 167, 209, 149, 241, 153, 251, 237, 236, 171, 195, 243, 233,
	253, 240, 194, 250, 191, 155, 142, 137, 245, 235, 163, 242, 178, 152 };
*
*
*/

public class
CVSScramble extends Object
	{
	static public final String		RCS_ID = "$Id: CVSScramble.java,v 2.2 2003/07/27 01:08:32 time Exp $";
	static public final String		RCS_REV = "$Revision: 2.2 $";

	static private int[]	shifts;

	static
		{
		int i;
		int[] sh = new int[256];
		
		for ( i = 0 ; i < 32 ; ++i )
			{
			sh[i] = i;
			}

		sh[i++] = 114; sh[i++] = 120; sh[i++] = 53; sh[i++] = 79;
		sh[i++] = 96; sh[i++] = 109; sh[i++] = 72; sh[i++] = 108;
		sh[i++] = 70; sh[i++] = 64; sh[i++] = 76; sh[i++] = 67;
		sh[i++] = 116; sh[i++] = 74; sh[i++] = 68; sh[i++] = 87;
		sh[i++] = 111; sh[i++] = 52; sh[i++] = 75; sh[i++] = 119;
		sh[i++] = 49; sh[i++] = 34; sh[i++] = 82; sh[i++] = 81;
		sh[i++] = 95; sh[i++] = 65; sh[i++] = 112; sh[i++] = 86;
		sh[i++] = 118; sh[i++] = 110; sh[i++] = 122; sh[i++] = 105;
		sh[i++] = 41; sh[i++] = 57; sh[i++] = 83; sh[i++] = 43;
		sh[i++] = 46; sh[i++] = 102; sh[i++] = 40; sh[i++] = 89;
		sh[i++] = 38; sh[i++] = 103; sh[i++] = 45; sh[i++] = 50;
		sh[i++] = 42; sh[i++] = 123; sh[i++] = 91; sh[i++] = 35;
		sh[i++] = 125; sh[i++] = 55; sh[i++] = 54; sh[i++] = 66;
		sh[i++] = 124; sh[i++] = 126; sh[i++] = 59; sh[i++] = 47;
		sh[i++] = 92; sh[i++] = 71; sh[i++] = 115; sh[i++] = 78;
		sh[i++] = 88; sh[i++] = 107; sh[i++] = 106; sh[i++] = 56;
		sh[i++] = 36; sh[i++] = 121; sh[i++] = 117; sh[i++] = 104;
		sh[i++] = 101; sh[i++] = 100; sh[i++] = 69; sh[i++] = 73;
		sh[i++] = 99; sh[i++] = 63; sh[i++] = 94; sh[i++] = 93;
		sh[i++] = 39; sh[i++] = 37; sh[i++] = 61; sh[i++] = 48;
		sh[i++] = 58; sh[i++] = 113;
		sh[i++] = 32; sh[i++] = 90; sh[i++] = 44; sh[i++] = 98;
		sh[i++] = 60; sh[i++] = 51; sh[i++] = 33; sh[i++] = 97;
		sh[i++] = 62; sh[i++] = 77; sh[i++] = 84; sh[i++] = 80;
		sh[i++] = 85; sh[i++] = 223; sh[i++] = 225; sh[i++] = 216;
		sh[i++] = 187; sh[i++] = 166; sh[i++] = 229; sh[i++] = 189;
		sh[i++] = 222; sh[i++] = 188; sh[i++] = 141; sh[i++] = 249;
		sh[i++] = 148; sh[i++] = 200; sh[i++] = 184; sh[i++] = 136;
		sh[i++] = 248; sh[i++] = 190; sh[i++] = 199; sh[i++] = 170;
		sh[i++] = 181; sh[i++] = 204; sh[i++] = 138; sh[i++] = 232;
		sh[i++] = 218; sh[i++] = 183;
		sh[i++] = 255; sh[i++] = 234; sh[i++] = 220; sh[i++] = 247;
		sh[i++] = 213; sh[i++] = 203;
		sh[i++] = 226; sh[i++] = 193; sh[i++] = 174; sh[i++] = 172;
		sh[i++] = 228; sh[i++] = 252; sh[i++] = 217; sh[i++] = 201;
		sh[i++] = 131; sh[i++] = 230; sh[i++] = 197; sh[i++] = 211;
		sh[i++] = 145; sh[i++] = 238;
		sh[i++] = 161; sh[i++] = 179; sh[i++] = 160; sh[i++] = 212;
		sh[i++] = 207; sh[i++] = 221; sh[i++] = 254; sh[i++] = 173;
		sh[i++] = 202; sh[i++] = 146; sh[i++] = 224; sh[i++] = 151;
		sh[i++] = 140; sh[i++] = 196;
		sh[i++] = 205; sh[i++] = 130; sh[i++] = 135; sh[i++] = 133;
		sh[i++] = 143; sh[i++] = 246; sh[i++] = 192; sh[i++] = 159;
		sh[i++] = 244; sh[i++] = 239; sh[i++] = 185; sh[i++] = 168;
		sh[i++] = 215; sh[i++] = 144;
		sh[i++] = 139; sh[i++] = 165; sh[i++] = 180; sh[i++] = 157;
		sh[i++] = 147; sh[i++] = 186; sh[i++] = 214; sh[i++] = 176;
		sh[i++] = 227; sh[i++] = 231; sh[i++] = 219; sh[i++] = 169;
		sh[i++] = 175; sh[i++] = 156;
		sh[i++] = 206; sh[i++] = 198; sh[i++] = 129; sh[i++] = 164;
		sh[i++] = 150; sh[i++] = 210; sh[i++] = 154; sh[i++] = 177;
		sh[i++] = 134; sh[i++] = 127; sh[i++] = 182; sh[i++] = 128;
		sh[i++] = 158; sh[i++] = 208;
		sh[i++] = 162; sh[i++] = 132; sh[i++] = 167; sh[i++] = 209;
		sh[i++] = 149; sh[i++] = 241; sh[i++] = 153; sh[i++] = 251;
		sh[i++] = 237; sh[i++] = 236; sh[i++] = 171; sh[i++] = 195;
		sh[i++] = 243; sh[i++] = 233;
		sh[i++] = 253; sh[i++] = 240; sh[i++] = 194; sh[i++] = 250;
		sh[i++] = 191; sh[i++] = 155; sh[i++] = 142; sh[i++] = 137;
		sh[i++] = 245; sh[i++] = 235; sh[i++] = 163; sh[i++] = 242;
		sh[i++] = 178; sh[i++] = 152;

		CVSScramble.shifts = sh;
		}

	public static String
	scramblePassword( String password, char selector )
		{
		if ( selector == 'A' )
			{
			StringBuffer buf = new StringBuffer( "A" );

			for ( int i = 0 ; i < password.length() ; ++i )
				{
				char ch = password.charAt(i);

				byte newCh = (byte)
					(CVSScramble.shifts[ ( (int)ch & 255 ) ] & 255);

				buf.append( (char)newCh );
				}

			return buf.toString();
			}
		else
			{
			return null;
			}
		}

	public static String
	unScramblePassword( String scramble )
		{
		char	selector = scramble.charAt(0);

		if ( selector == 'A' )
			{
			// This method is symmetrical.
			String pass =
				CVSScramble.scramblePassword
					( scramble.substring( 1 ), 'A' );

			return pass.substring( 1 ); // Drop the 'A' spec...
			}
		else
			{
			return null;
			}
		}

	}
