<%@ page language="java" contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants,
	        org.openelisglobal.common.util.DateUtil"%>

<%@ page isELIgnored="false"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<script type="text/javascript">
	/**
	 * an object gathers all the methods about tweeking the VL form together in one place.
	 **/
	function VLProjectChecker() {

		this.idPre = "vl.";
		var specialKeys = new Array();
		specialKeys.push(8); //Backspace

		this.checkDate = function(field, blanksAllowed) {
			makeDirty();
			if (field == null)
				return; // just so we don't have to have this field on all forms, but is listed in checkAllSampleFields
			checkValidDate(field);
			checkRequiredField(field, blanksAllowed);
			compareSampleField(field.id, false, blanksAllowed);
		};

		this.checkVLRequestReason = function() {
			clearFormElements("vl.vlOtherReasonForRequest");
			this.displayedByReasonOther();
		};
		this.displayedByReasonOther = function() {
			var field = $("vl.vlReasonForRequest");
			showElements((field.selectedIndex == 5), "vl.reasonOtherRow");
		};

		this.checkInterruptedARVTreatment = function() {
			clearFormElements("vl.arvTreatmentInitDate,vl.arvTreatmentRegime,vl.currentARVTreatmentINNs0,vl.currentARVTreatmentINNs1,vl.currentARVTreatmentINNs2,vl.currentARVTreatmentINNs3");
			this.displayedByInterruptedARVTreatment();
		};

		this.displayedByInterruptedARVTreatment = function() {
			var field = $("vl.currentARVTreatment");
			showElements(
					(field.selectedIndex == 1),
					"vl.arvTreatmentInitDateRow,vl.arvTreatmentTherapRow,vl.onGoingARVTreatmentINNsRow,vl.currentARVTreatmentINNRow0,vl.currentARVTreatmentINNRow1,vl.currentARVTreatmentINNRow2,vl.currentARVTreatmentINNRow3");
		};

		this.checkVLBenefit = function() {
			clearFormElements("vl.priorVLLab,vl.priorVLValue,vl.priorVLDate");
			this.displayedByVLBenefit();
		};
		this.displayedByVLBenefit = function() {
			var field = $("vl.vlBenefit");
			showElements((field.selectedIndex == 1),
					"vl.priorVLLabRow,vl.priorVLValueRow,vl.priorVLDateRow");
		};
		function IsNumeric(field, e) {
			var keyCode = e.which ? e.which : e.keyCode
			var ret = ((keyCode >= 48 && keyCode <= 57) || specialKeys
					.indexOf(keyCode) != -1);
			document.getElementById("error").style.display = ret ? "none"
					: "inline";
			return ret;
		}
		;

		this.refresh = function() {
			this.refreshBase();
			this.displayedByVLBenefit();
			this.displayedByReasonOther();
			this.displayedByInterruptedARVTreatment();
		};

	}

	VLProjectChecker.prototype = new BaseProjectChecker();
	/// the object which knows about VL questions and which fields to show etc.
	vl = new VLProjectChecker();
</script>
<h2>
	<spring:message code="sample.entry.project.VL.title" />
</h2>
<table style="width: 100%">
	<tr>
		<td class="required">*</td>
		<td><spring:message code="sample.entry.project.ARV.centerName" />
		</td>
		<td><form:select path="projectData.ARVcenterName"
				id="vl.centerName" onchange="vl.checkCenterName(true)">
				<option value=""></option>
				<form:options
					items="${form.organizationTypeLists.ARV_ORGS_BY_NAME.list}"
					itemLabel="organizationName" itemValue="id" />
			</form:select>
			<div id="vl.centerNameMessage" class="blank"></div></td>
	</tr>
	<tr>
		<td class="required">*</td>
		<td><spring:message code="patient.project.centerCode" /></td>
		<td><form:select path="projectData.ARVcenterCode"
				id="vl.centerCode" onchange="vl.checkCenterCode(true)">
				<option value=""></option>
				<form:options items="${form.organizationTypeLists.ARV_ORGS.list}"
					itemLabel="doubleName" itemValue="id" />
			</form:select>
			<div id="vl.centerCodeMessage" class="blank"></div></td>
	</tr>
	<tr>
		<td></td>
		<td><spring:message code="patient.project.nameOfClinician" /></td>
		<td><form:input path="observations.nameOfDoctor"
				onchange="makeDirty();compareAllObservationHistoryFields(true);"
				cssClass="text" id="vl.nameOfDoctor" size="50" />
			<div id="vl.nameOfDoctorMessage" class="blank"></div></td>
	</tr>
	<tr>
		<td></td>
		<td><spring:message code="patient.project.nameOfSampler" /></td>
		<td><form:input path="observations.nameOfSampler"
				onchange="makeDirty();compareAllObservationHistoryFields(true)"
				cssClass="text" id="vl.nameOfSampler" size="50" />
			<div id="vl.nameOfSamplerMessage" class="blank"></div></td>
	</tr>
	<tr>
		<td class="required" width="2%">*</td>
		<td width="28%"><spring:message
				code="sample.entry.project.receivedDate" />&nbsp;<%=DateUtil.getDateUserPrompt()%>
		</td>
		<td width="70%"><form:input path="receivedDateForDisplay"
				onkeyup="addDateSlashes(this, event);"
				onchange="vl.checkReceivedDate(false)" cssClass="text"
				id="vl.receivedDateForDisplay" maxlength="10" />
			<div id="vl.receivedDateForDisplayMessage" class="blank"></div></td>
	</tr>
	<tr>
		<td></td>
		<td><spring:message code="sample.entry.project.receivedTime" />&nbsp;<spring:message
				code="sample.military.time.format" /></td>
		<td><form:input path="receivedTimeForDisplay"
				onkeyup="filterTimeKeys(this, event);"
				onblur="vl.checkReceivedTime(true);" cssClass="text"
				id="vl.receivedTimeForDisplay" maxlength="5" />
			<div id="vl.receivedTimeForDisplayMessage" class="blank"></div></td>
	</tr>
	<tr>
		<td class="required">*</td>
		<td><spring:message code="sample.entry.project.dateTaken" />&nbsp;<%=DateUtil.getDateUserPrompt()%>
		</td>
		<td><form:input path="interviewDate"
				onkeyup="addDateSlashes(this, event);"
				onchange="vl.checkInterviewDate(true);" cssClass="text"
				id="vl.interviewDate" maxlength="10" />
			<div id="vl.interviewDateMessage" class="blank"></div></td>
	</tr>
	<tr>
		<td></td>
		<td><spring:message code="sample.entry.project.timeTaken" />&nbsp;<spring:message
				code="sample.military.time.format" /></td>
		<td><form:input path="interviewTime"
				onkeyup="filterTimeKeys(this, event);"
				onblur="vl.checkInterviewTime(true);" cssClass="text"
				id="vl.interviewTime" maxlength="5" />
			<div id="vl.interviewTimeMessage" class="blank"></div></td>
	</tr>

	<tr>
		<td class="required">+</td>
		<td class=""><spring:message code="patient.subject.number" /></td>
		<td><form:input path="subjectNumber"
				onchange="vl.checkSubjectNumber(true);" id="vl.subjectNumber"
				cssClass="text" maxlength="7" />
			<div id="vl.subjectNumberMessage" class="blank"></div></td>
	</tr>
	<tr>
		<td class="required">+</td>
		<td><spring:message code="patient.site.subject.number" /></td>
		<td><form:input path="siteSubjectNumber"
				id="vl.siteSubjectNumber" cssClass="text"
				onkeyup="addPatientCodeSlashes(this, event);"
				onchange="vl.checkSiteSubjectNumber(true, false);validateSiteSubjectNumber(this); makeDirty();" maxlength="18"/>
			<div id="vl.siteSubjectNumberMessage" class="blank"></div></td>
	</tr>
<%-- 	<tr>
		<td class=""></td>
		<td><spring:message code="patient.upid.code" /></td>
		<td><form:input path="upidCode" id="vl.upidCode" cssClass="text" />
			<div id="vl.upidCodeMessage" class="blank"></div></td>
	</tr> --%>
	<tr>
		<td class="required">*</td>
		<td><spring:message code="patient.project.labNo" /></td>
		<td>
			<div class="blank">
				<spring:message code="sample.entry.project.LVL" />
			</div> <input type="text" name="vl.labNoForDisplay" id="vl.labNoForDisplay"
			size="5" class="text"
			onchange="handleLabNoChange( this, '<spring:message code="sample.entry.project.LVL"/>', false ); makeDirty();"
			maxlength="5" /> <form:input path="labNo" cssClass="text"
				style="display: none;" id="vl.labNo" />
			<div id="vl.labNoMessage" class="blank"></div>
		</td>
		<td></td>
	</tr>
	<tr>
		<td class="required">*</td>
		<td><spring:message code="patient.project.dateOfBirth" />&nbsp;<%=DateUtil.getDateUserPrompt()%>
		</td>
		<td><form:input path="birthDateForDisplay"
				onkeyup="addDateSlashes(this, event);"
				onchange="vl.checkDateOfBirth(false)" cssClass="text"
				id="vl.dateOfBirth" maxlength="10" />
			<div id="vl.dateOfBirthMessage" class="blank"></div></td>
	</tr>
	<tr>
		<td></td>
		<td><spring:message code="patient.age" /></td>
		<td><label for="vl.age"><spring:message code="label.year" /></label>
			<INPUT type="text" name="ageYear" id="vl.age" size="3"
			onchange="vl.checkAge( this, true, 'year' );" maxlength="2" />
			<div id="vl.ageMessage" class="blank"></div></td>
	</tr>
	<tr>
		<td class="required">*</td>
		<td><spring:message code="patient.project.gender" /></td>
		<td><form:select path="gender" onchange="vl.checkGender(true)"
				id="vl.gender">
				<option value=""></option>
				<form:options items="${form.formLists.GENDERS}"
					itemLabel="localizedName" itemValue="id" />
			</form:select>
			<div id="vl.genderMessage" class="blank"></div></td>
	</tr>

	<tr>
		<td></td>
		<td class="observationsQuestion"><spring:message
				code="sample.project.vlPregnancy" /></td>
		<td><form:select path="observations.vlPregnancy"
				onchange="makeDirty();compareAllObservationHistoryFields(true)"
				id="vl.vlPregnancy">
				<option value=""></option>
				<form:options items="${form.dictionaryLists.YES_NO.list}"
					itemLabel="localizedName" itemValue="id" />
			</form:select>
			<div id="vl.vlPregnancyMessage" class="blank"></div></td>
	</tr>

	<tr>
		<td></td>
		<td class="observationsQuestion"><spring:message
				code="sample.project.vlSuckle" /></td>
		<td><form:select path="observations.vlSuckle"
				onchange="makeDirty();compareAllObservationHistoryFields(true)"
				id="vl.vlSuckle">
				<option value=""></option>
				<form:options items="${form.dictionaryLists.YES_NO.list}"
					itemLabel="localizedName" itemValue="id" />
			</form:select>
			<div id="vl.vlSuckleMessage" class="blank"></div></td>
	</tr>


	<tr>
		<td class="required">*</td>
		<td><spring:message code="patient.project.hivType" /></td>
		<td><form:select path="observations.hivStatus"
				onchange="vl.checkHivStatus(true);" id="vl.hivStatus">
				<option value=""></option>
				<form:options items="${form.dictionaryLists.HIV_TYPES.list}"
					itemLabel="localizedName" itemValue="id" />
			</form:select>
			<div id="vl.hivStatusMessage" class="blank"></div></td>
	</tr>



	<tr>
		<td colspan="5"><hr /></td>
	</tr>
	<%-- _________________________________________________ --%>

	<tr>
		<td></td>
		<td class="observationsQuestion"><spring:message
				code="sample.entry.project.arv.treatment" /></td>
		<td><form:select path="observations.currentARVTreatment"
				onchange="vl.checkInterruptedARVTreatment();compareAllObservationHistoryFields(true);"
				id="vl.currentARVTreatment">
				<option value=""></option>
				<form:options items="${form.dictionaryLists.YES_NO.list}"
					itemLabel="localizedName" itemValue="id" />
			</form:select>
			<div id="vl.currentARVTreatmentMessage" class="blank"></div></td>
	</tr>
	<tr id="vl.arvTreatmentInitDateRow" style="display: none">
		<td></td>
		<td class="observationsSubquestion"><spring:message
				code="sample.entry.project.arv.treatment.initDate" /></td>
		<td><form:input path="observations.arvTreatmentInitDate"
				onkeyup="addDateSlashes(this, event);"
				onchange="vl.checkDate(this,false);" cssClass="text"
				id="vl.arvTreatmentInitDate" maxlength="10" />
			<div id="vl.arvTreatmentInitDateMessage" class="blank"></div></td>
	</tr>

	<tr id="vl.arvTreatmentTherapRow" style="display: none">
		<td></td>
		<td class="observationsSubquestion"><spring:message
				code="sample.entry.project.arv.treatment.therap.line" /></td>
		<td><form:select path="observations.arvTreatmentRegime"
				onchange="makeDirty();compareAllObservationHistoryFields(true);"
				id="vl.arvTreatmentRegime" cssClass="text">
				<option value=""></option>
				<form:options items="${form.dictionaryLists.ARV_REGIME.list}"
					itemLabel="localizedName" itemValue="id" />
			</form:select>
			<div id="vl.arvTreatmentRegimeMessage" class="blank"></div></td>
	</tr>

	<tr id="vl.onGoingARVTreatmentINNsRow" style="display: none">
		<td></td>
		<td class="observationsSubquestion"><spring:message
				code="sample.entry.project.arv.treatment.regimen" /></td>
	</tr>
	<c:forEach var="ongoingARVTreatment" varStatus="iter"
		items="${form.observations.currentARVTreatmentINNsList}">
		<tr style="display: none"
			id='vl.currentARVTreatmentINNRow${iter.index}'>
			<td></td>
			<td class="bulletItem">${iter.index + 1})</td>
			<td><form:input
					path='observations.currentARVTreatmentINNsList[${iter.index}]'
					onchange="makeDirty();compareAllObservationHistoryFields(true);"
					cssClass="text" id='vl.currentARVTreatmentINNs${iter.index}' />
				<div id='vl.currentARVTreatmentINNs${iter.index}>Message'
					class="blank"></div></td>
		</tr>
	</c:forEach>

	<tr>
		<td colspan="5"><hr /></td>
	</tr>
	<%-- __________________________________________________________________________________________________ --%>

	<tr>
		<td></td>
		<td><spring:message code="sample.entry.project.vl.reason" /></td>
		<td><form:select path="observations.vlReasonForRequest"
				onchange="vl.checkVLRequestReason();compareAllObservationHistoryFields(true);"
				id="vl.vlReasonForRequest">
				<option value=""></option>
				<form:options
					items="${form.dictionaryLists.ARV_REASON_FOR_VL_DEMAND.list}"
					itemLabel="localizedName" itemValue="id" />
			</form:select>
			<div id="vl.vlReasonForRequestMessage" class="blank"></div></td>
	</tr>

	<tr id="vl.reasonOtherRow" style="display: none">
		<td></td>
		<td class="Subquestion"><spring:message
				code="sample.entry.project.vl.specify" /></td>
		<td><form:input path="observations.vlOtherReasonForRequest"
				onchange="compareAllObservationHistoryFields(true);" cssClass="text"
				id="vl.vlOtherReasonForRequest" maxlength="50" />
			<div id="vl.vlOtherReasonForRequestMessage" class="blank"></div></td>
	</tr>

	<tr>
		<td colspan="5"><hr /></td>
	</tr>
	<%-- _________________________________________________ --%>

	<tr>
		<td></td>
		<td colspan="3" class="sectionTitle"><spring:message
				code="sample.project.cd4init" /></td>
	</tr>
	<tr>
		<td></td>
		<td><spring:message code="sample.project.cd4Count" /></td>
		<td><form:input path="observations.initcd4Count"
				onchange="makeDirty();compareAllObservationHistoryFields(true);"
				maxlength="4" cssClass="text" id="vl.initcd4Count" />
			<div id="vl.initcd4CountMessage" class="blank"></div></td>
	</tr>
	<tr>
		<td></td>
		<td><spring:message code="sample.project.cd4Percent" /></td>
		<td><form:input path="observations.initcd4Percent"
				onchange="makeDirty();compareAllObservationHistoryFields(true);"
				cssClass="text" id="vl.initcd4Percent" />
			<div id="vl.initcd4PercentMessage" class="blank"></div></td>
	</tr>
	<tr>
		<td></td>
		<td><spring:message code="sample.project.Cd4Date" /></td>
		<td><form:input path="observations.initcd4Date"
				onkeyup="addDateSlashes(this, event);"
				onchange="vl.checkDate(this,false);" cssClass="text"
				id="vl.initcd4Date" maxlength="10" />
			<div id="vl.initcd4DateMessage" class="blank"></div></td>
	</tr>
	<tr>
		<td colspan="5"><hr /></td>
	</tr>
	<%-- _________________________________________________ --%>

	<tr>
		<td></td>
		<td colspan="3" class="sectionTitle"><spring:message
				code="sample.project.cd4demand" /></td>
	</tr>
	<tr>
		<td></td>
		<td><spring:message code="sample.project.cd4Count" /></td>
		<td><form:input path="observations.demandcd4Count"
				onchange="makeDirty();compareAllObservationHistoryFields(true);"
				maxlength="4" cssClass="text" id="vl.demandcd4Count" />
			<div id="vl.demandcd4CountMessage" class="blank"></div></td>
	</tr>
	<tr>
		<td></td>
		<td><spring:message code="sample.project.cd4Percent" /></td>
		<td><form:input path="observations.demandcd4Percent"
				onchange="makeDirty();compareAllObservationHistoryFields(true);"
				cssClass="text" id="vl.demandcd4Percent" />
			<div id="vl.demandcd4PercentMessage" class="blank"></div></td>
	</tr>
	<tr>
		<td></td>
		<td><spring:message code="sample.project.Cd4Date" /></td>
		<td><form:input path="observations.demandcd4Date"
				onkeyup="addDateSlashes(this, event);"
				onchange="vl.checkDate(this,false);" cssClass="text"
				id="vl.demandcd4Date" maxlength="10" />
			<div id="vl.demandcd4DateMessage" class="blank"></div></td>
	</tr>
	<tr>
		<td colspan="5"><hr /></td>
	</tr>
	<%-- __________________________________________________________________________________________________ --%>
	<tr>
		<td></td>
		<td class="observationsQuestion"><spring:message
				code="sample.project.priorVLRequest" /></td>
		<td><form:select path="observations.vlBenefit"
				onchange="vl.checkVLBenefit();compareAllObservationHistoryFields(true);"
				id="vl.vlBenefit">
				<option value=""></option>
				<form:options items="${form.dictionaryLists.YES_NO.list}"
					itemLabel="localizedName" itemValue="id" />
			</form:select>
			<div id="vl.vlBenefitMessage" class="blank"></div></td>
	</tr>

	<tr id="vl.priorVLLabRow" style="display: none">
		<td></td>
		<td class="observationsSubquestion"><spring:message
				code="sample.project.priorVLLab" /></td>
		<td><form:input path="observations.priorVLLab" cssClass="text"
				id="vl.priorVLLab" maxlength="50" />
			<div id="vl.priorVLLabMessage" class="blank"></div></td>

	</tr>
	<tr id="vl.priorVLValueRow" style="display: none">
		<td></td>
		<td class="observationsSubquestion"><spring:message
				code="sample.project.VLValue" /></td>
		<td><form:input path="observations.priorVLValue" cssClass="text"
				onkeypress="vl.IsNumeric(this,event);" id="vl.priorVLValue"
				maxlength="10" />
			<div id="vl.priorVLValueMessage" class="blank"></div></td>

	</tr>

	<tr id="vl.priorVLDateRow" style="display: none">
		<td></td>
		<td class="observationsSubquestion"><spring:message
				code="sample.project.VLDate" /></td>
		<td><form:input path="observations.priorVLDate"
				onkeyup="addDateSlashes(this, event);"
				onchange="vl.checkDate(this,false);" cssClass="text"
				id="vl.priorVLDate" maxlength="10" />
			<div id="vl.priorVLDateMessage" class="blank"></div></td>
	</tr>
	<tr>
		<td>&nbsp;</td>
	</tr>

	<tr>
		<td colspan="5"><hr /></td>
	</tr>
	<tr id="vl.patientRecordStatusRow" style="display: none;">
		<td class="required"></td>
		<td><spring:message code="patient.project.patientRecordStatus" />
		</td>
		<td><INPUT type="text" id="vl.PatientRecordStatus" size="20"
			class="readOnly text" disabled="disabled" readonly="readonly" />
			<div id="vl.PatientRecordStatusMessage" class="blank"></div></td>
	</tr>
	<tr id="vl.sampleRecordStatusRow" style="display: none;">
		<td class="required"></td>
		<td><spring:message code="patient.project.sampleRecordStatus" />
		</td>
		<td><INPUT type="text" id="vl.SampleRecordStatus" size="20"
			class="readOnly text" disabled="disabled" readonly="readonly" />
			<div id="vl.SampleRecordStatusMessage" class="blank"></div></td>
	</tr>
	<tr>
		<td colspan="6"><hr /></td>
	</tr>
	<tr id="vl.underInvestigationRow">
		<td class="required"></td>
		<td><spring:message code="patient.project.underInvestigation" />
		</td>
		<td><form:select path="observations.underInvestigation"
				onchange="makeDirty();compareAllObservationHistoryFields(true)"
				id="vl.underInvestigation">
				<option value=""></option>
				<form:options items="${form.dictionaryLists.YES_NO.list}"
					itemLabel="localizedName" itemValue="id" />
			</form:select>
			<div id="vl.underInvestigationMessage" class="blank"></div></td>
	</tr>
	<tr id="vl.underInvestigationCommentRow">
		<td class="required"></td>
		<td><spring:message
				code="patient.project.underInvestigationComment" /></td>
		<td colspan="3"><form:input
				path="projectData.underInvestigationNote" maxlength="1000" size="80"
				onchange="makeDirty();" id="vl.underInvestigationComment" />
			<div id="vl.underInvestigationCommentMessage" class="blank"></div></td>
	</tr>
	<tr>
		<td></td>
		<td colspan="3" class="sectionTitle"><spring:message
				code="sample.entry.project.title.specimen" /></td>
	</tr>
	<tr>
		<td width="2%"></td>
		<td width="38%"><spring:message
				code="sample.entry.project.ARV.edtaTubeTaken" /></td>
		<td width="60%"><form:checkbox path="ProjectData.edtaTubeTaken"
				id="vl.edtaTubeTaken"
				onchange="vl.checkSampleItem($('vl.edtaTubeTaken'));checkVLSampleType(this);" />
			<div id="vl.edtaTubeTakenMessage" class="blank"></div></td>
	</tr>

	<tr>
		<td width="2%"></td>
		<td width="38%"><spring:message
				code="sample.entry.project.title.dryBloodSpot" /></td>
		<td width="60%"><form:checkbox path="ProjectData.dbsvlTaken"
				id="vl.dbsvlTaken"
				onchange="vl.checkSampleItem($('vl.dbsvlTaken'));checkVLSampleType(this);" />
			<div id="vl.dbsvlTakenMessage" class="blank"></div></td>
	</tr>
	<tr>
		<td width="2%"></td>
		<td width="38%"><spring:message
				code="sample.entry.project.title.psc" /></td>
		<td width="60%"><form:checkbox path="ProjectData.pscvlTaken"
				id="vl.pscvlTaken"
				onchange="vl.checkSampleItem($('vl.pscvlTaken'));checkVLSampleType(this);" />
			<div id="vl.pscvlTakenMessage" class="blank"></div></td>
	</tr>
	<tr>
		<td></td>
		<td colspan="3" class="sectionTitle"><spring:message
				code="sample.entry.project.title.tests" /></td>
	</tr>
	<tr>
		<td></td>
		<td><spring:message code="sample.entry.project.ARV.viralLoadTest" /></td>
		<td><form:checkbox path="ProjectData.viralLoadTest"
				id="vl.viralLoadTest"
				onchange="vl.checkSampleItem($('vl.edtaTubeTaken'), this);" />
			<div id="vl.viralLoadTestMessage" class="blank"></div></td>
	</tr>


</table>
