<%@ page language="java" contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants,
			org.openelisglobal.internationalization.MessageUtil,
			org.openelisglobal.dataexchange.resultreporting.beans.ReportingConfiguration,
			org.openelisglobal.common.util.Versioning"%>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>


<script type="text/javascript" src="scripts/ajaxCalls.js?"></script>


<script type="text/javascript" >


	
function setMyCancelAction(){
	var form = document.getElementById("mainForm");
	form.action = "CancelResultReportingConfiguration";
	form.submit();
	return true;	
}

function savePage(){
	if( formValid() ){
		var form = document.getElementById("mainForm");
		form.action = "ResultReportingConfiguration";
		form.submit();
		return true;		
	}

	return false;
}

function formValid(){
	return hasRequriedValues();
}

function /*boolean*/ hasRequriedValues(){
	//var url = $("urlField");

	//if( document.getElementById("mainForm").enabled[0].checked && url.value.blank() ){
	//	url.style.borderColor = "red";
	//	alert("<%= MessageUtil.getMessage("resultreporting.config.missing.url")%>");
	//	return false;
	//}
	
	return true;	
}

function  /*void*/ processTestSuccess(xhr){

    //alert(xhr.responseText);
	var testResult = xhr.responseXML.getElementsByTagName("testResult").item(0).getAttribute("result");

	alert( testResult );
}
</script>

<c:forEach items="${form.reports}" var="reports" varStatus="iter">

<h2><c:out value="${reports.title}"/></h2>
<table >
	<tr >
		<td width="180px">
			<form:hidden path="reports[${iter.index}].enabledId" />
			<spring:message code="testusage.config.enable" var="enableMsg"/>
			<spring:message code="testusage.config.disable" var="disableMsg"/>
			<form:radiobutton path="reports[${iter.index}].enabled" 
						value="enable" label="${enableMsg}"/>
			<form:radiobutton path="reports[${iter.index}].enabled" 
						value="disable" label="${disableMsg}"/>
	</td>
	</tr>
	<tr>
	<td colspan="2"><spring:message htmlEscape="false" code="resultreporting.config.url"/></td>  	
  </tr>
  <tr>
  <td>&nbsp;</td>
  <td>
  		<form:hidden path="reports[${iter.index}].urlId" />
    	<form:input path="reports[${iter.index}].url"
    			   id = "${reports.connectionTestIdentifier}"
    			   size="80" />
    </td>
  </tr>
  <c:if test="${resports.isScheduled}">
  <tr>
  	<td colspan="2"><spring:message code="testusage.config.transmit.instructions"/>
  	</td>
  </tr>
  <tr>
  	<td>&nbsp;</td>
  	<td>
  	 <spring:message code="testusage.config.time"/>&nbsp;
			<form:select path="reports[${iter.index}].scheduleHours"
						 cssClass="gatherDependent"
						 id="scheduleHours" 
						 onchange=" makeDirty();">
				<form:options items="${form.hourList}" itemLabel="value" itemValue="id"/>
			</form:select>:
			<form:select path="reports[${iter.index}].scheduleMin"
			             cssClass="gatherDependent"
			             id="scheduleMin" 
			             onchange=" makeDirty();">
				<form:options items="${form.minList}" itemLabel="value" itemValue="id"/>
		</form:select>
    </td>
  </tr>
  </c:if>
 <c:if test="${reports.showAuthentication}">
 <tr>
    <td><br/><spring:message code="testusage.config.transmit.name"/></td>
  </tr>
  <tr>
    <td>     
    	<form:input path="reports[${iter.index}].userName" 
    			   cssClass="gatherDependent sendingDependent"
    			   onchange=" makeDirty();"/>
    </td>
  </tr>
  <tr>
    <td><br/><spring:message code="testusage.config.transmit.password"/></td>
  </tr>
  <tr>  
    <td><form:password path="reports[${iter.index}].password"
    				   cssClass="gatherDependent sendingDependent"
    				   onchange=" makeDirty();"/>
    </td>
  </tr>
</c:if>     
</table>
<c:if test="${reports.showBacklog}">
	<spring:message code="result.report.queue.msg"/>
	<br/><br/>
	<spring:message code="result.report.queue.size"/>: <c:out value="${reports.backlogSize}" />
</c:if> 
</c:forEach>