<%@ page language="java"
	contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.organization.valueholder.Organization,
			org.openelisglobal.common.action.IActionConstants,
			org.openelisglobal.common.formfields.FormFields,
			org.openelisglobal.internationalization.MessageUtil" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>




<table width="100%" border=2">
	<tr>
	   <th>
	     <spring:message code="label.form.select"/>
	   </th>
	   <th><%--bugzilla 1412 rearrange--%>
	   	  <spring:message code="provider.providerLastName"/>
	   </th>
	   <th>
	   	  <spring:message code="provider.providerFirstName"/>
	   </th>
	   <th>
	      <spring:message code="provider.isActive"/>
	   </th>
	   <th>
	   	  <spring:message code="provider.telephone"/>
	   </th>
	   <th>
	   	  <spring:message code="provider.fax"/>
	   </th>

	</tr>
	<form:form name="${form.formName}" 
				   action="${form.formAction}" 
				   modelAttribute="form" 
				   onSubmit="return submitForm(this);" 
				   method="${form.formMethod}"
				   id="menuForm">
	<c:forEach items="${form.menuList}" var="provider" varStatus="iter">
	  <tr>
	   <td class="textcontent">
	      <form:checkbox path="selectedIDs" onclick="output()" value="${provider.id}"/>
   	   </td>
	   <td class="textcontent">
	   	  <c:out value="${provider.person.lastName}"/>
	   </td>
	   <td class="textcontent">
	        <c:out value="${provider.person.firstName}"/>
	   </td>
	   <td class="textcontent">
	   	  <c:out value="${provider.active}"/>
	   </td>
	   <td class="textcontent">
	   	  <c:out value="${provider.person.primaryPhone}"/>
	   	  &nbsp;
	   </td>
	   <td class="textcontent">
	   	  <c:out value="${provider.person.fax}"/>
	   	  &nbsp;
	   </td>
     </tr>
	</c:forEach>
	</form:form>
</table>
