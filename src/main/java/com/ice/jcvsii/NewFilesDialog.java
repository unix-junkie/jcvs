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
class		NewFilesDialog
extends		JDialog
implements	ActionListener, ListSelectionListener
	{
	private boolean		okClicked;

	private File		dirFile;
	private CVSEntry	dirEntry;
	private Vector		fileV;
	private JList		fileList;
	private JTextField	ignoreText;
	private JButton		okButton;
	private String		ignoreStr;


	public
	NewFilesDialog
			( Frame parent, boolean modal, String prompt )
		{
		super( parent, "New Files", modal );

		this.okClicked = false;
		this.dirFile = null;
		this.fileList = null;

		this.ignoreStr =
			Config.getPreferences().getProperty
				( Config.GLOBAL_USER_IGNORES, null );

		this.establishDialogContents( prompt );

		Dimension sz = this.getPreferredSize();
		if ( sz.width < 360 ) sz.width = 360;		// UNDONE properties these!
		if ( sz.height < 240 ) sz.height = 240;
		this.setSize( sz );

		Point location =
			AWTUtilities.centerDialogInParent( this, parent );

		this.setLocation( location.x, location.y );

		this.addWindowListener
			( new WindowAdapter()
				{
				public void
				windowActivated( WindowEvent evt )
					{ fileList.requestFocus(); }
				}
			);
		}

	public void
	refreshFileList( File dirF, CVSEntry dirEntry )
		{
		File ignFile;
		this.dirFile = dirF;
		this.dirEntry = dirEntry;

		this.fileV = new Vector();
		String[] files = this.dirFile.list();

		UserPrefs prefs = Config.getPreferences();

		CVSIgnore dirIgnore = new CVSIgnore();

		String ignoreName =
			prefs.getProperty( Config.GLOBAL_IGNORE_FILENAME, null );

		this.ignoreStr = this.ignoreText.getText();
		if ( this.ignoreStr != null )
			{
			dirIgnore.addIgnoreSpec( this.ignoreStr );
			}

		ignFile = new File( this.dirFile, ignoreName );
		if ( ignFile.exists() && ignFile.canRead() )
			{
			dirIgnore.addIgnoreFile( ignFile );
			}

		ignFile = new File( this.dirFile, ".cvsignore" );
		if ( ignFile.exists() )
			{
			dirIgnore.addIgnoreFile( ignFile );
			}

		for ( int i = 0 ; i < files.length ; ++i )
			{
			String fileName = files[i];

			File f = new File( this.dirFile, fileName );

			if ( ! f.isFile() || ! f.exists() || ! f.canRead() )
				continue;

			if ( fileName.equals( ignoreName ) )
				continue;

			if ( fileName.equals( ".cvsignore" ) )
				continue;

			if ( dirIgnore.isFileToBeIgnored( fileName ) )
				continue;

			CVSEntryVector eV = this.dirEntry.getEntryList();

			if ( this.dirEntry.locateEntry( fileName ) != null )
				continue;

			this.fileV.addElement( fileName );
			}

		this.fileList.setListData( this.fileV );
		}

	public String[]
	getSelectedFiles()
		{
		if ( this.okClicked )
			{
			Object[] items = this.fileList.getSelectedValues();
			String[] result = new String[ items.length ];
			if ( items.length >  0 )
				System.arraycopy( items, 0, result, 0, items.length );
			return result;
			}
		else
			{
			return new String[0];
			}
		}

    public void
    valueChanged( ListSelectionEvent evt )
        {
		if ( this.fileList.getSelectedIndex() == -1 )
			this.okButton.setEnabled( false );
		else
			this.okButton.setEnabled( true );
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
		else if ( command.compareTo( "CLEAR" ) == 0 )
			{
			SwingUtilities.invokeLater
				( new Runnable()
					{ public void run()
						{ fileList.clearSelection(); } } );
			}
        }

	public void
	establishDialogContents( String prompt ) 
		{
		JButton		button;
		JPanel		controlPanel;

		UserPrefs prefs = Config.getPreferences();

 		JLabel promptLabel = new JLabel( prompt );
		promptLabel.setBorder( new EmptyBorder( 2, 2, 0, 0 ) );
		promptLabel.setFont(
			prefs.getFont(
				"newFileDialog.prompt.font",
				new Font( "Dialog", Font.BOLD, 14 ) ) );

 		this.fileList = new JList();
		this.fileList.addListSelectionListener( this );
		this.fileList.setFont(
			prefs.getFont(
				"newFileDialog.text.font",
				new Font( "Dialog", Font.BOLD, 12 ) ) );

		JScrollPane scroller = new JScrollPane( this.fileList );

		JPanel ignorePanel = new JPanel();
		ignorePanel.setLayout( new BorderLayout() );
		ignorePanel.setBorder( new EmptyBorder( 1, 2, 8, 2 ) );

		JLabel ignoreLbl = new JLabel( "Ignore:" );
		ignoreLbl.setBorder( new EmptyBorder( 1, 1, 1, 4 ) );
		this.ignoreText = new JTextField( this.ignoreStr );
		this.ignoreText.addActionListener
			( new ActionListener()
				{
				public void
				actionPerformed( ActionEvent evt )
					{
					refreshFileList( dirFile, dirEntry );
					}
				} );

		ignorePanel.add( BorderLayout.WEST, ignoreLbl );
		ignorePanel.add( BorderLayout.CENTER, this.ignoreText );

		controlPanel = new JPanel();
		controlPanel.setLayout( new GridLayout( 1, 2, 20, 20 ) );

		ResourceMgr rmgr = ResourceMgr.getInstance();

		this.okButton = new JButton( rmgr.getUIString( "name.for.ok" ) );
		this.okButton.addActionListener( this );
		this.okButton.setActionCommand( "OK" );
		this.okButton.setEnabled( false );
		controlPanel.add( this.okButton );

		button = new JButton( rmgr.getUIString( "name.for.cancel" ) );
		button.addActionListener( this );
		button.setActionCommand( "CANCEL" );
		controlPanel.add( button );

		button = new JButton( rmgr.getUIString( "name.for.clear" ) );
		button.addActionListener( this );
		button.setActionCommand( "CLEAR" );
		controlPanel.add( button );

		Container content = this.getContentPane();
		content.setLayout( new BorderLayout() );

		JPanel contPan = new JPanel();
		contPan.setLayout( new BorderLayout( 2, 2 ) );
		contPan.setBorder( new EmptyBorder( 3, 3, 3, 3 ) );
		content.add( BorderLayout.CENTER, contPan );

		JPanel southPan = new JPanel();
		southPan.setLayout( new BorderLayout() );
		southPan.add( BorderLayout.NORTH, ignorePanel );
		southPan.add( BorderLayout.WEST, button );
		southPan.add( BorderLayout.EAST, controlPanel );
		southPan.setBorder( new EmptyBorder( 3, 0, 3, 0 ) );

		contPan.add( BorderLayout.NORTH, promptLabel );
		contPan.add( BorderLayout.CENTER, scroller );
		contPan.add( BorderLayout.SOUTH, southPan );
		}

	}

