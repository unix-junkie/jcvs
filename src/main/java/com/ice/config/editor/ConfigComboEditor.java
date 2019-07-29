
package com.ice.config.editor;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.ice.config.ConfigureEditor;
import com.ice.config.ConfigureSpec;
import com.ice.pref.UserPrefs;


public
class		ConfigComboEditor
extends		ConfigureEditor
	{
	protected JPanel			comboPanel;
	protected JComboBox			combo;


	public
	ConfigComboEditor()
		{
		super( "Combo" );
		}

	@Override
	public void
	edit( final UserPrefs prefs, final ConfigureSpec spec )
		{
		super.edit( prefs, spec );

		this.comboPanel.removeAll();

		final String propName = spec.getPropertyName();

		final String choice = prefs.getProperty( propName, null );

		final String[] choices = spec.getChoices();
		this.combo = new JComboBox( choices );
		this.comboPanel.add( this.combo );

		if ( choice != null )
			{
			this.combo.setSelectedItem( choice );
			}

		this.comboPanel.validate();
		this.comboPanel.repaint( 250 );
		}

	@Override
	public void
	saveChanges( final UserPrefs prefs, final ConfigureSpec spec )
		{
		final String propName = spec.getPropertyName();

		final String oldChoice = prefs.getProperty( propName, null );

		final String newChoice = (String)this.combo.getSelectedItem();
		prefs.setProperty( propName, newChoice );
		}

	@Override
	public void
	requestInitialFocus()
		{
		}

	@Override
	protected JPanel
	createEditPanel()
		{
		final JPanel result = new JPanel();

		result.setLayout( new BoxLayout( result, BoxLayout.Y_AXIS ) );
		result.setBorder( new EmptyBorder( 5, 5, 5, 5 ) );

		return this.comboPanel = result;
		}

	}

