<%@ page language="java"
	contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants, 
			org.openelisglobal.internationalization.MessageUtil,
			     org.openelisglobal.common.formfields.FormFields,
			     org.openelisglobal.common.formfields.FormFields.Field,
			     org.openelisglobal.sample.util.AccessionNumberUtil,
			     org.openelisglobal.common.util.ConfigurationProperties.Property,
			     org.openelisglobal.common.util.*" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>

 <%
	 boolean supportSTNumber = FormFields.getInstance().useField(Field.StNumber);
	 boolean supportMothersName = FormFields.getInstance().useField(Field.MothersName);
	 boolean supportSubjectNumber = FormFields.getInstance().useField(Field.SubjectNumber);
	 boolean supportNationalID = FormFields.getInstance().useField(Field.NationalID);
	 boolean supportLabNumber = FormFields.getInstance().useField(Field.SEARCH_PATIENT_WITH_LAB_NO);
 %>
 
<script>
var date = new Date()
var offset = date.getTimezoneOffset()

var pageNumber = ${(startIndex / 50) + 1};
var firstPage = pageNumber == 1;
var lastPage = ${total == endIndex};
	
function searchFhirReferrals() {
	var externalAccessionNumber = jQuery("#externalAccessionNumber").val();
// 	var patientID = jQuery("#patientID").val();
	var patientFirstName = jQuery("#patientFirstName").val();
	var patientLastName = jQuery("#patientLastName").val();
// 	var dateOfBirth = jQuery("#dateOfBirth").val();
	var gender = jQuery("#gender").val();
	const params = new URLSearchParams({
		"page": "1",
		});
	if (externalAccessionNumber) {
		params.append("externalAccessionNumber",externalAccessionNumber);
	}
// 	if (patientID) {
// 		params.append("patientID", patientID);
// 	}
	if (patientFirstName) {
		params.append("patientFirstName", patientFirstName);
	}
	if (patientLastName) {
		params.append("patientLastName", patientLastName);
	}
// 	if (dateOfBirth) {
// 		params.append("dateOfBirth", dateOfBirth);
// 	}
	if (gender) {
		params.append("gender", gender);
	}
	window.location.href = "FhirReferralReception?" + params.toString();
}

function nextPage() {
	var externalAccessionNumber = jQuery("#externalAccessionNumber").val();
// 	var patientID = jQuery("#patientID").val();
	var patientFirstName = jQuery("#patientFirstName").val();
	var patientLastName = jQuery("#patientLastName").val();
// 	var dateOfBirth = jQuery("#dateOfBirth").val();
	var gender = jQuery("#gender").val();
	const params = new URLSearchParams({
		"page": (pageNumber + 1),
		});
	if (externalAccessionNumber) {
		params.append("externalAccessionNumber",externalAccessionNumber);
	}
// 	if (patientID) {
// 		params.append("patientID", patientID);
// 	}
	if (patientFirstName) {
		params.append("patientFirstName", patientFirstName);
	}
	if (patientLastName) {
		params.append("patientLastName", patientLastName);
	}
// 	if (dateOfBirth) {
// 		params.append("dateOfBirth", dateOfBirth);
// 	}
	if (gender) {
		params.append("gender", gender);
	}
	window.location.href = "FhirReferralReception?" + params.toString();
}

function prevPage() {
	var externalAccessionNumber = jQuery("#externalAccessionNumber").val();
// 	var patientID = jQuery("#patientID").val();
	var patientFirstName = jQuery("#patientFirstName").val();
	var patientLastName = jQuery("#patientLastName").val();
// 	var dateOfBirth = jQuery("#dateOfBirth").val();
	var gender = jQuery("#gender").val();
	const params = new URLSearchParams({
		"page": (pageNumber -1),
		});
	if (externalAccessionNumber) {
		params.append("externalAccessionNumber",externalAccessionNumber);
	}
// 	if (patientID) {
// 		params.append("patientID", patientID);
// 	}
	if (patientFirstName) {
		params.append("patientFirstName", patientFirstName);
	}
	if (patientLastName) {
		params.append("patientLastName", patientLastName);
	}
// 	if (dateOfBirth) {
// 		params.append("dateOfBirth", dateOfBirth);
// 	}
	if (gender) {
		params.append("gender", gender);
	}
	window.location.href = "FhirReferralReception?" + params.toString();
}

function enterOrder(eOrderId, eOrderExternalId) {
	var closeButton = document.createElement("button");
	closeButton.setAttribute("onclick", "closeOrder('" + eOrderId + "')");
	closeButton.setAttribute("id", "close_" + eOrderId);
	closeButton.textContent = "<spring:message code="label.button.exit"/>"
	
	var iframe = document.createElement("iframe");
	iframe.setAttribute("id", "iframe_" + eOrderId);
	iframe.setAttribute("src", "SamplePatientEntry?attemptAutoSave=true&ID=" + eOrderExternalId);
	iframe.style.width = "640px";
	iframe.style.height = "480px";
	var container = document.getElementById("iframecontainer_" + eOrderId);
	container.appendChild(closeButton);
	container.appendChild(document.createElement("br"));
	container.appendChild(iframe);
}

function closeOrder(eOrderId) {
	var container = document.getElementById("iframecontainer_" + eOrderId);
	while(container.firstChild){
		container.removeChild(container.firstChild); 
	}
}



function enableEnhancedSearchButton(eventCode){
	var enhancedSearchButton = jQuery("#enhancedSearchButton");
	enhancedSearchButton.removeAttr("disabled");
	
// 	var patientIdNumberSearch = document.getElementById("patientID");
// 	patientIdNumberSearch.addEventListener("keyup", function(event) {
// 		if(event.keyCode === 13) {
// 			event.preventDefault();
// 			document.getElementById("enhancedSearchButton").click();
// 		}
// 	});
	var lastNameSearch = document.getElementById("patientLastName");
	lastNameSearch.addEventListener("keyup", function(event) {
		if(event.keyCode === 13) {
			event.preventDefault();
			document.getElementById("enhancedSearchButton").click();
		}
	});
	var firstNameSearch = document.getElementById("patientFirstName");
	firstNameSearch.addEventListener("keyup", function(event) {
		if(event.keyCode === 13) {
			event.preventDefault();
			document.getElementById("enhancedSearchButton").click();
		}
	});
// 	var dateOfBirthSearch = document.getElementById("dateOfBirth");
// 	dateOfBirthSearch.addEventListener("keyup", function(event) {
// 		if(event.keyCode === 13) {
// 			event.preventDefault();
// 			document.getElementById("enhancedSearchButton").click();
// 		}
// 	});
	var genderSearch = document.getElementById("gender");
// 	genderSearch.addEventListener("change", function(event) {
// 			document.getElementById("enhancedSearchButton").click();
	});
}

jQuery(window).load(function(){	
	jQuery('button.prevButton').each(function(){
		jQuery(this).prop('disabled', firstPage);
	});
	jQuery('button.nextButton').each(function(){
		jQuery(this).prop('disabled', lastPage);
	});
});
</script>

<h2><spring:message code="sample.entry.search" /></h2>
	<table>
		<tr>
			<td style="text-align: left;"><spring:message
					code="patient.labno.search" /> :</td>
			<td><form:input
					path="externalAccessionNumber"
					id="externalAccessionNumber" 
					size="40"
					maxlength="<%= Integer.toString(AccessionNumberUtil.getMaxAccessionLength()) %>"
					oninput="enableEnhancedSearchButton(event.which);"
					placeholder='<%=MessageUtil.getMessage("label.select.search.here")%>' />
			</td>
		</tr>
<!-- 		<tr> -->
<%-- 			<td style="text-align: left;"><spring:message --%>
<%-- 					code="patient.id.number.search" /> :</td> --%>
<%--  			<td><form:input --%>
<%-- 					path="patientId"  --%>
<!-- 					id="patientID"  -->
<!-- 					size="40"  -->
<!-- 					oninput="enableEnhancedSearchButton(event.which);" -->
<%-- 					placeholder='<%=MessageUtil.getMessage("label.select.search.here")%>' /> --%>
<!-- 			</td> -->
<!-- 		</tr> -->
		<tr>
			<td style="text-align: left;"><spring:message
					code="patient.epiLastName" /> :</td>
			<td><form:input
					path="patientLastName"
					id="patientLastName" 
					size="40" 
					oninput="enableEnhancedSearchButton(event.which);"
					placeholder='<%=MessageUtil.getMessage("label.select.search.here")%>' />
			</td>
		</tr>
		<tr>
			<td style="text-align: left;"><spring:message
					code="patient.epiFirstName" /> :</td>
			<td><form:input
					path="patientFirstName"
				id="patientFirstName"
				size="40"
				oninput="enableEnhancedSearchButton(event.which);"
				placeholder='<%=MessageUtil.getMessage("label.select.search.here")%>' />
			</td>
		</tr>
		</table>
		
		
		
		<table>
		<tr>
<%-- 			<td style="text-align: right;"><spring:message --%>
<%-- 					code="patient.birthDate" />&nbsp;<%=DateUtil.getDateUserPrompt()%>:	</td> --%>
<%-- 			<td><form:input --%>
<%-- 					path="dateOfBirth"  --%>
<!-- 				id="dateOfBirth" -->
<!-- 				size="20" -->
<!-- 				onkeyup="addDateSlashes(this,event); normalizeDateFormat(this);" -->
<!--                 onchange="checkValidAgeDate( this );" -->
<!-- 				oninput="enableEnhancedSearchButton(event.which);" -->
<%-- 				placeholder='<%=MessageUtil.getMessage("label.select.search.here")%>' /> --%>
<!-- 				<div id="patientProperties.birthDateForDisplayMessage" class="blank" -->
<!-- 					style="text-align: left;"></div></td> -->
			<td style="text-align: left;"><spring:message code="patient.gender" />:</td>
			<td><form:select
					path="gender" id="gender" style="float: left"
				onchange="enableEnhancedSearchButton(event.which); checkIndex(this)" tabindex="1"
				class="patientEnhancedSearch">
					<option value=""></option>
					<form:options items="${form.genders}" itemValue="id" itemLabel="value"/>
			</form:select></td>
			<td><input type="button" name="enhancedSearchButton"
				class="patientEnhancedSearch"
				value="<%=MessageUtil.getMessage("label.patient.search")%>"
				id="enhancedSearchButton" onclick="searchFhirReferrals()"
				disabled="disabled"></td>
		</tr>
		<tr>
			<td>
			<span id="loading" class="fa-2x" hidden="hidden"><i class="fas fa-spinner fa-pulse"></i></span>
			</td>
		</tr>
	</table>

<c:if test="${empty form.electronicOrders}">
	<h2><spring:message code="eorder.noresults"/></h2>
</c:if>
<c:if test="${not empty form.electronicOrders}">
	<h2>
		<spring:message code="eorder.results"/> ${startIndex + 1} -  ${endIndex} <spring:message code="eorder.of"/> ${total}
		<button class="prevButton" onClick="prevPage(); return false;"><spring:message code="label.button.previous"/></button>
		<button class="nextButton" onClick="nextPage(); return false;"><spring:message code="label.button.next"/></button>
	</h2>
	<table style="width:100%">
		<tr>
			<td style="width:1%"></td>
			<td style="width:98%">
				<c:forEach var="eOrder" items="${form.electronicOrders}">
					
					<h3>
						<span><spring:message code="eorder.externalid"/>: 
						<c:out value="${eOrder.externalId}"/>
<%-- 						<c:if test=""> --%>
							<input type="button" 
								onclick="enterOrder(${eOrder.id}, '${eOrder.externalId}');" 
								value="<spring:message code="eorder.enterorder"/>" />
<%-- 						</c:if> --%>
						</span>
						<span style="float:right"><spring:message code="eorder.lastupdated"/>: 
						<c:if test="${empty sessionScope.timezone}">
							<fmt:formatDate value="${eOrder.lastupdated}" pattern="yyyy-MM-dd HH:mm z"/> 
						</c:if>
						<c:if test="${not empty sessionScope.timezone}">
							<fmt:formatDate value="${eOrder.lastupdated}" timeZone="${sessionScope.timezone}" pattern="yyyy-MM-dd HH:mm z"/> 
						</c:if>
						</span>
					</h3>
					<div id="info" >
						<b><spring:message code="eorder.timestamp"/>:</b> 
						<c:if test="${empty sessionScope.timezone}">
							<fmt:formatDate value="${eOrder.orderTimestamp}" pattern="yyyy-MM-dd HH:mm z"/> 
						</c:if>
						<c:if test="${not empty sessionScope.timezone}">
							<fmt:formatDate value="${eOrder.orderTimestamp}" timeZone="${sessionScope.timezone}" pattern="yyyy-MM-dd HH:mm z"/> 
						</c:if>
						<br>
						<table>
						<tr>
							<td><b><spring:message code="eorder.patient"/>:</b></td>
							<td></td>
							<td></td>
						</tr>
						<tr>
							 <td></td
							 ><td><b><spring:message code="eorder.patient.name"/>: </b><c:out value="${eOrder.patient.person.lastName}"/>, <c:out value="${eOrder.patient.person.firstName}"/></td>
							 <td><b><spring:message code="eorder.patient.gender"/>: </b><c:out value="${eOrder.patient.gender}"/></td>
						</tr>
						<tr>
							 <td></td>
							 <td><b><spring:message code="eorder.patient.birthdate"/>: </b><c:out value="${eOrder.patient.birthDateForDisplay}"/></td>
							 <td><b><spring:message code="eorder.patient.id"/>: </b><c:out value="${eOrder.patient.externalId}"/></td>
						</tr>
						</table>
						<b><spring:message code="eorder.status"/>: </b> <spring:message code="${eOrder.status.nameKey}"/><br>
						<div class="iframe-holder" id="iframecontainer_${eOrder.id}">
						</div>
					</div>
					<hr>
				</c:forEach>
			</td>
			<td style="width:1%"></td>
		</tr>
	</table>
	<h2>
		<spring:message code="eorder.results"/> ${startIndex + 1} -  ${endIndex} <spring:message code="eorder.of"/> ${total}
		<button class="prevButton" onClick="prevPage(); return false;"><spring:message code="label.button.previous"/></button>
		<button class="nextButton" onClick="nextPage(); return false;"><spring:message code="label.button.next"/></button>
	</h2>
</c:if>


<table>
<thead>
<tr>
<td>
</td>
</tr>
</thead>
<tbody>
</tbody>
</table>