package org.openelisglobal.microbiology.form;

import java.util.List;

public class MicroCaseTestCatalogForm {
    public List<Option> tests = List.of();
    public List<Option> panels = List.of();

    public record Option(String id, String name) {
    }
}
