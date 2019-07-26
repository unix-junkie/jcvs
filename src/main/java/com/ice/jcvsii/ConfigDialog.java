/*
** Java CVS client application package.
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
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;

import com.ice.cvsc.CVSLog;
import com.ice.config.ConfigurePanel;
import com.ice.config.DefaultConfigureEditorFactory;
import com.ice.pref.UserPrefs;
import com.ice.util.ResourceUtilities;


/**
 * The Configuration Dialog.
 *
 * @version $Revision: 1.4 $
 * @author Timothy Gerard Endres,
 *  <a href="mailto:time@ice.com">time@ice.com</a>.
 */

public
class		ConfigDialog
extends		JDialog
implements	ActionListener
	{
	static public final String		RCS_ID = "$Id: ConfigDialog.java,v 1.4 2000/06/11 00:18:53 time Exp $";
	static public final String		RCS_REV = "$Revision: 1.4 $";

	private boolean				okClicked;
	private ConfigurePanel		configPan;

	
	public
	ConfigDialog( Frame parent, String title, UserPrefs prefs, UserPrefs specs )
		{
		super( parent, title, true );

		this.okClicked = false;

		Container content = this.getContentPane();

		content.setLayout( new BorderLayout() );

		DefaultConfigureEditorFactory factory =
			new DefaultConfigureEditorFactory( specs );

		factory.addEditor( "cmdexec", new ExecCommandEditor() );
		factory.addEditor( "srvrcmd", new ServerCommandEditor() );
		factory.addEditor( "plafcls", new LookAndFeelEditor() );

		this.configPan = new ConfigurePanel( prefs, specs, factory );

		content.add( BorderLayout.CENTER, this.configPan );
		
		JPanel buttons = new JPanel();
		buttons.setLayout( new GridLayout( 1, 2 ) );

		ResourceMgr rmgr = ResourceMgr.getInstance();

		JButton okBtn = new JButton( rmgr.getUIString( "name.for.save" ) );
		okBtn.addActionListener( this );
		okBtn.setActionCommand( "SAVE" );
		buttons.add( buttonPanel( okBtn ) );

		JButton canBtn = new JButton( rmgr.getUIString( "name.for.cancel" ) );
		canBtn.addActionListener( this );
		canBtn.setActionCommand( "CANCEL" );
		buttons.add( buttonPanel( canBtn ) );
		
		JPanel butPan = new JPanel();
		butPan.setLayout( new BorderLayout() );
		butPan.add( "East", buttons );

		JPanel southPan = new JPanel();
		southPan.setLayout( new BorderLayout() );
		southPan.add( BorderLayout.NORTH, new JSeparator( SwingConstants.HORIZONTAL ) );
		southPan.add( BorderLayout.CENTER, butPan );

		content.add( BorderLayout.SOUTH, southPan );

		this.pack();

		this.addWindowListener(
			new WindowAdapter()
			{
			public void windowActivated( WindowEvent evt )
				{
				configPan.setDividerLocation( 0.3 ); // UNDONE property this.
				}
			} );

		}

	private JPanel
	buttonPanel( JButton button )
		{
		JPanel panel = new JPanel();
		panel.setBorder(new EmptyBorder(4, 4, 4, 4));
		panel.setLayout(new BorderLayout());
		panel.add("Center", button);
		return panel;
		}

	public void
	editProperty( String prop )
		{
		this.configPan.editProperty( prop );
		}

	public void
	editProperties( String[] props )
		{
		this.configPan.editProperties( props );
		}

	public void
	editPath( String path )
		{
		this.configPan.editPath( path );
		}

	public void
	editPaths( String[] paths )
		{
		this.configPan.editPaths( paths );
		}

	public void
	actionPerformed( ActionEvent event )
		{
		boolean doExit = false;
		String command = event.getActionCommand();

		if ( command.equals( "SAVE" ) )
			{
			doExit = true;
			this.okClicked = true;
			this.configPan.saveCurrentEdit();
			this.configPan.commit();
			}
		else if ( command.equals( "CANCEL" ) )
			{
			doExit = true;
			}

		if ( doExit )
			{
			this.dispose();
			}
		}

	public boolean
	getOKClicked()
		{
		return this.okClicked;
		}

	}

