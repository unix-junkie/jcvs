
package com.ice.config;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import java.util.Enumeration;

import static java.util.Collections.list;


public
class		ConfigureTreeNode
extends		DefaultMutableTreeNode
	{
	private ConfigureSpec		spec;

	public
	ConfigureTreeNode( final String name )
		{
		super( name );
		}

	public
	ConfigureTreeNode( final String name, final ConfigureSpec spec )
		{
		super( name );
		this.spec = spec;
		}

	public void
	add( final ConfigureTreeNode node )
		{
		final int chCnt = this.getChildCount();

		for ( int i = 0 ; i < chCnt ; ++i )
			{
			final ConfigureTreeNode chNode =
				(ConfigureTreeNode) this.getChildAt( i );

			if ( node.getName().compareTo( chNode.getName() ) < 0 )
				{
				this.insert( node, i );
				return;
				}
			}

		super.add( node );
		}

	public String
	getName()
		{
		return (String) getUserObject();
		}

	public String
	getPathName()
		{
		final StringBuilder result = new StringBuilder();

		final TreeNode[] path = this.getPath();
		// We start at 1 to avoid the root
		for ( int i = 1 ; i < path.length ; ++i )
			{
			result.append( ((ConfigureTreeNode) path[i]).getName() );
			if ( i < path.length - 1 )
				result.append('.');
			}

		return result.toString();
		}

	public ConfigureSpec
	getConfigureSpec()
		{
		return this.spec;
		}

	public void
	setPropertySpec( final ConfigureSpec spec )
		{
		this.spec = spec;
		}

	@SuppressWarnings("RedundantCast")
	public ConfigureTreeNode
	getChild( final String name )
		{
		for ( final TreeNode node : list( (Enumeration<TreeNode>) this.children() ) )
			{
			if ( node.toString().equals( name ) )
				return (ConfigureTreeNode) node;
			}

		return null;
		}

	}

