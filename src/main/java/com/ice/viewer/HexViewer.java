
package com.ice.viewer;

import java.awt.Adjustable;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.io.InputStream;
import java.text.FieldPosition;
import java.util.Vector;

import javax.activation.CommandObject;
import javax.activation.DataHandler;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;

import com.ice.text.HexNumberFormat;
import com.ice.util.AWTUtilities;


class		HexViewer
extends		JComponent
implements	CommandObject
	{
	private static final int			BLOCKSIZE = 256;
	private static final int			HEXBYTES = 16;
	private static final int			HEXLINES = 16;
	private static final int			PAGEINCR = 8;

		private final JScrollPane		scroller = null;

	private final JEditorPane		editorPane = null;
	private final EditorKit		editor = null;
	private final Document		doc = null;

	private boolean			hitEOF;
	private boolean			closeStream;
	private int				dataLength = -1;
	private InputStream		dataStream;

		private final Vector			blockCache;
	private HexNumberFormat	hexFmt;

	private HexCanvas		hexCanvas;
	private JScrollBar		scrollBar;
	private JTextField		blkHexField;
	private JTextField		blkDecField;
	private JTextField		offHexField;
	private JTextField		offDecField;
	private Cursor			saveCursor;


    HexViewer()
		{
		this( null, -1 );
		}

    private HexViewer(final InputStream content, final int length)
		{
		this.hitEOF = false;
		this.dataStream = null;
		this.blockCache = new Vector();
		this.setDoubleBuffered( true );

		this.establishContents();

		if ( content != null )
			{
			this.setMessage( content, length );
			}
		}

	@Override
	public void
	removeNotify()
		{
		super.removeNotify();
		this.checkClose();
		}

	private void
	checkClose()
		{
		if ( this.dataStream != null )
			{
			if ( this.closeStream )
				{
				try { this.dataStream.close(); }
				catch ( final IOException ex ) { }
				this.dataStream = null;
				}
			}
		}

	public boolean
	getCloseStream()
		{
		return this.closeStream;
		}

	public void
	setCloseStream( final boolean flag )
		{
		this.closeStream = flag;
		}

    /**
     * the CommandObject method to accept our DataHandler
     * @param dh	the datahandler used to get the content
     */
    @Override
    public void
	setCommandContext( final String verb, final DataHandler dh )
		throws IOException
		{

			final InputStream content = dh.getInputStream();

		this.setMessage( content, -1 );
		}

    /**
     * sets the current message to be displayed in the viewer
     */
    private void
	setMessage(final InputStream content, final int length)
		{
		this.dataStream = content;
		this.dataLength = length;

		this.adjustScroller();

		if ( content != null )
			{
			this.setCurrentBlock( 0 );
			}

		this.invalidate();
		this.validate();
		}

	private void
	adjustScroller()
		{
		if ( this.dataLength < 0 )
			{
			this.scrollBar.setMaximum( PAGEINCR );
			}
		else
			{
			this.scrollBar.setMaximum
				( (this.dataLength + BLOCKSIZE - 1)
					/ BLOCKSIZE );
			}
		}

	private void
	setCurrentBlock( final int offset )
		{
		this.setWaitCursor();

		int blkNum = offset / BLOCKSIZE;

		if ( blkNum >= this.blockCache.size() )
			{
			try {
				if ( this.readBlock( blkNum ) == -1 )
					{
					this.hitEOF = true;
					blkNum = this.blockCache.size() - 1;
					this.scrollBar.setValue( blkNum );
					this.scrollBar.setMaximum( blkNum + 1 );
					}
				}
			catch ( final IOException ex )
				{
				ex.printStackTrace( System.err );
				blkNum = this.blockCache.size() - 1;
				}
			}

			final int currentBlkIdx = blkNum;

		if (currentBlkIdx < this.blockCache.size() )
			{
			byte[] dispData =
				(byte[]) this.blockCache.elementAt( blkNum );

			if (this.hitEOF
			    && this.dataLength >= 0
			    && currentBlkIdx
			       == this.blockCache.size() - 1 )
				{
				final int rem =
						this.dataLength
						- currentBlkIdx * BLOCKSIZE;

				if ( rem != BLOCKSIZE )
					{
					final byte[] remData = new byte[ rem ];
					System.arraycopy( dispData, 0, remData, 0, rem );
					dispData = remData;
					}
				}

			this.hexCanvas.displayData( dispData );
			}
		else
			{
			this.hexCanvas.displayEOF();
			}

		if ( this.dataLength < 0 )
			{
			if (currentBlkIdx >
					this.scrollBar.getMaximum()
						- PAGEINCR )
				{
				this.scrollBar.setMaximum
					(currentBlkIdx + PAGEINCR );
				}
			}

		int hexVal = currentBlkIdx;
		String fldStr = this.hexFmt.format(hexVal);
		this.blkHexField.setText( fldStr );
		fldStr = String.valueOf(hexVal);
		this.blkDecField.setText( fldStr );

		hexVal = currentBlkIdx * BLOCKSIZE;
		fldStr = this.hexFmt.format(hexVal);
		this.offHexField.setText( fldStr );
		fldStr = String.valueOf(hexVal);
		this.offDecField.setText( fldStr );

		this.scrollBar.setValue(currentBlkIdx);

		this.resetCursor();
		}

	private int
	readBlock(final int blkNum)
		throws IOException
		{
		int result = 0;
		int curBlk = this.blockCache.size();

		for ( ; curBlk <= blkNum ; ++curBlk )
			{
			final byte[] buf = new byte[ BLOCKSIZE ];

			int off = 0;
			int need = buf.length;
			for ( ; need > 0 ; )
				{
				final int numRead = this.dataStream.read( buf, off, need );
				if ( numRead < 0 )
					{
					result = -1;
					this.checkClose();
					if ( this.dataLength < 0 )
						{
						this.dataLength =
							curBlk * BLOCKSIZE + off;
						}
					break;
					}

				off += numRead;
				need -= numRead;
				}

			if ( off > 0 )
				{
				this.blockCache.addElement( buf );
				}
			}

		return result;
		}

	private void
	establishContents()
		{
		int row = 0;
		this.setLayout( new GridBagLayout() );

		this.hexFmt = new HexNumberFormat( "XXXXXXXX" );

		final JComponent ctlPanel = new JPanel();
		ctlPanel.setLayout( new GridBagLayout() );
		ctlPanel.setBorder( new EmptyBorder( 2, 2, 2, 2 ) );
		AWTUtilities.constrain(
			this, ctlPanel,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.CENTER,
			0, row++, 1, 1, 1.0, 0.0 );

		int ctlRow = 0;
		int ctlCol = 0;

		JLabel label = new JLabel( "Block:" );
		label.setHorizontalAlignment( SwingConstants.RIGHT );
		label.setBorder( new EmptyBorder( 0, 2, 0, 2 ) );
		AWTUtilities.constrain(
			ctlPanel, label,
			GridBagConstraints.NONE,
			GridBagConstraints.CENTER,
			ctlCol++, ctlRow, 1, 1, 0.0, 0.0 );

		this.blkHexField = new JTextField();
		AWTUtilities.constrain(
			ctlPanel, this.blkHexField,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.CENTER,
			ctlCol++, ctlRow, 1, 1, 0.2, 0.0 );

		this.blkDecField = new JTextField();
		AWTUtilities.constrain(
			ctlPanel, this.blkDecField,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.CENTER,
			ctlCol++, ctlRow, 1, 1, 0.25, 0.0 );

		label = new JLabel( "Offset:" );
		label.setHorizontalAlignment( SwingConstants.RIGHT );
		label.setBorder( new EmptyBorder( 0, 8, 0, 2 ) );
		AWTUtilities.constrain(
			ctlPanel, label,
			GridBagConstraints.NONE,
			GridBagConstraints.CENTER,
			ctlCol++, ctlRow, 1, 1, 0.0, 0.0 );

		this.offHexField = new JTextField();
		AWTUtilities.constrain(
			ctlPanel, this.offHexField,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.CENTER,
			ctlCol++, ctlRow, 1, 1, 0.2, 0.0 );

		this.offDecField = new JTextField();
		AWTUtilities.constrain(
			ctlPanel, this.offDecField,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.CENTER,
			ctlCol++, ctlRow++, 1, 1, 0.35, 0.0 );

		final JComponent dataPanel = new JPanel();
		dataPanel.setLayout( new GridBagLayout() );
		dataPanel.setBorder( new EmptyBorder( 2, 2, 2, 2 ) );
		AWTUtilities.constrain(
			this, dataPanel,
			GridBagConstraints.BOTH,
			GridBagConstraints.CENTER,
			0, row++, 1, 1, 1.0, 1.0 );

		this.scrollBar =
			new JScrollBar
				( Adjustable.HORIZONTAL, 0, 1, 0, 1 );

		this.scrollBar.setBlockIncrement( PAGEINCR );
		this.scrollBar.getModel().addChangeListener
			( this.new ScrollerChangeListener() );

		AWTUtilities.constrain(
			dataPanel, this.scrollBar,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.CENTER,
			0, 0, 1, 1, 1.0, 0.0 );

		this.hexCanvas = new HexCanvas();
		this.hexCanvas.setOpaque( true );
		this.hexCanvas.setBackground( Color.white );
		this.hexCanvas.setBorder( new EmptyBorder( 0, 8, 0, 2 ) );
		this.hexCanvas.setFont
			( new Font( "Monospaced", Font.PLAIN, 12 ) );

		AWTUtilities.constrain(
			dataPanel, this.hexCanvas,
			GridBagConstraints.BOTH,
			GridBagConstraints.CENTER,
			0, 1, 1, 1, 1.0, 1.0 );
		}

	private static final
	class		HexCanvas
	extends		JComponent
		{
		private int			hexHeight;
		private int			hexByteW;
		private int			hexQuadW;
		private int			hexDataW;
		private int			hexSepW;
		private int			hexCharW;
		private int			hexLineW;
		private int			spaceW;
		private int			offsetW;

		private final Dimension	mDim;
		private final Dimension	pDim;

		private boolean		displayEOF;
		private byte[]		data;

		private final HexNumberFormat	format;


		private HexCanvas()
			{
			super();
			this.data = null;
			this.displayEOF = false;
			this.mDim = new Dimension( 20, 20 );
			this.pDim = new Dimension( 400, 400 );
			this.format = new HexNumberFormat( "XX" );
			}

		@Override
		public boolean
		isFocusTraversable()
			{
			return false;
			}

		void
		displayEOF()
			{
			this.data = null;
			this.displayEOF = true;
			this.repaint( 500 );
			}

		void
		displayData(final byte[] data)
			{
			this.data = data;
			this.displayEOF = false;
			this.repaint( 500 );
			}

		@Override
		public void
		update( final Graphics updateG )
			{
			this.paint( updateG );
			}

		@Override
		public synchronized void
		paint( final Graphics g )
			{
			int         i;
				int j;
				final int			top;
				final int left;
				final int			width;
				final int height;
				int			x;
				int y;

				final Dimension   sz = this.getSize();

			if ( this.isOpaque() )
				{
				g.setColor( this.getBackground() );
				g.fillRect( 0, 0, sz.width, sz.height );
				}

			g.setColor( this.getForeground() );

			final Font fHex = this.getFont();
			final Font fTitle = this.getFont();

			x = 1;
			y = this.hexHeight + 1;

			if ( this.displayEOF )
				{
				// UNDONE This is HORRIBLY inefficient!
				g.setFont( new Font( "Serif", Font.BOLD, 18 ) );
				final FontMetrics fm = g.getFontMetrics( g.getFont() );
				final String msg = "End Of Data";
				final int sW = fm.stringWidth( msg );
				x = (sz.width - sW) / 2;
				y = (sz.height - fm.getHeight()) / 2
						+ fm.getHeight();
				g.drawString( msg, x, y );
				return;
				}
			else if ( this.data == null )
				{
				return;
				}

			int cnt = 0;
			g.setFont( fHex );
			for ( i = 0
					; i < HEXLINES
							&& cnt < this.data.length
						; ++i )
				{
				final StringBuffer buf = new StringBuffer();
				final StringBuilder chBuf = new StringBuilder();
				final FieldPosition pos = new FieldPosition(0);

				this.format.format
					(i * HEXBYTES, buf, pos );
				buf.append( ": " );

				for ( j = 0
						; j < HEXBYTES
								&& cnt < this.data.length
							; ++j, ++cnt )
					{
					final int index = i * HEXBYTES + j;

					if ( index >= this.data.length )
						break;

					final char ch = (char)this.data[ index ];
					chBuf.append
						(ch >= 32 && ch < 127 ? ch : '.' );

					this.format.format
						((int) this.data[index], buf, pos );

					buf.append(' ');
					}

				for ( ; j < HEXBYTES ; ++j )
					{
					buf.append( "   " );
					}

				buf.append(' ');
				buf.append( chBuf );

				g.drawString( buf.toString(), x, y );

				y += this.hexHeight;
				}
			}

		@Override
		public void
		addNotify()
			{
			super.addNotify();
			this.computeDimensions();
			}

		@Override
		public void
		setFont( final Font f )
			{
			super.setFont( f );

			final Graphics g = this.getGraphics();
			if ( g != null )
				{
				this.establishFontMetrics( g, f );
				}
			}

		private void
		establishFontMetrics( final Graphics g, final Font f )
			{
			final FontMetrics fm = g.getFontMetrics( this.getFont() );
			this.hexHeight = fm.getLeading() + fm.getAscent();

			this.spaceW = fm.stringWidth( " " );
			this.offsetW = fm.stringWidth( "88: " );
			this.hexByteW = fm.stringWidth( "88" );
			this.hexQuadW =
				4 * hexByteW + 3 * this.spaceW;

			this.hexDataW = 4 * hexQuadW;

			this.hexSepW = fm.stringWidth( "  " );

			this.hexCharW =
				fm.stringWidth( "0123456789ABCDEF" );

			this.hexLineW =
				this.offsetW + this.hexDataW
					+ this.hexSepW + this.hexCharW;
			}

		void
		computeDimensions()
			{
			final int			width;
				final int height;
				final Dimension	sz = this.getSize();
			final Graphics	g = this.getGraphics();

			this.establishFontMetrics( g, this.getFont() );

			width = this.hexLineW + 2;
			height = HEXLINES * this.hexHeight + 2;

			this.mDim.width = width;
			this.mDim.height = height;

			this.pDim.width = mDim.width;
			this.pDim.height = mDim.height;
			}

		@Override
		public Dimension
		getPreferredSize()
			{
			return pDim;
			}

		@Override
		public Dimension
		getMinimumSize()
			{
			return pDim;
			}

		}

	private void
	resetCursor()
		{
		final Component f = this.getTopLevelAncestor();
		if ( f != null && this.saveCursor != null )
			f.setCursor( this.saveCursor );
		this.saveCursor = null;
		}

	private void
	setWaitCursor()
		{
		final Component f = this.getTopLevelAncestor();
		if ( f != null )
			{
			this.saveCursor = f.getCursor();
			f.setCursor
				( Cursor.getPredefinedCursor
					( Cursor.WAIT_CURSOR ) );
			}
		}

	private class
	ScrollerChangeListener implements ChangeListener
		{
		@Override
		public void
		stateChanged( final ChangeEvent event )
			{
			final int value = scrollBar.getValue();
			setCurrentBlock( value * BLOCKSIZE );
			}
		}

	}

