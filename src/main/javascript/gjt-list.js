//
// THIS SCRIPT ASSUMES THAT YOU RAN THE SCRIPT 'gjt-co.js' FIRST.
//

// This script will open the working directory that we previous
// checked out with 'gjt-co.js', and then print out the contents
// of that working directory.
//
// You run these scripts by downloading the Rhino package from
// Netscape at:
//    http://www.mozilla.org/rhino/download.html
//
// Then invoke the JavaScript shell with a command like this:
//    java org.mozilla.javascript.tools.shell.Main gjt-list.js
//
// You need to invoke the script in the script's directory, or
// it will be unable to locate the script files that are loaded
// using the load() function.
//

// Load some commonly used Java package definition variables.
load( "java-pkgdefs.js" );

// Load the jCVS package definition variables.
load( "jcvs-pkgdefs.js" );

// Load the GJT common variables (login, repos, cvshost, etc).
load( "gjt-common.js" );



// We define this function to wrap the inner function
// with exception handling.
function list_working_directory()
	{
	try { doListWD() }
	catch ( ex )
		{ ex.printStackTrace( System.err ); }
	}

// Do the deed.
function doListWD()
	{
	// Do we want to debug the protocol?
	var debugProtocol = false;

	// The hostname of the cvs server.
	var cvsHostname = "cvs.gjt.org";

	// The cvs root directory on the server.
	var cvsRootDir = "/gjt/cvsroot";

	// The local directory into which the checkout will occur.
	var localRootDir = "C:/gjt-test";

	// The path of the module to checkout (the GJT util package)
	var module = "java/org/gjt/util";


	// We need to create a CVSClient that will perform the
	// cvs protocol communications for us. We pass the cvs
	// server hostname and the default pserver port (2401).
	var client =
		new CVSClient
			( cvsHostname, CVSClient.DEFAULT_CVS_PORT );

	// We also need a new CVSProject. CVSProject is the object
	// that handles the processing of what occurred during the
	// protocol on CVSClient, as well as managing the local
	// working directory and admin files.
	var project = new CVSProject( client );

	var localRootF = new File( localRootDir );
	project.openProject( localRootF );

	System.err.println( "Project has been opened." );

	// children is a com.ice.cvsc.CVSEntry
	var rootEntry = project.getRootEntry();

	System.err.println( "R " + rootEntry.getFullName() );

	listChildEntries( "___", rootEntry );
	}

function listChildEntries( prefix, parent )
	{
	// children is a com.ice.cvsc.CVSEntryVector
	var children = parent.getEntryList();
	var chEnum = children.elements();
	for ( ; chEnum.hasMoreElements() ; )
		{
		var child = chEnum.nextElement();
		var isDir = child.isDirectory();
		System.err.print( isDir ? "D" : "F" );
		System.err.print( prefix );
		System.err.println( child.getFullName() );
		if ( isDir )
			{
			var newPfx = prefix + "___";
			listChildEntries( newPfx, child );
			}
		}
	}

// Call the function...
list_working_directory();
