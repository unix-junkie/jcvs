
package com.ice.config;

import java.util.Vector;
import java.util.Enumeration;

import com.ice.pref.UserPrefs;


public
class		ConfigureUtil
implements	ConfigureConstants
	{

	public static Vector
	readConfigSpecification( UserPrefs specs )
		throws InvalidSpecificationException
		{
		String specSfx = ".spec";
		Vector result = new Vector();
		String propPfx = specs.getPropertyPrefix();

		for ( Enumeration enum = specs.keys()
				; enum.hasMoreElements() ; )
			{
			String key = (String) enum.nextElement();

			if ( ! key.endsWith( specSfx ) )
				continue;

			// Strip off the property prefix to prepare for getProperty().
			String keyBase = specs.getBaseName( key );

			// Get the property type using this key.
			String type = specs.getProperty( keyBase, null );

			// Strip off the ".spec" suffix.
			keyBase =
				keyBase.substring
					( 0, (keyBase.length() - specSfx.length()) );

			// Get the other property parameters using the various suffixes.
			String path =
				specs.getProperty( keyBase + ".path", null );
			String name =
				specs.getProperty( keyBase + ".name", null );
			String desc =
				specs.getProperty( keyBase + ".desc", null );
			String help =
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
			else if ( type.equals( "choice" ) )
				{
				Vector sV = new Vector();
				for ( int ci = 0 ; ci < 32 ; ++ci )
					{
					String item =
						specs.getProperty
							( (keyBase + ".choice." + ci), null );
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
					String[] choices = new String[ sV.size() ];
					sV.copyInto( choices );

					ConfigureSpec spec =
						new ConfigureSpec
							( keyBase,
								type.trim(),
								path.trim(),
								name.trim(),
								( (desc == null) ? desc : desc.trim() ),
								( (help == null) ? help : help.trim() ),
								choices
							);

					result.addElement( spec );
					}
				}
			else
				{
				ConfigureSpec spec =
					new ConfigureSpec
						( keyBase,
							type.trim(),
							path.trim(),
							name.trim(),
							( (desc == null) ? desc : desc.trim() ),
							( (help == null) ? help : help.trim() ),
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

