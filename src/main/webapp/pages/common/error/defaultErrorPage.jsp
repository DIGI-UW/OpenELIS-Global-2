<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.*"%>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<!DOCTYPE html>
<html>
<head>
	<link rel="icon" href="images/favicon.ico" type="image/x-icon">
	<link rel="shortcut icon" href="images/favicon.ico" type="image/x-icon">
    <title><spring:message code="errors.unhandled.title"/></title>   
    <meta http-equiv='Content-Type' content='text/html; charset=UTF-8' />
    <meta http-equiv="pragma" content="no-cache">
    <meta http-equiv="cache-control" content="no-cache">
    <meta http-equiv="expires" content="0">    
    <meta http-equiv="description" content="Default error page">
    <link rel="stylesheet" type="text/css" href="css/bootstrap.css" />
    <script type="text/javascript" src="scripts/jquery-1.8.0.min.js"></script>
        
    <%-- Inline css --%> 
    <style type="text/css">
    #header {
        margin-bottom: 1em;
        min-width: 970px;
        margin-bottom: 10px;
        padding: 10px 0px 0px 15px;
        height: 88px;
        overflow: visible;
        background: #486086; /* Old browsers */
        background: -moz-linear-gradient(top, #486086 0%, #557aaf 100%); /* FF3.6+ */
        background: -webkit-gradient(linear, left top, left bottom, color-stop(0%,#486086), color-stop(100%,#557aaf)); /* Chrome,Safari4+ */
        background: -webkit-linear-gradient(top, #486086 0%,#557aaf 100%); /* Chrome10+,Safari5.1+ */
        background: -o-linear-gradient(top, #486086 0%,#557aaf 100%); /* Opera11.10+ */
        background: -ms-linear-gradient(top, #486086 0%,#557aaf 100%); /* IE10+ */
        filter: progid:DXImageTransform.Microsoft.gradient( startColorstr='#486086', endColorstr='#557aaf',GradientType=0 ); /* IE6-9 */
        background: linear-gradient(top, #486086 0%,#557aaf 100%); /* W3C */
    }
    #oe-logo {
        float: left;
        margin: 0 15px 0 0
    }
	#oe-logo-img {
	    height: 71px;
	    width: auto;
	}
    #oe-title {
        font-size: 24px;
        line-height: 32px;
        color: #fff;
        font-weight: bold
    }
    .troubleshooting {
        font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
        font-size: 12px;
    }
    </style>
</head>
  
<body id="defaultErrorPage">

<div id="header">
    <div id="oe-logo"><img id="oe-logo-img" src="images/openelis_logo.png" title="OpenELIS" alt="OpenELIS"></div>
    <div id="oe-title"><spring:message code="homePage.heading"/></div>
</div>
      
<div class="container">

    <h1><spring:message code="errorpage.title"/></h1>
    <br />

    <div class="well">	
        <div class="row">
            <div class="span2">
                <img src="images/icon-exclamation.png"  title="<spring:message code="errorpage.title"/>" alt="<spring:message code="errorpage.title"/>" />
            </div>
            <div class="span9">
                <h3><spring:message code="errorpage.head1"/></h3> 

                <p><spring:message code="errorpage.para1"/></p>

                <ul>
                    <li><spring:message code="errorpage.li1"/></li>
                    <li><spring:message code="errorpage.li2"/></li>
                    <li><spring:message code="errorpage.li3"/>
                        <ul>
                            <li><spring:message code="errorpage.li3a"/></li>
                            <li><spring:message code="errorpage.li3b"/></li>
                        </ul>
                    </li>
                    <li><spring:message code="errorpage.li4"/></li>
                </ul>

                <p><spring:message code="errorpage.para2"/></p>

                <br />
                <button class="btn" onclick="history.go( -1 );return true;"><i class="icon-chevron-left"></i> <spring:message code="errorpage.previous.button"/></button>
                <br /><br />

                <h3><spring:message code="errorpage.head2"/></h3>
                <p><spring:message code="errorpage.para3"/></p>
                <Br />
                <div class="troubleshooting">
                    <p><spring:message code="errorpage.info.time"/> <span id="error-time"> </span></p>

                    <p><spring:message code="errorpage.info.page"/><br />
                    <span id="error-path"> </span></p>

                    <p id="previous-path"></p>

                    <div><spring:message code="errorpage.info.browser"/></div>
                    <ul id="browser-details"></ul>
                </div>

            </div>
        </div>
    </div>	

</div>
    
<script type="text/javascript">

function isValidUrl (urlString) {
    try { 
    	return Boolean(new URL(urlString)); 
    }
    catch(e){ 
    	return false; 
    }
}

$(document).ready(function () {
    // jquery functions for details on error
    // Browser details not localized, but probably ok.
    var myDate = new Date().toDateString() + " " + new Date().toTimeString();
    $("#error-time").html(myDate);
    var browserDetails = "<li>Version: " + navigator.appVersion + "</li>";
    browserDetails += "<li>Cookies Enabled: " + navigator.cookieEnabled + "</li>";
    browserDetails += "<li>Platform: " + navigator.platform + "</li>";
    browserDetails += "<li>User-agent header: " + navigator.userAgent + "</li>";
    $("#browser-details").html(browserDetails);
    var pathname = location.href;
    $("#error-path").text(pathname);
//     ensure it is a url to prevent xss
    if (isValidUrl(document.referrer)) {
    	var previousPath = document.getElementById('previous-path');
    	previousPath.appendChild(document.createTextNode('<spring:message code="errorpage.previous"/>'));
    	previousPath.appendChild(document.createElement('br'));
    	previousPath.appendChild(document.createTextNode(document.referrer));
    }
});	
</script>
    
</body>
</html>


