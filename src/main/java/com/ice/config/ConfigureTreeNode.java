
package com.ice.config;

import java.util.Enumeration;

import javax.swing.tree.TreeNode;
import javax.swing.tree.DefaultMutableTreeNode;


public
class		ConfigureTreeNode
extends		DefaultMutableTreeNode
	{
	private ConfigureSpec		spec = null;

	public
	ConfigureTreeNode( String name )
		{
		super( name );
		}

	public
	ConfigureTreeNode( String name, ConfigureSpec spec )
		{
		super( name );
		this.spec = spec;
		}

	public void
	add( ConfigureTreeNode node )
		{
		int chCnt = this.getChildCount();

		for ( int i = 0 ; i < chCnt ; ++i )
			{
			ConfigureTreeNode chNode =
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
		StringBuffer result = new StringBuffer();

		TreeNode[] path = this.getPath();
		// We start at 1 to avoid the root
		for ( int i = 1 ; i < path.length ; ++i )
			{
			result.append( ((ConfigureTreeNode) path[i]).getName() );
			if ( i < (path.length - 1) )
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
	setPropertySpec( ConfigureSpec spec )
		{
		this.spec = spec;
		}

	public ConfigureTreeNode
	getChild( String name )
		{
		for ( Enumeration enum = children()
				; enum.hasMoreElements() ; )
			{
			ConfigureTreeNode node =
				(ConfigureTreeNode) enum.nextElement();

			if ( node.toString().equals( name ) )
				return node;
			}

		return null;
		}

	}

