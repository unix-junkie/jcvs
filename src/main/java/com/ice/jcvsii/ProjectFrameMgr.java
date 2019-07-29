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

package com.ice.jcvsii;

import java.util.Enumeration;
import java.util.Hashtable;

import com.ice.cvsc.CVSLog;


public
class		ProjectFrameMgr
	{
	private static boolean			debug = false;
	private static Hashtable<String, ProjectFrame>	frames = new Hashtable<String, ProjectFrame>();


	public static void
	addProject( final ProjectFrame frame, final String localRootPath )
		{
		if ( ProjectFrameMgr.debug )
			{
			CVSLog.logMsgStderr
				( "PROJECT_FRAME_MGR: ADD: " + localRootPath );
			}

		ProjectFrameMgr.frames.put( localRootPath, frame );
		}

	public static void
	removeProject( final ProjectFrame frame, final String localRootPath )
		{
		if ( ProjectFrameMgr.debug )
			{
			CVSLog.logMsgStderr
				( "PROJECT_FRAME_MGR: REMOVE: " + localRootPath );
			}

		// NOTE
		// Because the user can open subtrees, we can end up with a
		// case where the localRootPath is no longer the path that
		// was opened, because the localRoot was adjusted to point
		// at the root of the module. Ergo, we must remove by direct
		// comparison over the entire Hashtable.
		//
		final Enumeration keys = ProjectFrameMgr.frames.keys();
		for ( ; keys.hasMoreElements() ; )
			{
			final String key = (String) keys.nextElement();

			final ProjectFrame frm = ProjectFrameMgr.frames.get( key );

			if ( frm == frame )
				{
				if ( ProjectFrameMgr.debug )
					{
					CVSLog.logMsgStderr
						( "PROJECT_FRAME_MGR: REMOVE: Matched " + key );
					}
				ProjectFrameMgr.frames.remove( key );
				break;
				}
			}
		}

	public static boolean
	checkProjectOpen( final String localRootPath )
		{
		if ( ProjectFrameMgr.debug )
			{
			CVSLog.logMsgStderr
				( "PROJECT_FRAME_MGR: CHECK: " + localRootPath );
			}

		final ProjectFrame frame = ProjectFrameMgr.frames.get( localRootPath );

		if ( frame != null )
			{
			if ( frame.isShowing() )
				{
				frame.toFront();
				frame.requestFocus();
				}
			}

		return frame != null;
		}

	public static Iterable<ProjectFrame>
	enumerateProjectFrames()
		{
		return ProjectFrameMgr.frames.values();
		}

	public static void
	closeAllProjects()
		{
		for ( final String key : ProjectFrameMgr.frames.keySet() )
			{
			if ( ProjectFrameMgr.debug )
				{
				CVSLog.logMsgStderr
					( "PROJECT_FRAME_MGR: CLOSE: " + key );
				}

			final ProjectFrame frame =
				ProjectFrameMgr.frames.get( key );

			frame.dispose();
			}
		}

	}

