<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         import="java.util.List,
         		org.openelisglobal.common.action.IActionConstants,
         		org.openelisglobal.common.util.IdValuePair,
         		org.openelisglobal.internationalization.MessageUtil,
         		org.openelisglobal.common.util.Versioning,
         		org.openelisglobal.testconfiguration.controller.TestSectionCreateController" %>

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
<bean:define id="testList" name='${form.formName}' property="existingUomList" type="java.util.List"/>
<bean:define id="englishSectionNames" name='${form.formName}' property="existingEnglishNames" type="String"/>
<bean:define id="frenchSectionNames" name='${form.formName}' property="existingFrenchNames" type="String"/>
 --%>

<c:set var="testList" value="${form.existingUomList}" />
<c:set var="englishSectionNames" value="${form.existingEnglishNames}" />
<c:set var="frenchSectionNames" value="${form.existingFrenchNames}" />

<%
	int testCount = 0;
	int columnCount = 0;
	int columns = 4;
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
        var englishNames = "${form.existingEnglishNames}";
        var frenchNames = "${form.existingFrenchNames}";
        var duplicate = false;
        if( locale == 'english'){
            duplicate = englishNames.indexOf( '<%=TestSectionCreateController.NAME_SEPARATOR%>' + element.value + '<%=TestSectionCreateController.NAME_SEPARATOR%>') != -1;
        }else{
            duplicate = frenchNames.indexOf( '<%=TestSectionCreateController.NAME_SEPARATOR%>' + element.value + '<%=TestSectionCreateController.NAME_SEPARATOR%>') != -1;
        }

        if(duplicate){
            jQuery(element).addClass("error");
            alert("<spring:message code="configuration.uom.create.duplicate" />" );
        }else{
            jQuery(element).removeClass("error");
        }

        makeDirty();
    }

    function savePage() {
        window.onbeforeunload = null; // Added to flag that formWarning alert isn't needed.
        var form = document.getElementById("mainForm");
        form.action = "UomCreate";
        form.submit();
    }
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

    <input type="button" value="<%= MessageUtil.getContextualMessage("banner.menu.administration") %>"
           onclick="submitAction('MasterListsPage');"
           class="textButton"/>&rarr;
    <input type="button" value="<%= MessageUtil.getContextualMessage("configuration.test.management") %>"
           onclick="submitAction('TestManagementConfigMenu');"
           class="textButton"/>&rarr;
    <input type="button" value="<%= MessageUtil.getContextualMessage("configuration.uom.manage") %>"
           onclick="submitAction('UomManagement');"
           class="textButton"/>&rarr;

<%=MessageUtil.getContextualMessage( "configuration.uom.create" )%>
<br><br>

<%  
    List testList = (List) pageContext.getAttribute("testList");
%>

<div id="editDiv" >
    <h1 id="action"><spring:message code="label.button.edit"/></h1>
    <h2><spring:message code="configuration.uom.create"/> </h2>

    <table>
    	<col style="width:40%">
    	<col style="width:40%">
    	<col style="width:40%">
    	<thead>
        <tr><th style="text-align: center"><spring:message code="uom.new"/></th></tr>
<%--         <tr><td style="text-align: center"><spring:message code="label.english"/></td></tr> --%>
        <tr>
            <td><span class="requiredlabel">*</span><form:input path="uomEnglishName" cssClass="required" size="40"
                                                               onchange="handleInput(this, 'english');"/>
            </td>
            <%-- 
            <td><span class="requiredlabel">*</span><html:text property="uomFrenchName" name="${form.formName}" size="40"
                                                               styleClass="required" onchange="handleInput(this, 'french');"/>
            </td>
            --%>
        </tr>
        </thead> 
    </table>
    <div id="confirmationMessage" style="display:none">
        <h4><spring:message code="configuration.uom.confirmation.explain" /></h4>
    </div>
    <div style="text-align: center" id="editButtons">
        <input type="button" value="<%=MessageUtil.getContextualMessage("label.button.next")%>"
               onclick="confirmValues();"/>
        <input type="button" value="<%=MessageUtil.getContextualMessage("label.button.previous")%>"
               onclick="submitAction('UomManagement');"/>
    </div>
    <div style="text-align: center; display: none;" id="confirmationButtons">
        <input type="button" value="<%=MessageUtil.getContextualMessage("label.button.accept")%>"
               onclick="savePage();"/>
        <input type="button" value="<%=MessageUtil.getContextualMessage("label.button.reject")%>"
               onclick='rejectConfirmation();'/>
    </div>
</div>



<h3><spring:message code="uom.existing" /></h3>
<table>
    <% while(testCount < testList.size()){%>
    <tr>
        <td width='<%= 100/columns + "%"%>'><%= ((IdValuePair)testList.get(testCount)).getValue()%>
            <%
                testCount++;
                columnCount = 1;
            %></td>
        <% while(testCount < testList.size() && ( columnCount < columns )){%>
        <td width='<%= 100/columns + "%"%>'><%= ((IdValuePair)testList.get(testCount)).getValue()%>
            <%
                testCount++;
                columnCount++;
            %></td>
        <% } %>

    </tr>
    <% } %>
</table>
</form:form>

