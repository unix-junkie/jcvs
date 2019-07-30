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

/**
 * Encapsulates a single CVS server response item.
 * A response item has an ID indicating what the response was.
 * Some responses include parameters, and in some cases, files,
 * and the CVSReponseItem encapsulates these 'additional' data.
 *
 * @version $Revision: 2.5 $
 * @author Timothy Gerard Endres, <a href="mailto:time@ice.com">time@ice.com</a>.
 * @see CVSClient
 * @see CVSRequest
 */

public class
CVSResponseItem {
	public static final String	RCS_ID = "$Id: CVSResponseItem.java,v 2.5 2003/07/27 01:08:32 time Exp $";
	public static final String	RCS_REV = "$Revision: 2.5 $";

	public static final int		CHECKED_IN			= 1;
	public static final int		CHECKSUM			= 2;
	public static final int		CLEAR_STATIC_DIR	= 3;
	public static final int		CLEAR_STICKY		= 4;
	public static final int		COPY_FILE			= 5;
	public static final int		CREATED				= 6;
	public static final int		MERGED				= 7;
	public static final int		MODULE_EXPANSION	= 8;
	public static final int		NEW_ENTRY			= 9;
	public static final int		NOTIFIED			= 10;
	public static final int		PATCHED				= 11;
	public static final int		REMOVED				= 12;
	public static final int		REMOVE_ENTRY		= 13;
	public static final int		UPDATED				= 14;
	public static final int		UPDATE_EXISTING		= 15;
	public static final int		VALID_REQUESTS		= 16;
	public static final int		SET_CHECKIN_PROG	= 17;
	public static final int		SET_STATIC_DIR		= 18;
	public static final int		SET_STICKY			= 19;
	public static final int		SET_UPDATE_PROG		= 20;

	// These are temporaries use in the
	public static final int		GET_FULL_PATH		= 1;
	public static final int		GET_ENTRIES_LINE	= 2;
	public static final int		GET_MODE_LINE		= 3;
	public static final int		GET_NEW_NAME		= 4;
	public static final int		GET_TAG_SPEC		= 5;
	public static final int		GET_PROGRAM			= 6;
	public static final int		GET_FILE			= 7;


	private boolean			valid;

	private final int				type;
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


	public CVSResponseItem( final int type )
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
	setAddState( final int state )
		{
		this.addState = state;
		}

	public boolean
	isGZIPed()
		{
		return this.isGZIPed;
		}

	public void
	setGZIPed( final boolean isGZIPed )
		{
		this.isGZIPed = isGZIPed;
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

	public String
	getEntriesLine()
		{
		return this.entriesLine;
		}

	public void
	setEntriesLine( final String line )
		{
		this.entriesLine = line;
		}

	public String
	getModeLine()
		{
		return this.modeLine;
		}

	public void
	setModeLine( final String line )
		{
		this.modeLine = line;
		}

	public File
	getFile()
		{
		return this.file;
		}

	public void
	setFile( final File file )
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
				catch ( final SecurityException ex )
					{
					result = false;
					CVSLog.logMsg
						("ERROR deleting temp file '"
						 + this.file.getPath() + '\'');
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
	setPathName( final String pathName )
		{
		this.pathName = pathName;
		}

	public String
	getRepositoryPath()
		{
		final int index = this.reposName.lastIndexOf( '/' );

			// REVIEW
			return index < 0 ? "." : this.reposName.substring(0, index);
		}

	public String
	getRepositoryName()
		{
		return this.reposName;
		}

	public void
	setRepositoryName( final String reposName )
		{
		this.reposName = reposName;
		}

	public String
	getNewName()
		{
		return this.newName;
		}

	public void
	setNewName( final String name )
		{
		this.newName = name;
		}

	public String
	getProgram()
		{
		return this.newName;
		}

	public void
	setProgram( final String program )
		{
		this.useProgram = program;
		}

	public String
	getTagSpec()
		{
		return this.tagSpec;
		}

	public void
	setTagSpec( final String tagspec )
		{
		this.tagSpec = tagspec;
		}

	public String
	getChecksum()
		{
			return this.type != CHECKSUM ? null : this.text;
		}

	public void
	setChecksum( final String sumStr )
		{
		if ( this.type == CHECKSUM )
			{
			this.text = sumStr;
			}
		}

	public String
	getValidRequests()
		{
			return this.type != VALID_REQUESTS ? null : this.text;
		}

	public void
	setValidRequests( final String requests )
		{
		if ( this.type == VALID_REQUESTS )
			{
			this.text = requests;
			}
		}

	@Override
	public String
	toString()
		{
		return "[ "
		       + "type=" + this.type + ','
		       + "pathName=" + this.pathName + ','
		       + "reposName=" + this.reposName + ','
		       + "modeLine=" + this.modeLine + ','
		       + "entriesLine=" + this.entriesLine + ','
		       + "newName=" + this.newName + ','
		       + "tagSpec=" + this.tagSpec + ','
		       + "useProgram=" + this.useProgram + ','
		       + "file=" + this.file
		       + " ]";
		}
	}

