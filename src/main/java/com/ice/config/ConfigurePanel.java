
package com.ice.config;

import static com.ice.config.ConfigureConstants.CFG_DEFAULT;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.util.Properties;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import com.ice.pref.UserPrefs;

public
class		ConfigurePanel
extends		JPanel
implements	TreeSelectionListener
	{
	private final JTree			tree;
	private final JLabel		title;
	private final JPanel		editorPanel;
	private final JSplitPane	splitter;

		private Vector<ConfigureSpec>	specVector;

	private final UserPrefs		prefs;
	private final UserPrefs		origPrefs;

	private final ConfigureTreeModel	model;
	private ConfigureEditor		currEditor;
	private ConfigureTreeNode		currSelection;
	protected Properties			template = new Properties();

	private ConfigureEditorFactory	factory;


	public
	ConfigurePanel( final UserPrefs cfgPrefs, final UserPrefs specs )
		{
		this( cfgPrefs, specs,
				new DefaultConfigureEditorFactory( specs ) );
		}

	public
	ConfigurePanel
			( final UserPrefs cfgPrefs, final UserPrefs specs,
				final ConfigureEditorFactory factory )
		{
		this.origPrefs = cfgPrefs;

		this.prefs =
			cfgPrefs.createWorkingCopy
				( "Configuration Working Copy" );

			this.setLayout( new BorderLayout() );
		this.setBorder( new EmptyBorder( 5, 5, 5, 5 ) );

		this.model = new ConfigureTreeModel();

		try {
			this.specVector =
				ConfigureUtil.readConfigSpecification( specs );
			}
		catch ( final InvalidSpecificationException ex )
			{
			// REVIEW
			// UNDONE
			// Should we not throw this?
			ex.printStackTrace();
			this.specVector = new Vector();
			}

		this.establishConfigTree();

		this.tree = new ConfigureTree( model );
		this.tree.addTreeSelectionListener( this );

		final Component treeScroller = new JScrollPane(this.tree );

		final JPanel pan = new JPanel();
		pan.setLayout( new BorderLayout() );
		pan.setPreferredSize( new Dimension( 125, 225 ) );
		pan.add( BorderLayout.CENTER, treeScroller );

		this.editorPanel = new EditorPanel();
		this.editorPanel.setLayout( new BorderLayout() );
		this.editorPanel.setBorder( new EmptyBorder( 5, 5, 5, 5 ) );

		final JPanel contentPanel = new JPanel();
		contentPanel.setLayout( new BorderLayout() );

		this.title = new JLabel( "Properties", SwingConstants.LEFT );
		this.title.setPreferredSize( new Dimension( 30, 30 ) );
		this.title.setBackground( new Color( 224, 224, 255 ) );
		this.title.setForeground( Color.black );
		this.title.setOpaque( true );
		this.title.setFont
			( new Font( this.getFont().getName(), Font.BOLD, 14 ) );
		this.title.setBorder
			( new CompoundBorder
				( new LineBorder( Color.black ),
					new EmptyBorder( 5, 5, 5, 5 ) ) );

		this.splitter =
			new JSplitPane
				( JSplitPane.HORIZONTAL_SPLIT,
					true, pan, contentPanel );

		this.splitter.setDividerSize( 5 );

		contentPanel.add( BorderLayout.NORTH, this.title );

		contentPanel.add( BorderLayout.CENTER, this.editorPanel );

		this.add( BorderLayout.CENTER, this.splitter );

		this.factory = factory;
		}

	public void
	setDividerLocation( final double divPct )
		{
		this.splitter.setDividerLocation( divPct );
		}

	public ConfigureEditorFactory
	getEditorFactory()
		{
		return this.factory;
		}

	public void
	setEditorFactory( final ConfigureEditorFactory factory )
		{
		this.factory = factory;
		}

	public void
	commit()
		{
		for ( final ConfigureSpec spec : this.specVector )
			{
			final ConfigureEditor editor =
				this.factory.createEditor( spec.getPropertyType() );

			if ( editor != null )
				{
				editor.commit( spec, this.prefs, this.origPrefs );
				}
			else
				{
				// UNDONE report this!!!
				}
			}
		}

	private void
	establishConfigTree()
		{
		for ( final ConfigureSpec spec : this.specVector )
			{
			final String path = spec.getPropertyPath();

			this.model.addPath( path, spec );
			}
		}

	public void
	saveCurrentEdit()
		{
		if ( this.currSelection != null && this.currEditor != null )
			{
			this.currEditor.saveChanges
				( this.prefs, this.currSelection.getConfigureSpec() );
			}
		}

	@Override
	public void
	valueChanged( final TreeSelectionEvent event )
		{
		final Object obj = tree.getLastSelectedPathComponent();

		if ( obj == this.currSelection )
			return;

		this.saveCurrentEdit();

		synchronized ( this.editorPanel.getTreeLock() )
			{
			this.editorPanel.removeAll();
			title.setText( "" );

			if ( obj != null )
				{
				final ConfigureTreeNode node =
					this.currSelection =
						(ConfigureTreeNode) obj;

				if ( node.isLeaf() )
					{
					final ConfigureSpec spec = node.getConfigureSpec();

					if ( spec != null )
						{
						this.currEditor =
							this.factory.createEditor
								( node.getConfigureSpec().getPropertyType() );

						if ( this.currEditor == null )
							this.currEditor =
								this.factory.createEditor( CFG_DEFAULT );

						final StringBuilder sb = new StringBuilder();

						sb.append( spec.getName() );

						if ( this.currEditor != null )
							{
							if ( this.currEditor.isModified
									( spec, this.prefs, this.origPrefs ) )
								{
								sb.append( " *" );
								}
							}
						else
							{
							sb.append( " (NO EDITOR)" );
							}

						title.setText( sb.toString() );

						if ( this.currEditor != null )
							{
							this.currEditor.edit( this.prefs, spec );

							this.editorPanel.add
								( BorderLayout.CENTER, this.currEditor );

							this.editorPanel.revalidate();

							this.currEditor.requestInitialFocus();
							}
						}
					}
				else
					{
					this.currEditor = null;
					this.currSelection = null;
					title.setText( node.getName() );
					}
				}
			}

		this.editorPanel.repaint( 250 );
		}

	public void
	addEditor( final String type, final ConfigureEditor editor )
		{
		if ( this.factory instanceof DefaultConfigureEditorFactory )
			{
			((DefaultConfigureEditorFactory) this.factory).addEditor
				( type, editor );
			}
		else
			{
			new Throwable
				( "can not add editor, factory is not class "
						+ "DefaultConfigureEditorFactory" ).
					printStackTrace();
			}
		}

	public String
	treePath( final TreePath treePath )
		{
		ConfigureTreeNode node;
		final Object[] list = treePath.getPath();
		final StringBuilder path = new StringBuilder();

		for ( int i = 1 ; i < list.length ; i++ )
			{
			node = (ConfigureTreeNode) list[i];
			if ( i > 1 )
				path.append('.');
			path.append( node.getName() );
			}

		return path.toString();
		}

	public void
	editProperty( final String propName )
		{
		final String[] propNames = { propName };
		this.editProperties( propNames );
		}

	public void
	editProperties( final String... propNames )
		{
		final int numSpecs = this.specVector.size();

		final Vector pathV = new Vector();

		for ( int i = propNames.length - 1 ; i >= 0 ; --i )
			{
			final String propName = propNames[i];

			for ( int j = 0 ; j < numSpecs ; ++j )
				{
				final ConfigureSpec spec = this.specVector.elementAt(j);

				if ( spec.getPropertyName().equals( propName ) )
					{
					pathV.addElement( spec.getPropertyPath() );
					break;
					}
				}
			}

		if (!pathV.isEmpty())
			{
			final String[] paths = new String[ pathV.size() ];
			pathV.copyInto( paths );
			this.editPaths( paths );
			}
		}

	public void
	editPath( final String path )
		{
		final String[] paths = { path };
		this.editPaths( paths );
		}

	public void
	editPaths( final String... paths )
		{
		for ( int i = paths.length - 1 ; i >= 0 ; --i )
			{
			final ConfigureTreeNode node =
				this.model.getPathNode( paths[ i ] );

			if ( node != null )
				{
				final TreePath tPath = new TreePath( node.getPath() );
				this.tree.expandPath( tPath );
				if ( i == 0 )
					this.tree.setSelectionPath( tPath );
				}
			}
		}


	/**
	 * This panel is used by the editor panel so that we can tell
	 * the scroll pane we are inside to track out width with the
	 * viewport. This essentially eliminates horizontal scrolling
	 * which is quite ugly in this context.
	 */
	private static
	class		EditorPanel
	extends		JPanel
	implements	Scrollable
		{
		@Override
		public Dimension
		getPreferredScrollableViewportSize()
			{
			return this.getPreferredSize();
			}

		@Override
		public int
		getScrollableBlockIncrement
				( final Rectangle visibleRect, final int orientation, final int direction )
			{
				return orientation == SwingConstants.VERTICAL ? visibleRect.height - 10 : visibleRect.width - 10;
			}

		@Override
		public boolean
		getScrollableTracksViewportHeight()
			{
			return false;
			}

		@Override
		public boolean
		getScrollableTracksViewportWidth()
			{
			return true;
			}

		@Override
		public int
		getScrollableUnitIncrement
				( final Rectangle visibleRect, final int orientation, final int direction )
			{
			if ( orientation == SwingConstants.VERTICAL )
				{
				final int unit = visibleRect.height / 10;
				return unit == 0 ? 1 : unit > 20 ? 20 : unit;
				}
			else
				{
				final int unit = visibleRect.width / 10;
				return unit == 0 ? 1 : unit > 20 ? 20 : unit;
				}
			}
		}
	}
