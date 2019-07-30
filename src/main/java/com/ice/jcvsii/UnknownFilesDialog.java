/*
** Java cvs client application package.
** Copyright (c) 1997-2002 by Sherali Karimov, Timothy Gerard Endres
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

package com.ice.jcvsii;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import com.ice.cvsc.CVSIgnore;


/**
 * Dialog that displays the list of unknown files in the project and
 * lets user choose which ones to delete.
 *
 * @author Sherali Karimov <sher@mailandnews.com>
 */

class		UnknownFilesDialog
extends		JDialog
	{
	private final JList			lstFiles = new JList();
	private final MyListModel		mdlList = new MyListModel();

	private final JComponent pnlControl = new JPanel();
	private final AbstractButton btnCancel = new JButton();
	private final AbstractButton btnDelete = new JButton();
	private final AbstractButton btnSelectAll = new JButton();
	private final LayoutManager borderLayout1 = new BorderLayout();
	private final LayoutManager gridBagLayout1 = new GridBagLayout();
	private final JScrollPane		scrlList = new JScrollPane();

		private final Vector			selectedList = new Vector();
	private final AbstractButton btnAdd = new JButton();

	private boolean			cancelled;
	private boolean			deleted;


	UnknownFilesDialog( final Frame owner, final Vector vct, final String title, final boolean dirs )
		{
		super( owner, title, true );
		try {
			this.jbInit( dirs );
			this.setData( vct );
			super.setDefaultCloseOperation( super.DO_NOTHING_ON_CLOSE );
			}
		catch ( final Exception ex )
			{
			ex.printStackTrace();
			}
		}

	private void
	setData(final Vector data)
		{
		this.selectedList.clear();
		this.mdlList.setData( data );
		}

	private void
	jbInit( final boolean dirs )
		throws Exception
		{
			final Border titledBorder1 = new TitledBorder
					(BorderFactory.createEtchedBorder
							(Color.white, new Color(148, 145, 140)), "Unknown Files");

		this.getContentPane().setLayout( this.borderLayout1 );
		this.btnCancel.setText("Cancel");
		this.btnCancel.addActionListener(
			this::btnCancelActionPerformed
			);

		if ( ! dirs )
			{
			this.btnDelete.setFont( new Font( "Dialog", 1, 12 ) );
			this.btnDelete.setForeground( Color.red );
			this.btnDelete.setText( "Delete" );
			this.btnDelete.addActionListener(
				this::btnDeleteActionPerformed
				);
			}

		this.pnlControl.setLayout( this.gridBagLayout1 );
		this.pnlControl.setBorder( BorderFactory.createEtchedBorder() );
		this.btnSelectAll.setText( "Select All" );
		this.btnSelectAll.addActionListener(
			this::btnSelectAllActionPerformed
			);

		this.scrlList.setBorder(titledBorder1);
		this.btnAdd.setFont(new Font("Dialog", 1, 12));
		this.btnAdd.setForeground(Color.blue);
		this.btnAdd.setText("Add to project");
		this.btnAdd.addActionListener(
			this::btnAddActionPerformed
			);

		this.getContentPane().add(pnlControl,  BorderLayout.SOUTH);
		this.pnlControl.add(btnSelectAll,
			new GridBagConstraints(
				0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
				new Insets( 5, 5, 5, 0 ), 0, 0) );

		this.pnlControl.add(btnCancel,
			new GridBagConstraints(
				3, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
				new Insets( 5, 10, 5, 5 ), 0, 0 ) );

		if ( ! dirs )
			{
			this.pnlControl.add(btnDelete,
				new GridBagConstraints(
					2, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
					new Insets( 5, 10, 5, 10 ), 0, 0 ) );
			}

		this.pnlControl.add(btnAdd,
			new GridBagConstraints(
				1, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
				new Insets( 5, 10, 5, 0 ), 0, 0 ) );

		this.getContentPane().add( this.scrlList, BorderLayout.CENTER );
		scrlList.getViewport().add( this.lstFiles, null );
		lstFiles.setModel( this.mdlList );
		lstFiles.setCellRenderer( new MyCellRenderer() );
		lstFiles.addMouseListener( new MyEditEventListener() );
		}

	private
	class		MyEditEventListener
	extends		MouseAdapter
		{
		@Override
		public void
		mouseReleased( final MouseEvent e )
			{
			mdlList.toggleItemAt
				( lstFiles.locationToIndex( e.getPoint() ) );
			}
		}

	private final
	class		MyCellRenderer
	extends		JCheckBox
	implements	ListCellRenderer
		{
		final Color selColor;

		private MyCellRenderer()
			{
			// UNDONE - configurable
			this.selColor = new Color( 208, 224, 240 );
			this.setOpaque( true );
			this.setBorder( null );
			}

		@Override
		public Component
		getListCellRendererComponent(
				final JList list, final Object value, final int index,
				final boolean isSelected, final boolean cellHasFocus )
			{
			if ( value instanceof MyItem )
				{
				final boolean sel = ((MyItem) value).isSelected;
				this.setSelected( sel );
				this.setText( ((MyItem) value).label );
				this.setBackground( sel ? this.selColor : Color.white );
				this.setForeground( Color.black );
				return this;
				}

			return null;
			}
		}

//	private
//	class		MyCellEditor
//	extends		JCheckBox
//	implements	ListCellRenderer
//		{
//		public MyCellRenderer()
//			{
//			this.setOpaque(true);
//			}
//
//		public Component
//		getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
//			{
//			if ( value instanceof MyItem )
//				{
//				this.setText(((MyItem)value).label);
//				this.setSelected(((MyItem)value).isSelected);
//				return this;
//				}
//			return null;
//			}
//		}

	private final
	class		MyListModel
	extends		AbstractListModel
	implements Iterable<MyItem>
		{
		final Vector<MyItem>	listItems = new Vector<>();
		final CVSIgnore	ignore = new CVSIgnore();

		private MyListModel()
			{
			this.ignore.addIgnoreSpec
				( Config.getPreferences().getProperty
					( ConfigConstants.GLOBAL_USER_IGNORES, null ) );
			}

		void
		setData(final Vector ukns)
			{
			this.listItems.clear();
			if ( ukns != null && !ukns.isEmpty())
				{
				for( int i=0 ; i < ukns.size() ; i++ )
					{
					final Object obj = ukns.elementAt( i );
					if (obj instanceof File)
						{
						final File f = (File) obj;

						if ( this.ignore.isFileToBeIgnored( f.getName() ) )
							continue;

						listItems.add(new MyItem(f));
						}
					}
				}

			this.fireContentsChanged( this, 0, 1 );
			}

		@Override
		public int
		getSize()
			{
			return this.listItems.size();
			}

		void
		toggleItemAt(final int index)
			{
			if ( index > -1 && index < this.listItems.size() )
				{
				final MyItem item = this.listItems.elementAt( index );
				item.isSelected = ! item.isSelected;
				this.fireContentsChanged( this, index, index );
				}
			}

		void
		selectAllItems()
			{
			final int size = this.listItems.size();
			for ( int i = 0 ; i < size ; i++ )
				{
				this.listItems.elementAt(i).isSelected = true;
				}

			this.fireContentsChanged( this, 0, size-1 );
			}

		@Override
		public Object
		getElementAt( final int i )
			{
			final int size = getSize();
			if ( i < 0 || i >= size )
				{
				return null;
				}

			return this.listItems.elementAt(i);
			}

		public boolean
		addElement( final Object o )
			{
			this.addElementAt( o, this.listItems.size() );
			return true;
			}

		void
		addElementAt(final Object obj, final int pos)
			{
			if (obj instanceof File)
				{
				this.listItems.add(pos, new MyItem((File) obj));
				this.fireContentsChanged( this, pos, pos + 1 );
				}
			}

		public Object
		deleteElementAt( final int i )
			{
			if (this.listItems.isEmpty())
				return null;
			final Object ret = this.listItems.remove(i);
			this.fireContentsChanged( this, i, i + 1 );
			return ret;
			}

		public boolean
		containsElement( final Object obj )
			{
			final int i = this.listItems.indexOf(obj);
			return i >= 0;
			}

		public Object
		deleteElement( final Object obj )
			{
			if ( this.listItems.isEmpty() )
				return null;
			this.listItems.remove(obj);
			this.fireContentsChanged( this, 0, this.listItems.size() );
			return obj;
			}

		@Override
		public Iterator<MyItem>
		iterator()
			{
			return this.listItems.iterator();
			}
		}

	private static final
	class		MyItem
		{
		final String		label;
		boolean		isSelected;
		final File		file;

		private MyItem(final File newFile)
			{
			this.label = newFile.getAbsolutePath();
			this.file = newFile;
			}
		}

	private void
	btnCancelActionPerformed( final ActionEvent e )
		{
		this.cancelled = true;
		this.selectedList.clear();
		super.dispose();
		}

	private void
	btnSelectAllActionPerformed( final ActionEvent e )
		{
		this.mdlList.selectAllItems();
		}

	private void
	btnDeleteActionPerformed( final ActionEvent e )
		{
		this.deleted = true;
		dispose();
		}

	private void
	btnAddActionPerformed( final ActionEvent e )
		{
		this.deleted = false;
		dispose();
		}

	public boolean
	isCancelAction()
		{
		return cancelled;
		}

	public boolean
	isAddAction()
		{
		return ! deleted;
		}

	public boolean
	isDeleteAction()
		{
		return deleted;
		}

	@Override
	public void
	dispose()
		{
		this.selectedList.clear();
		for ( final MyItem item : this.mdlList )
			{
			if ( item.isSelected )
				this.selectedList.add( item.file );
			}

		super.dispose();
		}

	@Override
	public void
	show()
		{
		deleted = false;
		cancelled = false;

		super.pack();
		final Dimension s = this.getSize();
		final Dimension d = Toolkit.getDefaultToolkit().getScreenSize();

		if ( s.height > d.height )
			s.height = d.height;
		if ( s.width > d.width )
			s.width = d.width;

		this.setLocation
			( (d.width - s.width) / 2, (d.height - s.height) / 2 );

		super.show();
		}

	public File[]
	selectFiles()
		{
		show();

		// sort the results out after dispose has been called
		final File[] array = new File[ selectedList.size() ];
		Arrays.setAll(array, i -> (File) this.selectedList.elementAt(i));

		return array;
		}

	}
