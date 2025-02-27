<%@page import="org.openelisglobal.common.action.IActionConstants"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.formfields.FormFields,
	        org.openelisglobal.common.formfields.FormFields.Field,
	        org.openelisglobal.common.util.ConfigurationProperties,
	        org.openelisglobal.common.util.IdValuePair,
	        org.openelisglobal.common.util.ConfigurationProperties.Property,
	        org.openelisglobal.common.util.DateUtil,
	        org.openelisglobal.internationalization.MessageUtil,
	        org.openelisglobal.common.util.Versioning"%>
<%@ page isELIgnored="false"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<c:set var="formName" value="${form.formName}" />
<c:set var="entryDate" value="${form.currentDate}" />

<%
boolean useCollectionDate = FormFields.getInstance().useField(Field.CollectionDate);
boolean useInitialSampleCondition = FormFields.getInstance().useField(Field.InitialSampleCondition);
boolean useSampleNature = FormFields.getInstance().useField(Field.SampleNature);
boolean useCollector = FormFields.getInstance().useField(Field.SampleEntrySampleCollector);
boolean autofillCollectionDate = ConfigurationProperties.getInstance()
		.isPropertyValueEqual(Property.AUTOFILL_COLLECTION_DATE, "true");
%>
<link href="select2/css/select2.min.css" rel="stylesheet" />
<script type="text/javascript" src="select2/js/select2.min.js"></script>
<script type="text/javascript" src="scripts/additional_utilities.js"></script>
<script type="text/javascript" src="scripts/jquery.asmselect.js?"></script>
<script type="text/javascript" src="scripts/ajaxCalls.js?"></script>
<script type="text/javascript" src="scripts/laborder.js?"></script>

<link rel="stylesheet" type="text/css" href="css/jquery.asmselect.css?" />




<script type="text/javascript">

var useCollectionDate = <%=useCollectionDate%>;
var autoFillCollectionDate = <%=autofillCollectionDate%>;
var useInitialSampleCondition = <%=useInitialSampleCondition%>;
var useSampleNature = <%=useSampleNature%>;
var useCollector = <%=useCollector%>;
var currentCheckedType = -1;
var currentTypeForTests = -1;
var selectedTypeRowId = -1;
var sampleChangeListeners = [];
var sampleIdStart = 0;
var currentSampleDiv ;
var sampleManualselected = true;


function /*void*/ addSampleChangedListener( listener ){
	sampleChangeListeners.push( listener );
}

function /*void*/ notifyChangeListeners(){
	for(var i = 0; i < sampleChangeListeners.length; i++){
			sampleChangeListeners[i]();
	}
}

function addNewSamples(element){
    currentSampleDiv = element.parentNode.parentNode.parentNode.parentNode.parentNode;
	var samplesAdded = currentSampleDiv.getElementsByClassName("samplesAdded")[0];
	samplesAdded.show();

	var addTable = currentSampleDiv.getElementsByClassName("samplesAddedTable")[0];

	var typeElement = element;
	var typeIndex = typeElement.selectedIndex;
	var sampleDescription = typeElement.options[typeIndex].text;
	var sampleTypeValue = typeElement.options[typeIndex].value;
	var currentTime = getCurrentTime();
	
	addTypeToTable(addTable, sampleDescription, sampleTypeValue, currentTime, '${entryDate}' );
    var newIndex = SampleTypes.length;
    SampleTypes[newIndex] = new SampleType(sampleTypeValue, sampleDescription);
	
	notifyChangeListeners();
	
	testAndSetSave();
}

function testAndSetSave(){
	//N.B. This is bogus and the saving issues need to be sorted out.
	// Story https://www.pivotaltracker.com/story/show/31485441 
	if(window.setSave){
		setSave();
	}else if(window.setSaveButton){
		setSaveButton();
	}
}

function addTypeToTable(table, sampleDescription, sampleType, currentTime, currentDate ) {
	    if(table.rows.length > 1){
            table.rows[1].remove();
	   }
		var rowLength = table.rows.length;
		var parentDiv = table.parentNode.parentNode;
		var rowLabel = parentDiv.id.substring(parentDiv.id.indexOf('_') + 1);
		var newRow = table.insertRow(rowLength);

		var cellCount = 0;
		newRow.id = "_" + rowLabel;


		var selectionBox = newRow.insertCell(cellCount);
		var sampleId = newRow.insertCell(++cellCount);
		var type = newRow.insertCell(++cellCount);

		if( useInitialSampleCondition ){
			// var newMulti = $("prototypeID").parentNode.cloneNode(true);
			// var selection = newMulti.getElementsByTagName("select")[0];
			// selection.id = "initialCondition_" + rowLabel;

			// var initialConditionCell = newRow.insertCell(++cellCount);
			// initialConditionCell.innerHTML = newMulti.innerHTML.replace("initialSampleConditionList", "formBreaker");

			// jQuery("#initialCondition_" + rowLabel).asmSelect({	removeLabel: "x"});
		}
		
		if( useSampleNature ){
			var newSelect = $("sampleNaturePrototypeID").parentNode.cloneNode(true);
			var selection = newSelect.getElementsByTagName("select")[0];
			selection.id = "sampleNature_" + rowLabel;

			var sampleNatureCell = newRow.insertCell(++cellCount);
 			sampleNatureCell.innerHTML = newSelect.innerHTML.replace("sampleNatureList", "formBreaker");
		}

		if( useCollectionDate ){
			var collectionDate = newRow.insertCell(++cellCount);
			var collectionTime = newRow.insertCell(++cellCount);
		}
		if( useCollector ){
			var collector = newRow.insertCell(++cellCount);
		}
		var tests = newRow.insertCell(++cellCount);
		var rejectCell = newRow.insertCell(++cellCount);
		var rejectReasonCell = newRow.insertCell(++cellCount);
		var remove = newRow.insertCell(++cellCount);

		selectionBox.innerHTML = getCheckBoxHtml( rowLabel, true );
		sampleId.innerHTML = getSampleIdHtml(rowLabel);
		type.innerHTML = getSampleTypeHtml( rowLabel, sampleDescription, sampleType );
		if( useCollectionDate ){
			var sampleTypeEntry = getSampleTypeMapEntry(sampleType);
			if (sampleTypeEntry && typeof sampleTypeEntry.collectionDate !== 'undefined') {
				collectionDate.innerHTML = getCollectionDateHtml( rowLabel, sampleTypeEntry.collectionDate);
				collectionTime.innerHTML = getCollectionTimeHtml( rowLabel, sampleTypeEntry.collectionTime);
			} else {
				collectionDate.innerHTML = getCollectionDateHtml( rowLabel, autoFillCollectionDate ? currentDate : "" );
				collectionTime.innerHTML = getCollectionTimeHtml( rowLabel, autoFillCollectionDate ? currentTime : "" );
			}
		}
		if( useCollector ) {
			collector.innerHTML = getCollectorHtml( rowLabel);
		}
		tests.innerHTML = getTestsHtml( rowLabel );

		rejectCell.innerHTML =  getRejectCheckBoxHtml(rowLabel);

		var newRejection = $("rejectPrototypeID").parentNode.cloneNode(true);
		var rejectSelection = newRejection.getElementsByTagName("select")[0];
		rejectSelection.id = "rejectedReasonId_" + rowLabel;
		rejectReasonCell.innerHTML = newRejection.innerHTML.replace("rejectReasonList", "rejectionList");
		remove.innerHTML = getRemoveButtonHtml( rowLabel );

		
		sampleManualselected = false
		sampleClicked( rowLabel );
}


function getCheckBoxHtml( row, selectRow ){
	return "<input type='radio' name='sampleSelect' id='select_" + row + "' onclick='sampleClicked(" + row + " )' " +
			(selectRow ? "CHECKED" : " ") + " >";
}

function getSampleIdHtml(row){
	return "<input name='sequence' size ='4' value='" + (parseInt(row) + parseInt(sampleIdStart) ) + "' id='sequence_" + row + "' class='text sampleId' type='text'  disabled='disabled' >";
}

function getSampleTypeHtml(  row, sampleDescription, sampleType ){
	return   sampleDescription + "<input name='sampleType' id='typeId_" + row + "'value='" + sampleType + "' type='hidden'>";
}

function getCollectionDateHtml( row, date ){ 
	return "<input name='collectionDate' maxlength='10' size ='12' value='" + date + "' onkeyup=\"addDateSlashes(this, event);\" onchange=\"checkValidEntryDate(this, 'past', true);\" id='collectionDate_" + row + "' class='text' type='text'>";
}

function getCollectionTimeHtml( row, time ){
	return "<input name='collectionTime' maxlength='10' size ='12' value='" + time + "' onkeyup='filterTimeKeys(this, event);' onblur='checkValidTime(this, true);' id='collectionTime_" + row + "' class='text' type='text'>";
}

function getTestsHtml(row){
	return "<input id='testIds_" + row + "' type='hidden'>" +
	       "<input id='panelIds_" + row + "' type='hidden'>" +
            "<input id='testSectionMap_" + row + "' type='hidden'>" +
            "<input id='testTypeMap_" + row + "' type='hidden'>" +
            "<textarea name='tests' id='tests_" + row + "' cols='65' class='text' readonly='true'  />";

}

function getCollectorHtml(row){
	return "<input name='collector'  value='' id='collector_" + row + "' class='text' type='text'>";

}
function getRemoveButtonHtml( row ){
	return "<input name='remove' value='" + "<spring:message code="sample.entry.remove.sample"/>" + "' class='textButton' onclick='removeSample(this);testAndSetSave();' id='removeButton_" + row +"' type='button' >";
}

function getRejectCheckBoxHtml(row ){
	return "<input name='reject' class='rejected' id='reject_" + row  + "' type='checkbox' onclick='activateRejectReason(" + row +");'>" ;
}

function activateRejectReason(row){
  if(jQuery("#reject_" + row).prop("checked")){
	if (confirm("<%=MessageUtil.getMessage("sample.entry.reject.warning")%>")){
		jQuery("#rejectedReasonId_" + row).prop("disabled" , !jQuery("#reject_" + row).prop("checked"));
	}else{
		jQuery("#reject_" + row).prop("checked" , !jQuery("#reject_" + row).prop("checked"));
	}
  }else{
     jQuery("#rejectedReasonId_" + row).prop("disabled" , !jQuery("#reject_" + row).prop("checked"));
  }	
}
function getCurrentTime(){
	var date = new Date();

	return (formatToTwoDigits(date.getHours()) + ":"  + formatToTwoDigits(date.getMinutes()));
}

function formatToTwoDigits( number ){
	return number > 9 ? number : "0" + number;
}


function removeRow( row ){
	var checkedRowRemoved = false;
	var table  = currentSampleDiv.getElementsByClassName("samplesAddedTable")[0];
	var rowID = "_" + row;
	var rows = table.rows;


	for( var i = rows.length - 1; i > 0; i--){
		if( rows[i].id == rowID ){
			checkedRowRemoved = $("select" + rowID).checked;
			table.deleteRow( i );
			break;
		}
	}

	if( rows.length == 1 ){
		var samplesAdded = currentSampleDiv.getElementsByClassName("samplesAdded")[0];
	    samplesAdded.hide();
	}else if( checkedRowRemoved){
		$("select" + rows[1].id).checked = true;
		sampleClicked( rows[1].id.sub('_', '') );
	}

    if (typeof(referralTestSelected) === 'function') {
    	referralTestSelected();
	}
    if (typeof(assignTestsToSelected) === 'function') {
    	assignTestsToSelected();
	}
	
	testAndSetSave();
}

function removeSample(element){
	var sampleBlock = element.parentNode.parentNode.parentNode.parentNode.parentNode.parentNode.parentNode
	sampleBlock.remove();
	addNotificationsOptions();
	testAndSetSave();
}

function loadSamples(){
	$("sampleXML").value = convertSamplesToXml();
}

function convertSamplesToXml(){
	var sampleDivs = document.getElementsByClassName("samplesAddedTable");

	var xml = "<?xml version='1.0' encoding='utf-8'?><samples>";

	for(var x = 1; x < sampleDivs.length ; x++ ){
		var rows = sampleDivs[x].rows;
		if(rows.length == 2){
          xml = xml + convertSampleToXml( rows[1].id);
		}
	}

	xml = xml + "</samples>";
	return xml;
}

function convertSampleToXml( id ){

	var xml = "<sample sampleID='" + jQuery("#typeId" + id).val() +
			  "' date='" + (useCollectionDate ? jQuery("#collectionDate" + id).val() : '') +
			  "' time='" + (useCollectionDate ? jQuery("#collectionTime" + id).val() : '') +
			  "' collector='" + (useCollector ? jQuery("#collector" + id).val() : '') +
			  "' tests='" + jQuery("#testIds" + id).val() +
              "' testSectionMap='" + jQuery("#testSectionMap" + id).val() +
              "' testSampleTypeMap=\"" + jQuery("#testTypeMap" + id).val() +
			  "\" panels='" + jQuery("#panelIds" + id).val() + 
			  "' rejected='" + jQuery("#reject" + id).prop("checked") +
			  "' rejectReasonId='" + jQuery("#rejectedReasonId" + id).val() + "'" ;

	if( useInitialSampleCondition ){
		//var initialConditions = $("initialCondition" + id);
		//var optionLength = initialConditions.options.length;
		xml += " initialConditionIds='' ";
		// for( var i = 0; i < optionLength; ++i ){
		// 	if( initialConditions.options[i].selected ){
		// 		xml += initialConditions.options[i].value + ",";
		// 	}
		// }

		// xml =  xml.substring(0,xml.length - 1);
		//xml += "'";
	}
	if( useSampleNature ){
		var sampleNature = $("sampleNature" + id);
		var optionLength = sampleNature.options.length;
		xml += " sampleNatureId=' ";
		for( var i = 0; i < optionLength; ++i ){
			if( sampleNature.options[i].selected ){
				xml += sampleNature.options[i].value + ",";
			}
		}

		xml =  xml.substring(0,xml.length - 1);
		xml += "'";
	}

	xml +=  " />";

	return xml;
}


function sampleTypeSelected( element ){
	var currentTypeIndex = element.selectedIndex;
	if(currentTypeIndex != 0){
		addNewSamples(element);
	}
}

function sampleClicked( id ){
	selectedTypeRowId = id;
	currentCheckedType = $("typeId_" + id).value;

	editSelectedTest();
}


function processGetTestSuccess(xhr){

    //alert(xhr.responseText);
    var response = xhr.responseXML.getElementsByTagName("formfield").item(0);
	var i;
	var testTable = currentSampleDiv.getElementsByClassName("addTestTable")[0];
	var testDropdown = currentSampleDiv.getElementsByClassName("testDropdown")[0];
	var panelTable = currentSampleDiv.getElementsByClassName("addPanelTable")[0];
    var tests = response.getElementsByTagName("test");
    var isVariableSampleType = response.getElementsByTagName("variableSampleType").length > 0;
    clearTable( testTable );
    clearTable( panelTable );
	clearDropdown(testDropdown);
    
    if( tests.length == 0){
        alert("<%=MessageUtil.getMessage("sample.entry.noTests")%>" );
		removeRow( selectedTypeRowId );
    }else{
       if( isVariableSampleType){
           jQuery("#userSampleTypeHead").show();
       }else{
           jQuery("#userSampleTypeHead").hide();
       }
	   for( i = 0; i < tests.length; i++ ){
	   		insertTestIntoTestTable( tests[i], testTable, testDropdown, isVariableSampleType );
	   }
	   if (!testDropdown.classList.contains("select2Applied")) {
		   //alphabetize sort
// 		   var opt = jQuery(testDropdown).find('option').sort(function (a,b) { return a.text.toUpperCase().localeCompare(b.text.toUpperCase()) });
// 		   jQuery(testDropdown).append(opt);

		   var select2Dropdown = jQuery(testDropdown).select2({
			    placeholder: "<spring:message code="label.button.search"/>",
			    allowClear: true
			});
		   select2Dropdown.on("change", function () {
			    var items= jQuery(this).val();       
			    jQuery(testTable.getElementsByClassName("testCheckbox")).each(function() {
				    jQuery(this).attr('checked', false);
				    jQuery(this).trigger("onclick");
			    });
			    if (items != null) {
			    	for (i = 0; i < items.length; i++) {
						var checkbox = jQuery(testTable.getElementsByClassName("test_" + items[i])[0]);
						checkbox.attr('checked', true);
						checkbox.trigger("onclick");
					}
			    }
				
			  
			});
		   testDropdown.classList.add("select2Applied");
	   }
	   
	   var panels = response.getElementsByTagName("panel");
	   for( i = 0; i < panels.length; i++ ){
	   		insertPanelIntoPanelTable( panels[i], panelTable );
	   }
	
		currentSampleDiv.getElementsByClassName("testSelections")[0].show();
	
		setSampleTests(isVariableSampleType);
	}
	sampleManualselected = true
}

function insertTestIntoTestTable( test, testTable, testDropdown, userSampleTypes ){
	var name = getValueFromXmlElement( test, "name" );
	var id = getValueFromXmlElement( test, "id" );
	var userBench = "true" == getValueFromXmlElement( test, "userBenchChoice" );
	var row = testTable.rows.length;
	var nominalRow = row - 1;
	var newRow = testTable.insertRow(row);
	var selectionCell = newRow.insertCell(0);
	var nameCell = newRow.insertCell(1);
    var qualifiableId = "";
    var userSampleTypesList, userVariableSampleTypes,selectionClone;
	
	//TODO get select
	//add select option
	var opt = document.createElement('option');
    opt.text = name;
    opt.value = nominalRow;
    testDropdown.appendChild(opt);

	newRow.id = "availRow_" + nominalRow;

	selectionCell.innerHTML = getTestCheckBoxesHtml( nominalRow, userBench, userSampleTypes);
	nameCell.innerHTML = getTestDisplayRowHtml( name, id, nominalRow );

	if( userBench ){
		$("sectionHead").show();
		selectionCell = newRow.insertCell(2);
		selectionCell.id = "testSection_" + nominalRow;
		selectionClone = jQuery("#sectionPrototype").clone().show();
		selectionClone.children("#testSectionPrototypeID").attr("id", "sectionSelect_" + nominalRow);
		selectionCell.innerHTML = selectionClone.html();
	}

    if( userSampleTypes ){
        userVariableSampleTypes = test.getElementsByTagName("variableSampleTypes");
        userSampleTypesList  = test.getElementsByTagName("type");
        if( userVariableSampleTypes[0].attributes.getNamedItem("qualifiableId")){
            qualifiableId = userVariableSampleTypes[0].attributes.getNamedItem("qualifiableId").nodeValue;
        }
        $("userSampleTypeHead").show();
        selectionCell = newRow.insertCell(2);
        selectionCell.id = "userSampleType_" + nominalRow;
        selectionClone = jQuery("#userSampleTypePrototype").clone().show();
        jQuery.each(userSampleTypesList, function (index, value) {
            selectionClone.children("#userSampleTypePrototypeID").append(jQuery('<option>', {
                value: value.attributes.getNamedItem("id").nodeValue,
                text : value.attributes.getNamedItem("name").nodeValue
            }));
        }  );

        selectionClone.children("#userSampleTypePrototypeID").attr("id", "userSampleTypeSelect_" + nominalRow);
        selectionCell.innerHTML = selectionClone.html();
        jQuery("#userSampleTypeSelect_" + nominalRow).change(function(){
            userSampleTypeSelectionChanged( "userSampleTypeSelect_" + nominalRow, nominalRow, qualifiableId  );
        });

        selectionCell = newRow.insertCell(3);
        selectionCell.id = "userSampleTypeQualifier_" + nominalRow;
        selectionClone = jQuery("#userSampleTypeQualifierPrototype").clone();
        selectionClone.children("#userSampleTypeQualifierPrototypeID").removeAttr("disabled");
        selectionClone.children("#userSampleTypeQualifierPrototypeID").attr("id", "userSampleTypeQualifierID_" + nominalRow);
        selectionClone.children("#userSampleTypeQualifierPrototypeValueID").val(qualifiableId);
        selectionClone.children("#userSampleTypeQualifierPrototypeValueID").attr("id", "userSampleTypeQualifierValueID_" + nominalRow);
        selectionCell.innerHTML = selectionClone.html();
        selectionCell.hide();
    }
}

function insertPanelIntoPanelTable( panel, panelTable ){

	var name = getValueFromXmlElement( panel, "name" );
	var id = getValueFromXmlElement( panel, "id");
	var testMap = getValueFromXmlElement( panel, "testMap" );

	//This sillyness is because a single value will be interpreted as an array size rather than a member
	if( testMap.indexOf( "," ) == -1 ){
		testMap += ", " + testMap;
	}

	var row = panelTable.rows.length;
	var nominalRow = row - 1;
	var newRow = panelTable.insertRow(row);

	var selectionCell = newRow.insertCell(0);
	var nameCell = newRow.insertCell(1);

	selectionCell.innerHTML = getPanelCheckBoxesHtml(testMap, nominalRow, id );
	nameCell.innerHTML = name;
}

function getTestCheckBoxesHtml( row, userBench, userSampleTypes ){
	var benchUpdate = userBench ? "setUserSectionSelection(this, \'" + row + "\');" : " ";
    var sampleTypeUpdate =  userSampleTypes ? "setUserSampleTypeSelection(this, \'" + row + "\' );" : " ";
	return "<input name='testSelect'  class='testCheckbox test_" + row + "' id='test_" + row + "' type='checkbox' onclick=\"" +  benchUpdate + sampleTypeUpdate + " assignTestsToSelected();" + "\" >";
}

function getPanelCheckBoxesHtml(map, row, id ){
    panelTestsMap[id] = map;
	return "<input name='panelSelect' value='" + id + "' id='panel_" + row + "' onclick='panelSelected(this, new Array(" + map + "));assignTestsToSelected( this, \"" + id + "\")' type='checkbox'>";
}

function getTestDisplayRowHtml( name, id, row ){
	return "<input name='testName' class='testName_" + row  + "'  value='" + id + "' id='testName_" + row  + "' type='hidden' >" + name;
}

function getValueFromXmlElement( parent, tag ){
	var element = parent.getElementsByTagName( tag );

	return (element && element.length > 0 )  ? element[0].childNodes[0].nodeValue : "";
}

function clearTable(table){
    $("sectionHead").hide();
	var rows = table.rows.length - 1;
	while( rows > 0 ){
		table.deleteRow( rows-- );
	}
}

function clearDropdown(dropdown) {
	let options = dropdown.getElementsByTagName('option');

	for (var i=options.length; i--;) {
		dropdown.removeChild(options[i]);
	}
}

function deselectAllTests(){
	deselecAllTestsForList( "addTestTable" );
	deselecAllTestsForList( "addPanelTable" );
}

function deselecAllTestsForList( table ){
	/**
	var testTable = $(table);
	var inputs = testTable.getElementsByTagName( "input" );

	for( var i = 0; i < inputs.length; i = i + 2 ){
		if (i > 0)
		 inputs[i].checked = false;
	}
	*/
}

function setUserSectionSelection(testCheckbox, row){
	if( testCheckbox.checked){
		jQuery("#testSection_" + row + " select").removeAttr("disabled");
		jQuery("#testSection_" + row + " span").css("visibility", "visible");
	}else{
		jQuery("#testSection_" + row + " select").attr("disabled", "disabled");
		jQuery("#testSection_" + row + " span").css("visibility", "hidden");
	}	
}

function setUserSampleTypeSelection(testCheckbox, row){
    var sampleTypeSelection = jQuery("#userSampleType_" + row + " select");

    if( testCheckbox.checked){
        sampleTypeSelection.removeAttr("disabled");
        sampleTypeSelection.addClass('required');
        jQuery("#userSampleType_" + row + " span").css("visibility", "visible");
    }else{
        sampleTypeSelection.val(0);
        sampleTypeSelection.attr("disabled", "disabled");
        sampleTypeSelection.removeClass('required');
        jQuery("#userSampleType_" + row + " span").css("visibility", "hidden");
        jQuery("#userSampleTypeQualifierID_" + row).val('');
        jQuery("#userSampleTypeQualifierID_" + row).removeClass('required');
        jQuery("#userSampleTypeQualifier_" + row).hide();

    }
}

function assignTestsToSelected(checkbox, panelId){
	var testTable = currentSampleDiv.getElementsByClassName("addTestTable")[0];
	var chosenTests = [];
	var chosenIds = [];
	var i, row,nameNode;
	var displayTests = "";
	var testIds = "";

	var inputs = testTable.getElementsByClassName("testCheckbox");

	for( i = 0; i < inputs.length; i++ ){
		if( inputs[i].checked ){
            row = inputs[i].id.split("_")[1];
           nameNode = currentSampleDiv.getElementsByClassName("testName_" + row)[0];
			chosenTests.push(nameNode.nextSibling.nodeValue);
			chosenIds.push( nameNode.value);
		}
	}

	if( chosenTests.length > 0 ){
		displayTests = chosenTests[0]; //this is done to get the ',' right
		testIds = chosenIds[0];
		for( i = 1; i < chosenTests.length; i++ ){
			displayTests += ", " + chosenTests[i];
			testIds += ", " + chosenIds[i];
		}
	}

	$("tests_" + selectedTypeRowId).value = chosenTests;
	$("testIds_" + selectedTypeRowId).value = chosenIds;

	if( checkbox){
		var panelIdElement = $("panelIds_" + selectedTypeRowId);
		
		if( checkbox.checked ){
			panelIdElement.value = addIdToUniqueIdList(panelId, panelIdElement.value);
		}else{
			var panelIdArray = panelIdElement.value.split(",");
			panelIdArray.splice(panelIdArray.indexOf(panelId), 1);
			panelIdElement.value = panelIdArray.join(",");
		}		
	}
	getNotificationsForTests(chosenIds, addNotificationConfigurations);
	addNotificationsOptions();
	if (typeof(referralTestSelected) === 'function') {
    	referralTestSelected();
	}
	testAndSetSave();
}

function addNotificationsOptions() {
	var resultReportingSection = document.getElementById("resultReportingSection");
	if (resultReportingSection == null ) {
		return;
	}
	if (!jQuery('.reportingHeader').length) {
		var resultsSectionHeader  = document.createElement("h2");
		resultsSectionHeader.setAttribute('class', 'reportingHeader');
		resultsSectionHeader.appendChild(document.createTextNode("<spring:message code='testnotification.patiententry.header'/>"));
		resultReportingSection.appendChild(resultsSectionHeader);
	}

	jQuery('#resultReportingSection .referralRow').addClass('deleteReferralRow');
	
	var reportingRows = jQuery('.reportingRow');
	reportingRows.addClass('deleteReportingRow');
	var reportingDivs = jQuery('.reportingDiv');
	reportingDivs.addClass('deleteReportingRow');

	var sampleTables = document.getElementsByClassName("samplesAddedTable");
	for(var x = 1 ; x < sampleTables.length ; x++){
      var samples = sampleTables[x].getElementsByClassName("sampleId");
	  samples.each(function(index, value) {
		var sampleNum = index.value
		var testNames = document.getElementById("tests_" + sampleNum).value.split(",");
		var testIds = document.getElementById("testIds_" + sampleNum).value.split(",");
		addNotificationOptionForRow(resultReportingSection, "<spring:message code='' text='Sample'/> " + sampleNum, sampleNum, testIds, testNames);
	});
	}
 
	jQuery('.deleteReportingRow').remove();
}

function addNotificationOptionForRow(resultReportingSection, rowLabel, sampleNum, testIds, testNames) {
	var table, div;
	if (testIds.length === 0) {
		return;
	}
	if (jQuery('#reportingDiv_sample_' + sampleNum).length) {
		div = document.getElementById('reportingDiv_sample_' + sampleNum);
		div.setAttribute('class', 'reportingDiv');
		table = document.getElementById('reportingTable_sample_' + sampleNum);
		table.setAttribute('class', 'reportingTable');
	} else {
		var div = document.createElement('div');
		div.setAttribute('id', 'reportingDiv_sample_' + sampleNum);
		div.setAttribute('class', 'reportingDiv');
		resultReportingSection.appendChild(div)
		
		var sampleSectionHeader  = document.createElement("h3");
		sampleSectionHeader.appendChild(document.createTextNode(rowLabel));
		div.appendChild(sampleSectionHeader)
		
		table = document.createElement("table");
		table.setAttribute('id', 'reportingTable_sample_' + sampleNum);
		table.setAttribute('class', 'reportingTable');
		var row = document.createElement("tr");
		row.setAttribute('class', 'reportingRowHeader');
		row.setAttribute('id', 'reportingRowHeader_sample_' + sampleNum);
		var col = document.createElement("td");
		row.appendChild(col);
		col = document.createElement("td");
		col.colSpan = "2";
		col.style.textAlign = "center";
		col.style.fontWeight = "bold";
		col.appendChild(document.createTextNode("<spring:message code='label.patient'/>"))
		row.appendChild(col);
		col = document.createElement("td");
		col.colSpan = "2";
		col.style.textAlign = "center";
		col.style.fontWeight = "bold";
		col.appendChild(document.createTextNode("<spring:message code='label.requester'/>"))
		row.appendChild(col);
		table.appendChild(row)
		div.appendChild(table);
	}

	
	for (var i = 0; i < testIds.length; ++i) {
		if (testIds[i] !== "") {
			addNotificationsOption(sampleNum, testIds[i], testIds[i], testNames[i], table);
		}
	}
}

function addNotificationsOption(sampleNum, testNum, testId, testName, table) {
	var resultReportingSection = document.getElementById("resultReportingSection");
	if (resultReportingSection == null ) {
		return;
	}
	if (jQuery('#reportingRow_sample_' + sampleNum + '_test_' + testNum).length) {
		document.getElementById('reportingRow_sample_' + sampleNum + '_test_' + testNum).setAttribute("class", "referralRow");
		return;
	}
	
	var row = document.createElement("tr");
	row.setAttribute('id', 'reportingRow_sample_' + sampleNum + '_test_' + testNum);
	row.setAttribute('class', 'reportingRow');
	var col = document.createElement("td");
	var emailNote = "<spring:message code='externalconnections.email'/>";
	var smsNote = "<spring:message code='externalconnections.sms'/>";
	col.appendChild(document.createTextNode(testName));
	col.style.fontWeight = "bold";
	row.appendChild(col);
	col = document.createElement("td");
	var checkbox = document.createElement("input");
	checkbox.type = "checkbox";
	checkbox.classList.add("patientEmailInput");
	checkbox.setAttribute("onchange","editNotificationValue(\"" + testId+ "\", \"" + testName + "\")");
	checkbox.value = testId;
	checkbox.id = "patientEmail_" + testId;
	col.appendChild(checkbox);
	col.appendChild(document.createTextNode(emailNote));
	row.appendChild(col);
	col = document.createElement("td");
	checkbox = document.createElement("input");
	checkbox.type = "checkbox";
	checkbox.classList.add("patientSMSInput");
	checkbox.setAttribute("onchange","editNotificationValue(\"" + testId+ "\", \"" + testName + "\")");
	checkbox.value = testId;
	checkbox.id = "patientSMS_" + testId;
	col.appendChild(checkbox);
	col.appendChild(document.createTextNode(smsNote));
	row.appendChild(col);
	col = document.createElement("td");
	checkbox = document.createElement("input");
	checkbox.type = "checkbox";
	checkbox.classList.add("providerEmailInput");
	checkbox.setAttribute("onchange","editNotificationValue(\"" + testId+ "\", \"" + testName + "\")");
	checkbox.value = testId;
	checkbox.id = "providerEmail_" + testId;
	col.appendChild(checkbox);
	col.appendChild(document.createTextNode(emailNote));
	row.appendChild(col);
	col = document.createElement("td");
	checkbox = document.createElement("input");
	checkbox.type = "checkbox";
	checkbox.classList.add("providerSMSInput");
	checkbox.setAttribute("onchange","editNotificationValue(\"" + testId+ "\", \"" + testName + "\")");
	checkbox.value = testId;
	checkbox.id = "providerSMS_" + testId;
	col.appendChild(checkbox);
	col.appendChild(document.createTextNode(smsNote));
	row.appendChild(col);
	table.appendChild(row);
	
}

function addNotificationConfigurations(xhr) {
	var jsonObj = JSON.parse(xhr.response);
	for (var i = 0; i < jsonObj.length; ++i) {
		var testId = jsonObj[i].testId;
		var patientEmail = jsonObj[i].patientEmail.active;
		var patientSMS = jsonObj[i].patientSMS.active;
		var providerEmail = jsonObj[i].providerEmail.active;
		var providerSMS = jsonObj[i].providerSMS.active;

		document.getElementById("patientEmail_" + testId).checked = patientEmail;
		document.getElementById("patientSMS_" + testId).checked = patientSMS;
		document.getElementById("providerEmail_" + testId).checked = providerEmail;
		document.getElementById("providerSMS_" + testId).checked = providerSMS;
	}
}

function editNotificationValue(testId, testName) {
	
	var notificationVals = jQuery('input[class="patientEmailInput"]:checked').map(function(){
        return jQuery(this).val();
    }).get().join(',');
	document.getElementById("patientEmailNotificationTestIds").value = notificationVals;
	
	notificationVals = jQuery('input[class="patientSMSInput"]:checked').map(function(){
        return jQuery(this).val();
     }).get().join(',');
	document.getElementById("patientSMSNotificationTestIds").value = notificationVals;
	
	notificationVals = jQuery('input[class="providerEmailInput"]:checked').map(function(){
        return jQuery(this).val();
     }).get().join(',');
	document.getElementById("providerEmailNotificationTestIds").value = notificationVals;
	
	notificationVals = jQuery('input[class="providerSMSInput"]:checked').map(function(){
        return jQuery(this).val();
     }).get().join(',');
	document.getElementById("providerSMSNotificationTestIds").value = notificationVals;

	document.getElementById("customNotificationLogic").value = "true";
}


function addIdToUniqueIdList(id, list) {
	if (list) {
		var array = list.split(",");
		var cnt = 0;
		
		for (var i=0; i<array.length; i++) {
			cnt++;
			if (id == array[i]) {
				return list;    
			}
		}
		if (array.length > 0) {
			list = list + "," + id;
			return list;
		} else {
			return id;
		}
			
	} 
	return id;
}

function sectionSelectionChanged( selectionElement ){
	var selection = jQuery( selectionElement);
	var testIdNumber = selection.attr("id").split("_")[1];
	var sectionMap = jQuery("#testSectionMap_" + selectedTypeRowId );
	sectionMap.val( sectionMap.val() + jQuery("#testName_" + testIdNumber).val() + ":" + selection.val() + "," );
	
	testAndSetSave();
}

function editSelectedTest( ){
	if( !sampleManualselected ){
    	getTestsForSampleType(currentCheckedType, processGetTestSuccess); //this is an asynchronous call and setSampleType will be called on the return of the call
    }else {
    	setSampleTests();
    }
}

function setSampleTests(isVariableSampleType ){
	currentSampleDiv = document.getElementById("samplesDisplay_"+selectedTypeRowId )
	deselectAllTests();
	var allTests = $("testIds_" + selectedTypeRowId ).value;
	var allPanels = $("panelIds_" + selectedTypeRowId).value;

	if( allTests.length > 0 ){
		var tests = allTests.split(",");
	    checkTests(tests, isVariableSampleType);
	}		
	
    if( allPanels.length > 0 ){
        var panels = allPanels.split(",");
		checkPanels(panels);
	}
   if (typeof(referralTestSelected) === 'function') {
    	referralTestSelected();
	}
	currentSampleDiv.getElementsByClassName("testSelections")[0].show();
	
}

function checkTests(tests, isVariableSampleType) {
	var inputs = currentSampleDiv.getElementsByClassName("addTestTable")[0].getElementsByClassName("testName");
    var testRow;

    for( var i = 0; i < tests.length; i++ ){
        for( var j = 0; j < inputs.length; j++ ){
        	if( inputs[j].value == tests[i] ){
                testRow = inputs[j].id.split("_")[1];
				jQuery("#"+currentSampleDiv.id).find("#test_" + testRow).click();
                if( isVariableSampleType){
                    populateUserSelectedType(testRow)
                }
	            break;
        	}
        }
    }
}

function populateUserSelectedType( testRow ){
    var selectedTypes = jQuery("#testTypeMap_" + selectedTypeRowId).val();
	var testId = jQuery("#"+currentSampleDiv.id).find("#testName_" + testRow).val();
    var selectedTypeList, selectedTypeListLength, typeSelection, selectedTypeName, selectionFound, i;

    if(selectedTypes){
        selectedTypeList = selectedTypes.split(",");
        selectedTypeListLength = selectedTypeList.length;
        typeSelection = jQuery("#userSampleTypeSelect_" + testRow);
        for( i = 0; i < selectedTypeListLength; i++ ){
            selectionFound = true;
            if( selectedTypeList[i].indexOf(testId) == 0){
                selectionFound = false;
                selectedTypeName = selectedTypeList[i].split(":")[1];
                typeSelection.children("option").each(function(j){
                    if( selectedTypeName == jQuery(this).text() ){
                        typeSelection.val(jQuery(this).val());
                        if( jQuery(this).val() == jQuery("#userSampleTypeQualifierValueID_" + testRow).val() ){
                            jQuery("#userSampleTypeQualifier_" + testRow).show();
                        }else{
                            jQuery("#userSampleTypeQualifier_" + testRow).hide();
                        }
                        selectionFound = true;
                        return;
                    }
                });

            }
            //if the selection is not found then it is "other"
            if( !selectionFound){
                jQuery("#userSampleTypeQualifierID_" + testRow).val( selectedTypeList[i].split(":")[1]);
            }
        }
    }
}
function checkPanels(panels) {
	pInputs = currentSampleDiv.getElementsByClassName("addPanelTable")[0].getElementsByTagName("input");
    for( var x = 0; x < panels.length; x++ ){
        for( var y = 0; y < pInputs.length; y++ ){
            if( pInputs[y].value == panels[x] ){
            	if (acceptExternalOrders) {
            		$(pInputs[y].id).click();
            		pInputs[y].checked = true;
            	} else {
            	    pInputs[y].checked = true;
            	}
                if (initializePanelTests) {
                	initializePanelTests = false;
	                var panelTests = getPanelTestMapEntry(panels[x]);      
	                if (panelTests) {
	                	panelSelected(pInputs[y], panelTests.split(","));
	                	assignTestsToSelected(pInputs[y], panels[x]);
	                }
	            }
                break;
            }
        }
    }
}

function panelSelected(checkBox, tests ){

	var testDropdown = jQuery(jQuery("#"+currentSampleDiv.id + " .testDropdown")[0]);
	var curVals = testDropdown.val() == null ? [] : testDropdown.val();
	var allVals;
	if (checkBox.checked) {
		allVals = [...new Set(curVals.concat(tests))];
	} else {
		allVals =  curVals.filter(function(value, index, arr){ 
	        return !tests.contains(value);
	    });
	}

	testDropdown.val(allVals).trigger('change');
}

function /*boolean*/ sampleAddValid( sampleRequired ){
	var testBoxes = document.getElementsByName("tests");
	var enable = true;
	var i;
	
	for(i = 0; i < testBoxes.length; i++ ){
		if( testBoxes[i].value.blank() ){
			return false;
		}
	}

	if( sampleRequired){
		var table  = document.getElementsByClassName("samplesAddedTable")[1];
		var rows = table.rows;
		//if length is 1, then no sample exists
		if( rows.length == 1 ){
			return false;
		}
	}

	//ensure that all enabled testSectionSelectors have values
	jQuery(".testSectionSelector:enabled").each( function(i, val){
		if( val.selectedIndex == 0){
			enable = false;
			return;
		}
	});

    //we should move everything to this model
    jQuery(".required").each( function(i, val){
        var elValue = jQuery(val).val();
         if( !elValue.trim().length || elValue == 0){
            enable = false;
            return;
        }
    });
	return enable;
}

function samplesHaveBeenAdded(){
	 if(!currentSampleDiv){
		return false;
	 }
	 return currentSampleDiv.getElementsByClassName("samplesAddedTable")[0].rows.length > 1;
}

function userSampleTypeSelectionChanged( userTypeSelectionId, row,  qualifiableId ){

    var sampleType =  jQuery("#" + userTypeSelectionId);
    var typeMap = jQuery("#testTypeMap_" + selectedTypeRowId );
	var testId = jQuery("#"+currentSampleDiv.id).find("#testName_" + testRow).val();
    typeMap.val( typeMap.val() + testId + ":" + jQuery("#" + userTypeSelectionId + " :selected").text() + "," );

    //testAndSetSave();

    if(sampleType.val() == qualifiableId){
        jQuery("#userSampleTypeQualifier_" + row).show();
        jQuery("#userSampleTypeQualifierID_" + row).addClass('required');
    }else{
        jQuery("#userSampleTypeQualifier_" + row).hide();
        jQuery("#userSampleTypeQualifierID_" + row).val('');
        jQuery("#userSampleTypeQualifierID_" + row).removeClass('required');
    }

    testAndSetSave();
}

function sampleTypeQualifierChanged(element){
    var typeMap = jQuery("#testTypeMap_" + selectedTypeRowId );
    typeMap.val( typeMap.val() +  jQuery("#testName_" + element.id.split("_")[1]).val() + ":" + element.value + "," );

    testAndSetSave();
}
</script>
<%
if (useInitialSampleCondition) {
%>
<div id="sampleConditionPrototype" style="display: none">
	<form:select path="initialSampleConditionList" multiple="true"
		title='<spring:message/>' id='prototypeID'>
		<c:forEach var="optionValue"
			items="${form.initialSampleConditionList}">
			<option value='${optionValue.id}'>${optionValue.value}</option>
		</c:forEach>
	</form:select>
</div>
<%
}
%>
<%
if (useSampleNature) {
%>
<div id="sampleNaturePrototype" style="display: none">
	<form:select path="" id='sampleNaturePrototypeID'>
		<form:options items="${form.sampleNatureList}" itemValue="id"
			itemLabel="value" />
	</form:select>
</div>
<%
}
%>
<div id="sectionPrototype" style="display: none;">
	<span class="requiredlabel" style="visibility: hidden;">*</span> <select
		id="testSectionPrototypeID" disabled
		onchange="sectionSelectionChanged( this );"
		class="testSectionSelector">
		<option value='0'></option>
		<c:forEach var="optionValue" items="${form.testSectionList}">
			<option value='${optionValue.id}'>${optionValue.value}</option>
		</c:forEach>
	</select>
</div>
<div id="userSampleTypePrototype" style="display: none;">
	<span class="requiredlabel" style="visibility: hidden;">*</span> <select
		id="userSampleTypePrototypeID" disabled="disabled">
		<option value='0'></option>

	</select>
</div>
<div id="userSampleTypeQualifierPrototype" style="display: none;">
	<span class="requiredlabel">*</span> <input
		id="userSampleTypeQualifierPrototypeID" size="12" value=""
		disabled="disabled" onchange="sampleTypeQualifierChanged(this)"
		type="text"> <input
		id="userSampleTypeQualifierPrototypeValueID" value="" type="hidden">
</div>

<div id="rejectedReasonPrototype" style="display: none">
	<form:select path="rejectReasonList" multiple="false"
		title='<spring:message/>' style="width:100%" disabled="true"
		id='rejectPrototypeID'>
		<option value=''></option>
		<c:forEach var="optionValue" items="${form.rejectReasonList}">
			<option value='${optionValue.id}'>${optionValue.value}</option>
		</c:forEach>
	</form:select>
</div>

<div id="crossPanels"></div>

<Table style="width: 100%">
	<tr>
		<td><spring:message code="sample.entry.sample.type" /></td>
	</tr>

	<tr>
		<td><select onchange="sampleTypeSelected(this);"
			id="sampleTypeSelect">
				<option value="" label="" />
				<c:forEach items="${form.sampleTypes}" var="sampleType">
					<option value="${sampleType.id}">${sampleType.value}</option>
				</c:forEach>
		</select></td>
	</tr>
</Table>

<br />
<div id="samplesAdded" class="samplesAdded" style="display: none;">
	<hr style="width: 100%" />

	<table id="samplesAddedTable" class="samplesAddedTable"
		width=<%=useCollectionDate ? "100%" : "80%"%>>
		<tr>
			<th style="width: 5%"></th>
			<th style="width: 10%"><spring:message code="sample.entry.id" />
			</th>
			<th style="width: 10%"><spring:message
					code="sample.entry.sample.type" /></th>
			<%
			if (useInitialSampleCondition) {
			%>
			<%-- <th style="width:15%">
					<spring:message code="sample.entry.sample.condition"/>
				</th> --%>
			<%
			}
			%>
			<%
			if (useSampleNature) {
			%>
			<th style="width: 15%"><spring:message
					code="sample.entry.sample.nature" /></th>
			<%
			}
			%>
			<%
			if (useCollectionDate) {
			%>
			<th><spring:message code="sample.collectionDate" />&nbsp;<%=DateUtil.getDateUserPrompt()%>
			</th>
			<th><spring:message code="sample.collectionTime" /></th>
			<%
			}
			%>
			<%
			if (useCollector) {
			%>
			<th><spring:message code="sample.entry.collector" /></th>
			<%
			}
			%>
			<th style="width: 35%"><span class='requiredlabel'>*</span>&nbsp;<spring:message
					code="sample.entry.sample.tests" /></th>
			<th style="width: 5%"><spring:message code="result.rejected" />
			</th>
			<th style="width: 10%"><spring:message
					code="note.type.rejectReason" /></th>
			<th style="width: 10%"></th>
		</tr>
	</table>
	<br />
	<div id="testSelections" class="testSelections" style="display: none;">
		<table style="margin-left: 1%; width: 60%;" id="addTables">
			<tr>
				<td style="width: 30%; vertical-align: top;">
					<span class="caption"> 
					<spring:message code="sample.entry.panels" />
					</span>
				</td>
				<td style="width: 70%; vertical-align: top; margin-left: 3%;">
					<span class="caption"> 
					<spring:message	code="sample.entry.available.tests" />
					</span> 
				</td>
				</tr>
				<tr>
				<td></td>
				<td>
					<select class="testDropdown" style="width: 97%;" multiple="multiple">
					</select>
				</td>
			</tr>
			<tr>
				<td style="width: 30%; vertical-align: top;">
					<table style="width: 97%" id="addPanelTable" class="addPanelTable">
						<tr>
							<th style="width: 20%">&nbsp;</th>
							<th style="width: 80%"><spring:message
									code="sample.entry.panel.name" /></th>
						</tr>

					</table>
				</td>
				<td style="width: 70%; vertical-align: top; margin-left: 3%;">
					<table style="width: 97%" id="addTestTable"
						class="addTestTable">

						<tr>
							<th style="width: 5%">&nbsp;</th>
							<th style="width: 50%"><spring:message
									code="sample.entry.available.test.names" /></th>
							<th style="width: 40%; display: none;" id="sectionHead">
								Section</th>
							<%-- <th style="width:25%" style="display:none" id="userSampleTypeHead">
                            <spring:message code="sample.entry.sample.type"/>
                        </th> --%>
							<th style="width: 20%">&nbsp;</th>
						</tr>
					</table></td>
			</tr>
		</table>
	</div>
</div>
<br />