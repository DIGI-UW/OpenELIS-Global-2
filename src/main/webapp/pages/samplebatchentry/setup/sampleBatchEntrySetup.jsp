<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page import="org.openelisglobal.common.action.IActionConstants,
                 org.openelisglobal.common.util.ConfigurationProperties,
                 org.openelisglobal.common.util.ConfigurationProperties.Property,
                 org.openelisglobal.common.util.Versioning,
                 org.openelisglobal.internationalization.MessageUtil" %>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<%
    boolean acceptExternalOrders = ConfigurationProperties.getInstance().isPropertyValueEqual(Property.ACCEPT_EXTERNAL_ORDERS, "true");
%>

<link rel="stylesheet" href="css/jquery_ui/jquery.ui.all.css?">
<link rel="stylesheet" href="css/customAutocomplete.css?">

<script type="text/javascript" src="scripts/utilities.js?" ></script>
<script src="scripts/ui/jquery.ui.core.js?"></script>
<script src="scripts/ui/jquery.ui.widget.js?"></script>
<script src="scripts/ui/jquery.ui.button.js?"></script>
<script src="scripts/ui/jquery.ui.menu.js?"></script>
<script src="scripts/ui/jquery.ui.position.js?"></script>
<script src="scripts/ui/jquery.ui.autocomplete.js?"></script>
<script src="scripts/customAutocomplete.js?"></script>
<script type="text/javascript" src="scripts/ajaxCalls.js?"></script>
<script type="text/javascript" src="scripts/laborder.js?"></script>

<script type="text/javascript" >

var acceptExternalOrders = <%= acceptExternalOrders %>;
var dirty = false;
var invalidSampleElements = [];

 
function isFieldValid(fieldname) {
    return invalidSampleElements.indexOf(fieldname) == -1;
}

function setSampleFieldInvalid(field) {
    if( invalidSampleElements.indexOf(field) == -1 ) {
        invalidSampleElements.push(field);
    }
}

function setSampleFieldValid(field) {
    var removeIndex = invalidSampleElements.indexOf( field );
    if( removeIndex != -1 ) {
        invalidSampleElements.splice( removeIndex,1);
    }
}

function  /*void*/ processValidateEntryDateSuccess(xhr) {
    var message = xhr.responseXML.getElementsByTagName("message").item(0).firstChild.nodeValue;
    var formField = xhr.responseXML.getElementsByTagName("formfield").item(0).firstChild.nodeValue;
    var isValid = message == "<%=IActionConstants.VALID%>";
    //utilites.js
    selectFieldErrorDisplay( isValid, $(formField));
    setSampleFieldValidity( isValid, formField );
    checkValidSubPages();
    if( message == '<%=IActionConstants.INVALID_TO_LARGE%>' ) {
        alert( '<spring:message code="error.date.inFuture"/>' );
    } else if( message == '<%=IActionConstants.INVALID_TO_SMALL%>' ) {
        alert( '<spring:message code="error.date.inPast"/>' );
    }
}


function checkValidEntryDate(date, dateRange, blankAllowed) {   
    if((!date.value || date.value == "") && !blankAllowed) {
        checkValidSubPages();
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
    if( valid ) {
        setSampleFieldValid(fieldName);
    } else {
        setSampleFieldInvalid(fieldName);
    }
}

function setMyCancelAction(form, action, validate, parameters){
    //first turn off any further validation
    setAction(document.getElementById("mainForm"), 'Cancel', 'no', '');
}

function /*void*/ loadSamples() {
    alert( "Implementation error:  loadSamples not found in addSample tile");
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

function /*void*/ nextPage() {
    loadSamples(); //in addSample tile
    window.onbeforeunload = null; // Added to flag that formWarning alert isn't needed.
    var form = document.getElementById("mainForm");
    //decide page to navigate to based on method of labels
    if (document.getElementById('study').value == "routine") {
		form.action = "SampleBatchEntry";
    } else {
    	form.action = "SampleBatchEntryByProject";
    }
    form.submit();
}

//check all subpages to see if 'next' should active
function checkValidSubPages() {
	var valid = configBarcodeValid() && sampleAddValid(true) && sampleOrderValid()
		&& invalidSampleElements.length == 0 && jQuery(".error").length == 0;
	setNext(valid)
}

function /*void*/ setNext(valid) {
    $("nextButtonId").disabled = !valid;
}

</script>


<%-- Order --%>    
<div id=orderEntryPage >
	<h2><%= MessageUtil.getContextualMessage("sample.entry.order.label") %></h2>	
	<jsp:include page="${sampleOrderFragment}"/>
</div>

<%-- Sample --%>  
<div id=sampleEntryPage >
	<h2><%= MessageUtil.getContextualMessage("sample.entry.sampleList.label") %></h2>
	<jsp:include page="${addSampleFragment}"/>
</div>

<%-- Barcode Configuration --%>
<div id=configureBarcodePage >
	<h2><%= MessageUtil.getContextualMessage("sample.batchentry.configureBarcode") %></h2>
	<jsp:include page="${configureLabelsFragment}"/>
</div>

