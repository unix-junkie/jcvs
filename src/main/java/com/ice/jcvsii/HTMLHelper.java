
package com.ice.jcvsii;

import java.awt.*;
import java.util.Vector;
import javax.swing.*;
import com.ice.util.StringUtilities;


public
class		HTMLHelper
	{
	public static StringBuffer
	generateHTMLDiff( StringBuffer buf, String rawDiff, String fileName, String rev1, String rev2 )
		{
		String lnSep = "\r\n";

		String lgdBgColor = "#FF6200";
		String lgdTitleColor = "#FFFFFF";

		String tableColor = "#F0F0F0";
		String titleColor = "#C0C0F0";
		String revHdrColor = "#E0E0E0";
		String diffHdrColor = "#99CCCC";

		String diffColorAdd = "#CCCCFF";
		String diffColorChg = "#99FF99";
		String diffColorRem = "#FFCCCC";
		String diffColorNil = "#CCCCCC";

		String codeFontBeg = "<font face=\"Helvetica,Arial\" size=\"-1\">";
		String codeFontEnd = "</font>";

		// UNDONE
		// REVIEW
		// I prefer a loop that processed lines individually. This
		// is very wasteful of memory!
		//
		String[] lines = StringUtilities.splitString( rawDiff, "\n" );

		int lnIdx = 0;
		int numLines = lines.length;

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

		Vector ltColV = new Vector();
		Vector rtColV = new Vector();

		for ( ; lnIdx < numLines ; ++lnIdx )
			{
			String ln = lines[lnIdx];

			if ( ln.startsWith( "@@" ) )
				{
				String[] flds;
				
				flds = StringUtilities.splitString( ln, " " );

				String oldStr = flds[1].substring(1);
				String newStr = flds[2].substring(1);

				flds = StringUtilities.splitString( oldStr, "," );
				String oldLineCnt = "";
				String oldLineNum = flds[0];
				if ( flds.length > 1 )
					oldLineCnt = flds[1];

				flds = StringUtilities.splitString( newStr, "," );
				String newLineCnt = "1";
				String newLineNum = flds[0];
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
				char diffCode = ln.charAt(0);
				String remStr = HTMLHelper.escapeHTML( ln.substring(1) );

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
					HTMLHelper.appendDiffLines( buf, state, ltColV, rtColV );
					
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

		HTMLHelper.appendDiffLines( buf, state, ltColV, rtColV );

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
		buf.append( HTMLHelper.adjustPlainText( rawDiff ) );
		buf.append( lnSep );
		buf.append( "</pre>" + lnSep );

		return buf;
		}

	private static StringBuffer
	appendDiffLines( StringBuffer buf, char state, Vector ltColV, Vector rtColV )
		{
		String lnSep = "\r\n";

		String clrRmv = "#FFCCCC";
		String clrChg = "#99FF99";
		String clrChgDk = "#44CC44";
		String clrAdd = "#CCCCFF";
		String clrNil = "#CCCCCC";

		String codeFontBeg = "<font face=\"Helvetica,Arial\" size=\"-1\">";
		String codeFontEnd = "</font>";

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

		return buf;
		}

	public static String
	adjustPlainText( String text )
		{
		int saveIdx = 0;

		int textLen = text.length();

		StringBuffer result = new StringBuffer( textLen + 2048 );

		result.append( "<pre>\n" );

		result.append( HTMLHelper.escapeHTML( text ) );
		
		result.append( "\n</pre>\n" );

		return result.toString();
		}

	public static String
	escapeHTML( String text )
		{
		int saveIdx = 0;
		int textLen = text.length();
		boolean sendNBSP = false;

		StringBuffer result =
			new StringBuffer( textLen + 2048 );

		if ( textLen == 0 )
			result.append( "&nbsp;" );

		for ( int i = 0 ; i < textLen ; ++i )
			{
			char ch = text.charAt(i);

			if ( ch == '<' )
				result.append( "&lt;" );
			else if ( ch == '>' )
				result.append( "&gt;" );
			else if ( ch == '&' )
				result.append( "&amp;" );
			else if ( ch == '"' )
				result.append( "&quot;" );
			else if ( ch == ' ' ) // UNDONE
				{
				sendNBSP = ! sendNBSP;
				if ( sendNBSP )
					result.append( "&nbsp;" );
				else
					result.append( " " );
				}
			else if ( ch == '\t' ) // UNDONE
				{
				sendNBSP = false;
				result.append( "&nbsp;&nbsp;&nbsp; " );
				}
			else
				result.append( ch );
			}
	
		return result.toString();
		}

	}


