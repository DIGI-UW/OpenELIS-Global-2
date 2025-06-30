<%@ page language="java"
	contentType="text/html; charset=UTF-8"
%>
<%@ page import="org.openelisglobal.common.constants.Constants" %>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<c:set var="success" value="${success || param.forward == 'success'}" />
<c:if test="${success || (not empty requestScope[Constants.SUCCESS_MSG])}">

<script type="text/javascript">
function /*void*/ showSuccessMessage( show ){
	$("successMsg").style.visibility = show ? 'visible' : 'hidden';
}
</script>

<div id="successMsg" 
     style="text-align:center; 
     		color:seagreen;  
     		width : 100%;
     		font-size:170%;" >
	 		
	<c:if test="${empty successMessage}">
		<spring:message code="save.success"/>
	</c:if>
	<c:if test="${not empty successMessage}">
		<c:out value="${successMessage}"/>
	</c:if>
	
</div>
</c:if>



