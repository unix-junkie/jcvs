/*
** Java cvs client library package.
** Copyright (c) 1997-2002 by Timothy Gerard Endres
** 
** This program is free software.
** 
** You may redistribute it and/or modify it under the terms of the GNU
** Library General Public License (LGPL) as published by the Free Software
** Foundation.
**
** Version 2 of the license should be included with this distribution in
** the file LICENSE.txt, as well as License.html. If the license is not
** included	with this distribution, you may find a copy at the FSF web
** site at 'www.gnu.org' or 'www.fsf.org', or you may write to the Free
** Software Foundation at 59 Temple Place - Suite 330, Boston, MA 02111 USA.
**
** THIS SOFTWARE IS PROVIDED AS-IS WITHOUT WARRANTY OF ANY KIND,
** NOT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY. THE AUTHOR
** OF THIS SOFTWARE, ASSUMES _NO_ RESPONSIBILITY FOR ANY
** CONSEQUENCE RESULTING FROM THE USE, MODIFICATION, OR
** REDISTRIBUTION OF THIS SOFTWARE. 
** 
*/

package com.ice.cvsc;

/**
 * The CVSResponseHandler interface provides an interface
 * for CVSClient to call for inline processing of the server
 * responses. This is used when the CVSRequest specifies that
 * the response should <strong>not</strong> be <em>queued</em>.
 *
 * @version $Revision: 2.2 $
 * @author Timothy Gerard Endres, <a href="mailto:time@ice.com">time@ice.com</a>.
 * @see CVSProject
 * @see CVSClient
 *
 */

public interface
CVSResponseHandler
	{
	static public final String		RCS_ID = "$Id: CVSResponseHandler.java,v 2.2 2002/01/26 04:36:38 time Exp $";
	static public final String		RCS_REV = "$Revision: 2.2 $";

	abstract public boolean
		handleResponseItem(
			CVSRequest request, CVSResponse response, CVSResponseItem item );
	}
