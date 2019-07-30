
package com.ice.config;

import java.awt.Dimension;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeSelectionModel;


class		ConfigureTree
extends		JTree
	{

	ConfigureTree( final TreeModel model )
		{
		this.setShowsRootHandles( true );
		this.setRootVisible( false );
		this.setScrollsOnExpand( false );
		this.setModel( model );

		this.getSelectionModel().setSelectionMode
			( TreeSelectionModel.SINGLE_TREE_SELECTION );

		this.putClientProperty( "JTree.lineStyle", "Angled" );

		final DefaultTreeCellRenderer defRend =
			new DefaultTreeCellRenderer()
				{
				/**
				 * Overrides return slightly taller preferred size value.
				 */
				@Override
				public Dimension
				getPreferredSize()
					{
					final Dimension result = super.getPreferredSize();
					if ( result != null ) result.height += 1;
					return result;
					}
				};

		defRend.setLeafIcon( null );
		defRend.setOpenIcon( null );
		defRend.setClosedIcon( null );

		this.setCellRenderer( defRend );
		}

	}

