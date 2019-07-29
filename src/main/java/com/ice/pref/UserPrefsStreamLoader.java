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
import java.io.InputStream;
import java.io.OutputStream;


/**
 * This class extends the UserPrefsLoader to allow loading and storing
 * via InputStreams and OutputStreams.
 *
 *
 * @author Tim Endres,
 *    <a href="mailto:time@ice.com">time@ice.com</a>.
 *
 */

public
class       UserPrefsStreamLoader
extends		UserPrefsLoader
    {
	private InputStream		inStream = null;
	private OutputStream	outStream = null;


	public
	UserPrefsStreamLoader()
		{
		super();
		}

	public
	UserPrefsStreamLoader( final InputStream in, final OutputStream out )
		{
		super();
		this.inStream = in;
		this.outStream = out;
		}

	public
	UserPrefsStreamLoader( final String appName, final String userName, final String prefsName )
		{
		super( appName, userName, prefsName );
		}

	public InputStream
	getInputStream()
		{
		return this.inStream;
		}

	public void
	setInputStream( final InputStream in )
		{
		this.inStream = in;
		}

	public OutputStream
	getOutputStream()
		{
		return this.outStream;
		}

	public void
	setOutputStream( final OutputStream out )
		{
		this.outStream = out;
		}

	public void
	loadPreferences( final UserPrefs prefs )
		throws IOException
		{
		if ( this.inStream == null )
			throw new IOException
				( "you have not yet called setInputStream()" );

		prefs.loadProperties( this.inStream );
		}

	public void
	storePreferences( final UserPrefs prefs )
		throws IOException
		{
		if ( this.outStream == null )
			throw new IOException
				( "you have not yet called setOutputStream()" );

		final String headerStr = "UserPrefsStreamLoader $Revision: 1.2 $";
		prefs.storeProperties( this.outStream, headerStr );
		}

	}

