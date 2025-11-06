<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.List,
                org.openelisglobal.common.action.IActionConstants,
				org.openelisglobal.sample.util.AccessionNumberUtil,
                org.openelisglobal.common.util.Versioning,
				org.openelisglobal.internationalization.MessageUtil,
				org.owasp.encoder.Encode" %>



<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>


<c:set var="type" value="${form.type}" />
<c:set var="pagingSearch" value="${form.paging.searchTermToPage}" />

<%-- N.B. testReflex.js is dependent on utilities.js so order is important  --%>
<script type="text/javascript" src="scripts/utilities.js?" ></script>
<script type="text/javascript" src="scripts/ajaxCalls.js?" ></script>
<script type="text/javascript" src="scripts/testReflex.js?" ></script>
<script type="text/javascript" src="scripts/OEPaging.js?"></script>
<script type="text/javascript" >

var dirty = false;

var pager = new OEPager('${form.formName}', '<spring:escapeBody javaScriptEscape="true">${(type == "") ? "" : "&type=" +=  type}</spring:escapeBody>');
pager.setCurrentPageNumber('<c:out value="${form.paging.currentPage}"/>');

var pageSearch; //assigned in post load function

var pagingSearch = new Object();
<c:forEach items="${pagingSearch}" var="paging" >
pagingSearch['${paging.id}'] = '${paging.value}';
</c:forEach>


jQuery(document).ready( function() {
			var searchTerm = '<%=Encode.forJavaScript(request.getParameter("searchTerm"))%>';

			pageSearch = new OEPageSearch( document.getElementById("searchNotFound"), "td", pager );

			if( searchTerm != "null" ){
				 pageSearch.highlightSearch( searchTerm, false );
			}

			});

function /*void*/ setModifiedFlag( index ){
	$("isModified_" + index ).value = true;

}

function /*void*/ makeDirty(){
	dirty=true;
	if( typeof(showSuccessMessage) === 'function' ){
		showSuccessMessage(false); //refers to last save
	}
	// Adds warning when leaving page if content has been entered into makeDirty form fields
	function formWarning(){ 
    return "<spring:message code="banner.menu.dataLossWarning"/>";
	}
	window.onbeforeunload = formWarning;
}

function  /*void*/ setMyCancelAction(form, action, validate, parameters)
{
	//first turn off any further validation
	setAction(document.getElementById("mainForm"), 'Cancel', 'no', '');
}

function  /*void*/ savePage()
{
	window.onbeforeunload = null; // Added to flag that formWarning alert isn't needed.
	var form = document.getElementById("mainForm");
	form.action = "AnalyzerResults"  + '<spring:escapeBody javaScriptEscape="true">${(type == "") ? "": "?type=" += type}</spring:escapeBody>';
	form.submit();

}

function validateAccessionNumberOnServer(field )
{
	new Ajax.Request (
                      'ajaxXML',  //url
                      {//options
                      method: 'get', //http method
                      parameters: 'provider=SampleEntryAccessionNumberValidationProvider&field=' + field.id + '&accessionNumber=' + field.value,
                      indicator: 'throbbing',
      				requestHeaders : {
    					"X-CSRF-Token" : getCsrfToken()
    				},
                      onSuccess:  processAccessionSuccess,
                      onFailure:  processAccessionFailure
                           }
                          );
}

function processAccessionSuccess(xhr)
{

	var formField = xhr.responseXML.getElementsByTagName("formfield").item(0);
	var message = xhr.responseXML.getElementsByTagName("message").item(0);
	var success = false;

	if (message.firstChild.nodeValue == "valid"){
		success = true;
	}

	setValidIndicaterOnField(success, formField.firstChild.nodeValue );
}

function processAccessionFailure(xhr)
{
	//unhandled error: someday we should be nicer to the user
}


function checkAccessionNumber( field )
{
	if ( field.value != null) {
		validateAccessionNumberOnServer( field );
	} else {
		setValidIndicaterOnField(false, name);
	}

}

function /*void*/ enableDisableCheckboxes( callingElement, index ){
	disableCheckbox( "accepted_" + index, callingElement, index);
	disableCheckbox( "rejected_" + index, callingElement, index);
	disableCheckbox( "deleted_" + index, callingElement, index);
}

function disableCheckbox( tag, callingElement){
	var box = $(tag);
	if( box && box != callingElement){
		box.checked = false;
	}
}

function /*void*/ markUpdated(){
	$("saveButtonId").disabled = false;
	makeDirty();
}

function toggleSelectAll( element ) {
    var index, item, checkboxes,matchedCheckboxes;

	if (element.id == "selectAllAccept" ) {
		checkboxes = jQuery(".accepted");
		matchedCheckboxes = jQuery(".rejected");
	} else if (element.id == "selectAllReject" ) {
		checkboxes = jQuery(".rejected");
		matchedCheckboxes = jQuery(".accepted");
	} else if (element.id == "selectNormalAccept" ) {
		checkboxes = jQuery(".normalAccepted");
		matchedCheckboxes = jQuery(".rejected");
	}
	else if (element.id == "selectAllIgnore" ) {
		checkboxes = jQuery(".ignored");
		matchedCheckboxes = jQuery(".rejected");
	}

	if (element.checked == true ) {
		for (index = 0; index < checkboxes.length; ++index) {
			  item = checkboxes[index];
			  item.checked = true;
		}
		for (index = 0; index < matchedCheckboxes.length; ++index) {
			  item = matchedCheckboxes[index];
			  item.checked = false;
		}
	} else if (element.checked == false ) {
		for (index = 0; index < checkboxes.length; ++index) {
			  item = checkboxes[index];
			  item.checked = false;
		}
	}

}

function onLISSuccess(xhr) {
	if (xhr.status == '200') {
		alert("action ran successfully")
	}
}

</script>

<select id="lisAction" onChange="document.getElementById('lisRunButton').disabled = false;">
	<option disabled selected value=""><spring:message code="analyzer.runlisaction.default" text="--- supported actions ---"/></option>
<c:forEach items="${form.supportedLISActions}" var="action" varStatus="action_iter">
	<option value="${action.actionName}">${action.actionName}</option>
</c:forEach>
</select>
<button id="lisRunButton" type="button" disabled onClick="runLIStoAnalyzerAction('Aquios', jQuery('#lisAction').val(), onLISSuccess)">
<spring:message code="analyzer.runlisaction.button" text="Run Action"/>
</button>


<form:hidden path="type"/>
<c:if test="${form.displayNotFoundMsg}">
	 <div class="indented-important-message"><spring:message code="result.noResultsFound"/></div>
</c:if>
<c:if test="${form.displayMissingTestMsg}">
     <h2><spring:message code="error.missing.test.mapping" /></h2><br/><br/>
</c:if>

<c:if test="${not form.displayNotFoundMsg}">
	<div class="alert alert-info" style="width: 540px">
		<div><strong><spring:message code="validation.accept"/></strong>: <spring:message code="validation.accept.explanation"/></div>
		<div><strong><spring:message code="validation.reject"/></strong>: <spring:message code="validation.reject.explanation"/></div>
		<div><strong><spring:message code="validation.delete"/></strong>: <spring:message code="validation.delete.explanation"/></div>
	</div>
</c:if>

<c:if test="${form.paging.totalPages != 0}">
	<form:hidden path="paging.currentPage" id="currentPageID" />
	<c:set var="total" value="${form.paging.totalPages}"/>
	<c:set var="currentPage" value="${form.paging.currentPage}"/>

	<input type="button" value='<%=MessageUtil.getMessage("label.button.previous") %>' style="width:100px;" onclick="pager.pageBack();" 
		<c:if test="${currentPage == '1'}">disabled="disabled"</c:if> />
	<input type="button" value='<%=MessageUtil.getMessage("label.button.next") %>' style="width:100px;" onclick="pager.pageFoward();" 
		<c:if test="${total == currentPage}">disabled="disabled"</c:if> />

	&nbsp;
	<c:out value="${form.paging.currentPage}"/> of
	<c:out value="${form.paging.totalPages}"/>
	<div class='textcontent' style="float: right" >
	<span style="visibility: hidden" id="searchNotFound"><em><%= MessageUtil.getMessage("search.term.notFound") %></em></span>
	<%=MessageUtil.getContextualMessage("result.sample.id")%> : &nbsp;
	<input type="text"
	       id="labnoSearch"
	       maxlength='<%=Integer.toString(AccessionNumberUtil.getMaxAccessionLength())%>' />
	<input type="button" onclick="pageSearch.doLabNoSearch(document.getElementById('labnoSearch'))" value='<%= MessageUtil.getMessage("label.button.search") %>'>
	</div>
	<Table style="width:80%" >
    <tr>
		<th colspan="2" style="background-color: white;width:15%;">
			<img src="./images/nonconforming.gif" /> = <%= MessageUtil.getContextualMessage("result.nonconforming.item")%>
		</th>
		<%-- <th style="text-align:center;width:3%;" style="background-color: white">&nbsp;
				<spring:message code="validation.accept.normal" />
			<input type="checkbox"
				name="selectNormalAccept"
				value="on"
				onclick="toggleSelectAll(this);"
				onchange="markUpdated(); makeDirty();"
				id="selectNormalAccept"
				class="acceptNormal">
		</th> --%>
		<th style="text-align:center;width:4%;" style="background-color: white">&nbsp;
				<spring:message code="validation.accept.all" />
			<input type="checkbox"
				name="selectAllAccept"
				value="on"
				onclick="toggleSelectAll(this);"
				onchange="markUpdated(); makeDirty();"
				id="selectAllAccept"
				class="accepted acceptAll">
		</th>
		<th  style="text-align:center;width:4%;" style="background-color: white">&nbsp;
		<spring:message code="validation.reject.all" />
			<input type="checkbox"
					name="selectAllReject"
					value="on"
					onclick="toggleSelectAll(this);"
					onchange="markUpdated(); makeDirty();"
					id="selectAllReject"
					class="rejected rejectAll">
		</th>
		<th  style="text-align:center;width:4%;" style="background-color: white">&nbsp;
		<spring:message code="Ignore All Results" />
			<input type="checkbox"
					name="selectAllIgnore"
					value="on"
					onclick="toggleSelectAll(this);"
					onchange="markUpdated(); makeDirty();"
					id="selectAllIgnore"
					class="ignored ignoreAll">
		</th>
		<th style="background-color: white;width:5%;">&nbsp;</th>
  	</tr>
</Table>
</c:if>
<table id="importDataTable" width="85%" border="0" cellspacing="0" >
	<tr><td><b><spring:message code="analyzer.results.results"/></b></td></tr>
	<tr>
		<th width="4%"><spring:message code="validation.accept"/></th>
		<th width="4%"><spring:message code="validation.reject"/></th>
		<th width="4%"><spring:message code="validation.delete"/></th>
		<th width="12%">
			<spring:message code="analyzer.results.labno"/>
		</th>
		<th width="15%">
			<spring:message code="analyzer.results.test"/>
		</th>
		<th width="13%">
			<spring:message code="analyzer.results.result"/>
		</th>
		<th width="8%">
			<spring:message code="result.test.date"/>
		</th>
		<th width="5%" ><spring:message code="analyzer.results.notes"/></th>
	</tr>
	<c:forEach items="${form.resultList}" var="resultList" varStatus="iter">
		<c:set var="showAccessionNumber" value="${resultList.accessionNumber != currentAccessionNumber}"/>
		<c:set var="itemReadOnly" value="${resultList.readOnly}"/>
		<c:if test="${showAccessionNumber}">
			<c:set var="currentAccessionNumber" value="${resultList.accessionNumber}"/>
			<c:set var="groupReadOnly" value="${resultList.groupIsReadOnly}"/>
		</c:if>
		<form:hidden path="resultList[${iter.index}].id" />
		<form:hidden path="resultList[${iter.index}].sampleGroupingNumber" />
		<form:hidden path="resultList[${iter.index}].readOnly" />
		<form:hidden path="resultList[${iter.index}].testResultType"/>
		<form:hidden path="resultList[${iter.index}].testId"  id='"testId_${iter.index}'/>
		<form:hidden path="resultList[${iter.index}].accessionNumber"  id='accessionNumberId_${iter.index}'/>
		<tr <c:if test="${resultList.isHighlighted}"> class="yellowHighlight"> </c:if> >
			<td  align="center">
			<c:if test="${showAccessionNumber && not groupReadOnly}">
				<form:checkbox path="resultList[${iter.index}].isAccepted"
							   id='accepted_${iter.index}'
							   onchange="markUpdated();"
							   cssClass="accepted"
							   onclick='enableDisableCheckboxes(this, ${iter.index} );' />
			 </c:if>
			</td>
			<td  align="center">
			<c:if test="${showAccessionNumber && not groupReadOnly}">
				<form:checkbox path="resultList[${iter.index}].isRejected"
							   id='rejected_"${iter.index}'
							   onchange="markUpdated();"
							   cssClass="rejected"
							   onclick='enableDisableCheckboxes(this, ${iter.index} );' />
			</c:if>
			</td>
				<td align="center">
			 <c:if test="${showAccessionNumber}">
				<form:checkbox path="resultList[${iter.index}].isDeleted"
							   id='deleted_${iter.index}'
							   cssClass="ignored"
							   onchange="markUpdated();"
							   onclick='enableDisableCheckboxes(this, ${iter.index} );' />
			 </c:if>
			</td>
			<td class='${resultList.accessionNumber}'>
				<c:if test="${showAccessionNumber}">
						<c:out value="${resultList.accessionNumber}" />
						<c:if test="${resultList.nonconforming}">
							<img src="./images/nonconforming.gif" />
						</c:if>
				</c:if>
			</td>
			<td>
				<span style="color: #000000"><c:out value="${resultList.testName}"/></span>
			</td>
			<td>
			    <c:if test="${groupReadOnly || itemReadOnly}">
				<form:input path="resultList[${iter.index}].result"
						   readonly="true"
						   size="20"
						   style="text-align:right;border-style:hidden;background-color:transparent" />
				</c:if><c:if test="${not (groupReadOnly || itemReadOnly)}">
				<c:if test="${resultList.testResultType == 'D'}">
					<form:select path="resultList[${iter.index}].result" 
								 id='resultId_${iter.index}'
					             onchange="markUpdated(); ${(resultList.userChoiceReflex) ? 'showUserReflexChoices(' += iter.index += ', null );' : ''} " >
					    <form:options items="${resultList.dictionaryResultList}" itemValue="id" itemLabel="dictEntry" />
					</form:select>
				</c:if>
				<c:if test="${resultList.testResultType != 'D'}">
					<form:input path="resultList[${iter.index}].result"
							   id='resultId_${iter.index}' 
					           style="text-align:right;"
					           onchange="markUpdated();"/>
				</c:if>
				<c:if test="${resultList.userChoiceReflex}">
				
				</c:if>
				</c:if>
				<c:out value="${resultList.units}"/>
			</td>
			<td >
			    <c:if test="${groupReadOnly || itemReadOnly}">
				<form:input path="resultList[${iter.index}].completeDate"
						   readonly="true"
						   size="10"
						   style="border-style:hidden;text-align:right;background-color:transparent" />
				</c:if><c:if test="${not (groupReadOnly || itemReadOnly)}">
				<form:input path="resultList[${iter.index}].completeDate"
						   onchange="markUpdated();"
						   size="10"
						   style="text-align:right" />
				</c:if>
			</td>
			<td align="center">
			<c:if test="${not (groupReadOnly || itemReadOnly)}">
						<c:if test="${empty resultList.note}">
						 	<img src="./images/note-add.gif"
						 	     onclick='showHideNotes(${iter.index});'
						 	     id='showHideButton_${iter.index}'
						    />
						 </c:if>
						 <c:if test="${not empty resultList.note}">
						 	<img src="./images/note-edit.gif"
						 	     onclick='showHideNotes( ${iter.index});'
						 	     id='showHideButton_${iter.index}'
						    />
						 </c:if>
				<input type="hidden" id='hideShow_${iter.index}' value="hidden" />
            </c:if>

		</td>
		</tr>
		<tr id='noteRow_${iter.index}' style="display: none;">
			<td colspan="2" valign="top" align="right"><spring:message code="note.note"/>:</td>
			<td colspan="6" align="left" >
			<form:textarea path="resultList[${iter.index}].note" 
						   id='note_${iter.index}' 
			           	   cols="100"
			           	   rows="3"
			           	   onchange="markUpdated();"/>
			</td>
		</tr>
		<c:if test="${resultList.userChoiceReflex}">
		<c:if test="${not resultList.isHighlighted}">
		<tr id='reflexInstruction_${iter.index}' ${resultList.selectionOneText == "" ? "style='display:none'" : " " } class="alert alert-info" >
            <td colspan="4" >&nbsp;</td>
			<td colspan="4" valign="top"  ><spring:message code="testreflex.actionselection.instructions" /></td>
		</tr>
		<tr id='reflexSelection_${iter.index}' ${resultList.selectionOneText == "" ? "style='display:none'" : " " } class="alert alert-info" >
		<td colspan="4" >&nbsp;</td>
		<td colspan="4" >
				<form:radiobutton path="resultList[${iter.index}].reflexSelectionId"
						    styleId='selectionOne_${iter.index}'
							value='${resultList.selectionOneValue}'
							onchange="reflexChoosen(${iter.index}, null, '${resultList.siblingReflexKey}');" />
							<label id='selectionOneLabel_${iter.index}'  for='selectionOne_${iter.index}' >${resultList.selectionOneText}</label>
				<br/>
				<form:radiobutton path="resultList[${iter.index}].reflexSelectionId"
							styleId='selectionTwo_${iter.index}'
							value='${resultList.selectionTwoValue}'
							onchange="reflexChoosen(${iter.index}, null , '${resutList.siblingReflexKey}');" />
							<label id='selectionTwoLabel_${iter.index}'  for='selectionTwo_${iter.index}' >${resultList.selectionTwoText}</label>
			</td>
		</tr>
	</c:if>
	</c:if>



	</c:forEach>

</table>
<c:if test="${form.paging.totalPages != 0}">
	<c:set var="total" value="${form.paging.totalPages}"/>
	<c:set var="currentPage" value="${form.paging.currentPage}"/>

	<input type="button" value='<%=MessageUtil.getMessage("label.button.previous") %>' style="width:100px;" onclick="pager.pageBack();" <c:if test="${currentPage == '1'}">disabled="disabled"</c:if> />
	<input type="button" value='<%=MessageUtil.getMessage("label.button.next") %>' style="width:100px;" onclick="pager.pageFoward();" <c:if test="${total == currentPage}">disabled="disabled"</c:if> />

	&nbsp;
	<c:out value="${form.paging.currentPage}"/> of
	<c:out value="${form.paging.totalPages}"/>
</c:if>

	<ajax:autocomplete
		  var="autocomplete"
		  source="testName"
		  target="selectedTestID"
		  baseUrl="ajaxAutocompleteXML"
		  className="autocomplete"
		  parameters="testName={testName},provider=TestAutocompleteProvider,fieldName=testName,idName=id"
		  indicator="indicator"
		  minimumCharacters="1"
		  parser="new ResponseXmlToHtmlListParser()"
 	/>
