package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroInterpretationRuleDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.form.MicroInterpretationRuleForm;
import org.openelisglobal.microbiology.valueholder.MicroInterpretationRule;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MicroInterpretationRuleService {
    private final MicroInterpretationRuleDAO rules;
    private final MicroAntibioticDAO antibiotics;
    private final MicroOrganismDAO organisms;

    public MicroInterpretationRuleService(MicroInterpretationRuleDAO rules, MicroAntibioticDAO antibiotics,
            MicroOrganismDAO organisms) {
        this.rules = rules;
        this.antibiotics = antibiotics;
        this.organisms = organisms;
    }

    @Transactional
    public MicroInterpretationRule save(MicroInterpretationRuleForm form, String actor) {
        MicroCaseServiceImpl.requireText(actor, "actor");
        if (form == null) {
            throw new IllegalArgumentException("INTERPRETATION_RULE_INVALID");
        }
        String antibioticId = trimToNull(form.antibioticId);
        if (antibioticId == null || antibiotics.get(antibioticId).isEmpty()) {
            throw new IllegalArgumentException("INTERPRETATION_RULE_ANTIBIOTIC_INVALID");
        }
        String organismId = trimToNull(form.organismId);
        String organismGroup = trimToNull(form.organismGroup);
        if ((organismId == null) == (organismGroup == null)) {
            throw new IllegalArgumentException("INTERPRETATION_RULE_TARGET_INVALID");
        }
        String interpretation = trimToNull(form.interpretation);
        if (!"RESISTANT".equals(interpretation)) {
            throw new IllegalArgumentException("INTERPRETATION_RULE_INVALID");
        }
        MicroInterpretationRule rule = form.id == null ? new MicroInterpretationRule()
                : rules.get(form.id).orElseThrow(() -> new IllegalArgumentException("INTERPRETATION_RULE_NOT_FOUND"));
        rule.setAntibioticId(antibioticId);
        rule.setOrganismId(organismId);
        rule.setOrganismGroup(organismGroup);
        rule.setInterpretation(interpretation);
        rule.setSource(trimToNull(form.source));
        rule.setActive(form.active == null || form.active);
        rule.setSysUserId(actor);
        if (form.id == null) {
            rules.insert(rule);
        } else {
            rules.update(rule);
        }
        return rule;
    }

    public List<MicroInterpretationRuleForm> getRules() {
        return rules.getAll().stream().map(rule -> {
            MicroInterpretationRuleForm form = toForm(rule);
            antibiotics.get(rule.getAntibioticId())
                    .ifPresent(antibiotic -> form.antibioticName = antibiotic.getDisplayName());
            if (rule.getOrganismId() != null) {
                organisms.get(rule.getOrganismId())
                        .ifPresent(organism -> form.organismName = organism.getDisplayName());
            }
            return form;
        }).toList();
    }

    private MicroInterpretationRuleForm toForm(MicroInterpretationRule rule) {
        MicroInterpretationRuleForm form = new MicroInterpretationRuleForm();
        form.id = rule.getId();
        form.antibioticId = rule.getAntibioticId();
        form.organismId = rule.getOrganismId();
        form.organismGroup = rule.getOrganismGroup();
        form.interpretation = rule.getInterpretation();
        form.source = rule.getSource();
        form.active = rule.isActive();
        return form;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
