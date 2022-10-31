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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collections;
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.TreeNode;

import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;


class		WorkBenchInfoDialog
extends		JDialog
implements	ActionListener
	{
	private final String			path;
	private final String			localRoot;
	private JTextField		nameField;
	private JTextField		displayField;
	private JTextArea		descField;

	private final boolean					isFolder;
	private WorkBenchDefinition		wDef;
	private final WorkBenchTreeNode		parentNode;


	WorkBenchInfoDialog
			( final Frame parFrame, final WorkBenchTreeNode parNode, final boolean isFolder,
				String defaultName, final String path, final String localRoot  )
		{
		super( parFrame, "WorkBench Definition", true );

		this.wDef = null;
		this.path = path;
		this.localRoot = localRoot;
		this.parentNode = parNode;
		this.isFolder = isFolder;

		// Attempt to compute a reasonable default token.
		if ( defaultName == null )
			{
			final long millis = System.currentTimeMillis();
			defaultName = (isFolder ? "F" : "P") + ( millis & 0x7FFFFF );
			}

		this.establishDialogContents( defaultName );

		this.pack();

		final Dimension sz = this.getSize();
		if ( sz.width < 480 ) sz.width = 480;
		if ( sz.height < 360 ) sz.height = 360;

		this.setSize( sz );

		this.setLocation
			( AWTUtilities.centerDialogInParent( this, parFrame ) );

		this.addWindowListener(
			new WindowAdapter()
				{
				@Override
				public void
				windowActivated( final WindowEvent e )
					{
					nameField.requestFocus();
					nameField.selectAll();
					}
				}
			);
		}

	public WorkBenchDefinition
	getWorkBenchDefinition()
		{
		return this.wDef;
		}

	private boolean
	checkName( final CharSequence name )
		{
		for ( int i = 0, sz = name.length() ; i < sz ; ++i )
			{
			final char ch = name.charAt(i);
			if ( ! Character.isLetterOrDigit( ch ) )
				return false;
			}

		return true;
		}

	private boolean
	checkUniqueness( final String name )
		{
		for ( final TreeNode node : Collections.list( this.parentNode.children() ) )
			{
			if ( ((WorkBenchTreeNode) node).getDefinition().getName().equals( name ) )
				return false;
			}

		return true;
		}

    @Override
    public void
    actionPerformed( final ActionEvent event )
        {
		boolean doDispose = false;

	    final String command = event.getActionCommand();

		if ( event.getSource() == this.nameField )
			{
			this.displayField.requestFocus();
			}
		else if ( event.getSource() == this.displayField )
			{
			this.descField.requestFocus();
			}
		else if ( command.compareTo( "OK" ) == 0 )
			{
			final ResourceMgr rmgr = ResourceMgr.getInstance();
			final String name = this.nameField.getText();
			if ( ! this.checkName( name ) )
				{
				final String msg = rmgr.getUIString( "wb.infodlg.invalid.name.msg" );
				final String title = rmgr.getUIString( "wb.infodlg.invalid.name.title" );
				JOptionPane.showMessageDialog
					( this, msg, title, JOptionPane.WARNING_MESSAGE );
				}
			else if ( ! this.checkUniqueness( name ) )
				{
				final String msg = rmgr.getUIString( "wb.infodlg.unique.name.msg" );
				final String title = rmgr.getUIString( "wb.infodlg.unique.name.title" );
				JOptionPane.showMessageDialog
					( this, msg, title, JOptionPane.WARNING_MESSAGE );
				}
			else
				{
				final String display = this.displayField.getText();
				final String desc = this.descField.getText();

					this.wDef = this.isFolder ? new WorkBenchDefinition
							(name, this.path, display, desc) : new WorkBenchDefinition
								    (name, this.path, display,
								     desc, this.localRoot);

				doDispose = true;
				}
			}
		else if ( command.compareTo( "CANCEL" ) == 0 )
			{
			this.wDef = null;
			doDispose = true;
			}

		if ( doDispose )
			{
			this.dispose();
			}
        }

	private void
	establishDialogContents(final String toke)
		{
		JLabel		label;
		final JButton		button;

		final UserPrefs prefs = Config.getPreferences();
		final ResourceMgr rmgr = ResourceMgr.getInstance();

		//
		// INFORMATION PANEL
		//
		final JComponent infoPan = new JPanel();
		infoPan.setLayout( new GridBagLayout() );
		infoPan.setBorder( new EmptyBorder( 4, 4, 4, 4 ) );

		final Font lblFont =
			prefs.getFont
				( "workBenchInfoDialog.label.font",
					new Font( "Dialog", Font.BOLD, 14 ) );

		int row = 0;

 		label = new JLabel( rmgr.getUIString( "wb.infodlg.brief.name" ) );
		label.setFont( lblFont );
 		AWTUtilities.constrain(
			infoPan, label,
			GridBagConstraints.NONE,
			GridBagConstraints.WEST,
			0, row++, 1, 1, 0.0, 0.0 );

 		this.nameField = new JTextField();
		this.nameField.setEditable( true );
		this.nameField.setText( toke == null ? "" : toke );
		this.nameField.addActionListener( this );
 		AWTUtilities.constrain(
			infoPan, this.nameField,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			0, row++, 1, 1, 1.0, 0.0 );

 		label = new JLabel( rmgr.getUIString( "wb.infodlg.display.name" ) );
 		label.setFont( lblFont );
		AWTUtilities.constrain(
			infoPan, label,
			GridBagConstraints.NONE,
			GridBagConstraints.WEST,
			0, row++, 1, 1, 0.0, 0.0 );

 		this.displayField = new JTextField();
		this.displayField.setEditable( true );
		this.displayField.setText( toke == null ? "" : toke );
		this.displayField.addActionListener( this );
 		AWTUtilities.constrain(
			infoPan, this.displayField,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			0, row++, 1, 1, 1.0, 0.0 );

 		label = new JLabel( rmgr.getUIString( "wb.infodlg.desc.name" ) );
 		label.setFont( lblFont );
		AWTUtilities.constrain(
			infoPan, label,
			GridBagConstraints.NONE,
			GridBagConstraints.WEST,
			0, row++, 2, 1, 0.0, 0.0 );

 		this.descField = new JTextArea();
		this.descField.setEditable( true );
		this.descField.setLineWrap( true );
		this.descField.setWrapStyleWord( true );
		this.descField.setBorder( new BevelBorder( BevelBorder.LOWERED ) );
 		AWTUtilities.constrain(
			infoPan, this.descField,
			GridBagConstraints.BOTH,
			GridBagConstraints.CENTER,
			0, row++, 2, 1, 1.0, 1.0 );


		//
		// CONTROL BUTTONS
		//
		final Container btnPan = new JPanel();
		btnPan.setLayout( new GridLayout( 1, 2, 5, 5 ) );

		final AbstractButton okBtn = new JButton(rmgr.getUIString("name.for.ok" ) );
		okBtn.addActionListener( this );
		okBtn.setActionCommand( "OK" );
		btnPan.add( okBtn );

		final AbstractButton canBtn = new JButton(rmgr.getUIString("name.for.cancel" ) );
		canBtn.addActionListener( this );
		canBtn.setActionCommand( "CANCEL" );
		btnPan.add( canBtn );

		final JComponent eastPan = new JPanel();
		eastPan.setLayout( new BorderLayout() );
		eastPan.setBorder( new EmptyBorder( 5, 5, 5, 5 ) );
		eastPan.add( BorderLayout.EAST, btnPan );

		final Container ctlPan = new JPanel();
		ctlPan.setLayout( new BorderLayout() );
		ctlPan.add( BorderLayout.NORTH, new JSeparator( SwingConstants.HORIZONTAL ) );
		ctlPan.add( BorderLayout.CENTER, eastPan );


		//
		// CONTENT LAYOUT
		//
		final Container content = this.getContentPane();
		content.setLayout( new BorderLayout() );

		content.add( BorderLayout.CENTER, infoPan );
		content.add( BorderLayout.SOUTH, ctlPan );
		}

	}
