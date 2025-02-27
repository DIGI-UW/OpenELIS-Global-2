<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page import="org.openelisglobal.common.action.IActionConstants,
                 org.openelisglobal.common.formfields.FormFields,
                 org.openelisglobal.common.formfields.FormFields.Field,
                 org.openelisglobal.patient.action.bean.PatientManagementInfo,
                 org.openelisglobal.common.services.PhoneNumberService,
				 org.openelisglobal.common.services.AddressService,
                 org.openelisglobal.common.util.*, org.openelisglobal.internationalization.MessageUtil" %>
				 <%@page import="org.openelisglobal.common.util.DateUtil"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<script type="text/javascript" src="scripts/ajaxCalls.js?"></script>
<script type="text/javascript" src="scripts/utilities.js?" ></script>

<c:set var="formName" value="${form.formName}" />
<c:set var="patientProperties" value="${form.patientProperties}" />

<%-- 		
<bean:define id="patientProperties" name='${form.formName}' property='patientProperties' type="PatientManagementInfo" /> --%>
<%
	String formName = (String) request.getAttribute("formName");
	PatientManagementInfo patientProperties = (PatientManagementInfo) request.getAttribute("patientProperties");

	boolean supportPatientPhone = FormFields.getInstance().useField(Field.PatientPhone);
	boolean supportPatientEmail = FormFields.getInstance().useField(Field.PatientEmail);
	boolean supportSTNumber = FormFields.getInstance().useField(Field.StNumber);
	boolean supportAKA = FormFields.getInstance().useField(Field.AKA);
	boolean supportMothersName = FormFields.getInstance().useField(Field.MothersName);
	boolean supportPatientType = FormFields.getInstance().useField(Field.PatientType);
	boolean supportInsurance = FormFields.getInstance().useField(Field.InsuranceNumber);
	boolean supportSubjectNumber = FormFields.getInstance().useField(Field.SubjectNumber);
	boolean subjectNumberRequired = ConfigurationProperties.getInstance().isPropertyValueEqual(ConfigurationProperties.Property.PATIENT_SUBJECT_NUMBER_REQUIRED, "true");
	boolean supportNationalID = FormFields.getInstance().useField(Field.NationalID);
	boolean nationalIDRequired = ConfigurationProperties.getInstance().isPropertyValueEqual(ConfigurationProperties.Property.PATIENT_NATIONAL_ID_REQUIRED, "true");
	boolean supportOccupation = FormFields.getInstance().useField(Field.Occupation);
	boolean supportCommune = FormFields.getInstance().useField(Field.ADDRESS_COMMUNE);
	boolean supportMothersInitial = FormFields.getInstance().useField(Field.MotherInitial);
	boolean supportAddressDepartment = FormFields.getInstance().useField(Field.ADDRESS_DEPARTMENT );
	String ambiguousDateReplacement = ConfigurationProperties.getInstance().getPropertyValue(ConfigurationProperties.Property.AmbiguousDateHolder);
	
	boolean patientNamesRequired = FormFields.getInstance().useField(Field.PatientNameRequired);
	
	boolean patientRequired = true;
	boolean patientIDRequired = true;
	boolean patientAgeRequired = true;
	boolean patientGenderRequired = true;
	
	if("SampleConfirmationEntryForm".equals( formName )){
		patientIDRequired = FormFields.getInstance().useField(Field.PatientIDRequired_SampleConfirmation);
		patientRequired = FormFields.getInstance().useField(Field.PatientRequired_SampleConfirmation );
		patientAgeRequired = false;
		patientGenderRequired = false;
	}else{
		patientIDRequired = ConfigurationProperties.getInstance().isPropertyValueEqual(ConfigurationProperties.Property.PATIENT_ID_REQUIRED, "true");
	    patientRequired = FormFields.getInstance().useField(Field.PatientRequired );
	    patientAgeRequired = true;
		patientGenderRequired = true;
	}
%>

<script type="text/javascript" >

var $jq = jQuery.noConflict();

/*the prefix pt_ is being used for scoping.  Since this is being used as a tile there may be collisions with other
  tiles with simular names.  Only those elements that may cause confusion are being tagged, and we know which ones will collide
  because we can predicte the future */

var supportPatientPhone = <%= supportPatientPhone %>;
var supportPatientEmail = <%= supportPatientEmail %>;
var supportSTNumber = <%= supportSTNumber %>;
var supportAKA = <%= supportAKA %>;
var supportMothersName = <%= supportMothersName %>;
var supportPatientType = <%= supportPatientType %>;
var supportInsurance = <%= supportInsurance %>;
var supportSubjectNumber = <%= supportSubjectNumber %>;
var subjectNumberRequired = <%= subjectNumberRequired %>;
var supportNationalID = <%= supportNationalID %>;
var nationalIDRequired = <%= nationalIDRequired %>;
var supportMothersInitial = <%= supportMothersInitial %>;
var supportCommune = <%= supportCommune %>;
var supportCity = <%= FormFields.getInstance().useField(Field.ADDRESS_VILLAGE) %>;
var supportOccupation = <%= supportOccupation %>;
var supportAddressDepartment = <%= supportAddressDepartment %>;
var patientRequired = <%= patientRequired %>;
var patientIDRequired = <%= patientIDRequired %>;
var patientNamesRequired = <%= patientNamesRequired %>;
var patientAgeRequired = <%= patientAgeRequired %>;
var patientGenderRequired = <%= patientGenderRequired %>;
var supportEducation = <%= FormFields.getInstance().useField(Field.PatientEducation) %>;
var supportPatientNationality = <%=  ConfigurationProperties.getInstance().isPropertyValueEqual(ConfigurationProperties.Property.PATIENT_NATIONALITY, "true") %>;
var supportMaritialStatus = <%= FormFields.getInstance().useField(Field.PatientMarriageStatus) %>;
var supportHealthRegion = <%= FormFields.getInstance().useField(Field.PatientHealthRegion) %>;
var supportHealthDistrict = <%= FormFields.getInstance().useField(Field.PatientHealthDistrict) %>;

var pt_invalidElements = [];
var pt_requiredFields = [];
if( patientAgeRequired){
	pt_requiredFields.push("dateOfBirthID");
}
if( patientGenderRequired){
	pt_requiredFields.push("genderID");
}
if( patientNamesRequired){
	pt_requiredFields.push("firstNameID"); 
	pt_requiredFields.push("lastNameID"); 
}

var pt_requiredOneOfFields = [];

if( patientIDRequired){
	pt_requiredOneOfFields.push("nationalID") ;
	pt_requiredOneOfFields.push("patientGUID_ID") ;
	if (supportSTNumber) {
		pt_requiredOneOfFields.push("ST_ID");
	} else if (supportSubjectNumber && subjectNumberRequired){
		pt_requiredOneOfFields.push("subjectNumberID");
	}
}

if (supportSubjectNumber && subjectNumberRequired){
	pt_requiredFields.push("subjectNumberID");
}

if (supportNationalID && nationalIDRequired) {
	pt_requiredFields.push("nationalID");
}

var updateStatus = "ADD";
var patientInfoChangeListeners = [];
var dirty = false;

function  /*bool*/ pt_isFieldValid(fieldname)
{
	return pt_invalidElements.indexOf(fieldname) == -1;
}


function  /*void*/ pt_setFieldInvalid(field)
{
	if( pt_invalidElements.indexOf(field) == -1 )
	{
		pt_invalidElements.push(field);
	}
}

function  /*void*/ pt_setFieldValid(field)
{
	var removeIndex = pt_invalidElements.indexOf( field );
	if( removeIndex != -1 )
	{
		for( var i = removeIndex + 1; i < pt_invalidElements.length; i++ )
		{
			pt_invalidElements[i - 1] = pt_invalidElements[i];
		}

		pt_invalidElements.length--;
	}
}

function  /*void*/ pt_setFieldValidity( valid, fieldName ){
	if( valid ){
		pt_setFieldValid( fieldName );
	}else{
		pt_setFieldInvalid( fieldName );
	}
}

function /*boolean*/ patientFormValid(){
	if ( patientRequired || !pt_patientRequiredFieldsAllEmpty()) {
		return pt_invalidElements.length == 0 && pt_requiredFieldsValid();
	} else {
		return true;
	}
}

function pt_patientRequiredFieldsAllEmpty() {
	var i;

	for(i = 0; i < pt_requiredFields.length; ++i ){
		if( !$(pt_requiredFields[i]).value.blank() ){
			return false;
		}
	}
	
	for(i = 0; i < pt_requiredOneOfFields.length; ++i ){
		if( !($(pt_requiredOneOfFields[i]).value.blank()) ){
			return false;
		}
	}
	return true;
}

function /*void*/ pt_setSave()
{
	if( window.setSave ){
		setSave();
	}else{
		$("saveButtonId").disabled = !patientFormValid();
	}
}

function /*boolean*/ pt_isSaveEnabled()
{
	return !$("saveButtonId").disabled;
}

function  /*void*/ setMyCancelAction(form, action, validate, parameters)
{

	//first turn off any further validation
	setAction(document.getElementById("mainForm"), 'Cancel', 'no', '');
}

function  /*void*/ pt_requiredFieldsValid(){
    var i;
	for( i = 0; i < pt_requiredFields.length; ++i ){
		if( $(pt_requiredFields[i]).value.blank() ){
			return false;
		}
	}

	if( pt_requiredOneOfFields.length == 0){
		return true;
	}

	for( i = 0; i < pt_requiredOneOfFields.length; ++i ){
		if( !($(pt_requiredOneOfFields[i]).value.blank()) ){
			return true;
		}
	}

	return false;
}

function  /*string*/ pt_requiredFieldsValidMessage()
{
	var hasError = false;
	var returnMessage = "";
	var oneOfMembers = "";
	var requiredField = "";
	var i;
	
	for( i = 0; i < pt_requiredFields.length; ++i ){
		if( $(pt_requiredFields[i]).value.blank() ){
			hasError = true;
			requiredField += " : " + pt_requiredFields[i];
		}
	}

	for( i = 0; i < pt_requiredOneOfFields.length; ++i ){
		if( !pt_requiredOneOfFields[i].value.blank() ){
			oneOfFound = true;
			break;
		}

		oneOfMemebers += " : " + pt_requiredOneOfFields[i];
	}

	if( !oneOFound ){
		hasError = true;
	}

	if( hasError )
	{
		if( !requiredField.blank() ){
			returnMessage = "Please enter the following patient values  " + requiredField;
		}
		if( !oneOfMembers.blank() ){
			returnMessage = "One of the following must have a value " + onOfMemebers;
		}
	}else{
		returnMessage = "valid";
	}

	return returnMessage;
}

function  /*void*/ processValidateDateSuccess(xhr){

    //alert(xhr.responseText);
	var message = xhr.responseXML.getElementsByTagName("message").item(0).firstChild.nodeValue;
	var formField = xhr.responseXML.getElementsByTagName("formfield").item(0).firstChild.nodeValue;

	var isValid = message == "<%=IActionConstants.VALID%>";

	setValidIndicaterOnField(isValid, formField);
	pt_setFieldValidity( isValid, formField );


	if( isValid ){
		updatePatientAge( $("dateOfBirthID") );
	}else if( message == "<%=IActionConstants.INVALID_TO_LARGE%>" ){
		alert( '<spring:message code="error.date.birthInPast" />' );
	}
	
	pt_setSave();
}

function normalizeDateFormat(element){
	var caretPosition = doGetCaretPosition(element);
	var date = element.value;
	var dateParts = [3];
	//If there are not 10 characters then we give up
	if( date.length != 10){
		return;
	}

	//replace all characters with x
	date = date.replace(/[^\d /]/g, "<%=ambiguousDateReplacement%>");

	dateParts[0] = date.substring(0,2);
	dateParts[1] = date.substring(3,5);
	dateParts[2] = date.substring(6);

	//make sure we don't mix meaning in date sections
	if( dateParts[0].indexOf("<%=ambiguousDateReplacement%>") != -1){
		dateParts[0] = "<%=ambiguousDateReplacement + ambiguousDateReplacement%>"
	}

	if( dateParts[1].indexOf("<%=ambiguousDateReplacement%>") != -1){
		dateParts[1] = "<%=ambiguousDateReplacement + ambiguousDateReplacement%>"
	}

	if( dateParts[2].indexOf("<%=ambiguousDateReplacement%>") != -1){
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
function setCaretPosition(ctrl, pos){
	if(ctrl.setSelectionRange)
	{
		ctrl.focus();
		ctrl.setSelectionRange(pos,pos);
	}
	else if (ctrl.createTextRange) {
		var range = ctrl.createTextRange();
		range.collapse(true);
		range.moveEnd('character', pos);
		range.moveStart('character', pos);
		range.select();
	}
}
function  /*void*/ checkValidAgeDate(dateElement)
{
	if( dateElement && !dateElement.value.blank() ){
		isValidDate( dateElement.value, processValidateDateSuccess, dateElement.name, "past" );
	}else{
		setValidIndicaterOnField(dateElement.value.blank(), dateElement.name);
	    pt_setFieldValidity( dateElement.value.blank(),  dateElement.name);
		pt_setSave();
		$("ageYears").value = null;
		$("ageMonths").value = null;
		$("ageDays").value = null;
	}
}


function  /*void*/ updatePatientAge( DOB )
{
	var date = String( DOB.value );

	var datePattern = '<%=DateUtil.getDateFormat() %>';
	var splitPattern = datePattern.split("/");
	var dayIndex = 0;
	var monthIndex = 1;
	var yearIndex = 2;

	for( var i = 0; i < 3; i++ ){
		if(splitPattern[i] == "DD"){
			dayIndex = i;
		}else if(splitPattern[i] == "MM" ){
			monthIndex = i;
		}else if(splitPattern[i] == "YYYY" ){
			yearIndex = i;
		}
	}

	var splitDOB = date.split("/");
	var monthDOB = splitDOB[monthIndex];
	var dayDOB = splitDOB[dayIndex];
	var yearDOB = splitDOB[yearIndex];

	if(monthDOB == "xx" || monthDOB == "XX" || dayDOB == "xx" || dayDOB == "XX" ){
		monthDOB = "01";
		dayDOB = "01";
	}

	var birthdate = new Date(yearDOB, monthDOB - 1, dayDOB);
	var today = new Date();
	var ageDate  = new Date(today - birthdate);
	
	var diffJSON = dateDiffToday(yearDOB + "-" + monthDOB + "-" + dayDOB);
	
	var ageYears = diffJSON['years'];
	var ageMonths = diffJSON['months'];
	var ageDays = diffJSON['days'];

	jQuery("#ageYears").val(ageYears);
	jQuery("#ageMonths").val(ageMonths);
	jQuery("#ageDays").val(ageDays);

    setValidIndicaterOnField( true, jQuery("#ageYears").attr('id'));
    setValidIndicaterOnField( true, jQuery("#ageMonths").attr('id'));
    setValidIndicaterOnField( true, jQuery("#ageDays").attr('id'));
    pt_setFieldValid( $("ageYears").name );
    pt_setFieldValid( $("ageMonths").name );
    pt_setFieldValid( $("ageDays").name );
}

function /*void*/ handleAgeChange()
{
	var ageYears = jQuery("#ageYears").val();
	var ageMonths = jQuery("#ageMonths").val();
	var ageDays = jQuery("#ageDays").val();
// 	if (ageMonths) {
// 		if (ageYears) {
// 			ageYears = ageYears + Math.floor(ageMonths / 12);
// 		} else {
// 			ageYears = Math.floor(ageMonths / 12);
// 		}
// 	}
// 	ageMonths = ageMonths % 12;
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

function  /*void*/ getDetailedPatientInfo()
{
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

function  /*void*/ setUpdateStatus( newStatus )
{
	if( updateStatus != newStatus )
	{
		updateStatus = newStatus;
		document.getElementById("processingStatus").value = newStatus;
	}
}

function  /*void*/ processSearchPopulateSuccess(xhr)
{

	setUpdateStatus("NO_ACTION");
    //alert(xhr.responseText);
	var response = xhr.responseXML.getElementsByTagName("formfield").item(0);
	
	var fhirUuidValue = getXMLValue(response, "fhirUuid");
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
	var genderValue = getSelectIndexFor( "genderID", getXMLValue(response, "gender"));
	var patientTypeValue = getSelectIndexFor( "patientTypeID", getXMLValue(response, "patientType"));
	var insuranceValue = getXMLValue(response, "insurance");
	var occupationValue = getXMLValue(response, "occupation");
	var patientUpdatedValue = getXMLValue(response, "patientUpdated");
	var personUpdatedValue = getXMLValue(response, "personUpdated");
	var addressDepartment = getXMLValue( response, "addressDept" );
	var education = getSelectIndexFor( "educationID", getXMLValue(response, "education"));
	var nationality = getSelectIndexFor( "nationalityID", getXMLValue(response, "nationality"));
	var otherNationality = getXMLValue( response, "otherNationality");
	var maritialStatus = getSelectIndexFor( "maritialStatusID", getXMLValue(response, "maritialStatus"));
	var healthRegion = getSelectIndexFor( "healthRegionID", getXMLValue(response, "healthRegion"));
	var healthDistrict = getXMLValue(response, "healthDistrict");
	var guid = getXMLValue( response, "guid");
	var phoneNumber = getXMLValue(response, "phoneNumber");	
	var email = getXMLValue(response, "email");	
	var contactLastName = getXMLValue( response, "contactLastName");
	var contactFirstName = getXMLValue( response, "contactFirstName");
	var contactPhone = getXMLValue( response, "contactPhone");
	var contactEmail = getXMLValue( response, "contactEmail");
	var contactPK = getXMLValue( response, "contactPK");

	setPatientInfo( nationalIDValue,
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
					guid,
					phoneNumber,
					email,
					contactLastName, 
					contactFirstName,
					contactPhone,
					contactEmail,
					contactPK,
					fhirUuidValue);

	<c:if test="${param.attemptAutoSave}">
		var validToSave =  patientFormValid() && sampleEntryTopValid();
		if (validToSave) {
			savePage();
		}
	</c:if>

}

function /*string*/ getXMLValue( response, key )
{
	var field = response.getElementsByTagName(key).item(0);

	if( field != null )
	{
		 return field.firstChild.nodeValue;
	}
	else
	{
		return undefined;
	}
}

function  /*void*/ processSearchPopulateFailure(xhr) {
		//alert(xhr.responseText); // do something nice for the user
}

function  /*void*/ clearPatientInfo(){
	setPatientInfo();
}

function /*void*/ clearErrors(){

	for( var i = 0; i < pt_invalidElements.length; ++i ){
		setValidIndicaterOnField( true, $(pt_invalidElements[i]).name );
	}

	pt_invalidElements = [];

}

function  /*void*/ setPatientInfo(nationalID, ST_ID, subjectNumber, lastName, firstName, aka, mother, street, city, dob, gender,
		patientType, insurance, occupation, patientUpdated, personUpdated, motherInitial, commune, addressDept, educationId, nationalId, nationalOther,
		maritialStatusId, healthRegionId, healthDistrictId, guid, phoneNumber, email, contactLastName, contactFirstName, contactPhone, contactEmail, contactPK,fhirUuidValue ) {

	clearErrors();

// 	jQuery("patientFhirUuid").val(fhirUuidValue == undefined ? "" : fhirUuidValue);
	if ( supportNationalID) { $("nationalID").value = nationalID == undefined ? "" : nationalID; }
	if(supportSTNumber){ $("ST_ID").value = ST_ID == undefined ? "" : ST_ID; }
	if(supportSubjectNumber){ $("subjectNumberID").value = subjectNumber == undefined ? "" : subjectNumber; }
	$("lastNameID").value = lastName == undefined ? "" : lastName;
	$("firstNameID").value = firstName == undefined ? "" : firstName;
	if(supportAKA){$("akaID").value = aka == undefined ? "" : aka; }
	if(supportMothersName){$("motherID").value = mother == undefined ? "" : mother; }
	if(supportMothersInitial){$("motherInitialID").value = (motherInitial == undefined ? "" : motherInitial); }
	$("streetID").value = street == undefined ? "" : street;
	if(supportCity){$("cityID").value = city == undefined ? "" : city; }
	if(supportCommune){$("communeID").value = commune == undefined ? "" : commune; }
	if(supportInsurance){$("insuranceID").value = insurance == undefined ? "" : insurance; }
	if(supportOccupation){$("occupationID").value = occupation == undefined ? "" : occupation; }
	$("patientLastUpdated").value = patientUpdated == undefined ? "" : patientUpdated;
	$("personLastUpdated").value = personUpdated == undefined ? "" : personUpdated;
	$("patientGUID_ID").value = guid == undefined ? "" : guid;
	if(supportPatientPhone) {$("patientPhone").value = phoneNumber == undefined ? "" : phoneNumber;}
	if(supportPatientEmail) {$("patientEmail").value = email == undefined ? "" : email;}
	$("genderID").selectedIndex = gender == undefined ? 0 : gender;
	if(supportPatientNationality){
		$("nationalityID").selectedIndex = nationalId == undefined ? 0 : nationalId; 
		$("nationalityOtherId").value = nationalOther == undefined ? "" : nationalOther;}
	if( supportEducation){ $("educationID").selectedIndex =  educationId == undefined ? 0 : educationId;}
	if( supportMaritialStatus){ $("maritialStatusID").selectedIndex = maritialStatusId == undefined ? 0 : maritialStatusId;}
	if( supportHealthRegion){ 
		$("healthRegionID").selectedIndex = healthRegionId == undefined ? 0 : healthRegionId;
	}
	if( supportHealthDistrict){
		if(document.getElementById("healthRegionID").selectedIndex != 0){
			getDistrictsForRegion( document.getElementById("healthRegionID").value, healthDistrictId, healthDistrictSuccess, null);
		} 
	}

	if(supportAddressDepartment){
		var deptMessage = $("deptMessage");
		deptMessage.innerText = deptMessage.textContent = "";
		//for historic reasons, we switched from text to dropdown
		if( addressDept == undefined ){
			$("departmentID").value = 0;
		}else if( isNaN( addressDept) ){
			$("departmentID").value = 0;
			deptMessage.textContent = "<%= MessageUtil.getMessage("patient.address.dept.entry.msg") %>" + " " + addressDept;
		}else{
			$("departmentID").value = addressDept;
		}

	}
	if (dob == undefined) {
		document.getElementById("dateOfBirthID").value = "";
		document.getElementById("ageYears").value = "";
		document.getElementById("ageMonths").value = "";
		document.getElementById("ageDays").value = "";
	} else {
		var dobElement = document.getElementById("dateOfBirthID").value = dob;
		updatePatientAge( $("dateOfBirthID") );
	}

	if(supportPatientType){$("patientTypeID").selectedIndex = patientType == undefined ? 0 : patientType; }

	$("contactLastNameID").value = contactLastName == undefined ? "" : contactLastName;
	$("contactFirstNameID").value = contactFirstName == undefined ? "" : contactFirstName;
	$("contactPhoneID").value = contactPhone == undefined ? "" : contactPhone;
	$("contactEmailID").value = contactEmail == undefined ? "" : contactEmail;
	$("contactPK_ID").value = contactPK == undefined ? "" : contactPK;

	// run this b/c dynamically populating the fields does not constitute an onchange event to populate the patmgmt tile
	// this is the fx called by the onchange event if manually changing the fields
	updatePatientEditStatus();

}

function  /*void*/ updatePatientEditStatus() {
	if (updateStatus == "NO_ACTION") {
		setUpdateStatus("UPDATE");
	}
	
	for(var i = 0; i < patientInfoChangeListeners.length; i++){
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

function /*void*/ makeDirty(){
	dirty=true;
	if( typeof(showSuccessMessage) === 'function' ){
		showSuccessMessage(false); //refers to last save
	}
	// Adds warning when leaving page if content has been entered into makeDirty form fields
	function formWarning(){ 
    return "<spring:message code="banner.menu.dataLossWarning"/>";
	}
	window.onbeforeunload = formWarning;
}

function  /*void*/  addPatient(){
	clearPatientInfo();
	clearErrors();
	if(supportSTNumber){$("ST_ID").disabled = false;}
	if(supportSubjectNumber){$("subjectNumberID").disabled = false;}
	if(supportNationalID){$("nationalID").disabled = false;}
	setUpdateStatus( "ADD" );
	jQuery("#PatientDetail").show();
	
	for(var i = 0; i < patientInfoChangeListeners.length; i++){
			patientInfoChangeListeners[i]("", "", "", "", "", "", "", "", "");
		}
}

function  /*void*/ savePage()
{
	window.onbeforeunload = null; // Added to flag that formWarning alert isn't needed.
	var form = document.getElementById("mainForm");
	form.action = "PatientManagement";
	form.submit();
}

function /*void*/ addPatientInfoChangedListener( listener ){
	patientInfoChangeListeners.push( listener );
}

function clearDeptMessage(){
	$("deptMessage").innerText = deptMessage.textContent = "";
}

function updateHealthDistrict( regionElement){
	getDistrictsForRegion( regionElement.value, "", healthDistrictSuccess, null);
}

function healthDistrictSuccess( xhr ){
	  //alert(xhr.responseText);
	  
	var message = xhr.responseXML.getElementsByTagName("message").item(0).firstChild.nodeValue;
	var districts = xhr.responseXML.getElementsByTagName("formfield").item(0).childNodes[0].childNodes;
	var selected = xhr.responseXML.getElementsByTagName("formfield").item(0).childNodes[1];
	var isValid = message == "<%=IActionConstants.VALID%>";
	var healthDistrict = $("healthDistrictID");
	var i = 0;

	healthDistrict.disabled = "";
	if( isValid ){
		healthDistrict.options.length = 0;
		healthDistrict.options[0] = new Option('', '');
		for( ;i < districts.length; ++i){
			<%-- 			is this supposed to be value value or value id? --%>
			healthDistrict.options[i + 1] = new Option(districts[i].attributes.getNamedItem("value").value, districts[i].attributes.getNamedItem("value").value);
		}
	}
	
	if( selected){
		healthDistrict.selectedIndex = getSelectIndexFor( "healthDistrictID", selected.childNodes[0].nodeValue);
	}
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
	
    setValidIndicaterOnField(success, labElement);
	pt_setFieldValidity( success, labElement );
	
    if( !success ){
        alert( message.firstChild.nodeValue );
	}
	
    pt_setSave();
}

function validateEmail( emailElement) {
	var valid = emailElement.value === "" || validEmail(emailElement.value);
	
	setValidIndicaterOnField(valid, emailElement.id);
    pt_setFieldValidity( valid, emailElement.id );
    pt_setSave();
}
function validEmail(email) {
	var re = /\S+@\S+\.\S+/;
    return re.test(email);
}

function validateSubjectNumber( el, numberType ){

    validateSubjectNumberOnServer( el.value, numberType, el.id, processSubjectNumberSuccess );
}

function  processSubjectNumberSuccess(xhr){
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
	pt_setFieldValidity( success, labElement );
	
    if( warning || fail ){
        alert( messageParts[1] );
	}
	
    pt_setSave();
}
</script>


<%-- <nested:hidden name='${form.formName}' property="patientProperties.currentDate" id="currentDate"/> --%>
<form:hidden path="patientProperties.currentDate" id="currentDate"/>

<div id="PatientPage" style="display:inline" >
<%-- 	<nested:hidden property="patientProperties.patientLastUpdated" name='${form.formName}' id="patientLastUpdated" />
	<nested:hidden property="patientProperties.personLastUpdated" name='${form.formName}'  id="personLastUpdated"/> --%>
<form:hidden path="patientProperties.patientLastUpdated" id="patientLastUpdated"/>
<form:hidden path="patientProperties.personLastUpdated" id="personLastUpdated"/>

<%-- 	<jsp:include page="${patientSearchFragment}"/> --%>
	<jsp:include page="${patientEnhancedSearchFragment}"/>

<%-- 	<nested:hidden name='${form.formName}' property="patientProperties.patientProcessingStatus" id="processingStatus" value="add" />
	<nested:hidden name='${form.formName}' property="patientProperties.patientPK" id="patientPK_ID" />
	<nested:hidden name='${form.formName}' property="patientProperties.guid" id="patientGUID_ID" /> --%>
<form:hidden path="patientProperties.patientUpdateStatus" id="processingStatus" value="ADD"/>
<form:hidden path="patientProperties.patientPK" id="patientPK_ID"/>
<form:hidden path="patientProperties.guid" id="patientGUID_ID"/>
<form:hidden path="patientProperties.patientContact.id" id="contactPK_ID"/>
<%-- <form:hidden path="patientProperties.fhirUuid" id="patientFhirUuid"/> --%>
	
   <%--  <logic:equal value="false" name="${form.formName}" property="patientProperties.readOnly" > --%>
   <c:if test="${form.patientProperties.readOnly == false }" >
	<br/>
	<div class="patientSearch">
		<hr style="width:100%" />
        <input type="button" value='<%= MessageUtil.getMessage("patient.new")%>' onclick="addPatient()">
	</div>
    <%-- </logic:equal> --%>
    </c:if>
	<div id="PatientDetail" class="patientFinishSearchShow" style="display:none;"  >
	<h2><spring:message code="patient.information"/></h2>
	<table style="width:80%" border="0">
    <tr>
        <% if( !supportSubjectNumber){ %>
        <td>
            <spring:message code="patient.externalId"/>
            <% if( patientIDRequired){ %>
            <span class="requiredlabel">*</span>
            <% } %>
        </td>
        <%} %>
        <% if( supportSTNumber){ %>
        <td style="text-align:right;">
            <spring:message code="patient.ST.number"/>:
        </td>
        <td>
            <%-- <nested:text name='${form.formName}'
                         property="patientProperties.STnumber"
                         onchange="validateSubjectNumber(this, 'STnumber');updatePatientEditStatus();"
                         id="ST_ID"
                         styleClass="text"
                         size="60" /> --%>
            <form:input path="patientProperties.STnumber" id="ST_ID" onchange="validateSubjectNumber(this, 'STnumber');updatePatientEditStatus();" size="60"/>
        </td>
    </tr>
    <tr>
		<td >&nbsp;
			
        </td>
        <%} %>
        <% if( supportSubjectNumber){ %>
		<td>&nbsp;
			
        </td>
        <td style="text-align:right;">
            <spring:message code="patient.subject.number"/>:
            <% if(subjectNumberRequired){ %>
            <span class="requiredlabel">*</span>
            <% } %>
        </td>
        <td>
            <%-- <nested:text name='${form.formName}'
                         property="patientProperties.subjectNumber"
                         onchange="validateSubjectNumber(this, 'subjectNumber');updatePatientEditStatus();"
                         id="subjectNumberID"
                         styleClass="text"
                         size="60" /> --%>
            <form:input path="patientProperties.subjectNumber" id="subjectNumberID" onchange="validateSubjectNumber(this, 'subjectNumber');updatePatientEditStatus();" cssClass="text" size="60"/>
        </td>
    </tr>
    <tr>
		<td >&nbsp;
			
        </td>
        <% } %>
        <% if( supportNationalID ){ %>
        <td style="text-align:right;">
            <%=MessageUtil.getContextualMessage("patient.NationalID") %>:
            <% if(nationalIDRequired){ %>
            <span class="requiredlabel">*</span>
			<% } %>
			
        </td>
        <td >
           <%--  <nested:text name='${form.formName}'
                         property="patientProperties.nationalId"
                         onchange="validateSubjectNumber(this, 'nationalId');updatePatientEditStatus();"
                         id="nationalID"
                         styleClass="text"
                         size="60"/> --%>
            <form:input path="patientProperties.nationalId" id="nationalID" onchange="validateSubjectNumber(this, 'nationalId');updatePatientEditStatus();" size="60"/>
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
			<% if( patientNamesRequired){ %>
				<span class="requiredlabel">*</span>
			<% } %>
		</td>
		<td >
			<%-- <nested:text name='${form.formName}'
					  property="patientProperties.lastName"
					  styleClass="text"
				      size="60"
				      onchange="updatePatientEditStatus();"
				      id="lastNameID"/> --%>
            <form:input path="patientProperties.lastName" id="lastNameID" onchange="updatePatientEditStatus();"  size="60"/>
		</td>
		<td style="text-align:right;">
			<spring:message code="patient.epiFirstName" />
			:
			<% if( patientNamesRequired){ %>
				<span class="requiredlabel">*</span>
			<% } %>	
		</td>
		<td >
			<%-- <nested:text name='${form.formName}'
					  property="patientProperties.firstName"
					  styleClass="text"
					  size="40"
					  onchange="updatePatientEditStatus();"
					  id="firstNameID"/> --%>
            <form:input path="patientProperties.firstName" id="firstNameID" onchange="updatePatientEditStatus();" size="40"/>
		</td>
	</tr>
	<% if(supportAKA){ %>
	<tr>
	<td></td>
	<td style="text-align:right;">
		<spring:message code="patient.aka"/>
	</td>
	<td>
		<%-- <nested:text name='${form.formName}'
				  property="patientProperties.aka"
				  onchange="updatePatientEditStatus();"
				  id="akaID"
				  styleClass="text"
				  size="60" /> --%>
            <form:input path="patientProperties.aka" id="akaID" onchange="updatePatientEditStatus();" size="60" />
	</td>
	</tr>
	<% } %>
	<% if( supportMothersName ){ %>
	<tr>
		<td></td>
		<td style="text-align:right;">
			<spring:message code="patient.mother.name"/>
		</td>
		<td>
			<%-- <nested:text name='${form.formName}'
					  property="patientProperties.mothersName"
					  onchange="updatePatientEditStatus();"
				  	  id="motherID"
				  	  styleClass="text"
				      size="60" /> --%>
            <form:input path="patientProperties.mothersName" id="motherID" onchange="updatePatientEditStatus();" size="60"/>
		</td>
	</tr>
	<% } if(supportMothersInitial){ %>
	<tr>
		<td></td>
		<td style="text-align:right;">
			<spring:message code="patient.mother.initial"/>
		</td>
		<td>
			<%-- <nested:text name='${form.formName}'
					  property="patientProperties.mothersInitial"
					  onchange="updatePatientEditStatus();"
				  	  id="motherInitialID"
				  	  styleClass="text"
				      size="1"
				      maxlength="1" /> --%>
            <form:input path="patientProperties.mothersInitial" id="motherInitialID" cssClass="text"  onchange="updatePatientEditStatus();" size="1" maxlength="1"/>
		</td>
	</tr>
	<%} %>
	<tr class="spacerRow" ><td colspan="2">&nbsp;</td></tr>
	<tr>
		<td style="width: 220px">
			<spring:message code="emergency.contact" />
		</td>
		<td style="text-align:right;">
			<spring:message code="patient.contactLastName" />:
		</td>
		<td >
			<form:input path="patientProperties.patientContact.person.lastName"
					  cssClass="text"
				      size="60"
				      onchange="updatePatientEditStatus();"
				      id="contactLastNameID"/>
		</td>
		<td style="text-align:right;">
			<spring:message code="patient.contactFirstName" />:
		</td>
		<td >
			<form:input path="patientProperties.patientContact.person.firstName"
					  cssClass="text"
					  size="25"
					  onchange="updatePatientEditStatus();"
					  id="contactFirstNameID"/>
		</td>
	</tr>
	<tr>
		<td style="width: 220px">
		</td>
		<td style="text-align:right;">
			<spring:message code="patient.contactEmail" />:
		</td>
		<td >
			<form:input path="patientProperties.patientContact.person.email"
					  cssClass="text"
				      size="60"
				      onchange="updatePatientEditStatus();validateEmail( this );"
				      id="contactEmailID"/>
		</td>
		<td style="text-align:right;">
			<spring:message code="patient.contactPhone" />:
		</td>
		<td >
			<form:input path="patientProperties.patientContact.person.primaryPhone"
					  cssClass="text"
					  size="25"
					  onchange="updatePatientEditStatus();"
					  id="contactPhoneID"/>
		</td>
	</tr>
	<tr class="spacerRow" ><td colspan="2">&nbsp;</td></tr>
	<tr>
		<td >
			<spring:message code="person.streetAddress" />
		</td>
		<td style="text-align:right;">
			<%=" " + AddressService.getAddresslineLabel1() %>:
		</td>
		<td>
			<%-- <nested:text name='${form.formName}'
					  property="patientProperties.streetAddress"
					  onchange="updatePatientEditStatus();"
					  id="streetID"
					  styleClass="text"
					  size="70" /> --%>
            <form:input path="patientProperties.streetAddress" id="streetID" cssClass="text" onchange="updatePatientEditStatus();" size="70"/>
		</td>
	</tr>
    <% if( supportCommune){ %>
    <tr>
        <td></td>
        <td style="text-align:right;">
			<%=" " + AddressService.getAddresslineLabel2() %>:
        </td>
        <td>
            <%-- <nested:text name='${form.formName}'
                         property="patientProperties.commune"
                         onchange="updatePatientEditStatus();"
                         id="communeID"
                         styleClass="text"
                         size="30" /> --%>
            <form:input path="patientProperties.commune" id="communeID" cssClass="text"  onchange="updatePatientEditStatus();" size="30"/>
        </td>
    </tr>
    <% } %>
	<% if( FormFields.getInstance().useField(Field.ADDRESS_VILLAGE )) { %>
	<tr>
		<td></td>
		<td style="text-align:right;">
			<%=" " + AddressService.getAddresslineLabel3() %>:
		</td>
		<td>
			<%-- <nested:text name='${form.formName}'
					  property="patientProperties.city"
					  onchange="updatePatientEditStatus();"
					  id="cityID"
					  styleClass="text"
					  size="30" /> --%>
            <form:input path="patientProperties.city" id="cityID" cssClass="text" onchange="updatePatientEditStatus();" size="30"/>
		</td>
	</tr>
	<% } %>
	<% if( supportAddressDepartment){ %>
	<tr>
		<td></td>
		<td style="text-align:right;">
			<spring:message code="person.department" />:
		</td>
		<td>
            <%-- <logic:equal value="false" name="${form.formName}" property="patientProperties.readOnly" > --%>
            <c:if test="${ patientProperties.readOnly == false}" >
            <form:select path="patientProperties.addressDepartment" id="departmentID" onchange="updatePatientEditStatus();clearDeptMessage();">
			<%-- <html:select name='${form.formName}'
						 property="patientProperties.addressDepartment"
						 onchange="updatePatientEditStatus();clearDeptMessage();"
					     id="departmentID" > --%>
			<option value="0" ></option>
			<form:options items="${patientProperties.addressDepartments}" itemLabel="dictEntry" itemValue="id"/>
			<%-- <html:optionsCollection name="${form.formName}" property="patientProperties.addressDepartments" label="dictEntry" value="id" /> --%>
			<%-- </html:select> --%><br>
			</form:select>
			<span id="deptMessage"></span>
            <%-- </logic:equal> --%>
            </c:if>
            <c:if test="${form.patientProperties.readOnly}" >
            <%-- <logic:equal value="true" name="${form.formName}" property="patientProperties.readOnly" > --%>
            <form:input path="patientProperties.addressDepartment" />
                <%-- <html:text property="patientProperties.addressDepartment" name="${form.formName}" /> --%>
            <%-- </logic:equal> --%>
            </c:if>
		</td>
	</tr>
	<% } %>
	<% if( supportPatientPhone){ %>
		<tr>
			<td>&nbsp;</td>
			<td style="text-align:right;"><%= MessageUtil.getContextualMessage("person.phone") %>: <%=" " + PhoneNumberService.getPhoneFormat() %></td>
			<td>
				<form:input id="patientPhone" path="patientProperties.primaryPhone" onchange="updatePatientEditStatus();validatePhoneNumber( this );" maxLength="35"/>
<%-- 				<html:text id="patientPhone" name='${form.formName}' property="patientProperties.phone" maxlength="35" onchange="validatePhoneNumber( this );" />
 --%>			</td>
		</tr>
	<% } %>
	<% if( supportPatientEmail){ %> 
		<tr>
			<td>&nbsp;</td>
			<td style="text-align:right;"><%= MessageUtil.getContextualMessage("person.email") %>:</td>
			<td>
				<form:input id="patientEmail" path="patientProperties.email" onchange="updatePatientEditStatus();validateEmail( this );" maxLength="35"/>			</td>
		</tr>
	<% } %>
	<tr class="spacerRow"><td >&nbsp;</td></tr>
	<% if( FormFields.getInstance().useField(Field.PatientHealthRegion)){ %>
	<tr>
	<td>&nbsp;</td>
	<td style="text-align:right;">
		<%=" " + AddressService.getGeographicUnitLabel1() %>:
	</td>
		<td>
			<%-- <nested:hidden name='${form.formName}' property="patientProperties.healthRegion" id="shadowHealthRegion" />
			<html:select name='${form.formName}'
						 property="patientProperties.healthRegion"
						 onchange="updateHealthDistrict( this );"
					     id="healthRegionID" >
			<option value="0" ></option>
			<html:optionsCollection name="${form.formName}" property="patientProperties.healthRegions" label="value" value="id" />
			</html:select> --%>
			
			<form:select path="patientProperties.healthRegion" onchange="updateHealthDistrict( this );" id="healthRegionID">
			<option value="0" ></option>
			<form:options items="${patientProperties.healthRegions}" itemLabel="value" itemValue="id"/>
			</form:select>
		</td>	
	</tr>		
	<% } %>
	<% if( FormFields.getInstance().useField(Field.PatientHealthDistrict)){ %>
	<tr>
	<td>&nbsp;</td>
	<td style="text-align:right;"><%=" " + AddressService.getGeographicUnitLabel2() %>:</td>
		<td>
			<%-- <html:select name='${form.formName}'
						 property="patientProperties.healthDistrict"
					     id="healthDistrictID"
					     disabled="true" >
			<option value="0" ></option>

			</html:select> --%>
			
			<form:select path="patientProperties.healthDistrict" id="healthDistrictID" disabled="true">
			<option value="0" ></option>
<%-- 			is this supposed to be value value or value id? --%>
			<form:options items="${patientProperties.healthDistricts}" itemLabel="value" itemValue="value"/>
			</form:select>
		</td>	
	</tr>		
	<% } %>
	</table>

	<table>
	<tr>
		<td style="text-align:right;">
			<spring:message code="patient.birthDate" />&nbsp;<%=DateUtil.getDateUserPrompt()%>:
			<% if(patientAgeRequired){ %>
				<span class="requiredlabel">*</span>
			<% } %>
		</td>
		<td>
			<%-- <nested:text name='${form.formName}'
					  property="patientProperties.birthDateForDisplay"
					  styleClass="text"
					  size="20"
                      maxlength="10"
                      onkeyup="addDateSlashes(this,event); normalizeDateFormat(this);"
                      onblur="checkValidAgeDate( this ); updatePatientEditStatus();"
					  id="dateOfBirthID" /> --%>
            <form:input path="patientProperties.birthDateForDisplay" 
                      onkeyup="addDateSlashes(this,event); normalizeDateFormat(this);"
                      onchange="checkValidAgeDate( this ); updatePatientEditStatus();"
                      id="dateOfBirthID" 
                      cssClass="text"
					  size="20"
                      maxlength="10"/>
			<div id="patientProperties.birthDateForDisplayMessage" class="blank" ></div>
		</td>
		<td style="text-align:right;">
			<spring:message code="patient.age" />:
		</td>
		<td >
            <%-- <html:text property="patientProperties.age"
                       name="${form.formName}"
                       size="3"
                       maxlength="3"
                       onchange="handleAgeChange( this ); updatePatientEditStatus();"
                       styleClass="text"
                    id="age"/> --%>
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
			<% if(patientGenderRequired){ %>
				<span class="requiredlabel">*</span>
			<% } %>
		</td>
		<td>
			<c:if test="${patientProperties.readOnly == false}" >
            <%-- <logic:equal value="false" name="${form.formName}" property="patientProperties.readOnly" > --%>
            <form:select path="patientProperties.gender" id="genderID" onchange="updatePatientEditStatus();" >
			<%-- <nested:select name='${form.formName}'
						 property="patientProperties.gender"
						 onchange="updatePatientEditStatus();"
						 id="genderID">
				<option value=" " ></option>
				<nested:optionsCollection name='${form.formName}' property="patientProperties.genders"   label="value" value="id" />
			</nested:select> --%>
			<option value=" " ></option>
			<form:options items="${patientProperties.genders}" itemLabel="value" itemValue="id"/>
			</form:select>
            <%-- </logic:equal> --%>
            </c:if>
            <c:if test="${patientProperties.readOnly}" >
            	<form:input path="patientProperties.gender" />
            </c:if>
            <%-- <logic:equal value="true" name="${form.formName}" property="patientProperties.readOnly" >
                <html:text property="patientProperties.gender" name="${form.formName}" />
            </logic:equal> --%>
		</td>
	</tr>
	<% if( supportInsurance || supportPatientType){ %>
	<tr>
		<% if( supportPatientType ){ %>
		<td style="text-align:right;">
			<spring:message code="patienttype.type" />:
		</td>
		<td>
			<form:select path="patientProperties.patientType" onchange="updatePatientEditStatus();" id="patientTypeID">
			<option value="0" ></option>
			<form:options items="${patientProperties.patientTypes}" itemLabel="description" itemValue="type"/>
				</form:select>
		</td>
		<% } if( supportInsurance ){ %>
		<td style="text-align:right;">
			<spring:message code="patient.insuranceNumber" />:
		</td>
		<td>
		<form:input path="patientProperties.insuranceNumber" onchange="updatePatientEditStatus();" id="insuranceID"/>
			<%-- <nested:text name='${form.formName}'
					  property="patientProperties.insuranceNumber"
					  onchange="updatePatientEditStatus();"
					  id="insuranceID"
					  styleClass="text"
					  size="20" /> --%>
		</td>
		<td style="text-align:right;">
		</td>
		<% } %>

	</tr>
	<% } if( supportOccupation ){ %>
	<tr>
	<td style="text-align:right;">
		<spring:message code="patient.occupation" />:
	</td>
	<td>
		<form:input path="patientProperties.occupation" onchange="updatePatientEditStatus();" id="occupationID"/>
		<%-- <nested:text name='${form.formName}'
				  property="patientProperties.occupation"
				  onchange="updatePatientEditStatus();"
				  id="occupationID"
				  styleClass="text"
				  size="20" /> --%>
	</td>
	</tr>
	<% } %>
	<% if( FormFields.getInstance().useField(Field.PatientEducation)){ %>
		<tr>
			<td style="text-align:right;"><spring:message code="patient.education"/>: </td>
				<td>
					<form:select path="patientProperties.education" id="educationID">
					<option value="0" ></option>
					<form:options items="${patientProperties.educationList}" itemLabel="value" itemValue="value"/>
					</form:select>
					<%-- <html:select name='${form.formName}'
								 property="patientProperties.education"
							     id="educationID" >
					<option value="0" ></option>
					<html:optionsCollection name="${form.formName}" property="patientProperties.educationList" label="value" value="value" />
					</html:select> --%>
				</td>	
			</tr>	
	<% } %>
	<% if( FormFields.getInstance().useField(Field.PatientMarriageStatus)){ %>
		<tr>
			<td style="text-align:right;"><spring:message code="patient.maritialStatus"/>: </td>
				<td>
					<form:select path="patientProperties.maritialStatus" id="maritialStatusID">
					<option value="0" ></option>
					<form:options items="${patientProperties.maritialList}" itemLabel="value" itemValue="value"/>
					</form:select>
					<%-- <html:select name='${form.formName}'
								 property="patientProperties.maritialStatus"
							     id="maritialStatusID" >
					<option value="0" ></option>
					<html:optionsCollection name="${form.formName}" property="patientProperties.maritialList" label="value" value="value" />
					</html:select> --%>
				</td>	
			</tr>	
	<% } %>
	<% if( ConfigurationProperties.getInstance().isPropertyValueEqual(ConfigurationProperties.Property.PATIENT_NATIONALITY, "true") ){ %>
		<tr>
			<td style="text-align:right;"><spring:message code="patient.nationality"/>: </td>
				<td>				
					<form:select path="patientProperties.nationality" id="nationalityID">
					<option value="0" ></option>	
					</form:select>					
				</td>
				<td><spring:message code="specify"/>:</td>
				<td>
					<%-- <form:input path="patientProperties.otherNationality" id="nationalityOtherId" /> --%>
					<form:input path="patientProperties.otherNationality" id="nationalityOtherId" />
				</td>	
			</tr>	
	<% } %>
	</table>
	</div>
</div>



<script type="text/javascript" >

//overrides method of same name in patientSearch
function selectedPatientChangedForManagement(firstName, lastName, gender, DOB, stNumber, subjectNumber, nationalID, mother, pk ){
	if( pk ){
		getDetailedPatientInfo();
		$("patientPK_ID").value = pk;
	}else{
		clearPatientInfo();
		setUpdateStatus("ADD");
	}
}

var registered = false;

function registerPatientChangedForManagement() {
	if (!registered) {
		if (typeof addPatientChangedListener === 'function') {
			addPatientChangedListener(selectedPatientChangedForManagement);
		}
		registered = true;
	}
}

registerPatientChangedForManagement();
</script> 
<script type="text/javascript" src="scripts/countries.js?" ></script>

