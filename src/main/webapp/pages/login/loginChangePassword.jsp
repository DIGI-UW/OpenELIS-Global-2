<%@ page language="java"
	contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants,
			org.openelisglobal.common.provider.validation.PasswordValidationFactory" %>   

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<script>
    function validateForm(form) {
        return validateLoginChangePasswordForm(form);
    }
    
    function validateLoginChangePasswordForm(form) {
    	return true;
    }

</script>


<table width="100%">
<tr>
    <td width="50%" valign="top">
    <table width="95%">    
    <tr>
        <td width="20%">&nbsp;</td>	
        <td colspan="2"><%=PasswordValidationFactory.getPasswordValidator().getInstructions() %></td>
        <td width="20%">&nbsp;</td>
    </tr>
    </table> 
    <br>
    <table width="95%">             
    <tr>
        <td width="20%">&nbsp;</td>	
        <td width="10%" align="right" noWrap><spring:message code="login.msg.userName"/>:</td>
        <td colspan="2" align="left">
            <form:input id="loginName" path="loginName"/>
        </td>
    </tr>   
     <tr>
        <td width="20%">&nbsp;</td>
        <td width="10%" noWrap><spring:message code="login.msg.password.current"/>:</td>
        <td colspan="2" align="left">
            <form:password id="password" path="password"/> 
        </td>
     </tr>
     <tr>
        <td colspan="4">&nbsp;</td>
     </tr>
     <tr>
        <td width="20%">&nbsp;</td>
        <td width="10%" align="right" noWrap><spring:message code="login.msg.password.new"/>:</td>
        <td colspan="2" align="left">
            <form:password id="newPassword" path="newPassword"/>
        </td> 
     </tr>
     <tr>
        <td width="20%">&nbsp;</td>
        <td width="10%" align="right" noWrap><spring:message code="login.msg.password.new.again"/>:</td>
        <td colspan="2" align="left">
            <form:password id="confirmPassword" path="confirmPassword"/>
        </td>  
     </tr>                    
    </table>    
    </td>
</tr>
</table>       


