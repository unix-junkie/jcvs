
package com.ice.jcvsii;

import java.util.Vector;
import java.util.Enumeration;
import java.util.StringTokenizer;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;

import com.ice.pref.UserPrefs;


public
class		WorkBenchTreeModel
extends		DefaultTreeModel
	{
	public
	WorkBenchTreeModel( WorkBenchTreeNode rootNode )
		{
		super( rootNode );
		}

	public void
	saveWorkBench( UserPrefs prefs )
		{
		WorkBenchTreeNode rootNode = 
			(WorkBenchTreeNode) this.getRoot();

		this.recursiveSave( prefs, rootNode );
		}

	public void
	recursiveSave( UserPrefs prefs, WorkBenchTreeNode rootNode )
		{
		if ( false )
			System.err.println
				( "SAVE WORKBENCH NODE: "
					+ rootNode.getDefinition().getFullPath() );

		String propName =
			"wb." + rootNode.getDefinition().getFullPath();

		WorkBenchDefinition def = rootNode.getDefinition();

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
			WorkBenchTreeNode[] childs = rootNode.getChildren();

			prefs.setTokens
				( propName + ".children", this.getChildTokens( childs ) );

			for ( int i = 0 ; i < childs.length ; ++i )
				{
				this.recursiveSave( prefs, childs[i] );
				}
			}
		}

	public void
	fireTreeChanged()
		{
		WorkBenchTreeNode rootNode = 
			(WorkBenchTreeNode) this.getRoot();
		this.fireTreeStructureChanged
			( rootNode, rootNode.getPath(), null, null );
		}

	public void
	loadWorkBench( UserPrefs prefs )
		{
		WorkBenchTreeNode rootNode = 
			(WorkBenchTreeNode) this.getRoot();

		this.recursiveLoad( prefs, rootNode );

		this.fireTreeStructureChanged
			( rootNode, rootNode.getPath(), null, null );
		}

	public void
	recursiveLoad( UserPrefs prefs, WorkBenchTreeNode rootNode )
		{
		String rootPath = rootNode.getDefinition().getFullPath();

		String propName = "wb." + rootPath;

		String[] chNames =
			prefs.getTokens( propName + ".children", new String[0] );

		for ( int i = 0 ; i < chNames.length ; ++i )
			{
			String name = chNames[i];

			propName = "wb." + rootPath + "." + name;

			boolean isFolder =
				! prefs.getBoolean( propName + ".isleaf", true );
			String display =
				prefs.getProperty( propName + ".name", null );
			String desc =
				prefs.getProperty( propName + ".desc", null );

			String localRoot =
				isFolder ? null :
					prefs.getProperty( propName + ".local", null );

			if ( display == null || desc == null
					|| (!isFolder && localRoot == null) )
				{
				// UNDONE
				(new Throwable
					( "loadWorkBench: path '" + propName +
						"' appears corrupted." )).printStackTrace();
				continue;
				}

			if ( isFolder )
				{
				WorkBenchDefinition childDef =
					new WorkBenchDefinition
						( name, rootPath, display, desc );

				WorkBenchTreeNode newNode =
					new WorkBenchTreeNode( childDef );

				rootNode.add( newNode );

				this.recursiveLoad( prefs, newNode );
				}
			else
				{
				WorkBenchDefinition childDef =
					new WorkBenchDefinition
						( name, rootPath, display, desc, localRoot );

				WorkBenchTreeNode newNode =
					new WorkBenchTreeNode( childDef );

				rootNode.add( newNode );
				}
			}
		}

	private String[]
	getChildTokens( WorkBenchTreeNode[] childs )
		{
		String[] result = new String[ childs.length ];
		
		for ( int i = 0 ; i < childs.length ; ++i )
			{
			result[i] = childs[i].getDefinition().getName();
			}

		return result;
		}

	}
