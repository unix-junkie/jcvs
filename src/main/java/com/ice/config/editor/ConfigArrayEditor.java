
package com.ice.config.editor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;

import com.ice.config.ConfigureEditor;
import com.ice.config.ConfigureSpec;


public
abstract
class		ConfigArrayEditor
extends		ConfigureEditor
implements	FocusListener
	{
	private JButton		insertButton;
		JTable		table;
	SAETableModel	model;


	ConfigArrayEditor(final String typeName)
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
	public boolean
	isTupleTable( final ConfigureSpec spec )
		{
		return false;
		}

	// REVIEW Is this a reasonable assumption?
	@Override
	public boolean
	isStringArray( final ConfigureSpec spec )
		{
		return true;
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

		this.insertButton = new JButton( "Insert" );

		this.model = new SAETableModel();
		this.table = new JTable( this.model )
			{
			@Override
			public Component
			getNextFocusableComponent()
				{ return insertButton; }
			};

		this.table.setIntercellSpacing( new Dimension( 3, 3 ) );

		final Component scroller = new JScrollPane(this.table );

		result.add( "Center", scroller );

		final Container ctlPan = new JPanel();
		ctlPan.setLayout( new GridLayout( 1, 3, 5, 5 ) );

		result.add( "South", ctlPan );

		this.insertButton.addActionListener(e -> insertElement());
		ctlPan.add( this.insertButton );

			final AbstractButton appendButton = new JButton("Append");
		appendButton.addActionListener(e -> appendElement());
		ctlPan.add(appendButton);

			final AbstractButton deleteButton = new JButton("Delete");
		deleteButton.addActionListener(e -> deleteElement());
		ctlPan.add(deleteButton);

		this.descOffset = 5;

		return result;
		}

	private void
	insertElement()
		{
		final int row = this.table.getSelectedRow();
		this.model.insertElement( "New String", row );
		this.table.setRowSelectionInterval( row, row );
		this.table.repaint( 250 );
		}

	private void
	appendElement()
		{
		this.model.appendElement( "New String" );
		final int row = this.model.getRowCount() - 1;
		this.table.setRowSelectionInterval( row, row );
		this.table.repaint( 250 );
		}

	private void
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

	static
	class		SAETableModel
	extends		AbstractTableModel
		{
		private final String[]		columnNames =
			{
			"Value"
			};
		private final Class[]			columnTypes =
			{
			String.class
			};

		private List<String>			data;


		SAETableModel()
			{
			this.data = null;
			}

		List<String>
		getData()
			{
			return this.data;
			}

		void
		setData(final List<String> data)
			{
			this.data = data;

			this.fireTableChanged
				( new TableModelEvent( this ) );
			}

		void
		insertElement(final String val, final int row)
			{
			this.data.add(row, val);
			this.fireTableRowsInserted( row, row );
			}

		void
		appendElement(final String val)
			{
			this.data.add( val );
			this.fireTableRowsInserted
				( this.data.size(), this.data.size() );
			}

		void
		deleteElement(final int row)
			{
			this.data.remove( row );
			this.fireTableRowsDeleted( row, row );
			}

		//
		// I N T E R F A C E    TableModel
		//

		@Override
		public String
		getColumnName( final int column )
			{
			return columnNames[ column ];
			}

		@Override
		public Class
		getColumnClass( final int column )
			{
			return columnTypes[column];
			}

		@Override
		public int
		getColumnCount()
			{
			return columnNames.length;
			}

		@Override
		public int
		getRowCount()
			{
			if ( this.data == null )
				return 0;

			return data.size();
			}

		@Override
		public Object
		getValueAt( final int aRow, final int aColumn )
			{
			return this.data.get( aRow );
			}

		@Override
		public void
		setValueAt( final Object value, final int row, final int column )
			{
			this.data.set( row, (String) value );
			}

		@Override
		public boolean
		isCellEditable( final int row, final int column )
			{
			return true;
			}
		}
	}

