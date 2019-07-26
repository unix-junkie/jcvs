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

/**
 * The CVSUserInterface interface provide the link between
 * a CVSProject and the user interface that is presenting
 * that project. This interface is used by the CVSProject,
 * and its client, to make requests to the user interface
 * to display progress, results, and the like.
 *
 * @version $Revision: 2.1 $
 * @author Timothy Gerard Endres, <a href="mailto:time@ice.com">time@ice.com</a>.
 * @see CVSProject
 * @see CVSClient
 * @see com.ice.jcvs.CVSProjectFrame
 *
 */

public interface
CVSUserInterface
	{
	static public final String		RCS_ID = "$Id: CVSUserInterface.java,v 2.1 1997/04/19 05:12:14 time Exp $";
	static public final String		RCS_REV = "$Revision: 2.1 $";

	abstract public void
		uiDisplayProgressMsg( String message );

	abstract public void
		uiDisplayProgramError( String error );

	abstract public void
		uiDisplayResponse( CVSResponse response );
	}
