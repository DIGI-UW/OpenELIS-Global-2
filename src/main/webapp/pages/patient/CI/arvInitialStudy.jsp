<%@ page language="java" contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants,
			org.openelisglobal.common.util.DateUtil" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<script type="text/javascript">
/*
 * Functionality just for this form and its fields
 */
function ArvInitialProjectChecker() {
	this.name = "ArvInitialProjectChecker";
	
	this.displayAnyPriorDiseases = function() {
	    var field = $("anyPriorDiseases");
		showElements((field.selectedIndex == 1),
				"priorDiseasesRow0,priorDiseasesRow1,priorDiseasesRow2,priorDiseasesRow3,priorDiseasesRow4,priorDiseasesRow5,priorDiseasesRow6,priorDiseasesRow7,priorDiseasesRow8,priorDiseasesRow9,priorDiseasesRow10,priorDiseasesRow");
		this.displayPriorDiseasesOther($('priorDiseases'),
				(field.selectedIndex == 1) ? 1 : "never");
	}

	this.displayPriorDiseasesOther = function(field, enableIndex) {
		enableSelectedRequiredSubfield(field, $('priorDiseasesValue'), enableIndex, 'priorDiseasesValueRow')
	}

	this.displayAnyCurrentDiseases = function() {
	    var field =$("anyCurrentDiseases");
		showElements(
				(field.selectedIndex == 1),
				"currentDiseasesRow0,currentDiseasesRow1,currentDiseasesRow2,currentDiseasesRow3,currentDiseasesRow4,currentDiseasesRow5,currentDiseasesRow6,currentDiseasesRow7,currentDiseasesRow8,currentDiseasesRow9,currentDiseasesRow10,currentDiseasesRow");
		this.displayCurrentDiseasesOther($('currentDiseases'),
				(field.selectedIndex == 1) ? 1 : "never");
	}

	this.displayCurrentDiseasesOther = function(field, enableIndex) {
		enableSelectedRequiredSubfield(field, $('currentDiseasesValue'), enableIndex, 'currentDiseasesValueRow')
	}

	this.checkArvProphylaxisBenefit = function (field) {
	    makeDirty();
	   	var prophyTreat = $("arvProphylaxis");
	    if (field.selectedIndex == 2) { // NO
	    	prophyTreat.selectedIndex = prophyTreat.options.length - 1; // NA is last choice
	    } else if (field.selectedIndex == 1 ) {	// explicit YES
	    	prophyTreat.selectedIndex = 0; // make the user pick again	    
	    }
		this.displayArvProphylaxisBenefit();
		compareAllObservationHistoryFields(true);
	}
	
	this.displayArvProphylaxisBenefit = function () {
		enableSelectedRequiredSubfield($('arvProphylaxisBenefit'), $('arvProphylaxis'), 1, 'arvProphylaxisRow');
	}

	this.displayPriorARVTreatment = function() {
		showElements(
				($("priorARVTreatment").selectedIndex == 1), // YES
				"priorARVInnRow,priorARVTreatmentRow0,priorARVTreatmentRow1,priorARVTreatmentRow2,priorARVTreatmentRow3");
	}

	this.displayNationality = function () {
		enableSelectedRequiredSubfield($('nationality'), $('nationalityOther'), -2, 'nationalityOtherRow' )
	}
	
	
	this.checkAnyCurrentDiseases = function (field) {
		makeDirty();
		this.displayAnyCurrentDiseases();
		var treatField = $("currentOITreatment");
		var treatNAIndex = treatField.options.length - 1;	// NA is in the last position
		if (field.selectedIndex == 2) { // No => treatment NA (the last choice)
			treatField.selectedIndex = treatNAIndex;  
		} else if (field.selectedIndex == 1 && treatField.selectedIndex == treatNAIndex ) { // Yes => move NA back to blank
			treatField.selectedIndex = 0;
		}
		compareAllObservationHistoryFields(true);
	}

	/**
	 * Check that all hidden and disabled fields are set in the right state.
	 **/
	this.refresh = function () {
		this.refreshBase();		
		this.displayPriorARVTreatment();
		this.displayArvProphylaxisBenefit();
		this.displayAnyPriorDiseases();
		this.displayAnyCurrentDiseases();
		this.displayNationality();
	}
}
ArvInitialProjectChecker.prototype = new BaseProjectChecker();
iarv = new ArvInitialProjectChecker();
</script>

<h2>
	<spring:message code="sample.entry.project.initialARV.title" />
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
				onchange="iarv.checkReceivedDate(false)" cssClass="text"
				id="receivedDateForDisplay" maxlength="10"/>
			<div id="receivedDateForDisplayMessage" class="blank" />
		</td>
	</tr>
	<tr>
		<%-- DEM 01 --%>
		<td class="required">*</td>
		<td>
			<spring:message code="patient.project.interviewDate" />
			&nbsp;
			<%=DateUtil.getDateUserPrompt()%>
		</td>
		<td>
			<form:input path="interviewDate"
				onkeyup="addDateSlashes(this, event);"
				onchange="iarv.checkInterviewDate(false)" cssClass="text"
				id="interviewDate" maxlength="10"/>
			<div id="interviewDateMessage" class="blank"></div>
		</td>
	</tr>
	<tr>
		<%-- DEM 02 --%>
		<td class="required">
			+
		</td>
		<td>
			<spring:message code="patient.subject.number" />
		</td>
		<td>
			<form:input path="subjectNumber"
				onchange="iarv.checkSubjectNumber(true)"
				id="subjectNumber" cssClass="text"
				maxlength="7" />
			<div id="subjectNumberMessage" class="blank"></div>
		</td>
	</tr>
	<tr>
		<td class="required">+</td>
		<td>
			<spring:message code="patient.site.subject.number" />
		</td>
		<td>
			<form:input path="siteSubjectNumber"
				onchange="iarv.checkSiteSubjectNumber(true, false)"
				id="siteSubjectNumber" cssClass="text" />
			<div id="siteSubjectNumberMessage" class="blank"></div>
		</td>
	</tr>
	<tr>
		<%-- DEM 03 --%>
		<td class="required">
			*
		</td>
		<td>
			<spring:message code="patient.project.labNo" />
		</td>
		<td>
			<div class="blank">
				<spring:message code="sample.entry.project.LART" />
			</div>
			<input type="text" name="arv.labNoForDisplay" id="labNoForDisplay"
				size="5" class="text"
				onchange="handleLabNoChange(this, '<spring:message code="sample.entry.project.LART"/>', false); makeDirty();"
				maxlength="5" />
			<form:input path="labNo"
				cssClass="text" style="display: none;"
				id="labNo" />
			<div id="labNoMessage" class="blank" ></div>
		</td>
	</tr>
	<tr>
		<%-- DEM 04 --%>
		<td class="required"></td>
		<td>
			<spring:message code="patient.project.centerName" />
		</td>
		<td>
			<form:select path="centerName"
				id="centerName" onchange="iarv.checkCenterName(true)">
				<form:option value=""/>
				<form:options items="${form.organizationTypeLists.ARV_ORGS_BY_NAME.list}" itemLabel="organizationName"
					itemValue="id" />
			</form:select>
			<div id="centerNameMessage" class="blank" ></div>
		</td>
	</tr>

	<tr>
		<%-- DEM 05 --%>
		<td class="required">*</td>
		<td>
			<spring:message code="patient.project.centerCode" />
		</td>
		<td>
			<form:select path="centerCode"
				id="centerCode" onchange="iarv.checkCenterCode(true)">
				<form:option value=""/>
				<form:options items="${form.organizationTypeLists.ARV_ORGS.list}" itemLabel="doubleName"
					itemValue="id" />
			</form:select>
			<div id="centerCodeMessage" class="blank"></div>
		</td>
	</tr>
	<tr>
		<%-- DEM 06 --%>
		<td></td>
		<td>
			<spring:message code="patient.project.patientFamilyName" />
		</td>
		<td>
			<form:input path="lastName"
				onchange="iarv.checkFamilyName(true)" cssClass="text" id="patientFamilyName"
				size="40"/>
			<div id="patientFamilyNameMessage" class="blank"></div>
		</td>
	</tr>
	<tr>
		<%-- DEM 07 --%>
		<td></td>
		<td>
			<spring:message code="patient.project.patientFirstNames" />
		</td>
		<td>
			<form:input path="firstName" size="40"
				onchange="iarv.checkFirstNames(true)" cssClass="text" id="patientFirstNames" />
			<div id="patientFirstNamesMessage" class="blank"></div>
		</td>
	</tr>
	<tr>
		<%-- DEM 08 --%>
		<td class="required">
			*
		</td>
		<td>
			<spring:message code="patient.project.gender" />
		</td>
		<td>
			<form:select path="gender"
				onchange="iarv.checkGender(false)" id="gender">
				<form:option value=""/>
				<form:options items="${form.formLists.GENDERS}" itemLabel="localizedName"
					itemValue="id" />
			</form:select>
			<div id="genderMessage" class="blank"></div>
		</td>
	</tr>
	<tr>
		<%-- DEM 09 --%>
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
				onkeyup="addDateSlashes(this, event);"
				onchange="iarv.checkDateOfBirth(false)" cssClass="text"
				id="dateOfBirth" maxlength="10"/>
			<div id="dateOfBirthMessage" class="blank"></div>
		</td>
	</tr>
	<tr>
			<td ></td>
			<td>
				<spring:message code="patient.age" />
			</td>
			<td>
				<label for="age" ><spring:message code="label.year" /></label>
				<input type="text" name="ageYear" id="age" size="3"
				   	onchange="iarv.checkAge( this, true, 'year' );"
				   	maxlength="2" />
				<div id="ageMessage" class="blank" ></div>
			</td>
	</tr>
	<tr id="hivStatusRow" style="display: none;">
		<td></td>
		<td>
			<spring:message code="patient.project.hivStatus" />
		</td>
		<td>
			<form:select path="observations.hivStatus"
					 onchange="iarv.checkHivStatus(true);"
					 id="hivStatus"  >
				<form:option value=""/>
				<form:options items="${form.projectData.hivStatusList}"
					itemLabel="localizedName" itemValue="id" />
			</form:select>
			<div id="hivStatusMessage" class="blank"></div>
		</td>
	</tr>

	<tr>
		<%-- DEM 10 --%>
		<td></td>
		<td>
			<spring:message code="patient.project.educationLevel" />
		</td>
		<td>
			<form:select path="observations.educationLevel" onchange="makeDirty();compareAllObservationHistoryFields(true)"
				id="educationLevel">
				<form:option value=""/>
				<form:options items="${form.dictionaryLists.EDUCATION_LEVELS.list}"
					itemLabel="localizedName" itemValue="id" />
			</form:select>
			<div id="educationLevelMessage" class="blank"></div>
		</td>
	</tr>
	<tr>
		<%-- DEM 11 --%>
		<td></td>
		<td>
			<spring:message code="patient.project.maritalStatus" />
		</td>
		<td>
			<form:select path="observations.maritalStatus" onchange="makeDirty();compareAllObservationHistoryFields(true)"
				id="maritalStatus">
				<form:option value=""/>
				<form:options items="${form.dictionaryLists.MARITAL_STATUSES.list}"
					itemLabel="localizedName" itemValue="id" />
			</form:select>
			<div id="maritalStatusMessage" class="blank"></div>
		</td>
	</tr>
	<tr>
		<%-- DEM 12 --%>
		<td></td>
		<td>
			<spring:message code="patient.project.nationality" />

		</td>
		<td>
			<form:select path="observations.nationality"
				onchange="iarv.displayNationality();compareAllObservationHistoryFields(true)"
				id="nationality" cssClass="text">
				<form:option value=""/>
				<form:options items="${form.dictionaryLists.SIMPLIFIED_NATIONALITIES.list}"
					itemLabel="localizedName" itemValue="id" />
			</form:select>
			<div id="nationalityMessage" class="blank"></div>
		</td>
	</tr>
	<tr id="nationalityOtherRow" style="display: none">
		<%-- DEM 12.1 a --%>
		<td class="required">
			*
		</td>
		<td class="observationsSubquestion">
			<spring:message code="patient.project.nationalityOther" />
		</td>
		<td>
			<form:input path="observations.nationalityOther"
				onchange="checkRequiredField(this); makeDirty();compareAllObservationHistoryFields(true)" cssClass="text"
				id="nationalityOther" />
			<div id="nationalityOtherMessage" class="blank"></div>
		</td>
	</tr>

	<tr>
		<%-- DEM 13 --%>
		<td></td>
		<td>
			<spring:message code="patient.project.legalResidence" />
		</td>
		<td>
			<form:input path="observations.legalResidence"
				onchange="makeDirty();compareAllObservationHistoryFields(true)"
				cssClass="text" id="legalResidence" />
			<div id="legalResidenceMessage" class="blank"></div>
		</td>
	</tr>
	<tr>
		<%-- DEM 14 --%>
		<td></td>
		<td>
			<spring:message code="patient.project.nameOfDoctor" />
		</td>
		<td>
			<form:input path="observations.nameOfDoctor"
				onchange="compareAllObservationHistoryFields(true)"
				cssClass="text" id="nameOfDoctor" size="50"/>
			<div id="nameOfDoctorMessage" class="blank"></div>
		</td>
	</tr>
	<tr>
		<td colspan="3">
			<hr />
		</td>
	</tr>
	<tr>
		<%-- CLI 01 Prior Diseases -------------------------------------------------------------------------------%>
		<td></td>
		<td>
			<br />
			<spring:message code="patient.project.antecedents" />
			<br />
		</td>
		<td>
			<form:select path="observations.anyPriorDiseases"
				onchange="iarv.displayAnyPriorDiseases();makeDirty();compareAllObservationHistoryFields(true)"
				cssClass="text" id="anyPriorDiseases">
				<form:option value=""/>
				<form:options items="${form.dictionaryLists.YES_NO.list}" itemLabel="localizedName"
					itemValue="id" />
			</form:select>
			<div id="anyPriorDiseasesMessage" class="blank" ></div>
		</td>
	</tr>
 	<c:forEach items="${form.observations.priorDiseasesList}" var="disease" varStatus="iter">
		<tr id='priorDiseasesRow${iter.index}' style="display: none">
			<!-- CLI 09.n -->
			<td></td>
			<td class="observationsQuestion observationsSubquestion">
				<c:out value="${disease.value}" />
			</td>
			<td>
				<form:select path='observations.${disease.name}'
					onchange="makeDirty();compareAllObservationHistoryFields(true)"					
					id="${disease.name}${iter.index}"
					>
					<form:option value=""/>
					<form:options items="${form.dictionaryLists.YES_NO.list}" itemLabel="localizedName"
						itemValue="id" />
				</form:select>
				<div id="${disease.name}.Message${iter.index}" class="blank" ></div>
			</td>
		</tr>
	</c:forEach> 
	<tr id="priorDiseasesRow" style="display: none">
		<td></td>
		<td class="observationsQuestion observationsSubquestion">
			<spring:message code="patient.project.other" />
		</td>
		<td>
			<form:select path="observations.priorDiseases"
				onchange="iarv.displayPriorDiseasesOther(this, 1);makeDirty();compareAllObservationHistoryFields(true)"
				id="priorDiseases">
				<form:option value=""/>
				<form:options items="${form.dictionaryLists.YES_NO.list}" itemLabel="localizedName"
					itemValue="id" />
			</form:select>
			<div id="priorDiseasesMessage" class="blank" ></div>
		</td>
	</tr>
	<tr id="priorDiseasesValueRow" style="display: none">
		<td class="required">
			*
		</td>
		<td class="observationsSubquestion">
			<spring:message code="patient.project.specify" />
		</td>
		<td>
			<form:input path="observations.priorDiseasesValue" onchange="makeDirty();compareAllObservationHistoryFields(true)"
				id="priorDiseasesValue"/>
			<div id="priorDiseasesMessage" class="blank"></div>
		</td>
	</tr>
	<tr>
		<%--CLI 02 --%>
		<td>
		</td>
		<td class="observationsQuestion">
			<spring:message code="patient.project.arvProphylaxisBenefit" />
		</td>
		<td>
			<form:select path="observations.arvProphylaxisBenefit"
				onchange="iarv.checkArvProphylaxisBenefit(this)"
				id="arvProphylaxisBenefit">
				<form:option value=""/>
				<form:options items="${form.dictionaryLists.YES_NO.list}" itemLabel="localizedName"
					itemValue="id" />
			</form:select>
			<div id="arvProphylaxisBenefitMessage" class="blank"></div>
		</td>
	</tr>
	<tr id="arvProphylaxisRow" style="display: none">
		<%--CLI 03 --%>
		<td class="required">
			*
		</td>
		<td class="observationsSubquestion">
			<spring:message code="patient.project.arvProphylaxis" />
		</td>
		<td>
			<form:select path="observations.arvProphylaxis"
				onchange="checkRequiredField(this); makeDirty();compareAllObservationHistoryFields(true)"
				id="arvProphylaxis">
				<form:option value=""/>
				<form:options items="${form.dictionaryLists.ARV_PROPHYLAXIS.list}"
					itemLabel="localizedName" itemValue="id" />
			</form:select>
			<div id="arvProphylaxisMessage" class="blank"></div>
		</td>
	</tr>
	<tr>
		<%--CLI 04 --%>
		<td></td>
		<td class="observationsQuestion">
			<spring:message code="patient.project.currentARVTreatment" />
		</td>
		<td>
			<form:select path="observations.currentARVTreatment" onchange="makeDirty();compareAllObservationHistoryFields(true)"
				id="currentARVTreatment">
				<form:option value=""/>
				<form:options items="${form.dictionaryLists.YES_NO.list}" itemLabel="localizedName"
					itemValue="id" />
			</form:select>
			<div id="currentARVTreatmentMessage" class="blank"></div>
		</td>
	</tr>
	<tr>
		<%-- CLI 05 --%>
		<td>
		</td>
		<td class="observationsQuestion">
			<spring:message code="patient.project.priorARVTreatment" />
		</td>
		<td>
			<form:select path="observations.priorARVTreatment"
				onchange="iarv.displayPriorARVTreatment(); makeDirty();compareAllObservationHistoryFields(true)"
				id="priorARVTreatment">
				<form:option value=""/>
				<form:options items="${form.dictionaryLists.YES_NO_NA.list}" itemLabel="localizedName"
					itemValue="id" />
			</form:select>
			<div id="priorARVTreatmentINNMessage" class="blank"></div>
		</td>
	</tr>
	<tr id="priorARVInnRow" style="display: none">
		<%-- CLI 06 --%>
		<td></td>
		<td class="observationsSubquestion" colspan="2" id="priorARVInn">
			<spring:message code="patient.project.priorARVInn" />
		</td>
	</tr>
	<c:forEach var="prevARVTreat" items="${form.observations.priorARVTreatmentINNsList}" varStatus="iter">
		<tr id='priorARVTreatmentRow${iter.index}'
			class="observationsSubquestion" style="display: none">
			<td></td>
			<td class="bulletItem" id='priorARVTreatmentItem${iter.index}'>${iter.index + 1})
			</td>
			<td>
				<form:input path='observations.priorARVTreatmentINNsList[${iter.index}]'
					cssClass="text" onchange="makeDirty();compareAllObservationHistoryFields(true)"
					id='priorARVTreatmentINNs${iter.index}'/>
				<div id='priorARVTreatmentINNs${iter.index}Message'
					class="blank"></div>
			</td>
		</tr>
	</c:forEach>

	<tr>
		<%-- CLI 07 --%>
		<td></td>
		<td>
			<spring:message code="patient.project.cotrimoxazoleTreatment" />
		</td>
		<td>
			<form:select path="observations.cotrimoxazoleTreatment"
				onchange="makeDirty();compareAllObservationHistoryFields(true)"
				id="cotrimoxazoleTreatment">
				<form:option value=""/>
				<form:options items="${form.dictionaryLists.YES_NO.list}" itemLabel="localizedName"
					itemValue="id" />
			</form:select>
			<div id="cotrimoxazoleTreatmentMessage" class="blank"></div>
		</td>
	</tr>
	<tr>
		<%-- CLI 08 --%>
		<td></td>
		<td>
			<spring:message code="patient.project.aidsStage" />
		</td>
		<td>
			<form:select path="observations.aidsStage"
				onchange="makeDirty();compareAllObservationHistoryFields(true)" id="aidsStage">
				<form:option value=""/>
				<form:options items="${form.dictionaryLists.AIDS_STAGES.list}" itemLabel="localizedName"
					itemValue="id" />
			</form:select>
			<div id="aidsStageMessage" class="blank"></div>
		</td>
	</tr>
	<tr>
		<td colspan="3">
			<hr />
		</td>
	</tr>
	<%-- Current Diseases -------------------------------------------------------------------------------%>
	<tr>
		<%-- CLI 09.n --%>
		<td></td>
		<td class="observationsQuestion">
			<br />
			<spring:message code="patient.project.anyCurrentDiseases" />
			<br />
		</td>
		<td>
			<form:select path="observations.anyCurrentDiseases"
				onchange="iarv.checkAnyCurrentDiseases(this)"
				cssClass="text" id="anyCurrentDiseases">
				<form:option value=""/>
				<form:options items="${form.dictionaryLists.YES_NO.list}" itemLabel="localizedName"
					itemValue="id" />
			</form:select>
			<div id="anyCurrentDiseasesMessage" class="blank"></div>
		</td>
	</tr>
    <c:forEach var="disease" items="${form.observations.currentDiseasesList}" varStatus="iter">
		<tr id='currentDiseasesRow${iter.index}' style="display: none">
			<!-- CLI 09.n -->
			<td></td>
			<td class="observationsQuestion observationsSubquestion">
				<c:out value="${disease.value}" />
			</td>
			<td>
				<form:select path='observations.${disease.name}'
					onchange="makeDirty();compareAllObservationHistoryFields(true)"					
					id="${disease.name}${iter.index}"
					>
					<form:option value=""/>
					<form:options items="${form.dictionaryLists.YES_NO.list}" itemLabel="localizedName"
						itemValue="id" />
				</form:select>
				<div id="${disease.name}.Message${iter.index}" class="blank" ></div>
			</td>
		</tr>
	</c:forEach>
	<tr id="currentDiseasesRow" style="display: none">
		<td></td>
		<td class="observationsQuestion observationsSubquestion">
			<spring:message code="patient.project.other" />
		</td>
		<td>
			<form:select path="observations.currentDiseases"
				onchange="iarv.displayCurrentDiseasesOther(this, 1);makeDirty();compareAllObservationHistoryFields(true)"
				id="currentDiseases">
				<form:option value=""/>
				<form:options items="${form.dictionaryLists.YES_NO.list}" itemLabel="localizedName"
					itemValue="id" />
			</form:select>
			<div id="currentDiseasesMessage" class="blank"></div>
		</td>
	</tr>

	<tr id="currentDiseasesValueRow" style="display: none">
		<td class="required">
			*
		</td>
		<td class="observationsSubquestion">
			<spring:message code="patient.project.specify" />
		</td>
		<td>
			<form:input path="observations.currentDiseasesValue" onchange="makeDirty();compareAllObservationHistoryFields(true)"
				id="currentDiseasesValue" />
			<div id="currentDiseasesMessage" class="blank"></div>
		</td>
	</tr>
	<tr>
		<%-- CLI 10 --%>
		<td></td>
		<td>
			<spring:message code="patient.project.currentOITreatment" />
		</td>
		<td>
			<form:select path="observations.currentOITreatment" onchange="makeDirty();compareAllObservationHistoryFields(true)"
				id="currentOITreatment">
				<form:option value=""/>
				<form:options items="${form.dictionaryLists.YES_NO_NA.list}" itemLabel="localizedName"
					itemValue="id" />
			</form:select>
			<div id="currentOITreatment" class="blank"></div>
		</td>
	</tr>
	<tr>
		<%-- CLI 11 --%>
		<td></td>
		<td>
			<spring:message code="patient.project.patientWeight" />
		</td>
		<td>
			<form:input path="observations.patientWeight"
				onchange="makeDirty();compareAllObservationHistoryFields(true)" cssClass="text" id="patientWeight" maxlength="3"/>
			<div id="patientWeightMessage" class="blank"></div>
		</td>
	</tr>
	<tr>
		<%-- CLI 11 --%>
		<td></td>
		<td>
			<spring:message code="patient.project.karnofskyScore" />
		</td>
		<td>
			<form:input path="observations.karnofskyScore"
				onchange="makeDirty();compareAllObservationHistoryFields(true)" cssClass="text" id="karnofskyScore" maxlength="3"/>
			<div id="karnofskyScoreMessage" class="blank"></div>
		</td>
	</tr>
	<tr>
		<td colspan="5">
			<hr />
		</td>
	</tr>
	<tr><td colspan="5"> </td></tr>
 	<tr id="patientRecordStatusRow"style="display: none;">
		<td class="required"></td>
		<td>
			<spring:message code="patient.project.patientRecordStatus" />
		</td>
		<td>
		<INPUT type="text" id="PatientRecordStatus" size="20" class="text readOnly" disabled="disabled" readonly="readonly" />
		<div id="PatientRecordStatusMessage" class="blank"></div>
		</td>
    </tr>
 	<tr id="sampleRecordStatusRow" style="display: none;">
		<td class="required"></td>
		<td>
			<spring:message code="patient.project.sampleRecordStatus" />
		</td>
		<td>
		<INPUT type="text" id="SampleRecordStatus" size="20" class="text readOnly" disabled="disabled" readonly="readonly" />
		<div id="SampleRecordStatusMessage" class="blank"></div>
		</td>
    </tr>
	<tr id="underInvestigationRow">
		<td class="required"></td>
		<td>
			<spring:message code="patient.project.underInvestigation" />
		</td>
		<td>
			<form:select path="observations.underInvestigation" onchange="makeDirty();compareAllObservationHistoryFields(true)"
			id="underInvestigation">
			<form:option value=""/>
			<form:options items="${form.dictionaryLists.YES_NO.list}" itemLabel="localizedName"
				itemValue="id" />
			</form:select>
			<div id="underInvestigationMessage" class="blank"></div>
		</td>
    </tr>    
 	<tr id="underInvestigationCommentRow">
		<td class="required"></td>
		<td>
			<spring:message code="patient.project.underInvestigationComment" />
		</td>
		<td colspan="3">
			<form:input path="projectData.underInvestigationNote" maxlength="1000" size="80"
				onchange="makeDirty();" id="underInvestigationComment" />
				<div id="underInvestigationCommentMessage" class="blank"></div>
		</td>
    </tr>
</table>