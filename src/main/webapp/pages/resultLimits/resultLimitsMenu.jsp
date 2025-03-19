<%@ page language="java"
	contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



 

<table width="100%" border=2">
	<tr>
	   <th>&nbsp;</th>
	   <th>&nbsp;</th>
	   <th>&nbsp;</th>
	   <th colspan="2" align="center">
	      <spring:message code="resultlimits.age"/>
	   </th>
	   <th>&nbsp;</th>
	   <th colspan="3" align="center">
	      <spring:message code="resultlimits.normal"/>
	   </th>
	   <th colspan="2" align="center">
	      <spring:message code="resultlimits.valid"/>
	   </th>
	</tr>
	<tr>
	   <th>
	     <spring:message code="label.form.select"/>
	   </th>
	   <th>
	   	  <spring:message code="resultlimits.test"/>
	   </th>
	   <th>
	   	  <spring:message code="resultlimits.resulttype"/>
	   </th>
	   <th>
	      <spring:message code="resultlimits.min"/>
	   </th>
	   <th>
	      <spring:message code="resultlimits.max"/>
	   </th>
	   <th>
	      <spring:message code="resultlimits.gender"/>
	   </th>
	   <th>
	      <spring:message code="resultlimits.low"/>
	   </th>
	   <th>
	      <spring:message code="resultlimits.high"/>
	   </th>
	   <th>
	   	  <spring:message code="resultLimits.dictionaryNormal" />
	   </th>
	   <th>
	      <spring:message code="resultlimits.low"/>
	   </th>
	   <th>
	      <spring:message code="resultlimits.high"/>
	   </th>
	</tr>
	<logic:iterate id="limit" name="${form.formName}" indexId="ctr" property="menuList" type="ResultLimitsLink">
	<bean:define id="limitId" name="limit" property="id"/>
	  <tr>
	   <td class="textcontent">
	      <html:multibox name="${form.formName}" property="selectedIDs">
	         <bean:write name="limitId" />
	      </html:multibox>
   	   </td>
   	   <td class="textcontent">
	   	  <bean:write name="limit" property="testName"/>
	   </td>
   	   <td class="textcontent">
	   	  <bean:write name="limit" property="resultType"/>
	   </td>
   	   <td class="textcontent" align="right" >
	   	  <bean:write name="limit" property="minDayAgeDisplay"/> / <bean:write name="limit" property="minAgeDisplay"/>
	   </td>
   	   <td class="textcontent" align="right" >
	   	  <bean:write name="limit" property="maxDayAgeDisplay"/> / <bean:write name="limit" property="maxAgeDisplay"/>
	   </td>
   	   <td class="textcontent"  align="center" >
	   	  <bean:write name="limit" property="gender"/>
	   </td>
   	   <td class="textcontent" align="right" >
	   	  <bean:write name="limit" property="lowNormalDisplay"/>
	   </td>
   	   <td class="textcontent" align="right" >
	   	  <bean:write name="limit" property="highNormalDisplay"/>
	   </td>
   	   <td class="textcontent" align="right" >
	   	  <bean:write name="limit" property="dictionaryNormal"/>
	   </td>
   	   <td class="textcontent" align="right" >
	   	  <bean:write name="limit" property="lowValidDisplay"/>
	   </td>
   	   <td class="textcontent" align="right" >
	   	  <bean:write name="limit" property="highValidDisplay"/>
	   </td>
     </tr>
	</logic:iterate>
</table>
