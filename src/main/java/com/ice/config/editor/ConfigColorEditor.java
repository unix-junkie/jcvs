
package com.ice.config.editor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.List;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.JTextComponent;

import com.ice.config.ConfigureEditor;
import com.ice.config.ConfigureSpec;
import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;


public
class		ConfigColorEditor
extends		ConfigureEditor
implements	FocusListener, ActionListener
	{
	private JTextField	rField;
	private JTextField	gField;
	private JTextField	bField;
	private JColorButton	color;

	public
	ConfigColorEditor()
		{
		super( "RGB Color" );
		}

	@Override
	public void
	edit( final UserPrefs prefs, final ConfigureSpec spec )
		{
		super.edit( prefs, spec );

		final Color color =
			prefs.getColor( spec.getPropertyName(), null );

		if ( color != null )
			{
			this.rField.setText( Integer.toString( color.getRed() ) );
			this.gField.setText( Integer.toString( color.getGreen() ) );
			this.bField.setText( Integer.toString( color.getBlue() ) );
			}
		else
			{
			this.rField.setText( "0" );
			this.gField.setText( "0" );
			this.bField.setText( "0" );
			}
		}

	@Override
	public void
	saveChanges( final UserPrefs prefs, final ConfigureSpec spec )
		{
		final String propName = spec.getPropertyName();

		try {
			final int r = Integer.parseInt( this.rField.getText() );
			final int g = Integer.parseInt( this.gField.getText() );
			final int b = Integer.parseInt( this.bField.getText() );

			final Color newVal = new Color( r, g, b );
			final Color oldVal = prefs.getColor( propName, Color.black );

			if ( ! newVal.equals( oldVal ) )
				{
				prefs.setColor( propName, newVal );
				}
			}
		catch ( final NumberFormatException ex )
			{
			ex.printStackTrace();
			}
		}

	@Override
	public void
	actionPerformed( final ActionEvent event )
		{
		final String cmdStr = event.getActionCommand();

		if ( cmdStr.equals( "COLORBUTTON" ) )
			{
			final Component cb = (Component) event.getSource();
			final Color c = cb.getBackground();
			this.rField.setText( Integer.toString( c.getRed() ) );
			this.gField.setText( Integer.toString( c.getGreen() ) );
			this.bField.setText( Integer.toString( c.getBlue() ) );
			this.color.setColor( c.getRed(), c.getGreen(), c.getBlue() );
			}
		}

	@Override
	public void
	requestInitialFocus()
		{
		this.rField.requestFocus();
		this.rField.selectAll();
		}

	private void
	computeColor()
		{
		try {
			final int red = Integer.parseInt( this.rField.getText() );
			final int green = Integer.parseInt( this.gField.getText() );
			final int blue = Integer.parseInt( this.bField.getText() );

			if ( red < 0 || red > 255 )
				{
				this.rField.setText( "0" );
				throw new NumberFormatException
					( "red value '" + red + "' is out of range" );
				}
			if ( green < 0 || green > 255 )
				{
				this.gField.setText( "0" );
				throw new NumberFormatException
					( "green value '" + green + "' is out of range" );
				}
			if ( blue < 0 || blue > 255 )
				{
				this.bField.setText( "0" );
				throw new NumberFormatException
					( "blue value '" + blue + "' is out of range" );
				}

			this.color.setColor( red, green, blue );
			}
		catch ( final NumberFormatException ex )
			{
			JOptionPane.showMessageDialog ( null,
				"one of the color fields is valid, " + ex.getMessage(),
				"Invalid Number", JOptionPane.ERROR_MESSAGE );
			}
		}

	@Override
	public void
	focusGained( final FocusEvent event )
		{
		this.computeColor();
		((JTextComponent) event.getComponent()).selectAll();
		}

	@Override
	public void
	focusLost( final FocusEvent event )
		{
		this.computeColor();
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

		JLabel lbl = new JLabel( "Red" );
		lbl.setBorder( new EmptyBorder( 1, 3, 1, 3 ) );
		AWTUtilities.constrain(
			result, lbl,
			GridBagConstraints.NONE,
			GridBagConstraints.WEST,
			col++, row, 1, 1, 0.0, 0.0 );

		this.rField = new JTextField( "0" );
		this.rField.addFocusListener( this );
		AWTUtilities.constrain(
			result, this.rField,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			col++, row++, 1, 1,  1.0, 0.0 );

		col = 0;
		lbl = new JLabel( "Green" );
		lbl.setBorder( new EmptyBorder( 1, 3, 1, 3 ) );
		AWTUtilities.constrain(
			result, lbl,
			GridBagConstraints.NONE,
			GridBagConstraints.WEST,
			col++, row, 1, 1, 0.0, 0.0 );

		this.gField = new JTextField( "0" );
		this.gField.addFocusListener( this );
		AWTUtilities.constrain(
			result, this.gField,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			col++, row++, 1, 1, 1.0, 0.0 );

		col = 0;
		lbl = new JLabel( "Blue" );
		lbl.setBorder( new EmptyBorder( 1, 3, 1, 3 ) );
		AWTUtilities.constrain(
			result, lbl,
			GridBagConstraints.NONE,
			GridBagConstraints.WEST,
			col++, row, 1, 1, 0.0, 0.0 );

		this.bField = new JTextField( "0" )
			{
			@Override
			public Component
			getNextFocusableComponent()
				{ return rField; }
			};
		this.bField.addFocusListener( this );
		AWTUtilities.constrain(
			result, this.bField,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			col++, row++, 1, 1, 1.0, 0.0 );

		this.color = new JColorButton(Color.red);
		AWTUtilities.constrain(
			result, this.color,
			GridBagConstraints.BOTH,
			GridBagConstraints.CENTER,
			2, 0, 1, 3, 1.0, 1.0 );

		final JComponent btnPan = new JPanel();
		btnPan.setLayout( new GridBagLayout() );
		btnPan.setBorder
				( new CompoundBorder
					( new TitledBorder
						( new EtchedBorder( EtchedBorder.RAISED ),
							"Color Table" ),
					new EmptyBorder( 3, 3, 3, 3 )
					)
				);

		AWTUtilities.constrain(
			result, btnPan,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.CENTER,
			0, row, 3, 1, 1.0, 0.0,
			new Insets( 5, 5, 5, 5 ) );

		row = col = 0;

		JColorButton cb;

		cb = new JColorButton(Color.black);
		cb.addActionListener( this );
		AWTUtilities.constrain(
			btnPan, cb,
			GridBagConstraints.NONE,
			GridBagConstraints.CENTER,
			col++, row, 1, 1, 0.0, 0.0 );
		cb = new JColorButton(Color.darkGray);
		cb.addActionListener( this );
		AWTUtilities.constrain(
			btnPan, cb,
			GridBagConstraints.NONE,
			GridBagConstraints.CENTER,
			col++, row, 1, 1, 0.0, 0.0 );
		cb = new JColorButton(
				new Color((Color.darkGray.getRed() + Color.gray.getRed()) / 2,
					  (Color.darkGray.getGreen() + Color.gray.getGreen()) / 2,
					  (Color.darkGray.getBlue() + Color.gray.getBlue()) / 2));
		cb.addActionListener( this );
		AWTUtilities.constrain(
			btnPan, cb,
			GridBagConstraints.NONE,
			GridBagConstraints.CENTER,
			col++, row, 1, 1, 0.0, 0.0 );
		cb = new JColorButton(Color.gray);
		cb.addActionListener( this );
		AWTUtilities.constrain(
			btnPan, cb,
			GridBagConstraints.NONE,
			GridBagConstraints.CENTER,
			col++, row, 1, 1, 0.0, 0.0 );
		cb = new JColorButton(
				new Color((Color.lightGray.getRed() + Color.gray.getRed()) / 2,
					  (Color.lightGray.getGreen() + Color.gray.getGreen()) / 2,
					  (Color.lightGray.getBlue() + Color.gray.getBlue()) / 2));
		cb.addActionListener( this );
		AWTUtilities.constrain(
			btnPan, cb,
			GridBagConstraints.NONE,
			GridBagConstraints.CENTER,
			col++, row, 1, 1, 0.0, 0.0 );
		cb = new JColorButton(Color.lightGray);
		cb.addActionListener( this );
		AWTUtilities.constrain(
			btnPan, cb,
			GridBagConstraints.NONE,
			GridBagConstraints.CENTER,
			col++, row, 1, 1, 0.0, 0.0 );
		cb = new JColorButton(
				new Color((Color.lightGray.getRed() + Color.white.getRed()) / 2,
					  (Color.lightGray.getGreen() + Color.white.getGreen()) / 2,
					  (Color.lightGray.getBlue() + Color.white.getBlue()) / 2));
		cb.addActionListener( this );
		AWTUtilities.constrain(
			btnPan, cb,
			GridBagConstraints.NONE,
			GridBagConstraints.CENTER,
			col++, row, 1, 1, 0.0, 0.0 );
		cb = new JColorButton(Color.white);
		cb.addActionListener( this );
		AWTUtilities.constrain(
			btnPan, cb,
			GridBagConstraints.NONE,
			GridBagConstraints.CENTER,
			col++, row, 1, 1, 0.0, 0.0 );

		++row;
		col = 0;

		cb = new JColorButton(Color.red);
		cb.addActionListener( this );
		AWTUtilities.constrain(
			btnPan, cb,
			GridBagConstraints.NONE,
			GridBagConstraints.CENTER,
			col++, row, 1, 1, 0.0, 0.0 );
		cb = new JColorButton(Color.blue);
		cb.addActionListener( this );
		AWTUtilities.constrain(
			btnPan, cb,
			GridBagConstraints.NONE,
			GridBagConstraints.CENTER,
			col++, row, 1, 1, 0.0, 0.0 );
		cb = new JColorButton(Color.green);
		cb.addActionListener( this );
		AWTUtilities.constrain(
			btnPan, cb,
			GridBagConstraints.NONE,
			GridBagConstraints.CENTER,
			col++, row, 1, 1, 0.0, 0.0 );
		cb = new JColorButton(Color.cyan);
		cb.addActionListener( this );
		AWTUtilities.constrain(
			btnPan, cb,
			GridBagConstraints.NONE,
			GridBagConstraints.CENTER,
			col++, row, 1, 1, 0.0, 0.0 );
		cb = new JColorButton(Color.magenta);
		cb.addActionListener( this );
		AWTUtilities.constrain(
			btnPan, cb,
			GridBagConstraints.NONE,
			GridBagConstraints.CENTER,
			col++, row, 1, 1, 0.0, 0.0 );
		cb = new JColorButton(Color.pink);
		cb.addActionListener( this );
		AWTUtilities.constrain(
			btnPan, cb,
			GridBagConstraints.NONE,
			GridBagConstraints.CENTER,
			col++, row, 1, 1, 0.0, 0.0 );
		cb = new JColorButton(Color.orange);
		cb.addActionListener( this );
		AWTUtilities.constrain(
			btnPan, cb,
			GridBagConstraints.NONE,
			GridBagConstraints.CENTER,
			col++, row, 1, 1, 0.0, 0.0 );
		cb = new JColorButton(Color.yellow);
		cb.addActionListener( this );
		AWTUtilities.constrain(
			btnPan, cb,
			GridBagConstraints.NONE,
			GridBagConstraints.CENTER,
			col++, row, 1, 1, 0.0, 0.0 );

		return result;
		}

	private static final
	class		JColorButton
	extends		JPanel
		{
		private int		red;
		private int		green;
		private int		blue;
		private final JButton	color;
		private final List listeners;

		private JColorButton(final Color c)
			{
			this( c.getRed(), c.getGreen(), c.getBlue() );
			}

		private JColorButton(final int r, final int g, final int b)
			{
			this.red = r;
			this.green = g;
			this.blue = b;

			this.listeners = new Vector();

			this.setLayout( new BorderLayout() );
			this.setBorder(
				new CompoundBorder(
					new EmptyBorder( 3, 3, 3, 3 ),
					new CompoundBorder(
						new BevelBorder( EtchedBorder.LOWERED ),
						new EmptyBorder( 1, 1, 1, 1 )
				) ) );

			this.color = new JButton( "" );
			this.color.setActionCommand( "COLORBUTTON" );
			this.color.setBackground( new Color( r, g, b ) );
			this.add( "Center", this.color );

			this.setMinimumSize( new Dimension( 36, 36 ) );
			this.setMaximumSize( new Dimension( 36, 36 ) );
			this.setPreferredSize( new Dimension( 36, 36 ) );
			}

		public void
		setColor( final Color c )
			{
			this.setColor( c.getRed(), c.getGreen(), c.getBlue() );
			}

		void
		setColor(final int r, final int g, final int b)
			{
			this.red = r;
			this.green = g;
			this.blue = b;

			this.color.setBackground( new Color( r, g, b ) );
			}

		public int
		getRed()
			{
			final Color c = this.color.getBackground();
			return c.getRed();
			}

		public int
		getGreen()
			{
			final Color c = this.color.getBackground();
			return c.getGreen();
			}

		public int
		getBlue()
			{
			final Color c = this.color.getBackground();
			return c.getBlue();
			}

		public synchronized void
		setActionCommand( final String cmd )
			{
			this.color.setActionCommand( cmd );
			}

		synchronized void
		addActionListener(final ActionListener listener)
			{
			this.color.addActionListener( listener );
			}

		public synchronized void
		removeActionListener( final ActionListener listener )
			{
			this.color.removeActionListener( listener );
			}
		}

	}

