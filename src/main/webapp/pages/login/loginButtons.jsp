<%@ page language="java"
	contentType="text/html; charset=UTF-8"
%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>


<table width="95%">
<tr><td colspan="4">&nbsp;</td>
<tr>
    <td width="20%">&nbsp;</td>	
    <td width="110" noWrap>&nbsp;</td>
    <td colspan="2" align="left">
        <%--bugzilla 2376--%>
        <spring:message code="label.button.submit" var="submitText"/>
        <input type="button" id="submitButton" onclick="submitOnClick(this);return false;" value="${submitText}"/>
  			
		<spring:message code="label.button.changePassword" var="changePasswordText"/>
		<spring:url var="changePassUrl" value="/ChangePasswordLogin" htmlEscape="true"/>
        <button type="button" onclick="window.location.href='${changePassUrl}'"><c:out value="${changePasswordText}"/> </button>
  		<c:if test="${form.useSAML}">
			<spring:message code="label.button.ssoLogin" var="ssoLoginText"/>
  			<spring:url var="ssoEndpoint" value="/LoginPage?useSAML=true" htmlEscape="true"/>
  	        <button type="button" onclick="window.location.href='${ssoEndpoint}'"><c:out value="${ssoLoginText}"/> </button>
  		</c:if>
  		</br>
  		<c:forEach items="${form.oauthUrls}" var="oauthUrl">
			<spring:message code="label.button.ssoLogin" var="ssoLoginText"/>
  			<spring:url var="ssoEndpoint" value="/${oauthUrl.value}" htmlEscape="true"/>
  	        <button type="button" onclick="window.location.href='${ssoEndpoint}'"><c:out value="${ssoLoginText}"/> </button>
  		</c:forEach>
    </td>        
</tr>         
</table>