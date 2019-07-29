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
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;

import com.ice.util.AWTUtilities;


/**
 * Shows the application's "About" dialog box.
 *
 * @version $Revision: 1.2 $
 * @author Timothy Gerard Endres, <a href="mailto:time@ice.com">time@ice.com</a>.
 */

public
class		HTMLDialog
extends		JDialog
implements	ActionListener
	{
	static public final String		RCS_ID = "$Id: HTMLDialog.java,v 1.2 1999/04/01 19:41:11 time Exp $";
	static public final String		RCS_REV = "$Revision: 1.2 $";

	private JTextArea	messageText;

	public
	HTMLDialog( final Frame parent, final String title, final boolean modal, final String html )
		{
		super( parent, title, modal );

		this.establishDialogContents( html );
/*
** This code causes the dialog to crash!
**
		Dimension sz = this.getPreferredSize();
		if ( sz.width < 400 ) sz.width = 400;
		if ( sz.height < 240 ) sz.height = 240;
		this.setSize( sz );
**
*/
		this.setSize( new Dimension( 480, 320 ) );

		Point location;

		if ( parent != null )
			{
			location =
				AWTUtilities.centerDialogInParent( this, parent );
			}
		else
			{
			location =
				AWTUtilities.computeDialogLocation( this, 480, 320 );
			}

		this.setLocation( location.x, location.y );
		}

    @Override
    public void
    actionPerformed( final ActionEvent evt )
        {
	    final String command = evt.getActionCommand();

		if ( command.compareTo( "OK" ) == 0 )
			{
			this.dispose();
			}
        }

	public void
	establishDialogContents( final String html )
		{
		JButton			button;

		JEditorPane pane = null;
		EditorKit editor = null;
		Document doc = null;

		try {
			pane = new JEditorPane();
			pane.setContentType( "text/html" );
			pane.setEditable( false );
			editor = pane.getEditorKit();
			doc = editor.createDefaultDocument();
			final Reader rdr = new StringReader( html );
			editor.read( rdr, doc, 0 );
			pane.setDocument( doc );
			}
		catch ( final IOException ex )
			{
			ex.printStackTrace( System.err );
			pane = null;
			}
		catch ( final BadLocationException ex )
			{
			ex.printStackTrace( System.err );
			pane = null;
			}

		final JScrollPane scroller = new JScrollPane();
		scroller.getViewport().add( pane );

		final JPanel ctlPan = new JPanel();
		ctlPan.setLayout( new BorderLayout() );

		final ResourceMgr rmgr = ResourceMgr.getInstance();
		button = new JButton( rmgr.getUIString( "name.for.ok" ) );
		button.addActionListener( this );
		button.setActionCommand( "OK" );
		ctlPan.add( BorderLayout.EAST, button );

		final JPanel content = new JPanel();
		content.setLayout( new BorderLayout( 0, 8 ) );
		content.setBorder( new EmptyBorder( 6, 6, 6, 6 ) );

		content.add( BorderLayout.CENTER, scroller );
		content.add( BorderLayout.SOUTH, ctlPan );

		this.getContentPane().add( content );
		}
	}
