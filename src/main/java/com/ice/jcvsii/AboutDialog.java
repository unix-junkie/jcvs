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

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.lang.*;
import javax.swing.*;
import javax.swing.border.*;

import com.ice.util.AWTUtilities;


/**
 * Shows the application's "About" dialog box.
 *
 * @version $Revision: 1.2 $
 * @author Timothy Gerard Endres, <a href="mailto:time@ice.com">time@ice.com</a>.
 */

public class
AboutDialog extends JDialog
		implements ActionListener
	{
	static public final String		RCS_ID = "$Id: AboutDialog.java,v 1.2 1999/04/01 19:41:10 time Exp $";
	static public final String		RCS_REV = "$Revision: 1.2 $";

	private String		messageString;
	private JTextArea	messageText;

	public
	AboutDialog( Frame parent )
		{
		super( parent, "jCVS II", true );

		this.messageString = null;

		this.establishDialogContents();

		this.pack();

		Dimension sz = this.getPreferredSize();

		Point location =
			AWTUtilities.computeDialogLocation
				( this, sz.width, sz.height );

		this.setLocation( location.x, location.y );
		}

    public void
    actionPerformed( ActionEvent evt )
        {
	    String command = evt.getActionCommand();

		if ( command.compareTo( "OK" ) == 0 )
			{
			this.dispose();
			}
        }

	public void
	establishDialogContents() 
		{
		JButton			button;

		Image img = null;
		try {
			img = AWTUtilities.getImageResource
				( "/com/ice/jcvsii/images/splash.gif" );
			}
		catch ( IOException ex )
			{
			ex.printStackTrace();
			}

		JLabel logoLabel = new JLabel( "jCVS II" );
		if ( img != null )
			{
			Icon icon = new ImageIcon( img );
			logoLabel = new JLabel( icon );
			}

		logoLabel.setBorder
			( new CompoundBorder
				( new EtchedBorder( EtchedBorder.LOWERED ),
					new EmptyBorder( 5, 5, 5, 5 ) ) );

 		this.messageText = new JTextArea();
		this.messageText.setOpaque( true );
		this.messageText.setEditable( false );
		this.messageText.setMargin( new Insets( 5, 5, 5, 5 ) );
		this.messageText.setFont( new Font( "Serif", Font.PLAIN, 12 ) );

		String[] fmtArgs = { JCVS.getVersionString() };

		ResourceMgr rmgr = ResourceMgr.getInstance();

		String msgStr =
			rmgr.getUIFormat( "about.dialog.text", fmtArgs );

		this.messageText.setText( msgStr );

		JScrollPane scroller = new JScrollPane();
		scroller.getViewport().add( this.messageText );

		JPanel ctlPan = new JPanel();
		ctlPan.setLayout( new BorderLayout() );

		button = new JButton( rmgr.getUIString( "name.for.ok" ) );
		button.addActionListener( this );
		button.setActionCommand( "OK" );
		ctlPan.add( BorderLayout.EAST, button );

		JPanel content = new JPanel();
		content.setLayout( new BorderLayout( 0, 8 ) );
		content.setBorder( new EmptyBorder( 6, 6, 6, 6 ) );

		content.add( BorderLayout.NORTH, logoLabel );
		content.add( BorderLayout.CENTER, scroller );
		content.add( BorderLayout.SOUTH, ctlPan );

		this.getContentPane().add( content );
		}
	}
