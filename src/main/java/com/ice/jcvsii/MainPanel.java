
package com.ice.jcvsii;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

import com.ice.cvsc.CVSProject;
import com.ice.util.AWTUtilities;


public
class		MainPanel
extends		JPanel
implements	ActionListener
	{
	MainFrame				mainFrame = null;

	WorkBenchPanel			wbPanel = null;
	CheckOutPanel			coPanel = null;
	ExportPanel				expPanel = null;
	ImportPanel				impPanel = null;
	CreatePanel				crtPanel = null;
	InitRepositoryPanel		initPanel = null;
	TestConnectPanel		testPanel = null;
	JTabbedPane				tabbedPane = null;


	public
	MainPanel( MainFrame mainFrame )
		{
		super();
		
		this.mainFrame = mainFrame;

		this.setLayout( new BorderLayout() );

		// Create a tab pane
		this.tabbedPane = new JTabbedPane();
		this.add( tabbedPane );

		ResourceMgr rmgr = ResourceMgr.getInstance();

		String tabName = rmgr.getUIString( "mainpan.workbench.tab.name" );
		this.wbPanel = new WorkBenchPanel( this );
		this.tabbedPane.addTab( tabName, null, this.wbPanel );

		tabName = rmgr.getUIString( "mainpan.checkout.tab.name" );
		this.coPanel = new CheckOutPanel( this );
		this.tabbedPane.addTab( tabName, null, this.coPanel );

		tabName = rmgr.getUIString( "mainpan.export.tab.name" );
		this.expPanel = new ExportPanel( this );
		this.tabbedPane.addTab( tabName, null, this.expPanel );

		tabName = rmgr.getUIString( "mainpan.import.tab.name" );
		this.impPanel = new ImportPanel( this );
		this.tabbedPane.addTab( tabName, null, this.impPanel );

		tabName = rmgr.getUIString( "mainpan.create.tab.name" );
		this.crtPanel = new CreatePanel( this );
		this.tabbedPane.addTab( tabName, null, this.crtPanel );

		tabName = rmgr.getUIString( "mainpan.initrep.tab.name" );
		this.initPanel = new InitRepositoryPanel( this );
		this.tabbedPane.addTab( tabName, null, this.initPanel );

		tabName = rmgr.getUIString( "mainpan.testconn.tab.name" );
		this.testPanel = new TestConnectPanel( this );
		this.tabbedPane.addTab( tabName, null, this.testPanel );

		this.tabbedPane.setSelectedIndex( 0 );
		}

	public MainFrame
	getMainFrame()
		{
		return this.mainFrame;
		}

	public void
	loadPreferences()
		{
		this.wbPanel.loadPreferences();
		this.coPanel.loadPreferences();
		this.expPanel.loadPreferences();
		this.impPanel.loadPreferences();
		this.crtPanel.loadPreferences();
		this.initPanel.loadPreferences();
		this.testPanel.loadPreferences();
		}

	public void
	savePreferences()
		{
		this.wbPanel.savePreferences();
		this.coPanel.savePreferences();
		this.expPanel.savePreferences();
		this.impPanel.savePreferences();
		this.crtPanel.savePreferences();
		this.initPanel.savePreferences();
		this.testPanel.savePreferences();
		}

	public void
	setAllTabsEnabled( boolean enabled )
		{
		for ( int i = 0, cnt = this.tabbedPane.getTabCount()
				; i < cnt ; ++i )
			{
			this.tabbedPane.setEnabledAt( i, enabled );
			}
		}

	public void
	addProjectToWorkBench( CVSProject project )
		{
		this.wbPanel.addProjectToWorkBench( project );
		}

	public void
	actionPerformed( ActionEvent event )
		{
		String command = event.getActionCommand();

		System.err.println
			( "UNKNOWN Command '" + command + "'" );
		}

	}

