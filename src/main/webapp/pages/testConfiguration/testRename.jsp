<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         import="org.openelisglobal.common.action.IActionConstants,
         		org.openelisglobal.common.util.IdValuePair,
         		org.openelisglobal.common.util.*, org.openelisglobal.internationalization.MessageUtil,
         		org.openelisglobal.common.util.Versioning,
         		java.util.List,
         		java.util.ArrayList,
         		org.openelisglobal.testconfiguration.form.TestRenameEntryForm" %>
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
<c:set var="formName" value="${form.formName}" />
<c:set var="testList" value="${form.testList}" />


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

    function setForEditing(testId, name) {
        jQuery("#editDiv").show();
        jQuery("#testName").text(name);
        jQuery(".error").each(function (index, value) {
            value.value = "";
            jQuery(value).removeClass("error");
            jQuery(value).removeClass("confirmation");
        });
        jQuery("#testId").val(testId);
        jQuery(".test").each(function () {
            var element = jQuery(this);
            element.prop("disabled", "disabled");
            element.addClass("disabled-text-button");
        });
        getTestNames(testId, testNameSuccess);
    }

    function testNameSuccess(xhr) {
        //alert(xhr.responseText);
        var formField = xhr.responseXML.getElementsByTagName("formfield").item(0);
        var message = xhr.responseXML.getElementsByTagName("message").item(0);
        var response;


        if (message.firstChild.nodeValue == "valid") {
            response = JSON.parse(formField.firstChild.nodeValue);
            jQuery("#nameEnglish").text(response["name"]["english"]);
            jQuery("#nameFrench").text(response["name"]["french"]);
            jQuery("#reportNameEnglish").text(response["reportingName"]["english"]);
            jQuery("#reportNameFrench").text(response["reportingName"]["french"]);
            jQuery(".required").each(function () {
                jQuery(this).val("");
            });
        }

        window.onbeforeunload = null;
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
            alert('<%=MessageUtil.getContextualMessage("error.all.required")%>');
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
            jQuery("#action").text('<%=MessageUtil.getContextualMessage("label.confirmation")%>');
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
        jQuery("#action").text('<%=MessageUtil.getContextualMessage("label.button.edit")%>');
    }

    function cancel() {
        jQuery("#editDiv").hide();
        jQuery("#testId").val("");
        jQuery(".test").each(function () {
            var element = jQuery(this);
            element.removeProp("disabled");
            element.removeClass("disabled-text-button");
        });
        window.onbeforeunload = null;
    }

    function handleInput(element) {
        jQuery(element).removeClass("error");
        makeDirty();
    }

    function savePage() {
        window.onbeforeunload = null; // Added to flag that formWarning alert isn't needed.
        var form = document.getElementById("mainForm");
        form.action = "TestRenameEntry";
        form.submit();
    }
</script>

<%
	int testCount = 0;
	int columnCount = 0;
	int columns = 3;
    List testList =  ((TestRenameEntryForm) request.getAttribute("form")).getTestList();
%>


	<form:form name="${form.formName}" 
				   action="${form.formAction}" 
				   modelAttribute="form" 
				   onSubmit="return submitForm(this);" 
				   method="${form.formMethod}"
				   id="mainForm">

<form:hidden path="testId" id="testId"/>


<input 	type="button"
		class="textButton" 
		value="<%= MessageUtil.getContextualMessage("banner.menu.administration")%>"
		onclick="submitAction('MasterListsPage');" >&rarr;

<input  type="button" 
		class="textButton"
		value="<%= MessageUtil.getContextualMessage("configuration.test.management") %>"
       	onclick="submitAction('TestManagementConfigMenu');" >&rarr;
        

<%=MessageUtil.getContextualMessage( "label.testName" ) %>
<br><br>


<div id="editDiv" style="display:none;">
    <h1 id="action"><spring:message code="label.button.edit"/></h1>
    <h2><%=MessageUtil.getContextualMessage( "sample.entry.test" )%>:<span id="testName"></span></h2>
    <br>
    <table>
        <tr>
            <td></td>
            <th colspan="2" style="text-align: center"><spring:message code="test.testName"/></th>
            <th colspan="2" style="text-align: center"><spring:message code="test.testName.reporting"/></th>
        </tr>
        <tr>
            <td></td>
            <td style="text-align: center"><spring:message code="label.english"/></td>
            <td style="text-align: center"><spring:message code="label.french"/></td>
            <td style="text-align: center"><spring:message code="label.english"/></td>
            <td style="text-align: center"><spring:message code="label.french"/></td>
        </tr>
    
     	<tr>
            <td style="padding-right: 20px"><spring:message code="label.current"/>:</td>
            <td id="nameEnglish" style="padding-left: 10px"></td>
            <td id="nameFrench" style="padding-left: 10px"></td>
            <td id="reportNameEnglish" style="padding-left: 10px"></td>
            <td id="reportNameFrench" style="padding-left: 10px"></td>
        </tr>
        
         <tr>
            <td style="padding-right: 20px"><spring:message code="label.new"/>:</td>
            <td><span class="requiredlabel">*</span>
            	<form:input
            			path="nameEnglish"
                        cssClass="required"
                        size="35"
                        onchange="handleInput(this);"/>
            </td>
            <td><span class="requiredlabel">*</span><form:input
            			path="nameFrench"
            			cssClass="required"
                        size="35"
                        onchange="handleInput(this);"/>
            </td>
            <td><span class="requiredlabel">*</span><form:input 
            			path="reportNameEnglish"
            			cssClass="required"
                        size="35"
                        onchange="handleInput(this);"/>
            </td>
            <td><span class="requiredlabel">*</span><form:input 
            			path="reportNameFrench"
            			cssClass="required"
                        size="35"
                        onchange="handleInput(this);"/>
            </td>
        </tr>
     </table>
     

  

     <div style="text-align: center" id="editButtons">
        <input type="button" value='<%=MessageUtil.getContextualMessage("label.button.save")%>'
               onclick="confirmValues();"/>
        <input type="button" value='<%=MessageUtil.getContextualMessage("label.button.cancel")%>'
               onclick='cancel()'/>
    </div>

    <div style="text-align: center; display: none;" id="confirmationButtons">
        <input type="button" value='<%=MessageUtil.getContextualMessage("label.button.accept")%>'
               onclick="savePage();"/>
        <input type="button" value='<%=MessageUtil.getContextualMessage("label.button.reject")%>'
               onclick='rejectConfirmation();'/>
    </div>
    <br><br>
   
     </div>
   </form:form>

<table>
    <% while(testCount < testList.size()){%>
    <tr>
        <td><input type="button" value='<%= ((IdValuePair)testList.get(testCount)).getValue() %>'
                   onclick="setForEditing( '<%= ((IdValuePair)testList.get(testCount)).getId() + "', '" + ((IdValuePair)testList.get(testCount)).getValue() %>');"
                   class="textButton test"/>
            <%
                testCount++;
                columnCount = 1;
            %></td>
        <% while(testCount < testList.size() && ( columnCount < columns )){%>
        <td><input type="button" value='<%= ((IdValuePair)testList.get(testCount)).getValue() %>'
                   onclick="setForEditing( '<%= ((IdValuePair)testList.get(testCount)).getId() + "', '" + ((IdValuePair)testList.get(testCount)).getValue() %>' );"
                   class="textButton test"/>
            <%
                testCount++;
                columnCount++;
            %></td>
        <% } %>

    </tr>
    <% } %>
</table>

<br>
<input type="button" value='<%= MessageUtil.getContextualMessage("label.button.finished") %>'
       onclick="submitAction('TestManagementConfigMenu');"/>

