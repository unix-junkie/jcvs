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

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import com.ice.cvsc.CVSEntry;
import com.ice.cvsc.CVSIgnore;
import com.ice.cvsc.CVSEntryVector;
import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;


public
class		ReleaseDetailsDialog
extends		JDialog
implements	ActionListener
	{
	private boolean		okClicked;

	private Vector		adds;
	private Vector		mods;
	private Vector		rems;
	private Vector		unks;

	private JTextArea	detailsText;
	private JButton		okButton;


	public
	ReleaseDetailsDialog
			( Frame parent, Vector adds, Vector mods, Vector rems, Vector unks )
		{
		super( parent, "ReleaseDetails", true );

		this.okClicked = false;
		this.adds = adds;
		this.mods = mods;
		this.rems = rems;
		this.unks = unks;

		StringBuffer buf = new StringBuffer();

		for ( int i = adds.size()-1 ; i >= 0 ; --i )
			buf.append( "New  " + adds.elementAt(i) + "\n" );
		if ( adds.size() > 0 ) buf.append( "\n" );

		for ( int i = mods.size()-1 ; i >= 0 ; --i )
			buf.append( "Mod  " + mods.elementAt(i) + "\n" );
		if ( mods.size() > 0 ) buf.append( "\n" );

		for ( int i = rems.size()-1 ; i >= 0 ; --i )
			buf.append( "Rem  " + rems.elementAt(i) + "\n" );
		if ( rems.size() > 0 ) buf.append( "\n" );

		for ( int i = unks.size()-1 ; i >= 0 ; --i )
			buf.append( "Unk  " + unks.elementAt(i) + "\n" );

		this.establishDialogContents
			( "Details of Project Release", buf.toString() );

		this.pack();

		Dimension sz = this.getPreferredSize();
		if ( sz.width < 480 ) sz.width = 480;	// UNDONE properties these!
		if ( sz.height < 420 ) sz.height = 420;
		this.setSize( sz );

		Point location =
			AWTUtilities.centerDialogInParent( this, parent );

		this.setLocation( location.x, location.y );

		this.addWindowListener
			( new WindowAdapter()
				{
				public void
				windowActivated( WindowEvent evt )
					{ /* fileList.requestFocus(); */ }
				}
			);
		}

	public boolean
	clickedOk()
		{
		return this.okClicked;
		}

    public void
    actionPerformed( ActionEvent evt )
        {
	    String command = evt.getActionCommand();

		if ( command.compareTo( "OK" ) == 0 )
			{
			this.okClicked = true;
			SwingUtilities.invokeLater
				( new Runnable() { public void run() { dispose(); } } );
			}
		else if ( command.compareTo( "CANCEL" ) == 0 )
			{
			this.okClicked = false;
			SwingUtilities.invokeLater
				( new Runnable() { public void run() { dispose(); } } );
			}
        }

	public void
	establishDialogContents( String prompt, String details ) 
		{
		JButton		button;
		JPanel		controlPanel;

		UserPrefs prefs = Config.getPreferences();

 		JLabel promptLabel = new JLabel( prompt );
		promptLabel.setBorder( new EmptyBorder( 2, 2, 0, 0 ) );
		promptLabel.setFont(
			prefs.getFont(
				"releaseDialog.prompt.font",
				new Font( "Dialog", Font.BOLD, 14 ) ) );

		this.detailsText = new JTextArea();
		this.detailsText.setText( details );
		this.detailsText.setFont(
			prefs.getFont(
				"releaseDialog.details.font",
				new Font( "Monospaced", Font.PLAIN, 12 ) ) );

		JScrollPane scroller = new JScrollPane( this.detailsText );

		controlPanel = new JPanel();
		controlPanel.setLayout( new GridLayout( 1, 2, 20, 20 ) );

		ResourceMgr rmgr = ResourceMgr.getInstance();

		this.okButton = new JButton( rmgr.getUIString( "name.for.ok" ) );
		this.okButton.addActionListener( this );
		this.okButton.setActionCommand( "OK" );
		controlPanel.add( this.okButton );

		button = new JButton( rmgr.getUIString( "name.for.cancel" ) );
		button.addActionListener( this );
		button.setActionCommand( "CANCEL" );
		controlPanel.add( button );

		Container content = this.getContentPane();
		content.setLayout( new BorderLayout() );

		JPanel contPan = new JPanel();
		contPan.setLayout( new BorderLayout( 2, 2 ) );
		contPan.setBorder( new EmptyBorder( 3, 3, 3, 3 ) );

		contPan.add( BorderLayout.NORTH, promptLabel );
		contPan.add( BorderLayout.CENTER, scroller );
		contPan.add( BorderLayout.SOUTH, controlPanel );

		content.add( BorderLayout.CENTER, contPan );
		}

	}

