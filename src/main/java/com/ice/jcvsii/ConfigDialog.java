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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import com.ice.config.ConfigurePanel;
import com.ice.config.DefaultConfigureEditorFactory;
import com.ice.pref.UserPrefs;


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
	private final ConfigurePanel		configPan;


	public
	ConfigDialog( final Frame parent, final String title, final UserPrefs prefs, final UserPrefs specs )
		{
		super( parent, title, true );

		this.okClicked = false;

		final Container content = this.getContentPane();

		content.setLayout( new BorderLayout() );

		final DefaultConfigureEditorFactory factory =
			new DefaultConfigureEditorFactory( specs );

		factory.addEditor( "cmdexec", new ExecCommandEditor() );
		factory.addEditor( "srvrcmd", new ServerCommandEditor() );
		factory.addEditor( "plafcls", new LookAndFeelEditor() );

		this.configPan = new ConfigurePanel( prefs, specs, factory );

		content.add( BorderLayout.CENTER, this.configPan );

		final JPanel buttons = new JPanel();
		buttons.setLayout( new GridLayout( 1, 2 ) );

		final ResourceMgr rmgr = ResourceMgr.getInstance();

		final JButton okBtn = new JButton( rmgr.getUIString( "name.for.save" ) );
		okBtn.addActionListener( this );
		okBtn.setActionCommand( "SAVE" );
		buttons.add( buttonPanel( okBtn ) );

		final JButton canBtn = new JButton( rmgr.getUIString( "name.for.cancel" ) );
		canBtn.addActionListener( this );
		canBtn.setActionCommand( "CANCEL" );
		buttons.add( buttonPanel( canBtn ) );

		final JPanel butPan = new JPanel();
		butPan.setLayout( new BorderLayout() );
		butPan.add( "East", buttons );

		final JPanel southPan = new JPanel();
		southPan.setLayout( new BorderLayout() );
		southPan.add( BorderLayout.NORTH, new JSeparator( SwingConstants.HORIZONTAL ) );
		southPan.add( BorderLayout.CENTER, butPan );

		content.add( BorderLayout.SOUTH, southPan );

		this.pack();

		this.addWindowListener(
			new WindowAdapter()
			{
			@Override
			public void windowActivated( final WindowEvent evt )
				{
				configPan.setDividerLocation( 0.3 ); // UNDONE property this.
				}
			} );

		}

	private JPanel
	buttonPanel( final JButton button )
		{
		final JPanel panel = new JPanel();
		panel.setBorder(new EmptyBorder(4, 4, 4, 4));
		panel.setLayout(new BorderLayout());
		panel.add("Center", button);
		return panel;
		}

	public void
	editProperty( final String prop )
		{
		this.configPan.editProperty( prop );
		}

	public void
	editProperties( final String[] props )
		{
		this.configPan.editProperties( props );
		}

	public void
	editPath( final String path )
		{
		this.configPan.editPath( path );
		}

	public void
	editPaths( final String[] paths )
		{
		this.configPan.editPaths( paths );
		}

	public void
	actionPerformed( final ActionEvent event )
		{
		boolean doExit = false;
		final String command = event.getActionCommand();

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

