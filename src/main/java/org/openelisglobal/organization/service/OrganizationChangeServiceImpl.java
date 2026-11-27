package org.openelisglobal.organization.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.organization.dao.OrganizationChangeDAO;
import org.openelisglobal.organization.valueholder.OrganizationChange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationChangeServiceImpl extends BaseObjectServiceImpl<OrganizationChange, Integer>
        implements OrganizationChangeService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<FieldChange>> CHANGE_LIST = new TypeReference<>() {
    };

    @Autowired
    protected OrganizationChangeDAO baseObjectDAO;

    public OrganizationChangeServiceImpl() {
        super(OrganizationChange.class);
    }

    @Override
    protected OrganizationChangeDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional
    public OrganizationChange record(Integer organizationId, String action, List<FieldChange> changes, String sysUserId,
            String actor) {
        OrganizationChange change = new OrganizationChange();
        change.setOrganizationId(organizationId);
        change.setAction(action);
        change.setActor(actor);
        change.setChangedAt(new Timestamp(System.currentTimeMillis()));
        if (sysUserId != null && sysUserId.matches("\\d+")) {
            change.setSystemUserId(Integer.valueOf(sysUserId));
        }
        change.setChanges(write(changes == null ? List.of() : changes));
        change.setSysUserId(sysUserId);
        change.setId(baseObjectDAO.insert(change));
        return change;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationChange> getForOrganization(Integer organizationId) {
        return baseObjectDAO.getForOrganization(organizationId);
    }

    @Override
    public List<FieldChange> changesOf(OrganizationChange change) {
        if (change == null || change.getChanges() == null || change.getChanges().isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(change.getChanges(), CHANGE_LIST);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "changesOf", e.getMessage());
            return List.of();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, List<String>> formerNames() {
        Map<Integer, List<String>> names = new LinkedHashMap<>();
        for (OrganizationChange change : baseObjectDAO.getNameChanges()) {
            for (FieldChange field : changesOf(change)) {
                if ("name".equals(field.field()) && field.oldValue() != null && !field.oldValue().isBlank()) {
                    names.computeIfAbsent(change.getOrganizationId(), k -> new ArrayList<>()).add(field.oldValue());
                }
            }
        }
        return names;
    }

    private static String write(List<FieldChange> changes) {
        try {
            return MAPPER.writeValueAsString(changes);
        } catch (Exception e) {
            LogEvent.logError(OrganizationChangeServiceImpl.class.getSimpleName(), "write", e.getMessage());
            return "[]";
        }
    }
}
