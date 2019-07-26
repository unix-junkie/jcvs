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
																			
import java.io.*;
import java.lang.*;
import java.text.*;
import java.util.*;

/**
 * Encapsulates a single CVS server response item.
 * A response item has an ID indicating what the response was.
 * Some responses include parameters, and in some cases, files,
 * and the CVSReponseItem encapsulates these 'additional' data.
 *
 * @version $Revision: 2.4 $
 * @author Timothy Gerard Endres, <a href="mailto:time@ice.com">time@ice.com</a>.
 * @see CVSClient
 * @see CVSRequest
 */

public class
CVSResponseItem extends Object
	{
	static public final String	RCS_ID = "$Id: CVSResponseItem.java,v 2.4 1998/07/05 22:48:25 time Exp $";
	static public final String	RCS_REV = "$Revision: 2.4 $";

	static public final int		CHECKED_IN			= 1;
	static public final int		CHECKSUM			= 2;
	static public final int		CLEAR_STATIC_DIR	= 3;
	static public final int		CLEAR_STICKY		= 4;
	static public final int		COPY_FILE			= 5;
	static public final int		CREATED				= 6;
	static public final int		MERGED				= 7;
	static public final int		MODULE_EXPANSION	= 8;
	static public final int		NEW_ENTRY			= 9;
	static public final int		NOTIFIED			= 10;
	static public final int		PATCHED				= 11;
	static public final int		REMOVED				= 12;
	static public final int		REMOVE_ENTRY		= 13;
	static public final int		UPDATED				= 14;
	static public final int		UPDATE_EXISTING		= 15;
	static public final int		VALID_REQUESTS		= 16;
	static public final int		SET_CHECKIN_PROG	= 17;
	static public final int		SET_STATIC_DIR		= 18;
	static public final int		SET_STICKY			= 19;
	static public final int		SET_UPDATE_PROG		= 20;

	// These are temporaries use in the
	static public final int		GET_FULL_PATH		= 1; 
	static public final int		GET_ENTRIES_LINE	= 2;
	static public final int		GET_MODE_LINE		= 3;
	static public final int		GET_NEW_NAME		= 4;
	static public final int		GET_TAG_SPEC		= 5;
	static public final int		GET_PROGRAM			= 6;
	static public final int		GET_FILE			= 7;


	private boolean			valid;

	private int				type;
	private int				addState;
	private boolean			isGZIPed;

	private File			file;
	private String			text;
	private String			pathName;
	private String			reposName;
	private String			modeLine;
	private String			entriesLine;

	private String			newName;
	private String			tagSpec;
	private String			useProgram;


	public CVSResponseItem( int type )
		{
		super();

		this.type = type;
		this.valid = false;
		this.isGZIPed = false;

		this.file = null;
		this.text = null;
		this.pathName = null;
		this.reposName = null;
		this.modeLine = null;
		this.entriesLine = null;

		this.newName = null;
		this.tagSpec = null;
		this.useProgram = null;
		}

	public int
	getType()
		{
		return type;
		}

	public int
	getAddState()
		{
		return this.addState;
		}

	public void
	setAddState( int state )
		{
		this.addState = state;
		}

	public boolean
	isGZIPed()
		{
		return this.isGZIPed;
		}

	public void
	setGZIPed( boolean isGZIPed )
		{
		this.isGZIPed = isGZIPed;
		}

	public boolean
	isValid()
		{
		return this.valid;
		}

	public void
	setValid( boolean valid )
		{
		this.valid = valid;
		}

	public String
	getEntriesLine()
		{
		return this.entriesLine;
		}

	public void
	setEntriesLine( String line )
		{
		this.entriesLine = line;
		}

	public String
	getModeLine()
		{
		return this.modeLine;
		}

	public void
	setModeLine( String line )
		{
		this.modeLine = line;
		}

	public File
	getFile()
		{
		return this.file;
		}

	public void
	setFile( File file )
		{
		this.file = file;
		}

	public boolean
	deleteFile()
		{
		boolean result = true;

		if ( this.file != null )
			{
			if ( this.file.exists() )
				{
				try {
					this.file.delete();
					}
				catch ( SecurityException ex )
					{
					result = false;
					CVSLog.logMsg
						( "ERROR deleting temp file '"
							+ this.file.getPath() + "'" );
					}
				}
			}

		return result;
		}

	public String
	getPathName()
		{
		return this.pathName;
		}

	public void
	setPathName( String pathName )
		{
		this.pathName = pathName;
		}

	public String
	getRepositoryPath()
		{
		int index = this.reposName.lastIndexOf( '/' );

		if ( index < 0 )
			return ".";	// REVIEW
		else
			return this.reposName.substring( 0, index );
		}

	public String
	getRepositoryName()
		{
		return this.reposName;
		}

	public void
	setRepositoryName( String reposName )
		{
		this.reposName = reposName;
		}

	public String
	getNewName()
		{
		return this.newName;
		}

	public void
	setNewName( String name )
		{
		this.newName = name;
		}

	public String
	getProgram()
		{
		return this.newName;
		}

	public void
	setProgram( String program )
		{
		this.useProgram = program;
		}

	public String
	getTagSpec()
		{
		return this.tagSpec;
		}

	public void
	setTagSpec( String tagspec )
		{
		this.tagSpec = tagspec;
		}

	public String
	getChecksum()
		{
		if ( this.type != CVSResponseItem.CHECKSUM )
			return null;
		else
			return this.text;
		}

	public void
	setChecksum( String sumStr )
		{
		if ( this.type == CVSResponseItem.CHECKSUM )
			{
			this.text = sumStr;
			}
		}

	public String
	getValidRequests()
		{
		if ( this.type != CVSResponseItem.VALID_REQUESTS )
			return null;
		else
			return this.text;
		}

	public void
	setValidRequests( String requests )
		{
		if ( this.type == CVSResponseItem.VALID_REQUESTS )
			{
			this.text = requests;
			}
		}

	public String
	toString()
		{
		return "[ "
			+ "type=" + this.type + ","
			+ "pathName=" + this.pathName + ","
			+ "reposName=" + this.reposName + ","
			+ "modeLine=" + this.modeLine + ","
			+ "entriesLine=" + this.entriesLine + ","
			+ "newName=" + this.newName + ","
			+ "tagSpec=" + this.tagSpec + ","
			+ "useProgram=" + this.useProgram
			+ " ]";
		}
	}
	   
