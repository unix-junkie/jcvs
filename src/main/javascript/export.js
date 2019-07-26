//
// This enccapsulates the "cvs export" command.
//
// You need Rhino to run these scripts:
//    http://www.mozilla.org/rhino/download.html
//
//


//
// Export the com.ice.jcvsii package sources.
//
function jCVSExport(
		login, passwd, cvsModule, relTag,
		cvsHostname, cvsRootPath, localRootPath,
		tempDirPath, logWriter
		)
	{
	var debugProtocol = ( logWriter != null );
	var entries = new CVSEntryVector();
	var arguments = new CVSArgumentVector();

	arguments.appendArgument( "-r" );
	arguments.appendArgument( relTag );
	arguments.appendArgument( cvsModule );

	var exportDirF = new File( localRootPath );
	if ( ! exportDirF.exists() )
		{
		if ( ! exportDirF.mkdirs() )
			{
			throw new IOException
				( "could not create export directory '"
					+ exportDirF.getPath() + "'." );
			}
		}

	var client =
		new CVSClient
			( cvsHostname, CVSClient.DEFAULT_CVS_PORT );

	var project = new CVSProject( client );

	// Now we proceed to set a plethora of CVSProject settings...

	project.setUserName( login );
	project.setPassword
		( CVSScramble.scramblePassword( passwd, 'A' ) );

	project.setTempDirectory( tempDirPath );
	project.setRepository( cvsModule );

	project.setRootDirectory( cvsRootPath );
	project.setLocalRootDirectory( localRootPath );
	project.setPServer( true );
	project.setConnectionMethod( CVSRequest.METHOD_INETD );
	project.setAllowsGzipFileMode( false );

	project.establishRootEntry( cvsRootPath );

	request = new CVSRequest();

	request.setPServer( true );
	request.setUserName( project.getUserName() );
	request.setPassword( project.getPassword() );

	request.setConnectionMethod( project.getConnectionMethod() );
	request.setServerCommand( project.getServerCommand() );
	request.setRshProcess( project.getRshProcess() );

	request.setPort( CVSClient.DEFAULT_CVS_PORT );
	request.setHostName( cvsHostname );

	request.setRepository( project.getRepository() );
	request.setLocalDirectory( project.getLocalRootDirectory() );
	request.setRootDirectory( project.getRootDirectory() );
	request.setRootRepository( project.getRootDirectory() );

	request.setSetVariables( project.getSetVariables() );

	request.setCommand( "export" );

	request.sendModule = false;
	request.sendArguments = true;
	request.sendEntries = false;
	request.handleUpdated = true;
	request.allowOverWrites = true;
	request.queueResponse = false;
	request.responseHandler = project;
	request.displayReponse = true;
	request.includeNotifies = false;
	request.handleEntries = false;
	request.traceRequest = debugProtocol;
	request.traceResponse = debugProtocol;
	request.traceTCPData = debugProtocol;
	request.traceProcessing = debugProtocol;

	request.allowGzipFileMode =
		project.allowsGzipFileMode();

	request.setEntries( entries );

	request.appendArguments( arguments );

	// Set the UI.
	var ui = new CVSUserInterface()
		{
		uiDisplayProgressMsg : function( message )
			{ System.err.println( "   " + message ); },

		uiDisplayProgramError : function( error )
			{ System.err.println( "   " + error ); },

		uiDisplayResponse : function( response )
			{
			System.err.println( response.getStdout() );
			System.err.println( response.getStderr() );
			}
		};

	request.setUserInterface( ui );

	if ( logWriter != null )
		{
		request.setRedirectWriter( logWriter );
		}

	var response = client.processCVSRequest( request );

	project.processCVSResponse( request, response );

	if ( response != null
			&& ! request.saveTempFiles )
		{
		response.deleteTempFiles();
		}

	if ( response.getStatus() != CVSResponse.OK )
		{
		throw new IOException
			( "checkout of '" + cvsModule + "' failed: "
				+ response.getDisplayResults() );
		}
	}
