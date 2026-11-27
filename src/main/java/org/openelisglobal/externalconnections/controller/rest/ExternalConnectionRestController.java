package org.openelisglobal.externalconnections.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jasypt.util.text.TextEncryptor;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.externalconnections.form.ExternalConnectionForm;
import org.openelisglobal.externalconnections.service.ExternalConnectionAuthenticationDataService;
import org.openelisglobal.externalconnections.service.ExternalConnectionContactService;
import org.openelisglobal.externalconnections.service.ExternalConnectionService;
import org.openelisglobal.externalconnections.valueholder.BasicAuthenticationData;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection.AuthType;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection.ProgrammedConnection;
import org.openelisglobal.externalconnections.valueholder.ExternalConnectionAuthenticationData;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.localization.valueholder.Localization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest")
@PreAuthorize("hasRole('ADMIN')")
public class ExternalConnectionRestController extends BaseController {

    static final int MAX_COLUMN_LENGTH = 255;

    private static final String[] ALLOWED_FIELDS = new String[] { "externalConnection.id", "externalConnection.active",
            "externalConnection.programmedConnection", "externalConnection.activeAuthenticationType",
            "externalConnection.uri", "externalConnection.nameLocalization.localizedValue",
            "externalConnection.descriptionLocalization.localizedValue", "basicAuthenticationData.username",
            "basicAuthenticationData.password", "externalConnectionContacts*" };

    @Autowired
    private ExternalConnectionService externalConnectionService;

    @Autowired
    private TextEncryptor textEncryptor;

    @Autowired
    private ExternalConnectionAuthenticationDataService externalConnectionAuthenticationDataService;

    @Autowired
    private ExternalConnectionContactService externalConnectionContactService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping(value = "/ExternalConnection")
    public ResponseEntity<Object> showExternalConnection(
            @RequestParam(value = ID, required = false) Integer externalConnectionId) {
        ExternalConnectionForm form = new ExternalConnectionForm();
        form.setAuthenticationTypes(Arrays.asList(AuthType.values()));

        if (externalConnectionId == null || externalConnectionId == 0) {
            form.setProgrammedConnections(offeredConnectionTypes());
            form.setExternalConnection(new ExternalConnection());
            form.setBasicAuthenticationData(new BasicAuthenticationData());
            form.setExternalConnectionContacts(new ArrayList<>());
        } else {
            ExternalConnection connection = externalConnectionService.get(externalConnectionId);
            if (connection == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("External connection not found.");
            }
            form.setProgrammedConnections(Arrays.asList(ProgrammedConnection.values()));
            form.setExternalConnection(connection);
            form.setExternalConnectionContacts(
                    externalConnectionContactService.getAllMatching("externalConnection.id", externalConnectionId));
            Map<AuthType, ExternalConnectionAuthenticationData> authData = externalConnectionAuthenticationDataService
                    .getForExternalConnection(externalConnectionId);
            form.setBasicAuthenticationData(withoutPassword((BasicAuthenticationData) authData.get(AuthType.BASIC)));
            form.setPasswordSet(hasPassword((BasicAuthenticationData) authData.get(AuthType.BASIC)));
        }

        return ResponseEntity.ok(form);
    }

    /**
     * The connection types a new row may take. Email, SMS and WhatsApp are set up
     * on the Channels page; their rows here are read only legacy.
     */
    static List<ProgrammedConnection> offeredConnectionTypes() {
        List<ProgrammedConnection> offered = new ArrayList<>();
        for (ProgrammedConnection type : ProgrammedConnection.values()) {
            if (!CHANNEL_TYPES.contains(type)) {
                offered.add(type);
            }
        }
        return offered;
    }

    static final Set<ProgrammedConnection> CHANNEL_TYPES = EnumSet.of(ProgrammedConnection.SMTP_SERVER,
            ProgrammedConnection.WHATSAPP_SERVER);

    /**
     * The stored secret never goes to the browser; the form says whether one is
     * set.
     */
    private static BasicAuthenticationData withoutPassword(BasicAuthenticationData stored) {
        if (stored == null) {
            return null;
        }
        BasicAuthenticationData shown = new BasicAuthenticationData();
        shown.setId(stored.getId());
        shown.setUsername(stored.getUsername());
        return shown;
    }

    /**
     * The address, user name and stored (encrypted) password each fit in 255
     * characters. A longer value used to fail when the transaction committed, after
     * this controller had returned, and the form only showed "Internal Server
     * Error".
     */
    private String tooLongField(ExternalConnection connection, BasicAuthenticationData basic) {
        if (connection.getUri() != null && connection.getUri().toString().length() > MAX_COLUMN_LENGTH) {
            return MessageUtil.getMessage("externalconnections.error.uriTooLong", String.valueOf(MAX_COLUMN_LENGTH));
        }
        if (basic == null) {
            return null;
        }
        if (basic.getUsername() != null && basic.getUsername().length() > MAX_COLUMN_LENGTH) {
            return MessageUtil.getMessage("externalconnections.error.usernameTooLong",
                    String.valueOf(MAX_COLUMN_LENGTH));
        }
        String password = basic.getPassword();
        if (password != null && !password.isEmpty() && textEncryptor.encrypt(password).length() > MAX_COLUMN_LENGTH) {
            return MessageUtil.getMessage("externalconnections.error.passwordTooLong");
        }
        return null;
    }

    private static boolean hasPassword(BasicAuthenticationData stored) {
        return stored != null && stored.getPassword() != null && !stored.getPassword().isEmpty();
    }

    @PostMapping(value = "/ExternalConnection")
    public ResponseEntity<Object> saveExternalConnection(HttpServletRequest request,
            @RequestBody ExternalConnectionForm form) {

        ExternalConnection formConnection = form.getExternalConnection();
        if (formConnection == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "External connection data is required."));
        }

        String sysUserId = getSysUserId(request);
        boolean isNew = formConnection.getId() == null || formConnection.getId() == 0;
        if (isNew && CHANNEL_TYPES.contains(formConnection.getProgrammedConnection())) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Email, SMS and WhatsApp are set up on the Notifications > Channels page."));
        }
        String tooLong = tooLongField(formConnection, form.getBasicAuthenticationData());
        if (tooLong != null) {
            return ResponseEntity.badRequest().body(Map.of("message", tooLong));
        }

        try {
            if (isNew) {
                ExternalConnection newConnection = new ExternalConnection();
                newConnection.setSysUserId(sysUserId);
                newConnection.setActive(formConnection.getActive() != null ? formConnection.getActive() : true);
                newConnection.setProgrammedConnection(formConnection.getProgrammedConnection());
                newConnection.setActiveAuthenticationType(formConnection.getActiveAuthenticationType());
                newConnection.setUri(formConnection.getUri());

                Localization nameLoc = new Localization();
                nameLoc.setDescription("external connection name");
                if (formConnection.getNameLocalization() != null) {
                    nameLoc.setLocalizedValue(formConnection.getNameLocalization().getLocalizedValue());
                }
                newConnection.setNameLocalization(nameLoc);

                Localization descLoc = new Localization();
                descLoc.setDescription("external connection description");
                if (formConnection.getDescriptionLocalization() != null) {
                    descLoc.setLocalizedValue(formConnection.getDescriptionLocalization().getLocalizedValue());
                }
                newConnection.setDescriptionLocalization(descLoc);

                Map<AuthType, ExternalConnectionAuthenticationData> authDataMap = new HashMap<>();
                if (form.getBasicAuthenticationData() != null && form.getBasicAuthenticationData().getUsername() != null
                        && !form.getBasicAuthenticationData().getUsername().isEmpty()) {
                    BasicAuthenticationData basicAuth = new BasicAuthenticationData();
                    basicAuth.setSysUserId(sysUserId);
                    basicAuth.setUsername(form.getBasicAuthenticationData().getUsername());
                    basicAuth.setPassword(form.getBasicAuthenticationData().getPassword());
                    authDataMap.put(AuthType.BASIC, basicAuth);
                }

                externalConnectionService.createNewExternalConnection(authDataMap, new ArrayList<>(), newConnection);
            } else {
                String nameValue = formConnection.getNameLocalization() != null
                        ? formConnection.getNameLocalization().getLocalizedValue()
                        : null;
                String descValue = formConnection.getDescriptionLocalization() != null
                        ? formConnection.getDescriptionLocalization().getLocalizedValue()
                        : null;
                String basicUsername = null;
                String basicPassword = null;
                if (form.getBasicAuthenticationData() != null && form.getBasicAuthenticationData().getUsername() != null
                        && !form.getBasicAuthenticationData().getUsername().isEmpty()) {
                    basicUsername = form.getBasicAuthenticationData().getUsername();
                    basicPassword = form.getBasicAuthenticationData().getPassword();
                }

                externalConnectionService.updateExternalConnectionFields(formConnection.getId(), sysUserId,
                        formConnection.getActive(), formConnection.getProgrammedConnection(),
                        formConnection.getActiveAuthenticationType(), formConnection.getUri(), nameValue, descValue,
                        basicUsername, basicPassword);
            }
            ConfigurationProperties.forceReload();
        } catch (LIMSRuntimeException e) {
            LogEvent.logError(e);
            if (e.getCause() instanceof org.hibernate.StaleObjectStateException) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message",
                        "Another transaction has updated this record. Please refresh and try again."));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to save external connection: " + e.getMessage()));
        }

        return ResponseEntity.ok(Map.of("message", "External connection saved successfully."));
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "externalConnectionDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/ExternalConnectionsMenu";
        }
        return "PageNotFound";
    }

    @Override
    protected String getPageTitleKey() {
        return "externalConnections.browse.title";
    }

    @Override
    protected String getPageSubtitleKey() {
        return "externalConnections.browse.title";
    }
}
