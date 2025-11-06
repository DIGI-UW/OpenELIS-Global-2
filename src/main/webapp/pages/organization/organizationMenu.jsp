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



<%
	boolean useOrgLocalAbbrev = FormFields.getInstance().useField(FormFields.Field.OrgLocalAbrev);
	boolean useOrgState = FormFields.getInstance().useField(FormFields.Field.OrgState);
	boolean useZipCode = FormFields.getInstance().useField(FormFields.Field.ZipCode);
	boolean useMLS = FormFields.getInstance().useField(FormFields.Field.MLS);
%>

<table width="100%" border=2">
	<tr>
	   <th>
	     <spring:message code="label.form.select"/>
	   </th>
		<% if( useOrgLocalAbbrev) { %>
	   <th><%--bugzilla 2069 added--%>
	   	  <spring:message code="organization.localAbbreviation"/>
	   </th>
	   <% } %>
	   <th><%--bugzilla 1412 rearrange--%>
	   	  <spring:message code="organization.organizationName"/>
	   </th>
	   <th>
	   	  <spring:message code="organization.parent"/>
	   </th>
	   <th>
	   	  <%= MessageUtil.getContextualMessage("organization.short") %>
	   </th>
	   <th>
	      <spring:message code="organization.isActive"/>
	   </th>
	   <th>
	   	  <spring:message code="organization.streetAddress"/>
	   </th>
	   <th>
	   	  <spring:message code="organization.city"/>
	   </th>
	   <% if( useOrgState){ %>
	   <th>
	   	  <spring:message code="organization.state"/>
	   </th>
	   <% } %>
	   <% if( useZipCode ){ %>
	    <th>
	   	  <spring:message code="organization.zipCode"/>
	   </th>
	   <% } %>
	   <th>
	   	  <spring:message code="organization.clia.number"/>
	   </th>
	   <% if( useMLS ){ %>
	   <th>
	   	  <spring:message code="organization.mls.lab"/>
	   </th>
	   <% } %>

	</tr>
	<form:form name="${form.formName}" 
				   action="${form.formAction}" 
				   modelAttribute="form" 
				   onSubmit="return submitForm(this);" 
				   method="${form.formMethod}"
				   id="menuForm">
	<c:forEach items="${form.menuList}" var="org1" varStatus="iter">
	<c:set var="orgID" value="${org1.id}"/>
	<c:if test="${not empty org1.organization}">
	 <c:set var="parentOrgID" value="${org1.organization.id}"/>
	</c:if>

	  <tr>
	   <td class="textcontent">
	      <form:checkbox path="selectedIDs" onclick="output()" value="${orgID}"/>
   	   </td>
		<% if( useOrgLocalAbbrev ){ %>
	   <td class="textcontent">
	   	  <c:out value="${org1.organizationLocalAbbreviation}"/>
	   </td>
	   <% } %>
	   <td class="textcontent">
	   	  <c:out value="${org1.organizationName}"/>
	   </td>
	   <td class="textcontent">
	    <c:if test="${not empty org1.organization}">
	        <c:out value="${org1.organization.organizationName}"/>
	    </c:if>
	      &nbsp;
	   </td>
	   <td class="textcontent">
	      <c:if test="${not empty org1.shortName}">
	   	  <c:out value="${org1.shortName}"/>
	   	  </c:if>
	   	  <c:if test="${empty org1.shortName}">
	   	   &nbsp;
	   	  </c:if>
	   </td>
	   <td class="textcontent">
	   	  <c:out value="${org1.isActive}"/>
	   </td>
	   <td class="textcontent">
	   	  <c:out value="${org1.streetAddress}"/>
	   	  &nbsp;
	   </td>
	   <td class="textcontent">
	   	  <c:out value="${org1.city}"/>
	   	  &nbsp;
	   </td>
	   <% if( useOrgState ){ %>
	  <td class="textcontent">
	   	  <c:out value="${org1.state}"/>
	   	  &nbsp;
	   </td>
	   <% } %>
	   <% if( useZipCode ){ %>
	   <td class="textcontent">
	   	  <c:out value="${org1.zipCode}"/>
	   	  &nbsp;
	   </td>
	   <% } %>
	   <td class="textcontent">
	   	  <c:out value="${org1.cliaNum}"/>
	   	  &nbsp;
	   </td>
	   <% if( useMLS ){ %>
	   <td class="textcontent">
	     <c:out value="${org1.mlsLabFlag}"/>
	   	  &nbsp;
	    </td>
	   <% } %>
     </tr>
	</c:forEach>
	</form:form>
</table>
