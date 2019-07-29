/*
** Java cvs client application package.
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

package com.ice.jcvsii;

import com.ice.cvsc.CVSRequest;


public
class		ServerDef
	{
	private final String	name;
	private final String	desc;
	private final String	method;
	private final String	module;
	private final String	userName;
	private final String	hostName;
	private final String	repository;

	public
	ServerDef
			( final String name, final String method, final String module, final String userName,
				final String hostName, final String repository, final String desc )
		{
		this.name = name;
		this.method = method;
		this.module = module;
		this.userName = userName;
		this.hostName = hostName;
		this.repository = repository;
		this.desc = desc;
		}

	public String
	toString()
		{
		return this.name;
		}

	public String
	getName()
		{
		return this.name;
		}

	public String
	getDescription()
		{
		return this.desc;
		}

	public String
	getUserName()
		{
		return this.userName;
		}

	public String
	getHostName()
		{
		return this.hostName;
		}

	public String
	getModule()
		{
		return this.module;
		}

	public String
	getRepository()
		{
		return this.repository;
		}

	public boolean
	isPServer()
		{
		return this.method.equalsIgnoreCase( "pserver" );
		}

	public boolean
	isSSHServer()
		{
		return this.method.equalsIgnoreCase( "ext" );
		}

	public int
	getConnectMethod()
		{
		return
			this.method.equalsIgnoreCase( "server" )
			? CVSRequest.METHOD_RSH
			: this.method.equalsIgnoreCase( "ext" )
				? CVSRequest.METHOD_SSH
				: CVSRequest.METHOD_INETD;
		}

	public String
	getConnectMethodName()
		{
		return this.method;
		}

	/**
	 * This method is used by our sorter.
	 * Returns 0 if equal, > 0 if this > other, else < 0.
	 *
	 * @return 0 if equal, > 0 if this > other, else < 0
	 * @author Urban Widmark <urban@svenskatest.se>
	 */
	public int
	compareTo( final ServerDef other )
		{
		final String s = other.getName();
		return name.compareTo( s );
		}
	}

