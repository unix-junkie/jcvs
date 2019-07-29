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

package com.mindbright.ssh;

import java.awt.*;
import java.awt.event.*;

import com.mindbright.gui.AWTConvenience;
import com.mindbright.gui.AWTGridBagContainer;

public final class SSHMiscDialogs {

    public static void alert(String title, String message, Frame parent) {
	Dialog alertDialog = null;
	Label  alertLabel;
	Button okAlertBut;

	alertDialog = new Dialog(parent, title, true);

	AWTGridBagContainer grid = new AWTGridBagContainer(alertDialog);

	grid.getConstraints().fill   = GridBagConstraints.HORIZONTAL;
	grid.getConstraints().anchor = GridBagConstraints.CENTER;
	grid.getConstraints().insets = new Insets(8, 4, 4, 8);

	alertLabel = new Label(message);
	grid.add(alertLabel, 0, GridBagConstraints.REMAINDER);

	okAlertBut = new Button("OK");
	okAlertBut.addActionListener(new AWTConvenience.CloseAction(alertDialog));

	grid.getConstraints().fill   = GridBagConstraints.NONE;
	grid.add(okAlertBut, 1, GridBagConstraints.REMAINDER);

	alertDialog.addWindowListener(new AWTConvenience.CloseAdapter(okAlertBut));

	AWTConvenience.setBackgroundOfChildren(alertDialog);

	alertDialog.setResizable(true);
	alertDialog.setTitle(title);
	alertDialog.pack();

	AWTConvenience.placeDialog(alertDialog);
	okAlertBut.requestFocus();
	alertDialog.setVisible(true);
    }

    public static String password(String title, String message, Frame parent) {
	return textInput(title, message, parent, '*', "", "Password:");
    }

    public static String textInput(String title, String message, Frame parent) {
	return textInput(title, null, parent, (char)0, "", message);
    }

    public static String textInput(String title, String message, Frame parent,
				   String defaultValue)
    {
	return textInput(title, null, parent, (char)0, defaultValue, message);
    }

    private static String textInput;
    public static String textInput(String title, String message, Frame parent,
				   char echo, String defaultValue, String prompt)
    {
	final Dialog    textInputDialog;
	final TextField textTxtInp;
	Label   txtMsgLabel, textPrompt;

	textInputDialog = new Dialog(parent, title, true);

	ActionListener      al;
	AWTGridBagContainer grid = new AWTGridBagContainer(textInputDialog);

	grid.getConstraints().fill   = GridBagConstraints.HORIZONTAL;
	grid.getConstraints().anchor = GridBagConstraints.CENTER;
	grid.getConstraints().insets = new Insets(8, 4, 4, 8);

	if(message != null && message.trim().length() > 0) {
	    txtMsgLabel = new Label(message);
	    grid.add(txtMsgLabel, 0, GridBagConstraints.REMAINDER);
	}

	textPrompt = new Label(prompt);
	grid.getConstraints().anchor = GridBagConstraints.WEST;
	grid.add(textPrompt, 1, 1);

	textTxtInp = new TextField();
	textTxtInp.setText(defaultValue);
	if(echo > (char)0) {
	    textTxtInp.setEchoChar(echo);
	}
	textTxtInp.setColumns(16);
	grid.add(textTxtInp, 1, GridBagConstraints.REMAINDER);

	Panel bp = new Panel(new FlowLayout());

	Button okBut, cancBut;
	bp.add(okBut = new Button("OK"));
	okBut.addActionListener(al = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    if(e.getActionCommand().equals("OK")) {
			textInput = textTxtInp.getText();
		    } else {
			textInput = null;
		    }
		    textInputDialog.setVisible(false);
		}
	    });

	bp.add(cancBut = new Button("Cancel"));
	cancBut.addActionListener(al);

	grid.add(bp, 2, GridBagConstraints.REMAINDER);

	textInputDialog.addWindowListener(new AWTConvenience.CloseAdapter(cancBut));

	AWTConvenience.setKeyListenerOfChildren(textInputDialog,
						new AWTConvenience.OKCancelAdapter(okBut, cancBut),
						null);

	AWTConvenience.setBackgroundOfChildren(textInputDialog);

	textInputDialog.setResizable(true);
	textInputDialog.setTitle(title);
	textInputDialog.pack();

	AWTConvenience.placeDialog(textInputDialog);

	textInputDialog.setVisible(true);

	return textInput;
    }

    private static String setPwdAnswer;
    public static String setPassword(String title, String message, Frame parent)
    {
	final Dialog  setPasswordDialog;
	final TextField setPwdText, setPwdText2;
	Label   setPwdMsgLabel;

	setPasswordDialog = new Dialog(parent, title, true);

	Label               lbl;
	ActionListener      al;
	AWTGridBagContainer grid = new AWTGridBagContainer(setPasswordDialog);

	grid.getConstraints().fill   = GridBagConstraints.HORIZONTAL;
	grid.getConstraints().anchor = GridBagConstraints.CENTER;
	grid.getConstraints().insets = new Insets(8, 4, 4, 8);

	setPwdMsgLabel = new Label(message);
	grid.add(setPwdMsgLabel, 0, GridBagConstraints.REMAINDER);

	grid.getConstraints().anchor = GridBagConstraints.WEST;
	lbl = new Label("Password:");
	grid.add(lbl, 1, 1);

	setPwdText = new TextField("", 12);
	setPwdText.setEchoChar('*');
	grid.add(setPwdText, 1, 1);

	lbl = new Label("Password again:");
	grid.add(lbl, 2, 1);

	setPwdText2 = new TextField("", 12);
	setPwdText2.setEchoChar('*');
	grid.add(setPwdText2, 2, 1);

	Panel bp = new Panel(new FlowLayout());

	Button okBut, cancBut;
	bp.add(okBut = new Button("OK"));

	okBut.addActionListener(al = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    if(e.getActionCommand().equals("OK")) {
			setPwdAnswer = setPwdText.getText();
			if(!setPwdAnswer.equals(setPwdText2.getText())) {
			    setPwdText.setText("");
			    setPwdText2.setText("");
			    return;
			}
		    } else {
			setPwdAnswer = null;
		    }
		    setPasswordDialog.setVisible(false);
		}
	    });

	bp.add(cancBut = new Button("Cancel"));
	cancBut.addActionListener(al);

	grid.add(bp, 3, GridBagConstraints.REMAINDER);

	setPasswordDialog.addWindowListener(new AWTConvenience.CloseAdapter(cancBut));

	AWTConvenience.setKeyListenerOfChildren(setPasswordDialog,
						new AWTConvenience.OKCancelAdapter(okBut, cancBut),
						null);
	AWTConvenience.setBackgroundOfChildren(setPasswordDialog);

	setPasswordDialog.setResizable(true);
	setPasswordDialog.setTitle(title);
	setPasswordDialog.pack();

	AWTConvenience.placeDialog(setPasswordDialog);

	setPasswordDialog.setVisible(true);

	return setPwdAnswer;
    }

    private static boolean confirmRet;
    public static boolean confirm(String title, String message, boolean defAnswer,
				  Frame parent) {
	return confirm(title, message, 0, 0, "Yes", "No", defAnswer, parent, false);
    }

    public static boolean confirm(String title, String message,
				  int rows, int cols,
				  final String yesLbl, String noLbl,
				  boolean defAnswer, Frame parent,
				  boolean scrollbar) {
	final Dialog  confirmDialog;
	Component     confirmText;
	Button  yesBut, noBut;

	confirmDialog = new Dialog(parent, title, true);

	ActionListener      al;
	AWTGridBagContainer grid = new AWTGridBagContainer(confirmDialog);

	grid.getConstraints().fill   = GridBagConstraints.HORIZONTAL;
	grid.getConstraints().anchor = GridBagConstraints.CENTER;
	grid.getConstraints().insets = new Insets(8, 4, 4, 8);

	if(rows == 0 || cols == 0) {
	    confirmText = new Label(message);
	} else {
	    confirmText = new TextArea(message, rows, cols,
				       scrollbar ? TextArea.SCROLLBARS_VERTICAL_ONLY : TextArea.SCROLLBARS_NONE);
	}
	grid.add(confirmText, 0, GridBagConstraints.REMAINDER);

	Panel bp = new Panel(new FlowLayout());

	bp.add(yesBut = new Button(yesLbl));

	yesBut.addActionListener(al = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    if(e.getActionCommand().equals(yesLbl))
			confirmRet = true;
		    else
			confirmRet = false;
		    confirmDialog.setVisible(false);
		}
	    });

	bp.add(noBut = new Button(noLbl));
	noBut.addActionListener(al);

	grid.add(bp, 1, GridBagConstraints.REMAINDER);

	confirmDialog.addWindowListener(new AWTConvenience.CloseAdapter(noBut));

	AWTConvenience.setBackgroundOfChildren(confirmDialog);

	confirmDialog.setResizable(true);
	confirmDialog.pack();

	AWTConvenience.placeDialog(confirmDialog);

	if(defAnswer)
	    yesBut.requestFocus();
	else
	    noBut.requestFocus();

	confirmDialog.setVisible(true);

	return confirmRet;
    }

    public static void notice(String title, String text, int rows, int cols,
			      boolean scrollbar, Frame parent) {
	Dialog   textDialog = null;
	TextArea textArea;
	Button   okBut;

	textDialog = new Dialog(parent, title, true);

	AWTGridBagContainer grid = new AWTGridBagContainer(textDialog);

	grid.getConstraints().fill   = GridBagConstraints.NONE;
	grid.getConstraints().anchor = GridBagConstraints.CENTER;
	grid.getConstraints().insets = new Insets(4, 4, 4, 4);

	textArea = new TextArea(text, rows, cols,
				scrollbar ? TextArea.SCROLLBARS_VERTICAL_ONLY : TextArea.SCROLLBARS_NONE);
	textArea.setEditable(false);
	grid.add(textArea, 0, GridBagConstraints.REMAINDER);

	okBut = new Button("OK");
	okBut.addActionListener(new AWTConvenience.CloseAction(textDialog));

	grid.add(okBut, 1, GridBagConstraints.REMAINDER);

	textDialog.addWindowListener(new AWTConvenience.CloseAdapter(okBut));

	AWTConvenience.setBackgroundOfChildren(textDialog);

	textDialog.setResizable(true);
	textDialog.pack();

	AWTConvenience.placeDialog(textDialog);
	okBut.requestFocus();
	textDialog.setVisible(true);
    }

}
