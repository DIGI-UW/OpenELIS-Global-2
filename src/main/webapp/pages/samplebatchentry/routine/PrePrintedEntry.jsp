<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page import="org.openelisglobal.common.action.IActionConstants,
				 org.openelisglobal.sample.util.AccessionNumberUtil" %>
                 
<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<script type="text/javascript">
var lineSeparator = "";
var minAccessionLength = <%= AccessionNumberUtil.getMinAccessionLength()%>; 
inPrintState = false;

//Adds warning when leaving page
window.onbeforeunload = formWarning;
function formWarning(){ 
	//firefox overwrites any message that is put as a page closing message
	return "Are you sure you want to leave this page?";
}

function saveLabel() {
	postBatchSample(onPostBatchSampleSuccess, defaultFailure);
}

function onPostBatchSampleSuccess() {
	setPatient();
	moveAccessionToRecentArea();
	jQuery("#labNo").val("");
    jQuery("#labNo").trigger('keyup');
	setSave();
}

//Add accession number to recent area
function moveAccessionToRecentArea() {
	var $recentTextArea = jQuery("#recentSummary");
	if ($recentTextArea.val()) {
		lineSeparator = "\n";
	}
	var newRecent = jQuery("#labNo").val() + lineSeparator + $recentTextArea.val();
	if ((newRecent.match(/\n/g)||[]).length >= 3) {
		newRecent = newRecent.slice(0,newRecent.lastIndexOf("\n"));
	}
	$recentTextArea.val(newRecent);
}

//check if labNo is valid accession number when a character key is pressed and length is accession length
function checkAccessionNumber(accessionNumber) {
	if (accessionNumber.value.length >= minAccessionLength) {
      	validateAccessionNumberOnServer(false, false, accessionNumber.id, accessionNumber.value, processAccessionSuccess, null);
	} else if (accessionNumber.value.length < minAccessionLength) {
		inPrintState = false;
		setSave();
	}
}

//check response from accession number validation
function processAccessionSuccess(xhr) {
    var formField = xhr.responseXML.getElementsByTagName("formfield").item(0);
    var message = xhr.responseXML.getElementsByTagName("message").item(0);
    if (message.firstChild.nodeValue == "valid") {
        inPrintState = true;
        setSave()
    } else {
        alert(message.firstChild.nodeValue);
        inPrintState = false;
        setSave()
    }
    var labElement = formField.firstChild.nodeValue;
    selectFieldErrorDisplay(success, document.getElementById(labElement));
}
</script>
<h2><spring:message code="sample.batchentry.preprinted.header.entry" /></h2>

<table>
<tr>
	<td>
		<spring:message code="sample.batchentry.preprinted.labno" />:
	</td>
</tr>
<tr>
	<td>
		<form:input path="sampleOrderItems.labNo"
			maxlength='<%= Integer.toString(AccessionNumberUtil.getMaxAccessionLength())%>'
            onchange="checkAccessionNumber(this);"
            styleClass="text"
            id="labNo"/>
		<button type="button" onclick="saveLabel();"
			id="saveButtonId" 
			disabled="disabled">
		<spring:message code="sample.batchentry.preprinted.save" />
		</button>
	</td>
</tr>
<tr>
	<td> <br></td>
</tr>
<tr>
	<td>
		<table>
			<tr>
				<td>
					<spring:message code="sample.batchentry.preprinted.summary" />:
				</td>
			</tr>
			<tr>
				<td>
					<textarea id="recentSummary" 
						rows="5" 
						cols="50"
						readonly="readonly"></textarea>
				</td>
			</tr>
		</table>
	</td>
</tr>
</table>