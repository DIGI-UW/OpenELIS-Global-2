<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page import="org.openelisglobal.common.action.IActionConstants,
                 org.openelisglobal.common.util.DateUtil,
                 org.openelisglobal.internationalization.MessageUtil,
                 java.util.List,
                 org.openelisglobal.common.util.Versioning,
                 org.openelisglobal.referral.action.beanitems.ReferralItem,
                 org.openelisglobal.common.util.IdValuePair,
                 org.openelisglobal.referral.action.beanitems.ReferredTest" %>


<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>


<%@ taglib prefix="fn" uri="jakarta.tags.functions"%>

<script type="text/javascript" src="scripts/tableSort.js"></script>

<script type="text/javascript">

var colSort = [0,0,0,0,0,0,0,0,0,0];

jQuery(document).ready( function() {
});

function search(searchType) {
	jQuery('#searchType').val(searchType);
	
	if (searchType == 'TEST_AND_DATES' && !jQuery('#startDate').val() && !jQuery('#endDate').val()) {
			alert('a date range must be specified');
			return;
	}
	
	jQuery('#mainForm').attr('method', 'GET');
	jQuery('#mainForm').submit();
}

function sort(col) {
	sortTableJquery('mainTable', col, ((colSort[col - 1]++) % 2) != 0);
}

function selectAll() {
	jQuery('.analysis-checkbox').each(function() {
		jQuery(this).attr('checked', 'checked');
		jQuery(this).trigger("change");
	})
}

function deselectAll() {
	jQuery('.analysis-checkbox').each(function() {
		jQuery(this).removeAttr('checked');
		jQuery(this).trigger("change");
	})
}

function selectChange(index) {
	jQuery('#patientReportPrint').attr('disabled', 'disabled');
	jQuery('.analysis-checkbox').each(function() {
		if (jQuery(this).attr('checked') === 'checked') {
			jQuery('#patientReportPrint').removeAttr('disabled');
		}
	})
}

function printPatientReports() {
	var analysisIds = [];
	jQuery("#mainForm input[name=analysisIds]:checked").each(function () {
		analysisIds.push(this.value);
	});
	window.open('ReportPrint?report=patientCILNSP_vreduit&type=patient&analysisIds=' + analysisIds); 
}

</script>

<form:hidden id="searchType" path="searchType"/>

<jsp:include page="${referredTestSearchFragment}"/>

<jsp:include page="${patientEnhancedSearchFragment}"/>
<button class="patientFinishSearchShow" hidden="hidden" type="button" onclick="search('PATIENT')">Search referrals by patient</button>
<hr>

<c:if test="${empty form.referralDisplayItems && form.searchFinished}">
    <h2><spring:message code="referral.noreferralDisplayItem"/></h2>
</c:if>
<c:if test="${not empty form.referralDisplayItems}">
Referred Tests Matching Search
<button type="button" id="patientReportPrint" onclick="printPatientReports()" disabled="disabled">Print Selected Patient Reports</button>
<button type="button" onclick="selectAll()">Select All</button>
<button type="button" onclick="deselectAll()">Select None</button>
<table id="mainTable"  class='alt-color-table'>
<thead>
<tr>
	<th></th>
    <th class='split-content'>
    	Result Date
    	<span class="fa" onclick='sort(1)'><i class="fas fa-sort"></i></span>
    </th>
    <th class='split-content'>Lab Number
    	<span class="fa" onclick='sort(2)'><i class="fas fa-sort"></i></span>
    </th>
    <th class='split-content'>Sent Date
    	<span class="fa" onclick='sort(3)'><i class="fas fa-sort"></i></span>
    </th>
    <th class='split-content'>Status
    	<span class="fa" onclick='sort(4)'><i class="fas fa-sort"></i></span>
    </th>
    <th class='split-content'>Last Name
    	<span class="fa" onclick='sort(5)'><i class="fas fa-sort"></i></span>
    </th>
    <th class='split-content'>First Name
    	<span class="fa" onclick='sort(6)'><i class="fas fa-sort"></i></span>
    </th>
    <th class='split-content'>Test Name
    	<span class="fa" onclick='sort(7)'><i class="fas fa-sort"></i></span>
    </th>
    <th class='split-content'>Result
    	<span class="fa" onclick='sort(8)'><i class="fas fa-sort"></i></span>
    </th>
    <th class='split-content'>Reference Lab
    	<span class="fa" onclick='sort(9)'><i class="fas fa-sort"></i></span>
    </th>
    <th>Notes
    </th>
</tr>
</thead>
<tbody>
<c:forEach items="${form.referralDisplayItems}" var="referralDisplayItem" varStatus="iter">
<tr id='referralRow_${iter.index}'> 
    <td>
        <form:checkbox path="analysisIds" class="analysis-checkbox" onchange="selectChange(${iter.index})" value="${referralDisplayItem.analysisId}"/>
    </td>
    <td>
       <c:out value="${referralDisplayItem.resultDate}"/>
    </td>
    <td>
       <c:out value="${referralDisplayItem.accessionNumber}"/>
    </td>
    <td>
       <c:out value="${referralDisplayItem.referredSendDate}"/>
    </td>
    <td>
       <c:out value="${referralDisplayItem.referralStatusDisplay}"/>
    </td>
    <td>
       <c:out value="${referralDisplayItem.patientLastName}"/>
    </td>
    <td>
       <c:out value="${referralDisplayItem.patientFirstName}"/>
    </td>
    <td>
       <c:out value="${referralDisplayItem.referringTestName}"/>
    </td>
    <td>
       <c:out value="${referralDisplayItem.referralResultsDisplay}"/>
    </td>
    <td>
       <c:out value="${referralDisplayItem.referenceLabDisplay}"/>
    </td>
    <td>
       <c:out value="${referralDisplayItem.notes}"/>
    </td>
</tr>
</c:forEach>
</tbody>
</table>
</c:if>
