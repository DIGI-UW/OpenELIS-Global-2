<%@ page language="java" contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants, 
			org.openelisglobal.patienttype.valueholder.PatientType"%>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>




	


<table width="100%" border=2">
	<tr>
		<th>
			<spring:message code="label.form.select"/>			
		</th>
		<th>
			<spring:message code="patienttype.type" />
		</th>
		<th>
			<spring:message code="patienttype.description" />
		</th>
	</tr>		
	<logic:iterate id="patientype" indexId="ctr" name="${form.formName}" property="menuList"	type="PatientType">
		<bean:define id="patientypeID" name="patientype" property="id" />
		<tr>
			<td class="textcontent">
				<html:multibox name='${form.formName}' property="selectedIDs">
					<bean:write name="patientypeID" />
				</html:multibox>
			</td>
			<td class="textcontent">
				<bean:write name="patientype" property="type" />
			</td>
			<td class="textcontent">
				<bean:write name="patientype" property="description" />
			</td>			
		</tr>
	</logic:iterate>
</table>
