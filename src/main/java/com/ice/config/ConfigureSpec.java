
package com.ice.config;

public
class		ConfigureSpec
implements	ConfigureConstants, java.io.Serializable
	{

	private final String		key;
	private final String		type;
	private final String		path;
	private final String		propName;
	private final String		description;
	private final String		help;

	private final String[]	choices;


	public
	ConfigureSpec
			( final String key, final String type, final String path,
				final String name, final String desc, final String help,
				final String[] choices )
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
		final int index = this.path.lastIndexOf( "." );
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

	@Override
	public String
	toString()
		{
		return "[" + this.type + "," + this.propName
			+ "," + this.description + "]";
		}

	}

