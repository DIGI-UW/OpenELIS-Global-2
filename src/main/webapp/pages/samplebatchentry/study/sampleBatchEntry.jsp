<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page import="org.openelisglobal.common.action.IActionConstants,
                 org.openelisglobal.common.util.ConfigurationProperties,
                 org.openelisglobal.common.util.ConfigurationProperties.Property,
                 org.openelisglobal.common.formfields.FormFields,
                 org.openelisglobal.common.formfields.FormFields.Field,
                 org.openelisglobal.common.util.Versioning,
                 org.owasp.encoder.Encode,
                 org.openelisglobal.internationalization.MessageUtil" %>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>


<%
	boolean restrictNewReferringSiteEntries = ConfigurationProperties.getInstance().isPropertyValueEqual(Property.restrictFreeTextRefSiteEntry, "true");
%>

<link rel="stylesheet" href="css/jquery_ui/jquery.ui.all.css?">
<link rel="stylesheet" href="css/customAutocomplete.css?">

<script type="text/javascript" src="scripts/utilities.js"></script>
<script type="text/javascript" src="scripts/ajaxCalls.js?"></script>
<script type="text/javascript" src="scripts/retroCIUtilities.js?" ></script>
<script type="text/javascript" src="scripts/entryByProjectUtils.js?"></script>
<script src="scripts/ui/jquery.ui.core.js?"></script>
<script src="scripts/ui/jquery.ui.widget.js?"></script>
<script src="scripts/ui/jquery.ui.button.js?"></script>
<script src="scripts/ui/jquery.ui.menu.js?"></script>
<script src="scripts/ui/jquery.ui.position.js?"></script>
<script src="scripts/ui/jquery.ui.autocomplete.js?"></script>
<script src="scripts/customAutocomplete.js?"></script>
<script type="text/javascript">
var inPrintState = true;	//is entryMethod in a print state 

function finish() {
	window.onbeforeunload = function(){};
    window.location = "SampleBatchEntrySetup";
}

//check fields, entryMethod printState, and then call patientManagement setSave
function setSave() {
	var saveAllowed = inPrintState;
	jQuery("#saveButtonId").prop('disabled', !saveAllowed);
}

function siteListChanged(textValue) {
    var siteList = $("requesterId");

    //if the index is 0 it is a new entry, if it is not then the textValue may include the index value
    if (siteList.selectedIndex == 0 || siteList.options[siteList.selectedIndex].label != textValue) {
        $("newRequesterName").value = textValue;
    }
}

function syncCenterInfo(centerInfo) {
	if (centerInfo == document.getElementById("vl.centerName")) {
		syncLists(centerInfo, document.getElementById("vl.centerCode"));
	} else if (centerInfo == document.getElementById("vl.centerCode")) {
		syncLists(centerInfo, document.getElementById("vl.centerName"));
	} else if (centerInfo == document.getElementById("eid.centerName")) {
		syncLists(centerInfo, document.getElementById("eid.centerCode"));
	} else if (centerInfo == document.getElementById("eid.centerCode")) {
		syncLists(centerInfo, document.getElementById("eid.centerName"));
	}
}

jQuery(document).ready(function() {
	setSave();
});

jQuery(document).ready(function () {
//     var dropdown = jQuery("select#requesterId");
//     autoCompleteWidth = dropdown.width() + 66 + 'px';
//     // Actually executes autocomplete
//     dropdown.combobox();

//     autocompleteResultCallBack = function (selectId, textValue) {
//         siteListChanged(textValue);
//     	setSave();
//     };
});
</script>
<div class="hidden-fields">
	<input id="lastPatientId" type="hidden">
	<form:hidden path="sampleOrderItems.newRequesterName" name='${form.formName}' id="newRequesterName"/>
	<form:hidden path="observations.projectFormName" name="${form.formName}" id="projectFormName"/>
	<form:hidden path="ProjectData.viralLoadTest" id="ProjectData.vlTest"/>
	<form:hidden path="ProjectData.edtaTubeTaken" id="ProjectData.edtaTube"/>
	<form:hidden path="ProjectData.dryTubeTaken" id="ProjectData.dryTube"/>
	<form:hidden path="ProjectData.dbsTaken" id="ProjectData.dbsTaken"/>
	<form:hidden path="ProjectData.dnaPCR" id="ProjectData.dnaPCR"/>
</div>
<table style="width:100%;">
	<tr>
	<td width="60%">
	<table style="width:100%;">
		<tr>
			<td>
				<h2><spring:message code="sample.batchentry.fields.specific"/></h2>
			</td>
		</tr>
<c:if test="${form.facilityIDCheck}">
		<c:if test='${empty form.facilityID}'>
			<c:if test='${study == "viralLoad" }'>
			<tr>
			<td>
				<spring:message code="sample.entry.project.ARV.centerName" />
				:  
				<form:select path="ProjectData.ARVcenterName" id="vl.centerName"  onchange="syncCenterInfo(this)">
					<form:options items="${form.organizationTypeLists.ARV_ORGS_BY_NAME.list}" itemLabel="organizationName" itemValue="id" />
				</form:select>
			</td>
		</tr>
		<tr>
			<td>
				<spring:message code="patient.project.centerCode" />
				:
				<form:select path="ProjectData.ARVcenterCode" id="vl.centerCode" onchange="syncCenterInfo(this)">
					<form:options items="${form.organizationTypeLists.ARV_ORGS.list}"	itemLabel="doubleName"  itemValue="id" />
				</form:select>
			</td>
		</tr>
			</c:if>
			<c:if test='${study == "EID" }'>
			<tr>
	        <td>
	            <spring:message code="sample.entry.project.siteName"/>
	            :
	            <form:select path="ProjectData.EIDSiteName" id="eid.centerName" onchange="syncCenterInfo(this)">
	                <form:options items="${form.organizationTypeLists.EID_ORGS_BY_NAME.list}" itemLabel="organizationName" itemValue="id" />
	            </form:select>
	        </td>
	    </tr>
	    <tr>
	        <td>
	        	<spring:message code="sample.entry.project.siteCode"/>
	        	:
	            <form:select path="ProjectData.EIDsiteCode" cssClass="text" id="eid.centerCode" onchange="syncCenterInfo(this)">
	                <form:options items="${form.organizationTypeLists.EID_ORGS.list}" itemLabel="doubleName" itemValue="id" />
	            </form:select>
	        </td>
	     </tr>
			</c:if>
		</c:if>
	</c:if>
	<c:if test="${form.patientInfoCheck}">
		<tr>
			<td>
				<jsp:include page="${patientInfoFragment}"/>
			</td>
		</tr>
	</c:if>
	<tr>
		<td>
			<jsp:include page="${entryMethodFragment}"/>
		</td>
	</tr>
</table>
	</td>
	<td width="40%" style="vertical-align:top">
	<table style="width:100%;" id="summary">
		<tr>
			<td colspan="2">
				<h2><spring:message code="sample.batchentry.fields.common"/></h2>
			</td>
		</tr>
		<tr>
			<td>
				<spring:message code="sample.batchentry.order.currentdate" />: 
				<form:input path="currentDate" readonly="true"/>
			</td>
			<td>
				<spring:message code="sample.batchentry.order.currenttime" />: 
				<form:input path="currentTime" readonly="true"/>
			</td>
		</tr>
		<tr>
			<td>
				<spring:message code="sample.batchentry.datereceived" />:
				<form:input path="sampleOrderItems.receivedDateForDisplay" readonly="true"/>
			</td>
			<td>
				<spring:message code="sample.batchentry.timereceived" />:
				<form:input path="sampleOrderItems.receivedTime" readonly="true"/>
			</td>
		</tr>
		<tr>
			<td colspan="2">
				<table style="width:100%">
					<tr>
						<th><spring:message code="sample.entry.sample.type"/></th>
						<th><spring:message code="test.testName"/></th>
					</tr>
					<tr>
						<td><%= Encode.forHtml((String) request.getAttribute("sampleType")) %></td>
						<td><%= Encode.forHtml((String) request.getAttribute("testNames")) %></td>
					</tr>
				</table>
				<form:hidden path="sampleXML"/>	
			</td>
		</tr>
		<tr>
		<c:if test='${not empty form.facilityID}'>
			<tr>
				<td>
					<spring:message code="sample.batchentry.barcode.label.facilityid" /> 
					: <%= Encode.forHtml((String) request.getAttribute("facilityName")) %>
					<form:hidden path="sampleOrderItems.referringSiteId" id="requesterId"/>
						
				</td>
			</tr>
		</c:if>
		<c:if test='${empty form.facilityID}'>
			<c:if test='${not empty form.sampleOrderItems.newRequesterName}'>
			<tr>
				<td>
					<spring:message code="sample.batchentry.barcode.label.facilityid" /> 
					: <%= Encode.forHtml((String) request.getAttribute("facilityName")) %>
					<form:hidden path="sampleOrderItems.referringSiteId" id="requesterId"/>
				</td>
			</tr>
			</c:if>
		</c:if>
		</tr>
	</table>
	</td>
	</tr>
</table>
