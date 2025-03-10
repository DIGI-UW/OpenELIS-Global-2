<%@ page language="java" contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants,
                 org.openelisglobal.internationalization.MessageUtil"%>

<%@ page isELIgnored="false"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>


<style>
.tab-label {
	overflow: hidden;
	border: 1px solid #ccc;
	background-color: #f1f1f1;
}

/* Style the buttons that are used to open the tab content */
.tab-label button {
	background-color: inherit;
	float: left;
	border: none;
	outline: none;
	cursor: pointer;
	padding: 14px 16px;
	transition: 0.3s;
}

/* Change background color of buttons on hover */
.tab-label button:hover {
	background-color: #ddd;
}

/* Create an active/current tablink class */
.tab-label button.active {
	background-color: #ccc;
}

/* Style the tab content */
.tabcontent {
	visibility: collapse;
	padding: 6px 12px;
	border: 1px solid #ccc;
	border-top: none;
}

.patientEmailRow {
	visibility: visible;
}
</style>


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
		document.getElementById("saveButtonId").disabled = false;
	}

	function enableEditSystemDefault() {
		var systemDefaults = document.getElementsByClassName("systemDefault");
		for (i = 0; i < systemDefaults.length; i++) {
			systemDefaults[i].disabled = false;
		}
		document.getElementById("editSystemDefaultPayloadTemplate").value = "true";
	}

	function openTab(event, tabClass) {
		// Declare all variables
		var i, tabcontent, tablinks, rows;

		// Get all elements with class="tabcontent" and hide them
		tabcontent = document.getElementsByClassName("tabcontent");
		for (i = 0; i < tabcontent.length; i++) {
			tabcontent[i].style.visibility = "collapse";
		}

		// Get all elements with class="tablinks" and remove the class "active"
		tablinks = document.getElementsByClassName("tablinks");
		for (i = 0; i < tablinks.length; i++) {
			tablinks[i].className = tablinks[i].className
					.replace(" active", "");
		}

		// Show the current tab, and add an "active" class to the button that opened the tab
		rows = document.getElementsByClassName(tabClass);
		for (i = 0; i < rows.length; i++) {
			rows[i].style.visibility = "visible";
		}
		event.currentTarget.className += " active";
	}

	function validateForm(form) {
		return true;
	}
	
	function savePage() {
		window.onbeforeunload = null;
		document.getElementById("mainForm").submit();
	}
</script>

<form:hidden path="config.id" />
<form:hidden path="config.test.id" />
<table width="70%">
	<tbody>
		<tr>
			<td>

				<h2>
					<c:out value="${form.config.test.localizedTestName.localizedValue}" />
				</h2> <br> <spring:message code="testnotification.patient.email" /> <form:checkbox
					path="config.patientEmail.active" onChange="makeDirty();" /> <spring:message
					code="testnotification.patient.sms" /> <form:checkbox
					path="config.patientSMS.active" onChange="makeDirty();" /> <spring:message
					 code="testnotification.provider.email" /> <form:checkbox
					path="config.providerEmail.active" onChange="makeDirty();" /> <spring:message
					 code="testnotification.provider.sms" /> <form:checkbox
					path="config.providerSMS.active" onChange="makeDirty();" />
			</td>
		</tr>
		<tr>
			<td>
				<div id="instructions"
					style="white-space: pre-line;background-color: rgb(200, 218, 218); padding: 20px;"
					><b><h2 style="background-color: rgb(200, 218, 218)"><spring:message code="testnotification.instructions.header"/></h2>
						<spring:message code="testnotification.instructions.body"/>
						<h3 style="background-color: rgb(200, 218, 218)"><spring:message code="testnotification.instructionis.variables.header"/></h3>
						<spring:message code="testnotification.instructionis.variables.body"/>
					</b>
				</div>
			</td>
		</tr>
		<!-- 	SYSTEM DEFAULT -->
		<tr>
			<td style="width: 100%;">
				<h2>
					<spring:message code="testnotification.systemdefault.template"/>
					<form:hidden id="editSystemDefaultPayloadTemplate"
						path="editSystemDefaultPayloadTemplate" value="false" />
					<form:hidden path="systemDefaultPayloadTemplate.id" />
				</h2>
			</td>
		</tr>
		<tr>
			<td><spring:message code="testnotification.subjecttemplate" /> <form:input
					path="systemDefaultPayloadTemplate.subjectTemplate"
					class="systemDefault" onChange="makeDirty();" disabled="true" />

				<button style="float: right;" type="button"
					onClick="enableEditSystemDefault()"><spring:message code="label.button.edit" /></button></td>
		</tr>
		<tr>
			<td><spring:message code="testnotification.messagetemplate"/></td>
		</tr>
		<tr>
			<td><form:textarea
					path="systemDefaultPayloadTemplate.messageTemplate" disabled="true"
					class="systemDefault" onChange="makeDirty();" cols="50" rows="10"
					style="overflow:scroll;" /></td>
		</tr>

		<!-- 		TEST DEFAULT -->
		<tr>
			<td>
				<h2>
					<spring:message code="testnotification.testdefault.template" />
				</h2>
			</td>
		</tr>
		<tr>
			<td><spring:message code="testnotification.subjecttemplate" /> <form:hidden
					path="config.defaultPayloadTemplate.type" value="TEST_RESULT" /> <form:input
					path="config.defaultPayloadTemplate.subjectTemplate"
					onChange="makeDirty();" /></td>
		</tr>
		<tr>
			<td><spring:message code="testnotification.messagetemplate" /></td>
		</tr>
		<tr>
			<td><form:textarea
					path="config.defaultPayloadTemplate.messageTemplate" cols="50"
					rows="10" style="overflow:scroll;" onChange="makeDirty();" /></td>
		</tr>

		<!-- 		METHOD/PERSON SPECIFIC -->
		<tr>
			<td>
				<h2>
					<spring:message code="testnotification.options" />
				</h2>
			</td>
		</tr>
		<tr>
			<td>
				<div class="tab-label">
					<button type="button" class="tablinks active"
						onclick="openTab(event, 'patientEmailRow')"><spring:message code="testnotification.patient.email"/></button>
					<button type="button" class="tablinks"
						onclick="openTab(event, 'patientSMSRow')"><spring:message code="testnotification.patient.sms"/></button>
					<button type="button" class="tablinks"
						onclick="openTab(event, 'providerEmailRow')"><spring:message code="testnotification.provider.email"/></button>
					<button type="button" class="tablinks"
						onclick="openTab(event, 'providerSMSRow')"><spring:message code="testnotification.provider.sms"/></button>
				</div>
			</td>
		</tr>

		<tr class="patientEmailRow tabcontent">
			<td>
				<h2>
					<spring:message code="testnotification.patient.email" />
				</h2>
			</td>
		</tr>
		<tr class="patientEmailRow tabcontent">
			<td>
				<table>
					<tr>
						<td><spring:message code="testnotification.bcc" text="BCC" />
						</td>
						<td><form:input
								path="config.patientEmail.additionalContacts"
								onChange="makeDirty();" /></td>
					</tr>
					<tr>
						<td><spring:message code="testnotification.subjecttemplate"/></td>
						<td><form:hidden
								path="config.patientEmail.payloadTemplate.type"
								value="TEST_RESULT" /> <form:input
								path="config.patientEmail.payloadTemplate.subjectTemplate"
								onChange="makeDirty();" /></td>
					</tr>
				</table>
			</td>
		</tr>
		<tr class="patientEmailRow tabcontent">
			<td><spring:message code="testnotification.messagetemplate" /></td>
		</tr>
		<tr class="patientEmailRow tabcontent">
			<td><form:textarea
					path="config.patientEmail.payloadTemplate.messageTemplate"
					cols="50" rows="10" style="overflow:scroll;"
					onChange="makeDirty();" /></td>
		</tr>
		<tr class="patientSMSRow tabcontent">
			<td>
				<h2>
					<spring:message code="testnotification.patient.sms" />
				</h2>
			</td>
		</tr>
		<tr class="patientSMSRow tabcontent">
			<td>
				<%-- 			<spring:message code="testnotification.subjecttemplate" --%>
				<%-- 					text="Subject" />  --%> <form:hidden
					path="config.patientSMS.payloadTemplate.type" value="TEST_RESULT" />
				<%-- 					<form:input --%> <%-- 					path="config.patientSMS.payloadTemplate.subjectTemplate" --%>
				<%-- 					onChange="makeDirty();" /> --%>
			</td>
		</tr>
		<tr class="patientSMSRow tabcontent">
			<td><spring:message code="testnotification.messagetemplate" /></td>
		</tr>
		<tr class="patientSMSRow tabcontent">
			<td><form:textarea
					path="config.patientSMS.payloadTemplate.messageTemplate" cols="50"
					rows="10" style="overflow:scroll;" onChange="makeDirty();" /></td>
		</tr>


		<tr class="providerEmailRow tabcontent">
			<td>
				<h2>
					<spring:message code="testnotification.provider.email" />
				</h2>
			</td>
		</tr>
		<tr class="providerEmailRow tabcontent">
			<td>
				<table>
					<tr>
						<td><spring:message code="testnotification.bcc" text="BCC" />
						</td>
						<td><form:input
								path="config.providerEmail.additionalContacts"
								onChange="makeDirty();" /></td>
					</tr>
					<tr>
						<td><spring:message code="testnotification.subjecttemplate"/></td>
						<td><form:hidden
								path="config.providerEmail.payloadTemplate.type"
								value="TEST_RESULT" /> <form:input
								path="config.providerEmail.payloadTemplate.subjectTemplate"
								onChange="makeDirty();" /></td>
					</tr>
				</table>
			</td>
		</tr>
		<tr class="providerEmailRow tabcontent">
			<td><spring:message code="testnotification.messagetemplate"/></td>
		</tr>
		<tr class="providerEmailRow tabcontent">
			<td><form:textarea
					path="config.providerEmail.payloadTemplate.messageTemplate"
					cols="50" rows="10" style="overflow:scroll;"
					onChange="makeDirty();" /></td>
		</tr>
		<tr class="providerSMSRow tabcontent">
			<td>
				<h2>
					<spring:message code="testnotification.provider.sms" />
				</h2>
			</td>
		</tr>
		<tr class="providerSMSRow tabcontent">
			<td>
				<%-- 			<spring:message code="testnotification.subjecttemplate" --%>
				<%-- 					text="Subject" />  --%> <form:hidden
					path="config.providerSMS.payloadTemplate.type" value="TEST_RESULT" />
				<%-- 					<form:input --%> <%-- 					path="config.providerSMS.payloadTemplate.subjectTemplate" --%>
				<%-- 					onChange="makeDirty();" /> --%>
			</td>
		</tr>
		<tr class="providerSMSRow tabcontent">
			<td><spring:message code="testnotification.messagetemplate"/></td>
		</tr>
		<tr class="providerSMSRow tabcontent">
			<td><form:textarea
					path="config.providerSMS.payloadTemplate.messageTemplate" cols="50"
					rows="10" style="overflow:scroll;" onChange="makeDirty();" /></td>
		</tr>
	</tbody>
</table>

