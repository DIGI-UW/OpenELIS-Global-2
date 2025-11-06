<%@ page language="java"
	contentType="text/html; charset=UTF-8"
	import="org.apache.commons.validator.GenericValidator, 
			org.openelisglobal.common.action.IActionConstants,
			org.openelisglobal.sample.util.AccessionNumberUtil,
			org.openelisglobal.common.util.IdValuePair,
			org.openelisglobal.internationalization.MessageUtil,
			org.openelisglobal.common.util.Versioning,
            org.openelisglobal.common.util.DateUtil,
		    java.text.DecimalFormat,
			java.util.List,
			org.openelisglobal.resultvalidation.bean.AnalysisItem,
			org.openelisglobal.common.util.ConfigurationProperties,
			org.openelisglobal.common.util.ConfigurationProperties.Property,
			org.owasp.encoder.Encode" %>
			
<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>


<%@ taglib prefix="fn" uri="jakarta.tags.functions"%>

<script type="text/javascript" src="scripts/OEPaging.js?"></script>
<script type="text/javascript" src="scripts/utilities.js"></script>

<%
	String url = request.getAttribute("jakarta.servlet.forward.servlet_path").toString();	
%>

<script>

function validateEntrySize( elementValue ){
	$("retrieveTestsID").disabled = (elementValue.length == 0);
}

function validateDateEntrySize( elementValue ){
	$("retrieveTestsByDate").disabled = (elementValue.length == 0);
}

function doShowTests(){
	var form = document.getElementById("mainForm");
	window.location.href = "${requestScope['jakarta.servlet.forward.request_uri']}?accessionNumber="  + $("searchAccessionID").value;
}

function doShowTestsForDate(){
	var form = document.getElementById("mainForm");
	window.location.href = "${requestScope['jakarta.servlet.forward.request_uri']}?date="  + encodeURIComponent($("searchDate").value);
}

function /*void*/ handleEnterEvent(  ){
	if( $("searchAccessionID").value.length > 0){
		doShowTests();
	}
	return false;
}

</script>

<% if( !(url.contains("RetroC"))){ %>
<div id="searchDiv" class="colorFill"  >
<div id="PatientPage" class="colorFill" style="display:inline" >
<h2><spring:message code="sample.entry.search"/></h2>
    <% if(url.contains("AccessionValidationRange")){ %> 
		<table width="50%">
			<tr >
			<td width="50%" align="right" >
				<%=MessageUtil.getContextualMessage("quick.entry.accession.range")%>
			</td>
			<td width="50%">
				<input name="accessionNumber"
					size="20"
					id="searchAccessionID"
					maxlength="<%=Integer.toString(AccessionNumberUtil.getMaxAccessionLength())%>"
					onkeyup="validateEntrySize( this.value );"
					onblur="validateEntrySize( this.value );"
					class="text"
					type="text">
				<spring:message code="sample.search.scanner.instructions"/>
			</td>
		</tr>
			
		</table>
		<br/>
		
		<button type="button" name="retrieveTestsButton" id="retrieveTestsID"  onclick="doShowTests();" disabled="disabled" >
			<%= MessageUtil.getContextualMessage("validationentry.accession.range") %>
		</button>
	<% }%>
	<% if(url.contains("ResultValidationByTestDate")){ %> 
		<table width="50%">
			<tr >
			<td width="50%" align="right" >
				<spring:message code="sample.search.date.instructions"/>
				(<%=DateUtil.getDateUserPrompt()%>)
			</td>
			<td width="50%">
				<input name="date"
					size="20"
					id="searchDate"
					onkeyup="validateDateEntrySize( this.value );addDateSlashes(this, event);"
					onblur="validateDateEntrySize( this.value );"
					class="text"
					type="text">
			</td>
		</tr>
			
		</table>
		<br/>
		
		<button type="button" name="retrieveTestsButton" id="retrieveTestsByDate"  onclick="doShowTestsForDate();" disabled="disabled" >
			<%= MessageUtil.getContextualMessage("validationentry.date.search") %>
		</button>
	<% }%>
	
	<h1>
		
	</h1>
</div>
</div>
<% }%>