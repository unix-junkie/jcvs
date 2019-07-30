/*
** Copyright (c) 1997 by Tim Endres
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

package com.ice.util;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Vector;


/**
 * The UserProperties class. This class is used to extend
 * the concept of System.getProperty(). This class adds the
 * following features:
 *
 * <ul>
 * <li> A hierarchical property definition structure.
 * <li> Typed property retrieval with default values.
 * <li> Hierarchical overrides allowings properties to
 * be overridden on a per user or per host or per host
 * and user basis.
 * <li> The ability to load from any valid resource, including
 * files stored in JAR files, files located via the file system,
 * which includes networked file systems, as well as any other
 * resource that can be identified via a URL, including web pages,
 * FTP-ed files, and more.
 * </ul>
 *
 * <p>
 * Here is how it works. We have <em>six</em> levels of
 * properties which are loaded based on various settings.
 * The levels look like this:
 *
 * <ol>
 * <li> Hardwired defaults.
 * <li> Application defined defaults.
 * <li> Defaults resource.
 * <li> System level resource list.
 * <li> Application property file.
 * <li> Application resource list.
 * </ol>
 *
 * <p>
 * Resource lists are colon (:) separated strings listing
 * resource names to be loaded into the properties.
 *
 * <p>
 * In a typical deployment, the developer will place the
 * defaults resource in the JAR file containing the application's
 * classes. This file will then define the application properties
 * file, which will usually be left empty allowing the user to
 * place all customizations in this local file. For distributed
 * applications, system resources will typically be supplied via
 * simple web pages, which allows for automatic updates of many
 * properties. The application resource list is then typically
 * reserved for specific user customizations, or for distributed
 * customizations, or updates.
 *
 * <p>
 * Typically, the System Resource List is defined in the
 * Defaults Resource. However, it can also be defined by
 * the application defaults, or can be hardwired into the
 * code if desired. Further, the Application Resource List
 * is typically defined by the Application Property File,
 * although it can be defined in any of the previous loaded
 * resources.
 *
 * <p>
 * Also note that the application prefix can be set at any
 * point up to and including the defaults resource. After
 * the defaults resource is loaded, the prefix property is
 * consulted, and if set, is used as the new application
 * prefix. The prefix property is named by adding together
 * the application's package name and the string 'propertyPrefix'.
 * Thus, the prefix property for 'com.ice.jcvs' would be named
 * 'com.ice.jcvs.propertyPrefix', and would typically be set
 * in the defaults resource.
 *
 * <p>
 * Things the application <strong>must</strong> do to use this class:
 *
 * <ul>
 * <li> Set the property prefix via UserProperties.setPropertyPrefix()
 * <li> Set the defaults resource via UserProperties.setDefaultsResource()
 * <li> Process any arguments via UserProperties.processOptions()
 * <li> Load all properties.
 * </ul>
 *
 * <p>
 * Here is an example from a typical main():
 * <pre>
 *		UserProperties.setPropertyPrefix( "WebTool." );
 *
 *		UserProperties.setDefaultsResource
 *			( "/com/ice/webtool/defaults.txt" );
 *
 *		// PROCESS PROPERTIES OPTIONS
 *		// The returned args are those not processed.
 *		args = UserProperties.processOptions( args );
 *
 *		// Now app should process remaining arguments...
 *
 *		// LOAD PROPERTIES
 *		UserProperties.loadProperties( "com.ice.webtool", null );
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
 * <p>
 * The following parameters are understood by processOptions():
 *
 * <ul>
 * <li> -propPrefix prefix     -- sets the property prefix.
 * <li> -propFile filename     -- sets the application property file.
 * <li> -propDefaults resource -- sets the defaults resource name.
 * <li> -propDebug             -- turns on debugging of property handling.
 * <li> -propVerbose           -- turns on verbosity.
 * <li> -propOS osname         -- set the property os suffix.
 * <li> -propUser username     -- set the property user name suffix.
 * </ul>
 *
 * @version $Revision: 1.10 $
 * @author Tim Endres,
 *    <a href="mailto:time@ice.com">time@ice.com</a>.
 */

final class
UserProperties
	{
	private static final String		PREFIX_PROPERTY = "propertyPrefix";
	private static final String		DEFAULTS_RSRC_NAME = ".com.ice.global.defaultsResource.";

	private static final String		GLOBAL_RSRCLIST_NAME = ".com.ice.global.propertyResourceList";
	private static final String		GLOBAL_RSRC_PREFIX = ".com.ice.global.propertyResource.";

	private static final String		APP_RSRCLIST_NAME = ".com.ice.local.propertyResourceList";
	private static final String		APP_RSRC_PREFIX = ".com.ice.local.propertyResource.";

	private static final String		LOCAL_PROPERTY = "global.localPropertyFile";
	private static final String		LOCAL_DEFAULT = null;

	private static final String		DYNAMIC_PROPERTY_VERSION = "1.0";


	private static boolean		debug;
	private static boolean		verbose;

	private static final String		osname;
	private static final String		userName;
	private static final String		userHome;
	private static final String		javaHome;

	private static String		prefix;

	private static String		osSuffix;
	private static String		userSuffix;

	private static String		defaultsResource;
	private static String		localPropertyFile;

	/**
	 * This is a Hashtable of Vectors. The table keys are
	 * dynamic property package names. Each Vector contains
	 * the list of property names in the dynamic package.
	 */
	private static final Hashtable<String, Vector<String>>	dynKeysTable;

	/**
	 * This is a Hashtable of Strings. The table keys are
	 * dynamic property package names. Each String is the
	 * pathname to the property file for the dynamic package.
	 */
	private static final Hashtable<String, String>	dynPathTable;

	/**
	 * Used for temporary working properties.
	 */
	private static final Properties	workingProps;


	static
		{
		debug = false;
		verbose = false;

		prefix = null;

		defaultsResource = null;
		localPropertyFile = null;

		dynKeysTable = new Hashtable<>();
		dynPathTable = new Hashtable<>();
		workingProps = new Properties();

		osname = System.getProperty( "os.name" );
		userName = System.getProperty( "user.name" );
		userHome = System.getProperty( "user.home" );
		javaHome = System.getProperty( "java.home" );

		osSuffix =
			osname.replace( ' ', '_' );
		userSuffix =
			userName.replace( ' ', '_' );
		}

	private UserProperties() {
		assert false;
	}

	public static String
	getOSName()
		{
		return osname;
		}

	public static String
	getUserHome()
		{
		return userHome;
		}

	public static String
	getUserName()
		{
		return userName;
		}

	private static void
	setDebug(final boolean debug)
		{
		UserProperties.debug = debug;
		}

	private static void
	setVerbose(final boolean verbose)
		{
		UserProperties.verbose = verbose;
		}

	private static void
	setLocalPropertyFile(final String fileName)
		{
		localPropertyFile = fileName;
		}

	private static void
	setDefaultsResource(final String rsrcName)
		{
		defaultsResource = rsrcName;
		}

	private static void
	setOSSuffix(final String suffix)
		{
		osSuffix = suffix;
		}

	private static void
	setUserSuffix(final String suffix)
		{
		userSuffix = suffix;
		}

	private static void
	setPropertyPrefix(final String prefix)
		{
			UserProperties.prefix = !prefix.isEmpty() && prefix.charAt(prefix.length() - 1) == '.' ? prefix : prefix + '.';
		}

	public static String
	getPropertyPrefix()
		{
		return prefix;
		}

	public static String
	getLineSeparator()
		{
		return System.getProperty( "line.separator", "\n" );
		}

	public static Font
	getFont( final String name, final Font defaultFont )
		{
		return
			Font.getFont
				( prefixedPropertyName( name ),
					defaultFont );
		}

	public static Color
	getColor( final String name, final Color defaultColor )
		{
		return
			Color.getColor
				( prefixedPropertyName( name ),
					defaultColor );
		}

	private static String
	prefixedPropertyName(final String name)
		{
		return prefix + name;
		}

	private static String
	normalizePropertyName(final String name)
		{
			return !name.isEmpty() && name.charAt(0) == '.' ? name.substring(1) : prefixedPropertyName(name);
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

	private static String
	getOverridableProperty( final String name, final String defval )
		{
		String		value = null;
		String		overName = null;
		String		fullName = null;

		fullName = normalizePropertyName( name );

		if (!fullName.isEmpty() && fullName.charAt(fullName.length() - 1) == '.')
			{
			fullName = fullName.substring( 0, fullName.length() - 1 );
			value = System.getProperty( fullName, defval );
			if ( debug )
				System.err.println
					("UserProperties.getOverridableProperty: "
					 + fullName + " = '" + value + '\'');
			return value;
			}

		if ( osSuffix != null
				&& userSuffix != null )
			{
			overName =
					fullName + '.' + osSuffix
					+ '.' + userSuffix;
			value = System.getProperty( overName, null );
			if ( debug )
				System.err.println
					("UserProperties.getOverridableProperty: "
					 + overName + " = '" + value + '\'');
			if ( value != null )
				return value;
			}

		if ( userSuffix != null )
			{
			overName = fullName + '.' + userSuffix;
			value = System.getProperty( overName, null );
			if ( debug )
				System.err.println
					("UserProperties.getOverridableProperty: "
					 + overName + " = '" + value + '\'');
			if ( value != null )
				return value;
			}

		if ( osSuffix != null )
			{
			overName = fullName + '.' + osSuffix;
			value = System.getProperty( overName, null );
			if ( debug )
				System.err.println
					("UserProperties.getOverridableProperty: "
					 + overName + " = '" + value + '\'');
			if ( value != null )
				return value;
			}

		if ( value == null )
			{
			value = System.getProperty( fullName, null );
			if ( debug )
				System.err.println
					("UserProperties.getOverridableProperty: "
					 + fullName + " = '" + value + '\'');
			}

		if ( value == null )
			{
			value = defval;
			if ( debug )
				System.err.println
					("UserProperties.getOverridableProperty: "
					 + name + " defaulted to '" + value + '\'');
			}

		return value;
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

	public static String
	getProperty( final String name, final String defval )
		{
			return getOverridableProperty
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

	public static int
	getProperty( final String name, final int defval )
		{
		int result = defval;

		final String val = getProperty( name, null );

		if ( val != null )
			{
			try { result = Integer.parseInt( val ); }
				catch ( final NumberFormatException ex )
					{ result = defval; }
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

	public static long
	getProperty( final String name, final long defval )
		{
		long result = defval;

		final String val = getProperty( name, null );

		if ( val != null )
			{
			try { result = Long.parseLong( val ); }
				catch ( final NumberFormatException ex )
					{ result = defval; }
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

	public static double
	getProperty( final String name, final double defval )
		{
		double result = defval;

		final String val = getProperty( name, null );

		if ( val != null )
			{
			try { result = Double.valueOf(val); }
				catch ( final NumberFormatException ex )
					{ result = defval; }
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

	public static boolean
	getProperty( final String name, final boolean defval )
		{
		boolean result = defval;

		final String val = getProperty( name, null );

		if ( val != null )
			{
			if ( val.equalsIgnoreCase( "T" )
					|| val.equalsIgnoreCase( "TRUE" )
					|| val.equalsIgnoreCase( "Y" )
					|| val.equalsIgnoreCase( "YES" ) )
				result = true;
			else if ( val.equalsIgnoreCase( "F" )
					|| val.equalsIgnoreCase( "FALSE" )
					|| val.equalsIgnoreCase( "N" )
					|| val.equalsIgnoreCase( "NO" ) )
				result = false;
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

	private static String[]
	getStringArray(final String name, final String[] defval)
		{
		String[] result = defval;

		final String val = getProperty( name, null );

		if ( val != null )
			{
			result = StringUtilities.splitString( val, ":" );
			}

		return result;
		}

	public static Vector
	getStringVector( final String name, final Vector defval )
		{
		Vector result = defval;

		final String[] sa =
			getStringArray
				( name, null );

		if ( sa != null )
			{
			result = new Vector();
			for ( final String element : sa )
				result.addElement( element );
			}

		return result;
		}

	/**
	 * Retrieve a system Class constant property.
	 *
	 * @param name The name of the property to retrieve.
	 * @param defval A default integer value.
	 * @return The integer value of the named property.
	 */

	public static int
	getClassConstant( final String name, final int defval )
		{
		int result = defval;

		final String val = getProperty( name, null );

		if ( val != null )
			{
			final int index = val.lastIndexOf('.');

			if ( index > 0 )
				{
				try {
					final String className = val.substring( 0, index );
					final String constName = val.substring( index + 1);
					final Class<?> cls = Class.forName(className );
					final Field fld = cls.getField( constName );
					result = fld.getInt( null );
					}
				catch ( final Exception ex )
					{
					result = defval;
					ICETracer.traceWithStack
						( "Exception getting constant." );
					}
				}
			}

		return result;
		}

	/**
	 * Establishes critical default properties.
	 *
	 * @param props The system properties to add properties into.
	 */

	private static void
	defaultProperties(final Properties props)
		{
			props.setProperty("com.ice.util.UserProperties.revision", "$Revision: 1.10 $");
			props.setProperty("copyright", "Copyright (c) by Tim Endres");

		//
		// Define the following to create a global
		// enterprise-wide defaults resource...
		// e.g.
		//
		// props.put
		//	( UserProperties.DEFAULTS_RSRC_NAME,
		//		"http://www.ice.com/properties/defaults.txt" );
		//
		}

	private static void
	includeProperties(final Properties into, final Properties from)
		{
		for ( final String key : from.stringPropertyNames() )
			{
			if ( key != null )
				{
				into.setProperty(key, from.getProperty(key));
				}
			}
		}

	private static void
	addDefaultProperties(final Properties props, final Properties defaultProps)
		{
		includeProperties( props, defaultProps );
		}

	private static void
	loadPropertiesStream( final InputStream in, final Properties props )
		throws IOException
		{
		props.load( in );
		}

	private static void
	doLoadPropertiesFile( final String path, final Properties props, final Properties loaded )
		throws IOException
		{
		final FileInputStream	in;
		final boolean			result = true;

		try { in = new FileInputStream( path ); }
		catch ( final IOException ex )
			{
			throw new IOException
				( "opening property file '" + path
					+ "' - " + ex.getMessage() );
			}

		try {
			if ( loaded != null )
				{
				loaded.load(in);
				includeProperties( props, loaded );
				}
			else
				{
				props.load(in);
				}
			}
		catch ( final IOException ex )
			{
			throw new IOException
				( "loading property file '" + path
					+ "' - " + ex.getMessage() );
			}

		try { in.close(); }
		catch ( final IOException ex )
			{
			throw new IOException
				( "closing property file '" + path
					+ "' - " + ex.getMessage() );
			}
		}

	/**
	 * Loads a named properties file into the System properties table.
	 *
	 * @param path The properties file's pathname.
	 * @param props The system properties to add properties into.
	 * @param loaded If not null, insert properties here before loading.
	 */

	private static boolean
	loadPropertiesFile( final String path, final Properties props, final Properties loaded )
		{
		final FileInputStream	in;
		boolean			result = true;

		if ( debug )
			System.err.println
				( "Loading property file '" + path + "'." );

		try {
			doLoadPropertiesFile( path, props, loaded );
			}
		catch ( final IOException ex )
			{
			System.err.println
				( "ERROR " + ex.getMessage() );
			result = false;
			}

		if ( result )
			System.err.println
				( "Loaded property file '" + path + "'." );

		return result;
		}

	/**
	 * Loads a named properties file into the System properties table.
	 * This method fails <em>silently,</em>, as we do not care if the
	 * file is there, and it is a feature that the user can remove the
	 * file to <em>reset</em> their settings.
	 *
	 * @param path The properties file's pathname.
	 * @param props The system properties to add properties into.
	 */

	private static void
	loadDynamicProperties( final String name, final String path )
		{
		final Properties	dynProps = new Properties();
		final Properties	sysProps = System.getProperties();

		if ( debug )
			System.err.println
				( "Loading  '" + name
					+ "' protperties from '" + path + "'." );

		try {
			doLoadPropertiesFile( path, sysProps, dynProps );
			addDynamicPropertyKeys( name, dynProps );
			System.err.println
				( "Loaded '" + name + "' properties from '" + path + "'." );
			}
		catch ( final IOException ex )
			{
			// Silently fail on dynamic property files!
			}
		}

	private static boolean
	loadPropertiesResource( final String name, final Properties props )
		{
		final InputStream	in;
		boolean		result = false;

		if ( debug )
			System.err.println
				("Load properties resource '" + name + '\'');

		try {
			in = ResourceUtilities.openNamedResource( name );
			props.load(in);
			in.close();
			result = true;
			}
		catch ( final IOException ex )
			{
			System.err.println
				( "ERROR loading properties resource '"
					+ name + "' - " + ex.getMessage() );
			}

		return result;
		}

	private static void
	loadPropertyResourceList(
			final String listPropName, final String rsrcPrefix, final Properties props )
		{
		final String rsrcListStr =
			getProperty( listPropName, null );

		if ( rsrcListStr != null )
			{
			final String[] rsrcList =
				StringUtilities.splitString( rsrcListStr, ":" );

			for ( int rIdx = 0
					; rsrcList != null && rIdx < rsrcList.length
						; ++rIdx )
				{
				final String rsrcTag = rsrcPrefix + rsrcList[rIdx];

				final String rsrcName =
					getProperty( rsrcTag, null );

				if ( rsrcName != null )
					{
					final boolean result =
						loadPropertiesResource
							( rsrcName, props );

					if ( ! result )
						{
						System.err.println
							("ERROR loading property resource '"
							 + rsrcName + '\'');
						}
					}
				}
			}
		}

	// UNDONE
	// This routine need to use JNDI (?) to get a 'global' property
	// file name (typically on a network mounted volume) to read,
	// which should in turn set the name of the local property file.
	// JNDI should also set some 'critical' properties, such as
	// the important OTA hostnames, service ports, etc.

	// REVIEW
	// UNDONE
	// This routine should have a 'filter' that filters out all
	// global properties that do not start with prefix?

	/**
	 * Load all related properties for this application.
	 * This class method will look for a global properties
	 * file, loading it if found, then looks for a local
	 * properties file and loads that.
	 */

	public static void
	loadProperties( final String packageName, final Properties appProps )
		{
		boolean			result;
		final File			propFile;
		String			propPath;
		final String			propName;
		String			rsrcName;

		if ( debug )
			{
			printContext( System.err );
			}

		final Properties sysProps =
			System.getProperties();

		if ( sysProps == null )
			return;

		defaultProperties( sysProps );

		//
		// ---- PROCESS THE DEFAULT PROPERTIES RESOURCE
		//
		rsrcName = defaultsResource;
		if ( rsrcName == null )
			{
			rsrcName =
				getProperty
					( DEFAULTS_RSRC_NAME, null );
			}

		if ( debug )
			System.err.println
				("Default Properties Resource '" + rsrcName + '\'');

		if ( rsrcName != null )
			{
			result =
				loadPropertiesResource
					( rsrcName, sysProps );

			System.err.println
				( "Loaded "
					+ ( result ? "the " : "no " )
					+ "default properties." );
			}

		//
		// ---- PROCESS THE APPLICATION DEFAULT PROPERTIES
		//
		if ( appProps != null )
			{
			if ( debug )
				System.err.println
					( "Adding application default properties." );

			addDefaultProperties
				( sysProps, appProps );
			}

		//
		// ---- PROCESS THE PREFIX PROPERTY
		//
		final String newPrefix = prefix;
		if ( debug )
			System.err.println
				("Prefix '" + newPrefix + '\'');

		if ( newPrefix == null )
			{
			getProperty
				(packageName + '.'
				 + PREFIX_PROPERTY, null );

			if ( newPrefix != null )
				{
				if ( debug )
					System.err.println
						("Prefix via property '" + newPrefix + '\'');

				setPropertyPrefix( newPrefix );
				if ( verbose )
					System.err.println
						("Property prefix set to '" + newPrefix + '\'');
				}
			}

		//
		// ---- PROCESS THE GLOBAL PROPERTIES RESOURCES
		//
		loadPropertyResourceList
			( GLOBAL_RSRCLIST_NAME,
				GLOBAL_RSRC_PREFIX, sysProps );

		//
		// ---- PROCESS THE GLOBAL DYNAMIC PROPERTY REGISTRATIONS
		//
		processDynamicProperties();

		//
		// ---- PROCESS THE LOCAL PROPERTIES FILE
		//
		propPath = localPropertyFile;
		if ( propPath == null )
			{
			propPath =
				getProperty
					( LOCAL_PROPERTY,
						LOCAL_DEFAULT );
			}

		if ( debug )
			System.err.println
				("Local property file '" + propPath + '\'');

		if ( propPath != null )
			{
			propFile = new File( propPath );
			if ( propFile.exists() )
				{
				result =
					loadPropertiesFile
						( propPath, sysProps, null );

				if ( ! result )
					{
					System.err.println
						("ERROR loading local property file '"
						 + propPath + '\'');
					}
				}
			}

		//
		// ---- PROCESS THE GLOBAL PROPERTIES RESOURCES
		//
		loadPropertyResourceList
			( APP_RSRCLIST_NAME,
				APP_RSRC_PREFIX, sysProps );
		}

	private static void
	processDynamicProperties()
		{
		//
		// First, register any dynamic property definitions
		// defined by global properties.
		//
		final Properties sysProps = System.getProperties();

		final String dynPropList =
			sysProps.getProperty( "global.dynamicPropList", null );

		if ( dynPropList != null )
			{
			final String[] dynList =
				StringUtilities.splitString( dynPropList, ":" );

			for ( final String dynName : dynList )
				{
				final String pathPropName =
					"global.dynamicPropFile." + dynName;

				String dynPath =
					sysProps.getProperty( pathPropName, null );

				if ( dynPath != null )
					{
					if ( dynPath.startsWith('~' + File.separator ) )
						{
						dynPath =
							sysProps.getProperty( "user.home", "" )
								+ dynPath.substring( 2 );
						}

					registerDynamicProperties
						( dynName, dynPath, new Properties() );
					}
				}
			}

		// Now, we do the actual loading of dynamic properties.
		for ( final String name : dynKeysTable.keySet() )
			{
			final String path =
				dynPathTable.get( name );

			loadDynamicProperties( name, path );
			}
		}

	private static void
	registerDynamicProperties(final String name, final String path, final Properties props)
		{
		dynPathTable.put( name, path );
		addDynamicPropertyKeys( name, props );
		}

	private static void
	addDynamicPropertyKeys( final String name, final Properties dynProps )
		{
		Vector<String> dynKeys =
			dynKeysTable.get( name );

		if ( dynKeys == null )
			{
			dynKeys =
				dynProps == null
					? new Vector( 0 )
					: new Vector( dynProps.size() );

			dynKeysTable.put( name, dynKeys );
			}

		if ( dynProps != null )
			{
			// Ensure all key names are
			for (final String keyName : dynProps.stringPropertyNames())
				{
				if ( ! dynKeys.contains( keyName ) )
					dynKeys.addElement( keyName );
				}
			}
		}

	/**
	 * This method expects the property keys to be
	 * <strong>normalized</strong>, meaning that they are
	 * the full property name with the prefix added on.
	 */

	private static void
	copyDynamicProperties( final String name, final Properties props )
		{
		final String path = dynPathTable.get( name );
		final Vector<String> keys = dynKeysTable.get(name );

		if ( keys == null || path == null )
			throw new NoSuchElementException
				("you have not registered the dynamic property "
				 + "package named '" + name + '\'');

		final Properties sysProps = System.getProperties();

		try {
			for ( final String key : props.stringPropertyNames() )
				{
				if ( key != null )
					{
					final String normalKey =
						normalizePropertyName( key );

					sysProps.setProperty(normalKey, props.getProperty(key));

					if ( ! keys.contains( normalKey ) )
						keys.addElement( normalKey );
					}
				}
			}
		catch ( final NoSuchElementException ex )
			{ }
		}

	private static void
	setDynamicProperties(final String name, final Properties props)
		{
		copyDynamicProperties( name, props );
		}

	/**
	 * This method expects the property keys to be <strong>not</strong>
	 * <em>normalized</em>, meaning that they are the full
	 * property name with the prefix added on.
	 */

	public static void
	setDynamicProperty( final String name, final String propName, final String propValue )
		{
		workingProps.clear();
			workingProps.setProperty(propName, propValue);
		setDynamicProperties
			( name, workingProps );
		}

	/**
	 * This method removes a property from the dynamic properties.
	 */

	public static void
	removeDynamicProperty( final String name, final String propName )
		{
		final String path = dynPathTable.get( name );
		final Vector<String> keys = dynKeysTable.get(name );

		if ( keys == null || path == null )
			throw new NoSuchElementException
				("you have not registered the dynamic property "
				 + "package named '" + name + '\'');

		final String normalKey =
			normalizePropertyName( propName );

		if ( keys.contains( normalKey ) )
			{
			keys.removeElement( normalKey );
			System.getProperties().remove( normalKey );
			}
		}

	public static void
	saveDynamicProperties( final String name )
		throws IOException
		{
		final String path = dynPathTable.get( name );
		final Vector<String> keys = dynKeysTable.get(name );

		if ( keys == null || path == null )
			throw new NoSuchElementException
				("you have not registered the dynamic property "
				 + "package named '" + name + '\'');

		final Properties dynProps = new Properties();
		final Properties sysProps = System.getProperties();

		final int count = keys.size();
		for ( int idx = 0 ; idx < count ; ++idx )
			{
			final String pName = keys.elementAt(idx);
			dynProps.setProperty(pName, sysProps.getProperty(pName));
			}

		saveDynamicPropFile( name, path, dynProps );
		}

	//
	// UNDONE
	// We should use an intermediate file and move at completion. This
	// would eliminate the file being trashed on an IOException.
	//
	private static void
	saveDynamicPropFile( final String name, final String path, final Properties dynProps )
		throws IOException
		{
		final String eol = System.getProperty( "line.separator", "\n" );
		final String comment = eol +
			"## --------------------  W A R N I N G  -------------------- " + eol +
			"#  This file is automatically generated." + eol +
			"#  Any changes you make to this file will be overwritten." + eol +
			"## ---------------------------------------------------------" + eol +
				       '#';

		final OutputStream out =
			new FileOutputStream( path );

			dynProps.setProperty("global.dynPropVersion." + name, DYNAMIC_PROPERTY_VERSION);

		dynProps.store( out, comment );

		out.close();
		}

	private static void
	printContext(final PrintStream out)
		{
		out.println
			("os.name    = '" + osname + '\'');
		out.println
			("user.name  = '" + userName + '\'');
		out.println
			("user.home  = '" + userHome + '\'');
		out.println
			("java.home  = '" + javaHome + '\'');

		out.println();

		out.println
			("prefix     = '" + prefix + '\'');
		out.println
			("osSuffix   = '" + osSuffix + '\'');
		out.println
			("userSuffix = '" + userSuffix + '\'');

		out.println();
		}

	public static void
	printUsage( final PrintStream out )
		{
		out.println
			( "Properties options:" );

		out.println
			( "   -propDebug             -- "
				+ "turns on debugging of property loading" );
		out.println
			( "   -propVerbose           -- "
				+ "turns on verbose messages during loading" );

		out.println
			( "   -propDefaults rsrcName -- "
				+ "sets default properties resource name" );
		out.println
			( "   -propFile path         -- "
				+ "sets application property file path" );

		out.println
			( "   -propOS suffix         -- "
				+ "sets the os suffix" );
		out.println
			( "   -propUser suffix       -- "
				+ "sets the user suffix" );
		out.println
			( "   -propPrefix prefix     -- "
				+ "sets application property prefix" );
		}

	public static String []
	processOptions( final String... args )
		{
		final Vector newArgs = new Vector( args.length );

		for ( int iArg = 0 ; iArg < args.length ; ++iArg )
			{
			if ( args[iArg].equals( "-propPrefix" )
						&& iArg + 1 < args.length )
				{
				setPropertyPrefix( args[++iArg] );
				}
			else if ( args[iArg].equals( "-propFile" )
						&& iArg + 1 < args.length )
				{
				setLocalPropertyFile( args[++iArg] );
				}
			else if ( args[iArg].equals( "-propDefaults" )
						&& iArg + 1 < args.length )
				{
				setDefaultsResource( args[++iArg] );
				}
			else if ( args[iArg].equals( "-propDebug" ) )
				{
				setDebug( true );
				}
			else if ( args[iArg].equals( "-propVerbose" ) )
				{
				setVerbose( true );
				}
			else if ( args[iArg].equals( "-propOS" )
						&& iArg + 1 < args.length )
				{
				setOSSuffix( args[++iArg] );
				}
			else if ( args[iArg].equals( "-propUser" )
						&& iArg + 1 < args.length )
				{
				setUserSuffix( args[++iArg] );
				}
			else
				{
				newArgs.addElement( args[iArg] );
				}
			}

		final String[] result = new String[ newArgs.size() ];
		for ( int i = 0 ; i < newArgs.size() ; ++i )
			result[i] = (String) newArgs.elementAt(i);

		return result;
		}

	}


