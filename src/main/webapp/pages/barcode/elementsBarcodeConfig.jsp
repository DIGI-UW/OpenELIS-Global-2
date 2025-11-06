<%@ page language="java" contentType="text/html; charset=UTF-8" %>

<%@ page import="org.openelisglobal.common.action.IActionConstants" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



	


<h2><spring:message code="siteInfo.section.elements"/></h2>
<p><spring:message code="siteInfo.description.elements"/></p>
<table width="80%">
	<tr>
		<td><b><spring:message code="siteInfo.elements.mandatory"/></b></td>
	</tr>
	<tr>
		<td style="text-align:center"><spring:message code="barcode.label.type.order"/></td>
		<td style="text-align:center"><spring:message code="barcode.label.type.specimen"/></td>
	</tr>
	<tr>
		<td style="text-align:center">
		<table style="margin:auto;border-spacing:15px 2px;">
			<tr>
				<td><ul>
					<li><spring:message code="barcode.label.info.labnumber"/></li>
					<li><spring:message code="barcode.label.info.patientid"/></li>
					<li>Site ID</li>
				</ul></td>
				<td><ul>
					<li><spring:message code="barcode.label.info.patientname"/></li>
					<li><spring:message code="barcode.label.info.patientdobfull"/></li>
				</ul></td>
			</tr>
		</table>
		</td>
		<td style="text-align:center">
		<table style="margin:auto;border-spacing:15px 2px;">
			<tr>
				<td><ul>
					<li><spring:message code="barcode.label.info.labnumber"/></li>
					<li><spring:message code="barcode.label.info.patientid"/></li>
				</ul></td>
				<td><ul>
					<li><spring:message code="barcode.label.info.patientname"/></li>
					<li><spring:message code="barcode.label.info.patientdobfull"/></li>
				</ul></td>
			</tr>
		</table>
		</td>
	</tr>
	<tr>
		<td><b><spring:message code="siteInfo.elements.optional"/></b></td>
	</tr>
	<tr>
		<td style="text-align:center"><spring:message code="barcode.label.type.order"/></td>
		<td style="text-align:center"><spring:message code="barcode.label.type.specimen"/></td>
	</tr>
	<tr>
		<td style="text-align:center">
		<table style="margin:auto;border-spacing:15px 2px;">
			<tr>
				<td><spring:message code="barcode.label.info.none"/></td>
			</tr>
		</table>
		</td>
		<td style="text-align:center">
		<table style="margin:auto;border-spacing:15px 2px;">
			<tr>
				<td>
					<form:checkbox path="collectionDateCheck"
						value="true"
						onchange="enableSave();"/>
					<spring:message code="barcode.label.info.collectiondatetime"/>
				</td>
			</tr>
			<tr>
				<td>
					<form:checkbox path="collectedByCheck"
						value="true"
						onchange="enableSave();"/>
					<spring:message code="barcode.label.info.collectedBy"/>
				</td>
			</tr>
			<tr>
				<td>
					<form:checkbox path="testsCheck"
						value="true"
						onchange="enableSave();"/>
					<spring:message code="barcode.label.info.tests"/>
				</td>
			</tr>
			<tr>
				<td>
					<form:checkbox path="patientSexCheck"
						value="true"
						onchange="enableSave();"/>
					<spring:message code="barcode.label.info.patientsexfull"/>
				</td>
			</tr>
		</table>
		</td>
	</tr>
</table>