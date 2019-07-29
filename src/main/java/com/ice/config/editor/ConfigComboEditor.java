
package com.ice.config.editor;

import java.awt.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.border.*;

import com.ice.config.*;
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

	public void
	edit( UserPrefs prefs, ConfigureSpec spec )
		{
		super.edit( prefs, spec );

		this.comboPanel.removeAll();

		String propName = spec.getPropertyName();

		String choice = prefs.getProperty( propName, null );

		String[] choices = spec.getChoices();
		this.combo = new JComboBox( choices );
		this.comboPanel.add( this.combo );

		if ( choice != null )
			{
			this.combo.setSelectedItem( choice );
			}

		this.comboPanel.validate();
		this.comboPanel.repaint( 250 );
		}

	public void
	saveChanges( UserPrefs prefs, ConfigureSpec spec )
		{
		String propName = spec.getPropertyName();

		String oldChoice = prefs.getProperty( propName, null );

		String newChoice = (String)this.combo.getSelectedItem();
		prefs.setProperty( propName, newChoice );
		}

	public void
	requestInitialFocus()
		{
		}

	protected JPanel
	createEditPanel()
		{
		JPanel result = new JPanel();

		result.setLayout( new BoxLayout( result, BoxLayout.Y_AXIS ) );
		result.setBorder( new EmptyBorder( 5, 5, 5, 5 ) );

		return this.comboPanel = result;
		}

	}

