
package com.ice.config.editor;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.table.*;

import com.ice.config.*;
import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;


public
class		ConfigTokenEditor
extends		ConfigArrayEditor
	{

	public
	ConfigTokenEditor()
		{
		super( "Tokens" );
		}

	public void
	edit( UserPrefs prefs, ConfigureSpec spec )
		{
		super.edit( prefs, spec );

		String[] tokes = prefs.getTokens( spec.getPropertyName(), null );

		if ( tokes != null )
			{
			Vector v = new Vector();
			for ( int i = 0 ; i < tokes.length ; ++i )
				v.addElement( tokes[i] );
			this.model.setData( v );
			}
		else
			{
			this.model.setData( new Vector() );
			}

		this.table.sizeColumnsToFit( -1 );
		this.table.repaint( 100 );
		}

	public void
	saveChanges( UserPrefs prefs, ConfigureSpec spec )
		{
		Vector vTokes = this.model.getData();
		String[] tokes = new String[ vTokes.size() ];
		vTokes.copyInto( tokes );
		prefs.setTokens( spec.getPropertyName(), tokes );
		}

	}

