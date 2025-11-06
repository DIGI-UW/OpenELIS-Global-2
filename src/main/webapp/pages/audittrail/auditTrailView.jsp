<%@ page language="java"
	contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants,
	org.openelisglobal.internationalization.MessageUtil,
	org.openelisglobal.sample.util.AccessionNumberUtil,
    org.openelisglobal.common.util.Versioning"
%>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<link rel="stylesheet" media="screen" type="text/css" href="css/bootstrap.css?" />
<link rel="stylesheet" media="screen" type="text/css" href="css/openElisCore.css?" />

<script type="text/javascript" src="scripts/utilities.js?" ></script>

<script type="text/javascript">

function search(){
	window.location.href = "AuditTrailReport?accessionNumberSearch=" + jQuery('#accessionNumberSearch').val();
	return false;
}

</script>

<h1 class="page-title"><spring:message code="reports.auditTrail" /></h1>

<form:form name="${form.formName}" 
		   action="${form.formAction}" 
		   modelAttribute="form" 
		   method="${form.formMethod}"
		   class="form-horizontal" >

	<div class="row-fluid">
	    <div class="span12">
	
		<%=MessageUtil.getContextualMessage("quick.entry.accession.number")%>: 
		<form:input path="accessionNumberSearch"
					id="accessionNumberSearch"
				   cssClass="input-medium" 
	        	   maxlength="<%=Integer.toString(AccessionNumberUtil.getMaxAccessionLength())%>" />
		<input class="btn" type="button" onclick="search();" value='<%=MessageUtil.getMessage("label.button.view") %>'>
		</div>
	</div>

	
	<hr>
		
	<c:if test="${not empty form.accessionNumber}">
	
		<c:if test="${empty form.log}">
		<div class="row-fluid">
		    <div class="span6">
		    	<em><spring:message code="sample.edit.sample.notFound" /></em>
		    </div>
		</div>
		</c:if>
		
		<c:if test="${not empty form.log}" >
		
		<div class="row-fluid order-details">
		    <div class="span12">
		        <span class="order-number"><c:out value="${form.accessionNumber}"  /></span> 
		        <spring:message code="reports.auditTrail.creation" />: <span id="dateCreated"></span>
		        <spring:message code="reports.auditTrail.days" />: <span id="daysInSystem"></span>
		    </div>
		</div>
	    <div class="current" >
            <h2><spring:message code="order.information" /></h2>
            <jsp:include page="${orderInfoFragment}"/>
            <jsp:include page="${patientInfoFragment}"/>
        </div>
		<div class="row-fluid">
			<div class="span12">		
				<div id="loading" class="loading-note"><img src="images/indicator.gif" /><spring:message code="loading" /></div>
				<table class="table table-small table-hover table-bordered table-striped" id="advancedTable">
					<thead>
				    	<tr id="rowHeader">
						    <th>ID</th>
							<th><span><spring:message code="reports.auditTrail.time"/></span></th>
						    <th><span><spring:message code="reports.auditTrail.item"/></span></th>
							<th><span><spring:message code="reports.auditTrail.action"/></span></th>
						    <th><span><spring:message code="reports.auditTrail.identifier"/></span></th>
							<th><span><spring:message code="reports.auditTrail.user"/></span></th>
							<th><span><spring:message code="reports.auditTrail.old.value"/></span></th>
							<th><span><spring:message code="reports.auditTrail.new.value"/></span></th>
						</tr>
					</thead>
					<tbody>
					<c:forEach items="${form.log}" var="log" varStatus="iter">
						<tr class="${log.item}">
							<td>${iter.index}</td>
							<td class="time-stamp">${log.date} ${log.time}</td>
							<td class="item-cell">${log.item}</td>
							<td>${log.attribute}</td>
							<td class="id-number">${log.identifier}</td>
							<td>${log.user}</td>
							<td>${log.oldValue}</td>
							<td>${log.newValue}</td>
						</tr>
					</c:forEach>
					</tbody>
				</table>
				
				<div id="showOptions" class="show-table-options">
					<button class="reset-sort btn btn-mini" disabled="disabled"><i class="icon-refresh"></i> Reset</button>
					<label> <spring:message code="audit.show" /> :
				        <select id="filterByType">
				        <%--  Options for filter are added via filterByType jquery function --%>
				            <option value=""><spring:message code="audit.show.all"/></option>
				        </select>
				    </label>
				</div>
						
			</div>
		</div>
	
		</c:if>
				
	</c:if>
</form:form>	
<c:if test="${not empty form.log}" >
    <script type="text/javascript">
        function getAuditSearchText(){  return '<spring:message code="audit.search.text" />';  }
        function getAuditFilteredFrom(){  return '<spring:message code="audit.filtered.from" />';  }
        function getAuditNoPrefix(){  return '<spring:message code="audit.no.prefix" />';  }
        function getAuditEntriesDisplayed(){  return '<spring:message code="audit.entries.displayed" />';  }
        function getAuditNoRecords(){  return '<spring:message code="audit.no.records" />';  }
    </script>
<script type="text/javascript" src="scripts/oe.datatables.functions.js?"></script>
</c:if>

<script type="text/javascript">

    jQuery(document).ready( function() {
        jQuery(".current input").each( function(index,elem){ jQuery(elem).attr('readonly', true) });
        jQuery(".current .spacerRow").each( function(index, elem){jQuery(elem).hide()});
    } );
</script>