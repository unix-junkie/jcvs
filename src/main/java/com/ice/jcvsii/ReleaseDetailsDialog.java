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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;


class		ReleaseDetailsDialog
extends		JDialog
implements	ActionListener
	{
	private boolean		okClicked;


		ReleaseDetailsDialog
			( final Frame parent, final Vector adds, final Vector mods, final Vector rems, final Vector unks )
		{
		super( parent, "ReleaseDetails", true );

		this.okClicked = false;

			final StringBuilder buf = new StringBuilder();

		for ( int i = adds.size()-1 ; i >= 0 ; --i )
			buf.append("New  ").append(adds.elementAt(i)).append('\n');
		if (!adds.isEmpty()) buf.append('\n');

		for ( int i = mods.size()-1 ; i >= 0 ; --i )
			buf.append("Mod  ").append(mods.elementAt(i)).append('\n');
		if (!mods.isEmpty()) buf.append('\n');

		for ( int i = rems.size()-1 ; i >= 0 ; --i )
			buf.append("Rem  ").append(rems.elementAt(i)).append('\n');
		if (!rems.isEmpty()) buf.append('\n');

		for ( int i = unks.size()-1 ; i >= 0 ; --i )
			buf.append("Unk  ").append(unks.elementAt(i)).append('\n');

		this.establishDialogContents
			( "Details of Project Release", buf.toString() );

		this.pack();

		final Dimension sz = this.getPreferredSize();
		if ( sz.width < 480 ) sz.width = 480;	// UNDONE properties these!
		if ( sz.height < 420 ) sz.height = 420;
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
					{ /* fileList.requestFocus(); */ }
				}
			);
		}

	public boolean
	clickedOk()
		{
		return this.okClicked;
		}

    @Override
    public void
    actionPerformed( final ActionEvent evt )
        {
	    final String command = evt.getActionCommand();

		if ( command.compareTo( "OK" ) == 0 )
			{
			this.okClicked = true;
			SwingUtilities.invokeLater(this::dispose);
			}
		else if ( command.compareTo( "CANCEL" ) == 0 )
			{
			this.okClicked = false;
			SwingUtilities.invokeLater(this::dispose);
			}
        }

	private void
	establishDialogContents(final String prompt, final String details)
		{
		final JButton		button;
		final JPanel		controlPanel;

		final UserPrefs prefs = Config.getPreferences();

 		final JComponent promptLabel = new JLabel(prompt );
		promptLabel.setBorder( new EmptyBorder( 2, 2, 0, 0 ) );
		promptLabel.setFont(
			prefs.getFont(
				"releaseDialog.prompt.font",
				new Font( "Dialog", Font.BOLD, 14 ) ) );

			final JTextArea detailsText = new JTextArea();
		detailsText.setText(details );
		detailsText.setFont(
			prefs.getFont(
				"releaseDialog.details.font",
				new Font( "Monospaced", Font.PLAIN, 12 ) ) );

		final Component scroller = new JScrollPane(detailsText);

		controlPanel = new JPanel();
		controlPanel.setLayout( new GridLayout( 1, 2, 20, 20 ) );

		final ResourceMgr rmgr = ResourceMgr.getInstance();

			final AbstractButton okButton = new JButton(rmgr.getUIString("name.for.ok"));
		okButton.addActionListener(this );
		okButton.setActionCommand("OK" );
		controlPanel.add(okButton);

		button = new JButton( rmgr.getUIString( "name.for.cancel" ) );
		button.addActionListener( this );
		button.setActionCommand( "CANCEL" );
		controlPanel.add( button );

		final Container content = this.getContentPane();
		content.setLayout( new BorderLayout() );

		final JComponent contPan = new JPanel();
		contPan.setLayout( new BorderLayout( 2, 2 ) );
		contPan.setBorder( new EmptyBorder( 3, 3, 3, 3 ) );

		contPan.add( BorderLayout.NORTH, promptLabel );
		contPan.add( BorderLayout.CENTER, scroller );
		contPan.add( BorderLayout.SOUTH, controlPanel );

		content.add( BorderLayout.CENTER, contPan );
		}

	}

