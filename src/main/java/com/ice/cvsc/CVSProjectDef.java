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
import java.io.IOException;

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
 * @version $Revision: 2.4 $
 * @author Timothy Gerard Endres, <a href="mailto:time@ice.com">time@ice.com</a>.
 * @see CVSClient
 * @see CVSProject
 * @see CVSEntryVector
 */

public
class		CVSProjectDef {
	public static final String		RCS_ID = "$Id: CVSProjectDef.java,v 2.4 2003/07/27 01:08:32 time Exp $";
	public static final String		RCS_REV = "$Revision: 2.4 $";

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
	CVSProjectDef( final String rootStr, final String reposStr )
		{
		this.parseRootDirectory( rootStr, reposStr );
		}

	public
	CVSProjectDef(
			final int connMeth, final boolean isPServ, final boolean noMode,
			final String host, final String user, final String rootDir, final String repos )
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
		return this.connectMethod == CVSRequest.METHOD_SSH;
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
		final String connMethod;

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

		final StringBuilder rootDirStr = new StringBuilder(128 );

		if ( ! this.noModeRoot )
			{
			rootDirStr.append(':');
			rootDirStr.append( connMethod );
			rootDirStr.append(':');
			}

		if (!this.userName.isEmpty())
			{
			rootDirStr.append( this.userName );
			rootDirStr.append('@');
			}

		rootDirStr.append( this.hostName );

	/*
	** UNDONE HIGH
	** Standard CVS URL has a port field:
	**
		// If there is a non-standard port, add to URL
		if ( this.isPServer() )
			{
			if ( this.port != CVSClient.DEFAULT_CVS_PORT )
				{
				rootDirStr.append( "#" );
				rootDirStr.append( this.port );
				}
			}
	**
	*/

		rootDirStr.append(':');
		rootDirStr.append( this.rootDirectory );

		return rootDirStr.toString();
		}

	private synchronized void
	parseRootDirectory(final String specification, final String repos)
		{
		final String		tempStr;
		final String		methodStr;
		final String		userNameStr = "";
		final String		hostNameStr = "";
		int			index;
			final int subidx;
			final int			connMethod;
		boolean		isOk = true;

		this.isValid = false;
		this.isPServer = false;
		this.repository = repos;
		this.noModeRoot = false;

		this.reason = "parsed '" + specification + '\'';

		String rootDirSpec = specification;

		if (!rootDirSpec.isEmpty() && rootDirSpec.charAt(0) == ':')
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
							switch (methodStr) {
							case "pserver":
								this.isPServer = true;
								this.connectMethod = CVSRequest.METHOD_INETD;
								break;
							case "server":
								this.isPServer = false;
								this.connectMethod = CVSRequest.METHOD_RSH;
								break;
							case "ext":
								this.isPServer = false;
								this.connectMethod = CVSRequest.METHOD_SSH;
								break;
							case "direct":
								this.isPServer = false;
								this.connectMethod = CVSRequest.METHOD_INETD;
								break;
							default:
								this.isPServer = false;
								this.connectMethod = CVSRequest.METHOD_RSH;
								break;
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
			this.repository = this.rootDirectory + '/' + this.repository;
			}

		}

	/**
	 * @param adminPath The path to the 'CVS/' admin directory.
	 */
	public static CVSProjectDef
	readDef( final String adminPath )
		throws IOException
		{
		final String rootPath =
			CVSProject.getAdminRootPath( adminPath );

		final File adminRootFile = new File( rootPath );

		if ( ! adminRootFile.exists() )
			throw new IOException
				( "admin Root file '" + adminRootFile.getPath()
					+ "' does not exist" );

		final String reposPath =
			CVSProject.getAdminRepositoryPath( adminPath );

		final File adminReposFile = new File( reposPath );

		if ( ! adminReposFile.exists() )
			throw new IOException
				( "admin Repository file '" + adminReposFile.getPath()
					+ "' does not exist" );

		final String rootDirectoryStr =
			CVSCUtilities.readStringFile( adminRootFile );

		if ( rootDirectoryStr == null )
			throw new IOException
				( "reading admin Root file '"
					+ adminRootFile.getPath() );

		final String repositoryStr =
			CVSCUtilities.readStringFile( adminReposFile );

		if ( repositoryStr == null )
			throw new IOException
				( "reading admin Repository file '"
					+ adminReposFile.getPath() );

		final CVSProjectDef def =
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


