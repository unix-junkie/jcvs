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
 * Encapsulates a 'Notify' request, resulting a an 'edit' or 'unedit'.
 *
 * @version $Revision: 2.1 $
 * @author Timothy Gerard Endres, <a href="mailto:time@ice.com">time@ice.com</a>.
 * @see CVSEntry
 * @see CVSRequest
 */

public class
CVSNotifyItem extends Object
	{
	static public final String	RCS_ID = "$Id: CVSNotifyItem.java,v 2.1 1997/04/19 05:12:15 time Exp $";
	static public final String	RCS_REV = "$Revision: 2.1 $";

	private String			type;
	private String			name;
	private String			time;
	private String			host;
	private String			wdir;
	private String			watches;

	private String			repository;


	public CVSNotifyItem(
			String type, String name, String time, String host,
			String wdir, String watches, String repository )
		{
		super();

		this.type = type;
		this.name = name;
		this.time = time;
		this.host = host;
		this.wdir = wdir;
		this.watches = watches;
		this.repository = repository;
		}

	public String
	getType()
		{
		return this.type;
		}

	public String
	getName()
		{
		return this.name;
		}

	public String
	getTime()
		{
		return this.time;
		}

	public String
	getHostname()
		{
		return this.host;
		}

	public String
	getWorkingDirectory()
		{
		return this.wdir;
		}

	public String
	getWatches()
		{
		return this.watches;
		}

	public String
	getRepository()
		{
		return this.repository;
		}

	public String
	getServerExtra()
		{
		return
			this.type + "\t" +
			this.time + "\t" +
			this.host + "\t" +
			this.wdir + "\t" +
			this.watches;
		}

	public String
	toString()
		{
		return "[" +
			this.type + ","	+
			this.name + ","	+
			this.time + ","	+
			this.host + ","	+
			this.wdir + ","	+
			this.watches + ","	+
			this.repository + "]";
		}
	}
	   
