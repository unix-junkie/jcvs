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
 * @version $Revision: 1.6 $
 * @author Timothy Gerard Endres,
 *  <a href="mailto:time@ice.com">time@ice.com</a>.
 */

interface	ConfigConstants
	{
	String		DEFAULT_MAILCAP_FILENAME = "/com/ice/jcvsii/mailcap.txt";
	String		DEFAULT_MIMETYPES_FILENAME = "/com/ice/jcvsii/mimetypes.txt";

	String		GLOBAL_TEMP_DIR = "global.temporaryDirectory";
	String		GLOBAL_CVS_LOG_FILE = "global.cvsLogFileName";
	String		GLOBAL_CVS_TIMEZONE = "global.timeStamp.timeZone";
	String		GLOBAL_LOAD_SERVERS = "global.loadDefaultServerDefs";
	String		GLOBAL_IGNORE_FILENAME = "global.ignoreFileName";
	String		GLOBAL_USER_IGNORES = "global.userIgnores";
	String		GLOBAL_RSH_COMMAND = "global.rshCommand";
	String		GLOBAL_SSH_COMMAND = "global.sshCommand";
	String		GLOBAL_SVRCMD_TABLE = "global.serverTable";
	String		GLOBAL_ALLOWS_FILE_GZIP = "global.allowGzipFileMode";
	String		GLOBAL_GZIP_STREAM_LEVEL = "global.gzipStreamLevel";
	String		GLOBAL_CVS_TRACE_ALL = "global.traceAll";
	String		GLOBAL_PROJECT_DEEP_DEBUG = "global.project.deepDebug";
	String		GLOBAL_PROJECT_DEBUG_ENTRYIO = "global.project.debugEntryIO";
	String		GLOBAL_RSH_PORT = "global.defPorts.server";
	String		GLOBAL_SSH_PORT = "global.defPorts.ext";
	String		GLOBAL_DIRECT_PORT = "global.defPorts.direct";
	String		GLOBAL_PSERVER_PORT = "global.defPorts.pserver";
	String		GLOBAL_MULTI_INTF = "global.multipleInterfaceSupport";

	String		PLAF_LOOK_AND_FEEL_CLASSNAME = "plaf.lookAndFeel.classname";


	int			EXEC_DEF_ENV_IDX = 0;
	int			EXEC_DEF_CMD_IDX = 1;

	String		GLOBAL_EXT_VERB_TABLE = "global.exec.verbs";

	String		GLOBAL_MAILCAP_FILE = "global.mailcap.fileName";
	String		GLOBAL_MIMETYPES_FILE = "global.mimetypes.fileName";

	String		MAIN_WINDOW_BOUNDS = "mainFrame.bounds";

	String		MAIN_PANEL_DIVIDER = "mainPanel.divider.location";

	// These are global project window properties...
	String		PROJECT_DOUBLE_CLICK_VERB = "project.tree.doubleClickVerb";
	String		PROJECT_DETAILS_TYPE = "project.details.content.type";
	String		PROJECT_TREE_FONT = "project.tree.font";
	String		PROJECT_TREE_LINESTYLE = "project.tree.linestyle";
	String		PROJECT_MODIFIED_TZ = "project.modified.tz";
	String		PROJECT_MODIFIED_FORMAT = "project.modified.format";

	// These are "per project" project window properties...
	String		PROJECT_WINDOW_BOUNDS = "project.bounds";
	String		PROJECT_NAME_WIDTH = "project.name.width";
	String		PROJECT_VERSION_WIDTH = "project.version.width";
	String		PROJECT_TAG_WIDTH = "project.tag.width";
	String		PROJECT_MODIFIED_WIDTH = "project.modified.width";

	String		OUTPUT_WINDOW_FONT = "outputFrame.font";
	String		OUTPUT_WINDOW_BOUNDS = "outputFrame.bounds";

	String		PRETTY_RAW_FONT = "prettyDiff.raw.font";
	String		PRETTY_DIFF_FONT = "prettyDiff.lbl.font";
	String		PRETTY_HEADER_FONT = "prettyDiff.header.font";
	String		PRETTY_TITLE_FONT = "prettyDiff.title.font";
	String		PRETTY_WINDOW_BOUNDS = "prettyDiff.bounds";

	String		SELECT_ENTRIES_HELP = "help.selectEntriesFile";

	String		WB_DET_TITLE_BG = "workBench.details.title.bg";
	String		WB_DET_TITLE_FONT = "workBench.details.title.font";
	String		WB_DET_TITLE_HEIGHT = "workBench.details.title.height";

	String		INFOPAN_METHOD = "info.method";
	String		INFOPAN_USER_NAME = "info.user";
	String		INFOPAN_SERVER_NAME = "info.server";
	String		INFOPAN_MODULE_NAME = "info.module";
	String		INFOPAN_REPOS_NAME = "info.repository";
	String		INFOPAN_EXPDIR_NAME = "info.exportdir";
	String		INFOPAN_ARGS_NAME = "info.arguments";

	String		IMPADDPAN_IGNORES = "addimport.user";
	String		IMPADDPAN_BINARIES = "addimport.server";
	String		IMPADDPAN_LOGMSG = "addimport.module";
	String		IMPADDPAN_VENDOR_TAG = "addimport.repository";
	String		IMPADDPAN_RELEASE_TAG = "addimport.exportdir";
	}

