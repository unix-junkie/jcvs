
package com.ice.config.editor;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import com.ice.config.ConfigureEditor;
import com.ice.config.ConfigureSpec;
import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;


public
abstract
class		ConfigNumberEditor
extends		ConfigureEditor
	{
	private JTextField	numField;


	ConfigNumberEditor(final String typeTitle)
		{
		super( typeTitle );
		}

	public abstract String
		getTypeTitle();

	protected abstract String
		formatNumber(UserPrefs prefs, ConfigureSpec spec);

	protected abstract boolean
		isChanged(UserPrefs prefs, ConfigureSpec spec, String numText);

	@Override
	public void
	edit( final UserPrefs prefs, final ConfigureSpec spec )
		{
		super.edit( prefs, spec );
		this.numField.setText( this.formatNumber( prefs, spec ) );
		}

	@Override
	public void
	saveChanges( final UserPrefs prefs, final ConfigureSpec spec )
		{
		final String numText = this.numField.getText();

		if ( this.isChanged( prefs, spec, numText ) )
			{
			prefs.setProperty( spec.getPropertyName(), numText );
			}
		}

	@Override
	public void
	requestInitialFocus()
		{
		this.numField.requestFocus();
		this.numField.selectAll();
		}

	@Override
	protected JPanel
	createEditPanel()
		{
		final JPanel result = new JPanel();
		result.setLayout( new GridBagLayout() );
		result.setBorder( new EmptyBorder( 5, 3, 3, 3 ) );

		int col = 0;
		final int row = 0;

		final JComponent lbl = new JLabel("Value" );
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

