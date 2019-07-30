
package com.ice.config;

import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.tree.DefaultTreeModel;


class		ConfigureTreeModel
extends		DefaultTreeModel
	{
	ConfigureTreeModel()
		{
		super( new ConfigureTreeNode( "Prefs" ) );
		}

	public void
	addPath( final String path, final ConfigureSpec spec )
		{
		final StringTokenizer tokenizer =
			new StringTokenizer( path, ".", false );

		ConfigureTreeNode node;
			ConfigureTreeNode next;
			node = (ConfigureTreeNode) getRoot();

		for ( ; tokenizer.hasMoreTokens() ; )
			{
			final String name = tokenizer.nextToken();
			next = node.getChild( name );

			if ( next == null )
				{
				next = new ConfigureTreeNode( name, spec );
				node.add( next );
				}

			node = next;
			}

		node.setPropertySpec( spec );

		}

	public ConfigureTreeNode
	getPathNode( final String path )
		{
		final StringTokenizer tokenizer =
			new StringTokenizer( path, ".", false );

		ConfigureTreeNode node;
		final ConfigureTreeNode next;
		node = (ConfigureTreeNode) getRoot();

		for ( ; node != null && tokenizer.hasMoreTokens() ; )
			{
			node = node.getChild( tokenizer.nextToken() );
			}

		return node;
		}

	public Vector
	getAllPaths()
		{
		final Vector vector = new Vector();
		getAllPaths( "", vector, (ConfigureTreeNode)getRoot() );
		return vector;
		}

	private void
	getAllPaths(final String path, final Vector vector, final ConfigureTreeNode node )
		{
		ConfigureTreeNode child;
		final int count = node.getChildCount();

		for ( int i = 0 ; i < count ; i++ )
			{
			String next = null;
			child = (ConfigureTreeNode) node.getChildAt(i);
			final String name = child.getName();

				next = path.isEmpty() ? name : path + '.' + name;

			vector.addElement( next );

			getAllPaths( next, vector, child );
			}
		}
	}
