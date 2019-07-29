
package com.ice.config.editor;

import java.awt.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.border.*;

import javax.swing.UIManager;

import com.ice.config.*;
import com.ice.pref.UserPrefs;


public
class		LookAndFeelEditor
extends		ConfigureEditor
	{
	protected JPanel			radioPanel;
	protected ButtonGroup		group;
	protected JRadioButton[]	choiceButtons;

	protected String[]			plafClassNames;
	protected String[]			plafDisplayNames;


	public
	LookAndFeelEditor()
		{
		super( "Look And Feel Class" );
		}

	public void
	edit( UserPrefs prefs, ConfigureSpec spec )
		{
		super.edit( prefs, spec );

		this.radioPanel.removeAll();

		String propName = spec.getPropertyName();

		String currPlaf = prefs.getProperty( propName, null );

		this.group = new ButtonGroup();

		this.getLookAndFeelInfo();
		String[] choices = this.plafDisplayNames;

		this.choiceButtons = new JRadioButton[ choices.length ];
		for ( int i = 0 ; i < this.plafDisplayNames.length ; ++i )
			{
			JRadioButton radio =
				new JRadioButton( this.plafDisplayNames[i] );

			this.choiceButtons[i] = radio;
			this.group.add( radio );
			this.radioPanel.add( radio );
			radio.setSelected( false );

			if ( currPlaf != null )
				{
				if ( currPlaf.equals( this.plafClassNames[i] ) )
					radio.setSelected( true );
				}
			else if ( i == 0 )
				{
				radio.setSelected( true );
				}
			}

		this.radioPanel.validate();
		this.radioPanel.repaint( 250 );
		}

	public void
	saveChanges( UserPrefs prefs, ConfigureSpec spec )
		{
		String propName = spec.getPropertyName();

		String oldChoice = prefs.getProperty( propName, null );

		for ( int i = 0 ; i < this.choiceButtons.length ; ++i )
			{
			if ( this.choiceButtons[i].isSelected() )
				{
				String newChoice = this.plafClassNames[i];
				prefs.setProperty( propName, newChoice );
				break;
				}
			}
		}

	public void
	requestInitialFocus()
		{
		}

	protected JPanel
	createEditPanel()
		{
		JPanel result = new JPanel();

		result.setLayout( new BoxLayout( result, BoxLayout.Y_AXIS ) );
		result.setBorder( new EmptyBorder( 5, 5, 5, 5 ) );

		return this.radioPanel = result;
		}

	private void
	getLookAndFeelInfo()
		{
		UIManager.LookAndFeelInfo[] lafi =
			UIManager.getInstalledLookAndFeels();

		this.plafClassNames = new String[ lafi.length + 1 ];
		this.plafDisplayNames = new String[ lafi.length + 1 ];

		this.plafClassNames[0] = "DEFAULT";
		this.plafDisplayNames[0] = "System Default";

		for ( int i = 0 ; i < lafi.length ; ++i )
			{
			this.plafClassNames[i+1] = lafi[i].getClassName();
			this.plafDisplayNames[i+1] = lafi[i].getName();
			}
		}

	}

