<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page import="org.openelisglobal.common.action.IActionConstants,
                 org.openelisglobal.common.util.ConfigurationProperties,
                 org.openelisglobal.common.util.ConfigurationProperties.Property,
                 org.openelisglobal.common.formfields.FormFields,
                 org.openelisglobal.common.formfields.FormFields.Field,
                 org.openelisglobal.common.util.Versioning,
                 org.openelisglobal.internationalization.MessageUtil,
                 org.openelisglobal.sample.bean.SampleOrderItem" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<c:set var="formName" value="${form.formName}" />
<c:set var="entryDate" value="${form.currentDate}" />
<c:set var="sampleOrderItems" value="${form.sampleOrderItems}" />


<%
    boolean useSTNumber =  FormFields.getInstance().useField(FormFields.Field.StNumber);
    boolean useMothersName = FormFields.getInstance().useField(FormFields.Field.MothersName);
    boolean useProviderInfo = FormFields.getInstance().useField(FormFields.Field.ProviderInfo);
    boolean patientRequired = FormFields.getInstance().useField(FormFields.Field.PatientRequired);
    boolean trackPayment = ConfigurationProperties.getInstance().isPropertyValueEqual(Property.TRACK_PATIENT_PAYMENT, "true");
    boolean requesterPersonRequired = FormFields.getInstance().useField(Field.SampleEntryRequesterPersonRequired);
	boolean acceptExternalOrders = ConfigurationProperties.getInstance().isPropertyValueEqual(Property.ACCEPT_EXTERNAL_ORDERS, "true");
    boolean restrictNewProviderEntries = ConfigurationProperties.getInstance().isPropertyValueEqual(Property.restrictFreeTextProviderEntry, "true");
    boolean restrictNewReferringSiteEntries = ConfigurationProperties.getInstance().isPropertyValueEqual(Property.restrictFreeTextRefSiteEntry, "true");
%>


<script type="text/javascript" src="scripts/utilities.js?" ></script>

<link type="text/css" rel="stylesheet" href="css/jquery_ui/jquery.ui.all.css?">
<link type="text/css" rel="stylesheet" href="css/customAutocomplete.css?">

<script type="text/javascript" src="scripts/ui/jquery.ui.core.js?"></script>
<script type="text/javascript" src="scripts/ui/jquery.ui.widget.js?"></script>
<script type="text/javascript" src="scripts/ui/jquery.ui.button.js?"></script>
<script type="text/javascript" src="scripts/ui/jquery.ui.menu.js?"></script>
<script type="text/javascript" src="scripts/ui/jquery.ui.position.js?"></script>
<script type="text/javascript" src="scripts/ui/jquery.ui.autocomplete.js?"></script>
<script type="text/javascript" src="scripts/customAutocomplete.js?"></script>
<script type="text/javascript" src="scripts/ajaxCalls.js?"></script>
<script type="text/javascript" src="scripts/laborder.js?"></script>


<script type="text/javascript" >

var useSTNumber = <%= useSTNumber %>;
var useMothersName = <%= useMothersName %>;
var requesterPersonRequired = <%= requesterPersonRequired %>;
var acceptExternalOrders = <%= acceptExternalOrders %>;
var restrictNewReferringSiteEntries = <%= restrictNewReferringSiteEntries %>;
var dirty = false;
var invalidSampleElements = [];
var requiredFields = new Array("labNo", "receivedDateForDisplay" );
var currentReferalDiv ;
var currentReferalDivSelector ;

if( requesterPersonRequired ){
	if (<%=restrictNewProviderEntries%>) {
		requiredFields.push("providerPersonId");
	} else {
		requiredFields.push("providerLastNameID");
	}
    
}

if (<%=restrictNewReferringSiteEntries%>) {
		requiredFields.push("requesterId");
} else {
		requiredFields.push("requesterName");
}

<% if( FormFields.getInstance().useField(Field.SampleEntryUseRequestDate)){ %>
    requiredFields.push("requestDate");
<% } %>

 
function isFieldValid(fieldname)
{
    return invalidSampleElements.indexOf(fieldname) == -1;
}

function setSampleFieldInvalid(field)
{
    if( invalidSampleElements.indexOf(field) == -1 )
    {
        invalidSampleElements.push(field);
    }
}

function setSampleFieldValid(field)
{
    var removeIndex = invalidSampleElements.indexOf( field );
    if( removeIndex != -1 )
    {
        invalidSampleElements.splice( removeIndex,1);
    }
}

function isSaveEnabled()
{
    return invalidSampleElements.length == 0;
}

function submitTheForm(form)
{
    setAction(form, 'Update', 'yes', '?ID=');
}

function  /*void*/ processValidateEntryDateSuccess(xhr){

    //alert(xhr.responseText);
    
    var message = xhr.responseXML.getElementsByTagName("message").item(0).firstChild.nodeValue;
    var formField = xhr.responseXML.getElementsByTagName("formfield").item(0).firstChild.nodeValue;

    var isValid = message == "<%=IActionConstants.VALID%>";

    //utilites.js
    selectFieldErrorDisplay( isValid, $(formField));
    setSampleFieldValidity( isValid, formField );
    setSave();

    if( message == '<%=IActionConstants.INVALID_TO_LARGE%>' ){
        alert( '<spring:message code="error.date.inFuture"/>' );
    }else if( message == '<%=IActionConstants.INVALID_TO_SMALL%>' ){
        alert( '<spring:message code="error.date.inPast"/>' );
    }
}


function checkValidEntryDate(date, dateRange, blankAllowed)
{   
    if((!date.value || date.value == "") && !blankAllowed){
        setSave();
        return;
    } else if ((!date.value || date.value == "") && blankAllowed) {
        setSampleFieldValid(date.id);
        setValidIndicaterOnField(true, date.id);
        return;
    }


    if( !dateRange || dateRange == ""){
        dateRange = 'past';
    }
    
    //ajax call from utilites.js
    isValidDate( date.value, processValidateEntryDateSuccess, date.id, dateRange );
}


function setSampleFieldValidity( valid, fieldName ){

    if( valid )
    {
        setSampleFieldValid(fieldName);
    }
    else
    {
        setSampleFieldInvalid(fieldName);
    }
}


function checkValidTime(time, blankAllowed)
{
    var lowRangeRegEx = new RegExp("^[0-1]{0,1}\\d:[0-5]\\d$");
    var highRangeRegEx = new RegExp("^2[0-3]:[0-5]\\d$");

    if (time.value.blank() && blankAllowed == true) {
        clearFieldErrorDisplay(time);
        setSampleFieldValid(time.name);
        setSave();
        return;        
    }

    if( lowRangeRegEx.test(time.value) ||
        highRangeRegEx.test(time.value) )
    {
        if( time.value.length == 4 )
        {
            time.value = "0" + time.value;
        }
        clearFieldErrorDisplay(time);
        setSampleFieldValid(time.name);
    }
    else
    {
        setFieldErrorDisplay(time);
        setSampleFieldInvalid(time.name);
    }

    setSave();
}

function setMyCancelAction(form, action, validate, parameters)
{
    //first turn off any further validation
    setAction(document.getElementById("mainForm"), 'Cancel', 'no', '');
}


function patientInfoValid()
{
    var hasError = false;
    var returnMessage = "";

    if( fieldIsEmptyById("patientID") )
    {
        hasError = true;
        returnMessage += ": patient ID";
    }

    if( fieldIsEmptyById("dossierID") )
    {
        hasError = true;
        returnMessage += ": dossier ID";
    }

    if( fieldIsEmptyById("firstNameID") )
    {
        hasError = true;
        returnMessage += ": first Name";
    }
    if( fieldIsEmptyById("lastNameID") )
    {
        hasError = true;
        returnMessage += ": last Name";
    }


    if( hasError )
    {
        returnMessage = "Please enter the following patient values  " + returnMessage;
    }else
    {
        returnMessage = "valid";
    }

    return returnMessage;
}



function saveItToParentForm(form) {
 submitTheForm(form);
}

function addPatientInfo(  ){
    $("patientInfo").show();
}

function showHideSection(button, targetId){
    targetId = targetId+button.name
    if( button.value == "+" ){
        showSection(button, targetId);
    }else{
        hideSection(button, targetId);
    }
}

function showSection( button, targetId){
    jQuery("#" + targetId ).show();
    button.value = "-";
}

function hideSection( button, targetId){
    jQuery("#" + targetId ).hide();
    button.value = "+";
}

function /*bool*/ requiredSampleEntryFieldsValid(){

    if( acceptExternalOrders){ 
        if (missingRequiredValues())
            return false;
    } 

    if( jQuery("#useReferral").prop('checked') && typeof(missingRequiredReferralValues) === 'function' ){
    	return !missingRequiredReferralValues();
    }
        
    for( var i = 0; i < requiredFields.length; ++i ){
        if( $(requiredFields[i]).value.blank() ){
            //special casing
            if( requiredFields[i] == "requesterId" && 
               !( ($("requesterId").selectedIndex == 0)  &&  $("newRequesterName").value.blank())){
                continue;
            }
        return false;
        }
    }
    
    return sampleAddValid( true );
}

function /*bool*/ sampleEntryTopValid(){
    return invalidSampleElements.length == 0 && requiredSampleEntryFieldsValid() && jQuery(".error").length == 0;
}

function /*void*/ loadSamples(){
    alert( "Implementation error:  loadSamples not found in addSample tile");
}

function show(id){
    document.getElementById(id).style.visibility="visible";
}

function hide(id){
    document.getElementById(id).style.visibility="hidden";
}


function capitalizeValue( text){
    $("requesterId").value = text.toUpperCase();
}

function checkOrderReferral(){

    var value = jQuery("#externalOrderNumber").val()
    getLabOrder(value, processLabOrderSuccess);
    showSection( $("orderSectionId"), 'orderDisplay');
}

function clearOrderData() { 
    var addTestTable = document.getElementsByClassName("addTestTable")[1];
	var addPanelTable = document.getElementsByClassName("addPanelTable")[1];
    clearTable(addTestTable);
    clearTable(addPanelTable);
    clearSearchResultTable();
    addPatient();
    clearPatientInfo();
    clearRequester();
    removeCrossPanelsTestsTable();

}

function processLabOrderSuccess(xhr){
    //alert(xhr.responseText);

    clearOrderData();
    
    <c:if test="${param.attemptAutoSave}">
	<c:choose>
	<c:when test="${not empty param.labNumber}">
		jQuery('#labNo').val('<spring:escapeBody javaScriptEscape="true">${param.labNumber}</spring:escapeBody>');
		setOrderModified();
	</c:when>
	<c:otherwise>
		setOrderModified();
		getNextAccessionNumber();
	</c:otherwise>
	</c:choose>
	</c:if>

    var message = xhr.responseXML.getElementsByTagName("message").item(0);
    var formField = xhr.responseXML.getElementsByTagName("formfield").item(0);
    var order = formField.getElementsByTagName("order").item(0);

    SampleTypes = [];
    CrossPanels = [];
    CrossTests = [];
    sampleTypeMap = {};

    if( message.firstChild.nodeValue == "valid" ) {
        jQuery(".patientSearch").hide();
        var patienttag = order.getElementsByTagName('patient');
        if (patienttag) {
            parsePatient(patienttag);
        }

        var requester = order.getElementsByTagName('requester');
        if (requester) {
            parseRequester(requester);
        }

        var requestingOrg = order.getElementsByTagName('requestingOrg');
        var location = order.getElementsByTagName('location');
        
       if (restrictNewReferringSiteEntries) {
            if (requestingOrg) {
                parseRequestingOrg(requestingOrg);
            }
            if (location && !jQuery("#requesterId").val()) {
                parseLocation(location);
            }
        }
        
        var useralert = order.getElementsByTagName("user_alert");
        var alertMessage = "";
        if (useralert) {
            if (useralert.length > 0) {
                alertMessage = useralert[0].firstChild.nodeValue;
                alert(alertMessage);
            }
        }
        var sampletypes = order.getElementsByTagName("sampleType");

        // initialize objects and globals
        sampleTypeOrder = -1;
        crossSampleTypeMap = {};
        crossSampleTypeOrderMap = {};

        parseSampletypes(sampletypes, SampleTypes);
        var crosspanels = order.getElementsByTagName("crosspanel");
        parseCrossPanels(crosspanels, crossSampleTypeMap, crossSampleTypeOrderMap);
        var crosstests = order.getElementsByTagName("crosstest");
        parseCrossTests(crosstests, crossSampleTypeMap, crossSampleTypeOrderMap);

        document.getElementsByClassName("samplesAdded")[1].show();
        notifyChangeListeners();
        testAndSetSave();
        populateCrossPanelsAndTests(CrossPanels, CrossTests, '${entryDate}');
        displaySampleTypes('${entryDate}');

        if (SampleTypes.length > 0)
             sampleClicked(1);

        } else {
            jQuery(".patientSearch").show();
            alert(message.firstChild.nodeValue);
        }

    <c:if test="${param.attemptAutoSave}">
	var validToSave =  patientFormValid() && sampleEntryTopValid();
	if (validToSave) {
		savePage();
	}
	</c:if>
}

function parsePatient(patienttag) {
    var guidtag = patienttag.item(0).getElementsByTagName("guid");
    var guid;
    if (guidtag) {
        if (guidtag[0].firstChild) {
            guid = guidtag[0].firstChild.nodeValue;
            patientSearch("", "", "", "", "", "", guid, "", "", "true", processSearchSuccess, processSearchFailure );
        }       
    }
}



function clearRequester() {
	clearProvider();
    
    $("labNo").value = '';
    $("receivedDateForDisplay").value = '${entryDate}';
    $("receivedTime").value = '';
 //   $("externalOrderNumber").value = '';

}

function clearProvider() {
	setSelectComboboxToId('providerPersonId', '');
}

function parseRequester(requester) {
    var requesterIdElement = requester.item(0).getElementsByTagName("personId");
    var requesterId = "";
    if (requesterIdElement.length > 0) {
    	requesterId = requesterIdElement[0].firstChild.nodeValue;

        setSelectComboboxToId("providerPersonId", requesterId);
    }
    
    var firstName = requester.item(0).getElementsByTagName("firstName");
    var first = "";
    if (firstName.length > 0) {
            first = firstName[0].firstChild.nodeValue;
            $("providerFirstNameID").value = first;
    }
    var lastName = requester.item(0).getElementsByTagName("lastName");
    var last = "";
    if (lastName.length > 0) {
        last = lastName[0].firstChild.nodeValue;
        $("providerLastNameID").value = last;    
    }
    
    var phoneNum = requester.item(0).getElementsByTagName("providerWorkPhoneID");
    var phone = "";
    if (phoneNum.length > 0) {
        if (phoneNum[0].firstChild) {
            phone = phoneNum[0].firstChild.nodeValue;
            $("providerWorkPhoneID").value = phone;
        }
    }
}

function parseRequestingOrg(requestingOrg) {
	var requestingOrgId = requestingOrg.item(0).getElementsByTagName("id");
    var id = "";
    if (requestingOrgId.length > 0) {
            id = requestingOrgId[0].firstChild.nodeValue;
    }
    setSelectComboboxToId("requesterId", id);
}

function parseLocation(location) {
	var locationId = location.item(0).getElementsByTagName("id");
    var id = "";
    if (locationId.length > 0) {
            id = locationId[0].firstChild.nodeValue;
    }
    setSelectComboboxToId("requesterId", id);
}

function parseSampletypes(sampletypes, SampleTypes) {
        
        var index = 0;
        for( var i = 0; i < sampletypes.length; i++ ) {

            var sampleTypeName = sampletypes.item(i).getElementsByTagName("name")[0].firstChild.nodeValue;
            var sampleTypeId   = sampletypes.item(i).getElementsByTagName("id")[0].firstChild.nodeValue;
            var panels         = sampletypes.item(i).getElementsByTagName("panels")[0];
            var tests          = sampletypes.item(i).getElementsByTagName("tests")[0];
            var collection     = sampletypes.item(i).getElementsByTagName("collection")[0];
            var sampleTypeInList = getSampleTypeMapEntry(sampleTypeId);
            if (!sampleTypeInList) {
                index++;
                SampleTypes[index-1] = new SampleType(sampleTypeId, sampleTypeName);
                sampleTypeMap[sampleTypeId] = SampleTypes[index-1];
                SampleTypes[index-1].rowid = index;
                sampleTypeInList = SampleTypes[index-1];
                                
                //var addTable = $("samplesAddedTable");
                //var sampleDescription = sampleTypeName;
                //var sampleTypeValue = sampleTypeId;
                //var currentTime = getCurrentTime();
                
                //addTypeToTable(addTable, sampleDescription, sampleTypeValue, currentTime,  '${entryDate}' );
            
            }
            var panelnodes = getNodeNamesByTagName(panels, "panel");
            var testnodes  = getNodeNamesByTagName(tests, "test");
            var collectionDate = collection.getElementsByTagName("date");
            var collectionTime = collection.getElementsByTagName("time");
            
            addPanelsToSampleType(sampleTypeInList, panelnodes);
            addTestsToSampleType(sampleTypeInList, testnodes);
            addCollectionToSampleType(sampleTypeInList, collectionDate, collectionTime);
           
        }

}

function addPanelsToSampleType(sampleType, panelNodes) {
    for (var i=0; i<panelNodes.length; i++) {
       sampleType.panels[sampleType.panels.length] = panelNodes[i];
    }
}

function addTestsToSampleType(sampleType, testNodes) {
    for (var i=0; i<testNodes.length; i++) {
       sampleType.tests[sampleType.tests.length] = new Test(testNodes[i].id, testNodes[i].name);
    }
}

function addCollectionToSampleType(sampleType, collectionDate, collectionTime) {
    for (var i=0; i<collectionDate.length; i++) {
        sampleType.collectionDate = collectionDate[i].firstChild.nodeValue;
     }
    for (var i=0; i<collectionTime.length; i++) {
        sampleType.collectionTime = collectionTime[i].firstChild.nodeValue;
     }
 }


function parseCrossPanels(crosspanels, crossSampleTypeMap, crossSampleTypeOrderMap) {
        for(i = 0; i < crosspanels.length; i++ ) {
            var crossPanelName = crosspanels.item(i).getElementsByTagName("name")[0].firstChild.nodeValue;
            var crossPanelId   = crosspanels.item(i).getElementsByTagName("id")[0].firstChild.nodeValue;
            var crossSampleTypes         = crosspanels.item(i).getElementsByTagName("crosssampletypes")[0];
            
            CrossPanels[i] = new CrossPanel(crossPanelId, crossPanelName);
            CrossPanels[i].sampleTypes = getNodeNamesByTagName(crossSampleTypes, "crosssampletype");
            CrossPanels[i].typeMap = new Array(CrossPanels[i].sampleTypes.length);
            
            for (j = 0; j < CrossPanels[i].sampleTypes.length; j = j + 1) {
                CrossPanels[i].typeMap[CrossPanels[i].sampleTypes[j].name] = "t";
                var sampleType = getCrossSampleTypeMapEntry(CrossPanels[i].sampleTypes[j].id);
                
                if (sampleType === undefined) {
                    crossSampleTypeMap[CrossPanels[i].sampleTypes[j].id] = CrossPanels[i].sampleTypes[j];
                    sampleTypeOrder = sampleTypeOrder + 1;
                    crossSampleTypeOrderMap[sampleTypeOrder] = CrossPanels[i].sampleTypes[j].id;
                }
            }
        }
} 

function parseCrossTests(crosstests, crossSampleTypeMap, crossSampleTypeOrderMap) {
    for (x = 0; x < crosstests.length; x = x + 1) {
        var crossTestName = crosstests.item(x).getElementsByTagName("name")[0].firstChild.nodeValue;
   //     var crossTestId   = crosstests.item(x).getElementsByTagName("id")[0].firstChild.nodeValue;
        var crossSampleTypes  = crosstests.item(x).getElementsByTagName("crosssampletypes")[0];
        
        CrossTests[x] = new CrossTest(crossTestName);
        CrossTests[x].sampleTypes = getNodeNamesByTagName(crossSampleTypes, "crosssampletype");
        CrossTests[x].typeMap = new Array(CrossTests[x].sampleTypes.length);    
        var sTypes = [];
        for (var y = 0; y < CrossTests[x].sampleTypes.length; y++) {
        
            //alert(crossTestName + " " + CrossTests[x].sampleTypes[y].id + " testid=" + CrossTests[x].sampleTypes[y].testId);
            sTypes[y] = CrossTests[x].sampleTypes[y];
            CrossTests[x].typeMap[CrossTests[x].sampleTypes[y].name] = "t";
            var sType = getCrossSampleTypeMapEntry(CrossTests[x].sampleTypes[y].id);
            
            if (sType === undefined) {
                crossSampleTypeMap[CrossTests[x].sampleTypes[y].id] = CrossTests[x].sampleTypes[y];
                sampleTypeOrder++;
                crossSampleTypeOrderMap[sampleTypeOrder] = CrossTests[x].sampleTypes[y].id;               
            }
        }
        crossTestSampleTypeTestIdMap[crossTestName] = sTypes;
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
    selectFieldErrorDisplay( success, $(labElement));
    setSampleFieldValidity( success, labElement);

    if( !success ){
        alert( message.firstChild.nodeValue );
    }

    setSave();
}

function toggleReferral(element) {
    var blockId = element.parentNode.id;
    var referalId = "referTestSection_" + blockId.substring(blockId.indexOf('_')+ 1);
    currentReferalDivSelector = "#" + referalId;
    currentReferalDiv = document.getElementById(referalId) ;
	currentReferalDiv.toggle();
}
</script>
<script>
jQuery(document).ready( function() {
    addSampleTable();
});
var counter = 0;
function addSampleTable(){
    counter ++
    var content = $("addSampleTemplate").innerHTML;
    var newTable = document.createElement('table');
    newTable.style = "width:100%";
    newTable.innerHTML = content;
    var inputShowHide = newTable.getElementsByTagName("input")[0];
    inputShowHide.name = counter ;
    var divSampleDisplay = newTable.getElementsByTagName("div")[0];
    divSampleDisplay.id = "samplesDisplay_" + counter;
    var divReferalDisplay = newTable.getElementsByClassName("referTestSection")[0];
    divReferalDisplay.id = "referTestSection_" + counter;
    $("samplesBlock").appendChild(newTable);
}
</script>

<%-- This define may not be needed, look at usages (not in any other jsp or js page--%>

<form:hidden path="currentDate"/>
<form:hidden path="sampleOrderItems.newRequesterName" />


<% if( acceptExternalOrders){ %>
<%= MessageUtil.getContextualMessage( "referring.order.number" ) %>:
<form:input id="externalOrderNumber" path="sampleOrderItems.externalOrderNumber" onchange="checkOrderReferral();makeDirty();"/>
<input type="button" name="searchExternalButton" value='<%= MessageUtil.getMessage("label.button.search")%>'
       onclick="checkOrderReferral();makeDirty();">
<%= MessageUtil.getContextualMessage( "referring.order.not.found" ) %>
<hr style="width:100%;height:5px"/>

<% } %>
       
<form:checkbox id="rememberSiteAndRequester" path="rememberSiteAndRequester"/><spring:message code="label.rememberSiteRequester"/>
<%-- <form:checkbox id="rememberSamplePanelTest" path="rememberSamplePanelTest"/> <spring:message code="label.remembersamplepaneltest"/>--%>
       
<br>
<br>
            
<div id=sampleEntryPage >
<input type="button" name="showHide" value='-' onclick="showHideSection(this, 'orderDisplay');" id="orderSectionId">
<%= MessageUtil.getContextualMessage("sample.entry.order.label") %>
<span class="requiredlabel">*</span>

<jsp:include page="${sampleOrderFragment}"/>

<hr style="width:100%;height:5px" />

<form:hidden  path="sampleXML"  id="sampleXML"/>
<form:hidden path="patientEmailNotificationTestIds" id="patientEmailNotificationTestIds"/>
<form:hidden path="patientSMSNotificationTestIds" id="patientSMSNotificationTestIds"/>
<form:hidden path="providerEmailNotificationTestIds" id="providerEmailNotificationTestIds"/>
<form:hidden path="providerSMSNotificationTestIds" id="providerSMSNotificationTestIds"/>
<form:hidden path="customNotificationLogic" id="customNotificationLogic" value="false"/>

<table id = "addSampleTemplate"  style="display:none;">
     <tr>
        <td >
            <input type="button" name="showHide" value="-" onclick="showHideSection(this, 'samplesDisplay_');" id="samplesSectionId">
            <%= MessageUtil.getContextualMessage("sample.entry.sampleList.label") %>
            <span class="requiredlabel">*</span>

            <div id="samplesDisplay_0" class="colorFill" >
                <jsp:include page="${addSampleFragment}"/>
                <form:checkbox path="useReferral" id="useReferral" onclick="toggleReferral(this);referralTestSelected();" value="true"/> <spring:message code="sample.entry.referral.toggle" />
            </div>

            <div id="referTestSection" class ="referTestSection" style="display:none;">
                <jsp:include page="${referralInfoFragment}"/>
            </div>        
           <hr >
        </td>
    </tr> 
</table>
<div id = "samplesBlock" style="width:100%">
</div>

<br />
<button type="button" onclick="addSampleTable();"><spring:message code="sample.entry.sample.new"/></button>
<hr style="width:100%;height:5px" />

<table style="width:100%">
    <tr>
        <td style="width:15%;text-align:left">
            <input type="button" name="showHide" value="-" onclick="showHideSection(this, 'patientInfo');" id="orderSectionId">
            <spring:message code="sample.entry.patient" />:
            <% if ( patientRequired ) { %><span class="requiredlabel">*</span><% } %>
        </td>
        <td style="width:15%" id="firstName"><b>&nbsp;</b></td>
        <td style="width:15%">
            <% if(useMothersName){ %><spring:message code="patient.mother.name"/>:<% } %>
        </td>
        <td style="width:15%" id="mother"><b>&nbsp;</b></td>
        <td style="width:10%">
            <% if( useSTNumber){ %><spring:message code="patient.ST.number"/>:<% } %>
        </td>
        <td style="width:15%" id="st"><b>&nbsp;</b></td>
        <td style="width:5%">&nbsp;</td>
    </tr>
    <tr>
        <td>&nbsp;</td>
        <td id="lastName"><b>&nbsp;</b></td>
        <td>
            <spring:message code="patient.birthDate"/>:
        </td>
        <td id="dob"><b>&nbsp;</b></td>
        <td>
            <%=MessageUtil.getContextualMessage("patient.NationalID") %>:
        </td>
        <td id="national"><b>&nbsp;</b></td>
        <td>
            <spring:message code="patient.gender"/>:
        </td>
        <td id="gender"><b>&nbsp;</b></td>
    </tr>
</table>

<div id="patientInfo"  >
    <jsp:include page="${patientInfoFragment}"/>
    <jsp:include page="${patientClinicalInfoFragment}"/>
</div>
</div>

<div id="resultReportingSection">
</div>

<script type="text/javascript" >

//all methods here either overwrite methods in tiles or all called after they are loaded

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

function  /*void*/ savePage()
{
    loadSamples(); //in addSample tile

    window.onbeforeunload = null; // Added to flag that formWarning alert isn't needed.
    var form = document.getElementById("mainForm");
    form.action = "SamplePatientEntry";
    form.submit();
}


function /*void*/ setSave()
{
    var validToSave =  patientFormValid() && sampleEntryTopValid();
    $("saveButtonId").disabled = !validToSave;
}

//called from patientSearch.jsp
function /*void*/ selectedPatientChangedForSample(firstName, lastName, gender, DOB, stNumber, subjectNumb, nationalID, mother, pk ){
    patientInfoChangedForSample( firstName, lastName, gender, DOB, stNumber, subjectNumb, nationalID, mother, pk );
    $("patientPK_ID").value = pk;

    setSave();
}

//called from patientManagment.jsp
function /*void*/ patientInfoChangedForSample( firstName, lastName, gender, DOB, stNumber, subjectNum, nationalID, mother, pk ){
    setPatientSummary( "firstName", firstName );
    setPatientSummary( "lastName", lastName );
    setPatientSummary( "gender", gender );
    setPatientSummary( "dob", DOB );
    if( useSTNumber){setPatientSummary( "st", stNumber );}
    setPatientSummary( "national", nationalID );
    if( useMothersName){setPatientSummary( "mother", mother );}
    $("patientPK_ID").value = pk;

    makeDirty();
    setSave();
}

function /*voiid*/ setPatientSummary( name, value ){
    $(name).firstChild.firstChild.nodeValue = value;
}

//overwrites function from patient search
function /*void*/ doSelectPatient(){
/*  $("firstName").firstChild.firstChild.nodeValue = currentPatient["first"];
    $("mother").firstChild.firstChild.nodeValue = currentPatient["mother"];
    $("st").firstChild.firstChild.nodeValue = currentPatient["st"];
    $("lastName").firstChild.firstChild.nodeValue = currentPatient["last"];
    $("dob").firstChild.firstChild.nodeValue = currentPatient["DOB"];
    $("national").firstChild.firstChild.nodeValue = currentPatient["national"];
    $("gender").firstChild.firstChild.nodeValue = currentPatient["gender"];
    $("patientPK_ID").value = currentPatient["pk"];

    setSave();

*/
}
 
var patientRegistered = false;
var sampleRegistered = false;

/* is registered in patientManagement.jsp */
function /*void*/ registerPatientChangedForSampleEntry(){
    if( !patientRegistered ){
        addPatientInfoChangedListener( patientInfoChangedForSample );
        patientRegistered = true;
    }
}

/* is registered in sampleAdd.jsp */
function /*void*/ registerSampleChangedForSampleEntry(){
    if( !sampleRegistered ){
        addSampleChangedListener( makeDirty );
        sampleRegistered = true;
    }
}

registerPatientChangedForSampleEntry();
registerSampleChangedForSampleEntry();

jQuery(document).ready(function() {
	
	<% if( acceptExternalOrders){ %>
	if (jQuery("#externalOrderNumber").val()) {
		checkOrderReferral();
	}
	<% } %>
	
	jQuery("#useReferral").prop('checked', false);
	
	jQuery("#saveButtonId").click(
		      function(event) {
		         event.preventDefault();
		      }
		   );
})

</script>
