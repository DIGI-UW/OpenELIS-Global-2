<%@page import="org.openelisglobal.common.action.IActionConstants"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.formfields.FormFields,
	org.openelisglobal.sample.util.AccessionNumberUtil,
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

<script type="text/javascript" src="scripts/utilities.js?"></script>
<script type="text/javascript" src="scripts/tbUtilities.js"></script>
<script type="text/javascript" src="scripts/additional_utilities.js"></script>
<script type="text/javascript" src="scripts/jquery.asmselect.js?"></script>
<script type="text/javascript" src="scripts/ajaxCalls.js?"></script>
<script type="text/javascript" src="scripts/laborder.js?"></script>

<link rel="stylesheet" type="text/css" href="css/jquery.asmselect.css?" />
<script type="text/javascript" src="select2/js/select2.min.js"></script>
<link rel="stylesheet" type="text/css" href="select2/css/select2.min.css">
<script type="text/javascript" src="scripts/jquery_ui/jquery-ui.min.js"></script>
<link rel="stylesheet" type="text/css" href="scripts/jquery_ui/jquery-ui.min.css"/>
<link rel="stylesheet" type="text/css" href="scripts/jquery_ui/jquery-ui.theme.min.css"/>


<script type="text/javascript">
fieldValidator = new FieldValidator();
fieldValidator.setRequiredFields(
		new Array('labNo','requestDate','receivedDate','referringSiteCode',
				'lastNameID','dateOfBirthID','genderID',
				'tbSpecimenNature','tbOrderReasons','tbDiagnosticMethods'));
		
function /*void*/setSaveButton() {
	var validToSave = fieldValidator.isAllValid();
	$("saveButtonId").disabled = !validToSave;

}
	function showHideSection(button, targetId) {
		targetId = targetId + button.name
		if (button.value == "+") {
			showSection(button, targetId);
		} else {
			hideSection(button, targetId);
		}
	}

	function showSection(button, targetId) {
		jQuery("#" + targetId).show();
		button.value = "-";
	}

	function hideSection(button, targetId) {
		jQuery("#" + targetId).hide();
		button.value = "+";
	}

	function toggleField(toShow, targetId) {
		if (toShow) {
			jQuery("#" + targetId).show();
			fieldValidator.addRequiredField(targetId.replace('Row', ''));
		} else {
			jQuery("#" + targetId.replace('Row', '')).val(null).trigger(
					'change');
			fieldValidator.removeRequiredField(targetId.replace('Row', ''));
			jQuery("#" + targetId).hide();
		}
	}

	function toggleOrderReasons() {
		var elm = jQuery("#tbOrderReasons");
		toggleField(elm[0].selectedIndex === 1, "tbDiagnosticReasonsRow");
		toggleField(elm[0].selectedIndex === 2, "tbFollowupReasonsRow");
		toggleField(elm[0].selectedIndex === 2, "tbSubjectNumberRow");
		setOrderModified();
	}
	
	function toggleTBFollowupPeriodLine() {
		var elm = jQuery("#tbFollowupReasons");
		toggleField(elm[0].selectedIndex === 1, "tbFollowupPeriodLine1Row");
		toggleField(elm[0].selectedIndex === 2, "tbFollowupPeriodLine2Row");
		setOrderModified();
	}
	
	function toggleTBSampleAspects() {
		var elm = jQuery("#tbDiagnosticMethodsRow");
		var selectedIndices = jQuery("#tbDiagnosticMethodsRow :selected").map((_, e) => e.index).get();
		toggleField(selectedIndices.includes(2), "tbAspectsRow");
		setOrderModified();
	}
	
	
	//
	//laborder
	//
	function checkAccessionNumber(accessionNumber) {
        //check if empty
        if (!fieldIsEmptyById("labNo")) {
            validateAccessionNumberOnServer(false, false, accessionNumber.id, accessionNumber.value, processAccessionSuccess, null);
        }
        else {
             selectFieldErrorDisplay(false, $("labNo"));
        }

        setCorrectSave();
    }
	
	function validateSubjectNumber(field){
		 const tbNumberFormat = /^[0-9]{2}-[0-9]{5}$/;
		 if(field.value){
			  if(tbNumberFormat.test(field.value)){
				  field.classList.remove("error");
				  setCorrectSave();
			  }
			  else{
				  field.classList.add("error");
			  }
		 }
	}
	
	function handleAgeChange(){
		var ageYears = jQuery("#ageYears").val();

		if( ageYears.blank()){
			$("dateOfBirthID").value = null;
		} else {
			
			var date = new Date();
			if ( !ageYears.blank() ) {
				date.setFullYear( date.getFullYear() - parseInt(ageYears));
			}
			
			var day = "01";
			var month = "01";
			var year = "xxxx";

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
		jQuery("#dateOfBirthID").trigger('change');
	}

    function processAccessionSuccess(xhr) {
        //alert(xhr.responseText);
        var formField = xhr.responseXML.getElementsByTagName("formfield").item(0);
        var message = xhr.responseXML.getElementsByTagName("message").item(0);
        var success = false;

        if (message.firstChild.nodeValue == "valid") {
            success = true;
        }
        var labElement = formField.firstChild.nodeValue;
        selectFieldErrorDisplay(success, $(labElement));

        if (!success) {
            alert(message.firstChild.nodeValue);
        }

        setCorrectSave();
    }

    function setCorrectSave(){
        if( window.setSave){
            setSave();
        }else if(window.setSaveButton){
            setSaveButton();
        }
    }

    function getNextAccessionNumber() {
        generateNextScanNumber(processScanSuccess);
    }

    function processScanSuccess(xhr) {
        //alert(xhr.responseText);
        var formField = xhr.responseXML.getElementsByTagName("formfield").item(0);
        var returnedData = formField.firstChild.nodeValue;

        var message = xhr.responseXML.getElementsByTagName("message").item(0);

        var success = message.firstChild.nodeValue == "valid";

        if (success) {
            $("labNo").value = returnedData;

        } else {
            alert("<%= MessageUtil.getMessage("error.accession.no.next") %>");
            $("labNo").value = "";
        }

        selectFieldErrorDisplay(success, $("labNo"));
        setValidIndicaterOnField(success, "labNo");

        setCorrectSave();
    }

    function processCodeSuccess(xhr) {
        //alert(xhr.responseText);
        var code = xhr.responseXML.getElementsByTagName("code").item(0);
        var success = xhr.responseXML.getElementsByTagName("message").item(0).firstChild.nodeValue == "valid";

        if (success) {
            jQuery("#requesterCodeId").val(code.getAttribute("value"));
        }
    }
    function setOrderModified(){
        jQuery("#orderModified").val("true");
        orderChanged = true;
        if( window.makeDirty ){ makeDirty(); }

        setCorrectSave();
    }
    
    function  /*void*/ processValidateEntryDateSuccess(xhr){

        //alert(xhr.responseText);
        
        var message = xhr.responseXML.getElementsByTagName("message").item(0).firstChild.nodeValue;
        var formField = xhr.responseXML.getElementsByTagName("formfield").item(0).firstChild.nodeValue;

        var isValid = message == "<%=IActionConstants.VALID%>";

        //utilites.js
        selectFieldErrorDisplay( isValid, $(formField));
        setSampleFieldValidity( isValid, formField );
        setSaveButton();

        if( message == '<%=IActionConstants.INVALID_TO_LARGE%>' ){
            alert( '<spring:message code="error.date.inFuture"/>' );
        }else if( message == '<%=IActionConstants.INVALID_TO_SMALL%>' ){
            alert( '<spring:message code="error.date.inPast"/>' );
        }
    }
    
    function checkValidEntryDate(date, dateRange, blankAllowed)
    {   
        if((!date.value || date.value == "") && !blankAllowed){
            setSaveButton();
            return;
        } else if ((!date.value || date.value == "") && blankAllowed) {
            setSampleFieldValid(date.id);
            setValidIndicaterOnField(true, date.id);
            return;
        }
        if( !dateRange || dateRange == ""){
            dateRange = 'past';
        }
        isValidDate( date.value, processValidateEntryDateSuccess, date.id, dateRange );
    }
    
	function savePage__(action) {
		window.onbeforeunload = null; 
		var form = document.getElementById("mainForm");
		if (action == null) {
			action = "MicrobiologyTb"
		}
		form.action = action;
		form.submit();
	}
</script>



<div id="tb_container">
	<%=MessageUtil.getContextualMessage("referring.order.number")%>:
	<form:input id="externalOrderNumber" path="externalOrderNumber"
		onchange="checkOrderReferral();makeDirty();" />
	<input type="button" name="searchExternalButton"
		value='<%=MessageUtil.getMessage("label.button.search")%>'
		onclick="checkOrderReferral();makeDirty();">
	<%=MessageUtil.getContextualMessage("referring.order.not.found")%>
	<hr style="width: 100%; height: 1px" />
	<br />
	<form:hidden path="modified" id="orderModified"/>
    <form:hidden path="sampleId" id="sampleId"/>
    
    <!--  -->
    <div id=orderSearchSection>
		<input type="button" name="showHide" value='-'
			onclick="showHideSection(this, 'orderSearch');" id="orderSearchId">
		<%=MessageUtil.getContextualMessage("sample.entry.search.label") %>
		<table id="orderSearchshowHide" style="display:none">
			<tr>
				<td style="width: 35%"><%=MessageUtil.getContextualMessage("quick.entry.accession.number")%>:</td>
				<td style="width: 65%"><form:input path="labnoForSearch"
						maxlength='<%=Integer.toString(AccessionNumberUtil.getMaxAccessionLength())%>'
						onchange="" cssClass="text" id="searchByLabNo" />
					<input type="button" name="searchButton" class="patientSearch" value="<%= MessageUtil.getMessage("label.patient.search")%>"
           			id="searchButton" onclick="searchOrder()">
				</td>
			</tr>
		</table>
	</div>
    
    <hr style="width: 100%; height: 1px" />
	<br />
    
   
	<div id=orderEntrySection>
		<input type="button" name="showHide" value='-'
			onclick="showHideSection(this, 'orderDisplay');" id="orderSectionId">
		<%=MessageUtil.getContextualMessage("sample.entry.order.label")%>
		<span class="requiredlabel">*</span>
		<table id="orderDisplayshowHide">
			<tr>
				<td style="width: 35%"><%=MessageUtil.getContextualMessage("quick.entry.accession.number")%>
					:<span	class="requiredlabel">*</span></td>
				<td style="width: 65%"><form:input path="labNo"
						maxlength='<%=Integer.toString(AccessionNumberUtil.getMaxAccessionLength())%>'
						onchange="checkAccessionNumber(this);" cssClass="text" id="labNo" />

					<spring:message code="sample.entry.scanner.instructions"
						htmlEscape="false" /> <input type="button"
					id="generateAccessionButton"
					value='<%=MessageUtil.getMessage("sample.entry.scanner.generate")%>'
					onclick="setOrderModified();getNextAccessionNumber(); "
					class="textButton"></td>
			</tr>

			<tr>
				<td><spring:message code="sample.entry.requestDate" />: <span
					class="requiredlabel">*</span><span style="font-size: xx-small;"><%=DateUtil.getDateUserPrompt()%></span></td>
				<td><form:input path="requestDate" id="requestDate"
						cssClass="required"
						onchange="setOrderModified();checkValidEntryDate(this, 'past')"
						onkeyup="addDateSlashes(this, event);" maxlength="10" />
			</tr>
			<tr>
				<td><%=MessageUtil.getContextualMessage("quick.entry.received.date")%>
					: <span class="requiredlabel">*</span> <span
					style="font-size: xx-small;"><%=DateUtil.getDateUserPrompt()%>
				</span></td>
				<td colspan="2"><form:input path="receivedDate"
						onchange="checkValidEntryDate(this, 'past');setOrderModified();"
						onkeyup="addDateSlashes(this, event);" maxlength="10"
						cssClass="text required" id="receivedDate" /></td>
			</tr>
			<tr>
				<td><%=MessageUtil.getContextualMessage("sample.tb.reference.unit")%>
					: <span class="requiredlabel">*</span></td>
				<td colspan="2"><form:select path="referringSiteCode"
						id="referringSiteCode" cssClass="centerCodeClass" onchange="setOrderModified();">
						<option value=" "></option>
						<form:options items="${form.referralOrganizations}"
							itemLabel="value" itemValue="id" />
					</form:select></td>
			</tr>
			<tr class="provider-info-row provider-extra-info-row">
				<td><%=MessageUtil.getContextualMessage("sample.entry.provider.name")%>:
				</td>
				<td><form:input path="providerLastName" id="providerLastName"
						onchange="" /></td>

			</tr>
			<tr class="provider-info-row provider-extra-info-row">
				<td><%=MessageUtil.getContextualMessage("sample.entry.provider.firstName")%>:
				</td>
				<td><form:input path="providerFirstName" id="providerFirstName"
						onchange="" /></td>
			</tr>

			<tr class="spacerRow">
				<td>&nbsp;</td>
			</tr>
		</table>
		<hr style="width: 100%; height: 2px" />
	</div>
	<br />
	<div id=patientEntrySection>
		<input type="button" name="showHide" value='-'
			onclick="showHideSection(this, 'patientDisplay');"
			id="patientSectionId">
		<%=MessageUtil.getContextualMessage("sample.entry.patient")%>
		<span class="requiredlabel">*</span>
		<table id="patientDisplayshowHide">
			<tr>
				<td style=""><spring:message code="patient.epiLastName" /> : <span
					class="requiredlabel">*</span></td>
				<td><form:input path="patientLastName" id="lastNameID"
						onchange="setOrderModified();" /></td>
				<td style=""><spring:message code="patient.epiFirstName" /> :<span
					class="requiredlabel"></span></td>
				<td><form:input path="patientFirstName" id="firstNameID"
						onchange="" size="25" /></td>
			</tr>
			<tr>
				<td style=""><spring:message code="person.phone" />:</td>
				<td><form:input path="patientPhone" cssClass="text" onchange=""
						id="patientPhone" /></td>
				<td style=""><spring:message code="person.streetAddress" />:</td>
				<td><form:input path="patientAddress" cssClass="text" size="25"
						onchange="" id="patientAddress" /></td>
			</tr>
			<tr>
				<td style=""><spring:message code="patient.birthDate" />&nbsp;<%=DateUtil.getDateUserPrompt()%>:
					<span class="requiredlabel">*</span></td>
				<td><form:input path="patientBirthDate"
						onkeyup="addDateSlashes(this,event);"
						onchange="checkValidEntryDate(this, 'past');convertToAge(this,'ageYears');"
						id="dateOfBirthID" cssClass="text" size="20" maxlength="10"/>
					<div id="patientbirthDateMessage" class="blank"></div></td>
				<td style=""><spring:message code="patient.age" />:</td>
				<td><form:input path="patientAge"
						onchange="handleAgeChange();" id="ageYears" cssClass="text"
						size="3" maxlength="3" placeholder="years" />
					<div class="blank">
						<spring:message code="years.label" />
					</div>
					<div id="ageYearsMessage" class="blank"></div></td>
				<td style=""><spring:message code="patient.gender" />: <span
					class="requiredlabel">*</span></td>
				<td><form:select path="patientGender" id="genderID" onchange="setOrderModified();">

						<option value=" "></option>
						<form:options items="${form.genders}" itemLabel="value"
							itemValue="id" />
					</form:select></td>
			</tr>
			<tr class="spacerRow">
				<td>&nbsp;</td>
			</tr>
		</table>
		<hr style="width: 100%; height: 2px" />
	</div>
	<br />
	<div id=sampleEntrySection>
		<input type="button" name="showHide" value='-'
			onclick="showHideSection(this, 'sampleDisplay');"
			id="sampleSectionId">
		<%=MessageUtil.getContextualMessage("sample.entry.sampleList.label")%>
		<span class="requiredlabel">*</span>
		<div id="sampleDisplayshowHide">
			<table>
				<tr>
					<td><%=MessageUtil.getContextualMessage("sample.tb.specimen.nature")%>
						: <span class="requiredlabel">*</span></td>
					<td colspan="2"><form:select path="tbSpecimenNature"
							id="tbSpecimenNature" cssClass="tbSpecimenNatureClass"
							style="min-width:200px" onchange="setOrderModified();">
							<option value="">&nbsp;</option>
							<form:options items="${form.tbSpecimenNatures}" itemLabel="value"
								itemValue="id" />
						</form:select></td>
					<td></td>
				</tr>
				<tr id="tbOrderReasonsRow">
					<td><%=MessageUtil.getContextualMessage("sample.tb.order.reasons")%>
						: <span class="requiredlabel">*</span></td>
					<td colspan="2"><form:select path="tbOrderReason"
							id="tbOrderReasons" cssClass="tbOrderReasonsClass"
							style="min-width:200px" onchange="toggleOrderReasons();">
							<option value="">&nbsp;</option>
							<form:options items="${form.tbOrderReasons}" itemLabel="value"
								itemValue="id" />
						</form:select></td>
					<td></td>
				</tr>
				<tr id="tbSubjectNumberRow">
					<td style=""><spring:message code="patient.subject.tbnumber" />:
						<span class="requiredlabel">*</span></td>
					<td><form:input path="tbSubjectNumber" id="tbSubjectNumber"
							onchange="validateSubjectNumber(this, 'subjectNumber');"
							cssClass="text" /></td>
					<td></td>
					<td></td>
				</tr>
				<tr id="tbDiagnosticReasonsRow">
					<td><%=MessageUtil.getContextualMessage("sample.tb.diagnostic.reasons")%>
						: <span class="requiredlabel">*</span></td>
					<td colspan="2"><form:select path="tbDiagnosticReason"
							id="tbDiagnosticReasons" cssClass="tbDiagnosticReasonsClass"
							style="min-width:200px" onchange="setOrderModified();">
							<option value="">&nbsp;</option>
							<form:options items="${form.tbDiagnosticReasons}"
								itemLabel="value" itemValue="id" />
						</form:select></td>
					<td></td>
				</tr>
				<tr id="tbFollowupReasonsRow">
					<td><%=MessageUtil.getContextualMessage("sample.tb.followup.reasons")%>
						: <span class="requiredlabel">*</span></td>
					<td colspan="2"><form:select path="tbFollowupReason"
							id="tbFollowupReasons" cssClass="tbFollowupReasonsClass"
							onchange="toggleTBFollowupPeriodLine()">
							<option value="">&nbsp;</option>
							<form:options items="${form.tbFollowupReasons}" itemLabel="value"
								itemValue="id" />
						</form:select> <span id="tbFollowupPeriodLine1Row"> <form:select
								path="tbFollowupPeriodLine1" id="tbFollowupPeriodLine1"
								cssClass="tbFollowupPeriodLine1Class" style="min-width:100px" onchange="setOrderModified();">
								<option value="">&nbsp;</option>
								<form:options items="${form.tbFollowupPeriodsLine1}"
									itemLabel="value" itemValue="id" />
							</form:select>
					</span> <span id="tbFollowupPeriodLine2Row"> <form:select
								path="tbFollowupPeriodLine2" id="tbFollowupPeriodLine2"
								cssClass="tbFollowupPeriodLine2Class" style="min-width:100px" onchange="setOrderModified();">
								<option value="">&nbsp;</option>
								<form:options items="${form.tbFollowupPeriodsLine2}"
									itemLabel="value" itemValue="id" />
							</form:select>
					</span></td>
					<td></td>
				</tr>
				<tr id="tbDiagnosticMethodsRow">
					<td><%=MessageUtil.getContextualMessage("sample.tb.diagnostic.methods")%>:
						<span class="requiredlabel">*</span></td>
					<%-- <td colspan="2"><form:select path="newSelectedTbMethods" --%>
					<td colspan="2"><form:select path="selectedTbMethod"
							id="tbDiagnosticMethods" multiple="false"
							cssClass="tbDiagnosticMethodsClass"
							onchange="toggleTBSampleAspects();showPanelAndTests(this)" style="min-width:300px">
							<option value="">&nbsp;</option>
							<form:options items="${form.tbDiagnosticMethods}"
								itemLabel="value" itemValue="id" />
						</form:select></td>
					<td></td>
				</tr>
				<tr id="tbAspectsRow">
					<td><%=MessageUtil.getContextualMessage("sample.tb.aspects")%>
						: <span class="requiredlabel">*</span></td>
					<td colspan="2"><form:select path="tbAspect" id="tbAspects"
							cssClass="tbAspectsClass" style="min-width:200px" onchange="setOrderModified();">
							<option value="">&nbsp;</option>
							<form:options items="${form.tbAspects}" itemLabel="value"
								itemValue="id" />
						</form:select></td>
					<td></td>
				</tr>
			</table>
			<br />
			<div id="testSelections" class="testSelections">
				<table style="margin-left: 1%; width: 60%;" id="addTables">
					<tr>
						<td style="width: 30%; vertical-align: top;"><span
							class="caption"> <spring:message
									code="sample.entry.panels" />
						</span></td>
						<td style="width: 70%; vertical-align: top; margin-left: 3%;">
							<span class="caption"> <spring:message
									code="sample.entry.available.tests" />
						</span>
						</td>
					</tr>
					<tr>
						<td style="width: 30%; vertical-align: top;">
							<table style="width: 97%" id="addPanelTableContainer"
								class="table addPanelTableContainer">
								<thead><tr>
									<th style="width: 20%">&nbsp;</th>
									<th style="width: 80%"><spring:message
											code="sample.entry.panel.name" /></th>
								</tr>
								</thead>
								<tbody id="addPanelTable">
								
								</tbody>

							</table>
						</td>
						<td style="width: 70%; vertical-align: top; margin-left: 3%;">
							<table style="width: 97%" id="addTestTableContainer" class="table addTestTableContainer">
								<tr>
									<th style="width: 5%">&nbsp;</th>
									<th style="width: 50%"><spring:message
											code="sample.entry.available.test.names" /></th>
									<th style="width: 40%; display: none;" id="sectionHead">
										Section</th>
									<th style="width: 20%">&nbsp;</th>
								</tr>
								<tbody id="addTestTable"></tbody>

							</table>
						</td>
					</tr>
				</table>
			</div>
		</div>
		<hr style="width: 100%; height: 2px" />
	</div>
</div>

<br />
<script type="text/javascript">
	function pageOnLoad() {
		jQuery('.centerCodeClass').select2();
		jQuery('.tbSpecimenNatureClass').select2();
		jQuery('.tbOrderReasonsClass').select2();
		jQuery('.tbDiagnosticReasonsClass').select2();
		jQuery('.tbFollowupReasonsClass').select2();
		jQuery('.tbFollowupPeriodLine1Class').select2();
		jQuery('.tbFollowupPeriodLine2Class').select2();
		jQuery('.tbDiagnosticMethodsClass').select2();
		jQuery('.tbAspectsClass').select2();
		toggleOrderReasons();
		toggleTBFollowupPeriodLine();
		toggleTBSampleAspects();
		jQuery("#requestDate").datepicker({
			dateFormat: 'dd/mm/yy',
			yearRange: "-1:+00"
		});
		jQuery("#receivedDate").datepicker({
			dateFormat: 'dd/mm/yy',
		     changeMonth: true,
		     changeYear: true,
		     yearRange: "-1:+00"
		});
		jQuery("#dateOfBirthID").datepicker({
			dateFormat: 'dd/mm/yy',
		     changeMonth: true,
		     changeYear: true,
		     yearRange: "-120:+00",
		     maxDate: new Date(),
		});
		
		showPanelAndTests($('tbDiagnosticMethods'));
		
		hideSection(document.getElementById('orderSearchId') ,'orderSearch');
		
		setSaveButton();
	}
	
	function showPanelAndTests(input){	
		if(!input){
			return;
		}
		if(input.value){
			selectedMethod = input.value;
			let testHtml='';
			let panelHtml='';
			jQuery.get( "MicrobiologyTb/panel_test?method="+selectedMethod, function(data) {
				if(data){
					jQuery("#addPanelTable").html('');
					jQuery("#addTestTable").html('');
					 for (const [key, value] of Object.entries(data.tests)) { 
							let d = '<tr>';
							d+='<td><input type="checkbox" value="'+value.id+'" name="newSelectedTests" id="test_'+value.id+'" class="tb_test" /></td>';
							d+='<td><label for="test_'+value.id+'">'+value.name+'</label></td>';
							d+='</tr>';
							testHtml+=d;
				         } 
					 jQuery("#addTestTable").append(testHtml);
					 
					 for (const [key, value] of Object.entries(data.panels)) { 
							let d = '<tr>';
							d+='<td><input type="checkbox" value="'+value.id+'" name="testSelected" id="panel_'+value.id+'" '+ 
							'onclick="togglePanelSelected(this,\''+value.test_ids+'\')"/></td>';
							d+='<td><label for="panel_'+value.id+'">'+value.name+'</label></td>';
							d+='</tr>';
							panelHtml+=d;
				         } 
					 jQuery("#addPanelTable").append(panelHtml);
				}
				else{
					jQuery("#addPanelTable").html('');
					jQuery("#addTestTable").html('');
				}
				});
		}
		else{
			jQuery("#addPanelTable").html('');
			jQuery("#addTestTable").html('');
		}
	}
	
	function togglePanelSelected(panel,testIds){
		var testList = testIds.split(',');
			for (test of testList){
				document.getElementById("test_"+test).click();
		}
	}
	
	
</script>