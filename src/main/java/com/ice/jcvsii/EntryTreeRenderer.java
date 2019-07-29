/*
** Copyright (c) 1998 by Timothy Gerard Endres
** <mailto:time@ice.com>  <http://www.ice.com>
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

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.table.TableColumnModel;

import com.ice.cvsc.CVSLog;
import com.ice.cvsc.CVSEntry;
import com.ice.cvsc.CVSTracer;
import com.ice.cvsc.CVSCUtilities;
import com.ice.util.AWTUtilities;


public
class		EntryTreeRenderer
extends		JComponent
implements	TreeCellRenderer
	{
	protected EntryColumnModel model;

	protected String	localRoot;

	protected boolean	isLeaf;
	protected boolean	isExpanded;
	protected boolean	isSelected;
	protected boolean	hasFocus;
	protected boolean	hasTreeFocus;

	protected int		iconWidth;
	protected int		iconHeight;
	protected int		nameOffset = 2;
	protected int		handleIndent = 8;

	protected Icon		icon;
	protected CVSEntry	entry;
	protected EntryNode	node;

	protected Icon		openFolder;
	protected Icon		closedFolder;

	protected Icon		addedFile;
	protected Icon		conflictFile;
	protected Icon		conModFile;
	protected Icon		lostFile;
	protected Icon		removedFile;
	protected Icon		modifiedFile;
	protected Icon		unchangedFile;


	public
	EntryTreeRenderer( String localRoot, EntryColumnModel columnModel )
		{
		this.model = columnModel;

		this.handleIndent = 8;		// REVIEW
		this.loadIconImages();
		this.setPreferredSize( new Dimension( 500, 18 ) );

		this.localRoot = localRoot;
		} 

	public Component
	getTreeCellRendererComponent(
			JTree tree, Object value,
			boolean selected, boolean expanded, boolean leaf,
			int row, boolean hasFocus )
		{
		this.isLeaf = leaf;
		this.isExpanded = expanded;
		this.isSelected = selected;
		this.hasFocus = hasFocus;
		this.hasTreeFocus = tree.hasFocus();

		this.node = (EntryNode) value;
        this.entry = node.getEntry();

		if ( leaf )
			{
			this.setIcon( determineIcon( this.entry ) );
			this.setToolTipText( entry.getName() );
			}
		else
			{
			this.setIcon( expanded ? this.openFolder : this.closedFolder );
			this.setToolTipText( null ); //no tool tip
			} 

		return this;
		}

	public String
	getLocalRoot()
		{
		return this.localRoot;
		}

	public void
	setIcon( Icon icon )
		{
		this.icon = icon;
		}

	public int
	getHandleIndent()
		{
		return this.handleIndent;
		}

	public void
	setHandleIndent( int indent )
		{
		this.handleIndent = indent;
		}

	public CVSEntry
	getEntry()
		{
		return this.entry;
		}

	public void
	setEntry( CVSEntry entry )
		{
		this.entry = entry;
		}

	public int
	getNameWidth()
		{
		return this.model.getNameWidth();
		}

	public void
	setNameWidth( int w )
		{
		this.model.setNameWidth( w );
		}

	public int
	getVersionWidth()
		{
		return this.model.getVersionWidth();
		}

	public void
	setVersionWidth( int w )
		{
		this.model.setVersionWidth( w );
		}

	public int
	getTagWidth()
		{
		return this.model.getTagWidth();
		}

	public void
	setTagWidth( int w )
		{
		this.model.setTagWidth( w );
		}

	public int
	getModifiedWidth()
		{
		return this.model.getModifiedWidth();
		}

	public void
	setModifiedWidth( int w )
		{
		this.model.setModifiedWidth( w );
		}

	public Dimension
	getPreferredSize()
		{
		Insets ins = this.getInsets();

		int w = this.getNameWidth() + this.getVersionWidth()
				+ this.getTagWidth() + this.getModifiedWidth()
				+ ins.left + ins.right;

		return new Dimension( w, 18 ); // REVIEW that 18!
		}

    public void
	paint( Graphics g ) 
		{
		String text = null;

		Insets ins = this.getInsets();

		FontMetrics fm = g.getFontMetrics();

		Shape saveClip = g.getClip();
		Rectangle clipBounds = g.getClipBounds();

		Rectangle bounds = this.getBounds();

		int fAscent = fm.getAscent();
		int fHeight = fm.getHeight();

		int insH = (ins.left + ins.right);
		int insV = (ins.top + ins.bottom);

		int width = bounds.width - insH;
		int height = bounds.height - insV;

		int baseLine =
			ins.top + (((height - fHeight) + 1) / 2) + fAscent;

		Rectangle iconR =
			new Rectangle( ins.left, ins.top, 2, height );

		if ( this.icon != null )
			{
			int iconH = this.icon.getIconHeight();

			if ( iconH <= (baseLine - ins.top) )
				iconR.y = baseLine - iconH;
			else
				iconR.y = ins.top + ((bounds.height - iconH) / 2);
			
			iconR.width = this.iconWidth;
			}

		int nameW = this.getNameWidth() -
					(bounds.x + ins.left + this.handleIndent);

		Rectangle nameR =
			new Rectangle
			// UNDONE missing iconToTextOffset...
				( (iconR.x + iconR.width), ins.top, nameW, height );

		if ( this.icon != null )
			{
			int tl = iconR.x + this.nameOffset
						 + this.icon.getIconWidth();

			if ( tl > nameR.x )
				nameR.x = tl;
			}

		// REVIEW intercell spacing?
		Rectangle versR =
			new Rectangle
				( (nameR.x + nameR.width), ins.top,
					this.getVersionWidth(), height );

		Rectangle tagR =
			new Rectangle
				( (versR.x + versR.width), ins.top,
					this.getTagWidth(), height );

		Rectangle modfR =
			new Rectangle
				( (tagR.x + tagR.width), ins.top,
					this.getModifiedWidth(), height );

		g.setFont( this.getFont() );

		//
		// P A I N T    I C O N
		//
		if ( this.icon != null)
			{
			this.icon.paintIcon( this, g, iconR.x, iconR.y );
			}

		//
		// E N T R Y    N A M E
		//
		text = this.entry.getName();
		if ( text != null )
			{
			int textX = nameR.x + 1; // REVIEW should be property
			int textY = nameR.y + baseLine;

			g.setClip( nameR.intersection(clipBounds) );

			if ( this.isSelected )
				{
				int w = fm.stringWidth( text ) + 3;
				Rectangle hiR =
					new Rectangle
						( nameR.x, textY - fAscent, w, fHeight );

				g.setColor( Color.lightGray );
				g.fillRect( hiR.x, hiR.y, hiR.width, hiR.height );

				if ( this.hasFocus )
					{
					g.setColor( Color.gray );
					g.drawRect( hiR.x, hiR.y, hiR.width, hiR.height );
					}
				}

			g.setColor( Color.black );
			g.drawString( text, textX, textY );
			}

		if ( this.isLeaf )
			{
			//
			// E N T R Y    V E R S I O N
			//
			text = this.node.getEntryVersion();
			if ( text != null )
				{
				g.setClip( versR.intersection(clipBounds) );

				int textX = versR.x + 1; // REVIEW should be property
				int textY = versR.y + baseLine;

				g.setColor( Color.black );
				g.drawString( text, textX, textY );

				if ( this.isSelected && this.hasFocus )
					{
					int w = fm.stringWidth( text ) + 1;
					int x1 = versR.x;
					int x2 = versR.x + w;
					int y = textY + 1;
					g.setColor( Color.gray );
					g.drawLine( x1, y, x2, y );
					}
				}

			//
			// T A G    V E R S I O N
			//
			text = this.node.getEntryTag();
			if ( text != null )
				{
				g.setClip( tagR.intersection(clipBounds) );

				int textX = tagR.x + 1; // REVIEW should be property
				int textY = tagR.y + baseLine;

				g.setColor( Color.black );
				g.drawString( text, textX, textY );

				if ( this.isSelected && this.hasFocus )
					{
					int w = fm.stringWidth( text ) + 1;
					int x1 = tagR.x;
					int x2 = tagR.x + w;
					int y = textY + 1;
					g.setColor( Color.gray );
					g.drawLine( x1, y, x2, y );
					}
				}

			//
			// E N T R Y    T I M E S T A M P
			//
			text = this.node.getEntryTimestamp();
			if ( text != null )
				{
				g.setClip( modfR.intersection(clipBounds) );

				int textX = modfR.x + 1; // REVIEW should be property
				int textY = modfR.y + baseLine;

				if ( false && this.isSelected )
					{
					int w = fm.stringWidth( text ) + 3;
					Rectangle hiR =
						new Rectangle
							( modfR.x, textY - fAscent, w, fHeight );

					g.setColor( Color.lightGray );
					g.fillRect( hiR.x, hiR.y, hiR.width, hiR.height );
					}

				g.setColor( Color.black );
				g.drawString( text, textX, textY );

				if ( this.isSelected && this.hasFocus )
					{
					int w = fm.stringWidth( text ) + 1;
					int x1 = modfR.x;
					int x2 = modfR.x + w;
					int y = textY + 1;
					g.setColor( Color.gray );
					g.drawLine( x1, y, x2, y );
					}
				}
			}

		if ( false )
			{
			g.setColor( new Color( 0, 0, 255 ) );
			g.drawRect( iconR.x, iconR.y, iconR.width, iconR.height );
			g.setColor( new Color( 0, 255, 0 ) );
			g.drawRect( nameR.x, nameR.y, nameR.width, nameR.height );
			g.setColor( new Color( 255, 0, 0 ) );
			g.drawRect( versR.x, versR.y, versR.width, versR.height );
			g.setColor( new Color( 0, 0, 255 ) );
			g.drawRect( tagR.x, tagR.y, tagR.width, tagR.height );
			g.setColor( new Color( 0, 255, 0 ) );
			g.drawRect( modfR.x, modfR.y, modfR.width, modfR.height );
			g.setColor( new Color( 64, 128, 64 ) );
			g.drawRect( bounds.x, bounds.y, bounds.width, bounds.height );
			}

		g.setClip( saveClip );
		}

	private Icon
	determineIcon( CVSEntry entry )
		{
		String path =
			this.localRoot + File.separator + this.entry.getFullName();

		File eFile = new File( CVSCUtilities.exportPath( path ) );

		if ( entry.isToBeRemoved() )
			return this.removedFile;
		else if ( entry.isNewUserFile() )
			return this.addedFile;
		else if ( ! eFile.exists() )
			return this.lostFile;
		else if ( entry.isInConflict() )
			return this.conflictFile;
		else if ( entry.isLocalFileModified( eFile ) )
			return this.modifiedFile;
		else
			return this.unchangedFile;
		}

	public void
	loadIconImages()
		{
		Image		image = null;
		Vector		names = new Vector();
		Hashtable	iconTable;

		iconTable = new Hashtable();

		names.addElement( "openFolder" );
		names.addElement( "closedFolder" );

		names.addElement( "unchangedFile" );
		names.addElement( "modifiedFile" );
		names.addElement( "removedFile" );
		names.addElement( "lostFile" );
		names.addElement( "addedFile" );
		names.addElement( "conflictFile" );
		names.addElement( "conModFile" );

		Toolkit tk = this.getToolkit();

		int maxWidth = 0;
		int maxHeight = 0;
		int width, height;

		MediaTracker tracker = new MediaTracker( this );

		for ( int i = 0 ; i < names.size() ; ++i )
			{
			String iconName =
				(String) names.elementAt( i );

			String imageURLName =
				"/com/ice/jcvsii/images/icons/" + iconName + ".gif";

			try {
				image = AWTUtilities.getImageResource( imageURLName );
				}
			catch ( IOException ex )
				{
				image = null;
				CVSLog.logMsg
					( "EntryTreeRenderer.loadIconImages: "
						+ "IO error loading icon '"
						+ iconName + "', " + ex.getMessage() );
				}

			if ( image != null )
				{
				tracker.addImage( image, 0 );
				iconTable.put( iconName, image );
				}
			else
				{
				CVSTracer.traceIf( true,
					"EntryTreeRenderer.loadIconImages: ERROR "
						+ "icon '" + iconName + " failed to load "
						+ "from URL '" +imageURLName+ "'" );
				}
			}

		try { tracker.waitForAll(); }
		catch ( InterruptedException ex )
			{
			CVSTracer.traceWithStack
				( "EntryTreeRenderer.loadIconImages: "
					+ "media tracker interrupted!\n"
					+ "   " + ex.getMessage() );
			}

		Enumeration enum = iconTable.elements();
		for ( ; enum.hasMoreElements() ; )
			{
			image = (Image) enum.nextElement();

			width = image.getWidth( null );
			height = image.getHeight( null );

			if ( width < 0 || height < 0 )
				{
				CVSTracer.traceWithStack
					( "EntryTreeRenderer.loadIconImages: "
						+ "NEGATIVE DIMENSION: "
						+ " Width " + width + " Height " + height );			
				}
			else
				{
				if ( width > maxWidth ) maxWidth = width;
				if ( height > maxHeight ) maxHeight = height;
				}
			}

		this.iconWidth = maxWidth;
		this.iconHeight = maxHeight;

		this.openFolder		= new ImageIcon( (Image) iconTable.get( "openFolder" ) );
		this.closedFolder	= new ImageIcon( (Image) iconTable.get( "closedFolder" ) );

		this.addedFile		= new ImageIcon( (Image) iconTable.get( "addedFile" ) );
		this.conflictFile	= new ImageIcon( (Image) iconTable.get( "conflictFile" ) );
		this.conModFile		= new ImageIcon( (Image) iconTable.get( "conModFile" ) );
		this.lostFile		= new ImageIcon( (Image) iconTable.get( "lostFile" ) );
		this.removedFile	= new ImageIcon( (Image) iconTable.get( "removedFile" ) );
		this.modifiedFile	= new ImageIcon( (Image) iconTable.get( "modifiedFile" ) );
		this.unchangedFile	= new ImageIcon( (Image) iconTable.get( "unchangedFile" ) );
		}

	}


