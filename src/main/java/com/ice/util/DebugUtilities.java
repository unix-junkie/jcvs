/*
** Copyright (c) 1998 by Timothy Gerard Endres
** <mailto:time@ice.com>  <http://www.ice.com>
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

import java.awt.Component;
import java.awt.Container;
import java.io.PrintWriter;


final class
DebugUtilities
	{
		private DebugUtilities() {
		}

		private static void
	printClassHierarchy(
				Class aClass, final PrintWriter writer, final String prefix)
		{
		String subPrefix = "-->";

		for ( int i = 0 ; ; ++i )
			{
			writer.println
				(prefix + ' ' + subPrefix
				 + ' ' + aClass.getName() );

			aClass = aClass.getSuperclass();

			if ( aClass == Object.class )
				break;

			subPrefix = "--" + subPrefix;
			}
		}

	private static void
	printContainerComponents(
			final Container cont, final PrintWriter writer,
			final String prefix, final boolean recurse)
		{
		final Component[] comps = cont.getComponents();

		if ( comps.length < 1 )
			{
			writer.println
				( prefix + "Contains no components." );
			}

		for ( int i = 0 ; i < comps.length ; ++i )
			{
			printClassHierarchy
				( comps[i].getClass(), writer,
				  prefix + '[' + i + ']');

			if ( recurse )
				{
				final Class<Container> c = Container.class;
				if ( c.isAssignableFrom( comps[i].getClass() ) )
					{
					printContainerComponents(
							(Container)comps[i], writer,
							prefix + '[' + i + "] ", recurse );
					}
				}
			}
		}

	}

