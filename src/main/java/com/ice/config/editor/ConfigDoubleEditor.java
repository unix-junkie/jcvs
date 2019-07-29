
package com.ice.config.editor;

import com.ice.config.ConfigureSpec;
import com.ice.pref.UserPrefs;


public
class		ConfigDoubleEditor
extends		ConfigNumberEditor
	{

	public
	ConfigDoubleEditor()
		{
		super( "Double" );
		}

	@Override
	public String
	getTypeTitle()
		{
		return "Double";
		}

	@Override
	public String
	formatNumber( final UserPrefs prefs, final ConfigureSpec spec )
		{
		final double dbl =
			prefs.getDouble( spec.getPropertyName(), 0.0 );

		return Double.toString( dbl );
		}

	@Override
	public boolean
	isChanged( final UserPrefs prefs, final ConfigureSpec spec, final String numText )
		{
		final double cur = Double.valueOf( numText ).doubleValue();
		final double old = prefs.getDouble( spec.getPropertyName(), 0 );
		return cur != old;
		}

	}

