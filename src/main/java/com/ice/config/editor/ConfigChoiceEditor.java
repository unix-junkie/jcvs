
package com.ice.config.editor;

import java.awt.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.border.*;

import com.ice.config.*;
import com.ice.pref.UserPrefs;


public
class		ConfigChoiceEditor
extends		ConfigureEditor
	{
	protected JPanel			radioPanel;
	protected ButtonGroup		group;
	protected JRadioButton[]	choiceButtons;


	public
	ConfigChoiceEditor()
		{
		super( "Choice" );
		}

	public void
	edit( UserPrefs prefs, ConfigureSpec spec )
		{
		super.edit( prefs, spec );

		this.radioPanel.removeAll();

		String propName = spec.getPropertyName();

		String choice = prefs.getProperty( propName, null );

		this.group = new ButtonGroup();

		String[] choices = spec.getChoices();
		this.choiceButtons = new JRadioButton[ choices.length ];
		for ( int i = 0 ; i < choices.length ; ++i )
			{
			JRadioButton radio = new JRadioButton( choices[i] );
			this.choiceButtons[i] = radio;
			this.group.add( radio );
			this.radioPanel.add( radio );
			radio.setSelected( false );
			if ( choice != null )
				{
				if ( choice.equals( choices[i] ) )
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
				String newChoice = this.choiceButtons[i].getText();
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

	}

