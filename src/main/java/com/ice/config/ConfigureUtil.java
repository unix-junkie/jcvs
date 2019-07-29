
package com.ice.config;

import java.util.Vector;

import com.ice.pref.UserPrefs;


public
class		ConfigureUtil
implements	ConfigureConstants
	{

	public static Vector
	readConfigSpecification( final UserPrefs specs )
		throws InvalidSpecificationException
		{
		final String specSfx = ".spec";
		final Vector result = new Vector();
		final String propPfx = specs.getPropertyPrefix();

		for ( final String key : specs.stringPropertyNames() )
			{
			if ( ! key.endsWith( specSfx ) )
				continue;

			// Strip off the property prefix to prepare for getProperty().
			String keyBase = specs.getBaseName( key );

			// Get the property type using this key.
			final String type = specs.getProperty( keyBase, null );

			// Strip off the ".spec" suffix.
			keyBase =
				keyBase.substring
					( 0, keyBase.length() - specSfx.length() );

			// Get the other property parameters using the various suffixes.
			final String path =
				specs.getProperty( keyBase + ".path", null );
			final String name =
				specs.getProperty( keyBase + ".name", null );
			final String desc =
				specs.getProperty( keyBase + ".desc", null );
			final String help =
				specs.getProperty( keyBase + ".help", null );

			String reason = "";
			boolean invalid = false;
			if ( type == null )
				{
				invalid = true;
				reason = "the spec has no property type";
				}
			else if ( path == null )
				{
				invalid = true;
				reason = "the spec has no config tree path";
				}
			else if ( name == null )
				{
				invalid = true;
				reason = "the spec has no property name";
				}
			else if ( type.equals( "choice" ) || type.equals( "combo" ) )
				{
				final Vector sV = new Vector();
				for ( int ci = 0 ; ci < 64 ; ++ci )
					{
					final String item =
						specs.getProperty
							( keyBase + "." + type + "." + ci, null );
					if ( item == null )
						break;
					sV.addElement( item );
					}

				if ( sV.size() < 2 )
					{
					invalid = true;
					reason = "choice config has no choices (need 2 or more)";
					}
				else
					{
					final String[] choices = new String[ sV.size() ];
					sV.copyInto( choices );

					final ConfigureSpec spec =
						new ConfigureSpec
							( keyBase,
								type.trim(),
								path.trim(),
								name.trim(),
								desc == null ? desc : desc.trim(),
								help == null ? help : help.trim(),
								choices
							);

					result.addElement( spec );
					}
				}
			else
				{
				final ConfigureSpec spec =
					new ConfigureSpec
						( keyBase,
							type.trim(),
							path.trim(),
							name.trim(),
							desc == null ? desc : desc.trim(),
							help == null ? help : help.trim(),
							null
						);

				result.addElement( spec );
				}

			if ( invalid )
				{
				throw new InvalidSpecificationException
					( "invalid configuration specification for '"
						+ keyBase + "', " + reason );
				}
			}

		return result;
		}

	}

