package org.openelisglobal.microbiology.form;

import java.util.List;

public record MicroOrderPreviewForm(List<CaseLine> cases, List<TestLine> ordinaryTests, List<SplitWarning> warnings,
        List<ReflexLine> reflexRules, List<NewUnitWarning> newUnitWarnings) {
    public record SpecimenLine(int index, String sampleTypeName) {
    }

    public record TestLine(int specimenIndex, String testId, String testName) {
    }

    public record CaseLine(String labUnitId, String labUnitName, List<SpecimenLine> specimens, List<String> testNames,
            boolean collectedInSets, List<MicroCaseSpecimenForm> bottles,
            List<MicroCultureSetWarningForm> setWarnings) {
    }

    public record SplitWarning(int specimenIndex, List<String> labUnits) {
    }

    public record NewUnitWarning(String labUnitId, String labUnitName, String testName) {
    }

    public record ReflexCondition(String testName, String sampleTypeName, String componentLabel, String relation,
            String value, String value2) {
    }

    public record ReflexLine(String name, String overall, List<ReflexCondition> conditions, List<String> addedTests) {
    }
}
