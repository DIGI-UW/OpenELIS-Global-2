<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         import="java.util.List,
         		org.openelisglobal.common.action.IActionConstants,
          		org.openelisglobal.common.util.IdValuePair,
          		org.openelisglobal.internationalization.MessageUtil,
         		org.openelisglobal.common.util.Versioning,
         		org.openelisglobal.test.action.BatchTestStatusChangeBean" %>

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

<script type="text/javascript" src="scripts/utilities.js?"></script>
<script type="text/javascript" src="scripts/ajaxCalls.js?"></script>
<script type="text/javascript" src="scripts/jquery.ui.js?"></script>
<script type="text/javascript" src="scripts/jquery.asmselect.js?"></script>
<script type="text/javascript"
        src="scripts/multiselectUtils.js?"></script>

<link rel="stylesheet" type="text/css" href="css/jquery.asmselect.css?" />

<script type="text/javascript">
    var currentSampleType = "";

    jQuery(document).ready(function(){
        <%if( request.getAttribute(IActionConstants.FWD_SUCCESS) != null &&
        ((Boolean)request.getAttribute(IActionConstants.FWD_SUCCESS)) ) { %>
        if( typeof(showSuccessMessage) != 'undefined' ){
            showSuccessMessage( true );
        }
        <% } %>
    });

    function convertToJQueryMultiselect(){
        jQuery("select[multiple]").asmSelect({
            removeLabel: "X"
        });

        jQuery("select[multiple]").change(function (e, data) {
            handleMultiSelectChange(e, data);
        });

        jQuery(".asmContainer").css("display", "inline-block");
    }

    function handleMultiSelectChange(e, data){
        enableNextStepButton();
    }

    function makeDirty() {
        dirty = true;
        if (typeof(showSuccessMessage) === 'function') {
            showSuccessMessage(false); //refers to last save
        }
        // Adds warning when leaving page if content has been entered into makeDirty form fields
        function formWarning() {
            return "<spring:message code="banner.menu.dataLossWarning"/>";
        }

        window.onbeforeunload = formWarning;
    }

    function submitAction(target) {
        window.onbeforeunload = null;
        var form = document.getElementById("mainForm");
        form.action = target;
        form.submit();
    }


    function sampleTypeChanged(selectElement) {
        jQuery("#verifySampleTypeSpan").text(jQuery(selectElement).find(":selected").text());
        currentSampleType = jQuery(selectElement).find(":selected").text();
        if (selectElement.value != 0) {
            getAllTestsForSampleType(selectElement.value, testsForSampleTypeSuccess);
        }
    }

    function testsForSampleTypeSuccess(xhr) {
       //alert(xhr.responseText);
        var formField = xhr.responseXML.getElementsByTagName("formfield").item(0);
        var message = xhr.responseXML.getElementsByTagName("message").item(0);
        var response, i, option, currentSelection, replacementSelection, replacementSelectionSpan;


        if (message.firstChild.nodeValue == "valid") {
            response = JSON.parse(formField.firstChild.nodeValue);
            currentSelection = jQuery("#currentTestSelection");
            replacementSelectionSpan = jQuery("#replacementTestSelectionSpan");
            currentSelection.empty();
            replacementSelectionSpan.empty();
            replacementSelection = jQuery('<select>').attr('multiple', 'multiple').attr('title', 'multiple').attr('id', 'replacementTestSelection');
            replacementSelectionSpan.append(replacementSelection);
            option = jQuery('<option/>').attr({"value" : 0});
            currentSelection.append(option);

            for( i = 0; i < response["tests"].length; i++){
                currentSelection.append(createOption(response["tests"][i]));
                if( response["tests"][i].isActive == 'Y'){
                    replacementSelection.append(createOption(response["tests"][i]));
                }
            }
        }

        if(!jQuery('#includeInactive').is(":checked")){
            jQuery(".inactiveTest").hide();
        }

        convertToJQueryMultiselect();

        jQuery("#testSelection").show();
        jQuery("#sampleSelectionDiv").hide();
        jQuery("#nextStepButton").attr("disabled", "disabled");
        if(jQuery("#cancelOnly").is(":checked")){ jQuery("#cancelOnly").click();}
    }

    function createOption(jsonTest){
        //alert(jsonTest.name);
        var  option = jQuery('<option/>');
        option.attr({ 'value': jsonTest.id }).text(jsonTest.name);
        if( jsonTest.isActive == 'N'){
            option.addClass("inactiveTest");
        }
        return option;
    }

    function removeMultiSelect(){
        jQuery('.asmListItemRemove').each(function(i) {
            jQuery(this).click();
        });
    }
    function currentTestChanged(currentTestSelection){
        if( currentTestSelection.value == 0){
            jQuery("#sampleSelectionDiv").hide();
            jQuery("#nextStepButton").attr('disabled', 'disabled');
        }else{
            getPendingAnalysisForTest( currentTestSelection.value, analysisForTestSuccess);
        }
    }

    function analysisForTestSuccess(xhr) {
        //alert(xhr.responseText);
        var formField = xhr.responseXML.getElementsByTagName("formfield").item(0);
        var message = xhr.responseXML.getElementsByTagName("message").item(0);
        var response, i, checkbox, notStarted, replacementSelection, replacementSelectionSpan;

        if (message.firstChild.nodeValue == "valid") {
            response = JSON.parse(formField.firstChild.nodeValue);
            addLabNoCoumns(response["notStarted"], jQuery("#notStartedSamplesSpan"),jQuery("#notStartedSamplesCheckBox"), true );
            addLabNoCoumns(response["technicianRejection"], jQuery("#technicianRejectedSamplesSpan"), jQuery("#technicianRejectedSamplesCheckBox"), true );
            addLabNoCoumns(response["biologistRejection"], jQuery("#biologistRejectedSamplesSpan"), jQuery("#biologistRejectedSamplesCheckBox"), true );
            addLabNoCoumns(response["notValidated"], jQuery("#notValidatedSamplesSpan"),jQuery("#notValidatedSamplesCheckBox"), false );
        }
        enableNextStepButton();
        jQuery("#sampleSelectionDiv").show();
    }

    function addLabNoCoumns(jsonAnalysisGroup, column, allCheckbox, checked){
        column.empty();
        if(jsonAnalysisGroup.length == 0){
            allCheckbox.removeAttr('checked');
            allCheckbox.attr('disabled', 'disabled');
            return;
        }
        allCheckbox.removeAttr('disabled');

        if( checked){
            allCheckbox.attr('checked','checked');
        }else{
            allCheckbox.removeAttr('checked');
        }

        for( i = 0; i < jsonAnalysisGroup.length; i++){
            checkbox = jQuery('<input/>').attr('type', 'checkbox').attr('value', jsonAnalysisGroup[i]["id"]).change(enableNextStepButton).addClass("labNoSelect");
            if( checked){
                checkbox.attr('checked','checked');
            }
            column.append(checkbox);
            column.append(jsonAnalysisGroup[i]["labNo"]);
            column.append('<br>')
        }
    }


    function showHideInactiveTest(inactiveTestCheckbox){
        if( inactiveTestCheckbox.checked){
            jQuery(".inactiveTest").show();
        }else{
            jQuery(".inactiveTest").hide();
            jQuery("#currentTestSelection").val(0);
        }
    }

    function applyAll(checkAll, spanId){
        if(checkAll.checked){
            jQuery("#" + spanId + " > :input").each(function(){
                jQuery(this).attr('checked', 'checked');
            });
        }else{
            jQuery("#" + spanId + " > :input").each(function(){
                jQuery(this).removeAttr('checked');
            });
        }
        enableNextStepButton();
    }

    function handleCancelOnly(checkboxElement){
        if( checkboxElement.checked){
            alert("<spring:message code="warning.test.batch.reassignment.cancel" />");
            jQuery("#replacementTestSelectionSpan").hide();
        }else{
            jQuery("#replacementTestSelectionSpan").show();
        }

        enableNextStepButton();
    }

    function enableNextStepButton(){
        if( (jQuery(".labNoSelect:checked").length > 0) && (jQuery("#cancelOnly").is(":checked") || jQuery('.asmListItemRemove').length > 0)){
            jQuery("#nextStepButton").removeAttr("disabled");
        }else{
            jQuery("#nextStepButton").attr("disabled", "disabled");
        }
    }


    function restoreSelect(){
        jQuery("#selectDiv").show();
        jQuery("#verifyDiv").hide();

    }
    function nextStep() {
        jQuery("#selectDiv").hide();
        buildVerify();
        jQuery("#verifyDiv").show();
    }

    function buildVerify(){
        var jsonWad = {};
        var canceledTest = jQuery("#currentTestSelection :selected");
        var toTestStrings = "";
        var count = 0;

        jQuery(".testReplaceFrom").text(canceledTest.text());
        jsonWad["current"] = canceledTest.text();
        jsonWad["sampleType"] = currentSampleType;

        if(jQuery('#cancelOnly:checked').length == 1){
            jQuery("#verifyReplaceMessage").hide();
            jQuery("#verifyCancelMessage").show();
        }else{
            jsonWad["replace"] = [];
            jQuery("#replacementTestSelection option:selected").each(function(i) {
                toTestStrings += ", " + jQuery(this).text();
                jsonWad["replace"][count++] = jQuery(this).val();
            });
            jQuery("#testReplaceTo").text(toTestStrings);
            jQuery("#verifyCancelMessage").hide();
            jQuery("#verifyReplaceMessage").show();
        }

        setUpSampleVerification( "notStartedSamplesSpan",
                "verifyChangeNotStartedSamplesSpan",
                "verifyNoChangeNotStartedSamplesSpan",
                jsonWad["changeNotStarted"] = [],
                jsonWad["noChangeNotStarted"] = [] );
        setUpSampleVerification( "technicianRejectedSamplesSpan",
                "verifyChangeTechnicianRejectedSamplesSpan",
                "verifyNoChangeTechnicianRejectedSamplesSpan",
                jsonWad["changeTechReject"] = [],
                jsonWad["noChangeTechReject"] = []);
        setUpSampleVerification( "biologistRejectedSamplesSpan",
                "verifyChangeBiologistRejectedSamplesSpan",
                "verifyNoChangeBiologistRejectedSamplesSpan",
                jsonWad["changeBioReject"] = [],
                jsonWad["noChangeBioReject"] = []);
        setUpSampleVerification( "notValidatedSamplesSpan",
                "verifyChangeNotValidatedSamplesSpan",
                "verifyNoChangeNotValidatedSamplesSpan",
                jsonWad["changeNotValidated"] = [],
                jsonWad["noChangeNotValidated"] = []);

        jQuery("#jsonWad").val(JSON.stringify(jsonWad));
    }

    function setUpSampleVerification( live, change, noChange, jsonChange, jsonNoChange){
        var count = 0;
        var verify = jQuery("#" + change);
        verify.empty();
        jQuery("#" + live + " :checked").each(function(){
            verify.append(this.nextSibling.nodeValue);
            verify.append("<br>");
            jsonChange[count++] = this.value;
        });

        count = 0;
        verify = jQuery("#" + noChange);
        verify.empty();
        jQuery("#" + live + " > input:not(:checked)").each(function(){
            verify.append(this.nextSibling.nodeValue);
            verify.append("<br>");
            jsonNoChange[count++] = this.value;
        });

    }
</script>

<form:hidden path="jsonWad" id="jsonWad" />

<h3><spring:message code="label.selectSampleType"/></h3>

<div id="selectDiv" >
    <c:if test="${not empty form.statusChangedList}" >
    <hr>
    <spring:message code="label.test.batch.status.change.warning" /><br><br>
    <div style="overflow: hidden">
    <div style="float:left; width:15%;overflow: hidden;">
        <spring:message code="label.sampleType" />:&nbsp;<c:out value="${form.statusChangedSampleType}" /><br><br>
        <spring:message code="label.currentTest" />:&nbsp;<c:out value="${form.statusChangedCurrentTest}" /><br><br>
        <c:out value="${form.statusChangedNextTest}" /><br>
    </div>
    <div style="float:left;overflow: hidden;">
        <table>
            <tr>
            <th width="30%"><spring:message code="result.sample.id" /></th>
            <th width="30%"><spring:message code="report.from" /></th>
            <th width="30%"><spring:message code="report.to" /></th>
            </tr>
            <c:forEach items="${form.statusChangedList}" var="bean">
                <tr>
                    <td><c:out value="${bean.labNo}" /></td>
                    <td><c:set value="${bean.oldStatus}" /></td>
                    <td><c:set value="${bean.newStatus}" /></td>
                </tr>
            </c:forEach>
        </table><br>
    </div>
    </div>
    <br>
    <hr>
    </c:if>
    <spring:message code="label.sampleType" /><br>

    <select onchange="sampleTypeChanged(this); makeDirty();" >
    <option value="0"></option>
    <c:forEach items="${form.sampleList}" var="sampleType">
    <option value="${sampleType.id}">${sampleType.value}</option>
    </c:forEach>
    </select>

    <div id="testSelection" style="overflow: hidden; display:none" >
    <br>
    <div id="currentTest" style="float:left; margin:0; width:33%;">
    <spring:message code="label.currentTest" /><br><br>
    <input type="checkbox" id="includeInactive" checked="checked" onchange="showHideInactiveTest(this)"><spring:message code="label.includeInactiveTests" /><br><br>
        <select class="required" id="currentTestSelection" onchange="currentTestChanged(this)" >
        </select>
    </div>

        <div id="replacementTest" style="float:left; margin:0;width:66%;">
    <spring:message code="label.replaceWith" /><br><br>
    <input type="checkbox" id="cancelOnly" onchange="handleCancelOnly(this)" ><spring:message code="label.cancel.test.no.replace" /><br><br>
        <span id="replacementTestSelectionSpan"><select multiple="multiple" id="replacementTestSelection" title="multiple" ></select></span>
    </div>

    </div>
    <div id="sampleSelectionDiv" style="overflow: hidden; display:none" >
    <br>
        <spring:message code="label.checkedWillBeModified"/><br><br>
        <div  style="float:left; margin:0;width:25%;">
            <spring:message code="label.analysisNotStarted"/><br><br>
            <input id="notStartedSamplesCheckBox" type="checkbox" checked="checked" onchange='applyAll(this, "notStartedSamplesSpan")'><spring:message code="label.button.checkAll" /><br><br>
            <span id="notStartedSamplesSpan" ></span>
        </div>
        <div  style="float:left; margin:0;width:25%;">
            <spring:message code="label.rejectedByTechnician"/><br><br>
            <input type="checkbox" id="technicianRejectedSamplesCheckBox" checked="checked" onchange='applyAll(this, "technicianRejectedSamplesSpan")'><spring:message code="label.button.checkAll" /><br><br>
            <span id="technicianRejectedSamplesSpan" ></span>
        </div>
        <div style="float:left; margin:0;width:25%;">
            <spring:message code="label.rejectedByBiologist" /><br><br>
            <input type="checkbox" id="biologistRejectedSamplesCheckBox" checked="checked" onchange='applyAll(this, "biologistRejectedSamplesSpan")'><spring:message code="label.button.checkAll" /><br><br>
            <span id="biologistRejectedSamplesSpan" ></span>
        </div>
        <div style="float:left; margin:0;width:25%;">
            <spring:message code="label.notValidated"/><br><br>
            <input type="checkbox"  id="notValidatedSamplesCheckBox" onchange='applyAll(this, "notValidatedSamplesSpan")'><spring:message code="label.button.checkAll" /><br><br>
            <span id="notValidatedSamplesSpan" ></span>
        </div>
    </div>
    <div align="center">
        <input type="button" id="nextStepButton" onclick="nextStep()" disabled="disabled" value='<%=MessageUtil.getMessage("label.button.process")%>'>
    &nbsp;<button onclick='submitAction("MasterListsPage")' ><spring:message code="label.button.cancel"/></button>
    </div>
</div>
<div id="verifyDiv" style="display:none">
    <spring:message code="label.sampleType" />:&nbsp;<span id="verifySampleTypeSpan" ></span><br><br>
    <div id="verifyReplaceMessage" >
        <spring:message code="label.test.batch.replace.start" />&nbsp;<span class="testReplaceFrom" ></span>&nbsp;<spring:message code="label.test.batch.replace.finish"/><span id="testReplaceTo" ></span>
    </div>
    <div id="verifyCancelMessage" >
        <spring:message code="label.test.batch.cancel.start" />&nbsp;<span class="testReplaceFrom" ></span>&nbsp;<spring:message code="label.test.batch.cancel.finish"/>
    </div>

    <hr>
    <div id="verifyChangeDiv" style="overflow: hidden;" >
        <br>
        <div  style="float:left; margin:0;width:25%;">
            <spring:message code="label.analysisNotStarted"/><br><br>
            <span id="verifyChangeNotStartedSamplesSpan" ></span>
        </div>
        <div  style="float:left; margin:0;width:25%;">
            <spring:message code="label.rejectedByTechnician"/><br><br>
            <span id="verifyChangeTechnicianRejectedSamplesSpan" ></span>
        </div>
        <div style="float:left; margin:0;width:25%;">
            <spring:message code="label.rejectedByBiologist" /><br><br>
            <span id="verifyChangeBiologistRejectedSamplesSpan" ></span>
        </div>
        <div style="float:left; margin:0;width:25%;">
            <spring:message code="label.notValidated"/><br><br>
            <span id="verifyChangeNotValidatedSamplesSpan" ></span>
        </div>
    </div>

    <hr>
    <spring:message code="label.test.batch.replace.start" />&nbsp;<span class="testReplaceFrom" ></span>&nbsp;<spring:message code="label.test.batch.no.change.finish"/><span id="testReplaceTo" ></span><br><br>

    <div id="verifyNoChangeDiv" style="overflow: hidden;" >
        <br>
        <div  style="float:left; margin:0;width:25%;">
            <spring:message code="label.analysisNotStarted"/><br><br>
            <span id="verifyNoChangeNotStartedSamplesSpan" ></span>
        </div>
        <div  style="float:left; margin:0;width:25%;">
            <spring:message code="label.rejectedByTechnician"/><br><br>
            <span id="verifyNoChangeTechnicianRejectedSamplesSpan" ></span>
        </div>
        <div style="float:left; margin:0;width:25%;">
            <spring:message code="label.rejectedByBiologist" /><br><br>
            <span id="verifyNoChangeBiologistRejectedSamplesSpan" ></span>
        </div>
        <div style="float:left; margin:0;width:25%;">
            <spring:message code="label.notValidated"/><br><br>
            <span id="verifyNoChangeNotValidatedSamplesSpan" ></span>
        </div>
    </div>

    <div align="center">
        <button onclick='submitAction("BatchTestReassignment")' ><spring:message code="label.button.accept"/></button>&nbsp;
        <input type="button" onclick="restoreSelect()"  value='<%=MessageUtil.getMessage("label.button.reject")%>'>
    </div>
</div>

