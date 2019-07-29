/******************************************************************************
 *
 * Copyright (c) 1999-2001 AppGate AB. All Rights Reserved.
 * 
 * This file contains Original Code and/or Modifications of Original Code as
 * defined in and that are subject to the MindTerm Public Source License,
 * Version 1.3, (the 'License'). You may not use this file except in compliance
 * with the License.
 * 
 * You should have received a copy of the MindTerm Public Source License
 * along with this software; see the file LICENSE.  If not, write to
 * AppGate AB, Stora Badhusgatan 18-20, 41121 Goteborg, SWEDEN
 *
 *****************************************************************************/

package com.mindbright.terminal;

import java.util.Hashtable;
import java.util.Properties;
import java.util.Enumeration;
import java.awt.Toolkit;

// !!! OUCH KLUDGE

public abstract class TerminalDefProps {

    // !!! OUCH (we don't want to drag in TerminalXTerm...
    //
    public final static String[] terminalTypes = {
	"xterm", "linux", "scoansi",  "att6386", "sun", "aixterm",
	"vt220", "vt100", "ansi",  "vt52",

	"xterm-color", "linux-lat", "", "at386", "", "", "vt320", "vt102"
    };
    public static String listAvailableTerminalTypes() {
	int i;
	String list = " ";
	for(i = 0; i < terminalTypes.length; i++)
	    list += terminalTypes[i] + " ";
	return list;
    }

    static public final int PROP_NAME    = 0;
    static public final int PROP_VALUE   = 1;
    static public Properties defaultProperties = new Properties();
    static public Hashtable  oldPropNames      = new Hashtable();
    static public final String[][] defaultPropDesc = {
	// Options
	{ "rev-video", "false"          },
	{ "autowrap", "true"            },
	{ "rev-autowrap", "false"       },
	{ "insert-mode", "false"        },
	{ "auto-linefeed", "false"      },
	{ "repos-input", "true"         },
	{ "repos-output", "true"        },
	{ "visible-cursor", "true"      },
	{ "local-echo", "false"         },
	{ "visual-bell", "false"        },
	{ "map-ctrl-space", "true"      },
	{ "80x132-toggle", "false"      },
	{ "80x132-enable", "true"       },

	{ "local-pgkeys", "false"       },
	{ "copy-crnl", "false"          },
	{ "ascii-line", "false"         },
	{ "copy-select", "true"         },

	// Settings
	{ "font-name", defaultFont()    },
	{ "font-size", "12"             },
	{ "geometry", "80x24"           },
	{ "term-type", terminalTypes[0] },
	{ "save-lines", "512"           },
	{ "scrollbar", "right"          },
	{ "bg-color", "white"           },
	{ "fg-color", "black"           },
	{ "cursor-color", "i_blue"      },
	{ "resize-gravity", "bottom"    },
	{ "backspace-send", "DEL"       },
	{ "delete-send", "DEL"          },
	{ "select-delim", "\" \""       },
	{ "paste-button", "middle"      },
    };
    static {
	for(int i = 0; i < defaultPropDesc.length; i++)
	    defaultProperties.put(defaultPropDesc[i][PROP_NAME], defaultPropDesc[i][PROP_VALUE]);

	oldPropNames.put("rv", "rev-video");
	oldPropNames.put("aw", "autowrap");
	oldPropNames.put("rw", "rev-autowrap");
	oldPropNames.put("im", "insert-mode");
	oldPropNames.put("al", "auto-linefeed");
	oldPropNames.put("sk", "repos-input");
	oldPropNames.put("si", "repos-output");
	oldPropNames.put("vi", "visible-cursor");
	oldPropNames.put("le", "local-echo");
	oldPropNames.put("vb", "visual-bell");
	oldPropNames.put("ct", "map-ctrl-space");
	oldPropNames.put("dc", "80x132-toggle");
	oldPropNames.put("da", "80x132-enable");
	oldPropNames.put("lp", "local-pgkeys");
	oldPropNames.put("sc", "copy-crnl");
	oldPropNames.put("ad", "ascii-line");
	oldPropNames.put("cs", "copy-select");
	oldPropNames.put("fn", "font-name");
	oldPropNames.put("fs", "font-size");
	oldPropNames.put("gm", "geometry");
	oldPropNames.put("te", "term-type");
	oldPropNames.put("sl", "save-lines");
	oldPropNames.put("sb", "scrollbar");
	oldPropNames.put("bg", "bg-color");
	oldPropNames.put("fg", "fg-color");
	oldPropNames.put("cc", "cursor-color");
	oldPropNames.put("rg", "resize-gravity");
	oldPropNames.put("bs", "backspace-send");
	oldPropNames.put("de", "delete-send");
	oldPropNames.put("sd", "select-delim");
	oldPropNames.put("pb", "paste-button");
    }

    public static String backwardCompatProp(String key) {
	String newName = (String)oldPropNames.get(key);
	if(newName != null) {
	    key = newName;
	}
	return key;
    }

    public static void setAsDefault(Properties props) {
	Enumeration enum = props.keys();
	while(enum.hasMoreElements()) {
	    String name  = (String)enum.nextElement();
	    String value = props.getProperty(name);
	    name = backwardCompatProp(name);
	    defaultProperties.put(name, value);
	}
    }

    public static String[] systemFonts;
    public static String fontList() {
	if(systemFonts == null)
	    systemFonts = Toolkit.getDefaultToolkit().getFontList();
	String list = "";
	for(int i = 0; i < systemFonts.length; i++) {
	    list += systemFonts[i];
	    if(i < systemFonts.length - 1)
		list += ", ";
	}
	return list;
    }

    public static String defaultFont() {
	if(fontExists("monospaced"))
	    return "Monospaced";
	if(fontExists("courier"))
	    return "Courier";
	if(fontExists("dialoginput"))
	    return "DialogInput";
	return systemFonts[0];
    }

    public static boolean fontExists(String font) {
	int i;
	if(systemFonts == null)
	    systemFonts = Toolkit.getDefaultToolkit().getFontList();
	for(i = 0; i < systemFonts.length; i++) {
	    if(systemFonts[i].equalsIgnoreCase(font))
		break;
	}
	if(i == systemFonts.length)
	    return false;
	return true;
    }

    public static boolean isProperty(String key) {
	key = backwardCompatProp(key);
	return defaultProperties.containsKey(key);
    }
}

