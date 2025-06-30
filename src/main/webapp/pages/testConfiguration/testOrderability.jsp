<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         import="java.util.List,
         		org.openelisglobal.common.action.IActionConstants,
         		org.openelisglobal.common.util.IdValuePair,
         		org.openelisglobal.internationalization.MessageUtil,
         		org.openelisglobal.test.beanItems.TestActivationBean,
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

<c:set var="jsonChangeList" value="${form.jsonChangeList}" />
<c:set var="activeTestList" value="${form.orderableTestList}" />
 
<script type="text/javascript" src="scripts/jquery-ui.js?"></script>


<link rel="stylesheet" media="screen" type="text/css"
      href="css/jquery_ui/jquery.ui.theme.css?"/>

<script type="text/javascript">
    var backFunction = selectBack;

    if (!jQuery) {
        var jQuery = jQuery.noConflict();
    }

    jQuery(document).ready( function() {
        configureForSelect();
    });

    function makeDirty() {
        function formWarning() {
            return "<spring:message code="banner.menu.dataLossWarning"/>";
        }

        window.onbeforeunload = formWarning;
    }

    function makeClean() {
        window.onbeforeunload = null;
    }

    function submitAction(target) {
        var form = document.getElementById("mainForm");
        form.action = target;
        form.submit();
    }
    function checkedChanged(isActive, element) {
        var jqueriedElement = jQuery(element);
        var activate, deactivate;

        if ((element.checked == isActive)) {
            jqueriedElement.parent().find("span").removeClass("altered");
            jqueriedElement.removeClass("activationChanged");
        } else {
            jqueriedElement.parent().find("span").addClass("altered");
            jqueriedElement.addClass("activationChanged");
        }

        deactivate = jQuery(".active.activationChanged").length;
        activate = jQuery(".inactive.activationChanged").length;

        jQuery("#nextButtonSelect").attr('disabled', ( deactivate == 0 && activate == 0));

        if(  deactivate == 0 && activate == 0){
            makeClean();
        }else{
            makeDirty();
        }
    }


    function nextStepToConfirmation(){
        var deactivate = jQuery(".active.activationChanged").length;
        var activate = jQuery(".inactive.activationChanged").length;
        configureForConfirmation();
        confirmationStep(activate, deactivate);
        jQuery("#activateSection input").prop("disabled", true);

        window.scrollTo(0,0);
    }

    function confirmationStep(activateNumber, deactivateNumber) {
        var jsonObj = {};

        jsonObj.activateTest = [];
        jsonObj.deactivateTest = [];

        if (activateNumber > 0) {
            listActivatedItems(jsonObj);
        }

        if (deactivateNumber > 0) {
            listDeactivatedItems(jsonObj);
        }

        jQuery("#jsonChangeList").val(JSON.stringify(jsonObj));
    }

    function listActivatedItems( jsonObj){

        var activateTestList = jQuery("#testActivateList");
        var jsonBlob, sampleType, activatedTests;

        jQuery("#testActivate").show();
        jQuery(".activeSampleType").each(function(){
            sampleType=jQuery(this);
            activatedTests= sampleType.parent().find(".inactive.activationChanged");
            if(activatedTests.length > 0) {
                activateTestList.append("<br />");
                activateTestList.append(sampleType.text());
                activateTestList.append("<br />");
                activatedTests.each(function () {
                    jsonBlob = {};
                    jsonBlob.id = jQuery(this).val();
                    jsonObj.activateTest[jsonObj.activateTest.length] = jsonBlob;
                    activateTestList.append("&nbsp;&nbsp;&nbsp;&nbsp;" + jQuery(this).siblings("span").text());
                    activateTestList.append("<br />");

                });
            }
        });

    }

    function listDeactivatedItems(jsonObj) {
        var sampleType, deactivatedTests, jsonBlob;
        var deactivateTestList = jQuery("#testDeactivateList");


        jQuery("#testDeactivate").show();
        jQuery(".activeSampleType").each(function () {
            sampleType = jQuery(this);
            deactivatedTests = sampleType.parent().find(".active.activationChanged");

            if (deactivatedTests.length > 0) {
                deactivateTestList.append("<br />");
                deactivateTestList.append(sampleType.text());

                deactivateTestList.append("<br />");
                deactivatedTests.each(function () {
                    jsonBlob = {};
                    jsonBlob.id = jQuery(this).val();
                    jsonObj.deactivateTest[jsonObj.deactivateTest.length] = jsonBlob;
                    deactivateTestList.append("&nbsp;&nbsp;&nbsp;&nbsp;" + jQuery(this).siblings("span").text());
                    deactivateTestList.append("<br />");
                });
            }

        });
    }


    function navigateBack() {
        backFunction();
        window.scrollTo(0,0);
    }

    function selectBack(){
        submitAction('TestManagementConfigMenu');
    }

    function confirmBack(){
           configureForSelect();
    }
    function configureForSelect() {
        jQuery("#step").text("<%=MessageUtil.getContextualMessage("configuration.test.orderable")%>");
        jQuery("#instructions").text("<spring:message code="instructions.test.order"/>");
        jQuery("#activateSection input").prop("disabled", false);
        jQuery(".selectHide").hide();
        jQuery(".selectShow").show();
        jQuery(".selectClear").empty();
        backFunction = selectBack;
    }


    function configureForConfirmation() {
        jQuery("#step").text("<%=MessageUtil.getContextualMessage("label.test.order.confirm")%>");
        jQuery("#instructions").text("<%=MessageUtil.getContextualMessage("instructions.test.activation.confirm")%>");
        jQuery(".confirmHide").hide();
        jQuery(".confirmShow").show();

        backFunction = confirmBack;
    }
    function savePage() {
        window.onbeforeunload = null; // Added to flag that formWarning alert isn't needed.

        var form = document.getElementById("mainForm");
        form.method = "POST";
        form.action = "TestOrderability";
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
<br>
<input type="button" value="<%= MessageUtil.getContextualMessage("banner.menu.administration") %>"
       onclick="submitAction('MasterListsPage');"
       class="textButton"/> &rarr;
<input type="button" value="<%= MessageUtil.getContextualMessage("configuration.test.management") %>"
       onclick="submitAction('TestManagementConfigMenu');"
       class="textButton"/>&rarr;
<span id="testActivationSelectionButton" class="selectHide confirmShow" style="display:none" >
<input type="button" value="<%=MessageUtil.getContextualMessage("configuration.test.orderable")%>"
       onclick="configureForSelect();"
       class="textButton"/>&rarr;
</span>

<span id="testActivationConfirmation" class="selectHide confirmShow" style="display:none" ><%=MessageUtil.getContextualMessage("label.confirmation")%></span>
<span id="testActivationSelection" class="selectShow confirmHide"><spring:message code="configuration.test.orderable"/></span>
<br><br>

<h1 id="step"></h1><br/>

<div class="indent"><span id="instructions"></span></div>
<br>

<div id="testActivate" class="selectHide" >
    <h4><%=MessageUtil.getContextualMessage("label.test.orderable")%></h4>

    <div id="testActivateList" class="selectClear"></div>
    <br>
</div>
<div id="testDeactivate" class="selectHide">
    <h4><%=MessageUtil.getContextualMessage("label.test.unorderable")%></h4>

    <div id="testDeactivateList" class="selectClear"></div>
    <br>
</div>

<div class="selectHide confirmShow" style="margin-left:auto; margin-right:auto;width: 40%;">
    <input type="button"
           class="confirmShow "
           value="<%=MessageUtil.getContextualMessage("label.button.accept")%>"
           onclick="savePage();"
           id="acceptButton"
           style="display: none"/>

    <input type="button" value="<%=MessageUtil.getContextualMessage("label.button.back")%>" onclick="navigateBack()" />
</div>

<hr/>

<%
	int testCount = 0;
	int columnCount = 0;
	int columns = 4;
%>

<form:hidden path="jsonChangeList" id="jsonChangeList"/>

<div id="activateSection" class="indent">
    <c:forEach var="activeBean" varStatus="iter" items="${form.orderableTestList}">
        <div>
            <span class="activeSampleType"><c:out value="${activeBean.sampleType.value}"/>
            </span>
            <div class="indent">
                <% 
                	TestActivationBean bean = (TestActivationBean) pageContext.getAttribute("activeBean");
                	List<IdValuePair> testList = bean.getActiveTests();
                	testCount = 0; 
                %>
                <% testCount = 0;%>
                <table>
                    <% while (testCount < testList.size()) {%>
                    <tr>
                        <%
                            columnCount = 0;
                        %>
                        <% while (testCount < testList.size() && (columnCount < columns)) {%>
                        <td>
                            <input type="checkbox"
                                   class="active"
                                   value="<%=testList.get(testCount).getId()%>"
                                   onchange="checkedChanged(true,this)"
                                   checked>
                            <span><%= testList.get(testCount).getValue()%></span>
                            <%
                                testCount++;
                                columnCount++;
                            %></td>
                        <% } %>
                        <% while (columnCount < columns) {
                            columnCount++; %>
                        <td></td>
                        <% } %>
                    </tr>
                    <% } %>
                </table>

                <% 
            		testList = bean.getInactiveTests();
                	testCount = 0; 
                %>
                <table>
                    <% while (testCount < testList.size()) {%>
                    <tr>
                        <%
                            columnCount = 0;
                            while (testCount < testList.size() && (columnCount < columns)) {%>
                        <td>
                            <input type="checkbox"
                                   class="inactive"
                                   value="<%=testList.get(testCount).getId()%>"
                                   onchange="checkedChanged(false,this)">
                            <span><%= testList.get(testCount).getValue()%></span>
                            <%
                                testCount++;
                                columnCount++;
                            %></td>
                        <% } %>
                        <% while (columnCount < columns) {
                            columnCount++; %>
                        <td></td>
                        <% } %>
                    </tr>
                    <% } %>
                </table>
            </div>
        </div>
     </c:forEach>
</div>

<div class="selectShow confirmHide" style="margin-left:auto; margin-right:auto;width: 40%;">
    <input type="button"
           value="<%= MessageUtil.getContextualMessage("label.button.next") %>"
           disabled="disabled"
           onclick="nextStepToConfirmation();"
           id="nextButtonSelect"/>

    <input type="button" value="<%=MessageUtil.getContextualMessage("label.button.cancel")%>" onclick="navigateBack()" />

</div>
</form:form>
