<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         import="org.openelisglobal.common.action.IActionConstants,
         		java.util.List,
         		org.openelisglobal.panel.valueholder.Panel,
         		org.openelisglobal.common.util.IdValuePair,
         		org.openelisglobal.internationalization.MessageUtil,
         		org.openelisglobal.common.util.Versioning,
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

<%-- 
<bean:define id="testList" name='${form.formName}' property="existingPanelList" type="java.util.List"/>
<bean:define id="inactiveTestList" name='${form.formName}' property="inactivePanelList" type="java.util.List"/>

<bean:define id="existingPanels" name='${form.formName}' property="existingPanelList" type="java.util.List"/>
<bean:define id="inactivePanels" name='${form.formName}' property="inactivePanelList" type="java.util.List"/>

<bean:define id="existingSampleTypes" name='${form.formName}' property="existingSampleTypeList" type="java.util.List"/>
<bean:define id="englishSectionNames" name='${form.formName}' property="existingEnglishNames" type="String"/>
<bean:define id="frenchSectionNames" name='${form.formName}' property="existingFrenchNames" type="String"/>
--%>
 
<c:set var="testList" value="${form.existingPanelList}" />
<c:set var="inactiveTestList" value="${form.inactivePanelList}" />

<c:set var="existingPanels" value="${form.existingPanelList}" />
<c:set var="inactivePanels" value="${form.inactivePanelList}" />

<c:set var="existingSampleTypeList" value="${form.existingSampleTypeList}" />
<c:set var="englishSectionNames" value="${form.existingEnglishNames}" />
<c:set var="frenchSectionNames" value="${form.existingFrenchNames}" />

<%!
	final String NAME_SEPARATOR = "$";
%>

<%
	int testCount = 0;
	int columnCount = 0;
	int columns = 4;
	int sampleTypeCount = 0;
%>

<script type="text/javascript">
    if (!jQuery) {
        var jQuery = jQuery.noConflict();
    }

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

    function rejectConfirmation() {
        jQuery(".required").each(function () {
            var element = jQuery(this);
            element.removeProp("readonly");
            element.removeClass("confirmation");
        });
        jQuery(".requiredlabel").each(function () {
            jQuery(this).show();
        });

        jQuery("#editButtons").show();
        jQuery("#confirmationButtons").hide();
        jQuery("#confirmationMessage").hide();
        jQuery("#action").text("<%=MessageUtil.getContextualMessage("label.button.edit")%>");
    }

    function handleInput(element, locale) {
        var englishNames = "${form.existingEnglishNames}".toLowerCase();
        var frenchNames = "${form.existingFrenchNames}".toLowerCase();
        var duplicate = false;
        if( locale == 'english'){
            duplicate = englishNames.indexOf( '<%=NAME_SEPARATOR%>' + element.value.toLowerCase() + '<%=NAME_SEPARATOR%>') != -1;
        }else{
            duplicate = frenchNames.indexOf( '<%=NAME_SEPARATOR%>' + element.value.toLowerCase() + '<%=NAME_SEPARATOR%>') != -1;
        }

        if(duplicate){
            jQuery(element).addClass("error");
            alert("<spring:message code="configuration.panel.create.duplicate" />" );
        }else{
            jQuery(element).removeClass("error");
        }

        makeDirty();
    }

    function savePage() {
        window.onbeforeunload = null; // Added to flag that formWarning alert isn't needed.
        var form = document.getElementById("mainForm");
        form.action = "PanelCreate";
        form.submit();
    }
    
    function confirmValues() {
        var hasError = false;
        jQuery(".required").each(function () {
            var input = jQuery(this);
            if (!input.val() || input.val().strip().length == 0) {
            	input.addClass("error");
                hasError = true;
            }
        });

        if (hasError) {
            alert("<%=MessageUtil.getContextualMessage("error.all.required")%>");
        } else {
            jQuery(".required").each(function () {
                var element = jQuery(this);
                element.prop("readonly", true);
                element.addClass("confirmation");
            });
            jQuery(".requiredlabel").each(function () {
                jQuery(this).hide();
            });
            jQuery("#editButtons").hide();
            jQuery("#confirmationButtons").show();
            jQuery("#confirmationMessage").show();
            jQuery("#action").text("<%=MessageUtil.getContextualMessage("label.confirmation")%>");
        }
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


    <input type="button" value="<%= MessageUtil.getContextualMessage("banner.menu.administration") %>"
           onclick="submitAction('MasterListsPage');"
           class="textButton"/>&rarr;
    <input type="button" value="<%= MessageUtil.getContextualMessage("configuration.test.management") %>"
           onclick="submitAction('TestManagementConfigMenu');"
           class="textButton"/>&rarr;
    <input type="button" value="<%= MessageUtil.getContextualMessage("configuration.panel.manage") %>"
           onclick="submitAction('PanelManagement');"
           class="textButton"/>&rarr;

<%=MessageUtil.getContextualMessage( "configuration.panel.create" )%>

<%    List existingSampleTypeList = (List) pageContext.getAttribute("existingSampleTypeList"); %>
<%    List existingPanels = (List) pageContext.getAttribute("existingPanels"); %>
<%    List inactivePanels = (List) pageContext.getAttribute("inactivePanels"); %>

<br><br>

<div id="editDiv" >
    <h1 id="action"><spring:message code="label.button.edit"/></h1>
    <h2><spring:message code="configuration.panel.create"/> </h2>

    <table>
        <tr>
            <th colspan="3" style="text-align: center"><spring:message code="panel.new"/></th>
        </tr>
        <tr>
            <td style="text-align: center"><spring:message code="label.english"/></td>
            <td style="text-align: center"><spring:message code="label.french"/></td>
            <td style="text-align: center"><spring:message code="label.sampleType"/></td>
        </tr>
        <tr>
        	<td><span class="requiredlabel">*</span><form:input path="panelEnglishName" cssClass="required" size="40"
                                                               onchange="handleInput(this, 'english');"/>
            </td>
            <td><span class="requiredlabel">*</span><form:input path="panelFrenchName" cssClass="required" size="40"
                                                               onchange="handleInput(this, 'french');"/>
            </td>
            <td>
            <span class="requiredlabel">*</span>
            		<form:select cssClass="required" path="sampleTypeId">
            			<option value="">
        				<% for(int i = 0; i < existingSampleTypeList.size(); i++){
            				IdValuePair sampleType = (IdValuePair)existingSampleTypeList.get(i);
        				%>
        					<option id='<%="option_" + sampleType.getId()%>' value="<%=sampleType.getId()%>"><%=sampleType.getValue()%></option>
        				<% } %>
    				</form:select>
            </td>
        </tr>
    </table>
    
    <div id="confirmationMessage" style="display:none">
        <h4><spring:message code="configuration.panel.confirmation.explain" /></h4>
    </div>
    <div style="text-align: center" id="editButtons">
        <input type="button" value="<%=MessageUtil.getContextualMessage("label.button.next")%>"
               onclick="confirmValues();"/>
        <input type="button" value="<%=MessageUtil.getContextualMessage("label.button.previous")%>"
               onclick="submitAction('PanelManagement');"/>
    </div>
    <div style="text-align: center; display: none;" id="confirmationButtons">
        <input type="button" value="<%=MessageUtil.getContextualMessage("label.button.accept")%>"
               onclick="savePage();"/>
        <input type="button" value="<%=MessageUtil.getContextualMessage("label.button.reject")%>"
               onclick='rejectConfirmation();'/>
    </div>
</div>
<% sampleTypeCount=0; %>
<h3><spring:message code="panel.existing" /></h3>

<% while(sampleTypeCount < existingPanels.size()){%>
<b><%=((SampleTypePanel)existingPanels.get(sampleTypeCount)).getTypeOfSampleName() %></b>

<table>
    <tr>
        <td width='100%>'>
        <% if( ((SampleTypePanel)existingPanels.get(sampleTypeCount)).getPanels() != null){ 
        
            testCount=0;
        %>
          
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
 <table> 
     <tr> 
        <td>
        <% if( ((SampleTypePanel)inactivePanels.get(sampleTypeCount)).getPanels() != null){ 
        
        	testCount = 0;
        %>    
        
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

