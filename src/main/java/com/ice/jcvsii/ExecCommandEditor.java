
package com.ice.jcvsii;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import com.ice.config.ConfigureEditor;
import com.ice.config.ConfigureSpec;
import com.ice.pref.PrefsTuple;
import com.ice.pref.PrefsTupleTable;
import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;

//	addEditor( String type, ConfigureEditor editor )

public
class		ExecCommandEditor
extends		ConfigureEditor
implements	ActionListener, ItemListener
	{
	protected PrefsTupleTable	cmdTable;

	protected JTextField		cmdText;
	protected JTextField		envText;

	protected JComboBox			cmdBox;


	public
	ExecCommandEditor()
		{
		super( "Exec Commands" );
		this.descOffset = 10;
		}

	@Override
	public void
	edit( final UserPrefs prefs, final ConfigureSpec spec )
		{
		super.edit( prefs, spec );

		this.cmdTable = Config.getInstance().getExecCmdDefinitions();

		this.cmdBox.setModel
			( new DefaultComboBoxModel
				( this.cmdTable.getKeyOrder() ) );

		this.cmdBox.setSelectedItem( null );
		this.cmdBox.repaint( 50 );
		this.validate();
		}

	@Override
	public void
	saveChanges( final UserPrefs prefs, final ConfigureSpec spec )
		{
		this.saveCurrentCommand
			( (String) this.cmdBox.getSelectedItem() );
		final String propName = spec.getPropertyName();
		prefs.setTupleTable( propName, this.cmdTable );
		}

	// REVIEW I'll bet we can think of a way to move this up a level...
	@Override
	public void
	commitChanges( final ConfigureSpec spec, final UserPrefs prefs, final UserPrefs orig )
		{
		final String propName = spec.getPropertyName();

		final PrefsTupleTable table =
			prefs.getTupleTable( propName, null );

		orig.removeTupleTable( propName );

		if ( table != null )
			{
			orig.setTupleTable( propName, table );
			}

		Config.getInstance().loadExecCmdDefinitions();
		}

	@Override
	public boolean
	isModified( final ConfigureSpec spec, final UserPrefs prefs, final UserPrefs orig )
		{
		final String propName = spec.getPropertyName();

		final PrefsTupleTable nt =
			prefs.getTupleTable( propName, null );

		final PrefsTupleTable ot =
			orig.getTupleTable( propName, null );

		if ( nt != null && ot != null )
			{
			if ( ! nt.equals( ot ) )
				{
				return true;
				}
			}
		else if ( nt != null || ot != null )
			{
			return true;
			}

		return false;
		}

	@Override
	public void
	requestInitialFocus()
		{
		this.cmdBox.requestFocus();
		}

	public void
	saveCurrentCommand( final String extVerb )
		{
		if ( extVerb != null )
			{
			final String cmd = this.cmdText.getText();
			final String env = this.envText.getText();

			final String[] vals = new String[2];
			vals[ ConfigConstants.EXEC_DEF_ENV_IDX ] = env;
			vals[ ConfigConstants.EXEC_DEF_CMD_IDX ] = cmd;

			final PrefsTuple tup = new PrefsTuple( extVerb, vals );

			this.cmdTable.putTuple( tup );
			}
		}

	public void
	newCommand()
		{
		String extVerb = null;

		for ( ; ; )
			{
			extVerb =
				JOptionPane.showInputDialog
					( "Enter key: .ext.verb (e.g. .java.edit)" );

			if ( extVerb == null )
				break;

			if ( extVerb.indexOf( "." ) == -1 )
				{
				JOptionPane.showMessageDialog
					( null, "The key '" + extVerb + "' is not valid.\n" +
							"The format is '.ext.verb'.\n",
						"Invalid Key", JOptionPane.WARNING_MESSAGE );
				continue;
				}

			final String[] tupVals = { "", "" };
			final PrefsTuple newTuple = new PrefsTuple( extVerb, tupVals );

			boolean append = true;
			for ( int i = 0, sz = this.cmdTable.size() ; i < sz ; ++i )
				{
				final PrefsTuple tup = this.cmdTable.getTupleAt(i);
				if ( extVerb.compareTo( tup.getKey() ) < 0 )
					{
					append = false;
					this.cmdTable.insertTupleAt( newTuple, i );
					break;
					}
				}

			if ( append )
				this.cmdTable.appendTuple( newTuple );

			this.cmdBox.setModel
				( new DefaultComboBoxModel
					( this.cmdTable.getKeyOrder() ) );

			this.cmdBox.setSelectedItem( extVerb );
			this.cmdBox.repaint( 500 );
			break;
			}
		}

	public void
	deleteCommand()
		{
		final String extVerb =
			(String) this.cmdBox.getSelectedItem();

		if ( extVerb != null )
			{
			final PrefsTuple tup = this.cmdTable.getTuple( extVerb );

			if ( tup != null )
				{
				this.cmdTable.removeTuple( tup );
				this.cmdBox.removeItem( extVerb );
				}
			}
		}

	public void
	actionPerformed( final ActionEvent event )
		{
		final String command = event.getActionCommand();

		if ( command.equals( "NEW" ) )
			{
			this.newCommand();
			}
		else if ( command.equals( "DEL" ) )
			{
			this.deleteCommand();
			}
		}

	public void
	itemStateChanged( final ItemEvent evt )
		{
		final int stateChg = evt.getStateChange();

		if ( stateChg == ItemEvent.SELECTED )
			{
			final String key = (String) evt.getItem();
			final PrefsTuple tup = this.cmdTable.getTuple( key );
			if ( tup != null )
				{
				this.cmdText.setText
					( tup.getValueAt( ConfigConstants.EXEC_DEF_CMD_IDX ) );
				this.envText.setText
					( tup.getValueAt( ConfigConstants.EXEC_DEF_ENV_IDX ) );
				}
			else
				{
				// UNDONE report this to the user?
				this.cmdText.setText( "" );
				this.envText.setText( "" );
				}
			}
		else if ( stateChg == ItemEvent.DESELECTED )
			{
			this.saveCurrentCommand( (String) evt.getItem() );
			this.cmdText.setText( "" );
			this.envText.setText( "" );
			}
		}

	@Override
	protected JPanel
	createEditPanel()
		{
		JLabel lbl;

		final JPanel result = new JPanel();
		result.setLayout( new GridBagLayout() );
		result.setBorder( new EmptyBorder( 5, 3, 3, 3 ) );

		final int cols = 3;
		int row = 0;

		JButton btn = new JButton( "New..." );
		btn.addActionListener( this );
		btn.setActionCommand( "NEW" );
		AWTUtilities.constrain(
			result, btn,
			GridBagConstraints.NONE,
			GridBagConstraints.WEST,
			0, row, 1, 1, 0.0, 0.0,
			new Insets( 0, 7, 0, 10 ) );

		this.cmdBox = new JComboBox();
		this.cmdBox.addItemListener( this );
		AWTUtilities.constrain(
			result, this.cmdBox,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.CENTER,
			1, row, 1, 1, 1.0, 0.0 );

		btn = new JButton( "Delete" );
		btn.addActionListener( this );
		btn.setActionCommand( "DEL" );
		AWTUtilities.constrain(
			result, btn,
			GridBagConstraints.NONE,
			GridBagConstraints.EAST,
			2, row++, 1, 1, 0.0, 0.0,
			new Insets( 0, 10, 0, 7 ) );

		lbl = new JLabel( "Command:" );
		AWTUtilities.constrain(
			result, lbl,
			GridBagConstraints.NONE,
			GridBagConstraints.WEST,
			0, row++, cols, 1, 0.0, 0.0,
			new Insets( 10, 0, 1, 0 ) );

		this.cmdText = new JTextField();
		AWTUtilities.constrain(
			result, this.cmdText,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			0, row++, cols, 1, 1.0, 0.0 );

		lbl = new JLabel( "Environment:" );
		lbl.setBorder( new EmptyBorder( 1, 3, 1, 3 ) );
		AWTUtilities.constrain(
			result, lbl,
			GridBagConstraints.NONE,
			GridBagConstraints.WEST,
			0, row++, cols, 1, 0.0, 0.0,
			new Insets( 10, 0, 1, 0 ) );

		this.envText = new JTextField();
		AWTUtilities.constrain(
			result, this.envText,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			0, row++, cols, 1, 1.0, 0.0 );

		return result;
		}

	}

