/*
** Tim Endres' utilities package.
** Copyright (c) 1997 by Tim Endres
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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.ImageProducer;
import java.io.IOException;
import java.net.URL;
import java.util.StringTokenizer;

/**
 * This is a class that contains useful utility functions related
 * to the Java AWT package.
 */


public final class
AWTUtilities
	{
		private AWTUtilities() {
		}

		public static Point
	centerDialogInParent(final Component dialog, final Component parent )
		{
		final Point parLoc = parent.getLocationOnScreen();

		final Dimension parSz = parent.getSize();
		final Dimension dlgSz = dialog.getSize();

		final int x = parLoc.x + (parSz.width - dlgSz.width) / 2;
		final int y = parLoc.y + (parSz.height - dlgSz.height) / 3;

		return new Point( x, y );
		}

	public static Point
	computeDialogLocation(final Component dialog, final int w, final int h )
		{
		final Dimension scrnSz =
			dialog.getToolkit().getScreenSize();

		final int x = (scrnSz.width - w) / 2;
		final int y = (scrnSz.height - h) / 3;

		return new Point( x, y );
		}

	public static Point
	computeDialogLocation( final Component dialog )
		{
		final Dimension dlgSz = dialog.getSize();
		final Dimension scrnSz =
			dialog.getToolkit().getScreenSize();

		final int x = (scrnSz.width - dlgSz.width) / 2;
		final int y = (scrnSz.height - dlgSz.height) / 3;

		return new Point( x, y );
		}

	public static Point
	computeDialogLocation(final Component dialog, final Component rel )
		{
		final Dimension dlgSz = dialog.getSize();
		final Dimension scrnSz = dialog.getToolkit().getScreenSize();

		int x = (scrnSz.width - dlgSz.width) / 2;
		int y = (scrnSz.height - dlgSz.height) / 3;

		if ( rel != null )
			{
			final Dimension relSz = rel.getSize();
			final Point loc = rel.getLocationOnScreen();

			x = loc.x + (relSz.width - dlgSz.width) / 2;

			y = loc.y + (relSz.height - dlgSz.height) / 2;
			}

		if ( x < 0 ) x = 0;
		if ( y < 0 ) y = 0;

		return new Point( x, y );
		}

	public static void
	constrain(
			final Container container, final Component component,
			final int fill, final int anchor,
			final int gx, final int gy, final int gw, final int gh, final double wx, final double wy )
		{
		final GridBagConstraints	c =
			new GridBagConstraints();

		c.fill = fill;
		c.anchor = anchor;
		c.gridx = gx;
		c.gridy = gy;
		c.gridwidth = gw;
		c.gridheight = gh;
		c.weightx = wx;
		c.weighty = wy;

		( (GridBagLayout)container.getLayout() ).
			setConstraints( component, c );

		container.add( component );
		}

	public static void
	constrain(
			final Container container, final Component component,
			final int fill, final int anchor,
			final int gx, final int gy, final int gw, final int gh,
			final double wx, final double wy, final Insets inset )
		{
		final GridBagConstraints	c =
			new GridBagConstraints();

		c.fill = fill;
		c.anchor = anchor;
		c.gridx = gx;
		c.gridy = gy;
		c.gridwidth = gw;
		c.gridheight = gh;
		c.weightx = wx;
		c.weighty = wy;
		c.insets = inset;

		( (GridBagLayout)container.getLayout() ).
			setConstraints( component, c );

		container.add( component );
		}

	public static void
	constrain(
			final Container container, final Component component,
			final int fill, final int anchor,
			final int gx, final int gy, final int gw, final int gh,
			final double wx, final double wy,
			final int ipadx, final int ipady )
		{
		final GridBagConstraints	c =
			new GridBagConstraints();

		c.fill = fill;
		c.anchor = anchor;
		c.gridx = gx;
		c.gridy = gy;
		c.gridwidth = gw;
		c.gridheight = gh;
		c.weightx = wx;
		c.weighty = wy;
		c.ipadx = ipadx;
		c.ipady = ipady;

		( (GridBagLayout)container.getLayout() ).
			setConstraints( component, c );

		container.add( component );
		}

	public static Font
	getFont( final String fontName )
		{
		final StringTokenizer toker =
			new StringTokenizer( fontName, "-", false );

		String sName = "Helvetica";
		String sStyle = "plain";
		String sSize = "12";

		final int numTokes = toker.countTokens();
		final boolean isok = true;

		try {
			if ( numTokes > 0 )
				{
				sName = toker.nextToken();

				if ( numTokes == 2 )
					{
					sSize = toker.nextToken();
					}
				else if ( numTokes == 3 )
					{
					sStyle = toker.nextToken();
					sSize = toker.nextToken();
					}
				}
			}
		catch ( final Exception ex )
			{
			System.err.println
				( "Bad font specification '" + fontName + "' - "
					+ ex.getMessage() );
			return null;
			}

		final int style =
				  sStyle.equalsIgnoreCase( "plain" )
					? Font.PLAIN :
				sStyle.equalsIgnoreCase( "bold" )
					? Font.BOLD :
				sStyle.equalsIgnoreCase( "italic" )
					? Font.ITALIC : Font.BOLD + Font.ITALIC;

		final int size = Integer.parseInt( sSize );

		return new Font( sName, style, size );
		}

	// The subtlety in getResource() is that it uses the
	// Class loader of the class used to get the rousource.
	// This means that if you want to load a resource from
	// your JAR file, then you better use a class in the
	// JAR file.

	public static Image
	getImageResource( final String name )
		throws IOException
		{
		return
			getImageResource
				( AWTUtilities.class, name );
		}

	private static Image
	getImageResource(final Class base, final String name)
		throws IOException
		{
		Image	result = null;

		final URL imageURL = base.getResource( name );

		if ( imageURL != null )
			{
			final Toolkit	tk = Toolkit.getDefaultToolkit();

			result = tk.createImage
				( (ImageProducer) imageURL.getContent() );
			}

		return result;
		}

	public static Image
	getSystemImageResource( final String name )
		throws IOException
		{
		Image	result = null;

		final URL imageURL = ClassLoader.getSystemResource( name );
		if ( imageURL != null )
			{
			final Toolkit	tk = Toolkit.getDefaultToolkit();

			result = tk.createImage
				( (ImageProducer) imageURL.getContent() );
			}

		return result;
		}

	}

