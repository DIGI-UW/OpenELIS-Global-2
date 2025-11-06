<%@ page language="java" contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants"%>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<c:set var="success" value="${success}" />

<%
	String saveDisabled = (String) request.getSession().getAttribute(IActionConstants.SAVE_DISABLED);
%>

	<table border="0" cellpadding="0" cellspacing="4" width="100%">
		<tbody valign="middle">
			<tr>
				<td align="right">
				<input type="button"
					   id="saveButtonId"
					   onclick="savePage();"
					   <c:if test="${saveDisabled}">disabled="disabled"</c:if>
					   value="<spring:message code="label.button.save" />">
				</td>
				<td>
				<input type="button"
					   id="cancelButtonId"
					   onclick="cancelAction();"
					   value="<spring:message code="label.button.cancel" />">
				</td>
			</tr>
		</tbody>
	</table>

<script type="text/javascript">

<%if( request.getAttribute(IActionConstants.FWD_SUCCESS) != null &&
      ((Boolean)request.getAttribute(IActionConstants.FWD_SUCCESS)) ) { %>
if( typeof(showSuccessMessage) != 'undefined' ){
	showSuccessMessage( true );
}
<% } %>

</script>