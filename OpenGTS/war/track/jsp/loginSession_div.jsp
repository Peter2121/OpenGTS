<%@ taglib uri="./Track" prefix="gts" %>
<%@ page isELIgnored="true" contentType="text/html; charset=UTF-8" %>
<%
//response.setContentType("text/html; charset=UTF-8");
//response.setCharacterEncoding("UTF-8");
response.setHeader("CACHE-CONTROL", "NO-CACHE");
response.setHeader("PRAGMA"       , "NO-CACHE");
response.setHeader("P3P"          , "CP='IDC DSP COR ADM DEVi TAIi PSA PSD IVAi IVDi CONi HIS OUR IND CNT'");
response.setDateHeader("EXPIRES"  , 0         );
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<!-- <!DOCTYPE HTML PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"> -->
<html xmlns='http://www.w3.org/1999/xhtml' xmlns:v='urn:schemas-microsoft-com:vml'>
<!-- jsp/loginSession_div.jsp: <gts:var>${version} [${privateLabelName}]</gts:var>
  =======================================================================================
  Copyright(C) 2007-2014 GeoTelematic Solutions, Inc., All rights reserved
  Project: OpenGTS - Open GPS Tracking System [http://www.opengts.org]
  =======================================================================================
-->
<gts:var ifKey="notDefined" value="true">
<!--
  See "logSession.jsp" for additional notes
  =======================================================================================
  Change History:
   2010/01/28  Martin D. Flynn
      -Initial Release
  =======================================================================================
-->
</gts:var>

<!-- Head -->
<head>

  <!-- meta -->
  <gts:var>
  <meta name="author" content="FlyTrace"/>
  <meta http-equiv="content-type" content='text/html; charset=UTF-8'/>
  <meta http-equiv="cache-control" content='no-cache'/>
  <meta http-equiv="pragma" content="no-cache"/>
  <meta http-equiv="expires" content="0"/>
  <meta name="copyright" content="${copyright}"/>
  <meta name="robots" content="none"/>
  <meta name="viewport" content="width=device-width, initial-scale=1"/>
  </gts:var>

  <!-- page title -->
  <gts:var>
  <title>${pageTitle}</title>
  </gts:var>

  <!-- default style -->
  <link rel='stylesheet' type='text/css' href='css/General.css'/>
  <link rel='stylesheet' type='text/css' href='css/MenuBar.css'/>
  <link rel='stylesheet' type='text/css' href='css/Controls.css'/>

  <!-- custom overrides style -->
  <link rel='stylesheet' type='text/css' href='custom/General.css'/>
  <link rel='stylesheet' type='text/css' href='custom/MenuBar.css'/>
  <link rel='stylesheet' type='text/css' href='custom/Controls.css'/>

  <!-- javascript -->
  <gts:track section="javascript"/>

  <!-- local style -->
  <style type="text/css">
    BODY { 
<gts:var if="isLocaleRTL" value="true">
        direction: RTL;
</gts:var>
    }
    TD.titleText {
        text-align: center;
    }
  </style>

  <!-- page specific style -->
  <gts:track section="stylesheet"/>

  <!-- custom override style -->
  <link rel='stylesheet' type='text/css' href='custom/Custom.css'/>
  <script type="text/javascript">
		function setContentDivPos() {
			var divObj = document.getElementById('content');
			var divHeight = divObj.clientHeight;
			var divWidth = divObj.clientWidth; 
			var pageWidth = window.innerWidth;
			var pageHeight = window.innerHeight;
			var paddingRight = (pageWidth-divWidth)/2;
			var paddingBottom = 50;
			var divLeft = pageWidth-divWidth-paddingRight;
			var divTop = pageHeight-divHeight-paddingBottom;
			if(divLeft<0) divLeft=0;
			if(divTop<0) divTop=0;
			divObj.style.left=divLeft;
			divObj.style.top=divTop;		
		}
		window.addEventListener("load",setContentDivPos,false);
		window.addEventListener("resize",setContentDivPos,false);
  </script>
</head>

<!-- ======================================================================================= -->

<body class=bg onload="<gts:track section='body.onload'/>" onunload="<gts:track section='body.onunload'/>">

<div id='content'>
<table class="<gts:track section='content.class.table'/>" width="100%" height="100%" align="center" border="0" cellspacing="0" cellpadding="0" style="padding-top: 5px;">
  <tr height="100%">
  <td class="<gts:track section='content.class.cell'/>">
  	<gts:track section="content.body"/>
  </td>
  </tr>
  <tr>
    <td id="<gts:track section='content.id.message'/>" class="<gts:track section='content.class.message'/>">
        <gts:track section="content.message"/>
    </td>
  </tr>
</table>
</div>
<table width="100%" height="100%" align="center" border="0" cellspacing="0" cellpadding="0" style="padding-top: 5px;">
<tbody>

  <!-- Begin Page contents ======================================== -->
  <tr height="100%">
  <td>
	&nbsp;
  </td>
  </tr>
  <!-- End Page contents ======================================== -->

  <!-- Begin Page footer ======================================== -->
  <tr>
    <td style="font-size: 7pt; border-bottom: 1px solid #888888;">&nbsp;</td>
  </tr>
  <tr>
  <td>
    <table class="copyrightFooterClear" width="100%" border="0" cellpadding="0" cellspacing="0">
    <tbody>
    <tr>
      <td style="padding: 0px 0px 2px 5px;">&nbsp;</td>
      <td width="100%">
         &nbsp;
         <span class="copyrightText"><gts:var>${copyright}</gts:var></span>
      </td>
    </tr>
    </tbody>
    </table>
  </td>
  </tr>
  <!-- End Page footer ======================================== -->

</tbody>
</table>
</body>

<!-- ======================================================================================= -->

</html>
