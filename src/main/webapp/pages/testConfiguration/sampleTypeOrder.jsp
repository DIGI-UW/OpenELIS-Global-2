<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         import="java.util.List,
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

<%
	int testCount = 0;
	int columnCount = 0;
	int columns = 4;
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


    function confirmValues() {
        jQuery("#editButtons").hide();
        jQuery("#confirmationButtons").show();
        jQuery("#editMessage").hide();
        jQuery("#action").text('<%=MessageUtil.getContextualMessage("label.confirmation")%>');

        jQuery(".sortable").sortable("disable");
    }

    function rejectConfirmation() {
        jQuery("#editButtons").show();
        jQuery("#confirmationButtons").hide();
        jQuery("#editMessage").show();
        jQuery("#action").text('<%=MessageUtil.getContextualMessage("label.button.edit")%>');

        jQuery(".sortable").sortable("enable");
    }

    function buildJSONList(){
        var sortOrder = 0;
        var jsonObj = {};
        jsonObj.sampleTypes = [];

        jQuery("li.sortItem").each(function(){
            jsonBlob = {};
            jsonBlob.id = jQuery(this).val();
            jsonBlob.sortOrder = sortOrder++;
            jsonObj.sampleTypes[sortOrder - 1] = jsonBlob;
        });

        jQuery("#jsonChangeList").val(JSON.stringify(jsonObj));
    }
    function savePage() {
        buildJSONList();
        window.onbeforeunload = null; // Added to flag that formWarning alert isn't needed.
        var form = document.getElementById("mainForm");
        form.action = "SampleTypeOrder";
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

    <form:hidden path="jsonChangeList" id="jsonChangeList"/>

    <input type="button" value='<%= MessageUtil.getContextualMessage("banner.menu.administration") %>'
           onclick="submitAction('MasterListsPage');"
           class="textButton"/>&rarr;
    <input type="button" value='<%= MessageUtil.getContextualMessage("configuration.test.management") %>'
           onclick="submitAction('TestManagementConfigMenu');"
           class="textButton"/>&rarr;
    <input type="button" value='<%= MessageUtil.getContextualMessage("configuration.sampleType.manage") %>'
           onclick="submitAction('SampleTypeManagement');"
           class="textButton"/>&rarr;

<%=MessageUtil.getContextualMessage( "configuration.sampleType.order" )%>

<%    List sampleTypeList = (List) pageContext.getAttribute("sampleTypeList"); %>
<br><br>

<div id="editDiv" >
    <h1 id="action"><spring:message code="label.button.edit"/></h1>

    <div id="editMessage" >
        <h3><spring:message code="configuration.sampleType.order.explain"/> </h3>
        <spring:message code="configuration.sampleType.order.explain.limits" /><br/><br/>
    </div>

    <UL class="sortable" style="width:250px">
        <% for(int i = 0; i < sampleTypeList.size(); i++){
            IdValuePair sampleType = (IdValuePair)sampleTypeList.get(i);
        %>
        <LI class="ui-state-default_oe sortItem" value='<%=sampleType.getId() %>' ><span class="ui-icon ui-icon-arrowthick-2-n-s" ></span><%=sampleType.getValue() %></LI>
        <% } %>

    </UL>

    <div style="text-align: center" id="editButtons">
        <input type="button" value='<%=MessageUtil.getContextualMessage("label.button.next")%>'
               onclick="confirmValues();"/>
        <input type="button" value='<%=MessageUtil.getContextualMessage("label.button.previous")%>'
               onclick='submitAction("SampleTypeManagement")'/>
    </div>
    <div style="text-align: center; display: none;" id="confirmationButtons">
        <input type="button" value='<%=MessageUtil.getContextualMessage("label.button.accept")%>'
               onclick="savePage();"/>
        <input type="button" value='<%=MessageUtil.getContextualMessage("label.button.reject")%>'
               onclick='rejectConfirmation();'/>
    </div>
</div>
</form:form>


