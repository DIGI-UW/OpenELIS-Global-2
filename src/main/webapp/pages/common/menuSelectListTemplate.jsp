<%@ page language="java"
	contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants"
 %>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>

<%-- place form definition here for menuforms --%>
	<table cellpadding="0" cellspacing="1" width="100%" height="100%" border="0">
	
			<tr valign="top">
				<td>
					<%--tiles:insertAttribute name="error"/--%>
				</td>
			</tr>
			<tr valign="top">
				<td>
					<jsp:include page="${preSelectionHeaderMenuFragment}"/>
				</td>
			</tr>
			<tr valign="top">
				<td>
					<jsp:include page="${headerMenuFragment}"/>
				</td>
			</tr>
			<tr valign="top">
				<td>
					<jsp:include page="${bodyMenuFragment}"/>
				</td>
			</tr>
			<tr valign="bottom">
				<td>
					<jsp:include page="${footerMenuFragment}"/>
				</td>
			</tr>

	</table>