package org.openelisglobal.common.provider.query;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.servlet.validation.AjaxServlet;
import org.openelisglobal.common.util.XMLUtil;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.search.service.SearchResultsService;
import org.openelisglobal.spring.util.SpringContext;

public class NCESampleSearchProvider extends BaseQueryProvider {

    protected AjaxServlet ajaxServlet = null;

    SampleService sampleService = SpringContext.getBean(SampleService.class);
    SampleItemService sampleItemService = SpringContext.getBean(SampleItemService.class);
    SampleHumanService sampleHumanService = SpringContext.getBean(SampleHumanService.class);
    protected SearchResultsService searchResultsService = SpringContext.getBean(SearchResultsService.class);

    @Override
    public void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String lastName = request.getParameter("lastName");
        String firstName = request.getParameter("firstName");
        String STNumber = request.getParameter("STNumber");
        // N.B. This is a bad name, it is other than STnumber
        String subjectNumber = request.getParameter("subjectNumber");
        String nationalID = request.getParameter("nationalID");
        String labNumber = request.getParameter("labNumber");
        String guid = request.getParameter("guid");
        // String suppressExternalSearch =
        // request.getParameter("suppressExternalSearch");
        String patientID = null;

        String result = VALID;
        StringBuilder xml = new StringBuilder();
        List<Sample> searchResults = new ArrayList<>();
        // If we have a lab number then the patient is in the system and we just
        // have to get the patient and format the xml
        if (!GenericValidator.isBlankOrNull(labNumber)) {
            Sample sample = sampleService.getSampleByAccessionNumber(labNumber);
            if (sample != null) {
                searchResults.add(sample);
            }
        } else {
            List<PatientSearchResults> results = searchResultsService.getSearchResults(lastName, firstName, STNumber,
                    subjectNumber, nationalID, nationalID, patientID, guid, "", "");

            for (PatientSearchResults patientSearchResults : results) {
                List<Sample> sampleList = sampleService.getSamplesForPatient(patientSearchResults.getPatientID());
                searchResults.addAll(sampleList);
            }
        }
        if (searchResults.size() == 0) {
            result = INVALID;
            xml.append("No results found for search criteria.");
        } else {
            for (Sample sample : searchResults) {
                createSampleXML(xml, sample);
            }
        }
        ajaxServlet.sendData(xml.toString(), result, request, response);
    }

    private String createSampleXML(StringBuilder xml, Sample sample) {

        xml.append("<sample ");
        XMLUtil.appendAttributeKeyValue("labOrderNumber", sample.getAccessionNumber(), xml);
        xml.append(">");
        List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());
        for (SampleItem sampleItem : sampleItems) {
            xml.append("<item>");
            XMLUtil.appendKeyValue("id", sampleItem.getId(), xml);
            XMLUtil.appendKeyValue("number", sampleItem.getSortOrder(), xml);
            XMLUtil.appendKeyValue("type", sampleItem.getTypeOfSample().getDescription(), xml);
            xml.append("</item>");
        }
        xml.append("</sample>");
        return "";
    }

    @Override
    public void setServlet(AjaxServlet ajaxServlet) {
        this.ajaxServlet = ajaxServlet;
    }

    @Override
    public AjaxServlet getServlet() {
        return ajaxServlet;
    }
}
