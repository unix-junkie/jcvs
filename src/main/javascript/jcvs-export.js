
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

// Load the jCVS predefined cvs functions.
load( "export.js" );



var jCVSReleaseTag	= "release-5-2-1";
var localRootDir	= "C:/release/jcvsii-5.2.1";


var tempDir = "C:/Windows/TEMP";
var login = "time";
var passwd = "2#=32oz";
var cvsCommand = "co";
var cvsHostname = "cvs.trustice.com";
var cvsRootDir = "/usr/local/cvs";
var modulePrefix = "ice/java";
var jcvsModule = modulePrefix + "/com/ice/jcvsii";
var pkgFilename = "package.html";

var depends = new Hashtable();


// We define this function to wrap the inner function
// with exception handling.
function exportJCVSRelease()
	{
	try {
		var exportDirF = new File( localRootDir + "/" + jcvsModule );
		if ( exportDirF.exists() )
			{
			System.err.println( "Skipping com.ice.jcvsii export." );
			}
		else
			{
		//	exportJCVSPackage();
			jCVSExport
				( login, passwd, jcvsModule, jCVSReleaseTag,
					cvsHostname, cvsRootDir, localRootDir, tempDir, null );
			}

		parseJCVSPackageDoc();

		exportDependencies();
		}
	catch ( ex )
		{ ex.printStackTrace( System.err ); }
	}

function exportDependencies()
	{
	var pkgEnum = depends.keys();

	for ( ; pkgEnum.hasMoreElements() ; )	
		{
		var pkgName = pkgEnum.nextElement();
		var pkgTag = depends.get( pkgName );

		System.err.println
			( "Exporting '" + pkgName + "' release '" + pkgTag + "'" );

		var pkgModule =
			(new java.lang.String( pkgName )).replace( '.', '/' );

		pkgModule = modulePrefix + "/" + pkgModule;

		var exportDirF = new File( localRootDir + "/" + pkgModule );
		if ( exportDirF.exists() )
			{
			System.err.println
				( "   Skipped export of '" + exportDirF.getPath() + "'." );
			}
		else
			{
			jCVSExport
				( login, passwd, pkgModule, pkgTag,
					cvsHostname, cvsRootDir, localRootDir, tempDir, null );

			System.err.println
				( "   Exported '" + pkgName + "' release '" + pkgTag + "'" );
			}
		}

	}

//
// Parse the package.html file.
//
function parseJCVSPackageDoc()
	{
	var fileDirPath =
		new Packages.java.lang.String
			( localRootDir + "/" + jcvsModule );

	fileDirPath = fileDirPath.replace( '/', '\\' );

	var pkgDocFile = new File( fileDirPath, "package.html" );
	if ( ! pkgDocFile.exists() )
		{
		System.err.println( "ERROR could not read package.html file:" );
		System.err.println( "      " + pkgDocFile.getPath() );
		throw new IOException
			( "no package document file: " + pkgDocFile.getPath() );
		}

	System.err.println
		( "Reading package document file: " + pkgDocFile.getPath() );

	var rdr = new BufferedReader( new FileReader( pkgDocFile ) );

	for ( ; ; )
		{
		var ln = rdr.readLine();
		if ( ln == null )
			break;

		if ( ln.startsWith( "@gjtdep " ) )
			{
			ln = ln.substring( 8 );
			var idx = ln.indexOf( " " );
			var pkgName = ln.substring( 0, idx );
			var relTag = ln.substring( idx + 1 );
		//	System.err.println( "PKG " + pkgName + " TAG " + relTag );
			depends.put( pkgName, relTag );
			}
		}

	rdr.close();
	}


// Call the main function...
exportJCVSRelease();
