package org.openelisglobal.microbiology.form;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class MicroAstAnalyzerResultRequestForm {

    public String sourceEventId;
    public String analyzerInstrumentId;
    public String analyzerCardId;
    public String analyzerSoftwareVersion;
    public String analyzerOrganismId;
    public String analyzerOrganismName;
    public BigDecimal analyzerOrganismConfidence;
    public List<String> analyzerExpertFlags = new ArrayList<>();
    public String instrumentQcReference;
    public Boolean qcPassed;
    public Timestamp loadedAt;
    public Timestamp completedAt;
    public List<String> analyzerMessageCodes = new ArrayList<>();
    public List<MicroAstAnalyzerReadingRequestForm> readings = new ArrayList<>();
}
