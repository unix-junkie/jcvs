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
 * CVSEntry implements the concept of a CVS Entry. Traditionally,
 * a CVS Entry is a line in an 'Entries' file in a 'CVS' admin
 * directory. A CVSEntry represents a CVS file that is checked
 * in or being checked in.
 *
 * CVSEntry objects contain all of the relavent information about
 * a CVS file, such as its name, check-out time, modification status,
 * local pathname, repository, etc.
 *
 * @version $Revision: 2.3 $
 * @author Timothy Gerard Endres, <a href="mailto:time@ice.com">time@ice.com</a>.
 * @see CVSClient
 * @see CVSProject
 * @see CVSEntryVector
 */

public
class		CVSProjectDef
extends		Object
	{
	static public final String		RCS_ID = "$Id: CVSProjectDef.java,v 2.3 2002/02/10 18:01:44 time Exp $";
	static public final String		RCS_REV = "$Revision: 2.3 $";

	/**
	 * True if this definition is parsed and valid.
	 */
	private boolean			isValid;

	/**
	 * True if this used a password INETD login method.
	 */
	private boolean			isPServer;

	/**
	 * The connection method.
	 */
	private int				connectMethod;
	private String			connectMethodStr;

	/**
	 * The CVS server hostname.
	 */
	private String			hostName;

	/**
	 * The user name used for login.
	 */
	private String			userName;

	/**
	 * The CVS root directory.
	 */
	private String			rootDirectory;

	/**
	 * The CVS repository path.
	 */
	private String			repository;

	/**
	 * The reason we are not valid.
	 */
	private String			reason;

	/**
	 * Set to true when we see a Root spec with no "method",
	 * such as "user@host:/path/to/cvs".
	 */
	private boolean			noModeRoot;


	public
	CVSProjectDef( String rootStr, String reposStr )
		{
		this.parseRootDirectory( rootStr, reposStr );
		}

	public
	CVSProjectDef(
			int connMeth, boolean isPServ, boolean noMode,
			String host, String user, String rootDir, String repos )
		{
		this.isValid = true; // UNDONE
		this.isPServer = isPServ;
		this.noModeRoot = noMode;

		this.hostName = host;
		this.userName = user;
		this.repository = repos;
		this.rootDirectory = rootDir;

		this.connectMethod = connMeth;

		if ( this.connectMethod == CVSRequest.METHOD_RSH )
			{
			this.connectMethodStr = "server";
			}
		else if ( this.connectMethod == CVSRequest.METHOD_SSH )
			{
			this.connectMethodStr = "ext";
			}
		else if ( this.isPServer )
			{
			this.connectMethodStr = "pserver";
			}
		else
			{
			this.connectMethodStr = "direct";
			}
		}

	public synchronized boolean
	isValid()
		{
		return this.isValid;
		}

	public synchronized boolean
	isPServer()
		{
		return this.isPServer;
		}

	public synchronized boolean
	isSSHServer()
		{
		return (this.connectMethod == CVSRequest.METHOD_SSH);
		}

	public synchronized int
	getConnectMethod()
		{
		return this.connectMethod;
		}

	public synchronized String
	getConnectMethodString()
		{
		return this.connectMethodStr;
		}

	public synchronized String
	getUserName()
		{
		return this.userName;
		}

	public synchronized String
	getHostName()
		{
		return this.hostName;
		}

	public synchronized String
	getRootDirectory()
		{
		return this.rootDirectory;
		}

	public synchronized String
	getRepository()
		{
		return this.repository;
		}

	public synchronized String
	getReason()
		{
		return this.reason;
		}

	public String
	getRootDirectorySpec()
		{
		String connMethod;

		if ( this.connectMethod == CVSRequest.METHOD_RSH )
			{
			connMethod = "server";
			}
		else if ( this.connectMethod == CVSRequest.METHOD_SSH )
			{
			connMethod = "ext";
			}
		else if ( this.isPServer() )
			{
			connMethod = "pserver";
			}
		else
			{
			connMethod = "direct";
			}

		StringBuffer rootDirStr = new StringBuffer( 128 );

		if ( ! this.noModeRoot )
			{
			rootDirStr.append( ":" );
			rootDirStr.append( connMethod );
			rootDirStr.append( ":" );
			}

		if ( this.userName.length() > 0 )
			{
			rootDirStr.append( this.userName );
			rootDirStr.append( "@" );
			}

		rootDirStr.append( this.hostName );
		rootDirStr.append( ":" );
		rootDirStr.append( this.rootDirectory );

		return rootDirStr.toString();
		}

	public synchronized boolean
	parseRootDirectory( String specification, String repos )
		{
		String		tempStr;
		String		methodStr;
		String		userNameStr = "";
		String		hostNameStr = "";
		int			index, subidx;
		int			connMethod;
		boolean		isOk = true;

		this.isValid = false;
		this.isPServer = false;
		this.repository = repos;
		this.noModeRoot = false;

		this.reason = "parsed '" + specification + "'";

		String rootDirSpec = specification;

		if ( rootDirSpec.startsWith( ":" ) )
			{
			rootDirSpec = rootDirSpec.substring( 1 );
			
			index = rootDirSpec.indexOf( ':' );
			if ( index > 0 )
				{
				methodStr = rootDirSpec.substring( 0, index );
				rootDirSpec = rootDirSpec.substring( index + 1 );
			
				if ( methodStr.equalsIgnoreCase( "ext" )
						|| methodStr.equalsIgnoreCase( "pserver" )
						|| methodStr.equalsIgnoreCase( "direct" )
						|| methodStr.equalsIgnoreCase( "server" ) )
					{
					this.connectMethodStr = methodStr;

					index = rootDirSpec.indexOf( ':' );
					tempStr = rootDirSpec.substring( 0, index );

					this.rootDirectory =
						rootDirSpec.substring( index + 1 );

					if ( index > 0 )
						{
						if ( methodStr.equals( "pserver" ) )
							{
							this.isPServer = true;
							this.connectMethod = CVSRequest.METHOD_INETD;
							}
						else if ( methodStr.equals( "server" ) )
							{
							this.isPServer = false;
							this.connectMethod = CVSRequest.METHOD_RSH;
							}
						else if ( methodStr.equals( "ext" ) )
							{
							this.isPServer = false;
							this.connectMethod = CVSRequest.METHOD_SSH;
							}
						else if ( methodStr.equals( "direct" ) )
							{
							this.isPServer = false;
							this.connectMethod = CVSRequest.METHOD_INETD;
							}
						else
							{
							this.isPServer = false;
							this.connectMethod = CVSRequest.METHOD_RSH;
							}

						subidx = tempStr.indexOf( '@' );
						if ( subidx > 0 )
							{
							// ':method:user@host:...' format
							this.userName = tempStr.substring( 0, subidx );
							this.hostName = tempStr.substring( subidx + 1 );
							}
						else
							{
							// ':method:host:...' format
							this.userName = System.getProperty( "user.name", "" );
							this.hostName = tempStr;
							if ( this.isPServer )
								{
								isOk = false;
								CVSLog.logMsg
									( "ERROR Root directory spec '"
										+ specification
										+ "' is invalid (pserver: no user)." );
								}
							}
						}
					else
						{
						isOk = false;
						this.reason =
							"ERROR Root directory spec '" + specification +
								"' is invalid (incomplete).";
						}
					}
				else
					{
					isOk = false;
					this.reason =
						"ERROR Root directory spec '" + specification +
							"' is invalid (server not 'server' or 'pserver').";
					}
				}
			else
				{
				isOk = false;
				this.reason =
					"ERROR Root directory spec '" + specification +
						"' is invalid (no server spec).";
				}

			this.isValid = isOk;
			}
		else
			{
			// The command line client sometimes uses an "empty method" with
			// a "user@host:path" syntax that implies the "server" method.
			//
			index = rootDirSpec.indexOf( '@' );
			subidx = rootDirSpec.indexOf( ':' );

			if ( index > 0 && subidx > index )
				{
				this.isValid = true;
				this.noModeRoot = true;
				this.isPServer = false;
				this.connectMethod = CVSRequest.METHOD_RSH;
				this.connectMethodStr = "";
				this.userName = rootDirSpec.substring( 0, index );
				this.hostName = rootDirSpec.substring( index + 1, subidx );
				this.rootDirectory = rootDirSpec.substring( subidx + 1 );
				}
			else
				{
				this.isValid = false;
				this.reason =
					"ERROR Root directory spec '" + specification +
						"' is invalid.";
				}
			}

		if ( this.isValid
				&& ! this.repository.startsWith( this.rootDirectory ) )
			{
			this.repository = this.rootDirectory + "/" + this.repository;
			}

		return this.isValid;
		}

	/**
	 * @param adminPath The path to the 'CVS/' admin directory.
	 */
	public static CVSProjectDef
	readDef( String adminPath )
		throws IOException
		{
		String rootPath =
			CVSProject.getAdminRootPath( adminPath );

		File adminRootFile = new File( rootPath );

		if ( ! adminRootFile.exists() )
			throw new IOException
				( "admin Root file '" + adminRootFile.getPath()
					+ "' does not exist" );

		String reposPath =
			CVSProject.getAdminRepositoryPath( adminPath );

		File adminReposFile = new File( reposPath );

		if ( ! adminReposFile.exists() )
			throw new IOException
				( "admin Repository file '" + adminReposFile.getPath()
					+ "' does not exist" );

		String rootDirectoryStr =
			CVSCUtilities.readStringFile( adminRootFile );

		if ( rootDirectoryStr == null )
			throw new IOException
				( "reading admin Root file '"
					+ adminRootFile.getPath() );

		String repositoryStr =
			CVSCUtilities.readStringFile( adminReposFile );

		if ( repositoryStr == null )
			throw new IOException
				( "reading admin Repository file '"
					+ adminReposFile.getPath() );

		CVSProjectDef def =
			new CVSProjectDef( rootDirectoryStr, repositoryStr );

		if ( ! def.isValid() )
			{
			throw new IOException
				( "CVS admin defintion is not valid, "
					+ def.getReason() );
			}
		
		return def;
		}

	}


