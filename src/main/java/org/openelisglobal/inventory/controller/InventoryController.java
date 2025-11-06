package org.openelisglobal.inventory.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.inventory.action.InventoryUtility;
import org.openelisglobal.inventory.form.InventoryForm;
import org.openelisglobal.inventory.form.InventoryKitItem;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.service.InventoryLocationService;
import org.openelisglobal.inventory.service.InventoryReceiptService;
import org.openelisglobal.inventory.validation.InventoryFormValidator;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLocation;
import org.openelisglobal.inventory.valueholder.InventoryReceipt;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@SessionAttributes("form")
public class InventoryController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "newKitsXML", "inventoryItems*.isActive",
            "inventoryItems*.isModified", "inventoryItems*.inventoryLocationId", "inventoryItems*.kitName",
            "inventoryItems*.type", "inventoryItems*.receiveDate", "inventoryItems*.expirationDate",
            "inventoryItems*.lotNumber", "inventoryItems*.organizationId", "inventoryItems*.source" };

    @Autowired
    private InventoryFormValidator formValidator;
    @Autowired
    private InventoryItemService inventoryItemService;
    @Autowired
    private InventoryLocationService inventoryLocationService;
    @Autowired
    private InventoryReceiptService inventoryReceiptService;
    @Autowired
    private OrganizationService organizationService;

    @ModelAttribute("form")
    public InventoryForm initForm() {
        return new InventoryForm();
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/ManageInventory", method = RequestMethod.GET)
    public ModelAndView showManageInventory(HttpServletRequest request, @ModelAttribute("form") BaseForm oldForm) {
        InventoryForm newForm = resetSessionFormToType(oldForm, InventoryForm.class);

        setupDisplayItems(newForm);

        addFlashMsgsToRequest(request);
        return findForward(FWD_SUCCESS, newForm);
    }

    private void setupDisplayItems(InventoryForm form) {
        request.setAttribute(ALLOW_EDITS_KEY, "true");
        request.setAttribute(PREVIOUS_DISABLED, "true");
        request.setAttribute(NEXT_DISABLED, "true");
        request.getSession().setAttribute(SAVE_DISABLED, FALSE);

        InventoryUtility utility = SpringContext.getBean(InventoryUtility.class);
        List<InventoryKitItem> list = utility.getExistingInventory();
        List<String> kitTypes = getTestKitTypes();
        List<IdValuePair> sources = getSources();

        form.setInventoryItems(list);
        form.setKitTypes(kitTypes);
        form.setSources(sources);
    }

    private List<String> getTestKitTypes() {
        List<String> types = new ArrayList<>();
        types.add(InventoryUtility.HIV);
        types.add(InventoryUtility.SYPHILIS);

        return types;
    }

    private List<IdValuePair> getSources() {
        List<IdValuePair> sources = new ArrayList<>();

        List<Organization> organizations = organizationService.getOrganizationsByTypeName("organizationName",
                "TestKitVender");

        for (Organization organization : organizations) {
            sources.add(new IdValuePair(organization.getId(), organization.getOrganizationName()));
        }

        return sources;
    }

    @RequestMapping(value = "/ManageInventory", method = RequestMethod.POST)
    public ModelAndView showManageInventoryUpdate(HttpServletRequest request,
            @ModelAttribute("form") @Validated(InventoryForm.ManageInventory.class) InventoryForm form,
            BindingResult result, RedirectAttributes redirectAttributes, SessionStatus status) {
        formValidator.validate(form, result);
        if (result.hasErrors()) {
            saveErrors(result);
            setupDisplayItems(form);
            return findForward(FWD_FAIL_INSERT, form);
        }
        List<InventoryKitItem> modifiedItems = getModifiedItems(form);
        List<InventorySet> modifiedInventory = createInventoryFromModifiedItems(modifiedItems);
        List<InventorySet> newInventory = createNewInventory(form);

        Errors errors = validateNewInventory(newInventory);

        if (errors.hasErrors()) {
            saveErrors(errors);
            setupDisplayItems(form);
            return findForward(FWD_FAIL_INSERT, form);
        }

        try {
            for (InventorySet inventory : modifiedInventory) {
                inventoryItemService.update(inventory.getItem());

                inventory.getLocation().setInventoryItem(inventory.getItem());
                inventoryLocationService.update(inventory.getLocation());

                inventoryReceiptService.update(inventory.getReceipt());
            }

            for (InventorySet inventory : newInventory) {
                inventoryItemService.insert(inventory.getItem());

                String id = inventory.getItem().getId();
                inventory.getLocation().setInventoryItem(inventory.getItem());
                inventory.getReceipt().setInventoryItemId(id);

                inventoryLocationService.insert(inventory.getLocation());
                inventoryReceiptService.insert(inventory.getReceipt());
            }

            // tx.commit();
        } catch (LIMSRuntimeException e) {
            // tx.rollback();
            setupDisplayItems(form);
            return findForward(FWD_FAIL_INSERT, form);
        }

        status.setComplete();
        redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);
        return findForward(FWD_SUCCESS_INSERT, form);
    }

    private Errors validateNewInventory(List<InventorySet> newInventory) {
        Errors errors = new BaseErrors();

        List<InventoryItem> items = inventoryItemService.getAllInventoryItems();
        List<String> names = new ArrayList<>();

        for (InventoryItem item : items) {
            names.add(item.getName());
        }

        for (InventorySet kitItem : newInventory) {
            for (String name : names) {
                if (name.equals(kitItem.getItem().getName())) {
                    errors.reject("errors.DuplicateRecord",
                            new String[] { MessageUtil.getMessage("inventory.testKit.name") },
                            "errors.DuplicateRecord");
                    return errors;
                }
            }
        }

        return errors;
    }

    private List<InventorySet> createInventoryFromModifiedItems(List<InventoryKitItem> modifiedItems) {
        List<InventorySet> modifiedInventory = new ArrayList<>();

        for (InventoryKitItem kitItem : modifiedItems) {
            modifiedInventory.add(createInventorySetFromInventoryKitItem(kitItem));
        }
        return modifiedInventory;
    }

    private InventorySet createInventorySetFromInventoryKitItem(InventoryKitItem kitItem) {
        InventoryItem item = new InventoryItem();
        InventoryLocation location = new InventoryLocation();
        InventoryReceipt receipt = new InventoryReceipt();

        InventorySet set = new InventorySet(item, location, receipt);

        item.setId(kitItem.getInventoryItemId());
        item.setIsActive(kitItem.getIsActive() ? "Y" : "N");
        item.setDescription(kitItem.getType());
        item.setName(kitItem.getKitName());
        item.setSysUserId(getSysUserId(request));

        location.setId(kitItem.getInventoryLocationId());
        location.setExpirationDate(DateUtil.convertStringDateToTruncatedTimestamp(kitItem.getExpirationDate()));
        location.setLotNumber(kitItem.getLotNumber());
        location.setSysUserId(getSysUserId(request));

        Organization organization = new Organization();
        organization.setId(kitItem.getOrganizationId());
        organizationService.getData(organization);

        receipt.setId(kitItem.getInventoryReceiptId());
        receipt.setOrganization(organization);
        receipt.setInventoryItemId(kitItem.getInventoryItemId());
        receipt.setReceivedDate(DateUtil.convertStringDateToTruncatedTimestamp(kitItem.getReceiveDate()));
        receipt.setSysUserId(getSysUserId(request));

        return set;
    }

    @SuppressWarnings("unchecked")
    private List<InventorySet> createNewInventory(InventoryForm form) {
        List<InventorySet> newInventory = new ArrayList<>();

        String newInventoryXml = form.getNewKitsXML();

        try {
            Document inventoryDom = DocumentHelper.parseText(newInventoryXml);

            for (Iterator i = inventoryDom.getRootElement().elementIterator("kit"); i.hasNext();) {

                Element kitItem = (Element) i.next();

                String kitName = kitItem.attributeValue("kitName");
                String receiveDate = kitItem.attributeValue("receive");
                String expirationDate = kitItem.attributeValue("expiration");
                String lotNumber = kitItem.attributeValue("lotNumber");
                String kitType = kitItem.attributeValue("kitType");
                String organizationId = kitItem.attributeValue("organizationId");

                InventorySet set = createInventorySet(kitName, receiveDate, expirationDate, lotNumber, kitType,
                        organizationId);

                newInventory.add(set);
            }
        } catch (DocumentException e) {
            LogEvent.logError(e.getMessage(), e);
        }
        return newInventory;
    }

    private InventorySet createInventorySet(String kitName, String receiveDate, String expirationDate, String lotNumber,
            String kitType, String organizationId) throws LIMSRuntimeException {
        InventoryItem item = new InventoryItem();
        InventoryLocation location = new InventoryLocation();
        InventoryReceipt receipt = new InventoryReceipt();

        InventorySet set = new InventorySet(item, location, receipt);

        item.setIsActive("Y");
        item.setDescription(kitType);
        item.setName(kitName);
        item.setSysUserId(getSysUserId(request));

        location.setExpirationDate(DateUtil.convertStringDateToTruncatedTimestamp(expirationDate));
        location.setLotNumber(lotNumber);
        location.setSysUserId(getSysUserId(request));

        Organization organization = new Organization();
        organization.setId(organizationId);
        organizationService.getData(organization);

        receipt.setOrganization(organization);
        receipt.setReceivedDate(DateUtil.convertStringDateToTruncatedTimestamp(receiveDate));
        receipt.setSysUserId(getSysUserId(request));
        return set;
    }

    @SuppressWarnings("unchecked")
    private List<InventoryKitItem> getModifiedItems(InventoryForm form) {
        List<InventoryKitItem> allItems = form.getInventoryItems();
        List<InventoryKitItem> modifiedItems = new ArrayList<>();

        if (allItems != null) {
            for (InventoryKitItem item : allItems) {
                if (item.getIsModified()) {
                    modifiedItems.add(item);
                }
            }
        }
        return modifiedItems;
    }

    // protected Errors validateAll(HttpServletRequest request, Errors errors,
    // BaseForm form) {
    //
    // // test validation against database
    // String testNameSelected = (String) form.get("testName");
    //
    // if (!StringUtil.isNullorNill(testNameSelected)) {
    // Test test = new Test();
    // test.setTestName(testNameSelected);
    // test = testService.getTestByName(test);
    //
    // String messageKey = "testresult.testName";
    //
    // if (test == null) {
    // errors.reject("errors.invalid", new Object[] {
    // MessageUtil.getMessage(messageKey) }, "errors.invalid");
    // }
    // }
    //
    // // scriptlet validation against database
    // String scriptletSelected = (String) form.get("scriptletName");
    //
    // if (!StringUtil.isNullorNill(scriptletSelected)) {
    // Scriptlet scriptlet = new Scriptlet();
    // scriptlet.setScriptletName(scriptletSelected);
    // scriptlet = scriptletService.getScriptletByName(scriptlet);
    //
    // String messageKey = "testresult.scriptletName";
    //
    // if (scriptlet == null) {
    // errors.reject("errors.invalid", new Object[] {
    // MessageUtil.getMessage(messageKey) }, "errors.invalid");
    // }
    // }
    //
    // // validate for testResult D -> value must be dictionary ID
    // String testResultType = (String) form.get("testResultType");
    //
    // if (testResultType.equals("D")) {
    // String val = (String) form.get("value");
    // String messageKey = "testresult.value";
    // try {
    // Integer.parseInt(val);
    //
    // Dictionary dictionary = new Dictionary();
    // dictionary.setId(val);
    // List dictionarys = dictionaryService.getAll();
    //
    // boolean found = false;
    // for (int i = 0; i < dictionarys.size(); i++) {
    // Dictionary d = (Dictionary) dictionarys.get(i);
    // if (dictionary.getId().equals(d.getId())) {
    // found = true;
    // }
    // }
    //
    // if (!found) {
    // errors.reject("errors.invalid", new Object[] {
    // MessageUtil.getMessage(messageKey) },
    // "errors.invalid");
    // }
    // } catch (NumberFormatException e) {
    // // bugzilla 2154
    // LogEvent.logError(e);
    // errors.reject("errors.invalid", new Object[] {
    // MessageUtil.getMessage(messageKey) }, "errors.invalid");
    // }
    // }
    // return errors;
    // }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "manageInventoryDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/ManageInventory";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "manageInventoryDefinition";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return "inventory.manage.title";
    }

    @Override
    protected String getPageSubtitleKey() {
        return "inventory.manage.title";
    }

    class InventorySet {

        private InventoryLocation location;
        private InventoryItem item;
        private InventoryReceipt receipt;

        public InventorySet(InventoryItem item, InventoryLocation location, InventoryReceipt receipt) {
            this.item = item;
            this.location = location;
            this.receipt = receipt;
        }

        public InventoryLocation getLocation() {
            return location;
        }

        public InventoryItem getItem() {
            return item;
        }

        public InventoryReceipt getReceipt() {
            return receipt;
        }
    }
}
