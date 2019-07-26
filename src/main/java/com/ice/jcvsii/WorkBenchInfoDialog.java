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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Enumeration;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;

import com.ice.cvsc.CVSProjectDef;
import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;


public
class		WorkBenchInfoDialog
extends		JDialog
implements	ActionListener
	{
	private String			path;
	private String			localRoot;
	private JTextField		nameField;
	private JTextField		displayField;
	private JTextArea		descField;

	private boolean					isFolder;
	private WorkBenchDefinition		wDef;
	private WorkBenchTreeNode		parentNode;


	public
	WorkBenchInfoDialog
			( Frame parFrame, WorkBenchTreeNode parNode, boolean isFolder,
				String defaultName, String path, String localRoot  )
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
			long millis = System.currentTimeMillis();
			defaultName = (isFolder ? "F" : "P") + ( millis & 0x7FFFFF );
			}

		this.establishDialogContents( defaultName );

		this.pack();

		Dimension sz = this.getSize();
		if ( sz.width < 480 ) sz.width = 480;
		if ( sz.height < 360 ) sz.height = 360;

		this.setSize( sz );

		this.setLocation
			( AWTUtilities.centerDialogInParent( this, parFrame ) );

		this.addWindowListener(
			new WindowAdapter()
				{
				public void
				windowActivated( WindowEvent e )
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
	checkName( String name )
		{
		for ( int i = 0, sz = name.length() ; i < sz ; ++i )
			{
			char ch = name.charAt(i);
			if ( ! Character.isLetterOrDigit( ch ) )
				return false;
			}

		return true;
		}

	private boolean
	checkUniqueness( String name )
		{
		Enumeration enum = this.parentNode.children();
		for ( ; enum.hasMoreElements() ; )
			{
			WorkBenchTreeNode node =
				(WorkBenchTreeNode) enum.nextElement();
			if ( node.getDefinition().getName().equals( name ) )
				return false;
			}

		return true;
		}

    public void
    actionPerformed( ActionEvent event )
        {
		boolean doDispose = false;

	    String command = event.getActionCommand();
		
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
			ResourceMgr rmgr = ResourceMgr.getInstance();
			String name = this.nameField.getText();
			if ( ! this.checkName( name ) )
				{
				String msg = rmgr.getUIString( "wb.infodlg.invalid.name.msg" );
				String title = rmgr.getUIString( "wb.infodlg.invalid.name.title" );
				JOptionPane.showMessageDialog
					( this, msg, title, JOptionPane.WARNING_MESSAGE );
				}
			else if ( ! this.checkUniqueness( name ) )
				{
				String msg = rmgr.getUIString( "wb.infodlg.unique.name.msg" );
				String title = rmgr.getUIString( "wb.infodlg.unique.name.title" );
				JOptionPane.showMessageDialog
					( this, msg, title, JOptionPane.WARNING_MESSAGE );
				}
			else
				{
				String display = this.displayField.getText();
				String desc = this.descField.getText();

				if ( this.isFolder )
					{
					this.wDef =
						new WorkBenchDefinition
							( name, this.path, display, desc );
					}
				else
					{
					this.wDef =
						new WorkBenchDefinition
							( name, this.path, display,
								desc, this.localRoot );
					}

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

	public void
	establishDialogContents( String toke ) 
		{
		JLabel		label;
		JButton		button;

		UserPrefs prefs = Config.getPreferences();
		ResourceMgr rmgr = ResourceMgr.getInstance();

		//
		// INFORMATION PANEL
		//
		JPanel infoPan = new JPanel();
		infoPan.setLayout( new GridBagLayout() );
		infoPan.setBorder( new EmptyBorder( 4, 4, 4, 4 ) );

		Font lblFont =
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
		JPanel btnPan = new JPanel();
		btnPan.setLayout( new GridLayout( 1, 2, 5, 5 ) );

		JButton okBtn = new JButton( rmgr.getUIString( "name.for.ok" ) );
		okBtn.addActionListener( this );
		okBtn.setActionCommand( "OK" );
		btnPan.add( okBtn );

		JButton canBtn = new JButton( rmgr.getUIString( "name.for.cancel" ) );
		canBtn.addActionListener( this );
		canBtn.setActionCommand( "CANCEL" );
		btnPan.add( canBtn );
		
		JPanel eastPan = new JPanel();
		eastPan.setLayout( new BorderLayout() );
		eastPan.setBorder( new EmptyBorder( 5, 5, 5, 5 ) );
		eastPan.add( BorderLayout.EAST, btnPan );

		JPanel ctlPan = new JPanel();
		ctlPan.setLayout( new BorderLayout() );
		ctlPan.add( BorderLayout.NORTH, new JSeparator( SwingConstants.HORIZONTAL ) );
		ctlPan.add( BorderLayout.CENTER, eastPan );


		//
		// CONTENT LAYOUT
		//
		Container content = this.getContentPane();
		content.setLayout( new BorderLayout() );

		content.add( BorderLayout.CENTER, infoPan );
		content.add( BorderLayout.SOUTH, ctlPan );
		}

	}
