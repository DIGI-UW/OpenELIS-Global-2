package org.openelisglobal.microbiology.form;

import java.util.ArrayList;
import java.util.List;

public class MicroReferenceAdminPageForm<T> {
    public List<T> rows = new ArrayList<>();
    public long total;
    public int page;
    public int pageSize;
}
