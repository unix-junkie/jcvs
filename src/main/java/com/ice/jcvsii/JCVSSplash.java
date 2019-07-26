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
import java.io.IOException;

import javax.swing.*;
import javax.swing.border.*;

import com.ice.util.AWTUtilities;


public
class		JCVSSplash
extends		JWindow
implements	KeyListener, MouseListener, ActionListener 
	{
	protected JPanel			mainPan = null;
	protected JProgressBar		progress = null;
	protected BoundedRangeModel	progressModel = null;

	public
	JCVSSplash( String title, BoundedRangeModel progressModel )
		{
		super(/* title */);

		this.progressModel = progressModel;

		Image img = null;
		try {
			img = AWTUtilities.getImageResource
				( "/com/ice/jcvsii/images/splash.gif" );
			}
		catch ( IOException ex )
			{
			ex.printStackTrace();
			}

		Dimension sz = new Dimension( 480, 360 );

		JLabel lbl = new JLabel( "jCVS II" );
		if ( img != null )
			{
			this.prepareImage( img, this );
			MediaTracker tracker = new MediaTracker( this );
			tracker.addImage( img, 0 );
			try { tracker.waitForAll(); }
			catch ( InterruptedException ex )
				{
				System.err.println
					( "JCVSSplash: media tracker interrupted!\n"
						+ "   " + ex.getMessage() );
				}
			sz.width = img.getWidth(this) + 80;
			sz.height = img.getHeight(this) + 80;

			Icon icon = new ImageIcon( img );
			lbl = new JLabel( icon );
			}

		Dimension screen =
			Toolkit.getDefaultToolkit().getScreenSize();

		int x = ( screen.width - sz.width ) / 2;
		int y = ( screen.height - sz.height ) / 3;

		this.setBounds( x, y, sz.width, sz.height );

		Container content = this.getContentPane();
		this.mainPan = new JPanel();
		this.mainPan.setLayout( new BorderLayout() );
		this.mainPan.setBorder
			( new CompoundBorder
				( new BevelBorder( BevelBorder.RAISED ),
					new CompoundBorder
						( new EmptyBorder( 5, 5, 5, 5 ),
							new BevelBorder( BevelBorder.LOWERED ) ) ) );

		content.setLayout( new BorderLayout() );
		content.add( BorderLayout.CENTER, this.mainPan );

		this.mainPan.add( BorderLayout.CENTER, lbl );

		lbl.setBorder
			( new CompoundBorder
				( new EmptyBorder( 5, 5, 5, 5 ),
					new CompoundBorder
						( new EtchedBorder( EtchedBorder.LOWERED ),
							new EmptyBorder( 5, 5, 5, 5 ) ) ) );

		if ( progressModel != null )
			{
			this.progress = new JProgressBar( progressModel );
			JPanel pan = new JPanel();
			pan.setBorder(new EmptyBorder( 2, 5, 5, 5 ) );
			pan.setLayout( new BorderLayout() );
			pan.add( BorderLayout.CENTER, this.progress );
			this.mainPan.add( BorderLayout.SOUTH, pan );
			}

		this.addWindowListener(
			new WindowAdapter()
				{
				public void windowClosing( WindowEvent e )
					{ dispose(); System.exit(0); }
				public void windowClosed( WindowEvent e )
					{ }
				} 
			);
		} 

	public synchronized void
	enableDismissEvents()
		{
		// Listen for key strokes
		this.addKeyListener( this );

		// Listen for mouse events
		this.addMouseListener( this );
		}

	// Dismiss the window on a key press
	public void keyTyped(KeyEvent event) {}
	public void keyReleased(KeyEvent event) {}
	public void keyPressed(KeyEvent event)
		{
		this.dispose();
		}

	// Dismiss the window on a mouse click
	public void mousePressed(MouseEvent event) {}
	public void mouseReleased(MouseEvent event) {}
	public void mouseEntered(MouseEvent event) {}
	public void mouseExited(MouseEvent event) {}
	public void mouseClicked(MouseEvent event)
		{
		this.dispose();
		}

	// Dismiss the window on a timeout
	public void
	actionPerformed( ActionEvent event )
		{
		if ( this.progressModel != null )
			{
			this.progressModel.setValue
				( this.progressModel.getMaximum() );
			this.progress.repaint();
			}

		try { Thread.currentThread().sleep(1000); }
		catch ( InterruptedException ex ) {}
		this.dispose();
		}

	public static void
	main( String[] args )
		{
		DefaultBoundedRangeModel model =
			new DefaultBoundedRangeModel( 0, 0, 0, 100 );

		JCVSSplash splash = new JCVSSplash( "jCVS II", model );

//		splash.enableDismissEvents();

		splash.show();

		// Critical - this focus request must be here after
		// showing the parent to get keystrokes properly.
		splash.requestFocus();

		splash.addWindowListener
			( new WindowAdapter()
				{
				public void windowClosed( WindowEvent e )
					{ System.exit(0); }
				}
			);

		(splash.new Progressor( splash, model )).start();
		}

	private
	class		Progressor
	extends		Thread
		{
		JCVSSplash splash;
		BoundedRangeModel model;

		public
		Progressor( JCVSSplash s, BoundedRangeModel m )
			{
			super( "Model" );
			this.splash = s;
			this.model = m;
			}

		public void
		run()
			{
			try { this.sleep( 100 ); }
			catch ( InterruptedException ex ) {}

			for ( ; this.model.getValue() < this.model.getMaximum() ; )
				{
				this.model.setValue( this.model.getValue() + 10 );
				try { this.sleep( 1000 ); }
					catch ( InterruptedException ex ) {}
				}

			this.model.setValue( this.model.getMaximum() );

			this.splash.enableDismissEvents();

			try { this.sleep( 8000 ); }
				catch ( InterruptedException ex ) {}

			this.splash.dispose();
			}
		}
	}


