
package com.ice.config.editor;

import com.ice.config.*;
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

	public String
	getTypeTitle()
		{
		return "Double";
		}

	public String
	formatNumber( UserPrefs prefs, ConfigureSpec spec )
		{
		double dbl =
			prefs.getDouble( spec.getPropertyName(), 0.0 );

		return Double.toString( dbl );
		}

	public boolean
	isChanged( UserPrefs prefs, ConfigureSpec spec, String numText )
		{
		double cur = Double.valueOf( numText ).doubleValue();
		double old = prefs.getDouble( spec.getPropertyName(), 0 );
		return ( cur != old );
		}

	}

