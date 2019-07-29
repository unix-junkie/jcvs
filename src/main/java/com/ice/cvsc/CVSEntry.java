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

import java.io.File;
import java.text.ParseException;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * CVSEntry implements the concept of a CVS Entry. Traditionally,
 * a CVS Entry is a line in an 'Entries' file in a 'CVS' admin
 * directory. A CVSEntry represents a CVS file that is checked
 * in or being checked in.
 *
 * CVSEntry objects contain all of the relavent information about
 * a CVS file, such as its name, check-out time, modification status,
 * local pathname, repository, etc.
 *
 * @version $Revision: 2.11 $
 * @author Timothy Gerard Endres, <a href="mailto:time@ice.com">time@ice.com</a>.
 * @see CVSClient
 * @see CVSProject
 * @see CVSEntryVector
 */

public
class		CVSEntry
extends		Object
implements	Cloneable
	{
	static public final String		RCS_ID = "$Id: CVSEntry.java,v 2.11 2003/07/27 01:08:32 time Exp $";
	static public final String		RCS_REV = "$Revision: 2.11 $";

	/**
	 * True if this entry is valid.
	 */
	private boolean			valid;

	/**
	 * True if this entry is a directory entry.
	 */
	private boolean			isDir;

	/**
	 * If this entry is a directory entry, then this is
	 * the entry vector for the entries in that directory.
	 */
	private CVSEntryVector	entryList;

	/**
	 * The full path of the repository as it comes from the
	 * 'Repository' file in the 'CVS' administration directory.
	 */
	private String			repository;

	/**
	 * This is the entry's 'local directory'. This is the
	 * local directory that is sent as the 'pathname' of many
	 * responses, and is sent with 'Directory' requests.
	 */
	private String			localDirectory;

	private boolean			isNoUserFile;
	private boolean			isNewUserFile;
	private boolean			isToBeRemoved;
	private boolean			isDirty;

	/**
	 * If this is set, then when the getServerEntryLine() is
	 * called, we will ignore the file's modification status
	 * and mark the entry as "/modified/". This was added to
	 * support jcvsweb, but is currently unused.
	 */
	private boolean			forceModified;

	/**
	 * If this is set, then when the getServerEntryLine() is
	 * called, we will ignore the "exists" flag and pretend
	 * that the file does NOT exist. This was added to support
	 * jcvsweb's need for updates of existing files for viewing
	 * purposes.
	 */
	private boolean			forceNoExistence;

	private CVSMode			mode;

	private CVSTimestamp	tsCache;
	private CVSTimestamp	cfCache;

	private String			name;
	private String			version;
	private String			timestamp;
	private String			conflict;
	private String			options;
	private String			tag;
	private String			date;

	private final Vector			childListeners;


	public CVSEntry()
		{
		super();

		this.valid = false;
		this.isDir = false;

		this.repository = null;
		this.localDirectory = null;
		this.entryList = null;

		this.isNoUserFile = false;
		this.isNewUserFile = false;
		this.isToBeRemoved = false;
		this.isDirty = false;
		this.forceModified = false;
		this.forceNoExistence = false;

		this.mode = null;
		this.tsCache = null;
		this.cfCache = null;

		this.name = "";
		this.version = "";
		this.timestamp = "";
		this.conflict = null;
		this.options = null;
		this.tag = null;
		this.date = null;

		this.childListeners = new Vector();
		}

	public boolean
	isValid()
		{
		return this.valid;
		}

	public void
	setValid( final boolean valid )
		{
		this.valid = valid;
		}

	public boolean
	isDirty()
		{
		return this.isDirty;
		}

	public void
	setDirty( final boolean dirty )
		{
		this.isDirty = dirty;
		}

	public boolean
	isForceModified()
		{
		return this.forceModified;
		}

	public void
	setForceModified( final boolean forceModified )
		{
		this.forceModified = forceModified;
		}

	public boolean
	isForceNoExistence()
		{
		return this.forceNoExistence;
		}

	public void
	setForceNoExistence( final boolean forceNoExistence )
		{
		this.forceNoExistence = forceNoExistence;
		}

	public String
	getName()
		{
		return this.name;
		}

	public void
	setName( final String name )
		{
		this.name = name;
		}

	public String
	getRepository()
		{
		return this.repository;
		}

	public void
	setRepository( final String repository )
		{
		this.repository =
			CVSCUtilities.stripFinalSlash( repository );
		}

	// 'LocalDirectory' here is in the sense of the
	// local-directory returned with cvs server responses!
	public String
	getLocalDirectory()
		{
		return this.localDirectory;
		}

	public void
	setLocalDirectory( final String directory )
		{
		this.localDirectory =
			CVSCUtilities.ensureFinalSlash( directory );
		}

	public String
	getFullName()
		{
		if ( this.isDirectory() )
			return this.getLocalDirectory();
		else
			return this.getLocalDirectory() + this.getName();
		}

	private String
	stripDotSlashPrefix( String path )
		{
		if ( path.startsWith( "./" ) )
			path = path.substring( 2 );
		return path;
		}

	/**
	 * This method was added when we finally decided to bite the
	 * bullet and change the naming scheme to work "correctly".
	 * This will return the same string as getFullName(), except
	 * that the "./" prefix is removed. This is preferable for
	 * building file path names, hence the name.
	 */
	public String
	getFullPathName()
		{
		return this.stripDotSlashPrefix( this.getFullName() );
		}

	/**
	 * This method was added when we finally decided to bite the
	 * bullet and change the naming scheme to work "correctly".
	 * This will return the same string as getLocalDirectory(),
	 * except that the "./" prefix is removed. This is preferable
	 * for building file path names, hence the name.
	 */
	public String
	getLocalPathName()
		{
		return this.stripDotSlashPrefix( this.getLocalDirectory() );
		}

	public String
	getRepositoryName()
		{
		return this.getRepository() + this.getName();
		}

	public String
	getArgumentName()
		{
		return this.getFullName();
		}

	/**
	 * Provides the directory-ness of this entry.
	 *
	 * @return True if this entry is a directory, else false.
	 */
	public boolean
	isDirectory()
		{
		return this.isDir;
		}

	public void
	appendEntry( final CVSEntry entry )
		{
		this.entryList.appendEntry( entry );
		this.fireChildAddedEvent
			( this.new ChildEvent( this.entryList.size() - 1, entry ) );
		}

	public boolean
	removeEntry( final CVSEntry entry )
		{
		boolean result = false;

		final int index = this.entryList.indexOf( entry );
		if ( index != -1 )
			{
			result = true;
			this.isDirty = true;
			final CVSEntry child = this.entryList.entryAt( index );
			this.entryList.removeElementAt( index );
			this.fireChildRemovedEvent
				( this.new ChildEvent( index, child ) );
			}

		return result;
		}

	public boolean
	removeEntry( final String entryName )
		{
		boolean result = false;

		for ( int i = 0, sz = this.entryList.size() ; i < sz ; ++i )
			{
			final CVSEntry entry = this.entryList.entryAt(i);
			if ( entryName.equals( entry.getName() ) )
				{
				result = true;
				this.isDirty = true;
				this.entryList.removeElementAt( i );
				this.fireChildRemovedEvent
					( this.new ChildEvent( i, entry ) );
				break;
				}
			}

		return result;
		}

	public void
	removeAllEntries()
		{
		if ( this.isDirectory() )
			{
			if ( this.entryList != null )
				{
				this.entryList.removeAllEntries();
				this.fireChildRemovedEvent
					( this.new ChildEvent( -1, null ) );
				}
			}
		}

	public CVSEntry
	locateEntry( final String name )
		{
		return this.entryList.locateEntry( name );
		}

	public CVSEntryVector
	getEntryList()
		{
		return this.entryList;
		}

	/**
	 * This method will make this entry a directory entry
	 * and establish its entry list with the list passed
	 * in the parameter. This is the <strong>only</strong> means of making
	 * a CVSEntry become a <em>directory entry</em>.
	 *
	 * @param entryList The directory's entry list.
	 */
	public void
	setDirectoryEntryList( final CVSEntryVector entryList )
		{
		if ( entryList != null )
			{
			this.isDir = true;
			this.entryList = entryList;
			}
		}

	public String
	getVersion()
		{
		return this.version;
		}

	public void
	setVersion( final String version )
		{
		this.isNoUserFile = false;
		this.isNewUserFile = false;
		this.isToBeRemoved = false;

		if ( version == null
				|| version.length() == 0 )
			{
			this.isNoUserFile = true;
			this.version = "";
			}
		else if ( version.startsWith( "-" ) )
			{
			this.isToBeRemoved = true;
			this.version = version.substring( 1 );
			}
		else if ( version.startsWith( "0" ) )  // that's a zero
			{
			this.isNewUserFile = true;
			this.version = version.substring( 1 );
			}
		else
			{
			this.version = version;
			}
		}

	public void
	markForRemoval( final boolean markState )
		{
		this.isToBeRemoved = markState;
		}

	private CVSTimestamp
	parseTimestamp( final String stampStr )
		{
		CVSTimestamp result =
			new CVSTimestamp(0);

		if ( stampStr != null )
			{
			final CVSTimestampFormat stamper =
				CVSTimestampFormat.getInstance();

			try {
				result = stamper.parse( stampStr );
				}
			catch ( final ParseException ex )
				{
				result = new CVSTimestamp(0);
				CVSTracer.traceWithStack(
					"CVSEntry.parseTimestamp: "
						+ "could not parse timestamp: '"
						+ stampStr + "' - " + ex.getMessage() );
				}
			}

		return result;
		}

	/**
	 * The cached CVSTimestamp (a subclass of Date), or null.
	 */
	public CVSTimestamp
	getCVSTime()
		{
		return this.tsCache;
		}

	public String
	getTimestamp()
		{
		return this.timestamp;
		}

	public String
	completeTimestamp()
		{
		return
			this.timestamp
			+ ( this.conflict == null
				? ""
				: "+" + this.conflict
				);
		}

	public String
	getTerseTimestamp()
		{
		if ( this.tsCache == null )
			{
			this.tsCache =
				this.parseTimestamp( this.timestamp );
			}

		if ( this.tsCache == null )
			{
			return this.timestamp; // punt!
			}
		else
			{
			final CVSTimestampFormat	stamper =
				CVSTimestampFormat.getInstance();

			return stamper.formatTerse( this.tsCache );
			}
		}

	/**
	 * Set the timestamp of this entry to that of the modification
	 * time of the file passed to this method.
	 *
	 * <b>NOTE</b> There is an issue with timestamps between Java
	 * and CVS. Specifically, Java time uses millisecond resolution
	 * and CVS time uses second resolution. The problem arises when
	 * a file is "sync-ed" with the CVS/Entries timestamp and the
	 * file's modtime is stored with non-zero milliseconds. When we
	 * later compare the file's modtime to that of the CVSEntry's
	 * timestamp, they will differ by the milliseconds quantity.
	 * To solve this problem, we strip milliseconds from any file
	 * timestamp coming into jCVS. This forces all of the timestamps
	 * to have zero millisecond digits, which will compare properly
	 * with the CVS timestamps.
	 */

	public void
	setTimestamp( final File entryFile )
		{
		// FIRST strip the millisecond digits and make them zero!
		long mTime = entryFile.lastModified();
		mTime = mTime / 1000 * 1000;

		final CVSTimestamp stamp = new CVSTimestamp( mTime );

		final CVSTimestampFormat	stamper =
			CVSTimestampFormat.getInstance();

		final String stampStr = stamper.format( stamp );

		this.setTimestamp( stampStr );
		}

	public void
	setTimestamp( String timeStamp )
		{
		// REVIEW
		if ( timeStamp == null )
			{
			CVSTracer.traceWithStack( "NULL TIMESTAMP!!!" );
			timeStamp = "";
			}

		String tstamp = new String( timeStamp );

		this.cfCache = null;
		this.conflict = null;

		if ( tstamp.length() < 1 )
			{
			this.timestamp = "";
			this.tsCache = null;
			}
		else if ( tstamp.startsWith( "+" ) )
			{
			// REVIEW - leave the timestamp in place...
			// We have received a "+conflict" format, which
			// typically only comes from the server.
			this.conflict = tstamp.substring( 1 );
			if ( this.conflict.equals( "=" ) )
				{
				// In this case, the server is indicating that the
				// file is "going to be equal" once the 'Merged' handling
				// is completed. To retain the "inConflict" nature of
				// the entry, we will simply set the conflict to an
				// empty string (not null), as the conflict will be
				// set very shortly as a result of the 'Merged' handling.
				//
				this.conflict = "";
				}
			}
		else
			{
			final int index = tstamp.indexOf( '+' );
			if ( index < 0 )
				{
				// Only the timestamp is provided (no '+').
				if ( tstamp.startsWith( "Initial " ) )
					{
					// This file was "added" but not committed,
					// timestamp is irrelevant
					this.timestamp = "";
					this.tsCache = null;
					}
				else if ( tstamp.equals( "Result of merge" ) )
					{
					// This file was "merged" timestamp must show modified
					this.timestamp = "";
					this.tsCache = null;
					}
				else if ( ! tstamp.equals( this.timestamp ) )
					{
					// Only update these if it is different
					this.tsCache = null; // signal need to parse!
					this.timestamp = tstamp;
					}
				}
			else
				{
				// The "timestamp+conflict" case.
				// This should <em>only</em> comes from an Entries
				// file, and should never come from the server.
				this.conflict = tstamp.substring( index + 1 );
				tstamp = tstamp.substring( 0, index );
				if ( ! tstamp.equals( this.timestamp ) )
					{
					// Only update these if it is different
					this.tsCache = null; // signal need to parse!
					//
					// REVIEW
					// UNDONE
					// This next check really should be more "generic"
					// in the sense of "if ( ! validTimestamp( tstamp ) )".
					//
					if ( tstamp.equals( "Result of merge" ) )
						{
						// REVIEW should we always set to conflict?
						// If timestamp is empty, use the conflict...
						if ( ( this.timestamp == null ||
								this.timestamp.length() == 0 )
									&& this.conflict.length() > 0 )
							{
							this.timestamp = this.conflict;
							}
						}
					else
						{
						this.timestamp = tstamp;
						}
					}
				}
			}

		final CVSTimestampFormat	stamper =
			CVSTimestampFormat.getInstance();

		// If tsCache is set to null, we need to update it...
		if ( this.tsCache == null
				&& this.timestamp.length() > 0 )
			{
			try {
				this.tsCache = stamper.parse( this.timestamp );
				}
			catch ( final ParseException ex )
				{
				this.tsCache = null;
				if ( false ) // in normal operations, this is ok
					CVSTracer.traceWithStack(
						"could not parse entries timestamp (cache): '"
						+ this.timestamp + "' - " + ex.getMessage() );
				}
		   }

		// If conflict is not null, we need to update cfCache...
		if ( this.conflict != null
				&& this.conflict.length() > 0 )
			{
			try {
				this.cfCache = stamper.parse( this.conflict );
				}
			catch ( final ParseException ex )
				{
				this.cfCache = null;
				if ( false ) // in normal operations, this is ok
					CVSTracer.traceWithStack(
						"could not parse entries conflict (cache): '"
						+ this.conflict + "' - " + ex.getMessage() );
				}
			}
if ( false )
CVSTracer.traceIf( true,
	"CVSEntry.setTimestamp: '"
	+ this.getName() + "' - '" + timeStamp
	+ "'\n   timestamp '" + this.timestamp + "' tsCache '"
	+ ( this.tsCache == null ? "(not set)" : "(set)" )
	+ "'\n   conflict  '"
	+ ( this.conflict == null ? "(null)" : this.conflict )
	+ "' cfCache '"
	+ ( this.cfCache == null ? "(not set)" : "(set)" )
	+ "'" );
		}

	/**
	 * <b>NOTE</b>Refer to note under setTimestamp( File ) pertaining
	 * to the resolution of file times and CVS timestamps.
	 */
	public void
	setConflict( final File entryFile )
		{
		// FIRST strip the millisecond digits and make them zero!
		long mTime = entryFile.lastModified();
		mTime = mTime / 1000 * 1000;

		this.cfCache = new CVSTimestamp( mTime );

		final CVSTimestamp stamp =
			new CVSTimestamp( this.cfCache.getTime() );

		final CVSTimestampFormat	stamper =
			CVSTimestampFormat.getInstance();

		final String stampStr = stamper.format( stamp );

		this.conflict = stampStr;
		}

	public String
	getOptions()
		{
		return this.options;
		}

	public void
	setOptions( final String options )
		{
		this.options = options;
		}

	public boolean
	isBinary()
		{
		return this.options.indexOf( "-kb" ) != -1;
		}

	public String
	getTag()
		{
		return this.tag;
		}

	public void
	setTag( final String tag )
		{
		this.tag = tag;
		this.date = null;
		}

	public String
	getDate()
		{
		return this.date;
		}

	public void
	setDate( final String date )
		{
		this.tag = null;
		this.date = date;
		}

	public CVSMode
	getMode()
		{
		return this.mode;
		}

	public void
	setMode( final CVSMode mode )
		{
		this.mode = mode;
		}

	public String
	getModeLine()
		{
		return
			this.mode == null
			? "u=rw,g=r,o=r" // UNDONE - better idea?
			: this.mode.getModeLine();
		}

	public boolean
	isNoUserFile()
		{
		return this.isNoUserFile;
		}

	public void
	setNoUserFile( final boolean isNo )
		{
		this.isNoUserFile = isNo;
		}

	public boolean
	isInConflict()
		{
		return this.conflict != null;
		}

	private String
	getConflict()
		{
		return this.conflict;
		}

	public boolean
	isNewUserFile()
		{
		return this.isNewUserFile;
		}

	public void
	setNewUserFile( final boolean isNew )
		{
		this.isNewUserFile = isNew;
		}

	public boolean
	isToBeRemoved()
		{
		return this.isToBeRemoved;
		}

	public void
	setToBeRemoved( final boolean toBe )
		{
		this.isToBeRemoved = toBe;
		}

	public boolean
	isLocalFileModified( final File localFile )
		{
		if ( this.forceModified )
			{
System.err.println( "CVSENTRY: force MOD? " + this.forceModified );
			return true;
			}

		// REVIEW is this the best return value for this case?
		if ( this.tsCache == null )
			return true;

		return ! this.tsCache.equalsTime( localFile.lastModified() );
		}


	// UNDONE - all of the "ParseException( , offset's" are zero!

	private String
	parseAToken( final StringTokenizer toker )
		{
		String token = null;

		try { token = toker.nextToken(); }
		catch ( final NoSuchElementException ex )
			{
			token = null;
			}

		return token;
		}

	public boolean
	parseEntryLine( final String parseLine, final boolean fromServer )
		throws ParseException
		{
		String token = null;
		String nameToke = null;
		String versionToke = null;
		String conflictToke = null;
		String optionsToke = null;
		String tagToke = null;

		this.valid = false;

		String entryLine = parseLine;

		// Strip the 'D' from 'Directory' entries
		if ( entryLine.startsWith( "D/" ) )
			{
			this.isDir = true;
			entryLine = entryLine.substring( 1 );
			}

		final StringTokenizer toker =
			new StringTokenizer( entryLine, "/", true );

		final int tokeCount = toker.countTokens();

		if ( tokeCount < 6 )
			{
			throw new ParseException
				( "not enough tokens in entries line "
					+ "(min 6, parsed " +tokeCount+ ")", 0 );
			}

		token = this.parseAToken( toker );
		if ( token == null || !token.equals( "/" ) )
			throw new ParseException
				( "could not parse name's starting slash", 0 );

		nameToke = this.parseAToken( toker );
		if ( nameToke == null )
			{
			throw new ParseException
				( "could not parse entry name", 0 );
			}
		else if ( nameToke.equals( "/" ) )
			{
			throw new ParseException
				( "entry has an empty name", 0 );
			}
		else
			{
			token = this.parseAToken( toker );
			if ( token == null || !token.equals( "/" ) )
				throw new ParseException
					( "could not parse version's starting slash", 0 );
			}

		versionToke = this.parseAToken( toker );
		if ( versionToke == null )
			{
			throw new ParseException
				( "out of tokens getting version field", 0 );
			}
		else if ( versionToke.equals( "/" ) )
			{
			versionToke = "";
			}
		else
			{
			token = this.parseAToken( toker );
			if ( token == null || !token.equals( "/" ) )
				throw new ParseException
					( "could not parse conflict's starting slash", 0 );
			}

		conflictToke = this.parseAToken( toker );
		if ( conflictToke == null )
			{
			throw new ParseException
				( "out of tokens getting conflict field", 0 );
			}
		else if ( conflictToke.equals( "/" ) )
			{
			conflictToke = "";
			}
		else
			{
			token = this.parseAToken( toker );
			if ( token == null || !token.equals( "/" ) )
				throw new ParseException
					( "could not parse options' starting slash", 0 );
			}

		optionsToke = this.parseAToken( toker );
		if ( optionsToke == null )
			{
			throw new ParseException
				( "out of tokens getting options field", 0 );
			}
		else if ( optionsToke.equals( "/" ) )
			{
			optionsToke = "";
			}
		else
			{
			token = this.parseAToken( toker );
			if ( token == null || !token.equals( "/" ) )
				throw new ParseException
					( "could not parse tag's starting slash", 0 );
			}

		tagToke = this.parseAToken( toker );
		if ( tagToke == null || tagToke.equals( "/" ) )
			{
			tagToke = "";
			}

		this.valid = true;

		if ( fromServer && conflictToke.length() > 0
				&& ! conflictToke.startsWith( "+" ) )
			{
			// We silently ignore conflicts that don't start with '+'
			// when they come from the server.
			conflictToke = "";
			}

		this.setName( nameToke );
		this.setVersion( versionToke );
		this.setTimestamp( conflictToke );
		this.setOptions( optionsToke );

		if ( tagToke == null || tagToke.length() < 1 )
			{
			this.setTag( null );
			}
		else
			{
			if ( tagToke.startsWith( "D" ) )
				{
				this.setDate( tagToke.substring( 1 ) );
				}
			else
				{
				this.setTag( tagToke.substring( 1 ) );
				}
			}

		return this.valid;
		}

	public String
	padString( final String str, final int width )
		{
		int		i;
		final StringBuffer result =
			new StringBuffer( width );

		result.append( str );
		for ( i = result.length()-1 ; i < width ; ++i )
			result.append( " " );

		return result.toString();
		}

	public String
	getAdminEntryLine()
		{
		if ( this.isDirectory() )
			{
			// REVIEW should we be carrying along options & tags?!
			return "D/" + this.name + "////";
			}

		final StringBuffer result = new StringBuffer("");

		result.append( "/" + this.name + "/" );

		if ( ! this.isNoUserFile() )
			{
			if ( this.isNewUserFile() )
				{
				result.append( "0" ); // that's a zero
				}
			else
				{
				if ( this.isToBeRemoved() )
					result.append( "-" );

				if ( this.version != null )
					result.append( this.version );
				}
			}

		result.append( "/" );

		if ( this.isNewUserFile() )
			{
			result.append( "Initial " + this.getName() );
			}
		else
			{
			result.append( this.timestamp );

			if ( this.isInConflict() )
				{
				result.append( "+" + this.conflict );
				}
			}

		result.append( "/" );

		if ( this.options != null )
			result.append( this.options );

		result.append( "/" );

		if ( this.tag != null )
			{
			result.append( "T" + this.tag );
			}
		else if ( this.date != null )
			{
			result.append( "D" + this.date );
			}

		return result.toString();
		}

	public String
	getServerEntryLine( final boolean exists, final boolean isModified )
		{
		if ( this.isDirectory() )
			{
			// REVIEW should we be carrying along options & tags?!
			return "/" + this.name + "////";
			}

		final StringBuffer result = new StringBuffer("");

		result.append( "/" + this.name + "/" );

		if ( ! this.isNoUserFile() )
			{
			if ( this.isNewUserFile() )
				{
				result.append( "0" ); // that's a zero
				}
			else
				{
				if ( this.isToBeRemoved() )
					result.append( "-" );

				if ( this.version != null && ! this.forceNoExistence )
					result.append( this.version );
				}
			}

		result.append( "/" );

		if ( this.isNewUserFile() )
			{
			result.append( "Initial " + this.getName() );
			}
		else if ( exists && ! this.forceNoExistence )
			{
			if ( this.isInConflict() )
				{
				result.append( "+" );
				}

			if ( isModified || forceModified )
				result.append( "modified" );
			else
				result.append( "=" );
			}

		result.append( "/" );

		if ( this.options != null )
			result.append( this.options );

		result.append( "/" );

		if ( this.tag != null && ! this.forceNoExistence )
			result.append( "T" + this.tag );
		else if ( this.date != null && ! this.forceNoExistence  )
			result.append( "D" + this.date );

		 CVSTracer.traceIf( false,
			"getServerEntryLine: '" + result.toString() + "'" );

		return result.toString();
		}

	public String
	toString()
		{
		return
			"[ " +
			this.getFullName() + "," +
			this.getAdminEntryLine() +
			" ]";
		}

	/**
	 * Adds all of the file entries in this directory entry to the vector supplied.
	 *
	 * @param vector The vector to add the file entries to.
	 */

	public void
	addFileEntries( final CVSEntryVector vector )
		{
		final CVSEntryVector entries = this.getEntryList();
		for ( int idx = 0 ; idx < entries.size() ; ++idx )
			{
			final CVSEntry entry = entries.entryAt( idx );
			if ( ! entry.isDirectory() )
				vector.appendEntry( entry );
			}
		}

	/**
	 * Adds all of the file entries in this directory entry, as well as every
	 * file entry in every subdirectory entry recursively.
	 *
	 * @param vector The vector to add the file entries to.
	 */

	public void
	addAllSubTreeEntries( final CVSEntryVector vector )
		{
		final CVSEntryVector dirs = new CVSEntryVector();
		final CVSEntryVector list = this.getEntryList();

		// First, append all of the files, caching directories...
		for ( int idx = 0 ; idx < list.size() ; ++idx )
			{
			final CVSEntry entry = list.entryAt( idx );
			if ( entry.isDirectory() )
				dirs.appendEntry( entry );
			else
				vector.appendEntry( entry );
			}

		// Now, process all of the cached directories...
		for ( int idx = 0 ; idx < dirs.size() ; ++idx )
			{
			final CVSEntry entry = dirs.entryAt( idx );
			entry.addAllSubTreeEntries( vector );
			}
		}

	public
	class		ChildEvent
		{
		CVSEntry	entry;
		CVSEntry	childEntry;
		int			childIndex;

		/**
		 * The source is the CVSEntry that parent's the child.
		 * The index is the index of the affected child.
		 */

		public
		ChildEvent( final int index, final CVSEntry child )
			{
			this.childIndex = index;
			this.childEntry = child;
			}

		public CVSEntry
		getCVSEntry()
			{
			return CVSEntry.this;
			}

		public int
		getChildIndex()
			{
			return this.childIndex;
			}

		public CVSEntry
		getChildEntry()
			{
			return this.childEntry;
			}

		}

	public
	interface	ChildEventListener
		{
		public void cvsEntryAddedChild( CVSEntry.ChildEvent event );
		public void cvsEntryRemovedChild( CVSEntry.ChildEvent event );
		}

	protected void
	fireChildAddedEvent( final ChildEvent event )
		{
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for ( int i = this.childListeners.size() - 1 ; i >= 0 ; --i )
			{
			((ChildEventListener) this.childListeners.elementAt(i)).
				cvsEntryAddedChild( event );
			}
		}

	protected void
	fireChildRemovedEvent( final ChildEvent event )
		{
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for ( int i = this.childListeners.size() - 1 ; i >= 0 ; --i )
			{
			((ChildEventListener) this.childListeners.elementAt(i)).
				cvsEntryRemovedChild( event );
			}
		}

	public void
	addChildEventListener( final ChildEventListener l )
		{
		this.childListeners.addElement( l );
		}

	public void
	removeChildEventListener( final ChildEventListener l )
		{
		this.childListeners.removeElement( l );
		}

	public String
	dumpString()
		{
		return this.dumpString( "" );
		}

	public String
	dumpString( final String prefix )
		{
		return
			prefix + "CVSEntry: " + super.toString() + "\n" +
			prefix + "   Name: " + this.getName() + "\n" +
			prefix + "   FullName: " + this.getFullName() + "\n" +
			prefix + "   LocalDir: " + this.getLocalDirectory() + "\n" +
			prefix + "   Repository: " + this.getRepository() + "\n" +
			prefix + "   Timestamp: " + this.getTimestamp() + "\n" +
			prefix + "   Conflict: " + this.getConflict();
		}

	}




