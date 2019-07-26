
package com.ice.config.editor;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.table.*;

import com.ice.pref.*;
import com.ice.config.*;


public
class		ConfigTupleTableEditor
extends		ConfigureEditor
implements	FocusListener
	{
	protected JButton			insertButton;
	protected JTable			table;
	protected TupleTableModel	model;


	public
	ConfigTupleTableEditor()
		{
		super( "Tuple Table" );
		}

	public
	ConfigTupleTableEditor( String typeName )
		{
		super( typeName );
		}

	public void
	requestInitialFocus()
		{
		}

	public void
	focusGained( FocusEvent event )
		{
		}

	public void
	focusLost( FocusEvent event )
		{
		}

	public void
	edit( UserPrefs prefs, ConfigureSpec spec )
		{
		super.edit( prefs, spec );

		PrefsTupleTable table =
			prefs.getTupleTable( spec.getPropertyName(), null );

		this.model.setData( table );

		this.table.sizeColumnsToFit( -1 );
		this.table.repaint( 100 );
		}

	public boolean
	isTupleTable( ConfigureSpec spec )
		{
		return true;
		}

	public boolean
	isStringArray( ConfigureSpec spec )
		{
		return false;
		}

	public void
	saveChanges( UserPrefs prefs, ConfigureSpec spec )
		{
		this.table.clearSelection();
		String propName = spec.getPropertyName();
		PrefsTupleTable table = this.model.getData();
		if ( table != null )
			{
			prefs.setTupleTable( propName, this.model.getData() );
			}
		}

	protected JPanel
	createEditPanel()
		{
		JPanel result = new JPanel();
		result.setLayout( new BorderLayout() );
		result.setBorder(
			new CompoundBorder(
				new EmptyBorder( 7, 7, 7, 7 ),
				new CompoundBorder(
					new EtchedBorder( EtchedBorder.RAISED ),
					new EmptyBorder( 2, 2, 2, 2 )
			) ) );

		result.setPreferredSize( new Dimension( 150, 225 ) );

		this.insertButton = new JButton( "Insert" );

		this.model = this.new TupleTableModel();
		this.table = new JTable( this.model );

		this.table.setIntercellSpacing( new Dimension( 1, 1 ) );

		JScrollPane scroller = new JScrollPane( this.table );

		result.add( "Center", scroller );

		JPanel ctlPan = new JPanel();
		ctlPan.setLayout( new GridLayout( 1, 3, 5, 5 ) );

		result.add( "South", ctlPan );

		this.insertButton.addActionListener(
			this.new ActionAdapter()
				{
				public void
				actionPerformed( ActionEvent e )
					{ insertElement(); }
				}
			);
		ctlPan.add( this.insertButton );

		JButton btn = new JButton( "Append" );
		btn.addActionListener(
			this.new ActionAdapter()
				{
				public void
				actionPerformed( ActionEvent e )
					{ appendElement(); }
				}
			);
		ctlPan.add( btn );

		btn = new JButton( "Delete" );
		btn.addActionListener(
			this.new ActionAdapter()
				{
				public void
				actionPerformed( ActionEvent e )
					{ deleteElement(); }
				}
			);
		ctlPan.add( btn );

		this.descOffset = 5;

		return result;
		}

	public void
	insertElement()
		{
		int row = this.table.getSelectedRow();
		this.model.insertElement( "New Key", row );
		this.table.setRowSelectionInterval( row, row );
		this.table.repaint( 250 );
		}

	public void
	appendElement()
		{
		this.model.appendElement( "New Key" );
		int row = this.model.getRowCount() - 1;
		this.table.setRowSelectionInterval( row, row );
		this.table.repaint( 250 );
		}

	public void
	deleteElement()
		{
		this.table.removeEditor();
		int row = this.table.getSelectedRow();
		if ( row >= 0 && row < this.model.getRowCount() )
			{
			this.model.deleteElement( row );
			if ( row >= this.model.getRowCount() )
				row = this.model.getRowCount() - 1;
			this.table.setRowSelectionInterval( row, row );
			this.table.repaint( 250 );
			}
		}

	public
	class		TupleTableModel
	extends		AbstractTableModel
		{
		private int					colCount = 0;

		private PrefsTupleTable		table = null;

		public
		TupleTableModel()
			{
			this.table = null;
			this.colCount = 0;
			}

		public
		TupleTableModel( PrefsTupleTable table )
			{
			this.table = table;
			this.colCount =
				( table == null ? 0
					: this.table.getMaximumTupleLength() );
			}

		public PrefsTupleTable
		getData()
			{
			return this.table;
			}

		public void
		setData( PrefsTupleTable table )
			{
			this.table = table;

			this.colCount =
				( table == null ? 0
					: this.table.getMaximumTupleLength() + 1 );

			this.fireTableStructureChanged();
			}

		public void
		insertElement( String key, int row )
			{
			String[] vals = new String[ this.colCount - 1 ];
			for ( int i = 0 ; i < this.colCount - 1 ; ++i ) vals[i] = "";
			PrefsTuple tup = new PrefsTuple( key, vals );
			this.insertElement( tup, row );
			}

		public void
		insertElement( PrefsTuple tup, int row )
			{
			this.table.insertTupleAt( tup, row );
			this.fireTableRowsInserted( row, row );
			}

		public void
		appendElement( String key )
			{
			String[] vals = new String[ this.colCount - 1 ];
			for ( int i = 0 ; i < this.colCount - 1 ; ++i ) vals[i] = "";
			PrefsTuple tup = new PrefsTuple( key, vals );
			this.appendElement( tup );
			}

		public void
		appendElement( PrefsTuple tup )
			{
			int sz = this.table.size();
			this.table.appendTuple( tup );
			this.fireTableRowsInserted( sz, sz );
			}

		public void
		deleteElement( int row )
			{
			this.table.removeTupleAt( row );
			this.fireTableRowsDeleted( row, row );
			}

		//
		// I N T E R F A C E    TableModel
		//

		public String
		getColumnName( int column )
			{
			return
				(column == 0
					? "Key"
					: "Value["+(column-1)+"]" );
			}
 
		public Class
		getColumnClass( int column )
			{
			return String.class;
			}

		public int
		getColumnCount()
			{
			return this.colCount; 
			}

		public int
		getRowCount()
			{
			if ( this.table == null )
				return 0;
			
			return table.size();
			}
 
		public Object
		getValueAt( int aRow, int aColumn )
			{
			if ( this.table == null )
				return "";

			PrefsTuple tuple = this.table.getTupleAt( aRow );
			if ( tuple != null )
				{
				if ( aColumn == 0 )
					{
					return tuple.getKey();
					}
				else if ( aColumn <= tuple.length() )
					{
					String val = tuple.getValues()[ aColumn - 1 ];
					return ( val == null ? "" : val );
					}
				else
					{
					return "";
					}
				}
			else
				{
				return "";
				}
			}

		public void
		setValueAt( Object value, int row, int column )
			{
			PrefsTuple tuple = this.table.getTupleAt( row );

			if ( column > 0 )
				{
				String[] vals = tuple.getValues();

				if ( (column - 1) >= vals.length )
					{
					String[] nvals = new String[ column ];
					System.arraycopy( vals, 0, nvals, 0, vals.length );
					for ( int j = vals.length ; j < column ; ++j )
						nvals[j] = "";
					vals = nvals;
					}

				vals[ column - 1 ] = (String)value;
				tuple.setValues( vals );
				}
			else if ( row < this.table.size() )
				{
				String[] vals = tuple.getValues();
				PrefsTuple newTup =
					new PrefsTuple( (String) value, vals );
				this.table.setTupleAt( newTup, row );
				}
			}

		public boolean
		isCellEditable( int row, int column )
			{
			return true;
			}
		}

	private
	class		ActionAdapter
	implements	ActionListener
		{
		public void
		actionPerformed( ActionEvent event )
			{
			}
		}

	}

