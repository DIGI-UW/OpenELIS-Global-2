package org.openelisglobal.reports.dataexport.dao;

import java.util.stream.Stream;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.referral.valueholder.ReferralResult;
import org.openelisglobal.reports.dataexport.form.ExportSnapshot;
import org.openelisglobal.test.valueholder.Test;

public interface ReferralExportDAO extends BaseDAO<Referral, String> {
    record Row(Referral referral, ReferralResult returnedResult, Test returnedTest) {
    }

    Stream<Row> stream(ExportSnapshot request);

    void clearReadBatch();
}
