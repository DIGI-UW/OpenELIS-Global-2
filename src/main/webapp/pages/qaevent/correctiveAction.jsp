<%--
  Created by IntelliJ IDEA.
  User: kenny
  Date: 2019-09-15
  Time: 13:50
  To change this template use File | Settings | File Templates.
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
         import="
                org.openelisglobal.common.util.Versioning" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Calendar" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>



<%
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    String today = df.format(Calendar.getInstance().getTime());
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
<div align="center">
    <h2><spring:message code="nonconforming.page.correctiveAction.title" /></h2>

    <select id="searchCriteria" tabindex="1" class="patientSearch">
        <option value="<c:out value="1" />">Lab Order Number</option>
        <option value="<c:out value="2" />">NCE Number</option>
    </select>
    <input type="text" name="searchValue" oninput="enableSearch()" id="searchValue">
    &nbsp;
    <button type="button" id="searchButtonId"
           onclick="searchNCE();" disabled ><spring:message code="label.button.search" /></button>

    <table id="searchResults">

    </table>

</div>
<c:if test="${not empty form.id}">
    <script>
        var actionTypes= [];
        <c:forEach items="${form.actionTypeList}" var="actionType">
        var at = {};
        at['value'] = '${actionType.value}';
        at['id'] = '${actionType.id}';
        actionTypes.push(at);
        </c:forEach>

        var actionLogs = [];
        <c:forEach items="${form.actionLog}" var="actionLog">
            var al = {};
            al['id'] = '${actionLog.id}';
            al['correctiveAction'] = '${actionLog.correctiveAction}';
            al['actionType'] = '${actionLog.actionType}';
            al['personResponsible'] = '${actionLog.personResponsible}';
            al['dateCompleted'] = '${actionLog.dateCompleted}';
            al['turnAroundTime'] = '${actionLog.turnAroundTime}';
            actionLogs.push(al);
        </c:forEach>
    </script>
    <h2><spring:message code="nonconforming.page.correctiveAction.title" /></h2>
    <form:hidden path="id" />
    <form:hidden path="currentUserId" />
    <form:hidden path="status" />
    <form:hidden path="reportDate" />
    <form:hidden path="reporterName" />
    <form:hidden path="prescriberName" />
    <form:hidden path="site" />
    <form:hidden path="nceNumber" />
    <form:hidden path="dateOfEvent" />
    <form:hidden path="labOrderNumber" />
    <form:hidden path="specimen" />
    <form:hidden path="reportingUnit" />
    <form:hidden path="description" />
    <form:hidden path="suspectedCauses" />
    <form:hidden path="proposedAction" />
    <form:hidden path="laboratoryComponent" />
    <form:hidden path="nceCategory" />
    <form:hidden path="nceType" />
    <form:hidden path="consequences" />
    <form:hidden path="recurrence" />
    <form:hidden path="severityScore" />
    <form:hidden path="colorCode" />
    <form:hidden path="correctiveAction" />
    <form:hidden path="controlAction" />
    <form:hidden path="comments" />
    <form:hidden path="discussionDate" />
    <form:hidden path="actionLogStr" />

    <table class="corrective-action-section-1">
        <tr>
            <td class="half-table"><spring:message code="nonconforming.event.ncenumber" /></td>
            <td><c:out value="${form.nceNumber}" /></td>
        </tr>
        <tr>
            <td class="half-table"><spring:message code="nonconforming.page.followUp.ncedate" /></td>
            <td><c:out value="${form.dateOfEvent}" /></td>
        </tr>
        <tr>
            <td class="half-table"><spring:message code="nonconforming.page.followUp.severity" /></td>
            <td>
                <c:forEach items="${form.severityConsequencesList}" var="severity">
                    <c:if test="${severity.id == form.consequences}" >
                        <c:out value="${severity.value}" />
                    </c:if>
                </c:forEach>
            </td>
        </tr>

        <tr>
            <td class="half-table"><spring:message code="nonconforming.page.followUp.reportingPerson" /></td>
            <td><c:out value="${form.name}" /></td>
        </tr>
        <tr>
            <td class="half-table"><spring:message code="nonconforming.page.correctiveAction.reportingDate" /></td>
            <td><c:out value="${form.reportDate}" /></td>
        </tr>
        <tr>
            <td class="half-table"><spring:message code="nonconforming.event.reportingUnit" /></td>
            <td>
                <c:forEach items="${form.reportingUnits}" var="reportingUnit">
                    <c:if test="${reportingUnit.id == form.reportingUnit}" >
                        <c:out value="${reportingUnit.value}" />
                    </c:if>
                </c:forEach>
            </td>
        </tr>

        <tr>
            <td class="half-table"><spring:message code="nonconforming.event.laborderNumber" /></td>
            <td><c:out value="${form.labOrderNumber}" /></td>
        </tr>
        <tr>
            <td class="half-table"><spring:message code="nonconforming.event.specimen" /></td>
            <td><c:out value="${form.specimen}" /></td>
        </tr>
        <tr>
            <td class="half-table"><spring:message code="nonconforming.page.correctiveAction.laboratoryComponent" /></td>
            <td>
                <c:forEach items="${form.labComponentList}" var="laboratoryComponent">
                    <c:if test="${laboratoryComponent.id == form.laboratoryComponent}" >
                        <c:out value="${laboratoryComponent.value}" />
                    </c:if>
                </c:forEach>
            </td>
        </tr>
        <tr>
            <td class="half-table"><spring:message code="nonconforming.page.followUp.nceCategory" /></td>
            <td>
                <c:forEach items="${form.nceCategories}" var="nceCategory">
                    <c:if test="${nceCategory.id == form.nceCategory}" >
                        <c:out value="${nceCategory.name}" />
                    </c:if>
                </c:forEach>
            </td>
        </tr>
        <tr>
            <td class="half-table"><spring:message code="nonconforming.page.followUp.nceType" /></td>
            <td>
                <c:forEach items="${form.nceTypes}" var="nceType">
                    <c:if test="${nceType.id == form.nceType}" >
                        <c:out value="${nceType.name}" />
                    </c:if>
                </c:forEach>
            </td>
        </tr>
    </table>

    <table class="corrective-action-section-2">
        <tr>
            <td>
                <spring:message code="nonconforming.page.correctiveAction.plannedAction" />
                 <p><c:out value="${form.correctiveAction}" /></p>
            </td>
        </tr>
        <tr>
            <td>
                <spring:message code="nonconforming.page.correctiveAction.plannedPreventive" />
                <p><c:out value="${form.controlAction}" /></p>
            </td>
        </tr>
        <tr>
            <td>
                <spring:message code="nonconforming.page.followUp.comments" />
                <p><c:out value="${form.comments}" /></p>
            </td>
        </tr>
    </table>

    <table class="full-table">
        <caption><spring:message code="nonconforming.page.correctiveAction.log" /></caption>
        <tr>
            <td class="column-right-text"><spring:message code="nonconforming.page.correctiveAction.discussionDate" /></td>
            <td id="discussionDatesView"><c:out value="${form.discussionDate}" /></td>
        </tr>
        <tr>
            <td></td>
            <td>
                <input id="new-discussion-date" placeholder="dd/MM/yyyy"/>
            </td>
        </tr>
        <tr>
            <td></td>
            <td>
                <button id="addNewDate" >Add new date</button>
            </td>
        </tr>
    </table>
    <table class="full-table" id="action-log-table">
        <tr>
            <th>Corrective action</th>
            <th><spring:message code="nonconforming.page.correctiveAction.actionType" /></th>
            <th>Person responsible</th>
            <th>Date Completed</th>
            <th>Turnaround time</th>
        </tr>
        <c:forEach items="${form.actionLog}" var="actionLog" varStatus="loop">
            <tr><input type="hidden" name="action-log-id-${loop.index}" value="${actionLog.id}" />
                <td><textarea name="correctiveAction-log-${loop.index}" onchange="checkIfValid()" readonly><c:out value="${actionLog.correctiveAction}"/></textarea></td>
                <td class="action-type">
                    <c:forEach items="${form.actionTypeList}" var="actionType">
                        <input  disabled type="checkbox" value="${actionType.id}" name="action-type-${loop.index}" checked="${actionLog.actionType.indexOf(actionType.id) != -1}" onchange="checkIfValid()"/>${actionType.value}<br />
                    </c:forEach>
                </td>
                <td><input type="text" name="responsiblePerson-${loop.index}" onchange="checkIfValid()" readonly value="<c:out value="${actionLog.personResponsible}"/>"/></td>
                <td>
                    <input type="text" name="dateActionCompleted-${loop.index}" id="dateActionCompleted-${loop.index}" readonly max="<%= today%>" value="<c:out value="${actionLog.dateCompleted}"/>"/>
                </td>
                <td>
                    <input type="hidden" name="turnAroundTime-${loop.index}" id="turnAroundTime-${loop.index}" />
                    <span id="turnAroundTimeSpan-${loop.index}"><c:out value="${actionLog.turnAroundTime}"/></span>
                </td>
            </tr>
        </c:forEach>
            <tr index="${fn:length(form.actionLog)}">
                <td><textarea name="correctiveAction-log-${fn:length(form.actionLog)}" onchange="checkIfValid()"></textarea></td>
                <td class="action-type">
                    <c:forEach items="${form.actionTypeList}" var="actionType">
                        <input  type="checkbox" value="${actionType.id}" onchange="checkIfValid()" name="action-type-${fn:length(form.actionLog)}"/>${actionType.value}<br />
                    </c:forEach>
                </td>
                <td><input type="text" name="responsiblePerson-${fn:length(form.actionLog)}" onchange="checkIfValid()"/></td>
                <td>
                    <input type="text" name="dateActionCompleted-${fn:length(form.actionLog)}"  id="dateActionCompleted-${fn:length(form.actionLog)}" max="<%= today%>" placeholder="dd/MM/yyyy" />
                </td>
                <td>
                    <input type="hidden" name="turnAroundTime-${fn:length(form.actionLog)}" id="turnAroundTime-${fn:length(form.actionLog)}" />
                    <span id="turnAroundTimeSpan-${fn:length(form.actionLog)}"></span>
                </td>
            </tr>
    </table>
    <div class="center-caption"><button id="saveButtonId" onclick="return savePage()"><spring:message code="label.button.save" /></button></div>
    <table class="full-table">
        <caption><spring:message code="nonconforming.page.correctiveAction.nceResolution" /></caption>
        <tr>
            <td></td>
        </tr>
        <tr>
            <td colspan="2">
                <spring:message code="nonconforming.page.correctiveAction.nceResolutionLabel" />
                <form:radiobutton name="effective" path="effective" value="Yes" onclick="checkNCE"/>
                <spring:message code="nonconforming.page.correctiveAction.yes" />
                <form:radiobutton name="effective" path="effective" value="No" onclick="checkNCE"/>
                <spring:message code="nonconforming.page.correctiveAction.no" />
            </td>
        </tr>
        <tr>
            <td><spring:message code="nonconforming.page.correctiveAction.signature" /></td>
            <td>
                <spring:message code="nonconforming.page.correctiveAction.dateCompleted" />
                <form:input path="dateCompleted" id="dateCompleted" type="text" placeholder="dd/MM/yyyy" onchange="checkIfValid()"/>
            </td>
        </tr>
        <tr id="followUpAlert" style="display: none"><td colspan="2"><spring:message code="nonconforming.page.correctiveAction.followUpAlert" /></td></tr>
    </table>
    <div class="center-caption"><button id="submitResolved" onclick="completeNCE()">
        <spring:message code="nonconforming.page.correctiveAction.submit" />
    </button></div>
</c:if>

<script type="text/javascript">
    function setSave(disabled) {
        if (document.getElementById("saveButtonId")) {
            document.getElementById("saveButtonId").disabled = disabled;
        }
    }

    function enableSearch() {
        document.getElementById("searchButtonId").disabled = document.getElementById("searchValue").value === "";
    }

    function setActionLog() {
        var actionLog = [];
        var rowCount = document.getElementById('action-log-table').rows.length;
        for (var i = 1; i < rowCount; i++) {
            var id = jQuery('input[name="action-log-id-' + (i - 1) + '"]').val();
            var correctiveAction = jQuery('textarea[name="correctiveAction-log-' + (i -1) + '"]').val();
            var turnAroundTime = jQuery('input[name="turnAroundTime-' + (i -1) + '"]').val();
            var dateCompleted = jQuery('input[name="dateActionCompleted-'  + (i -1) +  '"]').val();
            var responsiblePerson = jQuery('input[name="responsiblePerson-'  + (i -1) +  '"]').val();
            var at = jQuery('input[name="action-type-'  + (i -1) +  '"]:checked');
            var actionType = [];
            for (var j = 0; j < at.length; j++) {
                actionType.push(at[j].value)
            }
            var xml = "<actionLog><correctiveAction>" + correctiveAction + "</correctiveAction>" +
                "<turnAroundTime>" + turnAroundTime + "</turnAroundTime><dateCompleted>" + dateCompleted +
                "</dateCompleted><personResponsible>" + responsiblePerson + "</personResponsible>" +
                "<actionType>" + actionType.join(",") + "</actionType>";
            if (id && id != '') {
                xml += '<id>' + id + '</id>';
            }
            xml += '</actionLog>';
            actionLog.push(xml);
        }
        document.getElementById("actionLogStr").value = "<actionLogs>" + actionLog.join("") + "</actionLogs>";
    }
    /**
     *  Saves the form.
     */
    function savePage(e) {
        var form = document.getElementById("mainForm");
        window.onbeforeunload = null; // Added to flag that formWarning alert isn't needed.
        setActionLog();
        form.action = "NCECorrectiveAction";
        form.submit();
    }

    function checkNCE(e) {
        var disabled = true;
        if (e.target.checked && e.target.value === 'Yes') {
            disabled = false;
        }
        document.getElementById("dateCompleted").disabled = disabled;
        document.getElementById("submitResolved").disabled = disabled;
        if (disabled) {
            document.getElementById("followUpAlert").style.display = '';
        } else {
            document.getElementById("followUpAlert").style.display = 'none';
        }
    }

    function completeNCE() {
        if (validatePastDate('dateCompleted')) {
            var form = document.getElementById("mainForm");
            window.onbeforeunload = null; // Added to flag that formWarning alert isn't needed.
            setActionLog();
            form.action = "ResolveNonConformingEvent";
            form.submit();
        }
        return false;
    }
    /**
     *  Enable/Disable search button based on input
     */
    function enableSearch() {
        document.getElementById("searchButtonId").disabled = document.getElementById("searchValue").value === "";
    }

    function searchNCE() {
        var criteria = jQuery("#searchCriteria").val();
        var value = jQuery("#searchValue").val();
        var nceNumber;
        var labNumber = "";


        if (criteria == 2) {
            nceNumber =  value.trim();
        } else if (criteria == 1) {
            labNumber = value;
        }

        nceSearch(nceNumber, labNumber, "", false, processSearchSuccess);
    }

    function nceSearch(nceNumber, labNumber, guid, suppressExternalSearch, success, failure){
        if( !failure){failure = defaultFailure;	}
        new Ajax.Request (
            'ajaxQueryXML',  //url
            {//options
                method: 'get', //http method
                parameters: "provider=NonConformingEventSearchProvider&nceNumber=" + nceNumber +
                    "&labNumber=" + labNumber + "&status=CAPA" +
                    "&suppressExternalSearch=" + suppressExternalSearch,
				requestHeaders : {
    					"X-CSRF-Token" : getCsrfToken()
    			},
                onSuccess:  success,
                onFailure:  failure
            }
        );
    }

    function processSearchSuccess(xhr) {
        var formField = xhr.responseXML.getElementsByTagName("formfield").item(0);
        var message = xhr.responseXML.getElementsByTagName("message").item(0);

        if (message.firstChild.nodeValue == "valid") {
            jQuery("#searchResults").html("<tr><th>Date</th><th>NCE Number</th><th>Lab Section/Unit</th><th>Severity (Color)</th></tr>");
            var resultNodes = formField.getElementsByTagName("nce");
            if (resultNodes.length == 1) {
                var node = resultNodes[0];
                var nceNumberEl = node.getElementsByTagName('ncenumber').item(0);
                var nceNumber = (nceNumberEl ? nceNumberEl.firstChild.nodeValue : "#")
                if (nceNumber != '#') {
                    window.location = 'NCECorrectiveAction?nceNumber=' + nceNumber;
                }
            }
            for (var i = 0; i < resultNodes.length; i++) {
                document.getElementById("searchResults").insertAdjacentHTML('beforeend', addNCERow(resultNodes[i]));
            }

        } else {
            jQuery("#searchResults").html("<tr><th>No valid results</th></tr>");
        }
    }

    function addNCERow(nce) {
        var date = nce.getElementsByTagName("date").item(0);
        var nceNumberEl = nce.getElementsByTagName('ncenumber').item(0);
        var unit = nce.getElementsByTagName('unit').item(0);
        var colorCode = nce.getElementsByTagName('color').item(0);
        var nceNumber = (nceNumberEl ? nceNumberEl.firstChild.nodeValue : "#")
        var row = '<tr><td>' + (date ? date.firstChild.nodeValue : "") + '</td>' +
            '<td><a href="NCECorrectiveAction?nceNumber=' + nceNumber + '">' + nceNumber + '</a></td>' +
            '<td>' + (unit && unit.firstChild ? unit.firstChild.nodeValue : "") + '</td>' +
            '<td>' + (colorCode ? colorCode.firstChild.nodeValue : "") + '</td>' +
            '</tr><tr>';
        return row;
    }

    function checkIfValid() {

        var valid = true;
        var rowCount = document.getElementById('action-log-table').rows.length;
        for (var i = 1; i < rowCount; i++) {
            var id = jQuery('input[name="action-log-id-' + (i - 1) + '"]').val();
            var correctiveAction = jQuery('textarea[name="correctiveAction-log-' + (i -1) + '"]').val();
            var turnAroundTime = jQuery('input[name="turnAroundTime-' + (i -1) + '"]').val();
            var dateCompleted = jQuery('input[name="dateActionCompleted-'  + (i -1) +  '"]').val();
            var responsiblePerson = jQuery('input[name="responsiblePerson-'  + (i -1) +  '"]').val();
            var at = jQuery('input[name="action-type-'  + (i -1) +  '"]:checked');
            var actionType = [];
            for (var j = 0; j < at.length; j++) {
                actionType.push(at[j].value)
            }
            actionType = actionType.join(",");
            if (correctiveAction == '' || discussionDate == '' || turnAroundTime == '' || dateCompleted == '' || responsiblePerson == '' || actionType == '') {
                valid = false;
            }
            if(!validatePastDate('dateActionCompleted-'  + (i - 1) )) {
                valid = false;
            }
        }
        if (valid) {
            var discussiondate = document.getElementById("discussionDate").value;
            if (discussiondate == '') {
                valid = false;
            }
        }
        setSave(true);
        if (valid) {
            setSave(false);
        }
    }



    function addNewDate(e) {
        e.preventDefault();
        var date = document.getElementById("new-discussion-date").value;
        var discussionDate = document.getElementById("discussionDate").value;
        if (date !== '') {
            var dates = [];
            if (discussionDate != '') {
                dates = discussionDate.split(',');
            }
            dates.push(date);
            document.getElementById("discussionDate").value = dates.join(',');
            document.getElementById("discussionDatesView").innerHTML = dates.join(',');
            checkIfValid();
        }
    }

    function calculateTurnAroundTime(e) {
        var days = '';
        if (e.target.value !== "") {
            var reportDate = document.getElementById("reportDate").value.split('/');
            var date = new Date(reportDate[2] + '-' + reportDate[1] + '-' + reportDate[0]);
            var completeDate = new Date(e.target.value);
            days = (completeDate.getTime() - date.getTime()) / (1000 * 60 * 60 * 24);
        }
        var index = e.target.name.substring(20);
        document.getElementById("turnAroundTime-" + index).value = days;
        document.getElementById("turnAroundTimeSpan-" + index).innerHTML = days + ' days';
        checkIfValid();
    }

    function validatePastDate(name) {
        var date = new Date();
        var doe = jQuery('input[name="' + name + '"]').val().split('/');
        if(doe.length != 3) {
            return false;
        }
        var d = new Date(doe[1] + '/' + doe[0] + '/' + doe[2]);
        if (d < date) {
            return true;
        }
        return false;
    }
    jQuery(document).ready( function() {
        setSave(true);
        if (document.getElementById("addNewDate")) {
            document.getElementById("addNewDate").addEventListener("click", addNewDate);
        }
        // document.getElementById("dateCompleted").addEventListener("input", calculateTurnAroundTime);
        if (document.getElementById("submitResolved")) {
            document.getElementById("submitResolved").disabled = true;
        }
        jQuery('input[name*="dateActionCompleted-"]').on('change', calculateTurnAroundTime)
        var effectiveRadios = document.getElementsByName("effective");
        for (var i = 0; i < effectiveRadios.length; i++) {
            effectiveRadios[i].addEventListener("input", checkNCE);
        }
        /*if (actionTypes) {
            for (var i = 0; i < actionTypes.length; i++) {
               // jQuery('.action-type').append('<input type="checkbox" value="' + actionTypes[i].id + '" name="actionType" />' + actionTypes[i].value + '<br/>');
            }
        } */
        
        document.getElementById("searchValue").addEventListener('keydown', function(e){
        	
            if (document.getElementById("searchValue").value === '') return;
            // e.which === 13 means enter key is pressed.
            if (e.which === 13) {
            	e.preventDefault();
            	searchNCE();
            }
        });
    });

</script>
