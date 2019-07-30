
package com.ice.config;

import static com.ice.config.ConfigureConstants.CFG_BOOLEAN;
import static com.ice.config.ConfigureConstants.CFG_CHOICE;
import static com.ice.config.ConfigureConstants.CFG_COLOR;
import static com.ice.config.ConfigureConstants.CFG_COMBO;
import static com.ice.config.ConfigureConstants.CFG_DEFAULT;
import static com.ice.config.ConfigureConstants.CFG_DIMENSION;
import static com.ice.config.ConfigureConstants.CFG_DOUBLE;
import static com.ice.config.ConfigureConstants.CFG_FLOAT;
import static com.ice.config.ConfigureConstants.CFG_FONT;
import static com.ice.config.ConfigureConstants.CFG_INTEGER;
import static com.ice.config.ConfigureConstants.CFG_LONG;
import static com.ice.config.ConfigureConstants.CFG_POINT;
import static com.ice.config.ConfigureConstants.CFG_RECTANGLE;
import static com.ice.config.ConfigureConstants.CFG_STRING;
import static com.ice.config.ConfigureConstants.CFG_STRINGARRAY;
import static com.ice.config.ConfigureConstants.CFG_TOKENS;
import static com.ice.config.ConfigureConstants.CFG_TUPLETABLE;

import java.util.Hashtable;

import com.ice.config.editor.ConfigBooleanEditor;
import com.ice.config.editor.ConfigChoiceEditor;
import com.ice.config.editor.ConfigColorEditor;
import com.ice.config.editor.ConfigComboEditor;
import com.ice.config.editor.ConfigDimensionEditor;
import com.ice.config.editor.ConfigDoubleEditor;
import com.ice.config.editor.ConfigFloatEditor;
import com.ice.config.editor.ConfigFontEditor;
import com.ice.config.editor.ConfigIntegerEditor;
import com.ice.config.editor.ConfigLongEditor;
import com.ice.config.editor.ConfigPointEditor;
import com.ice.config.editor.ConfigRectangleEditor;
import com.ice.config.editor.ConfigStringArrayEditor;
import com.ice.config.editor.ConfigStringEditor;
import com.ice.config.editor.ConfigTokenEditor;
import com.ice.config.editor.ConfigTupleTableEditor;
import com.ice.pref.UserPrefs;

public
class		DefaultConfigureEditorFactory
		implements	ConfigureEditorFactory
	{
	private final Hashtable		editors;


		public
	DefaultConfigureEditorFactory( final UserPrefs specs )
		{

			this.editors = new Hashtable();

		this.editors.put
			( CFG_DEFAULT, this.createDefaultEditor( CFG_STRING ) );

		this.editors.put
			( CFG_STRING, this.createDefaultEditor( CFG_STRING ) );
		}

	@Override
	public ConfigureEditor
	createEditor( final String propertyType )
		{
		ConfigureEditor result = (ConfigureEditor)
			this.editors.get( propertyType );

		if ( result == null )
			{
			result = createDefaultEditor( propertyType );

			if ( result != null )
				{
				this.editors.put( propertyType, result );
				}
			}

		return result;
		}

	public void
	addEditor( final String propertyType, final ConfigureEditor editor )
		{
		this.editors.put( propertyType, editor );
		}

	public void
	removeEditor( final String propertyType )
		{
		this.editors.remove( propertyType );
		}

	private ConfigureEditor
	createDefaultEditor( final String propertyType )
		{
		if ( propertyType.equalsIgnoreCase( CFG_STRING ) )
			return new ConfigStringEditor();

		else if ( propertyType.equalsIgnoreCase( CFG_POINT ) )
			return new ConfigPointEditor();

		else if ( propertyType.equalsIgnoreCase( CFG_DIMENSION ) )
			return new ConfigDimensionEditor();

		else if ( propertyType.equalsIgnoreCase( CFG_RECTANGLE ) )
			return new ConfigRectangleEditor();

		else if ( propertyType.equalsIgnoreCase( CFG_BOOLEAN ) )
			return new ConfigBooleanEditor();

		else if ( propertyType.equalsIgnoreCase( CFG_INTEGER ) )
			return new ConfigIntegerEditor();

		else if ( propertyType.equalsIgnoreCase( CFG_LONG ) )
			return new ConfigLongEditor();

		else if ( propertyType.equalsIgnoreCase( CFG_FLOAT ) )
			return new ConfigFloatEditor();

		else if ( propertyType.equalsIgnoreCase( CFG_DOUBLE ) )
			return new ConfigDoubleEditor();

		else if ( propertyType.equalsIgnoreCase( CFG_COLOR ) )
			return new ConfigColorEditor();

		else if ( propertyType.equalsIgnoreCase( CFG_FONT ) )
			return new ConfigFontEditor();

		else if ( propertyType.equalsIgnoreCase( CFG_CHOICE ) )
			return new ConfigChoiceEditor();

		else if ( propertyType.equalsIgnoreCase( CFG_COMBO ) )
			return new ConfigComboEditor();

		else if ( propertyType.equalsIgnoreCase( CFG_STRINGARRAY ) )
			return new ConfigStringArrayEditor();

		else if ( propertyType.equalsIgnoreCase( CFG_TOKENS ) )
			return new ConfigTokenEditor();

		else if ( propertyType.equalsIgnoreCase( CFG_TUPLETABLE ) )
			return new ConfigTupleTableEditor();

		return null;
		}

	}

