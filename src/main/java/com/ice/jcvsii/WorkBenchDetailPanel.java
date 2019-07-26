
package com.ice.jcvsii;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.File;
import java.io.IOException;
import javax.swing.*;
import javax.swing.border.*;

import com.ice.cvsc.CVSProject;
import com.ice.cvsc.CVSProjectDef;
import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;


public
class		WorkBenchDetailPanel
extends		JPanel
implements	ActionListener, PropertyChangeListener
	{
	protected JLabel		title;
	protected JLabel		projectToke;

	protected JLabel		repository;
	protected JLabel		rootDirectory;
	protected JLabel		localDirectory;

	protected JLabel		userName;
	protected JLabel		hostName;
	protected JLabel		connectMethod;

	protected JPanel		descPan;
	protected JTextArea		descText;
	protected int			descOffset = 10;


	public
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
		UserPrefs prefs = Config.getPreferences();

		prefs.removePropertyChangeListener
			( Config.WB_DET_TITLE_BG, this );
		prefs.removePropertyChangeListener
			( Config.WB_DET_TITLE_FONT, this );
		prefs.removePropertyChangeListener
			( Config.WB_DET_TITLE_HEIGHT, this );
		}

	public void
	actionPerformed( ActionEvent event )
		{
		String command = event.getActionCommand();

		if ( command.equals( "UNKNOWN" ) )
			{
			}
		}

	public void
	propertyChange( PropertyChangeEvent evt )
		{
		String propName = evt.getPropertyName();

		UserPrefs p = (UserPrefs) evt.getSource();
		if ( propName.equals( Config.WB_DET_TITLE_BG ) )
			{
			title.setBackground( p.getColor
				( Config.WB_DET_TITLE_BG,
					title.getBackground() ) );
			}
		else if ( propName.equals( Config.WB_DET_TITLE_HEIGHT ) )
			{
			title.setPreferredSize
				( new Dimension( 125, p.getInteger
					( Config.WB_DET_TITLE_HEIGHT,
						title.getSize().height ) ) );
			title.revalidate();
			}
		else if ( propName.equals( Config.WB_DET_TITLE_FONT ) )
			{
			title.setFont( p.getFont
				( Config.WB_DET_TITLE_FONT,
					title.getFont() ) );
			title.revalidate();
			}

		title.repaint( 500 );
		}

	public void
	clearDefinition()
		{
		String titleStr =
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
	showDefinition( WorkBenchDefinition def )
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

			String adminPath =
				CVSProject.rootPathToAdminPath
					( def.getLocalDirectory() );

			try {
				CVSProjectDef projDef =
					CVSProjectDef.readDef( adminPath );

				this.repository.setText( projDef.getRepository() );
				this.rootDirectory.setText( projDef.getRootDirectory() );
				this.userName.setText( projDef.getUserName() );
				this.hostName.setText( projDef.getHostName() );
				this.connectMethod.setText( projDef.getConnectMethodString() );
				}
			catch ( IOException ex )
				{
				String errMsg =
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

		UserPrefs prefs = Config.getPreferences();
		ResourceMgr rmgr = ResourceMgr.getInstance();

		this.setLayout( new BorderLayout() );

		this.setMinimumSize( new Dimension( 175, 100 ) );
		this.setPreferredSize( new Dimension( 325, 125 ) );

		String titleStr =
			ResourceMgr.getInstance().getUIString( "wb.detail.title" );
		this.title = new JLabel( titleStr, JLabel.LEFT );
		this.title.setOpaque( true );
		this.title.setForeground( Color.black );
		this.title.setPreferredSize
			( new Dimension
				( 125, prefs.getInteger
					( Config.WB_DET_TITLE_HEIGHT, 35 ) ) );
		this.title.setBackground
			( prefs.getColor
				( Config.WB_DET_TITLE_BG, new Color( 232, 232, 255 ) ) );
		this.title.setFont
			( prefs.getFont
				( Config.WB_DET_TITLE_FONT,
					new Font( "SansSerif", Font.BOLD, 14 ) ) );
		this.title.setBorder
			( new CompoundBorder
				( new LineBorder( Color.black ),
					new EmptyBorder( 5, 5, 5, 5 ) ) );

		//
		// Create and add our property listeners.
		//

		prefs.addPropertyChangeListener
			( Config.WB_DET_TITLE_BG, this );

		prefs.addPropertyChangeListener
			( Config.WB_DET_TITLE_FONT, this );

		prefs.addPropertyChangeListener
			( Config.WB_DET_TITLE_HEIGHT, this );


		JPanel infoPan = new JPanel();
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

		this.projectToke = this.new DetailLabel( "  " );
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

		this.repository = this.new DetailLabel( "  " );
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

		this.rootDirectory = this.new DetailLabel( "  " );
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

		this.localDirectory = this.new DetailLabel( "  " );
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

		this.userName = this.new DetailLabel( "  " );
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

		this.hostName = this.new DetailLabel( "  " );
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

		this.connectMethod = this.new DetailLabel( "  " );
		AWTUtilities.constrain(
			infoPan, this.connectMethod,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			1, row++, 1, 1, 1.0, 0.0,
			new Insets( 1, 3, 3, 1 ) );

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

		String descTitle = rmgr.getUIString( "wb.detail.descpan.title" );
		this.descPan = new JPanel();
		this.descPan.setLayout( new BorderLayout() );
		this.descPan.add( BorderLayout.CENTER, descText );
		this.descPan.setBorder
			( new CompoundBorder
				( new TitledBorder
					(	new EtchedBorder( EtchedBorder.RAISED ), descTitle ),
					new EmptyBorder( 10, 15, 15, 15 )
				)
			);

		AWTUtilities.constrain(
			infoPan, this.descPan,
			GridBagConstraints.BOTH,
			GridBagConstraints.SOUTH,
			0, row++, 2, 1, 1.0, 1.0,
			new Insets( 10, 5, 5, 5 ) );

		this.add( BorderLayout.NORTH, title );

		this.add( BorderLayout.CENTER, infoPan );
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
			this.setBackground( new Color( 240, 240, 240 ) );
			this.setForeground( Color.black );
			this.setBorder
				( new CompoundBorder
					( new LineBorder( Color.darkGray ),
						new EmptyBorder( 1, 3, 1, 3 ) ) );
			}

		}

	}

