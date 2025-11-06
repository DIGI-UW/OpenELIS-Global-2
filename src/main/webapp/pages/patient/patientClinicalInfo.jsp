<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page import="org.openelisglobal.common.action.IActionConstants,
                 org.openelisglobal.common.formfields.FormFields,
                 org.openelisglobal.common.formfields.FormFields.Field,
                 org.openelisglobal.common.util.Versioning,
                 org.openelisglobal.patient.action.bean.PatientManagementInfo" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<c:set var="formName" value="${form.formName}" />


<script type="text/javascript" src="scripts/utilities.js?" ></script>

<script type="text/javascript" >


</script>

<%-- <nested:hidden name='${form.formName}' property="patientProperties.currentDate" id="currentDate"/> --%>

<div id="PatientClinicalPage" style="display:inline"  >
	<% if( FormFields.getInstance().useField(Field.SampleEntryPatientClinical)){ %>
	<h1><spring:message code="patient.clinical.head" /></h1><h2><spring:message code="patient.clinical.treatmentStatus"/></h2><br>
	<h3><spring:message code="patient.clinical.history"/></h3>
	<table style="width:80%">
		<tr>
			<td width="37%">(1)<spring:message code="patient.clinical.history.tb"/></td>
			<td width="10%">Oui/Non/Unk</td>
			<td width="6%">&nbsp;</td>
			<td width="37%">(2)<spring:message code="patient.clinical.history.std"/></td>
			<td width="10%">Oui/Non/Unk</td>
		</tr>
		<tr>
			<td align="right"><spring:message code="patient.clinical.tb.extraPulmonary" /></td>
			<td><input type="radio" name="patientClinicalProperties.tbExtraPulmanary" value="yes" >
			    <input type="radio" name="patientClinicalProperties.tbExtraPulmanary" value="no" >
			    <input type="radio" name="patientClinicalProperties.tbExtraPulmanary" value="unknown" checked="checked">
			</td>
			<td>&nbsp;</td>
			<td align="right"><spring:message code="patient.clinical.std.colon" /></td>
			<td><input type="radio" name="patientClinicalProperties.stdColonCancer" value="yes" >
			    <input type="radio" name="patientClinicalProperties.stdColonCancer" value="no" >
			    <input type="radio" name="patientClinicalProperties.stdColonCancer" value="unknown" checked="checked">
			</td>    
		</tr>
		<tr>
			<td align="right"><spring:message code="patient.clinical.tb.cerebral" /></td>
			<td><input type="radio" name="patientClinicalProperties.tbCerebral" value="yes" >
			    <input type="radio" name="patientClinicalProperties.tbCerebral" value="no" >
			    <input type="radio" name="patientClinicalProperties.tbCerebral" value="unknown" checked="checked">
			</td>
			<td>&nbsp;</td>
			<td align="right"><spring:message code="patient.clinical.std.candidose" /></td>
			<td><input type="radio" name="patientClinicalProperties.stdCandidiasis" value="yes" >
			    <input type="radio" name="patientClinicalProperties.stdCandidiasis" value="no" >
			    <input type="radio" name="patientClinicalProperties.stdCandidiasis" value="unknown" checked="checked">
			</td>    
		</tr>
		<tr>
			<td align="right"><spring:message code="patient.clinical.tb.meningitis" /></td>
			<td><input type="radio" name="patientClinicalProperties.tbMenigitis" value="yes" >
			    <input type="radio" name="patientClinicalProperties.tbMenigitis" value="no" >
			    <input type="radio" name="patientClinicalProperties.tbMenigitis" value="unknown" checked="checked">
			</td>
			<td>&nbsp;</td>
			<td align="right"><spring:message code="patient.clinical.std.kaposi" /></td>
			<td><input type="radio" name="patientClinicalProperties.stdKaposi" value="yes" >
			    <input type="radio" name="patientClinicalProperties.stdKaposi" value="no" >
			    <input type="radio" name="patientClinicalProperties.stdKaposi" value="unknown" checked="checked">
			</td>    
		</tr>
		<tr>
			<td align="right"><spring:message code="patient.clinical.tb.prurigo" /></td>
			<td><input type="radio" name="patientClinicalProperties.tbPrurigol" value="yes">
			    <input type="radio" name="patientClinicalProperties.tbPrurigol" value="no" >
			    <input type="radio" name="patientClinicalProperties.tbPrurigol" value="unknown" checked="checked">
			</td>
			<td>&nbsp;</td>
			<td align="right"><spring:message code="patient.clinical.std.zona" /></td>
			<td><input type="radio" name="patientClinicalProperties.stdZona" value="yes" >
			    <input type="radio" name="patientClinicalProperties.stdZona" value="no" >
			    <input type="radio" name="patientClinicalProperties.stdZona" value="unknown" checked="checked">
			</td>    
		</tr>
		<tr>
			<td align="right"><spring:message code="patient.clinical.tb.diarrhae" /></td>
			<td><input type="radio" name="patientClinicalProperties.tbDiarrhae" value="yes" >
			    <input type="radio" name="patientClinicalProperties.tbDiarrhae" value="no" >
			    <input type="radio" name="patientClinicalProperties.tbDiarrhae" value="unknown" checked="checked">
			</td>
			<td>&nbsp;</td>
			<td align="right" colspan="2"><spring:message code="other" />
				<form:input path="patientClinicalProperties.stdOther" />
			</td>
		</tr>
		<tr><td colspan="5" >&nbsp;</td></tr>
		<tr>
			<td colspan="4" align="right"><spring:message code="patient.clinical.prophylaxis.arv"/></td>
			<td><input type="radio" name="patientClinicalProperties.arvProphyaxixReceiving" value="yes" >
			    <input type="radio" name="patientClinicalProperties.arvProphyaxixReceiving" value="no" >
			    <input type="radio" name="patientClinicalProperties.arvProphyaxixReceiving" value="unknown" checked="checked">
			</td>
		</tr>
		<tr>
			<td colspan="4" align="right"><spring:message code="patient.clinical.prophylaxis.arv.type"/></td>
			<td>
				<form:select path="patientClinicalProperties.arvProphyaxixType">
				
					<form:options items="patientClinicalProperties.prophyaxixTypes"/>
				</form:select>
			</td>
		</tr>
		<tr>
			<td colspan="4" align="right"><spring:message code="patient.clinical.treatment.arv"/></td>
			<td><input type="radio" name="patientClinicalProperties.arvTreatmentReceiving" value="yes" >
			    <input type="radio" name="patientClinicalProperties.arvTreatmentReceiving" value="no" >
			    <input type="radio" name="patientClinicalProperties.arvTreatmentReceiving" value="unknown" checked="checked">
			</td>
		</tr>
		<tr>
			<td colspan="4" align="right"><spring:message code="patient.clinical.treatment.arv.recall"/></td>
			<td><input type="radio" name="patientClinicalProperties.arvTreatmentRemembered" value="yes" >
			    <input type="radio" name="patientClinicalProperties.arvTreatmentRemembered" value="no" >
			    <input type="radio" name="patientClinicalProperties.arvTreatmentRemembered" value="unknown" checked="checked">
			</td>
		</tr>
		<tr>
			<td colspan="4" align="right"><spring:message code="patient.clinical.treatment.arv.type1"/></td>
			<td><form:input path="patientClinicalProperties.arvTreatment1" />
			</td>
		</tr>
		<tr>
			<td colspan="4" align="right"><spring:message code="patient.clinical.treatment.arv.type2"/></td>
			<td><form:input path="patientClinicalProperties.arvTreatment2" />
			</td>
		</tr>
		<tr>
			<td colspan="4" align="right">&nbsp;</td>
			<td><form:input path="patientClinicalProperties.arvTreatment3" />
			</td>
		</tr>
		<tr>
			<td colspan="4" align="right">&nbsp;</td>
			<td><form:input path="patientClinicalProperties.arvTreatment4" />
			</td>
		</tr>
		<tr><td colspan="5" >&nbsp;</td></tr>
		<tr>
			<td colspan="4" align="right"><spring:message code="patient.clinical.prophylaxis.cotrimoxazole"/></td>
			<td><input type="radio" name="patientClinicalProperties.cotrimoxazoleReceiving" value="yes" >
			    <input type="radio" name="patientClinicalProperties.cotrimoxazoleReceiving" value="no" >
			    <input type="radio" name="patientClinicalProperties.cotrimoxazoleReceiving" value="unknown" checked="checked">
			</td>
		</tr>
		<tr>
			<td colspan="4" align="right"><spring:message code="patient.clinical.cotrimoxazole.stage"/></td>
			<td>
				<form:select path="patientClinicalProperties.cotrimoxazoleType">
					<form:options items="${form.patientClinicalProperties.arvStages}" itemValue="id" itemLabel="value"/>
				</form:select>
			</td>
		</tr>
		<tr>
			<td colspan="4"><spring:message code="patient.clinical.infection"/></td>
		</tr>
		<tr>
			<td colspan="3">(1)<spring:message code="patient.clinical.history.tb"/></td>
			<td colspan="2">(2)<spring:message code="patient.clinical.history.std"/></td>
		</tr>	
		<tr>
			<td align="right"><spring:message code="patient.clinical.tb.extraPulmonary" /></td>
			<td><input type="radio" name="patientClinicalProperties.infectionExtraPulmanary" value="yes" >
			    <input type="radio" name="patientClinicalProperties.infectionExtraPulmanary" value="no" >
			    <input type="radio" name="patientClinicalProperties.infectionExtraPulmanary" value="unknown" checked="checked">
			</td>
			<td>&nbsp;</td>
			<td align="right"><spring:message code="patient.clinical.std.colon" /></td>
			<td><input type="radio" name="patientClinicalProperties.stdInfectionColon" value="yes" >
			    <input type="radio" name="patientClinicalProperties.stdInfectionColon" value="no" >
			    <input type="radio" name="patientClinicalProperties.stdInfectionColon" value="unknown" checked="checked">
			</td>    
		</tr>
		<tr>
			<td align="right"><spring:message code="patient.clinical.tb.cerebral" /></td>
			<td><input type="radio" name="patientClinicalProperties.infectionCerebral" value="yes" >
			    <input type="radio" name="patientClinicalProperties.infectionCerebral" value="no" >
			    <input type="radio" name="patientClinicalProperties.infectionCerebral" value="unknown" checked="checked">
			</td>
			<td>&nbsp;</td>
			<td align="right"><spring:message code="patient.clinical.std.candidose" /></td>
			<td><input type="radio" name="patientClinicalProperties.stdInfectionCandidiasis" value="yes" >
			    <input type="radio" name="patientClinicalProperties.stdInfectionCandidiasis" value="no" >
			    <input type="radio" name="patientClinicalProperties.stdInfectionCandidiasis" value="unknown" checked="checked">
			</td>    
		</tr>
		<tr>
			<td align="right"><spring:message code="patient.clinical.tb.meningitis" /></td>
			<td><input type="radio" name="patientClinicalProperties.infectionMeningitis" value="yes" >
			    <input type="radio" name="patientClinicalProperties.infectionMeningitis" value="no" >
			    <input type="radio" name="patientClinicalProperties.infectionMeningitis" value="unknown" checked="checked">
			</td>
			<td>&nbsp;</td>
			<td align="right"><spring:message code="patient.clinical.std.kaposi" /></td>
			<td><input type="radio" name="patientClinicalProperties.stdInfectionKaposi" value="yes" >
			    <input type="radio" name="patientClinicalProperties.stdInfectionKaposi" value="no" >
			    <input type="radio" name="patientClinicalProperties.stdInfectionKaposi" value="unknown" checked="checked">
			</td>    
		</tr>
		<tr>
			<td align="right"><spring:message code="patient.clinical.tb.prurigo" /></td>
			<td><input type="radio" name="patientClinicalProperties.infectionPrurigol" value="yes" >
			    <input type="radio" name="patientClinicalProperties.infectionPrurigol" value="no" >
			    <input type="radio" name="patientClinicalProperties.infectionPrurigol" value="unknown" checked="checked">
			</td>
			<td>&nbsp;</td>
			<td align="right"><spring:message code="patient.clinical.std.zona" /></td>
			<td><input type="radio" name="patientClinicalProperties.stdInfectionZona" value="yes" >
			    <input type="radio" name="patientClinicalProperties.stdInfectionZona" value="no" >
			    <input type="radio" name="patientClinicalProperties.stdInfectionZona" value="unknown" checked="checked">
			</td>    
		</tr>
		<tr>
			<td align="right" colspan="2"><spring:message code="other" />
				<form:input path="patientClinicalProperties.infectionOther" />
			</td>
			<td colspan="4">&nbsp;</td>
		</tr>
		<tr>
			<td colspan="4" align="right"><spring:message code="patient.clinical.under.treatment"/></td>
			<td><input type="radio" name="patientClinicalProperties.infectionUnderTreatment" value="yes" >
			    <input type="radio" name="patientClinicalProperties.infectionUnderTreatment" value="no" >
			    <input type="radio" name="patientClinicalProperties.infectionUnderTreatment" value="unknown" checked="checked">
			</td>
		</tr>
		<tr>
			<td colspan="4" align="right"><spring:message code="patient.clinical.weight"/></td>
			<td><form:input path="patientClinicalProperties.weight" />
			</td>
		</tr>
			<tr>
			<td colspan="4" align="right"><spring:message code="patient.clinical.karnofsky"/></td>
			<td><form:input path="patientClinicalProperties.karnofskyScore" />
			</td>
		</tr>
		<tr>
			<td colspan="4" align="right"><spring:message code="patient.clinical.karnofsky.children"/></td>
			<td>&nbsp;</td>
		</tr>
	</table>
	
	<% } %>
</div>
 
<script type="text/javascript">

</script>
