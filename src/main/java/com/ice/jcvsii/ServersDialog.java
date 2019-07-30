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
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
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
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;


class		ServersDialog
extends		JDialog
implements	ActionListener, ListSelectionListener
	{
	private JList<ServerDef>		serverList;

	private JTextArea			descText;
		private JLabel				userNameLbl;
	private JLabel				hostNameLbl;
	private JLabel				repositoryLbl;
	private JLabel				moduleLbl;
	private JLabel				connMethodLbl;

		private ServerDef			definition;


		ServersDialog( final Frame parent, final UserPrefs prefs, final ConnectInfoPanel info )
		{
		super( parent, "CVS Servers", true );

			this.definition = null;

		this.establishDialogContents
			( Config.getInstance().getServerDefinitions() );

		final Dimension sz = this.getPreferredSize();
		if ( sz.width < 480 ) sz.width = 480;
		if ( sz.height < 400 ) sz.height = 400;
		this.setSize( sz );

		final Point location =
			AWTUtilities.centerDialogInParent( this, parent );

		this.setLocation( location.x, location.y );

		this.addWindowListener(
			new WindowAdapter()
				{
				@Override
				public void
				windowActivated(final WindowEvent e)
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
	loadServerDefs( final UserPrefs prefs )
		{
		final Vector result = new Vector();

		result.addElement
			( new ServerDef
				( "Giant Java Tree", "pserver", "java",
					"anoncvs", "cvs.gjt.org", "/gjt/cvsroot",
					"This is the anonymous Giant Java Tree server.\n\n"
					+ "This definition allows anyone to checkout the "
					+ "Giant Java Tree, but not commit changes." ) );

		return result;
		}

	@Override
	public void
	valueChanged( final ListSelectionEvent evt )
		{
		if ( ! evt.getValueIsAdjusting() )
			{
			this.definition =
				this.serverList.getSelectedValue();

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

    @Override
    public void
    actionPerformed( final ActionEvent event )
        {
		boolean doDispose = false;

	    final String command = event.getActionCommand();

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

	private void
	establishDialogContents(final Vector<ServerDef> defs)
		{
		JLabel		lbl;

		final ResourceMgr rmgr = ResourceMgr.getInstance();

		final Container content = this.getContentPane();
		content.setLayout( new BorderLayout() );

		final JComponent mainPan = new JPanel();
		mainPan.setLayout( new BorderLayout() );
		mainPan.setBorder( new EmptyBorder( 5, 5, 5, 5 ) );

		this.serverList = new JList( defs );
		this.serverList.addListSelectionListener( this );

		final Component scroller = new JScrollPane(this.serverList );
		scroller.setPreferredSize( new Dimension( 150, 75 ) );

		final JComponent scrollerPanel = new JPanel();
		scrollerPanel.setLayout( new BorderLayout() );
		scrollerPanel.add( scroller );
		scrollerPanel.setBorder
			( new CompoundBorder
				( new EtchedBorder( EtchedBorder.RAISED ),
					new EmptyBorder( 2, 2, 2, 2 ) ) );

			final JComponent infoPanel = new JPanel();
		infoPanel.setLayout(new GridBagLayout() );
		infoPanel.setBorder(new EmptyBorder(5, 10, 0, 5 ) );

		int row = 0;

		lbl = new JLabel( rmgr.getUIString( "name.for.user.name" ) );
		AWTUtilities.constrain(
				infoPanel, lbl,
				GridBagConstraints.NONE,
				GridBagConstraints.WEST,
				0, row, 1, 1, 0.0, 0.0 );

		this.userNameLbl = new DetailLabel(" ");
		AWTUtilities.constrain(
				infoPanel, this.userNameLbl,
				GridBagConstraints.HORIZONTAL,
				GridBagConstraints.WEST,
				1, row++, 1, 1, 1.0, 0.0,
				new Insets( 1, 4, 2, 4 ) );

		lbl = new JLabel( rmgr.getUIString( "name.for.cvsserver" ) );
		AWTUtilities.constrain(
				infoPanel, lbl,
				GridBagConstraints.NONE,
				GridBagConstraints.WEST,
				0, row, 1, 1, 0.0, 0.0 );

		this.hostNameLbl = new DetailLabel(" ");
		AWTUtilities.constrain(
				infoPanel, this.hostNameLbl,
				GridBagConstraints.HORIZONTAL,
				GridBagConstraints.WEST,
				1, row++, 1, 1, 1.0, 0.0,
				new Insets( 1, 4, 2, 4 ) );

		lbl = new JLabel( rmgr.getUIString( "name.for.cvsrepos" ) );
		AWTUtilities.constrain(
				infoPanel, lbl,
				GridBagConstraints.NONE,
				GridBagConstraints.WEST,
				0, row, 1, 1, 0.0, 0.0 );

		this.repositoryLbl = new DetailLabel(" ");
		AWTUtilities.constrain(
				infoPanel, this.repositoryLbl,
				GridBagConstraints.HORIZONTAL,
				GridBagConstraints.WEST,
				1, row++, 1, 1, 1.0, 0.0,
				new Insets( 1, 4, 2, 4 ) );

		lbl = new JLabel( rmgr.getUIString( "name.for.cvsmodule" ) );
		AWTUtilities.constrain(
				infoPanel, lbl,
				GridBagConstraints.NONE,
				GridBagConstraints.WEST,
				0, row, 1, 1, 0.0, 0.0 );

		this.moduleLbl = new DetailLabel(" ");
		AWTUtilities.constrain(
				infoPanel, this.moduleLbl,
				GridBagConstraints.HORIZONTAL,
				GridBagConstraints.WEST,
				1, row++, 1, 1, 1.0, 0.0,
				new Insets( 1, 4, 2, 4 ) );

		lbl = new JLabel( rmgr.getUIString( "name.for.connect.method" ) );
		AWTUtilities.constrain(
				infoPanel, lbl,
				GridBagConstraints.NONE,
				GridBagConstraints.WEST,
				0, row, 1, 1, 0.0, 0.0 );

		this.connMethodLbl = new DetailLabel(" ");
		AWTUtilities.constrain(
				infoPanel, this.connMethodLbl,
				GridBagConstraints.HORIZONTAL,
				GridBagConstraints.WEST,
				1, row++, 1, 1, 1.0, 0.0,
				new Insets( 1, 4, 2, 4 ) );

		this.descText =
			new JTextArea()
				{
				@Override
				public boolean isFocusTraversable() { return false; }
				};

		this.descText.setEnabled( false );
		this.descText.setEditable( false );
		this.descText.setDisabledTextColor( Color.black );
		this.descText.setLineWrap( true );
		this.descText.setWrapStyleWord( true );
		this.descText.setOpaque( false );

			final JComponent descPan = new JPanel();
		descPan.setLayout(new BorderLayout() );
		descPan.add("Center", descText );
			final int descOffset = 15;
			descPan.setBorder(
			new CompoundBorder(
				new EmptyBorder(descOffset, 5, 0, 5 ),
				new CompoundBorder(
					new TitledBorder(
						new EtchedBorder( EtchedBorder.RAISED ),
						"Description" ),
					new EmptyBorder( 10, 10, 10, 10 )
			) ) );

		AWTUtilities.constrain(
				infoPanel, descPan,
				GridBagConstraints.BOTH,
				GridBagConstraints.SOUTH,
				0, row++, 2, 1, 1.0, 1.0 );

		mainPan.add(BorderLayout.CENTER, infoPanel);

		final Container ctlPan = new JPanel();
		ctlPan.setLayout( new BorderLayout() );

		final JComponent btnPan = new JPanel();
		btnPan.setLayout( new GridLayout( 1, 2, 20, 5 ) );
		btnPan.setBorder( new EmptyBorder( 5, 5, 5, 5 ) );

		final AbstractButton okBtn = new JButton(rmgr.getUIString("name.for.ok" ) );
		okBtn.addActionListener( this );
		okBtn.setActionCommand( "OK" );
		btnPan.add( okBtn );

		final AbstractButton canBtn = new JButton(rmgr.getUIString("name.for.cancel" ) );
		canBtn.addActionListener( this );
		canBtn.setActionCommand( "CANCEL" );
		btnPan.add( canBtn );

		final Component sep = new JSeparator(SwingConstants.HORIZONTAL );

		ctlPan.add( BorderLayout.NORTH, sep );
		ctlPan.add( BorderLayout.EAST, btnPan );

		mainPan.add( BorderLayout.WEST, scrollerPanel );

		content.add( BorderLayout.SOUTH, ctlPan );

		content.add( BorderLayout.CENTER, mainPan );
		}

	private static final
	class		DetailLabel
	extends		JLabel
		{
		private DetailLabel(final String text)
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
