/*
** Java CVS client application package.
** Copyright (c) 1997-2002 by Timothy Gerard Endres, <time@jcvs.org>
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
import java.util.Vector;


public
class		DiffLayout
implements	LayoutManager
	{
    private int			leftWidth = 0;
    private int			rightWidth = 0;
    private int			preferredWidth = 0;
    private int			preferredHeight = 0;
    private boolean		sizeUnknown = true;


    public DiffLayout()
		{
		this.sizeUnknown = true;
		}

    // Required by LayoutManager.
	public void
	addLayoutComponent( String name, Component comp )
		{
		}

    // Required by LayoutManager.
	public void
	removeLayoutComponent( Component comp )
		{
		}

	private void
	setSizes( Container parent )
		{
		Dimension compSz = null;
		int nComps = parent.getComponentCount();

		//Reset preferred/minimum width and height.
		this.leftWidth = 0;
		this.rightWidth = 0;
		this.preferredWidth = 0;
		this.preferredHeight = 0;

		for ( int i = 0 ; i < nComps ; i += 2 )
			{
			int rowH = 0;

			Component cL = parent.getComponent( i );
			if ( cL.isVisible() )
				{
				Dimension dL = cL.getPreferredSize();
				if ( dL.height > rowH )
					rowH = dL.height;
				if ( dL.width > this.leftWidth )
					this.leftWidth = dL.width;
				}

			if ( (i + 1) < nComps )
				{
				Component cR = parent.getComponent( i + 1 );
				if ( cR.isVisible() )
					{
					Dimension dR = cR.getPreferredSize();
					if ( dR.height > rowH )
						rowH = dR.height;
					if ( dR.width > this.rightWidth )
						this.rightWidth = dR.width;
					}
				}

			this.preferredHeight += rowH;
			}

		this.preferredWidth = this.leftWidth + this.rightWidth;
		}


	// Required by LayoutManager.
	public Dimension
	preferredLayoutSize( Container parent )
		{
		Dimension dim = new Dimension( 0, 0 );

		this.setSizes( parent );

		//Always add the container's insets!
		Insets insets = parent.getInsets();

		dim.width =
			this.preferredWidth 
			+ insets.left + insets.right;

		dim.height =
			this.preferredHeight 
			+ insets.top + insets.bottom;

		this.sizeUnknown = false;

		return dim;
		}

	// Required by LayoutManager.
	public Dimension
	minimumLayoutSize( Container parent )
		{
		return this.preferredLayoutSize( parent );
		}

	// Required by LayoutManager.
    /* 
     * This is called when the panel is first displayed, 
     * and every time its size changes. 
     * Note: You CAN'T assume preferredLayoutSize or 
     * minimumLayoutSize will be called -- in the case 
     * of applets, at least, they probably won't be. 
     */
	public void
	layoutContainer( Container parent )
		{
		Insets insets = parent.getInsets();

		Dimension parentSz = parent.getSize();

		int maxWidth =
			parentSz.width - ( insets.left + insets.right );

		int maxHeight =
			parentSz.height - ( insets.top + insets.bottom );

		int nComps = parent.getComponentCount();

		int previousWidth = 0, previousHeight = 0;
		int x = insets.left, y = insets.top;
		int rowh = 0, start = 0;
		int xFudge = 0;

		// Go through the components' sizes, if neither 
		// preferredLayoutSize nor minimumLayoutSize has 
		// been called.
		if ( this.sizeUnknown )
			{
			this.setSizes( parent );
			}
        
		if ( maxWidth != this.preferredWidth )
			{
			xFudge = (maxWidth - preferredWidth) / 2;
			}

		if ( maxHeight > this.preferredHeight )
			{
		//	yFudge = (maxHeight - preferredHeight)/(nComps - 1);
			}

		for ( int i = 0 ; i < nComps ; i += 2 )
			{
			int rowH = 0;

			x = insets.left;
			Component cL = parent.getComponent( i );
			Component cR = null;

			if ( cL.isVisible() )
				{
				Dimension dL = cL.getPreferredSize();
				if ( dL.height > rowH )
					rowH = dL.height;
				}

			if ( (i + 1) < nComps )
				{
				cR = parent.getComponent( i + 1 );
				if ( cR.isVisible() )
					{
					Dimension dR = cR.getPreferredSize();
					if ( dR.height > rowH )
						rowH = dR.height;
					}
				}

			if ( cL.isVisible() )
				{
				cL.setBounds( x, y, this.leftWidth + xFudge, rowH );
				}

			if ( cR != null && cR.isVisible() )
				{
				x += this.leftWidth + xFudge;
				cR.setBounds( x, y, this.rightWidth + xFudge, rowH );
				}

			y += rowH;
			}
		}
    
	public String
	toString()
		{
		return getClass().getName();
		}

	}
