package org.openelisglobal.testreflex.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.common.util.LabelValuePair;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.testreflex.action.bean.ReflexRule;
import org.openelisglobal.testreflex.action.bean.ReflexRuleOptions;
import org.openelisglobal.testreflex.action.bean.ReflexRuleOptionsDisplayItem;
import org.openelisglobal.testreflex.service.TestReflexService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/rest/")
public class TestReflexRuleRestController {

    @Autowired
    TestReflexService reflexService;
    @Autowired
    DictionaryService dictionaryService;
    @Autowired
    TypeOfSampleService typeOfSampleService;

    @PostMapping(value = "reflexrule", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void saveReflexRule(HttpServletRequest request, @RequestBody ReflexRule reflexRule) {
        reflexService.saveOrUpdateReflexRule(reflexRule);
    }

    @PostMapping(value = "deactivate-reflexrule/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void deactivateReflexRule(@PathVariable String id) {
        try {
            reflexService.deactivateReflexRule(id);
        } catch (Exception e) {
        }

    }

    @GetMapping(value = "reflexrules", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<ReflexRule> getReflexRules(HttpServletRequest request) {
        List<ReflexRule> rules = reflexService.getAllReflexRules().stream().collect(Collectors.toList());
        rules.forEach(rule -> rule.setToggled(false));
        return !rules.isEmpty() ? rules : Collections.<ReflexRule>emptyList();
    }

    @GetMapping(value = "reflexrule-options", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ReflexRuleOptionsDisplayItem getReflexRuleOptions() {
        ReflexRuleOptionsDisplayItem options = new ReflexRuleOptionsDisplayItem();
        List<LabelValuePair> overallOptions = new ArrayList<>();
        ReflexRuleOptions.OverallOptions.stream()
                .forEach(option -> overallOptions.add(new LabelValuePair(option.getDisplayName(), option.name())));
        List<LabelValuePair> generalRelationOptions = new ArrayList<>();
        ReflexRuleOptions.GeneralRelationOptions.stream().forEach(
                option -> generalRelationOptions.add(new LabelValuePair(option.getDisplayName(), option.name())));
        List<LabelValuePair> numericRelationOptions = new ArrayList<>();
        ReflexRuleOptions.NumericRelationOptions.stream().forEach(
                option -> numericRelationOptions.add(new LabelValuePair(option.getDisplayName(), option.name())));
        options.setOverallOptions(overallOptions);
        options.setGeneralRelationOptions(generalRelationOptions);
        options.setNumericRelationOptions(numericRelationOptions);
        return options;
    }
}
