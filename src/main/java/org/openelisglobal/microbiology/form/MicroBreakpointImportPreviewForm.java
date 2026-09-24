package org.openelisglobal.microbiology.form;

import java.util.ArrayList;
import java.util.List;

public class MicroBreakpointImportPreviewForm {
    public String previewToken;
    public int totalRows;
    public int validRows;
    public int skippedRows;
    public int importedRows;
    public int unchangedRows;
    public List<MicroBreakpointImportErrorForm> errors = new ArrayList<>();
}
