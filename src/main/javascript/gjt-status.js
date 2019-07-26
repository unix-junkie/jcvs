//
// THIS SCRIPT ASSUMES THAT YOU RAN THE SCRIPT 'gjt-co.js' FIRST.
//

// This script will open the working directory that we previous
// checked out with 'gjt-co.js', and then perform a cvs status
// on every file in the wd.
//
// You run these scripts by downloading the Rhino package from
// Netscape at:
//    http://www.mozilla.org/rhino/download.html
//
// Then invoke the JavaScript shell with a command like this:
//    java org.mozilla.javascript.tools.shell.Main gjt-status.js
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

function status_working_directory()
	{
	try { doStatusWD() }
	catch ( ex )
		{ ex.printStackTrace( System.err ); }
	}


// Do the deed.

function doStatusWD()
	{
	// Do we want to debug the protocol?
	var debugProtocol = false;

	// This is the request spec (see CVSRequestSpec.html).
	var cvsStatusSpec = "status:a:EUAF:de:-v";

	// Entries is used for the CVSEntry objects that we wish to send
	// to the server for this command. For checkouts, since there are
	// no entries checked out yet, this vector remains empty.
	var entries = new CVSEntryVector();

	// Arguments contains the options and arguments that are passed
	// to the cvs command. For this checkout, we will only provide
	// the name of the module to checkout, which we add below.
	var arguments = new CVSArgumentVector();

	// We need to create a CVSClient for the project to use.
	// We do not need to set anything, project will manage it.
	var client = new CVSClient();

	// We also need a new CVSProject. CVSProject is the object
	// that handles the processing of what occurred during the
	// protocol on CVSClient, as well as managing the local
	// working directory and admin files.
	var project = new CVSProject( client );

	// The login information.
	// project.setUserName( login );
	project.setPassword
		( CVSScramble.scramblePassword( passwd, 'A' ) );

	// Note that when we open the project locally, we get most
	// of the "parameters" from that open via the localRootFile
	// that we pass in and the CVS/ admin files, of which CVS/Root
	// tells us the user, host, root directory, and combined
	// with CVS/Repository gives us the cvs repository directory.

	var localRootF = new File( localRootDir );
	project.openProject( localRootF );

	System.err.println( "Project has been opened." );

	System.err.println( "Performing status command..." );
	var request = new CVSRequest();
	var parsedOk = request.parseControlString( cvsStatusSpec );

	if ( parsedOk )
		{
		// children is a com.ice.cvsc.CVSEntry
		var rootEntry = project.getRootEntry();

		System.err.println( "ROOT: " + rootEntry );

		// Add all of the File CVSEntry's in our project.
		addCVSFileEntries( entries, rootEntry );

		// Set the UI.
		request.setUserInterface( ui );

		// We ignore arguments, since they were set in the spec.

		// Here we set the CVSEntries that are to go up to the server.
		// Our spec says to send the entries as the arguments to the
		// command, so you do not have to add them onto the arguments.
		request.setEntries( entries );

		// These flags control tracing of the protocol for debugging
		request.traceRequest = debugProtocol;
		request.traceResponse = debugProtocol;
		request.traceTCPData = debugProtocol;
		request.traceProcessing = debugProtocol;

		var response = new CVSResponse();
		var reqOk = project.performCVSRequest( request, response );
		if ( reqOk )
			{
			var respOk = project.processCVSResponse( request, response );
			if ( respOk )
				{
				System.err.println( "SUCCESS." );
				}
			else
				{
				System.err.println
					( "FAILED to process cvs response." );
				}
			}
		else
			{
			System.err.println
				( "FAILED to perform cvs request." );
			}
		}
	else
		{
		System.err.println
			( "FAILED to parse request spec: " + cvsStatusSpec );
		}
	}

// Recurse over the CVSEntry tree and add all of the file
// entries to our entries vector for this command.

function addCVSFileEntries( entries, parent )
	{
	// children is a com.ice.cvsc.CVSEntryVector
	var children = parent.getEntryList();
	var chEnum = children.elements();
	for ( ; chEnum.hasMoreElements() ; )
		{
		var child = chEnum.nextElement();
		if ( child.isDirectory() )
			{
			addCVSFileEntries( entries, child );
			}
		else
			{
			// Add this file CVSEntry.
			entries.addElement( child );
			System.err.println( "CVSEntry: " + child.getFullName() );
			}
		}
	}

// Call the main function...
status_working_directory();
