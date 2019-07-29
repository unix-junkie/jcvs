
package com.ice.config.editor;

import java.util.List;
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

	@Override
	public void
	edit( final UserPrefs prefs, final ConfigureSpec spec )
		{
		super.edit( prefs, spec );

		final String[] tokens = prefs.getTokens( spec.getPropertyName(), null );

		if ( tokens != null )
			{
			final List<String> v = new Vector<>();
			for ( final String token : tokens)
				v.add( token );
			this.model.setData( v );
			}
		else
			{
			this.model.setData( new Vector<String>() );
			}

		this.table.sizeColumnsToFit( -1 );
		this.table.repaint( 100 );
		}

	@Override
	public void
	saveChanges( final UserPrefs prefs, final ConfigureSpec spec )
		{
		final List<String> vTokens = this.model.getData();
		final String[] tokens = new String[ vTokens.size() ];
		vTokens.toArray( tokens );
		prefs.setTokens( spec.getPropertyName(), tokens );
		}

	}

