<%@ page language="java"
	contentType="text/html; charset=UTF-8"
	import="org.apache.commons.validator.GenericValidator, 
			org.openelisglobal.common.action.IActionConstants,
			org.openelisglobal.sample.util.AccessionNumberUtil,
			org.openelisglobal.common.util.IdValuePair,
			org.openelisglobal.internationalization.MessageUtil,
			org.openelisglobal.common.util.Versioning,
			org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl.ResultType,
		    java.text.DecimalFormat,
			java.util.List,
			org.openelisglobal.resultvalidation.bean.AnalysisItem,
			org.openelisglobal.common.util.ConfigurationProperties,
			org.openelisglobal.common.util.ConfigurationProperties.Property,
			org.owasp.encoder.Encode" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>


<%@ taglib prefix="fn" uri="jakarta.tags.functions"%>


<c:set var="testSection"	value='${form.testSection}' />
<c:set var="results" value="${form.resultList}" />
<c:set var="pagingSearch" value='${form.paging.searchTermToPage}'/>
<c:set var="testSectionsByName" value='${form.testSectionsByName}' />
<c:set var="resultCount" value="${fn:length(results)}" />
<c:set var="rowColorIndex" value="${2}" />

<%
	int rowColorIndex = 2; 
	String searchTerm = request.getParameter("searchTerm");
	String url = request.getAttribute("jakarta.servlet.forward.servlet_path").toString();	
	//boolean showTestSectionSelect = !ConfigurationProperties.getInstance().isPropertyValueEqual(Property.configurationName, "CI RetroCI");
%>


<script type="text/javascript" src="scripts/utilities.js?" ></script>
<script type="text/javascript" src="scripts/math-extend.js?" ></script>
<script type="text/javascript" src="scripts/utilities.js?" ></script>
<script type="text/javascript" src="scripts/OEPaging.js?"></script>
<script type="text/javascript" src="scripts/jquery.asmselect.js?"></script>
<link rel="stylesheet" type="text/css" href="css/jquery.asmselect.css?" />
<script type="text/javascript" src="scripts/testReflex.js?" ></script>
<script type="text/javascript" src="scripts/multiselectUtils.js?" ></script>
<script src="scripts/ajaxCalls.js" ></script>

<script>
var dirty = false;
var pager = new OEPager('${form.formName}', '<spring:escapeBody javaScriptEscape="true">${(analyzerType == "") ? "" : "&type=" +=  analyzerType}</spring:escapeBody>');
var pager = new OEPager('${form.formName}', '<spring:escapeBody javaScriptEscape="true">${(testSection == "") ? "" : "&type=" += testSection}</spring:escapeBody>' + '&test= <spring:escapeBody javaScriptEscape="true">testName</spring:escapeBody>');
pager.setCurrentPageNumber('<c:out value="${form.paging.currentPage}"/>');

var pageSearch; //assigned in post load function

var pagingSearch = {};

<c:forEach items="${pagingSearch}" var="paging">
	pagingSearch['${paging.id}'] = '${paging.value}';
</c:forEach>

jQuery(document).ready( function() {
			var searchTerm = '<%=Encode.forJavaScript(searchTerm)%>';
            loadMultiSelects();
            jQuery("select[multiple]").asmSelect({
                removeLabel: "X"
            });

            jQuery("select[multiple]").change(function(e, data) {
                handleMultiSelectChange( e, data );
            });

            pageSearch = new OEPageSearch( document.getElementById("searchNotFound"), "td", pager );
			
            
			if( searchTerm != "null" ){
				 pageSearch.highlightSearch( searchTerm, false );
			}
            jQuery(".asmContainer").css("display","inline-block");
			});


function removeReflexesFor(){
    //no-op
}
function showUserReflexChoices(){
    //no-op
}
function  /*void*/ setMyCancelAction(form, action, validate, parameters)
{
	//first turn off any further validation
	setAction(document.getElementById("mainForm"), 'Cancel', 'no', '');
}

function /*void*/ enableDisableCheckboxes( matchedElement, groupingNumber ){
	jQuery(matchedElement).checked = false;
	
	jQuery("sampleRejected_" + groupingNumber).checked = false;
	jQuery("sampleAccepted_" + groupingNumber).checked = false;
	jQuery("selectAllReject").checked = false;
	jQuery("selectAllAccept").checked = false;
}

function /*void*/ acceptSample(element, groupingNumber ){
	jQuery(".accepted_" + groupingNumber).each( function(item){
		item.checked = element.checked;
		}
	);

	jQuery(".rejected_" + groupingNumber).each( function(item){
		item.checked = false;
		}
	);
	
	jQuery("sampleRejected_" + groupingNumber).checked = false;
	jQuery("selectAllReject").checked = false;
	jQuery("selectAllAccept").checked = false;
}

function /*void*/ rejectSample(element, groupingNumber ){
	jQuery(".accepted_" + groupingNumber).each( function(item){
		item.checked = false;
		}
	);

	jQuery(".rejected_" + groupingNumber).each( function(item){
		item.checked = element.checked;
		}
	);
	
	jQuery("sampleAccepted_" + groupingNumber).checked = false;
	jQuery("selectAllAccept").checked = false;
	jQuery("selectAllReject").checked = false;
}

function /*void*/ markUpdated(){
	jQuery("#saveButtonId").prop('disabled', false);
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

function savePage() {
    if( !confirm("<%=MessageUtil.getMessage("validation.save.message")%>")){
        return;
    }

  window.onbeforeunload = null; // Added to flag that formWarning alert isn't needed.
	var form = document.getElementById("mainForm");
	form.action = "ResultValidation" + '?type=<spring:escapeBody javaScriptEscape="true">${testSection}</spring:escapeBody>&test=<spring:escapeBody javaScriptEscape="true">${testName}</spring:escapeBody>&';
	form.submit();
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

function updateLogValue(element, index ){
	var logField = jQuery("log_" + index );

	if( logField ){
		var logValue = Math.baseLog(element.value).toFixed(2);

		if( isNaN(logValue) ){
			jQuery(logField).html("--");
		}else{
			jQuery(logField).html(logValue);
		}
	}
}

function toggleSampleSelectAll( element,groupingNumber ) {
    var index, item, checkboxes,matchedCheckboxes;
	
	if (element.id == "sampleAccepted_" + groupingNumber ) {
		 checkboxes = jQuery(".accepted_" + groupingNumber);
		 matchedCheckboxes = jQuery(".rejected_" + groupingNumber);

	} else if (element.id == "sampleRejected_" + groupingNumber) {
		checkboxes = jQuery(".rejected_" + groupingNumber);
		 matchedCheckboxes = jQuery(".accepted_" + groupingNumber);
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


function trim(element, significantDigits){
    if( isNaN(significantDigits) || isNaN(element.value) ){
        return;
    }

    element.value = round(element.value, significantDigits);
}

function updateReflexChild( group){
 	var reflexGroup = jQuery(".reflexGroup_" + group);
	var childReflex = jQuery(".childReflex_" + group);
 	var i, childId, rowId, resultIds = "", values="", requestString = "";

 	if( childReflex ){
 		childId = childReflex[0].id.split("_")[1];
 		
		for( i = 0; i < reflexGroup.length; i++ ){
			if( childReflex[0] != reflexGroup[i]){
				rowId = reflexGroup[i].id.split("_")[1];
				resultIds += "," + jQuery("resultIdValue_" + rowId).value;
				values += "," + reflexGroup[i].value;
			}
		}
		
		requestString +=   "results=" +resultIds.slice(1) + "&values=" + values.slice(1) + "&childRow=" + childId;

		new Ajax.Request (
                      'ajaxQueryXML',  //url
                      {//options
                      method: 'get', //http method
                      parameters: 'provider=TestReflexCD4Provider&' + requestString,
                      indicator: 'throbbing',
      				  requestHeaders : {
    					  "X-CSRF-Token" : getCsrfToken()
    				  },
                      onSuccess:  processTestReflexCD4Success,
                      onFailure:  processTestReflexCD4Failure
                           }
                          );
 	}

}

function /*void*/ processTestReflexCD4Failure(){
	alert("failed");
}

function /*void*/ processTestReflexCD4Success(xhr)
{
	//alert( xhr.responseText );
	var formField = xhr.responseXML.getElementsByTagName("formfield").item(0);
	var message = xhr.responseXML.getElementsByTagName("message").item(0);
	var childRow, value;


	if (message.firstChild.nodeValue == "valid"){
		childRow = formField.getElementsByTagName("childRow").item(0).childNodes[0].nodeValue;
		value = formField.getElementsByTagName("value").item(0).childNodes[0].nodeValue;
		
		if( value && value.length > 0){
			jQuery("resultId_" + childRow).value = value;
		}

	}

}

/*It is not clear why this is needed but what it prevents is the browser trying to do a submit on 'enter'  Other pages handle this
differently.  Overrides formTemplate.jsp handleEnterEvent
*/
function /*boolean*/ handleEnterEvent(){
	return false;
}

function altAccessionHighlightSearch(accessionNumber) {
	if (confirm('Searching for an individual Lab no will take you to a new page.\n\nUnsaved data on this page will be lost.\n\nWould you like to continue?')) {
		window.onbeforeunload = null;
		var params = new URLSearchParams("accessionNumber=" + accessionNumber);
		window.location = "AccessionValidation?" + params.toString();
	}
}

</script>

<c:if test="${resultCount != 0}">
<div  style="width:80%" >
	<form:hidden path="paging.currentPage" id="currentPageID" />
	<c:set var="total" value="${form.paging.totalPages}"/>
	<c:set var="currentPage" value="${form.paging.currentPage}"/>
	1- <c:out value="${pageSize}"/>
	<c:if test="${analysisCount != 0}">
		 of <c:out value="${analysisCount}"/>
	</c:if>
	<c:if test="${empty analysisCount}">
	<button type="button" style="width:100px;" onclick="pager.pageBack();" <c:if test="${currentPage == 1}">disabled="disabled"</c:if>>
		<spring:message code="label.button.previous"/>
	</button>
	<button type="button" style="width:100px;" onclick="pager.pageFoward();" <c:if test="${currentPage == total}">disabled="disabled"</c:if>>
		<spring:message code="label.button.next"/>
	</button>
	&nbsp;
	<c:out value="${form.paging.currentPage}"/> <spring:message code="report.pageNumberOf" />
	<c:out value="${form.paging.totalPages}"/>
	</c:if>
	<span style="float : right" >
	<span style="visibility: hidden" id="searchNotFound"><em><%= MessageUtil.getMessage("search.term.notFound") %></em></span>
	<%=MessageUtil.getContextualMessage("result.sample.id")%> : &nbsp;
	<input type="text"
	       id="labnoSearch"
	       placeholder='<spring:message code="sample.search.scanner.instructions"/>'
	       maxlength='<%= Integer.toString(AccessionNumberUtil.getMaxAccessionLength())%>' />
	<input type="button" onclick="pageSearch.doLabNoSearch(document.getElementById('labnoSearch'))" value='<%= MessageUtil.getMessage("label.button.search") %>'>
	</span>
</div>
</c:if>
<form:hidden path="testSection" value="${param.type}"/>
<form:hidden path="testName" value="${param.test}"/>
<c:if test="${resultCount != 0}">
<Table style="width:80%" >
    <tr>
		<th colspan="2" style="background-color: white;width:15%;">
			<img src="./images/nonconforming.gif" /> = <%= MessageUtil.getContextualMessage("result.nonconforming.item")%>
		</th>
		<th style="text-align:center;width:3%;" style="background-color: white">&nbsp;
				<spring:message code="validation.accept.normal" />
			<input type="checkbox"
				name="selectNormalAccept"
				value="on"
				onclick="toggleSelectAll(this);"
				onchange="markUpdated(); makeDirty();"
				id="selectNormalAccept"
				class="acceptNormal">
		</th>
		<th style="text-align:center;width:3%;" style="background-color: white">&nbsp;
				<spring:message code="validation.accept.all" />
			<input type="checkbox"
				name="selectAllAccept"
				value="on"
				onclick="toggleSelectAll(this);"
				onchange="markUpdated(); makeDirty();"
				id="selectAllAccept"
				class="accepted acceptAll">
		</th>
		<th  style="text-align:center;width:3%;" style="background-color: white">&nbsp;
		<spring:message code="validation.reject.all" />
			<input type="checkbox"
					name="selectAllReject"
					value="on"
					onclick="toggleSelectAll(this);"
					onchange="markUpdated(); makeDirty();"
					id="selectAllReject"
					class="rejected rejectAll">
		</th>
		<th style="background-color: white;width:5%;">&nbsp;</th>
  	</tr>
</Table>
</c:if>
<Table style="width:80%" >
	<c:if test="${resultCount != 0}">
	<tr>
	    <td colspan="9"><hr/></td>
    </tr>    
	<tr>
    	<th>
			<spring:message code="resultsentry.accessionNumber"/>
		</th>
		<th>
	  		<spring:message code="sample.entry.project.testName"/>
		</th>
		<th>
			<spring:message code="analyzer.results.result"/>
		</th>
		<th style="text-align:center">
			<spring:message code="validation.accept" />
		</th>
		<th style="text-align:center">
			<spring:message code="validation.reject" />
		</th>
		<th>
			<spring:message code="result.notes"/>
		</th>
  	</tr>

	<c:forEach items="${form.resultList}" var="resultList" varStatus="iter" >
		<c:set var="showAccessionNumber" value="${resultList.accessionNumber != currentAccessionNumber}"/>
		<c:if test="${showAccessionNumber}">
			<c:set var="currentAccessionNumber" value="${resultList.accessionNumber}"/>
			<c:set var="rowColorIndex" value="${rowColorIndex + 1}"/>
		</c:if>
			<form:hidden path="resultList[${iter.index}].accessionNumber"/>
			<form:hidden path="resultList[${iter.index}].analysisId"/>
			<form:hidden path="resultList[${iter.index}].testId"/>
			<form:hidden path="resultList[${iter.index}].sampleId"/>
			<form:hidden path="resultList[${iter.index}].resultType"/>
			<form:hidden path="resultList[${iter.index}].sampleGroupingNumber"/>
			<form:hidden path="resultList[${iter.index}].noteId"/>
			<form:hidden path="resultList[${iter.index}].resultId" id='resultIdValue_${iter.index}'/>
            <form:hidden path="resultList[${iter.index}].hasQualifiedResult" id='hasQualifiedResult_${iter.index}' />

			<c:if test="${resultList.multipleResultForSample && showAccessionNumber}">
				<c:set var="showAccessionNumber" value="${false}"/>
			<tr  class='${(rowColorIndex % 2 == 0) ? "evenRow" : "oddRow"}' >
				<td colspan="3" class='${currentAccessionNumber}'>
	      			<c:out value="${resultList.accessionNumber}"/>
	    		</td>
	    		<td style="text-align:center">
					<form:checkbox path="resultList[${iter.index}].sampleIsAccepted"
								   id='sampleAccepted_${resultList.sampleGroupingNumber}'
								   cssClass="accepted"
								   onchange="markUpdated(); makeDirty();" 
								   name="sampleAccepted_${resultList.sampleGroupingNumber}'"
								   onclick='toggleSampleSelectAll(this,"${resultList.sampleGroupingNumber}");'/>

				</td>
				<td style="text-align:center">
					<form:checkbox path="resultList[${iter.index}].sampleIsRejected"
								   id='sampleRejected_${resultList.sampleGroupingNumber}'
								   cssClass="rejected"
								   name="sampleAccepted_${resultList.sampleGroupingNumber}'"
								   onchange="markUpdated(); makeDirty();"
								   onclick='toggleSampleSelectAll(this,"${resultList.sampleGroupingNumber}");'/>

				</td>
				<td>&nbsp;</td>
			</tr>
			</c:if>
     		<tr id='row_${iter.index}' class='${(rowColorIndex % 2 == 0) ? "evenRow" : "oddRow" }'  >
				<c:if test="${showAccessionNumber}">
	    		<td class='${currentAccessionNumber}' >
	      			<c:out value="${resultList.accessionNumber}"/>
	    		</td>
	    		</c:if><c:if test="${not showAccessionNumber}">
	    			<td></td>
	    		</c:if>
				<td>
					<c:out value="${resultList.testName}"/>
					<c:if test="${resultList.nonconforming}">
						<img src="./images/nonconforming.gif" />
					</c:if>
				</td>
				<td>
					<c:choose>
					<c:when test="${'N' == resultList.resultType}">
	    					<form:input path='resultList[${iter.index}].result' 
					           size="6" 
					           id='resultId_${iter.index}'
					           disabled="${resultList.readOnly}"
							   cssClass="${resultList.isHighlighted ? 'invalidHighlight' : ''}
							   		  ${resultList.reflexGroup ? ' reflexGroup_' += resultList.sampleGroupingNumber : ''}  
							          ${resultList.childReflex ? ' childReflex_' += resultList.sampleGroupingNumber : ''}"
							   onchange='markUpdated(); makeDirty(); updateLogValue(this, ${iter.index}); trim(this, "${resultList.significantDigits}"); showHideNotes(${iter.index});
								         ${(resultList.reflexGroup && (not resultList.childReflex)) ? "updateReflexChild(" += resulList.sampleGroupingNumber += " ); " : ""}'
								                />
						<c:out value="${resultList.units}"/>
					</c:when><c:when test="${'D' == resultList.resultType}">
						<form:select path="resultList[${iter.index}].result" 
						        id='resultId_${iter.index}' 
						        onchange= 'markUpdated(); makeDirty();
						         	${(resultList.qualifiedDictionaryId != null) ? "showQuanitiy( this, " += iter.index += ", " += resultList.qualifiedDictionaryId += ");" : "" }'  
						        >
						        <form:options items="${resultList.dictionaryResults}" itemValue="id" itemLabel="value"/>
						</form:select>
						<form:input path='resultList[${iter.index}].qualifiedResultValue'
			           			id='qualifiedDict_${iter.index}'
			           			style = 'display:${(resultList.hasQualifiedResult) ? "inline" : "none"}'
					   			disabled="${resultList.readOnly}" 
					   			/>
<!-- 					   			TODO make this a configurable option instead of hardcoded -->
					   	<c:if test="${false}">
                   	 		<c:out value="${resultList.units}"/>
                    	</c:if>
					</c:when><c:when test="${'M' == resultList.resultType}">
                    <%-- multiple results --%>
                    <form:select path="resultList[${iter.index}].multiSelectResultValues"
                            id='resultId_${iter.index}_0'
                            multiple="multiple"
                            disabled="${resultList.readOnly}"
                            onchange='markUpdated(${iter.index});
						              ${(empty resultList.multiSelectResultValues) ? "showNote( " += iter.index += ");" : ""}
						              ${(resultList.qualifiedDictionaryId != null) ? "showQuanitiy( this, " += iter.index += ", " += resultList.qualifiedDictionaryId += ", \\"M\\" );" :""}' 
						               >
						<form:options items="${resultList.dictionaryResults}" itemValue="id" itemLabel="value"/>
                    </form:select>
                    <form:hidden path="resultList[${iter.index}].multiSelectResultValues" id='multiresultId_${iter.index}' cssClass="multiSelectValues"  />
                    <form:input path='resultList[${iter.index}].qualifiedResultValue'
                           id='qualifiedDict_${iter.index}'
                           style='display:${(resultList.hasQualifiedResult) ? "inline" : "none"}'
                           disabled='${resultList.readOnly}'
                           onchange='markUpdated(${iter.index});'
                            />
                    <c:out value="${resultList.units}"/>
                    </c:when><c:when test="${'C' == resultList.resultType}">
                    <%-- cascading multiple results --%>
                    <div id='cascadingMulti_${iter.index}_0' class='cascadingMulti_${iter.index}' >
                    <input type="hidden" id='divCount_${iter.index}' value="0" >
                    <form:select path="resultList[${iter.index}].multiSelectResultValues"
                            id='resultId_${iter.index}_0'
                            multiple="multiple"
                            disabled='${resultList.readOnly}'
                            onchange='markUpdated(${iter.index});
						               ${(resultList.multiSelectResultValues) ? "showNote( " += iter.index += ");" : ""}
						               ${(resultList.qualifiedDictionaryId != null) ? "showQuanitiy( this, " += iter.index += ", " += resultList.qualifiedDictionaryId += ", \\"M\\" );" :""}' >
                        <form:options items="${resultList.dictionaryResults}" itemValue="id" itemLabel="value"/>
                    </form:select> 
                        <input class='addMultiSelect${iter.index}' type="button" value="+" onclick='addNewMultiSelect(${iter.index}, this);showNewNote( ${iter.index});'/>
                        <input class='removeMultiSelect${iter.index}' type="button" value="-" onclick='removeMultiSelect(\"target\");showNewNote( ${iter.index});' style="display: none" />
                        <form:hidden path="resultList[${iter.index}].multiSelectResultValues" id='multiresultId_${iter.index}'  cssClass="multiSelectValues" />
                    <form:input path='resultList[${iter.index}].qualifiedResultValue'
                           id='qualifiedDict_${iter.index}'
                           style = 'display:${resultList.hasQualifiedResult ? "inline" : "none"}'
                           disabled="${resultList.readOnly}"
                           onchange='markUpdated(${iter.index});'
                            />
                    <c:out value="${resultList.units}"/>
                     </div>
                    </c:when><c:when test="${'A' == resultList.resultType}">
                    <form:input path="resultList[${iter.index}].result"
                              size="16"
                              allowEdits='${!resultList.readOnly}'
                              id='resultId_${iter.index}'
                              onchange='markUpdated(); makeDirty(); updateLogValue(this, ${iter.index});'/>
                    <c:out value="${resultList.units}"/>

                    </c:when><c:when test="${'R' == resultList.resultType}">
						<form:textarea path="resultList[${iter.index}].result"
								  rows="2"
								  allowEdits='${!resultList.readOnly}'
								  id='resultId_${iter.index}'
								  onkeyup='value = value.substr(0, 200); markUpdated();'
								  />
						<c:out value="${resultList.units}"/>
			 			<br/>200 char max
					</c:when>
					</c:choose>
					<c:if test="${resultList.displayResultAsLog}">
						<br/>
						<div id='log_${iter.index}'
								class='results-readonly'>
								${resultList.result}
								<%-- <% try{
												Double value = Math.log10(Double.parseDouble(resultList.getResult()));
												DecimalFormat twoDForm = new DecimalFormat("##.##");
												out.print(Double.valueOf(twoDForm.format(value)));
												}catch(Exception e){
													out.print("--");} %>	 --%>	
						</div> log
					</c:if>
				</td>
				<c:if test="${resultList.showAcceptReject}">
				<td style="text-align:center">
					<form:checkbox path="resultList[${iter.index}].isAccepted"
								   id='accepted_${iter.index}'
								   cssClass='accepted accepted_${resultList.sampleGroupingNumber} ${resultList.normal ? "normalAccepted" : "" }'
								   onchange="markUpdated(); makeDirty();"
								   onclick='enableDisableCheckboxes("rejected_${iter.index}", "${resultList.sampleGroupingNumber}");' 
								   />
				</td>
				<td style="text-align:center">
					<form:checkbox path="resultList[${iter.index}].isRejected"
									   id='rejected_${iter.index}'
									   cssClass='rejected rejected_${resultList.sampleGroupingNumber}'
									   onchange="markUpdated(); makeDirty();"
									   onclick='enableDisableCheckboxes("accepted_${iter.index}", "${resultList.sampleGroupingNumber}");' 
									   />
				</td>
				</c:if><c:if test="${not resultList.showAcceptReject}">
				<td><spring:message code="label.computed"/></td><td><spring:message code="label.computed"/></td>
				</c:if>
				<td style="text-align:center">
					<c:if test="${!resultList.readOnly}">
				    	<c:if test="${empty resultList.note}">
						 	<img src="./images/note-add.gif"
						 	     onclick='showHideNotes( ${iter.index});'
						 	     id='showHideButton_${iter.index}'
						    />
						 </c:if>
						 <c:if test="${not empty resultList.note}">
						 	<img src="./images/note-edit.gif"
						 	     onclick='showHideNotes( ${iter.index});'
						 	     id='showHideButton_${iter.index}'
						    />
						 </c:if>
                    <input type="hidden" value="hidden" id='hideShow_${iter.index}' >
					</c:if>
				</td>
      		</tr>
      		<c:if test='${not empty resultList.pastNotes}'>
			<tr  >
				<td colspan="2" style="text-align:right;vertical-align:top"><spring:message code="label.prior.note" />: </td>
				<td colspan="6" style="text-align:left">
				${resultList.pastNotes}
				</td>
			</tr>
			</c:if>
      		<tr id='noteRow_${iter.index}'
				style="display: none;">
				<td colspan="2" style="text-align:right;vertical-align:top;"><spring:message code="note.note"/>:</td>
				<td colspan="6" style="text-align:left" >
					<form:textarea path="resultList[${iter.index}].note"
								   id='note_${iter.index}'
								   onchange='markUpdated(${iter.index});'
					           	   cols="100"
					           	   rows="3" />
				</td>
			</tr>
  	</c:forEach>
	<tr>
	    <td colspan="8"><hr/></td>
    </tr>



  	</c:if>
  	
	<c:if test="${resultCount == 0}">
		<c:if test="${form.displayTestSections}">
			<c:if test="${not empty form.testSectionId}">
			<h2><%= MessageUtil.getContextualMessage("result.noTestsFound") %></h2>
			</c:if>
		</c:if>
		<c:if test="${not form.displayTestSections}">
			<h2><%= MessageUtil.getContextualMessage("result.noTestsFound") %></h2>
		</c:if>
	</c:if>

	
	  	
</Table>
<c:if test="${not (form.paging.totalPages == 0)}">
	1- <c:out value="${pageSize}"/>
	<c:if test="${analysisCount != 0}">
		 of <c:out value="${analysisCount}"/>
	</c:if>
	<c:if test="${empty analysisCount}">
	<c:set var="total" value="${form.paging.totalPages}"/>
	<c:set var="currentPage" value="${form.paging.currentPage}"/>
	<button type="button" style="width:100px;" onclick="pager.pageBack();" <c:if test="${currentPage == 1}">disabled="disabled"</c:if>>
		<spring:message code="label.button.previous"/>
	</button>
	<button type="button" style="width:100px;" onclick="pager.pageFoward();" <c:if test="${currentPage == total}">disabled="disabled"</c:if>>
		<spring:message code="label.button.next"/>
	</button>
	&nbsp;
	<c:out value="${form.paging.currentPage}"/> of
	<c:out value="${form.paging.totalPages}"/>
	</c:if>
</c:if>