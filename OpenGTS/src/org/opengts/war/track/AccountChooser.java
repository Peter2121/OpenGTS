package org.opengts.war.track;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.war.tools.*;

public class AccountChooser {
// ------------------------------------------------------------------------

    public static boolean showSingleItemTextField(PrivateLabel privLabel)
    {
        if (privLabel == null) {
            return false;
        } else {
            return privLabel.getBooleanProperty(PrivateLabel.PROP_AccountChooser_singleItemTextField,false);
        }
    }
    
    // ------------------------------------------------------------------------

    public static IDDescription.SortBy getSortBy(PrivateLabel privLabel)
    {
        IDDescription.SortBy dft = IDDescription.SortBy.ID;
        if (privLabel == null) {
            return dft;
        } else
        if (privLabel.hasProperty(PrivateLabel.PROP_AccountChooser_sortBy)) {
            String sortBy = privLabel.getStringProperty(PrivateLabel.PROP_AccountChooser_sortBy,null);
            return IDDescription.GetSortBy(sortBy);
        } else {
            return dft;
        }
    }

    // ------------------------------------------------------------------------

    public static boolean isAccountChooserUseTable(PrivateLabel privLabel)
    {
        boolean dft = false;
        if (privLabel == null) {
            return dft;
        } else {
            return privLabel.getBooleanProperty(PrivateLabel.PROP_AccountChooser_useTable,dft);
        }
    }

    // ------------------------------------------------------------------------

    public static boolean isSearchEnabled(PrivateLabel privLabel)
    {
        boolean dft = false;
        if (privLabel == null) {
            return dft;
        } else {
            return privLabel.getBooleanProperty(PrivateLabel.PROP_AccountChooser_search,dft);
        }
    }

    // ------------------------------------------------------------------------

    public static boolean matchUsingContains(PrivateLabel privLabel)
    {
        boolean dft = true;
        if (privLabel == null) {
            return dft;
        } else
        if (privLabel.hasProperty(PrivateLabel.PROP_AccountChooser_matchContains)) {
            String mc = privLabel.getStringProperty(PrivateLabel.PROP_AccountChooser_matchContains,null);
            if (StringTools.isBlank(mc)) {
                return dft;
            } else
            if (mc.equalsIgnoreCase("contains")) {
                return true;
            } else
            if (mc.equalsIgnoreCase("startsWith")) {
                return false;
            } else {
                return StringTools.parseBoolean(mc,dft);
            }
        } else {
            return dft;
        }
    }

    // ------------------------------------------------------------------------

    public static int getIDPosition(PrivateLabel privLabel)
    {
        // 0=none, 1=first, 2=last
        int dft = 1;
        if (privLabel == null) {
            return dft;
        } else
        if (privLabel.hasProperty(PrivateLabel.PROP_AccountChooser_idPosition)) {
            String idPos = privLabel.getStringProperty(PrivateLabel.PROP_AccountChooser_idPosition,null);
            if (StringTools.isBlank(idPos)) {
                return dft;
            } else
            if (idPos.equalsIgnoreCase("first")) {
                return 1;
            } else
            if (idPos.equalsIgnoreCase("last")) {
                return 2;
            } else
            if (idPos.equalsIgnoreCase("none")) {
                return 0;
            } else {
                return dft;
            }
        } else {
            return dft;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // write Style
    
    public static void writeStyle(PrintWriter out, RequestProperties reqState)
        throws IOException 
    {
        WebPageAdaptor.writeCssLink(out, reqState, "AccountChooser.css", null);
    }

    // ------------------------------------------------------------------------
    // write JavaScript

    public static void writeJavaScript(PrintWriter out, Locale locale, RequestProperties reqState, String accountListURL)
        throws IOException
    {
        I18N               i18n      = I18N.getI18N(AccountChooser.class, locale);
        HttpServletRequest request   = reqState.getHttpServletRequest();
        PrivateLabel       privLabel = reqState.getPrivateLabel();

        /* start JavaScript */
        JavaScriptTools.writeStartJavaScript(out);

        /* vars */
        out.write("// AccountChooser vars\n");
//        JavaScriptTools.writeJSVar(out, "ACCOUNT_LIST_URL"           , accountListURL);
        JavaScriptTools.writeJSVar(out, "AccountChooserIDPosition"   , AccountChooser.getIDPosition(privLabel)); // 0=false, 1=first, 2=last
        JavaScriptTools.writeJSVar(out, "AccountChooserEnableSearch" , AccountChooser.isSearchEnabled(privLabel));
        JavaScriptTools.writeJSVar(out, "AccountChooserMatchContains", AccountChooser.matchUsingContains(privLabel));

        /* Localized text */
//  **************** TODO: create localized variables and values
        out.write("// AccountChooser localized text\n");
        JavaScriptTools.writeJSVar(out, "ACCOUNT_TEXT_ID"            , i18n.getString("AccountChooser.ID","ID"));
        JavaScriptTools.writeJSVar(out, "ACCOUNT_TEXT_Description"   , i18n.getString("AccountChooser.description","Description"));
        JavaScriptTools.writeJSVar(out, "ACCOUNT_TEXT_Search"        , i18n.getString("AccountChooser.search","Search"));

        /* end JavaScript */
        JavaScriptTools.writeEndJavaScript(out);

        /* DeviceChooser.js */
        JavaScriptTools.writeJSInclude(out, JavaScriptTools.qualifyJSFileRef("AccountChooser.js"), request);

    }

    public static void writeAccountList(PrintWriter out, RequestProperties reqState, String varName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
//        boolean      isFleet   = reqState.isFleet();
        Locale       locale    = reqState.getLocale();

        /* begin list var */
        out.write("// Account list\n");
        out.write("var "+varName+" = new Array(\n");

        /* Device/Group list */
        IDDescription.SortBy sortBy = AccountChooser.getSortBy(privLabel);
        java.util.List<IDDescription> idList = 
            reqState.createAccountIDDescriptionList(false/*inclInactv*/, sortBy);
        if (!ListTools.isEmpty(idList)) {
            // The 'accountID' is unique, but the description may not be.
            for (Iterator<IDDescription> i = idList.iterator(); i.hasNext();) { 
                IDDescription dd = i.next();
                String dgn  = _escapeText(dd.getID());            // won't contain quotes anyway
                String desc = _escapeText(dd.getDescription());   //
                String name = _escapeText(dd.getName());          //
                out.write("   { id:\""+dgn+"\", desc:\""+desc+"\", name:\""+name+"\" }");
                if (i.hasNext()) { out.write(","); }
                out.write("\n");
            }
        }
        
        /* debug extra entries? */
        int extraCount = (int)privLabel.getLongProperty(PrivateLabel.PROP_AccountChooser_extraDebugEntries,0L);
        if (extraCount > 0) {
            if (extraCount > 5000) { extraCount = 5000; } // arbitrary limit
            if (!ListTools.isEmpty(idList)) { out.write(",\n"); }
            int ofs = 1;
            for (int i = ofs; i < ofs + extraCount; i++) {
                String dgn  = ("account_") + i;
                String desc = i + " Account";
                out.write("   { id:\""+dgn+"\", desc:\""+desc+"\" }");
                if ((i + 1) < (ofs + extraCount)) { out.write(","); }
                out.write("\n");
            }
        }

        /* end list var */
        out.write(");\n");

    }

    // ------------------------------------------------------------------------
    
    private static String ID_CHOOSER_VIEW              = "acctChooserView";
    private static String ID_SEARCH_FORM               = "acctSearchForm";
    public  static String ID_SEARCH_TEXT               = "acctSearchText";
    private static String ID_DIV_TABLE                 = "AccountTableList";
    private static String ID_ACCTSELECT_TABLE          = "acctSelectIDTable";
    private static String CLASS_TABLE_COLUMN_SORTABLE  = "sortableX"; // requires 'sorttable.js'
    private static String CLASS_ACCTSELECT_DIV_VISIBLE = "acctSelectorDiv";
    private static String CLASS_ACCTSELECT_DIV_HIDDEN  = "acctSelectorDiv_hidden";
    private static String CLASS_ACCTSELECT_DIV_TABLE   = "acctSelectorTableList";
    private static String CLASS_ACCTSELECT_ROW_HEADER  = "acctSelectorRowHeader";
    private static String CLASS_ACCTSELECT_COL_HEADER  = "acctSelectorColHeader";
    private static String CLASS_ACCTSELECT_ROW_DATA    = "acctSelectorRowData";
    private static String CLASS_ACCTSELECT_COL_DATA    = "acctSelectorColData";
    private static String CLASS_ACCTSELECT_ROW_HIDDEN  = "acctSelectorRow_hidden";
    private static String CLASS_SEARCH_INPUT           = "acctChooserInput";

    private static int    IDPOS_NONE                  = 0;
    private static int    IDPOS_FIRST                 = 1;
    private static int    IDPOS_LAST                  = 2;

    private static int    WIDTH_ID                    = 80;
    private static int    WIDTH_DESC                  = 180;

    private static int    SEARCH_TEXT_SIZE            = 18;

// TODO: should go to the parent class    
    private static StringBuffer append(StringBuffer sb, String s)
    {
        s = StringTools.replace(s,"\n","\\n");
        s = StringTools.replace(s,"\"","\\\"");
        sb.append("  \"" + s + "\" +\n");
        return sb;
    }

    private static String accountGetTableHTML(RequestProperties reqState, IDDescription list[], int AccountChooserIDPosition, String searchVal)
    {
        PrivateLabel privLabel          = reqState.getPrivateLabel();
        Locale      locale              = reqState.getLocale();
        int         idWidth             = WIDTH_ID;
        int         dsWidth             = WIDTH_DESC;

        /* localized text */
        I18N   i18n                     = I18N.getI18N(AccountChooser.class, locale);
        String ACCOUNT_TEXT_ID           = i18n.getString("AccountChooser.ID","ID");
        String ACCOUNT_TEXT_Description  = i18n.getString("AccountChooser.description","Description");
        String ACCOUNT_TEXT_Search       = i18n.getString("AccountChooser.search","Search");

        /* begin table HTML */
        StringBuffer html = new StringBuffer();
        append(html,"<table id='"+ID_ACCTSELECT_TABLE+"' class='"+CLASS_TABLE_COLUMN_SORTABLE+"' cellspacing='0' cellpadding='0' border='1'>\n");
    
        // table header
        append(html,"<thead>\n");
        append(html,"<tr class='"+CLASS_ACCTSELECT_ROW_HEADER+"'>");
        if (AccountChooserIDPosition == IDPOS_NONE) {
            append(html,"<th nowrap class='"+CLASS_ACCTSELECT_COL_HEADER+"' width='"+dsWidth+"' valign='center'>"+ACCOUNT_TEXT_Description+"</th>");
        } else 
        if (AccountChooserIDPosition == IDPOS_LAST) {
            append(html,"<th nowrap class='"+CLASS_ACCTSELECT_COL_HEADER+"' width='"+dsWidth+"' valign='center'>"+ACCOUNT_TEXT_Description+"</th>");
            append(html,"<th nowrap class='"+CLASS_ACCTSELECT_COL_HEADER+"' width='"+idWidth+"' valign='center'>"+ACCOUNT_TEXT_ID+"</th>");
        } else {
            append(html,"<th nowrap class='"+CLASS_ACCTSELECT_COL_HEADER+"' width='"+idWidth+"' valign='center'>"+ACCOUNT_TEXT_ID+"</th>");
            append(html,"<th nowrap class='"+CLASS_ACCTSELECT_COL_HEADER+"' width='"+dsWidth+"' valign='center'>"+ACCOUNT_TEXT_Description+"</th>");
        }
        append(html,"</tr>\n");
        append(html,"</thead>\n");
    
        // table body
        int extraCount = (int)privLabel.getLongProperty(PrivateLabel.PROP_AccountChooser_extraDebugEntries,0L);
        append(html,"<tbody>\n");
        for (int d = 0; d < list.length + extraCount; d++) {
            String idVal = (d < list.length)? list[d].getID()          : ("v" + String.valueOf(d - list.length + 1));
            String desc  = (d < list.length)? list[d].getDescription() : (String.valueOf(d - list.length + 1) + " asset");

            /* omit items not matched */
            //if (!StringTools.isBlank(searchVal) && !desc.toLowerCase().startsWith(searchVal.toLowerCase())) { 
            //    continue; 
            //}
            String dsTxt = StringTools.htmlFilter(desc, StringTools.HTMLFilterType.TEXT);
            String dsVal = StringTools.htmlFilter(desc, StringTools.HTMLFilterType.VALUE).toLowerCase();

            /* save matched item */
            int selNdx = d;

            /* write html */
            append(html,"<tr idVal='"+idVal+"' dsVal='"+dsVal+"' selNdx='"+selNdx+"' class='"+CLASS_ACCTSELECT_ROW_DATA+"'>");
            if (AccountChooserIDPosition == IDPOS_NONE) {
                append(html,"<td nowrap class='"+CLASS_ACCTSELECT_COL_DATA+"' width='"+dsWidth+"' onclick=\"javascript:deviceSelected("+selNdx+")\">"+ dsTxt +"</td>");
            } else 
            if (AccountChooserIDPosition == IDPOS_LAST) {
                append(html,"<td nowrap class='"+CLASS_ACCTSELECT_COL_DATA+"' width='"+dsWidth+"' onclick=\"javascript:deviceSelected("+selNdx+")\">"+ dsTxt +"</td>");
                append(html,"<td nowrap class='"+CLASS_ACCTSELECT_COL_DATA+"' width='"+idWidth+"' onclick=\"javascript:deviceSelected("+selNdx+")\">"+ idVal +"</td>");
            } else {
                append(html,"<td nowrap class='"+CLASS_ACCTSELECT_COL_DATA+"' width='"+idWidth+"' onclick=\"javascript:deviceSelected("+selNdx+")\">"+ idVal +"</td>");
                append(html,"<td nowrap class='"+CLASS_ACCTSELECT_COL_DATA+"' width='"+dsWidth+"' onclick=\"javascript:deviceSelected("+selNdx+")\">"+ dsTxt +"</td>");
            }
            append(html,"</tr>\n");
    
        }
        append(html,"</tbody>\n");
    
        append(html,"</table>\n");
        return html.toString();
    
    }

    public static void writeChooserDIV(PrintWriter out, RequestProperties reqState, IDDescription list[], String searchVal)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        int         idPos               = AccountChooser.getIDPosition(privLabel);
        Locale      locale              = reqState.getLocale();
        int         idWidth             = WIDTH_ID;
        int         dsWidth             = WIDTH_DESC;

        /* localized text */
        I18N   i18n                     = I18N.getI18N(AccountChooser.class, locale);
        String ACCOUNT_TEXT_ID           = i18n.getString("AccountChooser.ID","ID");
        String ACCOUNT_TEXT_Description  = i18n.getString("AccountChooser.description","Description");
        String ACCOUNT_TEXT_Search       = i18n.getString("AccountChooser.search","Search");

        /* top DIV */
        out.write("\n");
        out.write("<!-- begin AccountChooser DIV -->\n");
        out.write("<div id='"+ID_CHOOSER_VIEW+"' class='"+CLASS_ACCTSELECT_DIV_HIDDEN+"'>\n");

        /* form */   
        if (DeviceChooser.isSearchEnabled(privLabel)) {
            out.write("<form id='"+ID_SEARCH_FORM+"' name='"+ID_SEARCH_FORM+"' method='GET' action=\"javascript:true;\" target='_self' style='padding-left:5px; background-color:#dddddd;'>\n"); // target='_top'
            out.write("<b>"+ACCOUNT_TEXT_Search+": </b>\n");
            out.write("<input id='"+ID_SEARCH_TEXT+"' name='"+ID_SEARCH_TEXT+"' class='"+CLASS_SEARCH_INPUT+"' type='text' value='' size='"+SEARCH_TEXT_SIZE+"' onkeypress=\"return searchKeyPressed(event);\" onkeyup=\"return deviceSearch();\"/>\n");
            out.write("</form>\n");
        }

        /* begin table */
        out.write("<div id='"+ID_DIV_TABLE+"' class='"+CLASS_ACCTSELECT_DIV_TABLE+"'>\n"); // FIX
        out.write("<table id='"+ID_ACCTSELECT_TABLE+"' class='"+CLASS_TABLE_COLUMN_SORTABLE+"' cellspacing='0' cellpadding='0' border='1'>\n");
    
        // table header
        out.write("<thead>\n");
        out.write("<tr class='"+CLASS_ACCTSELECT_ROW_HEADER+"'>");
        if (idPos == IDPOS_NONE) {
            out.write("<th nowrap class='"+CLASS_ACCTSELECT_COL_HEADER+"' width='"+dsWidth+"' valign='center'>"+ACCOUNT_TEXT_Description+"</th>");
        } else 
        if (idPos == IDPOS_LAST) {
            out.write("<th nowrap class='"+CLASS_ACCTSELECT_COL_HEADER+"' width='"+dsWidth+"' valign='center'>"+ACCOUNT_TEXT_Description+"</th>");
            out.write("<th nowrap class='"+CLASS_ACCTSELECT_COL_HEADER+"' width='"+idWidth+"' valign='center'>"+ACCOUNT_TEXT_ID+"</th>");
        } else {
            out.write("<th nowrap class='"+CLASS_ACCTSELECT_COL_HEADER+"' width='"+idWidth+"' valign='center'>"+ACCOUNT_TEXT_ID+"</th>");
            out.write("<th nowrap class='"+CLASS_ACCTSELECT_COL_HEADER+"' width='"+dsWidth+"' valign='center'>"+ACCOUNT_TEXT_Description+"</th>");
        }
        out.write("</tr>\n");
        out.write("</thead>\n");
    
        // table body
        int extraCount = (int)privLabel.getLongProperty(PrivateLabel.PROP_AccountChooser_extraDebugEntries,0L);
        out.write("<tbody>\n");
        for (int d = 0; d < list.length + extraCount; d++) {
            String idVal = (d < list.length)? list[d].getID()          : ("v" + String.valueOf(d - list.length + 1));
            String desc  = (d < list.length)? list[d].getDescription() : (String.valueOf(d - list.length + 1) + " asset");

            /* omit items not matched */
            //if (!StringTools.isBlank(searchVal) && !desc.toLowerCase().startsWith(searchVal.toLowerCase())) { 
            //    continue; 
            //}
            String dsTxt = StringTools.htmlFilter(desc, StringTools.HTMLFilterType.TEXT);
            String dsVal = StringTools.htmlFilter(desc, StringTools.HTMLFilterType.VALUE).toLowerCase();

            /* save matched item */
            int selNdx = d;

            /* write html */
            out.write("<tr idVal='"+idVal+"' dsVal='"+dsVal+"' selNdx='"+selNdx+"' class='"+CLASS_ACCTSELECT_ROW_DATA+"'>");
            if (idPos == IDPOS_NONE) {
                out.write("<td nowrap class='"+CLASS_ACCTSELECT_COL_DATA+"' width='"+dsWidth+"' onclick=\"javascript:accountSelected("+selNdx+")\">"+ dsTxt +"</td>");
            } else 
            if (idPos == IDPOS_LAST) {
                out.write("<td nowrap class='"+CLASS_ACCTSELECT_COL_DATA+"' width='"+dsWidth+"' onclick=\"javascript:accountSelected("+selNdx+")\">"+ dsTxt +"</td>");
                out.write("<td nowrap class='"+CLASS_ACCTSELECT_COL_DATA+"' width='"+idWidth+"' onclick=\"javascript:accountSelected("+selNdx+")\">"+ idVal +"</td>");
            } else {
                out.write("<td nowrap class='"+CLASS_ACCTSELECT_COL_DATA+"' width='"+idWidth+"' onclick=\"javascript:accountSelected("+selNdx+")\">"+ idVal +"</td>");
                out.write("<td nowrap class='"+CLASS_ACCTSELECT_COL_DATA+"' width='"+dsWidth+"' onclick=\"javascript:accountSelected("+selNdx+")\">"+ dsTxt +"</td>");
            }
            out.write("</tr>\n");
    
        }
        out.write("</tbody>\n");
    
        /* end table */
        out.write("</table>\n");
        out.write("</div>\n");
   
        /* end DIV */
        out.write("</div>\n");
        if (DeviceChooser.isSearchEnabled(privLabel)) {
            out.write("<script type=\"text/javascript\">\n");
            out.write("var acctChooserSearchTextElem = document.getElementById('"+ID_SEARCH_TEXT+"');\n");
            out.write("</script>\n");
        }
        out.write("<!-- end AccountChooser DIV -->\n");
        out.write("\n");

    }

    // ------------------------------------------------------------------------

    // TODO: should go to the parent class    
    private static String _escapeText(String s)
    {
        s = StringTools.trim(s);
      //s = StringTools.htmlFilterValue(s);
        s = StringTools.replace(s, "\\", "\\\\");   // must be first
        s = StringTools.replace(s, "\"", "\\\"");   // double-quotes
        s = StringTools.replace(s, "'", "\\'");     // single-quotes
        return s;
    }

    // ------------------------------------------------------------------------

}
