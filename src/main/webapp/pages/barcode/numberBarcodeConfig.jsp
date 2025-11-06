<%@ page language="java" contentType="text/html; charset=UTF-8" %>

<%@ page import="org.openelisglobal.common.action.IActionConstants" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



	

<script>
function checkFieldInt(field) {
	if (isNaN(field.value) || field.value.indexOf(".") > -1) {
		validator.setFieldValidity(false, field.id);
		selectFieldErrorDisplay(false, field);
		alert("<spring:message code='siteInfo.number.nonnumber'/>");
	} else if (parseInt(field.value) <= 0) {
		validator.setFieldValidity(false, field.id);
		selectFieldErrorDisplay(false, field);
		alert("<spring:message code='siteInfo.number.invalidnumber'/>");
	} else {
		validator.setFieldValidity(true, field.id);
		selectFieldErrorDisplay(true, field);
	}
	
}

function validateBarcodeNumericField(field) {
	checkFieldInt(field);
	if (validator.isFieldValid(field.id)) {
		if (field.id === 'numDefaultOrderLabels') {
			var maxField = document.getElementById('numMaxOrderLabels');
			if (+(field.value) > +(maxField.value)) {
				validator.setFieldValidity(false, field.id);
				selectFieldErrorDisplay(false, field);
				validator.setFieldValidity(false, maxField.id);
				selectFieldErrorDisplay(false, maxField);
				alert("<spring:message code='barcode.number.exceedsmax'/>");
			} else {
				validator.setFieldValidity(true, field.id);
				selectFieldErrorDisplay(true, field);
				validator.setFieldValidity(true, maxField.id);
				selectFieldErrorDisplay(true, maxField);
			}
		} else if (field.id === 'numDefaultSpecimenLabels') {
			var maxField = document.getElementById('numMaxSpecimenLabels');
			if (+(field.value) > +(maxField.value)) {
				validator.setFieldValidity(false, field.id);
				selectFieldErrorDisplay(false, field);
				validator.setFieldValidity(false, maxField.id);
				selectFieldErrorDisplay(false, maxField);
				alert("<spring:message code='barcode.number.exceedsmax'/>");
			} else {
				validator.setFieldValidity(true, field.id);
				selectFieldErrorDisplay(true, field);
				validator.setFieldValidity(true, maxField.id);
				selectFieldErrorDisplay(true, maxField);
			}
		} else if (field.id === 'numMaxOrderLabels') {
			var defaultField = document.getElementById('numDefaultOrderLabels');
			if (+(field.value) < +(defaultField.value)) {
				validator.setFieldValidity(false, field.id);
				selectFieldErrorDisplay(false, field);
				validator.setFieldValidity(false, defaultField.id);
				selectFieldErrorDisplay(false, defaultField);
				alert("<spring:message code='barcode.number.exceedsmax'/>");
			} else {
				validator.setFieldValidity(true, field.id);
				selectFieldErrorDisplay(true, field);
				validator.setFieldValidity(true, defaultField.id);
				selectFieldErrorDisplay(true, defaultField);
			}
		} else if (field.id === 'numMaxSpecimenLabels') {
			var defaultField = document.getElementById('numDefaultSpecimenLabels');
			if (+(field.value) < +(defaultField.value)) {
				validator.setFieldValidity(false, field.id);
				selectFieldErrorDisplay(false, field);
				validator.setFieldValidity(false, defaultField.id);
				selectFieldErrorDisplay(false, defaultField);
				alert("<spring:message code='barcode.number.exceedsmax'/>");
			} else {
				validator.setFieldValidity(true, field.id);
				selectFieldErrorDisplay(true, field);
				validator.setFieldValidity(true, defaultField.id);
				selectFieldErrorDisplay(true, defaultField);
			}
		} 
	}
	if (validator.isAllValid()) {
		enableSave();
	} else {
		disableSave();
	}
}

</script>

<h2><spring:message code="siteInfo.section.number"/></h2>
<h3><spring:message code="siteInfo.title.default.barcode"/></h3>
<p><spring:message code="siteInfo.description.default.barcode"/></p>
<table width="80%">
	<tr>
		<td>
			<spring:message code="barcode.label.type.order"/>
		</td>
		<td>
			<spring:message code="barcode.label.type.specimen"/>
		</td>
	</tr>
	<tr>
		<td>
			<form:input path="numDefaultOrderLabels" 
				id="numDefaultOrderLabels"
				onchange="validateBarcodeNumericField(this)"/>
		</td>
		<td>
			<form:input path="numDefaultSpecimenLabels"
				id="numDefaultSpecimenLabels"
				onchange="validateBarcodeNumericField(this)"/>
		</td>
	</tr>
</table>

<h3><spring:message code="siteInfo.title.max.barcode"/></h3>
<p><spring:message code="siteInfo.description.max.barcode"/></p>
<table width="80%">
	<tr>
		<td>
			<spring:message code="barcode.label.type.order"/>
		</td>
		<td>
			<spring:message code="barcode.label.type.specimen"/>
		</td>
	</tr>
	<tr>
		<td>
			<form:input path="numMaxOrderLabels" 
				id="numMaxOrderLabels"
				onchange="validateBarcodeNumericField(this)"/>
		</td>
		<td>
			<form:input path="numMaxSpecimenLabels"
				id="numMaxSpecimenLabels"
				onchange="validateBarcodeNumericField(this)"/>
		</td>
	</tr>
</table>