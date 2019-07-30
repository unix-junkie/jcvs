
package com.ice.jcvsii;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import com.ice.pref.UserPrefs;


class		WorkBenchTreeModel
extends		DefaultTreeModel
	{
	WorkBenchTreeModel( final TreeNode rootNode )
		{
		super( rootNode );
		}

	public void
	saveWorkBench( final UserPrefs prefs )
		{
		final WorkBenchTreeNode rootNode =
			(WorkBenchTreeNode) this.getRoot();

		this.recursiveSave( prefs, rootNode );
		}

	private void
	recursiveSave(final UserPrefs prefs, final WorkBenchTreeNode rootNode)
		{
		if ( false )
			System.err.println
				( "SAVE WORKBENCH NODE: "
					+ rootNode.getDefinition().getFullPath() );

		final String propName =
			"wb." + rootNode.getDefinition().getFullPath();

		final WorkBenchDefinition def = rootNode.getDefinition();

		prefs.setBoolean( propName + ".isleaf", ! def.isFolder() );
		prefs.setProperty( propName + ".name", def.getDisplayName() );
		prefs.setProperty( propName + ".desc", def.getDescription() );

		if ( ! def.isFolder() )
			{
			prefs.setProperty
				( propName + ".local", def.getLocalDirectory() );
			}
		else
			{
			final WorkBenchTreeNode[] childs = rootNode.getChildren();

			prefs.setTokens
				( propName + ".children", this.getChildTokens( childs ) );

			for ( final WorkBenchTreeNode child : childs )
				{
				this.recursiveSave( prefs, child );
				}
			}
		}

	public void
	fireTreeChanged()
		{
		final DefaultMutableTreeNode rootNode =
				(DefaultMutableTreeNode) this.getRoot();
		this.fireTreeStructureChanged
			( rootNode, rootNode.getPath(), null, null );
		}

	public void
	loadWorkBench( final UserPrefs prefs )
		{
		final WorkBenchTreeNode rootNode =
			(WorkBenchTreeNode) this.getRoot();

		this.recursiveLoad( prefs, rootNode );

		this.fireTreeStructureChanged
			( rootNode, rootNode.getPath(), null, null );
		}

	private void
	recursiveLoad(final UserPrefs prefs, final WorkBenchTreeNode rootNode)
		{
		final String rootPath = rootNode.getDefinition().getFullPath();

		String propName = "wb." + rootPath;

		final String[] chNames =
			prefs.getTokens( propName + ".children", new String[0] );

		for ( final String name : chNames )
			{
			propName = "wb." + rootPath + '.' + name;

			final boolean isFolder =
				! prefs.getBoolean( propName + ".isleaf", true );
			final String display =
				prefs.getProperty( propName + ".name", null );
			final String desc =
				prefs.getProperty( propName + ".desc", null );

			final String localRoot =
				isFolder ? null :
					prefs.getProperty( propName + ".local", null );

			if ( display == null || desc == null
					|| !isFolder && localRoot == null )
				{
				// UNDONE
				new Throwable
					( "loadWorkBench: path '" + propName +
						"' appears corrupted." ).printStackTrace();
				continue;
				}

			if ( isFolder )
				{
				final WorkBenchDefinition childDef =
					new WorkBenchDefinition
						( name, rootPath, display, desc );

				final WorkBenchTreeNode newNode =
					new WorkBenchTreeNode( childDef );

				rootNode.add( newNode );

				this.recursiveLoad( prefs, newNode );
				}
			else
				{
				final WorkBenchDefinition childDef =
					new WorkBenchDefinition
						( name, rootPath, display, desc, localRoot );

				final MutableTreeNode newNode =
					new WorkBenchTreeNode( childDef );

				rootNode.add( newNode );
				}
			}
		}

	private String[]
	getChildTokens( final WorkBenchTreeNode... childs )
		{
		final String[] result = new String[ childs.length ];

		for ( int i = 0 ; i < childs.length ; ++i )
			{
			result[i] = childs[i].getDefinition().getName();
			}

		return result;
		}

	}
