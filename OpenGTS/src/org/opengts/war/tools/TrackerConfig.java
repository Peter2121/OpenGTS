package org.opengts.war.tools;

import java.io.File;
import java.nio.charset.Charset;
import java.text.MessageFormat;

import org.opengts.util.*;
import org.opengts.util.SendMail.*;
import org.opengts.war.track.EMail;
import org.opengts.dbtools.*;
import org.opengts.geocoder.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

public class TrackerConfig {

    public static final String	PROP_Template_filename		= "trackerConfig.tmplFileName";
    public static final String	PROP_Config_filename		= "trackerConfig.confFileName";
    public static final String	PROP_Config_filename_mime	= "trackerConfig.confFileNameMime";
//    public static final String	PROP_Config_mail_subj		= "trackerConfig.mailSubj";
    public static final String	PROP_Template_nParams		= "trackerConfig.nParams";
    public static final String	PROP_Template_Param			= "trackerConfig.param";

    public static final String	PROP_Param_RTKey			= "RTKey";
    public static final String	PROP_Param_Database			= "DB";
    
	private String tmplFileName;
	private String tmplFileNameDir;
	private String tmpl;

	private int nParams;
	private String[] Params;
	private String confData;
	private String confFileName;
	private String confFileNameMime;
//	private String confMailSubj;
	
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
		
		confFileName = RTConfig.getString(PROP_Config_filename, null);
		confFileNameMime = RTConfig.getString(PROP_Config_filename_mime, null);
		if( (confFileName==null) || (confFileNameMime==null) ) return;

//		confMailSubj = RTConfig.getString(PROP_Config_mail_subj, "");
		
		File configDir = RTConfig.getLoadedConfigDir();
		tmplFileNameDir = configDir.toString();
		byte tmplData[] = FileTools.readFile(tmplFileNameDir+File.separator+tmplFileName);
        if (ListTools.isEmpty(tmplData)) return;
        
        if (!getParams()) return;
		
        MessageFormat mf = new MessageFormat(new String(tmplData));
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
			paramPrefix = StringTools.split(paramValue, '.')[0]; 
			switch (paramPrefix) {
			case PROP_Param_RTKey:
				Params[i]=RTConfig.getString(StringTools.split(paramValue,'.')[1], "");
				break;
			case PROP_Param_Database:
				db=StringTools.split(paramValue,'.');
				dbTable=db[1];
				dbField=db[2];
				switch (dbTable) {
				case "Device":
			        try {
			        	Params[i] = "";
			        	Device.Key devKey = new Device.Key(accountID, deviceID);
			        	if (!devKey.exists()) break;
			        	Device dev = devKey.getDBRecord(true);
//			        	dev.reload();
			        	Params[i] = dev.getFieldValue(dbField, "");
			        	break;
	                } catch (DBException dbe) {
	                	Print.logException("Getting Device data for TrackerConfig", dbe);
	                	break;
	                }
				case "Account":
					try {
						Params[i] = "";
		        		Account.Key accKey = new Account.Key(accountID);
		        		if (!accKey.exists()) break;
		        		Account acc = accKey.getDBRecord(true);
//		        		acc.reload();
		        		Params[i] = acc.getFieldValue(dbField, "");
						break;
					} catch (DBException dbe) {
                		Print.logException("Getting Account data for TrackerConfig", dbe);
                		break;
                	}
				default:
					Params[i]="";
				}
				break;
			default:
				Params[i]="";
			}
		}
		return true;
	}
	
	public boolean Send(String srcEmailAddr, String destEmailAddr, String subj, String body, SendMail.SmtpProperties smtpProps) {
		
		byte[] byteAtt;
        String cc   = null;
        String bcc  = null;
		byteAtt=confData.getBytes(Charset.forName(StringTools.CharEncoding_UTF_8));
		SendMail.Attachment att = new SendMail.Attachment(byteAtt, confFileName, confFileNameMime);

		return EMail.send(srcEmailAddr, destEmailAddr, cc, bcc, subj, body, smtpProps, att);
	}
	
	public boolean isInitialized() {
		return initialized;
	}
		
}
