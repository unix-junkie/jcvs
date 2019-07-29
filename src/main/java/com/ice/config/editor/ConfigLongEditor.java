
package com.ice.config.editor;

import com.ice.config.ConfigureSpec;
import com.ice.pref.UserPrefs;


public
class		ConfigLongEditor
extends		ConfigNumberEditor
	{

	public
	ConfigLongEditor()
		{
		super( "Long" );
		}

	@Override
	public String
	getTypeTitle()
		{
		return "Long";
		}

	@Override
	public String
	formatNumber( final UserPrefs prefs, final ConfigureSpec spec )
		{
		final long num =
			prefs.getLong( spec.getPropertyName(), 0 );

		return Long.toString( num );
		}

	@Override
	public boolean
	isChanged( final UserPrefs prefs, final ConfigureSpec spec, final String numText )
		{
		final long cur = Long.parseLong( numText );
		final long old = prefs.getLong( spec.getPropertyName(), 0 );
		return cur != old;
		}

	}

