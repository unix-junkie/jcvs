
package com.ice.config.editor;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import com.ice.config.ConfigureEditor;
import com.ice.config.ConfigureSpec;
import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;


public
class		ConfigPointEditor
extends		ConfigureEditor
	{
	protected JTextField	xField;
	protected JTextField	yField;


	public
	ConfigPointEditor()
		{
		super( "Point" );
		}

	@Override
	public void
	edit( final UserPrefs prefs, final ConfigureSpec spec )
		{
		super.edit( prefs, spec );

		final Point pt =
			prefs.getPoint( spec.getPropertyName(), null );

		if ( pt != null )
			{
			this.xField.setText( Integer.toString( pt.x ) );
			this.yField.setText( Integer.toString( pt.y ) );
			}
		else
			{
			this.xField.setText( "0" );
			this.yField.setText( "0" );
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

			final Point newVal = new Point( x, y );
			final Point oldVal = prefs.getPoint( propName, new Point( 0, 0 ) );

			if ( ! newVal.equals( oldVal ) )
				{
				prefs.setPoint( propName, newVal );
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
			GridBagConstraints.WEST,
			col++, row, 1, 1, 0.0, 0.0 );

		this.xField = new JTextField( "0" );
		AWTUtilities.constrain(
			result, this.xField,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			col++, row, 1, 1,  1.0, 0.0 );

		lbl = new JLabel( "Y" );
		lbl.setBorder( new EmptyBorder( 1, 3, 1, 3 ) );
		AWTUtilities.constrain(
			result, lbl,
			GridBagConstraints.NONE,
			GridBagConstraints.WEST,
			col++, row, 1, 1, 0.0, 0.0 );

		this.yField = new JTextField( "0" );
		AWTUtilities.constrain(
			result, this.yField,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			col++, row++, 1, 1, 1.0, 0.0 );

		return result;
		}

	}

