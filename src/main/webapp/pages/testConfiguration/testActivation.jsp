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
 <c:set var="activeTestList" value="${form.activeTestList}" />
 <c:set var="inactiveTestList" value="${form.inactiveTestList}" />
 

<%
	int testCount = 0;
	int columnCount = 0;
	int columns = 4;
%>

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

        //   alert( element.checked + ":"+ isActive + " -> " + (element.checked == isActive));

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

    function nextStepFromSelect() {
        var deactivate = jQuery(".active.activationChanged").length;
        var activate = jQuery(".inactive.activationChanged").length;

        jQuery("#activateSection input").prop("disabled", true);
        //next button is disabled if activate and deactivate = 0
        if (activate == 0) {
            configureForConfirmation();
            confirmationStep(activate, deactivate);
        } else {
            sortStep();
        }

        window.scrollTo(0,0);
    }

    function nextStepToConfirmation(){
        var deactivate = jQuery(".active.activationChanged").length;
        var activate = jQuery(".inactive.activationChanged").length;
        configureForConfirmation();
        confirmationStep(activate, deactivate);

        window.scrollTo(0,0);
    }

    function confirmationStep(activateNumber, deactivateNumber) {
        var jsonObj = {};
        jsonObj.activateSample = [];
        jsonObj.deactivateSample = [];
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
        var activateSampleType = true;
        var activateTestList = jQuery("#testActivateList");
        var activateSampleList = jQuery("#sampleTypeActivateList");
        var jsonBlob, sortOrder;

        jQuery("#testActivate").show();
        jQuery("#sortOrderList ul.sortable").each(function(){
            activateTestList.append("<br />");
            activateTestList.append( jQuery(this).val());
            activateTestList.append("<br />");
            sortOrder = 0;
            jQuery(this).find("li").each(function(){
                jsonBlob = {};
                jsonBlob.id = jQuery(this).val();
                jsonBlob.activated = jQuery(this).hasClass("altered");
                if( jsonBlob.activated){
                    activateTestList.append("&nbsp;&nbsp;&nbsp;&nbsp;" + jQuery(this).text() );
                    activateTestList.append("<br />");
                }
                jsonBlob.sortOrder = sortOrder++;
                jsonObj.activateTest[jsonObj.activateTest.length] = jsonBlob;
            });

        });

        jQuery("#sampleTypeSortOrder ul.sortable").each(function(){
            sortOrder = 0;
            jQuery(this).find("li").each(function(){
              //  if( activateSampleType){
                console.log("activating sample type");
                    jQuery("#sampleTypeActivate").show();
                    activateSampleType = false;
              //  }
                jsonBlob = {};
                jsonBlob.id = jQuery(this).val();
                jsonBlob.activated = jQuery(this).hasClass("altered");
                if( jsonBlob.activated){
                    activateSampleList.append( jQuery(this).text() );
                    activateSampleList.append("<br />");
                }
                jsonBlob.sortOrder = sortOrder++;
                jsonObj.activateSample[jsonObj.activateSample.length] = jsonBlob;
            });

        });
    }

    function listDeactivatedItems(jsonObj) {
        var sampleType, deactivatedTests, jsonBlob;
        var deactivateTestList = jQuery("#testDeactivateList");
        var deactivateSampleList = jQuery("#sampleTypeDeactivateList");


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

                if (deactivatedTests.length == sampleType.parent().find(".active").length) {
                    jsonBlob = {};
                    jsonBlob.id = sampleType.find("input").val();
                    jsonObj.deactivateSample[jsonObj.deactivateSample.length] = jsonBlob;
                    jQuery("#sampleTypeDeactivate").show();
                    deactivateSampleList.append(sampleType.text());
                    deactivateSampleList.append("<br/>");
                }
            }

        });
    }

    function sortStep() {
        configureForSort();
        createSortingUI();
        jQuery(".sortable").sortable();
        jQuery(".sortable").disableSelection();
    }

    function createSortingUI() {
        var sortColumnCount = 0;
        var sortDiv = jQuery("#sortOrderList");
        var table = jQuery(document.createElement("table"));
        var headerRow = jQuery(document.createElement("tr"));
        var bodyRow = jQuery(document.createElement("tr"));
        var addActiveSampleTypesToSort = true;
        var sampleType, activatedTests;
        var headerCell, bodyCell;

        table.append(headerRow);
        table.append(bodyRow);
        table.attr("width", "100%");

        jQuery(".activeSampleType,.inactiveSampleType").each(function () {
            sampleType = jQuery(this);
            activatedTests = sampleType.parent().find(".inactive.activationChanged");
            if (activatedTests.length > 0) {
                if (sortColumnCount >= <%=columns %>) {
                    sortDiv.append(table);
                    table = jQuery(document.createElement("table"));
                    headerRow = jQuery(document.createElement("tr"));
                    bodyRow = jQuery(document.createElement("tr"));
                    table.append(headerRow);
                    table.append(bodyRow);
                    table.attr("width", "100%");
                    sortColumnCount = 0;
                }


                headerCell = jQuery(document.createElement("th"));
                headerCell.attr("width", '25%');
                headerCell.text(sampleType.text());
                headerRow.append(headerCell);

                bodyCell = jQuery(document.createElement("td"));
                insertSortableTestList(bodyCell, sampleType, activatedTests);
                bodyRow.append(bodyCell);
                sortColumnCount++;
            }
        });

        if (sortColumnCount < <%=columns %>) {
            while (sortColumnCount < <%=columns %>) {
                headerCell = jQuery(document.createElement("td"));
                headerRow.append(headerCell);
                sortColumnCount++;
            }
        }
        sortDiv.append(table);

        jQuery(".inactiveSampleType").each(function () {
            sampleType = jQuery(this);
            activatedTests = sampleType.parent().find(".inactive.activationChanged");
            if (activatedTests.length > 0) {
                var UL = jQuery("#sampleTypeSortList");
                if( addActiveSampleTypesToSort){
                    jQuery("#sampleTypeSortOrder").show();
                    addSampleTypeListElements(jQuery(".activeSampleType"), UL, false);
                    addActiveSampleTypesToSort = false;
                }

                addSampleTypeListElement(sampleType, UL, true);
            }
        });

    }

    function insertSortableTestList(cell, sampleType, activatedTests) {
        var UL = jQuery(document.createElement("ul"));
        UL.val(sampleType.text());
        var tests = sampleType.parent().find(".sortable-test");
        UL.addClass("sortable");
        cell.append(UL);
        addTestListElements(tests, UL, false);
        addTestListElements(activatedTests, UL, true);

    }

    function addTestListElements(tests, UL, activated) {
        var LI, test;
        tests.each(function () {
            test = jQuery(this);
            //make sure it test has not been deactivated
            if (activated || !test.hasClass("activationChanged")) {
                LI = createLI(jQuery(this).parent().find("span").text(), jQuery(this).val(), activated);
                UL.append(LI);
            }
        });
    }

    function addSampleTypeListElements(sampleTypes, UL, highlight) {

        sampleTypes.each(function () {
            addSampleTypeListElement(jQuery(this), UL, highlight);
        });
    }

    function addSampleTypeListElement( sampleType, UL, highlight){
        UL.append(createLI(sampleType.text(), sampleType.find("input").val(), highlight ));
    }

    function createLI( name, value, highlight){
        var LI = jQuery(document.createElement("li"));
        var span;

        LI.val(value);
        LI.addClass("ui-state-default_oe");
        if (highlight) {
            LI.addClass("altered");
        }

        span = jQuery(document.createElement("span"));
        span.addClass("ui-icon ui-icon-arrowthick-2-n-s");
        LI.append(span);
        LI.append(name);

        return LI;
    }
    function navigateBack() {
        backFunction();
        window.scrollTo(0,0);
    }

    function selectBack(){
        submitAction('TestManagementConfigMenu');
    }

    function sortBack(){
        configureForSelect();
    }

    function confirmBack(){

       if(jQuery("#testActivationSortButton").is(":visible")){
           configureForSort();
       } else{
           configureForSelect();
       }
    }
    function configureForSelect() {
        jQuery("#step").text("<%=MessageUtil.getContextualMessage("label.testActivate")%>");
        jQuery("#instructions").text("<spring:message code="instructions.test.activation" htmlEscape="false"/>");
        jQuery("#activateSection input").prop("disabled", false);
        jQuery(".selectHide").hide();
        jQuery(".selectShow").show();
        jQuery(".selectClear").empty();
        backFunction = selectBack;
    }

    function configureForSort() {
        jQuery("#step").text("Sort");
        jQuery("#instructions").text("<%=MessageUtil.getContextualMessage("instructions.test.activation.sort")%>");
        jQuery(".sortHide").hide();
        jQuery(".sortShow").show();

        //The reason for the li is that the sample sortable UL is hardcoded as sortable, even if it has no contents
        if( jQuery(".sortable li").length > 0) {
            jQuery(".sortable").sortable("enable");
        }

        backFunction = sortBack;
    }

    function configureForConfirmation() {
        jQuery("#step").text("<%=MessageUtil.getContextualMessage("label.testActivate.confirm")%>");
        jQuery("#instructions").text("<%=MessageUtil.getContextualMessage("instructions.test.activation.confirm")%>");
        jQuery(".confirmHide").hide();
        jQuery(".confirmShow").show();

        //The reason for the li is that the sample sortable UL is hardcoded as sortable, even if it has no contents
        if( jQuery(".sortable li").length > 0) {
            jQuery(".sortable").sortable("disable");
        }

        if(jQuery("#testActivationSort").is(":visible")){
            jQuery("#testActivationSortButton").show();
            jQuery("#testActivationSort").hide();
        }

        backFunction = confirmBack;
    }
    function savePage() {
        window.onbeforeunload = null; // Added to flag that formWarning alert isn't needed.
        var form = document.getElementById("mainForm");
        form.method = "POST";
        form.action = "TestActivation";
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
				   
<input type="button" value='<%= MessageUtil.getContextualMessage("banner.menu.administration") %>'
       onclick="submitAction('MasterListsPage');"
       class="textButton"/> &rarr;
<input type="button" value='<%= MessageUtil.getContextualMessage("configuration.test.management") %>'
       onclick="submitAction('TestManagementConfigMenu');"
       class="textButton"/>&rarr;
<span id="testActivationSelectionButton" class="selectHide sortShow confirmShow" style="display:none" >
<input type="button" value='<%=MessageUtil.getContextualMessage("label.testActivate")%>'
       onclick="configureForSelect();"
       class="textButton"/>&rarr;
</span>

<span id="testActivationSortButton" class="selectHide sortHide" style="display:none" >
<input type="button" value='<%= MessageUtil.getContextualMessage("label.button.sort") %>'
       onclick="configureForSort();"
       class="textButton"/>&rarr;
</span>

<span id="testActivationSort" class="selectHide sortShow" style="display:none" ><%= MessageUtil.getContextualMessage("label.button.sort") %></span>
<span id="testActivationConfirmation" class="selectHide sortHide confirmShow" style="display:none" ><%=MessageUtil.getContextualMessage("label.confirmation")%></span>
<span id="testActivationSelection" class="selectShow sortHide confirmHide"><%=MessageUtil.getContextualMessage("label.testActivate")%></span>

<h1 id="step"><spring:message code="label.testActivate"/></h1><br/>

<div class="indent"><span id="instructions"><spring:message code="instructions.test.activation" htmlEscape="false" /></span></div>

<br>

<div id="testActivate" class="selectHide sortHide" >
    <h4><%=MessageUtil.getContextualMessage("label.test.activate")%></h4>

    <div id="testActivateList" class="selectClear"></div>
    <br>
</div>
<div id="sampleTypeActivate" class="selectHide  sortHide" >
    <h4><%=MessageUtil.getContextualMessage("label.sample.types.activate")%></h4>
    <div id="sampleTypeActivateList" class="selectClear"></div>
    <br>
</div>
<div id="testDeactivate" class="selectHide sortHide">
    <h4><%=MessageUtil.getContextualMessage("label.test.deactivate")%></h4>

    <div id="testDeactivateList" class="selectClear"></div>
    <br>
</div>
<div id="sampleTypeDeactivate" class="selectHide sortHide" >
    <h4><%=MessageUtil.getContextualMessage("label.sample.types.deactivate")%></h4>

    <div id="sampleTypeDeactivateList" class="selectClear"></div>
    <br>
</div>


<div id="sortOrder" class="selectHide sortShow" style="display:none">
    <h4><%=MessageUtil.getContextualMessage("label.test.display.order")%></h4>
    <div id="sortOrderList" class="selectClear" ></div>
</div>
<div id="sampleTypeSortOrder" class="selectHide" style="display: none">
    <hr/>
    <h4><%=MessageUtil.getContextualMessage("label.sample.type.display.order")%></h4>
    <table>
        <tr><th><%=MessageUtil.getContextualMessage("label.sample.types")%></th></tr>
        <tr><td>
            <ul class="sortable ui-sortable selectClear" id="sampleTypeSortList"></ul>
        </td></tr>
    </table>
</div>

<div class="selectHide sortShow confirmShow" style="margin-left:auto; margin-right:auto;width: 40%;">
    <input type="button"
           class="sortShow confirmHide"
           value='<%= MessageUtil.getContextualMessage("label.button.next")%>'
           onclick="nextStepToConfirmation();"
           id="nextButtonSort"/>
    <input type="button"
           class="sortHide confirmShow"
           value='<%=MessageUtil.getContextualMessage("label.button.accept")%>'
           onclick="savePage();"
           id="acceptButton"
           style="display: none"/>

    <input type="button" value='<%=MessageUtil.getContextualMessage("label.button.previous")%>' onclick="navigateBack()" />
</div>

<hr/>

<form:hidden path="jsonChangeList" id="jsonChangeList"/>

<div id="activateSection" class="indent">

    <c:forEach var="activeBean" varStatus="iter" items="${form.activeTestList}">
        <div>
            <span class="activeSampleType"><c:out value="${activeBean.sampleType.value}"/>
                <form:hidden path="activeTestList[${iter.index}].sampleType.id"/>
            </span>
            <div class="indent">
                <% 
                	TestActivationBean bean = (TestActivationBean) pageContext.getAttribute("activeBean");
                	List<IdValuePair> testList = bean.getActiveTests();
                	testCount = 0; 
                %>
                <table>
                    <% while (testCount < testList.size()) { %>
                    <tr>
                        <% columnCount = 0; %>
                        <% while (testCount < testList.size() && (columnCount < columns)) { %>
                        <td>
                            <input type="checkbox"
                                   class="active sortable-test"
                                   value='<%=testList.get(testCount).getId()%>'
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
                        <% columnCount = 0;
                           while (testCount < testList.size() && (columnCount < columns)) {%>
                        <td>
                            <input type="checkbox"
                                   class="inactive"
                                   value='<%=testList.get(testCount).getId()%>'
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
    
    <c:if test="${!empty form.inactiveTestList}">
        <br/><br/>
        <div style="text-align: center"><%= MessageUtil.getContextualMessage("label.testActivate.inactiveSampleTypes") %></div>
        <hr/>
        
        <c:forEach var="inactiveBean" varStatus="iter" items="${form.inactiveTestList}">
            <div>
            <span class="inactiveSampleType"><c:out value="${inactiveBean.sampleType.value}"/>
                <form:hidden path="inactiveTestList[${iter.index}].sampleType.id"/>
            </span>
            <div class="indent">
                <% 
                	TestActivationBean bean = (TestActivationBean) pageContext.getAttribute("inactiveBean");
                	List<IdValuePair> testList = bean.getInactiveTests();
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
                                   value='<%=testList.get(testCount).getId()%>'
                                   onchange="checkedChanged(false,this);">
                            <span><%= testList.get(testCount).getValue()%></span>
                            <%
                                testCount++;
                                columnCount++;
                            %></td>
                        <% } %>
                    </tr>
                    <% } %>
                </table>
            </div>
     	  </div>
 		</c:forEach>   
  	</c:if>
  	
</div>

<div class="selectShow sortHide confirmHide" style="margin-left:auto; margin-right:auto;width: 40%;">
    <input type="button"
           value='<%= MessageUtil.getContextualMessage("label.button.save") %>'
           disabled="disabled"
           onclick="nextStepFromSelect();"
           id="nextButtonSelect"/>

    <input type="button" value='<%=MessageUtil.getContextualMessage("label.button.cancel")%>' onclick="navigateBack()" />


</div>
</form:form>
