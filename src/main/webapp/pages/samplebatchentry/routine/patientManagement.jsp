<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page import="org.openelisglobal.common.action.IActionConstants,
                 org.openelisglobal.common.formfields.FormFields,
                 org.openelisglobal.common.formfields.FormFields.Field,
                 org.openelisglobal.patient.action.bean.PatientManagementInfo,
                 org.openelisglobal.common.util.*, org.openelisglobal.internationalization.MessageUtil" %>
				 <%@page import="org.openelisglobal.common.util.DateUtil"%>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<script type="text/javascript" src="scripts/ajaxCalls.js?"></script>
<script type="text/javascript" src="scripts/utilities.js?" ></script>

<%
	boolean supportSTNumber = FormFields.getInstance().useField(Field.StNumber);
	boolean supportAKA = FormFields.getInstance().useField(Field.AKA);
	boolean supportSubjectNumber = FormFields.getInstance().useField(Field.SubjectNumber);
	boolean subjectNumberRequired = ConfigurationProperties.getInstance().isPropertyValueEqual(ConfigurationProperties.Property.PATIENT_SUBJECT_NUMBER_REQUIRED, "true");
	boolean supportNationalID = FormFields.getInstance().useField(Field.NationalID);
		
	boolean patientIDRequired = ConfigurationProperties.getInstance().isPropertyValueEqual(ConfigurationProperties.Property.PATIENT_ID_REQUIRED, "true");
	boolean patientRequired = FormFields.getInstance().useField(Field.PatientRequired);
	boolean patientAgeRequired = false;
	boolean patientGenderRequired = false;
	boolean patientNamesRequired = FormFields.getInstance().useField(Field.PatientNameRequired);
	
	String ambiguousDateReplacement = ConfigurationProperties.getInstance().getPropertyValue(ConfigurationProperties.Property.AmbiguousDateHolder);
%>

<script type="text/javascript" >

var jQuery = jQuery.noConflict();

/*the prefix pt_ is being used for scoping.  Since this is being used as a tile there may be collisions with other
  tiles with simular names.  Only those elements that may cause confusion are being tagged, and we know which ones will collide
  because we can predicte the future */

var supportSTNumber = <%= supportSTNumber %>;
var supportAKA = <%= supportAKA %>
var supportSubjectNumber = <%= supportSubjectNumber %>;
var subjectNumberRequired = <%= subjectNumberRequired %>;
var supportNationalID = <%= supportNationalID %>;
var patientRequired = <%= patientRequired %>;
var patientIDRequired = <%= patientIDRequired %>;
var patientNamesRequired = <%= patientNamesRequired %>;
var patientAgeRequired = <%= patientAgeRequired %>;
var patientGenderRequired = <%= patientGenderRequired %>;

var pt_invalidElements = [];
var pt_requiredFields = [];
if (patientAgeRequired) {
	pt_requiredFields.push("dateOfBirthID");
}
if (patientGenderRequired) {
	pt_requiredFields.push("genderID");
}
if (patientNamesRequired) {
	pt_requiredFields.push("firstNameID"); 
	pt_requiredFields.push("lastNameID"); 
}

var pt_requiredOneOfFields = [];

if (patientIDRequired) {
	pt_requiredOneOfFields.push("nationalID") ;
	pt_requiredOneOfFields.push("patientGUID_ID") ;
	if (supportSTNumber) {
		pt_requiredOneOfFields.push("ST_ID");
	} else if (supportSubjectNumber && subjectNumberRequired) {
		pt_requiredOneOfFields = new Array("subjectNumberID");
	}
}

var updateStatus = "ADD";
var patientInfoChangeListeners = [];
var dirty = false;

function /*bool*/ pt_isFieldValid(fieldname) {
	return pt_invalidElements.indexOf(fieldname) == -1;
}


function /*void*/ pt_setFieldInvalid(field) {
	if (pt_invalidElements.indexOf(field) == -1) {
		pt_invalidElements.push(field);
	}
}

function /*void*/ pt_setFieldValid(field) {
	var removeIndex = pt_invalidElements.indexOf(field);
	if (removeIndex != -1) {
		for(var i = removeIndex + 1; i < pt_invalidElements.length; i++) {
			pt_invalidElements[i - 1] = pt_invalidElements[i];
		}
		pt_invalidElements.length--;
	}
}

function /*void*/ pt_setFieldValidity(valid, fieldName) {
	if (valid) {
		pt_setFieldValid(fieldName);
	} else {
		pt_setFieldInvalid(fieldName);
	}
}

function /*boolean*/ patientFormValid() {
	if (patientRequired || !pt_patientRequiredFieldsAllEmpty()) {
		return pt_invalidElements.length == 0 && pt_requiredFieldsValid();
	} else {
		return true;
	}
}

function pt_patientRequiredFieldsAllEmpty() {
	var i;

	for (i = 0; i < pt_requiredFields.length; ++i) {
		if (!$(pt_requiredFields[i]).value.blank()) {
			return false;
		}
	}
	
	for (i = 0; i < pt_requiredOneOfFields.length; ++i) {
		if (!($(pt_requiredOneOfFields[i]).value.blank())) {
			return false;
		}
	}
	return true;
}

function /*void*/ pt_setSave() {
	if (window.setSave) {
		setSave();
	} else {
		$("saveButtonId").disabled = !patientFormValid();
	}
}

function /*boolean*/ pt_isSaveEnabled() {
	return !$("saveButtonId").disabled;
}

function /*void*/ pt_requiredFieldsValid() {
    var i;
	for (i = 0; i < pt_requiredFields.length; ++i) {
		if ($(pt_requiredFields[i]).value.blank()) {
			return false;
		}
	}
	if (pt_requiredOneOfFields.length == 0) {
		return true;
	}
	for (i = 0; i < pt_requiredOneOfFields.length; ++i) {
		if (!($(pt_requiredOneOfFields[i]).value.blank())) {
			return true;
		}
	}
	return false;
}

function /*string*/ pt_requiredFieldsValidMessage() {
	var hasError = false;
	var returnMessage = "";
	var oneOfMembers = "";
	var requiredField = "";
    var i;

	for (i = 0; i < pt_requiredFields.length; ++i) {
		if ($(pt_requiredFields[i]).value.blank()) {
			hasError = true;
			requiredField += " : " + pt_requiredFields[i];
		}
	}

	for (i = 0; i < pt_requiredOneOfFields.length; ++i) {
		if (!pt_requiredOneOfFields[i].value.blank()) {
			oneOfFound = true;
			break;
		}
		oneOfMemebers += " : " + pt_requiredOneOfFields[i];
	}

	if (!oneOFound) {
		hasError = true;
	}

	if (hasError)	{
		if (!requiredField.blank()) {
			returnMessage = "Please enter the following patient values  " + requiredField;
		}
		if (!oneOfMembers.blank()) {
			returnMessage = "One of the following must have a value " + onOfMemebers;
		}
	} else {
		returnMessage = "valid";
	}

	return returnMessage;
}

function /*void*/ processValidateDateSuccess(xhr) {
    //alert(xhr.responseText);
	var message = xhr.responseXML.getElementsByTagName("message").item(0).firstChild.nodeValue;
	var formField = xhr.responseXML.getElementsByTagName("formfield").item(0).firstChild.nodeValue;
	var isValid = message == "<%=IActionConstants.VALID%>";

	setValidIndicaterOnField(isValid, formField);
	pt_setFieldValidity(isValid, formField);

	if (isValid) {
		updatePatientAge($("dateOfBirthID"));
	} else if (message == "<%=IActionConstants.INVALID_TO_LARGE%>") {
		alert('<spring:message code="error.date.birthInPast" />');
	}
	pt_setSave();
}

function normalizeDateFormat(element) {
	var caretPosition = doGetCaretPosition(element);
	var date = element.value;
	var dateParts = [3];
	//If there are not 10 characters then we give up
	if (date.length != 10) {
		return;
	}

	//replace all characters with x
	date = date.replace(/[^\d /]/g, "<%=ambiguousDateReplacement%>");
	dateParts[0] = date.substring(0,2);
	dateParts[1] = date.substring(3,5);
	dateParts[2] = date.substring(6);

	//make sure we don't mix meaning in date sections
	if (dateParts[0].indexOf("<%=ambiguousDateReplacement%>") != -1) {
		dateParts[0] = "<%=ambiguousDateReplacement + ambiguousDateReplacement%>"
	}
	if (dateParts[1].indexOf("<%=ambiguousDateReplacement%>") != -1) {
		dateParts[1] = "<%=ambiguousDateReplacement + ambiguousDateReplacement%>"
	}
	if (dateParts[2].indexOf("<%=ambiguousDateReplacement%>") != -1) {
		dateParts[2] = dateParts[2].replace(/<%=ambiguousDateReplacement%>/g, "0");
	}

	element.value = dateParts[0] + "/" + dateParts[1] + "/" + dateParts[2];
	setCaretPosition(element, caretPosition);
}

function doGetCaretPosition (ctrl) {
	var CaretPos = 0;	// IE Support
	if (document.selection) {
		ctrl.focus ();
		var Sel = document.selection.createRange ();
		Sel.moveStart ('character', -ctrl.value.length);
		CaretPos = Sel.text.length;
	}
	// Firefox support
	else if (ctrl.selectionStart || ctrl.selectionStart == '0')
		CaretPos = ctrl.selectionStart;
	return (CaretPos);
}
function setCaretPosition(ctrl, pos) {
	if (ctrl.setSelectionRange) {
		ctrl.focus();
		ctrl.setSelectionRange(pos,pos);
	} else if (ctrl.createTextRange) {
		var range = ctrl.createTextRange();
		range.collapse(true);
		range.moveEnd('character', pos);
		range.moveStart('character', pos);
		range.select();
	}
}

function /*void*/ checkValidAgeDate(dateElement) {
	if (dateElement && !dateElement.value.blank()) {
		isValidDate(dateElement.value, processValidateDateSuccess, dateElement.name, "past");
	} else {
		setValidIndicaterOnField(dateElement.value.blank(), dateElement.name);
	    pt_setFieldValidity(dateElement.value.blank(),  dateElement.name);
		pt_setSave();
		$("age").value = null;
	}
}


function /*void*/ updatePatientAge(DOB) {
	var date = String(DOB.value);

	var datePattern = '<%=DateUtil.getDateFormat()%>';
	var splitPattern = datePattern.split("/");
	var dayIndex = 0;
	var monthIndex = 1;
	var yearIndex = 2;

	for(var i = 0; i < 3; i++) {
		if (splitPattern[i] == "DD") {
			dayIndex = i;
		} else if (splitPattern[i] == "MM") {
			monthIndex = i;
		} else if (splitPattern[i] == "YYYY") {
			yearIndex = i;
		}
	}
	var splitDOB = date.split("/");
	var monthDOB = splitDOB[monthIndex];
	var dayDOB = splitDOB[dayIndex];
	var yearDOB = splitDOB[yearIndex];
	var today = new Date();
	var adjustment = 0;

	if (!monthDOB.match(/^\d+$/)) {
		monthDOB = "01";
	}
	if (!dayDOB.match(/^\d+$/)) {
		dayDOB = "01";
	}

	//months start at 0, January is month 0
	var monthToday = today.getMonth() + 1;

	if (monthToday < monthDOB || (monthToday == monthDOB && today.getDate() < dayDOB)) {
	    	adjustment = -1;
	}

	var calculatedAge = today.getFullYear() - yearDOB + adjustment;
	var age = document.getElementById("age");
	age.value = calculatedAge;
    setValidIndicaterOnField(true, $("age").name);
    pt_setFieldValid($("age").name);
}

function /*void*/ handleAgeChange(){
	var ageYears = jQuery("#ageYears").val();
	var ageMonths = jQuery("#ageMonths").val();
	var ageDays = jQuery("#ageDays").val();
	if( pt_checkValidAge() )
	{
		pt_updateDOB( ageYears, ageMonths, ageDays );
		if (ageYears >= 1 || ageMonths >= 1 || ageDays >= 1) {
			setValidIndicaterOnField( true, $("dateOfBirthID").name);
			pt_setFieldValid( $("dateOfBirthID").name );
		} else {
			setValidIndicaterOnField( false, $("dateOfBirthID").name);
			pt_setFieldInvalid( $("dateOfBirthID").name );
		}
	}

	pt_setSave();
}

function  /*bool*/ pt_checkValidAge()
{
	var valid = true;
	var ageYears = jQuery("#ageYears");
	var ageMonths = jQuery("#ageMonths");
	var ageDays = jQuery("#ageDays");
	if( !ageYears.val().blank() ){
		var regEx = new RegExp("^\\s*\\d{1,2}\\s*$");
		var yearValid = regEx.test(ageYears.val());
	 	valid = valid && yearValid;
		setValidIndicaterOnField(  yearValid , ageYears.attr('id') );
	} else {
		setValidIndicaterOnField(  true , ageYears.attr('id') );
	}

	if( !ageMonths.val().blank() ){
		var regEx = new RegExp("^\\s*\\d{1,2}\\s*$");
		var monthValid = regEx.test(ageMonths.val());
	 	valid = valid && monthValid;
		setValidIndicaterOnField(  monthValid , ageMonths.attr('id') );
	} else {
		setValidIndicaterOnField(  true , ageMonths.attr('id') );
	}

	if( !ageDays.val().blank() ){
		var regEx = new RegExp("^\\s*\\d{1,2}\\s*$");
		var dayValid = regEx.test(ageDays.val());
	 	valid = valid && dayValid;
		setValidIndicaterOnField(  dayValid , ageDays.attr('id') );
	} else {
		setValidIndicaterOnField(  true , ageDays.attr('id') );
	}

// 	pt_setFieldValidity( valid, age.name );

	return valid;
}

function  /*void*/ pt_updateDOB( ageYears, ageMonths, ageDays )
{
	if( ageYears.blank() && ageMonths.blank() && ageDays.blank() ){
		$("dateOfBirthID").value = null;
	} else {
		
		var date = new Date();
		if ( !ageDays.blank() ) {
			date.setDate( date.getDate() - parseInt(ageDays));
		}
		if ( !ageMonths.blank() ) {
			date.setMonth( date.getMonth() - parseInt(ageMonths));
		}
		if ( !ageYears.blank() ) {
			date.setFullYear( date.getFullYear() - parseInt(ageYears));
		}
		

		var day = "xx";
		var month = "xx";
		var year = "xxxx";
		if (!ageDays.blank() ) {
			day = date.getDate();
		}
		if (!ageMonths.blank() || !ageDays.blank() ) {
			//month is normally index based
			month = date.getMonth() + 1;
		}
		year = date.getFullYear();

		var datePattern = '<%=DateUtil.getDateFormat() %>';
		var splitPattern = datePattern.split("/");

		var DOB = "";

		for( var i = 0; i < 3; i++ ){
			if(splitPattern[i] == "DD"){
				DOB = DOB + day.toLocaleString('en', {minimumIntegerDigits:2}) + "/";
			}else if(splitPattern[i] == "MM" ){
				DOB = DOB + month.toLocaleString('en', {minimumIntegerDigits:2}) + "/";
			}else if(splitPattern[i] == "YYYY" ){
				DOB = DOB + year + "/";
			}
		}

		$("dateOfBirthID").value = DOB.substring(0, DOB.length - 1 );
	}
}

function /*void*/ setUpdateStatus(newStatus) {
	if (updateStatus != newStatus) {
		updateStatus = newStatus;
		document.getElementById("processingStatus").value = newStatus;
	}
}

function /*void*/ processSearchPopulateSuccess(xhr) {
	setUpdateStatus("NO_ACTION");
    //alert(xhr.responseText);
	var response = xhr.responseXML.getElementsByTagName("formfield").item(0);

	var nationalIDValue = getXMLValue(response, "nationalID");
	var STValue = getXMLValue(response, "ST_ID");
	var subjectNumberValue = getXMLValue(response, "subjectNumber");
	var lastNameValue = getXMLValue(response, "lastName");
	var firstNameValue = getXMLValue(response, "firstName");
	var akaValue = getXMLValue(response, "aka");
	var motherValue = getXMLValue(response, "mother");
	var motherInitialValue = getXMLValue(response, "motherInitial");
	var streetValue = getXMLValue(response, "street");
	var cityValue = getXMLValue(response, "city");
	var communeValue = getXMLValue(response, "commune");
	var dobValue = getXMLValue(response, "dob");
	var genderValue = getSelectIndexFor("genderID", getXMLValue(response, "gender"));
	var patientTypeValue = getSelectIndexFor("patientTypeID", getXMLValue(response, "patientType"));
	var insuranceValue = getXMLValue(response, "insurance");
	var occupationValue = getXMLValue(response, "occupation");
	var patientUpdatedValue = getXMLValue(response, "patientUpdated");
	var personUpdatedValue = getXMLValue(response, "personUpdated");
	var addressDepartment = getXMLValue(response, "addressDept");
	var education = getSelectIndexFor("educationID", getXMLValue(response, "education"));
	var nationality = getSelectIndexFor("nationalityID", getXMLValue(response, "nationality"));
	var otherNationality = getXMLValue(response, "otherNationality");
	var maritialStatus = getSelectIndexFor("maritialStatusID", getXMLValue(response, "maritialStatus"));
	var healthRegion = getSelectIndexByTextFor("healthRegionID", getXMLValue(response, "healthRegion"));
	var healthDistrict = getXMLValue(response, "healthDistrict");
	var guid = getXMLValue(response, "guid");

	setPatientInfo(nationalIDValue,
					STValue,
					subjectNumberValue,
					lastNameValue,
					firstNameValue,
					akaValue,
					motherValue,
					streetValue,
					cityValue,
					dobValue,
					genderValue,
					patientTypeValue,
					insuranceValue,
					occupationValue,
					patientUpdatedValue,
					personUpdatedValue,
					motherInitialValue,
					communeValue,
					addressDepartment,
					education,
					nationality,
					otherNationality,
					maritialStatus,
					healthRegion,
					healthDistrict,
					guid);

}

function /*string*/ getXMLValue(response, key) {
	var field = response.getElementsByTagName(key).item(0);

	if (field != null) {
		 return field.firstChild.nodeValue;
	}
	else {
		return undefined;
	}
}

function /*void*/ processSearchPopulateFailure(xhr) {
		//alert(xhr.responseText); // do something nice for the user
}

function /*void*/ clearPatientInfo() {
	setPatientInfo();
}

function /*void*/ clearErrors() {

	for (var i = 0; i < pt_invalidElements.length; ++i) {
		setValidIndicaterOnField(true, $(pt_invalidElements[i]).name);
	}

	pt_invalidElements = [];

}

function clearSelection() {
	jQuery("#searchResultsDiv").hide();
}

function /*void*/ setPatientInfo(nationalID, ST_ID, subjectNumber, lastName, firstName, aka, mother, street, city, dob, gender,
		patientType, insurance, occupation, patientUpdated, personUpdated, motherInitial, commune, addressDept, educationId, nationalId, nationalOther,
		maritialStatusId, healthRegionId, healthDistrictId, guid) {

	clearErrors();

	if (supportNationalID) { $("nationalID").value = nationalID == undefined ? "" : nationalID; }
	if (supportSTNumber) { $("ST_ID").value = ST_ID == undefined ? "" : ST_ID; }
	if (supportSubjectNumber) { $("subjectNumberID").value = subjectNumber == undefined ? "" : subjectNumber; }
	$("lastNameID").value = lastName == undefined ? "" : lastName;
	$("firstNameID").value = firstName == undefined ? "" : firstName;
	if (supportAKA) {$("akaID").value = aka == undefined ? "" : aka; }
	$("patientLastUpdated").value = patientUpdated == undefined ? "" : patientUpdated;
	$("personLastUpdated").value = personUpdated == undefined ? "" : personUpdated;
	$("patientGUID_ID").value = guid == undefined ? "" : guid;
	if (dob == undefined) {
		$("dateOfBirthID").value = "";
		$("age").value = "";
	} else {
		var dobElement = $("dateOfBirthID");
		dobElement.value = dob;
        checkValidAgeDate(dobElement);
	}
	document.getElementById("genderID").selectedIndex = gender == undefined ? 0 : gender;

	// run this b/c dynamically populating the fields does not constitute an onchange event to populate the patmgmt tile
	// this is the fx called by the onchange event if manually changing the fields
	updatePatientEditStatus();

}

function /*void*/ updatePatientEditStatus() {
	if (updateStatus == "NO_ACTION") {
		setUpdateStatus("UPDATE");
	}

	for (var i = 0; i < patientInfoChangeListeners.length; i++) {
			patientInfoChangeListeners[i]($("firstNameID").value,
										  $("lastNameID").value,
										  $("genderID").value,
										  $("dateOfBirthID").value,
										  supportSTNumber ? $("ST_ID").value : "",
										  supportSubjectNumber ? $("subjectNumberID").value : "",
										  supportNationalID ? $("nationalID").value : "",
										  supportMothersName ? $("motherID").value : null,
										  $("patientPK_ID").value);

		}

	makeDirty();

	pt_setSave();
}

function /*void*/ makeDirty() {
	dirty=true;
	if (typeof(showSuccessMessage) === 'function') {
		showSuccessMessage(false); //refers to last save
	}
	// Adds warning when leaving page if content has been entered into makeDirty form fields
	function formWarning() { 
    return "<spring:message code="banner.menu.dataLossWarning"/>";
	}
	window.onbeforeunload = formWarning;
}

function /*void*/  addPatient() {
	clearPatientInfo();
	clearErrors();
	clearSelection();
	if (supportSTNumber) {$("ST_ID").disabled = false;}
	if (supportSubjectNumber) {$("subjectNumberID").disabled = false;}
	if (supportNationalID) {$("nationalID").disabled = false;}
	setUpdateStatus("ADD");
	
	for(var i = 0; i < patientInfoChangeListeners.length; i++) {
			patientInfoChangeListeners[i]("", "", "", "", "", "", "", "", "");
		}
}
function /*void*/ addPatientInfoChangedListener(listener) {
	patientInfoChangeListeners.push(listener);
}

function clearDeptMessage() {
	$("deptMessage").innerText = deptMessage.textContent = "";
}

function validatePhoneNumber(phoneElement) {
    validatePhoneNumberOnServer(phoneElement, processPhoneSuccess);
}

function processPhoneSuccess(xhr) {
    //alert(xhr.responseText);

    var formField = xhr.responseXML.getElementsByTagName("formfield").item(0);
    var message = xhr.responseXML.getElementsByTagName("message").item(0);
    var success = false;

    if (message.firstChild.nodeValue == "valid") {
        success = true;
    }
    var labElement = formField.firstChild.nodeValue;

    setValidIndicaterOnField(success, labElement);
    pt_setFieldValidity(success, labElement);
    if (!success) {
        alert(message.firstChild.nodeValue);
    }

    pt_setSave();
}

function validateSubjectNumber(el, numberType) {
    validateSubjectNumberOnServer(el.value, numberType, el.id, processSubjectNumberSuccess);
}

function processSubjectNumberSuccess(xhr) {
    //alert(xhr.responseText);
    var formField = xhr.responseXML.getElementsByTagName("formfield").item(0);
    var message = xhr.responseXML.getElementsByTagName("message").item(0);
    var messageParts = message.firstChild.nodeValue.split("#");
    var valid = messageParts[0] == "valid";
    var warning = messageParts[0] == "warning";
    var fail = messageParts[0] == "fail";
    var success = valid || warning;
    var labElement = formField.firstChild.nodeValue;

    setValidIndicaterOnField(success, labElement);
    pt_setFieldValidity(success, labElement);

    if (warning || fail) {
        alert(messageParts[1]);
    }

    pt_setSave();
}

//check if and "identifying info" has been provided
//ie. STnumber, SubjectNumber, National ID, or name
function hasIdentifyingInfo() {
	<% if (supportSTNumber) { %>
	    if (jQuery("#ST_ID").val()) {
	    	return true
	    }            
    <%} %>
    <% if (supportSubjectNumber) { %>
    	if (jQuery("#subjectNumberID").val()) {
    		return true;
    	}
    <% } %>
    <% if (supportNationalID) { %>
    	if (jQuery("#nationalID").val()) {
    		return true;
    	}
	<%} %>
	if (jQuery("#lastNameID").val()) {
		return true;
	} else if (jQuery("#firstNameID").val()) {
		return true;
	} else {
		return false;
	}
}



function  /*void*/ getDetailedPatientInfo() {
	$("patientPK_ID").value = patientSelectID;

	new Ajax.Request (
                       'ajaxQueryXML',  //url
                        {//options
                          method: 'get', //http method
                          parameters: "provider=PatientSearchPopulateProvider&personKey=" + patientSelectID,
          				  requestHeaders : {
        					 "X-CSRF-Token" : getCsrfToken()
        				  },
                          onSuccess:  processSearchPopulateSuccess,
                          onFailure:  processSearchPopulateFailure
                         }
                          );
	}

</script>
<form:hidden path="patientProperties.currentDate" id="currentDate" />
<div id="PatientPage" style="width:90%;">
	<form:hidden path="patientProperties.patientLastUpdated" id="patientLastUpdated" />
	<form:hidden path="patientProperties.personLastUpdated" id="personLastUpdated"/>

<%-- 	<jsp:include page="${patientSearchFragment}"/> --%>
	<jsp:include page="${patientEnhancedSearchFragment}"/>

	<form:hidden path="patientProperties.patientUpdateStatus" id="processingStatus" value="ADD" />
	<form:hidden path="patientProperties.patientPK" id="patientPK_ID" />
	<form:hidden path="patientProperties.guid" id="patientGUID_ID" />
	<c:if test="${not form.patientProperties.readOnly}" >
	<br/>
	<div class="patientSearch">
		<hr style="width:100%" />
        <input type="button" value='<%= MessageUtil.getMessage("patient.new")%>' onclick="addPatient();">
	</div>
    </c:if>
	<div id="PatientDetail"   >
	<h3><spring:message code="patient.information"/></h3>
	<table style="width:80%" border="0">
    <tr>
        <% if (!supportSubjectNumber) { %>
        <td>
            <spring:message code="patient.externalId"/>
            <% if (patientIDRequired) { %>
            <span class="requiredlabel">*</span>
            <% } %>
        </td>
        <%} %>
        <% if (supportSTNumber) { %>
        <td style="text-align:right;">
            <spring:message code="patient.ST.number"/>:
        </td>
        <td>
            <form:input path="patientProperties.STnumber"
                         onchange="validateSubjectNumber(this, 'STnumber');updatePatientEditStatus();"
                         id="ST_ID"
                         cssClass="text"
                         size="60" />
        </td>
    </tr>
    <tr>
        <td >&nbsp;

        </td>
        <%} %>
        <% if (supportSubjectNumber) { %>
        <td>&nbsp;

        </td>
        <td style="text-align:right;">
            <spring:message code="patient.subject.number"/>:
            <% if (subjectNumberRequired) { %>
            <span class="requiredlabel">*</span>
            <% } %>
        </td>
        <td>
            <form:input path="patientProperties.subjectNumber"
                         onchange="validateSubjectNumber(this, 'subjectNumber');updatePatientEditStatus();"
                         id="subjectNumberID"
                         cssClass="text"
                         size="60" />
        </td>
    </tr>
    <tr>
        <td >&nbsp;

        </td>
        <% } %>
        <% if (supportNationalID) { %>
        <td style="text-align:right;">
            <%=MessageUtil.getContextualMessage("patient.NationalID") %>:

        </td>
        <td >
            <form:input path="patientProperties.nationalId"
                         onchange="validateSubjectNumber(this, 'nationalId');updatePatientEditStatus();"
                         id="nationalID"
                         cssClass="text"
                         size="60"/>
        </td>
        <td >&nbsp;

        </td>
        <td >&nbsp;

        </td>
    </tr>
    <%} %>
    <tr class="spacerRow" ><td colspan="2">&nbsp;</td></tr>
	<tr>
		<td style="width: 220px">
			<spring:message code="patient.name" />
		</td>
		<td style="text-align:right;">
			<spring:message code="patient.epiLastName" />
			:
			<% if (patientNamesRequired) { %>
				<span class="requiredlabel">*</span>
			<% } %>
		</td>
		<td >
			<form:input path="patientProperties.lastName"
					  cssClass="text"
				      size="60"
				      onchange="updatePatientEditStatus();"
				      id="lastNameID"/>
		</td>
		<td style="text-align:right;">
			<spring:message code="patient.epiFirstName" />
			:
			<% if (patientNamesRequired) { %>
				<span class="requiredlabel">*</span>
			<% } %>	
		</td>
		<td >
			<form:input path="patientProperties.firstName"
					  cssClass="text"
					  size="40"
					  onchange="updatePatientEditStatus();"
					  id="firstNameID"/>
		</td>
	</tr>
	<% if (supportAKA) { %>
	<tr>
	<td></td>
	<td style="text-align:right;">
		<spring:message code="patient.aka"/>
	</td>
	<td>
		<form:input path="patientProperties.aka"
				  onchange="updatePatientEditStatus();"
				  id="akaID"
				  cssClass="text"
				  size="60" />
	</td>
	</tr>
	<% } %>
	</table>

	<table>
	<tr>
		<td style="text-align:right;">
			<spring:message code="patient.birthDate" />&nbsp;<%=DateUtil.getDateUserPrompt()%>:
			<% if (patientAgeRequired) { %>
				<span class="requiredlabel">*</span>
			<% } %>
		</td>
		<td>
			<form:input path="patientProperties.birthDateForDisplay"
					  cssClass="text"
					  size="20"
                      maxlength="10"
                      onkeyup="addDateSlashes(this,event); normalizeDateFormat(this);"
                      onblur="checkValidAgeDate(this); updatePatientEditStatus();"
					  id="dateOfBirthID" />
			<div id="patientProperties.birthDateForDisplayMessage" class="blank" ></div>
		</td>
		<td style="text-align:right;">
			<spring:message code="patient.age" />:
		</td>
		<td >
           <form:input path="patientProperties.ageYears" 
           			  onchange="handleAgeChange(); updatePatientEditStatus();"
           			  id="ageYears"
                      cssClass="text"
                      size="3"
                      maxlength="3"
                      placeholder="years"
                        />
			<div  class="blank" ><spring:message code="years.label"/></div>
			<div id="ageYearsMessage" class="blank" ></div>
           <form:input path="patientProperties.ageMonths" 
           			  onchange="handleAgeChange(); updatePatientEditStatus();"
           			  id="ageMonths"
                      cssClass="text"
                      size="2"
                      maxlength="2"
                      placeholder="months"
                        />
			<div  class="blank" ><spring:message code="months.label"/></div>
			<div id="ageMonthsMessage" class="blank" ></div>
           <form:input path="patientProperties.ageDays" 
           			  onchange="handleAgeChange(); updatePatientEditStatus();"
           			  id="ageDays"
                      cssClass="text"
                      size="2"
                      maxlength="2"
                      placeholder="days"
                        />
			<div  class="blank" ><spring:message code="days.label"/></div>
			<div id="ageDaysMessage" class="blank" ></div>
		</td>
		<td style="text-align:right;">
			<spring:message code="patient.gender" />:
			<% if (patientGenderRequired) { %>
				<span class="requiredlabel">*</span>
			<% } %>
		</td>
		<td>
			<c:if test="${not form.patientProperties.readOnly}" >
			<form:select path="patientProperties.gender" onchange="updatePatientEditStatus();" id="genderID">
				<option value=" " ></option>
				<form:options items="${form.patientProperties.genders}" itemLabel="value" itemValue="id" />
			</form:select>
            </c:if>
            <c:if test="${form.patientProperties.readOnly}" >
                <form:input path="patientProperties.gender" />
            </c:if>
		</td>
	</tr>
	</table>
	</div>
</div>

<script type="text/javascript" >
//overrides method of same name in patientSearch
function selectedPatientChangedForManagement(firstName, lastName, gender, DOB, stNumber, subjectNumber, nationalID, mother, pk) {
	if (pk) {
		getDetailedPatientInfo();
		$("patientPK_ID").value = pk;
	} else {
		clearPatientInfo();
		setUpdateStatus("ADD");
	}
}

var registered = false;

function registerPatientChangedForManagement() {
	if (!registered) {
		addPatientChangedListener(selectedPatientChangedForManagement);
		registered = true;
	}
}

registerPatientChangedForManagement();
</script>