
<%@ page isELIgnored="false" 
	import="org.openelisglobal.common.action.IActionConstants"%>
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
     		font-size:170%;
     		display:hidden;" >
	 		
	<c:if test="${empty successMessage}">
		<spring:message code="save.success"/>
	</c:if>
	<c:if test="${not empty successMessage}">
		<c:out value="${successMessage}"/>
	</c:if>
	
</div>
</c:if>


<c:set var="menuDef" value="${menuDefinition}" />
<center>
<table cellpadding="0" cellspacing="0" width="100%" height="100%" border="0">
<tr>
<td>
 <jsp:include page="${rightMenuFragment}"/>
</td>
</tr>
</table>
</center>

