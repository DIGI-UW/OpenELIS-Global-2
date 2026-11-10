package org.openelisglobal.microbiology.service;

import java.util.*;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.form.MicroDstForm;
import org.openelisglobal.microbiology.valueholder.MicroMolecularDstBinding;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MicroMolecularDstCatalogService {
    private final MicroMolecularDstBindingDAO bindings;
    private final MicroAntibioticDAO antibiotics;
    private final TestService tests;
    private final TestResultComponentService components;
    private final TestResultService options;
    private final DictionaryService dictionaries;

    public MicroMolecularDstCatalogService(MicroMolecularDstBindingDAO bindings, MicroAntibioticDAO antibiotics,
            TestService tests, TestResultComponentService components, TestResultService options,
            DictionaryService dictionaries) {
        this.bindings = bindings;
        this.antibiotics = antibiotics;
        this.tests = tests;
        this.components = components;
        this.options = options;
        this.dictionaries = dictionaries;
    }

    public MicroDstForm.Catalog getCatalog() {
        var catalog = new MicroDstForm.Catalog();
        catalog.bindings = bindings.getBindings().stream().map(this::toForm).toList();
        catalog.antibiotics = antibiotics.getActiveAntibiotics().stream()
                .filter(a -> !"NONE".equals(a.getResistanceRole()))
                .map(a -> new MicroDstForm.Option(a.getId(), a.getDisplayName())).toList();
        catalog.tests = tests.getAllActiveTests(false).stream().map(t -> new MicroDstForm.TestOption(t.getId(),
                t.getName(),
                components.getActiveComponentsByTestId(t.getId()).stream()
                        .map(c -> new MicroDstForm.ComponentOption(c.getId(), c.getLabel(),
                                options.getActiveOptionsByComponentId(c.getId()).stream()
                                        .map(o -> new MicroDstForm.Option(o.getValue(),
                                                "D".equals(o.getTestResultType()) || "M".equals(o.getTestResultType())
                                                        ? dictionaries.getDictionaryById(o.getValue()).getDictEntry()
                                                        : o.getValue()))
                                        .toList()))
                        .filter(c -> c.options().size() > 1).toList()))
                .filter(t -> !t.components().isEmpty()).toList();
        return catalog;
    }

    @Transactional
    public MicroDstForm.Binding save(MicroDstForm.Binding request, String actor) {
        MicroCaseServiceImpl.requireText(actor, "actor");
        if (request == null)
            throw new IllegalArgumentException("DST_BINDING_INVALID");
        var catalog = getCatalog();
        var test = catalog.tests.stream().filter(t -> t.id().equals(request.testId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("DST_BINDING_TEST_INVALID"));
        var component = test.components().stream().filter(c -> c.id().equals(request.componentId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("DST_BINDING_COMPONENT_INVALID"));
        if (Objects.equals(request.susceptibleValue, request.resistantValue)
                || component.options().stream().noneMatch(o -> o.id().equals(request.susceptibleValue))
                || component.options().stream().noneMatch(o -> o.id().equals(request.resistantValue))
                || catalog.antibiotics.stream().noneMatch(a -> a.id().equals(request.antibioticId)))
            throw new IllegalArgumentException("DST_BINDING_VALUES_INVALID");
        if (catalog.bindings.stream().anyMatch(b -> !Objects.equals(b.id, request.id) && b.testId.equals(request.testId)
                && b.componentId.equals(request.componentId) && b.antibioticId.equals(request.antibioticId)))
            throw new MicroAstConflictException("DST_BINDING_DUPLICATE");
        var binding = request.id == null ? new MicroMolecularDstBinding()
                : bindings.get(request.id).orElseThrow(() -> new IllegalArgumentException("DST_BINDING_NOT_FOUND"));
        binding.setTestId(request.testId);
        binding.setComponentId(request.componentId);
        binding.setAntibioticId(request.antibioticId);
        binding.setSusceptibleValue(request.susceptibleValue);
        binding.setResistantValue(request.resistantValue);
        binding.setSysUserId(actor);
        if (request.id == null)
            bindings.insert(binding);
        else
            bindings.update(binding);
        return toForm(binding);
    }

    private MicroDstForm.Binding toForm(MicroMolecularDstBinding binding) {
        var form = new MicroDstForm.Binding();
        form.id = binding.getId();
        form.testId = binding.getTestId();
        form.componentId = binding.getComponentId();
        form.antibioticId = binding.getAntibioticId();
        form.susceptibleValue = binding.getSusceptibleValue();
        form.resistantValue = binding.getResistantValue();
        return form;
    }
}
