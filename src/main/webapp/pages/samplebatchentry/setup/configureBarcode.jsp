<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page import="org.openelisglobal.common.action.IActionConstants,
                 org.openelisglobal.common.formfields.FormFields.Field,
                 org.openelisglobal.common.formfields.FormFields, 
                 org.openelisglobal.common.util.ConfigurationProperties,
                 org.openelisglobal.common.util.ConfigurationProperties.Property" %>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<%
	boolean restrictNewReferringSiteEntries = ConfigurationProperties.getInstance().isPropertyValueEqual(Property.restrictFreeTextRefSiteEntry, "true");
    boolean useSiteDepartment = FormFields.getInstance().useField(Field.SITE_DEPARTMENT );
%>

<script type="text/javascript" >
var useSiteDepartment = <%= useSiteDepartment %>;

function togglePatientInfo() {
	if (document.getElementsByName('patientInfoCheck')[0].disabled == false) {
		document.getElementsByName('patientInfoCheck')[0].disabled = true;
	} else {
		document.getElementsByName('patientInfoCheck')[0].disabled = false;
	}
}

function checkFacilityID() {
	document.getElementsByName('facilityIDCheck')[0].disabled = false;
}
function uncheckFacilityID() {
	document.getElementsByName('facilityIDCheck')[0].disabled = true;
}
function toggleFacilityID() {
	if (document.getElementsByName('facilityIDCheck')[0].disabled == false) {
		uncheckFacilityID();
	} else {
		checkFacilityID();
	}
}
function processFacilityIDChange() {
	if (document.getElementById('requesterId').value != '') {
		checkFacilityID();
		document.getElementById('psuedoFacilityID').checked = true;
		document.getElementById('psuedoFacilityID').disabled = true;
	} else if (document.getElementById('newRequesterName').value != '') {
		checkFacilityID();
		document.getElementById('psuedoFacilityID').checked = true;
		document.getElementById('psuedoFacilityID').disabled = true;
	} else {
		document.getElementById('psuedoFacilityID').disabled = false;
	}
}

//validation logic for this 'page'
function configBarcodeValid() {
	if (document.getElementsByName('method')[0].value == 'On Demand') {
		return true;
	} else if (document.getElementsByName('method')[0].value == 'Pre-Printed') {
		 return true;
	} else {
		return false;
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

jQuery(document).ready(function () {
    var dropdown = jQuery("select#requesterId");
    autoCompleteWidth = dropdown.width() + 66 + 'px';
    // Actually executes autocomplete
    dropdown.combobox();

	autocompleteResultCallBack = function (selectId, value) {
        siteListChanged(value);
    	processFacilityIDChange();
	}
});

</script>
<form:hidden path="sampleOrderItems.newRequesterName" id="newRequesterName"/>

Barcode Method : 
<form:select name="method" id="methodId" path="method">
	<form:option value="On Demand"><spring:message code="sample.batchentry.barcode.ondemand"/></form:option>
	<form:option value="Pre-Printed"><spring:message code="sample.batchentry.barcode.preprinted"/></form:option>
</form:select>

<table style="width:100%">
<tr>
<td>
<table>
	<tr>
 		<td>
			<spring:message code="sample.batchentry.barcode.label.options"/>
		</td>
		<td>
			<input type="checkbox"
			id="psuedoFacilityID"
			onchange="toggleFacilityID();">
			<form:hidden path="facilityIDCheck" name="facilityIDCheck" disabled="true" value="true"/>
			<spring:message code="sample.batchentry.barcode.label.facilityid"/>
		</td>
		<td>
			<spring:message code="sample.batchentry.barcode.label.facilityid"/>:
		</td>
		<td>
			<c:if test="${not form.sampleOrderItems.readOnly}">
			
    		<spring:message code="error.site.invalid" var="invalidSite"/>
    	    <spring:message code="sample.entry.project.siteMaxMsg" var="siteMaxMessage"/>
		    	<form:select path="facilityID" 
		    	     id="requesterId" 
		    	     onchange="siteListChanged(this);processFacilityIDChange();" 
		    	     onkeyup="capitalizeValue( this.value );"
                     capitalize="true"
                     invalidlabid='${invalidSite}'
                     maxrepmsg='${siteMaxMessage}'
       				 clearNonMatching="<%=restrictNewReferringSiteEntries%>"
       				 >
		            <option value=""></option>
		            <form:options items="${form.sampleOrderItems.referringSiteList}" itemValue="id" itemLabel="value"/>
		    	</form:select>
			</c:if>
		    <c:if test="${form.sampleOrderItems.readOnly}">
		    	<form:input path="facilityID" id="requesterId"/>
		    </c:if>
		</td>
	</tr>
	<% if( useSiteDepartment ){ %>
	 <tr>
	    <c:if test="${not form.sampleOrderItems.readOnly}">
			<td></td>
			<td></td>
			<td>
				<spring:message code="sample.entry.project.siteDepartmentName"/>
			</td>
			<td>    
					<form:select path="sampleOrderItems.referringSiteDepartmentId" 
						id="requesterDepartmentId"  >
					<option ></option>
					<form:options items="${form.sampleOrderItems.referringSiteDepartmentList}" itemValue="id" itemLabel="value"/>
				</form:select>
			</td>
		</c:if>
	</tr>
	<% } %>
	<tr>
		<td></td>
		<td>
			<input type="checkbox"
			id="psuedoPatientInfo"
			onchange="togglePatientInfo()"
			/>
			<form:hidden path="patientInfoCheck" name="patientInfoCheck" disabled="true" value="true"/>
			<spring:message code="sample.batchentry.barcode.label.patientinfo"/>
		</td>
	</tr>
</table>
</td>
</tr>
</table>
