<%@ page import="org.openelisglobal.common.util.ConfigurationProperties,
				org.openelisglobal.common.util.ConfigurationProperties.Property,
				org.openelisglobal.common.util.Versioning" %>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<%-- &nbsp;&nbsp;&nbsp;&nbsp;<spring:message code="ellis.configuration" text="ellis.configuration"/>:&nbsp; --%>
<%-- <%=ConfigurationProperties.getInstance().getPropertyValue( Property.configurationName )%><br/> --%>
<table width="100%">
<tr>
<td width="15%" valign="top"><jsp:include page="${leftFragment}"/></td>
<td width="85%"><jsp:include page="${rightFragment}"/></td>
</tr>
</table>

