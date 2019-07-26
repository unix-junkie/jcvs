
// This script will checkout the org.gjt.util package from the
// Giant Java Tree anonymous CVS server. This will give us a
// simple repository to perform various tests with, which will
// serve as examples of jCVS JavaScripting.
//
// You run these scripts by downloading the Rhino package from
// Netscape at:
//    http://www.mozilla.org/rhino/download.html
//
// Then invoke the JavaScript shell with a command like this:
//    java org.mozilla.javascript.tools.shell.Main gjt-co.js
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
function checkout()
	{
	try { doCheckout() }
	catch ( ex )
		{ ex.printStackTrace( System.err ); }
	}

// Do the deed.
function doCheckout()
	{
	// Do we want to debug the protocol?
	var debugProtocol = false;

	// Entries is used for the CVSEntry objects that we wish to send
	// to the server for this command. For checkouts, since there are
	// no entries checked out yet, this vector remains empty.
	var entries = new CVSEntryVector();

	// Arguments contains the options and arguments that are passed
	// to the cvs command. For this checkout, we will only provide
	// the name of the module to checkout, which we add below.
	var arguments = new CVSArgumentVector();

	// Append our one argument, the module to be checked out.
	arguments.appendArgument( module );

	// We need to make sure that the local checkout directory
	// exists before we run the command.
	var coDir = new File( localRootDir );
	if ( ! coDir.exists() )
		{
		if ( ! coDir.mkdirs() )
			{
			throw new IOException
				( "could not create export directory '"
					+ coDir.getPath() + "'." );
			}
		}

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

	// Now we proceed to set a plethora of CVSProject settings...

	// The login information.
	project.setUserName( login );
	project.setPassword
		( CVSScramble.scramblePassword( passwd, 'A' ) );

	// The local temp dir
	project.setTempDirectory( tempDir );
	// The name of the module being managed.
	project.setRepository( module );

	// The root directory of the cvs repository on the server.
	project.setRootDirectory( cvsRootDir );
	// The local parent of the working directory. This is where
	// the checked out working directory will exist
	project.setLocalRootDirectory( localRootDir );
	// We are using PServer mode.
	project.setPServer( true );
	// The Connection method of INETD (TCP).
	project.setConnectionMethod( CVSRequest.METHOD_INETD );
	// GZIP File mode is not preferred.
	project.setAllowsGzipFileMode( false );

	// We must make sure that the Project has its root entry, as
	// CVSProject will not be able to create it from the context
	// that the server will send with the checkout. We use the
	// following method to create one.
	project.establishRootEntry( cvsRootDir );

	// Now we create the CVSRequest that will define the command
	// that we wish to run. This CVSRequest is ultimately handed
	// to our CVSClient for processing, which involves performing
	// the request in cvs client/server protocol.
	request = new CVSRequest();

	// The reason for these redundant sets is simple. Typically,
	// jCVS code hands a request to the project for processing,
	// and CVSProject fills in the following fields from the ones
	// we set above. However, since we are handing the request
	// directly to a CVSClient, and bypassing the CVSProject, we
	// need to set these here. Note how we just pull the values
	// out of CVSProject...

	request.setPServer( true );
	request.setUserName( project.getUserName() );
	request.setPassword( project.getPassword() );

	request.setConnectionMethod( project.getConnectionMethod() );
	request.setServerCommand( project.getServerCommand() );
	request.setRshProcess( project.getRshProcess() );

	request.setPort( CVSClient.DEFAULT_CVS_PORT );
	request.setHostName( cvsHostname );

	request.setRepository( module );
	request.setLocalDirectory( project.getLocalRootDirectory() );
	request.setRootDirectory( project.getRootDirectory() );
	request.setRootRepository( project.getRootDirectory() );

	request.setSetVariables( project.getSetVariables() );

	request.setCommand( cvsCommand );

	// Finally, set the necessary flags for this request.
	// Refer to CVSRequestSpec.html for more details.

	// Do we add the module onto the end of the arguments?
	// No, we add it to the arguments in this code.
	request.sendModule = false;
	// Do we send up the arguments vector? yes.
	request.sendArguments = true;
	// Do we send up the CVSEntries vector? For co, no.
	request.sendEntries = false;
	// Do we handle updated commands?
	request.handleUpdated = true;
	// Can CVSProject overwrite an existing file? Set to
	// false to be very cautious about local files, but
	// only in very rare circumstance.
	request.allowOverWrites = true;
	// We do *not* want to queue up the entire response, since
	// it may be enormous, thus we set this to false, and make
	// sure the responseHandler is set.
	request.queueResponse = false;
	// Set the object responsible for processing the response.
	// See note above about response processing.
	request.responseHandler = project;
	// We do not care about Notify's in checkout
	request.includeNotifies = false;
	// This is important, since it tells the project to manage
	// the CVSEntries, and to save the CVS/* admin files once
	// the command completes. If your CVS/ directories are not
	// properly written, you are missing the setting.
	request.handleEntries = true;

	// These flags control tracing of the protocol for debugging
	request.traceRequest = debugProtocol;
	request.traceResponse = debugProtocol;
	request.traceTCPData = debugProtocol;
	request.traceProcessing = debugProtocol;

	request.allowGzipFileMode =
		project.allowsGzipFileMode();

	// Here we set the CVSEntries that are to go up to the server.
	request.setEntries( entries );

	// Here we set the CVSArguments to go up to the server.
	request.appendArguments( arguments );

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

	// Set the UI.
	request.setUserInterface( ui );

	// Setup a log file and redirect output to it.
	// This is for debugging purposes, and requires
	// that you turn on the tracing flags above...
	var redir = null;
	if ( debugProtocol )
		{
		redir =
			new PrintWriter
				( new FileWriter
					( localRootDir + File.separator + "co-log.txt" ) );

		request.setRedirectWriter( redir );
		}

	// Ask CVSClient to perform the cvs command's protocol, and retrieve
	// the response from the server...
	response = client.processCVSRequest( request );

	// Now ask the CVSProject to process the response. This writes the
	// working directory files from temp, and saves the admin files.
	project.processCVSResponse( request, response );

	// We need to call cleanup of the temp files.
	if ( response != null
			&& ! request.saveTempFiles )
		{
		response.deleteTempFiles();
		}

	// If it failed, spit out a stack trace...
	if ( response.getStatus() != CVSResponse.OK )
		{
		throw new IOException
			( "checkout of '" + module + "' failed: "
				+ response.getDisplayResults() );
		}

	// Close the redirect if there was one...
	if ( redir != null )
		{
		redir.close();
		}
	}

// Call the function...
checkout();
