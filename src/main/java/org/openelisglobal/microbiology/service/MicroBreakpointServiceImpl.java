package org.openelisglobal.microbiology.service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.openelisglobal.microbiology.dao.MicroBreakpointRuleDAO;
import org.openelisglobal.microbiology.dao.MicroBreakpointStandardDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.dao.MicroWhonetOrganismDAO;
import org.openelisglobal.microbiology.form.MicroBreakpointDefaultForm;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.openelisglobal.microbiology.valueholder.MicroWhonetOrganism;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroBreakpointServiceImpl implements MicroBreakpointService {

    public static final String DEFAULT_AUTHORITY = "microbiologyDefaultBreakpointAuthority";
    public static final String DEFAULT_VERSION = "microbiologyDefaultBreakpointVersion";

    private final SiteInformationService settings;
    private final MicroBreakpointStandardDAO standardDAO;
    private final MicroBreakpointRuleDAO ruleDAO;
    private final MicroOrganismDAO organismDAO;
    private final MicroWhonetOrganismDAO whonetOrganismDAO;

    public MicroBreakpointServiceImpl(MicroBreakpointStandardDAO standardDAO, MicroBreakpointRuleDAO ruleDAO,
            SiteInformationService settings, MicroOrganismDAO organismDAO, MicroWhonetOrganismDAO whonetOrganismDAO) {
        this.organismDAO = organismDAO;
        this.whonetOrganismDAO = whonetOrganismDAO;
        this.settings = settings;
        this.standardDAO = standardDAO;
        this.ruleDAO = ruleDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public MicroBreakpointDefaultForm getDefaultStandard() {
        MicroBreakpointDefaultForm form = new MicroBreakpointDefaultForm();
        form.authority = setting(DEFAULT_AUTHORITY);
        form.version = setting(DEFAULT_VERSION);
        form.configured = !form.authority.isBlank() || !form.version.isBlank();
        if (form.configured) {
            standardDAO.findByAuthorityAndVersion(form.authority, form.version).ifPresent(standard -> {
                form.standardId = standard.getId();
                form.available = "Y".equals(standard.getIsActive()) && "ACTIVE".equals(standard.getLifecycleStatus());
            });
        }
        return form;
    }

    @Override
    @Transactional
    public MicroBreakpointDefaultForm setDefaultStandard(String standardId, String actorId) {
        if (actorId == null || actorId.isBlank()) {
            throw new IllegalArgumentException("authenticated actor is required");
        }
        MicroBreakpointStandard standard = getStandard(standardId);
        if (standard == null || !"Y".equals(standard.getIsActive())
                || !"ACTIVE".equals(standard.getLifecycleStatus())) {
            throw new IllegalArgumentException("AST_BREAKPOINT_STANDARD_NOT_AVAILABLE");
        }
        saveSetting(DEFAULT_AUTHORITY, standard.getAuthority(), actorId);
        saveSetting(DEFAULT_VERSION, standard.getVersion(), actorId);
        return getDefaultStandard();
    }

    private String setting(String name) {
        SiteInformation value = settings.getSiteInformationByName(name);
        return value == null || value.getValue() == null ? "" : value.getValue().trim();
    }

    private void saveSetting(String name, String value, String actorId) {
        SiteInformation setting = settings.getSiteInformationByName(name);
        boolean isNew = setting == null;
        if (isNew) {
            setting = new SiteInformation();
            setting.setName(name);
            setting.setValueType("text");
        }
        setting.setValue(value);
        setting.setSysUserId(actorId);
        settings.persistData(setting, isNew);
    }

    @Override
    @Transactional(readOnly = true)
    public MicroBreakpointStandard getActiveStandard(String authority, String version) {
        return standardDAO.getActiveStandard(authority, version);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroBreakpointStandard> getActiveStandards() {
        return standardDAO.getActiveStandards();
    }

    @Override
    @Transactional(readOnly = true)
    public MicroBreakpointStandard getStandard(String standardId) {
        return standardDAO.get(standardId).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public MicroBreakpointRule findBreakpointRule(String standardId, String organismId, Set<String> organismGroups,
            String antibioticId, String method, String specimenTypeId, String breakpointType) {
        return ruleDAO.findBestRule(standardId, organismId, organismGroups, antibioticId, method, specimenTypeId,
                breakpointType);
    }

    @Override
    @Transactional(readOnly = true)
    public MicroBreakpointRule findContextualBreakpointRule(String standardId, String organismId,
            Set<String> organismGroups, String antibioticId, String method, String specimenTypeId,
            String breakpointType, String clinicalContext, String diskContent) {
        return ruleDAO.findContextualRule(standardId, organismId, organismGroups, antibioticId, method, specimenTypeId,
                breakpointType, MicroBreakpointClinicalContext.validate(clinicalContext), diskContent);
    }

    @Override
    @Transactional(readOnly = true)
    public MicroBreakpointRule findWhonetBreakpointRule(String standardId, String organismId, String antibioticId,
            String breakpointType, String diskContent, String specimenCode, String clinicalContext) {
        MicroWhonetOrganism organism = organismId == null ? null
                : organismDAO.get(organismId).map(MicroOrganism::getWhonetCode).filter(code -> !code.isBlank())
                        .flatMap(whonetOrganismDAO::findCurrentByCode).orElse(null);
        if (organism == null) {
            return null;
        }
        boolean urine = URINE.equalsIgnoreCase(specimenCode), spinalFluid = SPINAL_FLUID.equalsIgnoreCase(specimenCode);
        boolean meningitis = spinalFluid || "MENINGITIS".equals(clinicalContext);
        boolean endocarditis = "ENDOCARDITIS".equals(clinicalContext);
        boolean stated = spinalFluid || clinicalContext != null;
        // As in the AMR package, a rule for another specimen or indication is dropped
        // before the closest organism level is chosen.
        List<MicroBreakpointRule> rules = ruleDAO.findWhonetRules(standardId, antibioticId, breakpointType).stream()
                .filter(rule -> "ZONE".equals(breakpointType)
                        ? disk(rule.getDiskContent()) != null && disk(rule.getDiskContent()).equals(disk(diskContent))
                        : rule.getDiskContent() == null)
                .filter(rule -> urine || !urinary(rule))
                .filter(rule -> !stated || !forInfection(rule) || has(rule, meningitis, endocarditis))
                .filter(rule -> !names(rule, "non-meningitis") || !meningitis)
                .filter(rule -> !names(rule, "non-endocarditis") || !endocarditis).toList();
        int closest = rules.stream().mapToInt(rule -> level(rule, organism)).min().orElse(NO_LEVEL);
        // An indication that was not stated is not guessed, and no broader rule stands
        // in for the rules that depend on it.
        return rules.stream().filter(rule -> closest < NO_LEVEL && level(rule, organism) == closest)
                .filter(rule -> stated
                        || !forInfection(rule) && !names(rule, "non-meningitis") && !names(rule, "non-endocarditis"))
                .min(Comparator.comparingInt((MicroBreakpointRule rule) -> urine && urinary(rule) ? 0 : 1)
                        .thenComparingInt(rule -> has(rule, meningitis, endocarditis) ? 0 : 1)
                        .thenComparingInt(rule -> site(rule.getSiteOfInfection())))
                .orElse(null);
    }

    // WHONET specimen codes, set on the sample type.
    private static final String URINE = "ur", SPINAL_FLUID = "sf";

    private static boolean forEndocarditis(MicroBreakpointRule rule) {
        return names(rule, "endocarditis") || names(rule, "endocarditis with combination treatment");
    }

    private static boolean forInfection(MicroBreakpointRule rule) {
        return names(rule, "meningitis") || forEndocarditis(rule);
    }

    private static boolean has(MicroBreakpointRule rule, boolean meningitis, boolean endocarditis) {
        return meningitis && names(rule, "meningitis") || endocarditis && forEndocarditis(rule);
    }

    private static boolean names(MicroBreakpointRule rule, String site) {
        return rule.getSiteOfInfection() != null && Arrays.stream(rule.getSiteOfInfection().split(","))
                .anyMatch(named -> named.trim().equalsIgnoreCase(site));
    }

    // "30ug", "30 µg" and "30 mcg" are one disk.
    private static String disk(String content) {
        return content == null ? null
                : content.toLowerCase(Locale.ROOT).replaceAll("\\s", "").replaceAll("(µ|μ|u|mc)g", "mcg");
    }

    private static boolean urinary(MicroBreakpointRule rule) {
        return rule.getSiteOfInfection() != null
                && rule.getSiteOfInfection().toLowerCase(Locale.ROOT).contains("urinary tract");
    }

    private static final int NO_LEVEL = 99;

    /** WHONET's site preference; a rule naming several sites takes its best one. */
    private static final List<String> SITE_ORDER = List.of("non-meningitis", "non-endocarditis", "parenteral", "",
            "uncomplicated urinary tract infection", "infections originating from the urinary tract", "meningitis",
            "endocarditis", "endocarditis with combination treatment", "intravenous", "oral", "inhaled",
            "investigational agent", "extraintestinal", "abscesses", "genital", "intestinal", "liposomal",
            "mammary gland", "mastitis", "metritis", "non-pneumonia", "other infections", "other indications",
            "pneumonia", "prophylaxis", "respiratory", "screen", "skin", "soft tissue", "wounds");

    private static int site(String sites) {
        int best = SITE_ORDER.size();
        for (String site : (sites == null ? "" : sites).split(",", -1)) {
            int index = SITE_ORDER.indexOf(site.trim().toLowerCase(Locale.ROOT));
            best = index >= 0 && index < best ? index : best;
        }
        return best;
    }

    /** Only the closest level counts; valueless rules there hide a wider one. */
    static boolean hasBreakpoint(List<MicroBreakpointRule> rules, MicroWhonetOrganism organism) {
        int closest = rules.stream().mapToInt(rule -> level(rule, organism)).min().orElse(NO_LEVEL);
        return closest < NO_LEVEL && rules.stream().anyMatch(rule -> level(rule, organism) == closest
                && rule.getSusceptibleValue() != null && rule.getResistantValue() != null);
    }

    /** Lower is more specific; NO_LEVEL when the rule is not for this organism. */
    private static int level(MicroBreakpointRule rule, MicroWhonetOrganism organism) {
        String code = rule.getOrganismCode();
        boolean anaerobe = organism.getAnaerobe() != null;
        return switch (rule.getOrganismCodeType()) {
        case "SEROVAR_GROUP" -> same(code, organism.getSerovarGroup()) ? 1 : NO_LEVEL;
        case "WHONET_ORG_CODE" -> same(code, organism.getWhonetOrgCode()) ? 2 : NO_LEVEL;
        case "SPECIES_GROUP" -> same(code, organism.getSpeciesGroup()) ? 3 : NO_LEVEL;
        case "GENUS_CODE" -> same(code, organism.getGenusCode()) ? 4 : NO_LEVEL;
        case "GENUS_GROUP" -> same(code, organism.getGenusGroup()) ? 5 : NO_LEVEL;
        case "FAMILY_CODE" -> same(code, organism.getFamilyCode()) ? 6 : NO_LEVEL;
        case "ANAEROBE+SUBKINGDOM_CODE" -> anaerobe && same(code, "AN" + organism.getSubkingdomCode()) ? 7 : NO_LEVEL;
        case "ANAEROBE" -> anaerobe ? 8 : NO_LEVEL;
        case "ALL" -> 9;
        default -> NO_LEVEL;
        };
    }

    private static boolean same(String ruleCode, String organismCode) {
        return ruleCode != null && ruleCode.equalsIgnoreCase(organismCode);
    }
}
