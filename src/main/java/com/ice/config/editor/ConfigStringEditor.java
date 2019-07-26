
package com.ice.config.editor;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

import com.ice.config.*;
import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;


public
class		ConfigStringEditor
extends		ConfigureEditor
	{
	protected JTextField	strField;


	public
	ConfigStringEditor()
		{
		super( "String" );
		}

	public void
	requestInitialFocus()
		{
		this.strField.requestFocus();
		this.strField.selectAll();
		}

	protected JPanel
	createEditPanel()
		{
		JPanel result = new JPanel();
		result.setLayout( new GridBagLayout() );
		result.setBorder( new EmptyBorder( 5, 3, 3, 3 ) );

		int col = 0;
		int row = 0;

		this.strField = new JTextField( "" );
		AWTUtilities.constrain(
			result, this.strField,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			0, row++, 1, 1,  1.0, 0.0,
			new Insets( 3, 0, 0, 0 ) );

		return result;
		}

	public void
	edit( UserPrefs prefs, ConfigureSpec spec )
		{
		super.edit( prefs, spec );

		String val =
			prefs.getProperty( spec.getPropertyName(), null );

		if ( val != null )
			{
			this.strField.setText( val );
			}
		else
			{
			this.strField.setText( "" );
			}
		}

	public void
	saveChanges( UserPrefs prefs, ConfigureSpec spec )
		{
		String propName = spec.getPropertyName();

		String oldVal = prefs.getProperty( propName, "" );
		String newVal = this.strField.getText();

		if ( ! newVal.equals( oldVal ) )
			{
			prefs.setProperty( propName, newVal );
			}
		}

	}

