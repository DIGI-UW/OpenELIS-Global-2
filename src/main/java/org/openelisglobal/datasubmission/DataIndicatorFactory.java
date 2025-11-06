package org.openelisglobal.datasubmission;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.datasubmission.valueholder.DataIndicator;
import org.openelisglobal.datasubmission.valueholder.DataResource;
import org.openelisglobal.datasubmission.valueholder.DataValue;
import org.openelisglobal.datasubmission.valueholder.TypeOfDataIndicator;

public class DataIndicatorFactory {

    // TO DO currently each datavalue put into a resource is hardcoded. could move
    // to TypeOfResource being stored in DB
    // and have datavalues generated from these. this would cut down on code and
    // also have less repetitive values in the DataValue table
    // if the often repeated datavalue namekey and column were moved into a separate
    // table and linked
    public static DataIndicator createBlankDataIndicatorForType(TypeOfDataIndicator typeOfIndicator) {

        DataIndicator indicator = new DataIndicator();
        String indicatorType = typeOfIndicator.getName();
        if ("Turnaround Time".equals(indicatorType)) {
            indicator = createTurnaroundTime();
        } else if ("VL Coverage".equals(indicatorType)) {
            indicator = createVLCoverage();
        } else if ("Testing Trends".equals(indicatorType)) {
            indicator = createTestingTrends();
        } else if ("VL Outcomes".equals(indicatorType)) {
            indicator = createVLOutcomes();
        } else if ("Gender Trends".equals(indicatorType)) {
            indicator = createGenderTrends();
        } else if ("Age Trends".equals(indicatorType)) {
            indicator = createAgeTrends();
        } else if ("Justification".equals(indicatorType)) {
            indicator = createJustification();
        } else if ("Gender Suppression".equals(indicatorType)) {
            indicator = createGenderSuppression();
        } else if ("Age Suppression".equals(indicatorType)) {
            indicator = createAgeSuppression();
        }
        indicator.setTypeOfIndicator(typeOfIndicator);
        indicator.setStatus(DataIndicator.UNSAVED);

        return indicator;
    }

    // TO DO expand to include the sub values of turnaround time: tat1 tat2 tat3
    private static DataIndicator createTurnaroundTime() {
        DataIndicator indicator = new DataIndicator();
        indicator.setDataValue(new DataValue());
        List<DataResource> resources = new ArrayList<DataResource>();
        DataResource resource;

        resource = new DataResource();
        resource.setName("summary");
        resource.setCollectionName("summaries");
        resource.setLevel(DataResource.ALL);
        List<DataValue> columnValues = new ArrayList<DataValue>();
        columnValues.add(new DataValue("tat4", "datasubmission.tat"));

        resource.setColumnValues(columnValues);
        resources.add(resource);

        indicator.setResources(resources);

        return indicator;
    }

    private static DataIndicator createVLCoverage() {
        DataIndicator indicator = new DataIndicator();
        indicator.setDataValue(new DataValue());
        List<DataResource> resources = new ArrayList<DataResource>();
        DataResource resource = new DataResource();

        resource.setName("suppression");
        resource.setCollectionName("suppressions");
        resource.setLevel(DataResource.ALL);
        List<DataValue> columnValues = new ArrayList<DataValue>();
        columnValues.add(new DataValue("suppressed", "datasubmission.suppressed"));
        columnValues.add(new DataValue("nonsuppressed", "datasubmission.nonsuppressed"));
        resource.setColumnValues(columnValues);
        resources.add(resource);

        indicator.setResources(resources);

        return indicator;
    }

    private static DataIndicator createVLOutcomes() {
        DataIndicator indicator = new DataIndicator();
        indicator.setDataValue(new DataValue());
        List<DataResource> resources = new ArrayList<DataResource>();
        DataResource resource = new DataResource();

        resource.setName("summary");
        resource.setCollectionName("summaries");
        resource.setLevel(DataResource.ALL);
        List<DataValue> columnValues = new ArrayList<DataValue>();
        columnValues.add(new DataValue("confirm2vl", "datasubmission.confirm2vl"));
        columnValues.add(new DataValue("confirmtx", "datasubmission.confirmtx"));
        columnValues.add(new DataValue("baseline", "datasubmission.baseline"));
        columnValues.add(new DataValue("baselinesustxfail", "datasubmission.baselinesustxfail"));
        columnValues.add(new DataValue("rejected", "datasubmission.rejected"));
        columnValues.add(new DataValue("received", "datasubmission.received"));
        columnValues.add(new DataValue("sitessending", "datasubmission.sitessending"));

        resource.setColumnValues(columnValues);
        resources.add(resource);

        indicator.setResources(resources);

        return indicator;
    }

    private static DataIndicator createTestingTrends() {
        DataIndicator indicator = new DataIndicator();
        indicator.setDataValue(new DataValue());
        List<DataResource> resources = new ArrayList<DataResource>();
        DataResource resource = new DataResource();

        resource.setName("summary");
        resource.setCollectionName("summaries");
        resource.setLevel(DataResource.ALL);
        List<DataValue> columnValues = new ArrayList<DataValue>();
        columnValues.add(new DataValue("dbs", "datasubmission.sample.dbs"));
        columnValues.add(new DataValue("plasma", "datasubmission.sample.plasma"));
        columnValues.add(new DataValue("edta", "datasubmission.sample.edta"));
        columnValues.add(new DataValue("alldbs", "datasubmission.sample.alldbs"));
        columnValues.add(new DataValue("allplasma", "datasubmission.sample.allplasma"));
        columnValues.add(new DataValue("alledta", "datasubmission.sample.alledta"));
        columnValues.add(new DataValue("Undetected", "datasubmission.undetected"));
        columnValues.add(new DataValue("less1000", "datasubmission.less1000"));
        columnValues.add(new DataValue("less5000", "datasubmission.less5000"));
        columnValues.add(new DataValue("above5000", "datasubmission.more5000"));

        resource.setColumnValues(columnValues);
        resources.add(resource);

        indicator.setResources(resources);

        return indicator;
    }

    private static DataIndicator createGenderTrends() {
        DataIndicator indicator = new DataIndicator();
        indicator.setDataValue(new DataValue());
        List<DataResource> resources = new ArrayList<DataResource>();
        DataResource resource = new DataResource();

        resource.setName("gender");
        resource.setCollectionName("genders");
        resource.setLevel(DataResource.ALL);
        resource.setHeaderKey("datasubmission.men");
        List<DataValue> columnValues = new ArrayList<DataValue>();
        columnValues.add(new DataValue("Undetected", "datasubmission.undetected"));
        columnValues.add(new DataValue("less1000", "datasubmission.less1000"));
        columnValues.add(new DataValue("less5000", "datasubmission.less5000"));
        columnValues.add(new DataValue("above5000", "datasubmission.more5000"));
        columnValues.add(new DataValue("gender", "Men", false));

        resource.setColumnValues(columnValues);
        resources.add(resource);

        resource = new DataResource();

        resource.setName("gender");
        resource.setCollectionName("genders");
        resource.setLevel(DataResource.ALL);
        resource.setHeaderKey("datasubmission.women");
        columnValues = new ArrayList<DataValue>();
        columnValues.add(new DataValue("Undetected", "datasubmission.undetected"));
        columnValues.add(new DataValue("less1000", "datasubmission.less1000"));
        columnValues.add(new DataValue("less5000", "datasubmission.less5000"));
        columnValues.add(new DataValue("above5000", "datasubmission.more5000"));
        columnValues.add(new DataValue("gender", "Women", false));

        resource.setColumnValues(columnValues);
        resources.add(resource);

        indicator.setResources(resources);

        return indicator;
    }

    private static DataIndicator createAgeTrends() {
        DataIndicator indicator = new DataIndicator();
        indicator.setDataValue(new DataValue());
        List<DataResource> resources = new ArrayList<DataResource>();
        DataResource resource = new DataResource();

        resource.setName("age");
        resource.setCollectionName("ages");
        resource.setLevel(DataResource.ALL);
        resource.setHeaderKey("datasubmission.less2");
        List<DataValue> columnValues = new ArrayList<DataValue>();
        columnValues.add(new DataValue("Undetected", "datasubmission.undetected"));
        columnValues.add(new DataValue("less1000", "datasubmission.less1000"));
        columnValues.add(new DataValue("less5000", "datasubmission.less5000"));
        columnValues.add(new DataValue("above5000", "datasubmission.more5000"));
        columnValues.add(new DataValue("age", "label.less2", false));

        resource.setColumnValues(columnValues);
        resources.add(resource);

        resource = new DataResource();
        resource.setName("age");
        resource.setCollectionName("ages");
        resource.setLevel(DataResource.ALL);
        resource.setHeaderKey("datasubmission.less9");
        columnValues = new ArrayList<DataValue>();
        columnValues.add(new DataValue("Undetected", "datasubmission.undetected"));
        columnValues.add(new DataValue("less1000", "datasubmission.less1000"));
        columnValues.add(new DataValue("less5000", "datasubmission.less5000"));
        columnValues.add(new DataValue("above5000", "datasubmission.more5000"));
        columnValues.add(new DataValue("age", "label.less9", false));

        resource.setColumnValues(columnValues);
        resources.add(resource);

        resource = new DataResource();
        resource.setName("age");
        resource.setCollectionName("ages");
        resource.setLevel(DataResource.ALL);
        resource.setHeaderKey("datasubmission.less14");
        columnValues = new ArrayList<DataValue>();
        columnValues.add(new DataValue("Undetected", "datasubmission.undetected"));
        columnValues.add(new DataValue("less1000", "datasubmission.less1000"));
        columnValues.add(new DataValue("less5000", "datasubmission.less5000"));
        columnValues.add(new DataValue("above5000", "datasubmission.more5000"));
        columnValues.add(new DataValue("age", "label.less14", false));

        resource.setColumnValues(columnValues);
        resources.add(resource);

        resource = new DataResource();
        resource.setName("age");
        resource.setCollectionName("ages");
        resource.setLevel(DataResource.ALL);
        resource.setHeaderKey("datasubmission.less19");
        columnValues = new ArrayList<DataValue>();
        columnValues.add(new DataValue("Undetected", "datasubmission.undetected"));
        columnValues.add(new DataValue("less1000", "datasubmission.less1000"));
        columnValues.add(new DataValue("less5000", "datasubmission.less5000"));
        columnValues.add(new DataValue("above5000", "datasubmission.more5000"));
        columnValues.add(new DataValue("age", "label.less19", false));

        resource.setColumnValues(columnValues);
        resources.add(resource);

        resource = new DataResource();
        resource.setName("age");
        resource.setCollectionName("ages");
        resource.setLevel(DataResource.ALL);
        resource.setHeaderKey("datasubmission.less24");
        columnValues = new ArrayList<DataValue>();
        columnValues.add(new DataValue("Undetected", "datasubmission.undetected"));
        columnValues.add(new DataValue("less1000", "datasubmission.less1000"));
        columnValues.add(new DataValue("less5000", "datasubmission.less5000"));
        columnValues.add(new DataValue("above5000", "datasubmission.more5000"));
        columnValues.add(new DataValue("age", "label.less24", false));

        resource.setColumnValues(columnValues);
        resources.add(resource);

        resource = new DataResource();
        resource.setName("age");
        resource.setCollectionName("ages");
        resource.setLevel(DataResource.ALL);
        resource.setHeaderKey("datasubmission.over25");
        columnValues = new ArrayList<DataValue>();
        columnValues.add(new DataValue("Undetected", "datasubmission.undetected"));
        columnValues.add(new DataValue("less1000", "datasubmission.less1000"));
        columnValues.add(new DataValue("less5000", "datasubmission.less5000"));
        columnValues.add(new DataValue("above5000", "datasubmission.more5000"));
        columnValues.add(new DataValue("age", "label.over25", false));

        resource.setColumnValues(columnValues);
        resources.add(resource);

        indicator.setResources(resources);

        return indicator;
    }

    private static DataIndicator createJustification() {
        DataIndicator indicator = new DataIndicator();
        indicator.setDataValue(new DataValue());
        List<DataResource> resources = new ArrayList<DataResource>();
        DataResource resource = new DataResource();

        resource.setName("justification");
        resource.setCollectionName("justifications");
        resource.setLevel(DataResource.ALL);
        List<DataValue> columnValues = new ArrayList<DataValue>();
        columnValues.add(new DataValue("tests", "datasubmission.reason.arv"));
        columnValues.add(new DataValue("justification", "arv", false));

        resource.setColumnValues(columnValues);
        resources.add(resource);

        resource = new DataResource();
        resource.setName("justification");
        resource.setCollectionName("justifications");
        resource.setLevel(DataResource.ALL);
        columnValues = new ArrayList<DataValue>();
        columnValues.add(new DataValue("tests", "datasubmission.reason.virologicalfail"));
        columnValues.add(new DataValue("justification", "virological", false));

        resource.setColumnValues(columnValues);
        resources.add(resource);

        resource = new DataResource();
        resource.setName("justification");
        resource.setCollectionName("justifications");
        resource.setLevel(DataResource.ALL);
        columnValues = new ArrayList<DataValue>();
        columnValues.add(new DataValue("tests", "datasubmission.reason.clinicalfail"));
        columnValues.add(new DataValue("justification", "clinical", false));

        resource.setColumnValues(columnValues);
        resources.add(resource);

        resource = new DataResource();
        resource.setName("justification");
        resource.setCollectionName("justifications");
        resource.setLevel(DataResource.ALL);
        columnValues = new ArrayList<DataValue>();
        columnValues.add(new DataValue("tests", "datasubmission.reason.immunologicalfail"));
        columnValues.add(new DataValue("justification", "immunological", false));

        resource.setColumnValues(columnValues);
        resources.add(resource);

        indicator.setResources(resources);

        return indicator;
    }

    private static DataIndicator createGenderSuppression() {
        DataIndicator indicator = new DataIndicator();
        indicator.setDataValue(new DataValue());
        List<DataResource> resources = new ArrayList<DataResource>();
        DataResource resource = new DataResource();

        resource.setName("suppression");
        resource.setCollectionName("suppressions");
        resource.setLevel(DataResource.ALL);
        List<DataValue> columnValues = new ArrayList<DataValue>();
        columnValues.add(new DataValue("male_suppressed", "datasubmission.malesuppressed"));
        columnValues.add(new DataValue("male_nonsuppressed", "datasubmission.malenonsuppressed"));
        columnValues.add(new DataValue("female_suppressed", "datasubmission.femalesuppressed"));
        columnValues.add(new DataValue("female_nonsuppressed", "datasubmission.femalenonsuppressed"));
        resource.setColumnValues(columnValues);
        resources.add(resource);

        indicator.setResources(resources);

        return indicator;
    }

    private static DataIndicator createAgeSuppression() {
        DataIndicator indicator = new DataIndicator();
        indicator.setDataValue(new DataValue());
        List<DataResource> resources = new ArrayList<DataResource>();
        DataResource resource = new DataResource();

        resource.setName("suppression");
        resource.setCollectionName("suppressions");
        resource.setLevel(DataResource.ALL);
        List<DataValue> columnValues = new ArrayList<DataValue>();
        columnValues.add(new DataValue("less2_suppressed", "datasubmission.less2suppressed"));
        columnValues.add(new DataValue("less2_nonsuppressed", "datasubmission.less2nonsuppressed"));
        columnValues.add(new DataValue("less9_suppressed", "datasubmission.less9suppressed"));
        columnValues.add(new DataValue("less9_nonsuppressed", "datasubmission.less9nonsuppressed"));
        columnValues.add(new DataValue("less14_suppressed", "datasubmission.less14suppressed"));
        columnValues.add(new DataValue("less14_nonsuppressed", "datasubmission.less14nonsuppressed"));
        columnValues.add(new DataValue("less19_suppressed", "datasubmission.less19suppressed"));
        columnValues.add(new DataValue("less19_nonsuppressed", "datasubmission.less19nonsuppressed"));
        columnValues.add(new DataValue("less24_suppressed", "datasubmission.less24suppressed"));
        columnValues.add(new DataValue("less24_nonsuppressed", "datasubmission.less24nonsuppressed"));
        columnValues.add(new DataValue("over25_suppressed", "datasubmission.over25suppressed"));
        columnValues.add(new DataValue("over25_nonsuppressed", "datasubmission.over25nonsuppressed"));
        resource.setColumnValues(columnValues);
        resources.add(resource);

        indicator.setResources(resources);

        return indicator;
    }
}
