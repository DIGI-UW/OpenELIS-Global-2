<%@ page language="java" contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants,
			org.openelisglobal.common.util.DateUtil" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



	
<script type="text/javascript">

function RtnProjectChecker() {
	this.idPre = "rtn.";

	this.displayNationality = function() {
		enableSelectedRequiredSubfield($(this.idPre + 'nationality'),
				$(this.idPre + 'nationalityOther'), -2,
				this.idPre + 'nationalityOtherRow')
	}

	this.checkNameOfDoctor = function(blanksAllowed) {
		makeDirty();
		checkRequiredField($("rtn.nameOfDoctor"));
		compareAllObservationHistoryFields(true);
	}

	this.checkAllSampleFields = function(blanksAllowed) {
		this.checkInterviewDate(blanksAllowed);
		this.checkReceivedDate(blanksAllowed);
        this.checkInterviewTime(true);
        this.checkReceivedTime(true);		
	}

	this.refresh = function() {
		this.refreshBase();
		this.displayNationality();
	}

	this.setFieldsForEdit = function(canEditPatientIDs, canEditSampleIDs) {
		this.setFieldReadOnly("labNoForDisplay", !canEditSampleIDs);
	}
}

RtnProjectChecker.prototype = new BaseProjectChecker();
rtn = new RtnProjectChecker();
</script>

<h2>
	<spring:message code="sample.entry.project.RTN.title" />
</h2>
<table>
	<tr>
		<td class="required">
			*
		</td>
		<td>
			<spring:message code="sample.entry.project.receivedDate" />
			&nbsp;
			<%=DateUtil.getDateUserPrompt()%>
		</td>
		<td>
			<form:input path="receivedDateForDisplay"
				onkeyup="addDateSlashes(this, event);"
				onchange="rtn.checkReceivedDate(false); makeDirty();"
				cssClass="text"
				id="rtn.receivedDateForDisplay" maxlength="10" />
			<div id="rtn.receivedDateForDisplayMessage" class="blank" ></div>
		</td>
	</tr>
	<tr>
		<%-- DEM 01 --%>
		<td class="required">
			*
		</td>
		<td class="observationsQuestion">
			<spring:message code="sample.entry.project.dateTaken" />
			&nbsp;
			<%=DateUtil.getDateUserPrompt()%>
		</td>
		<td>
			<form:input path="interviewDate"
				onkeyup="addDateSlashes(this, event);"
				onchange="rtn.checkInterviewDate(false)" cssClass="text"
				maxlength="10" id="rtn.interviewDate" />
			<div id="rtn.interviewDateMessage" class="blank"></div>
		</td>
	</tr>
	<tr>
		<%-- DEM 02 --%>
		<td class="required">
			*
		</td>
		<td id="observationsQuestion">
			<spring:message code="patient.project.nameOfDoctor" />
		</td>
		<td>
			<form:input path="observations.nameOfDoctor"
				cssClass="text" id="rtn.nameOfDoctor" size="50"
				onchange="rtn.checkNameOfDoctor(true);" />
			<div id="nameOfDoctorMessage" class="blank"></div>
		</td>
	</tr>
	<tr>
		<%-- DEM 03 --%>
		<td class="required">
			*
		</td>
		<td>
			<spring:message code="patient.project.hospitals" />
		</td>
		<td>
			<form:select path="centerCode"
				id="rtn.centerCode" cssClass="text"
				onchange="rtn.checkCenterCode(false)">
				<option value=""></option>
				<form:options items="${form.organizationTypeLists.RTN_HOSPITALS.list}" itemLabel="doubleName" itemValue="id" />
			</form:select>
			<div id="rtn.centerCodeMessage" class="blank"></div>
		</td>
	</tr>
	<tr>
		<%-- DEM 04 --%>
		<td class="required">
			*
		</td>
		<td class="observationsQuestion">
			<spring:message code="patient.project.service" />
		</td>
		<td>
			<form:select path="observations.service"
				id="rtn.service" cssClass="text"
				onchange="compareAllObservationHistoryFields(true);">
				<option value=""></option>
				<form:options items="${form.organizationTypeLists.RTN_SERVICES.list}" itemLabel="doubleName" itemValue="id" />
			</form:select>
			<div id="rtn.serviceMessage" class="blank"></div>
		</td>
	</tr>
	<tr>
		<%-- DEM 05 --%>
		<td></td>
		<td class="observationsQuestion">
			<spring:message code="patient.project.hospitalPatient" />
		</td>
		<td>
			<form:select path="observations.hospitalPatient" cssClass="text"
				id="rtn.hospitalPatient"
				onchange="compareAllObservationHistoryFields(true);">
				<option value=""></option>
				<form:options items="${form.dictionaryLists.YES_NO.list}" itemLabel="localizedName" itemValue="id" />
			</form:select>
			<div id="rtn.hospitalPatientMessage" class="blank" ></div>
		</td>
	</tr>
	<tr>
		<%-- DEM 06 --%>
		<td></td>
		<td class="observationsQuestion">
			<spring:message code="patient.project.patientFamilyName" />
		</td>
		<td>
			<form:input path="lastName" cssClass="text"
				id="rtn.patientFamilyName" onchange="rtn.checkFamilyName(true)"
				size="40" />
			<div id="rtn.patientFamilyNameMessage" class="blank"></div>
		</td>
	</tr>
	<tr>
		<%-- DEM 07 --%>
		<td></td>
		<td>
			<spring:message code="patient.project.patientFirstNames" />
		</td>
		<td>
			<form:input path="firstName" cssClass="text"
				id="rtn.patientFirstNames" onchange="rtn.checkFirstNames(true)"
				size="40" />
			<div id="rtn.patientFirstNamesMessage" class="blank"></div>
		</td>
	</tr>
	<tr>
		<%-- DEM 08 --%>
		<td class="required">
			*
		</td>
		<td>
			<spring:message code="patient.project.dateOfBirth" />
			&nbsp;
			<%=DateUtil.getDateUserPrompt()%>
		</td>
		<td>
			<form:input path="birthDateForDisplay"
				cssClass="text" id="rtn.dateOfBirth" maxlength="10"
				onkeyup="addDateSlashes(this, event);"
				onchange="rtn.checkDateOfBirth(true)" />
			<div id="rtn.dateOfBirthMessage" class="blank"></div>
		</td>
	</tr>
	<tr>
		<td></td>
		<td>
			<spring:message code="patient.age" />
		</td>
		<td>
			<label for="rtn.age">
				<spring:message code="label.year" />
			</label>
			<INPUT type="text" name="ageYear" id="rtn.age" size="3"
				onchange="rtn.checkAge( this, true, 'year' ); clearField('rtn.month');"
				maxlength="2" />
			<label for="rtn.month">
				<spring:message code="label.month" />
			</label>
			<INPUT type="text" name="ageMonth" id="rtn.month" size="3"
				onchange="rtn.checkAge( this, true, 'month' );clearField('rtn.age');"
				maxlength="2" />
			<div id="rtn.ageMessage" class="blank"></div>
		</td>
	</tr>
	<tr>
		<%-- DEM 10 --%>
		<td class="required">
			*
		</td>
		<td>
			<spring:message code="patient.project.gender" />
		</td>
		<td>
			<form:select path="gender"
				id="rtn.gender" cssClass="text"
				onchange="rtn.checkGender(true)">
				<option value=""></option>
				<form:options items="${form.formLists.GENDERS}" itemLabel="localizedName"
					itemValue="id" />
			</form:select>
			<div id="rtn.genderMessage" class="blank"></div>
		</td>
	</tr>
	<tr>
		<%-- DEM 11 --%>
		<td></td>
		<td>
			<spring:message code="patient.project.nationality" />

		</td>
		<td>
			<form:select path="observations.nationality"
				id="rtn.nationality" cssClass="text"
				onchange="rtn.displayNationality();compareAllObservationHistoryFields(true)">
				<option value=""></option>
				<form:options items="${form.dictionaryLists.NATIONALITIES.list}" itemLabel="localizedName" itemValue="id" />
			</form:select>
			<div id="rtn.nationalityMessage" class="blank"></div>
		</td>
	</tr>
	<tr id="rtn.nationalityOtherRow" style="display: none">
		<%-- DEM 11.1 --%>
		<td class="required">
			*
		</td>
		<td class="observationsSubquestion">
			<spring:message code="patient.project.nationalityOther" />
		</td>
		<td>
			<form:input path="observations.nationalityOther"
				onchange="checkRequiredField(this);compareAllObservationHistoryFields(true)"
				cssClass="text" id="rtn.nationalityOther" />
			<div id="rtn.nationalityOtherMessage" class="blank"></div>
		</td>
	</tr>
	<tr>
		<%-- DEM 12 --%>
		<td class="required"></td>
		<td>
			<spring:message code="patient.project.serologyReason" />
		</td>
		<td>
			<form:input path="observations.reason"
				cssClass="text" id="rtn.reason"
				onchange="compareAllObservationHistoryFields(true)" />
			<div id="rtn.reason" class="blank"></div>
		</td>
	</tr>
	<tr>
		<%-- DEM 13 --%>
		<td class="required">
			*
		</td>
		<td>
			<spring:message code="patient.project.labNo" />
		</td>
		<td>
			<div class="blank">
				<spring:message code="sample.entry.project.LRTN" />
			</div>
			<INPUT type=text name="rtn.labNoForDisplay" id="rtn.labNoForDisplay"
				size="5" class="text"
				onchange="handleLabNoChange( this, 'LRTN', false ); makeDirty();"
				maxlength="5" />
			<form:input path="labNo" cssClass="text"
				style="display:none;" id="rtn.labNo" />
			<div id="rtn.labNoMessage" class="blank" ></div>
		</td>
	</tr>
	<tr>
		<td colspan="4">
			<hr />
		</td>
	</tr>
	<tr>
		<td colspan="2" style="text-align: center">
			<spring:message code="patient.project.medicalHistory" />
		</td>
	</tr>		
	<c:forEach var="disease" varStatus="iter" items="${form.observations.rtnPriorDiseasesList}">
		<tr id='priorDiseasesRow${iter.index}' >
			<td></td>
			<td class="observationsQuestion observationsSubquestion">
				<c:out value="${disease.value}" />
			</td>
			<td>
				<form:select path='observations.${disease.name}'
					onchange="makeDirty();compareAllObservationHistoryFields(true)"
					id='rtn.${disease.name}'>
					<option value=""></option>
					<form:options items="${form.dictionaryLists['YES_NO_UNKNOWN_NA_NOTSPEC'].list}" itemLabel="localizedName" itemValue="id" />
				</form:select>
				<div id='rtn.${disease.name}.Message' class="blank"></div>
			</td>
		</tr>
	</c:forEach>
	<tr><td colspan="2" style="text-align:center"><spring:message code="patient.project.physicalExam" /></td>
	<%-- current --%>
	<c:forEach var="disease" varStatus="iter" items="${form.observations.rtnCurrentDiseasesList}">
		<tr id='currentDiseasesRow${iter.index}' >
			<td></td>
			<td class="observationsQuestion observationsSubquestion">
				<c:out value="${disease.value}" />
			</td>
			<td>
				<form:select path='observations.${disease.name}'
					onchange="makeDirty();compareAllObservationHistoryFields(true)"
					id='rtn.${disease.name}'>
					<option value=""></option>
					<form:options items="${form.dictionaryLists['YES_NO_UNKNOWN_NA_NOTSPEC'].list}" itemLabel="localizedName" itemValue="id" />
				</form:select>
				<div id='rtn.${disease.name}.Message' class="blank"></div>
			</td>
		</tr>
	</c:forEach>
		<%--
	<c:forEach var="disease" indexId="i" name="${form.formName}" property="dictionaryLists.RTN_DISEASES.list" >
		<tr id='rtn.priorDiseasesRow" + i %>' > 
			<td></td>
			<td class="observationQuestion"><bean:write name="disease" property="localizedName"/></td>
			<td>
				<form:select path='observations.priorDiseases[" + i + "]" %>'
				    id='rtn.priorDiseases" + i %>' onchange="compareAllObservationHistoryFields(true)">
					<form:options items="${dictionaryLists.YES_NO_UNKNOWN_NA_NOTSPEC.list}" itemLabel="localizedName" itemValue="id" />
				</form:select>
			</td>
		</tr>
	</c:forEach>
	<c:forEach var="disease" indexId="i" name="${form.formName}" property="dictionaryLists.RTN_EXAM_DISEASES.list" >
		<tr id='rtn.currDiseasesRow" + i %>' > 
			<td></td>
			<td class="observationQuestion"><bean:write name="disease" property="localizedName"/></td>
			<td>
				<form:select path='observations.currentDiseases[" + i + "]" %>'
				    id='rtn.currentDiseases" + i %>' onchange="compareAllObservationHistoryFields(true)">
					<form:options items="${dictionaryLists.YES_NO_UNKNOWN_NA_NOTSPEC.list}" itemLabel="localizedName" itemValue="id" />
				</form:select>
			</td>
		</tr>
	</c:forEach>
	 --%>
	<tr>
		<td colspan="5">
			<hr />
		</td>
	</tr>
	<tr>
		<td colspan="5">
		</td>
	</tr>
	<tr id="rtn.patientRecordStatusRow" style="display: none;">
		<td class="required"></td>
		<td>
			<spring:message code="patient.project.patientRecordStatus" />
		</td>
		<td>
			<INPUT type="text" id="rtn.PatientRecordStatus" size="20"
				class="text readOnly" disabled="disabled" readonly="readonly" />
			<div id="rtn.PatientRecordStatusMessage" class="blank"></div>
		</td>
	</tr>
	<tr id="rtn.sampleRecordStatusRow" style="display: none;">
		<td class="required"></td>
		<td>
			<spring:message code="patient.project.sampleRecordStatus" />
		</td>
		<td>
			<INPUT type="text" id="rtn.SampleRecordStatus" size="20"
				class="text readOnly" disabled="disabled" readonly="readonly" />
			<div id="rtn.SampleRecordStatusMessage" class="blank"></div>
		</td>
	</tr>
	<tr id="rtn.underInvestigationRow">
		<td class="required"></td>
		<td>
			<spring:message code="patient.project.underInvestigation" />
		</td>
		<td>
			<form:select path="observations.underInvestigation"
				onchange="makeDirty();compareAllObservationHistoryFields(true)"
				id="rtn.underInvestigation">
				<option value=""></option>
				<form:options items="${form.dictionaryLists.YES_NO.list}" itemLabel="localizedName" itemValue="id" />
			</form:select>
			<div id="rtn.underInvestigationMessage" class="blank"></div>
		</td>
	</tr>
	<tr id="rtn.underInvestigationCommentRow">
		<td class="required"></td>
		<td>
			<spring:message code="patient.project.underInvestigationComment" />
		</td>
		<td colspan="3">
			<form:input path="projectData.underInvestigationNote" maxlength="1000"
				size="80" onchange="makeDirty();"
				id="rtn.underInvestigationComment" />
				<div id="rtn.underInvestigationCommentMessage" class="blank"></div>
		</td>
	</tr>
</table>