/*
** ResourceBundle Manager Utility class.
** Authored in 1999 by Timothy Gerard Endres.
**
** This Java Class source has been placed in the public domain.
**
*/

package com.ice.util;

import java.text.MessageFormat;
import java.util.Hashtable;
import java.util.MissingResourceException;
import java.util.ResourceBundle;


/**
 * This class attempts to manage a set of ResourceBundles. For example,
 * an application may have one set of resources for the GUI elements in
 * the application, and another set of resources for context help, and
 * yet another set for builtin scripts.
 *
 * Each ResourceManager instance is identified by an id. Class methods
 * provided to allow quick access to any ResourceManager by id.
 *
 * ResourceManager, aside from manager multiple instances by id, also
 * add an additional method beyond that provided by ResourceBundle -
 * getFormat(). This method is like getString() in that is returns a
 * String resource, however, getFormat() formats the resource using
 * arguments passed to the method. This makes it easy to use resources
 * for both constant strings and formatted strings.
 *
 * @version $Revision: 1.1 $
 * @author Timothy Gerard Endres,
 *  <a href="mailto:time@ice.com">time@ice.com</a>.
 */

public final
class		ResourceManager
	{
	public static final String		RCS_ID = "$Id: ResourceManager.java,v 1.1 2000/01/28 02:24:45 time Exp $";
	public static final String		RCS_REV = "$Revision: 1.1 $";

	/**
	 * The table of all ResourceManagers keyed by 'id'.
	 */
	private static Hashtable		bundles;

		/**
	 * The resource bundle.
	 */
	private ResourceBundle			rsrc;


	/**
	 * Initializes the class by instantiating the bundles Hashtable.
	 * Your application must call this class method only once, and
	 * before calling any other methods in this class.
	 */

	public static void
	initialize()
		{
		bundles = new Hashtable();
		}

	/**
	 * Load a PropertyResourceBundle using the name, and add it to the
	 * bundles Hashtable keyed by id.
	 *
	 * @param id The id of the ResourceManager. This is used as the
	 *  key into the bundles table.
	 * @param name The name of the ResourceManager. This is used to
	 *  load the resource bundle.
	 */
	public static void
	load( final String id, final String name )
		{
		try {
			final ResourceBundle rsrc = ResourceBundle.getBundle( name );
			final ResourceManager rMgr = new ResourceManager( name, rsrc );
			bundles.put( id, rMgr );
			}
		catch ( final MissingResourceException ex )
			{
			ex.printStackTrace();
			}
		}

	/**
	 * Get a ResourceManager keyed by id.
	 *
	 * @param id The id of the ResourceManager to be returned.
	 * @return The ResourceManager identied by id.
	 */
	public static ResourceManager
	get( final String id )
		{
		return (ResourceManager)
			bundles.get( id );
		}

	/**
	 * Put a ResourceManager into the bundles Hashtable, keyed by id.
	 *
	 * @param id The id used to identify this ResourceManager.
	 * @param rMgr The resource manager to be managed.
	 * @return The previous ResourceManager identied by id, or null.
	 */
	public static ResourceManager
	put( final String id, final ResourceManager rMgr )
		{
		return (ResourceManager)
			bundles.put( id, rMgr );
		}

	/**
	 * This constructor is not used.
	 */
	private
	ResourceManager()
		{
		}

	/**
	 * Construct a ResourceManager with the given name and ResourceBundle.
	 *
	 * @param name The (display) name of this resource bundle.
	 * @param rsrc The resource bundle to be managed.
	 */
	private ResourceManager(final String name, final ResourceBundle rsrc)
		throws MissingResourceException
		{
			/**
			 * The resource bundle's name. Used for easy identification.
			 */
			this.rsrc = rsrc;
		}

	/**
	 * Set the debugging flag for this ResourceManager. If debugging is
	 * set to true, debugging will be printed to System.err.
	 *
	 * @param debug The new debugging setting.
	 */
	public void
	setDebug( final boolean debug )
		{
			/**
			 * Set to true to get processing debugging on stderr.
			 */
		}

	/**
	 * Get a string resource from the ResourceBundle that this
	 * ResourceManager is managing.
	 *
	 * @param key The key of the resource to retrieve.
	 * @return The resource string.
	 */
	public String
	getString( final String key )
		{
		return this.rsrc.getString( key );
		}

	/**
	 * Format a string resource from the ResourceBundle that this
	 * ResourceManager is managing. The key is used to retrieve a
	 * resource that is the format, which is then formatted using
	 * the provided arguments.
	 *
	 * @param key The key of the resource that is the message format.
	 * @param args The arguments to be used to format the message.
	 * @return The formatted resource message.
	 */

	public String
	getFormat( final String key, final Object... args )
		{
		return MessageFormat.format
			( this.rsrc.getString( key ), args );
		}

	}

