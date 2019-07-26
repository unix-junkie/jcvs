
package com.ice.jcvsii;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.beans.*;

import javax.activation.*;
import javax.swing.JOptionPane;

import com.ice.cvsc.CVSCUtilities;

import com.ice.jni.dde.*;

import com.ice.util.FileUtilities;
import com.ice.util.TempFileManager;


public
class		Win32Viewer
extends		Object
implements	CommandObject
	{
    private boolean			debug = false;


    public
	Win32Viewer()
		{
	//	this.debug =
	//		UserProperties.getProperty
	//			( "Win32ShellViewer.debug", false );
		}
    
	/**
	 * the CommandObject method to accept our DataHandler
	 * @param dh The datahandler used to get the content.
	 */
	public void
	setCommandContext( String verb, DataHandler dh )
		throws IOException
		{
		this.viewContent( verb, dh );
		}

	/**
	 * sets the current message to be displayed in the viewer
	 */
	public void
	viewContent( String verb, DataHandler dh )
		{
		DataSource ds = dh.getDataSource();

		if ( ! (ds instanceof FileDataSource) )
			{
			return;
			}

		FileDataSource fds = (FileDataSource) ds;

		try {
			// We instantiate a JNIDDE just to check the dll
			JNIDDE dde = new JNIDDE();

			String fileName = fds.getFile().getPath();

			String execDir = System.getProperty( "user.dir", "" );

			if ( this.debug )
				System.err.println
					( "Win32Shell: Verb = '" + verb + "' Filename = '"
						+ fileName + "'" );

			JNIDDE.shellExecute
				( verb, fileName, null, execDir, JNIDDE.SW_SHOWNORMAL );
			}
		catch ( UnsatisfiedLinkError er )
			{
			String msg =
				"It appears that you have not installed the\n"
				+ "Windows DDE native library, 'ICE_JNIDDE.dll'.\n"
				+ "Consult the documentation about Win32 installation.\n"
				+ "\n"
				+ er.getMessage()
				+ "\n";

			JOptionPane.showMessageDialog
				( null, msg, "Error", JOptionPane.ERROR_MESSAGE );
			}
		catch ( DDEException ex )
			{
			String msg =
				"Win32Viewer had trouble with the DDE communications.\n"
				+ "The most likely reason is that you have not defined an\n"
				+ "action for the verb '" + verb + "'.\n"
				+ "Consult the documentation about Win32 installation.\n"
				+ "\n"
				+ ex.getMessage()
				+ "\n";

			JOptionPane.showMessageDialog
				( null, msg, "Error", JOptionPane.ERROR_MESSAGE );
			}
		}

	}

