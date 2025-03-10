<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants,
			org.openelisglobal.common.util.ConfigurationProperties,
			org.owasp.encoder.Encode,
			org.openelisglobal.common.util.Versioning"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>


<html>
<head>
<link rel="stylesheet" href="fontawesome-free-5.13.1-web/css/fontawesome.min.css">
<link href="fontawesome-free-5.13.1-web/css/solid.css" rel="stylesheet">
	<link rel="apple-touch-icon" sizes="180x180" href="favicon/apple-touch-icon.png">
	<link rel="icon" type="image/png" sizes="32x32" href="favicon/favicon-32x32.png">
	<link rel="icon" type="image/png" sizes="16x16" href="favicon/favicon-16x16.png">
	<link rel="manifest" href="favicon/site.webmanifest">
	<link rel="mask-icon" href="favicon/safari-pinned-tab.svg" color="#5bbad5">
	<link rel="shortcut icon" href="favicon/favicon.ico">
	<meta name="apple-mobile-web-app-title" content="OpenELIS Global">
	<meta name="application-name" content="OpenELIS Global">
	<meta name="msapplication-TileColor" content="#2d89ef">
	<meta name="msapplication-config" content="favicon/browserconfig.xml">
	<meta name="theme-color" content="#ffffff">
	<meta http-equiv='Content-Type' content='text/html; charset=UTF-8' />

<link rel="stylesheet" media="screen" type="text/css"
	href="css/openElisCore.css?" />
<script type="text/javascript"
	src="scripts/jquery-1.8.0.min.js?"></script>
<script type="text/javascript"
	src="scripts/jquery.dataTables.min.js?"></script>
<script type="text/javascript"
	src="scripts/bootstrap.min.js?"></script>
<script type="text/javascript">
var  jQuery = jQuery.noConflict();
</script>
<script
	src="scripts/additional_utilities.js"></script>
<script type="text/javascript"
	src="scripts/prototype-1.5.1.js?"></script>
<script type="text/javascript"
	src="scripts/scriptaculous.js?"></script>
<script type="text/javascript"
	src="scripts/overlibmws.js?"></script>
<script type="text/javascript"
	src="scripts/ajaxtags-1.2.js?"></script>
<script type="text/javascript"
	src="scripts/Tooltip-0.6.0.js?"></script>
<script type="text/javascript"
	src="scripts/lightbox.js?"></script>
	
<script>
// works with values set in BaseForm.java
function cancelAction() {
	// if page has its own cancel function, call it instead 
	if (typeof myCancelAction === "function") {
		myCancelAction();
	}
	redirect = "${form.cancelAction}";
	method =  "${form.cancelMethod}";
	if (redirect == "null" || redirect == "") {
		redirect = "Home";
	}
	window.location = redirect;
	
}

function navigationAction(form, action, validate, parameters) {
	var context = '<%= request.getContextPath() %>';
	var formName = form.name;
	//alert("form name " + formName);
	var parsedFormName = formName.substring(1, formName.length - 4);
	parsedFormName = formName.substring(0,1).toUpperCase() + parsedFormName;
    //alert("parsedFormName " + parsedFormName);

    var idParameter = '<%= Encode.forJavaScript((String)request.getParameter("ID")) %>';
    var startingRecNoParameter = '<%= Encode.forJavaScript((String)request.getParameter("startingRecNo")) %>';
    //alert("This is idParameter " + idParameter);
    if (!idParameter) {
       idParameter = '0';
    }

    if (!startingRecNoParameter) {
       startingRecNoParameter = '1';
    }

    if (parameters != '') {
	   parameters = parameters + idParameter;
	} else {
	   parameters = parameters + "?ID=" + idParameter;
	}
    parameters = parameters + "&startingRecNo=" + startingRecNoParameter;
    
    window.location.href = context + '/' + action + parsedFormName + ""  + parameters ;
}

function setAction(form, action, validate, parameters, method) {
    var sessionid = getSessionFromURL(form.action);
	var context = '<%= request.getContextPath() %>';
	var formName = form.name;
	//alert("form name " + formName);
	var parsedFormName = formName.substring(1, formName.length - 4);
	parsedFormName = formName.substring(0,1).toUpperCase() + parsedFormName;
    //alert("parsedFormName " + parsedFormName);

    var idParameter = '<%= Encode.forJavaScript((String)request.getParameter("ID")) %>';
    var startingRecNoParameter = '<%= Encode.forJavaScript((String)request.getParameter("startingRecNo")) %>';
    //alert("This is idParameter " + idParameter);
    if (!idParameter) {
       idParameter = '0';
    }

    if (!startingRecNoParameter) {
       startingRecNoParameter = '1';
    }

    if (parameters != '') {
	   parameters = parameters + idParameter;
	} else {
	   parameters = parameters + "?ID=" + idParameter;
	}
    parameters = parameters + "&startingRecNo=" + startingRecNoParameter;


	form.action = context + '/' + action + parsedFormName + ""  + sessionid + parameters ;
	if (method != null && method != "") {
		form.method = method;
	}
	form.validateDocument = new Object();
	form.validateDocument.value = validate;
	//alert("Going to validatedAnDsubmitForm this is action " + form.action);
	validateAndSubmitForm(form);

}

//default behavior -- overide for behavior
function /*boolean*/ handleEnterEvent(){
	return true;
}

function enterIntercepter(e) {
	var code = e ? e.which : window.event.keyCode;
	if( code == 13){
		return handleEnterEvent()
	}
	
	return true
}
document.onkeypress = enterIntercepter;
if (document.layers) {
	document.captureEvents(Event.KEYPRESS);
}
</script>
<%
	if (request.getAttribute("cache") != null && request.getAttribute("cache").toString().equals("false"))
	{
%>
<meta http-equiv="Cache-Control"
	content="no-cache, no-store, proxy-revalidate, must-revalidate" />
<%-- HTTP 1.1 --%>
<meta http-equiv="Pragma" content="no-cache" />
<%-- HTTP 1.0 --%>
<meta http-equiv="Expires" content="0" />
<%
	}
%>

<title>
		<c:out value="${title}" />
</title>
<jsp:include page="${bannerFragment}"/>
<jsp:include page="${loginHeaderFragment}"/>
</head>

<%-- check_width()--%>
<body onLoad="focusOnFirstInputField();check_width();onLoad()">

	<%-- for optimistic locking--%>
	<table cellpadding="0" cellspacing="1" width="100%">
		<tr>
			<td><jsp:include page="${errorFragment}"/></td>
		</tr>
		<tr>
			<td><jsp:include page="${headerFragment}"/></td>
		</tr>
		<tr>
			<td><jsp:include page="${preSelectionHeaderFragment}"/></td>
		</tr>
		<tr>
			<td><jsp:include page="${bodyFragment}"/></td>
		</tr>
		<tr>
			<td><jsp:include page="${footerFragment}"/></td>
		</tr>
	</table>

</body>



</html>

