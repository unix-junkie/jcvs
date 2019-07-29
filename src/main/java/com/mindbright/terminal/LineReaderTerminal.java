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

public final class LineReaderTerminal implements TerminalInputListener {

    TerminalWin     terminal;
    StringBuffer    readLineStr;
    boolean         echoStar;
    boolean         isReadingLine;

    volatile boolean ctrlCPressed;
    volatile boolean ctrlDPressed;

    ExternalMessageException extMsg;

    static public class ExternalMessageException extends Exception {
	public ExternalMessageException(String msg) {
	    super(msg);
	}
    }

    public LineReaderTerminal(TerminalWin terminal) {
	this.terminal = terminal;
	terminal.addInputListener(this);
    }

    public void print(String str) {
	if(terminal != null) {
	    terminal.write(str);
	} else {
	    System.out.print(str);
	}
    }

    public void println(String str) {
	if(terminal != null) {
	    terminal.write(str + "\n\r");
	} else {
	    System.out.println(str);
	}
    }

    public void breakPromptLine(String msg) {
	if(isReadingLine) {
	    synchronized(this) {
		extMsg = new ExternalMessageException(msg);
		this.notify();
	    }
	}
    }

    public String readLine(String defaultVal) {
	synchronized(this) {
	    if(defaultVal != null) {
		readLineStr = new StringBuffer(defaultVal);
		terminal.write(defaultVal);
	    } else {
		readLineStr   = new StringBuffer();
	    }
	    isReadingLine = true;
	    try {
		this.wait();
	    } catch (InterruptedException e) {
		/* don't care */
	    }
	    isReadingLine = false;
	}
	return readLineStr.toString();
    }

    public String promptLine(String prompt, String defaultVal, boolean echoStar)
	throws ExternalMessageException
    {
	String line = null;
	if(terminal != null) {
	    terminal.setAttribute(Terminal.ATTR_BOLD, true);
	    terminal.write(prompt);
	    terminal.setAttribute(Terminal.ATTR_BOLD, false);
	    this.echoStar = echoStar;
	    line = readLine(defaultVal);
	    this.echoStar = false;
	} /*
	    else {
	    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	    System.out.print(prompt);
	    line = br.readLine();
	    }
	  */
	if(extMsg != null) {
	    ExternalMessageException msg = extMsg;
	    extMsg = null;
	    throw msg;
	}
	return line;
    }

    public boolean ctrlCPressed() {
	boolean pressed = ctrlCPressed;
	ctrlCPressed = false;
	return pressed;
    }

    // TerminalInputListener interface
    //
    public synchronized void typedChar(char c) {
	if(isReadingLine) {
	    if(c == (char)127 || c == (char)0x08) {
		if(readLineStr.length() > 0) {
		    boolean ctrlChar = false;
		    if(readLineStr.charAt(readLineStr.length() - 1) < ' ') {
			ctrlChar = true;
		    }
		    readLineStr.setLength(readLineStr.length() - 1);
		    terminal.write((char)8);
		    if(ctrlChar) terminal.write((char)8);
		    terminal.write(' ');
		    if(ctrlChar) terminal.write(' ');
		    terminal.write((char)8);
		    if(ctrlChar) terminal.write((char)8);
		} else
		    terminal.doBell();
	    } else if(c == '\r') {
		this.notify();
		terminal.write("\n\r");
	    } else {
		ctrlCPressed = false;
		ctrlDPressed = false;
		readLineStr.append(c);
		if(echoStar)
		    terminal.write('*');
		else
		    terminal.write(c);
	    }
	} else {
	    if(c == (char)0x03) {
		ctrlCPressed = true;
	    } else if(c == (char)0x04) {
		ctrlDPressed = true;
	    }
	}
    }

    public void sendBytes(byte[] b) {
	for(int i = 0; i < b.length; i++)
	    typedChar((char)b[i]);
    }

    public void signalWindowChanged(int rows, int cols, int vpixels, int hpixels) {
    }

    /* DEBUG/TEST
    public static void main(String[] argv) {
	java.awt.Frame frame = new java.awt.Frame();
	TerminalWin terminal = new TerminalWin(frame, new TerminalXTerm());
	LineReaderTerminal linereader = new LineReaderTerminal(terminal);

	frame.setLayout(new java.awt.BorderLayout());
	frame.add(terminal.getPanelWithScrollbar(),
		  java.awt.BorderLayout.CENTER);

	frame.pack();
	frame.show();

	linereader.println("Now entering lines...");
	String line;
	try {
	    while(true) {
		line = linereader.promptLine("prompt> ", "", false);
		System.out.println("line: " + line);
	    }
	} catch (Exception e) {
	    System.out.println("Error: " + e);
	}
    }
    */

}
