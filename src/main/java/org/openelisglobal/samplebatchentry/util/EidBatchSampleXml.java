package org.openelisglobal.samplebatchentry.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.apache.commons.validator.GenericValidator;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.openelisglobal.samplebatchentry.form.SampleBatchEntrySaveForm.EidSelection;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;

/**
 * Builds the sample XML of an EID batch order from the specimens and test
 * ticked on the setup screen: one sample per ticked specimen, each carrying the
 * DNA PCR test. Empty when no specimen or no test is ticked, or the catalog
 * lacks them, so the order is refused as one without samples.
 */
public class EidBatchSampleXml {

    static final String DRY_TUBE = "Dry Tube";
    static final String DRY_BLOOD_SPOT = "DBS";
    static final String DNA_PCR = "DNA PCR";
    private static final String HUMAN_DOMAIN = "H";

    private final TestService testService;
    private final TypeOfSampleService typeOfSampleService;

    public EidBatchSampleXml(TestService testService, TypeOfSampleService typeOfSampleService) {
        this.testService = testService;
        this.typeOfSampleService = typeOfSampleService;
    }

    public String build(EidSelection selection, String collectionDate) {
        if (selection == null || !selection.isDnaPCR()) {
            return "";
        }
        Optional<Test> dnaPcr = testService.getActiveTestByName(DNA_PCR).stream()
                .min(Comparator.comparingInt(test -> Integer.parseInt(test.getId())));
        List<String> specimenNames = new ArrayList<>();
        if (selection.isDryTubeTaken()) {
            specimenNames.add(DRY_TUBE);
        }
        if (selection.isDbsTaken()) {
            specimenNames.add(DRY_BLOOD_SPOT);
        }

        Document document = DocumentHelper.createDocument();
        Element samples = document.addElement("samples");
        for (String specimenName : specimenNames) {
            TypeOfSample specimen = findSpecimen(specimenName);
            if (specimen == null || dnaPcr.isEmpty()) {
                return "";
            }
            samples.addElement("sample").addAttribute("sampleID", specimen.getId())
                    .addAttribute("tests", dnaPcr.get().getId()).addAttribute("testSectionMap", "")
                    .addAttribute("date", GenericValidator.isBlankOrNull(collectionDate) ? "" : collectionDate)
                    .addAttribute("time", "").addAttribute("testSampleTypeMap", "").addAttribute("panels", "")
                    .addAttribute("numOrderLabels", "1").addAttribute("numSpecimenLabels", "1")
                    .addAttribute("initialConditionIds", "");
        }
        return samples.elements().isEmpty() ? "" : document.asXML();
    }

    private TypeOfSample findSpecimen(String description) {
        TypeOfSample query = new TypeOfSample();
        query.setDescription(description);
        query.setDomain(HUMAN_DOMAIN);
        return typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(query, true);
    }
}
