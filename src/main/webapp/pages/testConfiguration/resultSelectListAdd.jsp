<%--
   Created by IntelliJ IDEA.
   User: kenny
   Date: 2019-09-24
   Time: 20:06
   To change this template use File | Settings | File Templates.
 --%>

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
<%@ taglib uri="jakarta.tags.functions" prefix="fn" %>



<script>
    var normalConfirmAlert = '<spring:message code="configuration.selectList.confirmChange" />';
    var validating = false;
</script>
<script type="text/javascript" src="scripts/jquery-ui.js?"></script>
<link rel="stylesheet" media="screen" type="text/css"
      href="css/jquery_ui/jquery.ui.theme.css?"/>
<input 	type="button"
          class="textButton"
          value="<%= MessageUtil.getContextualMessage("banner.menu.administration")%>"
          onclick="submitAction('MasterListsPage');" >&rarr;

<input  type="button"
        class="textButton"
        value="<%= MessageUtil.getContextualMessage("configuration.test.management") %>"
        onclick="submitAction('TestManagementConfigMenu');" >&rarr;


<%=MessageUtil.getContextualMessage( "label.resultSelectList" ) %>
<br><br>

<%
    String p = request.getParameter("page");
    if (p == null) {
        p = "1";
    }
%>
<form:form name="${form.formName}"
           action="${form.formAction}"
           modelAttribute="form"
           onSubmit="return submitForm(this);"
           method="${form.formMethod}"
           id="mainForm">
<c:if test="${form.page == '1'}">
<h1 id="action"><spring:message code="configuration.selectList.header"/></h1>
<p><spring:message code="configuration.selectList.description"/></p>
<div style="width: 100%; min-height: 50px;">
    <div style="width: 300px; float: left;">
        <label style="width: 100%;">English</label>
        <form:input path="nameEnglish"/>
    </div>

    <div style="width: 300px; float: left;">
        <label style="width: 100%;">French</label>
        <form:input path="nameFrench"/>
    </div>
</div>

<div style="width: 100%; text-align: center">
    <button onclick="return moveNext()">
        <spring:message code="label.button.next"/>
    </button>
    <button onclick="return submitAction('TestManagementConfigMenu');">
        <spring:message code="label.button.cancel"/>
    </button>
</div>


</c:if>
<c:if test="${form.page == '2'}">
    <form:hidden path="nameEnglish"/>
    <form:hidden path="nameFrench"/>
<h1 id="action"><spring:message code="configuration.selectList.assign.header"/></h1>
<p class="verify-alert"><spring:message code="configuration.selectList.verifyMessage"/></p>
<p class="assign"><spring:message code="configuration.selectList.assign.description"/></p>
<p><spring:message code="configuration.selectList.assign.new"/></p>

<script>
    var tests= [];
    <c:forEach items="${form.tests}" var="test" varStatus="counter">
        var test = {};
        test['name'] = '${test.description}';
        test['id'] = '${test.id}';
        tests.push(test);
    </c:forEach>
    var currentItem = "<c:out value="${form.nameEnglish}" />";
</script>
<table style="margin: 0 auto; width: 20%;">
    <tr>
        <th>English</th><th>French</th>
    </tr>
    <tr>
        <td><c:out value="${form.nameEnglish}" /></td><td><c:out value="${form.nameFrench}" /></td>
    </tr>
</table>

<div class="result-select-box" id="result-select-el">

</div>
<div style="width: 100%; text-align: center; margin-top: 10px;">
    <button class="save-btn" onclick="return save()">
        <spring:message code="label.button.save" />
    </button>
    <button class="cancel-btn">
        <spring:message code="label.button.back"/>
    </button>
</div>
<table style="width: 100%">
    <caption class="available-tests-cation">Available Tests</caption>
    <c:forEach items="${form.tests}" begin="0" end="${fn:length(form.tests)}" step="4" var="test" varStatus="counter">
        <tr style="padding: 5px;">
            <td style="padding: 5px;" class="test-list-name-${form.tests[counter.index].id}">
                <a onclick="return selectTest(${form.tests[counter.index].id})"><c:out value="${form.tests[counter.index].description}" /></a>
            </td>
            <td style="padding: 5px;" class="test-list-name-${form.tests[counter.index + 1].id}">
                <a onclick="return selectTest(${form.tests[counter.index + 1].id})"><c:out value="${form.tests[counter.index + 1].description}" /></a>
            </td>
            <td style="padding: 5px;" class="test-list-name-${form.tests[counter.index + 2].id}">
                <a onclick="return selectTest(${form.tests[counter.index + 2].id})"><c:out value="${form.tests[counter.index + 2].description}" /></a>
            </td>
            <td style="padding: 5px;" class="test-list-name-${form.tests[counter.index + 3].id}">
                <a onclick="return selectTest(${form.tests[counter.index + 3].id})"><c:out value="${form.tests[counter.index + 3].description}" /></a>
            </td>
        </tr>
    </c:forEach>
</table>
<form:hidden path="testSelectListJson" />
<script>
    var testDictionary= {};
    <c:forEach items="${form.testDictionary}" var="dictionary">
    var dictionaries = [];
    var testId = '${dictionary.key}';
    <c:forEach items="${dictionary.value}" var="testDictionaries">
        var dic = {}
        dic.id = '${testDictionaries.id}';
        dic.value = '${testDictionaries.value}';
        dictionaries.push(dic);
    </c:forEach>
    testDictionary[testId] = dictionaries;
    </c:forEach>
</script>
</c:if>
</form:form>
<script>
    function moveNext() {
        var nameEnglish = jQuery('input[name="nameEnglish"]').val();
        var nameFrench = jQuery('input[name="nameFrench"]').val();

        var form = document.getElementById("mainForm");

        if (nameEnglish != '' || nameFrench != '') {
            window.onbeforeunload = null; // Added to flag that formWarning alert isn't needed.
            form.action = "ResultSelectListAdd";
            form.submit();
        }
        return false;
    }

    function selectTest(id) {
        if (validating) {
            return;
        }
        var test = tests.find(function(t) {
            return t.id == id;
        });
        if (test) {
            if (document.getElementById("test-select-" + test.id) == null) {
                var currentSelects = testDictionary[test.id];
                var li = []
                if (currentSelects) {
                    for (var i = 0; i < currentSelects.length; i++) {
                        li.push('<li class="ui-state-default_oe ui-state-default_oe-tag select-list" list-id="' + currentSelects[i].id + '"><span class="ui-icon ui-icon-arrowthick-2-n-s"></span>' + currentSelects[i].value + '</li>');
                    }
                }
                var html = '<div class="result-list-test" id="test-select-' + test.id + '" test-id="' + test.id + '">' +
                    '<p>' + test.name + '</p><ul id="result-list-ul-' + test.id + '" class="sortable">' + li.join('') +
                    '<li class="ui-state-default_oe ui-state-default_oe-tag select-list new-select-list-item" list-id="new-id"><span class="ui-icon ui-icon-arrowthick-2-n-s"></span>' + currentItem + '</li>' +
                    '</ul><p><input type="checkbox" class="normal" name="normal-' + test.id + '" onchange="return confirmNormalChange(\'normal-' + test.id + '\')"/>Normal</p>' +
                    '<p><input type="checkbox" class="qualifiable" name="qualifiable-' + test.id + '"/>Qualifiable</p>' +
                    '<p><button class="remove-btn" onclick="return removeTest(' + test.id + ')">Remove</button></p></div>';
                document.getElementById("result-select-el").insertAdjacentHTML('beforeend', html);
                jQuery("#result-list-ul-" + test.id).sortable()
                jQuery("#result-list-ul-" + test.id).disableSelection()
                highlightTests(test.id);
            }
        }
    }

    /**
     *  Highlights tests which have the same options as the test with the given testId.
     *  First the function will remove the current highlights.
     *  Then go through the tests and get the ones with the same number of options.
     *  Then it will check if all the options match.
     *  If they match then it will highlight the test.
     */
    function highlightTests(testId) {
        var selects = testDictionary[testId];
        var currentIds = getSelectIds(selects);
        jQuery('.highlighted-tests').removeClass('highlighted-tests');
        for (var key in testDictionary) {
            if (key != testId && testDictionary.hasOwnProperty(key)) {
                if (document.getElementById("test-select-" + key) == null) {
                    var tempSelects = testDictionary[key];
                    if (tempSelects.length == selects.length) {
                        var ids = getSelectIds(tempSelects);
                        var exist = true;
                        for (var i = 0; i < ids.length; i++) {
                            if (currentIds.indexOf(ids[i]) === -1) {
                                exist = false;
                            }
                        }
                        if (exist) {
                            jQuery('.test-list-name-' + key).addClass('highlighted-tests');
                        }
                    }
                }
            }
        }
    }

    function getSelectIds(selects) {
        var ids = [];
        for (var i = 0; i < selects.length; i++) {
            ids.push(selects[i].id);
        }
        return ids;
    }

    function confirmNormalChange(inputName) {
        var checked = jQuery('input[name="' + inputName + '"]').prop('checked');
        var newValue, oldValue;
        if (checked) {
            newValue = 'Normal';
            oldValue = 'Not Normal';
        } else {
            newValue = 'Not Normal';
            oldValue = 'Normal';
        }
        var alertText = normalConfirmAlert.replace('%oldvalue%', oldValue).replace('%newvalue%', newValue);
        var confirmChange = confirm(alertText);
        if (!confirmChange) {
            jQuery('input[name="' + inputName + '"]').prop('checked', !checked);
        }
    }

    function removeTest(id) {
        jQuery("#test-select-" + id).remove();
        return false;
    }

    function save() {
        var assignments = jQuery(".result-list-test");
        var testSelectListJson = [];
        for (var i = 0; i < assignments.length; i++) {
            var assignment = assignments[i];
            var testId = assignment.getAttribute('test-id');
            var selectLists = jQuery(assignment).find('.select-list');
            var items = [];
            for (var j = 0; j < selectLists.length; j++) {
                var selectList = selectLists[j];
                var selectOption = jQuery(selectList).attr('list-id');
                var selectJson = {};
                if (selectOption == 'new-id') {
                    selectJson.normal = jQuery('input[name="normal-"' + testId + '"]').prop('checked');
                    selectJson.qualifiable = jQuery('input[name="qualifiable-' + testId + '"]').prop('checked');
                } else {
                    selectJson.id = selectOption;
                }
                selectJson.order = j;
                items.push(selectJson);
            }
            var testResult = { id: testId, items: items };
            testSelectListJson.push(testResult);
        }
        jQuery(".remove-btn").attr('disabled', true);
        jQuery(".normal").attr('disabled', true);
        jQuery(".qualifiable").attr('disabled', true);
        jQuery("#result-list-ul-" + testId).sortable({ disabled: true })
        jQuery(".save-btn").html('Accept');
        var j = {};
        j.tests = testSelectListJson;
        jQuery('input[name="testSelectListJson"]').val(JSON.stringify(j));
        jQuery(".save-btn").on('click', accept);
        jQuery(".cancel-btn").off('click', goBack);
        jQuery(".cancel-btn").on('click', cancelSave);

        jQuery(".verify-alert").show();
        jQuery(".assign").hide();
        // document.getElementsByClassName('cancel-btn')[0].removeEventListener('click', submitAction);
        /*window.onbeforeunload = null; // Added to flag that formWarning alert isn't needed.
        form.action = "SaveResultSelectList";
        form.submit();*/
        validating = true;
        return false;
    }

    function accept() {
        window.onbeforeunload = null; // Added to flag that formWarning alert isn't needed.
        var form = document.getElementById("mainForm");
        form.action = "SaveResultSelectList";
        form.submit();
    }

    function submitAction(target) {
        window.location = target;
        return false;
    }
    
    function goBack() {
        window.location = 'ResultSelectListAdd';
        return false;
    }

    function cancelSave() {
        jQuery(".remove-btn").attr('disabled', false);
        jQuery(".normal").attr('disabled', false);
        jQuery(".qualifiable").attr('disabled', false);
        jQuery("#result-list-ul-" + testId).sortable({ disabled: false })
        jQuery(".save-btn").html('Save');

        jQuery(".save-btn").on('click', save);
        jQuery(".cancel-btn").off('click', cancelSave);
        jQuery(".cancel-btn").on('click', goBack);

        jQuery(".verify-alert").hide();
        jQuery(".assign").show();
        validating = false;
        return false;
    }

    jQuery(document).ready( function() {
        jQuery(".cancel-btn").on('click', goBack);
        jQuery(".verify-alert").hide();
    });
</script>