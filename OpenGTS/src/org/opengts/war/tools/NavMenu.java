package org.opengts.war.tools;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.Version;
import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.Account;
import org.opengts.db.tables.User;
import org.opengts.war.tools.*;
import org.opengts.war.track.IconMenu;
import org.opengts.war.track.page.TopMenu.MenuType;

public class NavMenu {

    public static final String NAV_TEXT      = "text";
    public static final String NAV_ICON      = "icon";
    
    public enum NavType implements EnumTools.IntValue, EnumTools.StringValue {
        TEXT        ( 0, NAV_TEXT  ), // default
        ICON        ( 1, NAV_ICON  );
        // ---
        private int      vv = 0;
        private String   aa = null;
        NavType(int v, String a)        { vv = v; aa = a; }
        public boolean isText()         { return this.equals(TEXT); }
        public boolean isIcon()         { return this.equals(ICON); }
        public int     getIntValue()    { return vv; }
        public String  getStringValue() { return this.toString(); }
        public String  toString()       { return aa; }
    }

    final private String leafIconClass = "itemLeafIcon";
    private PrivateLabel privLabel;
    private NavType navigationType;
    private String pageNavNames[];
    
    public NavMenu(PrivateLabel pl, String pNN[]) {
    	this.privLabel = pl;
    	this.pageNavNames = pNN;
    	
        this.navigationType = EnumTools.getValueOf(NavType.class, 
                privLabel.getStringProperty(PrivateLabel.PROP_NavMenu_menuType,null),
                NavType.TEXT);
    }
    
    public StringBuffer getStringBuffer(RequestProperties reqState) {
    	StringBuffer sb = new StringBuffer();
    	Account account 		= reqState.getCurrentAccount();
        PrivateLabel privLabel 	= reqState.getPrivateLabel();
        User user    			= reqState.getCurrentUser();
//	    	privLabel.hasReadAccess(user,wp.getAclName())    	
    	switch(navigationType) {
    	case TEXT:
            // <a href="/track/Track?page=menu.top">Main Menu</a> | 
            // <a href="/track/Track?page=login">Logout</a>&nbsp;&nbsp;
            for (int i = pageNavNames.length - 1; i >= 0; i--) {
                String pageName = pageNavNames[i];
                WebPage page = privLabel.getWebPage(pageName);
                if (page != null) {
                	if (page.systemAdminOnly() && !Account.isSystemAdmin(account)) continue;
                	if (!page.isOkToDisplay(reqState)) continue;
                	if (!privLabel.hasReadAccess(user,page.getAclName())) continue;
                	if (sb.length() > 0) { sb.append(" | "); }
                    String uri  = WebPageAdaptor.EncodeURL(reqState, page.getPageURI());
                    String desc = page.getNavigationDescription(reqState);
                    sb.append("&nbsp;<a href='"+uri+"'>").append(desc).append("</a>");
                } else {
                    String vers   = Version.getVersion();
                    String plName = privLabel.getName();
                    Print.logWarn("Page not found: " + pageName + " [v="+vers+", pl="+plName+"]");
                    //Print.logStackTrace("Page not found: " + pageName + " [v="+vers+", pl="+plName+"]");
                }
            }
            break;
        case ICON:
            for (int i = pageNavNames.length - 1; i >= 0; i--) {
                String pageName = pageNavNames[i];
                WebPage page = privLabel.getWebPage(pageName);
                if (page != null) {
                	if (page.systemAdminOnly() && !Account.isSystemAdmin(account)) continue;
                	if (!page.isOkToDisplay(reqState)) continue;
                	if (!privLabel.hasReadAccess(user,page.getAclName())) continue;
                    String uri  = WebPageAdaptor.EncodeURL(reqState, page.getPageURI());
                    String desc = page.getNavigationDescription(reqState);
                    String icon = page.getMenuIconImage();
                    String menuHelp = StringTools.trim(page.getMenuHelp(reqState, null));
                    if (!StringTools.isBlank(icon))
                    	sb.append("&nbsp;<a href='"+uri+"' title='"+menuHelp+"'>").append("<img class='"+leafIconClass+"' src='"+icon+"' alt='"+desc+"'/>").append("</a>&nbsp;");
                    else sb.append("&nbsp;<a href='"+uri+"'>").append(desc).append("</a>&nbsp;");
                } else {
                    String vers   = Version.getVersion();
                    String plName = privLabel.getName();
                    Print.logWarn("Page not found: " + pageName + " [v="+vers+", pl="+plName+"]");
                    //Print.logStackTrace("Page not found: " + pageName + " [v="+vers+", pl="+plName+"]");
                }
            }
        	break;
    	}
    	return sb;
    }
    
}
