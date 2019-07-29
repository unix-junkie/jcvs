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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;


public
class		MessageDialog
extends		JDialog
implements	ActionListener
	{
	private String		messageString;
	private JTextArea	messageText;


	public
	MessageDialog( final Frame parent, final boolean modal, final String prompt )
		{
		super( parent, "Message Argument", modal );

		this.messageString = null;

		this.establishDialogContents( prompt );

		final Dimension sz = this.getPreferredSize();
		if ( sz.width < 360 ) sz.width = 360;		// UNDONE properties these!
		if ( sz.height < 240 ) sz.height = 240;
		this.setSize( sz );

		final Point location =
			AWTUtilities.centerDialogInParent( this, parent );

		this.setLocation( location.x, location.y );

		this.addWindowListener
			( new WindowAdapter()
				{
				@Override
				public void
				windowActivated( final WindowEvent evt )
					{ messageText.requestFocus(); }
				}
			);
		}

	public String
	getMessage()
		{
		return this.messageString;
		}

    @Override
    public void
    actionPerformed( final ActionEvent evt )
        {
	    final String command = evt.getActionCommand();

		if ( command.compareTo( "OK" ) == 0 )
			{
			this.messageString = this.messageText.getText();
			}
		else if ( command.compareTo( "CANCEL" ) == 0 )
			{
			this.messageString = null;
			}

		this.dispose();
        }

	public void
	establishDialogContents( final String prompt )
		{
		JButton		button;
		JPanel		controlPanel;

		final UserPrefs prefs = Config.getPreferences();
		final ResourceMgr rmgr = ResourceMgr.getInstance();

 		final JLabel promptLabel = new JLabel( prompt );
		promptLabel.setBorder( new EmptyBorder( 2, 2, 0, 0 ) );
		promptLabel.setFont(
			prefs.getFont(
				"messageDialog.prompt.font",
				new Font( "Dialog", Font.BOLD, 14 ) ) );

 		this.messageText = new JTextArea();
		this.messageText.setMargin( new Insets( 4, 4, 4, 4 ) );
		this.messageText.setEditable( true );
		this.messageText.setFont(
			prefs.getFont(
				"messageDialog.text.font",
				new Font( "Serif", Font.PLAIN, 14 ) ) );

		final JScrollPane scroller = new JScrollPane( this.messageText );

		controlPanel = new JPanel();
		controlPanel.setLayout( new GridLayout( 1, 2, 20, 20 ) );

		button = new JButton( rmgr.getUIString( "name.for.ok" ) );
		button.addActionListener( this );
		button.setActionCommand( "OK" );
		controlPanel.add( button );

		button = new JButton( rmgr.getUIString( "name.for.cancel" ) );
		button.addActionListener( this );
		button.setActionCommand( "CANCEL" );
		controlPanel.add( button );

		final Container content = this.getContentPane();
		content.setLayout( new BorderLayout() );

		final JPanel contPan = new JPanel();
		contPan.setLayout( new BorderLayout( 2, 2 ) );
		contPan.setBorder( new EmptyBorder( 3, 5, 5, 5 ) );
		content.add( BorderLayout.CENTER, contPan );

		final JPanel southPan = new JPanel();
		southPan.setLayout( new BorderLayout() );
		southPan.add( BorderLayout.EAST, controlPanel );
		southPan.setBorder( new EmptyBorder( 8, 0, 5, 0 ) );

		contPan.add( BorderLayout.NORTH, promptLabel );
		contPan.add( BorderLayout.CENTER, scroller );
		contPan.add( BorderLayout.SOUTH, southPan );
		}

	}

