<%@ page language="java"
	contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.dictionary.valueholder.Dictionary,
		org.openelisglobal.common.action.IActionConstants" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>


<%@ taglib prefix="fn" uri="jakarta.tags.functions"%>

 

<table width="100%" border=2">
	<tr>
	   <th>
	     <spring:message code="label.form.select"/>
	   </th>
	   <th>
	   	  <spring:message code="dictionary.dictionarycategory"/>
	   </th>
	   <th>
	      <spring:message code="dictionary.dictEntry"/>
	   </th>
	   <th>
	      <spring:message code="dictionary.localAbbreviation"/>
	   </th>
	   <th>
	      <spring:message code="dictionary.isActive"/>
	   </th>
   
	</tr>
	<form:form name="${form.formName}" 
				   action="${form.formAction}" 
				   modelAttribute="form" 
				   onSubmit="return submitForm(this);" 
				   method="${form.formMethod}"
				   id="menuForm">
	<c:forEach items="${form.menuList}" var="dict" varStatus="iter">
	  <tr>	
	   <td class="textcontent">
	   
	      <form:checkbox path="selectedIDs" onclick="output()" value="${dict.id}"/>
     
   	   </td>	 
	   <td class="textcontent">
	        <c:out value="${dict.dictionaryCategory.categoryName}"/>
	      &nbsp;
       </td>
	   <td class="textcontent">
   	      <c:out value="${dict.dictEntry}" />
	      &nbsp;
       </td>
	   <td class="textcontent">
   	      <c:out value="${fn:substring(dict.localAbbreviation, 0, 10)}"/>
	      &nbsp;
       </td>
	   <td class="textcontent">
	   	  <c:out value="${dict.isActive}"/>
	   </td>
     </tr>
	</c:forEach>
	</form:form>
</table>
