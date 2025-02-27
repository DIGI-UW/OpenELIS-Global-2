<%@ page language="java" contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants,
	        org.openelisglobal.common.util.DateUtil,org.openelisglobal.internationalization.MessageUtil"%>

<%@ page isELIgnored="false"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<script type="text/javascript">
	/**
	 * an object gathers all the methods about tweeking the VL form together in one place.
	 **/
	
	function RecencyProjectChecker() {
		this.idPre = "rt.";
	}

	RecencyProjectChecker.prototype = new BaseProjectChecker();
	rt = new RecencyProjectChecker();
	
	
</script>
<h2>
	<spring:message code="sample.entry.project.RT.title" />
</h2>
<table>
			<tr>
				<td></td>
				<td colspan="2" class="sectionTitle"><spring:message
						code="sample.entry.project.title.org" /></td>
			</tr>
			<tr>
				<td class="required">*</td>
				<td><spring:message code="patient.project.centerCode" /></td>
				<td><form:select path="ProjectData.ARVcenterCode"
						id="rt.centerCode" onchange="rt.checkCenterCode(true)" class="centerCodeClass">
						<form:option value="">&nbsp;</form:option>
						<form:options items="${form.organizationTypeLists.ARV_ORGS_BY_NAME.list}"
							itemLabel="doubleName" itemValue="id" />
					</form:select><div id="rt.centerCodeMessage" class="blank"></div></td>
			</tr>
			<tr>
				<td></td>
				<td colspan="2" class="sectionTitle"><spring:message
						code="sample.entry.project.title.patientInfo" /></td>
			</tr>
			<tr>
				<td class="required">*</td>
				<td><%=MessageUtil.getContextualMessage("quick.entry.accession.number")%>
				</td>
				<td>
					<div class="blank">
						<spring:message code="sample.entry.project.RT" />
					</div> <INPUT type="text" name="rt.labNoForDisplay"
					id="rt.labNoForDisplay" size="5" class="text"
					onchange="handleLabNoChange( this, '<spring:message code="sample.entry.project.RT"/>', 'false' );makeDirty();"
					maxlength="5" /> <form:input path="labNo" cssClass="text"
						style="display:none;" id="rt.labNo" />
					<div id="rt.labNoMessage" class="blank"></div>
				</td>
			</tr>
			<tr>
				<td class="required">*</td>
				<td><spring:message code="sample.entry.project.recencyNumber" /></td>
				<td><form:input path="siteSubjectNumber" cssClass="text"
						id="rt.siteSubjectNumber" maxlength="18" onchange="" />
					<div id="rt.siteSubjectNumberMessage" class="blank"></div></td>
			</tr>
			<tr>
				<td class="required">*</td>
				<td><spring:message code="patient.birthDate" />&nbsp;<%=DateUtil.getDateUserPrompt()%>
				</td>
				<td><form:input path="birthDateForDisplay" cssClass="text"
						onkeyup="addDateSlashes(this, event);"
						onchange="rt.checkDateOfBirth(false);"  id="rt.dateOfBirth" maxlength="10" />
					<div id="rt.dateOfBirthMessage" class="blank"></div></td>
			</tr>
			<tr>
				<td></td>
				<td><spring:message code="patient.age" /></td>
				<td><label for="age"><spring:message code="label.year" /></label>
					<INPUT type='text' name='age' id="rt.age" size="3"
					onchange="rt.checkAge( this, true, 'year' );"
					maxlength="2" />
					<div id="ageMessage" class="blank"></div></td>
			</tr>
			<tr>
				<td class="required">*</td>
				<td><spring:message code="patient.project.gender" /></td>
				<td><form:select path="gender" id="rt.gender"
						onchange="rt.checkGender(true);rt.checkGenderForVlPregnancyOrSuckle()">
						<form:option value="">&nbsp;</form:option>
						<form:options items="${form.formLists.GENDERS}"
							itemLabel="localizedName" itemValue="id" />
					</form:select>
					<div id="rt.genderMessage" class="blank"></div></td>
			</tr>
			<tr id="rt.vlPregnancyRow" style="display: none">
				<td></td>
				<td><spring:message code="sample.project.vlPregnancy" /></td>
				<td><form:select path="observations.vlPregnancy"
						id="rt.vlPregnancy" onchange="">
						<form:option value="">&nbsp;</form:option>
						<form:options items="${form.dictionaryLists.YES_NO.list}"
							itemLabel="localizedName" itemValue="id" />
					</form:select><div id="rt.vlPregnancyMessage" class="blank"></div></td>
			</tr>
			<tr id="rt.vlSuckleRow" style="display: none">
				<td></td>
				<td><spring:message code="sample.project.vlSuckle" /></td>
				<td><form:select path="observations.vlSuckle" id="rt.vlSuckle"
						onchange="">
						<form:option value="">&nbsp;</form:option>
						<form:options items="${form.dictionaryLists.YES_NO.list}"
							itemLabel="localizedName" itemValue="id" />
					</form:select><div id="rt.vlSuckleMessage" class="blank"></div></td>
			</tr>
			<tr>
				<td class="required">*</td>
				<td><spring:message code="patient.project.hivType" /></td>
				<td><form:select path="observations.hivStatus"
						id="rt.hivStatus" onchange="">
						<form:option value="">&nbsp;</form:option>
						<form:options items="${form.dictionaryLists.HIV_TYPES.list}"
							itemLabel="localizedName" itemValue="id" />
					</form:select>
					<div id="hivStatusMessage" class="blank"></div></td>
			</tr>
			<tr>
				<td></td>
				<td colspan="2" class="sectionTitle"><spring:message
						code="sample.entry.project.title.sample" /></td>
			</tr>
			<tr>
				<td></td>
				<td><spring:message code="patient.project.nameOfClinician" /></td>
				<td><form:input path="observations.nameOfDoctor"
						cssClass="text" id="rt.nameOfDoctor" size="50"
						onchange="makeDirty();compareAllObservationHistoryFields(true)" />
					<div id="rt.nameOfDoctorMessage" class="blank"></div></td>
			</tr>
			<tr>
				<td></td>
				<td><spring:message code="patient.project.nameOfSampler" /></td>
				<td><form:input path="observations.nameOfSampler"
						cssClass="text" id="rt.nameOfSampler" size="50"
						onchange="makeDirty();compareAllObservationHistoryFields(true)" />
					<div id="rt.nameOfSamplerMessage" class="blank"></div></td>
			</tr>
			<tr>
				<td class="required">*</td>
				<td><spring:message code="sample.entry.project.receivedDate" />&nbsp;<%=DateUtil.getDateUserPrompt()%>
				</td>
				<td><form:input path="receivedDateForDisplay" cssClass="text"
						onkeyup="" onchange="" id="rt.receivedDateForDisplay"
						maxlength="10" />
					<div id="receivedDateForDisplayMessage" class="blank"></div></td>
			</tr>
			<tr>
				<td></td>
				<td><spring:message code="sample.entry.project.receivedTime" />&nbsp;<spring:message
						code="sample.military.time.format" /></td>
				<td><form:input path="receivedTimeForDisplay" cssClass="text"
						id="rt.receivedTimeForDisplay" maxlength="5" />
						<div id="rt.receivedTimeForDisplayMessage" class="blank"></div></td>
			</tr>
			<tr>
				<td class="required">*</td>
				<td><spring:message code="sample.entry.project.dateTaken" />&nbsp;<%=DateUtil.getDateUserPrompt()%>
				</td>
				<td><form:input path="interviewDate" onkeyup="" onchange=""
						cssClass="text" id="rt.interviewDate" maxlength="10" />
					<div id="rt.interviewDateMessage" class="blank"></div></td>
			</tr>
			<tr>
				<td></td>
				<td><spring:message code="sample.entry.project.timeTaken" />&nbsp;<spring:message
						code="sample.military.time.format" /></td>
				<td><form:input path="interviewTime" onkeyup="" onblur=""
						cssClass="text" id="rt.interviewTime" maxlength="5" />
						<div id="rt.interviewTimeMessage" class="blank"></div></td>
			</tr>
			<tr>
				<td></td>
				<td colspan="2" class="sectionTitle"><spring:message
						code="sample.entry.project.title.sampleType" /></td>
			</tr>
			<tr>
				<td></td>
				<td><spring:message code="sample.entry.project.recency.plasma" /></td>
				<td><form:checkbox path="ProjectData.plasmaTaken"
						checked="checked" onchange="rt.checkSampleItem($('rt.plasmaTaken'));checkVLSampleType(this);"
						 id="rt.plasmataken"/>
						<div id="rt.plasmaTakenMessage" class="blank"></div></td>
			</tr>
			<tr>
				<td></td>
				<td><spring:message code="sample.entry.project.recency.serum" /></td>
				<td><form:checkbox path="ProjectData.serumTaken"
						id="rt.serumTaken" onchange="rt.checkSampleItem($('rt.serumTaken'));checkVLSampleType(this);" />
						<div id="rt.serumTakenMessage" class="blank"></div></td>
			</tr>
			<tr>
				<td></td>
				<td colspan="2" class="sectionTitle"><spring:message
						code="sample.entry.project.title.tests" /></td>
			</tr>
			<tr>
				<td></td>
				<td><spring:message
						code="sample.entry.project.recency.asanteKit" /></td>
				<td><form:checkbox path="ProjectData.asanteTest"
						id="rt.asanteTest" onchange="vl.checkSampleItem($('vl.plasmaTaken'), this);" />
						<div id="rt.asanteTestMessage" class="blank"></div></td>
			</tr>
		</table>
