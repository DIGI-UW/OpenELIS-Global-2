package org.openelisglobal.pharmaceutical.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.pharmaceutical.valueholder.PharmaceuticalSample;

public interface PharmaceuticalSampleService {

    PharmaceuticalSample get(Integer id);

    List<PharmaceuticalSample> getAll();

    PharmaceuticalSample save(PharmaceuticalSample sample);

    PharmaceuticalSample update(PharmaceuticalSample sample);

    void delete(Integer id);

    PharmaceuticalSample findByUniqueSampleId(String uniqueSampleId);

    PharmaceuticalSample findByBarcode(String barcode);

    List<PharmaceuticalSample> findByStatus(PharmaceuticalSample.SampleStatus status);

    List<PharmaceuticalSample> findByLabType(PharmaceuticalSample.LabType labType);

    List<PharmaceuticalSample> findExpiringSoon(int daysAhead);

    List<PharmaceuticalSample> searchByName(String searchTerm);

    PharmaceuticalSample registerSample(PharmaceuticalSample sample, String userId);

    PharmaceuticalSample updateStatus(Integer sampleId, PharmaceuticalSample.SampleStatus newStatus, String userId);

    String generateBarcode(PharmaceuticalSample sample);

    Map<String, Object> getSampleWithDetails(Integer sampleId);
}
