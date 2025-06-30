<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<style>

.well {
 	width: 5rem; 
 	height:5rem; 
	cursor: pointer;
	background-size:7rem 7rem;
	background-repeat: no-repeat;
	padding: 1rem;
}

.inactiveWell {
	background-image:url(./images/circle_transparent.png);
}

.activeWell {
	color: #336699;
	background-image: url(./images/circle_clicked.png)
}

.well-label {
	font-weight: bold
}

.well-value {
}
</style>

<script type="text/javascript" src="./scripts/ajaxCalls.js"></script>
<script>

function getCurActiveIdentifier() {
	return jQuery(".activeWell").attr('identifier');
}

function getAnalyzerId() {
	return jQuery("#analyzerId").val();
}

function getTestLOINC() {
	return jQuery("#analyzerTests_" + getAnalyzerId() + "_testSelect").val();;
}

function getNumWellColumns() {
	return jQuery("#wellColumns_" + getAnalyzerId()).val();
}

function getNumWellRows() {
	return jQuery("#wellRows_" + getAnalyzerId()).val();
}

function getTestName() {
	return jQuery("#analyzerTests_" + getAnalyzerId() + " option:selected").text()
}

function getDateString(date) {
	var yyyy = date.getFullYear();
	var mm = date.getMonth() + 1 < 10 ? "0" + (date.getMonth() + 1) : date.getMonth() + 1;
	var dd = date.getDate() < 10 ? "0" + date.getDate() : date.getDate();
	var hh = date.getHours() < 10 ? "0" + date.getHours() : date.getHours();
	var MM = date.getMinutes() < 10 ? "0" + date.getMinutes() : date.getMinutes();
	
	return yyyy 
		+ "-" + mm
		+ "-"  + dd
		+ " " + hh
		+ ":" + MM;
}

function setTotalEmpty() {
	var total = getNumWellColumns() * getNumWellRows();
	var filled = jQuery(".filledWell").length;
	var empty = total - filled;
	jQuery(".emptyTotalNum").text(empty);
}

function setupNewExperiment() {
	window.beforeunload = "warning";
	jQuery("#step1").hide();
	setDefaultFilename();
	setTestNameForDisplay();
	jQuery("#step2").show();
}

function setDefaultFilename() {
	var dateString = getDateString(new Date());
	var testName = getTestName();
	var analyzerName = jQuery("#analyzerId option:selected").text();
	setFilename( dateString + " " + testName + " " + analyzerName);
}

function setTestNameForDisplay() {
	jQuery("#testName").val(getTestName());
}

function setFilename(filename) {
	jQuery("#filename").val(filename)
}

function setWell(well) {
	clearDisplayValues();
	jQuery(".well").removeClass("activeWell");
	jQuery(".well").addClass("inactiveWell");
	well.classList.add("activeWell");
	well.classList.add("filledWell");
	jQuery(".activeWell").removeClass("inactiveWell");
	if  (jQuery(".activeWell > .well-value-full").val()) {
		setSampleValuesFromXML(new DOMParser().parseFromString(jQuery(".activeWell > .well-value-full").val(), 'application/xml'));
	}
	jQuery("#selectPatientButtonID").prop("disabled", false);
	jQuery("#cposButton").prop("disabled", false);
	jQuery("#cnegButton").prop("disabled", false);
}

function clearDisplayValues() {
	jQuery("#summaryId").hide();
	jQuery(".sampleInfo").text("");
	jQuery(".patientInfo").text("");
	
}

function loadExperiment() {
	window.beforeunload = "warning";
	jQuery("#downloadSetup").attr('disabled',false);
	jQuery("#printSetup").attr('disabled',false);
	jQuery("#experimentId").val(jQuery("#previousRun").val());
	setDefaultFilename();
	setTestNameForDisplay();
	getPreviousExperimentSetup(jQuery("#previousRun").val(), loadExperimentSuccess);
}

function loadExperimentSuccess(xhr) {
	jQuery("#step1").hide();
	jQuery("#step2").show();
	var responseText = xhr.responseText;
	var jsonResponse = JSON.parse(responseText);
	Object.keys(jsonResponse).forEach(function(key) {
		if (!jsonResponse[key]) {
			
		} else if (jsonResponse[key] != "CNEG" && jsonResponse[key] != "CPOS") {
			searchForLabNo(jsonResponse[key], key);
		} else if (jsonResponse[key] == "CNEG" ) {
			setNegative(key);
		} else if (jsonResponse[key] == "CPOS" ) {
			setPositive(key);
		}
	});
}

//this one is used solely when loading a previous experiment setup
function searchForLabNo(labNumber, identifier) {
	patientSearch("", "", "", "", "", labNumber, "", "", "", false, searchByLabNoSuccess, null, [identifier, labNumber]);
}

function searchForPatientByLabNo(labNumber) {
	patientSearch("", "", "", "", "", labNumber, "", "", "", false, searchForPatientByLabNoSuccess, null, labNumber);
}

function searchByLabNoSuccess(xhr, extraParams) {
	var formField = xhr.responseXML.getElementsByTagName("formfield").item(0);
	var message = xhr.responseXML.getElementsByTagName("message").item(0);
	var resultNodes = formField.getElementsByTagName("result");

	//should always be length of one since we searched by accesion number
	for( var i = 0; i < resultNodes.length; i++ ) {
		setPatientValues(resultNodes.item(i));
	}
	handleSelectedPatientAlt(extraParams[0], extraParams[1]);
}

function searchForPatientByLabNoSuccess(xhr, extraParams) {
	var formField = xhr.responseXML.getElementsByTagName("formfield").item(0);
	var message = xhr.responseXML.getElementsByTagName("message").item(0);
	var resultNodes = formField.getElementsByTagName("result");

	//should always be length of one since we searched by accesion number
	for( var i = 0; i < resultNodes.length; i++ ) {
		setPatientValues(resultNodes.item(i));
	}
}

function setPatientValues(result) {
	var patient = result.getElementsByTagName("patient")[0];

	var firstName = getValueFromXmlElement( patient, "first");
	var lastName = getValueFromXmlElement( patient, "last");
	var gender = getValueFromXmlElement( patient, "gender");
	var DOB = getValueFromXmlElement( patient, "dob");
	var stNumber = getValueFromXmlElement( patient, "ST");
	var subjectNumber = getValueFromXmlElement( patient, "subjectNumber");
	var nationalID = getValueFromXmlElement( patient, "nationalID");
	var mother = getValueFromXmlElement( patient, "mother");
	var pk = getValueFromXmlElement( result, "id");
	var dataSourceName = getValueFromXmlElement( result, "dataSourceName");
	

	jQuery("#patNameSpan").text(lastName + ', ' + firstName);
	jQuery("#patSubjectIdSpan").text(subjectNumber);

}

function handleSelectedPatientAlt(identifier, accessionNumber) {
	if (!identifier) {
		identifier = getCurActiveIdentifier();
	}
	if (!accessionNumber) {
	    if(jQuery("#patientLabNoSearchValue").val()){//lab number
	        accessionNumber = jQuery("#patientLabNoSearchValue").val();
	    }
	}
    var patientID = patientSelectID ? patientSelectID : "";
//     var searchUrl = '${form.formAction}'.sub('Form','') + "?accessionNumber=" + accessionNumber + "&patientID=" + patientID;
//     if( !(typeof requestType === 'undefined') ){
//     	searchUrl += "&type=" + requestType;
//     }
    getSampleForLabOrderOrPatientWithTestLOINC(accessionNumber, patientID, getTestLOINC(), true, selectedPatientGetSampleSuccess, null, identifier);
}

function selectedPatientGetSampleSuccess(xhr, identifier) {
	setSampleValuesFromXML(xhr.responseXML, identifier);
}

function setSampleValuesFromXML(xml, identifier) {
	$("searchResultsDiv").hide();
	if (xml.getElementsByTagName("formfield").item(0).firstChild.nodeValue === 'empty') {
		alert('could not be found');
	} else {
		var labNo = xml.getElementsByTagName("labNo").item(0).firstChild.nodeValue;
		setLabInfoForWell(labNo, xml, identifier);
		searchForPatientByLabNo(labNo);
	}
}

function setLabInfoForWell(labNo, xml, identifier) {
	jQuery("#well_" + identifier + " > .well-value").text(labNo);
	jQuery("input[name='wellValues[" + identifier + "]']").val(labNo);
	jQuery("#well_" + identifier + " > .well-value-full").val(new XMLSerializer().serializeToString(xml));
	jQuery("#summaryId").show();
	jQuery("#labNoSpan").text(labNo);
	if (labNo != "CPOS" && labNo != "CNEG") {
		var receivedDate = xml.getElementsByTagName("receivedDateForDisplay").item(0).firstChild.nodeValue;
		var receivedTime = xml.getElementsByTagName("receivedTimeForDisplay").item(0).firstChild.nodeValue;
		jQuery("#receivedSpan").text(receivedDate + ' ' + receivedTime);
	}
}

function saveExperiment() {
	setupExperimentFile(saveExperimentSuccess)
}

function saveExperimentSuccess(xhr) {
	var responseText = xhr.responseText;
	console.log("experiment id: " + responseText);
	jQuery("#experimentId").val(responseText);
	jQuery("#downloadSetup").attr('disabled',false);
	jQuery("#printSetup").attr('disabled',false);
}

function downloadSetupFile() {
	const params = new URLSearchParams();
	params.append('report', 'MauritiusProtocolSheet');
	params.append('filename', jQuery("#filename").val());
	params.append('experimentId', jQuery("#experimentId").val());
	
	window.open("AnalyzerSetupFile/" + jQuery("#experimentId").val() + "?" + params.toString(), '_blank');
}

function printSetupFile() {
	const params = new URLSearchParams();
	params.append('report', 'MauritiusProtocolSheet');
	params.append('reportName', jQuery("#filename").val());
	params.append('experimentId', jQuery("#experimentId").val());
	
	window.open("ReportPrint?" + params.toString(), '_blank');
}

function setPositive(identifier) {
	var xml = new DOMParser().parseFromString("<fieldmessage><formfield>found</formfield><labNo>CPOS</labNo></fieldmessage>", 'application/xml');
	if (identifier) {
		setSampleValuesFromXML(xml, identifier);
	} else {
		setSampleValuesFromXML(xml, getCurActiveIdentifier());
	}
}

function setNegative(identifier) {
	var xml = new DOMParser().parseFromString("<fieldmessage><formfield>found</formfield><labNo>CNEG</labNo></fieldmessage>", 'application/xml');
	if (identifier) {
		setSampleValuesFromXML(xml, identifier);
	} else {
		setSampleValuesFromXML(xml, getCurActiveIdentifier());
	}
}

function showSelectedTestRow() {
	jQuery(".testSelectRow").hide();
	jQuery("#analyzerTests_" + getAnalyzerId()).show();
}

function generateTable() {
	var table = document.getElementById("experimentInputTable");
	generateColGroup(table);
	generateTableHeader(table, getNumWellColumns());
	generateTableBody(table, getNumWellRows(), getNumWellColumns());
}

function generateColGroup(table) {
	var colGroup = document.createElement('colgroup');
	var col = document.createElement('col')
	col.setAttribute("style", "color: #336699; background-color: #C8DADA; width: 1rem;")
	col.setAttribute('span', '1');
	colGroup.appendChild(col)
	table.appendChild(colGroup)
}

function generateTableHeader(table, numColumns) {
	var thead = table.createTHead();
	var row = thead.insertRow();
	row.setAttribute('style', 'text-align: center;')
	row.appendChild(createTH(""));
	for ( curCol = 0; curCol < numColumns; ++curCol ) {
		row.appendChild(createTH(curCol + 1));
	}
}

function generateTableBody(table, numRows, numColumns) {
	for (curRow = 0; curRow < numRows; ++curRow) {
		var row = table.insertRow();
		row.setAttribute('style', 'text-align: center;')
	    var cell = row.insertCell();
	    cell.appendChild(document.createTextNode(numToAlpha(curRow)));
		for (curCol = 0; curCol < numColumns; ++curCol) {
		    var cell = row.insertCell();
		    var identifier = numToAlpha(curRow) + (curCol + 1);
		    cell.setAttribute('id', 'well_' + identifier);
		    cell.setAttribute('identifier', identifier);
		    cell.setAttribute('onClick', 'setWell(this)');
		    cell.setAttribute('class', 'well inactiveWell');
		    cell.setAttribute('style', 'word-wrap:break-word');
		    var div = document.createElement('div');
		    div.setAttribute('class', 'well-label');
		    div.appendChild(document.createTextNode(identifier));
		    cell.appendChild(div);
		    div = document.createElement('div');
		    div.setAttribute('class', 'well-value');
		    cell.appendChild(div);
		    var sentInput = document.createElement('input');
		    sentInput.setAttribute('type', 'hidden');
		    sentInput.setAttribute('name', 'wellValues[' + identifier + ']');
		    sentInput.setAttribute('class', 'well-value-sent');
		    var mainForm = document.getElementById("mainForm");
		    mainForm.appendChild(sentInput);
		    var input = document.createElement('input');
		    input.setAttribute('class', 'well-value-full');
		    input.setAttribute('type', 'hidden');
		    cell.appendChild(input);
		}
	}
}

function createTH(data) {
	th = document.createElement("th");
	th.appendChild(document.createTextNode(data));
	return th;
}

function numToAlpha(num) {
	if (num < 0) {
		return -1;
	} 
	if (num > 25) {
		return numToAlpha(Math.floor(num/26 - 1)) + numToAlpha(num % 26);
	}
	return String.fromCharCode(num + 65);
}

jQuery( document ).ready(function() {
	generateTable();
	jQuery("#downloadSetup").attr('disabled',true);
	jQuery("#printSetup").attr('disabled',true);
	jQuery("#selectPatientButtonID").prop("disabled", true);
	jQuery("#cposButton").prop("disabled", true);
	jQuery("#cnegButton").prop("disabled", true);
	jQuery("#summaryId").hide();
	jQuery("#step2").hide();
	showSelectedTestRow();
	setTotalEmpty();
});

</script>
<input id="experimentId" type="hidden" name="experimentId"/>

<h2>
<spring:message code="" text="Analyzer Run Setup"/>
</h2>

<table id="step1" style="width: 80%;">
	<tr>
		<td>
			<spring:message code="" text="Analyzer Template Name"/>
		</td>
		<td>
			<form:select path="analyzerId" id="analyzerId" onChange="showSelectedTestRow()" >
			<form:options items="${form.analyzers}" itemLabel="label" itemValue="value"/>
			</form:select>
			<c:forEach items="${form.analyzersWellInfo}" var="analyzerWellInfo" varStatus="iter">
				<input type="hidden" id="wellColumns_${analyzerWellInfo.key}" value="${analyzerWellInfo.value.wellColumns}"/>
				<input type="hidden" id="wellRows_${analyzerWellInfo.key}" value="${analyzerWellInfo.value.wellRows}"/>
			</c:forEach>
		</td>
		<td>
		</td>
	</tr>
	<c:forEach items="${form.analyzersTests}" var="analyzerTests" varStatus="iter">
	<tr id="analyzerTests_${analyzerTests.key}" class="testSelectRow">
		<td>
			<spring:message code="" text="Test name"/>
		</td>
		<td>
			<select id="analyzerTests_${analyzerTests.key}_testSelect">
				<c:forEach items="${analyzerTests.value}" var="test" varStatus="test_iter">
				<option value="${test.value}">${test.label}</option>
				</c:forEach>
			</select>
		</td>
		<td>
		</td>
	</tr>
	</c:forEach>
	<tr>
		<td style="padding-bottom:2em;"></td>
	</tr>
	<tr style="vertical-align: top;">
		<td>
		</td>
		<td>
			<button type="button" onClick="setupNewExperiment()" ><spring:message code="" text="Setup New Experiment"/></button>
		</td>
		<td>
			<form:select path="previousRun" id="previousRun">
				<form:options items="${form.previousRuns}" itemLabel="label" itemValue="value"/>
			</form:select>
			<br>
			<br>
			<button type="button" onClick="loadExperiment()"><spring:message code="" text="Load Experiment"/></button>
		</td>
	</tr>
</table>

<table id="step2">
	<tr style="vertical-align: top;">
		<td>
			<table>
			<tr>
				<td>
					<spring:message code="" text="Analyzer Run Label"/>
					<form:input id="filename" path="filename" />
				</td>
			</tr>
			<tr>
				<td>
					<spring:message code="" text="Test Name"/>
					<input id="testName" readonly="readonly"/>
				</td>
			</tr>
			<tr>
				<td>
					<button id="cposButton" type="button" onClick="setPositive()"><spring:message code="" text="Positive Control Sample"/></button>
					<button id="cnegButton" type="button" onClick="setNegative()"><spring:message code="" text="Negative Control Sample"/></button>
				</td>
			</tr>
			</table>
			<jsp:include page="${patientEnhancedSearchFragment}"/>
			<table id="summaryId">
			<tr>
				<td>
					<b><spring:message code="" text="Sample Info"/></b>
				</td>
			</tr>
			<tr>
				<td>
					<spring:message code="" text="Lab No."/>:
					<span id="labNoSpan" class="sampleInfo"></span>
				</td>
				<td>
					<spring:message code="" text="Received"/>:
					<span id="receivedSpan" class="sampleInfo"></span>
				</td>
			</tr>
			<tr>
				<td>
					<b><spring:message code="" text="Patient Info"/></b>
				</td>
			</tr>
			<tr>
				<td>
					<spring:message code="" text="Patient Name"/>:
					<span id="patNameSpan" class="patientInfo"></span>
				</td>
				<td>
					<spring:message code="" text="Subject Number"/>:
					<span id="patSubjectIdSpan" class="patientInfo"></span>
				</td>
			</tr>
				
			</table>
		</td>
		<td>
	<!-- 		<div style="overflow:scroll; width: 70rem;"> -->
					<table id="experimentInputTable"  style="width: 85rem; text-align: center; table-layout: fixed;"></table>
					<div style="float:right;"><span class="emptyTotalNum"></span> <spring:message code="" text="Empty"/></div>
	<!-- 		</div> -->
		</td>
	</tr>
	<tr>
		<td colspan="2">
			<button type="button" onClick="saveExperiment();"><spring:message code="" text="Save Experiment"/></button>
			<button type="button" id="downloadSetup" onClick="downloadSetupFile();" disabled="disabled"><spring:message code="" text="Download Experiment Setup File"/></button>
			<button type="button" id="printSetup" onClick="printSetupFile();" disabled="disabled"><spring:message code="" text="Print Protocol Sheet"/></button>
		</td>
	</tr>
</table>

