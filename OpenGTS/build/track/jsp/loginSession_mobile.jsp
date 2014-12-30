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
<!-- custom/loginSession_mobile.jsp: <gts:var>${version} [${privateLabelName}] page=${pageName}</gts:var> 
  =======================================================================================
  Copyright(C) 2007-2014 GeoTelematic Solutions, Inc., All rights reserved
  Centered image banner
  =======================================================================================
-->
<head>

  <!-- meta -->
  <gts:var>
  <meta name="author" content="GeoTelematic Solutions, Inc."/>
  <meta http-equiv="content-type" content='text/html; charset=UTF-8'/>
  <meta http-equiv="cache-control" content='no-cache'/>
  <meta http-equiv="pragma" content="no-cache"/>
  <meta http-equiv="expires" content="0"/>
  <meta name="copyright" content="${copyright}"/>
  <meta name="robots" content="none"/>
  </gts:var>

  <!-- page title -->
  <gts:var>
  <title>${pageTitle}</title>
  </gts:var>

  <!-- default style -->
  <link rel='stylesheet' type='text/css' href='css/General.css'/>
  <!-- link rel='stylesheet' type='text/css' href='css/MenuBar.css'/ -->
  <link rel='stylesheet' type='text/css' href='css/Controls.css'/>

  <!-- custom overrides style -->
  <link rel='stylesheet' type='text/css' href='custom/General.css'/>
  <!-- link rel='stylesheet' type='text/css' href='custom/MenuBar.css'/ -->
  <link rel='stylesheet' type='text/css' href='custom/Controls.css'/>

  <!-- javascript -->
  <gts:track section="javascript"/>

  <!-- custom javascript -->
  <script src="./custom/custom.js" type="text/javascript"></script>

  <!-- local style -->
  <style type="text/css">
    BODY { 
        font-family: arial,verdana,sans-serif; 
        background-color: <gts:var>${Background.color=#F9F9FF}</gts:var>; 
        background-image: url(<gts:var>${Background.image}</gts:var>);
        background-position: <gts:var>${Background.position=center top}</gts:var>; /* 0% 0% | [left|right|center] [top|center|bottom] */
        background-repeat: <gts:var>${Background.repeat=repeat}</gts:var>;
    }
  </style>

  <!-- page specific style -->
  <gts:track section="stylesheet"/>

  <!-- custom override style -->
  <link rel='stylesheet' type='text/css' href='custom/Custom.css'/>

  <gts:var ifKey="ContentCell.color" compare="ne" value="">
  <!-- overriding 'contentCell' background color -->
  <style type="text/css">
    .contentTable { /* Controls.css */
        width: 100%;
        height: 95%; 
        max-height: 100%;
        margin-top: 3px;
    }
    .contentCell {
        background-color: ${ContentCell.color=#FBFBFB};
    }
    .contentTopMenuCell {
        background-color: ${ContentCell.color=#FBFBFB};
    }
    .contentTrackMapCell {
        background-color: ${ContentCell.color=#FBFBFB};
    }
    #iconMenu TR.menuGroupTitle {
        background-color: ${IconMenu.groupTitle.color=${ContentCell.color=#FBFBFB}};
    }
  </style>
  </gts:var>

</head>

<!-- ======================================================================================= -->

<body onload="<gts:track section='body.onload'/>" onunload="<gts:track section='body.onunload'/>">

<!-- table width="<gts:track section='banner.width' default='860'/>" height="100%" align="center" border="0" cellspacing="0" cellpadding="0" -->
<table width="100%" height="100%" align="left" border="0" cellspacing="0" cellpadding="0">
<tbody>

  <!-- Begin Page header/navigation ======================================== -->
  <tr id="TableRow_Navigation">
  <td align="center">
     <!-- table width="100%" border="0" cellpadding="0" cellspacing="0" -->
     <table width="100%" border="0" cellpadding="0" cellspacing="0">
     <tbody>
     <tr>
       <td class="navBarClear" nowrap align="left">&nbsp;<gts:var ifTrue="isLoggedIn"><i>${i18n.Account}:</i> ${accountDesc} (${userDesc})</gts:var></td>
       <td class="navBarClear" nowrap align="right" width="100%"><gts:var>&nbsp;${navigation}&nbsp;&nbsp;</gts:var></td>
     </tr>
     </tbody>
     </table>
  </td>
  </tr>
  <!-- End Page header/navigation ======================================== -->

  <!-- Begin Page contents ======================================== -->
  <tr id="TableRow_Content" height="100%">
  <td>
    <table width="100%" height="100%" border="0" cellpadding="0" cellspacing="0">
    <tbody>
    <tr>
        <td valign='top' align='center' height='100%'>
           <table class="<gts:track section='content.class.table'/>" cellspacing='0' cellpadding='0' border='0'>
           <tbody>
              <tr height='100%'>
                <td class="<gts:track section='content.class.cell'/>">
                    <gts:track section="content.body"/>
                </td>
              </tr>
              <tr>
                <td id="<gts:track section='content.id.message'/>" class="<gts:track section='content.class.message'/>">
                    <gts:track section="content.message"/>
                </td>
              </tr>
           </tbody>
           </table>
        </td>
    </tr>
    </tbody>
    </table>
  </td>
  </tr>
  <!-- End Page contents ======================================== -->

</tbody>
</table>
</body>

<!-- ======================================================================================= -->

</html>
