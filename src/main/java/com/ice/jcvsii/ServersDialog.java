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
import java.lang.*;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;

import com.ice.cvsc.CVSRequest;
import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;


public
class		ServersDialog
extends		JDialog
implements	ActionListener, ListSelectionListener
	{
	private JList				serverList;

	private JTextArea			descText;
	private JPanel				descPan;
	private JPanel				infoPanel;
	private JLabel				userNameLbl;
	private JLabel				hostNameLbl;
	private JLabel				repositoryLbl;
	private JLabel				moduleLbl;
	private JLabel				connMethodLbl;

	private UserPrefs			prefs;
	private ConnectInfoPanel	info;
	private ServerDef			definition;

	private int					descOffset = 15;


	public
	ServersDialog( Frame parent, UserPrefs prefs, ConnectInfoPanel info )
		{
		super( parent, "CVS Servers", true );

		this.info = info;
		this.prefs = prefs;
		this.definition = null;

		this.establishDialogContents
			( Config.getInstance().getServerDefinitions() );

		Dimension sz = this.getPreferredSize();
		if ( sz.width < 480 ) sz.width = 480;
		if ( sz.height < 400 ) sz.height = 400;
		this.setSize( sz );

		Point location =
			AWTUtilities.centerDialogInParent( this, parent );

		this.setLocation( location.x, location.y );

		this.addWindowListener(
			new WindowAdapter()
				{
				public void
				windowActivated(WindowEvent e)
					{
					}
				}
			);
		}

	public ServerDef
	getServerDefinition()
		{
		return this.definition;
		}

	public Vector
	loadServerDefs( UserPrefs prefs )
		{
		Vector result = new Vector();

		result.addElement
			( new ServerDef
				( "Giant Java Tree", "pserver", "java",
					"anoncvs", "cvs.gjt.org", "/gjt/cvsroot",
					"This is the anonymous Giant Java Tree server.\n\n"
					+ "This definition allows anyone to checkout the "
					+ "Giant Java Tree, but not commit changes." ) );
		
		return result;
		}

	public void
	valueChanged( ListSelectionEvent evt )
		{
		if ( ! evt.getValueIsAdjusting() )
			{
			this.definition =
				(ServerDef) this.serverList.getSelectedValue();

			if ( this.definition == null )
				{
				this.userNameLbl.setText( " " );
				this.hostNameLbl.setText( " " );
				this.repositoryLbl.setText( " " );
				this.moduleLbl.setText( " " );
				this.connMethodLbl.setText( " " );
				this.descText.setText( " " );
				}
			else
				{
				this.userNameLbl.setText
					( this.definition.getUserName() );
				this.hostNameLbl.setText
					( this.definition.getHostName() );
				this.repositoryLbl.setText
					( this.definition.getRepository() );
				this.moduleLbl.setText
					( this.definition.getModule() );
				this.connMethodLbl.setText
					( this.definition.getConnectMethodName() );
				this.descText.setText
					( this.definition.getDescription() );
				}

			this.repaint( 0 );
			}
		}

    public void
    actionPerformed( ActionEvent event )
        {
		boolean doDispose = false;

	    String command = event.getActionCommand();
		
		if ( command.compareTo( "OK" ) == 0 )
			{
			doDispose = true;
			}
		else if ( command.compareTo( "CANCEL" ) == 0 )
			{
			this.definition = null;
			doDispose = true;
			}

		if ( doDispose )
			{
			this.dispose();
			}
        }

	public void
	establishDialogContents( Vector defs ) 
		{
		JLabel		lbl;

		ResourceMgr rmgr = ResourceMgr.getInstance();

		Container content = this.getContentPane();
		content.setLayout( new BorderLayout() );

		JPanel mainPan = new JPanel();
		mainPan.setLayout( new BorderLayout() );
		mainPan.setBorder( new EmptyBorder( 5, 5, 5, 5 ) );

		this.serverList = new JList( defs );
		this.serverList.addListSelectionListener( this );

		JScrollPane scroller = new JScrollPane( this.serverList );
		scroller.setPreferredSize( new Dimension( 150, 75 ) );

		JPanel scrollerPanel = new JPanel();
		scrollerPanel.setLayout( new BorderLayout() );
		scrollerPanel.add( scroller );
		scrollerPanel.setBorder
			( new CompoundBorder
				( new EtchedBorder( EtchedBorder.RAISED ),
					new EmptyBorder( 2, 2, 2, 2 ) ) );

		this.infoPanel = new JPanel();
		this.infoPanel.setLayout( new GridBagLayout() );
		this.infoPanel.setBorder( new EmptyBorder( 5, 10, 0, 5 ) );

		int row = 0;

		lbl = new JLabel( rmgr.getUIString( "name.for.user.name" ) );
		AWTUtilities.constrain(
			this.infoPanel, lbl,
			GridBagConstraints.NONE,
			GridBagConstraints.WEST,
			0, row, 1, 1, 0.0, 0.0 );

		this.userNameLbl = this.new DetailLabel( " " );
		AWTUtilities.constrain(
			this.infoPanel, this.userNameLbl,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			1, row++, 1, 1, 1.0, 0.0,
			new Insets( 1, 4, 2, 4 ) );

		lbl = new JLabel( rmgr.getUIString( "name.for.cvsserver" ) );
		AWTUtilities.constrain(
			this.infoPanel, lbl,
			GridBagConstraints.NONE,
			GridBagConstraints.WEST,
			0, row, 1, 1, 0.0, 0.0 );

		this.hostNameLbl = this.new DetailLabel( " " );
		AWTUtilities.constrain(
			this.infoPanel, this.hostNameLbl,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			1, row++, 1, 1, 1.0, 0.0,
			new Insets( 1, 4, 2, 4 ) );

		lbl = new JLabel( rmgr.getUIString( "name.for.cvsrepos" ) );
		AWTUtilities.constrain(
			this.infoPanel, lbl,
			GridBagConstraints.NONE,
			GridBagConstraints.WEST,
			0, row, 1, 1, 0.0, 0.0 );

		this.repositoryLbl = this.new DetailLabel( " " );
		AWTUtilities.constrain(
			this.infoPanel, this.repositoryLbl,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			1, row++, 1, 1, 1.0, 0.0,
			new Insets( 1, 4, 2, 4 ) );

		lbl = new JLabel( rmgr.getUIString( "name.for.cvsmodule" ) );
		AWTUtilities.constrain(
			this.infoPanel, lbl,
			GridBagConstraints.NONE,
			GridBagConstraints.WEST,
			0, row, 1, 1, 0.0, 0.0 );

		this.moduleLbl = this.new DetailLabel( " " );
		AWTUtilities.constrain(
			this.infoPanel, this.moduleLbl,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			1, row++, 1, 1, 1.0, 0.0,
			new Insets( 1, 4, 2, 4 ) );

		lbl = new JLabel( rmgr.getUIString( "name.for.connect.method" ) );
		AWTUtilities.constrain(
			this.infoPanel, lbl,
			GridBagConstraints.NONE,
			GridBagConstraints.WEST,
			0, row, 1, 1, 0.0, 0.0 );

		this.connMethodLbl = this.new DetailLabel( " " );
		AWTUtilities.constrain(
			this.infoPanel, this.connMethodLbl,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			1, row++, 1, 1, 1.0, 0.0,
			new Insets( 1, 4, 2, 4 ) );

		this.descText =
			new JTextArea()
				{
				public boolean isFocusTraversable() { return false; }
				};

		this.descText.setEnabled( false );
		this.descText.setEditable( false );
		this.descText.setDisabledTextColor( Color.black );
		this.descText.setLineWrap( true );
		this.descText.setWrapStyleWord( true );
		this.descText.setOpaque( false );

		this.descPan = new JPanel();
		this.descPan.setLayout( new BorderLayout() );
		this.descPan.add( "Center", descText );
		this.descPan.setBorder(
			new CompoundBorder(
				new EmptyBorder( this.descOffset, 5, 0, 5 ),
				new CompoundBorder(
					new TitledBorder(
						new EtchedBorder( EtchedBorder.RAISED ),
						"Description" ),
					new EmptyBorder( 10, 10, 10, 10 )
			) ) );

		AWTUtilities.constrain(
			this.infoPanel, this.descPan,
			GridBagConstraints.BOTH,
			GridBagConstraints.SOUTH,
			0, row++, 2, 1, 1.0, 1.0 );

		mainPan.add( BorderLayout.CENTER, this.infoPanel );

		JPanel ctlPan = new JPanel();
		ctlPan.setLayout( new BorderLayout() );

		JPanel btnPan = new JPanel();
		btnPan.setLayout( new GridLayout( 1, 2, 20, 5 ) );
		btnPan.setBorder( new EmptyBorder( 5, 5, 5, 5 ) );

		JButton okBtn = new JButton( rmgr.getUIString( "name.for.ok" ) );
		okBtn.addActionListener( this );
		okBtn.setActionCommand( "OK" );
		btnPan.add( okBtn );

		JButton canBtn = new JButton( rmgr.getUIString( "name.for.cancel" ) );
		canBtn.addActionListener( this );
		canBtn.setActionCommand( "CANCEL" );
		btnPan.add( canBtn );

		JSeparator sep = new JSeparator( SwingConstants.HORIZONTAL );

		ctlPan.add( BorderLayout.NORTH, sep );
		ctlPan.add( BorderLayout.EAST, btnPan );

		mainPan.add( BorderLayout.WEST, scrollerPanel );

		content.add( BorderLayout.SOUTH, ctlPan );

		content.add( BorderLayout.CENTER, mainPan );
		}

	private
	class		DetailLabel
	extends		JLabel
		{
		public
		DetailLabel( String text )
			{
			super( text );

			this.setOpaque( true );
			this.setBackground( new Color( 250, 250, 250 ) );
			this.setForeground( Color.black );
			this.setBorder
				( new CompoundBorder
					( new LineBorder( Color.darkGray ),
						new EmptyBorder( 1, 3, 1, 3 ) ) );
			}

		}

	}
