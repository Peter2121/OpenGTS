// ----------------------------------------------------------------------------
//
// ----------------------------------------------------------------------------

/* these must match the class definitions in "AccountChooser.css" */
var ID_CHOOSER_VIEW              = "acctChooserView";
var ID_SEARCH_FORM               = "acctSearchForm";
var ID_SEARCH_TEXT               = "acctSearchText";
var ID_DIV_TABLE                 = "AccountTableList";
var ID_ACCTSELECT_TABLE          = "acctSelectIDTable";
var CLASS_TABLE_COLUMN_SORTABLE  = "sortableX"; // requires 'sorttable.js'
var CLASS_ACCTSELECT_DIV_VISIBLE = "acctSelectorDiv";
var CLASS_ACCTSELECT_DIV_HIDDEN  = "acctSelectorDiv_hidden";
var CLASS_ACCTSELECT_DIV_TABLE   = "acctSelectorTableList";
var CLASS_ACCTSELECT_ROW_HEADER  = "acctSelectorRowHeader";
var CLASS_ACCTSELECT_COL_HEADER  = "acctSelectorColHeader";
var CLASS_ACCTSELECT_ROW_DATA    = "acctSelectorRowData";
var CLASS_ACCTSELECT_COL_DATA    = "acctSelectorColData";
var CLASS_ACCTSELECT_ROW_HIDDEN  = "acctSelectorRow_hidden";
var CLASS_SEARCH_INPUT           = "acctChooserInput";

var ID_ACCOUNT_DD		         = "accountDropDown";
var ID_ACCOUNT_ID        		 = "accountSelector";
var ID_ACCOUNT_DESCR     		 = "accountDescription";

/* these must match PARM_TABLE_ACC_DEV parameter's value in UGroupInfo.java */
var ID_TABLE_ACC_DEV			 = "t_acctdev";

var IDPOS_NONE                  = 0;
var IDPOS_FIRST                 = 1;
var IDPOS_LAST                  = 2;

var WIDTH_ID                    = 80;
var WIDTH_DESC                  = 180;

var SEARCH_TEXT_SIZE            = 18;

var PREDEFINED_CHOOSER_HTML     = true;

// ----------------------------------------------------------------------------
// external variable definitions (see AccountChooser.java)

//var ACCOUNT_LIST_URL             = 
//var AccountChooserIDPosition     = 2    // 0=false, 1=first, 2=last
//var AccountChooserEnableSearch   = true
//var AccountChooserMatchContains  = true

//var ACCOUNT_TEXT_ID              = "ID"
//var ACCOUNT_TEXT_Description     = "Description"
//var ACCOUNT_TEXT_Search          = "Search"

// ----------------------------------------------------------------------------

var accountSelectorVisible       = false;
var accountSelectorView          = null;
var accountOld_onmousedown       = null;

// ----------------------------------------------------------------------------

var chooserRelativeElementID   = null;
var chooserRelativeElementDesc = null;
var chooserAccountList          = null;
var chooserFirstAccountNdx      = -1;

function groupInfoShowSelector()
{
    if (accountShowChooserList) {
        var list = (typeof TrackSelectorList != 'undefined')? TrackSelectorList : null;
        accountShowChooserList(ID_ACCOUNT_ID, ID_ACCOUNT_DESCR, list); 
    }
};

function accountShowChooserList(elemNameID, elemNameDesc, list)
{

    /* initial clear */
    chooserRelativeElementID   = null;
    chooserRelativeElementDesc = null;
    chooserAccountList          = null;
    chooserFirstAccountNdx      = -1;

    /* already displayed? close ... */
    if (accountSelectorVisible) {
        accountCloseChooser();
        return;
    }

    /* get destination ID field */
    var locElem = document.getElementById(elemNameID);
    if ((locElem != null) && (locElem.type != "hidden")) {
        // elemNameID
    } else {
        locElem = document.getElementById(elemNameDesc);
        if (locElem != null) {
            // elemNameDesc
        } else {
            return; // not found
        }
    }

    /* get Drop-Down span ID */
    var spanDD = document.getElementById(ID_ACCOUNT_DD);
    
    /* save global vars */
    chooserRelativeElementID   = elemNameID;
    chooserRelativeElementDesc = elemNameDesc;
    chooserAccountList         = list;

    /* location of Chooser */
    var absLoc  = getElementPosition(locElem);
    var absSiz  = getElementSize(locElem);
    var posTop  = absLoc.left;
    var posLeft = absLoc.top + absSiz.height + 2;
    
    if (PREDEFINED_CHOOSER_HTML) {
        
        accountSelectorView = document.getElementById(ID_CHOOSER_VIEW);
        if (accountSelectorView != null) {
            accountSelectorView.className      = CLASS_ACCTSELECT_DIV_VISIBLE;
            accountSelectorView.style.left     = posTop  + 'px';
            accountSelectorView.style.top      = posLeft + 'px';
            accountSelectorView.style.position = 'absolute';
            accountSelectorView.style.cursor   = 'default';
            accountSelectorView.style.zIndex   = 30000;
            accountSelectorVisible             = true;
        } else {
            //accountSelectorView = createDivBox(CLASS_ACCTSELECT_DIV_VISIBLE, absLoc.left, absLoc.top + absSiz.height + 2, -1, -1);
        }

    } else {

        /* create div */
        accountSelectorView = createDivBox(CLASS_ACCTSELECT_DIV_VISIBLE, posTop, posLeft, -1, -1);

        /* start html */
        var html = "";

        /* include search */
        if (AccountChooserEnableSearch) {
            html += "<form id='"+ID_SEARCH_FORM+"' name='"+ID_SEARCH_FORM+"' method='get' action=\"javascript:true;\" target='_top' style='padding-left:5px; background-color:#dddddd;'>";
            html += "<b>"+DEVICE_TEXT_Search+": </b>";
            html += "<input id='"+ID_SEARCH_TEXT+"' name='"+ID_SEARCH_TEXT+"' class='"+CLASS_SEARCH_INPUT+"' type='text' value='' size='"+SEARCH_TEXT_SIZE+"' onkeypress=\"return searchKeyPressed(event);\" onkeyup=\"return accountSearch();\"/>";
            html += "</form>\n";
        }
        
        /* table */
        html += "<div id='"+ID_DIV_TABLE+"'>\n";
        html += accountGetTableHTML(list, "");
        html += "</div>\n";
            
        /* make selection table visible */
        accountSelectorView.innerHTML = html;
        document.body.appendChild(accountSelectorView);
        accountSelectorVisible = true;
        
        // make table that we just added sortable
        var tableID = document.getElementById(ID_ACCTSELECT_TABLE);
        if (sorttable && tableID) {
            //sorttable.makeSortable(tableID);
        }

    }

    /* override 'onmousedown' */
    accountOld_onmousedown = document.onmousedown;
    document.onmousedown = function(e) {
        if (!e) var e = window.event;
        if (!e) { return false; }
        var targ = e.target? e.target : e.srcElement? e.srcElement : null;
        if (targ && (targ.nodeType == 3)) { targ = targ.parentNode; } // Safari bug?
        if (targ == locElem) {
            return false;
        } else {
            for (;targ && (targ.nodeName != "BODY"); targ = targ.parentNode) {
                if (targ == accountSelectorView)  { return false; }
                if (spanDD != null) { if (targ == spanDD) return false; }
                }
            accountCloseChooser();
            return true;
        }
    };
        
    /* focus on search text area */
    if (AccountChooserEnableSearch) {
        //document.devSearchForm.devSearchText.focus();
        if (acctChooserSearchTextElem) {
            //alert("1) Focusing on Search Text: " + ID_SEARCH_TEXT);
            focusOnSearchText(true);
        } else {
            var searchTextElem = document.getElementById(ID_SEARCH_TEXT);
            if (searchTextElem) {
                //alert("2) Focusing on Search Text: " + ID_SEARCH_TEXT);
                acctChooserSearchTextElem = searchTextElem;
                focusOnSearchText(true);
            } else {
                searchTextElem = document.getElementByName(ID_SEARCH_TEXT);
                if (searchTextElem) {
                    //alert("3) Focusing on Search Text: " + ID_SEARCH_TEXT);
                    acctChooserSearchTextElem = searchTextElem;
                    focusOnSearchText(true);
                } else {
                    alert("Search Text ID not found: " + ID_SEARCH_TEXT);
                }
            }
        }
    }

};

function focusOnSearchText(callback)
{
    if (AccountChooserEnableSearch && acctChooserSearchTextElem) {
        acctChooserSearchTextElem.focus();
        acctChooserSearchTextElem.select();
        if (callback) {
            //setTimeout("focusOnSearchText(false);", 1000);
        } else {
            //alert("Focused on search text input  ...");
        }
    }
};

var searchTableHeaderHtml = null;
function deviceGetTableHTML(list, searchVal)
{
    var idWidth = WIDTH_ID;
    var dsWidth = WIDTH_DESC;
    searchVal = searchVal.toLowerCase();

    /* table header */
    if (searchTableHeaderHtml == null) {
        var h = "";
        // begin table HTML
        h += "<table id='"+ID_ACCTSELECT_TABLE+"' class='"+CLASS_TABLE_COLUMN_SORTABLE+"' cellspacing='0' cellpadding='0' border='1'>\n";
        // table header
        h += "<thead>\n";
        h += "<tr class='"+CLASS_ACCTSELECT_ROW_HEADER+"'>";
        if (AccountChooserIDPosition == IDPOS_NONE) {
            h += "<th nowrap class='"+CLASS_ACCTSELECT_COL_HEADER+"' width='"+dsWidth+"' valign='center'>"+ACCOUNT_TEXT_Description+"</th>";
        } else 
        if (AccountChooserIDPosition == IDPOS_LAST) {
            h += "<th nowrap class='"+CLASS_ACCTSELECT_COL_HEADER+"' width='"+dsWidth+"' valign='center'>"+ACCOUNT_TEXT_Description+"</th>";
            h += "<th nowrap class='"+CLASS_ACCTSELECT_COL_HEADER+"' width='"+idWidth+"' valign='center'>"+ACCOUNT_TEXT_ID+"</th>";
        } else {
            h += "<th nowrap class='"+CLASS_ACCTSELECT_COL_HEADER+"' width='"+idWidth+"' valign='center'>"+ACCOUNT_TEXT_ID+"</th>";
            h += "<th nowrap class='"+CLASS_ACCTSELECT_COL_HEADER+"' width='"+dsWidth+"' valign='center'>"+ACCOUNT_TEXT_Description+"</th>";
        }
        h += "</tr>\n";
        h += "</thead>\n";
        searchTableHeaderHtml = h;
    }
    var html = searchTableHeaderHtml;
    
    /* pre-build TD html */
    var TD_idCell = "<td nowrap class='"+CLASS_ACCTSELECT_COL_DATA+"' width='"+idWidth+"' onclick=\"javascript:accountSelected(";
    var TD_dsCell = "<td nowrap class='"+CLASS_ACCTSELECT_COL_DATA+"' width='"+dsWidth+"' onclick=\"javascript:accountSelected(";

    /* table body */
    html += "<tbody>\n";
    chooserFirstAccountNdx = -1;
    if (!list) { list = []; }
    for (var d = 0; d < list.length; d++) {

        /* omit items not matched */
        if ((searchVal != "") && !list[d].desc.toLowerCase().startsWith(searchVal)) { 
            continue; 
        }
        var idVal = list[d].id;
        var dsVal = escapeText(list[d].desc);
        
        /* save first item */
        if (chooserFirstAccountNdx < 0) {
            chooserFirstAccountNdx = d;
        }

        /* save matched item */
        var selNdx = d;

        /* write html */
        html += "<tr class='" + CLASS_ACCTSELECT_ROW_DATA + "'>";
        if (AccountChooserIDPosition == IDPOS_NONE) {
            html += TD_dsCell + selNdx + ")\">" + dsVal + "</td>";
        } else 
        if (AccountChooserIDPosition == IDPOS_LAST) {
            html += TD_dsCell + selNdx + ")\">" + dsVal + "</td>";
            html += TD_idCell + selNdx + ")\">" + idVal + "</td>";
        } else {
            html += TD_idCell + selNdx + ")\">" + idVal + "</td>";
            html += TD_dsCell + selNdx + ")\">" + dsVal + "</td>";
        }
        html += "</tr>";

    }
    html += "</tbody>\n";

    html += "</table>\n";
    return html;

};

// ----------------------------------------------------------------------------

/* invoked on 'onkeypress' */
function searchKeyPressed(event)
{
    var isCR = isEnterKeyPressed(event);
    if (isCR && (chooserFirstAccountNdx >= 0)) {
        accountSelected(chooserFirstAccountNdx);
        return false;
    }
    return !isCR;
};

function isSearchMatch(searchVal, idVal, dsVal)
{
    if (!searchVal || (searchVal == "")) {
        return true;
    } else
    if (AccountChooserMatchContains) {
        if (dsVal && dsVal.contains(searchVal)) {
            return true;
        } else
        if (idVal && idVal.contains(searchVal)) {
            return true;
        }
    } else {
        if (dsVal && dsVal.startsWith(searchVal)) {
            return true;
        } else
        if (idVal && idVal.startsWith(searchVal)) {
            return true;
        }
    }
    return false;
};

function accountSearch(event)
{
    if (!AccountChooserEnableSearch) { return false; }
    //var searchVal = document.devSearchForm.devSearchText.value.toLowerCase();
    var searchTextElem = document.getElementById(ID_SEARCH_TEXT);
    if (!searchTextElem) { searchTextElem = document.getElementByName(ID_SEARCH_TEXT); }
    var searchVal = (searchTextElem)? searchTextElem.value.toLowerCase() : "";
    var tableDiv = document.getElementById(ID_DIV_TABLE);
    if (tableDiv) {

        /* DOM search through table */
        chooserFirstAccountNdx = -1;
        var tableID = document.getElementById(ID_ACCTSELECT_TABLE);
        if (PREDEFINED_CHOOSER_HTML) {
            
            if (tableID) {
                for (var i = 0; i < tableID.rows.length; i++) {
                    var row    = tableID.rows[i];
                    var idVal  = (AccountChooserIDPosition != IDPOS_NONE)? row.getAttribute("idVal") : null;
                    var dsVal  = row.getAttribute("dsVal"); // description
                    if (dsVal == null) {
                        // skip these records (first record is the header)
                    } else
                    if (isSearchMatch(searchVal,idVal,dsVal)) {
                        if (chooserFirstAccountNdx < 0) {
                            chooserFirstAccountNdx = numParseInt(row.getAttribute("selNdx"),i-1);
                        }
                        //if (row.className != CLASS_DEVSELECT_ROW_DATA) {
                            row.className =  CLASS_ACCTSELECT_ROW_DATA;
                        //}
                        // setting the 'style' directly does not produce the desired results.
                        //row.style.visibility = "visible";
                        //row.style.display    = "inline";
                    } else {
                        //if (row.className != CLASS_DEVSELECT_ROW_HIDDEN) {
                            row.className =  CLASS_ACCTSELECT_ROW_HIDDEN;
                        //}
                        // setting the 'style' directly does not produce the desired results.
                        //row.style.visibility = "collapse";
                        //row.style.display    = "none";
                    }
                }
            } else {
                alert("Table not found: " + ID_ACCTSELECT_TABLE);
            }
            
        } else {
    
            /* html */
            var tableHtml = deviceGetTableHTML(chooserAccountList, searchVal);
            tableDiv.innerHTML = tableHtml;
        
        }

    }
    return !isEnterKeyPressed(event);
};

// ----------------------------------------------------------------------------

function accountCloseChooser()
{
    if (accountSelectorVisible) {
        if (PREDEFINED_CHOOSER_HTML) {
            accountSelectorView.className = CLASS_ACCTSELECT_DIV_HIDDEN;
        } else {
            document.body.removeChild(accountSelectorView);
            accountSelectorView = null;
        }
        document.onmousedown = accountOld_onmousedown;
        accountSelectorVisible = false;
    }
};

// ----------------------------------------------------------------------------
/*
function deviceLoadList(elemNameID, elemNameDesc, deviceListURL) 
{
    try {
        var req = jsmGetXMLHttpRequest();
        if (req) {
            req.open("GET", deviceListURL, true);
            req.setRequestHeader("If-Modified-Since", "Sat, 1 Jan 2000 00:00:00 GMT");
            req.onreadystatechange = function() {
                if (req.readyState == 4) {
                    var data = req.responseText;
                    deviceParseList(elemNameID, elemNameDesc, data);
                } else
                if (req.readyState == 1) {
                    // alert('Loading points from URL: [' + req.readyState + ']\n' + mapURL);
                } else {
                    // alert('Problem loading URL? [' + req.readyState + ']\n' + mapURL);
                }
            }
            req.send(null);
        } else {
            alert("Error [deviceLoadList]:\n" + deviceURL);
        }
    } catch (e) {
        alert("Error [deviceLoadList]:\n" + e);
    }
};

function deviceParseList(elemNameID, elemNameDesc, data) 
{
    // TODO: parse 'data'
    var list = new Array();
    for (var d = 1; d <= 500; d++) {
        var D = new Object();
        D.id = "device_" + d;
        D.desc = "My Device #" + d;
        list.push(D);
    }
    accountShowChooserList(elemNameID, elemNameDesc, list)
};
*/
// ----------------------------------------------------------------------------

function accountSelected(x)
{
    if (x < 0) { return; }
    var tableID = document.getElementById(ID_ACCTSELECT_TABLE);
    if (!tableID) { return; }
    if (x >= tableID.rows.length) { return; }
    var selRow = tableID.rows[x];
    var selNdx = numParseInt(selRow.getAttribute("selNdx"),-1);
    if (x != selNdx) {
        selRow = tableID.rows[x + 1];
        selNdx = numParseInt(selRow.getAttribute("selNdx"),-1);
        if (x != selNdx) {
            alert("Cannot find selected row: " + x);
            return;
        }
    }
    var selID   = selRow.getAttribute("idVal");
    var selDesc = selRow.getAttribute("dsVal");
//    alert("Selected ("+x+") " + selID + " - " +selDesc + "!");

    /*
    var selItem = (chooserAccountList && (x < chooserAccountList.length))? chooserAccountList[x] : null;
    var selID   = selItem.id;
    var selDesc = selItem.desc;
    */

    // set id
    var idElem  = chooserRelativeElementID? document.getElementById(chooserRelativeElementID) : null;
    if (idElem != null) { idElem.value = selID; }
    
    // set description
    var dsElem  = chooserRelativeElementDesc? document.getElementById(chooserRelativeElementDesc) : null;
    if (dsElem != null) { dsElem.value = selDesc; }

    /* device delected */
    accountCloseChooser();
//    deviceDeviceChanged();
    showAccountDevices(selID,ID_TABLE_ACC_DEV);
    
};

// ----------------------------------------------------------------------------
