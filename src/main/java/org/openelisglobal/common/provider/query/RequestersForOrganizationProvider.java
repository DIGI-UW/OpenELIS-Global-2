/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.common.provider.query;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.openelisglobal.common.servlet.validation.AjaxServlet;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.common.util.XMLUtil;
import org.openelisglobal.organization.service.OrganizationContactService;
import org.openelisglobal.organization.valueholder.OrganizationContact;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.spring.util.SpringContext;

public class RequestersForOrganizationProvider extends BaseQueryProvider {

    protected OrganizationContactService organizationContactService = SpringContext
            .getBean(OrganizationContactService.class);

    protected AjaxServlet ajaxServlet = null;

    @Override
    public void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String orgId = request.getParameter("orgId");

        StringBuilder xml = new StringBuilder();

        createXMLOfRequesters(orgId, xml);

        ajaxServlet.sendData(xml.toString(), VALID, request, response);
    }

    private void createXMLOfRequesters(String orgId, StringBuilder xml) {
        List<OrganizationContact> orgContactList = organizationContactService.getListForOrganizationId(orgId);
        xml.append("<requesters>");
        for (OrganizationContact orgContact : orgContactList) {
            createXMLOfRequester(orgContact.getPerson(), xml);
        }
        xml.append("</requesters>");
    }

    private void createXMLOfRequester(Person person, StringBuilder xml) {
        xml.append("<requester ");

        XMLUtil.appendAttributeKeyValue("id", StringUtil.trim(person.getId()), xml);
        // xml.append("id=\"");
        // xml.append(StringUtil.trim(person.getId()));
        // xml.append("\" ");

        XMLUtil.appendAttributeKeyValue("firstName", StringUtil.trim(person.getFirstName()), xml);
        // xml.append("firstName=\"");
        // xml.append(StringUtil.trim(person.getFirstName()));
        // xml.append("\" ");

        XMLUtil.appendAttributeKeyValue("lastName", StringUtil.trim(person.getLastName()), xml);
        // xml.append("lastName=\"");
        // xml.append(StringUtil.trim(person.getLastName()));
        // xml.append("\" ");

        XMLUtil.appendAttributeKeyValue("phone", StringUtil.trim(person.getWorkPhone()), xml);
        // xml.append("phone=\"");
        // xml.append(StringUtil.trim(person.getWorkPhone()));
        // xml.append("\" ");
        XMLUtil.appendAttributeKeyValue("fax", StringUtil.trim(person.getFax()), xml);
        // xml.append("fax=\"");
        // xml.append(StringUtil.trim(person.getFax()));
        // xml.append("\" ");

        XMLUtil.appendAttributeKeyValue("email", StringUtil.trim(person.getEmail()), xml);
        // xml.append("email=\"");
        // xml.append(StringUtil.trim(person.getEmail()));
        // xml.append("\" ");

        xml.append(" />");
    }

    @Override
    public void setServlet(AjaxServlet as) {
        this.ajaxServlet = as;
    }

    @Override
    public AjaxServlet getServlet() {
        return this.ajaxServlet;
    }
}
