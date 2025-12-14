package org.openelisglobal.pharmaceutical.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.pharmaceutical.valueholder.EnvironmentalExcursionEvent;

public interface EnvironmentalExcursionEventDAO extends BaseDAO<EnvironmentalExcursionEvent, Integer> {

    List<EnvironmentalExcursionEvent> findByDeviceId(Integer deviceId);

    List<EnvironmentalExcursionEvent> findByStatus(EnvironmentalExcursionEvent.ExcursionStatus status);

    List<EnvironmentalExcursionEvent> findActiveExcursions();

    List<EnvironmentalExcursionEvent> findByAlertType(EnvironmentalExcursionEvent.AlertType alertType);
}
