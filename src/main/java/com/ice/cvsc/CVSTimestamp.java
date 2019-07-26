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
import java.util.*;

/**
 * The CVSTimestamp class is a subclass of Date, specifically
 * designed to be used as the time stamp of CVS entries. This
 * class allows us to display the timestamps of CVS Entries,
 * as well as determine when files have been updated.
 *
 * @version $Revision: 2.5 $
 * @author Timothy Gerard Endres, <a href="mailto:time@ice.com">time@ice.com</a>.
 * @see CVSTimestampFormat
 */

public class
CVSTimestamp extends Date
		implements Cloneable
	{
	static public final String		RCS_ID = "$Id: CVSTimestamp.java,v 2.5 1999/07/27 03:17:55 time Exp $";
	static public final String		RCS_REV = "$Revision: 2.5 $";

	public
	CVSTimestamp()
		{
		super();
		}

	public
	CVSTimestamp( long msSinceEpoch )
		{
		super( msSinceEpoch );
		}

	public
	CVSTimestamp( Date date )
		{
		super( date.getTime() );
		}

	/**
	 * Determines if this timestamp is considered equivalent to
	 * the time represented by the parameter we are passed. Note
	 * that we allow up to, but not including, one second of time
	 * difference, since Java allows millisecond time resolution
	 * while CVS stores second resolution timestamps. Further, we
	 * allow the resolution difference on either side of the second
	 * because we can not be sure of the rounding.
	 *
	 */
	public boolean
	equalsTime( long time )
		{
		return 
			( ( this.getTime() > time )
				? ( (this.getTime() - time) < 1000 )
				: ( (time - this.getTime()) < 1000 ) );
		}

	/**
	 * Determines if this timestamp is considered equivalent to
	 * the time represented by another timestamp. Note
	 * that we allow up to, but not including, one second of time
	 * difference, since Java allows millisecond time resolution
	 * while CVS stores second resolution timestamps. Further, we
	 * allow the resolution difference on either side of the second
	 * because we can not be sure of the rounding.
	 */
	public boolean
	equalsTimestamp( CVSTimestamp stamp )
		{
		return 
			( ( this.getTime() > stamp.getTime() )
				? ( (this.getTime() - stamp.getTime()) < 1000 )
				: ( (stamp.getTime() - this.getTime()) < 1000 ) );
		}

	}

