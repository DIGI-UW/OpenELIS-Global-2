package org.openelisglobal.containertype.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.containertype.dao.ContainerTypeDAO;
import org.openelisglobal.containertype.form.ContainerTypeForm;
import org.openelisglobal.containertype.form.ContainerTypeUsage;
import org.openelisglobal.containertype.form.ContainerTypeView;
import org.openelisglobal.containertype.valueholder.ContainerType;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContainerTypeServiceImpl implements ContainerTypeService {

    private static final Pattern HEX_COLOUR = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final Pattern CODE = Pattern.compile("^[A-Z0-9][A-Z0-9_-]{0,39}$");
    private static final BigDecimal MAX_VOLUME = new BigDecimal("99999999.99");

    @Autowired
    private ContainerTypeDAO containerTypeDAO;
    @Lazy
    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Override
    @Transactional(readOnly = true)
    public List<ContainerTypeView> list(String search, String domain, String category, boolean includeInactive) {
        String needle = StringUtils.trimToEmpty(search).toLowerCase(Locale.ROOT);
        return containerTypeDAO.getAll().stream().filter(type -> includeInactive || type.isActive())
                .filter(type -> StringUtils.isBlank(domain) || type.getDomains().contains(domain))
                .filter(type -> StringUtils.isBlank(category) || category.equalsIgnoreCase(type.getCategory()))
                .filter(type -> needle.isEmpty() || matches(type, needle))
                .sorted(Comparator.comparing(ContainerType::getName, String.CASE_INSENSITIVE_ORDER))
                .map(type -> toView(type, containerTypeDAO.countSamplesUsing(type.getName()))).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ContainerTypeView> get(Long id) {
        return find(id).map(type -> toView(type, containerTypeDAO.countSamplesUsing(type.getName())));
    }

    @Override
    @Transactional
    public ContainerTypeView save(Long id, ContainerTypeForm form, String sysUserId) {
        ContainerType type = id == null ? new ContainerType()
                : find(id).orElseThrow(() -> new ContainerTypeRefusedException("id", "notFound"));
        String code = StringUtils.trimToEmpty(form.code()).toUpperCase(Locale.ROOT);
        if (StringUtils.isBlank(form.name())) {
            throw new ContainerTypeRefusedException("name", "nameRequired");
        }
        if (!CODE.matcher(code).matches()) {
            throw new ContainerTypeRefusedException("code", "codeInvalid");
        }
        containerTypeDAO.findByCode(code).filter(other -> !other.getId().equals(type.getId())).ifPresent(other -> {
            throw new ContainerTypeRefusedException("code", "codeTaken");
        });
        Set<String> domains = new LinkedHashSet<>();
        for (String domain : form.domains() == null ? List.<String>of() : form.domains()) {
            String value = StringUtils.trimToEmpty(domain).toUpperCase(Locale.ROOT);
            if (!DOMAINS.contains(value)) {
                throw new ContainerTypeRefusedException("domains", "domainInvalid");
            }
            domains.add(value);
        }
        if (domains.isEmpty()) {
            throw new ContainerTypeRefusedException("domains", "domainRequired");
        }
        String hex = StringUtils.trimToNull(form.capColourHex());
        if (hex != null && !HEX_COLOUR.matcher(hex).matches()) {
            throw new ContainerTypeRefusedException("capColourHex", "colourInvalid");
        }
        if (form.nominalVolume() != null && form.nominalVolume().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ContainerTypeRefusedException("nominalVolume", "volumeInvalid");
        }
        if (form.nominalVolume() != null && form.nominalVolume().compareTo(MAX_VOLUME) > 0) {
            throw new ContainerTypeRefusedException("nominalVolume", "volumeTooLarge");
        }
        refuseIfLonger("name", form.name(), 120);
        refuseIfLonger("category", form.category(), 60);
        refuseIfLonger("additive", form.additive(), 120);
        refuseIfLonger("material", form.material(), 60);
        refuseIfLonger("volumeUnit", form.volumeUnit(), 10);
        refuseIfLonger("capColourName", form.capColourName(), 40);
        refuseIfLonger("standardCode", form.standardCode(), 60);
        Set<Integer> sampleTypeIds = new LinkedHashSet<>();
        for (String sampleTypeId : form.sampleTypeIds() == null ? List.<String>of() : form.sampleTypeIds()) {
            TypeOfSample sampleType = StringUtils.isNumeric(sampleTypeId)
                    ? typeOfSampleService.getTypeOfSampleById(sampleTypeId)
                    : null;
            if (sampleType == null) {
                throw new ContainerTypeRefusedException("sampleTypeIds", "sampleTypeUnknown");
            }
            sampleTypeIds.add(Integer.valueOf(sampleTypeId));
        }
        type.setCode(code);
        type.setName(form.name().trim());
        type.setCategory(StringUtils.trimToNull(form.category()));
        type.setAdditive(StringUtils.trimToNull(form.additive()));
        type.setMaterial(StringUtils.trimToNull(form.material()));
        type.setNominalVolume(form.nominalVolume());
        type.setVolumeUnit(form.nominalVolume() == null ? null : StringUtils.trimToNull(form.volumeUnit()));
        type.setCapColourHex(hex == null ? null : hex.toUpperCase(Locale.ROOT));
        type.setCapColourName(StringUtils.trimToNull(form.capColourName()));
        type.setStandardCode(StringUtils.trimToNull(form.standardCode()));
        type.getDomains().clear();
        type.getDomains().addAll(domains);
        type.getSampleTypeIds().clear();
        type.getSampleTypeIds().addAll(sampleTypeIds);
        type.setEdited(true);
        type.setSysUserId(sysUserId);
        if (type.getId() == null) {
            type.setId(containerTypeDAO.insert(type));
        } else {
            containerTypeDAO.update(type);
        }
        return toView(type, containerTypeDAO.countSamplesUsing(type.getName()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ContainerTypeUsage> usage(Long id) {
        return find(id)
                .map(type -> new ContainerTypeUsage(List.of(), containerTypeDAO.countSamplesUsing(type.getName())));
    }

    @Override
    @Transactional
    public int importEnvironmentalContainers() {
        return containerTypeDAO.importEnvironmentalDictionary();
    }

    @Override
    @Transactional
    public Optional<ContainerTypeView> setActive(Long id, boolean active, String sysUserId) {
        return find(id).map(type -> {
            type.setActive(active);
            type.setEdited(true);
            type.setSysUserId(sysUserId);
            containerTypeDAO.update(type);
            return toView(type, containerTypeDAO.countSamplesUsing(type.getName()));
        });
    }

    private Optional<ContainerType> find(Long id) {
        return id == null ? Optional.empty() : containerTypeDAO.get(id);
    }

    private static boolean matches(ContainerType type, String needle) {
        return List
                .of(type.getName(), type.getCode(), StringUtils.defaultString(type.getAdditive()),
                        StringUtils.defaultString(type.getMaterial()))
                .stream().anyMatch(value -> value.toLowerCase(Locale.ROOT).contains(needle));
    }

    private ContainerTypeView toView(ContainerType type, int samplesUsing) {
        List<IdValuePair> sampleTypes = type.getSampleTypeIds().stream()
                .map(sampleTypeId -> typeOfSampleService.getTypeOfSampleById(String.valueOf(sampleTypeId)))
                .filter(java.util.Objects::nonNull)
                .map(sampleType -> new IdValuePair(sampleType.getId(), sampleType.getLocalizedName()))
                .sorted(Comparator.comparing(IdValuePair::getValue, String.CASE_INSENSITIVE_ORDER)).toList();
        List<String> domains = DOMAINS.stream().filter(type.getDomains()::contains).toList();
        return new ContainerTypeView(type.getId(), type.getCode(), type.getName(), type.getCategory(),
                type.getAdditive(), type.getMaterial(), type.getNominalVolume(), type.getVolumeUnit(),
                type.getCapColourHex(), type.getCapColourName(), type.getStandardCode(), type.isActive(), domains,
                sampleTypes, samplesUsing);
    }

    /** A value longer than its column is refused on its field, never stored. */
    private static void refuseIfLonger(String field, String value, int max) {
        if (value != null && value.trim().length() > max) {
            throw new ContainerTypeRefusedException(field, "tooLong");
        }
    }
}
