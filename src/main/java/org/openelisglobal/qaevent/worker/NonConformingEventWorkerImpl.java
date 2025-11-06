package org.openelisglobal.qaevent.worker;

import java.lang.reflect.InvocationTargetException;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.qaevent.form.NonConformingEventForm;
import org.openelisglobal.qaevent.service.NCEventService;
import org.openelisglobal.qaevent.service.NceActionLogService;
import org.openelisglobal.qaevent.service.NceCategoryService;
import org.openelisglobal.qaevent.service.NceSpecimenService;
import org.openelisglobal.qaevent.service.NceTypeService;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.qaevent.valueholder.NceActionLog;
import org.openelisglobal.qaevent.valueholder.NceSpecimen;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NonConformingEventWorkerImpl implements NonConformingEventWorker {

    @Autowired
    private NCEventService ncEventService;
    @Autowired
    private NceSpecimenService nceSpecimenService;
    @Autowired
    private SystemUserService systemUserService;
    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    private NceCategoryService nceCategoryService;
    @Autowired
    private NceTypeService nceTypeService;
    @Autowired
    private NceActionLogService nceActionLogService;

    @Override
    public NcEvent create(String labOrderId, List<String> specimens, String sysUserId, String nceNumber) {
        NcEvent event = new NcEvent();
        event.setSysUserId(sysUserId);
        event.setNceNumber(nceNumber);
        event.setLabOrderNumber(labOrderId);
        event = ncEventService.save(event);
        for (String specimenId : specimens) {
            NceSpecimen specimen = new NceSpecimen();
            specimen.setNceId(Integer.parseInt(event.getId()));
            specimen.setSampleItemId(Integer.parseInt(specimenId));
            specimen.setSysUserId(sysUserId);
            nceSpecimenService.save(specimen);
        }
        return event;
    }

    @Override
    public boolean update(NonConformingEventForm form) {
        NcEvent ncEvent = ncEventService.get(form.getId());
        if (ncEvent != null) {
            ncEvent.setSysUserId(form.getCurrentUserId());
            // date from input field come in this pattern
            Date dateOfEvent = getDate(form.getDateOfEvent(), "dd/MM/yyyy");
            Date reportDate = getDate(form.getReportDate(), "dd/MM/yyyy");

            ncEvent.setStatus("Pending");
            ncEvent.setReportDate(reportDate);
            ncEvent.setDateOfEvent(dateOfEvent);
            ncEvent.setName(form.getName());
            ncEvent.setNameOfReporter(form.getReporterName());
            ncEvent.setPrescriberName(form.getPrescriberName());
            ncEvent.setSite(form.getSite());
            ncEvent.setDescription(form.getDescription());
            ncEvent.setSuspectedCauses(form.getSuspectedCauses());
            ncEvent.setProposedAction(form.getProposedAction());
            ncEvent.setReportingUnitId(form.getReportingUnit());

            if (ncEvent.getNameOfReporter() == null || "".equalsIgnoreCase(ncEvent.getNameOfReporter())) {
                ncEvent.setNameOfReporter(form.getName());
            }
            ncEventService.update(ncEvent);
            return true;
        }
        return false;
    }

    @Override
    public boolean updateFollowUp(NonConformingEventForm form) {
        NcEvent ncEvent = ncEventService.get(form.getId());
        if (ncEvent != null) {
            ncEvent.setStatus("CAPA");
            ncEvent.setLaboratoryComponent(form.getLaboratoryComponent());
            ncEvent.setNceCategoryId(Integer.valueOf(form.getNceCategory()));
            ncEvent.setNceTypeId(Integer.valueOf(form.getNceType()));
            ncEvent.setConsequenceId(form.getConsequences());
            ncEvent.setRecurrenceId(form.getRecurrence());
            ncEvent.setSeverityScore(form.getSeverityScore());
            ncEvent.setColorCode(form.getColorCode());
            ncEvent.setCorrectiveAction(form.getCorrectiveAction());
            ncEvent.setControlAction(form.getControlAction());
            ncEvent.setComments(form.getComments());
            ncEvent.setSysUserId(form.getCurrentUserId());
            ncEventService.update(ncEvent);
            return true;
        }
        return false;
    }

    /**
     * Given a String value, returns the equivalent {@link Date} object.
     *
     * @param value
     * @return {@link Date} object or null if there is a parse error.
     */
    private Date getDate(String value, String pattern) {
        if (value == null) {
            return null;
        }
        try {
            SimpleDateFormat format = new SimpleDateFormat(pattern);
            return new Date(format.parse(value).getTime());
        } catch (ParseException e) {
            LogEvent.logError(e);
        }
        return null;
    }

    @Override
    public void initFormForFollowUp(String nceNumber, NonConformingEventForm form)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        form.setNceCategories(nceCategoryService.getAllNceCategories());
        form.setNceTypes(nceTypeService.getAllNceTypes());
        form.setLabComponentList(
                DisplayListService.getInstance().getList(DisplayListService.ListType.LABORATORY_COMPONENT));
        form.setSeverityConsequencesList(
                DisplayListService.getInstance().getList(DisplayListService.ListType.SEVERITY_CONSEQUENCES_LIST));
        form.setSeverityRecurrenceList(
                DisplayListService.getInstance().getList(DisplayListService.ListType.SEVERITY_RECURRENCE_LIST));
        form.setReportingUnits(
                DisplayListService.getInstance().getList(DisplayListService.ListType.TEST_SECTION_ACTIVE));

        SystemUser systemUser = systemUserService.getUserById(form.getCurrentUserId());
        form.setName(systemUser.getFirstName() + " " + systemUser.getLastName());
        form.setNceNumber(System.currentTimeMillis() + "");
        NcEvent event = ncEventService.getMatch("nceNumber", nceNumber).get();
        if (event != null) {
            form.setReportDate(DateUtil.formatDateAsText(event.getReportDate()));
            form.setDateOfEvent(DateUtil.formatDateAsText(event.getDateOfEvent()));
            form.setName(event.getName());
            form.setReporterName(event.getNameOfReporter());
            form.setPrescriberName(event.getPrescriberName());
            form.setSite(event.getSite());
            form.setDescription(event.getDescription());
            form.setSuspectedCauses(event.getSuspectedCauses());
            form.setProposedAction(event.getProposedAction());
            form.setNceNumber(event.getNceNumber());
            form.setId(event.getId());
            form.setLabOrderNumber(event.getLabOrderNumber());
            form.setReportingUnit(event.getReportingUnitId());

            List<NceSpecimen> specimenList = nceSpecimenService.getAllMatching("nceId", event.getId());

            List<SampleItem> sampleItems = new ArrayList<>();
            for (NceSpecimen specimen : specimenList) {
                SampleItem si = sampleItemService.getData(specimen.getSampleItemId() + "");
                sampleItems.add(si);
            }
            form.setSpecimens(sampleItems);
        }
    }

    @Override
    public void initFormForCorrectiveAction(String nceNumber, NonConformingEventForm form)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        this.initFormForFollowUp(nceNumber, form);
        NcEvent event = ncEventService.getMatch("nceNumber", nceNumber).get();
        if (event != null) {
            form.setActionTypeList(
                    DisplayListService.getInstance().getList(DisplayListService.ListType.ACTION_TYPE_LIST));
            form.setLaboratoryComponent(event.getLaboratoryComponent());
            form.setNceCategory(event.getNceCategoryId() + "");
            form.setNceType(event.getNceTypeId() + "");
            form.setConsequences(event.getConsequenceId());
            form.setRecurrence(event.getRecurrenceId());
            form.setSeverityScore(event.getSeverityScore());
            form.setColorCode(event.getColorCode());
            form.setCorrectiveAction(event.getCorrectiveAction());
            form.setDiscussionDate(event.getDiscussionDate());
            form.setControlAction(event.getControlAction());
            form.setComments(event.getComments());
            form.setActionLog(nceActionLogService.getAllMatching("ncEventId", event.getId()));
        }
    }

    public List<NceActionLog> initNceActionLog(String nceActionLogStr) {
        try {
            Document actionLogDom = DocumentHelper.parseText(nceActionLogStr);

            List<Element> actionLogs = actionLogDom.getRootElement().elements("actionLog");
            List<NceActionLog> nceActionLogList = new ArrayList<>();
            for (Element actionLog : actionLogs) {
                NceActionLog al = new NceActionLog();
                al.setCorrectiveAction(actionLog.element("correctiveAction").getText());
                al.setTurnAroundTime(Integer.parseInt(actionLog.element("turnAroundTime").getText()));
                al.setDateCompleted(getDate(actionLog.element("dateCompleted").getText(), "dd/MM/yyyy"));
                al.setPersonResponsible(actionLog.element("personResponsible").getText());
                if (actionLog.element("id") != null) {
                    al.setId(actionLog.element("id").getText());
                }
                nceActionLogList.add(al);
            }

            /*
             * int length = nceActionLogStr.length(); // JSONArray arr = new
             * JSONArray(nceActionLogStr); ObjectMapper objectMapper = new ObjectMapper();
             * final JsonNode arrNode = objectMapper.readTree(nceActionLogStr);
             *
             * // form.setActionLog(nceActionLogList); for (final JsonNode objNode :
             * arrNode) {
             *
             * JsonNode ca = objNode.get("actionType");
             * actionLog.setActionType(ca.asText());
             *
             * JsonNode pr = objNode.get("personResponsible");
             * actionLog.setPersonResponsible(pr.asText());
             *
             * JsonNode tat = objNode.get("turnAroundTime");
             * actionLog.setTurnAroundTime(tat.asInt());
             *
             * JsonNode dc = objNode.get("dateCompleted"); String date = dc.asText();
             * actionLog.setDateCompleted(getDate(date, "yyyy-MM-dd"));
             *
             * if (objNode.has("ncEventId")) {
             * actionLog.setNcEventId(objNode.get("ncEventId").asInt()); } if
             * (objNode.has("id")) { actionLog.setId(objNode.get("id").asInt() + ""); }
             * nceActionLogList.add(actionLog); }
             */
            return nceActionLogList;
        } /*
           * catch (JsonParseException e) { jpLogEvent.logDebug(e); } catch
           * (JsonMappingException e) { jmLogEvent.logDebug(e); } catch (IOException e) {
           * io.printStackTrace(); }
           */ catch (DocumentException e) {
            LogEvent.logDebug(e);
        }
        return null;
    }

    private void setActionLogs(NonConformingEventForm form, NcEvent ncEvent) {
        if (form.getActionLog() != null) {
            List<NceActionLog> actionLogs = form.getActionLog();
            if (actionLogs != null) {
                for (NceActionLog actionLog : actionLogs) {
                    actionLog.setNcEventId(Integer.parseInt(ncEvent.getId()));
                    actionLog.setSysUserId(form.getCurrentUserId());
                    nceActionLogService.save(actionLog);
                }
            }
        }
    }

    @Override
    public boolean updateCorrectiveAction(NonConformingEventForm form) {
        NcEvent ncEvent = ncEventService.get(form.getId());
        if (ncEvent != null) {
            ncEvent.setDiscussionDate(form.getDiscussionDate());
            ncEvent.setDateCompleted(getDate(form.getDateCompleted(), "dd/MM/yyyy")); // Convert the string to a Date
                                                                                      // object
            setActionLogs(form, ncEvent);
            ncEvent.setSysUserId(form.getCurrentUserId());
            ncEventService.update(ncEvent);
            return true;
        }
        return false;
    }

    @Override
    public boolean resolveNCEvent(NonConformingEventForm form) {
        NcEvent ncEvent = ncEventService.get(form.getId());
        if (ncEvent != null) {
            ncEvent.setDiscussionDate(form.getDiscussionDate());
            setActionLogs(form, ncEvent);
            ncEvent.setStatus("Completed");
            ncEvent.setEffective(form.getEffective());
            SystemUser systemUser = systemUserService.getUserById(form.getCurrentUserId());
            ncEvent.setSignature(systemUser.getNameForDisplay());
            ncEvent.setDateCompleted(getDate(form.getDateCompleted(), "dd/MM/yyyy"));
            ncEvent.setSysUserId(form.getCurrentUserId());
            ncEventService.update(ncEvent);
            return true;
        }
        return false;
    }
}
