package org.openelisglobal.pharmaceutical.service;

import java.util.List;
import org.openelisglobal.pharmaceutical.valueholder.DeviationCAPA;

public interface DeviationCAPAService {

    DeviationCAPA get(Integer id);

    List<DeviationCAPA> getAll();

    DeviationCAPA save(DeviationCAPA deviationCAPA);

    DeviationCAPA update(DeviationCAPA deviationCAPA);

    void delete(Integer id);

    List<DeviationCAPA> findByAssayRunId(Integer assayRunId);

    List<DeviationCAPA> findByStatus(DeviationCAPA.CAPAStatus status);

    List<DeviationCAPA> findOpenCAPAs();

    DeviationCAPA findByDeviationNumber(String deviationNumber);

    DeviationCAPA createDeviation(Integer assayRunId, DeviationCAPA deviation, String userId);

    DeviationCAPA initiateCAPA(Integer deviationId, String correctiveAction, String preventiveAction, String userId);

    DeviationCAPA updateCAPAStatus(Integer deviationId, DeviationCAPA.CAPAStatus newStatus, String userId);

    DeviationCAPA closeCAPA(Integer deviationId, String closureNotes, String userId);

    String generateDeviationNumber();
}
