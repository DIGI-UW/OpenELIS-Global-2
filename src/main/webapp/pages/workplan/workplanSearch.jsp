<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page import="org.openelisglobal.common.action.IActionConstants,
                 org.openelisglobal.common.util.Versioning,
                 org.owasp.encoder.Encode" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<c:set var="type" value="${form.type}"/>
<c:set var="responseAction" value="${form.searchAction}"/>

<script type="text/javascript" src="scripts/utilities.js?" ></script>
<script type="text/javascript">

function doShowTests(element){
	window.location.href = '<c:url value="${responseAction}?type=${type}&selectedSearchID="/>' + element.value;
}

function doShowPriorityTests(element){
	window.location.href = '<c:url value="${responseAction}?type=${type}&priority="/>' + element.value;
}

function validateTest(){

	if ( fieldIsEmptyById( "testName" ) ) {
		setValidIndicaterOnField(false, "isValid");
	}
}


function /*boolean*/ handleEnterEvent(){
	if( !fieldIsEmptyById("testName")){
		doShowTests();
	}
	return false;
}

</script>

<div id="PatientPage" class="colorFill" style="display:inline" >
	<h2><spring:message code="sample.entry.search"/></h2>
	<table width="40%">
		<tr>
			<td width="50%" align="right" nowrap>
				<c:out value="${form.searchLabel}"/>
			</td>
			<td>
				<c:if test="${not (type == 'priority')}">
					<form:select path="selectedSearchID" id="testName"
							onchange="doShowTests(this);" >
						<option value=""></option>
						<form:options items="${form.searchTypes}" itemLabel="value" itemValue="id" />
					</form:select>
				</c:if>	
				<c:if test="${type == 'priority'}">
					<form:select path="priority" id="testName"
							onchange="doShowPriorityTests(this);" >
						<option value=""></option>
						<form:options items="${form.priorityList}" itemLabel="value" itemValue="id" />
					</form:select>
				</c:if>	
	   		</td>
		</tr>
	</table>
	<br/>
	<h1>
		<c:out value="${form.testName}"/>
	</h1>
</div>


<ajax:autocomplete
  source="testName"
  target="selectedTestID"
  baseUrl="ajaxAutocompleteXML"
  className="autocomplete"
  parameters="testName={testName},provider=TestAutocompleteProvider,fieldName=testName,idName=id"
  indicator="indicator"
  minimumCharacters="1"
  parser="new ResponseXmlToHtmlListParser()"
  />
