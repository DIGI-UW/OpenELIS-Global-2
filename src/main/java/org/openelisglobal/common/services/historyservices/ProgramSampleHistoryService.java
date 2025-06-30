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
 * <p>Copyright (C) ITECH, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.common.services.historyservices;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.audittrail.action.workers.AuditTrailItem;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.program.valueholder.ProgramSample;
import org.openelisglobal.program.valueholder.cytology.CytologySample;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.spring.util.SpringContext;

public class ProgramSampleHistoryService extends AbstractHistoryService {

    private static String TECHNICIAN_ATTRIBUTE = "technician";
    private static String PATHOLOGIST_ATTRIBUTE = "pathologist";
    private static String BLOCKS_ATTRIBUTE = "blocks";
    private static String SLIDES_ATTRIBUTE = "slides";
    private static String REQUESTS_ATTRIBUTE = "requests";
    private static String TECHNIQUES_ATTRIBUTE = "techniques";
    private static String CONCLUSIONS_ATTRIBUTE = "conclusions";
    private static String REPORTS_ATTRIBUTE = "reports";
    private static String GROSS_EXAM_ATTRIBUTE = "grossExam";
    private static String MICROSCOPY_EXAM_ATTRIBUTE = "microscopyExam";
    private static String REFERRED_ATTRIBUTE = "reffered";
    private static String CYTO_PATHOLOGIST_ATTRIBUTE = "cytoPathologist";
    private static String SPECIMEN_ADEQUACY_ATTRIBUTE = "specimenAdequacy";
    private static String DIAGNOSIS_ATTRIBUTE = "diagnosis";

    protected ReferenceTablesService referenceTablesService = SpringContext.getBean(ReferenceTablesService.class);
    protected HistoryService historyService = SpringContext.getBean(HistoryService.class);
    private Class<? extends ProgramSample> exactClass;

    private String PROGRAM_SAMPLE_TABLE_ID;

    public ProgramSampleHistoryService(ProgramSample programSample) {
        if (programSample instanceof PathologySample) {
            PROGRAM_SAMPLE_TABLE_ID = referenceTablesService.getReferenceTableByName("PATHOLOGY_SAMPLE").getId();
            exactClass = PathologySample.class;
        } else if (programSample instanceof ImmunohistochemistrySample) {
            PROGRAM_SAMPLE_TABLE_ID = referenceTablesService.getReferenceTableByName("IMMUNOHISTOCHEMISTRY_SAMPLE")
                    .getId();
            exactClass = ImmunohistochemistrySample.class;
        } else if (programSample instanceof CytologySample) {
            PROGRAM_SAMPLE_TABLE_ID = referenceTablesService.getReferenceTableByName("CYTOLOGY_SAMPLE").getId();
            exactClass = CytologySample.class;
        } else {
            PROGRAM_SAMPLE_TABLE_ID = referenceTablesService.getReferenceTableByName("PROGRAM_SAMPLE").getId();
            exactClass = ProgramSample.class;
        }
        setUpForProgramSample(programSample);
    }

    private void setUpForProgramSample(ProgramSample programSample) {
        History searchHistory = new History();
        searchHistory.setReferenceId("" + programSample.getId());
        searchHistory.setReferenceTable(PROGRAM_SAMPLE_TABLE_ID);
        historyList = historyService.getHistoryByRefIdAndRefTableId(searchHistory);

        newValueMap = new HashMap<>();
        attributeToIdentifierMap = new HashMap<String, String>();
        if (programSample instanceof PathologySample) {
            PathologySample pathology = (PathologySample) programSample;
            attributeToIdentifierMap.put(TECHNICIAN_ATTRIBUTE, TECHNICIAN_ATTRIBUTE);
            attributeToIdentifierMap.put(PATHOLOGIST_ATTRIBUTE, PATHOLOGIST_ATTRIBUTE);
            attributeToIdentifierMap.put(STATUS_ATTRIBUTE, STATUS_ATTRIBUTE);
            attributeToIdentifierMap.put(BLOCKS_ATTRIBUTE, BLOCKS_ATTRIBUTE);
            attributeToIdentifierMap.put(SLIDES_ATTRIBUTE, SLIDES_ATTRIBUTE);
            attributeToIdentifierMap.put(REQUESTS_ATTRIBUTE, REQUESTS_ATTRIBUTE);
            attributeToIdentifierMap.put(TECHNIQUES_ATTRIBUTE, TECHNIQUES_ATTRIBUTE);
            attributeToIdentifierMap.put(CONCLUSIONS_ATTRIBUTE, CONCLUSIONS_ATTRIBUTE);
            attributeToIdentifierMap.put(REPORTS_ATTRIBUTE, REPORTS_ATTRIBUTE);
            attributeToIdentifierMap.put(GROSS_EXAM_ATTRIBUTE, GROSS_EXAM_ATTRIBUTE);
            attributeToIdentifierMap.put(MICROSCOPY_EXAM_ATTRIBUTE, MICROSCOPY_EXAM_ATTRIBUTE);
            newValueMap.put(TECHNICIAN_ATTRIBUTE, pathology.getTechnician_Audit());
            newValueMap.put(PATHOLOGIST_ATTRIBUTE, pathology.getPathologist_Audit());
            newValueMap.put(STATUS_ATTRIBUTE, pathology.getStatus() == null ? "" : pathology.getStatus().getDisplay());
            newValueMap.put(BLOCKS_ATTRIBUTE, pathology.getBlocks_Audit());
            newValueMap.put(SLIDES_ATTRIBUTE, pathology.getSlides_Audit());
            newValueMap.put(REQUESTS_ATTRIBUTE, pathology.getRequests_Audit());
            newValueMap.put(TECHNIQUES_ATTRIBUTE, pathology.getTechniques_Audit());
            newValueMap.put(CONCLUSIONS_ATTRIBUTE, pathology.getConclusions_Audit());
            newValueMap.put(REPORTS_ATTRIBUTE, pathology.getReports_Audit());
            newValueMap.put(GROSS_EXAM_ATTRIBUTE, pathology.getGrossExam());
            newValueMap.put(MICROSCOPY_EXAM_ATTRIBUTE, pathology.getMicroscopyExam());
        } else if (programSample instanceof ImmunohistochemistrySample) {
            ImmunohistochemistrySample immunohistochemitry = (ImmunohistochemistrySample) programSample;
            attributeToIdentifierMap.put(TECHNICIAN_ATTRIBUTE, TECHNICIAN_ATTRIBUTE);
            attributeToIdentifierMap.put(PATHOLOGIST_ATTRIBUTE, PATHOLOGIST_ATTRIBUTE);
            attributeToIdentifierMap.put(STATUS_ATTRIBUTE, STATUS_ATTRIBUTE);
            attributeToIdentifierMap.put(REPORTS_ATTRIBUTE, REPORTS_ATTRIBUTE);
            attributeToIdentifierMap.put(REFERRED_ATTRIBUTE, REFERRED_ATTRIBUTE);
            newValueMap.put(TECHNICIAN_ATTRIBUTE, immunohistochemitry.getTechnician_Audit());
            newValueMap.put(PATHOLOGIST_ATTRIBUTE, immunohistochemitry.getPathologist_Audit());
            newValueMap.put(STATUS_ATTRIBUTE,
                    immunohistochemitry.getStatus() == null ? "" : immunohistochemitry.getStatus().getDisplay());
            newValueMap.put(REPORTS_ATTRIBUTE, immunohistochemitry.getReports_Audit());
            newValueMap.put(REFERRED_ATTRIBUTE,
                    immunohistochemitry.getReffered() == null ? "" : immunohistochemitry.getReffered().toString());
            exactClass = ImmunohistochemistrySample.class;
        } else if (programSample instanceof CytologySample) {
            CytologySample cytology = (CytologySample) programSample;
            attributeToIdentifierMap.put(TECHNICIAN_ATTRIBUTE, TECHNICIAN_ATTRIBUTE);
            attributeToIdentifierMap.put(CYTO_PATHOLOGIST_ATTRIBUTE, CYTO_PATHOLOGIST_ATTRIBUTE);
            attributeToIdentifierMap.put(STATUS_ATTRIBUTE, STATUS_ATTRIBUTE);
            attributeToIdentifierMap.put(SLIDES_ATTRIBUTE, SLIDES_ATTRIBUTE);
            attributeToIdentifierMap.put(SPECIMEN_ADEQUACY_ATTRIBUTE, SPECIMEN_ADEQUACY_ATTRIBUTE);
            attributeToIdentifierMap.put(DIAGNOSIS_ATTRIBUTE, DIAGNOSIS_ATTRIBUTE);
            attributeToIdentifierMap.put(REPORTS_ATTRIBUTE, REPORTS_ATTRIBUTE);
            newValueMap.put(TECHNICIAN_ATTRIBUTE, cytology.getTechnician_Audit());
            newValueMap.put(CYTO_PATHOLOGIST_ATTRIBUTE, cytology.getCytoPathologist_Audit());
            newValueMap.put(STATUS_ATTRIBUTE, cytology.getStatus() == null ? "" : cytology.getStatus().getDisplay());
            newValueMap.put(SLIDES_ATTRIBUTE, cytology.getSlides_Audit());
            newValueMap.put(SPECIMEN_ADEQUACY_ATTRIBUTE, cytology.getSpecimenAdequacy_Audit());
            newValueMap.put(DIAGNOSIS_ATTRIBUTE, cytology.getDiagnosis_Audit());
            newValueMap.put(REPORTS_ATTRIBUTE, cytology.getReports_Audit());
        }

        identifier = programSample.getSample().getAccessionNumber() + "(" + programSample.getProgram().getProgramName()
                + ")";

    }

    @Override
    protected void addInsertion(History history, List<AuditTrailItem> items) {
        items.add(getCoreTrail(history));
        if (exactClass.equals(PathologySample.class)) {
            setAndAddIfValueNotNull(items, history, STATUS_ATTRIBUTE);
        }
    }

    @Override
    protected void getObservableChanges(History history, Map<String, String> changeMap, String changes) {
        if (exactClass.equals(PathologySample.class)) {
            getObservablePathologyChanges(changeMap, changes);
        } else if (exactClass.equals(ImmunohistochemistrySample.class)) {
            getObservableImmunohistochemistryChanges(changeMap, changes);
        } else if (exactClass.equals(CytologySample.class)) {
            getObservableCytologyChanges(changeMap, changes);
        }
    }

    private void getObservableCytologyChanges(Map<String, String> changeMap, String changes) {
        // blank values first inserted so there is a value to compare against
        // otherwise the update value is discarded
        changeMap.put(STATUS_ATTRIBUTE, "");
        simpleChange(changeMap, changes, STATUS_ATTRIBUTE);
        changeMap.put(TECHNICIAN_ATTRIBUTE, "");
        simpleChange(changeMap, changes, TECHNICIAN_ATTRIBUTE);
        changeMap.put(PATHOLOGIST_ATTRIBUTE, "");
        simpleChange(changeMap, changes, PATHOLOGIST_ATTRIBUTE);
        changeMap.put(BLOCKS_ATTRIBUTE, "");
        simpleChange(changeMap, changes, BLOCKS_ATTRIBUTE);
        changeMap.put(BLOCKS_ATTRIBUTE, "");
        simpleChange(changeMap, changes, BLOCKS_ATTRIBUTE);
        changeMap.put(SLIDES_ATTRIBUTE, "");
        simpleChange(changeMap, changes, SLIDES_ATTRIBUTE);
        changeMap.put(REQUESTS_ATTRIBUTE, "");
        simpleChange(changeMap, changes, REQUESTS_ATTRIBUTE);
        changeMap.put(TECHNIQUES_ATTRIBUTE, "");
        simpleChange(changeMap, changes, TECHNIQUES_ATTRIBUTE);
        changeMap.put(CONCLUSIONS_ATTRIBUTE, "");
        simpleChange(changeMap, changes, CONCLUSIONS_ATTRIBUTE);
        changeMap.put(REPORTS_ATTRIBUTE, "");
        simpleChange(changeMap, changes, REPORTS_ATTRIBUTE);
        changeMap.put(GROSS_EXAM_ATTRIBUTE, "");
        simpleChange(changeMap, changes, GROSS_EXAM_ATTRIBUTE);
        changeMap.put(MICROSCOPY_EXAM_ATTRIBUTE, "");
        simpleChange(changeMap, changes, MICROSCOPY_EXAM_ATTRIBUTE);
    }

    private void getObservableImmunohistochemistryChanges(Map<String, String> changeMap, String changes) {
        // blank values first inserted so there is a value to compare against
        // otherwise the update value is discarded
        changeMap.put(STATUS_ATTRIBUTE, "");
        simpleChange(changeMap, changes, STATUS_ATTRIBUTE);
        changeMap.put(TECHNICIAN_ATTRIBUTE, "");
        simpleChange(changeMap, changes, TECHNICIAN_ATTRIBUTE);
        changeMap.put(PATHOLOGIST_ATTRIBUTE, "");
        simpleChange(changeMap, changes, PATHOLOGIST_ATTRIBUTE);
        changeMap.put(BLOCKS_ATTRIBUTE, "");
        simpleChange(changeMap, changes, REPORTS_ATTRIBUTE);
        changeMap.put(REPORTS_ATTRIBUTE, "");
        simpleChange(changeMap, changes, REFERRED_ATTRIBUTE);
        changeMap.put(REFERRED_ATTRIBUTE, "");
    }

    private void getObservablePathologyChanges(Map<String, String> changeMap, String changes) {
        // blank values first inserted so there is a value to compare against
        // otherwise the update value is discarded
        changeMap.put(STATUS_ATTRIBUTE, "");
        simpleChange(changeMap, changes, STATUS_ATTRIBUTE);
        changeMap.put(TECHNICIAN_ATTRIBUTE, "");
        simpleChange(changeMap, changes, TECHNICIAN_ATTRIBUTE);
        changeMap.put(PATHOLOGIST_ATTRIBUTE, "");
        simpleChange(changeMap, changes, PATHOLOGIST_ATTRIBUTE);
        changeMap.put(BLOCKS_ATTRIBUTE, "");
        simpleChange(changeMap, changes, BLOCKS_ATTRIBUTE);
        changeMap.put(BLOCKS_ATTRIBUTE, "");
        simpleChange(changeMap, changes, BLOCKS_ATTRIBUTE);
        changeMap.put(SLIDES_ATTRIBUTE, "");
        simpleChange(changeMap, changes, SLIDES_ATTRIBUTE);
        changeMap.put(REQUESTS_ATTRIBUTE, "");
        simpleChange(changeMap, changes, REQUESTS_ATTRIBUTE);
        changeMap.put(TECHNIQUES_ATTRIBUTE, "");
        simpleChange(changeMap, changes, TECHNIQUES_ATTRIBUTE);
        changeMap.put(CONCLUSIONS_ATTRIBUTE, "");
        simpleChange(changeMap, changes, CONCLUSIONS_ATTRIBUTE);
        changeMap.put(REPORTS_ATTRIBUTE, "");
        simpleChange(changeMap, changes, REPORTS_ATTRIBUTE);
        changeMap.put(GROSS_EXAM_ATTRIBUTE, "");
        simpleChange(changeMap, changes, GROSS_EXAM_ATTRIBUTE);
        changeMap.put(MICROSCOPY_EXAM_ATTRIBUTE, "");
        simpleChange(changeMap, changes, MICROSCOPY_EXAM_ATTRIBUTE);
    }

    @Override
    protected String getObjectName() {
        if (exactClass.equals(PathologySample.class)) {
            return MessageUtil.getMessage("auditTrail.pathologySample");
        } else if (exactClass.equals(ImmunohistochemistrySample.class)) {
            return MessageUtil.getMessage("auditTrail.immunohistochemistrySample");
        } else if (exactClass.equals(CytologySample.class)) {
            return MessageUtil.getMessage("auditTrail.cytologySample");
        } else {
            return MessageUtil.getMessage("auditTrail.programSample");
        }
    }

    @Override
    protected boolean showAttribute() {
        return false;
    }
}
