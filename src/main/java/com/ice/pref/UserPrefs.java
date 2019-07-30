/*
** User Preferences Package.
** Copyright (c) 1999 by Timothy Gerard Endres
**
** This program is free software.
**
** You may redistribute it and/or modify it under the terms of the GNU
** General Public License as published by the Free Software Foundation.
** Version 2 of the license should be included with this distribution in
** the file LICENSE, as well as License.html. If the license is not
** included	with this distribution, you may find a copy at the FSF web
** site at 'www.gnu.org' or 'www.fsf.org', or you may write to the
** Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139 USA.
**
** THIS SOFTWARE IS PROVIDED AS-IS WITHOUT WARRANTY OF ANY KIND,
** NOT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY. THE AUTHOR
** OF THIS SOFTWARE, ASSUMES _NO_ RESPONSIBILITY FOR ANY
** CONSEQUENCE RESULTING FROM THE USE, MODIFICATION, OR
** REDISTRIBUTION OF THIS SOFTWARE.
**
*/

package com.ice.pref;

import static java.util.Collections.emptyList;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import com.ice.util.StringUtilities;


/**
 * This class implements the "global" properties functionality.
 * It should be used in place of the System.getProperty() functionality.
 *
 * The class operates as an instance, meaning that an application may
 * have more than one instance of properties. Each instance is identified
 * by its name, and can be retrieved from the class by name.
 * The class manges a table of instances of properties.
 *
 * This allows the programmer to track properties for different
 * parts of their code, such as transient properties versus user
 * configuration properties. If you do nothing, the class will
 * create a single properties named "DEFAULT" to handle all
 * properties, so you do not need to manage multiple instances.
 *
 * <p>
 * Things the application <strong>must</strong> do to use this class:
 *
 * <ul>
 * <li> Set the property prefix via UserPrefs.getInstance().setPropertyPrefix()
 * <li> Load properties.
 * </ul>
 *
 * <p>
 * Here is an example from a typical main():
 * <pre>
 *		UserPrefs props = UserPrefs.getInstance();
 *		props.setPropertyPrefix( "com.ice.client." );
 *
 *		// LOAD PROPERTIES
 *      FileInputStream pin = new FileInputStream( PROP_FILE_NAME );
 *		props.loadProperties( in );
 *		pin.close();
 * </pre>
 *
 * <p>
 * Properties are accessed via the getProperty() methods, which
 * provide versions for String, int, double, and boolean values.
 * Any property is looked for as follows:
 *
 * <ol>
 * <li> fullname.osname.username
 * <li> fullname.username
 * <li> fullname.osname
 * <li> fullname
 * </ol>
 *
 * Whichever property is found first is returned.
 * The <em>fullname</em> consists of the property name prefixed
 * by the application's property prefix. The username and osname
 * suffixes are used to override general properties by os or by
 * user. These suffixes are printed at startup to System.err.
 * The osname suffix is the osname with spaces replaced by
 * underscores. The username suffix is the user's name with
 * spaces replaced by underscores.
 *
 * <p>
 * If the property name starts with a period
 * (.), then the prefix is not added to get the full name.
 * If the property name ends with a period (.), then none
 * of the suffixes are applied, and only the name is used
 * to search for a property.
 *
 * <p>
 * Thus, while the property name "mainWindow.x" would match
 * a property definition named "prefix.mainWindow.x.user", the
 * property ".mainWindow.x." would match a property with only
 * that name and no prefix or suffix, and the property
 * "mainWindow.x." would match "prefix.mainWindow.x" only
 * and not allow for suffix overrides.
 *
 * @author Tim Endres,
 *    <a href="mailto:time@ice.com">time@ice.com</a>.
 *
 */
public final
class       UserPrefs
extends		Properties
    {
	private static final long serialVersionUID = 4066236418157392250L;

	/**
	 * The instance of th DEFAULT preferences.
	 */
	private static UserPrefs		instance;

	/**
	 * Determines if debugging is turned on.
	 */
	private boolean			debug;

	/**
	 * The name of this properties instance.
	 */
	private String			name;

	/**
	 * This is property names prefix used for lookup.
	 */
	private String			prefix;

	/**
	 * This is the user home directory property.
	 */
	private String			userHome;
	/**
	 * This is the OS name appended to property names to override.
	 */
	private String			osSuffix;
	/**
	 * This is the user name appended to property names to override.
	 */
	private String			userSuffix;
	/**
	 * The delimiter used for string arrays and vectors.
	 */
	private String			delim;

	private Map<String, PropertyChangeSupport>	subScribers;


	/**
	 * We create an instance when the Class is loaded
	 * and work with the instance.
	 */
	static
		{
		setInstance( new UserPrefs( "DEFAULT" ) );
		}

	/**
	 * Get the current properties instance.
	 */
    private static UserPrefs
	getInstance()
    	{
    	return instance;
    	}

	/**
	 * Set the current properties instance.
	 */
    public static void
	setInstance( final UserPrefs inst )
    	{
    	instance = inst;
    	}

	/**
	 * Get the System line separator property.
	 *
	 * @param name The name of the property to normalize.
	 * @return The line separator property.
	 */

	public static String
	getLineSeparator()
		{
		return System.getProperty( "line.separator", "\n" );
		}

	/**
	 * We make this constructor private. It is not used.
	 */
	@SuppressWarnings("unused")
	private UserPrefs() {
		assert false;
	}

	/**
	 * Construct a UserPrefs named <em>name</em> with the System
	 * properties.
	 */
	public
	UserPrefs( final String name )
		{
		this( name, System.getProperties() );
		}

	/**
	 * This contructor, available to the user, requires the defaults.
	 * Use this constructor if you wish to set the default properties.
	 *
	 * @param defProps The default properties.
	 */
	public
	UserPrefs( final String nm, final Properties defProps )
		{
		super( defProps );

		this.initializePrefs( nm );
		}

	private void
	initializePrefs( final String nm )
		{
		this.name = nm;
		this.debug = false;
		this.delim = ":";
		this.prefix = this.name + '.';

		this.userHome =
			System.getProperty( "user.home", null );

		if ( this.userHome == null )
			{
			this.userHome =
				File.separatorChar == ':' ? "" : ".";
			}

		this.osSuffix =
			this.normalizeSuffix( System.getProperty( "os.name", "" ) );

		this.userSuffix =
			this.normalizeSuffix( System.getProperty( "user.name", "" ) );

		this.subScribers = new Hashtable<>();
		}

	public UserPrefs
	createWorkingCopy( final String nm )
		{
		final UserPrefs result = new UserPrefs( nm, this );
		result.setPropertyPrefix( this.getPropertyPrefix() );
		return result;
		}

	public String
	getOSSuffix()
		{
		return this.osSuffix;
		}

	public void
	setOSSuffix( final String suffix )
		{
		this.osSuffix = this.normalizeSuffix( suffix );
		}

	public String
	getUserSuffix()
		{
		return this.userSuffix;
		}

	public void
	setUserSuffix( final String suffix )
		{
		this.userSuffix = this.normalizeSuffix( suffix );
		}

	public void
	setPropertyPrefix( final String prefix )
		{
		if ( prefix == null || prefix.isEmpty())
			{
			this.prefix = ".";
			}
		else if (prefix.charAt(prefix.length() - 1) == '.')
			this.prefix = prefix;
		else
			this.prefix = prefix + '.';

		if ( this.debug )
			System.err.println
				("UserPrefs.setPropertyPrefix: prefix set to '"
				 + this.prefix + '\'');
		}

	public String
	getPropertyPrefix()
		{
		return this.prefix;
		}

	/**
	 * Get the current working directory path via the System
	 * property 'user.dir'.
	 *
	 * @return The user's home directory path.
	 */

	public String
	getCurrentDirectory()
		{
		String result = System.getProperty( "user.dir", null );
		if ( result == null )
			result = File.separatorChar == ':' ? "" : ".";
		return result;
		}

	/**
	 * Get the user's home directory path via the System
	 * property 'user.home'.
	 *
	 * @return The user's home directory path.
	 */

	public String
	getUserHome()
		{
		return this.userHome;
		}

	/**
	 * Get the user's home directory path via the System
	 * property 'user.home'.
	 *
	 * @return The user's home directory path.
	 */

	public void
	setUserHome( final String newHome ) {
		this.userHome = newHome;
		System.setProperty("user.home", newHome);
	}

	/**
	 * Given a property name with a prefix, such as 'com.ice.jcvs.tempDir',
	 * return the 'base name', which is the name with the prefix stripped
	 * off. So, if the prefix was 'com.ice.jcvs', the previous name would
	 * return 'tempDir'.
	 *
	 * @param propName The property name to get the base of.
	 * @return The base name, or the original name if it does not start
	 *         with the prefix.
	 */
	public String
	getBaseName( final String propName )
		{
		if ( this.prefix == null || this.prefix.isEmpty()
		     || this.prefix.equals( "." )
		     || !propName.isEmpty() && propName.charAt(0) == '.')
			{
			return propName;
			}

		if ( propName.startsWith( this.prefix ) )
			{
			return propName.substring( this.prefix.length() );
			}

		return propName;
		}

	private String
	normalizeSuffix( final String name )
		{
		return name.replace( ' ', '_' ).replace( '.', '_' ).
					replace( ',', '_' ).replace( '/', '_' );
		}

	/**
	 * Return a property name prefixed by the prefix that is set
	 * for this UserPrefs instance.
	 *
	 * @param name The name of the property to normalize.
	 * @return The normalized property name.
	 */

	private String
	prefixedPropertyName(final String name)
		{
		if ( this.debug )
			System.err.println
				("UserPrefs.prefixedPropertyName: prefix '"
				 + this.prefix + "' name '" + name + '\'');
		final StringBuilder result = new StringBuilder();
		if ( ! this.prefix.equals( "." ) )
			result.append( this.prefix );
		result.append( name );
		return result.toString();
		}

	/**
	 * Return a normalized version of a property name.
	 *
	 * If the name starts with a '.', we strip off the
	 * leading '.' and return the remainder of the name.
	 * That allows the user to specify property names that
	 * should not be prefixed.
	 *
	 * If the name does not start with a '.', then we
	 * return the named prefixed by the set prefix.
	 *
	 * @param name The name of the property to normalize.
	 * @return The normalized property name.
	 */

	private String
	normalizedPropertyName(final String name) {
		return !name.isEmpty() && name.charAt(0) == '.'
		       ? name.substring(1)
		       : this.prefixedPropertyName(name);
	}

	/**
	 * Retrieve a system string property.
	 * Returns a provided default value if the property
	 * is not defined.
	 *
	 * @param name The name of the property to retrieve.
	 * @param defval A default string value.
	 * @return The string value of the named property.
	 */

	private String
	locateProperty( final Properties props, final String normName )
		{
		String		value;
		String		overName;

		if ( this.osSuffix != null
				&& this.userSuffix != null )
			{
			overName =
					normName + '.' + this.osSuffix
					+ '.' + this.userSuffix;
			value = (String) props.get( overName );
			if ( this.debug )
				System.err.println
					("UserPrefs.getOverridableProperty: "
					 + overName + " = '" + value + '\'');
			if ( value != null )
				return value;
			}

		if ( this.userSuffix != null )
			{
			overName = normName + '.' + this.userSuffix;
			value = (String) props.get( overName );
			if ( this.debug )
				System.err.println
					("UserPrefs.getOverridableProperty: "
					 + overName + " = '" + value + '\'');
			if ( value != null )
				return value;
			}

		if ( this.osSuffix != null )
			{
			overName = normName + '.' + this.osSuffix;
			value = (String) props.get( overName );
			if ( this.debug )
				System.err.println
					("UserPrefs.getOverridableProperty: "
					 + overName + " = '" + value + '\'');
			if ( value != null )
				return value;
			}

		value = (String) props.get( normName );
		if ( this.debug )
			System.err.println
				("UserPrefs.getOverridableProperty: "
				 + normName + " = '" + value + '\'');

		return value;
		}

	private synchronized String
	getOverridableProperty( final String name, final String defval )
		{
		String		value;

		String normName = this.normalizedPropertyName( name );
		if ( this.debug )
			System.err.println
				("UserPrefs.getOverridableProperty: Normalized name '"
				 + normName + '\'');

		if (!normName.isEmpty() && normName.charAt(normName.length() - 1) == '.')
			{
			normName = normName.substring( 0, normName.length() - 1 );
			value = super.getProperty( normName, defval );
			if ( this.debug )
				System.err.println
					("UserPrefs.getOverridableProperty: "
					 + normName + " = '" + value + '\'');
			return value;
			}

		if ( this.debug )
			System.err.println
				( "UserPrefs.getOverridableProperty: Looking in '"+this.name+"'..." );

		value = this.locateProperty( this, normName );

		for ( Properties defs = this.defaults
				; value == null && defs != null ; )
			{
			String dNorm = normName;
			String dnm = "System.Properties";

			if ( defs instanceof UserPrefs )
				{
				dnm = ((UserPrefs) defs).name;
				dNorm = ((UserPrefs) defs).normalizedPropertyName( name );
				}

			if ( this.debug )
				System.err.println
					( "UserPrefs.getOverridableProperty: "
						+ "Looking in defaults '" + dnm + "'..." );

			value = this.locateProperty( defs, dNorm );

			if ( defs instanceof UserPrefs )
				{
				defs = ((UserPrefs) defs).defaults;
				}
			else
				{
				// If we are at the System properties, or any Properties
				// that is not ours, and we have not found the property,
				// then call getProperty() on those "foreign" Properties
				// to give them a chance to locate the property up their
				// defaults chain...
				//
				if ( this.debug )
				System.err.println
					( "UserPrefs.getOverridableProperty: "
						+ "Last ditch call to defs.getProperty() on '"
						+ dnm + "'..." );

					if (value == null) {
						value = defs.getProperty(dNorm, null);
					}

				defs = null;
				}
			}

		if ( value == null )
			{
			value = defval;
			if ( this.debug )
				System.err.println
					("UserPrefs.getOverridableProperty: "
					 + name + " defaulted to '" + value + '\'');
			}

		return value;
		}

	/**
	 * Retrieve a system string property.
	 * Returns a null if the property is not defined.
	 *
	 * @param name The name of the property to retrieve.
	 * @return The string value of the named property.
	 */

	@Override
	public String
	getProperty( final String name )
		{
			return this.getOverridableProperty
				( name, null );
		}

	/**
	 * Retrieve a system string property.
	 * Returns a provided default value if the property
	 * is not defined.
	 *
	 * @param name The name of the property to retrieve.
	 * @param defval A default string value.
	 * @return The string value of the named property.
	 */

	@Override
	public String
	getProperty( final String name, final String defval )
		{
			return this.getOverridableProperty
				( name, defval );
		}

	/**
	 * Retrieve a system integer property.
	 * Returns a provided default value if the property
	 * is not defined.
	 *
	 * @param name The name of the property to retrieve.
	 * @param defval A default integer value.
	 * @return The integer value of the named property.
	 */

	public int
	getInteger( final String name, final int defval )
		{
		int result = defval;

		final String val = this.getProperty( name, null );

		if ( val != null )
			{
			try { result = Integer.parseInt( val ); }
				catch ( final NumberFormatException ex ) {
				}
			}

		return result;
		}

	/**
	 * Retrieve a system long property.
	 * Returns a provided default value if the property
	 * is not defined.
	 *
	 * @param name The name of the property to retrieve.
	 * @param defval A default integer value.
	 * @return The integer value of the named property.
	 */

	public long
	getLong( final String name, final long defval )
		{
		long result = defval;

		final String val = this.getProperty( name, null );

		if ( val != null )
			{
			try { result = Long.parseLong( val ); }
				catch ( final NumberFormatException ex ) {
				}
			}

		return result;
		}

	/**
	 * Retrieve a system float property.
	 * Returns a provided default value if the property
	 * is not defined.
	 *
	 * @param name The name of the property to retrieve.
	 * @param defval A default float value.
	 * @return The float value of the named property.
	 */

	public float
	getFloat( final String name, final float defval )
		{
		float result = defval;

		final String val = this.getProperty( name, null );

		if ( val != null )
			{
			try { result = Float.valueOf(val); }
				catch ( final NumberFormatException ex ) {
				}
			}

		return result;
		}

	/**
	 * Retrieve a system double property.
	 * Returns a provided default value if the property
	 * is not defined.
	 *
	 * @param name The name of the property to retrieve.
	 * @param defval A default double value.
	 * @return The double value of the named property.
	 */

	public double
	getDouble( final String name, final double defval )
		{
		double result = defval;

		final String val = this.getProperty( name, null );

		if ( val != null )
			{
			try { result = Double.valueOf(val); }
				catch ( final NumberFormatException ex ) {
				}
			}

		return result;
		}

	/**
	 * Retrieve a system boolean property.
	 * Returns a provided default value if the property
	 * is not defined.
	 *
	 * @param name The name of the property to retrieve.
	 * @param defval A default boolean value.
	 * @return The boolean value of the named property.
	 */

	public boolean
	getBoolean( final String name, final boolean defval )
		{
		boolean result = defval;

		final String val = this.getProperty( name, null );

		if ( val != null )
			{
			if ( val.equalsIgnoreCase( "T" )
					|| val.equalsIgnoreCase( "TRUE" )
					|| val.equalsIgnoreCase( "Y" )
					|| val.equalsIgnoreCase( "YES" )
					|| val.equalsIgnoreCase( "1" ) )
				result = true;
			else if ( val.equalsIgnoreCase( "F" )
					|| val.equalsIgnoreCase( "FALSE" )
					|| val.equalsIgnoreCase( "N" )
					|| val.equalsIgnoreCase( "NO" )
					|| val.equalsIgnoreCase( "0" ) )
				result = false;
			}

		return result;
		}

	/**
	 * Retrieve a Font property.
	 * Fonts are represented with one of the standard representations:
	 *    fontname-style-pointsize
	 *    fontname-pointsize
	 *    fontname-style
	 *    fontname
	 *
	 * @param name The name of the property to retrieve.
	 * @param defval A default value.
	 * @return The Font identified by the named property.
	 */

	public Font
	getFont( final String name, final Font defaultFont ) {
		Font result = defaultFont;

		final String val = this.getProperty( name, null );
		if ( this.debug )
			System.err.println
				( "UserPrefs.getFont: property = " + val );

		if ( val != null ) {
			final String[] flds = splitString( val, "-" );
			if ( this.debug )
				System.err.println
					( "UserPrefs.getFont: flds.length = " + flds.length );

			switch (flds.length) {
			case 1:
				if (this.debug) {
					System.err.println
							("UserPrefs.getFont: [0] " + flds[0]);
				}
				result = new Font(flds[0], Font.PLAIN, 12);
				break;
			case 3:
				if (this.debug) {
					System.err.println
							("UserPrefs.getFont: [0] " + flds[0]
							 + " [1] " + flds[1] + " [2] " + flds[2]);
				}
				try {
					int style = Font.PLAIN;
					final int size = Integer.parseInt(flds[2]);
					if (flds[1].equalsIgnoreCase("BOLD")) {
						style = Font.BOLD;
					} else if (flds[1].equalsIgnoreCase("ITALIC")) {
						style = Font.ITALIC;
					} else if (flds[1].equalsIgnoreCase("BOLDITALIC")) {
						style = Font.BOLD | Font.ITALIC;
					}
					result = new Font(flds[0], style, size);
				} catch (final NumberFormatException ex) {
					if (this.debug) {
						System.err.println
								("UserPrefs.getFont: SIZE NumberFormatException: "
								 + ex.getMessage());
					}
				}
				break;
			case 2:
				// We must determine if field 2 is a size or a style...
				if (this.debug) {
					System.err.println
							("UserPrefs.getFont: [0] "
							 + flds[0] + " [1] " + flds[1]);
				}
				try {
					final int size = Integer.parseInt(flds[1]);
					result = new Font(flds[0], Font.PLAIN, size);
				} catch (final NumberFormatException ex) {
					if (this.debug) {
						System.err.println
								("UserPrefs.getFont: SIZE NumberFormatException: "
								 + ex.getMessage());
					}
					int style = Font.PLAIN;
					if (flds[1].equalsIgnoreCase("BOLD")) {
						style = Font.BOLD;
					} else if (flds[1].equalsIgnoreCase("ITALIC")) {
						style = Font.ITALIC;
					} else if (flds[1].equalsIgnoreCase("BOLDITALIC")) {
						style = Font.BOLD | Font.ITALIC;
					}

					result = new Font(flds[0], style, 12);
				}
				break;
			}
		}

		return result;
	}

	/**
	 * Retrieve a Color property.
	 * Colors are represented by an integer property.
	 *
	 * @param name The name of the property to retrieve.
	 * @param defval A default value.
	 * @return The Color value of the named property.
	 */

	public Color
	getColor( final String name, final Color defaultColor )
		{
		Color result = defaultColor;

		final String val = this.getProperty( name, null );
		if ( val != null )
			{
			try {
				final int rgb = Integer.parseInt( val );
				result = new Color( rgb );
				}
			catch ( final NumberFormatException ex )
				{
				ex.printStackTrace();
				}
			}

		return result;
		}

	/**
	 * Retrieve a Point property.
	 * Points are represented with a delimiter seperated string
	 * in the order x, y.
	 *
	 * @param name The name of the property to retrieve.
	 * @param defval A default value.
	 * @return The Point value of the named property.
	 */

	public Point
	getPoint( final String name, final Point defval )
		{
		Point result = defval;

		final String val = this.getProperty( name, null );
		if ( val != null )
			{
			final String[] flds = splitString( val, this.delim );
			if ( flds.length == 2 )
				{
				try {
					final Point p = new Point();
					p.x = Integer.parseInt( flds[0] );
					p.y = Integer.parseInt( flds[1] );
					result = p;
					}
				catch ( final NumberFormatException ex )
					{
					ex.printStackTrace();
					}
				}
			}

		return result;
		}

	/**
	 * Retrieve a Dimension property.
	 * Points are represented with a delimiter seperated string
	 * in the order width, height.
	 *
	 * @param name The name of the property to retrieve.
	 * @param defval A default value.
	 * @return The Dimension value of the named property.
	 */

	public Dimension
	getDimension( final String name, final Dimension defval )
		{
		Dimension result = defval;

		final String val = this.getProperty( name, null );
		if ( val != null )
			{
			final String[] flds = splitString( val, this.delim );
			if ( flds.length == 2 )
				{
				try {
					final Dimension dim = new Dimension();
					dim.width = Integer.parseInt( flds[0] );
					dim.height = Integer.parseInt( flds[1] );
					result = dim;
					}
				catch ( final NumberFormatException ex )
					{
					ex.printStackTrace();
					}
				}
			}

		return result;
		}

	/**
	 * Retrieve a Rectangle property.
	 * Rectangles are represented with a delimiter seperated string
	 * in the order x, y, w, h.
	 *
	 * @param name The name of the property to retrieve.
	 * @param defval A default value.
	 * @return The Rectangle value of the named property.
	 */

	public Rectangle
	getBounds( final String name, final Rectangle defval )
		{
		Rectangle result = defval;

		final String val = this.getProperty( name, null );

		if ( val != null )
			{
			final String[] flds = splitString( val, this.delim );

			if ( flds.length == 4 )
				{
				try {
					final Rectangle rect = new Rectangle();
					rect.x = Integer.parseInt( flds[0] );
					rect.y = Integer.parseInt( flds[1] );
					rect.width = Integer.parseInt( flds[2] );
					rect.height = Integer.parseInt( flds[3] );
					result = rect;
					}
				catch ( final NumberFormatException ex )
					{
					ex.printStackTrace();
					}
				}
			}

		return result;
		}

	/**
	 * Retrieve a system string array property list. String
	 * arrays are represented by colon separated strings.
	 * Returns a provided default value if the property
	 * is not defined.
	 *
	 * @param name The name of the property to retrieve.
	 * @param defval A default boolean value.
	 * @return The string array value of the named property.
	 */

	public String[]
	getStringArray( final String name, final String[] defval )
		{
		String[] result = defval;

		final List<String> v = this.getStringVector( name, null );
		if ( v != null )
			{
			result = new String[ v.size() ];
			v.toArray( result );
			}

		return result;
		}

	/**
	 * Retrieve a system string Vector property list. String
	 * vectors are represented by colon separated strings.
	 * Returns a provided default value if the property
	 * is not defined.
	 *
	 * @param name The name of the property to retrieve.
	 * @param defval A default boolean value.
	 * @return The Vector of strings of the named property.
	 */

	public List<String>
	getStringVector( final String name, final List<String> defval )
		{
		List<String> result = defval;

		final int size = this.getInteger( name + ".size", 0 );
		if ( size > 0 )
			{
			result = new Vector<>();
			for ( int i = 0 ; i < size ; ++i )
				{
				final String idxName = name + '.' + i;
				final String val = this.getProperty( idxName, "" );
				result.add( val );
				}
			}

		return result;
		}

	public String[]
	getTokens( final String name, final String[] defval )
		{
		String[] result = defval;

		final String val = this.getProperty( name, null );
		if ( val != null )
			{
			result = splitString( val, this.delim );
			}

		return result;
		}

	/**
	 * NOTE That we use the vectorString() method, since it will
	 * return an empty ending token if the property ends with the
	 * delimiter. Currently, splitString() does not do this.
	 */
	private static String[]
	splitString(final String splitStr, final String delim)
		{
		final List<String> sv = StringUtilities.vectorString( splitStr, delim );
		final String[] result = new String[ sv.size() ];
		sv.toArray( result );
		return result;
		}

	public PrefsTupleTable
	getTupleTable( final String name, final PrefsTupleTable defval )
		{
		PrefsTupleTable result = defval;

		String key;
		String val;

		final int size = this.getInteger( name + ".size", 0 );
		if ( size > 0 )
			{
			result = new PrefsTupleTable();

			for ( int row = 0 ; row < size ; ++row )
				{
				key = this.getProperty(name + '.' + row + ".key", null );
				final int rowSz =
					this.getInteger(name + '.' + row + ".size", 0 );

				if ( key != null && rowSz > 0 )
					{
					final List<String> tupV = new Vector<>();
					for ( int iv = 0 ; iv < rowSz ; ++iv )
						{
						val = this.getProperty
								(name + '.' + row + '.' + iv, null );

						if ( val == null )
							break;

						tupV.add( val );
						}

					final PrefsTuple tup = new PrefsTuple( key, tupV );

					result.putTuple( tup );
					}
				else if ( key != null )
					{
					result.putTuple( new PrefsTuple( key, emptyList() ) );
					}
				else
					{
					new Throwable
						("BAD tuple property '" + name + '\'').
							printStackTrace();
					}
				}
			}

		return result;
		}

	public void
	addPropertyChangeListener
			( final String propName, final PropertyChangeListener pL )
		{
		PropertyChangeSupport pList =
				this.subScribers.get( propName );

		if ( pList == null )
			{
			pList = new PropertyChangeSupport( this );
			this.subScribers.put( propName, pList );
			}

		pList.addPropertyChangeListener( pL );
		}

	public void
	removePropertyChangeListener
			( final String propName, final PropertyChangeListener pL )
		{
		final PropertyChangeSupport pList =
				this.subScribers.get( propName );

		if ( pList != null )
			{
			pList.removePropertyChangeListener( pL );
			}
		}

	private void
	firePropertyChange(final String propName, final String oldVal, final String newVal)
		{
		final PropertyChangeSupport pList =
				this.subScribers.get( propName );

		if ( pList != null )
			{
			pList.firePropertyChange( propName, oldVal, newVal );
			}
		}

	/**
	 * Set a property value without firing a property change event.
	 *
	 * @param name The name of the property to retrieve.
	 * @param value The property's value.
	 * @return The replaced value of the property if it exists.
	 */

	private synchronized String
	setPropertyNoFire(final String name, final String value) {
		final String normName = this.normalizedPropertyName( name );
		return (String) super.setProperty(normName, value);
	}

	/**
	 * Set a property value and fire property change event.
	 *
	 * @param name The name of the property to retrieve.
	 * @param value The property's value.
	 * @return The replaced value of the property if it exists.
	 */

	@Override
	public synchronized Object
	setProperty(final String name, final String value) {
		final String result = this.setPropertyNoFire( name, value );
		this.firePropertyChange( name, result, value );
		return result;
	}

	/**
	 * Set an int property.
	 *
	 * @param name The name of the property to set.
	 * @param value The property's value.
	 */

	public void
	setInteger( final String name, final int value )
		{
		final String valStr = String.valueOf(value);
		this.setProperty( name, valStr );
		}

	/**
	 * Set an int property.
	 *
	 * @param name The name of the property to set.
	 * @param value The property's value.
	 */

	public void
	setLong( final String name, final long value )
		{
		final String valStr = Long.toString( value );
		this.setProperty( name, valStr );
		}

	/**
	 * Set a float property.
	 *
	 * @param name The name of the property to set.
	 * @param value The property's value.
	 */

	public void
	setFloat( final String name, final float value )
		{
		final String valStr = Float.toString( value );
		this.setProperty( name, valStr );
		}

	/**
	 * Set a double property.
	 *
	 * @param name The name of the property to set.
	 * @param value The property's value.
	 */

	public void
	setDouble( final String name, final double value )
		{
		final String valStr = Double.toString( value );
		this.setProperty( name, valStr );
		}

	/**
	 * Set a boolean property.
	 *
	 * @param name The name of the property to set.
	 * @param value The property's value.
	 */

	public void
	setBoolean( final String name, final boolean value )
		{
		final String valStr = value ? "true" : "false";
		this.setProperty( name, valStr );
		}

	/**
	 * Set a Point property.
	 *
	 * @param name The name of the property to set.
	 * @param value The property's value.
	 */

	public void
	setPoint( final String name, final Point value )
		{
		final String valStr =
			value.x + this.delim + value.y;

		this.setProperty( name, valStr );
		}

	/**
	 * Set a Dimension property.
	 *
	 * @param name The name of the property to set.
	 * @param value The property's value.
	 */

	public void
	setDimension( final String name, final Dimension value )
		{
		final String valStr =
			value.width + this.delim + value.height;

		this.setProperty( name, valStr );
		}

	/**
	 * Set a Rectangle property.
	 *
	 * @param name The name of the property to set.
	 * @param value The property's value.
	 */

	public void
	setBounds( final String name, final Rectangle value )
		{
		final String valStr =
			value.x + this.delim + value.y + this.delim +
			value.width + this.delim + value.height;

		this.setProperty( name, valStr );
		}

	/**
	 * Set a Font property using the standard format ( see getFont() ).
	 *
	 * @param name The name of the property to set.
	 * @param value The property's value.
	 */

	public void
	setFont( final String name, final Font value )
		{
		final int style = value.getStyle();

		final String styleStr =
			style == Font.PLAIN ? null :
		style == Font.BOLD ? "BOLD" :
		style == Font.ITALIC ? "ITALIC"
			: "BOLDITALIC";

		final String valStr =
				value.getName() + '-' +
				( styleStr == null ? "" :
				  styleStr + '-') +
				value.getSize();

		this.setProperty( name, valStr );
		}

	/**
	 * Set a Color property using the standard format ( see getColor() ).
	 *
	 * @param name The name of the property to set.
	 * @param value The property's value.
	 */

	public void
	setColor( final String name, final Color value )
		{
		final String valStr = String.valueOf(value.getRGB());
		this.setProperty( name, valStr );
		}

	/**
	 * Set a String array property. Each string is stored as a separate property.
	 *
	 * @param name The name of the property to set.
	 * @param value The property's value.
	 */

	public void
	setStringArray( final String name, final String[] strings )
		{
		this.setInteger( name + ".size", strings.length );
		for ( int i = 0 ; i < strings.length ; ++i )
			{
			this.setProperty(name + '.' + i, strings[i] );
			}
		}

	/**
	 * Set a Tokens property. Tokens are short string values that are stored
	 * as a single property with the tokens separated by the delim.
	 *
	 * @param name The name of the property to set.
	 * @param value The String array of tokens.
	 */

	public void
	setTokens( final String name, final String[] tokes )
		{
		final StringBuilder buf = new StringBuilder();

		for ( int i = 0 ; i < tokes.length ; ++i )
			{
			buf.append( tokes[i] );
			if ( i < tokes.length - 1 )
				buf.append( this.delim );
			}

		this.setProperty( name, buf.toString() );
		}

	/**
	 * Set a TupleTable property. Tuple tables are stored as a string matrix
	 * where every cell of that matrix is a property string named using the
	 * matrix indices.
	 *
	 * <pre>
	 * Thus, the table:
	 *
	 * key.0 val.0.0 val.0.1 val.0.2
	 * key.1 val.1.0 val.1.1 val.1.2
	 * key.2 val.2.0 val.2.1 val.2.2
	 *
	 * would be stored as properties in this fashion:
	 *
	 * propName.0.key=key.0
	 * propName.0.0=val.0.0
	 * propName.0.1=val.0.1
	 * propName.0.2=val.0.2
	 * propName.1.key=key.1
	 * propName.1.0=val.1.0
	 * propName.1.1=val.1.1
	 * propName.1.2=val.1.2
	 * propName.2.key=key.2
	 * propName.2.0=val.2.0
	 * propName.2.1=val.2.1
	 * propName.2.2=val.2.2
	 *
	 * </pre>
	 *
	 * This design allows the property code to simply use a for
	 * loop incrementing the indices until a property returns null
	 * indicating the end of tuple values or the end of table (when
	 * a ".key" property returns null).
	 *
	 * @param name The name of the property to set.
	 * @param value The property's value.
	 */

	public void
	setTupleTable( final String name, final PrefsTupleTable table )
		{
		final Vector<String> kv = table.getKeyOrder();
		this.setInteger( name + ".size", kv.size() );

		for ( int i = 0 ; i < kv.size() ; ++i )
			{
			final PrefsTuple tup =
				table.getTuple(kv.elementAt(i));

			this.setProperty(name + '.' + i + ".key", tup.getKey() );

			final String[] vals = tup.getValues();
			this.setInteger(name + '.' + i + ".size", vals.length );

			for ( int j = 0 ; j < vals.length ; ++j )
				{
				final String propName = name + '.' + i + '.' + j;
				this.setProperty( propName, vals[j] );
				}
			}
		}

	//
	// REVIEW
	// Should this method also remove overrides? Should there be
	// another method removeOverridableProperty() to do that?
	//
	private void
	removeProperty(final String propName)
		{
		String normName = this.normalizedPropertyName( propName );
		if (!normName.isEmpty() && normName.charAt(normName.length() - 1) == '.')
			normName = normName.substring( 0, normName.length() - 1 );
		this.remove( normName );
		}

	public void
	removeStringArray( final String propName )
		{
		final int size = this.getInteger( propName + ".size", 0 );
		this.removeProperty( propName + ".size" );
		for ( int i = 0 ; i < size ; ++i )
			{
			this.removeProperty(propName + '.' + i );
			}
		}

	public void
	removeTupleTable( final String propName )
		{
		final int size = this.getInteger( propName + ".size", 0 );
		this.removeProperty( propName + ".size" );
		for ( int row = 0 ; row < size ; ++row )
			{
			this.removeProperty(propName + '.' + row + ".key" );
			final int cols = this.getInteger(propName + '.' + row + ".size", 0 );
			this.removeProperty(propName + '.' + row + ".size" );
			for ( int col = 0 ; col < cols ; ++col )
				{
				this.removeProperty(propName + '.' + row + '.' + col );
				}
			}
		}

	/**
	 * Simple test program. Run with no arguments.
	 * This code could be much more robust.
	 */
	public static void
	main( final String... args )
		{
		System.err.println( "UserPrefs.main: testing class..." );

		final Properties defPrefs = getInstance();

		defPrefs.setProperty
			( "testPref.1.1", "Pref '1.1' set directly on 'DEFAULT'" );
		getInstance().setProperty
			( "testPref.1.2", "Pref '1.2' set via getInstance" );

		final Properties prefsTwo = new UserPrefs("TestTwo", defPrefs );

		prefsTwo.setProperty
			( "testPref.2.1", "Pref '2.1' set directly on 'TestTwo'" );

		getInstance().setProperty
			( "testPref.2.2", "Pref '2.2' set via getInstance" );

		System.out.println( "======== Preferences 'DEFAULT' ========" );

		try {
			defPrefs.store( System.out, "DEFAULT PROPERTIES" );
			}
		catch ( final IOException ex )
			{
			ex.printStackTrace();
			}

		System.out.println( "======== ======= END 'DEFAULT' ========" );

		System.out.println();
		System.out.println();

		System.out.println( "======== Preferences 'TestTwo' ========" );

		try {
			prefsTwo.store( System.out, "TestTwo PROPERTIES" );
			}
		catch ( final IOException ex )
			{
			ex.printStackTrace();
			}

		System.out.println( "======== ======= END 'DEFAULT' ========" );
		}

	}

