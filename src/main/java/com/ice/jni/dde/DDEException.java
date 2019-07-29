
package com.ice.jni.dde;

/**
 * This exception is used to indicate general problems with
 * executing a native DDE command.
 *
 * @version $Revision: 1.2 $
 * @author Timothy Gerard Endres,
 *    <a href="mailto:time@ice.com">time@ice.com</a>.
 */

public class
DDEException extends Exception
	{
	static public final String	RCS_ID = "$Id: DDEException.java,v 1.2 1999/04/01 17:31:04 time Exp $";
	static public final String	RCS_REV = "$Revision: 1.2 $";
	static public final String	RCS_NAME = "$Name:  $";

	public
	DDEException()
		{
		super();
		}

	public
	DDEException( String msg )
		{
		super( msg );
		}
	}


