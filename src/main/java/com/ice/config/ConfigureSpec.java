
package com.ice.config;

import javax.swing.*;


public
class		ConfigureSpec
implements	ConfigureConstants, java.io.Serializable
	{

	private String		key;
	private String		type;
	private String		path;
	private String		propName;
	private String		description;
	private String		help;

	private String[]	choices;


	public
	ConfigureSpec
			( String key, String type, String path,
				String name, String desc, String help,
				String[] choices )
		{
		this.key = key;
		this.type = type;
		this.path = path;
		this.propName = name;
		this.description = desc;
		this.help = help;
		this.choices = choices;
		}

	public String
	getKey()
		{
		return this.key;
		}

	public String
	getName()
		{
		int index = this.path.lastIndexOf( "." );
		if ( index == -1 )
			return this.path;
		else
			return this.path.substring( index + 1 );
		}

	public String
	getPropertyPath()
		{
		return this.path;
		}

	public String
	getPropertyType()
		{
		return this.type;
		}

	public String
	getPropertyName()
		{
		return this.propName;
		}

	public String
	getDescription()
		{
		return this.description;
		}

	public String
	getHelp()
		{
		return this.help;
		}

	public String[]
	getChoices()
		{
		return this.choices;
		}

	public boolean
	isStringArray()
		{
		return this.type.equals( CFG_STRINGARRAY );
		}

	public boolean
	isTupleTable()
		{
		return this.type.equals( CFG_TUPLETABLE );
		}

	public String
	toString()
		{
		return "[" + this.type + "," + this.propName
			+ "," + this.description + "]";
		}

	}

