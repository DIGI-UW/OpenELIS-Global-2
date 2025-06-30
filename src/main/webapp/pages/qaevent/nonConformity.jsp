<%@ page language="java" contentType="text/html; charset=UTF-8"
		import="org.openelisglobal.common.action.IActionConstants,
				org.openelisglobal.common.formfields.FormFields,
                org.openelisglobal.common.formfields.FormFields.Field,
				org.openelisglobal.sample.util.AccessionNumberUtil,
                org.openelisglobal.common.provider.validation.NonConformityRecordNumberValidationProvider,
                org.openelisglobal.common.services.PhoneNumberService,
                org.openelisglobal.common.util.DateUtil,
                org.openelisglobal.internationalization.MessageUtil, 
                org.openelisglobal.common.util.Versioning,
                org.openelisglobal.qaevent.valueholder.retroCI.QaEventItem,
                org.openelisglobal.common.util.ConfigurationProperties" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<%
	boolean useProject = FormFields.getInstance().useField(Field.Project);
	boolean useSiteList = FormFields.getInstance().useField(Field.NON_CONFORMITY_SITE_LIST);
	boolean useSubjectNo = FormFields.getInstance().useField(Field.QASubjectNumber);
	boolean useNationalID = FormFields.getInstance().useField(Field.NationalID);
%>

<link rel="stylesheet" href="css/jquery_ui/jquery.ui.all.css?">
<link rel="stylesheet" href="css/customAutocomplete.css?">

<script src="scripts/ui/jquery.ui.core.js?"></script>
<script src="scripts/ui/jquery.ui.widget.js?"></script>
<script src="scripts/ui/jquery.ui.button.js?"></script>
<script src="scripts/ui/jquery.ui.menu.js?"></script>
<script src="scripts/ui/jquery.ui.position.js?"></script>
<script src="scripts/ui/jquery.ui.autocomplete.js?"></script>
<script src="scripts/customAutocomplete.js?"></script>
<script src="scripts/utilities.js?"></script>
<script src="scripts/ajaxCalls.js?"></script>

<script type="text/javascript" >

var dirty = false;

var confirmNewTypeMessage = "<spring:message code='nonConformant.confirm.newType.message'/>";

/* jQuery(function() {
	var eventsTable = $('qaEventsTable');
	if (eventsTable === null) {
		return;
	}
	var fields = eventsTable.getElementsBySelector(".requiredField");
	fields.each(function(field) {
				if( !field.disabled){
					fieldValidator.addRequiredField(field.id);
				}  
			});
}); */
function siteListChanged(textValue){
	var siteList = $("site");
	//if the index is 0 it is a new entry, if it is not then the textValue may include the index value
	if( siteList.selectedIndex == 0 || siteList.options[siteList.selectedIndex].label != textValue){
		  $("newServiceName").value = textValue;
		  siteList.selectedIndex = 0;
	}
	
	$("serviceNew").value = !textValue.blank();
}

function /*void*/loadForm() {
	if (!$("searchId").value.empty()) {
		var form = document.getElementById("mainForm");

		form.action = "NonConformity?labNo=" + $("searchId").value;
		form.method = "GET";

		form.submit();
	}
}

function setMyCancelAction() {
	setAction(document.getElementById("mainForm"), 'Cancel', 'no', '');
}
function doNothing(){
	
}

function onChangeSearchNumber(searchField) {
	var searchButton = $("searchButtonId");
	if (searchField.value === "") {
		searchButton.disable();
	} else {
	    validateAccessionNumberOnServer( true, true, searchField.id, searchField.value, processAccessionSuccess);
	}
}

function processAccessionSuccess(xhr)
{
    //alert(xhr.responseText);
	var message = xhr.responseXML.getElementsByTagName("message").item(0);
	var success = message.firstChild.nodeValue == "valid";

    var searchButton = $("searchButtonId");

	if( !success ){
		alert( message.firstChild.nodeValue );
        searchButton.disable();
	}else {
        searchButton.enable();
        searchButton.focus();
    }
}


/**
 * make the text of the blank option for sample type say "all types"
 */
function tweekSampleTypeOptions() {
	var eventsTable = $('qaEventsTable');
	if (eventsTable === null) {
		return;
	}
	var fields = eventsTable.getElementsBySelector(".typeOfSample");
	fields.each(function(field) {
				field.options[1].text = "<spring:message code='nonConformant.allSampleTypesText'/>";
			});
}

function addRow( ) {
	var eventsTable = $('qaEventsTable');
	if (eventsTable == null)
		return;
	var rows = eventsTable.getElementsBySelector("TR.qaEventRow");
	// display all rows up to the first unused one
	var row = rows.detect(function(row) {
		row.show(); // show each row as we go everyone
			var idFieldValue = row.getElementsBySelector(".id")[0].getValue();
			if (idFieldValue == "NEW") {
				enableRow(row, idFieldValue);
			} else {
				disableRow(row, true);
			}
			return idFieldValue == "";
		});
	if (row == null) {
		row = rows[rows.length - 1];
	}
	// we have an empty row
	row.show();
	var idFields = row.getElementsBySelector(".id");
	$(idFields[0]).value = "NEW";
	enableRow(row, "NEW");
	setSave();
}

function enableRow(row, newIndicator) {
	var isNew = newIndicator == "NEW";
	var elements = row.getElementsBySelector(".qaEventElement");

	elements.each(function(field) {
		field = $(field);
		if( isNew || field.id.startsWith("note")){
			field.enable();
			field.removeClassName("readOnly");
		}
	});
	
/* 	elements = row.getElementsBySelector(".requiredField");
	elements.each(function(field) {
		fieldValidator.addRequiredField(field.id);
	});
 */
}
function disableRow(row, userAdded) {
	var elements = row.getElementsBySelector(".qaEventElement");
	elements.each(function(field) {
		field = $(field);
		if( !(userAdded && field.id.startsWith("note"))){
			field.disable();
			field.addClassName("readOnly");
		}
	});
	
	/* elements = row.getElementsBySelector(".requiredField");
	elements.each(function(field) {
		fieldValidator.removeRequiredField(field.id);
	}); */
}

function areNewTypesOfSamples() {
	var eventsTable = $('qaEventsTable');
	var fields = eventsTable.getElementsBySelector(".typeOfSample");
	if ($("sampleItemsTypeOfSampleIds").value == "") {
		return false; // we don't worry about sample types when there aren't any at all
	}

    return fields.detect(function (field) {
        var ids = $("sampleItemsTypeOfSampleIds").value;
        var val = field.value;
        return (val !== null && val !== "0" && ids.indexOf("," + val + ",") == -1);
    }) != null;
}

function /*boolean*/ handleEnterEvent(){
	loadForm();
	return false;
}

function validateRecordNumber( recordElement){
	if( recordElement.value != ""){
		validateNonConformityRecordNumberOnServer( recordElement, recordNumberSuccess);
	}
	
	clearFieldErrorDisplay( recordElement);
}

function recordNumberSuccess( xhr){
    //alert(xhr.responseText);
	var message = xhr.responseXML.getElementsByTagName("message").item(0);
	var formField = xhr.responseXML.getElementsByTagName("formfield").item(0).firstChild.nodeValue;
	var success = message.firstChild.nodeValue == "Record not Found";
	var fieldElement = $(formField);

	selectFieldErrorDisplay( success, fieldElement);
    fieldValidator.setFieldValidity(success, fieldElement.id);

	if( !success ){
		alert( message.firstChild.nodeValue );
	}
	
	setSave();
}

function checkValidTime( timeElement ){
	var valid = !timeElement.value || checkTime(timeElement.value);
	selectFieldErrorDisplay( valid, timeElement);
	fieldValidator.setFieldValidity(valid, timeElement.id);
	setSave();
}

function enableDisableIfChecked(removeCheckBox){
	var row = removeCheckBox.parentNode.parentNode;
	var idFieldValue = row.getElementsBySelector(".id")[0].getValue();

	if( removeCheckBox.checked){
		disableRow(row, false );
	}else{
		enableRow(row, idFieldValue);
	}
	
	setSave();
}

function setSave(){
	var saveButton = $("saveButtonId"); 
	var validToSave = fieldValidator.isAllValid();

		//all this crap is to make sure there is not an enabled new row that has no value for the required field then none of the 
		//other fields have values		
	if( validToSave){
		jQuery("#qaEventsTable tbody tr").each( function(rowIndex, rowElement){
			var jqRow = jQuery(rowElement);
			if(validToSave && jqRow.is(":visible")){
				//if row is visible and the required field is blank make sure no other field has a value
				if( !(jqRow.find(".qaEventEnable").is(":checked") ) && requiredSelectionsNotDone( jqRow ) ){
					jqRow.find(".qaEventElement").each( function(index, element){
						var cellValue = jQuery(element).val(); 
						if( !(cellValue.length == 0 || cellValue == "0" )){
							validToSave = false;
							return;
						}
					});
				}
			}
		});
	}

	if( saveButton){
		saveButton.disabled = !validToSave;
	}	
}

function requiredSelectionsNotDone( jqRow ){
    var done = true;

    jqRow.find(".requiredField").each( function(index, element){
        if( element.value == 0 ){
            done = false;
            return;
        }
    });

    return !done;
}

function validatePhoneNumber( phoneElement){
    validatePhoneNumberOnServer( phoneElement, processPhoneSuccess);
}

function  processPhoneSuccess(xhr){
    //alert(xhr.responseText);

    var formField = xhr.responseXML.getElementsByTagName("formfield").item(0);
    var message = xhr.responseXML.getElementsByTagName("message").item(0);
    var success = false;

    if (message.firstChild.nodeValue == "valid"){
        success = true;
    }
    var labElement = formField.firstChild.nodeValue;
    selectFieldErrorDisplay( success, $(labElement));
    fieldValidator.setFieldValidity(success, labElement);

    if( !success ){
        alert( message.firstChild.nodeValue );
    }

    setSave();
}

</script>


<div align="center">
	<%= MessageUtil.getContextualMessage("quick.entry.accession.number") %>
	:
	<input type="text" name="labNo"
		maxlength='<%=Integer.toString(AccessionNumberUtil.getMaxAccessionLength())%>'
		value="" onchange="doNothing()" id="searchId">
	&nbsp;
	<input type="button" id="searchButtonId"
		value='<%=MessageUtil.getMessage("label.button.search")%>'
		onclick="loadForm();">
</div>
<hr />
<c:set var="readOnly" value="${form.readOnly}"/>
<c:if test="${not empty form.labNo}">
	<form:hidden path="sampleId" />
	<form:hidden path="patientId" />
	<form:hidden path="sampleItemsTypeOfSampleIds" />
	<table >
		<tr>
			<td >
				<%= MessageUtil.getContextualMessage("nonconformity.date") %>&nbsp;
                <span style="font-size: xx-small; "><%=DateUtil.getDateUserPrompt()%></span>
				:
			</td>
			<td>
				<form:input path="date"
				           id="date1"
					       maxlength="10" 
					       onchange="checkValidDate(this);" />
			<% if( FormFields.getInstance().useField(Field.QATimeWithDate)){ %>
				<%= MessageUtil.getContextualMessage("nonconformity.time") %>
				:
				<form:input path="time"
				           id="time"
					       maxlength="5" 
					       onchange="checkValidTime(this);" />
			<% } %>
			</td>
		</tr>
		<tr>
			<form:hidden path="project" />
			<% if ( useProject ) { %>
			<td >
				<spring:message code="label.study" />
				:
			</td>
			<c:if test="${empty form.project}">
				<td>
					<form:select path="projectId"
						onchange='makeDirty();'>
						<option value="0"></option>
						
						<form:options items="${form.projects}"
							itemLabel="localizedName" itemValue="id" />
					</form:select>
				</td>
			</c:if>
			<c:if test="${not empty project}">
				<td>
					<c:out value="${form.project}" />
				</td>
			</c:if>
			<% } %>
		</tr>
		<% if ( useSubjectNo ) { %>
		<tr>
			<td >
				<spring:message code="patient.subject.number" />
				:
			</td>
			<form:hidden path="subjectNew" id="subjectNew"/>
			<c:if test="${empty subjectNo}">
				<td>
					<form:input path="subjectNo"
						onchange="makeDirty();$('subjectNew').value = true;" />
				</td>
			</c:if>
			<c:if test="${not empty sujectNo}">
				<td>
					<c:out value="${form.subjectNo}" />
				</td>
			</c:if>
		</tr>
		<% } %>
		<% if ( FormFields.getInstance().useField(Field.StNumber) ) { %>
		<tr>
			<td >
				<spring:message code="patient.ST.number" />
				:
			</td>
			<form:hidden path="newSTNumber" id="newSTNumber"/>
			<c:if test="${empty STNumber}">
				<td>
					<form:input path="STNumber" onchange="makeDirty();$('newSTNumber').value = true;" />
				</td>
			</c:if>
			<c:if test="${not empty STNumber}">
				<td>
					<c:out value="${form.STNumber}" />
				</td>
			</c:if>
		</tr>
		<% } %>
		<% if ( useNationalID ) { %>
		<tr>
			<td >
			    <%=MessageUtil.getContextualMessage("patient.NationalID") %>
				:
			</td>
			<form:hidden path="nationalIdNew" id="nationalIdNew"/>
			<c:if test="${empty form.nationalId}">
				<td>
					<form:input path="nationalId"
						onchange="makeDirty();$('nationalIdNew').value = true;" />
				</td>
			</c:if>
			<c:if test="${not empty form.nationalId}">
				<td>
					<c:out value="${form.nationalId}" />
				</td>
			</c:if>
		</tr>
		<% } %>
		<tr>
			<td >
				<%=MessageUtil.getContextualMessage("sample.id") %>
				:
			</td>
			<td>
				<c:out value="${form.labNo}" />
			</td>
		</tr>
		<form:hidden path="serviceNew" />
			<form:hidden path="newServiceName" />
			<c:if test="${empty form.service}">
				<% if( useSiteList ){ %>
					<tr>
						<td><%= MessageUtil.getContextualMessage("sample.entry.project.siteName") %>
						<td>
    		            	<spring:message code="error.site.invalid" var="invalidSite"/>
    		           		<spring:message code="sample.entry.project.siteMaxMsg" var="siteMaxMessage"/>
							<form:select path="service" id="site"
									     onchange="makeDirty();$('serviceNew').value = true;"
		                    capitalize="true"
		                    invalidlabid='${invalidSite}'
		                    maxrepmsg='${siteMaxMessage}'
		       			    clearNonMatching="false"
		       			    >
								<option value=""></option>
								<form:options items="${form.siteList}" itemLabel="value" itemValue="id" />
						   	</form:select>
						</td>
					</tr>
				<% } else { %>
					<tr>
						<td ><spring:message code="patient.project.service" />:</td>
				  		<td><form:input path="service" onchange="makeDirty();$('serviceNew').value = true;" /></td>
					</tr>
				<% }%>
			</c:if>
			<c:if test="${not empty form.service}">
					<tr>
						<td ><%= MessageUtil.getContextualMessage("sample.entry.project.siteName") %>:</td>
						<td>
							<c:out value="${form.service}"/>
						</td>
					</tr>
			</c:if>
		<form:hidden path="doctorNew" />
		<%  if (FormFields.getInstance().useField(Field.QA_FULL_PROVIDER_INFO )) { %>
        <% if(ConfigurationProperties.getInstance().isPropertyValueEqual(ConfigurationProperties.Property.QA_SAMPLE_ID_REQUIRED, "true")) { %>
					<tr>
						<td><spring:message code="sample.clientReference" />:</td>
						<td >
						<c:if test="${empty requesterSampleID}">
							<form:input path="requesterSampleID" size="30" onchange="makeDirty();"/>
						</c:if>		  	
						<c:if test="${not empty requesterSampleID}">					
							<c:out value="${form.requesterSampleID}" />
						</c:if>
						</td>
					<td colspan="2">&nbsp;</td>
				</tr>
        <% } %>
				<tr>
					<td><%= MessageUtil.getContextualMessage("nonconformity.provider.label") %></td>
				</tr>
				<tr>
					<c:if test="${empty form.providerLastName}">
						<td align="right"><spring:message code="person.lastName" />:</td>
						<td >
						<form:input path="providerLastName"
							      id="providerLastNameID"
							      onchange="makeDirty();$('doctorNew').value = true;"
							      size="30" />
						<spring:message code="humansampleone.provider.firstName.short"/>:
						<form:input path="providerFirstName"
							      id="providerFirstNameID"
							      onchange="makeDirty();$('doctorNew').value = true;"
							      size="30" />
						</td>		      
					</c:if>
					<c:if test="${not empty form.providerLastName }">
						<form:hidden path="providerLastName"/>
						<form:hidden path="providerFirstName"/>
						<td align="right"><spring:message code="person.name" />:</td>
						<td><c:out value="${form.providerLastName}" />,&nbsp;<c:out value="${form.providerFirstName}" /></td>
					</c:if>		      
				</tr>
				<tr>
					<td align="right">
						<spring:message code="person.streetAddress.street" />:
					</td>
					<td>
					<c:if test="${empty providerStreetAddress}">
						<form:input path="providerStreetAddress"
					  			  cssClass="text"
					  			  size="70" />
					</c:if>
					<c:if test="${not empty providerStreetAddress}">
						<form:hidden path="providerStreetAddress" />
						<c:out value="${form.providerStreetAddress}" />
					</c:if>
					</td>
				</tr>
			<% if( FormFields.getInstance().useField(Field.ADDRESS_VILLAGE )) { %>
				<tr>
					<td align="right">
					    <%= MessageUtil.getContextualMessage("person.town") %>:
					</td>
					<td>
					<c:if test="${empty providerCity}">
						<form:input path="providerCity"
								  cssClass="text"
								  size="30" />
					</c:if>
					<c:if test="${not empty providerCity}">
						<form:hidden path="providerCity"/>
						<c:out value="${form.providerCity}" />
					</c:if>			  
					</td>
				</tr>
			<% } %>
			<% if( FormFields.getInstance().useField(Field.ADDRESS_COMMUNE)){ %>
			<tr>
				<td align="right">
					<spring:message code="person.commune" />:
				</td>
				<td>
				<c:if test="${empty providerCommune}">
					<form:input path="providerCommune"
							  cssClass="text"
							  size="30" />
				</c:if>
				<c:if test="${not empty providerCommune}">
					<form:hidden path="providerCommune" />
					<c:out value="${form.providerCommune}" />
				</c:if>
				</td>
			</tr>
			<% } %>
			<% if( FormFields.getInstance().useField(Field.ADDRESS_DEPARTMENT )){ %>
			<tr>
				<td align="right">
					<spring:message code="person.department" />:
				</td>
				<td>
				<c:if test="${empty providerDepartment}">
					<form:select path="providerDepartment"
							     id="departmentID" >
					<option value="0" ></option>
					<form:options items="${form.departments}" itemLabel="value" itemValue="id" />
					</form:select>
				</c:if>	
				<c:if test="${not empty providerDepartment}">
					<form:hidden path="providerDepartment"/>
					<c:out value="${form.providerDepartment}" />
				</c:if>
				</td>
			</tr>
			<% } %>

			<tr>
				<td align="right">
					<spring:message code="person.phone" />&nbsp;
					<%= PhoneNumberService.getPhoneFormat() %>
				</td>
				<td>
				<c:if test="${empty providerWorkPhone}">
					<form:input path="providerWorkPhone"
						      id="providerWorkPhoneID"
						      size="30"
						      maxlength="35"
						      cssClass="text"
						      onchange="validatePhoneNumber(this);makeDirty();$('doctorNew').value = true;" />
				    <div id="providerWorkPhoneMessage" class="blank" ></div>
				</c:if>    
				<c:if test="${not empty providerWorkPhone}">
					<form:hidden path="providerWorkPhone"/>
					<c:out value="${form.providerWorkPhone}" />
				</c:if>
				</td>
			</tr>
		<% } else { %>
			<tr>
				<td ><spring:message code="sample.entry.project.doctor" />:</td>
				<c:if test="${empty doctor}">
					<td>
						<form:input path="doctor"
							onchange="makeDirty();$('doctorNew').value = true;" />
					</td>
				</c:if>
				<c:if test="${not empty doctor}">
					<td >
						<form:hidden path="doctor"/>
						<c:out value="${form.doctor}" />
					</td>
				</c:if>
			</tr>
		<% } %>
	</table>
	
	<hr />
	<table style="width:95%" id="qaEventsTable">
		<thead>
		<tr>
			<th style="display: none"></th>
			<% if( FormFields.getInstance().useField(Field.QA_DOCUMENT_NUMBER)){ %>
				<th style="width:100px">
					<spring:message code="nonConformity.document.number" /><br> <%= NonConformityRecordNumberValidationProvider.getDocumentNumberFormat() %>  :
				</th>
			<% } %>
			<th style="width:22%">
				<spring:message code="label.refusal.reason" /><span class="requiredlabel">*</span>
			</th>
			<th style="width:16%">
				<spring:message code="label.sampleType" /><span class="requiredlabel">*</span>
			</th>
			<th style="width:11%">
				<%=MessageUtil.getContextualMessage("nonconformity.section") %>
			</th>
			<th style="width:13%">
				<%=MessageUtil.getContextualMessage("label.biologist") %>
			</th>
			<th >
				<%= MessageUtil.getContextualMessage("nonconformity.note") %>
			</th>
			<th style="width:5%">
				<spring:message code="label.remove" />
			</th>
		</tr>
		</thead>
		<tbody>
		<c:forEach items="${form.qaEvents}" var="qaEvent" varStatus="iter">
			<tr id="qaEvent_${iter.index}" style="display: none" class="qaEventRow">
			<% if( FormFields.getInstance().useField(Field.QA_DOCUMENT_NUMBER)){ %>
				<td>
					<form:input path="qaEvents[${iter.index}].recordNumber"
						id='record${iter.index }'
						cssClass="readOnly qaEventElement" 
						name='qaEvents'
						indexed = 'true'
						size = '12'
						onchange="setSave();makeDirty();validateRecordNumber(this)" />		 
				</td>
			<% } %>
				<td style="display: none">
					<form:hidden path="qaEvents[${iter.index}].id" id='id${iter.index}' cssClass="id" name="qaEvents"
						indexed="true" onchange='makeDirty();' />
				</td>
				<td>
					<form:select path="qaEvents[${iter.index}].qaEvent"
								 id='qaEvent${iter.index}' 
								 name="qaEvents"
						         cssClass="readOnly qaEventElement requiredField" 
						         indexed="true" 
						         disabled = "true"						    
						         style="width: 99%" 
						         onchange='setSave();makeDirty();'>
						<option value="0"></option>
						<form:options items="${form.qaEventTypes}" itemLabel="value" itemValue="id" />
					</form:select>
				</td>
				<td>
                    <form:select path="qaEvents[${iter.index}].sampleType" id='sampleType${iter.index}' name="qaEvents"
						cssClass="readOnly qaEventElement typeOfSample requiredField" disabled="false"
						onchange='setSave();makeDirty();'
						cssStyle="width: 99%">
                        <option value="0"></option>
                        <option value="-1" <c:if test="${qaEvents.sampleType != null and qaEvent.sampleType == '-1'}"> selected='selected' </c:if>></option>
						<form:options items="${form.typeOfSamples}" itemLabel="value" itemValue="id" />
					</form:select>
				</td>
				<td>
					<form:select path="qaEvents[${iter.index}].section" name="qaEvents" id='section${iter.index}'
						cssClass="readOnly qaEventElement" disabled="false"
						style="width: 99%"
						onchange='setSave();makeDirty();'>
						<option ></option>
						<form:options items="${form.sections}" itemLabel="localizedName" itemValue="nameKey" />
					</form:select>
				</td>
				<td>
					<form:input path="qaEvents[${iter.index}].authorizer" id='author${iter.index}' name="qaEvents"
						cssClass="readOnly qaEventElement" disabled="false"
						onchange='setSave();makeDirty();'
						cssStyle="width: 99%" />
				</td>
				<td>
					<form:input path="qaEvents[${iter.index}].note" id='note${iter.index}'
					           name="qaEvents"
							   cssClass="qaEventElement" 
							   disabled="false"
							   onchange='setSave();makeDirty();'
							   cssStyle="width: 99%" />
				</td>
				<td>
					<form:checkbox path="qaEvents[${iter.index}].remove" id='remove${iter.index}' name="qaEvents"
						cssClass="qaEventEnable"
						onclick='setSave();makeDirty(); enableDisableIfChecked(this)' />
				</td>
			</tr>
		</c:forEach>
		</tbody>
	</table>
	<input type="button" id="addButtonId"
		value='<%=MessageUtil.getMessage("label.button.add")%>'
		onclick="addRow()" />
	<hr />
	
	<form:hidden path="commentNew" id="commentNew" value="" />
	<spring:message code="label.comments" />:
	<div style="width: 50%">
		<form:textarea path="comment"
			onchange="makeDirty();$('commentNew').value = true;"
			disabled='${readOnly}' />
	</div>
</c:if>

<script type="text/javascript" >
//all methods here either overwrite methods in tiles or all called after they are loaded

function makeDirty() {
	dirty = true;

	if (typeof (showSuccessMessage) != 'undefined') {
		showSuccessMessage(false);
	}
	// Adds warning when leaving page if content has been entered into makeDirty form fields
	function formWarning(){ 
    return "<spring:message code="banner.menu.dataLossWarning"/>";
	}
	window.onbeforeunload = formWarning;
	
	setSave();
}

function savePage() {
	if (areNewTypesOfSamples() && !confirm(confirmNewTypeMessage)) {
		return false;
	}
	
	var form = document.getElementById("mainForm");
  
	window.onbeforeunload = null; // Added to flag that formWarning alert isn't needed.
	form.action = "NonConformity";
	form.submit();
}

addRow(); // call once at the beginning to make the table of Sample QA Events look right.
tweekSampleTypeOptions();

<% if( FormFields.getInstance().useField(Field.NON_CONFORMITY_SITE_LIST_USER_ADDABLE)){ %>
// Moving autocomplete to end - needs to be at bottom for IE to trigger properly
jQuery(document).ready( function() {
     	var dropdown = jQuery( "select#site" );
        autoCompleteWidth = dropdown.width() + 66 + 'px';
        dropdown.combobox();
        
        autocompleteResultCallBack = function( selectId, textValue) {
  				siteListChanged(textValue);
  				makeDirty();
  				setSave();
				};
});
<%  } %>

</script>
