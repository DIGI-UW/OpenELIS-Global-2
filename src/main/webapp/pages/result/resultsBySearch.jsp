<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page import="org.openelisglobal.common.action.IActionConstants" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<script type="text/javascript" >

function  /*void*/ setMyCancelAction(form, action, validate, parameters) 
{   
	//first turn off any further validation
	setAction(document.getElementById("mainForm"), 'Cancel', 'no', '');	
}

function doSelectPatientForResults(){	
	var form = document.getElementById("mainForm");

	form.action = "PatientResults?patientID="  + patientSelectID;

	form.submit();
}

function altAccessionSearchFunction(labNumber) {
	jQuery("#loading").hide();
	var url = new URL(window.location.href);
	url.searchParams.set('accessionNumber', labNumber);
	
	window.location.href = url.toString();
}

jQuery(document).ready( function() {
	<c:if test="${not empty requestScope.reflex_accessions}">
	var reflexAccessions = new Array();
	<c:forEach items="${requestScope.reflex_accessions}" var="accession">
	reflexAccessions.push('<c:out value="${accession}"/>');
	</c:forEach>
	var msg = 'A result has triggered another test to be added to order(s): ' + reflexAccessions.join(', ');
	
	alert(msg);
	</c:if>

});

</script>

<div id="searchDiv" class="colorFill"  >
<jsp:include page="${searchPanelFragment}"/>
</div>
<c:if test="${form.searchFinished}">
	<div id="resultsDiv" class="colorFill" >
		<jsp:include page="${resultsPanelFragment}"/>
	</div>
</c:if>



