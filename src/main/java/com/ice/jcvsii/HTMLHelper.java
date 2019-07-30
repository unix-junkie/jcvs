
package com.ice.jcvsii;

import java.util.Vector;

import com.ice.util.StringUtilities;


final
class		HTMLHelper
	{
		private HTMLHelper() {
		}

		public static StringBuffer
	generateHTMLDiff( final StringBuffer buf, final String rawDiff, final String fileName, final String rev1, final String rev2 )
		{
		final String lnSep = "\r\n";

		final String lgdBgColor = "#FF6200";
		final String lgdTitleColor = "#FFFFFF";

		final String tableColor = "#F0F0F0";
		final String titleColor = "#C0C0F0";
		final String revHdrColor = "#E0E0E0";
		final String diffHdrColor = "#99CCCC";

		final String diffColorAdd = "#CCCCFF";
		final String diffColorChg = "#99FF99";
		final String diffColorRem = "#FFCCCC";
		final String diffColorNil = "#CCCCCC";

		final String codeFontBeg = "<font face=\"Helvetica,Arial\" size=\"-1\">";
		final String codeFontEnd = "</font>";

		// UNDONE
		// REVIEW
		// I prefer a loop that processed lines individually. This
		// is very wasteful of memory!
		//
		final String[] lines = StringUtilities.splitString( rawDiff, "\n" );

		int lnIdx = 0;
		final int numLines = lines.length;

		for ( ; lnIdx < numLines ; ++lnIdx )
			{
			if ( lines[lnIdx].startsWith( "diff " ) )
				{
				++lnIdx;
				break;
				}
			}

		// UNDONE This is sophomoric!
		if ( lnIdx >= numLines )
			{
			buf.append( "<h3>No Differences</h3>" + lnSep );
			return buf;
			}

		//
		// START DIFF TABLE
		//
		buf.append( "<table bgcolor=\"" );
		buf.append( tableColor );
		buf.append( "\" width=\"100%\" border=0" );
		buf.append( " cellspacing=0 cellpadding=0>" );
		buf.append( lnSep );

		//
		// TITLE CELL
		//
		buf.append( "<tr bgcolor=\"" );
		buf.append( titleColor );
		buf.append( "\">" );
		buf.append( lnSep );

		buf.append( "<td align=\"center\" colspan=2>" + lnSep );

		buf.append( "<table width=\"100%\" border=1 cellpadding=3>" + lnSep );

		buf.append( "<tr>" + lnSep );

		buf.append( "<td align=center colspan=2>" + lnSep );
		buf.append( "<font size=\"+1\">" + lnSep );
		buf.append( "<b>" + lnSep );
		buf.append( "<a href=\"#RAW\">Diff</a>&nbsp;of " );
		buf.append( fileName );
		buf.append( "</b>" + lnSep );
		buf.append( "</font>" + lnSep );
		buf.append( "</td>" + lnSep );

		buf.append( "</tr>" + lnSep );

		buf.append( "<tr bgcolor=\"" );
		buf.append( revHdrColor );
		buf.append( "\">" + lnSep );

		buf.append( "<th align=center width=\"50%\">" + lnSep );
		buf.append( "<font size=\"+1\">" + lnSep );
		buf.append( "<b>" + lnSep );
		buf.append( "Version&nbsp;" );
		buf.append( rev1 );
		buf.append( "</b>" + lnSep );
		buf.append( "</font>" + lnSep );
		buf.append( "</th>" + lnSep );

		buf.append( "<th align=center>" + lnSep );
		buf.append( "<font size=\"+1\">" + lnSep );
		buf.append( "<b>" + lnSep );
		buf.append( "Version&nbsp;" );
		buf.append( rev2 );
		buf.append( "</b>" + lnSep );
		buf.append( "</font>" + lnSep );
		buf.append( "</th>" + lnSep );

		buf.append( "</tr>" + lnSep );

		buf.append( "</table>" + lnSep );

		buf.append( "</td>" + lnSep );

		buf.append( "</tr>" + lnSep );

		//
		// DIFFS
		//
		char state = 'D';

		final Vector ltColV = new Vector();
		final Vector rtColV = new Vector();

		for ( ; lnIdx < numLines ; ++lnIdx )
			{
			final String ln = lines[lnIdx];

			if ( ln.startsWith( "@@" ) )
				{
				String[] flds;

				flds = StringUtilities.splitString( ln, " " );

				final String oldStr = flds[1].substring(1);
				final String newStr = flds[2].substring(1);

				flds = StringUtilities.splitString( oldStr, "," );
				String oldLineCnt = "";
				final String oldLineNum = flds[0];
				if ( flds.length > 1 )
					oldLineCnt = flds[1];

				flds = StringUtilities.splitString( newStr, "," );
				String newLineCnt = "1";
				final String newLineNum = flds[0];
				if ( flds.length > 1 )
					newLineCnt = flds[1];

				buf.append( "<tr bgcolor=\"" );
				buf.append( diffHdrColor );
				buf.append( "\">" );
				buf.append( lnSep );

				buf.append( "<td width=\"50%\">" + lnSep );

				buf.append( "<table width=\"100%\" border=1 cellpadding=3>" + lnSep );
				buf.append( "<tr>" + lnSep );
				buf.append( "<td>" + lnSep );
				buf.append( "<b>Line&nbsp;" );
				buf.append( oldLineNum );
				buf.append( "</b>" + lnSep );
				buf.append( "</td>" + lnSep );
				buf.append( "</tr>" + lnSep );
				buf.append( "</table>" + lnSep );

				buf.append( "</td>" + lnSep );

				buf.append( "<td width=\"50%\">" + lnSep );

				buf.append( "<table width=100% border=1 cellpadding=3>" + lnSep );
				buf.append( "<tr>" + lnSep );
				buf.append( "<td>" + lnSep );
				buf.append( "<b>Line&nbsp;" );
				buf.append( newLineNum );
				buf.append( "</b>" + lnSep );
				buf.append( "</td>" + lnSep );
				buf.append( "</tr>" + lnSep );
				buf.append( "</table>" + lnSep );

				buf.append( "</td>" + lnSep );
				buf.append( "</tr>" + lnSep );

				state = 'D'; // DUMPing...
				ltColV.removeAllElements();
				rtColV.removeAllElements();
				}
			else
				{
				final char diffCode = ln.charAt(0);
				final String remStr = escapeHTML( ln.substring(1) );

				//########
				// ZZ
				// (Hen, zeller@think.de)
				// little state machine to parse unified-diff output
				// in order to get some nice 'ediff'-mode output
				// states:
				//  D "dump"             - just dump the value
				//  R "PreChangeRemove"  - we began with '-' .. so this could be the start of a 'change' area or just remove
				//  C "PreChange"        - okey, we got several '-' lines and moved to '+' lines -> this is a change block
				//#########
				if ( diffCode == '+' )
					{
					if ( state == 'D' )
						{
						// ZZ 'change' never begins with '+': just dump out value
						buf.append( "<tr>" + lnSep );

						buf.append( "<td bgcolor=\"" );
						buf.append( diffColorNil );
						buf.append( "\">" + lnSep );
						buf.append( codeFontBeg );
						buf.append( "&nbsp;" );
						buf.append( codeFontEnd );
						buf.append( lnSep + "</td>" + lnSep );

						buf.append( "<td bgcolor=\"" );
						buf.append( diffColorAdd );
						buf.append( "\">" + lnSep );
						buf.append( codeFontBeg );
						buf.append( remStr );
						buf.append( codeFontEnd );
						buf.append( lnSep + "</td>" + lnSep );

						buf.append( "</tr>" + lnSep );
						}
					else
						{
						// ZZ we got minus before
						state = 'C';
						rtColV.addElement( remStr );
						}
					}
				else if ( diffCode == '-' )
					{
					state = 'R';
					ltColV.addElement( remStr );
					}
				else
					{
					// ZZ empty diffcode
					appendDiffLines( buf, state, ltColV, rtColV );

					buf.append( "<tr>" + lnSep );

					buf.append( "<td>" + lnSep );
					buf.append( codeFontBeg );
					buf.append( remStr );
					buf.append( codeFontEnd );
					buf.append( lnSep +"</td>" + lnSep );

					buf.append( "<td>" + lnSep );
					buf.append( codeFontBeg );
					buf.append( remStr );
					buf.append( codeFontEnd );
					buf.append( lnSep + "</td>" + lnSep );

					buf.append( "</tr>" + lnSep );

					state = 'D';
					ltColV.removeAllElements();
					rtColV.removeAllElements();
					}
				}
			}

		appendDiffLines( buf, state, ltColV, rtColV );

		// UNDONE
		/*
		# state is empty if we didn't have any change
		if ( ! $state )
			{
			print "<tr><td colspan=2>&nbsp;</td></tr>";
			print "<tr bgcolor=\"$diffcolorEmpty\" >";
			print "<td colspan=2 align=center>";
			print "<b>- No viewable Change -</b>";
			print "</td></tr>";
			}
		*/

		buf.append( "<tr bgcolor=\"" );
		buf.append( lgdBgColor );
		buf.append( "\">" + lnSep );
		buf.append( "<td colspan=2>" + lnSep );

		//
		// L E G E N D TABLE
		//
		buf.append( "<table width=100% border=1>" + lnSep );

		buf.append( "<tr bgcolor=\"" );
		buf.append( lgdTitleColor );
		buf.append( "\">" + lnSep );
		buf.append( "<td align=\"center\">" + lnSep );
		buf.append( "<strong>-- Legend --</strong><br>" + lnSep );

		buf.append( "<table width=\"100%\" border=0 cellspacing=0 cellpadding=2>" + lnSep );

		buf.append( "<tr>" + lnSep );

		buf.append( "<td width=\"50%\" align=center bgcolor=\"" );
		buf.append( diffColorRem );
		buf.append( "\">" + lnSep );
		buf.append( "Removed in v." );
		buf.append( rev1 );
		buf.append( lnSep );
		buf.append( "</td>" + lnSep );
		buf.append( "<td width=\"50%\" bgcolor=\"" );
		buf.append( diffColorNil );
		buf.append( "\">&nbsp;" );
		buf.append( "</td>" + lnSep );

		buf.append( "</tr>" + lnSep );

		buf.append( "<tr bgcolor=\"" );
		buf.append( diffColorChg );
		buf.append( "\">" + lnSep );

		buf.append( "<td align=\"center\" colspan=2>" + lnSep );
		buf.append( "changed lines" + lnSep );
		buf.append( "</td>" + lnSep );

		buf.append( "</tr>" + lnSep );

		buf.append( "<tr>" + lnSep );
		buf.append( "<td width=\"50%\" bgcolor=\"" );
		buf.append( diffColorNil );
		buf.append( "\">&nbsp;" );
		buf.append( "</td>" + lnSep );

		buf.append( "<td width=\"50%\" align=\"center\" bgcolor=\"" );
		buf.append( diffColorAdd );
		buf.append( "\">" + lnSep );
		buf.append( "Inserted in v." + lnSep );
		buf.append( rev2 );
		buf.append( lnSep );
		buf.append( "</td>" + lnSep );
		buf.append( "</tr>" + lnSep );

		buf.append( "</table>" + lnSep ); // Colors Table

		buf.append( "</td>" + lnSep );
		buf.append( "</tr>" + lnSep );
		buf.append( "</table>" + lnSep ); // Legend Table

		buf.append( "</td>" + lnSep );
		buf.append( "</tr>" + lnSep );

		//
		// END DIFF TABLE
		//

		buf.append( "</table>" + lnSep );

		//
		// RAW DIFF
		//
		buf.append( "<a name=\"RAW\"></a>" + lnSep );
		buf.append( "<a href=\"#TOP\">Back To Top</a><br>" + lnSep );
		buf.append( "<pre>" + lnSep );
		buf.append( adjustPlainText( rawDiff ) );
		buf.append( lnSep );
		buf.append( "</pre>" + lnSep );

		return buf;
		}

	private static void
	appendDiffLines( final StringBuffer buf, final char state, final Vector ltColV, final Vector rtColV )
		{
		final String lnSep = "\r\n";

		final String clrRmv = "#FFCCCC";
		final String clrChg = "#99FF99";
		final String clrChgDk = "#44CC44";
		final String clrAdd = "#CCCCFF";
		final String clrNil = "#CCCCCC";

		final String codeFontBeg = "<font face=\"Helvetica,Arial\" size=\"-1\">";
		final String codeFontEnd = "</font>";

		if ( state == 'R' )
			{
			// ZZ we just got remove-lines before
			for ( int j = 0 ; j < ltColV.size() ; ++j )
				{
				buf.append( "<tr>" + lnSep );

				buf.append( "<td bgcolor=\"" );
				buf.append( clrRmv );
				buf.append( "\">" + lnSep );
				buf.append( codeFontBeg );
				buf.append( ltColV.elementAt(j) );
				buf.append( codeFontEnd );
				buf.append( lnSep );
				buf.append( "</td>" + lnSep );

				buf.append( "<td bgcolor=\"" );
				buf.append( clrNil );
				buf.append( "\">" + lnSep );
				buf.append( codeFontBeg );
				buf.append( "&nbsp;" );
				buf.append( codeFontEnd );
				buf.append( lnSep );
				buf.append( "</td>" + lnSep );

				buf.append( "</tr>" + lnSep );
				}
			}
		else if ( state == 'C' )
			{
			// ZZ state eq "PreChange"
			// ZZ we got removes with subsequent adds
			for ( int j = 0 ; j < ltColV.size() || j < rtColV.size() ; ++j )
				{
				// ZZ dump out both cols
				buf.append( "<tr>" + lnSep );

				if ( j < ltColV.size() )
					{
					buf.append( "<td bgcolor=\"" );
					buf.append( clrChg );
					buf.append( "\">" + lnSep );
					buf.append( codeFontBeg );
					buf.append( ltColV.elementAt(j) );
					buf.append( codeFontEnd );
					buf.append( lnSep );
					buf.append( "</td>" + lnSep );
					}
				else
					{
					buf.append( "<td bgcolor=\"" );
					buf.append( clrChgDk );
					buf.append( "\">" + lnSep );
					buf.append( codeFontBeg );
					buf.append( "&nbsp;" );
					buf.append( codeFontEnd );
					buf.append( lnSep );
					buf.append( "</td>" + lnSep );
					}

				if ( j < rtColV.size() )
					{
					buf.append( "<td bgcolor=\"" );
					buf.append( clrChg );
					buf.append( "\">" + lnSep );
					buf.append( codeFontBeg );
					buf.append( rtColV.elementAt(j) );
					buf.append( codeFontEnd );
					buf.append( lnSep );
					buf.append( "</td>" + lnSep );
					}
				else
					{
					buf.append( "<td bgcolor=\"" );
					buf.append( clrChgDk );
					buf.append( "\">" + lnSep );
					buf.append( codeFontBeg );
					buf.append( "&nbsp;" );
					buf.append( codeFontEnd );
					buf.append( lnSep );
					buf.append( "</td>" + lnSep );
					}

				buf.append( "</tr>" + lnSep );
				}
			}

		}

	private static String
	adjustPlainText(final CharSequence text)
		{
		final int saveIdx = 0;

		final int textLen = text.length();

			return "<pre>\n" +
				       escapeHTML(text) +
				       "\n</pre>\n";
		}

	private static String
	escapeHTML(final CharSequence text)
		{
		final int saveIdx = 0;
		final int textLen = text.length();
		boolean sendNBSP = false;

		final StringBuilder result =
			new StringBuilder(textLen + 2048 );

		if ( textLen == 0 )
			result.append( "&nbsp;" );

		for ( int i = 0 ; i < textLen ; ++i )
			{
			final char ch = text.charAt(i);

				switch (ch) {
				case '<':
					result.append("&lt;");
					break;
				case '>':
					result.append("&gt;");
					break;
				case '&':
					result.append("&amp;");
					break;
				case '"':
					result.append("&quot;");
					break;
				case ' ':
// UNDONE

					sendNBSP = !sendNBSP;
					if (sendNBSP)
						result.append("&nbsp;");
					else
						result.append(' ');
					break;
				case '\t':
// UNDONE

					sendNBSP = false;
					result.append("&nbsp;&nbsp;&nbsp; ");
					break;
				default:
					result.append(ch);
					break;
				}
			}

		return result.toString();
		}

	}


