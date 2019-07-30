
package com.ice.config.editor;

import java.util.List;
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

	@Override
	public void
	edit( final UserPrefs prefs, final ConfigureSpec spec )
		{
		super.edit( prefs, spec );

		final List<String> v = prefs.getStringVector( spec.getPropertyName(), null );

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

	@Override
	public void
	saveChanges( final UserPrefs prefs, final ConfigureSpec spec )
		{
		this.table.clearSelection();
		final List<String> vStrs = this.model.getData();
		final String[] strs = new String[ vStrs.size() ];
		vStrs.toArray( strs );
		prefs.setStringArray( spec.getPropertyName(), strs );
		}

	@Override
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

