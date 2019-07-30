
package com.ice.jcvsii;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import com.ice.cvsc.CVSProject;
import com.ice.cvsc.CVSProjectDef;
import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;


class		WorkBenchDetailPanel
extends		JPanel
implements	ActionListener, PropertyChangeListener
	{
	private JLabel		title;
	private JLabel		projectToke;

	private JLabel		repository;
	private JLabel		rootDirectory;
	private JLabel		localDirectory;

	private JLabel		userName;
	private JLabel		hostName;
	private JLabel		connectMethod;

		private JTextArea		descText;
	protected int			descOffset = 10;


	WorkBenchDetailPanel()
		{
		super();
		this.establishContents();
		}

	public void
	savePreferences()
		{
		//
		// Remove our property listeners.
		//
		final UserPrefs prefs = Config.getPreferences();

		prefs.removePropertyChangeListener
			( ConfigConstants.WB_DET_TITLE_BG, this );
		prefs.removePropertyChangeListener
			( ConfigConstants.WB_DET_TITLE_FONT, this );
		prefs.removePropertyChangeListener
			( ConfigConstants.WB_DET_TITLE_HEIGHT, this );
		}

	@Override
	public void
	actionPerformed( final ActionEvent event )
		{
		final String command = event.getActionCommand();

		if ( command.equals( "UNKNOWN" ) )
			{
			}
		}

	@Override
	public void
	propertyChange( final PropertyChangeEvent evt )
		{
		final String propName = evt.getPropertyName();

		final UserPrefs p = (UserPrefs) evt.getSource();
			switch (propName) {
			case ConfigConstants.WB_DET_TITLE_BG:
				title.setBackground(p.getColor
						(ConfigConstants.WB_DET_TITLE_BG,
						 title.getBackground()));
				break;
			case ConfigConstants.WB_DET_TITLE_HEIGHT:
				title.setPreferredSize
						(new Dimension(125, p.getInteger
								(ConfigConstants.WB_DET_TITLE_HEIGHT,
								 title.getSize().height)));
				title.revalidate();
				break;
			case ConfigConstants.WB_DET_TITLE_FONT:
				title.setFont(p.getFont
						(ConfigConstants.WB_DET_TITLE_FONT,
						 title.getFont()));
				title.revalidate();
				break;
			}

		title.repaint( 500 );
		}

	public void
	clearDefinition()
		{
		final String titleStr =
			ResourceMgr.getInstance().getUIString( "wb.detail.title" );
		this.title.setText( titleStr );

		this.projectToke.setText( " " );
		this.repository.setText( " " );
		this.rootDirectory.setText( " " );
		this.userName.setText( " " );
		this.hostName.setText( " " );
		this.connectMethod.setText( " " );
		this.localDirectory.setText( " " );
		this.descText.setText( " " );

		this.revalidate();
		this.repaint( 250 );
		}

	public void
	showDefinition( final WorkBenchDefinition def )
		{
		this.projectToke.setText( def.getName() );
		this.title.setText( def.getDisplayName() );

		this.descText.setText( def.getDescription() );

		if ( def.isFolder() )
			{
			this.localDirectory.setText( " " );
			this.repository.setText( " " );
			this.rootDirectory.setText( " " );
			this.userName.setText( " " );
			this.hostName.setText( " " );
			this.connectMethod.setText( " " );
			}
		else
			{
			this.localDirectory.setText( def.getLocalDirectory() );

			final String adminPath =
				CVSProject.rootPathToAdminPath
					( def.getLocalDirectory() );

			try {
				final CVSProjectDef projDef =
					CVSProjectDef.readDef( adminPath );

				this.repository.setText( projDef.getRepository() );
				this.rootDirectory.setText( projDef.getRootDirectory() );
				this.userName.setText( projDef.getUserName() );
				this.hostName.setText( projDef.getHostName() );
				this.connectMethod.setText( projDef.getConnectMethodString() );
				}
			catch ( final IOException ex )
				{
				final String errMsg =
					ResourceMgr.getInstance().getUIString
						( "wb.details.error.repos" );

				this.localDirectory.setText( def.getLocalDirectory() );
				this.repository.setText( errMsg );
				this.rootDirectory.setText( " " );
				this.userName.setText( " " );
				this.hostName.setText( " " );
				this.connectMethod.setText( " " );
				}
			}

		this.revalidate();
		this.repaint( 250 );
		}

	private void
	establishContents()
		{
		JLabel lbl;
		String lblStr;

		final UserPrefs prefs = Config.getPreferences();
		final ResourceMgr rmgr = ResourceMgr.getInstance();

		this.setLayout( new BorderLayout() );

		this.setMinimumSize( new Dimension( 175, 100 ) );
		this.setPreferredSize( new Dimension( 325, 125 ) );

		final String titleStr =
			ResourceMgr.getInstance().getUIString( "wb.detail.title" );
		this.title = new JLabel( titleStr, SwingConstants.LEFT );
		this.title.setOpaque( true );
		this.title.setForeground( Color.black );
		this.title.setPreferredSize
			( new Dimension
				( 125, prefs.getInteger
					( ConfigConstants.WB_DET_TITLE_HEIGHT, 35 ) ) );
		this.title.setBackground
			( prefs.getColor
				( ConfigConstants.WB_DET_TITLE_BG, new Color( 232, 232, 255 ) ) );
		this.title.setFont
			( prefs.getFont
				( ConfigConstants.WB_DET_TITLE_FONT,
					new Font( "SansSerif", Font.BOLD, 14 ) ) );
		this.title.setBorder
			( new CompoundBorder
				( new LineBorder( Color.black ),
					new EmptyBorder( 5, 5, 5, 5 ) ) );

		//
		// Create and add our property listeners.
		//

		prefs.addPropertyChangeListener
			( ConfigConstants.WB_DET_TITLE_BG, this );

		prefs.addPropertyChangeListener
			( ConfigConstants.WB_DET_TITLE_FONT, this );

		prefs.addPropertyChangeListener
			( ConfigConstants.WB_DET_TITLE_HEIGHT, this );


		final JComponent infoPan = new JPanel();
		infoPan.setLayout( new GridBagLayout() );
		infoPan.setBorder( new EmptyBorder( 9, 9, 9, 9 ) );

		int row = 0;

		lblStr = rmgr.getUIString( "wb.detail.token.lbl" );
		lbl = new JLabel( lblStr );
		AWTUtilities.constrain(
			infoPan, lbl,
			GridBagConstraints.NONE,
			GridBagConstraints.WEST,
			0, row, 1, 1, 0.0, 0.0 );

		this.projectToke = new DetailLabel("  ");
		AWTUtilities.constrain(
			infoPan, this.projectToke,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			1, row++, 1, 1, 1.0, 0.0,
			new Insets( 1, 3, 3, 1 ) );

		lblStr = rmgr.getUIString( "wb.detail.repos.lbl" );
		lbl = new JLabel( lblStr );
		AWTUtilities.constrain(
			infoPan, lbl,
			GridBagConstraints.NONE,
			GridBagConstraints.WEST,
			0, row, 1, 1, 0.0, 0.0 );

		this.repository = new DetailLabel("  ");
		AWTUtilities.constrain(
			infoPan, this.repository,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			1, row++, 1, 1, 1.0, 0.0,
			new Insets( 1, 3, 3, 1 ) );

		lblStr = rmgr.getUIString( "wb.detail.rootdir.lbl" );
		lbl = new JLabel( lblStr );
		AWTUtilities.constrain(
			infoPan, lbl,
			GridBagConstraints.NONE,
			GridBagConstraints.WEST,
			0, row, 1, 1, 0.0, 0.0 );

		this.rootDirectory = new DetailLabel("  ");
		AWTUtilities.constrain(
			infoPan, this.rootDirectory,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			1, row++, 1, 1, 1.0, 0.0,
			new Insets( 1, 3, 3, 1 ) );

		lblStr = rmgr.getUIString( "wb.detail.localdir.lbl" );
		lbl = new JLabel( lblStr );
		AWTUtilities.constrain(
			infoPan, lbl,
			GridBagConstraints.NONE,
			GridBagConstraints.WEST,
			0, row, 1, 1, 0.0, 0.0 );

		this.localDirectory = new DetailLabel("  ");
		AWTUtilities.constrain(
			infoPan, this.localDirectory,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			1, row++, 1, 1, 1.0, 0.0,
			new Insets( 1, 3, 3, 1 ) );

		lblStr = rmgr.getUIString( "wb.detail.user.lbl" );
		lbl = new JLabel( lblStr );
		AWTUtilities.constrain(
			infoPan, lbl,
			GridBagConstraints.NONE,
			GridBagConstraints.WEST,
			0, row, 1, 1, 0.0, 0.0 );

		this.userName = new DetailLabel("  ");
		AWTUtilities.constrain(
			infoPan, this.userName,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			1, row++, 1, 1, 1.0, 0.0,
			new Insets( 1, 3, 3, 1 ) );

		lblStr = rmgr.getUIString( "wb.detail.host.lbl" );
		lbl = new JLabel( lblStr );
		AWTUtilities.constrain(
			infoPan, lbl,
			GridBagConstraints.NONE,
			GridBagConstraints.WEST,
			0, row, 1, 1, 0.0, 0.0 );

		this.hostName = new DetailLabel("  ");
		AWTUtilities.constrain(
			infoPan, this.hostName,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			1, row++, 1, 1, 1.0, 0.0,
			new Insets( 1, 3, 3, 1 ) );

		lblStr = rmgr.getUIString( "wb.detail.method.lbl" );
		lbl = new JLabel( lblStr );
		AWTUtilities.constrain(
			infoPan, lbl,
			GridBagConstraints.NONE,
			GridBagConstraints.WEST,
			0, row, 1, 1, 0.0, 0.0 );

		this.connectMethod = new DetailLabel("  ");
		AWTUtilities.constrain(
			infoPan, this.connectMethod,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			1, row++, 1, 1, 1.0, 0.0,
			new Insets( 1, 3, 3, 1 ) );

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

		final String descTitle = rmgr.getUIString( "wb.detail.descpan.title" );
			final JComponent descPan = new JPanel();
		descPan.setLayout(new BorderLayout() );
		descPan.add(BorderLayout.CENTER, descText );
		descPan.setBorder
			( new CompoundBorder
				( new TitledBorder
					(	new EtchedBorder( EtchedBorder.RAISED ), descTitle ),
					new EmptyBorder( 10, 15, 15, 15 )
				)
			);

		AWTUtilities.constrain(
				infoPan, descPan,
				GridBagConstraints.BOTH,
				GridBagConstraints.SOUTH,
				0, row++, 2, 1, 1.0, 1.0,
				new Insets( 10, 5, 5, 5 ) );

		this.add( BorderLayout.NORTH, title );

		this.add( BorderLayout.CENTER, infoPan );
		}

	private static final
	class		DetailLabel
	extends		JLabel
		{
		private DetailLabel(final String text)
			{
			super( text );

			this.setOpaque( true );
			this.setBackground( new Color( 240, 240, 240 ) );
			this.setForeground( Color.black );
			this.setBorder
				( new CompoundBorder
					( new LineBorder( Color.darkGray ),
						new EmptyBorder( 1, 3, 1, 3 ) ) );
			}

		}

	}

