package org.openelisglobal.microbiology.form;

import java.util.ArrayList;
import java.util.List;

public class MicroBenchResultForm {
    public String caseId, actionId, error;
    public List<String> changed = new ArrayList<>();
    public List<Skip> skipped = new ArrayList<>();

    public record Skip(String cultureId, String container, String reason) {
    }
}
