<%@ page language="java" contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants,
				org.openelisglobal.common.formfields.FormFields,
                org.openelisglobal.common.formfields.FormFields.Field,
				org.openelisglobal.sample.util.AccessionNumberUtil,
                org.openelisglobal.common.provider.validation.NonConformityRecordNumberValidationProvider,
                org.openelisglobal.common.services.PhoneNumberService,
                org.openelisglobal.common.util.DateUtil,
                org.openelisglobal.internationalization.MessageUtil, 
                org.openelisglobal.common.util.Versioning,
                org.openelisglobal.qaevent.valueholder.retroCI.QaEventItem,
                org.openelisglobal.common.util.ConfigurationProperties"%>

<%@ page isELIgnored="false"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<%
boolean useProject = FormFields.getInstance().useField(Field.Project);
boolean useSiteList = FormFields.getInstance().useField(Field.NON_CONFORMITY_SITE_LIST);
boolean useSubjectNo = FormFields.getInstance().useField(Field.QASubjectNumber);
boolean useNationalID = FormFields.getInstance().useField(Field.NationalID);
%>

<script type="text/javascript" src="scripts/utilities.js?"></script>
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
		new Array('collectionDateId','receptionDateId','organizationId',
				'qaEventTypeId','sampleType'));

	var dirty = false;

	var confirmNewTypeMessage = "<spring:message code='nonConformant.confirm.newType.message'/>";

	function setMyCancelAction() {
		setAction(document.getElementById("mainForm"), 'Cancel', 'no', '');
	}
	function doNothing() {

	}

	function processAccessionSuccess(xhr) {
		//alert(xhr.responseText);
		var message = xhr.responseXML.getElementsByTagName("message").item(0);
		var success = message.firstChild.nodeValue == "valid";

		var searchButton = $("searchButtonId");

		if (!success) {
			alert(message.firstChild.nodeValue);
			searchButton.disable();
		} else {
			searchButton.enable();
			searchButton.focus();
		}
	}

	/**
	 * make the text of the blank option for sample type say "all types"
	 */
	function tweekSampleTypeOptions() {
		var eventsTable = $('qaEventsTable');
		if (eventsTable === null) {
			return;
		}
		var fields = eventsTable.getElementsBySelector(".typeOfSample");
		fields
				.each(function(field) {
					field.options[1].text = "<spring:message code='nonConformant.allSampleTypesText'/>";
				});
	}

	function enableRow(row, newIndicator) {
		var isNew = newIndicator == "NEW";
		var elements = row.getElementsBySelector(".qaEventElement");

		elements.each(function(field) {
			field = $(field);
			if (isNew || field.id.startsWith("note")) {
				field.enable();
				field.removeClassName("readOnly");
			}
		});

		/* 	elements = row.getElementsBySelector(".requiredField");
		 elements.each(function(field) {
		 fieldValidator.addRequiredField(field.id);
		 });
		 */
	}
	function disableRow(row, userAdded) {
		var elements = row.getElementsBySelector(".qaEventElement");
		elements.each(function(field) {
			field = $(field);
			if (!(userAdded && field.id.startsWith("note"))) {
				field.disable();
				field.addClassName("readOnly");
			}
		});

		/* elements = row.getElementsBySelector(".requiredField");
		elements.each(function(field) {
			fieldValidator.removeRequiredField(field.id);
		}); */
	}


	function checkValidTime(timeElement) {
		var valid = !timeElement.value || checkTime(timeElement.value);
		selectFieldErrorDisplay(valid, timeElement);
		fieldValidator.setFieldValidity(valid, timeElement.id);
		setSave();
	}

	function enableDisableIfChecked(removeCheckBox) {
		var row = removeCheckBox.parentNode.parentNode;
		var idFieldValue = row.getElementsBySelector(".id")[0].getValue();

		if (removeCheckBox.checked) {
			disableRow(row, false);
		} else {
			enableRow(row, idFieldValue);
		}

		setSave();
	}

	function setSave() {
		var saveButton = $("saveButtonId");
		var validToSave = fieldValidator.isAllValid();

		//all this crap is to make sure there is not an enabled new row that has no value for the required field then none of the 
		//other fields have values		
		if (validToSave) {
			jQuery("#qaEventsTable tbody tr")
					.each(
							function(rowIndex, rowElement) {
								var jqRow = jQuery(rowElement);
								if (validToSave && jqRow.is(":visible")) {
									//if row is visible and the required field is blank make sure no other field has a value
									if (!(jqRow.find(".qaEventEnable")
											.is(":checked"))
											&& requiredSelectionsNotDone(jqRow)) {
										jqRow
												.find(".qaEventElement")
												.each(
														function(index, element) {
															var cellValue = jQuery(
																	element)
																	.val();
															if (!(cellValue.length == 0 || cellValue == "0")) {
																validToSave = false;
																return;
															}
														});
									}
								}
							});
		}

		if (saveButton) {
			saveButton.disabled = !validToSave;
		}
	}

	function requiredSelectionsNotDone(jqRow) {
		var done = true;

		jqRow.find(".requiredField").each(function(index, element) {
			if (element.value == 0) {
				done = false;
				return;
			}
		});

		return !done;
	}

	function validatePhoneNumber(phoneElement) {
		validatePhoneNumberOnServer(phoneElement, processPhoneSuccess);
	}

	function processPhoneSuccess(xhr) {
		//alert(xhr.responseText);

		var formField = xhr.responseXML.getElementsByTagName("formfield").item(
				0);
		var message = xhr.responseXML.getElementsByTagName("message").item(0);
		var success = false;

		if (message.firstChild.nodeValue == "valid") {
			success = true;
		}
		var labElement = formField.firstChild.nodeValue;
		selectFieldErrorDisplay(success, $(labElement));
		fieldValidator.setFieldValidity(success, labElement);

		if (!success) {
			alert(message.firstChild.nodeValue);
		}

		setSave();
	}
</script>
<table>
	<tr>
		<td><%=MessageUtil.getContextualMessage("nonconformity.collectionDate")%>&nbsp;
			<span style="font-size: xx-small;"><%=DateUtil.getDateUserPrompt()%></span>
			:<span class="requiredlabel">*</span></td>
		<td><form:input path="collectionDateForDisplay" id="collectionDateId" cssClass="requiredField"
				maxlength="10" onchange="checkValidDate(this);" onkeyup="addDateSlashes(this, event);"/></td>
	</tr>
	<tr>
		<td><%=MessageUtil.getContextualMessage("nonconformity.receptionDate")%>&nbsp;
			<span style="font-size: xx-small;"><%=DateUtil.getDateUserPrompt()%></span>
			:<span class="requiredlabel">*</span></td>
		<td><form:input path="receptionDateForDisplay" id="receptionDateId" cssClass="requiredField"
				maxlength="10" onchange="checkValidDate(this);" onkeyup="addDateSlashes(this, event);"/></td>
	</tr>
	<tr>
		<form:hidden path="project" />
		<td><spring:message code="label.study" /> :</td>
		<c:if test="${empty form.project}">
			<td><form:select path="projectId" onchange='makeDirty();'>
					<option value="0"></option>

					<form:options items="${form.projects}" itemLabel="localizedName"
						itemValue="id" />
				</form:select></td>
		</c:if>
		<c:if test="${not empty project}">
			<td><c:out value="${form.project}" /></td>
		</c:if>
	</tr>
	<tr>
		<td><spring:message code="patient.subject.number" /> :</td>
		<td><form:input path="subjectNo" onchange="makeDirty();" /></td>
	</tr>
	<tr>
		<td><%=MessageUtil.getContextualMessage("sample.id")%> :</td>

		<td><form:input path="labNo" onchange="makeDirty();" /></td>
	</tr>
	<tr>
		<td><%=MessageUtil.getContextualMessage("sample.entry.project.ARV.centerName")%>
			: <span class="requiredlabel">*</span></td>
		<td colspan="2"><form:select path="organizationId"
				id="organizationId" cssClass="centerCodeClass"
				onchange="">
				<option value=" "></option>
				<form:options items="${form.siteList}"
					itemLabel="value" itemValue="id" />
			</form:select></td>
	</tr>
	<tr>
		<td><%=MessageUtil.getContextualMessage("patient.project.nameOfRequestor")%>:</td>
		<td><form:input path="doctor" onchange="makeDirty();" /></td>
	</tr>
	<tr>
		<td><%=MessageUtil.getContextualMessage("patient.project.nameOfSampler")%>:</td>
		<td><form:input path="samplerName" onchange="makeDirty();" /></td>
	</tr>
	<tr>
		<td style="width: 22%"><spring:message
				code="label.refusal.reason" /><span class="requiredlabel">*</span>
		</td>
		<td><form:select path="qaEventTypeId" id='qaEventTypeId'
				name="qaEvent" cssClass="requiredField" indexed="true"
				style="width: 99%" onchange='makeDirty();'>
				<option value="0"></option>
				<form:options items="${form.qaEventTypes}" itemLabel="value"
					itemValue="id" />
			</form:select></td>
	</tr>
	<tr>
		<td><spring:message code="label.sampleType" /> <span
			class="requiredlabel">*</span></td>
		<td><form:select path="typeOfSampleId" id='sampleType'
				name="qaEvents" cssClass="typeOfSample requiredField"
				onchange='makeDirty();'>
				<option value="0"></option>
				<option value="-1"></option>
				<form:options items="${form.typeOfSamples}" itemLabel="value"
					itemValue="id" />
			</form:select></td>
	</tr>
	<tr>
		<td style="width: 11%"><%=MessageUtil.getContextualMessage("nonconformity.section")%>:
		</td>
		<td><form:select path="sectionId" name="sections" id='sectionId'
				cssClass="qaSection" disabled="false" onchange='makeDirty();'>
				<option></option>
				<form:options items="${form.sections}" itemLabel="localizedName"
					itemValue="nameKey" />
			</form:select></td>
	</tr>
	<tr>
		<td><%=MessageUtil.getContextualMessage("label.biologist")%>:</td>
		<td><form:input path="biologist" onchange="makeDirty();" /></td>
	</tr>
	<tr>
		<td><%=MessageUtil.getContextualMessage("label.comments")%>:</td>
		<td><form:textarea path="comment" cssClass="qaEventComment"
				id='commentNote' name="comment" onchange="makeDirty();" /></td>

	</tr>
</table>

<hr />

<script type="text/javascript">
	//all methods here either overwrite methods in tiles or all called after they are loaded

	function makeDirty() {
		dirty = true;

		if (typeof (showSuccessMessage) != 'undefined') {
			showSuccessMessage(false);
		}
		// Adds warning when leaving page if content has been entered into makeDirty form fields
		function formWarning() {
			return "<spring:message code="banner.menu.dataLossWarning"/>";
		}
		window.onbeforeunload = formWarning;

		setSave();
	}

	function savePage() {

		var form = document.getElementById("mainForm");

		window.onbeforeunload = null; // Added to flag that formWarning alert isn't needed.
		form.action = "SampleRejection";
		form.submit();
	}

	// Moving autocomplete to end - needs to be at bottom for IE to trigger properly
	jQuery(document).ready(function() {
		var dropdown = jQuery("select#site");
		jQuery('.centerCodeClass').select2();

		autocompleteResultCallBack = function(selectId, textValue) {
			siteListChanged(textValue);
			makeDirty();
			setSave();
		};
	});
	
</script>
