package org.openelisglobal.microbiology.form;

import java.util.List;

public record MicroOrderPreviewForm(List<CaseLine> cases, List<TestLine> ordinaryTests, List<SplitWarning> warnings,
        List<ReflexLine> reflexRules) {
    public record SpecimenLine(int index, String sampleTypeName) {
    }

    public record TestLine(int specimenIndex, String testId, String testName) {
    }

    public record CaseLine(String labUnitId, String labUnitName, List<SpecimenLine> specimens, List<String> testNames,
            boolean collectedInSets) {
    }

    public record SplitWarning(int specimenIndex, List<String> labUnits) {
    }

    public record ReflexLine(String name, List<String> addedTests) {
    }
}
