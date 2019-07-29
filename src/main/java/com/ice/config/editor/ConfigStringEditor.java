
package com.ice.config.editor;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import com.ice.config.ConfigureEditor;
import com.ice.config.ConfigureSpec;
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
		final JPanel result = new JPanel();
		result.setLayout( new GridBagLayout() );
		result.setBorder( new EmptyBorder( 5, 3, 3, 3 ) );

		final int col = 0;
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
	edit( final UserPrefs prefs, final ConfigureSpec spec )
		{
		super.edit( prefs, spec );

		final String val =
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
	saveChanges( final UserPrefs prefs, final ConfigureSpec spec )
		{
		final String propName = spec.getPropertyName();

		final String oldVal = prefs.getProperty( propName, "" );
		final String newVal = this.strField.getText();

		if ( ! newVal.equals( oldVal ) )
			{
			prefs.setProperty( propName, newVal );
			}
		}

	}

