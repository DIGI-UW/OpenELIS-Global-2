package org.openelisglobal.configuration.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.configuration.dao.UnresolvedReferenceDAO;
import org.openelisglobal.configuration.service.ImportRunContext.PendingReference;
import org.openelisglobal.configuration.valueholder.UnresolvedReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnresolvedReferenceServiceImpl extends BaseObjectServiceImpl<UnresolvedReference, String>
        implements UnresolvedReferenceService {

    private static final String SYSTEM_USER = "1";

    @Autowired
    protected UnresolvedReferenceDAO baseObjectDAO;

    @Autowired
    private ReferenceAliasService referenceAliasService;

    @Autowired
    private CatalogReferenceResolver resolver;

    public UnresolvedReferenceServiceImpl() {
        super(UnresolvedReference.class);
    }

    @Override
    protected UnresolvedReferenceDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordPending(String domain, String fileName, int lineNumber) {
        for (PendingReference pending : ImportRunContext.drainPending()) {
            UnresolvedReference open = baseObjectDAO.getOpen(pending.referenceType(), pending.referenceValue());
            if (open != null) {
                open.setOccurrences(open.getOccurrences() + 1);
                open.setImportRunId(ImportRunContext.getRunId());
                open.setDomain(domain);
                open.setFileName(fileName);
                open.setLineNumber(lineNumber);
                open.setContext(pending.context());
                open.setSysUserId(SYSTEM_USER);
                update(open);
                continue;
            }
            UnresolvedReference reference = new UnresolvedReference();
            reference.setImportRunId(ImportRunContext.getRunId());
            reference.setDomain(domain);
            reference.setFileName(fileName);
            reference.setLineNumber(lineNumber);
            reference.setReferenceType(pending.referenceType());
            reference.setReferenceValue(pending.referenceValue());
            reference.setContext(pending.context());
            reference.setStatus(UnresolvedReference.STATUS_OPEN);
            reference.setSysUserId(SYSTEM_USER);
            insert(reference);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnresolvedReference> getOpen() {
        return baseObjectDAO.getByStatus(UnresolvedReference.STATUS_OPEN);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void closeResolvable() {
        for (UnresolvedReference reference : baseObjectDAO.getByStatus(UnresolvedReference.STATUS_OPEN)) {
            String targetId = resolver.idFor(reference.getReferenceType(), reference.getReferenceValue());
            if (targetId == null) {
                continue;
            }
            reference.setStatus(UnresolvedReference.STATUS_RESOLVED);
            reference.setResolution(UnresolvedReference.RESOLUTION_USE_EXISTING);
            reference.setResolvedTargetId(targetId);
            reference.setResolvedAt(new Timestamp(System.currentTimeMillis()));
            reference.setSysUserId(SYSTEM_USER);
            update(reference);
        }
        ImportRunContext.clearPending();
    }

    @Override
    @Transactional
    public UnresolvedReference resolve(String id, String resolution, String targetId, String sysUserId) {
        UnresolvedReference reference = get(id);
        if (reference == null) {
            throw new LIMSRuntimeException("No unresolved reference " + id);
        }
        if (!UnresolvedReference.STATUS_OPEN.equals(reference.getStatus())) {
            throw new LIMSRuntimeException("Reference " + id + " is already " + reference.getStatus());
        }
        switch (resolution) {
        case UnresolvedReference.RESOLUTION_SKIP -> reference.setStatus(UnresolvedReference.STATUS_SKIPPED);
        case UnresolvedReference.RESOLUTION_USE_EXISTING, UnresolvedReference.RESOLUTION_ALIAS -> {
            if (targetId == null || targetId.isBlank()) {
                throw new LIMSRuntimeException("Resolving a reference needs the record to use");
            }
            reference.setStatus(UnresolvedReference.STATUS_RESOLVED);
            reference.setResolvedTargetId(targetId);
            if (UnresolvedReference.RESOLUTION_ALIAS.equals(resolution)) {
                referenceAliasService.remember(reference.getReferenceType(), reference.getReferenceValue(), targetId,
                        sysUserId);
            }
        }
        default -> throw new LIMSRuntimeException("Unknown resolution " + resolution);
        }
        reference.setResolution(resolution);
        reference.setResolvedAt(new Timestamp(System.currentTimeMillis()));
        reference.setSystemUserId(Integer.valueOf(sysUserId));
        reference.setSysUserId(sysUserId);
        update(reference);
        return reference;
    }
}
