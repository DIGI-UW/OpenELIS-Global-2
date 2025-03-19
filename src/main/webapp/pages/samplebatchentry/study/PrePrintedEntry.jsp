<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page import="org.openelisglobal.common.action.IActionConstants,
                 org.openelisglobal.common.util.Versioning,
				 org.openelisglobal.sample.util.AccessionNumberUtil,
                 org.openelisglobal.internationalization.MessageUtil" %>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<script type="text/javascript">
var lineSeparator = "";
inPrintState = false;

//Adds warning when leaving page
window.onbeforeunload = formWarning;
function formWarning(){ 
	//firefox overwrites any message that is put as a page closing message
	return "Are you sure you want to leave this page?";
}

//check if labNo is valid accession number when a character key is pressed and length is accession length
function checkAccessionNumber(accessionNumber, event) {
	var charPressed = (event.keyCode >=48 && event.keyCode <=57) || (event.keyCode >=65 && event.keyCode <=90) || (event.keyCode >=96 && event.keyCode <=105)
	if (accessionNumber.value.length >= 9 && charPressed) {
      	validateAccessionNumberOnServer(accessionNumber, "initialSample", false, processAccessionSuccessStudy);
	} else if (accessionNumber.value.length < 9) {
		inPrintState = false;
		setSave();
	}
}

//check response from accession number validation
function processAccessionSuccessStudy(xhr) {
  var formField = xhr.responseXML.getElementsByTagName("formfield").item(0);
  var message = xhr.responseXML.getElementsByTagName("message").item(0);
  if (message.firstChild.nodeValue == "valid" || message.firstChild.nodeValue == "SAMPLE_NOT_FOUND" ) {
      inPrintState = true;
      setSave()
  } else {
      alert(message.firstChild.nodeValue);
  }
  var labElement = formField.firstChild.nodeValue;
  selectFieldErrorDisplay(success, document.getElementById(labElement));
}

//what happens when save button is pressed
function saveLabel() {
	if (study == "viralLoad"){
		postBatchSampleByProject('SampleEntryVLSave?type=initial', onPostBatchSampleSuccess, defaultFailure);
	} else if (study == "EID"){
		postBatchSampleByProject('SampleEntryEIDSave?type=initial', onPostBatchSampleSuccess, defaultFailure);
	}
}

//when a successful reponse is retured by the server this does NOT mean the sample was successfully entered
//	as the values are sent to the regular entry page and not a deicated server
function onPostBatchSampleSuccess(xhr) {
	validateAccessionNumberOnServer(document.getElementById('labNo'), "doubleSample", false, checkSampleEnteredSuccess);
}

//check to see if the sample was successfully entered
function checkSampleEnteredSuccess(xhr) {
    var formField = xhr.responseXML.getElementsByTagName("formfield").item(0);
    var message = xhr.responseXML.getElementsByTagName("message").item(0);
    if (message.firstChild.nodeValue == "SAMPLE_FOUND") {
    	moveAccessionToRecentArea();
    	jQuery("#labNo").val("");
        jQuery("#labNo").trigger('keyup');
    	setSave();
    } else {
        alert("<spring:message code='error.notentered' />");    	
    }
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
		<form:input path="labNo" maxlength='9' onkeyup="checkAccessionNumber(this, event);" cssClass="text" id="labNo"/>
		<button type="button" onclick="saveLabel();" id="saveButtonId" disabled="disabled">
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