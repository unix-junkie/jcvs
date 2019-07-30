
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
	public
	DDEException()
		{
		super();
		}

	public
	DDEException( final String msg )
		{
		super( msg );
		}
	}


