<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         import="org.openelisglobal.internationalization.MessageUtil,
         		java.util.List,
         		java.util.Locale,
         		org.springframework.context.i18n.LocaleContextHolder,
         		org.openelisglobal.common.action.IActionConstants,
         		org.openelisglobal.common.util.IdValuePair,
         		org.openelisglobal.common.util.Versioning,
         		org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl" %>

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

<%
	Locale locale = LocaleContextHolder.getLocale();
%>
<%--Do not add jquery.ui.js, it will break the sorting --%>
<script type="text/javascript" src="scripts/jquery.asmselect.js?"></script>
<script type="text/javascript" src="scripts/ajaxCalls.js?"></script>
<script type="text/javascript"
        src="scripts/multiselectUtils.js?"></script>
<script type="text/javascript" src="scripts/jquery-ui.js?"></script>
<link rel="stylesheet" type="text/css" href="css/jquery.asmselect.css?"/>
<link rel="stylesheet" media="screen" type="text/css"
      href="css/jquery_ui/jquery.ui.theme.css?"/>
<link rel="stylesheet" type="text/css" href="css/openElisCore.css?"/>


 <%--
<bean:define id="sampleTypeList" name='${form.formName}' property="sampleTypeList" type="java.util.List<IdValuePair>"/>
<bean:define id="panelList" name='${form.formName}' property="panelList" type="java.util.List<IdValuePair>"/>
<bean:define id="uomList" name='${form.formName}' property="uomList" type="java.util.List<IdValuePair>"/>
<bean:define id="resultTypeList" name='${form.formName}' property="resultTypeList" type="java.util.List<IdValuePair>"/>
<bean:define id="testUnitList" name='${form.formName}' property="labUnitList" type="java.util.List<IdValuePair>"/>
<bean:define id="ageRangeList" name='${form.formName}' property="ageRangeList" type="java.util.List<IdValuePair>"/>
<bean:define id="dictionaryList" name='${form.formName}' property="dictionaryList" type="java.util.List<IdValuePair>"/>
<bean:define id="groupedDictionaryList" name='${form.formName}' property="groupedDictionaryList"
             type="java.util.List<java.util.List<IdValuePair>>"/>
--%>
             
<c:set var="sampleTypeList" value="${form.sampleTypeList}" />
<c:set var="panelList" value="${form.panelList}" />
<c:set var="uomList" value="${form.uomList}" />
<c:set var="resultTypeList" value="${form.resultTypeList}" />
<c:set var="testUnitList" value="${form.labUnitList}" />
<c:set var="ageRangeList" value="${form.ageRangeList}" />
<c:set var="dictionaryList" value="${form.dictionaryList}" />
<c:set var="groupedDictionaryList" value="${form.groupedDictionaryList}" />             

<%
	int testCount = 0;
	int columnCount = 0;
	int columns = 4;
%>

<script type="text/javascript">
    var step = "step1";
    var currentNormalRangeIndex = 1;
    var maxAgeInMonths = 0;

    if (!jQuery) {
        var jQuery = jQuery.noConflict();
    }

    jQuery(document).ready(function () {
        jQuery("select[multiple]").asmSelect({
            removeLabel: "X"
        });

        jQuery("select[multiple]").change(function (e, data) {
            handleMultiSelectChange(e, data);
        });

        jQuery("#dictionarySelectId .asmSelect").css("max-width", "275px");
    });


    function augmentMultiselects(parent) {
        jQuery(parent + " select[multiple]").asmSelect({
            removeLabel: "X"
        });

        jQuery(parent + " select[multiple]").change(function (e, data) {
            handleMultiSelectChange(e, data);
        });
    }
    function handleMultiSelectChange(e, data) {
        //e is the event
        //e.currentTarget is the id of the select element
        //data.type will be add or drop
        //data.value is the value from the select list

        if (step == 'step2') {
            createOrderBoxForSampleType(data);
        } else if (step == 'step3Dictionary') {
            appendOrderBoxForDictionary(e, data);
        }
        checkReadyForNextStep(e, data);
    }

    function createOrderBoxForSampleType(data) {
        var sampleTypeName = jQuery("#sampleTypeSelection option[value=" + data.value + "]").text();
        var divId = data.value;
        if (data.type == 'add') {
            createNewSortingDiv(sampleTypeName, divId);
            getTestsForSampleType(data.value, testForSampleTypeSuccess);
        } else {
            jQuery("#" + divId).remove();
        }
    }

    function appendOrderBoxForDictionary(e, data) {
        var dictionaryName = jQuery(e.currentTarget).find("option[value=" + data.value + "]").text();
        var qualifiyList = jQuery("#qualifierSelection");
        var ul = jQuery("#dictionaryNameSortUI");
        var li, option;
        ul.addClass("sortable");
        if (e.currentTarget.id == "dictionarySelection") {
            if (data.type == 'add') {
                li = createLI(data.value, dictionaryName, false);
                li.addClass("ui-sortable-handle");
                ul.append(li);
                jQuery("#dictionaryNameSortUI").sortable();
                jQuery("#dictionaryNameSortUI").disableSelection();
                jQuery("#dictionaryQualify .asmContainer").remove();
                jQuery("#dictionaryQualify").append(qualifiyList);

                option = createOption(data.value, dictionaryName, false);
                qualifiyList.append(option);
                augmentMultiselects("#dictionaryQualify");

                option = createOption(data.value, dictionaryName, false);
                jQuery("#referenceSelection").append(option);
                
                option = createOption(data.value, dictionaryName, false);
                jQuery("#defaultTestResultSelection").append(option);
            } else {
                jQuery("#dictionaryNameSortUI li[value=" + data.value + "]").remove();

                jQuery("#dictionaryQualify .asmContainer").remove();
                qualifiyList.find("option[value=" + data.value + "]").remove();
                jQuery("#dictionaryQualify").append(qualifiyList);
                augmentMultiselects("#dictionaryQualify");
                jQuery("#referenceSelection option[value=" + data.value + "]").remove();
                jQuery("#defaultTestResultSelection option[value=" + data.value + "]").remove();
            }
        }
    }

    function dictionarySetSelected(index) {
        var dictionarySelect;
        //clear existing selections
        clearDictionaryLists();
        //add new selections
        jQuery("#dictionaryGroup_" + index + " li").each(function () {
            dictionarySelect = jQuery("#dictionarySelectId .asmSelect option[value=" + jQuery(this).val() + "]");
            dictionarySelect.attr("selected", "selected");
            dictionarySelect.trigger('change');
        });
    }

    function clearDictionaryLists(){
        jQuery("#qualifierSelection option").remove();
        clearMultiSelectContainer(jQuery(".dictionaryMultiSelect"));
        jQuery("#dictionaryNameSortUI li").remove();
        jQuery("#referenceSelection option").remove();
        jQuery("#referenceSelection").append(createOption("0", "", false));
        jQuery("#defaultTestResultSelection option").remove();
        jQuery("#defaultTestResultSelection").append(createOption("0", "", false));
    }
    function createOption(id, name, isActive) {
        var option = jQuery('<option/>');
        option.attr({'value': id}).text(name);
        if (isActive == 'N') {
            option.addClass("inactiveTest");
        }
        return option;
    }
    function guideSelection(checkbox) {
        if (checkbox.checked) {
            jQuery("#guide").show();
        } else {
            jQuery("#guide").hide();
        }
    }

    function genderMatersForRange(checked, index) {
        if (checked) {
            jQuery(".sexRange_" + index).show();
        } else {
            jQuery(".sexRange_" + index).hide();
            jQuery("#lowNormal_G_" + index).val("-Infinity");
            jQuery("#highNormal_G_" + index).val("Infinity");
            jQuery("#lowNormal_G_" + index).removeClass("error");
            jQuery("#highNormal_G_" + index).removeClass("error");
        }
    }

    function copyFromTestName() {
        jQuery("#testReportNameEnglish").val(jQuery("#testNameEnglish").val());
        jQuery("#testReportNameFrench").val(jQuery("#testNameFrench").val());
    }

    function testForSampleTypeSuccess(xhr) {
        //alert(xhr.responseText);
        var response = xhr.responseXML.getElementsByTagName("formfield").item(0);
        var tests = response.getElementsByTagName("test");
        var sampleTypeId = getValueFromXmlElement(response, "sampleTypeId");
        var test, name, id;
        var ul = jQuery(document.createElement("ul"));
        var length = tests.length;
        ul.addClass("sortable sortable-tag");

        for (var i = 0; i < length; ++i) {
            test = tests[i];
            name = getValueFromXmlElement(test, "name");
            id = getValueFromXmlElement(test, "id");
            ul.append(createLI(id, name, false));
        }

        <% if( locale.getLanguage().equals("en")){ %>
        	ul.append( createLI(0, jQuery("#testNameEnglish").val(), true) );
        <% } else { %>
        	ul.append( createLI(0, jQuery("#testNameFrench").val(), true) );
        <% } %>

        jQuery("#sort" + sampleTypeId).append(ul);

        jQuery(".sortable").sortable();
        jQuery(".sortable").disableSelection();
    }

    function createLI(id, name, highlight) {
        var li = jQuery(document.createElement("li"));
        var span = jQuery(document.createElement("span"));

        li.val(id);
        li.addClass("ui-state-default_oe ui-state-default_oe-tag");
        span.addClass("ui-icon ui-icon-arrowthick-2-n-s");
        li.append(span);
        li.append(name);
        if (highlight) {
            li.addClass("altered");
        }
        return li
    }
    function getValueFromXmlElement(parent, tag) {
        var element = parent.getElementsByTagName(tag);
        return element ? element[0].childNodes[0].nodeValue : "";
    }
    function createNewSortingDiv(sampleTypeName, divId) {
        var mainDiv = jQuery(document.createElement("div"));
        var nameSpan = createNameSpan(sampleTypeName);
        var sortSpan = createSortSpan(divId);

        mainDiv.attr("id", divId);
        mainDiv.addClass("sortingMainDiv");
        mainDiv.css("padding", "20px");
        mainDiv.append(nameSpan);
        mainDiv.append(sortSpan);
        jQuery("#endOrderMarker").before(mainDiv);

    }

    function createNameSpan(sampleTypeName) {
        var nameSpan = jQuery(document.createElement("span"));
        nameSpan.addClass("half-tab");
        nameSpan.append(sampleTypeName);
        return nameSpan;
    }

    function createSortSpan(divId) {
        var sortSpan = jQuery(document.createElement("span"));
        sortSpan.attr("id", "sort" + divId);

        return sortSpan;
    }

    function makeSortListsReadOnly() {
        if (jQuery(".sortable li").length > 0) {
            jQuery(".sortable").removeClass("sortable");
            jQuery(".ui-state-default_oe").removeClass("ui-state-default_oe");
        }
    }

    function upperAgeRangeChanged(index) {
    	var copy, htmlCopy, monthYear, lowAge, lowAgeValue, highAgeValue, lowAgeModifier, newDayValue;
        var element = jQuery("#upperAgeSetter_" + index);

        element.removeClass("error");
        if (element.val() != "Infinity") {
            monthYear = jQuery(".yearMonthSelect_" + index + ":checked").val();

            if (index != 0) {
                lowAge = jQuery("#lowerAge_" + index).text();
                lowAgeModifier = lowAge.charAt(lowAge.length - 1);
                lowAgeValue = lowAge.substring(0, lowAge.length - 1);
                lowAgeValue = lowAgeModifier == '<%=MessageUtil.getMessage("abbreviation.day.single")%>' ? +lowAgeValue : lowAgeModifier == '<%=MessageUtil.getMessage("abbreviation.month.single")%>' ? Math.floor(lowAgeValue * 365/12) : lowAgeValue *= 365;
<%--                 lowAgeValue = lowAgeModifier == "<%=MessageUtil.getContextualMessage("abbreviation.year.single")%>" ? lowAgeValue *= 12 : +lowAgeValue; --%>
                highAgeValue = +element.val();
                if (highAgeValue != element.val()) {
                    alert("<%=MessageUtil.getContextualMessage("error.age.value")%>");
                    element.addClass("error");
                    return;
                }

<%--                 newMonthValue = monthYear == '<%=MessageUtil.getContextualMessage("abbreviation.month.single")%>' ? highAgeValue : 12 * highAgeValue; --%>
                newDayValue = monthYear == '<%=MessageUtil.getMessage("abbreviation.day.single")%>' ? highAgeValue : monthYear == '<%=MessageUtil.getMessage("abbreviation.month.single")%>' ? Math.floor(highAgeValue * 365/12) : 365 * highAgeValue;
                if (newDayValue <= lowAgeValue) {
                    element.addClass("error");
                    alert("<%=MessageUtil.getContextualMessage("error.age.begining.ending.order")%>");
                    return;
                }
            }


            jQuery(element).hide();
            jQuery("#upperAge_" + index).text(element.val() + monthYear);
            jQuery(".yearMonthSelect_" + index).attr("disabled", "disabled");
            jQuery("#ageRangeSelect_" + index).attr("disabled", "disabled");
            copy = jQuery("#normalRangeTemplate table tbody").clone();
            htmlCopy = copy.html().replace(/index/g, currentNormalRangeIndex);
            jQuery("#endRow").before(htmlCopy);
            jQuery(".sexRange_" + currentNormalRangeIndex).hide();
            jQuery("#lowerAge_" + currentNormalRangeIndex).text(element.val() + monthYear);
            if (index != 0) {
                jQuery("#removeButton_" + index).hide();
            }
            currentNormalRangeIndex++;
        }

    }

    function removeLimitRow(index) {
        jQuery(".row_" + index).remove();

        for (var i = index - 1; index >= 0; i--) {
            if (jQuery(".row_" + i)) {
                jQuery(".yearMonthSelect_" + i).removeAttr("disabled");
                jQuery("#ageRangeSelect_" + i).removeAttr("disabled");
                jQuery("#ageRangeSelect_" + i).val(0);
                jQuery("#upperAge_" + i).text("");
                jQuery("#upperAgeSetter_" + i).show();
                if (i != 0) {
                    jQuery("#removeButton_" + i).show();
                }
                break;
            }
        }
    }

    function ageRangeSelected(element, index) {
        var ageInMonths = jQuery(element).find("option:selected").val();
        var selectFound = false;
        var optionValue;

        if (ageInMonths != 0) {
            if (ageInMonths == "Infinity") {
                jQuery("#upperAgeSetter_" + index).val(ageInMonths);
            } else if (ageInMonths % 12 == 0) {
                jQuery("input:radio[name=time_" + index + "]").val(["<%=MessageUtil.getContextualMessage("abbreviation.year.single")%>"]);
                jQuery("#upperAgeSetter_" + index).val(ageInMonths / 12);
            } else {
                jQuery("input:radio[name=time_" + index + "]").val(["<%=MessageUtil.getContextualMessage("abbreviation.month.single")%>"]);
                jQuery("#upperAgeSetter_" + index).val(ageInMonths);
            }
            upperAgeRangeChanged(index);

            jQuery("#ageRangeSelect_" + (currentNormalRangeIndex - 1) + " option").each(function () {
                optionValue = jQuery(this).val();
                if (!selectFound) {
                    if (optionValue == ageInMonths) {
                        selectFound = true;
                    }
                    if (optionValue != 0) {
                        jQuery(this).hide();
                    }
                }
            });
        }
    }

    function normalRangeCheck(index) {
        var lowNormalValue, highNormalValue, lowValidValue, highValidValue;
        var lowGenderNormalValue, highGenderNormalValue;
        var lowGenderNormal, highGenderNormal;
        var lowNormal = jQuery("#lowNormal_" + index);
        var highNormal = jQuery("#highNormal_" + index);
        var lowValid = jQuery("#lowValid");
        var highValid = jQuery("#highValid");
        var checkGenderValues = jQuery("#genderCheck_" + index).is(':checked');

        //check to see if the normal ranges are numeric (Except for infinity) and then compare them to make sure they
        //are ordered correctly.
        lowNormal.removeClass("error");
        lowNormalValue = +lowNormal.val();
        if (lowNormalValue != "-Infinity" &&
                lowNormalValue != lowNormal.val()) {
            lowNormal.addClass("error");
            alert("<%=MessageUtil.getContextualMessage("error.low.normal.value")%>");
            return;
        }

        highNormal.removeClass("error");
        highNormalValue = +highNormal.val();
        if (highNormalValue != "Infinity" &&
                highNormalValue != highNormal.val()) {
            highNormal.addClass("error");
            alert("<%=MessageUtil.getContextualMessage("error.high.normal.value")%>");
            return;
        }

        if (highNormalValue != "Infinity" && lowNormalValue != "-Infinity") {
            if (highNormalValue <= lowNormalValue) {
                highNormal.addClass("error");
                lowNormal.addClass("error");
                alert("<%=MessageUtil.getContextualMessage("error.low.normal.high.normal.order")%>");
                return;
            }
        }

        if (checkGenderValues) {
            lowGenderNormal = jQuery("#lowNormal_G_" + index);
            highGenderNormal = jQuery("#highNormal_G_" + index);
            lowGenderNormal.removeClass("error");
            lowGenderNormalValue = +lowGenderNormal.val();
            if (lowGenderNormalValue != "-Infinity" &&
                    lowGenderNormalValue != lowGenderNormal.val()) {
                lowGenderNormal.addClass("error");
                alert("<%=MessageUtil.getContextualMessage("error.low.normal.value")%>");
                return;
            }

            highGenderNormal.removeClass("error");
            highGenderNormalValue = +highGenderNormal.val();
            if (highGenderNormalValue != "Infinity" &&
                    highGenderNormalValue != highGenderNormal.val()) {
                highGenderNormal.addClass("error");
                alert("<%=MessageUtil.getContextualMessage("error.high.gender.value")%>");
                return;
            }

            if (highGenderNormalValue != "Infinity" && lowGenderNormalValue != "-Infinity") {
                if (highGenderNormalValue <= lowGenderNormalValue) {
                    highGenderNormal.addClass("error");
                    lowGenderNormal.addClass("error");
                    alert("<%=MessageUtil.getContextualMessage("error.low.normal.high.normal.order")%>");
                    return;
                }
            }
        }

        //below we are testing against the valid values
        lowValidValue = +lowValid.val();
        if (lowValidValue != "-Infinity" &&
                lowValidValue != lowValid.val()) {
            return;
        }

        highValidValue = +highValid.val();
        if (highValidValue != "Infinity" &&
                highValidValue != highValid.val()) {
            return;
        }


        if (lowValidValue == "-Infinity" && highValidValue == "Infinity") {
            return;
        }

        if (lowValidValue != "-Infinity" && lowNormalValue < lowValidValue) {
            lowNormal.addClass("error");
            alert("<%=MessageUtil.getContextualMessage("error.low.normal.low.valid.order")%>");
            return;
        }

        if (highValidValue != "Infinity" && highNormalValue > highValidValue) {
            highNormal.addClass("error");
            alert("<%=MessageUtil.getContextualMessage("error.high.normal.high.valid.order")%>");
            return;
        }

        if (checkGenderValues) {
            if (lowValidValue != "-Infinity" && lowGenderNormalValue < lowValidValue) {
                lowGenderNormal.addClass("error");
                alert("<%=MessageUtil.getContextualMessage("error.low.normal.low.valid.order")%>");
                return;
            }

            if (highValidValue != "Infinity" && highGenderNormalValue > highValidValue) {
                highGenderNormal.addClass("error");
                alert("<%=MessageUtil.getContextualMessage("error.high.normal.high.valid.order")%>");
            }
        }
    }

    function validRangeCheck() {
        var highValidValue, lowValidValue;
        var lowValid = jQuery("#lowValid");
        var highValid = jQuery("#highValid");
        lowValid.removeClass("error");
        lowValidValue = +lowValid.val();
        if (lowValidValue != "-Infinity" &&
                lowValidValue != lowValid.val()) {
            lowValid.addClass("error");
            alert("<%=MessageUtil.getContextualMessage("error.low.valid.value")%>");
            return;
        }
        highValid.removeClass("error");
        highValidValue = +highValid.val();
        if (highValidValue != "Infinity" &&
                highValidValue != highValid.val()) {
            highValid.addClass("error");
            alert("<%=MessageUtil.getContextualMessage("error.high.valid.value")%>");
            return;
        }
        if (lowValidValue != "-Infinity" && highValidValue != "Infinity" &&
                lowValidValue >= highValidValue) {
            highValid.addClass("error");
            lowValid.addClass("error");
            alert("<%=MessageUtil.getContextualMessage("error.low.high.valid.order")%>");
            return;
        }
        jQuery(".rowKey").each(function () {
            //index is in the template
            if (jQuery(this).val() != "index") {
                normalRangeCheck(jQuery(this).val());
            }
        });
    }

    function criticalRangeCheckLow(index) {
         var lowCriticalValue;

        var lowCritical = jQuery("#lowCritical");
        var lowValid = jQuery("#lowValid");
        var lowNormal = jQuery("#lowNormal_" + index); 

         lowCritical.removeClass("error");
        lowCriticalValue = lowCritical.val();
        if (lowCriticalValue != "-Infinity" && lowCriticalValue < lowValid.val() ||
         lowCriticalValue != "-Infinity" && lowCriticalValue  > lowNormal.val()) {
            lowCritical.addClass("error");
            alert("<%=MessageUtil.getContextualMessage("error.critical.range.value.low")%>");
            return;
        }     
        jQuery(".rowKey").each(function () {
            //index is in the template
            if (jQuery(this).val() != "index") {
                normalRangeCheck(jQuery(this).val());
            }
        });
    }

    function criticalRangeCheckHigh(index) {
        var highCriticalValue, highValidValue,highNormalValue;

        var highCritical = jQuery("#highCritical");
        var highValid = jQuery("#highValid");
        var highNormal = jQuery("#highNormal_" + index);
        
        highCritical.removeClass("error");
        highCriticalValue = highCritical.val();
        highValidValue = +highValid.val();
        highNormalValue = +highNormal.val();
        if (highCriticalValue != "-Infinity" && highCriticalValue < highNormalValue ||
         highCriticalValue != "-Infinity" && highCriticalValue > highValidValue) {
             highCritical.addClass("error");
            alert("<%=MessageUtil.getContextualMessage("error.critical.range.value.high")%>");
            return;
        }  
     }

    function reportingRangeCheck() {
        var highReportingRangeValue, lowReportingRangeValue;
        var lowReportingRange = jQuery("#lowReportingRange");
        var highReportingRange = jQuery("#highReportingRange");
        lowReportingRange.removeClass("error");
        lowReportingRangeValue = +lowReportingRange.val();
        if (lowReportingRangeValue != "-Infinity" &&
                lowReportingRangeValue != lowReportingRange.val()) {
            lowReportingRange.addClass("error");
            alert("<%=MessageUtil.getContextualMessage("error.out.side.range")%>");
            return;
        }
        highReportingRange.removeClass("error");
        highReportingRangeValue = +highReportingRange.val();
        if (highReportingRangeValue != "Infinity" &&
                highReportingRangeValue != highReportingRange.val()) {
            highReportingRange.addClass("error");
            alert("<%=MessageUtil.getContextualMessage("error.out.side.range")%>");
            return;
        }
        if (lowReportingRangeValue != "-Infinity" && highReportingRangeValue != "Infinity" &&
                lowReportingRangeValue >= highReportingRangeValue) {
            highReportingRange.addClass("error");
            lowReportingRange.addClass("error");
            alert("<%=MessageUtil.getContextualMessage("error.out.side.range")%>");
            return;
        }
        jQuery(".rowKey").each(function () {
            //index is in the template
            if (jQuery(this).val() != "index") {
                normalRangeCheck(jQuery(this).val());
            }
        });
    }
    function checkReadyForNextStep() {
        var ready = true;
        if (step == "step1") {
            jQuery("#step1Div .required").each(function () {
                if (!jQuery(this).val() || jQuery(this).val() == 0 || jQuery(this).val().length == 0) {
                    ready = false;
                }
            });
        } else if (step == "step2") {
            jQuery("#sampleTypeSelectionDiv .required").each(function () {
                if (!jQuery(this).val() || jQuery(this).val() == 0 || jQuery(this).val().length == 0) {
                    ready = false;
                }
            });
        } else if (step == "step3Dictionary") {
            jQuery("#testDisplayOrderDiv .required").each(function () {
                if (!jQuery(this).val() || jQuery(this).val() == 0 || jQuery(this).val().length == 0) {
                    ready = false;
                }
            });
        }

        jQuery("#nextButton").prop("disabled", !ready);
    }

    function nextStep() {
        var resultTypeId;

        if (step == 'step1') {
            step = 'step2';
            setStep1ReadOnlyFields();
            jQuery(".step1").hide();
            jQuery(".step2").show();
            jQuery("#nextButton").attr("disabled", "disabled");
            jQuery("#sampleTypeContainer").show();
        } else if (step == 'step2') {
            resultTypeId = jQuery("#resultTypeSelection").val();
            jQuery("#sortTitleDiv").attr("align", "left");
            jQuery("#step2BreadCrumb").hide();
            jQuery("#step2Guide").hide();
            makeSortListsReadOnly();
            if (resultTypeId == '<%= TypeOfTestResultServiceImpl.ResultType.ALPHA.getId()%>' ||
                    resultTypeId == '<%= TypeOfTestResultServiceImpl.ResultType.REMARK.getId()%>') {
                jQuery("#sampleTypeSelectionDiv").hide();
                jQuery("#sortTitleDiv").text("Sample type and test sort order");
                jQuery(".confirmShow").show();
                jQuery(".selectShow").hide();
                jQuery("#step2Confirm").show();
                createJSON();
            } else if (resultTypeId == '<%= TypeOfTestResultServiceImpl.ResultType.NUMERIC.getId() %>') {
                step = "step3Numeric";
                jQuery("#normalRangeDiv").show();
                jQuery("#sampleTypeSelectionDiv").hide();
                jQuery(".resultLimits").show();
                resetResultLimits();
            } else if (resultTypeId == '<%= TypeOfTestResultServiceImpl.ResultType.DICTIONARY.getId()%>' ||
                    resultTypeId == '<%= TypeOfTestResultServiceImpl.ResultType.MULTISELECT.getId()%>' ||
                    resultTypeId == '<%= TypeOfTestResultServiceImpl.ResultType.CASCADING_MULTISELECT.getId()%>') {
                step = 'step3Dictionary';
                jQuery("#sampleTypeSelectionDiv").hide();
                jQuery(".dictionarySelect").show();
                jQuery("#nextButton").attr("disabled", "disabled");
            }
        } else if (step == "step3Numeric") {
            jQuery("#normalRangeDiv input,select").attr("disabled", "disabled");
            jQuery(".confirmShow").show();
            jQuery(".selectShow").hide();
            jQuery(".resultLimits").hide();
            jQuery(".resultLimitsConfirm").show();
            createJSON();
        } else if (step == "step3Dictionary") {
            jQuery(".dictionarySelect").hide();
            jQuery("#sortDictionaryDiv").hide();
            buildVerifyDictionaryList();
            jQuery("#dictionaryVerifyId").show();
            jQuery("#referenceValue").text(jQuery("#referenceSelection option:selected").text());
            jQuery("#defaultTestResultValue").text(jQuery("#defaultTestResultSelection option:selected").text());
            jQuery(".selectListConfirm").show();
            jQuery(".confirmShow").show();
            jQuery(".selectShow").hide();
            createJSON();
        }
    }

    function buildVerifyDictionaryList() {
        var verifyList = jQuery("#dictionaryVerifyListId");
        var qualifyList = jQuery("#dictionaryQualify");
        var li, qualified;
        verifyList.empty();
        jQuery("#dictionaryNameSortUI li").each(function () {
            li = jQuery(document.createElement("li"));
            li.val(this.value);
            qualified = qualifyList.find("option[value=" + this.value + "]:selected").length == 1;
            li.append(jQuery(this).text() + (qualified ? "-- qualified" : ""));
            verifyList.append(li);
        });
    }
    function navigateBack() {
        jQuery(".confirmShow").hide();
        jQuery(".selectShow").show();
        if (step == 'step1') {
            submitAction('TestManagementConfigMenu');
        } else if (step == 'step2') {
            goBackToStep1();
        } else if ( step == 'step3Dictionary' || step == 'step3Numeric' ){
            goBackToStep2();
        }
    }

    function navigateBackFromConfirm() {
        jQuery(".confirmationBreadCrumb").hide();
        jQuery(".confirmShow").hide();
        jQuery(".selectShow").show();
        if (step == 'step2') {
            goBackToStep2();
        } else if (step == "step3Numeric") {
          goBackToResultLimits();
        } else if ( step == "step3Dictionary"){
          goBackToStep3Dictionary();
        }
    }

    function goBackToStep1() {
        step = 'step1';
        jQuery('.step2').hide();
        jQuery(".step1").show();
        jQuery(".confirmShow").hide();
        jQuery(".selectShow").show();
        jQuery(".dictionarySelect").hide();
        jQuery(".confirmationBreadCrumb").hide();
        jQuery("#nextButton").removeAttr("disabled");
        jQuery(".sortingMainDiv").remove();
        jQuery("#normalRangeDiv").hide();
        jQuery("#normalRangeDiv input,select").removeAttr("disabled");
        jQuery(".resultLimits").hide();
        jQuery("#sortTitleDiv").attr("align", "center");
        //The reason for the li is that the sample sortable UL is hardcoded as sortable, even if it has no contents
        if (jQuery(".sortable-tag li").length > 0) {
            jQuery(".sortable-tag").addClass("sortable");
            jQuery(".ui-state-default_oe-tag").addClass("ui-state-default_oe");
            jQuery(".sortable").sortable("enable");
        }
        jQuery("#sortTitleDiv").text('<spring:message code="label.test.display.order"/>');
        jQuery("#dictionaryVerifyId").hide();
        jQuery(".notStep1BreadCrumb").hide();

        clearMultiSelectContainer(jQuery("#sampleTypeSelectionDiv"));
        clearDictionaryLists();
    }

    function goBackToStep2() {
        step = "step2";
        jQuery(".step2").show();
        jQuery(".resultLimits").hide();
        jQuery("#sortTitleDiv").attr("align", "center");
        //The reason for the li is that the sample sortable UL is hardcoded as sortable, even if it has no contents
        if (jQuery(".sortable-tag li").length > 0) {
            jQuery(".sortable-tag").addClass("sortable");
            jQuery(".ui-state-default_oe-tag").addClass("ui-state-default_oe");
            jQuery(".sortable").sortable("enable");
        }
        jQuery("#sortTitleDiv").text('<spring:message code="label.test.display.order"/>');
        clearDictionaryLists();

        jQuery("#sampleTypeSelectionDiv").show();
        jQuery(".dictionarySelect").hide();
        jQuery("#dictioanryVerifyListId").hide();
        jQuery(".confirmationBreadCrumb").hide();
        jQuery("#nextButton").removeAttr("disabled");
        jQuery(".confirmShow").hide();
        jQuery(".selectShow").show();
        jQuery("#normalRangeDiv").hide();
        jQuery("#normalRangeDiv input,select").removeAttr("disabled");
        jQuery("#dictionaryVerifyId").hide();
    }

    function goBackToStep3Dictionary(){
        jQuery(".dictionarySelect").show();
        jQuery("#sortDictionaryDiv").show();
        jQuery("#dictionaryVerifyId").hide();
        jQuery(".confirmationBreadCrumb").hide();
        jQuery(".selectListConfirm").hide();
        jQuery(".confirmShow").hide();
        jQuery(".selectShow").show();
    }

    function goBackToResultLimits(){
        jQuery(".confirmShow").hide();
        jQuery(".selectShow").show();
        jQuery(".confirmationBreadCrumb").hide();
        jQuery("#dictionaryVerifyId").hide();
        jQuery("#normalRangeDiv").show();
        jQuery(".resultLimits").show();
        jQuery("#normalRangeDiv input,select").removeAttr("disabled");
        resetResultLimits();
    }

    function resetResultLimits(){
        genderMatersForRange(false,'0');
        jQuery("#normalRangeDiv .createdFromTemplate").remove();

        jQuery("#lowNormal_0").val("-Infinity");
        jQuery("#highNormal_0").val("Infinity");
        jQuery("#lowValid").val("-Infinity");
        jQuery("#highValid").val("Infinity");
        jQuery("#lowNormal_0").removeClass("error");
        jQuery("#highNormal_0").removeClass("error");
        jQuery("#lowReportingRange").val('-Infinity');
        jQuery("#highReportingRange").val('Infinity');
        jQuery("#significantDigits").val('');
        jQuery("#lowerAge_0").val('0');
        jQuery("#upperAge_0").text('');
        jQuery("#upperAgeSetter_0").val('Infinity');
        jQuery("#upperAgeSetter_0").show();
        jQuery("#ageRangeSelect_0").val(0);
        jQuery("#genderCheck_0").prop('checked', false);
    }
    function clearMultiSelectContainer(root) {
        root.find(".asmList li").remove();
        root.find(".asmSelect option:disabled").removeAttr("disabled");
        root.find(".asmSelect .asmOptionDisabled").removeClass("asmOptionDisabled");
        root.find(" option:selected").removeAttr("selected");
    }
    function setStep1ReadOnlyFields() {
        var panelNames = "";

        jQuery("#testNameEnglishRO").text(jQuery("#testNameEnglish").val());
        jQuery("#testNameFrenchRO").text(jQuery("#testNameFrench").val());
        jQuery("#testReportNameEnglishRO").text(jQuery("#testReportNameEnglish").val());
        jQuery("#testReportNameFrenchRO").text(jQuery("#testReportNameFrench").val());
        jQuery("#testSectionRO").text(jQuery("#testUnitSelection  option:selected").text());
        jQuery("#panelSelection option:selected").each(function () {
            panelNames += (jQuery(this).text()) + "\<br\>";
        });
        //we use append for optional
        jQuery("#panelRO").empty();
        jQuery("#panelRO").append(panelNames);
        jQuery("#uomRO").empty();
        jQuery("#uomRO").append(jQuery("#uomSelection  option:selected").text());
        jQuery("#loincRO").text(jQuery("#loinc").val());
        jQuery("#resultTypeRO").text(jQuery("#resultTypeSelection  option:selected").text());
        jQuery("#activeRO").text(jQuery("#active").attr("checked") ? "Y" : "N");
        jQuery("#orderableRO").text(jQuery("#orderable").attr("checked") ? "Y" : "N");
        jQuery("#notifyResultsRO").text(jQuery("#notifyResults").attr("checked") ? "Y" : "N");
        jQuery("#inLabOnlyRO").text(jQuery("#inLabOnly").attr("checked") ? "Y" : "N");
        jQuery("#antimicrobialResistanceRO").text(jQuery("#antimicrobialResistance").attr("checked") ? "Y" : "N");
    }

    function createJSON() {
        var jsonObj = {};
        jsonObj.testNameEnglish = jQuery("#testNameEnglish").val();
        jsonObj.testNameFrench = jQuery("#testNameFrench").val();
        jsonObj.testReportNameEnglish = jQuery("#testReportNameEnglish").val();
        jsonObj.testReportNameFrench = jQuery("#testReportNameFrench").val();
        jsonObj.testSection = jQuery("#testUnitSelection").val();
        jsonObj.panels = [];
        addJsonPanels(jsonObj);
        jQuery("#panelSelection").val();
        jsonObj.uom = jQuery("#uomSelection").val();
        jsonObj.loinc = jQuery("#loinc").val();
        jsonObj.resultType = jQuery("#resultTypeSelection").val();
        jsonObj.orderable = jQuery("#orderable").attr("checked") ? 'Y' : 'N';
        jsonObj.notifyResults = jQuery("#notifyResults").attr("checked") ? 'Y' : 'N';
        jsonObj.inLabOnly = jQuery("#inLabOnly").attr("checked") ? 'Y' : 'N';
        jsonObj.antimicrobialResistance = jQuery("#antimicrobialResistance").attr("checked") ? 'Y' : 'N';
        jsonObj.active = jQuery("#active").attr("checked") ? 'Y' : 'N';
        jsonObj.sampleTypes = [];
        addJsonSortingOrder(jsonObj);
        if (step == "step3Numeric") {
            addJsonResultLimits(jsonObj);
        } else if (step == "step3Dictionary") {
            addJsonDictionary(jsonObj);
        }

        console.log(JSON.stringify(jsonObj));
        jQuery("#jsonWad").val(JSON.stringify(jsonObj));
    }

    function addJsonPanels(jsonObj) {
        var panelSelections = jQuery("#panelSelection").val();
        var jsonPanel;
        jsonObj.panels = [];
        if (panelSelections) {
            panelSelections.each(function (value, index) {
                jsonPanel = {};
                jsonPanel.id = value;
                jsonObj.panels[index] = jsonPanel;
            });
        }
    }

    function addJsonSortingOrder(jsonObj) {
        var sampleTypes = jQuery("#sampleTypeSelection").val();
        var i, jsonSampleType, index, test;


        for (i = 0; i < sampleTypes.length; i++) {
            jsonSampleType = {};
            jsonSampleType.typeId = sampleTypes[i];
            jsonSampleType.tests = [];
            index = 0;
            jQuery("#" + sampleTypes[i] + " li").each(function () {
                test = {};
                test.id = jQuery(this).val();
                jsonSampleType.tests[index++] = test;
            });
            jsonObj.sampleTypes[i] = jsonSampleType;
        }
    }
    function addJsonResultLimits(jsonObj) {
        var rowIndex, limit, gender, yearMonth, upperAge;
        var countIndex = 0;

        jsonObj.lowValid = jQuery("#lowValid").val();
        jsonObj.highValid = jQuery("#highValid").val();
        jsonObj.lowReportingRange = jQuery("#lowReportingRange").val();
        jsonObj.highReportingRange = jQuery("#highReportingRange").val();
        jsonObj.lowCritical = jQuery("#lowCritical").val();
        jsonObj.highCritical = jQuery("#highCritical").val();

        jsonObj.significantDigits = jQuery("#significantDigits").val();
        jsonObj.resultLimits = [];


        jQuery("#normalRangeDiv .rowKey").each(function () {
            rowIndex = jQuery(this).val();
            gender = jQuery("#genderCheck_" + rowIndex).is(":checked");
            yearMonth = monthYear = jQuery(".yearMonthSelect_" + rowIndex + ":checked").val();
            limit = {};

            upperAge = jQuery("#upperAgeSetter_" + rowIndex).val();
            if (upperAge != "Infinity") {
                limit.highAgeRange = yearMonth == '<%=MessageUtil.getMessage("abbreviation.day.single")%>' ? upperAge : yearMonth == '<%=MessageUtil.getMessage("abbreviation.month.single")%>' ? Math.floor(upperAge * 365/12).toString() : (365 * upperAge).toString();
            } else {
                limit.highAgeRange = upperAge;
            }

            limit.gender = gender;
            limit.lowNormal = jQuery("#lowNormal_" + rowIndex).val();
            limit.highNormal = jQuery("#highNormal_" + rowIndex).val();

            if (gender) {
                limit.lowNormalFemale = jQuery("#lowNormal_G_" + rowIndex).val();
                limit.highNormalFemale = jQuery("#highNormal_G_" + rowIndex).val();
            }

            jsonObj.resultLimits[countIndex++] = limit;
        });

    }

    function addJsonDictionary(jsonObj) {
        var dictionary;
        jsonObj.dictionary = [];

        jQuery("#dictionarySelection option:selected").each(function (index, value) {
            if (jQuery("#referenceSelection option:selected[value=" + value.value + "]").length == 1) {
                jsonObj.dictionaryReference = value.value;
            }
            if (jQuery("#defaultTestResultSelection option:selected[value=" + value.value + "]").length == 1) {
                jsonObj.defaultTestResult = value.value;
            }
            dictionary = {};
            dictionary.value = value.value;
            dictionary.qualified = jQuery("#qualifierSelection option:selected[value=" + value.value + "]").length == 1 ? "Y" : "N";
            jsonObj.dictionary[index] = dictionary;
        });
    }
    function submitAction(target) {
        var form = document.getElementById("mainForm");
        form.action = target;
        form.submit();
    }


</script>
<br>


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

  <%    List<IdValuePair> sampleTypeList = (List<IdValuePair>) pageContext.getAttribute("sampleTypeList"); %>
  <%    List<IdValuePair> panelList = (List<IdValuePair>) pageContext.getAttribute("panelList"); %>
  <%    List<IdValuePair> uomList = (List<IdValuePair>) pageContext.getAttribute("uomList"); %>
  <%    List<IdValuePair> resultTypeList = (List<IdValuePair>) pageContext.getAttribute("resultTypeList"); %>
  <%    List<IdValuePair> testUnitList = (List<IdValuePair>) pageContext.getAttribute("testUnitList"); %>
  <%    List<IdValuePair> ageRangeList = (List<IdValuePair>) pageContext.getAttribute("ageRangeList"); %>
  <%    List<IdValuePair> dictionaryList = (List<IdValuePair>) pageContext.getAttribute("dictionaryList"); %>
  <%    List<IdValuePair> groupedDictionaryList = (List<IdValuePair>) pageContext.getAttribute("groupedDictionaryList"); %>

    <form:hidden id="jsonWad" name='${form.formName}' path="jsonWad"/>

    <input type="button" value="<%= MessageUtil.getContextualMessage("banner.menu.administration") %>"
           onclick="submitAction('MasterListsPage');"
           class="textButton"/> &rarr;
    <input type="button" value="<%= MessageUtil.getContextualMessage("configuration.test.management") %>"
           onclick="submitAction('TestManagementConfigMenu');"
           class="textButton"/>&rarr;
    <span class="step1">
            <spring:message code="configuration.test.add"/>
    </span>
    <span class="step2 notStep1BreadCrumb" id="step2BreadCrumb" style="display: none">
        <input type="button" value="<%= MessageUtil.getContextualMessage("configuration.test.add") %>"
               onclick="goBackToStep1();"
               class="textButton"/>&rarr;
        <%=MessageUtil.getContextualMessage("label.selectSampleType")%>
    </span>
    <span id="step2Confirm notStep1BreadCrumb" class="confirmationBreadCrumb" style="display: none">
        <input type="button" value="<%= MessageUtil.getMessage("configuration.test.add") %>"
               onclick="goBackToStep1();"
               class="textButton"/>&rarr;
        <input type="button" value="<%=MessageUtil.getContextualMessage("label.selectSampleType")%>"
               onclick="goBackToStep2();"
               class="textButton"/>&rarr;
        <spring:message code="label.confirmation" />
    </span>
    <span class="dictionarySelect notStep1BreadCrumb" style="display : none" >
        <input type="button" value="<%= MessageUtil.getContextualMessage("configuration.test.add") %>"
               onclick="goBackToStep1();"
               class="textButton"/>&rarr;
        <input type="button" value="<%=MessageUtil.getContextualMessage("label.selectSampleType")%>"
               onclick="goBackToStep2();"
               class="textButton"/>&rarr;
        <spring:message code="label.select.list.values" />
    </span>
     <span class="resultLimits notStep1BreadCrumb" style="display : none" >
        <input type="button" value="<%= MessageUtil.getContextualMessage("configuration.test.add") %>"
               onclick="goBackToStep1();"
               class="textButton"/>&rarr;
         <input type="button" value="<%=MessageUtil.getContextualMessage("label.selectSampleType")%>"
                onclick="goBackToStep2();"
                class="textButton"/>&rarr;
         <spring:message code="label.set.result.limits" />
    </span>
    <span class="selectListConfirm confirmationBreadCrumb notStep1BreadCrumb" style="display : none" >
        <input type="button" value="<%= MessageUtil.getContextualMessage("configuration.test.add") %>"
               onclick="goBackToStep1();"
               class="textButton"/>&rarr;
        <input type="button" value="<%=MessageUtil.getContextualMessage("label.selectSampleType")%>"
           onclick="goBackToStep2();"
           class="textButton"/>&rarr;
        <input type="button" value="<%=MessageUtil.getContextualMessage("label.select.list.values")%>"
               onclick="goBackToStep3Dictionary();"
               class="textButton"/>&rarr;
        <spring:message code="label.confirmation" />
    </span>
    <span class="resultLimitsConfirm confirmationBreadCrumb" style="display : none" >
        <input type="button" value="<%= MessageUtil.getContextualMessage("configuration.test.add") %>"
               onclick="goBackToStep1();"
               class="textButton"/>&rarr;
        <input type="button" value="<%=MessageUtil.getContextualMessage("label.selectSampleType")%>"
               onclick="goBackToStep2();"
               class="textButton"/>&rarr;
        <input type="button" value="<%=MessageUtil.getContextualMessage("label.set.result.limits")%>"
               onclick="goBackToResultLimits();"
               class="textButton"/>&rarr;
        <spring:message code="label.confirmation" />
    </span>


    <h3><spring:message code="configuration.test.add"/></h3>

    <input type="checkbox" onchange="guideSelection(this)"><spring:message code="label.show.guide" /><br/><br/>

    <div id="guide" style="display: none">
        <span class="requiredlabel">*</span> <spring:message code="label.required.field" /><br/><br/>
        <span class="step1">
            <spring:message htmlEscape="false" code="configuration.test.add.guide.test" />
        </span>
        <span class="step2" id="step2Guide" style="display: none">
           <spring:message htmlEscape="false"  code="configuration.test.add.guide.sample.type" />
        </span>
        <span class="dictionarySelect" style="display: none" >
           <spring:message htmlEscape="false"  code="configuration.test.add.guide.select.list" />
        </span>
        <span class="resultLimits" style="display: none;">
            <spring:message htmlEscape="false"  code="configuration.test.add.guide.result.limits" />
        </span>
        <span class="confirmShow" style="display: none">
            <spring:message htmlEscape="false"  code="configuration.test.add.guide.verify" />
        </span>
        <br/>
        <hr/>
    </div>


    <div id="step1Div" class="step1">
        <table width="80%">
            <tr>
                <td width="25%">
                    <table>
                        <tr>
                            <td colspan="2"><spring:message code="test.testName"/><span class="requiredlabel">*</span></td>
                        </tr>
                        <tr>
                            <td width="25%" align="right"><spring:message code="label.english"/></td>
                            <td width="75%"><input type="text" id="testNameEnglish" class="required"
                                                   onchange="checkReadyForNextStep()"/></td>
                        </tr>
                        <tr>
                            <td width="25%" align="right"><spring:message code="label.french"/></td>
                            <td width="75%"><input type="text" id="testNameFrench" class="required"
                                                   onchange="checkReadyForNextStep()"/></td>
                        </tr>
                        <tr>
                            <td>&nbsp;</td>
                        </tr>
                        <tr>
                            <td colspan="2"><spring:message code="test.testName.reporting"/><span
                                    class="requiredlabel">*</span></td>
                        </tr>
                        <tr>
                            <td></td>
                            <td><input type="button" onclick="copyFromTestName(); checkReadyForNextStep()"
                                       value='<%= MessageUtil.getContextualMessage("test.add.copy.name")%>'></td>
                        </tr>
                        <tr>
                            <td width="25%" align="right"><spring:message code="label.english"/></td>
                            <td width="75%"><input type="text" id="testReportNameEnglish" class="required"
                                                   onchange="checkReadyForNextStep()"/></td>
                        </tr>
                        <tr>
                            <td width="25%" align="right"><spring:message code="label.french"/></td>
                            <td width="75%"><input type="text" id="testReportNameFrench" class="required"
                                                   onchange="checkReadyForNextStep()"/></td>
                        </tr>
                    </table>
                </td>
                <td width="25%" style="vertical-align: top; padding: 4px">
                    <spring:message code="test.testSectionName"/><span class="requiredlabel">*</span><br/>
                    <select id="testUnitSelection" class="required" onchange="checkReadyForNextStep()">
                        <option value="0"></option>
                        <% for (IdValuePair pair : testUnitList) { %>
                        <option value='<%=pair.getId()%>'><%=pair.getValue()%>
                        </option>
                        <% } %>
                    </select>
                </td>
                <td width="25%" style="vertical-align: top; padding: 4px" id="panelSelectionCell">
                    <spring:message code="typeofsample.panel.panel"/><br/>
                    <select id="panelSelection" multiple="multiple" title="Multiple">
                        <% for (IdValuePair pair : panelList) { %>
                        <option value='<%=pair.getId()%>'><%=pair.getValue()%>
                        </option>
                        <% } %>
                    </select><br/><br/><br/>
                    <spring:message code="label.unitofmeasure"/><br/>
                    <select id="uomSelection">
                        <option value='0'></option>
                        <% for (IdValuePair pair : uomList) { %>
                        <option value='<%=pair.getId()%>'><%=pair.getValue()%>
                        </option>
                        <% } %>
                    </select>
                </td>
                <td width="25%" style="vertical-align: top; padding: 4px">
                    <spring:message code="result.resultType"/><span class="requiredlabel">*</span><br/>
                    <select id="resultTypeSelection" class="required" onchange="checkReadyForNextStep()">
                        <option value="0"></option>
                        <% for (IdValuePair pair : resultTypeList) { %>
                        <option value='<%=pair.getId()%>'><%=pair.getValue()%>
                        </option>
                        <% } %>
                    </select> 
                    <br /> 
                    <br/>
                    <br/>
                    <spring:message code="label.loinc" /><br />
					<form:input path="loinc" type="text" id="loinc"
					onchange="checkReadyForNextStep()" />
					<br/>
                    <br/><br/>
                    <label for="antimicrobialResistance"><spring:message code="test.antimicrobialResistance"/></label>
                    <input type="checkbox" id="antimicrobialResistance" /><br/>
                    <label for="orderable"><spring:message code="test.isActive"/></label>
                    <input type="checkbox" id="active" checked="checked"/><br/>
                    <label for="orderable"><spring:message code="label.orderable"/></label>
                    <input type="checkbox" id="orderable" checked="checked"/><br/>
                    <label for="notifyResults"> <spring:message code="test.notifyResults" /></label>
                    <input type="checkbox" id='notifyResults'/><br/>
                    <label for="inLabOnly"> <spring:message code="test.inLabOnly" /></label>
                    <input type="checkbox" id='inLabOnly'/><br/>
                </td>
            </tr>
        </table>
    </div>
    <div id="sampleTypeContainer" class="step2" style="width: 100%; overflow: hidden; display : none">
        <div  style="float:left;  width:20%;">
            <spring:message code="test.testName"/><br/>
            <span class="tab"><spring:message code="label.english"/>: <span id="testNameEnglishRO"></span></span><br/>
            <span class="tab"><spring:message code="label.french"/>: <span id="testNameFrenchRO"></span></span><br/>
            <br/>
            <spring:message code="test.testName.reporting"/><br/>
            <span class="tab"><spring:message code="label.english"/>: <span
                    id="testReportNameEnglishRO"></span></span><br/>
                <span class="tab"><spring:message code="label.french"/>: <span
                        id="testReportNameFrenchRO"></span></span><br/>
            <br/>
            <spring:message code="test.testSectionName"/>
            <div id="testSectionRO" class="tab"></div>
            <br/>
            <spring:message code="typeofsample.panel.panel"/>
            <div class="tab" id="panelRO"><spring:message code="label.none"/></div>
            <br/>
            <spring:message code="label.unitofmeasure"/>
            <div class="tab" id="uomRO"><spring:message code="label.none"/></div>
            <br/>
            <spring:message code="label.loinc" />
			<div class="tab" id="loincRO">
				<spring:message code="label.none" />
			</div>
			<br />
            <spring:message code="result.resultType"/>
            <div class="tab" id="resultTypeRO"></div>
            <br/>

            <spring:message code="test.antimicrobialResistance"/>
            <div class="tab" id="antimicrobialResistanceRO"></div>
            <br/>

            <spring:message code="test.isActive"/>
            <div class="tab" id="activeRO"></div>
            <br/>
            <spring:message code="label.orderable"/>
            <div class="tab" id="orderableRO"></div>
            <br/>
            <spring:message code="test.notifyResults"/>
            <div class="tab" id="notifyResultsRO"></div>
            <br/>
            <spring:message code="test.inLabOnly"/>
            <div class="tab" id="inLabOnlyRO"></div>
            <br/>
        </div>
        <div class="step2" style="float:right;  width:80%; display: none">
            <div  id="sampleTypeSelectionDiv" class="step2" style="float:left; width:20%;">
                <spring:message code="label.sampleType"/><span class="requiredlabel">*</span>
                <select id="sampleTypeSelection" class="required" multiple="multiple" title="Multiple">
                    <% for (IdValuePair pair : sampleTypeList) { %>
                    <option value='<%=pair.getId()%>'><%=pair.getValue()%>
                    </option>
                    <% } %>
                </select><br/>
            </div>
            <div id="testDisplayOrderDiv" style="float:left; width:80%;">
                <div id="sortTitleDiv" align="center"><spring:message code="label.test.display.order"/></div>
                <div id="endOrderMarker"></div>
                <div class="dictionarySelect dictionaryMultiSelect" id="dictionarySelectId"
                     style="padding:10px; float:left; width:280px; display:none; overflow: hidden ">
                    <spring:message code="label.select.list.options"/><span class="requiredlabel">*</span><br/>
                    <select id="dictionarySelection" class="required" multiple="multiple" title="Multiple">
                        <% for (IdValuePair pair : dictionaryList) { %>
                        <option value='<%=pair.getId()%>'><%=pair.getValue()%>
                        </option>
                        <% } %>
                    </select><br/><br/><br/>
                </div>
                <div id="dictionaryVerifyId"
                     style="padding:10px; float:left; width:280px; display:none; overflow: hidden;">
                    <span><span class="half-tab"><spring:message code="label.select.list" /></span><br/>
                    <ul id="dictionaryVerifyListId">

                    </ul>
                    </span>
                    <span><spring:message code="label.reference.value" /><br><ul><li id="referenceValue"></li></ul></span>
                	<span><spring:message code="label.default.result" /><br><ul><li id="defaultTestResultValue"></li></ul></span>
                </div>
                <div id="sortDictionaryDiv" align="center" class="dictionarySelect"
                     style="padding:10px;float:left; width:33%; display:none;"><spring:message code="label.result.order" />
                    <span id="dictionarySortSpan" align="left">
                        <UL id="dictionaryNameSortUI">

                        </UL>
                    </span></div>
                <div class="dictionarySelect" style="padding:10px; float:left; width:280px;display:none">
                    <div id="dictionaryReference">
                        <spring:message code="label.reference.value" /><br/>
                        <select id='referenceSelection'>
                            <option value="0">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</option>
                        </select>
                    </div>
                    <br>
					<div id="defaultTestResult">
                        <spring:message code="label.default.result" /><br/>
                        <select id='defaultTestResultSelection'>
                            <option value="0">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</option>
                        </select>
                    </div>
                    <br>

                    <div id="dictionaryQualify" class="dictionaryMultiSelect">
                        <spring:message code="label.qualifiers" /><br/>
                        <select id='qualifierSelection' multiple='multiple' title='Multiple'></select>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <div id="dictionaryExistingGroups" class="dictionarySelect" style="display:none; width:100%">
        <div style="width:100%; text-align:center;"><spring:message code="label.existing.test.sets"/></div>
        <hr>
        <table>
            <% while (testCount < groupedDictionaryList.size()) {%>
            <tr>
                <td id='<%= "dictionaryGroup_" + testCount%>' style="padding: 5px 10px; vertical-align: top">
                    <input type="button" value="<%=MessageUtil.getContextualMessage("label.form.select")%>" onclick="<%="dictionarySetSelected(" + testCount + ");" %>"
                           class="textButton"/>
                    <ul style="padding-left:0; list-style-type: none">
                        <% for (IdValuePair pair : (Iterable<IdValuePair>) groupedDictionaryList.get(testCount)) { %>
                        <li value="<%=pair.getId()%>"><%=pair.getValue()%>
                        </li>
                        <% } %>
                    </ul>
                    <%
                        testCount++;
                        columnCount = 1;
                    %></td>
                <% while (testCount < groupedDictionaryList.size() && (columnCount < columns)) {%>
                <td id='<%= "dictionaryGroup_" + testCount%>' style="padding: 5px 10px; vertical-align: top">
                    <input type="button" value="<%=MessageUtil.getContextualMessage("label.form.select")%>" onclick="<%="dictionarySetSelected(" + testCount + ");" %>"
                           class="textButton"/>
                    <ul style="padding-left:0; list-style-type: none">
                        <% for (IdValuePair pair : (Iterable<IdValuePair>) groupedDictionaryList.get(testCount)) { %>
                        <li value="<%=pair.getId()%>"><%=pair.getValue()%>
                        </li>
                        <% } %>
                    </ul>
                </td>
                    <%
                        testCount++;
                        columnCount++;
                    %>
                <% } %>

            </tr>
            <% } %>
        </table>
    </div>
    <div id="normalRangeTemplate" style="display:none;">
        <table>
            <tr class="row_index createdFromTemplate">
                <td><input type="hidden" class="rowKey" value="index"/><input id="genderCheck_index" type="checkbox"
                                                                              onchange="genderMatersForRange(this.checked, 'index')">
                </td>
                <td>
                        <span class="sexRange_index" style="display: none">
                            <spring:message code="sex.male" />
                        </span>
                </td>
                <td style="white-space:nowrap;"><input class="yearMonthSelect_index" type="radio" name="time_index" value="<%=MessageUtil.getContextualMessage("abbreviation.year.single")%>"
                           onchange="upperAgeRangeChanged( 'index' )" checked><spring:message code="abbreviation.year.single" />
                    <input class="yearMonthSelect_index" type="radio" name="time_index" value="<%=MessageUtil.getContextualMessage("abbreviation.month.single")%>"
                           onchange="upperAgeRangeChanged( 'index' )"><spring:message code="abbreviation.month.single" />
                    <input class="yearMonthSelect_index" type="radio" name="time_index" value="<%=MessageUtil.getContextualMessage("abbreviation.day.single")%>"
                           onchange="upperAgeRangeChanged('index')"> <spring:message code="abbreviation.day.single" />&nbsp;</td>

                           
                <td id="lowerAge_index">0</td>
                <td><input type="text" id="upperAgeSetter_index" value="Infinity" size="10"
                           onchange="upperAgeRangeChanged( 'index' )"><span id="upperAge_index"></span></td>
                <td>
                    <select id="ageRangeSelect_index" onchange="ageRangeSelected( this, 'index');">
                        <option value="0"></option>
                        <% for (IdValuePair pair : ageRangeList) { %>
                        <option value='<%=pair.getId()%>'><%=pair.getValue()%>
                        </option>
                        <% } %>
                    </select>
                </td>
                <td><input type="text" value="-Infinity" size="10" id="lowNormal_index" class="lowNormal"
                           onchange="normalRangeCheck('index');"></td>
                <td><input type="text" value="Infinity" size="10" id="highNormal_index" class="highNormal"
                           onchange="normalRangeCheck('index');"></td>
                
                <td></td>
                <td></td>
                <td></td>
                <td><input id="removeButton_index" type="button" class="textButton" onclick='removeLimitRow( index );'
                           value="<%=MessageUtil.getContextualMessage("label.remove")%>"/></td>
            </tr>
            <tr class="sexRange_index row_index createdFromTemplate">
                <td></td>
                <td><spring:message code="sex.female" /></td>
                <td></td>
                <td></td>
                <td></td>
                <td></td>
                <td><input type="text" value="-Infinity" size="10" id="lowNormal_G_index" class="lowNormal"
                           onchange="normalRangeCheck('index');"></td>
                <td><input type="text" value="Infinity" size="10" id="highNormal_G_index" class="highNormal"
                           onchange="normalRangeCheck('index');"></td>
                <td></td>
                <td></td>
                <td></td>
            </tr>
        </table>
    </div>
    <div id="normalRangeDiv" style="display:none;">
        <h3><spring:message code="label.range" /></h3>
        <table style="display:inline-table">
            <tr>
                <th colspan="6"><spring:message code="label.age.range" /></th>
                <th colspan="2"><spring:message code="configuration.test.catalog.normal.range" /></th>
                <th colspan="2"><spring:message code="label.reporting.range" /> </th>
                 <th colspan="2"><spring:message code="configuration.test.catalog.valid.range" /> </th>
                 <th colspan="4"><spring:message code="configuration.test.catalog.critical.range" /> </th>
            </tr>
            <tr>
                <td><spring:message code="label.sex.dependent" /></td>
                <td><span class="sexRange" style="display: none"><spring:message code="label.sex" /> </span></td>
                <td colspan="4" align="center"></td>
                <td colspan="2" align="center"></td>
                <td colspan="2" align="center"></td>
                <td colspan="2"></td>
            </tr>
            <tr class="row_0">
                <td><input type="hidden" class="rowKey" value="0"/><input id="genderCheck_0" type="checkbox"
                                                                          onchange="genderMatersForRange(this.checked, '0')">
                </td>
                <td>
                        <span class="sexRange_0" style="display: none">
                            <spring:message code="sex.male" />
                        </span>
                </td>
                <td style="white-space:nowrap;" ><input class="yearMonthSelect_0" type="radio" name="time_0" value="<%=MessageUtil.getContextualMessage("abbreviation.year.single")%>"
                           onchange="upperAgeRangeChanged('0')" checked><spring:message code="abbreviation.year.single" />
                    <input class="yearMonthSelect_0" type="radio" name="time_0" value="<%=MessageUtil.getContextualMessage("abbreviation.month.single")%>"
                           onchange="upperAgeRangeChanged('0')"><spring:message code="abbreviation.month.single" />
                    <input class="yearMonthSelect_0" type="radio" name="time_0" value="<%=MessageUtil.getContextualMessage("abbreviation.day.single")%>"
                           onchange="upperAgeRangeChanged('0')"><spring:message code="abbreviation.day.single" />
                           &nbsp;
                </td>
                <td id="lowerAge_0">0&nbsp;</td>
                <td><input type="text" id="upperAgeSetter_0" value="Infinity" size="10"
                           onchange="upperAgeRangeChanged('0')"><span id="upperAge_0"></span></td>
                <td>
                    <select id="ageRangeSelect_0" onchange="ageRangeSelected( this, '0');">
                        <option value="0"></option>
                        <% for (IdValuePair pair : ageRangeList) { %>
                        <option value='<%=pair.getId()%>'><%=pair.getValue()%>
                        </option>
                        <% } %>
                    </select>
                </td>
                <td><input type="text" value="-Infinity" size="10" id="lowNormal_0" class="lowNormal"
                           onchange="normalRangeCheck('0');"></td>
                <td><input type="text" value="Infinity" size="10" id="highNormal_0" class="highNormal"
                           onchange="normalRangeCheck('0');"></td>
                <td><input type="text" value="-Infinity" size="10" id="lowReportingRange" onchange="reportingRangeCheck();"></td>
                <td><input type="text" value="Infinity" size="10" id="highReportingRange" onchange="reportingRangeCheck();"></td>

                <td><input type="text" value="-Infinity" size="10" id="lowValid" onchange="validRangeCheck();"></td>
                <td><input type="text" value="Infinity" size="10" id="highValid" onchange="validRangeCheck();"></td>

                <td><input type="text" value="-Infinity" size="5" id="lowCritical" onchange="criticalRangeCheckLow('0');"></td>
                <td><input type="text" value="-Infinity" size="5" id="highCritical" onchange="criticalRangeCheckHigh('0');"></td>
            </tr>
            <tr class="sexRange_0 row_0" style="display: none">
                <td></td>
                <td><spring:message code="sex.female" /></td>
                <td></td>
                <td></td>
                <td></td>
                <td></td>
                <td><input type="text" value="-Infinity" size="10" id="lowNormal_G_0" class="lowNormal"
                           onchange="normalRangeCheck('0');"></td>
                <td><input type="text" value="Infinity" size="10" id="highNormal_G_0" class="highNormal"
                           onchange="normalRangeCheck('0');"></td>
                <td></td>
                <td></td>
                <td></td>
            </tr>
            <tr id="endRow"></tr>
        </table>
        <label for="significantDigits"><spring:message code="label.significant.digits" /></label>
        <input type="number" min="0" max="10" id="significantDigits">
    </div>


    <div class="selectShow" style="margin-left:auto; margin-right:auto;width: 40%; ">
        <input type="button"
               value="<%= MessageUtil.getContextualMessage("label.button.next") %>"
               disabled="disabled"
               onclick="nextStep();"
               id="nextButton"/>
        <input type="button" value="<%=MessageUtil.getContextualMessage("label.button.back")%>" onclick="navigateBack()"/>
    </div>
    <div class="confirmShow" style="margin-left:auto; margin-right:auto;width: 40%; display: none">
        <input type="button"
               value="<%= MessageUtil.getContextualMessage("label.button.accept") %>"
               onclick="submitAction('TestAdd');"/>
        <input type="button" value="<%=MessageUtil.getContextualMessage("label.button.back")%>"
               onclick="navigateBackFromConfirm()"/>
    </div>
</form:form>
