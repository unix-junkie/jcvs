
package com.ice.config;

import java.util.Enumeration;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;


public
class		ConfigureTreeNode
extends		DefaultMutableTreeNode
	{
	private ConfigureSpec		spec = null;

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
		final StringBuffer result = new StringBuffer();

		final TreeNode[] path = this.getPath();
		// We start at 1 to avoid the root
		for ( int i = 1 ; i < path.length ; ++i )
			{
			result.append( ((ConfigureTreeNode) path[i]).getName() );
			if ( i < path.length - 1 )
				result.append( "." );
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

	public ConfigureTreeNode
	getChild( final String name )
		{
		for ( final Enumeration enumeration = children()
				; enumeration.hasMoreElements() ; )
			{
			final ConfigureTreeNode node =
				(ConfigureTreeNode) enumeration.nextElement();

			if ( node.toString().equals( name ) )
				return node;
			}

		return null;
		}

	}

