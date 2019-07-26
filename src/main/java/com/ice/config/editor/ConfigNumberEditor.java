
package com.ice.config.editor;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

import com.ice.config.*;
import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;


public
abstract
class		ConfigNumberEditor
extends		ConfigureEditor
	{
	protected JTextField	numField;


	public
	ConfigNumberEditor( String typeTitle )
		{
		super( typeTitle );
		}

	abstract public String
		getTypeTitle();

	abstract public String
		formatNumber( UserPrefs prefs, ConfigureSpec spec );

	abstract public boolean
		isChanged( UserPrefs prefs, ConfigureSpec spec, String numText );

	public void
	edit( UserPrefs prefs, ConfigureSpec spec )
		{
		super.edit( prefs, spec );
		this.numField.setText( this.formatNumber( prefs, spec ) );
		}

	public void
	saveChanges( UserPrefs prefs, ConfigureSpec spec )
		{
		String numText = this.numField.getText();

		if ( this.isChanged( prefs, spec, numText ) )
			{
			prefs.setProperty( spec.getPropertyName(), numText );
			}
		}

	public void
	requestInitialFocus()
		{
		this.numField.requestFocus();
		this.numField.selectAll();
		}

	protected JPanel
	createEditPanel()
		{
		JPanel result = new JPanel();
		result.setLayout( new GridBagLayout() );
		result.setBorder( new EmptyBorder( 5, 3, 3, 3 ) );

		int col = 0;
		int row = 0;

		JLabel lbl = new JLabel( "Value" );
		lbl.setBorder( new EmptyBorder( 1, 3, 1, 3 ) );
		AWTUtilities.constrain(
			result, lbl,
			GridBagConstraints.NONE,
			GridBagConstraints.WEST,
			col++, row, 1, 1, 0.0, 0.0 );

		this.numField = new JTextField( "0" );
		AWTUtilities.constrain(
			result, this.numField,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			col++, row, 1, 1,  1.0, 0.0 );

		return result;
		}

	}

