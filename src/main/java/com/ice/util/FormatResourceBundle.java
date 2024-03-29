/*
** Authored by Timothy Gerard Endres
** <mailto:time@ice.com>  <http://www.ice.com>
**
** This work has been placed into the public domain.
** You may use this work in any way and for any purpose you wish.
**
** THIS SOFTWARE IS PROVIDED AS-IS WITHOUT WARRANTY OF ANY KIND,
** NOT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY. THE AUTHOR
** OF THIS SOFTWARE, ASSUMES _NO_ RESPONSIBILITY FOR ANY
** CONSEQUENCE RESULTING FROM THE USE, MODIFICATION, OR
** REDISTRIBUTION OF THIS SOFTWARE.
**
*/


package com.ice.util;

import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;


public final
class		FormatResourceBundle
extends		ResourceBundle
	{
	private static final Hashtable	alias = new Hashtable();
	private static final Hashtable	cache = new Hashtable();

		/**
	 * The actual resource bundle that we are wrapping.
	 */
	private final ResourceBundle		bundle;


	/**
	 * Add a resource name alias. This allows you to use shorter
	 * names for your resource bundles. For example:
	 * <pre>
	 * FormatResourceBundle.addAlias( "ui", "com.ice.mail.resources.ui" );
	 * ...
	 * FormatResourceBundle fmtBndl =
	 *    FormatResourceBundle.getFormatBundle( "ui" );
	 * ...
	 * FormatResourceBundle.getFormatBundle( "ui" ).getString( "menuName" );
	 * </pre>
	 *
	 * The alias will only work with the getFormatBundle() class methods
	 * in this class, and will not work with the other ResourceBundle
	 * getBundle() class methods.
	 *
	 * @param alias The alias by which you will refer to this resource bundle.
	 * @param baseName The actual resource name used to get the bundle.
	 *
	 */

	public static void
	addAlias( final String alias, final String baseName )
		{
		FormatResourceBundle.alias.put( alias, baseName );
		}

	/**
	 * This class method allows you to remove a bundle from the cache.
	 * This allows the bundle to be GC-ed.
	 *
	 * @param baseName The resource bundle's name or alias.
	 */

	public static void
	unCache( final String alias, final String baseName )
		{
		String name = (String)FormatResourceBundle.alias.get( baseName );
		if ( name == null ) name = baseName;
		cache.remove( name );
		}

	/**
	 * Get a FormatResourceBundle. This method actually calls
	 * ResourceBundle.getBundle(). However, first, the method
	 * substitutes any aliases added by the programmer, and
	 * second, the method wraps the retrieved ResourceBundle
	 * within a FormatResourceBundle adding the getFormatString()
	 * method to those already provided by ResourceBundle().
	 *
	 * @param baseName The name, or alias, of the bundle to get.
	 * @return The cached or a new FormatResource.
	 */
	public static FormatResourceBundle
	getFormatBundle( final String baseName )
		{
		String name = (String) alias.get(baseName );
		if ( name == null ) name = baseName;

		FormatResourceBundle result = (FormatResourceBundle)
			cache.get( name );

		if ( result == null )
			{
			final ResourceBundle bundle =
				ResourceBundle.getBundle( name );

			if ( bundle != null )
				{
				result = new FormatResourceBundle( bundle );
				cache.put( name, result );
				}
			}

		return result;
		}

	/**
	 * Get a FormatResourceBundle. This method is identical to
	 * getFormatBundle( String baseName ), except that it takes
	 * an additional locale parameter.
	 *
	 * @param baseName The name, or alias, of the bundle to get.
	 * @param locale The locale to use in locating the bundle.
	 * @return The cached or a new FormatResource.
	 */
	public static FormatResourceBundle
	getFormatBundle( final String baseName, final Locale locale )
		{
		String name = (String) alias.get(baseName );
		if ( name == null ) name = baseName;

		FormatResourceBundle result = (FormatResourceBundle)
			cache.get( name );

		if ( result == null )
			{
			final ResourceBundle bundle =
				ResourceBundle.getBundle( name, locale );

			if ( bundle != null )
				{
				result = new FormatResourceBundle( bundle );
				cache.put( name, result );
				}
			}

		return result;
		}

	/**
	 * Construct a new FormatResourceBundle by encapsulating another
	 * ResourceBundle object which is the actual bundle.
	 *
	 * @param bundle The resource bundle that we are encapsulating.
	 */

	private FormatResourceBundle(final ResourceBundle bundle)
		{
		super();
		this.bundle = bundle;
		}

	/**
	 * This method simply extends the ResourceBundle's getString()
	 * with a method that allows a default value to be specified.
	 *
	 * @param key The key of the string resource to format.
	 * @param defValue The default value used if key not found.
	 * @return The resource string.
	 */

	public String
	getString( final String key, final String defValue )
		{
		String rsrcStr = defValue;

		try { rsrcStr = this.bundle.getString( key ); }
		catch ( final MissingResourceException ex )
			{
				/**
				 * Set this to true to get output whenever a resource is missing.
				 * Set to false for release!
				 */
				final boolean debug = true;
				if (debug)
				System.err.println
					("MISSING RESOURCE: '" + key + '\'');
			rsrcStr = defValue;
			}

		return rsrcStr;
		}

	/**
	 * Get a string from the resource bundle, and return the formatted
	 * version of the string using args. This method gets the resource
	 * string identified by key, then passes the string to the
	 * MessageFormat.format() as the format string, and passes args
	 * as the format arguments.
	 *
	 * @param key The key of the string resource to format.
	 * @param args The arguments to use to format the string.
	 * @return The formatted string.
	 */

	public String
	getFormatString( final String key, final Object... args )
		{
		final String fmtStr = this.bundle.getString( key );

		return fmtStr == null
			? null
			: this.format( fmtStr, args );
		}

	/**
	 * Return an enumeration of the resource keys. This method simply
	 * calls the same method of the resource bundle we are encapsulating.
	 *
	 * @return An enumeration of the resource keys.
	 */

	@Override
	public Enumeration<String>
	getKeys()
		{
		return this.bundle.getKeys();
		}

	/**
	 * Return an object identified by its key. This method simply
	 * calls the same method of the resource bundle we are encapsulating.
	 *
	 * @param key The key of the object to return.
	 * @return The object identified by the key.
	 */

	@Override
	protected Object
	handleGetObject( final String key )
		{
		return this.bundle.getObject( key );
		}

	/**
	 * This is a HORRIBLE HACK that is necessitated because of the
	 * terribly lazing programming displayed by the author of the
	 * core Java class 'java.text.MessageFormat'. The sloppy coding
	 * hard codes a limit of ten items that may be replaced by the
	 * MessageFormat.format() method. Incredible.
	 *
	 * Thus, we need a method to allow more general formatting. The
	 * simplest thing I could come up with was to parse up the format
	 * text so that we MessageFormat.format() each item one at a time.
	 * I am not happy with this code, but am not up to a more general
	 * solution at this time. This is depressing...
	 *
	 */

	private static String
	filterQuotes( String str )
		{
		final StringBuilder buf = new StringBuilder();

		for ( ; ; )
			{
			final int idx = str.indexOf( "''" );
			if ( idx == -1 )
				{
				buf.append( str );
				break;
				}

			buf.append(str, 0, idx);
			buf.append('\'');
			str = str.substring( idx + 2 );
			}

		return buf.toString();
		}

	private String
	format( final String formatStr, final Object... fmtArgs )
		{
		final StringBuilder result =
			new StringBuilder(formatStr.length() + 256 );

		String workStr = formatStr;

		for ( ; ; )
			{
			final int lcbIdx = workStr.indexOf('{');
			if ( lcbIdx == -1 )
				{
				break;
				}

			if ( lcbIdx > 0 )
				{
				final char lqt = workStr.charAt(lcbIdx-1);
				final char num = workStr.charAt(lcbIdx+1);
				final char rcb = workStr.charAt(lcbIdx+2);

				final String leftStr = workStr.substring( 0, lcbIdx );

				if ( lqt == '\'' && num == '\'' )
					{
					// This is a quoted brace, put it...
					result.append
						( filterQuotes
							( workStr.substring( 0, lcbIdx - 1 ) ) );

					result.append('{');

					workStr = workStr.substring( lcbIdx + 2 );
					}
				else if ( num >= '0' && num <= '9' && rcb == '}' )
					{
					// This is a valid format item, to be replaced...
					result.append( filterQuotes( leftStr ) );
					final String fmtStr = "{" + num + '}';
					result.append( MessageFormat.format( fmtStr, fmtArgs ) );
					workStr = workStr.substring( lcbIdx + 3 );
					}
				else
					{
					// This is an error, I believe!
					result.append( filterQuotes( leftStr ) );
					result.append( "ERR{ERR" );
					workStr = workStr.substring( lcbIdx + 1 );
					}
				}
			else
				{
				final char num = workStr.charAt(1);
				final char rcb = workStr.charAt(2);
				if ( rcb == '}' && num >= '0' && num <= '9' )
					{
					final String fmtStr = "{" + num + '}';
					result.append
						( MessageFormat.format( fmtStr, fmtArgs ) );
					workStr = workStr.substring( 3 );
					}
				else
					{
					result.append('{');
					workStr = workStr.substring( 1 );
					}
				}
			}

		if (!workStr.isEmpty())
			{
			result.append( filterQuotes( workStr ) );
			}

		return result.toString();
		}

	}

