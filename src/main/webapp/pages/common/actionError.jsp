<%@ page language="java"
	contentType="text/html; charset=UTF-8"
	import="java.util.Iterator,
		jakarta.servlet.jsp.JspException,
		org.openelisglobal.common.action.IActionConstants,
		org.openelisglobal.common.util.resources.ResourceLocator,
		org.owasp.encoder.Encode,
		org.openelisglobal.common.constants.Constants" %>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>


<!DOCTYPE html>

<script>

function onLoad() {

  	// bugzilla 1397 If a page requires special functionality before load that isn't global, create a
	// preOnLoad method (if we need to run js before errors popup)
	if(window.prePageOnLoad)
	{  
		prePageOnLoad();
	}
       
  	// If a page requires special functionality on load that isn't global, create a
	// pageOnLoad method
	if(window.pageOnLoad)
	{  
		pageOnLoad();
	}

}

// The Struts action form object associated with this page. It is initialized in
// the onLoad() function below to ensure that it is available when defined.
var myActionForm;

// Initialize myActionForm variable after load.
myActionForm = document.forms["<%= (String)request.getAttribute(IActionConstants.FORM_NAME) %>"];



</script>
<c:if test="${not empty requestScope[Constants.REQUEST_ERRORS].globalErrors}">
<center><h1>
<c:forEach items="${requestScope[Constants.REQUEST_ERRORS].globalErrors}" var="error">
	<spring:message code="${error.code}" arguments="${error.arguments}" text="${error.code} ${error.defaultMessage}" /><br>
</c:forEach>
</h1></center>
</c:if>
<c:if test="${not empty requestScope[Constants.REQUEST_ERRORS].fieldErrors}">
<center><h1>
<c:forEach items="${requestScope[Constants.REQUEST_ERRORS].fieldErrors}" var="error">
	<c:out value="${error.field}"/>: <spring:message code="${error.code}" arguments="${error.arguments}" text="${error.code} ${error.defaultMessage}" /><br>
</c:forEach>
</h1></center>
</c:if>

<c:if test="${not empty requestScope[Constants.REQUEST_WARNINGS]}">
<center><h1>
<c:forEach items="${requestScope[Constants.REQUEST_WARNINGS]}" var="warningMsg">
	<c:out value="${warningMsg}"/><br>
</c:forEach>
</h1></center>
</c:if>

