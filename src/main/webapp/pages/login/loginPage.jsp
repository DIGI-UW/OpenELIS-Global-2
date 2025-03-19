<%@ page language="java"
	contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants,
			org.openelisglobal.internationalization.MessageUtil" %>
<%@ page import="org.openelisglobal.common.constants.Constants" %>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<script>
    function validateForm(form) {
        return validateLoginForm(form);
    }

function submitOnEnter(e){

 if (enterKeyPressed(e)) {
   var button = document.getElementById("submitButton");
   e.returnValue=false;
   e.cancel = true;
   button.click();
 }

}


function submitOnClick(button){
	document.getElementById("mainForm").submit();
}

</script>
<table width="100%">
<tr>
    <td width="50%" valign="top">
        <table width="95%">
        <tr>
            <td width="20%">&nbsp;</td>
            <td colspan="2">
                <spring:message code="login.notice.message" />
            </td>
            <td width="20%">&nbsp;</td>
        </tr>
        <tr>
            <td width="20%">&nbsp;<br/><br/></td>
            <td colspan="2">
                <%= MessageUtil.getContextualMessage("login.notice.notification") %>
            </td>
            <td width="20%">&nbsp;</td>
        </tr>
        </table>
        <br>
        <table width="95%">
        <tr>
            <td width="20%">&nbsp;</td>
            <td width="10%" noWrap><spring:message code="login.msg.userName"/>:</td>
            <td colspan="2" align="left">
                <%--bugzilla 2173, 2376--%>
                <form:input autocomplete="off" path="loginName" id="loginName" onkeypress="submitOnEnter(event)"/>
            </td>
        </tr>
        <tr>
            <td width="20%">&nbsp;</td>
            <td width="10%" noWrap><spring:message code="login.msg.password"/>:</td>
            <td colspan="2" align="left">
                
                <form:password autocomplete="off" path="password" id="password" onkeypress="submitOnEnter(event)"/>
            </td>
        </tr>
        </table>
    </td>
</tr>
</table>




