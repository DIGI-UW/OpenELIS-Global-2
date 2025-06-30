<%@ page language="java" contentType="text/html; charset=UTF-8" %>

<%@ page import="org.openelisglobal.common.action.IActionConstants" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>

<script>
function useAltAccessionChange() {
	enableSave();
	jQuery("#prePrintAltAccessionPrefix").prop("disabled", jQuery("#prePrintDontUseAltAccession").prop('checked'));
	altAccessionKeyUp();
}

function altAccessionOnChange() {
	if (jQuery("#prePrintAltAccessionPrefix").val().length < 4) {
		alert("<spring:message code="labno.alt.prefix.error.length"/>");
	} 
	altAccessionKeyUp();
}

function altAccessionKeyUp() {
	if (altAccessionValid()) {
		enableSave();
	} else {
		disableSave();
	}
	
	if (jQuery("#prePrintAltAccessionPrefix").val().toLowerCase() === jQuery("#sitePrefix").val().toLowerCase()) {
	    alert("<spring:message code="labno.alt.prefix.error.unique"/>");
	} 
}

function altAccessionValid() {
	if (!jQuery("#prePrintDontUseAltAccession").prop('checked')) {
		if (jQuery("#prePrintAltAccessionPrefix").val().toLowerCase() === jQuery("#sitePrefix").val().toLowerCase()) {
			return false;
		}
		if (jQuery("#prePrintAltAccessionPrefix").val().length < 4) {
			return false;
		}
	}
	return true;
}

jQuery(document).ready(function() {
	useAltAccessionChange();
}) 
</script>

<h2><spring:message code="siteInfo.section.altAccession"/></h2>
<%-- <p><spring:message code="siteInfo.description.altAccession"/></p> --%>
<form:checkbox id="prePrintDontUseAltAccession" path="prePrintDontUseAltAccession" onChange="useAltAccessionChange()"/> 
<spring:message code="labno.alt.prefix.use"/>
<br>
<form:hidden id="sitePrefix" path="sitePrefix" />
<spring:message code="labno.alt.prefix.instruction"/>: <form:input id="prePrintAltAccessionPrefix" path="prePrintAltAccessionPrefix" maxlength="4" onChange="altAccessionOnChange()" onKeyUp="altAccessionKeyUp()"/>
<br>
<spring:message code="labno.alt.prefix.note"/>
