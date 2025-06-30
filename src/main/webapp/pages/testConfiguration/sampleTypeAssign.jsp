<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         import="java.util.List,
         		java.util.LinkedHashMap,
         		java.util.Map,
         		org.openelisglobal.common.action.IActionConstants,
         		org.openelisglobal.common.util.IdValuePair,
         		org.openelisglobal.internationalization.MessageUtil,
         		org.openelisglobal.common.util.Versioning" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>


<%--
  ~ The contents of this file are subject to the Mozilla Public License
  ~ Version 1.1 (the "License"); you may not use this file except in
  ~ compliance with the License. You may obtain a copy of the License at
  ~ http://www.mozilla.org/MPL/
  ~
  ~ Software distributed under the License is distributed on an "AS IS"
  ~ basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
  ~ License for the specific language governing rights and limitations under
  ~ the License.
  ~
  ~ The Original Code is OpenELIS code.
  ~
  ~ Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
  --%>

<script type="text/javascript" src="scripts/ajaxCalls.js?"></script>
<script type="text/javascript" src="scripts/jquery-ui.js?"></script>

<c:set var="sampleTypeList" value="${form.sampleTypeList}" />
<c:set var="sampleTypeTestList" value="${form.sampleTypeTestList}" />

<%
	int testCount = 0;
	int columnCount = 0;
	int columns = 3;
	int columnSize = (int) (100 / columns);
%>

<link rel="stylesheet" media="screen" type="text/css"
      href="css/jquery_ui/jquery.ui.theme.css?"/>

<script type="text/javascript">
    if (!jQuery) {
        var jQuery = jQuery.noConflict();
    }

    jQuery(document).ready( function(){
        jQuery(".sortable").sortable({
            stop: function( ) {makeDirty();}
        });
    });

    function makeDirty(){
        function formWarning(){
            return "<spring:message code="banner.menu.dataLossWarning"/>";
        }
        window.onbeforeunload = formWarning;
    }

    function submitAction(target) {
        var form = document.getElementById("mainForm");
        form.action = target;
        form.submit();
    }

    function testSelected(input, id, currentSampleType, onlyOneTestInSampleType, sampleTypeId) {
        makeDirty();
        jQuery(".test").each(function () {
            jQuery(this).attr("disabled", "disabled");
            jQuery(this).addClass("disabled-text-button");
        });

        jQuery(".selectedTestName").text(input.value);
        jQuery("#action").text('<spring:message code="label.button.edit"/>');
        jQuery("#fromSampleType").text(currentSampleType);
        jQuery("#testId").val(id);
        jQuery("#deactivateSampleTypeId").val(onlyOneTestInSampleType ? sampleTypeId : "");
        jQuery("#warnDeactivteSampleType").text(currentSampleType);
        jQuery(".edit-step").show();
        jQuery(".select-step").hide();
        jQuery(window).scrollTop(0);

    }

    function sampleTypeSelected( selection){
        var optionId = jQuery(selection).val();
        jQuery("#saveButton").attr("disabled", (0 == optionId));
        jQuery("#toSampleType").text(jQuery("#option_" + optionId).text());
        jQuery("#sampleTypeId").val(optionId);
    }

    function confirmValues() {
        jQuery("#editButtons").hide();
        jQuery(".confirmation-step").show();
        jQuery("#action").text('<%=MessageUtil.getContextualMessage("label.confirmation")%>');
        if( jQuery("#deactivateSampleTypeId").val().length > 0){
            jQuery("#deatcitvateWarning").show();
        }else{
            jQuery("#deatcitvateWarning").hide();
        }

        jQuery("#sampleTypeSelection").attr("disabled", true);

    }

    function rejectConfirmation() {
        jQuery("#editButtons").show();
        jQuery(".confirmation-step").hide();
        jQuery("#action").text('<%=MessageUtil.getContextualMessage("label.button.edit")%>');

        jQuery("#sampleTypeSelection").attr("disabled", false);
    }


    function savePage() {
        window.onbeforeunload = null; // Added to flag that formWarning alert isn't needed.
        var form = document.getElementById("mainForm");
        form.action = "SampleTypeTestAssign";
        form.submit();
    }
</script>

<style>
table{
  width: 80%;
}
td {
  width: 25%;
}
</style>

<form:form name="${form.formName}" 
				   action="${form.formAction}" 
				   modelAttribute="form" 
				   onSubmit="return submitForm(this);" 
				   method="${form.formMethod}"
				   id="mainForm">


    <form:hidden path="testId" id="testId"/>
    <form:hidden path="sampleTypeId" id="sampleTypeId"/>
    <form:hidden path="deactivateSampleTypeId" id="deactivateSampleTypeId"/>

    <input type="button" value='<%= MessageUtil.getContextualMessage("banner.menu.administration") %>'
           onclick="submitAction('MasterListsPage');"
           class="textButton"/>&rarr;
    <input type="button" value='<%= MessageUtil.getContextualMessage("configuration.test.management") %>'
           onclick="submitAction('TestManagementConfigMenu');"
           class="textButton"/>&rarr;
    <input type="button" value='<%= MessageUtil.getContextualMessage("configuration.sampleType.manage") %>'
           onclick="submitAction('SampleTypeManagement');"
           class="textButton"/>&rarr;

<%=MessageUtil.getContextualMessage( "configuration.sampleType.assign" )%>

<%    List sampleTypeList = (List) pageContext.getAttribute("sampleTypeList"); %>

<br><br>

    <h1 id="action" ><spring:message code="label.form.select"/></h1>
    <h1 id="action" class="edit-step" style="display: none"></h1>
    <h2><spring:message code="configuration.sampleType.assign"/> </h2>

    <div class="select-step" >
        <spring:message code="configuration.sampleType.assign.explain" />
    </div>
    <div class="edit-step" style="display:none">

        Test: <span class="selectedTestName" ></span><br><br>
        &nbsp;&nbsp;<spring:message code="configuration.sampleType.assign.new.type" />:&nbsp;
   
    <select id="sampleTypeSelection" onchange="sampleTypeSelected(this);">
        <% for(int i = 0; i < sampleTypeList.size(); i++){
            IdValuePair sampleType = (IdValuePair)sampleTypeList.get(i);
        %>
        <option id='<%="option_" + sampleType.getId()%>' value="<%=sampleType.getId()%>"><%=sampleType.getValue()%></option>
        <% } %>
    </select>

    <div class="confirmation-step" style="display:none">
        <br><span class="selectedTestName" ></span>&nbsp;<spring:message code="configuration.sampleType.confirmation.move.phrase" />&nbsp;<span id="fromSampleType" ></span> <spring:message code="word.to" /> <span id="toSampleType" ></span>.
        <div id="deatcitvateWarning" >
            <br/><span id="warnDeactivteSampleType"></span>&nbsp;<spring:message code="configuration.sampleType.assign.deactivate" />
        </div>
    </div>

    <div style="text-align: center" id="editButtons">
        <input id="saveButton" type="button" value='<%=MessageUtil.getContextualMessage("label.button.next")%>'
               onclick="confirmValues();" disabled="disabled"/>
        <input type="button" value='<%=MessageUtil.getContextualMessage("label.button.previous")%>'
               onclick='window.onbeforeunload = null; submitAction("SampleTypeTestAssign")'/>
    </div>
    <div style="text-align: center; display: none;" class="confirmation-step">
        <input type="button" value='<%=MessageUtil.getContextualMessage("label.button.accept")%>'
               onclick="savePage();"/>
        <input type="button" value='<%=MessageUtil.getContextualMessage("label.button.reject")%>'
               onclick='rejectConfirmation();'/>
    </div>
</div>
    
    
    	<%
    	//<bean:define id="testMap" name='${form.formName}' property="sampleTypeTestList" type="java.util.LinkedHashMap<IdValuePair, java.util.List<IdValuePair>>"/>
    	
			LinkedHashMap<IdValuePair, List<IdValuePair>> testMap = new LinkedHashMap<IdValuePair, List<IdValuePair>>();
			testMap = (LinkedHashMap<IdValuePair, List<IdValuePair>>) pageContext.getAttribute("sampleTypeTestList");
      %>

<% for( IdValuePair pair : testMap.keySet()){
 List<IdValuePair> testList = testMap.get(pair);
 %>
    <div>
        <h4><%=pair.getValue()%></h4>
        <% testCount = 0;%>
        <table width="95%" style="position:relative;left:5%">
            <% while (testCount < testList.size()) {%>
            <tr>
                <%
                    columnCount = 0;
                %>
                <% while (testCount < testList.size() && (columnCount < columns)) {%>
                <td width='<%=columnSize + "%"%>'>
                    <input type="button"
                           class="textButton test"
                           value='<%=testList.get(testCount).getValue()%>'
                           onclick="testSelected(this, '<%=testList.get(testCount).getId() %>', '<%=pair.getValue()%>', <%= testList.size() == 1 %>, '<%=pair.getId()%>')"
                           checked>
                    <%
                        testCount++;
                        columnCount++;
                    %></td>
                <% } %>
                <% while (columnCount < columns) {
                    columnCount++; %>
                <td width='<%=columnSize + "%"%>'></td>
                <% } %>
            </tr>
            <% } %>
        </table>

    </div>
<%}%>
</form:form>

