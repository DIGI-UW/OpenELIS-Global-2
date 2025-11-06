<%--
  Created by IntelliJ IDEA.
  User: kenny
  Date: 2019-09-15
  Time: 12:48
  To change this template use File | Settings | File Templates.
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
         import="org.openelisglobal.common.util.Versioning" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



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

<script>
    var nceTypes= [];
    <c:forEach items="${form.nceTypes}" var="nceType">
    var nceType = {};
    nceType['name'] = '${nceType.name}';
    nceType['id'] = '${nceType.id}';
    nceType['categoryId'] = '${nceType.categoryId}';
    nceTypes.push(nceType);
    </c:forEach>
    
</script>

<div align="center">
    <h2><spring:message code="nonconforming.page.view.title" /></h2>

    <select id="searchCriteria" tabindex="1" class="patientSearch">
        <option value="<c:out value="1" />"><spring:message code="nonconforming.event.laborderNumber" /></option>
        <option value="<c:out value="2" />"><spring:message code="nonconforming.event.ncenumber" /></option>
    </select>
    <input type="text" name="searchValue"
           value="" oninput="enableSearch()" id="searchValue">
    &nbsp;
    <button type="button" id="searchButtonId"
           onclick="searchNCE();" disabled><spring:message code="label.button.search" /></button>

    <table id="searchResults">

    </table>

</div>
<c:if test="${not empty form.labOrderNumber}">
<h2><spring:message code="nonconforming.page.followUp.title" /></h2>
<form:hidden path="id" />
<form:hidden path="currentUserId" />
<form:hidden path="status" />
<form:hidden path="reporterName" />
<form:hidden path="site" />
<form:hidden path="severityScore" />
<form:hidden path="colorCode" />

<table class="full-table">
    <tr class="view-non-conforming-section-1">
        <td><spring:message code="nonconforming.event.ncenumber" /></td>
        <td><form:hidden path="nceNumber" /><c:out value="${form.reportDate}" /></td>

        <td><spring:message code="nonconforming.page.followUp.reportDate" /></td>
        <td><form:hidden path="reportDate" /><c:out value="${form.reportDate}" /></td>
    </tr>
    <tr>
        <td><spring:message code="nonconforming.page.followUp.ncedate" /></td>
        <td><form:hidden path="dateOfEvent" /><c:out value="${form.dateOfEvent}" /></td>

        <td><spring:message code="nonconforming.page.followUp.reportingPerson" /></td>
        <td><form:hidden path="name" /><c:out value="${form.reporterName}" /></td>
    </tr>

    <tr>
        <td><spring:message code="nonconforming.labOrderNumber" /></td>
        <td><form:hidden path="labOrderNumber" /><c:out value="${form.labOrderNumber}" /></td>

        <td><spring:message code="nonconforming.event.reportingUnit" /></td>
        <td>
            <form:hidden path="reportingUnit" />
            <c:forEach items="${form.reportingUnits}" var="reportingUnit">
                <c:if test="${reportingUnit.id == form.reportingUnit}" >
                    <c:out value="${reportingUnit.value}" />
                </c:if>
            </c:forEach>
        </td>
    </tr>
    <tr>
        <td><spring:message code="nonconforming.event.specimen" /></td>
        <td>
            <form:hidden path="specimen" />
            <c:forEach items="${form.specimens}" var="specimen" varStatus="counter">
                <c:if test="${counter.index != 0}" >
                    <c:out value=", " />
                </c:if>
                <c:out value="${specimen.typeOfSample.description}" />
            </c:forEach>
        </td>
    </tr>
    <tr>
        <td><spring:message code="nonconforming.event.prescriberNameAndSite" /></td>
        <td><form:hidden path="prescriberName" /><c:out value="${form.prescriberName}" /> - <c:out value="${form.site}" /></td>

        <td><spring:message code="nonconforming.page.followUp.laboratoryComponent" /></td>
        <td>
            <form:select path="laboratoryComponent" onchange="checkIfValid()" >
                <form:option value="">Select one</form:option>
                <form:options items="${form.labComponentList}" itemLabel="value" itemValue="id" />
            </form:select>
        </td>
    </tr>
</table>

<table class="full-table">
    <tr>
        <td class="view-non-conforming-descriptions">
            <spring:message code="nonconforming.page.followUp.description" />
            <p><form:hidden path="description" /><c:out value="${form.description}" /></p>
        </td>
        <td>
            <strong><spring:message code="nonconforming.page.followUp.nceType" /></strong>
            <table class="full-table">
                <tr>
                    <td class="half-table"><spring:message code="nonconforming.page.followUp.nceCategory" /></td>
                    <td>
                        <p>
                            <form:select path="nceCategory" id="nceCategory" onchange="resetNceType()">
                                <option value="">Select one</option>
                                <form:options items="${form.nceCategories}" itemLabel="name" itemValue="id" />
                            </form:select>
                        </p>
                    </td>
                </tr>
                <tr>
                    <td class="half-table"><spring:message code="nonconforming.page.followUp.nceType" /></td>
                    <td>
                        <p>
                            <form:select path="nceType" id="nceType" onchange="checkIfValid()">
                                <option value="">Select one</option>
                                <form:options items="${form.nceTypes}" itemLabel="name" itemValue="id" />
                            </form:select>
                        </p>
                    </td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>
            <spring:message code="nonconforming.page.followUp.suspectedCause" />
            <p><form:hidden path="suspectedCauses" /><c:out value="${form.suspectedCauses}" /></p>
        </td>
        <td>
            <label><strong><spring:message code="nonconforming.page.followUp.severity" /></strong></label>
            <table class="full-table">
                <tr>
                    <td class="half-table"><spring:message code="nonconforming.page.followUp.howSevere" /></td>
                    <td>
                        <p>
                            <form:select path="consequences" onchange="calculateSeverityScore()">
                                <form:options items="${form.severityConsequencesList}" itemLabel="value" itemValue="id" />
                            </form:select>
                        </p>
                    </td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>
            <spring:message code="nonconforming.page.followUp.proposedAction" />
            <p><form:hidden path="proposedAction" /><c:out value="${form.proposedAction}" /></p>
        </td>
        <td>
            <table class="full-table">
                <tr>
                    <td class="half-table"><spring:message code="nonconforming.page.followUp.likelyRecurrence" /></td>
                    <td>
                        <p>
                            <form:select path="recurrence" onchange="calculateSeverityScore()">
                                <form:options items="${form.severityRecurrenceList}" itemLabel="value" itemValue="id" />
                            </form:select>
                        </p>
                    </td>
                </tr>
                <tr>
                    <td colspan="2">
                        <p><spring:message code="nonconforming.page.followUp.lowSeverity" /></p>
                        <p><spring:message code="nonconforming.page.followUp.highSeverity" /></p>
                    </td>
                </tr>
                <tr>
                    <td class="half-table"><spring:message code="nonconforming.page.followUp.severityScore" /> </td>
                    <td>
                        <strong><span id="severityScoreLabel"><c:out value="${form.severityScore}" /></span></strong>
                    </td>
                </tr>
            </table>


        </td>
    </tr>
</table>

<table class="full-table">
    <caption class="center-caption"><spring:message code="nonconforming.page.followUp.correctiveAction" /></caption>
    <tr>
        <td>
            <spring:message code="nonconforming.page.followUp.correctiveActionDescription" />
            <p><form:textarea path="correctiveAction" onchange="checkIfValid()" rows="5"></form:textarea></p>
        </td>
    </tr>
    <tr>
        <td>
            <spring:message code="nonconforming.page.followUp.prevConcActionDescription" />
            <p><form:textarea path="controlAction" rows="5"></form:textarea></p>
        </td>
    </tr>
    <tr>
        <td>
            <spring:message code="nonconforming.page.followUp.comments" />
            <p><form:textarea path="comments" rows="5"></form:textarea></p>
        </td>
    </tr>
</table>
<div class="center-caption">
    <button id="saveButtonId" onclick="savePage()">
        <spring:message code="button.label.submit" />
    </button>
</div>
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

    /**
     *  Saves the form.
     */
    function savePage() {
        var form = document.getElementById("mainForm");
        window.onbeforeunload = null; // Added to flag that formWarning alert isn't needed.
        form.action = "ViewNonConformingEvent";
        form.submit();
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
        var nceNumber = "";
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
                    "&labNumber=" + labNumber + "&status=Pending" +
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
        console.log(xhr)
        var formField = xhr.responseXML.getElementsByTagName("formfield").item(0);
        var message = xhr.responseXML.getElementsByTagName("message").item(0);

        if (message.firstChild.nodeValue == "valid") {
            jQuery("#searchResults").html("<tr><th>Date</th><th>NCE Number</th><th>Lab Section/Unit</th></tr>");
            var resultNodes = formField.getElementsByTagName("nce");
            if (resultNodes.length == 1) {
                var node = resultNodes[0];
                var nceNumberEl = node.getElementsByTagName('ncenumber').item(0);
                var nceNumber = (nceNumberEl ? nceNumberEl.firstChild.nodeValue : "#")
                if (nceNumber != '#') {
                    window.location = 'ViewNonConformingEvent?nceNumber=' + nceNumber;
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
        var nceNumber = (nceNumberEl ? nceNumberEl.firstChild.nodeValue : "#")
        var row = '<tr><td>' + (date ? date.firstChild.nodeValue : "") + '</td>' +
            '<td><a href="ViewNonConformingEvent?nceNumber=' + nceNumber + '">' + nceNumber + '</a></td>' +
            '<td>' + (unit && unit.firstChild ? unit.firstChild.nodeValue : "") + '</td>' +
            '</tr><tr>';
        return row;
    }

    function checkIfValid() {
        var correctiveAction = jQuery('input[name="correctiveAction"]').val();
        var consequences = jQuery('input[name="consequences"]').val();
        var nceType = jQuery('input[name="nceType"]').val();
        var nceCategory = jQuery('input[name="nceCategory"]').val();
        var laboratoryComponent = 'd'; //jQuery('input[name="laboratoryComponent"]').val();
        setSave(true);
        if (correctiveAction != '' && consequences !== '' && nceType !== '' && nceCategory !== '' && laboratoryComponent != '') {
            setSave(false);
        }
    }
    
    function resetNceType() {
        var nceCategory = jQuery('select[name="nceCategory"]').val();
        var el = document.getElementById("nceType");
        var j = el.options.length - 1;
        while (j > 0) {
            el.remove(j);
            j--;
        }

        for (var i = 0; i < nceTypes.length; i++) {
            var nce = nceTypes[i];
            if (nce.categoryId == nceCategory) {
                el.append(new Option(nce.name, nce.id));
            }
        }
    }

    function calculateSeverityScore() {
        var consequences = jQuery('select[name="consequences"]').val();
        var recurrence = jQuery('select[name="recurrence"]').val();
        if (consequences != '' && recurrence != '') {
            var score = Number(consequences) * Number(recurrence);
            document.getElementById("severityScoreLabel").innerHTML = score;
            document.getElementById("severityScore").value = score;
            if (score >= 1 && score <= 3) {
                document.getElementById("colorCode").value = 'Green';
            } else if (score >= 4 && score <= 6) {
                document.getElementById("colorCode").value = 'Yellow';
            } else if (score >= 7 && score <= 9) {
                document.getElementById("colorCode").value = 'Red';
            }
        }
        checkIfValid();
    }

    jQuery(document).ready( function() {
        setSave(true);
        
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