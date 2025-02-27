<%--
  Created by IntelliJ IDEA.
  User: kenny
  Date: 2019-09-29
  Time: 11:23
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java"
         import="org.openelisglobal.common.action.IActionConstants,
				org.openelisglobal.common.formfields.FormFields,
                org.openelisglobal.common.formfields.FormFields.Field,
                org.openelisglobal.common.provider.validation.NonConformityRecordNumberValidationProvider,
                org.openelisglobal.common.services.PhoneNumberService,
                org.openelisglobal.common.util.DateUtil,
                org.openelisglobal.internationalization.MessageUtil,
                org.openelisglobal.common.util.Versioning,
                org.openelisglobal.qaevent.valueholder.retroCI.QaEventItem,
                org.openelisglobal.common.util.ConfigurationProperties" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%
	boolean useProject = FormFields.getInstance().useField(Field.Project);
	boolean useSiteList = FormFields.getInstance().useField(Field.NON_CONFORMITY_SITE_LIST);
	boolean useSubjectNo = FormFields.getInstance().useField(Field.QASubjectNumber);
	boolean useNationalID = FormFields.getInstance().useField(Field.NationalID);
%>

<link rel="stylesheet" href="css/jquery_ui/jquery.ui.all.css?">
<link rel="stylesheet" href="css/customAutocomplete.css?">

<script src="scripts/ui/jquery.ui.core.js?"></script>
<script src="scripts/ui/jquery.ui.widget.js?"></script>
<script src="scripts/ui/jquery.ui.button.js?"></script>
<script src="scripts/ui/jquery.ui.menu.js?"></script>
<script src="scripts/ui/jquery.ui.position.js?"></script>
<script src="scripts/ui/jquery.ui.autocomplete.js?"></script>
<script src="scripts/customAutocomplete.js?"></script>
<script src="scripts/utilities.js?"></script>
<script src="scripts/ajaxCalls.js?"></script>
<script type="text/javascript" src="scripts/jquery-ui.js?"></script>
<link rel="stylesheet" media="screen" type="text/css"
      href="css/jquery_ui/jquery.ui.theme.css?"/>

<h2><spring:message code="reports.configuration.title" /></h2>

<script>
    var reports= [];
    <c:forEach items="${form.reportList}" var="report">
        var report = {};
        report['id'] = '${report.id}';
        report['name'] = '${report.name}';
        report['category'] = '${report.category}';
        report['menuElementId'] = '${report.menuElementId}';
        report['isVisible'] = '${report.isVisible}';
        report['sortOrder'] = '${report.sortOrder}';
        reports.push(report);
    </c:forEach>

    var reportCategory = {};
    <c:forEach items="${form.reportCategoryList}" var="category">
        // var category = {};

        reportCategory['${category.id}'] = '${category.name}';
        // report['name'] = '${category.name}';
        // reportCategory.push(category);
    </c:forEach>
    var _csrf = '?${_csrf.parameterName}=${_csrf.token}';
</script>
<div id="new-report-btn-div">
    <button onclick="newReport()"><spring:message code="report.configuration.addNewReport" /></button>
</div>
<table id="report-table">

    <tr>
        <th><spring:message code="report.configuration.name" /></th>
        <th><spring:message code="report.configuration.category" /></th>
        <th><spring:message code="report.configuration.visible" /></th><th></th>
    </tr>
    <c:forEach items="${form.reportList}" var="report">
        <tr>
            <td><c:out value="${report.name}" /></td>
            <td category="${report.category}" class="category">
                <c:forEach items="${form.reportCategoryList}" var="category">
                    <c:if test="${report.category == category.id}" >
                        <c:out value="${category.name}" />
                    </c:if>
                </c:forEach>
            </td>
            <td>
                <c:if test="${report.isVisible == 'true'}">
                    <spring:message code="report.configuration.yes" />
                </c:if>
                <c:if test="${report.isVisible != 'true'}">
                    <spring:message code="report.configuration.no" />
                </c:if>

            </td>
            <td>
                <button onclick="editReport('<c:out value="${report.id}" />', '<c:out value="${report.category}" />')">
                    <spring:message code="report.configuration.edit" />
                </button>
            </td>
        </tr>
    </c:forEach>
</table>

<div id="edit-report-div">
<form:form name="${form.formName}"
           action="${form.formAction}"
           modelAttribute="form"
           onSubmit="return submitForm(this);"
           method="${form.formMethod}"
           cssClass="edit-report-form"
           id="mainForm">

    <div class="form-div">
        <label><spring:message code="report.configuration.name" /></label>
        <form:hidden path="currentReport.name"/>
        <p><strong id="currentReportName"></strong></p>
    </div>

    <div class="form-div">
        <label><spring:message code="report.configuration.category" /></label>
        <form:select path="currentReport.category" cssClass="form-control" onchange="onCategoryChange()">
            <form:option value="">Select one</form:option>
            <form:options items="${form.reportCategoryList}" itemLabel="name" itemValue="id"/>
        </form:select>
    </div>
    <div class="form-div">
        <label><spring:message code="report.configuration.visible" /></label>
        <input type="checkbox" name="currentReport.isVisible" />
    </div>

    <ul id="report-ordering-panel" class="report-ordering-panel sortable sortable-tag ui-sortable report-ordering-panel">

    </ul>
    <input name="idOrder" type="hidden" />
    <input name="currentReport.id" type="hidden" />
    <button onclick="save('mainForm')">
        <spring:message code="label.button.save" />
    </button>
    <button onclick="return cancelEdit()">
        <spring:message code="label.button.cancel" />
    </button>

</form:form>
</div>

<div id="new-report-div">
    <form:form name="${form.formName}"
               action="${form.formAction}"
               modelAttribute="form"
               onSubmit="return submitForm(this);"
               method="${form.formMethod}"
               cssClass="edit-report-form"
               enctype="multipart/form-data"
               id="new-report-form">

        <div class="form-div">
            <label><spring:message code="report.configuration.name" /> <span class="requiredlabel">*</span></label>
            <form:input path="currentReport.name" cssClass="form-control reportName" />
        </div>

        <div class="form-div">
            <label><spring:message code="report.configuration.category" /><span class="requiredlabel">*</span></label>
            <form:select path="currentReport.category" cssClass="form-control" onchange="onCategoryChange()">
                <form:option value="">Select one</form:option>
                <form:options items="${form.reportCategoryList}" itemLabel="name" itemValue="id"/>
            </form:select>
        </div>
        <div class="form-div">
            <label><spring:message code="report.configuration.visible" /></label>
            <input type="checkbox" name="currentReport.isVisible" />
        </div>

        <%--<ul id="report-ordering-panel" class="report-ordering-panel sortable sortable-tag ui-sortable report-ordering-panel">

        </ul>--%>
        <input name="idOrder" type="hidden" />
        <input name="currentReport.id" type="hidden" />
        <div class="form-div">
            <label><spring:message code="report.configuration.menuElementId" /><span class="requiredlabel">*</span></label>
            <form:input type="text" path="currentReport.menuElementId" cssClass="form-control menuElementId" />
        </div>
        <div class="form-div">
            <label><spring:message code="report.configuration.menuDisplayKey" /><span class="requiredlabel">*</span></label>
            <form:input type="text" path="menuDisplayKey" cssClass="form-control" />
        </div>
        <div class="form-div">
            <label><spring:message code="report.configuration.menuActionUrl" /><span class="requiredlabel">*</span></label>
            <form:input type="text" path="menuActionUrl" cssClass="form-control" />
        </div>
        <div class="form-div">
            <label><spring:message code="report.configuration.attachReportData" /><span class="requiredlabel">*</span></label>
            <form:input type="file" path="reportDataFile" accept=".jasper" cssClass="form-control" />
        </div>

        <div class="form-div">
            <label><spring:message code="report.configuration.attachReportTemplate" /><span class="requiredlabel">*</span></label>
            <form:input type="file" path="reportTemplateFile" accept=".jrxml" cssClass="form-control" />
        </div>

        <button onclick="return save('new-report-form')">
            <spring:message code="label.button.save" />
        </button>
        <button onclick="return cancelEdit()">
            <spring:message code="label.button.cancel" />
        </button>

    </form:form>
</div>

<script>

    function cancelEdit() {
        jQuery('#edit-report-div').hide();
        jQuery('#new-report-div').hide();
        jQuery('#new-report-btn-div').show();
        jQuery('#report-table').show();
        return false;
    }

    function onCategoryChange() {
        /*var type = jQuery('select[name="currentReport.type"]').val();

        if (type == '') {
            jQuery('select[name="currentReport.category"]').val('');
            jQuery('#report-ordering-panel').html('');
            jQuery('#report-ordering-panel').sortable();
            return;
        }*/
        var category = jQuery('select[name="currentReport.category"]:visible').val();
        var id = jQuery('input[name="currentReport.id"]:visible').val();
        var found = false;
        var visibleReports = getVisibleReport(category);
        var html = [];
        for (var i = 0; i < visibleReports.length; i++) {
            var r = visibleReports[i];
            if (r.id == id) {
                found = true;
            }
            html.push('<li class="ui-state-default_oe ui-state-default_oe-tag report-sort-order ' + (r.id == id ? 'current-report' : '') + '" report-id="' + r.id + '"><span class="ui-icon ui-icon-arrowthick-2-n-s"></span>' + r.name + '</li>');
        }
        if (!found) {
            var rep;
            for (var i = 0; i < reports.length; i++) {
                var report = reports[i];
                if (report.id == id) {
                    rep = report;
                }
            }
            if (rep) {
                html.push('<li class="ui-state-default_oe ui-state-default_oe-tag report-sort-order current-report"  report-id="' + id + '"><span class="ui-icon ui-icon-arrowthick-2-n-s"></span>' + rep.name + '</li>');
            }
        }
        jQuery('.report-ordering-panel:visible').html(html.join(''));
        jQuery('.report-ordering-panel:visible').sortable();
    }

    function getVisibleReport(category) {
        var visibleReports = [];
        for (var i = 0; i < reports.length; i++) {
            var report = reports[i];
            if (report.category == category) {
                visibleReports.push(report);
            }
        }
        return visibleReports;
    }

    function editReport(reportId, category) {
        jQuery('#edit-report-div').show();
        jQuery('#new-report-div').hide();
        jQuery('#report-table').hide();
        var visibleReports = getVisibleReport(category);

        var html = [];
        var currentReport;
        for (var i = 0; i < visibleReports.length; i++) {
            var rep = visibleReports[i];
            if (rep.id == reportId) {
                currentReport = rep;
            }
            html.push('<li class="ui-state-default_oe ui-state-default_oe-tag report-sort-order ' + (rep.id == reportId ? 'current-report' : '') + '" report-id="' + rep.id + '"><span class="ui-icon ui-icon-arrowthick-2-n-s"></span>' + rep.name + '</li>');
        }
        if (currentReport) {
            jQuery('input[name="currentReport.name"]').val(currentReport.name);
            jQuery('#currentReportName').html(currentReport.name);
            jQuery('input[name="currentReport.id"]').val(currentReport.id);
            jQuery('select[name="currentReport.category"]').val(currentReport.category);
            jQuery('input[name="currentReport.isVisible"]').prop('checked', currentReport.isVisible == 'true');
        }
        jQuery('#report-ordering-panel').html(html.join(''));
        jQuery('#report-ordering-panel').sortable();
    }

    function newReport() {
        jQuery('#new-report-div').show();
        jQuery('#new-report-btn-div').hide();
        jQuery('#report-table').hide();
    }

    function validateNewReport() {
        var reportDataFile = jQuery('#reportDataFile').val();
        var reportTemplateFile = jQuery('#reportTemplateFile').val();
        var menuActionUrl = jQuery('#menuActionUrl').val();
        var menuElementId = jQuery('.menuElementId').val();
        var menuDisplayKey = jQuery('#menuDisplayKey').val();
        var name = jQuery('.reportName').val();

        if (reportDataFile == '' || reportTemplateFile == '' || menuActionUrl == '' || menuElementId == '' || menuDisplayKey == '' || name == '') {
            return false;
        }
        return true;
    }
    function save(id) {
        var order = jQuery(".report-sort-order:visible");
        var idOrder = [];
        for (var i = 0; i < order.length; i++) {
            var o = order[i];
            var reportId = o.getAttribute('report-id');
            idOrder.push(reportId);
        }
        if (id == 'new-report-form' && !validateNewReport()) {
            alert('Fill in all required fields.')
            return false;
        }

        var form = document.getElementById(id);
        window.onbeforeunload = null; // Added to flag that formWarning alert isn't needed.
        jQuery('input[name="idOrder"]').val(idOrder.join(','));
        form.action = "ReportConfiguration" + _csrf;
        form.submit();
    }

    jQuery(document).ready(function() {
        jQuery('#new-report-div').hide();
        jQuery('#edit-report-div').hide();
    })
</script>