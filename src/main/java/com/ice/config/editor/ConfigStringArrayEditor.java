
package com.ice.config.editor;

import java.util.Vector;

import com.ice.config.ConfigureSpec;
import com.ice.pref.UserPrefs;


public
class		ConfigStringArrayEditor
extends		ConfigArrayEditor
	{

	public
	ConfigStringArrayEditor()
		{
		super( "String Array" );
		}

	public boolean
	isTupleTable( final ConfigureSpec spec )
		{
		return false;
		}

	public boolean
	isStringArray( final ConfigureSpec spec )
		{
		return true;
		}

	public void
	edit( final UserPrefs prefs, final ConfigureSpec spec )
		{
		super.edit( prefs, spec );

		final Vector v = prefs.getStringVector( spec.getPropertyName(), null );

		if ( v != null )
			{
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
		this.table.clearSelection();
		final Vector vStrs = this.model.getData();
		final String[] strs = new String[ vStrs.size() ];
		vStrs.copyInto( strs );
		prefs.setStringArray( spec.getPropertyName(), strs );
		}

	public void
	commitChanges( final ConfigureSpec spec, final UserPrefs prefs, final UserPrefs orig )
		{
		final String propName = spec.getPropertyName();
		final String[] strs = prefs.getStringArray( propName, null );
		orig.removeStringArray( propName );
		if ( strs != null )
			{
			orig.setStringArray( propName, strs );
			}
		}

	}

