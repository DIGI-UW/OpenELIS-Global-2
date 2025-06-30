<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         import="org.openelisglobal.common.action.IActionConstants,
		         org.openelisglobal.common.util.IdValuePair,
		         org.openelisglobal.internationalization.MessageUtil,
		         org.openelisglobal.common.util.Versioning,
		         java.util.List,
		         org.openelisglobal.panel.valueholder.Panel,
		         org.openelisglobal.testconfiguration.action.PanelTests,
		         org.openelisglobal.test.valueholder.Test" %>

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


 
<%--<bean:define id="panelList" name='${form.formName}' property="panelList" type="java.util.List"/> --%>
<c:set var="panelList" value="${form.panelList}" />
<c:set var="selectedPanel" value="${form.selectedPanel}" />

<%
	int testCount = 0;
	int columnCount = 0;
	int columns = 3;
	int columnSize = (int) (100 / columns);
	
    Boolean success = (Boolean)request.getAttribute("success");
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

    function testSelected(input, id, currentPanel, onlyOneTestInPanel, panelId) {
        makeDirty();
        jQuery(".test").each(function () {
            jQuery(this).attr("disabled", "disabled");
            jQuery(this).addClass("disabled-text-button");
        });

        jQuery(".selectedTestName").text(input.value);
        jQuery("#action").text('<spring:message code="label.button.edit"/>');
        jQuery("#fromPanel").text(currentPanel);
        jQuery("#testId").val(id);
        jQuery("#deactivatePanelId").val(onlyOneTestInPanel ? panelId : "");
        jQuery("#warnDeactivtePanel").text(currentPanel);
        jQuery(".edit-step").show();
        jQuery(".select-step").hide();
        jQuery(window).scrollTop(0);

    }

    function panelSelected(panelId){
   	    window.location.href = "PanelTestAssign?panelId=" + panelId ;
    }

    function confirmValues() {
        jQuery("#editButtons").hide();
        jQuery(".confirmation-step").show();
        jQuery("#action").text('<%=MessageUtil.getContextualMessage("label.confirmation")%>');
        if( jQuery("#deactivatePanelId").val().length > 0){
            jQuery("#deatcitvateWarning").show();
        }else{
            jQuery("#deatcitvateWarning").hide();
        }

        jQuery("#panelSelection").attr("disabled", true);

    }

    function rejectConfirmation() {
        jQuery("#editButtons").show();
        jQuery(".confirmation-step").hide();
        jQuery("#action").text('<%=MessageUtil.getContextualMessage("label.button.edit")%>');

        jQuery("#panelSelection").attr("disabled", false);
    }


    function savePage() {
        //window.onbeforeunload = null; // Added to flag that formWarning alert isn't needed.
        jQuery('#list1 option').prop('selected', true);
        var form = document.getElementById("mainForm");
        form.action = "PanelTestAssign";
        form.submit();
    }
    
    jQuery(function(){
        jQuery("#button1").click(function(){
            jQuery("#list1 > option:selected").each(function(){
                jQuery(this).remove().appendTo("#list2");
                jQuery('#list2 option').prop('selected', false);
            });
        });
        
        jQuery("#button2").click(function(){
            jQuery("#list2 > option:selected").each(function(){
                jQuery(this).remove().appendTo("#list1");
                jQuery('#list1 option').prop('selected', false);
            });
        });
    });
    

</script>

<style>
table{
  width: 100%;
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


<div id="successMsg" style="text-align:center; color:seagreen;  width : 100%;font-size:170%; 
  visibility : <%=((success != null && success) ? "visible" : "hidden") %>" >
    <spring:message code="save.success"/>
</div>

     <%    PanelTests selectedPanel = (PanelTests) pageContext.getAttribute("selectedPanel"); %>
     <form:hidden path="panelId" id="panelId" />
     <form:hidden path="deactivatePanelId" id="deactivatePanelId"/>

    <input type="button" value='<%= MessageUtil.getContextualMessage("banner.menu.administration") %>'
           onclick="submitAction('MasterListsPage');"
           class="textButton"/>&rarr;
    <input type="button" value='<%= MessageUtil.getContextualMessage("configuration.test.management") %>'
           onclick="submitAction('TestManagementConfigMenu');"
           class="textButton"/>&rarr;
    <input type="button" value='<%= MessageUtil.getContextualMessage("configuration.panel.manage") %>'
           onclick="submitAction('PanelManagement');"
           class="textButton"/>&rarr;

<%=MessageUtil.getContextualMessage( "configuration.panel.assign" )%>
<br><br>

    <h1 id="action" ><spring:message code="label.form.select"/></h1>
    <h1 id="action" class="edit-step" style="display: none"></h1>
    <h2><spring:message code="configuration.panel.assign"/> </h2>

    <div class="select-step" >
    
    <%    List panelList = (List) pageContext.getAttribute("panelList"); %>
    
    Panel:<select class="required" onchange="panelSelected(this.value);">
            			<option value="">
        				<% for(int i = 0; i < panelList.size(); i++){
            				IdValuePair panel = (IdValuePair)panelList.get(i);	%>
        					<option id='<%="option_" + panel.getId()%>' value="<%=panel.getId()%>"><%=panel.getValue()%></option>
        				<% } %>
    				</select>
<br> 
                
    </div>
    <div class="edit-step" style="display:none">

        Test: <span class="selectedTestName" ></span><br><br>
        &nbsp;&nbsp;<spring:message code="configuration.panel.assign.new.type" />:&nbsp;

    <div class="confirmation-step" style="display:none">
        <br><span class="selectedTestName" ></span>&nbsp;<spring:message code="configuration.testUnit.confirmation.move.phrase" />&nbsp;<span id="fromPanel" ></span> <spring:message code="word.to" /> <span id="toPanel" ></span>.
        <div id="deatcitvateWarning" >
            <br/><span id="warnDeactivtePanel"></span>&nbsp;<spring:message code="configuration.panel.assign.deactivate" />
        </div>
    </div>

    <div style="text-align: center" id="editButtons">
        <input id="saveButton" type="button" value='<%=MessageUtil.getContextualMessage("label.button.next")%>'
               onclick="confirmValues();" disabled="disabled"/>configuration.testUnit.confirmation.move.phrase
        <input type="button" value='<%=MessageUtil.getContextualMessage("label.button.previous")%>'
               onclick='window.onbeforeunload = null; submitAction("PanelTestAssign")'/>
    </div>
    <div style="text-align: center; display: none;" class="confirmation-step">
        <input type="button" value='<%=MessageUtil.getContextualMessage("label.button.accept")%>'
               onclick="savePage();"/>
        <input type="button" value='<%=MessageUtil.getContextualMessage("label.button.reject")%>'
               onclick='rejectConfirmation();'/>
    </div>
</div>

<% if (selectedPanel.getPanelIdValuePair()!= null )  {%>

<table style="width:80%">
<tr>
<td >
<div>

    <h3><%=selectedPanel.getPanelIdValuePair().getValue() %> Tests</h3>
    
    <form:select style="height:200px;line-height:200px;width:100%;" cssClass="required" path="currentTests" id="list1">
        <% for (IdValuePair panelTest : selectedPanel.getTests()) {%>
       		<option id='<%="option_" + panelTest.getId()%>' value="<%=panelTest.getId()%>"><%=panelTest.getValue()%></option>
       <% } %>
    </form:select>

</div>
</td>
<td align="center" valign="middle">
<input id="button2" type="button" value="&lt" /><br>
<input id="button1" type="button" value="&gt" />
</td>
<td>
<div>
     <h3>Available Tests (<%=selectedPanel.getSampleTypeIdValuePair().getValue() %>)</h3>
     
    <form:select style="height:200px;line-height:200px;width:100%;" cssClass="required" path="availableTests" id="list2">
        <% for (IdValuePair availableTest : selectedPanel.getAvailableTests()) {%>
       		<option id='<%="option_" + availableTest.getId()%>' value="<%=availableTest.getId()%>"><%=availableTest.getValue()%></option>
       <% } %>
    </form:select>

</div>
</td>
</tr>
<tr>
<td colspan="3" align="right">

        <input type="button" value='Save' onclick="savePage();" />
</td>
</tr>

</table>
<% } %>
</form:form>

