<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page import="org.openelisglobal.common.action.IActionConstants,
                 org.openelisglobal.internationalization.MessageUtil,
                 org.openelisglobal.sample.bean.SampleOrderItem" %>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



      

<script>
var lineSeparator = "";

//Adds warning when leaving page
window.onbeforeunload = formWarning;
function formWarning(){ 
	//firefox overwrites any message that is put as a page closing message
	return "Are you sure you want to leave this page?";
}

function printLabel() {
	inPrintState = false;
	setSave();
	jQuery("#nextButtonId").prop('disabled', false);
	var labNo = jQuery("#labNo").val();
	var patientId = jQuery("#lastPatientId").val();
	jQuery("#barcodeArea").show();
	//make request to LabelMakerServlet
	var src = "LabelMakerServlet?labNo=" + labNo + "&patientId=" + patientId;
	jQuery("#ifbarcode").attr('src', src);
}

function nextLabel() {
	inPrintState = true;
	setSave();
	jQuery("#nextButtonId").prop('disabled', true);
	moveAccessionToRecentArea();
	jQuery("#labNo").val("");
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


//functions for generating and checking accession number
function getNextAccessionNumber() {
    generateNextScanNumber(processScanSuccess, defaultFailure);
}

function checkAccessionNumber(accessionNumber) {
    //check if empty
    if (!fieldIsEmptyById("labNo")) {
        validateAccessionNumberOnServer(false, false, accessionNumber.id, accessionNumber.value, processAccessionSuccess, null);
    } else {
         selectFieldErrorDisplay(false, document.getElementById("labNo"));
    }
}

//parses values once generateNextScanNumber returns from server
function processScanSuccess(xhr) {
    var formField = xhr.responseXML.getElementsByTagName("formfield").item(0);
    var returnedData = formField.firstChild.nodeValue;
    var message = xhr.responseXML.getElementsByTagName("message").item(0);
    var success = message.firstChild.nodeValue == "valid";
    if (success) {
        jQuery("#labNo").val(returnedData);
    } else {
        alert("<%= MessageUtil.getMessage("error.accession.no.next") %>");
        jQuery("#labNo").val("");
    }
    selectFieldErrorDisplay(success, document.getElementById("labNo"));
    setValidIndicaterOnField(success, "labNo");
    jQuery("#labNo").trigger('change');
}

//called once accession number is validated on server
function processAccessionSuccess(xhr) {
    var formField = xhr.responseXML.getElementsByTagName("formfield").item(0);
    var message = xhr.responseXML.getElementsByTagName("message").item(0);
    if (message.firstChild.nodeValue == "valid") {
        postBatchSample(onPostBatchSampleSuccess, defaultFailure);
    } else {
        alert(message.firstChild.nodeValue);
    }
    var labElement = formField.firstChild.nodeValue;
    selectFieldErrorDisplay(success, document.getElementById(labElement));
}

function onPostBatchSampleSuccess() {
	printLabel();
// 	setPatientThenPrintLabel();
}

function setPatientThenPrintLabel() {
	var splitName;
    var lastName = "";
    var firstName = "";
    var STNumber = "";
    var subjectNumber = "";
    var nationalID = "";
    var labNumber = "";
    
    var dateOfBirth = "";
    var age = "";
    var gender = "";
	labNumber = jQuery("#labNo").val();
    
    dateOfBirth = jQuery("#dateOfBirthSearchValue").val().trim();
    gender = jQuery("#searchGendersSearchValues").val().trim();
    jQuery("#searchLabNumber").val(labNumber);
    patientSearch(lastName, firstName, STNumber, subjectNumber, nationalID, labNumber, "", dateOfBirth, gender, false, processSearchSuccessPrint);
}

//set patient id in hidden field before making request to LabelMakerServlet, 
//so that barcode request contains all needed information
function processSearchSuccessPrint(xhr) {
	var formField = xhr.responseXML.getElementsByTagName("formfield").item(0);
	var message = xhr.responseXML.getElementsByTagName("message").item(0);
	if( message.firstChild.nodeValue == "valid" ) {
		var resultNode = formField.getElementsByTagName("result").item(0);
		var id = resultNode.getElementsByTagName("id").item(0);
		jQuery("#lastPatientId").val(id.firstChild.nodeValue);
	}
	if (window.hasIdentifyingInfo) {
		//fill out patient management form if any "identifying info" was provided
		if (hasIdentifyingInfo() && !jQuery("#patientPK_ID").val()) {
			processSearchSuccess(xhr);
			searchDone = true;
		} 
	}
	printLabel();
}
</script>

<h2><spring:message code="sample.batchentry.ondemand.header.print"/></h2>
<table style="width:100%;">
<tr>
	<td>
		<%-- gets next accession, and calls submit and print if success --%>
		<button type="button" onclick="getNextAccessionNumber();"
			id="saveButtonId">
			<spring:message code="sample.batchentry.ondemand.saveprint" />
		</button>
		<%-- sets up for next label to be printed --%>
		<button type="button" onclick="nextLabel();"
			id="nextButtonId"
			disabled="disabled">
			<spring:message code="sample.batchentry.ondemand.next" />
		</button>
	</td>
</tr>
<tr>
	<td> <br></td>
</tr>
<tr>
	<td>
		<spring:message code="sample.batchentry.ondemand.current" />:
	</td>
</tr>
<tr>
	<td>
		<form:input path="sampleOrderItems.labNo"
           	onchange="checkAccessionNumber(this);"
            cssClass="text"
            id="labNo"
            readonly="true"/>
	</td>
</tr>
<tr>
	<td>
		<table>
			<tr>
				<td>
					<spring:message code="sample.batchentry.ondemand.previous" />:
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
		<div style="display:none;" id="barcodeArea">
			<h2><spring:message code="barcode.common.section.barcode.header"/></h2>
			<iframe  src="about:blank" id="ifbarcode" width="100%" height="300px"></iframe>
		</div>
	</td>
</tr>
</table>