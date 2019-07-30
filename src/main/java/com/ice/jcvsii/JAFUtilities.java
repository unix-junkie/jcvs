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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;

import javax.activation.CommandInfo;
import javax.activation.CommandObject;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import com.ice.pref.UserPrefs;


final
class		JAFUtilities {
	private JAFUtilities() {
	}

	public static void
	openFile( final String entryName, final File entryFile, final String verb )
		{
		final int			i;
			final int index;
			final String		argsel = null;
		final String		method = null;

		final UserPrefs prefs = Config.getPreferences();

		final DataHandler dh =
			new DataHandler( new FileDataSource( entryFile ) );

		if ( prefs.getBoolean( "global.useJAF", false ) )
			{
			final Object viewer = getMailcapViewer( dh, verb );

			if ( viewer != null )
				{
				if ( viewer instanceof Component )
					{
					String title = entryFile.getPath();

					if ( title == null || title.length() < 1 )
						title = entryName;

					final Component frame =
						new ComponentFrame
							( (Component)viewer, title, dh.getDataSource() );

					frame.show();
					}
				}
			}
		else
			{
			final ExecViewer ev = new ExecViewer();
			ev.exec( verb, dh );
			}
		}


	private static Object
	getMailcapViewer( final DataHandler dh, final String verb )
		{
		Object bean = null;
		String msg = null;

		final CommandInfo ci = dh.getCommand( verb );

		if ( ci == null )
			{
			String contentType = dh.getContentType();

			try {
				final MimeType mime = new MimeType( contentType );
				contentType = mime.getBaseType();
				}
			catch ( final MimeTypeParseException ex )
				{ }

			final String[] fmtArgs = { verb, contentType };
			msg = ResourceMgr.getInstance().getUIFormat
				( "jaf.getcommand.failed.msg", fmtArgs );
			}
		else
			{
			bean = dh.getBean( ci );
			if ( bean == null )
				{
				final CommandObject ev = new ExecViewer();
				try {
					ev.setCommandContext( verb, dh );
					bean = ev;
					}
				catch ( final IOException ex )
					{
					ex.printStackTrace();
					// UNDONE Move stack trace into msg bean...
					final String[] fmtArgs = { verb, ex.getMessage() };
					msg = ResourceMgr.getInstance().getUIFormat
						( "jaf.execviewer.failed.msg", fmtArgs );
					}
				}
			}

		if ( bean == null )
			{
			final JComponent msgPan = new JPanel();
			msgPan.setLayout( new BorderLayout() );
			msgPan.setBorder( new EmptyBorder( 5, 5, 5, 5 ) );

			final JTextArea msgText = new JTextArea( msg );
			msgText.setMargin( new Insets( 10, 10, 10, 10 ) );
			msgText.setFont( new Font( "Serif", Font.PLAIN, 18 ) );
			msgPan.add( BorderLayout.CENTER, msgText );

			bean = msgPan;
			}

		return bean;
		}

	}


