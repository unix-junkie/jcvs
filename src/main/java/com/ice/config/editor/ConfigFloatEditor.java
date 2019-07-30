
package com.ice.config.editor;

import com.ice.config.ConfigureSpec;
import com.ice.pref.UserPrefs;


public
class		ConfigFloatEditor
extends		ConfigNumberEditor
	{

	public
	ConfigFloatEditor()
		{
		super( "Double" );
		}

	@Override
	public String
	getTypeTitle()
		{
		return "Float";
		}

	@Override
	public String
	formatNumber( final UserPrefs prefs, final ConfigureSpec spec )
		{
		final float num =
			prefs.getFloat( spec.getPropertyName(), 0.0F );

		return Float.toString( num );
		}

	@Override
	public boolean
	isChanged( final UserPrefs prefs, final ConfigureSpec spec, final String numText )
		{
		final float cur = Float.valueOf( numText );
		final float old = prefs.getFloat( spec.getPropertyName(), 0 );
		return cur != old;
		}

	}

