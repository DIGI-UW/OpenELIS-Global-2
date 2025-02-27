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

<table width="60%">
	<tr>
		<td class="label" width="10%">
			<spring:message code="role.name" />
			:
			<span class="requiredlabel">*</span>
		</td>
		<td width="40%">
			<html:select name="${form.formName}" property="userNameId">
				<html:option value="0">&nbsp;</html:option>
				<html:optionsCollection name="${form.formName}" property="users" label="loginName" value="id"/>	
			</html:select>

		</td>
		<td class="label" width="50%">
			<spring:message code="role.description" />
		</td>
	</tr>
	<logic:iterate  name="${form.formName}" property="roles" id="role" >
	<tr>
	<td>&nbsp;</td>
	<td>&nbsp;</td>
	<td>
		<html:multibox name="${form.formName}" property="selectedRoles" >
			<bean:write name="role" property="id" />
		</html:multibox>
		<bean:write name="role" property="name" />
	</td>
	</tr>
	</logic:iterate>
	<tr>
		<td>
			&nbsp;
		</td>
	</tr>
</table>


