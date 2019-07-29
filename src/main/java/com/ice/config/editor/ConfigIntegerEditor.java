
package com.ice.config.editor;

import com.ice.config.ConfigureSpec;
import com.ice.pref.UserPrefs;


public
class		ConfigIntegerEditor
extends		ConfigNumberEditor
	{

	public
	ConfigIntegerEditor()
		{
		super( "Integer" );
		}

	@Override
	public String
	getTypeTitle()
		{
		return "Integer";
		}

	@Override
	public String
	formatNumber( final UserPrefs prefs, final ConfigureSpec spec )
		{
		final int num =
			prefs.getInteger( spec.getPropertyName(), 0 );

		return Integer.toString( num );
		}

	@Override
	public boolean
	isChanged( final UserPrefs prefs, final ConfigureSpec spec, final String numText )
		{
		final int cur = Integer.parseInt( numText );
		final int old = prefs.getInteger( spec.getPropertyName(), 0 );
		return cur != old;
		}

	}

