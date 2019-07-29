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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;


public
class		PasswordDialog
extends		JDialog
implements	ActionListener
	{
	private String			userName;
	private String			password;
	private JTextField		userNameField;
	private JPasswordField	passwordField;

	public
	PasswordDialog( final Frame parent, final String userName )
		{
		super( parent, "Login Information", true );

		this.userName = userName;

		this.establishDialogContents( userName );

		this.pack();

		final Dimension sz = this.getSize();

		final Point location =
			AWTUtilities.computeDialogLocation
				( this, sz.width, sz.height );

		this.setLocation( location.x, location.y );

		this.addWindowListener(
			new WindowAdapter()
				{
				public void
				windowActivated(final WindowEvent e)
					{
					passwordField.requestFocus();
					}
				}
			);
		}

	public String
	getUserName()
		{
		return this.userName;
		}

	public String
	getPassword()
		{
		return this.password;
		}

    public void
    actionPerformed( final ActionEvent event )
        {
		boolean doDispose = true;

	    final String command = event.getActionCommand();

		if ( event.getSource() == this.passwordField )
			{
			this.userName = this.userNameField.getText();
			this.password =
				new String( this.passwordField.getPassword() );
			}
		else if ( event.getSource() == this.userNameField )
			{
			this.passwordField.requestFocus();
			this.passwordField.selectAll();
			doDispose = false;
			}
		else if ( command.compareTo( "OK" ) == 0 )
			{
			this.userName = this.userNameField.getText();
			this.password =
				new String( this.passwordField.getPassword() );
			}
		else if ( command.compareTo( "CANCEL" ) == 0 )
			{
			this.userName = null;
			this.password = null;
			}

		if ( doDispose )
			{
			this.dispose();
			}
        }

	public void
	establishDialogContents( final String userName )
		{
		JLabel		label;
		JButton		button;

		final UserPrefs prefs = Config.getPreferences();
		final ResourceMgr rmgr = ResourceMgr.getInstance();

		final JPanel mainPanel = new JPanel();
		mainPanel.setLayout( new GridBagLayout() );
		mainPanel.setBorder
			( new CompoundBorder
				( new EtchedBorder( EtchedBorder.LOWERED ),
					new EmptyBorder( 3, 3, 3, 3 ) ) );

		final Font lblFont =
			prefs.getFont
				( "passwordDialog.label.font",
					new Font( "Dialog", Font.BOLD, 14 ) );

 		label = new JLabel( rmgr.getUIString( "name.for.user.name" ) );
		label.setFont( lblFont );
 		AWTUtilities.constrain(
			mainPanel, label,
			GridBagConstraints.NONE,
			GridBagConstraints.WEST,
			0, 0, 1, 1, 1.0, 1.0,
			new Insets( 1, 3, 1, 5 ) );

 		this.userNameField = new JTextField( 16 );
		this.userNameField.setEditable( true );
		if ( userName != null )
			this.userNameField.setText( userName );
		this.userNameField.addActionListener( this );
 		AWTUtilities.constrain(
			mainPanel, this.userNameField,
			GridBagConstraints.NONE,
			GridBagConstraints.WEST,
			1, 0, 1, 1, 1.0, 1.0,
			new Insets( 10, 1, 5, 1 ) );

 		label = new JLabel( rmgr.getUIString( "name.for.user.pass" ) );
 		label.setFont( lblFont );
		AWTUtilities.constrain(
			mainPanel, label,
			GridBagConstraints.NONE,
			GridBagConstraints.WEST,
			0, 1, 1, 1, 1.0, 1.0,
			new Insets( 1, 3, 1, 5 ) );

 		this.passwordField = new JPasswordField( 16 );
		this.passwordField.setEditable( true );
		this.passwordField.setEchoChar( '*' );
		this.passwordField.addActionListener( this );
 		AWTUtilities.constrain(
			mainPanel, this.passwordField,
			GridBagConstraints.NONE,
			GridBagConstraints.WEST,
			1, 1, 1, 1, 1.0, 1.0,
			new Insets( 5, 1, 10, 1 ) );

		final JPanel controlPanel = new JPanel();
		controlPanel.setLayout( new GridLayout( 1, 2, 5, 5 ) );

		button = new JButton( rmgr.getUIString( "name.for.ok" ) );
		button.addActionListener( this );
		button.setActionCommand( "OK" );
		controlPanel.add( button );

		button = new JButton( rmgr.getUIString( "name.for.cancel" ) );
		button.addActionListener( this );
		button.setActionCommand( "CANCEL" );
		controlPanel.add( button );

		final JPanel southPan = new JPanel();
		southPan.setLayout( new BorderLayout() );
		southPan.add( BorderLayout.EAST, controlPanel );
		southPan.setBorder( new EmptyBorder( 12, 0, 2, 0 ) );

		final Container content = this.getContentPane();
		content.setLayout( new BorderLayout() );

		final JPanel contPan = new JPanel();
		contPan.setLayout( new BorderLayout( 2, 2 ) );
		contPan.setBorder( new EmptyBorder( 3, 5, 5, 5 ) );
		content.add( BorderLayout.CENTER, contPan );

		contPan.add( BorderLayout.CENTER, mainPanel );
		contPan.add( BorderLayout.SOUTH, southPan );
		}

	}
