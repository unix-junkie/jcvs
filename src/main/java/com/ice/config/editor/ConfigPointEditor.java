
package com.ice.config.editor;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

import com.ice.config.*;
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

	public void
	edit( UserPrefs prefs, ConfigureSpec spec )
		{
		super.edit( prefs, spec );

		Point pt =
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

	public void
	saveChanges( UserPrefs prefs, ConfigureSpec spec )
		{
		String propName = spec.getPropertyName();

		try {
			int x = Integer.parseInt( this.xField.getText() );
			int y = Integer.parseInt( this.yField.getText() );

			Point newVal = new Point( x, y );
			Point oldVal = prefs.getPoint( propName, new Point( 0, 0 ) );

			if ( ! newVal.equals( oldVal ) )
				{
				prefs.setPoint( propName, newVal );
				}
			}
		catch ( NumberFormatException ex )
			{
			ex.printStackTrace();
			}
		}

	public void
	requestInitialFocus()
		{
		this.xField.requestFocus();
		this.xField.selectAll();
		}

	protected JPanel
	createEditPanel()
		{
		JPanel result = new JPanel();
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

