<%@ page language="java"
	contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



 

<table width="100%" border=2">
	<tr>
	   <th>
	     <spring:message code="label.form.select"/>
	   </th>
	   <%--remove the following 09/12/2006 bugzilla 1399--%>
	   <%--
	   <th>
	      <spring:message code="test.id"/>
	   </th>
	   --%>
	   <th><%--bugzilla 1412--%>
	   	  <spring:message code="test.testSectionName"/>
	   </th>
	   <th>
	   	  <spring:message code="test.testName"/>
	   </th>
	   <%--bugzilla 1399 sorting by testSection so show it/ remove test method--%>
	   <th>
	   	  <spring:message code="test.description"/>
	   </th>
	   <th>
	   	  <spring:message code="test.isActive"/>
	   </th>
       <%--bugzilla 1784 added isReportable--%>
	   <th>
	   	  <spring:message code="test.isReportable"/>
	   </th>
	   <%--bugzilla 1856--%>
	   <th>
	   	  <spring:message code="test.sortOrder"/>
	   </th> 
    </tr>
	<logic:iterate id="tst" indexId="ctr" name="${form.formName}" property="menuList" type="org.openelisglobal.test.valueholder.Test">
	<bean:define id="tstID" name="tst" property="id"/>
	  <tr>
	   <td class="textcontent">
	      <html:multibox name='${form.formName}' property="selectedIDs">
	         <bean:write name="tstID" />
	      </html:multibox>
   	   </td>
   	   <%--remove the following 09/12/2006 bugzilla 1399--%>
	   <%--
	   <td class="textcontent">
	      <bean:write name="tst" property="id"/>
	   </td>
	   --%>
	   <td class="textcontent">
	   	 <logic:notEmpty name="tst" property="testSection">
	        <bean:write name="tst" property="testSection.testSectionName"/>
	     </logic:notEmpty>
	   </td>
	   <td class="textcontent">
	      <bean:write name="tst" property="testName"/>
	   </td>
	   <td class="textcontent">
	   	  <bean:write name="tst" property="description"/>
	   </td>
	   <td class="textcontent">
	   	  <bean:write name="tst" property="isActive"/>
	   </td>
       <%--bugzilla 1784added isReportable--%>
	   <td class="textcontent">
	   	  <bean:write name="tst" property="isReportable"/>
	   </td>
	   <%--bugzilla 1856--%>
	   <td class="textcontent">
	   	  <bean:write name="tst" property="sortOrder"/>
	   </td>
     </tr>
	</logic:iterate>
</table>
