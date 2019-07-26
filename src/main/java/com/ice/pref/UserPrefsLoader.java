/*
** User Preferences Package.
** Copyright (c) 1999 by Timothy Gerard Endres
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

package com.ice.pref;

import java.io.*;
import java.lang.System;
import java.lang.Throwable;
import java.util.*;



/**
 * This class extends the "global" properties functionality to provide
 * a facility for loading and storing UserPrefs properties.
 *
 *
 * @author Tim Endres,
 *    <a href="mailto:time@ice.com">time@ice.com</a>.
 *
 */

public abstract
class       UserPrefsLoader
extends		Properties
implements	UserPrefsConstants
	{
	private String		appName = null;
	private String		userName = null;
	private String		prefsName = null;


	public static UserPrefsLoader
	getDefaultLoader()
		{
		return getLoader( UserPrefsLoader.STREAM_LOADER );
		}

	public static UserPrefsLoader
	getDefaultLoader( String appName, String userName, String prefsName )
		{
		UserPrefsLoader result =
			UserPrefsLoader.getDefaultLoader();

		result.setAppName( appName );
		result.setUserName( userName );
		result.setPrefsName( prefsName );

		return result;
		}

	public static UserPrefsLoader
	getLoader( String loaderName )
		{
		if ( loaderName.equalsIgnoreCase
				( UserPrefsLoader.FILE_LOADER ) )
			{
			return new UserPrefsFileLoader();
			}
		else if ( loaderName.equalsIgnoreCase
				( UserPrefsLoader.STREAM_LOADER ) )
			{
			return new UserPrefsStreamLoader();
			}

		return null;
		}

	public static UserPrefsLoader
	getLoader(
			String loaderName, String appName,
			String userName, String prefsName )
		{
		UserPrefsLoader result =
			UserPrefsLoader.getLoader( loaderName );

		if ( result != null )
			{
			result.setAppName( appName );
			result.setUserName( userName );
			result.setPrefsName( prefsName );
			}

		return result;
		}

	public
	UserPrefsLoader()
		{
		}

	public
	UserPrefsLoader( String appName, String userName, String prefsName )
		{
		this.appName = appName;
		this.userName = userName;
		this.prefsName = prefsName;
		}

	public String
	getAppName()
		{
		return this.appName;
		}

	public void
	setAppName( String name )
		{
		this.appName = name;
		}

	public String
	getUserName()
		{
		return this.userName;
		}

	public void
	setUserName( String name )
		{
		this.userName = name;
		}

	public String
	getPrefsName()
		{
		return this.prefsName;
		}

	public void
	setPrefsName( String prefsName )
		{
		this.prefsName = prefsName;
		}

	public abstract void
		loadPreferences( UserPrefs prefs )
			throws IOException;

	public abstract void
		storePreferences( UserPrefs prefs )
			throws IOException;

	}

