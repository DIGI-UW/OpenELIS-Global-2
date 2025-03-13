<%@ page language="java" 
		contentType="text/html; charset=UTF-8" %>
<%@ page import="org.openelisglobal.common.action.IActionConstants,
				org.openelisglobal.common.formfields.AdminFormFields,
				org.openelisglobal.common.formfields.AdminFormFields.Field" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



 

<table width="80%" border="2">
	<tr>
	   	<th><spring:message code="externalconnect.menu.title" /></th>
	</tr>
	<logic:iterate id="urlForDisplay" name="${form.formName}" property="menuList">
	  	<tr>
   	   		<td class="textcontent">
	    		<a href="<bean:write name="urlForDisplay" property="urlAddress"/>"><bean:message name="urlForDisplay" property="displayKey"/></a>
	   		</td>
     	</tr>
	</logic:iterate>
</table>