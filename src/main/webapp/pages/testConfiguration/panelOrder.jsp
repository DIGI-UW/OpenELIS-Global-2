<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         import="org.openelisglobal.common.action.IActionConstants,
		         org.openelisglobal.common.util.IdValuePair,
		         org.openelisglobal.internationalization.MessageUtil,
		         org.openelisglobal.common.util.Versioning,
		         java.util.List,
		         org.openelisglobal.panel.valueholder.Panel,
		         org.openelisglobal.typeofsample.valueholder.TypeOfSample,
		         org.openelisglobal.testconfiguration.action.SampleTypePanel" %>


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

 <%-- 
<bean:define id="panelList" name='${form.formName}' property="panelList" type="java.util.List"/>
<bean:define id="testList" name='${form.formName}' property="existingPanelList" type="java.util.List"/>
<bean:define id="inactiveTestList" name='${form.formName}' property="inactivePanelList" type="java.util.List"/>

<bean:define id="existingPanels" name='${form.formName}' property="existingPanelList" type="java.util.List"/>
<bean:define id="inactivePanels" name='${form.formName}' property="inactivePanelList" type="java.util.List"/>

<bean:define id="existingSampleTypes" name='${form.formName}' property="existingSampleTypeList" type="java.util.List"/>
--%>
 
<c:set var="panelList" value="${form.panelList}" />
<c:set var="testList" value="${form.existingPanelList}" />
<c:set var="inactiveTestList" value="${form.inactivePanelList}" />

<c:set var="existingPanels" value="${form.existingPanelList}" />
<c:set var="inactivePanels" value="${form.inactivePanelList}" />

<c:set var="existingSampleTypes" value="${form.existingSampleTypeList}" />

<%!
	final String NAME_SEPARATOR = "$";
%>

<%
	int testCount = 0;
	int columnCount = 0;
	int columns = 4;
	int sampleTypeCount = 0;
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
        jsonObj.panels = [];

        jQuery("li.sortItem").each(function(){
            jsonBlob = {};
            jsonBlob.id = jQuery(this).val();
            jsonBlob.sortOrder = sortOrder++;
            jsonObj.panels[sortOrder - 1] = jsonBlob;
        });

        jQuery("#jsonChangeList").val(JSON.stringify(jsonObj));
    }
    function savePage() {
        buildJSONList();
        window.onbeforeunload = null; // Added to flag that formWarning alert isn't needed.
        var form = document.getElementById("mainForm");
        form.action = "PanelOrder";
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
    <input type="button" value='<%= MessageUtil.getContextualMessage("configuration.panel.manage") %>'
           onclick="submitAction('PanelManagement');"
           class="textButton"/>&rarr;

<%=MessageUtil.getContextualMessage( "configuration.panel.order" )%>

<%    List panelList = (List) pageContext.getAttribute("panelList"); %>
<%    List existingPanels = (List) pageContext.getAttribute("existingPanels"); %>
<%    List inactivePanels = (List) pageContext.getAttribute("inactivePanels"); %>

<br><br>

<div id="editDiv" >
    <h1 id="action"><spring:message code="label.button.edit"/></h1>

    <div id="editMessage" >
        <h3><spring:message code="configuration.panel.order.explain"/> </h3>
        <spring:message code="configuration.panel.order.explain.limits" /><br/><br/>
    </div>

    <UL class="sortable" style="width:250px">
        <% for(int i = 0; i < panelList.size(); i++){
            IdValuePair panel = (IdValuePair)panelList.get(i);
        %>
        <LI class="ui-state-default_oe sortItem" value='<%=panel.getId() %>' ><span class="ui-icon ui-icon-arrowthick-2-n-s" ></span><%=panel.getValue() %></LI>
        <% } %>

    </UL>

    <div style="text-align: center" id="editButtons">
        <input type="button" value='<%=MessageUtil.getContextualMessage("label.button.next")%>'
               onclick="confirmValues();"/>
        <input type="button" value='<%=MessageUtil.getContextualMessage("label.button.previous")%>'
               onclick='submitAction("PanelManagement")'/>
    </div>
    <div style="text-align: center; display: none;" id="confirmationButtons">
        <input type="button" value='<%=MessageUtil.getContextualMessage("label.button.accept")%>'
               onclick="savePage();"/>
        <input type="button" value='<%=MessageUtil.getContextualMessage("label.button.reject")%>'
               onclick='rejectConfirmation();'/>
    </div>
</div>
<% sampleTypeCount=0; %>
<h3><spring:message code="panel.existing" /></h3>

<% while(sampleTypeCount < existingPanels.size()){%>
<b><%=((SampleTypePanel)existingPanels.get(sampleTypeCount)).getTypeOfSampleName() %></b>

<table width="80%">
    <tr>
        <td width='100%>'>
        <% if( ((SampleTypePanel)existingPanels.get(sampleTypeCount)).getPanels() != null){ 
        
            testCount=0;
        %>
        &nbsp;&nbsp;          
        <% while(testCount < ((SampleTypePanel)existingPanels.get(sampleTypeCount)).getPanels().size()){ %>
            <% if (testCount != 0) { %>
                <%=", " %>
            <% } %>
            <%=((SampleTypePanel)existingPanels.get(sampleTypeCount)).getPanels().get(testCount).getLocalizedName()%>
            <% testCount++; %>
        <% } %>  
        <% } %>
         </td>
    </tr>
</table>
<% sampleTypeCount++; %>
<br>
<% } %>
<h3><spring:message code="panel.existing.inactive" /></h3>

<% if( !inactivePanels.isEmpty()){ %>
<% sampleTypeCount = 0; %>
<% while(sampleTypeCount < inactivePanels.size()){%>
<b><%=((SampleTypePanel)inactivePanels.get(sampleTypeCount)).getTypeOfSampleName() %></b>
 <table width="80%"> 
     <tr> 
        <td width="100%">
        <% if( ((SampleTypePanel)inactivePanels.get(sampleTypeCount)).getPanels() != null){ 
        
            testCount = 0;
        %>    
        &nbsp;&nbsp;
        <% while(testCount < ((SampleTypePanel)inactivePanels.get(sampleTypeCount)).getPanels().size()){%>
            <% if (testCount != 0) { %>
                <%=", " %>
            <% } %>            
            <%= ((SampleTypePanel)inactivePanels.get(sampleTypeCount)).getPanels().get(testCount).getLocalizedName() %>
            <% testCount++; %>
        <% } %>
        <% } %>
        </td>
     </tr> 
 </table> 
<% sampleTypeCount++; %>
<br>
<% } %>
<% } %>
</form:form>


