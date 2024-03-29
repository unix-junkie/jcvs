
package com.ice.config;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.io.IOException;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import com.ice.pref.PrefsTupleTable;
import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;


public
abstract
class		ConfigureEditor
extends		JPanel
	{
	protected UserPrefs			prefs;
	protected ConfigureSpec		spec;

	private boolean			helpIsShowing;

	private JPanel			helpPanel;
	private JTextArea			helpText;
	private JButton			helpButton;

	private JPanel			editPanel;
	private JScrollPane		editScroller;

		private JPanel			descPan;
	private JTextArea			descText;
	protected int				descOffset = 25;


	public abstract void
		saveChanges( UserPrefs prefs, ConfigureSpec spec );

	public abstract void
		requestInitialFocus();

	protected abstract JPanel
		createEditPanel();


	protected ConfigureEditor(final String type)
		{
		super();
		this.establishContents( type );
		}

	public void
	edit( final UserPrefs prefs, final ConfigureSpec spec )
		{
		final String help = spec.getHelp();
		final String desc = spec.getDescription();

		if ( this.helpIsShowing )
			{
			this.toggleHelp();
			}

		if ( desc != null && !desc.isEmpty())
			{
			this.descText.setText( desc );
			this.descPan.setVisible( true );
			}
		else
			{
			this.descPan.setVisible( false );
			}

		this.descPan.revalidate();

		if ( help != null && !help.isEmpty())
			{
			this.helpButton.setEnabled( true );
			this.helpText.setText( help );
			this.helpText.revalidate();
			}
		else
			{
			this.helpButton.setEnabled( false );
			this.helpText.setText( "No Help Available." );
			}

		this.helpPanel.revalidate();
		}

	public void
	commit( final ConfigureSpec spec, final UserPrefs prefs, final UserPrefs orig )
		{
		if ( this.isModified( spec, prefs, orig ) )
			{
			this.commitChanges( spec, prefs, orig );
			}
		}

	/**
	 * This will commit the changes from prefs to orig. This method
	 * provides a number of default commits that will cover the majority
	 * of properties, and covers all of the default editors. You will
	 * need to override this method if your property type is not handled
	 * here.
	 */

	protected void
	commitChanges(final ConfigureSpec spec, final UserPrefs prefs, final UserPrefs orig)
		{
		final String propName = spec.getPropertyName();

		if ( this.isStringArray( spec ) )
			{
			final String[] strAry =
				prefs.getStringArray( propName, null );

			orig.removeStringArray( propName );
			if ( strAry != null )
				{
				orig.setStringArray( propName, strAry );
				}
			}
		else if ( this.isTupleTable( spec ) )
			{
			final PrefsTupleTable table =
				prefs.getTupleTable( propName, null );

			orig.removeTupleTable( propName );
			if ( table != null )
				{
				orig.setTupleTable( propName, table );
				}
			}
		else
			{
			final String value = prefs.getProperty( propName );
			orig.setProperty( propName, value );
			}
		}

	protected boolean
	isTupleTable(final ConfigureSpec spec)
		{
		return spec.isTupleTable();
		}

	protected boolean
	isStringArray(final ConfigureSpec spec)
		{
		return spec.isStringArray();
		}

	/**
	 * This will check for changes in prefs relative to orig. This method
	 * provides a number of default checks that will cover the majority
	 * of properties, and covers all of the default editors. You will
	 * need to override this method if your property type is not handled
	 * here.
	 */

	public boolean
	isModified( final ConfigureSpec spec, final UserPrefs prefs, final UserPrefs orig )
		{
		final String propName = spec.getPropertyName();

		if ( this.isTupleTable( spec ) )
			{
			final PrefsTupleTable nt =
				prefs.getTupleTable( propName, null );

			final PrefsTupleTable ot =
				orig.getTupleTable( propName, null );

				return nt != null && ot != null ? !nt.equals(ot) : nt != null || ot != null;
			}
		else if ( this.isStringArray( spec ) )
			{
			final String[] na =
				prefs.getStringArray( propName, null );
			final String[] oa =
				orig.getStringArray( propName, null );

			if ( na != null && oa != null )
				{
				if ( na.length != oa.length )
					return true;

				for ( int i = 0 ; i < na.length ; ++i )
					if ( ! na[i].equals( oa[i] ) )
						return true;
				}
			else return na != null || oa != null;
			}
		else
			{
			final String ns = prefs.getProperty( propName );
			final String os = orig.getProperty( propName );

				return ns != null && os != null ? !ns.equals(os) : ns != null || os != null;
			}

		return false;
		}

	/**
	 * Override for your own tip.
	 */
	private String
	getHelpButtonToolTipText()
		{
		return "Show Help Text";
		}

	private JPanel
	establishHelpPanel()
		{
		final JLabel lbl;

		final JPanel result = new JPanel();

		result.setLayout( new BorderLayout() );
		result.setBorder( new EmptyBorder( 5, 3, 3, 3 ) );

		this.helpText = new JTextArea();
		this.helpText.setEnabled( false );
		this.helpText.setEditable( false );
		this.helpText.setDisabledTextColor( Color.black );
		this.helpText.setLineWrap( true );
		this.helpText.setWrapStyleWord( true );
		this.helpText.setOpaque( false );
		this.helpText.setMargin( new Insets( 2, 4, 2, 4 ) );

		result.add( BorderLayout.CENTER, this.helpText );

		return result;
		}

	private void toggleHelp() {
		final JViewport editScrollerViewport = this.editScroller.getViewport();
		if (this.helpIsShowing) {
			editScrollerViewport.remove(this.helpPanel);
			editScrollerViewport.setView(this.editPanel);
		} else {
			editScrollerViewport.remove(this.editPanel);
			editScrollerViewport.setView(this.helpPanel);
		}
		this.editScroller.revalidate();

		this.repaint(50);
		this.helpIsShowing = !this.helpIsShowing;
	}

	private JPanel
	establishEditPanel( final String type )
		{
		final int col = 0;
		int row = 0;

		final JPanel result = new JPanel();
		result.setLayout( new GridBagLayout() );

			final JPanel editorPanel = this.createEditPanel();

		AWTUtilities.constrain(
				result, editorPanel,
				GridBagConstraints.BOTH,
				GridBagConstraints.CENTER,
				0, row++, 1, 1, 1.0, 1.0 );

		this.descText = new JTextArea( "" );
		this.descText.setEnabled( false );
		this.descText.setEditable( false );
		this.descText.setDisabledTextColor( Color.black );
		this.descText.setLineWrap( true );
		this.descText.setWrapStyleWord( true );
		this.descText.setOpaque( false );

		this.descPan = new JPanel();
		this.descPan.setLayout( new BorderLayout() );
		this.descPan.add( "Center", descText );
		this.descPan.setBorder
			( new CompoundBorder
				( new TitledBorder
					( new EtchedBorder( EtchedBorder.RAISED ), "Description" ),
					new EmptyBorder( 10, 15, 15, 15 )
				)
			);

		AWTUtilities.constrain(
			result, this.descPan,
			GridBagConstraints.BOTH,
			GridBagConstraints.SOUTH,
			0, row++, 1, 1, 1.0, 1.0,
			new Insets( this.descOffset, 5, 5, 5 ) );

		final Component fillerPan = new JPanel();
		AWTUtilities.constrain(
			result, fillerPan,
			GridBagConstraints.BOTH,
			GridBagConstraints.SOUTH,
			0, row++, 1, 1, 1.0, 1.0 );

		return result;
		}

	private void
	establishContents( final String type )
		{
		this.setLayout( new BorderLayout() );

		final Container typePan = new JPanel();
		typePan.setLayout( new GridBagLayout() );

		final JComponent lbl = new JLabel(type );
		lbl.setBorder( new EmptyBorder( 3, 3, 5, 3 ) );
		AWTUtilities.constrain(
			typePan, lbl,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			0, 0, 1, 1, 1.0, 0.0 );

		try {
			final Image iHelp =
				AWTUtilities.getImageResource
					( "/com/ice/config/images/icons/confighelp.gif" );
			final Icon helpIcon = new ImageIcon( iHelp );
			this.helpButton = new JButton( helpIcon )
				{
				@Override
				public boolean isFocusTraversable() { return false; }
				};
			this.helpButton.setMargin( new Insets( 1,3,1,3 ) );
			}
		catch ( final IOException ex )
			{
			this.helpButton = new JButton( "?" );
			}

		this.helpButton.setToolTipText( this.getHelpButtonToolTipText() );

		this.helpButton.addActionListener( evt -> toggleHelp() );
		AWTUtilities.constrain(
			typePan, this.helpButton,
			GridBagConstraints.NONE,
			GridBagConstraints.EAST,
			1, 0, 1, 1, 0.0, 0.0 );

		this.helpPanel = this.establishHelpPanel();

		this.editPanel = this.establishEditPanel( type );
		this.editScroller = new JScrollPane( this.editPanel );

		this.add( BorderLayout.CENTER, this.editScroller );
		this.add( BorderLayout.NORTH, typePan );
		}

	}

