<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page import="org.openelisglobal.common.action.IActionConstants,
                 org.openelisglobal.common.util.DateUtil,
                 org.openelisglobal.internationalization.MessageUtil,
                 java.util.List,
                 org.openelisglobal.common.util.Versioning,
                 org.openelisglobal.referral.action.beanitems.ReferralItem,
                 org.openelisglobal.common.util.IdValuePair,
                 org.openelisglobal.referral.action.beanitems.ReferredTest" %>


<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>


<%@ taglib prefix="fn" uri="jakarta.tags.functions"%>

<!-- <script type="text/javascript" src="scripts/jquery.ui.js?"></script> -->
<script type="text/javascript"
        src="scripts/jquery.asmselect.js?"></script>
<script type="text/javascript" src="scripts/utilities.js?"></script>
<script type="text/javascript" src="scripts/testReflex.js?" ></script>

<script type="text/javascript">
function referralTestSelected(e) {
	var index = 0;
	jQuery(currentReferalDivSelector).children('#mainTable .referralRow').addClass('deleteReferralRow');
	
    var samples = document.getElementById("samplesBlock").getElementsByClassName("sampleId");
    samples.each(function(node, index) {
		var sampleNum = node.value;
		var testNames = document.getElementById("tests_" + node.value).value.split(",");
		var testIds = document.getElementById("testIds_" + node.value).value.split(",");
		for (var j = 0; j < testIds.length; ++j) {
			if (testIds[j] !== "") {
				createReferralOption(sampleNum, testIds[j], testIds[j], testNames[j], index);
			}
		}
	});
	jQuery('.deleteReferralRow').remove();
	setSave();
	}

function /*void*/ markModified(index) {
	checkFinish(index);
	jQuery("#modified_" + index).val('true');
	setSave();
    makeDirty();
}

function missingRequiredReferralValues() {
    var missing = false;

    jQuery(".referralRow").each(function (index, element) {
        var children = jQuery(element).find(".requiredReferral");
        if (!((children[1].value == 0 && children[0].value == 0) ||
                (children[1].value != 0 && children[0].value != 0))) {
            missing = true;
        }
    });

    return missing;

}

function /*void*/ validateDateFormat(dateElement) {
    var valid = true;
    if (dateElement.value.length > 0) {
        var dateRegEx = new RegExp("\\d{2}/\\d{2}/\\d{4}");
        valid = dateRegEx.test(dateElement.value);
    }

    dateElement.style.borderColor = valid ? "" : "red";
}

function checkFinish(index) {
	var hasNumericResult = jQuery("#numericResult_" + index).val() !== undefined && jQuery("#numericResult_" + index).val() !== "";
	var hasDictionaryResult = jQuery("#dictionaryResult_" + index).val() !== undefined && jQuery("#dictionaryResult_" + index).val() !== "0";
	var hasMultiSelectResult = jQuery("#resultMultiSelect_" + index).val() !== undefined && jQuery("#resultMultiSelect_" + index).val() !== "";
	var hasResult = hasNumericResult ||  hasDictionaryResult || hasMultiSelectResult;
	jQuery("#finished_" + index).prop('disabled', !hasResult);
	if (!hasResult && jQuery('#finished_' + index).is(':checked')) {
		jQuery("#finished_" + index).prop('checked', false);
	}
}

function createReferralOption(sampleNum, testNum, testId, testName, index) {
	var table, tableBody, row, cell1, cell2, cell3, cell4, cell5;
	var select, option;
	
	referalDiv = document.getElementById('referTestSection_' + sampleNum)
	if(typeof referalDiv === 'undefined'){
        return;
	}
	table = referalDiv.getElementsByClassName("mainTable")[0];
	tableBody = table.getElementsByTagName('tbody')[0];
	
	if (jQuery('#sample_' + sampleNum + '_test_' + testNum).length) {
		document.getElementById('sample_' + sampleNum + '_test_' + testNum).className = "referralRow";
		return;
	}
	
	row = tableBody.insertRow(-1);
	row.setAttribute('id', 'sample_' + sampleNum + '_test_' + testNum);
	row.setAttribute('class', 'referralRow');
	cell1 = row.insertCell(-1);
	cell2 = row.insertCell(-1);
	cell3 = row.insertCell(-1);
	cell4 = row.insertCell(-1);
	cell5 = row.insertCell(-1);

	cell1.className = "center-text";
	cell2.className = "center-text";
	cell3.className = "center-text";
	cell4.className = "center-text";
	cell5.className = "center-text";
	
	var referralReasonSelect = document.createElement("select");
	referralReasonSelect.setAttribute('name','referralItems[' + index + '].referralReasonId');
	referralReasonSelect.setAttribute('id', 'referralReasonId_' + index);
	referralReasonSelect.setAttribute('onchange', 'markModified("' + index + '");');
	<c:forEach items="${form.referralReasons}" var="referralReason" varStatus="iter">
	option = document.createElement('option');
	option.value = '${referralReason.id}';
	option.innerHTML = '<c:out value="${referralReason.value}"/>';
	referralReasonSelect.appendChild(option);
	</c:forEach>
	cell1.appendChild(referralReasonSelect);

	var referrerInput = document.createElement("input");
	referrerInput.setAttribute('type','text');
	referrerInput.setAttribute('name','referralItems[' + index + '].referrer');
	referrerInput.setAttribute('id', 'referrer_' + index);
	referrerInput.setAttribute('onchange', 'markModified("' + index + '");');
	referrerInput.setAttribute('value', '${userSessionData.elisUserName}');
	cell2.appendChild(referrerInput);
	
	var referralOrgSelect = document.createElement("select");
	referralOrgSelect.setAttribute('class','requiredReferral');
	referralOrgSelect.setAttribute('name','referralItems[' + index + '].referredInstituteId');
	referralOrgSelect.setAttribute('id', 'referredInstituteId_' + index);
	referralOrgSelect.setAttribute('onchange', 'markModified("' + index + '");');
    <c:if test="${form.referralOrganizations.size() != 1}" >
    option = document.createElement('option');
	option.value = '0';
	referralOrgSelect.appendChild(option);
    </c:if>
	<c:forEach items="${form.referralOrganizations}" var="referralOrganization" varStatus="iter">
	option = document.createElement('option');
	option.value = "${referralOrganization.id}";
	option.innerHTML = '${referralOrganization.value}';
	referralOrgSelect.appendChild(option);
	</c:forEach>
	cell3.appendChild(referralOrgSelect);

	var referrerInput = document.createElement("input");
	referrerInput.setAttribute('type','text');
	referrerInput.setAttribute('name','referralItems[' + index + '].referredSendDate');
	referrerInput.value = jQuery('#requestDate').val();
	referrerInput.setAttribute('id', 'sendDate_' + index);
	referrerInput.setAttribute('size', '8');
	referrerInput.setAttribute('maxLength', '10');
	referrerInput.setAttribute('onchange', 'markModified("' + index + '");checkValidEntryDate(this, \'past\')');
	referrerInput.setAttribute('onkeyup', 'addDateSlashes(this, event);');
	cell4.appendChild(referrerInput);

	var shadowReferredTestInput = document.createElement("hidden");
// 	shadowReferredTestInput.setAttribute('type','hidden');
	shadowReferredTestInput.setAttribute('name','_referralItems[' + index + '].referredTestId');
	shadowReferredTestInput.setAttribute('id', 'shadowReferredTest_' + index);
	cell5.appendChild(shadowReferredTestInput);
	
	var referredTestSelect = document.createElement("select");
	referredTestSelect.setAttribute('class','requiredReferral');
	referredTestSelect.setAttribute('name','referralItems[' + index + '].referredTestId');
	referredTestSelect.setAttribute('id', 'testSelection_' + index);
	referredTestSelect.setAttribute('onchange', 'markModified("' + index + '");');
// 	option = document.createElement('option');
// 	option.value = 0;
// 	referredTestSelect.appendChild(option);
	option = document.createElement('option');
	option.value = testId;
	option.innerHTML = testName;
	referredTestSelect.appendChild(option);
	cell5.appendChild(referredTestSelect);
	
}

</script>

<table width="75%" border="0" cellspacing="0" cellpadding="1" id="mainTable" class="mainTable">
<thead>
<tr>
    <th colspan="6" class="headerGroup"><spring:message code="referral.header.group.request"/></th>
</tr>
<tr>
    <th><spring:message code="referral.reason"/><span class="requiredlabel">*</span></th>
    <th><spring:message code="referral.referer"/></th>
    <th><spring:message code="referral.institute"/><span class="requiredlabel">*</span></th>
    <th><spring:message code="referral.sent.date"/><br/><%=DateUtil.getDateUserPrompt()%></th>
    <th><spring:message code="test.testName"/><span class="requiredlabel">*</span></th>
</tr>
</thead>
<tbody>
</tbody>
</table>
