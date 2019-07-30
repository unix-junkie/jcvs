
package com.ice.util;

import java.awt.Component;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;


final class
MenuProperties {
	private MenuProperties() {
	}

	public static JPopupMenu
    loadPopupMenu( final String menuPropertyName, final ActionListener listener )
		{
		final JPopupMenu popup = new JPopupMenu();

		addMenuItems( popup, menuPropertyName, listener );

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
			final JComponent menu, final String menuPropertyName,
			final ActionListener listener)
		{
		final String[]	itemList;
		String		itemString;
		final String		menuString;
		String		itemNameStr;
		JMenuItem	mItem;

		menuString =
			UserProperties.getProperty
				( "menu." + menuPropertyName, null );

		if ( menuString == null )
			{
			ICETracer.traceWithStack
				( "Menu definition property '"
					+ menuPropertyName + "' is not defined." );
			return;
			}

		itemList = StringUtilities.splitString( menuString, ":" );

		if ( itemList != null )
			{
			for (final String item : itemList)
				{
				itemNameStr =
						"item." + menuPropertyName + '.' + item;

				itemString =
					UserProperties.getProperty( itemNameStr, null );

				if ( itemString == null )
					{
					ICETracer.traceWithStack
						( "Menu definition '" + menuPropertyName
							+ "' is missing item definition property '"
							+ itemNameStr + "'." );
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
						ICETracer.traceWithStack
							( "Menu '" + menuPropertyName
								+ "' Item '" + itemNameStr
								+ "' has invalid definition." );
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
								( subMenu, subMenuName, listener );

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


