package org.openelisglobal.qaevent.qiconfig.breach;

import java.math.BigDecimal;
import org.openelisglobal.qaevent.valueholder.NcEvent;

/**
 * OGC-712 — creates the auto-NCE for a QI indicator that has breached its
 * qi_config action threshold. Near-copy of the QC-violation path (#3869): the
 * NCE is keyed on trigger source ({@code QI_BREACH},
 * {@code indicatorKey:periodKey}) so at most one NCE is created per indicator
 * per period no matter how often the evaluator runs.
 */
public interface QiBreachNceService {

    NcEvent createBreachNce(String indicatorKey, String periodKey, BigDecimal actualValue, BigDecimal actionThreshold,
            String direction);
}
