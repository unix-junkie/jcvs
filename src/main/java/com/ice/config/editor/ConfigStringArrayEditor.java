
package com.ice.config.editor;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.table.*;

import com.ice.config.*;
import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;


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
	isTupleTable( ConfigureSpec spec )
		{
		return false;
		}

	public boolean
	isStringArray( ConfigureSpec spec )
		{
		return true;
		}

	public void
	edit( UserPrefs prefs, ConfigureSpec spec )
		{
		super.edit( prefs, spec );

		Vector v = prefs.getStringVector( spec.getPropertyName(), null );

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
	saveChanges( UserPrefs prefs, ConfigureSpec spec )
		{
		this.table.clearSelection();
		Vector vStrs = this.model.getData();
		String[] strs = new String[ vStrs.size() ];
		vStrs.copyInto( strs );
		prefs.setStringArray( spec.getPropertyName(), strs );
		}

	public void
	commitChanges( ConfigureSpec spec, UserPrefs prefs, UserPrefs orig )
		{
		String propName = spec.getPropertyName();
		String[] strs = prefs.getStringArray( propName, null );
		orig.removeStringArray( propName );
		if ( strs != null )
			{
			orig.setStringArray( propName, strs );
			}
		}

	}

