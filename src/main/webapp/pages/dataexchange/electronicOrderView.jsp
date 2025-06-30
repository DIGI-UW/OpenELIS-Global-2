<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ page import="org.openelisglobal.common.util.DateUtil" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
 

<link href="select2/css/select2.min.css" rel="stylesheet" />
<script type="text/javascript" src="select2/js/select2.min.js"></script>
<script type="text/javascript" src="scripts/tableSort.js"></script>
<script type="text/javascript" src="scripts/utilities.js"></script>
<script type="text/javascript" src="scripts/ajaxCalls.js"></script>
<script type="text/javascript" src="scripts/jquery-ui.js?"></script>
<script type="text/javascript">

var colSort = [0,0,0,0,0,0,0,0,0,0,0,0];
var dirty = false;
var entering = false;

function makeDirty() {
    dirty = true;
    function formWarning() {
        return "<spring:message code="banner.menu.dataLossWarning"/>";
    }
    window.onbeforeunload = formWarning;
}

function searchByIdentifier() {
	const params = new URLSearchParams({
		searchType: "IDENTIFIER",
		searchValue: jQuery('#searchValue').val(),
		useAllInfo: jQuery('#allInfo1').is(':checked'),
		});
	
	window.location.href = "ElectronicOrders?" + params.toString();

}

function searchByDateAndStatus() {
	const params = new URLSearchParams({
		searchType: "DATE_STATUS",
		startDate: jQuery('#startDate').val(),
		endDate: jQuery('#endDate').val(),
		statusId: jQuery('#statusId').val(),
		useAllInfo: jQuery('#allInfo2').is(':checked'),
		});
	
	window.location.href = "ElectronicOrders?" + params.toString();

}

function sort(col) {
	if (entering) {
		alert('No sorting while data is being entered');
	} else {
		sortTableJquery('eOrderTable', col, ((colSort[col - 1]++) % 2) != 0);
	}
}


function enterOrder(index) {
	entering = true;
	var externalOrderId = jQuery('#externalOrderId_' + index).val();
	
	var tBody = document.getElementById('eOrderTableBody');
	var eOrderRow = document.getElementById('eOrderRow_' + index);
	eOrderRow.style.display = 'none';
	sortedRowIndex = eOrderRow.rowIndex;
	var row = tBody.insertRow(sortedRowIndex - 1);
	row.id = 'filler-row_' + index;
	row.style.display = 'none';
	row = tBody.insertRow(sortedRowIndex - 1);
	row.id = 'enter-row_' + index;
	var cell = row.insertCell();
	cell = row.insertCell();
	cell.colSpan = '11';
	var inputLabelHTML = 'Lab Number';
	var inputHTML = '<input type="text" id="eOrderLabNumber_' + index + '" name="eOrderLabNumber_' + index + '" onchange="makeDirty()"/>';
	var instructionHTML = '<spring:message code="sample.entry.scanner.instructions" htmlEscape="false"/>';
    var generateButton = '<button type="button" id="generateAccessionButton" onclick="getNextAccessionNumber(' + index + ');" class="textButton"><spring:message code="sample.entry.scanner.generate" htmlEscape="false"/></button>';
	var saveHTML = '<button type="button" onclick="saveEntry(\'' + index + '\')">Save</button>';
	var cancelHTML = '<button type="button" onclick="restoreRow(\'' + index + '\');">Cancel</button>';
// 	var checkboxHTML = '<input type="checkbox" name="printBarcode_' + index + '"/>';
	var checkboxHTML = '';
// 	var checkboxLabelHTML = 'Print Barcode Labels';
	var checkboxLabelHTML = '';
	
	cell.innerHTML = inputLabelHTML + inputHTML + instructionHTML + generateButton + saveHTML + cancelHTML + checkboxHTML + checkboxLabelHTML;
}

function restoreRow(index) {
	var table = document.getElementById('eOrderTable');
	var fillerRow = document.getElementById('filler-row_' + index);
	table.deleteRow(fillerRow.rowIndex);
	var enterRow = document.getElementById('enter-row_' + index);
	table.deleteRow(enterRow.rowIndex);
	var eOrderRow = document.getElementById('eOrderRow_' + index);
	eOrderRow.style.display = '';
}

function saveEntry(index) {
	entering = false;
	
    var labNumber = jQuery("#eOrderLabNumber_" + index).val();
	var externalOrderId = jQuery('#externalOrderId_' + index).val();

	restoreRow(index);
	markRowOutOfSync(index);
	
	window.open('SamplePatientEntry?ID=' + externalOrderId + '&labNumber=' + labNumber + '&attemptAutoSave=true', "_blank");
}

function markRowOutOfSync(index) {
	jQuery('#enterButton_' + index).attr('disabled', 'disabled');
	jQuery('#editButton_' + index).attr('disabled', 'disabled');
	jQuery('#eOrderRow_' + index).addClass('unsynced-resource');
}

function getNextAccessionNumber(index) {
    generateNextScanNumber(processScanSuccess, false, index);
}

function processScanSuccess(xhr, index) {
    var formField = xhr.responseXML.getElementsByTagName("formfield").item(0);
    var returnedData = formField.firstChild.nodeValue;

    var message = xhr.responseXML.getElementsByTagName("message").item(0);

    var success = message.firstChild.nodeValue == "valid";

    if (success) {
        jQuery("#eOrderLabNumber_" + index).val(returnedData);

    } else {
        alert("<spring:message code="error.accession.no.next"/>");
        $("labNo").value = "";
    }

}

function editOrder(index) {
	var externalOrderId = jQuery('#externalOrderId_' + index).val();
	location.href='SamplePatientEntry?ID=' + externalOrderId;
}

jQuery(document).ready( function() {
	jQuery('.basic-multiselect').select2();
});
</script>

<form:hidden id="searchType" path="searchType"/>

Search for Test Requests
<br>
Search by family name, national ID number, lab number from referring lab, or passport number
<br>
<form:input path="searchValue" id="searchValue" /> 
<button type="button" onclick="searchByIdentifier()"><spring:message code="label.button.search" /></button>
<form:checkbox path="useAllInfo" id="allInfo1" value="true"/> <spring:message code="label.eorder.allinfo" text="get all info" />
<hr>
Test Requests by Date, and Status
Enter the date range for test requests. This will search by the date of the referral, or the order date of the electronic request

<br> 
Start Date <%=DateUtil.getDateUserPrompt()%>
<form:input path="startDate" onkeyup="addDateSlashes(this,event); "/>
End Date <%=DateUtil.getDateUserPrompt()%>
<form:input path="endDate"	onkeyup="addDateSlashes(this,event); "/>
Status
<form:select path="statusId">
<option value="">All Statuses</option>
<form:options items="${form.statusSelectionList}" itemLabel="value" itemValue="id"/>
</form:select>
<br>
<button type="button" onclick="searchByDateAndStatus()"><spring:message code="label.button.search" /></button>
<form:checkbox path="useAllInfo" id="allInfo2" value="true" /> <spring:message code="label.eorder.allinfo" text="get all info" />
<br>

<c:if test="${empty form.eOrders && form.searchFinished}">
    <h2><spring:message code="referral.noreferralDisplayItem"/></h2>
</c:if>
<c:if test="${not empty form.eOrders}">
Test Requests Matching Search - Select Enter to add a lab number and accept the order as is, or select Edit to view more details or make changes
<br>
<table>
<tr class='unsynced-resource'>
<td>
highlighted rows specifies a resource that is likely out of sync with the server
</td>
</tr>
</table>
<table id="eOrderTable"  class='alt-color-table'>
<thead>
<tr>
	<th style="background-color:white;"></th>
    <th class='split-content'>
    	Request Date
    	<span class="fa" onclick='sort(1)'><i class="fas fa-sort"></i></span>
    </th>
    <th class='split-content'>
    	Last Name
    	<span class="fa" onclick='sort(2)'><i class="fas fa-sort"></i></span>
    </th>
    <th class='split-content'>
    	First Name
    	<span class="fa" onclick='sort(3)'><i class="fas fa-sort"></i></span>
    </th>
    <th class='split-content'>
    	National ID
    	<span class="fa" onclick='sort(4)'><i class="fas fa-sort"></i></span>
    </th>
    <th class='split-content'>
    	Requesting Facility
    	<span class="fa" onclick='sort(5)'><i class="fas fa-sort"></i></span>
    </th>
	<th class='split-content'>
    	Priority
    	<span class="fa" onclick='sort(6)'><i class="fas fa-sort"></i></span>
    </th>
    <th class='split-content'>
    	Status
    	<span class="fa" onclick='sort(7)'><i class="fas fa-sort"></i></span>
    </th>
    <th class='split-content'>
    	Test Name
    	<span class="fa" onclick='sort(8)'><i class="fas fa-sort"></i></span>
    </th>
    <th class='split-content'>
    	Referring Lab Number
    	<span class="fa" onclick='sort(9)'><i class="fas fa-sort"></i></span>
    </th>
    <th class='split-content'>
    	Passport Number
    	<span class="fa" onclick='sort(10)'><i class="fas fa-sort"></i></span>
    </th>
    <th class='split-content'>
    	Subject Number
    	<span class="fa" onclick='sort(11)'><i class="fas fa-sort"></i></span>
    </th>
    <th class='split-content'>
    	Lab Number
    	<span class="fa" onclick='sort(12)'><i class="fas fa-sort"></i></span>
    </th>
	<th style="background-color:white;"></th>
</tr>
</thead>
<tbody id="eOrderTableBody">
	<c:forEach items="${form.eOrders}" var="eOrder" varStatus="iter">
		<c:set var="entered" value="${not empty eOrder.labNumber}"/>
		<tr id='eOrderRow_${iter.index}'> 
	    <td style="background-color:white;">
	    	<form:hidden id="externalOrderId_${iter.index}" path="eOrders[${iter.index}].externalOrderId"  />
	    	<button type="button" id="enterButton_${iter.index}" onclick="enterOrder('${iter.index}')" ${entered ? 'disabled="disabled"' : '' }>Enter</button>
		    <button type="button" id="editButton_${iter.index}" onclick="editOrder('${iter.index}')" ${entered ? 'disabled="disabled"' : '' }>Edit</button>
	    </td>
	    <td class="dateCol">
	       <c:out value="${eOrder.requestDateDisplay}"/>
	    </td>
	    <td>
	       <c:out value="${eOrder.patientLastName}"/>
	    </td>
	    <td>
	       <c:out value="${eOrder.patientFirstName}"/>
	    </td>
	    <td>
	       <c:out value="${eOrder.patientNationalId}"/>
	    </td>
	    <td>
	       <c:out value="${eOrder.requestingFacility}"/>
	    </td>
		<td>
	       <c:out value="${eOrder.priority}"/>
	    </td>
	    <td>
	       <c:out value="${eOrder.status}"/>
	    </td>
	    <td>
	       <c:out value="${eOrder.testName}"/>
	    </td>
	    <td>
	       <c:out value="${eOrder.referringLabNumber}"/>
	    </td>
	    <td>
	       <c:out value="${eOrder.passportNumber}"/>
	    </td>
	    <td>
	       <c:out value="${eOrder.subjectNumber}"/>
	    </td>
	    <td>
	       <c:out value="${eOrder.labNumber}"/>
	    </td>
	    <td style="background-color:white;">
			<c:if test="${not empty eOrder.warnings}">
				<span class="show-text-hover">
				<img src="./images/nonconforming.gif">
				<span class="hidden-text">
				<c:forEach items="${eOrder.warnings}" var="warning">
	       			<c:out value="${warning}"/><br>
				</c:forEach> 
				</span>
				</span>
			</c:if>
		</td>
	</tr>
	</c:forEach>
</tbody>
</table>
</c:if>

