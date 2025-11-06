<%@ page language="java"
	contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants,
            org.openelisglobal.sample.util.AccessionNumberUtil,
		    org.openelisglobal.common.util.DateUtil,
			org.openelisglobal.internationalization.MessageUtil,
			org.openelisglobal.common.util.Versioning,
			org.openelisglobal.reports.form.ReportForm.DateType,
			org.owasp.encoder.Encode" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>


<%-- Creates updated UI. Removing for current release 
<link rel="stylesheet" media="screen" type="text/css" href="css/bootstrap.min.css?" />
<link rel="stylesheet" media="screen" type="text/css" href="css/openElisCore.css?" />
--%>

<script type="text/javascript" src="scripts/utilities.js?" ></script>

<script type="text/javascript">


function datePeriodUpdated(selectionElement){
	var selectedValue = selectionElement.options[selectionElement.selectedIndex].value;

    if( selectedValue == "custom"){
    	$("lowerMonth").disabled = false;
    	$("lowerYear").disabled = false;
    	$("upperMonth").disabled = false;
    	$("upperYear").disabled = false;
    }else{
    	clearDate( "lowerMonth");
    	clearDate( "lowerYear");
    	clearDate( "upperMonth");
    	clearDate( "upperYear");
    	
    	$("dateWarning").style.visibility = "hidden";
    }
}

function clearAllDates(){
		setDateWarning( false, "" );
}

function clearDate( id ){
	var element = $(id);
	element.value="";
	element.disabled = true;
	element.style.borderColor = "";	
}
function /*boolean*/ formCorrect(){
	var datePeriod = $("datePeriod");
	
	if( datePeriod ){
		var selectedValue = datePeriod.options[datePeriod.selectedIndex].value;
		if( selectedValue == "custom"){
			var hasMissingValue = false;
						
			if( missingValue("lowerYear")){ hasMissingValue = true;}
			if( missingValue("lowerMonth")){ hasMissingValue = true;}
			if( missingValue("upperYear")){ hasMissingValue = true;}
			if( missingValue("upperMonth")){ hasMissingValue = true;}
		
			if( hasMissingValue){ 
				$("dateWarning").innerHTML = "Dates are required for custom range";
				return false;
			}
			
			var lowerYearValue = $("lowerYear").value;
			var lowerMonthValue = $("lowerMonth").value;
			var upperYearValue = $("upperYear").value;
			var upperMonthValue = $("upperMonth").value;
			
			var low = getCompoundDate( lowerYearValue, lowerMonthValue);
			var upper = getCompoundDate(upperYearValue, upperMonthValue);
			
			if( low >= upper){
				setDateWarning( true, "Start date must be before end date" );
				return false;	
			}
			
			var yearDiff = parseInt(upperYearValue - lowerYearValue );
			
			if( yearDiff > 1 ){
			 	setDateWarning( true, "Maximum date span is 12 months" );
				return false;
			}else if( yearDiff == 1 ){
				var monthDiff = parseInt(upperMonthValue - lowerMonthValue );
				
				if( monthDiff >= 0){
					setDateWarning( true, "Maximum date span is 12 months"  );
					return false;
				}
			}
		}
	}

	return true;
}

function setDateWarning( on, warning ){
				$("lowerYear").style.borderColor = on ? "red" : "";	
				$("lowerMonth").style.borderColor = on ? "red" : "";	
				$("upperYear").style.borderColor = on ? "red" : "";	
				$("upperMonth").style.borderColor = on ? "red" : "";
				$("dateWarning").innerHTML = warning;
}

function /*int*/ getCompoundDate( year, month){
	month = month.length == 1 ? "0" + month : month;
	return parseInt( year + month); 

}

function /*boolean*/ missingValue( id ){
	if( $(id).value == ""){
		$(id).style.borderColor = "red";
		return true;
	}

	return false;
}

function onCancel(){
	var form = document.getElementById("mainForm");
	form.action = "CancelReport";
	form.submit();
	return true;
}

function onPrint(){
	if( formCorrect()){
		var form = document.getElementById("mainForm");
		form.action = "ReportPrint";
		form.target = "_blank";
		form.method = 'get';
		form.submit();
		return false;
	}
	
	return true;
}

function checkValidEntryDate(date, dateRange, blankAllowed)
{
    if((!date.value || date.value == "") && !blankAllowed){
        setSaveButton();
        return;
    } else if ((!date.value || date.value == "") && blankAllowed) {
        setValidIndicaterOnField(true, date.id);
        setSaveButton();
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
    setSaveButton();

    if( message == '<%=IActionConstants.INVALID_TO_LARGE%>' ){
        alert( "<spring:message code="error.date.inFuture"/>" );
    }else if( message == '<%=IActionConstants.INVALID_TO_SMALL%>' ){
        alert( "<spring:message code="error.date.inPast"/>" );
    }
	
}

function setSaveButton() {
	jQuery('#printNew').attr('disabled', errorsOnForm());
}

</script>
<script>
   jQuery(document).ready( function() {
      jQuery('#allLabUnits').click();
    });

   function selectAll(element) {
	 var checkBoxes = jQuery('.' + element.id );
	 checkBoxes.prop("checked", !checkBoxes.prop("checked"));
  }
</script>

<h2><c:out value="${form.reportName}"/></h2>

<div class="oe-report">

<form:hidden path="report" id="reportRequest"/>
<form:hidden path="type" id="reportType"/>

<c:if test="${not form.noRequestSpecifications}">
  <div>

	  <c:if test="${form.useAccessionDirect}">
  		<h3><spring:message code="report.enter.labNumber.headline"/></h3>
	  	<spring:message code="sample.search.scanner.instructions"/>.
	  </c:if>
	  <c:if test="${form.useHighAccessionDirect}">
	  	<spring:message code="sample.search.scanner.instructions.highaccession"/>
	  </c:if>
	  <br>
	  <br>
	  <c:if test="${form.useAccessionDirect}">
		<span style="padding-left: 10px">
			<%= MessageUtil.getContextualMessage("report.from") %>
		</span>
		<form:input path="accessionDirect"
				   cssClass="input-medium" 
				   maxlength='<%= Integer.toString(AccessionNumberUtil.getMaxAccessionLength())%>'
				   />
	  </c:if>
	  <c:if test="${form.useHighAccessionDirect}">
		<span style="padding-left: 10px"><%= MessageUtil.getContextualMessage("report.to") %></span>
			<form:input path="highAccessionDirect"
			           cssClass="input-medium"
			           maxlength='<%= Integer.toString(AccessionNumberUtil.getMaxAccessionLength())%>'/>
	  </c:if>
  </div>
  
<%--   <c:if test="${form.useHighAccessionDirect}"> --%>
<%--     <div><span style="padding-left: 10px"><%= MessageUtil.getContextualMessage("report.enter.labNumber.detail") %></span></div> --%>
<%--   </c:if> --%>

  <c:if test="${form.usePatientSearch}">
  		<h3><spring:message code="report.enter.patient.headline"/></h3>
  		<spring:message code="report.enter.patient.headline.description"/>
 	
	<jsp:include page="${patientEnhancedSearchFragment}"/>
  </c:if>
  <c:if test="${form.useSiteSearch}">
  		<h3><spring:message code="report.enter.site.headline"/></h3>
		<jsp:include page="${siteSearchFragment}"/>
  </c:if>
  <c:if test="${form.usePatientNumberDirect}">
  
	<div><strong><%= MessageUtil.getContextualMessage("report.enter.subjectNumber") %></strong></div>
  </c:if>
  <div>

	  <c:if test="${form.usePatientNumberDirect}">
		<div style="padding: 5px 0 0 10px"><form:input path="patientNumberDirect" cssClass="input-medium"/></div>
	  </c:if>

	  <c:if test="${form.useUpperPatientNumberDirect}">
	   <span style="padding-left: 10px"><%= MessageUtil.getContextualMessage("report.to") %></span>
		<form:input path="patientUpperNumberDirect" cssClass="input-medium"/>
	  </c:if>
  </div>
  
  <div>
	  <c:if test="${form.useLowerDateRange}">
	  	<span style="padding-left: 10px"><spring:message code="report.date.start"/>&nbsp;<%=DateUtil.getDateUserPrompt()%></span>
		<form:input path="lowerDateRange" cssClass="input-medium" onkeyup="addDateSlashes(this, event);" onchange="checkValidEntryDate(this, 'any', true);" maxlength="10"/>
	  </c:if>
	  <c:if test="${form.useUpperDateRange}">
	  	<span style="padding-left: 10px"><spring:message code="report.date.end"/>&nbsp;<%=DateUtil.getDateUserPrompt()%></span>
	  	<form:input path="upperDateRange" cssClass="input-medium" maxlength="10" onkeyup="addDateSlashes(this, event);" onchange="checkValidEntryDate(this, 'any', true);"/>
	  </c:if>
  </div>
 
  <c:if test="${form.useLocationCode}">
  	<div>
   	  <span style="padding-left: 10px"><spring:message code="report.select.service.location"/></span>
      <form:select path="locationCode" cssClass="text" >
		<option value=""></option><form:options items="${form.locationCodeList}" itemLabel="organizationName" itemValue="id" />
	  </form:select>
	</div>
  </c:if>
 
  <c:if test="${form.useProjectCode}">
   	<div>
	  <spring:message code="report.select.project"/>
	  <form:select path="projectCode" cssClass="text" >
		<option value=""></option><form:options  items="${form.projectCodeList}" itemLabel="localizedName" itemValue="id" />
	  </form:select>
	</div>
  	</c:if>
	<c:if test="${form.useExportDateType}">
	<div>
  		<spring:message code="report.label.site.dateType"/>
	    <form:select path="dateType" 
	    		 id="dateTypeId" 
	             >
	    	<option value="<%= DateType.ORDER_DATE.toString() %>"><%= MessageUtil.getContextualMessage( DateType.ORDER_DATE.getMessageKey() ) %></option>
	    	<option value="<%= DateType.RESULT_DATE.toString() %>"><%= MessageUtil.getContextualMessage( DateType.RESULT_DATE.getMessageKey() ) %></option>
	    	<option value="<%= DateType.PRINT_DATE.toString() %>"><%= MessageUtil.getContextualMessage( DateType.PRINT_DATE.getMessageKey() ) %></option>
	    </form:select>
	</div>
  </c:if>
  
  <c:if test="${form.useDashboard}">
   	<div>
	  <spring:message code="report.select.project"/>
	  <form:select path="vlStudyType" cssClass="text" >
		<option value=""></option><form:options  items="${form.projectCodeList}" itemLabel="localizedName" itemValue="id" />
	  </form:select>
	</div>
  </c:if>
  
  <c:if test="${form.usePredefinedDateRanges}">
   	<div>
	   <spring:message code="report.select.date.period"/>
	     <form:select path="datePeriod" cssClass="text" onchange="datePeriodUpdated(this)" id="datePeriod">
	     	<option value='year'><%= MessageUtil.getMessage("report.select.date.period.year") %></option>
	     	<option value='months3'><%= MessageUtil.getMessage("report.select.date.period.months.3") %></option>
	     	<option value='months6'><%= MessageUtil.getMessage("report.select.date.period.months.6") %></option>
	     	<option value='months12'><%= MessageUtil.getMessage("report.select.date.period.months.12") %></option>
	     	<option value='custom'><%= MessageUtil.getMessage("report.selecte.date.period.custom") %></option>
		 </form:select>
	</div>
	<div>
  	    <spring:message code="report.date.start"/>
			<form:select path="lowerMonth" 
						 cssClass="text" 
						 disabled="true" 
						 id="lowerMonth"
						 onchange="clearAllDates();" >
				<option value=""></option><form:options items="${form.monthList}" itemLabel="value" itemValue="id" />
			</form:select>
			<form:select path="lowerYear" 
						 cssClass="text"  
						 disabled="true" 
						 id="lowerYear" 
						 onchange="clearAllDates();">
				<option value=""></option><form:options items="${form.yearList}" itemLabel="value" itemValue="id" />
			</form:select>
	    <spring:message code="report.date.end"/>
			<form:select path="upperMonth" 
			             cssClass="text"   
			             disabled="true" 
			             id="upperMonth"
			             onchange="clearAllDates();">
				<option value=""></option><form:options items="${form.monthList}" itemLabel="value" itemValue="id" />
			</form:select>
			<form:select path="upperYear" 
			             cssClass="text"   
			             disabled="true" 
			             id="upperYear"
			             onchange="clearAllDates();">
				<option value=""></option><form:options items="${form.yearList}" itemLabel="value" itemValue="id" />
			</form:select>
  			<div id="dateWarning" ></div>
	</div>
  </c:if>
    <c:if test="${not empty form.selectList && form.useStatisticsParams == false}">
   	<div>
       <c:set var="selectList" value="${form.selectList}" />
       <span style="padding-left: 10px"><label for="selectList">
       <c:out value="${form.selectList.label}"/></label>
	   <form:select path="selectList.selection" cssClass="text" id="selectList">
		   <option value=""></option><form:options items="${form.selectList.list}" itemLabel="value" itemValue="id" />
       </form:select></span>
	</div>
    </c:if>
</c:if>
 <c:if test="${form.useStatisticsParams}">
	<div>
	        <b><spring:message code="report.select.labUnit"/></b><br>
			<table>
			  <tr>
					<td>
						<input type="checkbox" id="allLabUnits" onClick="selectAll(this);"/>
					</td>
					<td>
						<spring:message code="report.all"/> 
					</td>
				</tr>
			    <c:forEach items="${form.selectList.list}" var="labUnit" varStatus="iter">
				<tr>
					<td>
						<form:checkbox class="allLabUnits" path="labSections" value="${labUnit.id}"/>
					</td>
					<td>
						<c:out value="${labUnit.value}"/>
					</td>
				</tr>
			   </c:forEach>
		   </table>	
			<br>
	</div>
	<div>
	       <b><spring:message code="report.select.priority"/></b><br>
	 <table>
	       <tr>
				<td>
					<input type="checkbox" id="allPriorities" onClick="selectAll(this);"/>
				</td>
				<td>
					<spring:message code="report.all"/> 
				</td>
			</tr>
		<c:forEach items="${form.priorityList}" var="oderPriority" varStatus="iter">
		    <tr>
				<td>
					<form:checkbox class="allPriorities" path="priority" value="${oderPriority.id}"/>
				</td>
				<td>
					<c:out value="${oderPriority.value}"/>
				</td>
			</tr>
		</c:forEach>
	  </table>	
	  <br>
	</div>
	<div>
	           <b><spring:message code="report.select.timeFrame"/></b><br>
			   <spring:message code="report.select.timeFrame.note"/>
			   <br>
		<table>
		<tr>
				<td>
					<input type="checkbox" id="allTimes" onClick="selectAll(this);"/>
				</td>
				<td>
					<spring:message code="report.all"/> 
				</td>
			</tr>
		<c:forEach items="${form.receptionTimeList}" var="time" varStatus="iter">
		    <tr>
				<td>
					<form:checkbox class="allTimes" path="receptionTime" value="${time.id}" />
				</td>
				<td>
					<c:out value="${time.value}"/>
				</td>
			</tr>
		</c:forEach>
	  </table>	
	  <br>
	</div>
	<div>
	    <spring:message code="report.select.year"/><br> 
		<form:select path="upperYear" cssClass="text" id="upperYear" >
			<form:options items="${form.yearList}" itemLabel="value" itemValue="id" />
		</form:select>
	</div>

 </c:if>

<br/>
<div align="left" style="width:80%" >
<c:if test="${not empty form.instructions}">
<c:out value="${form.instructions}"/>
</c:if>
</div>
<div style="margin-left: 50px">
	<input id="printNew" type="button" class="btn" name="printNew" onclick="onPrint();" value="<%=MessageUtil.getMessage("label.button.print.new.window") %>">
</div>

</div>