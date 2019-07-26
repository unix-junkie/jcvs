
//
// The following  variables are common to all of the
// GJT operations. It is loaded by all of the gjt-
// example scripts.
//

// ============================================================
// MODIFY THESE PLEASE
// ============================================================
//
// The following two variables must be modified to match your
// environment. The tempDir is where temp files are created,
// and localRootDir is the directory into which the working
// directory used in the examples will be checked out. The
// defaults settings are for windows platforms.

// The local temp dir for working files.
var tempDir = "C:/Windows/TEMP";

// The local directory into which the checkout will occur.
var localRootDir = "C:/gjt-test";

// ============================================================
// END OF MODIFY THESE PLEASE...
// ============================================================


// The login information (anonymous access to the Giant Java Tree)
var login = "anoncvs";
var passwd = "anoncvs";

// The cvs command to run
var cvsCommand = "co";

// The hostname of the cvs server.
var cvsHostname = "cvs.gjt.org";

// The cvs root directory on the server.
var cvsRootDir = "/gjt/cvsroot";

// The path of the module to checkout (the GJT util package)
var module = "java/org/gjt/util";

// SANITY CHECKS
var sane = true;

var f = new File( tempDir );
if ( ! f.exists() )
	{
	sane = false;
	System.err.println
		( "ERROR, temp dir '" + f.getPath() + "' does not exist." );
	System.err.println
		( "Please set 'tempDir' in 'gjt-common.js' "
			+ "to a valid directory." );
	System.err.println( "Look for the string 'MODIFY THESE PLEASE'." );
	}

var dirF = new File( localRootDir );
if ( ! dirF.exists() && ! dirF.mkdirs() )
	{
	sane = false;
	System.err.println
		( "ERROR, local root directory '" + dirF.getPath()
			+ "' can not be created." );
	System.err.println
		( "Please set 'localRootDir' in 'gjt-common.js' "
			+ "to a valid directory." );
	System.err.println( "Look for 'MODIFY THESE PLEASE'." );
	}

if ( ! sane )
	{
	System.err.println
		( "Not proceeding due to failed sanity checks." );
	System.exit(0);
	}

// We define our own UI interface that simply prints the output
// from the command to stderr.
var ui = new CVSUserInterface()
	{
	uiDisplayProgressMsg : function( message )
		{
		System.err.println( message );
		},

	uiDisplayProgramError : function( error )
		{
		System.err.println( error );
		},

	uiDisplayResponse : function( response )
		{
		System.err.println( response );
		}
	};

