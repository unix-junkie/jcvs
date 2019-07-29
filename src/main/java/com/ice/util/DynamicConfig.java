/*
** Copyright (c) 1998 by Timothy Gerard Endres
** <mailto:time@ice.com>  <http://www.ice.com>
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

package com.ice.util;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.Vector;


public abstract class
DynamicConfig
	{
	protected String		name = null;
	protected File			homeDir = null;
	protected Properties	workingProps = null;


	public
	DynamicConfig( final String name )
		{
		this.name = name;
		this.workingProps = new Properties();
		this.determineHomeDirectory();
		}

	public String
	getName()
		{
		return this.name;
		}

	public void
	saveProperties()
		throws IOException
		{
		UserProperties.saveDynamicProperties( this.name );
		}

	public void
	setProperty( final String propName, final boolean value )
		{
		UserProperties.setDynamicProperty
			( this.name, propName,
				value ? "true" : "false" );
		}

	public void
	setProperty( final String propName, final int value )
		{
		UserProperties.setDynamicProperty
			( this.name, propName, ""+value );
		}

	public void
	setProperty( final String propName, final String value )
		{
		UserProperties.setDynamicProperty
			( this.name, propName, value );
		}

	public void
	removeProperty( final String propName )
		{
		UserProperties.removeDynamicProperty( this.name, propName );
		}

	public void
	setStringArray( final String propName, final String[] strArray )
		{
		final StringBuffer buf = new StringBuffer();
		for ( int idx = 0 ; idx < strArray.length ; ++idx )
			{
			buf.append( strArray[idx] );
			if ( idx < strArray.length - 1 )
				buf.append( ":" );
			}

		UserProperties.setDynamicProperty
			( this.name, propName, buf.toString() );
		}

	public void
	setStringArray( final String propName, final Vector strArray )
		{
		final int size = strArray.size();
		final StringBuffer buf = new StringBuffer();
		for ( int idx = 0 ; idx < size ; ++idx )
			{
			buf.append( (String) strArray.elementAt(idx) );
			if ( idx < size - 1 )
				buf.append( ":" );
			}

		UserProperties.setDynamicProperty
			( this.name, propName, buf.toString() );
		}

	public File
	getHomeDirectory()
		{
		return this.homeDir;
		}

	public Rectangle
	getBounds( final String propName, final Rectangle defBounds )
		{
		this.workingProps.clear();

		final Rectangle result = new Rectangle();
		defBounds.x =
			UserProperties.getProperty
				( propName + ".x", defBounds.x );
		defBounds.y =
			UserProperties.getProperty
				( propName + ".y", defBounds.y );
		defBounds.width =
			UserProperties.getProperty
				( propName + ".width", defBounds.width );
		defBounds.height =
			UserProperties.getProperty
				( propName + ".height", defBounds.height );

		return defBounds;
		}

	public void
	saveBounds( final String propName, final Rectangle bounds )
		{
		this.saveBounds
			( propName, bounds.x, bounds.y,
				bounds.width, bounds.height );
		}

	public void
	saveBounds( final String propName, final int x, final int y, final int w, final int h )
		{
		this.workingProps.clear();

		this.workingProps.put( propName + ".x", ""+x );
		this.workingProps.put( propName + ".y", ""+y );
		this.workingProps.put( propName + ".width", ""+w );
		this.workingProps.put( propName + ".height", ""+h );

		UserProperties.setDynamicProperties( this.name, this.workingProps );
		}

	public void
	saveLocation( final String propName, final int x, final int y )
		{
		this.workingProps.clear();

		this.workingProps.put( propName + ".x", ""+x );
		this.workingProps.put( propName + ".y", ""+y );

		UserProperties.setDynamicProperties( this.name, this.workingProps );
		}

	public void
	saveSize( final String propName, final int w, final int h )
		{
		this.workingProps.clear();

		this.workingProps.put( propName + ".width", ""+w );
		this.workingProps.put( propName + ".height", ""+h );

		UserProperties.setDynamicProperties( this.name, this.workingProps );
		}

	protected boolean
	isPropertySet( final String propName )
		{
		boolean result = true;

		final String propValue =
			UserProperties.getProperty( propName, null );

		if ( propValue == null )
			result = false;

		return result;
		}
	private void
	determineHomeDirectory()
		{
		String userDirName =
			System.getProperty( "user.home", null );

		if ( userDirName == null )
			{
			userDirName = System.getProperty( "user.dir", null );
			}

		if ( userDirName == null )
			{
			// REVIEW We are using a questionable algorithm here.
			this.homeDir =
				new File( File.separatorChar == ':'
							? ":" : "." );
			}
		else
			{
			this.homeDir = new File( userDirName );
			}
		}
	}

