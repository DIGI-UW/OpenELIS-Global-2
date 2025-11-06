
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page import="org.openelisglobal.common.action.IActionConstants,
			     org.openelisglobal.common.formfields.FormFields,
			     org.openelisglobal.common.formfields.FormFields.Field,
				 org.openelisglobal.sample.util.AccessionNumberUtil,
			     org.openelisglobal.common.util.ConfigurationProperties.Property,
			     org.openelisglobal.common.util.*, org.openelisglobal.reports.form.ReportForm.DateType, 
			     org.openelisglobal.internationalization.MessageUtil" %>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<%
    boolean useReferralSiteList = FormFields.getInstance().useField( FormFields.Field.RequesterSiteList );
	boolean useSiteDepartment = FormFields.getInstance().useField(Field.SITE_DEPARTMENT );
%>
	
<c:set var="formName" value="${form.formName}"/>

<script type="text/javascript" src="scripts/ajaxCalls.js?" ></script>
<script type="text/javascript">

var useReferralSiteList = <%= useReferralSiteList%>;
var useSiteDepartment = <%= useSiteDepartment %>;

function siteListChanged(siteList) {
    var siteList = $("requesterId");

	if ( useSiteDepartment ) {
		if(document.getElementById("requesterId").selectedIndex != 0){
			getDepartmentsForSiteClinic( document.getElementById("requesterId").value, "", siteDepartmentSuccess, null);
		} 
	}
}

function siteDepartmentSuccess (xhr) {
    console.log(xhr.responseText);
    var message = xhr.responseXML.getElementsByTagName("message").item(0).firstChild.nodeValue;
	var departments = xhr.responseXML.getElementsByTagName("formfield").item(0).childNodes[0].childNodes;
	var selected = xhr.responseXML.getElementsByTagName("formfield").item(0).childNodes[1];
	var isValid = message == "<%=IActionConstants.VALID%>";
	var requesterDepartment = jQuery("#requesterDepartmentId");
	var i = 0;

	requesterDepartment.disabled = "";
	if( isValid ){
		requesterDepartment.children('option').remove();
		requesterDepartment.append(new Option('', ''));
		for( ;i < departments.length; ++i){
// 						is this supposed to be value value or value id?
		requesterDepartment.append(
				new Option(departments[i].attributes.getNamedItem("value").value, 
					departments[i].attributes.getNamedItem("id").value));
		}
	}
	
	if( selected){
		requesterDepartment.selectedIndex = getSelectIndexFor( "requesterDepartmentId", selected.childNodes[0].nodeValue);
	}
}


</script>

<input type="hidden" id="searchLabNumber">

<div id="siteSearch" style="display:inline;" >
<table>
<%-- <% if( useReferralSiteList ){ %> --%>
<tr>
    <td>
        <%= MessageUtil.getContextualMessage( "sample.entry.project.siteName" ) %>:
    		<form:select path="referringSiteId" 
    				 id="requesterId" 
                     onchange="siteListChanged(this);"
	 >
            <option ></option>
            <form:options items="${form.referringSiteList}" itemValue="id" itemLabel="value"/>
            </form:select>
    </td>
</tr>
	<% if( useSiteDepartment ){ %>
	<tr>
	    <td>
	        <%= MessageUtil.getContextualMessage( "sample.entry.project.siteDepartmentName" ) %>:
	    		<form:select path="referringSiteDepartmentId" 
	    				 id="requesterDepartmentId" 
	                     >
	            <option value="0" ></option>
	            <form:options items="${form.referringSiteDepartmentList}" itemValue="id" itemLabel="value"/>
	            </form:select>
	    </td>
	</tr>
	<% } %>
<%-- <% } %> --%>
<tr>
  		<td><spring:message code="report.patient.site.description"/></td>
</tr>
<tr>
  		<td><form:checkbox path="onlyResults" /> <spring:message code="report.label.site.onlyResults"/></td>
</tr>
<tr>
  		<td><spring:message code="report.label.site.dateType"/>
	    <form:select path="dateType" 
	    		 id="dateTypeId" 
	             >
	    	<option value="<%= DateType.RESULT_DATE.toString() %>"><%= MessageUtil.getContextualMessage( DateType.RESULT_DATE.getMessageKey() ) %></option>
	    	<option value="<%= DateType.ORDER_DATE.toString() %>"><%= MessageUtil.getContextualMessage( DateType.ORDER_DATE.getMessageKey() ) %></option>
	    </form:select>
	    </td>
</tr>
<tr>
	<td>
	  	<spring:message code="report.date.start"/>&nbsp;<%=DateUtil.getDateUserPrompt()%>
		<form:input path="lowerDateRange" cssClass="input-medium" onkeyup="addDateSlashes(this, event);" onchange="checkValidEntryDate(this, 'any', true);" maxlength="10"/>
	  	<spring:message code="report.date.end"/>&nbsp;<%=DateUtil.getDateUserPrompt()%>
	  	<form:input path="upperDateRange" cssClass="input-medium" maxlength="10" onkeyup="addDateSlashes(this, event);" onchange="checkValidEntryDate(this, 'any', true);"/>
	</td>
</tr>

</table>
</div>