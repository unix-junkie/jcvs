
package com.ice.config.editor;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import com.ice.config.ConfigureEditor;
import com.ice.config.ConfigureSpec;
import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;


public
class		ConfigRectangleEditor
extends		ConfigureEditor
	{
	protected JTextField	xField;
	protected JTextField	yField;
	protected JTextField	wField;
	protected JTextField	hField;


	public
	ConfigRectangleEditor()
		{
		super( "Rectangle" );
		}

	@Override
	public void
	edit( final UserPrefs prefs, final ConfigureSpec spec )
		{
		super.edit( prefs, spec );

		final Rectangle rect =
			prefs.getBounds( spec.getPropertyName(), null );

		if ( rect != null )
			{
			this.xField.setText( Integer.toString( rect.x ) );
			this.yField.setText( Integer.toString( rect.y ) );
			this.wField.setText( Integer.toString( rect.width ) );
			this.hField.setText( Integer.toString( rect.height ) );
			}
		else
			{
			this.xField.setText( "0" );
			this.yField.setText( "0" );
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
			final int x = Integer.parseInt( this.xField.getText() );
			final int y = Integer.parseInt( this.yField.getText() );
			final int w = Integer.parseInt( this.wField.getText() );
			final int h = Integer.parseInt( this.hField.getText() );

			final Rectangle newVal = new Rectangle( x, y, w, h );
			final Rectangle oldVal =
				prefs.getBounds
					( propName, new Rectangle( 0, 0, 0, 0 ) );

			if ( ! newVal.equals( oldVal ) )
				{
				prefs.setBounds( propName, newVal );
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
		this.xField.requestFocus();
		this.xField.selectAll();
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

		JLabel lbl = new JLabel( "X" );
		lbl.setBorder( new EmptyBorder( 1, 3, 1, 3 ) );
		AWTUtilities.constrain(
			result, lbl,
			GridBagConstraints.NONE,
			GridBagConstraints.EAST,
			col++, row, 1, 1, 0.0, 0.0 );

		this.xField = new JTextField( "0" );
		AWTUtilities.constrain(
			result, this.xField,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			col++, row, 1, 1, 1.0, 0.0 );

		lbl = new JLabel( "Y" );
		lbl.setBorder( new EmptyBorder( 1, 3, 1, 3 ) );
		AWTUtilities.constrain(
			result, lbl,
			GridBagConstraints.NONE,
			GridBagConstraints.EAST,
			col++, row, 1, 1, 0.0, 0.0 );

		this.yField = new JTextField( "0" );
		AWTUtilities.constrain(
			result, this.yField,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			col++, row++, 1, 1, 1.0, 0.0 );

		col = 0;

		lbl = new JLabel( "Width" );
		lbl.setBorder( new EmptyBorder( 1, 3, 1, 3 ) );
		AWTUtilities.constrain(
			result, lbl,
			GridBagConstraints.NONE,
			GridBagConstraints.EAST,
			col++, row, 1, 1, 0.0, 0.0 );

		this.wField = new JTextField( "0" );
		AWTUtilities.constrain(
			result, this.wField,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			col++, row, 1, 1, 1.0, 0.0 );

		lbl = new JLabel( "Height" );
		lbl.setBorder( new EmptyBorder( 1, 3, 1, 3 ) );
		AWTUtilities.constrain(
			result, lbl,
			GridBagConstraints.NONE,
			GridBagConstraints.EAST,
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

