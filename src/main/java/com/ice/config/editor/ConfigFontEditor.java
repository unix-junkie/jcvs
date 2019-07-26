
package com.ice.config.editor;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;

import com.ice.config.*;
import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;


public
class		ConfigFontEditor
extends		ConfigureEditor
implements	FocusListener, ItemListener, ChangeListener
	{
	protected JComboBox		fontName;
	protected JTextField	sizeField;
	protected JCheckBox		boldCheck;
	protected JCheckBox		italicCheck;
	protected JLabel		exLabel;


	public
	ConfigFontEditor()
		{
		super( "Text Font" );
		}

	protected Font
	getConfiguredFont()
		throws NumberFormatException
		{
		String name = (String) this.fontName.getSelectedItem();
		int size = Integer.parseInt( this.sizeField.getText() );
		int style = Font.PLAIN;
		if ( this.boldCheck.isSelected() )
			style |= Font.BOLD;
		if ( this.italicCheck.isSelected() )
			style |= Font.ITALIC;
		return new Font( name, style, size );
		}

	public void
	edit( UserPrefs prefs, ConfigureSpec spec )
		{
		super.edit( prefs, spec );

		Font font = prefs.getFont( spec.getPropertyName(), null );

		if ( font != null )
			{
			this.fontName.setSelectedItem( font.getName() );
			this.sizeField.setText( Integer.toString( font.getSize() ) );
			this.boldCheck.setSelected( font.isBold() );
			this.italicCheck.setSelected( font.isItalic() );
			}
		else
			{
			this.fontName.setSelectedItem( "Monospaced" );
			this.sizeField.setText( "12" );
			this.boldCheck.setSelected( false );
			this.italicCheck.setSelected( false );
			}
		}

	public void
	saveChanges( UserPrefs prefs, ConfigureSpec spec )
		{
		String propName = spec.getPropertyName();

		try {
			Font newVal = this.getConfiguredFont();

			Font oldVal =
				prefs.getFont
					( propName, new Font( "Serif", Font.PLAIN, 12) );

			if ( ! newVal.equals( oldVal ) )
				{
				prefs.setFont( propName, newVal );
				}
			}
		catch ( NumberFormatException ex )
			{
			JOptionPane.showMessageDialog ( null,
				"the font size field is valid, " + ex.getMessage(),
				"Invalid Size", JOptionPane.ERROR_MESSAGE ); 
			}
		}

	public void
	requestInitialFocus()
		{
		this.sizeField.requestFocus();
		this.sizeField.selectAll();
		}

	private void
	showConfiguredFont()
		{
		try {
			Font f = this.getConfiguredFont();
			this.exLabel.setFont( f );
			this.exLabel.repaint( 250 );
			}
		catch ( NumberFormatException ex )
			{
			JOptionPane.showMessageDialog ( null,
				"the font size field is valid, " + ex.getMessage(),
				"Invalid Size", JOptionPane.ERROR_MESSAGE ); 
			}
		}

	public void
	stateChanged( ChangeEvent event )
		{
		this.showConfiguredFont();
		}

	public void
	itemStateChanged( ItemEvent event )
		{
		this.showConfiguredFont();
		}

	public void
	focusGained( FocusEvent event )
		{
		this.showConfiguredFont();

		Component comp = event.getComponent();
		if ( comp instanceof JTextField )
			((JTextField) comp).selectAll();
		}

	public void
	focusLost( FocusEvent event )
		{
		this.showConfiguredFont();
		}

	protected JPanel
	createEditPanel()
		{
		JPanel result = new JPanel();
		result.setLayout( new GridBagLayout() );
		result.setBorder( new EmptyBorder( 5, 3, 3, 3 ) );

		int col = 0;
		int row = 0;

		JLabel lbl = new JLabel( "Font" );
		lbl.setBorder( new EmptyBorder( 1, 3, 1, 3 ) );
		AWTUtilities.constrain(
			result, lbl,
			GridBagConstraints.NONE,
			GridBagConstraints.WEST,
			col++, row, 1, 1, 0.0, 0.0 );

		this.fontName =
			new JComboBox
				( Toolkit.getDefaultToolkit().getFontList() );

		this.fontName.addItemListener( this );
		this.fontName.addFocusListener( this );
		AWTUtilities.constrain(
			result, this.fontName,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			col++, row++, 1, 1,  1.0, 0.0 );

		col = 0;
		lbl = new JLabel( "Size" );
		lbl.setBorder( new EmptyBorder( 1, 3, 1, 3 ) );
		AWTUtilities.constrain(
			result, lbl,
			GridBagConstraints.NONE,
			GridBagConstraints.WEST,
			col++, row, 1, 1, 0.0, 0.0 );

		this.sizeField = new JTextField( "0" );
		this.sizeField.addFocusListener( this );
		AWTUtilities.constrain(
			result, this.sizeField,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			col++, row++, 1, 1, 1.0, 0.0 );

		col = 0;

		JPanel chkPan = new JPanel();
		chkPan.setLayout( new GridBagLayout() );
		chkPan.setBorder( new EmptyBorder( 5, 5, 5, 5 ) );
		AWTUtilities.constrain(
			result, chkPan,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.CENTER,
			0, row++, 2, 1, 1.0, 0.0 );

		this.boldCheck = new JCheckBox( "Bold" );
		this.boldCheck.addFocusListener( this );
		this.boldCheck.addChangeListener( this );
		this.boldCheck.setHorizontalAlignment( SwingConstants.CENTER );

		this.italicCheck = new JCheckBox( "Italic" )
			{
			public Component
			getNextFocusableComponent()
				{ return fontName; }
			};
		this.italicCheck.addFocusListener( this );
		this.italicCheck.addChangeListener( this );
		this.italicCheck.setHorizontalAlignment( SwingConstants.CENTER );

		AWTUtilities.constrain(
			chkPan, this.boldCheck,
			GridBagConstraints.NONE,
			GridBagConstraints.CENTER,
			0, 0, 1, 1, 0.5, 0.0 );

		AWTUtilities.constrain(
			chkPan, this.italicCheck,
			GridBagConstraints.NONE,
			GridBagConstraints.CENTER,
			1, 0, 1, 1, 0.5, 0.0 );

		JPanel exPan = new JPanel();
		exPan.setLayout( new BorderLayout() );
		exPan.setBorder(
			new CompoundBorder(
				new EmptyBorder( 5, 5, 5, 5 ),
				new CompoundBorder(
					new EtchedBorder( EtchedBorder.RAISED ),
					new EmptyBorder( 5, 10, 5, 10 )
			) ) );

		this.exLabel = new JLabel( "Sample" );
		this.exLabel.setForeground( Color.black );
		this.exLabel.setHorizontalAlignment( SwingConstants.CENTER );

		exPan.add( "Center", this.exLabel );
		AWTUtilities.constrain(
			result, exPan,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.CENTER,
			0, row++, 2, 1, 1.0, 0.0 );

		return result;
		}

	}

