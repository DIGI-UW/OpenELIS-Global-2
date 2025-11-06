package org.openelisglobal.organization.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.address.service.AddressPartService;
import org.openelisglobal.address.service.OrganizationAddressService;
import org.openelisglobal.address.valueholder.AddressPart;
import org.openelisglobal.address.valueholder.OrganizationAddress;
import org.openelisglobal.citystatezip.service.CityStateZipService;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.formfields.FormFields;
import org.openelisglobal.common.formfields.FormFields.Field;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.dataexchange.fhir.exception.FhirPersistanceException;
import org.openelisglobal.dataexchange.fhir.exception.FhirTransformationException;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.organization.form.OrganizationForm;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.service.OrganizationTypeService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.organization.valueholder.OrganizationType;
import org.owasp.encoder.Encode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.SessionStatus;

@RestController
@RequestMapping("/rest")
public class OrganizationRestController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "id", "parentOrgName",
            "organizationLocalAbbreviation", "organizationName", "shortName", "isActive", "multipleUnit",
            "streetAddress", "city", "department", "commune", "village", "state", "zipCode", "internetAddress",
            "mlsSentinelLabFlag", "cliaNum", "mlsLabFlag", "selectedTypes*" };

    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private OrganizationAddressService organizationAddressService;
    @Autowired
    private CityStateZipService cityStateZipService;
    @Autowired
    private OrganizationTypeService organizationTypeService;
    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private FhirTransformService fhirTransformService;

    // @ModelAttribute("form")
    // public OrganizationForm form() {
    // return new OrganizationForm();
    // }

    // private static boolean useZip =
    // FormFields.getInstance().useField(FormFields.Field.ZipCode);
    private static boolean useState = FormFields.getInstance().useField(FormFields.Field.OrgState);
    private static boolean useDepartment = FormFields.getInstance().useField(Field.ADDRESS_DEPARTMENT);
    private static boolean useCommune = FormFields.getInstance().useField(Field.ADDRESS_COMMUNE);
    private static boolean useVillage = FormFields.getInstance().useField(Field.ADDRESS_VILLAGE);

    private final String DEPARTMENT_ID;
    private final String COMMUNE_ID;
    private final String VILLAGE_ID;

    private static final String DEPARTMENT_ADDRESS_KEY = "department";
    private static final String COMMUNE_ADDRESS_KEY = "commune";
    private static final String VILLAGE_ADDRESS_KEY = "village";

    private static boolean useParentOrganization = FormFields.getInstance().useField(Field.OrganizationParent);
    private static boolean useOrganizationState = FormFields.getInstance().useField(Field.OrgState);
    private static boolean useOrganizationTypeList = FormFields.getInstance().useField(Field.InlineOrganizationTypes);

    public OrganizationRestController(AddressPartService addressPartService) {
        String departmentId = null;
        String communeId = null;
        String villageId = null;

        List<AddressPart> partList = addressPartService.getAll();
        for (AddressPart addressPart : partList) {
            if ("department".equals(addressPart.getPartName())) {
                departmentId = addressPart.getId();
            } else if ("commune".equals(addressPart.getPartName())) {
                communeId = addressPart.getId();
            } else if ("village".equals(addressPart.getPartName())) {
                villageId = addressPart.getId();
            }
        }
        DEPARTMENT_ID = departmentId;
        COMMUNE_ID = communeId;
        VILLAGE_ID = villageId;

        if (useDepartment && departmentId == null) {
            throw new IllegalStateException("can't use department without department Id");
        }
        if (useCommune && communeId == null) {
            throw new IllegalStateException("can't use commune without commune Id");
        }
        if (useVillage && villageId == null) {
            throw new IllegalStateException("can't use village without village Id");
        }
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping(value = { "/Organization", "/NextPreviousOrganization" })
    public ResponseEntity<Object> showOrganization(HttpServletRequest request)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        OrganizationForm newForm = new OrganizationForm();
        // OrganizationForm newForm = resetSessionFormToType(oldForm,
        // OrganizationForm.class);

        newForm.setCancelAction("CancelOrganization");

        // The first job is to determine if we are coming to this action with an
        // ID parameter in the request. If there is no parameter, we are
        // creating a new Organization.
        // If there is a parameter present, we should bring up an existing
        // Organization to edit.
        String id = "";
        String start = "";
        // validate start
        if (StringUtils.isNumericSpace(request.getParameter("startingRecNo"))) {
            start = request.getParameter("startingRecNo");
        }
        // validate id
        if (request.getParameter(ID) != null && request.getParameter(ID).matches(ValidationHelper.ID_REGEX)) {
            id = request.getParameter(ID);
        }

        // request.setAttribute(ALLOW_EDITS_KEY, "true");
        // request.setAttribute(PREVIOUS_DISABLED, "true");
        // request.setAttribute(NEXT_DISABLED, "true");

        List<Dictionary> departmentList = getDepartmentList();
        newForm.setDepartmentList(departmentList);

        Organization organization;

        // redirect to get organization for next or previous entry
        if (FWD_NEXT.equals(request.getParameter("direction"))) {
            organization = organizationService.getNext(id);
            String newId = organization.getId();

            // return new ModelAndView("redirect:/Organization?ID=" +
            // Encode.forUriComponent(newId) +
            // "&startingRecNo="
            // + Encode.forUriComponent(start));
            return ResponseEntity.status(HttpStatus.FOUND).header("Location", "/Organization?ID="
                    + Encode.forUriComponent(newId) + "&startingRecNo=" + Encode.forUriComponent(start)).build();
        } else if (FWD_PREVIOUS.equals(request.getParameter("direction"))) {
            organization = organizationService.getPrevious(id);
            String newId = organization.getId();
            // return new ModelAndView("redirect:/Organization?ID=" +
            // Encode.forUriComponent(newId) +
            // "&startingRecNo="
            // + Encode.forUriComponent(start));
            return ResponseEntity.status(HttpStatus.FOUND).header("Location", "/Organization?ID="
                    + Encode.forUriComponent(newId) + "&startingRecNo=" + Encode.forUriComponent(start)).build();
        }

        boolean isNew = (id == null) || "0".equals(id);
        if (isNew) {
            request.setAttribute("key", "organization.add.title");
            organization = new Organization();

            // default isActive to 'Y'
            organization.setIsActive(YES);
            organization.setMlsSentinelLabFlag(NO);
            organization.setMlsLabFlag("N");
        } else {
            request.setAttribute("key", "organization.edit.title");

            organization = organizationService.get(id);
            if (organization.getOrganization() != null) {
                organization.setSelectedOrgId(organization.getOrganization().getId());
            }

            if (organizationService.hasNext(id)) {
                request.setAttribute(NEXT_DISABLED, "false");
            }
            if (organizationService.hasPrevious(id)) {
                request.setAttribute(PREVIOUS_DISABLED, "false");
            }

            if (useCommune || useDepartment || useVillage) {
                List<OrganizationAddress> orgAddressList = organizationAddressService
                        .getAddressPartsByOrganizationId(id);

                for (OrganizationAddress orgAddress : orgAddressList) {
                    if (useCommune && COMMUNE_ID.equals(orgAddress.getAddressPartId())) {
                        newForm.setCommune(orgAddress.getValue());
                    } else if (useVillage && VILLAGE_ID.equals(orgAddress.getAddressPartId())) {
                        newForm.setVillage(orgAddress.getValue());
                    } else if (useDepartment && DEPARTMENT_ID.equals(orgAddress.getAddressPartId())) {
                        newForm.setDepartment(orgAddress.getValue());
                    }
                }
            }
        }

        // initialize state to MN
        if (organization.getState() == null) {
            organization.setState("MN");
        }

        if (organization.getId() != null && !organization.getId().equals("0")) {
            request.setAttribute(ID, organization.getId());
        }

        PropertyUtils.copyProperties(newForm, organization);

        if (useParentOrganization) {
            setParentOrganiztionName(newForm, organization);
        }

        if (useOrganizationState) {
            setCityStateZipList(newForm);
        }

        if (useOrganizationTypeList) {
            List<OrganizationType> orgTypeList = getOrganizationTypeList();
            List<String> selectedList = new ArrayList<>();
            newForm.setOrgTypes(orgTypeList);

            if (organization.getId() != null && orgTypeList != null) {
                if (orgTypeList.size() > 0) {
                    List<String> selectedOrgTypeList = organizationService
                            .getTypeIdsForOrganizationId(organization.getId());
                    for (String orgTypeId : selectedOrgTypeList) {
                        selectedList.add(orgTypeId);
                    }
                }
            }
            newForm.setSelectedTypes(selectedList);
        }

        handleSelfReferencingParentOrg(newForm.getOrganization());
        // return findForward(FWD_SUCCESS, newForm);
        return ResponseEntity.ok(newForm);
    }

    private void setParentOrganiztionName(OrganizationForm form, Organization organization)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Organization parentOrg = new Organization();
        String parentOrgName = null;

        if (!StringUtil.isNullorNill(organization.getSelectedOrgId())) {
            parentOrg = organizationService.get(organization.getSelectedOrgId());
            parentOrgName = parentOrg.getOrganizationName();
        }

        form.setParentOrgName(parentOrgName);
    }

    private void setCityStateZipList(OrganizationForm form)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        if (FormFields.getInstance().useField(FormFields.Field.OrgState)) {
            // bugzilla 1545
            List states = cityStateZipService.getAllStateCodes();
            form.setStates(states);
        }
    }

    // private void setCityStateZipList(OrganizationForm form)
    // throws IllegalAccessException, InvocationTargetException,
    // NoSuchMethodException {
    // List<CityStateZip> cityStateZipList = cityStateZipService.getAll();
    // form.setCityStateZipList(cityStateZipList);
    // }

    private List<OrganizationType> getOrganizationTypeList() {

        List<OrganizationType> orgTypeList = organizationTypeService.getAll();
        if (orgTypeList == null) {
            orgTypeList = new ArrayList<>();
        }

        return orgTypeList;
    }

    private List<Dictionary> getDepartmentList() {
        return dictionaryService.getDictionaryEntrysByCategoryAbbreviation("description", "haitiDepartment", true);
    }

    @GetMapping("/organization/{id}")
    public ResponseEntity<Organization> getOrganization(@PathVariable("id") String id) {
        Organization organization = organizationService.get(id);
        if (organization != null) {
            handleSelfReferencingParentOrg(organization);
            return ResponseEntity.ok(organization);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(value = "/Organization")
    public ResponseEntity<Object> showUpdateOrganization(@RequestBody @Valid OrganizationForm form,
            BindingResult result) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException,
            FhirTransformationException, FhirPersistanceException {

        // setDefaultButtonAttributes(request);
        if (result.hasErrors()) {
            saveErrors(result);
            // return findForward(FWD_FAIL_INSERT, form);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Failed to save or update organization. Validation errors exist.");
        }

        Organization organization;
        boolean isNew = (StringUtil.isNullorNill(form.getId()) || "0".equals(form.getId()));
        if (isNew) {
            organization = new Organization();
            // request.setAttribute("key", "organization.add.title");
        } else {
            organization = organizationService.get(form.getId());
            // request.setAttribute("key", "organization.edit.title");
            if (organization == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Organization not found.");
            }
        }
        List<String> selectedOrgTypes = form.getSelectedTypes();

        organization.setSysUserId(getSysUserId(request));

        // List states = getPossibleStates(form);

        PropertyUtils.copyProperties(organization, form);

        if (FormFields.getInstance().useField(FormFields.Field.OrganizationParent)) {
            String parentOrgName = form.getParentOrgName();
            Organization o = new Organization();
            o.setOrganizationName(parentOrgName);
            Organization parentOrg = organizationService.getActiveOrganizationByName(o, false);
            organization.setOrganization(parentOrg);
        }
        Map<String, OrganizationAddress> addressParts = createAddressParts(form, isNew);

        try {
            if (!isNew) {
                if (organization.getFhirUuid() == null) {
                    UUID uuid = organizationService.get(organization.getId()).getFhirUuid();
                    if (uuid == null) {
                        organization.setFhirUuid(UUID.randomUUID());
                    } else {
                        organization.setFhirUuid(uuid);
                    }
                }
                organizationService.update(organization);
            } else {
                if (organization.getFhirUuid() == null) {
                    organization.setFhirUuid(UUID.randomUUID());
                }
                organizationService.insert(organization);
            }

            persistAddressParts(organization, addressParts);

            linkOrgWithOrgType(organization, selectedOrgTypes);
            fhirTransformService.transformPersistOrganization(organization);

        } catch (LIMSRuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            if (e.getCause() instanceof org.hibernate.StaleObjectStateException) {
                result.reject("errors.OptimisticLockException");
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Another transaction has updated the organization. Please refresh and try again.");
            } else if
            // bugzilla 1482
            (e.getCause() instanceof LIMSDuplicateRecordException) {
                String messageKey = "organization.organization";
                String msg = MessageUtil.getMessage(messageKey);
                result.reject("errors.DuplicateRecord.activeonly", new String[] { msg },
                        "errors.DuplicateRecord.activeonly");
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Duplicate record error.");
            } else {
                result.reject("errors.UpdateException");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to save or update organization. Error: " + e.getMessage());
            }
            // saveErrors(result);
            // request.setAttribute(PREVIOUS_DISABLED, "true");
            // request.setAttribute(NEXT_DISABLED, "true");
            // return findForward(FWD_FAIL_INSERT, form);
            // return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed
            // to save or
            // update organization. Error: " + e.getMessage());

        }
        // finally {
        // HibernateUtil.closeSession();
        // }
        // PropertyUtils.copyProperties(form, organization);

        // if (states != null) {
        // form.setStates(states);
        // }

        if (organization.getId() != null && !organization.getId().equals("0")) {
            request.setAttribute(ID, organization.getId());
        }

        DisplayListService.getInstance().refreshList(DisplayListService.ListType.REFERRAL_ORGANIZATIONS);
        DisplayListService.getInstance().refreshLists();

        // redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);
        // status.setComplete();
        // return findForward(FWD_SUCCESS_INSERT, form);
        return ResponseEntity.ok("Organization saved or updated successfully.");
    }

    // private void setDefaultButtonAttributes(HttpServletRequest request) {
    // request.setAttribute(ALLOW_EDITS_KEY, "true");
    // request.setAttribute(PREVIOUS_DISABLED, "false");
    // request.setAttribute(NEXT_DISABLED, "false");
    // }

    private void persistAddressParts(Organization organization, Map<String, OrganizationAddress> addressParts) {
        OrganizationAddress departmentAddress = addressParts.get(DEPARTMENT_ADDRESS_KEY);
        if (departmentAddress != null) {
            organizationAddressService.save(departmentAddress);
        }

        OrganizationAddress communeAddress = addressParts.get(COMMUNE_ADDRESS_KEY);
        if (communeAddress != null) {
            organizationAddressService.save(communeAddress);
        }
        OrganizationAddress villageAddress = addressParts.get(VILLAGE_ADDRESS_KEY);
        if (villageAddress != null) {
            organizationAddressService.save(villageAddress);
        }
    }

    private Map<String, OrganizationAddress> createAddressParts(OrganizationForm form, boolean isNew) {
        Map<String, OrganizationAddress> addressParts = new HashMap<>();
        OrganizationAddress departmentAddress = null;
        OrganizationAddress communeAddress = null;
        OrganizationAddress villageAddress = null;
        if (useDepartment || useCommune || useVillage) {
            if (!isNew) {
                List<OrganizationAddress> orgAddressList = organizationAddressService
                        .getAddressPartsByOrganizationId(form.getId());

                for (OrganizationAddress orgAddress : orgAddressList) {
                    if (DEPARTMENT_ID.equals(orgAddress.getAddressPartId())) {
                        departmentAddress = orgAddress;
                    } else if (COMMUNE_ID.equals(orgAddress.getAddressPartId())) {
                        communeAddress = orgAddress;
                    } else if (VILLAGE_ID.equals(orgAddress.getAddressPartId())) {
                        villageAddress = orgAddress;
                    }
                }
            }

            if (useDepartment) {
                if (departmentAddress == null) {
                    departmentAddress = new OrganizationAddress();
                    departmentAddress.setAddressPartId(DEPARTMENT_ID);
                    departmentAddress.setType("D");
                    departmentAddress.setOrganizationId(form.getId());
                }

                departmentAddress.setValue(form.getDepartment());
                departmentAddress.setSysUserId(getSysUserId(request));
                addressParts.put(DEPARTMENT_ADDRESS_KEY, departmentAddress);
            }

            if (useCommune) {
                if (communeAddress == null) {
                    communeAddress = new OrganizationAddress();
                    communeAddress.setAddressPartId(COMMUNE_ID);
                    communeAddress.setType("T");
                    communeAddress.setOrganizationId(form.getId());
                }

                communeAddress.setValue(form.getCommune());
                communeAddress.setSysUserId(getSysUserId(request));
            }

            if (useVillage) {
                if (villageAddress == null) {
                    villageAddress = new OrganizationAddress();
                    villageAddress.setAddressPartId(VILLAGE_ID);
                    villageAddress.setType("T");
                    villageAddress.setOrganizationId(form.getId());
                }

                villageAddress.setValue(form.getVillage());
                villageAddress.setSysUserId(getSysUserId(request));
            }
        }
        return addressParts;
    }

    private void linkOrgWithOrgType(Organization organization, List<String> selectedOrgTypes) {
        organizationService.deleteAllLinksForOrganization(organization.getId());

        for (String typeId : selectedOrgTypes) {
            organizationService.linkOrganizationAndType(organization, typeId);
        }
    }

    private List getPossibleStates(OrganizationForm form) {
        List states = null;
        if (useState) {
            if (form.getStates() != null) {
                states = (List) form.getStates();
            } else {
                states = cityStateZipService.getAllStateCodes();
            }
        }
        return states;
    }

    @GetMapping(value = "/CancelOrganization")
    public ResponseEntity<String> cancelOrganization(HttpServletRequest request, SessionStatus status) {
        status.setComplete();
        // return findForward(FWD_CANCEL, new OrganizationForm());
        return ResponseEntity.ok("Cancellation successful");
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "organizationDefinition";
        } else if (FWD_FAIL.equals(forward)) {
            return "redirect:/MasterListsPage";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/OrganizationMenu";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "organizationDefinition";
        } else if (FWD_CANCEL.equals(forward)) {
            return "redirect:/OrganizationMenu";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return (String) request.getAttribute("key");
    }

    @Override
    protected String getPageSubtitleKey() {
        return (String) request.getAttribute("key");
    }

    public static void handleSelfReferencingParentOrg(Organization childOrg) {
        if (childOrg != null && childOrg.getOrganization() != null
                && childOrg.getOrganization().getId().equals(childOrg.getId())) {
            Organization newParent = new Organization();
            newParent.setOrganizationName(childOrg.getOrganizationName());
            newParent.setId(childOrg.getId());
            childOrg.setOrganization(newParent);
        }

    }
}
