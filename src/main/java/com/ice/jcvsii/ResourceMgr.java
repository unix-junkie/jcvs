/*
** Java CVS client application package.
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

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;


/**
 * The Configuration class.
 *
 * @version $Revision: 1.2 $
 * @author Timothy Gerard Endres,
 *  <a href="mailto:time@ice.com">time@ice.com</a>.
 */

public
class		ResourceMgr
	{
	static public final String		RCS_ID = "$Id: ResourceMgr.java,v 1.2 1999/04/01 19:41:11 time Exp $";
	static public final String		RCS_REV = "$Revision: 1.2 $";

	/**
	 * The instance of the ONLY ResourceMgr.
	 */
	private static ResourceMgr		instance;

	/**
	 * Set to true to get processing debugging on stderr.
	 */
	private boolean					debug;

	/**
	 * The manager's name. Currently, used only to improved debugging output.
	 */
	private final String					name;

	/**
	 * The user interface resource bundle. This includes strings like menu
	 * items, window titles, user prompts, field labels, etc.
	 */
	private ResourceBundle			ui;


	public static ResourceMgr
	getInstance()
		{
		return ResourceMgr.instance;
		}

	public static void
	initializeResourceManager( final String name )
		{
		ResourceMgr.instance = new ResourceMgr( name );
		ResourceMgr.instance.initializeResources();
		}

	public
	ResourceMgr()
		{
		this.name = "DEFAULT";
		}

	public
	ResourceMgr( final String name )
		{
		this.name = name;
		}

	public String
	getUIString( final String key )
		{
		return this.ui.getString( key );
		}

	public String
	getUIFormat( final String key, final Object[] args )
		{
		return MessageFormat.format
			( this.ui.getString( key ), args );
		}

	private void
	printResourceInfo( final String name, final ResourceBundle rb )
		{
		System.err.println
			( "Loaded resource bundle '" + name + "'." );

	//  JDK2 required...
	//	System.err.println
	//		( "   " + name + ".Locale = " + rb.getLocale() );
		}

	public void
	initializeResources()
		{
		String rbnm;
		try {
			// USER INTERFACE BUNDLE
			rbnm = "com.ice.jcvsii.rsrcui";
			this.ui = ResourceBundle.getBundle( rbnm );
			this.printResourceInfo( rbnm, this.ui );
			}
		catch ( final MissingResourceException ex )
			{
			ex.printStackTrace();
			}
		}

	}


// myResourceBundle.getString("OkKey")

