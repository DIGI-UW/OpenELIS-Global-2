package org.openelisglobal.pharmaceutical.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.pharmaceutical.valueholder.PharmaceuticalSample;

public interface PharmaceuticalSampleDAO extends BaseDAO<PharmaceuticalSample, Integer> {

    PharmaceuticalSample findByUniqueSampleId(String uniqueSampleId);

    PharmaceuticalSample findByBarcode(String barcode);

    List<PharmaceuticalSample> findByStatus(PharmaceuticalSample.SampleStatus status);

    List<PharmaceuticalSample> findByLabType(PharmaceuticalSample.LabType labType);

    List<PharmaceuticalSample> findExpiringSoon(int daysAhead);

    List<PharmaceuticalSample> searchByName(String searchTerm);
}
