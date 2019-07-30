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

import java.util.Hashtable;
import java.util.Map.Entry;

import com.ice.cvsc.CVSLog;


final
class		ProjectFrameMgr
	{
	private static final boolean			debug = false;
	private static final Hashtable<String, ProjectFrame>	frames = new Hashtable<>();

		private ProjectFrameMgr() {
		}


		public static void
	addProject( final ProjectFrame frame, final String localRootPath )
		{
		if ( debug )
			{
			CVSLog.logMsgStderr
				( "PROJECT_FRAME_MGR: ADD: " + localRootPath );
			}

		frames.put( localRootPath, frame );
		}

	public static void
	removeProject( final ProjectFrame frame, final String localRootPath )
		{
		if ( debug )
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
		for (final Entry<String, ProjectFrame> stringProjectFrameEntry : frames.entrySet())
			{
			final ProjectFrame frm = stringProjectFrameEntry.getValue();

			if ( frm == frame )
				{
				if ( debug )
					{
					CVSLog.logMsgStderr
						("PROJECT_FRAME_MGR: REMOVE: Matched " + stringProjectFrameEntry.getKey());
					}
				frames.remove(stringProjectFrameEntry.getKey());
				break;
				}
			}
		}

	public static boolean
	checkProjectOpen( final String localRootPath )
		{
		if ( debug )
			{
			CVSLog.logMsgStderr
				( "PROJECT_FRAME_MGR: CHECK: " + localRootPath );
			}

		final ProjectFrame frame = frames.get( localRootPath );

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
		return frames.values();
		}

	public static void
	closeAllProjects()
		{
		for (final Entry<String, ProjectFrame> stringProjectFrameEntry : frames.entrySet())
			{
			if ( debug )
				{
				CVSLog.logMsgStderr
					("PROJECT_FRAME_MGR: CLOSE: " + stringProjectFrameEntry.getKey());
				}

			final ProjectFrame frame =
					stringProjectFrameEntry.getValue();

			frame.dispose();
			}
		}

	}

