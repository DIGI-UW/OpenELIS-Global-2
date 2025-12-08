<%@ page language="java" contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants,
	        org.openelisglobal.common.formfields.FormFields,
	        org.openelisglobal.common.util.DateUtil"%>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>


	
<script type="text/javascript">

/**
 * an object gathers all the methods about tweeking the followup ARV form together in one place.
 **/
function ArvFollowupProjectChecker() {

    this.idPre = "farv.";
	this.displayAnyCurrentDiseases = function () {
	    var field = $("farv.anyCurrentDiseases");
		showElements((field.selectedIndex == 1) , "farv.currentDiseasesRow0,farv.currentDiseasesRow1,farv.currentDiseasesRow2,farv.currentDiseasesRow3,farv.currentDiseasesRow4,farv.currentDiseasesRow5,farv.currentDiseasesRow6,farv.currentDiseasesRow7,farv.currentDiseasesRow8,farv.currentDiseasesRow9,farv.currentDiseasesRow" );
		this.displayCurrentDiseasesOther($('farv.currentDiseases'), (field.selectedIndex == 1)?1:"never");
	}

	this.displayCurrentDiseasesOther = function (field, enableIndex) {
		enableSelectedRequiredSubfield(field, $('farv.currentDiseasesValue'), 1, 'farv.currentDiseasesValueRow')
	}

	
	this.checkInterruptedARVTreatment = function () {
		clearFormElements("farv.priorARVTreatment,farv.arvTreatmentChange");
		this.displayedByInterruptedARVTreatment();
		this.checkPriorARVTreatment();
	}
	
	this.displayedByInterruptedARVTreatment = function () {
	    var field = $("farv.interruptedARVTreatment");
		showElements( (field.selectedIndex == 2), "farv.priorARVTreatmentRow,farv.arvTreatmentChangeRow" );
		this.displayedByPriorARVTreatment(1);
	}
	
	this.checkPriorARVTreatment = function () {
		clearFormElements("farv.priorARVTreatmentINNs0,farv.priorARVTreatmentINNs1,farv.priorARVTreatmentINNs2,farv.priorARVTreatmentINNs3");
		this.displayedByPriorARVTreatment(1);
		clearFormElements("farv.arvTreatmentAnyAdverseEffects");
		this.displayedByArvTreatmentAnyAdverseEffects();
	}
	
	this.displayedByPriorARVTreatment = function (enableIndex) {
		var field = $("farv.priorARVTreatment");
		showElements( (field.selectedIndex == enableIndex), "farv.arvTreatmentAnyAdverseEffectsRow,farv.onGoingARVTreatmentINNsRow,farv.priorARVTreatmentINNRow0,farv.priorARVTreatmentINNRow1,farv.priorARVTreatmentINNRow2,farv.priorARVTreatmentINNRow3");
		this.displayedByArvTreatmentAnyAdverseEffects(1/*(field.selectedIndex == 1)?1:"never"*/);
	}
	
	this.checkArvTreatmentAnyAdverseEffects = function() {
		clearFormElements( "farv.arvTreatmentAdvEffType0,farv.arvTreatmentAdvEffType1,farv.arvTreatmentAdvEffType2,farv.arvTreatmentAdvEffType3");
		clearFormElements( "farv.arvTreatmentAdvEffGrd0,farv.arvTreatmentAdvEffGrd1,farv.arvTreatmentAdvEffGrd2,farv.arvTreatmentAdvEffGrd3");
		this.displayedByArvTreatmentAnyAdverseEffects(1);
	}

	this.displayedByArvTreatmentAnyAdverseEffects = function (enableIndex) {
		var field = $("farv.arvTreatmentAnyAdverseEffects");
		showElements( (field.selectedIndex == enableIndex), "farv.arvTreatmentAdverseEffectsRow0,farv.arvTreatmentAdverseEffectsRow1,farv.arvTreatmentAdverseEffectsRow2,farv.arvTreatmentAdverseEffectsRow3");
	}

	this.displayARVTreatmentNew = function () { /** called from SUI 24 **/
	    var field1 = $("farv.arvTreatmentNew");
		showElements( (field1.selectedIndex == 1), "farv.arvTreatmentRegimeRow" );
		showElements( (field1.selectedIndex == 1), "farv.prescribedARVTreatmentINNsRow,farv.futureARVTreatmentINNsRow0,farv.futureARVTreatmentINNsRow1,farv.futureARVTreatmentINNsRow2,farv.futureARVTreatmentINNsRow3" );
	}

	this.displayCotriTreatment = function () {
	    var field = $("farv.cotrimoxazoleTreatment");
		showElements( (field.selectedIndex == 1), "farv.cotrimoxazoleTreatmentAnyAdverseEffectsRow");
		this.displayCotriAdverseEffects();
	}

	this.displayCotriAdverseEffects = function ( field, enableIndex ) {
		var field = $('farv.cotrimoxazoleTreatAnyAdvEff');
		showElements( (field.selectedIndex == 1), "farv.cotrimoxazoleTreatAdvEffRow0,farv.cotrimoxazoleTreatAdvEffRow1,farv.cotrimoxazoleTreatAdvEffRow2,farv.cotrimoxazoleTreatAdvEffRow3");
	}
	this.displayAny2ndTreatment = function () {
		var field = $('farv.anySecondaryTreatment');
		showElements( (field.selectedIndex == 1), "farv.secondaryTreatmentRow");
	}

	/**
	 * Check that all hidden and disabled fields are set in the right state.
	 **/
	this.refresh = function () {
		this.refreshBase();		
		this.displayAnyCurrentDiseases();
		this.displayedByInterruptedARVTreatment();
		this.displayARVTreatmentNew();
		this.displayCotriTreatment();
		this.displayAny2ndTreatment();
	}

	var only2CharsComparator = new function () {
	    this.compare = function(existing, newVal) {
	    	if ( existing == undefined ) {
	    		return newVal == null || newVal == "";
	    	} else {
				return existing.substring(0,2) == newVal.substring(0,2);
			}
		}
	}

	this.checkFamilyName = function (blanksAllowed) {
		makeDirty();
		compareFieldToExisting( this.idPre + "patientFamilyName", false, patientLoader, blanksAllowed, "lastName", only2CharsComparator);
	}

	this.checkFirstNames = function (blanksAllowed) {
		makeDirty();
		compareFieldToExisting( this.idPre + "patientFirstNames", false, patientLoader, blanksAllowed, "firstName", only2CharsComparator);
	}

}

ArvFollowupProjectChecker.prototype = new BaseProjectChecker();
/// the object which knows about Followup ARV questions and which fields to show etc.
farv = new ArvFollowupProjectChecker();

</script>

<h2>
	<spring:message code="sample.entry.project.followupARV.title" />
</h2>
<table style="width:100%">

	<tr>
		<td class="required">*</td>
		<td>
			<spring:message code="sample.entry.project.receivedDate"/>&nbsp;<%=DateUtil.getDateUserPrompt()%>
		</td>
		<td>
		<form:input path="receivedDateForDisplay"
				onkeyup="addDateSlashes(this, event);"
				onchange="farv.checkReceivedDate(false);"
				cssClass="text"
				id="farv.receivedDateForDisplay" maxlength="10"/>
				<div id="farv.receivedDateForDisplayMessage" class="blank" ></div>
		</td>
	</tr>
	<tr><%-- SUI 01 --%>
		<td class="required">*</td>
		<td>
			<spring:message code="patient.project.interviewDate" />&nbsp;<%=DateUtil.getDateUserPrompt()%>
		</td>
		<td>
			<form:input path="interviewDate"
				onkeyup="addDateSlashes(this, event);"
				onchange="farv.checkInterviewDate(false);"
				cssClass="text" id="farv.interviewDate" maxlength="10"/>
			<div id="farv.interviewDateMessage" class="blank"></div>
		</td>
	</tr>
	<tr> <%-- SUI 02 --%>
		<td class="required">+</td>
		<td class="">
			<spring:message code="patient.subject.number"/>
		</td>
		<td>
			<form:input path="subjectNumber"
					  onchange="farv.checkSubjectNumber(true);"
					  id="farv.subjectNumber"
					  cssClass="text"
					  maxlength="7"/>
			<div id="farv.subjectNumberMessage" class="blank"></div>
		</td>
	</tr>
	<tr>
		<td class="required">+</td>
		<td><spring:message code="patient.site.subject.number"/></td>
		<td>
			<form:input path="siteSubjectNumber"
				id="farv.siteSubjectNumber" cssClass="text"
				onchange="farv.checkSiteSubjectNumber(true, false); makeDirty();"
			/>
			<div id="farv.siteSubjectNumberMessage" class="blank"></div>
		</td>
	</tr>
	<tr> <%-- SUI 03 --%>
		<td class="required">*</td>
		<td>
			<spring:message code="patient.project.labNo" />
		</td>
		<td>
			<div class="blank"><spring:message code="sample.entry.project.LART"/></div>
			<input type="text" name="farv.labNoForDisplay" id="farv.labNoForDisplay" size="5" class="text"
			   	onchange="handleLabNoChange( this, '<spring:message code="sample.entry.project.LART"/>', false ); makeDirty();"
			   	maxlength="5" />
		  	<form:input path="labNo"
				cssClass="text" style="display: none;"
				id="farv.labNo" />
			<div id="farv.labNoMessage" class="blank" ></div>
		</td>
		<td>
		</td>
	</tr>
	<tr> <%-- SUI 04 --%>
		<td></td>
		<td>
			<spring:message code="patient.project.centerName" />
		</td>
		<td>
			<form:select path="centerName"
				id="farv.centerName" onchange="farv.checkCenterName(false)">
				<option value=""></option>
				<form:options items="${form.organizationTypeLists.ARV_ORGS_BY_NAME.list}" itemLabel="organizationName"
					itemValue="id" />
			</form:select>
			<div id="farv.centerNameMessage" class="blank"></div>
		</td>
	</tr>
	<tr> <%-- SUI 05 --%>
		<td class="required">*</td>
		<td>
			<spring:message code="patient.project.centerCode" />
		</td>
		<td>
			<form:select path="centerCode" id="farv.centerCode"
					 onchange="farv.checkCenterCode(false)" >
				<option value=""></option>
				<form:options items="${form.organizationTypeLists.ARV_ORGS.list}" itemLabel="doubleName" itemValue="id" />
			</form:select>
			<div id="farv.centerCodeMessage" class="blank"></div>
		</td>
	</tr>
	<tr> <%-- SUI 06 --%>
		<td></td>
		<td>
			<spring:message code="patient.project.patientFamilyName" />
		</td>
		<td>
			<form:input path="lastName"
				onchange="farv.checkFamilyName(true)" maxlength="2" cssClass="text" id="farv.patientFamilyName" />
			<div id="farv.patientFamilyNameMessage" class="blank"></div>
		</td>
	</tr>
	<tr> <%-- SUI 07 --%>
		<td></td>
		<td>
			<spring:message code="patient.project.patientFirstNames" />
		</td>
		<td>
			<form:input path="firstName"
				onchange="farv.checkFirstNames(true)" maxlength="2" cssClass="text" id="farv.patientFirstNames" />
			<div id="patientFirstNamesMessage" class="blank"></div>
		</td>
	</tr>
	<tr> <%-- SUI 08 --%>
		<td class="required">*</td>
		<td>
			<spring:message code="patient.project.gender" />
		</td>
		<td>
			<form:select path="gender"
					 onchange="farv.checkGender(false)" id="farv.gender"  >
				<option value=""></option>
				<form:options items="${form.formLists.GENDERS}"
					itemLabel="localizedName" itemValue="id" />
			</form:select>
			<div id="farv.genderMessage" class="blank"></div>
		</td>
	</tr>
	<tr> <%-- SUI 09 --%>
		<td class="required">*</td>
		<td>
			<spring:message code="patient.project.dateOfBirth" />&nbsp;<%=DateUtil.getDateUserPrompt()%>
		</td>
		<td>
			<form:input path="birthDateForDisplay"
				onkeyup="addDateSlashes(this, event);"
				onchange="farv.checkDateOfBirth(false)"	cssClass="text" id="farv.dateOfBirth" maxlength="10"/>
			<div id="farv.dateOfBirthMessage" class="blank"></div>
		</td>
	</tr>
	<tr>
			<td ></td>
			<td>
				<spring:message code="patient.age" />
			</td>
			<td>
				<label for="farv.age" ><spring:message code="label.year" /></label>
				<input type="text" name="ageYear" id="farv.age" size="3"
				   	onchange="farv.checkAge( this, true, 'year' );"
				   	maxlength="2" />
				<div id="farv.ageMessage" class="blank" ></div>
			</td>
	</tr>
	<tr> <%-- SUI 10 --%>
		<td></td>
		<td>
			<spring:message code="patient.project.patientWeight" />
		</td>
		<td>
			<form:input path="observations.patientWeight"
				onchange="compareAllObservationHistoryFields(true);" cssClass="text" id="farv.patientWeight" maxlength="3"/>
			<div id="farv.patientWeightMessage" class="blank"></div>
		</td>
	</tr>
	<tr> <%-- SUI 11 --%>
		<td></td>
		<td>
			<spring:message code="patient.project.karnofskyScore" />
		</td>
		<td>
			<form:input path="observations.karnofskyScore"
				onchange="compareAllObservationHistoryFields(true);" cssClass="text" id="farv.karnofskyScore" maxlength="3"/>
			<div id="farv.karnofskyScoreMessage" class="blank"></div>
		</td>
	</tr>
	<tr > <%-- SUI 12 --%>
		<td></td>
		<td>
			<spring:message code="patient.project.hivStatus" />
		</td>
		<td>
			<form:select path="observations.hivStatus"
					 onchange="farv.checkHivStatus(true);"
					 id="farv.hivStatus"  >
				<option value=""></option>
				<form:options items="${form.dictionaryLists.HIV_STATUSES.list}"
					itemLabel="localizedName" itemValue="id" />
			</form:select>
			<div id="farv.hivStatusMessage" class="blank"></div>
		</td>
	</tr>
	<tr> <%-- SUI 13 --%>
		<td></td>
		<td>
			<spring:message code="patient.project.cd4Count" />
		</td>
		<td>
			<form:input path="observations.cd4Count"
				onchange="compareAllObservationHistoryFields(true);" maxlength="4"
				cssClass="text" id="farv.cd4Count" />
			<div id="farv.cd4CountMessage" class="blank"></div>
		</td>
	</tr>
	<tr> <%-- SUI 14 --%>
		<td></td>
		<td>
			<spring:message code="patient.project.cd4Percent" />
		</td>
		<td>
			<form:input path="observations.cd4Percent"
				onchange="compareAllObservationHistoryFields(true);"
				cssClass="text" id="farv.cd4Percent" />
			<div id="farv.cd4PercentMessage" class="blank"></div>
		</td>
	</tr>
	<tr> <%-- SUI 15 --%>
		<td></td>
		<td>
			<spring:message code="patient.project.priorCd4Date" />
		</td>
		<td>
			<form:input path="observations.priorCd4Date"
				onkeyup="addDateSlashes(this, event);"
				onchange="compareAllObservationHistoryFields(true);"
				cssClass="text" id="priorCd4Date" />
			<div id="farv.priorCd4DateMessage" class="blank"></div>
		</td>
	</tr>
	<tr> <%-- SUI 16 --%>
		<td></td>
		<td>
			<spring:message code="patient.project.nameOfDoctor" />
		</td>
		<td>
			<form:input path="observations.nameOfDoctor"
				onchange="compareAllObservationHistoryFields(true);" cssClass="text" id="farv.nameOfDoctor" size="50"/>
			<div id="farv.nameOfDoctorMessage" class="blank"></div>
		</td>
	</tr>
	<tr><td colspan="5"><hr/></td></tr><%-- _________________________________________________ --%>
	<tr> <%-- SUI 17 --%>
		<td></td>
		<td class="observationsQuestion">
			<spring:message code="patient.project.anyDiseasesSinceLast" />
		</td>
		<td>
			<form:select path="observations.anyCurrentDiseases"
				onchange="farv.displayAnyCurrentDiseases();compareAllObservationHistoryFields(true);" id="farv.anyCurrentDiseases"  >
				<option value=""></option>
				<form:options items="${form.dictionaryLists.YES_NO.list}"
					itemLabel="localizedName" itemValue="id" />
			</form:select>
			<div id="farv.anyCurrentDiseasesMessage" class="blank"></div>
		</td>
	</tr>
	<%-- Diseases --%>
	<c:forEach  var="disease" varStatus="iter" items="${form.observations.currentDiseasesList}">
		<tr id='farv.currentDiseasesRow${iter.index}' style="display: none">
			<%-- CLI 09.n --%>
			<td></td>
			<td class="observationsQuestion observationsSubquestion">
				<c:out value="${disease.value}" />
			</td>
			<td>
				<form:select path='observations.${disease.name}'
					onchange="makeDirty();compareAllObservationHistoryFields(true)"					
					id='farv.${disease.name}'
					>
					<option value=""></option>
					<form:options items="${form.dictionaryLists.YES_NO.list}" itemLabel="localizedName"
						itemValue="id" />
				</form:select>
				<div id="farv.${disease.name}.Message" class="blank"></div>
			</td>
		</tr>
	</c:forEach>
	<tr id="farv.currentDiseasesRow" style="display:none">
		<td></td>
		<td class="observationsQuestion observationsSubquestion">
			<spring:message code="patient.project.other"/>
		</td>
		<td>
			<form:select path="observations.currentDiseases"
				onchange="farv.displayCurrentDiseasesOther(this, 1);compareAllObservationHistoryFields(true);"
				id="farv.currentDiseases"  >
				<option value=""></option>
				<form:options items="${form.dictionaryLists.YES_NO.list}"
					itemLabel="localizedName" itemValue="id" />
			</form:select>
			<div id="currentDiseasesMessage" class="blank"></div>
		</td>
	</tr>
	<tr id="farv.currentDiseasesValueRow" style="display:none">
		<td class="required">*</td>
		<td class="observationsSubquestion">
			<spring:message code="patient.project.specify"/>
		</td>
		<td>
			<form:input path="observations.currentDiseasesValue"
				onchange="compareAllObservationHistoryFields(true);"
				id="farv.currentDiseasesValue"  />
			<div id="farv.currentDiseasesMessage" class="blank" ></div>
		</td>
	</tr>

	<tr> <%-- SUI 18 --%>
		<td></td>
		<td class="observationsQuestion">
			<spring:message code="patient.project.antiTbTreatment"/>
		</td>
		<td>
			<form:select path="observations.antiTbTreatment"
				onchange="compareAllObservationHistoryFields(true);"	id="farv.antiTbTreatment"  >
				<option value=""></option>
				<form:options items="${form.dictionaryLists.YES_NO.list}"
					itemLabel="localizedName" itemValue="id" />
			</form:select>
			<div id="farv.antiTbTreatmentMessage" class="blank"></div>
		</td>
	</tr>
	<tr> <%-- SUI 19 --%>
		<td></td>
		<td class="observationsQuestion">
			<spring:message code="patient.project.interruptedARVTreatment"/>
		</td>
		<td>
			<form:select path="observations.interruptedARVTreatment"
				onchange="farv.checkInterruptedARVTreatment();compareAllObservationHistoryFields(true);"	id="farv.interruptedARVTreatment"  >
				<option value=""></option>
				<form:options items="${form.dictionaryLists.YES_NO.list}"
					itemLabel="localizedName" itemValue="id" />
			</form:select>
			<div id="farv.interruptedARVTreatmentMessage" class="blank"></div>
		</td>
	</tr>
	<tr id="farv.priorARVTreatmentRow" style="display:none"> <%-- SUI 20 --%>
		<td></td>
		<td class="observationsSubquestion">
			<spring:message code="patient.project.onGoingARVTreatment"/>
		</td>
		<td>
			<form:select path="observations.priorARVTreatment"
				onchange="farv.checkPriorARVTreatment();compareAllObservationHistoryFields(true);compareAllObservationHistoryFields(true);"	id="farv.priorARVTreatment"  >
				<option value=""></option>
				<form:options items="${form.dictionaryLists.YES_NO_NA.list}"
					itemLabel="localizedName" itemValue="id" />
			</form:select>
			<div id="farv.priorARVTreatmentMessage" class="blank"></div>
		</td>
	</tr>
	<tr id="farv.onGoingARVTreatmentINNsRow" style="display:none"> <%--SUI 21 --%>
		<td></td>
		<td class="observationsSubSubquestion" colspan="2">
			<spring:message code="patient.project.onGoingARVTreatmentINNs" />
		</td>
	</tr>
	<c:forEach var="ongoingARVTreatment" varStatus="iter" items="${form.observations.priorARVTreatmentINNsList}" >
		<tr style="display:none" id='farv.priorARVTreatmentINNRow${iter.index}'><%-- SUI 21 --%>
			<td></td>
			<td class="bulletItem">${iter.index + 1})</td>
			<td>
				<form:input path='observations.priorARVTreatmentINNsList[${iter.index}]'
					onchange="compareAllObservationHistoryFields(true);"
					cssClass="text" id='farv.priorARVTreatmentINNs${iter.index}' />
				<div id='farv.priorARVTreatmentINNs${iter.index}Message' class="blank"></div>
			</td>
		</tr>
	</c:forEach>

	<tr id="farv.arvTreatmentAnyAdverseEffectsRow" style="display:none"> <%-- SUI 22 --%>
		<td></td>
		<td class="observationsSubSubquestion">
			<spring:message code="patient.project.treatmentAnyAdverseEffects"/>
		</td>
		<td>
			<form:select path="observations.arvTreatmentAnyAdverseEffects"
				onchange="farv.checkArvTreatmentAnyAdverseEffects();compareAllObservationHistoryFields(true);"	id="farv.arvTreatmentAnyAdverseEffects" >
				<option value=""></option>
				<form:options items="${form.dictionaryLists.YES_NO_NA.list}"
					itemLabel="localizedName" itemValue="id" />
			</form:select>
			<div id="farv.arvTreatmentAnyAdverseEffectsMessage" class="blank"></div>
		</td>
	</tr>
	<c:forEach var="adverseEffect" varStatus="iter" items="${form.observations.arvTreatmentAdverseEffects}" >
		<tr style="display:none" id='farv.arvTreatmentAdverseEffectsRow${iter.index}'><%-- SUI 22.n --%>
			<td ></td>
			<td style="text-align:right">
				<spring:message code="patient.project.treatmentAdverseEffects.type"/>
				<form:input path='observations.arvTreatmentAdverseEffects[${iter.index}].type'
					onchange="makeDirty();compareAllObservationHistoryFields(true);"
					id='farv.arvTreatmentAdvEffType${iter.index}'/>
					<div id="farv.arvTreatmentAdvEffType${iter.index}Message" class="blank"></div>
			</td>
			<td >
				<spring:message code="patient.project.treatmentAdverseEffects.grade"/>
				<form:input path='observations.arvTreatmentAdverseEffects[${iter.index}].grade'
					onchange="makeDirty();compareAllObservationHistoryFields(true);"
					id='farv.arvTreatmentAdvEffGrd${iter.index}' />
				<div id="farv.arvTreatmentAdvEffType${iter.index}Message" class="blank"></div>
				<div id="farv.arvTreatmentAdvEffGrd${iter.index}Message" class="blank"></div>
			</td>
		</tr>
	</c:forEach>
	<tr id="farv.arvTreatmentChangeRow" style="display:none"> <%-- SUI 23 --%>
		<td></td>
		<td class="observationsSubquestion">
			<spring:message code="patient.project.arvTreatmentChange"/>
		</td>
		<td>
			<form:select path="observations.arvTreatmentChange"
				onchange="compareAllObservationHistoryFields(true);" cssClass="text" id="farv.arvTreatmentChange"  >
				<option value=""></option>
				<form:options items="${form.dictionaryLists.YES_NO_NA.list}"
					itemLabel="localizedName" itemValue="id" />
			</form:select>
			<div id="farv.arvTreatmentChangeMessage" class="blank"></div>
		</td>
	</tr>
	<tr><td colspan="5"><hr/></td></tr><%-- __________________________________________________________________________________________________ --%>
	<tr> <%-- SUI 24 --%>
		<td></td>
		<td class="observationsQuestion">
			<spring:message code="patient.project.arvTreatmentNew"/>
		</td>
		<td>
			<form:select path="observations.arvTreatmentNew"
				onchange="farv.displayARVTreatmentNew();compareAllObservationHistoryFields(true);" id="farv.arvTreatmentNew" cssClass="text" >
				<option value=""></option>
				<form:options items="${form.dictionaryLists.YES_NO.list}" itemLabel="localizedName" itemValue="id" />
			</form:select>
			<div id="ARVTreatmentNewMessage" class="blank"></div>
		</td>
	</tr>
	<tr id="farv.arvTreatmentRegimeRow" style="display:none"> <%-- SUI 25 --%>
		<td></td>
		<td class="observationsSubquestion">
			<spring:message code="patient.project.arvTreatmentRegime"/>
		</td>
		<td>
			<form:select path="observations.arvTreatmentRegime"
				onchange="compareAllObservationHistoryFields(true);" id="farv.arvTreatmentRegime" cssClass="text" >
				<option value=""></option>
				<form:options items="${form.dictionaryLists.ARV_REGIME.list}" itemLabel="localizedName" itemValue="id" />
			</form:select>
			<div id="farv.arvTreatmentRegimeMessage" class="blank"></div>
		</td>
	</tr>
	<tr id="farv.prescribedARVTreatmentINNsRow" style="display:none"> <%--SUI 26 --%>
		<td></td>
		<td class="observationsSubquestion" colspan="2">
			<spring:message code="patient.project.prescribedARVTreatmentINNs" />
		</td>
	</tr>
	<c:forEach var="futureARVTreatment" varStatus="iter" items="${form.observations.futureARVTreatmentINNsList}" >
		<tr id='farv.futureARVTreatmentINNsRow${iter.index}' style="display:none"><%-- SUI 26.n --%>
			<td></td>
			<td class="bulletItem">${iter.index + 1})</td>
			<td>
				<form:input path='observations.futureARVTreatmentINNsList[${iter.index}]'
					onchange="compareAllObservationHistoryFields(true);"
					cssClass="text" id='farv.futureARVTreatmentINNs${iter.index}' />
				<div id="farv.futureARVTreatmentINNs_Message" class="blank"></div>
			</td>
		</tr>
	</c:forEach>
	<tr> <%-- SUI 27 --%>
		<td></td>
		<td>
			<spring:message code="patient.project.cotrimoxazoleTreatment" />
		</td>
		<td>
			<form:select path="observations.cotrimoxazoleTreatment"
				onchange="farv.displayCotriTreatment(); compareAllObservationHistoryFields(true);"	id="farv.cotrimoxazoleTreatment"  >
				<option value=""></option>
				<form:options items="${form.dictionaryLists.YES_NO.list}"
					itemLabel="localizedName" itemValue="id" />
			</form:select>
			<div id="farv.cotrimoxazoleTreatmentMessage" class="blank"></div>
		</td>
	</tr>
	<tr id="farv.cotrimoxazoleTreatmentAnyAdverseEffectsRow" style="display:none"> <%-- SUI 28 --%>
		<td></td>
		<td class="observationsSubquestion">
			<spring:message code="patient.project.treatmentAnyAdverseEffects"/>
		</td>
		<td>
			<form:select path="observations.cotrimoxazoleTreatmentAnyAdverseEffects"
				onchange="farv.displayCotriAdverseEffects();compareAllObservationHistoryFields(true);"	id="farv.cotrimoxazoleTreatAnyAdvEff"  >
				<option value=""></option>
				<form:options items="${form.dictionaryLists.YES_NO_NA.list}" itemLabel="localizedName" itemValue="id" />
			</form:select>
			<div id="farv.cotrimoxazoleTreatmentAnyAdverseEffectsMessage" class="blank"></div>
		</td>
	</tr>
	<c:forEach var="adverseEffect" varStatus="iter" items="${form.observations.cotrimoxazoleTreatmentAdverseEffects}" >
		<tr id='farv.cotrimoxazoleTreatAdvEffRow${iter.index}' style="display:none"><%-- SUI 29 --%>
			<td ></td>
			<td style="text-align:right">
				<spring:message code="patient.project.treatmentAdverseEffects.type"/>
				<form:input path='observations.cotrimoxazoleTreatmentAdverseEffects[${iter.index}].type'
					onchange="compareAllObservationHistoryFields(true);" id='farv.cotrimoxazoleTreatAdvEffType${iter.index}>'/>
					<div id="farv.cotrimoxazoleTreatmentAnyAdverseEffectsMessage" class="blank"></div>
			</td>
			<td>
				<spring:message code="patient.project.treatmentAdverseEffects.grade"/>
				<form:input path='observations.cotrimoxazoleTreatmentAdverseEffects[${iter.index}].grade'
					onchange="compareAllObservationHistoryFields(true);" id='farv.cotrimoxazoleTreatAdvEffGrd${iter.index}' />
					<div id="farv.cotrimoxazoleTreatAdvEffGrd${iter.index}Message" class="blank"></div>
			</td>
		</tr>
	</c:forEach>
	<tr> <%-- SUI 30 --%>
		<td ></td>
		<td class="observationsQuestion">
			<spring:message code="patient.project.anySecondaryTreatment"/>
		</td>
		<td>
			<form:select path="observations.anySecondaryTreatment"
				onchange="farv.displayAny2ndTreatment();compareAllObservationHistoryFields(true);"	id="farv.anySecondaryTreatment"  >
				<option value=""></option>
				<form:options items="${form.dictionaryLists.YES_NO_NA.list}"
					itemLabel="localizedName" itemValue="id" />
			</form:select>
			<div id="farv.anySecondaryTreatmentMessage" class="blank"></div>
		</td>
	</tr>
	<tr id="farv.secondaryTreatmentRow" style="display:none"> <%-- SUI 31 --%>
		<td></td>
		<td class="observationsQuestion observationsSubquestion" >
			<spring:message code="patient.project.secondaryTreatment" />
		</td>
		<td>
			<form:select path="observations.secondaryTreatment"
				onchange="compareAllObservationHistoryFields(true);" id="farv.secondaryTreatment" cssClass="text"  >
				<option value=""></option>
				<form:options items="${form.dictionaryLists.ARV_PROPHYLAXIS_2.list}" itemLabel="localizedName" itemValue="id" />
			</form:select>
			<div id="farv.secondaryTreatmentMessage" class="blank"></div>
		</td>
	</tr>
	<tr><%-- SUI 32 --%>
		<td></td>
		<td><spring:message code="patient.project.clinicVisits" /></td>
		<td>
			<form:input path="observations.clinicVisits"
				onchange="compareAllObservationHistoryFields(true);"
				cssClass="text" id="farv.clinicVisits"  /> 
			<div id="observations.clinicVisitsMessage" class="blank"></div>
		</td>
	</tr>
	<tr><td colspan="5"><hr/></td></tr><%-- __________________________________________________________________________________________________ --%>
	 	<tr id="farv.patientRecordStatusRow" style="display: none;">
		<td class="required"></td>
		<td>
			<spring:message code="patient.project.patientRecordStatus" />
		</td>
		<td>
		<input type="text" id="farv.PatientRecordStatus" size="20" class="text readOnly" disabled="disabled" readonly="readonly" />
		<div id="farv.PatientRecordStatusMessage" class="blank"></div>
		</td>
    </tr>
 	<tr id="farv.sampleRecordStatusRow" style="display: none;">
		<td class="required"></td>
		<td>
			<spring:message code="patient.project.sampleRecordStatus" />
		</td>
		<td>
		<input type="text" id="farv.SampleRecordStatus" size="20" class="text readOnly" disabled="disabled" readonly="readonly" />
		<div id="farv.SampleRecordStatusMessage" class="blank"></div>
		</td>
    </tr>
	<tr id="farv.underInvestigationRow">
		<td class="required"></td>
		<td>
			<spring:message code="patient.project.underInvestigation" />
		</td>
		<td>
			<form:select path="observations.underInvestigation" onchange="makeDirty();compareAllObservationHistoryFields(true)"
			id="farv.underInvestigation">
			<option value=""></option>
			<form:options items="${form.dictionaryLists.YES_NO.list}" itemLabel="localizedName"
				itemValue="id" />
			</form:select>
			<div id="farv.underInvestigationMessage" class="blank"></div>
		</td>
    </tr>    
	<tr id="farv.underInvestigationCommentRow" >
		<td class="required"></td>
		<td>
			<spring:message code="patient.project.underInvestigationComment" />
		</td>
		<td colspan="3">
			<form:input path="projectData.underInvestigationNote" maxlength="1000" size="80"
				onchange="makeDirty();" id="farv.underInvestigationComment" />
				<div id="farv.underInvestigationCommentMessage" class="blank"></div>
		</td>
    </tr>
</table>
