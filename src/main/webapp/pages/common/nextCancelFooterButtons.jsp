<%@ page language="java" contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants"%>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>




<%
	String nextDisabled = (String) request.getSession().getAttribute(IActionConstants.NEXT_DISABLED);
%>

	<table border="0" cellpadding="0" cellspacing="4" width="100%">
		<tbody valign="middle">
			<tr>
				<td align="right">
					<button type="button" 
							onclick="nextPage();" 
							id="nextButtonId" 
							disabled="<%=Boolean.valueOf(nextDisabled).booleanValue()%>">
						<spring:message code="label.button.next" />
					</button>
				</td>
				<td>
					<button type="button" onclick="cancelAction();" id="cancelButtonId">
						<spring:message code="label.button.cancel" />
					</button>
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