<%@ page language="java" contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants,
			org.openelisglobal.login.valueholder.UserSessionData,
			org.openelisglobal.common.util.*,org.openelisglobal.internationalization.MessageUtil,
            java.util.HashSet,org.owasp.encoder.Encode"%>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>


<%
	String requestType = (String)request.getSession().getAttribute("type");
	HashSet accessMap = (HashSet)request.getSession().getAttribute(IActionConstants.PERMITTED_ACTIONS_MAP);
	boolean isAdmin = ((UserSessionData) request.getSession().getAttribute(IActionConstants.USER_SESSION_DATA)).isAdmin();
	// no one should edit patient numbers at this time.  PAH 11/05/2010
	boolean canEditPatientSubjectNos =  isAdmin || accessMap.contains(IActionConstants.MODULE_ACCESS_PATIENT_SUBJECTNOS_EDIT);
	boolean canEditAccessionNo = isAdmin || accessMap.contains(IActionConstants.MODULE_ACCESS_SAMPLE_ACCESSIONNO_EDIT);
%>

<script type="text/javascript" src="scripts/utilities.js?"></script>
<script type="text/javascript" src="scripts/neon2/retroCIUtilities.js"></script>
<script type="text/javascript" src="scripts/neon/entryByProjectUtils.js"></script>
<script type="text/javascript">

var dirty = false;
/* TODO PAHill the code in retroCIUtilities.js uses the var type, while this page uses requestType.  We should have one. */
var requestType = '<%=Encode.forJavaScript(requestType)%>';
var type = requestType;
var pageType = "Patient";
var formName = '${form.formName}';
birthDateUsageMessage = "<spring:message code='error.dob.complete.less.two.years'/>";
previousNotMatchedMessage = "<spring:message code='error.2ndEntry.previous.not.matched'/>";
noMatchFoundMessage = "<spring:message code='patient.message.patientNotFound'/>";					
saveNotUnderInvestigationMessage = "<spring:message code='patient.project.conflicts.saveNotUnderInvestigation'/>";
var canEditPatientSubjectNos = <%= canEditPatientSubjectNos %>;
var canEditAccessionNo = <%= canEditAccessionNo %>;

function /*void*/ setSaveButton() {
	if (projectChecker != null ) {
		projectChecker.setSubjectOrSiteSubjectEntered();
	}
	var validToSave = fieldValidator.isAllValid() && requestType != "readonly";
	$("saveButtonId").disabled = !validToSave;
}

/**
 * General function for all AJAX search failures.
 * @param xhr the XML returned from the AJAX search
 */
function processSearchFailure(xhr)
{
	//alert( xhr.responseText );
	alert("<spring:message code="error.system"/>");
}

function  /*void*/ setMyCancelAction(form, action, validate, parameters)
{
	// patientLoader.compareDetails($("patientPK"));
	// actually needs to move to the 'third' AJAX thread after laoding for comparison of patient, sample and observation history fields.
	setAction(document.getElementById("mainForm"), 'Cancel', 'no', '');
}

function clearAllFormFields(formName) {
	var elements = document.forms[formName].elements;
	for (i=0; i< elements.length; i++) {
		clearFormElement(elements[i]);
	}
}

/**
 * This function is similar to utilities.js clearField( fieldId ), but covers other types of fields, is not clear that this is actually better.
 * @author PAHill
 */
function clearFormElement(field) {
    if (field == null) return;
	var type = field.type.toLowerCase();
	switch(type) {
	case "text":
	case "password":
	case "textarea":
	case "hidden":
		field.value = "";
		break;
	case "radio":
	case "checkbox":
		if (field.checked) {
			field.checked = false;
		}
		break;
	case "select-one":
	case "select-multi":
		field.selectedIndex = -1;
		break;
	default:
		break;
	}
}

function clearFormElements(fieldIds) {
	var fields = fieldIds.split(',');
	for (var i=0; i< fields.length; i++) {
		clearFormElement($(fields[i].trim()));
	}
} 
</script>


<script type="text/javascript">
function Studies() {
	this.validators = new Array();
	this.studyNames = ["InitialARV_Id", "FollowUpARV_Id", "RTN_Id", "_Id", "RTN_Id","EID_Id","VL_Id","Recency_Id"];

	this.validators["InitialARV_Id"] = new FieldValidator();
	this.validators["InitialARV_Id"].setRequiredFields( new Array("receivedDateForDisplay", "interviewDate", "centerCode", "subjectOrSiteSubject", "labNo", "gender", "dateOfBirth") );
	this.validators["FollowUpARV_Id"] = new FieldValidator();
	this.validators["FollowUpARV_Id"].setRequiredFields( new Array("subjectOrSiteSubject", "farv.receivedDateForDisplay", "farv.interviewDate", "farv.centerCode", "farv.labNo", "farv.gender", "farv.dateOfBirth") );

	this.validators["EID_Id"] = new FieldValidator();
	this.validators["EID_Id"].setRequiredFields( new Array("eid.receivedDateForDisplay", "eid.interviewDate", "eid.centerCode", "eid.centerName", "subjectOrSiteSubject", "eid.labNo", "eid.dateOfBirth", "eid.gender" ) );
	
	this.validators["VL_Id"] = new FieldValidator();
	this.validators["VL_Id"].setRequiredFields( new Array("vl.receivedDateForDisplay", "vl.interviewDate", "subjectOrSiteSubject", "vl.labNo", "vl.dateOfBirth", "vl.gender" ) );
	
	this.validators["RTN_Id"] = new FieldValidator();
	this.validators["RTN_Id"].setRequiredFields( new Array("rtn.labNo", "rtn.receivedDateForDisplay", "rtn.interviewDate", "rtn.gender", "rtn.dateOfBirth", "rtn.nameOfDoctor", "rtn.service", "rtn.hospital") );

	this.validators["Recency_Id"] = new FieldValidator();
	this.validators["Recency_Id"].setRequiredFields(new Array("rt.centerCode", "rt.receivedDateForDisplay","rt.interviewDate", "rt.gender", "rt.labno", "rt.asanteTest","rt.dateOfBirth"));
	
	this.getValidator = function /*FieldValidator*/ (divId) {
		return this.validators[divId];
	}

	this.projectChecker = new Array();

	/**
	 * checkers are all about the business logic of what fields have to match an existing record at what time
	 * while validators are enforcing required fields while using lists of field names, so don't have much specifics.
	 */
	this.initializeProjectChecker = function () {
		this.projectChecker["InitialARV_Id"] = iarv;
		this.projectChecker["FollowUpARV_Id"] = farv;
		this.projectChecker["EID_Id"] = eid;	
		this.projectChecker["VL_Id"] = vl;
		this.projectChecker["RTN_Id"] = rtn;
		this.projectChecker["Recency_Id"] = rtn;
	}

	this.getProjectChecker = function (divId) {
		this.initializeProjectChecker(); // not clear why a navigating back to this page makes field checkers empty, so we'll always load.
		return this.projectChecker[divId];
	}
}

studies = new Studies();
projectChecker = null;

// set the selected value of the study drop down and switch to display that study
function selectStudy( divId ) {
	var i = getSelectIndexFor("studyFormsID", divId);
	document.getElementById("mainForm").studyForms.selectedIndex = i;
	switchStudyForm( divId );
}

var formFields = null;
// Switch the form value to a study and show the element by that name.
// the ID of the div with the right questions is the same as the projectFormName
function switchStudyForm( divId ) {
	hideAllDivs();
	//setDefaultTests(divId);
	if (divId != "" && divId != "0") {
		sessionStorage.setItem("selectedDivId",divId);
		$("projectFormName").value = divId;
    	toggleDisabledDiv(document.getElementById(divId), true);
		document.getElementById(divId).style.display = "block";
		fieldValidator = studies.getValidator(divId); // reset the page var fieldValidator for all fields to use.
		projectChecker   = studies.getProjectChecker(divId);	// reset the page var projectChecker
		projectChecker.setSubjectOrSiteSubjectEntered();	
	    adjustFieldsForRequestType();
		setSaveButton();
	} else {
	    adjustFieldsForRequestType()
		setSaveButton();
	}
}

function adjustFieldsForRequestType() {
	// TODO PAHill -- maybe one guard check at the top for projectChecker?  No need to tweek fields if there are no fields to show right?
	if ( projectChecker != null ) {
		projectChecker.setSubjectOrSiteSubjectEntered();
		var nameField = $(projectChecker.idPre + "centerName");
		var codeField = $(projectChecker.idPre + "centerCode");
		if ( nameField && codeField ) {
			syncLists(codeField, nameField);		
		}	
		
	}
	
  	if (requestType == "readwrite" || requestType == "readonly") {
    	//$("studyFormsID").style.display = "none";
    	if ( projectChecker != null ) {
			$(projectChecker.idPre + "patientRecordStatusRow").style.display = "table-row";
			$(projectChecker.idPre + "sampleRecordStatusRow").style.display = "table-row";
		}
		//$("iarv.underInvestigationCommentRow").style.display = "table-row";
    }

	var elements = document.getElementById("mainForm").elements;
	
	if (projectChecker == null ) {
		return;
	}
	
	switch (requestType) {
	case "initial":
		break;
	case "verify":
		break;		
	case "readwrite":
		// projectChecker.setFieldsForEdit( canEditPatientSubjectNos, canEditAccessionNo );    		
		setSaveButton();
		break;
	case "readonly":
		if (projectChecker != null ) {
	    	$("hivStatusRow").style.display = "table-row";
		}
		for (i=0; i< elements.length; i++) {
			setFieldReadOnly(elements[i], true);
		}
	//	setFieldReadOnly( document.getElementById("mainForm").searchLastName, false);
	//	setFieldReadOnly( document.getElementById("mainForm").searchFirstName, false);
	//	setFieldReadOnly( document.getElementById("mainForm").searchNationalID, false);
		setFieldReadOnly( document.getElementById("mainForm").searchButton, false);
		setFieldReadOnly( document.getElementById("mainForm").cancelButtonId, false);
	//	setFieldReadOnly( document.getElementById("mainForm").searchLabNumber, false);
        setFieldReadOnly( document.getElementById("mainForm").saveButtonId, false );
        setFieldReadOnly( document.getElementById("mainForm").searchValue, false );
        setFieldReadOnly( document.getElementById("mainForm").searchCriteria, false );
		document.getElementById("mainForm").saveButtonId.disabled = true;
		break;
	}
}

function setProjectName(divId) {

}


/**
 * A list of answers that equate to yes in certain lists when comparing (cross check or 2nd entry for a match).
 */
yesesInDiseases = [
     <%=Encode.forJavaScript(org.openelisglobal.dictionary.ObservationHistoryList.YES_NO.getList().get(0).getId()) %>,
	 <%=Encode.forJavaScript(org.openelisglobal.dictionary.ObservationHistoryList.YES_NO_UNKNOWN.getList().get(0).getId()) %>
	 ];
	 
var type = '<%=Encode.forJavaScript(requestType)%>';

function hideAllDivs(){
	toggleDisabledDiv(document.getElementById("InitialARV_Id"), false);
	toggleDisabledDiv(document.getElementById("FollowUpARV_Id"), false);
	toggleDisabledDiv(document.getElementById("RTN_Id"), false);
	toggleDisabledDiv(document.getElementById("EID_Id"), false);
	toggleDisabledDiv(document.getElementById("VL_Id"), false);
	toggleDisabledDiv(document.getElementById("Recency_Id"), false);

	document.getElementById('InitialARV_Id').style.display = "none";
	document.getElementById('FollowUpARV_Id').style.display = "none";
	document.getElementById('RTN_Id').style.display = "none";
	document.getElementById('EID_Id').style.display = "none";
	document.getElementById('VL_Id').style.display = "none"; 
	document.getElementById('Recency_Id').style.display = "none"; 
}
</script>

<script type="text/javascript">

//all methods here either overwrite methods in tiles or all called after they are loaded

function /*void*/ makeDirty(){
	dirty=true;
	if( typeof(showSuccessMessage) === 'function' ){ 
		showSuccessMessage(false); //refers to last save
	}
	// Adds warning when leaving page if content has been entered into makeDirty form fields
	function formWarning(){ 
    return '<spring:message code="banner.menu.dataLossWarning"/>';
	}
	window.onbeforeunload = formWarning;
}

function savePage__() {
	window.onbeforeunload = null; // Added to flag that formWarning alert isn't needed.
	var form = document.getElementById("mainForm");
	if ( requestType == "readwrite" ) {
		form.action = "PatientEditByProject?type=" + requestType;
	} else {
		form.action = "PatientEntryByProject?type=" + requestType;
	}
	form.submit();
}

function initializeStudySelection() {
	//selectStudy($('projectFormName').value);
	selectStudy(sessionStorage.getItem("selectedDivId"));
}

</script>
<b><spring:message code="sample.entry.project.form" /> </b>
<c:if test="${not empty patientSearchFragment}">
	<jsp:include page="${patientSearchFragment}"/>
</c:if>
<%-- <tiles:insertAttribute name="patientSearch" ignore="true"/> --%>
<br/>
<select name="studyForms" onchange="switchStudyForm(this.value);" id="studyFormsID">
	<option value="0" selected>
	</option>
	<option value="InitialARV_Id">
		<spring:message code="sample.entry.project.initialARV.title" />
	</option>
	<option value="FollowUpARV_Id">
		<spring:message code="sample.entry.project.followupARV.title" />
	</option>
	<option value="RTN_Id">
		<spring:message code="sample.entry.project.RTN.title" />
	</option>
	<option value="VL_Id" ><spring:message code="sample.entry.project.VL.title"/></option>
	<option value="EID_Id" ><spring:message code="sample.entry.project.EID.title"/></option>
	<option value="Recency_Id" ><spring:message code="sample.entry.project.RT.title"/></option>
</select>
<br />
<hr />

<form:hidden path="observations.projectFormName" id="projectFormName" />
<form:hidden path="patientUpdateStatus" id="processingStatus"  />
<form:hidden path="patientPK" 			id="patientPK" />
<form:hidden path="samplePK" 			id="samplePK" />
<form:hidden path="" id="subjectOrSiteSubject" value="" />
<form:hidden path="patientFhirUuid" id="patientFhirUuid"/>
<div id="studies">
	<div id="InitialARV_Id" style="display: none;">
		<jsp:include page="${arvInitialStudyFragment}"/>
	</div>
	
 	<div id="FollowUpARV_Id" style="display: none;">
		<jsp:include page="${arvFollowupStudyFragment}"/>
	</div>
	<div id="EID_Id" style="display: none;">
		<jsp:include page="${ediStudyFragment}"/>
	</div>
	<div id="VL_Id" style="display: none;">
		<jsp:include page="${lvStudyFragment}"/>
	</div>
	<div id="RTN_Id" style="display: none;">
		<jsp:include page="${rtnStudyFragment}"/>
	</div>
	<div id="Recency_Id" style="display: none;">
		<jsp:include page="${rtStudyFragment}"/>
	</div>
</div>
<script type="text/javascript">
function checkVLSampleType(e){
	$("vl.pscvlTaken").checked = (e.id === "vl.pscvlTaken");
	$("vl.dbsvlTaken").checked = (e.id === "vl.dbsvlTaken");
	$("vl.edtaTubeTaken").checked = (e.id === "vl.edtaTubeTaken");
}

// All openElis struts pages have a function to override to do some work onLoad
function onLoad() {
	// alert("load 1 " + $("projectFormName").value);
	initializeStudySelection();
	studies.initializeProjectChecker();
	registerPatientSearchChanged();
	projectChecker == null || projectChecker.refresh();
	vl.checkGenderForVlPregnancyOrSuckle();
}
</script>
