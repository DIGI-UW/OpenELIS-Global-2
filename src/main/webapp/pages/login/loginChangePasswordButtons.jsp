<%@ page language="java"
	contentType="text/html; charset=UTF-8"
%>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<table width="100%">
<tr>
    <td width="50%" valign="top">
        <table width="95%">
        <tr><td colspan="4">&nbsp;</td>
        <tr>         
            <td width="20%">&nbsp;</td>
            <td width="10%" noWrap>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                                   &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                                   &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td>
            <td colspan="2" align="left">
                <form:button type="submit" value="submit" >
  		            <spring:message code="label.button.submit"/>
                </form:button>
                <spring:url var="loginUrl" value="/LoginPage"/>
                <button type="button" onclick="window.location.href='${loginUrl}'">
  		            <spring:message code="label.button.exit"/>
                </button>
            </td> 
        </tr>
        </table>
    </td>
</tr>
</table>  