<%@ page language="java"
	contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants,
			org.openelisglobal.role.valueholder.Role" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



 

<table width="80%" border="2">
	<tr>
		<th>&nbsp;</th>
	   	<th><spring:message code="role.name" /></th>
	   	<th><spring:message code="role.description"/></th>
	   	<th><spring:message code="role.isGroupingRole"/></th>
	   	<th><spring:message code="role.parent.role"/></th>
	   	<th><spring:message code="role.display.key"/></th>
	</tr>
	<logic:iterate id="limit" name="${form.formName}" indexId="ctr" property="menuList" type="Role">
		<bean:define id="limitId" name="limit" property="id"/>
	  	<tr>
	   		<td class="textcontent">
	      		<html:multibox name="${form.formName}" property="selectedIDs">
	         		<bean:write name="limitId" />
	      		</html:multibox>
   	   		</td>
   	   		<td class="textcontent">
	    		<bean:write name="limit" property="name"/>
	   		</td>
   	  	 	<td class="textcontent">
	   	  		<bean:write name="limit" property="description"/>
	   		</td>
   	  	 	<td class="textcontent">
	   	  		<bean:write name="limit" property="groupingRole"/>
	   		</td>
   	  	 	<td class="textcontent">
	   	  		<bean:write name="limit" property="groupingParent"/>
	   		</td>
   	  	 	<td class="textcontent">
	   	  		<bean:write name="limit" property="displayKey"/>
	   		</td>
     	</tr>
	</logic:iterate>
</table>
