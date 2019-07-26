
package com.ice.config.editor;

import com.ice.config.*;
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

	public String
	getTypeTitle()
		{
		return "Float";
		}

	public String
	formatNumber( UserPrefs prefs, ConfigureSpec spec )
		{
		float num =
			prefs.getFloat( spec.getPropertyName(), 0.0F );

		return Float.toString( num );
		}

	public boolean
	isChanged( UserPrefs prefs, ConfigureSpec spec, String numText )
		{
		float cur = Float.valueOf( numText ).floatValue();
		float old = prefs.getFloat( spec.getPropertyName(), 0 );
		return ( cur != old );
		}

	}

