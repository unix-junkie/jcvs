
package com.ice.config.editor;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

import com.ice.config.*;
import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;


public
class		ConfigBooleanEditor
extends		ConfigureEditor
	{
	protected JRadioButton	tButton;
	protected JRadioButton	fButton;
	protected ButtonGroup	group;


	public
	ConfigBooleanEditor()
		{
		super( "Boolean" );
		}

	public void
	edit( UserPrefs prefs, ConfigureSpec spec )
		{
		super.edit( prefs, spec );

		boolean val =
			prefs.getBoolean( spec.getPropertyName(), false );

		if ( val )
			{
			this.tButton.setSelected( true );
			}
		else
			{
			this.fButton.setSelected( true );
			}
		}

	public void
	saveChanges( UserPrefs prefs, ConfigureSpec spec )
		{
		String propName = spec.getPropertyName();

		boolean newVal = this.tButton.isSelected();
		boolean oldVal = prefs.getBoolean( propName, false );

		if ( newVal != oldVal )
			{
			prefs.setBoolean( propName, newVal );
			}
		}

	public void
	requestInitialFocus()
		{
		this.tButton.requestFocus();
		}

	protected JPanel
	createEditPanel()
		{
		JPanel result = new JPanel();
		result.setLayout( new GridBagLayout() );
		result.setBorder( new EmptyBorder( 5, 25, 5, 5 ) );

		int col = 0;
		int row = 0;

		this.tButton = new JRadioButton( "True" );
		AWTUtilities.constrain(
			result, this.tButton,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			0, row++, 1, 1,  1.0, 0.0 );

		this.fButton = new JRadioButton( "False" );
		AWTUtilities.constrain(
			result, this.fButton,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			0, row++, 1, 1,  1.0, 0.0 );

		this.group = new ButtonGroup();
		this.group.add( this.tButton );
		this.group.add( this.fButton );

		return result;
		}

	}

