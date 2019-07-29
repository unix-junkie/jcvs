
package com.ice.config;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import com.ice.pref.PrefsTuple;
import com.ice.pref.PrefsTupleTable;
import com.ice.pref.UserPrefs;
import com.ice.pref.UserPrefsConstants;
import com.ice.pref.UserPrefsFileLoader;
import com.ice.pref.UserPrefsLoader;


public
class		ConfigureTest
extends		JPanel
implements	ActionListener
	{
	protected JButton			ok, cancel;
	protected ConfigurePanel	configure;
	protected UserPrefs			prefs;
	protected UserPrefs			specs;


	public
	ConfigureTest()
		throws IOException
		{
		File f;

		setLayout(new BorderLayout());

		final String dir = System.getProperty( "user.dir" );

		final UserPrefsFileLoader loader = (UserPrefsFileLoader)
			UserPrefsLoader.getLoader( UserPrefsConstants.FILE_LOADER );

		this.prefs = new UserPrefs( "ConfigTest", null );
		this.prefs.setPropertyPrefix( "com.ice.config" );

		f = new File( dir, "testprops.txt" );
		if ( f.exists() && f.isFile() && f.canRead() )
			{
			System.err.println
				( "Loading properties file '" + f.getPath() + "'" );
			loader.setFile( f );
			loader.loadPreferences( prefs );
			}
		else
			{
			System.err.println( "Loading default properties." );
			this.setDefaultProperties( prefs );
			}

		this.specs = new UserPrefs( "ConfigSpecs", null );
		this.specs.setPropertyPrefix( "" );

		f = new File( dir, "testspecs.txt" );
		if ( f.exists() && f.isFile() && f.canRead() )
			{
			System.err.println
				( "Loading specifications file '" + f.getPath() + "'" );
			loader.setFile( f );
			loader.loadPreferences( specs );
			}
		else
			{
			System.err.println( "Loading default specifications." );
			this.setDefaultSpecifications( specs );
			}

		//
		// P R O P E R T I E S
		//
		//
		// P R O P E R T Y    S P E C I F I C A T I O N S
		//

		this.configure = new ConfigurePanel( prefs, specs );

		this.add("Center", configure);

		final JPanel buttons = new JPanel();
		buttons.setLayout( new GridLayout( 1, 2 ) );
		buttons.add( buttonPanel( ok = new JButton( "OK" ) ) );
		buttons.add( buttonPanel( cancel = new JButton( "Cancel" ) ) );
		ok.addActionListener( this );
		cancel.addActionListener( this );

		final JPanel butPan = new JPanel();
		butPan.setLayout( new BorderLayout() );
		butPan.add( "East", buttons );

		final JPanel south = new JPanel();
		south.setLayout( new BorderLayout() );
		south.add( "North", new JSeparator( SwingConstants.HORIZONTAL ) );
		south.add( "Center", butPan );

		this.add( "South", south );
		}

	private void
	setDefaultProperties( final UserPrefs prefs )
		{
		prefs.setProperty
			( "contactDialog.title", "Contact Information" );
		prefs.setProperty
			( "contactDialog.nodesc", "No Description" );
		prefs.setBoolean
			( "contactDialog.visible", true );
		prefs.setPoint
			( "contactDialog.origin",
				new Point( 120, 120 ) );
		prefs.setDimension
			( "contactDialog.size",
				new Dimension( 120, 120 ) );
		prefs.setBounds
			( "contactDialog.bounds",
				new Rectangle( 5, 5, 120, 120 ) );
		prefs.setDouble
			( "contactDialog.quantity", 714.431 );

		prefs.setColor
			( "ui.color", new Color( 255, 178, 64 ) );
		prefs.setFont
			( "ui.font", new Font( "San-Serif", Font.BOLD, 18 ) );

		prefs.setFloat
			( "numbers.float", 1990.0725F );
		prefs.setDouble
			( "numbers.double", 1992.0618 );
		prefs.setLong
			( "numbers.long", 71404021961L );
		prefs.setInteger
			( "numbers.integer", 19870814 );

		prefs.setProperty
			( "misc.choice", "Choice Three" );

		String[] sa = new String[5];
		sa[0] = "This is string one.";
		sa[1] = "This is string two.";
		sa[2] = "This is string three. This string is going to be very very very long to see what happens.";
		sa[3] = "This is string four.";
		sa[4] = "This is string five.";

		prefs.setStringArray( "misc.people", sa );

		final String[] ta = new String[3];
		ta[0] = "token1";
		ta[1] = "token2";
		ta[2] = "token3";
		prefs.setTokens( "misc.tokens", ta );

		final PrefsTuple tup;
		final PrefsTupleTable tupT = new PrefsTupleTable();
		sa = new String[3];
		sa[0] = "1";
		sa[1] = "4";
		sa[2] = "Urgent Clinical";
		tupT.putTuple( new PrefsTuple( "1-Urgent Clinical", sa ) );

		sa = new String[3];
		sa[0] = "2";
		sa[1] = "4";
		sa[2] = "Urgent Member";
		tupT.putTuple( new PrefsTuple( "2-Urgent Member", sa ) );

		sa = new String[3];
		sa[0] = "5";
		sa[1] = "24";
		sa[2] = "Non Urgent/Routine";
		tupT.putTuple( new PrefsTuple( "5-Non Urgent/Routine", sa ) );

		sa = new String[3];
		sa[0] = "6";
		sa[1] = "720";
		sa[2] = "Upon Return";
		tupT.putTuple( new PrefsTuple( "6-Upon Return", sa ) );

		prefs.setTupleTable( "misc.priorities", tupT );
		}

	private void
	setDefaultSpecifications( final UserPrefs specs )
		{
		specs.setProperty
			( "spec.client.misc.choice",
				"choice:misc.choice:A Choice property." );
		specs.setProperty
			( "choice.misc.choice.1", "Choice One" );
		specs.setProperty
			( "choice.misc.choice.2", "Choice Two" );
		specs.setProperty
			( "choice.misc.choice.3", "Choice Three" );

		specs.setProperty
			( "spec.client.misc.tokens",
				"tokens:misc.tokens:A Tokens property." );

		specs.setProperty
			( "spec.client.misc.people",
				"stringarray:misc.people:A String Array property." );

		specs.setProperty
			( "spec.client.misc.priorities",
				"tupletable:misc.priorities:"
				+ "A Tuple Table property of priorities." );

		specs.setProperty
			( "spec.client.ui.color",
				"color:ui.color:A Color property." );

		specs.setProperty
			( "spec.client.ui.font",
				"font:ui.font:A Font property." );

		specs.setProperty
			( "spec.client.numbers.float",
				"float:numbers.float:A Float property." );

		specs.setProperty
			( "spec.client.numbers.double",
				"double:numbers.double:A Double property." );

		specs.setProperty
			( "spec.client.numbers.long",
				"long:numbers.long:A Long property." );

		specs.setProperty
			( "spec.client.numbers.integer",
				"integer:numbers.integer:An Integer property." );

		specs.setProperty
			( "spec.client.contactDialog.quantity",
				"double:contactDialog.quantity:"
				+ "The quantity." );

		specs.setProperty
			( "spec.client.contactDialog.origin",
				"point:contactDialog.origin:"
				+ "The topleft corner of the dialog when displayed." );

		specs.setProperty
			( "spec.client.contactDialog.size",
				"dimension:contactDialog.size:"
				+ "The width and height of the dialog "
				+ "when it is displayed and layed out." );

		specs.setProperty
			( "spec.client.contactDialog.title",
				"string:contactDialog.title:"
				+ "The title of the Dialog window." );

		specs.setProperty
			( "spec.client.contactDialog.nodesc",
				"string:contactDialog.nodesc:" );

		specs.setProperty
			( "spec.client.contactDialog.bounds",
				"rectangle:contactDialog.bounds:"
				+ "The bounds of the Dialog window." );

		specs.setProperty
			( "spec.client.contactDialog.visible",
				"boolean:contactDialog.visible:"
				+ "Whether or not the dialog box is visible." );
		}

	private JPanel
	buttonPanel( final JButton button )
		{
		final JPanel panel = new JPanel();
		panel.setBorder(new EmptyBorder(4, 4, 4, 4));
		panel.setLayout(new BorderLayout());
		panel.add("Center", button);
		return panel;
		}

	public void
	actionPerformed( final ActionEvent event )
		{
		boolean doExit = false;
		final Object source = event.getSource();

		if ( source == ok )
			{
			this.saveProperties();
			doExit = true;
			}
		else if ( source == cancel )
			{
			doExit = true;
			}

		if ( doExit )
			{
			setVisible( false );
			System.exit( 0 );
			}
		}

	public void
	saveProperties()
		{
		this.configure.saveCurrentEdit();

		this.configure.commit();

		String dir = System.getProperty( "user.dir" );

		final FileDialog dlg =
			new FileDialog
				( (Frame)this.getTopLevelAncestor(),
					"Save Properties To", FileDialog.SAVE );

		dlg.setFile( "saveprops.txt" );
		dlg.setDirectory( dir );

		dlg.show();

		final String file = dlg.getFile();
		dir = dlg.getDirectory();

		if ( file != null && dir != null )
			{
			final UserPrefsFileLoader loader = (UserPrefsFileLoader)
				UserPrefsLoader.getLoader( UserPrefsConstants.FILE_LOADER );

			final File f = new File( dir, file );

			loader.setFile( f );

			try {
				loader.storePreferences( this.prefs );
				}
			catch ( final IOException ex )
				{
				ex.printStackTrace();
				}
			}
		}

	public static void
	main( final String[] args )
		throws IOException
		{
		final JFrame frame = new JFrame("Configure Test");
		frame.setBounds(100, 100, 500, 440);
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add("Center", new ConfigureTest());
		frame.show();
		}

	}

