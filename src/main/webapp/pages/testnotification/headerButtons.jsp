<%@ page language="java"
	contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants,
			org.openelisglobal.common.util.resources.ResourceLocator,
			java.util.Locale,
			org.openelisglobal.internationalization.MessageUtil"
%>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>




<table border="0" cellpadding="0" cellspacing="0" width="100%">
	<tbody >
	<tr>
			<td>&nbsp;</td>
  	</tr>
  	<tr>
			<td>
				<button type="button" onClick="saveChanges()"><spring:message code="label.button.save" /></button>
			</td>

		</tr>
   </tbody>
</table>

<script>
	function saveChanges() {
		window.onbeforeunload = null;
		jQuery("#menuForm").submit();
	}
</script> 
