function addOnLoadEvent(func) {
    if (window.addEventListener) // W3C standard
    {
      window.addEventListener('load', func, false); // NB **not** 'onload'
    }
    else if (window.attachEvent) // Microsoft
    {
      window.attachEvent('onload', func);
    }
}
/*
 * Processes a successfully accession number validation
 */
function processAccessionSuccess(xhr, field, isWanted)
{
    //alert(xhr.responseText);
    var formField = xhr.responseXML.getElementsByTagName("formfield").item(0);
    var xmlMessage = xhr.responseXML.getElementsByTagName("message").item(0);

    var message = xmlMessage.firstChild.nodeValue;
    var labElementId = formField.firstChild.nodeValue;
    if (message == "SAMPLE_FOUND" || message == "valid" ) {
        sampleLoader.findDetailsByAccessionNumber($(labElementId).value, true, labElementId );
    } else {
        var labNoIsGood = ( message == "SAMPLE_NOT_FOUND" ); // just nonexisting is not a problem, SAMPLE_NOT_FOUND is a success
        updateFieldValidity(labNoIsGood, labElementId );
        updateFieldConflict(labNoIsGood, labElementId, message);
        sampleLoader.existing = null;
        projectChecker.clearExistingPatient();
        observationHistoryLoader.existing = null;
        // compareFieldToExisting("subjectNumber", true, patientLoader, false);
        projectChecker.checkAllSubjectFields(true, true);
        projectChecker.checkAllSampleFields(true, true);
        compareAllObservationHistoryFields(true);
    }
}
   
function comparePatientField( fieldId, crossCheckField, isBlankAllowed, tagName ) {
    return compareFieldToExisting( fieldId, crossCheckField, patientLoader, isBlankAllowed, tagName);
}

function compareSampleField( fieldId, crossCheckField, isBlankAllowed, tagName) {
    return compareFieldToExisting( fieldId, crossCheckField, sampleLoader, isBlankAllowed, tagName);
}

simpleComparator =  new function SimpleComparator () {
    this.compare = function (existing, newVal) {
        // EITHER there is something to compare and they match.
        if (existing == newVal) {
            return true;
        }
        // OR there is nothing to compare and the newVal is blank (or undefined)
        if ( existing == undefined ) {
            if (newVal == "" || newVal == undefined) {
                return true;
            } 
            // Late change before shipping to CI 09/2010: during initial entry we may have a mostly blank patient (and history values) from sample entry, but that's nothing to complain about. 
            if ( requestType == 'initial') {
                return true;
            }
        }
        return false;
    }
}

/***
 *
 * @return  true => it matches, false => it doesn't match
 */
function /* boolean */ compareFieldToExisting(fieldId, crossCheckField, loader, isBlankAllowed, tagName, comparator, existingVal) {
    if (comparator == undefined) {
        comparator = simpleComparator;
    }
    var field = $(fieldId);
    if (field == null) {
        return true;    // if there is no field, it matches just fine.
    }
    // nothing loaded which to compare and no override value which to compare, all is well
    if ( loader.existing == null && existingVal == null ) {
        updateFieldValidity(true, fieldId );
        return updateFieldConflict(true, fieldId, "" );
    }
    var isBlank = (field.value == undefined || field.value == "");
    isBlankAllowed = (isBlankAllowed == undefined)? true : isBlankAllowed;
    if (isBlankAllowed && isBlank ) {
        updateFieldValidity(true, fieldId );
        return updateFieldConflict(true, fieldId );
    }
    if (existingVal == null ) { // is there anything provide or some place lookup a value? 
        if (tagName == null) {
            tagName = fieldId;
        }
        tagName = tagName.substring(tagName.indexOf('.') + 1);
        existingVal = loader.getExistingValue(tagName);
    }
    // if we have a value already show any difference, if we have a blank don't bother on patient fields
    var initial = requestType == "initial";
    //var notPatientLoader = loader != patientLoader;
    // on initial, compare only non-blank, everywhere else compare anything 
    if ( (initial && existingVal != undefined) || !initial ) {    
        var newVal = field.value;
        var display = findCurrentDisplayValue(field, existingVal, comparator);
        // Unfortunate special case: Due to DB value being 00:00 when time left blank
        // here is the correction       
        if ((field.id.indexOf("interviewTime") !== -1 || field.id.indexOf("receivedTime") !== -1) && 
                existingVal == "00:00") {
            if (isBlank)
                return true;
            else            
                return updateFieldConflict(comparator.compare(existingVal, newVal), fieldId, previousNotMatchedMessage + " " + blankTextField );
        }           
        else        
            return updateFieldConflict(comparator.compare(existingVal, newVal), fieldId, previousNotMatchedMessage + " " + display );
    } else { // undefined is a match
        return updateFieldConflict(true, fieldId, "" );
    }
}

/**
 * look through the drop down and find the display text for the given value.
 * @param field select field
 * @param value value to search for
 * @param weakYesMatching T => allow anything but yes to map to NO
 * @return
 */
function findCurrentDisplayValue(field, value, comparator) {
    if (field.type == "text" || field.type == "hidden") {
        newValue = blankOrValue(value);
    } else {
        var i = findInputSelectionIndex(field, value);
        if (comparator.mapDisplayIndex != undefined ) {
            i = comparator.mapDisplayIndex(i);
        }
        newValue = field[i].text;
    }
    return newValue;
}

/**
 *
 * @param good
 * @param fieldId
 * @param message
 * @return the value of good, so some fields, ie. subjectNumber field, can mark the field invalid.
 */
function /* boolean */ updateFieldConflict(good, fieldId, message) {
    var spanNodeId = fieldId+"Text";
    var spanNode = document.getElementById(spanNodeId);
    if (spanNode != null) { // drop any old error text
        spanNode.parentNode.removeChild(spanNode);
    }
    var iconDiv = $(fieldId).nextSibling.nextSibling;
    if( !iconDiv){
        iconDiv = $(fieldId).nextSibling;
    }
    fieldValidator.setFieldConflict(good, fieldId);
    if (!good) {
        spanNode = document.createElement("span");
        spanNode.setAttribute("id", spanNodeId);
        spanNode.appendChild(document.createTextNode(message));
        iconDiv.parentNode.insertBefore(spanNode, iconDiv.nextSibling);
    }
    return good;
}

/*
 * Processes a failed attempt of accession number validation
 *
 */
function processAccessionFailure(xhr)
{
    alert("processAccessionFailure");
}

function handleLabNoChange( displayField, prefix, isWanted ) {
    
    var displayId = displayField.id;
    var labNoFieldId = displayId.substr(0, displayId.length - 10);  // all such fields end with "ForDisplay", 10 chars.
    var idField = document.getElementById(labNoFieldId);

    idField.value = prefix + displayField.value;
    if (requestType == 'readwrite') {
        return;
    } 

    var recordType = "initial" + pageType;
    if (type == ("verify")) {
        recordType = "double" + pageType;
        isWanted = true;
    }
    validateAccessionNumberOnServer( idField, recordType, isWanted );
}

function validateAccessionNumberOnServer( field, recordType, isWanted )
{
    var projectFormName = "";
    var pfnFld = document.getElementById("projectFormName");
    // when verifying (second entry) the sample had to have been entered on the same type of form.
    if (pfnFld != null ) {
        projectFormName = pfnFld.value;
    }
    new Ajax.Request (
                      'ajaxXML',  //url
                      {//options
                      method: 'get', //http method
                      parameters: 'provider=SampleEntryAccessionNumberValidationProvider&field=' + field.id + '&accessionNumber=' + field.value + '&recordType=' + recordType + '&isRequired=' + isWanted +"&projectFormName=" + projectFormName,
                      indicator: 'throbbing',
                	  requestHeaders : {
              			"X-CSRF-Token" : getCsrfToken()
                	  },
                      onSuccess:  function (xhr) {
                          processAccessionSuccess(xhr, field, isWanted);
                      },
                      onFailure:  processAccessionFailure
                      }
           );
}

/*
 *
 * Disables or Enables divs
 */
function toggleDisabledDiv(element, booleanValue) {
    if (element == null) return;
    try {
     element.disabled = booleanValue ? false: true;
    }
    catch(E){}

    if (element != null && element.childNodes != null && element.childNodes.length > 0) {
        for (var x = 0; x < element.childNodes.length; x++) {

            toggleDisabledDiv(element.childNodes[x], booleanValue);
        }
    }
}

/***
 * Handles DBS Subject ID changes - used for both DBS in sample and in patient
 */

function handleDBSSubjectId(blanksAllowed) {
    var dbsSubjectNumber = document.getElementById("eid.subjectNumber");
    var codeSite = document.getElementById("eid.codeSiteID");
    var infant = document.getElementById("eid.infantID");
   /* if ( 3 > codeSite.value.length || 3 < codeSite.value.length ||
         4 > infant.value.length   || 4 < infant.value.length ) {
        dbsSubjectNumber.value = "";
    } else {*/
        dbsSubjectNumber.value = "DBS" + codeSite.value + infant.value;
          //}
    projectChecker.checkSubjectNumber(blanksAllowed);
}

function setInputSelectionIndex(selectObject, value) {
    selectObject.selectedIndex = findInputSelectionIndex(selectObject, value);
}

function findInputSelectionIndex(selectObject, value) {
    for(index = 0; index < selectObject.length; index++) {
        if( selectObject[index].value == value) {
            return index;
        }
    }
    return 0;
}

//TODO PAHill is this in one of the more general utility js files?
// no nulls, either "" or the given value
function /* String */ blankOrValue(property) {
    return (property == undefined) ? "" : property
}

/**
 * Set the index of one list so that both lists have the value, different indexes, different text
 * @param selectList1 list to find the current set value
 * @param list2 list to move to same value
 */
function /*void*/ syncLists(selectList1, selectList2) {
    if ( selectList1 ==  null || selectList2 == null) return;
    if ( selectList2.options == null || selectList1.options == null) return; // check in case all org lists have not been changed to both be drop downs.
    var list2i = findInputSelectionIndex(selectList2, selectList1.options[selectList1.selectedIndex].value);
    selectList2.selectedIndex = list2i;
}


/**sample add by project **/


	function checkVLSampleType(e) {
		$("vl.pscvlTaken").checked = (e.id === "vl.pscvlTaken");
		$("vl.dbsvlTaken").checked = (e.id === "vl.dbsvlTaken");
		$("vl.edtaTubeTaken").checked = (e.id === "vl.edtaTubeTaken");
	}


	function findEOrder() {
		var value = jQuery("#externalOrderNumber").val()
		if (value) {
			var url = new URL(window.location.href);
			var search_params = url.searchParams;
			search_params.set('ID', value);
			url.search = search_params.toString();
			window.location.replace(url.toString());
		}
	}

function getNextAccessionNumber() {
    generateNextScanNumber(processScanSuccess);
}

function processScanSuccess(xhr) {
    var formField = xhr.responseXML.getElementsByTagName("formfield").item(0);
    var returnedData = formField.firstChild.nodeValue;
    var message = xhr.responseXML.getElementsByTagName("message").item(0);
    var success = message.firstChild.nodeValue == "valid";
	if (success) {
      $("hpv.labNoForDisplay").value = returnedData;
      $("hpv.labNo").value = returnedData;
    } else {
        $("hpv.labNoForDisplay").value = "";
        $("hpv.labNo").value = "";
    }
}

function checkAccessionNumber(accessionNumber) {
    if (!fieldIsEmptyById("hpv.labNo")) {
        validateAccessionNumberOnServer(false, false, accessionNumber.id, accessionNumber.value, processAccessionSuccess, null);
    }
    else {
         selectFieldErrorDisplay(false, $("hpv.labNo"));
    }
}

