<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page import="org.openelisglobal.common.action.IActionConstants,
				 org.openelisglobal.common.formfields.FormFields,
				 org.openelisglobal.common.formfields.FormFields.Field,
				 org.openelisglobal.common.util.DateUtil,
				 org.openelisglobal.internationalization.MessageUtil"  %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<%
	boolean useCollectionDate = FormFields.getInstance().useField(Field.CollectionDate);
	boolean useSampleStatus = FormFields.getInstance().useField(Field.SearchSampleStatus);
%>
 
<script type="text/javascript">

var newSearchInfo = false;

function doShowTests(){
    newSearchInfo = false;
//     make get request of the search fields only
	window.location.href = "StatusResults" 
			+ "?collectionDate=" + encodeURIComponent($("collectionDate").value ) 
			+ "&recievedDate=" + encodeURIComponent($("recievedDate").value )
			+ "&selectedTest=" + encodeURIComponent($("selectedTest").value ) 
			+ "&selectedAnalysisStatus=" + encodeURIComponent($("selectedAnalysisStatus").value )
			+ "&selectedSampleStatus=" + encodeURIComponent($("selectedSampleStatus").value );
}

function /*boolean*/ handleEnterEvent(){
	if( newSearchInfo ){
		doShowTests();
	}
	return false;
}

function /*void*/ dirtySearchInfo(e){ 
	var code = e ? e.which : window.event.keyCode;
	if( code != 13 ){
		newSearchInfo = true; 
	}
}
</script>

<div id="PatientPage" class="colorFill" style="display:inline" >

	<h2><spring:message code="sample.entry.search"/></h2>
	<table width="70%">
	<tr >
		<td >
			<% if(useCollectionDate){ %>  <%= MessageUtil.getContextualMessage("sample.collectionDate")  %><br><span style="font-size: xx-small; "><%=DateUtil.getDateUserPrompt()%></span> <% } %>
		</td>
		<td >
			<spring:message code="sample.receivedDate"/><br><span style="font-size: xx-small; "><%=DateUtil.getDateUserPrompt()%></span>
		</td>
		<td >
			<spring:message code="test.testName"/>
		</td>
		<td >
			<%= MessageUtil.getContextualMessage("analysis.status") %>
		</td>
		<% if( useSampleStatus ){ %>
		<td >
			<spring:message code="sample.status"/>
		</td>
		<% } %>
	</tr>

	<tr>
	<td >
		<% if(useCollectionDate){ %> <form:input path="collectionDate" onkeyup="dirtySearchInfo( event )" /> <%} %>
	</td>
	<td>
		<form:input path="recievedDate" onkeyup="dirtySearchInfo( event )"/>
	</td>
	<td>
			<form:select  path="selectedTest" onchange="dirtySearchInfo( event )">
				<form:options items="${form.testSelections}" itemLabel="value" itemValue="id"/>
			</form:select>
	</td>
	<td>
			<form:select  path="selectedAnalysisStatus" onchange="dirtySearchInfo( event )" >
				<form:options items="${form.analysisStatusSelections}" itemLabel="description" itemValue="id" />
			</form:select>
	</td>
	<% if( useSampleStatus ){ %>
	<td>
			<form:select  path="selectedSampleStatus" onchange="dirtySearchInfo( event )">
				<form:options items="${form.sampleStatusSelections}" itemLabel="description" itemValue="id" />
			</form:select>
	</td>
	<% } %>
	</tr>

	</table>
	<button type="button" name="searchButton" onclick="doShowTests()"  >
		<spring:message code="resultsentry.status.search"/>
	</button>

</div>



