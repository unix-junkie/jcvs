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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;

import javax.swing.JOptionPane;

import com.ice.cvsc.CVSLog;


final
class		CVSUserDialog
	{
	public static final int		NOTE = 1;
	public static final int		ERROR = 2;

		private CVSUserDialog() {
		}


		public static void
	Note( final String message )
		{
		Note( null, message );
		}

	public static void
	Note(final Component parent, final String message )
		{
		final Point location =
			Config.getPreferences().getPoint
				( "noteDialog.location", new Point( 40, 40 ) );

		final Dimension size =
			Config.getPreferences().getDimension
				( "noteDialog.size", new Dimension( 480, 200 ) );

		Note( parent, message, location, size );
		}

	public static void
	Note(final Component parent, final String message, final Point location )
		{
		final Dimension size =
			Config.getPreferences().getDimension
				( "noteDialog.size", new Dimension( 480, 200 ) );

		Note( parent, message, location, size );
		}

	private static void
	Note(final Component parent, final String message, final Point location, final Dimension size)
		{
		JOptionPane.showMessageDialog
			( parent, message, "Note", JOptionPane.INFORMATION_MESSAGE );
		}

	public static void
	Error( final String message )
		{
		Note( null, message );
		}

	public static void
	Error(final Component parent, final String message )
		{
		final Point loc =
			Config.getPreferences().getPoint
				( "errorDialog.location", new Point( 40, 40 ) );

		final Dimension size =
			Config.getPreferences().getDimension
				( "errorDialog.size", new Dimension( 480, 200 ) );

		Note( parent, message, loc, size );
		}

	public static void
	Error(final Component parent, final String message, final Point location )
		{
		final Dimension size =
			Config.getPreferences().getDimension
				( "errorDialog.size", new Dimension( 480, 200 ) );

		Note( parent, message, location, size );
		}

	public static void
	Error(final Component parent, final String message, final Point location, final Dimension size )
		{
		CVSLog.logMsg("ERROR_DIALOG: '" + message + '\'');

		JOptionPane.showMessageDialog
			( parent, message, "Error", JOptionPane.ERROR_MESSAGE );
		}

	}
