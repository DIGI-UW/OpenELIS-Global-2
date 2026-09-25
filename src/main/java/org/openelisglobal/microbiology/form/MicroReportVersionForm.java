package org.openelisglobal.microbiology.form;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class MicroReportVersionForm {
    public String id;
    public String caseId;
    public String amendmentId;
    public Integer versionNumber;
    public String releaseType;
    public String content;
    public Timestamp releasedAt;
    public String releasedBy;
    public String correctsVersionId;
    public List<MicroReportVersionSourceForm> sources = new ArrayList<>();
}
