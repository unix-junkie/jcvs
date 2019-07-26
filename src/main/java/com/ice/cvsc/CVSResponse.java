/*
** Java cvs client library package.
** Copyright (c) 1997 by Timothy Gerard Endres
** 
** This program is free software.
** 
** You may redistribute it and/or modify it under the terms of the GNU
** General Public License as published by the Free Software Foundation.
** Version 2 of the license should be included with this distribution in
** the file LICENSE, as well as License.html. If the license is not
** included	with this distribution, you may find a copy at the FSF web
** site at 'www.gnu.org' or 'www.fsf.org', or you may write to the
** Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139 USA.
**
** THIS SOFTWARE IS PROVIDED AS-IS WITHOUT WARRANTY OF ANY KIND,
** NOT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY. THE AUTHOR
** OF THIS SOFTWARE, ASSUMES _NO_ RESPONSIBILITY FOR ANY
** CONSEQUENCE RESULTING FROM THE USE, MODIFICATION, OR
** REDISTRIBUTION OF THIS SOFTWARE. 
** 
*/

package com.ice.cvsc;
																			
import java.io.*;
import java.lang.*;
import java.text.*;
import java.util.*;

/**
 * The CVSResponse class encapsulates a CVS server's response to
 * a request. The response will contain a list of the server's
 * response lines, as well as all downloaded files (which are
 * stored in temporary files). Once you are finished with a
 * CVSResponse <strong>it is important</strong> to call	the
 * <em>deleteTempFile()</em> method of the reponse object,
 * or temporary files will go undeleted and populate the local
 * temp directory.
 *
 * @version $Revision: 2.2 $
 * @author Timothy Gerard Endres, <a href="mailto:time@ice.com">time@ice.com</a>.
 * @see CVSClient
 * @see CVSRequest
 */

public class
CVSResponse extends Object
	{
	static public final String		RCS_ID = "$Id: CVSResponse.java,v 2.2 1999/05/18 07:13:20 time Exp $";
	static public final String		RCS_REV = "$Revision: 2.2 $";

	static public final int			OK		= 0;
	static public final int			ERROR	= 1;


	private boolean			valid;

	private int				status;

	private String			errorCode;
	private String			errorText;

	private StringBuffer	stdErrStr;
	private StringBuffer	stdOutStr;

	private CVSRespItemVector	itemList;


	public CVSResponse()
		{
		super();

		this.valid = true;

		this.itemList = new CVSRespItemVector();

		this.errorCode = "";
		this.errorText = "";

		this.stdErrStr = new StringBuffer( 4096 );
		this.stdOutStr = new StringBuffer( 32 );
		}

	public void
	appendStdOut( String text )
		{
		this.stdOutStr.append( text );
		}

	public void
	appendStdErr( String text )
		{
		this.stdErrStr.append( text );
		}

	public void
	addResponseItem( CVSResponseItem item )
		{
		this.itemList.appendItem( item );
		}

	public CVSRespItemVector
	getItemList()
		{
		return this.itemList;
		}

	public boolean
	isValid()
		{
		return this.valid;
		}

	public void
	setValid( boolean valid )
		{
		this.valid = valid;													
		}

	public int
	getStatus()
		{
		return this.status;
		}

	public void
	setStatus( int status )
		{
		this.status = status;													

		this.errorCode = "";
		this.errorText = "";
		}

	public void
	setErrorStatus( String codeStr, String textStr )
		{
		this.status = CVSResponse.ERROR;

		this.errorCode = codeStr;
		this.errorText = textStr;
		}

	public String
	getErrorCode()
		{
		return this.errorCode;
		}

	public String
	getErrorText()
		{
		return this.errorText;
		}

	public String
	getStderr()
		{
		return this.stdErrStr.toString();
		}

	public String
	getStdout()
		{
		return this.stdOutStr.toString();
		}

	public void
	appendStderr( String msg )
		{
		this.stdErrStr.append( msg );
		}

	public void
	appendStdout( String msg )
		{
		this.stdOutStr.append( msg );
		}

	public int
	itemTypeCount( int type )
		{
		int				count = 0;
		CVSResponseItem item;

		for ( int i = 0 ; i < this.itemList.size() ; ++i )
			{
			item = this.itemList.itemAt( i );
			if ( item.getType() == type )
				count++;
			}

		return count;
		}

	public CVSResponseItem
	getFirstItemByType( int type )
		{
		CVSResponseItem item;

		for ( int i = 0 ; i < this.itemList.size() ; ++i )
			{
			item = this.itemList.itemAt( i );
			if ( item.getType() == type )
				return item;
			}

		return null;
		}

	public CVSResponseItem
	getNextItemByType( int type, CVSResponseItem lastItem )
		{
		int				i;
		CVSResponseItem item;

		for ( i = 0 ; i < this.itemList.size() ; ++i )
			{
			item = this.itemList.itemAt( i );
			if ( item == lastItem )
				{
				++i;
				break;
				}
			}

		for ( ; i < this.itemList.size() ; ++i )
			{
			item = this.itemList.itemAt( i );
			if ( item.getType() == type )
				return item;
			}

		return null;
		}

	public void
	printResponse( PrintStream out )
		{
		out.println( "=============================================================" );
		
		out.println( "RESPONSE has " + this.itemList.size() + " items:" );
		if ( this.itemList.size() > 0 )
			{
			this.itemList.printResponseItemList( out, "   " );
			}
		
		out.println( "\n" + this.getStderr() + "\n" + this.getStdout() );
		
		out.println( "=============================================================" );
		}

	//
	// For now we just clean up the temporary files...
	//
	public boolean
	deleteTempFiles()
		{
		boolean	err;
		boolean result = true;

		for ( int i = 0 ; i < this.itemList.size() ; ++i ) 
			{
			CVSResponseItem item = this.itemList.itemAt( i );

			err = item.deleteFile();

			if ( ! err )
				result = false;
			}

		return result;
		}

	public String
	getDisplayResults()
		{
		StringBuffer finalResult =
						new StringBuffer("");

		String stdout = this.getStdout();
		String stderr = this.getStderr();

		if ( stderr.length() > 0 || stdout.length() > 0 )
			{
			if ( stderr.length() > 0 )
				{
				finalResult.append( stderr );
				if ( stdout.length() > 0 )
					finalResult.append( "\n" );
				}

			if ( stdout.length() > 0 )
				{
				finalResult.append( stdout );
				}
			}

		if ( this.getStatus() == CVSResponse.OK )
			{
			finalResult.append
				( "\n** The command completed successfully." );
			}
		else
			{
			finalResult.append
				( "\n** The command completed with an error status." );
			}

		return finalResult.toString();
		}

	public String
	toString()
		{
		if ( this.valid )
			{
			return "CVSResponse: "
				+ this.itemList.size() + " items.\n"
				+ this.stdErrStr + "\n"
				+ this.stdOutStr;
			}
		else
			{
			return "CVSResponse: not valid";
			}
		}
	}
	   
