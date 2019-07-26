
package com.ice.pref;

import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JComponent;
import javax.swing.JMenuItem;

import com.ice.util.StringUtilities;


public
class		MenuPrefs
extends		Object
	{
	static public JPopupMenu
    loadPopupMenu(
			UserPrefs prefs, String menuPropertyName,
			ActionListener listener )
		{
		JPopupMenu popup = new JPopupMenu();

		MenuPrefs.addMenuItems
			( prefs, popup, menuPropertyName, listener );

		return popup;
		}

	static public void
	addGenericItem( JComponent menu, JComponent item )
		{
		if ( menu instanceof JMenu )
			{
			JMenu jm = (JMenu) menu;

			if ( item == null )
				jm.addSeparator();
			else
				jm.add( item );
			}
		else
			{
			JPopupMenu jp = (JPopupMenu) menu;

			if ( item == null )
				jp.addSeparator();
			else
				jp.add( item );
			}
		}

	static public void
	addMenuItems(
			UserPrefs prefs, JComponent menu,
			String menuPropertyName, ActionListener listener )
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
			(new Exception
				( "Menu definition property '"
					+ menuPropertyName + "' is not defined." )).
				printStackTrace( System.err );
			return;
			}

		itemList = StringUtilities.splitString( menuString, ":" );

		if ( itemList != null )
			{
			for ( int iIdx = 0 ; iIdx < itemList.length ; ++iIdx )
				{
				itemNameStr =
					"item." + menuPropertyName + "." + itemList[iIdx];

				itemString =
					prefs.getProperty( itemNameStr, null );

				if ( itemString == null )
					{
					(new Exception
						( "Menu definition '" + menuPropertyName
							+ "' is missing item definition property '"
							+ itemNameStr + "'." )).
						printStackTrace( System.err );
					}
				else
					{
					int colonIdx = itemString.indexOf( ':' );

					if ( itemString.equals( "-" ) )
						{
						MenuPrefs.addGenericItem( menu, null );
						}
					else if ( colonIdx < 0 )
						{
						(new Exception
							( "Menu '" + menuPropertyName
								+ "' Item '" + itemNameStr
								+ "' has invalid definition." )).
						printStackTrace( System.err );
						}
					 else
						{
						String title =
							itemString.substring( 0, colonIdx );

						String command =
							itemString.substring( colonIdx + 1 );

						if ( command.equals( "@" ) )
							{
							JMenu subMenu = new JMenu( title );

							String subMenuName =
								menuPropertyName + "." + itemList[iIdx];

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
