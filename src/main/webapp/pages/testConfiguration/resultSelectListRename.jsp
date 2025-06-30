<%--
  Created by IntelliJ IDEA.
  User: kenny
  Date: 2019-10-01
  Time: 20:53
  To change this template use File | Settings | File Templates.
--%>
<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         import="org.openelisglobal.common.action.IActionConstants,
         		org.openelisglobal.common.util.IdValuePair,
         		org.openelisglobal.common.util.*, org.openelisglobal.internationalization.MessageUtil,
         		org.openelisglobal.common.util.Versioning,
         		java.util.List,
         		java.util.ArrayList,
         		org.openelisglobal.common.provider.query.EntityNamesProvider" %>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib uri="jakarta.tags.functions" prefix="fn" %>



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
<%=MessageUtil.getContextualMessage( "configuration.selectList.rename" ) %>
<br><br>
<div id="form-div">
<form:form name="${form.formName}"
           action="${form.formAction}"
           modelAttribute="form"
           onSubmit="return submitForm(this);"
           method="${form.formMethod}"
           id="mainForm">

    <table class="rename-result-list-table">
        <tr>
            <th>English</th><th>French</th>
        </tr>
        <tr>
            <td class="english"></td><td class="french"></td>
        </tr>
    </table>
    <div class="rename-result-list-form">
        <div style="width: 300px; float: left;">
            <label style="width: 100%;">English</label>
            <form:input path="nameEnglish"/>
        </div>

        <div style="width: 300px; float: left;">
            <label style="width: 100%;">French</label>
            <form:input path="nameFrench"/>
        </div>
        <input type="hidden" id="resultSelectOptionId" name="resultSelectOptionId"/>
    </div>

    <div style="width: 100%; text-align: center">
        <button onclick="return save()">
            <spring:message code="label.button.save"/>
        </button>
        <button onclick="return cancel();">
            <spring:message code="label.button.cancel"/>
        </button>
    </div>
</form:form>
</div>
<table>
    <c:forEach items="${form.resultSelectOptionList}" begin="0" end="${fn:length(form.resultSelectOptionList)}" step="4" var="option" varStatus="counter">
        <tr style="padding: 5px;">
            <td style="padding: 5px;" class="test-list-name-${form.resultSelectOptionList[counter.index].id}">
                <a onclick="return editName(${form.resultSelectOptionList[counter.index].id})"><c:out value="${form.resultSelectOptionList[counter.index].dictEntry}" /></a>
            </td>
            <td style="padding: 5px;" class="test-list-name-${form.resultSelectOptionList[counter.index + 1].id}">
                <a onclick="return editName(${form.resultSelectOptionList[counter.index + 1].id})"><c:out value="${form.resultSelectOptionList[counter.index + 1].dictEntry}" /></a>
            </td>
            <td style="padding: 5px;" class="test-list-name-${form.resultSelectOptionList[counter.index + 2].id}">
                <a onclick="return editName(${form.resultSelectOptionList[counter.index + 2].id})"><c:out value="${form.resultSelectOptionList[counter.index + 2].dictEntry}" /></a>
            </td>
            <td style="padding: 5px;" class="test-list-name-${form.resultSelectOptionList[counter.index + 3].id}">
                <a onclick="return editName(${form.resultSelectOptionList[counter.index + 3].id})"><c:out value="${form.resultSelectOptionList[counter.index + 3].dictEntry}" /></a>
            </td>
        </tr>
    </c:forEach>
</table>


<script>
    var options= {};
    var localization = {};
    <c:forEach items="${form.resultSelectOptionList}" var="dictionary">
        var id = '${dictionary.id}';
        options[id] = '${dictionary.dictEntry}';
        localization[id] = "${dictionary.localizedDictionaryName.localeValues}"
    </c:forEach>

    function editName(id) {
        var name = options[id];
        var fr = name;
        var en = name;
        if (localization[id] != '') {
            var localized = localization[id];
            var names = localized.substring(1, localized.indexOf('}'));
            var parts = names.split(',');
            for(var i = 0; i < parts.length; i++) {
                var trimmed = parts[i].trim();
                if (trimmed.startsWith('fr')) {
                    fr = trimmed.substring(trimmed.indexOf('fr') + 3);
                } else if (trimmed.startsWith('en')) {
                    en = trimmed.substring(trimmed.indexOf('en') + 3);
                }
            }
            if (parts[0].startsWith('fr')) {

            }
        }
        jQuery('.english').html(en);
        jQuery('.french').html(fr);
        jQuery('#resultSelectOptionId').val(id);
        jQuery("#form-div").show();

    }

    function save() {
        var form = document.getElementById("mainForm");
        form.action = "SelectListRenameEntry";
        form.submit();
    }

    function cancel() {
        jQuery("#form-div").hide();
        return false;
    }

    jQuery(document).ready( function() {
        jQuery("#form-div").hide();
    });
</script>