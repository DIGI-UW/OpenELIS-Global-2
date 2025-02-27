<%@ page language="java"
	contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



 

<table width="100%" border="2">
	<tr>
	   <th>
	     <spring:message code="label.form.select"/>
	   </th>
	   <th>
	   	  <spring:message code="systemuser.lastName"/>
	   </th>
	   <th>
	      <spring:message code="systemuser.firstName"/>s
	   </th>
	   <th>
	   	  <spring:message code="systemuser.loginName"/>
	   </th>
	   <th>
	   	  <spring:message code="login.password.expired.date"/>
	   </th>
	   <th>
	   	  <spring:message code="login.account.locked"/>
	   </th>
	   <th>
	   	  <spring:message code="login.account.disabled"/>
	   </th>
	   <th>
	   	  <spring:message code="systemuser.isActive" />
	   </th>
	   <th>
	   	  <spring:message code="login.timeout"/>
	   </th>
	</tr>
	
	<form:form name="${form.formName}" 
		   action="${form.formAction}" 
		   modelAttribute="form" 
		   method="${form.formMethod}"
		   id="menuForm">
	<c:forEach items="${form.menuList}" var="systemUser" varStatus="iter">
	  <tr>
	   <td class="textcontent">
	   <form:checkbox path="selectedIDs" value="${systemUser.combinedUserID}" onclick="output()"/>
       
   	   </td>
   	   
	   <td class="textcontent">
	   	  <c:out value="${systemUser.lastName}"/>
	   </td>
	   <td class="textcontent">
	   	  <c:out value="${systemUser.firstName}"/>
	   </td>
	   <td class="textcontent">
	   	  <c:out value="${systemUser.loginName}"/>
	   </td>
	   <td class="textcontent">
	   	  <c:out value="${systemUser.expDate}"/>
	   </td>
	   <td class="textcontent">
	   	  <c:out value="${systemUser.locked}"/>
	   </td>
	   <td class="textcontent">
	   	  <c:out value="${systemUser.disabled}"/>
	   </td>
	   <td class="textcontent">
	   	  <c:out value="${systemUser.active}"/>
	   </td>
	   <td class="textcontent">
	   	  <c:out value="${systemUser.timeout}"/>
	   </td>
	   
       </tr>
	</c:forEach>
	</form:form>
</table>
