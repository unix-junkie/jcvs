
package com.ice.jni.dde;


/**
 * <p>
 * The JNIDDE class provides native access to Windows DDE.
 */

public final class
JNIDDE
	{
	/**
	 * Constants for use in the showCmd argument in shellExecute().
	 * These values come from WinUser.h or WinResrc.h
	 */
	public static final int		SW_HIDE = 0;
	public static final int		SW_SHOWNORMAL = 1;
	public static final int		SW_NORMAL = 1;
	public static final int		SW_SHOWMINIMIZED = 2;
	public static final int		SW_SHOWMAXIMIZED = 3;
	public static final int		SW_MAXIMIZE = 3;
	public static final int		SW_SHOWNOACTIVATE = 4;
	public static final int		SW_SHOW = 5;
	public static final int		SW_MINIMIZE = 6;
	public static final int		SW_SHOWMINNOACTIVE = 7;
	public static final int		SW_SHOWNA = 8;
	public static final int		SW_RESTORE = 9;
	public static final int		SW_SHOWDEFAULT = 10;

	static
		{
		System.loadLibrary( "ICE_JNIDDE" );
		}

	public static void
	main( final String... argv)
		{
		if ( argv.length < 3 )
			{
			System.err.println
				( "usage: service topic command" );
			return;
			}

		final boolean result;

		try {
			result =
				ddeExecute
					( argv[0], argv[1], argv[2], false );
			}
		catch ( final DDEException ex )
			{
			System.err.println
				( "JNIDDE.main: DDEException: " + ex.getMessage() );
			}
		}

	public static native boolean
		ddeExecute(
				String service, String topic, String command,
				boolean isAsync )
			throws DDEException;

	public static native void
		shellExecute(
				String operation, String fileName, String parameter,
				String defaultDir, int showHide )
			throws DDEException;
	}

