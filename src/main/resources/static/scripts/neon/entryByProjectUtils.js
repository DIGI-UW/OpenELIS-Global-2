/*
* The contents of this file are subject to the Mozilla Public License
* Version 1.1 (the "License"); you may not use this file except in
* compliance with the License. You may obtain a copy of the License at
* http://www.mozilla.org/MPL/
*
* Software distributed under the License is distributed on an "AS IS"
* basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
* License for the specific language governing rights and limitations under
* the License.
*
* The Original Code is OpenELIS code.
*
* Copyright (C) The Minnesota Department of Health.  All Rights Reserved.
*
* Contributor(s): CIRG, University of Washington, Seattle WA.
*/

/**
 * Cote d'Ivoire
 * There are two-ways through these objects one to "load" patient/subject then sample then observation history when searching on the edit/view screen.
 * The other is to "find" each when doing comparison during entry or 2nd entry.
 * @author pahill
 * @since 2010-07-16
 */
//Copied-From patientManagement.jsp with changes
// We could put all of this 'copied-from' into a reusable JS Object included in a JSP file then here we would provide an override
// of setPatientInfo with all of the values returned by patient search since FormFields encapsulats which fields to show and
// setPatientInfo would pick up those to map into the edit form.
function selectedPatientChanged(firstName, lastName, gender, DOB, stNumber, subjectNumber, nationalID, mother, pk ){
	if( pk ){
	    fieldValidator.setAllFieldsValid();
	    patientLoader.clearFields();
	    sampleLoader.clearFields();
	    observationHistoryLoader.clearFields();
		$("patientPK").value = pk;
		patientLoader.loadDetails( pk );
		//  patient loading will be followed by sample loading will be followed by observationHistory.loadDetails
		hivStatusLoader.load( pk, fieldValidator.idPre + "hivStatus" );  // only needed viewing of HIVStatus somewhere other than hivFollowup after test is complete. 
	}else{
		patientLoader.clearFields();
	    sampleLoader.clearFields();
	    observationHistoryLoader.clearFields();
	    $("patientPK").value = null;
	    $("samplePK").value = null;
	    hivStatusLoader.clearFields();
	    switchStudyForm( "" );
	}
}

// connect this page up to the patient search tile functionality
var registered_PatientSearchChanged = false;
function registerPatientSearchChanged(){
	if( registered_PatientSearchChanged == false && window.addPatientChangedListener != undefined){
		addPatientChangedListener( selectedPatientChanged );
		registered_PatientSearchChanged = true;
	}
}

function processLoadFailure() {
	alert("loading failure.");
}

/**
 * Dynamically changes a field to be required as controlled by the drop down values of another field.
 * @param field - the field which controls a second field
 * @param subfield - the sometimes required field controlled by the field.
 * @param otherOptions - < 0 => number of choices at the END of options list which means that subfield is now required.
 *    The assumption is that 'other' and 'none-of-the-above' are typically listed at the end.
 *    otherOptions > 0 => exact item in the options list (e.g. the YES) which means that the subfield is now required.
 * @param elementsStr - the larger divs/TDs etc. which contains all of the HTML to show/hide; comma separate list
 * @author pahill
 */
function enableSelectedRequiredSubfield( field, subfield, otherOptions, elementsStr ) {
	if ( otherOptions < 0 ) {
		nowRequired = field.selectedIndex > field.options.length - 1 + otherOptions;
	} else {
		nowRequired = (field.selectedIndex == otherOptions);
	}
	// because the fieldChecker and fieldValidator is not switched before loading all fields (including ones controlling subfields),
	// all checkers are told about all DB values when loaded, and this function is called by update() so would declare a field in another form required,
	// you can never get that other field filled in and thus no save button ever.
	// until we fix we won't make any subfield required.
//	enableSubfield(nowRequired, subfield);
	showElements(nowRequired, elementsStr);
}

/**
 * Do all the right tweeking to the UI for a field that is required depending on another field.
 * @param required - true => make it visible and required; false => make it invisible and not required
 * @param subfield - the field to change
 */
function enableSubfield( required, subfield ) {
	
	if ( required ) {
		subfield.disabled = false;
		subfield.style.visibility = "visible";
		subfield.style.display = "inline";
		fieldValidator.addRequiredField( subfield.id );
		updateFieldValidity( !$(subfield.value.blank()), subfield.id);
	} else {
		subfield.disabled = true;
		subfield.style.visibility = "hidden";
		subfield.style.display = "none";
		fieldValidator.removeRequiredField( subfield.id );
		updateFieldValidity( true, subfield.id);
	}
}

function showElements( show, elementsStr ) {
	var elements = elementsStr.split(',');
	for (var i=0; i< elements.length; i++) {
		var fieldName = elements[i];
		if ($(fieldName) == null) {
			// alert( "Programmer Error: in showElements(): field \"" + fieldName + "\" not found.");
		} else {
			if (show) {
				$(fieldName).style.display = "";  // bring it back, not "block" or "in-line", just let it be its default.
			} else {
				$(fieldName).style.display = "none";
			}
		}
	}
}

/**
 * If one field is checked, check a list of others.  If the field is unchecked make the others unchecked.
 * @param field - the field (not ID) to check the state of.
 * @param parem2, param3, param4 .. the IDs of other fields to match to the 1st. 
 * @return void
 */
function /*void*/ synchronizeCheckBoxes(){
	var checked = arguments[0].checked;

	for(var i = 1; i < arguments.length; i++ ){
		$(arguments[i]).checked = checked;
	}
}

function  /*void*/ savePage() {
	if ( projectChecker != null && projectChecker.checkAllFields != undefined) {
		projectChecker.checkAllFields(false);
	} else {
		alert("projectChecker.checkAllFields undefined");
	}

	if ( fieldValidator.isAnyConflicted() == 0 ) {
		savePage__();
	} else if ( confirm(saveNotUnderInvestigationMessage) ) {
		savePage__();
	} else {
		// no auto save, no on confirm save, thus nothing to do.
	}
}

	function validateSiteSubjectNumber(field){
		 const siteSubjectNumber = /^([0-9A-Za-z]{5}\/[0-9A-Za-z]{2})\/[\d]{2}\/[\d]{5}[Ee]?$/g;
		 if(field.value){
			  if(siteSubjectNumber.test(field.value)){
				  field.classList.remove("error");
			  }
			  else{
				  field.classList.add("error");
			  }
		 }
	}

// class BaseLoader to work with the patient search tile and load (or remember) the results returned.
function BaseLoader() {
	/** 
	 * method to actually make the ajax call
	 */  
    this.doAjax = function /*void*/ (urlParameters, onSuccessFunc, onFailureFunc) {
    	if (onFailureFunc == null) {
    		onFailureFunc = processLoadFailure;
    	}
		new Ajax.Request ('ajaxQueryXML',  //url
                        {//options
                          method: 'get', //http method
                          parameters: urlParameters,
						  requestHeaders : {
								"X-CSRF-Token" : getCsrfToken()
						  },
                          onSuccess:  onSuccessFunc, onFailure: onFailureFunc
                         });
	}
    /**
     * Look up in any response a value given a key
     * @param response  the value in the XML for the key
     * @param key an element in the response of the form <key>value</key>
     * @return
     */
	this.getResponseProperty = function /*string*/ getResponseProperty( response, key ) {
		var field = null;
		if( response != null ) {
			field = response.getElementsByTagName(key).item(0);
		}

		if( field != null )	{
			return field.firstChild.nodeValue;
		} else {
			return undefined;
		}
	};
	
	/**
	 * Get a value from the current saved existing value (return probably save as a result of a this.find...(...) call
	 * @param tagName <tagName>value</tagName>
	 * @return the value in the XML
	 */
	this.getExistingValue = function /* String */ (tagName) {
		return this.getResponseProperty(this.existing, tagName);
	}	

	/**
	 * A kludge method that just tries the name of field in all possible studies.
	 * Used when loading for editing, so that we don't leave value lying around in fields (in hidden tabs on the same page)
	 * when a new one is loaded.
	 * @param fieldId
	 */
	this.clearFieldInAllStudies = function (fieldId) {
		clearField(document.getElementById(fieldId));
		clearField(document.getElementById("farv." + fieldId));
		clearField(document.getElementById("eid." + fieldId));
		clearField(document.getElementById("vl." + fieldId));
		clearField(document.getElementById("rt." + fieldId));
		clearField(document.getElementById("rtn." + fieldId));
	}

	/**
	 * A kludge method which takes a value and pushes it into all study field without regard to the current study.
	 * TODO PAHill Note: This could be made faster and less stupid by directly using the current projectChecker page global and setting
	 * only the field of the current study form.
	 *    
	 * @param fieldId
	 * @param value
	 * @return
	 */
	this.setFieldInAllStudies = function (fieldId, value) {
		// console.info("updating field " + fieldId + " to " + value);
		this.setField(fieldId, value);
		this.setField("farv." + fieldId, value);
		this.setField("rtn." + fieldId, value);
		this.setField("eid." + fieldId, value);
		this.setField("vl." + fieldId, value);
		this.setField("rt." + fieldId, value);
	};

	/**
	 * Set a form field with a value, includes either a dropdown list or text field
	 */
	this.setField = function (fieldId, value) {
		var element = document.getElementById(fieldId);
		//console.info("Setting field " + fieldId + " to " + value); // firebugs only

		if (element != null) {
			// TODO PAHill 2009-07-11  Not all field types are covered by this method
			if (element.type == "text" || element.type == "hidden") {
				// console.info("Field " + fieldId + ".value = " + value);
				element.value = value;
			} else {
				// console.info("Field " + fieldId + " set to index of " + value);
				setInputSelectionIndex(element, value);
			}
		} else {
			// console.info("ObservationHistory.setField: " + fieldId + " does not exist."); // firegbugs only
		}
	}
}

/**
 * AJAX Loader for patient information.
 */
function PatientLoader() {
	this.url = "provider=PatientSearchPopulateProvider";
	
	this.existing = null;
	/**
	 * This member indicates that value of the subjectNumber loaded from a sampleNumber
	 */
	this.existingSubjectNumber = "";
	
    this.loadDetails = function  /*void*/ (pk, sampleNo ) {
		this.doAjax( this.url + "&personKey=" + pk,
			function /*void*/ (xhr){
		        patientLoader.processLoadSuccess(xhr, sampleNo);
		    }
		);
	};
	
	this.clearExisting = function (clearExistingSubjectNumber) {
		this.existing = null;
		if (clearExistingSubjectNumber) {
			this.existingSubjectNumber = null;
		}
	}

	this.findPatientBy = function (byParameter, subNoField, isRequired, func) { // nationalID or personKey
		var subjectNumber = subNoField.value;
		var fieldId = subNoField.id;
		this.existing = null;
		this.doAjax( this.url + "&" + byParameter + "=" + subjectNumber,
			function /*void*/ (xhr){
		        patientLoader.processFindSuccess(xhr, isRequired, fieldId, func);
		    }
		);
	}

	this.processFindSuccess = function (xhr, isRequired, fieldId, nextFunc) {
		var xml = xhr.responseXML;
		//alert(xhr.responseText);
		var message = this.getResponseProperty(xml, "message");
		if (message == 'valid') {
			this.existing = xml.getElementsByTagName("formfield").item(0);
			$('patientPK').value =  this.getResponseProperty(this.existing, "ID");
			updateFieldConflict(true, fieldId, "");
			updateFieldValidity(true, fieldId);
		} else {
			// PAH 11/16 this.existing = undefined;
			$('patientPK').value = null;
			hivStatusLoader.existing = undefined;	// the hivStatus gets cleared also 
			if (isRequired) {
				updateFieldConflict(false, fieldId, message);
				updateFieldValidity(false, fieldId);
			}
		}
		nextFunc();
	}

	this.clearFields = function  /*void*/ (){
		this.setFields();
	}

	this.processLoadSuccess = function /*void*/ (xhr, sampleNo) {
		// console.info("PatientLoader.processLoadSuccess:" + xhr.responseText);
		var response = xhr.responseXML.getElementsByTagName("formfield").item(0);
		this.existing = response;
		patientLoader.setExistingSubjectNumber();
		var nationalID = this.getResponseProperty(response, "nationalID");
		var lastName  = this.getResponseProperty(response, "lastName");
		var firstName = this.getResponseProperty(response, "firstName");
		var externalID = this.getResponseProperty(response, "externalID");
		var dob = this.getResponseProperty(response,   "dob");
		var gender = this.getResponseProperty(response, "gender");

		this.setFields(nationalID, lastName, firstName, externalID, dob, gender);
		var searchLabNumber = $("searchLabNumber").value; 
		if ( searchLabNumber != "" ) {
			sampleLoader.loadDetailsByAccessionNumber(searchLabNumber);
		} else {
			sampleLoader.loadDetails($("patientPK").value);
		}
	}
	
	
	
	/**
	 * load part 2: given everything read from the AJAX call, set all the fields
	 */
	this.setFields = function /* void */ (nationalID, lastName, firstName, externalID, dob, gender) {
	   	this.setFieldInAllStudies("subjectNumber", blankOrValue(nationalID));
		this.setFieldInAllStudies("patientFamilyName", blankOrValue(lastName));
		this.setFieldInAllStudies("patientFirstNames", blankOrValue(firstName));
		this.setFieldInAllStudies("siteSubjectNumber", blankOrValue(externalID));

		if (dob == undefined) {
			dob = "";
			this.setFieldInAllStudies("dateOfBirth", dob);
		} else {
			this.setFieldInAllStudies("dateOfBirth", dob);
			// TODO PAHill we could go back to projectChecker to do only the current fields instead of banging on all of them, but we don't know the type of study yet because we haven't loaded the sample.
			/*handlePatientBirthDateChange($("dateOfBirth"), $("interviewDate"), false, $("age"));
			handlePatientBirthDateChange($("farv.dateOfBirth"), $("farv.interviewDate"), false, $("farv.age"));
			handlePatientBirthDateChange($("eid.dateOfBirth"),  $("eid.interviewDate"), false, null, $('eid.month'), $('eid.ageWeek'));
			handlePatientBirthDateChange($("vl.dateOfBirth"),  $("vl.interviewDate"), false, $("vl.age"));
			handlePatientBirthDateChange($("rt.dateOfBirth"),  $("rt.interviewDate"), false, $("rt.age"));
			handlePatientBirthDateChange($("rtn.dateOfBirth"),  $("rtn.interviewDate"), false, $("rtn.age"), $("rtn.month")); */
		} 
		this.setFieldInAllStudies("gender", gender);
	}
	
	/**
	 * This is only called after a sample is found and is valid in 2nd Entry triggered by a loading of a valid patient
	 */
	this.handleNewPatientId = function () {
		patientLoader.findPatientBy("personKey", $("patientPK"), true,
			function () {
				patientLoader.setExistingSubjectNumber();
				projectChecker.checkAllSubjectFields(true, true);
				// now back over to the loaders to load observation history and load hivStatus
				observationHistoryLoader.findDetails();
				hivStatusLoader.find($("patientPK").value);
			}
		);
	}
		
	/**	
	 * At some point we finish loading the patient associated with the sample and we need to record the original sample number of the patient  that is associated with the current sample.
	 * @return
	 */
	this.setExistingSubjectNumber = function () {
		this.existingSubjectNumber = this.getResponseProperty(this.existing, "nationalID"); 
	}	
}

/**
 * Ajax Loading for sample information
 */
function SampleLoader() {
	this.existing = null;
	this.url = "provider=SampleSearchPopulateProvider";
	
	/**
	 * Load (push value into the actual form fields) all of sample related fields. 
	 * @param pk the patient primary PK
	 */
    this.loadDetails = function  /*void*/ (pk) {
		this.doAjax(this.url + "&patientKey=" + pk,
			function /*void*/ (xhr){
		    	sampleLoader.processLoadSuccess(xhr);
			}
		);
	};

	/**
	 * Method to set all the fields which might be loaded to blank, so that a field unspecified form the server doesn't leave an old value in the field.
	 */
	this.clearFields = function  /*void*/ (){
		this.setFields();
	}

	/**
	 * Callback when sample loading comes back from the server.
	 */
	this.processLoadSuccess = function /* void */ (xhr) {
		// console.info("SampleLoader.processLoadSuccess " + xhr.responseText);
		var response = xhr.responseXML.getElementsByTagName("formfield").item(0);
		this.existing = response;
		var patientPK = this.getResponseProperty(response, "patientPK");
		var samplePK = this.getResponseProperty(response, "samplePK");
		// console.info("samplePK from AJAX is " + samplePK);
		var labNo = this.getResponseProperty(response, "labNo");
		var receivedDate = this.getResponseProperty(response, "receivedDateForDisplay");
		var interviewDate = this.getResponseProperty(response, "collectionDateForDisplay");
		var centerName = this.getResponseProperty(response, "centerName");
		var centerCode = this.getResponseProperty(response, "centerCode");

		this.setFields(samplePK, labNo, receivedDate, interviewDate, centerName, centerCode);
		observationHistoryLoader.loadDetails();
	};
	
	/**
	 * sample load part 2:  set all the fields related directly to the sample
	 * @return
	 */
	this.setFields = function /* void */ (samplePK, labNo, receivedDate, interviewDate, centerName, centerCode) {
		$("samplePK").value = blankOrValue(samplePK);
		// console.info("(samplePK).value = " + $("samplePK").value);
		labNo = blankOrValue(labNo);
		this.setFieldInAllStudies("labNo", labNo);
        // All prefixes have 4-letters, so we can strip 4 to get the number
		this.setFieldInAllStudies("labNoForDisplay", labNo.substring(4));
		this.setFieldInAllStudies("receivedDateForDisplay", blankOrValue(receivedDate));
		this.setFieldInAllStudies("interviewDate", blankOrValue(interviewDate));
		this.setFieldInAllStudies("centerName", blankOrValue(centerName));
		this.setFieldInAllStudies("centerCode", blankOrValue(centerCode));
	};
	
	this.loadDetailsByAccessionNumber = function (accession) {
		this.doAjax(this.url + "&accessionNo="+ accession,
				function /*void*/ (xhr){
			    	sampleLoader.processLoadSuccess(xhr);
				}
			);
	}
	
	/**
	 * Starting with an accession number, find (get value from server but don't push into the actual form fields) all the details.  
	 * @return
	 */
	this.findDetailsByAccessionNumber = function (accession, isRequired, fieldId, otherFieldsToCheckFunc) {
		this.doAjax(this.url + "&accessionNo="+ accession,
			function /*void*/ (xhr){
		    	sampleLoader.processFindSuccess(xhr, isRequired, fieldId, otherFieldsToCheckFunc);
			}
		);
	}

	/**
	 * PatientLoader Callback for find method(s)
	 */
	this.processFindSuccess = function(xhr, isRequired, fieldId, extFunction) {
		var xml = xhr.responseXML;
		var message = this.getResponseProperty(xml, "message");
		var result = xml.getElementsByTagName("formfield").item(0);
		var samplePK = this.getResponseProperty(result, "samplePK");	// no sample ID => nothing found
		if (samplePK != null) {
			this.existing = result;
			$('samplePK').value = samplePK;
			$('patientPK').value = this.getResponseProperty(this.existing, "patientPK");
			updateFieldConflict(true, fieldId, "");
			updateFieldValidity(true, fieldId);
		} else if (isRequired) {
			updateFieldConflict(false, fieldId, "?");
			updateFieldValidity(false, fieldId);
		}

		patientLoader.handleNewPatientId();
		projectChecker.checkAllSampleFields(true);
	}	
}

/**
 * Object to do loading (push into form) or finding (just get values) for ObservationHistory information.
 */
function ObservationHistoryLoader() {
	this.url = "provider=ObservationHistoryPopulateProvider";
	this.existing = null;

    this.loadDetails = function  /*void*/ () {
		this.doAjax(this.url + "&patientKey=" + $("patientPK").value + "&sampleKey=" + $("samplePK").value,
			function /*void*/ processLoadSuccess(xhr){
				observationHistoryLoader.processLoadSuccess(xhr);
			}
		);
	};
	
	/**
	 * find the observation history information based on the patient AND sample IDs
	 */
	this.findDetails = function () {
		var patientPK = $("patientPK").value;
		var samplePK = $("samplePK").value;
		if (patientPK == null || samplePK == null) {
			this.existing = null;
			return;
		}
		this.doAjax(this.url + "&patientKey=" + patientPK + "&sampleKey=" + samplePK  ,
			function /*void*/ processFindSuccess(xhr){
				observationHistoryLoader.processFindSuccess(xhr);
			}
		);
	};

	/**
	 * callback for finding (don't update form just get the data for) OH information
	 */
	this.processFindSuccess = function (xhr) {
		var xml = xhr.responseXML;
		var message = this.getResponseProperty(xml, "message");
		this.existing = xml.getElementsByTagName("formfield").item(0);
		this.afterFind();
	}

	/**
	 * clear all of the fields which are actually observation history fields.
	 */
	this.clearFields = function  /*void*/ (){
		this.clearFieldInAllStudies("projectFormName");
		this.clearFieldInAllStudies("educationLevel");
		this.clearFieldInAllStudies("maritalStatus");
		this.clearFieldInAllStudies("nationality");
		this.clearFieldInAllStudies("nationalityOther");
		this.clearFieldInAllStudies("legalResidence");
		this.clearFieldInAllStudies("nameOfDoctor");
		this.clearFieldInAllStudies("anyPriorDiseases");

		this.clearFieldInAllStudies("priorDiseases0");
		this.clearFieldInAllStudies("priorDiseases1");
		this.clearFieldInAllStudies("priorDiseases2");
		this.clearFieldInAllStudies("priorDiseases3");
		this.clearFieldInAllStudies("priorDiseases4");
		this.clearFieldInAllStudies("priorDiseases5");
		this.clearFieldInAllStudies("priorDiseases6");
		this.clearFieldInAllStudies("priorDiseases7");
		this.clearFieldInAllStudies("priorDiseases8");
		this.clearFieldInAllStudies("priorDiseases9");
		this.clearFieldInAllStudies("priorDiseases10");
		this.clearFieldInAllStudies("priorDiseases11");
		this.clearFieldInAllStudies("priorDiseases12");
		
		this.clearFieldInAllStudies("arvProphylaxisBenefit");
		this.clearFieldInAllStudies("arvProphylaxis");
		this.clearFieldInAllStudies("currentARVTreatment");
		this.clearFieldInAllStudies("priorARVTreatment");
		this.clearFieldInAllStudies("cotrimoxazoleTreatment");
		this.clearFieldInAllStudies("aidsStage");
		this.clearFieldInAllStudies("anyCurrentDiseases");
		
		this.clearFieldInAllStudies("currentDiseases0");
		this.clearFieldInAllStudies("currentDiseases1");
		this.clearFieldInAllStudies("currentDiseases2");
		this.clearFieldInAllStudies("currentDiseases3");
		this.clearFieldInAllStudies("currentDiseases4");
		this.clearFieldInAllStudies("currentDiseases5");
		this.clearFieldInAllStudies("currentDiseases6");
		this.clearFieldInAllStudies("currentDiseases7");
		this.clearFieldInAllStudies("currentDiseases8");
		this.clearFieldInAllStudies("currentDiseases9");
		this.clearFieldInAllStudies("currentDiseases10");
		this.clearFieldInAllStudies("currentDiseases11");
		this.clearFieldInAllStudies("currentDiseases12");
		
		this.clearFieldInAllStudies("currentOITreatment");
		this.clearFieldInAllStudies("patientWeight");
		this.clearFieldInAllStudies("karnofskyScore");
		this.clearFieldInAllStudies("hivStatus");
		this.clearFieldInAllStudies("cd4Count");
        this.clearFieldInAllStudies("cd4Percent");
        this.clearFieldInAllStudies("priorCd4Date");
        this.clearFieldInAllStudies("antiTbTreatment");
        this.clearFieldInAllStudies("interruptedARVTreatment");
        this.clearFieldInAllStudies("arvTreatmentAnyAdverseEffects" );
        this.clearFieldInAllStudies("arvTreatmentChange");
        this.clearFieldInAllStudies("arvTreatmentNew");
        this.clearFieldInAllStudies("arvTreatmentRegime");
        this.clearFieldInAllStudies("cotrimoxazoleTreatAnyAdvEff");
        this.clearFieldInAllStudies("anySecondaryTreatment");
        this.clearFieldInAllStudies("secondaryTreatment");
        this.clearFieldInAllStudies("clinicVisits");

        this.clearFieldInAllStudies("hospital");
        this.clearFieldInAllStudies("service");
        this.clearFieldInAllStudies("hospitalPatient");

        this.clearFieldInAllStudies("whichPCR");
        this.clearFieldInAllStudies("reasonForSecondPCRTest");
        this.clearFieldInAllStudies("indFirstTestName");
        this.clearFieldInAllStudies("indSecondTestName");
        this.clearFieldInAllStudies("indFirstTestDate");
        this.clearFieldInAllStudies("indSecondTestDate");
        this.clearFieldInAllStudies("indFirstTestResult");
        this.clearFieldInAllStudies("indSecondTestResult");
        this.clearFieldInAllStudies("indSiteFinalResult");

        this.clearFieldInAllStudies("eidInfantPTME"           );
        this.clearFieldInAllStudies("eidTypeOfClinic"         );
        this.clearFieldInAllStudies("eidHowChildFed"          );
        this.clearFieldInAllStudies("eidStoppedBreastfeeding" );
        this.clearFieldInAllStudies("eidInfantSymptomatic"    );
        this.clearFieldInAllStudies("eidMothersHIVStatus"     );
        this.clearFieldInAllStudies("eidMothersARV"           );
        this.clearFieldInAllStudies("eidInfantsARV"           );
        this.clearFieldInAllStudies("eidInfantCotrimoxazole"  );
        this.clearFieldInAllStudies("reasonForRequest"     );   
        this.clearFieldInAllStudies("underInvestigationComment");   
        this.clearFieldInAllStudies("underInvestigation");
        
        this.clearFieldInAllStudies("CTBPul"     );
		this.clearFieldInAllStudies("CTBExpul"   );
		this.clearFieldInAllStudies("CCrblToxo"  );
		this.clearFieldInAllStudies("CCryptoMen" );
		this.clearFieldInAllStudies("CGenPrurigo");
		this.clearFieldInAllStudies("CIST"       );
		this.clearFieldInAllStudies("CCervCancer");
		this.clearFieldInAllStudies("COpharCand" );
		this.clearFieldInAllStudies("CKaposiSarc");
		this.clearFieldInAllStudies("CShingles"  );
		this.clearFieldInAllStudies("CDiarrheaC" );
		this.clearFieldInAllStudies("PTBPul"     );
		this.clearFieldInAllStudies("PTBExpul"   );
		this.clearFieldInAllStudies("PCrblToxo"  );
		this.clearFieldInAllStudies("PCryptoMen" );
		this.clearFieldInAllStudies("PGenPrurig" );
		this.clearFieldInAllStudies("PIST"       );
		this.clearFieldInAllStudies("PCervCancer");
		this.clearFieldInAllStudies("POpharCand" );
		this.clearFieldInAllStudies("PKaposiSarc");
		this.clearFieldInAllStudies("PShingles"  );
		this.clearFieldInAllStudies("PDiarrheaC" );
		this.clearFieldInAllStudies("weightLoss" );
		this.clearFieldInAllStudies("diarrhea"   );
		this.clearFieldInAllStudies("fever"      );
		this.clearFieldInAllStudies("cough"      );
		this.clearFieldInAllStudies("pulTB"      );
		this.clearFieldInAllStudies("expulTB"    );
		this.clearFieldInAllStudies("swallPaint" );
		this.clearFieldInAllStudies("cryptoMen"  );
		this.clearFieldInAllStudies("recPneumon" );
		this.clearFieldInAllStudies("sespis"     );
		this.clearFieldInAllStudies("recInfect"  );
		this.clearFieldInAllStudies("curvixC"    );
		this.clearFieldInAllStudies("matHIV"     );
		this.clearFieldInAllStudies("cachexie"   );
		this.clearFieldInAllStudies("thrush"     );
		this.clearFieldInAllStudies("dermPruip"  );
		this.clearFieldInAllStudies("herpes"     );
		this.clearFieldInAllStudies("zona"       );
		this.clearFieldInAllStudies("sarcKapo"   );
		this.clearFieldInAllStudies("xIngPadenp" );
		this.clearFieldInAllStudies("HIVDement"  );
		
		this.clearFieldInAllStudies("arvTreatmentInitDate"  );
		this.clearFieldInAllStudies("arvTreatmentRegime"  );
		this.clearFieldInAllStudies("vlOtherReasonForRequest"  );
		this.clearFieldInAllStudies("initcd4Count"  );
		this.clearFieldInAllStudies("initcd4Percent"  );
		this.clearFieldInAllStudies("initcd4Date"  );
		this.clearFieldInAllStudies("demandcd4Count"  );
		this.clearFieldInAllStudies("demandcd4Percent"  );
		this.clearFieldInAllStudies("demandcd4Date"  );
		this.clearFieldInAllStudies("vlBenefit"  );
		this.clearFieldInAllStudies("priorVLLab"  );
		this.clearFieldInAllStudies("priorVLValue"  );
		this.clearFieldInAllStudies("priorVLDate"  );
		
	}

	/**
	 * callback for loading ObservationHistory information.
	 */
	this.processLoadSuccess = function /*void*/ (xhr) {
	    // console.info("ObservationHistoryLoader.processLoadSuccess:" + xhr.responseText);
		var response = null;
		if ( xhr != null) {			
			var xml = xhr.responseXML;			
			response = xml.getElementsByTagName("formfield").item(0);
			this.existing = response;
			if (response != null) {
				var children = response.childNodes;
				for (var i = 0; i < children.length; i++) {
					var element = children[i];
					var name = element.nodeName;
					var value = element.firstChild.nodeValue;
					this.setFieldInAllStudies(name, value);
		  	    }
			}
		}
		this.afterLoad();
	}

	this.setFields = function /* void */ () {
		alert("unimplemented feature");
	}

	/**
	 * Note All the methods below are in other objects or in the page scope
	 */
	this.afterLoad = function() {
		// tell the form related object to show/hide all the various subquestions, transfer/reformat special values etc.
	    iarv.refresh();
	    farv.refresh();
	    eid.refresh();
	    vl.refresh();
	    rt.refresh();
	    if (rtn != null) {
		    rtn.refresh();
	    }
	    initializeStudySelection();
	}

	this.afterFind = function () {
		compareAllObservationHistoryFields(true);
	}
}


/**
 * Called once a new Patient is loaded
 */
function afterNewPatient() {
	projectChecker.checkAllSubjectFields(true, true);
	hivStatusLoader.find($("patientPK").value);					
}

/**                                                                                                                                                                                  
 * ProjectCheckers know about particular fields on particular forms, not too much reusable here, except within the RetroCI patient and subject forms
 * (1) what to do verify that the form is right (filled in, values point to the right to type of record etc.) for the project and the current requestType.
 * (2) putting the conflicts in the fieldValidator list.
 * Naming conventions
 * this.checkXyz is probably what is called in a onChange for a particular field probably named idPre + "xyz"
 * this.checkAll {Subject, Sample, SampleItem} call multiple of the checkXyz methods when a whole bunch need checking. 
 * SampleItem fields are the sample type, sample item, analysis and/or the test fields.
 */
function BaseProjectChecker() {
	// different study/project forms differ in the ID for the same field only by their prefix which is defined here.
	this.idPre = "";
	
	this.refresh = function() {
		this.refreshBase();
	}
	
	this.refreshBase = function () {
		this.fillLabNo();	    
		this.handlePatientBirthDateChange();
		setSaveButton();
	}
	
	this.handlePatientBirthDateChange = function () {
		handlePatientBirthDateChange( $(this.idPre + "dateOfBirth"), $(this.idPre + "interviewDate"), false, $(this.idPre + "age"), $(this.idPre + 'month'), $(this.idPre + 'ageWeek'), birthDateUsageMessage );
	}
	
	this.checkReceivedDate = function (blanksAllowed) {
	 	makeDirty();
		var field = $(this.idPre + "receivedDateForDisplay");
		if (field == null) return; // just so we don't have to have this field on all forms, but is listed in checkAllSampleFields
		checkValidDate(field);
		checkRequiredField(field, blanksAllowed);
		compareSampleField( field.id, false, blanksAllowed);
	}

	this.checkReceivedTime = function (blanksAllowed) {
	 	makeDirty();
		var field = $(this.idPre + "receivedTimeForDisplay");
		if (field == null) return; // just so we don't have to have this field on all forms, but is listed in checkAllSampleFields
		var isValid = checkValidTimeEntry(field, blanksAllowed);
		compareSampleField( field.id, false, blanksAllowed);		
		updateFieldValidity(isValid, field.id );
	}	

	this.checkInterviewDate = function (blanksAllowed) {
		makeDirty();
		var field = $(this.idPre + "interviewDate");
		checkValidDate(field);
		checkRequiredField(field, blanksAllowed);
		compareSampleField( field.id, false, blanksAllowed, "collectionDateForDisplay");
		this.handlePatientBirthDateChange();
	}

	this.checkInterviewTime = function (blanksAllowed) {
	 	makeDirty();
		var field = $(this.idPre + "interviewTime");
		if (field == null) return; // just so we don't have to have this field on all forms, but is listed in checkAllSampleFields
		var isValid = checkValidTimeEntry(field, blanksAllowed);
		compareSampleField( field.id, false, blanksAllowed, "collectionTimeForDisplay");
		updateFieldValidity(isValid, field.id );
	};	

	this.setSubjectOrSiteSubjectEntered = function () {
		var subjectNumber = $(this.idPre + "subjectNumber");
		subjectNumber = (subjectNumber == null)?"":subjectNumber.value;
		var siteSubjectNumber = $(this.idPre + "siteSubjectNumber");
		siteSubjectNumber = (siteSubjectNumber == null)?"":siteSubjectNumber.value;
		if ( siteSubjectNumber == "" && subjectNumber == "" ) {
			document.getElementById("mainForm").subjectOrSiteSubject.value = "";
		} else {
			document.getElementById("mainForm").subjectOrSiteSubject.value = "1 OR The other IS set";
		}
	};
	
	this.checkSubjectNumber = function (blanksAllowed) {
		makeDirty();
		this.setSubjectOrSiteSubjectEntered();
		var field = $(this.idPre + "subjectNumber");
		this.handleSubjectChange("nationalID", field, false, afterNewPatient);
	}
	
	this.checkSiteSubjectNumber = function (blanksAllowed, isWanted) {
		makeDirty();
		this.setSubjectOrSiteSubjectEntered();
		var snField = $(this.idPre + "subjectNumber");
		var ssnField = $(this.idPre + "siteSubjectNumber");
		if (sampleLoader.existing != null ) {
			// if we already found a sample, we just compare to that 
			var compared = comparePatientField( this.idPre + "siteSubjectNumber", false, blanksAllowed, "externalID");
			// updateFieldValidity(compared, this.idPre + "siteSubjectNumber");
		} else if ( snField.value == "" && ssnField.value != "" ) {
			// if the siteSubjectNumber is filled in try that
			this.handleSubjectChange("externalID", ssnField, isWanted, afterNewPatient);
		} else {
			this.checkAllSubjectFields(blanksAllowed, true);
		}
	}
	
	/**
	 * Only call this if you want to load any matching subject from the server  It does load, if a sample is already loaded, so that the subject stay with the LAB #
	 * @param field
	 * @param isWanted
	 * @return
	 */
	this.handleSubjectChange = function ( findByTag, field, isWanted, func ) {
		patientLoader.findPatientBy(findByTag, field, isWanted, func);
	}
	
	this.checkAllSubjectFields = function (blanksAllowed, validateSubjectNumber) {
		this.checkAllSubjectFieldsBasic(blanksAllowed, validateSubjectNumber);
	}
	this.checkAllSubjectFieldsBasic = function (blanksAllowed, validateSubjectNumbers) {
		if ( validateSubjectNumbers != undefined && validateSubjectNumbers == true ) {
		    // var isWanted = (requestType != 'initial');
			// Mark both the subject and site subject as not savable if the user changes them and doesn't have the priv.
			var existingVal = patientLoader.existingSubjectNumber;
			existingVal = ( existingVal != "" && existingVal != null && existingVal != undefined )?existingVal:null;
			var isGood = compareFieldToExisting(this.idPre + "subjectNumber", true, patientLoader, true, "nationalID", simpleComparator, existingVal)
							|| canEditPatientSubjectNos;
			// this was commented out, but seems needed, if a user enters a known sample and a mismatched patientID, then moves to an unknown. At that point we need to make sure mismatches are cleared, once sample, subject etc. have been cleared appropriately.
			updateFieldValidity(isGood, this.idPre + "subjectNumber");
			isGood = compareFieldToExisting(this.idPre + "siteSubjectNumber", true, patientLoader, true, "externalID")
							|| canEditPatientSubjectNos;
			updateFieldValidity(isGood, this.idPre + "siteSubjectNumber");
		}
		
		this.checkGender(blanksAllowed);
		this.checkDateOfBirth(blanksAllowed);
		this.checkFamilyName(blanksAllowed);
		this.checkFirstNames(blanksAllowed);
		this.checkHivStatus(blanksAllowed);
	}

	this.checkCenterName = function (blanksAllowed) {
		makeDirty();
		var nameField = $(this.idPre + "centerName");
		var codeField = $(this.idPre + "centerCode");
		syncLists(nameField, codeField);
		checkRequiredField(codeField, blanksAllowed);
		compareSampleField(codeField.id, false, blanksAllowed);
	}

	this.checkCenterCode = function (blanksAllowed) {
		makeDirty();
		var nameField = $(this.idPre + "centerName");
		var codeField = $(this.idPre + "centerCode");
		checkRequiredField(codeField, blanksAllowed);
		compareSampleField(codeField.id, false, blanksAllowed);
		syncLists(codeField, nameField);
	}

	this.checkFamilyName = function (blanksAllowed) {
		makeDirty();
		if ( $(this.idPre + "patientFamilyName") != undefined && $(this.idPre + "patientFamilyName").value == "UNKNOWN_" ) {
			updateFieldValidity(false, this.idPre + "patientFamilyName" );
		} else {
			comparePatientField( this.idPre + "patientFamilyName", false, blanksAllowed, "lastName");
		}
	}
	this.checkFirstNames = function (blanksAllowed) {
		makeDirty();
		comparePatientField( this.idPre + "patientFirstNames", false, blanksAllowed, "firstName");
	}
	
	this.checkGenderForVlPregnancyOrSuckle = function () {
		//Observation[YES_NO] set "No" option selected by default when selected gender = "F"
		if($(this.idPre+"gender").value === 'F'){
			$(this.idPre+"vlPregnancy").value=1251; //1251 is th dictionary ID for "No" response 
			$(this.idPre+"vlSuckle").value=1251;
		}
	}

	this.checkGender = function (blanksAllowed) {
		makeDirty();
		checkRequiredField($(this.idPre + "gender"), blanksAllowed);
		comparePatientField( this.idPre + "gender", false, blanksAllowed);
	}
	
	/**
	 * general patient field checking of non-required field.
	 */
	this.checkPatientField = function (fieldBaseId, blanksAllowed, tagName) {
		makeDirty();
		comparePatientField( this.idPre + fieldBaseId, false, blanksAllowed, tagName);
	}

	this.checkDateOfBirth = function(blanksAllowed) {
		makeDirty();
		dobField = $(this.idPre + "dateOfBirth");
		this.handlePatientBirthDateChange();
		var compared = comparePatientField(dobField.id, true, blanksAllowed, "dob");
		// if the DOB doesn't match then you can't use the entered date.
		// updateFieldValidity(compared, this.idPre + "dateOfBirth");
		checkValidDate(dobField);
		checkRequiredField(dobField, blanksAllowed);
	}
	
	this.checkAge = function(field, blanksAllowed, ageFieldType) {
		makeDirty();
		handlePatientAgeChange(field, this.idPre + "dateOfBirth", this.idPre + "interviewDate", ageFieldType );
		this.checkDateOfBirth(blanksAllowed);
	}

	this.checkAllSampleFields = function (blanksAllowed) {
		this.checkCenterName(blanksAllowed);
		this.checkCenterCode(blanksAllowed);
		this.checkInterviewDate(blanksAllowed);
		this.checkReceivedDate(blanksAllowed);
		this.checkInterviewTime(true);
        this.checkReceivedTime(true);
	}

	this.checkAllFields = function(blanksAllowed) {
		this.checkAllSubjectFields(blanksAllowed);
		this.checkAllSampleFields(blanksAllowed);
		compareAllObservationHistoryFields(blanksAllowed, this.idPre);
		compareTimeFields(blanksAllowed, this.idPre);
		this.checkAllSampleItemFields();
	}

	this.checkAllSampleItemFields = function () {
		// nothing to do during patient entry, but sample entry includes various fields to check
	}

	this.checkSampleItem = function (itemField, testField) {
		// sometimes the same projectChecker is used in sample and patient entry, thus, if we called this with nothing, forget it.
		// OR if we are doing initial entry, we'll have a sample, but no sample entries, so don't bother
		if (itemField == null || requestType == 'initial') {
			return;
		}
		var samplePK = $("samplePK").value;
		if ( samplePK == null || samplePK == "") {
			// Clear itemField of conflict message
			return;
		}
		var itemId = itemField.id;
		// each itemField ID has the form like rtn.dryTubeTaken - what we want is the "dryTube" part. It become "Dry Tube" on the server.
		var itemTag = itemId.substring(itemId.indexOf(".")+1);
		itemTag = itemTag.substring(0, itemTag.indexOf("Taken"));

		var testTag = null;
		if ( testField !=  null) {
			var testId = testField.id;
			testTag = testId.substring(testId.indexOf(".")+1);
		}

		// ask the server, if value in this field is actually set in the data.  Report back to the appropriate field
		sampleItemTestLoader.find(samplePK, (testField != null)?testField:itemField, itemTag, $('projectFormName').value, testTag);
	}

	/**
	 * Clear the existing known patient, if we should.
	 */
	this.clearExistingPatient = function () {
		var subjectNumber = $(this.idPre + "subjectNumber");
		subjectNumber = (subjectNumber == null)?"":subjectNumber.value;
		var siteSubjectNumber = $(this.idPre + "siteSubjectNumber");
		siteSubjectNumber = (siteSubjectNumber == null)?"":siteSubjectNumber.value;
		// if no explicit Sub# nor Site Sub # => forget any existing patient info
		if (subjectNumber == "" && siteSubjectNumber == "") {
			patientLoader.clearExisting(true);
			hivStatusLoader.existing = null;
			return;
		}
		var existingSn = patientLoader.getExistingValue("nationalID");
		var existingSsn = patientLoader.getExistingValue("externalID");
		// a. if there is some subjectNumber and it matches the data, we can keep the current patient data.
		// b. if there is NO subjectNumber, but the siteSubjectNumber matches, we can keep the patient data.
		if (subjectNumber == "" ) {
			if ( siteSubjectNumber != existingSsn) {
				// case b.
				patientLoader.clearExisting(true);
				hivStatusLoader.existing = null;
			} 
		} else if (subjectNumber != existingSn) {
			// case a.
			patientLoader.clearExisting(subjectNumber != "");
			hivStatusLoader.existing = null;
		}
	}
	
	this.checkHivStatus = function (isBlankAllowed) {
		var hivStatusFieldId = this.idPre + "hivStatus";
		if ( $(hivStatusFieldId) != null) {		
			compareFieldToExisting(hivStatusFieldId, false, hivStatusLoader, isBlankAllowed);
		}
	}
	
	/**
	 * tweak the right fields to setup the form when editing an entry (readWrite)
	 */
	this.setFieldsForEdit = function(canEditPatientIDs, canEditSampleIDs) {
		this.setFieldReadOnly("labNoForDisplay", !canEditSampleIDs);
		this.setFieldReadOnly("subjectNumber", !canEditPatientIDs);
		this.setFieldReadOnly("siteSubjectNumber", !canEditPatientIDs);
	}
	
	this.setFieldReadOnly = function(fieldId, readOnly) {
		setFieldReadOnly($(this.idPre + fieldId), readOnly );
	}
	
	this.fillLabNo = function () {
		$(this.idPre + "labNoForDisplay").value = $(this.idPre + "labNo").value.substring(4);
	}
	
}

/**
 * Disable the field and add to the class "readOnly" OR NOT disable (enable) the field and remove the class "readOnly", depending on the value of 2nd arg.
 */
function setFieldReadOnly(field, readOnly ) {
    if( field) {
        field.disabled = (readOnly) ? true : false;
        field.className = (readOnly) ? field.className + " readOnly " : field.className.replace(" readOnly ", "");
    }
}

function compareTimeFields(isBlankAllowed, fieldPrefix) {
	if (requestType == 'initial') {	// checking the page-level variable to see if we care to check at all we may have an existing sample and subject created by sample entry, but that doesn't mean we care about any (non-existant) observation histories.
		return;
	}
	var idPre = projectChecker.idPre;
	compareFieldToExisting(idPre + "receivedTimeForDisplay", false, sampleLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "interviewTime", false, sampleLoader, isBlankAllowed, "collectionTimeForDisplay");
}

function compareAllObservationHistoryFields(isBlankAllowed, fieldPrefix) {
	makeDirty();
	if (requestType == 'initial') {	// checking the page-level variable to see if we care to check at all we may have an existing sample and subject created by sample entry, but that doesn't mean we care about any (non-existant) observation histories.
		return;
	}
	var idPre = projectChecker.idPre;
	compareFieldToExisting(idPre + "educationLevel", false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "maritalStatus", false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "nationality", false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "nationalityOther", false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "legalResidence", false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "nameOfDoctor", false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "anyPriorDiseases", false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "arvProphylaxisBenefit", false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "arvProphylaxis", false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "currentARVTreatment", false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "priorARVTreatment", false, observationHistoryLoader, isBlankAllowed);
	for(var i = 0; i <= 11; i++) {
		compareFieldToExisting(idPre + "priorARVTreatmentINNs"+i, false, observationHistoryLoader, isBlankAllowed);
	}
	compareFieldToExisting(idPre + "cotrimoxazoleTreatment", false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "aidsStage", false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "anyCurrentDiseases", false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "currentOITreatment", false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "patientWeight", false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "karnofskyScore", false, observationHistoryLoader, isBlankAllowed);

	compareFieldToExisting(idPre + "cd4Count", false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "hivStatus", false, observationHistoryLoader, isBlankAllowed);
    compareFieldToExisting(idPre + "cd4Percent", false, observationHistoryLoader, isBlankAllowed);
    compareFieldToExisting(idPre + "priorCd4Date", false, observationHistoryLoader, isBlankAllowed);
    compareFieldToExisting(idPre + "antiTbTreatment", false, observationHistoryLoader, isBlankAllowed);
    compareFieldToExisting(idPre + "interruptedARVTreatment", false, observationHistoryLoader, isBlankAllowed);
    compareFieldToExisting(idPre + "arvTreatmentAnyAdverseEffects", false, observationHistoryLoader, isBlankAllowed);
    for(var i = 0; i < 4; i ++) {
		compareFieldToExisting(idPre + "arvTreatmentAdvEffType"+i, false, observationHistoryLoader, isBlankAllowed, "arvTreatmentAdvEffType"+i);    	
		compareFieldToExisting(idPre + "arvTreatmentAdvEffGrd"+i, false, observationHistoryLoader, isBlankAllowed, "arvTreatmentAdvEffGrd"+i);    	
    }
    for(var i = 0; i < 4; i ++) {
		compareFieldToExisting(idPre + "currentARVTreatmentINNs"+i, false, observationHistoryLoader, isBlankAllowed, "currentARVTreatmentINNs"+i);    	
    }
    compareFieldToExisting(idPre + "arvTreatmentChange", false, observationHistoryLoader, isBlankAllowed);
    compareFieldToExisting(idPre + "arvTreatmentNew", false, observationHistoryLoader, isBlankAllowed);
    compareFieldToExisting(idPre + "arvTreatmentRegime", false, observationHistoryLoader, isBlankAllowed);
    compareFieldToExisting(idPre + "cotrimoxazoleTreatAnyAdvEff", false, observationHistoryLoader, isBlankAllowed);
    compareFieldToExisting(idPre + "anySecondaryTreatment", false, observationHistoryLoader, isBlankAllowed);
    compareFieldToExisting(idPre + "secondaryTreatment", false, observationHistoryLoader, isBlankAllowed);
    compareFieldToExisting(idPre + "clinicVisits", false, observationHistoryLoader, isBlankAllowed);

    compareFieldToExisting(idPre + "hospital", false, observationHistoryLoader, isBlankAllowed);
    compareFieldToExisting(idPre + "service", false, observationHistoryLoader, isBlankAllowed);
    compareFieldToExisting(idPre + "hospitalPatient", false, observationHistoryLoader, isBlankAllowed);
    compareFieldToExisting(idPre + "reason", false, observationHistoryLoader, isBlankAllowed);

    compareFieldToExisting(idPre + "whichPCR", false, observationHistoryLoader, isBlankAllowed);
    compareFieldToExisting(idPre + "reasonForSecondPCRTest", false, observationHistoryLoader, isBlankAllowed);
    
    compareFieldToExisting(idPre + "nameOfRequestor" , false, observationHistoryLoader, isBlankAllowed);
    compareFieldToExisting(idPre + "nameOfSampler" , false, observationHistoryLoader, isBlankAllowed);
    compareFieldToExisting(idPre + "eidInfantPTME" , false, observationHistoryLoader, isBlankAllowed);
    compareFieldToExisting(idPre + "eidTypeOfClinic"        , false, observationHistoryLoader, isBlankAllowed);
    compareFieldToExisting(idPre + "eidTypeOfClinicOther"   , false, observationHistoryLoader, isBlankAllowed);
    compareFieldToExisting(idPre + "eidHowChildFed"         , false, observationHistoryLoader, isBlankAllowed);
    compareFieldToExisting(idPre + "eidStoppedBreastfeeding", false, observationHistoryLoader, isBlankAllowed);        
    compareFieldToExisting(idPre + "eidInfantSymptomatic"   , false, observationHistoryLoader, isBlankAllowed);
    compareFieldToExisting(idPre + "eidMothersHIVStatus"    , false, observationHistoryLoader, isBlankAllowed);
    compareFieldToExisting(idPre + "eidMothersARV"          , false, observationHistoryLoader, isBlankAllowed);
    compareFieldToExisting(idPre + "eidInfantsARV"          , false, observationHistoryLoader, isBlankAllowed);
    compareFieldToExisting(idPre + "eidInfantCotrimoxazole" , false, observationHistoryLoader, isBlankAllowed);
    
    compareFieldToExisting(idPre + "indFirstTestName", false, observationHistoryLoader, isBlankAllowed);
    compareFieldToExisting(idPre + "indSecondTestName", false, observationHistoryLoader, isBlankAllowed);
    compareFieldToExisting(idPre + "indFirstTestDate", false, observationHistoryLoader, isBlankAllowed);
    compareFieldToExisting(idPre + "indSecondTestDate", false, observationHistoryLoader, isBlankAllowed);
    compareFieldToExisting(idPre + "indFirstTestResult", false, observationHistoryLoader, isBlankAllowed);
    compareFieldToExisting(idPre + "indSecondTestResult", false, observationHistoryLoader, isBlankAllowed);
    compareFieldToExisting(idPre + "indSiteFinalResult", false, observationHistoryLoader, isBlankAllowed);
    compareFieldToExisting(idPre + "reasonForRequest", false, observationHistoryLoader, isBlankAllowed, "reason");
    compareFieldToExisting(idPre + "underInvestigation", false, observationHistoryLoader, isBlankAllowed);
    
    compareFieldToExisting(idPre + "hivStatus", false, hivStatusLoader, isBlankAllowed);
    // we do not compare previous underInvestigationComment to the current value.
    
    compareFieldToExisting(idPre + "CTBPul"     , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "CTBExpul"   , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "CCrblToxo"  , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "CCryptoMen" , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "CGenPrurigo", false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "CIST"       , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "CCervCancer", false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "COpharCand" , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "CKaposiSarc", false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "CShingles"  , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "CDiarrheaC" , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "PTBPul"     , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "PTBExpul"   , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "PCrblToxo"  , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "PCryptoMen" , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "PGenPrurigo", false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "PIST"       , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "PCervCancer", false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "POpharCand" , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "PKaposiSarc", false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "PShingles"  , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "PDiarrheaC" , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "weightLoss" , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "diarrhea"   , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "fever"      , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "cough"      , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "pulTB"      , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "expulTB"    , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "swallPaint" , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "cryptoMen"  , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "recPneumon" , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "sespis"     , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "recInfect"  , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "curvixC"    , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "matHIV"     , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "cachexie"   , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "thrush"     , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "dermPruip"  , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "herpes"     , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "zona"       , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "sarcKapo"   , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "xIngPadenp" , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "HIVDement"  , false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "priorDiseases",   false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "currentDiseases", false, observationHistoryLoader, isBlankAllowed);
	
	
	compareFieldToExisting(idPre + "arvTreatmentInitDate", false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "arvTreatmentRegime", false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "vlReasonForRequest", false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "vlOtherReasonForRequest", false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "initcd4Count", false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "initcd4Percent", false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "initcd4Date", false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "demandcd4Count", false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "demandcd4Percent", false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "demandcd4Date", false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "vlBenefit", false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "priorVLLab", false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "priorVLValue", false, observationHistoryLoader, isBlankAllowed);
	compareFieldToExisting(idPre + "priorVLDate", false, observationHistoryLoader, isBlankAllowed);
		
}

function SampleItemTestLoader() {
	this.existing = null;
	this.url = "provider=SampleItemTestProvider";

	// field is the field onto which to put any comparison error
    this.find = function  /*void*/ (samplePK, field, itemTag, projectFormName, testTag) {
		this.doAjax(this.url + "&sampleKey=" + samplePK + "&projectFormName=" + projectFormName
				+ "&sampleItemTypeTag=" + itemTag + "&testTag=" + blankOrValue(testTag),
			function /*void*/ (xhr){
		    	sampleItemTestLoader.processFindSuccess(xhr, field);
			},
			function (xhr) {
				sampleItemTestLoader.processFindFailure(xhr, field);
			}
		);
	};

	this.processFindFailure = function(xhr, field) {
		updateFieldConflict(false, field.id, testInvalid );
	};

	this.processFindSuccess = function(xhr, field) {
		var xml = xhr.responseXML;
		var message = this.getResponseProperty(xml, "message");
		var result = xml.getElementsByTagName("formfield").item(0).firstChild.wholeText;
		updateFieldConflict((result == "true") == field.checked, field.id, previousNotMatchedMessage );
	}
}

function HivStatusLoader() {
	this.existing = null;
	this.url = "provider=HivStatusProvider";

	// field is the field onto which to put any comparison error
    this.find = function  /*void*/ (patientId) {
    	if ( projectChecker != farv ) {
    		return;
    	}
    	if ( patientId == "" ) {
    		farv.checkHivStatus(true);
    		return;
    	}
		this.doAjax(this.url + "&patientId=" + patientId,
			function /*void*/ (xhr){
		    	hivStatusLoader.processFindSuccess(xhr);
			},
			function (xhr) {
				hivStatusLoader.processFailure(xhr);
			}
		);
	};
	
	/**
	 * Only used during read only viewing to show the latest known value.
	 */
	this.load = function (patientId, fieldId) {
    	if ( projectChecker != farv ) {
    		return;
    	}
		this.doAjax(this.url + "&patientId=" + patientId,
			function /*void*/ (xhr){
		    	hivStatusLoader.processLoadSuccess(xhr, fieldId);
			},
			function (xhr) {
				hivStatusLoader.processFailure(xhr, fieldId);
			}
		);
	};
	
	this.processLoadSuccess = function (xhr, fieldId) {
		fieldId = "farv.hivStatus";	// there really is only one right field.
		var xml = xhr.responseXML;
		var message = this.getResponseProperty(xml, "message");
		if (message == 'valid') {
			var response = xhr.responseXML.getElementsByTagName("formfield").item(0);
			var hivStatus = this.getResponseProperty(response, "hivStatus");
			this.setField(fieldId, blankOrValue(hivStatus));
		} else {
			// ??
		}
	}

	this.processFailure = function(xhr, fieldId) {
		updateFieldConflict(false, fieldId );
	}

	this.processFindSuccess = function(xhr, fieldId) {
		fieldId = "farv.hivStatus";	// there really is only one right field.
		var xml = xhr.responseXML;
		//alert(xhr.responseText);
		var message = this.getResponseProperty(xml, "message");
		if (message == 'valid') {
			this.existing = xml.getElementsByTagName("formfield").item(0);
		    farv.checkHivStatus(true);
		} else {
			this.existing = undefined;
		    farv.checkHivStatus(true);
		}
	}
	
	this.setFields = function (value) {
		this.setField( fieldValidator.idPre + "hivStatus", value);		
	}
	
	this.clearFields = function () {
		this.setFields();
	}	
}

// Define all of the loaders as Loaders and create one of each
PatientLoader.prototype = new BaseLoader();
ObservationHistoryLoader.prototype = new BaseLoader();
SampleLoader.prototype = new BaseLoader();
SampleItemTestLoader.prototype = new BaseLoader();
HivStatusLoader.prototype = new BaseLoader();

patientLoader = new PatientLoader();
sampleLoader = new SampleLoader();
observationHistoryLoader = new ObservationHistoryLoader();
sampleItemTestLoader = new SampleItemTestLoader();
hivStatusLoader = new HivStatusLoader();
