<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page import="org.openelisglobal.common.action.IActionConstants,
			     org.openelisglobal.common.formfields.FormFields,
			     org.openelisglobal.common.formfields.FormFields.Field,
				 org.openelisglobal.sample.util.AccessionNumberUtil,
			     org.openelisglobal.common.util.ConfigurationProperties.Property,
			     org.openelisglobal.internationalization.MessageUtil,
			     org.openelisglobal.common.util.*, org.openelisglobal.internationalization.MessageUtil" %>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<c:set var="localDBOnly" value='<%=Boolean.toString(ConfigurationProperties.getInstance().getPropertyValueLowerCase(Property.UseExternalPatientInfo).equals("false"))%>'/>

 <%
	 boolean supportSTNumber = FormFields.getInstance().useField(Field.StNumber);
	 boolean supportMothersName = FormFields.getInstance().useField(Field.MothersName);
	 boolean supportSubjectNumber = FormFields.getInstance().useField(Field.SubjectNumber);
	 boolean supportNationalID = FormFields.getInstance().useField(Field.NationalID);
	 boolean supportLabNumber = FormFields.getInstance().useField(Field.SEARCH_PATIENT_WITH_LAB_NO);
 %>

<script type="text/javascript" src="scripts/utilities.js?" ></script>
<script type="text/javascript" src="scripts/ajaxCalls.js?" ></script>
<script type="text/javascript">
var validator = new FieldValidator();
validator.setRequiredFields( new Array("quantity") );

var supportSTNumber = <%=supportSTNumber%>;
var supportMothersName = <%=supportMothersName%>;
var supportSubjectNumber = <%=supportSubjectNumber%>;
var supportNationalID = <%=supportNationalID%>;
var supportLabNumber = <%=supportLabNumber%>;
var patientSelectID;
var patientInfoHash = [];
var patientChangeListeners = [];
var localDB = <c:out value="${localDBOnly}" />;
var newSearchInfo = false;


function checkFieldInt(field) {
	if (isNaN(field.value) || field.value.indexOf(".") > -1) {
		validator.setFieldValidity(false, field.id);
		selectFieldErrorDisplay(false, field);
		alert("<spring:message code='siteInfo.number.nonnumber'/>");
	} else if (parseInt(field.value) <= 0) {
		validator.setFieldValidity(false, field.id);
		selectFieldErrorDisplay(false, field);
		alert("<spring:message code='siteInfo.number.invalidnumber'/>");
	} else {
		validator.setFieldValidity(true, field.id);
		selectFieldErrorDisplay(true, field);
	}
	if (validator.isAllValid()) {
		enablePrint();
	} else {
		disablePrint();
	}
}

//hardcoded to enable the order print as it is only field
function enablePrint() {
	document.getElementById("orderPrintButton").disabled = false;
}

//hardcoded to disable the order print as it is only field
function disablePrint() {
	document.getElementById("orderPrintButton").disabled = true;
}

//search patients using labNo
function searchPatients() {
    var labNumber = jQuery("#searchValue").val();
    var lastName = "";
    var firstName = "";
    var STNumber = "";
    var subjectNumber = "";
    var nationalID = "";

	newSearchInfo = false;
    jQuery("#resultsDiv").hide();
    jQuery("#barcodeArea").hide();
    jQuery("#searchLabNumber").val('');
    jQuery("#searchLabNumber").val(labNumber);

	patientSearch(lastName, firstName, STNumber, subjectNumber, nationalID, labNumber, "", "", "", false, processSearchSuccess);

}

function processSearchSuccess(xhr) {
	//alert( xhr.responseText );
	var formField = xhr.responseXML.getElementsByTagName("formfield").item(0);
	var message = xhr.responseXML.getElementsByTagName("message").item(0);
	var table = $("searchResultTable");

	clearTable(table);
	clearPatientInfoCache();

	if( message.firstChild.nodeValue == "valid" ) {
		$("noPatientFound").hide();
		$("searchResultsDiv").show();
		var resultNodes = formField.getElementsByTagName("result");
		for( var i = 0; i < resultNodes.length; i++ ) {
			addPatientToSearch( table, resultNodes.item(i) );
		}

		<c:if test="${patientSearch.loadFromServerWithPatient}" >
		if( resultNodes.length == 1 ){
			handleSelectedPatient();
		}
		</c:if>
	} else {
		$("searchResultsDiv").hide();
		$("noPatientFound").show();
		selectPatient( null );
	}
}

function clearSearchResultTable() {
	var table = $("searchResultTable");
	clearTable(table);
	clearPatientInfoCache();
}

function clearTable(table){
	var rows = table.rows.length - 1;
	while( rows > 0 ){
		table.deleteRow( rows-- );
	}
}

function clearPatientInfoCache(){
	patientInfoHash = [];
}

function addPatientToSearch(table, result ){
	var patient = result.getElementsByTagName("patient")[0];
	var firstName = getValueFromXmlElement( patient, "first");
	var lastName = getValueFromXmlElement( patient, "last");
	var gender = getValueFromXmlElement( patient, "gender");
	var DOB = getValueFromXmlElement( patient, "dob");
	var stNumber = getValueFromXmlElement( patient, "ST");
	var subjectNumber = getValueFromXmlElement( patient, "subjectNumber");
	var nationalID = getValueFromXmlElement( patient, "nationalID");
	var mother = getValueFromXmlElement( patient, "mother");
	var pk = getValueFromXmlElement( result, "id");
	var dataSourceName = getValueFromXmlElement( result, "dataSourceName");

	var row = createRow( table, firstName, lastName, gender, DOB, stNumber, subjectNumber, nationalID, mother, pk, dataSourceName );
	addToPatientInfo( firstName, lastName, gender, DOB, stNumber, subjectNumber, nationalID, mother, pk );
	if( row == 1 ){
		patientSelectID = pk;
		$("sel_1").checked = "true";
		selectPatient( pk );
	}
}

function getValueFromXmlElement( parent, tag ){
	var element = parent.getElementsByTagName( tag ).item(0);
	return element ? element.firstChild.nodeValue : "";
}

function createRow(table, firstName, lastName, gender, DOB, stNumber, subjectNumber, nationalID, mother, pk,  dataSourceName){
		var row = table.rows.length;
		var newRow = table.insertRow(row);
		newRow.id = "_" + row;
		var cellCounter = -1;
		var selectionCell = newRow.insertCell(++cellCounter);
		if( !localDB){
			var dataSourceCell = newRow.insertCell(++cellCounter);
			dataSourceCell.innerHTML = nonNullString( dataSourceName );
		}
		var lastNameCell = newRow.insertCell(++cellCounter);
		var firstNameCell = newRow.insertCell(++cellCounter);
		var genderCell = newRow.insertCell(++cellCounter);
		var dobCell = newRow.insertCell(++cellCounter);
		var motherCell = supportMothersName ? newRow.insertCell(++cellCounter) : null;
		var stCell = supportSTNumber ? newRow.insertCell(++cellCounter) : null;
		var subjectNumberCell = supportSubjectNumber ? newRow.insertCell(++cellCounter) : null;
		var nationalCell = supportNationalID ? newRow.insertCell(++cellCounter) : null;
		selectionCell.innerHTML = getSelectionHtml( row, pk );
		lastNameCell.innerHTML = nonNullString( lastName );
		firstNameCell.innerHTML = nonNullString( firstName );
		genderCell.innerHTML = nonNullString( gender );
		if( supportSTNumber){stCell.innerHTML = nonNullString( stNumber );}
		if( supportSubjectNumber){subjectNumberCell.innerHTML = nonNullString( subjectNumber );}
		if( supportNationalID){nationalCell.innerHTML = nonNullString( nationalID );}

		dobCell.innerHTML = nonNullString( DOB );
		if(supportMothersName){motherCell.innerHTML = nonNullString( mother );}

		return row;
}

function getSelectionHtml( row, key){
	return "<input name='selPatient' id='sel_" + row + "' value='" + key + "' onclick='selectPatient(this.value)' type='radio'>";
}

function /*String*/ nonNullString( target ){
	return target == "null" ? "" : target;
}

function addToPatientInfo( firstName, lastName, gender, DOB, stNumber, subjectNumber, nationalID, mother, pk ){
	var info = [];
	info["first"] = nonNullString( firstName );
	info["last"] = nonNullString( lastName );
	info["gender"] = nonNullString( gender );
	info["DOB"] = nonNullString( DOB );
	info["ST"] = nonNullString( stNumber );
	info["subjectNumber"] = nonNullString( subjectNumber );
	info["national"] = nonNullString( nationalID );
	info["mother"] = nonNullString( mother );

	patientInfoHash[pk] = info;
}


function selectPatient( patientID ){
    var i;
	if( patientID ){
		patientSelectID = patientID;
		var info = patientInfoHash[patientID];
		for(i = 0; i < patientChangeListeners.length; i++){
			patientChangeListeners[i](info["first"],info["last"],info["gender"],info["DOB"],info["ST"],info["subjectNumber"],info["national"],info["mother"], patientID);
		}
	} else {
		for(i = 0; i < patientChangeListeners.length; i++){
			patientChangeListeners[i]("","","","","","","","", null);
		}
	}
}

function /*void*/ addPatientChangedListener( listener ){
	patientChangeListeners.push( listener );
}


function /*void*/ handleEnterEvent( ){
		if( newSearchInfo ){
			searchPatients();
		}
		return false;
}

function /*void*/ dirtySearchInfo(e){ 
	var code = e ? e.which : window.event.keyCode;
	if( code != 13 ){
		newSearchInfo = true; 
	}
}

function /*void*/ doNothing(){ 
}

function enableSearchButton(eventCode){
    var valueElem = jQuery("#searchValue");
    var searchButton = jQuery("#searchButton");
    if( valueElem.val()){
        searchButton.removeAttr("disabled");
        if( eventCode == 13 ){
            searchButton.click();
        }
    } else {
        searchButton.attr("disabled", "disabled");
    }
    valueElem.attr("maxlength","<%=Integer.toString(AccessionNumberUtil.getMaxAccessionLength())%>");
    
}

function handleSelectedPatient(){
    var accessionNumber = jQuery("#searchValue").val();

    $("searchResultsDiv").style.display = "none";
    var form = document.getElementById("mainForm");
    form.action = '${form.formAction}' + "?";
    if( !(typeof requestType === 'undefined') ){
        form.action += "&type=" + requestType;
    }
    jQuery("#accessionNumber").val(jQuery("#searchValue").val());
    jQuery("#patientId").val(patientSelectID);
    
    form.submit();
}

function printBarcode(button) {
	var labNo = document.getElementsByName('accessionNumber')[0].value;
	var patientId = document.getElementsByName('patientId')[0].value;
	var type = "";
	var quantity = "";
	if (confirm("<%=MessageUtil.getMessage("barcode.message.reprint.confirmation")%>")) {
        if (button.id == "defaultPrintButton") {
        type = "default";
        } else if (button.id == "orderPrintButton") {
        type = "order";
        quantity = document.getElementById('quantity').value;
        } else if (button.id == "printBlankBarcodeButton") {
        type = "blank";
        } else {
        type = "specimen";
        labNo = button.id;
        quantity = 1;
        }
        jQuery("#searchLabNumber").val('');
        document.getElementById("ifbarcode").src = 'LabelMakerServlet?labNo=' + labNo + '&type='
                + type + '&patientId=' + patientId + '&quantity=' + quantity;
        document.getElementById("barcodeArea").show();
        }
    }

    function checkPrint() {
        var disableButton = false;
        if (document.getElementById('labelType').selectedIndex == 0) {
        disableButton = true;
        } else if (!document.getElementById('quantity').value) {
        disableButton = true;
        }
        document.getElementById('printBarcodeButton').disabled = disableButton;
    }

    function finish() {
        window.location = "Dashboard";
    }
</script>

<!-- new for pre-printing -->
<jsp:include page="${prePrintingFragment}"/>

<!-- end new for pre-printing -->

<input type="hidden" id="searchLabNumber">
<form:hidden path="accessionNumber"/>
<form:hidden path="patientId"/>

<div id="PatientPage" class=" patientSearch" style="display:inline;" >

	<h2><spring:message code="sample.entry.search.barcode"/></h2>
    <c:if test="${!empty warning}">
        <h3 class="important-text"><spring:message code="order.modify.search.warning" /></h3>
    </c:if>
    		
   	<spring:message code="barcode.print.search.accessionnumber"/>:
	<spring:message code="sample.search.scanner.instructions"/>
    <input size="35"
           maxlength="120"
           id="searchValue"
           class="patientSearch"
           placeholder='<%=MessageUtil.getMessage("label.select.search.here")%>'
           type="text"
           oninput="enableSearchButton(event.which);"
           tabindex="2"/>

    <input type="button"
           name="searchButton"
           class="patientSearch"
           value="<%= MessageUtil.getMessage("label.patient.search")%>"
           id="searchButton"
           onclick="searchPatients()"
           disabled="disabled" >

	<div id="noPatientFound" align="center" style="display:none" >
		<h1><spring:message code="patient.search.not.found"/></h1>
	</div>
	<div id="searchResultsDiv" style="display:none;">
		<c:choose>
			<c:when test="${localDBOnly == 'false' }">
			<table id="searchResultTable" style="width:90%">
				<tr>
					<th width="2%"></th>
					<th width="10%" >
						<spring:message code="patient.data.source" />
					</th>
			</c:when>
			<c:otherwise>
			<table id="searchResultTable" width="70%">
				<tr>
					<th width="2%"></th>
			</c:otherwise>
		</c:choose>
				<th width="18%">
					<spring:message code="patient.epiLastName"/>
				</th>
				<th width="15%">
					<spring:message code="patient.epiFirstName"/>
				</th>
				<th width="5%">
					<spring:message code="patient.gender"/>
				</th>
				<th width="11%">
					<spring:message code="patient.birthDate"/>
				</th>
				<% if( supportMothersName ){ %>
				<th width="20%">
					<spring:message code="patient.mother.name"/>
				</th>
				<% } if(supportSTNumber){ %>
				<th width="12%">
					<spring:message code="patient.ST.number"/>
				</th>
				<% } %>
				<% if(supportSubjectNumber){ %>
				<th width="12%">
					<spring:message code="patient.subject.number"/>
				</th>
				<% } %>
				<% if(supportNationalID){ %>
				<th width="12%">
                    <%=MessageUtil.getContextualMessage("patient.NationalID") %>
                </th>
                <% } %>
			</tr>
		</table>
		</div>
		<br/>
		<c:if test="${!empty form.accessionNumber}">
		<table  id="patientInfo" width="50%">
			<tr>
				<th>
					<spring:message code="sample.entry.patient"/>
				</th>
				<th>
					<spring:message code="patient.birthDate"/>
				</th>
				<th>
					<spring:message code="patient.gender"/>
				</th>
				<th>
					<spring:message code="patient.NationalID"/>
				</th>
			</tr>
			<tr>
				<td>
					<c:out value="${patientName}"/>&nbsp;
				</td>
				<td>
					<c:out value="${dob}"/>&nbsp;
				</td>
				<td>
					<c:out value="${gender}"/>				
				</td>
				<td>
					<c:out value="${nationalId}"/>
				</td>
			</tr>
		</table>
		<h2><spring:message code="barcode.print.section.set"/></h2>
        <spring:message code="barcode.print.set.instruction"/>
        
        <spring:message var="printButton" code='barcode.print.set.button'/>
        <input type="button" 
        	id="defaultPrintButton"
        	value="${printButton }"
        	onclick="printBarcode(this);"/>
        <h2><spring:message code="barcode.print.section.individual"/></h2>
        <table width="50%">
        	<tr>
        		<th>
        			<spring:message code="barcode.print.individual.type"/>
        		</th>
        		<th>
        			<spring:message code="barcode.print.individual.labnumber"/>
        		</th>
        		<th>
        			<spring:message code="barcode.print.individual.info"/>
        		</th>
        		<th>
        			<spring:message code="barcode.print.individual.number"/>
        		</th>
        	<tr>
	        	<td>
	        		<spring:message code="barcode.label.type.order"/>
	        	</td>
        		<td>
        			<c:out value="${form.accessionNumber}" />
        		</td>
        		<td>
        		</td>
        		<td>
        			<input type="text"
        				id="quantity"
        				value="1"
        				onchange="checkFieldInt(this)" >
        		</td>
        		<td>
        		<spring:message var="orderPrintButton" code='barcode.print.individual.button'/>
        			<input type="button" 
			        	id="orderPrintButton"
			        	value="${orderPrintButton}"
			        	onclick="printBarcode(this);">
        		</td>
        	</tr>
        	<c:forEach var="test" items="${existingTests}">
        		<tr>
        			<td>
        				<spring:message code="barcode.label.type.specimen"/>
        			</td>
        			<td>
        				<c:out value="${test.accessionNumber}" />
        				
        			</td>
        			<td>
        				<c:out value="${test.sampleType}" />
        			</td>
        			<td>
        				1
        			</td>
        			<td><spring:message var="printIndividualButton" code='barcode.print.individual.button'/>
        				<input type="button"
        					id='<c:out value="${test.accessionNumber}" />'
        					value="${printIndividualButton }"
        					onclick="printBarcode(this);">
        			</td>
        		</tr>
        	</c:forEach>
        </table>
		</div>
	
	</c:if>
        
	<div style="display:none;" id="barcodeArea">
		<h2><spring:message code="barcode.common.section.barcode.header"/></h2>
		<iframe  src="about:blank" id="ifbarcode" width="75%" height="300px"></iframe>
	</div>
	
	
