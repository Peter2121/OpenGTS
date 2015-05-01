package org.opengts.war.tools;

import java.io.File;
import java.text.MessageFormat;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.geocoder.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

public class TrackerConfig {

    public static final String	PROP_Template_filename	= "trackerConfig.tmplFileName";
    public static final String	PROP_Template_nParams	= "trackerConfig.nParams";
    public static final String	PROP_Template_Param		= "trackerConfig.param";

    public static final String	PROP_Param_RTKey		= "RTKey";
    public static final String	PROP_Param_Database		= "DB";
    
	private String tmplFileName;
	private String tmplFileNameDir;
	private String tmpl;

	private int nParams;
	private String[] Params;
	private String confData;
	
	private String deviceID;
	private String accountID;
	private String userID;
	
	private boolean initialized;
	
	public TrackerConfig(String account, String user, String device) {
		
		initialized = false;
		
		accountID = account;
		userID = user;
		deviceID = device;
		
		tmplFileName = RTConfig.getString(PROP_Template_filename, null);
		nParams = RTConfig.getInt(PROP_Template_nParams, 0);
		if( (tmplFileName==null) || (nParams==0) ) return;
		
		File configDir = RTConfig.getLoadedConfigDir();
		tmplFileNameDir = configDir.toString();
		byte tmplData[] = FileTools.readFile(tmplFileNameDir+tmplFileName);
        if (ListTools.isEmpty(tmplData)) return;
        
        if (!getParams()) return;
		
        MessageFormat mf = new MessageFormat(tmplData.toString());
		StringBuffer sb = mf.format(Params, new StringBuffer(), null);
		confData = sb.toString();
		initialized = true;
	}
	
	private boolean getParams() {
		
		String paramName;
		String paramValue;
		String paramPrefix;
		String db[];
		String dbTable;
		String dbField;
		
		Params = new String[nParams];
		
		for(int i=0; i<nParams;i++) {
			paramName = PROP_Template_Param + "." + String.valueOf(i);
			paramValue = RTConfig.getString(paramName, "");
			if(paramValue.length() < 2) { Params[i]=""; continue; }
			paramPrefix = paramValue.split("\\.", 1)[0]; 
			switch (paramPrefix) {
			case PROP_Param_RTKey:
				Params[i]=RTConfig.getString(paramValue.split("\\.", 2)[1], "");
				break;
			case PROP_Param_Database:
				db=paramValue.split("\\.", 3);
				dbTable=db[1];
				dbField=db[2];
				switch (dbTable) {
				case "Device":
					DeviceRecord dr = new DeviceRecord();	//**** need something to point to current device
					dr.setDeviceID(deviceID);
					Params[i] = dr.getFieldValue(dbField).toString();
					break;
				case "Account":
					AccountRecord ar = new AccountRecord();	//**** need something to point to current account
					ar.SetCurrentAccount(accountID);
					Params[i] = ar.getFieldValue(dbField).toString();
					break;
				default:
					Params[i]="";
				}
			default:
				Params[i]="";
			}
		}
		return true;
	}
	
	public boolean Send(String email) {
		return true;
	}
	
	public boolean isInitialized() {
		return initialized;
	}
		
}
