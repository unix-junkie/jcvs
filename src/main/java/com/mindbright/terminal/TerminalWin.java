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

import java.io.IOException;
import java.util.NoSuchElementException;
import java.awt.*; 
import java.awt.event.*; 
import java.util.Vector;
import java.util.Properties;
import java.util.Enumeration;

public final class TerminalWin extends Canvas
    implements Terminal, KeyListener, AdjustmentListener, MouseListener,
	       MouseMotionListener, ComponentListener, FocusListener
{

    boolean metaKeyKludge  = false;
    boolean ctrlKeyKludge  = false;

    int     lastKeyKludge     = 0;
    boolean lastKeyWasPressed = false;

    boolean pendingShow = true;

    final static boolean DEBUG         = false;
    final static boolean DEBUGKEYEVENT = false;

    final static public int GRAVITY_SOUTHWEST = 0;
    final static public int GRAVITY_NORTHWEST = 1;

    final static public int MIN_ROWS = 2;
    final static public int MIN_COLS = 8;
    final static public int MAX_COLS = 512;
    final static public int MAX_ROWS = 512;

    Vector                   inListeners;
    TerminalOutputListener   outListener;
    TerminalClipboardHandler clipboard;
    TerminalInterpreter      interpreter;
    Scrollbar                scrollbar;
    boolean                  haveScrollbar;
    PopupMenu                popupmenu;
    Panel                    myPanel;
    Frame                    ownerFrame;

    private boolean logoDraw;
    private Image   logoImg;
    private int     logoX;
    private int     logoY;
    private int     logoW;
    private int     logoH;

    public int popupButton = InputEvent.BUTTON3_MASK;
    public int pasteButton = InputEvent.BUTTON2_MASK;

    TerminalMenuHandler menuHandler;

    String              title;

    Properties props;
    boolean propsChanged;
    String savedGeomPos;
    boolean insideConstructor;

    byte    bsCharacter;
    byte    delCharacter;

    boolean haveFocus;

    boolean repaintPending;
    boolean cursorHollow;
    boolean cursorDrawn;
    boolean complexScroll;
    boolean fullRefresh;
    boolean writeMultiChars;

    int     dirtyTop;
    int     dirtyBottom;
    int     dirtyLeft;
    int     dirtyRight;

    int     resizeGravity;

    int rows;
    int cols;
    int vpixels;
    int hpixels;
    int borderWidth  = 2;
    int borderHeight = 2;

    int windowTop;
    int windowBottom;
    int windowLeft;
    int windowRight;

    int charWidth;
    int charHeight;
    int charMaxAscent;
    int charMaxDescent;
    int charLeading;
    int baselineIndex;

    int curRow;
    int curCol;
    int lastCursorRow;
    int lastCursorCol;

    int     selectRowAnchor;
    int     selectColAnchor;
    int     selectRowLast;
    int     selectColLast;
    boolean hasSelection;
    boolean selectReverse;
    String  selectDelims;
    int     selectClickRow = -1;
    boolean selectClickState;
    long    lastLeftClick  = 0;

    int curAttr;

    int curRowSave;
    int curColSave;
    int curAttrSave;

    Color origBgColor;
    Color origFgColor;
    Color cursorColor;

    public final static Color termColors[] = {
	Color.black,
	Color.red.darker(),
	Color.green.darker(),
	Color.yellow.darker(),
	Color.blue.darker(),
	Color.magenta.darker(),
	Color.cyan.darker(),
	Color.white,
	Color.darkGray,
	Color.red,
	Color.green,
	Color.yellow,
	Color.blue,
	Color.magenta,
	Color.cyan,
	Color.white
    };

    public final static String[] termColorNames = {
	"black", "red", "green", "yellow", "blue", "magenta", "cyan", "white",
	"i_black", "i_red", "i_green", "i_yellow",
	"i_blue","i_magenta", "i_cyan", "i_white"
    };

    char[][]  screen;
    int[][]   attributes;

    int saveLines;
    int visTop;
    int saveVisTop;

    // (NOTE: The real terminal attributes are in Terminal.java)
    //
    public final static int ATTR_CHARNOTDRAWN = 0x0000;
    public final static int ATTR_LINEDRAW     = 0x0100;
    public final static int ATTR_SELECTED     = 0x1000;
    public final static int ATTR_CHARDRAWN    = 0x8000;

    public final static int MASK_ATTR   = 0x0000ffff;
    public final static int MASK_FGCOL  = 0x00ff0000;
    public final static int MASK_BGCOL  = 0xff000000;
    public final static int SHIFT_FGCOL = 16;
    public final static int SHIFT_BGCOL = 24;

    public final static char[] spacerow = new char[MAX_COLS];
    public final static int[]  zerorow  = new int[MAX_COLS];
    static {
	for(int i = 0; i < MAX_COLS; i++) {
	    spacerow[i] = ' ';
	    zerorow[i]  = 0;
	}
    }

    boolean[] tabStops = new boolean[MAX_COLS];

    boolean[] termOptions;

    Image     memImage;
    Graphics  memGraphics;
    Dimension memImageSize;

    public Font plainFont;
    public Font boldFont;

    public TerminalWin(Frame ownerFrame, TerminalInterpreter interpreter,
		       Properties initProps)
	throws IllegalArgumentException
    {
	this(ownerFrame, interpreter, initProps, false);
    }

    public TerminalWin(Frame ownerFrame, TerminalInterpreter interpreter,
		       Properties initProps, boolean setAsDefault)
	throws IllegalArgumentException
    {
	super();

	scrollbar     = null;
	haveScrollbar = false;
	title         = null;
	termOptions   = new boolean[Terminal.OPT_LAST_OPT];
	cursorDrawn   = false;
	cursorHollow  = false;
	curAttr       = ATTR_CHARDRAWN;
	curRow        = 0;
	curCol        = 0;
	visTop        = 0;
	saveVisTop    = 0;

	if(setAsDefault) {
	    TerminalDefProps.setAsDefault(initProps);
	}

	repaintPending = false;
	savedGeomPos   = "";

	this.ownerFrame  = ownerFrame;
	this.interpreter = interpreter;
	interpreter.setTerminal(this);

	insideConstructor = true;
	setProperties(initProps, true);
	insideConstructor = false;
	propsChanged = false;

	resetTabs();

	// !!! We don't receive the proper component-events on the Canvas IMHO?!
	//
	ownerFrame.addComponentListener(this);

	// !!! Ok, in spite of all our efforts here, we seem to need this
	// for certain situations, I give up once again...
	//
	// ownerFrame.addKeyListener(this);

	addKeyListener(this);
	addComponentListener(this);
	addFocusListener(this);
	addMouseMotionListener(this);
	addMouseListener(this);
    }

    public TerminalWin(Frame ownerFrame, TerminalInterpreter interpreter) throws IllegalArgumentException, NoSuchElementException {
	this(ownerFrame, interpreter, TerminalDefProps.defaultProperties);
    }

    public void setMenus(TerminalMenuHandler menus) {
	menuHandler = menus;
    }

    public TerminalMenuHandler getMenus() {
	return menuHandler;
    }

    public void updateMenus() {
	if(menuHandler != null)
	    menuHandler.update();
    }

    public void setLogo(Image logoImg, int x, int y, int w, int h) {
	this.logoImg = logoImg;
	this.logoX   = x;
	this.logoY   = y;
	this.logoW   = w;
	this.logoH   = h;
    }

    public Image getLogo() {
	return logoImg;
    }

    public boolean showLogo() {
	logoDraw = (logoImg != null);
	makeAllDirty(true);
	return logoDraw;
    }

    public void hideLogo() {
	logoDraw = false;
	makeAllDirty(true);
    }

    public void setProperties(Properties newProps, boolean merge)
	throws IllegalArgumentException, NoSuchElementException
    {
	String name, value;
	Enumeration enum;
	int i;
	Properties oldProps = props;

	props = new Properties(TerminalDefProps.defaultProperties);

	if(merge && oldProps != null) {
	    enum = oldProps.keys();
	    while(enum.hasMoreElements()) {
		name  = (String)enum.nextElement();
		value = oldProps.getProperty(name);
		props.put(name, value);
	    }
	}

	enum = newProps.keys();
	while(enum.hasMoreElements()) {
	    name  = (String)enum.nextElement();
	    if(!TerminalDefProps.isProperty(name))
		throw new NoSuchElementException("unknown terminal-property '" + name + "'");
	}

	// Order is important to get this right, set "normal" settings first, then options
	// !!! OUCH
	//
	String  oldVal;

	for(i = termOptions.length; i < TerminalDefProps.defaultPropDesc.length; i++) {
	    name  = TerminalDefProps.defaultPropDesc[i][TerminalDefProps.PROP_NAME];
	    value = newProps.getProperty(name);
	    name  = TerminalDefProps.backwardCompatProp(name);
	    if(value == null)
		value = props.getProperty(name);
	    if(!merge && oldProps != null) {
		oldVal = oldProps.getProperty(name);
		setProperty(name, value, !value.equals(oldVal));
	    } else {
		setProperty(name, value, insideConstructor);
	    }
	}
	for(i = 0; i < termOptions.length; i++) {
	    name  = TerminalDefProps.defaultPropDesc[i][TerminalDefProps.PROP_NAME];
	    value = newProps.getProperty(name);
	    name  = TerminalDefProps.backwardCompatProp(name);
	    if(value == null)
		value = props.getProperty(name);
	    if(!merge && oldProps != null) {
		oldVal = oldProps.getProperty(name);
		setProperty(name, value, !value.equals(oldVal));
	    } else {
		setProperty(name, value, insideConstructor);
	    }
	}
    }

    public Properties getProperties() {
	return props;
    }

    public boolean getPropsChanged() {
	return propsChanged;
    }

    public void setPropsChanged(boolean value) {
	propsChanged = value;
    }

    public void resetToDefaults() {
	setProperties(TerminalDefProps.defaultProperties, false);
    }

    public String getProperty(String key) {
	key  = TerminalDefProps.backwardCompatProp(key);
	return props.getProperty(key);
    }

    public String getDefaultProperty(String key) {
	key  = TerminalDefProps.backwardCompatProp(key);
	return TerminalDefProps.defaultProperties.getProperty(key);
    }

    public void resetProperty(String key) {
	key  = TerminalDefProps.backwardCompatProp(key);
	setProperty(key, getDefaultProperty(key));
    }

    public void setProperty(String key, String value)
	throws IllegalArgumentException, NoSuchElementException
    {
	setProperty(key, value, false);
    }

    public synchronized void setProperty(String key, String value,
					 boolean forceSet)
	throws IllegalArgumentException, NoSuchElementException
    {
	int i;
	boolean isEqual = false;
	String val;

	key = TerminalDefProps.backwardCompatProp(key);

	if(((val = getProperty(key)) != null) && val.equals(value)) {
	    isEqual = true;
	    if(!forceSet)
		return;
	}

	for(i = 0; i < termOptions.length; i++) {
	    if(TerminalDefProps.defaultPropDesc[i][TerminalDefProps.PROP_NAME].equals(key))
		break;
	}
	if(i < termOptions.length) {
	    if(!(value.equals("true") || value.equals("false")))
		throw new IllegalArgumentException("value for '" + key + "' must be 'true' or 'false'");
	    setOption(i, (new Boolean(value)).booleanValue());
	} else {
	    if(key.equals("term-type")) {
		if(interpreter instanceof TerminalXTerm)
		    ((TerminalXTerm)interpreter).setTerminalType(value);
	    } else if(key.equals("font-name")) {
		setFont(value, Integer.parseInt(getProperty("font-size")));
	    } else if(key.equals("font-size")) {
		try {
		    setFont(getProperty("font-name"), Integer.parseInt(value));
		} catch (NumberFormatException e) {
		    throw new IllegalArgumentException("value for '" + key + "' must be an integer");
		}
	    } else if(key.equals("save-lines")) {
		try {
		    int sl = Integer.parseInt(value);
		    if(sl < 0 || sl > 8192)
			throw new NumberFormatException();
		    setSaveLines(sl);
		} catch (NumberFormatException e) {
		    throw new IllegalArgumentException("value for '" + key + "' must be an integer (0-8192)");
		}
	    } else if(key.equals("scrollbar")) {
		if(myPanel != null) {
		    if(value.equals("left") || value.equals("right")) {
			if(scrollbar != null) {
			    myPanel.remove(scrollbar);
			    if(value.equals("right"))
				myPanel.add(scrollbar, BorderLayout.EAST);
			    else
				myPanel.add(scrollbar, BorderLayout.WEST);
			    haveScrollbar = true;
			    updateScrollbarValues();
			    // !!! REMOVE
			    // scrollbar.setWindowSide(value);
			    ownerFrame.pack();
			    requestFocus();
			}
		    } else if(value.equals("none")) {
			if(scrollbar != null)
			    myPanel.remove(scrollbar);
			ownerFrame.pack();
			requestFocus();
			haveScrollbar = false;
		    } else {
			throw new IllegalArgumentException("scrollbar can be right, left or none");
		    }
		}
	    } else if(key.equals("bg-color") ||
		      key.equals("fg-color") ||
		      key.equals("cursor-color")) {
		Color c;
		try {
		    if(Character.isDigit(value.charAt(0))) {
			c = getTermRGBColor(value);
		    } else {
			c = getTermColor(value);
		    }
		} catch (NumberFormatException e) {
		    throw new IllegalArgumentException("valid colors: 'color-name' or '<r>,<g>,<b>'");
		}
		if(key.equals("bg-color")) {
		    origBgColor = c;
		    setBackground(origBgColor);
		} else if(key.equals("cursor-color")) {
		    cursorColor = c;
		} else {
		    origFgColor = c;
		    setForeground(origFgColor);
		}
		makeAllDirty(false);
	    } else if(key.equals("resize-gravity")) {
		int rg;
		if(value.equals("top")) {
		    rg = GRAVITY_NORTHWEST;
		} else if(value.equals("bottom")) {
		    rg = GRAVITY_SOUTHWEST;
		} else {
		    throw new IllegalArgumentException("reszize gravity can be 'top' or 'bottom'");
		}
		this.resizeGravity = rg;
	    } else if(key.equals("delete-send")) {
		if(value.equals("DEL"))
		    delCharacter = (byte)0x7f;
		else if(value.equals("BS")) {
		    delCharacter = (byte)0x08;
		} else {
		    throw new IllegalArgumentException("delete character can be 'DEL' or 'BS'");
		}
	    } else if(key.equals("backspace-send")) {
		if(value.equals("DEL"))
		    bsCharacter = (byte)0x7f;
		else if(value.equals("BS")) {
		    bsCharacter = (byte)0x08;
		} else {
		    throw new IllegalArgumentException("backspace character can be 'DEL' or 'BS'");
		}
	    } else if(key.equals("geometry")) {
		setGeometry(value, true);
	    } else if(key.equals("select-delim")) {
		if(!(value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"')) {
		    value = "\"" + value + "\"";
		}
		selectDelims = value.substring(1, value.length());
	    } else if(key.equals("paste-button")) {
		if(value.equals("middle")) {
		    pasteButton = InputEvent.BUTTON2_MASK;
		} else if(value.equals("right")) {
		    pasteButton = InputEvent.BUTTON3_MASK;
		} else if(value.equals("shift+left")) {
		    pasteButton = InputEvent.BUTTON1_MASK |
			InputEvent.SHIFT_MASK;
		} else {
		    throw new IllegalArgumentException("mouse paste button can be 'middle' or 'right'");
		}
	    } else {
		throw new NoSuchElementException("unknown terminal-property '" + key + "'");
	    }
	}
	props.put(key, value);

	if(!isEqual) {
	    propsChanged = true;
	    updateMenus();
	}
    }

    public static Color getTermRGBColor(String value) throws NumberFormatException {
	int r, g, b, c1, c2;
	Color c;
	c1 = value.indexOf(',');
	c2 = value.lastIndexOf(',');
	if(c1 == -1 || c2 == -1)
	    throw new NumberFormatException();
	r = Integer.parseInt(value.substring(0, c1).trim());
	g = Integer.parseInt(value.substring(c1 + 1, c2).trim());
	b = Integer.parseInt(value.substring(c2 + 1).trim());
	c = new Color(r, g, b);
	return c;
    }

    public static Color getTermColor(String name) throws IllegalArgumentException {
	int i;
	for(i = 0; i < termColors.length; i++) {
	    if(termColorNames[i].equalsIgnoreCase(name))
		break;
	}
	if(i == termColors.length)
	    throw new IllegalArgumentException("Unknown color: " + name);
	return termColors[i];
    }

    //
    // !!! Ouch !!!
    //
    public void setGeometry(String geometry, boolean doResize) throws IllegalArgumentException {
	int ro, co, xPos, yPos, xSz, ySz, delim = geometry.indexOf('x'), delX, delY;
	delX = geometry.indexOf('+');
	delY = geometry.indexOf('-');

	if(delY != -1)
	    delX = ((delX > delY || delX == -1) ? delY : delX);

	try {
	    if(delim == -1)
		throw new Exception();

	    co = Integer.parseInt(geometry.substring(0, delim).trim());
	    ro = Integer.parseInt(geometry.substring(delim + 1,
						     (delX == -1 ? geometry.length() : delX)).trim());
	    xSz = (2 * borderWidth ) + (charWidth  * co);
	    ySz = (2 * borderHeight) + (charHeight * ro);

	    if(delX != -1) {
		delY = geometry.indexOf('+', delX + 1);
		if(delY == -1) {
		    delY = geometry.indexOf('-', delX + 1);
		    if(delY == -1)
			throw new Exception();
		}

		Dimension sDim  = Toolkit.getDefaultToolkit().getScreenSize();
		Insets    fIns  = ownerFrame.getInsets();
		int       sbSz  = (haveScrollbar ? scrollbar.getSize().width : 0);

		xPos = Integer.parseInt(geometry.substring(delX + 1, delY).trim());
		yPos = Integer.parseInt(geometry.substring(delY + 1).trim());

		if(geometry.charAt(delX) == '-')
		    xPos = sDim.width - xSz - xPos - fIns.left - fIns.right - sbSz;
		if(geometry.charAt(delY) == '-')
		    yPos = sDim.height - ySz - yPos - fIns.top - fIns.bottom;

		savedGeomPos = geometry.substring(delX).trim();

		// !!! We can only calculate right position when ownerFrame isShowing otherwise
		// the insets and everything is not set, then we set pendingShow to be called later
		// from componentShown
		//
		if(isShowing())
		    ownerFrame.setLocation(xPos, yPos);

	    } else {
		savedGeomPos = "";
	    }
	} catch(Exception e) {
	    throw new IllegalArgumentException("geometry must be '<cols>x<rows>[position]', e.g. '80x24+0-0'");
	}
	if(doResize) {
	    setSize(xSz, ySz);
	    if(isShowing()) {
		componentResized(null);
		ownerFrame.pack();
		requestFocus();
	    } else {
		pendingShow = true;
		setWindowSize(ro, co);
		resetWindow();
		clearScreen();
	    }
	}
    }

    final private void setFont(String name, int size) {
	plainFont = new Font(name, Font.PLAIN, size);
	boldFont  = new Font(name, Font.BOLD, size);

	super.setFont(plainFont);
	getDimensionOfText(0, 0);

	if(isShowing()) {
	    componentResized(null);
	}

    }

    public void setFont(Font font) {
	setFont(font.getName(), font.getSize());
    }

    public void setTitle(String title) {
	this.title = title;
	signalWindowChanged(rows, cols, vpixels, hpixels);
    }

    public String getTitle() {
	return title;
    }

    public void setPopupButton(int buttonNum) {
	switch(buttonNum) {
	case 1:
	    popupButton = InputEvent.BUTTON1_MASK;
	    break;
	case 2:
	    popupButton = InputEvent.BUTTON2_MASK;
	    break;
	case 3:
	    popupButton = InputEvent.BUTTON3_MASK;
	    break;
	default:
	    break;
	}
    }

    public PopupMenu getPopupMenu(String header) {
	if(popupmenu != null)
	    return popupmenu;
	popupmenu = new PopupMenu(header);
	this.add(popupmenu);
	return popupmenu;
    }

    void updateScrollbarValues() {
	if(haveScrollbar) {
	    scrollbar.setValues(visTop, rows, 0, saveVisTop + rows);
	    scrollbar.setBlockIncrement(rows);
	}
    }

    public Panel getPanelWithScrollbar() {
	if(myPanel != null)
	    return myPanel;

	haveScrollbar = true;
	scrollbar = new Scrollbar(Scrollbar.VERTICAL);
	updateScrollbarValues();
	scrollbar.addAdjustmentListener(this);

	myPanel = new Panel(new BorderLayout());
	myPanel.add(this, BorderLayout.CENTER);
	String sb = getProperty("scrollbar");
	if(sb.equals("left"))
	    myPanel.add(scrollbar, BorderLayout.WEST);
	else if(sb.equals("right"))
	    myPanel.add(scrollbar, BorderLayout.EAST);
	else
	    haveScrollbar = false; // No scrollbar

	/* !!! REMOVE
	if(haveScrollbar)
	    scrollbar.setWindowSide(sb);
	*/

	return myPanel;
    }

    private final void setSaveLines(int n) {
	int oldSaveLines = saveLines;
	int fromRow, toRow, copyRows;
	boolean outOfMemory = false;
	n = (n < 0 ? 0 : n);
	n = (n > 8192 ? 8192 : n);

	if(saveLines != n) {
	    char[][] oldScreen     = screen;
	    int[][]  oldAttributes = attributes;
	    saveLines              = n;
	    try {
		setWindowSize(rows, cols);
	    } catch (OutOfMemoryError e) {
		saveLines = oldSaveLines;
		setWindowSize(rows, cols);
		outOfMemory = true;
	    }
	    toRow       = 0;
	    if(oldSaveLines < saveLines) {
		fromRow     = 0;
		copyRows    = oldSaveLines + rows;
	    } else {
		if(saveVisTop <= saveLines) {
		    fromRow     = 0;
		    copyRows    = saveVisTop + rows;
		} else {
		    fromRow     = saveVisTop - saveLines;
		    copyRows    = saveLines + rows;
		    saveVisTop -= fromRow;
		}
	    }
	    System.arraycopy(oldScreen, fromRow, screen, toRow, copyRows);
	    System.arraycopy(oldAttributes, fromRow, attributes, toRow, copyRows);

	    visTop = saveVisTop;

	    updateScrollbarValues();

	    if(outOfMemory) {
		outOfMemory = false;
		write("\n\rOut of memory allocating scrollback buffer, reverting to " + saveLines + " lines!");
	    }

	}
    }

    public void clearSaveLines() {
	int fromRow, toRow, copyRows;

	char[][] oldScreen     = screen;
	int[][]  oldAttributes = attributes;
	setWindowSize(rows, cols);
	fromRow = saveVisTop;
	System.arraycopy(oldScreen, saveVisTop, screen, 0, rows);
	System.arraycopy(oldAttributes, saveVisTop, attributes, 0, rows);
	saveVisTop = 0;
	visTop     = 0;
	updateScrollbarValues();
	makeAllDirty(true);
    }

    public void setWindowSize(int rows, int cols) {
	if(DEBUG) System.out.println("setWindowSize: " + cols + "x" + rows);
	this.rows    = rows;
	this.cols    = cols;
	screen       = new char[rows + saveLines][cols];
	attributes   = new int[rows + saveLines][cols];
    }

    public void setInterpreter(TerminalInterpreter interpreter) {
	if(interpreter != null)
	    this.interpreter = interpreter;
    }

    //
    // Terminal interface
    //

    public String terminalType() {
	return interpreter.terminalType();
    }
    public int rows() {
	return rows;
    }
    public int cols() {
	return cols;
    }
    public int vpixels() {
	return vpixels;
    }
    public int hpixels() {
	return hpixels;
    }

    protected final void makeAllDirty(boolean instantUpdate) {
	updateDirtyArea(0, 0, rows, cols);
	if(instantUpdate && isShowing()) {
	    Graphics g = getGraphics();
	    if(g != null)
		update(g);
	} else {
	    repaint();
	}
    }
    protected final void updateDirtyArea(int top, int left, int bottom, int right) {
	if(top < dirtyTop)
	    dirtyTop = top;
	if(bottom > dirtyBottom)
	    dirtyBottom = bottom;
	if(left < dirtyLeft)
	    dirtyLeft = left;
	if(right > dirtyRight)
	    dirtyRight = right;
    }
    synchronized final public void write(char c) {
	if(outListener != null && !writeMultiChars) {
	    outListener.write(c);
	}
	if(visTop != saveVisTop && termOptions[OPT_SCROLL_SI]) {
	    repaintPending = false; // To make the code below do repaint()
	    visTop         = saveVisTop;
	    if(haveScrollbar)
		scrollbar.setValue(visTop);
	    updateDirtyArea(0, 0, rows, cols);
	}

	int ic;
	if((ic = interpreter.interpretChar(c)) != TerminalInterpreter.IGNORE) {
	    c = (char)ic;
	    if(curCol == cols) {
		if(termOptions[OPT_AUTO_WRAP]) {
		    curRow += 1;
		    curCol = 0;
		    if(curRow == windowBottom) {
			scrollUp(1);
			curRow = windowBottom - 1;
		    }
		} else
		    curCol--;
	    }

	    if(termOptions[OPT_INSERTMODE]) {
		insertChars(1);
	    }

	    // Keep track of the spanning update area
	    //
	    updateDirtyArea(curRow, curCol, curRow + 1, curCol + 1);

	    int idxRow = visTop + curRow;
	    attributes[idxRow][curCol] = curAttr;
	    screen[idxRow][curCol++]   = c;
	}

	repaint();
    }
    public synchronized final void write(char[] c, int off, int len) {
	waitForMore     = true;
	writeMultiChars = true;
	int end = off + len;
	for(int i = off; i < end; i++)
	    write(c[i]);
	if(outListener != null) {
	    outListener.write(c, off, len);
	}
	writeMultiChars = false;
    }
    public synchronized final void write(byte[] c, int off, int len) {
	write(new String(c, off, len)); // Convert to default encoding
    }
    public final void write(String s) {
	char[] carr = s.toCharArray();
	write(carr, 0, carr.length);
    }

    public void writeLineDrawChar(char c) {
	if(curCol == cols) {
	    if(termOptions[OPT_AUTO_WRAP]) {
		curRow += 1;
		curCol = 0;
		if(curRow == windowBottom) {
		    scrollUp(1);
		    curRow = windowBottom - 1;
		}
	    } else
		curCol--;
	}
	// Keep track of the spanning update area
	//
	updateDirtyArea(curRow, curCol, curRow + 1, curCol + 1);

	int idxRow = visTop + curRow;
	attributes[idxRow][curCol] = (curAttr | ATTR_LINEDRAW);
	screen[idxRow][curCol++]   = c;
    }

    public void addInputListener(TerminalInputListener inListener) {
	if(inListeners == null) {
	    inListeners = new Vector();
	}
	inListeners.removeElement(inListener);
	inListeners.addElement(inListener);
    }

    public void removeInputListener(TerminalInputListener inListener) {
	if(inListeners != null) {
	    inListeners.removeElement(inListener);
	    if(inListeners.size() == 0) {
		inListeners = null;
	    }
	}
    }

    public void addOutputListener(TerminalOutputListener outListener) {
	// !!! TODO have several listeners...
	this.outListener = outListener;
    }

    public void removeOutputListener(TerminalOutputListener outListener) {
	// !!! TODO have several listeners...
	this.outListener = null;
    }

    public void setClipboard(TerminalClipboardHandler clipboard) {
	this.clipboard = clipboard;
	if(hasSelection) {
	    clipboard.setSelection(getSelection());
	} else {
	    clipboard.clearSelection();
	}
    }

    public TerminalClipboardHandler getClipboard() {
	return clipboard;
    }

    public void typedChar(char c) {
	if(DEBUG) System.out.println("typedChar: " + c + "(" + (int)c + ")");
	if(inListeners != null) {
	    int n = inListeners.size();
	    for(int i = 0; i < n; i++) {
		TerminalInputListener inListener = (TerminalInputListener)
		    inListeners.elementAt(i);
		if(inListener != null) {
		    inListener.typedChar(c);
		}
	    }
	}
    }

    public final void sendBytes(byte[] b) {
	if(DEBUG) System.out.println("Sending " + b.length + " bytes");
	if(inListeners != null) {
	    int n = inListeners.size();
	    for(int i = 0; i < n; i++) {
		TerminalInputListener inListener = (TerminalInputListener)
		    inListeners.elementAt(i);
		if(inListener != null) {
		    inListener.sendBytes(b);
		}
	    }
	}
    }

    public void signalWindowChanged(int rows, int cols, int vpixels, int hpixels) {
	if(DEBUG) System.out.println("SIGWINCH: " + rows + ", " + cols);
	if(inListeners != null) {
	    int n = inListeners.size();
	    for(int i = 0; i < n; i++) {
		TerminalInputListener inListener = (TerminalInputListener)
		    inListeners.elementAt(i);
		if(inListener != null) {
		    inListener.signalWindowChanged(rows, cols, vpixels, hpixels);
		}
	    }
	}
    }

    public void doBell() {
	if(termOptions[OPT_VIS_BELL]) {
	    setOption(OPT_REV_VIDEO, !termOptions[OPT_REV_VIDEO]);
	    setOption(OPT_REV_VIDEO, !termOptions[OPT_REV_VIDEO]);
	} else {
	    Toolkit toolkit = Toolkit.getDefaultToolkit();
	    if(toolkit != null) {
		try {
		    toolkit.beep();
		} catch (Exception e) {
		    // !!! Could not beep, we are probably an unpriviliged applet
		    // Automatically enable visual-bell now and "sound" it instead
		    //
		    setOption(OPT_VIS_BELL, true);
		    doBell();
		}
	    }
	}
    }

    public void doBS() {
	if(DEBUG) System.out.println("doBS");
	cursorBackward(1);
    }

    public void doTab() {
	int i;
	if(curCol < windowRight) {
	    for(i = curCol + 1; i < windowRight; i++)
		if(tabStops[i])
		    break;
	    curCol = (i < windowRight ? i : windowRight - 1);
	}
	if(DEBUG) System.out.println("doTab");
    }

    public void doTabs(int n) {
	while(n-- > 0)
	    doTab();
    }
    public void doBackTabs(int n) {
	if(DEBUG) System.out.println("doBackTabs: " + n);
	int i;
	if(curCol > 0 && n >= 0) {
	    for(i = curCol - 1; i >= 0; i--) {
		if(tabStops[i]) {
		    if(--n == 0)
			break;
		}
	    }
	    curCol = (i < 0 ? 0 : i);
	}
    }

    public void setTab(int col) {
	tabStops[col] = true;
    }

    public void clearTab(int col) {
	tabStops[col] = false;
    }

    public void resetTabs() {
	for(int i = 0; i < MAX_COLS; i++) {
	    if((i % 8) == 0)
		tabStops[i] = true;
	    else
		tabStops[i] = false;
	}
    }

    public void clearAllTabs() {
	for(int i = 0; i < MAX_COLS; i++)
	    tabStops[i] = false;
    }

    public void doCR() {
	curCol = windowLeft;
	if(DEBUG) System.out.println("doCR");
    }

    public void doLF() {
	curRow += 1;
	if(curRow == windowBottom) {
	    scrollUp(1);
	    curRow = windowBottom - 1;
	}
	if(termOptions[OPT_LOCAL_ECHO] && termOptions[OPT_AUTO_LF]) {
	    doCR();
	}
	if(DEBUG) System.out.println("doLF");
    }

    public void resetInterpreter() {
	interpreter.vtReset();
	makeAllDirty(true);
    }

    public void resetWindow() {
	windowTop     = 0;
	windowBottom  = rows;
	windowLeft    = 0;
	windowRight   = cols;
	complexScroll = false;
    }
    public void setWindow(int top, int bottom) {
	setWindow(top, 0, bottom, cols);
    }
    public void setWindow(int top, int left, int bottom, int right) {
	windowTop    = top;
	windowLeft   = left;
	windowBottom = bottom;
	windowRight  = right;
	if(DEBUG) System.out.println("setWindow: " + top + ", " + bottom + ", " + left + ", " + right);

	// Ensure that the selected area is totally outside the scrolling
	// region OR that the scrolling region starts at the top of the
	// screen and the selection is completely above the scrolling
	// regions bottom. This makes things alot easier and is not that much
	// of problem.
	//
	if(hasSelection) {
	    int selRowAnch = selectRowAnchor - visTop;
	    int selRowLast = selectRowLast - visTop;
	    if(top != 0 && (selRowAnch >= 0 || selRowLast >= 0)) {
		if(!(selRowAnch < top && selRowLast < top ||
		     selRowAnch >= bottom && selRowLast >= bottom)) {
		    clearSelection();
		}
	    } else {
		if(!(selRowAnch < bottom && selRowLast < bottom)) {
		    clearSelection();
		}
	    }
	}

	if(windowLeft != 0 || windowRight != cols)
	    complexScroll = true;
	else
	    complexScroll = false;

    }
    public int  getWindowTop() {
	return windowTop;
    }
    public int  getWindowBottom() {
	return windowBottom;
    }
    public int  getWindowLeft() {
	return windowLeft;
    }
    public int  getWindowRight() {
	return windowRight;
    }

    public int getCursorV() {
	return curRow;
    }
    public int getCursorH() {
	return curCol;
    }

    public void cursorSetPos(int v, int h, boolean relative) {
	if(DEBUG) System.out.println("cursorSetPos: " + v + ", " + h + "(" + relative + ")");
	int maxV = rows - 1;
	int maxH = cols - 1;
	int minV = 0;
	int minH = 0;
	if(relative) {
	    v += windowTop;
	    maxV = windowBottom - 1;
	    minV = windowTop;
	    h += windowLeft;
	    maxH = windowRight - 1;
	    minH = windowLeft;
	}
	if(v < minV)
	    v = minV;
	if(h < minH)
	    h = minH;
	if(v > maxV)
	    v = maxV;
	if(h > maxH)
	    h = maxH;
	curRow = v;
	curCol = h;
    }
    public void cursorUp(int n) {
	if(DEBUG) System.out.println("cursorUp: " + n);
	int min = (curRow < windowTop ? 0 : windowTop);
	curRow -= n;
	if(curRow < min)
	    curRow = min;
    }
    public void cursorDown(int n) {
	if(DEBUG) System.out.println("cursorDown: " + n);
	int max = (curRow > windowBottom - 1 ? rows - 1: windowBottom - 1);
	curRow += n;
	if(curRow > max)
	    curRow = max;
    }
    public void cursorForward(int n) {
	if(DEBUG) System.out.println("cursorFwd: " + n);
	curCol += n;
	if(curCol > windowRight)
	    curCol = windowRight;
    }
    public void cursorBackward(int n) {
	if(DEBUG) System.out.println("cursorBack: " + n);
	curCol -= n;
	if(curCol < windowLeft) {
	    if(termOptions[OPT_REV_WRAP]) {
		curCol = windowRight - (windowLeft - curCol);
		cursorUp(1);
	    } else {
		curCol = windowLeft;
	    }
	}
    }
    public void cursorIndex(int n) {
	if(DEBUG) System.out.println("cursorIndex: " + n);
	if(curRow > windowBottom || curRow + n < windowBottom)
	    cursorDown(n);
	else {
	    int m = windowBottom - curRow;
	    cursorDown(m);
	    scrollUp((n - m) + 1);
	}
    }
    public void cursorIndexRev(int n) {
	if(DEBUG) System.out.println("cursorIndexRev: " + n);
	if(curRow < windowTop || curRow - n >= windowTop)
	    cursorUp(n);
	else {
	    int m = curRow - windowTop;
	    scrollDown(n - m);
	    cursorUp(m);
	}
    }

    public void cursorSave() {
	curRowSave  = curRow;
	curColSave  = curCol;
	curAttrSave = curAttr;
    }
    public void cursorRestore() {
	curRow  = curRowSave;
	curCol  = curColSave;
	curAttr = curAttrSave;
    }

    public void scrollUp(int n) {
	int windowHeight = windowBottom - windowTop;
	int i, j = windowTop;

	if(DEBUG) System.out.println("scrollUp: " + n);

	if(complexScroll) {
	    // !!! TODO: This is untested...
	    if(n < windowHeight) {
		j = (windowHeight - n) + windowTop;
		for(i = windowTop; i < j; i++) {
		    System.arraycopy(screen[visTop + i + n], windowLeft, screen[visTop + i],
				     windowLeft, windowRight - windowLeft);
		    System.arraycopy(attributes[visTop + i + n], windowLeft,
				     attributes[visTop + i], windowLeft, (windowRight - windowLeft));
		}
	    }
	    for(i = j; i < windowBottom; i++) {
		System.arraycopy(spacerow, 0, screen[visTop + i], windowLeft, windowRight - windowLeft);
		System.arraycopy(zerorow, 0, attributes[visTop + i], windowLeft, (windowRight - windowLeft));
	    }
	} else {
	    if(windowTop == 0 && (windowBottom == rows) && saveLines > 0) {
		int sl = (n < windowHeight ? n : windowHeight);
		int ll;
		if((visTop + sl) > saveLines) {
		    if(hasSelection) {
			if(!(((selectRowAnchor - n) < 0) || ((selectRowLast   - n) < 0))) {
			    selectRowAnchor -= n;
			    selectRowLast -= n;
			} else {
			    clearSelection();
			}
		    }
		    ll = windowHeight - sl;
		    System.arraycopy(screen, sl, screen, 0, saveLines + ll);
		    System.arraycopy(attributes, sl, attributes, 0, saveLines + ll);
		    for(i = windowHeight - sl; i < windowHeight; i++) {
			screen[saveLines + i]     = new char[cols];
			attributes[saveLines + i] = new int[cols];
		    }
		} else {
		    visTop    += sl;
		    saveVisTop = visTop;
		    updateScrollbarValues();
		}
	    } else {
		if(n < windowHeight) {
		    j = (windowHeight - n) + windowTop;
		    System.arraycopy(screen, visTop + windowTop + n, screen, visTop + windowTop,
				     (windowHeight - n));
		    System.arraycopy(attributes, visTop + windowTop + n, attributes,
				     visTop + windowTop, (windowHeight - n));
		}
		for(i = j; i < windowBottom; i++) {
		    screen[visTop + i]     = new char[cols];
		    attributes[visTop + i] = new int[cols];
		}
	    }
	}

	updateDirtyArea(windowTop, windowLeft, windowBottom, windowRight);
    }

    public void scrollDown(int n) {
	int windowHeight = windowBottom - windowTop;
	int i, j = windowBottom;

	if(DEBUG) System.out.println("scrollDown: " + n);

	if(complexScroll) {
	    // !!! TODO: This is untested...
	    if(n < windowHeight) {
		j = windowTop + n;
		for(i = windowBottom - 1; i >= j; i--) {
		    System.arraycopy(screen[visTop + i - n], windowLeft, screen[visTop + i], windowLeft, windowRight - windowLeft);
		    System.arraycopy(attributes[visTop + i - n], windowLeft,
				     attributes[visTop + i], windowLeft, (windowRight - windowLeft));
		}
	    }
	    for(i = windowTop; i < j; i++) {
		System.arraycopy(spacerow, 0, screen[visTop + i], windowLeft, windowRight - windowLeft);
		System.arraycopy(zerorow, 0, attributes[visTop + i], windowLeft, (windowRight - windowLeft));
	    }
	} else {
	    if(n < windowHeight) {
		j = windowTop + n;
		System.arraycopy(screen, visTop + windowTop, screen, visTop + windowTop + n,
				 windowHeight - n);
		System.arraycopy(attributes, visTop + windowTop, attributes, visTop + windowTop + n,
				 windowHeight - n);
	    }
	    for(i = windowTop; i < j; i++) {
		screen[visTop + i]     = new char[cols];
		attributes[visTop + i] = new int[cols];
	    }
	}
	updateDirtyArea(windowTop, 0, windowBottom, cols);
    }

    public void clearBelow() {
	if(DEBUG) System.out.println("clearBelow");
	clearRight();
	int[] attrLine = new int[cols];
	int i;
	for(i = 0; i < cols; i++)
	    attrLine[i] = (curAttr & (MASK_BGCOL | MASK_FGCOL | ATTR_BGCOLOR | ATTR_FGCOLOR));
	for(i = curRow + 1; i < windowBottom; i++) {
	    screen[visTop + i]     = new char[cols];
	    attributes[visTop + i] = new int[cols];
	    System.arraycopy(attrLine, 0, attributes[visTop + i], 0, cols);
	}
	updateDirtyArea(curRow, 0, windowBottom, cols);
    }
    public void clearAbove() {
	if(DEBUG) System.out.println("clearAbove");
	clearLeft();
	int[] attrLine = new int[cols];
	int i;
	for(i = 0; i < cols; i++)
	    attrLine[i] = (curAttr & (MASK_BGCOL | MASK_FGCOL | ATTR_BGCOLOR | ATTR_FGCOLOR));
	for(i = windowTop; i < curRow; i++) {
	    screen[visTop + i]     = new char[cols];
	    attributes[visTop + i] = new int[cols];
	    System.arraycopy(attrLine, 0, attributes[visTop + i], 0, cols);
	}
	updateDirtyArea(windowTop, 0, curRow, cols);
    }
    public void clearScreen() {
	if(DEBUG) System.out.println("clearScreen");
	int i;
	int[] attrLine = new int[cols];

	for(i = 0; i < cols; i++)
	    attrLine[i] = (curAttr & (MASK_BGCOL | MASK_FGCOL | ATTR_BGCOLOR | ATTR_FGCOLOR));
	for(i = windowTop; i < windowBottom; i++) {
	    screen[saveVisTop + i]     = new char[cols];
	    attributes[saveVisTop + i] = new int[cols];
	    System.arraycopy(attrLine, 0, attributes[saveVisTop + i], 0, cols);
	}

	// Don't call updateDirtyArea(0, 0, rows, cols);
	// we want the values reset instead of updated
	//
	dirtyTop = 0;
	dirtyBottom = rows;
	dirtyLeft = 0;
	dirtyRight = cols;
	repaint();
    }
    public void clearRight() {
	if(DEBUG) System.out.println("clearRight");
	System.arraycopy(spacerow, 0, screen[visTop + curRow], curCol, cols - curCol);
	for(int i = curCol; i < cols; i++) {
	    attributes[visTop + curRow][i] =
		(curAttr & (MASK_BGCOL | MASK_FGCOL | ATTR_BGCOLOR | ATTR_FGCOLOR));
	}
	updateDirtyArea(curRow, curCol, curRow + 1, cols);
    }
    public void clearLeft() {
	if(DEBUG) System.out.println("clearLeft");
	System.arraycopy(spacerow, 0, screen[visTop + curRow], 0, curCol);
	for(int i = 0; i < curCol; i++) {
	    attributes[visTop + curRow][i] =
		(curAttr & (MASK_BGCOL | MASK_FGCOL | ATTR_BGCOLOR | ATTR_FGCOLOR));
	}
	dirtyLeft   = 0;
	updateDirtyArea(curRow, 0, curRow + 1, curCol);
    }
    public void clearLine() {
	if(DEBUG) System.out.println("clearLine");
	screen[visTop + curRow]     = new char[cols];
	attributes[visTop + curRow] = new int[cols];
	for(int i = 0; i < cols; i++)
	    attributes[visTop + curRow][i] =
		(curAttr & (MASK_BGCOL | MASK_FGCOL | ATTR_BGCOLOR | ATTR_FGCOLOR));
	dirtyLeft   = 0;
	dirtyRight  = cols;
	updateDirtyArea(curRow, 0, curRow + 1, cols);
    }

    public void eraseChars(int n) {
	if(DEBUG) System.out.println("eraseChars");
	if(n > cols - curCol)
	    n = cols - curCol;
	System.arraycopy(spacerow, 0, screen[visTop + curRow], curCol, n);
	for(int i = 0; i < n; i++) {
	    attributes[visTop + curRow][curCol + i] =
		(curAttr & (MASK_BGCOL | MASK_FGCOL | ATTR_BGCOLOR | ATTR_FGCOLOR));
	}
	updateDirtyArea(curRow, curCol, curRow, curCol + n);
    }
    public void insertChars(int n) {
	int edge = windowRight;
	if(DEBUG) System.out.println("inserChars: " + n);
	if(curCol < windowLeft || curCol > windowRight)
	    return;
	if((curCol + n) < windowRight) {
	    edge = curCol +  n;
	    System.arraycopy(screen[visTop + curRow], curCol, screen[visTop + curRow], edge,
			     windowRight - edge);
	    System.arraycopy(attributes[visTop + curRow], curCol,
			     attributes[visTop + curRow], edge, (windowRight - edge));
	}
	System.arraycopy(spacerow, 0, screen[visTop + curRow], curCol, edge - curCol);
	for(int i = curCol; i < edge; i++) {
	    attributes[visTop + curRow][i] =
		(curAttr & (MASK_BGCOL | MASK_FGCOL | ATTR_BGCOLOR | ATTR_FGCOLOR));
	}
	updateDirtyArea(curRow, curCol, curRow + 1, windowRight);
    }
    public void deleteChars(int n) {
	int edge = curCol;
	if(DEBUG) System.out.println("deleteChars: " + n);
	if(curCol < windowLeft || curCol > windowRight)
	    return;
	if((curCol + n) < windowRight) {
	    edge = windowRight - n;
	    System.arraycopy(screen[visTop + curRow], curCol + n, screen[visTop + curRow], curCol,
			     edge - curCol);
	    System.arraycopy(attributes[visTop + curRow], (curCol + n), attributes[visTop + curRow],
			     curCol, (edge - curCol));
	}
	System.arraycopy(spacerow, 0, screen[visTop + curRow], edge, windowRight - edge);
	for(int i = edge; i < windowRight; i++) {
	    attributes[visTop + curRow][i] =
		(curAttr & (MASK_BGCOL | MASK_FGCOL | ATTR_BGCOLOR | ATTR_FGCOLOR));
	}
	updateDirtyArea(curRow, curCol, curRow + 1, windowRight);
    }
    public void insertLines(int n) {
	int i, edge = windowBottom;
	if(DEBUG) System.out.println("insertLines: " + n);

	if(curRow < windowTop || curRow > windowBottom)
	    return;

	if(complexScroll) {
	    // !!! TODO: This is untested...
	    if(curRow + n < windowBottom) {
		edge = curRow  + n;
		for(i = windowBottom - 1; i >= edge; i--) {
		    System.arraycopy(screen[visTop + i - n], windowLeft, screen[visTop + i], windowLeft,
				     windowRight - windowLeft);
		    System.arraycopy(attributes[visTop + i - n], windowLeft, attributes[visTop + i],
				     windowLeft, (windowRight - windowLeft));
		}
	    }
	    for(i = curRow; i < edge; i++) {
		System.arraycopy(spacerow, 0, screen[visTop + i], windowLeft, windowRight - windowLeft);
		System.arraycopy(zerorow, 0, attributes[visTop + i], windowLeft, (windowRight - windowLeft));
	    }
	} else {
	    if(curRow + n < windowBottom) {
		edge = curRow + n;
		System.arraycopy(screen, visTop + curRow, screen, visTop + edge, windowBottom - edge);
		System.arraycopy(attributes, visTop + curRow, attributes, visTop + edge,
				 windowBottom - edge);
	    }
	    int[] attrLine = new int[cols];
	    for(i = 0; i < cols; i++)
		attrLine[i] = (curAttr & (MASK_BGCOL | MASK_FGCOL | ATTR_BGCOLOR | ATTR_FGCOLOR));
	    for(i = curRow; i < edge; i++) {
		screen[visTop + i]     = new char[cols];
		attributes[visTop + i] = new int[cols];
		System.arraycopy(attrLine, 0, attributes[visTop + i], 0, cols);
	    }
	}

	updateDirtyArea(curRow, 0, windowBottom, cols);
    }
    public void deleteLines(int n) {
	int i, edge = curRow;
	if(DEBUG) System.out.println("deleteLines: " + n);

	if(curRow < windowTop || curRow > windowBottom)
	    return;

	if(complexScroll) {
	    // !!! TODO: This is untested...
	    if(curRow + n < windowBottom) {
		edge = windowBottom - n - 1;
		for(i = curRow; i <= edge; i++) {
		    System.arraycopy(screen[visTop + i + n], windowLeft, screen[visTop + i], windowLeft,
				     windowRight - windowLeft);
		    System.arraycopy(attributes[visTop + i + n], windowLeft, attributes[visTop + i],
				     windowLeft, (windowRight - windowLeft));
		}
	    }
	    for(i = edge; i < windowBottom; i++) {
		System.arraycopy(spacerow, 0, screen[visTop + i], windowLeft, windowRight - windowLeft);
		System.arraycopy(zerorow, 0, attributes[visTop + i], windowLeft, (windowRight - windowLeft));
	    }
	} else {
	    if(curRow + n < windowBottom) {
		edge = windowBottom - n;
		System.arraycopy(screen, visTop + curRow + n, screen, visTop + curRow,
				 edge - curRow);
		System.arraycopy(attributes, visTop + curRow + n, attributes, visTop + curRow,
				 edge - curRow);
	    }
	    int[] attrLine = new int[cols];
	    for(i = 0; i < cols; i++)
		attrLine[i] = (curAttr & (MASK_BGCOL | MASK_FGCOL | ATTR_BGCOLOR | ATTR_FGCOLOR));
	    for(i = edge; i < windowBottom; i++) {
		screen[visTop + i]     = new char[cols];
		attributes[visTop + i] = new int[cols];
		System.arraycopy(attrLine, 0, attributes[visTop + i], 0, cols);
	    }
	}

	updateDirtyArea(curRow, 0, windowBottom, cols);
    }

    public void setOption(int opt, boolean val) {
	if(DEBUG) System.out.println("setOption " + opt + "=" + val);
	if(opt > termOptions.length || opt < 0)
	    return;

	props.put(TerminalDefProps.defaultPropDesc[opt][0], String.valueOf(val));

	switch(opt) {
	case OPT_REV_VIDEO:
	    Color swap;
	    if(val != termOptions[opt]) {
		termOptions[opt] = val;
		swap             = origBgColor;
		origBgColor      = origFgColor;
		origFgColor      = swap;
		makeAllDirty(true);
	    }
	    break;
	case OPT_VIS_CURSOR:
	    repaint();
	    break;
	case OPT_AUTO_WRAP:
	case OPT_REV_WRAP:
	case OPT_INSERTMODE:
	case OPT_AUTO_LF:
	case OPT_SCROLL_SK:
	case OPT_SCROLL_SI:
	case OPT_LOCAL_PGKEYS:
	case OPT_COPY_CRNL:
	case OPT_ASCII_LDC:
	case OPT_LOCAL_ECHO:
	case OPT_VIS_BELL:
	case OPT_MAP_CTRLSP:
	case OPT_COPY_SEL:
	    break;
	case OPT_DECCOLM:
	    if(termOptions[opt] != val && termOptions[OPT_DEC132COLS]) {
		setProperty("gm", ((val ? 132 : 80) + "x" + rows + savedGeomPos));
		cursorSetPos(0, 0, false);
	    }
	    break;
	case OPT_DEC132COLS:
	    if(menuHandler != null)
		menuHandler.setEnabledOpt(OPT_DECCOLM, val);
	    break;
	}
	termOptions[opt] = val;
	if(menuHandler != null && opt < Terminal.OPT_LAST_MENU)
	    menuHandler.setStateOpt(opt, val);
    }
    public boolean getOption(int opt) {
	if(DEBUG) System.out.println("getOption " + opt);
	if(opt > termOptions.length || opt < 0)
	    return false;
	return termOptions[opt];
    }

    public void setAttribute(int attr, boolean val) {
	if(DEBUG) System.out.println("setAttr " + attr + "=" + val);
	if(val)
	    curAttr |= attr;
	else
	    curAttr &= ~attr;
    }
    public boolean getAttribute(int attr) {
	if(DEBUG) System.out.println("getAttr " + attr);
	if((curAttr & attr) == attr)
	    return true;
	return false;
    }
    public void setForegroundColor(int c) {
	if(DEBUG) System.out.println("setForegroundColor: " + c);
	if(c >= 0 && c < 8) {
	    if((curAttr & ATTR_BOLD) != 0)
		c += 8;
	    curAttr &= ~(ATTR_FGCOLOR | MASK_FGCOL);
	    curAttr |= (ATTR_FGCOLOR | (c << SHIFT_FGCOL));
	} else {
	    curAttr &= ~ATTR_FGCOLOR;
	}
    }
    public void setBackgroundColor(int c) {
	if(DEBUG) System.out.println("setBackgroundColor: " + c);
	if(c >= 0 && c < 8) {
	    curAttr &= ~(ATTR_BGCOLOR | MASK_BGCOL);
	    curAttr |= (ATTR_BGCOLOR | (c << SHIFT_BGCOL));
	} else {
	    curAttr &= ~ATTR_BGCOLOR;
	}
    }
    public void clearAllAttributes() {
	if(DEBUG) System.out.println("clearAllAttributes");
	curAttr = ATTR_CHARDRAWN;
    }

    private void kludgeProcessKeyEvent(KeyEvent e) {
	int    virtKey = e.getKeyCode();
	int    mod     = e.getModifiers();
	char   c       = e.getKeyChar();
	int    gMode   = 0;

	// Consume TAB to be sure no one uses it so we lose focus
	//
	if(virtKey == KeyEvent.VK_TAB) {
	    e.consume();
	}

	if(specialKeyHandler(c, virtKey, mod))
	    return;

	interpreter.keyHandler(virtKey, mod);

	if(c != KeyEvent.CHAR_UNDEFINED && c != (char)0) {
	    int transC;
	    // If keyKludgeFilter return -1 we should not process the char
	    //
	    if((transC = keyKludgeFilter(c, virtKey, mod)) == -1)
		return;

	    c = (char)transC;

	    if(termOptions[OPT_LOCAL_ECHO])
		write(c);

	    if((c == 0x0a || c == 0x0d) && termOptions[OPT_AUTO_LF]) {
		c = 0x0a;
		typedChar((char)0x0d);
	    }
	    typedChar(c);

	    // Reset window to bottom on keypress option
	    //
	    if(visTop != saveVisTop && termOptions[OPT_SCROLL_SK]) {
		visTop = saveVisTop;
		if(haveScrollbar)
		    scrollbar.setValue(visTop);
		makeAllDirty(false);
	    }
	}
    }

    private boolean isNewEvent(KeyEvent e, boolean byPressed) {
	int ch = (int) e.getKeyChar();

        if(lastKeyWasPressed != byPressed) {
            if(ch == lastKeyKludge)
                return false;
            // XXX: Are there any more cases like this?
            //
            if((lastKeyKludge == 0x0a && ch == 0x0d) || 
                (lastKeyKludge == 0x0d && ch == 0x0a)) {
                return false;
            }
        }
        lastKeyKludge     = ch;
        lastKeyWasPressed = byPressed;
        return true;
    }

    //
    // KeyListener interface
    //
    public void keyTyped(KeyEvent e) {
	char c = e.getKeyChar();

	if(DEBUGKEYEVENT) {
	    int virtKey = e.getKeyCode();
	    System.out.println("typed: " + c + " (" + ((int)c) + ") code=" + virtKey);
	}

        if(isNewEvent(e, false))
            kludgeProcessKeyEvent(e);
    }

    public void keyPressed(KeyEvent e) {
	int    virtKey = e.getKeyCode();
	int    mod     = e.getModifiers();
	char   c       = e.getKeyChar();
	int    gMode   = 0;

	if(DEBUGKEYEVENT) {
	    System.out.println("pressed: " + c + " (" + ((int)c) + ") code=" + virtKey);
	}

        if(isNewEvent(e, true))
            kludgeProcessKeyEvent(e);
    }

    public void keyReleased(KeyEvent e) {
	int    virtKey = e.getKeyCode();

	if(DEBUGKEYEVENT) {
	    char c = e.getKeyChar();
	    System.out.println("released: " + c + " (" + ((int)c) + ") code=" + virtKey);
	}

	switch(virtKey) {
	case KeyEvent.VK_ALT:
	    metaKeyKludge = false;
	    break;
	case KeyEvent.VK_CONTROL:
	    ctrlKeyKludge = false;
	    break;
	}
    }

    final boolean specialKeyHandler(int c, int virtKey, int mod) {
	boolean keyProcessed = false;
	byte[] b;

	switch(virtKey) {
	case KeyEvent.VK_ALT:
	    if(ctrlKeyKludge)
		ctrlKeyKludge = false; // !!! Seems that MS-lost sends ctrl+alt for right ALT !!!
	    else
		metaKeyKludge = true;
	    keyProcessed = true;
	    break;
	case KeyEvent.VK_CONTROL:
	    ctrlKeyKludge = true;
	    keyProcessed = true;
	    break;
	case KeyEvent.VK_BACK_SPACE:
	    b = new byte[1];
	    b[0] = bsCharacter;
	    sendBytes(b);
	    keyProcessed = true;
	    break;
	case KeyEvent.VK_DELETE:
	    b = new byte[1];
	    b[0] = delCharacter;
	    sendBytes(b);
	    keyProcessed = true;
	    break;
	case KeyEvent.VK_PAGE_UP:
	case KeyEvent.VK_PAGE_DOWN:
	case KeyEvent.VK_HOME:
	case KeyEvent.VK_END:
	    if(mod == InputEvent.SHIFT_MASK || termOptions[OPT_LOCAL_PGKEYS]) {
		localPageCtrlKeys(virtKey);
		keyProcessed = true;
	    }
	    break;
	case KeyEvent.VK_INSERT:
	    if(mod == InputEvent.SHIFT_MASK) {
		doPaste();
		keyProcessed = true;
	    } else if(mod == InputEvent.CTRL_MASK) {
		doCopy();
		keyProcessed = true;
	    }
	    break;
	case KeyEvent.VK_SHIFT:
	case KeyEvent.VK_CAPS_LOCK:
	    // For some reason there seems to be characters in keyevents with
	    // shift/caps, better filter them out
	    //
	    keyProcessed = true;
	}

	return keyProcessed;
    }
    final int keyKludgeFilter(char c, int virtKey, int mod) {
	//
	// The KeyEvent content seems to be a bit confusing (to say the least...)
	// in some situations given different locale's and especially different
	// platforms... This is not very funny, but then again who said anything
	// about terminal-stuff beeing some kind of amusement...
	//
	// !!!

	int transC = (int)c;

	if(virtKey == KeyEvent.VK_SHIFT ||
	   virtKey == KeyEvent.VK_CONTROL ||
	   virtKey == KeyEvent.VK_CAPS_LOCK ||
	   virtKey == KeyEvent.VK_ALT) {
	    // !!! Swallow keypress for these, seems to contain key chars on some
	    // combinations of JVM/OS/national keyboard
	    //
	    return -1;
	} else if ((mod & InputEvent.CTRL_MASK) != 0) {
	    switch(virtKey) {
	    case KeyEvent.VK_M: // Bug in MRJ (sent 0x0a on ^M, should be ^J)
		transC = 0x0d;
		break;
	    case KeyEvent.VK_SPACE: // To do ctrl-space (for emacs of course)
		if(termOptions[OPT_MAP_CTRLSP])
		    transC = 0;
		break;
	    default:
		if(((mod & InputEvent.ALT_MASK) == 0) &&
		   virtKey >= KeyEvent.VK_A && virtKey <= KeyEvent.VK_Z) {
		    // This is just to be sure that ctrl+<alphabetic-key> ends
		    // up generating the right 'ascii'
		    //
		    transC = ctrlAlphaKey(virtKey);

		    /* !!! TEMPORARILY DISABLED DUE TO CONFLICTS WITH NATIONAL CHARS
		       } else if(c == '@') {
		       transC = 0x00;
		       } else if(c == '[') {
		       transC = 0x1b;
		       } else if(c == '\\') {
		       transC = 0x1c;
		       } else if(c == ']') {
		       transC = 0x1d;
		       } else if(c == '^') {
		       transC = 0x1e;
		       SEE ABOVE REMOVE !!! */
		} else if(c == '_') {
		    transC = 0x1f;
		}
		break;
	    }
	} else {
	    // We always send 0x0d ^M on ENTER no matter where we are...
	    //
	    if (transC == 0x0a && virtKey == KeyEvent.VK_ENTER && !ctrlKeyKludge) {
		transC = 0x0d;
		/* !!! REMOVE
	    } else if(c == '~') {
		// !!! OUCH
		// We MIGHT process the sly tilde character here, Hazeltine warning...
		// (whomever gets the first tilde snatches further processing of it)
		//
		if(!tildeTypedKludge) {
		    tildePressedKludge = true;
		} else {
		    transC = -1;
		}
		*/
	    } else if(c == 65535) {
		// OUCH, JDK 1.2 generates this on the Shift and Caps keys(!)
		//
		transC = -1;
	    } else if(c == 65406) {
		// OUCH, IBM JDK 1.1.6 on Linux generates this on right alt (alt_gr)
		// (swedish-keyboard)
		//
		transC = -1;
	    }

	    // To be able to do meta-<key> in emacs with <left alt>
	    //
	    if(transC != -1 && metaKeyKludge) {
		typedChar((char)27);
	    }
	}

	return transC;
    }
    final int ctrlAlphaKey(int virtKey) {
	int ctrlC = 0;
	switch(virtKey) {
	case KeyEvent.VK_A:
	    ctrlC = 0x01;
	    break;

	case KeyEvent.VK_B:
	    ctrlC = 0x02;
	    break;

	case KeyEvent.VK_C:
	    ctrlC = 0x03;
	    break;

	case KeyEvent.VK_D:
	    ctrlC = 0x04;
	    break;

	case KeyEvent.VK_E:
	    ctrlC = 0x05;
	    break;

	case KeyEvent.VK_F:
	    ctrlC = 0x06;
	    break;

	case KeyEvent.VK_G:
	    ctrlC = 0x07;
	    break;

	case KeyEvent.VK_H:
	    ctrlC = 0x08;
	    break;

	case KeyEvent.VK_I:
	    ctrlC = 0x09;
	    break;

	case KeyEvent.VK_J:
	    ctrlC = 0x0A;
	    break;

	case KeyEvent.VK_K:
	    ctrlC = 0x0B;
	    break;

	case KeyEvent.VK_L:
	    ctrlC = 0x0C;
	    break;

	case KeyEvent.VK_M:
	    ctrlC = 0x0D;
	    break;

	case KeyEvent.VK_N:
	    ctrlC = 0x0E;
	    break;

	case KeyEvent.VK_O:
	    ctrlC = 0x0F;
	    break;

	case KeyEvent.VK_P:
	    ctrlC = 0x10;
	    break;

	case KeyEvent.VK_Q:
	    ctrlC = 0x11;
	    break;

	case KeyEvent.VK_R:
	    ctrlC = 0x12;
	    break;

	case KeyEvent.VK_S:
	    ctrlC = 0x13;
	    break;

	case KeyEvent.VK_T:
	    ctrlC = 0x14;
	    break;

	case KeyEvent.VK_U:
	    ctrlC = 0x15;
	    break;

	case KeyEvent.VK_V:
	    ctrlC = 0x16;
	    break;

	case KeyEvent.VK_W:
	    ctrlC = 0x17;
	    break;

	case KeyEvent.VK_X:
	    ctrlC = 0x18;
	    break;

	case KeyEvent.VK_Y:
	    ctrlC = 0x19;
	    break;

	case KeyEvent.VK_Z:
	    ctrlC = 0x1A;
	    break;
	}
	return ctrlC;
    }
    public final void localPageCtrlKeys(int virtKey) {
	switch(virtKey) {
	case KeyEvent.VK_PAGE_UP:
	    visTop -= rows;
	    if(visTop < 0)
		visTop = 0;
	    updateScrollbarValues();
	    makeAllDirty(true);
	    break;
	case KeyEvent.VK_PAGE_DOWN:
	    visTop += rows;
	    if(visTop > saveVisTop)
		visTop = saveVisTop;
	    updateScrollbarValues();
	    makeAllDirty(true);
	    break;
	case KeyEvent.VK_HOME:
	    visTop = 0;
	    updateScrollbarValues();
	    makeAllDirty(true);
	    break;
	case KeyEvent.VK_END:
	    visTop = saveVisTop;
	    updateScrollbarValues();
	    makeAllDirty(true);
	    break;
	}
    }

    //
    // FocusListener, AdjustmentListener, MouseListener, MouseMotionListener, ComponentListener
    //
    public void focusGained(FocusEvent e) {
	setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
	haveFocus = true;
	updateFocusCursor();
    }
    public void focusLost(FocusEvent e) {
	metaKeyKludge  = false;
	ctrlKeyKludge  = false;
	setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	haveFocus = false;
	updateFocusCursor();
    }
    final synchronized void updateFocusCursor() {
	Graphics g = getGraphics();
	if(g != null) {
	    hideCursor(g);
	    showCursor(g);
	}
    }

    public boolean isFocusTraversable() {
	return true;
    }

    // !!! Since the realization of the window is very different on different platforms
    // (w.r.t. generated events etc.) we don't listen to componentResized event until
    // window is shown, in that instance we also do the pending setGeometry.
    //
    public void componentMoved(ComponentEvent e) {
	// !!! TODO: Do we want to save absolute positions???
    }
    public void emulateComponentShown() {
	componentShown(new java.awt.event.ComponentEvent(ownerFrame, 0));
    }
    public synchronized void componentShown(ComponentEvent e) {
	if(e.getComponent() == ownerFrame && pendingShow) {
	    if(pendingShow) {
		// !!! Ad-hoc wait to let AWT thread in, seems to prevent it from sending a
		// componentResized when getSize() returns bad value AFTER we have done setSize(),
		// this only occurs on Linux (as far as I have seen) due to thread-scheduler lag ?!?
		try {
		    this.wait(500);
		} catch(InterruptedException ee) {
		}
		pendingShow = false;
		setGeometry(getProperty("gm"), true);
	    }
	}
    }
    public void componentHidden(ComponentEvent e) {
    }
    public synchronized void componentResized(ComponentEvent e) {
	Dimension dim = getSize();
	int newCols = (dim.width  - (2 * borderWidth))  / charWidth;
	int newRows = (dim.height - (2 * borderHeight)) / charHeight;
	int oldCols = cols;
	int oldRows = rows;
	char[][] oldScreen     = screen;
	int[][]  oldAttributes = attributes;

	if(DEBUG) System.out.println("componentResized: " + newCols + "x" + newRows + "(" +
				     dim.width + "." + dim.height + ")");

	if(pendingShow ||
	   (e != null && e.getComponent() != this) ||
	   (newCols <= 0 || newRows <= 0)) {
	    return;
	}

	vpixels = dim.height;
	hpixels = dim.width;

	if(newCols != oldCols)
	    clearSelection();

	// We only want to reallocate and do all this work if the component REALLY has
	// changed size, since we seem to get lot's of call-backs here we better check this...
	//
	if(newRows != rows || newCols != cols) {
	    setWindowSize(newRows, newCols);
	    resetWindow(); // !!! Is this right?
	    clearScreen();

	    oldCols = (oldCols < newCols ? oldCols : newCols);
	    if(resizeGravity == GRAVITY_NORTHWEST) {
		int copyRows = (oldRows < newRows ? oldRows : newRows) + saveVisTop;
		for(int i = 0; i < copyRows; i++) {
		    System.arraycopy(oldScreen[i], 0, screen[i], 0, oldCols);
		    System.arraycopy(oldAttributes[i], 0, attributes[i], 0, oldCols);
		}
	    } else {
		if(hasSelection) {
		    selectRowAnchor += newRows - oldRows;
		    selectRowLast   += newRows - oldRows;
		}
		int i, copyRows, fromTop, toTop;
		if(oldRows < newRows) {
		    int linesAdd = newRows - oldRows;
		    copyRows    = oldRows + saveVisTop;
		    fromTop     = 0;
		    curRow     += linesAdd;
		    if(saveVisTop - linesAdd < 0) {
			toTop       = linesAdd;
		    } else {
			toTop       = 0;
			visTop     -= linesAdd;
			saveVisTop -= linesAdd;
		    }
		} else {
		    int linesLost = oldRows - newRows;
		    toTop       = 0;
		    curRow     -= linesLost;
		    if(curRow < 0)
			curRow = 0;
		    if(saveVisTop + linesLost > saveLines) {
			copyRows    = newRows + saveVisTop;
			fromTop     = linesLost;
		    } else {
			copyRows    = oldRows + saveVisTop;
			fromTop     = 0;
			visTop     += linesLost;
			saveVisTop += linesLost;
		    }
		}
		for(i = 0; i < copyRows; i++) {
		    System.arraycopy(oldScreen[i + fromTop], 0, screen[i + toTop], 0, oldCols);
		    System.arraycopy(oldAttributes[i + fromTop], 0, attributes[i + toTop], 0, oldCols);
		}
	    }

	    if(curRow >= newRows)
		curRow = newRows - 1;
	    if(curCol >= newCols)
		curCol = newCols - 1;

	    if(lastCursorRow >= newRows || lastCursorCol >= newCols) {
		cursorDrawn  = false;
		cursorHollow = false;
	    }

	    updateScrollbarValues();

	    signalWindowChanged(rows, cols, vpixels, hpixels);

	    memGraphics = null;

	    String newGM = (cols + "x" + rows + savedGeomPos);
	    propsChanged = true;
	    props.put("geometry", newGM);
	    updateMenus();
	    makeAllDirty(false);
	    requestFocus();
	}

    }

    public void adjustmentValueChanged(AdjustmentEvent e) {
	int adjValue = e.getValue();
	if(adjValue >= 0 && adjValue <= saveVisTop) {
	    visTop = adjValue;
	    makeAllDirty(false);
	}
    }

    public void selectAll() {
	selectRowAnchor = 0;
	selectColAnchor = 0;
	selectRowLast   = saveVisTop + curRow;
	selectColLast   = curCol;
	selectReverse   = false;
	makeSelection(selectRowAnchor, selectColAnchor, selectRowLast, selectColLast);
	hasSelection = true;
	if(termOptions[OPT_COPY_SEL])
	    doCopy();
    }

    public void makeSelection(int startRow, int startCol, int endRow, int endCol) {
	int i, j;
	if(startRow != endRow) {
	    for(i = startCol; i < cols; i++)
		attributes[startRow][i] |= ATTR_SELECTED;
	    for(i = startRow + 1; i < endRow; i++)
		for(j = 0; j < cols; j++)
		    attributes[i][j] |= ATTR_SELECTED;
	    for(i = 0; i <= endCol; i++)
		attributes[endRow][i] |= ATTR_SELECTED;
	} else {
	    for(i = startCol; i <= endCol; i++)
		attributes[startRow][i] |= ATTR_SELECTED;
	}

	startRow -= visTop;
	endRow   -= visTop;
	if(startRow < 0)
	    startRow = 0;
	if(endRow < 0)
	    endRow = 0;

	updateDirtyArea(startRow, 0, endRow + 1, cols);
	repaint();
    }
    public void clearSelection(int startRow, int startCol, int endRow, int endCol) {
	int i, j;
	if(startRow != endRow) {
	    for(i = startCol; i < cols; i++)
		attributes[startRow][i] &= ~ATTR_SELECTED;
	    for(i = startRow + 1; i < endRow; i++)
		for(j = 0; j < cols; j++)
		    attributes[i][j] &= ~ATTR_SELECTED;
	    for(i = 0; i <= endCol; i++)
		attributes[endRow][i] &= ~ATTR_SELECTED;
	} else {
	    for(i = startCol; i <= endCol; i++)
		attributes[startRow][i] &= ~ATTR_SELECTED;
	}

	startRow -= visTop;
	endRow   -= visTop;
	if(startRow < 0)
	    startRow = 0;
	if(endRow < 0)
	    endRow = 0;

	updateDirtyArea(startRow, 0, endRow + 1, cols);
	repaint();
    }

    public void clearSelection() {
	if(!hasSelection)
	    return;
	if(selectReverse)
	    clearSelection(selectRowLast, selectColLast, selectRowAnchor, selectColAnchor);
	else
	    clearSelection(selectRowAnchor, selectColAnchor, selectRowLast, selectColLast);
	hasSelection = false;
	if(clipboard != null)
	    clipboard.clearSelection();
    }

    final int mouseRow(int y) {
	int mouseRow = (y - borderHeight) / charHeight;
	if(mouseRow < 0)
	    mouseRow = 0;
	else if(mouseRow >= rows)
	    mouseRow = rows - 1;
	return mouseRow;
    }
    final int mouseCol(int x) {
	int mouseCol = (x - borderWidth)  / charWidth;
	if(mouseCol < 0)
	    mouseCol = 0;
	else if(mouseCol >= cols)
	    mouseCol = cols - 1;
	return mouseCol;
    }

    public void mouseClicked(MouseEvent e) {
	// !!! TODO: Why is this if here ??? (avoid doing at "start-up"?)
	if(inListeners != null) {
	    if(e.getModifiers() == pasteButton) {
		doPaste();
	    }
	}
	if(e.getModifiers() == InputEvent.BUTTON1_MASK)
	    requestFocus();
    }
    public void mouseEntered(MouseEvent e) {
    }
    public void mouseExited(MouseEvent e) {
    }
    public void mousePressed(MouseEvent e) {
	long now = System.currentTimeMillis();

	int mouseRow = mouseRow(e.getY());
	int mouseCol = mouseCol(e.getX());

	if(DEBUG) System.out.println("char '" + screen[visTop + mouseRow][mouseCol] + "' attr: " +
				     attributes[visTop + mouseRow][mouseCol] + 
				     " fgcol: " +
				     ((attributes[visTop + mouseRow][mouseCol] & MASK_FGCOL) >>> SHIFT_FGCOL) +
				     " bgcol: " +
				     ((attributes[visTop + mouseRow][mouseCol] & MASK_BGCOL) >>> SHIFT_BGCOL));

	if(e.getModifiers() == (popupButton | InputEvent.CTRL_MASK)) {
	    if(popupmenu != null) {
		// !!! Kludge, the ctrl-button upevent is caught elsewhere
		// (and we don't seem to get focusLost?!)
		ctrlKeyKludge = false;
		popupmenu.show(this, e.getX(), e.getY());
	    }
	}

	interpreter.mouseHandler(mouseRow, mouseCol, true, e.getModifiers());

	mouseRow += visTop;

	clearSelection();
	selectRowAnchor = mouseRow;
	selectColAnchor = mouseCol;
	selectRowLast   = mouseRow;
	selectColLast   = mouseCol;

	if((now - lastLeftClick) < 250) {
	    doClickSelect(mouseRow, mouseCol);
	} else {
	    selectClickRow   = -1;
	    selectClickState = false;
	}
	lastLeftClick  = now;
    }
    public void mouseReleased(MouseEvent e) {
	int mouseRow = mouseRow(e.getY());
	int mouseCol = mouseCol(e.getX());

	// !!! TODO: Why is this if here ??? (avoid doing at "start-up"?)
	if(inListeners != null) {
	    if(e.getModifiers() == InputEvent.BUTTON1_MASK) {
		if(hasSelection) {
		    if(termOptions[OPT_COPY_SEL])
			doCopy();
		}
	    }
	}

	interpreter.mouseHandler(mouseRow, mouseCol, false, e.getModifiers());
    }
    public void mouseMoved(MouseEvent e) {
    }
    synchronized public void mouseDragged(MouseEvent e) {
	int mouseRow = (e.getY() - borderHeight) / charHeight;
	int mouseCol = (e.getX() - borderWidth)  / charWidth;

	if(mouseRow < 0)
	    mouseRow = 0;
	else if(mouseRow >= rows)
	    mouseRow = rows - 1;
	if(mouseCol < 0)
	    mouseCol = 0;
	else if(mouseCol >= cols)
	    mouseCol = cols - 1;

	mouseRow += visTop;

	if(mouseRow == selectRowLast && mouseCol == selectColLast && hasSelection) {
	    return;
	}

	boolean backwardSelection;
	boolean backwardsFromLast;

	if(selectRowAnchor > mouseRow ||
	   (selectRowAnchor == mouseRow && mouseCol < selectColAnchor))
	    backwardSelection = true;
	else
	    backwardSelection = false;

	if(backwardSelection != selectReverse) {
	    if(selectReverse)
		clearSelection(selectRowLast, selectColLast, selectRowAnchor, selectColAnchor);
	    else
		clearSelection(selectRowAnchor, selectColAnchor, selectRowLast, selectColLast);
	    selectReverse = backwardSelection;
	    selectRowLast = selectRowAnchor;
	    selectColLast = selectColAnchor;
	}

	if(selectRowLast > mouseRow ||
	   (selectRowLast == mouseRow && mouseCol < selectColLast))
	    backwardsFromLast = true;
	else
	    backwardsFromLast = false;

	if(selectReverse) {
	    if(backwardsFromLast)
		makeSelection(mouseRow, mouseCol, selectRowLast, selectColLast);
	    else
		clearSelection(selectRowLast, selectColLast, mouseRow, mouseCol);
	} else {
	    if(backwardsFromLast)
		clearSelection(mouseRow, mouseCol, selectRowLast, selectColLast);
	    else
		makeSelection(selectRowLast, selectColLast, mouseRow, mouseCol);
	}

	selectReverse = backwardSelection;

	selectRowLast = mouseRow;
	selectColLast = mouseCol;

	hasSelection = true;
    }

    // !!! Ouch !!!
    //
    final int nextPrintedChar(int row, int col) {
	int i;
	for(i = col; i < cols; i++)
	    if(screen[row][i] != 0)
		break;
	return i;
    }

    final int prevPrintedChar(int row, int col) {
	int i;
	for(i = col; i >= 0; i--)
	    if(screen[row][i] != 0)
		break;
	return i;
    }

    final String addSpaces(int start, int end) {
	int n = end - start;

	if(end == cols)
	    return "";

	char[] spaces = new char[n];
	System.arraycopy(spacerow, 0, spaces, 0, n);

	return new String(spaces);
    }

    public String getSelection() {
	if(!hasSelection)
	    return null;

	int startRow;
	int endRow;
	int startCol;
	int endCol;
	int i, j, n;
	String eol;

	if(termOptions[OPT_COPY_CRNL])
	    eol = "\r\n";
	else
	    eol = "\r";

	if(selectReverse) {
	    startRow = selectRowLast;
	    startCol = selectColLast;
	    endRow   = selectRowAnchor;
	    endCol   = selectColAnchor;
	} else {
	    startRow = selectRowAnchor;
	    startCol = selectColAnchor;
	    endRow   = selectRowLast;
	    endCol   = selectColLast;
	}

	StringBuffer result = new StringBuffer();

	if(startRow != endRow) {
	    for(i = startCol; i < cols; i++) {
		if(screen[startRow][i] == 0) {
		    n = nextPrintedChar(startRow, i);
		    result.append(addSpaces(i, n));
		    i = n - 1;
		} else
		    result.append(screen[startRow][i]);
	    }
	    if(i == cols)
		result.append(eol);
	    for(i = startRow + 1; i < endRow; i++) {
		for(j = 0; j < cols; j++) {
		    if(screen[i][j] == 0) {
			n = nextPrintedChar(i, j);
			result.append(addSpaces(j, n));
			j = n - 1;
		    } else
			result.append(screen[i][j]);
		}
		result.append(eol);
	    }
	    for(i = 0; i <= endCol; i++) {
		if(screen[endRow][i] == 0) {
		    n = nextPrintedChar(endRow, i);
		    result.append(addSpaces(i, n));
		    i = n - 1;
		} else
		    result.append(screen[endRow][i]);
	    }
	    if(i == cols)
		result.append(eol);
	} else {
	    for(i = startCol; i <= endCol; i++) {
		if(screen[startRow][i] == 0) {
		    n = nextPrintedChar(startRow, i);
		    result.append(addSpaces(i, n));
		    i = n - 1;
		} else
		    result.append(screen[startRow][i]);
	    }
	    if(i == cols)
		result.append(eol);
	}

	return result.toString();
    }

    public void doClickSelect(int row, int col) {
	if(selectClickRow == row && selectClickState) {
	    selectColAnchor = 0;
	    selectColLast   = cols - 1;
	} else {
	    int i;
	    if(screen[row][col] != 0) {
		for(i = col; i >= 0; i--)
		    if((selectDelims.indexOf(screen[row][i]) != -1) || screen[row][i] == 0)
			break;
		selectColAnchor = i + 1;
		for(i = col; i < cols ; i++)
		    if((selectDelims.indexOf(screen[row][i]) != -1) || screen[row][i] == 0)
			break;
		selectColLast = i - 1;
	    } else {
		selectColAnchor = prevPrintedChar(row, col) + 1;
		selectColLast   = nextPrintedChar(row, col) - 1;
	    }
	    selectColAnchor = (selectColAnchor > col ? col : selectColAnchor);
	    selectColLast   = (selectColLast   < col ? col : selectColLast);
	}
	selectClickState = !selectClickState;
	selectClickRow   = row;
	selectRowAnchor  = row;
	selectRowLast    = row;
	selectReverse    = false;
	hasSelection     = true;
	makeSelection(selectRowAnchor, selectColAnchor, selectRowLast, selectColLast);
    }
    public void doCopy() {
	if(clipboard != null) {
	    clipboard.setSelection(getSelection());
	}
    }
    public void doPaste() {
	if(clipboard != null) {
	    String selection = clipboard.getSelection();
	    if(selection != null) {
		if(termOptions[OPT_LOCAL_ECHO])
		    write(selection);
		sendBytes(selection.getBytes());
	    }
	}
    }

    //
    // Methods overridden from super-class Component + some helper functions
    //

    // !!! TODO: Move the char* init stuff out of this to a separate method
    //
    public Dimension getDimensionOfText(int rows, int cols) {
	FontMetrics fm = getFontMetrics(getFont());
	charWidth      = -1; // !!! Does not seem to work: fm.getMaxAdvance();
	charHeight     = fm.getHeight();
	charMaxAscent  = fm.getMaxAscent();
	charMaxDescent = fm.getMaxDescent();
	charLeading    = fm.getLeading();
	baselineIndex  = charMaxAscent + charLeading - 1;

	if(charWidth == -1)
	    charWidth = fm.charWidth('W');

	return new Dimension((cols * charWidth) + (2 * borderHeight),
			     (rows * charHeight) + (2 * borderWidth));
    }

    public Dimension getPreferredSize() {
	Dimension dim = getDimensionOfText(rows, cols);
	if(DEBUG) System.out.println("getPreferredSize " + cols + "x" + rows + "(" + dim + ")");
	return dim;
    }

    public Dimension getMinimumSize() {
	return getDimensionOfText(MIN_ROWS, MIN_COLS);
    }

    public Dimension getMaximumSize() {
	return getDimensionOfText(MAX_ROWS, MAX_COLS);
    }

    protected final void clearDirtyArea(Graphics source, Graphics dest) {
	boolean clearAll = (dirtyLeft   == 0    &&
			    dirtyRight  == cols &&
			    dirtyTop    == 0    &&
			    dirtyBottom == rows);
	int x, y, w, h;

	if(clearAll) {
	    Dimension dim = getSize();
	    x = 0;
	    y = 0;
	    w = dim.width;
	    h = dim.height;
	} else {
	    x = borderWidth + (charWidth   * dirtyLeft);
	    y = borderHeight + (dirtyTop    * charHeight);
	    w = (charWidth   * (dirtyRight  - dirtyLeft));
	    h = (charHeight  * (dirtyBottom - dirtyTop));
	}

	source.setColor(origBgColor);
	source.fillRect(x, y, w, h);
	source.setColor(origFgColor);
	dest.setClip(x, y, w, h);
    }

    public void repaint() {
	if(!repaintPending && isShowing() && !pendingShow) {
	    super.repaint();
	    repaintPending = true;
	}
    }

    public void paint(Graphics g) {
	update(g);
    }

    final Rectangle getClipRect(Graphics g) {
	Rectangle clipRect = g.getClipBounds();
	if(clipRect == null) {
	    Dimension winSize = getSize();
	    clipRect = new Rectangle(0, 0, winSize.width, winSize.height);
	}
	return clipRect;
    }

    boolean waitForMore  = false;
    boolean insideUpdate = false; // Kludge to avoid endless loop in pathological case...

    synchronized public void update(Graphics g) {
	Rectangle clipRect;
	int x;
	int y;

	// !!! This should not happen but better safe than sorry...
	//
	if(hpixels == 0 || vpixels == 0) {
	    Dimension dim = getSize();
	    vpixels = dim.height;
	    hpixels = dim.width;
	    if(hpixels == 0 || vpixels == 0)
		return;
	}

	int wcnt = 1;
	while(waitForMore) {
	    waitForMore = false;
	    try {
		this.wait(wcnt * 25);
	    } catch (InterruptedException e) {
	    }
	    if(wcnt++ > 3)
		waitForMore = false;
	}

	if((memGraphics == null) ||
	   (memImageSize == null) ||
	   (hpixels != memImageSize.width) ||
	   (vpixels != memImageSize.height)) {
	    memImageSize = new Dimension(hpixels, vpixels);
	    memImage     = createImage(hpixels, vpixels);
	}
	memGraphics = memImage.getGraphics();

	if(!repaintPending) {
	    // If we we don't have a repaint pending the cause for update must be "destroyed"
	    // window content.
	    //
	    dirtyTop    = 0;
	    dirtyBottom = rows;
	    dirtyLeft   = 0;
	    dirtyRight  = cols;
	    clipRect    = getClipRect(g);
	    memGraphics.setClip(clipRect.x, clipRect.y, clipRect.width, clipRect.height);
	    memGraphics.setColor(origBgColor);
	    memGraphics.fillRect(clipRect.x, clipRect.y, clipRect.width, clipRect.height);
	    memGraphics.setColor(origFgColor);
	} else {
	    if(dirtyTop == rows)
		dirtyTop = curRow;
	    if(dirtyBottom == 0)
		dirtyBottom = curRow + 1;
	    if(dirtyRight == 0)
		dirtyRight = curCol + 1;
	    if(dirtyLeft == cols)
		dirtyLeft = curCol;
	    clearDirtyArea(memGraphics, g);
	    clipRect = getClipRect(g);
	}

	int[]  attrRow;
	char[] charRow;
	int    attr;
	int    attrMasked;
	for(int i = dirtyTop; i < dirtyBottom; i++) {
	    y = borderHeight + (i * charHeight);
	    attrRow = attributes[visTop + i];
	    charRow = screen[visTop + i];
	    for(int j = dirtyLeft; j < dirtyRight; j++) {
		attr       = attrRow[j];
		attrMasked = (attr & MASK_ATTR);
		// Skip positions not drawn into
		if(attrMasked == ATTR_CHARNOTDRAWN)
		    continue;
		x = borderWidth  + (charWidth * j);
		if(((attr & ATTR_INVERSE) != 0) ^ ((attr & ATTR_SELECTED) != 0)) {
		    if((attr & ATTR_FGCOLOR) != 0) {
			memGraphics.setColor(termColors[(attr & MASK_FGCOL) >>> SHIFT_FGCOL]);
		    }
		    memGraphics.fillRect(x, y, charWidth, charHeight);
		    if((attr & ATTR_BGCOLOR) != 0) {
			memGraphics.setColor(termColors[(attr & MASK_BGCOL) >>> SHIFT_BGCOL]);
		    } else {
			memGraphics.setColor(origBgColor);
		    }
		} else {
		    if((attr & ATTR_BGCOLOR) != 0) {
			memGraphics.setColor(termColors[(attr & MASK_BGCOL) >>> SHIFT_BGCOL]);
			memGraphics.fillRect(x, y, charWidth, charHeight);
			memGraphics.setColor(origFgColor);
		    }
		    if((attr & ATTR_FGCOLOR) != 0) {
			memGraphics.setColor(termColors[(attr & MASK_FGCOL) >>> SHIFT_FGCOL]);
		    }
		}
		if((attrMasked & ATTR_CHARDRAWN) != 0) {
		    if((attr & ATTR_LINEDRAW) != 0) {
			drawLineDrawChar(memGraphics, x, y, baselineIndex, charRow[j]);
		    } else if((attr & ATTR_BOLD) != 0) {
			memGraphics.setFont(boldFont);
			memGraphics.drawChars(charRow, j, 1, x, y + baselineIndex);
			memGraphics.setFont(plainFont);
		    } else {
			memGraphics.drawChars(charRow, j, 1, x, y + baselineIndex);
		    }
		    if((attr & ATTR_UNDERLINE) != 0)
			memGraphics.drawLine(x, y + baselineIndex, x + charWidth, y + baselineIndex);
		}
		memGraphics.setColor(origFgColor);
	    }
	}

	g.drawImage(memImage, 0, 0, this);

	Rectangle cursor = new Rectangle(borderWidth  + (charWidth * lastCursorCol),
					 borderHeight + (lastCursorRow * charHeight),
					 charWidth, charHeight);
	if(!clipRect.intersects(cursor)) {
	    g.setClip(0, 0, hpixels, vpixels);
	    hideCursor(g);
	} else {
	    Rectangle intersection = clipRect.intersection(cursor);
	    g.setClip(0, 0, hpixels, vpixels);
	    if(!intersection.equals(cursor) && !insideUpdate) {
		// !!! Argh, this is no fun, better repaint it...
		g.setColor(origBgColor);
		g.fillRect(0, 0, hpixels, vpixels);
		g.setColor(origFgColor);
		insideUpdate = true;
		update(g);
		insideUpdate = false;
		return;
	    }
	}
	showCursor(g);

	if(logoDraw && logoImg != null) {
	    x = logoX;
	    y = logoY;
	    if(x == -1) {
		x = (hpixels / 2) - (logoW / 2);
	    }
	    if(y == -1) {
		y = (vpixels / 2) - (logoH / 2);
	    }
	    g.drawImage(logoImg, x, y, logoW, logoH, this);
	}

	// Reset dirty area (i.e. nothing is dirty now)
	//
	repaintPending = false;
	dirtyTop       = rows;
	dirtyBottom    = 0;
	dirtyLeft      = cols;
	dirtyRight     = 0;
    }

    final synchronized void hideCursor(Graphics g) {
	// Hide last drawn cursor
	//
	if(cursorDrawn) {
	    int x = borderWidth  + (charWidth * lastCursorCol);
	    int y = borderHeight + (lastCursorRow * charHeight);
	    if((attributes[visTop + lastCursorRow][lastCursorCol] & ATTR_INVERSE) != 0)
		g.setColor(origFgColor);
	    else
		g.setColor(origBgColor);
	    g.setXORMode(cursorColor);
	    if(cursorHollow) {
		g.drawRect(x, y, charWidth, charHeight - 1);
	    } else {
		g.fillRect(x, y, charWidth, charHeight);
	    }
	    g.setColor(origFgColor);
	    g.setPaintMode();
	    cursorDrawn = false;
	}
    }

    final synchronized void showCursor(Graphics g) {
	// Show current cursor
	//
	if(termOptions[OPT_VIS_CURSOR] && curCol < cols && curRow < rows) {
	    int x = borderWidth  + (charWidth * curCol);
	    int y = borderHeight + (curRow * charHeight);
	    g.setColor(cursorColor);
	    if((attributes[visTop + curRow][curCol] & ATTR_INVERSE) != 0)
		g.setXORMode(origFgColor);
	    else
		g.setXORMode(origBgColor);
	    if(haveFocus) {
		g.fillRect(x, y, charWidth, charHeight);
		cursorHollow = false;
	    } else {
		g.drawRect(x, y, charWidth, charHeight - 1);
		cursorHollow = true;
	    }
	    g.setPaintMode();
	    cursorDrawn = true;
	    lastCursorRow = curRow;
	    lastCursorCol = curCol;
	}
    }

    /*
      Glyph                      ACS            Ascii      VT100
      Name                       Name           Default    Name
      UK pound sign              ACS_STERLING   f          }
      arrow pointing down        ACS_DARROW     v          .
      arrow pointing left        ACS_LARROW     <          ,
      arrow pointing right       ACS_RARROW     >          +
      arrow pointing up          ACS_UARROW     ^          -
      board of squares           ACS_BOARD      #          h
      bullet                     ACS_BULLET     o          ~
      checker board (stipple)    ACS_CKBOARD    :          a
      degree symbol              ACS_DEGREE     \          f
      diamond                    ACS_DIAMOND    +          `
      greater-than-or-equal-to   ACS_GEQUAL     >          z
      greek pi                   ACS_PI         *          {
      horizontal line            ACS_HLINE      -          q
      lantern symbol             ACS_LANTERN    #          i
      large plus or crossover    ACS_PLUS       +          n
      less-than-or-equal-to      ACS_LEQUAL     <          y
      lower left corner          ACS_LLCORNER   +          m
      lower right corner         ACS_LRCORNER   +          j
      not-equal                  ACS_NEQUAL     !          |
      plus/minus                 ACS_PLMINUS    #          g
      scan line 1                ACS_S1         ~          o
      scan line 3                ACS_S3         -          p
      scan line 7                ACS_S7         -          r
      scan line 9                ACS_S9         _          s
      solid square block         ACS_BLOCK      #          0
      tee pointing down          ACS_TTEE       +          w
      tee pointing left          ACS_RTEE       +          u
      tee pointing right         ACS_LTEE       +          t
      tee pointing up            ACS_BTEE       +          v
      upper left corner          ACS_ULCORNER   +          l
      upper right corner         ACS_URCORNER   +          k
      vertical line              ACS_VLINE      |          x
    */

    final void drawLineDrawChar(Graphics g, int x, int y, int bi, char c) {
	int x2 = (x + (charWidth  / 2));
	int y2 = (y + (charHeight / 2));
	int xx = (x + charWidth);
	int yy = (y + charHeight);

	switch(c) {
	case ' ': // Blank
	case '_': // Blank
	    break;
	case '}': // UK pound
	    {
		char[] ca = new char[1];
		ca[0] = (char)0x00a3;
		g.drawChars(ca, 0, 1, x, y + bi);
		break;
	    }
	case '.': // Down arrow
	    break;
	case ',': // Left arrow
	    break;
	case '+': // Right arrow
	    break;
	case '-': // Up arrow
	    break;
	case 'h': // Board of squares
	    break;
	case '~': // Bullet
	    break;
	case 'a': // Checker board (stipple)
	    break;
	case 'f': // Degrees
	    {
		char[] ca = new char[1];
		ca[0] = (char)0x00b0;
		g.drawChars(ca, 0, 1, x, y + bi);
		break;
	    }
	case '`': // Diamond
	    int[] polyX = new int[4];
	    int[] polyY = new int[4];
	    polyX[0] = x2;
	    polyY[0] = y;
	    polyX[1] = xx;
	    polyY[1] = y2;
	    polyX[2] = x2;
	    polyY[2] = yy;
	    polyX[3] = x;
	    polyY[3] = y2;
	    g.fillPolygon(polyX, polyY, 4);
	    break;
	case 'z': // Greater than or equal
	    break;
	case '{': // Pi
	    break;
	case 'i': // Lantern
	    break;
	case 'y': // Less than or equal
	    break;
	case '|': // Not equal
	    break;
	case 'g': // Plus/Minus
	    {
		char[] ca = new char[1];
		ca[0] = (char)0x00b1;
		g.drawChars(ca, 0, 1, x, y + bi);
		break;
	    }
	case 'o': // Horizontal line (top)
	    g.drawLine(x, y, xx, y);
	    break;
	case 'p': // Horizontal line (top-half)
	    break;
	case 'r': // Horizontal line (bottom-half)
	    break;
	case 's': // Horizontal line (bottom)
	    g.drawLine(x, yy, xx, yy);
	    break;
	case '0': // Solid square block
	    break;
	case 'l': // Upper left corner
	    g.drawLine(x2, yy, x2, y2);
	    g.drawLine(x2, y2, xx, y2);
	    break;
	case 'k': // Upper right corner
	    g.drawLine(x, y2, x2, y2);
	    g.drawLine(x2, y2, x2, yy);
	    break;
	case 'm': // Lower left corner
	    g.drawLine(x2, y, x2, y2);
	    g.drawLine(x2, y2, xx, y2);
	    break;
	case 'j': // Lower right corner
	    g.drawLine(x2, y, x2, y2);
	    g.drawLine(x2, y2, x, y2);
	    break;
	case 'q': // Horizontal line (center)
	    g.drawLine(x, y2, xx, y2);
	    break;
	case 'x': // Vertical line
	    g.drawLine(x2, y, x2, yy);
	    break;
	case 'n': // Cross center lines
	    g.drawLine(x2, y, x2, yy);
	    g.drawLine(x, y2, xx, y2);
	    break;
	case 'u': // Right tee
	    g.drawLine(x2, y, x2, yy);
	    g.drawLine(x, y2, x2, y2);
	    break;
	case 't': // Left tee
	    g.drawLine(x2, y, x2, yy);
	    g.drawLine(x2, y2, xx, y2);
	    break;
	case 'v': // Bottom tee
	    g.drawLine(x, y2, xx, y2);
	    g.drawLine(x2, y2, x2, y);
	    break;
	case 'w': // Top tee
	    g.drawLine(x, y2, xx, y2);
	    g.drawLine(x2, y2, x2, yy);
	    break;
	default:
	    if(DEBUG) System.out.println("Unknown line-draw-char: " + c + " (" + ((int)c) + ")");
	    break;
	}
    }

    /* !!! Do we want this? Check Mac once again, this was for buggy AWT
    class FixedScrollbar extends Scrollbar {
	protected boolean onRight = true;

	public FixedScrollbar(int orientation) {
	    super(orientation);
	}
    
	public void setBounds(Rectangle r) {
	    reshape(r.x, r.y, r.width, r.height);
	}

	public void setBounds(int x, int y, int width, int height) {
	    reshape(x, y, width, height);
	}

	// use the deprecated version, because some things still call it
	public void reshape(int x, int y, int width, int height) {
	    if(onRight) {
		x++; // under window frame
		height++; // can overlap with size box
	    } else {
		x--; // under window frame
	    }
      
	    y--; // under title bar
	    height++; // compensate on bottom
	    super.reshape(x, y, width, height);
	}
    
	public void setWindowSide(String sb) {
	    onRight = sb.equals("right");
	}
    
    }
    */

}
