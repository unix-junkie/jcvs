
package com.ice.jcvsii;


import java.io.Serializable;

public
class		WorkBenchDefinition
implements	Serializable
	{
	private final boolean		leaf;
	private final String		name;
	private final String		path;
	private final String		displayName;
	private final String		description;
	private final String		localRoot;

	/**
	 * Folder
	 */

	public
	WorkBenchDefinition
			( final String name, final String path,
				final String display, final String desc )
		{
		this.leaf = false;
		this.name = name;
		this.path = path;
		this.displayName = display;
		this.description = desc;
		this.localRoot = null;
		}

	/**
	 * Project
	 */

	public
	WorkBenchDefinition
			( final String name, final String path,
				final String display, final String desc, final String root )
		{
		this.leaf = true;
		this.name = name;
		this.path = path;
		this.displayName = display;
		this.description = desc;
		this.localRoot = root;
		}

	public boolean
	isFolder()
		{
		return ! this.leaf;
		}

	public String
	getName()
		{
		return this.name;
		}

	public String
	getPath()
		{
		return this.path;
		}

	public String
	getFullPath()
		{
			return this.path != null && !this.path.isEmpty() ? this.path + '.' + this.name : this.name;
		}

	public String
	getDisplayName()
		{
		return this.displayName;
		}

	public String
	getDescription()
		{
		return this.description;
		}

	public String
	getLocalDirectory()
		{
		return this.localRoot;
		}

	}

