<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page import="org.openelisglobal.common.action.IActionConstants,
                 org.openelisglobal.common.util.DateUtil,
                 org.openelisglobal.internationalization.MessageUtil,
                 java.util.List,
                 org.openelisglobal.common.util.Versioning,
                 org.openelisglobal.referral.action.beanitems.ReferralItem,
                 org.openelisglobal.common.util.IdValuePair,
                 org.openelisglobal.referral.action.beanitems.ReferredTest" %>


<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>

<%@ taglib prefix="fn" uri="jakarta.tags.functions"%>

<script type="text/javascript" src="scripts/ajaxCalls.js?"></script>
<script type="text/javascript" src="scripts/utilities.js"></script>
<link href="select2/css/select2.min.css" rel="stylesheet" />
<script type="text/javascript" src="select2/js/select2.min.js"></script>
<script type="text/javascript">

function checkValidEntryDate(date, dateRange, blankAllowed)
{
    if((!date.value || date.value == "") && !blankAllowed){
        return;
    } else if ((!date.value || date.value == "") && blankAllowed) {
        setValidIndicaterOnField(true, date.id);
        return;
    }


    if( !dateRange || dateRange == ""){
        dateRange = 'past';
    }

    //ajax call from utilites.js
    isValidDate( date.value, validateDateSuccessCallback, date.id, dateRange );
}

function validateDateSuccessCallback(xhr) {
    var message = xhr.responseXML.getElementsByTagName("message").item(0).firstChild.nodeValue;
    var formField = xhr.responseXML.getElementsByTagName("formfield").item(0).firstChild.nodeValue;

    var isValid = message == "<%=IActionConstants.VALID%>";

    //utilites.js
    selectFieldErrorDisplay( isValid, $(formField));

    if( message == '<%=IActionConstants.INVALID_TO_LARGE%>' ){
        alert( "<spring:message code="error.date.inFuture"/>" );
    }else if( message == '<%=IActionConstants.INVALID_TO_SMALL%>' ){
        alert( "<spring:message code="error.date.inPast"/>" );
    }
	
}

jQuery(document).ready( function() {
	jQuery('.basic-multiselect').select2();
});

</script>

Results By Date / Test / Unit

Date Type: 
<form:select path="dateType">
<option value="SENT">Sent Date</option>
<option value="RESULT">Result Date</option>
</form:select>
Note if searching by result date, only tests with results will appear
<br><br>

Start Date <%=DateUtil.getDateUserPrompt()%> <form:input id='startDate' path="startDate" onkeyup="addDateSlashes(this, event);" onchange="checkValidEntryDate(this, 'any', true);"/> 
End Date <%=DateUtil.getDateUserPrompt()%> <form:input id='endDate' path="endDate" onkeyup="addDateSlashes(this, event);" onchange="checkValidEntryDate(this, 'any', true);"/>
<br><br>

Unit(s) 
<form:select path="testUnitIds" multiple="multiple" cssClass="basic-multiselect" >
<form:options items="${form.testUnitSelectionList}" itemLabel="value" itemValue="id"/>
</form:select>
Test(s)
<form:select path="testIds" multiple="multiple" cssClass="basic-multiselect">
<form:options items="${form.testSelectionList}" itemLabel="value" itemValue="id"/>
</form:select>
<br><br>
<button type="button" onclick="search('TEST_AND_DATES')">Search</button>
<hr>

Results By Lab Number
<br><br>
<form:input path="labNumber" /> Scan OR Enter Manually
<br><br>
<button type="button" onclick="search('LAB_NUMBER')">Search</button>
<hr>



