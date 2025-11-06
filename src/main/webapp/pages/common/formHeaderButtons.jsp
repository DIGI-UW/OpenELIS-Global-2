<%@ page language="java"
	contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants"
%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>

 
			
<c:set var="formName" value="${form.formName}"/>
<%
	if(null != pageContext.getAttribute("formName")) {
%>
<h1>
	<c:choose>
		<c:when test="${!empty subtitle}">
			<c:out value="<%=request.getAttribute(IActionConstants.PAGE_SUBTITLE_KEY)%>" />
		</c:when>
		<c:otherwise>
			<% if ("0".equals(request.getParameter("ID"))) { %>
			  <spring:message code="default.add.title" />
			<% } else { %>
			  <spring:message code="default.edit.title" />
			<%}%>
		</c:otherwise>
	</c:choose>
</h1>
<%
	}
%>


