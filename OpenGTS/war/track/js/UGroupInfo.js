/*
 
 Global variables:
 
 var GroupMembers = Array(); // Array of Device objects
 var AccountDevices = Array(); // Array of Device objects

 Device object:
 
 var Device = Object();
// Device.accountID
// Device.accountDesc
// Device.deviceID
// Device.deviceDesc
// Device.deviceName

 */

//var GroupMembers = Array(); // Array of Device objects
//var AccountDevices = Array(); // Array of Device objects

var separator = "#";
var accountDeviceSelected = -1;
var groupDeviceSelected = -1;

function showGroupMembers()
{

}

function showAccountDevices(acct,tid)
{
	var i,imax;
	var table = document.getElementById(tid);
	var tlen = table.rows.length;
	var dev;
	var row;
	var cellClassName = "adminTableBodyCol";
	var cellSelect;
	var cellAccId;
	var cellAccDesc;
	var cellDevId;
	var cellDevDesc;
	var cellDevName;
	var radioHTML = "<input type='radio' name='device'";
	for(i=1; i<tlen; i++) {
		table.deleteRow(1);
	}
	imax=AccountDevices.length;
	for(i=0; i<imax; i++) {
		dev=AccountDevices[i];
		if(	(typeof dev.accountID=="undefined") || (typeof dev.deviceID=="undefined") ) continue;
		if( (dev.accountID.length==0) || (dev.deviceID.length==0) ) continue;
		if( dev.accountID!=acct ) continue;
		tlen = table.rows.length;
		row = table.insertRow(-1);
		if(tlen%2 == 0) row.className="adminTableBodyRowOdd";
		else row.className="adminTableBodyRowEvn";
		cellSelect = row.insertCell(0);
		cellSelect.className = cellClassName;
		cellSelect.setAttribute("sorttable_customkey", tlen);
		cellSelect.innerHTML = radioHTML + " id='"+dev.accountID+separator+dev.deviceID+"'"
										 + " value='"+dev.accountID+separator+dev.deviceID+"'"
										 + " onclick='javascript:onAccountDeviceRadioClick(this);'"
										 + " >";
		cellAccId = row.insertCell(1);
		cellAccId.className = cellClassName;
		cellAccId.innerHTML = dev.accountID;
		cellAccDesc = row.insertCell(2);
		cellAccDesc.className = cellClassName;
		cellAccDesc.innerHTML = dev.accountDesc;
		cellDevId = row.insertCell(3);
		cellDevId.className = cellClassName;
		cellDevId.innerHTML = dev.deviceID;
		cellDevDesc = row.insertCell(4);
		cellDevDesc.className = cellClassName;
		cellDevDesc.innerHTML = dev.deviceDesc;
		cellDevName = row.insertCell(5);
		cellDevName.className = cellClassName;
		cellDevName.innerHTML = dev.deviceName;
	}
}

function addDeviceToGroup(dev,tid)
{
	if(	(typeof dev.accountID=="undefined") || (typeof dev.deviceID=="undefined") ) return;
	if( (dev.accountID.length==0) || (dev.deviceID.length==0) ) return;
	var i,imax;
	imax=GroupMembers.length;
	for(i=0; i<imax; i++) {
		if( (GroupMembers[i].accountID==dev.accountID) && (GroupMembers[i].deviceID==dev.deviceID) ) return;
	}
	GroupMembers.push(dev);
	var table = document.getElementById(tid);
	var tlen = table.rows.length;
	var row = table.insertRow(-1);
	if(tlen%2 == 0) row.className="groupDevicesDataRowEvn";
	else row.className="groupDevicesDataRowOdd";
	var radioHTML = "<input type='radio' name='device'"
					+ " id='"+dev.accountID+separator+dev.deviceID+"'"
					+ " value='"+dev.accountID+separator+dev.deviceID+"'"
					+ " onclick='javascript:onGroupDeviceRadioClick(this);'"
					+ " >";
	var cellClassName = "adminTableBodyCol";
	var cellSelect = row.insertCell(0);
	cellSelect.className = cellClassName;
	cellSelect.setAttribute("sorttable_customkey", tlen);
	cellSelect.innerHTML = radioHTML;
	var cellAccId = row.insertCell(1);
	cellAccId.className = cellClassName;
	cellAccId.innerHTML = dev.accountID;
	var cellAccDesc = row.insertCell(2);
	cellAccDesc.className = cellClassName;
	cellAccDesc.innerHTML = dev.accountDesc;
	var cellDevId = row.insertCell(3);
	cellDevId.className = cellClassName;
	cellDevId.innerHTML = dev.deviceID;
	var cellDevDesc = row.insertCell(4);
	cellDevDesc.className = cellClassName;
	cellDevDesc.innerHTML = dev.deviceDesc;
	var cellDevName = row.insertCell(5);
	cellDevName.className = cellClassName;
	cellDevName.innerHTML = dev.deviceName;
}

function removeDeviceFromGroup(dev,tid)
{
	var i,imax;
	if(	(typeof dev.accountID=="undefined") || (typeof dev.deviceID=="undefined") ) return;
	if( (dev.accountID.length==0) || (dev.deviceID.length==0) ) return; 
	imax=GroupMembers.length;
	for(i=0; i<imax; i++) {
		if( (GroupMembers[i].accountID==dev.accountID) && (GroupMembers[i].deviceID==dev.deviceID) ) {
			GroupMembers.splice(i,1);
			break;
		}
	}
	var radioId = dev.accountID+separator+dev.deviceID;
	var radio = document.getElementById(radioId);
	var selindex = radio.parentNode.getAttribute("sorttable_customkey");	// add header record
	var curindex = -1;
	var table = document.getElementById(tid);
	imax=table.rows.length;
	for(i=1; i<imax; i++) {
		curindex=table.rows[i].cells[0].getAttribute("sorttable_customkey");
		if(curindex==selindex) {
			table.deleteRow(i);
			break;
		} 
	}
}

function onAddButtonClick(atid,gtid)
{
	var i,imax,index;
	var table = document.getElementById(atid);
	var dev = new Object();
	imax=table.rows.length;
	for(i=1; i<imax; i++) {
		index=table.rows[i].cells[0].getAttribute("sorttable_customkey");
		if(index==accountDeviceSelected) {
			dev.accountID=table.rows[i].cells[1].innerHTML;
			dev.accountDesc=table.rows[i].cells[2].innerHTML;
			dev.deviceID=table.rows[i].cells[3].innerHTML;
			dev.deviceDesc=table.rows[i].cells[4].innerHTML;
			dev.deviceName=table.rows[i].cells[5].innerHTML;
			addDeviceToGroup(dev,gtid)
		}
	}
}

function onRemoveButtonClick(atid,gtid)
{
	var i,imax,index;
	var table = document.getElementById(gtid);
	var dev = new Object();	
	imax=table.rows.length;
	for(i=1; i<imax; i++) {
		index=table.rows[i].cells[0].getAttribute("sorttable_customkey");
		if(index==groupDeviceSelected) {
			dev.accountID=table.rows[i].cells[1].innerHTML;
			dev.accountDesc=table.rows[i].cells[2].innerHTML;
			dev.deviceID=table.rows[i].cells[3].innerHTML;
			dev.deviceDesc=table.rows[i].cells[4].innerHTML;
			dev.deviceName=table.rows[i].cells[5].innerHTML;
			removeDeviceFromGroup(dev,gtid)
		}
	}
}

function onAccountDeviceRadioClick(radio)
{
	var index = radio.parentNode.getAttribute("sorttable_customkey");
	accountDeviceSelected = index;
}

function onGroupDeviceRadioClick(radio)
{
	var index = radio.parentNode.getAttribute("sorttable_customkey");
	groupDeviceSelected = index;
}

function onGroupEditFormSubmit()
{
	var members_input = document.getElementById(PARM_GROUP_MEMBERS);
	if(members_input!=null) members_input.value=JSON.stringify(GroupMembers);
}