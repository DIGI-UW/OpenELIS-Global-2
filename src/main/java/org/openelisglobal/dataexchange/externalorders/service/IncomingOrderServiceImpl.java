package org.openelisglobal.dataexchange.externalorders.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.dataexchange.externalorders.dao.IncomingOrderDAO;
import org.openelisglobal.dataexchange.externalorders.dto.ExternalOrderRequest;
import org.openelisglobal.dataexchange.externalorders.valueholder.IncomingOrder;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncomingOrderServiceImpl extends AuditableBaseObjectServiceImpl<IncomingOrder, Integer>
        implements IncomingOrderService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    protected IncomingOrderDAO baseObjectDAO;

    @Autowired
    private ExternalOrderFormMapperService externalOrderFormMapperService;

    IncomingOrderServiceImpl() {
        super(IncomingOrder.class);
        this.auditTrailLog = false;
    }

    @Override
    protected IncomingOrderDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional
    public Integer receiveOrder(ExternalOrderRequest externalOrderRequest, String payloadJson, String receivedSysUserId) {
        IncomingOrder holding = new IncomingOrder();
        holding.setExternalOrderNumber(externalOrderRequest.getExternalOrderNumber());
        holding.setPatientGuid(externalOrderRequest.getPatientGuid());
        holding.setPayload(payloadJson);
        holding.setReceivedTimestamp(new Timestamp(System.currentTimeMillis()));
        holding.setReceivedSysUserId(receivedSysUserId);

        holding.setSysUserId(receivedSysUserId);
        return baseObjectDAO.insert(holding);
    }

    @Override
    @Transactional
    public IncomingOrder receiveOrMergeOrder(ExternalOrderRequest externalOrderRequest, String payloadJson,
            String receivedSysUserId) {
        if (externalOrderRequest == null) {
            throw new IllegalArgumentException("Missing payload");
        }
        if (externalOrderRequest.getExternalOrderNumber() == null
                || externalOrderRequest.getExternalOrderNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Missing externalOrderNumber");
        }
        if (externalOrderRequest.getPatientGuid() == null || externalOrderRequest.getPatientGuid().trim().isEmpty()) {
            throw new IllegalArgumentException("Missing patientGuid");
        }

        IncomingOrder holding = baseObjectDAO.getByExternalOrderNumber(externalOrderRequest.getExternalOrderNumber())
                .orElse(null);
        if (holding == null) {
            Integer id = receiveOrder(externalOrderRequest, payloadJson, receivedSysUserId);
            return baseObjectDAO.get(id).orElseThrow(() -> new IllegalStateException("Unable to create holding"));
        }

        if (!externalOrderRequest.getPatientGuid().equals(holding.getPatientGuid())) {
            throw new IllegalArgumentException("patientGuid cannot be changed");
        }

        ExternalOrderRequest existing;
        try {
            existing = objectMapper.readValue(holding.getPayload(), ExternalOrderRequest.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid stored payload");
        }

        ExternalOrderRequest merged = mergeExternalOrders(existing, externalOrderRequest);

        String mergedJson;
        try {
            mergedJson = objectMapper.writeValueAsString(merged);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON payload");
        }

        holding.setPayload(mergedJson);
        holding.setSysUserId(receivedSysUserId);
        return baseObjectDAO.update(holding);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncomingOrder> getOrders() {
        return baseObjectDAO.getAllOrdered("receivedTimestamp", true);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IncomingOrder> getOrderByExternalOrderNumber(String externalOrderNumber) {
        return baseObjectDAO.getByExternalOrderNumber(externalOrderNumber);
    }

    @Override
    @Transactional
    public IncomingOrder updateOrderByExternalOrderNumber(String externalOrderNumber, ExternalOrderRequest updatedRequest,
            String payloadJson, String updatedSysUserId) {
        IncomingOrder holding = baseObjectDAO.getByExternalOrderNumber(externalOrderNumber).orElse(null);
        if (holding == null) {
            throw new IllegalArgumentException("Unknown externalOrderNumber");
        }
        return updateExistingHolding(holding, updatedRequest, payloadJson, updatedSysUserId);
    }

    private IncomingOrder updateExistingHolding(IncomingOrder holding, ExternalOrderRequest updatedRequest,
            String payloadJson, String updatedSysUserId) {
        if (updatedRequest == null) {
            throw new IllegalArgumentException("Missing payload");
        }

        // immutable fields
        if (updatedRequest.getExternalOrderNumber() == null
                || !updatedRequest.getExternalOrderNumber().equals(holding.getExternalOrderNumber())) {
            throw new IllegalArgumentException("externalOrderNumber cannot be changed");
        }
        if (updatedRequest.getPatientGuid() == null || !updatedRequest.getPatientGuid().equals(holding.getPatientGuid())) {
            throw new IllegalArgumentException("patientGuid cannot be changed");
        }

        if (payloadJson == null || payloadJson.trim().isEmpty()) {
            try {
                payloadJson = objectMapper.writeValueAsString(updatedRequest);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid JSON payload");
            }
        }

        holding.setPayload(payloadJson);
        holding.setSysUserId(updatedSysUserId);
        return baseObjectDAO.update(holding);
    }

    @Override
    @Transactional
    public void finalizeHolding(String externalOrderNumber) {
        if (externalOrderNumber == null || externalOrderNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing externalOrderNumber");
        }

        IncomingOrder holding = baseObjectDAO.getByExternalOrderNumber(externalOrderNumber).orElse(null);
        if (holding != null) {
            baseObjectDAO.delete(holding);
        }
    }

    @Override
    @Transactional
    public void deleteHoldingByExternalOrderNumber(String externalOrderNumber) {
        if (externalOrderNumber == null || externalOrderNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing externalOrderNumber");
        }

        IncomingOrder holding = baseObjectDAO.getByExternalOrderNumber(externalOrderNumber).orElse(null);
        if (holding == null) {
            return;
        }

        if (holding.getSampleId() != null || (holding.getLabNo() != null && !holding.getLabNo().trim().isEmpty())) {
            throw new IllegalStateException("Order can't be deleted after collection");
        }

        baseObjectDAO.delete(holding);
    }

    @Override
    @Transactional(readOnly = true)
    public SamplePatientEntryForm buildSamplePatientEntryForm(String externalOrderNumber) {
        if (externalOrderNumber == null || externalOrderNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing externalOrderNumber");
        }

        IncomingOrder holding = baseObjectDAO.getByExternalOrderNumber(externalOrderNumber).orElse(null);
        if (holding == null) {
            throw new IllegalArgumentException("Unknown externalOrderNumber");
        }

        ExternalOrderRequest externalOrderRequest;
        try {
            externalOrderRequest = objectMapper.readValue(holding.getPayload(), ExternalOrderRequest.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid stored payload");
        }

        String payloadExternalOrderNumber = externalOrderRequest.getExternalOrderNumber();
        if (payloadExternalOrderNumber == null || !payloadExternalOrderNumber.equals(externalOrderNumber)) {
            throw new IllegalArgumentException("Stored payload externalOrderNumber mismatch");
        }

        return externalOrderFormMapperService.buildForm(externalOrderRequest);
    }

    private ExternalOrderRequest mergeExternalOrders(ExternalOrderRequest existing, ExternalOrderRequest incoming) {
        if (existing == null) {
            return incoming;
        }
        if (incoming == null) {
            return existing;
        }

        if (incoming.getExternalOrderNumber() == null
                || !incoming.getExternalOrderNumber().equals(existing.getExternalOrderNumber())) {
            throw new IllegalArgumentException("externalOrderNumber cannot be changed");
        }
        if (incoming.getPatientGuid() == null || !incoming.getPatientGuid().equals(existing.getPatientGuid())) {
            throw new IllegalArgumentException("patientGuid cannot be changed");
        }

        ExternalOrderRequest out = new ExternalOrderRequest();
        out.setExternalOrderNumber(existing.getExternalOrderNumber());
        out.setPatientGuid(existing.getPatientGuid());

        out.setPriority(incoming.getPriority() != null ? incoming.getPriority() : existing.getPriority());
        out.setReferringSiteId(incoming.getReferringSiteId() != null ? incoming.getReferringSiteId()
                : existing.getReferringSiteId());
        out.setReferringSiteName(incoming.getReferringSiteName() != null ? incoming.getReferringSiteName()
                : existing.getReferringSiteName());
        out.setReferringSiteDepartmentId(incoming.getReferringSiteDepartmentId() != null
                ? incoming.getReferringSiteDepartmentId()
                : existing.getReferringSiteDepartmentId());

        out.setProviderPersonId(incoming.getProviderPersonId() != null ? incoming.getProviderPersonId()
                : existing.getProviderPersonId());
        out.setProviderFirstName(incoming.getProviderFirstName() != null ? incoming.getProviderFirstName()
                : existing.getProviderFirstName());
        out.setProviderLastName(incoming.getProviderLastName() != null ? incoming.getProviderLastName()
                : existing.getProviderLastName());
        out.setProviderWorkPhone(incoming.getProviderWorkPhone() != null ? incoming.getProviderWorkPhone()
                : existing.getProviderWorkPhone());
        out.setProviderFax(incoming.getProviderFax() != null ? incoming.getProviderFax() : existing.getProviderFax());
        out.setProviderEmail(
                incoming.getProviderEmail() != null ? incoming.getProviderEmail() : existing.getProviderEmail());

        out.setReceivedDate(incoming.getReceivedDate() != null ? incoming.getReceivedDate() : existing.getReceivedDate());
        out.setReceivedTime(incoming.getReceivedTime() != null ? incoming.getReceivedTime() : existing.getReceivedTime());
        out.setRequestDate(incoming.getRequestDate() != null ? incoming.getRequestDate() : existing.getRequestDate());
        out.setProgramId(incoming.getProgramId() != null ? incoming.getProgramId() : existing.getProgramId());

        Map<String, ExternalOrderRequest.ExternalOrderSample> existingByKey = indexSamples(existing.getSamples());
        Map<String, ExternalOrderRequest.ExternalOrderSample> incomingByKey = indexSamples(incoming.getSamples());

        Set<String> orderedKeys = new LinkedHashSet<>();
        orderedKeys.addAll(existingByKey.keySet());
        orderedKeys.addAll(incomingByKey.keySet());

        List<ExternalOrderRequest.ExternalOrderSample> mergedSamples = new ArrayList<>();
        for (String key : orderedKeys) {
            ExternalOrderRequest.ExternalOrderSample ex = existingByKey.get(key);
            ExternalOrderRequest.ExternalOrderSample in = incomingByKey.get(key);

            if (ex == null) {
                mergedSamples.add(in);
                continue;
            }
            if (in == null) {
                mergedSamples.add(ex);
                continue;
            }

            mergedSamples.add(mergeSample(ex, in));
        }

        out.setSamples(mergedSamples);
        return out;
    }

    private Map<String, ExternalOrderRequest.ExternalOrderSample> indexSamples(
            List<ExternalOrderRequest.ExternalOrderSample> samples) {
        Map<String, ExternalOrderRequest.ExternalOrderSample> map = new LinkedHashMap<>();
        if (samples == null) {
            return map;
        }

        for (int i = 0; i < samples.size(); i++) {
            ExternalOrderRequest.ExternalOrderSample s = samples.get(i);
            if (s == null) {
                continue;
            }

            String key = (s.getSampleTypeId() != null && !s.getSampleTypeId().trim().isEmpty())
                    ? s.getSampleTypeId().trim()
                    : "__index__" + i;
            map.put(key, s);
        }

        return map;
    }

    private ExternalOrderRequest.ExternalOrderSample mergeSample(ExternalOrderRequest.ExternalOrderSample existing,
            ExternalOrderRequest.ExternalOrderSample incoming) {
        ExternalOrderRequest.ExternalOrderSample out = new ExternalOrderRequest.ExternalOrderSample();
        out.setSampleTypeId(incoming.getSampleTypeId() != null ? incoming.getSampleTypeId() : existing.getSampleTypeId());

        out.setCollectionDate(
                incoming.getCollectionDate() != null ? incoming.getCollectionDate() : existing.getCollectionDate());
        out.setCollectionTime(
                incoming.getCollectionTime() != null ? incoming.getCollectionTime() : existing.getCollectionTime());
        out.setCollector(incoming.getCollector() != null ? incoming.getCollector() : existing.getCollector());
        out.setQuantity(incoming.getQuantity() != null ? incoming.getQuantity() : existing.getQuantity());
        out.setUom(incoming.getUom() != null ? incoming.getUom() : existing.getUom());

        if (incoming.getTests() != null) {
            if (incoming.getTests().isEmpty()) {
                out.setTests(incoming.getTests());
            } else {
                List<ExternalOrderRequest.ExternalOrderTestRef> mergedTests = new ArrayList<>();
                if (existing.getTests() != null) {
                    mergedTests.addAll(existing.getTests());
                }
                mergedTests.addAll(incoming.getTests());
                out.setTests(dedupeTests(mergedTests));
            }
        } else {
            out.setTests(existing.getTests());
        }

        if (incoming.getPanels() != null) {
            if (incoming.getPanels().isEmpty()) {
                out.setPanels(incoming.getPanels());
            } else {
                List<ExternalOrderRequest.ExternalOrderPanelRef> mergedPanels = new ArrayList<>();
                if (existing.getPanels() != null) {
                    mergedPanels.addAll(existing.getPanels());
                }
                mergedPanels.addAll(incoming.getPanels());
                out.setPanels(dedupePanels(mergedPanels));
            }
        } else {
            out.setPanels(existing.getPanels());
        }

        if (incoming.getRemovedTests() != null && out.getTests() != null) {
            out.setTests(removeTests(out.getTests(), incoming.getRemovedTests()));
        }
        if (incoming.getRemovedPanels() != null && out.getPanels() != null) {
            out.setPanels(removePanels(out.getPanels(), incoming.getRemovedPanels()));
        }

        return out;
    }

    private List<ExternalOrderRequest.ExternalOrderTestRef> removeTests(
            List<ExternalOrderRequest.ExternalOrderTestRef> existing,
            List<ExternalOrderRequest.ExternalOrderTestRef> removed) {
        if (existing == null) {
            return null;
        }
        if (removed == null || removed.isEmpty()) {
            return existing;
        }

        Map<String, ExternalOrderRequest.ExternalOrderTestRef> byKey = new LinkedHashMap<>();
        for (ExternalOrderRequest.ExternalOrderTestRef t : existing) {
            if (t == null) {
                continue;
            }
            String key = normalizeKey(t.getTestGuid(), t.getLoinc());
            if (key == null) {
                continue;
            }
            byKey.putIfAbsent(key, t);
        }

        for (ExternalOrderRequest.ExternalOrderTestRef r : removed) {
            if (r == null) {
                continue;
            }
            String key = normalizeKey(r.getTestGuid(), r.getLoinc());
            if (key == null) {
                continue;
            }
            byKey.remove(key);
        }

        return new ArrayList<>(byKey.values());
    }

    private List<ExternalOrderRequest.ExternalOrderPanelRef> removePanels(
            List<ExternalOrderRequest.ExternalOrderPanelRef> existing,
            List<ExternalOrderRequest.ExternalOrderPanelRef> removed) {
        if (existing == null) {
            return null;
        }
        if (removed == null || removed.isEmpty()) {
            return existing;
        }

        Map<String, ExternalOrderRequest.ExternalOrderPanelRef> byKey = new LinkedHashMap<>();
        for (ExternalOrderRequest.ExternalOrderPanelRef p : existing) {
            if (p == null) {
                continue;
            }
            String key = normalizeKey(p.getPanelGuid(), p.getLoinc());
            if (key == null) {
                continue;
            }
            byKey.putIfAbsent(key, p);
        }

        for (ExternalOrderRequest.ExternalOrderPanelRef r : removed) {
            if (r == null) {
                continue;
            }
            String key = normalizeKey(r.getPanelGuid(), r.getLoinc());
            if (key == null) {
                continue;
            }
            byKey.remove(key);
        }

        return new ArrayList<>(byKey.values());
    }

    private List<ExternalOrderRequest.ExternalOrderTestRef> dedupeTests(
            List<ExternalOrderRequest.ExternalOrderTestRef> tests) {
        if (tests == null) {
            return null;
        }
        Map<String, ExternalOrderRequest.ExternalOrderTestRef> byKey = new LinkedHashMap<>();
        for (ExternalOrderRequest.ExternalOrderTestRef t : tests) {
            if (t == null) {
                continue;
            }

            String key = normalizeKey(t.getTestGuid(), t.getLoinc());
            if (key == null) {
                continue;
            }
            byKey.putIfAbsent(key, t);
        }
        return new ArrayList<>(byKey.values());
    }

    private List<ExternalOrderRequest.ExternalOrderPanelRef> dedupePanels(
            List<ExternalOrderRequest.ExternalOrderPanelRef> panels) {
        if (panels == null) {
            return null;
        }
        Map<String, ExternalOrderRequest.ExternalOrderPanelRef> byKey = new LinkedHashMap<>();
        for (ExternalOrderRequest.ExternalOrderPanelRef p : panels) {
            if (p == null) {
                continue;
            }

            String key = normalizeKey(p.getPanelGuid(), p.getLoinc());
            if (key == null) {
                continue;
            }
            byKey.putIfAbsent(key, p);
        }
        return new ArrayList<>(byKey.values());
    }

    private String normalizeKey(String guid, String loinc) {
        if (guid != null && !guid.trim().isEmpty()) {
            return "guid:" + guid.trim();
        }
        if (loinc != null && !loinc.trim().isEmpty()) {
            return "loinc:" + loinc.trim();
        }
        return null;
    }
}
