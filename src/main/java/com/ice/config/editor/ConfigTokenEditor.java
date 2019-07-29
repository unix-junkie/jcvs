
package com.ice.config.editor;

import java.util.Vector;

import com.ice.config.ConfigureSpec;
import com.ice.pref.UserPrefs;


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
	edit( final UserPrefs prefs, final ConfigureSpec spec )
		{
		super.edit( prefs, spec );

		final String[] tokes = prefs.getTokens( spec.getPropertyName(), null );

		if ( tokes != null )
			{
			final Vector v = new Vector();
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
	saveChanges( final UserPrefs prefs, final ConfigureSpec spec )
		{
		final Vector vTokes = this.model.getData();
		final String[] tokes = new String[ vTokes.size() ];
		vTokes.copyInto( tokes );
		prefs.setTokens( spec.getPropertyName(), tokes );
		}

	}

