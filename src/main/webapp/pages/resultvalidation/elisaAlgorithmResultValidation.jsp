<%@ page language="java"
	contentType="text/html; charset=UTF-8"
	import="java.util.List,
			org.openelisglobal.common.action.IActionConstants,
			org.openelisglobal.sample.util.AccessionNumberUtil,
			org.openelisglobal.resultvalidation.bean.AnalysisItem,
			org.openelisglobal.common.util.IdValuePair,
			org.openelisglobal.common.util.Versioning,
			org.openelisglobal.internationalization.MessageUtil,	
			org.owasp.encoder.Encode" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>


<%@ taglib prefix="fn" uri="jakarta.tags.functions"%>

	
<c:set var="testSection"	value='${form.testSection}' />
<c:set var="results" value="${form.resultList}" />
<c:set var="resultCount" value="${fn:length(results)}" />
<c:set var="pageSearchList" value='${form.paging.searchTermToPage}'/>	


<%
	boolean dirty = false;
	int rowColorIndex = 1;
	String searchTerm = request.getParameter("searchTerm");
%>
	
<script type="text/javascript" src="scripts/OEPaging.js?"></script>

<script type="text/javascript">

<c:if test="${form.formName == 'ResultValidationForm'}">
var pager = new OEPager('${form.formName}', '<spring:escapeBody javaScriptEscape="true">${(testSection == "") ? "" : "&type=" += testSection}</spring:escapeBody>');
pager.setCurrentPageNumber('<c:out value="${form.paging.currentPage}"/>');

var pageSearch; //assigned in post load function

var pagingSearch = new Object();
<c:forEach items="${pageSearchList}" var="page" >
	pagingSearch['${page.id}'] = '${paging.value}';
</c:forEach>

jQuery(document).ready( function() {
			var searchTerm = '<%=Encode.forJavaScript(searchTerm)%>';
			pageSearch = new OEPageSearch( document.getElementById("searchNotFound"), "td", pager );
			
			if( searchTerm != "null" ){
				 pageSearch.highlightSearch( searchTerm, false );
			}
			});
</c:if>

function  /*void*/ setMyCancelAction(form, action, validate, parameters)
{
	//first turn off any further validation
	setAction(document.getElementById("mainForm"), 'Cancel', 'no', '');
}

function /*void*/ enableDisableCheckboxes( matchedElement ){
	$(matchedElement).checked = false;
}

function /*void*/ setSaveButton( matchedElement ){
	$(matchedElement).checked = false;
}

function /*void*/ markUpdated(){
	$("saveButtonId").disabled = false;
}

function /*void*/ makeDirty(){
	$("saveButtonId").disabled = false;
  dirty = true;
  
	// do other work
  if( typeof(showSuccessMessage) === 'function' ){
    showSuccessMessage(false); //refers to last save
  }
		
	// Adds warning when leaving page if content has been entered into makeDirty form fields
	function formWarning(){ 
		return "<spring:message code="banner.menu.dataLossWarning"/>";
	}
	window.onbeforeunload = formWarning;
}

function printWorkplan() {

	var form = document.getElementById("mainForm");
	form.action = "PrintWorkplanReport";
	form.target = "_blank";
	form.submit();
}

function savePage() {

  window.onbeforeunload = null; // Added to flag that formWarning alert isn't needed.
	var form = document.getElementById("mainForm");
	form.action = "ResultValidation";
	form.submit();
}

function /*void*/ showHideNotes( index ){
	var current = $("hideShow_" + index ).value;

	if( current == "hidden" ){
		$("showHideButton_" + index).value = '<spring:message code="label.button.hide"/>';
		$("hideShow_" + index ).value = "showing";
		$("noteRow_" + index ).show();
	}else{
		$("showHideButton_" + index).value = $("note_" + index ).value.blank() ? '<spring:message code="label.button.add"/>' : '<spring:message code="label.button.edit"/>' ;
		$("hideShow_" + index ).value = "hidden";
		$("noteRow_" + index ).hide();
	}
}

function toggleSelectAll( element ) {

	if (element.id == "selectAllAccept" ) {
		var checkboxes = $$(".accepted");
		var matchedCheckboxes = $$(".rejected");
	} else if (element.id == "selectAllReject" ) {
		var checkboxes = $$(".rejected");
		var matchedCheckboxes = $$(".accepted");
	}

	if (element.checked == true ) {
		for (var index = 0; index < checkboxes.length; ++index) {
			  var item = checkboxes[index];
			  item.checked = true;
		}
		for (var index = 0; index < matchedCheckboxes.length; ++index) {
			  var item = matchedCheckboxes[index];
			  item.checked = false;
		}
	} else if (element.checked == false ) {
		for (var index = 0; index < checkboxes.length; ++index) {
			  var item = checkboxes[index];
			  item.checked = false;
		}
	}

}

/*It is not clear why this is needed but what it prevents is the browser trying to do a submit on 'enter'  Other pages handle this
differently.  Overrides formTemplate.jsp handleEnterEvent
*/
function /*boolean*/ handleEnterEvent(){
	return false;
}
</script>
<c:if test="${form.formName == 'ResultValidationForm'}">
<c:if test="${resultCount != 0}">
<div  style="width:80%" >
	<form:hidden path="paging.currentPage" id="currentPageID" />
	<c:set var="total" value="${form.paging.totalPages}"/>
	<c:set var="currentPage" value="${form.paging.currentPage}"/>

	<input type="button" value='<%=MessageUtil.getMessage("label.button.previous") %>' style="width:100px;" onclick="pager.pageBack();" 
		<c:if test="${currentPage == '1'}">disabled="disabled"</c:if> />
	<input type="button" value='<%=MessageUtil.getMessage("label.button.next") %>' style="width:100px;" onclick="pager.pageFoward();" 
		<c:if test="${total == currentPage}">disabled="disabled"</c:if> />

	&nbsp;
	<c:out value="${form.paging.currentPage}"/> <spring:message code="report.pageNumberOf" />
	<c:out value="${form.paging.totalPages}"/>
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
</c:if>

<form:hidden path="testSection" />
<Table width="80%" >
	<c:if test="${resultCount != 0}">
	<c:if test="${form.formName == 'WorkplanForm'}">
	<tr>
		<td>
      		<button type="button" property="print" id="print"  onclick="printWorkplan();"  >
				<spring:message code="workplan.print"/>
			</button>
		</td>
	</tr>
	<tr>
		<td colspan="11" >
			<img src="./images/nonconforming.gif" /> = <spring:message code="result.nonconforming.item"/>		
		</td>
	</tr>
	<tr>
	    <td colspan="11"><hr/></td>
    </tr>
	</c:if><c:if test="${form.formName == 'ResultValidationForm'}">
	<tr>
		<td colspan="11" >
			<img src="./images/nonconforming.gif" /> = <spring:message code="result.nonconforming.item"/>		
		</td>
	</tr>
	<tr>
	    <td colspan="14"><hr/></td>
    </tr>
    </c:if>
	<tr>
    	<th width="7%">
	  		<spring:message code="quick.entry.accession.number.CI"/>
		</th>
		<th width="5%">
	  		Murex Combinaison
		</th>
		<th width="5%">
	  		Genscreen
		</th>
		<th width="5%">
	  		Vironostika
		</th>
		<th width="5%">
			Innolia
		</th>
		<th width="5%">
			Bioline
		</th>
		<th width="5%">
	  		Genie II
		</th>
		<th width="5%">
	  		Genie II 1/100
		</th>
		<th width="5%">
	  		Genie II 1/10
		</th>
		<th width="5%">
	  		Western Blot 1
		</th>
		<th width="5%">
	  		Western Blot 2
		</th>
		<th width="5%">
	  		p24 Ag
		</th>
		<c:if test="${form.formName == 'WorkplanForm'}">
			<th width="10%">
		  		<spring:message code="result.validation.next.test"/>
			</th>
		</c:if><c:if test="${form.formName == 'ResultValidationForm'}">
			<th width="8%">
				<spring:message code="result.validation.final.result"/>
			</th>
			<th width="3%" align="center">
				Accept
				<br/>
				<input type="checkbox"
					name="selectAllAccept"
					value="on"
					onclick="toggleSelectAll(this);"
					onchange="markUpdated(); makeDirty();"
					id="selectAllAccept"
					class="accepted">
			</th>
			<th width="3%" align="center">
				Reject
				<br/>
				<input type="checkbox"
						name="selectAllReject"
						value="on"
						onclick="toggleSelectAll(this);"
						onchange="markUpdated(); makeDirty();"
						id="selectAllReject"
						class="rejected">
			</th>
		<%-- <th width="5%">
	  		Notes
		</th>--%>
		</c:if>
  	</tr>

	<c:forEach items="${form.resultList}" var="resultList" varStatus="iter">
			<form:hidden path="resultList[${iter.index}].sampleGroupingNumber" />
			<form:hidden path="resultList[${iter.index}].accessionNumber" />
			<form:hidden path="resultList[${iter.index}].analysisId" />
			<form:hidden path="resultList" />
			<c:set var="accessionNumber" value="${resultList.accessionNumber}"/>
     		<tr id='row_${iter.index}' class='${(rowColorIndex % 2 == 0) ? "evenRow" : "oddRow" }' >
     		<c:set var="rowColorIndex" value="${rowColorIndex + 1}"/>
	    		<td class='${accessionNumber}'>
	      			<c:out value="${resultList.accessionNumber}"/>
					<c:if test="${resultList.nonconforming}">
						<img src="./images/nonconforming.gif" />
					</c:if>
	    		</td>
				<td>
					<c:out value="${resultList.murexResult}"/>
				</td>
				<td>
					<c:out value="${resultList.genscreenResult}"/>
				</td>
				<td>
					<c:out value="${resultList.vironostikaResult}"/>
				</td>
				<td>
					<c:out value="${resultList.innoliaResult}"/>
				</td>
				<td>
					<c:out value="${resultList.biolineResult}"/>
				</td>
				<td>
					<c:out value="${resultList.genieIIResult}"/>
				</td>
				<td>
					<c:out value="${resultList.genieII100Result}"/>
				</td>
				<td>
					<c:out value="${resultList.genieII10Result}"/>
				</td>
				<td>
					<c:out value="${resultList.westernBlot1Result}"/>
				</td>
				<td>
					<c:out value="${resultList.westernBlot2Result}"/>
				</td>
				<td>
					<c:out value="${resultList.p24AgResult}"/>
				</td>
				<c:if test="${form.formName == 'WorkplanForm'}">				
				<td>
					<c:out value="${resultList.nextTest}"/>
				</td>
				</c:if><c:if test="${form.formName == 'ResultValidationForm'}">
				<td>
					<c:out value="${resultList.finalResult}"/>
				</td>
     			<td align="center">
				<form:checkbox path="resultList[${iter.index}].isAccepted" 
							   id='accepted_${iter.index}'
							   onchange="makeDirty();"
							   cssClass="accepted"
							   onclick='enableDisableCheckboxes("rejected_${iter.index}");' />
				</td>
				<td align="center">

					<form:checkbox path='resultList[${iter.index}].isRejected'
					               id='rejected_${iter.index}'
								   onchange="makeDirty();"
								   cssClass="rejected"
								   onclick='enableDisableCheckboxes("accepted_${iter.index}");' />
				</td>
				</c:if>
				<%-- <td align="center">
					<html:button property="notes"
								 style="background-color: transparent"
								 onclick='<%= "showHideNotes( ${iter.index});" %>'
								 id='<%="showHideButton_${iter.index}'
								 tabindex='-1'
								 cssClass="textButton">
								 <logic:empty name="resultList" property="note">
								 	<spring:message code="label.button.add"/>
								 </logic:empty>
								 <logic:notEmpty name="resultList" property="note">
								 	<spring:message code="label.button.edit"/>
								 </logic:notEmpty>
								 </html:button>
					<form:hidden path="hideShowFlag"  id='<%="hideShow_${iter.index}' value="hidden" />
				</td>
      		</tr>
      		 <tr id='<%="noteRow_${iter.index}' style="display: none;">
				<td colspan="2" valign="top" align="right"><spring:message code="note.note"/>:</td>
				<td colspan="6" align="left" >
					<html:textarea id='<%="note_${iter.index}'
								   onchange='<%="markUpdated(${iter.index});"%>'
							   	   name="resultList"
					           	   property="note"
					           	   indexed="true"
					           	   cols="100"
					           	   rows="3" />
				</td>--%>
			</tr>

  	</c:forEach>
	<tr>
	    <td colspan="13"><hr/></td>
    </tr>
    <c:if test="${form.formName == 'WorkplanForm'}">
    <tr>
		<td>
      		<button type="button" property="print" id="print"  onclick="printWorkplan();"  >
				<spring:message code="workplan.print"/>
			</button>
		</td>
	</tr>
	</c:if>
  	</c:if>
  	<c:if test="${resultCount == 0}">
		<h2><%= MessageUtil.getContextualMessage("result.noTestsFound") %></h2>
	</c:if>
</Table>
<c:if test="${form.formName == 'ResultValidationForm'}">
<c:if test="${form.paging.totalPages != 0}">
	<c:set var="total" value="${form.paging.totalPages}"/>
	<c:set var="currentPage" value="${form.paging.currentPage}"/>

	<input type="button" value='<%=MessageUtil.getMessage("label.button.previous") %>' style="width:100px;" onclick="pager.pageBack();" <c:if test="${currentPage == '1'}">disabled="disabled"</c:if> />
	<input type="button" value='<%=MessageUtil.getMessage("label.button.next") %>' style="width:100px;" onclick="pager.pageFoward();" <c:if test="${total == currentPage}">disabled="disabled"</c:if> />

	&nbsp;
	<c:out value="${form.paging.currentPage}"/> of
	<c:out value="${form.paging.totalPages}"/>
</c:if>
</c:if>
