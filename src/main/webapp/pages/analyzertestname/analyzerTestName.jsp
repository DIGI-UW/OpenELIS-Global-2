<%@ page language="java" contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants,
	org.openelisglobal.internationalization.MessageUtil,
	org.openelisglobal.analyzer.valueholder.Analyzer,
	org.openelisglobal.test.valueholder.Test" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



 
<c:set var="analyzerList" value="${form.analyzerList}" />
<c:set var="analyzerNameOrId" value="${form.analyzerId}" />
<c:set var="testList" value="${form.testList}" />
<c:set var="testNameOrId" value="${form.testId}" />

<script>

	jQuery(document).ready( function() {
		jQuery("#analyzerIdHidden").val(jQuery("#analyzerId").val());
		if(jQuery("#analyzerId").val() != 0 ){
			jQuery("#analyzerId").attr('disabled', 'disabled');
			jQuery("#analyzerTestNameId").attr('disabled','disabled');
		}
	});
function validateForm(form) {

	if( $("analyzerId").selectedIndex == 0 ||
	    $("analyzerTestNameId").value == null ||
	    $("testId").selectedIndex == 0 ){
	    		alert('<%=MessageUtil.getMessage("error.all.required") %>');
	    		return false;
	    }

    return true;
}

	function copyToHiddenAnalyzer( element ){
		jQuery("#analyzerIdHidden").val(element.value);
	}

</script>
<div style="border: 1px solid;width:40%">
	<br>
	<span style="width:40%;float:left;padding-left: 4px;" ><spring:message code="analyzer.label" />
			:
			<span class="requiredlabel">*</span>
	</span>
	<form:hidden path="analyzerId" id="analyzerIdHidden" />
	<select id="analyzerId" onchange="copyToHiddenAnalyzer(this);" >
		<option value="0"></option>
		<c:forEach items="${analyzerList}" var="analyzer">
		<option value="${analyzer.id}" ${(analyzer.id == analyzerNameOrId || analyzer.name == analyzerNameOrId) ? "selected='selected'" : ""}>
			${analyzer.name}
		</option>
		</c:forEach>
	</select><br><br>
	<span style="width:40%;float:left;padding-left: 4px;" >
		<spring:message code="analyzer.test.name" />:
		<span class="requiredlabel">*</span>
	</span>
	<form:input path="analyzerTestName" id="analyzerTestNameId" /><br>&nbsp;
</div>
<br>
<div style="width:40%">
	<span style="width:40%;float:left;padding-left: 4px;" >
		<spring:message code="analyzer.test.actual.name" />:
			<span class="requiredlabel">*</span>
	</span>
<form:select path="testId" id="testId" >
	<form:option value="0">&nbsp;</form:option>
	<c:forEach items="${testList}" var="test">
		<option value="${test.id}" ${(test.id == testNameOrId || test.name == testNameOrId)  ? "selected='selected'" : ""}>
			${test.name}
		</option>
	</c:forEach>
</form:select>
</div><br><br>

