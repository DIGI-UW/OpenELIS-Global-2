<%@ page language="java" contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	import="java.util.List, java.util.Locale,
                 org.openelisglobal.testconfiguration.beans.TestCatalogBean"%>
<%@ page import="org.openelisglobal.common.util.IdValuePair"%>
<%@ page import="org.openelisglobal.internationalization.MessageUtil"%>
<%@ page import="org.openelisglobal.common.util.Versioning"%>
<%@ page
	import="org.openelisglobal.common.util.ConfigurationProperties"%>
<%@ page
	import="org.openelisglobal.common.util.ConfigurationProperties.Property"%>
<%@ page
	import="org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl"%>
<%@ page
	import="org.openelisglobal.common.provider.query.EntityNamesProvider"%>
<%@ page
	import="org.openelisglobal.testconfiguration.beans.ResultLimitBean"%>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<%@ taglib prefix="ajax" uri="/tags/ajaxtags" %>
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
String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
%>
<%--Do not add jquery.ui.js, it will break the sorting --%>
<script type="text/javascript"
	src="scripts/jquery.asmselect.js?"></script>
<script type="text/javascript"
	src="scripts/ajaxCalls.js?"></script>
<script type="text/javascript"
	src="scripts/multiselectUtils.js?"></script>
<script type="text/javascript"
	src="scripts/jquery-ui.js?"></script>
<link rel="stylesheet" type="text/css"
	href="css/jquery.asmselect.css?" />
<link rel="stylesheet" media="screen" type="text/css"
	href="css/jquery_ui/jquery.ui.theme.css?" />
<link rel="stylesheet" type="text/css"
	href="css/openElisCore.css?" />


<script type="text/javascript"
	src="scripts/ajaxCalls.js?"></script>	
<script type="text/javascript"
	src="scripts/utilities.js"></script>


<%--
<bean:define id="testList" name='${form.formName}' property="testList"
	type="java.util.List<IdValuePair>" />

<bean:define id="sampleTypeList" name='${form.formName}'
	property="sampleTypeList" type="java.util.List<IdValuePair>" />
<bean:define id="panelList" name='${form.formName}' property="panelList"
	type="java.util.List<IdValuePair>" />
<bean:define id="uomList" name='${form.formName}' property="uomList"
	type="java.util.List<IdValuePair>" />
<bean:define id="resultTypeList" name='${form.formName}'
	property="resultTypeList" type="java.util.List<IdValuePair>" />
<bean:define id="testUnitList" name='${form.formName}'
	property="labUnitList" type="java.util.List<IdValuePair>" />
<bean:define id="ageRangeList" name='${form.formName}'
	property="ageRangeList" type="java.util.List<IdValuePair>" />
<bean:define id="dictionaryList" name='${form.formName}'
	property="dictionaryList" type="java.util.List<IdValuePair>" />
<bean:define id="groupedDictionaryList" name='${form.formName}'
	property="groupedDictionaryList"
	type="java.util.List<java.util.List<IdValuePair>>" />
<bean:define id="testCatBeanList" name='${form.formName}'
	property="testCatBeanList" type="List<TestCatalogBean>" />
 --%>
 
<c:set var="testList" value="${form.testList}" />
<c:set var="sampleTypeList" value="${form.sampleTypeList}" />
<c:set var="panelList" value="${form.panelList}" />
<c:set var="uomList" value="${form.uomList}" />
<c:set var="resultTypeList" value="${form.resultTypeList}" />
<c:set var="testUnitList" value="${form.labUnitList}" />
<c:set var="ageRangeList" value="${form.ageRangeList}" />
<c:set var="dictionaryList" value="${form.dictionaryList}" />
<c:set var="groupedDictionaryList" value="${form.groupedDictionaryList}" />
<c:set var="testCatBeanList" value="${form.testCatBeanList}" />

<%
	int testCount = 0;
	int columnCount = 0;
	int columns = 3;
%>

	<script type="text/javascript">
	var valueChanged = true;
	
    function makeDirty(){
        window.onbeforeunload = "<spring:message code="banner.menu.dataLossWarning"/>";
    }

    function setForEditing(testId, name) {
    	jQuery("#testId").val(testId);
    	jQuery("#catDiv").show();
        jQuery("#testName").text(name);
        jQuery(".error").each(function (index, value) {
            value.value = "";
            jQuery(value).removeClass("error");
            jQuery(value).removeClass("confirmation");
        });
        
        jQuery(".test").each(function () {
            var element = jQuery(this);
            element.prop("disabled", "disabled");
            element.addClass("disabled-text-button");
        });
        
        var resultType = "";
        jQuery(".resultClass").each(function (i,elem) {
        	if(testId !== jQuery(elem).attr("fTestId")){    		
        		jQuery(elem).remove();
        	} else {
				// from type_of_test_result table, translate to DisplayListService
        		switch(jQuery(elem).attr('fresulttype')) {
        	    case "R": 
        	    	jQuery("#resultTypeSelection").val(1) 
        	    	break;
        	    case "D": 
        	    	jQuery("#resultTypeSelection").val(2) 
        	    	break;
        	    case "T": 
        	    	jQuery("#resultTypeSelection").val(3) 
        	    	break;
        	    case "N": 
        	    	jQuery("#resultTypeSelection").val(4) 
        	    	break;
        	    case "A": 
        	    	jQuery("#resultTypeSelection").val(5) 
        	    	break;
        	    case "M": 
        	    	jQuery("#resultTypeSelection").val(6) 
        	    	break;
        	    case "C": 
        	    	jQuery("#resultTypeSelection").val(7) 
        	    	break;
        	    default:
        		}
				
				var panel = jQuery(elem).attr('fPanel');
				var optionsArray = Array.from(jQuery("#panelSelection")[0]);
				var panelOption = optionsArray.filter(function(elem) { return elem.label === panel });
				panelOption.forEach(function(elem) {
					jQuery(elem).attr("selected", true)
				});

				jQuery("#notifyResults").prop('checked', jQuery(elem).attr('fNotifyResults') === 'true');
				jQuery("#inLabOnly").prop('checked', jQuery(elem).attr('fInLabOnly') === 'true');
				jQuery("#antimicrobialResistance").prop('checked', jQuery(elem).attr('fantimicrobialResistance') === 'true');

				jQuery("#panelSelection").change();
							
        	}
        });
        
        getTestNames(testId, testNameSuccess);
		getTestEntities(testId, testEntitiesSuccess);
		jQuery("#nextButton").prop("disabled", false);	
		jQuery("#step1Div").show();
    }
   
    function testEntitiesSuccess(xhr) {
        //alert(xhr.responseText);
        var formField = xhr.responseXML.getElementsByTagName("formfield").item(0);
        var message = xhr.responseXML.getElementsByTagName("message").item(0);
        var response;
        var testSectionId = "";
        var uomId = "";

        if (message.firstChild.nodeValue == "valid") {
            response = JSON.parse(formField.firstChild.nodeValue);
            testSectionId = response["entities"]["testSectionId"];
            uomId = response["entities"]["uomId"];
            
         	//getEntityNames(testSectionId, "<%=EntityNamesProvider.TEST_SECTION%>", testSectionNameSuccess );
         	//getEntityNames(uomId, "<%=EntityNamesProvider.UNIT_OF_MEASURE%>", uomNameSuccess );
            
            jQuery("#loinc").val(response["entities"]["loinc"]);
            jQuery("#testUnitSelection").val(response["entities"]["testSectionId"]);
            jQuery("#uomSelection").val(response["entities"]["uomId"]);
        }
        
        window.onbeforeunload = null;
        jQuery("#step1Div").show();
    }
    
    function uomNameSuccess(xhr) {
        //alert(xhr.responseText);
        var formField = xhr.responseXML.getElementsByTagName("formfield").item(0);
        var message = xhr.responseXML.getElementsByTagName("message").item(0);
        var response;

        if (message.firstChild.nodeValue == "valid") {
            response = JSON.parse(formField.firstChild.nodeValue);
            jQuery("#uomEnglish").text(response["name"]["english"]);
            jQuery(".required").each(function () {
                jQuery(this).val("");
            });
        }

        window.onbeforeunload = null;
    }
    
    function testSectionNameSuccess(xhr) {
        //alert(xhr.responseText);
        var formField = xhr.responseXML.getElementsByTagName("formfield").item(0);
        var message = xhr.responseXML.getElementsByTagName("message").item(0);
        var response;

        if (message.firstChild.nodeValue == "valid") {
            response = JSON.parse(formField.firstChild.nodeValue);
            jQuery("#testSectionEnglish").text(response["name"]["english"]);
            jQuery("#testSectionFrench").text(response["name"]["french"]);
            jQuery(".required").each(function () {
                jQuery(this).val("");
            });
        }

        window.onbeforeunload = null;
    }

    function testNameSuccess(xhr) {
        //alert(xhr.responseText);
        var formField = xhr.responseXML.getElementsByTagName("formfield").item(0);
        var message = xhr.responseXML.getElementsByTagName("message").item(0);
        var response;

        if (message.firstChild.nodeValue == "valid") {
            response = JSON.parse(formField.firstChild.nodeValue);
            
            jQuery("#testNameEnglish").val(response["name"]["english"]);
            
            jQuery("#testNameFrench").val(response["name"]["french"]);
            
            jQuery("#testReportNameEnglish").val(response["reportingName"]["english"]);
            
            jQuery("#testReportNameFrench").val(response["reportingName"]["french"]);
            
            //alert(response["reportingName"]["french"]);
            //jQuery(".required").each(function () {
              //  jQuery(this).val("");
            //});
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
        form.action = "TestModifyEntry";
        form.submit();
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
    
    var step = "step1";
    var currentNormalRangeIndex = 1;
    var maxAgeInMonths = 0;
    var useResultLimitDefault = true;
    var defaultResultLimits = null;

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
    	if(typeof data === 'undefined') return;
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

  	
    function dictionarySetDefaultForEditing(valuesArray) {
    	valuesArray.forEach(function(value) {
            var dictionarySelect = jQuery("#dictionarySelectId .asmSelect option[value=" + value + "]");
            dictionarySelect.attr("selected", "selected");
            dictionarySelect.trigger('change');
    	});
    }

  	
    function dictionarySetDefault(valuesArray) {  
        var dictionaryOption;
        clearDictionaryLists();
        var optionArray = Array.from(jQuery("#dictionarySelection")[0]);
        
        for(var i = 0; i < valuesArray.length; i++) {
            jQuery("#dictionarySelection").add
        	dictionaryOption = optionArray.filter(function(elem) { return elem.label === valuesArray[i] });
        	
        	for(var j=dictionaryOption.length; j>0; j--) {
        		dictionaryOption.splice(j,1);
        	}

            
        	dictionaryOption.forEach(function(inner) {
            	jQuery(inner).attr("selected", "selected")
                jQuery("#dictionarySelection").change();
        	});
        	

        }
        jQuery("#dictionarySelection").change();
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

    function copyFromTestName() {
        jQuery("#testReportNameEnglish").val(jQuery("#testNameEnglish").val());
        jQuery("#testReportNameFrench").val(jQuery("#testNameFrench").val());
    }

    function testForSampleTypeSuccess(xhr) {
        //alert(xhr.responseText);
        var response = xhr.responseXML.getElementsByTagName("formfield").item(0);
        var tests = response.getElementsByTagName("test");
        var sampleTypeId = getValueFromXmlElement(response, "sampleTypeId");
        var test, name, modName, id, modId;
        var ul = jQuery(document.createElement("ul"));
        var length = tests.length;
        var testId = jQuery("#testId").val() 
        ul.addClass("sortable sortable-tag");

        for (var i = 0; i < length; ++i) {
            test = tests[i];
            name = getValueFromXmlElement(test, "name");
            
            <%if (locale.equals("en_US") || locale.equals("en")) {%>
            modName = jQuery("#testNameEnglish").val();
            <%} else {%>
            modName = jQuery("#testNameFrench").val();
            <%}%>
            
            id = getValueFromXmlElement(test, "id");
            if (id != testId ) {
            	ul.append(createLI(id, name, false));
            } else {
            	ul.append(createLI(id, modName, true));
            }
        }

<%--         <%if (locale.equals("en_US") || locale.equals("en")) {%> --%>
//         	ul.append( createLI(modId, jQuery("#testNameEnglish").val(), true) );
<%--         <%} else { %> --%>
//         	ul.append( createLI(modId, jQuery("#testNameFrench").val(), true) );
<%--         <% } %> --%>

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

<%--                 newMonthValue = monthYear == '<%=MessageUtil.getMessage("abbreviation.month.single")%>' ? highAgeValue : 12 * highAgeValue; --%>
                newDayValue = monthYear == '<%=MessageUtil.getMessage("abbreviation.day.single")%>' ? highAgeValue : monthYear == '<%=MessageUtil.getMessage("abbreviation.month.single")%>' ? Math.floor(highAgeValue * 365/12) : 365 * highAgeValue;
<%--                 newDayValue = monthYear == '<%=MessageUtil.getMessage("abbreviation.day.single")%>' ? highAgeValue : monthYear == '<%=MessageUtil.getMessage("abbreviation.month.single")%>' ? Math.floor(highAgeValue * 30.44) : Math.floor(365.25 * highAgeValue); --%>

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
        var lowNormalValue, highNormalValue, lowValidValue, highValidValue,lowReportingRangeValue,highReportingRangeValue;
        var lowGenderNormalValue, highGenderNormalValue;
        var lowGenderNormal, highGenderNormal;
        var lowNormal = jQuery("#lowNormal_" + index);
        var highNormal = jQuery("#highNormal_" + index);
        var lowValid = jQuery("#lowValid");
        var lowReportingRange = jQuery("#lowReportingRange");
        var highReportingRange = jQuery("#highReportingRange");
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

        //below we are testing against the valid and reporting range values
        lowValidValue = +lowValid.val();
        if (lowValidValue != "-Infinity" &&
                lowValidValue != lowValid.val()) {
            return;
        }

        lowReportingRangeValue = +lowReportingRange.val();
        if (lowReportingRangeValue != "-Infinity" &&
                lowReportingRangeValue != lowReportingRange.val()) {
            return;
        }

        highValidValue = +highValid.val();
        if (highValidValue != "Infinity" &&
                highValidValue != highValid.val()) {
            return;
        }

        highReportingRangeValue = +highReportingRange.val();
        if (highReportingRangeValue != "-Infinity" &&
                highReportingRangeValue != highReportingRange.val()) {
            return;
        }


        if (lowValidValue || lowReportingRange == "-Infinity" && highValidValue || highReportingRangeValue == "Infinity") {
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

        if (highReportingRangeValue != "Infinity" && highNormalValue > highReportingRangeValue) {
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

    function lowCriticalRangeCheck() {
        var lowCriticalRangeHighValue, lowCriticalRangeLowValue;
        var lowCriticalRangeLow = jQuery("#lowCriticalRangeLow");
        var lowCriticalRangeHigh = jQuery("#lowCriticalRangeHigh");

        lowCriticalRangeLow.removeClass("error");
        lowCriticalRangeLowValue = +lowCriticalRangeLow.val();
        if (lowCriticalRangeLowValue != "-Infinity" &&
          lowCriticalRangeLowValue != lowCriticalRangeLow.val()) {
            lowCriticalRangeLow.addClass("error");
            alert("<%=MessageUtil.getContextualMessage("error.low.valid.value")%>");
            return;
        }

        lowCriticalRangeHigh.removeClass("error");
        lowCriticalRangeHighValue = +lowCriticalRangeHigh.val();
        if (lowCriticalRangeHighValue != "Infinity" &&
           lowCriticalRangeHighValue != lowCriticalRangeHigh.val()) {
            lowCriticalRangeHigh.addClass("error");
            alert("<%=MessageUtil.getContextualMessage("error.high.valid.value")%>");
            return;
        }

        jQuery(".rowKey").each(function () {
            //index is in the template
            if (jQuery(this).val() != "index") {
                lowCriticalRangeCheck(jQuery(this).val());
            }
        });
    }

    function highCriticalRangeCheck() {
        var highCriticalRangeHighValue, highCriticalRangeLowValue;
        var highCriticalRangeLow = jQuery("#highCriticalRangeLow");
        var highCriticalRangeHigh = jQuery("#highCriticalRangeHigh");

        highCriticalRangeLow.removeClass("error");
        highCriticalRangeLowValue = +highCriticalRangeLow.val();
        if (highCriticalRangeLowValue != "-Infinity" &&
          highCriticalRangeLowValue != highCriticalRangeLow.val()) {
            highCriticalRangeLow.addClass("error");
            alert("<%=MessageUtil.getContextualMessage("error.low.valid.value")%>");
            return;
        }

        highCriticalRangeHigh.removeClass("error");
        highCriticalRangeHighValue = +highCriticalRangeHigh.val();
        if (highCriticalRangeHighValue != "Infinity" &&
          highCriticalRangeHighValue != highCriticalRangeHigh.val()) {
            highCriticalRangeHigh.addClass("error");
            alert("<%=MessageUtil.getContextualMessage("error.high.valid.value")%>");
            return;
        }

        jQuery(".rowKey").each(function () {
            //index is in the template
            if (jQuery(this).val() != "index") {
                lowCriticalRangeCheck(jQuery(this).val());
            }
        });
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
    
    function editRangeAsk(){
    	//alert(step);
    	step = 'step3NumericAsk';
    	useResultLimitDefault = false;
    	nextStep();
    }
    
    function editDictionaryAsk(){
    	//alert(step);
    	step = 'step3DictionaryAsk';
    	nextStep();
    }

    function nextStep() {
        var resultTypeId;
        var testId = jQuery("#testId").val() 
		//alert(step);
        if (step == 'step1') {
            step = 'step2';
            setStep1ReadOnlyFields();
            jQuery(".step1").hide();
            jQuery(".step2").show();
            jQuery("#nextButton").attr("disabled", "disabled");
            jQuery("#sampleTypeContainer").show();
            
            var sampleType = null;
            var sampleTypeId = null;
            
            jQuery(".resultClass").each(function (i,elem) {
            	sampleType = jQuery(elem).attr("fSampleType")
            });
            
			var optionsArray = Array.from(jQuery("#sampleTypeSelection")[0]);
			var sampleTypeOption = optionsArray.filter(function(elem) { return elem.label === sampleType });
			sampleTypeOption.forEach(function(elem) {
				jQuery(elem).attr("selected", true)
				sampleTypeId = jQuery(elem).attr("value");
			});
			
			var sampleTypeData = {value:sampleTypeId, type:"add"};
			//Triggered manualy, non-event. Added test for undefined event object in createOrderBoxForSampleType
			//because "change" triggers extra call
			if (sampleTypeId != null) {
	            createOrderBoxForSampleType(sampleTypeData);
				jQuery("#sampleTypeSelection").change();
			}
			jQuery("#nextButton").prop("disabled", false);	

        } else if (step == 'step2') {
        	var significantDigits = null;
			var dictionaryValues = null;
			var dictionaryIds = null;
			var referenceValue = null;
			var referenceId = null;

            jQuery(".resultClass").each(function (i,elem) {
            	significantDigits = jQuery(elem).attr("fSignificantDigits")
            	dictionaryIds = jQuery(elem).attr("fDictionaryIds")
            	dictionaryValues = jQuery(elem).attr("fDictionaryValues")
            	referenceValue = jQuery(elem).attr("fReferenceValue")
            	referenceId = jQuery(elem).attr("fReferenceId")
            });
            
            if (dictionaryValues !== null && dictionaryValues !== "null") {
                var tmpArray = dictionaryValues.split("[");
                var tmpArray = tmpArray[1].split("]");
                var tmpArray = tmpArray[0].split(", ");
                var valuesArray = jQuery.makeArray(tmpArray);
                dictionarySetDefault(valuesArray);
            }
            
            resultTypeId = jQuery("#resultTypeSelection").val();
            jQuery("#sortTitleDiv").attr("align", "left");
            jQuery("#step2BreadCrumb").hide();
            jQuery("#step2Guide").hide();
            makeSortListsReadOnly();
            if (resultTypeId == '<%=TypeOfTestResultServiceImpl.ResultType.ALPHA.getId()%>' ||
                    resultTypeId == '<%=TypeOfTestResultServiceImpl.ResultType.REMARK.getId()%>') {
                jQuery("#sampleTypeSelectionDiv").hide();
                jQuery("#sortTitleDiv").text("Sample type and test sort order");
                jQuery(".confirmShow").show();
                jQuery(".selectShow").hide();
                jQuery("#step2Confirm").show();
                createJSON(); 
            } else if (resultTypeId == '<%=TypeOfTestResultServiceImpl.ResultType.NUMERIC.getId()%>') {
            		step = "step3Numeric";
            		jQuery("#significantDigits").val(significantDigits);
                    jQuery("#normalRangeAskDiv").show();
                    jQuery("#editResultLimitsButton").show();
                    jQuery("#sampleTypeSelectionDiv").hide();
            } else if (resultTypeId == '<%=TypeOfTestResultServiceImpl.ResultType.DICTIONARY.getId()%>' ||
                    resultTypeId == '<%=TypeOfTestResultServiceImpl.ResultType.MULTISELECT.getId()%>' ||
                    resultTypeId == '<%=TypeOfTestResultServiceImpl.ResultType.CASCADING_MULTISELECT.getId()%>') {
                step = 'step3Dictionary';
                jQuery("#sampleTypeSelectionDiv").hide();
                jQuery("#dictionaryAskDiv").show();
            }
        } else if (step == "step3NumericAsk") {
        	step = "step3Numeric";
        	jQuery("#normalRangeDiv").show();
            jQuery("#sampleTypeSelectionDiv").hide();
            jQuery(".resultLimits").show();
    		getTestResultLimits(testId, testResultLimitsSuccess);
        } else if (step == "step3Numeric") {
        	
        	var defaultLimitsString = null;
        	defaultLimitsString = jQuery("#fLimit").val();
        	console.log("defaultLimitsString:" + defaultLimitsString);
        	defaultResultLimits = buildResultLimitesFromDefault(defaultLimitsString);
        	
            jQuery("#normalRangeDiv input,select").attr("disabled", "disabled");
            jQuery(".confirmShow").show();
            jQuery(".selectShow").hide();
            jQuery(".resultLimits").hide();
            jQuery(".resultLimitsConfirm").show();
            
            createJSON();
            
        } else if (step == "step3DictionaryAsk") {
        	step = 'step3Dictionary';
        	jQuery("#dictionaryAskDiv").hide();
        	jQuery("#dictionaryVerifyId").hide();
        	jQuery(".dictionarySelect").show();
        	
        	var dictionaryValues = null;
        	var dictionaryIds = null;
        	var referenceValue = null;
        	var referenceId = null;
            jQuery(".resultClass").each(function (i,elem) {
            	dictionaryValues = jQuery(elem).attr("fDictionaryValues")
            	dictionaryIds = jQuery(elem).attr("fDictionaryIds")
            	referenceValue = jQuery(elem).attr("fReferenceValue")
            	referenceId = jQuery(elem).attr("fReferenceId")
            });

            if (dictionaryValues !== null && dictionaryValues !== "null") {
	            var tmpArray = dictionaryIds.split("[");
	            var tmpArray = tmpArray[1].split("]");
	            var tmpArray = tmpArray[0].split(", ");
	            var valuesArray = jQuery.makeArray(tmpArray);
	            dictionarySetDefaultForEditing(valuesArray);
            }
            
            if (referenceValue == "n/a") {
                jQuery("#referenceValue").text(referenceValue);
            } else {
            	jQuery("#referenceValue").text(jQuery(referenceValue).text());
            }
            jQuery("#referenceId").text(jQuery(referenceId).text());
            
        } else if (step == "step3Dictionary") {
        	jQuery("#dictionaryAskDiv").hide();
            jQuery(".dictionarySelect").hide();
            jQuery("#sortDictionaryDiv").hide();
           
            
            var dictionaryValues = null;
        	var dictionaryIds = null;
        	var referenceValue = null;
        	var referenceId = null;
            jQuery(".resultClass").each(function (i,elem) {
            	dictionaryValues = jQuery(elem).attr("fDictionaryValues")
            	dictionaryIds = jQuery(elem).attr("fDictionaryIds")
            	referenceValue = jQuery(elem).attr("fReferenceValue")
            	referenceId = jQuery(elem).attr("fReferenceId")
            });
            
            if(jQuery("#referenceSelection option").length == 1) {
            	//console.log("dict default:" + dictionaryValues);
            	jQuery("#referenceValue").text(referenceValue);
            	buildVerifyDictionaryListFromDefault(dictionaryValues);
            } else {
            	console.log("dict group:");
            	jQuery("#referenceValue").text(jQuery("#referenceSelection option:selected").text());
            	buildVerifyDictionaryList();
            }
            jQuery("#defaultTestResultValue").text(jQuery("#defaultTestResultSelection option:selected").text());
            
            jQuery("#dictionaryVerifyId").show();
            jQuery(".selectListConfirm").show();
            jQuery(".confirmShow").show();
            jQuery(".selectShow").hide();
            createJSON();
        }

        if (valueChanged) {
        	jQuery("#acceptButton").prop('disabled', false);
        } else {
        	jQuery("#acceptButton").prop('disabled', true);
        }
    }
    
    jQuery(".dictionarySelect").hide();
    jQuery("#sortDictionaryDiv").hide();
    
    function buildResultLimitesFromDefault(defaultLimitsString) {
    	var resultLimits = [];
    	
    	var gender = null;
    	var age = [];
    	var ageLow = null; var ageHigh = null;
    	var normal = [];
    	var normalLow = null; var normalHigh = null;
    	var valid = [];
    	var validLow = null; var validHigh = null;
        var report = [];
        var reportLow = null; var reportHigh = null;
		
    	var tmpArray = defaultLimitsString.split("|");
    	
    	for (var i = 0; i < tmpArray.length-1; i++) {
                if (tmpArray[i].split(",").length < 4) { continue; }
    		    var tmpRangeArray = tmpArray[i].split(",");
    			gender = tmpRangeArray[0];
    			
    			var lowHigh = tmpRangeArray[1].split("-");
    			ageLow = lowHigh[0]; 
    			ageHigh = (lowHigh.length == 2) ? lowHigh[1] : "Infinity";
    			age = [ ageLow, ageHigh];
    			
    			var normalLowHigh = tmpRangeArray[2].split("-");
    			normalLow = (normalLowHigh.length == 2) ? normalLowHigh[0] : "-Infinity";
    			normalHigh = (normalLowHigh.length == 2) ? normalLowHigh[1] : "Infinity";
    			normal = [normalLow, normalHigh];

    			var validLowHigh = tmpRangeArray[3].split("-");
    			validLow = (validLowHigh.length == 2) ? validLowHigh[0] : "-Infinity";
    			validHigh = (validLowHigh.length == 2) ? validLowHigh[1] : "Infinity";
    			valid = [validLow, validHigh];

                var reportLowHigh = tmpRangeArray[4].split("-");
    			reportLow = (reportLowHigh.length == 2) ? reportLowHigh[0] : "-Infinity";
    			reportHigh = (reportLowHigh.length == 2) ? reportLowHigh[1] : "Infinity";
    			report = [reportLow, reportHigh];
    			
    			resultLimits.push([gender, age, normal, valid ,report]);
    	}

        return resultLimits;
    }
    
    function buildVerifyDictionaryListFromDefault(dictionaryValues) {
        var verifyList = jQuery("#dictionaryVerifyListId");
        var qualifyList = jQuery("#dictionaryQualify");
        var li, qualified;
        if (dictionaryValues !== null && dictionaryValues !== "null") {
	        var tmpArray = dictionaryValues.split("[");
	    	var tmpArray = tmpArray[1].split("]");
	    	var tmpArray = tmpArray[0].split(", ");
	    	dictionaryValues = jQuery.makeArray(tmpArray);
        } else {
        	dictionaryValues = [];
        }
        verifyList.empty();
        for(var i=0; i < dictionaryValues.length; i++ ) {
            li = jQuery(document.createElement("li"));
            li.val(dictionaryValues[i]);
            //qualified = qualifyList.find("option[value=" + this.value + "]:selected").length == 1;
            //li.append(jQuery(this).text() + (qualified ? "-- qualified" : ""));
            li.append(dictionaryValues[i]);
            verifyList.append(li);
        };
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
        	window.location.href = "TestManagementConfigMenu";
        } else if (step == 'step2') {
            goBackToStep1();
        } else if ( step == 'step3Dictionary' || step == 'step3Numeric' || step == 'step3NumericAsk' || step == 'step3DictionaryAsk'){
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
        jQuery("#editResultLimitsButton").hide();
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
        jQuery("#dictionaryAskDiv").hide();
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
		getTestResultLimits(testId, testResultLimitsSuccess);
    }

    function doLims(item, index){
    	//alert(item,index);
    	var items = item.split(",");
    	alert(items);
    	var gender = items[0];
    	
    	var age = items[1];
    	var ages = age.split("-");
    	var ageLow = ages[0];
    	var ageHigh = ages[1];
    	
    	var normal = items[2];
    	var normals = normal.split("-");
    	var normalLow = normals[0];
    	var normalHigh = normals[1];
    	
    	var valid = items[3];
    	var valids = valid.split("-");
    	var validLow = valids[0];
    	var validHigh = valids[1];
    	
    	//alert(gender, age, normalLow, normalHigh, )
    	//ageRangeSelected();
    }
    
    function testResultLimitsSuccess(xhr) {
        var formField = xhr.responseXML.getElementsByTagName("formfield").item(0);
        var message = xhr.responseXML.getElementsByTagName("message").item(0);
//     	alert(formField.firstChild.nodeValue);
//     	alert(message.firstChild.nodeValue);
        
        resultLimitsJson = JSON.parse(formField.firstChild.nodeValue).resultLimits;
        resetResultLimits();
        
        var row = 0;
        for ( var x=0; x < resultLimitsJson.length ; ++x ) {
            var resultLimit = resultLimitsJson[x];

            var highValid = resultLimit.highValid;
            var lowValid = resultLimit.lowValid;
            var highNormal = resultLimit.highNormal;
            var lowNormal = resultLimit.lowNormal;
            var gender = resultLimit.gender;
            var minAge = resultLimit.minAge;
            var maxAge = resultLimit.maxAge == null ? 'Infinity' : resultLimit.maxAge;
            
            if (isNullOrWhitespace(gender)) {
            	jQuery('#highValid').val(highValid);
            	jQuery('#lowValid').val(lowValid);
            	jQuery('#highNormal_' + row).val(highNormal);
            	jQuery('#lowNormal_' + row).val(lowNormal);
            	
            	jQuery('input:radio[name=time_' + row + ']:nth(2)').attr('checked',true);
            	jQuery('#upperAgeSetter_' + row).val(maxAge);
            	upperAgeRangeChanged(row);
            	++row;
            } else if (gender == 'M') {
            	jQuery('#genderCheck_' + row).prop('checked', true);
            	genderMatersForRange(true, row)
            	jQuery('#highValid').val(highValid);
            	jQuery('#lowValid').val(lowValid);
            	jQuery('#highNormal_' + row).val(highNormal);
            	jQuery('#lowNormal_' + row).val(lowNormal);
            } else if (gender == 'F') {
            	jQuery('#highValid').val(highValid);
            	jQuery('#lowValid').val(lowValid);
            	jQuery('#highNormal_G_' + row).val(highNormal);
            	jQuery('#lowNormal_G_' + row).val(lowNormal);
            	
            	jQuery('input:radio[name=time_' + row + ']:nth(2)').attr('checked',true);
            	jQuery('#upperAgeSetter_' + row).val(maxAge);
            	upperAgeRangeChanged(row);
            	++row;
            }
            
            
        }
    }
    
    function setResultLimits(){
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
        //jQuery("#significantDigits").val('');
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
        jsonObj.active = jQuery("#active").attr("checked") ? 'Y' : 'N';
		jsonObj.antimicrobialResistance = jQuery("#antimicrobialResistance").attr("checked") ? 'Y' : 'N';

        jQuery(".resultClass").each(function (i,elem) {
        	jsonObj.testId = jQuery(elem).attr('fTestId');
            console.log("createJSON: " + jQuery(elem).attr('fTestId') + ":" + jQuery(elem).attr('fResultType'));
        });
        
        jsonObj.sampleTypes = [];
        addJsonSortingOrder(jsonObj);
        
        if (step == "step3Numeric") {
        	if(useResultLimitDefault) {
            	addJsonResultLimitsFromDefault(jsonObj);
            	useResultLimitDefault = false;
        	} else {
        		addJsonResultLimits(jsonObj);
        	}
        } else if (step == "step3Dictionary") {
        	
            addJsonDictionary(jsonObj);
        
        	console.log(JSON.stringify(jsonObj.dictionary));
        	//dictionary from defaults if empty
        	if(JSON.stringify(jsonObj.dictionary) == "[]") {
        		console.log(JSON.stringify(jsonObj.dictionaryReference));
        		var significantDigits = null;
				var dictionaryValues = null;
				var referenceValue = null;
				var dictionaryIds = null;
				var referenceId = null;

           	 	jQuery(".resultClass").each(function (i,elem) {
            		significantDigits = jQuery(elem).attr("fSignificantDigits")
            		dictionaryValues = jQuery(elem).attr("fDictionaryValues")
            		referenceValue = jQuery(elem).attr("fReferenceValue")
            		dictionaryIds = jQuery(elem).attr("fDictionaryIds")
            		referenceId = jQuery(elem).attr("fReferenceId")
            	});
            	//console.log("Values:" + dictionaryValues + "::::" + referenceValue);
            	//console.log("Ids:" + dictionaryIds + "::::" + referenceId);
            	//dictionary by id and reference by id required
            	var valuesArray = dictionaryValues.split("[");
        		var valuesArray = valuesArray[1].split("]");
        		var valuesArray = valuesArray[0].split(", ");
        		
        		var idsArray = dictionaryIds.split("[");
        		var idsArray = idsArray[1].split("]");
        		var idsArray = idsArray[0].split(", ");
            	
            	for(var i=0; i < idsArray.length; i++) {
            		dictionary = {};
                	dictionary.value = idsArray[i];
                	dictionary.qualified = "N";
                	jsonObj.dictionary[i] = dictionary;
        		}
            	if (referenceId == "null") {
            		referenceId = null;
            	}
            	jsonObj.dictionaryReference = referenceId;
        	}
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

		if (sampleTypes != null) {
	        for (i = 0; i < sampleTypes.length; i++) {
	            jsonSampleType = {};
	            jsonSampleType.typeId = sampleTypes[i];
	            jsonSampleType.tests = [];
	            index = 0;
	            jQuery("#" + sampleTypes[i] + " li").each(function () {
	            	//console.log("addJsonSortingOrder: " + jsonObj.testId + ":" + jQuery(this).val());
	            	//if (jsonObj.testId != jQuery(this).val()){
	            	if (true){
	                	test = {};
	                	test.id = jQuery(this).val();
	                	jsonSampleType.tests[index++] = test;
	            	}
	            });
	            jsonObj.sampleTypes[i] = jsonSampleType;
	        }
        }
    }
    
    function addJsonResultLimitsFromDefault(jsonObj) {
    	//gnr, global defaultResultLimits
        if(defaultResultLimits.length == 0){
            return ;
        }
    	
    	for (var i = 0; i < defaultResultLimits.length; i++) {
        		console.log("addJsonResultLimitsFromDefault:defaultResultLimits:" + i + ":" + defaultResultLimits[i]);
        }
        var rowIndex, limit, gender, yearMonth, upperAge;
        var countIndex = 0;

        jsonObj.lowValid = defaultResultLimits[0][3][0];
        jsonObj.highValid = defaultResultLimits[0][3][1];
        jsonObj.lowReportingRange = defaultResultLimits[0][4][0];
        jsonObj.highReportingRange = defaultResultLimits[0][4][1];
        jsonObj.resultLimits = [];

        for (var rowIndex = 0; rowIndex < defaultResultLimits.length; rowIndex++) {
            
            //yearMonth = monthYear = jQuery(".yearMonthSelect_" + rowIndex + ":checked").val();
            //yearMonth = 'D'; // always month regardless
            limit = {};

            upperAge = defaultResultLimits[rowIndex][1][1];
            if (upperAge != "Infinity") {
                // 0D/0M/0Y
                 var ageLimitArray = upperAge.split("/");
                 var days = ageLimitArray[0].slice(0, -1);
                 var months = ageLimitArray[1].slice(0, -1);
                 var years = ageLimitArray[2].slice(0, -1);
                 var totalDays = 0;
                  if(days > 0){
                     totalDays = days ;
                  }
                  if(months > 0){
                      totalDays = Math.floor(months * 365/12); 
                  }

                  if(years > 0){
                     totalDays = years * 365 ;
                  }
            	 limit.highAgeRange = (totalDays).toString();
            } else {
                limit.highAgeRange = "Infinity";
            }
            
            if( defaultResultLimits[rowIndex][0] == "n/a" ){
            	limit.gender = false;
            } else {
            	limit.gender = true;
            }
            
            limit.lowNormal = defaultResultLimits[rowIndex][2][0];
            limit.highNormal = defaultResultLimits[rowIndex][2][1];

            if (limit.gender) {
                limit.lowNormalFemale = defaultResultLimits[rowIndex][2][0];
                limit.highNormalFemale = defaultResultLimits[rowIndex][2][1];
                //limit.reportingRangeFemale = not used
                rowIndex++; // m/f in same json object so skip row
            }

            jsonObj.resultLimits[countIndex++] = limit;
        }

    }
    
    function addJsonResultLimits(jsonObj) {
        var rowIndex, limit, gender, yearMonth, upperAge;
        var countIndex = 0;

        jsonObj.lowValid = jQuery("#lowValid").val();
        jsonObj.highValid = jQuery("#highValid").val();
        jsonObj.lowReportingRange = jQuery("#lowReportingRange").val();
        jsonObj.highReportingRange = jQuery("#highReportingRange").val();
        jsonObj.significantDigits = jQuery("#significantDigits").val();
        jsonObj.resultLimits = [];

        jQuery("#normalRangeDiv .rowKey").each(function () {
            rowIndex = jQuery(this).val();
            gender = jQuery("#genderCheck_" + rowIndex).is(":checked");
            yearMonth = monthYear = jQuery(".yearMonthSelect_" + rowIndex + ":checked").val();
            limit = {};

            upperAge = jQuery("#upperAgeSetter_" + rowIndex).val();
            if (upperAge != "Infinity") {
                //limit.highAgeRange = yearMonth == "<%=MessageUtil.getContextualMessage("abbreviation.year.single")%>" ? (upperAge * 365).toString() : upperAge; --%>
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
        console.log(jQuery("jsonWad").val()); 
        form.action = target;
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
				   
  <%    List<IdValuePair> testUnitList = (List<IdValuePair>) pageContext.getAttribute("testUnitList"); %>
  <%    List<IdValuePair> panelList = (List<IdValuePair>) pageContext.getAttribute("panelList"); %>
  <%    List<IdValuePair> uomList = (List<IdValuePair>) pageContext.getAttribute("uomList"); %>
  <%    List<IdValuePair> resultTypeList = (List<IdValuePair>) pageContext.getAttribute("resultTypeList"); %>
  <%    List<IdValuePair> dictionaryList = (List<IdValuePair>) pageContext.getAttribute("dictionaryList"); %>
  <%    List<IdValuePair> sampleTypeList = (List<IdValuePair>) pageContext.getAttribute("sampleTypeList"); %>
  <%    List<IdValuePair> ageRangeList = (List<IdValuePair>) pageContext.getAttribute("ageRangeList"); %>
  <%    List<IdValuePair> testList = (List<IdValuePair>) pageContext.getAttribute("testList"); %>
  
	<form:hidden id="jsonWad" name='${form.formName}' path="jsonWad" />

	<input type="button"
		value="<%=MessageUtil.getContextualMessage("banner.menu.administration")%>"
		onclick="submitAction('MasterListsPage');" class="textButton" />
	&rarr; <input type="button"
		value="<%=MessageUtil.getContextualMessage("configuration.test.management")%>"
		onclick="submitAction('TestManagementConfigMenu');"
		class="textButton" />&rarr; <span class="step1"> <spring:message code="configuration.test.modify" />
	</span> <span class="step2 notStep1BreadCrumb" id="step2BreadCrumb"
		style="display: none"> <input type="button"
		value="<%=MessageUtil.getContextualMessage("configuration.test.modify")%>"
		onclick="goBackToStep1();" class="textButton" />&rarr; <%=MessageUtil.getContextualMessage("label.selectSampleType")%>
	</span> <span id="step2Confirm notStep1BreadCrumb"
		class="confirmationBreadCrumb" style="display: none"> <input
		type="button"
		value="<%=MessageUtil.getContextualMessage("configuration.test.modify")%>"
		onclick="goBackToStep1();" class="textButton" />&rarr; <input
		type="button"
		value="<%=MessageUtil.getContextualMessage("label.selectSampleType")%>"
		onclick="goBackToStep2();" class="textButton" />&rarr; <spring:message code="label.confirmation" />
	</span> <span class="dictionarySelect notStep1BreadCrumb"
		style="display: none"> <input type="button"
		value="<%=MessageUtil.getContextualMessage("configuration.test.modify")%>"
		onclick="goBackToStep1();" class="textButton" />&rarr; <input
		type="button"
		value="<%=MessageUtil.getContextualMessage("label.selectSampleType")%>"
		onclick="goBackToStep2();" class="textButton" />&rarr; <spring:message code="label.select.list.values" />
	</span> <span class="resultLimits notStep1BreadCrumb" style="display: none">
		<input type="button"
		value="<%=MessageUtil.getContextualMessage("configuration.test.modify")%>"
		onclick="goBackToStep1();" class="textButton" />&rarr; <input
		type="button"
		value="<%=MessageUtil.getContextualMessage("label.selectSampleType")%>"
		onclick="goBackToStep2();" class="textButton" />&rarr; <spring:message code="label.set.result.limits" />
	</span> <span
		class="selectListConfirm confirmationBreadCrumb notStep1BreadCrumb"
		style="display: none"> <input type="button"
		value="<%=MessageUtil.getContextualMessage("configuration.test.modify")%>"
		onclick="goBackToStep1();" class="textButton" />&rarr; <input
		type="button"
		value="<%=MessageUtil.getContextualMessage("label.selectSampleType")%>"
		onclick="goBackToStep2();" class="textButton" />&rarr; <input
		type="button"
		value="<%=MessageUtil.getContextualMessage("label.select.list.values")%>"
		onclick="goBackToStep3Dictionary();" class="textButton" />&rarr; <spring:message code="label.confirmation" />
	</span> <span class="resultLimitsConfirm confirmationBreadCrumb"
		style="display: none"> <input type="button"
		value="<%=MessageUtil.getContextualMessage("configuration.test.modify")%>"
		onclick="goBackToStep1();" class="textButton" />&rarr; <input
		type="button"
		value="<%=MessageUtil.getContextualMessage("label.selectSampleType")%>"
		onclick="goBackToStep2();" class="textButton" />&rarr; <input
		type="button"
		value="<%=MessageUtil.getContextualMessage("label.set.result.limits")%>"
		onclick="goBackToResultLimits();" class="textButton" />&rarr; <spring:message code="label.confirmation" />
	</span>

	<form:hidden path="testId" name="${form.formName}" id="testId" />

	<div id="catDiv" style="display: none">
		<h1 id="action">
			<spring:message code="label.button.edit" />
		</h1>

		<h2><%=MessageUtil.getContextualMessage("sample.entry.test")%>:<span
				id="testName"></span>
		</h2>
        <%    List<TestCatalogBean> testCatBeanList = (List<TestCatalogBean>) pageContext.getAttribute("testCatBeanList"); %>
		<%
			for (TestCatalogBean bean : testCatBeanList) {
		%>
		<table>
			<tbody  fTestId='<%=bean.getId()%>' 
					fResultType='<%=bean.getResultType()%>'
					fPanel='<%=bean.getPanel()%>' 
					fSampleType='<%=bean.getSampleType()%>'
					fSignificantDigits='<%=bean.getSignificantDigits()%>'
					fDictionaryValues='<%=bean.getDictionaryValues()%>'
					fDictionaryIds='<%=bean.getDictionaryIds()%>'
					fReferenceValue='<%=bean.getReferenceValue()%>'
					fReferenceId='<%=bean.getReferenceId()%>'
					fNotifyResults='<%=bean.getNotifyResults()%>'
					fInLabOnly='<%=bean.getInLabOnly()%>'
					fantimicrobialResistance='<%=bean.getAntimicrobialResistance()%>'
				class='resultClass'>
				
				<tr>
					<td colspan="2"><span class="catalog-label"><spring:message code="configuration.test.catalog.name" /></span></td>
					<td colspan="2"><span class="catalog-label"><spring:message code="configuration.test.catalog.report.name" /></span></td>
				</tr>
				<%
					int i = 0;
					while (i < bean.getLocalization().getAllActiveLocales().size()) {
						Locale locale1 = bean.getLocalization().getAllActiveLocales().get(i++); 
						Locale locale2 = null;
						if (i < bean.getLocalization().getAllActiveLocales().size()) {
							locale2 = bean.getLocalization().getAllActiveLocales().get(i++); 
						}
						if (locale2 != null) {
					%>
				<tr>
					<td width="25%"><span class="catalog-label"><%=locale1.getLanguage() %>.</span> <b><%=bean.getLocalization().getLocalizedValue(locale1) %></b>
					</td>
					<td width="25%"><span class="catalog-label"><%=locale2.getLanguage() %>.</span> <b><%=bean.getLocalization().getLocalizedValue(locale2) %></b>
					</td>
					<td width="25%"><span class="catalog-label"><%=locale1.getLanguage() %>.</span> <b><%=bean.getReportLocalization().getLocalizedValue(locale1) %></b>
					</td>
					<td width="25%"><span class="catalog-label"><%=locale2.getLanguage() %>.</span> <b><%=bean.getReportLocalization().getLocalizedValue(locale2) %></b>
					</td>
				</tr>
					<%	} else { %>
				<tr>
					<td colspan="2"><span class="catalog-label"><%=locale1.getLanguage() %>.</span> <b><%=bean.getLocalization().getLocalizedValue(locale1) %></b>
					</td>
					<td colspan="2"><span class="catalog-label"><%=locale1.getLanguage() %>.</span> <b><%=bean.getReportLocalization().getLocalizedValue(locale1) %></b>
					</td>
				</tr>
					<%
						}
					}
				%>
				<tr>
					<td><b><%=bean.getActive()%></b></td>
					<td><b><%=bean.getAntimicrobialResistance()%></b></td>
					<td><b><%=bean.getOrderable()%></b></td>
					<%if (bean.getNotifyResults()) { %><td><b><spring:message code="test.notifyResults"/></b></td><%} %>
					<%if (bean.getInLabOnly()) { %><td><b><spring:message code="test.inLabOnly"/></b></td><%} %>
				</tr>
				<tr>
					<td><span class="catalog-label"><spring:message code="label.test.unit" /></span> <b><%=bean.getTestUnit()%></b></td>
					<td><span class="catalog-label"><spring:message code="label.sample.types" /></span> <b><%=bean.getSampleType()%></b></td>
					<td><span class="catalog-label"><spring:message code="label.panel" /></span> <b><%=bean.getPanel()%></b></td>
					<td><span class="catalog-label"><spring:message code="label.result.type" /></span> <b><%=bean.getResultType()%></b></td>
				</tr>
				<tr>
					<td><span class="catalog-label"><spring:message code="label.uom" /></span> <b><%=bean.getUom()%></b></td>
					<td><span class="catalog-label"><spring:message code="label.significant.digits" /></span> <b><%=bean.getSignificantDigits()%></b>
					</td>
					<td><span class="catalog-label"><spring:message code="label.loinc" /></span> <b><%=bean.getLoinc()%></b></td>
				</tr>

				<%
					if (bean.isHasDictionaryValues()) {
							boolean top = true;
							for (String value : bean.getDictionaryValues()) {
				%>
				<tr>
					<td>
						<%
							if (top) {
						%><span class="catalog-label"><spring:message code="configuration.test.catalog.select.values" /></span> <%
 	}
 %>
					</td>
					<td colspan="2"><b><%=value%></b></td>
					<td colspan="2">
						<%
							if (top) {
											top = false;
						%><span class="catalog-label"><spring:message code="configuration.test.catalog.reference.value" /></span> <b><%=bean.getReferenceValue()%></b>
					</td>
					<%
						}
								}
							%>
				</tr>
					<%
							}
					%>

				<%
					if (bean.isHasLimitValues()) {
						 String limitArray = null;
				%>
				<tr>
					<td colspan="5" align="center"><span class="catalog-label"><spring:message code="configuration.test.catalog.result.limits" /></span></td>
				</tr>
				<tr>
					<td><span class="catalog-label"><spring:message code="label.sex" /></span></td>
					<td><span class="catalog-label"><spring:message code="configuration.test.catalog.age.range" /></span></td>
					<td><span class="catalog-label"><spring:message code="configuration.test.catalog.normal.range" /></span></td>
					<td><span class="catalog-label"><spring:message code="configuration.test.catalog.valid.range" /></span></td>
                    <td><span class="catalog-label"><spring:message code="configuration.test.catalog.reporting.range" /></span></td>
                    <td><span class="catalog-label"><spring:message code="configuration.test.catalog.critical.range" /></span></td>

				</tr>
				<%
					String fLimitString = "";
					for (ResultLimitBean limitBean : bean.getResultLimits()) {
						fLimitString = fLimitString + limitBean.getGender() + ",";
						fLimitString = fLimitString + limitBean.getAgeRange() + ",";
						fLimitString = fLimitString + limitBean.getNormalRange() + ",";
						fLimitString = fLimitString + limitBean.getValidRange() + ",";
                        fLimitString = fLimitString + limitBean.getReportingRange() + ",";
                        fLimitString = fLimitString + limitBean.getCriticalRange() + "|";

				%>
				<tr>
					<td><b><%=limitBean.getGender()%></b></td>
					<td><b><%=limitBean.getAgeRange()%></b></td>
					<td><b><%=limitBean.getNormalRange()%></b></td>
					<td><b><%=limitBean.getValidRange()%></b></td>
                    <td><b><%=limitBean.getReportingRange()%></b></td>
                    <td><b><%=limitBean.getCriticalRange()%></b></td>
                            
				</tr>
				<%
					}
					%>
					<tr><td>
					<%--<input id="fLimit" type="hidden" value='<%=fLimitString%>' />  --%>
					<input id="fLimit" type="hidden" value='<%=fLimitString%>' />
					</td></tr>
					<%
						}
					}
				%>
			</tbody>
		</table>

	</div>
	
	<%
		// section testAdd
	%>

	<span class="step2 notStep1BreadCrumb" id="step2BreadCrumb"
		style="display: none"> <input type="button"
		value="<%=MessageUtil.getContextualMessage("configuration.test.modify")%>"
		onclick="goBackToStep1();" class="textButton" />&rarr; <%=MessageUtil.getContextualMessage("label.selectSampleType")%>
	</span> <span id="step2Confirm notStep1BreadCrumb"
		class="confirmationBreadCrumb" style="display: none"> <input
		type="button"
		value="<%=MessageUtil.getContextualMessage("configuration.test.modify")%>"
		onclick="goBackToStep1();" class="textButton" />&rarr; <input
		type="button"
		value="<%=MessageUtil.getContextualMessage("label.selectSampleType")%>"
		onclick="goBackToStep2();" class="textButton" />&rarr; <spring:message code="label.confirmation" />
	</span> <span class="dictionarySelect notStep1BreadCrumb"
		style="display: none"> <input type="button"
		value="<%=MessageUtil.getContextualMessage("configuration.test.modify")%>"
		onclick="goBackToStep1();" class="textButton" />&rarr; <input
		type="button"
		value="<%=MessageUtil.getContextualMessage("label.selectSampleType")%>"
		onclick="goBackToStep2();" class="textButton" />&rarr; <spring:message code="label.select.list.values" />
	</span> <span class="resultLimits notStep1BreadCrumb" style="display: none">
		<input type="button"
		value="<%=MessageUtil.getContextualMessage("configuration.test.modify")%>"
		onclick="goBackToStep1();" class="textButton" />&rarr; <input
		type="button"
		value="<%=MessageUtil.getContextualMessage("label.selectSampleType")%>"
		onclick="goBackToStep2();" class="textButton" />&rarr; <spring:message code="label.set.result.limits" />
	</span> <span
		class="selectListConfirm confirmationBreadCrumb notStep1BreadCrumb"
		style="display: none"> <input type="button"
		value="<%=MessageUtil.getContextualMessage("configuration.test.modify")%>"
		onclick="goBackToStep1();" class="textButton" />&rarr; <input
		type="button"
		value="<%=MessageUtil.getContextualMessage("label.selectSampleType")%>"
		onclick="goBackToStep2();" class="textButton" />&rarr; <input
		type="button"
		value="<%=MessageUtil.getContextualMessage("label.select.list.values")%>"
		onclick="goBackToStep3Dictionary();" class="textButton" />&rarr; <spring:message code="label.confirmation" />
	</span> <span class="resultLimitsConfirm confirmationBreadCrumb"
		style="display: none"> <input type="button"
		value="<%=MessageUtil.getContextualMessage("configuration.test.modify")%>"
		onclick="goBackToStep1();" class="textButton" />&rarr; <input
		type="button"
		value="<%=MessageUtil.getContextualMessage("label.selectSampleType")%>"
		onclick="goBackToStep2();" class="textButton" />&rarr; <input
		type="button"
		value="<%=MessageUtil.getContextualMessage("label.set.result.limits")%>"
		onclick="goBackToResultLimits();" class="textButton" />&rarr; <spring:message code="label.confirmation" />
	</span>

	<h3>
		<spring:message code="configuration.test.modify" />
	</h3>

	<input type="checkbox" onchange="guideSelection(this)">
	<spring:message code="label.show.guide" />
	<br />
	<br />

	<div id="guide" style="display: none">
		<span class="requiredlabel">*</span>
		<spring:message code="label.required.field" />
		<br />
		<br /> <span class="step1"> <spring:message htmlEscape="false" code="configuration.test.modify.guide.test" />
		</span> <span class="step2" id="step2Guide" style="display: none"> <spring:message htmlEscape="false" code="configuration.test.modify.guide.sample.type" />
		</span> <span class="dictionarySelect" style="display: none"> <spring:message htmlEscape="false" code="configuration.test.modify.guide.select.list" />
		</span> <span class="resultLimits" style="display: none;"> <spring:message htmlEscape="false" code="configuration.test.modify.guide.result.limits" />
		</span> <span class="confirmShow" style="display: none"> <spring:message htmlEscape="false" code="configuration.test.modify.guide.verify" />
		</span> <br />
		<hr />
	</div>
	<div id="step1Div" class="step1" style="display: none">
		<table width="80%">
			<tr>
				<td width="25%">
					<table>
						<tr>
							<td colspan="2"><spring:message code="test.testName" /><span
								class="requiredlabel">*</span></td>
						</tr>
						<tr>
							<td width="25%" align="right"><spring:message code="label.english" /></td>
							<td width="75%"><input type="text" id="testNameEnglish"
								class="required" onchange="checkReadyForNextStep()" /></td>
						</tr>
						<tr>
							<td width="25%" align="right"><spring:message code="label.french" /></td>
							<td width="75%"><input type="text" id="testNameFrench"
								class="required" onchange="checkReadyForNextStep()" /></td>
						</tr>
						<tr>
							<td>&nbsp;</td>
						</tr>
						<tr>
							<td colspan="2"><spring:message code="test.testName.reporting" /><span
								class="requiredlabel">*</span></td>
						</tr>
						<tr>
							<td></td>
							<td><input type="button"
								onclick="copyFromTestName(); checkReadyForNextStep()"
								value='<%=MessageUtil.getContextualMessage("test.add.copy.name")%>'></td>
						</tr>
						<tr>
							<td width="25%" align="right"><spring:message code="label.english" /></td>
							<td width="75%"><input type="text"
								id="testReportNameEnglish" class="required"
								onchange="checkReadyForNextStep()" /></td>
						</tr>
						<tr>
							<td width="25%" align="right"><spring:message code="label.french" /></td>
							<td width="75%"><input type="text" id="testReportNameFrench"
								class="required" onchange="checkReadyForNextStep()" /></td>
						</tr>
					</table>
				</td>


  
				<td width="25%" style="vertical-align: top; padding: 4px"><spring:message code="test.testSectionName" /><span class="requiredlabel">*</span><br />
					<select id="testUnitSelection" class="required"
					onchange="checkReadyForNextStep()">
						<option value="0"></option>
						<%
							for (IdValuePair pair : testUnitList) {
						%>
						<option value='<%=pair.getId()%>'><%=pair.getValue()%>
						</option>
						<%
							}
						%>
				</select><br />
				<br />
				<br />
				<br />
				<br /> <spring:message code="label.loinc" /><br />
					<form:input path="loinc" type="text" id="loinc"
					onchange="checkReadyForNextStep()" /></td>

				<td width="25%" style="vertical-align: top; padding: 4px"
					id="panelSelectionCell"><spring:message code="typeofsample.panel.panel" /><br /> 
					<select id="panelSelection" name="panelSelection" multiple="multiple" title="Multiple">
                        <% for (IdValuePair pair : panelList) { %>
                        <option value='<%=pair.getId()%>'><%=pair.getValue()%>
                        </option>
                        <% } %>
					   
				</select><br />
				<br />
				<br /> <spring:message code="label.unitofmeasure" /><br /> 
				<select id="uomSelection">
                        <option value='0'></option>
                        <% for (IdValuePair pair : uomList) { %>
                        <option value='<%=pair.getId()%>'><%=pair.getValue()%>
                        </option>
                        <% } %>
                    </select>
                </td>
				<td width="25%" style="vertical-align: top; padding: 4px"><spring:message code="result.resultType" /><span class="requiredlabel">*</span><br />
					 <select id="resultTypeSelection" class="required" onchange="checkReadyForNextStep()">
                        <option value="0"></option>
                        <% for (IdValuePair pair : resultTypeList) { %>
                        <option value='<%=pair.getId()%>'><%=pair.getValue()%>
                        </option>
                        <% } %>
				</select><br />
				<br />
				<br />
				<br />
				<br />
					<label for="antimicrobialResistance"><spring:message code="test.antimicrobialResistance"/></label>
					<input type="checkbox" id="antimicrobialResistance" /><br/>
				<label for="orderable"><spring:message code="test.isActive" /></label>
				<input type="checkbox" id="active"checked="checked" /><br />
				<label for="orderable"><spring:message code="label.orderable" /></label>
				<input type="checkbox" id="orderable" checked="checked" /><br/>
				<label for="notifyResults"><spring:message code="test.notifyResults" /></label>
				<input type="checkbox" id="notifyResults"/><br/>
				<label for="inLabOnly"><spring:message code="test.inLabOnly" /></label>
				<input type="checkbox" id="inLabOnly"/>
				</td>
			</tr>
		</table>
	</div>
	<div id="sampleTypeContainer" class="step2"
		style="width: 100%; overflow: hidden; display: none">
		<div style="float: left; width: 20%;">
			<spring:message code="test.testName" />
			<br /> <span class="tab"><spring:message code="label.english" />:
				<span id="testNameEnglishRO"></span></span><br /> <span class="tab"><spring:message
					code="label.french" />: <span id="testNameFrenchRO"></span></span><br />
			<br />
			<spring:message code="test.testName.reporting" />
			<br /> <span class="tab"><spring:message code="label.english" />:
				<span id="testReportNameEnglishRO"></span></span><br /> <span class="tab"><spring:message
					code="label.french" />: <span id="testReportNameFrenchRO"></span></span><br />
			<br />
			<spring:message code="test.testSectionName" />
			<div id="testSectionRO" class="tab"></div>
			<br />
			<spring:message code="typeofsample.panel.panel" />
			<div class="tab" id="panelRO">
				<spring:message code="label.none" />
			</div>
			<br />
			<spring:message code="label.unitofmeasure" />
			<div class="tab" id="uomRO">
				<spring:message code="label.none" />
			</div>
			<br />
			<spring:message code="label.loinc" />
			<div class="tab" id="loincRO">
				<spring:message code="label.none" />
			</div>
			<br />
			<spring:message code="result.resultType" />
			<div class="tab" id="resultTypeRO"></div>
			<br />

			<spring:message code="test.antimicrobialResistance"/>
			<div class="tab" id="antimicrobialResistanceRO"></div>
			<br/>
			<spring:message code="test.isActive" />
			<div class="tab" id="activeRO"></div>
			<br />
			<spring:message code="label.orderable" />
			<div class="tab" id="orderableRO"></div>
			<br />
			<spring:message code="test.notifyResults" />
			<div class="tab" id="notifyResultsRO"></div>
			<br />
			<spring:message code="test.inLabOnly" />
			<div class="tab" id="inLabOnlyRO"></div>
			<br />
		</div>
		<div class="step2" style="float: right; width: 80%; display: none">
			<div id="sampleTypeSelectionDiv" class="step2"
				style="float: left; width: 20%;">
				<spring:message code="label.sampleType" />
				<span class="requiredlabel">*</span> <select
					id="sampleTypeSelection" class="required" multiple="multiple"
					title="Multiple">
					<% for (IdValuePair pair : sampleTypeList) { %>
					<option value='<%=pair.getId()%>'><%=pair.getValue()%>
					</option>
					<% } %>
				</select><br />
			</div>
			<div id="testDisplayOrderDiv" style="float: left; width: 80%;">
				<div id="sortTitleDiv" align="center">
					<spring:message code="label.test.display.order" />
				</div>
				<div id="endOrderMarker"></div>

				<div class="dictionarySelect dictionaryMultiSelect"
					id="dictionarySelectId"
					style="padding: 10px; float: left; width: 280px; display: none; overflow: hidden">
					<spring:message code="label.select.list.options" />
					<span class="requiredlabel">*</span><br /> <select
						id="dictionarySelection" class="required" multiple="multiple"
						title="Multiple">
						<% for (IdValuePair pair : dictionaryList) { %>
						<option value='<%=pair.getId()%>'><%=pair.getValue()%>
						</option>
						<% } %>
					</select><br /> <br /> <br />
				</div>
				<div id="dictionaryVerifyId"
					style="padding: 10px; float: left; width: 280px; display: none; overflow: hidden;">
					<span><span class="half-tab"><spring:message
								code="label.select.list" /></span><br />
						<ul id="dictionaryVerifyListId">

						</ul> </span> <span><spring:message code="label.reference.value" /><br>
						<ul>
							<li id="referenceValue"></li>
						</ul></span> <span><spring:message code="label.default.result" /><br>
					<ul>
							<li id="defaultTestResultValue"></li>
						</ul></span>
				</div>
				<div id="sortDictionaryDiv" align="center" class="dictionarySelect"
					style="padding: 10px; float: left; width: 33%; display: none;">
					<spring:message code="label.result.order" />
					<span id="dictionarySortSpan" align="left">
						<UL id="dictionaryNameSortUI">

						</UL>
					</span>
				</div>
				<div class="dictionarySelect"
					style="padding: 10px; float: left; width: 280px; display: none">
					<div id="dictionaryReference">
						<spring:message code="label.reference.value" />
						<br /> <select id='referenceSelection'>
							<option value="0">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</option>
						</select>
					</div>
					<br>
					<div id="defaultTestResult">
						<spring:message code="label.default.result" />
						<br /> <select id='defaultTestResultSelection'>
							<option value="0">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</option>
						</select>
					</div>
					<br>

					<div id="dictionaryQualify" class="dictionaryMultiSelect">
						<spring:message code="label.qualifiers" />
						<br /> <select id='qualifierSelection' multiple='multiple'
							title='Multiple'></select>
					</div>
				</div>
			</div>
		</div>
	</div>
	<div id="dictionaryAskDiv" style="display: none;">
		<input type="button"
			value="<%=MessageUtil.getContextualMessage("label.button.editSelectValues")%>"
			onclick="editDictionaryAsk();" id="editDictionaryButton" />
	</div>
	<% 	List<IdValuePair> groupedDictionaryList = (List<IdValuePair>) pageContext.getAttribute("groupedDictionaryList"); %>
	<div id="dictionaryExistingGroups" class="dictionarySelect"
		style="display: none; width: 100%">
		<div style="width: 100%; text-align: center;">
			<spring:message code="label.existing.test.sets" />
		</div>
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
					</ul></td>
				  <%
                        testCount++;
                        columnCount++;
                    %>
                <% } %>

			</tr>
            <% } %>
		</table>
	</div>
	<div id="normalRangeTemplate" style="display: none;">
		<table>
			<tr class="row_index createdFromTemplate">
				<td><input type="hidden" class="rowKey" value="index" /><input
					id="genderCheck_index" type="checkbox"
					onchange="genderMatersForRange(this.checked, 'index')"></td>
				<td><span class="sexRange_index" style="display: none">
						<spring:message code="sex.male" />
				</span></td>
				<td style="white-space:nowrap;">
				<input class="yearMonthSelect_index" type="radio"
					name="time_index"
					value="<%=MessageUtil.getContextualMessage("abbreviation.year.single")%>"
					onchange="upperAgeRangeChanged( 'index' )" checked>
				<spring:message code="abbreviation.year.single" /> 
				<input
					class="yearMonthSelect_index" type="radio" name="time_index"
					value="<%=MessageUtil.getContextualMessage("abbreviation.month.single")%>"
					onchange="upperAgeRangeChanged( 'index' )">
				<spring:message code="abbreviation.month.single" />
				<input
					class="yearMonthSelect_index" type="radio" name="time_index"
					value="<%=MessageUtil.getContextualMessage("abbreviation.day.single")%>"
					onchange="upperAgeRangeChanged('index')">
				<spring:message code="abbreviation.day.single" />&nbsp;</td>
				<td id="lowerAge_index">0</td>
				<td><input type="text" id="upperAgeSetter_index"
					value="Infinity" size="10"
					onchange="upperAgeRangeChanged( 'index' )"><span
					id="upperAge_index"></span></td>
				<td><select id="ageRangeSelect_index"
					onchange="ageRangeSelected( this, 'index');">
						<option value="0"></option>
						 <% for (IdValuePair pair : ageRangeList) { %>
                        <option value='<%=pair.getId()%>'><%=pair.getValue()%>
                        </option>
                        <% } %>
				</select></td>
				<td><input type="text" value="-Infinity" size="10"
					id="lowNormal_index" class="lowNormal"
					onchange="normalRangeCheck('index');"></td>
				<td><input type="text" value="Infinity" size="10"
					id="highNormal_index" class="highNormal"
					onchange="normalRangeCheck('index');"></td>
				<td></td>
				<td></td>
				<td></td>
				<td><input id="removeButton_index" type="button"
					class="textButton" onclick='removeLimitRow( index );'
					value="<%=MessageUtil.getContextualMessage("label.remove")%>" /></td>
			</tr>
			<tr class="sexRange_index row_index createdFromTemplate">
				<td></td>
				<td><spring:message code="sex.female" /></td>
				<td></td>
				<td></td>
				<td></td>
				<td></td>
				<td><input type="text" value="-Infinity" size="10"
					id="lowNormal_G_index" class="lowNormal"
					onchange="normalRangeCheck('index');"></td>
				<td><input type="text" value="Infinity" size="10"
					id="highNormal_G_index" class="highNormal"
					onchange="normalRangeCheck('index');"></td>
				<td></td>
				<td></td>
				<td></td>
			</tr>
		</table>
	</div>
	<div id="normalRangeAskDiv" style="display: none;">
	<input type="button"
			value="<%=MessageUtil.getContextualMessage("label.button.editResultLimits")%>"
			onclick="editRangeAsk();" id="editResultLimitsButton" /> 
	</div>
	<div id="normalRangeDiv" style="display: none;">
		<h3>
			<spring:message code="label.range" />
		</h3>      
		<table style="display: inline-table">
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
				<td><input type="hidden" class="rowKey" value="0" /><input
					id="genderCheck_0" type="checkbox"
					onchange="genderMatersForRange(this.checked, '0')"></td>
				<td><span class="sexRange_0" style="display: none"> <spring:message code="sex.male" />
				</span></td>
				<td style="white-space:nowrap;"><input class="yearMonthSelect_0" type="radio" name="time_0"
					value="<%=MessageUtil.getContextualMessage("abbreviation.year.single")%>"
					onchange="upperAgeRangeChanged('0')" checked>
				<spring:message code="abbreviation.year.single" /> <input
					class="yearMonthSelect_0" type="radio" name="time_0"
					value="<%=MessageUtil.getContextualMessage("abbreviation.month.single")%>"
					onchange="upperAgeRangeChanged('0')">
				<spring:message code="abbreviation.month.single" /><input
					class="yearMonthSelect_0" type="radio" name="time_0"
					value="<%=MessageUtil.getContextualMessage("abbreviation.day.single")%>"
					onchange="upperAgeRangeChanged('0')">
				<spring:message code="abbreviation.day.single" />&nbsp;</td>
				<td id="lowerAge_0">0&nbsp;</td>
				<td><input type="text" id="upperAgeSetter_0" value="Infinity"
					size="10" onchange="upperAgeRangeChanged('0')"><span
					id="upperAge_0"></span></td>
				<td><select id="ageRangeSelect_0"
					onchange="ageRangeSelected( this, '0');">
						<option value="0"></option>
						<%
							for (IdValuePair pair : ageRangeList) {
						%>
						<option value='<%=pair.getId()%>'><%=pair.getValue()%>
						</option>
						<%
							}
						%>
				</select></td>
				<td><input type="text" value="-Infinity" size="10"
					id="lowNormal_0" class="lowNormal"
					onchange="normalRangeCheck('0');"></td>
				<td><input type="text" value="Infinity" size="10"
					id="highNormal_0" class="highNormal"
					onchange="normalRangeCheck('0');"></td>                
                <td><input type="text" value="-Infinity" size="10" id="lowReportingRange" onchange="reportingRangeCheck();"></td>
                <td><input type="text" value="Infinity" size="10" id="highReportingRange" onchange="reportingRangeCheck();"></td>
                <td><input type="text" value="infinity" size="5" id="lowCritical"></td>
                <td><input type="text" value="infinity" size="5" id="highCritical"></td>
				<td><input type="text" value="-Infinity" size="10"
					id="lowValid" onchange="validRangeCheck();"></td>
				<td><input type="text" value="Infinity" size="10"
					id="highValid" onchange="validRangeCheck();"></td>
			</tr>               
			<tr class="sexRange_0 row_0" style="display: none">
				<td></td>
				<td><spring:message code="sex.female" /></td>
				<td></td>
				<td></td>
				<td></td>
				<td></td>
				<td><input type="text" value="-Infinity" size="10"
					id="lowNormal_G_0" class="lowNormal"
					onchange="normalRangeCheck('0');"></td>
				<td><input type="text" value="Infinity" size="10"
					id="highNormal_G_0" class="highNormal"
					onchange="normalRangeCheck('0');"></td>
				<td></td>
				<td></td>
				<td></td>
			</tr>        
			<tr id="endRow"></tr>      
		</table>
		<label for="significantDigits"><spring:message code="label.significant.digits" /></label> <input type="number" min="0"
			max="10" id="significantDigits">
	</div>

	<div class="selectShow"
		style="margin-left: auto; margin-right: auto; width: 40%;">
		<input type="button"
			value="<%=MessageUtil.getContextualMessage("label.button.next")%>"
			disabled="disabled" onclick="nextStep();" id="nextButton" /> <input
			type="button"
			value="<%=MessageUtil.getContextualMessage("label.button.back")%>"
			onclick="navigateBack()" />
	</div>
	<div class="confirmShow"
		style="margin-left: auto; margin-right: auto; width: 40%; display: none">
		<input type="button"
			id="acceptButton"
			value="<%=MessageUtil.getContextualMessage("label.button.accept")%>"
			onclick="submitAction('TestModifyEntry');" /> <input
			type="button"
			value="<%=MessageUtil.getContextualMessage("label.button.back")%>"
			onclick="navigateBackFromConfirm()" />
	</div>
	<table>
		<%
			testCount = 0;
			columnCount = 1;
			while (testCount < testList.size()) {
		%>
		<tr>
			<td><input type="button"
				value='<%=((IdValuePair) testList.get(testCount)).getValue()%>'
				onclick="setForEditing( '<%=((IdValuePair) testList.get(testCount)).getId() + "', '"
						+ ((IdValuePair) testList.get(testCount)).getValue()%>');"
				class="textButton test" /> <%
 		testCount++;
 		columnCount = 1;
 %></td>
			<%
				while (testCount < testList.size() && (columnCount < columns)) {
			%>
			<td><input type="button"
				value='<%=((IdValuePair) testList.get(testCount)).getValue()%>'
				onclick="setForEditing( '<%=((IdValuePair) testList.get(testCount)).getId() + "', '"
							+ ((IdValuePair) testList.get(testCount)).getValue()%>' );"
				class="textButton test" /> <%
 		testCount++;
 		columnCount++;
 %></td>
			<%
				}
			%>

		</tr>
		<%
			}
		%>
	</table>

	<br> <input type="button"
		value='<%=MessageUtil.getContextualMessage("label.button.finished")%>'
		onclick="submitAction('TestManagementConfigMenu');" />
</form:form>
