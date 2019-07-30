
package com.ice.jcvsii;

import java.awt.Container;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;



class		MainTabPanel
extends		JPanel
	{
	private final MainPanel		parent;

	MainTabPanel(final MainPanel parent)
		{
		super();
		this.parent = parent;
		this.setBorder( new EmptyBorder( 4, 4, 4, 4 ) );
		}

	MainPanel
	getMainPanel()
		{
		return this.parent;
		}

	public MainFrame
	getMainFrame()
		{
		return this.parent.getMainFrame();
		}

	public void
	savePreferences()
		{
		}

	//
	// HACK
	// The setCursor() code is broken.
	// (Geez, Sun, talk about critical and obvious!).
	// So, we set the cursor on the top level ancestor, which
	// appears to do what we want. Hopefully, we can fix this
	// later on to setCursor() on "this" instead!
	//

	void
	setWaitCursor()
		{
		final Container frame = this.getTopLevelAncestor();
		MainFrame.setWaitCursor( frame, true );
		}

	void
	resetCursor()
		{
		final Container frame = this.getTopLevelAncestor();
		MainFrame.setWaitCursor( frame, false );
		}

	}

