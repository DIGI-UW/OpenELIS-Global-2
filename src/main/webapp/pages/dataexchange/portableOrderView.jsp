<%@ page language="java" contentType="text/html; charset=UTF-8"%>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>

 

<script type="text/javascript">
var pageNumber = ${(startIndex / 50) + 1};
var firstPage = pageNumber == 1;
var lastPage = ${total == endIndex};
	
function sortBy(sortOption) {
	const params = new URLSearchParams({
		"sortOrder": sortOption,
		});
	window.location.href = "portableorders?" + params.toString();
}

function nextPage() {
	var sortOption = jQuery("#sortSelect").val();
	const params = new URLSearchParams({
		"sortOrder": sortOption,
		"page": (pageNumber + 1),
		});
	window.location.href = "portableorders?" + params.toString();
}

function prevPage() {
	var sortOption = jQuery("#sortSelect").val();
	const params = new URLSearchParams({
		"sortOrder": sortOption,
		"page": (pageNumber -1),
		});
	window.location.href = "portableorders?" + params.toString();
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

<spring:message code="porder.sort"/>: 
<form:select path="sortOrder" id="sortSelect" onchange="sortBy(this.value)">
	<form:options items="${form.sortOrderOptions}" itemValue="value" itemLabel="label" />
</form:select>
<form:hidden path="page"/>

<c:if test="${empty form.POrders}">
	<h2><spring:message code="porder.noresults"/></h2>
</c:if>
<c:if test="${not empty form.POrders}">
	<h2>
		<spring:message code="porder.results"/> ${startIndex + 1} -  ${endIndex} <spring:message code="porder.of"/> ${total}
		<button class="prevButton" onClick="prevPage(); return false;"><spring:message code="label.button.previous"/></button>
		<button class="nextButton" onClick="nextPage(); return false;"><spring:message code="label.button.next"/></button>
	</h2>
	<table style="width:100%">
		<tr>
			<td style="width:1%"></td>
			<td style="width:98%">
				<c:forEach var="pOrder" items="${form.POrders}">
					
					<h3>
						<span><spring:message code="porder.externalid"/>: <c:out value="${pOrder.externalId}"/></span>
						<span style="float:right"><spring:message code="porder.lastupdated"/>: <c:out value="${pOrder.lastupdated}"/></span>
					</h3>
					<div id="info" >
						<b><spring:message code="porder.timestamp"/>:</b> <c:out value="${pOrder.orderTimestamp}"/><br>
						<table>
						<tr>
							<td><b><spring:message code="porder.patient"/>:</b></td>
							<td></td>
							<td></td>
						</tr>
						<tr>
							 <td></td
							 ><td><b><spring:message code="porder.patient.name"/>: </b><c:out value="${pOrder.patient.person.lastName}"/>, <c:out value="${pOrder.patient.person.firstName}"/></td>
							 <td><b><spring:message code="porder.patient.gender"/>: </b><c:out value="${pOrder.patient.gender}"/></td>
						</tr>
						<tr>
							 <td></td>
							 <td><b><spring:message code="porder.patient.birthdate"/>: </b><c:out value="${pOrder.patient.birthDateForDisplay}"/></td>
							 <td><b><spring:message code="porder.patient.id"/>: </b><c:out value="${pOrder.patient.externalId}"/></td>
						</tr>
						</table>
						<b><spring:message code="porder.status"/>: </b> <spring:message code="${pOrder.status.nameKey}"/><br>
						<b><spring:message code="porder.message"/>: </b><br> 
						<div class="colorFill message" style="white-space:pre;padding:5px;"><c:out value="${pOrder.data}"/></div>
					</div>
					<hr>
				</c:forEach>
			</td>
			<td style="width:1%"></td>
		</tr>
	</table>
	<h2>
		<spring:message code="porder.results"/> ${startIndex + 1} -  ${endIndex} <spring:message code="porder.of"/> ${total}
		<button class="prevButton" onClick="prevPage(); return false;"><spring:message code="label.button.previous"/></button>
		<button class="nextButton" onClick="nextPage(); return false;"><spring:message code="label.button.next"/></button>
	</h2>
</c:if>
