
package com.ice.pref;

import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.ice.util.StringUtilities;


public
class		MenuPrefs
extends		Object
	{
	static public JPopupMenu
    loadPopupMenu(
			final UserPrefs prefs, final String menuPropertyName,
			final ActionListener listener )
		{
		final JPopupMenu popup = new JPopupMenu();

		MenuPrefs.addMenuItems
			( prefs, popup, menuPropertyName, listener );

		return popup;
		}

	static public void
	addGenericItem( final JComponent menu, final JComponent item )
		{
		if ( menu instanceof JMenu )
			{
			final JMenu jm = (JMenu) menu;

			if ( item == null )
				jm.addSeparator();
			else
				jm.add( item );
			}
		else
			{
			final JPopupMenu jp = (JPopupMenu) menu;

			if ( item == null )
				jp.addSeparator();
			else
				jp.add( item );
			}
		}

	static public void
	addMenuItems(
			final UserPrefs prefs, final JComponent menu,
			final String menuPropertyName, final ActionListener listener )
		{
		String[]	itemList;
		String		itemString;
		String		menuString;
		String		itemNameStr;
		JMenuItem	mItem;

		menuString =
			prefs.getProperty
				( "menu." + menuPropertyName, null );

		if ( menuString == null )
			{
			new Exception
				( "Menu definition property '"
					+ menuPropertyName + "' is not defined." ).
				printStackTrace( System.err );
			return;
			}

		itemList = StringUtilities.splitString( menuString, ":" );

		if ( itemList != null )
			{
			for ( final String item : itemList )
				{
				itemNameStr =
					"item." + menuPropertyName + "." + item;

				itemString =
					prefs.getProperty( itemNameStr, null );

				if ( itemString == null )
					{
					new Exception
						( "Menu definition '" + menuPropertyName
							+ "' is missing item definition property '"
							+ itemNameStr + "'." ).
						printStackTrace( System.err );
					}
				else
					{
					final int colonIdx = itemString.indexOf( ':' );

					if ( itemString.equals( "-" ) )
						{
						MenuPrefs.addGenericItem( menu, null );
						}
					else if ( colonIdx < 0 )
						{
						new Exception
							( "Menu '" + menuPropertyName
								+ "' Item '" + itemNameStr
								+ "' has invalid definition." ).
						printStackTrace( System.err );
						}
					 else
						{
						final String title =
							itemString.substring( 0, colonIdx );

						final String command =
							itemString.substring( colonIdx + 1 );

						if ( command.equals( "@" ) )
							{
							final JMenu subMenu = new JMenu( title );

							final String subMenuName =
								menuPropertyName + "." + item;

							MenuPrefs.addMenuItems
								( prefs, subMenu, subMenuName, listener );

							MenuPrefs.addGenericItem( menu, subMenu );
							}
						else if ( title.equals( "-" ) )
							{
							MenuPrefs.addGenericItem( menu, null );
							}
						else
							{
							mItem = new JMenuItem( title );

							if ( listener != null )
								{
								mItem.addActionListener( listener );
								mItem.setActionCommand( command );
								}

							MenuPrefs.addGenericItem( menu, mItem );
							}
						}
					} // itemString != null
				} // foreach item
			} // itemList != null
		}
	}
