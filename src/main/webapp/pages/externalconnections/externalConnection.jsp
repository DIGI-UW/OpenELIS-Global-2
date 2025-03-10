<%@ page language="java" contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants,
                 org.openelisglobal.internationalization.MessageUtil"%>

<%@ page isELIgnored="false"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>

<script>

	jQuery(document).ready(function() {
		displayAuthTypeLogic();
		checkIfHttps();
	});
	
	function displayAuthTypeLogic() {
		jQuery(".authRow").hide();
		var authType = jQuery("#authenticationType").val();
		jQuery("#" + authType + "AuthRow").show();
	} 
	
	function checkIfHttps() {
		if (jQuery("#connectionUri").val().startsWith("https")) {
			jQuery("#uploadCertRow").show();
			jQuery("#locked").show();
			jQuery("#unlocked").hide();
		} else if (jQuery("#connectionUri").val().startsWith("http")) {
			jQuery("#uploadCertRow").hide();
			jQuery("#unlocked").show();
			jQuery("#locked").hide();
		} else if (jQuery("#connectionUri").val() == "") {
			jQuery("#uploadCertRow").hide();
			jQuery("#unlocked").hide();
			jQuery("#locked").hide();
		} else {
			jQuery("#uploadCertRow").hide();
		}
	}
	
	function setSave() {
		jQuery("#saveButtonId").attr("disabled", !validateForm(jQuery("#mainForm")));
	}

	function validateForm(form) {
		if (jQuery.trim(jQuery("#connectionName").val()) === '') {
			return false;
		}
		if (jQuery.trim(jQuery("#programmedConnection").val()) === '') {
			return false;
		}
		if (jQuery.trim(jQuery("#authenticationType").val()) === '') {
			return false;
		}
		if (jQuery.trim(jQuery("#connectionUri").val()) === '') {
			return false;
		}
		
		return true;
	}
	
	function savePage() {
		var id = jQuery("#externalConnectionId").val();
		if (id) {
			jQuery("#mainForm").attr('action', 'ExternalConnection?ID=' + jQuery("#externalConnectionId").val())
		}
		jQuery("#mainForm").submit();
	}
	
</script>

<form:form name="${form.formName}" 
			action="${form.formAction}" 
			modelAttribute="form" 
			onSubmit="return submitForm(this);" 
			method="${form.formMethod}"
			id="mainForm"
			enctype="multipart/form-data">

<div style="color: DarkRed;"><spring:message code="externalconnections.instructions"/></div>

<form:hidden id="externalConnectionId" path="externalConnection.id" />
<form:hidden path="externalConnection.active" value="true" />
<c:if test="${not empty form.externalConnection.lastupdated}">
	<form:hidden id="externalConnectionId" path="externalConnection.lastupdated" />
	<form:hidden  path="externalConnection.descriptionLocalization.lastupdated" />
	<form:hidden  path="externalConnection.nameLocalization.lastupdated" />
</c:if>
<table>
	<tr class="spacerRow">
		<td>&nbsp;</td>
	</tr>
	<tr>
		<td><spring:message code="externalconnections.name"/></td>
	</tr>
	<tr>
		<td>
			<form:hidden  path="externalConnection.nameLocalization.id" />
			<form:input path="externalConnection.nameLocalization.localizedValue"
				id="connectionName" onChange="setSave()"/></td>
	</tr>
	<tr class="spacerRow">
		<td>&nbsp;</td>
	</tr>
	<tr>
		<td><spring:message code="externalconnections.programmedconnection"/></td>
	</tr>
	<tr>
		<td>
			<form:select id="programmedConnection" path="externalConnection.programmedConnection" onChange="setSave()">
				<form:option value="" label=""/>
				<form:options items="${form.programmedConnections}" itemLabel="message" itemValue="value"/>
			</form:select></td>
	</tr>
	<tr class="spacerRow">
		<td>&nbsp;</td>
	</tr>

	<tr>
		<td><spring:message code="externalconnections.description"/> <span style="font-style:italic;">(<spring:message code="generic.optional"/>)</span></td>
	</tr>

	<tr>
		<td>
			<form:hidden  path="externalConnection.descriptionLocalization.id" />
			<form:textarea path="externalConnection.descriptionLocalization.localizedValue" onChange="setSave()"
				id="connectiondescription" rows="10" cols="50" style="width:auto"/></td>
	</tr>
	<tr class="spacerRow">
		<td>&nbsp;</td>
	</tr>

	<tr>
		<td><spring:message code="externalconnections.contactinfo"/> <span style="font-style:italic;">(<spring:message code="generic.optional"/>)</span></td>
	</tr>
	<tr>
		<td>
		<spring:message code="person.lastName" var="lastNamePlaceholder"/>
		<spring:message code="person.firstName" var="firstNamePlaceholder"/>
		<spring:message code="person.phone" var="phonePlaceholder"/>
		<spring:message code="person.email" var="emailPlaceholder"/>
		<table>
		<thead>
			<tr>
			<th>${lastNamePlaceholder}</th>
			<th>${firstNamePlaceholder}</th>
			<th>${phonePlaceholder}</th>
			<th>${emailPlaceholder}</th>
			</tr>
		</thead>
		<tbody>
		<c:choose>
		<c:when test="${empty form.externalConnectionContacts}">
			<tr>
				<td><form:input
					path="externalConnectionContacts[0].person.lastName"
					placeholder="${lastNamePlaceholder}" onChange="setSave()" /></td>
				<td><form:input
					path="externalConnectionContacts[0].person.firstName"
					placeholder="${firstNamePlaceholder}" onChange="setSave()" /></td>
				<td><form:input
					path="externalConnectionContacts[0].person.primaryPhone"
					placeholder="${phonePlaceholder}" onChange="setSave()" /></td>
				<td><form:input
					path="externalConnectionContacts[0].person.email"
					placeholder="${emailPlaceholder}" onChange="setSave()" /></td>
			</tr>
		</c:when>
		<c:otherwise>
		<c:forEach items="${form.externalConnectionContacts}" var="contact"
				varStatus="iter">
			<tr>
				
				<td>
					<form:hidden
						path="externalConnectionContacts[${iter.index}].id"/>
					<form:hidden
						path="externalConnectionContacts[${iter.index}].lastupdated"/>
					<form:hidden
						path="externalConnectionContacts[${iter.index}].person.id"/>
					<form:hidden
						path="externalConnectionContacts[${iter.index}].person.lastupdated"/>
					<form:input
						path="externalConnectionContacts[${iter.index}].person.lastName"
						placeholder="${lastNamePlaceholder}" /></td>
				<td><form:input
						path="externalConnectionContacts[${iter.index}].person.firstName"
						placeholder="${firstNamePlaceholder}" onChange="setSave()" /></td>
				<td><form:input
						path="externalConnectionContacts[${iter.index}].person.primaryPhone"
						placeholder="${phonePlaceholder}" onChange="setSave()" /></td>
				<td><form:input
						path="externalConnectionContacts[${iter.index}].person.email"
						placeholder="${emailPlaceholder}" onChange="setSave()" /></td>
			</tr>
		</c:forEach>
		</c:otherwise>
		</c:choose>
		</tbody>
		</table>
		</td>
	</tr>
	<tr class="spacerRow">
		<td>&nbsp;</td>
	</tr>

	<tr>
		<td><spring:message code="externalconnections.authtype"/></td>
	</tr>
	<tr>
		<td><form:select
				path="externalConnection.activeAuthenticationType"
				id='authenticationType' onChange="setSave();displayAuthTypeLogic()">
				<form:options items="${form.authenticationTypes}" itemValue="value"
					itemLabel="message" />
			</form:select></td>
	</tr>
	<tr id="certificateAuthRow" class="authRow" style="display:none;"><td>
		<spring:message code="externalconnections.authtype.cert.instructions" htmlEscape="false"/>
		<br>
		<spring:message code="externalconnections.authtype.cert.upload"/>
		<input type="file" name="certificate"  onChange="setSave()"/>
		<c:if test="${not empty form.certificateAuthenticationData.id}">
			<form:hidden path="certificateAuthenticationData.id"/>
			<form:hidden path="certificateAuthenticationData.lastupdated"/>
		</c:if>
	</td></tr>
	<tr id="basicAuthRow" class="authRow" style="display:none;"><td>
		<c:if test="${not empty form.basicAuthenticationData.id}">
			<form:hidden path="basicAuthenticationData.id"/>
			<form:hidden path="basicAuthenticationData.lastupdated"/>
		</c:if>
		<spring:message code="externalconnections.authtype.basic.username"/>
		<form:input path="basicAuthenticationData.username" onChange="setSave()"/>
		<br>
		<spring:message code="externalconnections.authtype.basic.password"/>
		<form:password path="basicAuthenticationData.password" value="${form.basicAuthenticationData.password}" onChange="setSave()"/>
	</td></tr>
	<tr id="bearerAuthRow" style="display:none;"><td>
	</td></tr>
	<tr id="noneAuthRow" class="authRow" style="display:none;"><td>
	</td></tr>
	
	<tr class="spacerRow">
	<td>&nbsp;</td>
	</tr>

	<tr>
		<td><spring:message code="externalconnections.url.instruction"/></td>
	</tr>

	<tr>
		<td>
			<form:input id="connectionUri" path="externalConnection.uri" onInput="setSave();checkIfHttps()"/>
			<span id="locked"><i class="fas fa-lock" style="color:Green;"></i></span>
			<span id="unlocked"><i class="fas fa-lock-open" style="color:DarkRed;"></i></span>
		</td>
	</tr>
</table>
</form:form>
