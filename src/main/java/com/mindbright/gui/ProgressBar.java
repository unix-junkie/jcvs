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

import com.mindbright.util.Progress;

import java.awt.*;

public class ProgressBar extends Canvas implements Progress {
    long  max     = 0;
    long  current = 0;
    int   width   = 0;
    int   height  = 0;
    Color barColor;

    FontMetrics fm;

    Image    img;
    Graphics memG;

    public synchronized void setBarColor(Color c) {
	barColor = c;
    }
    public void progress(int value) {
	setValue(value);
    }
    public synchronized void setValue(int v) {
	setValue(v, false);
    }
    public synchronized void setValue(long v, boolean repaintNow) {
	current = (v > max ? max : v);
	if(repaintNow) {
	    this.update(getGraphics());
	} else {
	    repaint();
	}
    }
    public synchronized void setMax(long max, boolean reset) {
	this.max = max;
	if(reset)
	    current = 0;
	setValue(current, true);
    }
    public Dimension getPreferredSize() {
	return new Dimension(width, height);
    }
    public ProgressBar(long max, int width, int height) {
	super();
	this.max    = max;
	this.width  = width;
	this.height = height;
	barColor    = Color.black;
    }

    public boolean isFinished() {
	return current == max;
    }

    public void update(Graphics g) {
	paint(g);
    }

    public synchronized void paint(Graphics g) {
	int         perc = (int)(max > 0 ?
				 ((100 * current) / max) : 100);
	int         w = (int)(((double)perc / 100) * (double)(width - 2));
	String      p = perc + "%";

	if(fm == null) {
	    fm = g.getFontMetrics(g.getFont());
	}

	if(img == null) {
	    setBackground(Color.white);
	    img  = createImage(width, height);
	    memG = img.getGraphics();
	}

	memG.setPaintMode();
	memG.setColor(Color.white);
	memG.fillRect(0, 0, width, height);
	memG.setColor(Color.black);

	memG.drawRect(0, 0, width - 1, height - 1);
	memG.drawString(p, (width / 2) - (fm.stringWidth(p) / 2) + 1, (height / 2) + fm.getMaxAscent() + fm.getLeading() - (fm.getHeight() / 2));
	memG.setColor(barColor);
	memG.setXORMode(Color.white);
	memG.fillRect(1, 1, w, height - 2);

	g.drawImage(img, 0, 0, this);
    }
}
