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

package com.ice.jcvsii;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.activation.DataSource;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

/**
 * this Frame provides a utility class for displaying a single
 * Component in a Frame.
 *
 */

public class
ComponentFrame extends Frame
	{
	private String		contentType;

	/**
	 * creates the frame with the given name
	 * @param what	the component to display
	 * @param name	the name of the Frame
	 */
	public
	ComponentFrame( final Component comp, final String name, final DataSource source )
		{
		super( name );

		try {
			final MimeType mime = new MimeType( source.getContentType() );
			this.contentType = mime.getBaseType();
			}
		catch ( final MimeTypeParseException ex )
			{
			this.contentType = source.getContentType();
			}

		this.addWindowListener( new WinClose() );

		this.setLayout( new BorderLayout( 1, 1 ) );

		if ( comp != null )
			{
			this.add( comp, BorderLayout.CENTER );
			}

		this.pack();

		this.loadLayoutProperties();
		}

	public void
	loadLayoutProperties()
		{
		}

	public void
	saveLayoutProperties()
		{
		}

	private Rectangle
	computeDefaultPosition()
		{
		final Dimension sz = this.getSize();
		final Dimension scrnSz = this.getToolkit().getScreenSize();

		if ( sz.width > scrnSz.width - 10 )
			sz.width = scrnSz.width - 10;
		if ( sz.height > scrnSz.height - 10 )
			sz.height = scrnSz.height - 10;

		int x = (scrnSz.width - sz.width) / 2;
		int y = (scrnSz.height - sz.height) / 3;

		// Make sure we are not off the screen.
		if ( x < 0 ) x = 0;
		if ( y < 0 ) y = 0;

		// Make sure we are not bigger than the screen.
		if ( x + sz.width > scrnSz.width )
			sz.width = scrnSz.width - x;

		if ( y + sz.height > scrnSz.height )
			sz.height = scrnSz.height - y;

		return new Rectangle( x, y, sz.width, sz.height );
		}

	class
	WinClose extends WindowAdapter
		{
		public void windowClosing( final WindowEvent e )
			{
			saveLayoutProperties();
			e.getWindow().dispose();
			}
		}
	}

