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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * This class extends the "global" properties functionality to provide
 * a facility for loading and storing UserPrefs properties.
 *
 *
 * @author Tim Endres,
 *    <a href="mailto:time@ice.com">time@ice.com</a>.
 *
 */

public
class       UserPrefsFileLoader
extends		UserPrefsLoader
    {
	private File		prefsFile = null;


	public
	UserPrefsFileLoader()
		{
		super();
		}

	public
	UserPrefsFileLoader( final File f )
		{
		super();
		this.prefsFile = f;
		}

	public
	UserPrefsFileLoader( final String appName, final String userName, final String prefsName )
		{
		super( appName, userName, prefsName );
		}

	public File
	getFile()
		{
		return this.prefsFile;
		}

	public void
	setFile( final File f )
		{
		this.prefsFile = f;
		}

	@Override
	public void
	loadPreferences( final UserPrefs prefs )
		throws IOException
		{
		FileInputStream fin = null;

		try {
			fin = new FileInputStream( this.prefsFile );
			}
		catch ( final FileNotFoundException ex )
			{
			throw new IOException
				( "could not locate file '"
					+ this.prefsFile.getPath()
					+ "', " + ex.getMessage() );
			}

		prefs.loadProperties( fin );

		fin.close();
		}

	@Override
	public void
	storePreferences( final UserPrefs prefs )
		throws IOException
		{
		FileOutputStream fout = null;

		fout = new FileOutputStream( this.prefsFile );

		final String headerStr = "UserPrefsFileLoader $Revision: 1.2 $";

		prefs.storeProperties( fout, headerStr );

		fout.close();
		}

	}

