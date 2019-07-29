
package com.ice.config.editor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.List;

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
	protected JButton		insertButton;
	protected JButton		appendButton;
	protected JButton		deleteButton;
	protected JTable		table;
	protected SAETableModel	model;


	public
	ConfigArrayEditor( final String typeName )
		{
		super( typeName );
		}

	@Override
	public void
	requestInitialFocus()
		{
		}

	public void
	focusGained( final FocusEvent event )
		{
		}

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

		this.model = this.new SAETableModel();
		this.table = new JTable( this.model )
			{
			@Override
			public Component
			getNextFocusableComponent()
				{ return insertButton; }
			};

		this.table.setIntercellSpacing( new Dimension( 3, 3 ) );

		final JScrollPane scroller = new JScrollPane( this.table );

		result.add( "Center", scroller );

		final JPanel ctlPan = new JPanel();
		ctlPan.setLayout( new GridLayout( 1, 3, 5, 5 ) );

		result.add( "South", ctlPan );

		this.insertButton.addActionListener(
			this.new ActionAdapter()
				{
				@Override
				public void
				actionPerformed( final ActionEvent e )
					{ insertElement(); }
				}
			);
		ctlPan.add( this.insertButton );

		this.appendButton = new JButton( "Append" );
		this.appendButton.addActionListener(
			this.new ActionAdapter()
				{
				@Override
				public void
				actionPerformed( final ActionEvent e )
					{ appendElement(); }
				}
			);
		ctlPan.add( this.appendButton );

		this.deleteButton = new JButton( "Delete" );
		this.deleteButton.addActionListener(
			this.new ActionAdapter()
				{
				@Override
				public void
				actionPerformed( final ActionEvent e )
					{ deleteElement(); }
				}
			);
		ctlPan.add( this.deleteButton );

		this.descOffset = 5;

		return result;
		}

	public void
	insertElement()
		{
		final int row = this.table.getSelectedRow();
		this.model.insertElement( "New String", row );
		this.table.setRowSelectionInterval( row, row );
		this.table.repaint( 250 );
		}

	public void
	appendElement()
		{
		this.model.appendElement( "New String" );
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

	public
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


		public
		SAETableModel()
			{
			this.data = null;
			}

		public List<String>
		getData()
			{
			return this.data;
			}

		public void
		setData( final List<String> data )
			{
			this.data = data;

			this.fireTableChanged
				( new TableModelEvent( this ) );
			}

		public void
		insertElement( final String val, final int row )
			{
			this.data.add(row, val);
			this.fireTableRowsInserted( row, row );
			}

		public void
		appendElement( final String val )
			{
			this.data.add( val );
			this.fireTableRowsInserted
				( this.data.size(), this.data.size() );
			}

		public void
		deleteElement( final int row )
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

		public int
		getColumnCount()
			{
			return columnNames.length;
			}

		public int
		getRowCount()
			{
			if ( this.data == null )
				return 0;

			return data.size();
			}

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

	private
	class		ActionAdapter
	implements	ActionListener
		{
		public void
		actionPerformed( final ActionEvent event )
			{
			}
		}

	}

