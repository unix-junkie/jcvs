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
import java.util.*;

import javax.swing.*;

import com.ice.cvsc.*;
import com.ice.pref.UserPrefs;


public
class		OutputFrame
extends		JFrame
implements	ActionListener
	{
	private ProjectFrame		projectFrame;

	private JTextArea			outputText;
	private JScrollPane			scroller;

	private boolean				isRedirecting;
	private File				redirectFile;
	private BufferedWriter		redirectWriter;

	private JMenuItem			endItem;
	private JMenuItem			beginItem;

	private String				fileDialogDefaultPath;


	public
	OutputFrame( ProjectFrame projectFrame, String title )
		{
		super( title );

		this.projectFrame = projectFrame;

		this.outputText = new JTextArea();
		this.scroller = new JScrollPane( this.outputText );

		this.isRedirecting = false;
		this.redirectFile = null;
		this.redirectWriter = null;

		this.endItem = null;
		this.beginItem = null;

		this.fileDialogDefaultPath = null;

		this.outputText.setEditable( false );
		this.outputText.setBackground( Color.white );
 		this.outputText.setFont
			( Config.getPreferences().getFont
				( Config.OUTPUT_WINDOW_FONT,
					new Font( "Monospaced", Font.PLAIN, 12 ) ) );

		Container content = this.getContentPane();

		content.setLayout( new BorderLayout( 0, 0 ) );

		content.add( "Center", this.scroller );

		this.establishMenuBar();

		this.addWindowListener(
			new WindowAdapter()
				{
				// We do not dispose here, as these are expensize windows that
				// we do not want to create and dispose of frequently. Only the
				// project frame that owns us can dispose of us!
				public void
					windowClosing( WindowEvent e )
						{ setVisible( false ); }

				public void
					windowClosed( WindowEvent e )
						{ windowBeingClosed(); }
				}
			);
		}

	// We are handed the prefs because we "belong" to somebody, and
	// they may have their own concept of preferences, like ProjectFrames.

	public void
	loadPreferences( Rectangle defBounds )
		{
		this.setBounds
			( this.projectFrame.getPreferences().getBounds
				( Config.OUTPUT_WINDOW_BOUNDS, defBounds ) );
		}

	public void
	savePreferences()
		{
		Rectangle bounds = this.getBounds();

		if ( bounds.x >= 0 && bounds.y >= 0
				&& bounds.width > 0 && bounds.height > 0 )
			{
			this.projectFrame.getPreferences().setBounds
				( Config.OUTPUT_WINDOW_BOUNDS, bounds );
			}
		}

	public void
	windowBeingClosed()
		{
		this.savePreferences();

		if ( this.isRedirecting
				&& this.redirectWriter != null )
			{
			try { this.redirectWriter.close(); }
				catch ( IOException ex ) { }
			}

		this.projectFrame.outputIsClosing();
		this.projectFrame = null; // remove references!
		}

	public String
	getText()
		{
		return this.outputText.getText();
		}

	public void
	setText( String newText )
		{
		if ( this.isRedirecting
				&& this.redirectWriter != null )
			{
			try {
				String lineSep =
					UserPrefs.getLineSeparator();

				this.redirectWriter.write( newText );
				if ( ! newText.endsWith( lineSep ) )
					{
					this.redirectWriter.write( lineSep );
					}
				}
			catch ( IOException ex )
				{
				this.endRedirection();
				this.outputText.setText
					( "*** ERROR writing to redirect file." );
				this.outputText.revalidate();
				this.outputText.repaint( 500 );
				}
			}
		else
			{
			this.outputText.setText( newText );
			this.outputText.revalidate();
			this.outputText.repaint( 500 );
			}
		}

    public void
    actionPerformed( ActionEvent evt )
        {
		String	subCmd;
	    String	command = evt.getActionCommand();

		if ( command.startsWith( "Hide" ) )
			{
			SwingUtilities.invokeLater
				( new Runnable()
					{
					public void run()
						{ setVisible( false ); }
					}
				);
			}
		else if ( command.startsWith( "Close" ) )
			{
			SwingUtilities.invokeLater
				( new Runnable()
					{
					public void run()
						{ dispose(); }
					}
				);
			}
		else if ( command.startsWith( "Show" ) )
			{
			SwingUtilities.invokeLater
				( new Runnable()
					{
					public void run()
						{
						projectFrame.setVisible( true );
						projectFrame.toFront();
						projectFrame.requestFocus();
						}
					}
				);
			}
		else if ( command.startsWith( "SaveToFile" ) )
			{
			SwingUtilities.invokeLater
				( new Runnable()
					{
					public void run()
						{ saveToFile(); }
					}
				);
			}
		else if ( command.startsWith( "Redirect" ) )
			{
			SwingUtilities.invokeLater
				( new Runnable()
					{
					public void run()
						{ redirectToFile(); }
					}
				);
			}
		else if ( command.startsWith( "EndRedirect" ) )
			{
			SwingUtilities.invokeLater
				( new Runnable()
					{
					public void run()
						{ endRedirection(); }
					}
				);
			}
		else if ( command.startsWith( "CopyText" ) )
			{
			this.outputText.copy();
			}
		else if ( command.startsWith( "SelectAll" ) )
			{
			this.outputText.selectAll();
			}
		}

	public void
	saveToFile()
		{
		FileDialog dialog = new
			FileDialog( this, "Save To File", FileDialog.SAVE );

		if ( this.fileDialogDefaultPath != null )
			dialog.setDirectory( this.fileDialogDefaultPath );

		dialog.show();

		String dirName = dialog.getDirectory();
		String fileName = dialog.getFile();
		
		if ( dirName != null && fileName != null )
			{
			File outF = null;
			this.fileDialogDefaultPath = dirName;

			try {
				outF = new File( dirName, fileName );

				PrintWriter out =
					new PrintWriter( new FileWriter( outF ) );

				BufferedReader rdr =
					new BufferedReader
						( new StringReader( this.outputText.getText() ) );

				for ( ; ; )
					{
					String ln = rdr.readLine();
					if ( ln == null )
						break;
					out.println( ln );
					}

				out.close();
				}
			catch ( IOException ex )
				{
				CVSUserDialog.Error
					( "Could not save text to file '"
						+ outF.getPath() + "':\n   "
						+ ex.getMessage() );
				}
			}
		}

	public void
	redirectToFile()
		{
		if ( this.isRedirecting )
			{
			CVSUserDialog.Note
				( "Output is already redirected to '"
					+ this.redirectFile.getPath() + "'" );

			return;
			}
		
		FileDialog dialog = new
			FileDialog( this, "Redirect File", FileDialog.SAVE );

		if ( this.fileDialogDefaultPath != null )
			dialog.setDirectory( this.fileDialogDefaultPath );

		dialog.show();

		String dirName = dialog.getDirectory();
		String fileName = dialog.getFile();
		
		if ( dirName != null && fileName != null )
			{
			this.fileDialogDefaultPath = dirName;

			try {
				this.isRedirecting = true;
				this.redirectFile = new File( dirName, fileName );
				this.redirectWriter =
					new BufferedWriter
						( new FileWriter( this.redirectFile ) );
				}
			catch ( IOException ex )
				{
				CVSUserDialog.Error
					( "Could not redirect to file '"
						+ this.redirectFile.getPath() + "'.\n"
						+ ex.getMessage() );

				this.redirectFile = null;
				this.redirectWriter = null;
				this.isRedirecting = false;
				}

			if ( this.isRedirecting )
				{
				this.beginItem.setEnabled( false );
				this.endItem.setEnabled( true );
				this.outputText.setText
					( "Redirecting to file '"
						+ this.redirectFile.getPath() + "'..." );
				}
			}
		}

	public void
	endRedirection()
		{
		if ( ! this.isRedirecting )
			return;

		if ( this.redirectWriter != null )
			{
			try { this.redirectWriter.close(); }
			catch ( IOException ex )
				{
				CVSUserDialog.Error
					( "Failed closing redirect file '"
						+ this.redirectFile.getPath()
						+ "'.\n" + ex.getMessage() );
				}
			}

		this.outputText.setText
			( "Redirection to file ended." );

        this.beginItem.setEnabled( true );
        this.endItem.setEnabled( false );

		this.redirectFile = null;
		this.redirectWriter = null;
		this.isRedirecting = false;
		}

	private void
	establishMenuBar()
		{
		JMenuItem		mItem;

		JMenuBar mBar = new JMenuBar();

		JMenu mFile = new JMenu( "File", true );
		mBar.add( mFile );

		mItem = new JMenuItem( "Show Project" );
		mFile.add( mItem );
		mItem.addActionListener( this );
		mItem.setActionCommand( "Show" );
		mItem.setAccelerator
			( KeyStroke.getKeyStroke
				( KeyEvent.VK_P, Event.CTRL_MASK ) );


		mFile.addSeparator();

		mItem = new JMenuItem( "Save To File..." );
		mFile.add( mItem );
		mItem.addActionListener( this );
		mItem.setActionCommand( "SaveToFile" );
		mItem.setAccelerator
			( KeyStroke.getKeyStroke
				( KeyEvent.VK_S, Event.CTRL_MASK ) );

		mFile.addSeparator();

		mItem = new JMenuItem( "Redirect To File..." );
		mFile.add( mItem );
		mItem.addActionListener( this );
		mItem.setActionCommand( "Redirect" );
		mItem.setAccelerator
			( KeyStroke.getKeyStroke
				( KeyEvent.VK_R, Event.CTRL_MASK ) );

		this.beginItem = mItem;

		mItem = new JMenuItem( "End File Redirection" );
		mFile.add( mItem );
		mItem.addActionListener( this );
		mItem.setActionCommand( "EndRedirect" );
		mItem.setEnabled( false );
		this.endItem = mItem;

		mFile.addSeparator();

		mItem = new JMenuItem( "Hide Window" );
		mFile.add( mItem );
		mItem.addActionListener( this );
		mItem.setActionCommand( "Hide" );
		mItem.setAccelerator
			( KeyStroke.getKeyStroke
				( KeyEvent.VK_W, Event.CTRL_MASK ) );

		JMenu mEdit = new JMenu( "Edit", true );
		mBar.add( mEdit );

		mItem = new JMenuItem( "Copy" );
		mEdit.add( mItem );
		mItem.addActionListener( this );
		mItem.setActionCommand( "CopyText" );
		mItem.setAccelerator
			( KeyStroke.getKeyStroke
				( KeyEvent.VK_C, Event.CTRL_MASK ) );

		mEdit.addSeparator();

		mItem = new JMenuItem( "Select All" );
		mEdit.add( mItem );
		mItem.addActionListener( this );
		mItem.setActionCommand( "SelectAll" );
		mItem.setAccelerator
			( KeyStroke.getKeyStroke
				( KeyEvent.VK_A, Event.CTRL_MASK ) );

		this.setJMenuBar( mBar );
		}

	}
