/*
** Java CVS client application package.
** Copyright (c) 1997 by Timothy Gerard Endres
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


/**
 * The Configuration constants.
 *
 * @version $Revision: 1.5 $
 * @author Timothy Gerard Endres,
 *  <a href="mailto:time@ice.com">time@ice.com</a>.
 */

public
interface	ConfigConstants
	{
	public static final String		DEFAULT_MAILCAP_FILENAME = "/com/ice/jcvsii/mailcap.txt";
	public static final String		DEFAULT_MIMETYPES_FILENAME = "/com/ice/jcvsii/mimetypes.txt";

	public static final String		GLOBAL_TEMP_DIR = "global.temporaryDirectory";
	public static final String		GLOBAL_CVS_LOG_FILE = "global.cvsLogFileName";
	public static final String		GLOBAL_CVS_TIMEZONE = "global.timeStamp.timeZone";
	public static final String		GLOBAL_LOAD_SERVERS = "global.loadDefaultServerDefs";
	public static final String		GLOBAL_IGNORE_FILENAME = "global.ignoreFileName";
	public static final String		GLOBAL_USER_IGNORES = "global.userIgnores";
	public static final String		GLOBAL_RSH_COMMAND = "global.rshCommand";
	public static final String		GLOBAL_SSH_COMMAND = "global.sshCommand";
	public static final String		GLOBAL_SVRCMD_TABLE = "global.serverTable";
	public static final String		GLOBAL_ALLOWS_FILE_GZIP = "global.allowGzipFileMode";
	public static final String		GLOBAL_GZIP_STREAM_LEVEL = "global.gzipStreamLevel";
	public static final String		GLOBAL_CVS_TRACE_ALL = "global.traceAll";
	public static final String		GLOBAL_PROJECT_DEEP_DEBUG = "global.project.deepDebug";
	public static final String		GLOBAL_PROJECT_DEBUG_ENTRYIO = "global.project.debugEntryIO";
	public static final String		GLOBAL_RSH_PORT = "global.defPorts.server";
	public static final String		GLOBAL_SSH_PORT = "global.defPorts.ext";
	public static final String		GLOBAL_DIRECT_PORT = "global.defPorts.direct";
	public static final String		GLOBAL_PSERVER_PORT = "global.defPorts.pserver";
	public static final String		GLOBAL_MULTI_INTF = "global.multipleInterfaceSupport";

	public static final String		PLAF_LOOK_AND_FEEL_CLASSNAME = "plaf.lookAndFeel.classname";


	public static final int			EXEC_DEF_ENV_IDX = 0;
	public static final int			EXEC_DEF_CMD_IDX = 1;

	public static final String		GLOBAL_EXT_VERB_TABLE = "global.exec.verbs";

	public static final String		GLOBAL_MAILCAP_FILE = "global.mailcap.fileName";
	public static final String		GLOBAL_MIMETYPES_FILE = "global.mimetypes.fileName";

	public static final String		MAIN_WINDOW_BOUNDS = "mainFrame.bounds";

	public static final String		MAIN_PANEL_DIVIDER = "mainPanel.divider.location";

	// These are global project window properties...
	public static final String		PROJECT_DOUBLE_CLICK_VERB = "project.tree.doubleClickVerb";
	public static final String		PROJECT_DETAILS_TYPE = "project.details.content.type";
	public static final String		PROJECT_TREE_FONT = "project.tree.font";
	public static final String		PROJECT_TREE_LINESTYLE = "project.tree.linestyle";
	public static final String		PROJECT_MODIFIED_TZ = "project.modified.tz";
	public static final String		PROJECT_MODIFIED_FORMAT = "project.modified.format";

	// These are "per project" project window properties...
	public static final String		PROJECT_WINDOW_BOUNDS = "project.bounds";
	public static final String		PROJECT_NAME_WIDTH = "project.name.width";
	public static final String		PROJECT_VERSION_WIDTH = "project.version.width";
	public static final String		PROJECT_TAG_WIDTH = "project.tag.width";
	public static final String		PROJECT_MODIFIED_WIDTH = "project.modified.width";

	public static final String		OUTPUT_WINDOW_FONT = "outputFrame.font";
	public static final String		OUTPUT_WINDOW_BOUNDS = "outputFrame.bounds";

	public static final String		PRETTY_RAW_FONT = "prettyDiff.raw.font";
	public static final String		PRETTY_DIFF_FONT = "prettyDiff.lbl.font";
	public static final String		PRETTY_HEADER_FONT = "prettyDiff.header.font";
	public static final String		PRETTY_TITLE_FONT = "prettyDiff.title.font";
	public static final String		PRETTY_WINDOW_BOUNDS = "prettyDiff.bounds";

	public static final String		SELECT_ENTRIES_HELP = "help.selectEntriesFile";

	public static final String		WB_DET_TITLE_BG = "workBench.details.title.bg";
	public static final String		WB_DET_TITLE_FONT = "workBench.details.title.font";
	public static final String		WB_DET_TITLE_HEIGHT = "workBench.details.title.height";

	public static final String		INFOPAN_METHOD = "info.method";
	public static final String		INFOPAN_USER_NAME = "info.user";
	public static final String		INFOPAN_SERVER_NAME = "info.server";
	public static final String		INFOPAN_MODULE_NAME = "info.module";
	public static final String		INFOPAN_REPOS_NAME = "info.repository";
	public static final String		INFOPAN_EXPDIR_NAME = "info.exportdir";
	public static final String		INFOPAN_ARGS_NAME = "info.arguments";

	public static final String		IMPADDPAN_IGNORES = "addimport.user";
	public static final String		IMPADDPAN_BINARIES = "addimport.server";
	public static final String		IMPADDPAN_LOGMSG = "addimport.module";
	public static final String		IMPADDPAN_VENDOR_TAG = "addimport.repository";
	public static final String		IMPADDPAN_RELEASE_TAG = "addimport.exportdir";
	}

