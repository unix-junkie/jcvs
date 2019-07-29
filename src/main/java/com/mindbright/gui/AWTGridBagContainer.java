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

package com.mindbright.gui;

import java.awt.*;
import java.awt.event.*;

public final class AWTGridBagContainer {

    private Container          container;
    private GridBagLayout      grid;
    private GridBagConstraints gridc;

    public AWTGridBagContainer(Container container) {
	grid  = new GridBagLayout();
	gridc = new GridBagConstraints();
	this.container = container;
	container.setLayout(grid);

	gridc.fill   = GridBagConstraints.HORIZONTAL;
	gridc.anchor = GridBagConstraints.WEST;
	gridc.insets = new Insets(4, 4, 0, 4);
    }

    public GridBagConstraints getConstraints() {
	return gridc;
    }

    public void add(Component comp, int gridy, int gridwidth) {
	gridc.gridy     = gridy;
	gridc.gridwidth = gridwidth;
	grid.setConstraints(comp, gridc);
	container.add(comp);
    }

}

