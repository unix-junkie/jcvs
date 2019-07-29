
package com.ice.jcvsii;

import java.io.IOException;

import javax.activation.CommandObject;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.swing.JOptionPane;

import com.ice.jni.dde.DDEException;
import com.ice.jni.dde.JNIDDE;


public
class		Win32Viewer
extends		Object
implements	CommandObject
	{
    private final boolean			debug = false;


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
	@Override
	public void
	setCommandContext( final String verb, final DataHandler dh )
		throws IOException
		{
		this.viewContent( verb, dh );
		}

	/**
	 * sets the current message to be displayed in the viewer
	 */
	public void
	viewContent( final String verb, final DataHandler dh )
		{
		final DataSource ds = dh.getDataSource();

		if ( ! (ds instanceof FileDataSource) )
			{
			return;
			}

		final FileDataSource fds = (FileDataSource) ds;

		try {
			// We instantiate a JNIDDE just to check the dll
			final JNIDDE dde = new JNIDDE();

			final String fileName = fds.getFile().getPath();

			final String execDir = System.getProperty( "user.dir", "" );

			if ( this.debug )
				System.err.println
					( "Win32Shell: Verb = '" + verb + "' Filename = '"
						+ fileName + "'" );

			JNIDDE.shellExecute
				( verb, fileName, null, execDir, JNIDDE.SW_SHOWNORMAL );
			}
		catch ( final UnsatisfiedLinkError er )
			{
			final String msg =
				"It appears that you have not installed the\n"
				+ "Windows DDE native library, 'ICE_JNIDDE.dll'.\n"
				+ "Consult the documentation about Win32 installation.\n"
				+ "\n"
				+ er.getMessage()
				+ "\n";

			JOptionPane.showMessageDialog
				( null, msg, "Error", JOptionPane.ERROR_MESSAGE );
			}
		catch ( final DDEException ex )
			{
			final String msg =
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

