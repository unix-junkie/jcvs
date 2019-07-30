
package com.ice.config.editor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.table.AbstractTableModel;

import com.ice.config.ConfigureEditor;
import com.ice.config.ConfigureSpec;
import com.ice.pref.PrefsTuple;
import com.ice.pref.PrefsTupleTable;
import com.ice.pref.UserPrefs;


public
class		ConfigTupleTableEditor
extends		ConfigureEditor
implements	FocusListener
	{
		protected JTable			table;
	protected TupleTableModel	model;


	public
	ConfigTupleTableEditor()
		{
		super( "Tuple Table" );
		}

	protected ConfigTupleTableEditor(final String typeName)
		{
		super( typeName );
		}

	@Override
	public void
	requestInitialFocus()
		{
		}

	@Override
	public void
	focusGained( final FocusEvent event )
		{
		}

	@Override
	public void
	focusLost( final FocusEvent event )
		{
		}

	@Override
	public void
	edit( final UserPrefs prefs, final ConfigureSpec spec )
		{
		super.edit( prefs, spec );

		final PrefsTupleTable table =
			prefs.getTupleTable( spec.getPropertyName(), null );

		this.model.setData( table );

		this.table.sizeColumnsToFit( -1 );
		this.table.repaint( 100 );
		}

	@Override
	public boolean
	isTupleTable( final ConfigureSpec spec )
		{
		return true;
		}

	@Override
	public boolean
	isStringArray( final ConfigureSpec spec )
		{
		return false;
		}

	@Override
	public void
	saveChanges( final UserPrefs prefs, final ConfigureSpec spec )
		{
		this.table.clearSelection();
		final String propName = spec.getPropertyName();
		final PrefsTupleTable table = this.model.getData();
		if ( table != null )
			{
			prefs.setTupleTable( propName, this.model.getData() );
			}
		}

	@Override
	protected JPanel
	createEditPanel()
		{
		final JPanel result = new JPanel();
		result.setLayout( new BorderLayout() );
		result.setBorder(
			new CompoundBorder(
				new EmptyBorder( 7, 7, 7, 7 ),
				new CompoundBorder(
					new EtchedBorder( EtchedBorder.RAISED ),
					new EmptyBorder( 2, 2, 2, 2 )
			) ) );

		result.setPreferredSize( new Dimension( 150, 225 ) );

			final AbstractButton insertButton = new JButton("Insert");

		this.model = new TupleTableModel();
		this.table = new JTable( this.model );

		this.table.setIntercellSpacing( new Dimension( 1, 1 ) );

		final Component scroller = new JScrollPane(this.table );

		result.add( "Center", scroller );

		final Container ctlPan = new JPanel();
		ctlPan.setLayout( new GridLayout( 1, 3, 5, 5 ) );

		result.add( "South", ctlPan );

		insertButton.addActionListener(e -> insertElement());
		ctlPan.add(insertButton);

		JButton btn = new JButton( "Append" );
		btn.addActionListener(e -> appendElement());
		ctlPan.add( btn );

		btn = new JButton( "Delete" );
		btn.addActionListener(e -> deleteElement());
		ctlPan.add( btn );

		this.descOffset = 5;

		return result;
		}

	protected void
	insertElement()
		{
		final int row = this.table.getSelectedRow();
		this.model.insertElement( "New Key", row );
		this.table.setRowSelectionInterval( row, row );
		this.table.repaint( 250 );
		}

	protected void
	appendElement()
		{
		this.model.appendElement( "New Key" );
		final int row = this.model.getRowCount() - 1;
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

	public static
	class		TupleTableModel
	extends		AbstractTableModel
		{
		private int					colCount;

		private PrefsTupleTable		table;

		TupleTableModel()
			{
			this.table = null;
			this.colCount = 0;
			}

		public
		TupleTableModel( final PrefsTupleTable table )
			{
			this.table = table;
			this.colCount =
				table == null ? 0
					: this.table.getMaximumTupleLength();
			}

		public PrefsTupleTable
		getData()
			{
			return this.table;
			}

		public void
		setData( final PrefsTupleTable table )
			{
			this.table = table;

			this.colCount =
				table == null ? 0
					: this.table.getMaximumTupleLength() + 1;

			this.fireTableStructureChanged();
			}

		void
		insertElement(final String key, final int row)
			{
			final String[] vals = new String[ this.colCount - 1 ];
			for ( int i = 0 ; i < this.colCount - 1 ; ++i ) vals[i] = "";
			final PrefsTuple tup = new PrefsTuple( key, vals );
			this.insertElement( tup, row );
			}

		public void
		insertElement( final PrefsTuple tup, final int row )
			{
			this.table.insertTupleAt( tup, row );
			this.fireTableRowsInserted( row, row );
			}

		void
		appendElement(final String key)
			{
			final String[] vals = new String[ this.colCount - 1 ];
			for ( int i = 0 ; i < this.colCount - 1 ; ++i ) vals[i] = "";
			final PrefsTuple tup = new PrefsTuple( key, vals );
			this.appendElement( tup );
			}

		public void
		appendElement( final PrefsTuple tup )
			{
			final int sz = this.table.size();
			this.table.appendTuple( tup );
			this.fireTableRowsInserted( sz, sz );
			}

		void
		deleteElement(final int row)
			{
			this.table.removeTupleAt( row );
			this.fireTableRowsDeleted( row, row );
			}

		//
		// I N T E R F A C E    TableModel
		//

		@Override
		public String
		getColumnName( final int column )
			{
			return
				column == 0
				? "Key"
				: "Value[" + (column-1) + ']';
			}

		@Override
		public Class
		getColumnClass( final int column )
			{
			return String.class;
			}

		@Override
		public int
		getColumnCount()
			{
			return this.colCount;
			}

		@Override
		public int
		getRowCount()
			{
			if ( this.table == null )
				return 0;

			return table.size();
			}

		@Override
		public Object
		getValueAt( final int aRow, final int aColumn )
			{
			if ( this.table == null )
				return "";

			final PrefsTuple tuple = this.table.getTupleAt( aRow );
			if ( tuple != null )
				{
				if ( aColumn == 0 )
					{
					return tuple.getKey();
					}
				else if ( aColumn <= tuple.length() )
					{
					final String val = tuple.getValues()[ aColumn - 1 ];
					return val == null ? "" : val;
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

		@Override
		public void
		setValueAt( final Object value, final int row, final int column )
			{
			final PrefsTuple tuple = this.table.getTupleAt( row );

			if ( column > 0 )
				{
				String[] vals = tuple.getValues();

				if ( column - 1 >= vals.length )
					{
					final String[] nvals = new String[ column ];
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
				final String[] vals = tuple.getValues();
				final PrefsTuple newTup =
					new PrefsTuple( (String) value, vals );
				this.table.setTupleAt( newTup, row );
				}
			}

		@Override
		public boolean
		isCellEditable( final int row, final int column )
			{
			return true;
			}
		}
	}

