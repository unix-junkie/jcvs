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
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.border.TitledBorder;

import com.ice.cvsc.CVSIgnore;


/**
 * Dialog that displays the list of unknown files in the project and
 * lets user choose which ones to delete.
 *
 * @author Sherali Karimov <sher@mailandnews.com>
 */

public
class		UnknownFilesDialog
extends		JDialog
	{
	JList			lstFiles = new JList();
	MyListModel		mdlList = new MyListModel();

	JPanel			pnlControl = new JPanel();
	JButton			btnCancel = new JButton();
	JButton			btnDelete = new JButton();
	JButton			btnSelectAll = new JButton();
	BorderLayout	borderLayout1 = new BorderLayout();
	GridBagLayout	gridBagLayout1 = new GridBagLayout();
	JScrollPane		scrlList = new JScrollPane();
	TitledBorder	titledBorder1;

	Vector			selectedList = new Vector();
	JButton			btnAdd = new JButton();

	boolean			cancelled = false;
	boolean			deleted = false;


	public
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

	public void
	setData( final Vector data )
		{
		this.selectedList.clear();
		this.mdlList.setData( data );
		}

	private void
	jbInit( final boolean dirs )
		throws Exception
		{
		this.titledBorder1 = new TitledBorder
			( BorderFactory.createEtchedBorder
				( Color.white,new Color(148, 145, 140) ), "Unknown Files" );

		this.getContentPane().setLayout( this.borderLayout1 );
		this.btnCancel.setText("Cancel");
		this.btnCancel.addActionListener(
			new java.awt.event.ActionListener()
				{
				public void
				actionPerformed( final ActionEvent e )
					{
					btnCancelActionPerformed(e);
					}
				}
			);

		if ( ! dirs )
			{
			this.btnDelete.setFont( new java.awt.Font( "Dialog", 1, 12 ) );
			this.btnDelete.setForeground( Color.red );
			this.btnDelete.setText( "Delete" );
			this.btnDelete.addActionListener(
				new java.awt.event.ActionListener()
					{
					public void
					actionPerformed( final ActionEvent e )
						{
						btnDeleteActionPerformed(e);
						}
					}
				);
			}

		this.pnlControl.setLayout( this.gridBagLayout1 );
		this.pnlControl.setBorder( BorderFactory.createEtchedBorder() );
		this.btnSelectAll.setText( "Select All" );
		this.btnSelectAll.addActionListener(
			new java.awt.event.ActionListener()
				{
				public void
				actionPerformed( final ActionEvent e )
					{
						btnSelectAllActionPerformed(e);
					}
				}
			);

		this.scrlList.setBorder(titledBorder1);
		this.btnAdd.setFont(new java.awt.Font("Dialog", 1, 12));
		this.btnAdd.setForeground(Color.blue);
		this.btnAdd.setText("Add to project");
		this.btnAdd.addActionListener(
			new java.awt.event.ActionListener()
				{
				public void
				actionPerformed(final ActionEvent e)
					{
						btnAddActionPerformed(e);
					}
				}
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
		public void
		mouseReleased( final MouseEvent e )
			{
			mdlList.toggleItemAt
				( lstFiles.locationToIndex( e.getPoint() ) );
			}
		}

	private
	class		MyCellRenderer
	extends		JCheckBox
	implements	ListCellRenderer
		{
		Color selColor;

		public
		MyCellRenderer()
			{
			// UNDONE - configurable
			this.selColor = new Color( 208, 224, 240 );
			this.setOpaque( true );
			this.setBorder( null );
			}

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

	private
	class		MyListModel
	extends		AbstractListModel
		{
		Vector		listItems = new Vector();
		CVSIgnore	ignore = new CVSIgnore();

		public
		MyListModel()
			{
			this.ignore.addIgnoreSpec
				( Config.getPreferences().getProperty
					( ConfigConstants.GLOBAL_USER_IGNORES, null ) );
			}

		public void
		setData( final Vector ukns )
			{
			this.listItems.clear();
			if ( ukns != null && ukns.size() > 0 )
				{
				for( int i=0 ; i < ukns.size() ; i++ )
					{
					final Object obj = ukns.elementAt( i );
					if ( obj != null && obj instanceof File )
						{
						final File f = (File) obj;

						if ( this.ignore.isFileToBeIgnored( f.getName() ) )
							continue;

						listItems.add( new MyItem( f ) );
						}
					}
				}

			this.fireContentsChanged( this, 0, 1 );
			}

		public int
		getSize()
			{
			return this.listItems.size();
			}

		public void
		toggleItemAt( final int index )
			{
			if ( index > -1 && index < this.listItems.size() )
				{
				final MyItem item = (MyItem) this.listItems.elementAt( index );
				item.isSelected = ! item.isSelected;
				this.fireContentsChanged( this, index, index );
				}
			}

		public void
		selectAllItems()
			{
			final int size = this.listItems.size();
			for ( int i = 0 ; i < size ; i++ )
				{
				((MyItem) this.listItems.elementAt(i)).isSelected = true;
				}

			this.fireContentsChanged( this, 0, size-1 );
			}

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

		public void
		addElementAt( final Object obj, final int pos )
			{
			if ( obj != null && obj instanceof File )
				{
				this.listItems.add( pos, new MyItem((File) obj) );
				this.fireContentsChanged( this, pos, pos + 1 );
				}
			}

		public Object
		deleteElementAt( final int i )
			{
			if ( this.listItems.size() == 0 )
				return null;
			final Object ret = this.listItems.remove(i);
			this.fireContentsChanged( this, i, i + 1 );
			return ret;
			}

		public boolean
		containsElement( final Object obj )
			{
			final int i = this.listItems.indexOf(obj);
			if ( i >= 0 )
				return true;
			else
				return false;
			}

		public Object
		deleteElement( final Object obj )
			{
			if ( this.listItems.size() == 0 )
				return null;
			this.listItems.remove(obj);
			this.fireContentsChanged( this, 0, this.listItems.size() );
			return obj;
			}

		public Enumeration
		elements()
			{
			return this.listItems.elements();
			}
		}

	private
	class		MyItem
		{
		String		label = null;
		boolean		isSelected = false;
		File		file = null;

		public
		MyItem( final File newFile )
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

	public void
	dispose()
		{
		this.selectedList.clear();
		for ( final Enumeration enumeration = this.mdlList.elements()
				; enumeration.hasMoreElements() ; )
			{
			final MyItem item = (MyItem) enumeration.nextElement();
			if ( item.isSelected )
				this.selectedList.add( item.file );
			}

		super.dispose();
		}

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
		for ( int i = 0 ; i < array.length ; i++ )
			{
			array[i] = (File) this.selectedList.elementAt( i );
			}

		return array;
		}

	}
