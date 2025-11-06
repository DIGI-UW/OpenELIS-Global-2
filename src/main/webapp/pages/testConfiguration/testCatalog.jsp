<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         import="java.util.List,
         		 java.util.Locale,
                 org.openelisglobal.test.valueholder.TestSection,
                 org.openelisglobal.test.valueholder.TestCatalog,
                 org.openelisglobal.testconfiguration.beans.ResultLimitBean,
                 org.openelisglobal.common.util.IdValuePair,
                 org.openelisglobal.common.action.IActionConstants,
                 org.openelisglobal.internationalization.MessageUtil" %>

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

<c:set var="testSectionList" value="${form.testSectionList}" />
<c:set var="testCatalogList" value="${form.testCatalogList}" />

<%
    String currentTestUnitName = "";
%>

<script type="text/javascript">
    if (!jQuery) {
        var jQuery = jQuery.noConflict();
    }

    function sectionSelection(checkbox) {
        var element = jQuery(checkbox).val();
        if (checkbox.checked) {
            jQuery("#" + element).show();
        } else {
            jQuery("#" + element).hide();
        }
    }

    function sectionSelectionAll(checkbox) {
        var checked = checkbox.checked;
        var element;

        jQuery(".testSection").each(function () {
            element = jQuery(this);
            element.prop('checked', checked);
            if (checked) {
                jQuery("#" + element.val()).show();
            } else {
                jQuery("#" + element.val()).hide();
            }
        })
    }

    function guideSelection(checkbox) {
        if (checkbox.checked) {
            jQuery("#guide").show();
        } else {
            jQuery("#guide").hide();
        }
    }

    function submitAction(target) {
        var form = document.getElementById("mainForm");
        form.action = target;
        form.submit();
    }
</script>

<%
    List<String> testSectionList;
    testSectionList =  (List<String>) pageContext.getAttribute("testSectionList");
    List<TestCatalog> testCatalogList;
    testCatalogList =  (List<TestCatalog>) pageContext.getAttribute("testCatalogList");
%>

<form id="mainForm">
    <input type="button" value="<%= MessageUtil.getContextualMessage("banner.menu.administration") %>"
           onclick="submitAction('MasterListsPage');"
           class="textButton"/> &rarr;
    <input type="button" value="<%= MessageUtil.getContextualMessage("configuration.test.management") %>"
           onclick="submitAction('TestManagementConfigMenu');"
           class="textButton"/>&rarr;
    <spring:message code="configuration.test.catalog" />
</form>
<h1><spring:message code="configuration.test.catalog" /></h1>
<input type="checkbox" onchange="guideSelection(this)"><spring:message code="configuration.test.catalog.guide.show" /><br/><br/>

<div id="guide" style="display: none"><spring:message  htmlEscape="false" code="configuration.test.catalog.guide" /><hr/>
</div>

<h4><spring:message code="configuration.test.catalog.sections" /></h4>
<input type="checkbox" onchange="sectionSelectionAll(this)"><spring:message code="label.all" /><br/><br/>

<% for (String testSection : testSectionList) {%>
<input type="checkbox" class="testSection" value='<%=testSection.replace(" ", "_").replace("/", "_")%>'
       onchange="sectionSelection(this)"><%=testSection%><br/>
<% } %>
<br/>

<%-- This div has to do with the divs in the loop.  The closing div is before the opening div because each change of test unit
needs to be in a div.  This div matches the first time through and there is a closing div at the end of the html
which closes it the last time through--%>
<div>
<% for (TestCatalog bean : testCatalogList) { %>
<hr/>
    <% if (!currentTestUnitName.equals(bean.getTestUnit())) { %>
</div>
<div id='<%=bean.getTestUnit().replace(" ", "_").replace("/", "_")%>' style="display: none">
    <h2><%=bean.getTestUnit()%>
    </h2>
    <hr/>
    <%
            currentTestUnitName = bean.getTestUnit();
        } %>
    <table width="80%">
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
            <td><b><%=bean.getActive()%></b>
            </td>
            <td><b><%=bean.getOrderable()%></b>
            </td>
        </tr>
        <tr>
            <td><span class="catalog-label"><spring:message code="label.test.unit" /></span> <b><%=bean.getTestUnit()%></b>
            </td>
            <td><span class="catalog-label"><spring:message code="label.sample.types" /></span> <b><%=bean.getSampleType()%></b>
            </td>
            <td><span class="catalog-label"><spring:message code="label.panel" /></span> <b><%=bean.getPanel()%></b>
            </td>
            <td><span class="catalog-label"><spring:message code="label.result.type" /></span> <b><%=bean.getResultType()%></b>
            </td>
        </tr>
        <tr>
            <td><span class="catalog-label"><spring:message code="label.uom" /></span> <b><%=bean.getUom()%></b>
            </td>
            <td><span class="catalog-label"><spring:message code="label.significant.digits" /></span> <b><%= bean.getSignificantDigits() %></b>
            </td>
            <td><span class="catalog-label"><spring:message code="label.loinc" /></span> <b><%= bean.getLoinc() %></b>
            </td>
        </tr>
        <% if (bean.isHasDictionaryValues()) {
            boolean top = true;
            for (String value : bean.getDictionaryValues()) {
        %>
        <tr>
            <td><% if (top) { %><span class="catalog-label"><spring:message code="configuration.test.catalog.select.values" /></span><% } %></td>
            <td colspan="2"><b><%=value%></b>
            </td>
            <td colspan="2"><% if (top) {
                top = false;%><span class="catalog-label"><spring:message code="configuration.test.catalog.reference.value" /></span>
                <b><%=bean.getReferenceValue()%></b>
            </td>
            <% } %>
        </tr>
        <%
                }
            }
        %>
        <% if (bean.isHasLimitValues()) { %>
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
        <% for (ResultLimitBean limitBean : bean.getResultLimits()) {%>
        <tr>
            <td><b><%=limitBean.getGender()%></b>
            </td>
            <td><b><%=limitBean.getAgeRange()%></b>
            </td>
            <td><b><%=limitBean.getNormalRange()%></b>
            </td>
            <td><b><%=limitBean.getValidRange()%></b>
            </td>
            <td><b><%=limitBean.getReportingRange()%></b>
            </td>
            <td><b><%=limitBean.getCriticalRange()%></b>
            </td>
        </tr>
        <% } %>
        <% } %>
    </table>
<%} %>
    </div>


