<%@ page language="java" contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants,
			org.openelisglobal.common.util.resources.ResourceLocator,
			java.util.Locale,
			org.owasp.encoder.Encode, 
			org.openelisglobal.internationalization.MessageUtil"%>

<%@ page isELIgnored="false"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<%
String paginationMessage = "";
String totalCount = "0";
String fromCount = "0";
String toCount = "0";
boolean allowDeactivate = false;
boolean allowEdits = false;
String formName = "";
String searchColumn = "";
String pageInstruction = "";
String objectToAdd = "";
String adminFilterCheck = "";
String activeFilterCheck = "";
String pageSize = "0";
String filterRole = "";

if (request.getAttribute(IActionConstants.DEACTIVATE_DISABLED) != null) {
	allowDeactivate = request.getAttribute(IActionConstants.DEACTIVATE_DISABLED) != "true";
}

String addDisabled = "true";
if (request.getAttribute(IActionConstants.ADD_DISABLED) != null) {
	addDisabled = (String) request.getAttribute(IActionConstants.ADD_DISABLED);
}

//This is added for testAnalyteTestResult (we need to disable ADD until test is selected
String editable = "true";
if (request.getAttribute(IActionConstants.ALLOW_EDITS_KEY) != null) {
	editable = (String) request.getAttribute(IActionConstants.ALLOW_EDITS_KEY);
}
allowEdits = Boolean.valueOf(editable).booleanValue();

String editDisabled = "false";
if (request.getAttribute(IActionConstants.EDIT_DISABLED) != null) {
	editDisabled = (String) request.getAttribute(IActionConstants.EDIT_DISABLED);
}

boolean disableEdit = !Boolean.valueOf(editable).booleanValue() && Boolean.valueOf(editDisabled).booleanValue();

String notAllowSearching = "true";
if (request.getAttribute(IActionConstants.MENU_SEARCH_BY_TABLE_COLUMN) != null) {
	notAllowSearching = "false";

}
if (request.getAttribute(IActionConstants.MENU_SEARCH_BY_TABLE_COLUMN) != null) {
	{
		searchColumn = (String) request.getAttribute(IActionConstants.MENU_SEARCH_BY_TABLE_COLUMN);
	}
}

if (request.getAttribute(IActionConstants.FORM_NAME) != null) {
	formName = request.getAttribute(IActionConstants.FORM_NAME).toString();
}

if (request.getAttribute(IActionConstants.MENU_PAGE_INSTRUCTION) != null) {
	pageInstruction = request.getAttribute(IActionConstants.MENU_PAGE_INSTRUCTION).toString();
}

if (request.getAttribute(IActionConstants.MENU_OBJECT_TO_ADD) != null) {
	objectToAdd = request.getAttribute(IActionConstants.MENU_OBJECT_TO_ADD).toString();
}

if (request.getAttribute(IActionConstants.FILTER_CHECK_ADMIN) != null) {
	adminFilterCheck = "checked";
}

if (request.getAttribute(IActionConstants.FILTER_CHECK_ACTIVE) != null) {
	activeFilterCheck = "checked";
}

if (request.getAttribute(IActionConstants.PAGE_SIZE) != null) {
	pageSize = request.getAttribute(IActionConstants.PAGE_SIZE).toString();
}

if (request.getAttribute(IActionConstants.FILTER_ROLE) != null) {
	filterRole = request.getAttribute(IActionConstants.FILTER_ROLE).toString();
}
%>
<%
if (null != request.getAttribute(IActionConstants.FORM_NAME)) {
%>

<table width="100%" border="0" cellspacing="0" cellpadding="0">
	<tr>
		<td class="pageTitle" align="center">
			<h2>
				&nbsp;&nbsp;&nbsp;&nbsp;
				<c:if test="${not empty subtitle}">
					<c:out value="${subtitle}" />
				</c:if>
				&nbsp;&nbsp;
			</h2>
		</td>
	</tr>
</table>
<%
}
%>


<script>


function submitSearchForEnter(e){
    if (enterKeyPressed(e)) {
       var button = document.getElementById("searchButton");
       e.returnValue=false;
       e.preventDefault();
       e.cancel = true;
       button.click();
    }
    return false;
}

function submitSearchForClick(button){
	
     setMenuAction( button, document.getElementById("searchForm"), 'Search', 'yes', '?search=Y');
}
// to filter all results without first clicking the search button
function submitFilterForClick(button){
	var param = "?search=N";
    setMenuAction( button, document.getElementById("searchForm"), 'Search', 'yes', param);
}
</script>

<table border="0" cellpadding="0" cellspacing="0" width="100%">
	<tbody>
		<tr>
			<%
			if (!pageInstruction.isEmpty()) {
			%>
			<td colspan="6"><spring:message code="<%=pageInstruction%>" />
			</td>
			<%
			} else {
			%>
			<td>&nbsp;</td>
			<%
			}
			%>
		</tr>
		<tr>

			<%
			if (request.getAttribute(IActionConstants.MENU_TOTAL_RECORDS) != null) {
				totalCount = (String) request.getAttribute(IActionConstants.MENU_TOTAL_RECORDS);
			}
			if (request.getAttribute(IActionConstants.MENU_FROM_RECORD) != null) {
				fromCount = (String) request.getAttribute(IActionConstants.MENU_FROM_RECORD);
			}
			if (request.getAttribute(IActionConstants.MENU_TO_RECORD) != null) {
				toCount = (String) request.getAttribute(IActionConstants.MENU_TO_RECORD);
			}

			String msgResults = MessageUtil.getMessage("list.showing");
			String msgOf = MessageUtil.getMessage("list.of");

			paginationMessage = msgResults + " " + fromCount + " - " + toCount + " " + msgOf + " " + totalCount;
			%>

			<td>&nbsp;</td>


			<td colspan="5" align="right"><%=paginationMessage%></td>

			<%
			String previousDisabled = "false";
			String nextDisabled = "false";
			if (request.getAttribute(IActionConstants.PREVIOUS_DISABLED) != null) {
				previousDisabled = (String) request.getAttribute(IActionConstants.PREVIOUS_DISABLED);
			}
			if (request.getAttribute(IActionConstants.NEXT_DISABLED) != null) {
				nextDisabled = (String) request.getAttribute(IActionConstants.NEXT_DISABLED);
			}
			if (adminFilterCheck != "" || activeFilterCheck != "" || filterRole != "") {
				if ((Integer.valueOf(fromCount) + Integer.valueOf(pageSize) - 1) >= Integer.valueOf(totalCount)) {
					nextDisabled = "true";
				} else {
					nextDisabled = "false";
				}
				if (Integer.valueOf(fromCount) <= 1) {
					previousDisabled = "true";
				} else {
					previousDisabled = "false";
				}
			}
			%>
		</tr>
		<tr>
			<td>&nbsp;</td>
		</tr>
		<tr>
			<%-- we put "!" before disableEdit then the "Editer" button will be  always disabled at the  initialization of this page   --%>
			<td><button type="button" id="edit"
					onclick="setMenuAction(this, document.getElementById('menuForm'), '', 'yes', '?ID=');return false;"
					name="edit" <%if (disableEdit) {%> disabled="disabled" <%}%>>
					<spring:message code="label.button.modify" />
				</button> &nbsp;
				<button type="button" id="deactivate"
					onclick="setMenuAction(this, document.getElementById('menuForm'), 'Delete', 'yes', '?ID=');return false;"
					name="deactivate" disabled="disabled">
					<spring:message code="label.button.deactivate" />
				</button> &nbsp;&nbsp;

				<button type="button" id="add"
					onclick="setMenuAction(this, document.getElementById('menuForm'), '', 'yes', '?ID=0');return false;"
					name="add" <%if (Boolean.valueOf(addDisabled).booleanValue()) {%>
					disabled="disabled" <%}%>>
					<spring:message code="label.button.add" />
					<spring:message code="<%=objectToAdd%>" />

				</button></td>

			<c:if test="${not empty menuSearchByTableColumn}">
				<form:form name="${form.formName}" action="${form.formAction}"
					modelAttribute="form" onSubmit="return submitForm(this);"
					method="${form.formMethod}" id="searchForm">
					<td></td>

					<td align="right"><spring:message code="label.form.searchby" />
						<spring:message code="<%=searchColumn%>" /> <form:input
							path="searchString" onkeypress="submitSearchForEnter(event);"
							size="20" maxlength="20"
							disabled="<%=Boolean.valueOf(notAllowSearching).booleanValue()%>" />


						<button type="button" name="search" id="searchButton"
							onclick="submitSearchForClick(this);return false;"
							<%if (Boolean.valueOf(notAllowSearching).booleanValue()) {%>
							disabled="disabled" <%}%>>
							<spring:message code="label.button.search" />
						</button></td>
				</form:form>


			</c:if>

			<td></td>



			<td align="right">
				<button type="button" id="previous"
					onclick="setMenuAction(this, document.getElementById('menuForm'), '', 'yes', '?paging=1');return false;"
					name="previous"
					<%if (Boolean.valueOf(previousDisabled).booleanValue()) {%>
					disabled="disabled" <%}%>>
					<spring:message code="label.button.previous" />
				</button> &nbsp;
				<button type="button" id="next"
					onclick="setMenuAction(this, document.getElementById('menuForm'), '', 'yes', '?paging=2');return false;"
					name="next"
					<%if (Boolean.valueOf(nextDisabled).booleanValue()) {%>
					disabled="disabled" <%}%>>
					<spring:message code="label.button.next" />
				</button>
			</td>
		</tr>
		<tr>
			<td>&nbsp;</td>
		</tr>
		<tr>
			<%-- to apply filters to reasults --%>
			<c:if test="${not empty filter}">
				<td><spring:message code="menu.label.filter" /> : <select
					name="roleFilter" id="roleFilter"
					onchange="submitFilterForClick(this);return false;">
						<option value=""></option>
						<c:forEach items="${form.testSections}" var="role">
							<option value="${role.id}">${role.value}</option>
						</c:forEach>
				</select> <label for="roleFilter"> <spring:message
							code="menu.label.filter.role" /></label>&nbsp; <input
					<%=activeFilterCheck%> type="checkbox" id="isActive"
					name="isActive" onclick="submitFilterForClick(this);return false;" />
					<label for="isActive"> <spring:message
							code="menu.label.filter.active" /></label>&nbsp; <input
					<%=adminFilterCheck%> type="checkbox" id="isAdmin" name="isAdmin"
					onclick="submitFilterForClick(this);return false;" /> <label
					for="isAdmin"><spring:message
							code="menu.label.filter.admin" /></label></td>
			</c:if>
		</tr>
		<tr>
			<td>&nbsp;</td>
		</tr>
	</tbody>
</table>

<script>
   var textName = document.getElementById ("searchString");
 
   if (textName != null && textName.value != null)
   {  textName.focus();
      textName.value+='';
   }

function output() {
 var total = 0;
   for ( var i = 0; i < document.getElementById("menuForm").length; i++ ) {
      if ( document.getElementById("menuForm").elements[ i ].type == 'checkbox' ) {
         if (document.getElementById("menuForm").elements[ i ].checked == true ) {
            total++;
         }
      }
   }
     if(total == 0){
         <%if (allowEdits) {%>
    	 document.getElementById("edit").disabled=true;
    	 <%}%>		
    	 <%if (allowDeactivate) {%>
    	 	document.getElementById("deactivate").disabled=true;
    	 <%}%>	
     } else if(total == 1){
    	 <%if (allowEdits) {%>
    	 document.getElementById("edit").disabled=false;
    	 <%}%>		
    	 <%if (allowDeactivate) {%>
    	 	document.getElementById("deactivate").disabled=false;
    	 <%}%>		
     } else {
    	 <%if (allowEdits) {%>
    	 document.getElementById("edit").disabled=true;
    	 <%}%>		
    	 <%if (allowDeactivate) {%>
    	 	document.getElementById("deactivate").disabled=false;
    	 <%}%>		
     }
}

var roleFilter = document.getElementById("roleFilter");
if(roleFilter != null){
  roleFilter.value= '<%=Encode.forJavaScript((String) filterRole)%>';
}
</script>
