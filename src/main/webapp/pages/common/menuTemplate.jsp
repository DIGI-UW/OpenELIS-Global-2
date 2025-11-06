<!DOCTYPE html>
<%@ page language="java"
	contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants,
			org.openelisglobal.common.util.ConfigurationProperties,
			org.owasp.encoder.Encode,
			org.openelisglobal.common.util.Versioning" %>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
 

<html>
<%
String form = (String)request.getAttribute(IActionConstants.FORM_NAME);

if (form == null) {
	form = "n/a";
}

  int startingRecNo = 1;
   
  if (request.getAttribute("startingRecNo") != null) {
       startingRecNo = Integer.parseInt((String)request.getAttribute("startingRecNo"));
  }
   request.setAttribute("ctx", request.getContextPath());  
    
%>
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
	
<link rel="stylesheet" type="text/css" href="css/openElisCore.css?" />
<script type="text/javascript" src="scripts/jquery-1.8.0.min.js?"></script>
<script type="text/javascript" src="scripts/jquery.dataTables.min.js?"></script>
<script type="text/javascript" src="scripts/bootstrap.min.js?"></script>
<script type="text/javascript" src="scripts/tabs.js"></script> 
<script type="text/javascript" src="scripts/additional_utilities.js"></script> 
<script type="text/javascript" src="scripts/prototype-1.5.1.js?"></script>
<script type="text/javascript" src="scripts/scriptaculous.js?"></script>
<script type="text/javascript" src="scripts/overlibmws.js?"></script>
<script type="text/javascript" src="scripts/ajaxtags-1.2.js?"></script>
<script type="text/javascript" src="scripts/treeScript.js?"></script>
<script type="text/javascript" src="scripts/Tooltip-0.6.0.js?"></script>
<script type="text/javascript" src="scripts/lightbox.js?"></script>



<script>

function init() {
    check_width();
    initMenu();
}

if (document.addEventListener) {  
  document.addEventListener("DOMContentLoaded", init, false);
} else {
  window.onload = init; 
}

function setMenuAction(button, form, action, validate, parameters) {
  
  var fieldObj = document.getElementById("menuForm").elements['selectedIDs'];
  var ID = '';
  var searchString = '';
  var doSearch = '<%=request.getAttribute(IActionConstants.IN_MENU_SELECT_LIST_HEADER_SEARCH)%>';
  
  var searchedString = '<%=Encode.forJavaScript((String)request.getAttribute(IActionConstants.SEARCHED_STRING))%>';
  
  if (fieldObj != null) {
    //If only one checkbox
    if (fieldObj[0] == null) {
       if (fieldObj.value != null && fieldObj.checked == true) {
         ID = encodeURIComponent(fieldObj.value);
       }
   } else {
      for (var i = 0; i < fieldObj.length; i++) {
         if (fieldObj[i].checked == true) {
        	if(ID.length > 0) {
        		ID += ",";
        	}
            ID += encodeURIComponent(fieldObj[i].value);
            
            //break;
         }
       }
    }
  }
  
  
  
  var sessionid = getSessionFromURL(form.action);
  var context = '<%= request.getContextPath() %>';
  var formName = form.name; 
 
  var parsedFormName;
  
  if (button.name == 'previous' || button.name == 'next' || button.name == 'cancel'|| button.name=='search' || button.name == 'searchString') {
     parsedFormName = formName.substring(1, formName.length - 4);
     parsedFormName = formName.substring(0,1).toUpperCase() + parsedFormName;
     //the action relates to a specific item on the menu form
  } else if (button.name == 'add' || button.name == 'edit' || button.name == 'deactivate') {
     parsedFormName = formName.substring(1, formName.length - 8);
     parsedFormName = formName.substring(0,1).toUpperCase() + parsedFormName;
  } else {
  //EXCEPTION!!!!
  //in case no button was pressed 
  //selection from drop down was made (e.g. analyteTestResultMenu)
     parsedFormName = formName.substring(1, formName.length - 4);
     parsedFormName = formName.substring(0,1).toUpperCase() + parsedFormName;
  }
	
   if (button.name == 'edit' || button.name == 'deactivate') { 
      parameters += ID;
   }

 if (button.name == 'previous' || button.name == 'next') {
      if (doSearch != null) {
       
         if ( doSearch == "true")
         {  
            parameters += "&search=Y"; 
         }
         if (searchedString != "null") { 
             parameters += "&searchString=" + searchedString;
         }
    }
  }
  
  if (button.name != 'cancel' && button.name != 'search' && button.name != 'searchString') {
        parameters += '&startingRecNo=<%= startingRecNo %>';
  }
  
  if (button.name == 'search' || button.name == 'searchString')  {
  
    parameters += "&startingRecNo=1";
     
    var searchForm = document.getElementById("searchForm");
    if (searchForm != null) {
        var fieldObj = searchForm.elements['searchString'];
        searchString = fieldObj.value;
        parameters += "&searchString=" + searchString; 
    }
  }

   var adminFilterCheckBox = document.getElementById("isAdmin");
      if(adminFilterCheckBox != null){   
         if (adminFilterCheckBox.checked == true){
         parameters += "&filter=isAdmin";
      }
   }

   var activeFilterCheckBox = document.getElementById("isActive");
      if(activeFilterCheckBox != null){   
         if (activeFilterCheckBox.checked == true){     
         if(parameters.includes("filter=isAdmin")){
             parameters += ",isActive"
         }else{
             parameters += "&filter=isActive";
         }
        }
     }

     var roleFilter = document.getElementById("roleFilter");
      if(roleFilter != null){    
            var role = roleFilter.value;  
            parameters += "&roleFilter="+ role
     }


  form.action = context + '/' + action + parsedFormName + sessionid + parameters;
  form.selectedIDs = parameters;
  
  if ((button.name == 'edit' && ID == '') || (button.name=='search' && searchString == '') ||(button.name=='searchString' && searchString == '') ) {
  } else if (button.name == 'deactivate'){
      form.submit();
  } else {
	  window.location.href = form.action;
  }
   
       
}
</script>
<%
	if (request.getAttribute("cache") != null && request.getAttribute("cache").toString().equals("false")) 
	{
%>	
		<meta http-equiv="Cache-Control" content="no-cache, no-store, proxy-revalidate, must-revalidate"/> <%-- HTTP 1.1 --%>
		<meta http-equiv="Pragma" content="no-cache"/> <%-- HTTP 1.0 --%>
		<meta http-equiv="Expires" content="0"/> 
<%
	}
%>
	
	<title>
		<c:out value="${title}"/>
	</title>
<jsp:include page="${bannerFragment}"/>
<jsp:include page="${loginHeaderFragment}"/>
</head>


<%--html:errors/--%>

<body onLoad="onLoad()" >

	<table cellpadding="0" cellspacing="1" width="100%">
			<tr>
				<td>
					<jsp:include page="${errorFragment}"/>
				</td>
			</tr>
			<tr>
				<td>
					<jsp:include page="${headerFragment}"/>
				</td>
			</tr>
			<tr>
				<td>
					<jsp:include page="${bodyFragment}"/>
				</td>
			</tr>
			<tr>
				<td>
					<jsp:include page="${footerFragment}"/>
				</td>
			</tr>

	</table>

</body>

<script>
console.log('bufferSize: <%=out.getBufferSize()%> remaining: <%= out.getRemaining()%> used: <%=out.getBufferSize() - out.getRemaining()%> autoFlush: <%=out.isAutoFlush() %>')
</script>


</html>

