<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.*,
		 		 org.owasp.encoder.Encode" %>

<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>

<!DOCTYPE html>
<html>
<head>
<head>
	<link rel="icon" href="images/favicon.ico" type="image/x-icon">
	<link rel="shortcut icon" href="images/favicon.ico" type="image/x-icon">
    <title><spring:message code="errors.unhandled.title" text="errors.unhandled.title" /> </title>   
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
    <div id="oe-title"><spring:message code="homePage.heading" text="homePage.heading"/></div>
</div>
      
<div class="container">

    <h1><spring:message code="errorpage.404.title" text="errorpage.404.title"/></h1>
    <br />

    <div class="well">	
        <div class="row">
            <div class="span2">
                <img src="images/icon-exclamation-warning.png"  title="<spring:message code='errorpage.404.title' text='errorpage.404.title'/>" alt="<spring:message code='errorpage.404.title' text='errorpage.404.title'/>" />
            </div>
            <div class="span9">
            	<h3><spring:message code="errorpage.404.lead" text="errorpage.404.lead"/></h3>
                
                <p class="troubleshooting" id="error-path"> </p>
				
				<p id="previous-path"> </p>

                <p><spring:message code="errorpage.404.instructions" text="errorpage.404.instructions"/></p>

                
                <br />
                <button class="btn" onclick="history.go( -1 );return true;"><i class="icon-chevron-left"></i> <spring:message code="errorpage.previous.button"/></button>
                <a href="Dashboard" class="btn" style="margin-left: 20px"><i class="icon-home"></i> <spring:message code="errorpage.home.button"/></a>
                <br /><br />

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
    // Currently not localized.
    $("#error-path").text(location.href);

	//  ensure it is a url to prevent xss
	if (isValidUrl(document.referrer)) {
	     var referrerMsg = '<spring:message code="errorpage.previous"/><br />' + document.referrer;
	     $("#previous-path").html(referrerMsg);
	}
});	
</script>
    
</body>
</html>


