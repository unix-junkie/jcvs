
package com.ice.util;

final class
ClassUtilities
	{
		private ClassUtilities() {
		}

		public static boolean
	implementsInterface( final Class aClass, final String interfaceName )
		{
		int ii;

		final Class[] interfaces = aClass.getInterfaces();

		for ( ii = 0 ; ii < interfaces.length ; ++ii )
			{
			if ( interfaceName.equals( interfaces[ ii ].getName() ) )
				break;
			}

		return ii < interfaces.length;
		}

	}

