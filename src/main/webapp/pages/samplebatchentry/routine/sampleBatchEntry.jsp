<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page import="org.openelisglobal.common.action.IActionConstants,
                 org.openelisglobal.common.util.ConfigurationProperties,
                 org.openelisglobal.common.util.ConfigurationProperties.Property,
                 org.openelisglobal.common.formfields.FormFields.Field,
                 org.openelisglobal.common.formfields.FormFields, 
                 org.openelisglobal.common.util.Versioning,
		 		 org.owasp.encoder.Encode" %>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<%
	boolean restrictNewReferringSiteEntries = ConfigurationProperties.getInstance().isPropertyValueEqual(Property.restrictFreeTextRefSiteEntry, "true");
    boolean useSiteDepartment = FormFields.getInstance().useField(Field.SITE_DEPARTMENT );
%>

<link rel="stylesheet" href="css/jquery_ui/jquery.ui.all.css?">
<link rel="stylesheet" href="css/customAutocomplete.css?">

<script type="text/javascript" src="scripts/utilities.js"></script>
<script type="text/javascript" src="scripts/ajaxCalls.js?"></script>
<script src="scripts/ui/jquery.ui.core.js?"></script>
<script src="scripts/ui/jquery.ui.widget.js?"></script>
<script src="scripts/ui/jquery.ui.button.js?"></script>
<script src="scripts/ui/jquery.ui.menu.js?"></script>
<script src="scripts/ui/jquery.ui.position.js?"></script>
<script src="scripts/ui/jquery.ui.autocomplete.js?"></script>
<script src="scripts/customAutocomplete.js?"></script>
<script type="text/javascript">
var inPrintState = true;	//is entryMethod in a print state 
var useSiteDepartment = <%= useSiteDepartment %>;

function finish() {
    window.location = "SampleBatchEntrySetup";
}

//check fields, entryMethod printState, and then call patientManagement setSave
function setSave() {
	var saveAllowed = checkOptionalFields() && inPrintState;
	jQuery("#saveButtonId").prop('disabled', !saveAllowed);
}

//if optional fields are being used, check them for validity
function checkOptionalFields() {
	var fieldsValid = true;
	<c:if test="${patientInfoCheck}">
		if (!patientFormValid()) {
			return false;
		}
	</c:if>
	<c:if test="${facilityIDCheck}">
		if (!jQuery("#requesterId").val() && !jQuery("#newRequesterName").val()) {
			return false;
		}
	</c:if>
	return fieldsValid;
}

//sets patient PK when identifying information is provided for patientProperties
//fixes bug where person is added to database again and again
function setPatient() {
	var splitName;
  var lastName = "";
  var firstName = "";
  var STNumber = "";
  var subjectNumber = "";
  var nationalID = "";
  var labNumber = "";
	labNumber = jQuery("#labNo").val();
  jQuery("#searchLabNumber").val(labNumber);
	if (window.hasIdentifyingInfo) {
		if (hasIdentifyingInfo() && !jQuery("#patientPK_ID").val()) {
		    patientSearch(lastName, firstName, STNumber, subjectNumber, nationalID, labNumber, "", false, processSearchSuccess);
		}
	}
}

function siteListChanged(textValue) {
    var siteList = $("requesterId");

    //if the index is 0 it is a new entry, if it is not then the textValue may include the index value
    if (siteList.selectedIndex == 0 || siteList.options[siteList.selectedIndex].label != textValue) {
        $("newRequesterName").value = textValue;
    }
	if ( useSiteDepartment ) {
		if(document.getElementById("requesterId").selectedIndex != 0){
			getDepartmentsForSiteClinic( document.getElementById("requesterId").value, "", siteDepartmentSuccess, null);
		} 
	}
}

function siteDepartmentSuccess (xhr) {
    console.log(xhr.responseText);
    var message = xhr.responseXML.getElementsByTagName("message").item(0).firstChild.nodeValue;
	var departments = xhr.responseXML.getElementsByTagName("formfield").item(0).childNodes[0].childNodes;
	var selected = xhr.responseXML.getElementsByTagName("formfield").item(0).childNodes[1];
	var isValid = message == "<%=IActionConstants.VALID%>";
	var requesterDepartment = jQuery("#requesterDepartmentId");
	var i = 0;

	requesterDepartment.disabled = "";
	if( isValid ){
		requesterDepartment.children('option').remove();
		requesterDepartment.append(new Option('', ''));
		for( ;i < departments.length; ++i){
// 						is this supposed to be value value or value id?
		requesterDepartment.append(
				new Option(departments[i].attributes.getNamedItem("value").value, 
					departments[i].attributes.getNamedItem("id").value));
		}
	}
	
	if( selected){
		requesterDepartment.selectedIndex = getSelectIndexFor( "requesterDepartmentId", selected.childNodes[0].nodeValue);
	}
}

jQuery(document).ready(function() {
	setSave();
});

jQuery(document).ready(function () {
    var dropdown = jQuery("select#requesterId");
    autoCompleteWidth = dropdown.width() + 66 + 'px';
    // Actually executes autocomplete
    dropdown.combobox();

    autocompleteResultCallBack = function (selectId, textValue) {
        siteListChanged(textValue);
    	setSave();
    };
});
</script>
<div class="hidden-fields">
	<input id="lastPatientId" type="hidden">
	<form:hidden path="sampleOrderItems.newRequesterName" name='${form.formName}' id="newRequesterName"/>
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
			<c:if test='${empty form.sampleOrderItems.newRequesterName}'>
			<tr>
				<td>
					<spring:message code="sample.batchentry.barcode.label.facilityid" />
					:  
					<c:if test="${not form.sampleOrderItems.readOnly}">
    		            <spring:message code="error.site.invalid" var="invalidSite"/>
    		            <spring:message code="sample.entry.project.siteMaxMsg" var="siteMaxMessage"/>
				        <form:select id="requesterId" 
				            path="sampleOrderItems.referringSiteId" 
				            onkeyup="capitalizeValue( this.value );" 
				            onchange="siteListChanged(this);setSave();"
		                    capitalize="true"
		                    invalidlabid='${invalidSite}'
		                    maxrepmsg='${siteMaxMessage}'
		       			    clearNonMatching="<%=restrictNewReferringSiteEntries%>"
				            >
				            <option value=""></option>
				            <form:options items="${form.sampleOrderItems.referringSiteList}" itemLabel="value" itemValue="id"/>
				        </form:select>
					</c:if>
					<c:if test="${form.sampleOrderItems.readOnly}">
				            <form:input path="form.sampleOrderItems.referringSiteName" cssStyle="width:300px" />
				    </c:if>
				</td>
			</tr>
		<% if( useSiteDepartment ){ %>
		 <tr>
		    <c:if test="${not form.sampleOrderItems.readOnly}">
				<td>  <spring:message code="sample.entry.project.siteDepartmentName"/>:  
						<form:select path="sampleOrderItems.referringSiteDepartmentId" 
							id="requesterDepartmentId"  >
						<option ></option>
						<form:options items="${form.sampleOrderItems.referringSiteDepartmentList}" itemValue="id" itemLabel="value"/>
					</form:select>
				</td>
			</c:if>
		</tr>
	<% } %>
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
	<c:if test="${not form.patientInfoCheck}">
	<tr>
			<td>
				<form:hidden path="patientProperties.patientUpdateStatus" id="processingStatus" value="ADD" />
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
					: <%=Encode.forHtml((String) request.getAttribute("facilityName")) %>
					<form:hidden path="sampleOrderItems.referringSiteId" id="requesterId"/>
						
				</td>
			</tr>
		</c:if>
		<c:if test='${not empty form.sampleOrderItems.referringSiteDepartmentId}'>
			<tr>
				<td>
					<spring:message code="sample.entry.project.siteDepartmentName" /> 
					: <%= Encode.forHtml((String) request.getAttribute("departmentName")) %> 
					<form:hidden path="sampleOrderItems.referringSiteDepartmentId" id="requesterDepartmentId"/>			
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
		<c:if test='${empty form.sampleOrderItems.referringSiteDepartmentId}'>
			<tr>
				<td>
					<spring:message code="sample.entry.project.siteDepartmentName" /> 
					: <%= Encode.forHtml((String) request.getAttribute("departmentName")) %> 
					<form:hidden path="sampleOrderItems.referringSiteDepartmentId" id="requesterDepartmentId"/>			
				</td>
			</tr>
		</c:if>
	</tr>
</table>
</td>
</tr>
</table>
