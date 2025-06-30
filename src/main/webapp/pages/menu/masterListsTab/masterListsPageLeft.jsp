<%@ page language="java" contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.util.ConfigurationProperties,
			org.openelisglobal.common.formfields.AdminFormFields,
			org.openelisglobal.common.formfields.AdminFormFields.Field,
			org.openelisglobal.siteinformation.valueholder.SiteInformation"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>


<%--id is important for activating the menu tabs: see tabs.js from struts-menu for how masterListsSubMenu is used--%>
<%-- similar code will need to be added in the left panel and in tabs.js for any menu tab that has the submenu on the left hand side--%>

<ul id="masterListsSubMenu" class="leftnavigation">
<c:forEach items="${form.adminMenuItems}" var="adminMenuItem">
	<li>
		<a href=<c:url value="${adminMenuItem.path}"/> >
			<spring:message code="${adminMenuItem.messageKey}" />
		</a>
	</li>
</c:forEach>
</ul>
