package org.openelisglobal.lesotho.clinicalinfo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.lesotho.clinicalinfo.dao.OrderClinicalInfoDAO;
import org.openelisglobal.lesotho.clinicalinfo.valueholder.OrderClinicalInfo;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderClinicalInfoServiceImpl extends AuditableBaseObjectServiceImpl<OrderClinicalInfo, Integer>
        implements OrderClinicalInfoService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    protected OrderClinicalInfoDAO baseObjectDAO;

    @Autowired
    private SampleService sampleService;

    public OrderClinicalInfoServiceImpl() {
        super(OrderClinicalInfo.class);
        this.auditTrailLog = true;
    }

    @Override
    protected OrderClinicalInfoDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    private Optional<String> sampleIdFor(String accessionNumber) {
        if (StringUtils.isBlank(accessionNumber)) {
            return Optional.empty();
        }
        Sample sample = sampleService.getSampleByAccessionNumber(accessionNumber);
        return sample == null ? Optional.empty() : Optional.ofNullable(sample.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderClinicalInfo> getLatest(String accessionNumber) {
        return sampleIdFor(accessionNumber).flatMap(baseObjectDAO::getLatestBySampleId);
    }

    @Override
    @Transactional
    public SaveResult save(String accessionNumber, String clinicalDataJson, String sysUserId) {
        String sampleId = sampleIdFor(accessionNumber)
                .orElseThrow(() -> new IllegalArgumentException("no order has lab number " + accessionNumber));
        if (clinicalDataJson == null || clinicalDataJson.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("clinical information is missing or longer than " + MAX_LENGTH);
        }
        String normalized;
        try {
            JsonNode node = MAPPER.readTree(clinicalDataJson);
            if (node == null || !node.isObject()) {
                throw new IllegalArgumentException("clinical information must be a JSON object");
            }
            normalized = MAPPER.writeValueAsString(node);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException("clinical information is not valid JSON", e);
        }

        Optional<OrderClinicalInfo> latest = baseObjectDAO.getLatestBySampleId(sampleId);
        if (latest.isPresent() && normalized.equals(latest.get().getClinicalData())) {
            return new SaveResult(latest.get(), false);
        }
        OrderClinicalInfo info = new OrderClinicalInfo();
        info.setSampleId(sampleId);
        info.setClinicalData(normalized);
        info.setSysUserId(sysUserId);
        insert(info);
        return new SaveResult(info, true);
    }
}
