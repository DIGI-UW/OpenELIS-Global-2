<%@ page language="java" contentType="text/html; charset=UTF-8" %>

<%@ page import="org.openelisglobal.common.action.IActionConstants" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



	

<script>
function checkFieldFloat(field) {
	if (isNaN(field.value)) {
		validator.setFieldValidity(false, field.id);
		selectFieldErrorDisplay(false, field);
		alert("<spring:message code='siteInfo.size.nonnumber'/>");
	} else if (parseFloat(field.value) <= 0) {
		validator.setFieldValidity(false, field.id);
		selectFieldErrorDisplay(false, field);
		alert("<spring:message code='siteInfo.size.invalidnumber'/>");
	} else {
		validator.setFieldValidity(true, field.id);
		selectFieldErrorDisplay(true, field);
	}
	if (validator.isAllValid()) {
		enableSave();
	} else {
		disableSave();
	}
}

</script>

<h2><spring:message code="siteInfo.section.size"/></h2>
<p><spring:message code="siteInfo.description.dimensions"/></p>
<table width="80%">
	<tr>
		<td><spring:message code="barcode.label.type.order"/>:</td>
		<td><spring:message code="barcode.label.type.specimen"/>:</td>
		<td><spring:message code="barcode.label.type.block"/>:</td>
		<td><spring:message code="barcode.label.type.slide"/>:</td>
	</tr>
	<tr>
		<td>
			<spring:message code="siteInfo.size.height"/>:
			<form:input path="heightOrderLabels"
				id="heightOrderLabels"
				onchange="checkFieldFloat(this)"/>
			<spring:message code="siteInfo.size.units"/>
		</td>
		<td>
			<spring:message code="siteInfo.size.height"/>:
			<form:input path="heightSpecimenLabels"
				id="heightSpecimenLabels"
				onchange="checkFieldFloat(this)"/>
			<spring:message code="siteInfo.size.units"/>
		</td>
		<td>
			<spring:message code="siteInfo.size.height"/>:
			<form:input path="heightBlockLabels"
				id="heightBlockLabels"
				onchange="checkFieldFloat(this)"/>
			<spring:message code="siteInfo.size.units"/>
		</td>
		<td>
			<spring:message code="siteInfo.size.height"/>:
			<form:input path="heightSlideLabels"
				id="heightSlideLabels"
				onchange="checkFieldFloat(this)"/>
			<spring:message code="siteInfo.size.units"/>
		</td>
	</tr>
	<tr>
		<td>
			<spring:message code="siteInfo.size.width"/>:
			<form:input path="widthOrderLabels"
				id="widthOrderLabels"
				onchange="checkFieldFloat(this)"/>
			<spring:message code="siteInfo.size.units"/>
		</td>
		<td>
			<spring:message code="siteInfo.size.width"/>:
			<form:input path="widthSpecimenLabels"
				id="widthSpecimenLabels"
				onchange="checkFieldFloat(this)"/>
			<spring:message code="siteInfo.size.units"/>
		</td>
		<td>
			<spring:message code="siteInfo.size.width"/>:
			<form:input path="widthBlockLabels"
				id="widthBlockLabels"
				onchange="checkFieldFloat(this)"/>
			<spring:message code="siteInfo.size.units"/>
		</td>
		<td>
			<spring:message code="siteInfo.size.width"/>:
			<form:input path="widthSlideLabels"
				id="widthSlideLabels"
				onchange="checkFieldFloat(this)"/>
			<spring:message code="siteInfo.size.units"/>
		</td>
	</tr>
</table>