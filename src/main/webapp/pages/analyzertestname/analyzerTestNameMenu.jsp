<%@ page language="java"
	contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants,
			org.openelisglobal.analyzerimport.action.beans.NamedAnalyzerTestMapping" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



 

<table width="80%" border="2">
	<tr>
		<th>&nbsp;</th>
	   	<th><spring:message code="analyzer.label"/>&nbsp;-&nbsp;<spring:message code="analyzer.test.name"/> </th>
	   	<th> <spring:message code="analyzer.test.actual.name"/> </th>
	</tr>
	<form:form name="${form.formName}" 
				   action="${form.formAction}" 
				   modelAttribute="form" 
				   onSubmit="return submitForm(this);" 
				   method="${form.formMethod}"
				   id="menuForm">
	<c:forEach items="${form.menuList}" var="analyzerTest">
	  	<tr>
	   		<td class="textcontent">
	      		<form:checkbox path="selectedIDs" value="${analyzerTest.uniqueId}" onclick="output()"/>
			</td>
   	   		<td class="textcontent">
	    		<c:out value="${analyzerTest.analyzerName}"/>&nbsp;-&nbsp;
	   	  		<c:out value="${analyzerTest.analyzerTestName}"/>
	   		</td>
   	  	 	<td class="textcontent">
	   	  		<c:out value="${analyzerTest.actualTestName}"/>
	   		</td>
     	</tr>
	</c:forEach>
	</form:form>
</table>
