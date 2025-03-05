<%@ page language="java"
	contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.test.valueholder.TestSection,
			org.openelisglobal.common.action.IActionConstants" %>

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
	      <spring:message code="testsection.id"/>
	   </th>
	   --%>
	   <th><%--bugzilla 1412--%>
	   	  <spring:message code="testsection.organization"/>
	   </th>
	   <th>
	   	  <spring:message code="testsection.testSectionName"/>
	   </th> 
	   <th> <%-- bugzilla 2025 --%>
	      <spring:message code="testsection.parent"/>
	    </th>
	    <th>
	      <spring:message code="testsection.isExternal"/>
	   </th>
	   <th>
	   	  <spring:message code="testsection.description"/>
	   </th>
	   
	</tr>
	<logic:iterate id="testSec" indexId="ctr" name="${form.formName}" property="menuList" type="org.openelisglobal.test.valueholder.TestSection">
	<bean:define id="testSecID" name="testSec" property="id"/>
	
	  <tr>	
	   <td class="textcontent">
	      <html:multibox name="${form.formName}" property="selectedIDs">
	         <bean:write name="testSecID" />
	      </html:multibox>
     
   	   </td>
   	   <%--remove the following 09/12/2006 bugzilla 1399--%>
	   <%--
	   <td class="textcontent">
	      <bean:write name="testSec" property="id"/>
	   </td>
	   --%>

	   <td class="textcontent">
	   <logic:notEmpty name="testSec" property="organization">
	   	  <bean:write name="testSec" property="organization.organizationName"/>
	   	</logic:notEmpty>
	   </td>
	   <td class="textcontent">
          <bean:write name="testSec" property="testSectionName"/>
       </td>
       
       <%-- bugzilla 2025 --%>
       <td class="textcontent">
	    <logic:notEmpty name="testSec" property="parentTestSection">
	        <bean:write name="testSec" property="parentTestSection.testSectionName"/>
	    </logic:notEmpty>
	   </td>
       
	   <td class="textcontent">
	   	  <bean:write name="testSec" property="isExternal"/>
	   </td>
	   <td class="textcontent">
	     <bean:write name="testSec" property="description"/>
	   	  &nbsp;
	    </td>
     </tr>
	</logic:iterate>
</table>
