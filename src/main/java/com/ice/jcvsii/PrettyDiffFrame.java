/*
** Java cvs client application package.
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Event;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

import com.ice.util.AWTUtilities;
import com.ice.util.StringUtilities;


public
class		PrettyDiffFrame
extends		JFrame
implements	ActionListener, SwingConstants
	{
	private int					dCol;
	private int					dRow;

	private final ProjectFrame		projectFrame;
	private final JPanel				mainPanel;
	private JPanel				diffPanel;
	private final JScrollPane			scroller;

	private final Font				lblFont;
	private final Font				rawFont;
	private final Font				timeFont;
	private final Font				titleFont;
	private final Font				headerFont;

	private Color				clrRmv = null;
	private Color				clrChg = null;
	private Color				clrChgDk = null;
	private Color				clrAdd = null;
	private Color				clrNil = null;
	private Color				clrHeader = null;
	private Color				clrTitle = null;
	private Color				clrBack = null;


	public
	PrettyDiffFrame(
			final ProjectFrame projectFrame, final String title, final String fileName,
			final String diffs, final String rev1, final String rev2 )
		{
		super( title );

		this.projectFrame = projectFrame;

		this.clrRmv   = new Color( 255, 204, 204 );
		this.clrChg   = new Color( 153, 255, 153 );
		this.clrChgDk = new Color(  68, 204,  68 );
		this.clrAdd   = new Color( 204, 204, 255 );
		this.clrNil   = new Color( 204, 204, 204 );
		this.clrTitle = new Color( 224, 224, 224 );
		this.clrHeader = new Color( 240, 240, 255 );
		this.clrBack  = Color.white;

		this.mainPanel = new JPanel();
		this.mainPanel.setLayout( new GridBagLayout() );
		this.mainPanel.setOpaque( true );
		this.mainPanel.setBackground( this.clrBack );

		this.scroller = new JScrollPane( this.mainPanel );

 		this.timeFont =
			Config.getPreferences().getFont
				( ConfigConstants.PRETTY_TITLE_FONT,
					new Font( "SansSerif", Font.PLAIN, 10 ) );

 		this.titleFont =
			Config.getPreferences().getFont
				( ConfigConstants.PRETTY_TITLE_FONT,
					new Font( "SansSerif", Font.BOLD, 14 ) );

 		this.headerFont =
			Config.getPreferences().getFont
				( ConfigConstants.PRETTY_HEADER_FONT,
					new Font( "SansSerif", Font.BOLD, 12 ) );

 		this.lblFont =
			Config.getPreferences().getFont
				( ConfigConstants.PRETTY_DIFF_FONT,
					new Font( "Monospaced", Font.PLAIN, 12 ) );

 		this.rawFont =
			Config.getPreferences().getFont
				( ConfigConstants.PRETTY_RAW_FONT,
					new Font( "Monospaced", Font.PLAIN, 10 ) );

		final Container content = this.getContentPane();

		content.setLayout( new BorderLayout( 0, 0 ) );

		content.add( "Center", this.scroller );

		this.establishMenuBar();

		this.establishDiffs( diffs, fileName, rev1, rev2 );

		this.addWindowListener(
			new WindowAdapter()
				{
				// UNDONE
				// We do not dispose here, as these are expensize windows that
				// we do not want to create and dispose of frequently. Only the
				// project frame that owns us can dispose of us!
				@Override
				public void
					windowClosing( final WindowEvent e )
						{ setVisible( false ); dispose(); }

				@Override
				public void
					windowClosed( final WindowEvent e )
						{ windowBeingClosed(); }
				}
			);
		}

	// We are handed the prefs because we "belong" to somebody, and
	// they may have their own concept of preferences, like ProjectFrames.

	public void
	loadPreferences( final Rectangle defBounds )
		{
		this.setBounds
			( this.projectFrame.getPreferences().getBounds
				( ConfigConstants.PRETTY_WINDOW_BOUNDS, defBounds ) );
		}

	public void
	savePreferences()
		{
		final Rectangle bounds = this.getBounds();

		if ( bounds.x >= 0 && bounds.y >= 0
				&& bounds.width > 0 && bounds.height > 0 )
			{
			this.projectFrame.getPreferences().setBounds
				( ConfigConstants.PRETTY_WINDOW_BOUNDS, bounds );
			}
		}

	public void
	windowBeingClosed()
		{
		this.savePreferences();
		}

    public void
    actionPerformed( final ActionEvent evt )
        {
		final String	subCmd;
	    final String	command = evt.getActionCommand();

		if ( command.startsWith( "Close" ) )
			{
			SwingUtilities.invokeLater
				( new Runnable()
					{
					public void run()
						{
						dispose();
						}
					}
				);
			}
		}

	private String
	spaceTabs( final String text )
		{
		final int len = text.length();
		final StringBuffer buf = new StringBuffer( len * 2 );

		for ( int i = 0 ; i < len ; ++i )
			{
			final char ch = text.charAt(i);
			if ( ch == '\t' )
				buf.append( "    " ); // UNDONE configurable!
			else
				buf.append( ch );
			}

		return buf.toString();
		}

	private JLabel
	createDiffLabel( final String text )
		{
		JLabel lbl = null;
		lbl = new JLabel( text );
		lbl.setFont( this.lblFont );
		lbl.setForeground( Color.black );
		lbl.setOpaque( true );
		return lbl;
		}

	private void
	establishDiffs( final String rawDiff, String fileName, final String rev1, final String rev2 )
		{
		JLabel		lbl = null;
		JSeparator	sep = null;

		// UNDONE
		// REVIEW
		// I prefer a loop that processed lines individually. This
		// is very wasteful of memory!
		//
		final String[] lines = StringUtilities.splitString( rawDiff, "\n" );

		int lnIdx = 0;
		final String[] revStrs = { rev1, rev2 };
		final String[] timeStamps = { null, null };

		final int numLines = lines.length;

		// NOTE
		// The diff command for this functionality includes the flags '-u -w'
		// to get the "unified" diff output (designed for patch). This output
		// will always begin with these three lines:
		//
		//    diff -u -w -r1.2 JCVSlet.java
		//    --- ./com/ice/jcvslet/JCVSlet.java	2000/12/16 20:50:50	1.2
		//    +++ ./com/ice/jcvslet/JCVSlet.java	2002/02/10 18:07:10
		//
		// We leverage these lines to determines some global parameters, such
		// as the file's name and the versions being diff-ed. The "fields" are
		// separated by the tab character.
		//
		// However, there are times, such as error cases, when we do not get
		// these two lines. If we see lines, but never see these two, then we
		// will assume that there was an error, and display it as such.
		//

		final Vector errV = new Vector();
		boolean gotDiffLines = false;
		for ( ; lnIdx < numLines ; ++lnIdx )
			{
			if ( lines[lnIdx].startsWith( "--- " )
					|| lines[lnIdx].startsWith( "+++ " ) )
				{
				final int idx = lines[lnIdx].startsWith( "--- " ) ? 0 : 1;

				final StringTokenizer toker =
					new StringTokenizer( lines[lnIdx].substring(4), "\t" );

				if ( toker.hasMoreTokens() )
					{
					final String name = toker.nextToken();
					if ( fileName == null )
						fileName = name;
					}

				if ( toker.hasMoreTokens() )
					{
					final String timestamp = toker.nextToken();
					if ( timeStamps[idx] == null )
						timeStamps[idx] = timestamp;
					}

				if ( toker.hasMoreTokens() )
					{
					final String rev = toker.nextToken();
					if ( revStrs[idx] == null )
						revStrs[idx] = rev;
					}
				else
					{
					if ( revStrs[idx] == null )
						revStrs[idx] = "Current";
					}

				if ( idx == 1 )
					{
					gotDiffLines = true;
					lnIdx++;
					break;
					}
				}
			else
				{
				errV.add( lines[lnIdx] );
				}
			}

		this.dCol = 0;
		this.dRow = 0;

		if ( lnIdx >= numLines )
			{
			lbl = this.createDiffLabel( "No Differences" );
			lbl.setFont( this.headerFont );
			AWTUtilities.constrain(
				this.mainPanel, lbl,
				GridBagConstraints.HORIZONTAL,
				GridBagConstraints.NORTHWEST,
				this.dCol, this.dRow, 2, 1, 1.0, 0.0,
				new Insets( 2,2,2,2 ) );
			return;
			}

		//
		// TITLE LABELS
		//
		if ( fileName != null )
			{
			lbl = new JLabel( "Diffs for '" + fileName + "'", CENTER );

			lbl.setFont( this.titleFont );
			lbl.setOpaque( true );
			lbl.setBackground( this.clrTitle );
			lbl.setForeground( Color.black );
			AWTUtilities.constrain(
				this.mainPanel, lbl,
				GridBagConstraints.HORIZONTAL,
				GridBagConstraints.NORTHWEST,
				this.dCol++, this.dRow, 2, 1, 1.0, 0.0,
				new Insets( 0,0,0,0 ) );

			this.dCol = 0;
			this.dRow++;
			}

		lbl = new JLabel( "Version: " + revStrs[0], CENTER );
		lbl.setFont( this.titleFont );
		lbl.setOpaque( true );
		lbl.setBackground( this.clrTitle );
		lbl.setForeground( Color.black );
		AWTUtilities.constrain(
			this.mainPanel, lbl,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.NORTHWEST,
			this.dCol++, this.dRow, 1, 1, 1.0, 0.0,
			new Insets( 0,0,0,0 ) );

		lbl = new JLabel( "Version: " + revStrs[1], CENTER );
		lbl.setFont( this.titleFont );
		lbl.setOpaque( true );
		lbl.setBackground( this.clrTitle );
		lbl.setForeground( Color.black );
		AWTUtilities.constrain(
			this.mainPanel, lbl,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.NORTHWEST,
			this.dCol++, this.dRow, 1, 1, 1.0, 0.0,
			new Insets( 0,0,0,0 ) );

		this.dCol = 0;
		this.dRow++;

		if ( timeStamps[0] != null || timeStamps[1] == null )
			{
			lbl = new JLabel( timeStamps[0]==null?" ":timeStamps[0], CENTER );
			lbl.setFont( this.timeFont );
			lbl.setOpaque( true );
			lbl.setBackground( this.clrTitle );
			lbl.setForeground( Color.black );
			AWTUtilities.constrain(
				this.mainPanel, lbl,
				GridBagConstraints.HORIZONTAL,
				GridBagConstraints.NORTHWEST,
				this.dCol++, this.dRow, 1, 1, 1.0, 0.0,
				new Insets( 0,0,0,0 ) );

			lbl = new JLabel( timeStamps[1]==null?" ":timeStamps[1], CENTER );
			lbl.setFont( this.timeFont );
			lbl.setOpaque( true );
			lbl.setBackground( this.clrTitle );
			lbl.setForeground( Color.black );
			AWTUtilities.constrain(
				this.mainPanel, lbl,
				GridBagConstraints.HORIZONTAL,
				GridBagConstraints.NORTHWEST,
				this.dCol++, this.dRow, 1, 1, 1.0, 0.0,
				new Insets( 0,0,0,0 ) );

			this.dCol = 0;
			this.dRow++;
			}

		sep = new JSeparator( HORIZONTAL );
		AWTUtilities.constrain(
			this.mainPanel, sep,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.NORTH,
			this.dCol, this.dRow, 2, 1, 1.0, 0.0,
			new Insets( 1,1,1,1 ) );

		this.dCol = 0;
		this.dRow++;

		this.diffPanel = new JPanel();
		this.diffPanel.setLayout( new DiffLayout() );
		this.diffPanel.setOpaque( true );
		this.diffPanel.setBackground( this.clrBack );
		AWTUtilities.constrain(
			this.mainPanel, this.diffPanel,
			GridBagConstraints.BOTH,
			GridBagConstraints.NORTHWEST,
			this.dCol, this.dRow, 2, 1, 1.0, 1.0,
			new Insets( 2,2,2,2 ) );

		this.dCol = 0;
		this.dRow++;

		//
		// DIFFS
		//
		char state = 'D';

		final Vector ltColV = new Vector();
		final Vector rtColV = new Vector();

		for ( ; lnIdx < numLines ; ++lnIdx )
			{
			final String ln = lines[lnIdx];

			if ( ln.startsWith( "@@" ) )
				{
				String[] flds;

				flds = StringUtilities.splitString( ln, " " );

				final String oldStr = flds[1].substring(1);
				final String newStr = flds[2].substring(1);

				flds = StringUtilities.splitString( oldStr, "," );
				String oldLineCnt = "";
				final String oldLineNum = flds[0];
				if ( flds.length > 1 )
					oldLineCnt = flds[1];

				flds = StringUtilities.splitString( newStr, "," );
				String newLineCnt = "1";
				final String newLineNum = flds[0];
				if ( flds.length > 1 )
					newLineCnt = flds[1];

				sep = new JSeparator( HORIZONTAL );
				this.diffPanel.add( sep );
				sep = new JSeparator( HORIZONTAL );
				this.diffPanel.add( sep );

				lbl = this.createDiffLabel( "Line " + oldLineNum );
				lbl.setFont( this.headerFont );
				lbl.setBackground( this.clrHeader );
				lbl.setForeground( Color.black );
				this.diffPanel.add( lbl );

				lbl = this.createDiffLabel( "Line " + newLineNum );
				lbl.setFont( this.headerFont );
				lbl.setBackground( this.clrHeader );
				lbl.setForeground( Color.black );
				this.diffPanel.add( lbl );

				sep = new JSeparator( HORIZONTAL );
				this.diffPanel.add( sep );
				sep = new JSeparator( HORIZONTAL );
				this.diffPanel.add( sep );

				state = 'D'; // DUMPing...
				ltColV.removeAllElements();
				rtColV.removeAllElements();
				}
			else
				{
				final char diffCode = ln.charAt(0);
				String remStr = ln.substring(1);

				remStr = this.spaceTabs( remStr );

				//########
				// ZZ
				// (Hen, zeller@think.de)
				// little state machine to parse unified-diff output
				// in order to get some nice 'ediff'-mode output
				// states:
				//  D "dump"             - just dump the value
				//  R "PreChangeRemove"  - we began with '-' .. so this could be the start of a 'change' area or just remove
				//  C "PreChange"        - okey, we got several '-' lines and moved to '+' lines -> this is a change block
				//#########
				if ( diffCode == '+' )
					{
					if ( state == 'D' )
						{
						// ZZ 'change' never begins with '+': just dump out value

						lbl = this.createDiffLabel( " " );
						lbl.setBackground( this.clrBack );
						this.diffPanel.add( lbl );

						lbl = this.createDiffLabel( remStr.length()==0?" ":remStr );
						lbl.setBackground( this.clrAdd );
						this.diffPanel.add( lbl );
						}
					else
						{
						// ZZ we got minus before
						state = 'C';
						rtColV.addElement( remStr );
						}
					}
				else if ( diffCode == '-' )
					{
					state = 'R';
					ltColV.addElement( remStr );
					}
				else
					{
					// ZZ empty diffcode
					this.appendDiffElements( state, ltColV, rtColV );

					lbl = this.createDiffLabel( remStr );
					lbl.setBackground( this.clrBack );
					this.diffPanel.add( lbl );

					lbl = this.createDiffLabel( remStr );
					lbl.setBackground( this.clrBack );
					this.diffPanel.add( lbl );

					state = 'D';
					ltColV.removeAllElements();
					rtColV.removeAllElements();
					}
				}
			}

		this.appendDiffElements( state, ltColV, rtColV );

		// UNDONE
		/*
		# state is empty if we didn't have any change
		if ( ! $state )
			{
			print "<tr><td colspan=2>&nbsp;</td></tr>";
			print "<tr bgcolor=\"$diffcolorEmpty\" >";
			print "<td colspan=2 align=center>";
			print "<b>- No viewable Change -</b>";
			print "</td></tr>";
			}
		*/

		//
		// LEGEND PANEL
		//
		this.appendLegend( revStrs );

		//
		// RAW DIFF
		//
		if ( false )
			{
			final JTextArea rawArea = new JTextArea( rawDiff );
			rawArea.setFont( this.rawFont );
			rawArea.setOpaque( true );
			rawArea.setEditable( false );
			rawArea.setLineWrap( false );
			rawArea.setTabSize( 4 );

			AWTUtilities.constrain(
				this.mainPanel, rawArea,
				GridBagConstraints.BOTH,
				GridBagConstraints.NORTHWEST,
				this.dCol++, this.dRow, 2, 1, 1.0, 1.0,
				new Insets( 1,1,1,1 ) );

			this.dCol = 0;
			this.dRow++;
			}
		}

	private void
	appendDiffElements( final char state, final Vector ltColV, final Vector rtColV )
		{
		JLabel lbl = null;

		if ( state == 'R' )
			{
			// ZZ we just got remove-lines before
			for ( int j = 0 ; j < ltColV.size() ; ++j )
				{
				lbl = this.createDiffLabel( (String)ltColV.elementAt(j) );
				lbl.setBackground( this.clrRmv );
				this.diffPanel.add( lbl );

				lbl = this.createDiffLabel( " " );
				lbl.setBackground( this.clrBack );
				this.diffPanel.add( lbl );
				}
			}
		else if ( state == 'C' )
			{
			// ZZ state eq "PreChange"
			// ZZ we got removes with subsequent adds
			for ( int j = 0 ; j < ltColV.size() || j < rtColV.size() ; ++j )
				{
				// ZZ dump out both cols
				if ( j < ltColV.size() )
					{
					lbl = this.createDiffLabel( (String)ltColV.elementAt(j) );
					lbl.setBackground( this.clrChg );
					this.diffPanel.add( lbl );
					}
				else
					{
					lbl = this.createDiffLabel( " " );
					lbl.setBackground( this.clrChgDk );
					this.diffPanel.add( lbl );
					}

				if ( j < rtColV.size() )
					{
					lbl = this.createDiffLabel( (String)rtColV.elementAt(j) );
					lbl.setBackground( this.clrChg );
					this.diffPanel.add( lbl );
					}
				else
					{
					lbl = this.createDiffLabel( " " );
					lbl.setBackground( this.clrChgDk );
					this.diffPanel.add( lbl );
					}
				}
			}
		}

	private void
	appendLegend( final String[] revStrs )
		{
		JLabel lbl = null;

		final JPanel outerPan = new JPanel();
		outerPan.setOpaque( true );
		outerPan.setBackground( this.clrBack );
		outerPan.setLayout( new GridBagLayout() );
		outerPan.setBorder(
			new CompoundBorder(
				new EmptyBorder( 3, 3, 3, 3 ),
				new EtchedBorder( EtchedBorder.RAISED ) ) );

		AWTUtilities.constrain(
			this.mainPanel, outerPan,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.NORTH,
			0, this.dRow++, 2, 1, 1.0, 0.0,
			new Insets( 0,0,0,0 ) );

		final JPanel legendPan = new JPanel();
		legendPan.setOpaque( true );
		legendPan.setBackground( this.clrTitle );
		legendPan.setLayout( new GridBagLayout() );

		AWTUtilities.constrain(
			outerPan, legendPan,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.NORTH,
			0, 0, 1, 1, 1.0, 0.0,
			new Insets( 0,0,0,0 ) );

		int subCol = 0;
		int subRow = 0;

		lbl = new JLabel( " LEGEND ", CENTER );
		lbl.setFont( this.titleFont );
		lbl.setOpaque( true );
		lbl.setBackground( this.clrTitle );
		lbl.setForeground( Color.black );
		AWTUtilities.constrain(
			legendPan, lbl,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.NORTH,
			subCol++, subRow, 2, 1, 1.0, 0.0,
			new Insets( 0,0,0,0 ) );

		subCol = 0;
		subRow++;

		AWTUtilities.constrain(
			legendPan, new JSeparator( HORIZONTAL ),
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.NORTH,
			subCol++, subRow, 2, 1, 1.0, 0.0,
			new Insets( 0,0,0,0 ) );

		subCol = 0;
		subRow++;

		String verStr =
			revStrs[0].equalsIgnoreCase( "Current" )
				? "current version"
				: "version " + revStrs[0];

		lbl = new JLabel( "Removed from " + verStr + ".", CENTER );
		lbl.setOpaque( true );
		lbl.setBackground( this.clrRmv );
		lbl.setForeground( Color.black );
		AWTUtilities.constrain(
			legendPan, lbl,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.NORTH,
			subCol++, subRow, 1, 1, 1.0, 0.0,
			new Insets( 0,0,0,0 ) );

		lbl = new JLabel( " " );
		lbl.setOpaque( true );
		lbl.setBackground( this.clrBack );
		AWTUtilities.constrain(
			legendPan, lbl,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.NORTH,
			subCol++, subRow, 1, 1, 1.0, 0.0,
			new Insets( 0,0,0,0 ) );

		subCol = 0;
		subRow++;

		lbl = new JLabel( "Changed between the versions.", CENTER );
		lbl.setOpaque( true );
		lbl.setBackground( this.clrChg );
		lbl.setForeground( Color.black );
		AWTUtilities.constrain(
			legendPan, lbl,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.NORTH,
			subCol++, subRow, 2, 1, 1.0, 0.0,
			new Insets( 0,0,0,0 ) );

		subCol = 0;
		subRow++;

		lbl = new JLabel( " " );
		lbl.setOpaque( true );
		lbl.setBackground( this.clrBack );
		AWTUtilities.constrain(
			legendPan, lbl,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.NORTH,
			subCol++, subRow, 1, 1, 1.0, 0.0,
			new Insets( 0,0,0,0 ) );

		verStr =
			revStrs[1].equalsIgnoreCase( "Current" )
				? "current version"
				: "version " + revStrs[1];

		lbl = new JLabel( "Inserted into " + verStr + ".", CENTER );
		lbl.setOpaque( true );
		lbl.setBackground( this.clrAdd );
		lbl.setForeground( Color.black );
		AWTUtilities.constrain(
			legendPan, lbl,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.NORTH,
			subCol++, subRow, 1, 1, 1.0, 0.0,
			new Insets( 0,0,0,0 ) );

		this.dCol = 0;
		this.dRow++;
		}

	private void
	establishMenuBar()
		{
		JMenuItem		mItem;

		final JMenuBar mBar = new JMenuBar();

		final JMenu mFile = new JMenu( "File", true );
		mBar.add( mFile );
/*
		mItem = new JMenuItem( "Show Project" );
		mFile.add( mItem );
		mItem.addActionListener( this );
		mItem.setActionCommand( "Show" );
		mItem.setAccelerator
			( KeyStroke.getKeyStroke
				( KeyEvent.VK_P, Event.CTRL_MASK ) );


		mFile.addSeparator();
*/
		mItem = new JMenuItem( "Close Window" );
		mFile.add( mItem );
		mItem.addActionListener( this );
		mItem.setActionCommand( "Close" );
		mItem.setAccelerator
			( KeyStroke.getKeyStroke
				( KeyEvent.VK_W, Event.CTRL_MASK ) );

		this.setJMenuBar( mBar );
		}

	}
