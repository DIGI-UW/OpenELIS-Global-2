<%@ page language="java" contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants,
			org.openelisglobal.localization.valueholder.Localization"%>

<%@ page isELIgnored="false"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<script>
	var dirty = false;

	function makeDirty() {
		dirty = true;

		if (typeof (showSuccessMessage) !== 'undefined') {
			showSuccessMessage(false);
		}
		// Adds warning when leaving page if content has been entered into makeDirty form fields
		function formWarning() {
			return "<spring:message code="banner.menu.dataLossWarning"/>";
		}
		window.onbeforeunload = formWarning;
	}

	function editPageForTest(testId) {
		window.location
	}
</script>

<table style="width: 80%; border-collapse: collapse;" id="mainTable">
	<thead>
		<tr>
			<th rowspan="2">&nbsp;</th>
			<th rowspan="2"><spring:message code="label.testName" /></th>
			<th colspan="2"><spring:message code="label.patient"/></th>
			<th colspan="2"><spring:message code="label.provider"/></th>
		</tr>
		<tr>
			<th><spring:message code="externalconnection.email" /></th>
			<th><spring:message code="externalconnection.sms"/></th>
			<th><spring:message code="externalconnection.email" /></th>
			<th><spring:message code="externalconnection.sms" /></th>
		</tr>
	</thead>
	<tbody>
		<form:form name="${form.formName}" action="${form.formAction}"
			modelAttribute="form" method="${form.formMethod}" id="menuForm">
			<c:forEach items="${form.menuList}" var="notificationConfig"
				varStatus="iter">
				<form:hidden path="menuList[${iter.index}].test.id" />
				<form:hidden
					path="menuList[${iter.index}].defaultPayloadTemplate.id" />
				<form:hidden path="menuList[${iter.index}].id" />
				<tr>
					<td style="border-top: solid thin;">&nbsp;</td>
					<td style="border-top: solid thin;">${notificationConfig.test.augmentedTestName}</td>
					<td
						style="border-top: solid thin; text-align: center; vertical-align: middle;">
						<form:checkbox path="menuList[${iter.index}].patientEmail.active"
							value="${notificationConfig.patientEmail.active}"
							onChange="makeDirty();" />
					</td>
					<td
						style="border-top: solid thin; text-align: center; vertical-align: middle;">
						<form:checkbox path="menuList[${iter.index}].patientSMS.active"
							value="${notificationConfig.patientSMS.active}"
							onChange="makeDirty();" />
					</td>
					<td
						style="border-top: solid thin; text-align: center; vertical-align: middle;">
						<form:checkbox path="menuList[${iter.index}].providerEmail.active"
							value="${notificationConfig.providerEmail.active}"
							onChange="makeDirty();" />
					</td>
					<td
						style="border-top: solid thin; text-align: center; vertical-align: middle;">
						<form:checkbox path="menuList[${iter.index}].providerSMS.active"
							value="${notificationConfig.providerSMS.active}"
							onChange="makeDirty();" />
					</td>
					<c:url value="TestNotificationConfig" var="editUrl">
						<c:param name="testId" value="${notificationConfig.test.id}" />
					</c:url>
					<td style="text-align: center; vertical-align: middle;"><a
						style="cursor: pointer" href="${editUrl}"
						title="<spring:message code='tooltip.fulledit'/>">
							<i class="fas fa-cog"></i>
					</a></td>
				</tr>
			</c:forEach>
		</form:form>
	</tbody>
</table>
