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

import java.io.IOException;
import java.util.Properties;

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
	{
	private String		userName;

	public static UserPrefsLoader
	getLoader( final String loaderName )
		{
		if ( loaderName.equalsIgnoreCase
				( UserPrefsConstants.FILE_LOADER ) )
			{
			return new UserPrefsFileLoader();
			}
		if ( loaderName.equalsIgnoreCase
				( UserPrefsConstants.STREAM_LOADER ) )
			{
			return new UserPrefsStreamLoader();
			}

		return null;
		}

	UserPrefsLoader() {
		// empty
	}

	public String
	getUserName()
		{
		return this.userName;
		}

	public void
	setUserName( final String name )
		{
		this.userName = name;
		}

	public abstract void
		loadPreferences( UserPrefs prefs )
			throws IOException;

	public abstract void
		storePreferences( UserPrefs prefs )
			throws IOException;

	}

