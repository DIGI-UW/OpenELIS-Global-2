<%@ page language="java" contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<%--   --%>
<c:set var="formName" value="${form.formName}" />

<h3><spring:message code="plugin.installed.plugins" text="plugin.installed.plugins"/></h3>

<ul>
	<c:forEach items="${form.pluginList}" var="pluginNames">
		<li>
			${pluginNames}
		</li>
	</c:forEach>
<%--     <logic:iterate id="pluginNames" name="${form.formName}" property="pluginList">
		<li>
		<bean:write name="pluginNames" />
		</li>
	</logic:iterate> --%>
</ul>
