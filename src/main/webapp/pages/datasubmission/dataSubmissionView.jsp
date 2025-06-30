<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ page import="org.openelisglobal.common.action.IActionConstants,
					org.openelisglobal.datasubmission.valueholder.DataIndicator,
					org.openelisglobal.common.util.DateUtil" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<script type="text/javascript">
function saveAndSubmit() {
	if (checkURL()) {
		if (confirmSentWarning()) {
			showsubmitting();
			var form = document.getElementById("mainForm");
			form.action = "DataSubmission?submit=true";
			form.submit();
		}
	} else {
		alert("<spring:message code="datasubmission.warning.missingurl" />");
	}
}

function saveAndExit() {
	var form = document.getElementById("mainForm");
	form.action = "DataSubmission";
	form.submit();
}

function dateChange() {
	var month = jQuery("#month").val();
	var year = jQuery("#year").val();
	window.location.replace("DataSubmission?month=" + month + "&year=" + year);
}

function editUrl() {
	jQuery("#url").removeAttr("disabled");
}

function checkURL() {
	return jQuery("#url").val() != "";
}

function showsubmitting() {
	if( typeof(showSuccessMessage) != 'undefined' ){
	   	showSuccessMessage( false );
	}
   jQuery("#sending").show();
	window.location = '#sending';
}

function confirmSentWarning() {
	var sentIndicators = [];
	var message = "<spring:message code="datasubmission.warning.sent" arguments="month, year"/>";
	jQuery("span.<%=DataIndicator.SENT%>").each(function() {
		sentIndicators.push(this.id);
	});
	jQuery("span.<%=DataIndicator.RECEIVED%>").each(function() {
		sentIndicators.push(this.id);
	});
	for (var i = 0; i < sentIndicators.length; i++) {
		message += "\n\u2022" + sentIndicators[i];
	}
	if (sentIndicators.length == 0) {
		return true;
	}
	return confirm(message);
}
<%if( request.getAttribute(IActionConstants.FWD_SUCCESS) != null && ((Boolean)request.getAttribute(IActionConstants.FWD_SUCCESS)) == true ) { %>
	if( typeof(showSuccessMessage) != 'undefined' ){
	   	showSuccessMessage( true );
	}
<% } %>
</script>

<div id="sending" style="text-align:center;color:DarkOrange;width:100%;font-size:170%;display:none;">
				<spring:message code="datasubmission.warning.sending"/>
</div>
<spring:message code="datasubmission.label.url" />: 
<form:input path="dataSubUrl.value" id="url" disabled="true"/>
<button type="button" onclick="editUrl();"><spring:message code="datasubmission.button.edit" /></button>

<h3><spring:message code="datasubmission.siteid"/> - <c:out value="${form.siteId}"/></h3>

<spring:message code="datasubmission.description" />

<table style="width:100%;border-spacing:0 5px;" >
<tr>
	<td><spring:message code="datasubmission.label.month" /></td>
	<td>
		<form:select path="month" id="month">
			<form:option value="1"><spring:message code="month.january.abbrev" /></form:option>
			<form:option value="2"><spring:message code="month.february.abbrev" /></form:option>
			<form:option value="3"><spring:message code="month.march.abbrev" /></form:option>
			<form:option value="4"><spring:message code="month.april.abbrev" /></form:option>
			<form:option value="5"><spring:message code="month.may.abbrev" /></form:option>
			<form:option value="6"><spring:message code="month.june.abbrev" /></form:option>
			<form:option value="7"><spring:message code="month.july.abbrev" /></form:option>
			<form:option value="8"><spring:message code="month.august.abbrev" /></form:option>
			<form:option value="9"><spring:message code="month.september.abbrev" /></form:option>
			<form:option value="10"><spring:message code="month.october.abbrev" /></form:option>
			<form:option value="11"><spring:message code="month.november.abbrev" /></form:option>
			<form:option value="12"><spring:message code="month.december.abbrev" /></form:option>
		</form:select>
	</td>
	<td><spring:message code="datasubmission.label.year" /></td>
	<td>
		<form:select path="year" id="year">
			<form:option value="2017">2017</form:option>
			<form:option value="2018">2018</form:option>
			<form:option value="2019">2019</form:option>
			<form:option value="2020">2020</form:option>
		</form:select>
	</td>
	<td><button type="button" onClick="dateChange();">Fetch Date</button></td>
</tr>
<tr>
	<td><b><spring:message code="datasubmission.checkbox.sendindicator"/></b></td>
	<td colspan="3"><b></b></td>
	<td><b></b></td>
</tr>
<c:forEach var="indicator" items="${form.indicators}" varStatus="iter">
	<c:set var="nameKey" value="${indicator.typeOfIndicator.nameKey}"/>
	<c:set var="descriptionKey" value="${indicator.typeOfIndicator.descriptionKey}"/>
<tr class="border_top">
	<td>
		<form:checkbox path="indicators[${iter.index}].sendIndicator" value="true" />
	</td>
	<td colspan="3">
		<span id="<spring:message code="${nameKey} }>" />" class="<c:out value="${indicator.status}" />">
			<b><spring:message code="${nameKey}" /></b>
		</span>
	</td>
	<td >
		<b><spring:message code="${nameKey}" /></b>
	</td>
</tr>
	<c:if test="${empty indicator.dataValue}" ><c:if test="${indicator.dataValue.visible}">
<tr>
	<td></td>
	<td  colspan="3"><c:out value="${indicator.dataValue.value}" /></td>
	<td >	<form:input path="indicators[${iter.index}].dataValue.value"/> </td>
</tr>

	</c:if></c:if>
<tr>
	<td></td>
	<td  colspan="3">
		<spring:message code="${descriptionKey}" />
	</td>
</tr>
	<c:forEach var="resource" items="${indicator.resources}" varStatus="resIter">
		<c:if test="${not empty resource.headerKey}">
<tr>
	<td></td>
	<td  colspan="3"></td>
	<td >
		<spring:message code="${resource.headerKey }" />
	</td>
</tr>
		</c:if>
		<c:forEach var="columnValue" items="${resource.columnValues}" varStatus="valIter">
			<c:if test="${columnValue.visible}">
<tr>
	<td></td>
	<td  colspan="3">
		
	</td>
	<td >
		<form:input path='indicators[${iter.index}].resources[${resIter.index}].columnValues[${valIter.index}].value' />
			<c:if test="${not empty columnValue.displayKey}">
		<spring:message code="${columnValue.displayKey}" />
			</c:if>
	</td>
</tr>

		</c:if>
	</c:forEach>
	</c:forEach>
<tr class="spacerRow"><td>&nbsp;</td></tr>
</c:forEach>
</table>

<button type="button" onclick="saveAndSubmit();"><spring:message code="datasubmission.button.savesubmit" /> </button>
<button type="button" onclick="saveAndExit();"><spring:message code="datasubmission.button.saveexit" /> </button>

