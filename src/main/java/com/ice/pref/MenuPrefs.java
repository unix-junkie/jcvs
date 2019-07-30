
package com.ice.pref;

import java.awt.Component;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.ice.util.StringUtilities;


public final
class		MenuPrefs {
	private MenuPrefs() {
	}

	public static JPopupMenu
    loadPopupMenu(
			final UserPrefs prefs, final String menuPropertyName,
			final ActionListener listener )
		{
		final JPopupMenu popup = new JPopupMenu();

		addMenuItems
			( prefs, popup, menuPropertyName, listener );

		return popup;
		}

	private static void
	addGenericItem(final JComponent menu, final Component item)
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

	private static void
	addMenuItems(
			final UserPrefs prefs, final JComponent menu,
			final String menuPropertyName, final ActionListener listener)
		{
		final String[]	itemList;
		String		itemString;
		final String		menuString;
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
						"item." + menuPropertyName + '.' + item;

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
						addGenericItem( menu, null );
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
							final JComponent subMenu = new JMenu(title );

							final String subMenuName =
									menuPropertyName + '.' + item;

							addMenuItems
								( prefs, subMenu, subMenuName, listener );

							addGenericItem( menu, subMenu );
							}
						else if ( title.equals( "-" ) )
							{
							addGenericItem( menu, null );
							}
						else
							{
							mItem = new JMenuItem( title );

							if ( listener != null )
								{
								mItem.addActionListener( listener );
								mItem.setActionCommand( command );
								}

							addGenericItem( menu, mItem );
							}
						}
					} // itemString != null
				} // foreach item
			} // itemList != null
		}
	}
