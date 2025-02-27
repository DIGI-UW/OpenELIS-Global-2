<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page import="org.openelisglobal.common.action.IActionConstants,
				org.openelisglobal.inventory.form.InventoryKitItem" %>


<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



	

<div id="PatientPage" class="colorFill" style="display:inline" >
	<c:if test="${not empty form.inventoryItems}" >
	<table width="40%" >
	<tr >
		<th width="10%">
			<spring:message code="inventory.testKit.id"/>
		</th>
		<th width="25%">
			<spring:message code="inventory.testKit.name"/>		
		</th>
		<th width="15%">
			<spring:message code="inventory.testKit.receiveDate"/>		
		</th>
		<th width="15%">
			<spring:message code="inventory.testKit.expiration"/>		
		</th>
		<th width="10%">
			<spring:message code="inventory.testKit.lot"/>		
		</th>
		<th width="25%">
			<spring:message code="inventory.testKit.source"/>		
		</th>
	</tr>
	<c:forEach var="inventoryItem"  items="${form.inventoryItems}" varStatus="iter">
		<c:if test="${inventoryItem.isActive}">
			<tr >
				<td >
					<c:out value="${inventoryItem.inventoryLocationId}"/>
				</td>
				<td >
					<c:out value="${inventoryItem.kitName}"/>
				</td>
				<td >
					<c:out value="${inventoryItem.receiveDate}"/>
				</td>
				<td >
					<c:out value="${inventoryItem.expirationDate}"/>
				</td>
				<td >
					<c:out value="${inventoryItem.lotNumber}"/>
				</td>
				<td >
					<c:out value="${inventoryItem.source}"/>
				</td>
			</tr>
		</c:if>
	</c:forEach>
	</table>
</c:if>
<c:if test="${empty form.inventoryItems}">
	<spring:message code="inventory.testKit.none"/>
</c:if>	

	
</div>



