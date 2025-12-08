<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page import="org.openelisglobal.common.action.IActionConstants,
          	     org.openelisglobal.common.util.ConfigurationProperties,
                 org.openelisglobal.common.util.ConfigurationProperties.Property,
                 org.openelisglobal.internationalization.MessageUtil,
                 org.openelisglobal.common.util.Versioning,
                 org.openelisglobal.common.util.DateUtil" %>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<%
    boolean acceptExternalOrders = ConfigurationProperties.getInstance().isPropertyValueEqual( Property.ACCEPT_EXTERNAL_ORDERS, "true" );
%>

<link rel="stylesheet" type="text/css" href="css/jquery.asmselect.css?"/>

<script type="text/javascript" src="scripts/additional_utilities.js"></script>
<script type="text/javascript" src="scripts/jquery.asmselect.js?"></script>
<script type="text/javascript" src="scripts/ajaxCalls.js?"></script>
<script type="text/javascript" src="scripts/laborder.js?"></script>
<script type="text/javascript">

//validation logic for this 'page'
function /*bool*/ sampleOrderValid() {
	if( acceptExternalOrders){ 
    	if (missingRequiredValues())
    		return false;
    }
   	if( $("currentDate").value.blank() ){
		return false;
    } else if ($("receivedDateForDisplay").value.blank() ){
        return false;
    }
    return true;
}



function checkValidTime(time, blankAllowed)
{
    var lowRangeRegEx = new RegExp("^[0-1]{0,1}\\d:[0-5]\\d$");
    var highRangeRegEx = new RegExp("^2[0-3]:[0-5]\\d$");

    if (time.value.blank() && blankAllowed == true) {
        clearFieldErrorDisplay(time);
        setSampleFieldValid(time.name);
        checkValidSubPages();
        return;        
    }

    if( lowRangeRegEx.test(time.value) ||
        highRangeRegEx.test(time.value) )
    {
        if( time.value.length == 4 )
        {
            time.value = "0" + time.value;
        }
        clearFieldErrorDisplay(time);
        setSampleFieldValid(time.name);
    }
    else
    {
        setFieldErrorDisplay(time);
        setSampleFieldInvalid(time.name);
    }

    checkValidSubPages();
}

function studyChanged(studyElement) {
	var study = studyElement.value;
	if (study == "routine") {
		jQuery("#psuedoPatientInfo").removeAttr("disabled");
		jQuery("#facility-combobox").show();
		jQuery("#routineSampleAdd").show();
		jQuery("#viralLoadSampleAdd").hide();
		jQuery("#EIDSampleAdd").hide();
	} else if (study == "viralLoad"){
		jQuery("#psuedoPatientInfo").attr('checked', false);
		jQuery("#psuedoPatientInfo").attr('disabled', 'disabled');
		document.getElementsByName('patientInfoCheck')[0].disabled = true;
		jQuery("select#requesterId").prop("selectedIndex", 0);
		jQuery("#facility-combobox").hide();
		jQuery("#routineSampleAdd").hide();
		jQuery("#viralLoadSampleAdd").show();
		jQuery("#EIDSampleAdd").hide();
	} else if (study == "EID"){
		jQuery("#psuedoPatientInfo").attr('checked', false);
		jQuery("#psuedoPatientInfo").attr('disabled', 'disabled');
		document.getElementsByName('patientInfoCheck')[0].disabled = true;
		jQuery("select#requesterId").prop("selectedIndex", 0);
		jQuery("#facility-combobox").hide();
		jQuery("#routineSampleAdd").hide();
		jQuery("#viralLoadSampleAdd").hide();
		jQuery("#EIDSampleAdd").show();
	}
}
</script>

<div id="orderDisplay">
<table style="width:100%">
<tr>
<td>
<table>
	<tr>
    	<td><%= MessageUtil.getContextualMessage("sample.batchentry.order.currentdate") %>
    		:
	        <span class="requiredlabel">*</span>
	        <span style="font-size: xx-small; "><%=DateUtil.getDateUserPrompt()%></span>
   	 	</td>
    	<td colspan="2">
	        <form:input path="currentDate" id="currentDate" cssClass="required" readonly="true" maxlength="10"/>
	     	<%= MessageUtil.getContextualMessage("sample.batchentry.order.currenttime") %>
	     	:
	        <span style="font-size: xx-small; "><%=DateUtil.getTimeUserPrompt()%></span> 
	        <form:input path="currentTime" onkeyup="filterTimeKeys(this, event);" id="currentTime" maxlength="5"/>
   		</td>
	</tr>
	<tr>
    	<td><%= MessageUtil.getContextualMessage( "quick.entry.received.date" ) %>
        	:
	        <span class="requiredlabel">*</span>
	        <span style="font-size: xx-small; "><%=DateUtil.getDateUserPrompt()%></span>
    	</td>
    	<td colspan="2">
    		<form:input path="sampleOrderItems.receivedDateForDisplay"
                  onchange="checkValidSubPages();checkValidEntryDate(this, 'past');"
                  onkeyup="addDateSlashes(this, event);"
                  maxlength="10"
                  cssClass="text required"
                  id="receivedDateForDisplay"/>
	        <spring:message code="sample.batchentry.order.receptiontime"/>
	        :
	        <span style="font-size: xx-small; "><%=DateUtil.getTimeUserPrompt()%></span>
	        <form:input path="sampleOrderItems.receivedTime" onkeyup="filterTimeKeys(this, event);" id="receivedTime" maxlength="5"  onblur="checkValidSubPages(); checkValidTime(this, true);"/>
    	</td>
	</tr>
	<tr <c:if test="${siteInfo == 'false'}">style='display:none'</c:if>>
    	<td>
    		<spring:message code="sample.entry.project.form"/>: 
    	</td>
    	<td>
    		<form:select path="study" id="study" onchange="studyChanged(this);checkValidSubPages();">
    			<option value="routine"><spring:message code="dictionary.program.routine" /></option>
    			<option value="viralLoad"><spring:message code="sample.entry.project.VL.simple.title" /></option>
    			<option value="EID"><spring:message code="sample.entry.project.EID.title" /></option>
    		</form:select>
    	</td>
	</tr>
	<tr class="spacerRow">
    	<td>&nbsp;</td>
	</tr>
</table>
</td>
</tr>
</table>
</div>
