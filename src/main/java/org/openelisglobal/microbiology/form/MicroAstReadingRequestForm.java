package org.openelisglobal.microbiology.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MicroAstReadingRequestForm {

    public String antibioticId;
    public BigDecimal rawValue;
}
