<%@ page language="java" contentType="text/html; charset=utf-8" %>
<%@ page import="org.openelisglobal.common.action.IActionConstants,
			     org.openelisglobal.common.formfields.FormFields,
			     org.openelisglobal.common.formfields.FormFields.Field,
			     org.openelisglobal.common.util.ConfigurationProperties.Property,
			     org.openelisglobal.common.util.StringUtil,
			     org.openelisglobal.common.util.*" %>


<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="fn" uri="jakarta.tags.functions"%>	
   
<c:set var="altAccessionLength" value="${fn:length(form.startingAtAccession)}"/>
<c:set var="altAccessionPrefixLength" value="6"/>
<c:set var="altAccessionValueLength" value="${altAccessionLength - altAccessionPrefixLength}"/>
<c:set var="altAccessionPrefix" value="${fn:substring(form.startingAtAccession, 0, altAccessionPrefixLength)}"/> 

<link rel="stylesheet" href="css/jquery_ui/jquery.ui.all.css">
<link rel="stylesheet" href="css/customAutocomplete.css">

<script src="scripts/ui/jquery.ui.core.js"></script>
<script src="scripts/ui/jquery.ui.widget.js"></script>
<script src="scripts/ui/jquery.ui.button.js"></script>
<script src="scripts/ui/jquery.ui.menu.js"></script>
<script src="scripts/ui/jquery.ui.position.js"></script>
<script src="scripts/ui/jquery.ui.autocomplete.js"></script>
<script src="scripts/customAutocomplete.js"></script>
<script type="text/javascript" src="scripts/ajaxCalls.js"></script>
<script>
function calculateTotal() {
	var numSetsOfLabels = document.getElementById('numSetsOfLabels').value;
	
	var numOrderLabelsPerSet = document.getElementById('numOrderLabelsPerSet').value;
	var numSpecimenLabelsPerSet = document.getElementById('numSpecimenLabelsPerSet').value;
	
	var numTotalOrderLabels = numSetsOfLabels * numOrderLabelsPerSet;
	var numTotalSpecimenLabels = numSetsOfLabels * numSpecimenLabelsPerSet;
	var numTotalLabels = numTotalOrderLabels + numTotalSpecimenLabels ;
// 	document.getElementById('numTotalOrderLabels').value = numTotalOrderLabels;
// 	document.getElementById('numTotalSpecimenLabels').value = numTotalSpecimenLabels;
	document.getElementById('numTotalLabels').value = numTotalLabels;
}
function preprintLabels() {
	var numSetsOfLabels = document.getElementById('numSetsOfLabels').value;
	
	var numOrderLabelsPerSet = document.getElementById('numOrderLabelsPerSet').value;
	var numSpecimenLabelsPerSet = document.getElementById('numSpecimenLabelsPerSet').value;
	
	var testIds = getTestIds();
	if (testIds.trim() == '') {
		alert("missing tests");
		return;
	}
	var startingAt;
	var startingAtValue ;
	if (jQuery("#useStartingAt").prop("checked")) {
		startingAtValue = String(jQuery("#startingAtValue").val()).padStart(${altAccessionValueLength}, "0");
		startingAt = '${altAccessionPrefix}' + startingAtValue;
	} else {
		startingAtValue = '';
		startingAt = ''; 
	}
	

	//label info
	const params = new URLSearchParams({
		"prePrinting": "true",
		"numSetsOfLabels": numSetsOfLabels,
		"numOrderLabelsPerSet": numOrderLabelsPerSet,
		"numSpecimenLabelsPerSet": numSpecimenLabelsPerSet,
		"facilityName": document.getElementById('requesterId').options[document.getElementById('requesterId').selectedIndex].text,
		"testIds": testIds,
		"startingAt": startingAt,
		});
	
    document.getElementById("ifpreprintbarcode").src = 'LabelMakerServlet?' + params.toString();
	document.getElementById("prePrintedBarcodeArea").show();
}
function getTestIds() {
	if (document.getElementById('testIds_1')) {
		return document.getElementById('testIds_1').value;
	} else {
		return "";
	}
}

function setStartingAt() {
	jQuery("#startingAtValue").prop('disabled', !jQuery("#useStartingAt").prop('checked'));
}

jQuery(document).ready(function () {
	calculateTotal()
	
    var dropdown = jQuery("select#requesterId");
    autoCompleteWidth = dropdown.width() + 66 + 'px';
    // Actually executes autocomplete
    dropdown.combobox();
    autocompleteResultCallBack = function (selectId, textValue) {
        siteListChanged(textValue);
    	processFacilityIDChange();
       // setOrderModified();
        //setCorrectSave();
    };
    setStartingAt();
});
</script>

<div>
<h2>Pre-Print Barcodes</h2>
<table>
	<tr>
		<td>Number of label sets:</td>
		<td><input id="numSetsOfLabels" name="numSetsOfLabels" type="number" value="1" size ="2" onchange="calculateTotal()"/></td>
	</tr>
	<tr>
		<td>Number of order labels per set:</td>
		<td><input id="numOrderLabelsPerSet" name="numOrderLabelsPerSet" type="number" value="${numDefaultOrderLabels}" size ="1" onchange="calculateTotal()"/></td>
		<td>Number of specimen labels per set:</td>
		<td><input id="numSpecimenLabelsPerSet" name="numSpecimenLabelsPerSet" type="number" value="${numDefaultSpecimenLabels}" size="1" onchange="calculateTotal()"/></td>
	</tr>
	<tr></tr>
<!-- 	<tr> -->
<!-- 		<td>Total Order Labels to Print:</td> -->
<!-- 		<td><input id="numTotalOrderLabels" type="number" value="1" size="2" readonly/></td> -->
<!-- 	</tr> -->
<!-- 	<tr> -->
<!-- 		<td>Total Specimen Labels to Print:</td> -->
<!-- 		<td><input id="numTotalSpecimenLabels" type="number" value="1" size="2" readonly/></td> -->
<!-- 	</tr> -->
	<tr>
		<td>Total Labels to Print:</td>
		<td><input id="numTotalLabels" type="number" value="2" size="2" readonly/></td>
	</tr>
</table>
<table>
	<tr>
 		<td>
			<spring:message code="sample.batchentry.barcode.label.options"/>
		</td>
		<td>
			<input type="checkbox"
			id="psuedoFacilityID"
			onchange="toggleFacilityID();">
			<form:hidden path="facilityIDCheck"
				disabled="true"
				value="true" />
			<spring:message code="sample.batchentry.barcode.label.facilityid"/>
		</td>
		<td><div id="facility-combobox">
			<c:if test="${not sampleOrderItems.readOnly}" >
    		    <spring:message code="error.site.invalid" var="invalidSite"/>
    		    <spring:message code="sample.entry.project.siteMaxMsg" var="siteMaxMessage"/>
		        <form:select id="requesterId"
		                     path="facilityID"
		                     onchange="siteListChanged(this);processFacilityIDChange();"
		                     onkeyup="capitalizeValue( this.value );"
		                     capitalize="true"
		                     invalidlabid='${invalidSite}'
		                     maxrepmsg='${siteMaxMessage}'
		       			     clearNonMatching="true"

		                >
		            <option value=""></option>
		            <form:options items="${form.sampleOrderItems.referringSiteList}" itemLabel="value"
		                                    itemValue="id"/>
		        </form:select>
			</c:if>
		    <c:if test="${sampleOrderItems.readOnly}" >
		            <form:input id="requesterId" path="facilityID"  style="width:300px" />
		    </c:if>
		</div></td>
	</tr>
	<tr <c:if test="${empty form.startingAtAccession}" >style="display:none;"</c:if>>
		<td>
			<spring:message code="labno.alt.startAt" />: <c:out value="${altAccessionPrefix}"/>
			<input id="startingAtValue" disabled="disabled" maxLength="${altAccessionValueLength}" value="${fn:substring(form.startingAtAccession, altAccessionPrefixLength, altAccessionLength)}"/>
			<input id="useStartingAt" type="checkbox" onChange="setStartingAt()"/><spring:message code="input.label.startat.toggle" />
<!-- 			<button type="button" onClick="enableStartingAt()"></button> -->
		</td>
		</tr>
</table>
</div>
<div>
<table width="75%">
	<tr>
		<td><h2>Sample</h2></td>
	</tr>
	<tr>
		<td><jsp:include page="${sampleAddFragment}"/></td>
	</tr>
	<tr>
		<td>NOTE: If a facility and/or sample and test are added, they will be printed on EVERY label</td>
	</tr>
</table>
</div>
<div>
<button id="prePrintButtonId" type="button" onclick="preprintLabels()">Pre-Print Labels</button>
</div>
<div style="display:none;" id="prePrintedBarcodeArea">
		<h2><spring:message code="barcode.common.section.barcode.header"/></h2>
		<iframe  src="about:blank" id="ifpreprintbarcode" width="75%" height="300px"></iframe>
</div>