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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.awt.*; 
import java.awt.event.*; 

import com.mindbright.gui.AWTConvenience;

public final class TerminalMenuHandlerFull extends TerminalMenuHandler 
    implements ActionListener, ItemListener
{
    protected final static int ACT_SETTINGS    = 0;
    protected final static int ACT_COLORS      = 1;
    protected final static int ACT_MISC        = 2;
    protected final static int ACT_FIND_CANCEL = 3;
    protected final static int ACT_FIND        = 4;

    private class Actions implements ActionListener, ItemListener {
	private int       action;

	public Actions(int action) {
	    this.action = action;
	}

	public void actionPerformed(ActionEvent e) {
	    switch(action) {
	    case ACT_SETTINGS:
		try {
		    term.setProperty("te", te[choiceTE.getSelectedIndex()]);
		    term.setProperty("fn", fn[choiceFN.getSelectedIndex()]);
		    term.setProperty("fs", textFS.getText());
		    term.setProperty("sb", sb[choiceSB.getSelectedIndex()]);
		    term.setProperty("sl", textSL.getText());
		    term.setProperty("gm", textCols.getText() + "x" + textRows.getText());

		    settingsDialog.setVisible(false);
		} catch (Exception ee) {
		    lblAlert.setText(ee.getMessage());
		}
		break;
	    case ACT_COLORS:
		try {
		    term.setProperty("fg", getSelectedColor(choiceFG, textFG));
		    term.setProperty("bg", getSelectedColor(choiceBG, textBG));
		    term.setProperty("cc", getSelectedColor(choiceCC, textCC));
		    colorsDialog.setVisible(false);
		} catch (Exception ee) {
		    lblAlertC.setText(ee.getMessage());
		}
		break;
	    case ACT_MISC:
		try {
		    term.setProperty("pb", pb[choicePB.getSelectedIndex()]);
		    term.setProperty("rg", rg[choiceRG.getSelectedIndex()]);
		    term.setProperty("sd", textSD.getText());
		    if(cbBS.getState())
			term.setProperty("bs", "DEL");
		    else
			term.setProperty("bs", "BS");
		    if(cbDEL.getState())
			term.setProperty("de", "BS");
		    else
			term.setProperty("de", "DEL");

		    term.setProperty("lp", String.valueOf(cbLocPG.getState()));
		    term.setProperty("sc", String.valueOf(cbCpWinCR.getState()));
		    term.setProperty("cs", String.valueOf(cbCpOnSel.getState()));
		    term.setProperty("ad", String.valueOf(cbAsciiLD.getState()));

		    settingsDialog2.setVisible(false);
		} catch (Exception ee) {
		    // !!! REMOVE
		    // Can't happen...
		    ee.printStackTrace();
		    System.out.println("Fatal error in dialog: " + ee);
		}
		break;
	    case ACT_FIND_CANCEL:
		findDialog.setVisible(false);
		if(findLen > 0) {
		    term.clearSelection(curFindRow, curFindCol, curFindRow, curFindCol + findLen - 1);
		}
		curFindRow = 0;
		curFindCol = 0;
		findLen    = 0;
		break;
	    case ACT_FIND:
		String txt = findText.getText();
		if(txt != null && txt.length() > 0) {
		    doFind();
		}
		break;
	    }
	}

	public void itemStateChanged(ItemEvent e) {
	    updateColors();
	}

    }

    TerminalWin term;
    Menu        fileMenu;
    Menu        editMenu;
    Menu        optionsMenu;
    String      titleName;

    Object[][]  menuItems;

    TerminalMenuListener listener;

    final static String[] settingsMenu = { "Terminal Settings",
					   "Emulation", "Resize gravity", "Font",
					   "Savelines", "Scrollbar", "Colors",
					   "Backspace"
    };

    final static int MENU_FILE     = 0;
    final static int MENU_EDIT     = 1;
    final static int MENU_OPTIONS  = 2;

    final static int M_FILE_CAPTURE = 1;
    final static int M_FILE_SEND    = 2;
    final static int M_FILE_CLOSE   = 4;

    final static int M_EDIT_COPY    = 1;
    final static int M_EDIT_PASTE   = 2;
    final static int M_EDIT_CPPASTE = 3;
    final static int M_EDIT_SELALL  = 4;
    final static int M_EDIT_FIND    = 5;
    final static int M_EDIT_CLS     = 7;
    final static int M_EDIT_CLEARSB = 8;
    final static int M_EDIT_VTRESET = 9;

    final static String[][] menuTexts = {
	{ "File", 
	  "_Capture To File...", "Send ASCII File...", null, "Close"
	},
	{ "Edit",
	  "Copy Ctrl+Ins", "Paste Shift+Ins", "Copy & Paste", "Select All",
	  "Find...", null,
	  "Clear Screen", "Clear Scrollback", "VT Reset"
	},
	{ "VT Options",
	  "_Reverse Video", "_Auto Wraparound", "_Reverse Wraparound",
	  "_Insert mode", "_Auto Linefeed",
	  "_Scroll to Bottom On Key Press",
	  "_Scroll to Bottom On Tty Output",
	  "_Visible Cursor", "_Local Echo",
	  "_Visual Bell",	"_Map <CTRL>+<SPC> To ^@ (<NUL>)",
	  "_Toggle 80/132 Columns", "_Enable 80/132 Switching",
	}
    };

    final static int NO_SHORTCUT = -1;
    final static int[][] menuShortCuts = {
	{ NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT, KeyEvent.VK_E },

	{ NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT, KeyEvent.VK_A,
	  KeyEvent.VK_F, NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT },

	{ NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT,
	  NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT,
	  NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT,
	  NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT },
    };

    // !!! OUCH
    public TerminalMenuHandlerFull() {
	this("MindTerm");
    }

    public TerminalMenuHandlerFull(String titleName) {
	this.titleName = titleName;
    }

    public void setTerminalWin(TerminalWin term) {
	this.term = term;
    }

    public void setTerminalMenuListener(TerminalMenuListener listener) {
	this.listener = listener;
    }

    public void updateSelection(boolean selectionAvailable) {
	((MenuItem)menuItems[MENU_EDIT][M_EDIT_COPY]).setEnabled(
								 selectionAvailable);
	((MenuItem)menuItems[MENU_EDIT][M_EDIT_CPPASTE]).setEnabled(
								    selectionAvailable);
    }

    public void update() {
	if(listener != null) {
	    listener.update();
	}
    }


    Dialog settingsDialog;
    Choice choiceTE, choiceFN, choiceSB;
    TextField textFS, textRows, textCols, textInitPos;
    Label lblAlert;
    final static String[] sb = { "left", "right", "none" };
    final static String[] te = TerminalXTerm.getTerminalTypes();
    final static String[] fn = Toolkit.getDefaultToolkit().getFontList();
    public final void termSettingsDialog() {
	if(settingsDialog == null) {
	    settingsDialog = new Dialog(term.ownerFrame, settingsMenu[0], true);

	    ItemListener       ilC;
	    Label              lbl;
	    GridBagLayout      grid  = new GridBagLayout();
	    GridBagConstraints gridc = new GridBagConstraints();
	    settingsDialog.setLayout(grid);

	    gridc.insets = new Insets(4, 4, 4, 4);
	    gridc.fill   = GridBagConstraints.NONE;
	    gridc.anchor = GridBagConstraints.WEST;

	    gridc.gridy = 0;
	    gridc.gridwidth = 6;
	    lbl = new Label("Terminal type:");
	    grid.setConstraints(lbl, gridc);
	    settingsDialog.add(lbl);
	    choiceTE = AWTConvenience.newChoice(te);
	    grid.setConstraints(choiceTE, gridc);
	    settingsDialog.add(choiceTE);

	    gridc.gridy = 1;
	    gridc.gridwidth = 4;
	    lbl = new Label("Columns:");
	    grid.setConstraints(lbl, gridc);
	    settingsDialog.add(lbl);

	    gridc.gridwidth = 2;
	    textCols = new TextField("", 3);
	    grid.setConstraints(textCols, gridc);
	    settingsDialog.add(textCols);

	    lbl = new Label("Rows:");
	    grid.setConstraints(lbl, gridc);
	    settingsDialog.add(lbl);

	    textRows = new TextField("", 3);
	    grid.setConstraints(textRows, gridc);
	    settingsDialog.add(textRows);

	    gridc.gridy = 2;
	    gridc.gridwidth = 2;
	    lbl = new Label("Font:");
	    grid.setConstraints(lbl, gridc);
	    settingsDialog.add(lbl);

	    gridc.gridwidth = 6;
	    choiceFN = AWTConvenience.newChoice(fn);
	    grid.setConstraints(choiceFN, gridc);
	    settingsDialog.add(choiceFN);

	    gridc.gridwidth = 2;
	    textFS = new TextField("", 3);
	    grid.setConstraints(textFS, gridc);
	    settingsDialog.add(textFS);

	    gridc.gridy = 3;
	    gridc.gridwidth = 6;
	    lbl = new Label("Scrollback buffer:");
	    grid.setConstraints(lbl, gridc);
	    settingsDialog.add(lbl);
	    textSL = new TextField("", 4);
	    grid.setConstraints(textSL, gridc);
	    settingsDialog.add(textSL);

	    gridc.gridy = 4;
	    lbl = new Label("Scrollbar position:");
	    grid.setConstraints(lbl, gridc);
	    settingsDialog.add(lbl);
	    choiceSB = AWTConvenience.newChoice(sb);
	    grid.setConstraints(choiceSB, gridc);
	    settingsDialog.add(choiceSB);

	    lblAlert = new Label("", Label.CENTER);
	    gridc.insets = new Insets(0, 0, 0, 0);
	    gridc.gridy = 5;
	    gridc.fill  = GridBagConstraints.HORIZONTAL;
	    gridc.gridwidth = GridBagConstraints.REMAINDER;
	    gridc.anchor = GridBagConstraints.CENTER;
	    grid.setConstraints(lblAlert, gridc);
	    settingsDialog.add(lblAlert);

	    Panel bp = new Panel(new FlowLayout());

	    Button b;
	    bp.add(b = new Button("OK"));
	    b.addActionListener(new Actions(ACT_SETTINGS));
	    bp.add(b = new Button("Cancel"));
	    b.addActionListener(new AWTConvenience.CloseAction(settingsDialog));

	    gridc.gridy = 6;
	    grid.setConstraints(bp, gridc);
	    settingsDialog.add(bp);

	    settingsDialog.addWindowListener(new AWTConvenience.CloseAdapter(b));

	    AWTConvenience.setBackgroundOfChildren(settingsDialog);

	    settingsDialog.setResizable(true);
	    settingsDialog.pack();
	}

	choiceTE.select(term.getProperty("te"));
	choiceFN.select(term.getProperty("fn"));
	textFS.setText(term.getProperty("fs"));
	textCols.setText(String.valueOf(term.cols()));
	textRows.setText(String.valueOf(term.rows()));
	choiceSB.select(term.getProperty("sb"));
	textSL.setText(term.getProperty("sl"));

	lblAlert.setText("");
	AWTConvenience.placeDialog(settingsDialog);

	choiceTE.requestFocus();
	settingsDialog.setVisible(true);
    }

    TextField textFG, textBG, textCC;
    Choice choiceFG, choiceBG, choiceCC;
    Dialog colorsDialog;
    Label lblAlertC;
    public final void termColorsDialog() {
	if(colorsDialog == null) {
	    colorsDialog = new Dialog(term.ownerFrame, "Terminal Colors", true);

	    ItemListener       ilC;
	    Label              lbl;
	    GridBagLayout      grid  = new GridBagLayout();
	    GridBagConstraints gridc = new GridBagConstraints();
	    colorsDialog.setLayout(grid);

	    gridc.insets = new Insets(4, 4, 4, 4);
	    gridc.fill   = GridBagConstraints.NONE;
	    gridc.anchor = GridBagConstraints.WEST;

	    gridc.gridy = 0;
	    gridc.gridwidth = 10;
	    lbl = new Label("Foreground color:");
	    grid.setConstraints(lbl, gridc);
	    colorsDialog.add(lbl);

	    gridc.gridy = 1;
	    gridc.gridwidth = 6;
	    choiceFG = new Choice();
	    grid.setConstraints(choiceFG, gridc);
	    colorsDialog.add(choiceFG);
	    choiceFG.addItemListener(ilC = new Actions(-1));

	    textFG = new TextField("", 10);
	    grid.setConstraints(textFG, gridc);
	    colorsDialog.add(textFG);

	    gridc.gridy = 2;
	    lbl = new Label("Background color:");
	    grid.setConstraints(lbl, gridc);
	    colorsDialog.add(lbl);

	    gridc.gridy = 3;
	    choiceBG = new Choice();
	    grid.setConstraints(choiceBG, gridc);
	    colorsDialog.add(choiceBG);
	    choiceBG.addItemListener(ilC);

	    textBG = new TextField("", 10);
	    grid.setConstraints(textBG, gridc);
	    colorsDialog.add(textBG);

	    gridc.gridy = 4;
	    lbl = new Label("Cursor color:");
	    grid.setConstraints(lbl, gridc);
	    colorsDialog.add(lbl);

	    gridc.gridy = 5;
	    choiceCC = new Choice();
	    grid.setConstraints(choiceCC, gridc);
	    colorsDialog.add(choiceCC);
	    choiceCC.addItemListener(ilC);

	    textCC = new TextField("", 10);
	    grid.setConstraints(textCC, gridc);
	    colorsDialog.add(textCC);

	    lblAlertC = new Label("", Label.CENTER);
	    gridc.insets = new Insets(0, 0, 0, 0);
	    gridc.gridy = 6;
	    gridc.fill  = GridBagConstraints.HORIZONTAL;
	    gridc.gridwidth = GridBagConstraints.REMAINDER;
	    gridc.anchor = GridBagConstraints.CENTER;
	    grid.setConstraints(lblAlertC, gridc);
	    colorsDialog.add(lblAlertC);

	    Panel bp = new Panel(new FlowLayout());

	    Button b;
	    bp.add(b = new Button("OK"));
	    b.addActionListener(new Actions(ACT_COLORS));
	    bp.add(b = new Button("Cancel"));
	    b.addActionListener(new AWTConvenience.CloseAction(colorsDialog));

	    gridc.gridy = 7;
	    grid.setConstraints(bp, gridc);
	    colorsDialog.add(bp);

	    fillChoices();

	    colorsDialog.addWindowListener(new AWTConvenience.CloseAdapter(b));

	    AWTConvenience.setBackgroundOfChildren(colorsDialog);

	    colorsDialog.setResizable(true);
	    colorsDialog.pack();
	}

	initColorSelect(choiceFG, textFG, term.getProperty("fg"));
	initColorSelect(choiceBG, textBG, term.getProperty("bg"));
	initColorSelect(choiceCC, textCC, term.getProperty("cc"));

	updateColors();

	lblAlertC.setText("");
	AWTConvenience.placeDialog(colorsDialog);

	choiceFG.requestFocus();
	colorsDialog.setVisible(true);
    }

    void initColorSelect(Choice c, TextField t, String colStr) {
	if(Character.isDigit(colStr.charAt(0))) {
	    c.select("custom rgb");
	    t.setText(colStr);
	} else {
	    t.setText("");
	    t.setEnabled(false);
	    c.select(colStr);
	}
    }

    void checkColorSelect(Choice c, TextField t) {
	int cs = c.getSelectedIndex();
    
	if(cs == 0) {
	    boolean en = t.isEnabled();
	    if(!en) {
		t.setEditable(true);
		t.setEnabled(true);
		t.setBackground(SystemColor.text);
		t.requestFocus();
	    }
	} else {
	    t.setText("");
	    t.setEditable(false);
	    t.setEnabled(false);
	    // on the Mac, Choices can't get keyboard focus
	    // so we may need to move focus away from the TextField
	    t.setBackground(term.termColors[cs - 1]);
	}
    }

    void updateColors() {
	checkColorSelect(choiceFG, textFG);
	checkColorSelect(choiceBG, textBG);
	checkColorSelect(choiceCC, textCC);
    }

    String getSelectedColor(Choice c, TextField t) {
	String colStr;
	if(c.getSelectedIndex() == 0)
	    colStr = t.getText();
	else
	    colStr = c.getSelectedItem();
	return colStr;
    }

    void fillChoices() {
	int i;
	choiceBG.add("custom rgb");
	choiceFG.add("custom rgb");
	choiceCC.add("custom rgb");
	for(i = 0; i < term.termColorNames.length; i++) {
	    choiceBG.add(term.termColorNames[i]);
	    choiceFG.add(term.termColorNames[i]);
	    choiceCC.add(term.termColorNames[i]);
	}
    }

    Dialog settingsDialog2;
    Choice choiceRG, choicePB;
    Checkbox cbDEL, cbBS, cbCpOnSel, cbCpWinCR, cbAsciiLD, cbLocPG;
    TextField textSL, textSD;
    final static String[] rg = { "bottom", "top" };
    final static String[] pb = { "middle", "right", "shift+left" };
    public final void termSettingsDialog2() {
	if(settingsDialog2 == null) {
	    int i;
	    settingsDialog2 = new Dialog(term.ownerFrame, "Terminal Misc. Settings", true);

	    Label              lbl;
	    GridBagLayout      grid  = new GridBagLayout();
	    GridBagConstraints gridc = new GridBagConstraints();
	    settingsDialog2.setLayout(grid);

	    gridc.insets = new Insets(4, 4, 0, 0);
	    gridc.fill   = GridBagConstraints.NONE;
	    gridc.anchor = GridBagConstraints.WEST;
	    gridc.gridwidth = 4;

	    gridc.gridy = 0;
	    gridc.gridwidth = 4;
	    lbl = new Label("Paste button:");
	    grid.setConstraints(lbl, gridc);
	    settingsDialog2.add(lbl);
	    choicePB = AWTConvenience.newChoice(pb);
	    grid.setConstraints(choicePB, gridc);
	    settingsDialog2.add(choicePB);

	    gridc.gridy = 1;
	    lbl = new Label("Select delim.:");
	    grid.setConstraints(lbl, gridc);
	    settingsDialog2.add(lbl);
	    textSD = new TextField("", 4);
	    grid.setConstraints(textSD, gridc);
	    settingsDialog2.add(textSD);

	    gridc.gridy = 2;
	    gridc.gridwidth = 8;
	    gridc.insets = new Insets(4, 16, 0, 0);
	    cbCpWinCR = new Checkbox("Copy <cr><nl> line ends");
	    grid.setConstraints(cbCpWinCR, gridc);
	    settingsDialog2.add(cbCpWinCR);

	    gridc.gridy = 3;
	    cbCpOnSel = new Checkbox("Copy on select");
	    grid.setConstraints(cbCpOnSel, gridc);
	    settingsDialog2.add(cbCpOnSel);

	    gridc.gridy = 4;
	    gridc.gridwidth = 4;
	    gridc.insets = new Insets(4, 4, 0, 0);
	    lbl = new Label("Resize gravity:");
	    grid.setConstraints(lbl, gridc);
	    settingsDialog2.add(lbl);
	    choiceRG = AWTConvenience.newChoice(rg);
	    grid.setConstraints(choiceRG, gridc);
	    settingsDialog2.add(choiceRG);

	    gridc.gridy = 5;
	    gridc.gridwidth = 8;
	    gridc.insets = new Insets(4, 16, 0, 0);
	    cbBS = new Checkbox("Backspace sends Delete");
	    grid.setConstraints(cbBS, gridc);
	    settingsDialog2.add(cbBS);

	    gridc.gridy = 6;
	    cbDEL = new Checkbox("Delete sends Backspace");
	    grid.setConstraints(cbDEL, gridc);
	    settingsDialog2.add(cbDEL);

	    gridc.gridy = 7;
	    cbLocPG = new Checkbox("Local PgUp/PgDn");
	    grid.setConstraints(cbLocPG, gridc);
	    settingsDialog2.add(cbLocPG);

	    gridc.gridy = 8;
	    cbAsciiLD = new Checkbox("Use ASCII for line draw");
	    grid.setConstraints(cbAsciiLD, gridc);
	    settingsDialog2.add(cbAsciiLD);


	    Panel bp = new Panel(new FlowLayout());

	    Button b;
	    bp.add(b = new Button("OK"));
	    b.addActionListener(new Actions(ACT_MISC));
	    bp.add(b = new Button("Cancel"));
	    b.addActionListener(new AWTConvenience.CloseAction(settingsDialog2));

	    gridc.gridy = 9;
	    gridc.fill  = GridBagConstraints.HORIZONTAL;
	    gridc.gridwidth = GridBagConstraints.REMAINDER;
	    gridc.anchor = GridBagConstraints.CENTER;
	    grid.setConstraints(bp, gridc);
	    settingsDialog2.add(bp);

	    settingsDialog2.addWindowListener(new AWTConvenience.CloseAdapter(b));

	    AWTConvenience.setBackgroundOfChildren(settingsDialog2);

	    settingsDialog2.setResizable(true);
	    settingsDialog2.pack();
	}

	choicePB.select(term.getProperty("pb"));
	String sdSet = term.getProperty("sd");
	if((sdSet.charAt(0) == '"' && sdSet.charAt(sdSet.length() - 1) == '"')) {
	    sdSet = sdSet.substring(1, sdSet.length() - 1);
	}
	textSD.setText(sdSet);
	cbCpOnSel.setState(Boolean.valueOf(term.getProperty("cs")).booleanValue());
	cbCpWinCR.setState(Boolean.valueOf(term.getProperty("sc")).booleanValue());

	choiceRG.select(term.getProperty("rg"));



	if(term.getProperty("bs").equals("DEL")) {
	    cbBS.setState(true);
	} else {
	    cbBS.setState(false);
	}
	if(term.getProperty("de").equals("BS")) {
	    cbDEL.setState(true);
	} else {
	    cbDEL.setState(false);
	}

	cbAsciiLD.setState(Boolean.valueOf(term.getProperty("ad")).booleanValue());
	cbLocPG.setState(Boolean.valueOf(term.getProperty("lp")).booleanValue());

	AWTConvenience.placeDialog(settingsDialog2);

	choiceRG.requestFocus();
	settingsDialog2.setVisible(true);
    }

    Dialog     findDialog = null;
    TextField  findText;
    Label      label;
    Checkbox   dirCheck, caseCheck;
    Button     findBut, cancBut;

    public final void findDialog() {
	if(findDialog == null) {
	    findDialog = new Dialog(term.ownerFrame, titleName + " - Find", false);
	    GridBagLayout      grid  = new GridBagLayout();
	    GridBagConstraints gridc = new GridBagConstraints();
	    findDialog.setLayout(grid);

	    gridc.fill   = GridBagConstraints.NONE;
	    gridc.anchor = GridBagConstraints.WEST;
	    gridc.gridwidth = 1;

	    gridc.gridy = 0;
	    label = new Label("Find:");
	    grid.setConstraints(label, gridc);
	    findDialog.add(label);

	    gridc.fill = GridBagConstraints.HORIZONTAL;
	    gridc.gridwidth = 5;

	    findText = new TextField("", 26);
	    grid.setConstraints(findText, gridc);
	    findDialog.add(findText);

	    gridc.gridwidth = 4;
	    gridc.ipadx = 4;
	    gridc.ipady = 4;
	    gridc.insets = new Insets(6, 3, 3, 6);

	    findBut = new Button("Find");
	    grid.setConstraints(findBut, gridc);
	    findDialog.add(findBut);

	    gridc.insets = new Insets(0, 0, 0, 0);
	    gridc.ipadx = 0;
	    gridc.ipady = 0;
	    gridc.gridwidth = 3;
	    gridc.gridy = 1;
	    gridc.fill   = GridBagConstraints.NONE;

	    caseCheck = new Checkbox("Case sensitive");
	    grid.setConstraints(caseCheck, gridc);
	    findDialog.add(caseCheck);
      
	    dirCheck = new Checkbox("Find backwards");
	    grid.setConstraints(dirCheck, gridc);
	    findDialog.add(dirCheck);

	    gridc.gridwidth = 4;
	    gridc.ipadx = 4;
	    gridc.ipady = 4;
	    gridc.insets = new Insets(3, 3, 6, 6);
	    gridc.fill = GridBagConstraints.HORIZONTAL;
	    cancBut = new Button("Cancel");
	    grid.setConstraints(cancBut, gridc);
	    findDialog.add(cancBut);

	    cancBut.addActionListener(new Actions(ACT_FIND_CANCEL));

	    findBut.addActionListener(new Actions(ACT_FIND));

	    findDialog.addWindowListener(new AWTConvenience.CloseAdapter(cancBut));

	    AWTConvenience.setBackgroundOfChildren(findDialog);
	    AWTConvenience.setKeyListenerOfChildren(findDialog,
						    new AWTConvenience.OKCancelAdapter(findBut, cancBut),
						    null);

	    findDialog.setResizable(true);
	    findDialog.pack();
	}

	AWTConvenience.placeDialog(findDialog);
	findText.requestFocus();
	findDialog.setVisible(true);
    }

    final static boolean doMatch(String findStr, char firstChar, char[] chars, int idx,
				 boolean caseSens, int len) {
	String cmpStr;
	if(caseSens) {
	    if(chars[idx] != firstChar)
		return false;
	    cmpStr = new String(chars, idx, len);
	    if(cmpStr.equals(findStr))
		return true;
	} else {
	    if(Character.toLowerCase(chars[idx]) != firstChar)
		return false;
	    cmpStr = new String(chars, idx, len);
	    if(cmpStr.equalsIgnoreCase(findStr))
		return true;
	}
	return false;
    }

    int curFindRow = 0;
    int curFindCol = 0;
    int findLen    = 0;

    void doFind() {
	String  findStr = findText.getText();
	String  cmpStr;
	int     len = findStr.length();
	boolean caseSens = caseCheck.getState();
	boolean revFind  = dirCheck.getState();
	int     lastRow  = term.saveVisTop + term.curRow;
	int     startCol;
	boolean found    = false;
	int     i, j = 0;
	char    firstChar = (caseSens ? findStr.charAt(0) : Character.toLowerCase(findStr.charAt(0)));

	if(findLen > 0) {
	    term.clearSelection(curFindRow, curFindCol, curFindRow, curFindCol + findLen - 1);
	}
    
	if(revFind) {
	    if(findLen > 0) {
		startCol = curFindCol - 1;
	    } else {
		curFindRow = lastRow;
		startCol   = term.cols - len;
	    }
	foundItRev:
	    for(i = curFindRow; i >= 0; i--) {
		for(j = startCol; j >= 0; j--) {
		    if(term.screen[i][j] == 0)
			continue;
		    if(doMatch(findStr, firstChar, term.screen[i], j, caseSens, len))
			break foundItRev;
		}
		startCol = term.cols - len;
	    }
	    if(i >= 0)
		found = true;
	} else {
	    startCol = curFindCol + findLen;
	foundIt:
	    for(i = curFindRow; i < lastRow; i++) {
		for(j = startCol; j < term.cols - len; j++) {
		    if(term.screen[i][j] == 0)
			continue;
		    if(doMatch(findStr, firstChar, term.screen[i], j, caseSens, len))
			break foundIt;
		}
		startCol = 0;
	    }
	    if(i < lastRow)
		found = true;
	}
	if(found) {
	    findLen = len;
	    if(term.saveVisTop < i)
		term.visTop = term.saveVisTop;
	    else if(term.visTop > i || (i - term.visTop > term.rows))
		term.visTop = i;
	    term.updateScrollbarValues();
	    term.makeAllDirty(false);
	    term.makeSelection(i, j, i, j + len - 1);
	    curFindRow = i;
	    curFindCol = j;
	    findLen    = len;
	} else {
	    term.doBell();
	    curFindRow = 0;
	    curFindCol = 0;
	    findLen    = 0;
	}
    }

    FileDialog sendFileDialog = null;
    public final void sendFileDialog() {
	if(sendFileDialog == null) {
	    sendFileDialog = new FileDialog(term.ownerFrame, titleName +
					    " - Select ASCII-file to send", FileDialog.LOAD);
	}
	sendFileDialog.setVisible(true);

	try {
	    netscape.security.PrivilegeManager.enablePrivilege("UniversalFileAccess");
	} catch (netscape.security.ForbiddenTargetException e) {
	    // !!!
	}

	String fileName = sendFileDialog.getFile();
	String dirName  = sendFileDialog.getDirectory();
	if(fileName != null && fileName.length() > 0) {
	    try {
		FileInputStream fileIn = new FileInputStream(dirName + fileName);
		byte[] bytes = new byte[fileIn.available()];
		fileIn.read(bytes);
		term.sendBytes(bytes);
	    } catch (Throwable t) {
		// !!! OUCH
		com.mindbright.ssh.SSHMiscDialogs.alert(titleName + " - Alert",
							t.getMessage(),
							term.ownerFrame);
	    }
	}
    }

    TerminalCapture termCapture;
    FileDialog captureToFileDialog = null;
    public final boolean captureToFileDialog() {
	if(captureToFileDialog == null) {
	    captureToFileDialog = new FileDialog(term.ownerFrame,
						 titleName + " - Select file to capture to", FileDialog.SAVE);
	}
	captureToFileDialog.setVisible(true);

	try {
	    netscape.security.PrivilegeManager.enablePrivilege("UniversalFileAccess");
	} catch (netscape.security.ForbiddenTargetException e) {
	    // !!!
	}

	String fileName = captureToFileDialog.getFile();
	String dirName  = captureToFileDialog.getDirectory();
	if(fileName != null && fileName.length() > 0) {
	    try {
		FileOutputStream fileOut =
		    new FileOutputStream(dirName + fileName, true);
		termCapture = new TerminalCapture(fileOut);
		termCapture.startCapture(term);
		return true;
	    } catch (Throwable t) {
		// !!! OUCH
		com.mindbright.ssh.SSHMiscDialogs.alert(titleName + " - Alert",
							t.getMessage(),
							term.ownerFrame);
	    }
	}
	return false;
    }

    public void endCapture() {
	if(termCapture != null) {
	    termCapture.endCapture();
	    try {
		termCapture.getTarget().close();
	    } catch (IOException e) {
		// !!! OUCH
		com.mindbright.ssh.SSHMiscDialogs.alert(titleName + " - Alert",
							e.getMessage(),
							term.ownerFrame);
	    }
	}
    }

    public void setEnabledOpt(int opt, boolean val) {
	((CheckboxMenuItem)menuItems[MENU_OPTIONS][opt + 1]).setEnabled(val);
    }

    public void setStateOpt(int opt, boolean val) {
	((CheckboxMenuItem)menuItems[MENU_OPTIONS][opt + 1]).setState(val);
    }

    public Menu getMenu(int idx) {
	Menu m = new Menu(menuTexts[idx][0]);
	int len = menuTexts[idx].length;
	MenuItem mi;
	String   t;

	if(menuItems == null)
	    menuItems = new Object[menuTexts.length][];
	if(menuItems[idx] == null)
	    menuItems[idx] = new Object[menuTexts[idx].length];

	for(int i = 1; i < len; i++) {
	    t = menuTexts[idx][i];
	    if(t == null) {
		m.addSeparator();
		continue;
	    }
	    if(t.charAt(0) == '_') {
		t = t.substring(1);
		mi = new CheckboxMenuItem(t);
		((CheckboxMenuItem)mi).addItemListener(this);
	    } else {
		mi = new MenuItem(t);
		mi.addActionListener(this);
	    }

	    if(menuShortCuts[idx][i] != NO_SHORTCUT) {
		mi.setShortcut(new MenuShortcut(menuShortCuts[idx][i], true));
	    }

	    menuItems[idx][i] = mi;
	    m.add(mi);
	}
	return m;
    }

    int[] mapAction(String action) {
	int[] id = new int[2];
	int i = 0, j = 0;

	for(i = 0; i < menuTexts.length; i++) {
	    for(j = 1; j < menuTexts[i].length; j++) {
		String mt = menuTexts[i][j];
		if(mt != null && action.equals(mt)) {
		    id[0] = i;
		    id[1] = j;
		    i = menuTexts.length;
		    break;
		}
	    }
	}
	return id;
    }

    public void actionPerformed(ActionEvent e) {
	int[] id = mapAction(((MenuItem)(e.getSource())).getLabel());
	handleMenuAction(id);
    }

    public void itemStateChanged(ItemEvent e) {
	int[] id = mapAction("_" + (String)e.getItem());
	handleMenuAction(id);
    }

    public void handleMenuAction(int[] id) {
	switch(id[0]) {
	case MENU_FILE:
	    switch(id[1]) {
	    case M_FILE_CAPTURE:
		if(((CheckboxMenuItem)
		    menuItems[MENU_FILE][M_FILE_CAPTURE]).getState()) {
		    if(!captureToFileDialog()) {
			((CheckboxMenuItem)
			 menuItems[MENU_FILE][M_FILE_CAPTURE]).setState(false);
		    }
		} else {
		    endCapture();
		}
		break;
	    case M_FILE_SEND:
		((TerminalMenuHandlerFull)term.getMenus()).sendFileDialog();
		break;
	    case M_FILE_CLOSE:
		if(listener != null) {
		    listener.close(this);
		}
		break;
	    }
	    break;

	case MENU_EDIT:
	    switch(id[1]) {
	    case M_EDIT_COPY:
		term.doCopy();
		break;
	    case M_EDIT_PASTE:
		term.doPaste();
		break;
	    case M_EDIT_CPPASTE:
		term.doCopy();
		term.doPaste();
		break;
	    case M_EDIT_SELALL:
		term.selectAll();
		break;
	    case M_EDIT_FIND:
		findDialog();
		break;
	    case M_EDIT_CLS:
		term.clearScreen();
		term.cursorSetPos(0, 0, false);
		break;
	    case M_EDIT_CLEARSB:
		term.clearSaveLines();
		break;
	    case M_EDIT_VTRESET:
		term.resetInterpreter();
		break;
	    }
	    break;

	case MENU_OPTIONS:
	    int i = id[1] - 1;
	    term.setProperty(TerminalDefProps.defaultPropDesc[i]
			     [TerminalDefProps.PROP_NAME],
			     String.valueOf(!term.termOptions[i]));
	    break;
	}
    }

}
