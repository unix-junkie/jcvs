
package com.ice.config.editor;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import com.ice.config.ConfigureEditor;
import com.ice.config.ConfigureSpec;
import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;


public
class		ConfigDimensionEditor
extends		ConfigureEditor
	{
	private JTextField	wField;
	private JTextField	hField;


	public
	ConfigDimensionEditor()
		{
		super( "Dimension" );
		}

	@Override
	public void
	edit( final UserPrefs prefs, final ConfigureSpec spec )
		{
		super.edit( prefs, spec );

		final Dimension dim =
			prefs.getDimension( spec.getPropertyName(), null );

		if ( dim != null )
			{
			this.wField.setText( Integer.toString( dim.width ) );
			this.hField.setText( Integer.toString( dim.height ) );
			}
		else
			{
			this.wField.setText( "0" );
			this.hField.setText( "0" );
			}
		}

	@Override
	public void
	saveChanges( final UserPrefs prefs, final ConfigureSpec spec )
		{
		final String propName = spec.getPropertyName();

		try {
			final int w = Integer.parseInt( this.wField.getText() );
			final int h = Integer.parseInt( this.hField.getText() );

			final Dimension newVal = new Dimension( w, h );
			final Dimension oldVal =
				prefs.getDimension( propName, new Dimension( 0, 0 ) );

			if ( ! newVal.equals( oldVal ) )
				{
				prefs.setDimension( propName, newVal );
				}
			}
		catch ( final NumberFormatException ex )
			{
			ex.printStackTrace();
			}
		}

	@Override
	public void
	requestInitialFocus()
		{
		this.wField.requestFocus();
		this.wField.selectAll();
		}

	@Override
	protected JPanel
	createEditPanel()
		{
		final JPanel result = new JPanel();
		result.setLayout( new GridBagLayout() );
		result.setBorder( new EmptyBorder( 5, 3, 3, 3 ) );

		int col = 0;
		int row = 0;

		JLabel lbl = new JLabel( "Width" );
		lbl.setBorder( new EmptyBorder( 1, 3, 1, 3 ) );
		AWTUtilities.constrain(
			result, lbl,
			GridBagConstraints.NONE,
			GridBagConstraints.WEST,
			col++, row, 1, 1, 0.0, 0.0 );

		this.wField = new JTextField( "0" );
		AWTUtilities.constrain(
			result, this.wField,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			col++, row, 1, 1,  1.0, 0.0 );

		lbl = new JLabel( "Height" );
		lbl.setBorder( new EmptyBorder( 1, 3, 1, 3 ) );
		AWTUtilities.constrain(
			result, lbl,
			GridBagConstraints.NONE,
			GridBagConstraints.WEST,
			col++, row, 1, 1, 0.0, 0.0 );

		this.hField = new JTextField( "0" );
		AWTUtilities.constrain(
			result, this.hField,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			col++, row++, 1, 1, 1.0, 0.0 );

		return result;
		}

	}

