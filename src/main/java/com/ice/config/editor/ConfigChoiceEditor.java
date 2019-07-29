
package com.ice.config.editor;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EmptyBorder;

import com.ice.config.ConfigureEditor;
import com.ice.config.ConfigureSpec;
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

	@Override
	public void
	edit( final UserPrefs prefs, final ConfigureSpec spec )
		{
		super.edit( prefs, spec );

		this.radioPanel.removeAll();

		final String propName = spec.getPropertyName();

		final String choice = prefs.getProperty( propName, null );

		this.group = new ButtonGroup();

		final String[] choices = spec.getChoices();
		this.choiceButtons = new JRadioButton[ choices.length ];
		for ( int i = 0 ; i < choices.length ; ++i )
			{
			final JRadioButton radio = new JRadioButton( choices[i] );
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

	@Override
	public void
	saveChanges( final UserPrefs prefs, final ConfigureSpec spec )
		{
		final String propName = spec.getPropertyName();

		final String oldChoice = prefs.getProperty( propName, null );

		for ( final JRadioButton choiceButton : this.choiceButtons )
			{
			if ( choiceButton.isSelected() )
				{
				final String newChoice = choiceButton.getText();
				prefs.setProperty( propName, newChoice );
				break;
				}
			}
		}

	@Override
	public void
	requestInitialFocus()
		{
		}

	@Override
	protected JPanel
	createEditPanel()
		{
		final JPanel result = new JPanel();

		result.setLayout( new BoxLayout( result, BoxLayout.Y_AXIS ) );
		result.setBorder( new EmptyBorder( 5, 5, 5, 5 ) );

		return this.radioPanel = result;
		}

	}

