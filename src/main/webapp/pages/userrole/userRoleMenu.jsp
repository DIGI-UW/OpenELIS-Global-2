<%@ page language="java"
	contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants,
			org.openelisglobal.userrole.valueholder.UserRole" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



 

<table width="80%" border="2">
	<tr>
		<th>&nbsp;</th>
	   	<th><spring:message code="systemuserrole.user"/></th>
	   	<th> <spring:message code="systemuserrole.role"/> </th>
	</tr>
	<logic:iterate id="userRoles" name="${form.formName}" indexId="ctr" property="menuList" type="UserRole">
		<%--<bean:define id="limitId" name="userRoles" property="id"/> --%>
	  	<tr>
	   		<td class="textcontent">
	      		<html:multibox name="${form.formName}" property="selectedIDs">
	         		<bean:write name="userRoles" property="uniqueIdentifyer" />
	      		</html:multibox>
   	   		</td>
   	   		<td class="textcontent">
	    		<bean:write name="userRoles" property="userName"/>
	   		</td>
   	  	 	<td class="textcontent">
	   	  		<bean:write name="userRoles" property="roleName"/>
	   		</td>
     	</tr>
	</logic:iterate>
</table>
