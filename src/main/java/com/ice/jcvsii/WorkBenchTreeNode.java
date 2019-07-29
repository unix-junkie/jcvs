
package com.ice.jcvsii;

import java.util.Enumeration;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;


public
class		WorkBenchTreeNode
extends		DefaultMutableTreeNode
	{
	public
	WorkBenchTreeNode( final WorkBenchDefinition def )
		{
		super( def );
		}

	@Override
	public String
	toString()
		{
		return this.getDefinition().getDisplayName();
		}

	public WorkBenchDefinition
	getDefinition()
		{
		return (WorkBenchDefinition) getUserObject();
		}

	@Override
	public boolean
	isLeaf()
		{
		return ! this.getDefinition().isFolder();
		}

	public String
	getPathString()
		{
		final TreeNode[] path = this.getPath();
		final StringBuffer buf = new StringBuffer();

		for ( int i = 0 ; i < path.length ; ++i )
			{
			final WorkBenchTreeNode node = (WorkBenchTreeNode) path[i];
			buf.append( node.getDefinition().getName() );
			if ( i < path.length - 1 )
				buf.append( "." );
			}

		return buf.toString();
		}

	public WorkBenchTreeNode[]
	getChildren()
		{
		final WorkBenchTreeNode[] result =
			new WorkBenchTreeNode[ this.getChildCount() ];

		final Enumeration enumeration = this.children();
		for ( int i = 0 ; enumeration.hasMoreElements() ; ++i )
			result[i] = (WorkBenchTreeNode) enumeration.nextElement();

		return result;
		}

	public WorkBenchDefinition[]
	getChildDefinitions()
		{
		final int cnt = this.getChildCount();

		final WorkBenchDefinition[] result =
			new WorkBenchDefinition[ cnt ];

		final Enumeration enumeration = this.children();
		for ( int i = 0 ; enumeration.hasMoreElements() ; ++i )
			{
			result[i] = ((WorkBenchTreeNode) enumeration.nextElement()).getDefinition();
			}

		return result;
		}

	}

