<%@ page language="java" contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants"%>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<script>
 
function validateForm(form) {
     return true;
}
</script>

<table>
	<tr>
		<td class="label">
			<spring:message code="patienttype.id" />:
		</td>
		<td>
		<form:input path="id" readonly="readonly" />
		</td>
	</tr>
	<tr>
		<td class="label">
			<spring:message code="patienttype.type" />:<span class="requiredlabel">*</span>
		</td>
		<td>
			<form:input path="type" maxlength="1"/>
		</td>
	</tr>
	<tr>
		<td class="label">
			<spring:message code="patienttype.description" />:
		</td>
		<td>
			<form:input path="description" maxlength="50"/>
		</td>
	</tr>	
</table>
