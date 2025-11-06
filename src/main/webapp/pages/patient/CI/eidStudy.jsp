<%@ page language="java" contentType="text/html; charset=UTF-8"
    import="org.openelisglobal.common.action.IActionConstants,
            org.openelisglobal.common.util.DateUtil,
            org.openelisglobal.internationalization.MessageUtil"%>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>


  
<script type="text/javascript">
function EidProjectChecker() {
    this.idPre = "eid.";

    // eid.eidTypeOfClinicOther
    this.displayTypeOfClinicOther = function () {
        var field = $("eid.eidTypeOfClinic");
        enableSelectedRequiredSubfield(field, $("eid.eidTypeOfClinicOther"), -1, "eid.eidTypeOfClinicOtherRow" );
    }

    this.refresh = function () {
        this.refreshBase();         
        this.displayTypeOfClinicOther();
        var s = $("eid.subjectNumber").value;
        $("eid.infantID").value   = s.slice(-4);     // last 4 chars
        $("eid.codeSiteID").value = s.slice(3,-4); // and the numbers before that, without DBS
        
    }

    this.checkAllSampleItemFields = function () {
        this.checkSampleItem($('eid.dryTubeTaken'));
        this.checkSampleItem($('eid.dbsTaken'));
        this.checkSampleItem($('eid.dbsTaken'), $('eid.dnaPCR'));
    }
    
    this.checkEIDWhichPCR = function (field) {
        makeDirty();
        if (field.selectedIndex == 1) {
            var otherField = $("eid.reasonForSecondPCRTest"); 
            otherField.selectedIndex = otherField.options.length - 1;  
        }
        compareAllObservationHistoryFields(true)
    }
    
    this.setFieldsForEdit = function(canEditPatientIDs, canEditSampleIDs) {
        this.setFieldReadOnly("labNoForDisplay", !canEditSampleIDs);
        this.setFieldReadOnly("infantID", !canEditPatientIDs);
        this.setFieldReadOnly("codeSiteID", !canEditPatientIDs);
        this.setFieldReadOnly("siteSubjectNumber", !canEditPatientIDs);
    }   
}
EidProjectChecker.prototype = new BaseProjectChecker();
eid = new EidProjectChecker();
</script>

<h2><spring:message code="sample.entry.project.EID.title"/></h2>   
<table width="100%">
    <tr>
        <td class="required" width="2%">*</td>
        <td width="28%">
            <spring:message code="sample.entry.project.receivedDate"/>&nbsp;<%=DateUtil.getDateUserPrompt()%>
        </td>
        <td width="70%">
        <form:input path="receivedDateForDisplay"
                onkeyup="addDateSlashes(this, event);"
                onchange="eid.checkReceivedDate(false)"
                cssClass="text"
                id="eid.receivedDateForDisplay" maxlength="10"/>
                <div id="eid.receivedDateForDisplayMessage" class="blank" ></div>
        </td>
    </tr>
    <tr>
        <td></td>
        <td>
             <spring:message code="sample.entry.project.receivedTime" />&nbsp;<spring:message code="sample.military.time.format"/>
        </td>
        <td>
        <form:input path="receivedTimeForDisplay"
            onkeyup="filterTimeKeys(this, event);"              
            onblur="eid.checkReceivedTime(true);"
            cssClass="text"
            id="eid.receivedTimeForDisplay" maxlength="5"/>
            <div id="eid.receivedTimeForDisplayMessage" class="blank" ></div>
        </td>
    </tr>
    <tr>
        <td class="required">*</td>
        <td>
            <spring:message code="sample.entry.project.dateTaken"/>&nbsp;<%=DateUtil.getDateUserPrompt()%>
        </td>
        <td>
        <form:input path="interviewDate"
                onkeyup="addDateSlashes(this, event);"
                onchange="eid.checkInterviewDate(true);"
                cssClass="text"
                id="eid.interviewDate" maxlength="10"/>
                <div id="eid.interviewDateMessage" class="blank" ></div>
        </td>
    </tr>
    <tr>
        <td></td>
        <td>
            <spring:message code="sample.entry.project.timeTaken"/>&nbsp;<spring:message code="sample.military.time.format"/>
        </td>
        <td>
        <form:input path="interviewTime"
                onkeyup="filterTimeKeys(this, event);"                
                onblur="eid.checkInterviewTime(true);"
                cssClass="text"
                id="eid.interviewTime" maxlength="5"/>
                <div id="eid.interviewTimeMessage" class="blank" ></div>
        </td>
    </tr>       
    <tr>
        <td class="required">*</td>
        <td>
            <spring:message code="sample.entry.project.siteName"/>
        </td>

        <td>
            <form:select path="projectData.EIDSiteName"
                         id="eid.centerName"
                         onchange="eid.checkCenterName(true)">
                <option value=""></option>
                <form:options items="${form.organizationTypeLists['EID_ORGS_BY_NAME'].list}"
                    itemLabel="organizationName"
                    itemValue="id" />
            </form:select>
            <div id="eid.centerNameMessage" class="blank" ></div>
        </td>
    </tr>
    <tr>
        <td class="required">*</td>
        <td><spring:message code="sample.entry.project.siteCode"/></td>
        <td style="width: 40%;">
            <form:select path="projectData.EIDsiteCode" cssClass="text"
                    id="eid.centerCode"
                    onchange="eid.checkCenterCode(true);" >
                <option value=""></option>
                <form:options items="${form.organizationTypeLists['EID_ORGS'].list}" itemLabel="doubleName" itemValue="id" />
            </form:select>
            <div id="eid.centerCodeMessage" class="blank"></div>
        </td>

    </tr>
    <tr>
        <td class="required">+</td>
        <td><spring:message code="sample.entry.project.EID.infantNumber"/></td>
        <td>
            <div class="blank">DBS</div>
            <INPUT type="text" name="eid.codeSiteId" id="eid.codeSiteID" size="4" class="text"
                onchange="handleDBSSubjectId(); makeDirty();"
                maxlength="4" />
            <INPUT type="text" name="eid.infantID" id="eid.infantID" size="4" class="text"
                onchange="handleDBSSubjectId(); makeDirty();"
                maxlength="4" />
            <form:input path="subjectNumber"
                    cssClass="text" style="display:none;"
                    id="eid.subjectNumber"
                    onchange="checkRequiredField(this); makeDirty();" />
            <div id="eid.subjectNumberMessage" class="blank" ></div>
        </td>
    </tr>
    <tr>
        <td class="required">+</td>
        <td><spring:message code="sample.entry.project.EID.siteInfantNumber"/></td>
        <td>
            <form:input path="siteSubjectNumber"
                id="eid.siteSubjectNumber"
                cssClass="text"
                onkeyup="addPatientCodeSlashes(this, event);"
                onchange="eid.checkSiteSubjectNumber(true);validateSiteSubjectNumber(this)"  maxlength="18"/>
            <div id="eid.siteSubjectNumberMessage" class="blank"></div>
        </td>
    </tr>
    <tr>
        <td class="required">*</td>
        <td>
            <%=MessageUtil.getContextualMessage("quick.entry.accession.number")%>
        </td>
        <td>
            <div class="blank"><spring:message code="sample.entry.project.LDBS"/></div>
            <INPUT type="text" name="eid.labNoForDisplay" id="eid.labNoForDisplay" size="5" class="text"
                onchange="handleLabNoChange( this, '<spring:message code="sample.entry.project.LDBS"/>', false );makeDirty();"
                maxlength="5" />
            <form:input path="labNo"
                    cssClass="text" style="display:none;"
                    id="eid.labNo" />
            <div id="eid.labNoMessage" class="blank"></div>
        </td>
    </tr>
    <tr>
        <td></td>
        <td class="observationsQuestion"><spring:message code="patient.project.eidWhichPCR" /></td>
        <td>
            <form:select path="observations.whichPCR"
                    cssClass="text" id="eid.whichPCR"
                    onchange="eid.checkEIDWhichPCR(this)" >
                <option value=""></option>
                <form:options items="${form.dictionaryLists['EID_WHICH_PCR'].list}" itemLabel="localizedName" itemValue="id" />
            </form:select>
            <div id="eid.whichPCRMessage" class="blank"></div>
        </td>
    </tr>

    <tr>
        <td></td>
        <td class="observationsQuestion"><spring:message code="sample.entry.project.EID.reasonForPCRTest" /></td>
        <td>
            <form:select path="observations.reasonForSecondPCRTest" cssClass="text" id="eid.reasonForSecondPCRTest"
                    onchange="makeDirty();compareAllObservationHistoryFields(true)" >
                <option value=""></option>
                <form:options items="${form.dictionaryLists['EID_SECOND_PCR_REASON'].list}" itemLabel="localizedName" itemValue="id" />
            </form:select>
            <div id="eid.reasonForSecondPCRTestMessage" class="blank"></div>
        </td>
    </tr>

    <tr>
        <td></td>
        <td>
            <spring:message code="patient.project.nameOfRequestor" />
        </td>
        <td>
            <form:input path="observations.nameOfRequestor"
                      onchange="makeDirty();compareAllObservationHistoryFields(true)"
                      cssClass="text"
                      id="eid.nameOfRequestor" size="50"/>
            <div id="eid.nameOfRequestorMessage" class="blank"></div>
        </td>
    </tr>
    <tr>
        <td></td>
        <td>
            <spring:message code="patient.project.nameOfSampler" />
        </td>
        <td>
            <form:input path="observations.nameOfSampler"
                      onchange="makeDirty();compareAllObservationHistoryFields(true)"
                      cssClass="text"
                      id="eid.nameOfSampler" size="50"/>
            <div id="eid.nameOfSamplerMessage" class="blank"></div>
        </td>
    </tr>
        <tr>
            <td></td>
            <td colspan="3" class="sectionTitle">
                <spring:message code="sample.entry.project.title.infantInformation" />
            </td>
        </tr>

        <tr>
            <td class="required">*</td>
            <td>
                <spring:message code="patient.birthDate" />&nbsp;<%=DateUtil.getDateUserPrompt()%>
            </td>
            <td>
                <form:input path="birthDateForDisplay"
                      cssClass="text"
                      size="20"
                      maxlength="10"
                      onkeyup="addDateSlashes(this, event);"
                      onchange="eid.checkDateOfBirth(false);"
                      id="eid.dateOfBirth"
                      />
                <div id="eid.dateOfBirthMessage" class="blank" ></div>
            </td>
        </tr>
        <tr>
            <td class="required"></td>
            <td>
                <spring:message code="patient.age" />
            </td>
            <td>
                <label for="eid.month" ><spring:message code="label.month" /></label>
                <INPUT type="text" name="age" id="eid.month" size="3"
                    onchange="eid.checkAge( this, true, 'month' );clearField('eid.ageWeek');"
                    maxlength="2" />
                <label for="eid.ageWeek" ><spring:message code="label.week" /></label>
                <INPUT type="text" name="ageWeek" id="eid.ageWeek" size="3"
                    onchange="eid.checkAge( this, true, 'week' ); clearField('eid.month');"
                    maxlength="2" />
                <div id="eid.ageMessage" class="blank" ></div>
            </td>
        </tr>

        <tr>
            <td class="required">*</td>
            <td>
                <spring:message code="patient.gender" />
            </td>
            <td>
                <form:select path="gender"
                         id="eid.gender"
                         onchange="eid.checkGender(true)" >
                <option value=""></option>
                <form:options items="${form.formLists['GENDERS']}"
                    itemLabel="localizedName" itemValue="id" />
                </form:select>
                <div id="eid.genderMessage" class="blank" ></div>
            </td>
        </tr>
    <tr>
        <td></td>
        <td class="observationsQuestion"><spring:message code="patient.project.eidBenefitPTME" /></td>
        <td>
            <form:select path="observations.eidInfantPTME" cssClass="text" id="eid.eidInfantPTME"
                    onchange="makeDirty();compareAllObservationHistoryFields(true)" >
                <option value=""></option>
                <form:options items="${form.dictionaryLists['YES_NO'].list}" itemLabel="localizedName" itemValue="id" />
            </form:select>
            <div id="eid.InfantPTMEMessage" class="blank"></div>
        </td>
    </tr>
    <tr>
        <td></td>
        <td class="observationsQuestion"><spring:message code="patient.project.eidTypeOfClinic" /></td>
        <td>
            <form:select path="observations.eidTypeOfClinic" cssClass="text" id="eid.eidTypeOfClinic"
                    onchange="makeDirty();projectChecker.displayTypeOfClinicOther();compareAllObservationHistoryFields(true)" >
                <option value=""></option>
                <form:options items="${form.dictionaryLists['EID_TYPE_OF_CLINIC'].list}" itemLabel="localizedName" itemValue="id" />
            </form:select>
            <div id="eid.eidTypeOfClinicMessage" class="blank"></div>
        </td>
    </tr>
    <tr id="eid.eidTypeOfClinicOtherRow" style="display: none">
        <td>
        </td>
        <td class="observationsSubquestion"><em><spring:message code="patient.project.specify" /></em></td>
        <td>
            <form:input path="observations.eidTypeOfClinicOther"
                      onchange="makeDirty();compareAllObservationHistoryFields(true)"
                      cssClass="text"
                      id="eid.eidTypeOfClinicOther" />
            <div id="eid.eidTypeOfClinicOtherMessage" class="blank"></div>
        </td>
    </tr>
    <tr>
        <td></td>
        <td class="observationsQuestion"><spring:message code="patient.project.eidHowChildFed" /></td>
        <td>
            <form:select path="observations.eidHowChildFed" cssClass="text" id="eid.eidHowChildFed"
                    onchange="makeDirty();compareAllObservationHistoryFields(true)" >
                <option value=""></option>
                <form:options items="${form.dictionaryLists['EID_HOW_CHILD_FED'].list}" itemLabel="localizedName" itemValue="id" />
            </form:select>
            <div id="eid.eidHowChildFedMessage" class="blank"></div>
        </td>
    </tr>
    <tr>
        <td></td>
        <td class="observationsQuestion"><spring:message code="patient.project.eidStoppedBreastfeeding"/></td>
        <td>
            <form:select path="observations.eidStoppedBreastfeeding" cssClass="text" id="eid.eidStoppedBreastfeeding"
                    onchange="makeDirty();compareAllObservationHistoryFields(true)" >'
                <option value=""></option>
                <form:options items="${form.dictionaryLists['EID_STOPPED_BREASTFEEDING'].list}" itemLabel="localizedName" itemValue="id" />
            </form:select>
            <div id="eid.eidStoppedBreastfeedingMessage" class="blank"></div>
        </td>
    </tr>

    <tr>
        <td></td>
        <td class="observationsQuestion"><spring:message code="patient.project.eidInfantSymptomatic" /></td>
        <td>
            <form:select path="observations.eidInfantSymptomatic" cssClass="text" id="eid.eidInfantSymptomatic"
                    onchange="makeDirty();compareAllObservationHistoryFields(true)" >
                <option value=""></option>
                <form:options items="${form.dictionaryLists['YES_NO'].list}" itemLabel="localizedName" itemValue="id" />
            </form:select>
            <div id="eid.eidInfantSymptomaticMessage" class="blank"></div>
        </td>
    </tr>
        <td></td>
            <td class="observationsQuestion"><spring:message code="patient.project.eidInfantProphy" /></td>
        <td>
            <form:select path="observations.eidInfantsARV" cssClass="text" id="eid.eidInfantsARV"
                    onchange="makeDirty();compareAllObservationHistoryFields(true)" >
                <option value=""></option>
                <form:options items="${form.dictionaryLists['EID_INFANT_PROPHYLAXIS_ARV'].list}" itemLabel="localizedName" itemValue="id" />
            </form:select>
            <div id="eid.eidInfantsARVMessage" class="blank"></div>
        </td>
    </tr>
    <tr>
        <td></td>
            <td class="observationsQuestion"><spring:message code="patient.project.eidInfantCotrimoxazole" /></td>
        <td>
            <form:select path="observations.eidInfantCotrimoxazole" cssClass="text" id="eid.eidInfantCotrimoxazole"
                    onchange="makeDirty();compareAllObservationHistoryFields(true)" >
                <option value=""></option>
                <form:options items="${form.dictionaryLists['YES_NO_UNKNOWN'].list}" itemLabel="localizedName" itemValue="id" />
            </form:select>
            <div id="eid.eidInfantCotrimoxazoleMessage" class="blank"></div>
        </td>
    </tr>
    <tr>
        <td></td>
        <td colspan="3" class="sectionTitle">
            <spring:message code="sample.entry.project.title.mothersInformation" />
        </td>
    </tr>   
    <tr>
        <td></td>
        <td class="observationsQuestion"><spring:message code="patient.project.eidMothersStatus" /></td>
        <td>
            <form:select path="observations.eidMothersHIVStatus" cssClass="text" id="eid.eidMothersHIVStatus"
                    onchange="makeDirty();compareAllObservationHistoryFields(true)" >
                <option value=""></option>
                <form:options items="${form.dictionaryLists['EID_MOTHERS_HIV_STATUS'].list}" itemLabel="localizedName" itemValue="id" />
            </form:select>
            <div id="eid.eidMothersHIVStatusMessage" class="blank"></div>
        </td>
    </tr>
    <tr>
        <td></td>
        <td class="observationsQuestion"><spring:message code="patient.project.eidMothersARV" /></td>
        <td>
            <form:select path="observations.eidMothersARV" cssClass="text" id="eid.eidMothersARV"
                    onchange="makeDirty();compareAllObservationHistoryFields(true)" >
                <option value=""></option>
                <form:options items="${form.dictionaryLists['EID_MOTHERS_ARV_TREATMENT'].list}" itemLabel="localizedName" itemValue="id" />
            </form:select>
            <div id="eid.eidMothersARVMessage" class="blank"></div>
        </td>
    </tr>
    <tr>
        <td>&nbsp;</td>
    </tr>

    <tr><td colspan="5"><hr/></td></tr>
    <tr id="eid.patientRecordStatusRow"style="display: none;">
        <td class="required"></td>
        <td>
            <spring:message code="patient.project.patientRecordStatus" />
        </td>
        <td>
        <INPUT type="text" id="eid.PatientRecordStatus" size="20" class="readOnly text" disabled="disabled" readonly="readonly" />
        <div id="eid.PatientRecordStatusMessage" class="blank"></div>
        </td>
    </tr>
    <tr id="eid.sampleRecordStatusRow" style="display: none;">
        <td class="required"></td>
        <td>
            <spring:message code="patient.project.sampleRecordStatus" />
        </td>
        <td>
        <INPUT type="text" id="eid.SampleRecordStatus" size="20" class="readOnly text" disabled="disabled" readonly="readonly" />
        <div id="eid.SampleRecordStatusMessage" class="blank"></div>
        </td>
    </tr>
    <tr><td colspan="6"><hr/></td></tr>
    <tr id="eid.underInvestigationRow">
        <td class="required"></td>
        <td>
            <spring:message code="patient.project.underInvestigation" />
        </td>
        <td>
            <form:select path="observations.underInvestigation" onchange="makeDirty();compareAllObservationHistoryFields(true)"
            id="eid.underInvestigation">
            <option value=""></option>
            <form:options items="${form.dictionaryLists['YES_NO'].list}" itemLabel="localizedName"
                itemValue="id" />
            </form:select>
            <div id="eid.underInvestigationMessage" class="blank" ></div>
        </td>
    </tr>    
    <tr id="eid.underInvestigationCommentRow" >
        <td class="required"></td>
        <td>
            <spring:message code="patient.project.underInvestigationComment" />
        </td>
        <td colspan="3">
            <form:input path="projectData.underInvestigationNote" maxlength="1000" size="80"
                onchange="makeDirty();" id="eid.underInvestigationComment" />
                <div id="eid.underInvestigationMessage" class="blank" ></div>
        </td>
    </tr>
</table>
